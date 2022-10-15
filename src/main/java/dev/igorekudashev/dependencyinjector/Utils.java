package dev.igorekudashev.dependencyinjector;

import lombok.SneakyThrows;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Utils {

    public static Set<Class<?>> getClasses(ClassLoader classLoader, String packageName) throws ClassNotFoundException, IOException {
        String path = packageName.replace('.', '/');
        Iterator<URL> iterator = classLoader.getResources(path).asIterator();
        Iterable<URL> iterable = () -> iterator;
        return StreamSupport.stream(iterable.spliterator(), false)
                .map(URL::getFile)
                .map(File::new)
                .flatMap(directory -> findClasses(directory, packageName).stream())
                .collect(Collectors.toSet());
    }

    @SneakyThrows
    public static Set<Class<?>> findClasses(File directory, String packageName) {
        Set<Class<?>> classes = new HashSet<>();
        File[] files = directory.listFiles();
        if (directory.exists() && files != null) {
            for (File file : files) {
                String fullFileName = packageName + "." + file.getName();
                if (file.isDirectory()) {
                    classes.addAll(findClasses(file, fullFileName));
                } else if (file.getName().endsWith(".class")) {
                    classes.add(Class.forName(fullFileName.substring(0, fullFileName.length() - 6)));
                }
            }
        }
        return classes;
    }
}