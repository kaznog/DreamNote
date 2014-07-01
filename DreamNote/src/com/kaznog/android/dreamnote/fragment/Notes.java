package com.kaznog.android.dreamnote.fragment;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.internal.view.menu.MenuBuilder;
import com.actionbarsherlock.view.ActionMode;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.evernote.client.conn.ApplicationInfo;
import com.evernote.client.oauth.android.AuthenticationResult;
import com.evernote.client.oauth.android.EvernoteSession;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.listener.DeleteItemDialogResultListener;
import com.kaznog.android.dreamnote.listener.OnFragmentControlListener;
import com.kaznog.android.dreamnote.listener.OnPageScrollStateChangedListener;
import com.kaznog.android.dreamnote.listener.TagListItemListener;
import com.kaznog.android.dreamnote.listener.TagLoaderListener;
import com.kaznog.android.dreamnote.listener.onCreateLoaderListener;
import com.kaznog.android.dreamnote.listener.ViewPagerVisibilityListener;
import com.kaznog.android.dreamnote.loader.LoaderCursorSupport;
import com.kaznog.android.dreamnote.loader.LoaderCursorSupport.CursorLoaderListFragment;
import com.kaznog.android.dreamnote.loader.LoaderCursorSupport.CursorLoaderListFragment.onNoteListEventCallbackListener;
import com.kaznog.android.dreamnote.settings.PreferencesUtil;
import com.kaznog.android.dreamnote.util.AppInfo;
import com.kaznog.android.dreamnote.util.Constant;
import com.kaznog.android.dreamnote.util.DreamImageCache;
import com.kaznog.android.dreamnote.util.StringUtils;
import com.kaznog.android.dreamnote.view.NoteViewPager;
import com.kaznog.android.dreamnote.widget.DreamTabHost;
import com.kaznog.android.dreamnote.widget.SearchView;
import com.kaznog.android.dreamnote.widget.TagSelectorView;
import com.kaznog.android.dreamnote.db.DreamNoteProvider;
import com.kaznog.android.dreamnote.db.schema.ItemsSchema;
import com.kaznog.android.dreamnote.dialogfragment.DeleteItemDialogFragment;
import com.kaznog.android.dreamnote.evernote.DreamPostEnmlService;
import com.kaznog.android.dreamnote.fragment.DeleteItemTaskFragmentActivity.DeleteItemTaskFragment;
import com.kaznog.android.dreamnote.fragment.HtmlPreviewFragmentActivity.HtmlPreviewFragment;
import com.kaznog.android.dreamnote.fragment.HtmlEditFragmentActivity.HtmlEditFragment;
import com.kaznog.android.dreamnote.fragment.MemoEditFragmentActivity.MemoEditFragment;
import com.kaznog.android.dreamnote.fragment.MemoPreviewFragmentActivity.MemoPreviewFragment;
import com.kaznog.android.dreamnote.fragment.PhotoPreviewFragmentActivity.PhotoPreviewFragment;
import com.kaznog.android.dreamnote.fragment.PhotoEditFragmentActivity.PhotoEditFragment;
import com.kaznog.android.dreamnote.fragment.PreferenceFragment;
import com.kaznog.android.dreamnote.fragment.TagListFragment;
import com.kaznog.android.dreamnote.fragment.TagLoaderFragment;
import com.kaznog.android.dreamnote.fragment.ToDoEditFragmentActivity.ToDoEditFragment;
import com.kaznog.android.dreamnote.fragment.ToDoNewPreviewFragmentActivity.ToDoNewPreviewFragment;
import com.kaznog.android.dreamnote.fragment.ToDoPreviewFragmentActivity.ToDoPreviewFragment;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TabWidget;
import android.widget.TextView;
import android.widget.Toast;

public class Notes extends SherlockFragmentActivity
implements
OnPageScrollStateChangedListener,
onCreateLoaderListener,
SearchView.OnQueryTextListener,
SearchView.OnSearchViewCloseListener,
TagListItemListener,
TagLoaderListener,
onNoteListEventCallbackListener,
OnFragmentControlListener, DeleteItemDialogResultListener {
	public static DreamImageCache ic = new DreamImageCache();
	public WebView convertview;
	private DisplayMetrics metrics;
	private boolean search_mode = false;
	private boolean addnote_mode = false;
	private boolean listsort_mode = false;
	private String strQuery = "";
	private String[] arrTags = null;
	private Menu currentMenu;
	private MenuItem addnote;
	private SubMenu addnote_submenu;
	private SearchView searchview;
	private MenuItem SearchViewMenuItem;
	private MenuItem ListSortMenuItem;
	private SubMenu ListSortSubMenu;
	static MenuBuilder mTagListMenu;
	private MenuItem TagSelectorMenuItem;
	private TagSelectorView TagSelectorView;
	private ArrayListItem SelectedItems;
	private MenuItem ActionModeMenuItemShortCut;
	private MenuItem ActionModeMenuItemComplete;
	private MenuItem ActionModeMenuItemToInComplete;
	private MenuItem ActionModeMenuItemRefreshThumbnail;
	private MenuItem ActionModeMenuItemEvernote;
	private EvernoteSession session;

	private EditFragment editFragment;
	private TagLoaderFragment TagLoader;
	private HomeMenuListFragment hmFragment;
	private TabsPager mTabsPager;
	private static final int TABSPAGER_FRAGMENT = 0;
	public static final int HOME_FRAGMENT = 1;
	public static final int TAGLIST_FRAGMENT = 2;
	private static final int PREVIEW_MEMO_FRAGMENT = 3;
	private static final int PREVIEW_PHOTO_FRAGMENT = 4;
	private static final int PREVIEW_TODO_FRAGMENT = 5;
	private static final int PREVIEW_TODONEW_FRAGMENT = 6;
	private static final int PREVIEW_HTML_FRAGMENT = 7;
	public static final int EDIT_MEMO_FRAGMENT = 8;
	public static final int EDIT_PHOTO_FRAGMENT = 9;
	public static final int EDIT_TODO_FRAGMENT = 10;
	private static final int EDIT_TODONEW_FRAGMENT = 11;
	public static final int EDIT_HTML_FRAGMENT = 12;
	public static final int PREFERENCE_FRAGMENT = 13;
	public static final int HELP_FRAGMENT = 14;
	public static final int ABOUT_FRAGMENT = 15;
	private ArrayList<Integer> activetitles_fragment = null;
	private ArrayList<Integer> activedetails_fragment = null;
	private Item shownote_item = null;
	private int shownote_datatype = -1;
	private static ArrayList<Integer> sorttype = new ArrayList<Integer>();
	static {
		sorttype.add(R.id.action_menu_listsort_create_asc);
		sorttype.add(R.id.action_menu_listsort_create_desc);
		sorttype.add(R.id.action_menu_listsort_date_asc);
		sorttype.add(R.id.action_menu_listsort_date_desc);
		sorttype.add(R.id.action_menu_listsort_title_asc);
		sorttype.add(R.id.action_menu_listsort_title_desc);
		sorttype.add(R.id.action_menu_listsort_todo);
	};
	private static ArrayList<String> sortorder_param = new ArrayList<String>();
	static {
		sortorder_param.add(DreamNoteProvider.ORDER_CREATE_ASC);
		sortorder_param.add(DreamNoteProvider.ORDER_CREATE_DESC);
		sortorder_param.add(DreamNoteProvider.ORDER_DATE_ASC);
		sortorder_param.add(DreamNoteProvider.ORDER_DATE_DESC);
		sortorder_param.add(DreamNoteProvider.ORDER_TITLE_ASC);
		sortorder_param.add(DreamNoteProvider.ORDER_TITLE_DESC);
		sortorder_param.add(DreamNoteProvider.ORDER_TODO_DEFAULT);
	};
	private static int[] sortorder = {
			3,
			3,
			3,
			6,
			3
	};
	private static final int TAB_INDEX_ALL = 0;
	private static final int TAB_INDEX_MEMO = 1;
	private static final int TAB_INDEX_PHOTO = 2;
	private static final int TAB_INDEX_TODO = 3;
	private static final int TAB_INDEX_HTML = 4;
	private int currentpager_position = TAB_INDEX_ALL;
    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
	@Override
    protected void onCreate(Bundle savedInstanceState) {
		setTheme(R.style.Theme_SherlockCustom);
    	super.onCreate(savedInstanceState);
		if(Build.VERSION.SDK_INT < 8) {
			//回転できないようにする
			setFixedOrientation(true);
		}
		getSherlock().setUiOptions(ActivityInfo.UIOPTION_SPLIT_ACTION_BAR_WHEN_NARROW);

    	if(isTablet()) {
    		setContentView(R.layout.tablet_notes);
    	} else {
    		setContentView(R.layout.notes);
    	}
    	// ソフトウェアキーボードを初期表示状態で表示しないようにする
    	this.getWindow().setSoftInputMode(LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    	// スリープ状態にしないようにする
    	this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	// EvernoteSessionEx生成
    	setupSession(getApplicationContext());

    	if(savedInstanceState == null) {
	    	currentMenu = null;
    		TagSelectorView = null;
    		SelectedItems = null;
    		hmFragment = null;
        	if(activetitles_fragment == null) {
        		activetitles_fragment = new ArrayList<Integer>();
        	}
    		activetitles_fragment.add(TABSPAGER_FRAGMENT);
    		if(activedetails_fragment == null) {
    			activedetails_fragment = new ArrayList<Integer>();
    		}
		} else {
			//Log.d(Constant.LOG_TAG, "Notes onCreate get activetitles_fragment");
			activetitles_fragment = savedInstanceState.getIntegerArrayList("titles_fragment");
			activedetails_fragment = savedInstanceState.getIntegerArrayList("activedetails_fragment");
		}
    	TagLoader = new TagLoaderFragment();
    	TagLoader.registerFragment(this, TagLoaderFragment.TAG);
    	TagLoader.setTagLoaderListener(this);

		final ActionBar ab = getSupportActionBar();
		ab.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
		//アクションバーのアイコンを設定します
		ab.setIcon(R.drawable.actionbar_icon);
		// アクションバーのアイコンにロゴを使用しない
		ab.setDisplayUseLogoEnabled(false);
		// アクションバーのアイコンを有効化しておきます
		ab.setDisplayHomeAsUpEnabled(true);
		// アイテムリストページャーフラグメントの作成
		mTabsPager = TabsPager.newInstance(currentpager_position);
		FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
		ft.add(R.id.titles, mTabsPager, "tabspager");
		ft.commit();

        metrics = new DisplayMetrics();
        WindowManager manager = (WindowManager) this.getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(metrics);
        try {
	        CookieSyncManager.createInstance(getApplicationContext());
			CookieManager cookieManager = CookieManager.getInstance();
	    	cookieManager.removeAllCookie();
	    	cookieManager.acceptCookie();
        } catch(IllegalStateException e) {
        } catch(Exception e) {
        }
        convertview = new WebView(this);
        convertview.setVisibility(View.INVISIBLE);
        convertview.getSettings().setJavaScriptEnabled(false);
        convertview.getSettings().setJavaScriptCanOpenWindowsAutomatically(false);
        convertview.getSettings().setAllowFileAccess(false);
        convertview.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
			convertview.getSettings().setPluginState(WebSettings.PluginState.ON);
		} else {
			convertview.getSettings().setPluginsEnabled(true);
		}
        convertview.getSettings().setSupportMultipleWindows(false);
        convertview.getSettings().setNeedInitialFocus(false);
//        convertview.getSettings().setUseDoubleTree(false);
        convertview.getSettings().setUseWideViewPort(true);
        convertview.getSettings().setLoadWithOverviewMode(true);
        convertview.getSettings().setBuiltInZoomControls(false);
        convertview.getSettings().setLayoutAlgorithm(LayoutAlgorithm.NORMAL);
//        convertview.getSettings().setUserAgent(0);
        convertview.getSettings().setUserAgentString(null);
        convertview.clearView();
        convertview.loadUrl("about:blank");
        this.addContentView(convertview, new LayoutParams(metrics.widthPixels, metrics.widthPixels));
        // サムネール用キャッシュ作成
        if(ic == null) {
        	ic = new DreamImageCache();
        }
        // 起動時には、キャッシュフォルダのファイルを削除する
        ic.initialize(this, metrics, savedInstanceState == null);
		AppInfo.DebugLog(getApplicationContext(), "Notes onCreate");
        Intent i = getIntent();
        if(i != null) {
        	String itemid = i.getStringExtra("itemid");
        	if(itemid != null) {
        		Uri itemuri = ContentUris.withAppendedId(DreamNoteProvider.ITEMS_CONTENT_URI, Long.parseLong(itemid));
        		Cursor c = this.getContentResolver().query(itemuri, null, null, null, null);
        		if(c.moveToFirst()) {
					Item item = new Item();
					item.id = c.getLong(0);
					item.datatype = c.getInt(c.getColumnIndexOrThrow(ItemsSchema.DATATYPE));
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
					onNoteSelected(item.datatype, item);
        		}
        	}
        }
    }

    @Override
    protected void onDestroy() {
		if(convertview != null) {
			convertview.addJavascriptInterface(null, "HTMLOUT");
			convertview.setWebChromeClient(null);
			convertview.setWebViewClient(null);
			convertview.clearView();
			convertview.loadUrl("about:blank");
			convertview.clearCache(true);
			convertview.clearFormData();
			convertview.clearHistory();
//			convertview.destroy();
			convertview = null;
		}
    	try {
	    	CookieManager cookieManager = CookieManager.getInstance();
	    	cookieManager.removeAllCookie();
	    	cookieManager = null;
    	} catch(IllegalStateException e) {
    	} catch(Exception e) {
    	}
    	// スリープできるようにする
    	this.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    	super.onDestroy();
    }

    @Override
    protected void onStart() {
    	super.onStart();
    }

    @SuppressLint("NewApi")
    @Override
    protected void onResume() {
    	//Log.d(Constant.LOG_TAG, "onResume Notes Activity");
		if(Build.VERSION.SDK_INT > 10) {
			if(convertview != null) {
				convertview.onResume();
			}
		}
		CookieSyncManager.getInstance().stopSync();
    	super.onResume();
		if(session != null) {
			session.completeAuthentication(PreferencesUtil.getSharedPreferences(getApplicationContext()));
		}

		TabsPager pager = (TabsPager) getSupportFragmentManager().findFragmentByTag("tabspager");
		if(pager != null) {
			pager.setCurrentPosition(currentpager_position);
		}
		int currenttitles = activetitles_fragment.get(activetitles_fragment.size() - 1);
		switch(currenttitles) {
		case TAGLIST_FRAGMENT:
			break;
		case EDIT_MEMO_FRAGMENT:
		case EDIT_PHOTO_FRAGMENT:
		case EDIT_TODO_FRAGMENT:
		case EDIT_TODONEW_FRAGMENT:
		case EDIT_HTML_FRAGMENT:
			if(editFragment != null) {
				reAttachActivity(editFragment);
			}
		}
		if(isTabletMode()) {
		} else {
		}
    }

    @SuppressLint("NewApi")
    @Override
    public void onPause() {
    	super.onPause();
		CookieSyncManager.getInstance().sync();
		if(Build.VERSION.SDK_INT > 10) {
			if(convertview != null) {
				convertview.onPause();
			}
		}
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
    	//Log.d(Constant.LOG_TAG, "Notes onSaveInstanceState");
    	outState.putIntegerArrayList("titles_fragment", activetitles_fragment);
    	outState.putIntegerArrayList("activedetails_fragment", activedetails_fragment);
    	outState.putString("query", strQuery);
    	outState.putStringArray("tags", arrTags);
    	outState.putInt("currentpager_position", currentpager_position);
    	outState.putSerializable("shownote_item", shownote_item);
    	outState.putInt("shownote_datatype", shownote_datatype);
    	outState.putSerializable("SelectedItems", SelectedItems);
    	super.onSaveInstanceState(outState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
    	//Log.d(Constant.LOG_TAG, "Notes onRestoreInstanceState");
    	activetitles_fragment = savedInstanceState.getIntegerArrayList("titles_fragment");
    	activedetails_fragment = savedInstanceState.getIntegerArrayList("activedetails_fragment");
    	strQuery = savedInstanceState.getString("query");
    	arrTags = savedInstanceState.getStringArray("tags");
    	currentpager_position = savedInstanceState.getInt("currentpager_position");
    	shownote_item = (Item) savedInstanceState.getSerializable("shownote_item");
    	shownote_datatype = savedInstanceState.getInt("shownote_datatype");
    	try {
    		SelectedItems = (ArrayListItem) savedInstanceState.getSerializable("SelectedItems");
    	} catch(ClassCastException e) {
    		SelectedItems = null;
    	}
    }

    @Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

    public void setFixedOrientation(Boolean flg){
        if(flg){
            //this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
            Configuration config = getResources().getConfiguration();
            if(config.orientation == Configuration.ORIENTATION_LANDSCAPE){
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }else if (config.orientation == Configuration.ORIENTATION_PORTRAIT){
                this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        }else{
            //this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

	private void setupSession(Context context) {
        // アプリケーション名を取得
        String appName = context.getResources().getString(R.string.app_name);
        String appVersion = AppInfo.getVersionString(context);
	    ApplicationInfo info =
	    	      new ApplicationInfo(Constant.EN_OAUTH_CONSUMER_KEY, Constant.EN_OAUTH_CONSUMER_SECRET, Constant.EVERNOTE_HOST,
	    	          appName, appVersion);
	    String strAuthToken = PreferencesUtil.getPreferences(context, Constant.PREFS_EN_OAUTH_TOKEN, "");
	    String strNoteStoreUrl = PreferencesUtil.getPreferences(context, Constant.PREFS_EN_OAUTH_NOTE_STORE_URL, "");
	    String strWebApiUrlPrefix = PreferencesUtil.getPreferences(context, Constant.PREFS_EN_OAUTH_WEB_API_URL_PREFIX, "");
	    int UserId = PreferencesUtil.getPreferences(context, Constant.PREFS_EN_OAUTH_ID, 0);
	    if (strAuthToken.length() > 0 && strNoteStoreUrl.length() > 0 && strWebApiUrlPrefix.length() > 0 && (UserId > 0)) {
			PreferencesUtil.setPreferences(context, Constant.KEY_AUTHTOKEN, strAuthToken);
			PreferencesUtil.setPreferences(context, Constant.KEY_NOTESTOREURL, strNoteStoreUrl);
			PreferencesUtil.setPreferences(context, Constant.KEY_WEBAPIURLPREFIX, strWebApiUrlPrefix);
			PreferencesUtil.setPreferences(context, Constant.KEY_USERID, UserId);
	    }
	    if(strAuthToken.equals("") == false) {
	    	//AuthenticationResult result = new AuthenticationResult(strAuthToken, strNoteStoreUrl, strWebApiUrlPrefix, UserId);
	    	session = new EvernoteSession(info, PreferencesUtil.getSharedPreferences(this.getApplicationContext()), getTempDir(context));
	    } else {
	    	session = new EvernoteSession(info, getTempDir(context));
	    }
	}

	private File getTempDir(Context context) {
		return new File(AppInfo.getAppPath(context) + "/.cache");
	}

	private boolean isW360dp() {
        DisplayMetrics metrics = new DisplayMetrics();
        WindowManager manager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        manager.getDefaultDisplay().getMetrics(metrics);
		if(metrics.widthPixels > 360) {
			return true;
		} else {
			return false;
		}
//		return getResources().getBoolean(R.bool.isOverQvga);
	}

    private boolean isTablet() {
    	//return getResources().getBoolean(R.bool.is_tablet);
    	return false;
    }

    private boolean isTabletMode() {
    	/*
    	View v = findViewById(R.id.details);
    	if(v != null) return true;
    	*/
    	return false;
    }

    private boolean isSplitActionbarIsNarrow() {
    	return getResources().getBoolean(R.bool.split_action_bar_is_narrow);
    }

	@Override
	public void onPageScrollChanged(int position, boolean visibility) {
		final Fragment fragment = getFragmentAt(position);
		if(fragment instanceof ViewPagerVisibilityListener) {
			((ViewPagerVisibilityListener)fragment).onVisibilityChanged(visibility);

			String query = ((LoaderCursorSupport.CursorLoaderListFragment)fragment).getQueryText();
			String[] tags = ((LoaderCursorSupport.CursorLoaderListFragment)fragment).getTagsArray();
			boolean tagsres = false;
			if(arrTags != null) {
				if(tags == null) {
					tagsres = true;
				} else if(arrTags.length != tags.length) {
					tagsres = true;
				} else {
					ArrayList<String> flist = new ArrayList<String>(Arrays.asList(tags));
					int i = 0;
					while(i < arrTags.length) {
						if(flist.indexOf(arrTags[i]) == -1) {
							break;
						}
						i++;
					}
					if(i != arrTags.length) {
						tagsres = true;
					}
					flist.clear();
					flist = null;
				}
			} else if(tags != null) {
				tagsres = true;
			}
			if(strQuery == null) strQuery = "";
			if(query == null) query = "";
			if(strQuery.equals(query) == false || tagsres) {
				((LoaderCursorSupport.CursorLoaderListFragment)fragment).setForceQuery(strQuery, arrTags, sortorder_param.get(sortorder[position]));
			}
			if(visibility) {
				if(ListSortSubMenu != null) {
					MenuItem item = ListSortSubMenu.findItem(R.id.action_menu_listsort_todo);
					if(position == TAB_INDEX_TODO) {
						item.setVisible(true);
					} else {
						item.setVisible(false);
					}
					MenuItem currentsort = ListSortSubMenu.findItem(sorttype.get(sortorder[position]));
					currentsort.setChecked(true);
				}
				currentpager_position = position;
			}
		}
	}


	@Override
	public void onCreateLoader(int position, CursorLoaderListFragment fragment) {
		switch(position) {
		case TAB_INDEX_ALL:
			//Log.d(Constant.LOG_TAG, "onCreateLoader ALL");
			ItemAllFragment = fragment;
			break;
		case TAB_INDEX_MEMO:
			//Log.d(Constant.LOG_TAG, "onCreateLoader MEMO");
			ItemMemoFragment = fragment;
			break;
		case TAB_INDEX_PHOTO:
			//Log.d(Constant.LOG_TAG, "onCreateLoader PHOTO");
			ItemPhotoFragment = fragment;
			break;
		case TAB_INDEX_TODO:
			//Log.d(Constant.LOG_TAG, "onCreateLoader TODO");
			ItemToDoFragment = fragment;
			break;
		case TAB_INDEX_HTML:
			//Log.d(Constant.LOG_TAG, "onCreateLoader HTML");
			ItemHtmlFragment = fragment;
			break;
		}

	}
    private static LoaderCursorSupport.CursorLoaderListFragment ItemAllFragment;
    private static LoaderCursorSupport.CursorLoaderListFragment ItemMemoFragment;
    private static LoaderCursorSupport.CursorLoaderListFragment ItemPhotoFragment;
    private static LoaderCursorSupport.CursorLoaderListFragment ItemToDoFragment;
    private static LoaderCursorSupport.CursorLoaderListFragment ItemHtmlFragment;
    private static Fragment getFragmentAt(int position) {
    	switch(position) {
    	case TAB_INDEX_ALL:
    		return ItemAllFragment;
    	case TAB_INDEX_MEMO:
    		return ItemMemoFragment;
    	case TAB_INDEX_PHOTO:
    		return ItemPhotoFragment;
    	case TAB_INDEX_TODO:
    		return ItemToDoFragment;
    	case TAB_INDEX_HTML:
    		return ItemHtmlFragment;
    	default:
    		throw new IllegalStateException("Unknown Fragment index: " + position);
    	}
    }
    @Override
    public void onAttachFragment(Fragment fragment) {
    	int request = 0;
    	if(fragment instanceof LoaderCursorSupport.CursorLoaderListFragment) {
    		Bundle args = fragment.getArguments();
    		if(args != null) {
//    			String category = args.getString("category");
    			//Log.d(Constant.LOG_TAG, "onAttachFragment category: " + category);
//            	String tag = fragment.getTag();
//            	int fragid = fragment.getId();
            	//Log.d(Constant.LOG_TAG, "onAttachFragment tag: " + tag);
            	//Log.d(Constant.LOG_TAG, "onAttachFragment id: " + fragid);
    			int position = args.getInt("position");
                switch(position) {
                case TAB_INDEX_ALL:
                	ItemAllFragment = (CursorLoaderListFragment) fragment;
                	break;
                case TAB_INDEX_MEMO:
                	ItemMemoFragment = (CursorLoaderListFragment) fragment;
                	break;
                case TAB_INDEX_PHOTO:
                	ItemPhotoFragment = (CursorLoaderListFragment) fragment;
                	break;
                case TAB_INDEX_TODO:
                	ItemToDoFragment = (CursorLoaderListFragment) fragment;
                	break;
                case TAB_INDEX_HTML:
                	ItemHtmlFragment = (CursorLoaderListFragment) fragment;
                	break;
                }
     		} else {
     			//Log.d(Constant.LOG_TAG, "onAttachFragment not args(T T)");
     		}
    		((LoaderCursorSupport.CursorLoaderListFragment)fragment).setOnCreateLoaderListener(this);
    	} else if(fragment instanceof TagListFragment) {
    		Fragment f = getSupportFragmentManager().findFragmentByTag(HomeMenuListFragment.TAG);
    		if(f != null) {
    			((TagListFragment)fragment).setOnFragmentControlListener(TAGLIST_FRAGMENT, (OnFragmentControlListener)f);
    		}
    	} else if(fragment instanceof PreferenceFragment) {
    		Fragment f = getSupportFragmentManager().findFragmentByTag(HomeMenuListFragment.TAG);
    		if(f != null) {
    			((PreferenceFragment)fragment).setOnFragmentControlListener(PREFERENCE_FRAGMENT, (OnFragmentControlListener)f);
    		}
    	} else if(fragment instanceof HelpFragment) {
    		Fragment f = getSupportFragmentManager().findFragmentByTag(HomeMenuListFragment.TAG);
    		if(f != null) {
    			((HelpFragment)fragment).setOnFragmentControlListener(HELP_FRAGMENT, (OnFragmentControlListener)f);
    		}
    	} else if(fragment instanceof AboutFragment) {
    		Fragment f = getSupportFragmentManager().findFragmentByTag(HomeMenuListFragment.TAG);
    		if(f != null) {
    			((AboutFragment)fragment).setOnFragmentControlListener(ABOUT_FRAGMENT, (OnFragmentControlListener)f);
    		}
    	} else if(fragment instanceof HomeMenuListFragment) {
    		((HomeMenuListFragment)fragment).setOnFragmentControlListener(HOME_FRAGMENT, this);
    	} else if(fragment instanceof EditFragment) {
			if(fragment instanceof MemoEditFragment) {
				request = EDIT_MEMO_FRAGMENT;
			} else if(fragment instanceof PhotoEditFragment) {
				request = EDIT_PHOTO_FRAGMENT;
			} else if(fragment instanceof ToDoEditFragment) {
				request = EDIT_TODO_FRAGMENT;
			} else if(fragment instanceof HtmlEditFragment) {
				request = EDIT_HTML_FRAGMENT;
			}
    		if(isTabletMode()) {

    		} else {
    			//Log.d(Constant.LOG_TAG, "Notes onAttachFragment " + fragment.getClass().getSimpleName());
    			Fragment f = getSupportFragmentManager().findFragmentByTag("preview");
    			if(f != null) {
    				((EditFragment)fragment).setOnFragmentControlListener(request, (OnFragmentControlListener) f);
    			} else {
    				((EditFragment)fragment).setOnFragmentControlListener(request, this);
    			}
    		}
    		String tag = fragment.getTag();
    		//Log.d(Constant.LOG_TAG, "onAtachFragment MemoEditFrag tag: " + tag);
    		if(tag.equalsIgnoreCase("edit")) {
    			editFragment = (EditFragment) fragment;
    		}
    	} else if(fragment instanceof PreviewFragment) {
			if(fragment instanceof MemoPreviewFragment) {
				request = PREVIEW_MEMO_FRAGMENT;
			} else if(fragment instanceof PhotoPreviewFragment) {
				request = PREVIEW_PHOTO_FRAGMENT;
			} else if(fragment instanceof ToDoPreviewFragment) {
				request = PREVIEW_TODO_FRAGMENT;
			} else if(fragment instanceof ToDoNewPreviewFragment) {
				request = PREVIEW_TODONEW_FRAGMENT;
			} else if(fragment instanceof HtmlPreviewFragment) {
				request = PREVIEW_HTML_FRAGMENT;
			}
    		if(isTabletMode()) {

    		} else {
    			//Log.d(Constant.LOG_TAG, "Notes onAttachFragment " + fragment.getClass().getSimpleName());
				((PreviewFragment)fragment).setOnFragmentControlListener(request, this);
			}
    	} else if(fragment instanceof DeleteItemDialogFragment) {
    		Fragment f = getSupportFragmentManager().findFragmentByTag("preview");
    		if(f != null) {
    			((DeleteItemDialogFragment)fragment).setResultListener((DeleteItemDialogResultListener)f);
    		} else {
    			((DeleteItemDialogFragment)fragment).setResultListener(this);
    		}
    	}
    }

    public static class TabsPager extends SherlockFragment {
    	private NoteViewPager mViewPager;
    	DreamTabHost mTabHost;
    	TabsAdapter mTabsAdapter;

    	public static TabsPager newInstance(int position) {
    		TabsPager fragment = new TabsPager();
    		Bundle args = new Bundle();
    		args.putInt("position", position);
    		fragment.setArguments(args);
    		return fragment;
    	}

    	private TextView createTabwidgetView(int labelid, int bgid) {
    		TextView view = new TextView(getActivity());
    		view.setText(labelid);
    		view.setTextColor(Color.WHITE);
    		view.setCompoundDrawablesWithIntrinsicBounds(null, getResources().getDrawable(R.drawable.btn_viewer_header), null, getResources().getDrawable(R.drawable.btn_viewer_footer));
    		view.setTextSize(11);
    		view.setTypeface(Typeface.DEFAULT_BOLD);
    		view.setGravity(Gravity.CENTER);
    		view.setPadding(0, 0, 0, 0);
    		view.setBackgroundResource(bgid);
    		return view;
    	}
    	@Override
    	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    		//Log.d(Constant.LOG_TAG, "TabsPager onCreateView");
    		if(container == null) {
//    			//Log.d(Constant.LOG_TAG, "TabsPager onCreateView container == null");
    		}
    		View contentView = inflater.inflate(R.layout.fragment_tabs_pager, container, false);
    		if(contentView == null) {
//    			//Log.d(Constant.LOG_TAG, "TabsPager onCreateView contentView == null");
    		}
    		return contentView;
    	}

    	@Override
    	public void onActivityCreated(Bundle savedInstanceState) {
    		AppInfo.DebugLog(getSherlockActivity().getApplicationContext(), "Tabspager onActivityCreated");
    		super.onActivityCreated(savedInstanceState);
    		mTabHost = (DreamTabHost)getActivity().findViewById(android.R.id.tabhost);
    		mViewPager = (NoteViewPager)getActivity().findViewById(R.id.pager);
    		mTabHost.setup();
    		if(savedInstanceState == null) {
    			if(mTabsAdapter == null) {
		    		mTabsAdapter = new TabsAdapter(getSherlockActivity(), mTabHost, mViewPager);
		    		Bundle argsAll = new Bundle();
		    		argsAll.putString("category", "all");
		    		argsAll.putInt("position", 0);
		    		argsAll.putString("sortorder", sortorder_param.get(sortorder[0]));
		    		mTabsAdapter.addTab(
		    				mTabHost.newTabSpec("all").setIndicator(createTabwidgetView(R.string.viewer_all_label, R.drawable.vall_unselected)),
		    				LoaderCursorSupport.CursorLoaderListFragment.class, argsAll);
		    		Bundle argsMemo = new Bundle();
		    		argsMemo.putString("category", "memos");
		    		argsMemo.putInt("position", 1);
		    		argsMemo.putString("sortorder", sortorder_param.get(sortorder[1]));
		    		mTabsAdapter.addTab(
		    				mTabHost.newTabSpec("memo").setIndicator(createTabwidgetView(R.string.viewer_memo_label, R.drawable.vmemo_unselected)),
		    				LoaderCursorSupport.CursorLoaderListFragment.class, argsMemo);
		    		Bundle argsPhoto = new Bundle();
		    		argsPhoto.putString("category", "photos");
		    		argsPhoto.putInt("position", 2);
		    		argsPhoto.putString("sortorder", sortorder_param.get(sortorder[2]));
		    		mTabsAdapter.addTab(
		    				mTabHost.newTabSpec("photo").setIndicator(createTabwidgetView(R.string.viewer_pic_label, R.drawable.vpic_unselected)),
		    				LoaderCursorSupport.CursorLoaderListFragment.class, argsPhoto);
		    		Bundle argsToDo = new Bundle();
		    		argsToDo.putString("category", "todos");
		    		argsToDo.putInt("position", 3);
		    		argsToDo.putString("sortorder", sortorder_param.get(sortorder[3]));
		    		mTabsAdapter.addTab(
		    				mTabHost.newTabSpec("todo").setIndicator(createTabwidgetView(R.string.viewer_todo_label, R.drawable.vtodo_unselected)),
		    				LoaderCursorSupport.CursorLoaderListFragment.class, argsToDo);
		    		Bundle argsHtml = new Bundle();
		    		argsHtml.putString("category", "htmls");
		    		argsHtml.putInt("position", 4);
		    		argsHtml.putString("sortorder", sortorder_param.get(sortorder[4]));
		    		mTabsAdapter.addTab(
		    				mTabHost.newTabSpec("html").setIndicator(createTabwidgetView(R.string.viewer_clip_label, R.drawable.vclip_unselected)),
		    				LoaderCursorSupport.CursorLoaderListFragment.class, argsHtml);
		    		DisplayMetrics metrics = getResources().getDisplayMetrics();
		    		int h = (int)((56 * metrics.density) + 0.5f);
		    		TabWidget widget = mTabHost.getTabWidget();
		    		int tab_count = widget.getChildCount();
		    		for(int i = 0; i < tab_count; i++) {
		    			widget.getChildTabViewAt(i).getLayoutParams().height = h;
		    		}
		    		Bundle args = getArguments();
		    		if(args != null) {
		    			int initialize_position = args.getInt("position");
		    			mTabHost.setCurrentTab(initialize_position);
		    		}
    			}
    		} else {
    			AppInfo.DebugLog(getSherlockActivity().getApplicationContext(), "Tabspager onActivityCreated savedInstanceState != null");
    			mTabHost.setCurrentTabByTag(savedInstanceState.getString("tab"));
    		}
    	}

    	public void setCurrentPosition(int position) {
    		if(mTabHost != null) {
    			mTabHost.setCurrentTab(position);
    		} else {
    			Bundle args = getArguments();
    			args.putInt("position", position);
    			setArguments(args);
    		}
    	}

    	@Override
    	public void onSaveInstanceState(Bundle outState) {
    		super.onSaveInstanceState(outState);
    		outState.putString("tag", mTabHost.getCurrentTabTag());
    		setUserVisibleHint(true);
    	}

    	class TabInfo {
            private final String tag;
            private final Class<?> clss;
            private Bundle args;
            private Fragment fragment;

            TabInfo(String _tag, Class<?> _class, Bundle _args) {
                tag = _tag;
                clss = _class;
                args = _args;
            }
        }

        class DummyTabFactory implements TabHost.TabContentFactory {
            private final Context mContext;

            public DummyTabFactory(Context context) {
                mContext = context;
            }

            @Override
            public View createTabContent(String tag) {
                View v = new View(mContext);
                v.setMinimumWidth(0);
                v.setMinimumHeight(0);
                return v;
            }
        }

        public void setQuery(String QueryText, String[] arrTags) {
        	mTabsAdapter.setQuery(QueryText, arrTags);
        }

    	public class TabsAdapter extends FragmentStatePagerAdapter
    	implements TabHost.OnTabChangeListener, ViewPager.OnPageChangeListener {
    		private OnPageScrollStateChangedListener mOnPageScrollStateChangedListener;
    		public void setOnPageScrollStateChangedListener(OnPageScrollStateChangedListener listener) {
    			mOnPageScrollStateChangedListener = listener;
    		}
    		private final Context mContext;
    		@SuppressWarnings("unused")
			private final SherlockFragmentActivity activity;
    		private final TabHost mTabHost;
    		private final NoteViewPager mViewPager;
    		private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();
    		private HashMap<String, TabInfo> mapTabInfo = new HashMap<String, TabInfo>();
        	private int mCurrentPosition = -1;
        	private int mNextPosition = -1;

    		public TabsAdapter(SherlockFragmentActivity activity, TabHost tabhost, NoteViewPager viewpager) {
    			super(activity.getSupportFragmentManager());
    			mContext = activity;
    			this.activity = activity;
    			mTabHost = tabhost;
    			mViewPager = viewpager;
    			mTabHost.setOnTabChangedListener(this);
    			mViewPager.setAdapter(this);
    			mViewPager.setOnPageChangeListener(this);
    			setOnPageScrollStateChangedListener((OnPageScrollStateChangedListener) activity);
    		}

    		public void addTab(TabHost.TabSpec tabSpec, Class<?> clss, Bundle args) {
    			tabSpec.setContent(new DummyTabFactory(mContext));
    			String tag = tabSpec.getTag();
    			//Log.d(Constant.LOG_TAG, "addTab: " + tag);

    			TabInfo info = new TabInfo(tag, clss, args);
/*
    			info.fragment = activity.getSupportFragmentManager().findFragmentByTag(tag);
    			if(info.fragment != null && !info.fragment.isDetached()) {
    				FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
    				ft.detach(info.fragment);
    				ft.commit();
    				activity.getSupportFragmentManager().executePendingTransactions();
    				info.fragment = null;
    			}
*/
    			mTabs.add(info);
    			mapTabInfo.put(info.tag, info);
    			mTabHost.addTab(tabSpec);
    			notifyDataSetChanged();
    		}

    		@Override
            public int getCount() {
                return mTabs.size();
            }

    		public void setQuery(String QueryText, String[] arrTags) {
    			int tabs_size = mTabs.size();
    			for(int i = 0; i < tabs_size; i++) {
    				TabInfo info = mTabs.get(i);
    				Bundle args = info.args;
        			args.putString("query", QueryText);
        			args.putStringArray("tags", arrTags);
        			args.putString("sortorder", sortorder_param.get(sortorder[i]));
        			info.args = args;
        			mTabs.set(i, info);
    			}
    		}

    		@Override
            public Fragment getItem(int position) {
    			//Log.d(Constant.LOG_TAG, "TabsPager getItem position: " + position);
                TabInfo info = mTabs.get(position);
                if(info.fragment == null) {
                	//Log.d(Constant.LOG_TAG, "TabsPager getItem instantiate!");
                	info.fragment = SherlockFragment.instantiate(mContext, info.clss.getName(), info.args);
                }
                return info.fragment;
            }
            @Override
            public void onTabChanged(String tabId) {
            	int[] unselected = {
            		R.drawable.vall_unselected,
            		R.drawable.vmemo_unselected,
            		R.drawable.vpic_unselected,
            		R.drawable.vtodo_unselected,
            		R.drawable.vclip_unselected
            	};
                int position = mTabHost.getCurrentTab();
                //Log.d(Constant.LOG_TAG, "onTabChanged position: " + position);
                @SuppressWarnings("unused")
				Fragment frag = getItem(position);
                TabWidget widget = mTabHost.getTabWidget();
                int tab_count = widget.getChildCount();
                for(int i = 0; i < tab_count; i++) {
                	if(position == i) {
                        widget.getChildTabViewAt(i).setBackgroundResource(R.drawable.selected);
                	} else {
                		widget.getChildTabViewAt(i).setBackgroundResource(unselected[i]);
                	}
                }
                //Log.d(Constant.LOG_TAG, "onTabChanged viewpager setCurrentItem: " + position);
                mViewPager.setCurrentItem(position, true);
             }

        	@Override
        	public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        		// Nothing to do
        	}

            @Override
            public void onPageSelected(int position) {
            	if(mViewPager.getPagingEnabled() == false) return;
                TabWidget widget = mTabHost.getTabWidget();
                int oldFocusability = widget.getDescendantFocusability();
                widget.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
                mTabHost.setCurrentTab(position);
                widget.setDescendantFocusability(oldFocusability);
                mNextPosition = position;
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            	switch(state) {
            	case ViewPager.SCROLL_STATE_IDLE:
            		//Log.d(Constant.LOG_TAG, "onPageScrollState IDLE");
        			if(mCurrentPosition >= 0) {
        				if(mOnPageScrollStateChangedListener != null) {
        					mOnPageScrollStateChangedListener.onPageScrollChanged(mCurrentPosition, false);
        				}
        			}
        			if(mNextPosition >= 0) {
        				if(mOnPageScrollStateChangedListener != null) {
        					mOnPageScrollStateChangedListener.onPageScrollChanged(mNextPosition, true);
        				}
        			}
        			mCurrentPosition = mNextPosition;
            		break;
            	}
            }

            public int getCurrentPosition() {
            	return mCurrentPosition;
            }
    	}

    	public int getCurrentTabPosition() {
    		return mTabsAdapter.getCurrentPosition();
        }

    	public void setPagingEnabled(boolean enabled, int pager_position) {
    		mViewPager.setPagingEnabled(enabled);
/*
    		FrameLayout mTabContent = mTabHost.getTabContentView();
    		mTabContent.setEnabled(enabled);
    		mTabContent.setFocusable(enabled);
*/
    		if(enabled) {
    			mTabHost.getTabWidget().setVisibility(View.VISIBLE);
    		} else {
    			mTabHost.getTabWidget().setVisibility(View.GONE);
    		}
    	}

    	public boolean getPagingEnabled() {
    		return mViewPager.getPagingEnabled();
    	}
    }

	@Override
    public boolean onPrepareOptionsMenu(Menu menu) {
		//Log.d(Constant.LOG_TAG, "onPrepareOptionsMenu");
		return super.onPrepareOptionsMenu(menu);
	}

	private void refreshTagListMenu() {
		if(arrTags != null && arrTags.length > 0) {
			ArrayList<String> list = new ArrayList<String>(Arrays.asList(arrTags));
			int size = mTagListMenu.size();
			for(int i = 0; i < size; i++) {
				MenuItem tagitem = mTagListMenu.getItem(i);
				if(list.indexOf(tagitem.getTitle().toString()) != -1) {
					tagitem.setChecked(true);
				} else {
					tagitem.setChecked(false);
				}
			}
			list.clear();
			list = null;
		}
	}

	private void removeFragmentByTag(String tag) {
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
		if(fragment != null) {
			getSupportFragmentManager().beginTransaction().remove(fragment).commit();
			activetitles_fragment.remove(activetitles_fragment.get(activetitles_fragment.size() - 1));
//			supportInvalidateOptionsMenu();
			getSupportFragmentManager().executePendingTransactions();
		}
	}

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
		if(currentMenu != null) {
			currentMenu.clear();
			currentMenu.close();
			currentMenu = null;
		}
		if(menu != null) {
			if(menu.hasVisibleItems()) return true;
			menu.clear();
			menu.close();
		}
    	//Log.d(Constant.LOG_TAG, "onCreateOptionMenu");
    	if(isTabletMode()) {
	    	switch(activetitles_fragment.get(activetitles_fragment.size() - 1)) {
	    	case TABSPAGER_FRAGMENT:
		    	getSupportMenuInflater().inflate(R.menu.actionbar_menu, menu);
		    	SearchViewMenuItem = menu.findItem(R.id.action_menu_search);
		    	searchview = (SearchView)SearchViewMenuItem.getActionView();
		    	searchview.setOnQueryTextListener(this);
		    	searchview.setOnSearchViewCloseListener(this);
		    	searchview.setIconifiedByDefault(true);
		    	if(!TextUtils.isEmpty(strQuery)) {
		    		searchview.setQuery(strQuery, false);
		    		SearchViewMenuItem.expandActionView();
		    		searchview.onActionViewExpanded();
		    		search_mode = true;
		    	}

		    	addnote = menu.findItem(R.id.action_menu_addnote);
		    	addnote_submenu = addnote.getSubMenu();

		    	ListSortMenuItem = menu.findItem(R.id.action_menu_listsort);
		    	ListSortSubMenu = ListSortMenuItem.getSubMenu();

		    	TagSelectorMenuItem = menu.findItem(R.id.action_menu_select_tags);
		    	TagSelectorView = (TagSelectorView) TagSelectorMenuItem.getActionView();
		    	if(TagSelectorView != null) {
		    		TagSelectorView.setOnQueryTextListener(this);
		    	}
	    		if(mTagListMenu != null) {
	    			if(mTagListMenu.size() > 0) {
		    			refreshTagListMenu();
		    			if(TagSelectorView != null) {
		    				TagSelectorView.setTagList(mTagListMenu);
		    			}
			    		TagSelectorMenuItem.setEnabled(true);
			    		TagSelectorMenuItem.setVisible(true);
	    			} else {
			    		TagSelectorMenuItem.setEnabled(false);
			    		TagSelectorMenuItem.setVisible(false);
	    			}
		    	} else {
		    		TagSelectorMenuItem.setEnabled(false);
		    		TagSelectorMenuItem.setVisible(false);
		    	}
		    	//Log.d(Constant.LOG_TAG, "Tablet Mode onCreateOptionMenu");
		    	//Log.d(Constant.LOG_TAG, "current pager position: " + currentpager_position);
		    	break;
	    	}
    	} else {
    		int titles_frag = activetitles_fragment.get(activetitles_fragment.size() - 1);
    		AppInfo.DebugLog(getApplicationContext(), "onCreateOptionMenu switch menu current fragment: " + titles_frag);
	    	switch(titles_frag) {
	    	case TABSPAGER_FRAGMENT:
		    	getSupportMenuInflater().inflate(R.menu.actionbar_menu, menu);
		    	SearchViewMenuItem = menu.findItem(R.id.action_menu_search);
		    	searchview = (SearchView)SearchViewMenuItem.getActionView();
		    	searchview.setOnQueryTextListener(this);
		    	searchview.setOnSearchViewCloseListener(this);
		    	searchview.setIconifiedByDefault(true);
		    	if(!TextUtils.isEmpty(strQuery)) {
		    		searchview.setQuery(strQuery, false);
		    		SearchViewMenuItem.expandActionView();
		    		searchview.onActionViewExpanded();
		    		search_mode = true;
		    	}

		    	addnote = menu.findItem(R.id.action_menu_addnote);
		    	addnote_submenu = addnote.getSubMenu();

		    	ListSortMenuItem = menu.findItem(R.id.action_menu_listsort);
		    	ListSortSubMenu = ListSortMenuItem.getSubMenu();

		    	TagSelectorMenuItem = menu.findItem(R.id.action_menu_select_tags);
		    	TagSelectorView = (TagSelectorView) TagSelectorMenuItem.getActionView();
		    	if(TagSelectorView != null) {
		    		TagSelectorView.setOnQueryTextListener(this);
		    	}
	    		if(mTagListMenu != null) {
	    			if(mTagListMenu.size() > 0) {
		    			refreshTagListMenu();
		    			if(TagSelectorView != null) {
		    				TagSelectorView.setTagList(mTagListMenu);
		    			}
			    		TagSelectorMenuItem.setEnabled(true);
			    		TagSelectorMenuItem.setVisible(true);
	    			} else {
			    		TagSelectorMenuItem.setEnabled(false);
			    		TagSelectorMenuItem.setVisible(false);
	    			}
		    	} else {
		    		TagSelectorMenuItem.setEnabled(false);
		    		TagSelectorMenuItem.setVisible(false);
		    	}
	    		getSupportActionBar().setTitle(getResources().getString(R.string.viewer_title));
		    	break;
	    	case TAGLIST_FRAGMENT:
	    		getSupportMenuInflater().inflate(R.menu.help_about_menu, menu);
	    		getSupportActionBar().setTitle(getResources().getString(R.string.taglist_title));
	    		break;
			case HOME_FRAGMENT:
	    		getSupportMenuInflater().inflate(R.menu.dummy_menu, menu);
	    		getSupportActionBar().setTitle(getResources().getString(R.string.app_name));
	    		break;
			case PREFERENCE_FRAGMENT:
	    		getSupportMenuInflater().inflate(R.menu.preference_frag_menu, menu);
	    		getSupportActionBar().setTitle(getResources().getString(R.string.preference_title));
	    		break;
			case HELP_FRAGMENT:
	    		getSupportMenuInflater().inflate(R.menu.help_about_menu, menu);
	    		getSupportActionBar().setTitle(getResources().getString(R.string.cliphelp_title));
				break;
			case ABOUT_FRAGMENT:
	    		getSupportMenuInflater().inflate(R.menu.help_about_menu, menu);
	    		getSupportActionBar().setTitle(getResources().getString(R.string.about_dreamnote));
				break;
	    	case PREVIEW_MEMO_FRAGMENT:
	    		getSupportMenuInflater().inflate(R.menu.preview_memo_frag_menu, menu);
	    		getSupportActionBar().setTitle("");
	    		break;
	    	case PREVIEW_PHOTO_FRAGMENT:
	    		if(isSplitActionbarIsNarrow() == false) {
		    		getSupportMenuInflater().inflate(R.menu.preview_photo_frag_w480dp_menu, menu);
	    		} else {
		    		getSupportMenuInflater().inflate(R.menu.preview_photo_frag_menu, menu);
	    		}
	    		getSupportActionBar().setTitle("");
	    		break;
	    	case PREVIEW_TODONEW_FRAGMENT:
	    		getSupportMenuInflater().inflate(R.menu.preview_todonew_frag_menu, menu);
	    		getSupportActionBar().setTitle("");
	    		break;
	    	case PREVIEW_TODO_FRAGMENT:
	    		getSupportMenuInflater().inflate(R.menu.preview_todo_frag_menu, menu);
	    		getSupportActionBar().setTitle("");
	    		break;
	    	case PREVIEW_HTML_FRAGMENT:
	    		if(isSplitActionbarIsNarrow() == false) {
	    			getSupportMenuInflater().inflate(R.menu.preview_html_frag_w480dp_menu, menu);
	    		} else {
		    		Configuration config = getResources().getConfiguration();
		    		if(isW360dp() == false && config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
		    			getSupportMenuInflater().inflate(R.menu.preview_html_frag_menu_qvga, menu);
		    		} else {
		    			getSupportMenuInflater().inflate(R.menu.preview_html_frag_menu, menu);
		    		}
	    		}
	    		getSupportActionBar().setTitle("");
	    		break;
	    	case EDIT_MEMO_FRAGMENT:
	    		getSupportMenuInflater().inflate(R.menu.edit_memo_frag_menu, menu);
	    		break;
	    	case EDIT_PHOTO_FRAGMENT:
	    		getSupportMenuInflater().inflate(R.menu.edit_photo_frag_menu, menu);
	    		break;
	    	case EDIT_TODO_FRAGMENT:
	    		getSupportMenuInflater().inflate(R.menu.edit_todo_frag_menu, menu);
	    		break;
	    	case EDIT_HTML_FRAGMENT:
	    		getSupportMenuInflater().inflate(R.menu.edit_photo_frag_menu, menu);
	    		break;
	    	}
    	}
    	currentMenu = menu;
    	return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	AppInfo.DebugLog(getApplicationContext(), "onOptionsItemSelected");
    	Fragment fragment = null;
		switch (item.getItemId()) {
		case android.R.id.home:
			AppInfo.DebugLog(getApplicationContext(), "home selected");
			if(isTabletMode()) {
				switch(activetitles_fragment.get(activetitles_fragment.size() - 1)) {
				case TABSPAGER_FRAGMENT:
					if(search_mode == false) {
						// G+アプリのようにアイコンをタッチするとメニューの表示、非表示を切り替えます
						onClickedHomeButton(null);
					}
					addnote_submenu.close();
					break;
				case TAGLIST_FRAGMENT:
					TagListFragment f = (TagListFragment) getSupportFragmentManager().findFragmentByTag(TagListFragment.TAG);
					if(f != null) {
						f.onAbort();
					}
//					removeFragmentByTag("taglist");
					break;
				case HOME_FRAGMENT:
					HomeMenuListFragment hmf = (HomeMenuListFragment) getSupportFragmentManager().findFragmentByTag(HomeMenuListFragment.TAG);
					if(hmf != null) {
						hmf.onAbort();
					}
					break;
				case PREFERENCE_FRAGMENT:
					PreferenceFragment pf = (PreferenceFragment)getSupportFragmentManager().findFragmentByTag(PreferenceFragment.TAG);
					if(pf != null) {
						pf.onAbort();
					}
					break;
				case HELP_FRAGMENT:
					HelpFragment hf = (HelpFragment)getSupportFragmentManager().findFragmentByTag(HelpFragment.TAG);
					if(hf != null) {
						hf.onAbort();
					}
					break;
				case ABOUT_FRAGMENT:
					AboutFragment af = (AboutFragment)getSupportFragmentManager().findFragmentByTag(AboutFragment.TAG);
					if(af != null) {
						af.onAbort();
					}
					break;
				}
				return true;
			} else {
	    		int titles_frag = activetitles_fragment.get(activetitles_fragment.size() - 1);
	    		AppInfo.DebugLog(getApplicationContext(), "onOptionsItemSelected select menu [home] current fragment: " + titles_frag);
				switch(titles_frag) {
				case TABSPAGER_FRAGMENT:
					if(addnote_submenu == null) {
						supportInvalidateOptionsMenu();
						return true;
					}
					addnote_submenu.close();
					if(search_mode == false) {
						// G+アプリのようにアイコンをタッチするとメニューの表示、非表示を切り替えます
						onClickedHomeButton(null);
					}
					break;
				case TAGLIST_FRAGMENT:
					TagListFragment f = (TagListFragment) getSupportFragmentManager().findFragmentByTag(TagListFragment.TAG);
					if(f != null) {
						f.onAbort();
					}
//					removeFragmentByTag("taglist");
					break;
				case HOME_FRAGMENT:
					HomeMenuListFragment hmf = (HomeMenuListFragment) getSupportFragmentManager().findFragmentByTag(HomeMenuListFragment.TAG);
					if(hmf != null) {
						hmf.onAbort();
					}
					break;
				case PREFERENCE_FRAGMENT:
					PreferenceFragment pf = (PreferenceFragment)getSupportFragmentManager().findFragmentByTag(PreferenceFragment.TAG);
					if(pf != null) {
						pf.onAbort();
					}
					break;
				case HELP_FRAGMENT:
					HelpFragment hf = (HelpFragment)getSupportFragmentManager().findFragmentByTag(HelpFragment.TAG);
					if(hf != null) {
						hf.onAbort();
					}
					break;
				case ABOUT_FRAGMENT:
					AboutFragment af = (AboutFragment)getSupportFragmentManager().findFragmentByTag(AboutFragment.TAG);
					if(af != null) {
						af.onAbort();
					}
					break;
				case PREVIEW_MEMO_FRAGMENT:
				case PREVIEW_PHOTO_FRAGMENT:
				case PREVIEW_TODONEW_FRAGMENT:
				case PREVIEW_TODO_FRAGMENT:
				case PREVIEW_HTML_FRAGMENT:
					fragment = getSupportFragmentManager().findFragmentByTag("preview");
					if(fragment != null) {
						((PreviewFragment)fragment).onAbort();
					}
//					removeFragmentByTag("preview");
					break;
				case EDIT_MEMO_FRAGMENT:
				case EDIT_TODO_FRAGMENT:
				case EDIT_PHOTO_FRAGMENT:
				case EDIT_HTML_FRAGMENT:
					fragment = getSupportFragmentManager().findFragmentByTag("edit");
					if(fragment != null) {
						((EditFragment)fragment).onAbort();
					}
					break;
				}
				return true;
			}
		case R.id.action_menu_search:
			//Log.d(Constant.LOG_TAG, "search selected");
			addnote.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);
			ListSortMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

			SearchViewMenuItem.expandActionView();
			searchview.onActionViewExpanded();
			search_mode = true;
			if(hmFragment != null && hmFragment.isAvailable()) {
				hmFragment.hide();
			}
			if(addnote_mode) {
		    	addnote_submenu.close();
		    	addnote_mode = false;
			}
			if(listsort_mode) {
				ListSortSubMenu.close();
				listsort_mode = false;
			}
			break;
		case R.id.action_menu_addnote:
			//Log.d(Constant.LOG_TAG, "addnote selected");
			addnote_mode ^= addnote_mode;
			if(hmFragment != null && hmFragment.isAvailable()) {
				hmFragment.hide();
			}
			if(SearchViewMenuItem.isActionViewExpanded()) {
				SearchViewMenuItem.collapseActionView();
				search_mode = false;
			}
			if(listsort_mode) {
				ListSortSubMenu.close();
				listsort_mode = false;
			}
			break;
		case R.id.action_menu_addmemo:
			MemoEditFragment memofrag = new MemoEditFragment();
//			frag.setOnFragmentResultListener(EDIT_MEMO_FRAGMENT, this);
			editFragment = memofrag;
			addNoteFragment(getSupportFragmentManager(), memofrag, EDIT_MEMO_FRAGMENT, "edit");
//			supportInvalidateOptionsMenu();
			return true;
		case R.id.action_menu_addphoto:
			PhotoEditFragment photofrag = new PhotoEditFragment();
			editFragment = photofrag;
			addNoteFragment(getSupportFragmentManager(), photofrag, EDIT_PHOTO_FRAGMENT, "edit");
			return true;
		case R.id.action_menu_addtodo:
			ToDoEditFragment todofrag = new ToDoEditFragment();
			editFragment = todofrag;
			addNoteFragment(getSupportFragmentManager(), todofrag, EDIT_TODO_FRAGMENT, "edit");
			return true;
		case R.id.action_menu_listsort:
			//Log.d(Constant.LOG_TAG, "listsort selected");
			listsort_mode ^= listsort_mode;
			if(hmFragment != null && hmFragment.isAvailable()) {
				hmFragment.hide();
			}
			if(SearchViewMenuItem.isActionViewExpanded()) {
				SearchViewMenuItem.collapseActionView();
				search_mode = false;
			}
			if(addnote_mode) {
		    	addnote_submenu.close();
		    	addnote_mode = false;
			}
			break;
		case R.id.action_menu_listsort_create_asc:
			item.setChecked(true);
			//Log.d(Constant.LOG_TAG, "listsort create asc position: " + currentpager_position);
			sortorder[currentpager_position] = sorttype.indexOf(R.id.action_menu_listsort_create_asc);
			reOrder(currentpager_position);
			break;
		case R.id.action_menu_listsort_create_desc:
			item.setChecked(true);
			//Log.d(Constant.LOG_TAG, "listsort create desc position: " + currentpager_position);
			sortorder[currentpager_position] = sorttype.indexOf(R.id.action_menu_listsort_create_desc);
			reOrder(currentpager_position);
			break;
		case R.id.action_menu_listsort_date_asc:
			item.setChecked(true);
			//Log.d(Constant.LOG_TAG, "listsort updated asc position: " + currentpager_position);
			sortorder[currentpager_position] = sorttype.indexOf(R.id.action_menu_listsort_date_asc);
			reOrder(currentpager_position);
			break;
		case R.id.action_menu_listsort_date_desc:
			item.setChecked(true);
			//Log.d(Constant.LOG_TAG, "listsort updated desc position: " + currentpager_position);
			sortorder[currentpager_position] = sorttype.indexOf(R.id.action_menu_listsort_date_desc);
			reOrder(currentpager_position);
			break;
		case R.id.action_menu_listsort_title_asc:
			item.setChecked(true);
			//Log.d(Constant.LOG_TAG, "listsort title asc position: " + currentpager_position);
			sortorder[currentpager_position] = sorttype.indexOf(R.id.action_menu_listsort_title_asc);
			reOrder(currentpager_position);
			break;
		case R.id.action_menu_listsort_title_desc:
			item.setChecked(true);
			//Log.d(Constant.LOG_TAG, "listsort title desc position: " + currentpager_position);
			sortorder[currentpager_position] = sorttype.indexOf(R.id.action_menu_listsort_title_desc);
			reOrder(currentpager_position);
			break;
		case R.id.action_menu_listsort_todo:
			item.setChecked(true);
			//Log.d(Constant.LOG_TAG, "listsort todo position: " + currentpager_position);
			sortorder[currentpager_position] = sorttype.indexOf(R.id.action_menu_listsort_todo);
			reOrder(currentpager_position);
			break;
		default:
			break;
		}
		return super.onOptionsItemSelected(item);
    }

	@Override
    public boolean dispatchKeyEvent(KeyEvent event){
		Fragment fragment = null;
		int keycode = event.getKeyCode();
		AppInfo.DebugLog(getApplicationContext(), "Notes dispatchKeyEvent keycode: " + keycode);
		if(keycode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN){
			if(isTabletMode()) {
				if(activetitles_fragment.get(activetitles_fragment.size() - 1) == TAGLIST_FRAGMENT) {
					TagListFragment f = (TagListFragment) getSupportFragmentManager().findFragmentByTag("taglist");
					if(f != null) {
						f.onAbort();
					}
//						removeFragmentByTag("taglist");
					return true;
				}
				return super.dispatchKeyEvent(event);
			} else {
				switch(activetitles_fragment.get(activetitles_fragment.size() - 1)) {
				case TAGLIST_FRAGMENT:
					TagListFragment f = (TagListFragment) getSupportFragmentManager().findFragmentByTag(TagListFragment.TAG);
					if(f != null) {
						f.onAbort();
					}
//						removeFragmentByTag("taglist");
					return true;
				case HOME_FRAGMENT:
					HomeMenuListFragment hmf = (HomeMenuListFragment) getSupportFragmentManager().findFragmentByTag(HomeMenuListFragment.TAG);
					if(hmf != null) {
						hmf.onAbort();
					}
					return true;
				case PREFERENCE_FRAGMENT:
					PreferenceFragment pf = (PreferenceFragment)getSupportFragmentManager().findFragmentByTag(PreferenceFragment.TAG);
					if(pf != null) {
						pf.onAbort();
					}
					return true;
				case HELP_FRAGMENT:
					HelpFragment hf = (HelpFragment)getSupportFragmentManager().findFragmentByTag(HelpFragment.TAG);
					if(hf != null) {
						hf.onAbort();
					}
					return true;
				case ABOUT_FRAGMENT:
					AboutFragment af = (AboutFragment)getSupportFragmentManager().findFragmentByTag(AboutFragment.TAG);
					if(af != null) {
						af.onAbort();
					}
					return true;
				case PREVIEW_MEMO_FRAGMENT:
				case PREVIEW_PHOTO_FRAGMENT:
				case PREVIEW_TODONEW_FRAGMENT:
				case PREVIEW_TODO_FRAGMENT:
				case PREVIEW_HTML_FRAGMENT:
					fragment = getSupportFragmentManager().findFragmentByTag("preview");
					if(fragment != null) {
						((PreviewFragment)fragment).onAbort();
					}
//						removeFragmentByTag("preview");
					return true;
				case EDIT_MEMO_FRAGMENT:
				case EDIT_PHOTO_FRAGMENT:
				case EDIT_TODO_FRAGMENT:
				case EDIT_HTML_FRAGMENT:
					fragment = getSupportFragmentManager().findFragmentByTag("edit");
					if(fragment != null) {
						((EditFragment)fragment).onAbort();
					}
					return true;
				default:
					return super.dispatchKeyEvent(event);
				}
			}
		} else if(keycode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
			if(isTabletMode()) {
			} else {
				switch(activetitles_fragment.get(activetitles_fragment.size() - 1)) {
				case EDIT_MEMO_FRAGMENT:
				case EDIT_PHOTO_FRAGMENT:
				case EDIT_TODO_FRAGMENT:
				case EDIT_HTML_FRAGMENT:
					boolean result = false;
					AppInfo.DebugLog(getApplicationContext(), "Notes dispatchKeyEvent Enter");
					fragment = getSupportFragmentManager().findFragmentByTag("edit");
					if(fragment != null) {
						 result = ((EditFragment)fragment).onDispatchKeyDown(keycode, event);
					}
					if(result) {
						return super.dispatchKeyEvent(event);
					}
					return false;
				default:
					return super.dispatchKeyEvent(event);
				}
			}
		}
		return super.dispatchKeyEvent(event);
	}

    public void onClickedHomeButton(View view) {
		FragmentManager fm = getSupportFragmentManager();
		FragmentTransaction ft = fm.beginTransaction();
    	// フラグメントの表示の切替をする
		hmFragment = new HomeMenuListFragment();
		hmFragment.setOnFragmentControlListener(HOME_FRAGMENT, this);
		hmFragment.setTagListItemListener(this);
		ft.add(R.id.titles, hmFragment, HomeMenuListFragment.TAG);
		activetitles_fragment.add(HOME_FRAGMENT);
    	ft.commit();
    }

	@Override
	public boolean onSearchViewClose() {
		search_mode = false;
		addnote.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		ListSortMenuItem.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return false;
	}

//	private void reAttachActivity(CursorLoaderListFragment fragment) {
	private void reAttachActivity(Fragment fragment) {
		if(fragment.isDetached()) {
			//Log.d(Constant.LOG_TAG, "reAttach fragment: " + fragment.getClass().getSimpleName());

			FragmentManager fm = getSupportFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			ft.attach(fragment);
			ft.commit();
			fm.executePendingTransactions();

		}
	}

	private void reOrder(int position) {
		CursorLoaderListFragment frag = (CursorLoaderListFragment)getFragmentAt(position);
		if(frag != null) {
			//Log.d(Constant.LOG_TAG, "reOrder position: " + position);
			//Log.d(Constant.LOG_TAG, "reOrder sortParam: " + sortorder_param.get(sortorder[position]));
			reAttachActivity(frag);
			frag.setQuery(strQuery, arrTags, sortorder_param.get(sortorder[position]));
		}
	}

	private void reQuery() {
		if(ItemAllFragment != null) {
			//Log.d(Constant.LOG_TAG, "try retach fragment all");
			reAttachActivity(ItemAllFragment);
			ItemAllFragment.setQuery(strQuery, arrTags, sortorder_param.get(sortorder[TAB_INDEX_ALL]));
		}
		if(ItemMemoFragment != null) {
			//Log.d(Constant.LOG_TAG, "try retach fragment memo");
			reAttachActivity(ItemMemoFragment);
			ItemMemoFragment.setQuery(strQuery, arrTags, sortorder_param.get(sortorder[TAB_INDEX_MEMO]));
		}
		if(ItemPhotoFragment != null) {
			//Log.d(Constant.LOG_TAG, "try retach fragment photo");
			reAttachActivity(ItemPhotoFragment);
			ItemPhotoFragment.setQuery(strQuery, arrTags, sortorder_param.get(sortorder[TAB_INDEX_PHOTO]));
		}
		if(ItemToDoFragment != null) {
			//Log.d(Constant.LOG_TAG, "try retach fragment todo");
			reAttachActivity(ItemToDoFragment);
			ItemToDoFragment.setQuery(strQuery, arrTags, sortorder_param.get(sortorder[TAB_INDEX_TODO]));
		}
		if(ItemHtmlFragment != null) {
			//Log.d(Constant.LOG_TAG, "try retach fragment html");
			reAttachActivity(ItemHtmlFragment);
			ItemHtmlFragment.setQuery(strQuery, arrTags, sortorder_param.get(sortorder[TAB_INDEX_HTML]));
		}
		mTabsPager.setQuery(strQuery, arrTags);
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		return false;
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		//Log.d(Constant.LOG_TAG, "onQueryTextChange");
		strQuery = newText;
		if(StringUtils.isBlank(strQuery)) {
			SearchViewMenuItem.setIcon(R.drawable.ic_menu_search);
		} else {
			SearchViewMenuItem.setIcon(R.drawable.ic_menu_search_red);
		}
		reQuery();
		return false;
	}

	@Override
	public boolean onQueryTagsChange(String[] arrTags) {
		this.arrTags = arrTags;
		reQuery();
		return false;
	}

	@Override
	public void onTagListItem(String[] selectTags) {
		//Log.d(Constant.LOG_TAG, "onTagListItem");
		arrTags = selectTags;
		reQuery();
		removeFragmentByTag("taglist");
	}

	@Override
	public void onLoadFinished(Cursor result) {
		//DatabaseUtils.dumpCursor(result);
		ArrayList<String> resTagList = new ArrayList<String>();
		boolean isEof = result.moveToFirst();
		while(isEof) {
			@SuppressWarnings("unused")
			int id = result.getInt(result.getColumnIndex("_id"));
			String term = result.getString(result.getColumnIndex("term"));
//			//Log.d(Constant.LOG_TAG, "tag onloadfinished id: " + id + " term: " + term);
			resTagList.add(term);
			isEof = result.moveToNext();
		}
		ArrayList<String> list = null;
		if(arrTags != null && arrTags.length > 0) {
			list = new ArrayList<String>(Arrays.asList(arrTags));
			int size = arrTags.length;
			for(int i = 0; i < size; i++) {
				String tag = arrTags[i];
				if(resTagList.indexOf(tag) == -1) {
					list.remove(tag);
				}
			}
			arrTags = list.toArray(new String[0]);
		}
		if(mTagListMenu != null) {
//			mTagListMenu.removeGroup(0);
			mTagListMenu = null;
		}
		mTagListMenu = new MenuBuilder(this);
		Iterator<String> iter = resTagList.iterator();
		while(iter.hasNext()) {
			String term = iter.next();
			MenuItem tagitem = mTagListMenu.add(term);
			if(list != null && list.indexOf(term) != -1) {
				tagitem.setChecked(true);
			}
			tagitem.setCheckable(true);
		}
		mTagListMenu.setGroupEnabled(0, true);
		mTagListMenu.setGroupCheckable(0, true, false);
		if(TagSelectorMenuItem != null) {
    		if(mTagListMenu != null) {
    			if(mTagListMenu.size() > 0) {
	    			refreshTagListMenu();
	    			if(TagSelectorView != null) {
	    				TagSelectorView.setTagList(mTagListMenu);
	    			}
		    		TagSelectorMenuItem.setEnabled(true);
		    		TagSelectorMenuItem.setVisible(true);
    	    	} else {
    	    		TagSelectorMenuItem.setEnabled(false);
    	    		TagSelectorMenuItem.setVisible(false);
    	    	}
    		}
		}
		if(list != null) {
			list.clear();
			list = null;
		}
	}

	@Override
	public void onNoteSelected(int datatype, Item item) {
		//Log.d(Constant.LOG_TAG, "List Selected datatype: " + datatype);
		shownote_item = item;
		shownote_datatype = datatype;
		FragmentManager fm = getSupportFragmentManager();
		Fragment frag = fm.findFragmentByTag("preview");
		if(frag != null) {
			FragmentTransaction ft = fm.beginTransaction();
			ft.remove(frag).commit();
			fm.executePendingTransactions();
		}
		showNote(fm, datatype, item, "preview");
		supportInvalidateOptionsMenu();
	}

	@Override
	public void onNoteDeleted() {

	}

	@Override
	public void onItemLongClick(AdapterView<?> adapter, View view, int position, long id) {
		//Log.d(Constant.LOG_TAG, "Notes onItemLongClick");
		boolean enabled = mTabsPager.getPagingEnabled();
		if(enabled == false) {
			return;
		}
		mTabsPager.setPagingEnabled(false, currentpager_position);
		//Log.d(Constant.LOG_TAG, "Notes onItemLongClick currentpager: " + currentpager_position);
		CursorLoaderListFragment frag = (CursorLoaderListFragment)getFragmentAt(currentpager_position);
		frag.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
		MultipleChoiceMode ChoiceMode = new MultipleChoiceMode();
		ChoiceMode.setDeleteItemDialogResultListener(this);
		mMode = startActionMode(ChoiceMode);
	}

	private void addNoteFragmentTrans(FragmentManager fm, Fragment f, int ftype, String tag) {
		FragmentTransaction ft = fm.beginTransaction();
		if(isTabletMode()) {
			ft.add(R.id.details, f, tag);
			activedetails_fragment.add(ftype);
		} else {
			ft.add(R.id.titles, f, tag);
			activetitles_fragment.add(ftype);
		}
		ft.commit();
	}

	private void addNoteFragment(FragmentManager fm, Fragment f, int ftype, String tag) {
		((PreviewFragment)f).setOnFragmentControlListener(ftype, this);
		addNoteFragmentTrans(fm, f, ftype, tag);
	}

	private void showNote(final FragmentManager fm, final int datatype, final Item item, final String tag) {
		if(datatype == DreamNoteProvider.ITEMTYPE_MEMO) {
			MemoPreviewFragment memofrag = MemoPreviewFragment.newInstance(item);
			addNoteFragment(fm, memofrag, PREVIEW_MEMO_FRAGMENT, tag);
		} else if(datatype == DreamNoteProvider.ITEMTYPE_PHOTO) {
			PhotoPreviewFragment photofrag = PhotoPreviewFragment.newInstance(item);
			addNoteFragment(fm, photofrag, PREVIEW_PHOTO_FRAGMENT, tag);
		} else if(datatype == DreamNoteProvider.ITEMTYPE_TODONEW) {
			ToDoNewPreviewFragment f = ToDoNewPreviewFragment.newInstance(item);
			addNoteFragment(fm, f, PREVIEW_TODONEW_FRAGMENT, tag);
		} else if(datatype == DreamNoteProvider.ITEMTYPE_TODO) {
			ToDoPreviewFragment f = ToDoPreviewFragment.newInstance(item);
			addNoteFragment(fm, f, PREVIEW_TODO_FRAGMENT, tag);
		} else if(datatype == DreamNoteProvider.ITEMTYPE_HTML) {
			HtmlPreviewFragment f = HtmlPreviewFragment.newInstance(item);
			addNoteFragment(fm, f, PREVIEW_HTML_FRAGMENT, tag);
		}
	}


	@Override
	public void onAddNoteFragment(OnFragmentControlListener listener, Fragment fragment, int fragment_type, String tag) {
		if(fragment instanceof PreviewFragment) {
			((PreviewFragment)fragment).setOnFragmentControlListener(fragment_type, listener);
		} else if(fragment instanceof TagListFragment) {
			((TagListFragment)fragment).setOnFragmentControlListener(fragment_type, listener);
		} else if(fragment instanceof PreferenceFragment) {
			((PreferenceFragment)fragment).setOnFragmentControlListener(fragment_type, listener);
		} else if(fragment instanceof HelpFragment) {
			((HelpFragment)fragment).setOnFragmentControlListener(fragment_type, listener);
		} else if(fragment instanceof AboutFragment) {
			((AboutFragment)fragment).setOnFragmentControlListener(fragment_type, listener);
		}
		FragmentManager fm = getSupportFragmentManager();
		addNoteFragmentTrans(fm, fragment, fragment_type, tag);
		supportInvalidateOptionsMenu();
	}

	@Override
	public void onFragmentResult(Fragment fragment, int requestCode, int resultCode, Bundle extra) {
		//Log.d(Constant.LOG_TAG, "onFragmentResult requestCode: " + requestCode + " resultCode: " + resultCode);
/*
		switch(requestCode) {
		case PREVIEW_MEMO_FRAGMENT:
		case PREVIEW_PHOTO_FRAGMENT:
		case PREVIEW_TODONEW_FRAGMENT:
		case PREVIEW_TODO_FRAGMENT:
		case PREVIEW_HTML_FRAGMENT:
		case EDIT_MEMO_FRAGMENT:
		case EDIT_TODO_FRAGMENT:
			activetitles_fragment.remove(activetitles_fragment.get(activetitles_fragment.size() - 1));
			break;
		}
*/
		if(isTabletMode()) {
			activedetails_fragment.remove(activedetails_fragment.get(activedetails_fragment.size() - 1));
		} else {
			activetitles_fragment.remove(activetitles_fragment.get(activetitles_fragment.size() - 1));
		}
		supportInvalidateOptionsMenu();
	}

	ActionMode mMode;
	private boolean isItemSelected(int type) {
		Iterator<Item> iter = SelectedItems.iterator();
		while(iter.hasNext()) {
			Item item = (Item) iter.next();
			int datatype = item.datatype;
			if(datatype == type) {
				return true;
			}
		}
		return false;
	}
	@Override
	public void onMultiModeItemSelected(ArrayListItem list) {
		boolean todoEnabled;
		boolean todonewEnabled;
		boolean htmlEnabled;
		boolean everEnabled;
		boolean memo;
		boolean photo;
		boolean todo;
		boolean todonew;
		boolean html;

		if(SelectedItems != null) {
			SelectedItems.clear();
			SelectedItems = null;
		}
		SelectedItems = list;
		switch(currentpager_position) {
		case TAB_INDEX_ALL:
			memo = isItemSelected(DreamNoteProvider.ITEMTYPE_MEMO);
			photo = isItemSelected(DreamNoteProvider.ITEMTYPE_PHOTO);
			todo = isItemSelected(DreamNoteProvider.ITEMTYPE_TODO);
			todonew = isItemSelected(DreamNoteProvider.ITEMTYPE_TODONEW);
			html = isItemSelected(DreamNoteProvider.ITEMTYPE_HTML);
			ActionModeMenuItemEvernote.getSubMenu().setGroupEnabled(0, false);
			ActionModeMenuItemEvernote.getSubMenu().setGroupVisible(0, false);
			if(memo || photo) {
				todoEnabled = false;
				todonewEnabled = false;
				htmlEnabled = false;
				if(memo && photo) {
					everEnabled = false;
				} else {
					if(SelectedItems.size() > 1) {
						everEnabled = false;
					} else {
						everEnabled = true;
					}
				}
			} else {
				if(html && (todo || todonew)) {
					todoEnabled = false;
					todonewEnabled = false;
					htmlEnabled = false;
					everEnabled = false;
				} else if(html) {
					todoEnabled = false;
					todonewEnabled = false;
					htmlEnabled = true;
					if(SelectedItems.size() > 1) {
						everEnabled = false;
					} else {
						everEnabled = true;
					}
					ActionModeMenuItemEvernote.getSubMenu().setGroupEnabled(0, true);
					ActionModeMenuItemEvernote.getSubMenu().setGroupVisible(0, true);
				} else if(todo && todonew) {
					todoEnabled = false;
					todonewEnabled = false;
					htmlEnabled = false;
					everEnabled = true;
				} else if(todo) {
					todoEnabled = false;
					todonewEnabled = true;
					htmlEnabled = false;
					everEnabled = true;
				} else if(todonew) {
					todoEnabled = true;
					todonewEnabled = false;
					htmlEnabled = false;
					everEnabled = true;
				} else {
					todoEnabled = false;
					todonewEnabled = false;
					htmlEnabled = false;
					everEnabled = false;
				}
			}
			updateActionModeMenu(SelectedItems.size(), todoEnabled, todonewEnabled, htmlEnabled, everEnabled);
			break;
		case TAB_INDEX_MEMO:
			todoEnabled = false;
			todonewEnabled = false;
			htmlEnabled = false;
			if(SelectedItems.size() > 1) {
				everEnabled = false;
			} else {
				everEnabled = true;
			}
			updateActionModeMenu(SelectedItems.size(), todoEnabled, todonewEnabled, htmlEnabled, everEnabled);
			break;
		case TAB_INDEX_PHOTO:
			todoEnabled = false;
			todonewEnabled = false;
			htmlEnabled = false;
			if(SelectedItems.size() > 1) {
				everEnabled = false;
			} else {
				everEnabled = true;
			}
			updateActionModeMenu(SelectedItems.size(), todoEnabled, todonewEnabled, htmlEnabled, everEnabled);
			break;
		case TAB_INDEX_TODO:
			todo = isItemSelected(DreamNoteProvider.ITEMTYPE_TODO);
			todonew = isItemSelected(DreamNoteProvider.ITEMTYPE_TODONEW);
			everEnabled = true;
			if(todo && todonew) {
				todoEnabled = false;
				todonewEnabled = false;
				htmlEnabled = false;
			} else if(todo) {
				todoEnabled = false;
				todonewEnabled = true;
				htmlEnabled = false;
			} else {
				todoEnabled = true;
				todonewEnabled = false;
				htmlEnabled = false;
			}
			updateActionModeMenu(SelectedItems.size(), todoEnabled, todonewEnabled, htmlEnabled, everEnabled);
			break;
		case TAB_INDEX_HTML:
			todoEnabled = false;
			todonewEnabled = false;
			htmlEnabled = true;
			if(SelectedItems.size() > 1) {
				everEnabled = false;
			} else {
				everEnabled = true;
			}
			updateActionModeMenu(SelectedItems.size(), todoEnabled, todonewEnabled, htmlEnabled, everEnabled);
			break;
		}
		//Log.d(Constant.LOG_TAG, this.getClass().getSimpleName() + " onMultiModeItemSelected count: " + count);
	}

	private void updateActionModeMenu(int select_count, boolean todoEnabled, boolean todonewEnabled, boolean htmlEnabled, boolean everEnabled) {
		if(ActionModeMenuItemComplete != null) {
			ActionModeMenuItemComplete.setEnabled(todoEnabled);
			ActionModeMenuItemComplete.setVisible(todoEnabled);
			if(todoEnabled) {
				ActionModeMenuItemComplete.setIcon(R.drawable.btn_check_on_holo_light);
			} else {
				ActionModeMenuItemComplete.setIcon(R.drawable.btn_check_on_disabled_holo_light);
			}
		}
		if(ActionModeMenuItemToInComplete != null) {
			ActionModeMenuItemToInComplete.setEnabled(todonewEnabled);
			ActionModeMenuItemToInComplete.setVisible(todonewEnabled);
			if(todonewEnabled) {
				ActionModeMenuItemToInComplete.setIcon(R.drawable.btn_check_off_holo_light);
			} else {
				ActionModeMenuItemToInComplete.setIcon(R.drawable.btn_check_off_disabled_holo_light);
			}
		}
		if(ActionModeMenuItemRefreshThumbnail != null) {
			ActionModeMenuItemRefreshThumbnail.setEnabled(htmlEnabled);
			ActionModeMenuItemRefreshThumbnail.setVisible(htmlEnabled);
			if(htmlEnabled) {
				ActionModeMenuItemRefreshThumbnail.setIcon(R.drawable.ic_menu_refresh_thumbnail);
			} else {
				ActionModeMenuItemRefreshThumbnail.setIcon(R.drawable.ic_menu_disabled_refresh_thumbnail);
			}
		}
		if(ActionModeMenuItemShortCut != null) {
			if(select_count > 1) {
				ActionModeMenuItemShortCut.setEnabled(false);
				ActionModeMenuItemShortCut.setVisible(false);
				ActionModeMenuItemShortCut.setIcon(R.drawable.ic_menu_disabled_shortcut);
			} else {
				ActionModeMenuItemShortCut.setIcon(R.drawable.ic_menu_shortcut);
				ActionModeMenuItemShortCut.setEnabled(true);
				ActionModeMenuItemShortCut.setVisible(true);
			}
		}
		if(ActionModeMenuItemEvernote != null) {
			if(session.isLoggedIn()) {
				ActionModeMenuItemEvernote.setEnabled(everEnabled);
				ActionModeMenuItemEvernote.setVisible(everEnabled);
			}
		}
	}

	@Override
	public void onDeleteItemDialogResult(int result) {
		boolean memo = isItemSelected(DreamNoteProvider.ITEMTYPE_MEMO);
		boolean photo = isItemSelected(DreamNoteProvider.ITEMTYPE_PHOTO);
		boolean todo = isItemSelected(DreamNoteProvider.ITEMTYPE_TODO);
		boolean todonew = isItemSelected(DreamNoteProvider.ITEMTYPE_TODONEW);
		boolean html = isItemSelected(DreamNoteProvider.ITEMTYPE_HTML);
		String[] ItemType = new String[]{
				getResources().getString(R.string.itemtype_memo),
				getResources().getString(R.string.itemtype_photomemo),
				getResources().getString(R.string.itemtype_todo),
				getResources().getString(R.string.itemtype_todonew),
				getResources().getString(R.string.itemtype_clip)
		};
		if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) == false) {
			if(result == DialogInterface.BUTTON_POSITIVE) {
				result = DialogInterface.BUTTON_NEUTRAL;
			}
		}
		switch(result) {
		case DialogInterface.BUTTON_POSITIVE:
			if(photo || html) {
				DeleteItemTaskFragment f = new DeleteItemTaskFragment();
				f.registerFragment(this, DeleteItemTaskFragment.TAG);
				f.setSelectedItems(SelectedItems);
				f.startLoader();
			}
		case DialogInterface.BUTTON_NEUTRAL:
			ArrayList<String> uris = new ArrayList<String>();
			if(memo) {
				uris.add("memos");
			}
			if(photo) {
				uris.add("photos");
			}
			if(todo || todonew) {
				uris.add("todos");
			}
			if(html) {
				uris.add("htmls");
			}
			String strUris = uris.toString();
			ArrayList<String> ids = new ArrayList<String>();
			Iterator<Item> iter = SelectedItems.iterator();
			while(iter.hasNext()) {
				Item item = (Item) iter.next();
				ids.add(String.valueOf(item.id));
			}
			String strIds = ids.toString();
			String[] whereArgs = new String[] {
					strIds.substring(1, strIds.length() - 1),
					strUris.substring(1, strUris.length() - 1)
			};
			getContentResolver().delete(DreamNoteProvider.ITEMS_CONTENT_URI, null, whereArgs);
			String strResult;
			if(memo && photo && (todo || todonew) && html) {
				strResult = getResources().getString(R.string.item_delete_alltype_result);
			} else if(memo && photo && html) {
				strResult = getResources().getString(R.string.item_delete_memo_and_image_and_clip_result);
			} else if(memo && (todo || todonew) && html) {
				strResult = getResources().getString(R.string.item_delete_memo_and_todo_and_clip_result);
			} else if(memo && (todo || todonew) && photo) {
				strResult = getResources().getString(R.string.item_delete_memo_and_image_and_todo_result);
			} else if(photo && (todo || todonew) && html) {
				strResult = getResources().getString(R.string.item_delete_image_and_todo_and_clip_result);
			} else if(memo && (todo || todonew)) {
				strResult = getResources().getString(R.string.item_delete_memo_and_todo_result);
			} else if(memo && photo) {
				strResult = getResources().getString(R.string.item_delete_memo_and_image_result);
			} else if(memo && html) {
				strResult = getResources().getString(R.string.item_delete_memo_and_clip_result);
			} else if((todo || todonew) && photo) {
				strResult = getResources().getString(R.string.item_delete_todo_and_image_result);
			} else if((todo || todonew) && html) {
				strResult = getResources().getString(R.string.item_delete_todo_and_clip_result);
			} else if(photo && html) {
				strResult = getResources().getString(R.string.item_delete_image_and_clip_result);
			} else {
				if(SelectedItems.size() == 1) {
					Item item = SelectedItems.get(0);
					strResult = ItemType[item.datatype] + "\n" + item.title + getResources().getString(R.string.item_delete_msg_foot);
				} else {
					if(todo || todonew) {
						strResult = getResources().getString(R.string.item_delete_todo_result);
					} else if(photo) {
						strResult = getResources().getString(R.string.item_delete_image_result);
					} else if(html) {
						strResult = getResources().getString(R.string.item_delete_clip_result);
					} else {
						strResult = getResources().getString(R.string.item_delete_memo_result);
					}
				}
			}
			Toast.makeText(getApplicationContext(), strResult, Toast.LENGTH_LONG).show();
			break;
		}

	}

	private final class MultipleChoiceMode implements ActionMode.Callback {
		private DeleteItemDialogResultListener mDeleteItemDialogResultListener;

		@Override
		public boolean onCreateActionMode(ActionMode mode, Menu menu) {
			ActionModeMenuItemComplete = null;
			ActionModeMenuItemToInComplete = null;
			ActionModeMenuItemRefreshThumbnail = null;
			ActionModeMenuItemShortCut = null;
			ActionModeMenuItemEvernote = null;
			switch(currentpager_position) {
			case TAB_INDEX_ALL:
				getSupportMenuInflater().inflate(R.menu.actionmode_all_menu, menu);
				ActionModeMenuItemComplete = menu.findItem(R.id.actionmode_complete);
				ActionModeMenuItemToInComplete = menu.findItem(R.id.actionmode_toincomplete);
				ActionModeMenuItemRefreshThumbnail = menu.findItem(R.id.actionmode_refresh_thumbnail);
				ActionModeMenuItemShortCut = menu.findItem(R.id.actionmode_shortcut);
				ActionModeMenuItemEvernote = menu.findItem(R.id.actionmode_evernote);
				break;
			case TAB_INDEX_MEMO:
				getSupportMenuInflater().inflate(R.menu.actionmode_memo_menu, menu);
				ActionModeMenuItemShortCut = menu.findItem(R.id.actionmode_shortcut);
				ActionModeMenuItemEvernote = menu.findItem(R.id.actionmode_evernote);
				break;
			case TAB_INDEX_PHOTO:
				getSupportMenuInflater().inflate(R.menu.actionmode_photo_menu, menu);
				ActionModeMenuItemShortCut = menu.findItem(R.id.actionmode_shortcut);
				ActionModeMenuItemEvernote = menu.findItem(R.id.actionmode_evernote);
				break;
			case TAB_INDEX_TODO:
				getSupportMenuInflater().inflate(R.menu.actionmode_todo_menu, menu);
				ActionModeMenuItemComplete = menu.findItem(R.id.actionmode_complete);
				ActionModeMenuItemToInComplete = menu.findItem(R.id.actionmode_toincomplete);
				ActionModeMenuItemShortCut = menu.findItem(R.id.actionmode_shortcut);
				ActionModeMenuItemEvernote = menu.findItem(R.id.actionmode_evernote);
				break;
			case TAB_INDEX_HTML:
				getSupportMenuInflater().inflate(R.menu.actionmode_html_menu, menu);
				ActionModeMenuItemShortCut = menu.findItem(R.id.actionmode_shortcut);
				ActionModeMenuItemEvernote = menu.findItem(R.id.actionmode_evernote);
				break;
			}
			if(ActionModeMenuItemEvernote != null) {
				if(session.isLoggedIn() == false) {
					ActionModeMenuItemEvernote.setEnabled(false);
					ActionModeMenuItemEvernote.setVisible(false);
				}
			}
			return true;
		}

		public void setDeleteItemDialogResultListener(DeleteItemDialogResultListener listener) {
			mDeleteItemDialogResultListener = listener;
		}

		@Override
		public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
			return false;
		}

		@Override
		public boolean onActionItemClicked(ActionMode mode, MenuItem menuitem) {
			//Log.d(Constant.LOG_TAG, "onActionItemClicked currentpager: " + currentpager_position);

			Context context = getApplicationContext();

			switch(menuitem.getItemId()) {
			case R.id.actionmode_delete:
				onActionItemClickedInit();

				boolean photo = isItemSelected(DreamNoteProvider.ITEMTYPE_PHOTO);
				boolean html = isItemSelected(DreamNoteProvider.ITEMTYPE_HTML);
				DeleteItemDialogFragment f = new DeleteItemDialogFragment();
				Bundle args = new Bundle();
				if(photo && html) {
					args.putString("message", getResources().getString(R.string.item_delete_confirm_clip_and_photo_extend_data_msg));
				} else if(photo) {
					args.putString("message", getResources().getString(R.string.item_delete_confirm_photo_extend_data_msg));
				} else if(html) {
					args.putString("message", getResources().getString(R.string.item_delete_confirm_clip_extend_data_msg));
				}
				f.setArguments(args);
				f.setResultListener(mDeleteItemDialogResultListener);
				f.show(getSupportFragmentManager(), "DeleteItemDialog");
				break;
			case R.id.actionmode_shortcut:
				if(SelectedItems.size() == 0) { return false; }
				onActionItemClickedInit();
				/**
				 * コンテンツショートカットアイコンID
				 */
				int[] shortcuticonid = {
						R.drawable.menu_memo,
						R.drawable.photos2,
						R.drawable.todo_done_64,
						R.drawable.menu_todo,
						R.drawable.artbook
				};
				if(ic == null) {
					ic = new DreamImageCache();
					DisplayMetrics metrics = getResources().getDisplayMetrics();
					ic.initialize(context, metrics, false);
				} else if(ic.isInitialized() == false) {
					DisplayMetrics metrics = context.getResources().getDisplayMetrics();
					ic.initialize(context, metrics, false);
				}

				Item iitem = SelectedItems.get(0);
				Intent shortcut = new Intent(Intent.ACTION_VIEW);
				shortcut.setClassName(context, Notes.class.getName());
				shortcut.putExtra("itemid", String.valueOf(iitem.id));
				// ショートカットをHOMEに作成する
				Intent intent = new Intent();
				intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcut);
				intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, iitem.title);
				if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
					if(iitem.datatype == DreamNoteProvider.ITEMTYPE_HTML) {
						String appName = getResources().getString(R.string.app_name);
						final String clipfilepath = Environment.getExternalStorageDirectory().toString() + "/" + appName + "/.clip/" + iitem.path;
						final String thumbnailpath = clipfilepath + "/thumbnail.png";
						File thumbnailfile = new File(thumbnailpath);
						if(thumbnailfile.exists() == false) {
							Parcelable iconResource = Intent.ShortcutIconResource.fromContext(context, shortcuticonid[iitem.datatype]);
							intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
						} else {
							Bitmap bm = ic.get(thumbnailpath, null, null, true, null, null);
							Bitmap scaledbitmap = Bitmap.createScaledBitmap(bm, 128, 128, true);
							intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, scaledbitmap);
						}
					} else if(iitem.datatype == DreamNoteProvider.ITEMTYPE_PHOTO) {
						File photofile = new File(iitem.path);
						if(photofile.exists() == false) {
							Parcelable iconResource = Intent.ShortcutIconResource.fromContext(context, shortcuticonid[iitem.datatype]);
							intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
						} else {
							Bitmap bm = ic.get(iitem.path, null, null, true, null, null);
							if(bm != null) {
								Bitmap scaledbitmap = Bitmap.createScaledBitmap(bm, 128, 128, true);
								intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, scaledbitmap);
							} else {
								Parcelable iconResource = Intent.ShortcutIconResource.fromContext(context, shortcuticonid[iitem.datatype]);
								intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
							}
						}
					} else {
						Parcelable iconResource = Intent.ShortcutIconResource.fromContext(context, shortcuticonid[iitem.datatype]);
						intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
					}
				} else {
					Parcelable iconResource = Intent.ShortcutIconResource.fromContext(context, shortcuticonid[iitem.datatype]);
					intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
				}
				intent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
				sendBroadcast(intent);
				break;
			case R.id.actionmode_refresh_thumbnail:
				CursorLoaderListFragment frag = (CursorLoaderListFragment)getFragmentAt(currentpager_position);
				frag.clearSelection();
				mTabsPager.setPagingEnabled(true, currentpager_position);
				frag.setChoiceMode(ListView.CHOICE_MODE_NONE);

				if(ic == null) {
					ic = new DreamImageCache();
					DisplayMetrics metrics = getResources().getDisplayMetrics();
					ic.initialize(context, metrics, false);
				} else if(ic.isInitialized() == false) {
					DisplayMetrics metrics = context.getResources().getDisplayMetrics();
					ic.initialize(context, metrics, false);
				}
				String appName = getResources().getString(R.string.app_name);
				Iterator<Item> iter = SelectedItems.iterator();
				while(iter.hasNext()) {
					Item item = iter.next();
					String clipfilepath = Environment.getExternalStorageDirectory().toString() + "/" + appName + "/.clip/" + item.path;
					String thumbnailpath = clipfilepath + "/thumbnail.png";
					ic.remove(thumbnailpath);
					File thumbnailfile = new File(thumbnailpath);
					if(thumbnailfile.exists()) {
						thumbnailfile.delete();
					}
				}
				frag.refreshList();
				break;
			case R.id.actionmode_complete:
				onActionItemClickedInit();

				Iterator<Item> todoiter = SelectedItems.iterator();
				while(todoiter.hasNext()) {
					Item item = todoiter.next();
					item.datatype = DreamNoteProvider.ITEMTYPE_TODO;
		    		Date now = new Date();
		    		item.long_date = now.getTime();
		        	ContentValues values = new ContentValues();
		    		values.put(ItemsSchema.COLUMN_ID, item.id);
		        	values.put(ItemsSchema.DATATYPE, item.datatype);
		        	values.put(ItemsSchema.DATE, item.long_date);
		        	values.put(ItemsSchema.UPDATED, item.long_updated);
		        	values.put(ItemsSchema.CREATED, item.long_created);
		        	values.put(ItemsSchema.TITLE, item.title);
		        	values.put(ItemsSchema.CONTENT, item.content);
		        	values.put(ItemsSchema.DESCRIPTION, item.description);
		        	values.put(ItemsSchema.PATH, item.path);
		        	values.put(ItemsSchema.RELATED, item.related);
		        	values.put(ItemsSchema.CREATED, item.long_created);
		        	values.put(ItemsSchema.TAGS, item.tags);
		    		Uri uri = ContentUris.withAppendedId(DreamNoteProvider.ITEMS_CONTENT_URI, item.id);
		    		getContentResolver().update(uri, values, null, null);
				}
				break;
			case R.id.actionmode_toincomplete:
				onActionItemClickedInit();
				Iterator<Item> todonewiter = SelectedItems.iterator();
				while(todonewiter.hasNext()) {
					Item item = todonewiter.next();
					item.datatype = DreamNoteProvider.ITEMTYPE_TODONEW;
		    		Date now = new Date();
		    		item.long_date = now.getTime();
		        	ContentValues values = new ContentValues();
		    		values.put(ItemsSchema.COLUMN_ID, item.id);
		        	values.put(ItemsSchema.DATATYPE, item.datatype);
		        	values.put(ItemsSchema.DATE, item.long_date);
		        	values.put(ItemsSchema.UPDATED, item.long_updated);
		        	values.put(ItemsSchema.CREATED, item.long_created);
		        	values.put(ItemsSchema.TITLE, item.title);
		        	values.put(ItemsSchema.CONTENT, item.content);
		        	values.put(ItemsSchema.DESCRIPTION, item.description);
		        	values.put(ItemsSchema.PATH, item.path);
		        	values.put(ItemsSchema.RELATED, item.related);
		        	values.put(ItemsSchema.CREATED, item.long_created);
		        	values.put(ItemsSchema.TAGS, item.tags);
		    		Uri uri = ContentUris.withAppendedId(DreamNoteProvider.ITEMS_CONTENT_URI, item.id);
		    		getContentResolver().update(uri, values, null, null);
				}
				break;
			case R.id.actionmode_evernote:
				if(SelectedItems.size() == 0) { return false; }
				Item single_item = SelectedItems.get(0);
				if(single_item.datatype == DreamNoteProvider.ITEMTYPE_HTML) {
					return false;
				}
				if(ActionModeMenuItemEvernote.hasSubMenu()) {
					ActionModeMenuItemEvernote.getSubMenu().clear();
				}
				onActionItemClickedInit();
				Toast.makeText(context, R.string.please_wait, Toast.LENGTH_LONG).show();
				Intent everintent = new Intent(context, DreamPostEnmlService.class);
				int size = SelectedItems.size();
				if(size == 1) {
					everintent.putExtra("item", single_item);
					context.startService(everintent);
				} else {
					ArrayList<String> content = new ArrayList<String>();
					Iterator<Item> everiter = SelectedItems.iterator();
					while(everiter.hasNext()) {
						Item item = everiter.next();
						content.add(item.title);
						content.add(item.content);
						content.add(String.valueOf(item.datatype));
						content.add(item.tags);
					}
					everintent.putExtra("arritem", content);
//					everintent.putExtra("title", value)
					context.startService(everintent);
				}
				break;
			case R.id.actionmode_evernote_simple:
				onActionItemClickedInit();

				Toast.makeText(context, R.string.please_wait, Toast.LENGTH_LONG).show();
				Item simple_item = SelectedItems.get(0);
				Intent everintent_simple = new Intent(context, DreamPostEnmlService.class);
				everintent_simple.putExtra("item", simple_item);
				everintent_simple.putExtra("mode", false);
				context.startService(everintent_simple);
				break;
			case R.id.actionmode_evernote_style:
				onActionItemClickedInit();

				Toast.makeText(context, R.string.please_wait, Toast.LENGTH_LONG).show();
				Item style_item = SelectedItems.get(0);
				Intent everintent_style = new Intent(context, DreamPostEnmlService.class);
				everintent_style.putExtra("item", style_item);
				everintent_style.putExtra("mode", true);
				context.startService(everintent_style);
				break;
			}

			mode.finish();
			return true;
		}

		private void onActionItemClickedInit() {
			CursorLoaderListFragment frag = (CursorLoaderListFragment)getFragmentAt(currentpager_position);
			frag.clearSelection();

			mTabsPager.setPagingEnabled(true, currentpager_position);

			frag.setChoiceMode(ListView.CHOICE_MODE_NONE);
		}

		@Override
		public void onDestroyActionMode(ActionMode mode) {
			CursorLoaderListFragment frag = (CursorLoaderListFragment)getFragmentAt(currentpager_position);
			frag.clearSelection();
			mTabsPager.setPagingEnabled(true, currentpager_position);

			frag.setChoiceMode(ListView.CHOICE_MODE_NONE);
		}
	}

	@Override
	public void onRemoveRequest(String tag) {
		Fragment fragment = getSupportFragmentManager().findFragmentByTag(tag);
		if(fragment != null) {
			try {
				getSupportFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
				getSupportFragmentManager().executePendingTransactions();
			} catch(IllegalStateException ie) {

			}
		}
	}

	@Override
	public EvernoteSession getEvernoteSession() {
		if(session != null) {
			return session;
		}
		return null;
	}

	@Override
	public void setEvernoteSession(EvernoteSession session) {
		if(session != null) {
			this.session = session;
		}
	}
}