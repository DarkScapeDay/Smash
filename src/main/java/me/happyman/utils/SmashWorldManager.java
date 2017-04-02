package me.happyman.utils;

import me.happyman.SmashItemDrops.ItemDropManager;
import me.happyman.SmashItemDrops.Mine;
import me.happyman.SmashItemDrops.MonsterEgg;
import me.happyman.SmashItemDrops.SmashOrb;
import me.happyman.Listeners.HitData;
import me.happyman.Listeners.SmashAttackListener;
import me.happyman.Listeners.SmashItemManager;
import me.happyman.SmashKitMgt.SmashKitManager;
import me.happyman.commands.CommandVerifier;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashWorldListener;
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
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;

public class SmashWorldManager extends CommandVerifier implements CommandExecutor
{
    public static final int HIGHEST_TOURNEY_LEVEL = 3; //not 0-indexed! - The Tournament level that is for the highest tournament level players. If people with this level win a tourney game, they win a tournament
    public static final int MIN_FREE_GAMES_TO_JOIN_TOURNEYS = 5; //The Minimum games a player needs to play to unlock tourney games
    public static final long EXPIRATION_MINUTES = 120; //The minutes that a player's tournament level lasts
    public static final float DEFAULT_FLY_SPEED = 0.075F; //The fly speed for before and after Smash games start
    //You probable shouldn't change these ones.
    public static final String SMASH_WORLD_PREFIX = "Smash-"; //The prefix of world folder names
    public static final String GOTO_WORLD_CMD = "goto"; //Sends the sender to the world if that's okay
    public static final int SEARCH_DISTANCE = 100; //The maximum distance that will be searched (in blocks) for spawn locations
    private static final int TOURNEY_TOP_PERCENT = 30; //0-100, this is the percentage of players that will act on to the next tournament level in a tournament game.
    private static final int TOURNEY_GAME_TIME = 480; //The seconds in a tourney game.
    public static final int TOURNEY_WAIT_TIME = (int)(Math.round(TOURNEY_GAME_TIME/6)); //The number of seconds before an initialized tournament game will try to start
    private static final int TOURNEY_PLAYER_REQUIREMENT = 4; //The minimum players that must join before a tournament game will start
    private static final int FREE_GAME_TIME = 300; //The seconds in a freeplay game
    public static final int FREE_WAIT_TIME = (int)(Math.round(FREE_GAME_TIME/6)); //The number of seconds before an initialized freeplay game will start
    private static final int FREE_PLAYER_REQUIREMENT = 2; //The minimum players that must join before a freeplay game will start
    private static final int END_GAME_TIME = 10; //The amount of time before a world resets after a Smash game ends
    private static final int DEATH_DISTANCE = 150; //The distance from the center at which players will be assumed to be KO'ed and a ping sound will play
    private static final int MAX_BELOW = 30; //The distance below the center which will count as a KO
    private static final int KILLER_DELAY = 7; //The ticks between each check for a player being outside the KO zone
    private static final int MOTD_PERIOD = 240; //The period between a random motd message being sent to non-started Smash worlds
    private static final int MAX_LIVES = 10; //The max lives a player can prefer for deathmatch games
    private static final float READY_PERCENT_TO_START = 0.65F;
    private static final float VOTE_KICK_PERCENT = 0.75F;
    private static final String SMASH_TEMPLATE_PREFIX = "SmashWorld"; //The prefix of map world folder names
    private static final String PLAYER_SPAWN_FILENAME = "PlayerSpawnLocations"; //The name of the player spawn location file which stores player spawn locations (without the extension)
    private static final String ITEM_SPAWN_FILENAME = "ItemSpawnLocations"; //The name of the item spawn location file which stores item spawn locations...
    private static final String FREE_WORLD_ITEM_PREFIX = "Freeplay Game "; //What items that reference Freeplay games are called (in the gui)
    private static final String TOURNEY_WORLD_ITEM_PREFIX = "Tourney Game "; //What items that reference Tourney games are called (in the gui)
    private static final int MAX_FREEPLAY_WORLDS = 30; //The max number of Freeplay games that can be created
    private static final int MAX_TOURNEY_WORLDS = 30; //The max number of Tournament games that can be created
    private static final int ROUND_LORE_INDEX = 2; //The index of the Round lore on items which tp players to tourney worlds
    private static final String TOURNEY_INDICATOR = "t"; //The indicator in a world folder name which tells you it's a tourney world
    private static final String WORLD_INDICATOR = "w"; //The indicator on all world folder names which precedes the world number
    private static final String FREEPLAY_INDICATOR = "fp"; //This tells you it's a freeplay world
    private static final String SMASH_CMD = "smash"; //Excecuting this command will open the Smash gui if that's okay
    private static final String CREATE_WORLD_CMD = "createworld"; //This creates a world
    private static final String DELETE_WORLD_CMD = "deleteworld"; //This deletes a world
    private static final String START_CMD = "start"; //This starts the game
    private static final String LEAVE_CMD = "leave"; //This causes you to leave the game you're in
    private static final String LIST_MAP_CMD = "listmaps"; //This lists the maps that you can use when creating a world
    private static final String LIST_WORLD_CMD = "listworlds"; //This lists all Smash worlds
    private static final String RESET_CMD = "reset"; //This resets a Smash game (don't use it during a game unless you're a jerk or there's a bug)
    private static final String GET_WORLD_CMD = "getworld"; //Gets the world you're in (or another player)
    private static final String DMG_CMD = "adddamage"; //Adds DMG points to a player (not hearts, DMG!)
    private static final String DEATHMATCH_PREFERENCE_DATANAME = "Deathmatch"; //The name of the data which is for remembering people's deathmatch preferences
    private static final String SET_DEATHMATCH_PREFERENCE_CMD = "dm"; //The command used to set your deathmatch preference
    private static final String GET_MODE_CMD = "mode"; //Gets the mode of the Smash world you're in (point-based or deathmatch)
    private static final String SPECTATE_COMMAND = "spectate";
    private static final String READY_UP_CMD = "ready";
    private static final String VOTE_KICK_CMD = "votekick";
    private static final String END_GAME_CMD = "end";
    private static final int SPAWN_HEIGHT = 135; //The height which is used for KO zone calculations
    private static final int SEARCH_HEIGHT = 30; //The maximum height to search for spawn locations
    private static final int HIT_TICKS = 20; //The number of ticks between each hit for players
    private static final int DOUBLE_JUMP_LAND_DELAY = 1; //The delay in ticks before your items will be recharged
    private static final int REFUEL_COOLDOWN = 12; //The delay in ticks before your items can recharge again upon landing
    private static World FALLBACK_WORLD; //The world which is used for emergencies
    //Can you tell I like HashMaps?
    private static List<World> startGameOverrides; //This is used when a staff member wants to start a game after a delay
    private static ArrayList<Player> deadPlayers; //This is for keeping track of who should be tped when they exit the KO zone but shouldn't be tped right away
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
    private static HashMap<Integer, ArrayList<Player>> openWorldGuis; //The players who have world guis open, separated into tournament levels for convenience
    private static ArrayList<String> participants; //The people in a game who have gotten a KO and therefore aren't AFK
    private static ArrayList<Integer> takenTourneyWorldNumbers; //The world numbers of tournament worlds that have been taken already
    private static HashMap<Integer, Integer> tourneyLevels; //The corresponding tournament levels of tournament world numbers
    private static ArrayList<Integer> takenFreeplayWorldNumbers; //The world numbers of freeplay worlds that have been taken
    private static ArrayList<String> worldsBeingDeleted; //The list of names of worlds that are being deleted and therefore should be left alone
    private static ArrayList<World> startedGames;
    private static HashMap<World, Integer> deathMatchWorlds; //The number of lives that was selected for a deathmatch game (or 0 if it's not a deathmatch game)
    private static HashMap<World, Integer> originalPlayerCount; //The number of players that started out in a game, used to see when a tourney game should end
    private static ArrayList<Player> spectators; //This keeps track of who all is in spectator mode
    private static HashMap<Player, PlayerList> tabLists;
    private static HashMap<World, List<Player>> readyPlayers;
    private static HashMap<Player, List<Player>> whoToKick;
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
        plugin.setExecutor(CREATE_WORLD_CMD, this);
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

        itemSpawnLocations = new HashMap<World, ArrayList<Location>>();
        playerSpawnLocations = new HashMap<World, ArrayList<Location>>();
        deadPlayers = new ArrayList<Player>();
        killerTasks = new HashMap<World, Integer>();
        startGameOverrides = new ArrayList<World>();
        timeRemainingTasks = new HashMap<World, Integer>();
        itemSpawnTasks = new HashMap<World, Integer>();
        countdownTasks = new HashMap<World, Integer>();
        lastPlayerLocations = new HashMap<Player, Location>();
        endGameTasks = new HashMap<World, Integer>();
        doubleJumpTasks = new HashMap<World, Integer>();
        participants = new ArrayList<String>();
        ticksTilNextRefuel = new HashMap<Player, Integer>();
        openWorldGuis = new HashMap<Integer, ArrayList<Player>>();
        takenTourneyWorldNumbers = new ArrayList<Integer>();
        tourneyLevels = new HashMap<Integer, Integer>();
        takenFreeplayWorldNumbers = new ArrayList<Integer>();
        worldsBeingDeleted = new ArrayList<String>();
        motdMessages = new ArrayList<String>();
        deathMatchWorlds = new HashMap<World, Integer>();
        originalPlayerCount = new HashMap<World, Integer>();
        spectators = new ArrayList<Player>();
        tabLists = new HashMap<Player, PlayerList>();
        readyPlayers = new HashMap<World, List<Player>>();
        whoToKick = new HashMap<Player, List<Player>>();
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
                for (int i = 0; i <= HIGHEST_TOURNEY_LEVEL; i++)
                {
                    openWorldGuis.put(i, new ArrayList<Player>());
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
                        setSpectatorMode(p, w.getPlayers(), false);
                    }
                }
                for (Player p : playerList)
                {
                    getTabList(p).clearCustomTabs();
                    giveSmashGuiOpener(p);
                    if (hasWorldGuiOpen(p))
                    {
                        openWorldGuis.get(SmashStatTracker.getTourneyLevel(p)).add(p);
                        refreshWorldGui(p);
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
                        sendMessageToWorld(w, message, false);
                    }
                }
            }
        }, MOTD_PERIOD*10, MOTD_PERIOD*20);
    }

    public static void performDisable()
    {
        for (Player p : Bukkit.getOnlinePlayers())
        {
            getTabList(p).clearCustomTabs();
        }
    }

    public static void addTabEntry(Player p, String entry)
    {
        entry = ChatColor.stripColor(entry);
        PlayerList playerList = getTabList(p);
        List<String> oldTabs = playerList.getCustomTabs();
        if (!oldTabs.contains(entry) && oldTabs.size() < 20)
        {
            int j = oldTabs.size();
            oldTabs.add(entry);
            for (int i = 0; i < oldTabs.size(); i++)
            {
                while (playerList.getTabName(j) != null)
                {
                    j++;
                }
                if (j < 20)
                {
                    playerList.updateSlot(j, getSpectralEntry(entry));
                }
            }
        }
    }

    private static String getSpectralEntry(String entry)
    {
        return ChatColor.GRAY + "" + ChatColor.ITALIC + entry;
    }

    private static PlayerList getTabList(Player p)
    {
        if (!tabLists.containsKey(p))
        {
            tabLists.put(p, new PlayerList(p, PlayerList.SIZE_DEFAULT));
        }
        return tabLists.get(p);
    }

    public static void removeTabEntry(Player p, String entry)
    {
        entry = ChatColor.stripColor(entry);
        PlayerList playerList = getTabList(p);
        List<String> oldTabs = playerList.getCustomTabs();
        List<String> tabNames = new ArrayList<String>();
        for (int i = 0; i < playerList.getCustomTabs().size(); i++)
        {
            tabNames.add(playerList.getTabName(i));
        }
        if (oldTabs.contains(entry))
        {
            playerList.clearCustomTabs();
            int j = 0;
            for (int i = 0; i < oldTabs.size(); i++)
            {
                while (j < tabNames.size() - 1 && tabNames.get(j) == null)
                {
                    j++;
                }
                if (!oldTabs.get(i).equals(entry))
                {
                    playerList.updateSlot(j, getSpectralEntry(oldTabs.get(i)));
                }
                j++;
            }
        }
    }

    public static void addAllTabEntries(Player p)
    {
        for (Player online : Bukkit.getOnlinePlayers())
        {
            if (!isInSpectatorMode(online))
            {
                addTabEntry(online, p.getName());
            }
        }
    }

    public static void removeAllTabEntries(Player p)
    {
        for (Player online : Bukkit.getOnlinePlayers())
        {
            if (!isInSpectatorMode(online))
            {
                removeTabEntry(online, p.getName());
            }
        }
    }

    /**
     * Function: getOriginalPlayerCount
     * Purpose: To see how many players were originally in a deathmatch game so we know when to end a tournament game
     * @param w - The world we would like to inquire of the original player count
     * @return - The original number of players in that world or null if we don't know or shouldn't have called the function
     */
    public static Integer getOriginalPlayerCount(World w)
    {
        if (originalPlayerCount.containsKey(w))
        {
            return originalPlayerCount.get(w);
        }
        return null;
    }

    /**
     * Function: rememberOriginalPlayerCount
     * Purpose: To record how many players were in the world at the start of a tournament game
     * @param w - The world for which to record the original player count
     */
    public static void rememberOriginalPlayerCount(World w)
    {
        originalPlayerCount.put(w, getLivingPlayers(w).size());
    }

    /**
     * Clears the original player count record from memory
     * @param w - The world for which we don't need to know the original player count for any more
     */
    public static void forgetOriginalPlayerCount(World w)
    {
        if (originalPlayerCount.containsKey(w))
        {
            originalPlayerCount.remove(w);
        }
    }

    /**
     * Gets the deathmatch preference for the player in format "<preference> <lives>" from his player data file. It sets his preference to default if there wasn't already one.
     * @param p - The player for which we would like to fetch the deathmatch preference
     * @return - The deathmatch preference of the player as string
     */
    public static String getDeathMatchPreference(Player p)
    {
        String preference = plugin.getPluginDatum(p, DEATHMATCH_PREFERENCE_DATANAME);
        Scanner s = new Scanner(preference);
        if (preference.length() == 0 || !s.hasNextBoolean())
        {
            preference = "false 4";
            plugin.putPluginDatum(p, DEATHMATCH_PREFERENCE_DATANAME, preference);
        }
        else
        {
            boolean dm = s.nextBoolean();
            if (!s.hasNextInt())
            {
                preference = dm + " 4";
                plugin.putPluginDatum(p, DEATHMATCH_PREFERENCE_DATANAME, preference);
            }
        }
        return preference;
    }

    /**
     * Function: setDeathMatchPreference
     * Purpose: To set the deathmatch preference of a player
     * @param p - The player for which we would like to update PREFERENCE
     * @param preference - The boolean of whether or not the player prefers deathmatch games
     * @param lives - The lives of the player's deathmatch preference
     */
    public static void setDeathMatchPreference(Player p, boolean preference, int lives)
    {
        if (preference)
        {
            String display = "ves";
            if (lives == 1)
            {
                display = "fe";
            }
            p.sendMessage(ChatColor.GREEN + "You now prefer Deathmatch games with " + lives + " li" + display + ".");
            plugin.putPluginDatum(p, DEATHMATCH_PREFERENCE_DATANAME, "true " + lives);
        }
        else
        {
            p.sendMessage(ChatColor.GREEN + "You now prefer Point-based games.");
            if (lives < 3)
            {
                lives = 3;
            }
            plugin.putPluginDatum(p, DEATHMATCH_PREFERENCE_DATANAME, "false " + lives);
        }
    }

    /**
     * Function: determineDeathMatchPreference
     * Purpose: Calculated whether a world is a deathmatch world or not, and if so how many lives
     * @param w - The world to be situated for its deathmatch preference
     */
    private static void determineDeathMatchPreference(World w)
    {
        if (getLivingPlayers(w).size() == 0)
        {
            deathMatchWorlds.put(w, 0);
        }
        else if (!gameHasStarted(w))
        {
            int deathMatchPreferers = 0;
            int livesSum = 0;
            for (Player p : getLivingPlayers(w))
            {
                String setting = getDeathMatchPreference(p);

                Scanner s = new Scanner(setting);
                boolean prefersDms = s.nextBoolean();
                int dmLevel = s.nextInt();

                if (prefersDms)
                {
                    deathMatchPreferers++;
                }
                livesSum += dmLevel;
            }
            int playerCount = getLivingPlayers(w).size();
            float percentage = 1.0F*deathMatchPreferers/playerCount;
            boolean deathMatch = percentage > 0.5 || percentage == 0.5 && getLivingPlayers(w).size() > 2; //This can be >= if you're okay with 50% counting as majority

            if (deathMatch)
            {
                deathMatchWorlds.put(w, (int)Math.round(1.0*livesSum/getLivingPlayers(w).size()));
            }
            else
            {
                deathMatchWorlds.put(w, 0);
            }
        }
    }

    /**
     * Function: isDeathMatchWorld
     * Purpose: Determines whether or not the world is a deathmatch world
     * @param w - The world to be checked for being a deathmatch world
     * @return - True if the world is currently a deathmatch world
     */
    public static boolean isDeathMatchWorld(World w)
    {
        if (!deathMatchWorlds.containsKey(w))
        {
            determineDeathMatchPreference(w);
        }
        return deathMatchWorlds.get(w) != 0;
    }

    /**
     * Function: getDeathMatchLives
     * Purpose: To determine the current deathmatch lives of the given world
     * @param w - The world for which we would like to know the current deathmatch lives
     * @return - The number of lives that the world prefers for deathmatches, or 0 if it doesn't actually prefer deathmatches (error)
     */
    public static int getDeathMatchLives(World w)
    {
        if (isDeathMatchWorld(w))
        {
            return deathMatchWorlds.get(w);
        }
        return 0;
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
     * Function: isStartingSoon
     * Purpose: To determine if a game is about start. There's no check for the game already being started, so keep that in mind.
     * @param w - The world we would like to check for being about to start
     * @return - True if someone did /start or if there are enough players for the game to start
     */
    public static boolean isStartingSoon(World w)
    {
        return startGameOverrides.contains(w) || getStartRequirement(w) != null && getLivingPlayers(w).size() >= getStartRequirement(w);
    }

    public static void removeStartOverride(World w)
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
    public static World getFallbackWorld()
    {
        return FALLBACK_WORLD;
    }

    /**
     * Function: getLastPlayerLoction
     * Purpose: To determine the loction from which a player logged into a Smash world
     * @param p - The player for which we want to know his last location
     * @return - The location that the player was at before joining a Smash game
     */
    public static Location getLastPlayerLocation(Player p)
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
    private static boolean isTourneyWorld(String worldName)
    {
        return worldName.length() >= SMASH_WORLD_PREFIX.length() + TOURNEY_INDICATOR.length()
                && worldName.substring(SMASH_WORLD_PREFIX.length(), SMASH_WORLD_PREFIX.length() + TOURNEY_INDICATOR.length()).equals(TOURNEY_INDICATOR);
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
                    for (Player p : getLivingPlayers(w))
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
     * Function: playSoundToPlayers
     * Purpose: To play a certain sound of a certain pitch and volume in the relative directoin of the sound source player and a certain number of times with a certain delay between those times (just trust me, it's cool)
     * @param playerToExclude - The player who is "generating" the sound
     * @param players - The list of players to whom to play the sound (typically all the players in a Smash world). This can include the sound source player.
     * @param sound - The sound that we want to play to the players
     * @param volume - The volume of the sound (max is 1)
     * @param pitch - The pitch of the sound (max is 1)
     * @param ticksBetweenCalls - The number of ticks between sound calls
     * @param timesToCall - The number of times that the sound should be played
     */
    public static void playSoundToPlayers(final Player playerToExclude, Location soundSourceLocation, final List<Player> players, final Sound sound, final float volume, final float pitch, final int ticksBetweenCalls, final int timesToCall)
    {
        for (final Player player : players)
        {
            Location soundLocation = player.getLocation();
            if (playerToExclude == null || !player.equals(playerToExclude))
            {
                Vector relativeDirection = SmashManager.getUnitDirection(player.getLocation(), soundSourceLocation);
                int newDistance = 7; //The number of blocks away we want the sound to be
                soundLocation.setX(soundLocation.getX() + relativeDirection.getX() * newDistance);
                soundLocation.setY(soundLocation.getY() + relativeDirection.getY() * newDistance);
                soundLocation.setZ(soundLocation.getZ() + relativeDirection.getZ() * newDistance);
            }
            final Location actualLocation = soundLocation;
            for (int i = 0; i < timesToCall; i++)
            {
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run()
                    {
                        player.playSound(actualLocation, sound, volume, pitch);
                    }
                }, i*ticksBetweenCalls);
            }
        }
    }

    /**
     * Function: playSoundToPlayers
     * Purpose: To play a certain sound of a certain pitch and volume in the relative direction of the sound source player (just trust me, it's cool)
     * @param playerToExclude - The player who is "generating" the sound
     * @param players - The list of players to whom to play the sound (typically all the players in a Smash world). This can include the sound source player.
     * @param sound - The sound that we want to play to the players
     * @param volume - The volume of the sound (max is 1)
     * @param pitch - The pitch of the sound (max is 1)
     */
    public static void playSoundToPlayers(Player playerToExclude, List<Player> players, Sound sound, float volume, float pitch)
    {
        playSoundToPlayers(playerToExclude, playerToExclude.getLocation(), players, sound, volume, pitch, 0, 1);
    }

    public static void playSoundToPlayers(List<Player> players, Location soundSourceLocation, Sound sound, float volume, float pitch)
    {
        playSoundToPlayers(null, soundSourceLocation, players, sound, volume, pitch, 0, 1);
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
                    for (final Player p : getLivingPlayers(w))
                    {
                        boolean superFar = p.getLocation().distance(p.getWorld().getSpawnLocation()) > DEATH_DISTANCE;
                        if ((superFar || p.getLocation().getY() < SPAWN_HEIGHT - MAX_BELOW || p.getLocation().getY() < -50) && !deadPlayers.contains(p))
                        {
                            deadPlayers.add(p);
                            int tpDelay = 0;
                            Vector v = p.getVelocity();
                            if (superFar)
                            {
                                playSoundToPlayers(p, w.getPlayers(), Sound.ORB_PICKUP, 0.7F, 0.95F);
                                tpDelay = 15;
                            }
                            else if (SmashManager.getMagnitude(v.getX(), v.getY(), v.getZ()) > 3.2)
                            {
                                playSoundToPlayers(p, p.getLocation(), w.getPlayers(), Sound.EXPLODE, (float)1, (float)0.04, 1, 6);
                                playSoundToPlayers(p, w.getPlayers(), Sound.BLAZE_DEATH, (float)0.1, (float)0.05);
                                tpDelay = 20;
                            }
                            Runnable r = getRespawnRunnable(p);
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

                                sendMessageToWorld(w, ChatColor.GOLD + koMessage);
                                SmashScoreboardManager.setPlayerScoreValue(p, SmashScoreboardManager.getPlayerScoreValue(p) - 1);

                                if (SmashManager.hasLastHitter(p.getName()))
                                {
                                    String lastHitter = SmashManager.getLastHitterName(p.getName());
                                    if (!lastHitter.equals(p.getName()) && !isDeathMatchWorld(w))
                                    {
                                        SmashScoreboardManager.setPlayerScoreValue(lastHitter, w, SmashScoreboardManager.getPlayerScoreValue(lastHitter, w) + 1);
                                    }
                                }

                                if (isDeathMatchWorld(w))
                                {
                                    if (SmashScoreboardManager.getPlayerScoreValue(p) == 0)
                                    {
                                        performDeathMatchDeath(p, w, false);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        };
    }

    private static void performDeathMatchDeath(final Player p, final World w, final boolean quitter)
    {
        if (isSmashWorld(w) && gameHasStarted(w) && !gameHasEnded(w) &&
                SmashScoreboardManager.hasScoreboard(w) && SmashScoreboardManager.getScoreboardEntries(w).contains(SmashScoreboardManager.getPlayerEntryName(p)))
        {
            SmashStatTracker.performEloChange(p.getName(), w, getDeathMatchLives(w));
            SmashScoreboardManager.removeEntry(w, SmashScoreboardManager.getPlayerEntryName(p));
            if (!quitter)
            {
                handleLeave(p, p.getWorld(), false, true);
                sendMessageToWorld(w, ChatColor.GOLD + p.getName() + ChatColor.RED + "" + ChatColor.BOLD + " DEFEATED");
                if (isTourneyWorld(w))
                {
                    SmashStatTracker.incrementTourneyRoundsPlayed(p);
                    if (getTourneyLevel(w) == HIGHEST_TOURNEY_LEVEL)
                    {
                        SmashStatTracker.incrementTourneyGamesPlayed(p);
                    }
                }
                else
                {
                    SmashStatTracker.incrementFreeGamesPlayed(p);
                }
            }
            int remainingPlayers = 0;
            for (Player player : w.getPlayers())
            {
                if (SmashScoreboardManager.isOnScoreboard(player) && SmashScoreboardManager.getPlayerScoreValue(player) > 0)
                {
                    remainingPlayers++;
                }
            }
            final int rf = remainingPlayers;
            Bukkit.getScheduler().callSyncMethod(plugin, new Callable() {
                public String call() {
                    if (rf <= 1 || isTourneyWorld(w) && rf <= 1.0*TOURNEY_TOP_PERCENT/100*getOriginalPlayerCount(w))
                    {
                        endGame(w);
                    }
                    else if (!quitter)
                    {
                        setSpectatorMode(p, w.getPlayers(), true);
                    }
                    return "";
                }
            });
        }
    }

    /**
     * Function: recordKOStatistics
     * Purpose: To record the kill, death, elo change, etc resulting from a KO in a smash world
     * @param deadPlayer - The player that has unfortunately died... I mean been KOed
     */
    public static void recordKOStatistics(final Player deadPlayer, World w)
    {
            if (isSmashWorld(w) && gameHasStarted(w) && !gameHasEnded(w))
            {
                if (SmashManager.hasLastHitter(deadPlayer.getName()))
                {
                    String participant = SmashManager.getLastHitter(deadPlayer.getName()).killerName;
                    plugin.incrementPluginStatistic(participant, SmashStatTracker.KO_DEALT_SCORE);
                    plugin.incrementPluginStatistic(deadPlayer, SmashStatTracker.KO_RECEIVED_SCORE);
                    if (!isDeathMatchWorld(w))
                    {
                        SmashStatTracker.performEloChange(participant, deadPlayer.getName());
                    }
                    if (!participants.contains(participant))
                    {
                        participants.add(participant);
                    }
                }
                else
                {
                    if (!isDeathMatchWorld(w))
                    {
                        SmashStatTracker.performEloChange(deadPlayer.getName(), w);
                    }
                    plugin.incrementPluginStatistic(deadPlayer, SmashStatTracker.FALLEN_OUT_SCORE);
                }
            }
    }

    /**
     * Function: getRespawnRunnable
     * Purpose: Gets the runnable for respawning a Smash player (essentially killing them)
     * @param deadPlayer - The player who has DIED
     * @return - The runnable for respawning a player and recording the stats associated with the unfortunate event
     */
    private static Runnable getRespawnRunnable(final Player deadPlayer)
    {
        return new Runnable()
        {
            public void run()
            {
                recordKOStatistics(deadPlayer, deadPlayer.getWorld());
                respawnTeleport(deadPlayer);
            }
        };
    }

    /**
     * Function: respawnTeleport
     * Purpose: Respawns a player, but doesn't record KO statistics
     * @param p - The player we want to respawn
     */
    public static void respawnTeleport(final Player p)
    {
        SmashItemManager.cancelItemTasks(p);
        SmashKitManager.restoreAllUsagesAndCharges(p, false);
        SmashManager.clearDamage(p);
        SmashAttackListener.cancelKB(p);
        p.setFireTicks(-20);
        if (deadPlayers.contains(p))
        {
            deadPlayers.remove(p);
        }
        //resetToKitItems(Player p);
        SmashEntityTracker.setSpeedToZero(p);
        for (ItemStack item : p.getInventory().getContents())
        {
            if (item != null)
            {
                if (!SmashKitManager.getSelectedKit(p).hasItem(item) || SmashKitManager.getSelectedKit(p).getProperties().getFinalSmash().isThis(item))
                {
                    p.getInventory().remove(item);
                }
            }
        }
        p.getInventory().setArmorContents(SmashKitManager.getSelectedKit(p).getArmor());

        allowFullflight(p, false);
        p.teleport(getRandomPlayerSpawnLocation(p.getWorld()));
        SmashManager.forgetLastHitter(p);
        Bukkit.getScheduler().callSyncMethod(plugin, new Callable() {
            public String call() {
                if (Bukkit.getOnlinePlayers().contains(p))
                {
                    if (!gameHasEnded(p.getWorld()) && !isInSpectatorMode(p))
                    {
                        p.setAllowFlight(false);
                    }
                }
                return "";
            }
        });
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

        if (plugin.hasFile(w.getName(), PLAYER_SPAWN_FILENAME) && plugin.hasFile(w.getName(), ITEM_SPAWN_FILENAME))
        {
            for (List<String> line : plugin.getAllData(w.getName(), PLAYER_SPAWN_FILENAME))
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
            for (List<String> line : plugin.getAllData(w.getName(), ITEM_SPAWN_FILENAME))
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
        else if (plugin.hasFile(w.getName(), PLAYER_SPAWN_FILENAME) ^ plugin.hasFile(w.getName(), ITEM_SPAWN_FILENAME))
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
                plugin.printLinesToFile(w.getName(), PLAYER_SPAWN_FILENAME, dataList);


                dataList = new ArrayList<String>();
                for (int i = 0; i < itemSpawnLocations.get(w).size(); i++)
                {
                    Location l = itemSpawnLocations.get(w).get(i);
                    dataList.add(String.format("l%1$d: %2$d %3$d %4$d", i, l.getBlockX(), l.getBlockY(), + l.getBlockZ()));
                }
                plugin.printLinesToFile(w.getName(), ITEM_SPAWN_FILENAME, dataList);
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
        return (new Random()).nextFloat() < getLivingPlayers(w).size() * ItemDropManager.ITEMSPAWN_CHANCE_PER_PERIOD_PER_PLAYER;
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
     * Function: updateHitCooldown
     * Purpose: Updates the hit cooldown for everyone in the given world
     * @param w - The world for which we would like to update everyone's hit cooldown
     */
    public static void updateHitCooldown(World w)
    {
        for (Player p : w.getPlayers())
        {
            updateHitCooldown(p);
        }
    }

    /**
     * Function: getStartRequirement
     * Purpose: Gets the number of players that is needed for a game to start naturally
     * @param w - The world for which we would like to determine how many players are needed
     * @return - The number of players that is needed for the game to start
     */
    public static Integer getStartRequirement(World w)
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
    public static void startGame(World w)
    {
        determineDeathMatchPreference(w);
        if (isDeathMatchWorld(w) && getLivingPlayers(w).size() <= 1)
        {
            sendMessageToWorld(w, ChatColor.RED + "This game didn't start because it was a deathmatch game with only one player.");
            SmashScoreboardManager.resetStartTime(w);
        }
        else if (getLivingPlayers(w).size() == 0)
        {
            sendMessageToWorld(w, ChatColor.RED + "This game didn't start because there was no one in it.");
            SmashScoreboardManager.resetStartTime(w);
        }
        else
        {
            if (!startedGames.contains(w))
            {
                startedGames.add(w);
            }
            readyPlayers.remove(w);
            rememberOriginalPlayerCount(w);
            updateHitCooldown(w);
            removeStartOverride(w);
            cancelCountdown(w);
            SmashScoreboardManager.clearEntries(w);

            if (!isDeathMatchWorld(w))
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
                itemSpawnTasks.put(w, plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, r1, Math.round(1.0/(getLivingPlayers(w).size() * ItemDropManager.ITEMSPAWN_CHANCE_PER_PERIOD_PER_PLAYER) * ItemDropManager.ITEMSPAWN_PERIOD / 2), ItemDropManager.ITEMSPAWN_PERIOD));
            }
            else
            {
                plugin.sendErrorMessage("We were already spawning itemDrops!");
            }

            for (Player p : w.getPlayers())
            {
                if (!isInSpectatorMode(p))
                {
                    p.setAllowFlight(false);
                    //SmashScoreboardManager.addPlayerToScoreBoard(p);
                    SmashScoreboardManager.setPlayerScoreValue(p, getDeathMatchLives(w));
                    respawnTeleport(p);
                }
                if (isDeathMatchWorld(w))
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

    private static void endGame(World w)
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
                if (isInSpectatorMode(p))
                {
                    handleJoin(p, w, false, true);
                    setSpectatorMode(p, w.getPlayers(), false);
                }
            }
            if (whoForcedClose != null && isDeathMatchWorld(w))
            {
                sendMessageToWorld(w, ChatColor.RED + "" + ChatColor.BOLD + "The game was forcefully ended by " + whoForcedClose + "!");
                restartWorld(w);
            }
            else
            {
                for (String p : SmashScoreboardManager.getScoreboardEntries(w))
                {
                    if ((!isDeathMatchWorld(w) || SmashScoreboardManager.getPlayerScoreValue(p, w) > 0) && !scoreboardPlayerList.contains(ChatColor.stripColor(p)))
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
                    if (isTourneyWorld && getTourneyLevel(w) == HIGHEST_TOURNEY_LEVEL)
                    {
                        minContinueScore = scores.get(0);
                    }
                    else
                    {
                        while ((!isTourneyWorld || isTourneyWorld && scores.size() > SmashScoreboardManager.getScoreboardEntries(w).size()*TOURNEY_TOP_PERCENT/100 + 1) && scores.size() > 1)
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
                else if (isDeathMatchWorld(w))
                {
                    endMessage = ChatColor.GREEN + "" + ChatColor.BOLD +  "GAME!";
                }
                else
                {
                    endMessage = ChatColor.GREEN + "" + ChatColor.BOLD +  "TIME!";
                }
                sendMessageToWorld(w, endMessage);
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
                            SmashStatTracker.incrementTourneyRoundsPlayed(getLivingPlayers(w));
                        }
                    }
                    if (getTourneyLevel(w) < 1)
                    {
                        plugin.sendErrorMessage("Error! World " + w.getName() + " has a negative tournament level!");
                    }
                    else if (getTourneyLevel(w) < HIGHEST_TOURNEY_LEVEL - 1)
                    {
                        s = ChatColor.GREEN + playersString + " " + moveMoves + " to Round " + (getTourneyLevel(w) + 1) + "!";
                    }
                    else if (getTourneyLevel(w) < HIGHEST_TOURNEY_LEVEL)
                    {
                        s = ChatColor.GREEN + playersString + " " + moveMoves + " to the Final Round!";
                    }
                    else if (getTourneyLevel(w) == HIGHEST_TOURNEY_LEVEL)
                    {
                        SmashStatTracker.incrementTourneyGamesPlayed(getLivingPlayers(w));
                        messageEverybody = true;
                        s = ChatColor.GREEN + playersString + " " + hasHave + " won Smash Tournament #" + plugin.incrementPluginStatistic(plugin.SETTING_FOLDER, "Stats", "Tournaments") + "!";
                    }
                }
                else
                {
                    delay = END_GAME_TIME/3 * 20;
                    removeGame(w, false, true, true);
                    SmashScoreboardManager.removeScoreboardIfThere(w);
                    sendMessageToWorld(w, ChatColor.YELLOW + "And the winner is...");
                    for (Player p : w.getPlayers())
                    {
                        if (scoreboardPlayerList.contains(p.getName()))
                        {
                            SmashStatTracker.incrementFreeGamesWon(p);
                        }
                    }

                    SmashStatTracker.incrementFreeGamesPlayed(getLivingPlayers(w));
                    if (scoreboardPlayerList.size() == 1)
                    {
                        s = ChatColor.GREEN + playersString + "!";
                    }
                    else
                    {
                        s = ChatColor.GREEN + playersString + "!";
                    }
                }
                sendMessageWithDelay(s, w, messageEverybody, delay);

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
                                    halfwaySpeeds.put(p, participants.contains(p.getName()));
                                    if (participants.contains(p.getName()))
                                    {
                                        participants.remove(p.getName());
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
    private static void removeGame(World w, boolean deletingWorld, boolean removingForRestart, boolean removingForEndGame)
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
        forgetOriginalPlayerCount(w);
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
                        allowFullflight(p, true);
                        return "";
                    }
                });
            }
        }
    }

    /**
     * Function: allowFullflight
     * Purpose: To set a player to have flight turned
     *
     * @param p - The player to allow flight for
     * @param setFlying - Do you want to put the player into flight mode right away?
     */
    public static void allowFullflight(final Player p, final boolean setFlying)
    {
        p.setAllowFlight(true);
        if (p.getFlySpeed() != DEFAULT_FLY_SPEED && (!gameHasStarted(p.getWorld()) || isInSpectatorMode(p)))
        {
            p.setFlySpeed(DEFAULT_FLY_SPEED);
        }

        if (setFlying && ((Entity)p).isOnGround())
        {
            Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
                public String call()
                {
                    Location l = p.getLocation().clone();
                    l.add(0, 0.01, 0);
                    p.teleport(l);
                    return "";
                }
            });
        }

        p.setFlying(setFlying);
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
            setSpectatorMode(p, w.getPlayers(), false);
        }
        sendMessageOutsideWorld(w, ChatColor.GREEN + "World " + getShortWorldName(w) + " has been restarted!");
        startWorld(w);
    }

    /**
     * Function: startWorld
     * Purpose: To initialize a world's scoreboard and to initialize its players, along with starting necessary Smash world tasks
     *
     * @param w - The world to be initialized
     */
    public static void startWorld(World w)
    {
        if (isSmashWorld(w))
        {
            w.setPVP(false);
            SmashScoreboardManager.initializeScoreboard(w);
            for (Player p : getLivingPlayers(w))
            {
                initializeSmashPlayer(p);
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
     * Function: sendMessageWithDelay
     * Purpose: To send a message to a world or the whole server with a certain delay in ticks
     *
     * @param message - The String message to be sent
     * @param w - The world to which to send the message
     * @param messageAll - Do you want to message the whole server?
     * @param delay - The delay in ticks before the message will be sent
     */
    public static void sendMessageWithDelay(final String message, final World w, boolean messageAll, int delay)
    {
        if (delay == 0)
        {
            if (messageAll)
            {
                for (World world : Bukkit.getWorlds())
                {
                    sendMessageToWorld(world, message);
                }
            }
            else
            {
                sendMessageToWorld(w, message);
            }
        }
        else
        {
            if (messageAll)
            {
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run()
                    {
                        for (World world : Bukkit.getWorlds())
                        {
                            sendMessageToWorld(world, message);
                        }
                    }
                }, delay);
            }
            else
            {
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                    public void run()
                    {
                        sendMessageToWorld(w, message);
                    }
                }, delay);
            }
        }
    }

    /**
     * Function: sendMessageToWorld
     * Purpose: To send a message to all players in a world
     *
     * @param w - The world to which to send the message
     * @param message - The string message to be sent
     */
    public static void sendMessageToWorld(World w, String message, boolean sendToAfks)
    {
        for (Player p : w.getPlayers())
        {
            if (sendToAfks || !SmashEntityTracker.isHoldingStill(p))
            {
                p.sendMessage(message);
            }
        }
    }

    public static void sendMessageToWorld(World w, String message)
    {
        sendMessageToWorld(w, message, true);
    }

    public static void sendMessageOutsideWorld(World w, String message)
    {
        for (Player p : Bukkit.getOnlinePlayers())
        {
            World playerWorld = p.getWorld();
            if (!p.getWorld().equals(w) && !(isSmashWorld(playerWorld) && gameHasStarted(playerWorld)))
            {
                p.sendMessage(message);
            }
        }
    }

    /**
     * Function: startStartCountDown
     * Purpose: To start the countdown for the time remaining til start for a Smash world
     *
     * @param w - The world for which we would like to decrement the time remaining by 1 each second
     */
    public static void startStartCountDown(final World w)
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
    public static void cancelCountdown(World w)
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
    public static void cancelTime(World w)
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
     * Function: refreshWorldGui
     * Purpose: To refresh a player's gui so that he can see relavent worlds
     *
     * @param p - The play of whom to refresh the gui
     */
    private static void refreshWorldGui(final Player p)
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run()
            {
                p.openInventory(getWorldGui(p));
                Bukkit.getScheduler().callSyncMethod(plugin, new Callable() {
                    public String call()
                    {
                        p.updateInventory();
                        return "";
                    }
                });
            }
        }, 1);
    }

    /**
     * Function: refreshWorldGui
     * Purpose: To refresh the world guis of all players who have a certain Tourney level
     *
     * @param tourneyLevel - The tourney level of the players for whom you want to refresh the world gui
     */
    private static void refreshWorldGui(int tourneyLevel)
    {
        for (Player p : openWorldGuis.get(tourneyLevel))
        {
            refreshWorldGui(p);
        }
    }

    /**
     * Function: refreshNeededWorldGui
     * Purpose: To refresh the guis of all players who have guis open and whose available worlds contain the specified world
     *
     * @param w - The world which was altered in some way or deleted
     */
    private static void refreshNeededWorldGui(World w)
    {
        if (isTourneyWorld(w))
        {
            refreshWorldGui(getTourneyLevel(w));
        }
        else
        {
            for (int i = 1; i <= HIGHEST_TOURNEY_LEVEL; i++)
            {
                refreshWorldGui(i);
            }
        }
    }

    /**
     * Function: getWorldGuiLabel
     * Purpose: To get the label for the world gui
     *
     * @param p - The player for whom to get the label
     * @return - The label as String
     */
    private static String getWorldGuiLabel(Player p)
    {
        return ChatColor.BLACK + "Join Smash!";
    }

    /**
     * Function: isBeingDeleted
     * Purpose: To determine if a world is in the process of being deleted
     *
     * @param worldName - The name of the world for which to check the status of being deleted
     * @return - True if the world is being deleted
     */
    private static boolean isBeingDeleted(String worldName)
    {
        return worldsBeingDeleted.contains(worldName);
    }

    /**
     * Function: getAvaliableWorlds
     * Purpose: To get the list of worlds that a player can currently join
     *
     * @param p - The player for which to get avaliable worlds
     * @return - The list of worlds that the player can join
     */
    public static List<World> getAvaliableWorlds(Player p)
    {
        ArrayList<World> worlds = new ArrayList<World>();
        if (SmashStatTracker.hasAdvancedStatistics(p.getName()))
        {
            for (Integer i : takenTourneyWorldNumbers)
            {
                if (getTourneyLevel(i) == SmashStatTracker.getTourneyLevel(p) && !isBeingDeleted(getTourneyWorldName(SmashStatTracker.getTourneyLevel(p), i)))
                {
                    worlds.add(getTourneyWorld(i));
                }
            }
        }

        for (Integer i : takenFreeplayWorldNumbers)
        {
            if (!isBeingDeleted(getFreeplayWorldName(i)))
            {
                worlds.add(getFreeplayWorld(i));
            }
        }
        return worlds;
    }

    public static boolean hasSpecialPermissions(Player p)
    {
        return p.hasPermission(plugin.getCommand(CREATE_WORLD_CMD).getPermission());
    }

    /**
     * Function: getWorldGui
     * Purpose: To get the inventory that shows the worlds that a player can join with items representing differnet worlds.
     * This inventory also includes items for creating worlds for staff members
     *
     * @param p - The player for which to get the world gui
     * @return - The inventory of the world gui
     */
    private static Inventory getWorldGui(Player p)
    {
        ArrayList<ItemStack> representingItems = new ArrayList<ItemStack>();
        for (World w : getAvaliableWorlds(p))
        {
            representingItems.add(getWorldRepresenter(w));
        }

        if (hasSpecialPermissions(p))
        {
            representingItems.add(getTourneyWorldCreationItem(SmashStatTracker.getTourneyLevel(p)));
            representingItems.add(getFreeplayWorldCreationItem());
        }

        int worldCount = representingItems.size();
        int rows = worldCount/9 + 1;
        if (worldCount % 9 == 0)
        {
            rows -= 1;
        }
        Inventory worldInventory = Bukkit.createInventory(null, rows*9, getWorldGuiLabel(p));
        for (int i = 0; i < representingItems.size(); i++)
        {
            worldInventory.setItem(i, representingItems.get(i));
        }
        return worldInventory;
    }

    /**
     * Function: hasWorldGuiOpen
     * Purpose: To determine if a player has a world gui open
     *
     * @param p - The player for which to check if he has the world gui
     * @return - True if the player has a world gui open
     */
    private static boolean hasWorldGuiOpen(Player p)
    {
        return p.getOpenInventory().getTitle().equals(getWorldGuiLabel(p));
    }

    /**
     * Function: openWorldGui
     * Purpose: To display the world gui to a player
     *
     * @param p - The player for which to open a world gui
     */
    public static void openWorldGui(final Player p)
    {
        Bukkit.getScheduler().callSyncMethod(plugin, new Callable() {
            public String call()
            {
                if (!isSmashWorld(p.getWorld())
                        || !gameHasStarted(p.getWorld()) || gameHasEnded(p.getWorld()) || isInSpectatorMode(p))
                {
                    if (!hasWorldGuiOpen(p))
                    {
                        p.openInventory(getWorldGui(p));
                    }
                }
                else
                {
                    p.sendMessage(ChatColor.RED + "You are already in a game! Do /leave to leave, or /listworlds to see the other avaliable worlds!");
                }
                return "";
            }
        });
    }

    /**
     * Function: getTourneyWorldCreationItem
     * Purpose: To get the item that opens a tournament world for a specific tournament level
     *
     * @param tourneyLevel - The level for which to get the item which creates a tournament world for the specified tourney level
     * @return - The creation item for creating the tourney world
     */
    public static ItemStack getTourneyWorldCreationItem(int tourneyLevel)
    {
        ItemStack tourneyWorldCreationItem = plugin.getCustomItemStack(Material.DIAMOND, ChatColor.AQUA + "Create a Tournament world");
        ItemMeta meta = tourneyWorldCreationItem.getItemMeta();
        meta.setLore(Arrays.asList(ChatColor.GRAY + "" + ChatColor.ITALIC + "Round " + tourneyLevel));
        tourneyWorldCreationItem.setItemMeta(meta);
        return tourneyWorldCreationItem;
    }

    /**
     * Function: getFreeplayWorldCreationItem
     * Purpose: To get the item which creates a freeplay world
     *
     * @return - The item which creates a freeplay world
     */
    public static ItemStack getFreeplayWorldCreationItem()
    {
        return plugin.getCustomItemStack(Material.GOLD_INGOT, ChatColor.GOLD + "Create a Freeplay world");
    }

    /**
     * Function: getTourneyLevel
     * Purpose: To get the tourney level of a world (which doesn't change)
     *
     * @param w - The world for which to get the tourney level
     * @return The tourney level of the world, or -1 if it's invalid
     */
    private static int getTourneyLevel(World w)
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
            return Integer.valueOf("" + worldName.charAt(SMASH_WORLD_PREFIX.length() + TOURNEY_INDICATOR.length()));
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
    private static int getTourneyLevel(int worldNumber)
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
                plugin.deleteFile(name, PLAYER_SPAWN_FILENAME);
                plugin.deleteFile(name, ITEM_SPAWN_FILENAME);
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
            if (!takenTourneyWorldNumbers.contains(i))
            {
                World w = createSmashWorld(getTourneyWorldName(tourneyLevel, i), template);
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
            sender.sendMessage(ChatColor.GREEN + "Created " + getWorldDescription(w) + ChatColor.GREEN + "!");
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
    public static World createNextTourneyWorld(Player creator)
    {
        for (int i = 1; i <= MAX_TOURNEY_WORLDS; i++)
        {
            if (!takenTourneyWorldNumbers.contains(i) && !isBeingDeleted(getTourneyWorldName(SmashStatTracker.getTourneyLevel(creator), i)))
            {
                World w = createSmashWorld(getTourneyWorldName(SmashStatTracker.getTourneyLevel(creator), i));
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
            if (!takenFreeplayWorldNumbers.contains(i) && !isBeingDeleted(getFreeplayWorldName(i)))
            {
                World w = createSmashWorld(getFreeplayWorldName(i), template);
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
    public static World createNextFreeplayWorld(Player creator)
    {
        for (int i = 1; i <= MAX_FREEPLAY_WORLDS; i++)
        {
            if (!takenFreeplayWorldNumbers.contains(i))
            {
                World w =  createSmashWorld(getFreeplayWorldName(i));
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
            //and don't forget to start it

            if (isTourneyWorld(w.getName()))
            {
                logTourneyWorld(w.getName());
            }
            else if (!isTourneyWorld(w.getName()))
            {
                logFreeplayWorld(w.getName());
            }

            if (getLivingPlayers(w).size() > 0)
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
        int worldNumber = getWorldNumber(worldName);
        tourneyLevels.put(worldNumber, getTourneyLevel(worldName));
        for (int i = 0; i < takenTourneyWorldNumbers.size(); i++)
        {
            if (worldNumber < takenTourneyWorldNumbers.get(i))
            {
                takenTourneyWorldNumbers.add(i, worldNumber);
                return;
            }
        }
        if (takenTourneyWorldNumbers.size() == 0)
        {
            takenTourneyWorldNumbers.add(worldNumber);
        }
        else
        {
            takenTourneyWorldNumbers.add(worldNumber);
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
        int worldNumber = getWorldNumber(worldName);
        for (int i = 0; i < takenFreeplayWorldNumbers.size(); i++)
        {
            if (getWorldNumber(worldName) < takenFreeplayWorldNumbers.get(i))
            {
                takenFreeplayWorldNumbers.add(i, worldNumber);
                return;
            }
        }
        if (takenFreeplayWorldNumbers.size() == 0)
        {
            takenFreeplayWorldNumbers.add(getWorldNumber(worldName));
        }
        else
        {
            takenFreeplayWorldNumbers.add(worldNumber);
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
        int worldNumber = getWorldNumber(worldName);
        if (takenTourneyWorldNumbers.contains(worldNumber) && tourneyLevels.containsKey(worldNumber))
        {
            tourneyLevels.remove(worldNumber);
            takenTourneyWorldNumbers.remove((Integer)worldNumber);
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
        int worldNumber = getWorldNumber(worldName);
        if (takenFreeplayWorldNumbers.contains(worldNumber))
        {
            takenFreeplayWorldNumbers.remove((Integer)worldNumber);
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
     * @param p
     */
    public static void updateHitCooldown(Player p)
    {
        if (isSmashWorld(p.getWorld()))
        {
            p.setMaximumNoDamageTicks(HIT_TICKS);
        }
        else
        {
            p.setMaximumNoDamageTicks(20);
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
        File folder = new File(plugin.getServerPath());
        return folder.listFiles(fileFilter);
    }

    /**
     * Function:
     * Purpose:
     */
    public static void deleteWorldFolders()
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
                if (readyPlayers.containsKey(w))
                {
                    readyPlayers.remove(w);
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

    /**
     * Function:
     * Purpose:
     *
     * @param p
     * @param w
     * @param wasFromLogging
     */
    public static void handleJoin(final Player p, final World w, boolean wasFromLogging, boolean wasFromSpectation)
    {
        if (isSmashWorld(w))
        {
            if (wasFromLogging)
            {
                SmashWorldListener.displayJoinMessage(p);
            }

            if (!wasFromSpectation)
            {
                p.sendMessage(getWelcomeMessage(w));
                if (canBeJoinedRightNow(p, w))
                {
                    sendMessageToWorld(w, p.getDisplayName() + ChatColor.GRAY + " has joined the game!");
                    initializeSmashPlayer(p);
                    hideSpectatorsFromPlayer(p, w.getPlayers());
                }
                else
                {
                    sendMessageToWorld(w, p.getDisplayName() + ChatColor.GRAY + " has started spectating!");
                    setSpectatorMode(p, p.getWorld().getPlayers(), true);
                }

                //if (!gameHasStarted(w) || (getLivingPlayers(w).size() == 1 && wasFromLogging || getLivingPlayers(w).size() <= 2 && !wasFromLogging))
                //{
                //}
            }

            if (!gameHasEnded(w))
            {
                for (Player player : w.getPlayers())
                {
                    if (!p.equals(player) && SmashEntityTracker.isHoldingStill(p))
                    {
                        int amplifier = 3;
                        for (int i = 0; i < amplifier; i++)
                        {
                            player.playSound(player.getLocation(), Sound.NOTE_PLING, 1, 1);
                        }
                    }
                }
            }

            if (w.getPlayers().size() == 0 && wasFromLogging || w.getPlayers().size() <= 1 && !wasFromLogging)
            {
                startWorld(w);
            }
            /*else
            {
                if (w.getPlayers().size() > 5)
                {
                    //Vote for start?
                }
            }*/
        }
        else
        {
            SmashStatTracker.forgetOldElo(p.getName());
            if (!wasFromSpectation)
            {
                setSpectatorMode(p, w.getPlayers(), false);
            }
            resetPlayer(p);
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @param p
     */
    private static void initializeSmashPlayer(Player p)
    {
        World w = p.getWorld();
        if (isSmashWorld(w))
        {
            if (!gameHasStarted(w))
            {
                SmashStatTracker.rememberOldElo(p.getName());
            }
            resetPlayer(p);
            Vector v = p.getVelocity();
            v.setY(0);
            //Bukkit.getPlayer("HappyMan").sendMessage("setting velocity from smash player initializiation");
            p.setVelocity(v);
            if (SmashKitManager.canChangeKit(p))
            {
                allowFullflight(p, true);
            }
            if (gameHasStarted(w) && !gameHasEnded(w))
            {
                p.teleport(getRandomPlayerSpawnLocation(w));
            }
            if (canBeJoinedRightNow(p, w) && SmashScoreboardManager.hasScoreboard(w) && !SmashScoreboardManager.getScoreboardEntries(w).contains(SmashScoreboardManager.getPlayerEntryName(p)))
            {
                SmashScoreboardManager.setPlayerScoreValue(p, SmashWorldManager.getDeathMatchLives(p.getWorld()));
            }
            SmashManager.clearDamage(p);
            SmashKitManager.getSelectedKit(p).applyKitInventory(p);
        }
        else
        {
            plugin.sendErrorMessage("Error! Tried to Smash initialize a player who was not in a Smash world!");
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @param p
     */
    public static void resetPlayer(final Player p)
    {
        p.setFoodLevel(20);
        SmashItemManager.cancelItemTasks(p);
        if (!isInSpectatorMode(p) && !hasSpecialPermissions(p))
        {
            p.setAllowFlight(false);
        }
        if (!isSmashWorld(p.getWorld()) || isInSpectatorMode(p))
        {
            if (!isInSpectatorMode(p))
            {
                p.getInventory().setArmorContents(new ItemStack[] {new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR)});
            }
            p.setHealth(p.getMaxHealth());
            p.getInventory().clear();
            giveSmashGuiOpener(p);
        }
        else
        {
            SmashKitManager.restoreAllUsagesAndCharges(p, false);
        }
        SmashScoreboardManager.updateScoreboard(p);
        SmashManager.clearDamage(p);
        if (!hasSpecialPermissions(p) || isSmashWorld(p.getWorld()))
        {
            p.setGameMode(GameMode.ADVENTURE);
        }
        p.setExp(0);
        p.setLevel(0);
        updateHitCooldown(p);
        for (PotionEffect effect : p.getActivePotionEffects())
        {
            p.removePotionEffect(effect.getType());
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @param p
     * @param w
     * @param wasFromLogging
     */
    public static void handleLeave(final Player p, final World w, final boolean wasFromLogging, boolean justForSpectation)
    {
        if (isSmashWorld(w))
        {
            unhidePlayersFromPlayer(p, w.getPlayers());

            if (whoToKick.containsKey(p))
            {
                whoToKick.remove(p);
            }
            if (readyPlayers.containsKey(w) && readyPlayers.get(w).contains(p))
            {
                readyPlayers.get(w).remove(p);
            }
            if (wasFromLogging)
            {
                SmashEntityTracker.forgetSpeed(p);
            }
            if (!justForSpectation)
            {
                setSpectatorMode(p, w.getPlayers(), false);
            }
            resetPlayer(p);
            if (isDeathMatchWorld(w) && gameHasStarted(w) && !gameHasEnded(w))
            {
                performDeathMatchDeath(p, w, !justForSpectation);
            }
            SmashManager.forgetLastHitter(p);
            //SmashKitManager.deselectKit(p);
            if (getLivingPlayers(w).size() <= 1 && wasFromLogging || getLivingPlayers(w).size() == 0 && !wasFromLogging)
            {
                removeGame(w, false, false, false);
            }
            else if (gameHasStarted(w) && !gameHasEnded(w))
            {
                SmashWorldManager.recordKOStatistics(p, w);
                if (!justForSpectation && (wasFromLogging || !isDeathMatchWorld(w)) && !isInSpectatorMode(p))
                {
                    sendMessageToWorld(w, ChatColor.RED + p.getName() + " quit!");
                }
            }
        }
    }

    private static void setSpectatorMode(Player p, List<Player> players, boolean spectate)
    {
        if (spectate)
        {
            if (!isInSpectatorMode(p))
            {
                spectators.add(p);
                if (isSmashWorld(p.getWorld()))
                {
                    resetPlayer(p);
                    giveSmashGuiOpener(p);
                }
                p.sendMessage(ChatColor.GREEN + "" + ChatColor.ITALIC + "You are now in spectator mode.");
            }
            hideSpectatorFromPlayers(p, players);
            allowFullflight(p, true);
            //addTabEntry(p.getDisplayName());
            //removeAllTabEntries(p);
        }
        else
        {
            if (isInSpectatorMode(p))
            {
                spectators.remove(p);
                if (isSmashWorld(p.getWorld()))
                {
                    p.sendMessage(ChatColor.GREEN + "" + ChatColor.ITALIC + "You are no longer in spectator mode.");
                }
            }
            if (isSmashWorld(p.getWorld()))
            {
                initializeSmashPlayer(p);
            }
            else
            {
                resetPlayer(p);
            }
            if (!hasSpecialPermissions(p))
            {
                p.setAllowFlight(false);
            }
            for (Player player : Bukkit.getOnlinePlayers())
            {
                player.showPlayer(p);
            }
            //addAllTabEntries(p);
        }
    }

    private static void hideSpectatorsFromPlayers(List<Player> players)
    {
        for (Player p : players)
        {
            if (isInSpectatorMode(p))
            {
                hideSpectatorFromPlayers(p, players);
            }
        }
    }

    private static void hideSpectatorsFromPlayer(Player p, List<Player> playersToCheck)
    {
        for (Player player : playersToCheck)
        {
            if (isInSpectatorMode(player))
            {
                p.hidePlayer(player);
            }
        }
    }

    private static void unhidePlayersFromPlayer(Player p, List<Player> players)
    {
        for (Player player : players)
        {
            p.showPlayer(player);
        }
    }

    private static void hideSpectatorFromPlayers(Player p, List<Player> players)
    {
        for (Player player : players)
        {
            if (!isInSpectatorMode(player))
            {
                player.hidePlayer(p);
            }
        }
    }

    public static List<Player> getLivingPlayers(World w)
    {
        List<Player> players = new ArrayList<Player>();
        for (Player p : w.getPlayers())
        {
            if (!isInSpectatorMode(p))
            {
                players.add(p);
            }
        }
        return players;
    }

    public static boolean isInSpectatorMode(Player p)
    {
        return spectators.contains(p);
    }

    /**
     * Function:
     * Purpose:
     *
     * @param p
     */
    private static void sendPlayerToLastWorld(final Player p)
    {
        final World w;
        Location l = getLastPlayerLocation(p);
        if (getLastPlayerLocation(p) != null)
        {
            p.teleport(l);
            lastPlayerLocations.remove(p);
        }
        else
        {
            while(FALLBACK_WORLD == null){}
            w = FALLBACK_WORLD;
            p.teleport(w.getSpawnLocation());
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @param p
     * @param w2
     */
    public static void sendPlayerToWorld(final Player p, World w2)
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
        else if (!getAvaliableWorlds(p).contains(w2))
        {
            p.sendMessage(ChatColor.RED + "You are not allowed to join this game (at least not right now)!");
        }
        else if (!playerSpawnLocations.containsKey(w2) || playerSpawnLocations.get(w2).size() == 0)
        {
            p.sendMessage(ChatColor.RED + "Error! There are no spawn locations in map " + getMapName(w2));
        }
        else
        {
            if (!isSmashWorld(w1))
            {
                lastPlayerLocations.put(p, p.getLocation());
            }
            if (isHated(p, w2))
            {
                p.sendMessage(ChatColor.RED + "Sorry, but the players in this world have decided that they don't want you.");
            }
            else
            {
                p.teleport(getRandomPlayerSpawnLocation(w2));
            }
        }
    }

    private static boolean isHated(Player p, World w)
    {
        if (isSmashWorld(w))
        {
            int haters = 0;
            List<Player> whosInThere = w.getPlayers();
            for (Player potentialHater : whosInThere)
            {
                if (whoToKick.containsKey(potentialHater) && whoToKick.get(potentialHater).contains(p))
                {
                    haters++;
                }
            }
            return haters >= 2 && (float)haters/(whosInThere.size()) >= VOTE_KICK_PERCENT;
        }
        return false;
    }

    public static boolean canBeJoinedRightNow(Player p, World w)
    {
        return (!gameHasStarted(w) || gameHasEnded(w) || !isTourneyWorld(w) && !isDeathMatchWorld(w)) && getAvaliableWorlds(p).contains(w);
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

    public static Location getRandomItemSpawnLocation(World w)
    {
        ArrayList<Location> possibleSpawnLocations = itemSpawnLocations.get(w);
        return possibleSpawnLocations.get(r.nextInt(possibleSpawnLocations.size()));
    }

    /**
     * Function:
     * Purpose:
     *
     * @param w
     * @return
     */
    private static int getWorldNumber(World w)
    {
        if (isSmashWorld(w))
        {
            return getWorldNumber(w.getName());
        }
        else
        {
            return -1;
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @param worldName
     * @return
     */
    private static int getWorldNumber(String worldName)
    {
        int lengthOfStuffbeforeWorldNumber = SMASH_WORLD_PREFIX.length() + FREEPLAY_INDICATOR.length() + WORLD_INDICATOR.length();
        if (isTourneyWorld(worldName))
        {
            lengthOfStuffbeforeWorldNumber = SMASH_WORLD_PREFIX.length() + TOURNEY_INDICATOR.length() + 1 + WORLD_INDICATOR.length();
        }
        return Integer.valueOf(worldName.substring(lengthOfStuffbeforeWorldNumber, worldName.length()));
    }

    /**
     * Function:
     * Purpose:
     *
     * @param w
     * @return
     */
    private static String getWorldDescription(World w)
    {
        if (isTourneyWorld(w))
        {
            return "Smash " + getSimpleWorldName(w) + " [Round " + getTourneyLevel(w) + "]";
        }
        else
        {
            return getSimpleWorldName(w);
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @param w
     * @return
     */
    private static String getWelcomeMessage(World w)
    {
        return ChatColor.GREEN + "Entered " + getWorldDescription(w) + ChatColor.GREEN + "!";
    }

    /**
     * Function:
     * Purpose:
     *
     * @param w
     * @return
     */
    private static String getSimpleWorldName(World w)
    {
        if (isTourneyWorld(w))
        {
            return ChatColor.AQUA + TOURNEY_WORLD_ITEM_PREFIX + getWorldNumber(w);
        }
        if (isSmashWorld(w) && !isTourneyWorld(w))
        {
            return ChatColor.GOLD + FREE_WORLD_ITEM_PREFIX + getWorldNumber(w);
        }
        else
        {
            return w.getName();
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @param w
     * @return
     */
    private static String getWorldListName(World w)
    {
        return getSimpleWorldName(w) + " (" + getShortWorldName(w)  + ")" + ChatColor.BLUE + " | " + ChatColor.YELLOW + "Status: " + getWorldStatus(w);
    }

    /**
     * Function:
     * Purpose:
     *
     * @param w
     * @return
     */
    private static String getWorldStatus(World w)
    {
        String status;
        if (isBeingDeleted(w.getName()))
        {
            status = ChatColor.RED + "" + ChatColor.BOLD + "Being deleted";
        }
        else if (getLivingPlayers(w).size() == 0)
        {
            if (w.getPlayers().size() == 0)
            {

                status = ChatColor.GRAY + "Empty";
            }
            else
            {
                status = ChatColor.GRAY + "Virtually empty";
            }
        }
        else if (gameHasStarted(w))
        {
            status = ChatColor.RED + "In progress";
        }
        else if (gameHasEnded(w))
        {
            status = ChatColor.GOLD + "Game over";
        }
        else if (isStartingSoon(w))
        {
            status = ChatColor.GREEN + "Starting soon";
        }
        else
        {
            status = ChatColor.YELLOW + "Waiting for players";
        }
        return status;
    }

    /**
     * Function:
     * Purpose:
     *
     * @param w
     * @return
     */
    public static String getShortWorldName(World w)
    {
        String s = w.getName().replaceAll(SMASH_WORLD_PREFIX, "");
        s = s.replaceAll("w", "g");
        return s;
    }

    /**
     * Function:
     * Purpose:
     *This is for tourney worlds
     *
     * @param w
     * @return
     */
    private static ItemStack getWorldRepresenter(World w)
    {
        if (w == null || !isSmashWorld(w))
        {
            String description = "null";
            if (w != null)
            {
                description = "non-smash";
            }
            plugin.sendErrorMessage("Error! Could not get an item representation for a " + description + " world!");
            return plugin.getCustomItemStack(Material.BEDROCK, ChatColor.RED + "Bad world!!");
        }

        ItemStack worldRepresenter;
        if (isTourneyWorld(w))
        {
            worldRepresenter = plugin.getCustomItemStack(Material.DIAMOND_SWORD, getSimpleWorldName(w));
        }
        else
        {
            worldRepresenter = plugin.getCustomItemStack(Material.GOLD_SWORD, getSimpleWorldName(w));
        }

        if (worldRepresenter == null)
        {
            worldRepresenter = plugin.getCustomItemStack(Material.INK_SACK, ChatColor.RED + "Not a smash world!", 1);
        }
        ArrayList<String> itemMetas = new ArrayList<String>();
        itemMetas.add(ChatColor.GREEN + "Players: " + getLivingPlayers(w).size());
        itemMetas.add(ChatColor.GREEN + "Map: " + getMapName(w));
        determineDeathMatchPreference(w);
        String type = ChatColor.GREEN + "Point-based";
        if (isDeathMatchWorld(w))
        {
            type = ChatColor.RED + "Deathmatch";
        }
        if (gameHasStarted(w) && !gameHasEnded(w))
        {
            if (!isDeathMatchWorld(w))
            {
                itemMetas.add(ChatColor.RED + "" + ChatColor.ITALIC + "Time til end: " + SmashScoreboardManager.getTimeRemaining(SmashScoreboardManager.getTimeRemaining(w)));
            }
            itemMetas.add(ChatColor.YELLOW + "Status: " + getWorldStatus(w));
            itemMetas.add("Type: " + type);
        }
        else if (getLivingPlayers(w).size() > 0)
        {
            itemMetas.add(ChatColor.YELLOW + "Status: " + getWorldStatus(w));
            itemMetas.add(ChatColor.YELLOW + "Tentative type: " + type);
        }
        if (isTourneyWorld(w))
        {
            itemMetas.add(ROUND_LORE_INDEX, ChatColor.GRAY + "" + ChatColor.ITALIC + "Round " + getTourneyLevel(w));
        }
        ItemMeta meta = worldRepresenter.getItemMeta();
        meta.setLore(itemMetas);
        worldRepresenter.setItemMeta(meta);
        return worldRepresenter;
    }

    /**
     * Function:
     * Purpose:
     *
     * @param item
     * @return
     */
    public static World getWorldRepresented(ItemStack item)
    {
        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
        String worldType;
        if (name.startsWith(TOURNEY_WORLD_ITEM_PREFIX))
        {
            String roundLore = item.getItemMeta().getLore().get(ROUND_LORE_INDEX);
            worldType = TOURNEY_INDICATOR + (Integer.valueOf("" + roundLore.charAt(roundLore.length() - 1)));
        }
        else if (name.startsWith(FREE_WORLD_ITEM_PREFIX))
        {
            worldType = FREEPLAY_INDICATOR;
        }
        else
        {
            return null;
        }

        String worldName = SMASH_WORLD_PREFIX + worldType + WORLD_INDICATOR + (Integer.valueOf("" + name.charAt(name.length() - 1)));
        if (Bukkit.getWorld(worldName) == null)
        {
            plugin.sendErrorMessage("Error! The world that that item represents could not be found!");
            return FALLBACK_WORLD;
        }
        else return Bukkit.getWorld(worldName);
    }

    /**
     * Function:
     * Purpose:
     *
     * @param tourneyLevel
     * @param worldNumber
     * @return
     */
    private static String getTourneyWorldName(int tourneyLevel, int worldNumber)
    {
        return SMASH_WORLD_PREFIX + TOURNEY_INDICATOR + tourneyLevel + WORLD_INDICATOR + worldNumber;
    }

    /**
     * Function:
     * Purpose:
     *
     * @param worldNumber
     * @return
     */
    private static String getFreeplayWorldName(int worldNumber)
    {
        return SMASH_WORLD_PREFIX + FREEPLAY_INDICATOR + WORLD_INDICATOR + worldNumber;
    }

    /**
     * Function:
     * Purpose:
     *
     * @param worldNumber
     * @return
     */
    private static World getTourneyWorld(int worldNumber)
    {
        String worldName;
        if (tourneyLevels.containsKey(worldNumber))
        {
            worldName = getTourneyWorldName(tourneyLevels.get(worldNumber), worldNumber);
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
    private static World getFreeplayWorld(int worldNumber)
    {
        if (Bukkit.getWorld(getFreeplayWorldName(worldNumber)) != null)
        {
            return Bukkit.getWorld(getFreeplayWorldName(worldNumber));
        }
        plugin.sendErrorMessage("Error! Tried to get world " + getFreeplayWorldName(worldNumber) + ", but it didn't exist!");
        return null;
    }

    /**
     * Function:
     * Purpose:
     *
     * @param w
     * @return
     */
    public static String getMapName(World w)
    {
        if (Bukkit.getWorlds().contains(w))
        {
            return getMapName(w.getName());
        }
        plugin.sendErrorMessage("Error! Could not find world " + w.getName() + " and therefore could not get its map name!");
        return null;
    }

    /**
     * Function:
     * Purpose:
     *
     * @param worldFileName
     * @return
     */
    private static String getMapName(String worldFileName)
    {
        try
        {
            Scanner s = new Scanner(new File(worldFileName + "/mapName.txt"));
            if (s.hasNext())
            {
                return s.next();
            }
        }
        catch (FileNotFoundException ex)
        {
            plugin.sendErrorMessage("mapName.txt was missing from the world " + worldFileName);
        }
        return "unnamed";
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

        File folder = new File(plugin.getServerPath());
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

    /**
     * Function:
     * Purpose:
     *
     * @param p
     */
    public static void removeSmashGuiOpener(Player p)
    {
        if (SmashWorldListener.SMASH_GUI_ITEM != null && p.getInventory().contains(SmashWorldListener.SMASH_GUI_ITEM))
        {
            p.getInventory().remove(SmashWorldListener.SMASH_GUI_ITEM);
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @param p
     */
    public static void giveSmashGuiOpener(final Player p)
    {
        giveSmashGuiOpener(p, false);
        if (!isSmashWorld(p.getWorld()))
        {
            openWorldGui(p);
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @param p
     * @param alreadyChecked
     */
    private static void giveSmashGuiOpener(final Player p, final boolean alreadyChecked)
    {
        Bukkit.getScheduler().callSyncMethod(plugin, new Callable() {
            public String call()
            {
                if (SmashWorldListener.SMASH_GUI_ITEM != null && !p.getInventory().contains(SmashWorldListener.SMASH_GUI_ITEM) && (!isSmashWorld(p.getWorld()) || isInSpectatorMode(p)))
                {
                    p.getInventory().setItem(8, SmashWorldListener.SMASH_GUI_ITEM);
                    p.updateInventory();
                }
                if (!alreadyChecked)
                {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
                        public void run()
                        {
                            giveSmashGuiOpener(p, true);
                        }
                    }, 30);
                }
                return "";
            }
        });
    }

    private static World getBestWorld(int tourneyLevel, World currentWorld)
    {
        List<Integer> avaliableWorldNums;
        if (tourneyLevel == 0)
        {
            avaliableWorldNums = takenFreeplayWorldNumbers;
        }
        else if (tourneyLevel <= HIGHEST_TOURNEY_LEVEL)
        {
            avaliableWorldNums = takenTourneyWorldNumbers;
        }
        else
        {
            return null;
        }

        World nextWorld = null;
        for (int i = 0; i < avaliableWorldNums.size(); i++)
        {
            World candidateWorld;
            if (avaliableWorldNums == takenTourneyWorldNumbers)
            {
                 candidateWorld = getTourneyWorld(avaliableWorldNums.get(i));
            }
            else
            {
                candidateWorld = getFreeplayWorld(avaliableWorldNums.get(i));
            }

            if (tourneyLevel == 0 || getTourneyLevel(candidateWorld) == tourneyLevel)
            {
                if ((!SmashScoreboardManager.hasScoreboard(candidateWorld) || !gameHasStarted(candidateWorld) && SmashScoreboardManager.getStartTime(candidateWorld) > 7)
                        && (!getMapName(candidateWorld).equals(getMapName(currentWorld)) || candidateWorld.getPlayers().size() >= 1))
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
        for (Integer i : takenTourneyWorldNumbers)
        {
            w = getTourneyWorld(i);
            if (w != null)
            {
                list.add(getWorldListName(w));
            }
            else
            {
                plugin.sendErrorMessage("Error! When " + sender.getName() + " tried to list the worlds, one of the Tournament worlds was null!");
            }
        }
        for (Integer i : takenFreeplayWorldNumbers)
        {
            w = getFreeplayWorld(i);
            if (w != null)
            {
                list.add(getWorldListName(w));
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
    private String smashifyArgument(String arg)
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
                openWorldGui((Player)sender);
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
                determineDeathMatchPreference(w);
                if (isDeathMatchWorld(w) && w.getPlayers().size() <= 1)
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
                        sender.sendMessage(ChatColor.RED + "Game has already started in " + getShortWorldName(w) + ".");
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
        else if (plugin.matchesCommand(label, CREATE_WORLD_CMD))
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
                    if (tourneyLevel < 1 || tourneyLevel > HIGHEST_TOURNEY_LEVEL)
                    {
                        throw new NumberFormatException();
                    }
                }
                catch (NumberFormatException e)
                {
                    sender.sendMessage(ChatColor.RED + "Invalid Tournament level! Your options are 1-" + HIGHEST_TOURNEY_LEVEL + ".");
                    return true;
                }
                if (tourneyLevel >= 1 && tourneyLevel <= HIGHEST_TOURNEY_LEVEL)
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
                        sender.sendMessage(ChatColor.YELLOW + "Do you really want to delete the world you're in right now (" + getSimpleWorldName(p.getWorld()) + ChatColor.YELLOW + ")?");
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
                    sender.sendMessage(ChatColor.GREEN + "You deleted world " + getSimpleWorldName(w) + ChatColor.GREEN + ".");
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
                sender.sendMessage(color + "Map " + templates[i].getName().charAt(templates[i].getName().length() - 1) + ": " + getMapName(templates[i].getName()));
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
                    sender.sendMessage(ChatColor.YELLOW + "Game in " + getSimpleWorldName(w) + ChatColor.YELLOW + " reset!");
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
                            p.sendMessage(ChatColor.YELLOW + "Game in " + getSimpleWorldName(w) + ChatColor.YELLOW + " reset!");
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
                    p.sendMessage(ChatColor.YELLOW + "Game in " + getSimpleWorldName(w) + ChatColor.YELLOW + " reset.");
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
                    sender.sendMessage(ChatColor.GREEN + p.getName() + " is in " + getWorldDescription(w) + ChatColor.GREEN + " (" + getShortWorldName(w) + ") which is " + ChatColor.stripColor(getWorldStatus(w)).toLowerCase() + ChatColor.GREEN + ".");
                }
            }
            else if (!(sender instanceof Player))
            {
                sender.sendMessage(ChatColor.RED + "You are not in game!");
            }
            else
            {
                sender.sendMessage(ChatColor.GREEN + "You are in " + getWorldDescription(((Player)sender).getWorld()) + ChatColor.GREEN + " (" + getShortWorldName(((Player)sender).getWorld()) + ").");
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
                sender.sendMessage(ChatColor.GREEN + "You are currently in " + getShortWorldName(((Player)sender).getWorld()));
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
                if (!(FALLBACK_WORLD != null && worldName.equalsIgnoreCase(FALLBACK_WORLD.getName())))
                {
                    worldName = smashifyArgument(worldName);
                }
                if (Bukkit.getWorld(worldName) == null)
                {
                    sender.sendMessage(ChatColor.RED + "World " + args[0] + " not found!");
                }
                else
                {
                    sendPlayerToWorld((Player)sender, Bukkit.getWorld(worldName));
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
                String setting = getDeathMatchPreference(p);
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
                setDeathMatchPreference(p, newPreference, newLivesPreference);
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
            determineDeathMatchPreference(w);
            String tentativelyCurrently;
            int lives = getDeathMatchLives(w);
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
                gameType = "deathmatch games with " + getDeathMatchLives(w) + " lives";
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
                if (!isSmashWorld(w) && !hasSpecialPermissions(p))
                {
                    p.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "You're not in a Smash world!");
                }
                else if (isSmashWorld(w) && isInSpectatorMode(p) && !canBeJoinedRightNow(p, w) )
                {
                    p.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "You cannot join this game.");
                }
                else
                {
                    setSpectatorMode(p, p.getWorld().getPlayers(), !isInSpectatorMode(p));
                    if (!isInSpectatorMode(p))
                    {
                        handleJoin(p, p.getWorld(), false, true);
                    }
                    else
                    {
                        handleLeave(p, p.getWorld(), false, true);
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
                    if (!readyPlayers.containsKey(w))
                    {
                        readyPlayers.put(w, new ArrayList<Player>());
                    }
                    boolean wasAlreadyReady = readyPlayers.get(w).contains(p);
                    if (!wasAlreadyReady)
                    {
                        readyPlayers.get(w).add(p);
                    }
                    int whoIsReady = readyPlayers.get(w).size();
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
                else if (whoYouDontLike == null || !whoYouDontLike.getWorld().equals(w) || isInSpectatorMode(whoYouDontLike))
                {
                    sender.sendMessage(ChatColor.RED + "Player not found!");
                }
                else
                {
                    if (!whoToKick.containsKey(p))
                    {
                        whoToKick.put(p, new ArrayList<Player>());
                    }
                    whoToKick.get(p).add(whoYouDontLike);

                    if (isHated(p, w))
                    {
                        sendPlayerToLastWorld(whoYouDontLike);
                        sendMessageToWorld(w, ChatColor.YELLOW + whoYouDontLike.getName() + " has been kicked!");
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