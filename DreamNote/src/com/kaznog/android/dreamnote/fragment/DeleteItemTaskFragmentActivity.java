package com.kaznog.android.dreamnote.fragment;

import java.io.File;
import java.util.Iterator;

import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.kaznog.android.dreamnote.db.DreamNoteProvider;
import com.kaznog.android.dreamnote.util.ImageManager;

public class DeleteItemTaskFragmentActivity extends SherlockFragmentActivity {
	public static class DeleteItemTaskFragment extends SherlockFragment
	implements LoaderManager.LoaderCallbacks<Integer> {
		public static final String TAG = "DeleteItemTaskFragment";
		private ArrayListItem deleteItems;
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

		public void setSelectedItems(ArrayListItem items) {
			deleteItems = (ArrayListItem) items.clone();
		}

		public void startLoader() {
			getLoaderManager().initLoader(0, null, this);
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			super.onCreate(savedInstanceState);
		}

		@Override
		public Loader<Integer> onCreateLoader(int arg0, Bundle arg1) {
			return new DeleteItemTask(getSherlockActivity(), deleteItems);
		}

		@Override
		public void onLoadFinished(Loader<Integer> loader, Integer result) {
			deleteItems.clear();
			deleteItems = null;
		}

		@Override
		public void onLoaderReset(Loader<Integer> arg0) {
			// TODO 自動生成されたメソッド・スタブ

		}

	}

	public static class DeleteItemTask extends AsyncTaskLoader<Integer> {
		private ArrayListItem items;
		private Context context;
		public DeleteItemTask(Context context, ArrayListItem items) {
			super(context);
			this.context = context;
			this.items = items;
		}

		@Override
		protected void onStartLoading() {
			forceLoad();
		}

		@Override
		protected void onStopLoading() {
			cancelLoad();
		}

		@Override
		public Integer loadInBackground() {
			Iterator<Item> positer = items.iterator();
			while(positer.hasNext()) {
				Item positem = positer.next();
				if(positem.datatype == DreamNoteProvider.ITEMTYPE_PHOTO) {
					if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
						File deletefile = new File(positem.path);
						ImageManager.deleteGalleryFile(context.getContentResolver(), deletefile.getParent(), deletefile.getName());
						if(deletefile.exists()) {
							deletefile.delete();
						}
					}
				} else if(positem.datatype == DreamNoteProvider.ITEMTYPE_HTML) {
					// クリップデータをフォルダごと削除
					File clipdir = new File(positem.path);
					if(clipdir.exists()) {
						File[] clipcontents = clipdir.listFiles();
						for(File contentfile: clipcontents) {
							if(contentfile.isDirectory() == false) {
								contentfile.delete();
							}
						}
						clipdir.delete();
					}
				}
			}
			return 0;
		}
	}
}
