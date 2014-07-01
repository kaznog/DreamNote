package com.kaznog.android.dreamnote.fragment;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
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
import com.kaznog.android.dreamnote.dialogfragment.DeleteItemDialogFragment;
import com.kaznog.android.dreamnote.evernote.DreamPostEnmlService;
import com.kaznog.android.dreamnote.fragment.MemoEditFragmentActivity.MemoEditFragment;
import com.kaznog.android.dreamnote.listener.DeleteItemDialogResultListener;
import com.kaznog.android.dreamnote.listener.OnFragmentControlListener;
import com.kaznog.android.dreamnote.settings.PreferencesUtil;
import com.kaznog.android.dreamnote.util.Constant;

public class MemoPreviewFragmentActivity extends SherlockFragmentActivity {
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		FragmentManager fm = getSupportFragmentManager();
		if(fm.findFragmentById(android.R.id.content) == null) {
			MemoPreviewFragment f = new MemoPreviewFragment();
			fm.beginTransaction().add(android.R.id.content, f).commit();
		}
	}

	public static class MemoPreviewFragment extends PreviewFragment
	implements
	OnFragmentControlListener,
	DeleteItemDialogResultListener {
//		private long contentId;
		private TextView preview_title;
		private TextView preview_content;
		private TextView preview_tags;
		private EvernoteSession session;
		private MenuItem EverMenu;
		public static MemoPreviewFragment newInstance(Item item) {
			MemoPreviewFragment f = new MemoPreviewFragment();
			Bundle args = new Bundle();
			args.putSerializable("item", item);
			f.setArguments(args);
			return f;
		}

		@Override
		public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	        View root = createContentView(inflater, R.layout.preview_memo_frag);
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
			setContentShown(true);
			setHasOptionsMenu(true);
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

		protected void setupUI(View v) {
			preview_title = (TextView)v.findViewById(R.id.preview_title);
			preview_content = (TextView)v.findViewById(R.id.preview_content);
			preview_tags = (TextView)v.findViewById(R.id.preview_tags);
		}

		protected void setItemData() {
			preview_title.setText(item.title);
			preview_content.setText(item.content);
			preview_tags.setText(item.tags);
		}
/*
		@Override
		public Loader<Cursor> onCreateLoader(int id, Bundle args) {
			CursorLoader cl = null;
			if(args != null) {
				contentId = args.getLong("id");
				Uri noteUri = ContentUris.withAppendedId(DreamNoteProvider.ITEMS_CONTENT_URI, contentId);
				cl = new CursorLoader(getActivity(), noteUri, null, null, null, null);
			}
			return cl;
		}
		@Override
		public void onLoadFinished(Loader<Cursor> loader, Cursor result) {
			if(result.moveToFirst()) {
				preview_title.setText(result.getString(result.getColumnIndexOrThrow(ItemsSchema.TITLE)));
				preview_content.setText(result.getString(result.getColumnIndexOrThrow(ItemsSchema.CONTENT)));
				preview_tags.setText(result.getString(result.getColumnIndexOrThrow(ItemsSchema.TAGS)));
			}
			setContentShown(true);
		}
		@Override
		public void onLoaderReset(Loader<Cursor> arg0) {
			// TODO 自動生成されたメソッド・スタブ

		}
*/

		@Override
		public void onAbort() {
			setResult(RESULT_CANCELED, null);
    		FragmentTransaction ft = getSherlockActivity().getSupportFragmentManager().beginTransaction();
			ft.remove(this);
			ft.commit();

//			getSherlockActivity().supportInvalidateOptionsMenu();
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
		public void onPrepareOptionsMenu(Menu menu) {
			super.onPrepareOptionsMenu(menu);
		}

		@Override
		public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
			//Log.d(Constant.LOG_TAG, this.getClass().getSimpleName() + "onCreateOptionsmenu");
			super.onCreateOptionsMenu(menu, inflater);
			EverMenu = menu.findItem(R.id.pre_menu_frag_ever);
			setEvernoteMenuEnabled(EverMenu);

//			inflater.inflate(R.menu.preview_memo_frag_menu, menu);
//			getSherlockActivity().getSupportActionBar().setTitle("");
		}

	    @Override
	    public boolean onOptionsItemSelected(MenuItem item) {
			switch(item.getItemId()) {
			case R.id.pre_menu_frag_del:
				DeleteItemDialogFragment delf = new DeleteItemDialogFragment();
				delf.setArguments(null);
				delf.setResultListener(this);
				delf.show(getSherlockActivity().getSupportFragmentManager(), "DeleteItemDialog");
				break;
			case R.id.pre_menu_frag_edit:
				if(mFragmentControlListener != null) {
					MemoEditFragment f = MemoEditFragment.newUnstance(this.item);
					mFragmentControlListener.onAddNoteFragment(this, f, Notes.EDIT_MEMO_FRAGMENT, "edit");
					return true;
				}
				break;
			case R.id.pre_menu_frag_share:
				Intent intent = new Intent();
				intent.setAction(Intent.ACTION_SEND);
			    intent.setType("text/plain");
			    intent.putExtra(Intent.EXTRA_TEXT, this.item.content);
			    intent.putExtra(Intent.EXTRA_TITLE, this.item.title);
			    intent.putExtra(Intent.EXTRA_SUBJECT, this.item.title);
			    try {
			      startActivity(intent);
			    } catch (android.content.ActivityNotFoundException ex) {
			      Toast.makeText(getSherlockActivity().getApplicationContext(), R.string.err_activity_not_found, Toast.LENGTH_SHORT).show();
			    }
		    	break;
			case R.id.pre_menu_frag_ever:
				Toast.makeText(getSherlockActivity().getApplicationContext(), R.string.please_wait, Toast.LENGTH_LONG).show();
				Intent everintent = new Intent(this.getSherlockActivity().getApplicationContext(), DreamPostEnmlService.class);
				everintent.putExtra("item", this.item);
				this.getSherlockActivity().startService(everintent);
				break;
			default:
				return super.onOptionsItemSelected(item);
			}
	    	return true;
	    }

		@Override
		public void onAddNoteFragment(OnFragmentControlListener listener, Fragment fragment, int fragment_type, String tag) {
		}

		@Override
		public void onFragmentResult(Fragment fragment, int requestCode, int resultCode, Bundle extra) {
			if(requestCode == Notes.EDIT_MEMO_FRAGMENT) {
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
						"memos"
				};
				Context context = getSherlockActivity().getApplicationContext();
				getSherlockActivity().getContentResolver().delete(DreamNoteProvider.ITEMS_CONTENT_URI, null, whereArgs);
				String strResult = context.getResources().getString(R.string.itemtype_memo) + "\n" + item.title + context.getResources().getString(R.string.item_delete_msg_foot);
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
