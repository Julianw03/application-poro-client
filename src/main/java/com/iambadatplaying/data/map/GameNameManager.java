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

public class GameNameManager extends MapDataManager<String> {

    public GameNameManager(Starter starter) {
        super(starter);
    }

    @Override
    protected void doInitialize() {

    }

    @Override
    protected Matcher getURIMatcher(String uri) {
        return Util.UNMATCHABLE_PATTERN.matcher(uri);
    }

    @Override
    protected void doUpdateAndSend(Matcher uriMatcher, String type, JsonElement data) {

    }

    @Override
    protected void doShutdown() {

    }

    @Override
    public Optional<JsonObject> doLoad(String key) {
        if (key == null || key.isEmpty()) return Optional.empty();
        HttpsURLConnection con = starter.getConnectionManager().buildConnection(ConnectionManager.conOptions.GET, "/lol-summoner/v2/summoners/puuid/" + key);
        JsonObject data = ConnectionManager.getResponseBodyAsJsonObject(con);
        if (!data.has("errorCode")) return Optional.of(data);
        return Optional.empty();
    }

    @Override
    public String getMapEventName() {
        return DataManager.UPDATE_TYPE.MAP_GAME_NAMES.name();
    }

    @Override
    public String getKeyEventName() {
        return DataManager.UPDATE_TYPE.SINGLE_GAME_NAME.name();
    }

    @Override
    public Optional<JsonObject> getExternal(String key) {
        if (key == null) return Optional.empty();
        return get(key);
    }
}
