package com.kaznog.android.dreamnote.dialogfragment;

import java.util.ArrayList;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.listener.TagSelectorDialogListener;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

public class TagSelectorDialogFragment extends SherlockDialogFragment {
	private TagSelectorDialogListener mTagSelectorDialogListener;
	public void setOnTagSelectedListener(TagSelectorDialogListener listener) {
		mTagSelectorDialogListener = listener;
	}
	private String tagtext;
	private String[] arrTags = null;
	private ArrayList<String> taglist;
	private boolean[] _selections;
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();
		if(args == null) return null;
		arrTags = args.getStringArray("arrTags");
		taglist = args.getStringArrayList("taglist");
		tagtext = args.getString("tagtext");
		_selections =  new boolean[ arrTags.length ];
		for(int i = 0; i < _selections.length; i++) {
			_selections[i] = false;
		}
    	String tag = tagtext;
    	tag = tag.replaceAll("，", ",");
    	tag = tag.replaceAll(", ", ",");
    	tag = tag.replaceAll("、", ",");
    	tag = tag.replaceAll("､", ",");
    	tag = tag.replaceAll("　", ",");
    	tag = tag.replaceAll(" ", ",");

    	tag = tag.replaceAll(",", ", ");
    	String[] tag_list = tag.split(", ");
    	for(int i = 0; i < tag_list.length; i++) {
    		String t = tag_list[i].trim();
    		int index = taglist.indexOf(t);
    		if(index != -1) {
    			_selections[index] = true;
    		}
    	}
    	AlertDialog dialog = new AlertDialog.Builder(getActivity())
    	.setMultiChoiceItems( arrTags, _selections, new DialogSelectionClickHandler() )
    	.setPositiveButton( R.string.dialog_ok, new DialogButtonClickHandler() )
    	.create();
    	return dialog;
	}

	public class DialogSelectionClickHandler implements DialogInterface.OnMultiChoiceClickListener
	{
		public void onClick( DialogInterface dialog, int clicked, boolean selected )
		{
			//Log.i( "ME", strtaglist[ clicked ] + " selected: " + selected );
		}
	}

	public class DialogButtonClickHandler implements DialogInterface.OnClickListener
	{
		public void onClick( DialogInterface dialog, int clicked )
		{
			switch( clicked )
			{
				case DialogInterface.BUTTON_POSITIVE:
					printSelectedPlanets();
					break;
			}
		}
	}

	protected void printSelectedPlanets(){
		StringBuilder str_sel = new StringBuilder();
		for( int i = 0; i < arrTags.length; i++ ){
			if(_selections[i]) {
				str_sel.append(arrTags[i]);
				str_sel.append(", ");
			}
		}
    	String tag = tagtext.trim();
    	if(tag.equals("") == false) {
        	tag = tag.replaceAll("，", ",");
        	tag = tag.replaceAll(", ", ",");
        	tag = tag.replaceAll("、", ",");
        	tag = tag.replaceAll("､", ",");
        	tag = tag.replaceAll("　", ",");
        	tag = tag.replaceAll(" ", ",");

        	tag = tag.replaceAll(",", ", ");
	    	String[] tag_list = tag.split(", ");
	    	for(String t: tag_list) {
	    		t = t.trim();
	    		if(taglist.indexOf(t) == -1) {
	    			str_sel.append(t);
	    			str_sel.append(", ");
	    		}
	    	}
    	}
		if(str_sel.toString().equals("")) { return; }
		str_sel.deleteCharAt(str_sel.length() - 2);
		if(mTagSelectorDialogListener != null) {
			mTagSelectorDialogListener.onTagSelectorDialogSelected(str_sel.toString());
		}
	}
}
