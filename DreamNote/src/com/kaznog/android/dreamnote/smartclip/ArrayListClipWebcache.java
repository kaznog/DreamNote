package com.kaznog.android.dreamnote.smartclip;

import java.util.ArrayList;
import java.util.Iterator;

public class ArrayListClipWebcache extends ArrayList<ClipWebcache> {

	/**
	 *
	 */
	private static final long serialVersionUID = 6547178682644389844L;
	public ClipWebcache getCache(String url) {
		ClipWebcache result = null;
		Iterator<ClipWebcache> iter = this.iterator();
		while(iter.hasNext()) {
			ClipWebcache cwc = iter.next();
			if(url.equals(cwc.getUrl())) {
				result = cwc;
				break;
			}
		}
		return result;
	}
}
