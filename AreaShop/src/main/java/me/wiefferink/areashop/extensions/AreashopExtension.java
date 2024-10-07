package me.wiefferink.areashop.extensions;

import com.google.inject.Injector;
import me.wiefferink.areashop.AreaShop;

public interface AreashopExtension {

    void init(AreaShop plugin, Injector injector);

}
