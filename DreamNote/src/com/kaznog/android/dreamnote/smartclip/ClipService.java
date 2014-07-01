package com.kaznog.android.dreamnote.smartclip;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.db.DreamNoteProvider;
import com.kaznog.android.dreamnote.db.schema.ItemsSchema;
import com.kaznog.android.dreamnote.fragment.Notes;
import com.kaznog.android.dreamnote.util.AppInfo;
import com.kaznog.android.dreamnote.util.CharsetDetector;
//import com.kaznog.android.dreamnote.util.Constant;
import com.kaznog.android.dreamnote.util.StringUtils;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteCallbackList;
import android.os.RemoteException;
import android.widget.RemoteViews;

public class ClipService extends Service {
	private final RemoteCallbackList<ClipServiceCallbackInterface> callbacklist
	= new RemoteCallbackList<ClipServiceCallbackInterface>();
	private Timer updatetimer = new Timer();
	private final static int NOTIFY_SLEEP_TIME = 200;
	private final static int DEFAULT_SLEEP_TIME = 200;
	private long oldtime = 0;
	private long currenttime = 0;

	private ArrayList<Notification> NotifyList = null;
    private ArrayListClipQueueItem ClipQueueList = null;
    private ArrayListClipWebcache arrcache = null;
    private int cachecnt;
	private HashMap<String, String> contentfiles;
	private int clipfilecount = 0;
	private String userAgent;
	private String preview_content;
	private String clipfilepath;
	private ClipThread thread = null;
	@Override
	public void onCreate() {
		super.onCreate();
		ClipQueueList = new ArrayListClipQueueItem();
		NotifyList = new ArrayList<Notification>();
		updatetimer.schedule(task, 0, 1000);
	}
	@Override
	public IBinder onBind(Intent intent) {
		if(ClipServiceInterface.class.getName().equals(intent.getAction())) {
			return interfaceImpl_;
		}
		return null;
	}
	@Override
	public void onStart(Intent intent, int StartId) {
		super.onStart(intent, StartId);
		ClipQueueItem cq = null;
		try {
			cq = (ClipQueueItem) intent.getSerializableExtra("cq");
		} catch(NullPointerException ne) {
			cq = null;
		} catch(ClassCastException cce) {
			cq = null;
		}
		if(cq == null) {
			return;
		}
		int cqindex = ClipQueueList.size();

		AppInfo.DebugLog(this, "ClipService: onStart");
		AppInfo.DebugLog(this, "ClipService: onStart QueueIndex " + cqindex);

		ClipQueueList.add(cq);
		showNotification(cq.getQueueindex(), cq.getTitle());
//		showNotification(R.string.app_name, cq.getTitle());
		if(ClipQueueList.size() > 1) {return;}
		thread = new ClipThread(this);
		thread.start();
	}
	@Override
	public void onDestroy() {
		AppInfo.DebugLog(getApplicationContext(), "ClipService onDestroy!!");
		Iterator<ClipQueueItem> iter = ClipQueueList.iterator();
		while(iter.hasNext()) {
			ClipQueueItem cq = iter.next();
			cq.clear();
		}
		ClipQueueList.clear();
		ClipQueueList = null;
		callbacklist.kill();
		updatetimer.cancel();
		updatetimer = null;
		if(thread != null) {
		}
		super.onDestroy();
	}
	private TimerTask task = new TimerTask() {
		@Override
		public void run() {
			int n = callbacklist.beginBroadcast();
			for(int i = 0; i < n; i++) {
				Object objindex = callbacklist.getBroadcastCookie(i);
				int index = new Integer(objindex.toString()).intValue();
				try {
					int progress = ClipQueueList.get(index).getProgress();
					try {
						callbacklist.getBroadcastItem(i).update(progress);
					} catch (RemoteException e) {
						e.printStackTrace();
					}
				} catch(IndexOutOfBoundsException e) {

				}
			}
			callbacklist.finishBroadcast();
		}
	};
	private class ShowNotificationThread implements Runnable {
		private Context mContext = null;
		private int notifyIndex;
		private int mProgress;
		private long mDbIndex;
		public ShowNotificationThread(Context context, int index, int progress, long dbindex) {
			mContext = context;
			notifyIndex = index;
			mProgress = progress;
			mDbIndex = dbindex;
		}
		public void run() {
	        NotificationManager nm = (NotificationManager)mContext.getSystemService(Context.NOTIFICATION_SERVICE);
			Notification notify = NotifyList.get(notifyIndex);
			if(mProgress == -1) {
				notify.contentView.setTextViewText(R.id.clip_notify_title, mContext.getResources().getText(R.string.clip_service_failed_title));
				notify.flags = Notification.FLAG_AUTO_CANCEL;
				notify.tickerText = mContext.getResources().getText(R.string.clip_service_failed_title);
				notify.when = System.currentTimeMillis();
				nm.notify(notifyIndex, notify);
			} else if(mProgress == -2) {
				notify.contentView.setTextViewText(R.id.clip_notify_title, mContext.getResources().getText(R.string.clip_service_cancel_title));
				notify.flags = Notification.FLAG_AUTO_CANCEL;
				notify.tickerText = mContext.getResources().getText(R.string.clip_service_cancel_title);
				notify.when = System.currentTimeMillis();
				nm.notify(notifyIndex, notify);
			} else if(mProgress == 100) {
				Intent intent = new Intent(mContext, Notes.class);
				intent.putExtra("itemid", String.valueOf(mDbIndex));
		        PendingIntent pendingintent = PendingIntent.getActivity(mContext, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
		        notify.contentIntent = pendingintent;
//		        notify.icon = android.R.drawable.stat_sys_download_done;
		        notify.icon = R.drawable.notify_finished;
				notify.tickerText = mContext.getResources().getText(R.string.clip_service_complete_title);
				notify.when = System.currentTimeMillis();
//				notify.flags = notify.flags ^ (Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT);
				notify.flags = Notification.FLAG_AUTO_CANCEL;
				notify.contentView.setTextViewText(R.id.clip_notify_title, getResources().getText(R.string.clip_service_complete_title));
				notify.contentView.setProgressBar(R.id.clip_notify_progressBar, 100, 100, false);
//				notify.contentView.setImageViewResource(R.id.clip_notify_icon, android.R.drawable.stat_sys_download_done);
				notify.contentView.setImageViewResource(R.id.clip_notify_icon, R.drawable.notify_finished);
				nm.notify(notifyIndex, notify);
//				nm.cancel(notifyIndex);
			} else {
				notify.contentView.setProgressBar(R.id.clip_notify_progressBar, 100, mProgress, false);
				nm.notify(notifyIndex, notify);
			}
		}
	}
	//ノティフィケーションの表示
    private void showNotification(int queueIndex, String webtitle) {
        //ノティフィケーションオブジェクトの生成
    	String title = getResources().getString(R.string.clip_service_start_title);
//        Notification notification = new Notification(android.R.drawable.stat_sys_download, ticker, System.currentTimeMillis());
        Notification notification = new Notification(R.drawable.notify_download, title, System.currentTimeMillis());
        RemoteViews contentview = new RemoteViews(this.getPackageName(), R.layout.clip_notification_layout);
        contentview.setProgressBar(R.id.clip_notify_progressBar, 100, 0, true);
        contentview.setTextViewText(R.id.clip_notify_title, title);
        contentview.setTextViewText(R.id.clip_notify_text, webtitle);
//        contentview.setImageViewResource(R.id.clip_notify_icon, android.R.drawable.stat_sys_download);
        contentview.setImageViewResource(R.id.clip_notify_icon, R.drawable.notify_download);
        notification.contentView = contentview;
//        notification.flags = notification.flags | Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;
        Intent intent = new Intent(this, com.kaznog.android.dreamnote.smartclip.ClipServiceNotifyActivity.class);
        intent.putExtra("notifyID", queueIndex);
        intent.putExtra("webtitle", webtitle);
        PendingIntent pendingintent = PendingIntent.getActivity(this, 0, intent, Intent.FLAG_ACTIVITY_NEW_TASK);
        notification.contentIntent = pendingintent;

        //ノティフィケーションマネージャの取得
        NotificationManager nm = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        //ノティフィケーションの表示
        nm.notify(queueIndex, notification);

        NotifyList.add(notification);
    }
	// キャンセル処理
	private void cancel(int index) {
		if(ClipQueueList.size() < 1) return;
		ClipQueueItem cq = ClipQueueList.get(index);
		cq.setProgress(-2);
		ClipQueueList.set(index, cq);
	}
	private ClipServiceInterface.Stub interfaceImpl_ = new ClipServiceInterface.Stub() {
		public void registerCallback(ClipServiceCallbackInterface callback, int index) throws RemoteException {
			Object objindex = new Integer(index);
			callbacklist.register(callback, objindex);
		}
		public void unregisterCallback(ClipServiceCallbackInterface callback) throws RemoteException {
			callbacklist.unregister(callback);
		}
		public void cancel(int index) throws RemoteException {
			ClipService.this.cancel(index);
		}
	};
	private class ClipThread extends Thread {
		private Context mContext = null;
		private Handler mHandler = null;
		public ClipThread(Context context) {
			mContext = context;
			mHandler = new Handler();
		}
		private void diffsleep(long sleeptime) throws InterruptedException {
			currenttime = System.currentTimeMillis();
			long diff = currenttime - oldtime;
			if(diff > 3000) {
				oldtime = currenttime;
				Thread.sleep(sleeptime);
			}
		}

		private void getContent(String url, ClipQueueItem cq) throws Exception {
			try {
				Matcher m;
				String domain = url.replaceFirst("(?<!\\/)\\/(?!\\/).*$", "");
				String dir = url.replaceFirst("\\/[^\\/]*$", "");
				StringBuffer sb = new StringBuffer();
				m = Pattern.compile("(href=|src=|@import\\s*|file=)(['\"])(.+?)(?:\\2)", Pattern.CASE_INSENSITIVE).matcher(preview_content);
				while(m.find()) {

					String path = m.group(3);
					if(path.indexOf("/") == 0) {
						path = domain + path;
					} else if(path.indexOf("./") == 0) {
						path = dir + "/" + path;
					} else if(path.indexOf("../") == 0) {
						//path = dir + "/" + path;
						path = getPath(dir, path);
					} else {
						path = path.trim();
						if(path.toLowerCase().startsWith("http") == false
						&& path.toLowerCase().startsWith("data:") == false
						&& path.toLowerCase().startsWith("javascript:") == false
						&& path.toLowerCase().startsWith("tel:") == false
						&& path.toLowerCase().startsWith("mailto:") == false) {
							Matcher dm = Pattern.compile("^\\w+:\\/{2,}", Pattern.CASE_INSENSITIVE).matcher(path);
							int mc = 0;
							while(dm.find()) {
								mc++;
							}
							if(mc != 1) {
								path = dir + "/" + path;
							}
						}
					}
					Matcher m64 = Pattern.compile("(data:image\\/.*?base64|google.com\\/.*\\/data=)", Pattern.CASE_INSENSITIVE).matcher(path);
					if(m64.find() == false
					&& path.toLowerCase().startsWith("javascript:") == false
					&& path.toLowerCase().startsWith("tel:") == false
					&& path.toLowerCase().startsWith("mailto:") == false) {
						// フルドメインファイルパスになっているはずなので、
						// ファイルを取得
						String contentname = contentfiles.get(path);
						if(contentname == null) {
							int tmpcnt = ++clipfilecount;
							contentname = this.getContentName(tmpcnt, path);
							ClipWebcache cwc = arrcache.getCache(path);
							if(cwc != null) {
								contentfiles.put(path, contentname);
								convertClip(path, cq);
								cachecnt++;
								float progress = ((40.0f/arrcache.size()) * cachecnt);
								AppInfo.DebugLog(mContext, "getcontent arrcachesize: " + arrcache.size());
								AppInfo.DebugLog(mContext, "getcontent cachecnt: " + cachecnt);
						        clipsleep((int)progress, -1, cq);

								m.appendReplacement(sb, m.group(1) + m.group(2) + contentname + m.group(2));
							} else {
								m.appendReplacement(sb, m.group(1) + m.group(2) + path + m.group(2));
							}
						} else {
							m.appendReplacement(sb, m.group(1) + m.group(2) + contentname + m.group(2));
						}
					}
				}
		        clipsleep(41, -1, cq);
				m.appendTail(sb);
				preview_content = sb.toString();
				sb.setLength(0);
				m = Pattern.compile("(<?xml\\s+.+?encoding=\")([^'\">]+)", Pattern.CASE_INSENSITIVE).matcher(preview_content);
				if(m.find()) {
					preview_content = m.replaceAll("$1UTF-8");
				}
		        clipsleep(42, -1, cq);
				m = Pattern.compile("(<meta\\s+.+?charset=\")([^'\">]+)", Pattern.CASE_INSENSITIVE).matcher(preview_content);
				if(m.find()) {
					preview_content = m.replaceAll("$1UTF-8");
				} else {
			        clipsleep(43, -1, cq);
					m = Pattern.compile("(<meta\\scharset=\")([^'\">]+)", Pattern.CASE_INSENSITIVE).matcher(preview_content);
					if(m.find()) {
						preview_content = m.replaceAll("$1UTF-8");
					} else {
				        clipsleep(44, -1, cq);
						m = Pattern.compile("(<meta\\s+.+?charset=)([^\">]+)", Pattern.CASE_INSENSITIVE).matcher(preview_content);
						if(m.find()) {
							preview_content = m.replaceAll("$1UTF-8");
						}
					}
				}
		        clipsleep(45, -1, cq);
				m = Pattern.compile("(<script\\s+.+?charset=\")([^'\">]+)", Pattern.CASE_INSENSITIVE).matcher(preview_content);
				if(m.find()) {
					preview_content = m.replaceAll("$1utf-8");
				}
		        clipsleep(46, -1, cq);
			} catch(InterruptedException e) {
				throw new Exception();
			}
		}
		// CSSは1つにまとめないと表示できないようなので1つにまとめる
		private String clipCSS(String url) throws Exception {
			String content = null;
			try {
				Matcher m;
				String cnvurl = url;
				String domain = cnvurl.replaceFirst("(?<!\\/)\\/(?!\\/).*$", "");
				String dir = cnvurl.replaceFirst("\\/[^\\/]*$", "");
				ClipWebcache cwc = arrcache.getCache(url);
				if(cwc == null) { return ""; }
				String encoding = cwc.getEncoding();
				File cachefile = new File(clipfilepath + "/.webcache/" + cwc.getFilepath());
				content = readStringFile(cachefile, encoding);
				StringBuffer sb = new StringBuffer();
				m = Pattern.compile("(@import\\s*)(['\"])(.+?)(?:\\2)", Pattern.CASE_INSENSITIVE).matcher(content);
				while(m.find()) {

					String path = m.group(3);
					if(path.indexOf("/") == 0) {
						path = domain + path;
					} else if(path.indexOf("./") == 0) {
						path = dir + "/" + path;
					} else if(path.indexOf("../") == 0) {
						//path = dir + "/" + path;
						path = getPath(dir, path);
					} else {
						path = path.trim();
						if(path.startsWith("http") == false) {
							Matcher dm = Pattern.compile("^\\w+:\\/{2,}", Pattern.CASE_INSENSITIVE).matcher(path);
							int mc = 0;
							while(dm.find()) {
								mc++;
							}
							if(mc != 1) {
								path = dir + "/" + path;
							}
						}
					}
					// cssの場合は1つにまとめる
					if(path.lastIndexOf(".css") == path.length() - 4) {
						String csscontent = clipCSS(path);
						m.appendReplacement(sb, csscontent);
					}
				}
				m.appendTail(sb);
				content = sb.toString();
				sb.setLength(0);
			} catch(InterruptedException e) {
				throw new Exception();
			}
			return content;
		}
		private void convertClip(String url, ClipQueueItem cq) throws Exception {
			Matcher m;
			String filename;
			// 保存ファイル名生成
			filename = this.getContentName(clipfilecount, url);
			// 取得ファイル拡張子取得
			String ext = filename.substring(filename.lastIndexOf(".") + 1);
			ext = ext.toLowerCase();
			String content = null;
			String domain = url.replaceFirst("(?<!\\/)\\/(?!\\/).*$", "");
			String dir = url.replaceFirst("\\/[^\\/]*$", "");
			AppInfo.DebugLog(mContext, "convertClip url: " + url);
			AppInfo.DebugLog(mContext, "convertClip domain: " + domain);
			AppInfo.DebugLog(mContext, "convertClip dir: " + dir);
			ClipWebcache cwc = arrcache.getCache(url);
			if(cwc == null) { return; }
			File cachefile = new File(clipfilepath + "/.webcache/" + cwc.getFilepath());
			String mimetype = cwc.getMimetype();
			String encoding = cwc.getEncoding();
			if(mimetype.equals("text/css")) {
				content = clipCSS(url);
			} else if(mimetype.equals("text/javascript") || mimetype.equals("text/html")) {
				content = readStringFile(cachefile, encoding);
			}
			if(mimetype.equals("text/css")
			|| mimetype.equals("text/javascript")
			|| mimetype.equals("text/html")
			) {
				StringBuffer sb = new StringBuffer();
				m = Pattern.compile("(href=|src=|@import\\s*|file=)(['\"])(.+?)(?:\\2)", Pattern.CASE_INSENSITIVE).matcher(content);
				while(m.find()) {
					String path = m.group(3);
					if(path.indexOf("/") == 0) {
						path = domain + path;
					} else if(path.indexOf("./") == 0) {
						path = dir + "/" + path;
					} else if(path.indexOf("../") == 0) {
						//path = dir + "/" + path;
						path = getPath(dir, path);
					} else {
						path = path.trim();
						if(path.startsWith("http") == false) {
							Matcher dm = Pattern.compile("^\\w+:\\/{2,}", Pattern.CASE_INSENSITIVE).matcher(path);
							int mc = 0;
							while(dm.find()) {
								mc++;
							}
							if(mc != 1) {
								path = dir + "/" + path;
							}
						}
					}
					Matcher m64 = Pattern.compile("(data:image\\/.*?base64|google.com\\/.*\\/data=)", Pattern.CASE_INSENSITIVE).matcher(path);
					if(m64.find() == false) {
						// フルドメインファイルパスになっているはずなので、
						// ファイルを取得
						String contentname = contentfiles.get(path);
						if(contentname == null) {
							int tmpcnt = ++clipfilecount;
							contentname = this.getContentName(tmpcnt, path);
							ClipWebcache nextcache = arrcache.getCache(path);
							if(nextcache != null) {
								contentfiles.put(path, contentname);
								convertClip(path, cq);
								cachecnt++;
								float progress = (40.0f/arrcache.size()) * cachecnt;
								AppInfo.DebugLog(mContext, "convertclip arrcachesize: " + arrcache.size());
								AppInfo.DebugLog(mContext, "convertclip cachecnt: " + cachecnt);
						        clipsleep((int)progress, -1, cq);

						        m.appendReplacement(sb, m.group(1) + m.group(2) + contentname + m.group(2));
							} else {
								m.appendReplacement(sb, m.group(1) + m.group(2) + path + m.group(2));
							}
						} else {
							m.appendReplacement(sb, m.group(1) + m.group(2) + contentname + m.group(2));
						}
					}
				}
				m.appendTail(sb);
				content = sb.toString();
				sb.setLength(0);
				if(mimetype.equals("text/css")) {
					m = Pattern.compile("(url)(\\()(.+?)(\\))", Pattern.CASE_INSENSITIVE).matcher(content);
					while(m.find()) {
						String path = m.group(3);
						if(path.indexOf("'") == 0 || path.indexOf("\"") == 0) {
							path = path.substring(1, path.length() - 1);
						}
						if(path.indexOf("/") == 0) {
							path = domain + path;
						} else if(path.indexOf("./") == 0) {
							path = dir + "/" + path;
						} else if(path.indexOf("../") == 0) {
							//path = dir.substring(0, dir.lastIndexOf("/")) + "/" + path.substring(3);
							path = getPath(dir, path);
						} else {
							path = path.trim();
							if(path.startsWith("http") == false) {
								Matcher dm = Pattern.compile("^\\w+:\\/{2,}", Pattern.CASE_INSENSITIVE).matcher(path);
								int mc = 0;
								while(dm.find()) {
									mc++;
								}
								if(mc != 1) {
									path = dir + "/" + path;
								}
							}
						}
				    	int index = path.indexOf(")");
				    	if(index != -1) {
				    		path = path.substring(0, index);
				    	}
				    	path = path.trim();
						Matcher m64 = Pattern.compile("(data:image\\/.*?base64|google.com\\/.*\\/data=)", Pattern.CASE_INSENSITIVE).matcher(path);
						if(m64.find() == false) {
							String contentname = contentfiles.get(path);
							if(contentname == null) {
								int tmpcnt = ++clipfilecount;
								contentname = this.getContentName(tmpcnt, path);
								ClipWebcache nextcache = arrcache.getCache(path);
								if(nextcache != null) {
									contentfiles.put(path, contentname);
									convertClip(path, cq);
									cachecnt++;
									float progress = (40.0f/arrcache.size()) * cachecnt;
							        clipsleep((int)progress, -1, cq);

							        m.appendReplacement(sb, m.group(1) + m.group(2) + contentname + ")");
								} else {
									contentfiles.put(path, contentname);
									downloadContent(path, clipfilepath + "/" + contentname);
									m.appendReplacement(sb, m.group(1) + m.group(2) + contentname + ")");
								}
							} else {
								m.appendReplacement(sb, m.group(1) + m.group(2) + contentname + ")");
							}
						}
					}
					m.appendTail(sb);
					content = sb.toString();
					sb.setLength(0);
				}
				m = Pattern.compile("(<meta\\s+.+?charset=)([^'\">]+)", Pattern.CASE_INSENSITIVE).matcher(content);
				if(m.find()) {
					content = m.replaceAll("$1UTF-8");
				}
				m = Pattern.compile("(<script\\s+.+?charset=\")([^'\">]+)", Pattern.CASE_INSENSITIVE).matcher(content);
				if(m.find()) {
					content = m.replaceAll("$1utf-8");
				}
				m = Pattern.compile("(@charset\\s*)(['\"])(.+?)(?:\\2)", Pattern.CASE_INSENSITIVE).matcher(content);
				while(m.find()) {
					m.appendReplacement(sb, m.group(1) + m.group(2) + "utf-8" + m.group(2));
				}
				m.appendTail(sb);
				content = sb.toString();
				sb.setLength(0);
				try {
					filename = clipfilepath + "/" + filename;
					if(filename.indexOf("?") != -1) {
						filename = filename.substring(0, filename.indexOf("?"));
					}
					AppInfo.DebugLog(mContext, "convertClip write file: " + filename);
					BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(filename)), "UTF-8"));
					bw.write(content);
					bw.flush();
					bw.close();
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
					throw new Exception();
				} catch (IOException e) {
					e.printStackTrace();
					throw new Exception();
				}
			} else {
				if(filename.indexOf("?") != -1) {
					filename = filename.substring(0, filename.lastIndexOf("?"));
				}
				AppInfo.DebugLog(mContext, "copyFile srcFile: " + cachefile.getPath());
				AppInfo.DebugLog(mContext, "copyFile dstFile: " + clipfilepath + "/" + filename);
				copyFile(cachefile.getPath(), clipfilepath + "/" + filename);
			}
		}
	    private void downloadContent(String url, String dstFilePath) throws Exception {
	    	boolean result = false;
	    	DefaultHttpClient dhc = new DefaultHttpClient();
	    	HttpGet httpget = null;
	    	InputStream input = null;
	    	HttpEntity entity = null;
	    	HttpResponse res = null;
	    	ByteArrayOutputStream out = null;
	    	OutputStream output = null;
	    	int index = url.indexOf("\"");
	    	String requrl = url;
	    	if(index != -1) {
		    	requrl = url.substring(0, index -1);
	    	}
	    	index = requrl.indexOf("?");
	    	if(index != -1) {
	    		requrl = requrl.substring(0, index -1);
	    	}
	    	requrl = requrl.trim();
	    	if(requrl.equals("")) { return; }
	    	try {
	    		AppInfo.DebugLog(mContext, "downloadContent url: " + url);
	    		AppInfo.DebugLog(mContext, "downloadContent requrl: " + requrl);
	    		httpget = new HttpGet(requrl);
				httpget.setHeader("Accept-Charset", "UTF-8,*");
				httpget.addHeader("User-Agent", userAgent);
				res = dhc.execute(httpget);
				int status = res.getStatusLine().getStatusCode();
				if(status != HttpStatus.SC_OK) {
					return;
				}
				entity = res.getEntity();
				input = entity.getContent();
				out = new ByteArrayOutputStream();
				int size;
				byte[] byteArray = new byte[1024];
	    		while((size = input.read(byteArray)) != -1) {
	    			out.write(byteArray, 0, size);
	    		}
	    		File dstFile = new File(dstFilePath);
	    		output = new FileOutputStream(dstFile);
	    		output.write(out.toByteArray(), 0, out.size());
	    		output.flush();
	    		output.close();
	    		out.close();
			} catch(IllegalStateException e) {
				e.printStackTrace();
				result = true;
			} catch(IllegalArgumentException e) {
				e.printStackTrace();
				result = true;
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				result = true;
			} catch (IOException e) {
				e.printStackTrace();
				result = true;
			}  finally {
				try {
					if(out != null) {
						out.close();
						out = null;
					}
					if(output != null) {
						output.close();
						output = null;
					}
					if(input != null) {
						input.close();
						input = null;
					}
					if(entity != null) {
						entity.consumeContent();
						entity = null;
					}
					if(res != null) {
						res = null;
					}
					if(httpget != null) {
						httpget.abort();
						httpget = null;
					}
				} catch (IOException e) {
				}
				if(result) {
					throw new Exception();
				}
			}
	    }
		private String getContentName(int cnt, String path) {
			String wPath = path.substring(path.lastIndexOf("/") + 1);
			if(StringUtils.isBlank(wPath)) {
				wPath = "index.html";
			}
			wPath = wPath.replaceAll("\\*", "");
			return String.format("%04d-%s", cnt, wPath);
		}
		private String getPath(String dir, String path) {
			do {
				dir = dir.substring(0, dir.lastIndexOf("/"));
				path = path.substring(3);
			} while(path.indexOf("../") == 0);
			return dir + "/" + path;
		}
		private void getIndexCache(ClipWebcache cwc) throws InterruptedException, Exception {
	        String encoding = cwc.getEncoding();
	        File cachefile = new File(clipfilepath + "/.webcache/" + cwc.getFilepath());
        	String cachemain = readStringFile(cachefile, encoding);
        	if(cachemain == null) {
        		// 途中終了処理
        		throw new Exception();
        	} else if(cachemain.equals("")) {
        		throw new Exception();
        	}
        	diffsleep(DEFAULT_SLEEP_TIME);
    		int headpos = cachemain.indexOf("<head");
    		if(headpos == -1) {
    			headpos = cachemain.indexOf("<HEAD");
    		}
    		if(headpos == -1) {
    			StringBuilder sb = new StringBuilder();
    			sb.append("<html>");
    			sb.append(preview_content);
    			sb.append("</html>");
    			preview_content = "";
    			preview_content = null;
    			preview_content = sb.toString();
    			sb.setLength(0);
    			sb = null;
    		} else {
    			String head = cachemain.substring(0, headpos - 1);
    			StringBuilder sb = new StringBuilder();
    			sb.append(head);
    			sb.append(preview_content);
    			sb.append("</html>");
    			preview_content = "";
    			preview_content = null;
    			preview_content = sb.toString();
    			sb.setLength(0);
    			sb = null;
    		}
    		cachemain = "";
    		cachemain = null;
		}
		private void setDocType(String url) throws Exception {
			boolean result = false;
			DefaultHttpClient dhc = new DefaultHttpClient();
			HttpGet httpget = null;
			InputStream input = null;
			HttpEntity entity = null;
			HttpResponse res = null;
			ByteArrayOutputStream out = null;
			String content = "";
			try {
				httpget = new HttpGet(url);
				httpget.setHeader("Accept-Charset", "UTF-8,*");
				httpget.addHeader("User-Agent", userAgent);
				res = dhc.execute(httpget);
				int status = res.getStatusLine().getStatusCode();
				if(status != HttpStatus.SC_OK) {
					return;
				}
				entity = res.getEntity();
				input = entity.getContent();
				out = new ByteArrayOutputStream();
				int size;
				byte[] byteArray = new byte[1024];
	    		while((size = input.read(byteArray)) != -1) {
	    			out.write(byteArray, 0, size);
	    		}
				byteArray = null;
				byteArray = out.toByteArray();
				CharsetDetector detector = new CharsetDetector();
				Charset charset = detector.detectCharset(byteArray);
				String encoding = charset.name();
				// コンテンツの文字コートでコンテンツを取得する
	    		if(encoding != null) {
					String src = new String(byteArray, encoding);
					content = new String(src.getBytes("UTF-8"), "UTF-8");
	    		} else {
	    			content = new String(byteArray, "UTF-8");
	    		}
				byteArray = null;
				System.gc();
			} catch(IllegalStateException e) {
				e.printStackTrace();
				result = true;
			} catch(IllegalArgumentException e) {
				e.printStackTrace();
				result = true;
			} catch (ClientProtocolException e) {
				e.printStackTrace();
				result = true;
			} catch (IOException e) {
				e.printStackTrace();
				result = true;
			}  finally {
				try {
					if(input != null) {
						input.close();
						input = null;
					}
					if(entity != null) {
						entity.consumeContent();
						entity = null;
					}
					if(res != null) {
						res = null;
					}
					if(httpget != null) {
						httpget = null;
					}
				} catch (IOException e) {
				}
				if(result) {
					throw new Exception();
				}
			}
			int headpos = content.indexOf("<head");
			if(headpos == -1) {
				headpos = content.indexOf("<HEAD");
			}
			if(headpos == -1) {
				preview_content = "<html>" + preview_content + "</html>";
			} else {
				String head = content.substring(0, headpos - 1);
				preview_content = head + preview_content + "</html>";
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

		private String readStringFile(File srcFile, String cacheencoding) throws Exception {
			boolean result = false;
			String readString = "";
			InputStream input = null;
			try {
				input = new FileInputStream(srcFile);
				if(! StringUtils.isBlank(cacheencoding)) {
					return toString(input, cacheencoding);
				} else {
					readString = toString(input, null);
					String detectencord = "";
					Matcher m;
					m = Pattern.compile("(<meta\\s+.+?charset=)([^'\">]+)", Pattern.CASE_INSENSITIVE).matcher(readString);
					if(m.find()) {
						detectencord = m.group(2);
					} else {
						diffsleep(DEFAULT_SLEEP_TIME);

						m = Pattern.compile("(<meta\\scharset=)([^'\">]+)", Pattern.CASE_INSENSITIVE).matcher(readString);
						if(m.find()) {
							detectencord = m.group(2);
						} else {
							diffsleep(DEFAULT_SLEEP_TIME);

							m = Pattern.compile("(<?xml\\s+.+?encoding=\")([^'\">]+)", Pattern.CASE_INSENSITIVE).matcher(readString);
							if(m.find()) {
								detectencord = m.group(2);
							} else {
								diffsleep(DEFAULT_SLEEP_TIME);

								m = Pattern.compile("(<script\\s+.+?charset=\")([^'\">]+)", Pattern.CASE_INSENSITIVE).matcher(readString);
								if(m.find()) {
									detectencord = m.group(2);
								} else {
									diffsleep(DEFAULT_SLEEP_TIME);

									m = Pattern.compile("(@charset\\s*)(['\"])(.+?)(?:\\2)", Pattern.CASE_INSENSITIVE).matcher(readString);
									if(m.find()) {
										detectencord = m.group(3);
									}
								}
							}
						}
					}

					detectencord = detectencord.toUpperCase();
					if(detectencord.equals("UTF-8") == false) {
						if(StringUtils.isBlank(detectencord)) {
							input.close();
							input = null;
							CharsetDetector detector = new CharsetDetector();
							Charset charset = detector.detectCharset(srcFile);
							detectencord = charset.name();
							System.gc();
							if(detectencord != null) {
								input = new FileInputStream(srcFile);
								readString = toString(input, detectencord);
							} else {
							}
						} else {
							input.close();
							input = null;
							input = new FileInputStream(srcFile);
							readString = toString(input, detectencord);
						}
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
				readString = "";
				result = true;
			} catch (OutOfMemoryError e) {
				e.printStackTrace();
				readString = "";
				result = true;
			} catch (InterruptedException e) {
				e.printStackTrace();
				readString = "";
				result = true;
			} finally {
				try {
					if(input != null) {
						input.close();
						input = null;
					}
				} catch(IOException e) {
					e.printStackTrace();
				}
				if(result) {
					throw new Exception();
				}
			}
			System.gc();
	        return readString;
		}
		private void copyFile(String srcFilePath, String dstFilePath) throws Exception {
			File srcFile = new File(srcFilePath);
			File dstFile = new File(dstFilePath);

			// ディレクトリを作る.
			dstFile.getParentFile().mkdirs();

			// ファイルコピーのフェーズ
			try {
				InputStream input = null;
				OutputStream output = null;
				input = new FileInputStream(srcFile);
				output = new FileOutputStream(dstFile);
				int DEFAULT_BUFFER_SIZE = 1024 * 4;
				byte[] buffer = new byte[DEFAULT_BUFFER_SIZE];
				int n = 0;
				while (-1 != (n = input.read(buffer))) {
				  output.write(buffer, 0, n);
				}
				input.close();
				output.flush();
				output.close();
			} catch(FileNotFoundException e) {
				e.printStackTrace();
				throw new Exception();
			} catch (IOException e) {
				e.printStackTrace();
				throw new Exception();
			}
		}
		private void clearWebCache() {
			String cachedirpath = clipfilepath + "/.webcache";
			File cachedir = new File(cachedirpath);
			if(cachedir.exists()) {
				File[] cachefiles = cachedir.listFiles();
				if(cachefiles.length > 0) {
					for(File cachefile : cachefiles) {
						if(cachefile.isFile()) {
							cachefile.delete();
						}
					}
				}
				cachedir.delete();
			}
		}
		private void clearClipPath() {
			File clipdir = new File(clipfilepath);
			if(clipdir.exists()) {
				File[] clipfiles = clipdir.listFiles();
				if(clipfiles.length > 0) {
					for(File clipfile : clipfiles) {
						if(clipfile.isFile()) {
							clipfile.delete();
						}
					}
				}
				clipdir.delete();
			}
		}
		private void clipsleep(int progress, long dbindex, ClipQueueItem cq) throws ClipCancelException, InterruptedException {
			if(cq.getProgress() == -2) {
				throw new ClipCancelException();
			}
			cq.setProgress(progress);
			mHandler.post(new ShowNotificationThread(mContext, cq.getQueueindex(), progress, dbindex));
//			mHandler.post(new ShowNotificationThread(mContext, R.string.app_name, progress, dbindex));
			ClipQueueList.set(cq.getQueueindex(), cq);
			diffsleep(NOTIFY_SLEEP_TIME);
			AppInfo.DebugLog(mContext, "progress " + progress + "%");
		}
		@Override
		public void run() {
			this.setName("ClipServiceThread");
			oldtime = System.currentTimeMillis();

			ClipQueueItem cq = ClipQueueList.getNextQueueItem();
			AppInfo.DebugLog(mContext, "ClipService start index:" + cq.getQueueindex());
			do {
				try {
			        clipsleep(0, -1, cq);

					cachecnt = 0;
					String title = cq.getTitle();
					String description = cq.getDescription();
					String str_user_tags = cq.getStr_user_tags();
//					preview_content = cq.getContent();
					userAgent = cq.getUserAgent();
				    arrcache = cq.getArrcache();
					// アプリケーション名を取得
			        String appName = getResources().getString(R.string.app_name);
					clipfilepath = Environment.getExternalStorageDirectory().toString() + "/" + appName + "/.clip/" + cq.getClippath();
					File contentfile = new File(clipfilepath + "/.webcache/cachecontent.txt");
					preview_content = readStringFile(contentfile, "UTF-8");
					clipfilecount = 0;
			        if(cq.isClipmode() || (cq.isForcemode() == false)) {
			        	ClipWebcache cwc = arrcache.getCache(cq.getRequestUrl());
				        if(cwc != null) {
				        	getIndexCache(cwc);
				        	cachecnt++;
				        } else {
				        	setDocType(cq.getRequestUrl());
				        }
			    		System.gc();
			        }
					float progress = ((40.0f/arrcache.size()) * cachecnt);
			        clipsleep((int)progress, -1, cq);

			        File favicon = new File(clipfilepath + "/favicon.png");
					if(!favicon.exists()) {
						String favicon_url = "";
						// <LINK HREF要素に「favicon.ico」ファイルの参照が見つかるか確認
						String pattern_favicon = "<link.+?href=\"(.*?favicon.ico)";
						Matcher m = Pattern.compile(pattern_favicon, Pattern.CASE_INSENSITIVE).matcher(preview_content);
						if(m.find()) {
							// <LINK HREF要素に「favicon.ico」ファイルの参照が見つかった場合
							favicon_url = m.group(1);
						} else {
							// <link rel="icon" 要素が見つかるか確認
							pattern_favicon = "<link.+?rel=\"icon\".+?href=\"(.*)\"";
							m = Pattern.compile(pattern_favicon, Pattern.CASE_INSENSITIVE).matcher(preview_content);
							if(m.find()) {
								// <link rel="icon" 要素が見つかった場合
								favicon_url = m.group(1);
							} else {
								// <link rel="*icon" 要素が見つかるか確認
								pattern_favicon = "<link.+?rel=\".*?icon\".+?href=\"(.*)\"";
								m = Pattern.compile(pattern_favicon, Pattern.CASE_INSENSITIVE).matcher(preview_content);
								if(m.find()) {
									// <link rel="*icon" 要素が見つかった場合
									favicon_url = m.group(1);
								} else {
								}
							}
						}
						if(favicon_url.equals("") == false) {
							String domain = cq.getRequestUrl().replaceFirst("(?<!\\/)\\/(?!\\/).*$", "");
							String dir = cq.getRequestUrl().replaceFirst("\\/[^\\/]*$", "");
							if(favicon_url.indexOf("/") == 0) {
								favicon_url = domain + favicon_url;
							} else if(favicon_url.indexOf("./") == 0) {
								favicon_url = dir + "/" + favicon_url;
							} else if(favicon_url.indexOf("../") == 0) {
								//path = dir + "/" + path;
								favicon_url = getPath(dir, favicon_url);
							} else {
								Matcher dm = Pattern.compile("^\\w+:\\/{2,}", Pattern.CASE_INSENSITIVE).matcher(favicon_url);
								int mc = 0;
								while(dm.find()) {
									mc++;
								}
								if(mc != 1) {
									favicon_url = dir + "/" + favicon_url;
								}
							}

							AppInfo.DebugLog(mContext, "favicon url: " + favicon_url);
							try {
								downloadContent(favicon_url, clipfilepath + "/favicon.png");
								diffsleep(NOTIFY_SLEEP_TIME);
							} catch(Exception e) {
							}
						}
					}
					diffsleep(NOTIFY_SLEEP_TIME);
					if(cq.isClipmode()) {
				        contentfiles = new HashMap<String, String>();
				        contentfiles.put(cq.getRequestUrl(), "index.html");
				        // WEBコンテンツを取得
						getContent(cq.getRequestUrl(), cq);
						Matcher m;
						StringBuffer sb = new StringBuffer();
						m = Pattern.compile("(<meta\\scharset=\")([^'\">]+)", Pattern.CASE_INSENSITIVE).matcher(preview_content);
						if(!m.find()) {
							m = Pattern.compile("(<meta\\s+.+?charset=)([^\">]+)", Pattern.CASE_INSENSITIVE).matcher(preview_content);
							if(!m.find()) {
								m = Pattern.compile("(</head>)", Pattern.CASE_INSENSITIVE).matcher(preview_content);
								if(m.find()) {
									m.appendReplacement(sb, "<META HTTP-EQUIV=\"Content-type\" CONTENT=\"text/html; charset=UTF-8\">" + m.group(1));
								}
							}
						}
						m.appendTail(sb);
						preview_content = sb.toString();
						sb.setLength(0);

				        clipsleep(50, -1, cq);

						String filename = clipfilepath + "/index.html";
						AppInfo.DebugLog(mContext, clipfilepath + "/index.html save start");
						BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(new File(filename)), "UTF-8"));
						bw.write(preview_content);
						bw.flush();
						bw.close();
						AppInfo.DebugLog(mContext, clipfilepath + "/index.html saved");
					}

			        clipsleep(60, -1, cq);
			        if(cq.isClipmode() || (cq.isForcemode() == false)) {
						Matcher m = Pattern.compile("(<head)(.+?)(</head>)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(preview_content);
						if(m.find()) {
							preview_content = m.replaceAll("");
						}

						clipsleep(65, -1, cq);

						m = Pattern.compile("(<style)(.+?)(</style>)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(preview_content);
						if(m.find()) {
							preview_content = m.replaceAll("");
						}

						clipsleep(66, -1, cq);

						m = Pattern.compile("(<script)(.+?)(</script>)", Pattern.CASE_INSENSITIVE|Pattern.DOTALL).matcher(preview_content);
						if(m.find()) {
							preview_content = m.replaceAll("");
						}

						clipsleep(67, -1, cq);

						m = Pattern.compile("(//<\\!\\[CDATA\\[)(.+?)(//\\]\\]>)", Pattern.DOTALL).matcher(preview_content);
						if(m.find()) {
							preview_content = m.replaceAll("");
						}

						clipsleep(68, -1, cq);

						m = Pattern.compile("<br([^/>](/>|>)|(/>|>))", Pattern.CASE_INSENSITIVE).matcher(preview_content);
						if(m.find()) {
							preview_content = m.replaceAll("\n");
						}

						clipsleep(69, -1, cq);

						m = Pattern.compile("</div>", Pattern.CASE_INSENSITIVE).matcher(preview_content);
						if(m.find()) {
							preview_content = m.replaceAll("\n");
						}

						clipsleep(70, -1, cq);

						m = Pattern.compile("</h\\d+([^>]>|>)", Pattern.CASE_INSENSITIVE).matcher(preview_content);
						if(m.find()) {
							preview_content = m.replaceAll("\n");
						}

						clipsleep(71, -1, cq);

						m = Pattern.compile("<(\"[^\"]*\"|'[^']*'|[^'\">])*>", Pattern.CASE_INSENSITIVE).matcher(preview_content);
						if(m.find()) {
							preview_content = m.replaceAll("");
						}

						clipsleep(72, -1, cq);

						m = Pattern.compile("'", Pattern.CASE_INSENSITIVE).matcher(preview_content);
						if(m.find()) {
							preview_content = m.replaceAll("&apos;");
						}

						clipsleep(73, -1, cq);

						preview_content = preview_content.replaceAll("^[\\s　]*", "").replaceAll("[\\s　]*$", "");

						clipsleep(74, -1, cq);

						preview_content = preview_content.trim();
						String[] arrbody = preview_content.split("\n");
						StringBuilder sb = new StringBuilder();
						for(String line: arrbody) {
							if(StringUtils.isBlank(line)) {
								continue;
							}
							if(line.equals("\n")) {
								continue;
							}
							sb.append(line);
							sb.append("\n\n");
						}
						diffsleep(DEFAULT_SLEEP_TIME);
/*
						if(sb.length() > 1) {
							sb.deleteCharAt(sb.length() - 1);
						}
*/
						clipsleep(75, -1, cq);

						preview_content = sb.toString();

						clipsleep(76, -1, cq);

						sb.setLength(0);
						sb = null;
			        } else {
			        	preview_content = "";
			        }

					clipsleep(77, -1, cq);
					AppInfo.DebugLog(mContext, "content html replaced.");

					ArrayList<String> clip_tags = new ArrayList<String>();
					String[] user_tags = str_user_tags.split(",");
					for(String utag: user_tags) {
						utag = utag.trim();
						if(utag.equals("") == false) {
							clip_tags.add(utag);
						}
					}

					clipsleep(78, -1, cq);
					/*
					url = StringUtils.join(url.split("&"), "&amp;");
					url = StringUtils.join(url.split("]]>"), "]]&gt;");
					*/
					description = StringUtils.join(description.split("]]>"), "]]&gt;");

					clipsleep(90, -1, cq);
					AppInfo.DebugLog(mContext, "db progress start");

			    	String editing_tags = clip_tags.toString();
			    	editing_tags = editing_tags.substring(1, editing_tags.length() -1);
					Date now = new Date();
					ContentValues values = new ContentValues();
			    	values.put(ItemsSchema.DATATYPE, DreamNoteProvider.ITEMTYPE_HTML);
			    	values.put(ItemsSchema.DATE, now.getTime());
			    	values.put(ItemsSchema.UPDATED, now.getTime());
			    	values.put(ItemsSchema.CREATED, now.getTime());
			    	values.put(ItemsSchema.TITLE, title);
			    	values.put(ItemsSchema.CONTENT, preview_content);
			    	values.put(ItemsSchema.DESCRIPTION, description);
			    	values.put(ItemsSchema.PATH, cq.getClippath());
			    	values.put(ItemsSchema.RELATED, cq.getRequestUrl());
			    	values.put(ItemsSchema.TAGS, editing_tags);
			    	Uri result = mContext.getContentResolver().insert(DreamNoteProvider.ITEMS_CONTENT_URI, values);
			    	long dbindex = Long.parseLong(result.getPathSegments().get(1));
					AppInfo.DebugLog(mContext, "db progress end");
			    	cq.setProgress(100);
			    	ClipQueueList.set(cq.getQueueindex(), cq);
			    	if(contentfiles != null) {
				    	contentfiles.clear();
				    	contentfiles = null;
			    	}
			    	clearWebCache();
			    	mHandler.post(new ShowNotificationThread(mContext, cq.getQueueindex(), 100, dbindex));
//			    	mHandler.post(new ShowNotificationThread(mContext, R.string.app_name, 100, dbindex));
				} catch(ClipCancelException cce) {
			    	ClipQueueList.set(cq.getQueueindex(), cq);
			    	if(contentfiles != null) {
				    	contentfiles.clear();
				    	contentfiles = null;
			    	}
			    	clearWebCache();
			    	clearClipPath();
					mHandler.post(new ShowNotificationThread(mContext, cq.getQueueindex(), -2, -1));
//					mHandler.post(new ShowNotificationThread(mContext, R.string.app_name, -2, -1));
				} catch(Exception e) {
					e.printStackTrace();
					cq.setProgress(-1);
			    	ClipQueueList.set(cq.getQueueindex(), cq);
			    	if(contentfiles != null) {
				    	contentfiles.clear();
				    	contentfiles = null;
			    	}
			    	clearWebCache();
			    	clearClipPath();
					mHandler.post(new ShowNotificationThread(mContext, cq.getQueueindex(), -1, -1));
//					mHandler.post(new ShowNotificationThread(mContext, R.string.app_name, -1, -1));
				} finally {
			    	System.gc();
				}
		    	cq = ClipQueueList.getNextQueueItem();
			} while(cq != null);
			AppInfo.DebugLog(mContext, "clip service stop request.");
			Intent intent = new Intent(mContext, ClipService.class);
			mContext.stopService(intent);
		}
	}
	private class ClipCancelException extends Exception {
		/**
		 *
		 */
		private static final long serialVersionUID = -1140544899526294944L;

		public ClipCancelException() {}
	}
}
