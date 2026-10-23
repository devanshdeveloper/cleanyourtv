package io.github.devanshdeveloper.clockalarm;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public final class AlarmStore {

    public static final int MAX_ALARMS = 50;
    private static final String FILE_NAME = "alarms.txt";
    private static final String TMP_FILE_NAME = "alarms.txt.tmp";

    private AlarmStore() {}

    public static synchronized List<Alarm> load(Context context) {
        List<Alarm> list = new ArrayList<>();
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) return list;

        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                Alarm a = Alarm.deserialize(line);
                if (a != null) {
                    list.add(a);
                    if (list.size() >= MAX_ALARMS) break;
                }
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public static synchronized boolean save(Context context, List<Alarm> alarms) {
        File dir = context.getFilesDir();
        File tmp = new File(dir, TMP_FILE_NAME);
        File target = new File(dir, FILE_NAME);

        try {
            try (FileOutputStream fos = new FileOutputStream(tmp);
                 PrintWriter pw = new PrintWriter(new OutputStreamWriter(fos, StandardCharsets.UTF_8))) {
                int count = 0;
                for (Alarm a : alarms) {
                    if (a != null) {
                        pw.println(a.serialize());
                        if (++count >= MAX_ALARMS) break;
                    }
                }
                pw.flush();
                fos.getFD().sync();
            }
            return tmp.renameTo(target);
        } catch (Exception e) {
            tmp.delete();
            return false;
        }
    }

    public static synchronized Alarm getById(Context context, int id) {
        for (Alarm a : load(context)) {
            if (a.id == id) return a;
        }
        return null;
    }

    public static synchronized boolean saveOrUpdate(Context context, Alarm alarm) {
        List<Alarm> list = load(context);
        boolean replaced = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id == alarm.id) {
                list.set(i, alarm);
                replaced = true;
                break;
            }
        }
        if (!replaced) {
            if (list.size() >= MAX_ALARMS) return false;
            list.add(alarm);
        }
        return save(context, list);
    }

    public static synchronized boolean delete(Context context, int id) {
        List<Alarm> list = load(context);
        boolean removed = list.removeIf(a -> a.id == id);
        if (removed) {
            save(context, list);
        }
        return removed;
    }
}
