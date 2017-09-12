package me.happyman.worlds;

import me.happyman.MetaWorldGenerator;
import me.happyman.Other.ForcefieldManager;
import me.happyman.Plugin;
import me.happyman.SpecialItems.MetaItems;
import me.happyman.SpecialItems.SmashItemDrops.ItemDropManager;
import me.happyman.SpecialItems.SmashItemDrops.MonsterEgg;
import me.happyman.SpecialItems.SmashItemDrops.SmashOrbTracker;
import me.happyman.SpecialItems.SmashKitMgt.SmashKitManager;
import me.happyman.SpecialItems.SpecialItem;
import me.happyman.SpecialItems.SpecialItemTypes.PlaceableItem;
import me.happyman.SpecialItems.UsefulItemStack;
import me.happyman.WorldGenerationTools;
import me.happyman.utils.*;
import me.happyman.utils.Music.Song;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.Validate;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.weather.WeatherChangeEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.Vector;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;

import static me.happyman.Plugin.*;
import static me.happyman.SpecialItems.MetaItems.getRandomSpecialItem;
import static me.happyman.SpecialItems.SmashKitMgt.SmashKitManager.OWNED_KITS_DATANAME;
import static me.happyman.SpecialItems.SmashKitMgt.SmashKitManager.getKit;
import static me.happyman.utils.FileManager.*;
import static me.happyman.utils.SmashAttackManager.*;
import static me.happyman.worlds.EconomyItemManager.*;
import static me.happyman.worlds.MainListener.*;
import static me.happyman.worlds.SmashStatTracker.*;
import static me.happyman.worlds.SmashWorldInteractor.*;
import static me.happyman.worlds.SmashWorldManager.*;
import static me.happyman.worlds.WorldManager.*;

public enum WorldType //can't change enum names
{
    STUFFLAND(1, 1, true, null, true, false, false, true, true, false, GameMode.ADVENTURE, null, 0, 0)
    {
        private static final int SPAWN_PROTECTION_RANGE = 40;

        private boolean isNearSpawn(Location l)
        {
            return l.distanceSquared(l.getWorld().getSpawnLocation()) < SPAWN_PROTECTION_RANGE*SPAWN_PROTECTION_RANGE;
        }

        private boolean isNearSpawn(Block b)
        {
            return isNearSpawn(b.getLocation());
        }

        private boolean isNearSpawn(Player p)
        {
            return isNearSpawn(p.getLocation());
        }

        @Override
        boolean blockIsProtected(Block block, Player whoToTellIsProtected, boolean placeEvent)
        {
            return super.blockIsProtected(block, whoToTellIsProtected, placeEvent) || isNearSpawn(block.getLocation());
        }

        @Override
        boolean isAllowedToTeleport(Player p, Location location)
        {
            if (!super.isAllowedToTeleport(p, location))
            {
                return false;
            }
            if (getWorldType(location.getWorld()) == this && isNearSpawn(p) && !isNearSpawn(location))
            {
                p.sendMessage(ChatColor.YELLOW + "You aren't allowed to participate in Redstone Minigames if you're within " + SPAWN_PROTECTION_RANGE + " blocks of spawn.");
                return false;
            }
            return true;
        }

        @Override
        protected WorldStatus getWorldStatus(World w, CommandSender p)
        {
            return new WorldStatus("Random", WorldStatus.JoinableStatus.JOINABLE);
        }

        @Override
        public String getDisplayPrefix(World world)
        {
            return ChatColor.WHITE + "" + ChatColor.BOLD + "Stuffland";
        }

        @Override
        protected Material getJoinTheWorldMaterial(World w)
        {
            return Material.WOOD_SWORD;
        }

        @Override
        public WorldCreatorItem[] generateWorldCreatorItems()
        {
            ItemStack creationItem = new ItemStack(Material.WOOD);
            ItemMeta meta = creationItem.getItemMeta();
            meta.setDisplayName(getDisplayPrefix(null));
            creationItem.setItemMeta(meta);
            return new WorldCreatorItem[]
            {
                new WorldCreatorItem(this, creationItem)
            };
        }

        @Override
        public void performLoadWorldAction(World w, boolean forCreation)
        {
            super.performLoadWorldAction(w, forCreation);

//            for (Player p : w.getPlayers())
//            {
//                if (!Bukkit.getScoreboardManager().getMainScoreboard().getTeam("R").getEntries().contains(p.getName()) &&
//                    !Bukkit.getScoreboardManager().getMainScoreboard().getTeam("afk").getEntries().contains(p.getName()))
//                {
//                    performResetAction(p);
//                    handleJoin(p, true, w, false);
//                }
//            }

            if (forCreation)
            {
                w.setGameRuleValue("doDaylightCycle", "false");
                w.setGameRuleValue("mobGriefing", "false");
                w.setGameRuleValue("keepInventory", "true");
                w.setDifficulty(Difficulty.PEACEFUL);

                setFallbackWorld(w);
            }
            for (Player player : w.getPlayers())
            {
                World lastWorld = getLastWorld(player);
                if (lastWorld != w)
                {
                    sendPlayerToWorld(player, lastWorld, true, true);
                }
            }
        }


        @Override
        void performEntityDamageByEntity(EntityDamageByEntityEvent event)
        {
            super.performEntityDamageByEntity(event);

            if (!event.isCancelled())
            {
                Location l = event.getDamager().getLocation();
                if (l.distance(l.getWorld().getSpawnLocation()) < 30)
                {
                    if (event.getDamager() instanceof Player)
                    {
                        event.getDamager().sendMessage(ChatColor.RED + "You can't PVP right at spawn.");
                    }
                    event.setCancelled(true);
                }
            }
        }

        @Override
        protected void handleJoin(final Player joiner, boolean cameFromNullWorld, final World w, boolean fromSpectation)
        {
            super.handleJoin(joiner, cameFromNullWorld, w, fromSpectation);
            openSmashGui(joiner, true);
            Bukkit.getScoreboardManager().getMainScoreboard().getTeam("afk").removeEntry(joiner.getName());
            Bukkit.getScoreboardManager().getMainScoreboard().getTeam("R").addEntry(joiner.getName());
        }

        @Override
        protected void handleLeave(Player leaver, World w, boolean goingToNullWorld, boolean fromSpectation)
        {
            super.handleLeave(leaver, w, goingToNullWorld, fromSpectation);
            Bukkit.getScoreboardManager().getMainScoreboard().getTeam("R").removeEntry(leaver.getName());
            Bukkit.getScoreboardManager().getMainScoreboard().getTeam("afk").removeEntry(leaver.getName());
        }

        @Override
        public boolean handleCommand(Player sender, String label, String[] args)
        {
            if (super.handleCommand(sender, label, args))
            {
                return true;
            }
            if (matchesCommand(label, SPAWN_CMD))
            {
                sender.teleport(getFallbackLocation());
                return true;
            }
            else if (matchesCommand(label, FOOD_CMD))
            {
                int distance = (int)sender.getLocation().distance(sender.getWorld().getSpawnLocation());
                int requiredCloseness = 50;
                if (distance <= requiredCloseness)
                {
                    ItemStack foodStack = new ItemStack(Material.COOKED_BEEF);
                    foodStack.setAmount(foodStack.getMaxStackSize());
                    sender.getInventory().addItem(foodStack);
                }
                else
                {
                    sender.sendMessage(ChatColor.RED + "You must be " + (distance - requiredCloseness) + " blocks closer to spawn to use this command.");
                }
            }
            else if (matchesCommand(label, SPECTATE_COMMAND))
            {
                if (hasSpecialPermissions(sender))
                {
                    setSpectatorMode(sender, !isInSpectatorMode(sender));
                }
                else
                {
                    sender.sendMessage(ChatColor.RED + "You are not allowed to use this command in this world.");
                }
                return true;
            }
            return true;
        }
    },
    SMASH(0, 60,true, null, true, false, true, true, false, false, GameMode.ADVENTURE, null, 0, 0)
    {
        private static final int PLAYERS_TO_START_AUTOMATICALLY = 10;
        private final HashMap<Integer, Integer> tourneyLevels = new HashMap<Integer, Integer>();
        private final int KILLER_DELAY = 7; //The ticks between each check for a player being outside the KO zone
        private final float READY_PERCENT_TO_START = 0.65F;
        private final int END_GAME_TIME = 10; //The amount of time before a world resets after a Smash game ends
        private final int DEATH_DISTANCE = 150; //The distance from the center at which players will be assumed to be KO'ed and a ping sound will play
        private final int MAX_BELOW = 30; //The distance below the center which will count as a KO
        private final int FREE_GAME_TIME = 300; //The seconds in a freeplay game
        private final int FREE_WAIT_TIME = (int)(Math.round(FREE_GAME_TIME/6)); //The number of seconds before an initialized freeplay game will start
        private final int TOURNEY_GAME_TIME = 480; //The seconds in a tourney game.
        private final int TOURNEY_WAIT_TIME = (int)(Math.round(TOURNEY_GAME_TIME/6)); //The number of seconds before an initialized tournament game will try to start
        private final int FREE_PLAYER_REQUIREMENT = 2; //The minimum players that must join before a freeplay game will start
        private final int TOURNEY_PLAYER_REQUIREMENT = 4; //The minimum players that must join before a tournament game will start
        private final int SEARCH_HEIGHT = 30; //The maximum height to search for spawn locations
        private final String PLAYER_SPAWN_FILENAME = "PlayerSpawnLocations.txt"; //The colorlessName of the player spawn location file which stores player spawn locations (without the extension)
        private final String ITEM_SPAWN_FILENAME = "ItemSpawnLocations.txt"; //The colorlessName of the item spawn location file which stores item spawn locations...
        private final HashMap<World, Integer> killerTasks = new HashMap<World, Integer>(); //The task for checking if people are outside the KO zone
        private final HashMap<World, Integer> doubleJumpTasks = new HashMap<World, Integer>(); //The task for checking if people are on the ground for recharging their items
        private final List<World> startGameOverrides = new ArrayList<World>(); //This is used when a staff member wants to start a game after a delay
        private final HashMap<World, Integer> timeRemainingTasks = new HashMap<World, Integer>(); //The task for keeping track of the time remaining in a started Smash game
        private final HashMap<World, Integer> itemSpawnTasks = new HashMap<World, Integer>(); //The tasks for spawning items at random locations in a Smash game
        private final ArrayList<String> participants = new ArrayList<String>(); //The people in a game who have gotten a KO and therefore aren't AFK
        private final HashMap<World, Integer> deathMatchWorlds = new HashMap<World, Integer>(); //The number of lives that was selected for a deathmatch game (or 0 if it's not a deathmatch game)
        private final HashMap<World, Integer> originalPlayerCount = new HashMap<World, Integer>(); //The number of players that started out in a game, used to see when a tourney game should end
        private final String START_TIME_SCORE = ChatColor.AQUA + "Time til start:";
        private final String FINAL_SCORES = ChatColor.GOLD + "   " + ChatColor.BOLD + "Final Scores";
        private final HashMap<World, Integer> timeRemaining = new HashMap<World, Integer>();
        private final HashMap<World, Integer> countdownTasks = new HashMap<World, Integer>(); //The task for keeping track of time til start in a Smash game
        private final Random r = new Random();

        private void deleteTourneyLevelFile(World world)
        {
            File f = getTourneyLevelFile(world, false);
            if (f.exists())
            {
                f.delete();
            }
        }

        private File getTourneyLevelFile(World world, boolean forceValid)
        {
            return FileManager.getWorldFile(world, "", "TourneyLevel.txt", forceValid);
        }

        private void setTourneyLevel(World world, Integer level)
        {
            if (world == null)
            {
                return;
            }
            if (level == null || level < LOWEST_TOURNEY_LEVEL)
            {
                deleteTourneyLevelFile(world);
            }
            else
            {
                if (level > HIGHEST_TOURNEY_LEVEL)
                {
                    level = HIGHEST_TOURNEY_LEVEL;
                }
                FileManager.putFileContents(getTourneyLevelFile(world, true), level);
            }
            tourneyLevels.put(getWorldNumber(world), level);
        }

        private Integer getTourneyLevelFromFile(World world)
        {
            File f = getTourneyLevelFile(world, false);;
            try
            {
                return !f.exists() ? null : Integer.valueOf(FileManager.readFileContents(f));
            }
            catch (NumberFormatException ex)
            {
                deleteTourneyLevelFile(world);
            }
            return null;
        }

        /**
         * Function: getTourneyLevel
         * Purpose: To get the tourney level of a world (which doesn't change)
         *
         * @param w - The world for which to get the tourney level
         * @return The tourney level of the world, or -1 if it's invalid
         */
        private Integer getTourneyLevel(World w)
        {
            return getTourneyLevel(getWorldNumber(w));
        }

        /**
         * Function: getTourneyLevel
         * Purpose: To get the tourney level of a world given its world number
         *
         * @param worldNumber - The number of the world for which to get the tourney level
         * @return - The level of the world
         */
        private Integer getTourneyLevel(int worldNumber)
        {
            Integer cachedResult = tourneyLevels.get(worldNumber);
            if (cachedResult == null)
            {
                cachedResult = getTourneyLevelFromFile(getWorld(worldNumber));
                tourneyLevels.put(worldNumber, cachedResult);
            }
            return cachedResult;
        }

        @Override
        public void performDropItem(PlayerDropItemEvent event)
        {
            super.performDropItem(event);
            event.setCancelled(true);
        }

        @Override
        public WorldCreatorItem[] generateWorldCreatorItems()
        {
            WorldCreatorItem[] result = new WorldCreatorItem[HIGHEST_TOURNEY_LEVEL - LOWEST_TOURNEY_LEVEL + 2];

            ItemStack creationItem = new ItemStack(Material.GOLD_INGOT);
            ItemMeta gMeta = creationItem.getItemMeta();
            gMeta.setDisplayName(getDisplayPrefix(false));
            creationItem.setItemMeta(gMeta);
            result[0] = new WorldCreatorItem(this, creationItem);

            for (int i = 1, tourneyLevel = LOWEST_TOURNEY_LEVEL; tourneyLevel <= HIGHEST_TOURNEY_LEVEL; i++, tourneyLevel++)
            {
                creationItem = new ItemStack(Material.DIAMOND);
                ItemMeta meta = creationItem.getItemMeta();
                ArrayList<String> lore = new ArrayList<String>();
                lore.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "Round " + tourneyLevel);
                meta.setLore(lore);
                meta.setDisplayName(getDisplayPrefix(true));
                creationItem.setItemMeta(meta);

                final int finalTourneyLevel = tourneyLevel;
                result[i] = new WorldCreatorItem(this, creationItem)
                {
                    @Override
                    public void performAction(Player clicker)
                    {
                        createWorld(clicker, true, finalTourneyLevel);
                    }
                };
            }

            return result;
        }


        /**
         * Function: rememberOriginalPlayerCount
         * Purpose: To record how many players were in the world at the start of a tournament game
         * @param w - The world for which to record the original player count
         */
        private void rememberOriginalPlayerCount(World w)
        {
            originalPlayerCount.put(w, getNonspectators(w).size());
        }

        /**
         * Function: getOriginalPlayerCount
         * Purpose: To see how many players were originally in a deathmatch game so we know when to end a tournament game
         * @param w - The world we would like to inquire of the original player count
         * @return - The original number of players in that world or null if we don't know or shouldn't have called the function
         */
        private Integer getOriginalPlayerCount(World w)
        {
            Integer result = originalPlayerCount.get(w);
            if (result == null)
            {
                sendErrorMessage("Error! Forgot to remember original Player count in " + w.getName());
            }
            return result;
        }

        /**
         * Clears the original player count record from memory
         * @param w - The world for which we don't need to know the original player count for any more
         */
        private void forgetOriginalPlayerCount(World w)
        {
            originalPlayerCount.remove(w);
        }

        protected void setTimeRemaining(World w, int timeLeft)
        {
            if (gameHasStarted(w))
            {
                timeRemaining.put(w, timeLeft);
                if (timeLeft <= 5 && timeLeft > 0)
                {
                    sendMessageToWorld(w, ChatColor.GOLD + " " + timeLeft);
                    playCountdownSound(w.getPlayers());
                }
                if (!isDeathMatchWorld(w))
                {
                    getWorldScoreboard(w).setSideTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "    Score [" + getTimeRemaining(timeLeft) + "]  ");
                }
            }
        }


//    private static final HashMap<World, Team> scoreTeams = new HashMap<World, Team>();
//    private static final HashMap<World, Objective> scoreObjectives = new HashMap<World, Objective>();
//    private static final HashMap<World, Objective> damageObjectives = new HashMap<World, Objective>();

//    protected static void initializeSmashScoreboard(final World w)
//    {
//        if (w != null && !scoreboards.containsKey(w) && SmashWorldManager.isSmashWorld(w))
//        {
//            Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
//            scoreboards.put(w, scoreboard);
//
//            Objective rightSide = scoreboard.registerNewObjective("score", "dummy");
//            rightSide.setDisplaySlot(DisplaySlot.SIDEBAR);
//            scoreObjectives.put(w, rightSide);
//            setSideTitle(w, ChatColor.GOLD + "" + ChatColor.BOLD + "    Map: " + getMapName(w) + "  ");
//
//            Objective belowName = scoreboard.registerNewObjective("damage", "dummy");
//            belowName.setDisplaySlot(DisplaySlot.BELOW_NAME);
//            belowName.setDisplayName(ChatColor.WHITE + "%");
//            damageObjectives.put(w, belowName);
//
//            Team team = scoreboard.registerNewTeam("scores");
//            team.setPrefix(ChatColor.AQUA + "");
//            team.setSuffix(ChatColor.AQUA + "");
//            scoreTeams.put(w, team);
//
//            resetStartTime(w);
//            startCountDown(w);
//            enableScoreboard(w);
//        }
//        else if (w != null)
//        {
//            sendErrorMessage("Error! World " + w.getName() + "'s scoreboard was already created!");
//        }
//        else
//        {
//            sendErrorMessage("Error! Tried to initialize a Smash scoreboard in non-smash world!");
//        }
//    }


        private Integer getTimeRemaining(World w)
        {
            Integer time = timeRemaining.get(w);
            if (time != null)
            {
                return time;
            }
            sendErrorMessage("Error! Tried to get the time remaining for a bad world!");
            return -1;
        }

        private String getTimeRemaining(int timeRemaining)
        {
            int minutes = timeRemaining / 60;
            int seconds = timeRemaining % 60;
            String secondsZeroBuffer = "";
            if (seconds < 10)
            {
                secondsZeroBuffer = "0";
            }
            return minutes + ":" + secondsZeroBuffer + seconds;
        }

        private Integer getTimeTilStart(World w)
        {
            Integer timeRemaining = getWorldSideScore(w, START_TIME_SCORE);
            if (timeRemaining != null)
            {
                return timeRemaining;
            }
//            sendErrorMessage(ChatColor.RED + "Tried to get the time remaining for a world that had started!");
            return 0;
        }

        private boolean hasScoreboard(World w)
        {
            return getWorldScoreboard(w).hasSideScoreboard();
        }

        /**
         * Function: startStartCountDown
         * Purpose: To start the countdown for the time remaining til start for a Smash world
         *
         * @param w - The world for which we would like to decrement the time remaining by 1 each second
         */
        private void startCountDown(final World w)
        {
            cancelCountdown(w);
            countdownTasks.put(w, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
            {
                public void run()
                {
                    if (!gameHasStarted(w))
                    {
                        subtractStartTime(w, 1);
                    }
                }
            }, 0, 20));
        }

        /**
         * Function: cancelCoundown
         * Purpose: To deactivateFinalSmash the time til start task in a world if it's going
         *
         * @param w - The world for which to deactivateFinalSmash the time til start
         */
        private void cancelCountdown(World w)
        {
            if (countdownTasks.containsKey(w))
            {
                getPlugin().getServer().getScheduler().cancelTask(countdownTasks.get(w));
                countdownTasks.remove(w);
            }
        }

        private void setStartTime(World w, int seconds)
        {
            UsefulScoreboard scoreboard = getWorldScoreboard(w);
            scoreboard.clearEntries();
            scoreboard.setSideScore(START_TIME_SCORE, seconds);
        }

        private void playCountdownSound(List<Player> players)
        {
            for (Player p : players)
            {
                p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 0.7F, 0.5F);
            }
        }

        private void subtractStartTime(World w, int seconds)
        {
            if (gameHasStarted(w))
            {
                sendErrorMessage("Error! Game in world " + w.getName() + " has already started!");
            }
            else
            {
                int timeLeft = 0;
                if (getTimeTilStart(w) != null)
                {
                    timeLeft = getTimeTilStart(w);
                }
                else
                {
                    sendErrorMessage("Error! Could not get the start time for world " + w.getName());
                }
                int newTime = timeLeft - seconds;
                setStartTime(w, newTime);
                if (newTime > 0)
                {
                    if (newTime < 6 && willStartSoon(w))
                    {
                        playCountdownSound(w.getPlayers());
                        sendMessageToWorld(w, ChatColor.GREEN + " " + newTime);
                    }
                }
                else if (willStartSoon(w))
                {
                    startGame(w);
                }
                else
                {
                    sendMessageToWorld(w, ChatColor.YELLOW + "There must be " + getStartRequirement(w) + " players in order for this game to start! :(", false);
                    resetStartTime(w);
                }
            }
        }

        private void resetStartTime(World w)
        {
            removeStartOverride(w);
            int waitTime = FREE_WAIT_TIME;
            if (isTourneyWorld(w))
            {
                waitTime = TOURNEY_WAIT_TIME;
            }
            getWorldScoreboard(w).clearEntries();
            setStartTime(w, waitTime);
        }

        private void removeScoreboard(World w)
        {
            getWorldScoreboard(w).clearEntries();
        }

//    protected static void enableScoreboard(World w)
//    {
//        for (Player p : w.getPlayers())
//        {
//            enableScoreboard(p);
//        }
//    }
//
//    protected static void enableScoreboard(Player p)
//    {
//        World w = p.getWorld();
//        if (!scoreboards.containsKey(w))
//        {
//            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
//        }
//        else// if (!p.getScoreboard().equals(scoreboards.get(w)))
//        {
//            p.setScoreboard(scoreboards.get(w));
//        }
//    }

        private String getPlayerEntryName(Player p)
        {
            return getPlayerEntryName(p.getName());
        }

        private String getPlayerEntryName(String p)
        {
            return ChatColor.AQUA + p;
        }

        private Integer getPlayerScoreValue(String p, World w)
        {
            return getWorldSideScore(w, getPlayerEntryName(p));
        }

        private Integer getPlayerScoreValue(Player p)
        {
            return getPlayerScoreValue(p.getName(), p.getWorld());
        }

        private void setPlayerScoreValue(Player p, int score)
        {
            setPlayerScoreValue(p.getName(), p.getWorld(), score);
        }

        private void setPlayerScoreValue(String p, World w, int score)
        {
            setWorldSideScore(w, getPlayerEntryName(p), score);
        }

        /**
         * Function: startGame
         * Purpose: To start a Smash game in a world
         *
         * @param w - The world of the game to be started
         */
        private void startGame(World w)
        {
            determineDeathMatchPreference(w);
            if (isDeathMatchWorld(w) && getNonspectators(w).size() <= 1)
            {
                sendMessageToWorld(w, ChatColor.RED + "This game didn't start because it was a deathmatch game with only one player.");
                resetStartTime(w);
            }
            else if (getNonspectators(w).size() == 0)
            {
                sendMessageToWorld(w, ChatColor.RED + "This game didn't start because there was no one in it.");
                resetStartTime(w);
            }
            else
            {
                SmashWorldManager.setGameStarted(w, true);
                forgetReadyState(w);
                rememberOriginalPlayerCount(w);
                SmashWorldInteractor.updateHitCooldown(w);
                removeStartOverride(w);
                cancelCountdown(w);
                UsefulScoreboard scoreboard = getWorldScoreboard(w);
                scoreboard.clearEntries();

                if (!isDeathMatchWorld(w))
                {
                    int gameTime = FREE_GAME_TIME;
                    if (isTourneyWorld(w))
                    {
                        gameTime = TOURNEY_GAME_TIME;
                    }
                    setTimeRemaining(w, gameTime);

                    timeRemainingTasks.put(w, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), getTimeRemainingRunnable(w), 20, 20));
                }
                else
                {
                    scoreboard.setSideTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "Lives");
                }

                if (!itemSpawnTasks.containsKey(w))
                {
                    itemSpawnTasks.put(w, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), getItemSpawnRunnable(w), Math.round(1.0/(getNonspectators(w).size() * ItemDropManager.ITEMSPAWN_CHANCE_PER_PERIOD_PER_PLAYER) * ItemDropManager.ITEMSPAWN_PERIOD / 2), ItemDropManager.ITEMSPAWN_PERIOD));
                }
                else
                {
                    sendErrorMessage("We were already spawning itemDrops!");
                }

                for (Player p : w.getPlayers())
                {
                    if (!isInSpectatorMode(p))
                    {
                        p.setAllowFlight(false);
                        //addPlayerToScoreBoard(p);
                        setPlayerScoreValue(p, getDeathMatchLives(w));
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
                    p.playSound(p.getLocation(), Sound.BLOCK_NOTE_PLING, 1F, 1.1F);
                }
                w.setPVP(true);
            }
        }

        /**
         * Function: respawnTeleport
         * Purpose: Respawns a player, but doesn't record KO statistics
         * @param p - The player we want to respawn
         */
        private void respawnTeleport(final Player p)
        {
            performResetAction(p);
            SmashAttackManager.clearDamage(p);
            VelocityModifier.cancelKnockback(p);
            setIsDead(p, false);
            //resetToKitItems(Player p);
            SmashEntityTracker.setSpeedToZero(p);
            getKit(p).applyKitInventory(p, true);

            allowFullflight(p, false);
            p.teleport(SmashWorldManager.getRandomPlayerSpawnLocation(p.getWorld()));
            SmashManager.forgetLastHitter(p);
            Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable() {
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
         * Function: willStartSoon
         * Purpose: To determine if a game is about start. There's no check for the game already being started, so keep that in mind.
         * @param w - The world we would like to check for being about to start
         * @return - True if someone did /start or if there are enough players for the game to start
         */
        private boolean willStartSoon(World w)
        {
            return startGameOverrides.contains(w) || getStartRequirement(w) != null && getNonspectators(w).size() >= getStartRequirement(w);
        }

        /**
         * Function: getStartRequirement
         * Purpose: Gets the number of players that is needed for a game to start naturally
         * @param w - The world for which we would like to determine how many players are needed
         * @return - The number of players that is needed for the game to start
         */
        private Integer getStartRequirement(World w)
        {
            if (isTourneyWorld(w))
            {
                return TOURNEY_PLAYER_REQUIREMENT;
            }
            return FREE_PLAYER_REQUIREMENT;
        }

        private void removeStartOverride(World w)
        {
            if (startGameOverrides.contains(w))
            {
                startGameOverrides.remove(w);
            }
        }

        /**
         * Function: getItemSpawnRunnable
         * Purpose: To get the runnable which will spawn an item (or not) depending on parameters which you probably shouldn't worry about because you're probably too stupid to figure it out anyway.
         * @param w - The world for which we would like to spawn an item drop... or not
         * @return - The Runnable for spawning an item in a world
         */
        private Runnable getItemSpawnRunnable(final World w)
        {
            return new Runnable()
            {
                public void run()
                {
                    if (gameHasStarted(w) && !gameHasEnded(w))
                    {
                        if (getRandomBoolean(w))
                        {
                            spawnItemDropOrFinalSmash(w);
                        }
                    }
                    else
                    {
                        sendErrorMessage("Error! Tried to spawn item at a bad time!");
                    }
                }
            };
        }

        private void spawnItemDropOrFinalSmash(final World w)
        {
            Location selectedLocation = getRandomItemSpawnLocation(w);
            if (r.nextFloat() < SmashOrbTracker.SMASH_ORB_SPAWN_CHANCE && SmashOrbTracker.canCreateOrb(w))
            {
                selectedLocation = selectedLocation.add(0, 7, 0);
                final Location finalLocation = selectedLocation;
                SmashOrbTracker.createOrb(finalLocation);
            }
            else
            {
                final Location finalLocation = selectedLocation;
                Block block = selectedLocation.getBlock();
                if (block.getType().equals(Material.AIR))
                {
                    w.playSound(selectedLocation, Sound.ENTITY_ITEM_PICKUP, 1, 2);
                    Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
                    {
                        public String call()
                        {
                            final Item item = w.dropItem(finalLocation, ItemDropManager.getRandomItemDrop());
                            //Bukkit.getAttacker("HappyMan").sendMessage("setting velocity of item drop");
                            item.setVelocity(new Vector().zero());
                            return "";
                        }
                    });
                    block.setType(SmashItemManager.MATERIAL_FOR_ITEM_DROPS);
                }
            }
        }

        /**
         * Function: getRandomBoolean
         * Purpose: To return a random boolean (true or false, for the uninitiated), which determines whether an item should be spawned or not based on several parameters.
         * @param w - The world for which we would like to determine if an item should be spawned
         * @return - True if an item should be spawned... there is randomness involved
         */
        private boolean getRandomBoolean(World w)
        {
            return (new Random()).nextFloat() < getNonspectators(w).size() * ItemDropManager.ITEMSPAWN_CHANCE_PER_PERIOD_PER_PLAYER;
        }

        /**
         * Function: getTimeRemainingRunnable
         * Purpose: To get a runnable that will countdown the time remaining in a currently in-progress Smash game, ending it when the time runs out
         *
         * @param w - The world to get the time remaining runnable for
         * @return - The runnable that will countdown the time remaining one time
         */
        private Runnable getTimeRemainingRunnable(final World w)
        {
            return new Runnable()
            {
                public void run()
                {
                    if (w.getPlayers().size() != 0)
                    {
                        if (getTimeRemaining(w) > 0)
                        {
                            setTimeRemaining(w, getTimeRemaining(w) - 1);
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
         * Function: getKillerRunnable
         * Purpose: Gets the runnable that checks for a player being outside the KO zone, and if they are and the game has started, excecutes the necessary respawn procedures.
         * @param w - The world for which we would like to get such a USEFUL runnable!!!
         * @return - The runnable for the world which will is used for Smash games that have or haven't started, but is most important for started games
         */
        private Runnable getKillerRunnable(final World w)
        {
            return new Runnable()
            {
                public void run()
                {
                    for (final Player p : getNonspectators(w))
                    {
                        boolean superFar = p.getLocation().distance(p.getWorld().getSpawnLocation()) > DEATH_DISTANCE;
                        if ((superFar || p.getLocation().getY() < SPAWN_HEIGHT - MAX_BELOW || p.getLocation().getY() < -50) && !SmashWorldManager.isDead(p))
                        {
                            SmashWorldManager.setIsDead(p, true);
                            int tpDelay = 0;
                            org.bukkit.util.Vector v = p.getVelocity();
                            if (superFar)
                            {
                                WorldManager.playSoundToPlayers(p, w.getPlayers(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.7F, 0.95F);
                                tpDelay = 15;
                            }
                            else if (SmashManager.getMagnitude(v.getX(), v.getY(), v.getZ()) > 3.2)
                            {
                                WorldManager.playSoundToPlayers(p, p.getLocation(), w.getPlayers(), Sound.ENTITY_GENERIC_EXPLODE, (float)1, (float)0.04, 1, 6);
                                WorldManager.playSoundToPlayers(p, w.getPlayers(), Sound.ENTITY_BLAZE_DEATH, (float)0.1, (float)0.05);
                                tpDelay = 20;
                            }
                            Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
                                @Override
                                public void run() {
                                    recordKOStatistics(p, w);
                                    if (p.getWorld().equals(w))
                                    {
                                        respawnTeleport(p);
                                    }
                                }
                            }, tpDelay);

                            if (gameHasStarted(w) && !gameHasEnded(w))
                            {
                                //Get the KO message****
                                String koMessage = "null";
                                Random rn = new Random();
                                String deadPlayerName = ChatColor.GOLD + p.getName(); //+ getKit(deadPlayer)
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
                                        killerKit = "(" + SmashKitManager.getKitNameOutput(Bukkit.getPlayer(killerName)) + ")";
                                    }
                                    String deadKit = "";
                                    deadKit = "(" + SmashKitManager.getKitNameOutput(p) + ")";
                                    switch(rn.nextInt(1))
                                    {
                                        case 0: koMessage = deadPlayerName + deadKit + ChatColor.RESET + " was KO'd by " + ChatColor.GREEN +  killerName + killerKit + "'s " + ChatColor.DARK_AQUA + weaponName + ChatColor.RESET + ".";
                                    }
                                }
                                //**************

                                sendMessageToWorld(w, ChatColor.GOLD + koMessage);
                                setPlayerScoreValue(p, getPlayerScoreValue(p) - 1);

                                if (SmashManager.hasLastHitter(p.getName()))
                                {
                                    String lastHitter = SmashManager.getLastHitterName(p.getName());
                                    if (!lastHitter.equals(p.getName()) && !isDeathMatchWorld(w))
                                    {
                                        setPlayerScoreValue(lastHitter, w, getPlayerScoreValue(lastHitter, w) + 1);
                                    }
                                }

                                if (isDeathMatchWorld(w))
                                {
                                    if (getPlayerScoreValue(p) == 0)
                                    {
                                        performDeathMatchDeath(p, w, false);
                                    }
                                }
                            }
                        }
                    }
                }
            };
        }

        /**
         * Function: restartSmashWorld
         * Purpose: To restart a world for restarting purposes
         *
         * @param w - The world to be reset
         */
        private void restartSmashWorld(final World w)
        {
            removeGame(w, false, true, false);
            for (Player p : w.getPlayers())
            {
                setSpectatorMode(p, false);
            }
            sendMessageOutsideWorld(w, ChatColor.GREEN + "World " + getDisplayName(w) + ChatColor.GREEN + " has been restarted!");

            startSmashWorld(w);
        }

        /**
         * Function: endGame
         * Purpose: To end a Smash game, announcing winners and recording statistics, among other things
         *
         * @param w - The world of the game to be ended
         */
        private void endGame(final World w, String whoForcedClose)
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
                        setSpectatorMode(p, false);
                    }
                }
                if (whoForcedClose != null && isDeathMatchWorld(w))
                {
                    sendMessageToWorld(w, ChatColor.RED + "" + ChatColor.BOLD + "The game was forcefully ended by " + whoForcedClose + "!");
                    restartSmashWorld(w);
                }
                else
                {
                    List<String> entries = getWorldScoreboard(w).getColorlessEntries();
                    for (String p : entries)
                    {
                        if ((!isDeathMatchWorld(w) || getPlayerScoreValue(p, w) > 0) && !scoreboardPlayerList.contains(ChatColor.stripColor(p)))
                        {
                            scoreboardPlayerList.add(ChatColor.stripColor(p));
                        }
                    }
                    List<Integer> scores = new ArrayList<Integer>();
                    for (String p : entries)
                    {
                        scores.add(getPlayerScoreValue(p, w));
                    }
                    if (scores.size() > 0)
                    {
                        Collections.sort(scores, Collections.reverseOrder());
                        int minContinueScore;
                        if (isTourneyWorld && getTourneyLevel(w) == SmashStatTracker.getHighestTourneyLevelPossible())
                        {
                            minContinueScore = scores.get(0);
                        }
                        else
                        {
                            while ((!isTourneyWorld || scores.size() > entries.size()* SmashWorldInteractor.TOURNEY_TOP_PERCENT/100 + 1) && scores.size() > 1)
                            {
                                scores.remove(scores.size() - 1);
                            }
                            minContinueScore = scores.get(scores.size() - 1);
                        }
                        for (int i = 0; i < scoreboardPlayerList.size(); i++)
                        {
                            if (getPlayerScoreValue(scoreboardPlayerList.get(i), w) < minContinueScore)
                            {
                                scoreboardPlayerList.remove(i);
                                i--;
                            }
                        }
                    }
                    else
                    {
                        sendErrorMessage("Error! There were no score entries on the scoreboard in " + w.getName());
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
                        p.playSound(p.getLocation(), Sound.ENTITY_ENDERDRAGON_GROWL, 1F, 1F);
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
                    else if (w.getPlayers().size() > 0)
                    {
                        playersString = w.getPlayers().get(0).getName();
                    }
                    else
                    {
                        sendErrorMessage("Critical error! Ended a game that had no players: " + w.getName() + "!");
                        playersString = "ERROR";
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
                    addScoreToTotalPoints(w);
                    boolean messageEverybody = false;
                    UsefulScoreboard scoreboard = getWorldScoreboard(w);
                    scoreboard.setSideTitle(FINAL_SCORES);
                    if (isTourneyWorld)
                    {
                        removeGame(w, false, false, true);
                        for (Player p : w.getPlayers())
                        {
                            if (scoreboardPlayerList.contains(p.getName()))
                            {
                                incrementTourneyRoundsPlayed(getNonspectators(w));
                            }
                        }
                        if (getTourneyLevel(w) < 1)
                        {
                            sendErrorMessage("Error! World " + w.getName() + " has a negative tournament level!");
                        }
                        else if (getTourneyLevel(w) < SmashStatTracker.getHighestTourneyLevelPossible() - 1)
                        {
                            s = ChatColor.GREEN + playersString + " " + moveMoves + " to Round " + (getTourneyLevel(w) + 1) + "!";
                        }
                        else if (getTourneyLevel(w) < SmashStatTracker.getHighestTourneyLevelPossible())
                        {
                            s = ChatColor.GREEN + playersString + " " + moveMoves + " to the Final Round!";
                        }
                        else if (getTourneyLevel(w) == SmashStatTracker.getHighestTourneyLevelPossible())
                        {
                            SmashStatTracker.incrementTourneyGamesPlayed(getNonspectators(w));
                            messageEverybody = true;
                            s = ChatColor.GREEN + playersString + " " + hasHave + " won Smash Tournament #" + FileManager.incrementStatistic(FileManager.getServerDataFile( "", "Stats", true), "Tournaments") + "!";
                        }
                    }
                    else
                    {
                        delay = END_GAME_TIME/3 * 20;
                        removeGame(w, false, true, true);
                        scoreboard.clearEntries();
                        sendMessageToWorld(w, ChatColor.YELLOW + "And the winner is...");
                        for (Player p : w.getPlayers())
                        {
                            if (scoreboardPlayerList.contains(p.getName()))
                            {
                                SmashStatTracker.incrementFreeGamesWon(p);
                            }
                        }

                        incrementFreeGamesPlayed(getNonspectators(w));
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

                    cancelEndGameTask(w);

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
                                        if (!SmashEntityTracker.isAfk(p))
                                        {
                                            halfwaySpeeds.put(p, true);
                                        }
                                    }
                                }
                                i++;
                            }
                            else if (i == iterations)
                            {
                                for (Player p : w.getPlayers())
                                {
                                    if (SmashEntityTracker.getSpeed(p) < 0.001 && halfwaySpeeds.containsKey(p) && !halfwaySpeeds.get(p))
                                    {
                                        WorldManager.sendPlayerToLastWorld(p, false);
                                        p.sendMessage(ChatColor.YELLOW + "You were afk and were kicked!");
                                    }
                                    else
                                    {
                                        World selectedWorld = getBestSmashWorld(p);
                                        if (selectedWorld == null)
                                        {
                                            WorldManager.sendPlayerToLastWorld(p, false);
                                        }
                                        else if (!w.equals(selectedWorld))
                                        {
                                            if (sendPlayerToWorld(p, selectedWorld, false, false))
                                            {
                                                p.sendMessage(ChatColor.GRAY + "Sending you to the next Game now!");
                                            }
                                            else
                                            {
                                                sendErrorMessage("Error! Tried to send " + p.getName() + " to a world he couldn't go to!");
                                            }
                                        }
                                        else
                                        {
                                            p.sendMessage(ChatColor.GRAY + "This world has been restarted!");
                                        }
                                    }
                                }
                                //Bukkit.broadcastMessage("restarting game");
                                restartSmashWorld(w);
                            }
                        }
                    };
                    SmashWorldManager.scheduleEndGameTask(w, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), r, 0, period));
                }
            }
        }

        @Override
        public boolean sendPlayerToWorld(Player p, World w2, boolean forcefully, boolean pretendTheyWerentAlreadyInOne)
        {
            if (!forcefully)
            {
                if (isTourneyWorld(w2) && !SmashStatTracker.canJoinTourneys(p.getName()))
                {
                    p.sendMessage(ChatColor.RED + "You must play " + SmashStatTracker.getGamesTilCanJoinTourneys(p.getName()) + " more games before you can play in tournaments.");
                    return false;
                }
                else if (!getAvaliableWorlds(p).contains(w2))
                {
                    p.sendMessage(ChatColor.RED + "You are not allowed to join this game (at least not right now)!");
                    return false;
                }
                else if (!hasValidPlayerSpawnLocations(w2))
                {
                    p.sendMessage(ChatColor.RED + "Error! There are no spawn locations in map " + getMapName(w2));
                    return false;
                }
                else if (SmashWorldInteractor.isHated(p, w2))
                {
                    p.sendMessage(ChatColor.RED + "Sorry, but the players in this world have decided that they don't want you.");
                    return false;
                }
            }
            p.teleport(getRandomPlayerSpawnLocation(w2));
            return true;
        }

        private int incrementFreeGamesPlayed(Player p)
        {
            incrementPointBasedGamesPlayed(p);
            return FileManager.incrementStatistic(getGeneralPlayerFile(p), FREE_GAMES_PLAYED);
        }

        private void incrementPointBasedGamesPlayed(Player p)
        {
            if (!isDeathMatchWorld(p.getWorld()))
            {
                FileManager.incrementStatistic(getGeneralPlayerFile(p), POINT_BASED_GAMES_PLAYED);
            }
        }

        private void incrementFreeGamesPlayed(List<Player> players)
        {
            for (Player p : players)
            {
                incrementFreeGamesPlayed(p);
            }
        }

        private void incrementTourneyRoundsPlayed(Player p)
        {
            incrementPointBasedGamesPlayed(p);
            FileManager.incrementStatistic(getGeneralPlayerFile(p), TOURNEY_ROUNDS_PLAYED);
        }

        private void incrementTourneyRoundsPlayed(List<Player> players)
        {
            for (Player p : players)
            {
                incrementTourneyRoundsPlayed(p);
            }
        }

        private void addScoreToTotalPoints(World w)
        {
            if (hasScoreboard(w))
            {
                for (String s : getWorldScoreboard(w).getColorlessEntries())
                {
                    Player player = Bukkit.getPlayer(s);
                    if (player != null)
                    {
                        addScoreToTotalPoints(player);
                    }
                }
            }
        }

        private void addScoreToTotalPoints(Player p)
        {
            World w = p.getWorld();
            if (gameHasStarted(w) && !isDeathMatchWorld(w) && getPlayerScoreValue(p) != null)
            {
                int oldScore = FileManager.getIntData(getGeneralPlayerFile(p), POINTS_ACCUMULATED_DATANAME);
                FileManager.putData(getGeneralPlayerFile(p), POINTS_ACCUMULATED_DATANAME, oldScore + getPlayerScoreValue(p));
            }
        }

        private void endGame(World w)
        {
            endGame(w, null);
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
        void removeGame(World w, boolean deletingWorld, boolean removingForRestart, boolean removingForEndGame)
        {
    /*if (!(deletingWorld ^ removingForRestart ^ removingForEndGame) && !(!deletingWorld && !removingForEndGame))
    {
        sendErrorMessage("Warning! Removed a game with more than one intention!");
    }*/
            SmashWorldManager.forgetIfGameIsStarted(w);

            removeScoreboard(w);

            cancelCountdown(w);
            cancelTime(w);
            SmashWorldManager.cancelEndGameTask(w);
            forgetOriginalPlayerCount(w);
            removeStartOverride(w);
            SmashEntityTracker.forgetSpeedFactors(w);
            if (!removingForEndGame)
            {
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
                getKit(p).getProperties().getFinalSmash().performResetAction(p);
                SmashManager.forgetLastHitter(p);
            }

            if (removingForEndGame || removingForRestart)
            {
                w.setPVP(false);
                for (final Player p : players)
                {
                    Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable() {
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
         * Function: cancelTime
         * Purpose: To deactivateFinalSmash the task that counts down the time remaining in an ongoing Smash game
         *
         * @param w - The world for which to stop counting down the time remaining
         */
        private void cancelTime(World w)
        {
            if (itemSpawnTasks.containsKey(w))
            {
                Bukkit.getScheduler().cancelTask(itemSpawnTasks.get(w));
                itemSpawnTasks.remove(w);
            }
            if (timeRemainingTasks.containsKey(w))
            {
                Bukkit.getScheduler().cancelTask(timeRemainingTasks.get(w));
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
        private void cancelConstantTasks(World w)
        {
            if (doubleJumpTasks.containsKey(w))
            {
                Bukkit.getScheduler().cancelTask(doubleJumpTasks.get(w));
                doubleJumpTasks.remove(w);
            }
            if (killerTasks.containsKey(w))
            {
                Bukkit.getScheduler().cancelTask(killerTasks.get(w));
                killerTasks.remove(w);
            }
        }

        //***************************************************

        @Override
        public void performUnloadWorldAction(World w)
        {
            super.performUnloadWorldAction(w);
            int worldNumber = getWorldNumber(w);
            tourneyLevels.remove(worldNumber);
            removeScoreboard(w);
            forgetReadyState(w);
            forgetItemSpawnLocations(w);
            forgetPlayerSpawnLocations(w);

            removeGame(w, true, false, false);

            for (final Player p : w.getPlayers())
            {
                SmashManager.forgetLastHitter(p);
            }
        }

        @Override
        void performProjectileHit(ProjectileHitEvent event)
        {
            super.performProjectileHit(event);

            final Projectile proj = event.getEntity();
            if (proj instanceof Arrow)
            {
                Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
                {
                    public String call()
                    {
                        proj.remove();
                        return "";
                    }
                });
            }
        }

        @Override
        public String getDisplayPrefix(World world)
        {
            return getDisplayPrefix(isTourneyWorld(world));
        }

        public String getDisplayPrefix(boolean hasTourneyLevel)
        {
            return hasTourneyLevel ? (ChatColor.AQUA + "Tourney Game") : (ChatColor.GOLD + "Freeplay Game");
        }

        @Override
        protected Material getJoinTheWorldMaterial(World world)
        {
            return isTourneyWorld(world) ? Material.DIAMOND_SWORD : Material.GOLD_SWORD;
        }

        @Override
        public void performLoadWorldAction(World w, boolean forCreation)
        {
            super.performLoadWorldAction(w, forCreation);
            clearEntities(w);
            if (forCreation)
            {
//                    deleteFile(DirectoryType.ROOT, w.getName(), PLAYER_SPAWN_FILENAME);
//                    deleteFile(DirectoryType.ROOT, w.getName(), ITEM_SPAWN_FILENAME);
                w.setGameRuleValue("doDaylightCycle", "false");
                w.setGameRuleValue("doMobSpawning", "false");
                w.setGameRuleValue("keepInventory", "true");
                w.setGameRuleValue("doTileDrops", "false");
                w.setGameRuleValue("mobGriefing", "false");
                w.setDifficulty(Difficulty.EASY);
                w.setSpawnLocation(0, SPAWN_HEIGHT, 0);
            }
            loadSpawns(w);
            if (getNonspectators(w).size() > 0)
            {
                startSmashWorld(w);
            }
        }

        private File getItemSpawnLocationFile(World world, boolean validate)
        {
            return FileManager.getWorldFile(world, "", ITEM_SPAWN_FILENAME, validate);
        }

        private File getPlayerSpawnLocationFile(World world, boolean validate)
        {
            return FileManager.getWorldFile(world, "", PLAYER_SPAWN_FILENAME, validate);
        }

        /**
         * Function: loadSpawns
         * Purpose: To loadData where on earth the spawn locations are. This is used when a world is created or the plugin is reloaded. Who knows, maybe one day it'll be used for a greater purpose.
         * As of 1/23/2017 these locations were pumpkins for player spawn locations and melons for item spawn locations
         * @param w - The world for which we would like to loadData in spawn locations
         */
        public void loadSpawns(World w)
        {
            if (w == null)
            {
                sendErrorMessage("Error! Could not find null world when finding item locations for it.");
            }
            else
            {
                File playerSpawnFile = getPlayerSpawnLocationFile(w, false);
                File itemSpawnFile = getItemSpawnLocationFile(w, false);
                if (playerSpawnFile.exists() && itemSpawnFile.exists())
                {
                    for (String line : FileManager.readLinesFromFile(playerSpawnFile))
                    {
                        Location readLocation = FileManager.deserializeLocation(w, line);
                        SmashWorldManager.addPlayerSpawnLocation(w, readLocation);
                    }
                    for (String line : FileManager.readLinesFromFile(itemSpawnFile))
                    {
                        Location readLocation = FileManager.deserializeLocation(w, line);
                        SmashWorldManager.addItemSpawnLocation(w, readLocation);
                    }
                }
                else
                {
                    for (int x = -SEARCH_DISTANCE; x <= SEARCH_DISTANCE; x += 2)
                    {
                        for (int z = -SEARCH_DISTANCE; z <= SEARCH_DISTANCE; z += 2)
                        {
                            boolean lastWasSolid = false;
                            for (int y = SPAWN_HEIGHT - SEARCH_HEIGHT; y <= SPAWN_HEIGHT + SEARCH_HEIGHT; y++)
                            {
                                Block blockToCheck = w.getBlockAt(x, y, z);
                                boolean thisIsSolid = blockToCheck.getType().isBlock() && blockToCheck.getType().isSolid();
                                if (lastWasSolid && !thisIsSolid)
                                {
                                    if (r.nextFloat() < 0.5)
                                    {
                                        Location spawnLocation = new Location(w, x + 0.5, y + 0.01, z + 0.5);
                                        spawnLocation.setDirection(SmashManager.getUnitDirection(spawnLocation, w.getSpawnLocation()));
                                        spawnLocation.setPitch(0);
                                        addPlayerSpawnLocation(w, spawnLocation);
                                    }
                                    else
                                    {
                                        addItemSpawnLocation(w, new Location(w, x + 0.5, y + 0.6, z + 0.5));
                                    }
                                }

                                lastWasSolid = thisIsSolid;
//                                boolean foundSpawnBlock = false;
//                                if (blockToCheck.getType().equals(Material.PUMPKIN))
//                                {
//                                    Location spawnLocation = new Location(w, x + 0.5, y + 0.01, z + 0.5);
//                                    spawnLocation.setDirection(SmashManager.getUnitDirection(spawnLocation, w.getSpawnLocation()));
//                                    spawnLocation.setPitch(0);
//                                    addPlayerSpawnLocation(w, spawnLocation);
//                                    foundSpawnBlock = true;
//                                }
//                                else if (blockToCheck.getType().equals(Material.MELON_BLOCK))
//                                {
//                                    addItemSpawnLocation(w, new Location(w, x + 0.5, y + 0.6, z + 0.5));
//                                    foundSpawnBlock = true;
//                                }
//                                if (foundSpawnBlock)
//                                {
//                                    blockToCheck.setType(Material.AIR);
//
//                                    Block blockOn = blockToCheck.getRelative(0, -1, 0);
//                                    if (blockOn.getType() == Material.DIRT)
//                                    {
//                                        blockOn.setType(Material.GRASS);
//                                    }
//                                }
                            }
                        }
                    }

                    ArrayList<Location> playerSpawnLocations = SmashWorldManager.getPlayerSpawnLocations(w);
                    ArrayList<Location> itemSpawnLocations = SmashWorldManager.getItemSpawnLocations(w);
                    if (playerSpawnLocations != null && itemSpawnLocations != null && playerSpawnLocations.size() > 0 && itemSpawnLocations.size() > 0)
                    {
                        FileManager.printLinesToFile(getPlayerSpawnLocationFile(w, true), serializeLocations(playerSpawnLocations));
                        FileManager.printLinesToFile(getItemSpawnLocationFile(w, true), serializeLocations(itemSpawnLocations));
                    }
                    else
                    {
                        sendErrorMessage("Error! We didn't have valid spawn location files or didn't have the means to create them in world " + w.getName() + ". Deleting the world.");
                        deleteWorld(w);
                    }
                }
            }
        }

        @Override
        void performDamage(EntityDamageEvent event)
        {
            super.performDamage(event);
            if (event.isCancelled()) return;

            if (event.getEntity() instanceof Player)
            {
                World world = event.getEntity().getWorld();
                Player player = (Player)event.getEntity();
                EntityDamageEvent.DamageCause cause = event.getCause();
                switch (cause)
                {
                    case ENTITY_ATTACK:
                    case ENTITY_EXPLOSION:
                        break;
                    case FALL:
                        event.setCancelled(true);
                        break;
                    default:
                        event.setDamage(0);
                        if (gameIsInProgress(world))
                        {
                            switch (cause)
                            {
                                case FIRE_TICK:
                                case FIRE:
                                case CONTACT:
                                case DROWNING:
                                    boolean fireImmune = SmashKitManager.getKit(player).isImmuneToFire();
                                    if (fireImmune || isShielded(player))
                                    {
                                        if (!fireImmune)
                                        {
                                            switch (cause)
                                            {
                                                case FIRE:
                                                case FIRE_TICK:
                                                    player.setFireTicks(-20);
                                                    break;
                                            }
                                        }
                                        event.setCancelled(true);
                                    }
                                    else
                                    {
                                        addDamage((Player) event.getEntity(), 4F, true);
                                    }
                                    break;
                            }
                        }
                        break;
                }
            }
        }

        public boolean isShielded(Entity player)
        {
            return player instanceof Player && SmashKitManager.isShielded((Player)player);
        }

        public float getRemainingShield(Entity player)
        {
            return player instanceof Player ? SmashKitManager.getRemainingShield((Player)player) : -1;
        }

        @Override
        public void performEntityDamageByEntity(EntityDamageByEntityEvent event)
        {
            super.performEntityDamageByEntity(event);
            if (event.isCancelled()) return;


            World world = event.getEntity().getWorld();
            event.setDamage(0);
            if (!gameIsInProgress(world) || !(event.getEntity() instanceof Player))
            {
                return;
            }

            WorldType.AttackSource source = WorldManager.getAttackSource(event.getDamager());
            if (source == null || !source.isLiving() || source.getLivingEntity() == event.getEntity())
            {
                event.setCancelled(true);
                return;
            }

            final LivingEntity damager = source.getLivingEntity();
            if (damager instanceof Player)
            {
                final Player victim = (Player)event.getEntity();
                float power = source.getDamage(event);
                Player damagerPlayer = (Player)damager;
                final String damageWeaponName = source.getWeaponName();

                SpecialItem itemUsed = source.getSpecialItem();
                if (itemUsed != null && itemUsed.getItemStack().getType().toString().toLowerCase().contains("sword"));
                {
                    power *= SWORD_BUFF;
                }

                Location l = damagerPlayer.getEyeLocation();
                attackPlayer(damagerPlayer, damageWeaponName, l, victim, power, false);

                repairItem(damagerPlayer, damagerPlayer.getItemInHand());
            }
        }

        @Override
        public boolean playerCanDamagePlayer(Entity attacker, Entity victim)
        {
            if (!super.playerCanDamagePlayer(attacker, victim))
            {
                return false;
            }

            if (victim instanceof Player)
            {
                Player playerVictim = (Player)victim;

                if (isShielded(victim))
                {
                    notifyOfShield(attacker, playerVictim);
                    return false;
                }
                else if (isShielded(attacker))
                {
                    return false;
                }
                else if (isOnHitCooldown(playerVictim))
                {
                    return false;
                }
            }
            return true;
        }

        @Override
        void handlePlayerItemDamageEvent(PlayerItemDamageEvent e)
        {
            super.handlePlayerItemDamageEvent(e);

            repairItem(e.getPlayer(), e.getItem());
        }

        @Override
        public Iterator<SpecialItem> getSpecialItemIterator(LivingEntity player)
        {
            final ArrayList<SpecialItem> items = new ArrayList<SpecialItem>();
            items.addAll(ItemDropManager.getSmashItemDrops());
            if (player != null && player instanceof Player)
            {
                items.addAll(SmashKitManager.getKitItems((Player)player));
            }
            return items.iterator();
        }

//        @Override
//        public SpecialItem getSpecialItem(Player user, ItemStack item)
//        {
//            if (user != null)
//            {
//                SmashKitManager.SmashKit kit = getKit(user);
//                SpecialItem it = kit.getSpecialItem(item);
//                if (it == null)
//                {
//                    SmashKitManager.SmashKit maskKit = getMaskKit(user);
//                    if (maskKit != null && kit != maskKit)
//                    {
//                        it = maskKit.getSpecialItem(item);
//                    }
//                    if (it == null)
//                    {
//                        it = ItemDropManager.getSmashDropItem(item);
//                    }
//                }
//                return it;
//            }
//            else
//            {
//                sendErrorMessage("Error! Tried to get the item a null player was using!");
//                return null;
//            }
//        }


        @Override
        void performPlayerInteract(PlayerInteractEvent event)
        {
            super.performPlayerInteract(event);
            
            if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK) && event.getClickedBlock().getType().toString().toLowerCase().contains("door"))
            {
                event.getPlayer().sendMessage(ChatColor.RED + "The door is locked!");
                event.setCancelled(true);

                for (Player p : event.getPlayer().getWorld().getPlayers())
                {
                    if (p.getLocation().getBlock().getType().toString().toLowerCase().contains("door"))
                    {
                        if (!p.equals(event.getPlayer()))
                        {
                            p.teleport(event.getPlayer());
                        }
                        else
                        {
                            Location l = p.getLocation();
                            org.bukkit.util.Vector x = l.getDirection();
                            p.teleport(new Location(p.getWorld(), l.getX() + x.getX(), l.getY(), l.getZ() + x.getZ()));
                        }
                    }
                }
            }
            else if ((event.getAction().equals(Action.LEFT_CLICK_AIR) || event.getAction().equals(Action.LEFT_CLICK_BLOCK))
                    && SmashOrbTracker.isLookingAtSmashOrb(event.getPlayer()))
            {
                SmashOrbTracker.hitSmashOrb(event.getPlayer());
            }
        }

        @Override
        public void performPlayerDropItem(PlayerDropItemEvent event)
        {
            super.performPlayerDropItem(event);
            if (event.getItemDrop().getItemStack() != null)
            {
                Player p = event.getPlayer();
                SpecialItem item = getSpecialItem(p, event.getItemDrop().getItemStack());

                if (item != null && item.performDropAction(p))
                {
                    event.setCancelled(true);
                }
            }
        }

        @Override
        boolean canBeJoined(World world, Player p)
        {
            return super.canBeJoined(world, p) && (!isTourneyWorld(world) || getTourneyLevel(world) == SmashStatTracker.getTourneyLevel(p));
        }

        private boolean canBeJoinedInRightNow(World world, Player p)
        {
            return canBeJoined(world, p) && (!hasScoreboard(world) || !gameHasStarted(world) && getTimeTilStart(world) > 7 || !isTourneyWorld(world) && !isDeathMatchWorld(world));
        }

        @Override
        public void handleJoin(Player joiner, boolean cameFromNullWorld, World w, boolean fromSpectation)
        {
            super.handleJoin(joiner, cameFromNullWorld, w, fromSpectation);

            if (cameFromNullWorld)
            {
                syncSpectators(joiner);
                SmashWorldListener.displayJoinMessage(joiner);
            }

            if (!fromSpectation && (w.getPlayers().size() == 0 && cameFromNullWorld || w.getPlayers().size() <= 1 && !cameFromNullWorld))
            {
                startSmashWorld(w);
            }
            else if (canBeJoinedInRightNow(w, joiner))
            {
                initializeSmashPlayer(joiner);
                if (getNonspectators(w).size() >= PLAYERS_TO_START_AUTOMATICALLY && !gameHasStarted(w))
                {
                    startGame(w);
                }
            }
            else
            {
                setSpectatorMode(joiner, true);
            }
        }

        private void initializeSmashPlayer(Player p)
        {
            performResetAction(p);
            World w = p.getWorld();
            if (!gameHasStarted(w))
            {
                SmashStatTracker.rememberOldElo(p.getName());
            }
            //Bukkit.getAttacker("HappyMan").sendMessage("setting velocity from smash player initializiation");
            p.setVelocity(p.getVelocity().setY(0));
            if (SmashKitManager.canChangeKit(p))
            {
                allowFullflight(p, true);
            }
            if (gameIsInProgress(w))
            {
                p.teleport(getRandomPlayerSpawnLocation(w));
            }
            if (canBeJoinedInRightNow(w, p) && hasScoreboard(w) && !isOnWorldScoreboard(p))
            {
                setPlayerScoreValue(p, getDeathMatchLives(p.getWorld()));
            }
            SmashAttackManager.clearDamage(p);
            getKit(p).applyKitInventory(p);
        }

        /**
         * Function: startSmashWorld
         * Purpose: To initialize a world's scoreboard and to initialize its players, along with starting necessary Smash world tasks
         *
         * @param w - The world to be initialized
         */
        private void startSmashWorld(World w)
        {
            w.setTime(DEFAULT_WORLD_TIME);
            setBiome(w, Biome.PLAINS);
            w.setPVP(false);

            UsefulScoreboard scoreboard = getWorldScoreboard(w);
            scoreboard.setSideTitle(ChatColor.GOLD + "" + ChatColor.BOLD + "   " + getMapName(w) + "  ");
            scoreboard.setVisibleToAllUsers(true);
            resetStartTime(w);
            startCountDown(w);

            for (Player p : getNonspectators(w))
            {
                initializeSmashPlayer(p);
            }

            if (killerTasks.containsKey(w) || doubleJumpTasks.containsKey(w))
            {
                cancelConstantTasks(w);
            }
            Runnable r = getKillerRunnable(w);
            killerTasks.put(w, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), r, 0, KILLER_DELAY));
        }


        @Override
        String getSpectatorJoinMessage(Player player, World worldJoined)
        {
            return player.getDisplayName() + ChatColor.GRAY + " has started spectating!";
        }

        @Override
        boolean canPlayJoinLeaveMessages(World world)
        {
            return super.canPlayJoinLeaveMessages(world) && !gameHasEnded(world);
        }

        @Override
        public void handleLeave(Player leaver, World worldFrom, boolean goingToNullWorld, boolean fromSpectation)
        {
            super.handleLeave(leaver, worldFrom, goingToNullWorld, fromSpectation);
            addScoreToTotalPoints(leaver);
            if (goingToNullWorld)
            {
                SmashEntityTracker.forgetSpeed(leaver);
            }

            SmashStatTracker.forgetOldElo(leaver.getName());

            SmashWorldInteractor.forgetWhoIHate(leaver);
            SmashWorldInteractor.forgetReadyState(leaver, worldFrom);
            if (!fromSpectation)
            {
                setSpectatorMode(leaver, false);
                if (FTPAccessor.isWorking())
                {
                    FTPAccessor.saveProfile(leaver);
                }
                else if (hasSpecialPermissions(leaver))
                {
                    leaver.sendMessage(ChatColor.RED + "Your profile will not immediately be updated on the website.");
                }
            }
//                resetPlayer(p);
            if (isDeathMatchWorld(worldFrom) && gameIsInProgress(worldFrom))
            {
                performDeathMatchDeath(leaver, worldFrom, !fromSpectation);
            }
            SmashManager.forgetLastHitter(leaver);

            //SmashKitManager.deselectKit(p);
            if (getNonspectators(worldFrom).size() <= 1 && goingToNullWorld || getNonspectators(worldFrom).size() == 0 && !goingToNullWorld)
            {
                removeGame(worldFrom, false, false, false);
            }
            else if (gameIsInProgress(worldFrom))
            {
                recordKOStatistics(leaver, worldFrom);
            }
        }

        @Override
        String getNormalLeaveMessage(Player leaver, World w)
        {
            return ChatColor.RED + leaver.getName() + " quit!";
        }

        @Override
        List<String> getTypeSpecificLoreForJoinItem(World w, Player p)
        {
            Integer tourneyLevel = getTourneyLevel(w);
            ArrayList<String> lores = new ArrayList<String>();
            lores.add(ChatColor.GREEN + "Map: " + getMapName(w));
            determineDeathMatchPreference(w);
            String gameType = isDeathMatchWorld(w) ? ChatColor.RED + "Deathmatch" : (ChatColor.GREEN + "Point-based");

            if (gameIsInProgress(w))
            {
                if (!isDeathMatchWorld(w))
                {
                    lores.add(ChatColor.RED + "" + ChatColor.ITALIC + "Time til end: " + getTimeRemaining(getTimeRemaining(w)));
                }
                lores.add("Type: " + gameType);
            }
            else if (getNonspectators(w).size() > 0)
            {
                lores.add(ChatColor.YELLOW + "Tentative type: " + gameType);
            }

            if (tourneyLevel != null)
            {
                lores.add(ChatColor.GRAY + "" + ChatColor.ITALIC + "Round " + tourneyLevel);
            }
            return lores;
        }

        @Override
        protected WorldStatus getWorldStatus(World w, CommandSender p)
        {
            if (p instanceof Player && isTourneyWorld(w))
            {
                int tourneyLevel = getTourneyLevel(w);
                if (!SmashStatTracker.canJoinTourneys(p.getName()))
                {
                    return new WorldStatus(ChatColor.RED + "Locked", WorldStatus.JoinableStatus.NOT_JOINABLE);
                }
                else if (tourneyLevel != SmashStatTracker.getTourneyLevel((Player)p))
                {
                    return new WorldStatus(ChatColor.RED + "Level " + tourneyLevel, WorldStatus.JoinableStatus.NOT_JOINABLE);
                }
            }

            if (gameHasStarted(w))
            {
                return new WorldStatus(ChatColor.RED + "In progress", WorldStatus.JoinableStatus.IN_PROGRESS);
            }
            else if (gameHasEnded(w))
            {
                return new WorldStatus(ChatColor.GOLD + "Game over", WorldStatus.JoinableStatus.JOINABLE);
            }
            else if (willStartSoon(w))
            {
                return new WorldStatus(ChatColor.GREEN + "Starting soon", WorldStatus.JoinableStatus.JOINABLE);
            }
            return new WorldStatus(ChatColor.BLUE + "Joinable", WorldStatus.JoinableStatus.JOINABLE);
        }

        /**
         * Function: determineDeathMatchPreference
         * Purpose: Calculated whether a world is a deathmatch world or not, and if so how many lives
         * @param w - The world to be situated for its deathmatch preference
         */
        protected void determineDeathMatchPreference(World w)
        {
            if (getNonspectators(w).size() == 0)
            {
                deathMatchWorlds.put(w, 0);
            }
            else if (!gameHasStarted(w))
            {
                int deathMatchPreferers = 0;
                int livesSum = 0;
                for (Player p : getNonspectators(w))
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
                int playerCount = getNonspectators(w).size();
                float percentage = 1.0F*deathMatchPreferers/playerCount;
                boolean deathMatch = percentage > 0.5 || percentage == 0.5 && getNonspectators(w).size() > 2; //This can be >= if you're okay with 50% counting as majority

                if (deathMatch)
                {
                    deathMatchWorlds.put(w, (int)Math.round(1.0*livesSum/ getNonspectators(w).size()));
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
        public boolean isDeathMatchWorld(World w)
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
        protected int getDeathMatchLives(World w)
        {
            if (isDeathMatchWorld(w))
            {
                return deathMatchWorlds.get(w);
            }
            return 0;
        }

        /**
         * Function: recordKOStatistics
         * Purpose: To record the kill, death, elo change, etc resulting from a KO in a smash world
         * @param deadPlayer - The player that has unfortunately died... I mean been KOed
         */
        public void recordKOStatistics(final Player deadPlayer, World w)
        {
            if (SmashWorldManager.isSmashWorld(w) && gameHasStarted(w) && !gameHasEnded(w))
            {
                if (SmashManager.hasLastHitter(deadPlayer.getName()))
                {
                    String participant = SmashManager.getLastHitter(deadPlayer.getName()).killerName;
                    FileManager.incrementStatistic(getGeneralPlayerFile(participant), SmashStatTracker.KO_DEALT_SCORE);
                    FileManager.incrementStatistic(getGeneralPlayerFile(deadPlayer), SmashStatTracker.KO_RECEIVED_SCORE);
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
                    FileManager.incrementStatistic(getGeneralPlayerFile(deadPlayer), SmashStatTracker.FALLEN_OUT_SCORE);
                }
            }
        }

        public void performDeathMatchDeath(final Player p, final World w, final boolean quitter)
        {
            if (gameIsInProgress(w) && hasScoreboard(w) && isOnWorldScoreboard(p))
            {
                SmashStatTracker.performEloChange(p.getName(), w, getDeathMatchLives(w));
                getWorldScoreboard(w).removeSideEntry(getPlayerEntryName(p), false);
                if (!quitter)
                {
                    handleWorldTransfer(p, p.getWorld(), null, true);
                    sendMessageToWorld(w, ChatColor.GOLD + p.getName() + ChatColor.RED + "" + ChatColor.BOLD + " DEFEATED");
                    if (isTourneyWorld(w))
                    {
                        incrementTourneyRoundsPlayed(p);
                        if (getTourneyLevel(w) == SmashStatTracker.getHighestTourneyLevelPossible())
                        {
                            SmashStatTracker.incrementTourneyGamesPlayed(p);
                        }
                    }
                    else
                    {
                        incrementFreeGamesPlayed(p);
                    }
                }
                int remainingPlayers = 0;
                for (Player player : w.getPlayers())
                {
                    if (isOnWorldScoreboard(player) && getPlayerScoreValue(player) > 0)
                    {
                        remainingPlayers++;
                    }
                }
                final int rf = remainingPlayers;
                Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
                {
                    public String call()
                    {
                        if (rf <= 1 || isTourneyWorld(w) && rf <= 1.0*TOURNEY_TOP_PERCENT/100* getOriginalPlayerCount(w))
                        {
                            endGame(w);
                        }
                        else if (!quitter)
                        {
                            setSpectatorMode(p, true);
                        }
                        return "";
                    }
                });
            }
        }

        private String getMapName(World w)
        {
            if (w != null)
            {
                return getMapName(w.getName());
            }
            sendErrorMessage("Error! Could not find a world and therefore could not get its map name!");
            return "unnamed";
        }

        private String getMapName(String worldFileName)
        {
            return getMapName(FileManager.getWorldFile(worldFileName, "", "mapName.txt"));
        }

        private String getMapName(File f)
        {
            return FileManager.readFileContents(f);
        }

        @Override
        boolean performCreateWorldCommand(CommandSender sender, String[] argsAfterType)
        {
            int argIndex = 0;
            if (argIndex >= argsAfterType.length)
            {
                return false;
            }

            final Integer tourneyLevel;
            if (argIndex < argsAfterType.length)
            {
                try
                {
                    tourneyLevel = Integer.valueOf(argsAfterType[argIndex++]);
                    if (tourneyLevel < LOWEST_TOURNEY_LEVEL || tourneyLevel > HIGHEST_TOURNEY_LEVEL)
                    {
                        sender.sendMessage(ChatColor.RED + "That tournament level is invalid! Your options are " + LOWEST_TOURNEY_LEVEL + "-" + HIGHEST_TOURNEY_LEVEL + ".");
                        return true;
                    }
                }
                catch (NumberFormatException ex)
                {
                    return false;
                }
            }
            else
            {
                tourneyLevel = null;
            }

            final String map;
            if (argIndex < argsAfterType.length)
            {
                for (File f  : this.getSourceWorlds())
                {
                    getMapName(f);
                }
            }

            boolean joinOnCreate = false;
            if (argIndex < argsAfterType.length)
            {
                String joinInput = argsAfterType[argIndex++];
                joinOnCreate = isTrue(joinInput) && sender instanceof Player;
            }

            createWorld(sender, joinOnCreate, tourneyLevel);
            return true;
        }

        private World createWorld(CommandSender sender, boolean joinOnCreate, Integer tourneyLevel)
        {
            if (tourneyLevel == null)
            {
                return createWorld(sender, joinOnCreate);
            }

            World world = createWorld(sender, false);
            setTourneyLevel(world, tourneyLevel);
            if (joinOnCreate && sender instanceof Player)
            {
                sendPlayerToWorld((Player)sender, world, false, false);
            }
            return world;
        }

        @Override
        public boolean handleCommand(Player sender, final String label, String[] args)
        {
            if (super.handleCommand(sender, label, args))
            {
                return true;
            }
            if (matchesCommand(label, SPECTATE_COMMAND))
            {
                if (isInSpectatorMode(sender) && !canBeJoinedInRightNow(sender.getWorld(), sender))
                {
                    sender.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "You cannot join this game.");
                }
                else
                {
                    setSpectatorMode(sender, !isInSpectatorMode(sender));
                }
                return true;
            }
            else if (matchesCommand(label, LIST_MAP_CMD))
            {
                File[] templates = getSourceWorlds();

                if (templates == null || templates.length == 0)
                {
                    sender.sendMessage(ChatColor.RED + "There aren't any maps. Looks like you'll need to add some.");
                    return true;
                }

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

                return true;
            }
            else if (matchesCommand(label, GET_MODE_CMD))
            {
                if (args.length > 0 && args[0].equalsIgnoreCase("help"))
                {
                    return false;
                }
                World w;
                String yourThat;
                if (args.length > 0)
                {
                    args[0] = WorldManager.smashifyArgument(args[0]);
                    w = Bukkit.getWorld(args[0]);
                    if (w == null)
                    {
                        sender.sendMessage(ChatColor.RED + "World not found!");
                        return true;
                    }
                    else if (!SmashWorldManager.isSmashWorld(w))
                    {
                        sender.sendMessage(ChatColor.RED + "That is not a Smash world.");
                        return true;
                    }
                    yourThat = "That";
                }
                else
                {
                    w = sender.getWorld();
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
            else if (matchesCommand(label, START_CMD))
            {
                World w;

                if (args.length > 1)
                {
                    if (!getDrainWorldPrefix().toLowerCase().startsWith(args[1].toLowerCase()))
                    {
                        args[1] = getDrainWorldPrefix();
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
                else
                {
                    w = sender.getWorld();
                }

                if (gameHasStarted(w))
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
                        if (w.equals(sender.getWorld()))
                        {
                            sender.sendMessage(ChatColor.RED + "Game has already started!");
                        }
                        else
                        {
                            sender.sendMessage(ChatColor.RED + "Game has already started in " + getDisplayName(w) + ".");
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
                                setStartTime(w, timeTilStart);
                            }
                        }
                    }
                }
                return true;
            }
            else if (matchesCommand(label, RESET_CMD))
            {
                final World w;
                if (args.length == 0)
                {
                    w = sender.getWorld();
                }
                else
                {
                    args[0] = WorldManager.smashifyArgument(args[0]);
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

                if (args.length > 0)
                {
                    Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable() {
                        public String call()
                        {
                            restartSmashWorld(w);
                            return "";
                        }
                    });
                    sender.sendMessage(ChatColor.YELLOW + "Game in " + getDisplayName(w) + ChatColor.YELLOW + " reset!");
                }
                else if (gameHasStarted(w) && !gameHasEnded(w))
                {
                    final Player finalP = sender;
                    new Verifier.BooleanVerifier(sender, ChatColor.YELLOW + (sender.getWorld() == w ? "Do you really want to restart the world you're in right now!?" :
                                                                                                                    "That game is in progress. Are you sure you want to reset it?"))
                    {
                        @Override
                        public void performYesAction()
                        {
                            Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
                            {
                                public String call()
                                {
                                    restartSmashWorld(w);
                                    return "";
                                }
                            });
                            if (finalP.getWorld() != w)
                            {
                                finalP.sendMessage(ChatColor.YELLOW + "Game in " + getDisplayName(w) + ChatColor.YELLOW + " reset!");
                            }
                        }

                        @Override
                        public void performNoAction()
                        {
                            finalP.sendMessage(ChatColor.YELLOW + "Command /" + label + " cancelled");
                        }
                    };
                }
                else
                {
                    Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable() {
                        public String call()
                        {
                            restartSmashWorld(w);
                            return "";
                        }
                    });
                    sender.sendMessage(ChatColor.YELLOW + "Game in " + getDisplayName(w) + ChatColor.YELLOW + " reset.");
                }
                return true;
            }
            else if (matchesCommand(label, READY_UP_CMD))
            {
                World whereToGo = null;
                World w = sender.getWorld();
                if (!gameHasStarted(w))
                {
                    boolean changedReadyState = setReady(sender, w, true);
                    int whoIsReady = getNumberOfReadyPlayers(w);
                    if (whoIsReady >= FREE_PLAYER_REQUIREMENT && (float)whoIsReady/w.getPlayers().size() >= READY_PERCENT_TO_START)
                    {
                        startGame(w);
                    }
                    else if (changedReadyState)
                    {
                        sender.sendMessage(ChatColor.GREEN + "You are now ready!");
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.GREEN + "You were already ready, cap'n!");
                    }
                }

                return true;
            }
            else if (matchesCommand(label, VOTE_KICK_CMD))
            {
                if (args.length == 0 || args[0].equalsIgnoreCase("help"))
                {
                    return false;
                }
                else
                {
                    World w = sender.getWorld();
                    Player whoYouDontLike = Bukkit.getPlayer(args[0]);
                    if (whoYouDontLike == null || !whoYouDontLike.getWorld().equals(w) || isInSpectatorMode(whoYouDontLike))
                    {
                        sender.sendMessage(ChatColor.RED + "Player not found!");
                    }
                    else
                    {
                        SmashWorldInteractor.addPlayerWhoIHate(sender, whoYouDontLike);

                        if (SmashWorldInteractor.isHated(sender, w))
                        {
                            WorldManager.sendPlayerToLastWorld(whoYouDontLike, false);
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
            else if (matchesCommand(label, END_GAME_CMD))
            {
                World w;
                String thisThat;
                if (args.length > 0)
                {
                    args[0] = WorldManager.smashifyArgument(args[0]);
                    w = Bukkit.getWorld(args[0]);
                    thisThat = "That";
                }
                else
                {
                    w = sender.getWorld();
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
                else if (!gameHasStarted(w) || gameHasEnded(w))
                {
                    sender.sendMessage(ChatColor.RED + thisThat + " game isn't in progress!");
                }
                else
                {
                    endGame(w, sender.getName());
                }
                return true;
            }
            return false;
        }

        @Override
        public World getBestWorld(Player p)
        {
            World bestWorld = null;
            World oldWorld = p.getWorld();

            String oldMapName;
            if (!SmashWorldManager.isSmashWorld(oldWorld))
            {
                oldMapName = null;
            }
            else
            {
                oldMapName = getMapName(oldWorld);
            }

            boolean prefersTourneys = getAndForgetIfPrefersTourneys(p);

            int bestDesirability = -1;
            for (Integer number : takenWorldNumbers())
            {
                World candidateWorld = getWorld(number);

                if (canBeJoined(candidateWorld, p) && isTourneyWorld(candidateWorld) == prefersTourneys)
                {
                    int candidateDesirability = 0;
                    if (oldMapName != null && !getMapName(candidateWorld).equals(oldMapName))
                    {
                        candidateDesirability++;
                    }
                    candidateDesirability += candidateWorld.getPlayers().size();
                    if (candidateWorld != oldWorld)
                    {
                        candidateDesirability++;
                    }

                    if (candidateDesirability > bestDesirability)
                    {
                        bestWorld = candidateWorld;
                        bestDesirability = candidateDesirability;
                    }
                }
            }
            if (bestWorld == null)
            {
                bestWorld = createWorld(p, true);
                setTourneyLevel(bestWorld, SmashStatTracker.getTourneyLevel(p));
            }
            return bestWorld;
        }

        private boolean isTourneyWorld(World world)
        {
            return getTourneyLevel(world) != null;
        }
    },
    META(0, 1, true, "MetaWorldMetaWorldGenerator", false, true, false, false, true, true, GameMode.SURVIVAL, 10, 3, 10)
    {
        private final Random r = new Random();
        private static final int MAXIMUM_BOOST = 3;
        private final HashMap<World, Integer> superBoosts = new HashMap<World, Integer>();
        private static final float BASE_EMERALD_BOOST_FACTOR = 10f;
        private static final int BOOST_ON_WEEKENDS = 2;
        private static final float BASE_SPECIAL_ITEM_DROP_CHANCE_PER_BLOCK = 0.02f;
        private static final String SMASH_KIT_SHOP_GUI_NAME = "Smash Kit Shop";
        private final ArrayList<Player> shopDoorDwellers = new ArrayList<Player>();
        private boolean initialized = false;
        private static final int MOTD_INTERVAL_SECONDS = 300;
        private static final int BLOCKS_TO_GET_A_SINGLE_BREAD = 45;

        @Override
        protected WorldStatus getWorldStatus(World w, CommandSender p)
        {
            return new WorldStatus(ChatColor.GOLD + "" + ChatColor.BOLD + "META", WorldStatus.JoinableStatus.JOINABLE);
        }

        @Override
        public void performUnloadWorldAction(World w)
        {
            super.performUnloadWorldAction(w);
            ForcefieldManager.removeForcefield(w);
        }

        @Override
        public void giveStarterItems(Player p)
        {
            super.giveStarterItems(p);

            InventoryManager.giveItem(p, new ItemStack(Material.STONE_SWORD, 1), true, false);
            Location whereDied = getLastDeathLocation(p, p.getWorld());
            final short howManyBreadToGive;
            if (whereDied == null)
            {
                howManyBreadToGive = 20;
            }
            else
            {
                short breadByDistance = (short)(whereDied.distance(p.getLocation())/BLOCKS_TO_GET_A_SINGLE_BREAD);
                howManyBreadToGive = breadByDistance > 64 ? (short)64 : breadByDistance;
            }
            InventoryManager.giveItem(p, MetaItems.BREAD_THAT_CANT_BE_SOLD.getSpecialItem().getItemStack(), howManyBreadToGive, true, false);

            WorldType.WORLD_GUI.give(p);
        }

        @Override
        public String getDisplayPrefix(World world)
        {
            return ChatColor.BLUE + "The Meta World";
        }

        @Override
        public Iterator<SpecialItem> getSpecialItemIterator(LivingEntity player)
        {
            return new Iterator<SpecialItem>()
            {
                private int index = 0;
                private final MetaItems[] itemList = MetaItems.values();

                @Override
                public boolean hasNext()
                {
                    return index < itemList.length;
                }

                @Override
                public SpecialItem next()
                {
                    return itemList[index++].getSpecialItem();
                }

                @Override
                public void remove()
                {
                    sendErrorMessage("Don't remove stuff from this");
                }
            };
        }

        @Override
        protected Material getJoinTheWorldMaterial(World w)
        {
            return Material.IRON_SWORD;
        }

        @Override
        public WorldCreatorItem[] generateWorldCreatorItems()
        {
            ItemStack creationItem = new ItemStack(Material.IRON_INGOT);
            ItemMeta meta = creationItem.getItemMeta();
            meta.setDisplayName(getDisplayPrefix(null));
            creationItem.setItemMeta(meta);
            return new WorldCreatorItem[]
            {
                new WorldCreatorItem(this, creationItem)
            };
        }

        @Override
        public void performLoadWorldAction(final World world, boolean forCreation)
        {
            super.performLoadWorldAction(world, forCreation);
            new ForcefieldManager.CenterSquareForcefield(world, 1f, ForcefieldManager.Forcefield.DEFAULT_TICK_INTERVAL*3, MetaWorldGenerator.HALF_WORLD_LENGTH, 10000)
            {
                @Override
                public String getWarningMessage(Player p)
                {
                    return ChatColor.RED + "You have reached the end of the world. Please turn back now.";
                }
            };

            if (!initialized)
            {
                initialized = true;
                final float ticksInDayLight = 864000;
                final double origDayTicksPerTick = 24000d/(ticksInDayLight*2);
                //private final float origNightTicksPerTick = 24000/(ticksInDayLight*2);
                final long currentTime = System.currentTimeMillis();
                //final float timeOfYear = (float)((currentTime + 864000000) % 31557600000L)/31557600000L;
                //final float darkness = ((timeOfYear < 0.5f ? 1f - timeOfYear : timeOfYear) - 0.5f)*2f;

                Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
                {
                    private double minecraftWorldTime = ((double)((currentTime - /*convert to edt*/14400000 - /*adjust for sunrise being at 7am, not 12am*/25200000) % 86400000)/86400000)*24000;

                    public void run()
                    {
                        minecraftWorldTime += origDayTicksPerTick;//minecraftWorldTime < 12000 ? origDayTicksPerTick : origNightTicksPerTick;

                        for (final World w : getWorlds())
                        {
                            w.setFullTime((long)minecraftWorldTime);
                        }

                        if (minecraftWorldTime % 500 == 0)
                        {
                            minecraftWorldTime = ((double)((System.currentTimeMillis() - /*convert to edt*/14400000 - /*adjust for sunrise being at 7am, not 12am*/25200000) % 86400000)/86400000)*24000;
                        }
                    }
                }, 0, 1);

                Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
                {
                    int i = 0;
                    public void run()
                    {
                        final String message;
                        switch (i)
                        {
                            case 0:
                                if (isWeekend())
                                {
                                    message = ChatColor.GOLD + "A WEEKEND BOOST is currently active! Emeralds drop " + BOOST_ON_WEEKENDS + "x as much by default" + (BOOST_ON_WEEKENDS != getSuperBoost(world) ? " (" + getSuperBoost(world) + "x right now)" : "") + "!";
                                }
                                else
                                {
                                    message = ChatColor.GOLD + "On weekends, Emeralds' base drop rate is " + BOOST_ON_WEEKENDS + "x the norm.";
                                }
                                break;
                            default:
                                message = ChatColor.BLUE + "Want to hang out on discord? Here's the invite! http://discordapp.com/invite/UcXMmZK";
                                i = -1;
                                break;
                        }
                        broadcastMotdMessage(message);
                        i++;
                    }
                }, MOTD_INTERVAL_SECONDS*20/2, MOTD_INTERVAL_SECONDS*20);
            }

            if (forCreation)
            {
                world.setGameRuleValue("keepInventory", "false");
                world.setGameRuleValue("doMobSpawning", "true");
                world.setDifficulty(Difficulty.HARD);
                Chunk chunk = world.getChunkAt(0, 0);
                int niceY = 0xFF;
                for (; niceY > 0; niceY--)
                {
                    Material mat = chunk.getBlock(0, niceY, 0).getType();
                    if (mat.isSolid() && !mat.equals(Material.LEAVES))
                    {
                        break;
                    }
                }
                world.setSpawnLocation(0, niceY + 1, 0);
            }

            EconomyCurrencyManager.startWorld(world);
        }

        @Override
        void performPlayerInteract(PlayerInteractEvent event)
        {
            super.performPlayerInteract(event);
            if (event.isCancelled()) return;

            if (event.getAction().equals(Action.LEFT_CLICK_BLOCK))
            {
                final Block b = event.getClickedBlock();

                if (b.getType().equals(Material.LEAVES) && !blockIsProtected(b, event.getPlayer(), false))
                {
                    final Player p = event.getPlayer();
                    Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
                    {
                        public String call()
                        {
                            for (ItemStack itemDrop : b.getDrops())
                            {
                                b.getWorld().dropItemNaturally(b.getLocation(), itemDrop);
                            }
                            if (!p.getGameMode().equals(GameMode.CREATIVE))
                            {
                                p.playSound(p.getLocation(), Sound.BLOCK_GRASS_BREAK, 1, 1);
                            }
                            b.setType(Material.AIR);
                            return "";
                        }
                    });
                }
            }
        }

        @Override
        public void performBlockBreak(BlockBreakEvent event)
        {
            super.performBlockBreak(event);
            if (event.isCancelled()) return;

            SpecialItem specialItem = getHeldSpecialItem(event.getPlayer());
            if (specialItem != null)
            {
                specialItem.performBlockBreak(event.getPlayer(), event.getBlock());
            }

            if (event.getBlock().getType().equals(Material.EMERALD_ORE) && event.getPlayer().getGameMode() != GameMode.CREATIVE)
            {
                boolean isSilk = false;
                Collection<ItemStack> drops = event.getBlock().getDrops();
                for (ItemStack item : drops)
                {
                    if (item.getType().equals(Material.EMERALD_ORE))
                    {
                        isSilk = true;
                        break;
                    }
                }
                if (!isSilk)
                {
                    World world = event.getBlock().getWorld();
                    int normalAmount = drops.size();
                    float randomFloat = r.nextFloat();
                    float randomizationFactor = randomFloat*2;
                    int superBoost = getSuperBoost(world);
                    short newEmeraldCount = (short)Math.round(BASE_EMERALD_BOOST_FACTOR*superBoost*randomizationFactor*normalAmount);
                    if (randomFloat < BASE_SPECIAL_ITEM_DROP_CHANCE_PER_BLOCK*superBoost)
                    {
                        MetaItems item = getRandomSpecialItem();
                        world.dropItemNaturally(event.getBlock().getLocation(), item.getSpecialItem().getItemStack());
                        InteractiveChat.MessagePart[] message = new InteractiveChat.MessagePart[]
                        {
                            new InteractiveChat.MessagePart(ChatColor.GOLD + "" + ChatColor.BOLD + event.getPlayer().getName().toUpperCase() + ChatColor.DARK_RED + "" + ChatColor.BOLD + " JUST MINED A "),
                            new InteractiveChat.MessagePart( ChatColor.DARK_RED + "" + ChatColor.BOLD + "RARE ITEM", ChatColor.GREEN + "Rarity: " + (1f/item.getSpawnChance()), null),
                            new InteractiveChat.MessagePart(ChatColor.DARK_RED + "" + ChatColor.BOLD + "!")
                        };
                        for (Player p : world.getPlayers())
                        {
                            InteractiveChat.sendMessage(p, message);
                        }
                    }
                    else
                    {
                        for (int i = 0; i < newEmeraldCount; i++)
                        {
                            world.dropItemNaturally(event.getBlock().getLocation(), new ItemStack(Material.EMERALD));
                        }
                    }

                    event.setCancelled(true);
                    event.getBlock().setType(Material.AIR);
//                        addEmeralds(event.getAttacker(), newEmeraldCount, true);
                }
            }
        }

        private boolean isWeekend()
        {
            int dayNumber = (int)(System.currentTimeMillis()/86400000L) % 7;
            switch (dayNumber)
            {
                case 2: //Saturday
                case 3: //Sunday
                    return true;
                default:
                    return false;
            }
        }

        private int getSuperBoost(World world)
        {
            Integer boost = superBoosts.get(world);
            if (boost == null)
            {
                boost = 1;
            }
            if (isWeekend())
            {
                boost += BOOST_ON_WEEKENDS - 1;
            }
            return boost;
        }

        private boolean superBoostIsActive(World world)
        {
            return getSuperBoost(world) != 1;
        }

        private void subtractSuperBoost(World world, int boostLevelsToSubtract)
        {
            int newBoost = getSuperBoost(world) - boostLevelsToSubtract;
            if (newBoost <= 1)
            {
                if (newBoost != 1)
                {
                    sendErrorMessage("Error! Overcorrected super boost!");
                }
                superBoosts.remove(world);
            }
            else
            {
                superBoosts.put(world, newBoost);
            }
        }

        private boolean canBeBoosted(World world, int amount)
        {
            return getSuperBoost(world) + amount <= MAXIMUM_BOOST;
        }

        private void addSuperBoost(final World world, final int boostLevelsToAdd, int minutesDuration)
        {
            if (boostLevelsToAdd < 0)
            {
                sendErrorMessage("Error! Must have positive boost level!");
                return;
            }
            int currentBoost = getSuperBoost(world);
            if (currentBoost >= MAXIMUM_BOOST)
            {
                return;
            }
            int newBoost = currentBoost + boostLevelsToAdd;
            newBoost = newBoost > MAXIMUM_BOOST ? MAXIMUM_BOOST : newBoost;

            superBoosts.put(world, newBoost);
            for (Player p : world.getPlayers())
            {
                p.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "EMERALD ORES NOW YIELD " + ChatColor.DARK_RED + "" + newBoost + "x" + ChatColor.GREEN + " as much! This will last for " + minutesDuration + " minutes!");
            }
            Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable()
            {
                @Override
                public void run()
                {
                    subtractSuperBoost(world, boostLevelsToAdd);
                }
            }, minutesDuration*1200);
        }

        @Override
        protected void handleLeave(Player leaver, World w, boolean goingToNullWorld, boolean fromSpectation)
        {
            super.handleLeave(leaver, w, goingToNullWorld, fromSpectation);
            if (shopDoorDwellers.contains(leaver))
            {
                shopDoorDwellers.remove(leaver);
            }
        }

        @Override
        public void performPlayerMove(PlayerMoveEvent event)
        {
            super.performPlayerMove(event);
            if (event.isCancelled()) return;

            Player p = event.getPlayer();
            switch (MetaWorldGenerator.getShopEntranceState(event.getFrom(), event.getTo()))
            {
                case ENTERING:
                    if (!shopDoorDwellers.contains(p))
                    {
                        shopDoorDwellers.add(p);
                        sendMessageFromKitVendor(event.getPlayer(), "Welcome to my humble abode, " + event.getPlayer().getName() + "! I hope you find what you're looking for in my stock!");
                    }
                    break;
                case EXITING:
                    if (shopDoorDwellers.contains(p))
                    {
                        shopDoorDwellers.remove(p);
                        sendMessageFromKitVendor(event.getPlayer(), "Leaving so soon? Well, come again!");
                        if (Verifier.isVerifier(p))
                        {
                            Verifier.forcablyReleaseVerifier(p);
                        }
                    }
                    break;
            }
        }

        @Override
        protected void performPlayerInteractEntity(PlayerInteractEntityEvent event)
        {
            super.performPlayerInteractEntity(event);
            if (event.isCancelled()) return;

            if (event.getRightClicked().getType().equals(EntityType.VILLAGER) && event.getRightClicked().getCustomName() != null)
            {
                String name = event.getRightClicked().getCustomName();
                if (name.endsWith(MetaWorldGenerator.KIT_SELLER_HERMIT_NAME))
                {
                    final Player p = event.getPlayer();
                    if (openKitPurchaseGui(p))
                    {
                        event.setCancelled(true);
                    }
                }
            }
        }

        private void sendMessageFromKitVendor(Player p, String message)
        {
            sendMessageToPlayer(p, MetaWorldGenerator.KIT_SELLER_HERMIT_NAME, message);
        }

        private boolean openKitPurchaseGui(final Player p)
        {
            GuiManager.GuiInventory inv = new GuiManager.GuiInventory(SMASH_KIT_SHOP_GUI_NAME)
            {
                @Override
                public String getFailureMessage(final Player p)
                {
                    sendMessageFromKitVendor(p, "That's all the " + OWNED_KITS_DATANAME + " I have to sell!");
                    Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            sendMessageFromKitVendor(p, "If you want more you'll have talk to my supplier HappyMan as he has trapped me here.");
                        }
                    }, 20);
                    return null;
                }
            };

           // SmashKitManager.getKit(
            for (final SmashKitManager.SmashKit kit : SmashKitManager.getUnavaliableKits(p))
            {
                final short cost = (short)kit.getCost();
                inv.addItem(new GuiManager.GuiItem(kit.getKitRepresenter(), ChatColor.GREEN + "" + ChatColor.BOLD + kit.getDisplayName(), new String[] {"" + cost + " " + EconomyCurrencyManager.EMERALD_COUNT_DATANAME})
                {
                    @Override
                    public void performAction(final Player clicker)
                    {
                        final short emeralds = EconomyCurrencyManager.getEmeralds(clicker);
                        if (emeralds >= cost - 1)
                        {
                            if ((int)emeralds == cost - 1)
                            {
                                sendMessageFromKitVendor(clicker,"You're just 1 " + EconomyCurrencyManager.EMERALD_COUNT_DATANAME.substring(0, EconomyCurrencyManager.EMERALD_COUNT_DATANAME.length() - 1) + " short of affording that... good thing I don't actually care!");
                            }
                            sendMessageFromKitVendor(clicker, "Are you sure you want to buy " + kit.getDisplayName() + "? I know you kids tend to misclick these days...");
                            new Verifier.BooleanVerifier(p, null)
                            {
                                @Override
                                public void performYesAction()
                                {
                                    sendMessageFromKitVendor(clicker,"You're now the proud owner of " + kit.getDisplayName() + "! It was a pleasure doing business with you! Have a nice day.");
                                    SmashKitManager.givePlayerKit(clicker, kit);
                                    EconomyCurrencyManager.removeEmeralds(clicker, cost);
                                    removeFromGui();
                                }

                                @Override
                                public void performNoAction()
                                {
                                    sendMessageFromKitVendor(p, "Well that's okay. See if there's anything else you'd like.");
                                }
                            };

                            //clicker.closeInventory();
                        }
                        else
                        {
                            sendMessageFromKitVendor(p, "You'll need to gather " + (int)(cost - emeralds) + " more " + EconomyCurrencyManager.EMERALD_COUNT_DATANAME + " before we can make a deal on " + kit.getDisplayName() + ".");
                        }
                    }
                }, false);
            }
            return inv.open(p);
        }

        @Override
        public boolean handleCommand(final Player sender, String label, String[] args)
        {
            if (super.handleCommand(sender, label, args))
            {
                return true;
            }
            if (matchesCommand(label, SPAWN_CMD))
            {
                sender.teleport(sender.getWorld().getSpawnLocation());
                return true;
            }
            else if (matchesCommand(label, SPECTATE_COMMAND))
            {
                if (hasSpecialPermissions(sender))
                {
                    setSpectatorMode(sender, !isInSpectatorMode(sender));
                }
                else
                {
                    sender.sendMessage(ChatColor.RED + "You are not allowed to use this command in this world.");
                }
                return true;
            }
            return false;
        }

        @Override
        void handleJoin(Player joiner, boolean cameFromNullWorld, World worldTo, boolean fromSpectation) {
            super.handleJoin(joiner, cameFromNullWorld, worldTo, fromSpectation);
            Song.playIntroSong(joiner);
        }
    },
    INVALID(-1, -1, false, null, true, false, false, false, true, false, GameMode.CREATIVE, null, 0, 0)
    {
        @Override
        public String getDisplayPrefix(World world)
        {
            return "Invalid World";
        }

        @Override
        protected Material getJoinTheWorldMaterial(World w)
        {
            return Material.BEDROCK;
        }

        @Override
        protected WorldStatus getWorldStatus(World w, CommandSender p)
        {
            return null;
        }

        @Override
        public World getBestWorld(Player p)
        {
            return null;
        }

        @Override
        public WorldCreatorItem[] generateWorldCreatorItems()
        {
            return null;
        }
    };

    //@TODO:bring back
    static //handle land events
    {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
        {
            private static final int REFUEL_COOLDOWN = 12; //The delay in ticks before your items can recharge again upon landing
            private final HashMap<Player, Integer> ticksTilNextRefuel = new HashMap<Player, Integer>();

            public void run()
            {
                for (Player p : Bukkit.getOnlinePlayers())
                {
                    int ticksLeft = getTicksTilNextRefuel(p);
                    ticksLeft = ticksLeft == 0 ? ticksLeft : ticksLeft - 1;
                    boolean onGround = isTouchingTheGround(p);
                    if (onGround)
                    {
                        if (ticksLeft == 0)
                        {
                            SpecialItem heldItem = getWorldType(p.getWorld()).getSpecialItem(p, p.getItemInHand());
                            ItemStack[] contents = p.getInventory().getContents();
                            for (int i = 0; i < 36; i++)
                            {
                                ItemStack itemStack = contents[i];
                                SpecialItem item = getWorldType(p.getWorld()).getSpecialItem(p, itemStack);
                                if (item != null)
                                {
                                    item.performLandAction(p);
                                    if (item == heldItem)
                                    {
                                        item.performHeldLandAction(p);
                                    }
                                    else
                                    {
                                        item.performUnheldLandAction(p);
                                    }
                                }
                            }
                            //reset cooldown
                            ticksTilNextRefuel.put(p, REFUEL_COOLDOWN);
                        }
                    }
                    else if (ticksLeft > 0)
                    {
                        ticksTilNextRefuel.put(p, ticksLeft);
                    }
                    else
                    {
                        ticksTilNextRefuel.remove(p);
                    }
                }
            }

            private boolean isTouchingTheGround(Player p)
            {
                return ((Entity)p).isOnGround() && p.getLocation().getY()%0.5 < 0.04;
            }

            /**
             * Function: cooldownIsOver
             * Purpose: To determine if a player can have his items recharged
             * @param p - The player who we want to check for being allowed to have his items recharged
             * @return - True if the player can have his items recharged
             */
            private int getTicksTilNextRefuel(Player p)
            {
                Integer ticksLeft = ticksTilNextRefuel.get(p);
                if (ticksLeft == null)
                {
                    return 0;
                }
                else if (ticksLeft <= 0)
                {
                    ticksTilNextRefuel.remove(p);
                }
                return ticksLeft;
                //p.addPotionEffect(new PotionEffect(PotionEffectType.WEAKNESS, 1, 1));
            }
        }, 0, 1);
    }

    public static final SpecialItem WORLD_GUI = new SpecialItem(new UsefulItemStack(Material.BRICK, ChatColor.YELLOW + "" + ChatColor.BOLD + "Navigate Worlds"))
    {
        @Override
        public boolean give(Player p)
        {
            if (!InventoryManager.hasItem(p, getItemStack()))
            {
                return InventoryManager.giveItem(p, getItemStack(), true, false, 8);
            }
            return false;
        }

        @Override
        public boolean performDropAction(Player p)
        {
            super.performDropAction(p);
            if (getWorldType(p.getWorld()) == STUFFLAND)
            {
                return true;
            }
            else if (p.getItemInHand() == null || p.getItemInHand().getType() == Material.AIR)
            {
                p.sendMessage(ChatColor.YELLOW + "You just dropped the World Navigator. Do /" + NAV_COMMAND + " to get it back.");
            }
            return false;
        }

        @Override
        public void performRightClickAction(Player p, Block blockClicked)
        {
            super.performRightClickAction(p, blockClicked);
            openSmashGui(p);
        }
    };
    public static final int LOWEST_WORLD_NUM = 1;
    private static final HashMap<Player, Boolean> tourneyPreferers = new HashMap<Player, Boolean>();
    private static final ArrayList<Player> spectators = new ArrayList<Player>(); //This keeps track of who all is in spectator mode
    private static final List<WorldGui> openWorldGuis = new ArrayList<WorldGui>();
    private static final ArrayList<Player> playersWhoHaveBeenAllowedToTeleport = new ArrayList<Player>();
    private static final int PLAYER_DISTANCE_CONSIDERED_CLOSE = 50;
    private static final int PLAYER_DISTANCE_CONSIDERED_CLOSE_SQUARED = PLAYER_DISTANCE_CONSIDERED_CLOSE*PLAYER_DISTANCE_CONSIDERED_CLOSE;

    private final HashMap<Entity, AttackSource> entitySources = new HashMap<Entity, AttackSource>();
    private final ArrayList<String> worldsBeingDeleted = new ArrayList<String>();
//    private final HashMap<Player, HashMap<ItemStack, Integer>> repairTasks = new HashMap<Player, HashMap<ItemStack, Integer>>();

    private final boolean valid;
    private final int minWorldsOfType;
    private final int maxWorldsOfType;
    private final List<Integer> takenWorldNumbers = new ArrayList<Integer>();
    private final boolean fightingHunger;
    private final boolean rememberPlayerState;
    private final GameMode defaultGamemode;
    private final Integer teleportDelaySeconds;
    private final int minimumMaxWarps;
    private final int maximumMaxWarps;
    private final String generator;
    private final HashMap<String, Boolean> checkedWorlds = new HashMap<String, Boolean>(); //the boolean is if successful check for Bukkit.yml file world generator

    private final HashMap<World, UsefulScoreboard> worldScoreboards = new HashMap<World, UsefulScoreboard>();
    private final String worldScoreboardTitle;
    private final String belowNameSuffix;
    private final HashMap<World, HashMap<Player, UsefulScoreboard>> personalScoreboards = new HashMap<World, HashMap<Player, UsefulScoreboard>>();
    private final String personalScoreboardTitle;
    private final WorldCreatorItem[] worldCreatorItems;
    private final boolean preventConsolidation;
    private final boolean preventRain;
    private final boolean allowDestruction;
    private final boolean hasAnEconomy;
    private final ArrayList<World> worldsWhoCanChangeWeather = new ArrayList<World>();
    private static int timePreventedWeatherChange = 0;
    private static final ArrayList<Player> playersNotInAWorld = new ArrayList<Player>();

    WorldType(int minWorldsOfType, int maxWorldsOfType, boolean valid, String generator, boolean fightHunger,
              boolean rememberPlayerState, boolean preventConsolidation, boolean preventRain,
              boolean allowDestruction, boolean hasAnEconomy, GameMode defaultGamemode, Integer teleportDelaySeconds, int minimumMaxWarps, int maximumMaxWarps, String worldScoreboardTitle, String belowNameScoreSuffix, String personalScoreboardTitle)
    {
        this.maxWorldsOfType = maxWorldsOfType;
        this.minWorldsOfType = minWorldsOfType;

        this.generator = generator;
        this.preventConsolidation = preventConsolidation;
        this.preventRain = preventRain;
        this.allowDestruction = allowDestruction;
        this.hasAnEconomy = hasAnEconomy;
        this.defaultGamemode = defaultGamemode;
        this.teleportDelaySeconds = teleportDelaySeconds;
        this.minimumMaxWarps = minimumMaxWarps;
        this.maximumMaxWarps = maximumMaxWarps;
        this.worldScoreboardTitle = worldScoreboardTitle;
        this.personalScoreboardTitle = personalScoreboardTitle;
        this.belowNameSuffix = belowNameScoreSuffix;
        this.valid = valid;
        this.fightingHunger = fightHunger;
        this.rememberPlayerState = rememberPlayerState;

        WorldCreatorItem[] creatorItems;
        try
        {
            creatorItems = generateWorldCreatorItems();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
            sendErrorMessage("Error! Couldn't get CREATION items!");
            creatorItems = null;
            //this.fullIndicator = ChatColor.RESET + "" + chatColor + fullIndicator;
        }
        this.worldCreatorItems = creatorItems;
    }

    WorldType(int minWorldsOfType, int maxWorldsOfType, boolean valid, String generator, boolean fightHunger,
              boolean rememberPlayerState, boolean preventConsolidation, boolean preventRain, boolean allowDestruction, boolean hasAnEconomy, GameMode defaultGamemode, Integer teleportDelaySeconds, int minimumMaxWarps, int maximumMaxWarps)
    {
        this(minWorldsOfType, maxWorldsOfType, valid, generator, fightHunger, rememberPlayerState, preventConsolidation, preventRain, allowDestruction, hasAnEconomy, defaultGamemode, teleportDelaySeconds, minimumMaxWarps, maximumMaxWarps, null, null, null);
    }



    public static String getChatMessage(String name, String message)
    {
        return getChatMessagePrefix(name) + message;
    }

    private static String getChatMessagePrefix(String name)
    {
        return name == null ? "" : ChatColor.RESET + "<" + name + ChatColor.RESET + "> ";
    }

    private static String getChatMessagePrefix(Player p)
    {
        return p == null ? "-null-" : getChatMessagePrefix(p.getDisplayName());
    }

    private static String getChatMessagePrefix(String nameColor, Player p)
    {
        return p == null ? "-null-" : getChatMessagePrefix(nameColor == null || nameColor.length() == 0 ? p.getDisplayName() : (nameColor + p.getName()));
    }

    /**
     * Function:
     * Purpose:
     *
     *
     * @param p
     * @param w
     * @return
     */
    private static String getWelcomeMessage(Player p, World w)
    {
        return ChatColor.GREEN + "Entered " + getWorldType(w).getDisplayName(w) + ChatColor.GREEN + "!";
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
                Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
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
                Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
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
            if (sendToAfks || !SmashEntityTracker.isAfk(p))
            {
                p.sendMessage(message);
            }
        }
    }

    public static void sendMessageToWorld(Player whoIsSending, String whoIsSendingNamePrefix, String message)
    {
        if (!Verifier.isVerifier(whoIsSending))
        {
            sendMessageToWorld(whoIsSending.getWorld(), getChatMessagePrefix(whoIsSendingNamePrefix, whoIsSending) + message);
        }
    }

    public static void sendMessageToPlayer(Player player, String whoIsSendingName, String message)
    {
        player.sendMessage(getChatMessagePrefix(whoIsSendingName) + message);
    }

    public static void sendMessageToWorld(Player whoIsSending, String message)
    {
        sendMessageToWorld(whoIsSending, "", message);
    }

    public static void sendMessageToWorld(World w, String message)
    {
        sendMessageToWorld(w, message, true);
    }

    public static void sendMessageOutsideWorld(World w, String message)
    {
        for (Player p : Bukkit.getOnlinePlayers())
        {
//            World playerWorld = p.getWorld();
            if (!p.getWorld().equals(w)/* && !(isSmashWorld(playerWorld) && SmashWorldManager.gameHasStarted(playerWorld))*/)
            {
                p.sendMessage(message);
            }
        }
    }

    public final void broadcastMotdMessage(String message)
    {
        String motdPrefix = "[" + ChatColor.GREEN + "" + ChatColor.BOLD + getDrainWorldPrefix().toUpperCase() + ChatColor.WHITE + "] " + ChatColor.RESET + ChatColor.YELLOW;
        for (World world : getWorlds())
        {
            for (Player p : world.getPlayers())
            {
                if (!SmashEntityTracker.isAfk(p) && p.getGameMode() != GameMode.CREATIVE)
                {
                    p.sendMessage(motdPrefix + message);
                }
            }
        }
    }
//    /**
//     * Function: refreshWorldGui
//     * Purpose: To build the world guis of all players who have a certain Tourney level
//     *
//     * @param tourneyLevel - The tourney level of the players for whom you want to build the world gui
//     */
//    protected static void refreshWorldGui(int tourneyLevel)
//    {
//        if (openWorldGuis.containsKey(tourneyLevel))
//        {
//            for (Player p : WorldManager.openWorldGuis.get(tourneyLevel))
//            {
//                refreshWorldGui(p);
//            }
//        }
//    }


    /**
     * Function: getAvaliableWorlds
     * Purpose: To get the list of worlds that a player can currently join
     *
     * @param p - The player for which to get avaliable worlds
     * @return - The list of worlds that the player can join
     */
    public final List<World> getAvaliableWorlds(Player p)
    {
        ArrayList<World> avalibaleWorlds = new ArrayList<World>();
        for (WorldType type : values())
        {
            for (World world : type.getWorlds())
            {
                if (type.canBeJoined(world, p))
                {
                    avalibaleWorlds.add(world);
                }
            }
        }
        if (!avalibaleWorlds.contains(getFallbackWorld()))
        {
            avalibaleWorlds.add(0, getFallbackWorld());
        }
        return avalibaleWorlds;
    }

    private static boolean hasSpecialPermissions(Player p)
    {
        return Plugin.hasPermissionsForCommand(p, CREATE_WORLD_CMD);
    }

    public static void openSmashGui(final Player p, final boolean giveOpener)
    {
        Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
        {
            public String call()
            {
                if (giveOpener)
                {
                    giveSmashGuiOpener(p);
                }

                for (WorldGui inv : openWorldGuis)
                {
                    if (inv.hasThisOpen(p))
                    {
                        return "";
                    }
                }
                new WorldGui(p).open(p);

                return "";
            }
        });
    }

    /**
     * Function: openSmashGui
     * Purpose: To display the world gui to a player
     *
     * @param p - The player for which to open a world gui
     */
    public static void openSmashGui(final Player p)
    {
        openSmashGui(p, false);
    }

    public static void handleWorldTransfer(Player p, World worldFrom, final World worldTo, boolean wasToFromSpectation)
    {
        if (wasToFromSpectation || worldFrom == null || worldTo == null || worldFrom != worldTo)
        {
            if (worldFrom != null)
            {
                getWorldType(worldFrom).handleLeave(p, worldFrom, worldTo == null, wasToFromSpectation);
            }

            if (worldTo != null)
            {
                getWorldType(worldTo).handleJoin(p, worldFrom == null, worldTo, wasToFromSpectation);
            }
        }
    }

    /**
     * Gets the deathmatch preference for the player in format "<preference> <lives>" from his player data file. It sets his preference to default if there wasn't already one.
     * @param p - The player for which we would like to fetch the deathmatch preference
     * @return - The deathmatch preference of the player as string
     */
    public static String getDeathMatchPreference(Player p)
    {
        String preference = FileManager.getData(getGeneralPlayerFile(p), DEATHMATCH_PREFERENCE_DATANAME);
        Scanner s = new Scanner(preference);
        if (preference.length() == 0 || !s.hasNextBoolean())
        {
            preference = "false 4";
            FileManager.putData(getGeneralPlayerFile(p), DEATHMATCH_PREFERENCE_DATANAME, preference);
        }
        else
        {
            boolean dm = s.nextBoolean();
            if (!s.hasNextInt())
            {
                preference = dm + " 4";
                FileManager.putData(getGeneralPlayerFile(p), DEATHMATCH_PREFERENCE_DATANAME, preference);
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
            FileManager.putData(getGeneralPlayerFile(p), DEATHMATCH_PREFERENCE_DATANAME, "true " + lives);
        }
        else
        {
            p.sendMessage(ChatColor.GREEN + "You now prefer Point-based games.");
            if (lives < 3)
            {
                lives = 3;
            }
            FileManager.putData(getGeneralPlayerFile(p), DEATHMATCH_PREFERENCE_DATANAME, "false " + lives);
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
        if (setFlying && ((Entity)p).isOnGround())
        {
            Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
            {
                public String call()
                {
                    Location l = p.getLocation();
                    p.teleport(new Location(p.getWorld(), l.getX(), l.getY() + 0.01, l.getZ()));
                    return "";
                }
            });
        }
        p.setFlying(setFlying);
    }

    public static void setSpectatorMode(Player newPlayer, boolean spectate)
    {
        if (spectate)
        {
            if (!isInSpectatorMode(newPlayer))
            {
                spectators.add(newPlayer);
                newPlayer.sendMessage(ChatColor.GREEN + "" + ChatColor.ITALIC + "You are now in spectator mode.");
                handleWorldTransfer(newPlayer, newPlayer.getWorld(), newPlayer.getWorld(), true);
            }
            allowFullflight(newPlayer, true);
            //addTabEntry(p.getDisplayName());
            //removeAllTabEntries(p);
        }
        else
        {
            if (isInSpectatorMode(newPlayer))
            {
                spectators.remove(newPlayer);
                newPlayer.sendMessage(ChatColor.GREEN + "" + ChatColor.ITALIC + "You are no longer in spectator mode.");
                handleWorldTransfer(newPlayer, newPlayer.getWorld(), newPlayer.getWorld(), true);
            }
            if (!hasSpecialPermissions(newPlayer))
            {
                newPlayer.setAllowFlight(false);
            }
            //addAllTabEntries(p);
        }
        syncSpectators(newPlayer);
    }

    public static List<Player> getSpectators(World world)
    {
        List<Player> players = new ArrayList<Player>();
        for (Player dweller : world.getPlayers())
        {
            if (isInSpectatorMode(dweller))
            {
                players.add(dweller);
            }
        }
        return players;
    }

    public static List<Player> getNonspectators(World w)
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

    final boolean allowsWarps()
    {
        return minimumMaxWarps > 0 && maximumMaxWarps >= minimumMaxWarps;
    }

    public File[] getSourceWorlds()
    {
        FilenameFilter fileFilter = new FilenameFilter()
        {
            public boolean accept(File file, String name)
            {
                if (isValid() && name.startsWith(getSourceWorldPrefix()))
                {
                    return true;
                }
                return false;
            }
        };

        File[] templates = getRoot().listFiles(fileFilter);
        return templates == null ? new File[0] : templates;
    }

    public void giveStarterItems(Player p)
    {
    }

    static class Warp extends Location
    {
        static List<String> warpCreateVariations = new ArrayList<String>();
        static List<String> warpRemoveVariations = new ArrayList<String>();
        static
        {
            warpCreateVariations.add("create");
            warpCreateVariations.add("add");
            warpCreateVariations.add("set");
            warpRemoveVariations.add("delete");
            warpRemoveVariations.add("remove");
        }

        private final String name;

        private static File getWarpFile(Player p, boolean forceValid)
        {
            return FileManager.getPlayerFile(p, p.getWorld(), "", "Warps.json", forceValid);
        }

        private static File getWarpFile(Player p)
        {
            return getWarpFile(p, true);
        }

        public static List<Warp> getWarps(Player p, boolean outputResults)
        {
            World world = p.getWorld();
            WorldType type = getWorldType(world);
            List<Warp> result = new ArrayList<Warp>();
            if (!type.allowsWarps())
            {
                if (outputResults)
                {
                    p.sendMessage(ChatColor.RED + "This world doesn't allow warping");
                }
            }
            else
            {
                File f = getWarpFile(p, false);
                if (f.exists())
                {
                    for (Entry entry : FileManager.getAllEntries(f))
                    {
                        result.add(new Warp(p, entry.getKey(), FileManager.deserializeLocation(entry.getValue()), false));
                    }
                }
            }

            if (outputResults)
            {
                if (result.size() == 0)
                {
                    p.sendMessage(ChatColor.YELLOW + "You have no warps right now. Do /warp create <name> to create one!");
                }
                else
                {
                    p.sendMessage(ChatColor.GREEN + "Your Warps:");
                    for (Warp warp : result)
                    {
                        InteractiveChat.sendMessage(p, warp.toString(), ChatColor.YELLOW + "Click to warp to " +  warp.name + "!", "warp " + warp.name);
                    }
                }
            }
            return result;
        }

        @Override
        public String toString()
        {
            return ChatColor.WHITE + name + ": " + ChatColor.BLUE + toCoords();
        }

        private String toCoords()
        {
            return getBlockX() + ", " + getBlockY() + ", " + getBlockZ();
        }


        public static void createWarp(final Player p, final String warpName)
        {
            List<Warp> warps = getWarps(p, false);
            WorldType worldType = getWorldType(p);
            final int maxWarps = worldType.getMaxWarps(p);
            final String lowerWarpName = warpName.toLowerCase();
            if (warps.size() >= maxWarps)
            {
                p.sendMessage(ChatColor.RED + "You've reached your maximum number of warps (" + maxWarps + ")! Do /warp delete <name> to remove warps!");
            }
            else if (warpCreateVariations.contains(lowerWarpName) || warpRemoveVariations.contains(lowerWarpName) ||
                    lowerWarpName.equals("help") || lowerWarpName.equals("list"))
            {
                p.sendMessage(ChatColor.RED + "You can't create a warp named " + warpName);
            }
            else if (ForcefieldManager.isOutsideForcefield(p))
            {
                p.sendMessage(ChatColor.RED + "Nice try. Too bad I thought of that.");
            }
            else if (!worldType.blockIsProtected(p.getLocation().getBlock(), p, true))
            {
                for (final Warp warp : warps)
                {
                    if (warp.name.toLowerCase().equals(lowerWarpName))
                    {
                        final Location newLocation = p.getLocation();
                        new Verifier.BooleanVerifier(p, ChatColor.YELLOW + "You already have a warp called " + warp.name + ". Overwrite it?")
                        {
                            @Override
                            public void performYesAction()
                            {
                                warp.removeData(p);
                                new Warp(p, warpName, newLocation, true);
                                p.sendMessage(ChatColor.GREEN + "Warp " + warpName + " is now where you were just standing when you typed \"/warp create " + warpName + "\"!"); //don't want to give coords away
                            }

                            @Override
                            public void performNoAction()
                            {
                                p.sendMessage(ChatColor.RED + "Warp creation cancelled!");
                            }
                        };
                        return;
                    }
                }

                Warp newWarp = new Warp(p, warpName, p.getLocation(), true);
                p.sendMessage(ChatColor.GREEN + "Created new warp called " + newWarp.getName() + " where you're standing!");
            }
        }

        private void removeData(Player owner)
        {
            FileManager.removeEntryWithKey(getWarpFile(owner), name);
        }

        public static void removeWarp(final Player p, String name)
        {
            List<Warp> warps = getWarps(p, false);
            for (final Warp warp : warps)
            {
                if (warp.name.equalsIgnoreCase(name))
                {
                    new Verifier.BooleanVerifier(p, ChatColor.YELLOW + "Are you sure you'd like to remove warp " + warp.name + "?")
                    {
                        @Override
                        public void performYesAction()
                        {
                            warp.removeData(p);
                            p.sendMessage(ChatColor.GREEN + "Warp " + warp.name + " deleted!");
                        }

                        @Override
                        public void performNoAction()
                        {
                            p.sendMessage(ChatColor.GREEN + "Cancelled deleting warp " + warp.name);
                        }
                    };
                    return;
                }
            }

            p.sendMessage(ChatColor.RED + "Did not find warp " + name + ". These are your current warps:");
            for (Warp warp : warps)
            {
                p.sendMessage(warp.toString());
            }
        }


        private Warp(Player owner, String name, double x, double y, double z, float yaw, float pitch, boolean justCreated)
        {
            super(owner.getWorld(), x, y, z, yaw, pitch);
            this.name = name;
            if (justCreated)
            {
                FileManager.putData(getWarpFile(owner), name, FileManager.serializeLocation(this));
            }
        }

        private Warp(Player owner, String name, Location location, boolean justCreated)
        {
            this(owner, name, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch(), justCreated);
        }

        public String getName()
        {
            return name;
        }

    }

    private File getUnlockedWarpsFile(Player p, boolean forceValid)
    {
        return FileManager.getPlayerFile(p, p.getWorld(), "", "UnlockedWarps.txt", forceValid);
    }

    public final boolean incrementMaxWarps(Player p)
    {
        return setMaxWarps(p, getMaxWarps(p) + 1);
    }

    final boolean setMaxWarps(Player p, int maxWarps)
    {
        if (maxWarps >= minimumMaxWarps && maxWarps <= maximumMaxWarps)
        {
            File f = getUnlockedWarpsFile(p, true);
            FileManager.putFileContents(f, maxWarps);
            return true;
        }
        return false;
    }

    public final int getMaxWarps(Player p)
    {
        File f = getUnlockedWarpsFile(p, false);
        if (f.exists())
        {
            try
            {
                return Integer.valueOf(FileManager.readFileContents(f));
            }
            catch (NumberFormatException ex)
            {
                ex.printStackTrace();
                f.delete();
            }
        }
        return minimumMaxWarps;
    }

    protected static void syncSpectators(Player p)
    {
        if (!isInSpectatorMode(p))
        {
            for (Player player : Bukkit.getOnlinePlayers())
            {
                player.showPlayer(p);
                if (isInSpectatorMode(player))
                {
                    p.hidePlayer(player);
                }
            }
        }
        else
        {
            for (Player player : Bukkit.getOnlinePlayers())
            {
                p.showPlayer(player);
                if (!isInSpectatorMode(player))
                {
                    player.hidePlayer(p);
                }
            }
        }
    }

    public static boolean isInSpectatorMode(Player p)
    {
        return spectators.contains(p);
    }

    /**
     * Function:
     * Purpose:
     *
     * @param sender
     */
    public static void listWorlds(CommandSender sender)
    {
        boolean foundAWorld = false;
        for (WorldType type : values())
        {
            for (World world : type.getWorlds())
            {
                if (world != null)
                {
                    WorldStatus status = type.getWorldStatus(world, sender);
                    String statusPart = "";
                    String hoverMessage = "";
                    if (status.getMessage() != null)
                    {
                        statusPart = ChatColor.BLUE + "| " + ChatColor.YELLOW + "Status: " + status.getMessage();
                    }

                    if (status.getJoinableStatus() != null)
                    {
                        hoverMessage = status.getJoinableStatus().getHoverMessage();
                    }

                    if (sender instanceof Player)
                    {
                        InteractiveChat.sendMessage((Player)sender, new InteractiveChat.MessagePart[]
                        {
                            new InteractiveChat.MessagePart(type.getDisplayName(world) + " ", hoverMessage, JOIN_CMD + " " + world.getName()),
                            new InteractiveChat.MessagePart(statusPart)
                        });
                    }
                    else
                    {
                        sender.sendMessage(type.getDisplayName(world) + " (" + world.getName()  + ")" + statusPart);
                    }
                    foundAWorld = true;
                }
                else
                {
                    sendErrorMessage("Error! When " + sender.getName() + " tried to list the worlds, one of the Tournament worlds was null!");
                }
            }
        }

        if (!foundAWorld)
        {
            sender.sendMessage(ChatColor.GRAY + "No Smash worlds were found.");
        }
    }
//
//    public static boolean isGuiOpener(ItemStack item)
//    {
//        try
//        {
//            return item.getItemMeta().getDisplayName().equalsIgnoreCase(SMASH_GUI_ITEM.getItemMeta().getDisplayName());
//        }
//        catch (NullPointerException ex) {}
//        return false;
//    }

    /**
     * Function:
     * Purpose:
     *
     * @param p
     */
//    private static void removeSmashGuiOpener(Player p)
//    {
//        WORLD_GUI.getSpecialItem().removeAll(p);
//    }

    public static boolean hasGuiOpener(Player p)
    {
        return p.getInventory().contains(WORLD_GUI.getItemStack());
    }

//    /**
//     * Function:
//     * Purpose:
//     *
//     * @param p
//     */
//    public static void giveSmashGuiOpener(final Player p)
//    {
//        giveSmashGuiOpener(p, false);
//    }

    /**
     * Function:
     * Purpose:
     *
     * @param p
     */
    public static void giveSmashGuiOpener(final Player p/*, final boolean alreadyChecked*/)
    {
        WORLD_GUI.give(p);
//        Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable() {
//            public String call()
//            {
//                WorldType type = getWorldType(p.getWorld());
//                if (!hasGuiOpener(p) && (!SmashWorldManager.isSmashWorld(type) || isInSpectatorMode(p)))
//                {
//                    boolean foundSpaceForIt = false;
//                    if (p.getInventory().getSpecialItem(8) == null || p.getInventory().getSpecialItem(8).getType().equals(Material.AIR))
//                    {
//                        p.getInventory().setItem(8, SMASH_GUI_ITEM);
//                        foundSpaceForIt = true;
//                    }
//                    else
//                    {
//                        for (int i = 0; i < p.getInventory().getContents().length; i++)
//                        {
//                            if (p.getInventory().getContents()[i] == null || p.getInventory().getContents()[i].getType().equals(Material.AIR))
//                            {
//                                foundSpaceForIt = true;
//                                p.getInventory().setItem(i, SMASH_GUI_ITEM);
//                                break;
//                            }
//                        }
//                    }
//
//                    if (foundSpaceForIt)
//                    {
//                        p.updateInventory();
//                        if (!alreadyChecked && type == STUFFLAND)
//                        {
//                            Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
//                                public void run()
//                                {
//                                    giveSmashGuiOpener(p, true);
//                                }
//                            }, 30);
//                        }
//                    }
//                }
//                return "";
//            }
//        });
    }

    static void setTourneyPreferer(Player p, boolean preferTourney)
    {
        Boolean alreadyPrefersTourneys = tourneyPreferers.put(p, preferTourney);
    }

    static boolean getAndForgetIfPrefersTourneys(Player p)
    {
        Boolean prefersTourneys = tourneyPreferers.get(p);
        if (prefersTourneys == null)
        {
            prefersTourneys = SmashStatTracker.getTourneyLevel(p) > SmashStatTracker.LOWEST_TOURNEY_LEVEL;
        }
        else
        {
            tourneyPreferers.remove(p);
        }
        return prefersTourneys;
    }

    public static WorldType getWorldType(Block b)
    {
        return getWorldType(b.getWorld());
    }

    public static WorldType getWorldType(Player p)
    {
        return getWorldType(p.getWorld());
    }

    public static WorldType getWorldType(World w)
    {
        return w == null ? INVALID : getWorldType(w.getName());
    }

    public static WorldType getWorldType(String worldName)
    {
        if (worldName != null)
        {
            for (WorldType type : values())
            {
                if (worldName.startsWith(type.getDrainWorldPrefix()) && !worldName.startsWith(type.getSourceWorldPrefix()) && !worldName.endsWith("_nether"))
                {
                    return type;
                }
            }
        }
        return INVALID;
    }



    public static class AttackSource
    {
        private final AttackCulprit culprit;
        private final boolean isProjectile;
        private final Integer attackPowerOverride;
        private final int durationTicks;

        public static class AttackCulprit
        {
            private final String attackerName;
            private final String weaponName;
            private final LivingEntity entity;
            private SpecialItem specialItem;

            public AttackCulprit(String attackerName, String customWeaponName)
            {
                Player player = attackerName.length() > 0x10 ? null : Bukkit.getPlayer(attackerName);
                if (player != null)
                {
                    this.entity = player;
                    this.attackerName = player.getName();
                }
                else
                {
                    this.entity = null;
                    this.attackerName = UUIDFetcher.getCapitalName(attackerName);
                }
                this.weaponName = customWeaponName;
                this.specialItem = null;
            }

            public AttackCulprit(String attackerName, SpecialItem customWeapon)
            {
                this(attackerName, customWeapon.coloredName());
                this.specialItem = customWeapon;
            }

            public AttackCulprit(String attackerName)
            {
                this(attackerName, (String)null);
            }

            public AttackCulprit(LivingEntity entity, String customWeaponName)
            {
                this.entity = entity;
                this.attackerName =            entity == null ? null :
                                    entity instanceof Player ? ((Player)entity).getDisplayName() :
                                                                entity.getName();
                if (customWeaponName == null)
                {
                    final ItemStack itemHeld;
                    final SpecialItem specialItem;
                    if (entity != null)
                    {
                        itemHeld = entity.getEquipment().getItemInHand();
                        specialItem = getWorldType(entity.getWorld()).getSpecialItem(entity, itemHeld);
                    }
                    else
                    {
                        itemHeld = null;
                        specialItem = null;
                    }
                    this.weaponName = specialItem == null ? (itemHeld == null ? "air" : InventoryManager.getItemName(itemHeld)) : specialItem.coloredName();
                }
                else
                {
                    this.weaponName = customWeaponName;
                }
                this.specialItem = null;
            }

            public AttackCulprit(LivingEntity entity, SpecialItem customWeapon)
            {
                this(entity, customWeapon.coloredName());
                this.specialItem = customWeapon;
            }

            public AttackCulprit(LivingEntity entity, SpecialItem specialItem, String customWeaponName)
            {
                this(entity, customWeaponName);
                this.specialItem = specialItem;
            }

            public AttackCulprit(LivingEntity entity)
            {
                this(entity, (String)null);
            }
        }

        public AttackSource(AttackCulprit culprit, boolean isProjectile)
        {
            this(culprit, isProjectile, null);
        }

        public AttackSource(AttackCulprit culprit, boolean isProjectile, int durationTicks, Integer attackPowerOverride)
        {
            Validate.notNull(culprit);
            this.culprit = culprit;
            this.durationTicks = durationTicks;
            this.isProjectile = isProjectile;
            this.attackPowerOverride = attackPowerOverride;
        }

        public AttackSource(AttackCulprit player)
        {
            this(player, false, 23, null);
        }

        public AttackSource(AttackCulprit player, boolean isProjectile, Integer customPower)
        {
            this(player, isProjectile, 20*30, customPower);
        }

        public boolean hasWeapon()
        {
            return culprit.weaponName != null;
        }

        public String getWeaponName()
        {
            Validate.isTrue(hasWeapon());
            return culprit.weaponName;
        }

        public boolean hasSpecialItem()
        {
            return culprit.specialItem != null;
        }

        public SpecialItem getSpecialItem()
        {
            Validate.isTrue(hasSpecialItem());
            return culprit.specialItem;
        }


        public boolean isPlayer()
        {
            return isLiving() && culprit.entity instanceof Player;
        }

        public Player getPlayer()
        {
            Validate.isTrue(isPlayer());
            return (Player)culprit.entity;
        }

        public boolean isLiving()
        {
            return culprit.entity != null;
        }

        public LivingEntity getLivingEntity()
        {
            Validate.isTrue(isLiving());
            return culprit.entity;
        }

        public boolean hasAttackerName()
        {
            return culprit.attackerName != null;
        }

        public String getAttackerColoredName()
        {
            Validate.isTrue(hasAttackerName());
            return culprit.attackerName;
        }

        public String getAttackerColorlessName()
        {
            return ChatColor.stripColor(getAttackerColoredName());
        }

        public int getDurationTicks()
        {
            return durationTicks;
        }

        public AttackCulprit getCulprit()
        {
            return culprit;
        }

        public float getDamage(EntityDamageByEntityEvent event)
        {
            return attackPowerOverride == null ? (!hasSpecialItem() ? (float)event.getDamage() :
                    (isProjectile ? getSpecialItem().getRangeDamage(event) : getSpecialItem().getMeleeDamage(event))) : attackPowerOverride;
        }

        public void performHit(Entity victim)
        {
            if (hasSpecialItem() && victim != null && culprit.entity != null)
            {
                if (isProjectile)
                {
                    getSpecialItem().performRangeHit(culprit.entity, victim);
                }
                else
                {
                    getSpecialItem().performMeleeHit(culprit.entity, victim);
                }
            }
        }
    }

    public AttackSource getAttackSource(Entity entity)
    {
        if (entity == null)
        {
            return null;
        }
        if (entity instanceof Player || entity instanceof LivingEntity && getHeldSpecialItem((LivingEntity)entity) != null)
        {
            return new AttackSource(new AttackSource.AttackCulprit((LivingEntity)entity));
        }
        return entitySources.get(entity);
    }

    public void setAttackSource(final Entity entity, final AttackSource wieldedItem)
    {
        if (!entitySources.containsKey(entity))
        {
            entitySources.put(entity, wieldedItem);

            Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable()
            {
                @Override
                public void run()
                {
                    if (!wieldedItem.isLiving() || entity != wieldedItem.getLivingEntity() && !(entity instanceof Player))
                    {
                        entity.remove();
                    }
                    entitySources.remove(entity);
                }
            }, wieldedItem.getDurationTicks());
        }
    }

    //******************************************

    final Integer getWorldSideScore(World w, String entry)
    {
        return getWorldScoreboard(w).getSideScore(entry);
    }

    final void setScoreboardsInvisible(Player p)
    {
        setPersonalScoreboardVisible(p, false);
        setWorldScoreboardVisible(p, false);
    }

    final void setWorldScoreboardVisible(World world, boolean visible)
    {
        getWorldScoreboard(world).setVisibleToAllUsers(visible);
    }

    final void setWorldScoreboardVisible(Player p, boolean visible)
    {
        getWorldScoreboard(p.getWorld()).setVisible(p, visible);
    }

    final void setPersonalScoreboardVisible(Player p, boolean visible)
    {
        getPersonalScoreboard(p).setVisibleToAllUsers(visible);
    }

    final void setWorldSideScore(World world, String entry, Integer score)
    {
        getWorldScoreboard(world).setSideScore(entry, score);
    }

    final void setWorldSideScore(World world, String entry, String value)
    {
        getWorldScoreboard(world).setSideScore(entry, value);
    }

    final void setPersonalSideScore(Player p, String entry, Integer score)
    {
        getPersonalScoreboard(p).setSideScore(entry, score);
    }

    final void setPersonalSideScore(Player p, String entry, String value)
    {
        getPersonalScoreboard(p).setSideScore(entry, value);
    }

    public final void setWorldBelowNameScore(Player p, Integer score)
    {
        getWorldScoreboard(p).setBelowNameScore(p, score);
    }

    final void setWorldScoreboardTitle(World world, String title)
    {
        getWorldScoreboard(world).setSideTitle(title);
    }

    final void setPersonalScoreboardTitle(Player p, String title)
    {
        getPersonalScoreboard(p).setSideTitle(title);
    }

    final boolean isOnWorldScoreboard(String entry, World w)
    {
        return getWorldScoreboard(w).getColorlessEntries().contains(ChatColor.stripColor(entry));
    }

    boolean canPlayJoinLeaveMessages(World world)
    {
        return true;
    }

    final boolean isOnWorldScoreboard(Player p)
    {
        return isOnWorldScoreboard(p.getName(), p.getWorld());
    }

    //**************************************************
    final void removeWorldScoreboard(World w)
    {
        UsefulScoreboard scoreboard = worldScoreboards.get(w);
        if (scoreboard != null)
        {
            scoreboard.destroy();
            worldScoreboards.remove(w);
        }
    }

    final boolean hasWorldScoreboard(World world)
    {
        return worldScoreboards.get(world) != null;
    }

    public final UsefulScoreboard getWorldScoreboard(World world/*, boolean createIfMissing*/)
    {
        UsefulScoreboard scoreboard = worldScoreboards.get(world);
        if (scoreboard == null)
        {
            scoreboard = new UsefulScoreboard(world, worldScoreboardTitle, belowNameSuffix);
            worldScoreboards.put(world, scoreboard);
        }
        return scoreboard;
    }

    final UsefulScoreboard getWorldScoreboard(Player p)
    {
        return getWorldScoreboard(p.getWorld());
    }

    final void removePersonalScoreboard(Player p, World world)
    {
        HashMap<Player, UsefulScoreboard> scoreboardMap = personalScoreboards.get(world);
        if (scoreboardMap != null)
        {
            UsefulScoreboard scoreboard = scoreboardMap.get(p);
            if (scoreboard != null)
            {
                scoreboard.destroy();
                scoreboardMap.remove(p);

                if (scoreboardMap.size() == 0)
                {
                    personalScoreboards.remove(world);
                }
            }
        }
    }

    final void removePersonalScoreboard(Player p)
    {
        removePersonalScoreboard(p, p.getWorld());
    }

    final boolean hasPersonalScoreboard(Player p, World world)
    {
        HashMap<Player, UsefulScoreboard> scoreboardMap = personalScoreboards.get(world);
        return scoreboardMap != null && scoreboardMap.get(p) != null;
    }

    final UsefulScoreboard getPersonalScoreboard(Player p)
    {
        return getPersonalScoreboard(p, p.getWorld());
    }

    final UsefulScoreboard getPersonalScoreboard(Player p, World world/*, boolean createIfMissing*/)
    {
        HashMap<Player, UsefulScoreboard> scoreboardMap = personalScoreboards.get(world);
        if (scoreboardMap == null)
        {
            scoreboardMap = new HashMap<Player, UsefulScoreboard>();
            personalScoreboards.put(world, scoreboardMap);
        }

        UsefulScoreboard scoreboard = scoreboardMap.get(p);
        if (scoreboard == null)
        {
            scoreboard = new UsefulScoreboard(p, personalScoreboardTitle, belowNameSuffix);
            scoreboardMap.put(p, scoreboard);
        }
        return scoreboard;
    }

    //**************************************************

    String getDeathMessage(PlayerDeathEvent event, Player whoWeAreTelling)
    {
        return ChatColor.AQUA + event.getDeathMessage();
    }

    String getNormalLeaveMessage(Player leaver, World w)
    {
        return leaver.getDisplayName() + ChatColor.GRAY + " has left the game!";
    }

    String getSpectatorLeaveMessage(Player leaver, World w)
    {
        return null;
    }

    String getNormalJoinMessage(Player player, World worldJoined)
    {
        return player.getDisplayName() + ChatColor.GRAY + " has joined the game!";
    }

    String getSpectatorJoinMessage(Player player, World worldJoined)
    {
        return null;
    }

    final String getSourceWorldPrefix()
    {
        return getDrainWorldPrefix() + "World";
    }

    final String getDrainWorldPrefix()
    {
        return "" + name().charAt(0) + (name().length() > 1 ? name().substring(1, name().length()).toLowerCase() : "");
    }

    final String getDisplayName(int worldNumber)
    {
        return getDisplayName(getWorld(worldNumber));
    }

    public final String getDisplayName(World world)
    {
        if (isValid())
        {
            if (getMaxWorlds() <= 1)
            {
                return getDisplayPrefix(world) + ChatColor.RESET;
            }
            return getDisplayPrefix(world) + " " + getWorldNumber(world) + ChatColor.RESET;
        }
        else
        {
            return world.getName();
        }
    }

    abstract String getDisplayPrefix(World world);

    final int getWorldNumber(World w)
    {
        if (isValid())
        {
            String worldName = w.getName();
            String prefix = getDrainWorldPrefix();
            try
            {
                if (worldName.length() <= prefix.length())
                {
                    throw new NumberFormatException();
                }
                String stuffAfterPrefix = worldName.substring(prefix.length(), worldName.length());
                return Integer.valueOf(stuffAfterPrefix);
            }
            catch (NumberFormatException ex)
            {
                sendErrorMessage(ChatColor.RED + "World " + worldName + " has messed up name!");
            }
        }
        else
        {
            sendErrorMessage("Error! Tried to get the world number of an invalid world!");
        }
        return -1;
    }

    final String getWorldName(int worldNumber)
    {
        return getDrainWorldPrefix() + worldNumber;
    }

    final World getWorld(int worldNumber)
    {
        return Bukkit.getWorld(getWorldName(worldNumber));
    }

    //*****************************************************

    public World getBestWorld(Player p)
    {
        World bestWorld = null;
        for (Integer i : takenWorldNumbers())
        {
            World candidate = getWorld(i);
            if (candidate != null && (bestWorld == null || candidate.getPlayers().size() > bestWorld.getPlayers().size()))
            {
                bestWorld = candidate;
            }
        }
        return bestWorld;
    }

    public final ItemStack getWorldRepresenter(World w, Player p)
    {
        if (w == null || !isValid())
        {
            return getCustomItemStack(INVALID.getJoinTheWorldMaterial(w), getDisplayName(w));
        }

        ItemStack worldRepresenter = getCustomItemStack(getJoinTheWorldMaterial(w), getDisplayName(w));

        if (worldRepresenter == null)
        {
            worldRepresenter = getCustomItemStack(Material.INK_SACK, ChatColor.RED + "Not a smash world!", 1);
        }
        List<String> itemLores = getLoreForJoinItem(w, p);
        if (itemLores != null && itemLores.size() > 0)
        {
            ItemMeta meta = worldRepresenter.getItemMeta();
            meta.setLore(itemLores);
            worldRepresenter.setItemMeta(meta);
        }
        return worldRepresenter;
    }

    List<Integer> takenWorldNumbers()
    {
        return new ArrayList<Integer>(takenWorldNumbers);
    }

    public List<World> getWorlds()
    {
        ArrayList<World> result = new ArrayList<World>();
        for (Integer i : takenWorldNumbers)// (int i = LOWEST_WORLD_NUM; i < maxWorldsOfType + LOWEST_WORLD_NUM; i++)
        {
            World w = getWorld(i);
            if (w != null)
            {
                result.add(w);
            }
        }
        return result;
    }

    public int getMinWorlds()
    {
        return minWorldsOfType;
    }

    public int getMaxWorlds()
    {
        return maxWorldsOfType;
    }

    public void unloadWorld(World w)
    {
        if (isValid() && w != null && getWorlds().contains(w))
        {
            performUnloadWorldAction(w);

            for (WorldGui inv : openWorldGuis)
            {
                inv.removeItem(w);
            }

            clearBukkitYML(w.getName());
            for (Player p : w.getPlayers())
            {
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }

            WorldManager.setLocked(w, false);
            int worldNumber = getWorldNumber(w);

            setTaken(worldNumber, false);
        }
        else
        {
            sendErrorMessage("Error! Tried to unload a bad world!");
        }
    }

    public void setTaken(int worldNumber, boolean taken)
    {
        if (isValid())
        {
            if (taken && !takenWorldNumbers.contains(worldNumber))
            {
                int last = takenWorldNumbers.size();
                int i = 0;
                for (; i < last; i++)
                {
                    if (takenWorldNumbers.get(i) > worldNumber)
                    {
                        takenWorldNumbers.add(i, worldNumber);
                        return;
                    }
                }
                if (i == last)
                {
                    takenWorldNumbers.add(last, worldNumber);
                }
            }
            else if (!taken && takenWorldNumbers.contains(worldNumber))
            {
                takenWorldNumbers.remove((Integer)worldNumber);
            }
        }
    }

    public boolean worldNumberIsTaken(int worldNumber)
    {
        return takenWorldNumbers != null && takenWorldNumbers.contains(worldNumber) || isBeingDeleted(getWorldName(worldNumber));
    }

    public boolean canCreateWorlds()
    {
        return takenWorldNumbers.size() + worldsBeingDeleted.size() < maxWorldsOfType;
    }

    private boolean canDeleteWorlds()
    {
        return takenWorldNumbers().size() >= getMinWorlds();
    }

    public final WorldStatus getPreWorldStatus(World w, CommandSender p)
    {
        if (isBeingDeleted(w.getName()))
        {
            return new WorldStatus(ChatColor.RED + "" + ChatColor.BOLD + "Being deleted", WorldStatus.JoinableStatus.NOT_JOINABLE);
        }
        else if (getNonspectators(w).size() == 0)
        {
            if (w.getPlayers().size() == 0)
            {
                return new WorldStatus(ChatColor.GRAY + "Empty", WorldStatus.JoinableStatus.JOINABLE);
            }
            else
            {
                return new WorldStatus(ChatColor.GRAY + "Virtually empty", WorldStatus.JoinableStatus.JOINABLE);
            }
        }

        WorldStatus status = getWorldStatus(w, p);
        return status == null ? new WorldStatus(ChatColor.GREEN + "Joinable", WorldStatus.JoinableStatus.JOINABLE) : status;
    }



    //*****************************************************

    public final Iterator<SpecialItem> getGeneralItemIterator()
    {
        return getSpecialItemIterator(null);
    }

    public Iterator<SpecialItem> getSpecialItemIterator(LivingEntity player)
    {
        return new Iterator<SpecialItem>()
        {
            @Override
            public boolean hasNext()
            {
                return false;
            }

            @Override
            public void remove() {}

            @Override
            public SpecialItem next()
            {
                return null;
            }
        };
    }

    public final SpecialItem getSpecialItem(LivingEntity user, ItemStack item)
    {
        if (!isValid() || user == null || user.getWorld() == null || item == null || !item.hasItemMeta() || !item.getItemMeta().hasDisplayName())
        {
            return null;
        }
        if (WORLD_GUI.isThis(item))
        {
            return WORLD_GUI;
        }

        Iterator<SpecialItem> specialItemIterator = getSpecialItemIterator(user);
        while (specialItemIterator.hasNext())
        {
            SpecialItem specialItem = specialItemIterator.next();
            if (specialItem.isThis(item))
            {
                return specialItem;
            }
        }
        return null;
    }

    public final SpecialItem getHeldSpecialItem(LivingEntity p)
    {
        final ItemStack heldItem;
        if (p instanceof Player)
        {
            heldItem = ((Player)p).getItemInHand();
        }
        else
        {
            heldItem = p.getEquipment().getItemInHand();
        }
        return getSpecialItem(p, heldItem);
    }

    //****************

    void performPlayerDropItem(PlayerDropItemEvent event) {}

    abstract Material getJoinTheWorldMaterial(World w);

    void performLoadWorldAction(World w, boolean forCreation)
    {
        if (preventRain)
        {
            allowWeatherChanges(w, true);
            w.setStorm(false);
            w.setThundering(false);
            w.setWeatherDuration(10000);
            allowWeatherChanges(w, false);
        }

        if (!forCreation)
        {
            Iterator<SpecialItem> specialItemIterator = getGeneralItemIterator();
            while (specialItemIterator.hasNext())
            {
                specialItemIterator.next().performLoadWorldAction(w);
            }
        }

        for (WorldGui inv : openWorldGuis)
        {
            inv.addItem(w);
        }

        int worldNumber = getWorldNumber(w);
        setTaken(worldNumber, true);
    }

    void performUnloadWorldAction(World w) {}

    final void performResetAction(Player p)
    {
        InventoryKeep.resetInventoryState(p);
        SmashEntityTracker.resetSpeedAlteredPlayer(p);
        if (p.getGameMode() != defaultGamemode)
        {
            p.setGameMode(defaultGamemode);
        }
        PortalManager.cancelPortalTasks(p);
        p.getInventory().clear();
        if (!isInSpectatorMode(p))
        {
            if (!hasSpecialPermissions(p))
            {
                p.setAllowFlight(false);
            }
            p.getInventory().setArmorContents(new ItemStack[] {new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR)});
        }

        clearDamage(p);
        p.setHealth(p.getMaxHealth());
        p.setExp(0);
        p.setFoodLevel(20);
        p.setFireTicks(-20);
        p.setLevel(0);
        updateHitCooldown(p);
        for (PotionEffect effect : p.getActivePotionEffects())
        {
            p.removePotionEffect(effect.getType());
        }
        p.updateInventory();


        Iterator<SpecialItem> it = getSpecialItemIterator(p);
        while (it.hasNext())
        {
            SpecialItem item = it.next();
            item.performResetAction(p);
        }
    }

    void performBlockBreak(BlockBreakEvent event)
    {
        Block b = event.getBlock();
        if (blockIsProtected(b, event.getPlayer(), false))
        {
            event.setCancelled(true);
        }
    }

    final void performBlockPlace(BlockPlaceEvent event)
    {
        if (event.isCancelled()) return;

        if (blockIsProtected(event.getBlock(), event.getPlayer(), true))
        {
            event.setCancelled(true);
        }
    }

    final void performBlockExplode(EntityExplodeEvent event)
    {
        for (int i = 0; i < event.blockList().size(); i++)
        {
            Block b = event.blockList().get(i);
            PlaceableItem.performExploded(b);
            if (blockIsProtected(b, null, false))
            {
                event.blockList().remove(i--);
            }
        }
    }

    final void performBlockBurn(BlockBurnEvent event)
    {
        if (blockIsProtected(event.getBlock(), null, false))
        {
            event.setCancelled(true);
            Block b = event.getBlock().getRelative(0, 1, 0);
            if (b.getType().equals(Material.FIRE))
            {
                b.setType(Material.AIR);
            }
        }
//        checkBlock(e.getBlock().getRelative(0, 0, -1));
//        checkBlock(e.getBlock().getRelative(0, 0, 1));
//        checkBlock(e.getBlock().getRelative(0, -1, 0));
//        checkBlock(e.getBlock().getRelative(0, 1, 0));
//        checkBlock(e.getBlock().getRelative(-1, 0, 0));
//        checkBlock(e.getBlock().getRelative(1, 0, 0));
//
//
//        checkBlock(e.getBlock().getRelative(0, 1, 1));
//        checkBlock(e.getBlock().getRelative(0, -1, 1));
//        checkBlock(e.getBlock().getRelative(1, -1, 0));
//        checkBlock(e.getBlock().getRelative(-1, 1, 0));
//        checkBlock(e.getBlock().getRelative(1, 0, -1));
//        checkBlock(e.getBlock().getRelative(-1, 0, 1));
//
//        checkBlock(e.getBlock().getRelative(1, 1, 1));
//        checkBlock(e.getBlock().getRelative(1, 1, -1));
//        checkBlock(e.getBlock().getRelative(1, -1, 1));
//        checkBlock(e.getBlock().getRelative(1, -1, -1));
//        checkBlock(e.getBlock().getRelative(-1, 1, 1));
//        checkBlock(e.getBlock().getRelative(-1, 1, -1));
//        checkBlock(e.getBlock().getRelative(-1, -1, 1));
//        checkBlock(e.getBlock().getRelative(-1, -1, -1));
    }

    final void performBlockForm(BlockFormEvent event)
    {
        if (blockIsProtected(event.getNewState().getBlock(), null, true))/*event.getNewState().getType().equals(Material.SNOW) */
        {
            event.setCancelled(true);
        }
    }

    final void performBlockIgnite(BlockIgniteEvent event)
    {
        if (blockIsProtected(event.getBlock(), null, false))
        {
            switch (event.getCause())
            {
                case SPREAD:
                case FLINT_AND_STEEL:
                case EXPLOSION:
                case FIREBALL:
                    event.setCancelled(true);
            }
        }
    }

    final void performLiquidPlacement(PlayerBucketEmptyEvent event)
    {
        if (blockIsProtected(event.getBlockClicked(), event.getPlayer(), true))
        {
            event.setCancelled(true);
        }
    }

    void performPlayerMove(PlayerMoveEvent event) {}

    void performPlayerInteractEntity(PlayerInteractEntityEvent event)
    {
        if (event.getRightClicked() instanceof Boat)
        {
            final Player player = event.getPlayer();
            if (getVersion(player) <= 8)
            {
                event.setCancelled(true);
                player.sendMessage(ChatColor.YELLOW + "To use boats, switch to 1.9. Sorry I couldn't make boats work on better versions.");
            }
        }
    }

    final void performWeatherChange(WeatherChangeEvent event)
    {
        if (!worldCanChangeWeather(event.getWorld()))
        {
            timePreventedWeatherChange++;
            Bukkit.getConsoleSender().sendMessage(loggerPrefix() + ChatColor.GREEN + "Prevented WeatherChangeEvent for the " + timePreventedWeatherChange
                    + getOrdinalIndicator(timePreventedWeatherChange) + " time since startup!");
            event.setCancelled(true);
        }
    }

    void performProjectileLaunch(ProjectileLaunchEvent event)
    {
        if (event.getEntity().getShooter() instanceof LivingEntity)
        {
            LivingEntity livingEntity = (LivingEntity)event.getEntity().getShooter();
            SpecialItem item = getHeldSpecialItem(livingEntity);
            if (item != null)
            {
                item.performProjectileLaunch(livingEntity, event.getEntity());
                setAttackSource(event.getEntity(), new WorldType.AttackSource(new AttackSource.AttackCulprit(livingEntity, item), true));
            }
        }
//        if (event.getEntity().getShooter() instanceof LivingEntity)
//        {
//            LivingEntity livingEntity = (LivingEntity)event.getEntity().getShooter();
//            if (blockIsProtected(livingEntity.getLocation().getBlock(), livingEntity instanceof Player ? (Player)livingEntity : null, false))
//            {
//                event.setCancelled(true);
//            }
//        }
    }

    void performProjectileHit(ProjectileHitEvent event)
    {
        final Projectile proj = event.getEntity();
        if (proj.getShooter() instanceof LivingEntity)
        {
            WorldType.AttackSource attackSource = getAttackSource(proj);
            if (attackSource != null && attackSource.isLiving() && attackSource.hasSpecialItem())
            {
                attackSource.getSpecialItem().performProjectileLand(attackSource.getLivingEntity(), proj);
            }
        }
    }

    final void performConsolidate(ItemMergeEvent event)
    {
        if (preventConsolidation)
        {
            event.setCancelled(true);
        }
    }

    void performDamage(EntityDamageEvent event)
    {
        if (event.getEntity() instanceof Player && event.getCause().equals(EntityDamageEvent.DamageCause.FALL))
        {
            Player p = (Player)event.getEntity();
            SpecialItem heldItem = getHeldSpecialItem(p);
            if (heldItem != null && heldItem.performHeldLandAction(p))
            {
                event.setCancelled(true);
            }

            ItemStack[] contents = p.getInventory().getContents();
            for (int i = 0; i < 36; i++)
            {
                ItemStack unheldItem = contents[i];
                SpecialItem item = getSpecialItem(p, unheldItem);
                if (item != null && item != heldItem)
                {
                    item.performUnheldLandAction(p);
                }
            }
        }

        if (hasGenerator())
        {
            switch (event.getCause())
            {
                case ENTITY_ATTACK:
                case MAGIC:
                case THORNS:
                case PROJECTILE:
                case ENTITY_EXPLOSION:
                    if (WorldGenerationTools.getChunkProtectionLevel(event.getEntity().getLocation()) == WorldGenerationTools.ChunkProtectionLevel.DISABLE_EVERYTHING)
                    {
                        if (event.getEntity() instanceof Monster)
                        {
                            event.getEntity().remove();
                        }
                        else
                        {
                            event.setCancelled(true);
                        }
                        return;
                    }
                    break;
            }
        }
    }

    void performEntityDamageByEntity(EntityDamageByEntityEvent event)
    {
        if (!playerCanDamagePlayer(event.getDamager(), event.getEntity()))
        {
            event.setCancelled(true);
        }

        WorldType.AttackSource wItem = getAttackSource(event.getDamager());
        if (wItem != null)
        {
            event.setDamage(wItem.getDamage(event));
            wItem.performHit(event.getEntity());
        }
    }

    public boolean playerCanDamagePlayer(Entity attacker, Entity victim)
    {
        if (!allowDestruction && victim instanceof Item || attacker.getWorld() != victim.getWorld())
        {
            return false;
        }
        if (hasGenerator())
        {
            if (victim instanceof LivingEntity)
            {
                if (WorldGenerationTools.getChunkProtectionLevel(attacker.getLocation()) == WorldGenerationTools.ChunkProtectionLevel.DISABLE_EVERYTHING)
                {
                    if (attacker instanceof Monster)
                    {
                        attacker.remove();
                    }
                    else if (attacker instanceof Projectile && ((Projectile)attacker).getShooter() instanceof Monster)
                    {
                        ((Monster)((Projectile)attacker).getShooter()).remove();
                    }
                    else if (!(victim instanceof Monster))
                    {
                        if (attacker instanceof Player)
                        {
                            attacker.sendMessage(ChatColor.BLUE + "PvP is disabled here!");
                        }
                        return false;
                    }
                }
                else if (WorldGenerationTools.getChunkProtectionLevel(victim.getLocation()) == WorldGenerationTools.ChunkProtectionLevel.DISABLE_EVERYTHING)
                {
                    if (attacker instanceof Player)
                    {
                        attacker.sendMessage(ChatColor.BLUE + "That " + victim.getType().name().replaceAll("_", " ").toLowerCase() + " is inside a no-PVP zone!");
                    }
                    return false;
                }
            }
        }

        if (attacker instanceof Player && isInSpectatorMode((Player)attacker))
        {
            ((Player)attacker).sendMessage(ChatColor.GRAY + "You cannot attack while in spectator mode!");
            return false;
        }
        else if (victim instanceof Player && isInSpectatorMode((Player)victim))
        {
            return false;
        }
        return true;
    }

    final void handlePlayerDeath(PlayerDeathEvent event)
    {
        Player whoDied = event.getEntity();
        for (ItemStack item : event.getEntity().getInventory().getContents())
        {
            SpecialItem theItem = getSpecialItem(whoDied, item);
            if (theItem != null)
            {
                theItem.performUserDeathAction(whoDied, whoDied.getKiller());
            }
        }

        if (rememberPlayerState)
        {
            PlayerStateRecorder.forgetPlayerState(whoDied, whoDied.getWorld());
        }

        for (Player player : whoDied.getWorld().getPlayers())
        {
            String deathMessage = getDeathMessage(event, player);
            player.sendMessage(deathMessage == null ? event.getDeathMessage() : deathMessage);
        }
        event.setDeathMessage("");

        setLastDeathLocation(whoDied, whoDied.getLocation());
    }

    final void handlePickupItem(EntityPickupItemEvent event)
    {
        if (event.getEntity() instanceof Player)
        {
            Player p = (Player)event.getEntity();
            SpecialItem item = getSpecialItem(p, event.getItem().getItemStack());
            if (item != null)
            {
                if (item.performItemPickup(p, event.getItem().getLocation()))
                {
                    event.setCancelled(true);
                }
                else
                {
                    item.performSelectAction(p);
                }
            }
        }
    }

    void handlePlayerItemDamageEvent(PlayerItemDamageEvent e) {}

    final void handleToggleSneak(PlayerToggleSneakEvent event)
    {
        SpecialItem item = getHeldSpecialItem(event.getPlayer());
        if (item != null)
        {
            item.performSneakAction(event.getPlayer(), event.isSneaking());
        }
    }

    final void handlePlayerItemHeld(PlayerItemHeldEvent event)
    {
        //from Smash code
//                final Player p = e.getAttacker();
//                  ItemStack selectedItem = p.getInventory().getSmashDropItem(e.getNewSlot());
//                Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable() {
//                    public String call()
//                    {
//                        ItemStack selectedItem = p.getItemInHand();
//                        ItemStack previousItem = p.getInventory().getSpecialItem(e.getPreviousSlot());
//
//                        if (selectedItem == null || previousItem == null || !selectedItem.equals(previousItem))
//                        {
//                            SpecialItem previousSpecialItem = getSpecialItem(p, previousItem);
//                            if (previousSpecialItem != null)
//                            {
//                                previousSpecialItem.performDeselectAction(p);
//                            }
//                            SpecialItem selectedSpecialItem = getSpecialItem(p, selectedItem);
//                            if (selectedSpecialItem != null)
//                            {
//                                selectedSpecialItem.performSelectAction(p);
//                            }
//                        }
//                        return "";
//                    }
//                });
        final Player p = event.getPlayer();
        SpecialItem previousSpecialItem = getSpecialItem(p, p.getInventory().getItem(event.getPreviousSlot()));
        if (previousSpecialItem != null)
        {
            previousSpecialItem.performDeselectAction(p);
        }

        SpecialItem selectedSpecialItem = getSpecialItem(p, p.getInventory().getItem(event.getNewSlot()));
        if (selectedSpecialItem != null)
        {
            selectedSpecialItem.performSelectAction(p);
        }
    }

    static final void handlePlayerTeleportEvent(final PlayerTeleportEvent event)
    {
        final Player player = event.getPlayer();

        World oldWorld = event.getFrom().getWorld();
        World newWorld = event.getTo().getWorld();
        if (!playersWhoHaveBeenAllowedToTeleport.contains(player) && (oldWorld != newWorld || event.getTo().distanceSquared(event.getFrom()) > 3))
        {
            WorldType type = getWorldType(player.getWorld());

            if (!type.isAllowedToTeleport(player, event.getTo()))
            {
                event.setCancelled(true);
            }
            else
            {
                Integer delay = type.getTeleportDelaySeconds();
                if (delay != null && delay > 0 && player.getGameMode() != GameMode.CREATIVE && !isInSpectatorMode(player))
                {
                    if (nonFriendliesAreNearby(player))
                    {
                        event.setCancelled(true);
                        SmashEntityTracker.performActionWhenSafe(player, new SmashEntityTracker.AfkAction()
                        {
                            @Override
                            public void performAction()
                            {
                                forceTp(player, event.getTo());
                            }
                        }, delay, ChatColor.YELLOW + "There is another player nearby. Wait " + delay + " seconds to teleport!", ChatColor.GREEN + "Teleport successful!", ChatColor.RED + "You moved! Teleport cancelled!");
                    }
                    else if (nonFriendliesAreNearby(event.getTo(), player))
                    {
                        event.setCancelled(true);
                        new Verifier.BooleanVerifier(player, ChatColor.YELLOW + "There are potentially unfriendly players near there. Are you sure you'd like to teleport?")
                        {
                            @Override
                            public void performYesAction()
                            {
                                forceTp(player, event.getTo());
                            }

                            @Override
                            public void performNoAction()
                            {
                                player.sendMessage(ChatColor.RED + "Teleport cancelled!");
                            }
                        };
                    }
                }
            }
        }

        if (!event.isCancelled() && oldWorld != newWorld)
        {
            if (playersNotInAWorld.contains(player))
            {
                playersNotInAWorld.remove(player);
            }
            else
            {
                PlayerStateRecorder.rememberPlayerState(player, oldWorld);
            }
            PlayerStateRecorder.loadPlayerState(player, newWorld);
        }
    }

    void performPlayerInteract(PlayerInteractEvent event)
    {
        Player p = event.getPlayer();

        switch (event.getAction())
        {
            case LEFT_CLICK_BLOCK:
                if (p.getGameMode() == GameMode.ADVENTURE && blockIsProtected(event.getClickedBlock(), p, false))
                {
                    switch (event.getClickedBlock().getType())
                    {
                        case LONG_GRASS:
                        case YELLOW_FLOWER:
                        case RED_ROSE:
                        case VINE:
                        case LADDER:
                        case LEAVES:
                        case LEAVES_2:
                        case DEAD_BUSH:
                            event.setCancelled(true);
                            break;
                    }
                }
            case LEFT_CLICK_AIR:
                Entity theEntity = SmashAttackManager.getEntityBeingFaced(p, 4, 22.5F);
                if (theEntity != null && theEntity instanceof LivingEntity)
                {
                    WorldType.AttackSource source = WorldManager.getAttackSource(theEntity);
                    if (source != null && source.hasSpecialItem())
                    {
                        source.getSpecialItem().performLeftClickedWhileInMidAirAction(p, theEntity);
                    }
                }
                break;
            case RIGHT_CLICK_AIR:
            case RIGHT_CLICK_BLOCK:
            case PHYSICAL:
                if (isInSpectatorMode(p) && blockIsProtected(event.getClickedBlock() == null ? p.getLocation().getBlock() : event.getClickedBlock(), p, false))
                {
                    event.setCancelled(true);
                }
                break;
        }

        SpecialItem item = getSpecialItem(event.getPlayer(), event.getItem());
        if (item != null)
        {
            if (item.canBeUsed(p))
            {
                switch (event.getAction())
                {
                    case RIGHT_CLICK_AIR:
                        item.performRightClickAction(p, null);
                        break;
                    case RIGHT_CLICK_BLOCK:
                        item.performRightClickAction(p, event.getClickedBlock());
                        break;
                    case LEFT_CLICK_AIR:
                        item.performLeftClickAction(p, null);
                        break;
                    case LEFT_CLICK_BLOCK:
                        item.performLeftClickAction(p, event.getClickedBlock());
                        break;
                }
            }

            switch (event.getAction())
            {
                case RIGHT_CLICK_BLOCK:
                    if (!item.canPerformRightClickAction())
                    {
                        event.setCancelled(true);
                        p.updateInventory();
                    }
                    break;
            }
        }
    }

    void performCommand(final PlayerCommandPreprocessEvent event)
    {
        if (event.getMessage().length() > 1)
        {
            String commandSent = event.getMessage().substring(1, event.getMessage().length()).toLowerCase();
            Scanner s = new Scanner(commandSent);
            if (s.hasNext())
            {
                String label = s.next();
                if (hasGenerator() && label.equals("tp") && s.hasNext())
                {
                    s.next();
                    if (s.hasNext())
                    {
                        String niceName = toString();
                        if (niceName.length() > 1)
                        {
                            niceName = niceName.charAt(0) + niceName.substring(1, niceName.length()).toLowerCase();
                        }
                        else
                        {
                            niceName = "certain";
                        }
                        event.getPlayer().sendMessage(ChatColor.RED + "There shall be no coordinate tps in " + niceName + " worlds.");
                        event.setCancelled(true);
                    }
                }
                else if (label.equalsIgnoreCase("stop"))
                {
                    event.setCancelled(true);
                    shutServerDown(true);
                }
                else if (label.equalsIgnoreCase("stats"))
                {
                    event.setCancelled(true);
                    event.getPlayer().performCommand(commandSent.replaceAll("stats", "statistics"));
                }
                else if (label.equalsIgnoreCase("help"))
                {
                    event.setCancelled(true);
                    SmashStatTracker.displayHelpMessage(event.getPlayer());
                }
                else if (label.equalsIgnoreCase(START_CMD) && !hasSpecialPermissions(event.getPlayer()))
                {
                    event.setMessage(event.getMessage().replaceFirst(START_CMD, READY_UP_CMD));
                }
                else if (label.equalsIgnoreCase("weather"))
                {
                    allowWeatherChanges(event.getPlayer().getWorld(), true);
                    Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
                    {
                        public String call()
                        {
                            allowWeatherChanges(event.getPlayer().getWorld(), false);
                            return "";
                        }
                    });
                }
            }
        }
    }

    void performDropItem(PlayerDropItemEvent event)
    {
        Player player = event.getPlayer();
        SpecialItem item = getSpecialItem(player, event.getItemDrop().getItemStack());
        if (item != null)
        {
            if (item.performDropAction(player))
            {
                event.setCancelled(true);
            }
        }
    }

    boolean performCreateWorldCommand(CommandSender sender, String[] argsAfterType)
    {
        if (!isValid())
        {
            StringBuilder validTypesString = new StringBuilder();
            for (WorldType type : WorldType.values())
            {
                if (type.isValid())
                {
                    if (validTypesString.toString().length() != 0)
                    {
                        validTypesString.append(", ");
                    }
                    validTypesString.append(type.getDrainWorldPrefix());
                }
            }
            sender.sendMessage(ChatColor.RED + "Invalid world type! Valid world types are " + validTypesString.toString());
            return true;
        }

        boolean joinOnCreate = false;
        if (argsAfterType.length > 0)
        {
            String joinInput = argsAfterType[0];
            joinOnCreate = isTrue(joinInput) && sender instanceof Player;
        }

        createWorld(sender, joinOnCreate);
        return true;
    }


    private void bindAmountTask(final Player p, final ItemStack itemToSell, final short tally)
    {
        if (canAnyBeSold(p, itemToSell))
        {
            new Verifier.StringVerifier(p, ChatColor.YELLOW + "How many " + InventoryManager.getPluralName(itemToSell, (short)2) + " would you like to sell?")
            {
                @Override
                public void performAction(String decision)
                {
                    final Short amount;
                    if (decision.equalsIgnoreCase("all"))
                    {
                        amount = tally;
                    }
                    else if (decision.equalsIgnoreCase("half"))
                    {
                        amount = (short)(tally/2 + ((tally % 2) == 0 ? 0 : 1));
                    }
                    else
                    {
                        amount = getShortValue(decision);
                    }

                    bindPriceTask(p, itemToSell, tally, amount);
                }
            };
        }
    }

    private void bindPriceTask(final Player p, final ItemStack itemToSell, final short tally, final short amount)
    {
        if (tally < amount)
        {
            new Verifier.BooleanVerifier(p, ChatColor.RED + "You only have " + tally + " " + InventoryManager.getPluralName(itemToSell, tally) + " to sell! Sell all instead?")
            {
                @Override
                public void performYesAction()
                {
                    bindPriceTask(p, itemToSell, tally, tally);
                }

                @Override
                public void performNoAction()
                {
                    p.sendMessage(ChatColor.YELLOW + "Posting cancelled.");
                }
            };
        }
        else if (canAmountBeSold(p, itemToSell, tally, amount))
        {
            new Verifier.FloatVerifier(p, ChatColor.YELLOW + "Name your price.", ChatColor.RED + "Invalid price!")
            {
                @Override
                public void performAction(Float decision)
                {
                    EconomyItemManager.addToMarket(p, itemToSell, amount, decision);
                }
            };
        }
    }

    boolean handleCommand(final Player sender, String label, String[] args)
    {
        if (hasAnEconomy)
        {
            if (matchesCommand(label, SELL_CMD))
            {
                final ItemStack itemToSell = new ItemStack(sender.getItemInHand());
                final short tally = InventoryManager.getItemTally(sender, itemToSell);
                if (args.length == 0)
                {
                    bindAmountTask(sender, itemToSell, tally);
                }
                else
                {
                    try
                    {
                        String arg = args[0];
                        Short amount;
                        if (arg.equalsIgnoreCase("all"))
                        {
                            amount = tally;
                        }
                        else if (arg.equalsIgnoreCase("half"))
                        {
                            amount = (short)(tally/2 + ((tally % 2) == 0 ? 0 : 1));
                        }
                        else
                        {
                            amount = Short.valueOf(arg);
                        }

                        if (args.length == 1)
                        {
                            bindPriceTask(sender, itemToSell, tally, amount);
                        }
                        else
                        {
                            EconomyItemManager.addToMarket(sender, itemToSell, amount, Float.valueOf(args[1]));
                        }
                    }
                    catch (NumberFormatException ex)
                    {
                        sender.sendMessage(ChatColor.RED + "Invalid input!");
                    }
                }
                return true;
            }
            else if (matchesCommand(label, BUY_CMD))
            {
                if (label.toLowerCase().startsWith("my"))
                {
                    getLocalUUIDInventory(sender).open(sender);
                }

                else if (args.length == 0)
                {
                    getGlobalMaterialInventory(sender).open(sender);
                }
                else
                {
                    String firstArg = args[0];
                    if (args.length == 1)
                    {
                        if ("players".startsWith(firstArg.toLowerCase()))
                        {
                            getGlobalUUIDInventory(sender).open(sender);
                            return true;
                        }

                        for (Material mat : Material.values())
                        {
                            if (mat.name().startsWith(firstArg.toUpperCase()) && !mat.equals(Material.AIR))
                            {
                                getLocalMaterialInventory(mat, sender).open(sender);
                                return true;
                            }
                        }
                    }

                    getLocalUUIDInventory(firstArg, sender).open(sender);
                }
                return true;
            }
            else if (matchesCommand(label, CONSOLIDATE_COMMAND))
            {
                new Verifier.BooleanVerifier(sender, ChatColor.YELLOW + "Consolidate your items? All item durabilities will be averaged. Enchantments and custom names will be preserved.")
                {
                    @Override
                    public void performYesAction()
                    {
                        InventoryManager.consolidateItems(sender);
                    }

                    @Override
                    public void performNoAction()
                    {
                        sender.sendMessage(ChatColor.GREEN + "Consolidation cancelled");
                    }
                };
                return true;
            }
        }
        if (allowsWarps())
        {
            if (matchesCommand(label, WARP_COMMAND))
            {
                switch (args.length)
                {
                    case 2:
                        final String createOrDelete = args[0].toLowerCase();
                        final String warpToEditName = args[1];
                        if (Warp.warpCreateVariations.contains(createOrDelete))
                        {
                            Warp.createWarp(sender, warpToEditName);
                        }
                        else if (Warp.warpRemoveVariations.contains(createOrDelete))
                        {
                            Warp.removeWarp(sender, warpToEditName);
                        }
                        else
                        {
                            return false;
                        }
                        break;
                    case 1:
                        final String warpName = args[0];
                        final String lowerWarpName = warpName.toLowerCase();
                        if (lowerWarpName.equals("help"))
                        {
                            return false;
                        }
                        if (lowerWarpName.equals("list"))
                        {
                            Warp.getWarps(sender, true);
                        }
                        else
                        {
                            for (Warp warp : Warp.getWarps(sender, false))
                            {
                                if (warp.getName().toLowerCase().equals(lowerWarpName))
                                {
                                    sender.teleport(warp);
                                    return true;
                                }
                            }
                            sender.sendMessage(ChatColor.RED + "Warp " + warpName + " not found!");
                        }
                        break;
                    case 0:
                        Warp.getWarps(sender, true);
                        break;
                    default:
                        return false;
                }
                return true;
            }
        }
        return false;
    }

    void handleJoin(Player joiner, boolean cameFromNullWorld, World worldTo, boolean fromSpectation)
    {

        Song.stopPlayingSong(joiner);
        if (hasWorldScoreboard(worldTo))
        {
            setWorldScoreboardVisible(joiner, true);
        }

//        if (cameFromNullWorld)
//        {
        forceTp(joiner, PlayerStateRecorder.getLastLocationInWorld(joiner, worldTo));
//        }

        if (canPlayJoinLeaveMessages(worldTo))
        {
            final String joinMessage = fromSpectation ? getSpectatorJoinMessage(joiner, worldTo) : getNormalJoinMessage(joiner, worldTo);
            final String origWelcomeMessage = getWelcomeMessage(joiner, worldTo);
            final String welcomeMessage = origWelcomeMessage == null || origWelcomeMessage.length() == 0 ? joinMessage : origWelcomeMessage;

            final boolean messageIsOkay = joinMessage != null && joinMessage.length() > 0;

            if (messageIsOkay)
            {
                for (Player player : worldTo.getPlayers())
                {
                    player.sendMessage(player == joiner ? welcomeMessage : joinMessage);
                }
            }
            if (!isInSpectatorMode(joiner))
            {
                final int amplifier = 3;
                for (Player nativePlayer : worldTo.getPlayers())
                {
                    if (joiner != nativePlayer && SmashEntityTracker.isAfk(nativePlayer) && (messageIsOkay || hasSpecialPermissions(nativePlayer)))
                    {
                        for (int i = 0; i < amplifier; i++)
                        {
                            nativePlayer.playSound(nativePlayer.getLocation(), Sound.BLOCK_NOTE_PLING, 1, 1);
                        }
                    }
                }
            }
        }
    }

    void handleLeave(Player leaver, World w, boolean goingToNullWorld, boolean fromSpectation)
    {
        removePersonalScoreboard(leaver, w);
        if (canPlayJoinLeaveMessages(w))
        {
            final String leaveMessage = fromSpectation ? getSpectatorLeaveMessage(leaver, w) : getNormalLeaveMessage(leaver, w);

            final boolean messageIsOkay = leaveMessage != null && leaveMessage.length() > 0;

            if (messageIsOkay)
            {
                for (Player player : w.getPlayers())
                {
                    if (player != leaver)
                    {
                        player.sendMessage(leaveMessage);
                    }
                }
            }
        }
    }

    final World createWorld(CommandSender sender, boolean joinOnCreate)
    {
        World newWorld = createWorld();
        if (newWorld == null)
        {
            if (getWorlds().size() >= getMaxWorlds())
            {
                sender.sendMessage(ChatColor.RED + "The limit of " + getMaxWorlds() + " has already been reached!");
            }
            else
            {
                sender.sendMessage(ChatColor.RED + "Unable to create world for some reason.");
            }
        }
        else if (sender instanceof Player && joinOnCreate)
        {
            sendPlayerToWorld((Player)sender, newWorld, false, false);
        }
        else
        {
            sender.sendMessage(ChatColor.GREEN + "Created " + getDisplayName(newWorld) + ChatColor.GREEN + "!");
        }
        return newWorld;
    }

    /**
     * Function: createSmashWorld
     * Purpose: To create a brand new smash world of a given colorlessName given the colorlessName of a Plugin world
     *
     * @return - The new world
     */
    public final World createWorld()
    {
        if (!canCreateWorlds())
        {
            return null;
        }

        int worldNumber = WorldType.LOWEST_WORLD_NUM;
        for (; worldNumber < getMaxWorlds() + LOWEST_WORLD_NUM; worldNumber++)
        {
            if (!worldNumberIsTaken(worldNumber))
            {
                break;
            }
        }

        final String name = getWorldName(worldNumber);
        final World result = worldNumber >= maxWorldsOfType + LOWEST_WORLD_NUM ? null : createWorld(name);
        if (result == null)
        {
            sendErrorMessage("Error! Couldn't make world " + name);
        }
        return result;
    }

    public World createWorld(String name)
    {
        File worldFolder = getWorldFolder(name);
        boolean preExisting = worldFolder.exists();
        if (preExisting)
        {
            World createdAlreadyWorld = Bukkit.getWorld(name);
            if (createdAlreadyWorld != null)
            {
                return createdAlreadyWorld;
            }
        }
        else if (!worldFolder.mkdir())
        {
            sendErrorMessage("Error! Unable to create directory for " + name);
            return null;
        }

        try
        {
            if (!preExisting)
            {
                final Random r = new Random();
                File[] listOfTemplates = getSourceWorlds();
                if (!hasGenerator() || listOfTemplates.length > 0 && r.nextFloat() < 0.5) //using template
                {
                    if (listOfTemplates.length == 0)
                    {
                        sendErrorMessage("Error! Couldn't find any templates when trying to create " + name);
                        return null;
                    }

                    File sourceWorldFolder = listOfTemplates[r.nextInt(listOfTemplates.length)];

                    if (!sourceWorldFolder.exists() || !sourceWorldFolder.isDirectory())
                    {
                        sendErrorMessage("Error! Could not find world folder when creating world " + name + " (not possible)");
                        return null;
                    }

                    FileUtils.copyDirectory(sourceWorldFolder, worldFolder);

                    File[] filesToDelete = worldFolder.listFiles(new FilenameFilter()
                    {
                        public boolean accept(File file, String s)
                        {
                            return s.equals("uid.dat") || s.startsWith("session");
                        }
                    });
                    if (filesToDelete != null)
                    {
                        for (File f : filesToDelete)
                        {
                            if (!f.delete())
                            {
                                sendErrorMessage("Error! Failed to delete " + f.getName() + " when creating " + worldFolder.getName() + "!");
                                return null;
                            }
                        }
                    }
                }
                else if (!checkBukkitYML(name))
                {
                    Bukkit.getConsoleSender().sendMessage(ChatColor.YELLOW + "You must restart the server to create " + name);
                    worldFolder.delete();
                    return null;
                }
            }

            World createdWorld = new WorldCreator(name).createWorld();
            if (createdWorld != null)
            {
                performLoadWorldAction(createdWorld, true);
            }
            return createdWorld;
        }
        catch (IOException e)
        {
            sendErrorMessage("Error! Unable to instantiate world " + name);
            e.printStackTrace();
        }
        return null;
    }

    public boolean sendPlayerToWorld(Player p, World w, boolean forcefully, boolean pretendTheyWerentAlreadyInOne)
    {
        World w1 = p.getWorld();
        if (!Bukkit.getWorlds().contains(w) || w == null || isBeingDeleted(w.getName()))
        {
            return false;
        }
        else if (w1.equals(w))
        {
            p.sendMessage(ChatColor.YELLOW + "You are already in " + getDisplayName(w) + ChatColor.YELLOW + "!");
            return false;
        }

        if (pretendTheyWerentAlreadyInOne && !playersNotInAWorld.contains(p))
        {
            playersNotInAWorld.add(p);
        }

        Location whereToGo = PlayerStateRecorder.getLastLocationInWorld(p, w);
        if (forcefully)
        {
            forceTp(p, whereToGo);
        }
        else
        {
            p.teleport(whereToGo);
        }

        return true;
    }

    protected abstract WorldStatus getWorldStatus(World w, CommandSender p);

    public boolean haveWeChecked(String w)
    {
        return checkedWorlds.containsKey(w);
    }

    //false if we had to add the lines
    public boolean checkBukkitYML(String worldName)
    {
        if (hasGenerator() && !haveWeChecked(worldName))
        {
            File configFile = getBukkitYMLFile();

            try
            {
                Yaml yaml = new Yaml();
                FileInputStream stream = new FileInputStream(configFile);
                Map map = (Map)yaml.load(stream);
                stream.close();

                final String generatorPluginName = generator;
                if (!map.containsKey("worlds"))
                {
                    Map<String, Object> worldnameMap = new HashMap<String, Object>();
                    Map<String, String> generatorMap= new HashMap<String, String>();
                    generatorMap.put("generator", generatorPluginName);
                    worldnameMap.put(worldName, generatorMap);

                    map.put("worlds", worldnameMap);
                    checkedWorlds.put(worldName, false);
                }
                else
                {
                    Map<String, Object> worldsMap = (Map<String, Object>)map.get("worlds");
                    if (!worldsMap.containsKey(worldName))
                    {
                        Map<String, String> generatorMap = new HashMap<String, String>();
                        generatorMap.put("generator", generatorPluginName);

                        worldsMap.put(worldName, generatorMap);
                        checkedWorlds.put(worldName, false);
                    }
                    else
                    {
                        Map<String, Object> worldnameMap = (Map<String, Object>)worldsMap.get(worldName);
                        if (!worldnameMap.containsKey("generator"))
                        {
                            worldnameMap.put("generator", generatorPluginName);
                            checkedWorlds.put(worldName, false);
                        }
                    }
                }
                if (checkedWorlds.containsKey(worldName) && !checkedWorlds.get(worldName))
                {
                    PrintWriter writer = new PrintWriter(configFile);
                    yaml.dump(map, writer);
                    writer.close();
                }
//                DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
//                DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
//                Document xmlObject = dBuilder.parse(configFile);
//
//                //optional, but recommended
//                //read this - http://stackoverflow.com/questions/13786607/normalization-in-dom-parsing-with-java-how-does-it-work
//                xmlObject.getDocumentElement().normalize();
//
//
//                NodeList l = xmlObject.getElementsByTagName("world");
//                if (l.getPowerOf2() == 0)
//                {
//                    xmlObject.insertBefore(xmlObject, xmlObject.createTextNode("world"));
//                }


//                Scanner s = new Scanner(configFile);
//                final String[] dataLines = getDataLinesForBukkitYML(w);
//                final List<String> lines = new ArrayList<String>();
//                String deepString = "";
//                int spaces = 0;
//                int j = 0;
//                int lineNumber = 0;
//                boolean missing = false;
//                while (s.hasNextLine())
//                {
//                    String line = s.nextLine();
//                    if (j == 0)
//                    {
//                        if (line.length() > 0 && line.charAt(0) != '#')
//                        {
//                            j = lineNumber;
//                        }
//                        lineNumber++;
//                    }
//                    if (spaces == 0)
//                    {
//                        while (spaces < line.length() && line.charAt(spaces) == ' ')
//                        {
//                            spaces++;
//                        }
//                        if (line.length() == spaces)
//                        {
//                            spaces = 0;
//                        }
//                    }
//                    lines.add(line);
//                }
//                s.close();
//
//                for (int i = 0; i < dataLines.length; i++)
//                {
//                    if (!missing)
//                    {
//                        boolean contains = false;
//                        for (; j < lines.size() ; j++)
//                        {
//                            String line = lines.get(j);
//                            if (i != 0 && (line.length() <= deepString.length() || !line.startsWith(deepString) || line.charAt(deepString.length()) == ' '))
//                            {
//                                j--;
//                                break;
//                            }
//                            if (line.equals(deepString + dataLines[i]))
//                            {
//                                contains = true;
//                                j++;
//                                break;
//                            }
//                        }
//                        if (!contains)
//                        {
//                            missing = true;
//                        }
//                    }
//                    if (missing)
//                    {
//                        lines.add(j++, deepString + dataLines[i]);
//                    }
//                    for (int k = 0; k < spaces; k++)
//                    {
//                        deepString += " ";
//                    }
//                }
//
//                if (missing)
//                {
//                    PrintWriter writer = new PrintWriter(configFile);
//                    for (String line : lines)
//                    {
//                        writer.println(line);
//                    }
//                    writer.close();
//                    checkedWorlds.put(w, false);
//                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
                checkedWorlds.put(worldName, false);
            }

            if (!checkedWorlds.containsKey(worldName))
            {
                checkedWorlds.put(worldName, true);
            }
        }
        return checkedWorlds.get(worldName);
    }

    protected void setIsBeingDeleted(String worldName, boolean beingDeleted)
    {
        boolean alreadyBeingDeleted = isBeingDeleted(worldName);
        if (alreadyBeingDeleted && !beingDeleted)
        {
            worldsBeingDeleted.remove(worldName);
        }
        else if (!alreadyBeingDeleted && beingDeleted)
        {
            worldsBeingDeleted.add(worldName);
        }
    }

    boolean canBeJoined(World world, Player p)
    {
        return world != null && !worldIsLocked(world) && !isBeingDeleted(world.getName());
    }

    public static boolean worldCanChangeWeather(World w)
    {
        WorldType type = getWorldType(w);
        return !type.preventRain || type.worldsWhoCanChangeWeather.contains(w);
    }

    public static void allowWeatherChanges(World w, boolean allow)
    {
        WorldType type = getWorldType(w);
        if (type.preventRain)
        {
            boolean contains = type.worldsWhoCanChangeWeather.contains(w);
            if (!contains && allow)
            {
                type.worldsWhoCanChangeWeather.add(w);
            }
            else if (contains && !allow)
            {
                type.worldsWhoCanChangeWeather.remove(w);
            }
        }
    }

    boolean blockIsProtected(Block block, Player whoToTellIsProtected, boolean placeEvent)
    {
        if (whoToTellIsProtected != null)
        {
            if (hasSpecialPermissions(whoToTellIsProtected) && whoToTellIsProtected.getGameMode() == GameMode.CREATIVE)
            {
                return false;
            }
            if (isInSpectatorMode(whoToTellIsProtected))
            {
                whoToTellIsProtected.sendMessage(ChatColor.RED + "You can't affect the world while spectating!");
                return true;
            }
        }

        WorldGenerationTools.ChunkProtectionLevel protectionLevel = !hasGenerator() ? null : WorldGenerationTools.getChunkProtectionLevel(block);
        if (allowDestruction)
        {
            if (protectionLevel == null)
            {
                return false;
            }
            else
            {
                switch (protectionLevel)
                {
                    case UNPROTECTED:
                    {
                        return false;
                    }
                    case DISABLE_BLOCK_BREAKING:
                    {
                        switch (block.getType())
                        {
                            case SAPLING:
                                return false;
                        }
                        if (!placeEvent)
                        {
                            switch (block.getType())
                            {
                                case LONG_GRASS:
                                case LEAVES_2:
                                case LOG:
                                case LOG_2:
                                case YELLOW_FLOWER:
                                case RED_ROSE:
                                case DEAD_BUSH:
                                    return false;
                            }
                        }

                        if (whoToTellIsProtected != null)
                        {
                            whoToTellIsProtected.sendMessage(ChatColor.AQUA + "This area is protected by a strong magic!" +
                                    " Move farther from spawn to break blocks!");
                        }
                        break;
                    }
                    case DISABLE_EVERYTHING:
                    {
                        if (!placeEvent)
                        {
                            switch (block.getType())
                            {
                                case LEAVES:
                                    return false;
                            }
                        }

                        if (whoToTellIsProtected != null)
                        {
                            whoToTellIsProtected.sendMessage(ChatColor.AQUA + "This area is protected by a strong magic!");
                        }
                        break;
                    }
                }
            }
        }
        return true;
    }

    public final boolean allowsDestruction()
    {
        return allowDestruction;
    }

    public final List<String> getLoreForJoinItem(World w, Player p)
    {
        ArrayList<String> itemLores = new ArrayList<String>();
        int numberOfPlayers = getNonspectators(w).size();
        if (numberOfPlayers > 0)
        {
            itemLores.add(ChatColor.GREEN + "Players: " + numberOfPlayers);
        }

        WorldStatus status = getWorldStatus(w, p);
        if (status != null)
        {
            String statusMessage = status.getMessage();
            if (statusMessage != null && statusMessage.length() > 0)
            {
                itemLores.add("Status: " + statusMessage);
            }
        }
        List<String> specificLore = getTypeSpecificLoreForJoinItem(w, p);
        if (specificLore != null && specificLore.size() > 0)
        {
            itemLores.addAll(specificLore);
        }
        return itemLores;
    }

    List<String> getTypeSpecificLoreForJoinItem(World w, Player p)
    {
        return null;
    }

    /**
     * Function:
     * Purpose:
     *
     * @param w
     */
    public static boolean deleteWorld(final World w) //The part that needs to be done if you didn't just start the plugin
    {
        if (w != null)
        {
            List<World> existantWorlds = Bukkit.getWorlds();
            final WorldType type = getWorldType(w);
            boolean canDelete = (!type.isValid() || type.canDeleteWorlds()) && (existantWorlds.size() == 0 || existantWorlds.get(0) != w);
            if (canDelete)
            {
                type.setIsBeingDeleted(w.getName(), true);

                for (final Player p : w.getPlayers())
                {
                    Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
                    {
                        public String call()
                        {
                            if (getLastWorld(p).equals(w))
                            {
                                World fallback = type.isValid() ? type.getBestWorld(p) : getFallbackWorld();
                                if (fallback != null && !fallback.equals(w))
                                {
                                    forceTp(p, fallback.getSpawnLocation());
                                }
                                else
                                {
                                    initializeFallbackWorld();
                                    forceTp(p, getFallbackLocation());
                                }
                            }
                            else
                            {
                                sendPlayerToLastWorld(p, true);
                            }
                            return "";
                        }
                    });
                }

                type.unloadWorld(w);

                Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable()
                {
                    public void run()
                    {
                        deleteWorldFolder(getWorldFolder(w));
                    }
                }, 140);
                return true;
            }
        }
        else
        {
            sendErrorMessage("Error! Could not delete null world!");
        }
        return false;
    }

    public static void deleteWorldFolder(File f) //The part that needs to be done regardless of whether you just started the plugin
    {
        String worldName = f.getName();
        World w = Bukkit.getWorld(worldName);
        if (w != null)
        {
            Bukkit.unloadWorld(w, false);
        }
        FileManager.deleteFile(f);
        getWorldType(worldName).setIsBeingDeleted(f.getName(), false);
    }

    private void clearBukkitYML(String worldName)
    {
//        try
//        {
//            File f = getBukkitYMLFile();
//            FileInputStream stream = new FileInputStream(f);
//            Yaml yaml = new Yaml();
//            Map map = (Map)yaml.load(stream);
//            stream.close();
//
//            if (map.containsKey("worlds"))
//            {
//                Map<String, Object> worldsMap = (Map<String, Object>)map.get("worlds");
//                if (worldsMap.containsKey(worldName))
//                {
//                    Map<String, Object> worldnameMap = (Map<String, Object>)worldsMap.get(worldName);
//                    if (worldnameMap.containsKey("generator"))
//                    {
//                        worldnameMap.remove("generator");
//                    }
//                    worldsMap.remove(worldName);
//                    PrintWriter writer = new PrintWriter(f);
//                    yaml.dump(map, writer);
//                    writer.close();
//                }
//            }
//        }
//        catch (IOException e)
//        {
//            e.printStackTrace();
//        }


//        if (hasGenerator())
//        {
//            File configFile = getSpecificFile(DirectoryType.ROOT, "", "bukkit.yml");
//            try
//            {
//                Scanner s = new Scanner(configFile);
//                final String[] dataLines = getDataLinesForBukkitYML(w);
//
//                if (dataLines.length > 0)
//                {
//                    final List<String> lines = new ArrayList<String>();
//                    String deepString = "";
//                    int spaces = 0;
//                    int firstNonCommentIndex = 0;
//                    int lineNumber = 0;
//                    while (s.hasNextLine())
//                    {
//                        String line = s.nextLine();
//                        if (firstNonCommentIndex == 0)
//                        {
//                            if (line.length() > 0 && line.charAt(0) != '#')
//                            {
//                                firstNonCommentIndex = lineNumber;
//                            }
//                            lineNumber++;
//                        }
//                        if (spaces == 0)
//                        {
//                            while (spaces < line.length() && line.charAt(spaces) == ' ')
//                            {
//                                spaces++;
//                            }
//                            if (line.length() == spaces)
//                            {
//                                spaces = 0;
//                            }
//                        }
//                        lines.add(line);
//                    }
//
//                    for (int i = 0; i < dataLines.length; i++)
//                    {
//                        dataLines[i] = deepString + dataLines[i];
//                        for (int k = 0; k < spaces; k++)
//                        {
//                            deepString = deepString + " ";
//                        }
//                    }
//                    for (int i = lines.size() - 1; i >= firstNonCommentIndex; i--)
//                    {
//                        String line = lines.get(i);
//                        int dataLineIndex = dataLines.length - 1;
//                        boolean foundMatch = true;
//                        for (int k = i; k > i - dataLines.length && k >= firstNonCommentIndex; k--, dataLineIndex--)
//                        {
//                            if (!lines.get(k).equals(dataLines[dataLineIndex]))
//                            {
//                                foundMatch = false;
//                                break;
//                            }
//                        }
//                        if (foundMatch)
//                        {
//                            for (int k = i; k > i - dataLines.length; k--)
//                            {
//                                lines.remove(k);
//                            }
//                            break;
//                        }
//                    }
//
//                    PrintWriter writer = new PrintWriter(configFile);
//                    for (String line : lines)
//                    {
//                        writer.println(line);
//                    }
//                    writer.close();
//                }
//            }
//            catch (FileNotFoundException e)
//            {
//                e.printStackTrace();
//            }
//        }
    }

    public final Integer getTeleportDelaySeconds()
    {
        return teleportDelaySeconds;
    }

    private static File getBukkitYMLFile()
    {
        return FileManager.getSpecificFile("", "bukkit.yml");
    }

    private String[] getDataLinesForBukkitYML(String w)
    {
        return new String[] {"worlds:", w + ":","generator: " + getPlugin().getName()};
    }

    public String getMetaWorldGenerator()
    {
        return generator;
    }

    public boolean isFightingHunger()
    {
        return fightingHunger;
    }

    public GameMode getDefaultGamemode()
    {
        return defaultGamemode;
    }

    public boolean hasGenerator()
    {
        return generator != null;
    }

    public boolean isValid()
    {
        return valid;
    }

    public boolean isBeingDeleted(String name)
    {
        return worldsBeingDeleted.contains(name);
    }

    public boolean remembersPlayerStates(Player p)
    {
        return rememberPlayerState || hasSpecialPermissions(p);
    }

    private static class WorldCreatorItem extends GuiManager.GuiItem
    {
        private static final String WORLD_CREATION_ITEM_PREFIX = ChatColor.GRAY + "Create a ";
        private final WorldType typeToCreate;

        private static ItemStack getProperWorldCreationItem(ItemStack item)
        {
            if (item == null)
            {
                item = new ItemStack(Material.STONE_SWORD);
            }
            String currentName = !item.hasItemMeta() || !item.getItemMeta().hasDisplayName() ? "" : item.getItemMeta().getDisplayName();
            if (!ChatColor.stripColor(currentName).startsWith(ChatColor.stripColor(WORLD_CREATION_ITEM_PREFIX)))
            {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(WORLD_CREATION_ITEM_PREFIX + currentName);
                item.setItemMeta(meta);
            }
            return item;
        }

        public WorldCreatorItem(WorldType typeToCreate, ItemStack item)
        {
            super(getProperWorldCreationItem(item));
            this.typeToCreate = typeToCreate;
        }

        @Override
        public void performAction(Player clicker)
        {
            typeToCreate.createWorld(clicker, true);
        }

        public WorldType getType()
        {
            return typeToCreate;
        }
    }

    private static class WorldJoinItem extends GuiManager.GuiItem
    {
        private final World worldRepresented;

        private WorldJoinItem(World worldRepresented, Player p)
        {
            super(getWorldType(worldRepresented).getWorldRepresenter(worldRepresented, p));
            this.worldRepresented = worldRepresented;
        }

        private World getWorldRepresented()
        {
            return worldRepresented;
        }

        @Override
        public void performAction(Player clicker)
        {
            getWorldType(worldRepresented).sendPlayerToWorld(clicker, worldRepresented, false, false);
        }
    }

    private static class WorldGui extends GuiManager.GuiInventory
    {
        private final Player who;

        private WorldGui(Player p)
        {
            super(ChatColor.BLACK + "~ Game List ~");
            openWorldGuis.add(this);
            this.who = p;
            for (final WorldType type : WorldType.values())
            {
                if (type.isValid())
                {
                    for (final World world : type.getWorlds())
                    {
                        addItem(world);
                    }
                }
            }
            for (final WorldType type : WorldType.values())
            {
                if (type.isValid())
                {
                    addAll(type.getWorldCreatorItems(), false);
                }
            }
        }

        @Override
        public void handleInventoryClose(Player playerWhoClosedIt)
        {
            super.handleInventoryClose(playerWhoClosedIt);
            openWorldGuis.remove(this);
        }

        @Override
        public void addItem(GuiManager.GuiItem item, boolean refreshNeededGuis, Integer index)
        {
            if (item instanceof WorldJoinItem)
            {
                World world = ((WorldJoinItem)item).getWorldRepresented();
                WorldType type = getWorldType(world);
                if (type.canBeJoined(world, who))
                {
                    super.addItem(item, refreshNeededGuis, index);
                }
            }
            else if (item instanceof WorldCreatorItem)
            {
                WorldType type = ((WorldCreatorItem)item).getType();
                if (type.canCreateWorlds() && hasSpecialPermissions(who))
                {
                    super.addItem(item, refreshNeededGuis, index);
                }
            }
            else
            {
                super.addItem(item, refreshNeededGuis, index);
            }
        }

        private void addItem(World worldChanged)
        {
            if (worldChanged != null)
            {
                addItem(new WorldJoinItem(worldChanged, who), true);
            }
        }

        private void removeItem(World worldChanged)
        {
            if (worldChanged != null)
            {
                for (GuiManager.GuiItem item : getContents())
                {
                    if (item instanceof WorldJoinItem)
                    {
                        WorldJoinItem worldJoinItem = (WorldJoinItem)item;
                        if (worldJoinItem.getWorldRepresented() == worldChanged)
                        {
                            removeItem(worldJoinItem, false);
                        }
                    }
                }
            }
        }
    }

    private final WorldCreatorItem[] getWorldCreatorItems()
    {
        return worldCreatorItems;
    }

    abstract WorldCreatorItem[] generateWorldCreatorItems();

    public static boolean nonFriendliesAreNearby(Player p)
    {
        return nonFriendliesAreNearby(p.getLocation(), p);
    }

    private static boolean nonFriendliesAreNearby(Location l, Player player)
    {
        if (l.getWorld() == player.getWorld())
        {
            int lX = l.getBlockX();
            int lZ = l.getBlockZ();
            int lY = l.getBlockY();
            for (Player nemesis : l.getWorld().getPlayers())
            {
                if (!playersAreFriendly(nemesis, player))
                {
                    final Vector locationOfNemesis = nemesis.getLocation().toVector();
                    final int nX = locationOfNemesis.getBlockX();
                    final int nY = locationOfNemesis.getBlockY();
                    final int nZ = locationOfNemesis.getBlockZ();
                    final int dXSquared = (nX - lX)*(nX - lX);
                    final int dYSquared = (nY - lY)*(nY - lY);
                    final int dZSquared = (nZ - lZ)*(nZ - lZ);
                    if (dXSquared + dYSquared + dZSquared < PLAYER_DISTANCE_CONSIDERED_CLOSE_SQUARED)
                    {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    public boolean playersAreTeamed(Player player1, Player player2)
    {
        return false;
    }

    private static final boolean playersAreFriendly(Player nemesis, Player player)
    {
        if (nemesis == player)
        {
            return true;
        }
        return getWorldType(nemesis.getWorld()).playersAreTeamed(nemesis, player);
    }

    boolean isAllowedToTeleport(Player p, Location location)
    {
        return true;
    }

    public final boolean isAllowedToSellItem(Player p, ItemStack item, boolean tellPlayer)
    {
        if (hasAnEconomy)
        {
            SpecialItem specialItem = getSpecialItem(p, item);
            if (specialItem == null || specialItem.canBeSold())
            {
                return true;
            }
            else
            {
                p.sendMessage(ChatColor.RED + InventoryManager.getItemName(item) + ChatColor.RED + " cannot be sold.");
                return false;
            }
        }
        if (tellPlayer)
        {
            p.sendMessage(ChatColor.RED + "You can't sell stuff in this world.");
        }
        return false;
    }

    public static void forceTp(Player p, Location l)
    {
        if (!playersWhoHaveBeenAllowedToTeleport.contains(p))
        {
            playersWhoHaveBeenAllowedToTeleport.add(p);
        }
        p.teleport(l);
        playersWhoHaveBeenAllowedToTeleport.remove(p);
    }

    public static void forceTp(Player p, Player whoToTpTo)
    {
        forceTp(p, whoToTpTo.getLocation());
    }

    public static void veryForcefullyTp(final Player player, final Location location)
    {
        forceTp(player, location);
        Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
        {
            public String call()
            {
                if (player.getLocation().distanceSquared(location) > 1)
                {
                    veryForcefullyTp(player, location);
                }
                else
                {
                    Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (player.getLocation().distanceSquared(location) > 1)
                            {
                                veryForcefullyTp(player, location);
                            }
                        }
                    }, 10);
                }
                return "";
            }
        });
    }

    public void repairItem(final Player p, final ItemStack item)
    {
        if (!p.getItemInHand().equals(item))
        {
            item.setDurability((short)-1);///(short)-1);
        }
        else
        {
//        HashMap<ItemStack, Integer> itemRepairTasks = repairTasks.get(p);
//        if (itemRepairTasks == null)
//        {
//            itemRepairTasks = new HashMap<ItemStack, Integer>();
//            repairTasks.put(p, itemRepairTasks);
//        }
//        if (!itemRepairTasks.containsKey(item))
//        {
//            final HashMap<ItemStack, Integer> finalItemRepairTasks = itemRepairTasks;
//            itemRepairTasks.put(item,
            Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable()
            {
                public void run()
                {
                    if (!p.getItemInHand().equals(item) && InventoryManager.hasItem(p, item) && !isDead(p))
                    {
                        item.setDurability((short)-1);
                    }
//                  finalItemRepairTasks.remove(item);
                }
            }, 60);
//            );
//        }
        }
    }

    private static File getLastDeathLocationFile(String whoDied, World whereDied, boolean forceValid)
    {
        return FileManager.getPlayerFile(whoDied, whereDied, "", "DeathLocations.json", forceValid);
    }

    public static Location getLastDeathLocation(String player, World world)
    {
        File deathLogfile = getLastDeathLocationFile(player, world, false);
        if (deathLogfile.exists())
        {
            return FileManager.deserializeLocation(world, FileManager.getData(deathLogfile, UUIDFetcher.getUUID(player)));
        }
        return null;
    }

    public static Location getLastDeathLocation(Player player, World world)
    {
        return getLastDeathLocation(player.getName(), world);
    }

    public static void setLastDeathLocation(Player player, Location location)
    {
        if (location != null)
        {
            File deathLogFile = getLastDeathLocationFile(player.getName(), location.getWorld(), true);
            FileManager.putData(deathLogFile, UUIDFetcher.getUUID(player), FileManager.serializeLocation(location));
        }
    }
//    public void cancelRepairs(Player p)
//    {
//        HashMap<ItemStack, Integer> repairTaskMap = repairTasks.get(p);
//        if (repairTaskMap != null)
//        {
//            for (int task : repairTaskMap.values())
//            {
//                Bukkit.getScheduler().cancelTask(task);
//            }
//            repairTasks.remove(p);
//        }
//    }
//
//    public World getWorldRepresented(ItemStack item)
//    {
//        if (isWorldCreator(item))
//        {
//            World representedWorld = type.getWorldRepresented(item);
//            if (representedWorld != null)
//            {
//                return representedWorld;
//
//                Integer tourneyLevel = null;
//                if (type.hasTourneyLevel())
//                {
//                    if (!item.hasItemMeta() || !item.getItemMeta().hasLore() || item.getItemMeta().getLore().size() < 1)
//                    {
//                        sendErrorMessage("Error! Could not get a new world from item " + item.getItemMeta().getDisplayName());
//                        return null;
//                    }
//                    else
//                    {
//                        String lore = ChatColor.stripColor(item.getItemMeta().getLore().get(0));
//                        String roundNumberString = "";
//                        for (int i = 0; i < lore.length(); i++)
//                        {
//                            char c = lore.charAt(i);
//                            if (Character.isDigit(c))
//                            {
//                                roundNumberString += c;
//                            }
//                            else
//                            {
//                                break;
//                            }
//                        }
//                        if (roundNumberString.equals(""))
//                        {
//                            sendErrorMessage("Error! Could not get the round number from item " + item.getItemMeta().getDisplayName());
//                            return null;
//                        }
//                        tourneyLevel = Integer.valueOf(roundNumberString);
//                    }
//                }
//                return createWorld(tourneyLevel);
//            }
//            return null;
//        }
//
//        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
//        int worldNumber = -1;
//        WorldType worldType = WorldType.INVALID;
//        for (WorldType type : WorldType.values())
//        {
//            if (type.isValid() && name.contains(ChatColor.stripColor(type.getDisplayIndicator())))
//            {
//                if (type.getMaxWorlds() > 1)
//                {
//                    int firstIndex = name.length() - 1;
//                    while (Character.isDigit(name.charAt(firstIndex - 1)))
//                    {
//                        firstIndex--;
//                    }
//                    String numberOfWorldString = name.substring(firstIndex, name.length());
//
//                    worldNumber = Integer.valueOf(numberOfWorldString);
//                }
//                else
//                {
//                    worldNumber = WorldType.LOWEST_WORLD_NUM;
//                }
//
//                worldType = type;
//                break;
//            }
//        }
//
//        if (worldType.isValid())
//        {
//            World w = getWorld(worldType, worldNumber);
//            if (w == null)
//            {
//                sendErrorMessage("Error! The world that that item represents could not be found!");
//                return getFallbackWorld();
//            }
//            return w;
//        }
//
//        return null;
//    }
}