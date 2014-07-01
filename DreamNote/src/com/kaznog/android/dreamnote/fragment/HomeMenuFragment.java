package com.kaznog.android.dreamnote.fragment;

import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;

import com.actionbarsherlock.app.SherlockListFragment;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.listener.OnFragmentControlListener;

public abstract class HomeMenuFragment extends SherlockListFragment
implements
OnTouchListener {
	protected boolean mDisableFragmentAnimations;
	protected abstract View createView(LayoutInflater inflater, ViewGroup container);

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		final View contentView = createView(inflater, container);
		// 画面の右半分を占める透明なビューを取得する
		final View view = contentView.findViewById(android.R.id.background);
		if(view == null) {
			throw new NullPointerException("View occupies the right half on the screen does not exist");
		}
		view.setOnTouchListener(this);
		return contentView;
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		if(savedInstanceState == null) {
			mDisableFragmentAnimations = false;
		} else {
			mDisableFragmentAnimations = savedInstanceState.getBoolean("mDisableFragmentAnimations");
		}
		setHasOptionsMenu(true);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean("mDisableFragmentAnimations", mDisableFragmentAnimations);
		setUserVisibleHint(true);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
    	return super.onOptionsItemSelected(item);
    }

	@Override
	public boolean onTouch(View view, MotionEvent event) {
		if(isAvailable()) {
			if(event.getAction() == MotionEvent.ACTION_DOWN) {
				onAbort();
				return true;
			}
		}
		return false;
	}

	/**
	  * フラグメントの表示、非表示を切り替える。
	  * このメソッドはマネージャにこのフラグメントを追加した後に呼ぶ
	  *
	  * @param ft トランザクション
	  */
	public void toggleVisibility(FragmentTransaction ft) {
		if(!isDetached()) {
			ft.detach(this);
		} else {
			ft.attach(this);
		}
	}
	/**
	  * キーが押下された時に呼ばれる処理。
	  * このメソッドを呼ぶ前に、二重押下やエラー防止のため以下のようなチェックを必要とする。
	  * <code>
	  * if (fragment != null && fragment.isAvailable()) { }
	  * </code>
	  * @param keyCode
	  * @param event
	  * @return
	  */
/*
	public boolean onKeyDown(int KeyCode, KeyEvent event) {
		if(event.getAction() == KeyEvent.ACTION_DOWN
		&& event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
			onAbort();
			return true;
		}
		return false;
	}
*/
	/**
	  * フラグメントが利用できるかどうか。
	  * フラグメントが表示されているかつ、取り外されていない状態かどうか
	  *
	  * @return 利用できるかどうか
	  */
	public boolean isAvailable() {
		return isVisible() && !isDetached();
	}

	/**
	  * フラグメントを画面から消す
	  */
	public void hide() {
		final FragmentTransaction ft = getFragmentManager().beginTransaction();
//		ft.setCustomAnimations(R.anim.fragment_slide_in, R.anim.fragment_slide_out).detach(this).commit();
		ft.setCustomAnimations(R.anim.fragment_slide_in, R.anim.fragment_slide_out).remove(this).commit();
	}

	@Override
	public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
		Animation anim;
		if(enter) {
			anim = AnimationUtils.loadAnimation(getSherlockActivity(), R.anim.fragment_slide_in);
		} else {
			if(mDisableFragmentAnimations) {
				return null;
/*
				anim = new Animation() {};
				anim.setDuration(0);
*/
			} else {
				anim = AnimationUtils.loadAnimation(getSherlockActivity(), R.anim.fragment_slide_out);
			}
			anim.setAnimationListener(new AnimationListener() {

				@Override
				public void onAnimationEnd(Animation arg0) {
				}

				@Override
				public void onAnimationRepeat(Animation arg0) {
				}

				@Override
				public void onAnimationStart(Animation animation) {
				}

			});
		}
		return anim;
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

	public void onAbort() {
		FragmentTransaction ft = getSherlockActivity().getSupportFragmentManager().beginTransaction();
//		ft.setCustomAnimations(R.anim.fragment_slide_in, R.anim.fragment_slide_out).remove(this).commit();
		ft.remove(this);
		ft.commit();
		setResult(android.app.Activity.RESULT_CANCELED, null);

//		getSherlockActivity().supportInvalidateOptionsMenu();
	}

	/**
	  * トランザクションのヘルパーメソッド。
	  * このメソッドを呼んでトランザクションを開始すると、
	  * スライドイン、アウトのアニメーションを付加できる。
	  *
	  * @param activity フラグメントアクティビティ
	  * @return トランザクション
	  */
	public static FragmentTransaction beginTransaction(FragmentActivity activity) {
		final FragmentTransaction ft = activity.getSupportFragmentManager().beginTransaction();
		ft.setCustomAnimations(R.anim.fragment_slide_in, R.anim.fragment_slide_out);
		return ft;
	}
}
