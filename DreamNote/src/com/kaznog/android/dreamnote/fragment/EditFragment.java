package com.kaznog.android.dreamnote.fragment;

import java.util.ArrayList;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.db.DreamNoteProvider;
import com.kaznog.android.dreamnote.db.schema.ItemsSchema;
import com.kaznog.android.dreamnote.db.schema.TagsSchema;
import com.kaznog.android.dreamnote.dialogfragment.TagSelectorDialogFragment;
import com.kaznog.android.dreamnote.listener.AbortDialogResultListener;
import com.kaznog.android.dreamnote.listener.MenuEventListener;
import com.kaznog.android.dreamnote.listener.TagSelectorDialogListener;
import com.kaznog.android.dreamnote.util.AppInfo;
import com.kaznog.android.dreamnote.widget.DreamNoteEditText;
import com.kaznog.android.dreamnote.widget.DreamNoteTitleEditText;
import com.kaznog.android.dreamnote.widget.TagAutoCompleteTextView;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager.LayoutParams;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.MultiAutoCompleteTextView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

public abstract class EditFragment extends PreviewFragment
implements
MenuEventListener,
TagSelectorDialogListener,
AbortDialogResultListener,
LoaderManager.LoaderCallbacks<Cursor> {
	protected boolean mIsTagsLoaded = false;
	protected CursorLoader tagsloader;
	protected View decor;
	protected View mContainerView;
	protected View mSplitView;

	protected boolean mode;
	protected String[] arrTags = null;
	protected ArrayList<String> taglist;
	protected DreamNoteTitleEditText titletext;
	protected DreamNoteEditText contenttext;
	protected TagAutoCompleteTextView tagtext;
	protected ImageButton TagSelButton;
	protected String[] aborttoastmsg;
	protected String[] abortalertmsgs;
	protected String[] modealerttitle;
	protected String[][] alertmsgs;
	protected SpannableStringBuilder ssbtitle;
	protected SpannableStringBuilder ssbcontent;
	protected SpannableStringBuilder ssbtag;
	protected String editing_title;
	protected String editing_content;
	protected String editing_tags;

	protected void onActivityCreatedRestore(Bundle savedInstanceState) {
		item = (Item) savedInstanceState.getSerializable("item");
		editing_title = savedInstanceState.getString("editing_title");
		editing_content = savedInstanceState.getString("editing_content");
		editing_tags = savedInstanceState.getString("editing_tags");
		mode = savedInstanceState.getBoolean("mode");
		mIsTagsLoaded = savedInstanceState.getBoolean("mIsTagsLoaded");
		arrTags = savedInstanceState.getStringArray("arrTags");
		taglist = savedInstanceState.getStringArrayList("taglist");
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		setUserVisibleHint(true);
		onSaveInstanceMoreState(outState);
	}

	protected void onSaveInstanceMoreState(Bundle outState) {
		outState.putBoolean("mode", mode);
		outState.putBoolean("mIsTagsLoaded", mIsTagsLoaded);
		outState.putStringArray("arrTags", arrTags);
		outState.putStringArrayList("taglist", taglist);
		ssbtitle = (SpannableStringBuilder) titletext.getText();
		ssbcontent = (SpannableStringBuilder)contenttext.getText();
		ssbtag = (SpannableStringBuilder)tagtext.getText();
		outState.putString("editing_title", ssbtitle.toString());
		outState.putString("editing_content", ssbcontent.toString());
		outState.putString("editing_tags", ssbtag.toString());
		outState.putSerializable("item", item);
	}

	protected abstract void setupMsg();
	protected String getAlertTitle() {
		return modealerttitle[mode ? 0 : 1];
	}
	protected String getAlertMsg(int index) {
		return alertmsgs[mode ? 0 : 1][index];
	}
	protected abstract boolean isChanged();
	public abstract void onMenuEvent(int menuId);

	protected void initializeData() {
	}

	protected View getContainerView() {
		return mContainerView;
	}
	protected void updateActionbar(boolean enabled) {
		if(mContainerView != null) {
			if(enabled) {
				if(mContainerView.getVisibility() == View.GONE) {
					mContainerView.setVisibility(View.VISIBLE);
					if(mSplitView != null) {
						mSplitView.setVisibility(View.VISIBLE);
					}
				}
			} else {
				if(mContainerView.getVisibility() == View.VISIBLE) {
					mContainerView.setVisibility(View.GONE);
					if(mSplitView != null) {
						mSplitView.setVisibility(View.GONE);
					}
				}
			}
		}
	}
	public boolean onDispatchKeyDown(int keyCode, KeyEvent event) {
		if(onFocusView != null) {
			if(onFocusView == contenttext) {
				onFocusView.requestFocus();
				onFocusView.dispatchKeyEventPreIme(event);
				return false;
			} else if(onFocusView == titletext) {
				contenttext.requestFocus();
				return false;
			} else if(onFocusView == tagtext) {
				updateActionbar(true);
				unFocusSpecific();
//				tagtext.clearFocus();
				hideIME();
				TagSelButton.requestFocus();
				return false;
			}
		}
		return true;
	}

	protected OnEditorActionListener mOnTitleEditorActionListener = new OnEditorActionListener() {
		@Override
		public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
			int keyCode = 0;
			if(event != null) {
				keyCode = event.getKeyCode();
			}
			AppInfo.DebugLog(getSherlockActivity().getApplicationContext(), "onEditorAction keyCode: " + keyCode + " actionId: " + actionId);
			if(event == null && actionId == EditorInfo.IME_ACTION_NEXT
			|| event == null && actionId == EditorInfo.IME_ACTION_DONE
			|| event != null && keyCode == KeyEvent.KEYCODE_ENTER) {
				contenttext.requestFocus();
				return true;
			}
			return false;
		}
	};
	protected OnEditorActionListener mOnTagEditorActionListener = new OnEditorActionListener() {

		@Override
		public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
			int keyCode = 0;
			if(event != null) {
				keyCode = event.getKeyCode();
			}
			if(event == null && actionId == EditorInfo.IME_ACTION_NEXT
			|| event == null && actionId == EditorInfo.IME_ACTION_DONE
			|| event != null && keyCode == KeyEvent.KEYCODE_ENTER) {
				updateActionbar(true);
				unFocusSpecific();
//				tagtext.clearFocus();
				hideIME();
				TagSelButton.requestFocus();
				return true;
			}
			return false;
		}
		
	};
	protected View onFocusView;
	protected OnFocusChangeListener mFocusChangeListener = new OnFocusChangeListener() {
		@Override
		public void onFocusChange(View view, boolean flag) {
			if(flag) {
				AppInfo.DebugLog(getSherlockActivity(), "onFocus");
				onFocusView = view;
				updateActionbar(false);
				onFocusSpecific();
			} else {
				AppInfo.DebugLog(getSherlockActivity(), "unFocus");
				updateActionbar(true);
				unFocusSpecific();
			}
		}
	};
	protected abstract void onFocusSpecific();
	protected abstract void unFocusSpecific();

	protected void setupContainer() {
		decor = this.getSherlockActivity().getWindow().getDecorView();
		mContainerView = null;
		mSplitView = null;
		if(Build.VERSION.SDK_INT > 10) {
			int mContainerViewId;
			int mSplitViewId;
			try {
				mContainerViewId = (Integer) Class.forName(
				        "com.android.internal.R$id").getField("action_bar_container").get(null);
				mContainerView = decor.findViewById(mContainerViewId);
				if(isSplitActionbarIsNarrow()) {
					mSplitViewId = (Integer) Class.forName(
					        "com.android.internal.R$id").getField("split_action_bar").get(null);
					mSplitView = decor.findViewById(mSplitViewId);
				}
			} catch (IllegalArgumentException e) {
			} catch (IllegalAccessException e) {
			} catch (NoSuchFieldException e) {
			} catch (ClassNotFoundException e) {
			} catch(StackOverflowError se) {
			}
		} else {
			try {
				mContainerView = decor.findViewById(R.id.abs__action_bar_container);
				mSplitView = decor.findViewById(R.id.abs__split_action_bar);
			} catch(StackOverflowError se) {

			}
		}
	}
	protected void setupUI(View v) {
		setupContainer();
    	// タイトル入力ビューの取得
		titletext = (DreamNoteTitleEditText)v.findViewById(R.id.memo_title);
		// 本文入力ビューの取得
		contenttext = (DreamNoteEditText)v.findViewById(R.id.memo_content);
		// タグ入力ビューの取得
    	tagtext = (TagAutoCompleteTextView)v.findViewById(R.id.MultiAutoCompleteTagText);
    	tagtext.addTextChangedListener(mTextWatcher);
		// タグ選択ボタンビューの取得
    	TagSelButton = (ImageButton)v.findViewById(R.id.TagSelButton);
    	TagSelButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				if(arrTags != null && arrTags.length > 0) {
					showTagSelectorDialog();
				}
			}
    	});

    	// タグ読み込み中は無効にしておく
    	TagSelButton.setEnabled(false);
	}

	protected void setItemData() {
	}

	protected void setupTagCompleteText() {
		if(arrTags != null && arrTags.length > 0) {
			ArrayAdapter<String> autoTextAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, arrTags);
			tagtext.setAdapter(autoTextAdapter);
			tagtext.setTokenizer(new MultiAutoCompleteTextView.CommaTokenizer());
		}
	}

	protected void setButtonEnable() {
    	TagSelButton.setEnabled(true);
	}

    protected TextWatcher mTextWatcher = new TextWatcher() {

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			onTagTextChanged(s);
		}

		@Override
		public void afterTextChanged(Editable arg0) {}
    };

    protected void onTagTextChanged(CharSequence s) {
    	ssbtag = (SpannableStringBuilder)tagtext.getText();
    	String tag = ssbtag.toString();
    	if(tag.equals("")) {
    		return;
    	} else {
        	tag = tag.replaceAll(" ", "");
        	tag = tag.replaceAll("，", ",");
        	tag = tag.replaceAll(", ", ",");
        	tag = tag.replaceAll("、", ",");
        	tag = tag.replaceAll("､", ",");
        	tag = tag.replaceAll("　", ",");

        	tag = tag.replaceAll(",", ", ");
    	}
    	if(tag.equalsIgnoreCase(ssbtag.toString())) return;
		tagtext.setText(tag);
		tagtext.setSelection(tag.length());
    }

	protected void showTagSelectorDialog() {
		TagSelectorDialogFragment f = new TagSelectorDialogFragment();
		Bundle args = new Bundle();
		ssbtag = (SpannableStringBuilder)tagtext.getText();
		args.putString("tagtext", ssbtag.toString());
		args.putStringArray("arrTags", arrTags);
		args.putStringArrayList("taglist", taglist);
		f.setArguments(args);
		f.setOnTagSelectedListener(this);
		f.show(getFragmentManager(), "TagSelectorDialog");
	}

	@Override
	public void onTagSelectorDialogSelected(String selectedTags) {
		tagtext.setText(selectedTags);
	}

	public void onAbortDialogResult(int result) {
		if(result == android.app.Activity.RESULT_CANCELED) {
			//Log.d(Constant.LOG_TAG, "EditFragment onAbortDialogResult CANCELED");

			setResult(android.app.Activity.RESULT_CANCELED, null);

			FragmentTransaction ft = getSherlockActivity().getSupportFragmentManager().beginTransaction();
			ft.remove(this);
			ft.commit();

			getSherlockActivity().supportInvalidateOptionsMenu();
		}
	}

	static final String[] TAGS_PROJECTION = new String[] {
		"_ID as _id",
		TagsSchema.TERM
	};
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		mIsTagsLoaded = false;
		tagsloader = new CursorLoader(
				getActivity(),
				Uri.parse("content://" + DreamNoteProvider.AUTHORITY + "/tags"),
				TAGS_PROJECTION, null, null, null);
		return tagsloader;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor result) {
		taglist = new ArrayList<String>();
		boolean isEof = result.moveToFirst();
		while(isEof) {
			String term = result.getString(result.getColumnIndex("term"));
			taglist.add(term);
			isEof = result.moveToNext();
		}
		if(taglist.size() > 0) {
			arrTags = (String[])taglist.toArray(new String[0]);
			setupTagCompleteText();
		} else {
			arrTags = null;
		}
		setButtonEnable();
		setContentShown(true);
		mIsTagsLoaded = true;
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		// TODO 自動生成されたメソッド・スタブ

	}

	protected void hideIME() {
        InputMethodManager imm = (InputMethodManager)
        		this.getSherlockActivity().getWindow().getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm != null) {
            imm.hideSoftInputFromWindow(this.getSherlockActivity().getWindow().getDecorView().getWindowToken(), 0);
        }
	}

	protected void saveItem() {
		editing_tags = editing_tags.replaceAll(" ", "");
		editing_tags = editing_tags.replaceAll("，", ",");
		editing_tags = editing_tags.replaceAll(", ", ",");
		editing_tags = editing_tags.replaceAll("、", ",");
		editing_tags = editing_tags.replaceAll("､", ",");
		editing_tags = editing_tags.replaceAll("　", ",");
		String[] arr_user_tags = editing_tags.split(",");
		ArrayList<String> memo_tags = new ArrayList<String>();
    	for(String utag: arr_user_tags) {
    		utag = utag.trim();
    		if(utag.equals("") == false) {
    			if(memo_tags.indexOf(utag) == -1) {
    				memo_tags.add(utag);
				}
    		}
    	}
    	editing_tags = memo_tags.toString();
    	this.item.tags = editing_tags.substring(1, editing_tags.length() -1);
    	ContentValues values = new ContentValues();
    	if(mode) {
    		values.put(ItemsSchema.COLUMN_ID, this.item.id);
    	}
    	values.put(ItemsSchema.DATATYPE, this.item.datatype);
    	values.put(ItemsSchema.DATE, this.item.long_date);
    	values.put(ItemsSchema.UPDATED, this.item.long_updated);
    	values.put(ItemsSchema.CREATED, this.item.long_created);
    	values.put(ItemsSchema.TITLE, this.item.title);
    	values.put(ItemsSchema.CONTENT, this.item.content);
    	values.put(ItemsSchema.DESCRIPTION, this.item.description);
    	values.put(ItemsSchema.PATH, this.item.path);
    	values.put(ItemsSchema.RELATED, this.item.related);
    	values.put(ItemsSchema.TAGS, this.item.tags);
    	if(mode) {
    		Uri uri = ContentUris.withAppendedId(DreamNoteProvider.ITEMS_CONTENT_URI, this.item.id);
    		getSherlockActivity().getContentResolver().update(uri, values, null, null);
    	} else {
    		getSherlockActivity().getContentResolver().insert(DreamNoteProvider.ITEMS_CONTENT_URI, values);
    	}
	}
}
