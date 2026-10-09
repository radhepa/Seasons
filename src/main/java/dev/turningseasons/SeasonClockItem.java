package dev.turningseasons;

import dev.turningseasons.api.Season;

import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

/**
 * The season clock: its face shows the season (the item model picks a texture from the
 * {@code turningseasons:season} component, which the server keeps up to date), and using it says
 * the date. Holding one shows the small season HUD.
 */
public class SeasonClockItem extends Item {
    public static final DataComponentType<Season> SEASON = Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE,
            Identifier.fromNamespaceAndPath(TurningSeasons.MOD_ID, "season"),
            DataComponentType.<Season>builder().persistent(Season.CODEC)
                    .networkSynchronized(ByteBufCodecs.idMapper(i -> Season.values()[i], Season::ordinal)).build());

    public static final Identifier ID = Identifier.fromNamespaceAndPath(TurningSeasons.MOD_ID, "season_clock");
    public static final Item ITEM = Registry.register(BuiltInRegistries.ITEM, ID,
            new SeasonClockItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, ID)).stacksTo(1)
                    .component(SEASON, Season.SPRING)));

    public SeasonClockItem(Properties properties) {
        super(properties);
    }

    public static void init() {
        // loading the class registers the item and its component
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        Season now = Calendar.server().date().season();
        if (stack.get(SEASON) != now) stack.set(SEASON, now);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide()) {
            player.sendOverlayMessage(SeasonCommand.describe(Calendar.server().date()));
            level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS, 0.5F, 1.4F);
        }
        return InteractionResult.SUCCESS;
    }
}
