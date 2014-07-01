package com.kaznog.android.dreamnote.fragment;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.ViewGroup.LayoutParams;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.internal.view.menu.MenuBuilder;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.evernote.client.oauth.android.EvernoteSession;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.db.DreamNoteProvider;
import com.kaznog.android.dreamnote.db.schema.ItemsSchema;
import com.kaznog.android.dreamnote.dialogfragment.DeleteItemDialogFragment;
import com.kaznog.android.dreamnote.evernote.DreamPostEnmlService;
import com.kaznog.android.dreamnote.fragment.DeleteItemTaskFragmentActivity.DeleteItemTaskFragment;
import com.kaznog.android.dreamnote.fragment.HtmlEditFragmentActivity.HtmlEditFragment;
import com.kaznog.android.dreamnote.listener.DeleteItemDialogResultListener;
import com.kaznog.android.dreamnote.listener.OnFragmentControlListener;
import com.kaznog.android.dreamnote.settings.PreferencesUtil;
import com.kaznog.android.dreamnote.util.AppInfo;
import com.kaznog.android.dreamnote.util.Constant;
import com.kaznog.android.dreamnote.util.StringUtils;
import com.kaznog.android.dreamnote.util.extendFileUtils;

public class HtmlPreviewFragmentActivity extends SherlockFragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FragmentManager fm = getSupportFragmentManager();
		if(fm.findFragmentById(android.R.id.content) == null) {
			HtmlPreviewFragment f = new HtmlPreviewFragment();
			fm.beginTransaction().add(android.R.id.content, f).commit();
		}
	}

	public static class HtmlPreviewFragment extends PreviewFragment
	implements
	MenuBuilder.Callback,
	com.actionbarsherlock.internal.view.menu.MenuPresenter.Callback,
	LoaderManager.LoaderCallbacks<Cursor>,
	OnFragmentControlListener,
	DeleteItemDialogResultListener {
		private CursorLoader contentloader;
		private SendContentTask sendcontenttask;
		private boolean mIsSendContentTaskReq;
		private MenuBuilder mMenu;
		private MenuItem EverMenu;
		private MenuItem shareMenu;
		private View border001;
		private View border002;
		private ImageView catagory_line;
		private ScrollView preview_scrollarea;
		private int visibleIconId;
		private int invisibleIconId;
		private boolean scrollareaborder_mode = false;
		private TextView preview_title;
		private TextView preview_description;
		private TextView preview_tags;
		private WebView preview_html;
		private DisplayMetrics metrics;
		private boolean mIsContentLoaded;
		private boolean mJavascriptEnable;
		private boolean screen_mode;
		private Handler mHandler;
		private EvernoteSession session;

		public static HtmlPreviewFragment newInstance(Item item) {
			HtmlPreviewFragment f = new HtmlPreviewFragment();
			Bundle args = new Bundle();
			args.putSerializable("item", item);
			f.setArguments(args);
			return f;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	        View root = createContentView(inflater, R.layout.preview_html_frag);
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
			setupSession(this.getSherlockActivity().getApplicationContext());
			if(savedInstanceState == null) {
				contentloader = null;
				sendcontenttask = null;
				mIsSendContentTaskReq = false;
				mIsContentLoaded = false;
				item = (Item)getArguments().getSerializable("item");
				scrollareaborder_mode = false;
				screen_mode = false;
				mMenu = null;
				shareMenu = null;
				EverMenu = null;
		        // javascript実行設定を取得
				String strjavascript = PreferencesUtil.getPreferences(this.getSherlockActivity().getApplicationContext(), Constant.PREFS_JAVASCRIPT, "");
				mJavascriptEnable = strjavascript.equals("") ? false : true;

				setupUI(mContentContainer);
				Bundle args = new Bundle();
				args.putLong("id", item.id);
				getLoaderManager().initLoader(0, args, this);
				setContentShown(false);
			} else {
				mIsSendContentTaskReq = savedInstanceState.getBoolean("mIsSendContentTaskReq");
				screen_mode = savedInstanceState.getBoolean("screen_mode");
				mJavascriptEnable = savedInstanceState.getBoolean("mJavascriptEnable");
				mIsContentLoaded = savedInstanceState.getBoolean("mIsContentLoaded");
				item = (Item) savedInstanceState.getSerializable("item");
				scrollareaborder_mode = savedInstanceState.getBoolean("scrollareaborder_mode");
				setupUI(mContentContainer);
				if(mIsContentLoaded) {
					setItemData();
					setContentShown(true);
				} else {
					Bundle args = new Bundle();
					args.putLong("id", item.id);
					getLoaderManager().initLoader(0, args, this);
					setContentShown(false);
				}
			}
			setHasOptionsMenu(true);
		}

		@Override
		public void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);
			outState.putSerializable("item", item);
			outState.putBoolean("scrollareaborder_mode", scrollareaborder_mode);
			outState.putBoolean("mIsContentLoaded", mIsContentLoaded);
			outState.putBoolean("mJavascriptEnable", mJavascriptEnable);
			outState.putBoolean("screen_mode", screen_mode);
			outState.putBoolean("mIsSendContentTaskReq", mIsSendContentTaskReq);
			setUserVisibleHint(true);
		}

		@SuppressLint("NewApi")
		@Override
		public void onPause() {
			super.onPause();
			if(Build.VERSION.SDK_INT > 10) {
				if(preview_html != null) {
					preview_html.onPause();
				}
			}
		}

		@SuppressLint("NewApi")
		@Override
		public void onResume() {
			Loader<Cursor> loader = getLoaderManager().getLoader(1);
			if(loader != null) {
				getLoaderManager().destroyLoader(1);
			}
			if(Build.VERSION.SDK_INT > 10) {
				if(preview_html != null) {
					preview_html.onResume();
				}
			}
			super.onResume();
			if(session != null) {
				session.completeAuthentication(PreferencesUtil.getSharedPreferences(this.getSherlockActivity().getApplicationContext()));
				if(EverMenu != null) {
					setEvernoteMenuEnabled(EverMenu);
				}
			}
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
			extendFileUtils.clearfile(getSherlockActivity());
	    	super.onDestroyView();
		}

		@Override
		public void onDestroy() {
			if(preview_html != null) {
				preview_html.setWebChromeClient(null);
				preview_html.setWebViewClient(null);
				preview_html.clearView();
				preview_html.loadUrl("about:blank");
				preview_html.clearCache(true);
				preview_html.clearFormData();
				preview_html.clearHistory();
//				preview_html.destroy();
				preview_html = null;
			}
			if(mMenu != null) {
				mMenu.clear();
				mMenu = null;
			}
			if(mHandler != null) {
				mHandler = null;
			}
			super.onDestroy();
		}

		private void changelayout() {
			if(screen_mode) {
				catagory_line.setVisibility(View.GONE);
				border001.setVisibility(View.GONE);
				border002.setVisibility(View.GONE);
				preview_scrollarea.setVisibility(View.GONE);
				preview_html.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
				preview_title.setVisibility(View.GONE);
				return;
			}
			Configuration config = getResources().getConfiguration();
			if(config.orientation == Configuration.ORIENTATION_PORTRAIT) {
				if(preview_scrollarea.getVisibility() == View.INVISIBLE) {
		    		// 詳細エリア(備考本文、タグ)が表示状態でない場合
					preview_html.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
				} else {
					// htmlビューのY軸開始位置を取得
					int html_top = preview_html.getTop();
					// 詳細エリア(備考本文、タグ)が表示状態の場合
					preview_html.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, (metrics.heightPixels / 2) - html_top));
				}
			} else if(config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
				if(preview_scrollarea.getVisibility() == View.INVISIBLE) {
		    		// 詳細エリア(備考本文、タグ)が表示状態でない場合
					preview_html.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
				} else {
					// 写真ビューの幅を取得
					int html_width = (int)((metrics.widthPixels - (8 * metrics.density)) / 2)/*preview_title.getWidth() / 2*/;
					// 詳細エリア(備考本文、タグ)が表示状態の場合
					preview_html.setLayoutParams(new LinearLayout.LayoutParams(html_width, LayoutParams.MATCH_PARENT));
				}
			}
		}
		protected void setupUI(View v) {
	        metrics = new DisplayMetrics();
	        WindowManager manager = (WindowManager) getSherlockActivity().getSystemService(Context.WINDOW_SERVICE);
	        manager.getDefaultDisplay().getMetrics(metrics);

	        catagory_line = (ImageView)v.findViewById(R.id.item_category_line);
	        border001 = (View)v.findViewById(R.id.preview_html_border001);
	        border002 = (View)v.findViewById(R.id.preview_html_border002);
			preview_title = (TextView)v.findViewById(R.id.preview_title);
			preview_description = (TextView)v.findViewById(R.id.preview_description);
			preview_tags = (TextView)v.findViewById(R.id.preview_tags);
			// 写真サムネールのビューを取得
//			if(preview_html != null) {
//				preview_html.destroy();
//			}
			preview_html = (WebView)v.findViewById(R.id.preview_html_webview);
			// スクロール範囲のビューを取得
			preview_scrollarea = (ScrollView)v.findViewById(R.id.preview_scrollarea);
	        // モードに応じてスクロール範囲を(表示|非表示)にする
	        preview_scrollarea.setVisibility(scrollareaborder_mode ? View.VISIBLE : View.INVISIBLE);
			changelayout();
		}

		protected void setItemData() {
			preview_title.setText(item.title);
			if(item.description.trim().equals("")) {
				// 備考がない場合
				preview_description.setText(R.string.non_description);
				preview_description.setTextColor(Color.GRAY);
			} else {
				// 備考がある場合
				preview_description.setText(item.description);
				preview_description.setTextColor(Color.BLACK);
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
			String itempath = item.path;
	        // アプリケーション名を取得
	        String appName = getResources().getString(R.string.app_name);
			// フォルダ名生成
			String clipfilepath = Environment.getExternalStorageDirectory().toString() + "/" + appName + "/.clip/" + itempath;
			if(preview_html != null) {
				//Log.d(Constant.LOG_TAG, "HtmlPreviewFragment setItemData preview WebView exist!");
				// WebViewの初期化
				extendFileUtils.clearfile(getSherlockActivity());
				//Log.d(Constant.LOG_TAG, "HtmlPreviewFragment setItemData clearfile!");
				initializeWebView(preview_html, appName, mJavascriptEnable);
				//Log.d(Constant.LOG_TAG, "HtmlPreviewFragment setItemData initializeWebView called!");
				String data = "";
				String loadDataPath = "file://" + Environment.getDataDirectory() + "/data/DreamNote/";
				if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
					File clipdir = new File(clipfilepath);
					File clipindex = new File(clipfilepath + "/index.html");
					File clipbody = new File(clipfilepath + "/clipbody.jpg");
					if(clipdir.exists() && (clipindex.exists() || clipbody.exists())) {
						if(clipindex.exists()) {
							//Log.d(Constant.LOG_TAG, "HtmlPreviewFragment setItemData clipfileindex exist!");
							File[] clipcontents = clipdir.listFiles();
							for(File contentfile: clipcontents) {
								if(contentfile.isDirectory() == false && contentfile.getName().equals("thumbnail.info") == false) {
									try {
										InputStream is = new FileInputStream(contentfile);
										extendFileUtils.in2file(this.getSherlockActivity().getApplicationContext(), is, contentfile.getName());
										is = null;
									} catch (FileNotFoundException e) {
										// TODO 自動生成された catch ブロック
										//Log.d(Constant.LOG_TAG, "HtmlPreviewFragment setItemData FileNotFoundException");
										e.printStackTrace();
									} catch (Exception e) {
										// TODO 自動生成された catch ブロック
										//Log.d(Constant.LOG_TAG, "HtmlPreviewFragment setItemData Exception");
										e.printStackTrace();
									}
								}
							}
							//Log.d(Constant.LOG_TAG, "HtmlPreviewFragment setItemData clipfile loaded!");
							preview_html.loadUrl("content://com.kaznog.android.dreamnote/index.html");
							//Log.d(Constant.LOG_TAG, "HtmlPreviewFragment setItemData loadUrl!");
						} else {
							data = Constant.PREVIEW_CONTENT_HEAD + "<img src=\"" + clipfilepath + "/clipbody.jpg\"/>" + Constant.PREVIEW_CONTENT_FOOT;
							preview_html.loadDataWithBaseURL(loadDataPath, data, "text/html", "UTF-8", null);
							//Log.d(Constant.LOG_TAG, "HtmlPreviewFragment setItemData loadDataWithBaseURL!");
						}
					} else {
						String msg = String.format(getResources().getString(R.string.err_preview_clip_not_found), "[" + clipfilepath + "] ");
						data = Constant.PREVIEW_CONTENT_NOT_FOUND_HEAD + msg + Constant.PREVIEW_CONTENT_NOT_FOUND_FOOT;
						preview_html.loadDataWithBaseURL(loadDataPath, data, "text/html", "UTF-8", null);
						//Log.d(Constant.LOG_TAG, "HtmlPreviewFragment setItemData loadDataWithBaseURL!");
					}
				} else {
					String msg = String.format(getResources().getString(R.string.err_preview_clip_not_found), "[" + clipfilepath + "] ");
					data = Constant.PREVIEW_CONTENT_NOT_FOUND_HEAD + msg + Constant.PREVIEW_CONTENT_NOT_FOUND_FOOT;
					preview_html.loadDataWithBaseURL(loadDataPath, data, "text/html", "UTF-8", null);
					//Log.d(Constant.LOG_TAG, "HtmlPreviewFragment setItemData loadDataWithBaseURL!");
				}
			} else {
				//Log.d(Constant.LOG_TAG, "WebView not exist html no loading");
			}
		}

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			if(id == 0) {
				if(args != null) {
					contentId = args.getLong("id");
					Uri noteUri = ContentUris.withAppendedId(DreamNoteProvider.GETCONTENT_URI, contentId);
					contentloader = new CursorLoader(getActivity(), noteUri, null, null, null, null);
				}
				return contentloader;
			} else if(id == 1) {
				sendcontenttask = new SendContentTask(getSherlockActivity().getApplicationContext(), item.content, item.related);
				return sendcontenttask;
			}
			return null;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor result) {
			if(loader.getId() == 0) {
				if(result.moveToFirst()) {
					try {
						item.content = result.getString(result.getColumnIndexOrThrow(ItemsSchema.CONTENT));
					} catch(IllegalArgumentException e) {
						item.content = "";
					} catch(android.database.StaleDataException e) {
						item.content = "";
						Toast.makeText(getSherlockActivity().getApplicationContext(), "unknown DB error", Toast.LENGTH_LONG);
					}
					AppInfo.DebugLog(getSherlockActivity().getApplicationContext(), "html content loadfinished: " + item.content);
					setItemData();
					setShareMenuEnabled();
					setContentShown(true);
					mIsContentLoaded = true;
				}
				//result.close();
			} else if(loader.getId() == 1) {
				AppInfo.DebugLog(getSherlockActivity().getApplicationContext(), "shareTask onfinished");
				if(result.moveToFirst()) {
					getLoaderManager().destroyLoader(1);
					String content = "";
					try {
						content = result.getString(result.getColumnIndexOrThrow("result"));
					} catch(IllegalArgumentException e) {
						//result.close();
						return;
					}
					Intent intent = new Intent();
					intent.setAction(Intent.ACTION_SEND);
				    intent.setType("text/plain");
				    intent.putExtra(Intent.EXTRA_TEXT, content);
				    intent.putExtra(Intent.EXTRA_TITLE, item.title);
				    intent.putExtra(Intent.EXTRA_SUBJECT, item.title);
				    try {
				      getSherlockActivity().startActivity(intent);
				    } catch (android.content.ActivityNotFoundException ex) {
				      Toast.makeText(getSherlockActivity().getApplicationContext(), R.string.err_activity_not_found, Toast.LENGTH_SHORT).show();
				    }
				}
				AppInfo.DebugLog(getSherlockActivity().getApplicationContext(), "shareTask onfinished result close");
				//result.close();
			}
		}

		@Override
		public void onLoaderReset(Loader<Cursor> loader) {
			// TODO 自動生成されたメソッド・スタブ

		}

		@SuppressWarnings("deprecation")
		@SuppressLint("NewApi")
		private void initializeWebView(WebView view, String appName, boolean jsenable) {
			view.getSettings().setSupportMultipleWindows(false);
			view.getSettings().setJavaScriptEnabled(jsenable);
			view.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
				view.getSettings().setPluginState(WebSettings.PluginState.ON);
			} else {
				view.getSettings().setPluginsEnabled(true);
			}
			view.getSettings().setLightTouchEnabled(true);
			view.getSettings().setUseWideViewPort(true);
			view.getSettings().setLoadWithOverviewMode(true);
			view.getSettings().setSupportZoom(true);
			view.getSettings().setBuiltInZoomControls(true);
			if(Build.VERSION.SDK_INT > 10) {
				view.getSettings().setDisplayZoomControls(true);
			}
			view.getSettings().setDatabaseEnabled(true);
			view.getSettings().setDatabasePath(Environment.getDataDirectory() + "/data/" + appName + "/database");
			view.getSettings().setDomStorageEnabled(true);
			view.getSettings().setAppCachePath(Environment.getDataDirectory() + "/data/" + appName + "/cache");
			view.getSettings().setAppCacheEnabled(true);
			view.getSettings().setAllowFileAccess(false);
			view.getSettings().setLayoutAlgorithm(LayoutAlgorithm.NORMAL);
//			view.getSettings().setUserAgent(0);
			view.getSettings().setUserAgentString(null);
			view.getSettings().setCacheMode(WebSettings.LOAD_NORMAL);
			view.setWebViewClient(mHtmlPreviewWebViewClient);
			view.setWebChromeClient(null);
		}
		WebViewClient mHtmlPreviewWebViewClient = new WebViewClient() {
			@Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				if(url.startsWith("file://")) {
					view.stopLoading();
					return;
				}
			}
	    	//ページの読み込み完了
	    	@Override
	    	public void onPageFinished(WebView view, String url) {
	    	}
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url)
			{
				// YouTube video link
				if (url.startsWith("vnd.youtube:"))
				{
				    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
				    return true;
				}
				if (url.startsWith("tel:")) {
			        Intent call = new Intent(Intent.ACTION_DIAL);
			        call.setData(Uri.parse(url));
			        startActivity(call);
			        return true;
				}
				if(url.startsWith("mailto:")) {
					url = url.replaceFirst("mailto:", "");
			        url = url.trim();
			        Intent i = new Intent(Intent.ACTION_SEND);
			        i.setType("plain/text").putExtra(Intent.EXTRA_EMAIL, new String[]{url});
			        startActivity(i);
			        return true;
				}
				Matcher m = Pattern.compile(".?youtube.com\\/.+?watch\\?v=([a-z0-9A-Z_]+)", Pattern.CASE_INSENSITIVE).matcher(url);
				if(m.find()) {
					String id = m.group(1);
				    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id)));
				    return true;
				}
				if (url.startsWith("file://")) {
					return true;
				}
				if(url.startsWith("content://")) {
					return true;
				}
				if(url.startsWith("http://") || url.startsWith("https://")) {
					startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
					return true;
				}
				return true;
			}
		};

		@Override
		public void onAbort() {
			if(screen_mode) {
				// 最大化解除
				screen_mode = false;
				getSherlockActivity().getSupportActionBar().show();
				preview_title.setVisibility(View.VISIBLE);
				catagory_line.setVisibility(View.VISIBLE);
				border001.setVisibility(View.VISIBLE);
				border002.setVisibility(View.VISIBLE);
				preview_scrollarea.setVisibility(scrollareaborder_mode ? View.VISIBLE : View.INVISIBLE);
				mHandler.post( new Runnable() {
		    		public void run() {
		    			while(getSherlockActivity().getSupportActionBar().isShowing() == false
		    			&& preview_html.getTop() == 0) {
		    				try {
		    					Thread.sleep(500);
							} catch (InterruptedException e) {
								// TODO 自動生成された catch ブロック
								e.printStackTrace();
							}
		    			}
						changelayout();
		    		}
				});
				return;
			}
			setResult(RESULT_CANCELED, null);
    		FragmentTransaction ft = getSherlockActivity().getSupportFragmentManager().beginTransaction();
			ft.remove(this);
			ft.commit();

			//sherlock master dev
//			getSherlockActivity().supportInvalidateOptionsMenu();
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
			MenuItem item = null;
			super.onCreateOptionsMenu(menu, inflater);
			Configuration config = getResources().getConfiguration();
			if(config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
				visibleIconId = R.drawable.ic_menu_right;
				invisibleIconId = R.drawable.ic_menu_left;
			} else {
				visibleIconId = R.drawable.ic_menu_down;
				invisibleIconId = R.drawable.ic_menu_up;
			}
			item = menu.findItem(R.id.pre_menu_frag_viewswitch);
			if(item != null) {
				item.setIcon(scrollareaborder_mode ? visibleIconId : invisibleIconId);
			}
			item = menu.findItem(R.id.pre_menu_frag_javascript);
			setJavascriptMenu(item);
			EverMenu = menu.findItem(R.id.pre_menu_frag_ever);
			setEvernoteMenuEnabled(EverMenu);
			item = menu.findItem(R.id.pre_menu_frag_browser);
			setBrowserMenuEnabled(item);
			shareMenu = menu.findItem(R.id.pre_menu_frag_share);
			setShareMenuEnabled();
		}

	    @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	    	//Log.d(Constant.LOG_TAG, this.getClass().getSimpleName() + " onOptionsItemSelected");
	    	if(mIsContentLoaded) {
	    		return onMenuItemSelected(null, item);
	    	}
	    	return false;
	    }

		private void setShareMenuEnabled() {
			if(shareMenu != null) {
				if(item != null) {
					shareMenu.setEnabled(item.content.equals("") ? false : true);
				}
			}
		}

		private void setBrowserMenuEnabled(MenuItem item) {
			if(item != null) {
				boolean enabled = this.item.related.equals("") ? false : true;
				item.setEnabled(enabled);
				item.setVisible(enabled);
			}
		}

		private void setEvernoteMenuEnabled(MenuItem item) {
			if(item != null) {
				boolean loggedIn = false;
				if(session != null) {
					loggedIn = session.isLoggedIn();
				}
				item.setEnabled(loggedIn);
				item.setVisible(loggedIn);
			}
		}

		private void setJavascriptMenu(MenuItem item) {
			int iconid;
			int titleid;
			if(item != null) {
				if(mJavascriptEnable) {
					iconid = R.drawable.ic_menu_javascript_stop;
					titleid = R.string.menu_javascript_stop_description;
				} else {
					iconid = R.drawable.ic_menu_javascript_run;
					titleid = R.string.menu_javascript_run_description;
				}
				item.setIcon(iconid);
				item.setTitle(titleid);
			}
		}

		@Override
		public boolean onMenuItemSelected(MenuBuilder menu, MenuItem item) {
			if(mIsContentLoaded == false) return false;
			switch(item.getItemId()) {
			case R.id.pre_menu_frag_del:
				DeleteItemDialogFragment delf = new DeleteItemDialogFragment();
				Bundle args = new Bundle();
				args.putString("message", getResources().getString(R.string.item_delete_confirm_clip_extend_data_msg));
				delf.setArguments(args);
				delf.setResultListener(this);
				delf.show(getSherlockActivity().getSupportFragmentManager(), "DeleteItemDialog");
				return true;
			case R.id.pre_menu_frag_edit:
				if(mFragmentControlListener != null) {
					HtmlEditFragment f = HtmlEditFragment.newInstance(this.item);
					mFragmentControlListener.onAddNoteFragment(this, f, Notes.EDIT_HTML_FRAGMENT, "edit");
				}
				return true;
			case R.id.pre_menu_frag_share:
				getLoaderManager().initLoader(1, null, this);
				return true;
			case R.id.pre_menu_frag_browser:
				Intent browserintent = new Intent(Intent.ACTION_VIEW, Uri.parse(this.item.related));
				this.getSherlockActivity().startActivity(browserintent);
				return true;
			case R.id.pre_menu_frag_fullscreen:
				screen_mode = true;
				getSherlockActivity().getSupportActionBar().hide();
				changelayout();
				Toast.makeText(getSherlockActivity().getApplicationContext(), R.string.preview_html_fullscreen, Toast.LENGTH_SHORT).show();
				return true;
			case R.id.pre_menu_frag_ever_simple:
				Toast.makeText(getSherlockActivity().getApplicationContext(), R.string.please_wait, Toast.LENGTH_LONG).show();
				Intent everintent_simple = new Intent(this.getSherlockActivity().getApplicationContext(), DreamPostEnmlService.class);
				everintent_simple.putExtra("item", this.item);
				everintent_simple.putExtra("mode", false);
				this.getSherlockActivity().startService(everintent_simple);
				return true;
			case R.id.pre_menu_frag_ever_style:
				Toast.makeText(getSherlockActivity().getApplicationContext(), R.string.please_wait, Toast.LENGTH_LONG).show();
				Intent everintent_style = new Intent(this.getSherlockActivity().getApplicationContext(), DreamPostEnmlService.class);
				everintent_style.putExtra("item", this.item);
				everintent_style.putExtra("mode", true);
				this.getSherlockActivity().startService(everintent_style);
				return true;
			case R.id.pre_menu_frag_javascript:
				mJavascriptEnable = mJavascriptEnable ? false : true;
				setJavascriptMenu(item);
				setItemData();
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
				return true;
			}
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

		@Override
		public void onAddNoteFragment(OnFragmentControlListener listener,
				Fragment fragment, int fragment_type, String tag) {
		}

		@Override
		public void onFragmentResult(Fragment fragment, int requestCode,
				int resultCode, Bundle extra) {
			if(requestCode == Notes.EDIT_HTML_FRAGMENT) {
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
					"htmls"
			};
			Context context = getSherlockActivity().getApplicationContext();
			getSherlockActivity().getContentResolver().delete(DreamNoteProvider.ITEMS_CONTENT_URI, null, whereArgs);
			String strResult = context.getResources().getString(R.string.itemtype_clip) + "\n" + item.title + context.getResources().getString(R.string.item_delete_msg_foot);
			Toast.makeText(context, strResult, Toast.LENGTH_LONG).show();
			onAbort();
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

	public static class SendContentTask extends AsyncTaskLoader<Cursor> {
		private Context context;
		private String body;
		private String related;
		private String[] RESULT_CURSOR;
		public SendContentTask(Context context, String content, String related) {
			super(context);
			this.context = context;
			body = content;
			if(body == null) {
				body = "";
			}
			this.related = related;
			RESULT_CURSOR = new String[] { "result" };
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
			AppInfo.DebugLog(context, "SendContenttask loadInBackground start");
			if(body.equals("") == false) {
				Matcher m = Pattern.compile("(<head)(.+?)(</head>)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(body);
				if(m.find()) {
					body = m.replaceAll("");
				}
				m = Pattern.compile("(<style)(.+?)(</style>)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(body);
				if(m.find()) {
					body = m.replaceAll("");
				}
				m = Pattern.compile("(<script)(.+?)(</script>)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(body);
				if(m.find()) {
					body = m.replaceAll("");
				}
				m = Pattern.compile("(//<\\!\\[CDATA\\[)(.+?)(//\\]\\]>)", Pattern.DOTALL).matcher(body);
				if(m.find()) {
					body = m.replaceAll("");
				}
				m = Pattern.compile("<br([^/>](/>|>)|(/>|>))", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll("\n");
				}
				m = Pattern.compile("</div>", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll("\n");
				}
	/*
				m = Pattern.compile("<hr(\"[^\"]*\"|'[^']*'|[^'\">])*>)", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll("\n");
				}
	*/
				m = Pattern.compile("</h\\d+([^>]>|>)", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll("\n");
				}
	/*
				m = Pattern.compile("<address(\"[^\"]*\"|'[^']*'|[^'\">])*>)", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll("\n");
				}
				m = Pattern.compile("<blockquote(\"[^\"]*\"|'[^']*'|[^'\">])*>)", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll("\n");
				}
				m = Pattern.compile("<dl(\"[^\"]*\"|'[^']*'|[^'\">])*>)", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll("\n");
				}
				m = Pattern.compile("<fieldset(\"[^\"]*\"|'[^']*'|[^'\">])*>)", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll("\n");
				}
				m = Pattern.compile("<form(\"[^\"]*\"|'[^']*'|[^'\">])*>)", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll("\n");
				}
				m = Pattern.compile("<noframes(\"[^\"]*\"|'[^']*'|[^'\">])*>)", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll("\n");
				}
				m = Pattern.compile("<ol(\"[^\"]*\"|'[^']*'|[^'\">])*>)", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll("\n");
				}
				m = Pattern.compile("<p(\"[^\"]*\"|'[^']*'|[^'\">])*>)", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll("\n");
				}
				m = Pattern.compile("<pre(\"[^\"]*\"|'[^']*'|[^'\">])*>)", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll("\n");
				}
				m = Pattern.compile("<table(\"[^\"]*\"|'[^']*'|[^'\">])*>)", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll("\n");
				}
				m = Pattern.compile("<ul(\"[^\"]*\"|'[^']*'|[^'\">])*>)", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll("\n");
				}
				m = Pattern.compile("<tr(\"[^\"]*\"|'[^']*'|[^'\">])*>)", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll("\n");
				}
				m = Pattern.compile("<td(\"[^\"]*\"|'[^']*'|[^'\">])*>)", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll(" ");
				}
				m = Pattern.compile("<a(\"[^\"]*\"|'[^']*'|[^'\">])*>)", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll(" ");
				}
	*/
				m = Pattern.compile("<(\"[^\"]*\"|'[^']*'|[^'\">])*>", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll("");
				}
				m = Pattern.compile("&nbsp;", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll(" ");
				}
				m = Pattern.compile("&amp;", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll("&");
				}
				m = Pattern.compile("&lt;", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll("<");
				}
				m = Pattern.compile("&gt;", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll(">");
				}
				m = Pattern.compile("&quot;", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll("\"");
				}
				m = Pattern.compile("&apos;", Pattern.CASE_INSENSITIVE).matcher(body);
				if(m.find()) {
					body = m.replaceAll("'");
				}
				body = body.replaceAll("^[\\s　]*", "").replaceAll("[\\s　]*$", "");
				body = body.trim();
				String[] arrbody = body.split("\n");
				StringBuilder sb = new StringBuilder();
				sb.append(related);
				sb.append("\n\n");
				for(String line: arrbody) {
					if(StringUtils.isBlank(line)) { continue; }
					if(line.equals("\n")) { continue; }
					sb.append(line);
					sb.append("\n\n");
				}
				sb.deleteCharAt(sb.length() - 1);
				body = sb.toString();
				sb.setLength(0);
				sb = null;
			}
			MatrixCursor c = new MatrixCursor(RESULT_CURSOR);
			c.addRow(new Object[] {
				body
			});
			return c;
		}
	}
}
