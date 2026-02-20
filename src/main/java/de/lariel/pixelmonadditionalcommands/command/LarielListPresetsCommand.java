package de.lariel.pixelmonadditionalcommands.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.pixelmonmod.pixelmon.api.npc.NPCPreset;
import com.pixelmonmod.pixelmon.init.registry.PixelmonRegistry;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import org.jetbrains.annotations.NotNull;

public class LarielListPresetsCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("lariellistpresets")
                .executes(ctx -> listPresets(ctx, ""))
                .then(Commands.argument("filter", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            var registry = getNpcPresets(ctx);
                            registry.keySet().forEach(id -> builder.suggest(id.toString()));
                            return builder.buildFuture();
                        })
                        .executes(ctx -> listPresets(ctx, StringArgumentType.getString(ctx, "filter")))));
    }

    private static int listPresets(CommandContext<CommandSourceStack> ctx, String filter) {
        var registry = getNpcPresets(ctx);
        var matches = registry.keySet().stream().map(ResourceLocation::toString).filter(id -> id.contains(filter)).toList();
        if (matches.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No presets found."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Presets:\n" + String.join("\n", matches)), false);
        return matches.size();
    }

    private static @NotNull Registry<NPCPreset> getNpcPresets(CommandContext<CommandSourceStack> ctx) {
        var level = ctx.getSource().getLevel();
        return level.registryAccess().registryOrThrow(PixelmonRegistry.NPC_PRESET_REGISTRY);
    }
}
