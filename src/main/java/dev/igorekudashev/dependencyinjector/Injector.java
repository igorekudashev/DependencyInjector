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
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.stream.Collectors;

public class Injector {

    private static final Queue<Dependency> dependencies = new PriorityQueue<>();

    public static void addDependency(Object object) {
        addDependency(object, 0);
    }

    public static void addDependency(Object object, int order) {
        dependencies.offer(Dependency.getFromObject(object, order));
    }

    public static void inject(Class clazz) {
        inject(clazz.getPackageName());
    }

    public static void inject(Package pack) {
        inject(pack.getName());
    }

    public static void inject(String rootPackageName) {
        try {
            Map<Class, List<Field>> injectFields = new HashMap<>();
            Utils.getClasses(rootPackageName).forEach(clazz -> {
                Dependency dependency = Dependency.getFromClass(clazz);
                if (dependency != null) {
                    dependencies.offer(dependency);
                } else {
                    getFieldsForInject(clazz).forEach(field -> {
                        injectFields.putIfAbsent(field.getType(), new ArrayList<>());
                        injectFields.get(field.getType()).add(field);
                    });
                }
            });
            while (dependencies.peek() != null) {
                Dependency dependency = dependencies.poll();
                if (injectFields.containsKey(dependency.getDependencyClass())) {
                    injectFields.remove(dependency.getDependencyClass()).forEach(field -> {
                        if (Modifier.isStatic(field.getModifiers())) {
                            try {
                                field.set(field.getDeclaringClass(), dependency.buildObject());
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
    }

    private static List<Field> getFieldsForInject(Class clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(StaticImport.class))
                .peek(field -> field.setAccessible(true))
                .collect(Collectors.toList());
    }
}
