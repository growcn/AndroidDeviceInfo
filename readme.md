## 获取设备ID



```
  <uses-permission android:name="android.permission.READ_PHONE_STATE" /> 
```


```
RequestParams params = new RequestParams();
Map<String, Object> mDeviceInfo = new DeviceInfo(this).getDeviceParams();

# 唯一设备号(不刷机的情况下是不会变的)
params.put("device[identifier]", mDeviceInfo.get("identifier"));

params.put("device[hardware]", mDeviceInfo.get("hardware"));
params.put("device[os]", mDeviceInfo.get("os"));
params.put("device[screen_resolution]",mDeviceInfo.get("screen_resolution"));
params.put("device[processor]", mDeviceInfo.get("processor"));
params.put("device[lng]", mDeviceInfo.get("lng"));
params.put("device[lat]", mDeviceInfo.get("lat"));
params.put("device[imei]", mDeviceInfo.get("imei"));
params.put("device[imsi]", mDeviceInfo.get("imsi"));
params.put("device[brand]", mDeviceInfo.get("brand"));
params.put("device[vercode]", mDeviceInfo.get("verCode"));
params.put("device[vername]", mDeviceInfo.get("verName"));
params.put("device[means_access]", mDeviceInfo.get("means_access"));
params.put("device[wifi_mac]", mDeviceInfo.get("wifi_mac"));

```

