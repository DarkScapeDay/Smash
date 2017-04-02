package me.happyman.Listeners;

import me.happyman.source;
import me.happyman.utils.Verifier;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

/**
 * Created by Timothy on 1/6/2017.
 */
public class VerifierHandler implements Listener
{
    private final source plugin;

    public VerifierHandler(source plugin)
    {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }


    @EventHandler
    public void typingEvent(AsyncPlayerChatEvent e)
    {
        if (Verifier.isVerifier(e.getPlayer()))
        {
            Verifier.getPendingVerifiers().get(e.getPlayer()).handleResponse(e.getPlayer(), e.getMessage().substring(0, e.getMessage().length() - 1));
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void commandEvent(PlayerCommandPreprocessEvent e)
    {
        if (Verifier.isVerifier(e.getPlayer()))
        {
            e.setCancelled(true);
            Verifier.getPendingVerifiers().get(e.getPlayer()).handleResponse(e.getPlayer(), e.getMessage().substring(1, e.getMessage().length()));
        }
    }
}
