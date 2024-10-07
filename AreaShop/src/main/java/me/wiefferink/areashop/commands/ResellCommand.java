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
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.standard.DoubleParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Singleton
public class ResellCommand extends AreashopCommandBean {

    private static final CloudKey<Double> KEY_PRICE = CloudKey.of("price", Double.class);
    private final CommandFlag<BuyRegion> regionFlag;
    private final MessageBridge messageBridge;
    private final IFileManager fileManager;

    @Inject
    public ResellCommand(@Nonnull MessageBridge messageBridge, @Nonnull IFileManager fileManager) {
        this.messageBridge = messageBridge;
        this.fileManager = fileManager;
        ParserDescriptor<CommandSource<?>, BuyRegion> regionParser = ParserDescriptor.of(
                new BuyRegionParser<>(fileManager, this::suggestBuyRegions),
                BuyRegion.class
        );
        this.regionFlag = CommandFlag.<CommandSource<?>>builder("region").withComponent(regionParser).build();
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    @Nonnull
    protected Command.Builder<? extends CommandSource<?>> configureCommand(@Nonnull Command.Builder<CommandSource<?>> builder) {
        return builder.literal("resell")
                .required(KEY_PRICE, DoubleParser.doubleParser(0))
                .flag(this.regionFlag)
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("resell");
    }


    @Override
    public String getHelpKey(@NotNull CommandSender target) {
        if (target.hasPermission("areashop.resellall") || target.hasPermission("areashop.resell")) {
            return "help-resell";
        }
        return null;
    }


    private void handleCommand(@Nonnull CommandContext<CommandSource<?>> context) {
        CommandSender sender = context.sender().sender();
        if (!sender.hasPermission("areashop.resell") && !sender.hasPermission("areashop.resellall")) {
            messageBridge.message(sender, "resell-noPermissionOther");
            return;
        }
        double price = context.get(KEY_PRICE);
        BuyRegion buy = RegionParseUtil.getOrParseBuyRegion(context, sender, this.regionFlag);
        if (!buy.isSold()) {
            messageBridge.message(sender, "resell-notBought", buy);
            return;
        }
        if (sender.hasPermission("areashop.resellall")) {
            buy.enableReselling(price);
            buy.update();
            messageBridge.message(sender, "resell-success", buy);
        } else if (sender.hasPermission("areashop.resell") && sender instanceof Player player) {
            if (!buy.isOwner(player)) {
                messageBridge.message(sender, "resell-noPermissionOther", buy);
                return;
            }

            if (buy.getBooleanSetting("buy.resellDisabled")) {
                messageBridge.message(sender, "resell-disabled", buy);
                return;
            }

            buy.enableReselling(price);
            buy.update();
            messageBridge.message(sender, "resell-success", buy);
        } else {
            messageBridge.message(sender, "resell-noPermission", buy);
        }
    }

    private CompletableFuture<Iterable<Suggestion>> suggestBuyRegions(
            @Nonnull CommandContext<CommandSource<?>> context,
            @Nonnull CommandInput input
    ) {
        String text = input.peekString();
        List<Suggestion> suggestions = this.fileManager.getBuysRef().stream()
                .filter(region -> region.isSold() && !region.isInResellingMode())
                .map(GeneralRegion::getName)
                .filter(name -> name.startsWith(text))
                .map(Suggestion::suggestion)
                .toList();
        return CompletableFuture.completedFuture(suggestions);
    }

}
















