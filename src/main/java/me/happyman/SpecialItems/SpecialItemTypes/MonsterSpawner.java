package me.happyman.SpecialItems.SpecialItemTypes;

import com.sun.istack.internal.NotNull;
import me.happyman.MetaWorldGeneratorMain;
import me.happyman.Plugin;
import me.happyman.SpecialItems.UsefulItemStack;
import me.happyman.worlds.GuiManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.CreatureSpawner;
import org.bukkit.block.Sign;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.concurrent.Callable;

public class MonsterSpawner extends PlaceableItem //implements Listener
{
    private static final HashMap<Player, MonsterType> selectedMonsters = new HashMap<Player, MonsterType>();
//    private static boolean initialized = false;

    public MonsterSpawner()
    {
        super(new UsefulItemStack(Material.MOB_SPAWNER, ChatColor.BLACK + "" + ChatColor.BOLD + "Monster Spawner"),
                ChatColor.GREEN + "Mob spawner placed!", "MobSpawnerLocations.json");
//        if (!initialized)
//        {
//            Bukkit.getPluginManager().registerEvents(this, Plugin.getPlugin());
//            initialized = true;
//        }
    }

    private static MonsterType getMonsterType(PlacedBlock knownBlock)
    {
        Object additionalData = knownBlock.getAdditionalData();
        if (additionalData != null && additionalData instanceof MonsterType)
        {
            return (MonsterType)additionalData;
        }
        Plugin.sendErrorMessage("Error! Could not get monster type from a monster spawner at " + knownBlock.getCoords());
        return null;
    }

//    @EventHandler
//    public static void handleMobSpawn(SpawnerSpawnEvent event)
//    {
//        PlacedBlock knownBlock = getKnownBlock(event.getSpawner().getBlock());
//        if (knownBlock != null && knownBlock.getOwner().getClass() == MonsterSpawner.class)
//        {
//            MonsterType monsterType = getMonsterType(knownBlock);
//            if (monsterType != null)
//            {
//                event.setCancelled(true);
//                Location location = event.getEntity().getLocation();
//                location.getWorld().spawnEntity(location, monsterType.getMonster());
//            }
//        }
//    }

    private static class MonsterType
    {
        private final EntityType monster;
        private final String displayName;
        private final byte data;

        public MonsterType(@NotNull EntityType monster, @NotNull String displayName, byte dataOfSpawnEgg)
        {
            this.monster = monster;
            this.displayName = displayName;
            this.data = dataOfSpawnEgg;
        }

        public ItemStack getRepresentingItem()
        {
            return new UsefulItemStack(Material.MONSTER_EGG, displayName, data);
        }

        public String getDisplayName()
        {
            return displayName;
        }

        public EntityType getMonster()
        {
            return monster;
        }
    }

    private static final MonsterType[] MONSTER_LIST = new MonsterType[]
    {
        new MonsterType(EntityType.ZOMBIE, "Zombie", (byte)40),
        new MonsterType(EntityType.PIG_ZOMBIE, "Zombie Pigman", (byte)42),
        new MonsterType(EntityType.SKELETON, "Skeleton", (byte)28),
        new MonsterType(EntityType.ENDERMAN, "Enderman", (byte)8)
    };

    private void openSelectMonsterGui(Player p)
    {
        new SelectMonsterGui().open(p);
    }

    class SelectMonsterGui extends GuiManager.GuiInventory
    {
        SelectMonsterGui()
        {
            super(ChatColor.BLACK + "" + ChatColor.BOLD + "Select Monster");
            for (final MonsterType monster : MONSTER_LIST)
            {
                addItem(new GuiManager.GuiItem(monster.getRepresentingItem())
                {
                    @Override
                    public void performAction(Player clicker)
                    {
                        selectedMonsters.put(clicker, monster);
                        clicker.sendMessage(ChatColor.GREEN + "Selected " + monster.getDisplayName() + "!");
                        clicker.closeInventory();
                    }
                }, false);
            }
        }
    }

    @Override
    protected String[] serializeAdditionalData(Player placer)
    {
        MonsterType selectedMonster = selectedMonsters.get(placer);
        return new String[] {selectedMonster == null ? null : selectedMonster.getMonster().name()};
    }

    @Override
    protected MonsterType deserializeAdditionalData(String[] additionalDatas)
    {
        if (additionalDatas.length > 0)
        {
            String typeName = additionalDatas[0];
            if (!typeName.equals("\0"))
            {
                for (MonsterType monsterType : MONSTER_LIST)
                {
                    if (monsterType.getMonster().name().equals(typeName))
                    {
                        return monsterType;
                    }
                }
            }
        }
        return null;
    }

    @Override
    public void performDeselectAction(Player p)
    {
        super.performDeselectAction(p);
        selectedMonsters.remove(p);
    }

    @Override
    public void performRightClickAction(Player p, Block blockClicked)
    {
        super.performRightClickAction(p, blockClicked);
        boolean needToPick = true;
        if (blockClicked != null)
        {
            final MonsterType selectedMonster = selectedMonsters.get(p);
            if (selectedMonster != null)
            {
                final Block placedBlock = placeOne(p, blockClicked);
                if (placedBlock != null && placedBlock.getState() instanceof CreatureSpawner)
                {
                    CreatureSpawner spawner = (CreatureSpawner)placedBlock.getState();
                    spawner.setSpawnedType(selectedMonster.getMonster());
                    spawner.update();
                    needToPick = false;
                }
            }
            else
            {
                p.sendMessage(ChatColor.GREEN + "Choose a monster type before placing.");
            }
        }
        if (needToPick)
        {
            openSelectMonsterGui(p);
        }
    }

    @Override
    protected void performMiscClearanceAction(PlaceableItem.PlacedBlock block)
    {

    }

    @Override
    protected boolean performBrokenAction(PlaceableItem.PlacedBlock block, Player player)
    {
        if (give(player))
        {
            player.sendMessage(ChatColor.GREEN + "Monster spawner broken!");
            return false;
        }
        player.sendMessage(ChatColor.GREEN + "You've got to make space, Brian.");
        return true;
    }

    @Override
    protected boolean performSteppedOn(PlaceableItem.PlacedBlock block, LivingEntity who)
    {
        return true;
    }

    @Override
    protected boolean performHitByProjectileEvent(PlaceableItem.PlacedBlock block, LivingEntity shooter)
    {
        if (shooter instanceof Player)
        {
            Player p = (Player)shooter;
            MonsterType type = getMonsterType(block);
            String prefix = ChatColor.GREEN + "That spawner is currently spawning ";
            p.sendMessage(prefix + (type == null ? "an unknown monster" : type.getDisplayName()));
        }
        return true;
    }
}