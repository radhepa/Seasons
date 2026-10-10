package dev.turningseasons;

import java.util.Arrays;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;

import dev.turningseasons.api.CalendarDate;
import dev.turningseasons.api.Season;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;

/**
 * {@code /season}                         what season and day it is (anyone)
 * {@code /season set <season> [day]}      jump forward to that season's morning (operators)
 * {@code /season advance <days>}          move the calendar forward some days (operators)
 * {@code /season length}                  how many days each season lasts (anyone)
 * {@code /season length <days>}           change this world's season length (operators / cheats on)
 */
public final class SeasonCommand {
    private SeasonCommand() {}

    private static final DynamicCommandExceptionType UNKNOWN = new DynamicCommandExceptionType(
            name -> Component.translatable("commands.turningseasons.unknown", name));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("season")
                .executes(SeasonCommand::query)
                .then(Commands.literal("set").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("season", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(Arrays.stream(Season.values()).map(Season::id), b))
                                .executes(ctx -> set(ctx, 1))
                                .then(Commands.argument("day", IntegerArgumentType.integer(1))
                                        .executes(ctx -> set(ctx, IntegerArgumentType.getInteger(ctx, "day"))))))
                .then(Commands.literal("advance").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(Commands.argument("days", IntegerArgumentType.integer(1, 10000))
                                .executes(SeasonCommand::advance)))
                .then(Commands.literal("length")
                        .executes(SeasonCommand::length)
                        .then(Commands.argument("days", IntegerArgumentType.integer(1, 10000))
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .executes(SeasonCommand::setLength))));
    }

    /** "It's Autumn, day 3 of 30 (year 1). Winter comes in 28 days." */
    public static Component describe(CalendarDate date) {
        return Component.translatable("commands.turningseasons.query", date.season().displayName(), date.dayOfSeason(),
                date.daysPerSeason(), date.year(), date.season().next().displayName(), date.daysUntilNextSeason());
    }

    private static int query(CommandContext<CommandSourceStack> ctx) {
        CalendarDate date = Calendar.server().date();
        ctx.getSource().sendSuccess(() -> describe(date), false);
        return date.season().ordinal() + 1;
    }

    private static int set(CommandContext<CommandSourceStack> ctx, int day) throws CommandSyntaxException {
        String name = StringArgumentType.getString(ctx, "season");
        Season season = Season.byId(name);
        if (season == null) throw UNKNOWN.create(name);
        Calendar c = Calendar.server();
        long jump = CalendarDate.ticksUntil(c.elapsed(), c.daysPerSeason(), season, day);
        SeasonsServer.setElapsed(ctx.getSource().getServer(), c.elapsed() + jump);
        CalendarDate date = Calendar.server().date();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.turningseasons.set", describe(date)), true);
        return 1;
    }

    private static int length(CommandContext<CommandSourceStack> ctx) {
        int days = Calendar.server().daysPerSeason();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.turningseasons.length", days, days * 4), false);
        return days;
    }

    private static int setLength(CommandContext<CommandSourceStack> ctx) {
        int days = IntegerArgumentType.getInteger(ctx, "days");
        SeasonsServer.setDaysPerSeason(ctx.getSource().getServer(), days);
        CalendarDate date = Calendar.server().date();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.turningseasons.length.set", days, describe(date)), true);
        return days;
    }

    private static int advance(CommandContext<CommandSourceStack> ctx) {
        int days = IntegerArgumentType.getInteger(ctx, "days");
        Calendar c = Calendar.server();
        SeasonsServer.setElapsed(ctx.getSource().getServer(), c.elapsed() + (long) days * CalendarDate.TICKS_PER_DAY);
        CalendarDate date = Calendar.server().date();
        ctx.getSource().sendSuccess(() -> Component.translatable("commands.turningseasons.set", describe(date)), true);
        return days;
    }
}
