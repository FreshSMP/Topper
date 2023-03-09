package me.hsgamer.topper.spigot.storage;

import me.hsgamer.hscore.bukkit.config.BukkitConfig;
import me.hsgamer.topper.core.holder.DataHolder;
import me.hsgamer.topper.core.storage.DataStorage;
import me.hsgamer.topper.spigot.config.AutoSaveConfig;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

public class YamlStorageSupplier<T> implements Function<DataHolder<T>, DataStorage<T>> {
    private static String baseFolderPath = "top";
    private final JavaPlugin plugin;
    private final File baseFolder;
    private final Converter<T> converter;

    public YamlStorageSupplier(JavaPlugin plugin, Converter<T> converter) {
        this.plugin = plugin;
        baseFolder = new File(plugin.getDataFolder(), baseFolderPath);
        this.converter = converter;
    }

    public static void setBaseFolderPath(String baseFolderPath) {
        YamlStorageSupplier.baseFolderPath = baseFolderPath;
    }

    @Override
    public DataStorage<T> apply(DataHolder<T> holder) {
        return new DataStorage<T>(holder) {
            private final AutoSaveConfig config = new AutoSaveConfig(plugin, new BukkitConfig(new File(baseFolder, holder.getName() + ".yml")));

            @Override
            public CompletableFuture<Map<UUID, T>> load() {
                Map<String, Object> values = config.getValues(false);
                return CompletableFuture.supplyAsync(() -> {
                    Map<UUID, T> map = new HashMap<>();
                    values.forEach((uuid, value) -> {
                        T finalValue = converter.toValue(value);
                        if (finalValue != null) {
                            map.put(UUID.fromString(uuid), finalValue);
                        }
                    });
                    return map;
                });
            }

            @Override
            public CompletableFuture<Void> save(UUID uuid, T value, boolean urgent) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                BukkitRunnable runnable = new BukkitRunnable() {
                    @Override
                    public void run() {
                        config.set(uuid.toString(), converter.toRaw(value));
                        future.complete(null);
                    }
                };
                if (urgent) {
                    runnable.run();
                } else {
                    runnable.runTask(plugin);
                }
                return future;
            }

            @Override
            public CompletableFuture<Optional<T>> load(UUID uuid, boolean urgent) {
                CompletableFuture<Optional<T>> future = new CompletableFuture<>();
                BukkitRunnable runnable = new BukkitRunnable() {
                    @Override
                    public void run() {
                        Optional<T> optional = Optional.ofNullable(config.get(uuid.toString())).map(converter::toValue);
                        future.complete(optional);
                    }
                };
                if (urgent) {
                    runnable.run();
                } else {
                    runnable.runTask(plugin);
                }
                return future;
            }

            @Override
            public void onRegister() {
                config.setup();
            }

            @Override
            public void onUnregister() {
                config.finalSave();
            }
        };
    }

    public interface Converter<T> {
        T toValue(Object object);

        Object toRaw(T object);
    }
}
