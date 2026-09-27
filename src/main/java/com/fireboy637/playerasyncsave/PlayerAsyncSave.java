package com.fireboy637.playerasyncsave;

import com.mojang.logging.LogUtils;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class PlayerAsyncSave implements ModInitializer {
    public static @Nullable MinecraftServer server;
    public static final Logger LOGGER = LogUtils.getLogger();
    public static final int THREADS = Integer.getInteger("fireboy637.pps.threads", Math.clamp(Runtime.getRuntime().availableProcessors() - 2, 4, 8));

    private static final AtomicInteger ID = new AtomicInteger(0);
    private static final ExecutorService POOL = Executors.newFixedThreadPool(THREADS, r -> {
        Thread t = new Thread(r, "PlayerAsyncSave-Worker-" + ID.getAndIncrement());
        t.setDaemon(true);
        return t;
    });
    private static final ConcurrentHashMap<String, Future<?>> IN_PROGRESS = new ConcurrentHashMap<>();


    @ApiStatus.Internal // 敢从模组外/线程外调用这个的肯定是十八码了
    public static void submitTask(Player player, Path playerDirPath, Path tmpFile, CompoundTag dataToStore) {
        if (server != null && Thread.currentThread() != server.getRunningThread()) {
            var e = new RuntimeException("PlayerAsyncSave: outside Server Thread");
            LOGGER.error("PlayerAsyncSave: task submit outside Server Thread, current thread is {}", Thread.currentThread().getName(), e);
            throw e;
        }

        var uuid = player.getStringUUID();
        var name = player.getPlainTextName();
        var task = IN_PROGRESS.get(uuid);
        if (task != null) {
            try {
                task.get(); // 防旧任务覆盖
            } catch (ExecutionException _) { /* 处理过了，不再接受 */ }
			catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOGGER.warn("Interrupted while waiting for previous save of {}!", name, e);
                return;
            }
        }
        var future = POOL.submit(() -> {
            try {
                NbtIo.writeCompressed(dataToStore, tmpFile);
                Path realFile = playerDirPath.resolve(uuid + ".dat");
                Path oldFile = playerDirPath.resolve(uuid + ".dat_old");
                Util.safeReplaceFile(realFile, tmpFile, oldFile);
            } catch (Exception e) {
                PlayerAsyncSave.LOGGER.warn("Failed to save player data for {}!", name, e);
            } finally {
                IN_PROGRESS.remove(uuid); // 本来就只有一个线程可以提交，现在肯定没有旧任务
            }
        });

        IN_PROGRESS.put(uuid, future);
        if (future.isDone()) {
            IN_PROGRESS.remove(uuid, future);
        }
    }

    // 兜底，怕你需要等保存完成
    public static void ensureTaskCompleted(String msg) {
        for (; ; ) {
            var tasks = List.copyOf(IN_PROGRESS.values());
            if (tasks.isEmpty()) break; // 绝大多数时候都应该是空的
            for (var task : tasks) {
                try {
                    task.get();
                } catch (ExecutionException _) { /* 处理过了，不再接受 */ }
				catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    LOGGER.warn("Interrupted while checking saving tasks!", e);
                    return;
                }
            }
        }
        LOGGER.debug("[{}] Ensured that all previous tasks cleared!", msg);
    }

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> PlayerAsyncSave.server = server);
        ServerLifecycleEvents.SERVER_STOPPED.register(_ -> PlayerAsyncSave.server = null);
    }
}
