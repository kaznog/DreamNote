package com.kaznog.android.dreamnote.widget;

import com.kaznog.android.dreamnote.util.AppInfo;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

public class DreamNoteEditText extends EditText {
	private Context context;
	public interface DreamNoteEditTextListener {
		void onEditTextImeHidden();
	}
	private DreamNoteEditTextListener mDreamNoteEditTextListener;
	public void setDreamNoteEditTextListener(DreamNoteEditTextListener listener) {
		mDreamNoteEditTextListener = listener;
	}
	public DreamNoteEditText(Context context) {
		this(context, null);
	}
	public DreamNoteEditText(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	public DreamNoteEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);
		this.context = context;
		mDreamNoteEditTextListener = null;
	}
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
    	AppInfo.DebugLog(context, "DreamNoteEditText onKeyDown keyCode:" + keyCode);
    	int action = event.getAction();
    	AppInfo.DebugLog(context, "DreamNoteEditText onKeyDown action:" + action);

		return super.onKeyDown(keyCode, event);
	}
    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
    	AppInfo.DebugLog(context, "DreamNoteEditText onKeyPreIme keyCode:" + keyCode);
    	int action = event.getAction();
    	AppInfo.DebugLog(context, "DreamNoteEditText onKeyPreIme action:" + action);

    	if (keyCode == KeyEvent.KEYCODE_BACK) {
    		if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                KeyEvent.DispatcherState state = getKeyDispatcherState();
                if (state != null) {
                    state.startTracking(event, this);
                }
                return true;
    		}  else if (event.getAction() == KeyEvent.ACTION_UP) {
                KeyEvent.DispatcherState state = getKeyDispatcherState();
                if (state != null) {
                    state.handleUpEvent(event);
                }
                if (event.isTracking() && !event.isCanceled()) {
                	if(mDreamNoteEditTextListener != null) {
                		mDreamNoteEditTextListener.onEditTextImeHidden();
                	}
                	return true;
                }
    		}
    	} else if(keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
    		return false;
    	}
    	return super.onKeyPreIme(keyCode, event);
    }
}
