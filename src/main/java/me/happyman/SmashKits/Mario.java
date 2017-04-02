package me.happyman.SmashKits;

import me.happyman.ItemTypes.RocketItem;
import me.happyman.SmashItems.NormalSword;
import me.happyman.SmashKitMgt.LeatherArmorSet;
import me.happyman.SmashKitMgt.SmashKit;
import me.happyman.SmashKitMgt.SmashKitProperties;
import me.happyman.SpecialKitItems.MarioFinalSmash;
import org.bukkit.ChatColor;
import org.bukkit.Material;

public class Mario extends SmashKit
{
    public Mario()
    {
        super(
            new SmashKitProperties(ChatColor.RED + "" + ChatColor.BOLD + "Marly", Material.IRON_SWORD, ChatColor.BLUE + "" + ChatColor.ITALIC + "Wahoo!",
                new MarioFinalSmash()),
            new LeatherArmorSet(ChatColor.GOLD + "" + ChatColor.BOLD + "Marly's Outfit", new int[]{255, 0, 0}, new int[]{0, 0, 255}, new int[]{255, 0, 0}, new int[]{255, 0, 0}),
            new NormalSword(),
            new RocketItem(ChatColor.RED + "   " + ChatColor.BOLD + "Double-Jump", 2, 1.1F) {}
        );
        addItem(MarioFinalSmash.getFireball());
    }
}
