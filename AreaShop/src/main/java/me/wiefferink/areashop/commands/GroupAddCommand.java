package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.RegionParseUtil;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RegionFactory;
import me.wiefferink.areashop.regions.RegionGroup;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.context.CommandInput;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.standard.StringParser;
import org.incendo.cloud.suggestion.Suggestion;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;

@Singleton
public class GroupAddCommand extends AreashopCommandBean {

    private static final CloudKey<String> KEY_GROUP = CloudKey.of("group", String.class);

    private final IFileManager fileManager;
    private final RegionFactory regionFactory;
    private final MessageBridge messageBridge;
    private final CommandFlag<GeneralRegion> regionFlag;

    @Inject
    public GroupAddCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager,
            @Nonnull RegionFactory regionFactory

    ) {
        this.messageBridge = messageBridge;
        this.fileManager = fileManager;
        this.regionFactory = regionFactory;
        this.regionFlag = RegionParseUtil.createDefault(fileManager);
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    public String getHelpKey(@NotNull CommandSender target) {
        if(target.hasPermission("areashop.groupadd")) {
            return "help-groupadd";
        }
        return null;
    }

    @Override
    protected @Nonnull Command.Builder<? extends CommandSource<?>> configureCommand(@Nonnull Command.Builder<CommandSource<?>> builder) {
        return builder.literal("groupadd")
                .required(KEY_GROUP, StringParser.stringParser(), this::suggestGroupNames)
                .flag(this.regionFlag)
                .handler(this::handleCommand);
    }

    @Override
    protected @Nonnull CommandProperties properties() {
        return CommandProperties.of("groupadd");
    }

    private void handleCommand(@Nonnull CommandContext<CommandSource<?>> context) {
        CommandSender sender = context.sender().sender();
        if (!sender.hasPermission("areashop.groupadd")) {
            throw new AreaShopCommandException("groupadd-noPermission");
        }
        String rawGroup = context.get(KEY_GROUP);
        RegionGroup group = fileManager.getGroup(rawGroup);
        if (group == null) {
            group = regionFactory.createRegionGroup(rawGroup);
            fileManager.addGroup(group);
        }
        GeneralRegion declaredRegion = context.flags().get(this.regionFlag);
        if (declaredRegion != null) {
            if (!group.addMember(declaredRegion)) {
                throw new AreaShopCommandException("groupadd-failed", group.getName(), declaredRegion);
            }
            declaredRegion.update();
            this.messageBridge.message(sender,
                    "groupadd-success",
                    group.getName(),
                    group.getMembers().size(),
                    declaredRegion);
            return;
        }

        Collection<GeneralRegion> regions = RegionParseUtil.getOrParseRegionsInSel(context, sender, this.regionFlag);
        Set<GeneralRegion> regionsSuccess = new TreeSet<>();
        Set<GeneralRegion> regionsFailed = new TreeSet<>();
        for (GeneralRegion region : regions) {
            if (group.addMember(region)) {
                regionsSuccess.add(region);
            } else {
                regionsFailed.add(region);
            }
        }
        if (!regionsSuccess.isEmpty()) {
            messageBridge.message(sender,
                    "groupadd-weSuccess",
                    group.getName(),
                    Utils.combinedMessage(regionsSuccess, "region"));
        }
        if (!regionsFailed.isEmpty()) {
            messageBridge.message(sender,
                    "groupadd-weFailed",
                    group.getName(),
                    Utils.combinedMessage(regionsFailed, "region"));
        }
        // Update all regions, this does it in a task, updating them without lag
        fileManager.updateRegions(List.copyOf(regionsSuccess), sender);
    }

    private CompletableFuture<Iterable<Suggestion>> suggestGroupNames(
            @Nonnull CommandContext<CommandSource<?>> context,
            @Nonnull CommandInput input
    ) {
        String text = input.peekString();
        List<Suggestion> suggestions = this.fileManager.getGroupNames().stream()
                .filter(name -> name.startsWith(text))
                .map(Suggestion::suggestion)
                .toList();
        return CompletableFuture.completedFuture(suggestions);
    }
}










