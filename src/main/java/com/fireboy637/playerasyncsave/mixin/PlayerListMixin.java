package com.fireboy637.playerasyncsave.mixin;

import com.fireboy637.playerasyncsave.PlayerAsyncSave;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerList.class)
public class PlayerListMixin {
    @Inject(method = "saveAll", at = @At("HEAD"))
    private void pas$ensureCompleted(CallbackInfo ci) {
        PlayerAsyncSave.ensureTaskCompleted("Before next save");
    }
}
