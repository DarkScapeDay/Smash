package me.happyman.SpecialItems.SpecialItemTypes;

import me.happyman.SpecialItems.UsefulItemStack;
import me.happyman.worlds.WorldManager;
import me.happyman.worlds.WorldType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.Callable;

import static me.happyman.Plugin.cancelTaskAfterDelay;
import static me.happyman.Plugin.getPlugin;

public class FireballLauncher extends SpecialItemWithCharge implements Listener
{
    private static final int TICKS_LIFE_OF_TNT = 36;
    private static final int TNT_DMG = 17;
    private static final String ITEM_NAME = ChatColor.RED + "" + ChatColor.BOLD + "Fireball";

    private static FireballLauncher instance = null;

    public FireballLauncher()
    {
        super(new UsefulItemStack(Material.FIREWORK_CHARGE, ITEM_NAME), 0.0026F, 0.5F, SpecialItemWithCharge.ChargingMode.CHARGE_AUTOMATICALLY);
        Bukkit.getPluginManager().registerEvents(this, getPlugin());

        if (instance == null)
        {
            instance = this;
        }
    }

    @Override
    public void performRightClickAction(final Player p, Block blockClicked)
    {
        super.performRightClickAction(p, blockClicked);
        launchTNT(p, p.getLocation().getDirection().multiply(1.3F));
    }

    @Override
    public void performLeftClickAction(Player p, Block blockClicked)
    {
        super.performLeftClickAction(p, blockClicked);
        performRightClickAction(p, blockClicked);
    }

    private void launchTNT(Player p, Vector launchVelocity)
    {
        launchTNT(p, launchVelocity, TNT_DMG);
    }

    private void launchTNT(Player p, Vector launchVelocity, int dmg)
    {
        launchTNT(p, launchVelocity, dmg, ITEM_NAME);
    }

    public static Entity launchTNT(final Player p, final Vector launchVelocity, int dmg, String culpritName)
    {
        final TNTPrimed tnt = (TNTPrimed)p.getWorld().spawnEntity(p.getLocation(), EntityType.PRIMED_TNT);
        p.getWorld().playSound(p.getLocation(), Sound.BLOCK_STONE_PRESSUREPLATE_CLICK_ON, 1F, 0.2F);
        tnt.setFuseTicks(TICKS_LIFE_OF_TNT);
        Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable() {
            public String call() {
                //Bukkit.getAttacker("HappyMan").sendMessage("setting launchVelocity of TNT");
                tnt.setVelocity(launchVelocity);
                return "";
            }
        });
        WorldManager.setAttackSource(tnt, new WorldType.AttackSource(new WorldType.AttackSource.AttackCulprit(p,  instance), true, dmg));

        final int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
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
                        Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable() {
                            public String call() {
                                //Bukkit.getAttacker("HappyMan").sendMessage("setting launchVelocity of TNT");
                                tnt.setVelocity(v);
                                return "";
                            }
                        });
                    }
                }
            }
        }, 0, 1);
        cancelTaskAfterDelay(task, FireballLauncher.TICKS_LIFE_OF_TNT);
        return tnt;
    }

    @Override
    public void performLeftClickedWhileInMidAirAction(Player clicker, Entity projectileClicked)
    {
        super.performLeftClickedWhileInMidAirAction(clicker, projectileClicked);
        projectileClicked.remove();
        clicker.getWorld().playSound(clicker.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 0.7F, 2F);
    }

//    @EventHandler
//    public static void clickTheFireball(PlayerInteractEvent e)
//    {
//        if (e.getAction().equals(Action.LEFT_CLICK_BLOCK) || e.getAction().equals(Action.LEFT_CLICK_AIR))
//        {
//            Player p = e.getAttacker();
//            if (SmashWorldManager.isSmashWorld(p.getWorld()))
//            {
//                Entity theEntity = SmashAttackManager.getEntityBeingFaced(p, 4, 22.5F);
//                if (theEntity != null && theEntity instanceof TNTPrimed && SmashWorldManager.isSmashWorld(theEntity.getWorld())
//                        && SmashEntityTracker.hasCulprit(theEntity) && SmashEntityTracker.getWeaponName(theEntity).equals(ITEM_NAME)
//                        && !SmashEntityTracker.getCulpritName(theEntity).equals(p.getName())
//                        )
//                {
//                    theEntity.remove();
//                    p.getWorld().playSound(p.getLocation(), Sound.EXPLODE, 0.7F, 2F);
//                }
//            }
//        }
//    }
}