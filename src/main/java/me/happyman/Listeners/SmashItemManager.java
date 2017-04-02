package me.happyman.Listeners;

import me.happyman.ItemTypes.*;
import me.happyman.SmashItems.CompassItem;
import me.happyman.SmashItems.ShieldItem;
import me.happyman.SmashItemDrops.Hammer;
import me.happyman.SmashItemDrops.ItemDropManager;
import me.happyman.SmashKitMgt.SmashKitManager;
import me.happyman.commands.SmashManager;
import me.happyman.source;
import me.happyman.utils.SmashEntityTracker;
import me.happyman.utils.SmashWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.Callable;

public class SmashItemManager implements Listener
{
    public static final Material MATERIAL_FOR_ITEM_DROPS = Material.STEP;
    private static source plugin;

    private static HashMap<Player, ItemStack[]> originalItems;
    private static HashMap<Player, ItemStack[]> originalArmor;
    private static HashMap<Player, SmashItemWithTask> actualOriginalItem;

    public SmashItemManager(SmashManager smashManager, source plugIn)
    {
        plugin = plugIn;
        new SmashKitManager(smashManager, plugin);
        Bukkit.getPluginManager().registerEvents(this, plugin);
        new SmashAttackListener(smashManager, plugin);
        new SmashMishapPreventor(smashManager, plugin);
        new SmashEntityTracker();
        new ItemDropManager();

        originalItems = new HashMap<Player, ItemStack[]>();
        originalArmor = new HashMap<Player, ItemStack[]>();
        actualOriginalItem = new HashMap<Player, SmashItemWithTask>();
    }

    //True if successfully found a place to place the item
    public static boolean giveItem(final Player p, ItemStack item)
    {
        for (int i = 0; i < p.getInventory().getContents().length; i++)
        {
            ItemStack inventoryItem = p.getInventory().getItem(i);
            if (inventoryItem == null || inventoryItem.getType().equals(Material.AIR))
            {
                p.getInventory().setItem(i, item);
                p.updateInventory();
                Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
                    public String call()
                    {
                        p.updateInventory();
                        return "";
                    }
                });
                return true;
            }
        }
        return false;
    }

    public static void removeOneItemFromHand(Player p)
    {
        removeItemsFromHand(p, 1);
    }

    public static void removeItemsFromHand(Player p, int amount)
    {
        if (p.getItemInHand().getAmount() > amount)
        {
            p.getItemInHand().setAmount(p.getItemInHand().getAmount() - amount);
        }
        else
        {
            p.setItemInHand(new ItemStack(Material.AIR));
            //p.getItemInHand().setType(Material.AIR);
        }
    }



    public static void cancelItemTasks(Player p)
    {
        SmashKitManager.getSelectedKit(p).getProperties().getFinalSmash().cancelTask(p);
        cancelSpecialWielder(p);
        SmashEntityTracker.resetSpeedAlteredPlayer(p);
        SmashMishapPreventor.cancelRepairs(p);
        for (ItemStack item : p.getInventory().getContents())
        {
            if (SmashKitManager.kitHasItem(p, item))
            {
                SmashItem kitItem = SmashKitManager.getKitItem(p, item);
                if (!kitItem.isBeingHeld(p))
                {
                    if (kitItem instanceof SmashItemWithTask)
                    {
                        ((SmashItemWithTask) kitItem).cancelTask(p);
                    }
                }
            }
            else if (ItemDropManager.isSmashDropItem(item) && ItemDropManager.getSmashDropItem(item) instanceof SmashItemWithTask)
            {
                SmashItemWithTask itemDrop = (SmashItemWithTask)ItemDropManager.getSmashDropItem(item);
                if (!itemDrop.isBeingHeld(p))
                {
                    itemDrop.cancelTask(p);
                }
            }
        }
    }

    public static boolean isSpecialWielder(Player p)
    {
        return originalItems.containsKey(p);
    }

    private static SmashItemWithTask getActualItem(Player p)
    {
        if (actualOriginalItem.containsKey(p))
        {
            return actualOriginalItem.get(p);
        }
        SmashManager.getPlugin().sendErrorMessage("Error! Couldn't get the Hammer SmashItem for player " + p.getName());
        return null;
    }

    private static void cancelSpecialWielder(Player p)
    {
        if (isSpecialWielder(p))
        {
            getActualItem(p).cancelTask(p);
        }
    }

    public static void resetSpecialWielder(Player p)
    {
        resetSpecialWielder(p, true);
    }

    public static void resetSpecialWielder(Player p, boolean resetItems)
    {
        if (resetItems)
        {
            p.getInventory().setContents(originalItems.get(p));
            p.getEquipment().setArmorContents(originalArmor.get(p));

            if (p.getItemInHand() == null || !SmashKitManager.kitHasItem(p, p.getItemInHand()) || !(SmashKitManager.getKitItem(p, p.getItemInHand()) instanceof SmashItemWithExp))
            {
                p.setExp(0);
            }
            else
            {
                ((SmashItemWithExp)SmashKitManager.getKitItem(p, p.getItemInHand())).setExpToRemaining(p);
            }
        }
        if (originalItems.containsKey(p))
        {
            originalItems.remove(p);
        }
        if (originalArmor.containsKey(p))
        {
            originalArmor.remove(p);
        }
        if (actualOriginalItem.containsKey(p))
        {
            actualOriginalItem.remove(p);
        }
        setItemSlotForKitChange(p);
    }

    public static void setItemSlotForKitChange(Player p)
    {
        if (SmashKitManager.kitHasItem(p, p.getItemInHand()))
        {
            SmashItem item = SmashKitManager.getKitItem(p, p.getItemInHand());
            if (!(item instanceof RocketItem || item instanceof CompassItem || item instanceof ShieldItem))
            {
                p.getInventory().setHeldItemSlot(0);
            }
        }
    }

    public static SmashItemWithTask getWieldedItem(Player p)
    {
        return actualOriginalItem.get(p);
    }

    public static void setSpecialWielder(Player p, SmashItemWithTask theSmashItem)
    {
        if (isSpecialWielder(p))
        {
            resetSpecialWielder(p, true);
        }

        actualOriginalItem.put(p, theSmashItem);
        originalItems.put(p, p.getInventory().getContents());
        originalArmor.put(p, p.getEquipment().getArmorContents());
    }

    /*public void performDeselects(Player p)
    {
        for (ItemStack item : p.getInventory().getContents())
        {
            if (SmashKitManager.isKitItem(p, item))
            {
                SmashItem kitItem = SmashKitManager.getSelectedKit(p).getSmashDropItem(item);
                if (kitItem instanceof SmashItemWithExp)
                {
                    ((SmashItemWithExp)kitItem).performDeselectAction(p);
                }
            }
        }
    }*/

    @EventHandler
    public void xpBarUpdate(final PlayerItemHeldEvent e)
    {
        final Player p = e.getPlayer();
        //ItemStack selectedItem = p.getInventory().getSmashDropItem(e.getNewSlot());

        Bukkit.getScheduler().callSyncMethod(plugin, new Callable() {
            public String call()
            {
                ItemStack selectedItem = p.getItemInHand();
                ItemStack previousItem = p.getInventory().getItem(e.getPreviousSlot());
                if (!Hammer.isWieldingHammer(p))
                {
                    if (selectedItem == null)
                    {
                        deselectInterestingItem(p, previousItem);
                    }
                    else if (SmashKitManager.kitHasItem(p, selectedItem) && SmashKitManager.getKitItem(p, selectedItem) instanceof SmashItemWithExp)
                    {
                        ((SmashItemWithExp)SmashKitManager.getKitItem(p, selectedItem)).setExpToRemaining(p);
                    }
                    else if (ItemDropManager.isSmashDropItem(selectedItem) && ItemDropManager.getSmashDropItem(selectedItem) instanceof SmashItemWithExp)
                    {
                        ((SmashItemWithExp)ItemDropManager.getSmashDropItem(selectedItem)).setExpToRemaining(p);
                    }
                    else
                    {
                        deselectInterestingItem(p, previousItem);
                    }
                }
                return "";
            }
        });
    }

    @EventHandler
    public void performItemAction(final PlayerInteractEvent e)
    {
        if (e.getItem() != null)
        {
            ItemStack itemClicked = e.getItem();
            Player p = e.getPlayer();
            if (SmashWorldManager.isSmashWorld(p.getWorld())
                    && ((e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
            || e.getAction().equals(Action.LEFT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_BLOCK)))
            {
                SmashItem item = null;
                if (SmashKitManager.getCurrentKit(p).hasItem(itemClicked) ||
                        !SmashKitManager.getCurrentKit(p).equals(SmashKitManager.getSelectedKit(p)) && SmashKitManager.getSelectedKit(p).hasItem(itemClicked))
                {
                    item = SmashKitManager.getKitItem(p, itemClicked);
                }
                else if (ItemDropManager.isSmashDropItem(itemClicked))
                {
                    item = ItemDropManager.getSmashDropItem(itemClicked);
                }

                if (item != null)
                {
                    if (!item.isProjectile())
                    {
                        e.setCancelled(true);
                    }
                    if (((e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.RIGHT_CLICK_BLOCK))
                        || (e.getAction().equals(Action.LEFT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_BLOCK)) && item.canBeLeftClicked())
                            && item.canUseItem(p))
                    {
                        item.performRightClickAction(p);
                    }
                }
            }
        }
    }

    private void deselectInterestingItem(Player p, ItemStack item)
    {
        if (item != null)
        {
            if (SmashKitManager.kitHasItem(p, item))
            {
                SmashKitManager.getKitItem(p, item).performDeselectAction(p);
            }
        }
        p.setExp(0);
    }
}
