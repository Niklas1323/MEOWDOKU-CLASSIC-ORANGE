package com.meowdoki.app;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.Locale;

/**
 * What the home-screen widgets show, read from the game's save (localStorage 'meowdoki.save.v1').
 * MainActivity copies the save here whenever the game goes to the background.
 */
final class WidgetData {
    static final int GOAL = 3; // puzzles per day for a streak day (MDK_GOAL in the game)
    static final int SKIN_COUNT = 14;
    private static final String[] SKINS = {"orange", "black", "white", "calico", "gray", "siamese", "cream",
            "chocolate", "blue", "lilac", "strawberry", "blueberry", "mint", "golden"};

    boolean german;
    int streak;
    int today;        // puzzles solved today
    int level = 1;    // next level to play
    boolean allDone;
    boolean dailyDone;
    int skin;         // index into the face sprite

    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("widgets", Context.MODE_PRIVATE);
    }

    /** Stores the game's save JSON (or null) and redraws every widget. */
    static void store(Context c, String saveJson) {
        if (saveJson == null || saveJson.isEmpty() || "null".equals(saveJson)) return;
        prefs(c).edit().putString("save", saveJson).apply();
        Widgets.updateAll(c);
    }

    static WidgetData load(Context c) {
        WidgetData d = new WidgetData();
        d.german = "de".equals(Locale.getDefault().getLanguage());
        String json = prefs(c).getString("save", null);
        if (json == null) return d;
        try {
            JSONObject s = new JSONObject(json);
            String lang = s.optString("lang", "");
            if ("de".equals(lang)) d.german = true;
            else if ("en".equals(lang)) d.german = false;
            d.level = Math.max(1, s.optInt("level", 1));
            d.allDone = s.optBoolean("allDone", false);
            String skin = s.optString("skin", "orange");
            for (int i = 0; i < SKINS.length; i++) if (SKINS[i].equals(skin)) d.skin = i;
            JSONObject played = s.optJSONObject("played");
            JSONObject done = s.optJSONObject("done");
            Calendar day = Calendar.getInstance();
            String todayKey = key(day);
            if (played != null) {
                d.today = played.optInt(todayKey, 0);
                // same rule as the game: today counts once the goal is met, otherwise start from yesterday
                if (d.today < GOAL) day.add(Calendar.DAY_OF_MONTH, -1);
                while (played.optInt(key(day), 0) >= GOAL) {
                    d.streak++;
                    day.add(Calendar.DAY_OF_MONTH, -1);
                }
            }
            d.dailyDone = done != null && truthy(done.opt(todayKey));
        } catch (Exception ignored) { }
        return d;
    }

    /* JavaScript truthiness for a JSON value */
    private static boolean truthy(Object v) {
        if (v == null || v == JSONObject.NULL || Boolean.FALSE.equals(v) || "".equals(v)) return false;
        return !(v instanceof Number) || ((Number) v).doubleValue() != 0;
    }

    private static String key(Calendar d) {
        return String.format(Locale.US, "%04d-%02d-%02d", d.get(Calendar.YEAR), d.get(Calendar.MONTH) + 1, d.get(Calendar.DAY_OF_MONTH));
    }
}
