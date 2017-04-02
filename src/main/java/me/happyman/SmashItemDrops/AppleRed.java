package me.happyman.SmashItemDrops;

import org.bukkit.ChatColor;
import org.bukkit.Material;

public class AppleRed extends HealingItem
{
    public AppleRed()
    {
        super(Material.APPLE, ChatColor.GOLD + "" + ChatColor.BOLD + "Healing Apple", 50);
    }
}
