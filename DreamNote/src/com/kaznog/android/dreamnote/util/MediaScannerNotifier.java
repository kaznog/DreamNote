package com.kaznog.android.dreamnote.util;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.media.MediaScannerConnection.MediaScannerConnectionClient;
import android.net.Uri;

public class MediaScannerNotifier implements MediaScannerConnectionClient {
    private MediaScannerConnection mConnection;
    private String mPath;
    private String mMimeType;

    public MediaScannerNotifier(Context context, String path, String mimeType) {
        mPath = path;
        mMimeType = mimeType;
        mConnection = new MediaScannerConnection(context, this);
        mConnection.connect();
    }

	public void onMediaScannerConnected() {
		// TODO 自動生成されたメソッド・スタブ
		mConnection.scanFile(mPath, mMimeType);
	}

	public void onScanCompleted(String arg0, Uri arg1) {
		// TODO 自動生成されたメソッド・スタブ
		mConnection.disconnect();
	}

}
