package com.fireboy637.playerasyncsave;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;

import java.util.concurrent.*;

public class PlayerAsyncSave implements ModInitializer {
	public static final Logger LOGGER = LogUtils.getLogger();
	public static final int THREADS = Integer.getInteger("fireboy637.pps.threads", Math.clamp(Runtime.getRuntime().availableProcessors(), 4, 8));

	private static final ExecutorService POOL = Executors.newFixedThreadPool(THREADS, r -> {
		Thread t = new Thread(r, "PlayerAsyncSave-Worker");
		t.setDaemon(true);
		return t;
	});
	private static final ConcurrentLinkedQueue<Future<?>> ALL_TASKS = new ConcurrentLinkedQueue<>();

    public static void submitTask(Runnable task) {
		ALL_TASKS.add(POOL.submit(task));
	}

	public static void ensureCompleted() {
		Future<?> task;
		while ((task = ALL_TASKS.poll()) != null) {
			try { task.get(); }
			catch (InterruptedException e) { Thread.currentThread().interrupt(); }
			catch (ExecutionException e) { LOGGER.error("Saving player task failed!", e.getCause());}
		}
		LOGGER.warn("Ensured that all tasks completed!");
	}

	@Override
	public void onInitialize() {}
}
