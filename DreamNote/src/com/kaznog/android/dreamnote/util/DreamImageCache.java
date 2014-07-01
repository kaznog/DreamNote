package com.kaznog.android.dreamnote.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Picture;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.WindowManager;
import android.webkit.WebChromeClient;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import com.kaznog.android.dreamnote.R;
import com.kaznog.android.dreamnote.fragment.Notes;
import com.kaznog.android.dreamnote.item.ConnectionLock;

public class DreamImageCache {
	private Context _context;
	public static DisplayMetrics _metrics;
	private static DreamImageCache _instance = null;
	private String CacheDirName;
	private final HashMap<String, File> _cacheFileLists;
//	private final ArrayList<String> _tasklist = new ArrayList<String>();
	private File CacheDir;
	private static final int HARD_CACHE_CAPACITY = 8;
	private static final int DELAY_BEFORE_PURGE = 10 * 1000; // in milliseconds
	public static final boolean SCALING = false;
	public static final boolean NONSCALING = true;
	private final HashMap<String, Bitmap> _HardCache =
		new LinkedHashMap<String, Bitmap>(HARD_CACHE_CAPACITY / 2, 0.75f, true) {

		/**
			 *
			 */
			private static final long serialVersionUID = -6930799525908643675L;

		@Override
		protected boolean removeEldestEntry(LinkedHashMap.Entry<String, Bitmap> eldest) {
			if(size() > HARD_CACHE_CAPACITY) {
				_SoftCache.put(eldest.getKey(), new SoftReference<Bitmap>(eldest.getValue()));
				return true;
			} else {
				return false;
			}
		}
	};
	private final static ConcurrentHashMap<String, SoftReference<Bitmap>> _SoftCache =
		new ConcurrentHashMap<String, SoftReference<Bitmap>>(HARD_CACHE_CAPACITY / 2);

	private final Handler purgeHandler = new Handler();
	private final Runnable purger = new Runnable() {
        public void run() {
            clearCache();
        }
    };

	public static DreamImageCache getInstance() {
		if(_instance == null) {
			_instance = new DreamImageCache();
		}
		return _instance;
	}

	public void finalize() {
		_instance = null;
	}

	public DreamImageCache() {
		CacheDirName = "";
		// キャッシュファイル配列初期化
		_cacheFileLists = new HashMap<String, File>();
	}

	public boolean isInitialized() {
		if(_context == null || _metrics == null) {
			return false;
		}
		return true;
	}
	public void initialize(Context context, DisplayMetrics metrics, boolean remove) {
        _context = context;
        _metrics = metrics;
        if(remove) {
        	removeChachefile();
        }
	}
	public void removeChachefile() {
		if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
	        // アプリケーション名を取得
	        String appName = _context.getResources().getString(R.string.app_name);
	        // SDカードのディレクトリ/アプリケーション名のディレクトリパスを作成
	        CacheDirName = Environment.getExternalStorageDirectory().toString() + "/" + appName + "/.cache";
	    	// ディレクトリがなければ作成
	        CacheDir = new File(CacheDirName);
	        CacheDir.mkdirs();	// Make dir if not exists
	        // キャッシュディレクトリにファイルがあったら削除
	        File[] CacheFiles = CacheDir.listFiles();
	        if(CacheFiles != null) {
		        for(File cachefile : CacheFiles) {
		        	if(!cachefile.isDirectory()) {
		        		cachefile.delete();
		        	}
		        }
	        }
		}
		_cacheFileLists.clear();
        clearCache();
	}
	public void remove(String key) {
		SoftReference<Bitmap> ref = _SoftCache.get(key);
		if(ref != null) {
			Bitmap bm = ref.get();
			bm.recycle();
			bm = null;
			ref.clear();
			ref = null;
		}
		_SoftCache.remove(key);
		synchronized(_HardCache) {
			Bitmap bm = _HardCache.get(key);
			if(bm != null) {
				bm.recycle();
				bm = null;
			}
			_HardCache.remove(key);
		}
		File cachefile = _cacheFileLists.get(key);
		if(cachefile != null) {
			cachefile.delete();
			_cacheFileLists.remove(key);
		}
	}
	public void put(String key, Bitmap bitmap) {
		OutputStream outputStream = null;

		synchronized(_HardCache) {
			Bitmap tbm = _HardCache.get(key);
			if(tbm == null) {
				_HardCache.put(key, bitmap);
			}
		}

		// キャッシュファイルが保存されているか確認
		if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
			File cachefile = _cacheFileLists.get(key);
			if(cachefile != null) {
				// キャッシュがあるなら終了
				if(cachefile.exists()) { return; }
			}
			// キャッシュに保存しておく
			String newcachename = getUniqueFileName();
			File newcachefile = new File(newcachename);
			try {
				// ユニークなファイル名でキャッシュを作る
				if (newcachefile.createNewFile()) {
					// ユニークなキャッシュが作成できたら、jpegで保存
	                outputStream = new FileOutputStream(newcachefile);
	                if(key.endsWith(".png") || key.endsWith(".PNG")) {
	                	bitmap.compress(CompressFormat.PNG, 100, outputStream);
	                } else {
	                	bitmap.compress(CompressFormat.JPEG, 75, outputStream);
	                }
	                // jpegで保存できたら、キャッシュファイル配列へ登録
	                _cacheFileLists.put(key, newcachefile);
				}
			} catch(IllegalStateException e) {
			} catch (IOException e) {
			} finally {
				if(outputStream != null) {
					try {
						outputStream.close();
					} catch (IOException e) {
					}
				}
			}
		}
	}

	private String getUniqueFileName() {
		String fullFilename = "";
		boolean result = false;
		// ユニークなファイル名を生成し、ファイルが存在しない場合(file exist == false)ループを抜けてファイル名を返す
		do {
			fullFilename = CacheDirName + "/" + java.util.UUID.randomUUID().toString() + ".info";
			result = (new File(fullFilename)).exists();
		} while(result != false);
		return fullFilename;
	}

	private void resetPurgeTimer() {
        purgeHandler.removeCallbacks(purger);
        purgeHandler.postDelayed(purger, DELAY_BEFORE_PURGE);
    }

	public void clearCache() {
/*
		for(Map.Entry<String, Bitmap> cache : _HardCache.entrySet()) {
			Bitmap bm = cache.getValue();
			bm.recycle();
			bm = null;
		}
*/
		_HardCache.clear();
/*
		for(Map.Entry<String,SoftReference<Bitmap>> cache : _SoftCache.entrySet()) {
			SoftReference<Bitmap> ref = cache.getValue();
			Bitmap bm = ref.get();
			bm.recycle();
			bm = null;
		}
*/
		_SoftCache.clear();
	}

	public Bitmap get(String key, ListView listview, ImageView imageview, boolean mode, String data, BaseAdapter adapter) {
//		Log.d(Constant.LOG_TAG, "get req " + key);
		resetPurgeTimer();
		if(Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()) == false) {
			return null;
		}
		Bitmap bitmap = getBitmapFromCache(key);

		if(bitmap == null) {
			File cachefile = _cacheFileLists.get(key);
			if(cachefile != null) {
				if(cachefile.exists()) {
					// キャッシュファイルがある場合は読み込む
					bitmap = loadBitmap(cachefile.getPath());
					if(bitmap == null) {
						// キャッシュがあるのにBitmapを生成できない場合は、
						// 再取得する
						// キャッシュファイルを削除
						cachefile.delete();
						// キャッシュファイル配列から削除
						_cacheFileLists.remove(key);
						bitmap = reloadImage(key, listview, imageview, mode, data, adapter);
					}
				} else {
					_cacheFileLists.remove(key);
					bitmap = reloadImage(key, listview, imageview, mode, data, adapter);
				}
			} else {
				bitmap = reloadImage(key, listview, imageview, mode, data, adapter);
			}
		} else {
			cancelPotentialDownload(key, imageview);
			if(imageview != null) {
				imageview.setImageBitmap(bitmap);
			}
		}
		return bitmap;
	}

	private Bitmap reloadImage(String key, ListView listview, ImageView imageview, boolean mode, String data, BaseAdapter adapter) {
		Bitmap bitmap = null;
		if(data == null && key.indexOf(Environment.getExternalStorageDirectory().toString()) == 0) {
			if(mode == DreamImageCache.NONSCALING) {
				bitmap = loadBitmap(key);
				bitmap = rotateExifBitmap(key, bitmap);
			} else {
				bitmap = loadScaledBitmap(key);
				bitmap = rotateExifBitmap(key, bitmap);
			}
			if(bitmap != null && bitmap.isRecycled() == false) {
				put(key, bitmap);
			} else {
				bitmap = null;
				forceDownload(key, adapter, listview, imageview, mode, data);
			}
		} else {
			forceDownload(key, adapter, listview, imageview, mode, data);
		}
		return bitmap;
	}

	private void forceDownload(String url, BaseAdapter adapter, ListView listview, ImageView imageview, boolean mode, String data) {
        if (url == null
        ||  adapter == null
		||  listview == null
		||  imageview == null) {
            return;
        }
        if (cancelPotentialDownload(url, imageview)) {
        	LoadImageTask task = new LoadImageTask(_context, _metrics, adapter, listview, imageview);
            DownloadedDrawable downloadedDrawable = new DownloadedDrawable(task);
            if(imageview != null) {
            	imageview.setImageDrawable(downloadedDrawable);
            }
            String strmode = mode == DreamImageCache.SCALING ? null : "NOSCALE";
            try {
            	task.execute(url, strmode, data);
            } catch(RejectedExecutionException e) {
            	downloadedDrawable = null;
            	imageview.setImageBitmap(null);
            	return;
            }
        }
	}

	public Bitmap getBitmapFromCache(String key) {
		Bitmap result = null;

		synchronized(_HardCache) {
			result = _HardCache.get(key);
			if(result != null) {
				// Bitmapが保持されていた場合は、
				// 配列の最後尾に登録しなおしてSoftCacheへ移動しないようにする
				_HardCache.remove(key);
				_HardCache.put(key, result);
				return result;
			}
		}

		// ハードキャッシュから削除されている場合は、
		// ソフトキャッシュを確認
		SoftReference<Bitmap> ref = _SoftCache.get(key);
		if(ref != null) {
			result = ref.get();
		}
		if(result != null) {
			// ソフトキャッシュがある場合は、ソフトキャッシュから削除してハードキャッシュへ再登録
			_SoftCache.remove(key);
			synchronized(_HardCache) {
				_HardCache.put(key, result);
			}
		} else {
			// ソフトキャッシュから削除されてしまっている場合
			if(ref != null) {
				// キャッシュ登録してあったのにBitmapがクリアされてしまっている場合は、
				// 登録を削除して、再度取得しなおす。
				_SoftCache.remove(key);
			}
		}
		return result;
	}

	private boolean cancelPotentialDownload(String url, ImageView imageview) {
		LoadImageTask task = getLoadImageTask(imageview);
		if(task != null) {
			String task_url = task.url;
			if((task_url == null) || (!task_url.equals(url))) {
				task.cancel(true);
			} else {
				return false;
			}
		}
		return true;
	}

	private static LoadImageTask getLoadImageTask(ImageView imageview) {
		if(imageview != null) {
			Drawable drawable = imageview.getDrawable();
			if(drawable instanceof DownloadedDrawable) {
				DownloadedDrawable downloadeddrawable = (DownloadedDrawable)drawable;
				return downloadeddrawable.getLoadImageTask();
			}
		}
		return null;
	}

	static class DownloadedDrawable extends ColorDrawable {
		private final WeakReference<LoadImageTask> loadImageTaskReference;

		public DownloadedDrawable(LoadImageTask loadImageTask) {
			super(Color.WHITE);
			loadImageTaskReference = new WeakReference<LoadImageTask>(loadImageTask);
		}

		public LoadImageTask getLoadImageTask() {
			return loadImageTaskReference.get();
		}
	}

	private class LoadImageTask extends AsyncTask<String, Void, Bitmap> {
		private WeakReference<ImageView> weakimage;
		private BaseAdapter adapter;
		private ListView listview;
		private String tagname;
		private String url;
		private DisplayMetrics metrics;
		private Context context;
		public LoadImageTask(Context context, DisplayMetrics metrics, BaseAdapter adapter, ListView listview, ImageView imageview) {
			this.metrics = metrics;
			this.context = context;
			this.adapter = adapter;
			this.listview = listview;
			tagname = imageview.getTag().toString();
			weakimage = new WeakReference<ImageView>(imageview);
		}
		@Override
		protected Bitmap doInBackground(String... params) {
			Bitmap bm = null;
			boolean mode;
			String webdata;

			url = params[0];
			mode = params[1] == null ? false : true;
			webdata = params[2];
			try {
				// キャッシュファイルが保存されているか確認
				File cachefile = _cacheFileLists.get(url);
				if(cachefile != null) {
					if(cachefile.exists()) {
						synchronized(ConnectionLock.getInstance()._lock) {
							// キャッシュファイルがある場合は読み込む
							bm = loadBitmap(cachefile.getPath());
/*
							try {
								bm = BitmapFactory.decodeFile(cachefile.getPath());
							} catch(OutOfMemoryError e) {
								//メモリオーバーで画像が読めない場合は1/2で縮小
								int scale = 2;
								do {
									//メモリオーバーで画像が読めない場合は1/2で縮小
									BitmapFactory.Options options = new BitmapFactory.Options();
									options.inJustDecodeBounds = false;
									options.inSampleSize = scale;
									try {
										bm = BitmapFactory.decodeFile(url, options);
									} catch(OutOfMemoryError e2) {
										// 1/5でも読めない場合は読むのを諦める
										if(scale > 5) break;
										scale++;
									}
								} while(bm == null);
							}
*/
							if(bm == null) {
								// キャッシュがあるのにBitmapを生成できない場合は、
								// 再度HTTPにて取得する
								// キャッシュファイルを削除
								cachefile.delete();
								// キャッシュファイル配列から削除
								_cacheFileLists.remove(url);
								return null;
							}
						}
					} else {
						_cacheFileLists.remove(url);
						return null;
					}
				} else if(context != null){
					if(webdata != null) {
						// HTMLのレンダリング結果を画像にする場合
						synchronized(ConnectionLock.getInstance()._lock) {
							bm = getBitmapFromCache(url);
							if(bm != null) { return bm; }
					        String appName = context.getResources().getString(R.string.app_name);
							Notes viewer = (Notes)context;
							WebView webv = viewer.convertview;
							//webv.setWebChromeClient(new ICWebChromeClient(url));
							webv.setWebViewClient(new ICWebViewClient(url));
							String clipfilepath = Environment.getExternalStorageDirectory().toString() + "/" + appName + "/.clip/" + url;

							File clipdir = new File(clipfilepath);
							File[] clipcontents = clipdir.listFiles();
							for(File contentfile: clipcontents) {
								if(contentfile.isDirectory() == false) {
									try {
										InputStream is = new FileInputStream(contentfile);
										extendFileUtils.in2file(context, is, contentfile.getName());
										is = null;
									} catch (FileNotFoundException e) {
									} catch (Exception e) {
									}
								}
							}
							webv.loadUrl("content://com.kaznog.android.dreamnote/index.html");

//							webv.loadUrl("file://" + clipfilepath + "/index.html");
							do {
								bm = getBitmapFromCache(url);
							} while(bm == null);
							webv.setWebChromeClient(null);
							webv.setWebViewClient(null);
							webv.clearView();
							webv.loadUrl("about:blank");
							webv.stopLoading();
							extendFileUtils.clearfile(context);
							File thumbnailfile = new File(clipfilepath + "/thumbnail.png");
							OutputStream outputStream = null;
							try {
								// ユニークなファイル名でキャッシュを作る
								if (thumbnailfile.createNewFile()) {
									// ユニークなキャッシュが作成できたら保存
					                outputStream = new FileOutputStream(thumbnailfile);
				                	bm.compress(CompressFormat.PNG, 100, outputStream);
								}
							} catch (IOException e) {
							} finally {
								if(outputStream != null) {
									try {
										outputStream.close();
									} catch (IOException e) {
									}
								}
							}
							System.gc();
						}
					} else {
						synchronized(ConnectionLock.getInstance()._lock) {
							// 画像の場合は、拡張ストレージから読み込むかHTTPGETする
							if(url.indexOf(Environment.getExternalStorageDirectory().toString()) == 0) {
								// 読み込み元が拡張ストレージの場合
								if(mode) {
									// 縮小しない場合
									bm = loadBitmap(url);
								} else {
									// 縮小する場合
									bm = loadScaledBitmap(url);
								}
								// jpegファイルの場合は、Exif情報があれば画像の向きを修正する
								bm = rotateExifBitmap(url, bm);
					        	System.gc();
							} else {
								bm = dhcgetImage(url, mode);
							}
						}
					}
				}
			} catch(Exception e) {
//				e.printStackTrace();
			}
			return bm;
		}

		@Override
        public void onPostExecute(Bitmap result) {
			if(this.isCancelled()) {
				result = null;
			}
			if(result != null && result.isRecycled() == false) {
				Bitmap bm = getBitmapFromCache(url);
				if(bm == null) {
					put(url, result);
				} else {
					result = bm;
				}
				ImageView thumbnailView = (ImageView) listview.findViewWithTag(tagname);
				if(thumbnailView != null) {
					if(weakimage != null) {
						ImageView image = weakimage.get();
						if(image != null) {
							image.setImageBitmap(result);
//							image.invalidate();
						}
					}
/*
					thumbnailView.setImageBitmap(result);
					thumbnailView.invalidate();
*/
					adapter.notifyDataSetChanged();
				}
			}
			weakimage.clear();
			weakimage = null;
        }

        private Bitmap dhcgetImage(String strURL, boolean mode) {
        	//Bitmap生成用バッファ
        	byte[] result = null;

        	DefaultHttpClient httpClient;
        	HttpGet httpget = null;;
        	HttpResponse response = null;
        	InputStream is = null;
        	ByteArrayOutputStream out = null;
        	// リサイズ適用後Bitmap
        	Bitmap resizedBitmap = null;
        	// entity 取得初期化
        	HttpEntity httpEntity = null;
        	int size = 0;
        	try {
            	//画像読み込み用バッファ
            	byte[] byteArray = new byte[1024];
        		httpClient = new DefaultHttpClient();
        		HttpParams clientparams = httpClient.getParams();
        		// 接続に10秒、データ取得に10秒待機
        		HttpConnectionParams.setConnectionTimeout(clientparams, 10000);
        		HttpConnectionParams.setSoTimeout(clientparams, 10000);
        		httpget = new HttpGet(strURL);
        		response = httpClient.execute(httpget);
        		int status = response.getStatusLine().getStatusCode();
            	if(status != HttpStatus.SC_OK) {
            		// レスポンスがNGの場合は即終了
            		httpget.abort();
            	} else {
	        		httpEntity = response.getEntity();
	        		is = httpEntity.getContent();
	        		out = new ByteArrayOutputStream();
	        		while((size = is.read(byteArray)) != -1) {
	        			out.write(byteArray, 0, size);
	        		}
	        		result = out.toByteArray();
            	}
        	} catch(UnknownHostException e) {
        		httpget.abort();
        	} catch(ClientProtocolException cpe) {
        		httpget.abort();
        	} catch(IOException ie) {
        		httpget.abort();
        	} catch(OutOfMemoryError oome) {
        		httpget.abort();
        	} catch(Exception e) {
        		httpget.abort();
        	} finally {
        		try {
	        		if(is != null) {
						is.close();
	        		}
        			if(out != null) {
        				out.close();
        			}
        			if(httpEntity != null) {
        				httpEntity.consumeContent();
        			}
        		} catch(Exception e) {
        		}
        	}
        	if(result == null) {
        		return null;
        	}
        	if(result.length == 0) {
        		return null;
        	}
        	try {
        		Bitmap OrigBitmap = BitmapFactory.decodeByteArray(result, 0, result.length);
        		result = null;
            	if(mode) {
            		System.gc();
            		return OrigBitmap;
            	}
	        	int OrigWidth = OrigBitmap.getWidth();
	        	int OrigHeight = OrigBitmap.getHeight();

	        	// DisplayMetricsがGCされるのぉ！？
	        	if(this.metrics == null) {
	        		this.metrics = new DisplayMetrics();
	        		// contextを念のために取得してあるよ～
		        	if(context != null) {
			        	WindowManager manager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
			            manager.getDefaultDisplay().getMetrics(this.metrics);
		        	} else {
		        		// contextがないと画面サイズ取れないから、固定値
		        		this.metrics.widthPixels = 320;
		        		this.metrics.heightPixels = 480;
		        	}
	        	}
	        	int newWidth = 96;
	        	int newHeight = 96;
	        	float scaleWidth = ((float) newWidth) / OrigWidth;
	        	//float scaleHeight = ((float) newHeight) / OrigHeight;

	        	if((OrigWidth <= newWidth) && (OrigHeight <= newHeight)) {
	        		System.gc();
	        		return OrigBitmap;
	        	}
	        	Matrix matrix = new Matrix();
	        	//幅のみでスケール値を決めとく
	        	matrix.postScale(scaleWidth, scaleWidth);
	        	//rescale bitmap
	        	resizedBitmap = Bitmap.createBitmap(OrigBitmap, 0, 0, OrigWidth, OrigHeight, matrix, true);

	        	OrigBitmap.recycle();
	        	OrigBitmap = null;
	        	System.gc();
           	} catch(OutOfMemoryError e) {
    			return null;
        	}
        	return resizedBitmap;
        }
	}

	@SuppressWarnings("unused")
	private class ICWebChromeClient extends WebChromeClient {
		private int oldprogress;
		private String url;
		public ICWebChromeClient(String url) {
			oldprogress = 0;
			this.url = url;
		}
		public void onProgressChanged(WebView view, int progress) {
			if(oldprogress != progress && progress == 100) {
				Picture p = view.capturePicture();
				int OrigWidth = p.getWidth();
				if(OrigWidth == 0) { return; }
				Bitmap OrigBitmap = Bitmap.createBitmap(144, 144, Bitmap.Config.RGB_565);
				Canvas c = new Canvas(OrigBitmap);
				float scale = 144 * view.getScale() / OrigWidth;
				c.scale(scale, scale);
				c.translate(-view.getScrollX(), -view.getScrollY());
				c.drawPicture(p);
				int[] pixels = new int[144 * 144];
				int pixel = 0;
				OrigBitmap.getPixels(pixels, 0, 144, 0, 0, 144, 144);
				for(int y = 0; y < 144; y++) {
					for(int x = 0; x < 144; x++) {
						pixel |= pixels[x + y * 144];
					}
				}
				if(pixel != 0) {
					put(url, OrigBitmap);
				}
			}
			oldprogress = progress;
		}
	}
	private class ICWebViewClient extends WebViewClient {
		private String loadurl;
		public ICWebViewClient(String url) {
			this.loadurl = url;
		}
		@Override
		public void onPageStarted(WebView view, String url, Bitmap favicon) {
			if(url.startsWith("file://")) {
				view.stopLoading();
				return;
			}
		}
		@Override
		public void onPageFinished(WebView view, String url) {
			Picture p = view.capturePicture();
			int OrigWidth = p.getWidth();
			if(OrigWidth == 0) { return; }
			Bitmap OrigBitmap = null;
			try {
				OrigBitmap = Bitmap.createBitmap(OrigWidth, OrigWidth, Bitmap.Config.RGB_565);
			} catch(OutOfMemoryError e) {
				OrigBitmap = Bitmap.createBitmap(144, 144, Bitmap.Config.RGB_565);
			}
			Canvas c = new Canvas(OrigBitmap);
			c.translate(-view.getScrollX(), -view.getScrollY());
			c.drawPicture(p);
			put(loadurl, OrigBitmap);
		}
    	@Override
    	public boolean shouldOverrideUrlLoading(WebView view, String url) {
    		//view.loadUrl(url);
    		// プレビューでリンクをタッチしても他のURLへ遷移しないようにする
			return true;
    	}
	}

	private Bitmap loadBitmap(String path) {
		Bitmap bm = null;
		int scale = 2;
		// 縮小しない場合
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			//この値をtrueにすると実際には画像を読み込まず、
			//画像のサイズ情報だけを取得することができます。
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeFile(path, options);
			int scaleW = options.outWidth / _metrics.widthPixels + 1;
			int scaleH = options.outHeight / _metrics.heightPixels + 1;
			scale = Math.max(scaleW, scaleH);
			//今度は画像を読み込みたいのでfalseを指定
			options.inJustDecodeBounds = false;
			//先程計算した縮尺値を指定
			options.inSampleSize = scale;
			bm = BitmapFactory.decodeFile(path, options);
		} catch(OutOfMemoryError e) {
			System.gc();
			int retrycount = 1;
			do {
				scale++;
				//メモリオーバーで画像が読めない場合は1/2で縮小
				BitmapFactory.Options options = new BitmapFactory.Options();
				options.inJustDecodeBounds = false;
				options.inSampleSize = scale;
				try {
					bm = BitmapFactory.decodeFile(path, options);
				} catch(OutOfMemoryError e2) {
					System.gc();
					// 1/5でも読めない場合は読むのを諦める
					if(retrycount > 5) return null;
					retrycount++;
				}
			} while(bm == null);
		}
		return bm;
	}
	private Bitmap loadScaledBitmap(String path) {
		Bitmap bm = null;
		//読み込み用のオプションオブジェクトを生成
		BitmapFactory.Options options = new BitmapFactory.Options();
		//この値をtrueにすると実際には画像を読み込まず、
		//画像のサイズ情報だけを取得することができます。
		options.inJustDecodeBounds = true;
		//画像ファイル読み込み
		//ここでは上記のオプションがtrueのため実際の
		//画像は読み込まれないです。
		BitmapFactory.decodeFile(path, options);
		int scaleW = options.outWidth / 96 + 1;
		int scaleH = options.outHeight / 96 + 1;

		//縮尺は整数値で、2なら画像の縦横のピクセル数を1/2にしたサイズ。
		//3なら1/3にしたサイズで読み込まれます。
		int scale = Math.max(scaleW, scaleH);
		//今度は画像を読み込みたいのでfalseを指定
		options.inJustDecodeBounds = false;

		//先程計算した縮尺値を指定
		options.inSampleSize = scale;
		bm = BitmapFactory.decodeFile(path, options);
		return bm;
	}
	private Bitmap rotateExifBitmap(String path, Bitmap bm) {
		Bitmap rotateBitmap = null;
		if(bm == null) return bm;
		// jpegファイルの場合は、Exif情報があれば画像の向きを修正する
		if(path.endsWith(".jpg") || path.endsWith(".JPG")
		|| path.endsWith(".jpeg") || path.endsWith(".JPEG")) {
			ExifInterface exifInterface;
			try {
				Matrix matrix = new Matrix();
				try {
					exifInterface = new ExifInterface(path);
					if(exifInterface == null) {
						return bm;
					}
				} catch(IOException e) {
					return bm;
				}
				int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
				if(orientation == ExifInterface.ORIENTATION_ROTATE_90
				|| orientation == ExifInterface.ORIENTATION_ROTATE_180
				|| orientation == ExifInterface.ORIENTATION_ROTATE_270
				) {
					switch(orientation) {
					case ExifInterface.ORIENTATION_ROTATE_90:
						matrix.postRotate(90.0f);
						break;
					case ExifInterface.ORIENTATION_ROTATE_180:
						matrix.postRotate(180.0f);
						break;
					case ExifInterface.ORIENTATION_ROTATE_270:
						matrix.postRotate(270.0f);
						break;
					}
					try {
						rotateBitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
					} catch(OutOfMemoryError r) {
						System.gc();
						float scale = 2.0f;
						do {
							float postscale = 1.0f / scale;
							matrix.postScale(postscale, postscale);
							try {
								rotateBitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
							} catch(OutOfMemoryError e) {
								System.gc();
								if(scale > 10.0f) return null;
								scale += 1.0f;
							} catch(IllegalArgumentException e) {
								if(bm != null) {
									//Log.d(Constant.LOG_TAG, "width: " + bm.getWidth());
									//Log.d(Constant.LOG_TAG, "height: " + bm.getHeight());
									return bm;
								} else {
									return null;
								}
							}
						} while(rotateBitmap == null);
					} catch(IllegalArgumentException e) {
						if(bm != null) {
							return bm;
						} else {
							return null;
						}
					}
					bm.recycle();
					bm = null;
				} else {
					return bm;
				}
			} catch(OutOfMemoryError e) {
				System.gc();
				return null;
			}
			return rotateBitmap;
		} else {
			return bm;
		}
	}
}
