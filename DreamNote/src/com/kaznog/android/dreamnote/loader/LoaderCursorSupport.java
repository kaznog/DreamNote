package com.kaznog.android.dreamnote.loader;

import java.util.ArrayList;
import java.util.Arrays;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.db.DreamNoteProvider;
import com.kaznog.android.dreamnote.db.schema.ItemsSchema;
import com.kaznog.android.dreamnote.fragment.ArrayListItem;
import com.kaznog.android.dreamnote.fragment.Item;
import com.kaznog.android.dreamnote.listener.ViewPagerVisibilityListener;
import com.kaznog.android.dreamnote.listener.onCreateLoaderListener;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ListView;

public class LoaderCursorSupport extends SherlockFragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FragmentManager fm = getSupportFragmentManager();
		if(fm.findFragmentById(android.R.id.content) == null) {
			CursorLoaderListFragment list = new CursorLoaderListFragment();
			fm.beginTransaction().add(android.R.id.content, list).commit();
		}
	}

	public static class CursorLoaderListFragment extends SherlockListFragment
	implements ViewPagerVisibilityListener, LoaderManager.LoaderCallbacks<Cursor>, OnItemLongClickListener {
		int position;
		public String category = "";
		String strQuery = "";
		String[] arrTags = null;
		String sortOrder = "";
		ListCursorAdapter mCursorAdapter;
		ListView mListView;
		static final int SEARCH_ID = Menu.FIRST;
		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			setEmptyText(getResources().getText(R.string.viewer_not_countent));

			//
//			setHasOptionsMenu(true);

			mListView = getListView();
			mListView.setOnItemLongClickListener(this);
//			mListView.setDrawSelectorOnTop(true);
//			mListView.setFocusableInTouchMode(false);
			mListView.setClickable(true);
			mListView.setLongClickable(true);
			mListView.setChoiceMode(ListView.CHOICE_MODE_NONE);
			mListView.setScrollingCacheEnabled(false);
//			mListView.setSelector(R.drawable.notes_selector);
			//mListView.setBackgroundColor(Color.WHITE);
//			mListView.setDrawSelectorOnTop(false);
			mCursorAdapter = new ListCursorAdapter(
							getActivity().getApplicationContext(),
							R.layout.cursoritem_row,
							null,
							false,
							mListView
						);
			setListAdapter(mCursorAdapter);

			setListShown(false);
			if(savedInstanceState != null) {
				category = savedInstanceState.getString("category");
				arrTags = savedInstanceState.getStringArray("tags");
				strQuery = savedInstanceState.getString("query");
				position = savedInstanceState.getInt("position");
				sortOrder = savedInstanceState.getString("sortorder");
				Bundle args = new Bundle();
				args.putString("category", category);
				args.putString("query", strQuery);
				args.putStringArray("tags", arrTags);
				args.putInt("position", position);
				args.putString("sortorder", sortOrder);
				getLoaderManager().initLoader(0, args, this);
			} else {
				getLoaderManager().initLoader(0, getArguments(), this);
			}
		}

		@Override
		public void onSaveInstanceState(Bundle outState) {
			//Log.d(Constant.LOG_TAG, "LoaderCursorSupoort onSaveInstanceState");
			super.onSaveInstanceState(outState);
			outState.putString("category", category);
			outState.putStringArray("tags", arrTags);
			outState.putString("query", strQuery);
			outState.putInt("position", position);
			outState.putString("sortorder", sortOrder);
		}

		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			CursorLoader cl = null;
			String strContentUri = "";
			if(args == null) {
				strContentUri = "content://" + DreamNoteProvider.AUTHORITY + "/items";
				category = "all";
				strQuery = "";
				arrTags = null;
				position = 0;
			} else {
				//Log.d(Constant.LOG_TAG, "onCreateLoader args exist");
				category = args.getString("category");
				if(category == null) {
					//Log.d(Constant.LOG_TAG, "onCreateLoader not exist category");
					strContentUri = "content://" + DreamNoteProvider.AUTHORITY + "/items";
					category = "all";
					strQuery = "";
					arrTags = null;
					position = 0;
				} else {
					//Log.d(Constant.LOG_TAG, "onCreateLoader category exist");
					if(category.equalsIgnoreCase("all")) {
						strContentUri = "content://" + DreamNoteProvider.AUTHORITY + "/items";
					} else if(category.equalsIgnoreCase("memos")) {
						strContentUri = "content://" + DreamNoteProvider.AUTHORITY + "/memos";
					} else if(category.equalsIgnoreCase("photos")) {
						strContentUri = "content://" + DreamNoteProvider.AUTHORITY + "/photos";
					} else if(category.equalsIgnoreCase("todos")) {
						strContentUri = "content://" + DreamNoteProvider.AUTHORITY + "/todos";
					} else if(category.equalsIgnoreCase("htmls")) {
						strContentUri = "content://" + DreamNoteProvider.AUTHORITY + "/htmls";
					} else {
						//Log.d(Constant.LOG_TAG, "onCreateLoader Unknown category: " + category);
						strContentUri = "content://" + DreamNoteProvider.AUTHORITY + "/items";
					}
					strQuery = args.getString("query");
					arrTags = args.getStringArray("tags");
					position = args.getInt("position");
					sortOrder = args.getString("sortorder");
				}
			}
			if(mOnCreateLoaderListener != null) {
				mOnCreateLoaderListener.onCreateLoader(position, this);
			}
			//Log.d(Constant.LOG_TAG, "onCreateLoader contenturi: " + strContentUri);
			cl = new CursorLoader(
					getActivity(),
					Uri.parse(strContentUri),
					null,
					strQuery, arrTags, sortOrder);
			return cl;
		}

		public void onLoadFinished(Loader<Cursor> loader, Cursor result) {
//			DatabaseUtils.dumpCursor(result);
//			//Log.d(Constant.LOG_TAG, "onLoadFinished result count: " + result.getCount());
			mCursorAdapter.swapCursor(result);
			mCursorAdapter.notifyDataSetChanged();
			if(isResumed()) {
				setListShown(true);
			} else {
				setListShownNoAnimation(true);
			}
//			setMenuVisibility(true);
//			getSherlockActivity().supportInvalidateOptionsMenu();
		}

		public void onLoaderReset(Loader<Cursor> loader) {
			//Log.d(Constant.LOG_TAG, "onLoaderReset");
			mCursorAdapter.swapCursor(null);
		}

		@Override
		public void onVisibilityChanged(boolean visible) {
			// TODO 自動生成されたメソッド・スタブ

		}

		public void setQuery(String strQuery, String[] arrTags, String sortOrder) {
			boolean res = false;
			if(arrTags != null) {
				if(this.arrTags == null) {
					res = true;
				} else if(arrTags.length != this.arrTags.length) {
					res = true;
				} else {
					ArrayList<String> flist = new ArrayList<String>(Arrays.asList(this.arrTags));
					int i = 0;
					while(i < arrTags.length) {
						if(flist.indexOf(arrTags[i]) == -1) {
							break;
						}
						i++;
					}
					if(i != arrTags.length) {
						res = true;
					}
					flist.clear();
					flist = null;
				}
			} else {
				res = true;
			}
			if(this.strQuery == null) this.strQuery = "";
			if(strQuery == null) strQuery = "";
			if(this.strQuery.equals(strQuery) == false || res) {
				setForceQuery(strQuery, arrTags, sortOrder);
			}
		}

		public void setForceQuery(String strQuery, String[] arrTags, String sortOrder) {
			this.strQuery = strQuery;
			this.arrTags = arrTags;
			this.sortOrder = sortOrder;
			Bundle args = new Bundle();
			args.putString("category", category);
			args.putString("query", strQuery);
			args.putStringArray("tags", arrTags);
			args.putInt("position", position);
			args.putString("sortorder", sortOrder);
			try {
				//Log.d(Constant.LOG_TAG, "try restartLoader category: " + category);
				getLoaderManager().restartLoader(0, args, this);
			} catch(Exception e) {
//				e.printStackTrace();
			}
		}

		public String getQueryText() {
			return strQuery;
		}

		public String[] getTagsArray() {
			return arrTags;
		}
		private onCreateLoaderListener mOnCreateLoaderListener;
		public void setOnCreateLoaderListener(onCreateLoaderListener listener) {
			mOnCreateLoaderListener = listener;
		}

		@Override
		public void onListItemClick(ListView list, View view, int position, long id) {
			Cursor c = (Cursor)getListAdapter().getItem(position);
			if(getListView().getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE) {
//				super.onListItemClick(list, view, position, id);
				ListCursorAdapter adapter = (ListCursorAdapter)list.getAdapter();
				adapter.toggle(position);
				mContainerCallback.onMultiModeItemSelected(adapter.getCheckedItem());
				return;
			}
			int datatype = c.getInt(c.getColumnIndexOrThrow(ItemsSchema.DATATYPE));
			if(datatype != DreamNoteProvider.ITEMTYPE_TODOSEPARATOR) {
				super.onListItemClick(list, view, position, id);
				Item item = new Item();
				item.id = id;
				item.datatype = datatype;
				item.long_date = c.getLong(c.getColumnIndexOrThrow(ItemsSchema.LONG_DATE));
				item.date = c.getString(c.getColumnIndexOrThrow(ItemsSchema.DATE));
				item.long_updated = c.getLong(c.getColumnIndexOrThrow(ItemsSchema.LONG_UPDATED));
				item.updated = c.getString(c.getColumnIndexOrThrow(ItemsSchema.UPDATED));
				item.title = c.getString(c.getColumnIndexOrThrow(ItemsSchema.TITLE));
				item.content = c.getString(c.getColumnIndexOrThrow(ItemsSchema.CONTENT));
				item.description = c.getString(c.getColumnIndexOrThrow(ItemsSchema.DESCRIPTION));
				item.path = c.getString(c.getColumnIndexOrThrow(ItemsSchema.PATH));
				item.related = c.getString(c.getColumnIndexOrThrow(ItemsSchema.RELATED));
				item.long_created = c.getLong(c.getColumnIndexOrThrow(ItemsSchema.LONG_CREATED));
				item.created = c.getString(c.getColumnIndexOrThrow(ItemsSchema.CREATED));
				String tags = c.getString(c.getColumnIndexOrThrow(ItemsSchema.TAGS));
				item.tags = tags.substring(1, tags.length() - 1);
				mContainerCallback.onNoteSelected(datatype, item);
			}
		}

		@SuppressLint("NewApi") @Override
		public boolean onItemLongClick(AdapterView<?> adapter, View view, int position, long id) {
			//Log.d(Constant.LOG_TAG, "onItemLongClick");
			((ListCursorAdapter)getListAdapter()).toggle(position);
			mContainerCallback.onItemLongClick(adapter, view, position, id);
			mContainerCallback.onMultiModeItemSelected(((ListCursorAdapter)getListAdapter()).getCheckedItem());
			return false;
		}

		public void setChoiceMode(int mode) {
			ListView list = getListView();
			list.setChoiceMode(mode);
		}

		public void clearSelection() {
			ListView list = getListView();
			if(list.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE) {
				ListCursorAdapter adapter = (ListCursorAdapter)list.getAdapter();
				int size = adapter.getCount();
				for(int pos = 0; pos < size; pos++) {
					adapter.setChecked(pos, false);
				}
			}
			list.clearChoices();
			this.mCursorAdapter.notifyDataSetChanged();
		}

		public void refreshList() {
			this.mCursorAdapter.notifyDataSetChanged();
		}

		public interface onNoteListEventCallbackListener {
			public void onNoteSelected(int datatype, Item item);
			public void onNoteDeleted();
			public void onMultiModeItemSelected(ArrayListItem list);
			public void onItemLongClick(AdapterView<?> adapter, View view, int position, long id);
		}
		private onNoteListEventCallbackListener mContainerCallback;
		@Override
		public void onAttach(Activity activity) {
			super.onAttach(activity);
			try {
				mContainerCallback = (onNoteListEventCallbackListener)activity;
			} catch(ClassCastException e) {
				activity.finish();
				throw new ClassCastException(activity.toString() + "most implement NoteListEventCallback");
			}
		}
	}
}
