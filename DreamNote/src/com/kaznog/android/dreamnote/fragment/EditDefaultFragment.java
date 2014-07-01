package com.kaznog.android.dreamnote.fragment;

import android.os.Bundle;

public abstract class EditDefaultFragment extends EditFragment {

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		// 表示メッセージ初期化
		setupMsg();

		if(savedInstanceState == null) {
			// UIの初期化
			setupUI(mContentContainer);
			Bundle args = getArguments();
			if(args != null) {
				mode = true;
				item = (Item)args.getSerializable("item");
			} else {
				mode = false;
				item = new Item();
			}
			initializeData();
			setItemData();
			getLoaderManager().initLoader(0, null, this);
			setContentShown(false);
		} else {
			onActivityCreatedRestore(savedInstanceState);
			// UIの初期化
			setupUI(mContentContainer);
			setItemData();
			if(mIsTagsLoaded == false) {
				getLoaderManager().initLoader(0, null, this);
				setContentShown(false);
			} else {
				setupTagCompleteText();
				setContentShown(true);
				setButtonEnable();
			}
		}
		setHasOptionsMenu(true);
	}
}
