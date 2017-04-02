package me.happyman.ItemTypes;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;

public abstract class SwordItemWithUsages extends SmashItemWithUsages
{
    public SwordItemWithUsages(Material mat, int maxUsages)
    {
        super(mat, ChatColor.GOLD + "" + ChatColor.BOLD + "Sword", maxUsages, false);
    }

    public SwordItemWithUsages(Material mat)
    {
        super(mat, ChatColor.GOLD + "" + ChatColor.BOLD + "Sword", 0, false);
    }

    public SwordItemWithUsages(Material mat, String name, Enchantment[] enchantments, int[] ints)
    {
        super(mat, name, enchantments, ints, 0, false);
    }
}
