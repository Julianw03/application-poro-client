package com.iambadatplaying.data.state;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.Util;
import com.iambadatplaying.data.DataManager;
import com.iambadatplaying.lcuHandler.ConnectionManager;

import javax.net.ssl.HttpsURLConnection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AccountLoadoutManager extends StateDataManager {

    private static final Pattern uriMatcher = Pattern.compile("/lol-loadouts/v4/loadouts/scope/account$");

    public AccountLoadoutManager(Starter starter) {
        super(starter);
    }

    @Override
    protected Optional<JsonObject> fetchCurrentState() {
        HttpsURLConnection con = starter.getConnectionManager().buildConnection(ConnectionManager.conOptions.GET, "/lol-loadouts/v4/loadouts/scope/account");
        return dataArrayToAccountObject(ConnectionManager.getResponseBodyAsJsonArray(con));
    }

    @Override
    public String getEventName() {
        return DataManager.UPDATE_TYPE.STATE_CURRENT_LOADOUT.name();
    }

    @Override
    protected void doInitialize() {

    }

    @Override
    protected Matcher getURIMatcher(String uri) {
        return uriMatcher.matcher(uri);
    }

    private Optional<JsonObject> dataArrayToAccountObject(JsonArray data) {
        if (data == null || data.isEmpty()) return Optional.empty();
        return Util.asJsonObject(data.get(0));
    }

    @Override
    protected void doUpdateAndSend(Matcher uriMatcher, String type, JsonElement data) {
        switch (type) {
            case UPDATE_TYPE_DELETE:
                break;
            case UPDATE_TYPE_UPDATE:
            case UPDATE_TYPE_CREATE:
                if (!data.isJsonArray()) return;
                dataArrayToAccountObject(data.getAsJsonArray()).ifPresent(updatedState -> {
                    if (Util.equalJsonElements(currentState, updatedState)) return;
                    this.currentState = updatedState;
                    sendCurrentState();
                });

        }
    }

    @Override
    protected void doShutdown() {

    }
}
