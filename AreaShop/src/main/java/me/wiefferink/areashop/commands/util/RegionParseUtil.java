package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.managers.IFileManager;
import me.wiefferink.areashop.regions.BuyRegion;
import me.wiefferink.areashop.regions.GeneralRegion;
import me.wiefferink.areashop.regions.RentRegion;
import me.wiefferink.areashop.tools.Utils;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.key.CloudKey;
import org.incendo.cloud.parser.ParserDescriptor;
import org.incendo.cloud.parser.flag.CommandFlag;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.List;

public final class RegionParseUtil {

    private RegionParseUtil() {
        throw new IllegalStateException("Cannot instantiate static utility class");
    }

    @Nonnull
    public static CommandFlag<GeneralRegion> createDefault(@Nonnull IFileManager fileManager) {
        return CommandFlag.builder("region")
                .withComponent(GeneralRegionParser.generalRegionParser(fileManager))
                .build();
    }

    @Nonnull
    public static Collection<GeneralRegion> getOrParseRegionsInSel(
            @Nonnull CommandContext<?> context,
            @Nonnull CommandSender sender,
            @Nonnull CommandFlag<GeneralRegion> regionFlag
    ) {
        if (!(sender instanceof Player player)) {
            throw new AreaShopCommandException("cmd-weOnlyByPlayer");
        }
        GeneralRegion declaredRegion = context.flags().get(regionFlag);
        if (declaredRegion != null) {
            return List.of(declaredRegion);
        }
        Location location = player.getLocation();
        List<GeneralRegion> regions = Utils.getImportantRegions(location);
        if (!regions.isEmpty()) {
            return regions;

        }
        throw new AreaShopCommandException("cmd-noRegionsAtLocation");
    }

    @Nonnull
    public static GeneralRegion getOrParseRegion(
            @Nonnull CommandContext<?> context,
            @Nonnull CommandSender sender,
            @Nonnull CommandFlag<GeneralRegion> flag
    ) throws AreaShopCommandException {
        GeneralRegion region = context.flags().get(flag);
        if (region != null) {
            return region;
        }
        if (!(sender instanceof Entity entity)) {
            throw new AreaShopCommandException("cmd-automaticRegionOnlyByPlayer");
        }
        Location location = entity.getLocation();
        List<GeneralRegion> regions = Utils.getImportantRegions(location);
        String errorMessageKey;
        if (regions.isEmpty()) {
            errorMessageKey = "cmd-noRegionsAtLocation";
        } else if (regions.size() > 1) {
            errorMessageKey = "cmd-moreRegionsAtLocation";
        } else {
            return regions.getFirst();
        }
        throw new AreaShopCommandException(errorMessageKey);
    }

    @Nonnull
    public static GeneralRegion getOrParseRegion(
            @Nonnull CommandContext<?> context,
            @Nonnull CommandSender sender,
            @Nonnull CloudKey<GeneralRegion> key
    ) throws AreaShopCommandException {
        GeneralRegion region = context.getOrDefault(key, null);
        if (region != null) {
            return region;
        }
        if (!(sender instanceof Entity entity)) {
            throw new AreaShopCommandException("cmd-automaticRegionOnlyByPlayer");
        }
        Location location = entity.getLocation();
        List<GeneralRegion> regions = Utils.getImportantRegions(location);
        String errorMessageKey;
        if (regions.isEmpty()) {
            errorMessageKey = "cmd-noRegionsAtLocation";
        } else if (regions.size() > 1) {
            errorMessageKey = "cmd-moreRegionsAtLocation";
        } else {
            return regions.getFirst();
        }
        throw new AreaShopCommandException(errorMessageKey);
    }


    @Nonnull
    public static CommandFlag<BuyRegion> createDefaultBuy(@Nonnull IFileManager fileManager) {
        return CommandFlag.builder("region")
                .withComponent(ParserDescriptor.of(new BuyRegionParser<>(fileManager), BuyRegion.class))
                .build();
    }

    @Nonnull
    public static CommandFlag<RentRegion> createDefaultRent(@Nonnull IFileManager fileManager) {
        return CommandFlag.builder("region")
                .withComponent(ParserDescriptor.of(new RentRegionParser<>(fileManager), RentRegion.class))
                .build();
    }

    @Nonnull
    public static BuyRegion getOrParseBuyRegion(
            @Nonnull CommandContext<?> context,
            @Nonnull CommandSender sender,
            @Nonnull CommandFlag<BuyRegion> flag) {
        BuyRegion buyRegion = context.flags().get(flag);
        if (buyRegion != null) {
            return buyRegion;
        }
        if (!(sender instanceof Player player)) {
            throw new AreaShopCommandException("cmd-automaticRegionOnlyByPlayer");
        }
        List<BuyRegion> regions = Utils.getImportantBuyRegions(player.getLocation());
        if (regions.isEmpty()) {
            throw new AreaShopCommandException("cmd-noRegionsAtLocation");
        } else if (regions.size() != 1) {
            throw new AreaShopCommandException("cmd-moreRegionsAtLocation");
        }
        return regions.getFirst();
    }

    @Nonnull
    public static RentRegion getOrParseRentRegion(
            @Nonnull CommandContext<?> context,
            @Nonnull CommandSender sender,
            @Nonnull CommandFlag<RentRegion> flag) {
        RentRegion rentRegion = context.flags().get(flag);
        if (rentRegion != null) {
            return rentRegion;
        }
        if (!(sender instanceof Player player)) {
            throw new AreaShopCommandException("cmd-automaticRegionOnlyByPlayer");
        }
        List<RentRegion> regions = Utils.getImportantRentRegions(player.getLocation());
        if (regions.isEmpty()) {
            throw new AreaShopCommandException("cmd-noRegionsAtLocation");
        } else if (regions.size() != 1) {
            throw new AreaShopCommandException("cmd-moreRegionsAtLocation");
        }
        return regions.getFirst();
    }
    
}
