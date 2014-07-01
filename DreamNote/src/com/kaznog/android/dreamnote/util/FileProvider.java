/**
 *
 */
package com.kaznog.android.dreamnote.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.net.URI;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;

/**
 * @author noguchi
 *
 */
public class FileProvider extends ContentProvider {
	/**
	 * コンテンツプロバイダの初期化
	 */
    @Override
    public boolean onCreate() {
        return true;
    }

    /**
     * ファイルの提供
     */
    @Override
    public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        File file = new File(URI.create("file:///data/data/com.kaznog.android.dreamnote/files/" + uri.getLastPathSegment()));
        ParcelFileDescriptor parcel = ParcelFileDescriptor.open(file,ParcelFileDescriptor.MODE_READ_ONLY);
        return parcel;
    }
	/**
	 * データベースの削除命令(未使用)
	 */
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
        File file = new File(URI.create("file:///data/data/com.kaznog.android.dreamnote/files/" + uri.getLastPathSegment()));
        file.delete();
		return 0;
	}

	/**
	 * 種別の取得(未使用)
	 */
	@Override
	public String getType(Uri uri) {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	/**
	 * データベースの挿入命令(未使用)
	 */
	@Override
	public Uri insert(Uri uri, ContentValues values) {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

    /**
     * データベースのクエリー命令(未使用)
     */
    @Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		// TODO 自動生成されたメソッド・スタブ
		return null;
	}

	/**
	 * データベースの更新命令(未使用)
	 */
	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		// TODO 自動生成されたメソッド・スタブ
		return 0;
	}

}
