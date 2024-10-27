package com.iambadatplaying.tasks.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.Util;
import com.iambadatplaying.data.BasicDataManager;
import com.iambadatplaying.data.map.FriendManager;
import com.iambadatplaying.lcuHandler.ConnectionManager;
import com.iambadatplaying.rest.servlets.ServletUtils;
import com.iambadatplaying.tasks.Task;

import javax.net.ssl.HttpsURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FriendOnlineNotifier extends Task {

    private final Map<String, JsonObject> friendMap = Collections.synchronizedMap(new HashMap<>());

    private static final Pattern lolChatV1FriendsPattern = Pattern.compile("/lol-chat/v1/friends/(.*)");

    private static final String DESCRIPTION = "Notifies when friends come online via the in-client notification system. (League Client only)";

    @Override
    public void notify(String type, String uri, JsonElement data) {
        if (!running || starter == null) return;

        Matcher matcher = lolChatV1FriendsPattern.matcher(uri);
        if (!matcher.matches()) return;

        switch (type) {
            case BasicDataManager.UPDATE_TYPE_DELETE:
                String id = matcher.group(1);
                friendMap.remove(id);
                break;
            case BasicDataManager.UPDATE_TYPE_CREATE:
            case BasicDataManager.UPDATE_TYPE_UPDATE:
                if (!data.isJsonObject()) return;
                Optional<JsonObject> updatedFriend = dataToMinimalFriend(data);
                updatedFriend.ifPresent(friend -> {
                    String newAvailability = friend.get("availability").getAsString().trim();
                    JsonObject oldFriend = friendMap.get(friend.get("id").getAsString());
                    String oldAvailability = Util.getString(oldFriend, "availability", "offline").trim();
                    if (newAvailability.equals(oldAvailability)) return;
                    log("Update for " + friend.get("gameName").getAsString() + " " + friend.get("gameTag").getAsString() + ": " + oldAvailability + " -> " + newAvailability, Starter.LOG_LEVEL.INFO);
                    String friendId = friend.get("id").getAsString();
                    friendMap.put(friendId, friend);
                    if ("offline".equals(oldAvailability) || "mobile".equals(oldAvailability) || "away".equals(oldAvailability)) {
                        showNotification("Friend Online", friend.get("gameName").getAsString() + "#" + friend.get("gameTag").getAsString() + " is now online");
                    }
                });
                break;
        }
    }

    private void showNotification(String title, String details) {
        JsonObject requestData = new JsonObject();
        requestData.addProperty("title", title);
        requestData.addProperty("details", details);
        JsonObject request = new JsonObject();
        request.addProperty("critical", false);
        request.addProperty("iconUrl", "/fe/lol-settings/poro_smile.png");
        request.addProperty("detailKey", "pre_translated_details");
        request.addProperty("titleKey", "pre_translated_title");
        request.add("data", requestData);
        HttpsURLConnection con = starter.getConnectionManager().buildConnection(ConnectionManager.conOptions.POST, "/player-notifications/v1/notifications", request.toString());
        Optional<JsonObject> responseJson = Util.getInputStream(con).flatMap(Util::inputStreamToString).flatMap(Util::parseJson).flatMap(Util::asJsonObject);
        con.disconnect();
        if (!responseJson.isPresent()) {
            log("Failed to send notification", Starter.LOG_LEVEL.WARN);
            return;
        }

        JsonObject response = responseJson.get();
        if (response.has("errorCode")) {
            log("Failed to send notification: " + response.get("message").getAsString(), Starter.LOG_LEVEL.WARN);
            return;
        }

        log("Notification sent", Starter.LOG_LEVEL.INFO);
        return;
    }

    private Optional<JsonObject> dataToMinimalFriend(JsonElement data) {
        return Util.asJsonObject(data).map(friendObj ->
                {
                    JsonObject minimalFriend = new JsonObject();
                    Util.copyJsonAttributes(friendObj, minimalFriend, "availability", "puuid", "id", "gameName", "gameTag");
                    return minimalFriend;
                });
    }

    @Override
    protected void doInitialize() {
        FriendManager friendManager = (FriendManager) starter.getDataManager().<String>getMapManager(FriendManager.class);
        if (friendManager == null) {
            log("FriendManager not found", Starter.LOG_LEVEL.WARN);
            return;
        }
        friendManager.getMap().values().forEach(
                savedFriend -> {
                    JsonObject minimalFriend = new JsonObject();
                    Util.copyJsonAttributes(savedFriend, minimalFriend, "availability", "puuid", "id", "gameName", "gameTag");
                    friendMap.put(savedFriend.get("id").getAsString(), minimalFriend);
                }
        );
        log("Copied friendMap", Starter.LOG_LEVEL.INFO);
    }

    @Override
    protected void doShutdown() {
        log("Clearing friendMap", Starter.LOG_LEVEL.INFO);
        friendMap.clear();
    }

    public static String getDescription() {
        return DESCRIPTION;
    }
}
