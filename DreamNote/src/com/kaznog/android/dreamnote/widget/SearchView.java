package com.kaznog.android.dreamnote.widget;

import com.actionbarsherlock.view.CollapsibleActionView;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.util.Constant;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SearchView extends LinearLayout
implements
CollapsibleActionView {
	private OnQueryTextListener mOnQueryChangeListener;
	private OnClickListener mOnSearchClickListener;
	private OnSearchViewCloseListener mOnSearchViewCloseListener;
//	private OnFocusChangeListener mOnQueryTextFocusChangeListener;

	private View mSearchButton;
    private View mSubmitButton;
    private View mSearchPlate;
    private View mSubmitArea;
    private View mSearchEditFrame;

    private SearchQueryEditText mQueryTextView;
    private ImageView mCloseButton;
    private ImageView mSearchHintIcon;
//    private int mCollapsedImeOptions;

    private boolean mIconifiedByDefault;
    private boolean mIconified;
    private boolean mSubmitButtonEnabled;
    private boolean mExpandedInActionView;
    private boolean mClearingFocus;

    private CharSequence mOldQueryText;
    @SuppressWarnings("unused")
	private CharSequence mUserQuery;
    private CharSequence mQueryHint;
    @SuppressWarnings("unused")
    private int mMaxWidth;

    private Runnable mShowImeRunnable = new Runnable() {
        public void run() {
            InputMethodManager imm = (InputMethodManager)
                    getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

            if (imm != null) {
                imm.showSoftInput(mQueryTextView, 0, null);
            }
        }
    };

    private Runnable mUpdateDrawableStateRunnable = new Runnable() {
        public void run() {
            updateFocusedState();
        }
    };

    /**
     * Callbacks for changes to the query text.
     */
    public interface OnQueryTextListener {

        /**
         * Called when the user submits the query. This could be due to a key press on the
         * keyboard or due to pressing a submit button.
         * The listener can override the standard behavior by returning true
         * to indicate that it has handled the submit request. Otherwise return false to
         * let the SearchView handle the submission by launching any associated intent.
         *
         * @param query the query text that is to be submitted
         *
         * @return true if the query has been handled by the listener, false to let the
         * SearchView perform the default action.
         */
        boolean onQueryTextSubmit(String query);

        /**
         * Called when the query text is changed by the user.
         *
         * @param newText the new content of the query text field.
         *
         * @return false if the SearchView should perform the default action of showing any
         * suggestions if available, true if the action was handled by the listener.
         */
        boolean onQueryTextChange(String newText);

        boolean onQueryTagsChange(String[] arrTags);
    }

    public interface OnSearchViewCloseListener {

        /**
         * The user is attempting to close the SearchView.
         *
         * @return true if the listener wants to override the default behavior of clearing the
         * text field and dismissing it, false otherwise.
         */
        boolean onSearchViewClose();
    }

	public SearchView(Context context) {
		this(context, null);
	}

	public SearchView(Context context, AttributeSet attrs) {
		super(context, attrs);
		LayoutInflater.from(context).inflate(R.layout.actionbar_searchview, this, true);
		// 各ビューをIDから取得する
		mSearchButton = findViewById(R.id.search_button);
		mQueryTextView = (SearchQueryEditText) findViewById(R.id.search_src_text);
		mQueryTextView.setSearchView(this);
		mQueryTextView.setImeOptions(EditorInfo.IME_ACTION_SEARCH);
        mSearchEditFrame = findViewById(R.id.search_edit_frame);
        mSearchPlate = findViewById(R.id.search_plate);
        mSubmitArea = findViewById(R.id.submit_area);
        mSubmitButton = findViewById(R.id.search_go_btn);
        mCloseButton = (ImageView) findViewById(R.id.search_close_btn);
        mSearchHintIcon = (ImageView) findViewById(R.id.search_mag_icon);

        mSearchButton.setOnClickListener(mOnClickListener);
        mCloseButton.setOnClickListener(mOnClickListener);
        mSubmitButton.setOnClickListener(mOnClickListener);

        mQueryTextView.addTextChangedListener(mTextWatcher);

//        updateViewsVisibility(true);
        updateViewsVisibility(mIconifiedByDefault);
        updateQueryHint();
	}

    /**
     * Sets the IME options on the query text field.
     *
     * @see TextView#setImeOptions(int)
     * @param imeOptions the options to set on the query text field
     *
     * @attr ref android.R.styleable#SearchView_imeOptions
     */
    public void setImeOptions(int imeOptions) {
        mQueryTextView.setImeOptions(imeOptions);
    }

    /**
     * Sets the input type on the query text field.
     *
     * @see TextView#setInputType(int)
     * @param inputType the input type to set on the query text field
     *
     * @attr ref android.R.styleable#SearchView_inputType
     */
    public void setInputType(int inputType) {
        mQueryTextView.setInputType(inputType);
    }

    /** @hide */
    @Override
    public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
        // Don't accept focus if in the middle of clearing focus
        if (mClearingFocus) return false;
        // Check if SearchView is focusable.
        if (!isFocusable()) return false;
/*
        // If it is not iconified, then give the focus to the text field
        if (!isIconified()) {
        	boolean result = mQueryTextView.requestFocus(direction, previouslyFocusedRect);
            if (result) {
                updateViewsVisibility(false);
            }
            return result;
        } else {
            return super.requestFocus(direction, previouslyFocusedRect);
        }
*/
        return super.requestFocus(direction, previouslyFocusedRect);
    }

    /** @hide */
    @Override
    public void clearFocus() {
    	//Log.d(Constant.LOG_TAG, "SearchView clearFocus");
        mClearingFocus = true;
        setImeVisibility(false);
        super.clearFocus();
        mQueryTextView.clearFocus();
        mClearingFocus = false;
    }

    /**
     * Sets a listener for user actions within the SearchView.
     *
     * @param listener the listener object that receives callbacks when the user performs
     * actions in the SearchView such as clicking on buttons or typing a query.
     */
    public void setOnQueryTextListener(OnQueryTextListener listener) {
        mOnQueryChangeListener = listener;
    }

    /**
     * Sets a listener to inform when the user closes the SearchView.
     *
     * @param listener the listener to call when the user closes the SearchView.
     */
    public void setOnSearchViewCloseListener(OnSearchViewCloseListener listener) {
        mOnSearchViewCloseListener = listener;
    }

    /**
     * Sets a listener to inform when the focus of the query text field changes.
     *
     * @param listener the listener to inform of focus changes.
     */
/*
    public void setOnQueryTextFocusChangeListener(OnFocusChangeListener listener) {
        mOnQueryTextFocusChangeListener = listener;
    }
*/

    /**
     * Sets a listener to inform when the search button is pressed. This is only
     * relevant when the text field is not visible by default. Calling {@link #setIconified
     * setIconified(false)} can also cause this listener to be informed.
     *
     * @param listener the listener to inform when the search button is clicked or
     * the text field is programmatically de-iconified.
     */
    public void setOnSearchClickListener(OnClickListener listener) {
        mOnSearchClickListener = listener;
    }

    /**
     * Returns the query string currently in the text field.
     *
     * @return the query string
     */
    public CharSequence getQuery() {
        return mQueryTextView.getText();
    }

    /**
     * Sets a query string in the text field and optionally submits the query as well.
     *
     * @param query the query string. This replaces any query text already present in the
     * text field.
     * @param submit whether to submit the query right now or only update the contents of
     * text field.
     */
    public void setQuery(CharSequence query, boolean submit) {
        mQueryTextView.setText(query);
        if (query != null) {
            mQueryTextView.setSelection(query.length());
            mUserQuery = query;
        }
    }

    /**
     * Sets the hint text to display in the query text field. This overrides any hint specified
     * in the SearchableInfo.
     *
     * @param hint the hint text to display
     *
     * @attr ref android.R.styleable#SearchView_queryHint
     */
    public void setQueryHint(CharSequence hint) {
        mQueryHint = hint;
        updateQueryHint();
    }

    /**
     * Sets the default or resting state of the search field. If true, a single search icon is
     * shown by default and expands to show the text field and other buttons when pressed. Also,
     * if the default state is iconified, then it collapses to that state when the close button
     * is pressed. Changes to this property will take effect immediately.
     *
     * <p>The default value is true.</p>
     *
     * @param iconified whether the search field should be iconified by default
     *
     * @attr ref android.R.styleable#SearchView_iconifiedByDefault
     */
    public void setIconifiedByDefault(boolean iconified) {
        if (mIconifiedByDefault == iconified) return;
        mIconifiedByDefault = iconified;
        updateViewsVisibility(iconified);
        updateQueryHint();
    }

    /**
     * Returns the default iconified state of the search field.
     * @return
     */
    public boolean isIconfiedByDefault() {
        return mIconifiedByDefault;
    }

    /**
     * Iconifies or expands the SearchView. Any query text is cleared when iconified. This is
     * a temporary state and does not override the default iconified state set by
     * {@link #setIconifiedByDefault(boolean)}. If the default state is iconified, then
     * a false here will only be valid until the user closes the field. And if the default
     * state is expanded, then a true here will only clear the text field and not close it.
     *
     * @param iconify a true value will collapse the SearchView to an icon, while a false will
     * expand it.
     */
    public void setIconified(boolean iconify) {
        if (iconify) {
            onCloseClicked();
        } else {
            onSearchClicked();
        }
    }

    public boolean isIconified() {
        return mIconified;
    }

    /**
     * Enables showing a submit button when the query is non-empty. In cases where the SearchView
     * is being used to filter the contents of the current activity and doesn't launch a separate
     * results activity, then the submit button should be disabled.
     *
     * @param enabled true to show a submit button for submitting queries, false if a submit
     * button is not required.
     */
    public void setSubmitButtonEnabled(boolean enabled) {
        mSubmitButtonEnabled = enabled;
        updateViewsVisibility(isIconified());
    }

    /**
     * Returns whether the submit button is enabled when necessary or never displayed.
     *
     * @return whether the submit button is enabled automatically when necessary
     */
    public boolean isSubmitButtonEnabled() {
        return mSubmitButtonEnabled;
    }

    /**
     * Makes the view at most this many pixels wide
     *
     * @attr ref android.R.styleable#SearchView_maxWidth
     */
    public void setMaxWidth(int maxpixels) {
        mMaxWidth = maxpixels;

        requestLayout();
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Let the standard measurements take effect in iconified state.
        if (isIconified()) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        Log.i(Constant.LOG_TAG, "onMeasure width: " + width);
        Log.i(Constant.LOG_TAG, "onMeasure height: " + height);

        switch (widthMode) {
        case MeasureSpec.AT_MOST:
            // 最大でも指定のピクセル値にすべし
            if (mMaxWidth > 0) {
                width = Math.min(mMaxWidth, width);
            } else {
                width = Math.min(getPreferredWidth(), width);
            }
            break;
        case MeasureSpec.EXACTLY:
            // 厳密に指定のピクセル値にすべし
            if (mMaxWidth > 0) {
                width = Math.min(mMaxWidth, width);
            }
            break;
        case MeasureSpec.UNSPECIFIED:
            // (お好きなように＝ピクセル値指定なし
            width = mMaxWidth > 0 ? mMaxWidth : getPreferredWidth();
            break;
        }
        widthMode = MeasureSpec.EXACTLY;
//        super.onMeasure(MeasureSpec.makeMeasureSpec(MeasureSpec.getSize(widthMeasureSpec), MeasureSpec.EXACTLY), heightMeasureSpec);
        super.onMeasure(MeasureSpec.makeMeasureSpec(width, widthMode), heightMeasureSpec);
    }

    private int getPreferredWidth() {
        return getContext().getResources()
                .getDimensionPixelSize(R.dimen.search_view_preferred_width);
    }

    private CharSequence getDecoratedHint(CharSequence hintText) {
        // If the field is always expanded, then don't add the search icon to the hint
        if (!mIconifiedByDefault) return hintText;

        SpannableStringBuilder ssb = new SpannableStringBuilder("   "); // for the icon
        ssb.append(hintText);
        Drawable searchIcon = getContext().getResources().getDrawable(R.drawable.ic_menu_search);
        int textSize = (int) (mQueryTextView.getTextSize() * 1.25);
        searchIcon.setBounds(0, 0, textSize, textSize);
        ssb.setSpan(new ImageSpan(searchIcon), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        return ssb;
    }

    private void updateQueryHint() {
        if (mQueryHint != null) {
            mQueryTextView.setHint(getDecoratedHint(mQueryHint));
        } else {
            mQueryTextView.setHint(getDecoratedHint(""));
        }
    }

	private void updateViewsVisibility(final boolean collapsed) {
        mIconified = collapsed;
        // Visibility of views that are visible when collapsed
        final int visCollapsed = collapsed ? VISIBLE : GONE;
        // Is there text in the query
        final boolean hasText = !TextUtils.isEmpty(mQueryTextView.getText());

        mSearchButton.setVisibility(visCollapsed);
        updateSubmitButton(hasText);
        mSearchEditFrame.setVisibility(collapsed ? GONE : VISIBLE);
        mSearchHintIcon.setVisibility(mIconifiedByDefault ? GONE : VISIBLE);
        updateCloseButton();
        updateSubmitArea();
    }

    private boolean isSubmitAreaEnabled() {
        return (mSubmitButtonEnabled) && !isIconified();
    }

    private void updateSubmitButton(boolean hasText) {
        int visibility = GONE;
        if (mSubmitButtonEnabled && isSubmitAreaEnabled() && hasFocus() && hasText) {
            visibility = VISIBLE;
        }
        mSubmitButton.setVisibility(visibility);
    }

    private void updateSubmitArea() {
        int visibility = GONE;
        if (isSubmitAreaEnabled()
        && (mSubmitButton.getVisibility() == VISIBLE)) {
            visibility = VISIBLE;
        }
        mSubmitArea.setVisibility(visibility);
    }

    private void updateCloseButton() {
        final boolean hasText = !TextUtils.isEmpty(mQueryTextView.getText());
        // Should we show the close button? It is not shown if there's no focus,
        // field is not iconified by default and there is no text in it.
        final boolean showClose = hasText || (mIconifiedByDefault && !mExpandedInActionView);
        mCloseButton.setVisibility(showClose ? VISIBLE : GONE);
        mCloseButton.getDrawable().setState(hasText ? ENABLED_STATE_SET : EMPTY_STATE_SET);
    }

    private void postUpdateFocusedState() {
        post(mUpdateDrawableStateRunnable);
    }

    private void updateFocusedState() {
    	if(mQueryTextView != null) {
	        boolean focused = mQueryTextView.hasFocus();
	        mSearchPlate.getBackground().setState(focused ? FOCUSED_STATE_SET : EMPTY_STATE_SET);
	        mSubmitArea.getBackground().setState(focused ? FOCUSED_STATE_SET : EMPTY_STATE_SET);
    	}
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mOnSearchViewCloseListener != null) {
        	mOnSearchViewCloseListener.onSearchViewClose();
        }

        removeCallbacks(mUpdateDrawableStateRunnable);
        super.onDetachedFromWindow();
    }

    private void setImeVisibility(final boolean visible) {
        if (visible) {
            post(mShowImeRunnable);
        } else {
            removeCallbacks(mShowImeRunnable);
            InputMethodManager imm = (InputMethodManager)
                    getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

            if (imm != null) {
                imm.hideSoftInputFromWindow(getWindowToken(), 0);
            }
        }
    }

    private final OnClickListener mOnClickListener = new OnClickListener() {
    	@Override
        public void onClick(View v) {
            if (v == mSearchButton) {
                onSearchClicked();
            } else if (v == mCloseButton) {
                onCloseClicked();
            } else if (v == mQueryTextView) {
            }
        }
    };

    private final OnLongClickListener mOnLongClickListener = new OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
/*
			if(v == mTagButton) {
				int TagIconId = R.drawable.tag_icon_gray;
				if(mMenu != null && mMenu.size() > 0) {
					int size = mMenu.size();
					for(int i = 0; i < size; i++) {
						MenuItem item = mMenu.getItem(i);
						item.setChecked(false);
					}
					TagIconId = R.drawable.tag_icon_green;
				}
				mTagButton.setImageResource(TagIconId);
				arrTags = new String[0];
				if(mOnQueryChangeListener != null) {
					mOnQueryChangeListener.onQueryTagsChange(arrTags);
				}
				if(tagpopup != null && tagpopup.isShowing()) {
					tagpopup.setTagList(mMenu);
				}
				return true;
			}
*/
			return false;
		}
    };

    private void onCloseClicked() {
    	//Log.d(Constant.LOG_TAG, "SearchView onCloseClicked");
        CharSequence text = mQueryTextView.getText();
        if (TextUtils.isEmpty(text)) {
            // hide the keyboard and remove focus
            clearFocus();
            // collapse the search field
            updateViewsVisibility(true);
        } else {
            mQueryTextView.setText("");
            mQueryTextView.requestFocus();
        	onTextChanged("");
            setImeVisibility(true);
        }

    }

    private void onSearchClicked() {
        updateViewsVisibility(false);
        mQueryTextView.requestFocus();
//        setImeVisibility(true);
        if (mOnSearchClickListener != null) {
            mOnSearchClickListener.onClick(this);
        }
    }

    void onTextFocusChanged() {
        updateViewsVisibility(isIconified());
        // Delayed update to make sure that the focus has settled down and window focus changes
        // don't affect it. A synchronous update was not working.
        postUpdateFocusedState();
        if (mQueryTextView.hasFocus()) {
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasWindowFocus) {
        super.onWindowFocusChanged(hasWindowFocus);

        postUpdateFocusedState();
    }

    /**
     * {@inheritDoc}
     * 折りたたまれるときに呼ばれる
     */
    @Override
    public void onActionViewCollapsed() {
    	//Log.d(Constant.LOG_TAG, "SearchView onActionViewCollapsed");
        clearFocus();
        updateViewsVisibility(true);
//        mQueryTextView.setImeOptions(mCollapsedImeOptions);
        mExpandedInActionView = false;
    }

    /**
     * {@inheritDoc}
     * 拡大するときに呼ばれる
     */
    @Override
    public void onActionViewExpanded() {
        if (mExpandedInActionView) return;
    	//Log.d(Constant.LOG_TAG, "SearchView onActionViewExpanded");

        mExpandedInActionView = true;
//        mCollapsedImeOptions = mQueryTextView.getImeOptions();
//    	mQueryTextView.setImeOptions(mCollapsedImeOptions);
//        mQueryTextView.setText("");
        setIconified(false);
    }

    /**
     * Callback to watch the text field for empty/non-empty
     */
    private TextWatcher mTextWatcher = new TextWatcher() {

        public void beforeTextChanged(CharSequence s, int start, int before, int after) { }

        public void onTextChanged(CharSequence s, int start,
                int before, int after) {
            SearchView.this.onTextChanged(s);
        }

        public void afterTextChanged(Editable s) {
        }
    };

	private void onTextChanged(CharSequence newText) {
        CharSequence text = mQueryTextView.getText();
        mUserQuery = text;
        boolean hasText = !TextUtils.isEmpty(text);
        updateSubmitButton(hasText);
        updateCloseButton();
        updateSubmitArea();
        if (mOnQueryChangeListener != null && !TextUtils.equals(newText, mOldQueryText)) {
            mOnQueryChangeListener.onQueryTextChange(newText.toString());
        }
        mOldQueryText = newText.toString();
	}

	static boolean isLandscapeMode(Context context) {
        return context.getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE;
    }

	public static class SearchQueryEditText extends EditText {
		private SearchView mSearchView;
		public SearchQueryEditText(Context context) {
			super(context);
		}
		public SearchQueryEditText(Context context, AttributeSet attrs) {
			super(context, attrs);
		}
		void setSearchView(SearchView view) {
			mSearchView = view;
		}

		@SuppressWarnings("unused")
		private boolean isEmpty() {
			return TextUtils.getTrimmedLength(getText()) == 0;
		}

		@Override
		public void onWindowFocusChanged(boolean hasWindowFocus) {
			super.onWindowFocusChanged(hasWindowFocus);
			if(hasWindowFocus && mSearchView.hasFocus() && getVisibility() == VISIBLE) {
/*
				InputMethodManager inputManager = (InputMethodManager) getContext()
                        .getSystemService(Context.INPUT_METHOD_SERVICE);
                inputManager.showSoftInput(this, 0);
*/
			}
		}

		@Override
		protected void onFocusChanged(boolean focused, int direction, Rect previouslyFocusedRect) {
            super.onFocusChanged(focused, direction, previouslyFocusedRect);
            mSearchView.onTextFocusChanged();
		}

        @Override
        public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        	//Log.d(Constant.LOG_TAG, "SearchView onKeyPreIme");
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                // special case for the back key, we do not even try to send it
                // to the drop down list but instead, consume it immediately
                if (event.getAction() == KeyEvent.ACTION_DOWN && event.getRepeatCount() == 0) {
                    KeyEvent.DispatcherState state = getKeyDispatcherState();
                    if (state != null) {
                        state.startTracking(event, this);
                    }
                    return true;
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    KeyEvent.DispatcherState state = getKeyDispatcherState();
                    if (state != null) {
                        state.handleUpEvent(event);
                    }
                    if (event.isTracking() && !event.isCanceled()) {
                        mSearchView.clearFocus();
                        mSearchView.setImeVisibility(false);
                        return true;
                    }
                }
            }
            return super.onKeyPreIme(keyCode, event);
        }
	}
/*
	MenuBuilder mMenu;
	private void updateTagIcon() {
		int TagIconId = R.drawable.tag_icon_gray;
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
				TagIconId = R.drawable.tag_icon_red;
			} else {
				TagIconId = R.drawable.tag_icon_green;
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
//			mMenu.close();
//			mMenu = null;
			mMenu = tagpopup.getMenu();
			updateTagIcon();
		}
		if(mOnQueryChangeListener != null) {
			mOnQueryChangeListener.onQueryTagsChange(arrTags);
		}
	}
*/
}
