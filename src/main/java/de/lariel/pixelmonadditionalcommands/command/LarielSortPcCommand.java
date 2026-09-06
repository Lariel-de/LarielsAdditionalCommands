package de.lariel.pixelmonadditionalcommands.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.pixelmonmod.pixelmon.api.config.PixelmonConfigProxy;
import com.pixelmonmod.pixelmon.api.pokemon.Pokemon;
import com.pixelmonmod.pixelmon.api.registries.PixelmonSpecies;
import com.pixelmonmod.pixelmon.api.storage.PCStorage;
import com.pixelmonmod.pixelmon.api.storage.StorageProxy;
import com.pixelmonmod.pixelmon.client.gui.pc.sorting.DexNoPredicate;
import com.pixelmonmod.pixelmon.client.gui.pc.sorting.PCSortingLogic;
import com.pixelmonmod.pixelmon.client.gui.pc.sorting.ShinyPredicate;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LarielSortPcCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("larielsortpc")
                .requires(src -> src.hasPermission(2))
                .then(Commands.argument("mode", StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            builder.suggest("dex");
                            builder.suggest("shiny");
                            return builder.buildFuture();
                        })
                        .then(Commands.argument("ascending", BoolArgumentType.bool())
                                .then(Commands.argument("fromBox", IntegerArgumentType.integer(1, 1000))
                                        .suggests((ctx, builder) -> {
                                            var boxCount = getBoxCount(ctx);
                                            for (int i = 1; i <= boxCount; i++) builder.suggest(i);
                                            return builder.buildFuture();
                                        })
                                        .then(Commands.argument("toBox", IntegerArgumentType.integer(1, 1000))
                                                .suggests((ctx, builder) -> {
                                                    int from = 1;
                                                    try {
                                                        from = IntegerArgumentType.getInteger(ctx, "fromBox");
                                                    } catch (Exception ignored) {
                                                    }

                                                    var boxCount = getBoxCount(ctx);
                                                    for (int i = from; i <= boxCount; i++) builder.suggest(i);
                                                    return builder.buildFuture();
                                                })
                                                // Variant without rename
                                                .executes(ctx -> executeSort(
                                                        ctx,
                                                        StringArgumentType.getString(ctx, "mode"),
                                                        BoolArgumentType.getBool(ctx, "ascending"),
                                                        IntegerArgumentType.getInteger(ctx, "fromBox"),
                                                        IntegerArgumentType.getInteger(ctx, "toBox"),
                                                        false // rename default
                                                ))
                                                // Variant with rename
                                                .then(Commands.argument("rename", BoolArgumentType.bool())
                                                        .suggests((ctx, builder) -> {
                                                            builder.suggest("true");
                                                            builder.suggest("false");
                                                            return builder.buildFuture();
                                                        })
                                                        .executes(ctx -> executeSort(
                                                                ctx,
                                                                StringArgumentType.getString(ctx, "mode"),
                                                                BoolArgumentType.getBool(ctx, "ascending"),
                                                                IntegerArgumentType.getInteger(ctx, "fromBox"),
                                                                IntegerArgumentType.getInteger(ctx, "toBox"),
                                                                BoolArgumentType.getBool(ctx, "rename")
                                                        ))
                                                )
                                        )
                                )
                        )
                )
        );
    }

    private static int getBoxCount(CommandContext<CommandSourceStack> ctx) {
        var player = ctx.getSource().getPlayer();

        if (player == null) return 30;

        return PixelmonConfigProxy.getStorage().getComputerBoxes(player.getUUID());
    }

    private static int executeSort(CommandContext<CommandSourceStack> ctx, String mode, boolean ascending,
                                   int fromBox1Based, int toBox1Based, boolean rename) throws CommandSyntaxException {
        var player = ctx.getSource().getPlayerOrException();
        var pc = StorageProxy.getPCForPlayerNow(player);

        // 1-based entry → 0-based internal values
        var fromBox = fromBox1Based - 1;
        var toBox = toBox1Based - 1;

        if (toBox < fromBox) {
            ctx.getSource().sendFailure(Component.literal("toBox must be >= fromBox"));
            return 0;
        }

        if (pc == null) return -1;

        var logic = mode.equalsIgnoreCase("shiny") ? new ShinyPredicate() : new DexNoPredicate();

        sortBoxes(pc, logic, ascending, fromBox, toBox);

        if (rename) renameBoxesByGeneration(pc, fromBox, toBox);

        // Refresh the complete client-side PC state, including empty slots and box metadata.
        pc.initialize(player);

        ctx.getSource().sendSuccess(
                () -> Component.literal("Sorted boxes " + fromBox1Based + " to " + toBox1Based),
                true
        );

        return 1;
    }

    private static void sortBoxes(PCStorage storage, PCSortingLogic logic, boolean ascending, int fromBox, int toBox) {

        // Collect all pokes from the boxes in range
        List<Pokemon> list = new ArrayList<>();

        for (var box = fromBox; box <= toBox; box++) {
            for (var slot = 0; slot < 30; slot++) {
                var p = storage.getBox(box).get(slot);
                if (p != null) {
                    list.add(p);
                }
            }
        }

        // 2. Sort
        list.sort(logic.comparator(ascending));

        // 3. Write back
        var index = 0;
        for (var box = fromBox; box <= toBox; box++) {
            for (var slot = 0; slot < 30; slot++) {

                var p = (index < list.size()) ? list.get(index) : null;
                storage.getBox(box).set(slot, p);

                index++;
            }
        }
    }

    private static void renameBoxesByGeneration(PCStorage storage, int fromBox, int toBox) {
        for (var box = fromBox; box <= toBox; box++) {

            Set<Integer> gens = new HashSet<>();
            var pcBox = storage.getBox(box);

            for (var slot = 0; slot < 30; slot++) {
                var p = pcBox.get(slot);
                if (p != null) {
                    var dex = p.getSpecies().getDex();
                    var species = PixelmonSpecies.fromDex(dex);

                    if (species.isEmpty()) continue;

                    var gen = species.get().getGeneration();
                    gens.add(gen);
                }
            }

            if (gens.isEmpty()) {
                continue;
            }

            // Check for several generations → check, if they are consecutive
            List<Integer> list = new ArrayList<>(gens);
            boolean consecutive = true;

            for (int i = 1; i < list.size(); i++) {
                if (list.get(i) != list.get(i - 1) + 1) {
                    consecutive = false;
                    break;
                }
            }

            if (consecutive) {
                // Choose compact display: Gen 1–3
                pcBox.setName("Gen " + list.getFirst() + "–" + list.getLast());
                continue;
            }

            // Not consecutive → Gen 1 + 3 + 5
            String name = list.stream().map(Object::toString).reduce((a, b) -> a + " + " + b).map(s -> "Gen " + s).orElse("Mixed");
            pcBox.setName(name);
        }
    }
}