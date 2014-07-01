package com.kaznog.android.dreamnote.widget;

import com.kaznog.android.dreamnote.util.AppInfo;
import com.kaznog.android.dreamnote.widget.DreamNoteEditText.DreamNoteEditTextListener;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.EditText;

public class DreamNoteTitleEditText extends EditText {
	private Context context;
	public interface DreamNoteTitleEditTextListener {
		void onTitleEditTextImeHidden();
	}
	private DreamNoteTitleEditTextListener mDreamNoteTitleEditTextListener;
	public void setDreamNoteTitleEditTextListener(DreamNoteTitleEditTextListener listener) {
		mDreamNoteTitleEditTextListener = listener;
	}
	public DreamNoteTitleEditText(Context context) {
		this(context, null);
	}
	public DreamNoteTitleEditText(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	public DreamNoteTitleEditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);
		this.context = context;
		mDreamNoteTitleEditTextListener = null;
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
                	if(mDreamNoteTitleEditTextListener != null) {
                		mDreamNoteTitleEditTextListener.onTitleEditTextImeHidden();
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
