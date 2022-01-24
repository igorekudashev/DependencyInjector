package dev.igorekudashev.dependencyinjector;


import dev.igorekudashev.dependencyinjector.annotations.Factory;
import dev.igorekudashev.dependencyinjector.exceptions.DependencyInitializationException;
import dev.igorekudashev.dependencyinjector.exceptions.InvalidFactoryException;
import dev.igorekudashev.dependencyinjector.exceptions.InvalidFactoryMethodType;
import dev.igorekudashev.dependencyinjector.exceptions.NoAvailableFactoryException;
import dev.igorekudashev.dependencyinjector.exceptions.TooManyFactoriesException;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Context {

    private final Map<Class, Object> contextMap = new HashMap<>();

    public Object get(Class clazz) {
        if (!contextMap.containsKey(clazz)) {
            List<Method> factoryMethods = Arrays.stream(clazz.getDeclaredMethods())
                    .filter(method -> method.getAnnotation(Factory.class) != null)
                    .collect(Collectors.toList());
            List<Constructor> constructors = Arrays.stream(clazz.getDeclaredConstructors())
                    .filter(constructor -> constructor.getAnnotation(Factory.class) != null)
                    .collect(Collectors.toList());
            if (factoryMethods.size() + constructors.size() > 1) {
                throw new TooManyFactoriesException(clazz);
            } else if (factoryMethods.size() + constructors.size() == 0) {
                throw new NoAvailableFactoryException(clazz);
            }
            try {
                if (!factoryMethods.isEmpty()) {
                    createFromFactoryMethod(clazz, factoryMethods.get(0));
                } else {
                    createFromConstructor(clazz, constructors.get(0));
                }
            } catch (InvocationTargetException | InstantiationException | IllegalAccessException e) {
                throw new DependencyInitializationException(clazz);
            }
        }
        return contextMap.get(clazz);
    }

    private void createFromFactoryMethod(Class clazz, Method method) throws InvocationTargetException, IllegalAccessException {
        if (!Modifier.isStatic(method.getModifiers()) || method.getParameterTypes().length != 0) {
            throw new InvalidFactoryException(clazz, method);
        } else if (!method.getReturnType().equals(clazz)) {
            throw new InvalidFactoryMethodType(clazz, method);
        }
        method.setAccessible(true);
        contextMap.put(clazz, method.invoke(clazz));
    }

    private void createFromConstructor(Class clazz, Constructor constructor) throws InvocationTargetException, InstantiationException, IllegalAccessException {
        if (constructor.getParameterTypes().length != 0) {
            throw new InvalidFactoryException(clazz);
        }
        constructor.setAccessible(true);
        contextMap.put(clazz, constructor.newInstance());
    }
}
