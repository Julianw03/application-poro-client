package com.iambadatplaying.tasks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.tasks.impl.*;
import org.eclipse.jetty.util.UrlEncoded;

import java.net.URLEncoder;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ReworkedTaskManager {

    private final static HashMap<String, Class<? extends Task>> ALL_AVAILABLE_TASKS_LIST = new HashMap<>();

    public static String getTaskKey(Task task) {
        if (task == null) return null;
        return getTaskKey(task.getClass());
    }

    public static String getTaskKey(Class<? extends Task> taskClass) {
        if (taskClass == null) return null;
        return URLEncoder.encode(taskClass.getName().toLowerCase());
    }

    private final Starter                         starter;
    private final ConcurrentHashMap<String, Task> runningTaskMap = new ConcurrentHashMap<>();

    private boolean running = false;

    private Path taskDirPath;
    private ReworkedTaskLoader taskLoader;

    public ReworkedTaskManager(Starter starter) {
        this.starter = starter;
    }

    public Path getTaskDir() {
        return taskDirPath;
    }

    public boolean isRunning() {
        return running;
    }


    public void init() {
        ALL_AVAILABLE_TASKS_LIST.clear();
        this.runningTaskMap.clear();
        this.taskDirPath = starter.getTaskPath();
        loadDefaultTasks();
        this.taskLoader = new ReworkedTaskLoader(starter);
        taskLoader.setTaskDirectory(taskDirPath);
        taskLoader.init();
        running = true;
        log("Initialized");
    }

    public void loadDefaultTasks() {
        addAvailableTask(FriendOnlineNotifier.class);
        addAvailableTask(SuppressUx.class);
    }

    public synchronized void shutdown() {
        running = false;
        if (!runningTaskMap.isEmpty()) {
            for (Task task : runningTaskMap.values()) {
                if (task != null) {
                    task.shutdown();
                }
            }
        }
        runningTaskMap.clear();
        ALL_AVAILABLE_TASKS_LIST.clear();
    }

    public JsonArray getTaskList() {
        JsonArray taskList = new JsonArray();
        for (Class<? extends Task> clazz : ALL_AVAILABLE_TASKS_LIST.values()) {
            if (clazz == null) continue;
            JsonObject task = new JsonObject();
            task.addProperty(Task.KEY_TASKLIST_BACKEND_KEY, getTaskKey(clazz));
            task.addProperty(Task.KEY_TASKLIST_NAME, clazz.getSimpleName());
            taskList.add(task);
        }
        return taskList;
    }

    public void addAvailableTask(Class<? extends Task> taskClass) {
        if (taskClass == null) return;
        final String taskKey = getTaskKey(taskClass);
        ALL_AVAILABLE_TASKS_LIST.compute(taskKey, (key, prevTaskClass) -> {
         if (prevTaskClass != null) {
             removeAvailableTask(taskKey);
         }
         return taskClass;
        });
    }

    private void removeAvailableTask(String taskKey) {
        if (taskKey == null) return;
        ALL_AVAILABLE_TASKS_LIST.remove(taskKey);
        runningTaskMap.computeIfPresent(taskKey, ((key, runningTask) -> {
            runningTask.shutdown();
            return null;
        }));
    }

    private Optional<? extends Task> getTaskInstance(Class<? extends Task> taskClass) {
        try {
            final Task task = taskClass.getDeclaredConstructor().newInstance();
            return Optional.of(task);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public boolean startOrChangeTask(String taskKey, JsonObject arguments) {
        if (taskKey == null) return false;
        final Class<? extends Task> taskClass = ALL_AVAILABLE_TASKS_LIST.get(taskKey);
        if (taskClass == null) return false;
        final Optional<? extends Task> optTask = getTaskInstance(taskClass);
        if (!optTask.isPresent()) return false;
        final Task newMappedTask = optTask.get();
        newMappedTask.setMainInitiator(starter);
        if (!newMappedTask.setTaskArgs(arguments)) {
            return false;
        }
        runningTaskMap.compute(taskKey, (key, prevTask) -> {
            if (prevTask != null) {
                prevTask.shutdown();
            }
            newMappedTask.init();
            return newMappedTask;
        });
        return true;
    }

    public boolean stopTask(String taskKey) {
        final Task currentTask = runningTaskMap.get(taskKey);
        if (currentTask == null) return false;
        currentTask.shutdown();
        runningTaskMap.remove(taskKey);
        return true;
    }

    public void updateAllTasks(String type, String uri, JsonElement data) {
        if (type == null || uri == null || data == null) return;
        for (Task task : runningTaskMap.values()) {
            try {
                task.notify(type, uri, data);
            } catch (Exception e) {
                log(e.getMessage(), Starter.LOG_LEVEL.ERROR);
            }
        }
    }
    public ReworkedTaskLoader getTaskLoader() {
        return taskLoader;
    }

    public Task getRunningTask(String taskKey) {
        return runningTaskMap.get(taskKey);
    }

    public Class<? extends Task> getTaskClass(String taskKey) {
        return ALL_AVAILABLE_TASKS_LIST.get(taskKey);
    }

    private void log(String s, Starter.LOG_LEVEL level) {
        starter.log(this.getClass().getSimpleName() + ": " + s, level);
    }

    private void log(String s) {
        starter.log(this.getClass().getSimpleName() + ": " + s);
    }
}
