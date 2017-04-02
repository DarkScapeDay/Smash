package me.happyman.SmashItemDrops;

import me.happyman.ItemTypes.SmashItem;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashAttackListener;
import me.happyman.utils.SmashEntityTracker;
import me.happyman.utils.SmashWorldManager;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.*;

public class MonsterEgg extends SmashItem implements Listener
{
    private static final int MONSTER_DURATION = 30;

    private static List<EntityType> monsterTypes;
    private static HashMap<World, ArrayList<Integer>> monsterTasks;

    public MonsterEgg()
    {
        super(Material.EGG, ChatColor.YELLOW + "" + ChatColor.BOLD + "Spawn Egg", true);
        Bukkit.getPluginManager().registerEvents(this, SmashManager.getPlugin());

        monsterTasks = new HashMap<World, ArrayList<Integer>>();

        monsterTypes = new ArrayList<EntityType>();
        monsterTypes.add(EntityType.BLAZE);
        monsterTypes.add(EntityType.CREEPER);
        monsterTypes.add(EntityType.PIG_ZOMBIE);
        monsterTypes.add(EntityType.SKELETON);
        monsterTypes.add(EntityType.ZOMBIE);
        monsterTypes.add(EntityType.ENDER_DRAGON);
    }

    /*public static String getMobType(String mobName)
    {
        int i = 0;
        while (i < mobName.length() && mobName.charAt(i) != '\'')
        {
            i++;
        }
        return mobName.substring(i + 3, mobName.length());
    }

    public static String getMobOwner(String mobName)
    {
        int i = 0;
        while (i < mobName.length() && mobName.charAt(i) != '\'')
        {
            i++;
        }
        return ChatColor.stripColor(mobName.substring(0, i));
    }*/

    public static void cancelMonsterKillerTasks(World w)
    {
        if (monsterTasks.containsKey(w))
        {
            while (monsterTasks.get(w).size() > 0)
            {
                Bukkit.getScheduler().cancelTask(monsterTasks.get(w).get(0));
                monsterTasks.get(w).remove(0);
            }
            monsterTasks.remove(w);
        }
    }

    public static List<EntityType> getMonsters()
    {
        return monsterTypes;
    }

    public void killMobLater(final Entity monster, int ticks)
    {
        Runnable r = new Runnable()
        {
            public void run()
            {
                monster.remove();
            }
        };
        addMonsterTask(monster.getWorld(), Bukkit.getScheduler().scheduleSyncDelayedTask(SmashManager.getPlugin(), r, ticks));
    }

    public static int addMonsterTask(World w, int taskNumber)
    {
        if (!monsterTasks.containsKey(w))
        {
            monsterTasks.put(w, new ArrayList<Integer>());
        }
        monsterTasks.get(w).add(taskNumber);
        return taskNumber;
    }

    @EventHandler
    public void projLaunch(ProjectileLaunchEvent e)
    {
        if (e.getEntity().getShooter() instanceof Player && isThis(((Player)e.getEntity().getShooter()).getItemInHand())) // ((Player)e.getEntity().getShooter()).getItemInHand().hasItemMeta()
           // && ((Player)e.getEntity().getShooter()).getItemInHand().getItemMeta().hasDisplayName() && ((Player)e.getEntity().getShooter()).getItemInHand().getItemMeta().hasDisplayName()
           // && ((Player)e.getEntity().getShooter()).getItemInHand().getItemMeta().hasDisplayName().is
        {
            Player p = (Player)e.getEntity().getShooter();
            SmashEntityTracker.addCulprit(p, e.getEntity(), p.getItemInHand().getItemMeta().getDisplayName(), 5);
        }
    }

    @EventHandler
    public void preventMobsTargettingOwners(EntityTargetLivingEntityEvent e)
    {
        if (e.getEntity().getCustomName() != null && e.getTarget() != null && ChatColor.stripColor(e.getEntity().getCustomName()).startsWith(e.getTarget().getName() + "'"))
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void eggMobSpawn(ProjectileHitEvent e)
    {
        if (e.getEntity() instanceof Egg && e.getEntity().getShooter() instanceof Player
                && SmashWorldManager.gameHasStarted(e.getEntity().getWorld()) && !SmashWorldManager.gameHasEnded(e.getEntity().getWorld()) && SmashEntityTracker.hasCulprit(e.getEntity()) && SmashEntityTracker.getWeaponName(e.getEntity()).equals(getItem().getItemMeta().getDisplayName()))
        {
            final Player p = (Player)e.getEntity().getShooter();
            String owner = p.getName();
            Location l = e.getEntity().getLocation();
            l.setY(l.getY() + 1 - l.getY()%1 + 0.5);
            EntityType type = monsterTypes.get((new Random()).nextInt(monsterTypes.size()));
            final LivingEntity monster = (LivingEntity)l.getWorld().spawnEntity(l, type);

            String monsterType = SmashManager.getPlugin().capitalize(monster.getType().toString().toLowerCase());
            int power = 15;
            if (monster.getType().equals(EntityType.BLAZE))
            {
                power = 12;
            }
            else if (monster.getType().equals(EntityType.CREEPER))
            {
                power = 30;
            }
            else if (monster.getType().equals(EntityType.PIG_ZOMBIE))
            {
                monsterType = "Zombie Pigman";
                power = 10;
            }
            else if (monster.getType().equals(EntityType.SKELETON))
            {
                power = 10;
            }
            else if (monster.getType().equals(EntityType.ZOMBIE))
            {
                power = 10;
            }
            else if (monster.getType().equals(EntityType.ENDER_DRAGON))
            {
                power = 20;
            }
            power /= SmashAttackListener.DAMAGE_GAIN_FACTOR;

            SmashEntityTracker.addCulprit(p, monster, monsterType, power, true);
            monster.setCustomName(ChatColor.BLUE + owner + "'s " + monsterType);
            killMobLater(monster, MONSTER_DURATION*20);

            //monster.setFireTicks(0);
            //monster.setMetadata("generic.followRange", (MetadataValue)100);
            monster.setMaxHealth(40);
            monster.setHealth(40);
            LivingEntity target = SmashManager.getNearestEntityExcept(monster, p, false);
            if (target != null && monster instanceof Monster)
            {
                ((Monster)monster).setTarget(target);
            }

            monster.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,10000, 2, false, false));
            /*if (monster instanceof Skeleton)
            {
                int task = addMonsterTask(w, Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
                    public void run()
                    {
                        //Bukkit.broadcastMessage("spawning arrow");
                        Location arrowLocation = monster.getLocation();
                        Vector arrowDirection = arrowLocation.getDirection();
                        arrowLocation.setX(arrowLocation.getX() + 1.5*arrowDirection.getX());
                        arrowLocation.setY(arrowLocation.getX() + 1.5*arrowDirection.getY());
                        arrowLocation.setZ(arrowLocation.getX() + 1.5*arrowDirection.getZ());

                        Arrow arrow = (Arrow)monster.getWorld().spawnEntity(arrowLocation, EntityType.ARROW);
                        arrow.setVelocity(arrowDirection);
                        addArrowOwner(arrow, p.getName(), (Skeleton)monster);
                    }
                }, 2, 10));
                plugin.cancelTaskAfterDelay(task, MONSTER_DURATION*20);
            }*/
        }
    }

    public void performRightClickAction(Player p){}
}
