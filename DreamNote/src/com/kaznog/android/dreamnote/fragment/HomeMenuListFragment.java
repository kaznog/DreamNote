package com.kaznog.android.dreamnote.fragment;

import java.util.ArrayList;
import java.util.List;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.evernote.client.oauth.android.EvernoteSession;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.db.DreamNoteProvider;
import com.kaznog.android.dreamnote.fragment.PreferenceFragment;
import com.kaznog.android.dreamnote.fragment.TagListFragment;
import com.kaznog.android.dreamnote.listener.OnFragmentControlListener;
import com.kaznog.android.dreamnote.listener.TagListItemListener;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

public class HomeMenuListFragment extends HomeMenuFragment
implements
LoaderManager.LoaderCallbacks<Cursor>,
TagListItemListener,
OnFragmentControlListener {
	public static final String TAG = "HomeMenuListFragment";
	public static final int HOMEMENU_ID_SETTING = 0;
	public static final int HOMEMENU_ID_SELECTTAG = 1;
	public static final int HOMEMENU_ID_HELP = 2;
	public static final int HOMEMENU_ID_ABOUT = 3;
	public static final int HOMEMENU_ID_DBBACKUP = 99;
	private TagListItemListener mTagListItemListener;
	public void setTagListItemListener(TagListItemListener listener) {
		mTagListItemListener = listener;
	}
	private OnHomeMenuListener mHomeMenuListener;
	public interface OnHomeMenuListener {
		void onHomeMenuSelected(int HomeMenuID);
	}
	private HomeMenuAdapter mAdapter;
    public static class HomeMenuData {
    	private int menu_id;
    	private Bitmap imageData;
    	private String textData;
    	public void setMenuId(int id) {
    		menu_id = id;
    	}
    	public int getMenuId() {
    		return menu_id;
    	}
    	public void setImageData(Bitmap image) {
    		imageData = image;
    	}
    	public Bitmap getImageData() {
    		return imageData;
    	}
    	public void setTextData(String text) {
    		textData = text;
    	}
    	public String getTextData() {
    		return textData;
    	}
    }
    public static class HomeMenuAdapter extends ArrayAdapter<HomeMenuData> {
    	private LayoutInflater inflater;
    	static class HomeMenuHolder {
    		TextView textview;
    		ImageView imageview;
    	}
    	public HomeMenuAdapter(Context context, int resource, List<HomeMenuData> objects) {
    		super(context,resource, objects);
    		inflater = (LayoutInflater)context.getSystemService(android.content.Context.LAYOUT_INFLATER_SERVICE);
    	}

    	@Override
    	public View getView(int position, View convertView, ViewGroup parent) {
    		HomeMenuHolder holder;
    		final HomeMenuData item = getItem(position);
    		if(convertView == null) {
    			convertView = inflater.inflate(R.layout.homemenu_item, null);
    			holder = new HomeMenuHolder();
    			holder.textview = (TextView)convertView.findViewById(R.id.homemenu_text);
    			holder.imageview = (ImageView)convertView.findViewById(R.id.homemenu_icon);
    			convertView.setTag(holder);
    		} else {
    			holder = (HomeMenuHolder)convertView.getTag();
    		}
    		holder.textview.setText(item.getTextData());
    		holder.imageview.setImageBitmap(item.getImageData());
    		return convertView;
    	}
    }

    @Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	}


	@Override
	protected View createView(LayoutInflater inflater, ViewGroup container) {
		final View contentView = inflater.inflate(R.layout.home_menu, container, false);
		return contentView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// レイアウトで定義したリストにアダプタをセットしておきます
		List<HomeMenuData> list = new ArrayList<HomeMenuData>();
		HomeMenuData item1 = new HomeMenuData();
		item1.setMenuId(HOMEMENU_ID_SETTING);
		item1.setTextData(getResources().getString(R.string.menu_setting_description));
		item1.setImageData(BitmapFactory.decodeResource(getResources(), R.drawable.ic_menu_preferences));
		list.add(item1);
		HomeMenuData item2 = new HomeMenuData();
		item2.setMenuId(HOMEMENU_ID_HELP);
		item2.setTextData(getResources().getString(R.string.cliphelp_title));
		item2.setImageData(BitmapFactory.decodeResource(getResources(), R.drawable.artbook));
		list.add(item2);
		HomeMenuData item3 = new HomeMenuData();
		item3.setMenuId(HOMEMENU_ID_ABOUT);
		item3.setTextData(getResources().getString(R.string.about_dreamnote));
		item3.setImageData(BitmapFactory.decodeResource(getResources(), R.drawable.actionbar_icon));
		list.add(item3);
/*
		HomeMenuData item99 = new HomeMenuData();
		item99.setMenuId(HOMEMENU_ID_DBBACKUP);
		item99.setTextData("db backup");
		item99.setImageData(BitmapFactory.decodeResource(getResources(), R.drawable.setting));
		list.add(item99);
*/
		mAdapter = new HomeMenuAdapter(getActivity().getApplicationContext(), 0, list);
		setListAdapter(mAdapter);
		getLoaderManager().initLoader(0, null, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
		CursorLoader cl = null;
		cl = new CursorLoader(
					getActivity(),
					Uri.parse("content://" + DreamNoteProvider.AUTHORITY + "/tags"),
					null, null, null, null);
		return cl;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor result) {
		int adapter_cnt = mAdapter.getCount();
		String menu_selecttag_description = getResources().getString(R.string.menu_selecttags_description);
		if(result.getCount() > 0) {
			boolean exist = false;
			for(int i = 0; i < adapter_cnt; i++) {
				HomeMenuData item = mAdapter.getItem(i);
				if(item.getTextData().equals(menu_selecttag_description)) {
					exist = true;
					break;
				}
			}
			if(exist == false) {

				HomeMenuData selecttag_item = new HomeMenuData();
				selecttag_item.setMenuId(HOMEMENU_ID_SELECTTAG);
				selecttag_item.setTextData(menu_selecttag_description);
				selecttag_item.setImageData(BitmapFactory.decodeResource(getResources(), R.drawable.ic_menu_tag));
				mAdapter.insert(selecttag_item, 0);
				mAdapter.notifyDataSetChanged();

			}
		} else {
			for(int i = 0; i < adapter_cnt; i++) {
				HomeMenuData item = mAdapter.getItem(i);
				if(item.getTextData().equals(menu_selecttag_description)) {
					mAdapter.remove(item);
					mAdapter.notifyDataSetChanged();
					break;
				}
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> arg0) {
		// TODO 自動生成されたメソッド・スタブ

	}

	@Override
	public void onListItemClick(ListView listview, View view, int position, long id) {
		HomeMenuData item = mAdapter.getItem(position);
		switch(item.getMenuId()) {
		case HOMEMENU_ID_SELECTTAG:
			if(mFragmentControlListener != null) {
				mDisableFragmentAnimations = true;
				TagListFragment f = new TagListFragment();
				f.setTagListItemListener(this);
				mFragmentControlListener.onAddNoteFragment(this, f, Notes.TAGLIST_FRAGMENT, TagListFragment.TAG);
			}
			break;
		case HOMEMENU_ID_SETTING:
			if(mFragmentControlListener != null) {
				mDisableFragmentAnimations = true;
				PreferenceFragment f = new PreferenceFragment();
				mFragmentControlListener.onAddNoteFragment(this, f, Notes.PREFERENCE_FRAGMENT, PreferenceFragment.TAG);
			}
			break;
		case HOMEMENU_ID_HELP:
			if(mFragmentControlListener != null) {
				mDisableFragmentAnimations = true;
				HelpFragment f = new HelpFragment();
				mFragmentControlListener.onAddNoteFragment(this, f, Notes.HELP_FRAGMENT, HelpFragment.TAG);
			}
			break;
		case HOMEMENU_ID_ABOUT:
			if(mFragmentControlListener != null) {
				mDisableFragmentAnimations = true;
				AboutFragment f = new AboutFragment();
				mFragmentControlListener.onAddNoteFragment(this, f, Notes.ABOUT_FRAGMENT, AboutFragment.TAG);
			}
			break;
/*
		case HOMEMENU_ID_DBBACKUP:
			try {
				File sd = Environment.getExternalStorageDirectory();
				File data = Environment.getDataDirectory();
				if(sd.canWrite()) {
					String currentDBPath = "/data/" + getSherlockActivity().getPackageName() + "/databases/dreamnote";
					String backupDBPath = "DreamNote/dreamnote.db";
					File currentDB = new File(data, currentDBPath);
					File backupDB = new File(sd, backupDBPath);
					FileChannel src = new FileInputStream(currentDB).getChannel();
					FileChannel dst = new FileOutputStream(backupDB).getChannel();
					dst.transferFrom(src, 0, src.size());
					src.close();
					dst.close();
					Toast.makeText(getSherlockActivity().getBaseContext(), backupDB.toString(), Toast.LENGTH_LONG).show();
				}
			} catch(Exception e) {
				Toast.makeText(getSherlockActivity().getBaseContext(), e.toString(), Toast.LENGTH_LONG).show();
			}
			break;
*/
		}
		if(mHomeMenuListener != null) {
//			mHomeMenuListener.onHomeMenuSelected(item.getMenuId());
		}
	}

	public void setOnHomeMenuListener(OnHomeMenuListener listener) {
		mHomeMenuListener = listener;
	}

	@Override
	public void onTagListItem(String[] arrTags) {
		if(mTagListItemListener != null) {
			mTagListItemListener.onTagListItem(arrTags);
		}
	}

	@Override
	public void onAddNoteFragment(OnFragmentControlListener listener,
			Fragment fragment, int fragment_type, String tag) {
		// TODO 自動生成されたメソッド・スタブ

	}


	@Override
	public void onFragmentResult(Fragment fragment, int requestCode, int resultCode, Bundle extra) {
		if(requestCode == Notes.TAGLIST_FRAGMENT
		|| requestCode == Notes.PREFERENCE_FRAGMENT
		|| requestCode == Notes.HELP_FRAGMENT
		|| requestCode == Notes.ABOUT_FRAGMENT) {
			setResult(resultCode, null);
			setResult(resultCode, null);
		}
	}


	@Override
	public void onRemoveRequest(String tag) {
		// TODO 自動生成されたメソッド・スタブ

	}


	@Override
	public EvernoteSession getEvernoteSession() {
		if(mFragmentControlListener != null) {
			return mFragmentControlListener.getEvernoteSession();
		}
		return null;
	}


	@Override
	public void setEvernoteSession(EvernoteSession session) {
		if(mFragmentControlListener != null) {
			mFragmentControlListener.setEvernoteSession(session);
		}
	}
}
