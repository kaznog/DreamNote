package com.kaznog.android.dreamnote.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Checkable;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

public class CheckableLinearLayout extends LinearLayout implements Checkable {
	private boolean checked;
	private boolean mBroadcasting;
	private static final int[] CHECKED_STATE_SET = { android.R.attr.state_checked };
	private OnCheckedChangeListener mOnCheckedChangeListener;
	public static interface OnCheckedChangeListener {
		void onCheckedChanged(CheckableLinearLayout rowView, boolean isChecked);
	}
    public void setOnCheckedChangeListener(OnCheckedChangeListener listener) {
        mOnCheckedChangeListener = listener;
    }

	public CheckableLinearLayout(Context context) {
		super(context);
	}

	public CheckableLinearLayout(Context context, AttributeSet attrs) {
		super(context, attrs);
		// TODO 自動生成されたコンストラクター・スタブ
	}

	public CheckableLinearLayout(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	@Override
	public boolean isChecked() {
		return checked;
	}

	@Override
	public void setChecked(boolean checked) {
		if(this.checked != checked) {
			this.checked = checked;
			// 背景色を選択状態によって変更する
			refreshDrawableState();
			if (mBroadcasting) {
                return;
            }
			mBroadcasting = true;
			if (mOnCheckedChangeListener != null) {
				mOnCheckedChangeListener.onCheckedChanged(this, this.checked);
			}
			mBroadcasting = false;
		}
	}

	@Override
	public void toggle() {
		this.checked = !this.checked;
		refreshDrawableState();
	}

	@Override
	protected int[] onCreateDrawableState(int extraSpace) {
		final int[] drawableState = super.onCreateDrawableState(extraSpace + 1);
		if (isChecked()) {
			mergeDrawableStates(drawableState, CHECKED_STATE_SET);
		}
		return drawableState;
	}
}
