package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.RegionParseUtil;
import me.wiefferink.areashop.commands.util.RentRegionParser;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
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
public class UnrentCommand extends AreashopCommandBean {
    private final IFileManager fileManager;
    private final CommandFlag<RentRegion> regionFlag;

    @Inject
    public UnrentCommand(@Nonnull IFileManager fileManager) {
        ParserDescriptor<CommandSource<?>, RentRegion> regionParser =
                ParserDescriptor.of(new RentRegionParser<>(fileManager, this::suggestRegions), RentRegion.class);
        this.fileManager = fileManager;
        this.regionFlag = CommandFlag.<CommandSource<?>>builder("region")
                .withComponent(regionParser)
                .build();
    }

    /**
     * Check if a person can unrent the region.
     *
     * @param person The person to check
     * @param region The region to check for
     * @return true if the person can unrent it, otherwise false
     */
    public static boolean canUse(CommandSender person, GeneralRegion region) {
        if (person.hasPermission("areashop.unrent")) {
            return true;
        }
        if (person instanceof Player player) {
            return region.isOwner(player) && person.hasPermission("areashop.unrentown");
        }
        return false;
    }

    @Override
    public String getHelpKey(CommandSender target) {
        if (target.hasPermission("areashop.unrent") || target.hasPermission("areashop.unrentown")) {
            return "help-unrent";
        }
        return null;
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    protected Command.Builder<? extends CommandSource<?>> configureCommand(Command.@NotNull Builder<CommandSource<?>> builder) {
        return builder.literal("unrent")
                .flag(this.regionFlag)
                .handler(this::handleCommand);
    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("unrent");
    }

    private void handleCommand(@Nonnull CommandContext<CommandSource<?>> context) {
        CommandSender sender = context.sender().sender();
        if (!sender.hasPermission("areashop.unrent") && !sender.hasPermission("areashop.unrentown")) {
            throw new AreaShopCommandException("unrent-noPermission");
        }
        RentRegion rent = RegionParseUtil.getOrParseRentRegion(context, sender, this.regionFlag);
        if (!rent.isRented()) {
            throw new AreaShopCommandException("unrent-notRented", rent);
        }
        rent.unRent(true, sender);
    }

    @Nonnull
    private CompletableFuture<Iterable<Suggestion>> suggestRegions(
            @Nonnull CommandContext<CommandSource<?>> context,
            @Nonnull CommandInput input
    ) {
        String text = input.peekString();
        List<Suggestion> suggestions = this.fileManager.getRentsRef().stream()
                .filter(RentRegion::isRented)
                .map(GeneralRegion::getName)
                .filter(name -> name.startsWith(text))
                .map(Suggestion::suggestion)
                .toList();
        return CompletableFuture.completedFuture(suggestions);
    }
}








