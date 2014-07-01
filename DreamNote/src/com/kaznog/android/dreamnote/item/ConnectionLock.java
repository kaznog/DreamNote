/**
 *
 */
package com.kaznog.android.dreamnote.item;

/**
 * 通信時に同時コネクションの規制を行うためのクラス
 *
 * サムネール画像取得生成、クリップサムネール生成、サーバーからコンテンツ取得を行う際に
 * 同時に複数の処理を行わせないように、このクラスのパブリック変数「_lock」をsynchronizedさせて
 * 非同期処理を待機させる
 * @author kazuya noguchi
 *
 */
public class ConnectionLock {
	/**
	 * 排他制御オブジェクト
	 */
	public String _lock;
	/**
	 * クラスオブジェクトインスタンスを保持
	 */
	private static ConnectionLock _instance = null;
	/**
	 * インスタンスを取得
	 * @return ConnectionLock クラスのインスタンス
	 */
	public static ConnectionLock getInstance() {
		if(_instance == null) {
			_instance = new ConnectionLock();
		}
		return _instance;
	}
	/**
	 * コンストラクタ
	 *
	 * 排他制御オブジェクトを生成します
	 */
	public ConnectionLock() {
		_lock = new String("connect");
	}
}
