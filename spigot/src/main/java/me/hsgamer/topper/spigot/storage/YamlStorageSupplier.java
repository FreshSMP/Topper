package me.hsgamer.topper.spigot.storage;

import io.github.projectunified.minelib.scheduler.global.GlobalScheduler;
import me.hsgamer.hscore.bukkit.config.BukkitConfig;
import me.hsgamer.hscore.config.PathString;
import me.hsgamer.topper.core.holder.DataHolder;
import me.hsgamer.topper.core.storage.DataStorage;
import me.hsgamer.topper.core.storage.DataStorageSupplier;
import me.hsgamer.topper.extra.storage.converter.FlatEntryConverter;
import me.hsgamer.topper.spigot.config.AutoSaveConfig;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class YamlStorageSupplier<T> implements DataStorageSupplier<UUID, T> {
    private final JavaPlugin plugin;
    private final File baseFolder;
    private final FlatEntryConverter<UUID, T> converter;

    public YamlStorageSupplier(JavaPlugin plugin, File baseFolder, FlatEntryConverter<UUID, T> converter) {
        this.plugin = plugin;
        this.baseFolder = baseFolder;
        this.converter = converter;
    }

    @Override
    public DataStorage<UUID, T> getStorage(DataHolder<UUID, T> holder) {
        return new DataStorage<UUID, T>(holder) {
            private final AutoSaveConfig config = new AutoSaveConfig(plugin, new BukkitConfig(new File(baseFolder, holder.getName() + ".yml")));

            @Override
            public CompletableFuture<Map<UUID, T>> load() {
                Map<PathString, Object> values = config.getValues(false);
                return CompletableFuture.supplyAsync(() -> {
                    Map<UUID, T> map = new HashMap<>();
                    values.forEach((path, value) -> {
                        T finalValue = converter.toValue(value);
                        if (finalValue != null) {
                            map.put(converter.toKey(path.getLastPath()), finalValue);
                        }
                    });
                    return map;
                });
            }

            @Override
            public CompletableFuture<Void> save(UUID uuid, T value, boolean urgent) {
                CompletableFuture<Void> future = new CompletableFuture<>();
                Runnable runnable = () -> {
                    config.set(new PathString(converter.toRawKey(uuid)), converter.toRawValue(value));
                    future.complete(null);
                };
                if (urgent) {
                    runnable.run();
                } else {
                    GlobalScheduler.get(plugin).run(runnable);
                }
                return future;
            }

            @Override
            public CompletableFuture<Optional<T>> load(UUID uuid, boolean urgent) {
                CompletableFuture<Optional<T>> future = new CompletableFuture<>();
                Runnable runnable = () -> {
                    Optional<T> optional = Optional.ofNullable(config.get(new PathString(converter.toRawKey(uuid)))).map(converter::toValue);
                    future.complete(optional);
                };
                if (urgent) {
                    runnable.run();
                } else {
                    GlobalScheduler.get(plugin).run(runnable);
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
}
