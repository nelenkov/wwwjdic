package org.nick.wwwjdic.updates;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.util.Log;

public class Version implements Comparable<Version> {

    private static final String TAG = Version.class.getSimpleName();

    public static final String MARKET_ANDROID = "AndroidMarket";
    public static final String MARKET_AMAZON = "AmazonAppStore";

    private String packageName;
    private String marketName;
    private int versionCode;
    private String versionName;
    private String downloadUrl;

    public static Version getAppVersion(Context context, String marketName) {
        Version result = new Version();
        result.marketName = marketName;
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(context.getPackageName(),
                    0);
            result.packageName = info.packageName;
            result.versionCode = info.versionCode;
            result.versionName = info.versionName;
        } catch (NameNotFoundException e) {
            Log.w(TAG, "Unable to obtain package info: ", e);
            return null;
        }

        return result;
    }

    public Version() {
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageName) {
        this.packageName = packageName;
    }

    public String getMarketName() {
        return marketName;
    }

    public void setMarketName(String marketName) {
        this.marketName = marketName;
    }

    public int getVersionCode() {
        return versionCode;
    }

    public void setVersionCode(int versionCode) {
        this.versionCode = versionCode;
    }

    public String getVersionName() {
        return versionName;
    }

    public void setVersionName(String versionName) {
        this.versionName = versionName;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public static Version fromJson(String jsonStr) {
        try {
            return fromJsonObj(new JSONObject(jsonStr));
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    private static Version fromJsonObj(JSONObject jsonObj) throws JSONException {
        Version result = new Version();
        result.packageName = jsonObj.getString("packageName");
        result.marketName = jsonObj.getString("marketName");
        result.versionCode = jsonObj.getInt("versionCode");
        result.versionName = jsonObj.getString("versionName");
        result.downloadUrl = jsonObj.getString("downloadUrl");

        return result;
    }

    public static List<Version> listFromJson(String jsonStr) {
        List<Version> result = new ArrayList<Version>();
        try {
            JSONArray jsonArr = new JSONArray(jsonStr);
            for (int i = 0; i < jsonArr.length(); i++) {
                JSONObject jsonObj = jsonArr.getJSONObject(i);
                Version ver = fromJsonObj(jsonObj);
                result.add(ver);
            }
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        return result;
    }

    public static List<Version> findMatching(Version version,
            List<Version> versions) {
        List<Version> result = new ArrayList<Version>();
        for (Version v : versions) {
            if (version.getMarketName().equals(v.getMarketName())
                    && version.getPackageName().equals(v.getPackageName())) {
                result.add(v);
            }
        }

        // descending order
        Collections.sort(result, Collections.reverseOrder());
        return result;
    }

    @Override
    public int compareTo(Version rhs) {
        return versionCode - rhs.versionCode;
    }

    @Override
    public String toString() {
        return String.format("version[%s: %s(%d)]", packageName, versionName,
                versionCode);
    }
}
