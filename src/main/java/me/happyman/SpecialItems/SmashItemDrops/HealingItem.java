package me.happyman.SpecialItems.SmashItemDrops;

import me.happyman.SpecialItems.SpecialItemTypes.SpecialItemWithTask;
import me.happyman.SpecialItems.UsefulItemStack;
import me.happyman.utils.SmashAttackManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import static me.happyman.Plugin.getPlugin;


public class HealingItem extends SpecialItemWithTask implements Listener
{
    public static class AppleGolden extends HealingItem
    {
        public AppleGolden()
        {
            super(new UsefulItemStack(Material.GOLDEN_APPLE, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Healing Apple"));
        }
    }

    public static class AppleRed extends HealingItem
    {
        public AppleRed()
        {
            super(new UsefulItemStack(Material.APPLE, ChatColor.GOLD + "" + ChatColor.BOLD + "Healing Apple"), 50);
        }
    }

    private final int healAmount;
    private final boolean healAll;

    public HealingItem(UsefulItemStack item)
    {
        super(item);
        this.healAll = true;
        this.healAmount = -1;
    }

    public HealingItem(UsefulItemStack item, int healAmount)
    {
        super(item);
        this.healAmount = healAmount;
        healAll = false;
        Bukkit.getPluginManager().registerEvents(this, getPlugin());
    }

    @Override
    public void setExpToRemaining(Player p)
    {
        p.setExp(0);
    }

    @Override
    public void performRightClickAction(final Player p, Block blockClicked)
    {
        final int interval;
        interval = 1;
        removeOne(p);

        addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(),
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
                            SmashAttackManager.addDamage(p, -healthisTime, false);
                            if (SmashAttackManager.getDamage(p) == 0)
                            {
                                doneHealing = true;
                            }
                        }
                        else
                        {
                            if (healAmount > amountHealed + healthisTime && SmashAttackManager.getDamage(p) - healthisTime > 0)
                            {
                                SmashAttackManager.addDamage(p, -healthisTime, false);
                                amountHealed += healthisTime;
                            }
                            else
                            {
                                SmashAttackManager.addDamage(p, -(healAmount - amountHealed), false);
                                doneHealing = true;
                            }
                        }
                    }
                }
            }, 0, interval));
    }
}
