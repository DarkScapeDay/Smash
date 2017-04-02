package me.happyman.ItemTypes;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

public abstract class SmashItemWithExp extends SmashItem
{
    public SmashItemWithExp(Material mat, String name)
    {
        this(mat, name, false);
    }

    public SmashItemWithExp(Material mat, String name, boolean isProjectile)
    {
        super(mat, name, isProjectile);
    }

    public SmashItemWithExp(Material mat, String name, String lore)
    {
        this(mat, name, lore, false);
    }

    public SmashItemWithExp(Material mat, String name, String lore, boolean canBeLeftClicked)
    {
        super(mat, name, lore, false, canBeLeftClicked);
    }

    public SmashItemWithExp(Material mat, String name, Enchantment[] enchants, int[] levels)
    {
        this(mat, name, enchants, levels, false);
    }

    public SmashItemWithExp(Material mat, String name, Enchantment[] enchants, int[] levels, boolean isProjectile)
    {
        super(mat, name, enchants, levels, isProjectile);
    }

    public abstract void setExpToRemaining(Player p);

    @Override
    public void performDeselectAction(Player p)
    {
        p.setExp(0);
    }
}
