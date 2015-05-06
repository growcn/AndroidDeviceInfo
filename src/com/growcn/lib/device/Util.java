package com.growcn.lib.device;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.UnsupportedEncodingException;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

public class Util {
	private static final String TAG = "Util";
	public static final int VERSION = Integer.valueOf(Build.VERSION.SDK);

	public static boolean isEclairOrLater() {
		return VERSION >= 5;
	}

	// Returns as lhs.compareTo(rhs) - a negative number if lhs is a lower
	// version number than rhs,
	// 0 if equal, 1 if lhs is a greater version number than rhs.
	public static int compareVersionStrings(String lhs, String rhs) {
		Pattern p = Pattern.compile("\\.");
		String lhsa[] = p.split(lhs);
		String rhsa[] = p.split(rhs);
		int lb = Math.min(lhsa.length, rhsa.length);
		for (int i = 0; i < lb; ++i) {
			int leftAsInt = 0, rightAsInt = 0;
			try {
				leftAsInt = Integer.parseInt(lhsa[i]);
			} catch (NumberFormatException e) {
				Log.e(TAG, "compareVersionStrings(\"" + lhs + "\",\"" + rhs
						+ "\"): Bad version component " + lhsa[i]);
				return -1;
			}

			try {
				rightAsInt = Integer.parseInt(rhsa[i]);
			} catch (NumberFormatException e) {
				Log.e(TAG, "compareVersionStrings(\"" + lhs + "\",\"" + rhs
						+ "\"): Bad version component " + lhsa[i]);
				return 1;
			}

			int delta = leftAsInt - rightAsInt;
			if (delta != 0)
				return delta;
		}

		return lhsa.length - rhsa.length;
	}

	public static final byte[] toByteArray(InputStream is) throws IOException {
		final int CHUNK_SIZE = 4096;
		byte readBuffer[] = new byte[CHUNK_SIZE];
		ByteArrayOutputStream accumulator = new ByteArrayOutputStream();
		int count;
		while ((count = is.read(readBuffer)) > 0) {
			accumulator.write(readBuffer, 0, count);
		}
		accumulator.close();
		return accumulator.toByteArray();
	}

	public static void deleteFiles(File path) {
		if (path.isDirectory()) {
			String[] files = path.list();
			for (String name : files) {
				File child = new File(path, name);
				deleteFiles(child);
			}
		}
		path.delete();
	}

	// Copies all files under srcDir to dstDir.
	// If dstDir does not exist, it will be created.
	public static void copyDirectory(File srcDir, File dstDir)
			throws IOException {
		if (srcDir.isDirectory()) {
			if (!dstDir.exists()) {
				dstDir.mkdir();
			}

			String[] children = srcDir.list();
			for (int i = 0; i < children.length; i++) {
				copyDirectory(new File(srcDir, children[i]), new File(dstDir,
						children[i]));
			}
		} else {
			// This method is implemented in Copying a File
			copyFile(srcDir, dstDir);
		}
	}

	public static void copyFile(File src, File dst) throws IOException {
		InputStream in = new FileInputStream(src);
		OutputStream out = new FileOutputStream(dst);
		copyStream(in, out);
	}

	public static void copyStreamAndLeaveInputOpen(InputStream in,
			OutputStream out) throws IOException {
		// Copy the bits from instream to outstream
		int len;
		byte[] copyBuffer = new byte[16384];
		while ((len = in.read(copyBuffer)) > 0) {
			out.write(copyBuffer, 0, len);
		}
		out.close();
	}

	public static void copyStream(InputStream in, OutputStream out)
			throws IOException {
		copyStreamAndLeaveInputOpen(in, out);
		in.close();
	}

	public static void saveFile(byte[] in, String path) throws IOException {
		File file = new File(path);
		file.getParentFile().mkdirs();
		FileOutputStream out = new FileOutputStream(file);
		out.write(in);
		out.close();
	}

	public static void saveStreamAndLeaveInputOpen(InputStream in, String path)
			throws IOException {
		File file = new File(path);
		file.getParentFile().mkdirs();
		FileOutputStream out = new FileOutputStream(file);
		copyStreamAndLeaveInputOpen(in, out);
	}

	public static void saveStream(InputStream in, String path)
			throws IOException {
		saveStreamAndLeaveInputOpen(in, path);
		in.close();
	}

	public static boolean isSymblic(File f) {
		try {
			return !f.getCanonicalPath().equals(f.getAbsolutePath());
		} catch (IOException e) {
		}
		return false;
	}

	public static boolean sdcardReady(Context ctx) {
		if (!noSdcardPermission(ctx))
			return false;
		String state = Environment.getExternalStorageState();
		if (!Environment.MEDIA_MOUNTED.equals(state)) {
			return false;
		}
		return true;
	}

	public static boolean noPermission(String permission, Context ctx,
			PackageManager pm) {
		return PackageManager.PERMISSION_DENIED == pm.checkPermission(
				permission, ctx.getPackageName());
	}

	public static boolean noSdcardPermission(Context ctx) {
		PackageManager pm = ctx.getPackageManager();
		return noPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE, ctx, pm);
	}

	public static byte[] readWholeFile(String path) throws IOException {
		File f = new File(path);
		int len = (int) f.length();
		InputStream in = new FileInputStream(f);
		byte[] b = new byte[len];
		in.read(b);
		in.close();
		return b;
	}

	public static String getDpiName(Context ctx) {
		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager winMan = (WindowManager) ctx
				.getSystemService(Context.WINDOW_SERVICE);
		winMan.getDefaultDisplay().getMetrics(metrics);
		if (metrics.density >= 2) {
			return "hdpi";// "udpi";//��ʱ��֧��udpi
		} else if (metrics.density >= 1.5) {
			return "hdpi";
		} else {
			return "mdpi";
		}
	}

	public static DisplayMetrics getDisplayMetrics(Context ctx) {
		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager wm = (WindowManager) ctx
				.getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getMetrics(metrics);
		return metrics;
	}

	public static String hexSHA1(String rawString) {
		if (rawString == null)
			return null;

		String result = null;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA1");
			byte[] bytes = md.digest(rawString.getBytes("UTF-8"));
			result = new String(Hex.encodeHex(bytes));
		} catch (UnsupportedEncodingException e) {
		} catch (NoSuchAlgorithmException e) {
		}
		return result;
	}
}
