package com.dmzconfighalo.command;

import com.dmzconfighalo.DMZConfigHalo;
import com.dmzconfighalo.HaloManager;
import com.dmzconfighalo.HaloSettings;
import com.dmzconfighalo.compat.DmzStats;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.LinkedHashSet;
import java.util.OptionalInt;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Adds "/dmzhalo config ..." to DragonMine Z's existing /dmzhalo command.
 *
 * Brigadier MERGES two registrations of the same literal, but keeps the requirement of the FIRST one registered.
 * DMZ requires permission on the whole /dmzhalo root, which would hide "config" from normal players. So this
 * handler runs BEFORE DMZ (HIGHEST priority), registers the root with no requirement, and re-declares "on"/"off"
 * with DMZ's own permission check. DMZ's registration then merges into ours (adding its targets and executors).
 */
public final class HaloConfigCommand {

    private HaloConfigCommand() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();

        if (dispatcher.getRoot().getChild("dmzhalo") != null) {
            DMZConfigHalo.LOGGER.warn("[DMZ Config Halo] /dmzhalo was registered before this addon. "
                    + "'config' may only be visible to players with permission.");
        }

        dispatcher.register(Commands.literal("dmzhalo")
                .then(Commands.literal("on").requires(DmzPermissions::canToggleHalo))
                .then(Commands.literal("off").requires(DmzPermissions::canToggleHalo))
                .then(Commands.literal("config")
                        .requires(src -> src.getEntity() instanceof ServerPlayer)
                        .executes(HaloConfigCommand::showInfo)
                        .then(Commands.literal("height")
                                .then(Commands.argument("value", FloatArgumentType.floatArg(-HaloSettings.MAX_HEIGHT, HaloSettings.MAX_HEIGHT))
                                        .executes(HaloConfigCommand::setHeight)))
                        .then(Commands.literal("color")
                                .then(Commands.literal("reset").executes(HaloConfigCommand::resetColor))
                                .then(Commands.argument("hex", StringArgumentType.greedyString())
                                        .executes(HaloConfigCommand::setColor)))
                        .then(Commands.literal("formheight")
                                .executes(HaloConfigCommand::listForms)
                                .then(Commands.literal("clear").executes(HaloConfigCommand::clearForms))
                                .then(Commands.literal("current")
                                        .then(Commands.argument("value", FloatArgumentType.floatArg(-HaloSettings.MAX_HEIGHT, HaloSettings.MAX_HEIGHT))
                                                .executes(HaloConfigCommand::setCurrentForm)))
                                .then(Commands.argument("form", StringArgumentType.word())
                                        .suggests(HaloConfigCommand::suggestForms)
                                        .then(Commands.literal("reset").executes(HaloConfigCommand::resetForm))
                                        .then(Commands.argument("value", FloatArgumentType.floatArg(-HaloSettings.MAX_HEIGHT, HaloSettings.MAX_HEIGHT))
                                                .executes(HaloConfigCommand::setForm))))));
    }

    // ------------------------------------------------------------------ base height / color

    private static int showInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        HaloSettings s = HaloManager.get(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("dmzconfighalo.command.info",
                HaloSettings.formatHeight(s.height()), HaloSettings.formatColor(s.color())), false);
        return 1;
    }

    private static int setHeight(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        float value = FloatArgumentType.getFloat(ctx, "value");
        HaloSettings updated = HaloManager.get(player).withHeight(value);
        HaloManager.set(player, updated);
        ctx.getSource().sendSuccess(() -> Component.translatable("dmzconfighalo.command.height",
                HaloSettings.formatHeight(updated.height())), false);
        return 1;
    }

    private static int resetColor(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        HaloManager.set(player, HaloManager.get(player).withColor(-1));
        ctx.getSource().sendSuccess(() -> Component.translatable("dmzconfighalo.command.color.reset"), false);
        return 1;
    }

    private static int setColor(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String raw = StringArgumentType.getString(ctx, "hex");
        OptionalInt parsed = HaloSettings.parseHex(raw);
        if (parsed.isEmpty()) {
            ctx.getSource().sendFailure(Component.translatable("dmzconfighalo.command.color.invalid"));
            return 0;
        }
        HaloSettings updated = HaloManager.get(player).withColor(parsed.getAsInt());
        HaloManager.set(player, updated);
        ctx.getSource().sendSuccess(() -> Component.translatable("dmzconfighalo.command.color",
                HaloSettings.formatColor(updated.color())), false);
        return 1;
    }

    // ------------------------------------------------------------------ per-form height

    private static CompletableFuture<Suggestions> suggestForms(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        Set<String> names = new LinkedHashSet<>();
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            String form = DmzStats.activeFormName(player);
            if (form != null) {
                names.add(form);
            }
            String stack = DmzStats.activeStackFormName(player);
            if (stack != null) {
                names.add(stack);
            }
            names.addAll(HaloManager.get(player).formHeights().keySet());
        }
        return SharedSuggestionProvider.suggest(names, builder);
    }

    private static int listForms(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        HaloSettings s = HaloManager.get(player);
        if (s.formHeights().isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("dmzconfighalo.command.formheight.empty"), false);
            return 1;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("dmzconfighalo.command.formheight.list"), false);
        s.formHeights().forEach((name, value) -> ctx.getSource().sendSuccess(
                () -> Component.literal("  " + name + ": " + HaloSettings.formatHeight(value) + " px"), false));
        return s.formHeights().size();
    }

    private static int clearForms(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        HaloManager.set(player, HaloManager.get(player).withoutFormHeights());
        ctx.getSource().sendSuccess(() -> Component.translatable("dmzconfighalo.command.formheight.cleared"), false);
        return 1;
    }

    private static int setCurrentForm(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = DmzStats.activeFormName(player);
        if (name == null) {
            name = DmzStats.activeStackFormName(player);
        }
        if (name == null) {
            ctx.getSource().sendFailure(Component.translatable("dmzconfighalo.command.formheight.noform"));
            return 0;
        }
        return applyFormHeight(ctx, player, name, FloatArgumentType.getFloat(ctx, "value"));
    }

    private static int setForm(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        return applyFormHeight(ctx, player, StringArgumentType.getString(ctx, "form"),
                FloatArgumentType.getFloat(ctx, "value"));
    }

    private static int resetForm(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        return applyFormHeight(ctx, player, StringArgumentType.getString(ctx, "form"), 0.0F);
    }

    private static int applyFormHeight(CommandContext<CommandSourceStack> ctx, ServerPlayer player,
                                       String rawName, float value) {
        String key = HaloSettings.normalizeFormKey(rawName);
        if (key == null) {
            ctx.getSource().sendFailure(Component.translatable("dmzconfighalo.command.formheight.invalid"));
            return 0;
        }
        HaloSettings current = HaloManager.get(player);
        if (value != 0.0F && !current.formHeights().containsKey(key)
                && current.formHeights().size() >= HaloSettings.MAX_FORMS) {
            ctx.getSource().sendFailure(Component.translatable("dmzconfighalo.command.formheight.toomany",
                    HaloSettings.MAX_FORMS));
            return 0;
        }
        HaloManager.set(player, current.withFormHeight(key, value));
        if (value == 0.0F) {
            ctx.getSource().sendSuccess(() -> Component.translatable("dmzconfighalo.command.formheight.removed", key), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.translatable("dmzconfighalo.command.formheight.set", key,
                    HaloSettings.formatHeight(HaloSettings.clampHeight(value))), false);
        }
        return 1;
    }
}
