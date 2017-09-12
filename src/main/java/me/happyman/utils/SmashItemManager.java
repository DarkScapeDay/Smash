package me.happyman.utils;


import me.happyman.SpecialItems.MetaItems;
import me.happyman.SpecialItems.SmashGeneralKitItems.RocketItem;
import me.happyman.SpecialItems.SmashItemDrops.ItemDropManager;
import me.happyman.SpecialItems.SmashKitMgt.SmashKitManager;
import me.happyman.SpecialItems.SpecialItem;
import me.happyman.worlds.MainListener;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;


public class SmashItemManager
{
    public static final Material MATERIAL_FOR_ITEM_DROPS = Material.STEP;

    public SmashItemManager()
    {
//        Bukkit.getPluginManager().registerEvents(this, getPlugin());
        new MainListener();
        new SmashMishapPreventor();
        new SmashEntityTracker();
        ItemDropManager.initialize();
        new SmashKitManager();
    }

    //
//    public static boolean isSpecialWielder(Player p)
//    {
//        return originalItems.containsKey(p);
//    }

//    public static void resetToCachedItems(Player p, boolean resetItems)
//    {
//        if (resetItems)
//        {
//            p.getInventory().setContents(originalItems.get(p));
//            p.getEquipment().setArmorContents(originalArmor.get(p));
//
//            if (p.getItemInHand() == null || !SmashKitManager.kitHasItem(p, p.getItemInHand()) || !(SmashKitManager.getKitItem(p, p.getItemInHand()) instanceof SpecialItemWithExp))
//            {
//                p.setExp(0);
//            }
//            else
//            {
//                ((SpecialItemWithExp)SmashKitManager.getKitItem(p, p.getItemInHand())).setExpToRemaining(p);
//            }
//        }
//        if (originalItems.containsKey(p))
//        {
//            originalItems.remove(p);
//        }
//        if (originalArmor.containsKey(p))
//        {
//            originalArmor.remove(p);
//        }
//        setItemSlotForKitChange(p);
//    }

    public static void setItemSlotForKitChange(Player p)
    {
        SpecialItem item = MetaItems.getHeldSpecialItem(p);
        if (item != null && !(item instanceof RocketItem))
        {
            p.getInventory().setHeldItemSlot(0);
        }
    }

//    public void performDeselects(Player p)
//    {
//ItemStack[] contents = p.getInventory().getContents();
//        for (int i = 0; i < 36; i++)
//    {
//        ItemStack item = contents[i];
//            if (SmashKitManager.isKitItem(p, item))
//            {
//                SpecialItem kitItem = SmashKitManager.getKit(p).getSmashDropItem(item);
//                if (kitItem instanceof SpecialItemWithExp)
//                {
//                    ((SpecialItemWithExp)kitItem).performDeselectAction(p);
//                }
//            }
//        }
//    }
//
//    @EventHandler
//    public void onSneak(final PlayerToggleSneakEvent event)
//    {
//        if (isSmashWorld(event.getAttacker().getWorld()))
//        {
//            SpecialItem item = getSmashItem(event.getAttacker(), event.getAttacker().getItemInHand());
//            if (item != null)
//            {
//                item.performSneakAction(event.getAttacker(), event.isSneaking());
//            }
//        }
//    }

}
