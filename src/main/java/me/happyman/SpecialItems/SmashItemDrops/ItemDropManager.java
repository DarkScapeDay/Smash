package me.happyman.SpecialItems.SmashItemDrops;

import me.happyman.SpecialItems.SpecialItem;
import me.happyman.SpecialItems.SpecialItemTypes.MinePlacer;
import me.happyman.worlds.SmashWorldManager;
import me.happyman.worlds.WorldType;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import static me.happyman.Plugin.getPlugin;
import static me.happyman.Plugin.sendErrorMessage;

public class ItemDropManager
{
    private static final Material MATERIAL_FOR_ITEM_DROPS = Material.STEP;;
    private static final float ITEMSPAWN_CHANCE_PER_SECOND_PER_PLAYER = (float)0.025; //0-1 //.025 is about right //0.03
    public static final int ITEMSPAWN_PERIOD = 5; //Ticks (note that this won't affect how frequently items spawn overall)
    public static final float ITEMSPAWN_CHANCE_PER_PERIOD_PER_PLAYER = ITEMSPAWN_CHANCE_PER_SECOND_PER_PLAYER * ITEMSPAWN_PERIOD / 20;
    private static HashMap<String, SpecialItem> itemDropMap;
    private static HashMap<ItemStack, Float> spawnChances;
    private static float totalSpawnChances;
    private static ArrayList<Float> bases;
    private static ArrayList<ItemStack> itemDrops;
    private static boolean initialized = false;

    public static void initialize()
    {
        if (!initialized)
        {
            initialized = true;
            itemDropMap = new HashMap<String, SpecialItem>();
            spawnChances = new HashMap<ItemStack, Float>();
            totalSpawnChances = 0;
            bases = new ArrayList<Float>();

            Bukkit.getPluginManager().registerEvents(new ItemDropPickupListener(), getPlugin());
            new SmashOrbTracker();

            logItemDrop(new BaseballBat(3), 1F);
            logItemDrop(new Hammer(),1F);
            logItemDrop(new HealingItem.AppleRed(),0.7F);
            logItemDrop(new HealingItem.AppleGolden(),.22F);
            logItemDrop(new MinePlacer(new MinePlacer.MineItemStack(2),"Mine placed!", true, 50), 1F);
            logItemDrop(new Slowball(),1F);
            logItemDrop(new MonsterEgg(),1F);
            logItemDrop(new LightFeather(), 0.75F);
            logItemDrop(new SwitcherBall(), 2F);

            itemDrops = new ArrayList<ItemStack>(spawnChances.keySet());

            for (ItemStack item : itemDrops)
            {
                totalSpawnChances += getSpawnchance(item);
                bases.add(totalSpawnChances);
            }
        }
    }

    private static void logItemDrop(SpecialItem item, float spawnChance)
    {
        itemDropMap.put(item.getItemStack().getItemMeta().getDisplayName(), item);
        spawnChances.put(item.getItemStack(), spawnChance);
    }

    private static float getSpawnchance(ItemStack item)
    {
        if (spawnChances.containsKey(item))
        {
            return spawnChances.get(item);
        }
        sendErrorMessage("Error! Could not get the spawn chance because we didn't have it logged!");
        return -1;
    }

    public static ItemStack getRandomItemDrop()
    {
        float r = (new Random()).nextFloat()*totalSpawnChances;
        for (int i = 0; i < itemDrops.size(); i++)
        {
            if (r <= bases.get(i))
            {
                return itemDrops.get(i);
            }
        }

        sendErrorMessage("Error! Could not get random drop!");
        return null;
    }

    public static ArrayList<SpecialItem> getSmashItemDrops()
    {
        return new ArrayList<SpecialItem>(itemDropMap.values());
    }

    public static boolean isSmashDropItem(ItemStack item)
    {
        return getSmashDropItem(item) != null;
    }

    public static SpecialItem getSmashDropItem(ItemStack item)
    {
        if (item != null)
        {
            try
            {
                String name = item.getItemMeta().getDisplayName();
                return itemDropMap.get(name);
            }
            catch (NullPointerException ex) {}
        }
//        sendErrorMessage("Error! Tried to get the item drop represented by the itemstack " + item.toString() + ", but it wasn't an item drop!");
        return null;
    }


    private static class ItemDropPickupListener implements Listener
    {
        @EventHandler
        public static void onPickupEvent(PlayerPickupItemEvent e)
        {
            World w = e.getPlayer().getWorld();
            if (SmashWorldManager.isSmashWorld(w))
            {
                SpecialItem item = getSmashDropItem(e.getItem().getItemStack());
                if (item != null && e.getItem().getLocation().getBlock().getType().equals(MATERIAL_FOR_ITEM_DROPS) && !Hammer.isWieldingHammer(e.getPlayer()) && !WorldType.isInSpectatorMode(e.getPlayer()))
                {
                    item.performItemPickup(e.getPlayer(), e.getItem().getLocation());
                    item.performSelectAction(e.getPlayer());
                    e.getItem().getLocation().getBlock().setType(Material.AIR);
                }
                else //if (SmashOrb.getOrbMaterials().contains(e.getItemStack().getItemStack().getType()))
                {
                    e.setCancelled(true);
                }
            }
        }
    }
}
