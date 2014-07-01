package com.kaznog.android.dreamnote.util;

import com.evernote.edam.userstore.Constants;

public class Constant {
	public static final String LOG_TAG = "DreamNote";

	public final static String CONTENT_TYPE_APPLICATION_ATOM = "application/atom+xml;type=entry";

	public static final int REQUEST_ACTIVITY_HOME = 0;
	public static final int REQUEST_ACTIVITY_VIEWER = 1;
	public static final int REQUEST_ACTIVITY_MEMO = 2;
	public static final int REQUEST_ACTIVITY_PHOTO = 3;
	public static final int REQUEST_ACTIVITY_TODO = 4;
	public static final int REQUEST_ACTIVITY_SEARCH = 5;
	public static final int REQUEST_ACTIVITY_MENU = 6;
	public static final int REQUEST_ACTIVITY_HELP = 7;
	public static final int REQUEST_ACTIVITY_CALENDAR = 8;
	public static final int REQUEST_ACTIVITY_MEMO_PREVIEW = 9;
	public static final int REQUEST_ACTIVITY_PHOTO_PREVIEW = 10;
	public static final int REQUEST_ACTIVITY_TODO_PREVIEW = 11;
	public static final int REQUEST_ACTIVITY_TODO_NEW_PREVIEW = 12;
	public static final int REQUEST_ACTIVITY_HTML_PREVIEW = 13;
	public static final int REQUEST_ACTIVITY_KW_PREVIEW = 14;
	public static final int REQUEST_ACTIVITY_SMARTCLIP = 15;
	public static final int REQUEST_ACTIVITY_GALLERY = 95;
	public static final int REQUEST_ACTIVITY_INTENTCAMERA = 96;
	public static final int REQUEST_ACTIVITY_PREFERENCE = 97;
	public static final int REQUEST_ACTIVITY_LOGIN = 98;
	public static final int REQUEST_ACTIVITY_SESSIONCHECKER = 99;

	public static final String REQUEST_ACTIVATE_MENU = "activate_menu";
	public static final String MENU_RESULT_EXTRA = "select_menu";
	public static final int MENU_SELECT_HOME = 0;
	public static final int MENU_SELECT_VIEWER = 1;
	public static final int MENU_SELECT_MEMO = 2;
	public static final int MENU_SELECT_PHOTO = 3;
	public static final int MENU_SELECT_TODO = 4;
	public static final int MENU_SELECT_SEARCH = 5;
	public static final int MENU_SELECT_MENU = 6;
	public static final int MENU_SELECT_HELP = 7;
	public static final int MENU_SELECT_CALENDAR = 8;
	public static final int MENU_SELECT_PREFERENCE = 9;
	public static final String ACTIVITY_RESULT_REQUEST = "result_request";
	public static final String FRAGMENT_RESULT_REQUEST = "result_request";
	public static final int ACTIVITY_RESULT_REQUEST_NON = -1;
	public static final int ACTIVITY_RESULT_LOGOUT = 97;
	public static final int ACTIVITY_RESULT_UPDATEPREFS = 98;
	public static final int ACTIVITY_RESULT_READDATA = 99;
	public static final int FRAGMENT_RESULT_REQUEST_NON = 100;
	public static final int FRAGMENT_RESULT_READDATA = 101;

	public static final int PREFS_MAXRESULTS_MAX = 20;
	public static final int PREFS_MAXRESULTS_MIN = 5;
	public static final String PREFS_PHOTO_ORIENTATION = "NOTE_PHOTOORIENTATION";
	public static final String PREFS_MAXRESULTS = "NOTE_MAXRESULTS";
	public static final String PREFS_ONLOWMEMORY = "NOTE_ONLOWMEMORY";
	public static final String PREFS_SPLASH = "NOTE_SPLASH";
	public static final String PREFS_HOME = "NOTE_HOME";
	public static final String PREFS_LARGETHUMBNAIL = "NOTE_LARGETHUMBNAIL";
	public static final String PREFS_JAVASCRIPT = "NOTE_JAVASCRIPT";
	public static final String PREFS_WEBCLIP_PIC_FORCE = "WEBCLIP_PIC_FORCE";
	public static final String PREFS_SPLASH_ACTIVE = "splash";
	public static final String PREFS_HOME_ACTIVE = "home";
	public static final String PREFS_LARGETHUMBNAIL_ACTIVE = "largethumbnail";
	public static final String PREFS_ONLOWMEMORY_ACTIVE = "toast";
	public static final String PREFS_PHOTO_ORIENTATION_ACTIVE = "rotate";
	public static final String PREFS_JAVASCRIPT_ACTIVE = "javascript_on";
	public static final String PREFS_WEBCLIP_PIC_FORCE_ACTIVE = "force_download";
	public static final String PREFS_EN_OAUTH_ID = "en_oauth_uid";
	public static final String PREFS_EN_OAUTH_WEB_API_URL_PREFIX = "en_oauth_webapiurl_prefix";
	public static final String PREFS_EN_OAUTH_NOTE_STORE_URL = "en_oauth_note_store_url";
	public static final String PREFS_EN_OAUTH_TOKEN = "en_oauth_token";
	public static final String KEY_AUTHTOKEN = "evernote.authToken";
	public static final String KEY_NOTESTOREURL = "evernote.notestoreUrl";
	public static final String KEY_WEBAPIURLPREFIX = "evernote.webApiUrlPrefix";
	public static final String KEY_USERID = "evernote.userId";

	public static final String PREFS_ENID = "enid";
	public static final String PREFS_VECTOR = "vector";
	public static final String PREFS_ATTR = "attribute";
	public static final String ONLOWMEMORY_TOASTMESSAGE = "ÉÅÉÇÉäÇ™ïsë´ÇµÇƒÇ¢Ç‹Ç∑";

	public static final String PREVIEW_CONTENT_NOT_FOUND_HEAD = "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" /><meta http-equiv=\"Content-Style-Type\" content=\"text/css\" /><meta name=\"viewport\" content=\"width=device-width; initial-scale=1.0; maximum-scale=1.0; user-scalable=no;\" /><meta name=\"MobileOptimized\" content=\"width\" /><meta name=\"HandheldFriendly\" content=\"true\" /><style type=\"text/css\">* {font-family: sans-serif; line-height: 1.0em; margin: 0px; padding: 0px;} html {height: 100%;} body {background-color: #ffffff; height: 100%;} #content {color: #333333; padding: 16px 16px 32px 16px;} #content p {line-height: 1.375em; margin-bottom: 16px;}</style></head><body><div id=\"content\">";
	public static final String PREVIEW_CONTENT_NOT_FOUND_FOOT = "</div></body></html>";
	public static final String PREVIEW_CONTENT_HEAD = "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\" /><meta http-equiv=\"Content-Style-Type\" content=\"text/css\" /><meta name=\"viewport\" content=\"width=device-width; initial-scale=1.0; maximum-scale=1.0; user-scalable=yes;\" /><meta name=\"MobileOptimized\" content=\"width\" /><meta name=\"HandheldFriendly\" content=\"true\" /><style type=\"text/css\">* {font-family: sans-serif; line-height: 1.0em; margin: 0px; padding: 0px;} html {height: 100%;} body {background-color: #ffffff; height: 100%;} #content {color: #333333; padding: 16px 16px 32px 16px;} #content p {line-height: 1.375em; margin-bottom: 16px;}</style></head><body>";
	public static final String PREVIEW_CONTENT_FOOT = "</body></html>";
	public static final String ADD_PIC_FILENAME = "picture_filename";

	public static final String EN_OAUTH_CONSUMER_KEY = "if connecting to the Evernote, replace this string with the Consumer Key";
	public static final String EN_OAUTH_CONSUMER_SECRET = "if connecting to the Evernote, replace this string with the Consumer Secret";

	public static final String EVERNOTE_HOST = "www.evernote.com";
	public static final String EN_USERSTORE_URL = "https://" + EVERNOTE_HOST + "/edam/user";
	public static final String EN_NOTESTORE_URL_BASE = "https://" + EVERNOTE_HOST + "/edam/note/";
	public static final String EN_USER_AGENT = "DreamNote(Android) " + Constants.EDAM_VERSION_MAJOR + "." + Constants.EDAM_VERSION_MINOR;

}
