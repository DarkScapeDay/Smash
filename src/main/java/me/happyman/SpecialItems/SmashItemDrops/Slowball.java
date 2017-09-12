package me.happyman.SpecialItems.SmashItemDrops;

import me.happyman.SpecialItems.SpecialItemTypes.SpeedChanger;
import me.happyman.SpecialItems.UsefulItemStack;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class Slowball extends SpeedChanger
{
    public Slowball()
    {
        super(new UsefulItemStack(Material.SNOW_BALL, ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Slowball"), 0.707F, 15);
    }

    @Override
    public void setExpToRemaining(Player p) {}
}
