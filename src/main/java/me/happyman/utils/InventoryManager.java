package me.happyman.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import static me.happyman.Plugin.getPlugin;
import static me.happyman.Plugin.sendErrorMessage;

public class InventoryManager
{
    public static String getColorlessItemName(ItemStack item)
    {
        String result = getItemName(item);
        return result == null ? null : ChatColor.stripColor(result);
    }

    public static String getItemName(ItemStack item)
    {
        if (item != null)
        {
            if (item.hasItemMeta() && item.getItemMeta().hasDisplayName())
            {
                return item.getItemMeta().getDisplayName();
            }
            return getItemName(item.getType());
        }
        return null;
    }

    private static String getPluralName(String itemName, boolean multiple)
    {
        return itemName + (!multiple || itemName.length() > 0 && itemName.charAt(itemName.length() - 1) == 's' ? "" : "s");
    }

    public static String getPluralName(ItemStack item, boolean multiple)
    {
        return getPluralName(getColorlessItemName(item), multiple);
    }

    public static String getPluralName(ItemStack item, short howManyThereAre)
    {
        return getPluralName(item, howManyThereAre != 1);
    }

    public static String getPluralName(Material mat, boolean multiple)
    {
        return getPluralName(getItemName(mat), multiple);
    }

    public static String getPluralName(Material mat, short howManyThereAre)
    {
        return getPluralName(mat, howManyThereAre != 1);
    }

    public static String getItemName(Material mat)
    {
        return mat.name().toLowerCase();
    }

    public static short getHowManyCanBeGiven(Player p, Material mat, boolean stack)
    {
        short spaces = 0;
        short maxStackSize = (short)mat.getMaxStackSize();
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < 36; i++)
        {
            ItemStack item = contents[i];
            if (item == null || item.getType().equals(Material.AIR))
            {
                spaces += maxStackSize;
            }
            else if (stack && mat == item.getType() && !(item.hasItemMeta() && (item.getItemMeta().hasDisplayName() || item.getItemMeta().hasLore())))
            {
                spaces += maxStackSize - item.getAmount();
            }
        }
        return spaces;
    }

    public static short getHowManyCanBeGiven(Player p, ItemStack itemToGive, boolean stack)
    {
        short spaces = 0;
        short maxStackSize = (short)itemToGive.getMaxStackSize();
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < 36; i++)
        {
            ItemStack item = contents[i];
            if (item == null || item.getType() == Material.AIR)
            {
                spaces += maxStackSize;
            }
            else if (stack && itemsAreSame(itemToGive, item))
            {
                spaces += maxStackSize - item.getAmount();
            }
        }
        return spaces;
    }

    public static boolean canGive(Player p, Material mat, short amount, boolean stack)
    {
        return getHowManyCanBeGiven(p, mat, stack) >= amount;
    }

    public static boolean canGive(Player p, ItemStack itemToGive, boolean stack)
    {
        return canGive(p, itemToGive, (short)itemToGive.getAmount(), stack);
    }

    public static boolean canGive(Player p, ItemStack itemToGive, short amount, boolean stack)
    {
        return getHowManyCanBeGiven(p, itemToGive, stack) >= amount;
//        short spaces = 0;
//        short maxStackSize = (short)itemToGive.getMaxStackSize();
//        ItemStack[] contents = p.getInventory().getContents();
//        for (int i = 0; i < 36; i++)
//        {
//            ItemStack item = contents[i];
//            if (item == null || item.getType() == Material.AIR)
//            {
//                spaces += maxStackSize;
//            }
//            else if (stack && itemsAreSame(itemToGive, item))
//            {
//                spaces += maxStackSize - item.getAmount();
//            }
//
//            if (spaces >= amount)
//            {
//                return true;
//            }
//        }
//        return false;
    }

    public static void giveMaxItem(Player p, Material mat, boolean stack, boolean tellThePlayer)
    {
        short amountReceived = 0;
        final ItemStack[] inv = p.getInventory().getContents();
        if (stack)
        {
            for (int i = 0; i < 36; i++)
            {
                if (inv[i] != null && mat.equals(inv[i].getType()))
                {
                    amountReceived += inv[i].getMaxStackSize() - inv[i].getAmount();
                    inv[i].setAmount(inv[i].getMaxStackSize());
                }
            }
        }
        for (int i = 0; i < 36; i++)
        {
            if (inv[i] == null || inv[i].getType().equals(Material.AIR))
            {
                ItemStack fabricatedStack = new ItemStack(mat);
                amountReceived += fabricatedStack.getMaxStackSize();
                fabricatedStack.setAmount(fabricatedStack.getMaxStackSize());
                inv[i] = fabricatedStack;
            }
        }

        p.getInventory().setContents(inv);
        if (tellThePlayer && amountReceived > 0)
        {
            p.sendMessage(ChatColor.GREEN + "" + amountReceived + " " + getPluralName(mat, amountReceived) + " received.");
        }
    }

    public static boolean giveItem(final Player p, Material mat, final short originalAmountToGive, boolean stack, boolean tellThePlayer)
    {
        return giveItem(p, new ItemStack(mat), originalAmountToGive, stack, tellThePlayer);
//        if (canGive(p, mat, originalAmountToGive, stack))
//        {
//            if (originalAmountToGive < 0)
//            {
//                return false;
//            }
//            else if (originalAmountToGive == 0)
//            {
//                return true;
//            }
//            short amountToGive = originalAmountToGive;
//            final ItemStack[] inv = p.getInventory().getContents();
//            short maxStackSize = (short)mat.getMaxStackSize();
//            if (stack)
//            {
//                for (int i = 0; i < 36 && amountToGive > 0; i++)
//                {
//                    if (inv[i] != null && mat.equals(inv[i].getType()))
//                    {
//                        ItemStack fabricatedStack = new ItemStack(mat);
//                        short amountHere = (short)inv[i].getAmount();
//                        short amountICanGive = (short)(maxStackSize - amountHere);
//
//                        if (amountToGive < amountICanGive)
//                        {
//                            fabricatedStack.setAmount(amountHere + amountToGive);
//                            amountToGive = 0;
//                        }
//                        else
//                        {
//                            fabricatedStack.setAmount(maxStackSize);
//                            amountToGive -= amountICanGive;
//                        }
//                        inv[i] = fabricatedStack;
//                    }
//                }
//            }
//            for (int i = 0; i < 36 && amountToGive > 0; i++)
//            {
//                if (inv[i] == null || inv[i].getType().equals(Material.AIR))
//                {
//                    ItemStack fabricatedStack = new ItemStack(mat);
//
//                    if (amountToGive <= maxStackSize)
//                    {
//                        fabricatedStack.setAmount(amountToGive);
//                        amountToGive = 0;
//                    }
//                    else
//                    {
//                        fabricatedStack.setAmount(maxStackSize);
//                        amountToGive -= maxStackSize;
//                    }
//                    inv[i] = fabricatedStack;
//                }
//            }
//            short amountReceived = (short)(originalAmountToGive - amountToGive);
//            p.getInventory().setContents(inv);
//            String pluralName = getPluralName(mat, amountReceived);
//            if (tellThePlayer && originalAmountToGive > 0)
//            {
//                p.sendMessage(ChatColor.GREEN + "" + amountReceived + " " + pluralName + " received.");
//            }
//
//            if (amountToGive < 0)
//            {
//                sendErrorMessage("Error! Gave " + p.getName() + " too many " + pluralName + " (SpecialItem)!");
//            }
//            else if (amountToGive > 0)
//            {
//                sendErrorMessage("Error! Gave " + p.getName() + " too few " + pluralName + " (SpecialItem)!");
//            }
//            else
//            {
//                return true;
//            }
//            return false;
//        }
    }

    public static boolean giveItem(final Player p, ItemStack item, boolean stack, boolean tellThePlayer)
    {
        return giveItem(p, item, (short)item.getAmount(), stack, tellThePlayer);
    }

    public static boolean giveItem(final Player p, ItemStack item, final short originalAmountToGive, boolean stack, boolean tellThePlayer)
    {
        if (originalAmountToGive < 0)
        {
            return false;
        }
        else if (originalAmountToGive == 0)
        {
            return true;
        }
        else if (canGive(p, item, originalAmountToGive, stack))
        {
            short amountLeftToGive = originalAmountToGive;
            final ItemStack[] inv = p.getInventory().getContents();
            short maxStackSize = (short)item.getMaxStackSize();
            if (stack)
            {
                for (int i = 0; i < 36 && amountLeftToGive > 0; i++)
                {
                    if (itemsAreSame(inv[i], item))
                    {
                        ItemStack fabricatedStack = new ItemStack(item);
                        short amountHere = (short)inv[i].getAmount();
                        short amountICanGive = (short)(maxStackSize - amountHere);

                        if (amountLeftToGive < amountICanGive)
                        {
                            fabricatedStack.setAmount(amountHere + amountLeftToGive);
                            amountLeftToGive = 0;
                        }
                        else
                        {
                            fabricatedStack.setAmount(maxStackSize);
                            amountLeftToGive -= amountICanGive;
                        }
                        inv[i] = fabricatedStack;
                    }
                }
            }
            for (int i = 0; i < 36 && amountLeftToGive > 0; i++)
            {
                if (inv[i] == null || inv[i].getType().equals(Material.AIR))
                {
                    ItemStack fabricatedStack = new ItemStack(item);
                    if (amountLeftToGive <= maxStackSize)
                    {
                        fabricatedStack.setAmount(amountLeftToGive);
                        amountLeftToGive = 0;
                    }
                    else
                    {
                        fabricatedStack.setAmount(maxStackSize);
                        amountLeftToGive -= maxStackSize;
                    }
                    inv[i] = fabricatedStack;
                }
            }
            short amountReceived = (short)(originalAmountToGive - amountLeftToGive);
            String pluralName = getPluralName(item, amountReceived);
            p.getInventory().setContents(inv);
            if (tellThePlayer && originalAmountToGive > 0)
            {
                p.sendMessage(ChatColor.GREEN + "" + amountReceived + " " + pluralName + " received.");
            }

            if (amountLeftToGive < 0)
            {
                sendErrorMessage("Error! Gave " + p.getName() + " too many " + pluralName + "!");
            }
            else if (amountLeftToGive > 0)
            {
                sendErrorMessage("Error! Gave " + p.getName() + " too few " + pluralName + "!");
            }
            return amountLeftToGive <= 0;
        }
        return false;
    }

    public static boolean itemsAreSame(ItemStack item1, ItemStack item2)
    {
        return itemsAreSame(item1, item2, false);
    }

    private static boolean itemsAreSame(ItemStack item1, ItemStack item2, boolean allowDurabilityDifferences)
    {
        return itemsAreSame(item1, item2, allowDurabilityDifferences, false);
    }

    private static boolean itemsAreSame(ItemStack item1, ItemStack item2, boolean allowDurabilityDifferences, boolean allowNameDifferences)
    {
          return item1 == null && item2 == null ||
                item1 != null && item2 != null &&
               (allowDurabilityDifferences || item1.getDurability() == item2.getDurability()) &&
                item1.getType().equals(item2.getType()) &&
               (allowNameDifferences || !item1.hasItemMeta() && !item2.hasItemMeta() ||
                        item1.hasItemMeta() && item2.hasItemMeta() &&
                                (!item1.getItemMeta().hasDisplayName() && !item2.getItemMeta().hasDisplayName() ||
                                        item1.getItemMeta().hasDisplayName() && item2.getItemMeta().hasDisplayName() &&
                                                item1.getItemMeta().getDisplayName().equals(item2.getItemMeta().getDisplayName()))) &&
                (item1.getEnchantments() == null && item2.getEnchantments() == null ||
                        item1.getEnchantments() != null && item2.getEnchantments() != null && item1.getEnchantments().equals(item2.getEnchantments()));
    }

    public static boolean hasItem(Player p, ItemStack item)
    {
        return getItemTally(p, item) > 0;
    }

    public static boolean giveItem(Player p, ItemStack itemStack, boolean stack, boolean tellThePlayer, int preferredIndex)
    {
        ItemStack currentItem = p.getInventory().getItem(preferredIndex);
        if (currentItem == null || currentItem.getType() == Material.AIR)
        {
            p.getInventory().setItem(preferredIndex, itemStack);
            return true;
        }

        final int invSize = p.getInventory().getSize();
        for (int right = preferredIndex + 1, left = preferredIndex - 1; right < invSize || left >= 0; right++, left--)
        {
            if (right < invSize)
            {
                currentItem = p.getInventory().getItem(right);
                if (currentItem == null || currentItem.getType() == Material.AIR)
                {
                    p.getInventory().setItem(right, itemStack);
                    return true;
                }
            }
            if (left >= 0)
            {
                currentItem = p.getInventory().getItem(left);
                if (currentItem == null || currentItem.getType() == Material.AIR)
                {
                    p.getInventory().setItem(left, itemStack);
                    return true;
                }
            }
        }
        return giveItem(p, itemStack, stack, tellThePlayer);
    }

    private static class MegaItemStack extends ItemStack
    {
        private int numberOfItems;
        private int totalDurablity;

        MegaItemStack(ItemStack baseItem)
        {
            super(baseItem);
            this.numberOfItems = 0;
            this.totalDurablity = 0;
            addOneItem(baseItem);
        }

        int getNumberOfFullStacks()
        {
            return numberOfItems/64;
        }

        int getPartialStackSize()
        {
            return numberOfItems % 64;
        }

        void addOneItem(ItemStack item)
        {
            this.numberOfItems += item.getAmount();
            this.totalDurablity += item.getDurability();
        }

        void setToAvgDurability()
        {
            setDurability((short)((float)totalDurablity/numberOfItems));
        }
    }

    public static void consolidateItems(final Player p)
    {
        ArrayList<MegaItemStack> itemStacks = new ArrayList<MegaItemStack>();
        ItemStack[] originalContents = p.getInventory().getContents();

        for (ItemStack item : originalContents)
        {
            if (item != null && !item.getType().equals(Material.AIR))
            {
                boolean found = false;
                for (MegaItemStack result : itemStacks)
                {
                    if (InventoryManager.itemsAreSame(result, item, true))
                    {
                        result.addOneItem(item);
                        found = true;
                        break;
                    }
                }
                if (!found)
                {
                    itemStacks.add(new MegaItemStack(item));
                }
            }
        }

        ItemStack[] whatToSetTo = new ItemStack[originalContents.length];

        for (int megaIndex = 0, invIndex = 0; invIndex < originalContents.length && megaIndex < itemStacks.size(); megaIndex++)
        {
            MegaItemStack curItemStack = itemStacks.get(megaIndex);
            curItemStack.setToAvgDurability();

            int fullStacks = curItemStack.getNumberOfFullStacks();
            curItemStack.setAmount(64);
            for (int fullStacksGiven = 0; fullStacksGiven < fullStacks && invIndex < originalContents.length; fullStacksGiven++, invIndex++)
            {
                whatToSetTo[invIndex] = new ItemStack(curItemStack);
            }

            curItemStack.setAmount(curItemStack.getPartialStackSize());
            if (invIndex < originalContents.length && curItemStack.getAmount() > 0)
            {
                whatToSetTo[invIndex++] = new ItemStack(curItemStack);
            }
        }

        p.getInventory().setContents(whatToSetTo);
        Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
        {
            @Override
            public String call()
            {
                p.updateInventory();
                return "";
            }
        });
        p.sendMessage(ChatColor.GREEN + "Items consolidated");

    }

    public static void removeOneItemFromHand(Player p)
    {
        removeItemsFromHand(p, 1);
    }

    public static void removeItemsFromHand(final Player p, int amount)
    {
        final ItemStack it = new ItemStack(p.getItemInHand());
        int currentAmount = it.getAmount();
        final int newAmount = currentAmount - amount;

        Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
        {
            public String call()
            {
                if (newAmount > 0)
                {
                    it.setAmount(newAmount);
                    p.setItemInHand(it);
                }
                else
                {
                    p.setItemInHand(new ItemStack(Material.AIR));
        //            p.setItemInHand(new ItemStack(Material.AIR));
                    //p.getItemInHand().setType(Material.AIR);
                }
                return "";
            }
        });
    }

    private static short getItemTally(Player p, ItemStack item, short quota)
    {
        short items = 0;
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < 36; i++)
        {
            ItemStack particularItem = contents[i];
            if (itemsAreSame(item, particularItem))
            {
                items += particularItem.getAmount();
                if (items >= quota && quota != -1)
                {
                    return quota;
                }
            }
        }
        return items;
    }

    public static short getItemTally(Player p, ItemStack item)
    {
        return getItemTally(p, item, (short)-1);
    }

    private static short getItemTally(Player p, Material mat, short quota)
    {
        short items = 0;
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < 36; i++)
        {
            ItemStack particularItem = contents[i];
            if (particularItem != null && mat.equals(particularItem.getType()))
            {
                items += particularItem.getAmount();
                if (items >= quota && quota != -1)
                {
                    return quota;
                }
            }
        }
        return items;
    }

    public static short getItemTally(Player p, Material mat)
    {
        return getItemTally(p, mat, (short)-1);
    }

    public static void removeAllFromPlayer(final Player p, Material mat, boolean tellThePlayer)
    {
        ItemStack[] inv = p.getInventory().getContents();
        for (int i = 0; i < 36; i++)
        {
            if (inv[i] != null && inv[i].getType().equals(mat))
            {
                inv[i].setType(Material.AIR);
            }
        }
        p.getInventory().setContents(inv);
        if (tellThePlayer)
        {
            p.sendMessage(ChatColor.GREEN + "All " + getPluralName(mat, true) + " removed.");
        }

        Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
        {
            public String call()
            {
                p.updateInventory();
                return "";
            }
        });
    }

    public static void removeAllFromPlayer(final Player p, ItemStack itemToRemove, boolean tellThePlayer)
    {
        ItemStack[] inv = p.getInventory().getContents();
        for (int i = 0; i < 36; i++)
        {
            if (itemsAreSame(itemToRemove, inv[i], true))
            {
                inv[i].setType(Material.AIR);
            }
        }
        p.getInventory().setContents(inv);
        if (tellThePlayer)
        {
            p.sendMessage(ChatColor.GREEN + "All " + getPluralName(itemToRemove, (short)2) + " removed.");
        }

        Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
        {
            public String call()
            {
                p.updateInventory();
                return "";
            }
        });
    }

    public static void removeItem(final Player p, ItemStack itemToRemoved, short amount)
    {
        removeItem(p, itemToRemoved, amount, true);
    }

    public static void removeItem(final Player p, ItemStack itemToRemove, short amount, boolean tellThePlayer)
    {
        short amountNeededToTake = amount;
        ItemStack[] inv = p.getInventory().getContents();
        for (int i = 0; i < 36; i++)
        {
            ItemStack item = inv[i];
            if (itemsAreSame(itemToRemove, item))
            {
                short amountHere = (short)item.getAmount();
                if (amountNeededToTake >= amountHere)
                {
                    item.setType(Material.AIR);
                    amountNeededToTake -= amountHere;
                }
                else
                {
                    item.setAmount(amountHere - amountNeededToTake);
                    amountNeededToTake = 0;
                }

                if (amountNeededToTake <= 0)
                {
                    break;
                }
            }
        }
        p.getInventory().setContents(inv);
        String pluralName = getPluralName(itemToRemove, (short)(amount - amountNeededToTake));
        if (tellThePlayer)
        {
            p.sendMessage(ChatColor.GREEN + "" + (amount - amountNeededToTake) + " " + pluralName + " removed.");
        }

        Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
        {
            public String call()
            {
                p.updateInventory();
                return "";
            }
        });

        if (amountNeededToTake > 0)
        {
            sendErrorMessage("Error! Didn't remove enough " + pluralName + " from " + p.getName());
        }
        else if (amountNeededToTake < 0)
        {
            sendErrorMessage("Error! Took too many " + pluralName + " from " + p.getName());
        }
    }

    public static void removeItem(final Player p, Material itemToRemove, short amount, boolean tellThePlayer)
    {
        short amountNeededToTake = amount;
        ItemStack[] inv = p.getInventory().getContents();
        for (int i = 0; i < 36; i++)
        {
            ItemStack item = inv[i];
            if (item != null && itemToRemove.equals(item.getType()))
            {
                short amountHere = (short)item.getAmount();
                if (amountNeededToTake >= amountHere)
                {
                    item.setType(Material.AIR);
                    amountNeededToTake -= amountHere;
                }
                else
                {
                    item.setAmount(amountHere - amountNeededToTake);
                    amountNeededToTake = 0;
                }

                if (amountNeededToTake <= 0)
                {
                    break;
                }
            }
        }
        p.getInventory().setContents(inv);
        String pluralName = getPluralName(itemToRemove, (short)(amount - amountNeededToTake));
        if (tellThePlayer)
        {
            p.sendMessage(ChatColor.GREEN + "" + (amount - amountNeededToTake) + " " + pluralName + " removed.");
        }

        Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
        {
            public String call()
            {
                p.updateInventory();
                return "";
            }
        });

        if (amountNeededToTake > 0)
        {
            sendErrorMessage("Error! Didn't remove enough " + pluralName + " from " + p.getName());
        }
        else if (amountNeededToTake < 0)
        {
            sendErrorMessage("Error! Took too many " + pluralName + " from " + p.getName());
        }
    }
}
