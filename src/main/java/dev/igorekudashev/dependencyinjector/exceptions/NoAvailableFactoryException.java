package dev.igorekudashev.dependencyinjector.exceptions;


public class NoAvailableFactoryException extends RuntimeException {

    public NoAvailableFactoryException(Class<?> clazz) {
        super(String.format("Class '%s' doesn't contain an annotated factory method or constructor to generate the dependency", clazz.getName()));
    }

}
