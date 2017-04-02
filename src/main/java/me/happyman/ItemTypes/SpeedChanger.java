package me.happyman.ItemTypes;

import me.happyman.commands.SmashManager;
import me.happyman.utils.SmashEntityTracker;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

public abstract class SpeedChanger extends SmashItemWithOverlappingTasks
{
    private final float SPEED_FACTOR;
    private final int SPEED_DURATION;

    public SpeedChanger(Material mat, String name, float speedFactor, int speedDuration)
    {
        super(mat, name);
        SPEED_DURATION = speedDuration;
        SPEED_FACTOR = speedFactor;
    }

    public SpeedChanger(Material mat, String name, float speedFactor, int speedDuration, boolean isProjectile)
    {
        super(mat, name, isProjectile);
        SPEED_DURATION = speedDuration;
        SPEED_FACTOR = speedFactor;
    }

    public SpeedChanger(Material mat, String name, float speedFactor, int speedDuration, String lore)
    {
        super(mat, name, lore);
        SPEED_DURATION = speedDuration;
        SPEED_FACTOR = speedFactor;
    }

    public SpeedChanger(Material mat, String name, float speedFactor, int speedDuration, Enchantment[] enchants, int[] levels)
    {
        super(mat, name, enchants, levels);
        SPEED_DURATION = speedDuration;
        SPEED_FACTOR = speedFactor;
    }

    public void activateTask(final Player p)
    {
        if (SmashEntityTracker.putSpeedFactor(p, SPEED_FACTOR, this))
        {
            putTask(p, Bukkit.getScheduler().scheduleSyncDelayedTask(SmashManager.getPlugin(), new Runnable()
            {
                public void run()
                {
                    SmashEntityTracker.removeSpeedFactor(p, SPEED_FACTOR);
                }
            }, 20*SPEED_DURATION));
        }
    }
}
