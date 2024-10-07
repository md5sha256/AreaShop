package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.adapters.platform.OfflinePlayerHelper;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.OfflinePlayerParser;
import me.wiefferink.areashop.commands.util.RegionInfoUtil;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.tools.BukkitSchedulerExecutor;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;

import javax.annotation.Nonnull;

@Singleton
public class InfoPlayerCommand extends AreashopCommandBean {

    private static final CloudKey<String> KEY_PLAYER = CloudKey.of("player", String.class);

    private final MessageBridge messageBridge;
    private final IFileManager fileManager;
    private final OfflinePlayerHelper offlinePlayerHelper;
    private final BukkitSchedulerExecutor executor;

    @Inject
    public InfoPlayerCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager,
            @Nonnull OfflinePlayerHelper offlinePlayerHelper,
            @Nonnull BukkitSchedulerExecutor executor
    ) {
        this.messageBridge = messageBridge;
        this.fileManager = fileManager;
        this.offlinePlayerHelper = offlinePlayerHelper;
        this.executor = executor;
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    public String getHelpKey(CommandSender target) {
        return null;
    }

    @Override
    protected @Nonnull Command.Builder<? extends CommandSource<?>> configureCommand(@Nonnull Command.Builder<CommandSource<?>> builder) {
        return builder.literal("info").literal("player")
                .required(KEY_PLAYER, OfflinePlayerParser.parser())
                .handler(this::handleCommand);
    }

    @Override
    protected @Nonnull CommandProperties properties() {
        return CommandProperties.of("info region");
    }

    private void handleCommand(@Nonnull CommandContext<CommandSource<?>> context) {
        CommandSender sender = context.sender().sender();
        if (!sender.hasPermission("areashop.info")) {
            messageBridge.message(sender, "info-noPermission");
            return;
        }
        this.offlinePlayerHelper.lookupOfflinePlayerAsync(context.get(KEY_PLAYER))
                .whenCompleteAsync((offlinePlayer, exception) -> {
                    if (exception != null) {
                        sender.sendMessage("failed to lookup offline player!");
                        exception.printStackTrace();
                        return;
                    }
                    if (!offlinePlayer.hasPlayedBefore() && !offlinePlayer.isOnline()) {
                        this.messageBridge.message(sender, "cmd-invalidPlayer", offlinePlayer.getName());
                        return;
                    }
                    RegionInfoUtil.showRegionInfo(this.messageBridge, this.fileManager, sender, offlinePlayer);
                }, this.executor);
    }

}


























