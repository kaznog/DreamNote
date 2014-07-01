package com.kaznog.android.dreamnote.dialogfragment;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.listener.AbortDialogResultListener;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.Toast;

public class AbortDialogFragment extends SherlockDialogFragment {
	private String[] aborttoastmsg;
	private String[] modealerttitle;
	private String[] abortalertmsgs;
	private boolean mode;
	private AbortDialogResultListener mResultListener;
	public void setResultListener(AbortDialogResultListener listener) {
		mResultListener = listener;
	}
	private String getAlertTitle() {
		return modealerttitle[mode ? 0 : 1];
	}
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = this.getArguments();
		if(args == null) return null;
		mode = args.getBoolean("mode");
		aborttoastmsg = args.getStringArray("aborttoastmsg");
		modealerttitle = args.getStringArray("modealerttitle");
		abortalertmsgs = args.getStringArray("abortalertmsgs");
		final String abortmsg = aborttoastmsg[mode ? 0 : 1];
    	AlertDialog dialog = new AlertDialog.Builder(getActivity())
    	.setTitle(getAlertTitle())
    	.setMessage(abortalertmsgs[mode ? 0 : 1])
    	// ダイアログをキャンセルさせない
    	.setCancelable(false)
    	.setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
	    		// 戻ってもよい場合
	    		if(mode) {
	    			Toast.makeText(getActivity(), abortmsg, Toast.LENGTH_LONG).show();
	    		} else {
	    			Toast.makeText(getActivity(), abortmsg, Toast.LENGTH_LONG).show();
	    		}
	    		if(mResultListener != null) {
	    			mResultListener.onAbortDialogResult(android.app.Activity.RESULT_CANCELED);
	    		}
			}
		})
		.setNegativeButton(R.string.dialog_no, null)
    	.create();
    	return dialog;
	}
}
