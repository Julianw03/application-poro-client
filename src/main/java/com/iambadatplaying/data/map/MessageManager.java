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
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageManager extends MapDataManager<String> {

    public static final String CURRENT_CHAMP_SELECT = "CHAMP_SELECT";

    private static final String SYSTEM_MESSAGE = "system";

    private static final String SYSTEM_LEFT_ROOM = "left_room";

    String completeRegex = "/lol-chat/v1/conversations(/[^/]+)(/participants|/messages((/([^/]+))|$)|$)";

    Pattern completePattern = Pattern.compile(completeRegex);

    String conversationIdRegex = "/lol-chat/v1/conversations/([^/]+)";

    Pattern conversationIdPattern = Pattern.compile(conversationIdRegex);

    public MessageManager(Starter starter) {
        super(starter);
    }

    @Override
    protected void doInitialize() {

    }

    @Override
    protected Matcher getURIMatcher(String uri) {
        return completePattern.matcher(uri);
    }

    private Optional<String> extractConversationId(String matcherPart) {
        matcherPart = matcherPart.substring(1);

        if (!matcherPart.contains("@")) {
            try {
                return Optional.of(URLDecoder.decode(matcherPart, StandardCharsets.UTF_8.toString()));
            } catch (Exception e) {
                return Optional.empty();
            }
        } else {
            return Optional.of(matcherPart);
        }
    }

    @Override
    protected void doUpdateAndSend(Matcher uriMatcher, String type, JsonElement data) {
        log("Update");
        switch (type) {
            case UPDATE_TYPE_DELETE:
                handleDelete(uriMatcher, type, data);
                break;
            case UPDATE_TYPE_CREATE:
            case UPDATE_TYPE_UPDATE:
                //I swear to god riot, why do you have to make this so complicated
                //Peer to peer messages are updated via single messages, only override happens on init
                //For some godforsaken reason, group messages are updated via a message array, that replaces the current one AND via single messages
                //I should really look into the RTMP in the lower levels but this is beyond the scope of this project
                handleUpdateOrCreate(uriMatcher, type, data);
                break;
        }
    }

    private void handleUpdateOrCreate(Matcher uriMatcher, String type, JsonElement data) {
        final MatchResult result = uriMatcher.toMatchResult();
        int matchCount = result.groupCount();
        switch (matchCount) {
            case 0:
                break;
            case 1:
                String co = result.group(1);
                log(co);
                break;
            case 2:
                String convoId = result.group(1);
                if ("active".equals(convoId)) return;
                log("Updating conversation " + convoId + " via conversation data", Starter.LOG_LEVEL.DEBUG);
                updateConversationData(convoId, data.getAsJsonObject());
                break;
            case 3:
            default:
                log("Group count: " + matchCount);
                if ("/participants".equals(result.group(2)) || "".equals(result.group(2))) return;
                Optional<String> conversationId = extractConversationId(result.group(1));
                if (!conversationId.isPresent()) return;
                String conversationIdStr = conversationId.get();
                if ("active".equals(conversationIdStr)) return;
                if ("".equals(result.group(3))) {//Ends with /messages
                    log("Conversation " + conversationIdStr + " updated via message array", Starter.LOG_LEVEL.DEBUG);
                    handleMessageArray(conversationIdStr, data.getAsJsonArray());
                } else {
                    log("Conversation " + conversationIdStr + " updated via single message", Starter.LOG_LEVEL.DEBUG);
                    handleSingleMessage(conversationIdStr, data.getAsJsonObject());
                }
        }
    }

    private void updateConversationData(String conversationId, JsonObject updatedData) {
        Optional.ofNullable(map.get(conversationId)).ifPresent(
                currentData -> {
                    Util.copyJsonAttributes(updatedData, currentData, "inviterId","isMuted", "name","targetRegion", "type");
                }
        );
    }

    private void handleDelete(Matcher uriMatcher, String type, JsonElement data) {
        int matchCount = uriMatcher.groupCount();
        switch (matchCount) {
            case 0:
                break;
            case 2:
            case 3:
            default:
                if (!"".equals(uriMatcher.group(2))) return;
                log("Registered conversation deletion", Starter.LOG_LEVEL.DEBUG);
                Optional<String> conversationId = extractConversationId(uriMatcher.group(1));
                if (conversationId.isPresent()) {
                    log("Conversation " + conversationId.get() + " deleted, removing from cache", Starter.LOG_LEVEL.DEBUG);
                    map.remove(conversationId.get());
                    sendKeyUpdate(conversationId.get(), null);
                }
                break;
        }
    }

    private void handleMessageArray(String conversationId, JsonArray messages) {
        JsonObject conversation = map.get(conversationId);
        if (conversation == null) {
            conversation = fetchConversation(conversationId);
            if (conversation == null) {
                log("Failed to initialize conversation " + conversationId, Starter.LOG_LEVEL.WARN);
                return;
            }
            map.put(conversationId, conversation);
        }
        conversation.add("messages", messages);
        sendConversation(conversationId, conversation);
    }

    private void handleSingleMessage(String conversationId, JsonObject messageData) {
        if (!Util.jsonKeysPresent(messageData, "body", "type", "id")) {
            log("Message " + messageData.get("id").getAsString() + " is missing required fields", Starter.LOG_LEVEL.DEBUG);
            return;
        }
        String type = messageData.get("type").getAsString();
        String body = messageData.get("body").getAsString();
        //IF the message is a system message and of type "left_room" we know this conversation is ending, we dont initialize it
        if (type.equals(SYSTEM_MESSAGE) && body.equals(SYSTEM_LEFT_ROOM)) {
            log("Conversation " + conversationId + " ended, will not update", Starter.LOG_LEVEL.DEBUG);
            return;
        }
        //Otherwise we try to get the conversation, if it doesnt exist we initialize it
        JsonObject conversation = map.get(conversationId);
        if (conversation == null) {
            log("Conversation " + conversationId + " not found, initializing", Starter.LOG_LEVEL.DEBUG);
            conversation = fetchConversation(conversationId);
            if (conversation == null) {
                log("Failed to initialize conversation " + conversationId, Starter.LOG_LEVEL.WARN);
                return;
            }
        }
        String currentMessageId = messageData.get("id").getAsString();
        if (!conversation.has("messages")) return;
        JsonArray previousMessages = conversation.get("messages").getAsJsonArray();
        //We will just check the last 10 messages for duplicates, if we find one we dont add it
        for (int i = previousMessages.size() - 1; i >= 0 && i >= previousMessages.size() - 10; i--) {
            JsonObject previousMessage = previousMessages.get(i).getAsJsonObject();
            if (previousMessage.get("id").getAsString().equals(currentMessageId)) {
                log("Conversation " + conversationId + " already contains message " + currentMessageId + ", will not update", Starter.LOG_LEVEL.DEBUG);
                sendConversation(conversationId, conversation);
                return;
            }
        }
        previousMessages.add(messageData);

        sendConversation(conversationId, conversation);
    }

    private void sendConversation(String key,JsonObject conversation) {
        sendKeyUpdate(key, conversation);
    }


    @Override
    protected void doShutdown() {

    }

    @Override
    public Optional<JsonObject> doLoad(String key) {
        log("Load called for " + key, Starter.LOG_LEVEL.DEBUG);
        try {
            if (!key.contains("@")) {
                key = URLDecoder.decode(key, String.valueOf(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            return Optional.empty();
        }
        return Optional.ofNullable(fetchConversation(key));
    }

    private JsonObject fetchConversation(String conversationId) {
        log("Trying to fetch conversation " + conversationId, Starter.LOG_LEVEL.DEBUG);
        JsonObject initialFetchJson = ConnectionManager.getResponseBodyAsJsonObject(starter.getConnectionManager().buildConnection(ConnectionManager.conOptions.GET, "/lol-chat/v1/conversations/" + conversationId));
        if (initialFetchJson.has("httpStatus") && initialFetchJson.get("httpStatus").getAsInt() == 404) {
            initialFetchJson = initConversation(conversationId);
            if (initialFetchJson.has("errorCode")) {
                return null;
            }
            return initialFetchJson;
        }
        JsonElement messagesElement = ConnectionManager.getResponseBodyAsJsonElement(starter.getConnectionManager().buildConnection(ConnectionManager.conOptions.GET, "/lol-chat/v1/conversations/" + conversationId + "/messages"));
        if (!messagesElement.isJsonArray()) {
            log("Failed to fetch messages for conversation " + conversationId, Starter.LOG_LEVEL.ERROR);
            return null;
        }
        JsonArray messages = messagesElement.getAsJsonArray();
        initialFetchJson.add("messages", messages);
        return initialFetchJson;
    }

    private JsonObject initConversation(String conversationId) {
        log("Conversation " + conversationId + " not found, trying to initialize it", Starter.LOG_LEVEL.INFO);
        JsonObject body = new JsonObject();
        body.addProperty("id", conversationId);
        body.addProperty("type", "chat");
        log(body);
        return ConnectionManager.getResponseBodyAsJsonObject(starter.getConnectionManager().buildConnection(ConnectionManager.conOptions.POST, "/lol-chat/v1/conversations", body.toString()));
    }

    @Override
    public Optional<JsonObject> getExternal(String key) {
        if (key == null) return Optional.empty();
        return get(key);
    }

    @Override
    public String getMapEventName() {
        return DataManager.UPDATE_TYPE.MAP_CONVERSATIONS.name();
    }

    @Override
    public String getKeyEventName() {
        return DataManager.UPDATE_TYPE.SINGLE_CONVERSATION.name();
    }
}
