package dev.igorekudashev.dependencyinjector.exceptions;


public class InvalidDefaultConstructorFactoryException extends RuntimeException {

    public InvalidDefaultConstructorFactoryException(Class<?> clazz) {
        super(String.format("Class %s with @Factory annotation must only have default constructor", clazz.getName()));
    }
}
