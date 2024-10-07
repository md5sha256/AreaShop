package me.wiefferink.areashop.commands;

import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedCuboidRegion;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.commands.util.AreaShopCommandException;
import me.wiefferink.areashop.commands.util.AreashopCommandBean;
import me.wiefferink.areashop.commands.util.commandsource.CommandSource;
import me.wiefferink.areashop.commands.util.commandsource.PlayerCommandSource;
import me.wiefferink.areashop.events.ask.AddingRegionEvent;
import me.wiefferink.areashop.interfaces.WorldEditInterface;
import me.wiefferink.areashop.interfaces.WorldEditSelection;
import me.wiefferink.areashop.interfaces.WorldGuardInterface;
import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RegionFactory;
import me.wiefferink.areashop.regions.RegionGroup;
import me.wiefferink.areashop.tools.Utils;
import me.wiefferink.interactivemessenger.processing.Message;
import org.bukkit.Location;
import org.bukkit.block.BlockFace;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.util.Vector;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.incendo.cloud.Command;
import org.incendo.cloud.bean.CommandProperties;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.flag.CommandFlag;
import org.incendo.cloud.parser.standard.EnumParser;
import org.incendo.cloud.parser.standard.IntegerParser;
import org.incendo.cloud.parser.standard.StringParser;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.util.Optional;

@Singleton
public class StackCommand extends AreashopCommandBean {

    private static final CloudKey<Integer> KEY_AMOUNT = CloudKey.of("amount", Integer.class);
    private static final CloudKey<Integer> KEY_GAP = CloudKey.of("gap", Integer.class);
    private static final CloudKey<String> KEY_NAME = CloudKey.of("name", String.class);
    private static final CloudKey<GeneralRegion.RegionType> KEY_TYPE = CloudKey.of("type",
            GeneralRegion.RegionType.class);
    private static final CommandFlag<String> FLAG_GROUP = CommandFlag.builder("group")
            .withComponent(StringParser.stringParser())
            .build();
    private final MessageBridge messageBridge;
    private final Plugin plugin;
    private final WorldEditInterface worldEditInterface;
    private final WorldGuardInterface worldGuardInterface;
    private final RegionFactory regionFactory;
    private final IFileManager fileManager;

    @Inject
    public StackCommand(
            @Nonnull MessageBridge messageBridge,
            @Nonnull Plugin plugin,
            @Nonnull WorldEditInterface worldEditInterface,
            @Nonnull WorldGuardInterface worldGuardInterface,
            @Nonnull RegionFactory regionFactory,
            @Nonnull IFileManager fileManager

    ) {
        this.messageBridge = messageBridge;
        this.plugin = plugin;
        this.worldEditInterface = worldEditInterface;
        this.worldGuardInterface = worldGuardInterface;
        this.regionFactory = regionFactory;
        this.fileManager = fileManager;

    }

    @NotNull
    private static Vector calculateShift(BlockFace facing, WorldEditSelection selection, int gap) {
        Vector shift = new Vector(0, 0, 0);
        if (facing == BlockFace.SOUTH) {
            shift = shift.setZ(-selection.getLength() - gap);
        } else if (facing == BlockFace.WEST) {
            shift = shift.setX(selection.getWidth() + gap);
        } else if (facing == BlockFace.NORTH) {
            shift = shift.setZ(selection.getLength() + gap);
        } else if (facing == BlockFace.EAST) {
            shift = shift.setX(-selection.getWidth() - gap);
        } else if (facing == BlockFace.DOWN) {
            shift = shift.setY(-selection.getHeight() - gap);
        } else if (facing == BlockFace.UP) {
            shift = shift.setY(selection.getHeight() + gap);
        }
        return shift;
    }

    @Override
    public String getHelpKey(@NotNull CommandSender target) {
        if (target.hasPermission("areashop.stack")) {
            return "help-stack";
        }
        return null;
    }

    @Override
    public String stringDescription() {
        return null;
    }

    @Override
    protected Command.Builder<? extends CommandSource<?>> configureCommand(Command.@NotNull Builder<CommandSource<?>> builder) {
        return builder.literal("stack")
                .senderType(PlayerCommandSource.class)
                .required(KEY_AMOUNT, IntegerParser.integerParser(0))
                .required(KEY_GAP, IntegerParser.integerParser(0))
                .required(KEY_NAME, StringParser.stringParser())
                .required(KEY_TYPE, EnumParser.enumParser(GeneralRegion.RegionType.class))
                .flag(FLAG_GROUP)
                .handler(this::handleCommand);

    }

    @Override
    protected @NonNull CommandProperties properties() {
        return CommandProperties.of("stack");
    }

    private void handleCommand(@Nonnull CommandContext<PlayerCommandSource> context) {
        Player player = context.sender().sender();
        if (!player.hasPermission("areashop.stack")) {
            throw new AreaShopCommandException("stack-noPermission");
        }
        int amount = context.get(KEY_AMOUNT);
        int gap = context.get(KEY_GAP);
        String name = context.get(KEY_NAME);
        GeneralRegion.RegionType regionType = context.get(KEY_TYPE);
        Optional<String> optionalGroup = context.flags().getValue(FLAG_GROUP);
        // Get WorldEdit selection
        final WorldEditSelection selection = worldEditInterface.getPlayerSelection(player);
        if (selection == null) {
            this.messageBridge.message(player, "stack-noSelection");
            return;
        }
        // Get or create group
        RegionGroup group = null;
        if (optionalGroup.isPresent()) {
            String groupName = optionalGroup.get();
            group = fileManager.getGroup(groupName);
            if (group == null) {
                group = regionFactory.createRegionGroup(groupName);
                fileManager.addGroup(group);
            }
        }
        // Get facing of the player (must be clearly one of the four directions to make sure it is no mistake)
        BlockFace facing = Utils.yawToFacing(player.getLocation().getYaw());
        if (player.getLocation().getPitch() > 45) {
            facing = BlockFace.DOWN;
        } else if (player.getLocation().getPitch() < -45) {
            facing = BlockFace.UP;
        }
        if (!(facing == BlockFace.NORTH || facing == BlockFace.EAST || facing == BlockFace.SOUTH || facing == BlockFace.WEST || facing == BlockFace.UP || facing == BlockFace.DOWN)) {
            this.messageBridge.message(player,
                    "stack-unclearDirection",
                    facing.toString().toLowerCase().replace('_', '-'));
            return;
        }
        Vector shift = calculateShift(facing, selection, gap);
        BukkitRunnable task = stackTask(player, name, amount, selection, regionType, shift, group);
        task.runTaskTimer(plugin, 1, 1);
    }

    private BukkitRunnable stackTask(
            @Nonnull Player player,
            @Nonnull String name,
            int amount,
            @Nonnull WorldEditSelection selection,
            GeneralRegion.RegionType regionType,
            @Nonnull Vector shift,
            @Nullable RegionGroup group
    ) {
        final int regionsPerTick = plugin.getConfig().getInt("adding.regionsPerTick");

        Location minimumLocation = selection.getMinimumLocation();
        Vector minimumVector = new Vector(minimumLocation.getX(), minimumLocation.getY(), minimumLocation.getZ());
        Location maximumLocation = selection.getMaximumLocation();
        Vector maximumVector = new Vector(maximumLocation.getX(), maximumLocation.getY(), maximumLocation.getZ());
        return new BukkitRunnable() {
            private final RegionManager manager = worldGuardInterface.getRegionManager(selection.getWorld());
            private int current = -1;
            private int counter = 1;
            private int tooLow = 0;
            private int tooHigh = 0;

            @Override
            public void run() {
                for (int i = 0; i < regionsPerTick; i++) {
                    current++;
                    if (current >= amount) {
                        continue;
                    }
                    // Create the region name
                    String regionName = countToName(name, counter);
                    while (manager.getRegion(regionName) != null || fileManager.getRegion(regionName) != null) {
                        counter++;
                        regionName = countToName(name, counter);
                    }

                    // Add the region to WorldGuard (at startposition shifted by the number of this region times the blocks it should shift)
                    Vector minimum = minimumVector.clone().add(shift.clone().multiply(current));
                    Vector maximum = maximumVector.clone().add(shift.clone().multiply(current));

                    // Check for out of bounds
                    if (minimum.getBlockY() < 0) {
                        tooLow++;
                        continue;
                    } else if (maximum.getBlockY() > 256) {
                        tooHigh++;
                        continue;
                    }
                    ProtectedCuboidRegion region = worldGuardInterface.createCuboidRegion(regionName, minimum, maximum);
                    manager.addRegion(region);

                    // Add the region to AreaShop
                    GeneralRegion newRegion;
                    if (regionType == GeneralRegion.RegionType.BUY) {
                        newRegion = regionFactory.createRentRegion(regionName, selection.getWorld());
                    } else {
                        newRegion = regionFactory.createBuyRegion(regionName, selection.getWorld());
                    }

                    if (group != null) {
                        group.addMember(newRegion);
                    }
                    AddingRegionEvent event = fileManager.addRegion(newRegion);
                    if (event.isCancelled()) {
                        messageBridge.message(player, "general-cancelled", event.getReason());
                        continue;
                    }
                    newRegion.handleSchematicEvent(GeneralRegion.RegionEvent.CREATED);
                    newRegion.update();
                }
                if (current >= amount) {
                    if (player.isOnline()) {
                        int added = amount - tooLow - tooHigh;
                        Message wrong = Message.empty();
                        if (tooHigh > 0) {
                            wrong.append(Message.fromKey("stack-tooHigh").replacements(tooHigh));
                        }
                        if (tooLow > 0) {
                            wrong.append(Message.fromKey("stack-tooLow").replacements(tooLow));
                        }
                        messageBridge.message(player, "stack-addComplete", added, wrong);
                    }
                    this.cancel();
                }
            }
        };
    }

    /**
     * Build a name from a count, with the right length.
     *
     * @param template Template to put the name in (# to put the count there, otherwise count is appended)
     * @param count    Number to use
     * @return name with prepended 0's
     */
    private String countToName(String template, int count) {
        StringBuilder counterName = new StringBuilder().append(count);
        int minimumLength = plugin.getConfig().getInt("stackRegionNumberLength");
        while (counterName.length() < minimumLength) {
            counterName.insert(0, "0");
        }

        if (template.contains("#")) {
            return template.replace("#", counterName);
        } else {
            return template + counterName;
        }
    }

}










