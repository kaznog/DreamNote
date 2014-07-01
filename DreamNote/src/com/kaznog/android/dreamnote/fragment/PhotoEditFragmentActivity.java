package com.kaznog.android.dreamnote.fragment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Bitmap.CompressFormat;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.text.SpannableStringBuilder;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebView;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.evernote.client.oauth.android.EvernoteSession;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.db.DreamNoteProvider;
import com.kaznog.android.dreamnote.dialogfragment.AbortDialogFragment;
import com.kaznog.android.dreamnote.dialogfragment.PhotoChoiceModeSelectorDialogFragment;
import com.kaznog.android.dreamnote.dialogfragment.PhotoChoiceModeSelectorDialogFragment.PhotoChoiceModeSelectorListener;
import com.kaznog.android.dreamnote.listener.OnFragmentControlListener;
import com.kaznog.android.dreamnote.settings.PreferencesUtil;
import com.kaznog.android.dreamnote.util.Constant;
import com.kaznog.android.dreamnote.util.ImageManager;
import com.kaznog.android.dreamnote.util.StringUtils;
import com.kaznog.android.dreamnote.widget.DreamNoteEditText.DreamNoteEditTextListener;
import com.kaznog.android.dreamnote.widget.DreamNoteTitleEditText.DreamNoteTitleEditTextListener;
import com.kaznog.android.dreamnote.widget.TagAutoCompleteTextView.TagAutoCompleteTextViewListener;

public class PhotoEditFragmentActivity extends SherlockFragmentActivity
implements OnFragmentControlListener {
	private String tmpFileName;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Theme_SherlockCustom);
		super.onCreate(savedInstanceState);
		getSherlock().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);

    	// ソフトウェアキーボードを初期表示状態で表示しないようにする
    	this.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    	// スリープ状態にしないようにする
    	this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		final ActionBar ab = getSupportActionBar();
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		//アクションバーのアイコンを設定します
		ab.setIcon(R.drawable.actionbar_icon);
		// アクションバーのアイコンにロゴを使用しない
		ab.setDisplayUseLogoEnabled(false);
		// アクションバーのアイコンを無効化しておきます
		ab.setDisplayHomeAsUpEnabled(false);

    	if(savedInstanceState == null) {
    		tmpFileName = "";
			Intent i = getIntent();
			if(i != null) {
				String action = i.getAction();
				if(Intent.ACTION_SEND.equals(action)) {
					Uri uri = i.getParcelableExtra(Intent.EXTRA_STREAM);
					if(uri != null) {
						tmpFileName = uri.getPath();
						if(tmpFileName.startsWith("/external/images/media")) {
							try {
								ContentResolver cr = getContentResolver();
								String[] columns = {MediaStore.Images.Media.DATA };
								Cursor c = cr.query(uri, columns, null, null, null);
								if(c != null) {
									try {
										c.moveToFirst();
										tmpFileName = c.getString(0);
									} catch(Exception qe) {
										finish();
									}
								}
								c.close();
							} catch(Exception e) {
								finish();
							}
						}
						FragmentManager fm = getSupportFragmentManager();
						if(fm.findFragmentById(android.R.id.content) == null) {
							PhotoEditFragment f = new PhotoEditFragment();
							f.setOnFragmentControlListener(0, this);
							Bundle args = new Bundle();
							args.putString("pickfilename", tmpFileName);
							f.setArguments(args);
							fm.beginTransaction().add(android.R.id.content, f).commit();
						}
					} else {
						finish();
					}
				} else {
					FragmentManager fm = getSupportFragmentManager();
					if(fm.findFragmentById(android.R.id.content) == null) {
						PhotoEditFragment f = new PhotoEditFragment();
						f.setOnFragmentControlListener(0, this);
						fm.beginTransaction().add(android.R.id.content, f).commit();
					}
				}
			} else {
				FragmentManager fm = getSupportFragmentManager();
				if(fm.findFragmentById(android.R.id.content) == null) {
					PhotoEditFragment f = new PhotoEditFragment();
					f.setOnFragmentControlListener(0, this);
					fm.beginTransaction().add(android.R.id.content, f).commit();
				}
			}
    	}
	}

	public static class PhotoEditFragment extends EditFragment
	implements
	LoaderManager.LoaderCallbacks<Cursor>, PhotoChoiceModeSelectorListener, TagAutoCompleteTextViewListener, DreamNoteTitleEditTextListener, DreamNoteEditTextListener {
		private int visibleIconId;
		private int invisibleIconId;
		private DisplayMetrics metrics;
		private RelativeLayout EditArea;
		private WebView preview_photo;
		private boolean editarea_mode;
		private boolean mIsPhotoCreated = false;
		private boolean mIsPhotoChoiced = false;
		private boolean picimage_mode = false;
		private boolean selectpic_mode = false;
		private boolean shootpic_mode = false;
		private AsyncTaskLoader<Cursor> photoloader;
		private String preview_html;
		private String tempFileName;
		private String ShootFileName;
		private ArrayList<String> beforeImages = null;
		private ArrayList<String> afterImages = null;
		private Handler mHandler;

		public static PhotoEditFragment newInstance(Item item, String preview_html) {
			PhotoEditFragment f = new PhotoEditFragment();
			Bundle args = new Bundle();
			args.putSerializable("item", item);
			args.putString("preview_html", preview_html);
			f.setArguments(args);
			return f;
		}


		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	        View root = createContentView(inflater, R.layout.edit_photo_frag);
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
	        mHandler = new Handler(this.getSherlockActivity().getMainLooper());
			setupMsg();
			if(savedInstanceState == null) {
				editarea_mode = true;
				shootpic_mode = false;
				selectpic_mode = false;
				mIsPhotoChoiced = false;
				Bundle args = getArguments();
				if(args != null) {
					setupUI(mContentContainer);
					// start tagsloader
					getLoaderManager().initLoader(0, null, this);
					setContentShown(false);
					String pickfilename = args.getString("pickfilename");
					if(pickfilename == null) {
						// 編集
						mode = true;
						picimage_mode = false;
						item = (Item)args.getSerializable("item");
						initializeData();
						preview_html = args.getString("preview_html");
						setItemData();
						mIsPhotoCreated = true;
					} else {
						// ファイラーからの画像ファイル共有
						mode = false;
						picimage_mode = true;
						PreferencesUtil.setPreferences(this.getSherlockActivity().getApplicationContext(), Constant.ADD_PIC_FILENAME, pickfilename);
						item = new Item();
						item.path = pickfilename;
						initializeData();
						// 表示用画像生成 AsyncTaskLoader実行
						getLoaderManager().initLoader(1, null, this);
					}
				} else {
					// 新規
					mode = false;
					picimage_mode = false;
					item = new Item();
					initializeData();
					setupUI(mContentContainer);
					PhotoChoiceModeSelectorDialogFragment f = new PhotoChoiceModeSelectorDialogFragment();
					f.setPhotoChoiceModeSelectorListener(this);
					f.show(getFragmentManager(), "PhotoChoiceModeSelectorDialog");
					// start tagsloader
					getLoaderManager().initLoader(0, null, this);
					setContentShown(false);
				}
			} else {
				onActivityCreatedRestore(savedInstanceState);
				// UIの初期化
				setupUI(mContentContainer);
				setContentShown(false);
				if(mIsTagsLoaded == false) {
					getLoaderManager().initLoader(0, null, this);
				}
				if(mIsPhotoCreated) {
					setItemData();
					setupTagCompleteText();
					setContentShown(true);
					setButtonEnable();
				} else {
					if(selectpic_mode || shootpic_mode) {
						if(mIsPhotoChoiced) {
							getLoaderManager().initLoader(1, null, this);
						}
					} else {
						PhotoChoiceModeSelectorDialogFragment f = (PhotoChoiceModeSelectorDialogFragment) getSherlockActivity().getSupportFragmentManager().findFragmentByTag("PhotoChoiceModeSelectorDialog");
						if(f != null) {
							f.setPhotoChoiceModeSelectorListener(this);
						}
						//getLoaderManager().initLoader(1, null, this);
					}
				}
			}
			setHasOptionsMenu(true);
		}

		@Override
		protected void onActivityCreatedRestore(Bundle savedInstanceState) {
			super.onActivityCreatedRestore(savedInstanceState);
			beforeImages = savedInstanceState.getStringArrayList("beforeimages");
			afterImages = savedInstanceState.getStringArrayList("afterimages");
			mIsPhotoCreated = savedInstanceState.getBoolean("mIsPhotoCreated");
			mIsPhotoChoiced = savedInstanceState.getBoolean("mIsPhotoChoiced");
			editarea_mode = savedInstanceState.getBoolean("editarea_mode");
			picimage_mode = savedInstanceState.getBoolean("picimage_mode");
			selectpic_mode = savedInstanceState.getBoolean("selectpic_mode");
			shootpic_mode = savedInstanceState.getBoolean("shootpic_mode");
			preview_html = savedInstanceState.getString("preview_html");
			tempFileName = savedInstanceState.getString("tempFileName");
			ShootFileName = savedInstanceState.getString("ShootFileName");
		}

		@Override
		protected void onSaveInstanceMoreState(Bundle outState) {
			super.onSaveInstanceMoreState(outState);
			outState.putStringArrayList("beforeimages", beforeImages);
			outState.putStringArrayList("afterImages", afterImages);
			outState.putBoolean("mIsPhotoCreated", mIsPhotoCreated);
			outState.putBoolean("mIsPhotoChoiced", mIsPhotoChoiced);
			outState.putBoolean("editarea_mode", editarea_mode);
			outState.putBoolean("picimage_mode", picimage_mode);
			outState.putBoolean("selectpic_mode", selectpic_mode);
			outState.putBoolean("shootpic_mode", shootpic_mode);
			outState.putString("preview_html", preview_html);
			outState.putString("tempFileName", tempFileName);
			outState.putString("ShootFileName", ShootFileName);
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
			if(Build.VERSION.SDK_INT > 10) {
				if(preview_photo != null) {
					preview_photo.onResume();
				}
			}
			super.onResume();
		}

		@Override
		public void onDestroyView() {
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
			if(mHandler != null) {
				mHandler = null;
			}
			super.onDestroy();
		}

		@Override
		protected void initializeData() {
			super.initializeData();
			if(mode) {
				editing_title = item.title;
				editing_content = item.content;
				editing_tags = item.tags;
			} else {
				item.datatype = DreamNoteProvider.ITEMTYPE_PHOTO;
				editing_title = "";
				editing_content = "";
				editing_tags = "";
			}
			preview_html = "";
			tempFileName = "";
			ShootFileName = "";
		}

		private void changelayout() {
			Configuration config = getResources().getConfiguration();
			if(config.orientation == Configuration.ORIENTATION_PORTRAIT) {
				if(EditArea.getVisibility() == View.VISIBLE) {
					// 編集エリアが表示状態の場合
					int photo_top = getSherlockActivity().getSupportActionBar().getHeight() * 2;
//					int photo_top = EditArea.getTop() * 2;
					EditArea.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, (metrics.heightPixels - photo_top) / 2));
					preview_photo.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, (metrics.heightPixels - photo_top) / 2));
//					preview_photo.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1));
				} else {
					// 編集エリアが表示状態でない場合
					preview_photo.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
//					EditArea.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0));
				}
			} else if(config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
				if(EditArea.getVisibility() == View.VISIBLE) {
					// 編集エリアが表示状態の場合
					int photo_width = (int)(metrics.widthPixels / 2);
					EditArea.setLayoutParams(new LinearLayout.LayoutParams(photo_width, LayoutParams.MATCH_PARENT));
					preview_photo.setLayoutParams(new LinearLayout.LayoutParams(photo_width, LayoutParams.MATCH_PARENT));
				} else {
					// 編集エリアが表示状態でない場合
					preview_photo.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
				}
			}
		}
		@Override
		protected void onFocusSpecific() {
			preview_photo.setVisibility(View.GONE);
			EditArea.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		}
		@Override
		protected void unFocusSpecific() {
			preview_photo.setVisibility(View.VISIBLE);
			changelayout();
		}		
		@Override
		protected void setupUI(View v) {
			super.setupUI(v);
	        metrics = new DisplayMetrics();
	        WindowManager manager = (WindowManager) getSherlockActivity().getSystemService(Context.WINDOW_SERVICE);
	        manager.getDefaultDisplay().getMetrics(metrics);
	        EditArea = (RelativeLayout)v.findViewById(R.id.editarea);
	        EditArea.setVisibility(editarea_mode ? View.VISIBLE : View.GONE);
	        preview_photo = (WebView)v.findViewById(R.id.edit_photo_webview);
	        titletext.setDreamNoteTitleEditTextListener(this);
	        titletext.setOnFocusChangeListener(mFocusChangeListener);
	        contenttext.setDreamNoteEditTextListener(this);
	        contenttext.setOnFocusChangeListener(mFocusChangeListener);
	    	tagtext.setTagAutoCompleteTextViewListener(this);
	    	tagtext.setOnFocusChangeListener(mFocusChangeListener);

	    	titletext.setOnEditorActionListener(mOnTitleEditorActionListener);
	    	tagtext.setOnEditorActionListener(mOnTagEditorActionListener);
	        TagSelButton.requestFocus();

	        changelayout();
		}

		@Override
		protected void setItemData() {
			super.setItemData();
			// タイトル設定
			titletext.setText(editing_title);
			// 本文設定
			contenttext.setText(editing_content);
			// タグ設定
			tagtext.setText(editing_tags);
			loadPhotoHtml();
		}

		@Override
		public void onImeHidden() {
			InputMethodManager imm = (InputMethodManager)
					getSherlockActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			if(imm != null) {
				if(preview_photo.getVisibility() == View.GONE) {
					updateActionbar(true);
					unFocusSpecific();
//					tagtext.clearFocus();
			        TagSelButton.requestFocus();
					onFocusView = null;
				}
				imm.hideSoftInputFromWindow(tagtext.getWindowToken(), 0);
			}
		}

		@Override
		public void onEditTextImeHidden() {
			InputMethodManager imm = (InputMethodManager)
					getSherlockActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			if(imm != null) {
				if(preview_photo.getVisibility() == View.GONE) {
					updateActionbar(true);
					unFocusSpecific();
//					contenttext.clearFocus();
			        TagSelButton.requestFocus();
					onFocusView = null;
				}
				imm.hideSoftInputFromWindow(contenttext.getWindowToken(), 0);
			}
		}

		@Override
		public void onTitleEditTextImeHidden() {
			InputMethodManager imm = (InputMethodManager)
					getSherlockActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			if(imm != null) {
				if(preview_photo.getVisibility() == View.GONE) {
					updateActionbar(true);
					unFocusSpecific();
//					titletext.clearFocus();
			        TagSelButton.requestFocus();
					onFocusView = null;
				}
				imm.hideSoftInputFromWindow(titletext.getWindowToken(), 0);
			}
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
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			if(id == 0) {
				return super.onCreateLoader(id, args);
			} else if(id == 1) {
				photoloader = new PhotoConvertTask(getSherlockActivity(), item.path);
				return photoloader;
			}
			return null;
		}


		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor result) {
			if(loader.getId() == 0) {
				taglist = new ArrayList<String>();
				boolean isEof = result.moveToFirst();
				while(isEof) {
					String term = result.getString(result.getColumnIndex("term"));
					taglist.add(term);
					isEof = result.moveToNext();
				}
				arrTags = (String[])taglist.toArray(new String[0]);
				setupTagCompleteText();
				mIsTagsLoaded = true;
				if(mIsPhotoCreated) {
					setButtonEnable();
					setContentShown(true);
				} else {
					setContentShown(false);
				}
			} else if(loader.getId() == 1) {
				if(result.moveToFirst()) {
					preview_html = result.getString(result.getColumnIndexOrThrow("preview_html"));
					tempFileName = result.getString(result.getColumnIndexOrThrow("tempFileName"));
					setItemData();
					mIsPhotoCreated = true;
					if(mIsTagsLoaded) {
						setButtonEnable();
						setContentShown(true);
					} else {
						setContentShown(false);
					}
				}
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			// TODO 自動生成されたメソッド・スタブ

		}

		@Override
		protected boolean isChanged() {
			boolean onchange = false;
    		ssbtitle = (SpannableStringBuilder) titletext.getText();
    		ssbcontent = (SpannableStringBuilder)contenttext.getText();
    		ssbtag = (SpannableStringBuilder)tagtext.getText();
    		editing_title = ssbtitle.toString();
    		editing_content = ssbcontent.toString();
    		editing_tags = ssbtag.toString();
    		editing_tags = editing_tags.replaceAll("，", ",");
    		editing_tags = editing_tags.replaceAll(", ", ",");
    		editing_tags = editing_tags.replaceAll("、", ",");
    		editing_tags = editing_tags.replaceAll("､", ",");
    		editing_tags = editing_tags.replaceAll("　", ",");
    		editing_tags = editing_tags.replaceAll(" ", ",");
    		if(mode) {
	    		String[] arr_user_tags = editing_tags.split(",");
	    		ArrayList<String> memo_tags = new ArrayList<String>();
	        	for(String utag: arr_user_tags) {
	        		utag = utag.trim();
	        		if(utag.equals("") == false) {
	        			if(memo_tags.indexOf(utag) == -1) {
	        				memo_tags.add(utag);
						}
	        		}
	        	}
	        	editing_tags = memo_tags.toString();
	        	editing_tags = editing_tags.substring(1, editing_tags.length() -1);
				if((editing_title.equals(item.title) == false) || (editing_content.equals(item.content) == false) || (editing_tags.equals(item.tags) == false)){
					// 元の内容と変わっていた場合
					onchange = true;
				}
    		} else {
    			// 新規登録時は、写真を撮影または選択しているので無条件に onchange = true
    			onchange = true;
    		}
			return onchange;
		}

		@Override
		public void onAbortDialogResult(int result) {
			if(result == android.app.Activity.RESULT_CANCELED) {
				if(mode == false) {
					// 撮影した一時ファイルを削除
					deleteTemporaryPhotoFile(new File(ShootFileName));
				}
				super.onAbortDialogResult(result);
			}
		}

		@Override
		public void onAbort() {
    		hideIME();
    		View Container = getContainerView();
    		if(Container != null) {
    			if(Container.getVisibility() == View.GONE) {
    				updateActionbar(true);
    				return;
    			}
    		}
			final String abortmsg = aborttoastmsg[mode ? 0 : 1];
			// 編集されているか確認
			if(isChanged()) {
				AbortDialogFragment f = new AbortDialogFragment();
				Bundle args = new Bundle();
				args.putBoolean("mode", mode);
				args.putStringArray("aborttoastmsg", aborttoastmsg);
				args.putStringArray("modealerttitle", modealerttitle);
				args.putStringArray("abortalertmsgs", abortalertmsgs);
				f.setArguments(args);
				f.setResultListener(this);
				f.show(getFragmentManager(), "AbortDialog");
			} else {
	    		// 編集されていない場合
	    		if(mode) {
	    			Toast.makeText(getSherlockActivity().getApplicationContext(), abortmsg, Toast.LENGTH_LONG).show();
	    		} else {
					// 撮影した一時ファイルを削除
					deleteTemporaryPhotoFile(new File(ShootFileName));
	    			Toast.makeText(getSherlockActivity().getApplicationContext(), abortmsg, Toast.LENGTH_LONG).show();
	    		}
	    		setResult(RESULT_CANCELED, null);

	    		FragmentTransaction ft = getSherlockActivity().getSupportFragmentManager().beginTransaction();
				ft.remove(this);
				ft.commit();
			}
		}

		private void deleteTemporaryPhotoFile(File tempPhotoFile) {
			if(picimage_mode == false && selectpic_mode == false && shootpic_mode) {
				afterImages = ImageManager.getImageList(getSherlockActivity());
				Iterator<String> iter = afterImages.iterator();
				while(iter.hasNext()) {
					String filename = iter.next();
					if(beforeImages.indexOf(filename) == -1) {
						File tempfile = new File(filename);
						ImageManager.deleteGalleryFile(getSherlockActivity().getContentResolver(), tempfile.getParent(), tempfile.getName());
						tempfile.delete();
					}
				}
				String checkpath = Environment.getExternalStorageDirectory().toString() + "/DCIM";
				deletePlusFile(checkpath, tempPhotoFile);

				ImageManager.deleteGalleryFile(getSherlockActivity().getContentResolver(), tempPhotoFile.getParent(), tempPhotoFile.getName());
				// 一時ファイルがあった場合は削除
				if(tempPhotoFile.exists()) {
					tempPhotoFile.delete();
				}
			}
			PreferencesUtil.setPreferences(getSherlockActivity().getApplicationContext(), Constant.ADD_PIC_FILENAME, "");
			//表示用キャッシュファイルの削除
			if(tempFileName.equals("") == false) {
				File cacheFile = new File(tempFileName);
				if(cacheFile.exists()) {
					cacheFile.delete();
				}
			}
		}

		private void deletePlusFile(String checkpath, File tempPhotoFile) {
			File checkDir = new File(checkpath);
			if(checkDir.isDirectory() && checkDir.exists()) {
				String[] files = checkDir.list();
				for(String file : files) {
					File checkfile = new File(file);
					if(checkfile.isDirectory()
					&& file.equals(".") == false
					&& file.equals("..") == false
					&& file.startsWith(".") == false) {
						deletePlusFile(checkpath + "/" + file, tempPhotoFile);
					} else if(checkfile.isFile() && tempPhotoFile.getName().equals(file)) {
						ImageManager.deleteGalleryFile(getSherlockActivity().getContentResolver(), checkpath, file);
						File plusfile = new File(checkpath + "/" + file);
						plusfile.delete();
					}
				}
			}
		}

		//----------------------------------------------------------------------

		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			super.onCreateOptionsMenu(menu, inflater);
			Configuration config = getResources().getConfiguration();
			if(config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
				visibleIconId = R.drawable.ic_menu_left;
				invisibleIconId = R.drawable.ic_menu_right;
			} else {
				visibleIconId = R.drawable.ic_menu_up;
				invisibleIconId = R.drawable.ic_menu_down;
			}
			MenuItem item = menu.findItem(R.id.edit_menu_frag_viewswitch);
			if(item != null) {
				item.setIcon(editarea_mode ? visibleIconId : invisibleIconId);
			}
			item = menu.findItem(R.id.edit_menu_frag_save);
			if(item != null && mode) {
				item.setTitle(R.string.updatebutton_description);
			}
			if(mode) {
				getSherlockActivity().getSupportActionBar().setTitle(getResources().getString(R.string.editphoto_title));
			} else {
				getSherlockActivity().getSupportActionBar().setTitle(getResources().getString(R.string.addphoto_title));
			}
		}
	    @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	    	switch(item.getItemId()) {
	    	case R.id.edit_menu_frag_save:
	    		Context context = getSherlockActivity().getApplicationContext();
	    		hideIME();
				if(mode == false) {
					String resultImageFileName = "";
					ShootFileName = PreferencesUtil.getPreferences(context, Constant.ADD_PIC_FILENAME, "");
					File resultFile = new File(ShootFileName);
					if(picimage_mode) {
						resultImageFileName = ShootFileName;
					} else if(selectpic_mode) {
						resultImageFileName = ShootFileName;
					} else if(shootpic_mode) {
						// アプリフォルダへ移動し、移動後のフルパスを得る
						resultImageFileName = ImageManager.addImageAsApplication(context, ShootFileName);
					}
					deleteTemporaryPhotoFile(resultFile);
		    		this.item.path = resultImageFileName;
				}
	    		ssbtitle = (SpannableStringBuilder) titletext.getText();
	    		ssbcontent = (SpannableStringBuilder)contenttext.getText();
	    		ssbtag = (SpannableStringBuilder)tagtext.getText();
	    		editing_title = ssbtitle.toString();
	    		editing_content = ssbcontent.toString();
	    		editing_tags = ssbtag.toString();
	    		if(editing_title.trim().equals("")) {
	    			editing_title = getResources().getString(R.string.photo_nontitle);
	    		}
	        	this.item.title = editing_title;
	        	this.item.content = editing_content;
	        	Date now = new Date();
	    		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	    		String strdate = sdf.format(now);
	    		this.item.date = strdate;
	    		this.item.updated = strdate;
	    		this.item.long_date = now.getTime();
	    		this.item.long_updated = this.item.long_date;
	        	if(mode == false) {
	        		this.item.long_created = this.item.long_date;
	        		this.item.created = strdate;
	        	}
	        	saveItem();
	        	Bundle extra = new Bundle();
	        	extra.putInt(Constant.FRAGMENT_RESULT_REQUEST, Constant.FRAGMENT_RESULT_READDATA);
	        	extra.putSerializable("item", this.item);
	        	setResult(RESULT_OK, extra);
	    		FragmentTransaction ft = getSherlockActivity().getSupportFragmentManager().beginTransaction();
				ft.remove(this);
				ft.commit();
//				getSherlockActivity().supportInvalidateOptionsMenu();
	    		break;
	    	case R.id.edit_menu_frag_cancel:
	    		onAbort();
	    		break;
			case R.id.edit_menu_frag_viewswitch:
				if(EditArea.getVisibility() == View.VISIBLE) {
					EditArea.setVisibility(View.GONE);
					item.setIcon(invisibleIconId);
					editarea_mode = false;
				} else {
					EditArea.setVisibility(View.VISIBLE);
					item.setIcon(visibleIconId);
					editarea_mode = true;
				}
				changelayout();
				loadPhotoHtml();
				break;
	    	}
	    	return true;
	    }
		//----------------------------------------------------------------------

		@Override
		public void onPhotoChoiceModeSelected(int mode) {
			switch(mode) {
			case PhotoChoiceModeSelectorDialogFragment.CHOICE_MODE_PICK:
				SelectAnImage();
				break;
			case PhotoChoiceModeSelectorDialogFragment.CHOICE_MODE_SHOOT:
				StartNativeCamera();
				break;
			case PhotoChoiceModeSelectorDialogFragment.CHOICE_MODE_CANCEL:
				Toast.makeText(getSherlockActivity().getApplicationContext(), R.string.cancel_intent_camera, Toast.LENGTH_LONG).show();
				cancelFinish();
				break;
			}
		}

		private void SelectAnImage() {
			selectpic_mode = true;
			Intent mIntent = new Intent();
			mIntent.setType("image/*");
			mIntent.setAction(Intent.ACTION_PICK);
			startActivityForResult(mIntent, Constant.REQUEST_ACTIVITY_GALLERY);
		}

	    private void StartNativeCamera() {
			System.gc();
			shootpic_mode = true;
			Context context = getSherlockActivity().getApplicationContext();
	    	beforeImages = ImageManager.getImageList(getSherlockActivity());
			Intent mIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			ShootFileName = ImageManager.getImageNameAsApplication(context);
			PreferencesUtil.setPreferences(context, Constant.ADD_PIC_FILENAME, ShootFileName);
			Uri ImageUri = Uri.fromFile(new File(ShootFileName));
			mIntent.putExtra(MediaStore.EXTRA_OUTPUT, ImageUri);
	    	/////////////////////////////////////////////////
			startActivityForResult(mIntent, Constant.REQUEST_ACTIVITY_INTENTCAMERA);
	    }

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent intent) {
			Context context = getSherlockActivity().getApplicationContext();
			switch(requestCode) {
			case Constant.REQUEST_ACTIVITY_GALLERY:
				if(resultCode != RESULT_OK || intent == null) {
					Toast.makeText(context, R.string.cancel_intent_camera, Toast.LENGTH_LONG).show();
					cancelFinish();
				} else {
					Uri ImageUri = intent.getData();
					ShootFileName = ImageUri.getPath();
					if(ShootFileName.startsWith("/external/images/media")) {
						try {
							ContentResolver cr = getSherlockActivity().getContentResolver();
							String[] columns = {MediaStore.Images.Media.DATA };
							Cursor c = cr.query(ImageUri, columns, null, null, null);
							if(c != null) {
								try {
									c.moveToFirst();
									ShootFileName = c.getString(0);
								} catch(Exception qe) {
									Toast.makeText(context, R.string.cancel_intent_camera, Toast.LENGTH_LONG).show();
									cancelFinish();
								}
							}
							c.close();
						} catch(Exception e) {
							Toast.makeText(context, R.string.cancel_intent_camera, Toast.LENGTH_LONG).show();
							cancelFinish();
						}
					}
					PreferencesUtil.setPreferences(context, Constant.ADD_PIC_FILENAME, ShootFileName);
					item.path = ShootFileName;
					mIsPhotoChoiced = true;
					// 表示用画像生成 AsyncTaskLoader実行
					getLoaderManager().initLoader(1, null, this);
/*
					initPhotoView(getSherlockActivity(), item.path);
					setItemData();
					mIsPhotoCreated = true;
					if(mIsTagsLoaded) {
						setButtonEnable();
						setContentShown(true);
					} else {
						setContentShown(false);
					}
*/
				}
				break;
			case Constant.REQUEST_ACTIVITY_INTENTCAMERA:
				if(resultCode == RESULT_OK) {
					Uri ImageUri = null;
		    		if(intent != null) {
		    			ImageUri = intent.getData();
		    		}
		    		File resultFile = null;
		    		ShootFileName = PreferencesUtil.getPreferences(context, Constant.ADD_PIC_FILENAME, "");
					resultFile = new File(ShootFileName);
					if(resultFile.length() == 0 && ImageUri != null) {
						InputStream inputStream = null;
						OutputStream outputStream = null;
						byte[] buf = new byte[256];
						int size;
						try {
							ContentResolver cr = getSherlockActivity().getContentResolver();
							inputStream = cr.openInputStream(ImageUri);
							outputStream = new FileOutputStream(ShootFileName);
							while((size = inputStream.read(buf)) != -1) {
								outputStream.write(buf, 0, size);
							}
							buf = null;
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						} finally {
							if(inputStream != null) {
								try {
									inputStream.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
							if(outputStream != null) {
								try {
									outputStream.close();
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
					} else if(resultFile.length() == 0 && ImageUri == null) {
						Toast.makeText(context, R.string.err_pic_not_found, Toast.LENGTH_LONG).show();
						// ファイルがないなら(写真を撮らないなら)アクティビティを終了
						cancelFinish();
					}
					System.gc();
					item.path = ShootFileName;
					mIsPhotoChoiced = true;
					// 表示用画像生成 AsyncTaskLoader実行
					getLoaderManager().initLoader(1, null, this);
/*
					initPhotoView(getSherlockActivity(), item.path);
					setItemData();
					mIsPhotoCreated = true;
					if(mIsTagsLoaded) {
						setButtonEnable();
						setContentShown(true);
					} else {
						setContentShown(false);
					}
*/
	    		} else {
	    			// カメラアプリでキャンセルされた場合、登録画像なないので終了
					Toast.makeText(context, R.string.cancel_intent_camera, Toast.LENGTH_LONG).show();
					cancelFinish();
	    		}
				break;
			}
		}

		private void initPhotoView(Context context, String path) {
			String imagefilename = path;
			if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
				// アプリケーション名を取得
				String appName = context.getResources().getString(R.string.app_name);
				// キャッシュディレクトリ/アプリケーション名のディレクトリパスを作成
				String CacheDirName = Environment.getExternalStorageDirectory().toString() + "/" + appName + "/.cache";
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
		}

		private void cancelFinish() {
    		setResult(RESULT_CANCELED, null);
    		try {
	    		FragmentTransaction ft = getSherlockActivity().getSupportFragmentManager().beginTransaction();
				ft.remove(this);
				ft.commit();
    		} catch(IllegalStateException ie) {
    			mFragmentControlListener.onRemoveRequest("edit");
    		}
//			getSherlockActivity().supportInvalidateOptionsMenu();
		}

		@Override
		protected void setupMsg() {
			aborttoastmsg = new String[] {
				getResources().getString(R.string.abort_editphoto_toastmsg),
				getResources().getString(R.string.abort_addphoto_toastmsg)
			};
			abortalertmsgs = new String[] {
				getResources().getString(R.string.abort_editphoto_alertmsg),
				getResources().getString(R.string.abort_addphoto_alertmsg)
			};
			modealerttitle = new String[] {
				getResources().getString(R.string.editphoto_alerttitle),
				getResources().getString(R.string.addphoto_alerttitle)
			};
			alertmsgs = new String[][] {
				{
					getResources().getString(R.string.editphoto_alertmsg_home),
					getResources().getString(R.string.editphoto_alertmsg_viewer),
					getResources().getString(R.string.editphoto_alertmsg_memo),
					getResources().getString(R.string.editphoto_alertmsg_todo)
				},
				{
					getResources().getString(R.string.addphoto_alertmsg_home),
					getResources().getString(R.string.addphoto_alertmsg_viewer),
					getResources().getString(R.string.addphoto_alertmsg_memo),
					getResources().getString(R.string.addphoto_alertmsg_todo)
				}
			};
		}

		@Override
		public void onMenuEvent(int menuId) {
			// TODO 自動生成されたメソッド・スタブ

		}
	}

	public static class PhotoConvertTask extends AsyncTaskLoader<Cursor> {
		private Context context;
		private String path;
		private String preview_html;
		private String tempFileName;
		private String imagefilename;
		private final static String[] RESULT_CURSOR = new String[] {
			"preview_html",
			"tempFileName"
		};
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
		public Cursor loadInBackground() {
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
			MatrixCursor c = new MatrixCursor(RESULT_CURSOR);
			c.addRow(
					new Object[] {
							preview_html,
							tempFileName
					}
			);
			return c;
		}
	}


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		getSupportMenuInflater().inflate(R.menu.edit_photo_frag_menu, menu);
    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
    }

	@Override
    public boolean dispatchKeyEvent(KeyEvent event){
		if(event.getKeyCode() == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN){
			PhotoEditFragment f = (PhotoEditFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
			if(f != null) {
				f.setOnFragmentControlListener(0, this);
				f.onAbort();
				return true;
			} else {
				super.dispatchKeyEvent(event);
			}
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	public void onAddNoteFragment(OnFragmentControlListener listener, Fragment fragment, int fragment_type, String tag) {
	}

	@Override
	public void onFragmentResult(Fragment fragment, int requestCode, int resultCode, Bundle extra) {
		finish();
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
