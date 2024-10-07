package me.wiefferink.areashop.tools;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.MessageBridge;
import me.wiefferink.areashop.features.mail.MailService;
import me.wiefferink.areashop.services.ServiceManager;
import me.wiefferink.interactivemessenger.processing.Message;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

@Singleton
public class SimpleMessageBridge implements MessageBridge {
    private static final BukkitAudiences AUDIENCE_ADAPTER = BukkitAudiences.create(AreaShop.getInstance());
    private final ServiceManager serviceManager;

    @Inject
    public SimpleMessageBridge(@Nonnull ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    public static void send(Message message, CommandSender target) {
        if (target == null) {
            return;
        }
        if (!AreaShop.useMiniMessage()) {
            message.send(target);
            return;
        }

        if (message.get() == null || message.get().isEmpty() || (message.get().size() == 1 && message.get()
                .getFirst()
                .isEmpty())) {
            return;
        }
        Audience audience = AUDIENCE_ADAPTER.sender(target);
        audience.sendMessage(convertMessage(message));
    }

    public static void send(Message message, Object target) {
        if (target == null) {
            return;
        }
        if (!AreaShop.useMiniMessage()) {
            message.send(target);
            return;
        }

        if (target instanceof CommandSender sender) {
            send(message, sender);
        } else {
            String error = "AreaShop sent a non-supported Object. %s is not a CommandSender!";
            AreaShop.error(String.format(error, target.getClass().getName()));
        }
    }

    private static Component convertMessage(@Nonnull Message message) {
        message.doReplacements();

        StringBuilder messageStr = new StringBuilder();
        for (String line : message.getRaw()) {
            messageStr.append(line);
        }

        MiniMessage mm = MiniMessage.miniMessage();
        return mm.deserialize(messageStr.toString());
    }

    @Override
    public void messagePersistent(@Nonnull OfflinePlayer target,
                                  @Nonnull String key,
                                  @Nonnull Object... replacements) {
        Message message = Message.fromKey(key).replacements(replacements);
        messagePersistent(target, message);
    }


    public void messagePersistent(@Nonnull OfflinePlayer target, @Nonnull Message message) {
        if (target.isOnline()) {
            send(message, target.getPlayer());
            return;
        }
        Optional<MailService> optional = this.serviceManager.getService(MailService.class);
        if (optional.isEmpty()) {
            return;
        }
        MailService mailService = optional.get();
        List<String> serializedMessages;
        if (AreaShop.useMiniMessage()) {
            // The lang file we are using will already provide languages in MiniMessage format
            serializedMessages = message.get();
        } else {
            // We need to manually convert the messages into MiniMessage first
            serializedMessages = LanguageConverter.convertRawList(message.get());
        }
        serializedMessages.forEach(msg -> mailService.sendMail(target, msg));
    }

    /**
     * Send a message to a target without a prefix.
     *
     * @param target       The target to send the message to
     * @param key          The key of the language string
     * @param replacements The replacements to insert in the message
     */
    @Override
    public void messageNoPrefix(Object target, String key, Object... replacements) {
        Message m = Message.fromKey(key).replacements(replacements);
        send(m, target);
    }

    /**
     * Send a message to a target, prefixed by the default chat prefix.
     *
     * @param target       The target to send the message to
     * @param key          The key of the language string
     * @param replacements The replacements to insert in the message
     */
    @Override
    public void message(Object target, String key, Object... replacements) {
        Message m = Message.fromKey(key).prefix().replacements(replacements);
        send(m, target);
    }

}
