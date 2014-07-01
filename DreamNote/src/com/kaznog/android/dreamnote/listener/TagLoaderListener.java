package com.kaznog.android.dreamnote.listener;

import android.database.Cursor;

public interface TagLoaderListener {
	void onLoadFinished(Cursor result);
}
