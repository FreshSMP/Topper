package me.hsgamer.topper.core.agent.storage;

import me.hsgamer.topper.core.agent.TaskAgent;
import me.hsgamer.topper.core.entry.DataEntry;
import me.hsgamer.topper.core.flag.EntryTempFlag;
import me.hsgamer.topper.core.holder.DataHolder;
import me.hsgamer.topper.core.storage.DataStorage;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

public class StorageAgent<T> extends TaskAgent {
    public static final EntryTempFlag NEED_SAVING = new EntryTempFlag("needSaving");
    public static final EntryTempFlag IS_SAVING = new EntryTempFlag("isSaving");
    private final Queue<UUID> saveQueue = new ConcurrentLinkedQueue<>();
    private final List<Runnable> onLoadListeners = new ArrayList<>();
    private final Logger logger;
    private final DataHolder<T> holder;
    private final DataStorage<T> storage;
    private int maxEntryPerCall = 10;
    private boolean urgentSave = false;
    private boolean loadOnCreate = false;
    private boolean urgentLoad = true;

    public StorageAgent(Logger logger, DataStorage<T> storage) {
        this.logger = logger;
        this.holder = storage.getHolder();
        this.storage = storage;
    }

    private void save(DataEntry<T> entry) {
        if (entry.hasFlag(IS_SAVING)) return;
        if (!entry.hasFlag(NEED_SAVING)) return;
        entry.removeFlag(NEED_SAVING);
        entry.addFlag(IS_SAVING);
        storage.save(entry.getUuid(), entry.getValue(), urgentSave).whenComplete((result, throwable) -> entry.removeFlag(IS_SAVING));
    }

    private void load(DataEntry<T> entry) {
        storage.load(entry.getUuid(), urgentLoad).whenComplete((result, throwable) -> {
            if (throwable != null) {
                logger.log(Level.WARNING, throwable, () -> "Failed to load " + entry.getUuid());
            } else {
                result.ifPresent(entry::setValue);
            }
        });
    }

    public void loadIfExist(UUID uuid) {
        holder.getEntry(uuid).ifPresent(this::load);
    }

    @Override
    public void start() {
        holder.getCreateListenerManager().add(entry -> {
            saveQueue.add(entry.getUuid());
            if (loadOnCreate) {
                load(entry);
            }
        });
        holder.getRemoveListenerManager().add(entry -> {
            save(entry);
            saveQueue.remove(entry.getUuid());
        });
        holder.getUpdateListenerManager().add(entry -> entry.addFlag(NEED_SAVING));
        storage.onRegister();
        storage.load()
                .whenComplete((entries, throwable) -> {
                    if (throwable != null) {
                        logger.log(Level.SEVERE, "Failed to load top entries", throwable);
                    }
                    if (entries != null) {
                        entries.forEach((uuid, value) -> holder.getOrCreateEntry(uuid).setValue(value, false));
                    }
                    onLoadListeners.forEach(Runnable::run);
                });
        super.start();
    }

    @Override
    public void stop() {
        super.stop();
        storage.onUnregister();
    }

    @Override
    public void beforeStop() {
        urgentSave = true;
    }

    @Override
    protected Runnable getRunnable() {
        return () -> {
            List<UUID> list = new ArrayList<>();
            for (int i = 0; i < maxEntryPerCall; i++) {
                UUID uuid = saveQueue.poll();
                if (uuid == null) break;
                DataEntry<T> entry = holder.getOrCreateEntry(uuid);
                save(entry);
                list.add(uuid);
            }
            if (!list.isEmpty()) {
                saveQueue.addAll(list);
            }
        };
    }

    public void setMaxEntryPerCall(int maxEntryPerCall) {
        this.maxEntryPerCall = maxEntryPerCall;
    }

    public void addOnLoadListener(Runnable runnable) {
        onLoadListeners.add(runnable);
    }

    public void setUrgentLoad(boolean urgentLoad) {
        this.urgentLoad = urgentLoad;
    }

    public void setLoadOnCreate(boolean loadOnCreate) {
        this.loadOnCreate = loadOnCreate;
    }
}
