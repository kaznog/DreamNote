package com.kaznog.android.dreamnote.widget;

import com.actionbarsherlock.internal.view.menu.MenuBuilder;
import com.actionbarsherlock.view.MenuItem;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.listener.TagListItemListener;
import com.kaznog.android.dreamnote.widget.SearchView.OnQueryTextListener;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class TagSelectorView extends LinearLayout implements TagListItemListener {
	private ImageView mTagButton;
	private MenuBuilder mMenu;
	private String[] arrTags;
	private TagSelectorPopUpView tagpopup;
	private TagListItemListener mTagListItemListener;
	private OnQueryTextListener mOnQueryChangeListener;
	public TagSelectorView(Context context) {
		this(context, null);
	}

	public TagSelectorView(Context context, AttributeSet attrs) {
		this(context, attrs, 0);
	}

	public TagSelectorView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs);
		LayoutInflater.from(context).inflate(R.layout.actionbar_tagselector, this, true);

		mTagButton = (ImageView) findViewById(R.id.actionbar_tagselect_btn);
		mTagButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
	           	if(mMenu != null) {
            		if(mMenu.size() > 0) {
	            		if(tagpopup != null && tagpopup.isShowing()) {
	            			tagpopup.dismiss();
	            			tagpopup = null;
	            		} else {
			        		tagpopup = new TagSelectorPopUpView(getContext(), mMenu, mTagButton);
			        		tagpopup.setTagListItemListener(mTagListItemListener);
			        		tagpopup.show();
	            		}
            		}
            	}
 			}
		});
		mTagButton.setOnLongClickListener(mOnLongClickListener);
		mTagListItemListener = this;
	}

	private OnLongClickListener mOnLongClickListener = new OnLongClickListener() {

		@Override
		public boolean onLongClick(View arg0) {
			boolean isChecked = false;
			int TagIconId = R.drawable.ic_menu_tag;
			if(mMenu != null && mMenu.size() > 0) {
				int size = mMenu.size();
				for(int i = 0; i < size; i++) {
					MenuItem item = mMenu.getItem(i);
					if(item.isChecked()) {
						isChecked = true;
					}
					item.setChecked(false);
				}
			}
			mTagButton.setImageResource(TagIconId);
			arrTags = new String[0];
			if(mOnQueryChangeListener != null) {
				mOnQueryChangeListener.onQueryTagsChange(arrTags);
			}
			if(tagpopup != null && tagpopup.isShowing()) {
				tagpopup.setTagList(mMenu);
			}
			if(isChecked == false) {
		        final int[] screenPos = new int[2];
		        final Rect displayFrame = new Rect();
		        getLocationOnScreen(screenPos);
		        getWindowVisibleDisplayFrame(displayFrame);

		        final Context context = getContext();
		        final int width = getWidth();
		        final int height = getHeight();
		        final int midy = screenPos[1] + height / 2;
		        final int screenWidth = context.getResources().getDisplayMetrics().widthPixels;

		        Toast cheatSheet = Toast.makeText(context, R.string.tagselector_description, Toast.LENGTH_SHORT);
		        if (midy < displayFrame.height()) {
		            // Show along the top; follow action buttons
		            cheatSheet.setGravity(Gravity.TOP | Gravity.RIGHT,
		                    screenWidth - screenPos[0] - width / 2, height);
		        } else {
		            // Show along the bottom center
		            cheatSheet.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, height);
		        }
		        cheatSheet.show();
			}
			return true;
		}

	};

	private void updateTagIcon() {
		int TagIconId = R.drawable.ic_menu_tag;
		if(mMenu != null && mMenu.size() > 0) {
			int size = mMenu.size();
			boolean res = false;
			for(int i = 0; i < size; i++) {
				if(mMenu.getItem(i).isChecked()) {
					res = true;
					break;
				}
			}
			if(res) {
				TagIconId = R.drawable.ic_menu_tag_red;
			}
		}
		mTagButton.setImageResource(TagIconId);
	}

	public void setTagList(MenuBuilder menu) {
		mMenu = menu;
		updateTagIcon();
		if(tagpopup != null && tagpopup.isShowing()) {
			tagpopup.setTagList(menu);
		}
	}

	@Override
	public void onTagListItem(String[] arrTags) {
		this.arrTags = arrTags;
		if(tagpopup != null && tagpopup.isShowing()) {
			mMenu = tagpopup.getMenu();
			updateTagIcon();
		}
		if(mOnQueryChangeListener != null) {
			mOnQueryChangeListener.onQueryTagsChange(arrTags);
		}
	}

    public void setOnQueryTextListener(OnQueryTextListener listener) {
        mOnQueryChangeListener = listener;
    }
}
