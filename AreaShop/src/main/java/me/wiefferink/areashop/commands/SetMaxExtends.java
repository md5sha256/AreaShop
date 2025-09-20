package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.RegionParseUtil;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.RentRegion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

@Singleton
public class SetMaxExtends extends AreashopCommandBean {

    private static final CloudKey<Integer> KEY_EXTENDS = CloudKey.of("extends", Integer.class);
    private final MessageBridge messageBridge;
    private final CommandFlag<RentRegion> regionFlag;

    @Inject
    public SetMaxExtends(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager
    ) {
        this.messageBridge = messageBridge;
        this.regionFlag = RegionParseUtil.createDefaultRent(fileManager);
    }

    @Override
    public String getHelpKey(@NotNull CommandSender target) {
        if (target.hasPermission("areashop.setmaxextends")) {
            return "help-setmaxextends";
        }
        return null;
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    protected Command.Builder<? extends CommandSource<?>> configureCommand(Command.@NotNull Builder<CommandSource<?>> builder) {
        return builder.literal("setmaxextends")
                .required(KEY_EXTENDS, IntegerParser.integerParser(0))
                .flag(this.regionFlag)
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("setmaxextends");
    }

    private void handleCommand(@Nonnull CommandContext<CommandSource<?>> context) {
        CommandSender sender = context.sender().sender();
        if (!sender.hasPermission("areashop.setmaxextends") && (!sender.hasPermission("areashop.setmaxextends.landlord") && sender instanceof Player)) {
            this.messageBridge.message(sender, "setmaxextends-noPermission");
            return;
        }
        RentRegion rent = RegionParseUtil.getOrParseRentRegion(context, sender, this.regionFlag);
        if (!sender.hasPermission("areashop.setmaxextends")
                && !(sender instanceof Player player
                && rent.isLandlord(player.getUniqueId()))
        ) {
            this.messageBridge.message(sender, "setmaxextends-noLandlord", rent);
            return;
        }
        int extend = context.get(KEY_EXTENDS);

        sender.sendMessage();
        rent.setMaxExtends(extend);
        rent.update();
        this.messageBridge.message(sender, "setmaxextends-success", rent);
    }

}
