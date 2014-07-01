package com.kaznog.android.dreamnote.smartclip;

//なぜか同じパッケージなのにimportしないと使えない
import com.kaznog.android.dreamnote.smartclip.ClipServiceCallbackInterface;

interface ClipServiceInterface {
	// 登録
	void registerCallback(ClipServiceCallbackInterface callback, int index);
	// 登録解除
	void unregisterCallback(ClipServiceCallbackInterface callback);
	// クリップ中止
	void cancel(in int index);
}