package com.fireboy637.playerasyncsave;

import com.mojang.logging.LogUtils;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PlayerAsyncSave {
	public static final Logger LOGGER = LogUtils.getLogger();
	public static final int THREADS = Integer.getInteger("fireboy637.pps.threads", Math.clamp(Runtime.getRuntime().availableProcessors() - 2, 4, 8));

	private static final AtomicInteger ID = new AtomicInteger(0);
	private static final ExecutorService POOL = Executors.newFixedThreadPool(THREADS, r -> {
		Thread t = new Thread(r, "PlayerAsyncSave-Worker-" + ID.getAndIncrement());
		t.setDaemon(true);
		return t;
	});
    private static final ConcurrentHashMap<String, Future<?>> IN_PROGRESS = new ConcurrentHashMap<>();

	public static void submitTask(Player player, Path playerDirPath, Path tmpFile, CompoundTag dataToStore) {
		var uuid = player.getStringUUID();
		Future<?> task = IN_PROGRESS.get(uuid);
		if (task != null) {
            try {
                task.get();
            } catch (ExecutionException _) { }
			catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }

		var name = player.getPlainTextName();
		IN_PROGRESS.put(uuid, POOL.submit(() -> {
			try {
				NbtIo.writeCompressed(dataToStore, tmpFile);
				Path realFile = playerDirPath.resolve(uuid + ".dat");
				Path oldFile = playerDirPath.resolve(uuid + ".dat_old");
				Util.safeReplaceFile(realFile, tmpFile, oldFile);
			} catch (Exception e) {
				PlayerAsyncSave.LOGGER.warn("Failed to save player data for {}", name, e);
			} finally { IN_PROGRESS.remove(uuid); }
		}));
	}

	public static void ensureCompleted(String msg) {
		for (var task : IN_PROGRESS.values()) {
			try {
                task.get();
            } catch (ExecutionException _) { }
			catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        }
		IN_PROGRESS.clear();
		LOGGER.info("[{}] Ensured that all previous tasks completed!", msg);
	}
}
