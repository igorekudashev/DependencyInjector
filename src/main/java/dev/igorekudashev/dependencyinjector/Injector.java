package dev.igorekudashev.dependencyinjector;


import dev.igorekudashev.dependencyinjector.annotations.StaticImport;
import dev.igorekudashev.dependencyinjector.exceptions.InvalidDependencyField;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.stream.Collectors;

public class Injector {

    private final String rootPackageName;

    private final Context context = new Context();

    public Injector(String rootPackageName) {
        this.rootPackageName = rootPackageName;
        inject();
    }

    public Injector(Class clazz) {
        this(clazz.getPackageName());
    }

    public Injector(Package pack) {
        this(pack.getName());
    }

    private void load(Class clazz) {
        getFieldsForInject(clazz).forEach(field -> {
            if (Modifier.isStatic(field.getModifiers())) {
                try {
                    field.set(clazz, context.get(field.getType()));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            } else {
                throw new InvalidDependencyField(clazz, field);
            }
        });
    }

    private void inject() {
        try {
            getClasses().forEach(this::load);
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private List<Class> getClasses() throws ClassNotFoundException, IOException {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        String path = rootPackageName.replace('.', '/');
        Enumeration<URL> resources = classLoader.getResources(path);
        List<File> dirs = new ArrayList<>();
        while (resources.hasMoreElements()) {
            URL resource = resources.nextElement();
            dirs.add(new File(resource.getFile()));
        }
        ArrayList<Class> classes = new ArrayList<>();
        for (File directory : dirs) {
            classes.addAll(findClasses(directory, rootPackageName));
        }
        return classes;
    }

    private List<Class> findClasses(File directory, String packageName) throws ClassNotFoundException {
        List<Class> classes = new ArrayList<>();
        if (!directory.exists()) {
            return classes;
        }
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                classes.addAll(findClasses(file, packageName + "." + file.getName()));
            } else if (file.getName().endsWith(".class")) {
                classes.add(Class.forName(packageName + '.' + file.getName().substring(0, file.getName().length() - 6)));
            }
        }
        return classes;
    }

    private List<Field> getFieldsForInject(Class clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(field -> field.getAnnotation(StaticImport.class) != null)
                .peek(field -> field.setAccessible(true))
                .collect(Collectors.toList());
    }
}
