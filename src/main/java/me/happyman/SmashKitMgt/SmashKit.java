package me.happyman.SmashKitMgt;

import me.happyman.ItemTypes.*;
import me.happyman.SmashItems.*;
import me.happyman.SmashItemDrops.ItemDropManager;
import me.happyman.commands.SmashManager;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.util.*;
import java.util.concurrent.Callable;

public abstract class SmashKit implements Listener
{
    private final ItemStack[] contents;
    private final ItemStack[] armor;
    private final HashMap<ItemStack, SmashItem> items;

    private final SmashKitProperties properties;
    private final SmashItem sword;
    private final SmashItemWithUsages rocket;
    private final GroundPound groundPound;
    private static ShieldItem shield;
    private static CompassItem compass;

    public SmashKit(SmashKitProperties properties, LeatherArmorSet leatherArmor, SmashItem sword, SmashItemWithUsages rocket)
    {
        this(properties, leatherArmor, sword, rocket, null);
    }


    public SmashKit(SmashKitProperties properties, LeatherArmorSet leatherArmor, SmashItem sword, SmashItemWithUsages rocket, GroundPound groundPound)
    {
        this.properties = properties;

        items = new HashMap<ItemStack, SmashItem>();
        this.contents = new ItemStack[36];

        this.sword = sword;
        if (sword != null)
        {
            items.put(sword.getItem(), sword);
            this.contents[0] = sword.getItem();
        }

        this.rocket = rocket;
        items.put(rocket.getItem(), rocket);
        this.contents[1] = rocket.getItem();

        this.groundPound = groundPound;
        if (groundPound != null)
        {
            items.put(groundPound.getItem(), groundPound);
            this.contents[2] = groundPound.getItem();
        }

        this.shield = SmashKitManager.getShield();
        items.put(shield.getItem(), shield);
        this.contents[7] = shield.getItem();

        this.compass = SmashKitManager.getCompass();
        items.put(compass.getItem(), compass);
        this.contents[8] = compass.getItem();

        this.armor = new ItemStack[4];
        this.armor[3] = leatherArmor.getHelmet();
        this.armor[2] = leatherArmor.getChestplate();
        this.armor[1] = leatherArmor.getLeggings();
        this.armor[0] = leatherArmor.getBoots();

        Bukkit.getServer().getPluginManager().registerEvents(this, SmashManager.getPlugin());

        SmashKitManager.logSmashKit(this);
    }

    public String getDescription()
    {
        return properties.getKitRepresenter().getItemMeta().getLore().get(0);
    }

    public void addHiddenItem(SmashItem item)
    {
        int i = 9;
        while (contents[i] != null && i < 36)
        {
            i++;
        }
        if (contents[i] == null)
        {
            contents[i] = item.getItem();
        }
        items.put(item.getItem(), item);
    }

    public void addItem(SmashItem item, Integer invSlot)
    {
        if (invSlot != null)
        {
            int i = 0;
            if (contents[i] != null)
            {
                SmashManager.getPlugin().sendErrorMessage("Warning! Overwrote a Smash item with " + item.getItem().getItemMeta().getDisplayName() + " at slot " + invSlot + ".");
            }
            contents[i] = item.getItem();
        }
        items.put(item.getItem(), item);
    }

    public void addSecretItem(SmashItem item)
    {
        addItem(item, null);
    }

    public void addItem(SmashItem item)
    {
        int i = 0;
        while (contents[i] != null && i < 9)
        {
            i++;
        }
        if (contents[i] == null)
        {
            contents[i] = item.getItem();
        }
        items.put(item.getItem(), item);
    }

    public void applyKitInventory(Player p)
    {
        applyKitInventory(p, true);
    }

    public void applyKitInventory(final Player p, boolean clearItemDrops)
    {
        if (clearItemDrops)
        {
            p.getInventory().setContents(contents);
            applyKitArmor(p);
            Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable()
            {
                public String call()
                {
                    p.updateInventory();
                    return "";
                }
            });
        }
        else
        {
            final List<ItemStack> itemDropItems = new ArrayList<ItemStack>();
            for (ItemStack item : p.getInventory().getContents())
            {
                if (ItemDropManager.isSmashDropItem(item))
                {
                    itemDropItems.add(item);
                }
            }
            /*for (int i = 0; i < 9; i++)
            {
                if (!(p.getItemInHand().hasItemMeta() && p.getItemInHand().getItemMeta().hasDisplayName() && p.getItemInHand().getItemMeta().getDisplayName().equals(getShield().getItem().getItemMeta().getDisplayName())))
                {
                    //Bukkit.getPlayer("HappyMan").sendMessage("Set item at " + i + " to " + getName() + "'s");
                    p.getInventory().setItem(i, contents[i]);
                }
            }
            p.getInventory().setArmorContents(armor);*/
            applyKitInventory(p, true);
            Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable()
            {
                public String call()
                {
                    p.updateInventory();
                    return "";
                }
            });
            Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
                public String call() {
                    for (int i = 0; i < 36  && itemDropItems.size() > 0; i++)
                    {
                        if (p.getInventory().getItem(i) == null || p.getInventory().getItem(i).getType().equals(Material.AIR))
                        {
                            p.getInventory().setItem(i, itemDropItems.get(0));
                            itemDropItems.remove(0);
                        }
                    }
                    return "";
                }
            });
        }
    }

    public void applyKitArmor(Player p)
    {
        p.getInventory().setArmorContents(armor);
    }

    public boolean hasGroundPound()
    {
        return groundPound != null;
    }

    public GroundPound getGroundPound()
    {
        return groundPound;
    }

    public CompassItem getCompass()
    {
        return compass;
    }

    public SmashItem getSword()
    {
        if (sword != null)
        {
            return sword;
        }
        SmashManager.getPlugin().sendErrorMessage("Error! No sword item found!");
        return null;
    }

    public ShieldItem getShield()
    {
        return shield;
    }

    public SmashItemWithUsages getRocket()
    {
        return rocket;
    }

    public boolean isImmuneToFire()
    {
        return properties.isImmuneToFire();
    }

    public String getName()
    {
        return properties.getName();
    }

    public Set<ItemStack> getItemStacks()
    {
        return items.keySet();
    }

    public boolean hasItem(ItemStack item)
    {
        if (item != null)
        {
            if (items.containsKey(item))
            {
                return true;
            }
            for (SmashItem sitem : getItems())
            {
                if (sitem.isThis(item))
                {
                    return true;
                }
            }
        }
        return properties.getFinalSmash().getItem().equals(item);
    }

    public SmashItem getItem(ItemStack item)
    {
        if (items.containsKey(item))
        {
            return items.get(item);
        }
        else if (properties.getFinalSmash().getItem().equals(item))
        {
            return properties.getFinalSmash();
        }
        if (item != null)
        {
            for (SmashItem sitem : getItems())
            {
                //Bukkit.broadcastMessage("found " + sitem.getItem().getItemMeta().getDisplayName());
                if (sitem.isThis(item))
                {
                    return sitem;
                }
            };
        }
        return null;
    }

    public Collection<SmashItem> getItems()
    {
        return items.values();
    }

    public ItemStack[] getContents()
    {
        return contents;
    }

    public ItemStack[] getArmor()
    {
        return armor;
    }

    public boolean isActive(Player p)
    {
        return !SmashKitManager.canChangeKit(p);
    }

    public ItemStack getKitRepresenter()
    {
        return properties.getKitRepresenter();
    }

    public float getDamageIntakeMod()
    {
        return properties.getDamageIntakeMod();
    }

    public SmashKitProperties getProperties()
    {
        return properties;
    }

    public float getDamageOutPutMod()
    {
        return properties.getDamageOutPutMod();
    }
}
