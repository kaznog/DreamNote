package com.kaznog.android.dreamnote.fragment;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.internal.view.menu.ActionMenuPresenter;
import com.actionbarsherlock.internal.view.menu.ActionMenuView;
import com.actionbarsherlock.internal.view.menu.MenuBuilder;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.evernote.client.oauth.android.EvernoteSession;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.db.DreamNoteProvider;
import com.kaznog.android.dreamnote.dialogfragment.DeleteItemDialogFragment;
import com.kaznog.android.dreamnote.evernote.DreamPostEnmlService;
import com.kaznog.android.dreamnote.fragment.DeleteItemTaskFragmentActivity.DeleteItemTaskFragment;
import com.kaznog.android.dreamnote.fragment.PhotoEditFragmentActivity.PhotoEditFragment;
import com.kaznog.android.dreamnote.listener.DeleteItemDialogResultListener;
import com.kaznog.android.dreamnote.listener.OnFragmentControlListener;
import com.kaznog.android.dreamnote.settings.PreferencesUtil;
import com.kaznog.android.dreamnote.util.AppInfo;
import com.kaznog.android.dreamnote.util.Constant;
import com.kaznog.android.dreamnote.util.ImageManager;
import com.kaznog.android.dreamnote.util.StringUtils;

public class PhotoPreviewFragmentActivity extends SherlockFragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FragmentManager fm = getSupportFragmentManager();
		if(fm.findFragmentById(android.R.id.content) == null) {
			PhotoPreviewFragment f = new PhotoPreviewFragment();
			fm.beginTransaction().add(android.R.id.content, f).commit();
		}
	}
	public static class PhotoPreviewFragment extends PreviewFragment
	implements
	LoaderManager.LoaderCallbacks<List<String>>,
	MenuBuilder.Callback,
	com.actionbarsherlock.internal.view.menu.MenuPresenter.Callback,
	OnFragmentControlListener,
	DeleteItemDialogResultListener {
		private MenuBuilder mMenu;
		private MenuItem EverMenu;
		private MenuItem galleryItem;
		private MenuItem moreItem;
		private int visibleIconId;
		private int invisibleIconId;
		private boolean mIsPhotoCreated = false;
		private TextView preview_title;
		private TextView preview_content;
		private TextView preview_tags;
		private String preview_html;
		private WebView preview_photo;
		private ScrollView preview_scrollarea;
		private boolean scrollareaborder_mode = false;
		private String tempFileName;
		private DisplayMetrics metrics;
		private EvernoteSession session;

		public static PhotoPreviewFragment newInstance(Item item) {
			PhotoPreviewFragment f = new PhotoPreviewFragment();
			Bundle args = new Bundle();
			args.putSerializable("item", item);
			f.setArguments(args);
			return f;
		}

		@Override
		public void onCreate(Bundle savedInstanceState) {
			//Log.d(Constant.LOG_TAG, "PhotoPreviewFragment onCreate");
			super.onCreate(savedInstanceState);
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
			//Log.d(Constant.LOG_TAG, "PhotoPreviewFragment onCreateView");
			Configuration config = getResources().getConfiguration();
			if(config.orientation == Configuration.ORIENTATION_PORTRAIT) {
			} else if(config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			} else if(config.orientation == Configuration.ORIENTATION_SQUARE) {
			}
	        View root = createContentView(inflater, R.layout.preview_photo_frag);
			return root;
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
	        try {
		        CookieSyncManager.createInstance(getSherlockActivity().getApplicationContext());
				CookieManager cookieManager = CookieManager.getInstance();
		    	cookieManager.removeAllCookie();
		    	cookieManager.acceptCookie();
	        } catch(IllegalStateException e) {
	        } catch(Exception e) {
	        }
			setupSession(this.getSherlockActivity().getApplicationContext());
			if(savedInstanceState == null) {
				AppInfo.DebugLog(getSherlockActivity().getApplicationContext(), "PhotoPreviewFragment onActivityCreated savedInstanceState == null");
				preview_html = "";
				tempFileName = "";
				item = (Item)getArguments().getSerializable("item");
				scrollareaborder_mode = false;
				mIsPhotoCreated = false;
				mMenu = null;
				AppInfo.DebugLog(getSherlockActivity().getApplicationContext(), "preview_html: " + preview_html);
				setupUI(mContentContainer);
				setContentShown(false);
				getLoaderManager().initLoader(0, null, this);
			} else {
				AppInfo.DebugLog(getSherlockActivity().getApplicationContext(), "PhotoPreviewFragment onActivityCreated savedInstanceState != null");
				item = (Item) savedInstanceState.getSerializable("item");
				preview_html = savedInstanceState.getString("preview_html");
				tempFileName = savedInstanceState.getString("tempFileName");
				scrollareaborder_mode = savedInstanceState.getBoolean("scrollareaborder_mode");
				mIsPhotoCreated = savedInstanceState.getBoolean("mIsPhotoCreated");
				AppInfo.DebugLog(getSherlockActivity().getApplicationContext(), "preview_html: " + preview_html);
				setupUI(mContentContainer);
				setContentShown(false);
				if(mIsPhotoCreated) {
					setItemData();
					setContentShown(true);
				} else {
					getLoaderManager().initLoader(0, null, this);
				}
			}
			setHasOptionsMenu(true);
		}

		@Override
		public void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);
			outState.putSerializable("item", item);
			outState.putString("preview_html", preview_html);
			outState.putString("tempFileName", tempFileName);
			outState.putBoolean("mIsPhotoCreated", mIsPhotoCreated);
			outState.putBoolean("scrollareaborder_mode", scrollareaborder_mode);
/*
			if(preview_photo != null) {
				preview_photo.saveState(outState);
			}
*/
			setUserVisibleHint(true);
		}

		@SuppressLint("NewApi")
		@Override
		public void onPause() {
			super.onPause();
			if(Build.VERSION.SDK_INT > 10) {
				if(preview_photo != null) {
					preview_photo.onPause();
				}
			}
		}

		@SuppressLint("NewApi")
		@Override
		public void onResume() {
			//Log.d(Constant.LOG_TAG, "PhotoPreviewFragment onResume");
			if(Build.VERSION.SDK_INT > 10) {
				if(preview_photo != null) {
					preview_photo.onResume();
				}
			}
			super.onResume();
			if(session != null) {
				session.completeAuthentication(PreferencesUtil.getSharedPreferences(this.getSherlockActivity().getApplicationContext()));
				setEvernoteMenuEnabled();
			}
		}

		@Override
		public void onDestroyView() {
			//Log.d(Constant.LOG_TAG, "PhotoPreviewFragment onDestoryView");
	    	try {
		    	CookieManager cookieManager = CookieManager.getInstance();
		    	cookieManager.removeAllCookie();
		    	cookieManager = null;
	    	} catch(IllegalStateException e) {
	    	} catch(Exception e) {
	    	}
	    	super.onDestroyView();
		}

		@Override
		public void onDestroy() {
			//Log.d(Constant.LOG_TAG, "PhotoPreviewFragment onDestroy");
			if(preview_photo != null) {
				preview_photo.setWebChromeClient(null);
				preview_photo.setWebViewClient(null);
				preview_photo.clearView();
				preview_photo.loadUrl("about:blank");
				preview_photo.clearCache(true);
				preview_photo.clearFormData();
				preview_photo.clearHistory();
//				preview_photo.destroy();
				preview_photo = null;
			}
			if(mMenu != null) {
				mMenu.clear();
				mMenu = null;
			}
			super.onDestroy();
		}

		@Override
		public Loader<List<String>> onCreateLoader(int id, Bundle args) {
			//Log.d(Constant.LOG_TAG, "PhotoPreviewFragment onCreateLoader");
			return new PhotoConvertTask(getSherlockActivity(), item.path);
		}

		@Override
		public void onLoadFinished(Loader<List<String>> loader, List<String> result) {
			preview_html = result.get(0);
			tempFileName = result.get(1);
			mIsPhotoCreated = true;
			setItemData();
			setContentShown(true);
		}

		@Override
		public void onLoaderReset(Loader<List<String>> loader) {
			//Log.d(Constant.LOG_TAG, "PhotoPreviewFragment onLoaderReset");
		}

		private void changelayout() {
			Configuration config = getResources().getConfiguration();
			if(config.orientation == Configuration.ORIENTATION_PORTRAIT) {
				if(preview_scrollarea.getVisibility() == View.INVISIBLE) {
		    		// 詳細エリア(備考本文、タグ)が表示状態でない場合
					preview_photo.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
				} else {
					// 写真ビューのY軸開始位置を取得
					int photo_top = preview_photo.getTop();
					// 詳細エリア(備考本文、タグ)が表示状態の場合
					preview_photo.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, (metrics.heightPixels / 2) - photo_top));
				}
			} else if(config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
				if(preview_scrollarea.getVisibility() == View.INVISIBLE) {
					// 詳細エリア(備考本文、タグ)が表示状態でない場合
					preview_photo.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
				} else {
					// 写真ビューの幅を取得
					int photo_width = (int)((metrics.widthPixels - (8 * metrics.density)) / 2)/*preview_title.getWidth() / 2*/;
					// 詳細エリア(備考本文、タグ)が表示状態の場合
					preview_photo.setLayoutParams(new LinearLayout.LayoutParams(photo_width, LayoutParams.MATCH_PARENT));
				}
			}
		}
		protected void setupUI(View v) {
	        metrics = new DisplayMetrics();
	        WindowManager manager = (WindowManager) getSherlockActivity().getSystemService(Context.WINDOW_SERVICE);
	        manager.getDefaultDisplay().getMetrics(metrics);

			preview_title = (TextView)v.findViewById(R.id.preview_title);
			preview_content = (TextView)v.findViewById(R.id.preview_content);
			preview_tags = (TextView)v.findViewById(R.id.preview_tags);
			// 写真サムネールのビューを取得
//			if(preview_photo != null) {
//				preview_photo.destroy();
//			}
			preview_photo = (WebView)v.findViewById(R.id.preview_photo_webview);
			// スクロール範囲のビューを取得
			preview_scrollarea = (ScrollView)v.findViewById(R.id.preview_scrollarea);
	        // モードに応じてスクロール範囲を(表示|非表示)にする
	        preview_scrollarea.setVisibility(scrollareaborder_mode ? View.VISIBLE : View.INVISIBLE);
			changelayout();
		}

		protected void setItemData() {
			preview_title.setText(item.title);
			if(item.content.trim().equals("")) {
				// 備考がない場合
				preview_content.setText(R.string.non_description);
				preview_content.setTextColor(Color.GRAY);
			} else {
				// 備考がある場合
				preview_content.setText(item.content);
				preview_content.setTextColor(Color.BLACK);
			}
			if(item.tags.equals("")) {
				// タグがない場合はタグTextViewを表示しない
				preview_tags.setText(R.string.non_tags);
				preview_tags.setTextColor(Color.GRAY);
			} else {
				// タグがある場合
				preview_tags.setText(item.tags);
				preview_tags.setTextColor(Color.BLACK);
			}
			loadPhotoHtml();
		}

		@SuppressLint("NewApi")
		private void loadPhotoHtml() {
			if(preview_photo != null) {
				preview_photo.getSettings().setJavaScriptEnabled(true);
				preview_photo.getSettings().setLightTouchEnabled(true);
				preview_photo.getSettings().setUseWideViewPort(true);
				preview_photo.getSettings().setLoadWithOverviewMode(true);
				preview_photo.getSettings().setSupportZoom(true);
				preview_photo.getSettings().setBuiltInZoomControls(true);
				if(Build.VERSION.SDK_INT > 10) {
					preview_photo.getSettings().setDisplayZoomControls(true);
				}
				preview_photo.getSettings().setLayoutAlgorithm(LayoutAlgorithm.NORMAL);

				preview_photo.loadDataWithBaseURL("file://" + Environment.getDataDirectory() + "/data/DreamNote/", preview_html, "text/html", "UTF-8", null);
			}
		}
		@Override
		public void onAbort() {
			if(tempFileName != null) {
				if(tempFileName.equals("") == false) {
					File previewphotofile = new File(tempFileName);
					if(previewphotofile.exists()) {
						//Log.d(Constant.LOG_TAG, "onDestroy delete tempFile");
						previewphotofile.delete();
					}
		    	}
			}
			setResult(RESULT_CANCELED, null);
			FragmentManager fm = getSherlockActivity().getSupportFragmentManager();
			Fragment fragment = fm.findFragmentByTag("preview_photo");
			if(fragment != null) {
				fm.beginTransaction().remove(fragment).commit();
			}
    		FragmentTransaction ft = fm.beginTransaction();
			ft.remove(this);
			ft.commit();
		}

		//----------------------------------------------------------------------
		private void setupSession(Context context) {
			if(mFragmentControlListener != null) {
				session = mFragmentControlListener.getEvernoteSession();
			}
		}

		@Override
		public void onPrepareOptionsMenu(Menu menu) {
			super.onPrepareOptionsMenu(menu);
		}

		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			super.onCreateOptionsMenu(menu, inflater);
			Configuration config = getResources().getConfiguration();
			if(config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
				visibleIconId = R.drawable.ic_menu_right;
				invisibleIconId = R.drawable.ic_menu_left;
			} else {
				visibleIconId = R.drawable.ic_menu_down;
				invisibleIconId = R.drawable.ic_menu_up;
			}
			MenuItem item = menu.findItem(R.id.pre_menu_frag_viewswitch);
			if(item != null) {
				item.setIcon(scrollareaborder_mode ? visibleIconId : invisibleIconId);
			}
			EverMenu = menu.findItem(R.id.pre_menu_frag_ever);
			galleryItem = menu.findItem(R.id.pre_menu_frag_gallery);
			moreItem = menu.findItem(R.id.pre_menu_frag_more);
			setEvernoteMenuEnabled();
		}

	    @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	    	//Log.d(Constant.LOG_TAG, this.getClass().getSimpleName() + " onOptionsItemSelected");
	    	return onMenuItemSelected(null, item);
	    }

		private void setEvernoteMenuEnabled() {
			boolean everstate = false;
			if(session != null) {
				everstate = session.isLoggedIn();
			}
			if(EverMenu != null) {
				EverMenu.setEnabled(everstate);
				EverMenu.setVisible(everstate);
			}
			Configuration config = getResources().getConfiguration();
			if(config.orientation == Configuration.ORIENTATION_PORTRAIT) {
				if(galleryItem != null) {
					galleryItem.setEnabled(!everstate);
					galleryItem.setVisible(!everstate);
				}
				if(moreItem != null) {
					moreItem.setEnabled(everstate);
					moreItem.setVisible(everstate);
				}
			}
		}

		@Override
		public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
			if(mIsPhotoCreated == false) return false;
			switch(item.getItemId()) {
			case R.id.pre_menu_frag_del:
				DeleteItemDialogFragment delf = new DeleteItemDialogFragment();
				Bundle args = new Bundle();
				args.putString("message", getResources().getString(R.string.item_delete_confirm_photo_extend_data_msg));
				delf.setArguments(args);
				delf.setResultListener(this);
				delf.show(getSherlockActivity().getSupportFragmentManager(), "DeleteItemDialog");
				return true;
			case R.id.pre_menu_frag_edit:
				if(mFragmentControlListener != null) {
					PhotoEditFragment f = PhotoEditFragment.newInstance(this.item, preview_html);
					mFragmentControlListener.onAddNoteFragment(this, f, Notes.EDIT_PHOTO_FRAGMENT, "edit");
				}
				return true;
			case R.id.pre_menu_frag_share:
				Intent si = new Intent();
				si.setAction(Intent.ACTION_SEND);
			    si.setType("image/jpeg");
			    si.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(this.item.path)));
			    si.putExtra(Intent.EXTRA_TEXT, this.item.content);
			    si.putExtra(Intent.EXTRA_TITLE, this.item.title);
			    si.putExtra(Intent.EXTRA_SUBJECT, this.item.title);
			    try {
			      startActivity(si);
			    } catch (android.content.ActivityNotFoundException ex) {
			      Toast.makeText(getSherlockActivity().getApplicationContext(), R.string.err_activity_not_found, Toast.LENGTH_SHORT).show();
			    }
				return true;
			case R.id.pre_menu_frag_gallery:
			case R.id.pre_menu_frag_gallery_more:
				Intent gi = new Intent();
				gi.setAction(Intent.ACTION_VIEW);
				gi.setDataAndType(Uri.fromFile(new File(this.item.path)), "image/*");
				startActivityForResult(gi, Constant.REQUEST_ACTIVITY_GALLERY);
				return true;
			case R.id.pre_menu_frag_ever:
				Toast.makeText(getSherlockActivity().getApplicationContext(), R.string.please_wait, Toast.LENGTH_LONG).show();
				Intent everintent = new Intent(this.getSherlockActivity().getApplicationContext(), DreamPostEnmlService.class);
				everintent.putExtra("item", this.item);
				this.getSherlockActivity().startService(everintent);
				return true;
			case R.id.pre_menu_frag_viewswitch:
				if(preview_scrollarea.getVisibility() == View.VISIBLE) {
					// スクロール範囲が表示状態なら非表示にする
					preview_scrollarea.setVisibility(View.INVISIBLE);
					item.setIcon(invisibleIconId);
					scrollareaborder_mode = false;
				} else {
					// スクロール範囲が非表示状態なら表示する
					preview_scrollarea.setVisibility(View.VISIBLE);
					item.setIcon(visibleIconId);
					scrollareaborder_mode = true;
				}
				changelayout();
				loadPhotoHtml();
				return true;
			}
			//Log.d(Constant.LOG_TAG, "onMenuItemSelected Item: " + strItemId);
			return false;
		}

		@Override
		public void onMenuModeChange(MenuBuilder menu) {
			// TODO 自動生成されたメソッド・スタブ

		}

		@Override
		public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
			// TODO 自動生成されたメソッド・スタブ

		}

		@Override
		public boolean onOpenSubMenu(MenuBuilder subMenu) {
			// TODO 自動生成されたメソッド・スタブ
			return false;
		}
		//----------------------------------------------------------------------

		@Override
		public void onAddNoteFragment(OnFragmentControlListener listener,
				Fragment fragment, int fragment_type, String tag) {
			// TODO 自動生成されたメソッド・スタブ

		}

		@Override
		public void onFragmentResult(Fragment fragment, int requestCode,
				int resultCode, Bundle extra) {
			if(requestCode == Notes.EDIT_PHOTO_FRAGMENT) {
				if(resultCode == RESULT_OK && extra != null) {
					int next_request = extra.getInt(Constant.FRAGMENT_RESULT_REQUEST, Constant.FRAGMENT_RESULT_REQUEST_NON);
					if(next_request == Constant.FRAGMENT_RESULT_READDATA) {
						this.item = (Item) extra.getSerializable("item");
						setItemData();
					}
				}
				setResult(resultCode, null);
			}
		}

		@Override
		public void onDeleteItemDialogResult(int result) {
			if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) == false) {
				if(result == DialogInterface.BUTTON_POSITIVE) {
					result = DialogInterface.BUTTON_NEUTRAL;
				}
			}
			if(result == DialogInterface.BUTTON_POSITIVE) {
				DeleteItemTaskFragment f = new DeleteItemTaskFragment();
				f.registerFragment(getSherlockActivity(), DeleteItemTaskFragment.TAG);
				ArrayListItem items = new ArrayListItem();
				items.add(item);
				f.setSelectedItems(items);
				f.startLoader();
			}
			String[] whereArgs = new String[] {
					String.valueOf(item.id),
					"photos"
			};
			Context context = getSherlockActivity().getApplicationContext();
			getSherlockActivity().getContentResolver().delete(DreamNoteProvider.ITEMS_CONTENT_URI, null, whereArgs);
			String strResult = context.getResources().getString(R.string.itemtype_photomemo) + "\n" + item.title + context.getResources().getString(R.string.item_delete_msg_foot);
			Toast.makeText(context, strResult, Toast.LENGTH_LONG).show();
			onAbort();
		}

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent intent) {
			Context context = getSherlockActivity().getApplicationContext();
			switch(requestCode) {
			case Constant.REQUEST_ACTIVITY_GALLERY:
				File imageFile = new File(item.path);
				if(imageFile.exists() == false) {
					ImageManager.deleteGalleryFile(context.getContentResolver(), imageFile.getParent(), imageFile.getName());
					String[] whereArgs = new String[] {
							String.valueOf(item.id),
							"photos"
					};
					getSherlockActivity().getContentResolver().delete(DreamNoteProvider.ITEMS_CONTENT_URI, null, whereArgs);
					String strResult = context.getResources().getString(R.string.itemtype_photomemo) + "\n" + item.title + context.getResources().getString(R.string.item_delete_msg_foot);
					Toast.makeText(context, strResult, Toast.LENGTH_LONG).show();
					onAbort();
				}
				break;
			}
		}

		@Override
		public void onRemoveRequest(String tag) {
			// TODO 自動生成されたメソッド・スタブ

		}

		@Override
		public EvernoteSession getEvernoteSession() {
			// TODO 自動生成されたメソッド・スタブ
			return null;
		}

		@Override
		public void setEvernoteSession(EvernoteSession session) {
			// TODO Auto-generated method stub
			
		}
	}

	public static class PhotoConvertTask extends AsyncTaskLoader<List<String>> {
		private Context context;
		private String path;
		private String preview_html;
		private String tempFileName;
		private String imagefilename;
		public PhotoConvertTask(Context context, String path) {
			super(context);
			this.context = context;
			this.path = path;
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
		public List<String> loadInBackground() {
			if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
		        // アプリケーション名を取得
		        String appName = context.getResources().getString(R.string.app_name);
		        // キャッシュディレクトリ/アプリケーション名のディレクトリパスを作成
		        String CacheDirName = Environment.getExternalStorageDirectory().toString() + "/" + appName + "/.cache";
		        imagefilename = this.path;
				File picfile = new File(imagefilename);
				if(picfile.exists()) {
					OutputStream outputStream = null;
					Bitmap bm = null;
					BitmapFactory.Options options = new BitmapFactory.Options();
					options.inJustDecodeBounds = false;
					int scale = 1;
					do {
						bm = null;
	    				options.inSampleSize = scale;
						try {
							bm = BitmapFactory.decodeFile(imagefilename, options);
						} catch(OutOfMemoryError e) {
							System.gc();
							scale++;
						}
					} while(bm == null);
					if(imagefilename.endsWith(".jpg") || imagefilename.endsWith(".JPG")
					|| imagefilename.endsWith(".jpeg") || imagefilename.endsWith(".JPEG")) {
						ExifInterface exifInterface;
						Bitmap rotateBitmap = null;
						try {
							exifInterface = new ExifInterface(imagefilename);
							if(exifInterface != null) {
								int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
								if(orientation == ExifInterface.ORIENTATION_ROTATE_90
								|| orientation == ExifInterface.ORIENTATION_ROTATE_180
								|| orientation == ExifInterface.ORIENTATION_ROTATE_270
								) {
									Matrix matrix = new Matrix();
									switch(orientation) {
									case ExifInterface.ORIENTATION_ROTATE_90:
										matrix.postRotate(90.0f);
										break;
									case ExifInterface.ORIENTATION_ROTATE_180:
										matrix.postRotate(180.0f);
										break;
									case ExifInterface.ORIENTATION_ROTATE_270:
										matrix.postRotate(270.0f);
										break;
									}
									try {
										rotateBitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
									} catch(OutOfMemoryError e) {
										System.gc();
										float rotatescale = 2.0f;
										do {
											float postscale = 1.0f / rotatescale;
											matrix.postScale(postscale, postscale);
											try {
												rotateBitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
											} catch(OutOfMemoryError re) {
												System.gc();
												rotatescale++;
											} catch(IllegalArgumentException iae) {
												if(bm != null) {
													rotateBitmap = bm;
													bm = null;
												}
												break;
											}
										} while(rotateBitmap == null);
									}
									if(rotateBitmap != null) {
										if(bm != null) {
											bm.recycle();
											bm = null;
										}
										bm = rotateBitmap;
										rotateBitmap = null;
									}
								}
							}
						} catch(IOException e) {
						}
					}
					// 読み込んだビットマップをキャッシュファイルへ出力する
					tempFileName = StringUtils.getUniqueFileName(CacheDirName);
					File previewphotofile = new File(tempFileName);
					if(previewphotofile.exists()) {
						previewphotofile.delete();
					}
					try {
						// ユニークなファイル名でキャッシュを作る
						if (previewphotofile.createNewFile()) {
							// ユニークなキャッシュが作成できたら、jpegで保存
			                outputStream = new FileOutputStream(previewphotofile);
							if(imagefilename.endsWith(".jpg") || imagefilename.endsWith(".JPG")
									|| imagefilename.endsWith(".jpeg") || imagefilename.endsWith(".JPEG")) {
								bm.compress(CompressFormat.JPEG, 75, outputStream);
							} else {
								bm.compress(CompressFormat.PNG, 100, outputStream);
							}
						}
					} catch (IOException e) {
					} finally {
						if(outputStream != null) {
							try {
								outputStream.close();
							} catch (IOException e) {
							}
						}
					}
					bm.recycle();
					bm = null;
					preview_html = "<html><body><img src=\"" + tempFileName + "\" width=\"100%\"/></body></html>";
				} else {
					preview_html = Constant.PREVIEW_CONTENT_NOT_FOUND_HEAD + context.getResources().getString(R.string.err_preview_pic_not_found_head) + "[" + imagefilename + "] " + context.getResources().getString(R.string.err_preview_pic_not_found_foot) + Constant.PREVIEW_CONTENT_NOT_FOUND_FOOT;
				}
			} else {
				preview_html = Constant.PREVIEW_CONTENT_NOT_FOUND_HEAD + context.getResources().getString(R.string.err_external_storage) + "<br/>" + context.getResources().getString(R.string.err_preview_pic_not_found_head) + "[" + imagefilename + "] " + context.getResources().getString(R.string.err_preview_pic_not_found_foot) + Constant.PREVIEW_CONTENT_NOT_FOUND_FOOT;
			}
			List<String> result = new ArrayList<String>();
			result.add(preview_html);
			result.add(tempFileName);
			return result;
		}

	}
}
