package com.iambadatplaying.tasks.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.iambadatplaying.Starter;
import com.iambadatplaying.lcuHandler.ConnectionManager;
import com.iambadatplaying.tasks.ARGUMENT_TYPE;
import com.iambadatplaying.tasks.Task;
import com.iambadatplaying.tasks.builders.TaskArgumentBuilder;
import com.iambadatplaying.tasks.builders.impl.NumberDataBuilder;
import com.iambadatplaying.tasks.builders.impl.SelectDataBuilder;
import com.iambadatplaying.tasks.builders.impl.SelectOption;

import javax.net.ssl.HttpsURLConnection;

public class ChatAppearanceOverride extends Task {

    private final static String lol_gameflow_v1_gameflow_phase = "/lol-gameflow/v1/session";

    private static final String KEY_ICON_ID                 = "iconId";
    private static final String KEY_CHALLENGE_POINTS        = "challengePoints";
    private static final String KEY_RANKED_LEAGUE_QUEUE     = "rankedLeagueQueue";
    private static final String KEY_RANKED_LEAGUE_TIER      = "rankedLeagueTier";
    private static final String KEY_CHALLENGE_CRYSTAL_LEVEL = "challengeCrystalLevel";
    private static final String KEY_MASTERY_SCORE           = "masteryScore";
    private static final String KEY_AVAILABILITY            = "availability";

    private Integer iconId;
    private Integer challengePoints;
    private String  rankedLeagueQueue;
    private String  rankedLeagueTier;
    private String  challengeCrystalLevel;
    private Integer masteryScore;
    private String availability;

    public static JsonArray getTaskArguments() {
        JsonArray requiredArgs = new JsonArray();

        requiredArgs.add(
                new TaskArgumentBuilder()
                        .setDisplayName("Icon ID")
                        .setBackendKey(KEY_ICON_ID)
                        .setType(ARGUMENT_TYPE.NUMBER)
                        .setRequired(false)
                        .setDescription("The icon ID to display for other players")
                        .build()
        );


        requiredArgs.add(
                new TaskArgumentBuilder()
                        .setDisplayName("Challenge Points")
                        .setBackendKey(KEY_CHALLENGE_POINTS )
                        .setType(ARGUMENT_TYPE.NUMBER)
                        .setRequired(false)
                        .setDescription("The challenge points to display in your Hovercard")
                        .build()
        );


        requiredArgs.add(
                new TaskArgumentBuilder()
                        .setDisplayName("Ranked League Queue")
                        .setBackendKey(KEY_RANKED_LEAGUE_QUEUE)
                        .setType(ARGUMENT_TYPE.SELECT)
                        .setAdditionalData(getRankedLeagueQueueOptions())
                        .setRequired(false)
                        .setDescription("The rank queue Type to display in your Hovercard")
                        .build()
        );


        requiredArgs.add(
                new TaskArgumentBuilder()
                        .setDisplayName("Ranked League Tier")
                        .setBackendKey(KEY_RANKED_LEAGUE_TIER)
                        .setType(ARGUMENT_TYPE.SELECT)
                        .setAdditionalData(getRankedLeagueTierOptions())
                        .setRequired(false)
                        .setDescription("The ranked league tier to display in your Hovercard")
                        .build()
        );

        requiredArgs.add(
                new TaskArgumentBuilder()
                        .setDisplayName("Challenge Crystal Level")
                        .setBackendKey(KEY_CHALLENGE_CRYSTAL_LEVEL)
                        .setType(ARGUMENT_TYPE.SELECT)
                        .setAdditionalData(getChallengeCrystalLevelOptions())
                        .setRequired(false)
                        .setDescription("The challenge crystal level to display in your Hovercard")
                        .build()
        );

        requiredArgs.add(
                new TaskArgumentBuilder()
                        .setDisplayName("Mastery Score")
                        .setBackendKey(KEY_MASTERY_SCORE )
                        .setType(ARGUMENT_TYPE.NUMBER)
                        .setRequired(false)
                        .setDescription("The mastery score to display in your Hovercard")
                        .build()
        );

        requiredArgs.add(
                new TaskArgumentBuilder()
                        .setDisplayName(KEY_AVAILABILITY )
                        .setBackendKey(KEY_AVAILABILITY )
                        .setType(ARGUMENT_TYPE.SELECT)
                        .setAdditionalData(getAvailabilityOptions())
                        .setRequired(false)
                        .setDescription("The availability to display in your Hovercard")
                        .build()
        );

        return requiredArgs;
    }

    private static JsonObject getRankedLeagueQueueOptions() {
        return new SelectDataBuilder()
                .addOptions(
                        new SelectOption[]{
                                new SelectOption("Ranked Solo/Duo", "RANKED_SOLO_5x5"),
                                new SelectOption("Ranked Flex", "RANKED_FLEX_SR"),
                                new SelectOption("Ranked TFT", "RANKED_TFT")
                        }
                ).build();
    }

    private static JsonObject getRankedLeagueTierOptions() {
        return new SelectDataBuilder()
                .addOptions(
                        new SelectOption[]{
                                new SelectOption("Iron", "IRON"),
                                new SelectOption("Bronze", "BRONZE"),
                                new SelectOption("Silver", "SILVER"),
                                new SelectOption("Gold", "GOLD"),
                                new SelectOption("Platinum", "PLATINUM"),
                                new SelectOption("Emerald", "EMERALD"),
                                new SelectOption("Diamond", "DIAMOND"),
                                new SelectOption("Master", "MASTER"),
                                new SelectOption("Grandmaster", "GRANDMASTER"),
                                new SelectOption("Challenger", "CHALLENGER")
                        }
                )
                .build();
    }

    private static JsonObject getAvailabilityOptions() {
        return new SelectDataBuilder()
                .addOptions(
                        new SelectOption[]{
                                new SelectOption("Available", "chat"),
                                new SelectOption("Busy", "dnd"),
                                new SelectOption("Away", "away"),
                                new SelectOption("Offline", "offline"),
                                new SelectOption("Mobile", "mobile")
                        }
                )
                .build();
    }

    private static JsonObject getChallengeCrystalLevelOptions() {
        return new SelectDataBuilder()
                .addOptions(
                        new SelectOption[]{
                                new SelectOption("Iron", "IRON"),
                                new SelectOption("Bronze", "BRONZE"),
                                new SelectOption("Silver", "SILVER"),
                                new SelectOption("Gold", "GOLD"),
                                new SelectOption("Platinum", "PLATINUM"),
                                new SelectOption("Diamond", "DIAMOND"),
                                new SelectOption("Master", "MASTER"),
                                new SelectOption("Grandmaster", "GRANDMASTER"),
                                new SelectOption("Challenger", "CHALLENGER")
                        }
                )
                .build();
    }

    @Override
    public void notify(String type, String uri, JsonElement data) {
        if (!running || starter == null) return;
        if (!lol_gameflow_v1_gameflow_phase.equals(uri.trim())) return;
        if (!data.isJsonObject()) return;
        handleUpdateData(data.getAsJsonObject());
    }

    private JsonObject buildChatAppearanceOverride() {
        JsonObject chatAppearanceOverride = new JsonObject();
        try {
            JsonObject lol = new JsonObject();
            if (challengePoints != null) {
                lol.addProperty(KEY_CHALLENGE_POINTS , challengePoints.toString());
            }
            if (masteryScore != null) {
                lol.addProperty(KEY_MASTERY_SCORE , masteryScore.toString());
            }
            lol.addProperty(KEY_RANKED_LEAGUE_QUEUE, rankedLeagueQueue);
            lol.addProperty(KEY_CHALLENGE_CRYSTAL_LEVEL, challengeCrystalLevel);
            lol.addProperty(KEY_RANKED_LEAGUE_TIER, rankedLeagueTier);

            chatAppearanceOverride.add("lol", lol);

            if (iconId != null) {
                chatAppearanceOverride.addProperty("icon", iconId.intValue());
            }

            chatAppearanceOverride.addProperty(KEY_AVAILABILITY , availability);
        } catch (Exception e) {

        }
        return chatAppearanceOverride;
    }

    private void handleUpdateData(JsonObject updateData) {
        if (updateData == null || updateData.isEmpty()) return;
        String newGameflowPhase = updateData.get("data").getAsString();
        if ("EndOfGame".equals(newGameflowPhase)) {
            JsonObject body = buildChatAppearanceOverride();
            sendChatAppearanceOverride(body);
        }
    }

    private void sendChatAppearanceOverride(JsonObject body) {
        try {
            HttpsURLConnection con = starter.getConnectionManager().buildConnection(ConnectionManager.conOptions.PUT, "/lol-chat/v1/me", body.toString());
            String response = (String) starter.getConnectionManager().getResponse(ConnectionManager.responseFormat.STRING, con);
            starter.log(response);
            con.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doInitialize() {

    }

    @Override
    protected void doShutdown() {
        iconId = null;
        challengePoints = null;
        rankedLeagueQueue = null;
        rankedLeagueTier = null;
        challengeCrystalLevel = null;
        masteryScore = null;

        availability = null;
    }

    @Override
    public boolean setTaskArgs(JsonObject arguments) {
        try {
            if (arguments.has(KEY_ICON_ID)) {
                iconId = arguments.get(KEY_ICON_ID).getAsInt();
            }

            if (arguments.has(KEY_CHALLENGE_POINTS )) {
                challengePoints = arguments.get(KEY_CHALLENGE_POINTS ).getAsInt();
            }

            if (arguments.has(KEY_RANKED_LEAGUE_QUEUE)) {
                rankedLeagueQueue = arguments.get(KEY_RANKED_LEAGUE_QUEUE).getAsString();
            }

            if (arguments.has(KEY_CHALLENGE_CRYSTAL_LEVEL)) {
                challengeCrystalLevel = arguments.get(KEY_CHALLENGE_CRYSTAL_LEVEL).getAsString();
            }

            if (arguments.has(KEY_MASTERY_SCORE )) {
                masteryScore = arguments.get(KEY_MASTERY_SCORE ).getAsInt();
            }

            if (arguments.has(KEY_AVAILABILITY )) {
                availability = arguments.get(KEY_AVAILABILITY ).getAsString();
            }

            if (arguments.has(KEY_RANKED_LEAGUE_TIER)) {
                rankedLeagueTier = arguments.get(KEY_RANKED_LEAGUE_TIER).getAsString();
            }

            JsonObject body = buildChatAppearanceOverride();
            sendChatAppearanceOverride(body);
            return true;
        } catch (Exception e) {
            starter.log("Failed to set task arguments");
        }
        return false;
    }

    @Override
    public JsonObject getCurrentValues() {
        JsonObject taskArgs = new JsonObject();
        taskArgs.addProperty(KEY_ICON_ID, iconId);
        taskArgs.addProperty(KEY_CHALLENGE_POINTS , challengePoints);
        taskArgs.addProperty(KEY_RANKED_LEAGUE_QUEUE, rankedLeagueQueue);
        taskArgs.addProperty(KEY_CHALLENGE_CRYSTAL_LEVEL, challengeCrystalLevel);
        taskArgs.addProperty(KEY_MASTERY_SCORE , masteryScore);
        taskArgs.addProperty(KEY_AVAILABILITY , availability);
        taskArgs.addProperty(KEY_RANKED_LEAGUE_TIER, rankedLeagueTier);

        return taskArgs;
    }
}
