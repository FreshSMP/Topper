package me.hsgamer.topper.spigot.number;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;

public class NumberFormatter {
    private final Map<String, BiFunction<UUID, Number, String>> replacers = new HashMap<>();
    private String nullDisplayUuid = "---";
    private String nullDisplayName = "---";
    private String nullDisplayValue = "---";
    private String displayName = "";
    private String prefix = "";
    private String suffix = "";
    private int fractionDigits = -1;
    private char decimalSeparator = '.';
    private char groupSeparator = ',';
    private boolean showGroupSeparator = false;
    private DecimalFormat format;

    public NumberFormatter(Map<String, Object> map) {
        Optional.ofNullable(map.get("display-name")).ifPresent(s -> displayName = String.valueOf(s));
        Optional.ofNullable(map.get("prefix")).ifPresent(s -> prefix = String.valueOf(s));
        Optional.ofNullable(map.get("suffix")).ifPresent(s -> suffix = String.valueOf(s));
        Optional.ofNullable(map.get("fraction-digits")).ifPresent(s -> fractionDigits = Integer.parseInt(String.valueOf(s)));
        Optional.ofNullable(map.get("decimal-separator")).ifPresent(s -> decimalSeparator = String.valueOf(s).charAt(0));
        Optional.ofNullable(map.get("group-separator")).ifPresent(s -> groupSeparator = String.valueOf(s).charAt(0));
        Optional.ofNullable(map.get("show-group-separator")).ifPresent(s -> showGroupSeparator = Boolean.parseBoolean(String.valueOf(s)));
        Optional.ofNullable(map.get("null-display-uuid")).ifPresent(s -> nullDisplayUuid = String.valueOf(s));
        Optional.ofNullable(map.get("null-display-name")).ifPresent(s -> nullDisplayName = String.valueOf(s));
        Optional.ofNullable(map.get("null-display-value")).ifPresent(s -> nullDisplayValue = String.valueOf(s));
    }

    public NumberFormatter() {
        // EMPTY
    }

    public String getNullDisplayUuid() {
        return nullDisplayUuid;
    }

    public void setNullDisplayUuid(String nullDisplayUuid) {
        this.nullDisplayUuid = nullDisplayUuid;
    }

    public String getNullDisplayName() {
        return nullDisplayName;
    }

    public void setNullDisplayName(String nullDisplayName) {
        this.nullDisplayName = nullDisplayName;
    }

    public String getNullDisplayValue() {
        return nullDisplayValue;
    }

    public void setNullDisplayValue(String nullDisplayValue) {
        this.nullDisplayValue = nullDisplayValue;
    }

    public void addReplacer(String key, BiFunction<UUID, Number, String> replacer) {
        replacers.put(key, replacer);
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    public String getSuffix() {
        return suffix;
    }

    public void setSuffix(String suffix) {
        this.suffix = suffix;
    }

    public int getFractionDigits() {
        return fractionDigits;
    }

    public void setFractionDigits(int fractionDigits) {
        this.fractionDigits = fractionDigits;
    }

    public char getDecimalSeparator() {
        return decimalSeparator;
    }

    public void setDecimalSeparator(char decimalSeparator) {
        this.decimalSeparator = decimalSeparator;
    }

    public char getGroupSeparator() {
        return groupSeparator;
    }

    public void setGroupSeparator(char groupSeparator) {
        this.groupSeparator = groupSeparator;
    }

    public boolean isShowGroupSeparator() {
        return showGroupSeparator;
    }

    public void setShowGroupSeparator(boolean showGroupSeparator) {
        this.showGroupSeparator = showGroupSeparator;
    }

    private DecimalFormat getFormat() {
        if (format == null) {
            DecimalFormatSymbols symbols = new DecimalFormatSymbols();
            symbols.setDecimalSeparator(decimalSeparator);
            symbols.setGroupingSeparator(groupSeparator);

            format = new DecimalFormat();
            format.setRoundingMode(RoundingMode.HALF_EVEN);
            format.setGroupingUsed(showGroupSeparator);
            format.setMinimumFractionDigits(0);
            if (fractionDigits >= 0) {
                format.setMaximumFractionDigits(fractionDigits);
            }
            format.setDecimalFormatSymbols(symbols);
        }
        return format;
    }

    public String format(Number value) {
        return getFormat().format(value);
    }

    public String replace(String text, UUID uuid, Number value) {
        String replaced = text.replace("{prefix}", prefix)
                .replace("{suffix}", suffix)
                .replace("{uuid}", Optional.ofNullable(uuid).map(UUID::toString).orElse(nullDisplayUuid))
                .replace("{value}", Optional.ofNullable(value).map(this::format).orElse(nullDisplayValue))
                .replace("{name}", Optional.ofNullable(uuid).map(Bukkit::getOfflinePlayer).map(OfflinePlayer::getName).orElse(nullDisplayName))
                .replace("{value_raw}", Optional.ofNullable(value).map(String::valueOf).orElse(nullDisplayValue))
                .replace("{display_name}", displayName);
        for (Map.Entry<String, BiFunction<UUID, Number, String>> entry : replacers.entrySet()) {
            replaced = replaced.replace("{" + entry.getKey() + "}", entry.getValue().apply(uuid, value));
        }
        return replaced;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("prefix", prefix);
        map.put("suffix", suffix);
        map.put("fraction-digits", fractionDigits);
        map.put("decimal-separator", decimalSeparator);
        map.put("group-separator", groupSeparator);
        map.put("show-group-separator", showGroupSeparator);
        map.put("null-display-uuid", nullDisplayUuid);
        map.put("null-display-name", nullDisplayName);
        map.put("null-display-value", nullDisplayValue);
        return map;
    }
}
