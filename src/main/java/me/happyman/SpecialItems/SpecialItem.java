package me.happyman.SpecialItems;

import me.happyman.utils.InventoryManager;
import me.happyman.worlds.WorldType;
import org.apache.commons.lang.Validate;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.ItemStack;

import java.util.concurrent.Callable;

import static me.happyman.Plugin.getPlugin;

public class SpecialItem
{
    private final ItemStack item;
    private final String coloredName;
    private final String colorlessName;
    private final boolean canPerformDefaultAction;

    private boolean canPerformRightClickAction(Material mat)
    {
        switch (mat)
        {
            case BOW:
            case STONE_SWORD:
            case GOLD_SWORD:
            case DIAMOND_SWORD:
            case IRON_SWORD:
            case WOOD_SWORD:
            case SNOW_BALL:
            case EGG:
            case POTION:
            case SPLASH_POTION:
            case MONSTER_EGG:
            case SHEARS:
            case COMPASS:
            case DIAMOND_PICKAXE:
            case GOLD_PICKAXE:
            case IRON_PICKAXE:
            case STONE_PICKAXE:
            case WOOD_PICKAXE:
            case DIAMOND_AXE:
            case GOLD_AXE:
            case IRON_AXE:
            case STONE_AXE:
            case WOOD_AXE:
            case DIAMOND_HOE:
            case GOLD_HOE:
            case IRON_HOE:
            case STONE_HOE:
            case WOOD_HOE:
            case STONE_SPADE:
            case DIAMOND_SPADE:
            case GOLD_SPADE:
            case IRON_SPADE:
            case WOOD_SPADE:
            case FLINT_AND_STEEL:
            case FISHING_ROD:
            case DIAMOND_HELMET:
            case DIAMOND_CHESTPLATE:
            case DIAMOND_LEGGINGS:
            case DIAMOND_BOOTS:
            case IRON_HELMET:
            case IRON_CHESTPLATE:
            case IRON_LEGGINGS:
            case IRON_BOOTS:
            case CHAINMAIL_HELMET:
            case CHAINMAIL_CHESTPLATE:
            case CHAINMAIL_LEGGINGS:
            case CHAINMAIL_BOOTS:
            case LEATHER_HELMET:
            case LEATHER_CHESTPLATE:
            case LEATHER_LEGGINGS:
            case LEATHER_BOOTS:
            case BOOK:
            case BOOK_AND_QUILL:
                return true;
        }
        return false;
    }

    public SpecialItem(UsefulItemStack item)
    {
        Validate.notNull(item);
        this.canPerformDefaultAction = canPerformRightClickAction(item.getType());
        this.coloredName = item.getColoredName();
        this.colorlessName = ChatColor.stripColor(coloredName);
        this.item = item;
    }

    //    public void forgetAllProjectileSources()
//    {
//        ArrayList<Map.Entry> entries = new ArrayList<Map.Entry>(projectileSources.entrySet());
//        ArrayList<Entity> keysToRemove = new ArrayList<Entity>();
//        for (int i = 0; i < entries.size(); i++)
//        {
//            if (entries.get(i).getValue() == this)
//            {
//                keysToRemove.add((Entity)entries.get(i).getKey());
//            }
//        }
//        for (Entity entity : keysToRemove)
//        {
//            projectileSources.remove(entity);
//        }
//    }

    public String coloredName()
    {
        return coloredName;
    }

    public String colorlessName()
    {
        return colorlessName;
    }

    public ItemStack getItemStack()
    {
        return item;
    }

    public boolean isBeingHeld(Player p)
    {
        return isThis(p.getItemInHand());
    }

    public boolean inventoryContains(Player p)
    {
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < 36; i++)
        {
            ItemStack item = contents[i];
            if (isThis(item))
            {
                return true;
            }
        }
        return false;
    }

    public boolean isThis(ItemStack item)
    {
        return !(item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) && item.getItemMeta().getDisplayName().equals(coloredName);
    }

//    public boolean canGive(Player p)
//    {
//        return InventoryManager.canGive(p, getItemStack(), (short)getItemStack().getAmount(), true);
//    }
//
//    public final boolean give(Player p, boolean stack, boolean tellThePlayer)
//    {
//        return give(p, (short)getItemStack().getAmount(), stack, tellThePlayer);
//    }

    public boolean give(Player p)
    {
        if (InventoryManager.giveItem(p, getItemStack(),  (short)getItemStack().getAmount(), false, false))
        {
            if (isBeingHeld(p))
            {
                performSelectAction(p);
            }
            return true;
        }
        return false;
    }

    public final int removeAll(final Player p)
    {
        int amountFound = 0;
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++)
        {
            if (isThis(contents[i]))
            {
                amountFound += contents[i].getAmount();
                contents[i] = new ItemStack(Material.AIR);
            }
        }
        if (amountFound > 0)
        {
            p.getInventory().setContents(contents);
            p.updateInventory();
        }
        return amountFound;
    }

    public final boolean removeOne(final Player p)
    {
        ItemStack itemInHand = p.getItemInHand();
        if (isThis(itemInHand))
        {
            final ItemStack it = itemInHand;
            int currentAmount = it.getAmount();
            final int newAmount = currentAmount - 1;

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
                    }
                    return "";
                }
            });
            p.updateInventory();
            performResetAction(p);
            return true;
        }
        else
        {
            ItemStack[] contents = p.getInventory().getContents();
            for (int i = 0; i < contents.length; i++)
            {
                final ItemStack curItem = contents[i];
                if (isThis(curItem))
                {
                    final int newAmount = curItem.getAmount() - 1;

                    final int finalI = i;
                    Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
                    {
                        public String call()
                        {
                            if (newAmount > 0)
                            {
                                curItem.setAmount(newAmount);
                                p.getInventory().setItem(finalI, curItem);
                            }
                            else
                            {
                                p.getInventory().setItem(finalI, new ItemStack(Material.AIR));
                            }
                            return "";
                        }
                    });
                    p.updateInventory();
                    performResetAction(p);
                    return true;
                }
            }
        }
        return false;
    }

    public boolean canPerformRightClickAction()
    {
        return canPerformDefaultAction;
    }

    public boolean canBeUsed(Player p)
    {
        return !WorldType.isInSpectatorMode(p);
    }

    public boolean canBeSold()
    {
        return true;
    }

    public void performLoadWorldAction(World world) {}

    public void performResetAction(Player p) {}

    public float getMeleeDamage(EntityDamageByEntityEvent event)
    {
        return (float)event.getDamage();
    }

    public float getRangeDamage(EntityDamageByEntityEvent event)
    {
        return (float)event.getDamage();
    }

    public void performLeftClickedWhileInMidAirAction(Player clicker, Entity projectileClicked) {}

    public void performRightClickAction(Player p, Block blockClicked) {}

    public void performLeftClickAction(Player p, Block blockClicked) {}

    public boolean performDropAction(Player p) { return false; }

    public void performLandAction(Player p) {}

    public void performUnheldLandAction(Player p) {}

    public boolean performHeldLandAction(Player p)
    {
        return false;
    } //true if fall damage should be prevented

    public void performMeleeHit(LivingEntity attacker, Entity victim) {}

    public void performRangeHit(LivingEntity attacker, Entity victim) {}

    public void performProjectileLand(LivingEntity shooter, Projectile projectileThatLanded) {}

    public void performProjectileLaunch(LivingEntity shooter, Projectile projectileTheGotShot) {}

    public void performDeselectAction(Player p) {}

    public void performSelectAction(Player p) {}

    public void performUserDeathAction(Player p, Player killer) {}

    public boolean performItemPickup(Player p, Location whereTheItemWasFound) { return false; }

    public void performSneakAction(Player p, boolean sneaking) {}

    public void performBlockBreak(Player player, Block blockBroken) {}
}
