package de.lariel.pixelmonadditionalcommands;

import de.lariel.pixelmonadditionalcommands.command.*;
import de.lariel.pixelmonadditionalcommands.command.tempfixes.LarielPokeBattleCommand;
import de.lariel.pixelmonadditionalcommands.command.tempfixes.LarielPokeTestCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(LarielsAdditionalCommands.MOD_ID)
@EventBusSubscriber(modid = LarielsAdditionalCommands.MOD_ID)
public class LarielsAdditionalCommands {

    public static final String MOD_ID = "larielsadditionalcommands";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    private static LarielsAdditionalCommands instance;

    public LarielsAdditionalCommands(IEventBus bus) {
        instance = this;

        bus.addListener(LarielsAdditionalCommands::onModLoad);
    }

    public static void onModLoad(FMLCommonSetupEvent event) {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerStartingEvent event) {
        // Logic for when the server is starting here
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        // Logic for once the server has started here
    }

    @SubscribeEvent
    public static void onCommandRegister(RegisterCommandsEvent event) {
        LarielSpawnNpcCommand.register(event.getDispatcher());
        event.getDispatcher().register(LarielListPresetsCommand.registerListPresets());
        LarielSetTrainerToLevelCommand.register(event.getDispatcher());

        // temp fixes
        LarielPokeBattleCommand.register(event.getDispatcher());
        LarielPokeTestCommand.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerStopping(ServerStoppingEvent event) {
        // Logic for when the server is stopping
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        // Logic for when the server is stopped
    }

    public static LarielsAdditionalCommands getInstance() {
        return instance;
    }

    public static Logger getLogger() {
        return LOGGER;
    }
}
