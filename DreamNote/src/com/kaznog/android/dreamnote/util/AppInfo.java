package com.kaznog.android.dreamnote.util;

import com.kaznog.android.dreamnote.R;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Environment;
import android.util.Log;

public class AppInfo {
	public static boolean IsDebuggable(Context context) {
		PackageManager manager = context.getPackageManager();
		ApplicationInfo appinfo = null;
		try {
			appinfo = manager.getApplicationInfo(context.getPackageName(), 0);
		} catch (NameNotFoundException e) {
			return false;
		}
		if((appinfo.flags & ApplicationInfo.FLAG_DEBUGGABLE) == ApplicationInfo.FLAG_DEBUGGABLE) {
			return true;
		}
		return false;
	}
	public static void DebugLog(Context context, String log) {
		if(context != null) {
			if(AppInfo.IsDebuggable(context)) {
				Log.d(Constant.LOG_TAG, log);
			}
		}
	}
	public static String getVersionString(Context context) {
		int versionCode = 0;
		String versionName = "";
		String result = "";
        try {
            PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            versionCode = packageInfo.versionCode;
            versionName = packageInfo.versionName;
            result = String.format("Version %1$s (Build %2$s)", versionName, versionCode);
        } catch (NameNotFoundException e) {
            e.printStackTrace();
        }
        return result;
	}

	public static String getAppPath(Context context) {
		String appName = context.getResources().getString(R.string.app_name);
        return Environment.getExternalStorageDirectory().toString() + "/" + appName;
	}
}
