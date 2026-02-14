package de.lariel.pixelmonadditionalcommands.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.pixelmonmod.pixelmon.init.registry.PixelmonRegistry;
import de.lariel.pixelmonadditionalcommands.utility.LarielErrorLog;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceKeyArgument;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;
import net.minecraft.commands.arguments.coordinates.WorldCoordinates;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public class LarielSpawnNpcCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("larielspawnnpc")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("presetSearchString", ResourceKeyArgument.key(PixelmonRegistry.NPC_PRESET_REGISTRY))
                        .executes(ctx -> execute(ctx, false))
                        .then(Commands.argument("mode", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("First");
                                    builder.suggest("Last");
                                    builder.suggest("Random");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> execute(ctx, true))
                                .then(Commands.argument("pos", Vec3Argument.vec3())
                                        .executes(ctx -> execute(ctx, true))))));
    }

    private static int execute(CommandContext<CommandSourceStack> context, boolean hasMode) {
        var presetSearchString = context.getArgument("presetSearchString", ResourceKey.class).location().getPath();
        var pos = GetPosition(context);

        // Get search mode
        var mode = hasMode
                ? StringArgumentType.getString(context, "mode").toLowerCase()
                : "first";

        // Load registry
        var registry = Optional.ofNullable(ServerLifecycleHooks.getCurrentServer())
                .map(MinecraftServer::registryAccess)
                .map(ra -> ra.registryOrThrow(PixelmonRegistry.NPC_PRESET_REGISTRY))
                .orElse(null);

        if (registry == null) {
            return LarielErrorLog.LogError("Didn't found presets.", context);
        }

        // Get all presets that are matching the filter
        var matches = registry.entrySet().stream()
                .filter(e -> e.getKey().location().toString().contains(presetSearchString))
                .map(Map.Entry::getKey)
                .toList();

        // Print out error message if no preset was found.
        if (matches.isEmpty()) {
            var errorMessage = "Didn't find a preset that contains \"" + presetSearchString + "\".";
            return LarielErrorLog.LogDebug(errorMessage, context);
        }

        // Select a single preset depending on selection mode.
        var preset = switch (mode) {
            case "last" -> matches.getLast();
            case "random" -> matches.get(ThreadLocalRandom.current().nextInt(matches.size()));
            default -> matches.getFirst(); // "first"
        };

        var source = context.getSource();

        // Build command
        var cmd = "npc spawn " + preset.location() + " " + pos.x + " " + pos.y + " " + pos.z;

        // Spawn NPC with build in NPCCommand
        source.getServer().getCommands().performPrefixedCommand(source, cmd);

        return 1;
    }

    private static Vec3 GetPosition(CommandContext<CommandSourceStack> context) {
        var hasPos = context.getNodes().stream() .anyMatch(n -> n.getNode().getName().equals("pos"));

        if (hasPos)
        {
            var pos = context.getArgument("pos", WorldCoordinates.class);
            return pos.getPosition(context.getSource());
        }

        var executor = context.getSource().getEntity();

        if (executor == null) {
            LarielErrorLog.LogDebug("This command cannot be executed from console.", context);

            return new Vec3(0, 0, 0);
        }

        return new Vec3(executor.getX(), executor.getY(), executor.getZ());
    }
}
