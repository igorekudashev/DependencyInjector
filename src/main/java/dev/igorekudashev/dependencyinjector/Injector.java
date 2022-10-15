package dev.igorekudashev.dependencyinjector;


import dev.igorekudashev.dependencyinjector.annotations.DependencyConfiguration;
import dev.igorekudashev.dependencyinjector.annotations.StaticImport;
import dev.igorekudashev.dependencyinjector.exceptions.InvalidDependencyFieldException;
import dev.igorekudashev.dependencyinjector.exceptions.NoAvailableFactoryException;
import lombok.Setter;
import lombok.experimental.Accessors;

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
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

@Setter
@Accessors(chain = true)
public class Injector {

    private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private final Set<Class<?>> classes = new HashSet<>();
    private final Queue<Dependency> dependencies = new PriorityQueue<>();
    private Logger logger = Logger.getLogger("DependencyInjectorLogger");
    private boolean logging = false;

    public void addPreparedDependency(Object object) {
        addPreparedDependency(object, 0);
    }

    public void addPreparedDependency(Object object, int order) {
        dependencies.offer(Dependency.getFromPrepared(object, order));
    }

    public void addClassesForLoad(Set<Class<?>> classes) {
        this.classes.addAll(classes);
    }

    public void inject(Class<?> clazz) {
        inject(clazz.getPackageName());
    }

    public void inject(Package pack) {
        inject(pack.getName());
    }

    public void inject(String rootPackageName) {
        log(String.format("Starting injection in package %s..", rootPackageName));
        try {
            Map<Class<?>, List<Field>> injectFields = new HashMap<>();
            classes.addAll(Utils.getClasses(classLoader, rootPackageName));
            classes.forEach(clazz -> {
                log(String.format("Parsing %s..", clazz.getName()));
                if (clazz.isAnnotationPresent(DependencyConfiguration.class)) {
                    Dependency.getFromConfiguration(clazz).forEach(this::loadDependency);
                } else {
                    loadDependency(Dependency.getFromClass(clazz));
                }
                getFieldsForInject(clazz).forEach(field -> {
                    injectFields.putIfAbsent(field.getType(), new ArrayList<>());
                    injectFields.get(field.getType()).add(field);
                    log(String.format("Dependency field %s %s in class %s found", field.getType().getSimpleName(), field.getName(), clazz.getName()));
                });
            });
            while (dependencies.peek() != null) {
                Dependency dependency = dependencies.poll();
                if (injectFields.containsKey(dependency.getType())) {
                    injectFields.remove(dependency.getType()).forEach(field -> {
                        if (Modifier.isStatic(field.getModifiers())) {
                            try {
                                field.set(field.getDeclaringClass(), dependency.build());
                                log(String.format("Dependency field %s %s in class %s injected", field.getType().getSimpleName(), field.getName(), field.getDeclaringClass().getName()));
                            } catch (IllegalAccessException e) {
                                e.printStackTrace();
                            }
                        } else {
                            throw new InvalidDependencyFieldException(field.getDeclaringClass(), field);
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

    private void loadDependency(Dependency dependency) {
        if (dependency != null) {
            dependencies.offer(dependency);
            log(String.format("Dependency class %s loaded", dependency.getType().getName()));
        }
    }

    private List<Field> getFieldsForInject(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.isAnnotationPresent(StaticImport.class))
                .peek(field -> field.setAccessible(true))
                .collect(Collectors.toList());
    }

    private void log(String string) {
        if (logging) {
            logger.log(Level.INFO, "[Injector] " + string);
        }
    }
}