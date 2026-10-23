package com.gathin.tvhome;

/** A revision token makes identical pushes and resets distinct from ordinary app upgrades. */
final class ThemeChoiceRules {
    static boolean sameChoices(java.util.Map<String, ?> before, java.util.Map<String, ?> after) {
        return before != null && before.equals(after);
    }

    static boolean clear(String seen, String signature, String revision) {
        return seen == null ? !"absent".equals(revision) : !seen.equals(signature);
    }
}
