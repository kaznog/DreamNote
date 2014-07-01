package com.kaznog.android.dreamnote.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import com.kaznog.android.dreamnote.R;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.text.format.DateFormat;

public class ImageManager {
	@SuppressWarnings("unused")
	private static final String TAG = "ImageManager";

	public static ArrayList<String> getImageList(Activity activity){
		ArrayList<String> list = new ArrayList<String>();
		ContentResolver contentResolver = activity.getContentResolver();
		Cursor c = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
//		Cursor c = activity.managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, null);
		if(c != null) {
			while(c.moveToNext()){
				String imagefile = c.getString(c.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATA));
				File file = new File(imagefile);
				list.add(file.getPath());
			}
		}
		return list;
	}

	private static String createName(long dateTaken) {
		return DateFormat.format("yyyy-MM-dd-kk-mm-ss", dateTaken).toString();
	}

	public static String getImageNameAsApplication(Context context) {
        long dateTaken = System.currentTimeMillis();
        String name = createName(dateTaken) + ".jpg";
        // アプリケーション名を取得
        String appName = context.getResources().getString(R.string.app_name);
        // SDカードのディレクトリ/アプリケーション名のディレクトリパスを作成
        String path = Environment.getExternalStorageDirectory().toString() + "/" + appName;
        try {
        	// ディレクトリがなければ作成
        	File dir = new File(path);
        	if(!dir.exists()) {
        		dir.mkdirs();
        	}
        } catch(SecurityException e) {
//        	Log.w(TAG, e);
        }
        return path + "/" + name;
	}

	public static String addImageAsApplication(Context context, String sourcefile) {
        long dateTaken = System.currentTimeMillis();
        String name = createName(dateTaken) + ".jpg";
        // アプリケーション名を取得
        String appName = context.getResources().getString(R.string.app_name);
        // SDカードのディレクトリ/アプリケーション名のディレクトリパスを作成
        String path = Environment.getExternalStorageDirectory().toString() + "/" + appName;
        return addImageAsApplication(context, name, dateTaken, path, name, sourcefile);
	}

    public static String addImageAsApplication(Context context, String name,
            long dateTaken, String directory,
            String filename, String sourcefile) {
        String filePath = directory + "/" + filename;
        try {
            // ディレクトリがなければ作成
            File dir = new File(directory);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            // ファイル移動
            File source = new File(sourcefile);
            File destination = new File(directory, filename);
            source.renameTo(destination);
        } catch(Exception e) {
//        	Log.w(TAG, e);
        }
        // ギャラリーへ反映
        new MediaScannerNotifier(context, filePath, "image/jpeg");
        return filePath;
    }

	public static String addImageAsApplication(Context context, Bitmap bitmap) {
        long dateTaken = System.currentTimeMillis();
        String name = createName(dateTaken) + ".jpg";
        // アプリケーション名を取得
        String appName = context.getResources().getString(R.string.app_name);
        // SDカードのディレクトリ/アプリケーション名のディレクトリパスを作成
        String path = Environment.getExternalStorageDirectory().toString() + "/" + appName;
        return addImageAsApplication(context, name, dateTaken, path, name, bitmap, null);
    }

    public static String addImageAsApplication(Context context, String name,
            long dateTaken, String directory,
            String filename, Bitmap source, byte[] jpegData) {

        OutputStream outputStream = null;
        String filePath = directory + "/" + filename;
        try {
            // ディレクトリがなければ作成
            File dir = new File(directory);
            if (!dir.exists()) {
                dir.mkdirs();
            }
            // ファイル出力
            File file = new File(directory, filename);
            if (file.createNewFile()) {
                outputStream = new FileOutputStream(file);
                if (source != null) {
                    // JPEG圧縮してファイル出力
                    source.compress(CompressFormat.JPEG, 75, outputStream);
                } else {
                    outputStream.write(jpegData);
                }
            }

        } catch (FileNotFoundException ex) {
//            Log.w(TAG, ex);
            return null;
        } catch (IOException ex) {
//            Log.w(TAG, ex);
            return null;
        } finally {
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (Throwable t) {
                }
            }
        }

        // ギャラリーへ反映
        new MediaScannerNotifier(context, filePath, "image/jpeg");
        return filePath;
    }
/*
    public static File convertImageUriToFile(Uri uri, Activity activity) {
    	Cursor cursor = null;
    	File result = null;
    	try {
    		String[] proj = {
    				MediaStore.Images.Media.DATA,
    				MediaStore.Images.Media._ID,
    				MediaStore.Images.ImageColumns.ORIENTATION
    		};
    		cursor = activity.managedQuery(
	    				uri,			// The URI of the content provider to query.
	    				proj,			// projection : List of columns to return
	    				null,			// selection : WHERE clause; which rows to return (all rows)
	    				null,			// selectionArgs : WHERE clause selection arguments (none)
	    				null			// sortOrder : Order-by clause (ascending by name)
	    			);
    		int file_ColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
    		int orientation_ColumnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ORIENTATION);
    		if(cursor.moveToFirst()) {
    			String orientation = cursor.getString(orientation_ColumnIndex);
    			result = new File(cursor.getString(file_ColumnIndex));
    		}
    	} catch(IllegalArgumentException e) {
    	} catch(Exception e) {
    	}
    		finally {
    		if(cursor != null) {
    			cursor.close();
    		}
    	}
    	return result;
    }
*/
    public static boolean checkExistWebFile(String weburl)
    {
		HttpURLConnection c=null;
		int resCode=-1;

		try {
			URL url=new URL(weburl);
			c=(HttpURLConnection)url.openConnection();
			c.setRequestMethod("HEAD");
			c.connect();
			resCode = c.getResponseCode();
			c.disconnect();
		}catch(Exception e){
			try{
				if(c!=null)  c.disconnect();
			}catch(Exception e2){}
        }
        if(resCode==HttpURLConnection.HTTP_OK) return true;
        else  return false;
    }

    public static boolean deleteGalleryFile(ContentResolver cr, String directory, String filename) {
		Cursor cursor = null;
		try {
			filename = directory + "/" + filename;
			String[] proj = { MediaStore.Images.Media._ID };
			cursor = cr.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, proj, MediaStore.Images.Media.DATA + " =?", new String[]{filename}, null);
			if(cursor.getCount() != 0) {
				cursor.moveToFirst();
				int columnIndex = cursor.getColumnIndex(MediaStore.Images.Media._ID);
				Uri uri = ContentUris.appendId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.buildUpon(), cursor.getLong(columnIndex)).build();
				cr.delete(uri, null, null);
			}
		} finally {
			if(cursor != null) {
				cursor.close();
			}
		}
		return false;
	}

	//Bitmap→バイトデータ
	public static byte[] bmp2data(Bitmap src, Bitmap.CompressFormat format, int quality) {
		if(src != null) {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			src.compress(format,quality,os);
			return os.toByteArray();
		}
		return null;
	}
}
