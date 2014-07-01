package com.kaznog.android.dreamnote.fragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.evernote.client.oauth.android.EvernoteSession;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.db.DreamNoteProvider;
import com.kaznog.android.dreamnote.db.schema.ItemsSchema;
import com.kaznog.android.dreamnote.dialogfragment.DeleteItemDialogFragment;
import com.kaznog.android.dreamnote.evernote.DreamPostEnmlService;
import com.kaznog.android.dreamnote.fragment.ToDoEditFragmentActivity.ToDoEditFragment;
import com.kaznog.android.dreamnote.listener.DeleteItemDialogResultListener;
import com.kaznog.android.dreamnote.listener.OnFragmentControlListener;
import com.kaznog.android.dreamnote.settings.PreferencesUtil;
import com.kaznog.android.dreamnote.util.Constant;
import com.kaznog.android.dreamnote.util.StringUtils;

public class ToDoNewPreviewFragmentActivity extends SherlockFragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FragmentManager fm = getSupportFragmentManager();
		if(fm.findFragmentById(android.R.id.content) == null) {
			ToDoNewPreviewFragment f = new ToDoNewPreviewFragment();
			fm.beginTransaction().add(android.R.id.content, f).commit();
		}
	}

	public static class ToDoNewPreviewFragment extends PreviewFragment
	implements
	OnFragmentControlListener,
	DeleteItemDialogResultListener {
		private TextView preview_title;
		private TextView preview_content;
		private TextView preview_tags;
		private TextView preview_date;
		private EvernoteSession session;
		private MenuItem EverMenu;

		public static ToDoNewPreviewFragment newInstance(Item item) {
			ToDoNewPreviewFragment f = new ToDoNewPreviewFragment();
			Bundle args = new Bundle();
			args.putSerializable("item", item);
			f.setArguments(args);
			return f;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	        View root = createContentView(inflater, R.layout.preview_todo_new_frag);
			return root;
		}

		@Override
		public void onActivityCreated(Bundle savedInstanceState) {
			super.onActivityCreated(savedInstanceState);
			setupUI(mContentContainer);
			setupSession(this.getSherlockActivity().getApplicationContext());
			if(savedInstanceState == null) {
				EverMenu = null;
				item = (Item)getArguments().getSerializable("item");
				setItemData();
			} else {
				item = (Item) savedInstanceState.getSerializable("item");
				setItemData();
			}
			setHasOptionsMenu(true);
			setContentShown(true);
		}

		@Override
		public void onSaveInstanceState(Bundle outState) {
			super.onSaveInstanceState(outState);
			outState.putSerializable("item", item);
			setUserVisibleHint(true);
		}

		@Override
		public void onResume() {
			super.onResume();
			if(session != null) {
				session.completeAuthentication(PreferencesUtil.getSharedPreferences(this.getSherlockActivity().getApplicationContext()));
				if(EverMenu != null) {
					setEvernoteMenuEnabled(EverMenu);
				}
			}
		}

		private void setupUI(View v) {
			preview_title = (TextView)v.findViewById(R.id.preview_title);
			preview_content = (TextView)v.findViewById(R.id.preview_content);
			preview_tags = (TextView)v.findViewById(R.id.preview_tags);
			preview_date = (TextView)v.findViewById(R.id.preview_date);
		}

		protected void setItemData() {
			// タイトルを設定
			preview_title.setText(item.title);
			// 備考を設定
			if(item.content.trim().equals("")) {
				// 備考がない場合
				preview_content.setText(R.string.non_description);
				preview_content.setTextColor(Color.GRAY);
			} else {
				preview_content.setText(item.content);
				preview_content.setTextColor(Color.BLACK);
			}
			// 期限を設定
			String year = item.updated.substring(0, 4);
			if(year.equals("0001")) {
				preview_date.setText(R.string.non_limit);
			} else {
				String[] date = item.updated.split(" ");
				SimpleDateFormat sdf = new SimpleDateFormat("'('E')'");
				String DayOfTheWeek = sdf.format(StringUtils.toDate(item.updated));
				preview_date.setText(date[0] + DayOfTheWeek);
			}
			if(item.tags.equals("")) {
				// タグがない場合はタグTextViewを表示しない
				preview_tags.setText(R.string.non_tags);
				preview_tags.setTextColor(Color.GRAY);
			} else {
				// タグがある場合
				preview_tags.setText(item.tags);
				preview_tags.setTextColor(Color.BLACK);
			}
		}

		@Override
		public void onAbort() {
			setResult(RESULT_CANCELED, null);
    		FragmentTransaction ft = getSherlockActivity().getSupportFragmentManager().beginTransaction();
			ft.remove(this);
			ft.commit();

		}

		//----------------------------------------------------------------------
		private void setupSession(Context context) {
			if(mFragmentControlListener != null) {
				session = mFragmentControlListener.getEvernoteSession();
			}
		}
		private void setEvernoteMenuEnabled(MenuItem item) {
			if(item != null) {
				boolean loggedIn = false;
				if(session != null) {
					loggedIn = session.isLoggedIn();
				}
				item.setEnabled(loggedIn);
				item.setVisible(loggedIn);
			}
		}
		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			super.onCreateOptionsMenu(menu, inflater);
			EverMenu = menu.findItem(R.id.pre_menu_frag_ever);
			setEvernoteMenuEnabled(EverMenu);
		}

	    @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
			switch(item.getItemId()) {
			case R.id.pre_menu_frag_del:
				DeleteItemDialogFragment delf = new DeleteItemDialogFragment();
				delf.setArguments(null);
				delf.setResultListener(this);
				delf.show(getSherlockActivity().getSupportFragmentManager(), "DeleteItemDialog");
				return true;
			case R.id.pre_menu_frag_edit:
				if(mFragmentControlListener != null) {
					ToDoEditFragment f = ToDoEditFragment.newInstance(this.item);
					mFragmentControlListener.onAddNoteFragment(this, f, Notes.EDIT_TODO_FRAGMENT, "edit");
				}
				return true;
			case R.id.pre_menu_frag_share:
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_EDIT);
			    intent.setType("vnd.android.cursor.item/event");
			    intent.putExtra("title", this.item.title);
			    intent.putExtra("description", this.item.content);
			    intent.putExtra("allDay", true);
				String year = this.item.updated.substring(0, 4);
				if(year.equals("0001") == false) {
					Calendar cal = Calendar.getInstance();
					cal.clear();
					cal.setTime(StringUtils.toDate(this.item.updated));
					cal.add(Calendar.DATE, 1);
				    intent.putExtra("beginTime", cal.getTimeInMillis());
				    intent.putExtra("endTime", cal.getTimeInMillis());
				}
			    try {
			      startActivity(intent);
			    } catch (android.content.ActivityNotFoundException ex) {
			      Toast.makeText(getSherlockActivity().getApplicationContext(), R.string.err_activity_not_found, Toast.LENGTH_SHORT).show();
			    }
				return true;
			case R.id.pre_menu_frag_complete:
				this.item.datatype = DreamNoteProvider.ITEMTYPE_TODO;
	    		Date now = new Date();
	    		this.item.long_date = now.getTime();
	        	ContentValues values = new ContentValues();
	    		values.put(ItemsSchema.COLUMN_ID, this.item.id);
	        	values.put(ItemsSchema.DATATYPE, this.item.datatype);
	        	values.put(ItemsSchema.DATE, this.item.long_date);
	        	values.put(ItemsSchema.UPDATED, this.item.long_updated);
	        	values.put(ItemsSchema.CREATED, this.item.long_created);
	        	values.put(ItemsSchema.TITLE, this.item.title);
	        	values.put(ItemsSchema.CONTENT, this.item.content);
	        	values.put(ItemsSchema.DESCRIPTION, this.item.description);
	        	values.put(ItemsSchema.PATH, this.item.path);
	        	values.put(ItemsSchema.RELATED, this.item.related);
	        	values.put(ItemsSchema.CREATED, this.item.long_created);
	        	values.put(ItemsSchema.TAGS, this.item.tags);
	    		Uri uri = ContentUris.withAppendedId(DreamNoteProvider.ITEMS_CONTENT_URI, this.item.id);
	    		getSherlockActivity().getContentResolver().update(uri, values, null, null);
	        	setResult(RESULT_OK, null);
	    		FragmentTransaction ft = getSherlockActivity().getSupportFragmentManager().beginTransaction();
				ft.remove(this);
				ft.commit();
				return true;
			case R.id.pre_menu_frag_ever:
				Toast.makeText(getSherlockActivity().getApplicationContext(), R.string.please_wait, Toast.LENGTH_LONG).show();
				Intent everintent = new Intent(this.getSherlockActivity().getApplicationContext(), DreamPostEnmlService.class);
				everintent.putExtra("item", this.item);
				this.getSherlockActivity().startService(everintent);
				return true;
			}
	    	//Log.d(Constant.LOG_TAG, this.getClass().getSimpleName() + " onOptionsItemSelected: " + strItemId);
	    	return false;
	    }

		@Override
		public void onAddNoteFragment(OnFragmentControlListener listener,
				Fragment fragment, int fragment_type, String tag) {
			// TODO 自動生成されたメソッド・スタブ

		}

		@Override
		public void onFragmentResult(Fragment fragment, int requestCode,
				int resultCode, Bundle extra) {
			if(requestCode == Notes.EDIT_TODO_FRAGMENT) {
				if(resultCode == RESULT_OK && extra != null) {
					int next_request = extra.getInt(Constant.FRAGMENT_RESULT_REQUEST, Constant.FRAGMENT_RESULT_REQUEST_NON);
					if(next_request == Constant.FRAGMENT_RESULT_READDATA) {
						this.item = (Item) extra.getSerializable("item");
						setItemData();
					}
				}
				setResult(resultCode, null);
			}
		}

		@Override
		public void onDeleteItemDialogResult(int result) {
			if(result == DialogInterface.BUTTON_POSITIVE) {
				String[] whereArgs = new String[] {
						String.valueOf(item.id),
						"todos"
				};
				Context context = getSherlockActivity().getApplicationContext();
				getSherlockActivity().getContentResolver().delete(DreamNoteProvider.ITEMS_CONTENT_URI, null, whereArgs);
				String strResult = context.getResources().getString(R.string.itemtype_todonew) + "\n" + item.title + context.getResources().getString(R.string.item_delete_msg_foot);
				Toast.makeText(context, strResult, Toast.LENGTH_LONG).show();
				onAbort();
			}
		}

		@Override
		public void onRemoveRequest(String tag) {
			// TODO 自動生成されたメソッド・スタブ

		}

		@Override
		public EvernoteSession getEvernoteSession() {
			// TODO 自動生成されたメソッド・スタブ
			return null;
		}

		@Override
		public void setEvernoteSession(EvernoteSession session) {
			// TODO Auto-generated method stub
			
		}
	}
}
