package dev.theo.beaconpack.core;

/**
 * The two fuel decisions, pulled out of the ticker so they can be tested without a world.
 *
 * <p>Both are easy to get subtly wrong and expensive when they are: rounding the charge down makes
 * a cheap effect free forever, and accepting fuel the buffer cannot hold destroys the surplus.
 */
public final class FuelBudget {

    /**
     * What a tick costs.
     *
     * <p>Rounded up rather than down: a draw below one unit per tick would otherwise round to zero
     * and run indefinitely on an empty buffer.
     */
    public static int costFor(double perSecond, double seconds) {
        if (perSecond <= 0.0 || seconds <= 0.0) {
            return 0;
        }
        return (int) Math.ceil(perSecond * seconds);
    }

    /**
     * Whether an item may be burned into the buffer.
     *
     * <p>Only if it fits whole. Clamping the overflow away would silently destroy most of a
     * netherite ingot, and refusing instead is what gives the Capacity augment and the higher tiers
     * a purpose: they are what unlock the denser fuels.
     */
    public static boolean accepts(int fuel, int units, int capacity) {
        return units > 0 && fuel + units <= capacity;
    }

    private FuelBudget() {}
}
