package com.kaznog.android.dreamnote.db;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.ContentUris;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteMisuseException;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;
import android.os.Environment;
import android.support.v4.database.DatabaseUtilsCompat;
import android.util.Log;

import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.db.schema.ItemsSchema;
import com.kaznog.android.dreamnote.db.schema.TagsSchema;
import com.kaznog.android.dreamnote.fragment.Item;
import com.kaznog.android.dreamnote.util.Constant;
import com.kaznog.android.dreamnote.util.StringUtils;
import com.kaznog.android.dreamnote.util.extendFileUtils;

public class DreamNoteProvider extends ContentProvider {
	public static final String AUTHORITY = "com.kaznog.android.dreamnote.dreamnoteprovider";
	public static final String ORDER_DATE_ASC = "DATE_ASC";
	public static final String ORDER_DATE_DESC = "DATE_DESC";
	public static final String ORDER_CREATE_ASC = "CREATE_ASC";
	public static final String ORDER_CREATE_DESC = "CREATE_DESC";
	public static final String ORDER_TITLE_ASC = "TITLE_ASC";
	public static final String ORDER_TITLE_DESC = "TITLE_DESC";
	public static final String ORDER_TODO_DEFAULT = "TODO_DEFAULT";
	private static final String ORDER_DATE_ASC_SQL = " ORDER BY " + ItemsSchema.DATE + " ASC";
	private static final String ORDER_DATE_DESC_SQL = " ORDER BY " + ItemsSchema.DATE + " DESC";
	private static final String ORDER_CREATE_ASC_SQL = " ORDER BY " + ItemsSchema.CREATED + " ASC";
	private static final String ORDER_CREATE_DESC_SQL = " ORDER BY " + ItemsSchema.CREATED + " DESC";
	private static final String ORDER_TITLE_ASC_SQL = " ORDER BY LOWER(" + ItemsSchema.TITLE + ") ASC";
	private static final String ORDER_TITLE_DESC_SQL = " ORDER BY LOWER(" + ItemsSchema.TITLE + ") DESC";
	/**
	 * コンテンツ種別
	 */
	public static enum ITEMTYPE {
		memo,
		photo,
		todo,
		todonew,
		html,
		todo_separator
	};
	private DatabaseHelper mOpenHelper;
	private final UriMatcher mUriMatcher;
//	private final HashMap<String, String> mItemsProjectionMap;
//	private final HashMap<String, String> mTagsProjectionMap;
	private static final int DATATYPE_HTML = ITEMTYPE.html.ordinal();
	private static final int ITEMS = 101;
	private static final int ITEMS_ID = 102;
	private static final int MEMOS = 103;
	private static final int PHOTOS = 104;
	private static final int TODOS = 105;
	private static final int HTMLS = 107;
	private static final int TAGS = 201;
	private static final int TAGS_ID = 202;
	private static final int TAGS_ITEMS = 203;
	private static final int CONTENTS = 301;
	private static final int CLIPRECOVERY = 311;
//	private static final String TABLE_ITEMS = ItemsSchema.TABLE_NAME;
//	private static final String TABLE_TAGS = TagsSchema.TABLE_NAME;
	private static final String QUERY_TAGS_BASE_SQL =
	"SELECT DISTINCT "
+	TagsSchema.TABLE_NAME + "." + TagsSchema.TERM
+	" FROM " + TagsSchema.TABLE_NAME;
	private final static String[] TAGRESULT_CURSOR = new String[] {
		"_id", TagsSchema.TERM
	};
	private static final String QUERY_ITEMS_BASE_SQL =
	"SELECT DISTINCT "
+	ItemsSchema.TABLE_NAME + "." + ItemsSchema.COLUMN_ID + ", "
+	ItemsSchema.TABLE_NAME + "." + ItemsSchema.DATATYPE + ", "
+	ItemsSchema.TABLE_NAME + "." + ItemsSchema.DATE + ", "
+	ItemsSchema.TABLE_NAME + "." + ItemsSchema.UPDATED + ", "
+	ItemsSchema.TABLE_NAME + "." + ItemsSchema.TITLE + ", "
+	"CASE WHEN " + ItemsSchema.TABLE_NAME + "." + ItemsSchema.DATATYPE + " = " + DATATYPE_HTML + " THEN \"\" ELSE " + ItemsSchema.TABLE_NAME + "." + ItemsSchema.CONTENT + " END, "
+	ItemsSchema.TABLE_NAME + "." + ItemsSchema.DESCRIPTION + ","
+	ItemsSchema.TABLE_NAME + "." + ItemsSchema.PATH + ","
+	ItemsSchema.TABLE_NAME + "." + ItemsSchema.RELATED + ","
+	ItemsSchema.TABLE_NAME + "." + ItemsSchema.CREATED
+	" FROM " + ItemsSchema.TABLE_NAME
+	" LEFT JOIN " + TagsSchema.TABLE_NAME + " ON " + ItemsSchema.TABLE_NAME + "." + ItemsSchema.COLUMN_ID + " = " + TagsSchema.TABLE_NAME + "." + TagsSchema.ITEM_ID
+	" WHERE 1 = 1";
	private static final String QUERY_ITEMS_NOFILTER_SQL =
	"SELECT DISTINCT "
+	ItemsSchema.TABLE_NAME + "." + ItemsSchema.COLUMN_ID + ", "
+	ItemsSchema.TABLE_NAME + "." + ItemsSchema.DATATYPE + ", "
+	ItemsSchema.TABLE_NAME + "." + ItemsSchema.DATE + ", "
+	ItemsSchema.TABLE_NAME + "." + ItemsSchema.UPDATED + ", "
+	ItemsSchema.TABLE_NAME + "." + ItemsSchema.TITLE + ", "
+	ItemsSchema.TABLE_NAME + "." + ItemsSchema.CONTENT + ", "
+	ItemsSchema.TABLE_NAME + "." + ItemsSchema.DESCRIPTION + ","
+	ItemsSchema.TABLE_NAME + "." + ItemsSchema.PATH + ","
+	ItemsSchema.TABLE_NAME + "." + ItemsSchema.RELATED + ","
+	ItemsSchema.TABLE_NAME + "." + ItemsSchema.CREATED
+	" FROM " + ItemsSchema.TABLE_NAME
+	" LEFT JOIN " + TagsSchema.TABLE_NAME + " ON " + ItemsSchema.TABLE_NAME + "." + ItemsSchema.COLUMN_ID + " = " + TagsSchema.TABLE_NAME + "." + TagsSchema.ITEM_ID
+	" WHERE 1 = 1";
	private static final String QUERY_ITEMS_GETCONTENT_SQL =
	"SELECT "
+	ItemsSchema.TABLE_NAME + "." + ItemsSchema.COLUMN_ID + ", "
+	ItemsSchema.TABLE_NAME + "." + ItemsSchema.CONTENT
+	" FROM " + ItemsSchema.TABLE_NAME;
	private final static String[] RESULT_CURSOR = new String[] {
			"_id",
			ItemsSchema.DATATYPE,
			ItemsSchema.LONG_DATE,
			ItemsSchema.DATE,
			ItemsSchema.LONG_UPDATED,
			ItemsSchema.UPDATED,
			ItemsSchema.TITLE,
			ItemsSchema.CONTENT,
			ItemsSchema.DESCRIPTION,
			ItemsSchema.PATH,
			ItemsSchema.RELATED,
			ItemsSchema.LONG_CREATED,
			ItemsSchema.CREATED,
			ItemsSchema.TAGS
	};
	public final static int ITEMTYPE_MEMO = ITEMTYPE.memo.ordinal();
	public final static int ITEMTYPE_PHOTO = ITEMTYPE.photo.ordinal();
	public final static int ITEMTYPE_TODO = ITEMTYPE.todo.ordinal();
	public final static int ITEMTYPE_TODONEW = ITEMTYPE.todonew.ordinal();
	public final static int ITEMTYPE_HTML = ITEMTYPE.html.ordinal();
	public final static int ITEMTYPE_TODOSEPARATOR = ITEMTYPE.todo_separator.ordinal();

	public static final Uri ITEMS_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/items");
	public static final Uri TAGS_CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/tags");
	public static final Uri GETCONTENT_URI = Uri.parse("content://" + AUTHORITY + "/contents");
	public static final Uri CLIPRECOVERY_URI = Uri.parse("content://" + AUTHORITY + "/cliprecovery");
	public DreamNoteProvider() {
		mUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		mUriMatcher.addURI(AUTHORITY, "items", ITEMS);
		mUriMatcher.addURI(AUTHORITY, "items/#", ITEMS_ID);
		mUriMatcher.addURI(AUTHORITY, "memos", MEMOS);
		mUriMatcher.addURI(AUTHORITY, "photos", PHOTOS);
		mUriMatcher.addURI(AUTHORITY, "todos", TODOS);
		mUriMatcher.addURI(AUTHORITY, "htmls", HTMLS);
		mUriMatcher.addURI(AUTHORITY, "tags", TAGS);
		mUriMatcher.addURI(AUTHORITY, "tags/#", TAGS_ID);
		mUriMatcher.addURI(AUTHORITY, "tags/#/items", TAGS_ITEMS);
		mUriMatcher.addURI(AUTHORITY, "contents/#", CONTENTS);
		mUriMatcher.addURI(AUTHORITY, "cliprecovery", CLIPRECOVERY);
/*
		mItemsProjectionMap = new HashMap<String, String>();
		mItemsProjectionMap.put(ItemsSchema.COLUMN_ID, ItemsSchema.TABLE_NAME + "." + ItemsSchema.COLUMN_ID);
		mItemsProjectionMap.put(ItemsSchema.DATATYPE, ItemsSchema.TABLE_NAME + "." + ItemsSchema.DATATYPE);
		mItemsProjectionMap.put(ItemsSchema.DATE, ItemsSchema.TABLE_NAME + "." + ItemsSchema.DATE);
		mItemsProjectionMap.put(ItemsSchema.UPDATED, ItemsSchema.TABLE_NAME + "." + ItemsSchema.UPDATED);
		mItemsProjectionMap.put(ItemsSchema.TITLE, ItemsSchema.TABLE_NAME + "." + ItemsSchema.TITLE);
		mItemsProjectionMap.put(ItemsSchema.CONTENT, "CASE WHEN " + ItemsSchema.TABLE_NAME + "." + ItemsSchema.DATATYPE + " = " + DATATYPE_HTML + " THEN \"\" ELSE " + ItemsSchema.TABLE_NAME + "." + ItemsSchema.CONTENT + " END");
		mItemsProjectionMap.put(ItemsSchema.DESCRIPTION, ItemsSchema.TABLE_NAME + "." + ItemsSchema.DESCRIPTION);
		mItemsProjectionMap.put(ItemsSchema.PATH, ItemsSchema.TABLE_NAME + "." + ItemsSchema.PATH);
		mItemsProjectionMap.put(ItemsSchema.RELATED, ItemsSchema.TABLE_NAME + "." + ItemsSchema.RELATED);

		mTagsProjectionMap = new HashMap<String, String>();
		mTagsProjectionMap.put(TagsSchema.COLUMN_ID, TagsSchema.TABLE_NAME + "." + TagsSchema.COLUMN_ID);
		mTagsProjectionMap.put(TagsSchema.ITEM_ID, TagsSchema.TABLE_NAME + "." + TagsSchema.ITEM_ID);
		mTagsProjectionMap.put(TagsSchema.TERM, TagsSchema.TABLE_NAME + "." + TagsSchema.TERM);
*/
	}

	@Override
	public boolean onCreate() {
		mOpenHelper = new DatabaseHelper(getContext());
		return (mOpenHelper.getWritableDatabase() != null);
	}

	@Override
	public String getType(Uri uri) {
		switch(mUriMatcher.match(uri)) {
		case ITEMS:
			return ItemsSchema.CONTENT_TYPE;
		case ITEMS_ID:
			return ItemsSchema.CONTENT_ITEM_TYPE;
		case TAGS:
			return TagsSchema.CONTENT_TYPE;
		case TAGS_ID:
			return TagsSchema.CONTENT_ITEM_TYPE;
		case TAGS_ITEMS:
			return TagsSchema.CONTENT_TYPE;
		case CONTENTS:
			return ItemsSchema.CONTENT_ITEM_TYPE;
		case CLIPRECOVERY:
			return ItemsSchema.CONTENT_TYPE;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	private MatrixCursor getTagsResult(Cursor c) {
		MatrixCursor result = new MatrixCursor(TAGRESULT_CURSOR);
		int resid = 0;
		while(c.moveToNext()) {
			String term = c.getString(0);
			result.addRow(new Object[] {
					resid,
					term
			});
			resid++;
		}
		c.close();
		return result;
	}
	private MatrixCursor getMatrixItems(SQLiteDatabase db, Cursor c) {
		MatrixCursor result = new MatrixCursor(RESULT_CURSOR);
		while(c.moveToNext()) {
			int id = c.getInt(0);
			int datatype = c.getInt(1);
			long long_date = c.getLong(2);
			long long_updated = c.getLong(3);
			String title = c.getString(4);
			String content = c.getString(5);
			String description = c.getString(6);
			String path = c.getString(7);
			String related = c.getString(8);
			long long_created = c.getLong(9);

			ArrayList<String> tags  = new ArrayList<String>();
			String selection = TagsSchema.ITEM_ID + " = " + id;
			Cursor tag_cursor = null;
			do {
				try {
					tag_cursor = db.query(TagsSchema.TABLE_NAME, TagsSchema.COLUMNS, selection, null, null, null, null);
				} catch(SQLiteMisuseException e) {
					tag_cursor = null;
				} catch(IllegalStateException ie) {
					tag_cursor = null;
				}
			} while(tag_cursor == null);
			while(tag_cursor.moveToNext()) {
				String tag = tag_cursor.getString(2);
				tags.add(tag);
			}
			tag_cursor.close();
			title = StringUtils.join(title.split("]]&gt;"), "]]>");
			if(datatype == ITEMTYPE_HTML) {
				title = title.replaceAll("\r\n", "");
				title = title.replaceAll("\r", "");
				title = title.replaceAll("\n", "");
				title = title.replaceAll("&amp;", "&");
				title = title.replaceAll("&lt;", "<");
				title = title.replaceAll("&gt;", ">");
				title = title.replaceAll("&quot;", "\"");
				title = title.replaceAll("&apos;", "\'");
				title = title.replaceAll("&nbsp;", " ");
			}
			content = StringUtils.join(content.split("]]&gt;"), "]]>");
			description = StringUtils.join(description.split("]]&gt;"), "]]>");
			Date date = new Date(long_date);
			Date updated = new Date(long_updated);
			Date created = new Date(long_created);
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
			String strdate = sdf.format(date);
			String strupdated = sdf.format(updated);
			String strcreated = sdf.format(created);
			result.addRow(
					new Object[]{
						id,
						datatype,
						long_date,
						strdate,
						long_updated,
						strupdated,
						title,
						content,
						description,
						path,
						related,
						long_created,
						strcreated,
						tags.toString()
					}
			);
		}
		c.close();
		return result;
	}
	private String getItemsWhereSql(String basesql, String where, String[] whereArgs) {
		if(StringUtils.isBlank(where) == false) {
			where = where.replaceAll("　", " ");
			String[] arrquery = where.split(" ");
			String sqlkey = "";
			for(String key: arrquery) {
				key = key.trim();
				if(key.equals("") == false) {
					sqlkey += " AND ("
						   +  ItemsSchema.TABLE_NAME + "." + ItemsSchema.TITLE + " LIKE '%" + key + "%'"
						   +  " OR " + ItemsSchema.TABLE_NAME + "." + ItemsSchema.CONTENT + " LIKE '%" + key + "%'"
						   +  " OR " + ItemsSchema.TABLE_NAME + "." + ItemsSchema.DESCRIPTION + " LIKE '%" + key + "%'"
						   +  " OR " + TagsSchema.TABLE_NAME + "." + TagsSchema.TERM + " LIKE '%" + key + "%'"
						   +  " )";
				}
			}
			basesql += " AND (" + sqlkey.substring(5) + ")";
		}
		if(whereArgs != null) {
			String tagkey = "";
			for(String tag: whereArgs) {
				tag = tag.trim();
				if(StringUtils.isBlank(tag)) { continue; }
				tagkey += " OR  " + TagsSchema.TABLE_NAME + "." + TagsSchema.TERM + " = '" + tag + "'";
			}
			if(tagkey.equals("") == false) {
				basesql += " AND (" + tagkey.substring(5) + ")";
			}
		}
		return basesql;
	}

	private String addOrderByString(String sortOrder) {
		String orderString = ORDER_DATE_DESC_SQL;
		if(sortOrder.equalsIgnoreCase(ORDER_DATE_ASC)) {
			orderString = ORDER_DATE_ASC_SQL;
		} else if(sortOrder.equalsIgnoreCase(ORDER_CREATE_ASC)) {
			orderString = ORDER_CREATE_ASC_SQL;
		} else if(sortOrder.equalsIgnoreCase(ORDER_CREATE_DESC)) {
			orderString = ORDER_CREATE_DESC_SQL;
		} else if(sortOrder.equalsIgnoreCase(ORDER_TITLE_ASC)) {
			orderString = ORDER_TITLE_ASC_SQL;
		} else if(sortOrder.equalsIgnoreCase(ORDER_TITLE_DESC)) {
			orderString = ORDER_TITLE_DESC_SQL;
		}
		return orderString;
	}

	private MatrixCursor queryToDoDefault(SQLiteDatabase db, String where, String[] whereArgs) {
		MatrixCursor result = null;
		Cursor c = null;
		String sql;

		sql = QUERY_ITEMS_BASE_SQL;
		sql += " AND (" + ItemsSchema.TABLE_NAME + "." + ItemsSchema.DATATYPE + " = 2 OR " + ItemsSchema.TABLE_NAME + "." +ItemsSchema.DATATYPE + " = 3)";
		sql = getItemsWhereSql(sql, where, whereArgs);
		sql += " ORDER BY " + ItemsSchema.UPDATED + " DESC";
		do {
			try {
				c = db.rawQuery(sql, null);
			} catch(SQLiteMisuseException e) {
				c = null;
			} catch(IllegalStateException ie) {
				c = null;
			}
		} while(c == null);
		// todo用の処理を入れる
		result = getMatrixItems(db, c);
		ArrayList<Item> arrItems = new ArrayList<Item>();
		ArrayList<Item> doneItems = new ArrayList<Item>();
		while(result.moveToNext()) {
			Item item = new Item();
			item.id = result.getInt(0);
			item.datatype = result.getInt(1);
			item.long_date = result.getLong(2);
			item.date = result.getString(3);
			item.long_updated = result.getLong(4);
			item.updated = result.getString(5);
			item.title = result.getString(6);
			item.content = result.getString(7);
			item.description = result.getString(8);
			item.path = result.getString(9);
			item.related = result.getString(10);
			item.long_created = result.getLong(11);
			item.created = result.getString(12);
			item.tags = result.getString(13);
			if(item.datatype == DreamNoteProvider.ITEMTYPE_TODONEW) {
				arrItems.add(item);
			} else {
				doneItems.add(item);
			}
		}
		result.close();
		// updatedの日付の差分を見てセパレータを入れていく
		String title_pre = "";
		Calendar dayNowCal = Calendar.getInstance();
		dayNowCal.set(Calendar.HOUR_OF_DAY, 0);
		dayNowCal.set(Calendar.MINUTE, 0);
		dayNowCal.set(Calendar.SECOND, 0);
		for(int i = 0; i < arrItems.size(); i++) {
			Item item = arrItems.get(i);
			String[] arrupdated = item.updated.split(" ");
			String[] arrdate = arrupdated[0].split("/");
			int year = Integer.parseInt(arrdate[0]);
			int month = Integer.parseInt(arrdate[1]);
			int day = Integer.parseInt(arrdate[2]);
			Calendar dayDataCal = Calendar.getInstance();
			dayDataCal.set(Calendar.YEAR, year);
			dayDataCal.set(Calendar.MONTH, month - 1);
			dayDataCal.set(Calendar.DAY_OF_MONTH, day);
			dayDataCal.set(Calendar.HOUR_OF_DAY, 0);
			dayDataCal.set(Calendar.MINUTE, 0);
			dayDataCal.set(Calendar.SECOND, 0);
			int dayDiff = Math.round((dayDataCal.getTimeInMillis() - dayNowCal.getTimeInMillis()) / 86400000.0f);
			String title = "";
			if(item.updated.substring(0, 4).equals("0001")) {
				title = getContext().getResources().getString(R.string.todo_separator_title_nonlimit);
			} else if(dayDiff <= -1) {
				title = getContext().getResources().getString(R.string.todo_separator_title_expiration);
			} else if(dayDiff > 7) {
				title = getContext().getResources().getString(R.string.todo_separator_title_future);
			} else if(dayDiff > 1) {
				title = getContext().getResources().getString(R.string.todo_separator_title_next7days);
			} else if(dayDiff == 1) {
				title = getContext().getResources().getString(R.string.todo_separator_title_tomorrow);
			} else if(dayDiff == 0) {
				title = getContext().getResources().getString(R.string.todo_separator_title_today);
			}
			if(title.equals("") == false && title_pre.equals(title) == false) {
				Item separator = new Item();
				separator.datatype = DreamNoteProvider.ITEMTYPE_TODOSEPARATOR;
				separator.title = title;
				arrItems.add(i, separator);
				title_pre = title;
				i++;
			}
		}
		// 完了したtodoは更新日時順にする
		Collections.sort(doneItems, new Comparator<Item>() {
			public int compare(Item item1, Item item2) {
				if(item1.long_date < item2.long_date) return 1;
				if(item1.long_date > item2.long_date) return -1;
				return 0;
			}
		});
		arrItems.addAll(doneItems);
		doneItems.clear();
		result = new MatrixCursor(DreamNoteProvider.RESULT_CURSOR);
		for(int i = 0; i < arrItems.size(); i++) {
			Item item = arrItems.get(i);
			result.addRow(
				new Object[] {
					item.id,
					item.datatype,
					item.long_date,
					item.date,
					item.long_updated,
					item.updated,
					item.title,
					item.content,
					item.description,
					item.path,
					item.related,
					item.long_created,
					item.created,
					item.tags
				}
			);
		}
		return result;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String where, String[] whereArgs, String sortOrder) {
		String sql = "";
		Cursor c = null;
		MatrixCursor result = null;
		SQLiteDatabase db = mOpenHelper.getReadableDatabase();
//		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		switch(mUriMatcher.match(uri)) {
		case ITEMS:
			sql = QUERY_ITEMS_BASE_SQL;
			sql = getItemsWhereSql(sql, where, whereArgs);
			sql += addOrderByString(sortOrder);
//			Log.d(Constant.LOG_TAG, "Provider Query all: " + sql);
			do {
				try {
					c = db.rawQuery(sql, null);
				} catch(SQLiteMisuseException e) {
					c = null;
				} catch(IllegalStateException ie) {
					c = null;
				}
			} while(c == null);
			result = getMatrixItems(db, c);
			result.setNotificationUri(getContext().getContentResolver(), uri);
			return result;
		case MEMOS:
			sql = QUERY_ITEMS_BASE_SQL;
			sql += " AND " + ItemsSchema.TABLE_NAME + "." + ItemsSchema.DATATYPE + " = 0";
			sql = getItemsWhereSql(sql, where, whereArgs);
			sql += addOrderByString(sortOrder);
//			Log.d(Constant.LOG_TAG, "Provider Query memo: " + sql);
			do {
				try {
					c = db.rawQuery(sql, null);
				} catch(SQLiteMisuseException e) {
					c = null;
				} catch(IllegalStateException ie) {
					c = null;
				}
			} while(c == null);
			result = getMatrixItems(db, c);
			result.setNotificationUri(getContext().getContentResolver(), uri);
			return result;
		case PHOTOS:
			sql = QUERY_ITEMS_BASE_SQL;
			sql += " AND " + ItemsSchema.TABLE_NAME + "." + ItemsSchema.DATATYPE + " = 1";
			sql = getItemsWhereSql(sql, where, whereArgs);
			sql += addOrderByString(sortOrder);
//			Log.d(Constant.LOG_TAG, "Provider Query photo: " + sql);
			do {
				try {
					c = db.rawQuery(sql, null);
				} catch(SQLiteMisuseException e) {
					c = null;
				} catch(IllegalStateException ie) {
					c = null;
				}
			} while(c == null);
			result = getMatrixItems(db, c);
			result.setNotificationUri(getContext().getContentResolver(), uri);
			return result;
		case TODOS:
			if(sortOrder.equalsIgnoreCase(ORDER_TODO_DEFAULT)) {
				result = queryToDoDefault(db, where, whereArgs);
			} else {
				sql = QUERY_ITEMS_BASE_SQL;
				sql += " AND (" + ItemsSchema.TABLE_NAME + "." + ItemsSchema.DATATYPE + " = 2 OR " + ItemsSchema.TABLE_NAME + "." +ItemsSchema.DATATYPE + " = 3)";
				sql = getItemsWhereSql(sql, where, whereArgs);
				sql += addOrderByString(sortOrder);
				do {
					try {
						c = db.rawQuery(sql, null);
					} catch(SQLiteMisuseException e) {
						c = null;
					} catch(IllegalStateException ie) {
						c = null;
					}
				} while(c == null);
				result = getMatrixItems(db, c);
			}
			result.setNotificationUri(getContext().getContentResolver(), uri);
			return result;
		case HTMLS:
			sql = QUERY_ITEMS_BASE_SQL;
			sql += " AND " + ItemsSchema.TABLE_NAME + "." + ItemsSchema.DATATYPE + " = 4";
			sql = getItemsWhereSql(sql, where, whereArgs);
			sql += addOrderByString(sortOrder);
			do {
				try {
					c = db.rawQuery(sql, null);
				} catch(SQLiteMisuseException e) {
					c = null;
				} catch(IllegalStateException ie) {
					c = null;
				}
			} while(c == null);
			result = getMatrixItems(db, c);
			result.setNotificationUri(getContext().getContentResolver(), uri);
			return result;
		case ITEMS_ID:
			sql = QUERY_ITEMS_BASE_SQL;
			sql += " AND " + ItemsSchema.TABLE_NAME + "." + ItemsSchema.COLUMN_ID + "=" + uri.getPathSegments().get(1);
			do {
				try {
					c = db.rawQuery(sql, null);
				} catch(SQLiteMisuseException e) {
					c = null;
				} catch(IllegalStateException ie) {
					c = null;
				}
			} while(c == null);
			result = getMatrixItems(db, c);
			result.setNotificationUri(getContext().getContentResolver(), uri);
			return result;
		case CONTENTS:
			sql = QUERY_ITEMS_GETCONTENT_SQL;
			sql += " WHERE " + ItemsSchema.TABLE_NAME + "." + ItemsSchema.COLUMN_ID + "=" + uri.getPathSegments().get(1);
			do {
				try {
					c = db.rawQuery(sql, null);
				} catch(SQLiteMisuseException e) {
					c = null;
				} catch(IllegalStateException ie) {
					c = null;
				}
			} while(c == null);
			c.setNotificationUri(getContext().getContentResolver(), uri);
			return c;

		case TAGS:
			sql = QUERY_TAGS_BASE_SQL;
			do {
				try {
					c = db.rawQuery(sql, null);
				} catch(SQLiteMisuseException e) {
					c = null;
				} catch(IllegalStateException ie) {
					c = null;
				}
			} while(c == null);
			result = getTagsResult(c);
			result.setNotificationUri(getContext().getContentResolver(), uri);
			return result;
		case TAGS_ID:
			sql = QUERY_TAGS_BASE_SQL;
			sql += " WHERE " + TagsSchema.TABLE_NAME + "." + TagsSchema.COLUMN_ID + "=" + uri.getPathSegments().get(1);
			do {
				try {
					c = db.rawQuery(sql, null);
				} catch(SQLiteMisuseException e) {
					c = null;
				} catch(IllegalStateException ie) {
					c = null;
				}
			} while(c == null);
			result = getTagsResult(c);
			result.setNotificationUri(getContext().getContentResolver(), uri);
			return result;
		case TAGS_ITEMS:
			sql = QUERY_TAGS_BASE_SQL;
			sql += " WHERE " + TagsSchema.TABLE_NAME + "." + TagsSchema.ITEM_ID + "=" + uri.getPathSegments().get(1);
			do {
				try {
					c = db.rawQuery(sql, null);
				} catch(SQLiteMisuseException e) {
					c = null;
				} catch(IllegalStateException ie) {
					c = null;
				}
			} while(c == null);
			result = getTagsResult(c);
			result.setNotificationUri(getContext().getContentResolver(), uri);
			return result;
		}
//		DatabaseUtils.dumpCursor(c);
//		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	private void tagsinsert(SQLiteDatabase db, long itemid, String strtags) {
		if(strtags != null && strtags.trim().length() != 0) {
			String[] arrtags = strtags.split(",");
			for(String tag : arrtags) {
				tag = tag.trim();
				if(tag.equals("") == false) {
					ContentValues tagvalues = new ContentValues();
					tagvalues.put(TagsSchema.ITEM_ID, itemid);
					tagvalues.put(TagsSchema.TERM, tag);
					db.insert(TagsSchema.TABLE_NAME, null, tagvalues);
					tagvalues.clear();
					tagvalues = null;
				}
			}
		}
	}
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		switch(mUriMatcher.match(uri)) {
		case ITEMS:
			String strtags = values.getAsString(ItemsSchema.TAGS);
			if(strtags != null) {
				values.remove(ItemsSchema.TAGS);
			}
			long itemid = 0;
			db.beginTransaction();
			try {
				itemid = db.insert(ItemsSchema.TABLE_NAME, null, values);
				if(itemid > 0) {
					tagsinsert(db, itemid, strtags);
					db.setTransactionSuccessful();
				} else {
					throw new SQLException("Failed to insert row into " + uri);
				}
			} catch(SQLException e) {
				e.printStackTrace();
			} finally {
				db.endTransaction();
			}
			db.close();
			if(itemid > 0) {
				Uri noti = ContentUris.withAppendedId(Uri.parse("content://" + AUTHORITY + "/" + ItemsSchema.TABLE_NAME + "/"), itemid);
				getContext().getContentResolver().notifyChange(noti, null);
				String strtype = "";
				int type = values.getAsInteger(ItemsSchema.DATATYPE);
				if(type == ITEMTYPE_MEMO) {
					strtype = "memos";
				} else if(type == ITEMTYPE_PHOTO) {
					strtype = "photos";
				} else if(type == ITEMTYPE_TODONEW) {
					strtype = "todos";
				} else if(type == ITEMTYPE_HTML) {
					strtype = "htmls";
				}
				Uri typenoti = ContentUris.withAppendedId(Uri.parse("content://" + AUTHORITY + "/" + strtype + "/"), itemid);
				getContext().getContentResolver().notifyChange(typenoti, null);
				getContext().getContentResolver().notifyChange(Uri.parse("content://" + AUTHORITY + "/" + TagsSchema.TABLE_NAME + "/"), null);
				return noti;
			}
			break;
		case TAGS:
			long tagid = db.insert(TagsSchema.TABLE_NAME, null, values);
			db.close();
			if(tagid > 0) {
				Uri noti = ContentUris.withAppendedId(Uri.parse("content://" + AUTHORITY + "/" + TagsSchema.TABLE_NAME + "/"), tagid);
				getContext().getContentResolver().notifyChange(noti, null);
				return noti;
			}
			throw new SQLException("Failed to insert row into " + uri);
		case CLIPRECOVERY:
			String sql = "SELECT " + ItemsSchema.PATH + " FROM " + ItemsSchema.TABLE_NAME;
			Cursor c = null;
			do {
				try {
					c = db.rawQuery(sql, null);
				} catch(SQLiteMisuseException e) {
					c = null;
				} catch(IllegalStateException ie) {
					c = null;
				}
			} while(c == null);
			// アプリケーション名を取得
	        String appName = getContext().getResources().getString(R.string.app_name);
			String clippath = Environment.getExternalStorageDirectory().toString() + "/" + appName + "/.clip";
			String[] clipfolders = new File(clippath).list();
			if(clipfolders == null || clipfolders.length == 0) {
				c.close();
				db.close();
				return null;
			}
			ArrayList<String> recordFolders = new ArrayList<String>();
			try {
				while(c.moveToNext()) {
					recordFolders.add(c.getString(0));
				}
			} catch(Exception e) {
			}
			c.close();
			for(String clipfoldername : clipfolders) {
				if(StringUtils.isBlank(clipfoldername)) { continue; }
				if(clipfoldername.equals(".") || clipfoldername.equals("..")) { continue; }
				File recoveryfolder = new File(clippath + "/" + clipfoldername);
				if(!recoveryfolder.isDirectory()) { continue; }
				if(recordFolders.indexOf(clipfoldername) == -1) {
					File recoveryfile = new File(clippath + "/" + clipfoldername + "/index.html");
					String title = getContext().getResources().getString(R.string.clip_nontitle);
					String preview_content = "";
					if(recoveryfile.exists()) {
			        	preview_content = extendFileUtils.readStringFile(recoveryfile, "utf-8");
			        	Matcher m = Pattern.compile("(<title>)(.+?)(</title>)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(preview_content);
			        	if(m.find()) {
			        		title = m.group(2);
			        	}
						m = Pattern.compile("(<head)(.+?)(</head>)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(preview_content);
						if(m.find()) {
							preview_content = m.replaceAll("");
						}
						m = Pattern.compile("(<style)(.+?)(</style>)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(preview_content);
						if(m.find()) {
							preview_content = m.replaceAll("");
						}
						m = Pattern.compile("(<script)(.+?)(</script>)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(preview_content);
						if(m.find()) {
							preview_content = m.replaceAll("");
						}
						m = Pattern.compile("(//<\\!\\[CDATA\\[)(.+?)(//\\]\\]>)", Pattern.DOTALL).matcher(preview_content);
						if(m.find()) {
							preview_content = m.replaceAll("");
						}
						m = Pattern.compile("<br([^/>](/>|>)|(/>|>))", Pattern.CASE_INSENSITIVE).matcher(preview_content);
						if(m.find()) {
							preview_content = m.replaceAll("\n");
						}
						m = Pattern.compile("</div>", Pattern.CASE_INSENSITIVE).matcher(preview_content);
						if(m.find()) {
							preview_content = m.replaceAll("\n");
						}
						m = Pattern.compile("</h\\d+([^>]>|>)", Pattern.CASE_INSENSITIVE).matcher(preview_content);
						if(m.find()) {
							preview_content = m.replaceAll("\n");
						}
						m = Pattern.compile("<(\"[^\"]*\"|'[^']*'|[^'\">])*>", Pattern.CASE_INSENSITIVE).matcher(preview_content);
						if(m.find()) {
							preview_content = m.replaceAll("");
						}
						m = Pattern.compile("'", Pattern.CASE_INSENSITIVE).matcher(preview_content);
						if(m.find()) {
							preview_content = m.replaceAll("&apos;");
						}
						preview_content = preview_content.replaceAll("^[\\s　]*", "").replaceAll("[\\s　]*$", "");
						preview_content = preview_content.trim();
						String[] arrbody = preview_content.split("\n");
						StringBuilder sb = new StringBuilder();
						for(String line: arrbody) {
							if(StringUtils.isBlank(line)) { continue; }
							if(line.equals("\n")) { continue; }
							sb.append(line);
							sb.append("\n\n");
						}
						//sb.deleteCharAt(sb.length() - 1);
						preview_content = sb.toString();
						sb.setLength(0);
						sb = null;
					} else {
						File recoveryclipbody = new File(clippath + "/" + clipfoldername + "/clipbody.jpg");
						if(!recoveryclipbody.exists()) { continue; }
						preview_content = "";
					}
	        		Date now = new Date();
	            	ContentValues clipvalues = new ContentValues();
	            	clipvalues.put(ItemsSchema.DATATYPE, DreamNoteProvider.ITEMTYPE_HTML);
	            	clipvalues.put(ItemsSchema.DATE, now.getTime());
	            	clipvalues.put(ItemsSchema.UPDATED, now.getTime());
	            	clipvalues.put(ItemsSchema.CREATED, now.getTime());
	            	clipvalues.put(ItemsSchema.TITLE, title);
	            	clipvalues.put(ItemsSchema.CONTENT, preview_content);
	            	clipvalues.put(ItemsSchema.DESCRIPTION, "");
	            	clipvalues.put(ItemsSchema.PATH, clipfoldername);
	            	clipvalues.put(ItemsSchema.RELATED, "");
	    			long clip_itemid = 0;
	    			db.beginTransaction();
	    			try {
	    				clip_itemid = db.insert(ItemsSchema.TABLE_NAME, null, clipvalues);
	    				if(clip_itemid > 0) {
	    					db.setTransactionSuccessful();
	    				} else {
	    					throw new SQLException("Failed to insert row into " + uri);
	    				}
	    			} catch(SQLException e) {
	    				e.printStackTrace();
	    			} finally {
	    				db.endTransaction();
	    			}
				}
			}
			db.close();
			Uri noti = Uri.parse("content://" + AUTHORITY + "/items");
			getContext().getContentResolver().notifyChange(noti, null);
			getContext().getContentResolver().notifyChange(Uri.parse("content://" + AUTHORITY + "/htmls"), null);
			return noti;
		default:
			db.close();
			throw new IllegalArgumentException("Unknown URL " + uri);
		}
		return null;
	}

	@Override
	public int update(Uri uri, ContentValues values, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count = 0;
		String finalWhere;
		switch(mUriMatcher.match(uri)) {
		case ITEMS:
			count = db.update(ItemsSchema.TABLE_NAME, values, where, whereArgs);
			break;
		case ITEMS_ID:
			String strtags = values.getAsString(ItemsSchema.TAGS);
			if(strtags != null) {
				values.remove(ItemsSchema.TAGS);
			}
			long itemid = ContentUris.parseId(uri);
			finalWhere = DatabaseUtilsCompat.concatenateWhere(ItemsSchema.COLUMN_ID + "=" + itemid, where);
			db.beginTransaction();
			try {
				count = db.update(ItemsSchema.TABLE_NAME, values, finalWhere, whereArgs);
				db.delete(TagsSchema.TABLE_NAME, TagsSchema.ITEM_ID + "=" + itemid, null);
				tagsinsert(db, itemid, strtags);
				db.setTransactionSuccessful();
			} catch(SQLException e) {
			} finally {
				db.endTransaction();
			}

			String strtype = "";
			int type = values.getAsInteger(ItemsSchema.DATATYPE);
			if(type == ITEMTYPE_MEMO) {
				strtype = "memos";
			} else if(type == ITEMTYPE_PHOTO) {
				strtype = "photos";
			} else if(type == ITEMTYPE_TODO) {
				strtype = "todos";
			} else if(type == ITEMTYPE_TODONEW) {
				strtype = "todos";
			} else if(type == ITEMTYPE_HTML) {
				strtype = "htmls";
			}
			Uri typenoti = ContentUris.withAppendedId(Uri.parse("content://" + AUTHORITY + "/" + strtype + "/"), itemid);
			getContext().getContentResolver().notifyChange(typenoti, null);
			getContext().getContentResolver().notifyChange(Uri.parse("content://" + AUTHORITY + "/" + TagsSchema.TABLE_NAME + "/"), null);
			break;
		case TAGS:
			count = db.update(TagsSchema.TABLE_NAME, values, where, whereArgs);
			break;
		case TAGS_ID:
			long tagid = ContentUris.parseId(uri);
			finalWhere = DatabaseUtilsCompat.concatenateWhere(TagsSchema.COLUMN_ID + "=" + tagid, where);
			count = db.update(TagsSchema.TABLE_NAME, values, finalWhere, whereArgs);
			break;
		default:
			throw new IllegalArgumentException("Unknown URL " + uri);
		}
		db.close();
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public int delete(Uri uri, String where, String[] whereArgs) {
		SQLiteDatabase db = mOpenHelper.getWritableDatabase();
		int count = 0;
		switch(mUriMatcher.match(uri)) {
		case ITEMS:
			db.beginTransaction();
			try {
				String strWhere = "_ID IN (" + whereArgs[0] + ")";
				count = db.delete(ItemsSchema.TABLE_NAME, strWhere, null);
				String[] arrIds = whereArgs[0].split(",");
				for(String itemid: arrIds) {
					String trimid = itemid.trim();
					if(trimid.equals("")) continue;
					db.delete(TagsSchema.TABLE_NAME, TagsSchema.ITEM_ID + "=" + trimid, null);
				}
				db.setTransactionSuccessful();
			} catch(SQLiteException e) {

			} finally {
				db.endTransaction();
			}
			String[] arrUris = whereArgs[1].split(",");
			for(String struri: arrUris) {
				String strtype = struri.trim();
				if(strtype.equals("")) continue;
				getContext().getContentResolver().notifyChange(Uri.parse("content://" + AUTHORITY + "/" + strtype + "/"), null);
			}
			getContext().getContentResolver().notifyChange(Uri.parse("content://" + AUTHORITY + "/" + TagsSchema.TABLE_NAME + "/"), null);
			break;
		case ITEMS_ID:
			long itemid = ContentUris.parseId(uri);
			db.beginTransaction();
			try {
				count = db.delete(ItemsSchema.TABLE_NAME, ItemsSchema.COLUMN_ID + "=" + itemid, null);
				db.delete(TagsSchema.TABLE_NAME, TagsSchema.ITEM_ID + "=" + itemid, null);
				db.setTransactionSuccessful();
			} catch(SQLiteException e) {

			} finally {
				db.endTransaction();
			}
			break;
		case TAGS:
			count = db.delete(TagsSchema.TABLE_NAME, where, whereArgs);
			break;
		case TAGS_ID:
			long tagid = ContentUris.parseId(uri);
			count = db.delete(TagsSchema.TABLE_NAME, TagsSchema.COLUMN_ID + "=" + tagid, null);
			break;
		default:
			throw new IllegalArgumentException("Unknown URL " + uri);
		}
		db.close();
		getContext().getContentResolver().notifyChange(uri, null, false);
		return count;
	}

	private static class DatabaseHelper extends SQLiteOpenHelper {
		private static final String DATABASE_NAME = "dreamnote";
		private static final int DATABASE_VERSION = 2;
		private static final String CREATE_ITEMS_TABLE_SQL =
				"CREATE TABLE IF NOT EXISTS items ("
			+	"_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
			+	"datatype INTEGER NOT NULL DEFAULT 0, "
			+	"date INTEGER NOT NULL, "
			+	"updated INTEGER NOT NULL, "
			+	"title TEXT NOT NULL, "
			+	"content TEXT NOT NULL, "
			+	"description TEXT NOT NULL, "
			+	"path TEXT, "
			+	"related TEXT, "
			+	"created INTEGER NOT NULL DEFAULT 0, "
			+	"favorite INTEGER NOT NULL DEFAULT 0 "
			+	")";
		private static final String CREATE_TAGS_TABLE_SQL =
			"CREATE TABLE IF NOT EXISTS tags ("
		+	"_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
		+	"item_id INTEGER NOT NULL, "
		+	"term TEXT NOT NULL "
		+	")";
		@SuppressWarnings("unused")
		private static final String DROP_ITEMS_TABLE_SQL =
			"DROP TABLE if exists items";
		@SuppressWarnings("unused")
		private static final String DROP_TAGS_TABLE_SQL =
			"DROP TABLE if exists tags";

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
		}
		@Override
		public void onCreate(SQLiteDatabase db) {
			// DBが存在しない場合、自動的に呼び出される
			db.beginTransaction();
			try {
				db.execSQL(CREATE_ITEMS_TABLE_SQL);
				db.execSQL(CREATE_TAGS_TABLE_SQL);
				db.setTransactionSuccessful();
			} catch(SQLiteException e) {
			} finally {
				db.endTransaction();
			}
		}
		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion,
				int newVersion) {
			if(oldVersion == 1 && newVersion == 2) {
				db.beginTransaction();
				try {
					db.execSQL("ALTER TABLE items ADD COLUMN created INTEGER NOT NULL DEFAULT 0");
					db.execSQL("UPDATE items SET created = date");
					db.execSQL("ALTER TABLE items ADD COLUMN favorite INTEGER NOT NULL DEFAULT 0");
					db.setTransactionSuccessful();
					Log.i(Constant.LOG_TAG, "Database onUpgrade Success!");
				} catch(SQLiteException e) {
				} finally {
					db.endTransaction();
				}
			} else {
				Log.d(Constant.LOG_TAG, "DatabaseHelper onUpgrade Unknown Version");
/*
				db.execSQL(DROP_ITEMS_TABLE_SQL);
				db.execSQL(DROP_TAGS_TABLE_SQL);
				onCreate(db);
*/
			}
		}
	}
}
