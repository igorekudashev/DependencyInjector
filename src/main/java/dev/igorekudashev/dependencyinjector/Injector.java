package dev.igorekudashev.dependencyinjector;


import dev.igorekudashev.dependencyinjector.annotations.StaticImport;
import dev.igorekudashev.dependencyinjector.exceptions.InvalidDependencyField;
import dev.igorekudashev.dependencyinjector.exceptions.NoAvailableFactoryException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Collectors;

public class Injector {

    private static ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private static final Set<Class<?>> classes = new HashSet<>();
    private static final Queue<Dependency> dependencies = new PriorityQueue<>();
    private static boolean logging = false;

    public static void addDependency(Object object) {
        addDependency(object, 0);
    }

    public static void addDependency(Object object, int order) {
        dependencies.offer(Dependency.getFromObject(object, order));
    }

    public static void inject(Class<?> clazz) {
        inject(clazz.getPackageName());
    }

    public static void inject(Package pack) {
        inject(pack.getName());
    }

    public static void inject(String rootPackageName) {
        log(String.format("Starting injection in package %s..", rootPackageName));
        try {
            Map<Class<?>, List<Field>> injectFields = new HashMap<>();
            classes.addAll(Utils.getClasses(classLoader, rootPackageName));
            classes.forEach(clazz -> {
                log(String.format("Parsing %s..", clazz.getName()));
                Dependency dependency = Dependency.getFromClass(clazz);
                if (dependency != null) {
                    dependencies.offer(dependency);
                    log(String.format("Dependency class %s loaded", clazz.getName()));
                }
                getFieldsForInject(clazz).forEach(field -> {
                    injectFields.putIfAbsent(field.getType(), new ArrayList<>());
                    injectFields.get(field.getType()).add(field);
                    log(String.format("Dependency field %s %s in class %s found", field.getType().getSimpleName(), field.getName(), clazz.getName()));
                });
            });
            while (dependencies.peek() != null) {
                Dependency dependency = dependencies.poll();
                if (injectFields.containsKey(dependency.getDependencyClass())) {
                    injectFields.remove(dependency.getDependencyClass()).forEach(field -> {
                        if (Modifier.isStatic(field.getModifiers())) {
                            try {
                                field.set(field.getDeclaringClass(), dependency.buildObject());
                                log(String.format("Dependency field %s %s in class %s injected", field.getType().getSimpleName(), field.getName(), field.getDeclaringClass().getName()));
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        } else {
                            throw new InvalidDependencyField(field.getDeclaringClass(), field);
                        }
                    });
                }
            }
            if (!injectFields.isEmpty()) {
                throw new NoAvailableFactoryException(injectFields.keySet().toArray(new Class[0])[0]);
            }
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        log("Injection completed!");
    }

    private static List<Field> getFieldsForInject(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(StaticImport.class))
                .peek(field -> field.setAccessible(true))
                .collect(Collectors.toList());
    }

    public static void setLogging(boolean logging) {
        Injector.logging = logging;
    }

    public static void setClassLoader(ClassLoader classLoader) {
        Injector.classLoader = classLoader;
    }

    public static void addClassesForLoad(Set<Class<?>> classes) {
        Injector.classes.addAll(classes);
    }

    static void log(String string) {
        if (logging) {
            System.out.println("[Injector] " + string);
        }
    }
}
