package de.lariel.pixelmonadditionalcommands.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.pixelmonmod.pixelmon.api.command.PixelmonCommandUtils;
import com.pixelmonmod.pixelmon.api.config.PixelmonConfigProxy;
import com.pixelmonmod.pixelmon.api.storage.PartyStorage;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.entities.npcs.NPC;
import de.lariel.pixelmonadditionalcommands.utility.LarielErrorLog;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.Optional;

public class LarielSetTrainerToLevel {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("larielsetlevel")
                .then(Commands.argument("target", EntityArgument.entity())
                        .then(Commands.argument("level", StringArgumentType.string())
                                .suggests((ctx, builder) -> {
                                    var max = PixelmonConfigProxy.getGeneral().getMaxLevel();

                                    // Fix sorting of the recommended levels
                                    for (var i = 1; i <= max; i++) {
                                        builder.suggest(i, Component.literal(String.valueOf(i)));
                                    }

                                    return builder.buildFuture();
                                }).executes(ctx -> {
                                    var target = EntityArgument.getEntity(ctx, "target");
                                    var lvl = StringArgumentType.getString(ctx, "level");
                                    var level = PixelmonCommandUtils.requireInt(lvl, 1, PixelmonConfigProxy.getGeneral().getMaxLevel(), "parsing.int.invalid");

                                    if (!verifyEntity(ctx, target)) {
                                        LarielErrorLog.LogError("Entity could not be edited", ctx);
                                    }

                                    if ((!(target instanceof NPC npc))) {
                                        return LarielErrorLog.LogError("Target is no npc", ctx);
                                    }

                                    var party = npc.getParty();
                                    var team = Optional.ofNullable(party)
                                            .map(PartyStorage::getTeam)
                                            .orElse(null);

                                    if (team == null) {
                                        return LarielErrorLog.LogError("Couldn't get team from entity", ctx);
                                    }

                                    team.forEach((p) -> p.setLevel(level));

                                    ctx.getSource().sendSuccess(() -> Component.literal("Set level of " + target.getName().getString() + " to " + level), true);
                                    return 1;
                                }))));
    }

    private static boolean verifyEntity(CommandContext<CommandSourceStack> commandContext, Entity entity) {
        // If entity is in battle
        if (BattleRegistry.getBattle(entity) != null) {
            commandContext.getSource().sendFailure(Component.translatable("pixelmon.command.battle.cannotchallenge", entity.getDisplayName().getString()));
            return false;
        }

        return true;
    }
}
