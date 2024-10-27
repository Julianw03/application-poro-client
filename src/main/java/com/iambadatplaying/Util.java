package com.iambadatplaying;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.iambadatplaying.data.map.FriendGroupManager;
import com.iambadatplaying.data.map.FriendManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Pattern;

public class Util {

    public static Pattern UNMATCHABLE_PATTERN = Pattern.compile("^\\b$");

    private Util() {}

    public static Optional<JsonElement> parseJson(String json) {
        try {
            return Optional.of(JsonParser.parseString(json));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    public static boolean equalJsonElements(JsonElement a, JsonElement b) {
        if (a == null || b == null) return false;
        if (a.isJsonObject() && b.isJsonObject()) {
            return equalJsonObjects(a.getAsJsonObject(), b.getAsJsonObject());
        } else if (a.isJsonArray() && b.isJsonArray()) {
            return equalJsonArrays(a.getAsJsonArray(), b.getAsJsonArray());
        } else {
            return a.equals(b);
        }
    }

    private static boolean equalJsonObjects(JsonObject a, JsonObject b) {
        if (a == null || b == null) return false;
        if (a.entrySet().size() != b.entrySet().size()) return false;
        for (String key : a.keySet()) {
            if (!b.has(key)) return false;
            JsonElement aElement = a.get(key);
            JsonElement bElement = b.get(key);
            if (aElement.isJsonObject() && bElement.isJsonObject()) {
                if (!equalJsonObjects(aElement.getAsJsonObject(), bElement.getAsJsonObject())) return false;
            } else if (aElement.isJsonArray() && bElement.isJsonArray()) {
                if (!equalJsonArrays(aElement.getAsJsonArray(), bElement.getAsJsonArray())) return false;
            } else if (!aElement.equals(bElement)) return false;
        }
        return true;
    }

    private static boolean equalJsonArrays(JsonArray a, JsonArray b) {
        if (a == null || b == null) return false;
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            JsonElement aElement = a.get(i);
            JsonElement bElement = b.get(i);
            if (aElement.isJsonObject() && bElement.isJsonObject()) {
                if (!equalJsonObjects(aElement.getAsJsonObject(), bElement.getAsJsonObject())) return false;
            } else if (aElement.isJsonArray() && bElement.isJsonArray()) {
                if (!equalJsonArrays(aElement.getAsJsonArray(), bElement.getAsJsonArray())) return false;
            } else if (!aElement.equals(bElement)) return false;
        }
        return true;
    }

    public static boolean jsonKeysPresent(JsonObject jsonObject, String... attributes) {
        if (jsonObject == null || attributes == null) return false;
        for (String attribute : attributes) {
            if (!jsonObject.has(attribute)) return false;
        }
        return true;
    }

    public static void copyJsonAttrib(String key, JsonObject src, JsonObject dst) {
        if (src == null || dst == null) return;
        doCopyAttrib(key, src, dst);
    }

    private static void doCopyAttrib(String key, JsonObject src, JsonObject dst) {
        if (src.has(key)) {
            JsonElement object = src.get(key);
            if (object != null) {
                dst.add(key, object);
            }
        }
    }

    public static void copyJsonAttributes(JsonObject src, JsonObject dst, String... attributes) {
        if (src == null || dst == null || attributes == null) return;
        for (String attribute : attributes) {
            doCopyAttrib(attribute, src, dst);
        }
    }

    public static Optional<Integer> getOptInt(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key)) return Optional.empty();
        return Optional.of(jsonObject.get(key).getAsInt());
    }

    public static Integer getInt(JsonObject jsonObject, String key, Integer defaultValue) {
        if (jsonObject == null || !jsonObject.has(key)) return defaultValue;
        return jsonObject.get(key).getAsInt();
    }

    public static Optional<String> getOptString(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key)) return Optional.empty();
        return Optional.of(jsonObject.get(key).getAsString());
    }

    public static String getString(JsonObject jsonObject, String key, String defaultValue) {
        if (jsonObject == null || !jsonObject.has(key)) return defaultValue;
        return jsonObject.get(key).getAsString();
    }

    public static Optional<Boolean> getOptBool(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key)) return Optional.empty();
        return Optional.of(jsonObject.get(key).getAsBoolean());
    }

    public static Boolean getBoolean(JsonObject jsonObject, String key, Boolean defaultValue) {
        if (jsonObject == null || !jsonObject.has(key)) return defaultValue;
        return jsonObject.get(key).getAsBoolean();
    }

    public static Optional<JsonObject> getOptJSONObject(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key)) return Optional.empty();
        return Optional.of(jsonObject.get(key).getAsJsonObject());
    }

    public static Optional<JsonArray> getOptJSONArray(JsonObject jsonObject, String key) {
        if (jsonObject == null || !jsonObject.has(key)) return Optional.empty();
        return Optional.of(jsonObject.get(key).getAsJsonArray());
    }

    public static Optional<JsonObject> asJsonObject(JsonElement element) {
        if (element == null || !element.isJsonObject()) return Optional.empty();
        return Optional.of(element.getAsJsonObject());
    }

    public static Optional<JsonArray> asJsonArray(JsonElement element) {
        if (element == null || !element.isJsonArray()) return Optional.empty();
        return Optional.of(element.getAsJsonArray());
    }

    public static Optional<InputStream> getInputStream(HttpURLConnection connection) {
        try {
            return Optional.of(connection.getInputStream());
        } catch (IOException e) {
            try {
                return Optional.of(connection.getErrorStream());
            } catch (Exception ex) {}
        }

        return Optional.empty();
    }

    public static Optional<Integer> getResponseCode(HttpURLConnection connection) {
        try {
            return Optional.of(connection.getResponseCode());
        } catch (IOException e) {
            return Optional.empty();
        }
    }

    public static Optional<String> inputStreamToString(InputStream is) {
        if (is == null) return Optional.empty();
        StringBuilder result = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                result.append(line).append("\n");
            }
            is.close();
        } catch (IOException e) {
            return Optional.empty();
        }
        return Optional.of(result.toString());
    }
}
