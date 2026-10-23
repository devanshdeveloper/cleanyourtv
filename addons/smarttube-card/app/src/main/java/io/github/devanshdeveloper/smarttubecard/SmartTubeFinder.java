package io.github.devanshdeveloper.smarttubecard;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class SmartTubeFinder {

    private static final String PREF_NAME = "smarttube_link";
    private static final String KEY_CACHED_PKG = "cached_pkg";
    private static final String KEY_CACHED_CLS = "cached_cls";

    private SmartTubeFinder() {}

    public static boolean isCandidate(String appLabel, String activityLabel, String packageName, String ownPackage) {
        if (packageName == null || packageName.equals(ownPackage)) {
            return false;
        }
        String app = appLabel != null ? appLabel.toLowerCase(Locale.ROOT) : "";
        String act = activityLabel != null ? activityLabel.toLowerCase(Locale.ROOT) : "";
        return app.contains("smarttube") || act.contains("smarttube");
    }

    public static ComponentName getCachedComponent(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        String pkg = sp.getString(KEY_CACHED_PKG, null);
        String cls = sp.getString(KEY_CACHED_CLS, null);
        if (pkg != null && cls != null) {
            return new ComponentName(pkg, cls);
        }
        return null;
    }

    public static void saveCachedComponent(Context context, ComponentName cn) {
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        if (cn == null) {
            sp.edit().remove(KEY_CACHED_PKG).remove(KEY_CACHED_CLS).apply();
        } else {
            sp.edit().putString(KEY_CACHED_PKG, cn.getPackageName())
                    .putString(KEY_CACHED_CLS, cn.getClassName())
                    .apply();
        }
    }

    public static boolean isComponentCallable(PackageManager pm, ComponentName cn) {
        if (cn == null) return false;
        try {
            ActivityInfo ai = pm.getActivityInfo(cn, 0);
            return ai != null && ai.exported;
        } catch (Exception e) {
            return false;
        }
    }

    public static ComponentName findSmartTube(Context context) {
        PackageManager pm = context.getPackageManager();
        String ownPkg = context.getPackageName();

        // 1. Check cache first
        ComponentName cached = getCachedComponent(context);
        if (cached != null && isComponentCallable(pm, cached)) {
            return cached;
        }

        // 2. Scan Leanback launcher activities
        List<ResolveInfo> activities = new ArrayList<>();
        Intent leanbackIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER);
        List<ResolveInfo> lb = pm.queryIntentActivities(leanbackIntent, 0);
        if (lb != null) activities.addAll(lb);

        // 3. Scan Standard launcher activities
        Intent standardIntent = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> std = pm.queryIntentActivities(standardIntent, 0);
        if (std != null) activities.addAll(std);

        for (ResolveInfo ri : activities) {
            if (ri == null || ri.activityInfo == null) continue;
            String pkg = ri.activityInfo.packageName;
            CharSequence appLabel = ri.activityInfo.applicationInfo != null
                    ? ri.activityInfo.applicationInfo.loadLabel(pm) : null;
            CharSequence actLabel = ri.activityInfo.loadLabel(pm);

            if (isCandidate(appLabel != null ? appLabel.toString() : null,
                    actLabel != null ? actLabel.toString() : null,
                    pkg, ownPkg)) {
                ComponentName cn = new ComponentName(pkg, ri.activityInfo.name);
                saveCachedComponent(context, cn);
                return cn;
            }
        }

        saveCachedComponent(context, null);
        return null;
    }
}
