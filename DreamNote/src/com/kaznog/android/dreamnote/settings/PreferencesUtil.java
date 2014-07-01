package com.kaznog.android.dreamnote.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class PreferencesUtil {
	public static final SharedPreferences getSharedPreferences(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context);
	}
    /**
     * プリファレンス加算情報設定(int)
     */
    public static final void countUpPreferences(Context context, String key, int def, int value) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        value += settings.getInt(key, def);

        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    /**
     * プリファレンス情報設定(int)
     */
    public static final void setPreferences(Context context, String key, int value) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        editor.commit();
    }

    /**
     * プリファレンス情報設定(long)
     */
    public static final void setPreferences(Context context, String key, long value) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putLong(key, value);
        editor.commit();
    }

    /**
     * プリファレンス情報設定(string)
     */
    public static final void setPreferences(Context context, String key, String value) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        editor.commit();
    }

    /**
     * プリファレンス情報取得(int)
     */
    public static final int getPreferences(Context context, String key, int value) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getInt(key, value);
    }

    /**
     * プリファレンス情報取得(long)
     */
    public static final long getPreferences(Context context, String key, long value) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getLong(key, value);
    }

    /**
     * プリファレンス情報取得(string)
     */
    public static final String getPreferences(Context context, String key, String value) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        return settings.getString(key, value);
    }

    /**
     * プリファレンス情報削除(string)
     */
    public static final void removePreferences(Context context, String key) {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = settings.edit();
        editor.remove(key);
        editor.commit();
    }
}
