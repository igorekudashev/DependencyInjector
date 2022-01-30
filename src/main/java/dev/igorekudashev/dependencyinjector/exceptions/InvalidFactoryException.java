package dev.igorekudashev.dependencyinjector.exceptions;


import java.lang.reflect.Method;

public class InvalidFactoryException extends RuntimeException {

    public InvalidFactoryException(Class<?> clazz, Method method) {
        super(String.format("Invalid factory method '%s' in class '%s'. Method must be static and without args", method.getName(), clazz.getName()));
    }

    public InvalidFactoryException(Class<?> clazz) {
        super(String.format("Invalid factory constructor in class '%s'. Constructor must be without args", clazz.getName()));
    }

}
