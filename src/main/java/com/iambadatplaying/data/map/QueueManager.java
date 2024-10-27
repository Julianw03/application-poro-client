package com.iambadatplaying.data.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.data.DataManager;
import com.iambadatplaying.lcuHandler.ConnectionManager;

import javax.net.ssl.HttpsURLConnection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueueManager extends MapDataManager<Integer> {

    private static final Pattern lolQueueV1QueuePattern = Pattern.compile("/lol-game-queues/v1/queues(/(\\d*)|$)");

    private static final int GROUP_COUNT_UPDATE_ALL = 1;
    private static final int GROUP_COUNT_UPDATE_SINGLE_ID = 2;

    public QueueManager(Starter starter) {
        super(starter);
    }

    @Override
    protected void doInitialize() {
        fetchQueues();
    }

    private void fetchQueues() {
        JsonArray queues = ConnectionManager.getResponseBodyAsJsonArray(starter.getConnectionManager().buildConnection(ConnectionManager.conOptions.GET, "/lol-game-queues/v1/queues"));
        if (queues == null) return;
        for (int i = 0; i < queues.size(); i++) {
            JsonObject queue = queues.get(i).getAsJsonObject();
            if (!queue.has("id")) continue;
            int id = queue.get("id").getAsInt();
            map.put(id, queue);
        }
    }

    @Override
    protected Matcher getURIMatcher(String uri) {
        return lolQueueV1QueuePattern.matcher(uri);
    }

    @Override
    protected void doUpdateAndSend(Matcher uriMatcher, String type, JsonElement data) {
        switch (type) {
            case UPDATE_TYPE_CREATE:
            case UPDATE_TYPE_UPDATE:
                if (!data.isJsonObject()) return;
                switch (uriMatcher.groupCount()) {
                    case GROUP_COUNT_UPDATE_ALL:
                        JsonArray queues = data.getAsJsonArray();
                        for (int i = 0; i < queues.size(); i++) {
                            JsonObject queue = queues.get(i).getAsJsonObject();
                            if (!queue.has("id")) continue;
                            int id = queue.get("id").getAsInt();
                            map.put(id, queue);
                        }
                        sendCurrentState();
                        break;
                    case GROUP_COUNT_UPDATE_SINGLE_ID:
                        if (uriMatcher.group(2).isEmpty()) {
                            return;
                        }
                        Integer idToUpdate = Integer.parseInt(uriMatcher.group(2));
                        map.put(idToUpdate, data.getAsJsonObject());
                        sendKeyUpdate(idToUpdate, data);
                        break;
                    default:
                        break;
                }
                break;
            case UPDATE_TYPE_DELETE:
                //Queues per se cannot be deleted, only be updated to be 'PlatformDisabled'
                break;
        }
    }

    @Override
    protected void doShutdown() {

    }

    @Override
    public Optional<JsonObject> doLoad(Integer key) {
        HttpsURLConnection con = starter.getConnectionManager().buildConnection(ConnectionManager.conOptions.GET, "/lol-game-queues/v1/queues/" + key);
        JsonObject data = ConnectionManager.getResponseBodyAsJsonObject(con);
        if (!data.has("errorCode")) return Optional.of(data);
        return Optional.empty();
    }

    @Override
    public Optional<JsonObject> getExternal(String key) {
        if (key == null) return Optional.empty();
        if (key.isEmpty()) return Optional.empty();
        try {
            Integer puuid = Integer.valueOf(key);
            return get(puuid);
        } catch (Exception e) {}
        return Optional.empty();
    }

    @Override
    public String getMapEventName() {
        return DataManager.UPDATE_TYPE.MAP_QUEUES.name();
    }

    @Override
    public String getKeyEventName() {
        return DataManager.UPDATE_TYPE.SINGLE_QUEUE.name();
    }
}
