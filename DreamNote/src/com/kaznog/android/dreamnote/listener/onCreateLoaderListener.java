package com.kaznog.android.dreamnote.listener;

import com.kaznog.android.dreamnote.loader.LoaderCursorSupport;

public interface onCreateLoaderListener {
	void onCreateLoader(int position, LoaderCursorSupport.CursorLoaderListFragment fragment);
}
