package me.happyman.SpecialKitItems;

import me.happyman.ItemTypes.SmashItemWithCharge;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashAttackListener;
import me.happyman.utils.SmashEntityTracker;
import me.happyman.utils.SmashWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.Callable;

public class MarioFireball extends SmashItemWithCharge implements Listener
{
    private final int ticksLifeOfTNT = 36;
    private final int tntDmg = 17;

    public MarioFireball()
    {
        super(Material.FIREWORK_CHARGE, ChatColor.RED + "" + ChatColor.BOLD + "Fireball", 0.0026F, 0.5F, false);
        Bukkit.getPluginManager().registerEvents(this, SmashManager.getPlugin());
    }

    @Override
    public void performRightClickAction(final Player p)
    {
        if (canUseItem(p))
        {
            decreaseCharge(p);
            activateTask(p);
            launchTNT(p, p.getLocation().getDirection().multiply(1.3F));
        }
    }

    private void launchTNT(Player p, Vector launchVelocity)
    {
        launchTNT(p, launchVelocity, tntDmg);
    }

    private void launchTNT(Player p, Vector launchVelocity, int dmg)
    {
        launchTNT(p, launchVelocity, dmg, getItem().getItemMeta().getDisplayName());
    }

    public void launchTNT(final Player p, final Vector launchVelocity, int dmg, String culpritName)
    {
        final TNTPrimed tnt = (TNTPrimed)p.getWorld().spawnEntity(p.getLocation(), EntityType.PRIMED_TNT);
        p.getWorld().playSound(p.getLocation(), Sound.WOOD_CLICK, 1F, 0.2F);
        tnt.setFuseTicks(ticksLifeOfTNT);
        SmashEntityTracker.addCulprit(p, tnt, culpritName, dmg, true);
        Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
            public String call() {
                //Bukkit.getPlayer("HappyMan").sendMessage("setting launchVelocity of TNT");
                tnt.setVelocity(launchVelocity);
                return "";
            }
        });

        final int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable() {
            final float checkRange = 1.5F;
            public void run() {
                List<Entity> nearbyEntities = tnt.getNearbyEntities(checkRange, checkRange, checkRange);
                if (!nearbyEntities.contains(p))
                {
                    for (Entity e : nearbyEntities)
                    {
                        if (e instanceof Player)
                        {
                            tnt.setFuseTicks(0);
                        }
                    }
                    if (tnt.getFuseTicks() > 0)
                    {
                        final Vector v = tnt.getVelocity().clone();;
                        if (tnt.isOnGround())
                        {
                            v.setY(0.5);
                        }
                        else
                        {
                            v.setY(v.getY() - 0.07);

                        }
                        Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
                            public String call() {
                                //Bukkit.getPlayer("HappyMan").sendMessage("setting launchVelocity of TNT");
                                tnt.setVelocity(v);
                                return "";
                            }
                        });
                    }
                }
            }
        }, 0, 1);
        SmashManager.getPlugin().cancelTaskAfterDelay(task, ticksLifeOfTNT);
    }

    @EventHandler
    public void clickTheFireball(PlayerInteractEvent e)
    {
        if (e.getAction().equals(Action.LEFT_CLICK_BLOCK) || e.getAction().equals(Action.LEFT_CLICK_AIR))
        {
            Player p = e.getPlayer();
            Entity theEntity = SmashAttackListener.getEntityBeingFaced(p, 4, 22.5F);
            if (theEntity != null && theEntity instanceof TNTPrimed && SmashWorldManager.isSmashWorld(theEntity.getWorld())
                        && SmashEntityTracker.hasCulprit(theEntity) && SmashEntityTracker.getWeaponName(theEntity).equals(getItem().getItemMeta().getDisplayName())
                        && !SmashEntityTracker.getCulpritName(theEntity).equals(p.getName())
                    )
            {
                theEntity.remove();
                p.getWorld().playSound(p.getLocation(), Sound.EXPLODE, 0.7F, 2F);
            }
        }
    }
}