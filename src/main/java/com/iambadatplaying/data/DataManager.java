package com.iambadatplaying.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.data.array.*;
import com.iambadatplaying.data.map.*;
import com.iambadatplaying.data.state.*;
import com.iambadatplaying.frontendHandler.LocalWebSocket;
import com.iambadatplaying.lcuHandler.ConnectionManager;

import java.util.Collection;
import java.util.HashMap;

public class DataManager {
    public enum UPDATE_TYPE {
        // STATE UPDATE TYPES
        STATE_SELF_PRESENCE,
        STATE_GAMEFLOW_PHASE,
        STATE_LOBBY,
        STATE_LOOT,
        STATE_PATCHER,
        STATE_CHAMP_SELECT,
        STATE_HONOR_EOG,
        STATE_MATCHMAKING_SEARCH,
        STATE_STATS_EOG,
        STATE_CURRENT_SUMMONER,

        // MAP UPDATE TYPES
        MAP_FRIENDS,
        SINGLE_FRIEND,
        MAP_FRIEND_GROUPS,
        SINGLE_FRIEND_GROUP,
        MAP_FRIEND_HOVERCARDS,
        SINGLE_FRIEND_HOVERCARD,
        MAP_GENERIC_PRESENCES,
        SINGLE_GENERIC_PRESENCE,
        MAP_CONVERSATIONS,
        SINGLE_CONVERSATION,
        MAP_QUEUES,
        SINGLE_QUEUE,
        MAP_REGALIA,
        SINGLE_REGALIA,
        MAP_CHALLENGE_SUMMARY,
        SINGLE_CHALLENGE_SUMMARY,
        MAP_GAME_NAMES,
        SINGLE_GAME_NAME,

        // ARRAY
        ARRAY_TICKER_MESSAGES,
        ARRAY_INVITATIONS,
        ARRAY_OWNED_CHAMPIONS,
        ARRAY_OWNED_SKINS,

        // OTHER
        INTERNAL_STATE,
        ALL_INITIAL_DATA_LOADED;
    }

    private static final String DATA_STRING_EVENT = "event";
    private static final String DATA_STRING_DATA  = "data";
    private static final String DATA_STRING_ID    = "id";

    private final Starter                            starter;
    private final HashMap<String, StateDataManager>  stateDataManagers;
    private final HashMap<String, MapDataManager<?>> mapDataManagers;
    private final HashMap<String, ArrayDataManager>  arrayDataManagers;

    private boolean initialized = false;

    public DataManager(Starter starter) {
        this.starter = starter;
        this.stateDataManagers = new HashMap<>();
        this.mapDataManagers = new HashMap<>();
        this.arrayDataManagers = new HashMap<>();

        addStateManagers();
        addMapManagers();
        addArrayManagers();
    }

    public static String getEventDataString(String event, JsonElement data) {
        JsonObject dataToSend = new JsonObject();
        dataToSend.addProperty(DATA_STRING_EVENT, event);
        dataToSend.add(DATA_STRING_DATA, data);
        return dataToSend.toString();
    }

    public static String getKeyEventDataString(String event, String identifier, JsonElement data) {
        JsonObject dataToSend = new JsonObject();
        dataToSend.addProperty(DATA_STRING_EVENT, event);
        dataToSend.addProperty(DATA_STRING_ID, identifier);
        dataToSend.add(DATA_STRING_DATA, data);
        return dataToSend.toString();
    }

    private void addArrayManagers() {
        addManager(new TickerMessageManager(starter));
        addManager(new InvitationManager(starter));
        addManager(new ChampionInventoryManager(starter));
        addManager(new SkinInventoryManager(starter));
    }

    private void addMapManagers() {
        addManager(new RegaliaManager(starter));
        addManager(new FriendManager(starter));
        addManager(new GameNameManager(starter));
        addManager(new MessageManager(starter));
        addManager(new FriendGroupManager(starter));
        addManager(new QueueManager(starter));
        addManager(new FriendHovercardManager(starter));
        addManager(new SummonerIdToPuuidManager(starter));
        addManager(new GeneralChatPresenceManager(starter));
        addManager(new ChallengeSummaryDataManager(starter));
    }

    private void addStateManagers() {
        addManager(new LobbyData(starter));
        addManager(new GameflowData(starter));
        addManager(new ChatMeManager(starter));
        addManager(new LootDataManager(starter));
        addManager(new PatcherData(starter));
        addManager(new ReworkedChampSelectData(starter));
        addManager(new EOGHonorManager(starter));
        addManager(new MatchmakingSearchManager(starter));
        addManager(new CurrentSummonerManager(starter));
    }

    private void addManager(ArrayDataManager manager) {
        arrayDataManagers.put(manager.getClass().getName(), manager);
    }

    private void addManager(StateDataManager manager) {
        stateDataManagers.put(manager.getClass().getName(), manager);
    }

    private void addManager(MapDataManager manager) {
        mapDataManagers.put(manager.getClass().getName(), manager);
    }

    public void init() {
        if (initialized) {
            log("Already initialized, wont have any effect", Starter.LOG_LEVEL.WARN);
            return;
        }
        initialized = true;

        log("Initializing specific DataManagers", Starter.LOG_LEVEL.INFO);
        for (StateDataManager manager : stateDataManagers.values()) {
            manager.init();
        }

        for (MapDataManager manager : mapDataManagers.values()) {
            manager.init();
        }

        for (ArrayDataManager manager : arrayDataManagers.values()) {
            manager.init();
        }

        log("Specific DataManagers initialized!", Starter.LOG_LEVEL.INFO);
    }

    public void shutdown() {
        log("Shutting down specific DataManagers", Starter.LOG_LEVEL.INFO);
        for (StateDataManager manager : stateDataManagers.values()) {
            manager.shutdown();
        }

        for (MapDataManager manager : mapDataManagers.values()) {
            manager.shutdown();
        }

        for (ArrayDataManager manager : arrayDataManagers.values()) {
            manager.shutdown();
        }

        initialized = false;
    }

    public void update(String type, String uri, JsonElement data) {
        doUpdate(type, uri, data);
    }

    //This method doesnt utilize Threads to ensure that the initial data is sent before any other data
    public void sendInitialDataBlocking(LocalWebSocket localWebSocket) {
        for (StateDataManager manager : stateDataManagers.values()) {
            manager.sendCurrentState();
        }

        for (ArrayDataManager manager : arrayDataManagers.values()) {
            manager.sendCurrentState();
        }

        for (MapDataManager<?> manager : mapDataManagers.values()) {
            manager.sendCurrentState();
        }
    }

    private void doUpdate(String type, String uri, JsonElement data) {
        if (!initialized) {
            log("Not initialized, wont have any effect", Starter.LOG_LEVEL.WARN);
            return;
        }

        if (uri == null || uri.isEmpty()) {
            log("Uri is empty, or null, parsing error occurred", Starter.LOG_LEVEL.ERROR);
            return;
        }

        if (ConnectionManager.isProtectedRessource(uri)) {
            log("Update from protected Ressource, wont fire update", Starter.LOG_LEVEL.INFO);
            return;
        }

        if (type == null || type.isEmpty()) {
            log("Type is empty, or null, parsing error occurred", Starter.LOG_LEVEL.ERROR);
            return;
        }

        if (data == null || data.isJsonNull()) {
            data = new JsonObject();
        }

        if (!data.isJsonArray() && !data.isJsonObject()) {
            log("Not Array or Object : " + type + " " + uri + ": " + data, Starter.LOG_LEVEL.NO_PARSING);
            return;
        }

        if (data.toString().length() >= 100_000) {
            log(type + " " + uri + ": - Too long to print - ", Starter.LOG_LEVEL.LCU_MESSAGING);
        } else {
            log(type + " " + uri + ": " + data, Starter.LOG_LEVEL.LCU_MESSAGING);
        }

        final JsonElement finalData = data;
        for (StateDataManager manager : stateDataManagers.values()) {
            manager.update(uri, type, finalData);
        }

        for (MapDataManager manager : mapDataManagers.values()) {
            manager.update(uri, type, finalData);
        }

        for (ArrayDataManager manager : arrayDataManagers.values()) {
            manager.update(uri, type, finalData);
        }
    }

    public StateDataManager getStateManager(Class manager) {
        return stateDataManagers.get(manager.getName());
    }

    public <T> MapDataManager<T> getMapManager(Class manager) {
        return (MapDataManager<T>) mapDataManagers.get(manager.getName());
    }

    public ArrayDataManager getArrayManager(Class manager) {
        return arrayDataManagers.get(manager.getName());
    }

    public Collection<StateDataManager> getStateManagers() {
        return stateDataManagers.values();
    }

    public Collection<MapDataManager<?>> getMapManagers() {
        return mapDataManagers.values();
    }

    public Collection<ArrayDataManager> getArrayManagers() {
        return arrayDataManagers.values();
    }

    private void log(Object o, Starter.LOG_LEVEL level) {
        starter.log(this.getClass().getSimpleName() + ": " + o, level);
    }

    private void log(Object o) {
        log(o, Starter.LOG_LEVEL.DEBUG);
    }
}
