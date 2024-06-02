package me.hsgamer.topper.spigot.block;

import org.bukkit.Bukkit;
import org.bukkit.Location;

public class BlockEntry {
    public final Location location;
    public final String holderName;
    public final int index;

    public BlockEntry(Location location, String holderName, int index) {
        this.location = location;
        this.holderName = holderName;
        this.index = index;
    }

    public static BlockEntry deserialize(String rawValue) {
        String[] split = rawValue.split(",");
        Location location = new Location(
                Bukkit.getWorld(split[0]),
                Double.parseDouble(split[1]),
                Double.parseDouble(split[2]),
                Double.parseDouble(split[3])
        );
        String topHolderName = split[4];
        int index = Integer.parseInt(split[5]);
        return new BlockEntry(location, topHolderName, index);
    }

    public String serialize() {
        return String.join(",",
                location.getWorld().getName(),
                Double.toString(location.getX()),
                Double.toString(location.getY()),
                Double.toString(location.getZ()),
                holderName,
                String.valueOf(index)
        );
    }
}
