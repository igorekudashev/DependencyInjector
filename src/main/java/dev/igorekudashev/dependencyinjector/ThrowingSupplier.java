package dev.igorekudashev.dependencyinjector;


@FunctionalInterface
public interface ThrowingSupplier<T> {
    T get() throws Exception;
}
