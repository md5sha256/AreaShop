package me.wiefferink.areashop.commands.util;

import me.wiefferink.areashop.MessageBridge;
import org.bukkit.command.CommandSender;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

public class HelpRenderer {

    private final MessageBridge messageBridge;
    private final List<HelpProvider> providers;

    public HelpRenderer(@Nonnull MessageBridge messageBridge, @Nonnull List<? extends HelpProvider> providers) {
        this.providers = List.copyOf(providers);
        this.messageBridge = messageBridge;
    }

    public void showHelp(@Nonnull CommandSender target) {
        if (!target.hasPermission("areashop.help")) {
            this.messageBridge.message(target, "help-noPermission");
            return;
        }
        // Add all messages to a list
        List<String> messages = new ArrayList<>();
        this.messageBridge.message(target, "help-header");
        this.messageBridge.message(target, "help-alias");
        for (HelpProvider provider : providers) {
            String help = provider.getHelpKey(target);
            if (help != null && !help.isEmpty()) {
                messages.add(help);
            }
        }
        // Send the messages to the target
        for (String message : messages) {
            this.messageBridge.messageNoPrefix(target, message);
        }
    }

}
