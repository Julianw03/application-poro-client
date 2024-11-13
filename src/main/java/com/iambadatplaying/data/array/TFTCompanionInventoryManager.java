package com.iambadatplaying.data.array;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.iambadatplaying.Starter;
import com.iambadatplaying.Util;
import com.iambadatplaying.data.DataManager;
import com.iambadatplaying.lcuHandler.ConnectionManager;

import javax.net.ssl.HttpsURLConnection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TFTCompanionInventoryManager extends ArrayDataManager {

    private static final Pattern uriPattern = Pattern.compile("/lol-inventory/v2/inventory/COMPANION$");

    public TFTCompanionInventoryManager(Starter starter) {
        super(starter);
    }

    @Override
    protected Optional<JsonArray> fetchCurrentState() {
        HttpsURLConnection con = starter.getConnectionManager().buildConnection(ConnectionManager.conOptions.GET, "/lol-inventory/v2/inventory/COMPANION");
        JsonArray data = ConnectionManager.getResponseBodyAsJsonArray(con);
        return Optional.ofNullable(data);
    }

    @Override
    public String getEventName() {
        return DataManager.UPDATE_TYPE.ARRAY_OWNED_TFT_COMPANIONS.name();
    }

    @Override
    protected void doInitialize() {

    }

    @Override
    protected Matcher getURIMatcher(String uri) {
        return uriPattern.matcher(uri);
    }

    @Override
    protected void doUpdateAndSend(Matcher uriMatcher, String type, JsonElement data) {
        switch (type) {
            case UPDATE_TYPE_DELETE:
                break;
            case UPDATE_TYPE_UPDATE:
            case UPDATE_TYPE_CREATE:
                if (!data.isJsonArray()) return;
                JsonArray updatedState = data.getAsJsonArray();
                if (Util.equalJsonElements(updatedState, currentArray)) return;
                currentArray = updatedState;
                sendCurrentState();
                break;
        }
    }

    @Override
    protected void doShutdown() {

    }
}
