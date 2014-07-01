package com.kaznog.android.dreamnote.listener;

import com.evernote.client.oauth.android.EvernoteSession;

import android.os.Bundle;
import android.support.v4.app.Fragment;

public interface OnFragmentControlListener {
	public void onAddNoteFragment(OnFragmentControlListener listener, Fragment fragment, int fragment_type, String tag);
	public void onFragmentResult(Fragment fragment, int requestCode, int resultCode, Bundle extra);
	public void onRemoveRequest(String tag);
	public EvernoteSession getEvernoteSession();
	public void setEvernoteSession(EvernoteSession session);
}
