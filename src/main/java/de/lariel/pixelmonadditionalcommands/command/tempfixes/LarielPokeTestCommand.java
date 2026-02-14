package de.lariel.pixelmonadditionalcommands.command.tempfixes;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pixelmonmod.api.parsing.ParseAttempt;
import com.pixelmonmod.api.pokemon.PokemonSpecification;
import com.pixelmonmod.api.pokemon.PokemonSpecificationProxy;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;
import java.util.Objects;

public class LarielPokeTestCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        var base = Commands.literal("larielpoketest").requires(src -> src.hasPermission(2));
        var players = Commands.argument("players", EntityArgument.players());
        var spec = Commands.argument("spec", StringArgumentType.greedyString()).executes(ctx -> handleMatch(ctx, -1));
        var slot = Commands.argument("slot", IntegerArgumentType.integer(1, 6))
                .then(Commands.argument("spec", StringArgumentType.greedyString())
                        .executes(ctx -> handleMatch(ctx, IntegerArgumentType.getInteger(ctx, "slot"))));

        dispatcher.register(base.then(players.then(slot)).then(players.then(spec)));
    }

    private static int handleMatch(CommandContext<CommandSourceStack> ctx, int slot) throws CommandSyntaxException {
        String specString = StringArgumentType.getString(ctx, "spec");
        ParseAttempt<PokemonSpecification> attempt = PokemonSpecificationProxy.create(specString);
        if (!attempt.wasSuccess()) {
            ctx.getSource().sendFailure(Component.translatable("pixelmon.command.general.invalid", attempt.getError()));
            return 0;
        }
        PokemonSpecification spec = attempt.get();
        Collection<ServerPlayer> players = EntityArgument.getPlayers(ctx, "players");
        if (players.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal("No players found."));
            return 0;
        }
        int matched = findMatches(players, slot, spec);
        ctx.getSource().sendSuccess(() -> Component.translatable("pixelmon.command.poketest.matches", matched), false);
        return matched;
    }

    private static int findMatches(Collection<ServerPlayer> players, int slot, PokemonSpecification spec) {
        return players.stream()
                .map(StorageProxy::getPartyNow)
                .filter(Objects::nonNull)
                .mapToInt(party -> {
                    if (slot == -1) {
                        return party.countAll(spec);
                    }
                    Pokemon mon = party.get(slot - 1);
                    return (mon != null && spec.matches(mon)) ? 1 : 0;
                })
                .sum();
    }
}
