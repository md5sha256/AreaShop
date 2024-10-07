package me.wiefferink.areashop.commands.util;

import org.bukkit.command.CommandSender;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public interface HelpProvider {

    @Nullable String getHelpKey(@Nonnull CommandSender target);


}
