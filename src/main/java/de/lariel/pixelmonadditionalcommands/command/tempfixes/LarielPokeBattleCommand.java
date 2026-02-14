package de.lariel.pixelmonadditionalcommands.command.tempfixes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pixelmonmod.pixelmon.api.battles.BattleType;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.storage.PlayerPartyStorage;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.battles.BattleRegistry;
import com.pixelmonmod.pixelmon.battles.api.BattleBuilder;
import com.pixelmonmod.pixelmon.battles.api.rules.BattleRuleSet;
import com.pixelmonmod.pixelmon.battles.controller.ai.BattleAI;
import com.pixelmonmod.pixelmon.battles.controller.participants.BattleParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.EntityParticipant;
import com.pixelmonmod.pixelmon.battles.controller.participants.PlayerParticipant;
import com.pixelmonmod.pixelmon.entities.npcs.NPC;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

import java.awt.*;
import java.util.List;

public class LarielPokeBattleCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
                Commands.literal("larielpokebattle")
                        .then(buildSingle())
                        .then(buildDouble())
        );
    }

    private static int StartBattle(CommandContext<CommandSourceStack> ctx, boolean isDouble) throws CommandSyntaxException {
        Entity a = EntityArgument.getEntity(ctx, "a");
        Entity b = EntityArgument.getEntity(ctx, "b");

        if (!validateSameDimension(ctx, a, b)) return 0;
        if (!validateNotInBattle(ctx, a, b)) return 0;
        if (validateHasTeam(ctx, a)) return 0;
        if (validateHasTeam(ctx, b)) return 0;

        BattleParticipant p1 = createParticipant(a);
        BattleParticipant p2 = createParticipant(b);

        if (p1 == null || p2 == null) {
            ctx.getSource().sendFailure(Component.literal("Unable to prepare battle participants."));
            return 0;
        }

        BattleBuilder.builder()
                .teamOne(p1)
                .teamTwo(p2)
                .rules(BattleRuleSet.AG)
                .setBattleType(isDouble ? BattleType.DOUBLE : BattleType.SINGLE)
                .start(ctx.getSource().registryAccess());

        ctx.getSource().sendSuccess(
                () -> Component.literal("Battle started between " + p1.getName().getString() + " and " + p2.getName().getString()),
                false
        );

        return 1;
    }

    // ------------------------------------------------------------
    //  SINGLE BATTLE
    // ------------------------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> buildSingle() {
        return Commands.literal("single")
                .requires(src -> src.hasPermission(2))
                .then(
                        Commands.argument("a", EntityArgument.entity())
                                .then(
                                        Commands.argument("b", EntityArgument.entity())
                                                .executes(LarielPokeBattleCommand::runSingle)
                                )
                );
    }

    private static int runSingle(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return StartBattle(ctx, false);
    }

    // ------------------------------------------------------------
    //  DOUBLE BATTLE
    // ------------------------------------------------------------

    private static LiteralArgumentBuilder<CommandSourceStack> buildDouble() {
        return Commands.literal("double")
                .requires(src -> src.hasPermission(2))
                .then(
                        Commands.argument("a", EntityArgument.entity())
                                .then(
                                        Commands.argument("b", EntityArgument.entity())
                                                .executes(LarielPokeBattleCommand::runDouble)
                                )
                );
    }

    private static int runDouble(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return StartBattle(ctx, true);
    }

    // ------------------------------------------------------------
    //  VALIDATION
    // ------------------------------------------------------------

    private static boolean validateSameDimension(CommandContext<CommandSourceStack> ctx, Entity... e) {
        return validateSameDimension(ctx, List.of(e));
    }

    private static boolean validateSameDimension(CommandContext<CommandSourceStack> ctx, List<Entity> list) {
        Level dim = list.getFirst().level();
        for (Entity e : list) {
            if (e.level() != dim) {
                ctx.getSource().sendFailure(Component.literal("All participants must be in the same dimension."));
                return false;
            }
        }
        return true;
    }

    private static boolean validateNotInBattle(CommandContext<CommandSourceStack> ctx, Entity... e) {
        return validateNotInBattle(ctx, List.of(e));
    }

    private static boolean validateNotInBattle(CommandContext<CommandSourceStack> ctx, List<Entity> list) {
        for (Entity e : list) {
            if (BattleRegistry.getBattle(e) != null) {
                ctx.getSource().sendFailure(Component.literal(e.getName().getString() + " is already in a battle."));
                return false;
            }
        }
        return true;
    }

    private static boolean validateHasTeam(CommandContext<CommandSourceStack> ctx, Entity e) {
        if (e instanceof NPC npc) {
            var party = npc.getParty();
            if (party == null || party.countAblePokemon() < 1) {
                ctx.getSource().sendFailure(Component.literal(e.getName().getString() + " does not have enough Pokémon."));
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------
    //  PARTICIPANT CREATION
    // ------------------------------------------------------------

    private static BattleParticipant createParticipant(Entity e) {
        if (e instanceof ServerPlayer player) {
            PlayerPartyStorage party = StorageProxy.getPartyNow(player);
            if (party == null) return null;

            List<Pokemon> mons = party.findAll(Pokemon::canBattle);
            return mons.isEmpty() ? null : new PlayerParticipant(player, mons);
        }

        if (e instanceof NPC npc) {
            var storage = npc.getParty();
            var pokemon = storage.getTeam().toArray(new Pokemon[0]);

            return EntityParticipant.builder()
                    .entity(e)
                    .storage(storage)
                    .pokemon(pokemon)
                    .aiMode(BattleAI.RANDOM)
                    .build();
        }

        return null;
    }
}