package dev.turningseasons.mixin;

import dev.turningseasons.SeasonalGround;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Spring thaw and bloom ride along with vanilla's random snow/ice check on a column. */
@Mixin(ServerLevel.class)
abstract class ServerLevelMixin {
    @Inject(method = "tickPrecipitation", at = @At("TAIL"))
    private void turningseasons$thawAndBloom(BlockPos pos, CallbackInfo ci) {
        SeasonalGround.afterPrecipitation((ServerLevel) (Object) this, pos);
    }
}
