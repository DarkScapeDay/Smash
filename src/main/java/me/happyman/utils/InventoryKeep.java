package me.happyman.utils;

import me.happyman.Plugin;
import me.happyman.SpecialItems.SmashItemDrops.ItemDropManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static me.happyman.Plugin.getPlugin;

public class InventoryKeep
{
    public static class KeptInventory
    {
        private final ItemStack[] items;
        private final ItemStack[] armor;
        private final long timePutOn;
        private final long durationTicks;
        private final boolean highPriority;
        private final int resetTask;
        private boolean expiredOverride;
        private final Player player;

        public KeptInventory(final Player p, long timePutOn, int ticksItLasts, boolean highPriority)
        {
            items = p.getInventory().getContents();
            armor = p.getEquipment().getArmorContents();
            this.timePutOn = timePutOn;
            this.durationTicks = (long)ticksItLasts;
            this.highPriority = highPriority;
            resetTask = Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable()
            {
                public void run()
                {
                    setToCorrectInventoryState(p);
                }
            }, ticksItLasts + 1);
            expiredOverride = false;
            this.player = p;
        }

        public void cancelReset()
        {
            Bukkit.getScheduler().cancelTask(resetTask);
        }

        public boolean isOlder(KeptInventory other)
        {
            return this.timePutOn < other.timePutOn;
        }

        public boolean hasExpired()
        {
            return expiredOverride || Plugin.getMillisecond() >= timePutOn + durationTicks * 50L;
        }

        public Player getPlayer()
        {
            return player;
        }

        public void load(Player p)
        {
            ItemStack[] itemsLoadingIn = items;
            if (!highPriority)
            {
                ItemStack[] presentItems = p.getInventory().getContents();
                for (int j = 0; j < 36; j++)
                {
                    if (itemsLoadingIn[j] != null)
                    for (int i = 0; i < 36; i++)
                    {
                        if (presentItems[i] != null && presentItems[i].equals(itemsLoadingIn[j]))
                        {
                            presentItems[j] = null;
                        }
                    }
                }

                //present items are no longer duplicates
                for (int i = 0; i < 36; i++)
                {
                    if (ItemDropManager.isSmashDropItem(presentItems[i]))
                    {
                        int j;
                        for (j = i; j < 36 && itemsLoadingIn[j] != null && !itemsLoadingIn[j].getType().equals(Material.AIR); j++);
                        if (j < 36)
                        {
                            itemsLoadingIn[j] = presentItems[i];
                        }
                    }
                }
            }

            p.getInventory().setContents(itemsLoadingIn);
            p.getEquipment().setArmorContents(armor);
        }

        public boolean isLocked()
        {
            return highPriority;
        }

        public void setExpired()
        {
            expiredOverride = true;
            cancelReset();
            setToCorrectInventoryState(getPlayer());
        }
    }

    private static final HashMap<Player, List<KeptInventory>> inventories = new HashMap<Player, List<KeptInventory>>();

    public static KeptInventory setTempInv(Player p, int ticksNewStateLasts, boolean lockState)
    {
        return setTempInv(p, null, null, ticksNewStateLasts, lockState);
    }

    public static KeptInventory setTempInv(Player p, ItemStack[] newInventoryContents, int ticksNewStateLasts, boolean lockState)
    {
        return setTempInv(p, newInventoryContents, null, ticksNewStateLasts, lockState);
    }

    public static KeptInventory setTempInv(final Player p, final ItemStack[] newInventoryContents, final ItemStack[] newArmorContents, int ticksNewStateLasts, boolean lockState)
    {
        if (!inventories.containsKey(p))
        {
            inventories.put(p, new ArrayList<KeptInventory>());
        }
        for (KeptInventory inv : inventories.get(p))
        {
            if (inv.isLocked())
            {
                return null;
            }
        }
        KeptInventory newInv = new KeptInventory(p, Plugin.getMillisecond(), ticksNewStateLasts, lockState);
        inventories.get(p).add(newInv);


        if (newInventoryContents != null)
        {
            p.getInventory().setContents(newInventoryContents);
        }
        if (newArmorContents != null)
        {
            p.getInventory().setArmorContents(newArmorContents);
        }
        return newInv;
    }

    private static void setToCorrectInventoryState(Player p)
    {
        if (inventories.containsKey(p))
        {
            List<KeptInventory> invList = inventories.get(p);
            KeptInventory oldest = null;
            for (int i = 0; i < invList.size(); i++)
            {
                KeptInventory inv = invList.get(i);
                if (inv.hasExpired())
                {
                    if (oldest == null || inv.isOlder(oldest))
                    {
                        oldest = inv;
                    }
                    inv.cancelReset();
                    invList.remove(inv);
                }
            }
            if (invList.size() == 0)
            {
                inventories.remove(p);
            }
            if (oldest != null)
            {
                oldest.load(p);
            }
        }
    }

    public static void resetInventoryState(Player p)
    {
        List<KeptInventory> invs = inventories.get(p);
        if (invs != null)
        {
            KeptInventory oldest = null;
            for (KeptInventory inv : invs)
            {
                if (oldest == null || inv.isOlder(oldest))
                {
                    oldest = inv;
                }
                inv.cancelReset();
            }
            if (oldest != null)
            {
                oldest.load(p);
            }
            inventories.remove(p);
        }
    }
}
