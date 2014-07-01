package com.kaznog.android.dreamnote.evernote;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.evernote.client.conn.ApplicationInfo;
import com.evernote.client.conn.mobile.FileData;
import com.evernote.client.oauth.android.AuthenticationResult;
import com.evernote.client.oauth.android.EvernoteSession;
import com.evernote.edam.notestore.NoteStore;
import com.evernote.edam.type.Note;
import com.evernote.edam.type.NoteAttributes;
import com.evernote.edam.type.Resource;
import com.evernote.edam.userstore.UserStore;
import com.evernote.edam.util.EDAMUtil;
import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.db.DreamNoteProvider;
import com.kaznog.android.dreamnote.evernote.html.ArrayListCSS;
import com.kaznog.android.dreamnote.evernote.html.BaseToken;
import com.kaznog.android.dreamnote.evernote.html.CSS;
import com.kaznog.android.dreamnote.evernote.html.CommentNode;
import com.kaznog.android.dreamnote.evernote.html.HtmlNode;
import com.kaznog.android.dreamnote.evernote.html.PostEnmlException;
import com.kaznog.android.dreamnote.evernote.html.Utils;
import com.kaznog.android.dreamnote.fragment.Item;
import com.kaznog.android.dreamnote.fragment.Notes;
import com.kaznog.android.dreamnote.settings.PreferencesUtil;
import com.kaznog.android.dreamnote.util.AppInfo;
import com.kaznog.android.dreamnote.util.Base64;
import com.kaznog.android.dreamnote.util.Constant;
import com.kaznog.android.dreamnote.util.StringUtils;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.text.format.DateFormat;
import android.widget.Toast;

public class DreamPostEnmlService extends IntentService {
	private static final String ACTION_VIEW_NOTE = "com.evernote.action.VIEW_NOTE";
	private static final String EXTRA_NOTE_GUID = "NOTE_GUID";
	private static final String EXTRA_FULL_SCREEN = "FULL_SCREEN";
	// The prefix for an ENEX file, up to the note title
	// The ENML prefix for note content
	private static final String NOTE_PREFIX =
	"<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
	"<!DOCTYPE en-note SYSTEM \"http://xml.evernote.com/pub/enml2.dtd\">" +
	"<en-note>";
	private static final String TABLE_PREFIX = "<table border=\"0\" cellspacing=\"0\" cellpadding=\"0\">";
	private static final String TABLE_SUFFIX = "</table>";
	// The ENML postamble to every Evernote note
	private static final String NOTE_SUFFIX = "</en-note>";
	public final static int DEFAULT_SLEEP_TIME = 200;
	private Context mContext;
	private String appName;
	private String appPath;
	private Item item;
	private ArrayList<String> arrItem = null;
	private long oldtime = 0;
	private long currenttime = 0;
	private CleanerProperties props = null;
    private List<String> indents = null;
	private static final String DEFAULT_INDENTATION_STRING = "\t";
    private String indentString = DEFAULT_INDENTATION_STRING;
	// Used to interact with the Evernote web service
	private EvernoteSession session;
	private UserStore.Client userStore;
	private NoteStore.Client noteStore;
	private String ToDosTitle = "";

	public class ShowToast implements Runnable {
        private Context mContext;
        private String msg;
        public ShowToast(Context context, String msg) {
            mContext = context;
            this.msg = msg;
        }
		public void run() {
			Toast.makeText(mContext, msg, Toast.LENGTH_SHORT).show();
		}
    }
	public DreamPostEnmlService(String name) {
		super(name);
	}
	public DreamPostEnmlService() {
		super("PostEnmlService");
	}
	public void setUserStore(UserStore.Client userStore) {
		this.userStore = userStore;
	}
	public UserStore.Client getUserStore() {
		return userStore;
	}
	public void setNoteStore(NoteStore.Client noteStore) {
		this.noteStore = noteStore;
	}
	public NoteStore.Client getNoteStore() {
		return noteStore;
	}

	/**
	 * Set up communications with the Evernote web service API, including
	 * authenticating the user.
	 */
	private void setupSession(Context context) {
        // アプリケーション名を取得
        String appName = context.getResources().getString(R.string.app_name);
        String appVersion = AppInfo.getVersionString(context);
	    ApplicationInfo info =
	    	      new ApplicationInfo(Constant.EN_OAUTH_CONSUMER_KEY, Constant.EN_OAUTH_CONSUMER_SECRET, Constant.EVERNOTE_HOST,
	    	          appName, appVersion);
	    String strAuthToken = PreferencesUtil.getPreferences(context, Constant.PREFS_EN_OAUTH_TOKEN, "");
	    String strNoteStoreUrl = PreferencesUtil.getPreferences(context, Constant.PREFS_EN_OAUTH_NOTE_STORE_URL, "");
	    String strWebApiUrlPrefix = PreferencesUtil.getPreferences(context, Constant.PREFS_EN_OAUTH_WEB_API_URL_PREFIX, "");
	    int UserId = PreferencesUtil.getPreferences(context, Constant.PREFS_EN_OAUTH_ID, 0);
	    if (strAuthToken.length() > 0 && strNoteStoreUrl.length() > 0 && strWebApiUrlPrefix.length() > 0 && (UserId > 0)) {
			PreferencesUtil.setPreferences(context, Constant.KEY_AUTHTOKEN, strAuthToken);
			PreferencesUtil.setPreferences(context, Constant.KEY_NOTESTOREURL, strNoteStoreUrl);
			PreferencesUtil.setPreferences(context, Constant.KEY_WEBAPIURLPREFIX, strWebApiUrlPrefix);
			PreferencesUtil.setPreferences(context, Constant.KEY_USERID, UserId);
	    }
	    if(strAuthToken.equals("") == false) {
	    	//AuthenticationResult result = new AuthenticationResult(strAuthToken, strNoteStoreUrl, strWebApiUrlPrefix, UserId);
	    	session = new EvernoteSession(info, PreferencesUtil.getSharedPreferences(getApplicationContext()), getTempDir(context));
	    } else {
	    	session = new EvernoteSession(info, getTempDir(context));
	    }
	}

	private File getTempDir(Context context) {
		return new File(AppInfo.getAppPath(context) + "/.cache");
	}

	public boolean setupApi() {
		// You can also use EDAMUtil.getUserStoreClient() to build a UserStore.client
		try {
			setupSession(mContext);
			if(session == null) {
				throw new IllegalStateException();
			}
			if(session.isLoggedIn() == false) {
				if(!session.completeAuthentication(PreferencesUtil.getSharedPreferences(mContext))) {
					throw new IllegalStateException();
				}
			}
			setNoteStore(session.createNoteStore());
		} catch (Exception e) {
			if(this.item != null) {
				setFailedNotification(mContext.getResources().getString(R.string.evernote_err_api_setup));
			}
			return false;
		}
		return true;
	}

	@Override
	protected void onHandleIntent(Intent intent) {
		oldtime = System.currentTimeMillis();

		// アプリケーション名を取得
        mContext = this.getApplicationContext();
        appName = mContext.getResources().getString(R.string.app_name);
        appPath = Environment.getExternalStorageDirectory().toString() + "/" + appName;
		this.item = (Item)intent.getSerializableExtra("item");
		try {
			if(setupApi() == false) {
				return;
			}
			diffsleep(DEFAULT_SLEEP_TIME);
			if(this.item != null) {
				if(this.item.datatype == DreamNoteProvider.ITEMTYPE_HTML) {
					if(isPicClip()) {
						createPicEnexFile();
					} else {
						boolean mode = intent.getBooleanExtra("mode", false);
						createHtmlEnexFile(mode);
					}
				} else if(this.item.datatype == DreamNoteProvider.ITEMTYPE_PHOTO) {
					createPhotoEnexFile();
				} else if(this.item.datatype == DreamNoteProvider.ITEMTYPE_MEMO) {
					createMemoEnexFile();
				} else if(this.item.datatype == DreamNoteProvider.ITEMTYPE_TODO
					   || this.item.datatype == DreamNoteProvider.ITEMTYPE_TODONEW
				) {
					createToDoEnexFile();
				}
			} else {
				if(arrItem != null) {
					arrItem.clear();
					arrItem = null;
				}
				ToDosTitle = "";
				ToDosTitle = (String)intent.getStringExtra("todotitle");
				if(ToDosTitle == null) {
					Date now = new Date();
					SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
					String strdate = sdf.format(now);
					ToDosTitle = "ToDo " + strdate;
				}
				arrItem = intent.getStringArrayListExtra("arritem");
				createToDosEnexFile(ToDosTitle);
			}
		} catch(InterruptedException e) {
			setFailedNotification(mContext.getResources().getString(R.string.postever_failed_create_note));
		}
	}
	private String addToDoItem(boolean addtitle, String content, ArrayList<String> arrtags, Item todoitem) {
		String checked = "";
		if(todoitem.datatype == DreamNoteProvider.ITEMTYPE_TODO) {
			checked = " checked=\"true\"";
		}
		content += "<en-todo" + checked + "/>";
		if(addtitle || todoitem.content.trim().length() == 0) {
			content += todoitem.title + "<br/>";
		}
		if(todoitem.content.trim().length() != 0) {
			content += todoitem.content + "<br/>";
		}
        if(todoitem.tags != null && todoitem.equals("") == false) {
	        String[] arrString = todoitem.tags.split(",");
	        for(String tag: arrString) {
	        	tag = tag.trim();
	        	if(tag.equals("")) continue;
	        	if(arrtags.indexOf(tag) != -1) continue;
	        	arrtags.add(tag);
	        }
        }
        return content;
	}
	private void createToDoEnexFile() throws InterruptedException {
		try {
	        // Create a new Note
	        Note note = new Note();
	        note.setTitle(item.title);
	        ArrayList<String> arrtags = new ArrayList<String>();
	        String content = NOTE_PREFIX;
	        content = addToDoItem(false, content, arrtags, item);
	        content += NOTE_SUFFIX;;
	        note.setContent(content);
			note.setTagNames(arrtags);

			NoteAttributes attr = new NoteAttributes();
			attr.setSourceApplication(appName);
			attr.setSourceURL("");
			note.setAttributes(attr);
			Note createdNote = getNoteStore().createNote(session.getAuthToken(), note);
			setCreatedNotification(createdNote.getGuid());
		} catch(Exception e) {
			setFailedNotification(mContext.getResources().getString(R.string.postever_failed_create_note));
		}
	}
	private void createToDosEnexFile(String title) throws InterruptedException {
		try {
			Item todoitem = new Item();
	        // Create a new Note
	        Note note = new Note();
	        note.setTitle(title);
	        ArrayList<String> arrtags = new ArrayList<String>();
	        String content = NOTE_PREFIX;
	        Iterator<String> todoiter = arrItem.iterator();
	        while(todoiter.hasNext()) {
	        	String todotitle = todoiter.next();
	        	String todocontent = todoiter.next();
	        	int datatype = Integer.parseInt(todoiter.next());
	        	String tags = todoiter.next();
	        	todoitem.title = todotitle;
	        	todoitem.content = todocontent;
	        	todoitem.datatype = datatype;
	        	todoitem.tags = tags;
	        	if(todoitem.datatype == DreamNoteProvider.ITEMTYPE_TODO
	        	|| todoitem.datatype == DreamNoteProvider.ITEMTYPE_TODONEW) {
	        		content = addToDoItem(true, content, arrtags, todoitem);
	        	}
	        }
	        content += NOTE_SUFFIX;;
	        note.setContent(content);
			note.setTagNames(arrtags);

			NoteAttributes attr = new NoteAttributes();
			attr.setSourceApplication(appName);
			attr.setSourceURL("");
			note.setAttributes(attr);
			Note createdNote = getNoteStore().createNote(session.getAuthToken(), note);
			setCreatedNotification(createdNote.getGuid());
		} catch(Exception e) {
			setFailedNotification(mContext.getResources().getString(R.string.postever_failed_create_note));
		} finally {
			if(arrItem != null) {
				arrItem.clear();
				arrItem = null;
			}
		}
	}
	private void createMemoEnexFile() throws InterruptedException {
		try {
	        // Create a new Note
	        Note note = new Note();
	        note.setTitle(item.title);
	        String content = NOTE_PREFIX;
	        if(StringUtils.isBlank(item.content) == false) {
	        	content += "<p>" + item.content + "</p>";
	        }
	        content += NOTE_SUFFIX;;
	        note.setContent(content);
	        ArrayList<String> arrtags = new ArrayList<String>();
	        if(item.tags != null && item.equals("") == false) {
	        	AppInfo.DebugLog(mContext, "createMemoEnex tags create: " + item.tags);
		        String[] arrString = item.tags.split(",");
		        for(String tag: arrString) {
		        	tag = tag.trim();
		        	if(tag.equals("")) continue;
		        	if(arrtags.indexOf(tag) != -1) continue;
		        	AppInfo.DebugLog(mContext, "createMemoEnex add tags:" + tag);
		        	arrtags.add(tag);
		        }
	        }
			note.setTagNames(arrtags);
			AppInfo.DebugLog(mContext, "add tags");
			NoteAttributes attr = new NoteAttributes();
			attr.setSourceApplication(appName);
			attr.setSourceURL(item.related);
			note.setAttributes(attr);
			Note createdNote = getNoteStore().createNote(session.getAuthToken(), note);
			setCreatedNotification(createdNote.getGuid());
		} catch(Exception e) {
			setFailedNotification(mContext.getResources().getString(R.string.postever_failed_create_note));
			return;
		}
	}
	private void createPhotoEnexFile() throws InterruptedException {
        InputStream in;
		try {
			in = new BufferedInputStream(new FileInputStream(item.path));
	        FileData data = new FileData(EDAMUtil.hash(in), new File(item.path));
	        in.close();
			// 取得ファイル拡張子取得
			String ext = item.path.substring(item.path.lastIndexOf(".") + 1);
			if(ext.equals("jpg") || ext.equals("JPG")) {
				ext = "jpeg";
			}
			String mediatype = "image/" + ext;
	        Resource resource = new Resource();
	        resource.setData(data);
	        resource.setMime(mediatype);
	        // Create a new Note
	        Note note = new Note();
	        note.setTitle(item.title);
	        note.addToResources(resource);
	        String content = NOTE_PREFIX;
	        if(StringUtils.isBlank(item.content) == false) {
	        	content += "<p>" + item.content + "</p>";
	        }
	        content += "<en-media type=\"" + mediatype + "\" hash=\"" +
	            EDAMUtil.bytesToHex(resource.getData().getBodyHash()) + "\"/>" +
	            NOTE_SUFFIX;
	        note.setContent(content);
	        ArrayList<String> arrtags = new ArrayList<String>();
	        if(item.tags != null && item.equals("") == false) {
		        String[] arrString = item.tags.split(",");
		        for(String tag: arrString) {
		        	tag = tag.trim();
		        	if(tag.equals("")) continue;
		        	if(arrtags.indexOf(tag) != -1) continue;
		        	arrtags.add(tag);
		        }
	        }
			note.setTagNames(arrtags);
			AppInfo.DebugLog(mContext, "add tags");
			NoteAttributes attr = new NoteAttributes();
			attr.setSourceApplication(appName);
			attr.setSourceURL(item.related);
			note.setAttributes(attr);
			Note createdNote = getNoteStore().createNote(session.getAuthToken(), note);
			setCreatedNotification(createdNote.getGuid());
		} catch (Exception e) {
			setFailedNotification(mContext.getResources().getString(R.string.postever_failed_create_note));
			return;
		}

	}
	private void diffsleep(long sleeptime) throws InterruptedException {
		currenttime = System.currentTimeMillis();
		long diff = currenttime - oldtime;
		if(diff > 3000) {
			oldtime = currenttime;
			Thread.sleep(sleeptime);
		}
	}
	private boolean isPicClip() {
		File clipdir = new File(appPath +"/.clip/" + item.path);
		if(clipdir.exists() == false) return false;

		String[] filenames = clipdir.list();
		ArrayList<String> arrfiles = new ArrayList<String>(Arrays.asList(filenames));
		if(arrfiles.indexOf("clipbody0.jpg") == -1) return false;
		return true;
	}
	public void createHtmlEnexFile(boolean mode) throws InterruptedException {
		// enexの準備
		Matcher m = null;
		String content = readStringFile(new File(appPath + "/.clip/" + item.path + "/index.html"), "UTF-8");
		if(mode && checkXmlns(content)) {
//			this.mHandler.post(new ShowToast(PostEnmlService.this, mContext.getResources().getString(R.string.postever_xmlns_not_supported)));
			setFailedNotification(mContext.getResources().getString(R.string.postever_xmlns_not_supported));
			return;
		}
		ArrayListCSS arrcss = null;
		if(mode) {
			// CSS 構造の生成
			arrcss = new ArrayListCSS(mContext);
			arrcss.readCSS(appPath + "/.clip/" + item.path);
			m = Pattern.compile("(<style[^>]+)>(.+?)(</style>)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(content);
			while(m.find()) {
				String style = m.group(2);
				if(style.startsWith("@-webkit-keyframes")) { continue; }
				arrcss.addCSS(style);
			}
		}
		diffsleep(DEFAULT_SLEEP_TIME);
		content = eraseTag(content, "head>", "</head");
		content = eraseDummyTag(content,
								new String[] {
									"style",
									"script",
									"iframe",
									"ilayer",
									"noscript",
									"object",
									"applet",
									"embed",
									"bgsound"
								}
		);
		content = eraseTag(content, "style>", "</style");
		content = eraseTag(content, "style", "</style");
		content = eraseTag(content, "<style(\"[^\"]*\"|'[^']*'|[^'\">])*/>");
		content = eraseTag(content, "script>", "</script");
		content = eraseTag(content, "script", "</script");
		content = eraseTag(content, "<script(\"[^\"]*\"|'[^']*'|[^'\">])*/>");
		System.gc();
		diffsleep(DEFAULT_SLEEP_TIME);

		// HTMLを正常系に修正
		HtmlCleaner cleaner = new HtmlCleaner();
		props = cleaner.getProperties();
		props.setBooleanAttributeValues(CleanerProperties.BOOL_ATT_SELF);
		props.setOmitUnknownTags(true);
		props.setOmitComments(true);
		props.setOmitDeprecatedTags(true);
		props.setUseEmptyElementTags(false);
		props.setNamespacesAware(false);

		TagNode rootnode = cleaner.clean(content);
		diffsleep(DEFAULT_SLEEP_TIME);

		content = "";
		String html = "";
		try {
			if(mode) {
				replaseStyle3(rootnode, arrcss);
				arrcss.reset();
				arrcss = null;
				System.gc();
			}
			String enmlext = ".enml";
			String uniqname = DateFormat.format("yyyyMMddkkmmss", System.currentTimeMillis()).toString();
			String enmlname = appPath + "/.cache/temp_" + uniqname + enmlext;
	    	File nodefile = new File(enmlname);
			if(nodefile.exists()) {
				nodefile.delete();
			}
		    indents = new ArrayList<String>();
	        write( rootnode, new OutputStreamWriter(new FileOutputStream(nodefile), "UTF-8"), "UTF-8", false );
	        indents.clear();
	        indents = null;
	        html = readStringFile(nodefile, "UTF-8");
			if(nodefile.exists()) {
				nodefile.delete();
			}
		} catch (Exception e) {
			setFailedNotification(mContext.getResources().getString(R.string.postever_failed_create_note));
			return;
		}
		cleaner = null;
		rootnode.removeAllChildren();
		rootnode = null;
		props = null;
		diffsleep(DEFAULT_SLEEP_TIME);
		System.gc();
		m = Pattern.compile("<(/|)(FORM|INPUT|BUTTON|LABEL|LAYER|MARQUEE|MENU|ISINDEX|FRAME|FRAMESET|BASEFONT|BGSOUND|BASE|PLAINTEXT|NOBR)(\"[^\"]*\"|'[^']*'|[^'\">])*>", Pattern.CASE_INSENSITIVE).matcher(html);
		if(m.find()) {
			html = m.replaceAll("");
		}
		diffsleep(DEFAULT_SLEEP_TIME);
		m = Pattern.compile("(href=)(\"\"|'')", Pattern.CASE_INSENSITIVE).matcher(html);
		if(m.find()) {
			html = m.replaceAll("");
		}
		diffsleep(DEFAULT_SLEEP_TIME);
		System.gc();

		Matcher m64 = null;
		StringBuffer sb = new StringBuffer();
		m = Pattern.compile("(href=)(['\"])(.+?)(?:\\2)", Pattern.CASE_INSENSITIVE).matcher(html);
		while(m.find()) {
			if(m.group(3).startsWith("javascript:")) {
				m.appendReplacement(sb, "");
			}
			if(m.group(3).indexOf(":") == -1) {
				m.appendReplacement(sb, "");
			}
		}
		m.appendTail(sb);
		html = sb.toString();
		sb.setLength(0);
		diffsleep(DEFAULT_SLEEP_TIME);
		System.gc();

		initDTDmap();
		html = eraseAttrTag(html);
		html = setEndTag(html);
		dtdmap.clear();
		dtdmap = null;
		diffsleep(DEFAULT_SLEEP_TIME);
		System.gc();

		m = Pattern.compile("(src=)(['\"])(.+?)(?:\\2)", Pattern.CASE_INSENSITIVE).matcher(html);
		while(m.find()) {
			String filename = m.group(3);
			// 取得ファイル拡張子取得
			String ext = filename.substring(filename.lastIndexOf(".") + 1);
			m64 = Pattern.compile("(data:image\\/.*?base64|google.com\\/.*\\/data=)", Pattern.CASE_INSENSITIVE).matcher(filename);
			if(m64.find() == false
			&& (ext.equals("jpg") || ext.equals("gif") || ext.equals("png"))
			) {
				String f = appPath + "/.clip/" + item.path + "/" + filename;
				if(new File(f).exists()) {
					try {
						FileInputStream fi = new FileInputStream(f);
						ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
						byte[] block = new byte[10240];
						int len;
						while ((len = fi.read(block)) >= 0) {
						  byteOut.write(block, 0, len);
						}
						String base64body = Base64.encodeBytes(byteOut.toByteArray());
						m.appendReplacement(sb, m.group(1) + m.group(2) + "data:image/" + ext + ";base64," + base64body + m.group(2));
						byteOut.reset();
						byteOut = null;
						fi.close();
						fi = null;
						block = null;
						diffsleep(DEFAULT_SLEEP_TIME);
						System.gc();
					} catch (Exception e) {
//						e.printStackTrace();
						setFailedNotification(mContext.getResources().getString(R.string.postever_failed_create_note));
						return;
					}
				}
			}
		}
		m.appendTail(sb);
		html = sb.toString();
		sb.setLength(0);
		diffsleep(DEFAULT_SLEEP_TIME);
		System.gc();

		m = Pattern.compile("(url)(\\()(.+?)(\\))", Pattern.CASE_INSENSITIVE).matcher(html);
		while(m.find()) {
			String filename = m.group(3);
			// 取得ファイル拡張子取得
			String ext = filename.substring(filename.lastIndexOf(".") + 1);
			m64 = Pattern.compile("(data:image\\/.*?base64|google.com\\/.*\\/data=)", Pattern.CASE_INSENSITIVE).matcher(filename);
			if(m64.find() == false
			&& (ext.equals("jpg") || ext.equals("gif") || ext.equals("png"))
			) {
				String f = appPath + "/.clip/" + item.path + "/" + filename;
				if(new File(f).exists()) {
					try {
						FileInputStream fi = new FileInputStream(f);
						ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
						byte[] block = new byte[10240];
						int len;
						while ((len = fi.read(block)) >= 0) {
						  byteOut.write(block, 0, len);
						}
						String base64body = Base64.encodeBytes(byteOut.toByteArray());
						m.appendReplacement(sb, m.group(1) + m.group(2) + "data:image/" + ext + ";base64," + base64body + ")");
						byteOut.reset();
						byteOut = null;
						fi.close();
						fi = null;
						block = null;
						diffsleep(DEFAULT_SLEEP_TIME);
						System.gc();
					} catch (Exception e) {
//						e.printStackTrace();
						setFailedNotification(mContext.getResources().getString(R.string.postever_failed_create_note));
						return;
					}
				}
			}
		}
		m.appendTail(sb);
		html = "";
		System.gc();
		StringBuilder edamcontent = new StringBuilder();
		edamcontent.append(NOTE_PREFIX);
		if(StringUtils.isBlank(item.description) == false) {
			String desc = "<div>" + item.description.replaceAll("\n", "<br/>") + "</div><div><hr/></div>";
			edamcontent.append(desc);
		}
		diffsleep(DEFAULT_SLEEP_TIME);
		sb.insert(0, edamcontent.toString());
		diffsleep(DEFAULT_SLEEP_TIME);
		sb.append(NOTE_SUFFIX);
		edamcontent.setLength(0);
		edamcontent = null;
		System.gc();
		diffsleep(DEFAULT_SLEEP_TIME);
		diffsleep(DEFAULT_SLEEP_TIME);
		try {
			Note note = new Note();
			String title = item.title;
			title = title.replaceAll("&amp;", "&");
			title = title.replaceAll("&lt;", "<");
			title = title.replaceAll("&gt;", ">");
			title = title.replaceAll("&quot;", "\"");
			title = title.replaceAll("&apos;", "\'");
			title = title.replaceAll("&nbsp;", " ");
			note.setTitle(title);
			AppInfo.DebugLog(mContext, "create note add title");
			note.setContent(sb.toString());
			sb.setLength(0);
			sb = null;
			System.gc();
			diffsleep(DEFAULT_SLEEP_TIME);
	        ArrayList<String> arrtags = new ArrayList<String>();
	        if(item.tags != null && item.equals("") == false) {
		        String[] arrString = item.tags.split(",");
		        for(String tag: arrString) {
		        	tag = tag.trim();
		        	if(tag.equals("")) continue;
		        	if(arrtags.indexOf(tag) != -1) continue;
		        	arrtags.add(tag);
		        }
	        }
			note.setTagNames(arrtags);
			AppInfo.DebugLog(mContext, "add tags");
			NoteAttributes attr = new NoteAttributes();
			attr.setSourceApplication(appName);
			attr.setSourceURL(item.related);
			note.setAttributes(attr);
			AppInfo.DebugLog(mContext, "add attributes");
			diffsleep(DEFAULT_SLEEP_TIME);
			Note createdNote = getNoteStore().createNote(session.getAuthToken(), note);
			setCreatedNotification(createdNote.getGuid());
		} catch (Exception e) {
			setFailedNotification(mContext.getResources().getString(R.string.postever_failed_create_note));
			return;
		}
	}
	private void setFailedNotification(String notify_title) {
		Intent shortcut = new Intent(Intent.ACTION_VIEW);
		shortcut.setClassName(this.mContext, Notes.class.getName());
		if(item != null) {
			shortcut.putExtra("itemid", item.id);
			setNotification(
				notify_title,
				item.title,
				shortcut
			);
		} else {
			setNotification(
				notify_title,
				ToDosTitle,
				shortcut
			);
		}
	}
	private void setCreatedNotification(String guid) {
		boolean hideTitleBar = false;
	    Intent intent = new Intent();
	    intent.setAction(ACTION_VIEW_NOTE);
	    intent.putExtra(EXTRA_NOTE_GUID, guid);
	    intent.putExtra(EXTRA_FULL_SCREEN, hideTitleBar);
	    if(item != null) {
			setNotification(
				mContext.getResources().getString(R.string.postever_new_note_successfully),
				item.title,
				intent
			);
	    } else {
			setNotification(
				mContext.getResources().getString(R.string.postever_new_note_successfully),
				ToDosTitle,
				intent
			);
	    }
	}
	private void setNotification(String notify_title, String clip_title, Intent intent) {
        PendingIntent pendingintent = PendingIntent.getActivity(mContext, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
        Notification notification = new Notification(R.drawable.everclip, notify_title, System.currentTimeMillis());
        notification.setLatestEventInfo(mContext, notify_title, clip_title, pendingintent);
        //ノティフィケーションマネージャの取得
        NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        //ノティフィケーションの表示
        nm.notify(R.string.app_name, notification);
	}
	protected void serialize(TagNode tagNode, Writer writer) throws IOException, InterruptedException {
		TagNode[] arrnode = tagNode.getElementsByName("body", false);
		arrnode[0].setName("div");
		serializeEnml(arrnode[0], writer, 0);
	}
    public void write(TagNode tagNode, Writer writer, String charset, boolean omitEnvelope) throws IOException, InterruptedException {
		writer = new BufferedWriter(writer);
		serialize(tagNode, writer);

        writer.flush();
        writer.close();
	}
    private synchronized String getIndent(int level) {
        int size = indents.size();
        if (size <= level) {
            String prevIndent = size == 0 ? null : indents.get(size - 1);
            for (int i = size; i <= level; i++) {
                String currIndent = prevIndent == null ? "" : prevIndent + indentString;
                indents.add(currIndent);
                prevIndent = currIndent;
            }
        }

        return indents.get(level);
    }

    private String getIndentedText(String content, int level) {
        String indent = getIndent(level);
        StringBuilder result = new StringBuilder( content.length() );
        StringTokenizer tokenizer = new StringTokenizer(content, "\n\r");

        while (tokenizer.hasMoreTokens()) {
            String line = tokenizer.nextToken().trim();
            if (!"".equals(line)) {
                result.append(indent).append(line).append("\n");
            }
        }

        return result.toString();
    }
    private String getSingleLineOfChildren(List children) {
        StringBuilder result = new StringBuilder();
        Iterator childrenIt = children.iterator();
        boolean isFirst = true;

        while (childrenIt.hasNext()) {
            Object child = childrenIt.next();

            if ( !(child instanceof ContentNode) ) {
                return null;
            } else {
                String content = child.toString();

                // if first item trims it from left
                if (isFirst) {
                	content = Utils.ltrim(content);
                }

                // if last item trims it from right
                if (!childrenIt.hasNext()) {
                	content = Utils.rtrim(content);
                }

                if ( content.indexOf("\n") >= 0 || content.indexOf("\r") >= 0 ) {
                    return null;
                }
                result.append(content);
            }

            isFirst = false;
        }

        return result.toString();
    }
    protected boolean isScriptOrStyle(TagNode tagNode) {
        String tagName = tagNode.getName();
        return "script".equalsIgnoreCase(tagName) || "style".equalsIgnoreCase(tagName);
    }
    private boolean isValidXmlChar(char ch) {
        return ((ch >= 0x20) && (ch <= 0xD7FF)) ||
               (ch == 0x9) ||
               (ch == 0xA) ||
               (ch == 0xD) ||
               ((ch >= 0xE000) && (ch <= 0xFFFD)) ||
               ((ch >= 0x10000) && (ch <= 0x10FFFF));
    }
    private static final Map<Character, String> RESERVED_XML_CHARS = new HashMap<Character, String>();
    static {
        RESERVED_XML_CHARS.put('&', "&amp;");
        RESERVED_XML_CHARS.put('<', "&lt;");
        RESERVED_XML_CHARS.put('>', "&gt;");
        RESERVED_XML_CHARS.put('\"', "&quot;");
        RESERVED_XML_CHARS.put('\'', "&apos;");
    }
    private boolean isReservedXmlChar(char ch) {
        return RESERVED_XML_CHARS.containsKey(ch);
    }
    private boolean isValidInt(String s, int radix) {
        try {
            Integer.parseInt(s, radix);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    protected String escapeXml(String xmlContent) {
        boolean advanced = props.isAdvancedXmlEscape();
        boolean recognizeUnicodeChars = props.isRecognizeUnicodeChars();
//        boolean translateSpecialEntities = props.isTranslateSpecialEntities();

        if (xmlContent != null) {
    		int len = xmlContent.length();
    		StringBuilder result = new StringBuilder(len);

    		for (int i = 0; i < len; i++) {
    			char ch = xmlContent.charAt(i);

    			if (ch == '&') {
    				if ( (advanced || recognizeUnicodeChars) && (i < len-2) && (xmlContent.charAt(i+1) == '#') ) {
                        boolean isHex = Character.toLowerCase(xmlContent.charAt(i+2)) == 'x';
                        int charIndex = i + (isHex ? 3 : 2);
                        int radix = isHex ? 16 : 10;
                        String unicode = "";
                        while (charIndex < len) {
                            char currCh = xmlContent.charAt(charIndex);
                            if (currCh == ';') {
                                break;
                            } else if (isValidInt(unicode + currCh, radix)) {
                                unicode += currCh;
                                charIndex++;
                            } else {
                                charIndex--;
                                break;
                            }
                        }

    					if (isValidInt(unicode, radix)) {
                            char unicodeChar = (char)Integer.parseInt(unicode, radix);
                            if ( !isValidXmlChar(unicodeChar) ) {
                                i = charIndex;
                            } else if ( !isReservedXmlChar(unicodeChar) ) {
                                result.append( recognizeUnicodeChars ? String.valueOf(unicodeChar) : "&#" + unicode + ";" );
                                i = charIndex;
                            } else {
                                i = charIndex;
                                result.append("&#" + unicode + ";");
                            }
    					} else {
    						result.append('&');
    					}
    				} else {
    					result.append('&');
    				}
    			} else if (isReservedXmlChar(ch)) {
//    				result.append(ch);
    			} else {
    				result.append(ch);
    			}
    		}
    		String strres = result.toString();
    		strres = strres.replaceAll("&nbsp;", " ");
    		return strres;
    	}

    	return null;
//        return Utils.escapeXml(xmlContent, props, false);
    }
    protected boolean dontEscape(TagNode tagNode) {
        return props.isUseCdataForScriptAndStyle() && isScriptOrStyle(tagNode);
    }
    protected boolean isMinimizedTagSyntax(TagNode tagNode) {
        final TagInfo tagInfo = props.getTagInfoProvider().getTagInfo(tagNode.getName());
        return tagNode.getChildren().size() == 0 &&
               ( props.isUseEmptyElementTags() || (tagInfo != null && tagInfo.isEmptyTag()) );
    }
    protected boolean serializeOpenTag(TagNode tagNode, Writer writer, boolean newLine) throws IOException {
        String tagName = tagNode.getName();

        if (Utils.isEmptyString(tagName)) {
            return false;
        }
        if(tagName.equalsIgnoreCase("iframe")
        || tagName.equalsIgnoreCase("ilayer")
        || tagName.equalsIgnoreCase("NOSCRIPT")
        || tagName.equalsIgnoreCase("OBJECT")
        || tagName.equalsIgnoreCase("APPLET")
        || tagName.equalsIgnoreCase("EMBED")
        || tagName.equalsIgnoreCase("BLINK")
        || tagName.equalsIgnoreCase("BGSOUND")
        || tagName.equalsIgnoreCase("option")
        || tagName.equalsIgnoreCase("select")
        || tagName.equalsIgnoreCase("textarea")
        || tagName.equalsIgnoreCase("fieldset")
        || tagName.equalsIgnoreCase("legend")
        || tagName.equalsIgnoreCase("frameset")
        || tagName.equalsIgnoreCase("frame")
        ) {
        	return false;
        }
    	boolean display = true;
        if(tagNode.hasAttribute("style")) {
        	String styles = tagNode.getAttributeByName("style");
        	AppInfo.DebugLog(this.mContext, "openTag name: " + tagName + " style: " + styles);
        	String[] arrstyles = styles.split(";");
        	for(String style : arrstyles) {
        		style = style.toLowerCase().trim();
        		if(style.startsWith("display")) {
        			style = style.replaceAll(": ", ":");
        			String[] value = style.split(":");
        			if(value[1].trim().equals("none")) {
        				display = false;
        			} else {
        				display = true;
        			}
        		}
        	}
        }
        if(display) {
	        writer.write("<" + tagName);

	        // write attributes
	        for (Map.Entry<String, String> entry: tagNode.getAttributes().entrySet()) {
	            String attName = entry.getKey();
	            String escapestr = escapeXml(entry.getValue());

	            System.gc();
	            writer.write(" " + attName + "=\"" + escapestr + "\"");
	        }
	        if ( isMinimizedTagSyntax(tagNode) ) {
	            writer.write(" />");
	            if (newLine) {
	                writer.write("\n");
	            }
	        } else if (dontEscape(tagNode)) {
	            writer.write("><![CDATA[");
	        } else {
	            writer.write(">");
	        }
	        return true;
        } else {
        	return false;
        }
    }
    protected void serializeEndTag(TagNode tagNode, Writer writer, boolean newLine) throws IOException {
        String tagName = tagNode.getName();

        if (Utils.isEmptyString(tagName)) {
            return;
        }

        if (dontEscape(tagNode)) {
            writer.write("]]>");
        }
        writer.write( "</" + tagName + ">" );

        if (newLine) {
            writer.write("\n");
        }
    }
    protected void serializeEnml(TagNode tagNode, Writer writer, int level) throws IOException, InterruptedException {
        List tagChildren = tagNode.getChildren();
        boolean isHeadlessNode = Utils.isEmptyString(tagNode.getName());
        String indent = isHeadlessNode ? "" : getIndent(level);

        if(serializeOpenTag(tagNode, writer, true)) {

	        if ( !isMinimizedTagSyntax(tagNode) ) {
	            String singleLine = getSingleLineOfChildren(tagChildren);
	            boolean dontEscape = dontEscape(tagNode);
	            if (singleLine != null) {
	            	if ( !dontEscape(tagNode) ) {
	            		writer.write( escapeXml(singleLine) );
	            	} else {
	            		writer.write( singleLine.replaceAll("]]>", "]]&gt;") );
	            	}
	            } else {
	                if (!isHeadlessNode) {
	            	    writer.write("\n");
	                }
	                for (Object child: tagChildren) {
	                    if (child instanceof TagNode) {
	                        serializeEnml( (TagNode)child, writer, isHeadlessNode ? level : level + 1 );
	                    } else if (child instanceof ContentNode) {
	                        String content = dontEscape ? child.toString().replaceAll("]]>", "]]&gt;") : escapeXml(child.toString());
	                        writer.write( getIndentedText(content, isHeadlessNode ? level : level + 1) );
	                    } else if (child instanceof CommentNode) {
	                        CommentNode commentNode = (CommentNode) child;
	                        String content = commentNode.getCommentedContent();
	                        writer.write( getIndentedText(content, isHeadlessNode ? level : level + 1) );
	                    }
	                }
	            }

	            serializeEndTag(tagNode, writer, false);
				diffsleep(DreamPostEnmlService.DEFAULT_SLEEP_TIME);
	        }
        }
    }

	private String eraseDummyTag(String content, String[] tags) throws InterruptedException {
		for(String tag : tags) {
			content = eraseTag(content, "<" + tag + "></" + tag + ">");
		}
		return content;
	}
	private HashMap<String, String> dtdmap = null;
	private void initDTDmap() {
		dtdmap = new HashMap<String, String>();
		dtdmap.put("coreattrs", "style|title");
		dtdmap.put("i18n", "lang|xml:lang|dir");
		dtdmap.put("focus", "accesskey|tabindex");
		dtdmap.put("attrs", dtdmap.get("coreattrs") + "|" + dtdmap.get("i18n"));;
		dtdmap.put("TextAlign", "align");
		dtdmap.put("cellhalign", "align|char|charoff");
		dtdmap.put("cellvalign", "valign");
		dtdmap.put("a", dtdmap.get("attrs") + "|" + dtdmap.get("focus") + "|charset|type|name|href|hreflang|rel|rev|shape|coords|target");
		dtdmap.put("abbr", dtdmap.get("attrs"));
		dtdmap.put("acronym", dtdmap.get("attrs"));
		dtdmap.put("address", dtdmap.get("attrs"));
		dtdmap.put("area", dtdmap.get("attrs") + "|" + dtdmap.get("focus") + "|shape|coords|href|nohref|alt|target");
		dtdmap.put("b", dtdmap.get("attrs"));
		dtdmap.put("bdo", dtdmap.get("coreattrs") + "|lang|xml:lang|dir");
		dtdmap.put("big", dtdmap.get("attrs"));
		dtdmap.put("blockquote", dtdmap.get("attrs") + "|cite");
		dtdmap.put("br", dtdmap.get("coreattrs") + "|clear");
		dtdmap.put("caption", dtdmap.get("attrs") + "|align");
		dtdmap.put("center", dtdmap.get("attrs"));
		dtdmap.put("cite", dtdmap.get("attrs"));
		dtdmap.put("code", dtdmap.get("attrs"));
		dtdmap.put("col", dtdmap.get("attrs") + "|" + dtdmap.get("cellhalign") + "|" + dtdmap.get("cellvalign") + "|span|width");
		dtdmap.put("colgroup", dtdmap.get("attrs") + "|" + dtdmap.get("cellhalign") + "|" + dtdmap.get("cellvalign") + "|span|width");
		dtdmap.put("dd", dtdmap.get("attrs"));
		dtdmap.put("del", dtdmap.get("attrs") + "|cite|datetime");
		dtdmap.put("dfn", dtdmap.get("attrs"));
		dtdmap.put("div", dtdmap.get("attrs") + "|" + dtdmap.get("TextAlign"));
		dtdmap.put("dl", dtdmap.get("attrs") + "|compact");
		dtdmap.put("dt", dtdmap.get("attrs"));
		dtdmap.put("em", dtdmap.get("attrs"));
		dtdmap.put("font", dtdmap.get("coreattrs") + "|" + dtdmap.get("i18n") + "|size|color|face");
		dtdmap.put("h1", dtdmap.get("attrs") + "|" + dtdmap.get("TextAlign"));
		dtdmap.put("h2", dtdmap.get("attrs") + "|" + dtdmap.get("TextAlign"));
		dtdmap.put("h3", dtdmap.get("attrs") + "|" + dtdmap.get("TextAlign"));
		dtdmap.put("h4", dtdmap.get("attrs") + "|" + dtdmap.get("TextAlign"));
		dtdmap.put("h5", dtdmap.get("attrs") + "|" + dtdmap.get("TextAlign"));
		dtdmap.put("h6", dtdmap.get("attrs") + "|" + dtdmap.get("TextAlign"));
		dtdmap.put("hr", dtdmap.get("attrs") + "|align|noshade|size|width");
		dtdmap.put("i", dtdmap.get("attrs"));
		dtdmap.put("img", dtdmap.get("attrs") + "|src|alt|name|longdesc|height|width|usemap|ismap|align|border|hspace|vspace");
		dtdmap.put("ins", dtdmap.get("attrs") + "|cite|datetime");
		dtdmap.put("kbd", dtdmap.get("attrs"));
		dtdmap.put("li", dtdmap.get("attrs") + "|type|value");
		dtdmap.put("map", dtdmap.get("i18n") + "|title|name");
		dtdmap.put("ol", dtdmap.get("attrs") + "|type|compact|start");
		dtdmap.put("p", dtdmap.get("attrs") + "|" + dtdmap.get("TextAlign"));
		dtdmap.put("pre", dtdmap.get("attrs") + "|width");
		dtdmap.put("q", dtdmap.get("attrs") + "|cite");
		dtdmap.put("s", dtdmap.get("attrs"));
		dtdmap.put("samp", dtdmap.get("attrs"));
		dtdmap.put("small", dtdmap.get("attrs"));
		dtdmap.put("span", dtdmap.get("attrs"));
		dtdmap.put("strike", dtdmap.get("attrs"));
		dtdmap.put("strong", dtdmap.get("attrs"));
		dtdmap.put("sub", dtdmap.get("attrs"));
		dtdmap.put("sup", dtdmap.get("attrs"));
		dtdmap.put("table", dtdmap.get("attrs") + "|summary|width|border|cellspacing|cellpadding|align|bgcolor");
		dtdmap.put("tbody", dtdmap.get("attrs") + "|" + dtdmap.get("cellhalign") + "|" + dtdmap.get("cellvalign"));
		dtdmap.put("td", dtdmap.get("attrs") + "|" + dtdmap.get("cellhalign") + "|" + dtdmap.get("cellvalign") + "|abbr|rowspan|colspan|nowrap|bgcolor|width|height");
		dtdmap.put("tfoot", dtdmap.get("attrs") + "|" + dtdmap.get("cellhalign") + "|" + dtdmap.get("cellvalign"));
		dtdmap.put("th", dtdmap.get("attrs") + "|" + dtdmap.get("cellhalign") + "|" + dtdmap.get("cellvalign") + "|abbr|rowspan|colspan|nowrap|bgcolor|width|height");
		dtdmap.put("thead", dtdmap.get("attrs") + "|" + dtdmap.get("cellhalign") + "|" + dtdmap.get("cellvalign"));
		dtdmap.put("tr", dtdmap.get("attrs") + "|" + dtdmap.get("cellhalign") + "|" + dtdmap.get("cellvalign") + "|bgcolor");
		dtdmap.put("tt", dtdmap.get("attrs"));
		dtdmap.put("u", dtdmap.get("attrs"));
		dtdmap.put("ul", dtdmap.get("attrs") + "|type|compact");
		dtdmap.put("var", dtdmap.get("attrs"));
	}
	private String setEndTag(String content) throws InterruptedException {
		StringBuffer sb = new StringBuffer();
		Iterator<Entry<String, String>> iter = dtdmap.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<String, String> ent = iter.next();
			String tagname = ent.getKey();
			Matcher m = Pattern.compile("<" + tagname + "(\"[^\"]*\"|'[^']*'|[^\"'])([^\\/]*|[^\\/]*/)>", Pattern.CASE_INSENSITIVE).matcher(content);
			while(m.find()) {
				String tagbody = m.group(0);
				tagbody = tagbody.substring(tagname.length() + 1);
				if(tagname.equals("br") || tagname.equals("hr")) {
					if(tagbody.endsWith("/>")) {
						tagbody = tagbody.substring(0, tagbody.length() - 2);
					} else {
						tagbody = tagbody.substring(0, tagbody.length() - 1);
					}
					m.appendReplacement(sb, "<" + tagname + tagbody + "></" + tagname + ">");
				} else {
					if(tagbody.endsWith("/>") == false) { continue; }
					tagbody = tagbody.substring(0, tagbody.length() - 2);
					m.appendReplacement(sb, "<" + tagname + tagbody + "></" + tagname + ">");
				}
				diffsleep(DEFAULT_SLEEP_TIME);
			}
			m.appendTail(sb);
			content = sb.toString();
			sb.setLength(0);
		}
		return content;
	}
	private String eraseAttrTag(String content) throws InterruptedException {
		StringBuffer sb = new StringBuffer();
		Iterator<Entry<String, String>> iter = dtdmap.entrySet().iterator();
		while(iter.hasNext()) {
			Entry<String, String> ent = iter.next();
			String tagname = ent.getKey();
			AppInfo.DebugLog(mContext, "eraseattr tagname: " + tagname);
			String attribute = ent.getValue();
			AppInfo.DebugLog(mContext, "tagname: " + tagname);
//			Matcher m = Pattern.compile("<" + tagname + "\\s(\"[^\"]*\"|'[^']*'|[^\"'])([^\\/]*|[^\\/]*/)>", Pattern.CASE_INSENSITIVE).matcher(content);
			Matcher m = Pattern.compile("<" + tagname + "\\s(\"[^\"]*\"|'[^']*'|[^\"'])(.+?)>", Pattern.CASE_INSENSITIVE).matcher(content);
			while(m.find()) {
				String tagbody = m.group(0);
				tagbody = tagbody.substring(tagname.length() + 1);
				if(tagbody.trim().startsWith(">") || tagbody.trim().startsWith("/>")) {continue;}
				tagbody = tagbody.replaceAll("　", " ");
				Matcher tm = Pattern.compile("\\s(?!(" + attribute + "))[\\w-_]+=\"\"", Pattern.CASE_INSENSITIVE).matcher(tagbody);
				if(tm.find()) {
					AppInfo.DebugLog(mContext, "erase: " + tm.group(0));
					tagbody = tm.replaceAll(" ");
				}
				tm = Pattern.compile("\\s(?!(" + attribute + "))[\\w-_]+=([\"'])(.+?)(?:\\2)", Pattern.CASE_INSENSITIVE).matcher(tagbody);
				if(tm.find()) {
					AppInfo.DebugLog(mContext, "erase: " + tm.group(0));
					tagbody = tm.replaceAll(" ");
				}
				tm = Pattern.compile("\\s(?!(" + attribute + "))\\w[^\"]+\"=\"\"", Pattern.CASE_INSENSITIVE).matcher(tagbody);
				if(tm.find()) {
					AppInfo.DebugLog(mContext, "erase: " + tm.group(0));
					tagbody = tm.replaceAll(" ");
				}
				tm = Pattern.compile("\\s(?!(" + attribute + "))\\w[^\"]+\"=\"", Pattern.CASE_INSENSITIVE).matcher(tagbody);
				if(tm.find()) {
					AppInfo.DebugLog(mContext, "erase: " + tm.group(0));
					tagbody = tm.replaceAll(" ");
				}
				try {
				m.appendReplacement(sb, "<" + tagname + tagbody);
				} catch(java.lang.ArrayIndexOutOfBoundsException e) {
					AppInfo.DebugLog(mContext, "Exception tagname:" + tagname);
					AppInfo.DebugLog(mContext, "          tagbody:" + tagbody);
				}
				diffsleep(DEFAULT_SLEEP_TIME);
			}
			m.appendTail(sb);
			content = sb.toString();
			sb.setLength(0);
			System.gc();
		}
		Matcher m = Pattern.compile("\\s(class|id|tabindex|dynsrc|onclick|ondblclick"
				  + "|onKeyDown|onKeyPress|onKeyUp|ontouchstart"
				  + "|onMouseDown|onMouseUp|onMouseOver|onMouseOut|onMouseMove"
				  + "|onLoad|onUnload|onFocus|onBlur|onSubmit|onReset|onChange"
				  + "|onResize|onMove|onDragDrop|onAbort|onError|onSelect)"
				  + "(=|\\s=)(\"\"|'')",
				  Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(content);
		if(m.find()) {
			content = m.replaceAll(" ");
		}
		diffsleep(DEFAULT_SLEEP_TIME);

		m = Pattern.compile("\\s(class|id|tabindex|dynsrc|onclick|ondblclick"
				  + "|onKeyDown|onKeyPress|onKeyUp|ontouchstart"
				  + "|onMouseDown|onMouseUp|onMouseOver|onMouseOut|onMouseMove"
				  + "|onLoad|onUnload|onFocus|onBlur|onSubmit|onReset|onChange"
				  + "|onResize|onMove|onDragDrop|onAbort|onError|onSelect)"
				  + "(=|\\s=)([\"'])([^\"']+)([\"'])",
				  Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(content);
		if(m.find()) {
			content = m.replaceAll(" ");
		}
		diffsleep(DEFAULT_SLEEP_TIME);

		m = Pattern.compile("\\sstyle=\"style\"", Pattern.CASE_INSENSITIVE).matcher(content);
		if(m.find()) {
			content = m.replaceAll(" ");
		}
		diffsleep(DEFAULT_SLEEP_TIME);
		System.gc();
		return content;
	}
	private boolean checkXmlns(String content) {
		Matcher m = Pattern.compile("<([^\\s]+)(.+?)xmlns:(.+?)>", Pattern.CASE_INSENSITIVE).matcher(content);
		if(m.find()) {
			return true;
		}
		m = Pattern.compile("<[^:]:(.+?)>", Pattern.CASE_INSENSITIVE).matcher(content);
		if(m.find()) {
			return true;
		}
		m = Pattern.compile("</[^:]:(.+?)>", Pattern.CASE_INSENSITIVE).matcher(content);
		if(m.find()) {
			return true;
		}
		return false;
	}
	private String eraseTag(String content, String tag, String endtag) throws InterruptedException {
		StringBuffer sb = new StringBuffer();
		Matcher m = Pattern.compile("<" + tag + "(.+?)"+ endtag +">", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(content);
		while(m.find()) {
//			AppInfo.DebugLog(mContext, "erase tag <" + tag + m.group(1) + endtag + ">");
			m.appendReplacement(sb, "");
			diffsleep(DEFAULT_SLEEP_TIME);
		}
		m.appendTail(sb);
		content = sb.toString();
		sb.setLength(0);
		sb = null;
		System.gc();
		return content;
	}
	private String eraseTag(String content, String tag) throws InterruptedException {
		Matcher m = Pattern.compile(tag, Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(content);
		if(m.find()) {
			content = m.replaceAll("");
		}
		diffsleep(DEFAULT_SLEEP_TIME);
		System.gc();
		return content;
	}
	private void replaseStyle3(TagNode node, ArrayListCSS arrcss) throws Exception {
		for(int c = 0; c < arrcss.size(); c++) {
			CSS css = arrcss.get(c);
			String selector = css.getName();
			selector = selector.replaceAll(" ", "/");
			TagNode[] evaluatenodes = new CSSSelectorInter(selector).searchNode(node);
//			TagNode[] evaluatenodes = node.evaluateCSSSelector(mContext, selector);
			diffsleep(DEFAULT_SLEEP_TIME);

			if(evaluatenodes != null) {
				if(evaluatenodes.length > 0) {
					AppInfo.DebugLog(mContext, "selector: " + selector);

					for(int i = 0; i < evaluatenodes.length; i++) {
						TagNode enode = evaluatenodes[i];
						String attrstyle = enode.getAttributeByName("style");
						ArrayList<String> cssstyles = css.getStyles();
						StringBuilder sb = new StringBuilder();
						Iterator<String> styles = cssstyles.iterator();
						while(styles.hasNext()) {
							String style = styles.next();
							sb.append(style + " ");
						}
						if(attrstyle != null) {
							sb.insert(0, attrstyle + " ");
//							sb.append(attrstyle + " ");
						}
						String setstyle = sb.toString();
						AppInfo.DebugLog(mContext, "set style: " + setstyle);
						enode.setAttribute("style", setstyle);
						enode = null;
						sb.setLength(0);
						sb = null;
						diffsleep(DEFAULT_SLEEP_TIME);

						System.gc();
					}
				}
				evaluatenodes = null;
			}
		}
	}
	public void createPicEnexFile() throws InterruptedException {
		String enexname = appPath + "/.cache/temp.enex";
		if(isPicClip() == false) return;
		String content = readStringFile(new File(appPath + "/.clip/" + item.path + "/index.html"), "UTF-8");
		diffsleep(DEFAULT_SLEEP_TIME);
		Note note = new Note();
		note.setTitle(item.title);

		StringBuilder edamcontent = new StringBuilder();
		edamcontent.append(NOTE_PREFIX);
		if(StringUtils.isBlank(item.description) == false) {
			edamcontent.append("<div>" + item.description.replaceAll("\n", "<br/>") + "</div>");
			edamcontent.append("<div><hr/></div>");
		}
		diffsleep(DEFAULT_SLEEP_TIME);
		edamcontent.append(TABLE_PREFIX);
		Matcher m = Pattern.compile("(<tr>)(.+?)(<\\/tr>)", Pattern.DOTALL).matcher(content);
		try {
			while(m.find()) {
				String tr = m.group(2);
				edamcontent.append("<tr>");
				Matcher im = Pattern.compile("(<td>)(.+?)(<\\/td>)", Pattern.CASE_INSENSITIVE).matcher(tr);
				while(im.find()) {
					String td = im.group(2);
					Matcher sizem = Pattern.compile("(width=\")(.+?)(\")").matcher(td);
					int width = 0;
					int height = 0;
					if(sizem.find()) {
						width = Integer.parseInt(sizem.group(2));
					}
					sizem.reset();
					sizem = null;
					diffsleep(DEFAULT_SLEEP_TIME);
					sizem = Pattern.compile("(height=\")(.+?)(\")").matcher(td);
					if(sizem.find()) {
						height = Integer.parseInt(sizem.group(2));
					}
					sizem.reset();
					sizem = null;
					diffsleep(DEFAULT_SLEEP_TIME);
					Matcher tm = Pattern.compile("(src=)(['\"])(.+?)(?:\\2)", Pattern.CASE_INSENSITIVE).matcher(td);
					if(tm.find()) {
						String filename = tm.group(3);
						edamcontent.append("<td>");
						String f = appPath + "/.clip/" + item.path + "/" + filename;
						FileInputStream fi = new FileInputStream(f);
						ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
						byte[] block = new byte[10240];
						int len;
						while ((len = fi.read(block)) >= 0) {
						  byteOut.write(block, 0, len);
						}
						fi.close();
						fi = null;
						diffsleep(DEFAULT_SLEEP_TIME);
						String base64body = Base64.encodeBytes(byteOut.toByteArray());
						edamcontent.append("<img src=\"data:image/jpg;base64,");
						edamcontent.append(base64body);
						edamcontent.append("\" width=\"" + width + "\" height=\"" + height + "\" border=\"0\"/>");
						byteOut.reset();
						byteOut = null;
						base64body = "";
						System.gc();
						diffsleep(DEFAULT_SLEEP_TIME);

						edamcontent.append("</td>");
					}
				}
				diffsleep(DEFAULT_SLEEP_TIME);
				edamcontent.append("</tr>");
			}
			edamcontent.append(TABLE_SUFFIX);
			edamcontent.append(NOTE_SUFFIX);
			note.setContent(edamcontent.toString());
			edamcontent.setLength(0);
			diffsleep(DEFAULT_SLEEP_TIME);
			// Create the note on the server. The returned Note object
			// will contain server-generated attributes such as the note's
			// unique ID (GUID), the Resource's GUID, and the creation and update time.
	        ArrayList<String> arrtags = new ArrayList<String>();
	        if(item.tags != null && item.equals("") == false) {
		        String[] arrString = item.tags.split(",");
		        for(String tag: arrString) {
		        	tag = tag.trim();
		        	if(tag.equals("")) continue;
		        	if(arrtags.indexOf(tag) != -1) continue;
		        	arrtags.add(tag);
		        }
	        }
			note.setTagNames(arrtags);
			NoteAttributes attr = new NoteAttributes();
			attr.setSourceURL(item.related);
			attr.setSourceApplication(appName);
			note.setAttributes(attr);
			diffsleep(DEFAULT_SLEEP_TIME);
			Note createdNote = getNoteStore().createNote(session.getAuthToken(), note);
			setCreatedNotification(createdNote.getGuid());
		} catch(Exception e) {
			setFailedNotification(mContext.getResources().getString(R.string.err_activity_not_found));
			return;
		}
	}
	private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
	private long copyLarge(Reader input, Writer output) throws IOException, OutOfMemoryError, InterruptedException {
		char[] buffer = new char[DEFAULT_BUFFER_SIZE];
		long count = 0;
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			diffsleep(DEFAULT_SLEEP_TIME);
			count += n;
		}
		buffer = null;
		System.gc();
		return count;
	}

	private int copy(Reader input, Writer output) throws IOException, OutOfMemoryError, InterruptedException {
		long count = copyLarge(input, output);
		if(count > Integer.MAX_VALUE) {
			return -1;
		}
		return (int)count;
	}
	private void copy(InputStream input, Writer output) throws IOException, OutOfMemoryError, InterruptedException {
		InputStreamReader in = new InputStreamReader(input);
		copy(in, output);
	}

	private void copy(InputStream input, Writer output, String encording) throws IOException, OutOfMemoryError, InterruptedException {
		if(encording == null) {
			copy(input, output);
		} else {
			InputStreamReader in = new InputStreamReader(input, encording);
			copy(in, output);
		}
	}
	private String toString(InputStream input, String encording) throws IOException, OutOfMemoryError, InterruptedException {
		StringWriter sw = new StringWriter();
		copy(input, sw, encording);
		String result = sw.toString();
		sw.close();
		sw = null;
		System.gc();
		diffsleep(DEFAULT_SLEEP_TIME);
		return result;
	}

	public String readStringFile(File srcFile, String cacheencoding) throws InterruptedException {
		String readString = "";
		InputStream input = null;
		try {
			input = new FileInputStream(srcFile);
			return toString(input, cacheencoding);
		} catch (IOException e) {
			readString = "";
		} catch (OutOfMemoryError e) {
			readString = null;
		} finally {
			try {
				if(input != null) {
					input.close();
					input = null;
				}
			} catch(IOException e) {
				e.printStackTrace();
			}
		}
		System.gc();
        return readString;
	}
	private class CSSSelectorInter {
	    private String tokenArray[];
	    public CSSSelectorInter(String selector) {
	        StringTokenizer tokenizer = new StringTokenizer(selector, "*.#/+~:()[]\"'$^=<>", true);
	        int tokenCount = tokenizer.countTokens();
	        tokenArray = new String[tokenCount];

	        int index = 0;

	        while (tokenizer.hasMoreTokens()) {
	            tokenArray[index++] = tokenizer.nextToken();
	        }
	    }
	    public TagNode[] searchNode(TagNode node) throws InterruptedException {
	    	if(node == null) return null;
	    	return search(singleton(node), 0, tokenArray.length - 1, true);
	    }
	    private TagNode[] search(TagNode[] node, int from, int to, boolean isRecursive) throws InterruptedException {
			diffsleep(DEFAULT_SLEEP_TIME);
	    	if (from >= 0 && to < tokenArray.length && from <= to) {
	    		if ("".equals(tokenArray[from].trim())) {
	    			return search(node, from + 1, to, true);
	    		} else if (isToken("*", from)) {
	    			return getAllElements(node, from, to, true);
	    		} else if (isToken(">", from)) {
	    			if(isToken("*", from + 1)) {
	    				return search(node, from + 2, to, false);
	    			} else {
	    				return search(node, from + 1, to, false);
	    			}
//	    			return null;
	    		} else if (isToken("+", from)) {
	    			return null;
	    		} else if (isToken("~", from)) {
	    			return null;
	    		} else if (isToken("^", from)) {
	    			return null;
	    		} else if (isToken("$", from)) {
	    			return null;
	    		} else if (isToken(":", from)) {
	    			return null;
	    		} else if (isToken("[", from)) {
	    			int closingBracket = findClosingIndex(from, to);
	                if (closingBracket > 0) {
	                	if(closingBracket == from + 2) {
	                		TagNode[] result = findNodesByAttr(node, tokenArray[from + 1]);
	                		return search(result, closingBracket + 1, to, true);
	                	} else if(closingBracket == from + 6 && isToken("=", from + 2) && isToken("\"", from + 3) && isToken("\"", from + 5)) {
	                		TagNode[] result = findNodesByAttrValue(node, tokenArray[from + 1], tokenArray[from + 4]);
	                		return search(result, closingBracket + 1, to, true);
	                	} else {
	                		return null;
	                	}
	                } else {
	                	return null;
	                }
	    		} else if (isToken("(", from)) {
	    			return null;
	    		} else if (isToken("\"", from) || isToken("'", from)) {
	    			return null;
	    		} else if (isToken("/", from)) {
	    			return search(node, from + 1, to, isRecursive);
	    		} else if (isToken("#", from)) {
	    			return getElementsById(node, from + 1, to);
	    		} else if (isToken(".", from)) {
	    			return getElementsByClass(node, from + 1, to);
	    		} else {
	    			return getElementsByName(node, from, to, isRecursive);
	    		}
	    	} else {
	    		return node;
	    	}
	    }
	    private int findClosingIndex(int from, int to) {
	        if (from < to) {
	            String currToken = tokenArray[from];

	            if ("\"".equals(currToken)) {
	                for (int i = from + 1; i <= to; i++) {
	                    if ("\"".equals(tokenArray[i])) {
	                        return i;
	                    }
	                }
	            } else if ("'".equals(currToken)) {
	                for (int i = from + 1; i <= to; i++) {
	                    if ("'".equals(tokenArray[i])) {
	                        return i;
	                    }
	                }
	            } else if ( "(".equals(currToken) || "[".equals(currToken) || "/".equals(currToken) ) {
	                boolean isQuoteClosed = true;
	                boolean isAposClosed = true;
	                int brackets = "(".equals(currToken) ? 1 : 0;
	                int angleBrackets = "[".equals(currToken) ? 1 : 0;
	                int slashes = "/".equals(currToken) ? 1 : 0;
	                for (int i = from + 1; i <= to; i++) {
	                    if ( "\"".equals(tokenArray[i]) ) {
	                        isQuoteClosed = !isQuoteClosed;
	                    } else if ( "'".equals(tokenArray[i]) ) {
	                        isAposClosed = !isAposClosed;
	                    } else if ( "(".equals(tokenArray[i]) && isQuoteClosed && isAposClosed ) {
	                        brackets++;
	                    } else if ( ")".equals(tokenArray[i]) && isQuoteClosed && isAposClosed ) {
	                        brackets--;
	                    } else if ( "[".equals(tokenArray[i]) && isQuoteClosed && isAposClosed ) {
	                        angleBrackets++;
	                    } else if ( "]".equals(tokenArray[i]) && isQuoteClosed && isAposClosed ) {
	                        angleBrackets--;
	                    } else if ( "/".equals(tokenArray[i]) && isQuoteClosed && isAposClosed && brackets == 0 && angleBrackets == 0) {
	                        slashes--;
	                    }

	                    if (isQuoteClosed && isAposClosed && brackets == 0 && angleBrackets == 0 && slashes == 0) {
	                        return i;
	                    }
	                }
	            }

	        }

	        return -1;
	    }
	    private boolean isToken(String token, int index) {
	        int len = tokenArray.length;
	        return index >= 0 && index < len && tokenArray[index].trim().equals(token.trim());
	    }
	    private TagNode[] singleton(TagNode node) {
	    	TagNode[] result = new TagNode[1];
	    	result[0] = node;
	    	return result;
	    }
	    private TagNode[] getElementsById(TagNode[] node, int from, int to) throws InterruptedException {
	    	String selector = tokenArray[from].trim();
	    	TagNode[] result = findNodesByAttrValue(node, "id", selector);
	    	from++;
	    	while (isToken("#", from) || isToken(".", from)) {
	    		if(isToken("#", from)) {
	    			from++;
	    			result = getNodesByAttrValue(result, "id", tokenArray[from].trim());
	    			from++;
	    		} else if(isToken(".", from)) {
	    			from++;
	    			result = getNodesByAttrValue(result, "class", tokenArray[from].trim());
	    			from++;
	    		}
	    	}
	    	return search(result, from, to, true);
	    }
	    private TagNode[] getElementsByClass(TagNode[] node, int from, int to) throws InterruptedException {
	    	String selector = tokenArray[from].trim();
	    	TagNode[] result = findNodesByAttrValue(node, "class", selector);
	    	from++;
	    	while (isToken("#", from) || isToken(".", from)) {
	    		if(isToken("#", from)) {
	    			from++;
	    			result = getNodesByAttrValue(result, "id", tokenArray[from].trim());
	    			from++;
	    		} else if(isToken(".", from)) {
	    			from++;
	    			result = getNodesByAttrValue(result, "class", tokenArray[from].trim());
	    			from++;
	    		}
	    	}
	    	return search(result, from, to, true);
	    }
	    private TagNode[] getElementsByName(TagNode[] node, int from, int to, boolean isRecursive) throws InterruptedException {
	    	String selector = tokenArray[from].trim();
	    	TagNode[] result = findNodesByName(node, selector, isRecursive);
			from++;
	    	while (isToken("#", from) || isToken(".", from)) {
	    		if(isToken("#", from)) {
	    			from++;
	    			result = getNodesByAttrValue(result, "id", tokenArray[from].trim());
	    			from++;
	    		} else if(isToken(".", from)) {
	    			from++;
	    			result = getNodesByAttrValue(result, "class", tokenArray[from].trim());
	    			from++;
	    		}
	    	}
	    	return search(result, from, to, true);
	    }
	    private TagNode[] getAllElements(TagNode[] node, int from, int to, boolean isRecursive) throws InterruptedException {
			List<TagNode> result = new LinkedList<TagNode>();
			for(int i = 0; i < node.length; i++) {
				TagNode snode = node[i];
				TagNode[] resulttag = snode.getAllElements(isRecursive);
				diffsleep(DEFAULT_SLEEP_TIME);
				for(int t = 0; t < resulttag.length; t++) {
					result.add(resulttag[t]);
					diffsleep(DEFAULT_SLEEP_TIME);
				}
			}
			TagNode array[] = new TagNode[ result == null ? 0 : result.size() ];
	        for (int i = 0; i < result.size(); i++) {
	            array[i] = result.get(i);
	        }
	        result.clear();
			diffsleep(DEFAULT_SLEEP_TIME);
			return search(array, from + 1, to, true);
	    }
	    private TagNode[] getNodesByAttrValue(TagNode[] node, String AttrName, String AttrValue) throws InterruptedException {
			List<TagNode> result = new LinkedList<TagNode>();
			for(int i = 0; i < node.length; i++) {
				TagNode snode = node[i];
				diffsleep(DEFAULT_SLEEP_TIME);
				if(AttrValue.equalsIgnoreCase(snode.getAttributeByName(AttrName))) {
					result.add(snode);
				}
			}
			TagNode array[] = new TagNode[ result == null ? 0 : result.size() ];
	        for (int i = 0; i < result.size(); i++) {
	            array[i] = result.get(i);
	        }
	        result.clear();
			diffsleep(DEFAULT_SLEEP_TIME);
			return array;
	    }
	    private TagNode[] findNodesByAttr(TagNode[] node, String AttrName) throws InterruptedException {
			List<TagNode> result = new LinkedList<TagNode>();
			for(int i = 0; i < node.length; i++) {
				TagNode snode = node[i];
				TagNode[] resulttag = snode.getElementsHavingAttribute(AttrName, true);
				diffsleep(DEFAULT_SLEEP_TIME);
				for(int t = 0; t < resulttag.length; t++) {
					result.add(resulttag[t]);
				}
			}
			TagNode array[] = new TagNode[ result == null ? 0 : result.size() ];
	        for (int i = 0; i < result.size(); i++) {
	            array[i] = result.get(i);
	        }
	        result.clear();
			diffsleep(DEFAULT_SLEEP_TIME);
			return array;
	    }
		private TagNode[] findNodesByAttrValue(TagNode[] node, String AttrName, String AttrValue) throws InterruptedException {
			List<TagNode> result = new LinkedList<TagNode>();
			for(int i = 0; i < node.length; i++) {
				TagNode snode = node[i];
				TagNode[] resulttag = snode.getElementsByAttValue(AttrName, AttrValue, true, false);
				diffsleep(DEFAULT_SLEEP_TIME);
				for(int t = 0; t < resulttag.length; t++) {
					result.add(resulttag[t]);
				}
			}
			TagNode array[] = new TagNode[ result == null ? 0 : result.size() ];
	        for (int i = 0; i < result.size(); i++) {
	            array[i] = result.get(i);
	        }
	        result.clear();
			diffsleep(DEFAULT_SLEEP_TIME);
			return array;
		}
		private TagNode[] findNodesByName(TagNode[] node, String name, boolean isRecursive) throws InterruptedException {
			List<TagNode> result = new LinkedList<TagNode>();
			for(int i = 0; i < node.length; i++) {
				TagNode snode = node[i];
				TagNode[] resulttag = snode.getElementsByName(name, isRecursive);
				diffsleep(DEFAULT_SLEEP_TIME);
				for(int t = 0; t < resulttag.length; t++) {
					result.add(resulttag[t]);
				}
			}
			TagNode array[] = new TagNode[ result == null ? 0 : result.size() ];
	        for (int i = 0; i < result.size(); i++) {
	            array[i] = result.get(i);
	        }
	        result.clear();
			diffsleep(DEFAULT_SLEEP_TIME);
			return array;
		}
	}
	private abstract class TagToken implements BaseToken {

	    protected String name;

		public TagToken() {
		}

		public TagToken(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public String toString() {
			return name;
		}

	    abstract void setAttribute(String attName, String attValue);

	}
    /**
     * Used as base for different node checkers.
     */
    private interface ITagNodeCondition {
        public boolean satisfy(TagNode tagNode);
    }

    /**
     * All nodes.
     */
    private class TagAllCondition implements ITagNodeCondition {
        public boolean satisfy(TagNode tagNode) {
            return true;
        }
    }

    /**
     * Checks if node has specified name.
     */
    private class TagNodeNameCondition implements ITagNodeCondition {
        private String name;

        public TagNodeNameCondition(String name) {
            this.name = name;
        }

        public boolean satisfy(TagNode tagNode) {
            return tagNode == null ? false : tagNode.name.equalsIgnoreCase(this.name);
        }
    }

    /**
     * Checks if node contains specified attribute.
     */
    private class TagNodeAttExistsCondition implements ITagNodeCondition {
        private String attName;

        public TagNodeAttExistsCondition(String attName) {
            this.attName = attName;
        }

        public boolean satisfy(TagNode tagNode) {
            return tagNode == null ? false : tagNode.attributes.containsKey( attName.toLowerCase() );
        }
    }

    /**
     * Checks if node has specified attribute with specified value.
     */
    private class TagNodeAttValueCondition implements ITagNodeCondition {
        private String attName;
        private String attValue;
        private boolean isCaseSensitive;

        public TagNodeAttValueCondition(String attName, String attValue, boolean isCaseSensitive) {
            this.attName = attName;
            this.attValue = attValue;
            this.isCaseSensitive = isCaseSensitive;
        }

        public boolean satisfy(TagNode tagNode) {
            if (tagNode == null || attName == null || attValue == null) {
                return false;
            } else {
                return isCaseSensitive ?
                        attValue.equals( tagNode.getAttributeByName(attName) ) :
                        attValue.equalsIgnoreCase( tagNode.getAttributeByName(attName) );
            }
        }
    }
	private class TagNode extends TagToken implements HtmlNode {

	    private TagNode parent = null;
	    private Map<String, String> attributes = new LinkedHashMap<String, String>();
	    private List children = new ArrayList();
	    private Map<String, String> nsDeclarations = null;
	    private List<BaseToken> itemsToMove = null;

	    private transient boolean isFormed = false;


	    public TagNode(String name) {
	        super(name == null ? null : name.toLowerCase());
	    }

	    /**
	     * Changes name of the tag
	     * @param name
	     * @return True if new name is valid, false otherwise
	     */
	    public boolean setName(String name) {
	        if (Utils.isValidXmlIdentifier(name)) {
	            this.name = name;
	            return true;
	        }

	        return false;
	    }

	    /**
	     * @param attName
	     * @return Value of the specified attribute, or null if it this tag doesn't contain it.
	     */
	    public String getAttributeByName(String attName) {
			return attName != null ? attributes.get(attName.toLowerCase()) : null;
		}

	    /**
	     * @return Map instance containing all attribute name/value pairs.
	     */
	    public Map<String, String> getAttributes() {
			return attributes;
		}

	    /**
	     * Checks existance of specified attribute.
	     * @param attName
	     */
	    public boolean hasAttribute(String attName) {
	        return attName != null ? attributes.containsKey(attName.toLowerCase()) : false;
	    }

	    /**
	     * @deprecated Use setAttribute instead
	     * Adds specified attribute to this tag or overrides existing one.
	     * @param attName
	     * @param attValue
	     */
	    @Deprecated
	    public void addAttribute(String attName, String attValue) {
	        setAttribute(attName, attValue);
	    }

	    /**
	     * Adding new attribute ir overriding existing one.
	     * @param attName
	     * @param attValue
	     */
	    public void setAttribute(String attName, String attValue) {
	        if ( attName != null && !"".equals(attName.trim()) ) {
	            attName = attName.toLowerCase();
	            if ("xmlns".equals(attName)) {
	                addNamespaceDeclaration("", attValue);
	            } else if (attName.startsWith("xmlns:")) {
	                addNamespaceDeclaration( attName.substring(6), attValue );
	            } else {
	                attributes.put(attName, attValue == null ? "" : attValue );
	            }
	        }
	    }

	    /**
	     * Adds namespace declaration to the node
	     * @param nsPrefix Namespace prefix
	     * @param nsURI Namespace URI
	     */
	    public void addNamespaceDeclaration(String nsPrefix, String nsURI) {
	        if (nsDeclarations == null) {
	            nsDeclarations = new TreeMap<String, String>();
	        }
	        nsDeclarations.put(nsPrefix, nsURI);
	    }

	    /**
	     * @return Map of namespace declarations for this node
	     */
	    public Map<String, String> getNamespaceDeclarations() {
	        return nsDeclarations;
	    }

	    /**
	     * Removes specified attribute from this tag.
	     * @param attName
	     */
	    public void removeAttribute(String attName) {
	        if ( attName != null && !"".equals(attName.trim()) ) {
	            attributes.remove( attName.toLowerCase() );
	        }
	    }

	    /**
	     * @return List of children objects. During the cleanup process there could be different kind of
	     * childern inside, however after clean there should be only TagNode instances.
	     */
	    public List getChildren() {
			return children;
		}

	    /**
	     * @return Whether this node has child elements or not.
	     */
	    public boolean hasChildren() {
	        return children.size() > 0;
	    }

	    void setChildren(List children) {
	        this.children = children;
	    }

	    public List getChildTagList() {
	        List childTagList = new ArrayList();
	        for (int i = 0; i < children.size(); i++) {
	            Object item = children.get(i);
	            if (item instanceof TagNode) {
	                childTagList.add(item);
	            }
	        }

	        return childTagList;
	    }

	    /**
	     * @return An array of child TagNode instances.
	     */
	    public TagNode[] getChildTags() {
	        List childTagList = getChildTagList();
	        TagNode childrenArray[] = new TagNode[childTagList.size()];
	        for (int i = 0; i < childTagList.size(); i++) {
	            childrenArray[i] = (TagNode) childTagList.get(i);
	        }

	        return childrenArray;
	    }

	    /**
	     * @return Text content of this node and it's subelements.
	     */
	    public StringBuffer getText() {
	        StringBuffer text = new StringBuffer();
	        for (int i = 0; i < children.size(); i++) {
	            Object item = children.get(i);
	            if (item instanceof ContentNode) {
	                text.append(item.toString());
	            } else if (item instanceof TagNode) {
	                StringBuffer subtext = ((TagNode)item).getText();
	                text.append(subtext);
	            }
	        }

	        return text;
	    }

	    /**
	     * @return Parent of this node, or null if this is the root node.
	     */
	    public TagNode getParent() {
			return parent;
		}
	    public void addChild(Object child) {
	        if (child == null) {
	            return;
	        }
	        if (child instanceof List) {
	            addChildren( (List)child );
	        } else {
	            children.add(child);
	            if (child instanceof TagNode) {
	                TagNode childTagNode = (TagNode)child;
	                childTagNode.parent = this;
	            }
	        }
	    }

	    /**
	     * Add all elements from specified list to this node.
	     * @param newChildren
	     */
	    public void addChildren(List newChildren) {
	    	if (newChildren != null) {
	    		Iterator it = newChildren.iterator();
	    		while (it.hasNext()) {
	    			Object child = it.next();
	    			addChild(child);
	    		}
	    	}
	    }

	    /**
	     * Finds first element in the tree that satisfy specified condition.
	     * @param condition
	     * @param isRecursive
	     * @return First TagNode found, or null if no such elements.
	     */
	    private TagNode findElement(ITagNodeCondition condition, boolean isRecursive) {
	        if (condition == null) {
	            return null;
	        }

	        for (int i = 0; i < children.size(); i++) {
	            Object item = children.get(i);
	            if (item instanceof TagNode) {
	                TagNode currNode = (TagNode) item;
	                if ( condition.satisfy(currNode) ) {
	                    return currNode;
	                } else if (isRecursive) {
	                    TagNode inner = currNode.findElement(condition, isRecursive);
	                    if (inner != null) {
	                        return inner;
	                    }
	                }
	            }
	        }

	        return null;
	    }

	    /**
	     * Get all elements in the tree that satisfy specified condition.
	     * @param condition
	     * @param isRecursive
	     * @return List of TagNode instances with specified name.
	     * @throws InterruptedException
	     */
	    private List getElementList(ITagNodeCondition condition, boolean isRecursive) throws InterruptedException {
	        List result = new LinkedList();
	        if (condition == null) {
	            return result;
	        }

	        for (int i = 0; i < children.size(); i++) {
	            Object item = children.get(i);
	            if (item instanceof TagNode) {
	                TagNode currNode = (TagNode) item;
	                if ( condition.satisfy(currNode) ) {
	                    result.add(currNode);
	                }
	                if (isRecursive) {
	                    List innerList = currNode.getElementList(condition, isRecursive);
	                    if (innerList != null && innerList.size() > 0) {
	                        result.addAll(innerList);
	                    }
	                }
	            }
	            diffsleep(DEFAULT_SLEEP_TIME);
	        }

	        return result;
	    }

	    /**
	     * @param condition
	     * @param isRecursive
	     * @return The array of all subelemets that satisfy specified condition.
	     * @throws InterruptedException
	     */
	    private TagNode[] getElements(ITagNodeCondition condition, boolean isRecursive) throws InterruptedException {
	        final List list = getElementList(condition, isRecursive);
	        TagNode array[] = new TagNode[ list == null ? 0 : list.size() ];
	        for (int i = 0; i < list.size(); i++) {
	            array[i] = (TagNode) list.get(i);
	        }

	        return array;
	    }


	    public List getAllElementsList(boolean isRecursive) throws InterruptedException {
	        return getElementList( new TagAllCondition(), isRecursive );
	    }

	    public TagNode[] getAllElements(boolean isRecursive) throws InterruptedException {
	        return getElements( new TagAllCondition(), isRecursive );
	    }

	    public TagNode findElementByName(String findName, boolean isRecursive) {
	        return findElement( new TagNodeNameCondition(findName), isRecursive );
	    }

	    public List getElementListByName(String findName, boolean isRecursive) throws InterruptedException {
	        return getElementList( new TagNodeNameCondition(findName), isRecursive );
	    }

	    public TagNode[] getElementsByName(String findName, boolean isRecursive) throws InterruptedException {
	        return getElements( new TagNodeNameCondition(findName), isRecursive );
	    }

	    public TagNode findElementHavingAttribute(String attName, boolean isRecursive) {
	        return findElement( new TagNodeAttExistsCondition(attName), isRecursive );
	    }

	    public List getElementListHavingAttribute(String attName, boolean isRecursive) throws InterruptedException {
	        return getElementList( new TagNodeAttExistsCondition(attName), isRecursive );
	    }

	    public TagNode[] getElementsHavingAttribute(String attName, boolean isRecursive) throws InterruptedException {
	        return getElements( new TagNodeAttExistsCondition(attName), isRecursive );
	    }

	    public TagNode findElementByAttValue(String attName, String attValue, boolean isRecursive, boolean isCaseSensitive) {
	        return findElement( new TagNodeAttValueCondition(attName, attValue, isCaseSensitive), isRecursive );
	    }

	    public List getElementListByAttValue(String attName, String attValue, boolean isRecursive, boolean isCaseSensitive) throws InterruptedException {
	        return getElementList( new TagNodeAttValueCondition(attName, attValue, isCaseSensitive), isRecursive );
	    }

	    public TagNode[] getElementsByAttValue(String attName, String attValue, boolean isRecursive, boolean isCaseSensitive) throws InterruptedException {
	        return getElements( new TagNodeAttValueCondition(attName, attValue, isCaseSensitive), isRecursive );
	    }

	    /**
	     * Remove this node from the tree.
	     * @return True if element is removed (if it is not root node).
	     */
	    public boolean removeFromTree() {
	        if (parent != null) {
	            boolean existed = parent.removeChild(this);
	            parent = null;
	            return existed;
	        }
	        return false;
	    }

	    /**
	     * Remove specified child element from this node.
	     * @param child
	     * @return True if child object existed in the children list.
	     */
	    public boolean removeChild(Object child) {
	        return this.children.remove(child);
	    }

	    /**
	     * Removes all children (subelements and text content).
	     */
	    public void removeAllChildren() {
	        this.children.clear();
	    }

	    /**
	     * Replaces specified child node with specified replacement node.
	     * @param childToReplace Child node to be replaced
	     * @param replacement Replacement node
	     */
	    public void replaceChild(HtmlNode childToReplace, HtmlNode replacement) {
	        if (replacement == null) {
	            return;
	        }
	        ListIterator it = children.listIterator();
	        while (it.hasNext()) {
	            Object curr = it.next();
	            if (curr == childToReplace) {
	                it.set(replacement);
	                break;
	            }
	        }
	    }

	    /**
	     * @param child Child to find index of
	     * @return Index of the specified child node inside this node's children, -1 if node is not the child
	     */
	    public int getChildIndex(HtmlNode child) {
	        int index = 0;
	        for (Object curr: children) {
	            if (curr == child) {
	                return index;
	            }
	            index++;
	        }
	        return -1;
	    }

	    /**
	     * Inserts specified node at specified position in array of children
	     * @param index
	     * @param childToAdd
	     */
	    public void insertChild(int index, HtmlNode childToAdd) {
	        children.add(index, childToAdd);
	    }

	    /**
	     * Inserts specified node in the list of children before specified child
	     * @param node Child before which to insert new node
	     * @param nodeToInsert Node to be inserted at specified position
	     */
	    public void insertChildBefore(HtmlNode node, HtmlNode nodeToInsert) {
	        int index = getChildIndex(node);
	        if (index >= 0) {
	            insertChild(index, nodeToInsert);
	        }
	    }

	    /**
	     * Inserts specified node in the list of children after specified child
	     * @param node Child after which to insert new node
	     * @param nodeToInsert Node to be inserted at specified position
	     */
	    public void insertChildAfter(HtmlNode node, HtmlNode nodeToInsert) {
	        int index = getChildIndex(node);
	        if (index >= 0) {
	            insertChild(index + 1, nodeToInsert);
	        }
	    }

	    void addItemForMoving(BaseToken item) {
	    	if (itemsToMove == null) {
	    		itemsToMove = new ArrayList<BaseToken>();
	    	}

	    	itemsToMove.add(item);
	    }

	    List<BaseToken> getItemsToMove() {
			return itemsToMove;
		}

	    void setItemsToMove(List<BaseToken> itemsToMove) {
	        this.itemsToMove = itemsToMove;
	    }

		boolean isFormed() {
			return isFormed;
		}

		void setFormed(boolean isFormed) {
			this.isFormed = isFormed;
		}

		void setFormed() {
			setFormed(true);
		}

	    /**
	     * Collect all prefixes in namespace declarations up the path to the document root from the specified node
	     * @param prefixes Set of prefixes to be collected
	     */
	    void collectNamespacePrefixesOnPath(Set<String> prefixes) {
	        Map<String, String> nsDeclarations = getNamespaceDeclarations();
	        if (nsDeclarations != null) {
	            for (String prefix: nsDeclarations.keySet()) {
	                prefixes.add(prefix);
	            }
	        }
	        if (parent != null) {
	            parent.collectNamespacePrefixesOnPath(prefixes);
	        }
	    }

	    String getNamespaceURIOnPath(String nsPrefix) {
	        if (nsDeclarations != null) {
	            for (Map.Entry<String, String> nsEntry: nsDeclarations.entrySet()) {
	                String currName = nsEntry.getKey();
	                if ( currName.equals(nsPrefix) || ("".equals(currName) && nsPrefix == null) ) {
	                    return nsEntry.getValue();
	                }
	            }
	        }
	        if (parent != null) {
	            return parent.getNamespaceURIOnPath(nsPrefix);
	        }

	        return null;
	    }
	/*
	    public void serialize(Serializer serializer, Writer writer) throws IOException {
	    	serializer.serialize(this, writer);
	    }
	*/
	    TagNode makeCopy() {
	    	TagNode copy = new TagNode(name);
	        copy.attributes.putAll(attributes);
	    	return copy;
	    }
	}
	private class DefaultTagProvider extends HashMap<String, TagInfo> implements ITagInfoProvider {

	    /**
		 *
		 */
		private static final long serialVersionUID = 3676221418997924691L;
		// singleton instance, used if no other TagInfoProvider is specified
	    private DefaultTagProvider _instance;

	    /**
	     * @return Singleton instance of this class.
	     */
/*
	    public static synchronized DefaultTagProvider getInstance() {
	        if (_instance == null) {
	            _instance = this;
	        }
	        return _instance;
	    }
*/
	    public void setInstance(DefaultTagProvider _instance) {
	        if (_instance == null) {
	            this._instance = _instance;
	        }
	    }
	    public DefaultTagProvider() {
	        TagInfo tagInfo;

	        tagInfo = new TagInfo("div", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("div", tagInfo);

	        tagInfo = new TagInfo("span", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        this.put("span", tagInfo);

	        tagInfo = new TagInfo("meta", TagInfo.CONTENT_NONE, TagInfo.HEAD, false, false, false);
	        this.put("meta", tagInfo);

	        tagInfo = new TagInfo("link", TagInfo.CONTENT_NONE, TagInfo.HEAD, false, false, false);
	        this.put("link", tagInfo);

	        tagInfo = new TagInfo("title",  TagInfo.CONTENT_TEXT, TagInfo.HEAD, false, true, false);
	        this.put("title", tagInfo);

	        tagInfo = new TagInfo("style",  TagInfo.CONTENT_TEXT, TagInfo.HEAD, false, false, false);
	        this.put("style", tagInfo);

	        tagInfo = new TagInfo("bgsound", TagInfo.CONTENT_NONE, TagInfo.HEAD, false, false, false);
	        this.put("bgsound", tagInfo);

	        tagInfo = new TagInfo("h1", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("h1,h2,h3,h4,h5,h6,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("h1", tagInfo);

	        tagInfo = new TagInfo("h2", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("h1,h2,h3,h4,h5,h6,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("h2", tagInfo);

	        tagInfo = new TagInfo("h3", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("h1,h2,h3,h4,h5,h6,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("h3", tagInfo);

	        tagInfo = new TagInfo("h4", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("h1,h2,h3,h4,h5,h6,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("h4", tagInfo);

	        tagInfo = new TagInfo("h5", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("h1,h2,h3,h4,h5,h6,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("h5", tagInfo);

	        tagInfo = new TagInfo("h6", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("h1,h2,h3,h4,h5,h6,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("h6", tagInfo);

	        tagInfo = new TagInfo("p", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("p,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("p", tagInfo);

	        tagInfo = new TagInfo("strong", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        this.put("strong", tagInfo);

	        tagInfo = new TagInfo("em", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        this.put("em", tagInfo);

	        tagInfo = new TagInfo("abbr", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        this.put("abbr", tagInfo);

	        tagInfo = new TagInfo("acronym", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        this.put("acronym", tagInfo);

	        tagInfo = new TagInfo("address", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("address", tagInfo);

	        tagInfo = new TagInfo("bdo", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        this.put("bdo", tagInfo);

	        tagInfo = new TagInfo("blockquote", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("blockquote", tagInfo);

	        tagInfo = new TagInfo("cite", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        this.put("cite", tagInfo);

	        tagInfo = new TagInfo("q", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        this.put("q", tagInfo);

	        tagInfo = new TagInfo("code", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        this.put("code", tagInfo);

	        tagInfo = new TagInfo("ins", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        this.put("ins", tagInfo);

	        tagInfo = new TagInfo("del", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        this.put("del", tagInfo);

	        tagInfo = new TagInfo("dfn", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        this.put("dfn", tagInfo);

	        tagInfo = new TagInfo("kbd", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        this.put("kbd", tagInfo);

	        tagInfo = new TagInfo("pre", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("pre", tagInfo);

	        tagInfo = new TagInfo("samp", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        this.put("samp", tagInfo);

	        tagInfo = new TagInfo("listing", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("listing", tagInfo);

	        tagInfo = new TagInfo("var", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        this.put("var", tagInfo);

	        tagInfo = new TagInfo("br", TagInfo.CONTENT_NONE, TagInfo.BODY, false, false, false);
	        this.put("br", tagInfo);

	        tagInfo = new TagInfo("wbr", TagInfo.CONTENT_NONE, TagInfo.BODY, false, false, false);
	        this.put("wbr", tagInfo);

	        tagInfo = new TagInfo("nobr", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeTags("nobr");
	        this.put("nobr", tagInfo);

	        tagInfo = new TagInfo("xmp",  TagInfo.CONTENT_TEXT, TagInfo.BODY, false, false, false);
	        this.put("xmp", tagInfo);

	        tagInfo = new TagInfo("a", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeTags("a");
	        this.put("a", tagInfo);

	        tagInfo = new TagInfo("base", TagInfo.CONTENT_NONE, TagInfo.HEAD, false, false, false);
	        this.put("base", tagInfo);

	        tagInfo = new TagInfo("img", TagInfo.CONTENT_NONE, TagInfo.BODY, false, false, false);
	        this.put("img", tagInfo);

	        tagInfo = new TagInfo("area", TagInfo.CONTENT_NONE, TagInfo.BODY, false, false, false);
	        tagInfo.defineFatalTags("map");
	        tagInfo.defineCloseBeforeTags("area");
	        this.put("area", tagInfo);

	        tagInfo = new TagInfo("map", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeTags("map");
	        this.put("map", tagInfo);

	        tagInfo = new TagInfo("object", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        this.put("object", tagInfo);

	        tagInfo = new TagInfo("param", TagInfo.CONTENT_NONE, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("param", tagInfo);

	        tagInfo = new TagInfo("applet", TagInfo.CONTENT_ALL, TagInfo.BODY, true, false, false);
	        this.put("applet", tagInfo);

	        tagInfo = new TagInfo("xml", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        this.put("xml", tagInfo);

	        tagInfo = new TagInfo("ul", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("ul", tagInfo);

	        tagInfo = new TagInfo("ol", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("ol", tagInfo);

	        tagInfo = new TagInfo("li", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("li,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("li", tagInfo);

	        tagInfo = new TagInfo("dl", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("dl", tagInfo);

	        tagInfo = new TagInfo("dt", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeTags("dt,dd");
	        this.put("dt", tagInfo);

	        tagInfo = new TagInfo("dd", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeTags("dt,dd");
	        this.put("dd", tagInfo);

	        tagInfo = new TagInfo("menu", TagInfo.CONTENT_ALL, TagInfo.BODY, true, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("menu", tagInfo);

	        tagInfo = new TagInfo("dir", TagInfo.CONTENT_ALL, TagInfo.BODY, true, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("dir", tagInfo);

	        tagInfo = new TagInfo("table", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineAllowedChildrenTags("tr,tbody,thead,tfoot,colgroup,col,form,caption,tr");
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("tr,thead,tbody,tfoot,caption,colgroup,table,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param");
	        this.put("table", tagInfo);

	        tagInfo = new TagInfo("tr", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineFatalTags("table");
	        tagInfo.defineRequiredEnclosingTags("tbody");
	        tagInfo.defineAllowedChildrenTags("td,th");
	        tagInfo.defineHigherLevelTags("thead,tfoot");
	        tagInfo.defineCloseBeforeTags("tr,td,th,caption,colgroup");
	        this.put("tr", tagInfo);

	        tagInfo = new TagInfo("td", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineFatalTags("table");
	        tagInfo.defineRequiredEnclosingTags("tr");
	        tagInfo.defineCloseBeforeTags("td,th,caption,colgroup");
	        this.put("td", tagInfo);

	        tagInfo = new TagInfo("th", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineFatalTags("table");
	        tagInfo.defineRequiredEnclosingTags("tr");
	        tagInfo.defineCloseBeforeTags("td,th,caption,colgroup");
	        this.put("th", tagInfo);

	        tagInfo = new TagInfo("tbody", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineFatalTags("table");
	        tagInfo.defineAllowedChildrenTags("tr,form");
	        tagInfo.defineCloseBeforeTags("td,th,tr,tbody,thead,tfoot,caption,colgroup");
	        this.put("tbody", tagInfo);

	        tagInfo = new TagInfo("thead", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineFatalTags("table");
	        tagInfo.defineAllowedChildrenTags("tr,form");
	        tagInfo.defineCloseBeforeTags("td,th,tr,tbody,thead,tfoot,caption,colgroup");
	        this.put("thead", tagInfo);

	        tagInfo = new TagInfo("tfoot", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineFatalTags("table");
	        tagInfo.defineAllowedChildrenTags("tr,form");
	        tagInfo.defineCloseBeforeTags("td,th,tr,tbody,thead,tfoot,caption,colgroup");
	        this.put("tfoot", tagInfo);

	        tagInfo = new TagInfo("col", TagInfo.CONTENT_NONE, TagInfo.BODY, false, false, false);
	        tagInfo.defineFatalTags("table");
	        this.put("col", tagInfo);

	        tagInfo = new TagInfo("colgroup", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineFatalTags("table");
	        tagInfo.defineAllowedChildrenTags("col");
	        tagInfo.defineCloseBeforeTags("td,th,tr,tbody,thead,tfoot,caption,colgroup");
	        this.put("colgroup", tagInfo);

	        tagInfo = new TagInfo("caption", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineFatalTags("table");
	        tagInfo.defineCloseBeforeTags("td,th,tr,tbody,thead,tfoot,caption,colgroup");
	        this.put("caption", tagInfo);

	        tagInfo = new TagInfo("form", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, true);
	        tagInfo.defineForbiddenTags("form");
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("option,optgroup,textarea,select,fieldset,p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("form", tagInfo);

	        tagInfo = new TagInfo("input", TagInfo.CONTENT_NONE, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeTags("select,optgroup,option");
	        this.put("input", tagInfo);

	        tagInfo = new TagInfo("textarea", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeTags("select,optgroup,option");
	        this.put("textarea", tagInfo);

	        tagInfo = new TagInfo("select", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, true);
	        tagInfo.defineAllowedChildrenTags("option,optgroup");
	        tagInfo.defineCloseBeforeTags("option,optgroup,select");
	        this.put("select", tagInfo);

	        tagInfo = new TagInfo("option",  TagInfo.CONTENT_TEXT, TagInfo.BODY, false, false, true);
	        tagInfo.defineFatalTags("select");
	        tagInfo.defineCloseBeforeTags("option");
	        this.put("option", tagInfo);

	        tagInfo = new TagInfo("optgroup", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, true);
	        tagInfo.defineFatalTags("select");
	        tagInfo.defineAllowedChildrenTags("option");
	        tagInfo.defineCloseBeforeTags("optgroup");
	        this.put("optgroup", tagInfo);

	        tagInfo = new TagInfo("button", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeTags("select,optgroup,option");
	        this.put("button", tagInfo);

	        tagInfo = new TagInfo("label", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        this.put("label", tagInfo);

	        tagInfo = new TagInfo("fieldset", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("fieldset", tagInfo);

	        tagInfo = new TagInfo("legend", TagInfo.CONTENT_TEXT, TagInfo.BODY, false, false, false);
	        tagInfo.defineRequiredEnclosingTags("fieldset");
	        tagInfo.defineCloseBeforeTags("legend");
	        this.put("legend", tagInfo);

	        tagInfo = new TagInfo("isindex", TagInfo.CONTENT_NONE, TagInfo.BODY, true, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("isindex", tagInfo);

	        tagInfo = new TagInfo("script", TagInfo.CONTENT_ALL, TagInfo.HEAD_AND_BODY, false, false, false);
	        this.put("script", tagInfo);

	        tagInfo = new TagInfo("noscript", TagInfo.CONTENT_ALL, TagInfo.HEAD_AND_BODY, false, false, false);
	        this.put("noscript", tagInfo);

	        tagInfo = new TagInfo("b", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseInsideCopyAfterTags("u,i,tt,sub,sup,big,small,strike,blink,s");
	        this.put("b", tagInfo);

	        tagInfo = new TagInfo("i", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseInsideCopyAfterTags("b,u,tt,sub,sup,big,small,strike,blink,s");
	        this.put("i", tagInfo);

	        tagInfo = new TagInfo("u", TagInfo.CONTENT_ALL, TagInfo.BODY, true, false, false);
	        tagInfo.defineCloseInsideCopyAfterTags("b,i,tt,sub,sup,big,small,strike,blink,s");
	        this.put("u", tagInfo);

	        tagInfo = new TagInfo("tt", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseInsideCopyAfterTags("b,u,i,sub,sup,big,small,strike,blink,s");
	        this.put("tt", tagInfo);

	        tagInfo = new TagInfo("sub", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseInsideCopyAfterTags("b,u,i,tt,sup,big,small,strike,blink,s");
	        this.put("sub", tagInfo);

	        tagInfo = new TagInfo("sup", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseInsideCopyAfterTags("b,u,i,tt,sub,big,small,strike,blink,s");
	        this.put("sup", tagInfo);

	        tagInfo = new TagInfo("big", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseInsideCopyAfterTags("b,u,i,tt,sub,sup,small,strike,blink,s");
	        this.put("big", tagInfo);

	        tagInfo = new TagInfo("small", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseInsideCopyAfterTags("b,u,i,tt,sub,sup,big,strike,blink,s");
	        this.put("small", tagInfo);

	        tagInfo = new TagInfo("strike", TagInfo.CONTENT_ALL, TagInfo.BODY, true, false, false);
	        tagInfo.defineCloseInsideCopyAfterTags("b,u,i,tt,sub,sup,big,small,blink,s");
	        this.put("strike", tagInfo);

	        tagInfo = new TagInfo("blink", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseInsideCopyAfterTags("b,u,i,tt,sub,sup,big,small,strike,s");
	        this.put("blink", tagInfo);

	        tagInfo = new TagInfo("marquee", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("marquee", tagInfo);

	        tagInfo = new TagInfo("s", TagInfo.CONTENT_ALL, TagInfo.BODY, true, false, false);
	        tagInfo.defineCloseInsideCopyAfterTags("b,u,i,tt,sub,sup,big,small,strike,blink");
	        this.put("s", tagInfo);

	        tagInfo = new TagInfo("hr", TagInfo.CONTENT_NONE, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("hr", tagInfo);

	        tagInfo = new TagInfo("font", TagInfo.CONTENT_ALL, TagInfo.BODY, true, false, false);
	        this.put("font", tagInfo);

	        tagInfo = new TagInfo("basefont", TagInfo.CONTENT_NONE, TagInfo.BODY, true, false, false);
	        this.put("basefont", tagInfo);

	        tagInfo = new TagInfo("center", TagInfo.CONTENT_ALL, TagInfo.BODY, true, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("center", tagInfo);

	        tagInfo = new TagInfo("comment", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        this.put("comment", tagInfo);

	        tagInfo = new TagInfo("server", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        this.put("server", tagInfo);

	        tagInfo = new TagInfo("iframe", TagInfo.CONTENT_ALL, TagInfo.BODY, false, false, false);
	        this.put("iframe", tagInfo);

	        tagInfo = new TagInfo("embed", TagInfo.CONTENT_NONE, TagInfo.BODY, false, false, false);
	        tagInfo.defineCloseBeforeCopyInsideTags("a,bdo,strong,em,q,b,i,u,tt,sub,sup,big,small,strike,s,font");
	        tagInfo.defineCloseBeforeTags("p,address,label,abbr,acronym,dfn,kbd,samp,var,cite,code,param,xml");
	        this.put("embed", tagInfo);
	    }

	    public TagInfo getTagInfo(String tagName) {
	        return get(tagName);
	    }

	    /**
	     * Removes tag info with specified name.
	     * @param tagName Name of the tag to be removed from the tag provider.
	     */
	    public void removeTagInfo(String tagName) {
	        if (tagName != null) {
	            remove(tagName.toLowerCase());
	        }
	    }

	    /**
	     * Sets new tag info.
	     * @param tagInfo tag info to be added to the provider.
	     */
	    public void addTagInfo(TagInfo tagInfo) {
	        if (tagInfo != null) {
	            put(tagInfo.getName().toLowerCase(), tagInfo);
	        }
	    }

	}
	private interface ITagInfoProvider {
	    public TagInfo getTagInfo(String tagName);

	}
	private class TagInfo {
	    protected static final int HEAD_AND_BODY = 0;
		protected static final int HEAD = 1;
		protected static final int BODY = 2;

		protected static final int CONTENT_ALL = 0;
		protected static final int CONTENT_NONE = 1;
		protected static final int CONTENT_TEXT = 2;

	    private String name;
	    private int contentType;
	    private Set mustCloseTags = new HashSet();
	    private Set higherTags = new HashSet();
	    private Set childTags = new HashSet();
	    private Set permittedTags = new HashSet();
	    private Set copyTags = new HashSet();
	    private Set continueAfterTags = new HashSet();
	    private int belongsTo = BODY;
	    private String requiredParent = null;
	    private String fatalTag = null;
	    private boolean deprecated = false;
	    private boolean unique = false;
	    private boolean ignorePermitted = false;


	    public TagInfo(String name, int contentType, int belongsTo, boolean depricated, boolean unique, boolean ignorePermitted) {
	        this.name = name;
	        this.contentType = contentType;
	        this.belongsTo = belongsTo;
	        this.deprecated = depricated;
	        this.unique = unique;
	        this.ignorePermitted = ignorePermitted;
	    }

	    public void defineFatalTags(String commaSeparatedListOfTags) {
	        StringTokenizer tokenizer = new StringTokenizer(commaSeparatedListOfTags.toLowerCase(), ",");
	        while (tokenizer.hasMoreTokens()) {
	            String currTag = tokenizer.nextToken();
	            this.fatalTag = currTag;
	            this.higherTags.add(currTag);
	        }
	    }

	    public void defineRequiredEnclosingTags(String commaSeparatedListOfTags) {
	        StringTokenizer tokenizer = new StringTokenizer(commaSeparatedListOfTags.toLowerCase(), ",");
	        while (tokenizer.hasMoreTokens()) {
	            String currTag = tokenizer.nextToken();
	            this.requiredParent = currTag;
	            this.higherTags.add(currTag);
	        }
	    }

	    public void defineForbiddenTags(String commaSeparatedListOfTags) {
	        StringTokenizer tokenizer = new StringTokenizer(commaSeparatedListOfTags.toLowerCase(), ",");
	        while (tokenizer.hasMoreTokens()) {
	            String currTag = tokenizer.nextToken();
	            this.permittedTags.add(currTag);
	        }
	    }

	    public void defineAllowedChildrenTags(String commaSeparatedListOfTags) {
	        StringTokenizer tokenizer = new StringTokenizer(commaSeparatedListOfTags.toLowerCase(), ",");
	        while (tokenizer.hasMoreTokens()) {
	            String currTag = tokenizer.nextToken();
	            this.childTags.add(currTag);
	        }
	    }

	    public void defineHigherLevelTags(String commaSeparatedListOfTags) {
	        StringTokenizer tokenizer = new StringTokenizer(commaSeparatedListOfTags.toLowerCase(), ",");
	        while (tokenizer.hasMoreTokens()) {
	            String currTag = tokenizer.nextToken();
	            this.higherTags.add(currTag);
	        }
	    }

	    public void defineCloseBeforeCopyInsideTags(String commaSeparatedListOfTags) {
	        StringTokenizer tokenizer = new StringTokenizer(commaSeparatedListOfTags.toLowerCase(), ",");
	        while (tokenizer.hasMoreTokens()) {
	            String currTag = tokenizer.nextToken();
	            this.copyTags.add(currTag);
	            this.mustCloseTags.add(currTag);
	        }
	    }

	    public void defineCloseInsideCopyAfterTags(String commaSeparatedListOfTags) {
	        StringTokenizer tokenizer = new StringTokenizer(commaSeparatedListOfTags.toLowerCase(), ",");
	        while (tokenizer.hasMoreTokens()) {
	            String currTag = tokenizer.nextToken();
	            this.continueAfterTags.add(currTag);
	        }
	    }

	    public void defineCloseBeforeTags(String commaSeparatedListOfTags) {
	        StringTokenizer tokenizer = new StringTokenizer(commaSeparatedListOfTags.toLowerCase(), ",");
	        while (tokenizer.hasMoreTokens()) {
	            String currTag = tokenizer.nextToken();
	            this.mustCloseTags.add(currTag);
	        }
	    }

	    // getters and setters

	    public String getName() {
	        return name;
	    }

	    public void setName(String name) {
	        this.name = name;
	    }

	    public int getContentType() {
	        return contentType;
	    }

	    public Set getMustCloseTags() {
	        return mustCloseTags;
	    }

	    public void setMustCloseTags(Set mustCloseTags) {
	        this.mustCloseTags = mustCloseTags;
	    }

	    public Set getHigherTags() {
	        return higherTags;
	    }

	    public void setHigherTags(Set higherTags) {
	        this.higherTags = higherTags;
	    }

	    public Set getChildTags() {
	        return childTags;
	    }

	    public void setChildTags(Set childTags) {
	        this.childTags = childTags;
	    }

	    public Set getPermittedTags() {
	        return permittedTags;
	    }

	    public void setPermittedTags(Set permittedTags) {
	        this.permittedTags = permittedTags;
	    }

	    public Set getCopyTags() {
	        return copyTags;
	    }

	    public void setCopyTags(Set copyTags) {
	        this.copyTags = copyTags;
	    }

	    public Set getContinueAfterTags() {
	        return continueAfterTags;
	    }

	    public void setContinueAfterTags(Set continueAfterTags) {
	        this.continueAfterTags = continueAfterTags;
	    }

	    public String getRequiredParent() {
	        return requiredParent;
	    }

	    public void setRequiredParent(String requiredParent) {
	        this.requiredParent = requiredParent;
	    }

	    public int getBelongsTo() {
	        return belongsTo;
	    }

	    public void setBelongsTo(int belongsTo) {
	        this.belongsTo = belongsTo;
	    }

	    public String getFatalTag() {
	        return fatalTag;
	    }

	    public void setFatalTag(String fatalTag) {
	        this.fatalTag = fatalTag;
	    }

	    public boolean isDeprecated() {
	        return deprecated;
	    }

	    public void setDeprecated(boolean deprecated) {
	        this.deprecated = deprecated;
	    }

	    public boolean isUnique() {
	        return unique;
	    }

	    public void setUnique(boolean unique) {
	        this.unique = unique;
	    }

	    public boolean isIgnorePermitted() {
	        return ignorePermitted;
	    }

	    public boolean isEmptyTag() {
	        return CONTENT_NONE == contentType;
	    }

	    public void setIgnorePermitted(boolean ignorePermitted) {
	        this.ignorePermitted = ignorePermitted;
	    }

	    // other functionality

	    boolean allowsBody() {
	    	return CONTENT_NONE != contentType;
	    }

	    boolean isHigher(String tagName) {
	    	return higherTags.contains(tagName);
	    }

	    boolean isCopy(String tagName) {
	    	return copyTags.contains(tagName);
	    }

	    boolean hasCopyTags() {
	    	return !copyTags.isEmpty();
	    }

	    boolean isContinueAfter(String tagName) {
	    	return continueAfterTags.contains(tagName);
	    }

	    boolean hasPermittedTags() {
	    	return !permittedTags.isEmpty();
	    }

	    boolean isHeadTag() {
	    	return belongsTo == HEAD;
	    }

	    boolean isHeadAndBodyTag() {
	    	return belongsTo == HEAD || belongsTo == HEAD_AND_BODY;
	    }

	    boolean isMustCloseTag(TagInfo tagInfo) {
	        if (tagInfo != null) {
	            return mustCloseTags.contains( tagInfo.getName() ) || tagInfo.contentType == CONTENT_TEXT;
	        }

	        return false;
	    }

	    boolean allowsItem(BaseToken token) {
	        if ( contentType != CONTENT_NONE && token instanceof TagToken ) {
	            TagToken tagToken = (TagToken) token;
	            String tagName = tagToken.getName();
	            if ( "script".equals(tagName) ) {
	                return true;
	            }
	        }

	        if (CONTENT_ALL == contentType) {
	            if ( !childTags.isEmpty() ) {
	            	return token instanceof TagToken ? childTags.contains( ((TagToken)token).getName() ) : false;
	    		} else if ( !permittedTags.isEmpty() ) {
	    			return token instanceof TagToken ? !permittedTags.contains( ((TagToken)token).getName() ) : true;
	    		}
	            return true;
	        } else if ( CONTENT_TEXT == contentType ) {
	    		return !(token instanceof TagToken);
	    	}

	    	return false;
	    }

	    boolean allowsAnything() {
	    	return CONTENT_ALL == contentType && childTags.size() == 0;
	    }

	}
	private class CleanerProperties {

	    public static final String BOOL_ATT_SELF = "self";
	    public static final String BOOL_ATT_EMPTY = "empty";
	    public static final String BOOL_ATT_TRUE = "true";

	    ITagInfoProvider tagInfoProvider = null;

	    boolean advancedXmlEscape = true;
	    boolean useCdataForScriptAndStyle = true;
	    boolean recognizeUnicodeChars = true;
	    boolean omitUnknownTags = false;
	    boolean treatUnknownTagsAsContent = false;
	    boolean omitDeprecatedTags = false;
	    boolean treatDeprecatedTagsAsContent = false;
	    boolean omitComments = false;
	    boolean omitXmlDeclaration = false;
	    boolean omitDoctypeDeclaration = true;
	    boolean omitHtmlEnvelope = false;
	    boolean useEmptyElementTags = true;
	    boolean allowMultiWordAttributes = true;
	    boolean allowHtmlInsideAttributes = false;
	    boolean ignoreQuestAndExclam = true;
	    boolean namespacesAware = true;
	    String hyphenReplacementInComment = "=";
	    String pruneTags = null;
	    String booleanAttributeValues = BOOL_ATT_SELF;

	    public ITagInfoProvider getTagInfoProvider() {
	        return tagInfoProvider;
	    }

	    public boolean isAdvancedXmlEscape() {
	        return advancedXmlEscape;
	    }

	    public void setAdvancedXmlEscape(boolean advancedXmlEscape) {
	        this.advancedXmlEscape = advancedXmlEscape;
	    }
	    public boolean isUseCdataForScriptAndStyle() {
	        return useCdataForScriptAndStyle;
	    }

	    public void setUseCdataForScriptAndStyle(boolean useCdataForScriptAndStyle) {
	        this.useCdataForScriptAndStyle = useCdataForScriptAndStyle;
	    }
	    public boolean isRecognizeUnicodeChars() {
	        return recognizeUnicodeChars;
	    }

	    public void setRecognizeUnicodeChars(boolean recognizeUnicodeChars) {
	        this.recognizeUnicodeChars = recognizeUnicodeChars;
	    }

	    public boolean isOmitUnknownTags() {
	        return omitUnknownTags;
	    }

	    public void setOmitUnknownTags(boolean omitUnknownTags) {
	        this.omitUnknownTags = omitUnknownTags;
	    }

	    public boolean isTreatUnknownTagsAsContent() {
	        return treatUnknownTagsAsContent;
	    }

	    public void setTreatUnknownTagsAsContent(boolean treatUnknownTagsAsContent) {
	        this.treatUnknownTagsAsContent = treatUnknownTagsAsContent;
	    }

	    public boolean isOmitDeprecatedTags() {
	        return omitDeprecatedTags;
	    }

	    public void setOmitDeprecatedTags(boolean omitDeprecatedTags) {
	        this.omitDeprecatedTags = omitDeprecatedTags;
	    }

	    public boolean isTreatDeprecatedTagsAsContent() {
	        return treatDeprecatedTagsAsContent;
	    }

	    public void setTreatDeprecatedTagsAsContent(boolean treatDeprecatedTagsAsContent) {
	        this.treatDeprecatedTagsAsContent = treatDeprecatedTagsAsContent;
	    }

	    public boolean isOmitComments() {
	        return omitComments;
	    }

	    public void setOmitComments(boolean omitComments) {
	        this.omitComments = omitComments;
	    }

	    public boolean isOmitXmlDeclaration() {
	        return omitXmlDeclaration;
	    }

	    public void setOmitXmlDeclaration(boolean omitXmlDeclaration) {
	        this.omitXmlDeclaration = omitXmlDeclaration;
	    }

	    public boolean isOmitDoctypeDeclaration() {
	        return omitDoctypeDeclaration;
	    }

	    public void setOmitDoctypeDeclaration(boolean omitDoctypeDeclaration) {
	        this.omitDoctypeDeclaration = omitDoctypeDeclaration;
	    }

	    public boolean isOmitHtmlEnvelope() {
	        return omitHtmlEnvelope;
	    }

	    public void setOmitHtmlEnvelope(boolean omitHtmlEnvelope) {
	        this.omitHtmlEnvelope = omitHtmlEnvelope;
	    }

	    public boolean isUseEmptyElementTags() {
	        return useEmptyElementTags;
	    }

	    public void setUseEmptyElementTags(boolean useEmptyElementTags) {
	        this.useEmptyElementTags = useEmptyElementTags;
	    }

	    public boolean isAllowMultiWordAttributes() {
	        return allowMultiWordAttributes;
	    }

	    public void setAllowMultiWordAttributes(boolean allowMultiWordAttributes) {
	        this.allowMultiWordAttributes = allowMultiWordAttributes;
	    }

	    public boolean isAllowHtmlInsideAttributes() {
	        return allowHtmlInsideAttributes;
	    }

	    public void setAllowHtmlInsideAttributes(boolean allowHtmlInsideAttributes) {
	        this.allowHtmlInsideAttributes = allowHtmlInsideAttributes;
	    }

	    public boolean isIgnoreQuestAndExclam() {
	        return ignoreQuestAndExclam;
	    }

	    public void setIgnoreQuestAndExclam(boolean ignoreQuestAndExclam) {
	        this.ignoreQuestAndExclam = ignoreQuestAndExclam;
	    }

	    public boolean isNamespacesAware() {
	        return namespacesAware;
	    }

	    public void setNamespacesAware(boolean namespacesAware) {
	        this.namespacesAware = namespacesAware;
	    }

	    public String getHyphenReplacementInComment() {
	        return hyphenReplacementInComment;
	    }

	    public void setHyphenReplacementInComment(String hyphenReplacementInComment) {
	        this.hyphenReplacementInComment = hyphenReplacementInComment;
	    }

	    public String getPruneTags() {
	        return pruneTags;
	    }

	    public void setPruneTags(String pruneTags) {
	        this.pruneTags = pruneTags;
	    }

	    public String getBooleanAttributeValues() {
	        return booleanAttributeValues;
	    }

	    public void setBooleanAttributeValues(String booleanAttributeValues) {
	        if ( BOOL_ATT_SELF.equalsIgnoreCase(booleanAttributeValues) ||
	             BOOL_ATT_EMPTY.equalsIgnoreCase(booleanAttributeValues) ||
	             BOOL_ATT_TRUE.equalsIgnoreCase(booleanAttributeValues) ) {
	            this.booleanAttributeValues = booleanAttributeValues.toLowerCase();
	        } else {
	            this.booleanAttributeValues = BOOL_ATT_SELF;
	        }
	    }

	}
	private class ContentNode implements BaseToken, HtmlNode {

	    private StringBuilder content;

	    public ContentNode(String content) {
	        this.content = new StringBuilder(content);
	    }

	    ContentNode(char content[], int len) {
	        this.content = new StringBuilder(len + 16);
	        this.content.append(content, 0, len);
	    }

	    public String toString() {
	        return content.toString();
	    }

	    public StringBuilder getContent() {
	        return content;
	    }
	}
	private class EndTagToken extends TagToken {

	    public EndTagToken() {
	    }

	    public EndTagToken(String name) {
	        super(name == null ? null : name.toLowerCase());
	    }

	    void setAttribute(String attName, String attValue) {
	        // do nothing - simply ignore attributes in closing tag
	    }
	/*
	    public void serialize(Serializer serializer, Writer writer) {
	    	// do nothing - simply ignore serialization
	    }
	*/
	}
	abstract private class HtmlTokenizer {

		private final static int WORKING_BUFFER_SIZE = 1024;

	    private BufferedReader _reader;
	    private char[] _working = new char[WORKING_BUFFER_SIZE];

	    private transient int _pos = 0;
	    private transient int _len = -1;

	    private transient char _saved[] = new char[512];
	    private transient int _savedLen = 0;

//	    private transient DoctypeToken _docType = null;
	    private transient TagToken _currentTagToken = null;
	    private transient List<BaseToken> _tokenList = new ArrayList<BaseToken>();

	    private boolean _asExpected = true;

	    private boolean _isScriptContext = false;

	    private CleanerProperties props;

	    private boolean isOmitUnknownTags;
	    private boolean isTreatUnknownTagsAsContent;
	    private boolean isOmitDeprecatedTags;
	    private boolean isTreatDeprecatedTagsAsContent;
	    private boolean isNamespacesAware;
	    private boolean isOmitComments;
	    private boolean isAllowMultiWordAttributes;
	    private boolean isAllowHtmlInsideAttributes;

//	    private CleanerTransformations transformations;
	    private ITagInfoProvider tagInfoProvider;

	    private StringBuilder commonStr = new StringBuilder();

	    /**
	     * Constructor - cretes instance of the parser with specified content.
	     *
	     * @param reader
	     * @param props
	     * @param transformations
	     * @param tagInfoProvider
	     *
	     * @throws IOException
	     */
//	    public HtmlTokenizer(Reader reader, CleanerProperties props, CleanerTransformations transformations, ITagInfoProvider tagInfoProvider) throws IOException {
	    public HtmlTokenizer(Reader reader, CleanerProperties props, ITagInfoProvider tagInfoProvider) throws IOException {
	        this._reader = new BufferedReader(reader);
	        this.props = props;
	        this.isOmitUnknownTags = props.isOmitUnknownTags();
	        this.isTreatUnknownTagsAsContent = props.isTreatUnknownTagsAsContent();
	        this.isOmitDeprecatedTags = props.isOmitDeprecatedTags();
	        this.isTreatDeprecatedTagsAsContent = props.isTreatDeprecatedTagsAsContent();
	        this.isNamespacesAware = props.isNamespacesAware();
	        this.isOmitComments = props.isOmitComments();
	        this.isAllowMultiWordAttributes = props.isAllowMultiWordAttributes();
	        this.isAllowHtmlInsideAttributes = props.isAllowHtmlInsideAttributes();
//	        this.transformations = transformations;
	        this.tagInfoProvider = tagInfoProvider;
	    }

	    private void addToken(BaseToken token) throws InterruptedException {
	        _tokenList.add(token);
	        makeTree(_tokenList);
	    }

	    abstract void makeTree(List<BaseToken> tokenList) throws InterruptedException;

	    abstract TagNode createTagNode(String name);

	    private void readIfNeeded(int neededChars) throws IOException {
	        if (_len == -1 && _pos + neededChars >= WORKING_BUFFER_SIZE) {
	            int numToCopy = WORKING_BUFFER_SIZE - _pos;
	            System.arraycopy(_working, _pos, _working, 0, numToCopy);
	    		_pos = 0;

	            int expected = WORKING_BUFFER_SIZE - numToCopy;
	            int size = 0;
	            int charsRead;
	            int offset = numToCopy;
	            do {
	                charsRead = _reader.read(_working, offset, expected);
	                if (charsRead >= 0) {
	                    size += charsRead;
	                    offset += charsRead;
	                    expected -= charsRead;
	                }
	            } while (charsRead >= 0 && expected > 0);

	            if (expected > 0) {
	    			_len = size + numToCopy;
	            }

	            // convert invalid XML characters to spaces
	            for (int i = 0; i < (_len >= 0 ? _len : WORKING_BUFFER_SIZE); i++) {
	                int ch = _working[i];
	                if (ch >= 1 && ch <= 32 && ch != 10 && ch != 13) {
	                    _working[i] = ' ';
	                }
	            }
	        }
	    }

	    List<BaseToken> getTokenList() {
	    	return this._tokenList;
	    }

	    private void go() throws IOException {
	    	_pos++;
	    	readIfNeeded(0);
	    }

	    private void go(int step) throws IOException {
	    	_pos += step;
	    	readIfNeeded(step - 1);
	    }

	    /**
	     * Checks if content starts with specified value at the current position.
	     * @param value
	     * @return true if starts with specified value, false otherwise.
	     * @throws IOException
	     */
	    private boolean startsWith(String value) throws IOException {
	        int valueLen = value.length();
	        readIfNeeded(valueLen);
	        if (_len >= 0 && _pos + valueLen  > _len) {
	            return false;
	        }

	        for (int i = 0; i < valueLen; i++) {
	        	char ch1 = Character.toLowerCase( value.charAt(i) );
	        	char ch2 = Character.toLowerCase( _working[_pos + i] );
	        	if (ch1 != ch2) {
	        		return false;
	        	}
	        }

	        return true;
	    }

	    private boolean startsWithSimple(String value) throws IOException {
	        int valueLen = value.length();
	        readIfNeeded(valueLen);
	        if (_len >= 0 && _pos + valueLen  > _len) {
	            return false;
	        }

	        for (int i = 0; i < valueLen; i++) {
	        	if (value.charAt(i) != _working[_pos + i]) {
	        		return false;
	        	}
	        }

	        return true;
	    }

	    /**
	     * Checks if character at specified position is whitespace.
	     * @param position
	     * @return true is whitespace, false otherwise.
	     */
	    private boolean isWhitespace(int position) {
	    	if (_len >= 0 && position >= _len) {
	            return false;
	        }

	        return Character.isWhitespace( _working[position] );
	    }

	    /**
	     * Checks if character at current runtime position is whitespace.
	     * @return true is whitespace, false otherwise.
	     */
	    private boolean isWhitespace() {
	        return isWhitespace(_pos);
	    }

	    private boolean isWhitespaceSafe() {
	        return Character.isWhitespace( _working[_pos] );
	    }

	    /**
	     * Checks if character at specified position is equal to specified char.
	     * @param position
	     * @param ch
	     * @return true is equals, false otherwise.
	     */
	    private boolean isChar(int position, char ch) {
	    	if (_len >= 0 && position >= _len) {
	            return false;
	        }

	        return Character.toLowerCase(ch) == Character.toLowerCase(_working[position]);
	    }

	    /**
	     * Checks if character at current runtime position is equal to specified char.
	     * @param ch
	     * @return true is equal, false otherwise.
	     */
	    private boolean isChar(char ch) {
	        return isChar(_pos, ch);
	    }

	    private boolean isCharSimple(char ch) {
	        return (_len < 0 || _pos < _len) && (ch == _working[_pos]);
	    }

	    /**
	     * @return Current character to be read, but first it must be checked if it exists.
	     * This method is made for performance reasons to be used instead of isChar(...).
	     */
	    private char getCurrentChar() {
	        return _working[_pos];
	    }

	    private boolean isCharEquals(char ch) {
	        return _working[_pos] == ch;
	    }

	    /**
	     * Checks if character at specified position can be identifier start.
	     * @param position
	     * @return true is may be identifier start, false otherwise.
	     */
	    private boolean isIdentifierStartChar(int position) {
	    	if (_len >= 0 && position >= _len) {
	            return false;
	        }

	        char ch = _working[position];
	        return Character.isUnicodeIdentifierStart(ch) || ch == '_';
	    }

	    /**
	     * Checks if character at current runtime position can be identifier start.
	     * @return true is may be identifier start, false otherwise.
	     */
	    private boolean isIdentifierStartChar() {
	        return isIdentifierStartChar(_pos);
	    }

	    /**
	     * Checks if character at current runtime position can be identifier part.
	     * @return true is may be identifier part, false otherwise.
	     */
	    private boolean isIdentifierChar() {
	    	if (_len >= 0 && _pos >= _len) {
	            return false;
	        }

	        char ch = _working[_pos];
	        return Character.isUnicodeIdentifierStart(ch) || Character.isDigit(ch) || Utils.isIdentifierHelperChar(ch);
	    }

	    private boolean isValidXmlChar() {
	        return isAllRead() || Utils.isValidXmlChar(_working[_pos]);
	    }

	    private boolean isValidXmlCharSafe() {
	        return Utils.isValidXmlChar(_working[_pos]);
	    }

	    /**
	     * Checks if end of the content is reached.
	     */
	    private boolean isAllRead() {
	        return _len >= 0 && _pos >= _len;
	    }

	    /**
	     * Saves specified character to the temporary buffer.
	     * @param ch
	     */
	    private void save(char ch) {
	        if (_savedLen >= _saved.length) {
	            char newSaved[] = new char[_saved.length + 512];
	            System.arraycopy(_saved, 0, newSaved, 0, _saved.length);
	            _saved = newSaved;
	        }
	        _saved[_savedLen++] = ch;
	    }

	    /**
	     * Saves character at current runtime position to the temporary buffer.
	     */
	    private void saveCurrent() {
	        if (!isAllRead()) {
	            save( _working[_pos] );
	        }
	    }

	    private void saveCurrentSafe() {
	        save( _working[_pos] );
	    }

	    /**
	     * Saves specified number of characters at current runtime position to the temporary buffer.
	     * @throws IOException
	     */
	    private void saveCurrent(int size) throws IOException {
	    	readIfNeeded(size);
	        int pos = _pos;
	        while ( !isAllRead() && (size > 0) ) {
	            save( _working[pos] );
	            pos++;
	            size--;
	        }
	    }

	    /**
	     * Skips whitespaces at current position and moves foreward until
	     * non-whitespace character is found or the end of content is reached.
	     * @throws IOException
	     */
	    private void skipWhitespaces() throws IOException {
	        while ( !isAllRead() && isWhitespaceSafe() ) {
	            saveCurrentSafe();
	            go();
	        }
	    }

	    private boolean addSavedAsContent() throws InterruptedException {
	        if (_savedLen > 0) {
	            addToken(new ContentNode(_saved, _savedLen));
	            _savedLen = 0;
	            return true;
	        }

	        return false;
	    }

	    /**
	     * Starts parsing HTML.
	     * @throws IOException
	     * @throws InterruptedException
	     */
	    void start() throws IOException, InterruptedException {
	    	// initialize runtime values
	        _currentTagToken = null;
	        _tokenList.clear();
	        _asExpected = true;
	        _isScriptContext = false;

	        boolean isLateForDoctype = false;

	        this._pos = WORKING_BUFFER_SIZE;
	        readIfNeeded(0);

	        boolean isScriptEmpty = true;

	        while ( !isAllRead() ) {
	            // resets all the runtime values
	            _savedLen = 0;
	            _currentTagToken = null;
	            _asExpected = true;

	            // this is enough for making decision
	            readIfNeeded(10);

	            if (_isScriptContext) {
	                if ( startsWith("</script") && (isWhitespace(_pos + 8) || isChar(_pos + 8, '>')) ) {
	                    tagEnd();
	                } else if ( isScriptEmpty && startsWithSimple("<!--") ) {
	                    comment();
	                } else {
	                    boolean isTokenAdded = content();
	                    if (isScriptEmpty && isTokenAdded) {
	                        final BaseToken lastToken = _tokenList.get(_tokenList.size() - 1);
	                        if (lastToken != null) {
	                            final String lastTokenAsString = lastToken.toString();
	                            if (lastTokenAsString != null && lastTokenAsString.trim().length() > 0) {
	                                isScriptEmpty = false;
	                            }
	                        }
	                    }
	                }
	                if (!_isScriptContext) {
	                    isScriptEmpty = true;
	                }
	            } else {
	                if ( startsWith("<!doctype") ) {
	                	if ( !isLateForDoctype ) {
	                		doctype();
	                		isLateForDoctype = true;
	                	} else {
	                		ignoreUntil('<');
	                	}
	                } else if ( startsWithSimple("</") && isIdentifierStartChar(_pos + 2) ) {
	                	isLateForDoctype = true;
	                    tagEnd();
	                } else if ( startsWithSimple("<!--") ) {
	                    comment();
	                } else if ( startsWithSimple("<") && isIdentifierStartChar(_pos + 1) ) {
	                	isLateForDoctype = true;
	                    tagStart();
	                } else if ( props.isIgnoreQuestAndExclam() && (startsWithSimple("<!") || startsWithSimple("<?")) ) {
	                    ignoreUntil('>');
	                    if (isCharSimple('>')) {
	                        go();
	                    }
	                } else {
	                    content();
	                }
	            }
	            diffsleep(DEFAULT_SLEEP_TIME);
	        }

	        _reader.close();
	    }

	    /**
	     * Checks if specified tag name is one of the reserved tags: HTML, HEAD or BODY
	     * @param tagName
	     * @return
	     */
	    private boolean isReservedTag(String tagName) {
	        tagName = tagName.toLowerCase();
	        return "html".equals(tagName) || "head".equals(tagName) || "body".equals(tagName);
	    }

	    /**
	     * Parses start of the tag.
	     * It expects that current position is at the "<" after which
	     * the tag's name follows.
	     * @throws IOException
	     * @throws InterruptedException
	     */
	    private void tagStart() throws IOException, InterruptedException {
	        saveCurrent();
	        go();

	        if ( isAllRead() ) {
	            return;
	        }

	        String tagName = identifier();

//	        TagTransformation tagTransformation = null;
	/*
	        if (transformations != null && transformations.hasTransformationForTag(tagName)) {
	            tagTransformation = transformations.getTransformation(tagName);
	            if (tagTransformation != null) {
	                tagName = tagTransformation.getDestTag();
	            }
	        }
	*/
	        if (tagName != null) {
	            TagInfo tagInfo = tagInfoProvider.getTagInfo(tagName);
	            if ( (tagInfo == null && !isOmitUnknownTags && isTreatUnknownTagsAsContent && !isReservedTag(tagName)) ||
	                 (tagInfo != null && tagInfo.isDeprecated() && !isOmitDeprecatedTags && isTreatDeprecatedTagsAsContent) ) {
	                content();
	                return;
	            }
	        }

	        TagNode tagNode = createTagNode(tagName);
	        _currentTagToken = tagNode;

	        if (_asExpected) {
	            skipWhitespaces();
	            tagAttributes();

	            if (tagName != null) {
	/*
	                if (tagTransformation != null) {
	                    tagNode.transformAttributes(tagTransformation);
	                }
	*/
	                addToken(_currentTagToken);
	            }

	            if ( isCharSimple('>') ) {
	            	go();
	                if ( "script".equalsIgnoreCase(tagName) ) {
	                    _isScriptContext = true;
	                }
	            } else if ( startsWithSimple("/>") ) {
	            	go(2);
	                if ( "script".equalsIgnoreCase(tagName) ) {
	                    addToken( new EndTagToken(tagName) );
	                }
	            }

	            _currentTagToken = null;
	        } else {
	        	addSavedAsContent();
	        }
	    }


	    /**
	     * Parses end of the tag.
	     * It expects that current position is at the "<" after which
	     * "/" and the tag's name follows.
	     * @throws IOException
	     * @throws InterruptedException
	     */
	    private void tagEnd() throws IOException, InterruptedException {
	        saveCurrent(2);
	        go(2);

	        if ( isAllRead() ) {
	            return;
	        }

	        String tagName = identifier();
	/*
	        if (transformations != null && transformations.hasTransformationForTag(tagName)) {
	            TagTransformation tagTransformation = transformations.getTransformation(tagName);
	            if (tagTransformation != null) {
	                tagName = tagTransformation.getDestTag();
	            }
	        }
	*/
	        if (tagName != null) {
	            TagInfo tagInfo = tagInfoProvider.getTagInfo(tagName);
	            if ( (tagInfo == null && !isOmitUnknownTags && isTreatUnknownTagsAsContent && !isReservedTag(tagName)) ||
	                 (tagInfo != null && tagInfo.isDeprecated() && !isOmitDeprecatedTags && isTreatDeprecatedTagsAsContent) ) {
	                content();
	                return;
	            }
	        }

	        _currentTagToken = new EndTagToken(tagName);

	        if (_asExpected) {
	            skipWhitespaces();
	            tagAttributes();

	            if (tagName != null) {
	                addToken(_currentTagToken);
	            }

	            if ( isCharSimple('>') ) {
	            	go();
	            }

	            if ( "script".equalsIgnoreCase(tagName) ) {
	                _isScriptContext = false;
	            }

	            _currentTagToken = null;
	        } else {
	            addSavedAsContent();
	        }
	    }

	    /**
	     * Parses an identifier from the current position.
	     * @throws IOException
	     */
	    private String identifier() throws IOException {
	        _asExpected = true;

	        if ( !isIdentifierStartChar() ) {
	            _asExpected = false;
	            return null;
	        }

	        commonStr.delete(0, commonStr.length());

	        while ( !isAllRead() && isIdentifierChar() ) {
	            saveCurrentSafe();
	            commonStr.append( _working[_pos] );
	            go();
	        }

	        // strip invalid characters from the end
	        while ( commonStr.length() > 0 && Utils.isIdentifierHelperChar(commonStr.charAt(commonStr.length() - 1)) ) {
	            commonStr.deleteCharAt( commonStr.length() - 1 );
	        }

	        if ( commonStr.length() == 0 ) {
	            return null;
	        }

	        String id = commonStr.toString();

	        int columnIndex = id.indexOf(':');
	        if (columnIndex >= 0) {
	            String prefix = id.substring(0, columnIndex);
	            String suffix = id.substring(columnIndex + 1);
	            int nextColumnIndex = suffix.indexOf(':');
	            if (nextColumnIndex >= 0) {
	                suffix = suffix.substring(0, nextColumnIndex);
	            }
	            id = isNamespacesAware ? (prefix + ":" + suffix) : suffix;
	        }

	        return id;
	    }

	    /**
	     * Parses list tag attributes from the current position.
	     * @throws IOException
	     */
	    private void tagAttributes() throws IOException {
	        while( !isAllRead() && _asExpected && !isCharSimple('>') && !startsWithSimple("/>") ) {
	            skipWhitespaces();
	            String attName = identifier();

	            if (!_asExpected) {
	                if ( !isCharSimple('<') && !isCharSimple('>') && !startsWithSimple("/>") ) {
	                    if (isValidXmlChar()) {
	                        saveCurrent();
	                    }
	                    go();
	                }

	                if (!isCharSimple('<')) {
	                    _asExpected = true;
	                }

	                continue;
	            }

	            String attValue;

	            skipWhitespaces();
	            if ( isCharSimple('=') ) {
	                saveCurrentSafe();
	                go();
	                attValue = attributeValue();
	            } else if (CleanerProperties.BOOL_ATT_EMPTY.equals(props.booleanAttributeValues)) {
	                attValue = "";
	            } else if (CleanerProperties.BOOL_ATT_TRUE.equals(props.booleanAttributeValues)) {
	                attValue = "true";
	            } else {
	                attValue = attName;
	            }

	            if (_asExpected) {
	                _currentTagToken.setAttribute(attName, attValue);
	            }
	        }
	    }

	    /**
	     * Parses a single tag attribute - it is expected to be in one of the forms:
	     * 		name=value
	     * 		name="value"
	     * 		name='value'
	     * 		name
	     * @throws IOException
	     */
	    private String attributeValue() throws IOException {
	        skipWhitespaces();

	        if ( isCharSimple('<') || isCharSimple('>') || startsWithSimple("/>") ) {
	        	return "";
	        }

	        boolean isQuoteMode = false;
	        boolean isAposMode = false;

	        commonStr.delete(0, commonStr.length());

	        if ( isCharSimple('\'') ) {
	            isAposMode = true;
	            saveCurrentSafe();
	            go();
	        } else if ( isCharSimple('\"') ) {
	            isQuoteMode = true;
	            saveCurrentSafe();
	            go();
	        }

	        while ( !isAllRead() &&
	                ( ((isAposMode && !isCharEquals('\'') || isQuoteMode && !isCharEquals('\"')) && (isAllowHtmlInsideAttributes || !isCharEquals('>') && !isCharEquals('<')) && (isAllowMultiWordAttributes || !isWhitespaceSafe())) ||
	                  (!isAposMode && !isQuoteMode && !isWhitespaceSafe() && !isCharEquals('>') && !isCharEquals('<'))
	                )
	              ) {
	            if (isValidXmlCharSafe()) {
	                commonStr.append( _working[_pos] );
	                saveCurrentSafe();
	            }
	            go();
	        }

	        if ( isCharSimple('\'') && isAposMode ) {
	            saveCurrentSafe();
	            go();
	        } else if ( isCharSimple('\"') && isQuoteMode ) {
	            saveCurrentSafe();
	            go();
	        }


	        return commonStr.toString();
	    }

	    private boolean content() throws IOException, InterruptedException {
	        while ( !isAllRead() ) {
	            if (isValidXmlCharSafe()) {
	                saveCurrentSafe();
	            }
	            go();

	            if ( isCharSimple('<') ) {
	                break;
	            }
	        }

	        return addSavedAsContent();
	    }

	    private void ignoreUntil(char ch) throws IOException {
	        while ( !isAllRead() ) {
	        	go();
	            if ( isChar(ch) ) {
	                break;
	            }
	        }
	    }

	    private void comment() throws IOException, InterruptedException {
	    	go(4);
	        while ( !isAllRead() && !startsWithSimple("-->") ) {
	            if (isValidXmlCharSafe()) {
	                saveCurrentSafe();
	            }
	            go();
	        }

	        if (startsWithSimple("-->")) {
	        	go(3);
	        }

	        if (_savedLen > 0) {
	            if (!isOmitComments) {
	                String hyphenRepl = props.getHyphenReplacementInComment();
	                String comment = new String(_saved, 0, _savedLen).replaceAll("--", hyphenRepl + hyphenRepl);

	        		if ( comment.length() > 0 && comment.charAt(0) == '-' ) {
	        			comment = hyphenRepl + comment.substring(1);
	        		}
	        		int len = comment.length();
	        		if ( len > 0 && comment.charAt(len - 1) == '-' ) {
	        			comment = comment.substring(0, len - 1) + hyphenRepl;
	        		}

	        		addToken( new CommentNode(comment) );
	        	}
	            _savedLen = 0;
	        }
	    }

	    private void doctype() throws IOException {
	    	go(9);

	    	skipWhitespaces();
	    	String part1 = identifier();
		    skipWhitespaces();
		    String part2 = identifier();
		    skipWhitespaces();
		    String part3 = attributeValue();
		    skipWhitespaces();
		    String part4 = attributeValue();

		    ignoreUntil('<');
	    }
	}
	private class HtmlCleaner {

	    private String DEFAULT_CHARSET = System.getProperty("file.encoding");

	    /**
	     * Contains information about single open tag
	     */
	    private class TagPos {
			private int position;
			private String name;
			private TagInfo info;

			TagPos(int position, String name) {
				this.position = position;
				this.name = name;
	            this.info = tagInfoProvider.getTagInfo(name);
	        }
		}

	    /**
	     * Class that contains information and mathods for managing list of open,
	     * but unhandled tags.
	     */
	    private class OpenTags {
	        private List<TagPos> list = new ArrayList<TagPos>();
	        private TagPos last = null;
	        private Set<String> set = new HashSet<String>();

	        private boolean isEmpty() {
	            return list.isEmpty();
	        }

	        private void addTag(String tagName, int position) {
	            last = new TagPos(position, tagName);
	            list.add(last);
	            set.add(tagName);
	        }

	        private void removeTag(String tagName) {
	            ListIterator<TagPos> it = list.listIterator( list.size() );
	            while ( it.hasPrevious() ) {
	                TagPos currTagPos = it.previous();
	                if (tagName.equals(currTagPos.name)) {
	                    it.remove();
	                    break;
	                }
	            }

	            last =  list.isEmpty() ? null : list.get( list.size() - 1 );
	        }

	        private TagPos findFirstTagPos() {
	            return list.isEmpty() ? null : list.get(0);
	        }

	        private TagPos getLastTagPos() {
	            return last;
	        }

	        private TagPos findTag(String tagName) {
	            if (tagName != null) {
	                ListIterator<TagPos> it = list.listIterator(list.size());
	                String fatalTag = null;
	                TagInfo fatalInfo = tagInfoProvider.getTagInfo(tagName);
	                if (fatalInfo != null) {
	                    fatalTag = fatalInfo.getFatalTag();
	                }

	                while (it.hasPrevious()) {
	                    TagPos currTagPos = it.previous();
	                    if (tagName.equals(currTagPos.name)) {
	                        return currTagPos;
	                    } else if (fatalTag != null && fatalTag.equals(currTagPos.name)) {
	                        // do not search past a fatal tag for this tag
	                        return null;
	                    }
	                }
	            }

	            return null;
	        }

	        private boolean tagExists(String tagName) {
	            TagPos tagPos = findTag(tagName);
	            return tagPos != null;
	        }

	        private TagPos findTagToPlaceRubbish() {
	            TagPos result = null, prev = null;

	            if ( !isEmpty() ) {
	                ListIterator<TagPos> it = list.listIterator( list.size() );
	                while ( it.hasPrevious() ) {
	                    result = it.previous();
	                    if ( result.info == null || result.info.allowsAnything() ) {
	                    	if (prev != null) {
	                            return prev;
	                        }
	                    }
	                    prev = result;
	                }
	            }

	            return result;
	        }

	        private boolean tagEncountered(String tagName) {
	        	return set.contains(tagName);
	        }

	        /**
	         * Checks if any of tags specified in the set are already open.
	         * @param tags
	         */
	        private boolean someAlreadyOpen(Set tags) {
	        	Iterator<TagPos> it = list.iterator();
	            while ( it.hasNext() ) {
	            	TagPos curr = it.next();
	            	if ( tags.contains(curr.name) ) {
	            		return true;
	            	}
	            }


	            return false;
	        }
	    }

	    private class CleanTimeValues {
	        private OpenTags _openTags;
	        private boolean _headOpened = false;
	        private boolean _bodyOpened = false;
	        private Set _headTags = new LinkedHashSet();
	        private Set allTags = new TreeSet();

	        private TagNode htmlNode;
	        private TagNode bodyNode;
	        private TagNode headNode;
	        private TagNode rootNode;

	        private Set<String> pruneTagSet = new HashSet<String>();
	        private Set<TagNode> pruneNodeSet = new HashSet<TagNode>();
	    }

	    private CleanerProperties properties;

	    private ITagInfoProvider tagInfoProvider;

//	    private CleanerTransformations transformations = null;

	    /**
	     * Constructor - creates cleaner instance with default tag info provider and default properties.
	     */
	    public HtmlCleaner() {
	        this(null, null);
	    }

	    /**
	     * Constructor - creates the instance with specified tag info provider and default properties
	     * @param tagInfoProvider Provider for tag filtering and balancing
	     */
	    public HtmlCleaner(ITagInfoProvider tagInfoProvider) {
	        this(tagInfoProvider, null);
	    }

	    /**
	     * Constructor - creates the instance with default tag info provider and specified properties
	     * @param properties Properties used during parsing and serializing
	     */
	    public HtmlCleaner(CleanerProperties properties) {
	        this(null, properties);
	    }

	    /**
		 * Constructor - creates the instance with specified tag info provider and specified properties
		 * @param tagInfoProvider Provider for tag filtering and balancing
		 * @param properties Properties used during parsing and serializing
		 */
		public HtmlCleaner(ITagInfoProvider tagInfoProvider, CleanerProperties properties) {
			if(tagInfoProvider == null) {
				DefaultTagProvider provider = new DefaultTagProvider();
				provider.setInstance(provider);
				this.tagInfoProvider = provider;
			} else {
				this.tagInfoProvider = tagInfoProvider;
			}
//	        this.tagInfoProvider = tagInfoProvider == null ? DefaultTagProvider.getInstance() : tagInfoProvider;
	        this.properties = properties == null ? new CleanerProperties() : properties;
	        this.properties.tagInfoProvider = this.tagInfoProvider;
	    }

	    public TagNode clean(String htmlContent) throws InterruptedException {
	        try {
	            return clean( new StringReader(htmlContent) );
	        } catch (IOException e) {
	            // should never happen because reading from StringReader
	            throw new PostEnmlException(e);
	        }
	    }
	    public TagNode clean(Reader reader) throws IOException, InterruptedException {
	        return clean(reader, new CleanTimeValues());
	    }

	    /**
	     * Basic version of the cleaning call.
	     * @param reader
	     * @return An instance of TagNode object which is the root of the XML tree.
	     * @throws IOException
	     * @throws InterruptedException
	     */
	    public TagNode clean(Reader reader, final CleanTimeValues cleanTimeValues) throws IOException, InterruptedException {
	        cleanTimeValues._openTags = new OpenTags();
	        cleanTimeValues._headOpened = false;
	        cleanTimeValues._bodyOpened = false;
	        cleanTimeValues._headTags.clear();
	        cleanTimeValues.allTags.clear();
	        setPruneTags(properties.pruneTags, cleanTimeValues);

	        cleanTimeValues.htmlNode = createTagNode("html", cleanTimeValues);
	        cleanTimeValues.bodyNode = createTagNode("body", cleanTimeValues);
	        cleanTimeValues.headNode = createTagNode("head", cleanTimeValues);
	        cleanTimeValues.rootNode = null;
	        cleanTimeValues.htmlNode.addChild(cleanTimeValues.headNode);
	        cleanTimeValues.htmlNode.addChild(cleanTimeValues.bodyNode);

//	        HtmlTokenizer htmlTokenizer = new HtmlTokenizer(reader, properties, transformations, tagInfoProvider) {
	        HtmlTokenizer htmlTokenizer = new HtmlTokenizer(reader, properties, tagInfoProvider) {
	            @Override
	            void makeTree(List<BaseToken> tokenList) throws InterruptedException {
	                HtmlCleaner.this.makeTree( tokenList, tokenList.listIterator(tokenList.size() - 1), cleanTimeValues );
	            }

	            @Override
	            TagNode createTagNode(String name) {
	                return HtmlCleaner.this.createTagNode(name, cleanTimeValues);
	            }
	        };

			htmlTokenizer.start();

	        List<BaseToken> nodeList = htmlTokenizer.getTokenList();
	        closeAll(nodeList, cleanTimeValues);
	        createDocumentNodes(nodeList, cleanTimeValues);

	        calculateRootNode(cleanTimeValues);

	        // if there are some nodes to prune from tree
	        if ( cleanTimeValues.pruneNodeSet != null && !cleanTimeValues.pruneNodeSet.isEmpty() ) {
	            Iterator iterator = cleanTimeValues.pruneNodeSet.iterator();
	            while (iterator.hasNext()) {
	                TagNode tagNode = (TagNode) iterator.next();
	                TagNode parent = tagNode.getParent();
	                if (parent != null) {
	                    parent.removeChild(tagNode);
	                }
	            }
	            diffsleep(DEFAULT_SLEEP_TIME);
	        }

//	        cleanTimeValues.rootNode.setDocType( htmlTokenizer.getDocType() );

	        return cleanTimeValues.rootNode;
	    }

	    private TagNode createTagNode(String name, CleanTimeValues cleanTimeValues) {
	        TagNode node = new TagNode(name);
	        if ( cleanTimeValues.pruneTagSet != null && name != null && cleanTimeValues.pruneTagSet.contains(name.toLowerCase()) ) {
	            cleanTimeValues.pruneNodeSet.add(node);
	        }
	        return node;
	    }

	    private TagNode makeTagNodeCopy(TagNode tagNode, CleanTimeValues cleanTimeValues) {
	        TagNode copy = tagNode.makeCopy();
	        if ( cleanTimeValues.pruneTagSet != null && cleanTimeValues.pruneTagSet.contains(tagNode.getName()) ) {
	            cleanTimeValues.pruneNodeSet.add(copy);
	        }
	        return copy;
	    }

	    /**
	     * Assigns root node to internal variable.
	     * Root node of the result depends on parameter "omitHtmlEnvelope".
	     * If it is set, then first child of the body will be root node,
	     * or html will be root node otherwise.
	     */
	    private void calculateRootNode(CleanTimeValues cleanTimeValues) {
	        cleanTimeValues.rootNode =  cleanTimeValues.htmlNode;

	        if (properties.omitHtmlEnvelope) {
	            List bodyChildren = cleanTimeValues.bodyNode.getChildren();
	            if (bodyChildren != null) {
	                for (Object child: bodyChildren) {
	                    // if found child that is tag itself, then return it
	                    if (child instanceof TagNode) {
	                        cleanTimeValues.rootNode = (TagNode)child;
	                        break;
	                    }
	                }
	            }
	        }
	    }

	    /**
	     * Add attributes from specified map to the specified tag.
	     * If some attribute already exist it is preserved.
	     * @param tag
	     * @param attributes
	     */
		private void addAttributesToTag(TagNode tag, Map attributes) {
			if (attributes != null) {
				Map tagAttributes = tag.getAttributes();
				Iterator it = attributes.entrySet().iterator();
				while (it.hasNext()) {
					Map.Entry currEntry = (Map.Entry) it.next();
					String attName = (String) currEntry.getKey();
					if ( !tagAttributes.containsKey(attName) ) {
						String attValue = (String) currEntry.getValue();
						tag.setAttribute(attName, attValue);
					}
				}
			}
		}

	    /**
	     * Checks if open fatal tag is missing if there is a fatal tag for
	     * the specified tag.
	     * @param tag
	     */
	    private boolean isFatalTagSatisfied(TagInfo tag, CleanTimeValues cleanTimeValues) {
	    	if (tag != null) {
	            String fatalTagName = tag.getFatalTag();
	            return fatalTagName == null ? true : cleanTimeValues._openTags.tagExists(fatalTagName);
	    	}

	    	return true;
	    }

	    /**
	     * Check if specified tag requires parent tag, but that parent
	     * tag is missing in the appropriate context.
	     * @param tag
	     */
	    private boolean mustAddRequiredParent(TagInfo tag, CleanTimeValues cleanTimeValues) {
	    	if (tag != null) {
	    		String requiredParent = tag.getRequiredParent();
	    		if (requiredParent != null) {
		    		String fatalTag = tag.getFatalTag();
	                int fatalTagPositon = -1;
	                if (fatalTag != null) {
	                    TagPos tagPos = cleanTimeValues._openTags.findTag(fatalTag);
	                    if (tagPos != null) {
	                        fatalTagPositon = tagPos.position;
	                    }
	                }

		    		// iterates through the list of open tags from the end and check if there is some higher
		    		ListIterator<TagPos> it = cleanTimeValues._openTags.list.listIterator( cleanTimeValues._openTags.list.size() );
		            while ( it.hasPrevious() ) {
		            	TagPos currTagPos = it.previous();
		            	if (tag.isHigher(currTagPos.name)) {
		            		return currTagPos.position <= fatalTagPositon;
		            	}
		            }

		            return true;
	    		}
	    	}

	    	return false;
	    }

	    private TagNode createTagNode(TagNode startTagToken) {
	    	startTagToken.setFormed();
	    	return startTagToken;
	    }

	    private boolean isAllowedInLastOpenTag(BaseToken token, CleanTimeValues cleanTimeValues) {
	        TagPos last = cleanTimeValues._openTags.getLastTagPos();
	        if (last != null) {
				 if (last.info != null) {
	                 return last.info.allowsItem(token);
				 }
			}

			return true;
	    }

	    private void saveToLastOpenTag(List nodeList, BaseToken tokenToAdd, CleanTimeValues cleanTimeValues) {
	        TagPos last = cleanTimeValues._openTags.getLastTagPos();
	        if ( last != null && last.info != null && last.info.isIgnorePermitted() ) {
	            return;
	        }

	        TagPos rubbishPos = cleanTimeValues._openTags.findTagToPlaceRubbish();
	        if (rubbishPos != null) {
	    		TagNode startTagToken = (TagNode) nodeList.get(rubbishPos.position);
	            startTagToken.addItemForMoving(tokenToAdd);
	        }
	    }

	    private boolean isStartToken(Object o) {
	    	return (o instanceof TagNode) && !((TagNode)o).isFormed();
	    }

		void makeTree(List<BaseToken> nodeList, ListIterator<BaseToken> nodeIterator, CleanTimeValues cleanTimeValues) throws InterruptedException {
			// process while not reach the end of the list
			while ( nodeIterator.hasNext() ) {
				BaseToken token = nodeIterator.next();

	            if (token instanceof EndTagToken) {
					EndTagToken endTagToken = (EndTagToken) token;
					String tagName = endTagToken.getName();
					TagInfo tag = tagInfoProvider.getTagInfo(tagName);

					if ( (tag == null && properties.omitUnknownTags) || (tag != null && tag.isDeprecated() && properties.omitDeprecatedTags) ) {
						nodeIterator.set(null);
					} else if ( tag != null && !tag.allowsBody() ) {
						nodeIterator.set(null);
					} else {
						TagPos matchingPosition = cleanTimeValues._openTags.findTag(tagName);

	                    if (matchingPosition != null) {
	                        List closed = closeSnippet(nodeList, matchingPosition, endTagToken, cleanTimeValues);
	                        nodeIterator.set(null);
	                        for (int i = closed.size() - 1; i >= 1; i--) {
	                            TagNode closedTag = (TagNode) closed.get(i);
	                            if ( tag != null && tag.isContinueAfter(closedTag.getName()) ) {
	                                nodeIterator.add( makeTagNodeCopy(closedTag, cleanTimeValues) );
	                                nodeIterator.previous();
	                            }
	                        }
	                    } else if ( !isAllowedInLastOpenTag(token, cleanTimeValues) ) {
	                        saveToLastOpenTag(nodeList, token, cleanTimeValues);
	                        nodeIterator.set(null);
	                    }
	                }
				} else if ( isStartToken(token) ) {
	                TagNode startTagToken = (TagNode) token;
					String tagName = startTagToken.getName();
					TagInfo tag = tagInfoProvider.getTagInfo(tagName);

	                TagPos lastTagPos = cleanTimeValues._openTags.isEmpty() ? null : cleanTimeValues._openTags.getLastTagPos();
	                TagInfo lastTagInfo = lastTagPos == null ? null : tagInfoProvider.getTagInfo(lastTagPos.name);

	                // add tag to set of all tags
					cleanTimeValues.allTags.add(tagName);

	                // HTML open tag
	                if ( "html".equals(tagName) ) {
						addAttributesToTag(cleanTimeValues.htmlNode, startTagToken.getAttributes());
						nodeIterator.set(null);
	                // BODY open tag
	                } else if ( "body".equals(tagName) ) {
	                    cleanTimeValues._bodyOpened = true;
	                    addAttributesToTag(cleanTimeValues.bodyNode, startTagToken.getAttributes());
						nodeIterator.set(null);
	                // HEAD open tag
	                } else if ( "head".equals(tagName) ) {
	                    cleanTimeValues._headOpened = true;
	                    addAttributesToTag(cleanTimeValues.headNode, startTagToken.getAttributes());
						nodeIterator.set(null);
	                // unknown HTML tag and unknown tags are not allowed
	                } else if ( (tag == null && properties.omitUnknownTags) || (tag != null && tag.isDeprecated() && properties.omitDeprecatedTags) ) {
	                    nodeIterator.set(null);
	                // if current tag is unknown, unknown tags are allowed and last open tag doesn't allow any other tags in its body
	                } else if ( tag == null && lastTagInfo != null && !lastTagInfo.allowsAnything() ) {
	                    saveToLastOpenTag(nodeList, token, cleanTimeValues);
	                    nodeIterator.set(null);
	                } else if ( tag != null && tag.hasPermittedTags() && cleanTimeValues._openTags.someAlreadyOpen(tag.getPermittedTags()) ) {
	                	nodeIterator.set(null);
	                // if tag that must be unique, ignore this occurence
	                } else if ( tag != null && tag.isUnique() && cleanTimeValues._openTags.tagEncountered(tagName) ) {
	                	nodeIterator.set(null);
	                // if there is no required outer tag without that this open tag is ignored
	                } else if ( !isFatalTagSatisfied(tag, cleanTimeValues) ) {
						nodeIterator.set(null);
	                // if there is no required parent tag - it must be added before this open tag
	                } else if ( mustAddRequiredParent(tag, cleanTimeValues) ) {
						String requiredParent = tag.getRequiredParent();
						TagNode requiredParentStartToken = createTagNode(requiredParent, cleanTimeValues);
						nodeIterator.previous();
						nodeIterator.add(requiredParentStartToken);
						nodeIterator.previous();
	                // if last open tag has lower presidence then this, it must be closed
	                } else if ( tag != null && lastTagPos != null && tag.isMustCloseTag(lastTagInfo) ) {
						List closed = closeSnippet(nodeList, lastTagPos, startTagToken, cleanTimeValues);
						int closedCount = closed.size();

						// it is needed to copy some tags again in front of current, if there are any
						if ( tag.hasCopyTags() && closedCount > 0 ) {
							// first iterates over list from the back and collects all start tokens
							// in sequence that must be copied
							ListIterator closedIt = closed.listIterator(closedCount);
							List toBeCopied = new ArrayList();
							while (closedIt.hasPrevious()) {
								TagNode currStartToken = (TagNode) closedIt.previous();
								if ( tag.isCopy(currStartToken.getName()) ) {
									toBeCopied.add(0, currStartToken);
								} else {
									break;
								}
							}

							if (toBeCopied.size() > 0) {
								Iterator copyIt = toBeCopied.iterator();
								while (copyIt.hasNext()) {
									TagNode currStartToken = (TagNode) copyIt.next();
									nodeIterator.add( makeTagNodeCopy(currStartToken, cleanTimeValues) );
								}

	                            // back to the previous place, before adding new start tokens
								for (int i = 0; i < toBeCopied.size(); i++) {
									nodeIterator.previous();
								}
	                        }
						}

	                    nodeIterator.previous();
					// if this open tag is not allowed inside last open tag, then it must be moved to the place where it can be
	                } else if ( !isAllowedInLastOpenTag(token, cleanTimeValues) ) {
	                    saveToLastOpenTag(nodeList, token, cleanTimeValues);
	                    nodeIterator.set(null);
					// if it is known HTML tag but doesn't allow body, it is immediately closed
	                } else if ( tag != null && !tag.allowsBody() ) {
						TagNode newTagNode = createTagNode(startTagToken);
	                    addPossibleHeadCandidate(tag, newTagNode, cleanTimeValues);
	                    nodeIterator.set(newTagNode);
					// default case - just remember this open tag and go further
	                } else {
	                    cleanTimeValues._openTags.addTag( tagName, nodeIterator.previousIndex() );
	                }
				} else {
					if ( !isAllowedInLastOpenTag(token, cleanTimeValues) ) {
	                    saveToLastOpenTag(nodeList, token, cleanTimeValues);
	                    nodeIterator.set(null);
					}
				}
	            diffsleep(DEFAULT_SLEEP_TIME);
			}
	    }

		private void createDocumentNodes(List listNodes, CleanTimeValues cleanTimeValues) throws InterruptedException {
			Iterator it = listNodes.iterator();
	        while (it.hasNext()) {
	            Object child = it.next();

	            if (child == null) {
	            	continue;
	            }

				boolean toAdd = true;

	            if (child instanceof TagNode) {
	                TagNode node = (TagNode) child;
	                TagInfo tag = tagInfoProvider.getTagInfo( node.getName() );
	                addPossibleHeadCandidate(tag, node, cleanTimeValues);
				} else {
					if (child instanceof ContentNode) {
						toAdd = !"".equals(child.toString());
					}
				}

				if (toAdd) {
					cleanTimeValues.bodyNode.addChild(child);
				}
	            diffsleep(DEFAULT_SLEEP_TIME);
	        }

	        // move all viable head candidates to head section of the tree
	        Iterator headIterator = cleanTimeValues._headTags.iterator();
	        while (headIterator.hasNext()) {
	            TagNode headCandidateNode = (TagNode) headIterator.next();

	            // check if this node is already inside a candidate for moving to head
	            TagNode parent = headCandidateNode.getParent();
	            boolean toMove = true;
	            while (parent != null) {
	                if ( cleanTimeValues._headTags.contains(parent) ) {
	                    toMove = false;
	                    break;
	                }
	                parent = parent.getParent();
	            }

	            if (toMove) {
	                headCandidateNode.removeFromTree();
	                cleanTimeValues.headNode.addChild(headCandidateNode);
	            }
	            diffsleep(DEFAULT_SLEEP_TIME);
	        }
	    }

		private List closeSnippet(List nodeList, TagPos tagPos, Object toNode, CleanTimeValues cleanTimeValues) throws InterruptedException {
			List closed = new ArrayList();
			ListIterator it = nodeList.listIterator(tagPos.position);

			TagNode tagNode = null;
			Object item = it.next();
			boolean isListEnd = false;

			while ( (toNode == null && !isListEnd) || (toNode != null && item != toNode) ) {
				if ( isStartToken(item) ) {
	                TagNode startTagToken = (TagNode) item;
	                closed.add(startTagToken);
	                List<BaseToken> itemsToMove = startTagToken.getItemsToMove();
	                if (itemsToMove != null) {
	            		OpenTags prevOpenTags = cleanTimeValues._openTags;
	            		cleanTimeValues._openTags = new OpenTags();
	            		makeTree(itemsToMove, itemsToMove.listIterator(0), cleanTimeValues);
	                    closeAll(itemsToMove, cleanTimeValues);
	                    startTagToken.setItemsToMove(null);
	                    cleanTimeValues._openTags = prevOpenTags;
	                }

	                TagNode newTagNode = createTagNode(startTagToken);
	                TagInfo tag = tagInfoProvider.getTagInfo( newTagNode.getName() );
	                addPossibleHeadCandidate(tag, newTagNode, cleanTimeValues);
	                if (tagNode != null) {
						tagNode.addChildren(itemsToMove);
	                    tagNode.addChild(newTagNode);
	                    it.set(null);
	                } else {
	                	if (itemsToMove != null) {
	                		itemsToMove.add(newTagNode);
	                		it.set(itemsToMove);
	                	} else {
	                		it.set(newTagNode);
	                	}
	                }

	                cleanTimeValues._openTags.removeTag( newTagNode.getName() );
	                tagNode = newTagNode;
	            } else {
	            	if (tagNode != null) {
	            		it.set(null);
	            		if (item != null) {
	            			tagNode.addChild(item);
	                    }
	                }
	            }

				if ( it.hasNext() ) {
					item = it.next();
				} else {
					isListEnd = true;
				}
			}

			return closed;
	    }

	    /**
	     * Close all unclosed tags if there are any.
	     * @throws InterruptedException
	     */
	    private void closeAll(List<BaseToken> nodeList, CleanTimeValues cleanTimeValues) throws InterruptedException {
	        TagPos firstTagPos = cleanTimeValues._openTags.findFirstTagPos();
	        if (firstTagPos != null) {
	            diffsleep(DEFAULT_SLEEP_TIME);
	            closeSnippet(nodeList, firstTagPos, null, cleanTimeValues);
	        }
	    }

	    /**
	     * Checks if specified tag with specified info is candidate for moving to head section.
	     * @param tagInfo
	     * @param tagNode
	     */
	    private void addPossibleHeadCandidate(TagInfo tagInfo, TagNode tagNode, CleanTimeValues cleanTimeValues) {
	        if (tagInfo != null && tagNode != null) {
	            if ( tagInfo.isHeadTag() || (tagInfo.isHeadAndBodyTag() && cleanTimeValues._headOpened && !cleanTimeValues._bodyOpened) ) {
	                cleanTimeValues._headTags.add(tagNode);
	            }
	        }
	    }

	    public CleanerProperties getProperties() {
	        return properties;
	    }

	    private void setPruneTags(String pruneTags, CleanTimeValues cleanTimeValues) {
	        cleanTimeValues.pruneTagSet.clear();
	        cleanTimeValues.pruneNodeSet.clear();
	        if (pruneTags != null) {
	            StringTokenizer tokenizer = new StringTokenizer(pruneTags, ",");
	            while ( tokenizer.hasMoreTokens() ) {
	                cleanTimeValues.pruneTagSet.add( tokenizer.nextToken().trim().toLowerCase() );
	            }
	        }
	    }

	    /**
	     * @return ITagInfoProvider instance for this HtmlCleaner
	     */
	    public ITagInfoProvider getTagInfoProvider() {
	        return tagInfoProvider;
	    }
	}
}
