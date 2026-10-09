package dev.turningseasons.mixin;

import dev.turningseasons.Calendar;
import dev.turningseasons.SeasonalSpawns;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings.SpawnerData;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Seasonal spawn weights (data/&lt;ns&gt;/seasonal_spawns) applied to the list natural spawning picks from. */
@Mixin(NaturalSpawner.class)
abstract class NaturalSpawnerMixin {
    @Inject(method = "mobsAt", at = @At("RETURN"), cancellable = true)
    private static void turningseasons$seasonalWeights(ServerLevel level, StructureManager structures, ChunkGenerator generator,
            MobCategory category, BlockPos pos, CallbackInfoReturnable<WeightedList<SpawnerData>> cir) {
        if (Calendar.isSeasonal(level)) {
            cir.setReturnValue(SeasonalSpawns.reweight(cir.getReturnValue(), Calendar.server().date().season()));
        }
    }
}
