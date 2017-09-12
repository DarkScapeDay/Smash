package me.happyman.SpecialItems.SpecialItemTypes;

import me.happyman.SpecialItems.UsefulItemStack;
import org.bukkit.ChatColor;
import org.bukkit.Material;

public class SpecialSword extends SpecialItemWithUsages
{
    public static class NormalSpecialSword extends SpecialSword
    {
        public NormalSpecialSword(Material swordType)
        {
            super(swordType);
        }

        public NormalSpecialSword()
        {
            super(Material.IRON_SWORD);
        }
    }

    public SpecialSword(Material mat, int maxUsages)
    {
        this(new UsefulItemStack(mat, ChatColor.GOLD + "" + ChatColor.BOLD + "Sword"), maxUsages);
    }

    public SpecialSword(UsefulItemStack item)
    {
        this(item, 0);
    }

    public SpecialSword(UsefulItemStack item, int maxUsages)
    {
        super(item, maxUsages, false);
    }

    public SpecialSword(Material mat)
    {
        this(mat, 0);
    }
}
