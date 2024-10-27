package com.iambadatplaying.data.state;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.Util;
import com.iambadatplaying.data.DataManager;
import com.iambadatplaying.data.map.GameNameManager;
import com.iambadatplaying.lcuHandler.ConnectionManager;

import javax.net.ssl.HttpsURLConnection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EOGHonorManager extends StateDataManager {

    private static final Pattern HONOR_BALLOT_PATTERN = Pattern.compile("/lol-honor-v2/v1/ballot$");

    public EOGHonorManager(Starter starter) {
        super(starter);
    }

    @Override
    protected void doInitialize() {

    }

    @Override
    protected Matcher getURIMatcher(String uri) {
        return HONOR_BALLOT_PATTERN.matcher(uri);
    }

    @Override
    protected void doUpdateAndSend(Matcher uriMatcher, String type, JsonElement data) {
        switch (type) {
            case UPDATE_TYPE_CREATE:
            case UPDATE_TYPE_UPDATE:
                if (!data.isJsonObject()) return;
                Optional<JsonObject> updatedState = backendHonorToFrontendHonor(data.getAsJsonObject());
                if (!updatedState.isPresent()) return;
                JsonObject newState = updatedState.get();
                if (Util.equalJsonElements(newState, currentState)) return;
                currentState = newState;
                sendCurrentState();
                break;
            case UPDATE_TYPE_DELETE:
                resetState();
                break;
        }
    }

    private Optional<JsonObject> backendHonorToFrontendHonor(JsonObject data) {
        JsonObject frontendData = new JsonObject();
        Util.copyJsonAttributes(data, frontendData, "gameId");
        JsonArray eligiblePlayers;
        if (Util.jsonKeysPresent(data, "eligiblePlayers")) {
            eligiblePlayers = data.getAsJsonArray("eligiblePlayers");
        } else if (Util.jsonKeysPresent(data, "eligibleAllies")) {
            eligiblePlayers = data.getAsJsonArray("eligibleAllies");
        } else {
            return Optional.empty();
        }
        JsonArray feEligibleAllies = new JsonArray();
        for (int i = 0, size = eligiblePlayers.size(); i < size; i++) {
            JsonObject player = eligiblePlayers.get(i).getAsJsonObject();
            JsonObject fePlayer = new JsonObject();
            Util.copyJsonAttributes(player, fePlayer, "championName", "skinSplashPath", "summonerName", "puuid", "summonerId");
            String puuid = player.get("puuid").getAsString();
            starter.getDataManager()
                    .getMapManager(GameNameManager.class)
                    .get(puuid)
                    .ifPresent(
                            gameName ->
                                    fePlayer.addProperty(
                                            "gameName",
                                            gameName.get("gameName").getAsString()
                                    )
                    );
            feEligibleAllies.add(fePlayer);
        }
        if (Util.jsonKeysPresent(data, "eligibleOpponents")) {
            JsonArray eligibleOpponents = data.getAsJsonArray("eligibleOpponents");
            JsonArray feEligibleOpponents = new JsonArray();
            for (int i = 0, size = eligibleOpponents.size(); i < size; i++) {
                JsonObject opponent = eligibleOpponents.get(i).getAsJsonObject();
                JsonObject feOpponent = new JsonObject();
                Util.copyJsonAttributes(opponent, feOpponent, "championName", "skinSplashPath", "summonerName", "puuid", "summonerId");
                String puuid = opponent.get("puuid").getAsString();
                starter.getDataManager()
                        .getMapManager(GameNameManager.class)
                        .get(puuid)
                        .ifPresent(
                                gameName ->
                                        feOpponent.addProperty(
                                                "gameName",
                                                gameName.get("gameName").getAsString()
                                        )
                        );
                feEligibleOpponents.add(feOpponent);
            }
            frontendData.add("eligibleOpponents", feEligibleOpponents);
        }
        frontendData.add("eligibleAllies", feEligibleAllies);
        return Optional.of(frontendData);
    }

    @Override
    protected void doShutdown() {

    }

    @Override
    protected Optional<JsonObject> fetchCurrentState() {
        HttpsURLConnection con = starter.getConnectionManager().buildConnection(ConnectionManager.conOptions.GET, "/lol-honor-v2/v1/ballot");
        JsonObject data = ConnectionManager.getResponseBodyAsJsonObject(con);
        if (data.has("errorCode")) return Optional.empty();
        return backendHonorToFrontendHonor(data);
    }

    @Override
    public String getEventName() {
        return DataManager.UPDATE_TYPE.STATE_HONOR_EOG.name();
    }
}
