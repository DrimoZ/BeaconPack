package dev.drimoz.portablebeacons.core;

/**
 * Runtime as a player reads it.
 *
 * <p>One place rather than three. The screen, the item tooltip and the JEI fuel category all print
 * the same figure, and two of them had already drifted into separate copies of this method - which
 * is exactly how "4 h" in one panel becomes "3 h" in the next.
 *
 * <p>Deliberately coarse: the question behind the number is "will this last me the trip", so one
 * significant unit answers it and a precise "3 h 47 min 12 s" does not.
 */
public final class Durations {

    public static String format(int seconds) {
        if (seconds >= 3600) {
            return (seconds / 3600) + " h";
        }
        if (seconds >= 60) {
            return (seconds / 60) + " min";
        }
        return seconds + " s";
    }

    private Durations() {}
}
