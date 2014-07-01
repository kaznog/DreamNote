/**
 *
 */
package com.kaznog.android.dreamnote.db.schema;

/**
 * @author kaznog
 *
 */
public interface ItemsSchema {
	// テーブル名
	String TABLE_NAME = "items";
	// カラム名
	String COLUMN_ID = "_ID";
	String DATATYPE = "datatype";
	String LONG_DATE = "long_date";
	String DATE = "date";
	String LONG_UPDATED = "long_updated";
	String UPDATED = "updated";
	String TITLE = "title";
	String CONTENT = "content";
	String DESCRIPTION = "description";
	String PATH = "path";
	String RELATED = "related";
	String LONG_CREATED = "long_created";
	String CREATED = "created";
	String FAVORITE = "favorite";
	String TAGS = "tags";
	String CONTENT_TYPE = "vnd.android.cursor.dir/com.kaznog.android.dreamnote.item";
	String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/com.kaznog.android.dreamnote.item";
	String[] COLUMNS = {
		COLUMN_ID,
		DATATYPE,
		DATE,
		UPDATED,
		TITLE,
		CONTENT,
		DESCRIPTION,
		PATH,
		RELATED
	};
}
