package dev.igorekudashev.dependencyinjector.exceptions;


public class DependencyInitializationException extends RuntimeException {

    public DependencyInitializationException(Class clazz) {
        super(String.format("Unknown error during dependency initialization '%s'", clazz.getName()));
    }

}
