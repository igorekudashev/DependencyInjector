package dev.igorekudashev.dependencyinjector.exceptions;


import java.lang.reflect.Field;

public class InvalidDependencyField extends RuntimeException {

    public InvalidDependencyField(Class<?> clazz, Field field) {
        super(String.format("Dependency field '%s' in class '%s' must be static", field.getName(), clazz.getName()));
    }

}
