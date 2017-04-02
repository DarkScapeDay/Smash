package me.happyman.ItemTypes;

import me.happyman.Listeners.SmashItemManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.util.HashMap;

public abstract class SmashItemWithTask extends SmashItemWithExp
{
    private HashMap<Player, Integer> task;

    public SmashItemWithTask(Material mat, String name)
    {
        this(mat, name, false);
    }

    public SmashItemWithTask(Material mat, String name, boolean isProjectile)
    {
        super(mat, name, isProjectile);
        task = new HashMap<Player, Integer>();
    }

    public SmashItemWithTask(Material mat, String name, String lore)
    {
        this(mat, name, lore, false);
    }

    public SmashItemWithTask(Material mat, String name, String lore, boolean canBeLeftClicked)
    {
        super(mat, name, lore, canBeLeftClicked);
        task = new HashMap<Player, Integer>();
    }

    public SmashItemWithTask(Material mat, String name, Enchantment[] enchants,  int[] levels)
    {
        super(mat, name, enchants, levels);
        task = new HashMap<Player, Integer>();
    }

    public void setExpToRemaining(Player p) {}

    public boolean hasTaskActive(Player p)
    {
        return task.containsKey(p);
    }

    public void putTask(Player p, int taskNumber)
    {
        task.put(p, taskNumber);
    }

    public abstract void activateTask(Player p);

    public void cancelTask(Player p)
    {
        if (task.containsKey(p))
        {
            Bukkit.getScheduler().cancelTask(task.get(p));
            task.remove(p);
        }
    }

    public void performRightClickAction(Player p)
    {
        if (!isProjectile())
        {
            SmashItemManager.removeOneItemFromHand(p);
            activateTask(p);
        }
    }
}
