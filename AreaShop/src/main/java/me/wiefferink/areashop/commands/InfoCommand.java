package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.RegionGroupParser;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RegionGroup;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.SimpleMessageBridge;
import me.wiefferink.interactivemessenger.processing.Message;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.standard.EnumParser;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Stream;

@Singleton
public class InfoCommand extends AreashopCommandBean {

    private static final CommandFlag<Integer> FLAG_PAGE = CommandFlag.builder("page")
            .withComponent(IntegerParser.integerParser(1))
            .build();
    private static final CloudKey<RegionStateFilterType> KEY_TYPE = CloudKey.of("type", RegionStateFilterType.class);

    private final MessageBridge messageBridge;
    private final IFileManager fileManager;

    private final CommandFlag<RegionGroup> filterGroupFlag;

    @Inject
    public InfoCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager
    ) {
        this.messageBridge = messageBridge;
        this.fileManager = fileManager;
        this.filterGroupFlag = CommandFlag.builder("group")
                .withComponent(ParserDescriptor.of(new RegionGroupParser<>(fileManager, "info-noFiltergroup"),
                        RegionGroup.class))
                .build();
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    public String getHelpKey(@NotNull CommandSender target) {
        return null;
    }

    @Override
    protected @Nonnull Command.Builder<? extends CommandSource<?>> configureCommand(@Nonnull Command.Builder<CommandSource<?>> builder) {
        return builder.literal("info")
                .required(KEY_TYPE, EnumParser.enumParser(RegionStateFilterType.class))
                .flag(FLAG_PAGE)
                .flag(this.filterGroupFlag)
                .handler(this::handleCommand);
    }

    @Override
    protected @Nonnull CommandProperties properties() {
        return CommandProperties.of("info");
    }

    private void handleCommand(@Nonnull CommandContext<CommandSource<?>> context) {
        CommandSender sender = context.sender().sender();
        if (!sender.hasPermission("areashop.info")) {
            messageBridge.message(sender, "info-noPermission");
            return;
        }
        RegionStateFilterType filterType = context.get(KEY_TYPE);
        processOtherFilters(context, filterType);
    }

    private void processOtherFilters(@Nonnull CommandContext<CommandSource<?>> context,
                                     @Nonnull RegionStateFilterType filterType) {
        RegionGroup filterGroup = context.flags().get(this.filterGroupFlag);
        int page = context.flags().getValue(FLAG_PAGE).orElse(1);
        Stream<? extends GeneralRegion> toShow = switch (filterType) {
            case ALL -> fileManager.getRegionsRef().stream();
            case SOLD -> fileManager.getBuysRef().stream().filter(BuyRegion::isSold);
            case RENTED -> fileManager.getRentsRef().stream().filter(RentRegion::isRented);
            case FORRENT -> fileManager.getRentsRef().stream().filter(RentRegion::isAvailable);
            case FORSALE -> fileManager.getBuysRef().stream().filter(BuyRegion::isAvailable);
            case RESELLING -> fileManager.getBuysRef().stream().filter(BuyRegion::isInResellingMode);
            case NOGROUP -> {
                List<GeneralRegion> regions = new LinkedList<>(fileManager.getRegions());
                for (RegionGroup group : fileManager.getGroups()) {
                    regions.removeAll(group.getMemberRegions());
                }
                yield regions.stream();
            }
        };
        String header = switch (filterType) {
            case ALL -> "info-allHeader";
            case SOLD -> "info-soldHeader";
            case RENTED -> "info-rentedHeader";
            case FORRENT -> "info-forrentHeader";
            case FORSALE -> "info-forsaleHeader";
            case RESELLING -> "info-resellingHeader";
            case NOGROUP -> "info-nogroupHeader";
        };
        String baseCommand = switch (filterType) {
            case ALL -> "info all";
            case SOLD -> "info sold";
            case RENTED -> "info rented";
            case FORRENT -> "info forrent";
            case FORSALE -> "info forsale";
            case RESELLING -> "info reselling";
            case NOGROUP -> "info nogroup";
        };
        showSortedPagedList(context.sender().sender(), toShow, filterGroup, header, page, baseCommand);
    }


    /**
     * Display a page of a list of regions.
     *
     * @param sender       The CommandSender to send the messages to
     * @param regionStream The regions to display
     * @param filterGroup  The group to filter the regions by
     * @param keyHeader    The header to print above the page
     * @param pageInput    The page number, if any
     * @param baseCommand  The command to execute for next/previous page (/areashop will be added)
     */
    private void showSortedPagedList(
            CommandSender sender,
            Stream<? extends GeneralRegion> regionStream,
            RegionGroup filterGroup,
            String keyHeader,
            int pageInput,
            String baseCommand
    ) {
        int page = pageInput;
        int maximumItems = 20;
        int itemsPerPage = maximumItems - 2;
        List<? extends GeneralRegion> regions;
        if (filterGroup != null) {
            regions = regionStream.filter(generalRegion -> !filterGroup.isMember(generalRegion)).toList();
        } else {
            regions = regionStream.toList();
        }
        if (regions.isEmpty()) {
            messageBridge.message(sender, "info-noRegions");
            return;
        }
        // First sort by type, then by name
        regions = regions.stream().sorted((one, two) -> {
            int typeCompare = getTypeOrder(two).compareTo(getTypeOrder(one));
            if (typeCompare != 0) {
                return typeCompare;
            } else {
                return one.getName().compareTo(two.getName());
            }
        }).toList();
        // Header
        Message limitedToGroup = Message.empty();
        if (filterGroup != null) {
            limitedToGroup = Message.fromKey("info-limitedToGroup").replacements(filterGroup.getName());
        }
        messageBridge.message(sender, keyHeader, limitedToGroup);
        // Page entries
        int totalPages = (int) Math.ceil(regions.size() / (double) itemsPerPage); // Clip page to correct boundaries, not much need to tell the user
        if (regions.size() == itemsPerPage + 1) { // 19 total items is mapped to 1 page of 19
            itemsPerPage++;
            totalPages = 1;
        }
        page = Math.max(1, Math.min(totalPages, page));
        int linesPrinted = 1; // header
        for (int i = (page - 1) * itemsPerPage; i < page * itemsPerPage && i < regions.size(); i++) {
            String state;
            GeneralRegion region = regions.get(i);
            if (region.getType() == GeneralRegion.RegionType.RENT) {
                if (region.getOwner() == null) {
                    state = "Forrent";
                } else {
                    state = "Rented";
                }
            } else {
                if (region.getOwner() == null) {
                    state = "Forsale";
                } else if (!((BuyRegion) region).isInResellingMode()) {
                    state = "Sold";
                } else {
                    state = "Reselling";
                }
            }
            messageBridge.messageNoPrefix(sender, "info-entry" + state, region);
            linesPrinted++;
        }
        Message footer = Message.empty();
        // Previous button
        if (page > 1) {
            footer.append(Message.fromKey("info-pagePrevious").replacements(baseCommand + " --page " + (page - 1)));
        } else {
            footer.append(Message.fromKey("info-pageNoPrevious"));
        }
        // Page status
        if (totalPages > 1) {
            StringBuilder pageString = new StringBuilder("" + page);
            for (int i = pageString.length(); i < (totalPages + "").length(); i++) {
                pageString.insert(0, "0");
            }
            footer.append(Message.fromKey("info-pageStatus").replacements(page, totalPages));
            if (page < totalPages) {
                footer.append(Message.fromKey("info-pageNext").replacements(baseCommand + " --page " + (page + 1)));
            } else {
                footer.append(Message.fromKey("info-pageNoNext"));
            }
            // Fill up space if the page is not full (aligns header nicely)
            for (int i = linesPrinted; i < maximumItems - 1; i++) {
                sender.sendMessage(" ");
            }
            SimpleMessageBridge.send(footer, sender);
        }
    }

    /**
     * Get an integer to order by type, usable for Comparators.
     *
     * @param region The region to get the order for
     * @return An integer for sorting by type
     */
    private Integer getTypeOrder(GeneralRegion region) {
        if (region.getType() == GeneralRegion.RegionType.RENT) {
            if (region.getOwner() == null) {
                return 1;
            } else {
                return 2;
            }
        } else {
            if (region.getOwner() == null) {
                return 3;
            } else if (!((BuyRegion) region).isInResellingMode()) {
                return 4;
            } else {
                return 5;
            }
        }
    }


    private enum RegionStateFilterType {
        ALL,
        RENTED,
        FORRENT,
        SOLD,
        FORSALE,
        RESELLING,
        NOGROUP
    }

}


























