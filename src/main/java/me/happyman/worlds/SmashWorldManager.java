package me.happyman.worlds;

import me.happyman.SmashItemDrops.ItemDropManager;
import me.happyman.SmashItemDrops.Mine;
import me.happyman.SmashItemDrops.MonsterEgg;
import me.happyman.SmashItemDrops.SmashOrb;
import me.happyman.Listeners.HitData;
import me.happyman.Listeners.SmashItemManager;
import me.happyman.SmashKitMgt.SmashKitManager;
import me.happyman.commands.CommandVerifier;
import me.happyman.commands.SmashManager;
import me.happyman.utils.DirectoryType;
import me.happyman.utils.SmashEntityTracker;
import me.happyman.utils.SmashStatTracker;
import org.apache.commons.io.FileUtils;
import org.bukkit.*;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;

import static me.happyman.worlds.SmashScoreboardManager.*;

public class SmashWorldManager extends CommandVerifier implements CommandExecutor
{
    public static final long EXPIRATION_MINUTES = 120; //The minutes that a player's tournament level lasts
    //You probable shouldn't change these ones.
    public static final String SMASH_WORLD_PREFIX = "Smash-"; //The prefix of world folder names
    public static final String GOTO_WORLD_CMD = "goto"; //Sends the sender to the world if that's okay
    public static final int SEARCH_DISTANCE = 100; //The maximum distance that will be searched (in blocks) for spawn locations
    private static final int TOURNEY_GAME_TIME = 480; //The seconds in a tourney game.
    protected static final int TOURNEY_WAIT_TIME = (int)(Math.round(TOURNEY_GAME_TIME/6)); //The number of seconds before an initialized tournament game will try to start
    private static final int TOURNEY_PLAYER_REQUIREMENT = 4; //The minimum players that must join before a tournament game will start
    private static final int FREE_GAME_TIME = 300; //The seconds in a freeplay game
    protected static final int FREE_WAIT_TIME = (int)(Math.round(FREE_GAME_TIME/6)); //The number of seconds before an initialized freeplay game will start
    private static final int FREE_PLAYER_REQUIREMENT = 2; //The minimum players that must join before a freeplay game will start
    private static final int END_GAME_TIME = 10; //The amount of time before a world resets after a Smash game ends
    private static final int DEATH_DISTANCE = 150; //The distance from the center at which players will be assumed to be KO'ed and a ping sound will play
    private static final int MAX_BELOW = 30; //The distance below the center which will count as a KO
    private static final int KILLER_DELAY = 7; //The ticks between each check for a player being outside the KO zone
    private static final int MOTD_PERIOD = 240; //The period between a random motd message being sent to non-started Smash worlds
    private static final int MAX_LIVES = 10; //The max lives a player can prefer for deathmatch games
    private static final float READY_PERCENT_TO_START = 0.65F;
    private static final String SMASH_TEMPLATE_PREFIX = "SmashWorld"; //The prefix of map world folder names
    private static final String PLAYER_SPAWN_FILENAME = "PlayerSpawnLocations"; //The name of the player spawn location file which stores player spawn locations (without the extension)
    private static final String ITEM_SPAWN_FILENAME = "ItemSpawnLocations"; //The name of the item spawn location file which stores item spawn locations...
    private static final int MAX_FREEPLAY_WORLDS = 30; //The max number of Freeplay games that can be created
    private static final int MAX_TOURNEY_WORLDS = 30; //The max number of Tournament games that can be created
    private static final String SMASH_CMD = "smash"; //Excecuting this command will open the Smash gui if that's okay
    private static final String DELETE_WORLD_CMD = "deleteworld"; //This deletes a world
    private static final String START_CMD = "start"; //This starts the game
    private static final String LEAVE_CMD = "leave"; //This causes you to leave the game you're in
    private static final String LIST_MAP_CMD = "listmaps"; //This lists the maps that you can use when creating a world
    private static final String LIST_WORLD_CMD = "listworlds"; //This lists all Smash worlds
    private static final String RESET_CMD = "reset"; //This resets a Smash game (don't use it during a game unless you're a jerk or there's a bug)
    private static final String GET_WORLD_CMD = "getworld"; //Gets the world you're in (or another player)
    private static final String DMG_CMD = "adddamage"; //Adds DMG points to a player (not hearts, DMG!)
    private static final String SET_DEATHMATCH_PREFERENCE_CMD = "dm"; //The command used to set your deathmatch preference
    private static final String GET_MODE_CMD = "mode"; //Gets the mode of the Smash world you're in (point-based or deathmatch)
    private static final String SPECTATE_COMMAND = "spectate";
    private static final String READY_UP_CMD = "ready";
    private static final String VOTE_KICK_CMD = "votekick";
    private static final String END_GAME_CMD = "end";
    private static final int SPAWN_HEIGHT = 135; //The height which is used for KO zone calculations
    private static final int SEARCH_HEIGHT = 30; //The maximum height to search for spawn locations
    private static final int DOUBLE_JUMP_LAND_DELAY = 1; //The delay in ticks before your items will be recharged
    private static final int REFUEL_COOLDOWN = 12; //The delay in ticks before your items can recharge again upon landing
    protected static World FALLBACK_WORLD; //The world which is used for emergencies
    //Can you tell I like HashMaps?
    private static List<World> startGameOverrides; //This is used when a staff member wants to start a game after a delay
    private static HashMap<World, Integer> timeRemainingTasks; //The task for keeping track of the time remaining in a started Smash game
    private static HashMap<World, Integer> countdownTasks; //The task for keeping track of time til start in a Smash game
    private static HashMap<World, Integer> itemSpawnTasks; //The tasks for spawning items at random locations in a Smash game
    private static HashMap<World, Integer> endGameTasks; //The task for ending a Smash game
    private static HashMap<World, Integer> killerTasks; //The task for checking if people are outside the KO zone
    private static HashMap<World, Integer> doubleJumpTasks; //The task for checking if people are on the ground for recharging their items
    private static HashMap<World, ArrayList<Location>> playerSpawnLocations; //All the locations where players can spawn
    private static HashMap<World, ArrayList<Location>> itemSpawnLocations; //All the locations where items can spawn
    private static HashMap<Player, Location> lastPlayerLocations; //The last location of players before they joined a Smash game
    private static HashMap<Player, Integer> ticksTilNextRefuel; //The ticks before a player can refuel again after resetting their time till refuel
    private static HashMap<Integer, Integer> tourneyLevels; //The corresponding tournament levels of tournament world numbers
    private static ArrayList<String> worldsBeingDeleted; //The list of names of worlds that are being deleted and therefore should be left alone
    private static ArrayList<World> startedGames;
    private static Random r;
    private ArrayList<String> motdMessages; //The list of messages that can be displayed after each motd period (just one, please)

    /**
     * Constructor: SmashWorldManager
     * Purpose: To initialize the data structures and perform startup tasks such as
     * loading world and player data and stuff like that
     */
    public SmashWorldManager()
    {
        super(plugin);
        new SmashWorldListener();
        new SmashStatTracker(this, plugin);
        new SmashScoreboardManager();

        plugin.setExecutor(SMASH_CMD, this);
        plugin.setExecutor(START_CMD, this);
        plugin.setExecutor(LEAVE_CMD, this);
        plugin.setExecutor(DELETE_WORLD_CMD, this);
        plugin.setExecutor(LIST_MAP_CMD, this);
        plugin.setExecutor(LIST_WORLD_CMD, this);
        plugin.setExecutor(RESET_CMD, this);
        plugin.setExecutor(GET_WORLD_CMD, this);
        plugin.setExecutor(DMG_CMD, this);
        plugin.setExecutor(GOTO_WORLD_CMD, this);
        plugin.setExecutor(SET_DEATHMATCH_PREFERENCE_CMD, this);
        plugin.setExecutor(GET_MODE_CMD, this);
        plugin.setExecutor(SPECTATE_COMMAND, this);
        plugin.setExecutor(READY_UP_CMD, this);
        plugin.setExecutor(VOTE_KICK_CMD, this);
        plugin.setExecutor(END_GAME_CMD, this);
        plugin.setExecutor(SmashWorldInteractor.CREATE_WORLD_CMD, this);

        itemSpawnLocations = new HashMap<World, ArrayList<Location>>();
        playerSpawnLocations = new HashMap<World, ArrayList<Location>>();
        killerTasks = new HashMap<World, Integer>();
        startGameOverrides = new ArrayList<World>();
        timeRemainingTasks = new HashMap<World, Integer>();
        itemSpawnTasks = new HashMap<World, Integer>();
        countdownTasks = new HashMap<World, Integer>();
        lastPlayerLocations = new HashMap<Player, Location>();
        endGameTasks = new HashMap<World, Integer>();
        doubleJumpTasks = new HashMap<World, Integer>();
        ticksTilNextRefuel = new HashMap<Player, Integer>();
        tourneyLevels = new HashMap<Integer, Integer>();
        worldsBeingDeleted = new ArrayList<String>();
        motdMessages = new ArrayList<String>();
        startedGames = new ArrayList<World>();
        r = new Random();

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
        {
            public void run()
            {
                /*Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
                    public void run() {
                        //Bukkit.broadcastMessage("" + source.getSecond());
                    }
                }, 25, 20);*/

                Collection<Player> playerList = (Collection<Player>) Bukkit.getOnlinePlayers();
                for (int i = 0; i <= SmashWorldInteractor.HIGHEST_TOURNEY_LEVEL; i++)
                {
                    SmashWorldInteractor.getOpenWorldGuis().put(i, new ArrayList<Player>());
                }
                for (File worldFile : listSmashFolders())
                {
                    Bukkit.getServer().createWorld(new WorldCreator(worldFile.getName()));
                }
                for (World w : Bukkit.getWorlds())
                {
                    if (isSmashWorld(w))
                    {
                        loadWorld(w);
                        clearEntities(w);
                    }
                    else
                    {
                        for (Player p : w.getPlayers())
                        {
                            lastPlayerLocations.put(p, p.getLocation());
                        }
                    }
                    for (Player p : w.getPlayers())
                    {
                        SmashWorldInteractor.setSpectatorMode(p, w.getPlayers(), false);
                    }
                }
                for (Player p : playerList)
                {
                    SmashWorldInteractor.getTabList(p).clearCustomTabs();
                    SmashWorldInteractor.giveSmashGuiOpener(p);
                    if (SmashWorldInteractor.hasWorldGuiOpen(p))
                    {
                        SmashWorldInteractor.getOpenWorldGuis().get(SmashStatTracker.getTourneyLevel(p)).add(p);
                        SmashWorldInteractor.refreshWorldGui(p);
                    }
                    /*if (!isSmashWorld(p.getWorld()))
                    {
                        resetToSmashGuiOpener(p);
                    }*/
                }
            }
        }, 1);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run() {
                setFallbackWorld();
            }
        }, 40);

        //motdMessages.add(plugin.loggerPrefix() + ChatColor.AQUA + "I don't sell anything! If you'd like to donate, you can show your support at " + ChatColor.UNDERLINE + "http://patreon.com/happyman." + ChatColor.RESET + "" + ChatColor.AQUA + "All donations are greatly appreciated!");
        addMotdMessage("I need maps! Send any map submissions to me at happyman@finalsmash.us");
        addMotdMessage("Did you know you can /dm to set your gamemode preference?");
        addMotdMessage("Special thanks to B1izzard10 and TheCyberCreeper for helping Smash bugs!");
        addMotdMessage("HappyMan's email is happyman@finalsmash.us if you want to contact him for " + ChatColor.ITALIC + "any" + ChatColor.RESET + "" + ChatColor.AQUA + " reason");
        addMotdMessage("Did you know that you can afk in a world as long as you want since a sound is played when a player joins?");
        addMotdMessage("HappyMan recommends that you use hotkeys to switch between your items quickly.");

        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable() {
            public void run() {
                String message = motdMessages.get(r.nextInt(motdMessages.size()));
                for (World w : Bukkit.getWorlds())
                {
                    if (isSmashWorld(w) && !gameHasStarted(w))
                    {
                        SmashWorldInteractor.sendMessageToWorld(w, message, false);
                    }
                }
            }
        }, MOTD_PERIOD*10, MOTD_PERIOD*20);
    }

    public static void performDisable()
    {
        for (Player p : Bukkit.getOnlinePlayers())
        {
            SmashWorldInteractor.getTabList(p).clearCustomTabs();
        }
    }

    /**
     * Function: isStartingSoon
     * Purpose: To determine if a game is about start. There's no check for the game already being started, so keep that in mind.
     * @param w - The world we would like to check for being about to start
     * @return - True if someone did /start or if there are enough players for the game to start
     */
    protected static boolean isStartingSoon(World w)
    {
        return startGameOverrides.contains(w) || getStartRequirement(w) != null && SmashWorldInteractor.getLivingPlayers(w).size() >= getStartRequirement(w);
    }

    protected static void removeStartOverride(World w)
    {
        if (startGameOverrides.contains(w))
        {
            startGameOverrides.remove(w);
        }
    }

    /**
     * Function: setFallbackWorld
     * Purpose: Sets the fallback world to the first world in the Bukkit world list
     */
    private static void setFallbackWorld()
    {
        FALLBACK_WORLD = Bukkit.getWorlds().get(0);
    }

    /**
     * Function: getFallbackWorld
     * Purpose: To get the fallback world of the server
     * @return - The fallback world
     */
    protected static World getFallbackWorld()
    {
        return FALLBACK_WORLD;
    }

    /**
     * Function: getLastPlayerLoction
     * Purpose: To determine the loction from which a player logged into a Smash world
     * @param p - The player for which we want to know his last location
     * @return - The location that the player was at before joining a Smash game
     */
    protected static Location getLastPlayerLocation(Player p)
    {
        if (lastPlayerLocations.containsKey(p))
        {
            return lastPlayerLocations.get(p);
        }
        return null;
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
     * Purpose: To determine from a world name if the world is a Smash world
     * @param worldName - The name of the world which we want to know if it matches a Smash world name
     * @return - True if the world name is that of a Smash world
     */
    private static boolean isSmashWorld(String worldName)
    {
        return worldName.startsWith(SMASH_WORLD_PREFIX);
    }

    /**
     * Function: isTourneyWorld
     * Purpose: To determine if the given world is a Tournament world
     * @param w - The world which we would like to check for being a tournament world
     * @return - True if the world is a tournament world
     */
    public static boolean isTourneyWorld(World w)
    {
        return isTourneyWorld(w.getName());
    }

    /**
     * Function: isTourneyWorld
     * Purpose: The real check - sees if the world name is that of a tournament world
     * @param worldName - The world name which we want to examen
     * @return - True if the given world name corresponds to a Tournament world
     */
    protected static boolean isTourneyWorld(String worldName)
    {
        return worldName.length() >= SMASH_WORLD_PREFIX.length() + SmashWorldInteractor.TOURNEY_INDICATOR.length()
                && worldName.substring(SMASH_WORLD_PREFIX.length(), SMASH_WORLD_PREFIX.length() + SmashWorldInteractor.TOURNEY_INDICATOR.length()).equals(SmashWorldInteractor.TOURNEY_INDICATOR);
    }

    /**
     * Function: getJumpLandRefuelRunnable
     * Purpose: To return a runnable that will check if a player is on the ground, and if so will refuel them if they aren't cooling down on their item refuels
     * @param w - The world for which we would like to get the item refuel runnable
     * @return - The Runnable that will check for players being on the ground and recharging their items
     */
    private static Runnable getJumpLandRefuelRunnable(final World w)
    {
        return new Runnable()
        {
            public void run()
            {
                if (isSmashWorld(w))
                {
                    for (Player p : SmashWorldInteractor.getLivingPlayers(w))
                    {
                        if (((Entity)p).isOnGround() //|| p.getLocation().getBlock().getRelative(0, -1, 0).getType().equals(Material.AIR)
                            && p.getLocation().getY()%0.5 < 0.04)
                        {
                            //Bukkit.broadcastMessage("restoring fuel");
                            SmashKitManager.restoreAllUsagesAndCharges(p, true);
                           // SmashKitManager.getSelectedKit(p).getRocket().restoreUsages(p);
                        }
                        decrementTimeTilRefuel(p);
                    }
                }
            }
        };
    }

    /**
     * Function: resetTimeTilRefuel
     * Purpose: To reset a player's time til they can recharge their items so that they won't be able to recharge after a certain amount of time
     * @param p - The player who we don't want to have items recharged for a time
     */
    public static void resetTimeTilRefuel(Player p)
    {
        ticksTilNextRefuel.put(p, REFUEL_COOLDOWN);
    }

    /**
     * Function: canRefuelNow
     * Purpose: To determine if a player can have his items recharged
     * @param p - The player who we want to check for being allowed to have his items recharged
     * @return - True if the player can have his items recharged
     */
    public static boolean canRefuelNow(Player p)
    {
        if (!ticksTilNextRefuel.containsKey(p))
        {
            return true;
        }
        else if (ticksTilNextRefuel.get(p) == 0)
        {
            ticksTilNextRefuel.remove(p);
            return true;
        }
        p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 1, 1));
        return false;
    }

    /**
     * Function: decrementTimeTilRefuel
     * Purpose: Decrements the time til a player can refuel by one tick. How exciting.
     * @param p - The player for whom we would like to decrement refuel time
     */
    private static void decrementTimeTilRefuel(Player p)
    {
        if (ticksTilNextRefuel.containsKey(p))
        {
            if (ticksTilNextRefuel.get(p) > 0)
            {
                ticksTilNextRefuel.put(p, ticksTilNextRefuel.get(p) - 1);
            }
            else
            {
                ticksTilNextRefuel.remove(p);
            }
        }
    }

    /**
     * Function: getKillerRunnable
     * Purpose: Gets the runnable that checks for a player being outside the KO zone, and if they are and the game has started, excecutes the necessary respawn procedures.
     * @param w - The world for which we would like to get such a USEFUL runnable!!!
     * @return - The runnable for the world which will is used for Smash games that have or haven't started, but is most important for started games
     */
    private static Runnable getKillerRunnable(final World w)
    {
        return new Runnable()
        {
            public void run()
            {
                if (isSmashWorld(w)) //This could probably be removed fine
                {
                    for (final Player p : SmashWorldInteractor.getLivingPlayers(w))
                    {
                        boolean superFar = p.getLocation().distance(p.getWorld().getSpawnLocation()) > DEATH_DISTANCE;
                        if ((superFar || p.getLocation().getY() < SPAWN_HEIGHT - MAX_BELOW || p.getLocation().getY() < -50) && !SmashWorldInteractor.isDead(p))
                        {
                            SmashWorldInteractor.setIsDead(p, true);
                            int tpDelay = 0;
                            Vector v = p.getVelocity();
                            if (superFar)
                            {
                                SmashWorldInteractor.playSoundToPlayers(p, w.getPlayers(), Sound.ORB_PICKUP, 0.7F, 0.95F);
                                tpDelay = 15;
                            }
                            else if (SmashManager.getMagnitude(v.getX(), v.getY(), v.getZ()) > 3.2)
                            {
                                SmashWorldInteractor.playSoundToPlayers(p, p.getLocation(), w.getPlayers(), Sound.EXPLODE, (float)1, (float)0.04, 1, 6);
                                SmashWorldInteractor.playSoundToPlayers(p, w.getPlayers(), Sound.BLAZE_DEATH, (float)0.1, (float)0.05);
                                tpDelay = 20;
                            }
                            Runnable r = SmashWorldInteractor.getRespawnRunnable(p);
                            plugin.startRunnable(r);
                            plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, r, tpDelay);

                            if (gameHasStarted(w) && !gameHasEnded(w))
                            {
                                //Get the KO message****
                                String koMessage = "null";
                                Random rn = new Random();
                                String deadPlayerName = ChatColor.GOLD + p.getName(); //+ getSelectedKit(deadPlayer)
                                if (!SmashManager.hasLastHitter(p.getName()))
                                {
                                    switch(rn.nextInt(1))
                                    {
                                        case 0: koMessage = deadPlayerName + ChatColor.RESET + " fell out of the map!";
                                    }
                                }
                                else
                                {
                                    HitData lastHitter = SmashManager.getLastHitter(p.getName());
                                    String killerName =  lastHitter.killerName;
                                    String weaponName = lastHitter.weaponName;
                                    String killerKit = "";
                                    if (Bukkit.getPlayer(killerName) != null)
                                    {
                                        killerKit = "(" + SmashKitManager.getSelectedKitName(Bukkit.getPlayer(killerName)) + ")";
                                    }
                                    String deadKit = "";
                                    deadKit = "(" + SmashKitManager.getSelectedKitName(p) + ")";
                                    switch(rn.nextInt(1))
                                    {
                                        case 0: koMessage = deadPlayerName + deadKit + ChatColor.RESET + " was KO'd by " + ChatColor.GREEN +  killerName + killerKit + "'s " + ChatColor.DARK_AQUA + weaponName + ChatColor.RESET + ".";
                                    }
                                }
                                //**************

                                SmashWorldInteractor.sendMessageToWorld(w, ChatColor.GOLD + koMessage);
                                SmashScoreboardManager.setPlayerScoreValue(p, SmashScoreboardManager.getPlayerScoreValue(p) - 1);

                                if (SmashManager.hasLastHitter(p.getName()))
                                {
                                    String lastHitter = SmashManager.getLastHitterName(p.getName());
                                    if (!lastHitter.equals(p.getName()) && !SmashWorldInteractor.isDeathMatchWorld(w))
                                    {
                                        SmashScoreboardManager.setPlayerScoreValue(lastHitter, w, SmashScoreboardManager.getPlayerScoreValue(lastHitter, w) + 1);
                                    }
                                }

                                if (SmashWorldInteractor.isDeathMatchWorld(w))
                                {
                                    if (SmashScoreboardManager.getPlayerScoreValue(p) == 0)
                                    {
                                        SmashWorldInteractor.performDeathMatchDeath(p, w, false);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    /**
     * Function: loadSpawns
     * Purpose: To load where on earth the spawn locations are. This is used when a world is created or the plugin is reloaded. Who knows, maybe one day it'll be used for a greater purpose.
     * As of 1/23/2017 these locations were pumpkins for player spawn locations and melons for item spawn locations
     * @param w - The world for which we would like to load in spawn locations
     */
    private static void loadSpawns(World w)
    {
        if (w == null)
        {
            plugin.sendErrorMessage("Error! Could not find null world when finding item locations for it.");
            return;
        }

        if (!itemSpawnLocations.containsKey(w))
        {
            itemSpawnLocations.put(w, new ArrayList<Location>());
        }
        if (!playerSpawnLocations.containsKey(w))
        {
            playerSpawnLocations.put(w, new ArrayList<Location>());
        }

        if (plugin.hasFile(DirectoryType.ROOT, w.getName(), PLAYER_SPAWN_FILENAME) && plugin.hasFile(DirectoryType.ROOT, w.getName(), ITEM_SPAWN_FILENAME))
        {
            for (List<String> line : plugin.getAllDataSimple(DirectoryType.ROOT, w.getName(), PLAYER_SPAWN_FILENAME))
            {
                double[] coords = new double[line.size()];
                for (int i = 0; i < line.size(); i++)
                {
                    try
                    {
                        coords[i] = Double.valueOf(line.get(i));
                    }
                    catch (NumberFormatException e)
                    {
                        plugin.sendErrorMessage("Error! Tried to read " + PLAYER_SPAWN_FILENAME + ", but one of the coords had a bad format!");
                    }
                }
                playerSpawnLocations.get(w).add(new Location(w, coords[0], coords[1], coords[2], (float)coords[3], 0));
            }
            for (List<String> line : plugin.getAllDataSimple(DirectoryType.ROOT, w.getName(), ITEM_SPAWN_FILENAME))
            {
                int[] coords = new int[line.size()];
                for (int i = 0; i < line.size(); i++)
                {
                    try
                    {
                        coords[i] = Integer.valueOf(line.get(i));
                    }
                    catch (NumberFormatException e)
                    {
                        plugin.sendErrorMessage("Error! Tried to read " + ITEM_SPAWN_FILENAME + ", but one of the coords had a bad format!");
                    }
                }
                itemSpawnLocations.get(w).add(new Location(w, coords[0] + 0.5, coords[1] + 0.6, coords[2] + 0.5));
            }
        }
        else if (plugin.hasFile(DirectoryType.ROOT, w.getName(), PLAYER_SPAWN_FILENAME) ^ plugin.hasFile(DirectoryType.ROOT, w.getName(), ITEM_SPAWN_FILENAME))
        {
            plugin.sendErrorMessage("Error! We had one of the files for loading spawns, but we didn't have the other one!");
        }
        else
        {
            for (int x = -SEARCH_DISTANCE; x <= SEARCH_DISTANCE; x++)
            {
                for (int z = -SEARCH_DISTANCE; z <= SEARCH_DISTANCE; z++)
                {
                    for (int y = SPAWN_HEIGHT - SEARCH_HEIGHT; y <= SPAWN_HEIGHT + SEARCH_HEIGHT; y++)
                    {
                        Block blockToCheck = w.getBlockAt(x, y, z);
                        boolean foundSpawnBlock = false;
                        if (blockToCheck.getType().equals(Material.PUMPKIN))
                        {
                            Location spawnLocation = new Location(w, x + 0.5, y, z + 0.5, 0, 0);
                            spawnLocation.setDirection(SmashManager.getUnitDirection(spawnLocation, w.getSpawnLocation()));
                            spawnLocation.setPitch(0);
                            playerSpawnLocations.get(w).add(spawnLocation);
                            foundSpawnBlock = true;
                        }
                        else if (blockToCheck.getType().equals(Material.MELON_BLOCK))
                        {
                            itemSpawnLocations.get(w).add(new Location(w, x + 0.5, y + 0.6, z + 0.5));
                            foundSpawnBlock = true;
                        }
                        if (foundSpawnBlock)
                        {
                            blockToCheck.setType(Material.AIR);

                            Block blockOn = blockToCheck.getRelative(0, -1, 0);
                            if (blockOn.getType().equals(Material.DIRT))
                            {
                                blockOn.setType(Material.GRASS);
                            }
                        }
                    }
                }
            }
            if (playerSpawnLocations.size() > 0 && itemSpawnLocations.get(w).size() > 0)
            {
                List<String> dataList = new ArrayList<String>();
                for (int i = 0; i < playerSpawnLocations.get(w).size(); i++)
                {
                    Location l = playerSpawnLocations.get(w).get(i);
                    dataList.add(String.format("l%1$d: %2$.2f %3$.2f %4$.2f %5$.2f", i, l.getX(), l.getY(), l.getZ(), l.getYaw()));
                }
                plugin.printLinesToFile(DirectoryType.ROOT, w.getName(), PLAYER_SPAWN_FILENAME, dataList);

                dataList = new ArrayList<String>();
                for (int i = 0; i < itemSpawnLocations.get(w).size(); i++)
                {
                    Location l = itemSpawnLocations.get(w).get(i);
                    dataList.add(String.format("l%1$d: %2$d %3$d %4$d", i, l.getBlockX(), l.getBlockY(), + l.getBlockZ()));
                }
                plugin.printLinesToFile(DirectoryType.ROOT, w.getName(), ITEM_SPAWN_FILENAME, dataList);
            }
            else
            {
                plugin.sendErrorMessage("Error! We neither had valid spawn location files nor had the means to create it in world " + w.getName() + ". Deleting the world.");
                deleteWorld(w);
            }
        }
    }

    /**
     * Function: getItemSpawnRunnable
     * Purpose: To get the runnable which will spawn an item (or not) depending on parameters which you probably shouldn't worry about because you're probably too stupid to figure it out anyway.
     * @param w - The world for which we would like to spawn an item drop... or not
     * @return - The Runnable for spawning an item in a world
     */
    private static Runnable getItemSpawnRunnable(final World w)
    {
        return new Runnable()
        {
            public void run()
            {
                if (isSmashWorld(w) && gameHasStarted(w) && !gameHasEnded(w))
                {
                    if (getRandomBoolean(w))
                    {
                        spawnItemDropOrFinalSmash(w);
                    }
                }
                else
                {
                    plugin.sendErrorMessage("Error! Tried to spawn item at a bad time!");
                }
            }
        };
    }

    private static void spawnItemDropOrFinalSmash(final World w)
    {
        if (itemSpawnLocations.size() > 0)
        {
            Location selectedLocation = itemSpawnLocations.get(w).get(r.nextInt(itemSpawnLocations.get(w).size())).clone();
            if (r.nextFloat() < SmashOrb.SMASH_ORB_SPAWN_CHANCE && SmashOrb.canCreateOrb(w))
            {
                selectedLocation = selectedLocation.add(0, 7, 0);
                final Location finalLocation = selectedLocation;
                SmashOrb.createOrb(finalLocation);
            }
            else
            {
                final Location finalLocation = selectedLocation;
                Block block = selectedLocation.getBlock();
                if (block.getType().equals(Material.AIR))
                {
                    w.playSound(selectedLocation, Sound.ITEM_PICKUP, 1, 2);
                    Bukkit.getScheduler().callSyncMethod(plugin, new Callable() {
                        public String call() {
                            final Item item = w.dropItem(finalLocation, ItemDropManager.getRandomItemDrop());
                            //Bukkit.getPlayer("HappyMan").sendMessage("setting velocity of item drop");
                            item.setVelocity(new Vector().zero());
                            return "";
                        }
                    });
                    block.setType(SmashItemManager.MATERIAL_FOR_ITEM_DROPS);
                }
            }
        }
        else
        {
            plugin.sendErrorMessage("Error! Could not spawn item because we don't know of any item spawn locations!");
        }
    }

    /**
     * Function: getRandomBoolean
     * Purpose: To return a random boolean (true or false, for the uninitiated), which determines whether an item should be spawned or not based on several parameters.
     * @param w - The world for which we would like to determine if an item should be spawned
     * @return - True if an item should be spawned... there is randomness involved
     */
    private static boolean getRandomBoolean(World w)
    {
        return (new Random()).nextFloat() < SmashWorldInteractor.getLivingPlayers(w).size() * ItemDropManager.ITEMSPAWN_CHANCE_PER_PERIOD_PER_PLAYER;
    }

    /**
     * Function: gameHasStarted
     * Purpose: To determine if a Smash game has started
     * @param w - The world which we would like to check for being started
     * @return - True if the game has started (even if it has ended)
     */
    public static boolean gameHasStarted(World w)
    {
        return startedGames.contains(w);// SmashScoreboardManager.hasScoreboard(w) && SmashScoreboardManager.getStartTime(w) == 0;// && scoreboardManager.getTimeRemaining(w) != -1; //&& !getScoreboardManager().getScoreboardTitle(w).equals(FINAL_SCORES);
    }

    /**
     * Function: gameHasEnded
     * Purpose: To determine if a Smash game is in an end game state
     * @param w - The world to check for being over
     * @return - True if the game has ended in the given world (note that this will usually not be the case)
     */
    public static boolean gameHasEnded(World w)
    {
        return endGameTasks.containsKey(w);// gameHasStarted(w) && SmashScoreboardManager.getScoreboardTitle(w).equals(SmashScoreboardManager.FINAL_SCORES);
    }

    /**
     * Function: getStartRequirement
     * Purpose: Gets the number of players that is needed for a game to start naturally
     * @param w - The world for which we would like to determine how many players are needed
     * @return - The number of players that is needed for the game to start
     */
    protected static Integer getStartRequirement(World w)
    {
        if (isSmashWorld(w))
        {
            if (isTourneyWorld(w))
            {
                return TOURNEY_PLAYER_REQUIREMENT;
            }
            else
            {
                return FREE_PLAYER_REQUIREMENT;
            }
        }
        plugin.sendErrorMessage("Error! Tried to get the players required to start " + w.getName() + ", a non-smash world!");
        return null;
    }

    /**
     * Function: startGame
     * Purpose: To start a Smash game in a world
     *
     * @param w - The world of the game to be started
     */
    protected static void startGame(World w)
    {
        SmashWorldInteractor.determineDeathMatchPreference(w);
        if (SmashWorldInteractor.isDeathMatchWorld(w) && SmashWorldInteractor.getLivingPlayers(w).size() <= 1)
        {
            SmashWorldInteractor.sendMessageToWorld(w, ChatColor.RED + "This game didn't start because it was a deathmatch game with only one player.");
            SmashScoreboardManager.resetStartTime(w);
        }
        else if (SmashWorldInteractor.getLivingPlayers(w).size() == 0)
        {
            SmashWorldInteractor.sendMessageToWorld(w, ChatColor.RED + "This game didn't start because there was no one in it.");
            SmashScoreboardManager.resetStartTime(w);
        }
        else
        {
            if (!startedGames.contains(w))
            {
                startedGames.add(w);
            }
            SmashWorldInteractor.getReadyPlayers().remove(w);
            SmashWorldInteractor.rememberOriginalPlayerCount(w);
            SmashWorldInteractor.updateHitCooldown(w);
            removeStartOverride(w);
            cancelCountdown(w);
            SmashScoreboardManager.clearEntries(w);

            if (!SmashWorldInteractor.isDeathMatchWorld(w))
            {
                int gameTime = FREE_GAME_TIME;
                if (isTourneyWorld(w))
                {
                    gameTime = TOURNEY_GAME_TIME;
                }
                SmashScoreboardManager.setTimeRemaining(w, gameTime);

                Runnable r = getTimeRemainingRunnable(w);
                plugin.startRunnable(r);
                timeRemainingTasks.put(w, plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, r, 20, 20));
            }
            else
            {
                SmashScoreboardManager.setScoreboardTitle(w, ChatColor.GOLD + "" + ChatColor.BOLD + "Lives");
            }

            Runnable r1 = getItemSpawnRunnable(w);
            plugin.startRunnable(r1);
            if (!itemSpawnTasks.containsKey(w))
            {
                itemSpawnTasks.put(w, plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, r1, Math.round(1.0/(SmashWorldInteractor.getLivingPlayers(w).size() * ItemDropManager.ITEMSPAWN_CHANCE_PER_PERIOD_PER_PLAYER) * ItemDropManager.ITEMSPAWN_PERIOD / 2), ItemDropManager.ITEMSPAWN_PERIOD));
            }
            else
            {
                plugin.sendErrorMessage("We were already spawning itemDrops!");
            }

            for (Player p : w.getPlayers())
            {
                if (!SmashWorldInteractor.isInSpectatorMode(p))
                {
                    p.setAllowFlight(false);
                    //SmashScoreboardManager.addPlayerToScoreBoard(p);
                    SmashScoreboardManager.setPlayerScoreValue(p, SmashWorldInteractor.getDeathMatchLives(w));
                    SmashWorldInteractor.respawnTeleport(p);
                }
                if (SmashWorldInteractor.isDeathMatchWorld(w))
                {
                    p.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "The Game has begun!" + ChatColor.RESET + ChatColor.GREEN + " (The mode is " + ChatColor.RED +  "Deathmatch" + ChatColor.GREEN + "!)");
                }
                else
                {
                    p.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "The Game has begun!" + ChatColor.RESET + ChatColor.GREEN + " (The mode is " + ChatColor.BLUE + "points" + ChatColor.GREEN + "!)");
                }
                p.playSound(p.getLocation(), Sound.NOTE_PLING, 1F, 1.1F);
            }
            w.setPVP(true);
        }
    }

    protected static void endGame(World w)
    {
        endGame(w, null);
    }

    /**
     * Function: endGame
     * Purpose: To end a Smash game, announcing winners and recording statistics, among other things
     *
     * @param w - The world of the game to be ended
     */
    private static void endGame(final World w, String whoForcedClose)
    {
        w.setPVP(false);
        if (!gameHasEnded(w))
        {
            //************************ Determine winners
            final boolean isTourneyWorld = isTourneyWorld(w);
            final List<String> scoreboardPlayerList = new ArrayList<String>();
            for (Player p : w.getPlayers())
            {
                if (SmashWorldInteractor.isInSpectatorMode(p))
                {
                    SmashWorldInteractor.handleJoin(p, w, false, true);
                    SmashWorldInteractor.setSpectatorMode(p, w.getPlayers(), false);
                }
            }
            if (whoForcedClose != null && SmashWorldInteractor.isDeathMatchWorld(w))
            {
                SmashWorldInteractor.sendMessageToWorld(w, ChatColor.RED + "" + ChatColor.BOLD + "The game was forcefully ended by " + whoForcedClose + "!");
                restartWorld(w);
            }
            else
            {
                for (String p : SmashScoreboardManager.getScoreboardEntries(w))
                {
                    if ((!SmashWorldInteractor.isDeathMatchWorld(w) || SmashScoreboardManager.getPlayerScoreValue(p, w) > 0) && !scoreboardPlayerList.contains(ChatColor.stripColor(p)))
                    {
                        scoreboardPlayerList.add(ChatColor.stripColor(p));
                    }
                }
                List<Integer> scores = new ArrayList<Integer>();
                for (String p : SmashScoreboardManager.getScoreboardEntries(w))
                {
                    scores.add(SmashScoreboardManager.getPlayerScoreValue(p, w));
                }
                if (scores.size() > 0)
                {
                    Collections.sort(scores, Collections.reverseOrder());
                    int minContinueScore;
                    if (isTourneyWorld && getTourneyLevel(w) == SmashWorldInteractor.HIGHEST_TOURNEY_LEVEL)
                    {
                        minContinueScore = scores.get(0);
                    }
                    else
                    {
                        while ((!isTourneyWorld || isTourneyWorld && scores.size() > SmashScoreboardManager.getScoreboardEntries(w).size()* SmashWorldInteractor.TOURNEY_TOP_PERCENT/100 + 1) && scores.size() > 1)
                        {
                            scores.remove(scores.size() - 1);
                        }
                        minContinueScore = scores.get(scores.size() - 1);
                    }
                    for (int i = 0; i < scoreboardPlayerList.size(); i++)
                    {
                        if (SmashScoreboardManager.getPlayerScoreValue(scoreboardPlayerList.get(i), w) < minContinueScore)
                        {
                            scoreboardPlayerList.remove(i);
                            i--;
                        }
                    }
                }
                else
                {
                    plugin.sendErrorMessage("Error! There were no score entries on the scoreboard in " + w.getName());
                }

                //************************* Announce winners

                String endMessage;
                if (whoForcedClose != null)
                {
                    endMessage = ChatColor.RED + "" + ChatColor.BOLD + "The game was ended early by " + whoForcedClose + "!";
                }
                else if (SmashWorldInteractor.isDeathMatchWorld(w))
                {
                    endMessage = ChatColor.GREEN + "" + ChatColor.BOLD +  "GAME!";
                }
                else
                {
                    endMessage = ChatColor.GREEN + "" + ChatColor.BOLD +  "TIME!";
                }
                SmashWorldInteractor.sendMessageToWorld(w, endMessage);
                for (Player p : w.getPlayers())
                {
                    p.playSound(p.getLocation(), Sound.ENDERDRAGON_GROWL, 1F, 1F);
                    float eloScore = SmashStatTracker.getEloScore(p.getName());
                    p.sendMessage(ChatColor.GREEN + "Your new elo is " + Math.round(eloScore) + " (" + SmashStatTracker.eloChangeString(Math.round(eloScore - SmashStatTracker.getAndForgetOldElo(p.getName()))) + ChatColor.GREEN + ").");
                }

                String s = "";
                String hasHave = "have";
                String moveMoves = "advance";
                if (scoreboardPlayerList.size() == 1)
                {
                    hasHave = "has";
                    moveMoves = "advances";
                }

                String playersString;
                if (scoreboardPlayerList.size() > 0)
                {
                    playersString = scoreboardPlayerList.get(0);
                }
                else
                {
                    if (w.getPlayers().size() > 0)
                    {
                        playersString = w.getPlayers().get(0).getName();
                    }
                    else
                    {
                        playersString = "ERROR";
                    }
                    plugin.sendErrorMessage("Critical error! Ended a game that had no players: " + w.getName() + "!");
                }
                for (int i = 1; i < scoreboardPlayerList.size() - 1; i++)
                {
                    playersString += ", ";
                    playersString += scoreboardPlayerList.get(i);
                }
                String oxfordComma = "";
                if (scoreboardPlayerList.size() > 2)
                {
                    oxfordComma = ",";
                }
                if (scoreboardPlayerList.size() > 1)
                {
                    playersString += oxfordComma;
                    playersString += " and ";
                    playersString += scoreboardPlayerList.get(scoreboardPlayerList.size() - 1);
                }

                int delay = 0;
                SmashStatTracker.addScoreToTotalPoints(w);
                boolean messageEverybody = false;
                SmashScoreboardManager.setScoreboardTitle(w, SmashScoreboardManager.FINAL_SCORES);
                if (isTourneyWorld)
                {
                    removeGame(w, false, false, true);
                    for (Player p : w.getPlayers())
                    {
                        if (scoreboardPlayerList.contains(p.getName()))
                        {
                            SmashStatTracker.incrementTourneyRoundsPlayed(SmashWorldInteractor.getLivingPlayers(w));
                        }
                    }
                    if (getTourneyLevel(w) < 1)
                    {
                        plugin.sendErrorMessage("Error! World " + w.getName() + " has a negative tournament level!");
                    }
                    else if (getTourneyLevel(w) < SmashWorldInteractor.HIGHEST_TOURNEY_LEVEL - 1)
                    {
                        s = ChatColor.GREEN + playersString + " " + moveMoves + " to Round " + (getTourneyLevel(w) + 1) + "!";
                    }
                    else if (getTourneyLevel(w) < SmashWorldInteractor.HIGHEST_TOURNEY_LEVEL)
                    {
                        s = ChatColor.GREEN + playersString + " " + moveMoves + " to the Final Round!";
                    }
                    else if (getTourneyLevel(w) == SmashWorldInteractor.HIGHEST_TOURNEY_LEVEL)
                    {
                        SmashStatTracker.incrementTourneyGamesPlayed(SmashWorldInteractor.getLivingPlayers(w));
                        messageEverybody = true;
                        s = ChatColor.GREEN + playersString + " " + hasHave + " won Smash Tournament #" + plugin.incrementStatistic(DirectoryType.SERVER_DATA, "", "Stats", "Tournaments") + "!";
                    }
                }
                else
                {
                    delay = END_GAME_TIME/3 * 20;
                    removeGame(w, false, true, true);
                    SmashScoreboardManager.removeScoreboardIfThere(w);
                    SmashWorldInteractor.sendMessageToWorld(w, ChatColor.YELLOW + "And the winner is...");
                    for (Player p : w.getPlayers())
                    {
                        if (scoreboardPlayerList.contains(p.getName()))
                        {
                            SmashStatTracker.incrementFreeGamesWon(p);
                        }
                    }

                    SmashStatTracker.incrementFreeGamesPlayed(SmashWorldInteractor.getLivingPlayers(w));
                    if (scoreboardPlayerList.size() == 1)
                    {
                        s = ChatColor.GREEN + playersString + "!";
                    }
                    else
                    {
                        s = ChatColor.GREEN + playersString + "!";
                    }
                }
                SmashWorldInteractor.sendMessageWithDelay(s, w, messageEverybody, delay);

                //****************************** Actually end the game

                if (endGameTasks.containsKey(w))
                {
                    plugin.sendErrorMessage("Warning! Tried to schedule an endgame task, but one was already scheduled!");
                    cancelEndGameTask(w);
                }

                final HashMap<Player, Boolean> halfwaySpeeds = new HashMap<Player, Boolean>();
                final int iterations = 5;
                int period = (int)Math.round(1.0*END_GAME_TIME*20/iterations);
                Runnable r = new Runnable()
                {
                    int i = 0;
                    public void run()
                    {
                        //Bukkit.broadcastMessage("i: " + i + ", iterations: " + iterations);
                        if (i < iterations)
                        {
                            if (i == 0)
                            {
                                for (Player p : w.getPlayers())
                                {
                                    halfwaySpeeds.put(p, SmashWorldInteractor.getParticipants().contains(p.getName()));
                                    if (SmashWorldInteractor.getParticipants().contains(p.getName()))
                                    {
                                        SmashWorldInteractor.getParticipants().remove(p.getName());
                                    }
                                }
                            }
                            if (i > 0)
                            {
                                for (Player p : w.getPlayers())
                                {
                                    if (!SmashEntityTracker.isHoldingStill(p))
                                    {
                                        halfwaySpeeds.put(p, true);
                                    }
                                }
                            }
                            i++;
                        }
                        else if (i == iterations)
                        {
                            World selectedWorld;
                            if (isTourneyWorld(w))
                            {
                                 selectedWorld = getBestWorld(getTourneyLevel(w) + 1, w);
                            }
                            else
                            {
                                selectedWorld = getBestWorld(0, w);
                            }

                            for (Player p : w.getPlayers())
                            {
                                if (SmashEntityTracker.getSpeed(p) < 0.001 && halfwaySpeeds.containsKey(p) && !halfwaySpeeds.get(p))
                                {
                                    sendPlayerToLastWorld(p);
                                    p.sendMessage(ChatColor.YELLOW + "You were afk and were kicked!");
                                }
                                else if (selectedWorld == null)
                                {
                                    sendPlayerToLastWorld(p);
                                }
                                else if (!w.equals(selectedWorld))
                                {
                                    p.sendMessage(ChatColor.GRAY + "Sending you to the next Game now!");
                                    sendPlayerToWorld(p, selectedWorld);
                                }
                                else
                                {
                                    p.sendMessage(ChatColor.GRAY + "This world has been restarted!");
                                }
                            }
                            //Bukkit.broadcastMessage("restarting game");
                            restartWorld(w);
                        }
                    }
                };
                endGameTasks.put(w, plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, r, 0, period));
            }
        }
    }

    /**
     * Function: removeGame
     * Purpose: To remove a game according to the intention of the calling function
     *
     * @param w - The world of the game to be removed
     * @param deletingWorld - Is the world going to be deleted?
     * @param removingForRestart - Is the world going to be restarted immediately?
     * @param removingForEndGame - Has the game simply ended?
     */
    protected static void removeGame(World w, boolean deletingWorld, boolean removingForRestart, boolean removingForEndGame)
    {
        /*if (!(deletingWorld ^ removingForRestart ^ removingForEndGame) && !(!deletingWorld && !removingForEndGame))
        {
            plugin.sendErrorMessage("Warning! Removed a game with more than one intention!");
        }*/
        if (startedGames.contains(w))
        {
            startedGames.remove(w);
        }

        cancelCountdown(w);
        cancelTime(w);
        cancelEndGameTask(w);
        SmashWorldInteractor.forgetOriginalPlayerCount(w);
        removeStartOverride(w);
        SmashEntityTracker.forgetTransgressions(w);
        if (!removingForEndGame)
        {
            SmashScoreboardManager.removeScoreboardIfThere(w);
            cancelConstantTasks(w);
        }
        if (!deletingWorld)
        {
            clearEntities(w);
        }
        MonsterEgg.cancelMonsterKillerTasks(w);

        List<Player> players = w.getPlayers();
        for (Player p : players)
        {
            SmashKitManager.getSelectedKit(p).getProperties().getFinalSmash().cancelTask(p);
            SmashManager.forgetLastHitter(p);
        }

        if (removingForEndGame || removingForRestart)
        {
            w.setPVP(false);
            for (final Player p : players)
            {
                Bukkit.getScheduler().callSyncMethod(plugin, new Callable() {
                    public String call()
                    {
                        SmashWorldInteractor.allowFullflight(p, true);
                        return "";
                    }
                });
            }
        }
    }

    /**
     * Function: restartWorld
     * Purpose: To restart a world for restarting purposes
     *
     * @param w - The world to be reset
     */
    private static void restartWorld(final World w)
    {
        removeGame(w, false, true, false);
        for (Player p : w.getPlayers())
        {
            SmashWorldInteractor.setSpectatorMode(p, w.getPlayers(), false);
        }
        SmashWorldInteractor.sendMessageOutsideWorld(w, ChatColor.GREEN + "World " + SmashWorldInteractor.getShortWorldName(w) + " has been restarted!");
        startWorld(w);
    }

    /**
     * Function: startWorld
     * Purpose: To initialize a world's scoreboard and to initialize its players, along with starting necessary Smash world tasks
     *
     * @param w - The world to be initialized
     */
    protected static void startWorld(World w)
    {
        if (isSmashWorld(w))
        {
            w.setPVP(false);
            SmashScoreboardManager.initializeScoreboard(w);
            for (Player p : SmashWorldInteractor.getLivingPlayers(w))
            {
                SmashWorldInteractor.initializeSmashPlayer(p);
            }

            if (killerTasks.containsKey(w) || doubleJumpTasks.containsKey(w))
            {
                cancelConstantTasks(w);
            }
            Runnable r = getKillerRunnable(w);
            killerTasks.put(w, plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, r, 0, KILLER_DELAY));
            Runnable r1 = getJumpLandRefuelRunnable(w);
            doubleJumpTasks.put(w, plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, r1, 0, DOUBLE_JUMP_LAND_DELAY));
        }
    }

    /**
     * Function: getTimeRemainingRunnable
     * Purpose: To get a runnable that will countdown the time remaining in a currently in-progress Smash game, ending it when the time runs out
     *
     * @param w - The world to get the time remaining runnable for
     * @return - The runnable that will countdown the time remaining one time
     */
    private static Runnable getTimeRemainingRunnable(final World w)
    {
        return new Runnable()
        {
            public void run()
            {
                if (w.getPlayers().size() != 0)
                {
                    if (SmashScoreboardManager.getTimeRemaining(w) > 0)
                    {
                        SmashScoreboardManager.setTimeRemaining(w, SmashScoreboardManager.getTimeRemaining(w) - 1);
                    }
                    else
                    {
                        endGame(w);
                    }
                }
            }
        };
    }

    /**
     * Function: startStartCountDown
     * Purpose: To start the countdown for the time remaining til start for a Smash world
     *
     * @param w - The world for which we would like to decrement the time remaining by 1 each second
     */
    protected static void startStartCountDown(final World w)
    {
        cancelCountdown(w);
        countdownTasks.put(w, plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable()
        {
            public void run()
            {
                if (SmashWorldManager.isSmashWorld(w) && !SmashWorldManager.gameHasStarted(w))
                {
                    SmashScoreboardManager.subtractStartTime(w, 1);
                }
            }
        }, 20, 20));
    }

    /**
     * Function: cancelCoundown
     * Purpose: To deactivateFinalSmash the time til start task in a world if it's going
     *
     * @param w - The world for which to deactivateFinalSmash the time til start
     */
    protected static void cancelCountdown(World w)
    {
        if (countdownTasks.containsKey(w))
        {
            plugin.getServer().getScheduler().cancelTask(countdownTasks.get(w));
            countdownTasks.remove(w);
        }
    }

    /**
     * Function: cancelTime
     * Purpose: To deactivateFinalSmash the task that counts down the time remaining in an ongoing Smash game
     *
     * @param w - The world for which to stop counting down the time remaining
     */
    protected static void cancelTime(World w)
    {
        if (itemSpawnTasks.containsKey(w))
        {
            plugin.getServer().getScheduler().cancelTask(itemSpawnTasks.get(w));
            itemSpawnTasks.remove(w);
        }
        if (timeRemainingTasks.containsKey(w))
        {
            plugin.getServer().getScheduler().cancelTask(timeRemainingTasks.get(w));
            timeRemainingTasks.remove(w);
        }
    }

    /**
     * Function: cancelConstantTasks
     * Purpose: To deactivateFinalSmash the constantly running tasks of a world, including the task for checking if people are one the
     * ground to recharge their reusable items or to check if they're outside the kill zone
     *
     * @param w - The world for which to remove constantly running tasks
     */
    private static void cancelConstantTasks(World w)
    {
        if (doubleJumpTasks.containsKey(w))
        {
            plugin.getServer().getScheduler().cancelTask(doubleJumpTasks.get(w));
            doubleJumpTasks.remove(w);
        }
        if (killerTasks.containsKey(w))
        {
            plugin.getServer().getScheduler().cancelTask(killerTasks.get(w));
            killerTasks.remove(w);
        }
    }

    /**
     * Function: clearEntities
     * Purpose: To clear entities from the world and tasks that are directly related to entities
     *
     * @param w - The world for which to clear entities
     */
    private static void clearEntities(World w)
    {
        Mine.clearMines(w);
        SmashOrb.removePossibleOrb(w);
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
                    Bukkit.getScheduler().callSyncMethod(plugin, new Callable() {
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

    /**
     * Function: cancelEndGameTask
     * Purpose: To deactivateFinalSmash the task that announces winners and finally restarts the world
     *
     * @param w - The world for which to deactivateFinalSmash the end-game task
     */
    private static void cancelEndGameTask(World w)
    {
        if (endGameTasks.containsKey(w))
        {
            Bukkit.getScheduler().cancelTask(endGameTasks.get(w));
            endGameTasks.remove(w);
        }
    }

    /**
     * Function: refreshNeededWorldGui
     * Purpose: To build the guis of all players who have guis open and whose available worlds contain the specified world
     *
     * @param w - The world which was altered in some way or deleted
     */
    private static void refreshNeededWorldGui(World w)
    {
        if (isTourneyWorld(w))
        {
            SmashWorldInteractor.refreshWorldGui(getTourneyLevel(w));
        }
        else
        {
            for (int i = 1; i <= SmashWorldInteractor.HIGHEST_TOURNEY_LEVEL; i++)
            {
                SmashWorldInteractor.refreshWorldGui(i);
            }
        }
    }

    /**
     * Function: isBeingDeleted
     * Purpose: To determine if a world is in the process of being deleted
     *
     * @param worldName - The name of the world for which to check the status of being deleted
     * @return - True if the world is being deleted
     */
    protected static boolean isBeingDeleted(String worldName)
    {
        return worldsBeingDeleted.contains(worldName);
    }

    /**
     * Function: getTourneyLevel
     * Purpose: To get the tourney level of a world (which doesn't change)
     *
     * @param w - The world for which to get the tourney level
     * @return The tourney level of the world, or -1 if it's invalid
     */
    protected static int getTourneyLevel(World w)
    {
        return getTourneyLevel(w.getName());
    }

    /**
     * Function: getTourneyLevel
     * Purpose: To get the tourney level of the world given its name
     *
     * @param worldName - The name of the world for which to check tourney level
     * @return - The tourney level determined from the world name
     */
    private static int getTourneyLevel(String worldName)
    {
        if (isTourneyWorld(worldName))
        {
            return Integer.valueOf("" + worldName.charAt(SMASH_WORLD_PREFIX.length() + SmashWorldInteractor.TOURNEY_INDICATOR.length()));
        }
        plugin.sendErrorMessage("Error! Tried to get the Tournament level of a non-tournement Smash world!");
        return -1;
    }

    /**
     * Function: getTourneyLevel
     * Purpose: To get the tourney level of a world given its world number
     *
     * @param worldNumber - The number of the world for which to get the tourney level
     * @return - The level of the world
     */
    protected static int getTourneyLevel(int worldNumber)
    {
        return getTourneyLevel(getTourneyWorld(worldNumber));
    }

    /**
     * Function: createSmashWorld
     * Purpose: To create a brand new smash world of a given name given the name of a source world
     *
     * @param name - The name you want to give to the world
     * @param sourceWorld - The name of the world to copy
     * @return - The new world
     */
    private static World createSmashWorld(String name, String sourceWorld)
    {
        if (name == null)
        {
            plugin.sendErrorMessage("Error! Could not create world");
            return null;
        }
        else if (!(new File(name)).mkdir())
        {
            if (Bukkit.getWorld(name) != null)
            {
                plugin.sendErrorMessage("Error! World " + name + " already created!");
                return FALLBACK_WORLD;
            }
            else
            {
                plugin.sendErrorMessage("Warning! We already had a " + name + " on file! It's okay though because we'll just replace it!");
                plugin.deleteFile(DirectoryType.ROOT, name, PLAYER_SPAWN_FILENAME);
                plugin.deleteFile(DirectoryType.ROOT, name, ITEM_SPAWN_FILENAME);
            }
        }
        if (sourceWorld.endsWith("-1") && sourceWorld.startsWith(SMASH_TEMPLATE_PREFIX))
        {
            plugin.sendErrorMessage(ChatColor.RED + "Error! Could not find any Smash map by that number! Make sure you have SmashWorld0, SmashWorld1, etc.");
            return FALLBACK_WORLD;
        }
        File baseWorld = new File(sourceWorld);
        File newWorld = new File(name);
        try
        {
            FilenameFilter fileFilter = new FilenameFilter()
            {
                public boolean accept(File file, String s)
                {
                    return s.equals("uid.dat") || s.startsWith("session");
                }
            };
            File[] filteredFiles = baseWorld.listFiles(fileFilter);
            for (File f : filteredFiles)
            {
                f.delete();
            }
            FileUtils.copyDirectory(baseWorld, newWorld);

            World createdWorld = new WorldCreator(name).createWorld();
            createdWorld.setGameRuleValue("doDaylightCycle", "false");
            createdWorld.setGameRuleValue("doMobSpawning", "false");
            createdWorld.setGameRuleValue("keepInventory", "true");
            createdWorld.setGameRuleValue("doTileDrops", "false");
            createdWorld.setGameRuleValue("mobGriefing", "false");
            createdWorld.setDifficulty(Difficulty.EASY);
            createdWorld.setTime(7000);
            createdWorld.setSpawnLocation(0, SPAWN_HEIGHT, 0);
            loadWorld(createdWorld);
            refreshNeededWorldGui(createdWorld);

            return createdWorld;
        }
        catch (IOException e)
        {
            plugin.sendErrorMessage(ChatColor.RED + "Error! Unable to instantiate world " + name);
            return null;
        }

    }

    /**
     * Function: createSmashWorld
     * Purpose: To create a smash world copied from a random world to copy from
     *
     * @param name - The name you want to give to the new world
     * @return - The created world
     */
    private static World createSmashWorld(String name)
    {
        return createSmashWorld(name, getRandomSmashTemplate());
    }

    /**
     * Function: createNextTourneyWorld
     * Purpose: To create a tournament world that will not be the same name as an already created Tourney world number
     * following someone sending a create world command
     *
     * @param sender - The sender of the command
     * @param tourneyLevel - The tourney level of the world to be created
     * @param template - The map world to copy from when creating the world
     * @param joinOnCreate - Should the sender join as soon as the world is created?
     */
    private static World createNextTourneyWorld(CommandSender sender, int tourneyLevel, String template, boolean joinOnCreate)
    {
        for (int i = 1; i <= MAX_TOURNEY_WORLDS; i++)
        {
            if (!SmashWorldInteractor.getTakenTourneyWorldNumbers().contains(i))
            {
                World w = createSmashWorld(SmashWorldInteractor.getTourneyWorldName(tourneyLevel, i), template);
                if (sender != null)
                {
                    doCreationAction(sender, w, joinOnCreate);
                }
                return w;
            }
        }
        if (sender != null)
        {
            sender.sendMessage(ChatColor.RED + "The limit of " + MAX_TOURNEY_WORLDS + " has already been reached!");
        }
        return null;
    }

    /**
     * Function: doCreationAction
     * Purpose: To send a player to the world that he just created using a command if he's a player who wants to join
     * the world he just created
     *
     * @param sender - The CommandSender who issued the world creation command
     * @param w - The world which was created
     * @param joinOnCreate - Whether the CommandSender should be sent to the world if he's a player
     */
    private static void doCreationAction(CommandSender sender, World w, boolean joinOnCreate)
    {
        if (sender instanceof Player && joinOnCreate)
        {
            sendPlayerToWorld((Player)sender, w);
        }
        else
        {
            sender.sendMessage(ChatColor.GREEN + "Created " + SmashWorldInteractor.getWorldDescription(w) + ChatColor.GREEN + "!");
        }
    }

    /**
     * Function: createNextTourneyWorld
     * Purpose: To create the next Tourney world that's available given the taken tourney world numbers and those being
     * deleted
     *
     * @param sender - The CommandSender who's creating a world
     * @param tourneyLevel - The tourney level of the tourney world to be created
     */
    private static World createNextTourneyWorld(CommandSender sender, int tourneyLevel)
    {
        return createNextTourneyWorld(sender, tourneyLevel, getRandomSmashTemplate(), false);
    }

    /**
     * Function:
     * Purpose:
     *
     * @param creator
     */
    protected static World createNextTourneyWorld(Player creator)
    {
        for (int i = 1; i <= MAX_TOURNEY_WORLDS; i++)
        {
            if (!SmashWorldInteractor.getTakenTourneyWorldNumbers().contains(i) && !isBeingDeleted(SmashWorldInteractor.getTourneyWorldName(SmashStatTracker.getTourneyLevel(creator), i)))
            {
                World w = createSmashWorld(SmashWorldInteractor.getTourneyWorldName(SmashStatTracker.getTourneyLevel(creator), i));
                if (creator != null)
                {
                    sendPlayerToWorld(creator, w);
                }
                return w;
            }
        }
        if (creator != null)
        {
            creator.sendMessage(ChatColor.RED + "The limit of " + MAX_TOURNEY_WORLDS + " has already been reached!");
        }
        return null;
    }

    /**
     * Function:
     * Purpose:
     *
     * @param sender
     */
    private static World createNextFreeplayWorld(CommandSender sender)
    {
        return createNextFreeplayWorld(sender, getRandomSmashTemplate());
    }

    /**
     * Function:
     * Purpose:
     *
     * @param sender
     * @param template
     */
    private static World createNextFreeplayWorld(CommandSender sender, String template)
    {
        return createNextFreeplayWorld(sender, template, false);
    }

    /**
     * Function:
     * Purpose:
     *
     * @param sender
     * @param template
     * @param joinOnCreate
     */
    private static World createNextFreeplayWorld(CommandSender sender, String template, boolean joinOnCreate)
    {
        for (int i = 1; i <= MAX_FREEPLAY_WORLDS; i++)
        {
            if (!SmashWorldInteractor.getTakenFreeplayWorldNumbers().contains(i) && !isBeingDeleted(SmashWorldInteractor.getFreeplayWorldName(i)))
            {
                World w = createSmashWorld(SmashWorldInteractor.getFreeplayWorldName(i), template);
                if (sender != null)
                {
                    doCreationAction(sender, w, joinOnCreate);
                }
                return w;
            }
        }
        if (sender != null)
        {
            sender.sendMessage(ChatColor.RED + "The limit of " + MAX_FREEPLAY_WORLDS + " has already been reached!");
        }
        return null;
    }

    /**
     * Function:
     * Purpose:
     *
     * @param creator
     */
    protected static World createNextFreeplayWorld(Player creator)
    {
        for (int i = 1; i <= MAX_FREEPLAY_WORLDS; i++)
        {
            if (!SmashWorldInteractor.getTakenFreeplayWorldNumbers().contains(i))
            {
                World w =  createSmashWorld(SmashWorldInteractor.getFreeplayWorldName(i));
                if (creator != null)
                {
                    sendPlayerToWorld(creator, w);
                }
                return w;
            }
        }
        if (creator != null)
        {
            creator.sendMessage(ChatColor.RED + "The limit of " + MAX_FREEPLAY_WORLDS + " has already been reached!");
        }
        return null;
    }

    /**
     * Function:
     * Purpose:
     *
     * @param w
     */
    private static void loadWorld(World w)
    {
        if (isSmashWorld(w))
        {
            loadSpawns(w);
            //killerTasks.put(createdWorld, plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, getKillerRunnable(createdWorld), 0, KILLER_DELAY));
            //and don't cancelTasks to start it

            if (isTourneyWorld(w.getName()))
            {
                logTourneyWorld(w.getName());
            }
            else if (!isTourneyWorld(w.getName()))
            {
                logFreeplayWorld(w.getName());
            }

            if (SmashWorldInteractor.getLivingPlayers(w).size() > 0)
            {
                startWorld(w);
            }
        }
        else
        {
            plugin.sendErrorMessage("Error! Tried to load a non-smash world!");
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @param worldName
     */
    private static void logTourneyWorld(String worldName)
    {
        int worldNumber = SmashWorldInteractor.getWorldNumber(worldName);
        tourneyLevels.put(worldNumber, getTourneyLevel(worldName));
        for (int i = 0; i < SmashWorldInteractor.getTakenTourneyWorldNumbers().size(); i++)
        {
            if (worldNumber < SmashWorldInteractor.getTakenTourneyWorldNumbers().get(i))
            {
                SmashWorldInteractor.getTakenTourneyWorldNumbers().add(i, worldNumber);
                return;
            }
        }
        if (SmashWorldInteractor.getTakenTourneyWorldNumbers().size() == 0)
        {
            SmashWorldInteractor.getTakenTourneyWorldNumbers().add(worldNumber);
        }
        else
        {
            SmashWorldInteractor.getTakenTourneyWorldNumbers().add(worldNumber);
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @param worldName
     */
    private static void logFreeplayWorld(String worldName)
    {
        int worldNumber = SmashWorldInteractor.getWorldNumber(worldName);
        for (int i = 0; i < SmashWorldInteractor.getTakenFreeplayWorldNumbers().size(); i++)
        {
            if (SmashWorldInteractor.getWorldNumber(worldName) < SmashWorldInteractor.getTakenFreeplayWorldNumbers().get(i))
            {
                SmashWorldInteractor.getTakenFreeplayWorldNumbers().add(i, worldNumber);
                return;
            }
        }
        if (SmashWorldInteractor.getTakenFreeplayWorldNumbers().size() == 0)
        {
            SmashWorldInteractor.getTakenFreeplayWorldNumbers().add(SmashWorldInteractor.getWorldNumber(worldName));
        }
        else
        {
            SmashWorldInteractor.getTakenFreeplayWorldNumbers().add(worldNumber);
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @param worldName
     */
    private static void unlogTourneyWorld(String worldName)
    {
        int worldNumber = SmashWorldInteractor.getWorldNumber(worldName);
        if (SmashWorldInteractor.getTakenTourneyWorldNumbers().contains(worldNumber) && tourneyLevels.containsKey(worldNumber))
        {
            tourneyLevels.remove(worldNumber);
            SmashWorldInteractor.getTakenTourneyWorldNumbers().remove((Integer)worldNumber);
        }
        else
        {
            plugin.sendErrorMessage("Warning! Tried to unlog world " + worldName + " when it wasn't logged proper!");
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @param worldName
     */
    private static void unlogFreeplayWorld(String worldName)
    {
        int worldNumber = SmashWorldInteractor.getWorldNumber(worldName);
        if (SmashWorldInteractor.getTakenFreeplayWorldNumbers().contains(worldNumber))
        {
            SmashWorldInteractor.getTakenFreeplayWorldNumbers().remove((Integer)worldNumber);
        }
        else
        {
            plugin.sendErrorMessage("Warning! Tried to unlog world " + worldName + " when it wasn't logged!");
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @return
     */
    private static File[] listSmashFolders()
    {
        FilenameFilter fileFilter = new FilenameFilter()
        {
            public boolean accept(File file, String s)
            {
                return (s.startsWith(SMASH_WORLD_PREFIX));
            }
        };
        File folder = new File(plugin.getAbsolutePath(DirectoryType.ROOT, ""));
        return folder.listFiles(fileFilter);
    }

    /**
     * Function:
     * Purpose:
     */
    protected static void deleteWorldFolders()
    {
        for (File f : listSmashFolders())
        {
            deleteWorldFolder(f);
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @param f
     */
    private static void deleteWorldFolder(File f) //The part that needs to be done regardless of whether you just started the plugin
    {
        String worldName = f.getName();
        World w = Bukkit.getWorld(worldName);
        if (w != null)
        {
            Bukkit.unloadWorld(w, false);
        }

        plugin.deleteFolder(f);

        if (isTourneyWorld(worldName))
        {
            unlogTourneyWorld(worldName);
        }
        else if (isSmashWorld(worldName))
        {
            unlogFreeplayWorld(worldName);
        }
        if (worldsBeingDeleted.contains(worldName))
        {
            worldsBeingDeleted.remove(worldName);
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @param w
     */
    private static void deleteWorld(final World w) //The part that needs to be done if you didn't just start the plugin
    {
        if (w != null)
        {
            worldsBeingDeleted.add(w.getName());
            for (final Player p : w.getPlayers())
            {
                Bukkit.getScheduler().callSyncMethod(plugin, new Callable() {
                    public String call()
                    {
                        sendPlayerToLastWorld(p);
                        return "";
                    }
                });
            }
            if (isSmashWorld(w))
            {
                refreshNeededWorldGui(w);
                removeGame(w, true, false, false);
                //smashWorlds.remove(world);

                for (final Player p : w.getPlayers())
                {
                    p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
                    SmashManager.forgetLastHitter(p);
                }

                SmashScoreboardManager.removeScoreboardIfThere(w);
                if (SmashWorldInteractor.getReadyPlayers().containsKey(w))
                {
                    SmashWorldInteractor.getReadyPlayers().remove(w);
                }
                if (itemSpawnLocations.containsKey(w))
                {
                    itemSpawnLocations.remove(w);
                }
                if (playerSpawnLocations.containsKey(w))
                {
                    playerSpawnLocations.remove(w);
                }
            }
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                public void run()
                {
                    deleteWorldFolder(new File(w.getName()));
                }
            }, 140);
        }
        else
        {
            plugin.sendErrorMessage("Error! Could not delete null world!");
        }
    }

    public static Location getRandomItemSpawnLocation(World w)
    {
        ArrayList<Location> possibleSpawnLocations = itemSpawnLocations.get(w);
        return possibleSpawnLocations.get(r.nextInt(possibleSpawnLocations.size()));
    }

    /**
     * Function:
     * Purpose:
     *
     * @param worldNumber
     * @return
     */
    protected static World getTourneyWorld(int worldNumber)
    {
        String worldName;
        if (tourneyLevels.containsKey(worldNumber))
        {
            worldName = SmashWorldInteractor.getTourneyWorldName(tourneyLevels.get(worldNumber), worldNumber);
        }
        else
        {
            plugin.sendErrorMessage("Error! Tried to get the world name for Tourney world " + worldNumber + ", but the tourneyLevels HashMap didn't have the key!");
            return null;
        }
        if (Bukkit.getWorld(worldName) != null)
        {
            return Bukkit.getWorld(worldName);
        }
        plugin.sendErrorMessage("Error! Tried to get world " + worldName + ", but it didn't exist!");
        return null;
    }

    /**
     * Function:
     * Purpose:
     *
     * @param worldNumber
     * @return
     */
    protected static World getFreeplayWorld(int worldNumber)
    {
        if (Bukkit.getWorld(SmashWorldInteractor.getFreeplayWorldName(worldNumber)) != null)
        {
            return Bukkit.getWorld(SmashWorldInteractor.getFreeplayWorldName(worldNumber));
        }
        plugin.sendErrorMessage("Error! Tried to get world " + SmashWorldInteractor.getFreeplayWorldName(worldNumber) + ", but it didn't exist!");
        return null;
    }

    /**
     * Function:
     * Purpose:
     *
     * @return
     */
    private static File[] getSmashTemplates()
    {
        FilenameFilter fileFilter = new FilenameFilter()
        {
            public boolean accept(File file, String s)
            {
                return s.startsWith(SMASH_TEMPLATE_PREFIX);
            }
        };

        File folder = new File(plugin.getAbsolutePath(DirectoryType.ROOT, ""));
        return folder.listFiles(fileFilter);
    }

    /**
     * Function:
     * Purpose:
     *
     * @return
     */
    private static String getRandomSmashTemplate()
    {
        int numOfSmashWorlds = getSmashTemplates().length;

        if (numOfSmashWorlds > 0)
        {
            return getSmashTemplate((new Random()).nextInt(numOfSmashWorlds));
        }
        else
        {
            plugin.sendErrorMessage("There were no Smash maps for which to create a new Smash world! Please add SmashWorld0");
            return null;
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @param index
     * @return
     */
    private static String getSmashTemplate(int index)
    {
        return SMASH_TEMPLATE_PREFIX + index;
    }

    private static World getBestWorld(int tourneyLevel, World currentWorld)
    {
        List<Integer> avaliableWorldNums;
        if (tourneyLevel == 0)
        {
            avaliableWorldNums = SmashWorldInteractor.getTakenFreeplayWorldNumbers();
        }
        else if (tourneyLevel <= SmashWorldInteractor.HIGHEST_TOURNEY_LEVEL)
        {
            avaliableWorldNums = SmashWorldInteractor.getTakenTourneyWorldNumbers();
        }
        else
        {
            return null;
        }

        World nextWorld = null;
        for (int i = 0; i < avaliableWorldNums.size(); i++)
        {
            World candidateWorld;
            if (avaliableWorldNums == SmashWorldInteractor.getTakenTourneyWorldNumbers())
            {
                 candidateWorld = getTourneyWorld(avaliableWorldNums.get(i));
            }
            else
            {
                candidateWorld = getFreeplayWorld(avaliableWorldNums.get(i));
            }

            if (tourneyLevel == 0 || getTourneyLevel(candidateWorld) == tourneyLevel)
            {
                if ((!hasScoreboard(candidateWorld) || !gameHasStarted(candidateWorld) && SmashScoreboardManager.getStartTime(candidateWorld) > 7)
                        && (!SmashWorldInteractor.getMapName(candidateWorld).equals(SmashWorldInteractor.getMapName(currentWorld)) || candidateWorld.getPlayers().size() >= 1))
                {
                    nextWorld = candidateWorld;
                }
            }
        }
        if (nextWorld == null)
        {
            if (tourneyLevel == 0)
            {
                nextWorld = createNextFreeplayWorld(null);
            }
            else
            {
                nextWorld = createNextTourneyWorld(null, tourneyLevel);
            }
        }
        return nextWorld;
    }

    /**
     * Function:
     * Purpose:
     *
     * @param p
     */
    private static void sendPlayerToLastWorld(final Player p)
    {
        final Location l = getLastPlayerLocation(p);
        if (l != null)
        {
            p.teleport(l);
            lastPlayerLocations.remove(p);
        }
        else
        {
            while(FALLBACK_WORLD == null){}
            p.teleport(FALLBACK_WORLD.getSpawnLocation());
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @param p
     * @param w2
     */
    protected static void sendPlayerToWorld(final Player p, World w2)
    {
        World w1 = p.getWorld();
        if (w1.equals(w2))
        {
            if (w1.equals(FALLBACK_WORLD))
            {
                p.teleport(FALLBACK_WORLD.getSpawnLocation());
            }
            else
            {
                p.sendMessage(ChatColor.YELLOW + "You are already in this world!");
            }
        }
        else if (!Bukkit.getWorlds().contains(w2) || !isSmashWorld(w2))
        {
            if (!Bukkit.getWorlds().contains(w2))
            {
                if (FALLBACK_WORLD != null)
                {
                    p.sendMessage(ChatColor.RED + "World does not exist or is not a smash world! Sending you to back to fallback world!");
                    sendPlayerToWorld(p, FALLBACK_WORLD);
                }
            }
            else
            {
                sendPlayerToLastWorld(p);
            }
        }
        else if (isTourneyWorld(w2) && !SmashStatTracker.canJoinTourneys(p.getName()))
        {
            p.sendMessage(ChatColor.RED + "You must play " + SmashStatTracker.getGamesTilCanJoinTourneys(p.getName()) + " more games before you can play in tournaments.");
        }
        else if (!SmashWorldInteractor.getAvaliableWorlds(p).contains(w2))
        {
            p.sendMessage(ChatColor.RED + "You are not allowed to join this game (at least not right now)!");
        }
        else if (!playerSpawnLocations.containsKey(w2) || playerSpawnLocations.get(w2).size() == 0)
        {
            p.sendMessage(ChatColor.RED + "Error! There are no spawn locations in map " + SmashWorldInteractor.getMapName(w2));
        }
        else
        {
            if (!isSmashWorld(w1))
            {
                lastPlayerLocations.put(p, p.getLocation());
            }
            if (SmashWorldInteractor.isHated(p, w2))
            {
                p.sendMessage(ChatColor.RED + "Sorry, but the players in this world have decided that they don't want you.");
            }
            else
            {
                p.teleport(getRandomPlayerSpawnLocation(w2));
            }
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @param w
     * @return
     */
    public static Location getRandomPlayerSpawnLocation(World w)
    {
        ArrayList<Location> possibleSpawnLocations = playerSpawnLocations.get(w);
        return possibleSpawnLocations.get(r.nextInt(possibleSpawnLocations.size()));
    }

    private void addMotdMessage(String message)
    {
        motdMessages.add(plugin.loggerPrefix() + ChatColor.AQUA + message);
    }

    /**
     * Function:
     * Purpose:
     *
     * @param sender
     */
    private void listWorlds(CommandSender sender)
    {
        List<String> list = new ArrayList<String>();

        World w;
        for (Integer i : SmashWorldInteractor.getTakenTourneyWorldNumbers())
        {
            w = getTourneyWorld(i);
            if (w != null)
            {
                if (sender instanceof Player)
                {
                    list.add(SmashWorldInteractor.getWorldListName(w, (Player)sender));
                }
                else
                {
                    list.add(SmashWorldInteractor.getWorldListName(w, null));
                }
            }
            else
            {
                plugin.sendErrorMessage("Error! When " + sender.getName() + " tried to list the worlds, one of the Tournament worlds was null!");
            }
        }
        for (Integer i : SmashWorldInteractor.getTakenFreeplayWorldNumbers())
        {
            w = getFreeplayWorld(i);
            if (w != null)
            {
                if (sender instanceof Player)
                {
                    list.add(SmashWorldInteractor.getWorldListName(w, (Player)sender));
                }
                else
                {
                    list.add(SmashWorldInteractor.getWorldListName(w, null));
                }
            }
            else
            {
                plugin.sendErrorMessage("Error! When " + sender.getName() + " tried to list the worlds, one of the Freeplay worlds was null!");
            }
        }

        if (list.size() == 0)
        {
            sender.sendMessage(ChatColor.GRAY + "No Smash worlds were found.");
        }
        else
        {
            for (String s : list)
            {
                sender.sendMessage(s);
            }
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @param arg
     * @return
     */
    protected static String smashifyArgument(String arg)
    {
        if (!arg.startsWith(SMASH_WORLD_PREFIX) && arg.length() == 4)
        {
            arg = SMASH_WORLD_PREFIX + arg;
        }
        arg = arg.replaceAll("g", "w");
        return arg;
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
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (plugin.matchesCommand(label, SMASH_CMD))
        {
            if (sender instanceof Player)
            {
                SmashWorldInteractor.openWorldGui((Player)sender);
            }
            else
            {
                plugin.sendErrorMessage("You are not in game!");
            }
            return true;
        }
        else if (plugin.matchesCommand(label, START_CMD))
        {
            Player p = null;
            World w;

            if (args.length > 1)
            {
                if (!args[1].startsWith(SMASH_WORLD_PREFIX))
                {
                    args[1] = SMASH_WORLD_PREFIX + args[1];
                }
                if (Bukkit.getWorld(args[1]) == null)
                {
                    sender.sendMessage(ChatColor.RED + "Could not find world " + args[1]);
                    return true;
                }
                else
                {
                    w = Bukkit.getWorld(args[1]);
                }
            }
            else if (!(sender instanceof Player))
            {
                plugin.sendErrorMessage("You are not in game!");
                return true;
            }
            else
            {
                p = (Player)sender;
                w = p.getWorld();
            }

            if (!isSmashWorld(w))
            {
                if (p != null)
                {
                    sender.sendMessage(ChatColor.RED + "You are not in a Smash world!");
                }
                else
                {
                    sender.sendMessage(ChatColor.RED + "That is not a Smash world!");
                }
            }
            else if (gameHasStarted(w))
            {
                sender.sendMessage(ChatColor.RED + "That game has already started!");
            }
            else
            {
                SmashWorldInteractor.determineDeathMatchPreference(w);
                if (SmashWorldInteractor.isDeathMatchWorld(w) && w.getPlayers().size() <= 1)
                {
                    sender.sendMessage(ChatColor.RED + "You can't start a Deathmatch game when you're the only player! Either change your deathmatch preference (/dm) or bring someone in!");
                }
                else if (gameHasStarted(w))
                {
                    if (p != null && w.equals(p.getWorld()))
                    {
                        sender.sendMessage(ChatColor.RED + "Game has already started!");
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.RED + "Game has already started in " + SmashWorldInteractor.getShortWorldName(w) + ".");
                    }
                }
                else if (w.getPlayers().size() == 0)
                {
                    sender.sendMessage(ChatColor.RED + "There's no one in that world!");
                }
                else
                {
                    startGameOverrides.add(w);
                    boolean invalidStartTime = false;
                    int timeTilStart = 0;
                    if (args.length > 0)
                    {
                        args[0] = args[0].toLowerCase();
                        int multiplier = 1;
                        if (args[0].endsWith("m"))
                        {
                            multiplier = 60;
                        }
                        else if (args[0].endsWith("h"))
                        {
                            multiplier = 3600;
                        }
                        else if (args[0].toLowerCase().endsWith("d"))
                        {
                            multiplier = 86400;
                        }
                        if (multiplier != 1 || args[0].endsWith("s"))
                        {
                            args[0] = args[0].substring(0, args[0].length() - 1);
                        }
                        try
                        {
                            timeTilStart = Integer.valueOf(args[0]);
                            timeTilStart *= multiplier;
                            if (timeTilStart > 14400 || timeTilStart <= 0)
                            {
                                invalidStartTime = true;
                                if (timeTilStart > 0)
                                {
                                    sender.sendMessage(ChatColor.RED + "Start time out of bounds! Max is 4 hours.");
                                }
                                else
                                {
                                    sender.sendMessage(ChatColor.RED + "If you want to start the game right now, just do /start.");
                                }
                            }
                        }
                        catch (NumberFormatException e)
                        {
                            invalidStartTime = true;
                            sender.sendMessage(ChatColor.RED + "Invalid start time!");
                        }
                    }
                    if (!invalidStartTime)
                    {
                        if (timeTilStart == 0)
                        {
                            startGame(w);
                        }
                        else
                        {
                            SmashScoreboardManager.setStartTime(w, timeTilStart);
                        }
                    }
                }
            }
            return true;
        }
        else if (plugin.matchesCommand(label, LEAVE_CMD))
        {
            if (!(sender instanceof Player))
            {
                plugin.sendErrorMessage(ChatColor.RED + "You are not in-game. Therefore, you are not in a Smash game.");
            }
            else
            {
                Player p = (Player) sender;
                SmashStatTracker.addScoreToTotalPoints(p);
                sendPlayerToLastWorld(p);
            }
            return true;
        }
        else if (plugin.matchesCommand(label, SmashWorldInteractor.CREATE_WORLD_CMD))
        {
            int templateNumber = -1;
            boolean joinOnCreate = false;
            if (args.length > 1)
            {
                try
                {
                    templateNumber = Integer.valueOf("" + args[1]);
                    if (templateNumber < 0 || templateNumber >= getSmashTemplates().length)
                    {
                        throw new NumberFormatException();
                    }
                }
                catch (NumberFormatException e)
                {
                    sender.sendMessage(ChatColor.RED + "Invalid Smash template! Do /listmaps to find out which ones you can use!");
                    return true;
                }
                if (args.length > 2 && (args[2].equalsIgnoreCase("true") || args[2].equalsIgnoreCase("join")))
                {
                    joinOnCreate = true;
                }
            }
            if (args.length == 0)
            {
                sender.sendMessage(ChatColor.RED + "You must specify \"free\" or the Tournament round of the world you would like to create!");
            }
            else if (args[0].equalsIgnoreCase("help"))
            {
                return false;
            }
            else if (args[0].toLowerCase().startsWith("free"))
            {
                if (args.length > 1)
                {
                    createNextFreeplayWorld(sender, getSmashTemplate(templateNumber), joinOnCreate);
                }
                else
                {
                    createNextFreeplayWorld(sender);
                }
            }
            else
            {
                int tourneyLevel;
                try
                {
                    tourneyLevel = Integer.valueOf(args[0]);
                    if (tourneyLevel < 1 || tourneyLevel > SmashWorldInteractor.HIGHEST_TOURNEY_LEVEL)
                    {
                        throw new NumberFormatException();
                    }
                }
                catch (NumberFormatException e)
                {
                    sender.sendMessage(ChatColor.RED + "Invalid Tournament level! Your options are 1-" + SmashWorldInteractor.HIGHEST_TOURNEY_LEVEL + ".");
                    return true;
                }
                if (tourneyLevel >= 1 && tourneyLevel <= SmashWorldInteractor.HIGHEST_TOURNEY_LEVEL)
                {
                    if (args.length > 1)
                    {
                        createNextTourneyWorld(sender, tourneyLevel, getSmashTemplate(templateNumber), joinOnCreate);
                    }
                    else
                    {
                        createNextTourneyWorld(sender, tourneyLevel);
                    }
                }
            }

            return true;
        }
        else if (plugin.matchesCommand(label, DELETE_WORLD_CMD))
        {
            if (args.length == 0)
            {
                if (!(sender instanceof Player))
                {
                    plugin.sendErrorMessage("You must specify which world to delete, since you're not in one");
                    listWorlds(sender);
                }
                else
                {
                    Player p = (Player)sender;
                    if (!isVerifier(p))
                    {
                        bindVerifier(p, label, args);
                        sender.sendMessage(ChatColor.YELLOW + "Do you really want to delete the world you're in right now (" + SmashWorldInteractor.getSimpleWorldName(p.getWorld()) + ChatColor.YELLOW + ")?");
                    }
                    else if (plugin.isTrue(getDecision(p)))
                    {
                        releaseVerifier(p);
                        deleteWorld(p.getWorld());
                        sender.sendMessage(ChatColor.GREEN + "You deleted the world you were in.");
                    }
                    else if (plugin.isFalse(getDecision(p)))
                    {
                        releaseVerifier(p);
                        sender.sendMessage(ChatColor.GREEN + "Command /" + label + " cancelled!");
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.RED + "Invalid response!");
                    }
                }
            }
            else
            {
                if (args[0].equalsIgnoreCase("help"))
                {
                    return false;
                }
                String smashingArg = "" + args[0];
                smashingArg = smashifyArgument(smashingArg);
                if (Bukkit.getWorld(smashingArg) == null || !isSmashWorld(Bukkit.getWorld(smashingArg)))
                {
                    File f = new File(smashingArg);
                    if (f.exists() && args[0].startsWith(SMASH_WORLD_PREFIX))
                    {
                        sender.sendMessage(ChatColor.GREEN + "World wasn't found, but we deleted its folder");
                        deleteWorldFolder(f);
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.RED + "World " + args[0] + " not found!");
                    }
                }
                else
                {
                    World w = Bukkit.getWorld(smashingArg);
                    deleteWorld(w);
                    sender.sendMessage(ChatColor.GREEN + "You deleted world " + SmashWorldInteractor.getSimpleWorldName(w) + ChatColor.GREEN + ".");
                }
            }
            return true;
        }
        else if (plugin.matchesCommand(label, LIST_MAP_CMD))
        {
            File[] templates = getSmashTemplates();
            int i;
            for (i = templates.length - 1; i >= 0; i--)
            {
                ChatColor color;
                if (i % 2 == 0)
                {
                    color = ChatColor.BLUE;
                }
                else
                {
                    color = ChatColor.DARK_GREEN;
                }

                if (i == templates.length - 1)
                {
                    sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Smash maps:");
                }
                sender.sendMessage(color + "Map " + templates[i].getName().charAt(templates[i].getName().length() - 1) + ": " + SmashWorldInteractor.getMapName(templates[i].getName()));
            }
            if (i == 0)
            {
                sender.sendMessage(ChatColor.RED + "There aren't any maps. Looks like you'll need to add some.");
            }

            return true;
        }
        else if (plugin.matchesCommand(label, LIST_WORLD_CMD))
        {
            if (FALLBACK_WORLD != null)
            {
                sender.sendMessage(ChatColor.BLUE + "" + ChatColor.ITALIC + "Fallback world: " + FALLBACK_WORLD.getName());
            }
            sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "Smash World List:");
            listWorlds(sender);
            return true;
        }
        else if (plugin.matchesCommand(label, RESET_CMD))
        {
            Player p = null;
            if (sender instanceof Player)
            {
                p = (Player)sender;
            }
            final World w;
            if (args.length == 0)
            {
                if (!(sender instanceof Player))
                {
                    sender.sendMessage(ChatColor.RED + "You must specify which world to reset!");
                    return true;
                }
                else
                {
                    w = p.getWorld();
                }
            }
            else
            {
                args[0] = smashifyArgument(args[0]);
                if (Bukkit.getWorld(args[0]) == null)
                {
                    sender.sendMessage("Error! World " + args[0] + " not found!");
                    return true;
                }
                else
                {
                    w = Bukkit.getWorld(args[0]);
                }
            }
            if (isSmashWorld(w))
            {
                if (!(sender instanceof Player) || args.length > 0)
                {
                    Bukkit.getScheduler().callSyncMethod(plugin, new Callable() {
                        public String call()
                        {
                            restartWorld(w);
                            return "";
                        }
                    });
                    sender.sendMessage(ChatColor.YELLOW + "Game in " + SmashWorldInteractor.getSimpleWorldName(w) + ChatColor.YELLOW + " reset!");
                }
                else if (gameHasStarted(w) && !gameHasEnded(w) || isVerifier(p))
                {
                    if (!isVerifier(p))
                    {
                        if (p.getWorld() == w)
                        {
                            p.sendMessage(ChatColor.YELLOW + "Do you really want to restart the world you're in right now!?");
                        }
                        else
                        {
                            p.sendMessage(ChatColor.YELLOW + "That game is in progress. Are you sure you want to reset it?");
                        }
                        bindVerifier(p, label, args);
                    }
                    else if (plugin.isTrue(getDecision(p)))
                    {
                        releaseVerifier(p);
                        Bukkit.getScheduler().callSyncMethod(plugin, new Callable() {
                            public String call()
                            {
                                restartWorld(w);
                                return "";
                            }
                        });
                        if (p.getWorld() != w)
                        {
                            p.sendMessage(ChatColor.YELLOW + "Game in " + SmashWorldInteractor.getSimpleWorldName(w) + ChatColor.YELLOW + " reset!");
                        }
                    }
                    else if (plugin.isFalse(getDecision(p)))
                    {
                        releaseVerifier(p);
                        p.sendMessage(ChatColor.YELLOW + "Command /" + label + " cancelled");
                    }
                    else
                    {
                        p.sendMessage(ChatColor.RED + "Invalid response!");
                    }
                }
                else
                {
                    Bukkit.getScheduler().callSyncMethod(plugin, new Callable() {
                        public String call()
                        {
                            restartWorld(w);
                            return "";
                        }
                    });
                    p.sendMessage(ChatColor.YELLOW + "Game in " + SmashWorldInteractor.getSimpleWorldName(w) + ChatColor.YELLOW + " reset.");
                }

            }
            return true;
        }
        else if (plugin.matchesCommand(label, GET_WORLD_CMD))
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
                }
                else
                {
                    Player p = Bukkit.getPlayer(args[0]);
                    w = p.getWorld();
                    sender.sendMessage(ChatColor.GREEN + p.getName() + " is in " + SmashWorldInteractor.getWorldDescription(w) + ChatColor.GREEN + " (" + SmashWorldInteractor.getShortWorldName(w) + ") which is " + ChatColor.stripColor(SmashWorldInteractor.getWorldStatus(w, p)).toLowerCase() + ChatColor.GREEN + ".");
                }
            }
            else if (!(sender instanceof Player))
            {
                sender.sendMessage(ChatColor.RED + "You are not in game!");
            }
            else
            {
                sender.sendMessage(ChatColor.GREEN + "You are in " + SmashWorldInteractor.getWorldDescription(((Player)sender).getWorld()) + ChatColor.GREEN + " (" + SmashWorldInteractor.getShortWorldName(((Player)sender).getWorld()) + ").");
            }
            return true;
        }
        else if (plugin.matchesCommand(label, DMG_CMD))
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
                SmashManager.addDamage(Bukkit.getPlayer(args[0]), damage, false);
            }
            return true;
        }
        else if (plugin.matchesCommand(label, GOTO_WORLD_CMD))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage(ChatColor.RED + "You are not in game!");
            }
            else if (args.length == 0)
            {
                sender.sendMessage(ChatColor.GREEN + "You are currently in " + SmashWorldInteractor.getShortWorldName(((Player)sender).getWorld()));
                sender.sendMessage(ChatColor.GREEN + "Example: /goto fpg1");
                listWorlds(sender);
            }
            else if (args[0].equalsIgnoreCase("help"))
            {
                return false;
            }
            else
            {
                String worldName = args[0];
                World destination = null;
                boolean askingForFreeplay = false;
                boolean askingForTourney = false;
                if (args.length >= 3)
                {
                    String first2Args = args[0] + " " + args[1] + " ";
                    askingForFreeplay = first2Args.equalsIgnoreCase(SmashWorldInteractor.FREE_WORLD_ITEM_PREFIX);
                    askingForTourney = first2Args.equalsIgnoreCase(SmashWorldInteractor.TOURNEY_WORLD_ITEM_PREFIX);
                }
                if (askingForFreeplay || askingForTourney)
                {
                    try
                    {
                        int worldNum = Integer.valueOf(args[2]);
                        if (askingForFreeplay)
                        {
                            destination = getFreeplayWorld(worldNum);
                        }
                        else
                        {
                            destination = getTourneyWorld(worldNum);
                        }
                    }
                    catch (NumberFormatException ex)
                    {
                        sender.sendMessage(ChatColor.RED + "Please enter a valid game number.");
                        return true;
                    }
                }
                else if (FALLBACK_WORLD == null || !worldName.equalsIgnoreCase(FALLBACK_WORLD.getName()))
                {
                    destination = Bukkit.getWorld(smashifyArgument(worldName));
                }

                if (destination == null)
                {
                    String message = "";
                    for (int i = 0; i < args.length; i++)
                    {
                        if (i > 0)
                        {
                            message += " ";
                        }
                        message += args[i];
                    }
                    sender.sendMessage(ChatColor.RED + "World " + message + " not found!");
                }
                else
                {
                    sendPlayerToWorld((Player)sender, destination);
                }
            }
            return true;
        }
        else if (plugin.matchesCommand(label, SET_DEATHMATCH_PREFERENCE_CMD))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage(plugin.loggerPrefix() + "You are not a player, therefore you cannot change your deathmatch preference!");
            }
            else
            {
                Player p = (Player)sender;
                String setting = SmashWorldInteractor.getDeathMatchPreference(p);
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

                    if (plugin.isTrue(args[0]))
                    {
                        newPreference = true;
                    }
                    else if (plugin.isFalse(args[0]))
                    {
                        newPreference = false;
                    }
                    else
                    {
                        return false;
                    }
                }
                SmashWorldInteractor.setDeathMatchPreference(p, newPreference, newLivesPreference);
            }
            return true;
        }
        else if (plugin.matchesCommand(label, GET_MODE_CMD))
        {
            if (!(sender instanceof Player) || args.length > 0 && args[0].equalsIgnoreCase("help"))
            {
                return false;
            }
            else if (!isSmashWorld(((Player)sender).getWorld()) && args.length == 0)
            {
                sender.sendMessage(ChatColor.YELLOW + "You are not in a Smash world!");
                return true;
            }
            World w;
            String yourThat;
            if (args.length > 0)
            {
                args[0] = smashifyArgument(args[0]);
                w = Bukkit.getWorld(args[0]);
                if (w == null)
                {
                    sender.sendMessage(ChatColor.RED + "World not found!");
                    return true;
                }
                else if (!isSmashWorld(w))
                {
                    sender.sendMessage(ChatColor.RED + "That is not a Smash world.");
                    return true;
                }
                yourThat = "That";
            }
            else
            {
                w = ((Player)sender).getWorld();
                yourThat = "Your";
            }
            SmashWorldInteractor.determineDeathMatchPreference(w);
            String tentativelyCurrently;
            int lives = SmashWorldInteractor.getDeathMatchLives(w);
            if (!gameHasStarted(w))
            {
                tentativelyCurrently = "tentatively ";
            }
            else
            {
                tentativelyCurrently = "";
            }
            String gameType;
            if (lives == 0)
            {
                gameType = "point-based games";
            }
            else
            {
                gameType = "deathmatch games with " + SmashWorldInteractor.getDeathMatchLives(w) + " lives";
            }
            sender.sendMessage(ChatColor.YELLOW + yourThat + " world " + tentativelyCurrently + "prefers " + gameType + ".");
            return true;
        }
        else if (plugin.matchesCommand(label, SPECTATE_COMMAND))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage(ChatColor.RED + "I'd just like to ask if you can actually see.");
            }
            else if (args.length > 0 && args[0].equalsIgnoreCase("help"))
            {
                return false;
            }
            else
            {
                Player p = (Player)sender;
                World w = p.getWorld();
                if (!isSmashWorld(w) && !SmashWorldInteractor.hasSpecialPermissions(p))
                {
                    p.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "You're not in a Smash world!");
                }
                else if (isSmashWorld(w) && SmashWorldInteractor.isInSpectatorMode(p) && !SmashWorldInteractor.canBeJoinedRightNow(p, w) )
                {
                    p.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "You cannot join this game.");
                }
                else
                {
                    SmashWorldInteractor.setSpectatorMode(p, p.getWorld().getPlayers(), !SmashWorldInteractor.isInSpectatorMode(p));
                    if (!SmashWorldInteractor.isInSpectatorMode(p))
                    {
                        SmashWorldInteractor.handleJoin(p, p.getWorld(), false, true);
                    }
                    else
                    {
                        SmashWorldInteractor.handleLeave(p, p.getWorld(), false, true);
                    }
                }
            }
            return true;
        }
        else if (plugin.matchesCommand(label, READY_UP_CMD))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage(ChatColor.YELLOW + "You're not a player, nub!");
            }
            else
            {
                Player p = (Player)sender;
                World whereToGo = null;
                if (!isSmashWorld(((Player)sender).getWorld()))
                {
                    whereToGo = getBestWorld(0, p.getWorld());
                    if (whereToGo != null)
                    {
                        sender.sendMessage(ChatColor.YELLOW + "You're not in a Smash world! Sending you to one now!");
                        sendPlayerToWorld(p, whereToGo);
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.YELLOW + "Neither were you in a Smash world nor were we able to make one for you!");
                        return true;
                    }
                }
                World w = p.getWorld();
                if (whereToGo != null)
                {
                    w = whereToGo;
                }
                if (isSmashWorld(w) && !gameHasStarted(w))
                {
                    if (!SmashWorldInteractor.getReadyPlayers().containsKey(w))
                    {
                        SmashWorldInteractor.getReadyPlayers().put(w, new ArrayList<Player>());
                    }
                    boolean wasAlreadyReady = SmashWorldInteractor.getReadyPlayers().get(w).contains(p);
                    if (!wasAlreadyReady)
                    {
                        SmashWorldInteractor.getReadyPlayers().get(w).add(p);
                    }
                    int whoIsReady = SmashWorldInteractor.getReadyPlayers().get(w).size();
                    if (whoIsReady >= FREE_PLAYER_REQUIREMENT && (float)whoIsReady/w.getPlayers().size() >= READY_PERCENT_TO_START)
                    {
                        startGame(w);
                    }
                    else if (!wasAlreadyReady)
                    {
                        p.sendMessage(ChatColor.GREEN + "You are now ready!");
                    }
                    else
                    {
                        p.sendMessage(ChatColor.GREEN + "You were already ready, cap'n!");
                    }
                }
            }
            return true;
        }
        else if (plugin.matchesCommand(label, VOTE_KICK_CMD))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage(ChatColor.RED + "You must be in-game to use this command (think about it).");
            }
            else if (args.length == 0 || args[0].equalsIgnoreCase("help"))
            {
                return false;
            }
            else
            {
                Player p = (Player) sender;
                World w = p.getWorld();
                Player whoYouDontLike = Bukkit.getPlayer(args[0]);
                if (!isSmashWorld(p.getWorld()))
                {
                    sender.sendMessage(ChatColor.RED + "You aren't in a Smash world!");
                }
                else if (whoYouDontLike == null || !whoYouDontLike.getWorld().equals(w) || SmashWorldInteractor.isInSpectatorMode(whoYouDontLike))
                {
                    sender.sendMessage(ChatColor.RED + "Player not found!");
                }
                else
                {
                    if (!SmashWorldInteractor.getWhoToKick().containsKey(p))
                    {
                        SmashWorldInteractor.getWhoToKick().put(p, new ArrayList<Player>());
                    }
                    SmashWorldInteractor.getWhoToKick().get(p).add(whoYouDontLike);

                    if (SmashWorldInteractor.isHated(p, w))
                    {
                        sendPlayerToLastWorld(whoYouDontLike);
                        SmashWorldInteractor.sendMessageToWorld(w, ChatColor.YELLOW + whoYouDontLike.getName() + " has been kicked!");
                        whoYouDontLike.sendMessage(ChatColor.RED + "You have been kicked via player vote!");
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.YELLOW + "Voted to kick " + whoYouDontLike.getName());
                    }
                }
            }
            return true;
        }
        else if (plugin.matchesCommand(label, END_GAME_CMD))
        {
            if (!(sender instanceof Player) && args.length == 0)
            {
                sender.sendMessage(ChatColor.RED + "You must specify which game to end!");
            }
            else
            {
                World w;
                String thisThat;
                if (args.length > 0)
                {
                    args[0] = smashifyArgument(args[0]);
                    w = Bukkit.getWorld(args[0]);
                    thisThat = "That";
                }
                else
                {
                    w = ((Player)sender).getWorld();
                    thisThat = "Your";
                }

                if (w == null )
                {
                    if (args.length > 0)
                    {
                        sender.sendMessage(ChatColor.RED + "World " + args[0] + " not found!");
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.RED + "Error!");
                    }
                }
                else if (!isSmashWorld(w))
                {
                    sender.sendMessage(ChatColor.RED + thisThat + " isn't a Smash world! Do /listworlds to see them!");
                }
                else if (!gameHasStarted(w) || gameHasEnded(w))
                {
                    sender.sendMessage(ChatColor.RED + thisThat + " game isn't in progress!");
                }
                else
                {
                    endGame(w, sender.getName());
                }
            }
            return true;
        }
        return false;
    }
}