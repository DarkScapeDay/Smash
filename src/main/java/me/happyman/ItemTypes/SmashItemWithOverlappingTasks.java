package me.happyman.ItemTypes;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class SmashItemWithOverlappingTasks extends SmashItemWithTask
{
    private static HashMap<Player, List<Integer>> tasks;

    public SmashItemWithOverlappingTasks(Material mat, String name)
    {
        super(mat, name);
        tasks = new  HashMap<Player, List<Integer>>();
    }

    public SmashItemWithOverlappingTasks(Material mat, String name, boolean isProjectile)
    {
        super(mat, name, isProjectile);
        tasks = new  HashMap<Player, List<Integer>>();
    }

    public SmashItemWithOverlappingTasks(Material mat, String name, String lore)
    {
        super(mat, name, lore);
        tasks = new  HashMap<Player, List<Integer>>();
    }

    public SmashItemWithOverlappingTasks(Material mat, String name, Enchantment[] enchants, int[] levels)
    {
        super(mat, name, enchants, levels);
        tasks = new  HashMap<Player, List<Integer>>();
    }

    @Override
    public boolean hasTaskActive(Player p)
    {
        return tasks.containsKey(p);
    }

    @Override
    public void cancelTask(Player p)
    {
        if (hasTaskActive(p))
        {
            for (Integer i : tasks.get(p))
            {
                Bukkit.getScheduler().cancelTask(i);
            }
            tasks.remove(p);
        }
    }

    @Override
    public void putTask(Player p, int task)
    {
        if (!tasks.containsKey(p))
        {
            tasks.put(p, new ArrayList<Integer>());
        }
        tasks.get(p).add(task);
    }
}
