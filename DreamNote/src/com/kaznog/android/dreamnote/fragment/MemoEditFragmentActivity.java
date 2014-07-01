package com.kaznog.android.dreamnote.fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.text.SpannableStringBuilder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.db.DreamNoteProvider;
import com.kaznog.android.dreamnote.dialogfragment.AbortDialogFragment;
import com.kaznog.android.dreamnote.util.Constant;
import com.kaznog.android.dreamnote.widget.DreamNoteEditText.DreamNoteEditTextListener;
import com.kaznog.android.dreamnote.widget.DreamNoteTitleEditText.DreamNoteTitleEditTextListener;
import com.kaznog.android.dreamnote.widget.TagAutoCompleteTextView.TagAutoCompleteTextViewListener;

public class MemoEditFragmentActivity extends SherlockFragmentActivity {
	public static class MemoEditFragment extends EditDefaultFragment implements TagAutoCompleteTextViewListener, DreamNoteTitleEditTextListener, DreamNoteEditTextListener {
		public static MemoEditFragment newUnstance(Item item) {
			MemoEditFragment f = new MemoEditFragment();
			Bundle args = new Bundle();
			args.putSerializable("item", item);
			f.setArguments(args);
			return f;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	        View root = createContentView(inflater, R.layout.edit_memo_frag);
			return root;
		}

		@Override
		protected void initializeData() {
			super.initializeData();
			if(mode) {
				editing_title = item.title;
				editing_content = item.content;
				editing_tags = item.tags;
			} else {
				item.datatype = DreamNoteProvider.ITEMTYPE_MEMO;
				editing_title = "";
				editing_content = "";
				editing_tags = "";
			}
		}

		@Override
		protected void setupUI(View v) {
			super.setupUI(v);
			titletext.setDreamNoteTitleEditTextListener(this);
			titletext.setOnFocusChangeListener(mFocusChangeListener);
			contenttext.setDreamNoteEditTextListener(this);
			contenttext.setOnFocusChangeListener(mFocusChangeListener);
			tagtext.setTagAutoCompleteTextViewListener(this);
	    	tagtext.setOnFocusChangeListener(mFocusChangeListener);

	    	titletext.setOnEditorActionListener(mOnTitleEditorActionListener);
	    	tagtext.setOnEditorActionListener(mOnTagEditorActionListener);

	    	TagSelButton.requestFocus();
		}
		@Override
		protected void onFocusSpecific() {
		}
		@Override
		protected void unFocusSpecific() {
		}
		@Override
		public void onImeHidden() {
			InputMethodManager imm = (InputMethodManager)
					getSherlockActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			if(imm != null) {
				updateActionbar(true);
				unFocusSpecific();
//				tagtext.clearFocus();
		        TagSelButton.requestFocus();
				onFocusView = null;
				imm.hideSoftInputFromWindow(tagtext.getWindowToken(), 0);
			}
		}

		@Override
		public void onEditTextImeHidden() {
			InputMethodManager imm = (InputMethodManager)
					getSherlockActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			if(imm != null) {
				updateActionbar(true);
				unFocusSpecific();
//				contenttext.clearFocus();
		        TagSelButton.requestFocus();
				onFocusView = null;
				imm.hideSoftInputFromWindow(contenttext.getWindowToken(), 0);
			}
		}

		@Override
		public void onTitleEditTextImeHidden() {
			InputMethodManager imm = (InputMethodManager)
					getSherlockActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
			if(imm != null) {
				updateActionbar(true);
				unFocusSpecific();
//				titletext.clearFocus();
		        TagSelButton.requestFocus();
				onFocusView = null;
				imm.hideSoftInputFromWindow(titletext.getWindowToken(), 0);
			}
		}

		@Override
		protected void setItemData() {
			super.setItemData();
			// タイトル設定
			titletext.setText(editing_title);
			// 本文設定
			contenttext.setText(editing_content);
			// タグ設定
			tagtext.setText(editing_tags);
		}

		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			//Log.d(Constant.LOG_TAG, "MemoEditFragment onCreateOptionsMenu");
			super.onCreateOptionsMenu(menu, inflater);
			MenuItem item = menu.findItem(R.id.edit_menu_frag_save);
			if(item != null && mode) {
				item.setTitle(R.string.updatebutton_description);
			}
			if(mode) {
				getSherlockActivity().getSupportActionBar().setTitle(getResources().getString(R.string.editmemo_title));
			} else {
				getSherlockActivity().getSupportActionBar().setTitle(getResources().getString(R.string.addmemo_title));
			}
		}

	    @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
	    	switch(item.getItemId()) {
	    	case R.id.edit_menu_frag_save:
	    		hideIME();
	    		ssbtitle = (SpannableStringBuilder) titletext.getText();
	    		ssbcontent = (SpannableStringBuilder)contenttext.getText();
	    		ssbtag = (SpannableStringBuilder)tagtext.getText();
	    		editing_title = ssbtitle.toString();
	    		editing_content = ssbcontent.toString();
	    		editing_tags = ssbtag.toString();
	    		if(editing_title.trim().equals("") && editing_content.trim().equals("")) {
	    			Toast.makeText(getSherlockActivity().getApplicationContext(), R.string.memo_nontitle_nonbody, Toast.LENGTH_LONG).show();
	    			return false;
	    		}
	    		if(editing_title.trim().equals("")) {
	    			Matcher m = Pattern.compile("^(?:[\\s|\r|\n])*([^\r|\n]+)").matcher(editing_content);
	    			if(m.find()) {
	    				editing_title = m.group(1);
	    			} else {
	    				editing_title = getResources().getString(R.string.memo_nontitle);
	    			}
	    		}
	        	this.item.title = editing_title;
	        	this.item.content = editing_content;
	        	Date now = new Date();
	    		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
	    		String strdate = sdf.format(now);
	    		this.item.date = strdate;
	    		this.item.updated = strdate;
	    		this.item.long_date = now.getTime();
	    		this.item.long_updated = this.item.long_date;
	        	if(mode == false) {
	        		this.item.long_created = this.item.long_date;
	        		this.item.created = strdate;
	        	}
	        	saveItem();
	        	Bundle extra = new Bundle();
	        	extra.putInt(Constant.FRAGMENT_RESULT_REQUEST, Constant.FRAGMENT_RESULT_READDATA);
	        	extra.putSerializable("item", this.item);
	        	setResult(RESULT_OK, extra);
	    		FragmentTransaction ft = getSherlockActivity().getSupportFragmentManager().beginTransaction();
				ft.remove(this);
				ft.commit();
//				getSherlockActivity().supportInvalidateOptionsMenu();
	    		break;
	    	case R.id.edit_menu_frag_cancel:
	    		onAbort();
	    		break;
	    	}
	    	return true;
	    }

		private enum ALERTTYPE {
			VIEWER,
			PHOTO,
			TODO
		};
	    protected void setupMsg() {
			aborttoastmsg = new String[] {
				getResources().getString(R.string.abort_editmemo_toastmsg),
				getResources().getString(R.string.abort_addmemo_toastmsg)
			};
			abortalertmsgs = new String[] {
				getResources().getString(R.string.abort_editmemo_alertmsg),
				getResources().getString(R.string.abort_addmemo_alertmsg)
			};
			modealerttitle = new String[] {
				getResources().getString(R.string.editmemo_alerttitle),
				getResources().getString(R.string.addmemo_alerttitle)
			};
			alertmsgs = new String[][] {
				{
					getResources().getString(R.string.editmemo_alertmsg_viewer),
					getResources().getString(R.string.editmemo_alertmsg_photo),
					getResources().getString(R.string.editmemo_alertmsg_todo)
				},
				{
					getResources().getString(R.string.addmemo_alertmsg_viewer),
					getResources().getString(R.string.addmemo_alertmsg_photo),
					getResources().getString(R.string.addmemo_alertmsg_todo)
				}
			};
	    }
		/**
		 *
		 * @return boolean 登録内容が入力されているか元の内容と違っている場合は true
		 */
		protected boolean isChanged() {
			boolean onchange = false;
			ssbtitle = (SpannableStringBuilder)titletext.getText();
			ssbcontent = (SpannableStringBuilder)contenttext.getText();
	    	ssbtag = (SpannableStringBuilder)tagtext.getText();
    		editing_title = ssbtitle.toString();
    		editing_content = ssbcontent.toString();
    		editing_tags = ssbtag.toString();
    		editing_tags = editing_tags.replaceAll("，", ",");
    		editing_tags = editing_tags.replaceAll(", ", ",");
    		editing_tags = editing_tags.replaceAll("、", ",");
    		editing_tags = editing_tags.replaceAll("､", ",");
    		editing_tags = editing_tags.replaceAll("　", ",");
    		editing_tags = editing_tags.replaceAll(" ", ",");
    		if(mode) {
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
	        	editing_tags = editing_tags.substring(1, editing_tags.length() -1);
				if((editing_title.equals(item.title) == false) || (editing_content.equals(item.content) == false) || (editing_tags.equals(item.tags) == false)){
					// 元の内容と変わっていた場合
					onchange = true;
				}
			} else {
				if((editing_title.equals("") == false) || (editing_content.equals("") == false) || (editing_tags.equals("") == false)) {
					// 登録内容が入力されている場合
					onchange = true;
				}
			}
			return onchange;
		}
		@Override
		public void onAbort() {
    		hideIME();
    		View Container = getContainerView();
    		if(Container != null) {
    			if(Container.getVisibility() == View.GONE) {
    				updateActionbar(true);
    				return;
    			}
    		}
			final String abortmsg = aborttoastmsg[mode ? 0 : 1];
			// 編集されているか確認
			if(isChanged()) {
				AbortDialogFragment f = new AbortDialogFragment();
				Bundle args = new Bundle();
				args.putBoolean("mode", mode);
				args.putStringArray("aborttoastmsg", aborttoastmsg);
				args.putStringArray("modealerttitle", modealerttitle);
				args.putStringArray("abortalertmsgs", abortalertmsgs);
				f.setArguments(args);
				f.setResultListener(this);
				f.show(getFragmentManager(), "AbortDialog");
			} else {
	    		// 編集されていない場合
	    		if(mode) {
	    			Toast.makeText(getSherlockActivity().getApplicationContext(), abortmsg, Toast.LENGTH_LONG).show();
	    		} else {
	    			Toast.makeText(getSherlockActivity().getApplicationContext(), abortmsg, Toast.LENGTH_LONG).show();
	    		}

	    		setResult(RESULT_CANCELED, null);

	    		FragmentTransaction ft = getSherlockActivity().getSupportFragmentManager().beginTransaction();
				ft.remove(this);
				ft.commit();

				//sherlock master dev
//				getSherlockActivity().supportInvalidateOptionsMenu();
				//getSherlockActivity().invalidateOptionsMenu();
			}
		}

		@Override
		public void onMenuEvent(int menuId) {
			// TODO 自動生成されたメソッド・スタブ

		}
	}
}
