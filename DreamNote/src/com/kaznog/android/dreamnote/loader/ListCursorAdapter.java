package com.kaznog.android.dreamnote.loader;

import java.io.File;
import java.text.SimpleDateFormat;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.db.DreamNoteProvider;
import com.kaznog.android.dreamnote.db.schema.ItemsSchema;
import com.kaznog.android.dreamnote.fragment.ArrayListItem;
import com.kaznog.android.dreamnote.fragment.Item;
import com.kaznog.android.dreamnote.fragment.Notes;
import com.kaznog.android.dreamnote.settings.PreferencesUtil;
import com.kaznog.android.dreamnote.util.Constant;
import com.kaznog.android.dreamnote.util.DreamImageCache;
import com.kaznog.android.dreamnote.util.StringUtils;
import com.kaznog.android.dreamnote.view.CheckableLinearLayout;

import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Environment;
import android.support.v4.widget.ResourceCursorAdapter;
import android.util.DisplayMetrics;
import android.util.SparseBooleanArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class ListCursorAdapter extends ResourceCursorAdapter
implements CheckableLinearLayout.OnCheckedChangeListener {
	final int ITEMTYPE_MEMO = DreamNoteProvider.ITEMTYPE_MEMO;
	final int ITEMTYPE_PHOTO = DreamNoteProvider.ITEMTYPE_PHOTO;
	final int ITEMTYPE_TODO = DreamNoteProvider.ITEMTYPE_TODO;
	final int ITEMTYPE_TODONEW = DreamNoteProvider.ITEMTYPE_TODONEW;
	final int ITEMTYPE_HTML = DreamNoteProvider.ITEMTYPE_HTML;
	final int ITEMTYPE_TODOSEPARATOR = DreamNoteProvider.ITEMTYPE_TODOSEPARATOR;
	private int id;
	private int datatype;
	private String title;
	private String date;
	private String create_date;
	private String tags;
	private String content;
	private String path;
	private String updated;
	private String related;
	static class ViewHolder {
		CheckableLinearLayout rowlayout;
		TextView title;
		TextView description;
		TextView date_published;
		TextView date_created;
		ImageView cateline;
		ImageView image;
		ImageView thumbnail;
		ImageView favicon;
		TextView tagtext;
		LinearLayout taglayout;
	}
	private ListView mListView;
	private SparseBooleanArray mCheckBoxStatus;
	public ListCursorAdapter(Context context, int layout, Cursor c, boolean autoRequery, ListView listview) {
		super(context, layout, c, autoRequery);
		mListView = listview;
		if(c != null) {
			mCheckBoxStatus = new SparseBooleanArray(c.getCount());
		} else {
			mCheckBoxStatus = null;
		}
	}

	@Override
	public Cursor swapCursor(Cursor newCursor) {
		if(mCheckBoxStatus != null) {
			mCheckBoxStatus.clear();
			mCheckBoxStatus = null;
		}
		if(newCursor != null) {
			mCheckBoxStatus = new SparseBooleanArray(newCursor.getCount());
		}
		return super.swapCursor(newCursor);
	}

	@Override
	public boolean isEnabled(int position) {
		Cursor cursor = (Cursor) this.getItem(position);
		int datatype = cursor.getInt(cursor.getColumnIndexOrThrow(ItemsSchema.DATATYPE));
		if(datatype == ITEMTYPE_TODOSEPARATOR) {
			return false;
		}
		return true;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View view = super.getView(position, convertView, parent);
		ViewHolder holder = (ViewHolder)view.getTag();
		holder.rowlayout.setTag(position);
		holder.rowlayout.setChecked(mCheckBoxStatus.get(position, false));
		holder.rowlayout.setOnCheckedChangeListener(this);
		holder.rowlayout.setChecked(isChecked(position));
		return view;
	}

	public int getCheckedCount() {
		int count = 0;
		int size = mCheckBoxStatus.size();
		for(int i = 0; i < size; i++) {
			if(mCheckBoxStatus.valueAt(i)) {
				int key = mCheckBoxStatus.keyAt(i);
				if(mCheckBoxStatus.get(key) == true) {
					count++;
				}
			}
		}
		return count;
	}

	public ArrayListItem getCheckedItem() {
		ArrayListItem list = new ArrayListItem();
		int size = mCheckBoxStatus.size();
		for(int i = 0; i < size; i++) {
			if(mCheckBoxStatus.valueAt(i)) {
				int key = mCheckBoxStatus.keyAt(i);
				if(mCheckBoxStatus.get(key) == true) {
					Cursor c = (Cursor) this.getItem(key);
					Item item = new Item();
					item.id = c.getInt(c.getColumnIndexOrThrow(ItemsSchema.COLUMN_ID));
					item.datatype = c.getInt(c.getColumnIndexOrThrow(ItemsSchema.DATATYPE));
					item.long_date = c.getLong(c.getColumnIndexOrThrow(ItemsSchema.LONG_DATE));
					item.date = c.getString(c.getColumnIndexOrThrow(ItemsSchema.DATE));
					item.long_updated = c.getLong(c.getColumnIndexOrThrow(ItemsSchema.LONG_UPDATED));
					item.updated = c.getString(c.getColumnIndexOrThrow(ItemsSchema.UPDATED));
					item.title = c.getString(c.getColumnIndexOrThrow(ItemsSchema.TITLE));
					item.content = c.getString(c.getColumnIndexOrThrow(ItemsSchema.CONTENT));
					item.description = c.getString(c.getColumnIndexOrThrow(ItemsSchema.DESCRIPTION));
					item.path = c.getString(c.getColumnIndexOrThrow(ItemsSchema.PATH));
					item.related = c.getString(c.getColumnIndexOrThrow(ItemsSchema.RELATED));
					item.long_created = c.getLong(c.getColumnIndexOrThrow(ItemsSchema.LONG_CREATED));
					item.created = c.getString(c.getColumnIndexOrThrow(ItemsSchema.CREATED));
					item.tags = c.getString(c.getColumnIndexOrThrow(ItemsSchema.TAGS));
					item.tags = item.tags.substring(1, item.tags.length() - 1);
					list.add(item);
				}
			}
		}
		return list;
	}

	public boolean isChecked(int position){
		return mCheckBoxStatus.get(position, false);
	}

	public void setChecked(int position,boolean isChecked){
		mCheckBoxStatus.put(position, isChecked);
		this.notifyDataSetChanged();
	}

	public void toggle(int position) {
		setChecked(position, !isChecked(position));
	}

	@Override
	public View newView(Context context, Cursor cursor, ViewGroup viewgroup) {
		View view = super.newView(context, cursor, viewgroup);
		ViewHolder holder = new ViewHolder();
		initViewHolder(holder, view);
		view.setTag(holder);
//		bindView(view, context, cursor);
		return view;
	}

	@Override
	public void bindView(View view, Context context, Cursor cursor) {
		String year;
		ImageView imageview;
		if(Notes.ic == null) {
			Notes.ic = new DreamImageCache();
			DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
			Notes.ic.initialize(mContext, metrics, false);
		} else if(Notes.ic.isInitialized() == false) {
			DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
			Notes.ic.initialize(mContext, metrics, false);
		}
		id = cursor.getInt(cursor.getColumnIndexOrThrow(ItemsSchema.COLUMN_ID));
		datatype = cursor.getInt(cursor.getColumnIndexOrThrow(ItemsSchema.DATATYPE));
		title = cursor.getString(cursor.getColumnIndexOrThrow(ItemsSchema.TITLE));
		date = cursor.getString(cursor.getColumnIndexOrThrow(ItemsSchema.DATE));
		tags = cursor.getString(cursor.getColumnIndexOrThrow(ItemsSchema.TAGS));
		content = cursor.getString(cursor.getColumnIndexOrThrow(ItemsSchema.CONTENT));
		path = cursor.getString(cursor.getColumnIndexOrThrow(ItemsSchema.PATH));
		updated = cursor.getString(cursor.getColumnIndexOrThrow(ItemsSchema.UPDATED));
		related = cursor.getString(cursor.getColumnIndexOrThrow(ItemsSchema.RELATED));
		create_date = cursor.getString(cursor.getColumnIndexOrThrow(ItemsSchema.CREATED));
		boolean thumbnailscale = DreamImageCache.NONSCALING;
		if(datatype == ITEMTYPE_PHOTO || datatype == ITEMTYPE_HTML) {
	        String strlargethumbnail = PreferencesUtil.getPreferences(mContext.getApplicationContext(), Constant.PREFS_LARGETHUMBNAIL, "");
	        if(strlargethumbnail.equals("")) thumbnailscale = DreamImageCache.SCALING;
		}
		ViewHolder holder = (ViewHolder)view.getTag();
		defaultDraw(holder, cursor);
		if(datatype == ITEMTYPE_MEMO) {
			holder.title.setTextColor(0xff999900);
			holder.description.setTextColor(0xff996666);
			holder.cateline.setImageResource(R.drawable.category_line_memo);
			// メモ本文を設定
			holder.description.setVisibility(View.VISIBLE);
			holder.description.setText(content);
			holder.image.setVisibility(View.VISIBLE);
			try {
				holder.image.setImageResource(R.drawable.notebook);
			} catch(OutOfMemoryError e) {
				holder.image.setImageBitmap(null);
			}
			//メモ固有の表示しないものの設定
			holder.thumbnail.setVisibility(View.GONE);
		} else if(datatype == ITEMTYPE_PHOTO) {
			holder.title.setTextColor(0xff009900);
			holder.description.setTextColor(0xff996666);
			holder.cateline.setImageResource(R.drawable.category_line_pic);
			// 画像メモ備考を設定
			holder.description.setVisibility(View.VISIBLE);
			holder.description.setText(content);
			// サムネール描画
			if(thumbnailscale) {
				holder.thumbnail.setVisibility(View.VISIBLE);
				holder.image.setVisibility(View.GONE);
				imageview = holder.thumbnail;
			} else {
				holder.thumbnail.setVisibility(View.GONE);
				holder.image.setVisibility(View.VISIBLE);
				imageview = holder.image;
			}
			Bitmap tempbm = null;
			if(path.equals("") == false) {
				Bitmap image = null;
				if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
					imageview.setTag("photo_thumbnail_" + id);
					// 画像メモの画像を小さくしたサムネール用URLがある場合はイメージキャッシュから取得

					image = Notes.ic.get(path, mListView, imageview, thumbnailscale, null, this);
				}
				if(image != null) {
					// イメージキャッシュから取得できた場合は設定
					imageview.setImageBitmap(image);
				} else {
					// イメージキャッシュからサムネールを取得できなかった場合は
					// 読み込み中を表す「now printing」アイコンを設定
					try {
						imageview.setImageResource(R.drawable.now_printing);
					} catch(OutOfMemoryError e) {
						imageview.setImageBitmap(tempbm);
					}
				}
			} else {
				// 画像メモの画像を小さくしたサムネール用URLがない場合は未設定にする
				imageview.setImageBitmap(tempbm);
			}
		} else if(datatype == ITEMTYPE_TODO) {
			holder.title.setTextColor(0xff666666);
			holder.description.setTextColor(0xff666666);
			holder.cateline.setImageResource(R.drawable.category_line_todo);
			// 期限欄表示文字列の生成と設定
			holder.image.setVisibility(View.VISIBLE);
			try {
				holder.image.setImageResource(R.drawable.todo_done_64);
			} catch(OutOfMemoryError e) {
				holder.image.setImageBitmap(null);
			}
			holder.description.setVisibility(View.VISIBLE);
			year = updated.substring(0, 4);
			if(year.equals("0001")) {
				// 西暦が0001なら期限なしと設定
				holder.description.setText(R.string.todo_nonlimit_description);
				holder.description.setTextColor(Color.GRAY);
			} else {
				SimpleDateFormat sdf = new SimpleDateFormat(mContext.getResources().getString(R.string.todo_item_limit_format));
				String strdate = sdf.format(StringUtils.toDate(updated)) + StringUtils.getDayOfWeek(mContext, StringUtils.toDayOfWeek(updated) - 1);
				holder.description.setText("[" + mContext.getResources().getString(R.string.todo_limit_description) + "] " + strdate);
			}
			//メモ固有の表示しないものの設定
			holder.thumbnail.setVisibility(View.GONE);
		} else if(datatype == ITEMTYPE_TODONEW) {
			holder.title.setTextColor(0xffcc0000);
			holder.description.setTextColor(0xff996666);
			holder.cateline.setImageResource(R.drawable.category_line_todonew);
			// 期限欄表示文字列の生成と設定
			holder.image.setVisibility(View.VISIBLE);
			try {
				holder.image.setImageResource(R.drawable.notepad);
			} catch(OutOfMemoryError e) {
				holder.image.setImageBitmap(null);
			}
			holder.description.setVisibility(View.VISIBLE);
			year = updated.substring(0, 4);
			if(year.equals("0001")) {
				// 西暦が0001なら期限なしと設定
				holder.description.setText(R.string.todo_nonlimit_description);
				holder.description.setTextColor(Color.GRAY);
			} else {
				SimpleDateFormat sdf = new SimpleDateFormat(mContext.getResources().getString(R.string.todo_item_limit_format));
				String strdate = sdf.format(StringUtils.toDate(updated)) + StringUtils.getDayOfWeek(mContext, StringUtils.toDayOfWeek(updated) - 1);
				holder.description.setText("[" + mContext.getResources().getString(R.string.todo_limit_description) + "] " + strdate);
			}
			//メモ固有の表示しないものの設定
			holder.thumbnail.setVisibility(View.GONE);
		} else if(datatype == ITEMTYPE_TODOSEPARATOR) {
			holder.title.setTextColor(Color.WHITE);
		} else if(datatype == ITEMTYPE_HTML) {
			holder.title.setTextColor(0xff009999);
			holder.description.setTextColor(0xff996666);
			holder.cateline.setImageResource(R.drawable.category_line_clip);
			String appName = mContext.getResources().getString(R.string.app_name);
			// クリップの元URL情報を設定
			holder.description.setVisibility(View.VISIBLE);
			holder.description.setText(related);
			final String clipfilepath = Environment.getExternalStorageDirectory().toString() + "/" + appName + "/.clip/" + path;
			holder.favicon.setVisibility(View.VISIBLE);
			if(holder.favicon != null) {
				// ファビコン描画
				Bitmap favicon_image = null;
				if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
					String faviconpath = Environment.getExternalStorageDirectory().toString() + "/" + appName + "/.clip/" + path + "/favicon.png";
					File favicon = new File(faviconpath);
					if(favicon.exists()) {
						holder.favicon.setTag("html_favicon_" + id);
						favicon_image = Notes.ic.get(faviconpath, mListView, holder.favicon, DreamImageCache.NONSCALING, null, this);
					}
				}
				if(favicon_image != null) {
					// イメージキャッシュから取得できた場合は設定
					holder.favicon.setImageBitmap(favicon_image);
				} else {
					// イメージキャッシュから取得できなかった場合は未設定にする
					try {
						holder.favicon.setImageResource(R.drawable.ic_menu_globe);
					} catch(OutOfMemoryError e) {
						holder.favicon.setImageBitmap(null);
					}
				}
			}

			final String thumbnailpath = clipfilepath + "/thumbnail.png";
			// サムネール描画
			if(thumbnailscale) {
				holder.thumbnail.setVisibility(View.VISIBLE);
				holder.image.setVisibility(View.GONE);
				imageview = holder.thumbnail;
			} else {
				holder.thumbnail.setVisibility(View.GONE);
				holder.image.setVisibility(View.VISIBLE);
				imageview = holder.image;
			}
			// クリップ元のURLを保持している場合はサムネールを描画
			Bitmap image = null;
			if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
				File clipdir = new File(clipfilepath);
				if(clipdir.exists()) {
					imageview.setTag(thumbnailpath);

					String data = "<base href=\"" + related + "\" /><title>" + title + "</title><meta name=\"viewport\" content=\"target-densitydpi=device-dpi, width=480\" />" + content;
					File thumbnailfile = new File(thumbnailpath);
					if(thumbnailfile.exists() == false) {
						image = Notes.ic.get(path, mListView, imageview, thumbnailscale, data, this);
					} else {
						image = Notes.ic.get(thumbnailpath, mListView, imageview, thumbnailscale, null, this);
					}
				}
			}
			if(image != null) {
				// イメージキャッシュから取得できた場合はサムネール画像を設定
				imageview.setImageBitmap(image);
			} else {
				// イメージキャッシュからサムネールを取得できなかった場合は
				// 読み込み中を表す「now printing」アイコンを設定
				try {
					imageview.setImageResource(R.drawable.now_printing);
				} catch(OutOfMemoryError e) {
					imageview.setImageBitmap(null);
				}
			}
/*
			Drawable pressed = context.getResources().getDrawable(R.drawable.abs__list_focused_holo );
			Drawable tomei = new ColorDrawable(android.R.color.transparent);
			StateListDrawable d = new StateListDrawable();
			d.addState( new int[]{ android.R.attr.state_pressed }, pressed );
			d.addState( new int[]{ android.R.attr.state_selected }, pressed );
			d.addState( new int[]{ android.R.attr.state_focused }, pressed );
			d.addState( new int[]{ -android.R.attr.state_window_focused }, tomei );
			if(mListView.getChoiceMode() == ListView.CHOICE_MODE_MULTIPLE) {
				d.addState( new int[]{ android.R.attr.state_checked }, pressed );
			} else {
				Drawable checked = context.getResources().getDrawable(R.drawable.selector_mark );
				d.addState( new int[]{ android.R.attr.state_checked }, checked );
			}
			holder.rowlayout.setBackgroundDrawable(d);
*/
		}
	}

	private void initViewHolder(ViewHolder holder, View view) {
		holder.rowlayout = (CheckableLinearLayout)view.findViewById(R.id.item_rowlayout);
		TextView mTitle = (TextView)view.findViewById(R.id.item_title);
		mTitle.setTypeface(Typeface.DEFAULT_BOLD);
		holder.title = mTitle;
		holder.cateline = (ImageView)view.findViewById(R.id.item_category_line);
		holder.date_published = (TextView)view.findViewById(R.id.item_date_published);
		holder.date_created = (TextView)view.findViewById(R.id.item_created);
		holder.tagtext = (TextView)view.findViewById(R.id.tagtext);
		holder.taglayout = (LinearLayout)view.findViewById(R.id.taglayout);
		holder.description = (TextView)view.findViewById(R.id.item_desc);
		holder.image = (ImageView)view.findViewById(R.id.item_image);
		holder.image.setTag("");
		holder.image.setImageBitmap(null);
		holder.thumbnail = (ImageView)view.findViewById(R.id.item_thumbnail);
		holder.thumbnail.setTag("");
		holder.thumbnail.setImageBitmap(null);
		holder.favicon = (ImageView)view.findViewById(R.id.item_favicon);
		holder.favicon.setTag("");
		holder.favicon.setImageBitmap(null);
	}

	private void defaultDraw(ViewHolder holder, Cursor cursor) {
		holder.favicon.setVisibility(View.GONE);

		holder.title.setText(title);
		if(datatype == ITEMTYPE_TODOSEPARATOR) {
			holder.rowlayout.setBackgroundColor(0xffcccccc);
			holder.cateline.setVisibility(View.GONE);
			holder.date_published.setVisibility(View.GONE);
			holder.date_created.setVisibility(View.GONE);
			holder.taglayout.setVisibility(View.GONE);
			holder.description.setVisibility(View.GONE);
			holder.image.setVisibility(View.GONE);
			holder.thumbnail.setVisibility(View.GONE);
		} else {
//			if(datatype == ITEMTYPE_TODO) {
//				holder.rowlayout.setBackgroundColor(0xffcccccc);
//			} else {
//				holder.rowlayout.setBackgroundColor(Color.WHITE);
//			}
			holder.rowlayout.setBackgroundResource(R.drawable.notes_selector);
			holder.cateline.setVisibility(View.VISIBLE);
			if(date.length() > 16) {
				holder.date_published.setText(date.substring(0, 16) + " updated");
			} else {
				holder.date_published.setText("");
			}
			if(create_date.length() > 16) {
				holder.date_created.setText(create_date.substring(0, 16) + " created");
			} else {
				holder.date_created.setText("");
			}
			holder.date_published.setVisibility(View.VISIBLE);
			holder.date_created.setVisibility(View.VISIBLE);
			String tagstext = tags.substring(1, tags.length() - 1);
			if(tagstext.equals("")) {
				holder.taglayout.setVisibility(View.GONE);
			} else {
				holder.taglayout.setVisibility(View.VISIBLE);
				holder.tagtext.setText(tagstext);
			}
		}
	}

	@Override
	public void onCheckedChanged(CheckableLinearLayout rowView, boolean isChecked) {
		mCheckBoxStatus.put((Integer)rowView.getTag(), isChecked);
	}
}
