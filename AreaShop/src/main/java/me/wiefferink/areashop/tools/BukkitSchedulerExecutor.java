package me.wiefferink.areashop.tools;


import jakarta.inject.Inject;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;

public class BukkitSchedulerExecutor implements Executor {

    private final Plugin plugin;
    private final BukkitScheduler scheduler;

    @Inject
    public BukkitSchedulerExecutor(@NotNull Plugin plugin, @NotNull BukkitScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public void execute(@NotNull Runnable command) {
        this.scheduler.runTask(this.plugin, command);
    }
}
