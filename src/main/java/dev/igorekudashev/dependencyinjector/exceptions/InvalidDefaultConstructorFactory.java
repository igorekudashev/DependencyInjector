package dev.igorekudashev.dependencyinjector.exceptions;


public class InvalidDefaultConstructorFactory extends RuntimeException {

    public InvalidDefaultConstructorFactory(Class<?> clazz) {
        super(String.format("Class %s with @Factory annotation must only have default constructor", clazz.getName()));
    }
}
