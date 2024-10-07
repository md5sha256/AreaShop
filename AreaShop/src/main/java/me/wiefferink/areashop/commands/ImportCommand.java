package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AcceptedValuesParser;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.regions.ImportJobFactory;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.ParserDescriptor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;

@Singleton
public class ImportCommand extends AreashopCommandBean {

    private static final CloudKey<String> KEY_SOURCE = CloudKey.of("source", String.class);

    /* RegionGroup priority usage:
       0: Settings from /config.yml
       1: Settings from /worlds/<world>/config.yml
       2: Settings from /worlds/<world>/parent-regions.yml (if their priority is set it is added to this value)
     */
    private final ImportJobFactory importJobFactory;
    private final MessageBridge messageBridge;

    @Inject
    public ImportCommand(@Nonnull MessageBridge messageBridge, @Nonnull ImportJobFactory importJobFactory) {
        this.messageBridge = messageBridge;
        this.importJobFactory = importJobFactory;
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    public String getHelpKey(@NotNull CommandSender target) {
        if(target.hasPermission("areashop.import")) {
            return "help-import";
        }
        return null;
    }

    @Override
    protected @Nonnull Command.Builder<? extends CommandSource<?>> configureCommand(@Nonnull Command.Builder<CommandSource<?>> builder) {
        withConfirmation();
        var sourceParser = AcceptedValuesParser.ofConstant(List.of("RegionForSale"), "import-wrongSource", true);
        return builder.literal("import")
                .required(KEY_SOURCE, ParserDescriptor.of(sourceParser, String.class))
                .handler(this::handleCommand);
    }

    @Override
    protected @Nonnull CommandProperties properties() {
        return CommandProperties.of("import");
    }

    // TODO:
    //  - Landlord?
    //  - Friends
    //  - Region flags?
    //  - Settings from the 'permissions' section in RegionForSale/config.yml?

    private void handleCommand(@Nonnull CommandContext<CommandSource<?>> context) {
        CommandSender sender = context.sender().sender();
        if (!sender.hasPermission("areashop.import")) {
            this.messageBridge.message(sender, "import-noPermission");
            return;
        }
        String importSource = context.get(KEY_SOURCE);
        if (!"RegionForSale".equalsIgnoreCase(importSource)) {
            throw new AreaShopCommandException("import-wrongSource");
        }
        importJobFactory.createImportJob(sender).execute();
    }

}










