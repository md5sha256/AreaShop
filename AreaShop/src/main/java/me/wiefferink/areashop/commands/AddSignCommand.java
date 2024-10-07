package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.GeneralRegionParser;
import me.wiefferink.areashop.commands.util.RegionParseUtil;
import me.wiefferink.areashop.commands.util.SignProfileUtil;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.commands.util.commandsource.PlayerCommandSource;
import me.wiefferink.areashop.features.signs.RegionSign;
import me.wiefferink.areashop.features.signs.SignManager;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.tools.Materials;
import me.wiefferink.areashop.tools.SignUtils;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.BlockIterator;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Optional;

@Singleton
public class AddSignCommand extends AreashopCommandBean {

	private static final CloudKey<GeneralRegion> KEY_REGION = CloudKey.of("region", GeneralRegion.class);

	private final SignManager signManager;
	private final Plugin plugin;
	private final IFileManager fileManager;

	private final MessageBridge messageBridge;

    @Inject
    public AddSignCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull IFileManager fileManager,
            @Nonnull SignManager signManager,
            @Nonnull Plugin plugin
    ) {
		this.messageBridge = messageBridge;
        this.signManager = signManager;
        this.plugin = plugin;
		this.fileManager = fileManager;
    }

	@Override
	public String stringDescription() {
		return "Allows you to add signs to existing regions";
	}

	@Override
	public String getHelpKey(@NotNull CommandSender target) {
		if(target.hasPermission("areashop.addsign")) {
			return "help-addsign";
		}
		return null;
	}

	@Override
	protected @Nonnull Command.Builder<? extends CommandSource<?>> configureCommand(@Nonnull Command.Builder<CommandSource<?>> builder) {
		return builder.literal("addsign")
				.senderType(PlayerCommandSource.class)
				.optional(KEY_REGION, GeneralRegionParser.generalRegionParser(this.fileManager))
				.flag(SignProfileUtil.DEFAULT_FLAG)
				.handler(this::handleCommand);
	}

	private void handleCommand(@Nonnull CommandContext<PlayerCommandSource> context) {
		Player sender = context.sender().sender();
		if (!sender.hasPermission("areashop.addsign")) {
			throw new AreaShopCommandException("addsign-noPermission");
		}
		// Get the sign
		Block block = null;
		BlockIterator blockIterator = new BlockIterator(sender, 100);
		while(blockIterator.hasNext() && block == null) {
			Block next = blockIterator.next();
			if(next.getType() != Material.AIR) {
				block = next;
			}
		}
		if(block == null || !Materials.isSign(block.getType())) {
			messageBridge.message(sender, "addsign-noSign");
			return;
		}

		GeneralRegion region = RegionParseUtil.getOrParseRegion(context, sender, KEY_REGION);
		String profile = SignProfileUtil.getOrParseProfile(context, this.plugin);
		Optional<RegionSign> optionalRegionSign = this.signManager.signFromLocation(block.getLocation());
		if(optionalRegionSign.isPresent()) {
			RegionSign regionSign = optionalRegionSign.get();
			messageBridge.message(sender, "addsign-alreadyRegistered", regionSign.getRegion());
			return;
		}

		region.getSignsFeature().addSign(block.getLocation(), block.getType(), SignUtils.getSignFacing(block), profile);
		if(profile == null) {
			messageBridge.message(sender, "addsign-success", region);
		} else {
			messageBridge.message(sender, "addsign-successProfile", profile, region);
		}
		region.update();
	}


    @Override
    protected @Nonnull CommandProperties properties() {
		return CommandProperties.of("addsign");
	}
}










