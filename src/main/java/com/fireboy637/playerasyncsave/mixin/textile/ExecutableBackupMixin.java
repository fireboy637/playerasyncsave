package com.fireboy637.playerasyncsave.mixin.textile;

import com.fireboy637.playerasyncsave.PlayerAsyncSave;
import net.szum123321.textile_backup.core.create.ExecutableBackup;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ExecutableBackup.class)
public class ExecutableBackupMixin {
    @Inject(method = "lambda$call$0", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;saveEverything(ZZZ)Z", shift = At.Shift.AFTER))
    private void pas$ensureCompleted(CallbackInfo ci) {
        PlayerAsyncSave.ensureCompleted("Backup");
    }
}
