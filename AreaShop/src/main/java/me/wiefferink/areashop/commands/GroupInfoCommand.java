package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.RegionGroupParser;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.RegionGroup;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.ParserDescriptor;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Set;

@Singleton
public class GroupInfoCommand extends AreashopCommandBean {

    private static final CloudKey<RegionGroup> KEY_GROUP = CloudKey.of("group", RegionGroup.class);

    private final IFileManager fileManager;
    private final MessageBridge messageBridge;

    @Inject
    public GroupInfoCommand(@Nonnull MessageBridge messageBridge, @Nonnull IFileManager fileManager) {
        this.messageBridge = messageBridge;
        this.fileManager = fileManager;
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    public String getHelpKey(@NotNull CommandSender target) {
        if(target.hasPermission("areashop.groupinfo")) {
            return "help-groupinfo";
        }
        return null;
    }

    @Override
    protected @Nonnull Command.Builder<? extends CommandSource<?>> configureCommand(@Nonnull Command.Builder<CommandSource<?>> builder) {
        ParserDescriptor<CommandSource<?>, RegionGroup> regionGroupParser = ParserDescriptor.of(
                new RegionGroupParser<>(this.fileManager, "groupinfo-noGroup"),
                RegionGroup.class);
        return builder
                .literal("groupinfo")
                .required(KEY_GROUP, regionGroupParser)
                .handler(this::handleCommand);

    }

    @Override
    protected @Nonnull CommandProperties properties() {
        return CommandProperties.of("groupinfo");
    }

    public void handleCommand(@Nonnull CommandContext<CommandSource<?>> context) {
        if (!context.hasPermission("groupinfo")) {
            throw new AreaShopCommandException("groupinfo-noPermission");
        }
        RegionGroup group = context.get(KEY_GROUP);
        Set<String> members = group.getMembers();
        if (members.isEmpty()) {
            throw new AreaShopCommandException("groupinfo-noMembers", group.getName());
        }
        String seperatedMembers = Utils.createCommaSeparatedList(members);
        this.messageBridge.message(context.sender(), "groupinfo-members", group.getName(), seperatedMembers);
    }

}










