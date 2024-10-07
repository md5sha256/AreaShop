package me.wiefferink.areashop.adapters.plugins.essentials;

import net.essentialsx.api.v2.services.mail.MailSender;

import javax.annotation.Nonnull;
import java.util.UUID;

public record PluginMailSender(@Nonnull String name) implements MailSender {


    @Override
    public String getName() {
        return name();
    }

    @Override
    public UUID getUUID() {
        return null;
    }
}
