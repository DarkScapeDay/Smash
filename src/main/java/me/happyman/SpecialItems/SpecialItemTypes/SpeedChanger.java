package me.happyman.SpecialItems.SpecialItemTypes;

import me.happyman.SpecialItems.UsefulItemStack;
import me.happyman.utils.SmashEntityTracker;
import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import static me.happyman.Plugin.getPlugin;

public abstract class SpeedChanger extends SpecialItemWithTask
{
    private final float SPEED_FACTOR;
    private final int SPEED_DURATION;

    public SpeedChanger(UsefulItemStack item, float speedFactor, int speedDuration)
    {
        super(item);
        SPEED_DURATION = speedDuration;
        SPEED_FACTOR = speedFactor;
    }

    @Override
    public void performRightClickAction(final Player p, Block blockClicked)
    {
        if (SmashEntityTracker.putSpeedFactor(p, SPEED_FACTOR, this))
        {
            addTask(p, Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable()
            {
                public void run()
                {
                    SmashEntityTracker.removeSpeedFactor(p, SPEED_FACTOR);
                }
            }, 20*SPEED_DURATION));
        }
    }
}
