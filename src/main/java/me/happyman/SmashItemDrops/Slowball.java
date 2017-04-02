package me.happyman.SmashItemDrops;

import me.happyman.ItemTypes.SpeedChanger;
import me.happyman.commands.SmashManager;
import me.happyman.utils.SmashEntityTracker;
import me.happyman.utils.SmashWorldManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.entity.Snowball;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public class Slowball extends SpeedChanger implements Listener
{
    public Slowball()
    {
        super(Material.SNOW_BALL, ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Slowball", 0.707F, 15, true);
        Bukkit.getPluginManager().registerEvents(this, SmashManager.getPlugin());
    }

    @EventHandler
    public void onSnowballHit(EntityDamageByEntityEvent e)
    {
        World w = e.getEntity().getWorld();
        if (e.getDamager() instanceof Snowball && SmashEntityTracker.hasCulprit(e.getDamager())
                && SmashEntityTracker.getWeaponName(e.getDamager()).equals(getItem().getItemMeta().getDisplayName())
                && e.getEntity() instanceof Player
                && SmashWorldManager.isSmashWorld(w) && SmashWorldManager.gameHasStarted(w))
        {
            activateTask((Player)e.getEntity());
        }
    }

    @EventHandler
    public void nameThoseProjectiles(ProjectileLaunchEvent e)
    {
        if (e.getEntity().getShooter() instanceof Player && isThis(((Player)e.getEntity().getShooter()).getItemInHand()))//((Player)e.getEntity().getShooter()).getItemInHand().hasItemMeta()
               //&& ((Player)e.getEntity().getShooter()).getItemInHand().getItemMeta().hasDisplayName() && ((Player)e.getEntity().getShooter()).getItemInHand().getItemMeta().hasDisplayName())
        {
            Player p = (Player)e.getEntity().getShooter();
            SmashEntityTracker.addCulprit(p, e.getEntity(), getItem().getItemMeta().getDisplayName(), 7);
        }
    }
}
