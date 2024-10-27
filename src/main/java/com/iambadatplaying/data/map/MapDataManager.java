package com.iambadatplaying.data.map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.Util;
import com.iambadatplaying.data.BasicDataManager;
import com.iambadatplaying.data.DataManager;

import java.util.*;
import java.util.function.Function;

public abstract class MapDataManager<T> extends BasicDataManager {

    protected Map<T, JsonObject> map;

    protected MapDataManager(Starter starter) {
        super(starter);
        map = Collections.synchronizedMap(new HashMap<>());
    }

    public static <T> Map<T, JsonObject> getMapFromArray(JsonArray array, String identifier, Function<JsonElement, T> keyMapper) {
        Map<T, JsonObject> map = new HashMap<>();
        for (JsonElement element : array) {
            if (!element.isJsonObject()) continue;
            JsonObject object = element.getAsJsonObject();
            if (!object.has(identifier)) continue;
            T key = keyMapper.apply(object.get(identifier));
            map.put(key, object);
        }
        return map;
    }

    public Map<T, JsonObject> getMap() {
        return Collections.unmodifiableMap(map);
    }

    public Optional<JsonObject> get(T key) {
        if (map.containsKey(key)) return Optional.ofNullable(map.get(key));
        Optional<JsonObject> value = load(key);
        value.ifPresent(jsonObject -> map.put(key, jsonObject));
        return value;
    }

    public abstract Optional<JsonObject> getExternal(String key);

    public Optional<JsonObject> load(T key) {
        Optional<JsonObject> loadedObject = doLoad(key);
        loadedObject.ifPresent(
                jsonObject -> {
                    if (!Util.equalJsonElements(jsonObject, map.get(key))) {
                        sendKeyUpdate(key, jsonObject);
                    }
                }
        );
        return loadedObject;
    }

    protected abstract Optional<JsonObject> doLoad(T key);

    @Override
    public void sendCurrentState() {
        starter.getServer().sendToAllSessions(DataManager.getEventDataString(getMapEventName(), getMapAsJson()));
    }

    public void sendKeyUpdate(T key) {
        sendKeyUpdate(key, map.get(key));
    }

    protected void sendKeyUpdate(T key, JsonElement data) {
        if (key == null) return;
        starter.getServer().sendToAllSessions(DataManager.getKeyEventDataString(getKeyEventName(), key.toString(), data));
    }

    public void edit(T key, JsonObject value) {
        map.put(key, value);
    }

    public JsonObject getMapAsJson() {
        JsonObject mapAsJson = new JsonObject();
        for (Map.Entry<T, JsonObject> entry : map.entrySet()) {
            mapAsJson.add(entry.getKey().toString(), entry.getValue());
        }
        return mapAsJson;
    }

    @Override
    public void shutdown() {
        if (!initialized) return;
        initialized = false;
        doShutdown();
        map.clear();
    }

    public abstract String getMapEventName();

    public abstract String getKeyEventName();
}
