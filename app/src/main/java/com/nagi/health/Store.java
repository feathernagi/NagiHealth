package com.nagi.health;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Map;

/** Простое хранилище документов: путь -> JSON-строка. */
public class Store {
    static SharedPreferences sp(Context c) { return c.getSharedPreferences("db", Context.MODE_PRIVATE); }
    static String get(Context c, String path) { return sp(c).getString(path, null); }
    static void put(Context c, String path, String json) { sp(c).edit().putString(path, json).apply(); }
    /** Все документы коллекции (например days) как [{id,data}] */
    static String list(Context c, String coll) {
        JSONArray a = new JSONArray();
        String pre = coll + "/";
        for (Map.Entry<String, ?> e : sp(c).getAll().entrySet()) {
            String k = e.getKey();
            if (k.startsWith(pre) && k.indexOf('/', pre.length()) < 0) {
                try {
                    JSONObject o = new JSONObject();
                    o.put("id", k.substring(pre.length()));
                    o.put("data", new JSONObject(String.valueOf(e.getValue())));
                    a.put(o);
                } catch (Exception ignored) {}
            }
        }
        return a.toString();
    }
}
