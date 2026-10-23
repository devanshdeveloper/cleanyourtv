package io.github.devanshdeveloper.smarttubecard;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SmartTubeFinderTest {

    private static final String OWN_PKG = "io.github.devanshdeveloper.smarttubecard";

    @Test
    public void testCandidateMatching() {
        assertTrue(SmartTubeFinder.isCandidate("SmartTube", null, "com.teamsmart.videomanager.tv", OWN_PKG));
        assertTrue(SmartTubeFinder.isCandidate("SmartTube Next", null, "com.liskovsoft.smarttubetv.beta", OWN_PKG));
        assertTrue(SmartTubeFinder.isCandidate("smarttube", "Main", "com.liskovsoft.videomanager", OWN_PKG));
        assertTrue(SmartTubeFinder.isCandidate(null, "SmartTube TV", "com.liskovsoft.smarttubetv", OWN_PKG));
    }

    @Test
    public void testRejectsOwnPackage() {
        assertFalse(SmartTubeFinder.isCandidate("SmartTube Link", "MainActivity", OWN_PKG, OWN_PKG));
    }

    @Test
    public void testRejectsUnrelatedApps() {
        assertFalse(SmartTubeFinder.isCandidate("YouTube", "WatchActivity", "com.google.android.youtube.tv", OWN_PKG));
        assertFalse(SmartTubeFinder.isCandidate("Netflix", "MainActivity", "com.netflix.ninja", OWN_PKG));
        assertFalse(SmartTubeFinder.isCandidate(null, null, "com.example.app", OWN_PKG));
        assertFalse(SmartTubeFinder.isCandidate(null, null, null, OWN_PKG));
    }
}
