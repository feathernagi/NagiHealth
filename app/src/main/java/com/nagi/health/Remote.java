package com.nagi.health;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import org.json.JSONObject;

/** Загружает config.json из репозитория — так Claude меняет программу без пересборки. */
public class Remote {
    static final String REPO = "feathernagi/nagihealth";
    static final String CONFIG_URL = "https://raw.githubusercontent.com/" + REPO + "/main/config.json";

    static String http(String u) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(u).openConnection();
        c.setConnectTimeout(10000);
        c.setReadTimeout(15000);
        c.setRequestProperty("Accept", "application/vnd.github+json");
        c.setRequestProperty("User-Agent", "NagiHealth");
        c.setUseCaches(false);
        if (c.getResponseCode() != 200) throw new Exception("HTTP " + c.getResponseCode());
        try (InputStream in = c.getInputStream()) {
            ByteArrayOutputStream o = new ByteArrayOutputStream();
            byte[] b = new byte[8192]; int n;
            while ((n = in.read(b)) > 0) o.write(b, 0, n);
            return o.toString("UTF-8");
        }
    }

    static void fetchConfig(MainActivity a) {
        new Thread(() -> {
            try {
                String s = http(CONFIG_URL + "?t=" + System.currentTimeMillis());
                new JSONObject(s); // проверка, что это корректный JSON
                if (!s.equals(Store.get(a, "config/main"))) {
                    Store.put(a, "config/main", s);
                    a.runOnUiThread(a::configUpdated);
                }
            } catch (Exception ignored) {}
        }).start();
    }
}
