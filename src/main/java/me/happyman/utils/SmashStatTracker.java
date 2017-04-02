package me.happyman.utils;

import me.happyman.source;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class SmashStatTracker implements CommandExecutor
{
    private static source plugin;
    private static SmashWorldManager worldManager;

    private static final String FREE_GAMES_PLAYED = "Free games played";
    private static final String TOURNEY_GAMES_PLAYED = "Tournament games played";
    private static final String POINT_BASED_GAMES_PLAYED = "Point-based game played";
    public static final String TOURNEY_ROUNDS_PLAYED = "Tournament rounds played"; //This includes final rounds
    public static final String POINTS_ACCUMULATED = "Points accumulated"; //This is the addition of end game scores that the player has gotten
    private static final String TOURNEY_WINS = "Tournament wins";
    private static final String FREE_WINS = "Freeplay wins";
    private static final String TOURNEY_LEVEL_DATANAME = "Smash Tourney Level";
    private static final String TOURNEY_EXPIRATION_DATANAME = "Smash Expiration Time";
    public static final String KO_DEALT_SCORE = "KO's Dealt";
    public static final String KO_RECEIVED_SCORE = "KO's Received";
    public static final String FALLEN_OUT_SCORE = "Times fallen out";
    private static final String ELO_SCORE_DATANAME = "Smash Elo";
    private static final float ELO_CHANGE_RATE_SPEED = (float)0.02;
    private static final float ELO_FARMABILITY_MOD = 0.2F; //min = mod/(1+mod)
    private static final float ELO_CHANGE_AT_0_ELO = 0.5F;

    // killerEloIncrease = ELO_CHANGE_AT_0_ELO * ((float)Math.exp(-difference* ELO_CHANGE_RATE_SPEED)+ ELO_FARMABILITY_MOD)/(1+ ELO_FARMABILITY_MOD);
    private static final String SMASH_STAT_CMD = "statistics";

    private static HashMap<String, Float> originalScores;

    public SmashStatTracker(SmashWorldManager manager, source plugIn)
    {
        worldManager = manager;
        plugin = plugIn;

        originalScores = new HashMap<String, Float>();
        plugin.setExecutor(SMASH_STAT_CMD, this);
    }

    public static int getTourneyLevel(Player p)
    {
        int result;
        long expiration;
        try
        {
            result = Integer.valueOf(plugin.getPluginDatum(p, TOURNEY_LEVEL_DATANAME));
            expiration = Long.valueOf(plugin.getPluginDatum(p, TOURNEY_EXPIRATION_DATANAME));
            if (plugin.getMinute() > expiration && !SmashWorldManager.isTourneyWorld(p.getWorld()))
            {
                p.sendMessage(ChatColor.YELLOW + "Your Tournament level expired because you didn't complete a Tourney game in over " + SmashWorldManager.EXPIRATION_MINUTES + " minutes.");
                return resetTourneyLevel(p);
            }
        }
        catch (NumberFormatException e)
        {
            return resetTourneyLevel(p);
        }
        return result;
    }

    //Level is 0-indexed
    public static void setTourneyLevel(Player p, int level)
    {
        if (level > SmashWorldManager.HIGHEST_TOURNEY_LEVEL || level < 1)
        {
            Bukkit.getConsoleSender().sendMessage(plugin.loggerPrefix() + ChatColor.RED + "Error! Could not set tourney level to " + level);
        }
        else if (level > 1)
        {
            plugin.putPluginDatum(p, TOURNEY_LEVEL_DATANAME, level);
            plugin.putPluginDatum(p, TOURNEY_EXPIRATION_DATANAME, plugin.getMinute() + SmashWorldManager.EXPIRATION_MINUTES);
        }
        else
        {
            resetTourneyLevel(p);
        }
    }

    public static void incrementTourneyLevel(Player p)
    {
        if (getTourneyLevel(p) == SmashWorldManager.HIGHEST_TOURNEY_LEVEL)
        {
            resetTourneyLevel(p);
            incrementTourneyWins(p);
        }
        else if (getTourneyLevel(p) >= 1 && getTourneyLevel(p) < SmashWorldManager.HIGHEST_TOURNEY_LEVEL)
        {
            setTourneyLevel(p, getTourneyLevel(p) + 1);
        }
        else
        {
            Bukkit.getConsoleSender().sendMessage(plugin.loggerPrefix() + ChatColor.RED + "Error! Could not increment player " + p.getName() + "'s tourney level!");
        }
    }

    public static void incrementTourneyLevel(List<Player> players)
    {
        for (Player p : players)
        {
            incrementTourneyLevel(p);
        }
    }

    public static int resetTourneyLevel(String p)
    {
        plugin.putPluginDatum(p, TOURNEY_LEVEL_DATANAME, 1);
        plugin.putPluginDatum(p, TOURNEY_EXPIRATION_DATANAME, (long)1e15);
        return 1;
    }

    public static int resetTourneyLevel(Player p)
    {
        return resetTourneyLevel(p.getName());
    }

    //***************
    public static int incrementFreeGamesPlayed(Player p)
    {
        incrementPointBasedGamesPlayed(p);
        return plugin.incrementPluginStatistic(p, FREE_GAMES_PLAYED);
    }

    public static void incrementFreeGamesPlayed(List<Player> players)
    {
        for (Player p : players)
        {
            incrementFreeGamesPlayed(p);
        }
    }

    public static int getFreeGamesPlayed(String p)
    {
        return plugin.getPluginStatistic(p, FREE_GAMES_PLAYED);
    }

    public static int getFreeGamesPlayed(Player p)
    {
        return getFreeGamesPlayed(p.getName());
    }

    //***************
    public static int incrementFreeGamesWon(Player p)
    {
        return plugin.incrementPluginStatistic(p, FREE_WINS);
    }

    public static void incrementFreeGamesWon(List<Player> players)
    {
        for (Player p: players)
        {
            incrementFreeGamesWon(p);
        }
    }

    public static int getFreeGamesWon(Player p)
    {
        return getFreeGamesWon(p.getName());
    }

    public static int getFreeGamesWon(String p)
    {
        return plugin.getPluginStatistic(p, FREE_WINS);
    }

    //***************
    public static int incrementTourneyGamesPlayed(Player p)
    {
        return plugin.incrementPluginStatistic(p, TOURNEY_GAMES_PLAYED);
    }

    private static void incrementPointBasedGamesPlayed(Player p)
    {
        if (!SmashWorldManager.isDeathMatchWorld(p.getWorld()))
        {
            plugin.incrementPluginStatistic(p, POINT_BASED_GAMES_PLAYED);
        }
    }

    public static void incrementTourneyGamesPlayed(List<Player> players)
    {
        for (Player p : players)
        {
            incrementTourneyGamesPlayed(p);
        }
    }

    public static int getTourneyGamesPlayed(Player p)
    {
        return getTourneyGamesPlayed(p.getName());
    }

    public static int getTourneyGamesPlayed(String p)
    {
        return plugin.getPluginStatistic(p, TOURNEY_GAMES_PLAYED);
    }

    private int getPointBasedGamePlayed(String p)
    {
        int games = plugin.getPluginStatistic(p, POINT_BASED_GAMES_PLAYED);
        if (games == 0)
        {
            games = getTotalGamesPlayed(p);
            plugin.putPluginDatum(p, POINT_BASED_GAMES_PLAYED, games);
        }
        return games;
    }

    //***************
    private static int incrementTourneyWins(Player p)
    {
        return plugin.incrementPluginStatistic(p, TOURNEY_WINS);
    }

    private static void incrementTourneyWins(List<Player> players)
    {
        for (Player p : players)
        {
            incrementTourneyWins(p);
        }
    }

    public static int getTourneyGamesWon(Player p)
    {
        return getTourneyGamesWon(p.getName());
    }

    public static int getTourneyGamesWon(String p)
    {
        return plugin.getPluginStatistic(p, TOURNEY_WINS);
    }
    //****************
    public static void incrementTourneyRoundsPlayed(Player p)
    {
        incrementPointBasedGamesPlayed(p);
        plugin.incrementPluginStatistic(p, TOURNEY_ROUNDS_PLAYED);
    }

    public static void incrementTourneyRoundsPlayed(List<Player> players)
    {
        for (Player p : players)
        {
            incrementTourneyRoundsPlayed(p);
        }
    }
    //****************
    public static void addScoreToTotalPoints(World w)
    {
        if (SmashScoreboardManager.hasScoreboard(w))
        {
            for (String s : SmashScoreboardManager.getScoreboardEntries(w))
            {
                if (Bukkit.getPlayer(s) != null)
                {
                    addScoreToTotalPoints(Bukkit.getPlayer(s));
                }
            }
        }
    }

    public static void addScoreToTotalPoints(Player p)
    {
        World w = p.getWorld();
        if (SmashWorldManager.isSmashWorld(w) && SmashWorldManager.gameHasStarted(w) && !SmashWorldManager.isDeathMatchWorld(w) && SmashScoreboardManager.getPlayerScoreValue(p) != null)
        {
            int oldScore = plugin.getPluginStatistic(p, POINTS_ACCUMULATED);
            plugin.putPluginDatum(p, POINTS_ACCUMULATED, oldScore + SmashScoreboardManager.getPlayerScoreValue(p));
        }
    }

    //****************

    public static void rememberOldElo(String p)
    {
        originalScores.put(p, getEloScore(p));
    }

    public static float getOldElo(String p)
    {
        if (originalScores.containsKey(p))
        {
            return originalScores.get(p);
        }
        plugin.sendErrorMessage("Error! Tried to get the old elo of a player who we didn't know how much Elo they had before!");
        return getEloScore(p);
    }

    public static float getAndForgetOldElo(String p)
    {
        float oldElo = getOldElo(p);
        forgetOldElo(p);
        return oldElo;
    }

    public static void forgetOldElo(String p)
    {
        if (originalScores.containsKey(p))
        {
            originalScores.remove(p);
        }
    }

    public static float getEloScore(Player p)
    {
        return getEloScore(p.getName());
    }

    public static float getEloScore(String p)
    {
        float score;
        try
        {
            score = Float.valueOf(plugin.getPluginDatum(p, ELO_SCORE_DATANAME));
            if (score == 0)
            {
                throw new NumberFormatException();
            }
        }
        catch (NumberFormatException e)
        {
            score = 1000;
            plugin.putPluginDatum(p, ELO_SCORE_DATANAME, score);
        }
        return score;
    }

    public static String eloChangeString(int eloIncrease)
    {
        String sign;
        if (eloIncrease >= 0)
        {
            sign = ChatColor.GREEN + "+";
        }
        else
        {
            sign = ChatColor.RED + "";
        }
        return sign + String.format("%1$d", eloIncrease);
    }

    public static void performEloChange(String killer, String deadPlayer)
    {
        performEloChange(killer, deadPlayer, true);
    }

    public static void performEloChange(String killer, final String deadPlayer, boolean showDeadPlayer)
    {
        performEloChange(killer, deadPlayer, 1, showDeadPlayer);
    }

    private static float calculateKillerEloIncrease(float killerElo, float deadElo)
    {
        float difference = killerElo - deadElo;

        float killerEloIncrease;
        if (difference > 0)
        {
            killerEloIncrease = ELO_CHANGE_AT_0_ELO * ((float)Math.exp(-difference* ELO_CHANGE_RATE_SPEED)+ ELO_FARMABILITY_MOD)/(1+ ELO_FARMABILITY_MOD);
        }
        else
        {
            killerEloIncrease = ELO_CHANGE_AT_0_ELO * ((-difference* ELO_CHANGE_RATE_SPEED + 1)+ ELO_FARMABILITY_MOD)/(1+ ELO_FARMABILITY_MOD);
        }
        //Bukkit.getPlayer("HappyMan").sendMessage("" + killerEloIncrease);
        return killerEloIncrease;
    }

    private static void performEloChange(String killer, final String deadPlayer, float modification, boolean showDeadPlayer)
    {
        if (SmashStatTracker.hasAdvancedStatistics(killer) && SmashStatTracker.hasAdvancedStatistics(deadPlayer))
        {
            //Bukkit.getPlayer("HappyMan").sendMessage("has advanced stat");
            if (!killer.equals(deadPlayer))
            {
               //Bukkit.getPlayer("HappyMan").sendMessage("not equal");
                float killerElo = getEloScore(killer);
                float deadElo = getEloScore(deadPlayer);
                float killerEloIncrease = calculateKillerEloIncrease(killerElo, deadElo)*modification;

                increaseElo(killer, killerElo, killerEloIncrease, true);
                increaseElo(deadPlayer, deadElo, -killerEloIncrease, showDeadPlayer);
            }
            else
            {
                plugin.sendErrorMessage("Error! Tried to perform an elo change for someone killing themselves!");
            }
        }
    }

    public static void performEloChange(String deadPlayer, World w, float modification)
    {
        if (SmashWorldManager.isSmashWorld(w))
        {
            List<String> playersInvolved = new ArrayList<String>();
            for (Player p : SmashWorldManager.getLivingPlayers(w))
            {
                playersInvolved.add(p.getName());
            }
            if (playersInvolved.contains(deadPlayer))
            {
                playersInvolved.remove(deadPlayer);
            }

            if (playersInvolved.size() > 0)
            {
                for (String p : playersInvolved)
                {
                    if (!deadPlayer.equals(p))
                    {
                        performEloChange(p, deadPlayer, modification/playersInvolved.size(), false);
                    }
                }
            }
        }
        //deadPlayer.sendMessage(eloChangeString(Math.round(totalEloChanges)) + " elo");
    }

    public static void performEloChange(String deadPlayer, World w)
    {
        performEloChange(deadPlayer, w, 1F);
        //deadPlayer.sendMessage(eloChangeString(Math.round(totalEloChanges)) + " elo");
    }

    public static void increaseElo(String p, float formerElo, float eloIncrease, boolean displayTheChange)
    {
        /*if (displayTheChange && Bukkit.getPlayer(p) != null)
        {
            //p.sendMessage(eloChangeString(Math.round(eloIncrease)) + " elo");
        }*/
        //Bukkit.getPlayer("HappyMan").sendMessage("elo");
        plugin.putPluginDatum(p, ELO_SCORE_DATANAME, (formerElo + eloIncrease));
    }


    //***************

    public static void displayHelpMessage(Player p)
    {
        p.sendMessage(ChatColor.YELLOW + "Commands:");
        p.sendMessage(ChatColor.GREEN + "/smash");
        p.sendMessage(ChatColor.GREEN + "/listworlds");
        p.sendMessage(ChatColor.GREEN + "/goto <world>");
        p.sendMessage(ChatColor.GREEN + "/leave");
        p.sendMessage(ChatColor.GREEN + "/stats [player]");
        p.sendMessage(ChatColor.GREEN + "/kit");
        p.sendMessage(ChatColor.GREEN + "/dm [yes|no] [lives]");
        p.sendMessage(ChatColor.GREEN + "/find [player]");
        p.sendMessage(ChatColor.GREEN + "/mode");
        p.sendMessage(ChatColor.GREEN + "/spectate");
        p.sendMessage(ChatColor.GREEN + "/ready");
        p.sendMessage(ChatColor.GREEN + "/votekick");
    }

    public static int getTourneyRoundsPlayed(String p)
    {
        return plugin.getPluginStatistic(p, TOURNEY_ROUNDS_PLAYED);
    }

    public static int getTourneyRoundsPlayed(Player p)
    {
        return getTourneyRoundsPlayed(p.getName());
    }

    public static int getTotalGamesPlayed(String p)
    {
        //Bukkit.getPlayer("HappyMan").sendMessage("" + getFreeGamesPlayed(p) + getTourneyRoundsPlayed(p));
        return getFreeGamesPlayed(p) + getTourneyRoundsPlayed(p);
    }

    public static int getTotalGamesPlayed(Player p)
    {
        return getTotalGamesPlayed(p.getName());
    }

    public static boolean hasAdvancedStatistics(String p)
    {
        return getTotalGamesPlayed(p) >= SmashWorldManager.MIN_FREE_GAMES_TO_JOIN_TOURNEYS;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (plugin.matchesCommand(label, SMASH_STAT_CMD))
        {
            String p = null;
            if (args.length == 0)
            {
                if (!(sender instanceof Player))
                {
                    plugin.sendErrorMessage("You are not a player!");
                }
                else
                {
                    p = ((Player)sender).getName();
                }
            }
            else
            {
                /*List<String> offlines = new ArrayList<String>();
                boolean foundPlayer = Bukkit.getPlayer(args[0]) != null;
                for (OfflinePlayer offline : Bukkit.getOfflinePlayers())
                {
                    offlines.add(offline.getName());
                }*/

                if (plugin.playerHasFile(args[0]))
                {
                    p = args[0];
                }
                else
                {
                    sender.sendMessage(ChatColor.YELLOW + "It seems that '" + args[0] + "' hasn't played on this server before!");
                    return true;
                }
            }

            List<String> statLines = new ArrayList<String>();
            String prefix = ChatColor.DARK_GRAY + " | ";
            statLines.add(prefix + ChatColor.AQUA + "Tournaments played: " + getTourneyGamesPlayed(p));
            statLines.add(prefix + ChatColor.AQUA + "Tournaments won: " + getTourneyGamesWon(p));
            statLines.add(prefix + ChatColor.GOLD + "Free Games played: " + getFreeGamesPlayed(p));
            statLines.add(prefix + ChatColor.GOLD + "Free Games won: " + getFreeGamesWon(p));
            statLines.add(prefix + ChatColor.YELLOW + "KO's Dealt/Received: " + plugin.getPluginStatistic(p, KO_DEALT_SCORE) + "/" + plugin.getPluginStatistic(p, KO_RECEIVED_SCORE));
            statLines.add(prefix + ChatColor.YELLOW + "Times fallen off the map: " + plugin.getPluginStatistic(p, FALLEN_OUT_SCORE));
            if (hasAdvancedStatistics(p))
            {
                statLines.add(prefix + ChatColor.GRAY + "Avg end-game score: " + String.format("%.2f", 1.0 * plugin.getPluginStatistic(p, POINTS_ACCUMULATED) / getPointBasedGamePlayed(p)));
                statLines.add(prefix + ChatColor.GREEN + "" + ChatColor.BOLD + "Elo" + ChatColor.RESET + "" + ChatColor.GREEN + ": " + Math.round(getEloScore(p)));
            }

            int longestLength = 0;
            for (String line : statLines)
            {
                if (line.length() > longestLength)
                {
                    longestLength = line.length();
                }
            }
            String firstText = "Stats for " + plugin.getCapitalName(p);
            int equalsLength = (int)Math.round(1.0*(longestLength - firstText.length())/2/1.6);
            String equalses = "";
            for (int i = 0; i < equalsLength; i++)
            {
                equalses += "=";
            }
            String firstLine = ChatColor.YELLOW + "" + ChatColor.BOLD + equalses + firstText + equalses;

            sender.sendMessage(firstLine);
            for (String line : statLines)
            {
                sender.sendMessage(line);
            }
            return true;
        }
        return false;
    }

}
