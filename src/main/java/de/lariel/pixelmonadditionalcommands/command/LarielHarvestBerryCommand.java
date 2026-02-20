package de.lariel.pixelmonadditionalcommands.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pixelmonmod.pixelmon.blocks.ApricornLeavesBlock;
import com.pixelmonmod.pixelmon.blocks.BerryLeavesBlock;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class LarielHarvestBerryCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {

        // Variant A: radius
        dispatcher.register(Commands.literal("larielharvestradius")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("radius", IntegerArgumentType.integer(1, 128))
                        .suggests((ctx, builder) -> {
                            var suggestions = List.of(5, 10, 15, 20, 25, 32, 48, 64);
                            for (int r : suggestions) {
                                builder.suggest(r);
                            }
                            return builder.buildFuture();
                        })
                        .executes(ctx -> harvestRadius(
                                ctx,
                                IntegerArgumentType.getInteger(ctx, "radius")
                        ))
                )
        );

        // Variant B: area (like /fill)
        dispatcher.register(Commands.literal("larielharvestarea")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("from", BlockPosArgument.blockPos())
                        .then(Commands.argument("to", BlockPosArgument.blockPos())
                                .executes(ctx -> harvestArea(
                                        ctx,
                                        BlockPosArgument.getLoadedBlockPos(ctx, "from"),
                                        BlockPosArgument.getLoadedBlockPos(ctx, "to")
                                ))
                        )
                )
        );
    }

    private static int harvestRadius(CommandContext<CommandSourceStack> context, int radius) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();
        var origin = player.blockPosition();

        var min = origin.offset(-radius, -2, -radius);
        var max = origin.offset(radius, 5, radius);

        return harvestInBox(context, min, max, player);
    }

    private static int harvestArea(CommandContext<CommandSourceStack> context, BlockPos from, BlockPos to) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();

        // Sort Coordinates (like /fill)
        var min = new BlockPos(
                Math.min(from.getX(), to.getX()),
                Math.min(from.getY(), to.getY()),
                Math.min(from.getZ(), to.getZ())
        );

        var max = new BlockPos(
                Math.max(from.getX(), to.getX()),
                Math.max(from.getY(), to.getY()),
                Math.max(from.getZ(), to.getZ())
        );

        return harvestInBox(context, min, max, player);
    }

    private static int harvestInBox(CommandContext<CommandSourceStack> context, BlockPos min, BlockPos max, ServerPlayer player) {
        var level = player.serverLevel();
        var harvested = new AtomicInteger();

        // 1. Harvest berries
        BlockPos.betweenClosedStream(min, max).forEach(pos -> {
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof BerryLeavesBlock) && !(state.getBlock() instanceof ApricornLeavesBlock)) {
                return;
            }

            int age = state.getValue(BerryLeavesBlock.AGE);
            if (age < 2) {
                return;
            }

            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            var result = state.useWithoutItem(level, player, hit);
            if (result.consumesAction()) {
                harvested.incrementAndGet();
            }
        });

        // 2. Collect items
        var box = new AABB(
                min.getX(), min.getY(), min.getZ(),
                max.getX() + 1, max.getY() + 1, max.getZ() + 1
        );

        var items = level.getEntitiesOfClass(ItemEntity.class, box);

        for (var item : items) {
            var stack = item.getItem();

            if (!stack.isEmpty() && player.getInventory().add(stack)) {
                item.discard();
            }
        }

        context.getSource().sendSuccess(
                () -> Component.literal("Harvested " + harvested.get() + " leaves."),
                true
        );

        return harvested.get();
    }
}
