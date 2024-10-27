package com.iambadatplaying.tasks.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.tasks.ARGUMENT_TYPE;
import com.iambadatplaying.tasks.Task;
import com.iambadatplaying.tasks.builders.TaskArgumentBuilder;

public class TestTask extends Task {
    
    private static final String KEY_ARG1 = "arg1";
    
    private String arg1 = "Unknown";

    public static JsonArray getTaskArguments() {
        JsonArray requiredArgs = new JsonArray();

        requiredArgs.add(
                new TaskArgumentBuilder()
                        .setDisplayName(KEY_ARG1)
                        .setBackendKey(KEY_ARG1)
                        .setType(ARGUMENT_TYPE.TEXT)
                        .setRequired(true)
                        .setDescription("This is the first argument")
                        .build()
        );

        return requiredArgs;
    }

    public void notify(String type, String uri, JsonElement data) {
        log(type + " " + uri + ": " + data, Starter.LOG_LEVEL.INFO);
    }

    protected void doInitialize() {
    }

    public void doShutdown() {

    }

    public boolean setTaskArgs(JsonObject arguments) {
        try {
            arg1 = arguments.get(KEY_ARG1).getAsString();
            log("Modified Task-Args for Task " + this.getClass().getSimpleName(), Starter.LOG_LEVEL.DEBUG);
            return true;
        } catch (Exception e) {
            log("Failed to set Task-Args for Task " + this.getClass().getSimpleName(), Starter.LOG_LEVEL.ERROR);
        }
        return false;
    }

    @Override
    public JsonObject getCurrentValues() {
        JsonObject taskArgs = new JsonObject();
        taskArgs.addProperty(KEY_ARG1, arg1);
        return taskArgs;
    }
}
