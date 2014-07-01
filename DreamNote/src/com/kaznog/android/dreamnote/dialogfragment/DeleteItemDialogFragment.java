package com.kaznog.android.dreamnote.dialogfragment;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.listener.DeleteItemDialogResultListener;

public class DeleteItemDialogFragment extends SherlockDialogFragment {
	private DeleteItemDialogResultListener mResultListener;
	public void setResultListener(DeleteItemDialogResultListener listener) {
		mResultListener = listener;
	}
	DialogInterface.OnClickListener mDialogListener = new DialogInterface.OnClickListener() {
		@Override
		public void onClick(DialogInterface dialog, int which) {
			if(mResultListener != null) {
				mResultListener.onDeleteItemDialogResult(which);
			}
		}
	};
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
		dialogBuilder.setCancelable(false);
		Bundle args = this.getArguments();
		if(args != null) {
			String message = args.getString("message");
			if(message != null) {
				if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
					dialogBuilder.setMessage(message);
					dialogBuilder.setPositiveButton(getResources().getString(R.string.item_delete_both_deletion), mDialogListener);
					dialogBuilder.setNeutralButton(getResources().getString(R.string.item_delete_internal_only), mDialogListener);
				} else {
					dialogBuilder.setMessage(getResources().getString(R.string.item_delete_sdcard_notfound));
					dialogBuilder.setPositiveButton(getResources().getString(R.string.dialog_yes), mDialogListener);
				}
			} else {
				dialogBuilder.setMessage(getResources().getString(R.string.item_delete_confirm_msg));
				dialogBuilder.setPositiveButton(getResources().getString(R.string.dialog_yes), mDialogListener);
			}
		} else {
			dialogBuilder.setMessage(getResources().getString(R.string.item_delete_confirm_msg));
			dialogBuilder.setPositiveButton(getResources().getString(R.string.dialog_yes), mDialogListener);
		}
		dialogBuilder.setNegativeButton(getResources().getString(R.string.delete_cancelbutton_description), null);

		return dialogBuilder.create();
	}

}
