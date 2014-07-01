package com.kaznog.android.dreamnote.widget;

import java.util.HashSet;
import java.util.Set;

import com.actionbarsherlock.internal.view.View_HasStateListenerSupport;
import com.actionbarsherlock.internal.view.View_OnAttachStateChangeListener;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ImageView;

public class PopUpAnchorImageView extends ImageView
implements
View_HasStateListenerSupport {

	private final Set<View_OnAttachStateChangeListener> mListeners = new HashSet<View_OnAttachStateChangeListener>();
	public PopUpAnchorImageView(Context context) {
		this(context, null);
	}

	public PopUpAnchorImageView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public PopUpAnchorImageView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);
	}

	@Override
	public void addOnAttachStateChangeListener(View_OnAttachStateChangeListener listener) {
		mListeners.add(listener);
	}

	@Override
	public void removeOnAttachStateChangeListener(View_OnAttachStateChangeListener listener) {
		mListeners.remove(listener);
	}

	@Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        for (View_OnAttachStateChangeListener listener : mListeners) {
            listener.onViewAttachedToWindow(this);
        }
 	}

	@Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        for (View_OnAttachStateChangeListener listener : mListeners) {
            listener.onViewDetachedFromWindow(this);
        }
 	}
}
