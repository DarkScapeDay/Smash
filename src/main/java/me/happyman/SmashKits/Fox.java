package me.happyman.SmashKits;

import me.happyman.ItemTypes.RocketItem;
import me.happyman.ItemTypes.SmashItemWithCharge;
import me.happyman.SmashKitMgt.*;
import me.happyman.SpecialKitItems.FoxFinalSmash;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashAttackListener;
import me.happyman.utils.ParticleEffect;
import me.happyman.utils.SmashEntityTracker;
import org.bukkit.*;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.concurrent.Callable;

public class Fox extends SmashKit implements Listener
{
    private final ParticleEffect.OrdinaryColor laserColor = new ParticleEffect.OrdinaryColor(0, 255, 0);

    public Fox()
    {
        super(
            new SmashKitProperties(ChatColor.GOLD + "" + ChatColor.BOLD + "Fox", Material.INK_SACK, (short) 14, ChatColor.GOLD + "" + ChatColor.ITALIC + "Here I come!",
                new FoxFinalSmash(), 1, 1, false),
            new LeatherArmorSet(ChatColor.GOLD + "" + ChatColor.BOLD + "Fox's Armor", null, new int[]{121, 127, 125}, new int[]{61, 71, 46}, new int[]{122, 57, 63}),
            null,
            new RocketItem(ChatColor.RED+ "" + ChatColor.BOLD + "Double-Jump", 2, 1F)
            {
                @Override
                public boolean canUseItem(Player p)
                {
                    return super.canUseItem(p) && !SmashKitManager.hasFinalSmashActive(p);
                }
            }
        );
        addItem(new SmashItemWithCharge(Material.FLINT, ChatColor.BLACK + "" + ChatColor.BOLD + "Laser Gun", 0.0016666F, 0.066666F, false)
        {
            private final int LASER_POWER = 6;
            private final float LASER_SPEED = 60F;


            private void playLaserSound(Location l, int numOfTimes)
            {
                for (int m = 0; m < numOfTimes; m++)
                {
                    l.getWorld().playSound(l, Sound.FIREWORK_LAUNCH, .2F, 0.15F);
                }
            }

            public void performRightClickAction(Player p)
            {
                if (canUseItem(p))
                {
                    decreaseCharge(p);
                    activateTask(p);

                    final Vector v = p.getLocation().getDirection().multiply(LASER_SPEED);
                    Location launchLoc = SmashManager.getSafeProjLaunchLocation(p);
                    Vector facing = p.getLocation().getDirection().multiply(0.2);
                    double i = launchLoc.getX();
                    double j = launchLoc.getY();
                    double k = launchLoc.getZ();

                    final Arrow laser = (Arrow)p.getWorld().spawnEntity(launchLoc, EntityType.ARROW);
                    playLaserSound(launchLoc, 1);
                    for (int x = 0; x < 500; x++)
                    {
                        i += facing.getX();
                        j += facing.getY();
                        k += facing.getZ();
                        Location lForT = new Location(launchLoc.getWorld(), i, j, k);
                        if (!lForT.getBlock().getType().equals(Material.AIR))
                        {
                            break;
                        }
                        if (x % 7 == 0)
                        {
                            playLaserSound(lForT, 1);
                        }
                        SmashManager.playBasicParticle(lForT, laserColor, true);
                    }
                    int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable()
                    {
                        int i = 0;
                        @Override
                        public void run() {
                            if (i < SmashEntityTracker.ARROW_TRACKING_TIME - 1)
                            {
                                laser.setVelocity(v);
                            }
                            else
                            {
                                laser.remove();
                            }
                        }
                    }, 0, 1);
                    SmashManager.getPlugin().cancelTaskAfterDelay(task, SmashEntityTracker.ARROW_TRACKING_TIME);


                    SmashEntityTracker.addCulprit(p, laser, getItem().getItemMeta().getDisplayName(), LASER_POWER, true);
                }
            }

        }, 0);
        addItem(new SmashItemWithCharge(Material.FENCE, ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Kick", null, 0.14F, 1F, false, true)
        {
            final float KICK_RANGE = 2.5F;
            final float KICK_ANGLE = 70F;
            final float KICK_POWER = 15F;

            @Override
            public void performRightClickAction(final Player p)
            {
                p.updateInventory();
                Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
                    public String call() {
                        p.updateInventory();
                        return "";
                    }
                });
                if (canUseItem(p))
                {
                    activateTask(p);
                    decreaseCharge(p);
                    p.getWorld().playSound(p.getLocation(), Sound.BAT_TAKEOFF, 1F, 0.2F);
                    SmashAttackListener.attackPlayersInAngleRange(p, this, KICK_POWER, KICK_RANGE, KICK_ANGLE, KICK_ANGLE, isProjectile());
                }
            }
        });
        addSecretItem(((FoxFinalSmash)getProperties().getFinalSmash()).getFirer());
    }
}
