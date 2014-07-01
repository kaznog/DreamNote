package com.kaznog.android.dreamnote.evernote.html;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.kaznog.android.dreamnote.util.AppInfo;
import com.kaznog.android.dreamnote.util.StringUtils;
import com.kaznog.android.dreamnote.util.extendFileUtils;

import android.content.Context;

public class ArrayListCSS extends ArrayList<CSS> {
	/**
	 *
	 */
	private static final long serialVersionUID = 6970310764294631703L;
	private Context mContext = null;
	public ArrayListCSS(Context context) {
		mContext = context;
	}
	public void reset() {
		Iterator<CSS> iter = this.iterator();
		while(iter.hasNext()) {
			CSS css = iter.next();
			css.reset();
		}
		this.clear();
	}
	public void readCSS(String path) {
		File[] dir = new File(path).listFiles(extendFileUtils.getFileExtensionFilter(".css"));
		if(dir == null) { return; }
		for(File cssfile : dir) {
			String css_content = extendFileUtils.readStringFile(cssfile, "UTF-8");
			this.addCSS(css_content);
		}
	}
	public void addCSS(String css_content) {
		css_content = css_content.replaceAll("\r\n", "\n");
		css_content = css_content.replaceAll("\r", "\n");
		css_content = css_content.replaceAll("\n", " ");
		css_content = css_content.replaceAll("/\\*(.+?)\\*/", "");
		Matcher m = Pattern.compile("([^\\{]+)\\{([^\\}]+)(?:\\}|\\}\\w|\\}\\s)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(css_content);
		while(m.find()) {
			String types = m.group(1);
			types = types.trim();
			types = types.replaceAll("\\s", " ");
			types = types.replaceAll(" > ", ">");
			types = types.replaceAll(" >", ">");
			types = types.replaceAll("> ", ">");
			String styles = m.group(2).trim();
			AppInfo.DebugLog(mContext, "addCSS types: " + types);
			AppInfo.DebugLog(mContext, "addCSS styles: " + styles);
			String[] arrtypes = types.split(",");
			for(String type : arrtypes) {
				type = type.trim();
				if(StringUtils.isBlank(type)) { continue; }
				if(type.indexOf(":") != -1) { continue; }
				this.add(new CSS(type, styles));
			}
		}
	}
}
