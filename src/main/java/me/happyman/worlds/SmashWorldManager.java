package me.happyman.worlds;

import me.happyman.Plugin;
import me.happyman.SpecialItems.SmashItemDrops.MonsterEgg;
import me.happyman.SpecialItems.SmashItemDrops.SmashOrbTracker;
import me.happyman.SpecialItems.SpecialItemTypes.MinePlacer;
import me.happyman.utils.SmashAttackManager;
import me.happyman.utils.SmashItemManager;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Callable;

import static me.happyman.Plugin.*;
import static me.happyman.worlds.WorldManager.FIND_CMD;
import static me.happyman.worlds.WorldManager.LIST_MAP_CMD;

public class SmashWorldManager implements CommandExecutor
{

    public static final long EXPIRATION_MINUTES = 120; //The minutes that a player's tournament level lasts

    private static final int MAX_LIVES = 10; //The max lives a player can prefer for deathmatch games
    public static final String DMG_CMD = "adddamage"; //Adds DMG points to a player (not hearts, DMG!)
    public static final String SET_DEATHMATCH_PREFERENCE_CMD = "dm"; //The command used to set your deathmatch preference
    public static final int SPAWN_HEIGHT = 135; //The height which is used for KO zone calculations
    public static final int SEARCH_DISTANCE = 100; //The maximum distance that will be searched (in blocks) for spawn locations
    public static final long DEFAULT_WORLD_TIME = 7000;
    private static final ArrayList<Player> deadPlayers = new ArrayList<Player>(); //This is for keeping track of who should be tped when they exit the KO zone but shouldn't be tped right away
    private static final Random r = new Random();
    private static final ArrayList<World> startedGames = new ArrayList<World>();
    private static final HashMap<World, ArrayList<Location>> playerSpawnLocations = new HashMap<World, ArrayList<Location>>(); //All the locations where players can spawn
    private static final HashMap<World, ArrayList<Location>> itemSpawnLocations = new HashMap<World, ArrayList<Location>>(); //All the locations where items can spawn
    private static final HashMap<World, Integer> endGameTasks = new HashMap<World, Integer>(); //The task for ending a Smash game

    //Can you tell I like HashMaps?

    /**
     * Constructor: SmashWorldManager
     * Purpose: To initialize the data structures and perform startup tasks such as
     * loading world and player data and stuff like that
     */
    public SmashWorldManager()
    {
        new SmashItemManager();
        new SmashWorldListener();
        new PortalManager();

        setExecutor(LIST_MAP_CMD, this);
        setExecutor(FIND_CMD, this);
        setExecutor(DMG_CMD, this);
        setExecutor(SET_DEATHMATCH_PREFERENCE_CMD, this);

        //motdMessages.add(loggerPrefix() + ChatColor.AQUA + "I don't sell anything! If you'd like to donate, you can show your support at " + ChatColor.UNDERLINE + "http://patreon.com/happyman." + ChatColor.RESET + "" + ChatColor.AQUA + "All donations are greatly appreciated!");
        addMotdMessage("I need maps! Send any map submissions to me at happyman@finalsmash.us");
        addMotdMessage("Did you know you can /dm to set your gamemode preference?");
        addMotdMessage("Special thanks to B1izzard10 and TheCyberCreeper for helping Smash bugs!");
        addMotdMessage("HappyMan's email is happyman@finalsmash.us if you want to contact him for " + ChatColor.ITALIC + "any" + ChatColor.RESET + "" + ChatColor.AQUA + " reason");
        addMotdMessage("Did you know that you can afk in a world as long as you want since a sound is played when a player joins?");
        addMotdMessage("HappyMan recommends that you use hotkeys to switch between your items quickly.");

        Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
            public void run() {
                String message = WorldManager.motdMessages.get(r.nextInt(WorldManager.motdMessages.size()));
                for (World w : Bukkit.getWorlds())
                {
                    if (isSmashWorld(w) && !gameHasStarted(w))
                    {
                        WorldType.sendMessageToWorld(w, message, false);
                    }
                }
            }
        }, WorldManager.MOTD_PERIOD*10, WorldManager.MOTD_PERIOD*20);
    }

    protected static void setIsDead(Player p, boolean dead)
    {
        if (dead && !deadPlayers.contains(p))
        {
            deadPlayers.add(p);
        }
        else if (!dead && deadPlayers.contains(p))
        {
            deadPlayers.remove(p);
        }
    }

    /**
     * Function: isDead
     * Purpose: To determine if a player is in a state of emminent respawning
     * @param p - The player to be checked for being respawned
     * @return - True if the player is already about to respawn from being outside the KO zone
     */
    public static boolean isDead(Player p)
    {
        return deadPlayers.contains(p);
    }

    /**
     * Function: isSmashWorld
     * Purpose: Probably the most useful function in this class, it determines whether a world is a Smash world or not
     * @param w - The world which we want to check for being a Smash world
     * @return - True if the world is a Smash world
     */
    public static boolean isSmashWorld(World w)
    {
        return isSmashWorld(w.getName());
    }

    /**
     * Function: The real check - sees if a String corresponds to being a Smash world
     * Purpose: To determine from a world colorlessName if the world is a Smash world
     * @param worldName - The colorlessName of the world which we want to know if it matches a Smash world colorlessName
     * @return - True if the world colorlessName is that of a Smash world
     */
    public static boolean isSmashWorld(String worldName)
    {
        return isSmashWorld(WorldType.getWorldType(worldName));
    }

    public static boolean isSmashWorld(WorldType type)
    {
        return type == WorldType.SMASH;
    }

    public static Location getRandomPlayerSpawnLocation(World w)
    {
        ArrayList<Location> possibleSpawnLocations = playerSpawnLocations.get(w);
        return possibleSpawnLocations.get(r.nextInt(possibleSpawnLocations.size()));
    }

    public static Location getRandomItemSpawnLocation(World w)
    {
        ArrayList<Location> possibleSpawnLocations = itemSpawnLocations.get(w);
        return possibleSpawnLocations.get(r.nextInt(possibleSpawnLocations.size()));
    }

    public static void addPlayerSpawnLocation(World w, Location location)
    {
        ArrayList<Location> listToAddTo = playerSpawnLocations.get(w);
        if (listToAddTo == null)
        {
            listToAddTo = new ArrayList<Location>();
            playerSpawnLocations.put(w, listToAddTo);
        }
        listToAddTo.add(location);
    }

    public static void addItemSpawnLocation(World w, Location location)
    {
        ArrayList<Location> listToAddTo = itemSpawnLocations.get(w);
        if (listToAddTo == null)
        {
            listToAddTo = new ArrayList<Location>();
            itemSpawnLocations.put(w, listToAddTo);
        }
        listToAddTo.add(location);
    }

    protected static void forgetPlayerSpawnLocations(World w)
    {
        playerSpawnLocations.remove(w);
    }

    protected static void forgetItemSpawnLocations(World w)
    {
        if (itemSpawnLocations.containsKey(w))
        {
            itemSpawnLocations.remove(w);
        }
    }

    public static boolean hasValidPlayerSpawnLocations(World w)
    {
        return playerSpawnLocations.containsKey(w) && playerSpawnLocations.get(w).size() > 0;
    }

    public static ArrayList<Location> getPlayerSpawnLocations(World w)
    {
        return playerSpawnLocations.get(w);
    }

    public static ArrayList<Location> getItemSpawnLocations(World w)
    {
        return itemSpawnLocations.get(w);
    }

    public static void setBiome(World w, Biome type)
    {
        for (int i = -SEARCH_DISTANCE; i <= SEARCH_DISTANCE; i++)
        {
            for (int k = -SEARCH_DISTANCE; k <= SEARCH_DISTANCE; k++)
            {
                w.setBiome(i, k, type);
            }
        }
    }


    /**
     * Function: clearEntities
     * Purpose: To clear entities from the world and tasks that are directly related to entities
     *
     * @param w - The world for which to clear entities
     */
    protected static void clearEntities(World w)
    {
        if (isSmashWorld(w))
        {
            MinePlacer.clearAllBlocks(w);
            SmashOrbTracker.removePossibleOrb(w);
            for (final Entity entity : w.getEntities())
            {
                if (MonsterEgg.getMonsters().contains(entity.getType()))
                {
                    MonsterEgg.cancelMonsterKillerTasks(w);
                }
                else if (entity instanceof Item)
                {
                    if (entity.getLocation().getBlock().getType().equals(SmashItemManager.MATERIAL_FOR_ITEM_DROPS))
                    {
                        Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable() {
                            public String call()
                            {
                                entity.getLocation().getBlock().setType(Material.AIR);
                                return "";
                            }
                        });
                    }
                }
                if (!(entity instanceof Player))
                {
                    entity.remove();
                }
            }
        }
    }

    public static World getBestSmashWorld(Player p)
    {
        return WorldType.SMASH.getBestWorld(p);
    }

    public static int getNextTourneyLevel(int tourneyLevel)
    {
        tourneyLevel++;
        if (tourneyLevel > SmashStatTracker.HIGHEST_TOURNEY_LEVEL)
        {
            return SmashStatTracker.LOWEST_TOURNEY_LEVEL;
        }
        return tourneyLevel;
    }

    /**
     * Function: gameHasEnded
     * Purpose: To determine if a Smash game is in an end game state
     * @param w - The world to check for being over
     * @return - True if the game has ended in the given world (note that this will usually not be the case)
     */
    public static boolean gameHasEnded(World w)
    {
        return endGameTasks.containsKey(w);// gameHasStarted(w) && getScoreboardTitle(w).equals(FINAL_SCORES);
    }

    /**
     * Function: cancelEndGameTask
     * Purpose: To deactivateFinalSmash the task that announces winners and finally restarts the world
     *
     * @param w - The world for which to deactivateFinalSmash the end-game task
     */
    protected static void cancelEndGameTask(World w)
    {
        if (endGameTasks.containsKey(w))
        {
            Bukkit.getScheduler().cancelTask(endGameTasks.get(w));
            endGameTasks.remove(w);
        }
    }

    public static void scheduleEndGameTask(World w, int i)
    {
        endGameTasks.put(w, i);
    }

    /**
     * Function: gameHasStarted
     * Purpose: To determine if a Smash game has started
     * @param w - The world which we would like to check for being started
     * @return - True if the game has started (even if it has ended)
     */
    public static boolean gameHasStarted(World w)
    {
        return startedGames.contains(w);// hasScoreboard(w) && getStartTime(w) == 0;// && scoreboardManager.getTimeRemaining(w) != -1; //&& !getScoreboardManager().getScoreboardTitle(w).equals(FINAL_SCORES);
    }

    public static boolean gameIsInProgress(World w)
    {
        return gameHasStarted(w) && !gameHasEnded(w);
    }

    public static void setGameStarted(World w, boolean yes)
    {
        boolean st = startedGames.contains(w);
        if (!st && yes)
        {
            startedGames.add(w);
        }
        else if (st && !yes)
        {
            startedGames.remove(w);
        }
    }

    public static void forgetIfGameIsStarted(World w)
    {
        if (startedGames.contains(w))
        {
            startedGames.remove(w);
        }
    }

    private void addMotdMessage(String message)
    {
        WorldManager.motdMessages.add(loggerPrefix() + ChatColor.AQUA + message);
    }

    /**
     * Function:
     * Purpose:
     *
     * @param sender
     * @param cmd
     * @param label
     * @param args
     * @return
     */
    public boolean onCommand(CommandSender sender, Command cmd, final String label, String[] args)
    {
        if (matchesCommand(label, FIND_CMD))
        {
            World w;
            if (args.length > 0)
            {
                if (args[0].equalsIgnoreCase("help"))
                {
                    return false;
                }
                if (Bukkit.getPlayer(args[0]) == null)
                {
                    sender.sendMessage(ChatColor.RED + args[0] + " does not appear to be online.");
                    return true;
                }
                Player p = Bukkit.getPlayer(args[0]);
                w = p.getWorld();
                WorldType type = WorldType.getWorldType(w);
                WorldStatus status = type.getWorldStatus(w, p);

                String statusMessage = "";
                if (status != null)
                {
                    statusMessage = " which is " + ChatColor.stripColor(status.getMessage()).toLowerCase() + ChatColor.GREEN + ".";
                }
                sender.sendMessage(ChatColor.GREEN + p.getName() + " is in " + type.getDisplayName(w) + ChatColor.GREEN + " (" + w.getName() + ")" + statusMessage);
            }
            else if (!(sender instanceof Player))
            {
                sender.sendMessage(ChatColor.RED + "You are not in game!");
            }
            else
            {
                w = ((Player) sender).getWorld();
                sender.sendMessage(ChatColor.GREEN + "You are in " + WorldType.getWorldType(w).getDisplayName(w) + ChatColor.GREEN + " (" + w.getName() + ").");
            }
            return true;
        }
        else if (matchesCommand(label, DMG_CMD))
        {
            if (args.length == 0)
            {
                sender.sendMessage(ChatColor.RED + "You must specify who to damage!");
            }
            else if (Bukkit.getPlayer(args[0]) == null)
            {
                sender.sendMessage(ChatColor.RED + "Player not found!");
            }
            else
            {
                short damage;
                if (args.length < 2)
                {
                    damage = 50;
                }
                else
                {
                    try
                    {
                        damage = Short.valueOf(args[1]);
                    }
                    catch (NumberFormatException e)
                    {
                        sender.sendMessage(ChatColor.RED + "Invalid damage amount!");
                        return true;
                    }
                }
                SmashAttackManager.addDamage(Bukkit.getPlayer(args[0]), damage, false);
            }
            return true;
        }
        else if (matchesCommand(label, SET_DEATHMATCH_PREFERENCE_CMD))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage(loggerPrefix() + "You are not a player, therefore you cannot change your deathmatch preference!");
            }
            else
            {
                Player p = (Player)sender;
                String setting = WorldType.getDeathMatchPreference(p);
                Scanner s = new Scanner(setting);
                boolean preference = s.nextBoolean();
                int livesPreference = s.nextInt();

                boolean newPreference;
                int newLivesPreference;

                if (args.length == 0)
                {
                    newPreference = !preference;
                    newLivesPreference = livesPreference;
                }
                else
                {
                    if (args.length > 1)
                    {
                        try
                        {
                            newLivesPreference = Integer.valueOf(args[1]);
                            if (newLivesPreference == 0 || newLivesPreference > MAX_LIVES)
                            {
                                throw new NumberFormatException();
                            }
                        }
                        catch (NumberFormatException e)
                        {
                            return false;
                        }
                    }
                    else
                    {
                        newLivesPreference = livesPreference;
                    }

                    if (Plugin.isTrue(args[0]))
                    {
                        newPreference = true;
                    }
                    else if (Plugin.isFalse(args[0]))
                    {
                        newPreference = false;
                    }
                    else
                    {
                        return false;
                    }
                }
                WorldType.setDeathMatchPreference(p, newPreference, newLivesPreference);
            }
            return true;
        }
        return false;
    }
}