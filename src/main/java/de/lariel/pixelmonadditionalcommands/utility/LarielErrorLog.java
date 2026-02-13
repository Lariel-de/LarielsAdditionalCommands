package de.lariel.pixelmonadditionalcommands.utility;

import com.mojang.brigadier.context.CommandContext;
import de.lariel.pixelmonadditionalcommands.LarielsAdditionalCommands;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Logger;

public class LarielErrorLog {
    private static final Logger _logger;

    static {
        _logger = LarielsAdditionalCommands.getLogger();
    }

    public static int LogError(String errorMessage, CommandContext<CommandSourceStack> context) {
        return Log(errorMessage, context, Level.ERROR);
    }

    public static int LogDebug(String errorMessage, CommandContext<CommandSourceStack> context) {
        return Log(errorMessage, context, Level.DEBUG);
    }

    private static int Log(String errorMessage, CommandContext<CommandSourceStack> context, Level logLevel) {
        context.getSource().sendFailure(Component.literal(errorMessage));
        _logger.log(logLevel, errorMessage);
        return 0;
    }
}
