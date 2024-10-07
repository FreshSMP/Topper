package me.hsgamer.topper.spigot.plugin.holder.display;

import me.hsgamer.topper.spigot.plugin.holder.NumberTopHolder;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ValueDisplay {
    private static final Pattern VALUE_PLACEHOLDER_PATTERN = Pattern.compile("\\{value(?:_(.*))?}");

    private final NumberTopHolder holder;
    private final String line;
    private final String displayNullName;
    private final String displayNullUuid;
    private final String displayNullValue;

    public ValueDisplay(NumberTopHolder holder, Map<String, Object> map) {
        this.holder = holder;
        this.line = Optional.ofNullable(map.get("line"))
                .map(Object::toString)
                .orElse("&7[&b{index}&7] &b{name} &7- &b{value}");
        this.displayNullName = Optional.ofNullable(map.get("null-name"))
                .map(Object::toString)
                .orElse("---");
        this.displayNullUuid = Optional.ofNullable(map.get("null-uuid"))
                .map(Object::toString)
                .orElse("---");
        this.displayNullValue = Optional.ofNullable(map.get("null-value"))
                .map(Object::toString)
                .orElse("---");
    }

    public String getDisplayUuid(@Nullable UUID uuid) {
        return uuid != null ? uuid.toString() : displayNullUuid;
    }

    public String getDisplayName(@Nullable UUID uuid) {
        return Optional.ofNullable(uuid)
                .map(Bukkit::getOfflinePlayer)
                .map(OfflinePlayer::getName)
                .orElse(displayNullName);
    }

    public String getDisplayValue(@Nullable Double value, @Nullable String formatType) {
        if (value == null) {
            return displayNullValue;
        }

        if (formatType == null) {
            DecimalFormat decimalFormat = new DecimalFormat("#.##");
            return decimalFormat.format(value);
        }

        if (formatType.equals("raw")) {
            return String.valueOf(value);
        }

        try {
            DecimalFormat decimalFormat = new DecimalFormat(formatType);
            return decimalFormat.format(value);
        } catch (IllegalArgumentException e) {
            return "INVALID_FORMAT";
        }
    }

    public String getDisplayLine(int index /* 1-based */) {
        Map.Entry<UUID, Double> dataSnapshot = holder.getSnapshotAgent().getSnapshotByIndex(index - 1).orElse(null);
        String line = this.line
                .replace("{index}", String.valueOf(index))
                .replace("{uuid}", getDisplayUuid(dataSnapshot == null ? null : dataSnapshot.getKey()))
                .replace("{name}", getDisplayName(dataSnapshot == null ? null : dataSnapshot.getKey()));

        Double value = dataSnapshot == null ? null : dataSnapshot.getValue();
        Matcher matcher = VALUE_PLACEHOLDER_PATTERN.matcher(line);
        while (matcher.find()) {
            String formatType = matcher.group(1);
            line = line.replace(matcher.group(), getDisplayValue(value, formatType));
        }

        return line;
    }
}
