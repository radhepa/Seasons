package dev.turningseasons.mixin;

import dev.turningseasons.CropGrowth;
import dev.turningseasons.api.SeasonTags;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Seasonal crop growth: random ticks on tagged crops are skipped or doubled by season. */
@Mixin(BlockBehaviour.BlockStateBase.class)
abstract class BlockStateBaseMixin {
    @Inject(method = "randomTick", at = @At("HEAD"), cancellable = true)
    private void turningseasons$seasonalGrowth(ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        BlockState self = (BlockState) (Object) this;
        if (self.is(SeasonTags.SEASONAL_CROPS) && CropGrowth.onRandomTick(self, level, pos, random)) ci.cancel();
    }
}
