package dev.turningseasons.mixin;

import dev.turningseasons.SeasonClimate;
import dev.turningseasons.SeasonColors;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Temperate biomes get colder in winter (so vanilla snows and freezes there) and change colour. */
@Mixin(Biome.class)
abstract class BiomeMixin implements SeasonClimate.SeasonalBiome {
    @Unique
    private boolean turningseasons$temperate;

    @Override
    public boolean turningseasons$isTemperate() {
        return turningseasons$temperate;
    }

    @Override
    public void turningseasons$setTemperate(boolean temperate) {
        turningseasons$temperate = temperate;
    }

    // after vanilla's per-thread temperature cache, so the cache keeps natural values
    @Inject(method = "getTemperature", at = @At("RETURN"), cancellable = true)
    private void turningseasons$seasonalTemperature(BlockPos pos, int seaLevel, CallbackInfoReturnable<Float> cir) {
        if (turningseasons$temperate) {
            float shift = SeasonClimate.temperatureShift();
            if (shift != 0) cir.setReturnValue(cir.getReturnValueF() + shift);
        }
    }

    @Inject(method = "getGrassColor", at = @At("RETURN"), cancellable = true)
    private void turningseasons$seasonalGrass(double x, double z, CallbackInfoReturnable<Integer> cir) {
        if (turningseasons$temperate && SeasonClimate.colorPosition() >= 0) {
            cir.setReturnValue(SeasonColors.apply(cir.getReturnValueI(), SeasonColors.Kind.GRASS, SeasonClimate.colorPosition()));
        }
    }

    @Inject(method = "getFoliageColor", at = @At("RETURN"), cancellable = true)
    private void turningseasons$seasonalFoliage(CallbackInfoReturnable<Integer> cir) {
        if (turningseasons$temperate && SeasonClimate.colorPosition() >= 0) {
            cir.setReturnValue(SeasonColors.apply(cir.getReturnValueI(), SeasonColors.Kind.FOLIAGE, SeasonClimate.colorPosition()));
        }
    }
}
