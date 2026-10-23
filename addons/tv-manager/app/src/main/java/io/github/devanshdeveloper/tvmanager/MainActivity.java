package io.github.devanshdeveloper.tvmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.provider.Settings;
import android.text.BidiFormatter;
import android.text.TextDirectionHeuristics;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {
    private enum Tab { DASHBOARD, STORAGE, APPS, ADB, NETWORK, DOWNLOADS, SETTINGS }

    private Tab currentTab = Tab.DASHBOARD;
    private LinearLayout llPageContainer;
    private SettingsHelper settingsHelper;

    private Button tabDashboard, tabStorage, tabApps, tabAdb, tabNetwork, tabDownloads, tabSettings;
    private boolean sortAppsByDate = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settingsHelper = new SettingsHelper(this);
        llPageContainer = findViewById(R.id.llPageContainer);

        tabDashboard = findViewById(R.id.tabDashboard);
        tabStorage = findViewById(R.id.tabStorage);
        tabApps = findViewById(R.id.tabApps);
        tabAdb = findViewById(R.id.tabAdb);
        tabNetwork = findViewById(R.id.tabNetwork);
        tabDownloads = findViewById(R.id.tabDownloads);
        tabSettings = findViewById(R.id.tabSettings);

        tabDashboard.setOnClickListener(v -> selectTab(Tab.DASHBOARD));
        tabStorage.setOnClickListener(v -> selectTab(Tab.STORAGE));
        tabApps.setOnClickListener(v -> selectTab(Tab.APPS));
        tabAdb.setOnClickListener(v -> selectTab(Tab.ADB));
        tabNetwork.setOnClickListener(v -> selectTab(Tab.NETWORK));
        tabDownloads.setOnClickListener(v -> selectTab(Tab.DOWNLOADS));
        tabSettings.setOnClickListener(v -> selectTab(Tab.SETTINGS));

        findViewById(R.id.btnSaveReport).setOnClickListener(v -> generateAndSaveReport());

        selectTab(Tab.DASHBOARD);
        tabDashboard.requestFocus();
    }

    private void selectTab(Tab tab) {
        currentTab = tab;
        updateTabStyles();
        llPageContainer.removeAllViews();

        switch (tab) {
            case DASHBOARD:
                renderDashboard();
                break;
            case STORAGE:
                renderStorage();
                break;
            case APPS:
                renderApps();
                break;
            case ADB:
                renderAdb();
                break;
            case NETWORK:
                renderNetwork();
                break;
            case DOWNLOADS:
                renderDownloads();
                break;
            case SETTINGS:
                renderSettings();
                break;
        }
    }

    private void updateTabStyles() {
        tabDashboard.setSelected(currentTab == Tab.DASHBOARD);
        tabStorage.setSelected(currentTab == Tab.STORAGE);
        tabApps.setSelected(currentTab == Tab.APPS);
        tabAdb.setSelected(currentTab == Tab.ADB);
        tabNetwork.setSelected(currentTab == Tab.NETWORK);
        tabDownloads.setSelected(currentTab == Tab.DOWNLOADS);
        tabSettings.setSelected(currentTab == Tab.SETTINGS);
    }

    private String number(String value) {
        return BidiFormatter.getInstance().unicodeWrap(value, TextDirectionHeuristics.LTR);
    }

    // -------------------------------------------------------------
    // a) Dashboard Page
    // -------------------------------------------------------------
    private void renderDashboard() {
        addHeader("System Dashboard");

        long[] mem = readMemory();
        if (mem[0] >= 0) {
            addStatCard(getString(R.string.label_memory, number(StatsCapacity.memory(mem[0], mem[1]) + " MB")));
            if (mem[1] > 0) {
                addStatCard(getString(R.string.label_memory_used,
                        number(StatsCapacity.memory(Math.max(0, mem[1] - mem[0]), mem[1]) + " MB")));
            }
        }

        File data = Environment.getDataDirectory();
        addStatCard(getString(R.string.label_storage_stat,
                number(StatsCapacity.storage(data.getFreeSpace(), data.getTotalSpace()))));

        long uptimeMs = SystemClock.elapsedRealtime();
        long hours = uptimeMs / 3600000;
        long minutes = (uptimeMs % 3600000) / 60000;
        addStatCard(getString(R.string.label_uptime, hours + "h " + minutes + "m"));

        addStatCard(getString(R.string.label_device, Build.MODEL, Build.MANUFACTURER));
        addStatCard(getString(R.string.label_android_ver, Build.VERSION.RELEASE, Build.VERSION.SDK_INT));

        DisplayMetrics dm = getResources().getDisplayMetrics();
        addStatCard(getString(R.string.label_display, dm.widthPixels, dm.heightPixels, dm.densityDpi));
    }

    private long[] readMemory() {
        try (FileReader reader = new FileReader("/proc/meminfo")) {
            return MemoryCapacity.readMb(reader);
        } catch (IOException e) {
            return new long[]{-1, -1};
        }
    }

    // -------------------------------------------------------------
    // b) Storage Page
    // -------------------------------------------------------------
    private void renderStorage() {
        addHeader(getString(R.string.storage_title));

        File data = Environment.getDataDirectory();
        long free = data.getFreeSpace();
        long total = data.getTotalSpace();
        long used = total - free;
        int percentUsed = total > 0 ? (int) ((used * 100) / total) : 0;

        double gb = 1024.0 * 1024 * 1024;
        String freeStr = String.format(Locale.US, "%.2f GB", free / gb);
        String totalStr = String.format(Locale.US, "%.2f GB", total / gb);

        addStatCard(getString(R.string.storage_breakdown, freeStr, totalStr, percentUsed));

        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(100);
        bar.setProgress(percentUsed);
        bar.setPadding(0, 16, 0, 16);
        llPageContainer.addView(bar);

        TextView note = new TextView(this);
        note.setTextSize(14);
        if (free < 1024L * 1024 * 1024) {
            note.setText(R.string.storage_warning_low);
            note.setTextColor(0xFFFF8A80);
        } else {
            note.setText(R.string.storage_ok);
            note.setTextColor(0xFF81C784);
        }
        note.setPadding(0, 8, 0, 16);
        llPageContainer.addView(note);
    }

    // -------------------------------------------------------------
    // c) Apps Page
    // -------------------------------------------------------------
    private void renderApps() {
        addHeader("Installed Applications");

        Button btnSort = new Button(this);
        btnSort.setText(sortAppsByDate ? R.string.sort_by_date : R.string.sort_by_name);
        btnSort.setBackgroundResource(R.drawable.btn_pill);
        btnSort.setTextColor(Color.WHITE);
        btnSort.setTextSize(12);
        btnSort.setFocusable(true);
        btnSort.setOnClickListener(v -> {
            sortAppsByDate = !sortAppsByDate;
            renderApps();
        });
        llPageContainer.addView(btnSort, new LinearLayout.LayoutParams(-2, (int) (36 * getResources().getDisplayMetrics().density)));

        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(
                new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER), 0);
        if (activities.isEmpty()) {
            activities = pm.queryIntentActivities(
                    new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        }

        List<AppItem> items = new ArrayList<>();
        for (ResolveInfo ri : activities) {
            if (ri.activityInfo != null) {
                String pkg = ri.activityInfo.packageName;
                long installTime = 0;
                try {
                    PackageInfo pi = pm.getPackageInfo(pkg, 0);
                    installTime = pi.firstInstallTime;
                } catch (Exception ignored) {
                }
                items.add(new AppItem(String.valueOf(ri.loadLabel(pm)), pkg, installTime));
            }
        }

        if (sortAppsByDate) {
            items.sort((a, b) -> Long.compare(b.installTime, a.installTime));
        } else {
            items.sort((a, b) -> a.name.compareToIgnoreCase(b.name));
        }

        for (AppItem app : items) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.card_bg);
            row.setPadding(16, 10, 16, 10);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = 8;
            row.setLayoutParams(lp);

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, -2, 1f);
            textCol.setLayoutParams(textLp);

            TextView tvName = new TextView(this);
            tvName.setText(app.name);
            tvName.setTextSize(16);
            tvName.setTextColor(Color.WHITE);
            textCol.addView(tvName);

            TextView tvPkg = new TextView(this);
            tvPkg.setText(app.pkg);
            tvPkg.setTextSize(12);
            tvPkg.setTextColor(0xFF9E9E9E);
            textCol.addView(tvPkg);

            row.addView(textCol);

            Button btnInfo = new Button(this);
            btnInfo.setText(R.string.btn_app_info);
            btnInfo.setBackgroundResource(R.drawable.btn_pill);
            btnInfo.setTextColor(Color.WHITE);
            btnInfo.setTextSize(11);
            btnInfo.setFocusable(true);
            btnInfo.setOnClickListener(v -> {
                Intent i = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                        .setData(Uri.fromParts("package", app.pkg, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            });
            row.addView(btnInfo);

            Button btnUninstall = new Button(this);
            btnUninstall.setText(R.string.btn_uninstall);
            btnUninstall.setBackgroundResource(R.drawable.btn_pill);
            btnUninstall.setTextColor(0xFFEF9A9A);
            btnUninstall.setTextSize(11);
            btnUninstall.setFocusable(true);
            btnUninstall.setOnClickListener(v -> {
                Intent i = new Intent(Intent.ACTION_UNINSTALL_PACKAGE)
                        .setData(Uri.fromParts("package", app.pkg, null))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(i);
            });
            row.addView(btnUninstall);

            llPageContainer.addView(row);
        }
    }

    private static class AppItem {
        final String name;
        final String pkg;
        final long installTime;

        AppItem(String name, String pkg, long installTime) {
            this.name = name;
            this.pkg = pkg;
            this.installTime = installTime;
        }
    }

    // -------------------------------------------------------------
    // d) ADB Hygiene Page
    // -------------------------------------------------------------
    private void renderAdb() {
        addHeader(getString(R.string.adb_title));

        boolean usbAdb = Settings.Global.getInt(getContentResolver(), Settings.Global.ADB_ENABLED, 0) == 1;
        addStatCard(getString(R.string.adb_usb_status,
                usbAdb ? getString(R.string.adb_state_on) : getString(R.string.adb_state_off)));

        // Network ADB check
        boolean netAdb = isPortOpen("127.0.0.1", 5555);
        addStatCard(getString(R.string.adb_network_status,
                netAdb ? getString(R.string.adb_state_on) : getString(R.string.adb_state_off)));

        addHeader(getString(R.string.adb_guide_title));
        addTextLine(getString(R.string.adb_step1));
        addTextLine(getString(R.string.adb_step2));
        addTextLine(getString(R.string.adb_step3));
        addTextLine(getString(R.string.adb_step4));

        String ip = getIpAddress();
        addTextLine("");
        addTextLine(getString(R.string.adb_check_cmd, ip.isEmpty() ? "<tv ip>" : ip));
    }

    private boolean isPortOpen(String host, int port) {
        try (java.net.Socket s = new java.net.Socket()) {
            s.connect(new java.net.InetSocketAddress(host, port), 200);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // -------------------------------------------------------------
    // e) Network Page
    // -------------------------------------------------------------
    private void renderNetwork() {
        addHeader("Network Diagnostics");

        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = cm != null ? cm.getActiveNetworkInfo() : null;
        String type = info != null && info.isConnected() ? info.getTypeName() : "Disconnected";
        addStatCard(getString(R.string.net_type, type));

        WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (wm != null && wm.isWifiEnabled()) {
            WifiInfo wi = wm.getConnectionInfo();
            if (wi != null) {
                String ssid = wi.getSSID();
                if (ssid != null && ssid.startsWith("\"") && ssid.endsWith("\"")) {
                    ssid = ssid.substring(1, ssid.length() - 1);
                }
                addStatCard(getString(R.string.net_wifi_name, ssid));
                addTextLine(getString(R.string.net_wifi_note));
                addStatCard(getString(R.string.net_wifi_signal, wi.getRssi(),
                        WifiManager.calculateSignalLevel(wi.getRssi(), 5) + "/4 bars"));
            }
        }

        addStatCard(getString(R.string.net_ip, getIpAddress()));
    }

    private String getIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.getAddress().length == 4) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return "";
    }

    // -------------------------------------------------------------
    // f) Downloads (Download Manager)
    // -------------------------------------------------------------
    private void renderDownloads() {
        addHeader(getString(R.string.downloads_title));

        List<File> files = getDownloadFiles();
        long apkCount = files.stream().filter(f -> f.getName().toLowerCase(Locale.ROOT).endsWith(".apk")).count();

        Button btnDeleteAll = new Button(this);
        btnDeleteAll.setText(getString(R.string.btn_delete_all_apks) + " (" + apkCount + ")");
        btnDeleteAll.setBackgroundResource(R.drawable.btn_pill);
        btnDeleteAll.setTextColor(0xFFEF9A9A);
        btnDeleteAll.setTextSize(12);
        btnDeleteAll.setFocusable(true);
        btnDeleteAll.setEnabled(apkCount > 0);
        btnDeleteAll.setAlpha(apkCount > 0 ? 1f : 0.4f);
        btnDeleteAll.setOnClickListener(v -> confirmDeleteAllApks(files));
        llPageContainer.addView(btnDeleteAll, new LinearLayout.LayoutParams(-2, (int) (36 * getResources().getDisplayMetrics().density)));

        if (files.isEmpty()) {
            addTextLine(getString(R.string.no_downloads));
            return;
        }

        DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (File f : files) {
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setBackgroundResource(R.drawable.card_bg);
            row.setPadding(16, 10, 16, 10);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = 8;
            row.setLayoutParams(lp);

            LinearLayout textCol = new LinearLayout(this);
            textCol.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams textLp = new LinearLayout.LayoutParams(0, -2, 1f);
            textCol.setLayoutParams(textLp);

            TextView tvName = new TextView(this);
            tvName.setText(f.getName());
            tvName.setTextSize(16);
            tvName.setTextColor(Color.WHITE);
            textCol.addView(tvName);

            String sizeStr = formatFileSize(f.length());
            TextView tvSub = new TextView(this);
            tvSub.setText(sizeStr + " \u00B7 " + df.format(new Date(f.lastModified())));
            tvSub.setTextSize(12);
            tvSub.setTextColor(0xFF80DEEA);
            textCol.addView(tvSub);

            row.addView(textCol);

            if (f.getName().toLowerCase(Locale.ROOT).endsWith(".apk")) {
                Button btnInstall = new Button(this);
                btnInstall.setText(R.string.btn_install);
                btnInstall.setBackgroundResource(R.drawable.btn_pill);
                btnInstall.setTextColor(0xFF81C784);
                btnInstall.setTextSize(11);
                btnInstall.setFocusable(true);
                btnInstall.setOnClickListener(v -> installApk(f));
                row.addView(btnInstall);
            }

            Button btnDelete = new Button(this);
            btnDelete.setText(R.string.btn_delete);
            btnDelete.setBackgroundResource(R.drawable.btn_pill);
            btnDelete.setTextColor(0xFFEF9A9A);
            btnDelete.setTextSize(11);
            btnDelete.setFocusable(true);
            btnDelete.setOnClickListener(v -> {
                if (f.delete()) {
                    Toast.makeText(this, "Deleted: " + f.getName(), Toast.LENGTH_SHORT).show();
                    renderDownloads();
                }
            });
            row.addView(btnDelete);

            llPageContainer.addView(row);
        }
    }

    private List<File> getDownloadFiles() {
        List<File> list = new ArrayList<>();
        File pub = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (pub != null && pub.exists() && pub.isDirectory()) {
            File[] files = pub.listFiles();
            if (files != null) Collections.addAll(list, files);
        }
        File appDir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);
        if (appDir != null && appDir.exists() && appDir.isDirectory()) {
            File[] files = appDir.listFiles();
            if (files != null) Collections.addAll(list, files);
        }
        list.sort((a, b) -> Long.compare(b.lastModified(), a.lastModified()));
        return list;
    }

    private void installApk(File file) {
        try {
            Uri uri = FileProvider.getUriForFile(file);
            Intent intent = new Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, "application/vnd.android.package-archive")
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "Installer error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void confirmDeleteAllApks(List<File> files) {
        long apkCount = files.stream().filter(f -> f.getName().toLowerCase(Locale.ROOT).endsWith(".apk")).count();
        new AlertDialog.Builder(this)
                .setTitle(R.string.btn_delete_all_apks)
                .setMessage(getString(R.string.confirm_delete_apk, apkCount))
                .setPositiveButton(R.string.btn_delete, (dialog, which) -> {
                    int deleted = 0;
                    for (File f : files) {
                        if (f.getName().toLowerCase(Locale.ROOT).endsWith(".apk") && f.delete()) {
                            deleted++;
                        }
                    }
                    Toast.makeText(this, getString(R.string.deleted_count, deleted), Toast.LENGTH_SHORT).show();
                    renderDownloads();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format(Locale.US, "%.1f KB", bytes / 1024.0);
        return String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0));
    }

    // -------------------------------------------------------------
    // g) Settings Controls (WRITE_SECURE_SETTINGS)
    // -------------------------------------------------------------
    private void renderSettings() {
        addHeader(getString(R.string.settings_title));

        boolean hasPerm = settingsHelper.hasPermission();
        if (!hasPerm) {
            TextView warning = new TextView(this);
            warning.setText(getString(R.string.settings_missing_perm, getPackageName()));
            warning.setTextColor(0xFFFFCC80);
            warning.setTextSize(14);
            warning.setPadding(0, 0, 0, 16);
            llPageContainer.addView(warning);
        }

        // Animation scales
        addHeader(getString(R.string.anim_scale_title));
        LinearLayout animRow = new LinearLayout(this);
        animRow.setOrientation(LinearLayout.HORIZONTAL);
        addActionButton(animRow, getString(R.string.anim_off), hasPerm, () -> settingsHelper.setAnimationScale(0f));
        addActionButton(animRow, getString(R.string.anim_half), hasPerm, () -> settingsHelper.setAnimationScale(0.5f));
        addActionButton(animRow, getString(R.string.anim_normal), hasPerm, () -> settingsHelper.setAnimationScale(1.0f));
        llPageContainer.addView(animRow);

        // Screensaver
        addHeader(getString(R.string.screensaver_title));
        LinearLayout saverRow = new LinearLayout(this);
        saverRow.setOrientation(LinearLayout.HORIZONTAL);
        addActionButton(saverRow, getString(R.string.screensaver_clock), hasPerm, () ->
                settingsHelper.setScreensaver("com.gathin.tvhome/.ClockDream"));
        llPageContainer.addView(saverRow);

        // Private DNS
        addHeader(getString(R.string.dns_title));
        LinearLayout dnsRow = new LinearLayout(this);
        dnsRow.setOrientation(LinearLayout.HORIZONTAL);
        addActionButton(dnsRow, getString(R.string.dns_adguard), hasPerm, () ->
                settingsHelper.setPrivateDns("hostname", "dns.adguard-dns.com"));
        addActionButton(dnsRow, getString(R.string.dns_cloudflare), hasPerm, () ->
                settingsHelper.setPrivateDns("hostname", "one.one.one.one"));
        addActionButton(dnsRow, getString(R.string.dns_off), hasPerm, () ->
                settingsHelper.setPrivateDns("off", null));
        llPageContainer.addView(dnsRow);

        // Revert button
        Button btnRevert = new Button(this);
        btnRevert.setText(R.string.btn_revert);
        btnRevert.setBackgroundResource(R.drawable.btn_pill);
        btnRevert.setTextColor(0xFF80DEEA);
        btnRevert.setTextSize(12);
        btnRevert.setFocusable(true);
        btnRevert.setEnabled(hasPerm);
        btnRevert.setAlpha(hasPerm ? 1f : 0.4f);
        btnRevert.setOnClickListener(v -> {
            if (settingsHelper.restorePreviousSettings()) {
                Toast.makeText(this, R.string.reverted_toast, Toast.LENGTH_SHORT).show();
            }
        });
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, (int) (38 * getResources().getDisplayMetrics().density));
        lp.topMargin = 16;
        llPageContainer.addView(btnRevert, lp);
    }

    private void addActionButton(LinearLayout container, String label, boolean enabled, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setBackgroundResource(R.drawable.btn_pill);
        b.setTextColor(Color.WHITE);
        b.setTextSize(12);
        b.setEnabled(enabled);
        b.setAlpha(enabled ? 1f : 0.4f);
        b.setFocusable(true);
        b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, (int) (36 * getResources().getDisplayMetrics().density));
        lp.rightMargin = 8;
        container.addView(b, lp);
    }

    // -------------------------------------------------------------
    // h) Plain text Report Generator
    // -------------------------------------------------------------
    private void generateAndSaveReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== CLEAN YOUR TV - SYSTEM REPORT ===\n");
        sb.append("Generated: ").append(new Date()).append("\n\n");

        sb.append("-- Device Information --\n");
        sb.append("Model: ").append(Build.MODEL).append("\n");
        sb.append("Manufacturer: ").append(Build.MANUFACTURER).append("\n");
        sb.append("Device Code: ").append(Build.DEVICE).append("\n");
        sb.append("Android Version: ").append(Build.VERSION.RELEASE).append(" (API ").append(Build.VERSION.SDK_INT).append(")\n");
        DisplayMetrics dm = getResources().getDisplayMetrics();
        sb.append("Display: ").append(dm.widthPixels).append("x").append(dm.heightPixels).append(" (").append(dm.densityDpi).append(" dpi)\n\n");

        sb.append("-- Memory & Storage --\n");
        long[] mem = readMemory();
        sb.append("RAM: ").append(StatsCapacity.memory(mem[0], mem[1])).append(" MB\n");
        File data = Environment.getDataDirectory();
        sb.append("Internal Storage: ").append(StatsCapacity.storage(data.getFreeSpace(), data.getTotalSpace())).append("\n\n");

        sb.append("-- ADB Hygiene --\n");
        boolean usbAdb = Settings.Global.getInt(getContentResolver(), Settings.Global.ADB_ENABLED, 0) == 1;
        sb.append("USB Debugging: ").append(usbAdb ? "ENABLED" : "DISABLED").append("\n");
        sb.append("Network ADB Port 5555: ").append(isPortOpen("127.0.0.1", 5555) ? "OPEN" : "CLOSED").append("\n\n");

        sb.append("-- Network --\n");
        sb.append("IP Address: ").append(getIpAddress()).append("\n\n");

        File outDir = getExternalFilesDir(null);
        if (outDir == null) outDir = getFilesDir();
        File reportFile = new File(outDir, "tv_manager_report.txt");

        try (FileWriter writer = new FileWriter(reportFile)) {
            writer.write(sb.toString());
            Toast.makeText(this, getString(R.string.report_saved, reportFile.getAbsolutePath()), Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(this, "Failed to save report: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // -------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------
    private void addHeader(String title) {
        TextView tv = new TextView(this);
        tv.setText(title);
        tv.setTextColor(0xFF00BCD4);
        tv.setTextSize(18);
        tv.setPadding(0, 16, 0, 8);
        llPageContainer.addView(tv);
    }

    private void addStatCard(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(15);
        tv.setBackgroundResource(R.drawable.card_bg);
        tv.setPadding(16, 12, 16, 12);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.bottomMargin = 8;
        tv.setLayoutParams(lp);
        llPageContainer.addView(tv);
    }

    private void addTextLine(String text) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextColor(0xFFCCCCCC);
        tv.setTextSize(13);
        tv.setPadding(0, 2, 0, 2);
        llPageContainer.addView(tv);
    }
}
