package com.kaznog.android.dreamnote.fragment;

import java.io.Serializable;

public class Item implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -3521025216187373985L;
	/**
	 * コンテンツのIDを保持
	 */
	public long id;
	/**
	 * コンテンツ種別を保持
	 */
	public int datatype;
	/**
	 * コンテンツのタイトルを保持
	 */
	public String title;
	/**
	 * コンテンツの本文(本体)にあたる情報を保持
	 */
	public String content;
	public String description;
	public String related;
	/**
	 * コンテンツ情報dateのlong値
	 */
	public long long_date;
	/**
	 * コンテンツの更新日時
	 */
	public String date;
	/**
	 * コンテンツのupdated(期限)
	 */
	public String updated;
	/**
	 * コンテンツ情報updatedのlong値
	 */
	public long long_updated;
	/**
	 * コンテンツの登録日時
	 */
	public String created;
	/**
	 * コンテンツの登録日時のlong値
	 */
	public long long_created;
	/**
	 * ユーザー設定タグ文字列配列
	 */
	public String tags;
	/**
	 * コンテンツ固有の保存パス
	 */
	public String path;
	/**
	 * コンストラクタ
	 */
	public Item() {
		id = datatype = -1;
		title = content = description = related = date = updated = created = tags = "";
		long_date = long_updated = long_created = 0;
	}
}
