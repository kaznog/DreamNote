/**
 *
 */
package com.kaznog.android.dreamnote.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;

/**
 * @author noguchi
 *
 */
public class extendFileUtils {
	private static final int DEFAULT_BUFFER_SIZE = 1024 * 4;
	private static long copyLarge(Reader input, Writer output) throws IOException, OutOfMemoryError {
		char[] buffer = new char[DEFAULT_BUFFER_SIZE];
		long count = 0;
		int n = 0;
		while (-1 != (n = input.read(buffer))) {
			output.write(buffer, 0, n);
			count += n;
		}
		buffer = null;
		System.gc();
		return count;
	}

	private static int copy(Reader input, Writer output) throws IOException, OutOfMemoryError {
		long count = copyLarge(input, output);
		if(count > Integer.MAX_VALUE) {
			return -1;
		}
		return (int)count;
	}
	private static void copy(InputStream input, Writer output) throws IOException, OutOfMemoryError {
		InputStreamReader in = new InputStreamReader(input);
		copy(in, output);
	}

	private static void copy(InputStream input, Writer output, String encording) throws IOException, OutOfMemoryError {
		if(encording == null) {
			copy(input, output);
		} else {
			InputStreamReader in = new InputStreamReader(input, encording);
			copy(in, output);
		}
	}
	private static String toString(InputStream input, String encording) throws IOException, OutOfMemoryError {
		StringWriter sw = new StringWriter();
		copy(input, sw, encording);
		String result = sw.toString();
		sw.close();
		sw = null;
		System.gc();
		return result;
	}

	public static String readStringFile(String filename, String cacheencoding) {
		return readStringFile(new File(filename), cacheencoding);
	}
	public static String readStringFile(File srcFile, String cacheencoding) {
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
					m = Pattern.compile("(<meta\\scharset=)([^'\">]+)", Pattern.CASE_INSENSITIVE).matcher(readString);
					if(m.find()) {
						detectencord = m.group(2);
					} else {
						m = Pattern.compile("(<?xml\\s+.+?encoding=\")([^'\">]+)", Pattern.CASE_INSENSITIVE).matcher(readString);
						if(m.find()) {
							detectencord = m.group(2);
						} else {
							m = Pattern.compile("(<script\\s+.+?charset=\")([^'\">]+)", Pattern.CASE_INSENSITIVE).matcher(readString);
							if(m.find()) {
								detectencord = m.group(2);
							} else {
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
/*
						input = new FileInputStream(srcFile);
						UniversalDetector detector = new UniversalDetector(null);
						int size;
						byte[] byteArray = new byte[4096];
						while((size = input.read(byteArray)) > 0 && ! detector.isDone()) {
							detector.handleData(byteArray, 0, size);
						}
						detector.dataEnd();
						detectencord = detector.getDetectedCharset();
						detector.reset();
						detector = null;
*/
						CharsetDetector detector = new CharsetDetector();
						Charset charset = detector.detectCharset(srcFile);
						detectencord = charset.name();
						System.gc();
						if(detectencord != null) {
//							Log.d(Constant.LOG_TAG, "UniversalDetector detectencoding: " + detectencord);
//							input.close();
//							input = null;
							input = new FileInputStream(srcFile);
							readString = toString(input, detectencord);
						} else {
//							Log.d(Constant.LOG_TAG, "no detectencoding");
//							return readString;
						}
					} else {
//						Log.d(Constant.LOG_TAG, "detectencoding: " + detectencord);
						input.close();
						input = null;
						input = new FileInputStream(srcFile);
						readString = toString(input, detectencord);
					}
				}
			}
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
	public static void copyFile(String srcFilePath, String dstFilePath) {
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
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	//Rawリソースのファイル保存
    public static void raw2file(Context context, int resID,String fileName) throws Exception {
        InputStream in=null;
        String path=context.getFilesDir().getAbsolutePath()+"/"+fileName;
        if (!(new File(path)).exists()) {
            in=context.getResources().openRawResource(resID);
            in2file(context,in,fileName);
        }
    }

    //入力ストリームのファイル保存
    public static void in2file(Context context, InputStream in,String fileName)
        throws Exception {
        int size;
        byte[] w=new byte[1024];
        OutputStream out=null;
        try {
            out=context.openFileOutput(fileName, Context.MODE_WORLD_READABLE);
            while (true) {
                size=in.read(w);
                if (size<=0) break;
                out.write(w,0,size);
            };
            out.flush();
            out.close();
            out = null;
            in.close();
            in = null;
        } catch (Exception e) {
            try {
                if (in !=null) in.close();
                if (out!=null) out.close();
            } catch (Exception e2) {
            }
            throw e;
        }
    }
    public static void clearfile(Context context) {
    	String[] filelist = context.fileList();
    	for(String file: filelist) {
    		context.deleteFile(file);
    	}
    }
    public static FilenameFilter getFileExtensionFilter(final String extension) {
    	return new FilenameFilter() {
    		public boolean accept(File file, String name) {
    			return name.endsWith(extension);
    		}
    	};
    }
}
