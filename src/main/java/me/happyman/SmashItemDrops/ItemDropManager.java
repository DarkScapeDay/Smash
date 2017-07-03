package me.happyman.SmashItemDrops;

import me.happyman.ItemTypes.SmashItem;
import me.happyman.commands.SmashManager;
import me.happyman.worlds.SmashWorldInteractor;
import me.happyman.worlds.SmashWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class ItemDropManager implements Listener
{
    public static final Material MATERIAL_FOR_ITEM_DROPS = Material.STEP;;
    private static final float ITEMSPAWN_CHANCE_PER_SECOND_PER_PLAYER = (float)0.025; //0-1 //.025 is about right //0.03
    public static final int ITEMSPAWN_PERIOD = 5; //Ticks (note that this won't affect how frequently items spawn overall)
    public static final float ITEMSPAWN_CHANCE_PER_PERIOD_PER_PLAYER = ITEMSPAWN_CHANCE_PER_SECOND_PER_PLAYER * ITEMSPAWN_PERIOD / 20;
    private static HashMap<String, SmashItem> itemDropMap;
    private static HashMap<ItemStack, Float> spawnChances;
    private static float totalSpawnChances;
    private static ArrayList<Float> bases;
    private static ArrayList<ItemStack> sortedItems;

    public ItemDropManager()
    {
        new SmashOrb();

        itemDropMap = new HashMap<String, SmashItem>();
        spawnChances = new HashMap<ItemStack, Float>();

        logItemDrop(new BaseballBat(), 1F);
        logItemDrop(new Hammer(),1F);
        logItemDrop(new AppleRed(),0.7F);
        logItemDrop(new AppleGolden(),.22F);
        logItemDrop(new Mine(),1F);
        logItemDrop(new Slowball(),1F);
        logItemDrop(new MonsterEgg(),1F);
        logItemDrop(new LightFeather(), 0.75F);
        logItemDrop(new SwitcherBall(), 2F);

        Bukkit.getPluginManager().registerEvents(this, SmashManager.getPlugin());

        sortedItems = new ArrayList<ItemStack>(spawnChances.keySet());
        totalSpawnChances = 0;
        bases = new ArrayList<Float>();
        for (ItemStack item : sortedItems)
        {
            totalSpawnChances += getSpawnchance(item);
            bases.add(totalSpawnChances);
        }
    }

    private void logItemDrop(SmashItem item, float spawnChance)
    {
        itemDropMap.put(item.getItem().getItemMeta().getDisplayName(), item);
        spawnChances.put(item.getItem(), spawnChance);
    }

    private static float getSpawnchance(ItemStack item)
    {
        if (spawnChances.containsKey(item))
        {
            return spawnChances.get(item);
        }
        SmashManager.getPlugin().sendErrorMessage("Error! Could not get the spawn chance because we didn't have it logged!");
        return -1;
    }

    public static ItemStack getRandomItemDrop()
    {
        float r = (new Random()).nextFloat()*totalSpawnChances;
        for (int i = 0; i < sortedItems.size(); i++)
        {
            if (r <= bases.get(i))
            {
                return sortedItems.get(i);
            }
        }
        return null;
    }

    public static boolean isSmashDropItem(ItemStack item)
    {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName() && itemDropMap.containsKey(item.getItemMeta().getDisplayName());
    }

    public static SmashItem getSmashDropItem(ItemStack item)
    {
        if (isSmashDropItem(item))
        {
            return itemDropMap.get(item.getItemMeta().getDisplayName());
        }
        SmashManager.getPlugin().sendErrorMessage("Error! Tried to get the item drop represented by the itemstack " + item.toString() + ", but it wasn't an item drop!");
        return null;
    }

    @EventHandler
    public void onPickupEvent(PlayerPickupItemEvent e)
    {
        World w = e.getPlayer().getWorld();
        if (SmashWorldManager.isSmashWorld(w))
        {
            if (isSmashDropItem(e.getItem().getItemStack()) && e.getItem().getLocation().getBlock().getType().equals(MATERIAL_FOR_ITEM_DROPS) && !Hammer.isWieldingHammer(e.getPlayer()) && !SmashWorldInteractor.isInSpectatorMode(e.getPlayer()))
            {
                e.getItem().getLocation().getBlock().setType(Material.AIR);
            }
            else// if (SmashOrbTracker.getOrbMaterials().contains(e.getItem().getItemStack().getType()))
            {
                e.setCancelled(true);
            }
        }
    }
}
