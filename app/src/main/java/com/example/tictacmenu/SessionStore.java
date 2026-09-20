package com.example.tictacmenu;

import android.content.Context;
import android.content.SharedPreferences;

final class SessionStore {
    private static final String PREFS_NAME = "PREFS_NAME";
    private static final String KEY_STAY_CONNECTED = "stayConnect";
    private static final String KEY_DISPLAY_NAME = "displayName";

    private SessionStore() {
    }

    static void signIn(Context context, String displayName, boolean remember) {
        preferences(context).edit()
                .putString(KEY_DISPLAY_NAME, displayName)
                .putBoolean(KEY_STAY_CONNECTED, remember)
                .apply();
    }

    static String displayName(Context context) {
        return preferences(context).getString(KEY_DISPLAY_NAME, "");
    }

    static boolean shouldRestore(Context context) {
        return preferences(context).getBoolean(KEY_STAY_CONNECTED, false)
                && !displayName(context).isEmpty();
    }

    static void signOut(Context context) {
        preferences(context).edit().clear().apply();
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
