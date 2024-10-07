package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.command.CommandSender;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;

import javax.annotation.Nonnull;
import java.util.List;

@Singleton
public class GroupListCommand extends AreashopCommandBean {

	private final MessageBridge messageBridge;
	private final IFileManager fileManager;

	@Inject
	public GroupListCommand(@Nonnull MessageBridge messageBridge, @Nonnull IFileManager fileManager) {
		this.messageBridge = messageBridge;
		this.fileManager = fileManager;
	}

	@Override
	public String stringDescription() {
		return null;
	}

	@Override
	public String getHelpKey(CommandSender target) {
		if(target.hasPermission("areashop.grouplist")) {
			return "help-grouplist";
		}
		return null;
	}

	@Override
	protected @Nonnull Command.Builder<? extends CommandSource<?>> configureCommand(@Nonnull Command.Builder<CommandSource<?>> builder) {
		return builder.literal("grouplist", "groups")
				.handler(this::handleCommand);
	}

    @Override
    protected @Nonnull CommandProperties properties() {
		return CommandProperties.of("grouplist", "groups");
	}

	private void handleCommand(@Nonnull CommandContext<CommandSource<?>> context) {
		if (!context.hasPermission("areashop.grouplist")) {
			throw new AreaShopCommandException("grouplist-noPermission");
		}
		List<String> groups = this.fileManager.getGroupNames();
		CommandSender sender = context.sender().sender();
		if(groups.isEmpty()) {
			messageBridge.message(sender, "grouplist-noGroups");
		} else {
			messageBridge.message(sender, "grouplist-success", Utils.createCommaSeparatedList(groups));
		}
	}

}










