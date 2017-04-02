package me.happyman.SmashItemDrops;

import me.happyman.ItemTypes.SmashItemWithOverlappingTasks;
import me.happyman.commands.SmashManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;


public class HealingItem extends SmashItemWithOverlappingTasks implements Listener
{
    private final int healAmount;
    private final boolean healAll;

    public HealingItem(Material mat, String name)
    {
        super(mat, name);
        healAll = true;
        healAmount = -1;
    }

    public HealingItem(Material mat, String name, int healAmount)
    {
        super(mat, name);
        this.healAmount = healAmount;
        healAll = false;
        Bukkit.getPluginManager().registerEvents(this, SmashManager.getPlugin());
    }

    public void activateTask(final Player p)
    {
        final int interval;
        interval = 1;

        putTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(),
                new Runnable()
                {
                    int times = 0;
                    int amountHealed = 0;
                    boolean doneHealing = false;
                    public void run()
                    {
                        if (!doneHealing)
                        {
                            times++;
                            int healthisTime = (int)Math.round(Math.pow(times, 0.28));
                            if (healAll)
                            {
                                SmashManager.addDamage(p, -healthisTime, false);
                                if (SmashManager.getDamage(p) == 0)
                                {
                                    doneHealing = true;
                                }
                            }
                            else
                            {
                                if (healAmount > amountHealed + healthisTime && SmashManager.getDamage(p) - healthisTime > 0)
                                {
                                    SmashManager.addDamage(p, -healthisTime, false);
                                    amountHealed += healthisTime;
                                }
                                else
                                {
                                    SmashManager.addDamage(p, -(healAmount - amountHealed), false);
                                    doneHealing = true;
                                }
                            }
                        }
                    }
                }, 0, interval));
    }
}
