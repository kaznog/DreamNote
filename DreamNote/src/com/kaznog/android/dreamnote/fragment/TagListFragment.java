package com.kaznog.android.dreamnote.fragment;

import android.database.Cursor;
import android.database.DatabaseUtils;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.db.DreamNoteProvider;
import com.kaznog.android.dreamnote.db.schema.TagsSchema;
import com.kaznog.android.dreamnote.listener.OnFragmentControlListener;
import com.kaznog.android.dreamnote.listener.TagListItemListener;

public class TagListFragment extends SherlockListFragment
implements LoaderManager.LoaderCallbacks<Cursor> {
	public static final String TAG = "TagListFragment";
	SimpleCursorAdapter mAdapter;
	TagListItemListener mTagListItemListener;
	public void setTagListItemListener(TagListItemListener listener) {
		mTagListItemListener = listener;
	}
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.taglist_fragment, container, false);
		return view;
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		//setEmptyText(getResources().getText(R.string.viewer_not_countent));
		ListView list = getListView();
		list.setScrollingCacheEnabled(false);
		list.setBackgroundColor(Color.WHITE);
		mAdapter = new SimpleCursorAdapter(
					getActivity(),
					android.R.layout.simple_list_item_1,
					null,
					new String[] { TagsSchema.TERM },
					new int[] { android.R.id.text1},
					0);
		setListAdapter(mAdapter);

		//setListShown(false);
		getLoaderManager().initLoader(0, null, this);
		setHasOptionsMenu(true);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		setUserVisibleHint(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		switch(item.getItemId()) {
		case R.id.action_menu_back:
			onAbort();
			break;
		default:
			return super.onOptionsItemSelected(item);
		}
		return true;
    }

	static final String[] TAGS_PROJECTION = new String[] {
		"_ID as _id",
		TagsSchema.TERM
	};
	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		CursorLoader cl = null;
		cl = new CursorLoader(
					getActivity(),
					Uri.parse("content://" + DreamNoteProvider.AUTHORITY + "/tags"),
					TAGS_PROJECTION, null, null, null);
		return cl;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor result) {
		// TODO 自動生成されたメソッド・スタブ
		mAdapter.swapCursor(result);
		mAdapter.notifyDataSetChanged();
/*
        // The list should now be shown.
        if (isResumed()) {
            setListShown(true);
        } else {
            setListShownNoAnimation(true);
        }
*/
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		mAdapter.swapCursor(null);
	}

	@Override
	public void onListItemClick(ListView listview, View view, int position, long id) {
		super.onListItemClick(listview, view, position, id);
		Cursor c = (Cursor)this.getListAdapter().getItem(position);
		//Log.d(Constant.LOG_TAG, "select pos: " + position);
		DatabaseUtils.dumpCurrentRow(c);
		String term = c.getString(c.getColumnIndex("term"));
		mTagListItemListener.onTagListItem(new String[]{ term });

		onAbort();
	}

	public void onAbort() {
		setResult(android.app.Activity.RESULT_CANCELED, null);
		FragmentManager fm = getSherlockActivity().getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
		ft.remove(this);
		HomeMenuListFragment hf = (HomeMenuListFragment) fm.findFragmentByTag(HomeMenuListFragment.TAG);
		if(hf != null) {
			ft.remove(hf);
		}
		ft.commit();
	}
	public int requestCode;
	public OnFragmentControlListener mFragmentControlListener;
	public void setOnFragmentControlListener(int request, OnFragmentControlListener listener) {
		requestCode = request;
		mFragmentControlListener = listener;
	}
	public void setResult(int resultCode, Bundle extra) {
		if(mFragmentControlListener != null) {
			mFragmentControlListener.onFragmentResult(this, requestCode, resultCode, extra);
		}
	}
}