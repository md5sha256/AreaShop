package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.RegionGroupParser;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.commands.util.commandsource.PlayerCommandSource;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RegionGroup;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.Utils;
import me.wiefferink.interactivemessenger.processing.Message;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.standard.DoubleParser;
import org.incendo.cloud.parser.standard.EnumParser;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Singleton
public class FindCommand extends AreashopCommandBean {

    private static final CloudKey<GeneralRegion.RegionType> KEY_REGION_TYPE = CloudKey.of("regionType",
            GeneralRegion.RegionType.class);
    private static final CloudKey<Double> KEY_PRICE = CloudKey.of("maxPrice", Double.class);

    private final Economy economy;
    private final IFileManager fileManager;
    private final CommandFlag<RegionGroup> regionGroupFlag;

    private final MessageBridge messageBridge;

    @Inject
    public FindCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull Economy economy,
            @Nonnull IFileManager fileManager
    ) {
        this.messageBridge = messageBridge;
        this.economy = economy;
        this.fileManager = fileManager;
        this.regionGroupFlag = CommandFlag.builder("region")
                .withComponent(ParserDescriptor.of(new RegionGroupParser<>(fileManager, "find-wrongGroup"),
                        RegionGroup.class))
                .build();
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    public String getHelpKey(@NotNull CommandSender target) {
        if(target.hasPermission("areashop.find")) {
            return "help-find";
        }
        return null;
    }

    @Override
    protected @Nonnull Command.Builder<? extends CommandSource<?>> configureCommand(@Nonnull Command.Builder<CommandSource<?>> builder) {
        return builder.literal("find")
                .senderType(PlayerCommandSource.class)
                .required(KEY_REGION_TYPE, EnumParser.enumParser(GeneralRegion.RegionType.class))
                .optional(KEY_PRICE, DoubleParser.doubleParser(0))
                .flag(this.regionGroupFlag)
                .handler(this::handleCommand);
    }

    @Override
    protected @Nonnull CommandProperties properties() {
        return CommandProperties.of("find");
    }

    private void handleCommand(@Nonnull CommandContext<PlayerCommandSource> context) {
        Player sender = context.sender().sender();
        if (!sender.hasPermission("areashop.find")) {
            throw new AreaShopCommandException("find-noPermission");
        }
        double balance;
        if (economy != null) {
            balance = economy.getBalance(sender);
        } else {
            balance = 0;
        }
        boolean maxPriceSet = context.contains(KEY_PRICE);
        double maxPrice = context.getOrDefault(KEY_PRICE, Double.MAX_VALUE);
        RegionGroup group = context.flags().get(this.regionGroupFlag);
        GeneralRegion.RegionType regionType = context.get(KEY_REGION_TYPE);
        Message onlyInGroup;
        if (group != null) {
            onlyInGroup = Message.fromKey("find-onlyInGroup").replacements(group.getName());
        } else {
            onlyInGroup = Message.empty();
        }
        switch (regionType) {
            case BUY -> handleBuy(sender, balance, maxPrice, maxPriceSet, onlyInGroup, group);
            case RENT -> handleRent(sender, balance, maxPrice, maxPriceSet, onlyInGroup, group);
        }
    }

    private void handleBuy(@Nonnull Player sender,
                           double balance,
                           double maxPrice,
                           boolean maxPriceSet,
                           @Nonnull Message onlyInGroup,
                           RegionGroup group
    ) {
        Collection<BuyRegion> regions = fileManager.getBuysRef();
        List<BuyRegion> results = new LinkedList<>();
        for (BuyRegion region : regions) {
            if (!region.isSold()
                    && ((region.getPrice() <= balance && !maxPriceSet) || (region.getPrice() <= maxPrice && maxPriceSet))
                    && (group == null || group.isMember(region))
                    && (region.getBooleanSetting("general.findCrossWorld") || sender.getWorld()
                    .equals(region.getWorld()))
            ) {
                results.add(region);
            }
        }
        if (results.isEmpty()) {
            double currency = maxPriceSet ? maxPrice : balance;
            String key = maxPriceSet ? "find-noneFoundMax" : "find-noneFound";
            throw new AreaShopCommandException(key, "buy", Utils.formatCurrency(currency), onlyInGroup);
        }
        // Draw a random one
        BuyRegion region = results.get(ThreadLocalRandom.current().nextInt(results.size()));
        // Teleport
        double currency = maxPriceSet ? maxPrice : balance;
        String key = maxPriceSet ? "find-successMax" : "find-success";
        this.messageBridge.message(sender,
                key,
                "buy",
                Utils.formatCurrency(currency),
                onlyInGroup,
                region);
        boolean tpToSign = region.getBooleanSetting("general.findTeleportToSign");
        region.getTeleportFeature().teleportPlayer(sender, tpToSign, false);
    }

    private void handleRent(
            @Nonnull Player sender,
            double balance,
            double maxPrice,
            boolean maxPriceSet,
            @Nonnull Message onlyInGroup,
            RegionGroup group
    ) {
        Collection<RentRegion> regions = fileManager.getRentsRef();
        List<RentRegion> results = new LinkedList<>();
        for (RentRegion region : regions) {
            if (!region.isRented()
                    && ((region.getPrice() <= balance && !maxPriceSet) || (region.getPrice() <= maxPrice && maxPriceSet))
                    && (group == null || group.isMember(region))
                    && (region.getBooleanSetting("general.findCrossWorld") || sender.getWorld()
                    .equals(region.getWorld()))
            ) {
                results.add(region);
            }
        }
        if (results.isEmpty()) {
            double currency = maxPriceSet ? maxPrice : balance;
            String key = maxPriceSet ? "find-noneFoundMax" : "find-noneFound";
            throw new AreaShopCommandException(key, "rent", Utils.formatCurrency(currency), onlyInGroup);
        }
        // Draw a random one
        RentRegion region = results.get(ThreadLocalRandom.current().nextInt(results.size()));
        // Teleport
        double currency = maxPriceSet ? maxPrice : balance;
        String key = maxPriceSet ? "find-successMax" : "find-success";
        this.messageBridge.message(sender,
                key,
                "rent",
                Utils.formatCurrency(currency),
                onlyInGroup,
                region);
        boolean tpToSign = region.getBooleanSetting("general.findTeleportToSign");
        region.getTeleportFeature().teleportPlayer(sender, tpToSign, false);
    }

}



























