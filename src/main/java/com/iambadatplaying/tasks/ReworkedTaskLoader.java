package com.iambadatplaying.tasks;

import com.iambadatplaying.Starter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ReworkedTaskLoader {
    private final Starter starter;
    private       Path    taskDirPath;

    public ReworkedTaskLoader(Starter starter) {
        this.starter = starter;
    }

    public void setTaskDirectory(Path taskDirPath) {
        this.taskDirPath = taskDirPath;
    }

    public void init() {
        if (taskDirPath == null) {
            log("Task directory not set.", Starter.LOG_LEVEL.ERROR);
            return;
        }

        if (!Files.exists(taskDirPath)) {
            log(taskDirPath.toString() + " does not exist.", Starter.LOG_LEVEL.ERROR);
            return;
        }

        if (!Files.isDirectory(taskDirPath)) {
            log("Task directory is not a directory.", Starter.LOG_LEVEL.ERROR);
            return;
        }

        File[] taskFiles = taskDirPath.toFile().listFiles();
        if (taskFiles == null) {
            log("Failed to list files in task directory.", Starter.LOG_LEVEL.ERROR);
            return;
        }

        for (File taskFile : taskFiles) {
            if (taskFile.isFile()) {
                if (taskFile.getName().endsWith(".class")) {
                    log("Attempting to load task: " + taskFile.getName(), Starter.LOG_LEVEL.INFO);
                    loadTask(taskFile.toPath());
                } else if (taskFile.getName().endsWith(".java")) {
                    log("Skipping: " + taskFile.getName(), Starter.LOG_LEVEL.INFO);
                } else {
                    log("Ignoring file: " + taskFile.getName(), Starter.LOG_LEVEL.INFO);
                }
            }
        }
    }

    public void loadTask(Path taskClassPath) {
        File classFile = taskClassPath.toFile();
        CustomClassLoader classLoader = new CustomClassLoader();
        Class<?> taskClass;
        try {
            taskClass = classLoader.loadClass(classFile);
        } catch (Exception e) {
            log("Failed to load task class from file: " + classFile.getName(), Starter.LOG_LEVEL.ERROR);
            return;
        }

        Object taskInstance;
        try {
            taskInstance = taskClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            log("Failed to instantiate task class from file: " + classFile.getName(), Starter.LOG_LEVEL.ERROR);
            return;
        }

        if (taskInstance instanceof Task) {
            Task task = (Task) taskInstance;
            starter.getTaskManager().addAvailableTask(task.getClass());
        } else {
            log("The class in file: " + classFile.getName() + " does not implement Task interface.", Starter.LOG_LEVEL.ERROR);
        }
    }

    private void log(String s, Starter.LOG_LEVEL level) {
        starter.log(this.getClass().getSimpleName() + ": " + s, level);
    }

    private void log(String s) {
        log(s, Starter.LOG_LEVEL.DEBUG);
    }

    private static class CustomClassLoader extends ClassLoader {
        public Class<?> loadClass(File classFile) throws ClassNotFoundException, IOException {
            byte[] classBytes = Files.readAllBytes(classFile.toPath());
            return defineClass(null, classBytes, 0, classBytes.length);
        }
    }
}
