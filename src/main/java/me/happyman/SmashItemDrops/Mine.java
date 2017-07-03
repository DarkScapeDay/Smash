package me.happyman.SmashItemDrops;

import me.happyman.ItemTypes.SmashItem;
import me.happyman.commands.SmashManager;
import me.happyman.utils.DirectoryType;
import me.happyman.utils.SmashEntityTracker;
import me.happyman.Listeners.SmashItemManager;
import me.happyman.worlds.SmashWorldInteractor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.*;
import java.util.concurrent.Callable;

public class Mine extends SmashItem implements Listener
{
    private static final float SLOWDOWN_SPEED = 0.1414F;

    private static HashMap<World, List<Block>> mines;
    private static HashMap<Location, Player> mineCulprits;
    private static HashMap<Block, Integer> mineNumber;
    private static List<TNTPrimed> cancelRemoveTasks;
    public final ItemStack MINE_ITEM;
    public static final String MINE_LOCATION_FILE = "MineLocations";

    public Mine()
    {
        super(Material.STONE_PLATE, ChatColor.GRAY + " " + ChatColor.BOLD + "Land Mine");
        Bukkit.getPluginManager().registerEvents(this, SmashManager.getPlugin());

        mineCulprits = new HashMap<Location, Player>();
        cancelRemoveTasks = new ArrayList<TNTPrimed>();
        mines = new HashMap<World, List<Block>>();
        mineNumber = new HashMap<Block, Integer>();

        getItem().setAmount(2);
        MINE_ITEM = getItem();
    }

    public static void clearMines(World w)
    {
        if (mines.containsKey(w))
        {
            while (mines.get(w).size() > 0)
            {
                Block b = mines.get(w).get(0);
                activateMine(b);
                if (mineCulprits.containsKey(b.getLocation()))
                {
                    mineCulprits.remove(b.getLocation());
                }
                mines.get(w).remove(0);
            }
        }
        else if (SmashManager.getPlugin().hasFile(DirectoryType.ROOT, w.getName(), MINE_LOCATION_FILE))
        {
            final String worldName = w.getName();
            List<ArrayList<String>> locationList = SmashManager.getPlugin().getAllDataSimple(DirectoryType.ROOT, worldName, MINE_LOCATION_FILE);
            int size = locationList.size();
            for (int i = 0; i < size; i++)
            {
                Block b = w.getBlockAt(Integer.valueOf(locationList.get(i).get(0)), Integer.valueOf(locationList.get(i).get(1)), Integer.valueOf(locationList.get(i).get(2)));
                blowBlockUp(b, false);
            }
            if (size > 0)
            {
                SmashManager.getPlugin().clearFile(DirectoryType.ROOT, worldName, MINE_LOCATION_FILE);
            }
        }
    }

    private Block getRandomOffsetBlock(Block b)
    {
        return b.getRelative((new Random()).nextInt(3) - 1, 0, (new Random()).nextInt(3) - 1);
    }

    private boolean goodBlock(Block b)
    {
        return b.getRelative(0, 1, 0).getType().equals(Material.AIR) && !b.getType().equals(Material.AIR);
    }

    private void placeMineOnBlock(Player culprit, Block b)
    {
        Block mineBlock = b.getRelative(0, 1, 0);

        if (!mines.containsKey(mineBlock.getWorld()))
        {
            mines.put(mineBlock.getWorld(), new ArrayList<Block>());
        }

        int number = mines.get(mineBlock.getWorld()).size();
        mineNumber.put(mineBlock, number);
        SmashManager.getPlugin().putDatum(DirectoryType.ROOT, mineBlock.getWorld().getName(), MINE_LOCATION_FILE, "l" + number,
                String.format("%1$d %2$d %3$d", mineBlock.getX(), mineBlock.getY(), mineBlock.getZ()));

        mineBlock.setType(MINE_ITEM.getType());
        mines.get(mineBlock.getWorld()).add(mineBlock);
        mineCulprits.put(mineBlock.getLocation(), culprit);
    }

    public static void activateMine(final Block mine)
    {
        activateMine(mine, false);
    }

    public static void activateMine(final Block mine, boolean monsterSteppedOnIt)
    {
        final Location l = mine.getLocation();
        TNTPrimed killer = blowBlockUp(mine, monsterSteppedOnIt);
        if (mineNumber.containsKey(mine))
        {
            SmashManager.getPlugin().removeData(DirectoryType.ROOT, mine.getWorld().getName(),"l" + mineNumber.get(mine));
            mineNumber.remove(mine);
        }
        if (mineCulprits.containsKey(l))
        {
            SmashEntityTracker.addCulprit(mineCulprits.get(l), killer, "Land Mine", 50);
            mineCulprits.remove(l);
        }
        // else  {plugin.sendErrorMessage("Warning! Called activateMine on a mine that was not accounted for!");}
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
            Bukkit.getScheduler().scheduleSyncDelayedTask(SmashManager.getPlugin(), new Runnable() {
                public void run() {
                    b.setType(Material.AIR);
                }
            }, 1);
        }
        else
        {
            Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
                public String call() {
                    b.setType(Material.AIR);
                    return "";
                }
            });
        }
        return tnt;
    }

    public void performRightClickAction(final Player p)
    {
        ItemStack itemInhand = p.getItemInHand();
        final boolean threwMultiple;

        if (itemInhand.getAmount() < MINE_ITEM.getAmount())
        {
            p.setItemInHand(new ItemStack(Material.AIR));
            threwMultiple = false;
        }
        else
        {
            SmashItemManager.removeItemsFromHand(p, MINE_ITEM.getAmount());
            threwMultiple = true;
        }

        final Item item = p.getWorld().dropItem(p.getLocation(), itemInhand);
        Vector v = p.getLocation().getDirection();
        float vMod = 1.8F;
        v.setX(v.getX() * vMod);
        v.setY(v.getY() * vMod);
        v.setZ(v.getZ() * vMod);
        //Bukkit.getPlayer("HappyMan").sendMessage("setting velocity of mine");
        item.setVelocity(v);

        final Block targetBlock = p.getTargetBlock(new HashSet<Material>(Arrays.asList(Material.AIR)), 300);

        Bukkit.getScheduler().scheduleSyncDelayedTask(SmashManager.getPlugin(), new Runnable()
        {
            public void run()
            {
                item.remove();
                if (targetBlock.getY() < p.getLocation().getBlockY() + 3 && !targetBlock.getType().equals(Material.AIR) && targetBlock.getRelative(0, 1, 0).getType().equals(Material.AIR))
                {
                    if (Bukkit.getWorlds().contains(targetBlock.getLocation().getWorld()))
                    {
                        if (goodBlock(targetBlock))
                        {
                            placeMineOnBlock(p, targetBlock);
                        }
                        if (threwMultiple)
                        {
                            Block otherBlock = getRandomOffsetBlock(targetBlock);
                            for (int i = 0; i < 5 && !goodBlock(otherBlock); i++)
                            {
                                Block b = getRandomOffsetBlock(targetBlock);
                                if (goodBlock(b))
                                {
                                    otherBlock = b;
                                    break;
                                }
                            }
                            if (goodBlock(otherBlock))
                            {
                                placeMineOnBlock(p, otherBlock);
                            }
                        }
                    }
                }
            }
        }, Math.round(p.getLocation().distance(targetBlock.getLocation())/1.33));
    }

    @EventHandler
    public void steppedOnMine (PlayerInteractEvent e)
    {
        if (e.getAction().equals(Action.PHYSICAL) && mineNumber.containsKey(e.getClickedBlock()))
        {
            if (SmashWorldInteractor.isInSpectatorMode(e.getPlayer()))
            {
                e.setCancelled(true);
            }
            else
            {
                final Block b = e.getClickedBlock();
                activateMine(b, false);
            }
        }
    }

    @EventHandler
    public void monsterWasDumb(EntityInteractEvent e)
    {
        final Block b = e.getBlock();
        if (mineNumber.containsKey(b))
        {
            activateMine(b, true);
        }
    }

    @EventHandler
    public void hitMineProjEvent(final ProjectileHitEvent e)
    {
        final Entity proj = e.getEntity();
        Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
            public String call()
            {
                Block b = proj.getLocation().getBlock();
                if (mineNumber.containsKey(b))
                {
                    activateMine(b, false);
                }
                return "";
            }
        });
    }
}
