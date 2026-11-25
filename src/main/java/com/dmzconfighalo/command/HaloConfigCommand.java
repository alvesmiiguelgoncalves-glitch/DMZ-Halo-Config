package com.dmzconfighalo.command;

import com.dmzconfighalo.DMZConfigHalo;
import com.dmzconfighalo.HaloManager;
import com.dmzconfighalo.HaloSettings;
import com.dmzconfighalo.compat.DmzStats;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
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
import java.util.Locale;
import java.util.Map;
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

    private static final String[] HAIR_TYPES = {"base", "ssj", "ssj2", "ssj3", "ssj4", "custom", "none"};

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
                        .then(rotationNode())
                        .then(tableNode("formheight", "form", new FormTable()))
                        .then(tableNode("hairheight", "hair", new HairTable()))));
    }

    // ------------------------------------------------------------------ base height / color / info

    private static int showInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        HaloSettings s = HaloManager.get(player);
        ctx.getSource().sendSuccess(() -> Component.translatable("dmzconfighalo.command.info",
                HaloSettings.formatHeight(s.height()), HaloSettings.formatColor(s.color())), false);
        ctx.getSource().sendSuccess(() -> Component.translatable("dmzconfighalo.command.info.rotation",
                HaloSettings.formatHeight(s.rotX()), HaloSettings.formatHeight(s.rotY()),
                HaloSettings.formatHeight(s.rotZ())), false);
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

    // ------------------------------------------------------------------ rotation

    private static LiteralArgumentBuilder<CommandSourceStack> rotationNode() {
        LiteralArgumentBuilder<CommandSourceStack> node = Commands.literal("rotation")
                .executes(HaloConfigCommand::showInfo)
                .then(Commands.literal("reset").executes(HaloConfigCommand::resetRotation));
        for (char axis : new char[]{'x', 'y', 'z'}) {
            node.then(Commands.literal(String.valueOf(axis))
                    .then(Commands.argument("degrees", FloatArgumentType.floatArg(-HaloSettings.MAX_ROTATION, HaloSettings.MAX_ROTATION))
                            .executes(ctx -> setRotation(ctx, axis))));
        }
        return node;
    }

    private static int setRotation(CommandContext<CommandSourceStack> ctx, char axis) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        float degrees = HaloSettings.clampRotation(FloatArgumentType.getFloat(ctx, "degrees"));
        HaloManager.set(player, HaloManager.get(player).withRotation(axis, degrees));
        ctx.getSource().sendSuccess(() -> Component.translatable("dmzconfighalo.command.rotation.set",
                String.valueOf(axis).toUpperCase(Locale.ROOT), HaloSettings.formatHeight(degrees)), false);
        return 1;
    }

    private static int resetRotation(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        HaloManager.set(player, HaloManager.get(player).withoutRotation());
        ctx.getSource().sendSuccess(() -> Component.translatable("dmzconfighalo.command.rotation.reset"), false);
        return 1;
    }

    // ------------------------------------------------------------------ per-form / per-hair height

    /** What differs between "formheight" and "hairheight". */
    private interface Table {
        Map<String, Float> entries(HaloSettings s);

        HaloSettings with(HaloSettings s, String key, float value);

        HaloSettings cleared(HaloSettings s);

        /** Key of the player's current form / hair, or null. */
        String currentKey(ServerPlayer player);

        Set<String> suggestions(ServerPlayer player);
    }

    private static final class FormTable implements Table {
        public Map<String, Float> entries(HaloSettings s) {
            return s.formHeights();
        }

        public HaloSettings with(HaloSettings s, String key, float value) {
            return s.withFormHeight(key, value);
        }

        public HaloSettings cleared(HaloSettings s) {
            return s.withoutFormHeights();
        }

        public String currentKey(ServerPlayer player) {
            String form = DmzStats.activeFormName(player);
            return form != null ? form : DmzStats.activeStackFormName(player);
        }

        public Set<String> suggestions(ServerPlayer player) {
            Set<String> names = new LinkedHashSet<>();
            String form = DmzStats.activeFormName(player);
            if (form != null) {
                names.add(form);
            }
            String stack = DmzStats.activeStackFormName(player);
            if (stack != null) {
                names.add(stack);
            }
            return names;
        }
    }

    private static final class HairTable implements Table {
        public Map<String, Float> entries(HaloSettings s) {
            return s.hairHeights();
        }

        public HaloSettings with(HaloSettings s, String key, float value) {
            return s.withHairHeight(key, value);
        }

        public HaloSettings cleared(HaloSettings s) {
            return s.withoutHairHeights();
        }

        public String currentKey(ServerPlayer player) {
            return DmzStats.currentHairKey(player);
        }

        public Set<String> suggestions(ServerPlayer player) {
            Set<String> names = new LinkedHashSet<>();
            String current = DmzStats.currentHairKey(player);
            if (current != null) {
                names.add(current);
            }
            names.addAll(java.util.Arrays.asList(HAIR_TYPES));
            return names;
        }
    }

    private static LiteralArgumentBuilder<CommandSourceStack> tableNode(String literal, String argName, Table table) {
        return Commands.literal(literal)
                .executes(ctx -> listTable(ctx, literal, table))
                .then(Commands.literal("clear").executes(ctx -> clearTable(ctx, literal, table)))
                .then(Commands.literal("current")
                        .then(Commands.argument("value", FloatArgumentType.floatArg(-HaloSettings.MAX_HEIGHT, HaloSettings.MAX_HEIGHT))
                                .executes(ctx -> setCurrent(ctx, literal, table))))
                .then(Commands.argument(argName, StringArgumentType.word())
                        .suggests((ctx, builder) -> suggest(ctx, builder, table))
                        .then(Commands.literal("reset")
                                .executes(ctx -> applyEntry(ctx, literal, table, StringArgumentType.getString(ctx, argName), 0.0F)))
                        .then(Commands.argument("value", FloatArgumentType.floatArg(-HaloSettings.MAX_HEIGHT, HaloSettings.MAX_HEIGHT))
                                .executes(ctx -> applyEntry(ctx, literal, table, StringArgumentType.getString(ctx, argName),
                                        FloatArgumentType.getFloat(ctx, "value")))));
    }

    private static CompletableFuture<Suggestions> suggest(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder, Table table) {
        Set<String> names = new LinkedHashSet<>();
        if (ctx.getSource().getEntity() instanceof ServerPlayer player) {
            names.addAll(table.suggestions(player));
            names.addAll(table.entries(HaloManager.get(player)).keySet());
        }
        return SharedSuggestionProvider.suggest(names, builder);
    }

    private static int listTable(CommandContext<CommandSourceStack> ctx, String literal, Table table) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        Map<String, Float> entries = table.entries(HaloManager.get(player));
        if (entries.isEmpty()) {
            ctx.getSource().sendSuccess(() -> Component.translatable("dmzconfighalo.command." + literal + ".empty"), false);
            return 1;
        }
        ctx.getSource().sendSuccess(() -> Component.translatable("dmzconfighalo.command." + literal + ".list"), false);
        entries.forEach((name, value) -> ctx.getSource().sendSuccess(
                () -> Component.literal("  " + name + ": " + HaloSettings.formatHeight(value) + " px"), false));
        return entries.size();
    }

    private static int clearTable(CommandContext<CommandSourceStack> ctx, String literal, Table table) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        HaloManager.set(player, table.cleared(HaloManager.get(player)));
        ctx.getSource().sendSuccess(() -> Component.translatable("dmzconfighalo.command." + literal + ".cleared"), false);
        return 1;
    }

    private static int setCurrent(CommandContext<CommandSourceStack> ctx, String literal, Table table) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String name = table.currentKey(player);
        if (name == null) {
            ctx.getSource().sendFailure(Component.translatable("dmzconfighalo.command." + literal + ".nocurrent"));
            return 0;
        }
        return applyEntry(ctx, literal, table, name, FloatArgumentType.getFloat(ctx, "value"));
    }

    private static int applyEntry(CommandContext<CommandSourceStack> ctx, String literal, Table table,
                                  String rawName, float value) throws CommandSyntaxException {
        ServerPlayer player = ctx.getSource().getPlayerOrException();
        String key = HaloSettings.normalizeFormKey(rawName);
        if (key == null) {
            ctx.getSource().sendFailure(Component.translatable("dmzconfighalo.command.name.invalid"));
            return 0;
        }
        HaloSettings current = HaloManager.get(player);
        Map<String, Float> entries = table.entries(current);
        if (value != 0.0F && !entries.containsKey(key) && entries.size() >= HaloSettings.MAX_FORMS) {
            ctx.getSource().sendFailure(Component.translatable("dmzconfighalo.command.name.toomany", HaloSettings.MAX_FORMS));
            return 0;
        }
        HaloManager.set(player, table.with(current, key, value));
        if (value == 0.0F) {
            ctx.getSource().sendSuccess(() -> Component.translatable("dmzconfighalo.command." + literal + ".removed", key), false);
        } else {
            ctx.getSource().sendSuccess(() -> Component.translatable("dmzconfighalo.command." + literal + ".set", key,
                    HaloSettings.formatHeight(HaloSettings.clampHeight(value))), false);
        }
        return 1;
    }
}
