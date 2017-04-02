package me.happyman.ItemTypes;

import me.happyman.SmashKitMgt.SmashKitManager;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashItemManager;
import me.happyman.utils.SmashWorldManager;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;

public abstract class SmashItem
{
    private final ItemStack item;
    private final boolean isProjectile;
    private boolean canBeLeftClicked;

    public SmashItem(Material mat, String name)
    {
        this(mat, name, false);
    }

    public SmashItem(Material mat, String name, boolean isProjectile)
    {
        item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        this.isProjectile = isProjectile;
    }

    public SmashItem(Material mat, String name, boolean isProjectile, boolean canBeLeftClicked)
    {
        item = new ItemStack(mat, 1);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        this.isProjectile = isProjectile;
        this.canBeLeftClicked = canBeLeftClicked;
    }

    public SmashItem(Material mat, String name, String lore)
    {
        this(mat, name, lore, false);
    }

    public SmashItem(Material mat, String name, String lore, boolean isProjectile)
    {
        this(mat, name, lore, isProjectile, false);

    }

    public SmashItem(Material mat, String name, String lore, boolean isProjectile, boolean canBeLeftClicked)
    {
        this(mat, name, isProjectile, canBeLeftClicked);
        ItemMeta trackerMeta = item.getItemMeta();
        trackerMeta.setLore(new ArrayList<String>(Arrays.asList(lore)));
        item.setItemMeta(trackerMeta);
    }

    public SmashItem(Material mat, String name, Enchantment[] enchants, int[] levels)
    {
        this(mat, name, enchants, levels, false);
    }

    public SmashItem(Material mat, String name, Enchantment[] enchants, int[] levels, boolean isProjectile)
    {
        this(mat, name, isProjectile);
        if (enchants.length != levels.length)
        {
            SmashManager.getPlugin().sendErrorMessage("Error! Tried to add enchants without corresponding levels!");
        }
        else
        {
            ItemMeta meta = item.getItemMeta();
            for (int i = 0; i < enchants.length; i++)
            {
                meta.addEnchant(enchants[i], levels[i], true);
            }
            item.setItemMeta(meta);
        }
    }

    public boolean isProjectile()
    {
        return isProjectile;
    }

    public ItemStack getItem()
    {
        return item;
    }

    public boolean isBeingHeld(Player p)
    {
        return isThis(p.getItemInHand());
    }

    public boolean inventoryContains(Player p)
    {
        for (ItemStack i : p.getInventory().getContents())
        {
            if (isThis(i))
            {
                return true;
            }
        }
        return false;
    }

    public boolean isThis(ItemStack item)
    {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName() && item.getItemMeta().getDisplayName().equals(getItem().getItemMeta().getDisplayName());
    }

    public abstract void performRightClickAction(Player p);

    public boolean canBeLeftClicked()
    {
        return canBeLeftClicked;
    }

    public void performDeselectAction(Player p){};

    public boolean give(Player p)
    {
        if (SmashItemManager.giveItem(p, getItem()))
        {
            if (this instanceof SmashItemWithExp && isBeingHeld(p))
            {
                ((SmashItemWithExp)this).setExpToRemaining(p);
            }
            return true;
        }
        return true;
    }

    public void remove(Player p)
    {
        p.getInventory().remove(getItem());
    }

    public boolean canUseItem(Player p)
    {
        return !SmashKitManager.canChangeKit(p) && !SmashWorldManager.isInSpectatorMode(p);
    }
}
