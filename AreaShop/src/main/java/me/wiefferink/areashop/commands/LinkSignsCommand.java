package me.wiefferink.areashop.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.SignProfileUtil;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.commands.util.commandsource.PlayerCommandSource;
import me.wiefferink.areashop.managers.SignLinkerManager;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

@Singleton
public class LinkSignsCommand extends AreashopCommandBean {

    private final SignLinkerManager signLinkerManager;
    private final AreaShop plugin;

    private final CommandFlag<String> profileFlag;

    @Inject
    public LinkSignsCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull SignLinkerManager signLinkerManager,
            @Nonnull AreaShop plugin
    ) {
        this.signLinkerManager = signLinkerManager;
        this.plugin = plugin;
        this.profileFlag = SignProfileUtil.createDefault(plugin);
    }

    @Override
    public String stringDescription() {
        return null;
    }


    @Override
    protected Command.Builder<? extends CommandSource<?>> configureCommand(Command.@NotNull Builder<CommandSource<?>> builder) {
        return builder.literal("linksign")
                .senderType(PlayerCommandSource.class)
                .flag(this.profileFlag)
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("linksign");
    }

    @Override
    public String getHelpKey(CommandSender target) {
        if (target.hasPermission("areashop.linksigns")) {
            return "help-linksigns";
        }
        return null;
    }

    private void handleCommand(@Nonnull CommandContext<PlayerCommandSource> context) {
        Player player = context.sender().sender();
        if (!player.hasPermission("linksigns")) {
            throw new AreaShopCommandException("linksigns-noPermission");
        }
        if (signLinkerManager.isInSignLinkMode(player)) {
            signLinkerManager.exitSignLinkMode(player);
            return;
        }
        // Get the profile
        String profile = SignProfileUtil.getOrParseProfile(context, this.plugin);
        this.signLinkerManager.enterSignLinkMode(player, profile);
    }

}










