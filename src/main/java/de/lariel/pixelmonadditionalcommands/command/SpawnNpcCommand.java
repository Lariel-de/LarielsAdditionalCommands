package de.lariel.pixelmonadditionalcommands.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pixelmonmod.pixelmon.init.registry.PixelmonRegistry;
import de.lariel.pixelmonadditionalcommands.LarielsAdditionalCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.Level;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class SpawnNpcCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                LiteralArgumentBuilder.<CommandSourceStack>literal("larielspawnnpc")
                        .executes(context -> {
                            context.getSource().sendFailure(Component.literal("Invalid command"));
                            return 0;
                        })
                        .then(Commands.argument("presetSearchString", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    var level = ctx.getSource().getLevel();
                                    var registry = level.registryAccess().registryOrThrow(PixelmonRegistry.NPC_PRESET_REGISTRY);
                                    registry.keySet().forEach(id -> builder.suggest("\"" + id.toString() + "\""));
                                    return builder.buildFuture();
                                })
                                // Optional parameter: search mode
                                .then(Commands.argument("mode", StringArgumentType.string())
                                        .suggests((ctx, builder) -> {
                                            builder.suggest("first");
                                            builder.suggest("last");
                                            builder.suggest("random");
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> execute(ctx, true))
                                )
                                // Fallback without search mode defined -> it takes First
                                .executes(ctx -> execute(ctx, false))
                        )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> context, boolean hasMode) {
        var presetSearchString = StringArgumentType.getString(context, "presetSearchString");
        var logger = LarielsAdditionalCommands.getLogger();

        // Get search mode
        var mode = hasMode
                ? StringArgumentType.getString(context, "mode").toLowerCase()
                : "first";

        // Get Executor and Level
        var executor = context.getSource().getEntity();
        var level = context.getSource().getLevel();

        if (executor == null) {
            context.getSource().sendFailure(Component.literal("This command cannot be executed from console."));
            return 0;
        }

        // Load registry
        var registry = Optional.ofNullable(ServerLifecycleHooks.getCurrentServer())
                .map(MinecraftServer::registryAccess)
                .map(ra -> ra.registryOrThrow(PixelmonRegistry.NPC_PRESET_REGISTRY))
                .orElse(null);

        if (registry == null) {
            var errorMessage = "Didn't found presets.";
            context.getSource().sendFailure(Component.literal(errorMessage));
            logger.log(Level.ERROR, errorMessage);
            return 0;
        }

        // Get all presets that are matching the filter
        var matches = registry.entrySet().stream()
                .filter(e -> e.getKey().location().toString().contains(presetSearchString))
                .map(Map.Entry::getValue)
                .toList();

        // Print out error message if no preset was found.
        if (matches.isEmpty()) {
            var errorMessage = "Didn't find a preset that contains \"" + presetSearchString + "\".";
            context.getSource().sendFailure(Component.literal(errorMessage));
            logger.log(Level.DEBUG, errorMessage);
            return 0;
        }

        // Select a single preset depending on selection mode.
        var preset = switch (mode) {
            case "last" -> matches.getLast();
            case "random" -> matches.get(ThreadLocalRandom.current().nextInt(matches.size()));
            default -> matches.getFirst(); // "first"
        };

        // Spawn NPC
        var npc = preset.createBuilder()
                .position(executor.position())
                .track()
                .buildAndSpawn(level);

        // Add tag that it could be easily detected.
        npc.addTag("LarielsAdditonalCommandsNpc");

        // If player → NPC looks at player
        if (executor instanceof ServerPlayer player) {
            npc.moveNPC(() -> npc.lookAt(EntityAnchorArgument.Anchor.EYES, executor.position()));
            npc.setCurrentlyEditing(player);
        }

        var npcName = Optional.ofNullable(npc.getCustomName())
                .map(Component::getString)
                .orElse("No Custom Name found");

        context.getSource().sendSuccess(
                () -> Component.literal("Spawned: " + npcName + " (" + mode + ")"),
                true
        );

        return 1;
    }
}
