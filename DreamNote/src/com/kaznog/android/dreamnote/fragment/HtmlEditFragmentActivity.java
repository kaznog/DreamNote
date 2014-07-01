package com.kaznog.android.dreamnote.fragment;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Picture;
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
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.InputMethodManager;
import android.webkit.CacheManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebBackForwardList;
import android.webkit.WebChromeClient;
import android.webkit.WebIconDatabase;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.widget.ImageButton;
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
import com.kaznog.android.dreamnote.listener.OnFragmentControlListener;
import com.kaznog.android.dreamnote.settings.PreferencesUtil;
import com.kaznog.android.dreamnote.smartclip.ArrayListClipWebcache;
import com.kaznog.android.dreamnote.smartclip.ClipQueueItem;
import com.kaznog.android.dreamnote.smartclip.ClipService;
import com.kaznog.android.dreamnote.smartclip.ClipWebcache;
import com.kaznog.android.dreamnote.util.AppInfo;
import com.kaznog.android.dreamnote.util.Constant;
import com.kaznog.android.dreamnote.util.StringUtils;
import com.kaznog.android.dreamnote.util.extendFileUtils;
import com.kaznog.android.dreamnote.widget.DreamNoteEditText;
import com.kaznog.android.dreamnote.widget.DreamNoteEditText.DreamNoteEditTextListener;
import com.kaznog.android.dreamnote.widget.DreamNoteTitleEditText;
import com.kaznog.android.dreamnote.widget.DreamNoteTitleEditText.DreamNoteTitleEditTextListener;
import com.kaznog.android.dreamnote.widget.TagAutoCompleteTextView;
import com.kaznog.android.dreamnote.widget.TagAutoCompleteTextView.TagAutoCompleteTextViewListener;

public class HtmlEditFragmentActivity extends SherlockFragmentActivity
implements OnFragmentControlListener {
	private HtmlEditFragment htmleditfrag;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		AppInfo.DebugLog(getApplicationContext(), "HtmlEditFragmentActivity onCreate");
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
			// 初回起動時
	        Intent i = getIntent();

	        if(Intent.ACTION_SEND.equals(i.getAction())) {
	        	// WEBページ共有にて共有されたURLを取得
	        	CharSequence cs = i.getExtras().getCharSequence(Intent.EXTRA_TEXT);
	        	if(cs == null) { finish(); }
	        	String requestUrl = cs.toString();
	        	requestUrl = requestUrl.replaceAll("\r\n", "\n");
	        	requestUrl = requestUrl.replaceAll("\r", "\n");
	        	requestUrl = requestUrl.replaceAll(" ", "\n");
	        	if(requestUrl.indexOf("\n") != -1) {
	        		String[] arrargs = requestUrl.split("\n");
	        		for(String arg: arrargs) {
	        			arg = arg.trim();
	        			if(arg.startsWith("http")) {
	        				requestUrl = arg;
	        				break;
	        			}
	        		}
	        	}
	        	if(requestUrl.startsWith("http") == false) {
	        		Toast.makeText(getApplicationContext(), R.string.err_url_not_found, Toast.LENGTH_SHORT).show();
	        		finish();
	        	}
				AppInfo.DebugLog(getApplicationContext(), "requestUrl: " + requestUrl);
				if(requestUrl.equals("https://mobile.twitter.com/") || requestUrl.equals("https://mobile.twitter.com/#!/")) {
					requestUrl = "https://mobile.twitter.com/session/new";
				}
				FragmentManager fm = getSupportFragmentManager();
				if(fm.findFragmentById(android.R.id.content) == null) {
					htmleditfrag = new HtmlEditFragment();
					htmleditfrag.setOnFragmentControlListener(0, this);
					Bundle args = new Bundle();
					args.putString("requestUrl", requestUrl);
					htmleditfrag.setArguments(args);
					htmleditfrag.setRetainInstance(true);
					fm.beginTransaction().add(android.R.id.content, htmleditfrag).commit();
				}
	        } else {
	        	finish();
	        }
    	}
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		AppInfo.DebugLog(getApplicationContext(), "HtmlEditFragmentActivity onConfigurationChanged");
		super.onConfigurationChanged(newConfig);
		htmleditfrag.changelayout(true);
		supportInvalidateOptionsMenu();
	}

    @Override
    public void onDestroy() {
		AppInfo.DebugLog(getApplicationContext(), "HtmlEditFragmentActivity onDestroy");
    	extendFileUtils.clearfile(getApplicationContext());
    	WebIconDatabase.getInstance().close();
    	super.onDestroy();
    }

	public static class HtmlEditFragment extends EditFragment
	implements LoaderManager.LoaderCallbacks<Cursor>, TagAutoCompleteTextViewListener, DreamNoteTitleEditTextListener, DreamNoteEditTextListener {
		private String appName;
		private int visibleIconId;
		private int invisibleIconId;
		private DisplayMetrics metrics;
		private Configuration config;
		private LinearLayout LandArea;
		private RelativeLayout EditArea;
		private RelativeLayout EditArea_hide;
		protected DreamNoteTitleEditText titletext_hide;
		protected DreamNoteEditText contenttext_hide;
		protected TagAutoCompleteTextView tagtext_hide;
		protected ImageButton TagSelButton_hide;


		public WebView preview_html;
		private boolean editarea_mode;
		public String requestUrl;
		public String preview_content;
		public String contentbackup;
		public String interfacename;
		public boolean readclipFlag;
		public boolean mIsClipping;
		public boolean clipmode;
		private PostClipTask cliptask = null;
		private ClipChromeClient CCClient = null;
		private Bitmap favicon;

		public static HtmlEditFragment newInstance(Item item) {
			HtmlEditFragment f = new HtmlEditFragment();
			Bundle args = new Bundle();
			args.putSerializable("item", item);
			f.setArguments(args);
			return f;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	        View root = createContentView(inflater, R.layout.edit_html_frag);
			return root;
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			AppInfo.DebugLog(getActivity(), "HtmlEditFragment onActivityCreated");
			super.onActivityCreated(savedInstanceState);
	        appName = getResources().getString(R.string.app_name);
			Context context = getSherlockActivity().getApplicationContext();
			setupMsg();
			if(savedInstanceState == null) {
		        try {
			        CookieSyncManager.createInstance(getSherlockActivity().getApplicationContext());
					CookieManager cookieManager = CookieManager.getInstance();
			    	cookieManager.removeAllCookie();
			    	cookieManager.acceptCookie();
		        } catch(IllegalStateException e) {
		        } catch(Exception e) {
		        }
		        interfacename = "HTMLOUT" + java.util.UUID.randomUUID().toString().replace("-", "");
				editarea_mode = true;
				mIsClipping = false;
				readclipFlag = false;
				favicon = null;
				handler = null;
				Bundle args = getArguments();
				if(args != null) {
					requestUrl = args.getString("requestUrl");
					if(requestUrl == null) {
						// 編集
						mode = true;
						item = (Item)args.getSerializable("item");
						initializeData();
						changelayout(true);
						setItemData();
						preview_html.requestFocus(View.FOCUS_DOWN);
						preview_html.setOnTouchListener(new View.OnTouchListener() {

							@Override
							public boolean onTouch(View v, MotionEvent event) {
								if(v != null) {
									switch(event.getAction()) {
									case MotionEvent.ACTION_DOWN:
									case MotionEvent.ACTION_UP:
										if(!v.hasFocus()) {
											v.requestFocus();
										}
										break;
									}
								}
								return false;
							}
						});
						loadClipHtml();
					} else {
						if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) == false) {
							Toast.makeText(context, getResources().getString(R.string.err_external_storage) + "\n" + aborttoastmsg[mode ? 0 : 1], Toast.LENGTH_SHORT).show();
							cancelFinish();
						}
						mode = false;
						// ブラウザからの共有
						item = new Item();
						initializeData();
						changelayout(true);
						initializePreviewWebView(preview_html);
						preview_html.getSettings().setJavaScriptEnabled(true);
						setItemData();
						preview_html.requestFocus(View.FOCUS_DOWN);
						preview_html.setOnTouchListener(new View.OnTouchListener() {

							@Override
							public boolean onTouch(View v, MotionEvent event) {
								if(v != null) {
									switch(event.getAction()) {
									case MotionEvent.ACTION_DOWN:
									case MotionEvent.ACTION_UP:
										if(!v.hasFocus()) {
											v.requestFocus();
										}
										break;
									}
								}
								return false;
							}
						});
						preview_html.addJavascriptInterface(new contentloaderInterface(), interfacename);
						preview_html.setWebViewClient(new ClipClient(context));
						CCClient = new ClipChromeClient(context);
						preview_html.setWebChromeClient(CCClient);
						preview_html.loadUrl(requestUrl);
					}
					// start tagsloader
					getLoaderManager().initLoader(0, null, this);
					setContentShown(false);
				} else {
					cancelFinish();
				}
			} else {
				onActivityCreatedRestore(savedInstanceState);
				// UIの初期化
				changelayout(true);
				setItemData();
				preview_html.requestFocus(View.FOCUS_DOWN);
				preview_html.setOnTouchListener(new View.OnTouchListener() {

					@Override
					public boolean onTouch(View v, MotionEvent event) {
						if(v != null) {
							switch(event.getAction()) {
							case MotionEvent.ACTION_DOWN:
							case MotionEvent.ACTION_UP:
								if(!v.hasFocus()) {
									v.requestFocus();
								}
								break;
							}
						}
						return false;
					}
				});
				if(mode) {
					loadClipHtml();
				} else {
					initializePreviewWebView(preview_html);
					preview_html.getSettings().setJavaScriptEnabled(true);
					preview_html.addJavascriptInterface(new contentloaderInterface(), interfacename);
					preview_html.setWebViewClient(new ClipClient(context));
					CCClient = new ClipChromeClient(context);
					preview_html.setWebChromeClient(CCClient);
					preview_html.loadUrl(requestUrl);
				}
				if(mIsTagsLoaded) {
					setContentShown(true);
				} else {
					getLoaderManager().initLoader(0, null, this);
					setContentShown(false);
				}
			}
			setHasOptionsMenu(true);
		}

		@Override
		protected void onActivityCreatedRestore(Bundle savedInstanceState) {
			super.onActivityCreatedRestore(savedInstanceState);
			editarea_mode = savedInstanceState.getBoolean("editarea_mode");
			requestUrl = savedInstanceState.getString("requestUrl");
			preview_content = savedInstanceState.getString("preview_content");
			readclipFlag = savedInstanceState.getBoolean("readclipFlag");
			mIsClipping = savedInstanceState.getBoolean("mIsClipping");
			interfacename = savedInstanceState.getString("interfacename");
		}


		@Override
		protected void onSaveInstanceMoreState(Bundle outState) {
			super.onSaveInstanceMoreState(outState);
			outState.putBoolean("editarea_mode", editarea_mode);
			outState.putString("requestUrl", requestUrl);
			outState.putString("preview_content", "preview_content");
			outState.putBoolean("readclipFlag", readclipFlag);
			outState.putBoolean("mIsClipping", mIsClipping);
			outState.putString("interfacename", interfacename);
		}

		@SuppressLint("NewApi")
		@Override
		public void onPause() {
			super.onPause();
			CookieSyncManager.getInstance().sync();
			if(Build.VERSION.SDK_INT > 10) {
				if(preview_html != null) {
					preview_html.onPause();
				}
			}
		}

		@SuppressLint("NewApi")
		@Override
		public void onResume() {
			if(Build.VERSION.SDK_INT > 10) {
				if(preview_html != null) {
					preview_html.onResume();
				}
			}
			CookieSyncManager.getInstance().stopSync();
			super.onResume();
		}

		@Override
		public void onDestroyView() {
	    	super.onDestroyView();
		}

		@SuppressLint("NewApi") @Override
		public void onDestroy() {
			AppInfo.DebugLog(getSherlockActivity().getApplicationContext(), "HtmlEditFragment onDestroy");
			if(preview_html != null) {
	    		if(Build.VERSION.SDK_INT > 10) {
	    			preview_html.removeJavascriptInterface(interfacename);
	    		} else {
	    			preview_html.addJavascriptInterface(null, interfacename);
	    		}
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
	    	try {
		    	CookieManager cookieManager = CookieManager.getInstance();
		    	cookieManager.removeAllCookie();
		    	cookieManager = null;
	    	} catch(IllegalStateException e) {
	    	} catch(Exception e) {
	    	}
	    	if(favicon != null) { favicon.recycle(); favicon = null; }
			super.onDestroy();
		}

		@Override
		protected void initializeData() {
			super.initializeData();
			if(mode) {
				editing_title = item.title;
				editing_content = item.description;
				editing_tags = item.tags;
			} else {
				item.datatype = DreamNoteProvider.ITEMTYPE_PHOTO;
				editing_title = "";
				editing_content = "";
				editing_tags = "";
			}
		}

		public void changelayout(boolean setup) {
	        metrics = new DisplayMetrics();
	        WindowManager manager = (WindowManager) getSherlockActivity().getSystemService(Context.WINDOW_SERVICE);
	        manager.getDefaultDisplay().getMetrics(metrics);
			config = getResources().getConfiguration();
			if(setup) {
				setupUI(mContentContainer);
			}
			EditArea_hide.setVisibility(View.GONE);
			if(config.orientation == Configuration.ORIENTATION_PORTRAIT) {
				if(EditArea.getVisibility() == View.VISIBLE) {
					// 編集エリアが表示状態の場合
					int editarea_top = EditArea.getTop() * 2;
					EditArea.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, (metrics.heightPixels - editarea_top) / 2));
					LandArea.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, (metrics.heightPixels - editarea_top) / 2));
//					LandArea.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0));
					preview_html.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
//					EditArea.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 1));
				} else {
					// 編集エリアが表示状態でない場合
//					EditArea.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT, 0));
					LandArea.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
					preview_html.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
				}
			} else if(config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
				LandArea.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
				if(EditArea.getVisibility() == View.VISIBLE) {
					// 編集エリアが表示状態の場合
					int photo_width = (int)(metrics.widthPixels / 2);
					EditArea.setLayoutParams(new LinearLayout.LayoutParams(photo_width, LayoutParams.MATCH_PARENT));
					preview_html.setLayoutParams(new LinearLayout.LayoutParams(photo_width, LayoutParams.MATCH_PARENT));
				} else {
					// 編集エリアが表示状態でない場合
					preview_html.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
				}
			}
		}
		@Override
		protected void setupUI(View v) {
			if(handler == null) {
				handler = new Handler();
			}
			setupContainer();
			LandArea = (LinearLayout)v.findViewById(R.id.memo_edit_land);
	        preview_html = (WebView)v.findViewById(R.id.edit_html_webview);
			if(config.orientation == Configuration.ORIENTATION_PORTRAIT) {
		        EditArea = (RelativeLayout)v.findViewById(R.id.editarea);
				EditArea_hide = (RelativeLayout)v.findViewById(R.id.editarea_land);
		    	// タイトル入力ビューの取得
				titletext = (DreamNoteTitleEditText)v.findViewById(R.id.memo_title);
				titletext.removeTextChangedListener(mTitleTextWatcher);
				titletext.addTextChangedListener(mTitleTextWatcher);
				titletext.setDreamNoteTitleEditTextListener(this);
				titletext.setOnFocusChangeListener(mFocusChangeListener);

				titletext_hide = (DreamNoteTitleEditText)v.findViewById(R.id.memo_title_land);
				titletext_hide.removeTextChangedListener(mTitleTextWatcher);
				// 本文入力ビューの取得
				contenttext = (DreamNoteEditText)v.findViewById(R.id.memo_content);
				contenttext.removeTextChangedListener(mContentTextWatcher);
				contenttext.addTextChangedListener(mContentTextWatcher);
				contenttext.setDreamNoteEditTextListener(this);
				contenttext.setOnFocusChangeListener(mFocusChangeListener);

				contenttext_hide = (DreamNoteEditText)v.findViewById(R.id.memo_content_land);
				contenttext_hide.removeTextChangedListener(mContentTextWatcher);
				// タグ入力ビューの取得
		    	tagtext = (TagAutoCompleteTextView)v.findViewById(R.id.MultiAutoCompleteTagText);
		    	tagtext.removeTextChangedListener(mTextWatcher);
		    	tagtext.addTextChangedListener(mTextWatcher);
		    	tagtext.setTagAutoCompleteTextViewListener(this);
		    	tagtext.setOnFocusChangeListener(mFocusChangeListener);

		    	tagtext_hide = (TagAutoCompleteTextView)v.findViewById(R.id.MultiAutoCompleteTagText_land);
		    	tagtext_hide.removeTextChangedListener(mTextWatcher);
				// タグ選択ボタンビューの取得
		    	TagSelButton = (ImageButton)v.findViewById(R.id.TagSelButton);
		    	TagSelButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View view) {
						if(arrTags != null && arrTags.length > 0) {
							showTagSelectorDialog();
						}
					}
		    	});
		    	TagSelButton_hide = (ImageButton)v.findViewById(R.id.TagSelButton_land);
		    	TagSelButton_hide.setOnClickListener(null);

		    	// タグ読み込み中は無効にしておく
		    	TagSelButton.setEnabled(false);
		    	TagSelButton_hide.setEnabled(false);

		    	titletext.setOnEditorActionListener(mOnTitleEditorActionListener);
		    	tagtext.setOnEditorActionListener(mOnTagEditorActionListener);

			} else if(config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
		        EditArea = (RelativeLayout)v.findViewById(R.id.editarea_land);
				EditArea_hide = (RelativeLayout)v.findViewById(R.id.editarea);
		    	// タイトル入力ビューの取得
				titletext = (DreamNoteTitleEditText)v.findViewById(R.id.memo_title_land);
				titletext.removeTextChangedListener(mTitleTextWatcher);
				titletext.addTextChangedListener(mTitleTextWatcher);
				titletext.setDreamNoteTitleEditTextListener(this);
				titletext.setOnFocusChangeListener(mFocusChangeListener);

				titletext_hide = (DreamNoteTitleEditText)v.findViewById(R.id.memo_title);
				titletext_hide.removeTextChangedListener(mTitleTextWatcher);
				// 本文入力ビューの取得
				contenttext = (DreamNoteEditText)v.findViewById(R.id.memo_content_land);
				contenttext.removeTextChangedListener(mContentTextWatcher);
				contenttext.addTextChangedListener(mContentTextWatcher);
				contenttext.setDreamNoteEditTextListener(this);
				contenttext.setOnFocusChangeListener(mFocusChangeListener);

				contenttext_hide = (DreamNoteEditText)v.findViewById(R.id.memo_content);
				contenttext_hide.removeTextChangedListener(mContentTextWatcher);
				// タグ入力ビューの取得
		    	tagtext = (TagAutoCompleteTextView)v.findViewById(R.id.MultiAutoCompleteTagText_land);
		    	tagtext.removeTextChangedListener(mTextWatcher);
		    	tagtext.addTextChangedListener(mTextWatcher);
		    	tagtext.setTagAutoCompleteTextViewListener(this);
		    	tagtext.setOnFocusChangeListener(mFocusChangeListener);

		    	tagtext_hide = (TagAutoCompleteTextView)v.findViewById(R.id.MultiAutoCompleteTagText);
		    	tagtext_hide.removeTextChangedListener(mTextWatcher);
				// タグ選択ボタンビューの取得
		    	TagSelButton = (ImageButton)v.findViewById(R.id.TagSelButton_land);
		    	TagSelButton.setOnClickListener(new OnClickListener() {
					@Override
					public void onClick(View view) {
						if(arrTags != null && arrTags.length > 0) {
							showTagSelectorDialog();
						}
					}
		    	});
		    	TagSelButton_hide = (ImageButton)v.findViewById(R.id.TagSelButton);
		    	TagSelButton_hide.setOnClickListener(null);

		    	// タグ読み込み中は無効にしておく
		    	TagSelButton.setEnabled(false);
		    	TagSelButton_hide.setEnabled(false);

		    	titletext.setOnEditorActionListener(mOnTitleEditorActionListener);
		    	tagtext.setOnEditorActionListener(mOnTagEditorActionListener);

			}
			if(mIsTagsLoaded) {
				setupTagCompleteText();
				setButtonEnable();
			}
	        EditArea.setVisibility(editarea_mode ? View.VISIBLE : View.GONE);
	        TagSelButton.requestFocus();
		}

		@Override
		protected void onFocusSpecific() {
			preview_html.setVisibility(View.GONE);
			EditArea.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		}

		@Override
		protected void unFocusSpecific() {
			preview_html.setVisibility(View.VISIBLE);
			changelayout(false);
		}

		@Override
		public void onImeHidden() {
			AppInfo.DebugLog(getSherlockActivity(), "HtmlEditFragment onImeHidden");
			InputMethodManager imm = (InputMethodManager)
					getSherlockActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			if(imm != null) {
				if(preview_html.getVisibility() == View.GONE) {
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
			AppInfo.DebugLog(getSherlockActivity().getApplicationContext(), "onEditTextImeHidden");
			InputMethodManager imm = (InputMethodManager)
					getSherlockActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			if(imm != null) {

				if(preview_html.getVisibility() == View.GONE) {
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
				if(preview_html.getVisibility() == View.GONE) {
					updateActionbar(true);
					unFocusSpecific();
//					titletext.clearFocus();
			        TagSelButton.requestFocus();
					onFocusView = null;
				}
				imm.hideSoftInputFromWindow(titletext.getWindowToken(), 0);
			}
		}

	    protected TextWatcher mTitleTextWatcher = new TextWatcher() {

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				onTitleTextChanged(s);
			}

			@Override
			public void afterTextChanged(Editable arg0) {}
	    };

	    protected void onTitleTextChanged(CharSequence s) {
	    	ssbtitle = (SpannableStringBuilder)titletext.getText();
	    	String title = ssbtitle.toString();
	    	if(titletext_hide != null) {
	    		titletext_hide.setText(title);
	    	}
	    }


	    protected TextWatcher mContentTextWatcher = new TextWatcher() {

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				onContentTextChanged(s);
			}

			@Override
			public void afterTextChanged(Editable arg0) {}
	    };

	    protected void onContentTextChanged(CharSequence s) {
	    	ssbcontent = (SpannableStringBuilder)contenttext.getText();
	    	String content = ssbcontent.toString();
	    	if(contenttext_hide != null) {
	    		contenttext_hide.setText(content);
	    	}
	    }

	    @Override
	    protected void onTagTextChanged(CharSequence s) {
	    	super.onTagTextChanged(s);
	    	ssbtag = (SpannableStringBuilder)tagtext.getText();
	    	String tag = ssbtag.toString();
	    	if(tagtext_hide != null) {
	    		tagtext_hide.setText(tag);
	    	}
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
		}

		private void loadClipHtml() {
			String itempath = item.path;
			// フォルダ名生成
			String clipfilepath = Environment.getExternalStorageDirectory().toString() + "/" + appName + "/.clip/" + itempath;
			if(preview_html != null) {
				//Log.d(Constant.LOG_TAG, "HtmlPreviewFragment setItemData preview WebView exist!");
				// WebViewの初期化
				extendFileUtils.clearfile(getSherlockActivity());
				//Log.d(Constant.LOG_TAG, "HtmlPreviewFragment setItemData clearfile!");
				initializePreviewWebView(preview_html);
				preview_html.setWebViewClient(mHtmlPreviewWebViewClient);

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

		@SuppressWarnings("deprecation")
		@SuppressLint("NewApi")
		private void initializePreviewWebView(WebView view) {
	        // javascript実行設定を取得
//			String strjavascript = PreferencesUtil.getPreferences(this.getSherlockActivity().getApplicationContext(), Constant.PREFS_JAVASCRIPT, "");
//			boolean pref_javascript = strjavascript.equals("") ? false : true;
			view.getSettings().setSupportMultipleWindows(false);
			view.getSettings().setJavaScriptEnabled(false);
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
			view.setDrawingCacheEnabled(true);
			view.setWebViewClient(null);
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
		protected void setupMsg() {
			aborttoastmsg = new String[] {
				getResources().getString(R.string.abort_editclip_toastmsg),
				getResources().getString(R.string.abort_addclip_toastmsg)
			};
			abortalertmsgs = new String[] {
				getResources().getString(R.string.abort_editclip_alertmsg),
				getResources().getString(R.string.abort_addclip_alertmsg)
			};
			modealerttitle = new String[] {
				getResources().getString(R.string.editclip_alerttitle),
				getResources().getString(R.string.addclip_alerttitle)
			};
			alertmsgs = new String[][] {
				{
					getResources().getString(R.string.editclip_alertmsg_home),
					getResources().getString(R.string.editclip_alertmsg_viewer),
					getResources().getString(R.string.editclip_alertmsg_memo),
					getResources().getString(R.string.editclip_alertmsg_photo),
					getResources().getString(R.string.editclip_alertmsg_todo)
				},
				{
					getResources().getString(R.string.addclip_alertmsg_home),
					getResources().getString(R.string.addclip_alertmsg_viewer),
					getResources().getString(R.string.addclip_alertmsg_memo),
					getResources().getString(R.string.addclip_alertmsg_photo),
					getResources().getString(R.string.addclip_alertmsg_todo)
				}
			};
		}

		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			if(id == 0) {
				return super.onCreateLoader(id, args);
			} else if(id == 1) {
				cliptask = new PostClipTask(getSherlockActivity().getApplicationContext(), this, clipmode, preview_html.capturePicture(), favicon);
				return cliptask;
			}
			return null;
		}

		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor result) {
			if(loader == tagsloader) {
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
				setButtonEnable();
				setContentShown(true);
			} else if(loader == cliptask) {
				if(result.moveToFirst()) {
		            preview_html.stopLoading();
		            preview_html.setWebChromeClient(null);
		            preview_html.setWebViewClient(null);
		            preview_html.clearView();
		            preview_html.loadUrl("about:blank");
		        	try {
		    			Class.forName("android.webkit.WebView").getMethod("onPause", (Class[]) null).invoke(preview_html, (Object[]) null);
		    		} catch (IllegalArgumentException e) {
		    			e.printStackTrace();
		    		} catch (SecurityException e) {
		    			e.printStackTrace();
		    		} catch (IllegalAccessException e) {
		    			e.printStackTrace();
		    		} catch (InvocationTargetException e) {
		    			e.printStackTrace();
		    		} catch (NoSuchMethodException e) {
		    			e.printStackTrace();
		    		} catch (ClassNotFoundException e) {
		    			e.printStackTrace();
		    		}
		        	preview_html.setVisibility(View.GONE);
		        	preview_html.clearCache(true);
		        	preview_html.clearFormData();
		        	preview_html.clearHistory();
//		        	preview_html.destroy();
		        	preview_html = null;
					int resultInt = result.getInt(result.getColumnIndexOrThrow("result"));
					if(resultInt == 0) {
						cancelFinish();
					} else {
						Toast.makeText(getSherlockActivity().getApplicationContext(), R.string.clip_service_failed_title, Toast.LENGTH_LONG).show();
					}
				}
			}
		}

		private void cancelFinish() {
    		setResult(RESULT_CANCELED, null);
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
				if((editing_title.equals(item.title) == false) || (editing_content.equals(item.description) == false) || (editing_tags.equals(item.tags) == false)){
					// 元の内容と変わっていた場合
					onchange = true;
				}
    		} else {
    			// 新規登録時は、クリップしているので無条件に onchange = true
    			onchange = true;
    		}
			return onchange;
		}

		private void Aborts() {
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
	    			Toast.makeText(getSherlockActivity().getApplicationContext(), abortmsg, Toast.LENGTH_LONG).show();
	    		}
	    		setResult(RESULT_CANCELED, null);

	    		FragmentTransaction ft = getSherlockActivity().getSupportFragmentManager().beginTransaction();
				ft.remove(this);
				ft.commit();
			}
		}
		protected boolean innerPageLink(String url){
		   String[] u = url.split("/");
		   return u[u.length-1].indexOf("#") >= 0;
		}
		@Override
		public void onAbort() {
    		hideIME();
    		if(preview_html != null) {
    			if(preview_html.canGoBack()) {
    				WebBackForwardList list = preview_html.copyBackForwardList();
    				int ix = -1;
    				String url = null;
    				while(innerPageLink(url=list.getItemAtIndex(list.getCurrentIndex()+ix).getUrl())) ix--;
    				if(url != null) {
    					if(preview_html.canGoBackOrForward(ix)) {
    						preview_html.goBackOrForward(ix);
    					} else {
    						preview_html.goBack();
    					}
    				} else {
    					preview_html.goBack();
    				}
    				return;
    			}
    		}
    		Aborts();
		}

		//----------------------------------------------------------------------

		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			super.onCreateOptionsMenu(menu, inflater);
			AppInfo.DebugLog(getSherlockActivity().getApplicationContext(), "HtmlEditFragment onCreateOptionsMenu");
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
				getSherlockActivity().getSupportActionBar().setTitle(getResources().getString(R.string.editclip_title));
			} else {
				getSherlockActivity().getSupportActionBar().setTitle(getResources().getString(R.string.addclip_title));
			}
		}

	    @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
//	    	AppInfo.DebugLog(getSherlockActivity(), "HtmlEditFragment onOptionsItemSelected");
	    	switch(item.getItemId()) {
	    	case R.id.edit_menu_frag_save:
	    		if(mode) {
		    		hideIME();
		    		ssbtitle = (SpannableStringBuilder) titletext.getText();
		    		ssbcontent = (SpannableStringBuilder)contenttext.getText();
		    		ssbtag = (SpannableStringBuilder)tagtext.getText();
		    		editing_title = ssbtitle.toString();
		    		editing_content = ssbcontent.toString();
		    		editing_tags = ssbtag.toString();
		    		if(editing_title.trim().equals("")) {
		    			editing_title = getResources().getString(R.string.clip_nontitle);
		    		}
		        	this.item.title = editing_title;
		        	this.item.description = editing_content;
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
	    		}
	    		break;
	    	case R.id.edit_menu_frag_savehtml:
	    		if(readclipFlag) {
	    			mIsClipping = true;
		    		hideIME();
					EditArea.setVisibility(View.GONE);
					editarea_mode = false;
					changelayout(true);
					preview_html.invalidate();
/*
					EditArea.setVisibility(View.GONE);
		    		EditArea_hide.setVisibility(View.GONE);
		    		LandArea.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		    		preview_html.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
*/
	    			AppInfo.DebugLog(getSherlockActivity().getApplicationContext(), "on save-buttom preview_content: " + preview_content);
					contentbackup = preview_content;
					preview_content = "";
					interfacename = "HTMLOUT" + java.util.UUID.randomUUID().toString().replace("-", "");
					try {
						preview_html.getSettings().setJavaScriptEnabled(true);
						preview_html.addJavascriptInterface(new contentloaderInterface(), interfacename);
						preview_html.loadUrl("javascript:window." + interfacename + ".loadcontent(''+window.document.documentElement.innerHTML);");
					} catch(NullPointerException ne) {
						preview_html.addJavascriptInterface(new contentloaderInterface(), interfacename);
						preview_html.loadUrl("javascript:window." + interfacename + ".loadcontent(''+window.document.documentElement.innerHTML);");
					}
					clipmode = true;
					getLoaderManager().initLoader(1, null, this);
	    		}
	    		break;
	    	case R.id.edit_menu_frag_savepic:
	    		if(readclipFlag) {
	    			mIsClipping = true;
		    		hideIME();
					EditArea.setVisibility(View.GONE);
					editarea_mode = false;
					changelayout(true);
					preview_html.invalidate();
/*
		    		EditArea.setVisibility(View.GONE);
		    		EditArea_hide.setVisibility(View.GONE);
		    		LandArea.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		    		preview_html.setLayoutParams(new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
*/
					contentbackup = preview_content;
					preview_content = "";
					interfacename = "HTMLOUT" + java.util.UUID.randomUUID().toString().replace("-", "");
					try {
						preview_html.getSettings().setJavaScriptEnabled(true);
						preview_html.addJavascriptInterface(new contentloaderInterface(), interfacename);
						preview_html.loadUrl("javascript:window." + interfacename + ".loadcontent(''+window.document.documentElement.innerHTML);");
					} catch(NullPointerException ne) {
						preview_html.addJavascriptInterface(new contentloaderInterface(), interfacename);
						preview_html.loadUrl("javascript:window." + interfacename + ".loadcontent(''+window.document.documentElement.innerHTML);");
					}
					clipmode = false;
					getLoaderManager().initLoader(1, null, this);
	    		}
	    		break;
	    	case R.id.edit_menu_frag_cancel:
	    		if(mIsClipping == false) {
	    			hideIME();
	    			Aborts();
	    		}
	    		break;
			case R.id.edit_menu_frag_viewswitch:
				if(mIsClipping == false) {
					if(EditArea.getVisibility() == View.VISIBLE) {
						EditArea.setVisibility(View.GONE);
						item.setIcon(invisibleIconId);
						editarea_mode = false;
					} else {
						EditArea.setVisibility(View.VISIBLE);
						item.setIcon(visibleIconId);
						editarea_mode = true;
					}
					changelayout(true);
				}
				break;
	    	}
	    	return true;
	    }
		//----------------------------------------------------------------------

		@Override
		public void onMenuEvent(int menuId) {
			// TODO 自動生成されたメソッド・スタブ

		}
		private Handler handler;
	    private class contentloaderInterface {
	    	@SuppressWarnings("unused")
			public void loadcontent(final String html) {
	    		handler.post(new Runnable() {

					@SuppressLint("NewApi") public void run() {
			    		preview_content = html;
			    		if(Build.VERSION.SDK_INT > 10) {
			    			preview_html.removeJavascriptInterface(interfacename);
			    		} else {
			    			preview_html.addJavascriptInterface(null, interfacename);
			    		}
						readclipFlag = true;
					}

	    		});
	    	}
	    }
	    private class ClipChromeClient extends WebChromeClient {
	    	private Context context;
			private int oldprogress;
			public ClipChromeClient(Context context) {
				oldprogress = 0;
				this.context = context;
			}
			@Override
			public void onProgressChanged(WebView view, int progress) {
				String url = view.getUrl();
				if(url == null) {
					url = "";
				}
				boolean progresspass = url.startsWith("https://mobile.twitter.com/") && (progress > 10);
	    		AppInfo.DebugLog(context, "onProgressChanged: " + url);
	    		AppInfo.DebugLog(context, "onProgressChanged: " + progress);
				if((oldprogress != progress && progress == 100) || progresspass) {
					titletext.setText(view.getTitle());
					view.loadUrl("javascript:window." + interfacename + ".loadcontent(''+window.document.documentElement.innerHTML);");
					view.setWebChromeClient(null);
					//readclipFlag = true;
					CCClient = null;
				}
				oldprogress = progress;
			}
			@Override
			public void onReceivedIcon(WebView view, Bitmap icon) {
	    		if(favicon != null) {
	    			favicon.recycle();
	    			favicon = null;
	    		}
	    		favicon = view.getFavicon();
				super.onReceivedIcon(view, icon);
			}
		}
		private class ClipClient extends WebViewClient {
			private Context context;
			public ClipClient(Context context) {
				this.context = context;
			}
			//ページ読み込み開始
			@SuppressLint("NewApi") @Override
			public void onPageStarted(WebView view, String url, Bitmap favicon) {
				AppInfo.DebugLog(context, "onPageStatred url: " + url);
				if(url.startsWith("https://socialize.gigya.com")) {
					view.stopLoading();
					return;
				}
				if(url.startsWith("file://")) {
					view.stopLoading();
					return;
				}

				if(CCClient == null) {
					CCClient = new ClipChromeClient(context);
					view.setWebChromeClient(CCClient);
				}
				WebBackForwardList list = preview_html.copyBackForwardList();
				String historyurl = null;
				int historysize = list.getSize();
				for(int hi = 0; hi < historysize; hi++) {
					historyurl = list.getItemAtIndex(hi).getUrl();
					if(historyurl.equals(url)) {
						return;
					}
				}
				if(Build.VERSION.SDK_INT > 10) {
					preview_html.removeJavascriptInterface(interfacename);
				} else {
					preview_html.addJavascriptInterface(null, interfacename);
				}
				interfacename = "HTMLOUT" + java.util.UUID.randomUUID().toString().replace("-", "");
				preview_html.addJavascriptInterface(new contentloaderInterface(), interfacename);
				readclipFlag = false;
			}
	    	//ページの読み込み完了
	    	@Override
	    	public void onPageFinished(WebView view, String url) {
	    		if(CCClient == null) {
	    			ssbtitle = (SpannableStringBuilder) titletext.getText();
	    			String tmptitle = ssbtitle.toString().trim();
	    			String overtitle = view.getTitle();
	    			if(StringUtils.isBlank(overtitle) == false) {
	    				if(overtitle.equals(tmptitle) == false) {
	    					titletext.setText(overtitle);
	    					//readclipFlag = true;
	    					view.loadUrl("javascript:window." + interfacename + ".loadcontent(''+window.document.documentElement.innerHTML);");
	    				}
	    			}
	    		}
				requestUrl = url;
	    	}
	    	@Override
	    	public boolean shouldOverrideUrlLoading(WebView view, String url) {
	    		if(url == null) return false;
				AppInfo.DebugLog(context, "shouldOverrideUrlLoading: " + url);
				// YouTube video link
				if (url.startsWith("vnd.youtube:"))
				{
				    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
				    return true;
				}
				Matcher m = Pattern.compile(".?youtube.com\\/.+?watch\\?v=([a-z0-9A-Z]+)", Pattern.CASE_INSENSITIVE).matcher(url);
				if(m.find()) {
					String id = m.group(1);
				    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id)));
				    return true;
				}
/*
				if (url.equals("file:///android_asset/webkit/")) {
					return false;
				}
*/
				if(url.startsWith("file://")) {
					return true;
				}
				if(url.startsWith("content://")) {
					return true;
				}
				if (url.equals("https://mobile.twitter.com/") || url.equals("https://mobile.twitter.com/#!/")) {
					url = "https://mobile.twitter.com/session/new";
				}
				if(preview_content == null) {
					preview_content = "";
				}
				if(requestUrl == null) {
					requestUrl = "";
				}
				if(preview_content.equals("") == false || requestUrl.equals(url) == false) {
					CCClient = new ClipChromeClient(context);
					view.setWebChromeClient(CCClient);
				}
				//view.loadUrl(url);
				return false;
	    	}
	    }
	}

	public static class PostClipTask extends AsyncTaskLoader<Cursor> {
		private HtmlEditFragment fragment;
		private DisplayMetrics metrics;
		private Context context;
		private String appName;
    	private String title = "";
    	private String description = "";
    	private String user_tags = "";
		private String path;
		private File[] cachefiles;
		private ArrayListClipWebcache cachelist = null;
		private Bitmap ClipBodyImage;
		private Bitmap ThumbnailImage;
		private String[] RESULT_CURSOR;
		private String userAgent;
		private Picture thumbpic;
		private Bitmap favicon;
		private boolean clipmode;

		public PostClipTask(Context context, HtmlEditFragment f, boolean clipmode, Picture pic, Bitmap favicon) {
			super(context);
			this.context = context;
			path = "";
			cachefiles = null;
			cachelist = null;
			ClipBodyImage = null;
			ThumbnailImage = null;
			RESULT_CURSOR = new String[] { "result" };
			fragment = f;
			metrics = new DisplayMetrics();
	        WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
	        manager.getDefaultDisplay().getMetrics(metrics);
			// アプリケーション名を取得
	        appName = context.getResources().getString(R.string.app_name);
	        thumbpic = pic;
	        this.favicon = favicon;
	        this.clipmode = clipmode;
		}

		@Override
		protected void onStartLoading() {
			forceLoad();
		}

		@Override
		protected void onStopLoading() {
			cancelLoad();
//			clearClipPath(path);
			if(ClipBodyImage != null) {
				ClipBodyImage.recycle();
				ClipBodyImage = null;
			}
			if(ThumbnailImage != null) {
				ThumbnailImage.recycle();
				ThumbnailImage = null;
			}
		}

		@SuppressLint("NewApi")
		@Override
		public Cursor loadInBackground() {
			int cnt = 0;
			do {
				try {
					Thread.sleep(500);
					cnt++;
				} catch (InterruptedException e) {
					break;
				}
			} while(fragment.preview_content.equals("") && (cnt < 5));
			if(fragment.preview_content.equals("")) {
				fragment.preview_content = fragment.contentbackup;
			}
			AppInfo.DebugLog(context, "preview content: " + fragment.preview_content);
			userAgent = fragment.preview_html.getSettings().getUserAgentString();
			SpannableStringBuilder edittitle = (SpannableStringBuilder) fragment.titletext.getText();
			SpannableStringBuilder editcontent = (SpannableStringBuilder)fragment.contenttext.getText();
			SpannableStringBuilder edittag = (SpannableStringBuilder)fragment.tagtext.getText();
    		title = edittitle.toString();
    		description = editcontent.toString();
    		user_tags = edittag.toString();
    		if(title.trim().equals("")) {
    			title = context.getResources().getString(R.string.clip_nontitle);
    		}
    		user_tags = user_tags.replaceAll("，", ",");
    		user_tags = user_tags.replaceAll(", ", ",");
    		user_tags = user_tags.replaceAll("、", ",");
    		user_tags = user_tags.replaceAll("､", ",");
    		user_tags = user_tags.replaceAll("　", ",");
    		user_tags = user_tags.replaceAll(" ", ",");
    		String[] arr_user_tags = user_tags.split(",");
    		ArrayList<String> memo_tags = new ArrayList<String>();
        	for(String utag: arr_user_tags) {
        		utag = utag.trim();
        		if(utag.equals("") == false) {
        			if(memo_tags.indexOf(utag) == -1) {
        				memo_tags.add(utag);
    				}
        		}
        	}
        	user_tags = memo_tags.toString();
        	user_tags = user_tags.substring(1, user_tags.length() -1);
			// フォルダ作成
			path = createClipPath();
			String clipfilepath = Environment.getExternalStorageDirectory().toString() + "/" + appName + "/.clip/" + path;
			File clippath = new File(clipfilepath);
			clippath.mkdirs();

			Picture picture = thumbpic;
			if(picture.getWidth() != 0 && picture.getHeight() != 0) {
				Canvas c = null;
				if(clipmode == false) {
					AppInfo.DebugLog(context, "pic clip create start");
					int scnt = 0;
    				int savedHeight = 0;
    				int savedWidth = 0;
    				StringBuilder sb = new StringBuilder();
    				sb.append("<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\">");
    				do {
    					int height = Math.min(metrics.heightPixels, picture.getHeight() - savedHeight);
        				savedWidth = 0;
        				sb.append("<tr>");
    					do {
    						int width = Math.min(metrics.widthPixels, picture.getWidth() - savedWidth);
				    		try {
				    			ClipBodyImage = Bitmap.createBitmap( width, height, Bitmap.Config.RGB_565);
					    		c = new Canvas( ClipBodyImage );
					    		c.translate(-savedWidth, -savedHeight);
					    		c.drawPicture(picture);
								saveClipBodyImageNum(clipfilepath, scnt);
								sb.append("<td><img src=\"clipbody" + scnt + ".jpg\" width=\"" + width + "\" height=\"" + height + "\" border=\"0\"></td>");
								scnt++;
				    		} catch(OutOfMemoryError ofme) {
				    			ofme.printStackTrace();
				    			AppInfo.DebugLog(context, "OutOfMemory Error save clip body");
				    		}
				    		savedWidth += width;
    					} while(savedWidth != picture.getWidth());
    					sb.append("</tr>");
    					savedHeight += height;
    				} while(savedHeight != picture.getHeight());
    				sb.append("</table><body><html>");
    				if(metrics.widthPixels >= savedWidth) {
        				sb.insert(0, "<html><head><meta name=\"viewport\" content=\"width=device-width; initial-scale=1.0; maximum-scale=1.0; user-scalable=no;\" /></head><body style=\"margin-top: 0px; margin-left: 0px; margin-right: 0px; margin-bottom: 0px;\">");
    				} else {
        				sb.insert(0, "<html><body style=\"margin-top: 0px; margin-left: 0px; margin-right: 0px; margin-bottom: 0px;\">");
    				}
    				String filename = clipfilepath + "/index.html";
					BufferedWriter bw;
					try {
						bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(filename)), "UTF-8"));
						bw.write(sb.toString());
						bw.flush();
						bw.close();
						AppInfo.DebugLog(context, "content filename: " + filename);
						AppInfo.DebugLog(context, "pic clip content html created: " + sb.toString());
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					} catch (FileNotFoundException e) {
						e.printStackTrace();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
    			try {
    				ThumbnailImage = Bitmap.createBitmap( picture.getWidth(), picture.getWidth(), Bitmap.Config.RGB_565);
		    		c = new Canvas( ThumbnailImage );
		    		c.translate(-fragment.preview_html.getScrollX(), -fragment.preview_html.getScrollY());
		    		c.drawPicture(picture);

					saveThumbnailImage(clipfilepath);
    			} catch(OutOfMemoryError tofme) {
    	    		if(ThumbnailImage != null) {
    	    			ThumbnailImage.recycle();
    	    			ThumbnailImage = null;
    	    		}
    			}

    			// faviconとサムネイルをフォルダに保存
    			saveFaviconImage(clipfilepath);
			}
			MatrixCursor c = new MatrixCursor(RESULT_CURSOR);
    		if(savePreviewContent(clipfilepath + "/.webcache")) {
    			c.addRow(new Object[] { -1 } );
    			clearClipPath(path);
    		} else {
	            if(Build.VERSION.SDK_INT > 10) {
					String archivefilename = clipfilepath + File.separator + "webarchive.xml";
					boolean tmpfileresult;
					String tmpfilename = context.getFilesDir().getAbsolutePath() + File.separator + "tmpfile.html";
					//Log.d("AnotherNote", "tmpfilename: " + tmpfilename);
					File tmpfile = new File(tmpfilename);
					if(tmpfile.exists()) {
						tmpfile.delete();
					}
					fragment.preview_html.saveWebArchive(tmpfilename);
					do {
						tmpfileresult = tmpfile.exists() == false || tmpfile.length() == 0;
					}while(tmpfileresult);
	            	extendFileUtils.copyFile(tmpfilename, archivefilename);
					if(new File(archivefilename).exists() == false) {
						//Log.d("AnotherNote", "archivefilename is not exist");
						c.addRow(new Object[] { -1 } );
						return c;
					}
					File cachedir = new File(clipfilepath + File.separator + ".webcache");
					cachedir.mkdirs();
					cachelist = new ArrayListClipWebcache();
					saveArchive(clipfilepath, "webarchive.xml");
	            } else {
					// WebCacheデータ取得
	            	AppInfo.DebugLog(context, "PostClipTask loadInBackground getArrayListClipWebcache");
					cachelist = getArrayListClipWebcache();
					// キャッシュファイルを全てコピー
					AppInfo.DebugLog(context, "PostClipTask loadInBackground copyCacheFiles");
					copyCacheFiles(clipfilepath + "/.webcache");
	            }
				String strwebpicforce = PreferencesUtil.getPreferences(context, Constant.PREFS_WEBCLIP_PIC_FORCE, "");
				boolean pref_webclipforce = strwebpicforce.equals("") ? false : true;
				Intent intent = new Intent(context, ClipService.class);
				ClipQueueItem cq = new ClipQueueItem();
				cq.setTitle(title);
				cq.setDescription(description);
				cq.setStr_user_tags(user_tags);
				cq.setClipmode(clipmode);
				cq.setForcemode(pref_webclipforce);
				cq.setRequestUrl(fragment.requestUrl);
				cq.setUserAgent(userAgent);
				cq.setClippath(path);
				cq.setArrcache(cachelist);
				intent.putExtra("cq", cq);
				AppInfo.DebugLog(context, "PostClipTask loadInBackground startService");
				context.startService(intent);
    			c.addRow(new Object[] { 0 } );
    		}
        	return c;
		}

		private boolean savePreviewContent(String savepath) {
			boolean result = false;
			String filename = savepath + "/cachecontent.txt";
			File dstFile = new File(filename);
			dstFile.getParentFile().mkdirs();
			BufferedWriter bw;
			try {
				bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(dstFile), "UTF-8"));
				bw.write(fragment.preview_content);
				bw.flush();
				bw.close();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				result = true;
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				result = true;
			} catch (IOException e) {
				e.printStackTrace();
				result = true;
			}
			return result;
		}

		private boolean saveFaviconImage(String savepath) {
			OutputStream outputStream = null;
	    	if(favicon != null) {
	    		String filename = savepath + "/favicon.png";
	    		File faviconfile = new File(filename);
	    		try {
	    			outputStream = new FileOutputStream(faviconfile);
	    			favicon.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
	    		} catch (IOException e) {
	    			return false;
	    		} finally {
	    			if(outputStream != null) {
	    				try {
	    					outputStream.close();
	    					outputStream = null;
	    				} catch(IOException e) {
	    					return false;
	    				}
	    			}
	    		}
	    		favicon.recycle();
	    		favicon = null;
	    		return true;
	    	}
	    	return false;
		}

		private boolean saveThumbnailImage(String savepath) {
			OutputStream outputStream = null;
	    	if(ThumbnailImage != null) {
	        	String filename = savepath + "/thumbnail.png";
	        	File thumbnail = new File(filename);
	        	try {
	        		outputStream = new FileOutputStream(thumbnail);
	        		ThumbnailImage.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
	        	} catch(IOException e) {
	        		return false;
	        	} finally {
	        		if(outputStream != null) {
	        			try {
	        				outputStream.close();
	        				outputStream = null;
	        			} catch(IOException e) {
	        				return false;
	        			}
	        		}
	        	}
	        	ThumbnailImage.recycle();
	        	ThumbnailImage = null;
	        	return true;
	    	}
	    	return false;
		}

		private boolean saveClipBodyImageNum(String savepath, int num) {
			boolean result = true;
			OutputStream outputStream = null;
	    	if(ClipBodyImage != null) {
				if(clipmode == false) {
		        	String filename = savepath + "/clipbody" + num + ".jpg";
		        	File clipbody = new File(filename);
		        	try {
		        		outputStream = new FileOutputStream(clipbody);
		        		ClipBodyImage.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
		        	} catch(IOException e) {
		        		result = false;
		        	} finally {
		        		if(outputStream != null) {
		        			try {
		        				outputStream.close();
		        				outputStream = null;
		        			} catch(IOException e) {
		        				result = false;
		        			}
		        		}
		        	}
				}
	        	ClipBodyImage.recycle();
	        	ClipBodyImage = null;
	    	}
	    	return result;
		}

		private void saveArchive(String dirname, String filename) {

			int resource_cnt = 0;
			try {
				String archiveString = extendFileUtils.readStringFile(dirname + File.separator + filename, "UTF-8");
				//Log.d("AnotherNote", "readed webarchive.xml");
				Matcher m = Pattern.compile("(<mainResource>)(.+?)(</mainResource>)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(archiveString);
				if(m.find()) {
					String mainresourceString = m.group(2);
					Matcher elem = Pattern.compile("(<ArchiveResource>)(.+?)(</ArchiveResource>)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(mainresourceString);
					if(elem.find()) {
						String resourceString = elem.group(2);
						saveArchiveResource(dirname, resourceString, true, resource_cnt);
					}
					archiveString = m.replaceAll("");
				}
				m = Pattern.compile("(<subresources>)(.+?)(</subresources>)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(archiveString);
				if(m.find()) {
					String subresourceString = m.group(2);
					String START_TAG = "<ArchiveResource>";
					String END_TAG = "</ArchiveResource>";
					int startpos = subresourceString.indexOf(START_TAG);
					while(startpos != -1) {
						int endpos = subresourceString.indexOf(END_TAG);
						String resourceString = subresourceString.substring(startpos + START_TAG.length(), endpos);
						resource_cnt = saveArchiveResource(dirname, resourceString, false, resource_cnt);
						subresourceString = subresourceString.substring(endpos + END_TAG.length());
						startpos = subresourceString.indexOf("<ArchiveResource>");
					}
					archiveString = m.replaceAll("");
				}
			} catch(Exception e) {
				e.printStackTrace();
			}
		}

		@SuppressLint("NewApi")
		private int saveArchiveResource(String dirname, String resourceString, boolean indexflg, int resource_cnt) {
			String url;
			String mimeType;
			String encoding;
			String data;
			String decodeStrUrl;
			String decodeStrMimeType;
			String decodeStrEncoding;
			String decodeStrData;
			try {
				url = "";
				mimeType = "";
				encoding = "";
				data = "";
				decodeStrUrl = "";
				decodeStrMimeType = "";
				decodeStrEncoding = "";
				decodeStrData = "";
				Matcher melem = Pattern.compile("(<url>)(.+?)(</url>)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(resourceString);
				if(melem.find()) {
					//Log.d("AnotherNote", "url match");
					url = melem.group(2);
					resourceString = melem.replaceAll("");
				}
				melem = Pattern.compile("(<mimeType>)(.+?)(</mimeType>)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(resourceString);
				if(melem.find()) {
					//Log.d("AnotherNote", "mimeType match");
					mimeType = melem.group(2);
					resourceString = melem.replaceAll("");
				}
				melem = Pattern.compile("(<textEncoding>)(.+?)(</textEncoding>)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(resourceString);
				if(melem.find()) {
					//Log.d("AnotherNote", "textEncoding match");
					encoding = melem.group(2);
					resourceString = melem.replaceAll("");
				}
				melem = Pattern.compile("(<data>)(.+?)(</data>)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(resourceString);
				if(melem.find()) {
					//Log.d("AnotherNote", "data match");
					data = melem.group(2);
					resourceString = melem.replaceAll("");
				}

				byte[] byteUrl = android.util.Base64.decode(url, android.util.Base64.DEFAULT);
				decodeStrUrl = new String(byteUrl);
				byteUrl = null;
				byte[] byteMimeType = android.util.Base64.decode(mimeType, android.util.Base64.DEFAULT);
				decodeStrMimeType = new String(byteMimeType);
				byteMimeType = null;
				byte[] byteEncoding = android.util.Base64.decode(encoding, android.util.Base64.DEFAULT);
				decodeStrEncoding = new String(byteEncoding);
				byteEncoding = null;
				byte[] byteData = android.util.Base64.decode(data, android.util.Base64.DEFAULT);
				if(indexflg && decodeStrMimeType.equalsIgnoreCase("text/html")) {
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					out.write(byteData);
					String indexfilename = dirname + File.separator + ".webcache" + File.separator + "index.html";
					File tmpfile = new File(indexfilename);
					OutputStream tmpoutput = new FileOutputStream(tmpfile);
		    		tmpoutput.write(out.toByteArray(), 0, out.size());
		    		tmpoutput.flush();
		    		tmpoutput.close();
		    		tmpoutput = null;
		    		out.close();
		    		out = null;
		    		ClipWebcache cache = new ClipWebcache();
		    		cache.setEncoding(decodeStrEncoding);
		    		cache.setMimetype(decodeStrMimeType);
		    		cache.setUrl(decodeStrUrl);
		    		cache.setFilepath("index.html");
		    		cachelist.add(cache);
				} else {
					String wPath = decodeStrMimeType.substring(decodeStrMimeType.lastIndexOf("/") + 1);
					String resourcename = String.format("%04d.%s", resource_cnt, wPath);
					resource_cnt++;
					ByteArrayOutputStream out = new ByteArrayOutputStream();
					out.write(byteData);
					String filename = dirname + File.separator + ".webcache" + File.separator + resourcename;
					File tmpfile = new File(filename);
					OutputStream tmpoutput = new FileOutputStream(tmpfile);
		    		tmpoutput.write(out.toByteArray(), 0, out.size());
		    		tmpoutput.flush();
		    		tmpoutput.close();
		    		tmpoutput = null;
		    		out.close();
		    		out = null;
		    		ClipWebcache cache = new ClipWebcache();
		    		cache.setEncoding(decodeStrEncoding);
		    		cache.setMimetype(decodeStrMimeType);
		    		cache.setUrl(decodeStrUrl);
		    		cache.setFilepath(resourcename);
		    		cachelist.add(cache);
		    		//Log.d("AnotherNote", "saved resource file name: " + filename);
				}
				byteData = null;
			} catch(PatternSyntaxException e) {
				e.printStackTrace();
			} catch(IllegalArgumentException e) {
				e.printStackTrace();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
			return resource_cnt;
		}

		private ArrayListClipWebcache getArrayListClipWebcache() {
			ArrayListClipWebcache arrcache = new ArrayListClipWebcache();
			SQLiteDatabase db = null;
			do {
				try {
					db = context.openOrCreateDatabase("webviewCache.db", 0, null);
				} catch(SQLiteException e) {
					db = null;
				}
			} while(db == null);
			if(db != null) {
				String sql = "SELECT url, filepath, mimetype, encoding FROM cache";
				Cursor cursor = db.rawQuery(sql, null);
				if(cursor != null) {
					while(cursor.moveToNext()) {
						ClipWebcache cache = new ClipWebcache();
						cache.setUrl(cursor.getString(0));
						cache.setFilepath(cursor.getString(1));
						cache.setMimetype(cursor.getString(2));
						cache.setEncoding(cursor.getString(3));
						arrcache.add(cache);
					}
					cursor.close();
				}
				db.close();
			}
			return arrcache;
		}

		private void copyCacheFiles(String copypath) {
		    File basedir = CacheManager.getCacheFileBaseDir();
		    if(basedir == null) return;
		    cachefiles = basedir.listFiles();
			for(File cache : cachefiles) {
				extendFileUtils.copyFile(cache.getPath(), copypath + "/" + cache.getName());
			}
		}

		private String createClipPath() {
			// フォルダ名生成
			String path = DateFormat.format("yyyyMMddkkmmss", System.currentTimeMillis()).toString();
			String clipfilepath = Environment.getExternalStorageDirectory().toString() + "/" + appName + "/.clip/" + path;
	        try {
	        	// ディレクトリがなければ作成
	        	File dir = new File(clipfilepath);
	        	if(!dir.exists()) {
	        		dir.mkdirs();
	        	}
	        } catch(SecurityException e) {
	        	return null;
	        }
	        return path;
		}

		private void clearClipPath(String path) {
			String clipfilepath = Environment.getExternalStorageDirectory().toString() + "/" + appName + "/.clip/" + path;
			File clipdir = new File(clipfilepath);
			if(clipdir.exists()) {
				File[] clipfiles = clipdir.listFiles();
				if(clipfiles.length > 0) {
					for(File clipfile : clipfiles) {
						if(clipfile.isFile()) {
							clipfile.delete();
						}
					}
				}
				clipdir.delete();
			}
		}

	}

	private boolean isW540dp() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(metrics);
        AppInfo.DebugLog(this.getApplicationContext(), "width:" + metrics.widthPixels);
		if(metrics.widthPixels >= 540) {
			return true;
		} else {
			return false;
		}
	}

	@Override
    public boolean onCreateOptionsMenu(Menu menu) {
		Configuration config = getResources().getConfiguration();
		if(config.orientation == Configuration.ORIENTATION_PORTRAIT) {
			if(isW540dp() && Locale.getDefault().equals(Locale.JAPAN)) {
				getSupportMenuInflater().inflate(R.menu.edit_html_frag_menu, menu);
			} else {
				getSupportMenuInflater().inflate(R.menu.edit_html_frag_menu_hvga, menu);
			}
		} else if(config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
			getSupportMenuInflater().inflate(R.menu.edit_html_frag_menu_land, menu);
		}
    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
		return super.onOptionsItemSelected(item);
    }

	@Override
    public boolean dispatchKeyEvent(KeyEvent event){
		int keycode = event.getKeyCode();
    	AppInfo.DebugLog(this.getApplicationContext(), "HtmlEditFragment dispatchKeyEvent keyCode:" + keycode);
		HtmlEditFragment f = (HtmlEditFragment) getSupportFragmentManager().findFragmentById(android.R.id.content);
		if(keycode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN){
			if(f != null) {
				f.setOnFragmentControlListener(0, this);
				f.onAbort();
				return true;
			}
		} else if(keycode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
			boolean result = false;
			AppInfo.DebugLog(getApplicationContext(), "HtmlEditFragment dispatchKeyEvent Enter");
			if(f != null) {
				 result = f.onDispatchKeyDown(keycode, event);
			}
			if(result) {
				return super.dispatchKeyEvent(event);
			}
			return false;
		}
		return super.dispatchKeyEvent(event);
	}

	@Override
	public void onAddNoteFragment(OnFragmentControlListener listener,
			Fragment fragment, int fragment_type, String tag) {
		// TODO 自動生成されたメソッド・スタブ

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
