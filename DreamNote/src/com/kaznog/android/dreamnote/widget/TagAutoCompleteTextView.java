package com.kaznog.android.dreamnote.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.widget.MultiAutoCompleteTextView;

public class TagAutoCompleteTextView extends MultiAutoCompleteTextView {
	public interface TagAutoCompleteTextViewListener {
		void onImeHidden();
	}
	private TagAutoCompleteTextViewListener mTagAutoCompleteTextViewListener;
	public void setTagAutoCompleteTextViewListener(TagAutoCompleteTextViewListener listener) {
		mTagAutoCompleteTextViewListener = listener;
	}
	public TagAutoCompleteTextView(Context context) {
		this(context, null);
	}
	public TagAutoCompleteTextView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}
	public TagAutoCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);
		mTagAutoCompleteTextViewListener = null;
	}

    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
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
                	if(mTagAutoCompleteTextViewListener != null) {
                		mTagAutoCompleteTextViewListener.onImeHidden();
                	}
                	return true;
                }
    		}
    	}
    	return super.onKeyPreIme(keyCode, event);
    }
}
