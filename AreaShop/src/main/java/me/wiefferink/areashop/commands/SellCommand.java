package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.BuyRegionParser;
import me.wiefferink.areashop.commands.util.RegionParseUtil;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Singleton
public class SellCommand extends AreashopCommandBean {

    private final MessageBridge messageBridge;
    private final IFileManager fileManager;
    private final CommandFlag<BuyRegion> buyRegionFlag;

    @Inject
    public SellCommand(@Nonnull MessageBridge messageBridge, @Nonnull IFileManager fileManager) {
        ParserDescriptor<CommandSource<?>, BuyRegion> regionParser =
                ParserDescriptor.of(new BuyRegionParser<>(fileManager, this::suggestBuyRegions), BuyRegion.class);
        this.messageBridge = messageBridge;
        this.fileManager = fileManager;
        this.buyRegionFlag = CommandFlag.<CommandSource<?>>builder("region").withComponent(regionParser).build();
    }

    /**
     * Check if a person can sell the region.
     *
     * @param person The person to check
     * @param region The region to check for
     * @return true if the person can sell it, otherwise false
     */
    public static boolean canUse(CommandSender person, GeneralRegion region) {
        if (person.hasPermission("areashop.sell")) {
            return true;
        }
        if (person instanceof Player player) {
            return region.isOwner(player) && person.hasPermission("areashop.sellown");
        }
        return false;
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    protected Command.Builder<? extends CommandSource<?>> configureCommand(Command.@NotNull Builder<CommandSource<?>> builder) {
        return builder.literal("sell")
                .flag(this.buyRegionFlag)
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("sell");
    }

    @Override
    public String getHelpKey(CommandSender target) {
        if (target.hasPermission("areashop.sell") || target.hasPermission("areashop.sellown")) {
            return "help-sell";
        }
        return null;
    }

    private void handleCommand(@Nonnull CommandContext<CommandSource<?>> context) {
        CommandSender sender = context.sender().sender();
        if (!sender.hasPermission("areashop.sell") && !sender.hasPermission("areashop.sellown")) {
            this.messageBridge.message(sender, "sell-noPermission");
            return;
        }
        BuyRegion buy = RegionParseUtil.getOrParseBuyRegion(context, sender, this.buyRegionFlag);
        if (!buy.isSold()) {
            messageBridge.message(sender, "sell-notBought", buy);
            return;
        }
        buy.sell(true, sender);
    }

    private CompletableFuture<Iterable<Suggestion>> suggestBuyRegions(
            @Nonnull CommandContext<CommandSource<?>> context,
            @Nonnull CommandInput input
    ) {
        String text = input.peekString();
        List<Suggestion> suggestions = this.fileManager.getBuysRef().stream()
                .filter(BuyRegion::isSold)
                .map(GeneralRegion::getName)
                .filter(name -> name.startsWith(text))
                .map(Suggestion::suggestion)
                .toList();
        return CompletableFuture.completedFuture(suggestions);
    }
}
















