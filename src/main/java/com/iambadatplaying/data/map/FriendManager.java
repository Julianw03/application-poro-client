package com.iambadatplaying.data.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.Util;
import com.iambadatplaying.data.DataManager;
import com.iambadatplaying.lcuHandler.ConnectionManager;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FriendManager extends MapDataManager<String> {

    private static final Pattern lolChatV1FriendsPattern = Pattern.compile("/lol-chat/v1/friends/(.*)");

    private static final String KEY_ID = "id";

    public FriendManager(Starter starter) {
        super(starter);
    }

    @Override
    public Optional<JsonObject> doLoad(String key) {
        return Optional.empty();
    }

    @Override
    public void doInitialize() {
        fetchFriends();
    }
                 
    private void fetchFriends() {
        JsonArray friends = ConnectionManager.getResponseBodyAsJsonArray(starter.getConnectionManager().buildConnection(ConnectionManager.conOptions.GET, "/lol-chat/v1/friends"));
        if (friends == null) return;
        for (int i = 0; i < friends.size(); i++) {
            JsonObject friend = friends.get(i).getAsJsonObject();
            Optional<JsonObject> optFrontendFriend = backendToFrontendFriend(friend);
            if (!optFrontendFriend.isPresent()) continue;
            JsonObject frontendFriend = optFrontendFriend.get();
            map.put(friend.get(KEY_ID).getAsString(), frontendFriend);
        }
    }

    private Optional<JsonObject> backendToFrontendFriend(JsonObject friend) {
        JsonObject frontendFriend = new JsonObject();

        Optional<String> optPuuid = Util.getOptString(friend, KEY_ID);
        if (!optPuuid.isPresent()) return Optional.empty();
        String puuid = optPuuid.get();
        frontendFriend.addProperty(KEY_ID, puuid);

        Optional<Integer> optIcon = Util.getOptInt(friend, "icon");
        if (optIcon.isPresent()) {
            int icon = optIcon.get();
            if (icon < 1) icon = 1;
            frontendFriend.addProperty("iconId", icon);
        }

        if (friend.has("lol")) {
            JsonElement lol = friend.get("lol");
            if (lol.isJsonObject()) {
                JsonObject lolObj = lol.getAsJsonObject();
                Optional<String> optParty = Util.getOptString(lolObj, "pty");
                optParty.ifPresent(party -> lolObj.add("pty", Util.parseJson(party).orElse(new JsonObject())));

                Optional<String> optRegalia = Util.getOptString(lolObj, "regalia");
                optRegalia.ifPresent(regalia -> lolObj.add("regalia", Util.parseJson(regalia).orElse(new JsonObject())));
            }
        }

        Util.copyJsonAttributes(friend, frontendFriend, "availability", "statusMessage", "puuid", "id", "groupId", "lol", "summonerId", "gameName", "gameTag", "productName", "product");


        return Optional.of(frontendFriend);
    }

    @Override
    protected Matcher getURIMatcher(String uri) {
        return lolChatV1FriendsPattern.matcher(uri);
    }

    @Override
    protected void doUpdateAndSend(Matcher uriMatcher, String type, JsonElement data) {
        switch (type) {
            case UPDATE_TYPE_DELETE:
                String id = uriMatcher.group(1);
                map.remove(id);
                sendKeyUpdate(id, null);
                break;
            case UPDATE_TYPE_CREATE:
            case UPDATE_TYPE_UPDATE:
                Optional<JsonObject> updatedFriend = updateFriend(data);
                if (!updatedFriend.isPresent()) return;
                JsonObject dataObj = data.getAsJsonObject();
                JsonObject currentState = map.get(dataObj.get(KEY_ID).getAsString());
                JsonObject updatedState = updatedFriend.get();
                if (Util.equalJsonElements(updatedState, currentState)) return;
                String identifier = dataObj.get(KEY_ID).getAsString();
                map.put(identifier, updatedState);
                sendKeyUpdate(identifier, updatedState);
                break;
        }
    }

    private Optional<JsonObject> updateFriend(JsonElement friend) {
        if (!friend.isJsonObject()) return Optional.empty();
        JsonObject friendObj = friend.getAsJsonObject();
        return backendToFrontendFriend(friendObj);
    }

    @Override
    public void doShutdown() {
    }

    @Override
    public Optional<JsonObject> getExternal(String key) {
        if (key == null) return Optional.empty();
        return get(key);
    }

    @Override
    public String getMapEventName() {
        return DataManager.UPDATE_TYPE.MAP_FRIENDS.name();
    }

    @Override
    public String getKeyEventName() {
        return DataManager.UPDATE_TYPE.SINGLE_FRIEND.name();
    }
}
