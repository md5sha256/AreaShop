package me.wiefferink.areashop.commands.util;

import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.AddCommand;
import me.wiefferink.areashop.commands.AddFriendCommand;
import me.wiefferink.areashop.commands.AddSignCommand;
import me.wiefferink.areashop.commands.BuyCommand;
import me.wiefferink.areashop.commands.DelCommand;
import me.wiefferink.areashop.commands.DelFriendCommand;
import me.wiefferink.areashop.commands.DelSignCommand;
import me.wiefferink.areashop.commands.FindCommand;
import me.wiefferink.areashop.commands.GroupAddCommand;
import me.wiefferink.areashop.commands.GroupDelCommand;
import me.wiefferink.areashop.commands.GroupInfoCommand;
import me.wiefferink.areashop.commands.GroupListCommand;
import me.wiefferink.areashop.commands.HelpCommand;
import me.wiefferink.areashop.commands.InfoBaseCommand;
import me.wiefferink.areashop.commands.InfoCommand;
import me.wiefferink.areashop.commands.InfoPlayerCommand;
import me.wiefferink.areashop.commands.InfoRegionCommand;
import me.wiefferink.areashop.commands.LinkSignsCommand;
import me.wiefferink.areashop.commands.MeCommand;
import me.wiefferink.areashop.commands.MessageCommand;
import me.wiefferink.areashop.commands.QuickBuyCommand;
import me.wiefferink.areashop.commands.QuickDeleteCommand;
import me.wiefferink.areashop.commands.QuickRentCommand;
import me.wiefferink.areashop.commands.ReloadCommand;
import me.wiefferink.areashop.commands.RentCommand;
import me.wiefferink.areashop.commands.ResellCommand;
import me.wiefferink.areashop.commands.SchematicEventCommand;
import me.wiefferink.areashop.commands.SellCommand;
import me.wiefferink.areashop.commands.SetDurationCommand;
import me.wiefferink.areashop.commands.SetLandlordCommand;
import me.wiefferink.areashop.commands.SetOwnerCommand;
import me.wiefferink.areashop.commands.SetPriceCommand;
import me.wiefferink.areashop.commands.SetRestoreCommand;
import me.wiefferink.areashop.commands.SetTeleportCommand;
import me.wiefferink.areashop.commands.SetTransferCommand;
import me.wiefferink.areashop.commands.StackCommand;
import me.wiefferink.areashop.commands.StopResellCommand;
import me.wiefferink.areashop.commands.TeleportCommand;
import me.wiefferink.areashop.commands.ToggleHomeCommand;
import me.wiefferink.areashop.commands.TransferCommand;
import me.wiefferink.areashop.commands.UnrentCommand;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.commands.util.commandsource.CommandSourceMapper;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.incendo.cloud.exception.ArgumentParseException;
import org.incendo.cloud.exception.CommandExecutionException;
import org.incendo.cloud.exception.InvalidCommandSenderException;
import org.incendo.cloud.exception.handling.ExceptionController;
import org.incendo.cloud.exception.handling.ExceptionHandler;
import org.incendo.cloud.execution.ExecutionCoordinator;
import org.incendo.cloud.paper.PaperCommandManager;
import org.incendo.cloud.processors.cache.GuavaCache;
import org.incendo.cloud.processors.confirmation.ConfirmationConfiguration;
import org.incendo.cloud.processors.confirmation.ConfirmationManager;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class AreashopCommands {

    private static final List<Class<? extends AreashopCommandBean>> COMMAND_CLASSES = List.of(
            AddCommand.class,
            AddFriendCommand.class,
            AddSignCommand.class,
            BuyCommand.class,
            DelCommand.class,
            DelFriendCommand.class,
            DelSignCommand.class,
            FindCommand.class,
            GroupAddCommand.class,
            GroupDelCommand.class,
            GroupInfoCommand.class,
            GroupListCommand.class,
            HelpCommand.class,
            InfoCommand.class,
            InfoBaseCommand.class,
            InfoPlayerCommand.class,
            InfoRegionCommand.class,
            LinkSignsCommand.class,
            MeCommand.class,
            MessageCommand.class,
            QuickBuyCommand.class,
            QuickDeleteCommand.class,
            QuickRentCommand.class,
            ReloadCommand.class,
            RentCommand.class,
            ResellCommand.class,
            SchematicEventCommand.class,
            SellCommand.class,
            SetDurationCommand.class,
            SetLandlordCommand.class,
            SetOwnerCommand.class,
            SetPriceCommand.class,
            SetRestoreCommand.class,
            SetTeleportCommand.class,
            SetTransferCommand.class,
            StackCommand.class,
            StopResellCommand.class,
            TeleportCommand.class,
            ToggleHomeCommand.class,
            TransferCommand.class,
            UnrentCommand.class
    );

    private final MessageBridge messageBridge;

    private final Injector injector;
    private final PaperCommandManager<CommandSource<?>> commandManager;

    private final List<AreashopCommandBean> commands = new ArrayList<>();
    private HelpRenderer helpRenderer;

    @Inject
    AreashopCommands(@Nonnull Injector injector, @Nonnull Plugin plugin, @Nonnull MessageBridge messageBridge) {
        this.injector = injector;
        this.commandManager = PaperCommandManager.builder(new CommandSourceMapper())
                .executionCoordinator(ExecutionCoordinator.simpleCoordinator())
                .buildOnEnable(plugin);
        this.messageBridge = messageBridge;
    }

    public void registerCommands() {
        this.commands.clear();
        initCommandManager();
        var builder = this.commandManager.commandBuilder("areashop", "as");
        for (Class<? extends AreashopCommandBean> commandClass : COMMAND_CLASSES) {
            AreashopCommandBean commandBean = injector.getInstance(commandClass);
            this.commands.add(commandBean);
            var configuredBuilder = commandBean.configureCommand(builder);
            this.commandManager.command(configuredBuilder);
        }
        // Show help by default
        this.commandManager.command(builder.handler(context -> showHelp(context.sender().sender())));
        this.helpRenderer = new HelpRenderer(this.messageBridge, this.commands);
    }

    private void initCommandManager() {
        ExceptionController<CommandSource<?>> exceptionController = this.commandManager.exceptionController();
        // We need to unwrap ArgumentParseException because they wrap the custom exception messages
        exceptionController.registerHandler(ArgumentParseException.class,
                ExceptionHandler.unwrappingHandler(AreaShopCommandException.class));
        exceptionController.registerHandler(CommandExecutionException.class,
                ExceptionHandler.unwrappingHandler(AreaShopCommandException.class));
        exceptionController.registerHandler(InvalidCommandSenderException.class,
                new InvalidCommandSenderHandler(this.messageBridge));
        exceptionController.registerHandler(AreaShopCommandException.class,
                new ArgumentParseExceptionHandler<>(this.messageBridge));
        var confirmationConfiguration = ConfirmationConfiguration.<CommandSource<?>>builder()
                .cache(GuavaCache.of(CacheBuilder.newBuilder().build()))
                .noPendingCommandNotifier(x -> {
                })
                .confirmationRequiredNotifier((x, y) -> {
                })
                .build();
        var confirmationManager = ConfirmationManager.confirmationManager(confirmationConfiguration);
        commandManager.registerCommandPostProcessor(confirmationManager.createPostprocessor());
    }

    public void showHelp(@Nonnull CommandSender sender) {
        if (this.helpRenderer == null) {
            throw new IllegalStateException("Command handler not yet initialized!");
        }
        this.helpRenderer.showHelp(sender);
    }

}
