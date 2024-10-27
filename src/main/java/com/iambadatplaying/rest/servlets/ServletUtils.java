package com.iambadatplaying.rest.servlets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import javax.ws.rs.core.Response;

public class ServletUtils {

    public static final String KEY_MESSAGE = "message";
    public static final String KEY_ERROR = "error";
    public static final String KEY_DETAILS = "details";

    public static JsonObject createErrorJson(String message, String details) {
        JsonObject errorJson = new JsonObject();
        errorJson.addProperty(KEY_ERROR, message);

        if (details != null && !details.isEmpty()) {
            errorJson.addProperty(KEY_DETAILS, details);
        }

        return errorJson;
    }

    public static JsonObject createErrorJson(String message) {
        return createErrorJson(message, null);
    }

    public static JsonObject createSuccessJson(String message) {
        return createSuccessJson(message, (String) null);
    }

    public static JsonObject createSuccessJson(String message, String details) {
        JsonObject resp = new JsonObject();
        resp.addProperty(KEY_MESSAGE, message);

        if (details != null && !details.isEmpty()) {
            resp.addProperty(KEY_DETAILS, details);
        }

        return resp;
    }

    public static JsonObject createSuccessJson(String message, JsonElement details) {
        JsonObject resp = new JsonObject();
        resp.addProperty(KEY_MESSAGE, message);

        if (details != null && !details.isJsonNull()) {
            resp.add(KEY_DETAILS, details);
        }

        return resp;
    }
}
