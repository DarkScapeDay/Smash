package me.happyman.SmashItemDrops;

import me.happyman.ItemTypes.SpeedChanger;
import org.bukkit.ChatColor;
import org.bukkit.Material;

public class LightFeather extends SpeedChanger
{
    public LightFeather()
    {
        super(Material.FEATHER, ChatColor.GRAY + "Light Feather", 1.5F, 10);
    }
}
