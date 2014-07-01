package com.kaznog.android.dreamnote.util;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.SortedMap;

public class CharsetDetector {
	public static final String[] charsetsDefault = {
		"UTF-8",
		"UTF-16BE",
		"UTF-16LE",
		"UTF-16",
		"UTF-32BE",
		"UTF-32LE",
		"ISO-2022-JP",
		"SHIFT_JIS",
		"EUC-JP",
		"VISCII",
		"WINDOWS-1258",
		"WINDOWS-1252",
		"ISO-2022-CN",
		"BIG5",
		"EUC-TW",
		"GB18030",
		"HZ-GB-2312",
		"ISO-8859-5",
		"KOI8-R",
		"WINDOWS-1251",
		"MACCYRILLIC",
		"IBM866",
		"IBM855",
		"ISO-8859-7",
		"WINDOWS-1253",
		"ISO-8859-8",
		"WINDOWS-1255",
		"ISO-2022-KR",
		"EUC-KR"
	};
	public Charset detectCharset(File f) {
		Charset charset = null;
		try {
			// 使用可能なキャラセット
			SortedMap<String, Charset> m = Charset.availableCharsets();
			for (Charset c : m.values()) {
				charset = detectCharset(f, c);
				if(charset != null) { break; }
			}
		} catch(Exception e) {
			return null;
		}
		return charset;
	}
	public Charset detectCharset(File f, String[] charsets) {
		Charset charset = null;
		for(String charsetName : charsets) {
			charset = detectCharset(f, Charset.forName(charsetName));
			if(charset != null) { break; }
		}
		return charset;
	}
	private Charset detectCharset(File f, Charset charset) {
		try {
			BufferedInputStream input = new BufferedInputStream(new FileInputStream(f));
			CharsetDecoder decoder = charset.newDecoder();
			decoder.reset();
			byte[] buffer = new byte[4096];
			boolean identified = false;
			while((input.read(buffer) != -1) && (!identified)) {
				identified = identify(buffer, decoder);
			}
			input.close();
			if(identified) {
				return charset;
			} else {
				return null;
			}
		} catch(Exception e) {
			return null;
		}
	}
	public Charset detectCharset(byte[] bytes, String[] charsets) {
		Charset charset = null;
		boolean identified = false;
		try {
			for(String charsetName : charsets) {
				charset = Charset.forName(charsetName);
				CharsetDecoder decoder = charset.newDecoder();
				decoder.reset();
				identified = identify(bytes, decoder);
				if(identified) {
					break;
				}
			}
		} catch(Exception e) {
			return null;
		}
		return charset;
	}
	public Charset detectCharset(byte[] bytes) {
		Charset charset = null;
		boolean identified = false;
		try {
			// 使用可能なキャラセット
			SortedMap<String, Charset> m = Charset.availableCharsets();
			for (Charset c : m.values()) {
				CharsetDecoder decoder = c.newDecoder();
				decoder.reset();
				identified = identify(bytes, decoder);
				if(identified) {
					charset = c;
					break;
				}
			}
		} catch(Exception e) {
			return null;
		}
		return charset;
	}
	private boolean identify(byte[] bytes, CharsetDecoder decoder) {
		try {
			decoder.decode(ByteBuffer.wrap(bytes));
		} catch(CharacterCodingException e) {
			return false;
		}
		return true;
	}
}
