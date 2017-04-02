package me.happyman.Listeners;

import me.happyman.SmashKitMgt.SmashKitManager;
import me.happyman.commands.SmashManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;

public class SmashKitListener implements Listener
{

    public SmashKitListener()
    {
        Bukkit.getPluginManager().registerEvents(this, SmashManager.getPlugin());
    }

    //*****************

    @EventHandler
    public void closeInvEvent(InventoryCloseEvent e)
    {
        if (SmashKitManager.hasKitGuiOpen((Player)e.getPlayer()))
        {
            SmashKitManager.forgetOpenKitGui((Player)e.getPlayer());
        }
    }

    @EventHandler
    public void clickRepresenterEvent(InventoryClickEvent e)
    {
        if (e.getWhoClicked() instanceof Player)
        {
            Player p = (Player)e.getWhoClicked();
            if (SmashKitManager.isKitRepresenter(e.getCurrentItem()))
            {
                e.setCancelled(true);
                SmashKitManager.setKit(p, SmashKitManager.getKitRepresented(e.getCurrentItem()));
            }
        }
    }
}
