package dev.igorekudashev.dependencyinjector.exceptions;


import java.lang.reflect.Method;

public class InvalidFactoryMethodTypeException extends RuntimeException {

    public InvalidFactoryMethodTypeException(Class<?> clazz, Method method) {
        super(String.format("Factory method '%s' in class '%s' must return the same type as the class", method.getName(), clazz.getName()));
    }

}
