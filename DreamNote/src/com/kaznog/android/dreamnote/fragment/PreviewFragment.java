package com.kaznog.android.dreamnote.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

import com.actionbarsherlock.app.SherlockFragment;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.listener.OnFragmentControlListener;

public abstract class PreviewFragment extends SherlockFragment {
	protected long contentId;
	protected Item item;
	View mProgressContainer;
	View mContentContainer;
	public View createContentView(LayoutInflater inflater, int layoutid) {
		final Context context = getActivity();
        FrameLayout root = new FrameLayout(context);
        // ------------------------------------------------------------------

        LinearLayout pframe = new LinearLayout(context);
        pframe.setOrientation(LinearLayout.VERTICAL);
        pframe.setVisibility(View.GONE);
        pframe.setGravity(Gravity.CENTER);
        pframe.setBackgroundResource(android.R.color.background_light);

        ProgressBar progress = new ProgressBar(context, null,
                android.R.attr.progressBarStyleLarge);
        pframe.addView(progress, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        root.addView(pframe, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        mProgressContainer = pframe;
        // ------------------------------------------------------------------

//		View v = inflater.inflate(R.layout.preview_memo_frag, container, false);
//        mContentContainer = inflater.inflate(layoutid, root, true);
        mContentContainer = inflater.inflate(layoutid, null, false);

        root.addView(mContentContainer, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));

        root.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
        // ------------------------------------------------------------------
		return root;
	}

	public void setContentShown(boolean shown) {
		if(shown) {
			if(mContentContainer.getVisibility() == View.GONE) {
				mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
	                    getActivity(), android.R.anim.fade_out));
				mContentContainer.startAnimation(AnimationUtils.loadAnimation(
	                    getActivity(), android.R.anim.fade_in));
				mProgressContainer.setVisibility(View.GONE);
				mContentContainer.setVisibility(View.VISIBLE);
			}
		} else {
			if(mContentContainer.getVisibility() == View.VISIBLE) {
				mProgressContainer.startAnimation(AnimationUtils.loadAnimation(
	                    getActivity(), android.R.anim.fade_in));
				mContentContainer.startAnimation(AnimationUtils.loadAnimation(
	                    getActivity(), android.R.anim.fade_out));
				mProgressContainer.setVisibility(View.VISIBLE);
				mContentContainer.setVisibility(View.GONE);
			}
		}
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if(savedInstanceState != null) {
			//Log.d(Constant.LOG_TAG, getClass().getSimpleName() + " restore requestcode");
			requestCode = savedInstanceState.getInt("requestCode");
		}
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putInt("requestCode", requestCode);
		setUserVisibleHint(true);
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
	public abstract void onAbort();

    protected boolean isSplitActionbarIsNarrow() {
    	return this.getSherlockActivity().getResources().getBoolean(R.bool.split_action_bar_is_narrow);
    }
}
