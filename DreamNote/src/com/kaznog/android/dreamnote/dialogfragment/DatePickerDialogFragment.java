package com.kaznog.android.dreamnote.dialogfragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.DatePicker;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.listener.DatePickerDialogListener;
import com.kaznog.android.dreamnote.util.StringUtils;

public class DatePickerDialogFragment extends SherlockDialogFragment {
	private DatePickerDialogListener mDatePickerDialogListener;
	public void setDatePickerDialogListener(DatePickerDialogListener listener) {
		mDatePickerDialogListener = listener;
	}
	private int mYear;
	private int mMonth;
	private int mDay;
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		Bundle args = getArguments();
		if(args == null) return null;
		final Calendar calendar = Calendar.getInstance();
		final String currentDate = args.getString("SelDate");
		if(currentDate.equals(getResources().getString(R.string.todo_nonlimit_description))) {
			mYear = calendar.get(Calendar.YEAR);
			mMonth = calendar.get(Calendar.MONTH);
			mDay = calendar.get(Calendar.DAY_OF_MONTH);
		} else {
			String cDate = currentDate.substring(currentDate.indexOf(" ") + 1, currentDate.lastIndexOf("("));
			String[] arrstrdate = cDate.split("/");
			mYear = Integer.parseInt(arrstrdate[0]);
			mMonth = Integer.parseInt(arrstrdate[1]) - 1;
			mDay = Integer.parseInt(arrstrdate[2]);
		}
		DatePickerDialog datePickerDialog = new DatePickerDialog(getActivity(), SelDateListener, mYear, mMonth, mDay);
		datePickerDialog.setButton(DialogInterface.BUTTON_NEUTRAL, getResources().getString(R.string.todo_nonlimit_description), new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				if(mDatePickerDialogListener != null) {
					mDatePickerDialogListener.onDateSelected(getResources().getString(R.string.todo_nonlimit_description));
				}
			}
		});
		return datePickerDialog;
	}

	// DatePickerDialog の日付が変更されたときに呼び出されるコールバックを登録
	private DatePickerDialog.OnDateSetListener SelDateListener = new DatePickerDialog.OnDateSetListener() {
		public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
			mYear = year;
			mMonth = monthOfYear;
			mDay = dayOfMonth;
			SimpleDateFormat sdf = new SimpleDateFormat(getResources().getString(R.string.todo_limit_format));
			Calendar calendar = Calendar.getInstance();
			calendar.set(Calendar.YEAR, year);
			calendar.set(Calendar.MONTH, monthOfYear);
			calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
			int day_of_week = calendar.get(Calendar.DAY_OF_WEEK) - 1;
			String strdate = sdf.format(calendar.getTime()) + StringUtils.getDayOfWeek(getSherlockActivity().getApplicationContext(), day_of_week);
			if(mDatePickerDialogListener != null) {
				mDatePickerDialogListener.onDateSelected(strdate);
			}
		}
	};
}
