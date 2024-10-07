package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.RegionParseUtil;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;

@Singleton
public class SetPriceCommand extends AreashopCommandBean {


    private static final CloudKey<String> KEY_PRICE = CloudKey.of("price", String.class);
    private final CommandFlag<GeneralRegion> regionFlag;
    private final MessageBridge messageBridge;

    @Inject
    public SetPriceCommand(@Nonnull MessageBridge messageBridge, @Nonnull IFileManager fileManager) {
        this.messageBridge = messageBridge;
        this.regionFlag = RegionParseUtil.createDefault(fileManager);
    }

    @Override
    public String getHelpKey(CommandSender target) {
        if (target.hasPermission("areashop.setprice")) {
            return "help-setprice";
        }
        return null;
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    protected Command.Builder<? extends CommandSource<?>> configureCommand(Command.@NotNull Builder<CommandSource<?>> builder) {
        return builder.literal("setprice")
                .required(KEY_PRICE, StringParser.stringParser())
                .flag(this.regionFlag)
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("setprice");
    }

    private void handleCommand(@Nonnull CommandContext<CommandSource<?>> context) {
        CommandSender sender = context.sender().sender();
        if (!sender.hasPermission("areashop.setprice") && (!sender.hasPermission("areashop.setprice.landlord") && sender instanceof Player)) {
            this.messageBridge.message(sender, "setprice-noPermission");
            return;
        }
        GeneralRegion region = RegionParseUtil.getOrParseRegion(context, sender, this.regionFlag);
        if (!sender.hasPermission("areashop.setprice")
                && !(sender instanceof Player player && region.isLandlord(player.getUniqueId()))) {
            this.messageBridge.message(sender, "setprice-noLandlord", region);
            return;
        }
        String rawPrice = context.get(KEY_PRICE);
        if ("default".equalsIgnoreCase(rawPrice) || "reset".equalsIgnoreCase(rawPrice)) {
            if (region instanceof RentRegion rentRegion) {
                rentRegion.setPrice(null);
            } else if (region instanceof BuyRegion buyRegion) {
                buyRegion.setPrice(null);
            }
            region.update();
            this.messageBridge.message(sender, "setprice-successRemoved", region);
            return;
        }
        double price;
        try {
            price = Double.parseDouble(rawPrice);
        } catch (NumberFormatException e) {
            this.messageBridge.message(sender, "setprice-wrongPrice", rawPrice, region);
            return;
        }
        if (region instanceof RentRegion rentRegion) {
            rentRegion.setPrice(price);
            this.messageBridge.message(sender, "setprice-successRent", region);
        } else if (region instanceof BuyRegion buyRegion) {
            buyRegion.setPrice(price);
            this.messageBridge.message(sender, "setprice-successBuy", region);
        }
        region.update();
    }

}
