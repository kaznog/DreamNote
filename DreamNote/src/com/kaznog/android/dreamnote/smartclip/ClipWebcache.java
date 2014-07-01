package com.kaznog.android.dreamnote.smartclip;

import java.io.Serializable;

public class ClipWebcache implements Serializable {
	/**
	 *
	 */
	private static final long serialVersionUID = -5487230021974273858L;
	private String url;
	private String filepath;
	private String mimetype;
	private String encoding;
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getFilepath() {
		return filepath;
	}
	public void setFilepath(String filepath) {
		this.filepath = filepath;
	}
	public String getMimetype() {
		return mimetype;
	}
	public void setMimetype(String mimetype) {
		this.mimetype = mimetype;
	}
	public String getEncoding() {
		return encoding;
	}
	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}
}
