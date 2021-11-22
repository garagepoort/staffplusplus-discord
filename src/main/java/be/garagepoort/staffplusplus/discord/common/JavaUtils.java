package be.garagepoort.staffplusplus.discord.common;

import net.shortninja.staffplusplus.ILocation;
import org.apache.commons.lang.WordUtils;
import org.bukkit.Location;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author Shortninja, DarkSeraphim, ...
 */

public class JavaUtils {

    private static final List<TimeUnit> timeUnits = Arrays.asList(TimeUnit.DAYS, TimeUnit.HOURS, TimeUnit.MINUTES, TimeUnit.SECONDS);

    /**
     * "Serializes" the Location with simple string concatenation.
     *
     * @param location The Location to serialize.
     * @return String in the format of "x, y, z".
     */
    public static String serializeLocation(Location location) {
        return location.getBlockX() + ", " + location.getBlockY() + ", " + location.getBlockZ();
    }
    /**
     * "Serializes" the Location with simple string concatenation.
     *
     * @param location The Location to serialize.
     * @return String in the format of "x, y, z".
     */
    public static String serializeLocation(ILocation location) {
        return location.getX() + ", " + location.getY() + ", " + location.getZ();
    }

    public static String toHumanReadableDuration(final long millis) {
        if (millis <= 0) {
            return "None";
        }
        final StringBuilder builder = new StringBuilder();
        long acc = millis;
        int count = 0;
        for (final TimeUnit timeUnit : timeUnits) {
            final long convert = timeUnit.convert(acc, TimeUnit.MILLISECONDS);
            if (convert > 0) {
                builder.append(convert).append(' ').append(WordUtils.capitalizeFully(timeUnit.name())).append(", ");
                acc -= TimeUnit.MILLISECONDS.convert(convert, timeUnit);
                count++;
            }
            if(count >= 2) {
                // only show 2 time sections
                break;
            }
        }
        if (builder.length() == 0) {
            return "None";
        }
        return builder.substring(0, builder.length() - 2);
    }
}