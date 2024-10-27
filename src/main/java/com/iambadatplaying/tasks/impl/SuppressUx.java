package com.iambadatplaying.tasks.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.lcuHandler.ConnectionManager;
import com.iambadatplaying.tasks.Task;

import java.util.Timer;
import java.util.TimerTask;

public class SuppressUx extends Task {

    private Timer timer;

    public static JsonArray getTaskArguments() {
        return new JsonArray();
    }

    public void notify(String type, String uri, JsonElement data) {
            if (!running || starter == null) return;
            if (!uri.startsWith("/riotclient/ux-state/request")) return;
            if (!data.isJsonObject()) return;
            handleUpdateData(data.getAsJsonObject());
    }

    private void handleUpdateData(JsonObject JsonData) {
        String eventType = JsonData.get("eventType").getAsString();
        System.out.println(JsonData);
        switch (eventType) {
            case "Create":
            case "Update":
                JsonObject data = JsonData.get("data").getAsJsonObject();
                handleUXUpdateJson(data);
                break;
            case "Delete":
            default:
                break;
        }
    }

    private void handleUXUpdateJson(JsonObject data) {
        String state = data.get("state").getAsString();
        switch (state) {
            case "ShowMain":
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        killUx();
                    }
                }, 400);
                break;
            default:
                break;
        }
    }

    private void confirmUXLoad() {
        starter.getConnectionManager().getResponse(ConnectionManager.responseFormat.RESPONSE_CODE, starter.getConnectionManager().buildConnection(ConnectionManager.conOptions.PUT, "/riotclient/ux-load-complete", ""));
    }

    private void killUx() {
        System.out.println("Killing UX");
        System.out.println(starter.getConnectionManager().getResponse(ConnectionManager.responseFormat.RESPONSE_CODE, starter.getConnectionManager().buildConnection(ConnectionManager.conOptions.POST, "/riotclient/kill-ux", "")));
    }

    private void showUx() {
        starter.getConnectionManager().getResponse(ConnectionManager.responseFormat.RESPONSE_CODE, starter.getConnectionManager().buildConnection(ConnectionManager.conOptions.POST, "/riotclient/launch-ux", ""));
    }

    @Override
    protected void doInitialize() {
        timer = new Timer();
        killUx();
    }

    @Override
    protected void doShutdown() {
        timer.cancel();
        timer = null;
        showUx();
    }

    @Override
    public boolean setTaskArgs(JsonObject arguments) {
        return true;
    }

    @Override
    public JsonObject getCurrentValues() {
        return new JsonObject();
    }
}
