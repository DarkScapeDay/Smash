package me.happyman.SmashItems;

import me.happyman.ItemTypes.SmashItem;
import me.happyman.SmashItemDrops.SmashOrb;
import me.happyman.commands.SmashManager;
import me.happyman.worlds.SmashWorldManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class CompassItem extends SmashItem
{
    public CompassItem()
    {
        super(Material.COMPASS, ChatColor.GRAY + "" + ChatColor.BOLD + "Tracking compass", ChatColor.GRAY + "Tracks the player with the highest score");
    }

    public void performRightClickAction(Player p)
    {
        ItemMeta meta = getItem().getItemMeta();
        if (SmashOrb.hasSmashOrb(p.getWorld()))
        {
            meta.setDisplayName(ChatColor.GOLD + "Tracking Final Smash orb!");
            p.setCompassTarget(SmashOrb.getSmashOrb(p.getWorld()).getLocation());
        }
        else
        {
            Player closestPlayer = SmashManager.getPlayerWithHighScore(p);
            if (closestPlayer != null)
            {
                meta.setDisplayName(ChatColor.GOLD + "Tracking " + closestPlayer.getName());
                p.setCompassTarget(closestPlayer.getLocation());
            }
            else
            {
                meta.setDisplayName(ChatColor.GOLD + "No targets found");
            }
        }
        p.getItemInHand().setItemMeta(meta);
    }

    @Override
    public boolean canUseItem(Player p)
    {
        return SmashWorldManager.isSmashWorld(p.getWorld());
    }

    @Override
    public boolean isThis(ItemStack item)
    {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasLore() && item.getItemMeta().getLore().equals(getItem().getItemMeta().getLore());
    }

}
