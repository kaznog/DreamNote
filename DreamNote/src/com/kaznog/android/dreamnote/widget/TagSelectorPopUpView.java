package com.kaznog.android.dreamnote.widget;

import java.util.ArrayList;

import com.actionbarsherlock.R;
import com.actionbarsherlock.internal.view.View_HasStateListenerSupport;
import com.actionbarsherlock.internal.view.View_OnAttachStateChangeListener;
import com.actionbarsherlock.internal.view.menu.ListMenuItemView;
import com.actionbarsherlock.internal.view.menu.MenuBuilder;
import com.actionbarsherlock.internal.view.menu.MenuItemImpl;
import com.actionbarsherlock.internal.view.menu.MenuPresenter;
import com.actionbarsherlock.internal.view.menu.MenuView;
import com.actionbarsherlock.internal.view.menu.SubMenuBuilder;
import com.actionbarsherlock.internal.widget.IcsListPopupWindow;
import com.actionbarsherlock.view.MenuItem;
import com.kaznog.android.dreamnote.listener.TagListItemListener;
import android.content.Context;
import android.content.res.Resources;
import android.os.Parcelable;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.View.MeasureSpec;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ListAdapter;
import android.widget.PopupWindow;

public class TagSelectorPopUpView
implements AdapterView.OnItemClickListener, View.OnKeyListener,
ViewTreeObserver.OnGlobalLayoutListener, PopupWindow.OnDismissListener,
View_OnAttachStateChangeListener, MenuPresenter {
    static final int ITEM_LAYOUT = R.layout.abs__popup_menu_item_layout;

    private Context mContext;
    private LayoutInflater mInflater;
    private IcsListPopupWindow mPopup;
    private MenuBuilder mMenu;
    private int mPopupMaxWidth;
    private View mAnchorView;
    private ViewTreeObserver mTreeObserver;
    private TagListItemListener mTagListItemListener;

    private MenuAdapter mAdapter;

    private Callback mPresenterCallback;

    boolean mForceShowIcon;

    private ViewGroup mMeasureParent;
	public TagSelectorPopUpView(Context context, MenuBuilder menu) {
		this(context, menu, null);
	}
	public TagSelectorPopUpView(Context context, MenuBuilder menu, View anchorView) {
        mContext = context;
        mInflater = LayoutInflater.from(context);
        mMenu = menu;

        final Resources res = context.getResources();
        mPopupMaxWidth = Math.max(res.getDisplayMetrics().widthPixels / 2,
                res.getDimensionPixelSize(R.dimen.abs__config_prefDialogWidth));

        mAnchorView = anchorView;

        menu.addMenuPresenter(this);
	}

	@Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        return false;
    }

    public void setAnchorView(View anchor) {
        mAnchorView = anchor;
    }

    public void setForceShowIcon(boolean forceShow) {
        mForceShowIcon = forceShow;
    }

    public void show() {
        if (!tryShow()) {
            throw new IllegalStateException("MenuPopupHelper cannot be used without an anchor");
        }
    }

    public boolean tryShow() {
        mPopup = new IcsListPopupWindow(mContext, null, R.attr.popupMenuStyle);
        mPopup.setOnDismissListener(this);
        mPopup.setOnItemClickListener(this);

        mAdapter = new MenuAdapter(mMenu);
        mPopup.setAdapter(mAdapter);
        mPopup.setModal(true);

        View anchor = mAnchorView;
        if (anchor != null) {
            final boolean addGlobalListener = mTreeObserver == null;
            mTreeObserver = anchor.getViewTreeObserver(); // Refresh to latest
            if (addGlobalListener) mTreeObserver.addOnGlobalLayoutListener(this);
            ((View_HasStateListenerSupport)anchor).addOnAttachStateChangeListener(this);
            mPopup.setAnchorView(anchor);
        } else {
            return false;
        }

        mPopup.setContentWidth(Math.min(measureContentWidth(mAdapter), mPopupMaxWidth));
        mPopup.setInputMethodMode(PopupWindow.INPUT_METHOD_NOT_NEEDED);
        mPopup.show();
        mPopup.getListView().setOnKeyListener(this);
        return true;
    }

    public void dismiss() {
        if (isShowing()) {
            mPopup.dismiss();
        }
    }

    public void onDismiss() {
        mPopup = null;
        mMenu.close();
        if (mTreeObserver != null) {
            if (!mTreeObserver.isAlive()) mTreeObserver = mAnchorView.getViewTreeObserver();
            mTreeObserver.removeGlobalOnLayoutListener(this);
            mTreeObserver = null;
        }
        ((View_HasStateListenerSupport)mAnchorView).removeOnAttachStateChangeListener(this);
    }

    public boolean isShowing() {
        return mPopup != null && mPopup.isShowing();
    }

    public MenuBuilder getMenu() {
    	return mMenu;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        MenuAdapter adapter = mAdapter;
        //adapter.mAdapterMenu.performItemAction(adapter.getItem(position), 0);
        boolean checked = adapter.mAdapterMenu.getItem(position).isChecked() ? false : true;
        adapter.mAdapterMenu.getItem(position).setChecked(checked);
        mMenu.getItem(position).setChecked(checked);
        if(mTagListItemListener != null) {
        	mTagListItemListener.onTagListItem(adapter.getSelectedItem());
        }
    }


    private int measureContentWidth(ListAdapter adapter) {
        // Menus don't tend to be long, so this is more sane than it looks.
        int width = 0;
        View itemView = null;
        int itemType = 0;
        final int widthMeasureSpec =
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int heightMeasureSpec =
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        final int count = adapter.getCount();
        for (int i = 0; i < count; i++) {
            final int positionType = adapter.getItemViewType(i);
            if (positionType != itemType) {
                itemType = positionType;
                itemView = null;
            }
            if (mMeasureParent == null) {
                mMeasureParent = new FrameLayout(mContext);
            }
            itemView = adapter.getView(i, itemView, mMeasureParent);
            itemView.measure(widthMeasureSpec, heightMeasureSpec);
            width = Math.max(width, itemView.getMeasuredWidth());
        }
        return width;
    }

    @Override
    public void onGlobalLayout() {
        if (isShowing()) {
            final View anchor = mAnchorView;
            if (anchor == null || !anchor.isShown()) {
                dismiss();
            } else if (isShowing()) {
                // Recompute window size and position
                mPopup.show();
            }
        }
    }

    @Override
    public void onViewAttachedToWindow(View v) {
    }

    @Override
    public void onViewDetachedFromWindow(View v) {
        if (mTreeObserver != null) {
            if (!mTreeObserver.isAlive()) mTreeObserver = v.getViewTreeObserver();
            mTreeObserver.removeGlobalOnLayoutListener(this);
        }
        ((View_HasStateListenerSupport)v).removeOnAttachStateChangeListener(this);
    }

    @Override
    public void initForMenu(Context context, MenuBuilder menu) {
        // Don't need to do anything; we added as a presenter in the constructor.
    }

    @Override
    public MenuView getMenuView(ViewGroup root) {
        throw new UnsupportedOperationException("MenuPopupHelpers manage their own views");
    }

    @Override
    public void updateMenuView(boolean cleared) {
        if (mAdapter != null) mAdapter.notifyDataSetChanged();
    }

    @Override
    public void setCallback(Callback cb) {
        mPresenterCallback = cb;
    }

    @Override
    public boolean onSubMenuSelected(SubMenuBuilder subMenu) {
        if (subMenu.hasVisibleItems()) {
        	TagSelectorPopUpView subPopup = new TagSelectorPopUpView(mContext, subMenu, mAnchorView);
            subPopup.setCallback(mPresenterCallback);

            boolean preserveIconSpacing = false;
            final int count = subMenu.size();
            for (int i = 0; i < count; i++) {
                MenuItem childItem = subMenu.getItem(i);
                if (childItem.isVisible() && childItem.getIcon() != null) {
                    preserveIconSpacing = true;
                    break;
                }
            }
            subPopup.setForceShowIcon(preserveIconSpacing);

            if (subPopup.tryShow()) {
                if (mPresenterCallback != null) {
                    mPresenterCallback.onOpenSubMenu(subMenu);
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public void onCloseMenu(MenuBuilder menu, boolean allMenusAreClosing) {
        // Only care about the (sub)menu we're presenting.
        if (menu != mMenu) return;

        dismiss();
        if (mPresenterCallback != null) {
            mPresenterCallback.onCloseMenu(menu, allMenusAreClosing);
        }
    }

    @Override
    public boolean flagActionItems() {
        return false;
    }

    public boolean expandItemActionView(MenuBuilder menu, MenuItemImpl item) {
        return false;
    }

    public boolean collapseItemActionView(MenuBuilder menu, MenuItemImpl item) {
        return false;
    }

    @Override
    public int getId() {
        return 0;
    }

    @Override
    public Parcelable onSaveInstanceState() {
        return null;
    }

    @Override
    public void onRestoreInstanceState(Parcelable state) {
    }

    public void setTagList(MenuBuilder menu) {
    	if(mMenu != null) {
    		mMenu.close();
    		mMenu = null;
    	}
    	mMenu = menu;
    	if(mAdapter != null) {
	    	mAdapter.setAdapterMenu(menu);
	    	mAdapter.notifyDataSetChanged();
    	}
    }

    public void setTagListItemListener(TagListItemListener listener) {
    	mTagListItemListener = listener;
    }

    private class MenuAdapter extends BaseAdapter {
    	private MenuBuilder mAdapterMenu;
    	public MenuAdapter(MenuBuilder menu) {
            mAdapterMenu = menu;
        }
    	public void setAdapterMenu(MenuBuilder menu) {
    		if(mAdapterMenu != null) {
    			mAdapterMenu.close();
    			mAdapterMenu = null;
    		}
    		mAdapterMenu = menu;
    	}

    	public String[] getSelectedItem() {
    		ArrayList<String> arrTags = new ArrayList<String>();
    		int size = mAdapterMenu.size();
    		for(int i = 0; i < size; i++) {
    			MenuItem menu = mAdapterMenu.getItem(i);
    			if(menu.isChecked()) {
    				arrTags.add(menu.getTitle().toString());
    			}
    		}
    		return arrTags.toArray(new String[0]);
    	}

        public int getCount() {
            return mAdapterMenu.size();
        }

        public MenuItemImpl getItem(int position) {
            return (MenuItemImpl) mAdapterMenu.getItem(position);
        }

        public long getItemId(int position) {
            // Since a menu item's ID is optional, we'll use the position as an
            // ID for the item in the AdapterView
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(ITEM_LAYOUT, parent, false);
            }

            MenuView.ItemView itemView = (MenuView.ItemView) convertView;
            if (mForceShowIcon) {
                ((ListMenuItemView) convertView).setForceShowIcon(true);
            }
            itemView.initialize(getItem(position), 0);
            return convertView;
        }
    }
}
