package com.kaznog.android.dreamnote.dialogfragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.kaznog.android.dreamnote.R;

public class PhotoChoiceModeSelectorDialogFragment extends
		SherlockDialogFragment {
	public interface PhotoChoiceModeSelectorListener {
		void onPhotoChoiceModeSelected(int mode);
	}
	public static final int CHOICE_MODE_CANCEL = -1;
	public static final int CHOICE_MODE_PICK = 0;
	public static final int CHOICE_MODE_SHOOT = 1;
	private PhotoChoiceModeSelectorListener mPhotoChoiceModeSelectorListener;
	public void setPhotoChoiceModeSelectorListener(PhotoChoiceModeSelectorListener listener) {
		mPhotoChoiceModeSelectorListener = listener;
	}
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		final CharSequence[] mode = {
				getResources().getString(R.string.addphoto_choisemode_pick),
				getResources().getString(R.string.addphoto_choisemode_shoot)
			};
		AlertDialog dialog = new AlertDialog.Builder(getActivity())
		.setItems(mode, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(mPhotoChoiceModeSelectorListener != null) {
					switch(which) {
					case 0:
						mPhotoChoiceModeSelectorListener.onPhotoChoiceModeSelected(CHOICE_MODE_PICK);
						break;
					default:
						mPhotoChoiceModeSelectorListener.onPhotoChoiceModeSelected(CHOICE_MODE_SHOOT);
						break;
					}
				}
			}
		})
		.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				if(mPhotoChoiceModeSelectorListener != null) {
					mPhotoChoiceModeSelectorListener.onPhotoChoiceModeSelected(CHOICE_MODE_CANCEL);
				}
			}
		})
		.create();
		return dialog;
	}
}
