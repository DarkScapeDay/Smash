package me.happyman.SpecialKitItems;

import me.happyman.ItemTypes.SmashItem;
import me.happyman.commands.SmashManager;
import me.happyman.utils.SmashEntityTracker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;

import java.util.concurrent.Callable;

public class GanondorfEgg extends SmashItem implements Listener
{
    public GanondorfEgg()
    {
        super(Material.EGG, ChatColor.YELLOW + "Egg", ChatColor.GRAY + "Maybe this kit isn't so bad after all...", true);
        Bukkit.getPluginManager().registerEvents(this, SmashManager.getPlugin());
    }

    public void performRightClickAction(final Player p) {}

    @EventHandler
    public void throwEgg(ProjectileLaunchEvent e)
    {
        if (e.getEntity().getShooter() instanceof Player && isThis(((Player)e.getEntity().getShooter()).getItemInHand()))
        {
            final Player p = (Player)e.getEntity().getShooter();
            p.setItemInHand(getItem());
            Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
                public String call() {
                    p.setItemInHand(getItem());
                    p.updateInventory();
                    return "";
                }
            });
            SmashEntityTracker.addCulprit(p, e.getEntity(), getItem().getItemMeta().getDisplayName(), 5);
        }
    }
}
