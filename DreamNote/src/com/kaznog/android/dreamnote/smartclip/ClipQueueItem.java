package com.kaznog.android.dreamnote.smartclip;

import java.io.Serializable;

public class ClipQueueItem  implements Serializable {
	/**
	 * クラスシリアル値
	 */
	private static final long serialVersionUID = 8199183517584765381L;
	private String title;
	private String content;
	private String description;
	private String str_user_tags;
	private boolean clipmode;
	private boolean forcemode;
	private String requestUrl;
	private String userAgent;
	private String clippath;
	private ArrayListClipWebcache arrcache = null;
	private int progress;
	private int queueindex;
	public ClipQueueItem() {
		clear();
	}
	public void clear() {
		title = "";
		content = "";
		description = "";
		str_user_tags = "";
		requestUrl = "";
		userAgent = "";
		clippath = "";
		if(arrcache != null) {
			arrcache.clear();
			arrcache = null;
		}
		progress = 0;
		queueindex = 0;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getStr_user_tags() {
		return str_user_tags;
	}
	public void setStr_user_tags(String strUserTags) {
		str_user_tags = strUserTags;
	}
	public boolean isClipmode() {
		return clipmode;
	}
	public void setClipmode(boolean clipmode) {
		this.clipmode = clipmode;
	}
	public boolean isForcemode() {
		return forcemode;
	}
	public void setForcemode(boolean forcemode) {
		this.forcemode = forcemode;
	}
	public String getRequestUrl() {
		return requestUrl;
	}
	public void setRequestUrl(String requestUrl) {
		this.requestUrl = requestUrl;
	}
	public String getUserAgent() {
		return userAgent;
	}
	public void setUserAgent(String userAgent) {
		this.userAgent = userAgent;
	}
	public String getClippath() {
		return clippath;
	}
	public void setClippath(String clippath) {
		this.clippath = clippath;
	}
	public ArrayListClipWebcache getArrcache() {
		return arrcache;
	}
	public void setArrcache(ArrayListClipWebcache arrcache) {
		this.arrcache = arrcache;
	}
	public int getProgress() {
		return progress;
	}
	public void setProgress(int progress) {
		this.progress = progress;
	}
	public void setQueueindex(int queueindex) {
		this.queueindex = queueindex;
	}
	public int getQueueindex() {
		return queueindex;
	}
/*
	public void setNotifyID(int notifyID) {
		this.notifyID = notifyID;
	}
	public int getNotifyID() {
		return notifyID;
	}
*/
}
