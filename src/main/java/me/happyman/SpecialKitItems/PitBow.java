package me.happyman.SpecialKitItems;

import me.happyman.ItemTypes.SmashItem;
import me.happyman.commands.SmashManager;
import me.happyman.utils.ParticleEffect;
import me.happyman.utils.SmashEntityTracker;
import org.bukkit.*;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;

public class PitBow extends SmashItem implements Listener
{
    private final ParticleEffect.OrdinaryColor color;

    public PitBow()
    {
        super(Material.BOW, ChatColor.YELLOW + "" + ChatColor.BOLD + "Bow of Light", new Enchantment[] {Enchantment.DAMAGE_ALL, Enchantment.ARROW_INFINITE}, new int[] {7, 1}, true);
        Bukkit.getPluginManager().registerEvents(this, SmashManager.getPlugin());
        color = new ParticleEffect.OrdinaryColor(0, 0, 255);
    }
    public void performRightClickAction(Player p) {}

    @EventHandler
    public void fancy(final ProjectileLaunchEvent e)
    {
        if (e.getEntity() instanceof Arrow && e.getEntity().getShooter() instanceof Player && isBeingHeld((Player)e.getEntity().getShooter()))
        {
            final Arrow arrow = (Arrow)e.getEntity();
            final World w = arrow.getWorld();
            int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable()
            {
                @Override
                public void run()
                {
                    SmashManager.playBasicParticle(arrow, color, false);
                }
            }, 0, 0);
            SmashManager.getPlugin().cancelTaskAfterDelay(task, SmashEntityTracker.ARROW_TRACKING_TIME);
        }
    }
}
