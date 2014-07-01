/**
 *
 */
package com.kaznog.android.dreamnote.db.schema;

/**
 * @author kaznog
 *
 */
public interface TagsSchema {
	// テーブル名
	String TABLE_NAME = "tags";
	// カラム名
	String COLUMN_ID = "_ID";
	String ITEM_ID = "item_id";
	String TERM = "term";
	String CONTENT_TYPE = "vnd.android.cursor.dir/com.kaznog.android.dreamnote.tag";
	String CONTENT_ITEM_TYPE = "vnd.android.cursor.item/com.kaznog.android.dreamnote.tag";
	String[] COLUMNS = {
		COLUMN_ID,
		ITEM_ID,
		TERM
	};
}
