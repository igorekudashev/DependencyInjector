package dev.igorekudashev.dependencyinjector;

import dev.igorekudashev.dependencyinjector.annotations.DependencyConfiguration;
import dev.igorekudashev.dependencyinjector.annotations.Factory;
import dev.igorekudashev.dependencyinjector.exceptions.DependencyInitializationException;
import dev.igorekudashev.dependencyinjector.exceptions.InvalidDefaultConstructorFactoryException;
import dev.igorekudashev.dependencyinjector.exceptions.InvalidFactoryException;
import dev.igorekudashev.dependencyinjector.exceptions.InvalidFactoryMethodTypeException;
import dev.igorekudashev.dependencyinjector.exceptions.TooManyFactoriesException;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class Dependency implements Comparable<Dependency> {

    private final Class<?> clazz;
    private final int order;
    private final ThrowingSupplier<Object> supplier;

    private Dependency(Class<?> clazz, ThrowingSupplier<Object> supplier, int order) {
        this.clazz = clazz;
        this.supplier = supplier;
        this.order = order;
    }

    public static Dependency getFromObject(Object object, int order) {
        return new Dependency(object.getClass(), () -> object, order);
    }

    public static <C extends Class<?>> Dependency getFromClass(C clazz) {
        Factory classFactoryAnnotation = clazz.getAnnotation(Factory.class);
        if (classFactoryAnnotation != null) {
            Constructor<?>[] declaredConstructors = clazz.getDeclaredConstructors();
            if (declaredConstructors.length == 1 && declaredConstructors[0].getParameterTypes().length == 0) {
                return new Dependency(clazz, () -> createFromConstructor(clazz, declaredConstructors[0]), classFactoryAnnotation.value());
            } else {
                throw new InvalidDefaultConstructorFactoryException(clazz);
            }
        }
        List<Method> factoryMethods = Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Factory.class))
                .collect(Collectors.toList());
        List<Constructor<?>> constructors = Arrays.stream(clazz.getDeclaredConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Factory.class))
                .collect(Collectors.toList());
        if (factoryMethods.size() + constructors.size() == 0) {
            return null;
        } else if (factoryMethods.size() + constructors.size() > 1) {
            throw new TooManyFactoriesException(clazz);
        }
        Executable executable;
        ThrowingSupplier<Object> builder;
        if (factoryMethods.isEmpty()) {
            executable = constructors.get(0);
            builder = () -> createFromConstructor(clazz, (Constructor<?>) executable);
        } else {
            executable = factoryMethods.get(0);
            builder = () -> createFromFactoryMethod(clazz, (Method) executable);
        }
        return new Dependency(
                clazz, builder,
                executable.isAnnotationPresent(Factory.class) ? executable.getAnnotation(Factory.class).value() : 10);
    }

    public static <C extends Class<?>> List<Dependency> getFromConfiguration(C clazz) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Factory.class))
                .map(method -> {
                    ThrowingSupplier<Object> builder = () -> createFromFactoryMethod(clazz, method);
                    return new Dependency(method.getReturnType(), builder, method.getAnnotation(Factory.class).value());
                })
                .collect(Collectors.toList());
    }

    private static <C extends Class<?>> Object createFromFactoryMethod(C clazz, Method method) throws InvocationTargetException, IllegalAccessException {
        if (!Modifier.isStatic(method.getModifiers()) || method.getParameterTypes().length != 0) {
            throw new InvalidFactoryException(clazz, method);
        } else if (!method.getReturnType().equals(clazz) && !clazz.isAnnotationPresent(DependencyConfiguration.class)) {
            throw new InvalidFactoryMethodTypeException(clazz, method);
        }
        method.setAccessible(true);
        return method.invoke(clazz);
    }

    private static <C extends Class<?>> Object createFromConstructor(C clazz, Constructor<?> constructor) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        if (constructor.getParameterTypes().length != 0) {
            throw new InvalidFactoryException(clazz);
        }
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    @Override
    public int compareTo(Dependency o) {
        return order - o.order;
    }

    public Class<?> getDependencyClass() {
        return clazz;
    }

    public Object buildObject() {
        try {
            return supplier.get();
        } catch (Exception e) {
            e.printStackTrace();
            throw new DependencyInitializationException(clazz);
        }
    }
}
