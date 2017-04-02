package me.happyman.SpecialKitItems;

import me.happyman.ItemTypes.SmashItemWithCharge;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashAttackListener;
import me.happyman.utils.ParticleEffect;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.HashMap;
import java.util.Random;

public class SonicHoming extends SmashItemWithCharge implements Listener
{
    private HashMap<Player, Integer> movementTasks;
    private final int HOW_LONG_IT_TAKES_TICKS = 1*20;
    private final float ATTACK_RANGE = 9F;
    private final float FINAL_RANGE = 1.3F;
    private final float ATTACK_POWER = 7F/SmashAttackListener.DAMAGE_GAIN_FACTOR;
    private final float SLOPE_TO_WAYPOINT = 1F;
    private final int PARTICLE_COUNT = 60;
    private static final float PARTICLE_RAD = 0.5F;
    private Random r = new Random();
    private final ParticleEffect.OrdinaryColor blueColor = SmashManager.getParticleColor(0, 0, 255);
    private static final float MIN_RISE = 5F;

    public SonicHoming()
    {
        super(Material.SLIME_BALL, ChatColor.BLUE + "" + ChatColor.BOLD + "Homing Attack", new Enchantment[] {Enchantment.DIG_SPEED}, new int[] {9001}, .025F, 1, false);
        movementTasks = new HashMap<Player, Integer>();
    }

    @Override
    public boolean canUseItem(Player p)
    {
        return super.canUseItem(p) && !movementTasks.containsKey(p);
    }

    @Override
    public void performRightClickAction(final Player p)
    {
        final Player targetPlayer = SmashManager.getNearestPlayerToPlayer(p);
        if (targetPlayer != null)
        {
            final Location targetLocation = targetPlayer.getLocation();
            if (p.getLocation().distance(targetLocation) <= ATTACK_RANGE)
            {
                float flatD = (float)Math.sqrt(Math.pow(targetLocation.getX() - p.getLocation().getX(), 2) + Math.pow(targetLocation.getY() - p.getLocation().getY(), 2));
                final Location wayPoint = p.getLocation().clone();
                wayPoint.setY(targetLocation.getY() + SLOPE_TO_WAYPOINT*flatD);
                if (wayPoint.getY() - p.getLocation().getY() < MIN_RISE)
                {
                    wayPoint.setY(p.getLocation().getY() + MIN_RISE);
                }
                int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable() {
                    Location currentTarget = wayPoint;
                    boolean takenCareOfThankYou = false;

                    private boolean isCloseToTarget(Player p)
                    {
                        return currentTarget != null && p.getLocation().distance(currentTarget) < 0.5F;
                    }

                    public void run()
                    {
                        if (currentTarget != null)
                        {
                            for (int i = 0; i < PARTICLE_COUNT; i++)
                            {
                                float pitch = (float)(r.nextFloat()*Math.PI - Math.PI/2);
                                float yaw = (float)(r.nextFloat()*2*Math.PI);
                                SmashManager.playBasicParticle(
                                        SmashManager.getAbsFromRelLocFRU(p.getEyeLocation(),
                                                (float)(Math.cos(pitch)*Math.cos(yaw)*PARTICLE_RAD),
                                                (float)(Math.cos(pitch)*Math.sin(yaw)*PARTICLE_RAD),
                                                (float)(Math.sin(pitch)*PARTICLE_RAD), false),
                                        blueColor, true);
                            }

                            SmashAttackListener.sendEntityTowardLocation(currentTarget, p, 1, false);
                            if (isCloseToTarget(p))
                            {
                                if (currentTarget == wayPoint)
                                {
                                    currentTarget = targetLocation;
                                }
                                else if (currentTarget == targetLocation)
                                {
                                    currentTarget = null;
                                }
                            }

                            if (SmashAttackListener.attackPlayersInRange(p, "Homing Attack", ATTACK_POWER, FINAL_RANGE))
                            {
                                currentTarget = null;
                            }
                        }
                        else if (!takenCareOfThankYou)
                        {
                            movementTasks.remove(p);
                            takenCareOfThankYou = true;
                        }
                    }
                }, 0, 1);
                movementTasks.put(p, task);
                SmashManager.getPlugin().cancelTaskAfterDelay(task, HOW_LONG_IT_TAKES_TICKS);
            }
        }
    }

    @EventHandler
    public void oopsGotHit(EntityDamageByEntityEvent e)
    {
        if (e.getEntity() instanceof Player)
        {
            Player p = (Player)e.getEntity();
            if (movementTasks.containsKey(p))
            {
                Bukkit.getScheduler().cancelTask(movementTasks.get(p));
                movementTasks.remove(p);
            }
        }
    }
}
