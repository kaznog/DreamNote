package com.kaznog.android.dreamnote.fragment;

import android.annotation.SuppressLint;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

import com.actionbarsherlock.app.SherlockFragment;
import com.actionbarsherlock.view.MenuItem;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.listener.OnFragmentControlListener;

public class HelpFragment extends SherlockFragment {
	public static final String TAG = "HelpFragment";
	private WebView help_webview;
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.help_frag, container, false);
		return view;
	}
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		setupUI();
		setHasOptionsMenu(true);
	}
	private void setupUI() {
		help_webview = (WebView) getSherlockActivity().findViewById(R.id.help_webview);
		help_webview.loadUrl("file:///android_asset/clip_help.html");
	}
	@SuppressLint("NewApi")
	@Override
	public void onPause() {
		super.onPause();
		if(Build.VERSION.SDK_INT > 10) {
			if(help_webview != null) {
				help_webview.onPause();
			}
		}
	}
	@SuppressLint("NewApi")
	@Override
	public void onResume() {
		if(Build.VERSION.SDK_INT > 10) {
			if(help_webview != null) {
				help_webview.onResume();
			}
		}
		super.onResume();
	}
	@Override
	public void onDestroy() {
		if(help_webview != null) {
			help_webview.setWebChromeClient(null);
			help_webview.setWebViewClient(null);
			help_webview.clearView();
			help_webview.loadUrl("about:blank");
			help_webview.clearCache(true);
			help_webview.clearFormData();
			help_webview.clearHistory();
			help_webview = null;
		}
		super.onDestroy();
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
