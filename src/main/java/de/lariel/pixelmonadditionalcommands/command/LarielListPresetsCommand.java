package de.lariel.pixelmonadditionalcommands.command;

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

public class LarielListPresetsCommand {
    public static LiteralArgumentBuilder<CommandSourceStack> registerListPresets() {
        return Commands.literal("lariellistpresets")
                .executes(ctx -> listPresets(ctx, ""))
                .then(Commands.argument("filter", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            ServerLevel level = ctx.getSource().getLevel();
                            Registry<NPCPreset> registry = level.registryAccess().registryOrThrow(PixelmonRegistry.NPC_PRESET_REGISTRY);
                            registry.keySet().forEach(id -> builder.suggest(id.toString()));
                            return builder.buildFuture();
                        })
                        .executes(ctx -> listPresets(ctx, StringArgumentType.getString(ctx, "filter"))));
    }

    private static int listPresets(CommandContext<CommandSourceStack> ctx, String filter) {
        ServerLevel level = ctx.getSource().getLevel();
        Registry<NPCPreset> registry = level.registryAccess().registryOrThrow(PixelmonRegistry.NPC_PRESET_REGISTRY);
        var matches = registry.keySet().stream().map(ResourceLocation::toString).filter(id -> id.contains(filter)).toList();
        if (matches.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No presets found."));
            return 0;
        }
        ctx.getSource().sendSuccess(() -> Component.literal("Presets:\n" + String.join("\n", matches)), false);
        return matches.size();
    }
}
