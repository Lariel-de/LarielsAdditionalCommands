package de.lariel.pixelmonadditionalcommands.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.pixelmonmod.pixelmon.api.npc.NPCPreset;
import com.pixelmonmod.pixelmon.init.registry.PixelmonRegistry;
import de.lariel.pixelmonadditionalcommands.LarielsAdditionalCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.Level;

import java.util.Map;

public class SpawnNpcCommand {
    /**
     *
     * Used for registering the command on the {@link net.neoforged.neoforge.event.RegisterCommandsEvent}
     * <p>
     * For more information about brigadier, how it works, what things mean, and lots of examples please read the
     * GitHub READ ME here <a href="https://github.com/Mojang/brigadier/blob/master/README.md">URL</a>
     *
     * @param dispatcher The dispatcher from the event
     */
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(LiteralArgumentBuilder.<CommandSourceStack>literal("larielspawnnpc")
                .executes(context -> {
                    context.getSource().sendFailure(Component.literal("Invalid command"));
                    return 0;
                })
                .then(Commands.argument("presetSearchString", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            ServerLevel level = ctx.getSource().getLevel();
                            Registry<NPCPreset> registry = level.registryAccess().registryOrThrow(PixelmonRegistry.NPC_PRESET_REGISTRY);
                            registry.keySet().forEach(id -> builder.suggest(id.toString()));
                            return builder.buildFuture();
                        })
                        .executes(context -> {
                            var presetSearchString = StringArgumentType.getString(context, "presetSearchString");
                            var logger = LarielsAdditionalCommands.getLogger();

                            // Get executor of the command and the level
                            var executor = context.getSource().getEntity();
                            var level = context.getSource().getLevel();

                            // Read out all npc presets
                            var registry = ServerLifecycleHooks.getCurrentServer().registryAccess().registry(PixelmonRegistry.NPC_PRESET_REGISTRY).get();

                            // Search for the given preset
                            var preset = registry.entrySet().stream()
                                    .filter(e -> e.getKey().location().toString().contains(presetSearchString))
                                    .map(Map.Entry::getValue)
                                    .findFirst()
                                    .orElse(null);

                            // If there is no matching preset, print out an error message
                            if (preset == null) {
                                var errorMessage = "Didn't found a preset that contains \"" + presetSearchString + "\".";
                                context.getSource().sendFailure(Component.literal(errorMessage));
                                logger.log(Level.DEBUG, errorMessage);

                                return 0;
                            }

                            // If preset was found create npc on the executors position.
                            var npc = preset.createBuilder().position(new Vec3(executor.getX(), executor.getY(), executor.getZ())).track().buildAndSpawn(level);
                            npc.addTag("LarielsAdditonalCommandsNpc");

                            // If executor was a player, let the npc look at the player.
                            if (executor instanceof ServerPlayer player) {
                                npc.moveNPC(() -> npc.lookAt(EntityAnchorArgument.Anchor.EYES, executor.position()));
                                npc.setCurrentlyEditing(player);
                            }

                            // Print out success message on console.
                            context.getSource().sendSuccess(() -> Component.literal("Spawned: " + npc.getCustomName().getString()), true);

                            return 1;
                        })
                ));
    }
}
