package com.iambadatplaying.data.map;

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

public class FriendHovercardManager extends MapDataManager<String> {

    private static final Pattern friendHovercardPattern = Pattern.compile("/lol-hovercard/v1/friend-info/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})");

    public FriendHovercardManager(Starter starter) {
        super(starter);
    }

    @Override
    protected void doInitialize() {

    }

    @Override
    protected Matcher getURIMatcher(String uri) {
        return friendHovercardPattern.matcher(uri);
    }

    @Override
    protected void doUpdateAndSend(Matcher uriMatcher, String type, JsonElement data) {
        switch (type) {
            case UPDATE_TYPE_UPDATE:
            case UPDATE_TYPE_CREATE:
                if (!data.isJsonObject()) return;
                final JsonObject object = data.getAsJsonObject();
                if (!Util.jsonKeysPresent(object, "id")) return;
                String key = object.get("id").getAsString();
                log("Updating friend hovercard with key " + key, Starter.LOG_LEVEL.DEBUG);
                log(object, Starter.LOG_LEVEL.DEBUG);
                final JsonObject oldObject = map.get(key);
                map.put(key, object);
                sendKeyUpdate(key, object);
                break;
            case UPDATE_TYPE_DELETE:
                String keyToDelete = uriMatcher.group(1);
                map.keySet()
                        .stream()
                        .filter(puuid -> puuid.startsWith(keyToDelete))
                        .findFirst()
                        .ifPresent(iterKey -> {
                            map.remove(iterKey);
                            sendKeyUpdate(iterKey, null);
                        });
                break;
        }
    }

    @Override
    protected void doShutdown() {

    }

    @Override
    public Optional<JsonObject> doLoad(String key) {
        HttpsURLConnection con = starter.getConnectionManager().buildConnection(ConnectionManager.conOptions.GET, "/lol-hovercard/v1/friend-info/" + key);
        JsonObject data = ConnectionManager.getResponseBodyAsJsonObject(con);
        if (data == null) return Optional.empty();
        if (!Util.jsonKeysPresent(data, "id")) return Optional.empty();
        return Optional.of(data);
    }

    @Override
    public String getMapEventName() {
        return DataManager.UPDATE_TYPE.MAP_FRIEND_HOVERCARDS.name();
    }

    @Override
    public String getKeyEventName() {
        return DataManager.UPDATE_TYPE.SINGLE_FRIEND_HOVERCARD.name();
    }

    @Override
    public Optional<JsonObject> getExternal(String key) {
        if (key == null) return Optional.empty();
        return get(key);
    }
}
