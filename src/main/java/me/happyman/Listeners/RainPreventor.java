package me.happyman.Listeners;

import me.happyman.source;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.weather.WeatherChangeEvent;

public class RainPreventor implements Listener
{
    private final source plugin;
    private int score;

    public RainPreventor(source plugin)
    {
        this.plugin = plugin;
        score = 0;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void rainEvent(WeatherChangeEvent e)
    {
        score++;
        String attachment = plugin.getOrdinalIndicator(score);
        plugin.getServer().getConsoleSender().sendMessage(plugin.loggerPrefix() + ChatColor.GREEN + "Prevented " + e.getEventName() + " for the " + score + attachment + " time since startup!");
        e.setCancelled(true);
    }
}
