package io.github.devanshdeveloper.livechannels;

import java.util.Objects;

/**
 * A single live source: TV input (HDMI/AV), TV Tuner, or installed broadcaster/IPTV app.
 */
public final class SourceEntry {
    public static final String TYPE_INPUT = "input";
    public static final String TYPE_TUNER = "tuner";
    public static final String TYPE_APP = "app";

    private final String id;
    private String name;
    private final String type;
    private final String target;

    public SourceEntry(String id, String name, String type, String target) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.target = target;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public String getTarget() {
        return target;
    }

    /** Serializes this entry to a single tab-separated line. */
    public String encode() {
        // Replace newlines/tabs in user-entered name to avoid breaking delimiters
        String safeName = name.replace("\t", " ").replace("\n", " ");
        return id + "\t" + type + "\t" + safeName + "\t" + target;
    }

    /** Parses one tab-separated line into a SourceEntry, or null if malformed. */
    public static SourceEntry decode(String line) {
        if (line == null) return null;
        String[] parts = line.split("\t", 4);
        if (parts.length < 4) return null;
        return new SourceEntry(parts[0], parts[2], parts[1], parts[3]);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SourceEntry)) return false;
        SourceEntry that = (SourceEntry) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
