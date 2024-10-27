package com.iambadatplaying.frontendHandler;

import com.google.gson.JsonObject;
import com.iambadatplaying.ConnectionStatemachine;
import com.iambadatplaying.Starter;
import com.iambadatplaying.data.DataManager;

public class FrontendMessageHandler {

    private final Starter starter;

    public FrontendMessageHandler(Starter starter) {
        this.starter = starter;
    }


    public void sendInitialData(LocalWebSocket localWebSocket) {
        starter.getDataManager().sendInitialDataBlocking(localWebSocket);
        sendInitialUpdatesDone(localWebSocket);
    }

    public void sendCurrentState(LocalWebSocket localWebSocket) {
        ConnectionStatemachine csm = starter.getConnectionStatemachine();
        JsonObject newStateObject = new JsonObject();
        newStateObject.addProperty("state", csm.getCurrentState().name());
        localWebSocket.sendMessage(DataManager.getEventDataString(DataManager.UPDATE_TYPE.INTERNAL_STATE.name(), newStateObject));
    }

    private void sendInitialUpdatesDone(LocalWebSocket localWebSocket) {
        JsonObject data = new JsonObject();
        data.addProperty("done", true);
        localWebSocket.sendMessage(DataManager.getEventDataString(DataManager.UPDATE_TYPE.ALL_INITIAL_DATA_LOADED.toString(), data));
    }

    private void log(String s, Starter.LOG_LEVEL level) {
        starter.log(this.getClass().getSimpleName() + ": " + s, level);
    }

    private void log(String s) {
        log(s, Starter.LOG_LEVEL.DEBUG);
    }
}
