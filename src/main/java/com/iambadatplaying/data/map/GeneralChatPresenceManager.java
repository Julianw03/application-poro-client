package com.iambadatplaying.data.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.Util;
import com.iambadatplaying.data.DataManager;
import com.iambadatplaying.lcuHandler.ConnectionManager;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GeneralChatPresenceManager extends MapDataManager<String> {

    //Used for capturing this /lol-chat/v1/conversations/{conversationId}/participants/{participantId}$
    private static final Pattern chatPresenceManger   = Pattern.compile("/lol-chat/v1/conversations/(([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})(%40|@)\\S{2,}\\.pvp\\.net)/participants/(([0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12})(%40|@)\\S{2,}\\.pvp\\.net)$");
    private static final int     EXPECTED_GROUP_COUNT = 6;

    public GeneralChatPresenceManager(Starter starter) {
        super(starter);
    }

    @Override
    public Optional<JsonObject> doLoad(String key) {
        return Optional.empty();
    }

    @Override
    protected void doInitialize() {
        fetchPotentialLobbyPresence();
    }

    private void fetchPotentialLobbyPresence() {
        JsonObject optLobbyData = ConnectionManager.getResponseBodyAsJsonObject(starter.getConnectionManager().buildConnection(ConnectionManager.conOptions.GET, "/lol-lobby/v2/lobby"));
        if (optLobbyData == null) return;
        if (Util.jsonKeysPresent(optLobbyData,"errorCode")) return;
        if (!Util.jsonKeysPresent(optLobbyData,"mucJwtDto")) return;

        JsonObject chatData = optLobbyData.getAsJsonObject("mucJwtDto");
        String domain = chatData.get("domain").getAsString();
        String targetRegion = chatData.get("targetRegion").getAsString();
        String channelId = chatData.get("channelClaim").getAsString();

        StringBuilder sb = new StringBuilder();
        sb.append("/lol-chat/v1/conversations/");
        sb.append(channelId);
        sb.append("@");
        sb.append(domain);
        sb.append(".");
        sb.append(targetRegion);
        sb.append(".pvp.net/participants");

        log("Lobby detected, trying to fetch all members data from base url: " + sb.toString(), Starter.LOG_LEVEL.DEBUG);

        JsonArray participants = ConnectionManager.getResponseBodyAsJsonArray(starter.getConnectionManager().buildConnection(ConnectionManager.conOptions.GET, sb.toString()));
        if (participants == null) return;
        for (JsonElement participant: participants) {
            if (!participant.isJsonObject()) continue;
            JsonObject jsonObject = participant.getAsJsonObject();
            if (!jsonObject.has("puuid")) return;

            String puuid = jsonObject.get("puuid").getAsString();
            log("Adding info for: " + puuid);
            map.put(puuid, jsonObject);
        }
    }

    @Override
    protected Matcher getURIMatcher(String uri) {
        return chatPresenceManger.matcher(uri);
    }

    @Override
    protected void doUpdateAndSend(Matcher uriMatcher, String type, JsonElement data) {
        switch (type) {
            case UPDATE_TYPE_CREATE:
            case UPDATE_TYPE_UPDATE:
                if (uriMatcher.groupCount() != EXPECTED_GROUP_COUNT) return;
                if (!data.isJsonObject()) return;
                final String conversationId = uriMatcher.group(1);
                String memberPuuid = uriMatcher.group(5);
                JsonObject updatedData = data.getAsJsonObject();
                JsonObject currentEntry = map.get(memberPuuid);
                if (Util.equalJsonElements(currentEntry, updatedData)) return;
                log("Updated Presence for " + memberPuuid + " via " + conversationId);
                map.put(memberPuuid, updatedData);
                sendKeyUpdate(memberPuuid, updatedData);
                break;
            default:
                return;
        }
    }

    @Override
    protected void doShutdown() {

    }

    @Override
    public Optional<JsonObject> getExternal(String key) {
        if (key == null) return Optional.empty();
        return get(key);
    }

    @Override
    public String getMapEventName() {
        return DataManager.UPDATE_TYPE.MAP_GENERIC_PRESENCES.name();
    }

    @Override
    public String getKeyEventName() {
        return DataManager.UPDATE_TYPE.SINGLE_GENERIC_PRESENCE.name();
    }
}
