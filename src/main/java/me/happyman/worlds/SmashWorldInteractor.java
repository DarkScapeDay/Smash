package me.happyman.worlds;

import me.happyman.Listeners.SmashItemManager;
import me.happyman.SmashKitMgt.SmashKitManager;
import me.happyman.commands.SmashManager;
import me.happyman.source;
import me.happyman.utils.*;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.Callable;

import static me.happyman.worlds.SmashWorldManager.*;

public class SmashWorldInteractor
{
    private static final int HIT_TICKS = 20; //The number of ticks between each hit for players
    public static final int HIGHEST_TOURNEY_LEVEL = 3; //not 0-indexed! - The Tournament level that is for the highest tournament level players. If people with this level win a tourney game, they win a tournament
    public static final int MIN_FREE_GAMES_TO_JOIN_TOURNEYS = 12; //The Minimum games a player needs to play to unlock tourney games
    public static final float DEFAULT_FLY_SPEED = 0.075F; //The fly speed for before and after Smash games start
    protected static final int TOURNEY_TOP_PERCENT = 30; //0-100, this is the percentage of players that will act on to the next tournament level in a tournament game.
    private static final float VOTE_KICK_PERCENT = 0.75F;
    protected static final String FREE_WORLD_ITEM_PREFIX = "Freeplay Game "; //What items that reference Freeplay games are called (in the gui)
    protected static final String TOURNEY_WORLD_ITEM_PREFIX = "Tourney Game "; //What items that reference Tourney games are called (in the gui)
    private static final int ROUND_LORE_INDEX = 2; //The index of the Round lore on items which tp players to tourney worlds
    protected static final String TOURNEY_INDICATOR = "t"; //The indicator in a world folder name which tells you it's a tourney world
    private static final String WORLD_INDICATOR = "w"; //The indicator on all world folder names which precedes the world number
    private static final String FREEPLAY_INDICATOR = "fp"; //This tells you it's a freeplay world
    protected static final String CREATE_WORLD_CMD = "createworld"; //This creates a world
    private static final String DEATHMATCH_PREFERENCE_DATANAME = "Deathmatch"; //The name of the data which is for remembering people's deathmatch preferences
    private static ArrayList<Player> deadPlayers = new ArrayList<Player>(); //This is for keeping track of who should be tped when they exit the KO zone but shouldn't be tped right away
    private static HashMap<Integer, ArrayList<Player>> openWorldGuis = new HashMap<Integer, ArrayList<Player>>(); //The players who have world guis open, separated into tournament levels for convenience
    private static ArrayList<String> participants = new ArrayList<String>(); //The people in a game who have gotten a KO and therefore aren't AFK
    private static ArrayList<Integer> takenTourneyWorldNumbers = new ArrayList<Integer>(); //The world numbers of tournament worlds that have been taken already
    private static ArrayList<Integer> takenFreeplayWorldNumbers = new ArrayList<Integer>(); //The world numbers of freeplay worlds that have been taken
    private static HashMap<World, Integer> deathMatchWorlds = new HashMap<World, Integer>(); //The number of lives that was selected for a deathmatch game (or 0 if it's not a deathmatch game)
    private static HashMap<World, Integer> originalPlayerCount = new HashMap<World, Integer>(); //The number of players that started out in a game, used to see when a tourney game should end
    private static ArrayList<Player> spectators = new ArrayList<Player>(); //This keeps track of who all is in spectator mode
    private static HashMap<Player, PlayerList> tabLists = new HashMap<Player, PlayerList>();
    private static HashMap<World, List<Player>> readyPlayers = new HashMap<World, List<Player>>();

    protected static HashMap<Player, List<Player>> getWhoToKick()
    {
        return whoToKick;
    }

    private static HashMap<Player, List<Player>> whoToKick = new HashMap<Player, List<Player>>();

    protected static List<Integer> getTakenTourneyWorldNumbers()
    {
        return takenTourneyWorldNumbers;
    }
    protected static List<Integer> getTakenFreeplayWorldNumbers()
    {
        return takenFreeplayWorldNumbers;
    }

    protected static void addTabEntry(Player p, String entry)
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

    static PlayerList getTabList(Player p)
    {
        if (!tabLists.containsKey(p))
        {
            tabLists.put(p, new PlayerList(p, PlayerList.SIZE_DEFAULT));
        }
        return tabLists.get(p);
    }

    protected static void removeTabEntry(Player p, String entry)
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

    protected static void addAllTabEntries(Player p)
    {
        for (Player online : Bukkit.getOnlinePlayers())
        {
            if (!isInSpectatorMode(online))
            {
                addTabEntry(online, p.getName());
            }
        }
    }

    protected static void removeAllTabEntries(Player p)
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
    protected static Integer getOriginalPlayerCount(World w)
    {
        if (originalPlayerCount.containsKey(w))
        {
            return originalPlayerCount.get(w);
        }
        return null;
    }

    /**
     * Clears the original player count record from memory
     * @param w - The world for which we don't need to know the original player count for any more
     */
    protected static void forgetOriginalPlayerCount(World w)
    {
        if (originalPlayerCount.containsKey(w))
        {
            originalPlayerCount.remove(w);
        }
    }

    /**
     * Function: rememberOriginalPlayerCount
     * Purpose: To record how many players were in the world at the start of a tournament game
     * @param w - The world for which to record the original player count
     */
    protected static void rememberOriginalPlayerCount(World w)
    {
        SmashWorldInteractor.originalPlayerCount.put(w, getLivingPlayers(w).size());
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
     * Gets the deathmatch preference for the player in format "<preference> <lives>" from his player data file. It sets his preference to default if there wasn't already one.
     * @param p - The player for which we would like to fetch the deathmatch preference
     * @return - The deathmatch preference of the player as string
     */
    protected static String getDeathMatchPreference(Player p)
    {
        String preference = SmashManager.getPlugin().getDatum(p, DEATHMATCH_PREFERENCE_DATANAME);
        Scanner s = new Scanner(preference);
        if (preference.length() == 0 || !s.hasNextBoolean())
        {
            preference = "false 4";
            SmashManager.getPlugin().putDatum(p, DEATHMATCH_PREFERENCE_DATANAME, preference);
        }
        else
        {
            boolean dm = s.nextBoolean();
            if (!s.hasNextInt())
            {
                preference = dm + " 4";
                SmashManager.getPlugin().putDatum(p, DEATHMATCH_PREFERENCE_DATANAME, preference);
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
    protected static void setDeathMatchPreference(Player p, boolean preference, int lives)
    {
        if (preference)
        {
            String display = "ves";
            if (lives == 1)
            {
                display = "fe";
            }
            p.sendMessage(ChatColor.GREEN + "You now prefer Deathmatch games with " + lives + " li" + display + ".");
           SmashManager.getPlugin().putDatum(p, DEATHMATCH_PREFERENCE_DATANAME, "true " + lives);
        }
        else
        {
            p.sendMessage(ChatColor.GREEN + "You now prefer Point-based games.");
            if (lives < 3)
            {
                lives = 3;
            }
           SmashManager.getPlugin().putDatum(p, DEATHMATCH_PREFERENCE_DATANAME, "false " + lives);
        }
    }

    /**
     * Function: determineDeathMatchPreference
     * Purpose: Calculated whether a world is a deathmatch world or not, and if so how many lives
     * @param w - The world to be situated for its deathmatch preference
     */
    static void determineDeathMatchPreference(World w)
    {
        if (getLivingPlayers(w).size() == 0)
        {
            deathMatchWorlds.put(w, 0);
        }
        else if (!SmashWorldManager.gameHasStarted(w))
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
    protected static int getDeathMatchLives(World w)
    {
        if (isDeathMatchWorld(w))
        {
            return deathMatchWorlds.get(w);
        }
        return 0;
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
    protected static void playSoundToPlayers(final Player playerToExclude, Location soundSourceLocation, final List<Player> players, final Sound sound, final float volume, final float pitch, final int ticksBetweenCalls, final int timesToCall)
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
                Bukkit.getScheduler().scheduleSyncDelayedTask(SmashManager.getPlugin(), new Runnable() {
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
    protected static void playSoundToPlayers(Player playerToExclude, List<Player> players, Sound sound, float volume, float pitch)
    {
        playSoundToPlayers(playerToExclude, playerToExclude.getLocation(), players, sound, volume, pitch, 0, 1);
    }

    public static void playSoundToPlayers(List<Player> players, Location soundSourceLocation, Sound sound, float volume, float pitch)
    {
        playSoundToPlayers(null, soundSourceLocation, players, sound, volume, pitch, 0, 1);
    }

    static void performDeathMatchDeath(final Player p, final World w, final boolean quitter)
    {
        if (SmashWorldManager.isSmashWorld(w) && SmashWorldManager.gameHasStarted(w) && !SmashWorldManager.gameHasEnded(w) &&
                SmashScoreboardManager.hasScoreboard(w) && SmashScoreboardManager.getScoreboardEntries(w).contains(SmashScoreboardManager.getPlayerEntryName(p)))
        {
            SmashStatTracker.performEloChange(p.getName(), w, getDeathMatchLives(w));
            SmashScoreboardManager.removeEntry(w, SmashScoreboardManager.getPlayerEntryName(p));
            if (!quitter)
            {
                handleLeave(p, p.getWorld(), false, true);
                sendMessageToWorld(w, ChatColor.GOLD + p.getName() + ChatColor.RED + "" + ChatColor.BOLD + " DEFEATED");
                if (SmashWorldManager.isTourneyWorld(w))
                {
                    SmashStatTracker.incrementTourneyRoundsPlayed(p);
                    if (SmashWorldManager.getTourneyLevel(w) == HIGHEST_TOURNEY_LEVEL)
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
            Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
                public String call() {
                    if (rf <= 1 || SmashWorldManager.isTourneyWorld(w) && rf <= 1.0*TOURNEY_TOP_PERCENT/100*getOriginalPlayerCount(w))
                    {
                        SmashWorldManager.endGame(w);
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
    protected static void recordKOStatistics(final Player deadPlayer, World w)
    {
            if (SmashWorldManager.isSmashWorld(w) && SmashWorldManager.gameHasStarted(w) && !SmashWorldManager.gameHasEnded(w))
            {
                if (SmashManager.hasLastHitter(deadPlayer.getName()))
                {
                    String participant = SmashManager.getLastHitter(deadPlayer.getName()).killerName;
                   SmashManager.getPlugin().incrementStatistic(participant, SmashStatTracker.KO_DEALT_SCORE);
                   SmashManager.getPlugin().incrementStatistic(deadPlayer, SmashStatTracker.KO_RECEIVED_SCORE);
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
                   SmashManager.getPlugin().incrementStatistic(deadPlayer, SmashStatTracker.FALLEN_OUT_SCORE);
                }
            }
    }

    /**
     * Function: getRespawnRunnable
     * Purpose: Gets the runnable for respawning a Smash player (essentially killing them)
     * @param deadPlayer - The player who has DIED
     * @return - The runnable for respawning a player and recording the stats associated with the unfortunate event
     */
    static Runnable getRespawnRunnable(final Player deadPlayer)
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
    protected static void respawnTeleport(final Player p)
    {
        SmashItemManager.cancelItemTasks(p);
        SmashKitManager.restoreAllUsagesAndCharges(p, false);
        SmashManager.clearDamage(p);
        VelocityModifier.cancelKnockback(p);
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
        p.teleport(SmashWorldManager.getRandomPlayerSpawnLocation(p.getWorld()));
        SmashManager.forgetLastHitter(p);
        Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
            public String call() {
                if (Bukkit.getOnlinePlayers().contains(p))
                {
                    if (!SmashWorldManager.gameHasEnded(p.getWorld()) && !isInSpectatorMode(p))
                    {
                        p.setAllowFlight(false);
                    }
                }
                return "";
            }
        });
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
        if (p.getFlySpeed() != DEFAULT_FLY_SPEED && (!SmashWorldManager.gameHasStarted(p.getWorld()) || isInSpectatorMode(p)))
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
     * Function: sendMessageWithDelay
     * Purpose: To send a message to a world or the whole server with a certain delay in ticks
     *
     * @param message - The String message to be sent
     * @param w - The world to which to send the message
     * @param messageAll - Do you want to message the whole server?
     * @param delay - The delay in ticks before the message will be sent
     */
    protected static void sendMessageWithDelay(final String message, final World w, boolean messageAll, int delay)
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
                Bukkit.getScheduler().scheduleSyncDelayedTask(SmashManager.getPlugin(), new Runnable() {
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
                Bukkit.getScheduler().scheduleSyncDelayedTask(SmashManager.getPlugin(), new Runnable() {
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
    protected static void sendMessageToWorld(World w, String message, boolean sendToAfks)
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

    protected static void sendMessageOutsideWorld(World w, String message)
    {
        for (Player p : Bukkit.getOnlinePlayers())
        {
            World playerWorld = p.getWorld();
            if (!p.getWorld().equals(w) && !(SmashWorldManager.isSmashWorld(playerWorld) && SmashWorldManager.gameHasStarted(playerWorld)))
            {
                p.sendMessage(message);
            }
        }
    }

    protected static HashMap<Integer,ArrayList<Player>> getOpenWorldGuis() {
        return openWorldGuis;
    }

    /**
     * Function: refreshWorldGui
     * Purpose: To build a player's gui so that he can see relavent worlds
     *
     * @param p - The play of whom to build the gui
     */
    static void refreshWorldGui(final Player p)
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(SmashManager.getPlugin(), new Runnable() {
            public void run()
            {
                p.openInventory(getWorldGui(p));
                Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
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
     * Purpose: To build the world guis of all players who have a certain Tourney level
     *
     * @param tourneyLevel - The tourney level of the players for whom you want to build the world gui
     */
    static void refreshWorldGui(int tourneyLevel)
    {
        for (Player p : openWorldGuis.get(tourneyLevel))
        {
            refreshWorldGui(p);
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
     * Function: getAvaliableWorlds
     * Purpose: To get the list of worlds that a player can currently join
     *
     * @param p - The player for which to get avaliable worlds
     * @return - The list of worlds that the player can join
     */
    public static List<World> getAvaliableWorlds(Player p)
    {
        ArrayList<World> worlds = new ArrayList<World>();
        if (SmashStatTracker.canJoinTourneys(p.getName()))
        {
            for (Integer i : takenTourneyWorldNumbers)
            {
                if (getTourneyLevel(i) == SmashStatTracker.getTourneyLevel(p) && !isBeingDeleted(getTourneyWorldName(SmashStatTracker.getTourneyLevel(p), i)))
                {
                    World w = getTourneyWorld(i);
                    if (w != null)
                    {
                        worlds.add(w);
                    }
                }
            }
        }

        for (Integer i : takenFreeplayWorldNumbers)
        {
            if (!isBeingDeleted(getFreeplayWorldName(i)))
            {
                World w = getFreeplayWorld(i);
                if (w != null)
                {
                    worlds.add(w);
                }
            }
        }
        return worlds;
    }

    public static boolean hasSpecialPermissions(Player p)
    {
        return p.hasPermission(SmashManager.getPlugin().getCommand(CREATE_WORLD_CMD).getPermission());
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
            representingItems.add(getWorldRepresenter(w, p));
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
    static boolean hasWorldGuiOpen(Player p)
    {
        return p.getOpenInventory().getTitle().equals(getWorldGuiLabel(p));
    }

    /**
     * Function: openWorldGui
     * Purpose: To display the world gui to a player
     *
     * @param p - The player for which to open a world gui
     */
    protected static void openWorldGui(final Player p)
    {
        Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
            public String call()
            {
                if (!SmashWorldManager.isSmashWorld(p.getWorld())
                        || !SmashWorldManager.gameHasStarted(p.getWorld()) || SmashWorldManager.gameHasEnded(p.getWorld()) || isInSpectatorMode(p))
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
    protected static ItemStack getTourneyWorldCreationItem(int tourneyLevel)
    {
        ItemStack tourneyWorldCreationItem =SmashManager.getPlugin().getCustomItemStack(Material.DIAMOND, ChatColor.AQUA + "Create a Tournament world");
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
    protected static ItemStack getFreeplayWorldCreationItem()
    {
        return source.getCustomItemStack(Material.GOLD_INGOT, ChatColor.GOLD + "Create a Freeplay world");
    }

    /**
     * Function:
     * Purpose:
     *
     * @param p
     */
    protected static void updateHitCooldown(Player p)
    {
        if (SmashWorldManager.isSmashWorld(p.getWorld()))
        {
            p.setMaximumNoDamageTicks(HIT_TICKS);
        }
        else
        {
            p.setMaximumNoDamageTicks(20);
        }
    }

    /**
     * Function: updateHitCooldown
     * Purpose: Updates the hit cooldown for everyone in the given world
     * @param w - The world for which we would like to update everyone's hit cooldown
     */
    protected static void updateHitCooldown(World w)
    {
        for (Player p : w.getPlayers())
        {
            updateHitCooldown(p);
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
    protected static void handleJoin(final Player p, final World w, boolean wasFromLogging, boolean wasFromSpectation)
    {
        if (SmashWorldManager.isSmashWorld(w))
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

            if (!SmashWorldManager.gameHasEnded(w))
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
                SmashWorldManager.startWorld(w);
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
            if (hasSpecialPermissions(p) && p.isFlying())
            {
                p.setGameMode(GameMode.CREATIVE);
            }
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @param p
     */
    static void initializeSmashPlayer(Player p)
    {
        World w = p.getWorld();
        if (SmashWorldManager.isSmashWorld(w))
        {
            if (!SmashWorldManager.gameHasStarted(w))
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
            if (SmashWorldManager.gameHasStarted(w) && !SmashWorldManager.gameHasEnded(w))
            {
                p.teleport(SmashWorldManager.getRandomPlayerSpawnLocation(w));
            }
            if (canBeJoinedRightNow(p, w) && SmashScoreboardManager.hasScoreboard(w) && !SmashScoreboardManager.getScoreboardEntries(w).contains(SmashScoreboardManager.getPlayerEntryName(p)))
            {
                SmashScoreboardManager.setPlayerScoreValue(p, getDeathMatchLives(p.getWorld()));
            }
            SmashManager.clearDamage(p);
            SmashKitManager.getSelectedKit(p).applyKitInventory(p);
        }
        else
        {
           SmashManager.getPlugin().sendErrorMessage("Error! Tried to Smash initialize a player who was not in a Smash world!");
        }
    }

    /**
     * Function:
     * Purpose:
     *
     * @param p
     */
    protected static void resetPlayer(final Player p)
    {
        p.setFoodLevel(20);
        SmashItemManager.cancelItemTasks(p);
        PortalManager.cancelPortalTasks(p);
        if (!isInSpectatorMode(p) && !hasSpecialPermissions(p))
        {
            p.setAllowFlight(false);
        }

        if (!SmashWorldManager.isSmashWorld(p.getWorld()) || isInSpectatorMode(p))
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
        if (!hasSpecialPermissions(p) || SmashWorldManager.isSmashWorld(p.getWorld()))
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
    protected static void handleLeave(final Player p, final World w, final boolean wasFromLogging, boolean justForSpectation)
    {
        if (SmashWorldManager.isSmashWorld(w))
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
            if (isDeathMatchWorld(w) && SmashWorldManager.gameHasStarted(w) && !SmashWorldManager.gameHasEnded(w))
            {
                performDeathMatchDeath(p, w, !justForSpectation);
            }
            SmashManager.forgetLastHitter(p);
            if (FTPAccessor.isWorking())
            {
                FTPAccessor.saveProfile(p);
            }
            else if (hasSpecialPermissions(p))
            {
                p.sendMessage(ChatColor.RED + "Your profile will not immediately be updated on the website.");
            }

            //SmashKitManager.deselectKit(p);
            if (getLivingPlayers(w).size() <= 1 && wasFromLogging || getLivingPlayers(w).size() == 0 && !wasFromLogging)
            {
                SmashWorldManager.removeGame(w, false, false, false);
            }
            else if (SmashWorldManager.gameHasStarted(w) && !SmashWorldManager.gameHasEnded(w))
            {
                recordKOStatistics(p, w);
                if (!justForSpectation && (wasFromLogging || !isDeathMatchWorld(w)) && !isInSpectatorMode(p))
                {
                    sendMessageToWorld(w, ChatColor.RED + p.getName() + " quit!");
                }
            }
        }
    }

    static void setSpectatorMode(Player p, List<Player> players, boolean spectate)
    {
        if (spectate)
        {
            if (!isInSpectatorMode(p))
            {
                spectators.add(p);
                if (SmashWorldManager.isSmashWorld(p.getWorld()))
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
                if (SmashWorldManager.isSmashWorld(p.getWorld()))
                {
                    p.sendMessage(ChatColor.GREEN + "" + ChatColor.ITALIC + "You are no longer in spectator mode.");
                }
            }
            if (SmashWorldManager.isSmashWorld(p.getWorld()))
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

    protected static boolean isHated(Player p, World w)
    {
        if (SmashWorldManager.isSmashWorld(w))
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

    protected static boolean canBeJoinedRightNow(Player p, World w)
    {
        return (!SmashWorldManager.gameHasStarted(w) || SmashWorldManager.gameHasEnded(w) || !SmashWorldManager.isTourneyWorld(w) && !isDeathMatchWorld(w)) && getAvaliableWorlds(p).contains(w);
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
        if (SmashWorldManager.isSmashWorld(w))
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
    static int getWorldNumber(String worldName)
    {
        int lengthOfStuffbeforeWorldNumber = SmashWorldManager.SMASH_WORLD_PREFIX.length() + FREEPLAY_INDICATOR.length() + WORLD_INDICATOR.length();
        if (SmashWorldManager.isTourneyWorld(worldName))
        {
            lengthOfStuffbeforeWorldNumber = SmashWorldManager.SMASH_WORLD_PREFIX.length() + TOURNEY_INDICATOR.length() + 1 + WORLD_INDICATOR.length();
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
    static String getWorldDescription(World w)
    {
        if (SmashWorldManager.isTourneyWorld(w))
        {
            return "Smash " + getSimpleWorldName(w) + " [Round " + SmashWorldManager.getTourneyLevel(w) + "]";
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
    static String getSimpleWorldName(World w)
    {

        if (SmashWorldManager.isTourneyWorld(w))
        {
            return ChatColor.AQUA + TOURNEY_WORLD_ITEM_PREFIX + getWorldNumber(w);
        }
        if (SmashWorldManager.isSmashWorld(w) && !SmashWorldManager.isTourneyWorld(w))
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
    static String getWorldListName(World w, Player p)
    {
        return getSimpleWorldName(w) + " (" + getShortWorldName(w)  + ")" + ChatColor.BLUE + " | " + ChatColor.YELLOW + "Status: " + getWorldStatus(w, p);
    }

    /**
     * Function:
     * Purpose:
     *
     * @param w
     * @return
     */
    static String getWorldStatus(World w, Player p)
    {
        if (p != null && SmashWorldManager.isTourneyWorld(w))
        {
            int tLevel = SmashWorldManager.getTourneyLevel(w);
            if (!SmashStatTracker.canJoinTourneys(p.getName()))
            {
                return ChatColor.RED + "Locked";
            }
            else if (tLevel != SmashStatTracker.getTourneyLevel(p))
            {
                return ChatColor.RED + "Req. level " + tLevel;
            }
        }

        String status;
        if (SmashWorldManager.isBeingDeleted(w.getName()))
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
        else if (SmashWorldManager.gameHasStarted(w))
        {
            status = ChatColor.RED + "In progress";
        }
        else if (SmashWorldManager.gameHasEnded(w))
        {
            status = ChatColor.GOLD + "Game over";
        }
        else if (SmashWorldManager.isStartingSoon(w))
        {
            status = ChatColor.GREEN + "Starting soon";
        }
        else
        {
            status = ChatColor.GREEN + "Waiting for players";
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
        String s = w.getName().replaceAll(SmashWorldManager.SMASH_WORLD_PREFIX, "");
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
    private static ItemStack getWorldRepresenter(World w, Player p)
    {

        ItemStack worldRepresenter;
        if (w == null || !SmashWorldManager.isSmashWorld(w))
        {
            String description = "null";
            if (w != null)
            {
                description = "non-smash";
            }
            SmashManager.getPlugin().sendErrorMessage("Error! Could not get an item representation for a " + description + " world!");
            worldRepresenter = source.getCustomItemStack(Material.BEDROCK, ChatColor.RED + "Bad world!!");
        }
        else
        {
            if (SmashWorldManager.isTourneyWorld(w))
            {
                worldRepresenter = source.getCustomItemStack(Material.DIAMOND_SWORD, getSimpleWorldName(w));
            }
            else
            {
                worldRepresenter = source.getCustomItemStack(Material.GOLD_SWORD, getSimpleWorldName(w));
            }

            if (worldRepresenter == null)
            {
                worldRepresenter = source.getCustomItemStack(Material.INK_SACK, ChatColor.RED + "Not a smash world!", 1);
            }
            ArrayList<String> itemLores = new ArrayList<String>();
            itemLores.add(ChatColor.GREEN + "Players: " + getLivingPlayers(w).size());
            itemLores.add(ChatColor.GREEN + "Map: " + getMapName(w));
            determineDeathMatchPreference(w);
            String type = ChatColor.GREEN + "Point-based";
            if (isDeathMatchWorld(w))
            {
                type = ChatColor.RED + "Deathmatch";
            }
            if (SmashWorldManager.gameHasStarted(w) && !SmashWorldManager.gameHasEnded(w))
            {
                if (!isDeathMatchWorld(w))
                {
                    itemLores.add(ChatColor.RED + "" + ChatColor.ITALIC + "Time til end: " + SmashScoreboardManager.getTimeRemaining(SmashScoreboardManager.getTimeRemaining(w)));
                }
                itemLores.add(ChatColor.YELLOW + "Status: " + getWorldStatus(w, p));
                itemLores.add("Type: " + type);
            }
            else if (getLivingPlayers(w).size() > 0)
            {
                itemLores.add(ChatColor.YELLOW + "Status: " + getWorldStatus(w, p));
                itemLores.add(ChatColor.YELLOW + "Tentative type: " + type);
            }
            if (SmashWorldManager.isTourneyWorld(w))
            {
                itemLores.add(ROUND_LORE_INDEX, ChatColor.GRAY + "" + ChatColor.ITALIC + "Round " + SmashWorldManager.getTourneyLevel(w));
            }
            ItemMeta meta = worldRepresenter.getItemMeta();
            meta.setLore(itemLores);
            worldRepresenter.setItemMeta(meta);
        }
        return worldRepresenter;
    }

    protected static boolean isWorldRepresenter(ItemStack itemClicked)
    {
        if (!itemClicked.getItemMeta().hasDisplayName())
        {
            return false;
        }
        String name = itemClicked.getItemMeta().getDisplayName();
        return !name.toLowerCase().contains("create") && (name.toLowerCase().contains("world") || name.contains(FREE_WORLD_ITEM_PREFIX) || name.contains(TOURNEY_WORLD_ITEM_PREFIX));
    }

    /**
     * Function:
     * Purpose:
     *
     * @param item
     * @return
     */
    protected static World getWorldRepresented(ItemStack item)
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

        String worldName = SmashWorldManager.SMASH_WORLD_PREFIX + worldType + WORLD_INDICATOR + (Integer.valueOf("" + name.charAt(name.length() - 1)));
        if (Bukkit.getWorld(worldName) == null)
        {
           SmashManager.getPlugin().sendErrorMessage("Error! The world that that item represents could not be found!");
            return SmashWorldManager.FALLBACK_WORLD;
        }
        else return Bukkit.getWorld(worldName);
    }

    /**
     * Function:
     * Purpose:
     *
     * @param w
     * @return
     */
    protected static String getMapName(World w)
    {
        if (Bukkit.getWorlds().contains(w))
        {
            return getMapName(w.getName());
        }
       SmashManager.getPlugin().sendErrorMessage("Error! Could not find world " + w.getName() + " and therefore could not get its map name!");
        return null;
    }

    /**
     * Function:
     * Purpose:
     *
     * @param worldFileName
     * @return
     */
    static String getMapName(String worldFileName)
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
           SmashManager.getPlugin().sendErrorMessage("mapName.txt was missing from the world " + worldFileName);
        }
        return "unnamed";
    }

    /**
     * Function:
     * Purpose:
     *
     * @param p
     */
    protected static void removeSmashGuiOpener(Player p)
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
    protected static void giveSmashGuiOpener(final Player p)
    {
        giveSmashGuiOpener(p, false);
        if (!SmashWorldManager.isSmashWorld(p.getWorld()))
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
        Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
            public String call()
            {
                if (SmashWorldListener.SMASH_GUI_ITEM != null && !p.getInventory().contains(SmashWorldListener.SMASH_GUI_ITEM) && (!SmashWorldManager.isSmashWorld(p.getWorld()) || isInSpectatorMode(p)))
                {
                    p.getInventory().setItem(8, SmashWorldListener.SMASH_GUI_ITEM);
                    p.updateInventory();
                }
                if (!alreadyChecked)
                {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(SmashManager.getPlugin(), new Runnable() {
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

    protected static HashMap<World,List<Player>> getReadyPlayers() {
        return readyPlayers;
    }

    /**
     * Function:
     * Purpose:
     *
     * @param tourneyLevel
     * @param worldNumber
     * @return
     */
    protected static String getTourneyWorldName(int tourneyLevel, int worldNumber)
    {
        return SmashWorldManager.SMASH_WORLD_PREFIX + SmashWorldInteractor.TOURNEY_INDICATOR + tourneyLevel + SmashWorldInteractor.WORLD_INDICATOR + worldNumber;
    }

    /**
     * Function:
     * Purpose:
     *
     * @param worldNumber
     * @return
     */
    protected static String getFreeplayWorldName(int worldNumber) {
        return SmashWorldManager.SMASH_WORLD_PREFIX + SmashWorldInteractor.FREEPLAY_INDICATOR + SmashWorldInteractor.WORLD_INDICATOR + worldNumber;
    }


    protected static ArrayList<String> getParticipants() {
        return participants;
    }
}
