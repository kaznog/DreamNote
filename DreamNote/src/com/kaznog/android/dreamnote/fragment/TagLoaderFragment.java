package com.kaznog.android.dreamnote.fragment;

import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.kaznog.android.dreamnote.db.DreamNoteProvider;
import com.kaznog.android.dreamnote.db.schema.TagsSchema;
import com.kaznog.android.dreamnote.listener.TagLoaderListener;

public class TagLoaderFragment extends SherlockFragment
implements LoaderManager.LoaderCallbacks<Cursor> {
	public static final String TAG = "TagLoaderFragment";
	private TagLoaderListener mTagLoaderListener;
	private static final String[] TAGS_PROJECTION = new String[] {
		"_ID as _id",
		TagsSchema.TERM
	};

	protected void registerFragment(SherlockFragmentActivity activity, String tag) {
		FragmentManager fm = activity.getSupportFragmentManager();
		Fragment fragment = fm.findFragmentByTag(tag);
		if(fragment != null) {
			FragmentTransaction ft = fm.beginTransaction();
			ft.remove(fragment);
			ft.commit();
			fm.executePendingTransactions();
		}
		FragmentTransaction ft = fm.beginTransaction();
		ft.add(this, tag);
		ft.commit();
		fm.executePendingTransactions();
	}

	public void setTagLoaderListener(TagLoaderListener listener) {
		mTagLoaderListener = listener;
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
		CursorLoader cl = null;
		cl = new CursorLoader(
					getActivity(),
					Uri.parse("content://" + DreamNoteProvider.AUTHORITY + "/tags"),
					TAGS_PROJECTION, null, null, null);
		return cl;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor result) {
		if(mTagLoaderListener != null) {
			mTagLoaderListener.onLoadFinished(result);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
	}

}