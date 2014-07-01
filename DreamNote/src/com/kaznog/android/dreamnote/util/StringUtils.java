package com.kaznog.android.dreamnote.util;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.kaznog.android.dreamnote.R;

import android.content.Context;

public class StringUtils {
	public static String getEncodeType(String rawString) {
        ArrayList<String> encodeType = new ArrayList<String>();
        encodeType.add("UTF-8");
        encodeType.add("UTF-16");
        encodeType.add("MS932");
        encodeType.add("EUC_JP");
        encodeType.add("SJIS");
        encodeType.add("ISO2022JP");
        encodeType.add("JIS0201");
        encodeType.add("JIS0208");
        encodeType.add("JIS0212");
        encodeType.add("Cp930");
        encodeType.add("Cp939");
        encodeType.add("Cp942");
        encodeType.add("Cp943");
        encodeType.add("Cp33722");
        for (Iterator<String> iter = encodeType.iterator(); iter.hasNext();) {
            String encode = (String) iter.next();
            String specificEncoded;
            String autoEncoded;
            try {
                specificEncoded = new String(rawString.getBytes("iso-8859-1"),encode);
                autoEncoded = new String(rawString.getBytes("iso-8859-1"),"JISAutoDetect");
            } catch (UnsupportedEncodingException e) {
                return null;
            }
            if (specificEncoded.equals(autoEncoded)) {
                return encode;
            }
        }
        return null;
    }
	public static String getDayOfWeek(Context context, int dayofweek) {
		String[] arr_day_of_week = {
//				"日", "月", "火", "水", "木", "金", "土"
				context.getResources().getString(R.string.sunday),
				context.getResources().getString(R.string.monday),
				context.getResources().getString(R.string.tuesday),
				context.getResources().getString(R.string.wednesday),
				context.getResources().getString(R.string.thursday),
				context.getResources().getString(R.string.friday),
				context.getResources().getString(R.string.saturday)
		};
		return "(" + arr_day_of_week[dayofweek] + ")";
	}
	public static boolean isBlank(String text) {
		if(text == null) return true;
		if(text.trim().length() == 0) return true;
		return false;
	}
	public static boolean isEmpty(String text) {
		if(text == null) return true;
		if(text.length() == 0) return true;
		return false;
	}
	public static String join(String[] arry, String with) {
		StringBuilder builder = new StringBuilder();
		for(String s: arry) {
			if(builder.length() > 0) {
				builder.append(with);
			}
			builder.append(s);
		}
		return builder.toString();
	}
	public static int toDayOfWeek(String date) {
		String[] arrdate = date.split(" ");
		String strdate = arrdate[0];
		String[] arrstrdate = strdate.split("/");
		int year = Integer.parseInt(arrstrdate[0]);
		int month = Integer.parseInt(arrstrdate[1]);
		int day = Integer.parseInt(arrstrdate[2]);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month - 1, day);
        return cal.get(Calendar.DAY_OF_WEEK);
	}
	/**
	 *
	 * @param date String format [yyyy/MM/dd HH:mm:ss]
	 * @return Date
	 */
	public static Date toDate(String date) {
		String[] arrdate = date.split(" ");
		String strdate = arrdate[0];
		String[] arrstrdate = strdate.split("/");
		int year = Integer.parseInt(arrstrdate[0]);
		int month = Integer.parseInt(arrstrdate[1]);
		int day = Integer.parseInt(arrstrdate[2]);
        Calendar cal = Calendar.getInstance();
        cal.clear();
        cal.set(year, month - 1, day);
        int HH;
        int mm;
        int ss;
        if(arrdate.length > 1) {
        	String strtime = arrdate[1];
        	switch(strtime.length()) {
        	case 5:
        		HH = Integer.parseInt(strtime.substring(0,2));
                mm = Integer.parseInt(strtime.substring(3,5));
                cal.set(Calendar.HOUR_OF_DAY,HH);
                cal.set(Calendar.MINUTE,mm);
            	break;
        	case 8:
        		HH = Integer.parseInt(strtime.substring(0,2));
                mm = Integer.parseInt(strtime.substring(3,5));
                ss = Integer.parseInt(strtime.substring(6,8));
                cal.set(Calendar.HOUR_OF_DAY,HH);
                cal.set(Calendar.MINUTE,mm);
                cal.set(Calendar.SECOND,ss);
                break;
        	}
        }

		return cal.getTime();
	}
	/**
	 *
	 * @param date String format [yyyy/MM/dd HH:mm:ss]
	 * @return GMT milliseconds
	 */
	public static long toLongDate(String date) {
		//if(date.length() != 19) { return (long)0; }
		return toDate(date).getTime();
    }

	public static String getUniqueFileName(String parentpath) {
		String fullFilename = "";
		boolean result = false;
		// ユニークなファイル名を生成し、ファイルが存在しない場合(file exist == false)ループを抜けてファイル名を返す
		do {
			fullFilename = parentpath + "/" + java.util.UUID.randomUUID().toString() + ".info";
			result = (new File(fullFilename)).exists();
		} while(result != false);
		return fullFilename;
	}

	public static class EncryptedData {
        public byte[] iv;
        public byte[] data;
    }
	public static String encrypt(String seed, String cleartext) throws Exception {
        byte[] rawKey = getRawKey(seed.getBytes());
        byte[] result = encrypt(rawKey, cleartext.getBytes());
        return toHex(result);
    }
	public static String decrypt(String seed, String encrypted) throws Exception {
        byte[] rawKey = getRawKey(seed.getBytes());
        byte[] enc = toByte(encrypted);
        byte[] result = decrypt(rawKey, enc);
        return new String(result);
    }
	private static byte[] getRawKey(byte[] seed) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
        sr.setSeed(seed);
        kgen.init(128, sr); // 192 and 256 bits may not be available
        SecretKey skey = kgen.generateKey();
        byte[] raw = skey.getEncoded();
        return raw;
    }
	private static byte[] encrypt(byte[] raw, byte[] clear) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
        byte[] encrypted = cipher.doFinal(clear);
        return encrypted;
    }
	private static byte[] decrypt(byte[] raw, byte[] encrypted) throws Exception {
        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
        Cipher cipher = Cipher.getInstance("AES");
        cipher.init(Cipher.DECRYPT_MODE, skeySpec);
        byte[] decrypted = cipher.doFinal(encrypted);
        return decrypted;
    }
	public static String toHex(String txt) {
        return toHex(txt.getBytes());
    }
    public static String fromHex(String hex) {
        return new String(toByte(hex));
    }

    public static byte[] toByte(String hexString) {
        int len = hexString.length()/2;
        byte[] result = new byte[len];
        for (int i = 0; i < len; i++)
            result[i] = Integer.valueOf(hexString.substring(2*i, 2*i+2), 16).byteValue();
        return result;
    }

    public static String toHex(byte[] buf) {
        if (buf == null)
            return "";
        StringBuffer result = new StringBuffer(2*buf.length);
        for (int i = 0; i < buf.length; i++) {
            appendHex(result, buf[i]);
        }
        return result.toString();
    }
    private final static String HEX = "0123456789ABCDEF";
    private static void appendHex(StringBuffer sb, byte b) {
        sb.append(HEX.charAt((b>>4)&0x0f)).append(HEX.charAt(b&0x0f));
    }
}
