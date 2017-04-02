package me.happyman.SmashItemDrops;

import me.happyman.SmashKitMgt.SmashKitManager;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashAttackListener;
import me.happyman.utils.SmashWorldManager;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.concurrent.Callable;

public class SmashOrb implements CommandExecutor
{
    private static final String GIVE_ORB_COMMAND = "giveorb";

    private static HashMap<World, SmashOrbTracker> takenOrbWorlds;
    private static final int ORB_DURATION = 90*20;
    public static final float SMASH_ORB_SPAWN_CHANCE = 0.03F; //This is the absolute chance that an orb will spawn instead when an item supposedly is dropped

    public SmashOrb()
    {
        takenOrbWorlds = new HashMap<World, SmashOrbTracker>();
        SmashManager.getPlugin().setExecutor(GIVE_ORB_COMMAND, this);
    }

    public static void createOrb(final Location spawnLocation)
    {
        final World w = spawnLocation.getWorld();
        SmashWorldManager.sendMessageToWorld(spawnLocation.getWorld(), ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "" + ChatColor.BOLD + "A Final Smash Orb has spawned!");
        for (int i = 0; i < 3; i++)
        {
            for (Player p : w.getPlayers())
            {
                if (!SmashWorldManager.isInSpectatorMode(p))
                {
                    SmashWorldManager.playSoundToPlayers(w.getPlayers(), spawnLocation, Sound.ENDERMAN_TELEPORT, .6F, 1F);
                }
            }
        }

        Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
            public String call()
            {
                final SmashOrbTracker tracker = new SmashOrbTracker((Item)w.dropItem(spawnLocation, new ItemStack(SmashOrbTracker.getRandomOrbMaterial())), spawnLocation);

                int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable() {
                    int iteration = 0;
                    @Override
                    public void run() {
                        Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
                            public String call() {
                                if (iteration <= ORB_DURATION)
                                {
                                    if (iteration == ORB_DURATION)
                                    {
                                        tracker.remove();
                                    }
                                    else
                                    {
                                        tracker.act();
                                    }
                                    iteration++;
                                }
                                return "";
                            }
                        });
                    }
                }, 0, 1);
                tracker.setTask(task);
                SmashManager.getPlugin().cancelTaskAfterDelay(task, ORB_DURATION);
                return "";
            }
        });
    }

    /**
     *
     * @return - True if the entity was a non-removed orb
     */
    public static boolean removePossibleOrb(World w)
    {
        if (hasSmashOrb(w))
        {
            takenOrbWorlds.get(w).remove();
            return true;
        }
        return false;
    }

    public static SmashOrbTracker getSmashOrb(World w)
    {
        if (hasSmashOrb(w))
        {
            return takenOrbWorlds.get(w);
        }
        return null;
    }

    public static boolean canCreateOrb(World w)
    {
        if (hasSmashOrb(w))
        {
            return false;
        }
        for (Player p : w.getPlayers())
        {
            if (SmashKitManager.getSelectedKit(p).getProperties().getFinalSmash().hasTaskActive(p))
            {
                return false;
            }
        }
        return true;
    }

    public static boolean hasSmashOrb(World w)
    {
        return takenOrbWorlds.containsKey(w);
    }

    public static void hitSmashOrb(Player p)
    {
        World w = p.getWorld();
        if (hasSmashOrb(w))
        {
            if (!SmashKitManager.hasFinalSmashActive(p))
            {
                getSmashOrb(w).hit(p);
            }
        }
        else
        {
            SmashManager.getPlugin().sendErrorMessage("Error! Tried to hit a Smash Orb in a world that didn't have one!");
        }
    }

    public static boolean isLookingAtSmashOrb(Player p)
    {
        World w = p.getWorld();
        if (hasSmashOrb(w) && !SmashWorldManager.isInSpectatorMode(p))
        {
            Entity e = SmashAttackListener.getEntityBeingFaced(p, 4.2F, 5);
            return e != null && e instanceof Item && SmashOrbTracker.getOrbMaterials().contains(((Item)e).getItemStack().getType());
        }
        return false;
    }

    protected static void logOrb(SmashOrbTracker smashOrbTracker)
    {
        takenOrbWorlds.put(smashOrbTracker.getCenter().getWorld(), smashOrbTracker);
    }

    protected static void unlogSmashOrb(World w)
    {
        if (hasSmashOrb(w))
        {
            takenOrbWorlds.remove(w);
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (SmashManager.getPlugin().matchesCommand(label, GIVE_ORB_COMMAND))
        {
            if (!(sender instanceof Player) && args.length == 0)
            {
                sender.sendMessage(ChatColor.RED + "You must specify to whom you would like to give the orb.");
            }
            else
            {
                Player p;
                if (args.length > 0)
                {
                    p = Bukkit.getPlayer(args[0]);
                    if (p == null)
                    {
                        sender.sendMessage(ChatColor.RED +  "Player not found!");
                        return true;
                    }
                }
                else
                {
                    p = (Player)sender;
                }
                if (!SmashWorldManager.isSmashWorld(p.getWorld()))
                {
                    sender.sendMessage(ChatColor.RED + "That player isn't in a Smash world!");
                }
                else
                {
                    SmashKitManager.getSelectedKit(p).getProperties().getFinalSmash().give(p);
                }
            }
            return true;
        }
        return false;
    }
}
