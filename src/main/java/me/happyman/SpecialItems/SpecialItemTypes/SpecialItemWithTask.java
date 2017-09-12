package me.happyman.SpecialItems.SpecialItemTypes;

import me.happyman.SpecialItems.UsefulItemStack;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;

public abstract class SpecialItemWithTask extends SpecialItemWithExp
{
    private final HashMap<Player, ArrayList<Integer>> instanceTasks = new HashMap<Player, ArrayList<Integer>>();;

    public SpecialItemWithTask(UsefulItemStack item)
    {
        super(item);
    }

    public boolean hasInstanceTaskActive(Player p)
    {
        return instanceTasks.containsKey(p);
    }

    @Override
    public void performResetAction(Player p)
    {
        super.performResetAction(p);
        cancelTasks(p);
    }

    public final void cancelTasks(Player p)
    {
        if (hasInstanceTaskActive(p))
        {
            for (Integer i : instanceTasks.get(p))
            {
                Bukkit.getScheduler().cancelTask(i);
            }
            instanceTasks.remove(p);
        }
    }

    protected final void addTask(Player p, int task)
    {
        if (!instanceTasks.containsKey(p))
        {
            instanceTasks.put(p, new ArrayList<Integer>());
        }
        instanceTasks.get(p).add(task);
    }
    //public abstract void setExpToRemaining(Player p);
    //public abstract void performRightClickAction(Player p);
}
