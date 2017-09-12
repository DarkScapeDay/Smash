package me.happyman.SpecialItems.SpecialItemTypes;

import me.happyman.SpecialItems.UsefulItemStack;
import me.happyman.worlds.WorldManager;
import me.happyman.worlds.WorldType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;

import java.util.concurrent.Callable;

import static me.happyman.Plugin.*;

public class MinePlacer extends PlaceableItem
{
    private final Integer customDamage;
    private static final int MAX_SHOOTING_ABOVE = 5;
    private final boolean allowMonstersExplodingThem;

    public MinePlacer(MineItemStack item, String placementMessage, boolean allowMonstersExplodingThem)
    {
        this(item, placementMessage, allowMonstersExplodingThem, null);
    }

    public MinePlacer(MineItemStack item, String placementMessage, boolean allowMonstersExplodingThem, Integer customDamage)
    {
        super(item, placementMessage, "MineLocations.json", new Material[]
        {
            Material.WOOD_PLATE
        });
        this.allowMonstersExplodingThem = allowMonstersExplodingThem;
        this.customDamage = customDamage;
    }

    public static class MineItemStack extends UsefulItemStack
    {
        private static final Material DEFAULT_MATERIAL = Material.STONE_PLATE;
        private static final String DEFAULT_NAME = ChatColor.GRAY + "" + ChatColor.BOLD + "Land Mine";

        public MineItemStack()
        {
            super(DEFAULT_MATERIAL, DEFAULT_NAME);
        }

        public MineItemStack(int amount)
        {
            super(DEFAULT_MATERIAL, DEFAULT_NAME, amount);
        }

        public MineItemStack(int amount, short damage)
        {
            super(DEFAULT_MATERIAL, DEFAULT_NAME, amount, damage);
        }

        public MineItemStack(int amount, short damage, Byte data)
        {
            super(DEFAULT_MATERIAL, DEFAULT_NAME, amount, damage, data);
        }
    }


    public static TNTPrimed blowBlockUp(final Block b, boolean monsterSteppedOnIt)
    {
        Location l = b.getLocation();
        l.setX(l.getX() + 0.5);
        l.setZ(l.getZ() + 0.5);
        TNTPrimed tnt = (TNTPrimed)b.getWorld().spawnEntity(l, EntityType.PRIMED_TNT);
        tnt.setFuseTicks(0);
        if (monsterSteppedOnIt)
        {
            Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable()
            {
                public void run()
                {
                    b.setType(Material.AIR);
                }
            }, 1);
        }
        else
        {
            Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
            {
                public String call()
                {
                    b.setType(Material.AIR);
                    return "";
                }
            });
        }
        return tnt;
    }

    private void activateMine(PlacedBlock block, boolean monsterSteppedOnIt)
    {
        performBlowupAction(block);
        WorldManager.setAttackSource(blowBlockUp(block.getBlock(), monsterSteppedOnIt), new WorldType.AttackSource(new WorldType.AttackSource.AttackCulprit(block.getPlacerName(), this), true, customDamage));
    }

    protected void performBlowupAction(PlacedBlock block) {}

    @Override
    protected void performMiscClearanceAction(PlacedBlock block)
    {
        activateMine(block, false);
    }

    @Override
    protected boolean performBrokenAction(PlacedBlock block, Player breaker)
    {
        activateMine(block, false);
        return false;
    }

    @Override
    protected boolean performSteppedOn(PlacedBlock block, LivingEntity who)
    {
        boolean wasPlayer = who instanceof Player;
        if (wasPlayer || allowMonstersExplodingThem)
        {
            activateMine(block, !wasPlayer);
            return false;
        }
        return true;
    }

    @Override
    protected boolean performHitByProjectileEvent(PlacedBlock block, LivingEntity shooter)
    {
        boolean wasPlayer = shooter instanceof Player;
        if (wasPlayer || allowMonstersExplodingThem)
        {
            activateMine(block, false);
            return false;
        }
        return true;
    }

    @Override
    protected boolean canPlaceOnBlock(Player p, Block baseBlock)
    {
        return super.canPlaceOnBlock(p, baseBlock) && baseBlock.getY() < p.getLocation().getBlockY() + MAX_SHOOTING_ABOVE;
    }

    @Override
    public void performRightClickAction(final Player p, Block blockClicked)
    {
        super.performRightClickAction(p, blockClicked);
        placeAllInHandWhereLooking(p, 300);
    }
}
