package dev.igorekudashev.dependencyinjector;

import dev.igorekudashev.dependencyinjector.annotations.DependencyConfiguration;
import dev.igorekudashev.dependencyinjector.annotations.Factory;
import dev.igorekudashev.dependencyinjector.exceptions.DependencyInitializationException;
import dev.igorekudashev.dependencyinjector.exceptions.InvalidDefaultConstructorFactoryException;
import dev.igorekudashev.dependencyinjector.exceptions.InvalidFactoryException;
import dev.igorekudashev.dependencyinjector.exceptions.InvalidFactoryMethodTypeException;
import dev.igorekudashev.dependencyinjector.exceptions.TooManyFactoriesException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class Dependency implements Comparable<Dependency> {

    private final Class<?> type;
    private final ThrowingSupplier<Object> builder;
    private final int order;

    public static Dependency getFromPrepared(Object object, int order) {
        return new Dependency(object.getClass(), () -> object, order);
    }

    public static <C extends Class<?>> Dependency getFromClass(C clazz) {
        Factory classFactoryAnnotation = clazz.getAnnotation(Factory.class);
        if (classFactoryAnnotation != null) {
            return Arrays.stream(clazz.getDeclaredConstructors())
                    .filter(constructor -> constructor.getParameterTypes().length == 0)
                    .findFirst()
                    .map(constructor -> new Dependency(clazz, () -> buildFromConstructor(constructor), classFactoryAnnotation.value()))
                    .orElseThrow(() -> new InvalidDefaultConstructorFactoryException(clazz));
        }
        List<Method> factoryMethods = Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Factory.class))
                .toList();
        List<Constructor<?>> constructors = Arrays.stream(clazz.getDeclaredConstructors())
                .filter(constructor -> constructor.isAnnotationPresent(Factory.class))
                .toList();
        if (factoryMethods.size() + constructors.size() == 0) {
            return null;
        } else if (factoryMethods.size() + constructors.size() > 1) {
            throw new TooManyFactoriesException(clazz);
        }
        Executable executable;
        ThrowingSupplier<Object> builder;
        if (factoryMethods.isEmpty()) {
            executable = constructors.get(0);
            builder = () -> buildFromConstructor((Constructor<?>) executable);
        } else {
            executable = factoryMethods.get(0);
            builder = () -> buildFromMethod((Method) executable);
        }
        return new Dependency(
                clazz, builder,
                executable.isAnnotationPresent(Factory.class) ? executable.getAnnotation(Factory.class).value() : 10);
    }

    public static <C extends Class<?>> List<Dependency> getFromConfiguration(C clazz) {
        return Arrays.stream(clazz.getDeclaredMethods())
                .filter(method -> method.isAnnotationPresent(Factory.class))
                .map(method -> new Dependency(
                                method.getReturnType(),
                                () -> buildFromMethod(method),
                                method.getAnnotation(Factory.class).value()))
                .toList();
    }

    private static Object buildFromMethod(Method method) throws InvocationTargetException, IllegalAccessException {
        Class<?> clazz = method.getDeclaringClass();
        if (!Modifier.isStatic(method.getModifiers()) || method.getParameterTypes().length != 0) {
            throw new InvalidFactoryException(clazz, method);
        } else if (!method.getReturnType().equals(clazz) && !clazz.isAnnotationPresent(DependencyConfiguration.class)) {
            throw new InvalidFactoryMethodTypeException(clazz, method);
        }
        method.setAccessible(true);
        return method.invoke(clazz);
    }

    private static <C extends Class<?>> Object buildFromConstructor(Constructor<?> constructor) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        if (constructor.getParameterTypes().length != 0) {
            throw new InvalidFactoryException(constructor.getDeclaringClass());
        }
        constructor.setAccessible(true);
        return constructor.newInstance();
    }

    @Override
    public int compareTo(Dependency o) {
        return order - o.order;
    }

    public Object build() {
        try {
            return builder.get();
        } catch (Exception e) {
            e.printStackTrace();
            throw new DependencyInitializationException(type);
        }
    }
}