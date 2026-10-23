package io.github.devanshdeveloper.livechannels;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class SourceManagerTest {
    private SourceManager manager;

    @Before
    public void setUp() {
        manager = new SourceManager();
    }

    @Test
    public void testAddAndRemoveSources() {
        SourceEntry s1 = new SourceEntry("1", "HDMI 1", SourceEntry.TYPE_INPUT, "HW1");
        SourceEntry s2 = new SourceEntry("2", "TV Tuner", SourceEntry.TYPE_TUNER, "com.example.tv");

        manager.addSource(s1);
        manager.addSource(s2);

        assertEquals(2, manager.getSources().size());
        assertEquals("HDMI 1", manager.getSources().get(0).getName());

        assertTrue(manager.removeSource("1"));
        assertEquals(1, manager.getSources().size());
        assertEquals("TV Tuner", manager.getSources().get(0).getName());
        assertFalse(manager.removeSource("non-existent"));
    }

    @Test
    public void testRenameSource() {
        SourceEntry s1 = new SourceEntry("1", "HDMI 1", SourceEntry.TYPE_INPUT, "HW1");
        manager.addSource(s1);

        assertTrue(manager.renameSource("1", "PlayStation 5"));
        assertEquals("PlayStation 5", manager.getSources().get(0).getName());
        assertFalse(manager.renameSource("1", "   "));
        assertFalse(manager.renameSource("unknown", "Xbox"));
    }

    @Test
    public void testReordering() {
        SourceEntry s1 = new SourceEntry("1", "S1", SourceEntry.TYPE_INPUT, "T1");
        SourceEntry s2 = new SourceEntry("2", "S2", SourceEntry.TYPE_INPUT, "T2");
        SourceEntry s3 = new SourceEntry("3", "S3", SourceEntry.TYPE_INPUT, "T3");

        manager.addSource(s1);
        manager.addSource(s2);
        manager.addSource(s3);

        assertFalse(manager.moveUp(0)); // Cannot move first item up
        assertTrue(manager.moveUp(1));  // Move s2 up to index 0
        assertEquals("S2", manager.getSources().get(0).getName());
        assertEquals("S1", manager.getSources().get(1).getName());

        assertFalse(manager.moveDown(2)); // Cannot move last item down
        assertTrue(manager.moveDown(0));  // Move s2 down to index 1
        assertEquals("S1", manager.getSources().get(0).getName());
        assertEquals("S2", manager.getSources().get(1).getName());
    }

    @Test
    public void testEncodeDecodeRoundtrip() {
        SourceEntry s1 = new SourceEntry("id1", "HDMI 1", SourceEntry.TYPE_INPUT, "HW1");
        SourceEntry s2 = new SourceEntry("id2", "IPTV App", SourceEntry.TYPE_APP, "com.iptv.app");

        manager.addSource(s1);
        manager.addSource(s2);

        String encoded = manager.encodeSources();
        assertNotNull(encoded);

        SourceManager decodedManager = new SourceManager();
        decodedManager.decodeSources(encoded);

        List<SourceEntry> result = decodedManager.getSources();
        assertEquals(2, result.size());
        assertEquals("id1", result.get(0).getId());
        assertEquals("HDMI 1", result.get(0).getName());
        assertEquals(SourceEntry.TYPE_INPUT, result.get(0).getType());
        assertEquals("HW1", result.get(0).getTarget());

        assertEquals("id2", result.get(1).getId());
        assertEquals("IPTV App", result.get(1).getName());
        assertEquals(SourceEntry.TYPE_APP, result.get(1).getType());
        assertEquals("com.iptv.app", result.get(1).getTarget());
    }

    @Test
    public void testLastOpenedAndOpenOnLaunch() {
        SourceEntry s1 = new SourceEntry("1", "HDMI 1", SourceEntry.TYPE_INPUT, "HW1");
        SourceEntry s2 = new SourceEntry("2", "HDMI 2", SourceEntry.TYPE_INPUT, "HW2");
        manager.addSource(s1);
        manager.addSource(s2);

        assertEquals("HDMI 1", manager.getLastOpenedSource().getName()); // Default to first

        manager.setLastOpenedId("2");
        assertEquals("HDMI 2", manager.getLastOpenedSource().getName());

        manager.removeSource("2");
        assertEquals("HDMI 1", manager.getLastOpenedSource().getName()); // Falls back to first remaining

        assertFalse(manager.isOpenOnLaunch());
        manager.setOpenOnLaunch(true);
        assertTrue(manager.isOpenOnLaunch());
    }
}
