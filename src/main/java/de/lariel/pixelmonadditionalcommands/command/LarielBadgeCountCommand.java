package de.lariel.pixelmonadditionalcommands.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pixelmonmod.pixelmon.init.registry.PixelmonDataComponents;
import com.pixelmonmod.pixelmon.items.BadgeCaseItem;
import com.pixelmonmod.pixelmon.items.BadgeItem;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class LarielBadgeCountCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("larielbadgecount")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(ctx -> checkBadges(ctx, false))
                        .then(Commands.literal("unique")
                                .executes(ctx -> checkBadges(ctx, true)))));
    }

    private static int checkBadges(CommandContext<CommandSourceStack> ctx, boolean isUnique) throws CommandSyntaxException {
        var player = EntityArgument.getPlayer(ctx, "player");

        var badgeCounts = collectAllBadges(player);

        if (badgeCounts.isEmpty()) {
            ctx.getSource().sendFailure(Component.literal(player.getName().getString() + " has no BadgeItems."));
            return 0;
        }

        var formatted = formatBadgeCounts(badgeCounts);
        var resultCount = calculateResultCount(badgeCounts, isUnique);

        ctx.getSource().sendSuccess(
                () -> Component.literal(
                        player.getName().getString()
                                + (isUnique
                                ? " has " + resultCount + " unique BadgeItem(s): "
                                : " has " + resultCount + " BadgeItem(s): ")
                                + formatted),
                false
        );

        return resultCount;
    }

    private static Map<String, Long> collectAllBadges(ServerPlayer player) {
        Map<String, Long> badgeCounts = new HashMap<>();

        collectInventoryBadges(player, badgeCounts);
        collectBadgeCaseBadges(player, badgeCounts);

        return badgeCounts;
    }

    private static void collectInventoryBadges(ServerPlayer player, Map<String, Long> badgeCounts) {
        var allStacks = Stream.of(
                player.getInventory().items.stream(),
                player.getInventory().armor.stream(),
                player.getInventory().offhand.stream()
        ).flatMap(s -> s);

        allStacks
                .filter(stack -> !stack.isEmpty())
                .filter(stack -> stack.getItem() instanceof BadgeItem)
                .forEach(stack -> {
                    var name = stack.getHoverName().getString();
                    badgeCounts.merge(name, 1L, Long::sum);
                });
    }

    private static void collectBadgeCaseBadges(ServerPlayer player, Map<String, Long> badgeCounts) {
        var badgeCaseStack = BadgeCaseItem.findFirstRegisteredBadgeCase(player);
        if (badgeCaseStack == null) return;

        var badgeCase = badgeCaseStack.get(PixelmonDataComponents.BADGE_CASE.get());
        if (badgeCase == null) return;

        for (var badge : badgeCase.badges()) {
            if (badge.isEmpty()) continue;

            var name = badge.getHoverName().getString();
            badgeCounts.merge(name, 1L, Long::sum);
        }
    }

    private static String formatBadgeCounts(Map<String, Long> badgeCounts) {
        return badgeCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(e -> e.getValue() + "x " + e.getKey())
                .collect(Collectors.joining(", "));
    }

    private static int calculateResultCount(Map<String, Long> badgeCounts, boolean isUnique) {
        return isUnique
                ? badgeCounts.size()
                : badgeCounts.values().stream().mapToInt(Long::intValue).sum();
    }
}
