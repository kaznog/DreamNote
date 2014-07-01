package com.kaznog.android.dreamnote.evernote.html;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.kaznog.android.dreamnote.util.StringUtils;

public class CSS {
	private String name;
	private String path;
	private String target;
	private String targettag;
	private String targetclass;
	private String targetid;
	private ArrayList<String> styles = null;
	private ArrayList<String> ids = null;
	private ArrayList<String> classes = null;
	public CSS() {
		name = "";
		setStyles(new ArrayList<String>());
		this.ids = new ArrayList<String>();
		this.classes = new ArrayList<String>();
		this.path = this.buildPath();
	}
	public CSS(String name) {
		this.name = name;
		this.buildtTarget();
		setStyles(new ArrayList<String>());
		this.ids = new ArrayList<String>();
		this.classes = new ArrayList<String>();
		this.path = this.buildPath();
	}
	public CSS(String name, String styles) {
		this.name = name;
		this.buildtTarget();
		this.setStyles(styles);
		this.ids = new ArrayList<String>();
		this.classes = new ArrayList<String>();
		this.path = this.buildPath();
	}
	public CSS(String name, String[] styles) {
		this.name = name;
		this.buildtTarget();
		this.setStyles(styles);
		this.ids = new ArrayList<String>();
		this.classes = new ArrayList<String>();
		this.path = this.buildPath();
	}
	public CSS(String name, ArrayList<String>styles) {
		this.name = name;
		this.buildtTarget();
		this.setStyles(styles);
		this.ids = new ArrayList<String>();
		this.classes = new ArrayList<String>();
		this.path = this.buildPath();
	}
	public boolean isCssClass() {
		if(target.startsWith(".")) {
			return true;
		} else if(target.indexOf(".") != -1) {
			return true;
		}
		return false;
	}
	public boolean isCssId() {
		if(target.startsWith("#")) {
			return true;
		} else if(target.indexOf("#") != -1) {
			return true;
		}
		return false;
	}
	private void buildtTarget() {
		String[] arrname = name.split(" ");
		target = arrname[arrname.length - 1];
		target = target.trim();
		targetclass = "";
		targetid = "";
		targettag = "";
		if(target.indexOf(".") != -1 && target.indexOf("#") == -1) {
			if(target.startsWith(".")) {
				targetclass = target.substring(1);
				targettag = "";
			} else {
				targetclass = target.substring(target.indexOf(".") + 1);
				targettag = target.substring(0, target.indexOf("."));
			}
			targetclass = targetclass.replaceAll("\\.", " ");
			targetid = "";
		} else if(target.indexOf("#") != -1 && target.indexOf(".") == -1) {
			if(target.startsWith("#")) {
				targetid = target.substring(1);
				targettag = "";
			} else {
				targetid = target.substring(target.indexOf("#") + 1);
				targettag = target.substring(0, target.indexOf("#"));
			}
			targetid = targetid.replaceAll("#", " ");
			targetclass = "";
		} else if(target.indexOf("#") != -1 && target.indexOf(".") != -1) {
			int idpos = target.indexOf("#");
			int classpos = target.indexOf(".");
			String querytarget = target.replaceAll("#", " ").replaceAll("\\.", " ");
			int idendpos = querytarget.indexOf(" ", idpos + 1);
			if(idendpos == -1) {
				idendpos = querytarget.length();
			}
			int classendpos = querytarget.indexOf(" ", classpos + 1);
			if(classendpos == -1) {
				classendpos = querytarget.length();
			}
			targetid = querytarget.substring(idpos + 1, idendpos);
			targetid = targetid.replaceAll("#", " ");
			targetclass = querytarget.substring(classpos + 1, classendpos);
			targetclass = targetclass.replaceAll("\\.", " ");
			int endpos = target.indexOf(" ");
			if(idpos == 0 || classpos == 0) {
				targettag = "";
			} else if(endpos == -1) {
				targettag = target;
			} else {
				targettag = target.substring(0, target.indexOf(" "));
			}
		} else {
			targettag = target;
		}
		targettag = targettag.trim();
		targetclass = targetclass.trim();
		targetid = targetid.trim();
	}
	public String getName() {
		return this.name;
	}
	public String getTarget() {
		return this.target;
	}
	public String getTargetId() {
		return this.targetid;
	}
	public String getTargetClass() {
		return this.targetclass;
	}
	public String getTargetTag() {
		return this.targettag;
	}
	public String getPath() {
		return this.path;
	}
	public ArrayList<String> getIds() {
		return this.ids;
	}
	public ArrayList<String> getClasses() {
		return this.classes;
	}
	public String buildPath() {
		Matcher m = null;
		Matcher sm = null;
		String[] arrname = name.split(" ");
		StringBuilder sb = new StringBuilder();
		StringBuffer sbf = new StringBuffer();
		for(String elem : arrname) {
			m = Pattern.compile("([^\\+]+)\\+((|[ ]+)\\w+)", Pattern.CASE_INSENSITIVE).matcher(elem);
			if(m.find()) {
				elem = m.group(1) + "/../" + m.group(2) + "[1]";
			}
			m = Pattern.compile("\\:(nth-child|nth-of-type)", Pattern.CASE_INSENSITIVE).matcher(elem);
			if(m.find()) {
				elem = m.replaceAll("");
			}
			m = Pattern.compile("(#)([^ \\.#]+)", Pattern.CASE_INSENSITIVE).matcher(elem);
			while(m.find()) {
				String in = m.group(2);
				sm = Pattern.compile("([^\\.]+)(\\.)([^ \\.#]+)", Pattern.CASE_INSENSITIVE).matcher(in);
				if(sm.find()) {
					in = sm.group(1).trim();
					String icn = sm.group(3);
					m.appendReplacement(sbf, "['" + in + "'=@id]['" + icn + "'=@class]");
					this.ids.add(in);
					this.classes.add(icn);
				} else {
					m.appendReplacement(sbf, "['" + in + "'=@id]");
					this.ids.add(in);
				}
			}
			m.appendTail(sbf);
			elem = sbf.toString();
			sbf.setLength(0);
			m = Pattern.compile("(\\.)([^ \\.#]+)", Pattern.CASE_INSENSITIVE).matcher(elem);
			while(m.find()) {
				String cn = m.group(2);
//				elem = m.replaceAll("['" + cn + "'=@class]");
				sm = Pattern.compile("([^#]+)(#)([^ \\.#]+)", Pattern.CASE_INSENSITIVE).matcher(cn);
				if(sm.find()) {
					cn = sm.group(1);
					String cin = sm.group(3);
					m.appendReplacement(sbf, "['" + cn + "'=@class]['" + cin + "'=@id]");
					this.classes.add(cn);
					this.ids.add(cin);
				} else {
					m.appendReplacement(sbf, "['" + cn + "'=@class]");
					this.classes.add(cn);
				}
			}
			m.appendTail(sbf);
			elem = sbf.toString();
			sbf.setLength(0);
/*
			if(elem.equals("*")) {
				elem = "";
			} else if(elem.startsWith("[")) {
				elem = "*" + elem;
			}
*/
			if(elem.startsWith("[")) {
				elem = "*" + elem;
			}
			sb.append("/" + elem);
		}
		sb.deleteCharAt(0);
		return sb.toString();
	}
	public void setStyles(String[] styles) {
		if(styles.length > 0) {
			ArrayList<String> setstyles = new ArrayList<String>();
			for(String style: styles) {
				style = style.trim();
				if(StringUtils.isBlank(style)) { continue; }
				setstyles.add(style + ";");
			}
			this.setStyles(setstyles);
		}
	}
	public void setStyles(String styles) {
		String[] setstyles = styles.split(";");
		this.setStyles(setstyles);
	}
	public void setStyles(ArrayList<String> styles) {
		if(this.styles != null) {
			this.styles.clear();
			this.styles = null;
		}
		this.styles = styles;
	}
	public ArrayList<String> getStyles() {
		return styles;
	}
	public void reset() {
		this.classes.clear();
		this.classes = null;
		this.ids.clear();
		this.ids = null;
		this.styles.clear();
		this.styles = null;
		System.gc();
	}
}
