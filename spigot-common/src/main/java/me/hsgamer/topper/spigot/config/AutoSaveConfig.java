package me.hsgamer.topper.spigot.config;

import io.github.projectunified.minelib.scheduler.common.task.Task;
import io.github.projectunified.minelib.scheduler.global.GlobalScheduler;
import me.hsgamer.hscore.config.Config;
import me.hsgamer.hscore.config.DecorativeConfig;
import me.hsgamer.hscore.config.PathString;
import org.bukkit.plugin.Plugin;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class AutoSaveConfig extends DecorativeConfig {
    private final Plugin plugin;
    private final AtomicBoolean isSaving = new AtomicBoolean(false);
    private final AtomicReference<Task> currentSaveTask = new AtomicReference<>();

    public AutoSaveConfig(Plugin plugin, Config config) {
        super(config);
        this.plugin = plugin;
    }

    @Override
    public void set(PathString path, Object value) {
        super.set(path, value);
        if (!isSaving.get()) {
            isSaving.set(true);
            Task task = GlobalScheduler.get(plugin).runLater(() -> {
                save();
                isSaving.set(false);
            }, 40L);
            currentSaveTask.set(task);
        }
    }

    public void finalSave() {
        Optional.ofNullable(currentSaveTask.getAndSet(null)).ifPresent(task -> {
            try {
                task.cancel();
            } catch (Exception ignored) {
                // IGNORED
            }
        });
        if (isSaving.get()) return;
        save();
    }
}
