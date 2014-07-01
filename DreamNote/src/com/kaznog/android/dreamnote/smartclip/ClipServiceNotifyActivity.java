package com.kaznog.android.dreamnote.smartclip;

import java.util.List;

import com.kaznog.android.dreamnote.R;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ClipServiceNotifyActivity extends Activity {
	private final static int PROGRESS_NOTIFIED = 123;
	private ClipServiceInterface binder = null;
	private TextView notify_title_view;
	private TextView notify_text_view;
	private Button notify_cancel_button_view;
	private ProgressBar notify_progress_view;
	private int notifyID;
	private String notify_webtitle;
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //タイトルバー非表示
        requestWindowFeature(Window.FEATURE_NO_TITLE);
		// レイアウト設定
		setContentView(R.layout.clip_service_notify);
		Intent i = this.getIntent();
		if(i == null) {
			finish();
		}
		notifyID = i.getIntExtra("notifyID", -1);
		if(notifyID == -1) {
			finish();
		}
		notify_webtitle = i.getStringExtra("webtitle");
		notify_text_view = (TextView)findViewById(R.id.clip_notify_text);
		notify_text_view.setText(notify_webtitle);
		notify_progress_view = (ProgressBar)findViewById(R.id.clip_notify_progressBar);
		notify_progress_view.setIndeterminate(true);
		notify_progress_view.setProgress(0);
		notify_progress_view.setMax(100);
		notify_title_view = (TextView)findViewById(R.id.clip_notify_title);
		notify_title_view.setText(R.string.clip_service_start_title);
		notify_cancel_button_view = (Button)findViewById(R.id.clip_notify_cancel);
		notify_cancel_button_view.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(binder != null) {
					try {
						binder.cancel(notifyID);
					} catch (RemoteException e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					}
				}
			}
		});
		// サービスインテントの作成
		Intent serviceIntent = new Intent(ClipServiceInterface.class.getName());

		// サービスとの接続
		if(isServiceRunning(ClipService.class.getName())) {
			bindService(serviceIntent, connection, BIND_AUTO_CREATE);
		} else {
			finish();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if(binder != null) {
			try {
				binder.unregisterCallback(callback);
			} catch(RemoteException e) {
			}
		}
		if(isServiceRunning(ClipService.class.getName())) {
			if(connection != null) {
				unbindService(connection);
			}
		}
	}
	private boolean isServiceRunning(String className) {
		ActivityManager am = (ActivityManager)getSystemService(Context.ACTIVITY_SERVICE);
		List<ActivityManager.RunningServiceInfo> serviceInfo = am.getRunningServices(Integer.MAX_VALUE);
		for(int i = 0; i < serviceInfo.size(); i++) {
			if(serviceInfo.get(i).service.getClassName().equals(className)) {
				return true;
			}
		}
		return false;
	}
	private ServiceConnection connection = new ServiceConnection() {
		// サービス接続時に呼ばれる
		public void onServiceConnected(ComponentName name, IBinder service) {
			binder = ClipServiceInterface.Stub.asInterface(service);
			try {
				binder.registerCallback(callback, notifyID);
			} catch(RemoteException e) {
			}
		}
		// サービス切断時に呼ばれる
		public void onServiceDisconnected(ComponentName name) {
			binder = null;
		}
	};
	private Handler updatehandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch(msg.what) {
			case PROGRESS_NOTIFIED:
				if(msg.arg1 == -1) {
//					notify_progress_view.setVisibility(View.GONE);
					notify_progress_view.setIndeterminate(false);
					notify_title_view.setText(R.string.clip_service_failed_title);
					notify_cancel_button_view.setVisibility(View.GONE);
				} else if(msg.arg1 == -2) {
					notify_progress_view.setIndeterminate(false);
					notify_title_view.setText(R.string.clip_service_cancel_title);
					notify_cancel_button_view.setVisibility(View.GONE);
				} else if(msg.arg1 == 100) {
					notify_progress_view.setProgress(msg.arg1);
					notify_progress_view.setIndeterminate(false);
					notify_title_view.setText(R.string.clip_service_complete_title);
					notify_cancel_button_view.setVisibility(View.GONE);
				} else {
					notify_progress_view.setProgress(msg.arg1);
					notify_progress_view.setIndeterminate(false);
				}
				break;
			default:
				super.handleMessage(msg);
				break;
			}
		}
	};
	private ClipServiceCallbackInterface callback = new ClipServiceCallbackInterface.Stub() {
		public void update(int progress) throws RemoteException {
			updatehandler.sendMessage(updatehandler.obtainMessage(PROGRESS_NOTIFIED, progress, 0));
		}
	};
}
