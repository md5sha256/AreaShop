package me.wiefferink.areashop.commands;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.commands.util.commandsource.PlayerCommandSource;
import me.wiefferink.areashop.features.signs.RegionSign;
import me.wiefferink.areashop.features.signs.SignManager;
import me.wiefferink.areashop.tools.Materials;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.util.BlockIterator;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Optional;

@Singleton
public class DelSignCommand extends AreashopCommandBean {

    private final SignManager signManager;
    private final MessageBridge messageBridge;

    @Inject
    public DelSignCommand(@Nonnull MessageBridge messageBridge, @Nonnull SignManager signManager) {
        this.signManager = signManager;
        this.messageBridge = messageBridge;
    }

    @Override
    public String stringDescription() {
        return null;
    }


    @Override
    protected @Nonnull CommandProperties properties() {
        return CommandProperties.of("deletesign", "delsign");
    }

    @Override
    protected @Nonnull Command.Builder<? extends CommandSource<?>> configureCommand(@Nonnull Command.Builder<CommandSource<?>> builder) {
        return builder.literal("delsign", "deletesign")
                .senderType(PlayerCommandSource.class)
                .handler(this::handleCommand);
    }

    @Override
    public String getHelpKey(@NotNull CommandSender target) {
        if (target.hasPermission("areashop.delsign")) {
            return "help-delsign";
        }
        return null;
    }

    private void handleCommand(@Nonnull CommandContext<PlayerCommandSource> context) {
        Player sender = context.sender().sender();
        if (!sender.hasPermission("areashop.delsign")) {
            throw new AreaShopCommandException("delsign-noPermission");
        }
        // Get the sign
        Block block = null;
        BlockIterator blockIterator = new BlockIterator(sender, 100);
        while (blockIterator.hasNext() && block == null) {
            Block next = blockIterator.next();
            if (!next.getType().isAir()) {
                block = next;
            }
        }
        if (block == null || !Materials.isSign(block.getType())) {
            throw new AreaShopCommandException("delsign-noSign");
        }
        Optional<RegionSign> optionalSign = signManager.removeSign(block.getLocation());
        if (optionalSign.isEmpty()) {
            throw new AreaShopCommandException("delsign-noRegion");
        }
        RegionSign regionSign = optionalSign.get();
        messageBridge.message(sender, "delsign-success", regionSign.getRegion());
        regionSign.remove();
        // Sometimes the RegionSign data is corrupted. Forcefully set the block to air
        block.setType(Material.AIR);
    }

}










