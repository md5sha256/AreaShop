package me.wiefferink.areashop.adapters.plugins.essentials;

import com.earth2me.essentials.IEssentials;
import me.wiefferink.areashop.features.mail.MailService;
import net.ess3.api.IUser;
import net.essentialsx.api.v2.services.mail.MailSender;
import org.bukkit.OfflinePlayer;

import javax.annotation.Nonnull;

public class EssentialsMailService implements MailService {

    private final MailSender sender;
    private final net.essentialsx.api.v2.services.mail.MailService mailService;
    private final IEssentials essentials;

    public EssentialsMailService(
            @Nonnull net.essentialsx.api.v2.services.mail.MailService mailService,
            @Nonnull MailSender sender,
            @Nonnull IEssentials essentials
    ) {
        this.sender = sender;
        this.mailService = mailService;
        this.essentials = essentials;
    }

    @Override
    public void sendMail(@Nonnull OfflinePlayer recipient, @Nonnull String message) {
        IUser user = this.essentials.getUser(recipient.getUniqueId());
        if (user == null) {
            return;
        }
        this.mailService.sendMail(user, this.sender, message);
    }

}
