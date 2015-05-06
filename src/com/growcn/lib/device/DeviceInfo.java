package com.growcn.lib.device;

import org.apache.commons.codec.binary.Hex;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.ConnectivityManager;
import android.telephony.TelephonyManager;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

public class DeviceInfo {
	private static String TAG = "device";
	private Context mContext;

	public DeviceInfo(Context ctx) {
		mContext = ctx;
	}

	public Map<String, Object> getDeviceParams() {
		HashMap<String, Object> device = new HashMap<String, Object>();
		device.put("identifier", findUDID(mContext));
		device.put("hardware", getModelString());
		device.put("os", getOSVersionString());
		device.put("screen_resolution", getScreenInfo(mContext));
		device.put("processor", getProcessorInfo());
		// Location location =
		// LocationProxy.getInstance(mContext).getLocation();
		// device.put("lng",
		// location == null ? "unknown" : location.getLongitude() + "");
		// device.put("lat", location == null ? "unknown" :
		// location.getLatitude()
		// + "");

		Map<String, String> mPhoneInfo = getPhoneInfo(mContext);
		device.put("imei", mPhoneInfo.get("imei"));
		device.put("imsi", mPhoneInfo.get("imsi"));
		device.put("line1_number", mPhoneInfo.get("line1_number"));
		device.put("line2_number", mPhoneInfo.get("line2_number"));
		device.put("brand", getBrand());

		Map<String, String> mPackageVser = getPackageVser(mContext);
		device.put("verName", mPackageVser.get("verName"));
		device.put("verCode", mPackageVser.get("verCode"));
		device.put("means_access", getMeansOfAccess());
		device.put("wifi_mac", getWifiMac());
		return device;
	}

	/**
	 * find mobile ANDROID ID ("9774d56d682e549c" is a magic universal 2.2
	 * emulator ID, which a number of mobiles' Android ID equals to)
	 * 
	 * @return android_id (If ANDROID_ID is null, return device ID)
	 */
	public static String findUDID(Context cxt) {

		String androidID = android.provider.Settings.Secure.getString(
				cxt.getContentResolver(),
				android.provider.Settings.Secure.ANDROID_ID);
		// BaseUtils.log(null, "get ANDROID ID: " + androidID);
		// 如果读取不到androidID，那么以设备号作为AndroidID,
		// 不过这个长度可能不一致，因此进行长度检查
		// some devices's android Id are '9774d56d682e549c'
		if (androidID == null || androidID.equals("9774d56d682e549c")) {
			String deviceId = null;
			try {
				TelephonyManager tm = (TelephonyManager) cxt
						.getSystemService(Context.TELEPHONY_SERVICE);
				deviceId = tm.getDeviceId();
			} catch (SecurityException e) {
				Log.e(TAG, e.getMessage());
			}
			if (deviceId != null && deviceId.length() > 16) {
				deviceId = deviceId.substring(0, 16);
			}

			if (deviceId != null && !deviceId.equals("000000000000000")) {
				androidID = deviceId;
			}
		}
		// If there's no android ID, or if it's the magic universal 2.2 emulator
		// ID, we need to generate one.
		if (androidID != null && !androidID.equals("9774d56d682e549c")
				&& !androidID.equals("000000000000000")) {
			// BaseUtils.log(null, "return : " + "android-id-" + androidID);
			return "android-id-" + androidID;
		} else {
			// We're in an emulator.
			/*
			 * SyncedStore.Reader r = getPrefs().read(); try { androidID =
			 * r.getString("udid", null); } finally { r.complete(); }
			 */

			if (androidID == null) {
				byte randomBytes[] = new byte[16];
				new Random().nextBytes(randomBytes);
				androidID = "android-emu-"
						+ new String(Hex.encodeHex(randomBytes)).replace(
								"\r\n", "");

				/*
				 * SyncedStore.Editor e = getPrefs().edit(); try {
				 * e.putString("udid", androidID); } finally { e.commit(); }
				 */
			}
			// BaseUtils.log(null, " return : " + androidID);
			return androidID;
		}
	}

	public static String AliasfindUDID(Context cxt) {
		String uuid = findUDID(cxt);
		// android-id-d5773af4f0e1eea9
		return uuid.replaceAll("-", "");
	}

	private static String cat(String filename) {
		FileInputStream f;
		try {
			f = new FileInputStream(filename);
			BufferedReader br = new BufferedReader(new InputStreamReader(f),
					8192);
			StringBuilder sb = new StringBuilder();
			String line = null;
			while ((line = br.readLine()) != null) {
				sb.append(line + "\n");
			}
			br.close();
			return sb.toString();
		} catch (Exception e) {
			// d'oh
		}

		return "unknown";
	}

	public static String getProcessorInfo() {
		String family = "unknown";
		try {
			for (String l : cat("/proc/cpuinfo").split("\n")) {
				if (l.startsWith("Processor\t")) {
					family = l.split(":")[1].trim();
					break;
				}
			}
		} catch (Exception e) {
			// Johnny can't parse
		}

		return String.format("family(%s) min(%s) max(%s)", family,
				cat("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq")
						.split("\n")[0],
				cat("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
						.split("\n")[0]);
	}

	public static String getModelString() {
		return "p(" + android.os.Build.PRODUCT + ")/m("
				+ android.os.Build.MODEL + ")";
	}

	public static String getOSVersionString() {
		return "v" + android.os.Build.VERSION.RELEASE + " ("
				+ android.os.Build.VERSION.INCREMENTAL + ")";
	}

	public static String getScreenInfo(Context ctx) {
		DisplayMetrics metrics = Util.getDisplayMetrics(ctx);
		return String.format("%dx%d (%f dpi)", metrics.widthPixels,
				metrics.heightPixels, metrics.density);
	}

	public static Map<String, String> getPhoneInfo(Context ctx) {
		String line1_number = null;
		String line2_number = null;
		String imei = null;
		String imsi = null;
		String sim = null;

		try {
			TelephonyManager tm = (TelephonyManager) ctx
					.getSystemService(Context.TELEPHONY_SERVICE);
			line1_number = tm.getLine1Number();
			line2_number = tm.getLine1Number();
			// sim_no = tm.getSimSerialNumber();
			imei = tm.getDeviceId();
			imsi = tm.getSubscriberId();
		} catch (SecurityException e) {
			Log.e("ForTest", e.getMessage());
		}
		HashMap<String, String> phone = new HashMap<String, String>();
		phone.put("line1_number", line1_number);
		phone.put("line2_number", line2_number);
		phone.put("imei", imei);
		phone.put("imsi", imsi);
		// Log.d(Config.TAG, "-------" + phone);
		return phone;
	}

	public static Map<String, String> getPackageVser(Context ctx) {
		PackageInfo info;
		String versionName = null;
		int versionCode = 0;
		String packageNames = null;
		HashMap<String, String> mPackage = new HashMap<String, String>();

		try {
			info = ctx.getPackageManager().getPackageInfo(ctx.getPackageName(),
					0);
			versionName = info.versionName;// 当前应用的版本名称
			versionCode = info.versionCode; // 当前版本的版本号
			packageNames = info.packageName;// 当前版本的包名
		} catch (NameNotFoundException e) {
			e.printStackTrace();
		}

		mPackage.put("verName", versionName);
		mPackage.put("verCode", versionCode + "");
		mPackage.put("packageNames", packageNames);
		return mPackage;
	}

	// 取得牌子
	public static String getBrand() {
		String manufacturer = null;
		try {
			Class<android.os.Build> build_class = android.os.Build.class;
			java.lang.reflect.Field manu_field;
			manu_field = build_class.getField("MANUFACTURER");
			manufacturer = (String) manu_field.get(new android.os.Build());
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchFieldException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Log.d(Config.TAG,"-----brand:"+manufacturer);
		return manufacturer;
	}

	// 上网方式 wifi gprs
	public String getMeansOfAccess() {
		String str = "";
		try {
			ConnectivityManager cm;
			cm = (ConnectivityManager) mContext
					.getSystemService(mContext.CONNECTIVITY_SERVICE);
			NetworkInfo net = cm.getActiveNetworkInfo();
			str = net.getTypeName();
		} catch (Exception e) {
			// TODO: handle exception

		}
		// boolean
		// isWifiConnected=cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState()
		// == NetworkInfo.State.CONNECTED ? true : false ;
		// boolean
		// isGprsConnected=cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState()
		// == NetworkInfo.State.CONNECTED ? true : false ;
		return str;
	}

	// wifi的mac
	public String getWifiMac() {
		String macAddress = null;
		try {
			WifiManager wifiMgr = (WifiManager) mContext
					.getSystemService(mContext.WIFI_SERVICE);
			WifiInfo info = (null == wifiMgr ? null : wifiMgr
					.getConnectionInfo());
			if (null != info) {
				macAddress = info.getMacAddress();
				// Log.d(Config.TAG,
				// "------------------asdfdddddddasfasf"+macAddress);
			}
		} catch (Exception e) {
			// TODO: handle exception
		}
		return macAddress;

	}
}
