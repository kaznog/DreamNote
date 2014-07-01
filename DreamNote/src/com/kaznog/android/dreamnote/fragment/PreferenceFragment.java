package com.kaznog.android.dreamnote.fragment;

import java.io.File;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.evernote.client.conn.ApplicationInfo;
import com.evernote.client.oauth.android.AuthenticationResult;
import com.evernote.client.oauth.android.EvernoteSession;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.db.DreamNoteProvider;
import com.kaznog.android.dreamnote.dialogfragment.AbortDialogFragment;
import com.kaznog.android.dreamnote.listener.AbortDialogResultListener;
import com.kaznog.android.dreamnote.listener.OnFragmentControlListener;
import com.kaznog.android.dreamnote.settings.PreferencesUtil;
import com.kaznog.android.dreamnote.util.AppInfo;
import com.kaznog.android.dreamnote.util.Constant;

public class PreferenceFragment extends SherlockFragment
implements
View.OnClickListener,
AbortDialogResultListener {
	public static final String TAG = "PreferenceFragment";
	private boolean saved_pref_splash;
	private boolean saved_pref_largethumbnail;
	private boolean saved_pref_javascript;
	private boolean saved_pref_webpicforce;
	private boolean saved_pref_en_logined;
	private EvernoteSession session;
	private CheckedTextView pref_splash;
	private CheckedTextView pref_largethumbnail;
	private CheckedTextView pref_javascript;
	private CheckedTextView pref_webpicforce;
	private Button pref_enconfig;
	private Button pref_cliprecovery;
	private Context mContext;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.preference_frag, container, false);
		return view;
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mContext = this.getSherlockActivity().getApplicationContext();
		if(savedInstanceState == null) {
			String strsplash = PreferencesUtil.getPreferences(mContext, Constant.PREFS_SPLASH, "");
			saved_pref_splash = strsplash.equals("") ? false : true;
			String strlargethumbnail = PreferencesUtil.getPreferences(mContext, Constant.PREFS_LARGETHUMBNAIL, "");
			saved_pref_largethumbnail = strlargethumbnail.equals("") ? false : true;
			String strjavascript = PreferencesUtil.getPreferences(mContext, Constant.PREFS_JAVASCRIPT, "");
			saved_pref_javascript = strjavascript.equals("") ? false : true;
			String strwebpicforce = PreferencesUtil.getPreferences(mContext, Constant.PREFS_WEBCLIP_PIC_FORCE, "");
			saved_pref_webpicforce = strwebpicforce.equals("") ? false : true;
			PreferencesUtil.removePreferences(mContext, Constant.PREFS_ENID);
			PreferencesUtil.removePreferences(mContext, Constant.PREFS_ATTR);
		} else {
			saved_pref_splash = savedInstanceState.getBoolean("saved_pref_splash");
			saved_pref_largethumbnail = savedInstanceState.getBoolean("saved_pref_largethumbnail");
			saved_pref_javascript = savedInstanceState.getBoolean("saved_pref_javascript");
			saved_pref_webpicforce = savedInstanceState.getBoolean("saved_pref_webpicforce");
		}
		setupUI(mContext);
		setHasOptionsMenu(true);
	}
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("saved_pref_splash", saved_pref_splash);
		outState.putBoolean("saved_pref_largethumbnail", saved_pref_largethumbnail);
		outState.putBoolean("saved_pref_javascript", saved_pref_javascript);
		outState.putBoolean("saved_pref_webpicforce", saved_pref_webpicforce);
		setUserVisibleHint(true);
	}

	@Override
	public void onResume() {
		super.onResume();
		if(session != null) {
			if (!session.completeAuthentication(PreferencesUtil.getSharedPreferences(this.getSherlockActivity().getApplicationContext()))) {
			      // We only want to do this when we're resuming after authentication...
			//    Toast.makeText(this, "Evernote login failed", Toast.LENGTH_LONG).show();
			}
			updateUI();
		}
	}

	private void setupSession(Context context) {
		if(mFragmentControlListener != null) {
			session = mFragmentControlListener.getEvernoteSession();
			saved_pref_en_logined = session.isLoggedIn();
		} else {
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
		    	session = new EvernoteSession(info, PreferencesUtil.getSharedPreferences(getSherlockActivity().getApplicationContext()), getTempDir(context));
		    	saved_pref_en_logined = true;
		    } else {
		    	session = new EvernoteSession(info, getTempDir(context));
		    	saved_pref_en_logined = false;
		    }
		}
	    updateUI();
	}

	private void updateUI() {
		if(session.isLoggedIn()) {
			pref_enconfig.setText(R.string.preference_enconfig_log_out);
		} else {
			pref_enconfig.setText(R.string.preference_enconfig_log_in);
		}
	}

	private File getTempDir(Context context) {
		return new File(AppInfo.getAppPath(context) + "/.cache");
	}

	private void setupUI(Context context) {

		pref_splash = (CheckedTextView) getSherlockActivity().findViewById(R.id.pref_splash);
		pref_splash.setChecked(saved_pref_splash);
		pref_splash.setOnClickListener(this);
		pref_largethumbnail = (CheckedTextView) getSherlockActivity().findViewById(R.id.pref_largethumbnail);
		pref_largethumbnail.setChecked(saved_pref_largethumbnail);
		pref_largethumbnail.setOnClickListener(this);
		pref_javascript = (CheckedTextView) getSherlockActivity().findViewById(R.id.pref_javascript);
		pref_javascript.setChecked(saved_pref_javascript);
		pref_javascript.setOnClickListener(this);
		pref_webpicforce = (CheckedTextView) getSherlockActivity().findViewById(R.id.pref_webpic_force);
		pref_webpicforce.setChecked(saved_pref_webpicforce);
		pref_webpicforce.setOnClickListener(this);
		pref_cliprecovery = (Button) getSherlockActivity().findViewById(R.id.pref_cliprecovery);
		pref_cliprecovery.setOnClickListener(this);
		pref_enconfig = (Button) getSherlockActivity().findViewById(R.id.pref_enconfig);
		pref_enconfig.setOnClickListener(this);
		setupSession(context);
//		session.completeAuthentication(PreferencesUtil.getSharedPreferences(this.getSherlockActivity().getApplicationContext()));
//		saved_pref_en_logined = session.isLoggedIn();
	}

	public void startAuth(View view) {
		if (session.isLoggedIn()) {
			session.logOut(PreferencesUtil.getSharedPreferences(this.getSherlockActivity().getApplicationContext()));
		} else {
			if(Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
				Toast.makeText(getSherlockActivity().getApplicationContext(), R.string.preference_enconfig_browserhint, Toast.LENGTH_SHORT).show();
			}
			session.authenticate(this.getActivity());
		}
		updateUI();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	switch(item.getItemId()) {
    	case R.id.pref_frag_save:
			Context context = this.getSherlockActivity().getApplicationContext();
			AuthenticationResult result = session.getAuthenticationResult();
			if(result != null) {
				String strAuthToken = result.getAuthToken();
				String strNoteStoreUrl = result.getNoteStoreUrl();
				String strWebApiUrlPrefix = result.getWebApiUrlPrefix();
				int UserId = result.getUserId();
				PreferencesUtil.setPreferences(context, Constant.PREFS_EN_OAUTH_TOKEN, strAuthToken);
				PreferencesUtil.setPreferences(context, Constant.PREFS_EN_OAUTH_NOTE_STORE_URL, strNoteStoreUrl);
				PreferencesUtil.setPreferences(context, Constant.PREFS_EN_OAUTH_WEB_API_URL_PREFIX, strWebApiUrlPrefix);
				PreferencesUtil.setPreferences(context, Constant.PREFS_EN_OAUTH_ID, UserId);
			} else {
				PreferencesUtil.removePreferences(context, Constant.PREFS_EN_OAUTH_TOKEN);
				PreferencesUtil.removePreferences(context, Constant.PREFS_EN_OAUTH_NOTE_STORE_URL);
				PreferencesUtil.removePreferences(context, Constant.PREFS_EN_OAUTH_WEB_API_URL_PREFIX);
				PreferencesUtil.removePreferences(context, Constant.PREFS_EN_OAUTH_ID);
			}
			String strjavascript = pref_javascript.isChecked() ? Constant.PREFS_JAVASCRIPT_ACTIVE : "";
			PreferencesUtil.setPreferences(context, Constant.PREFS_JAVASCRIPT, strjavascript);
			String strsplash = pref_splash.isChecked() ? Constant.PREFS_SPLASH_ACTIVE : "";
			PreferencesUtil.setPreferences(context, Constant.PREFS_SPLASH, strsplash);
			String strlargethumbnail = pref_largethumbnail.isChecked() ? Constant.PREFS_LARGETHUMBNAIL_ACTIVE : "";
			PreferencesUtil.setPreferences(context, Constant.PREFS_LARGETHUMBNAIL, strlargethumbnail);
			String strwebclipforce = pref_webpicforce.isChecked() ? Constant.PREFS_WEBCLIP_PIC_FORCE_ACTIVE : "";
			PreferencesUtil.setPreferences(context, Constant.PREFS_WEBCLIP_PIC_FORCE, strwebclipforce);
    		setResult(android.app.Activity.RESULT_CANCELED, null);
			FragmentManager fm = getSherlockActivity().getSupportFragmentManager();
    		FragmentTransaction ft = fm.beginTransaction();
			ft.remove(this);
			HomeMenuListFragment hf = (HomeMenuListFragment) fm.findFragmentByTag(HomeMenuListFragment.TAG);
			if(hf != null) {
				ft.remove(hf);
			}
			ft.commit();
    		break;
    	case R.id.pref_frag_cancel:
    		onAbort();
    		break;
    	}
    	return true;
    }

	private boolean isChanged() {
		boolean onchange = false;
		if(pref_javascript.isChecked() != saved_pref_javascript
		|| pref_splash.isChecked() != saved_pref_splash
		|| pref_largethumbnail.isChecked() != saved_pref_largethumbnail
		|| pref_webpicforce.isChecked() != saved_pref_webpicforce
		|| saved_pref_en_logined != session.isLoggedIn()
		) {
			// 元の内容と変わっていた場合
			onchange = true;
		}
		return onchange;
	}

    public void onAbort() {
    	//変更されているか確認
    	if(isChanged()) {
			AbortDialogFragment f = new AbortDialogFragment();
			Bundle args = new Bundle();
			args.putBoolean("mode", true);
			args.putStringArray("aborttoastmsg", new String[] {getResources().getString(R.string.prefs_cancel_resultmsg), null});
			args.putStringArray("modealerttitle", new String[] {getResources().getString(R.string.prefs_cancel_alerttitle), null});
			args.putStringArray("abortalertmsgs", new String[] {getResources().getString(R.string.prefs_cancel_alertmsg), null});
			f.setArguments(args);
			f.setResultListener(this);
			f.show(getFragmentManager(), "AbortDialog");
    	} else {
    		//変更されていなかったら
			Toast.makeText(this.getSherlockActivity().getApplicationContext(), R.string.prefs_cancel_resultmsg, Toast.LENGTH_LONG).show();
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
	@Override
	public void onAbortDialogResult(int result) {
		if(result == android.app.Activity.RESULT_CANCELED) {
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
	}
	@Override
	public void onClick(View v) {
		if(v == pref_splash
		|| v == pref_largethumbnail
		|| v == pref_javascript
		|| v == pref_webpicforce) {
			CheckedTextView ckeckview = (CheckedTextView)v;
			if(ckeckview.isChecked()) {
				ckeckview.setChecked(false);
			} else {
				ckeckview.setChecked(true);
			}
		} else if(v == pref_cliprecovery) {
			this.getSherlockActivity().getContentResolver().insert(DreamNoteProvider.CLIPRECOVERY_URI, null);
		} else if(v == pref_enconfig) {
			startAuth(v);
		}
	}
}
