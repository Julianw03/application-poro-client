package com.iambadatplaying.data.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.Util;
import com.iambadatplaying.data.DataManager;
import com.iambadatplaying.lcuHandler.ConnectionManager;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChallengeSummaryDataManager extends MapDataManager<String> {

    private static final Pattern lolChallengeSummaryPattern = Pattern.compile("/lol-challenges/v1/summary-player-data/player/([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})");


    public ChallengeSummaryDataManager(Starter starter) {
        super(starter);
    }

    @Override
    public Optional<JsonObject> getExternal(String key) {
        return get(key);
    }

    @Override
    public Optional<JsonObject> doLoad(String key) {
        JsonObject data = ConnectionManager.getResponseBodyAsJsonObject(starter.getConnectionManager().buildConnection(ConnectionManager.conOptions.GET, "/lol-challenges/v1/summary-player-data/player/" + key));
        log("Loaded challenge data for " + key, Starter.LOG_LEVEL.DEBUG);
        log(data, Starter.LOG_LEVEL.DEBUG);
        if (data == null) return Optional.empty();
        if (data.has("errorCode")) return Optional.empty();
        sendKeyUpdate(key);
        return Optional.of(data);
    }

    @Override
    public String getMapEventName() {
        return DataManager.UPDATE_TYPE.MAP_CHALLENGE_SUMMARY.name();
    }

    @Override
    public String getKeyEventName() {
        return DataManager.UPDATE_TYPE.SINGLE_CHALLENGE_SUMMARY.name();
    }

    @Override
    protected void doInitialize() {

    }

    @Override
    protected Matcher getURIMatcher(String uri) {
        return lolChallengeSummaryPattern.matcher(uri);
    }

    @Override
    protected void doUpdateAndSend(Matcher uriMatcher, String type, JsonElement data) {
        String puuid = uriMatcher.group(1);
        switch (type) {
            case UPDATE_TYPE_DELETE:
                map.remove(puuid);
                sendKeyUpdate(puuid, null);
                break;
            case UPDATE_TYPE_CREATE:
            case UPDATE_TYPE_UPDATE:
                final JsonObject challengeData = data.getAsJsonObject();
                final JsonObject existingData = map.get(puuid);
                if (Util.equalJsonElements(existingData, data)) return;
                map.put(puuid, challengeData);
                sendKeyUpdate(puuid, challengeData);
                break;
            default:
                break;
        }
    }

    private JsonObject singlePuuidUpdateObject(String puuid, JsonObject data) {
        JsonObject updateObject = new JsonObject();
        updateObject.addProperty("puuid", puuid);
        updateObject.add("challengeSummary", data);
        return updateObject;
    }

    @Override
    protected void doShutdown() {
    }
}
