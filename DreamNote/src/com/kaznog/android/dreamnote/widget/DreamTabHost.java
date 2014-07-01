package com.kaznog.android.dreamnote.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TabHost;

public class DreamTabHost extends TabHost {

	public DreamTabHost(Context context) {
		super(context);
	}

	public DreamTabHost(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

    @Override
    public void dispatchWindowFocusChanged(boolean hasFocus) {
        if (getCurrentView() != null){
            super.dispatchWindowFocusChanged(hasFocus);
        }
    }
}
