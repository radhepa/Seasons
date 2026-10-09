package dev.turningseasons.mixin;

import dev.turningseasons.Calendar;
import dev.turningseasons.SeasonConfig;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.animal.Animal;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Seasonal breeding: parents rest a shorter time in spring and a longer time in winter. */
@Mixin(Animal.class)
abstract class AnimalMixin {
    /** Vanilla's breeding cooldown in ticks. */
    private static final int VANILLA_COOLDOWN = 6000;

    @Inject(method = "finalizeSpawnChildFromBreeding", at = @At("TAIL"))
    private void turningseasons$seasonalCooldown(ServerLevel level, Animal partner, @Nullable AgeableMob offspring, CallbackInfo ci) {
        if (!Calendar.isSeasonal(level)) return;
        float factor = SeasonConfig.get().breedingCooldown.get(Calendar.server().date().season());
        int cooldown = Math.max(20, Math.round(VANILLA_COOLDOWN * factor));
        ((Animal) (Object) this).setAge(cooldown);
        partner.setAge(cooldown);
    }
}
