package me.happyman.SpecialKitItems;

import me.happyman.ItemTypes.SmashItemWithCharge;
import me.happyman.SmashKitMgt.SmashKitManager;
import me.happyman.SmashKits.Kirby;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashAttackListener;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class KirbySword extends SmashItemWithCharge implements Listener
{
    private HashMap<Player, Integer> finalCutterTasks;
    private HashMap<Entity, Integer> fallingTasks;

    public KirbySword()
    {
        super(Material.IRON_SWORD, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Sword", 0.1F, 1F, false);//, new Enchantment[]{Enchantment.DAMAGE_ALL}, new int[]{8});
        Bukkit.getPluginManager().registerEvents(this, SmashManager.getPlugin());
        finalCutterTasks = new HashMap<Player, Integer>();
        fallingTasks = new HashMap<Entity, Integer>();
    }

    public void cancelTask(Player p, boolean justCancelActualAction)
    {
        if (SmashKitManager.canChangeKit(p) || justCancelActualAction)
        {
            cancelFinalCutter(p);
            cancelFallingTask(p);
        }
        if (!justCancelActualAction)
        {
            super.cancelTask(p);
        }
    }


    public void cancelTask(Player p)
    {
        cancelTask(p, false);
    }

    private void cancelFinalCutter(Player p)
    {
        if (finalCutterTasks.containsKey(p))
        {
            Bukkit.getScheduler().cancelTask(finalCutterTasks.get(p));
            finalCutterTasks.remove(p);
        }
    }

    private void cancelFallingTask(Player p)
    {
        if (fallingTasks.containsKey(p))
        {
            Bukkit.getScheduler().cancelTask(fallingTasks.get(p));
            fallingTasks.remove(p);
        }
    }

    public void performRightClickAction(final Player p)
    {
        if (canUseItem(p) && !fallingTasks.containsKey(p)) //&& !SmashKitManager.canChangeKit(p))
        {
            //Bukkit.getPlayer("HappyMan").sendMessage("setting velocity for Kirby final cutter");
            p.setVelocity(new Vector(0, 1.42, 0));
            fallingTasks.put(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable()
            {
                int i = 0;
                int delay = 15;
                public void run()
                {
                    if (i > 3 && p.getLocation().getY() % 0.5 < 0.20001 && p.getLocation().getY() % 0.5 > 0.1999)
                    {
                        i = delay;
                    }
                    if (i < delay)
                    {
                        i++;
                    }
                    else if (i == delay)
                    {
                        //Bukkit.getPlayer("HappyMan").sendMessage("setting velocity for final cutter");
                        p.setVelocity(new Vector(0, -4, 0));
                        i++;
                    }
                }
            }, 0, 1));
        }
    }

    @EventHandler
    public void finalCutter(EntityDamageEvent e)
    {
        if (e.getCause().equals(EntityDamageEvent.DamageCause.FALL) && fallingTasks.containsKey(e.getEntity()))
        {
            final Player p = (Player)e.getEntity();
            p.getWorld().playSound(p.getLocation(), Sound.LAVA_POP, 1F, 1F);
            final Item sword = p.getWorld().dropItem(p.getLocation().add(0, 0.2, 0), getItem());

            final Vector v = SmashManager.getVectorOfYaw(p, 1.2F).setY(0.09F);
            final int life = 17;
            setCharge(p, 0);
            activateTask(p);
            cancelFinalCutter(p);
            Bukkit.getScheduler().scheduleSyncDelayedTask(SmashManager.getPlugin(), new Runnable() {
                public void run() {
                    sword.remove();
                }
            }, life);
            finalCutterTasks.put(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable() {
                int i = 0;
                List<Location> locations = new ArrayList<Location>();
                public void run()
                {
                    if (i <= life)
                    {
                        for (Entity e : sword.getNearbyEntities(1, 0.2, 1))
                        {
                            if (e instanceof Player && !e.equals(p))
                            {
                                locations.add(0, sword.getLocation());
                                if (locations.size() > 3)
                                {
                                    locations.remove(3);
                                }
                                SmashAttackListener.attackPlayer(p, "Final Cutter", locations.get(locations.size() - 1), (Player)e, 12, true);
                            }
                        }
                        //Bukkit.getPlayer("HappyMan").sendMessage("setting velocity of final cutter");
                        sword.setVelocity(v);
                        i++;
                    }
                }
            }, 0, 1));
            cancelFallingTask(p);
        }
    }

    @EventHandler
    public void preventFinalSwordPickup(PlayerPickupItemEvent e)
    {
        ItemStack item = e.getItem().getItemStack();
        if (isThis(item))
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void onRightClickDuringFall(PlayerInteractEvent e)
    {
        if (!isThis(e.getItem()) && SmashKitManager.getSelectedKit(e.getPlayer()) instanceof Kirby)
        {
            cancelTask(e.getPlayer(), true);
        }
    }

    /*public static Pig spawnLargeItem(Location l, ItemStack item)
    {
        Giant giant = (Giant)l.getWorld().spawn(l, Giant.class); //.spawnEntity(l.clone().add(0, 7, 0), EntityType.GIANT);
        //giant.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100000, 2, false, false));
        giant.getEquipment().setItemInHand(item);

        Pig pig = (Pig)l.getWorld().spawnEntity(l, EntityType.PIG);
        //pig.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 10000, 1, false, false));
        pig.setPassenger(giant);

        return pig;

    }*/
}
