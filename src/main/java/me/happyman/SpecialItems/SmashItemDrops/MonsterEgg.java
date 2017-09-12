package me.happyman.SpecialItems.SmashItemDrops;

import me.happyman.Plugin;
import me.happyman.SpecialItems.SpecialItem;
import me.happyman.SpecialItems.UsefulItemStack;
import me.happyman.utils.SmashManager;
import me.happyman.worlds.SmashWorldManager;
import me.happyman.worlds.WorldManager;
import me.happyman.worlds.WorldType;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static me.happyman.Plugin.getPlugin;

public class MonsterEgg extends SpecialItem
{
    private static final int MONSTER_DURATION = 30;

    private static List<EntityType> monsterTypes = new ArrayList<EntityType>();;
    private static HashMap<World, ArrayList<Integer>> monsterTasks = new HashMap<World, ArrayList<Integer>>();

    public MonsterEgg()
    {
        super(new UsefulItemStack(Material.EGG, ChatColor.YELLOW + "" + ChatColor.BOLD + "Spawn Egg"));

        monsterTypes.add(EntityType.BLAZE);
        monsterTypes.add(EntityType.CREEPER);
        monsterTypes.add(EntityType.PIG_ZOMBIE);
        monsterTypes.add(EntityType.SKELETON);
        monsterTypes.add(EntityType.ZOMBIE);
        monsterTypes.add(EntityType.ENDER_DRAGON);
    }

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
        addMonsterTask(monster.getWorld(), Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), r, ticks));
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

    @Override
    public float getRangeDamage(EntityDamageByEntityEvent event)
    {
        return 5;
    }

    @Override
    public void performProjectileLand(final LivingEntity shooter, Projectile projectileThatLanded)
    {
        super.performProjectileLand(shooter, projectileThatLanded);
        if (SmashWorldManager.gameIsInProgress(projectileThatLanded.getWorld()) && shooter instanceof Player)
        {
            String owner = shooter.getName();
            Location l = projectileThatLanded.getLocation();
            l.setY(l.getY() + 1 - l.getY()%1 + 0.5);
            EntityType type = monsterTypes.get((new Random()).nextInt(monsterTypes.size()));
            final LivingEntity monster = (LivingEntity)l.getWorld().spawnEntity(l, type);

            String monsterType = Plugin.capitalize(monster.getType().toString().toLowerCase());
            int power = 30;
            if (monster.getType().equals(EntityType.BLAZE))
            {
                power = 24;
            }
            else if (monster.getType().equals(EntityType.CREEPER))
            {
                power = 60;
            }
            else if (monster.getType().equals(EntityType.PIG_ZOMBIE))
            {
                monsterType = "Zombie Pigman";
                power = 20;
            }
            else if (monster.getType().equals(EntityType.SKELETON))
            {
                power = 20;
            }
            else if (monster.getType().equals(EntityType.ZOMBIE))
            {
                power = 20;
            }
            else if (monster.getType().equals(EntityType.ENDER_DRAGON))
            {
                power = 40;
            }

            monster.setCustomName(ChatColor.BLUE + owner + "'s " + monsterType);
            WorldManager.setAttackSource(monster, new WorldType.AttackSource(new WorldType.AttackSource.AttackCulprit(shooter, this, monsterType), false, MONSTER_DURATION*20, power));

            monster.setMaxHealth(40);
            monster.setHealth(40);
            LivingEntity target = SmashManager.getNearestEntityExcept(monster, shooter, false);
            if (target != null && monster instanceof Monster)
            {
                ((Monster)monster).setTarget(target);
            }

            monster.addPotionEffect(new PotionEffect(PotionEffectType.SPEED,10000, 2, false, false));
        }
    }
}
