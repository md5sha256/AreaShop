package me.wiefferink.areashop.adapters.plugins.essentials;

import com.earth2me.essentials.Essentials;
import com.google.inject.Injector;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import me.wiefferink.areashop.AreaShop;
import me.wiefferink.areashop.extensions.AreashopExtension;
import me.wiefferink.areashop.features.homeaccess.AccessControlValidator;
import me.wiefferink.areashop.features.homeaccess.OwnershipControlValidator;
import me.wiefferink.areashop.features.mail.MailService;
import net.essentialsx.api.v2.services.mail.MailSender;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;

import javax.annotation.Nonnull;

public class EssentialsExtension implements AreashopExtension {

    private boolean enabled;
    private Essentials essentials;

    @Override
    public void init(@Nonnull AreaShop plugin, @Nonnull Injector injector) {
        if (this.enabled) {
            return;
        }
        Injector childInjector = injector.createChildInjector(new FactoryModuleBuilder().build(HomeListenerFactory.class));
        Server server = childInjector.getInstance(Server.class);
        Plugin essentialsPlugin = server.getPluginManager().getPlugin("Essentials");
        if (essentialsPlugin == null) {
            return;
        }
        this.essentials = (Essentials) essentialsPlugin;
        AreaShop.info("EssentialsX detected; registering optional features");
        registerServices(plugin);
        if (!server.getPluginManager().isPluginEnabled("Essentials")) {
            return;
        }
        AreaShop.info("EssentialsX detected; binding implementation for home access");
        HomeListenerFactory factory = childInjector.getInstance(HomeListenerFactory.class);
        AccessControlValidator controlValidator = new OwnershipControlValidator();
        HomeModificationListener listener = factory.createListener(controlValidator);
        server.getPluginManager().registerEvents(listener, plugin);
        this.enabled = true;
    }

    private void registerServices(@Nonnull AreaShop plugin) {
        if (plugin.getConfig().getBoolean("enable-mail-notifications")) {
            MailSender mailSender = new PluginMailSender(plugin.getName());
            MailService mailService = new EssentialsMailService(this.essentials.getMail(), mailSender, this.essentials);
            plugin.getServiceManager().registerService(MailService.class, mailService);
        }
    }
}
