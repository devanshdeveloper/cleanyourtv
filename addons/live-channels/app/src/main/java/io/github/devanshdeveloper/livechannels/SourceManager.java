package io.github.devanshdeveloper.livechannels;

import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Manages sources, ordering, and preferences for Live Channels.
 */
public final class SourceManager {
    public static final String PREF_SOURCES = "sources_data";
    public static final String PREF_LAST_OPENED_ID = "last_opened_id";
    public static final String PREF_OPEN_ON_LAUNCH = "open_on_launch";

    private final List<SourceEntry> sources = new ArrayList<>();
    private String lastOpenedId = null;
    private boolean openOnLaunch = false;

    public List<SourceEntry> getSources() {
        return Collections.unmodifiableList(sources);
    }

    public void addSource(SourceEntry entry) {
        if (entry != null && !sources.contains(entry)) {
            sources.add(entry);
        }
    }

    public boolean removeSource(String id) {
        if (id == null) return false;
        boolean removed = sources.removeIf(s -> s.getId().equals(id));
        if (id.equals(lastOpenedId)) {
            lastOpenedId = sources.isEmpty() ? null : sources.get(0).getId();
        }
        return removed;
    }

    public boolean renameSource(String id, String newName) {
        if (id == null || newName == null || newName.trim().isEmpty()) return false;
        for (SourceEntry s : sources) {
            if (s.getId().equals(id)) {
                s.setName(newName.trim());
                return true;
            }
        }
        return false;
    }

    public boolean moveUp(int index) {
        if (index > 0 && index < sources.size()) {
            Collections.swap(sources, index, index - 1);
            return true;
        }
        return false;
    }

    public boolean moveDown(int index) {
        if (index >= 0 && index < sources.size() - 1) {
            Collections.swap(sources, index, index + 1);
            return true;
        }
        return false;
    }

    public SourceEntry findById(String id) {
        if (id == null) return null;
        for (SourceEntry s : sources) {
            if (s.getId().equals(id)) return s;
        }
        return null;
    }

    public String getLastOpenedId() {
        return lastOpenedId;
    }

    public void setLastOpenedId(String id) {
        this.lastOpenedId = id;
    }

    public SourceEntry getLastOpenedSource() {
        if (lastOpenedId != null) {
            SourceEntry found = findById(lastOpenedId);
            if (found != null) return found;
        }
        return sources.isEmpty() ? null : sources.get(0);
    }

    public boolean isOpenOnLaunch() {
        return openOnLaunch;
    }

    public void setOpenOnLaunch(boolean openOnLaunch) {
        this.openOnLaunch = openOnLaunch;
    }

    public String encodeSources() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < sources.size(); i++) {
            if (i > 0) sb.append("\n");
            sb.append(sources.get(i).encode());
        }
        return sb.toString();
    }

    public void decodeSources(String data) {
        sources.clear();
        if (data == null || data.trim().isEmpty()) return;
        String[] lines = data.split("\n");
        for (String line : lines) {
            SourceEntry entry = SourceEntry.decode(line.trim());
            if (entry != null) {
                sources.add(entry);
            }
        }
    }

    public void load(SharedPreferences prefs) {
        decodeSources(prefs.getString(PREF_SOURCES, null));
        lastOpenedId = prefs.getString(PREF_LAST_OPENED_ID, null);
        openOnLaunch = prefs.getBoolean(PREF_OPEN_ON_LAUNCH, false);
    }

    public void save(SharedPreferences prefs) {
        prefs.edit()
                .putString(PREF_SOURCES, encodeSources())
                .putString(PREF_LAST_OPENED_ID, lastOpenedId)
                .putBoolean(PREF_OPEN_ON_LAUNCH, openOnLaunch)
                .apply();
    }
}
