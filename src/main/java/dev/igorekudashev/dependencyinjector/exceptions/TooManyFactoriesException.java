package dev.igorekudashev.dependencyinjector.exceptions;


public class TooManyFactoriesException extends RuntimeException {

    public TooManyFactoriesException(Class<?> clazz) {
        super(String.format("Class '%s' has more than one annotated factory method or constructor", clazz.getName()));
    }

}
