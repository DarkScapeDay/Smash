package me.happyman.SmashItemDrops;

import me.happyman.ItemTypes.SmashItem;
import me.happyman.commands.SmashManager;
import me.happyman.utils.SmashEntityTracker;
import me.happyman.utils.SmashWorldManager;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public class SwitcherBall extends SmashItem implements Listener
{
    //private final String weaponName = ChatColor.WHITE + "" + ChatColor.BOLD + "Switcher Ball";

    public SwitcherBall()
    {
        super(Material.SNOW_BALL, ChatColor.WHITE + "" + ChatColor.BOLD + "Switcher Ball", new Enchantment[] {Enchantment.ARROW_INFINITE}, new int[] {1}, true);
        Bukkit.getPluginManager().registerEvents(this, SmashManager.getPlugin());
    }

    public void performRightClickAction(Player p) {}

    @EventHandler
    public void hitBySwitcherball(final EntityDamageByEntityEvent e)
    {
        final Entity damager = e.getDamager();
        final Entity damaged = e.getEntity();
        World w = damager.getWorld();
        if (SmashEntityTracker.hasCulprit(damager) && SmashEntityTracker.getWeaponName(damager).equals(getItem().getItemMeta().getDisplayName())
                && SmashWorldManager.isSmashWorld(w) && SmashWorldManager.gameHasStarted(w))
        {
            final Player shooter = Bukkit.getPlayer(SmashEntityTracker.getCulpritName(damager));
            if (shooter != null)
            {
                final Location l1 = shooter.getLocation().clone();
                //Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
                  //  public String call() {
                        shooter.teleport(damaged.getLocation());
                        damaged.teleport(l1);
                 //       return "";
                 //   }
                //});
            }
        }
    }

    @EventHandler
    public void throwSwitcherBall(ProjectileLaunchEvent e)
    {
        if (e.getEntity().getShooter() instanceof Player && isThis(((Player)e.getEntity().getShooter()).getItemInHand()))
        {
            final Player p = (Player)e.getEntity().getShooter();
            SmashEntityTracker.addCulprit(p, e.getEntity(), getItem().getItemMeta().getDisplayName(), -4);
        }
    }
}
