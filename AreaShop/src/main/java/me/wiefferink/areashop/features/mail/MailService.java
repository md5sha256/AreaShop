package me.wiefferink.areashop.features.mail;

import org.bukkit.OfflinePlayer;

import javax.annotation.Nonnull;

public interface MailService {
    void sendMail(@Nonnull OfflinePlayer recipient, @Nonnull String message);

}
