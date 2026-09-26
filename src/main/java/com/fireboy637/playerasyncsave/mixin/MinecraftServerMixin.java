package com.fireboy637.playerasyncsave.mixin;

import com.fireboy637.playerasyncsave.PlayerAsyncSave;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class MinecraftServerMixin {
    @Inject(method = "stopServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;saveAll()V", shift = At.Shift.AFTER))
    private void pas$ensureCompleted(CallbackInfo ci) {
        PlayerAsyncSave.ensureCompleted();
    }
}
