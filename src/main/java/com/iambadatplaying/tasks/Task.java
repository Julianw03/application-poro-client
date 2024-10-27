package com.iambadatplaying.tasks;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;

import java.util.Optional;


/**
 * @author IAmBadAtPlaying
 */
public abstract class Task {

    protected Starter starter;
    protected boolean running = false;

    public static final String METHOD_GET_TASK_ARGUMENTS = "getTaskArguments";
    public static final String METHOD_GET_TASK_DESCRIPTION = "getDescription";
    public static final String METHOD_GET_TASK_NAME = "getName";

    public static final String KEY_TASKLIST_BACKEND_KEY = "taskKey";
    public static final String KEY_TASKLIST_NAME        = "name";

    public static final String KEY_TASK_NAME           = "name";
    public static final String KEY_TASK_ARGUMENTS      = "arguments";
    public static final String KEY_TASK_CURRENT_VALUES = "currentValues";
    public static final String KEY_TASK_RUNNING        = "running";
    public static final String KEY_TASK_DESCRIPTION    = "description";

    public static final String KEY_TASK_ARGUMENT_DISPLAY_NAME  = "displayName";
    public static final String KEY_TASK_ARGUMENT_BACKEND_KEY   = "backendKey";
    public static final String KEY_TASK_ARGUMENT_TYPE          = "type";
    public static final String KEY_TASK_ARGUMENT_REQUIRED      = "required";
    public static final String KEY_TASK_ARGUMENT_DESCRIPTION   = "description";

    public static final String KEY_TASKS_ADDITIONAL_DATA = "additionalData";


    private static final String DEFAULT_DESCRIPTION = "No description available";

    public static JsonArray getTaskArguments() {
        return new JsonArray();
    }

    public static String getTaskName(Class<? extends  Task> clazz) {
        if (clazz == null) return "Unknown";
        try {
            return (String) clazz.getMethod(METHOD_GET_TASK_NAME).invoke(null);
        } catch (Exception e) {}
        return clazz.getSimpleName();
    }

    public static String getTaskDescription(Class<? extends  Task> clazz) {
        if (clazz == null) return DEFAULT_DESCRIPTION;
        try {
            return (String) clazz.getMethod(METHOD_GET_TASK_DESCRIPTION).invoke(null);
        } catch (Exception e) {}
        return DEFAULT_DESCRIPTION;
    }

    public static JsonArray getTaskArguments(Class<? extends  Task> clazz) {
        if (clazz == null) return new JsonArray();
        try {
            return (JsonArray) clazz.getMethod(METHOD_GET_TASK_ARGUMENTS).invoke(null);
        } catch (Exception e) {}
        return new JsonArray();
    }

    protected Task() {
    }


    protected Task(Starter starter) {
        this.starter = starter;
    }

    /**
     * Method to receive LCU updates
     *
     * @param type Type of the update
     * @param uri  The uri that is affected by the update
     * @param data The updated data
     */
    public abstract void notify(String type, String uri, JsonElement data);

    /**
     * This should set the MainInitiator if not already set in Constructor. MUST be called before {@link #init()}
     */

    public void setMainInitiator(Starter starter) {
        if (this.starter != null) return;
        this.starter = starter;
    }

    /**
     * This will call {@link #doInitialize()} if the MainInitiator is set and running. If not it returns.
     */

    public final void init() {
        if (starter == null || !starter.isInitialized()) {
            System.out.println("Task cant initialize, MainInitiator is null or not running");
        }
        doInitialize();
        this.running = true;
    }

    /**
     * This will initialize the task. Called internally by {@link #init()}
     */
    protected abstract void doInitialize();

    /**
     * This will set running to false, call {@link #doShutdown()} and then set the mainInitiator to null.
     */
    public final void shutdown() {
        this.running = false;
        doShutdown();
        this.starter = null;
    }

    /**
     * This will shutdown the task. Used internally.
     *
     * @see #shutdown()
     */
    protected abstract void doShutdown();

    /**
     * @param arguments The arguments in json format to be passed and saved.
     */
    public boolean setTaskArgs(JsonObject arguments) {
        return true;
    }

    /**
     * @return The associated backendKeys and their values
     */
    public JsonObject getCurrentValues() {
        return new JsonObject();
    }

    /**
     * @return Whether the task is running or not
     */
    public boolean isRunning() {
        return this.running;
    }

    protected final void log(String s, Starter.LOG_LEVEL level) {
        Optional.ofNullable(starter).ifPresent(starter -> starter.log(this.getClass().getName() + ": " + s, level));
    }

    protected final void log(String s) {
        Optional.ofNullable(starter).ifPresent(starter -> starter.log(this.getClass().getName() + ": " + s));
    }
}
