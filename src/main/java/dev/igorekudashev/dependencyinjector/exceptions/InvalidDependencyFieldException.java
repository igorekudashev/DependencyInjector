package dev.igorekudashev.dependencyinjector.exceptions;


import java.lang.reflect.Field;

public class InvalidDependencyFieldException extends RuntimeException {

    public InvalidDependencyFieldException(Class<?> clazz, Field field) {
        super(String.format("Dependency field '%s' in class '%s' must be static", field.getName(), clazz.getName()));
    }

}
