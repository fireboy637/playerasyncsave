package com.fireboy637.playerasyncsave.mixin;

import com.fireboy637.playerasyncsave.PlayerAsyncSave;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.PlayerDataStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.IOException;
import java.nio.file.Path;

@Mixin(PlayerDataStorage.class)
public class PlayerDataStorageMixin {
    @Inject(method = "save",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/nbt/NbtIo;writeCompressed(Lnet/minecraft/nbt/CompoundTag;Ljava/nio/file/Path;)V",
                    shift = At.Shift.BEFORE
            ),
            cancellable = true)
    private void pas$submitWriteTask(Player player, CallbackInfo ci,
                                 @Local(name = "playerDirPath") Path playerDirPath,
                                 @Local(name = "tmpFile") Path tmpFile,
                                 @Local(name = "dataToStore") CompoundTag dataToStore) {
        var uuid = player.getStringUUID();
        var name = player.getPlainTextName();
        PlayerAsyncSave.submitTask(() ->
        {
            try {
                NbtIo.writeCompressed(dataToStore, tmpFile);
                Path realFile = playerDirPath.resolve(uuid + ".dat");
                Path oldFile = playerDirPath.resolve(uuid + ".dat_old");
                Util.safeReplaceFile(realFile, tmpFile, oldFile);
            } catch (IOException e) {
                PlayerAsyncSave.LOGGER.warn("Failed to save player data for {}", name, e);
            }
        });
        ci.cancel();
    }
}
