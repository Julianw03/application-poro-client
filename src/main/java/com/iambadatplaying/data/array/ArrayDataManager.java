package com.iambadatplaying.data.array;

import com.google.gson.JsonArray;
import com.iambadatplaying.Starter;
import com.iambadatplaying.data.BasicDataManager;
import com.iambadatplaying.data.DataManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public abstract class ArrayDataManager extends BasicDataManager {

    protected JsonArray currentArray = null;
    protected ArrayDataManager(Starter starter) {
        super(starter);
    }

    public Optional<JsonArray> getCurrentState() {
        if (!initialized) {
            log("Not initialized, wont fetch current state", Starter.LOG_LEVEL.ERROR);
            return Optional.empty();
        }
        if (currentArray != null) return Optional.of(currentArray);
        Optional<JsonArray> newState = fetchCurrentState();
        newState.ifPresent(jsonArray -> currentArray = jsonArray);
        return newState;
    }

    public void setCurrentState(JsonArray currentState) {
        this.currentArray = currentState;
    }

    protected abstract Optional<JsonArray> fetchCurrentState();

    @Override
    public void sendCurrentState() {
        getCurrentState().ifPresent(
                state ->
                        starter.getServer().sendToAllSessions(DataManager.getEventDataString(getEventName(), state))
        );
    }

    @Override
    public void shutdown() {
        if (!initialized) return;
        initialized = false;
        doShutdown();
        currentArray = null;
    }

    public void resetState() {
        if (!initialized) {
            log("Not initialized, wont have any effect", Starter.LOG_LEVEL.WARN);
            return;
        }
        currentArray = new JsonArray();
        sendCurrentState();
    }

    public abstract String getEventName();
}
