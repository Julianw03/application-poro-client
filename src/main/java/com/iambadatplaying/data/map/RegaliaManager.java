package com.iambadatplaying.data.map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.Util;
import com.iambadatplaying.data.DataManager;
import com.iambadatplaying.lcuHandler.ConnectionManager;

import java.math.BigInteger;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegaliaManager extends MapDataManager<BigInteger> {

    private final static Pattern lolRegaliaV2SummonerPattern = Pattern.compile("/lol-regalia/v2/summoners/(.*)/regalia/async");

    public RegaliaManager(Starter starter) {
        super(starter);
    }

    @Override
    public Optional<JsonObject> doLoad(BigInteger key) {
        JsonObject regalia = ConnectionManager.getResponseBodyAsJsonObject(starter.getConnectionManager().buildConnection(ConnectionManager.conOptions.GET, "/lol-regalia/v3/summoners/" + key.toString() + "/regalia"));
        if (regalia == null) return Optional.empty();
        if (regalia.has("errorCode")) return Optional.empty();
        return Optional.of(regalia);
    }

    @Override
    public String getMapEventName() {
        return DataManager.UPDATE_TYPE.MAP_REGALIA.name();
    }

    @Override
    public String getKeyEventName() {
        return DataManager.UPDATE_TYPE.SINGLE_REGALIA.name();
    }

    @Override
    public void doInitialize() {
    }

    protected Matcher getURIMatcher(String uri) {
        return lolRegaliaV2SummonerPattern.matcher(uri);
    }

    @Override
    protected void doUpdateAndSend(Matcher uriMatcher, String type, JsonElement data) {
        String summonerIdStr = uriMatcher.replaceAll("$1");
        BigInteger summonerId = new BigInteger(summonerIdStr);
        switch (type) {
            case UPDATE_TYPE_DELETE:
                map.remove(summonerId);
                sendKeyUpdate(summonerId, null);
                break;
            case UPDATE_TYPE_CREATE:
            case UPDATE_TYPE_UPDATE:
                Optional<JsonObject> getOpt = doLoad(summonerId);
                if (!getOpt.isPresent()) return;
                JsonObject regalia = getOpt.get();
                if (Util.equalJsonElements(regalia, data)) return;
                map.put(summonerId, regalia);
                sendKeyUpdate(summonerId, regalia);
                break;
        }
    }

    @Override
    public void doShutdown() {
    }

    @Override
    public Optional<JsonObject> getExternal(String key) {
        if (key == null) return Optional.empty();
        if (key.isEmpty()) return Optional.empty();
        try {
            BigInteger summonerId = new BigInteger(key);
            return get(summonerId);
        } catch (Exception e) {}
        return Optional.empty();
    }

}
