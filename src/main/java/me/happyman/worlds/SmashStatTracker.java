package me.happyman.worlds;

import me.happyman.Plugin;
import me.happyman.utils.FileManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static me.happyman.Plugin.*;
import static me.happyman.SpecialItems.SmashKitMgt.SmashKitManager.SMASH_KIT_CMD;
import static me.happyman.utils.FileManager.getGeneralPlayerFile;
import static me.happyman.worlds.MainListener.*;

public class SmashStatTracker
{
    public static final String FREE_GAMES_PLAYED = "Free games played";
    public static final String TOURNEY_GAMES_PLAYED = "Tournament games played";
    public static final String POINT_BASED_GAMES_PLAYED = "Point-based game played";
    public static final String TOURNEY_ROUNDS_PLAYED = "Tournament rounds played"; //This includes final rounds
    public static final String POINTS_ACCUMULATED_DATANAME = "Points accumulated"; //This is the addition of end game scores that the player has gotten
    public static final String TOURNEY_WINS = "Tournament wins";
    public static final String FREE_WINS = "Freeplay wins";
    public static final String TOURNEY_LEVEL_DATANAME = "Smash Tourney Level";
    public static final String TOURNEY_EXPIRATION_DATANAME = "Smash Expiration Time";
    public static final String KO_DEALT_SCORE = "KO's Dealt";
    public static final String KO_RECEIVED_SCORE = "KO's Received";
    public static final String FALLEN_OUT_SCORE = "Times fallen out";
    public static final String ELO_SCORE_DATANAME = "Smash Elo";
    public static final String NAME_DATANAME = "Name";

    public static final int MIN_FREE_GAMES_TO_JOIN_TOURNEYS = 12; //The Minimum games a player needs to play to unlock tourney games
    public static final int HIGHEST_TOURNEY_LEVEL = 3; //not 0-indexed! - The Tournament level that is for the highest tournament level players. If people with this level win a tourney game, they win a tournament
    public static final int LOWEST_TOURNEY_LEVEL = 1;
    private static final float ELO_CHANGE_RATE_SPEED = (float)0.02;
    private static final float ELO_FARMABILITY_MOD = 0.2F; //min = mod/(1+mod)
    private static final float ELO_CHANGE_AT_0_ELO = 0.5F;

    private static final HashMap<String, Float> originalScores = new HashMap<String, Float>();

    public static int getTourneyLevel(Player p)
    {
        int result;
        long expiration;
        try
        {
            result = Integer.valueOf(FileManager.getData(getGeneralPlayerFile(p), TOURNEY_LEVEL_DATANAME));
            expiration = Long.valueOf(FileManager.getData(getGeneralPlayerFile(p), TOURNEY_EXPIRATION_DATANAME));
            if (Plugin.getMinute() > expiration)
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
        if (level > HIGHEST_TOURNEY_LEVEL || level < LOWEST_TOURNEY_LEVEL)
        {
            Bukkit.getConsoleSender().sendMessage(loggerPrefix() + ChatColor.RED + "Error! Could not set tourney level to " + level);
        }
        else if (level > LOWEST_TOURNEY_LEVEL)
        {
            FileManager.putData(getGeneralPlayerFile(p), TOURNEY_LEVEL_DATANAME, level);
            FileManager.putData(getGeneralPlayerFile(p), TOURNEY_EXPIRATION_DATANAME, getMinute() + SmashWorldManager.EXPIRATION_MINUTES);
        }
        else
        {
            resetTourneyLevel(p);
        }
    }

    public static void incrementTourneyLevel(Player p)
    {
        if (getTourneyLevel(p) == HIGHEST_TOURNEY_LEVEL)
        {
            resetTourneyLevel(p);
            incrementTourneyWins(p);
        }
        else if (getTourneyLevel(p) >= LOWEST_TOURNEY_LEVEL && getTourneyLevel(p) < HIGHEST_TOURNEY_LEVEL)
        {
            setTourneyLevel(p, getTourneyLevel(p) + 1);
        }
        else
        {
            Bukkit.getConsoleSender().sendMessage(loggerPrefix() + ChatColor.RED + "Error! Could not increment player " + p.getName() + "'s tourney level!");
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
        FileManager.putData(getGeneralPlayerFile(p), TOURNEY_LEVEL_DATANAME, LOWEST_TOURNEY_LEVEL);
        FileManager.putData(getGeneralPlayerFile(p), TOURNEY_EXPIRATION_DATANAME, (long)1e15);
        return LOWEST_TOURNEY_LEVEL;
    }

    public static int resetTourneyLevel(Player p)
    {
        return resetTourneyLevel(p.getName());
    }

    //***************

    public static int getFreeGamesPlayed(String p)
    {
        return FileManager.getIntData(getGeneralPlayerFile(p), FREE_GAMES_PLAYED);
    }

    public static int getFreeGamesPlayed(Player p)
    {
        return getFreeGamesPlayed(p.getName());
    }

    //***************
    public static int incrementFreeGamesWon(Player p)
    {
        return FileManager.incrementStatistic(getGeneralPlayerFile(p), FREE_WINS);
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
        return FileManager.getIntData(getGeneralPlayerFile(p), FREE_WINS);
    }

    //***************
    public static int incrementTourneyGamesPlayed(Player p)
    {
        return FileManager.incrementStatistic(getGeneralPlayerFile(p), TOURNEY_GAMES_PLAYED);
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
        return FileManager.getIntData(getGeneralPlayerFile(p), TOURNEY_GAMES_PLAYED);
    }

    public static int getHighestTourneyLevelPossible()
    {
        return HIGHEST_TOURNEY_LEVEL;
    }

    public static int getMinimumFreeGamesToJoinTourneys()
    {
        return MIN_FREE_GAMES_TO_JOIN_TOURNEYS;
    }

    protected static int getPointBasedGamePlayed(File f, String p)
    {
        int games = FileManager.getIntData(f, POINT_BASED_GAMES_PLAYED);
        if (games == 0)
        {
            games = getTotalGamesPlayed(p);
            FileManager.putData(f, POINT_BASED_GAMES_PLAYED, games);
        }
        return games;
    }

    //***************
    private static int incrementTourneyWins(Player p)
    {
        return FileManager.incrementStatistic(getGeneralPlayerFile(p), TOURNEY_WINS);
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
        return FileManager.getIntData(getGeneralPlayerFile(p), TOURNEY_WINS);
    }
    //****************

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
        sendErrorMessage("Error! Tried to get the old elo of a player who we didn't know how much Elo they had before!");
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
            score = Float.valueOf(FileManager.getData(getGeneralPlayerFile(p), ELO_SCORE_DATANAME));
            if (score == 0)
            {
                throw new NumberFormatException();
            }
        }
        catch (NumberFormatException e)
        {
            score = 1000;
            FileManager.putData(getGeneralPlayerFile(p), ELO_SCORE_DATANAME, score);
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
        //Bukkit.getAttacker("HappyMan").sendMessage("" + killerEloIncrease);
        return killerEloIncrease;
    }

    private static void performEloChange(String killer, final String deadPlayer, float modification, boolean showDeadPlayer)
    {
        if (SmashStatTracker.canJoinTourneys(killer) && SmashStatTracker.canJoinTourneys(deadPlayer))
        {
            //Bukkit.getAttacker("HappyMan").sendMessage("has advanced stat");
            if (!killer.equals(deadPlayer))
            {
               //Bukkit.getAttacker("HappyMan").sendMessage("not equal");
                float killerElo = getEloScore(killer);
                float deadElo = getEloScore(deadPlayer);
                float killerEloIncrease = calculateKillerEloIncrease(killerElo, deadElo)*modification;

                increaseElo(killer, killerElo, killerEloIncrease, true);
                increaseElo(deadPlayer, deadElo, -killerEloIncrease, showDeadPlayer);
            }
            else
            {
                //sendErrorMessage("Error! Tried to perform an elo change for someone killing themselves!");
            }
        }
    }

    public static void performEloChange(String deadPlayer, World w, float modification)
    {
        if (SmashWorldManager.isSmashWorld(w))
        {
            List<String> playersInvolved = new ArrayList<String>();
            for (Player p : WorldType.getNonspectators(w))
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
        /*if (displayTheChange && Bukkit.getAttacker(p) != null)
        {
            //p.sendMessage(eloChangeString(Math.round(eloIncrease)) + " elo");
        }*/
        //Bukkit.getAttacker("HappyMan").sendMessage("elo");
        FileManager.putData(getGeneralPlayerFile(p), ELO_SCORE_DATANAME, (formerElo + eloIncrease));
    }


    //***************

    public static void displayHelpMessage(Player p)
    {
        p.sendMessage(ChatColor.YELLOW + "Commands:");
        p.sendMessage(ChatColor.GREEN + "/" + WorldManager.NAV_COMMAND);
        p.sendMessage(ChatColor.GREEN + "/" + WorldManager.LIST_WORLD_CMD);
        p.sendMessage(ChatColor.GREEN + "/" + WorldManager.JOIN_CMD + " <world>");
        p.sendMessage(ChatColor.GREEN + "/" + WorldManager.LEAVE_CMD);
        p.sendMessage(ChatColor.GREEN + "/" + StatCommandExecutor.STAT_CMD + " [player]");
        p.sendMessage(ChatColor.GREEN + "/" + SMASH_KIT_CMD);
        p.sendMessage(ChatColor.GREEN + "/" + SmashWorldManager.SET_DEATHMATCH_PREFERENCE_CMD + " [yes|no] [lives]");
        p.sendMessage(ChatColor.GREEN + "/" + WorldManager.FIND_CMD + " [player]");
        p.sendMessage(ChatColor.GREEN + "/" + SPECTATE_COMMAND);
        p.sendMessage(ChatColor.GREEN + "/" + READY_UP_CMD);
        p.sendMessage(ChatColor.GREEN + "/" + VOTE_KICK_CMD);
    }

    public static int getTourneyRoundsPlayed(String p)
    {
        return FileManager.getIntData(getGeneralPlayerFile(p), TOURNEY_ROUNDS_PLAYED);
    }

    public static int getTourneyRoundsPlayed(Player p)
    {
        return getTourneyRoundsPlayed(p.getName());
    }

    public static int getTotalGamesPlayed(String p)
    {
        //Bukkit.getAttacker("HappyMan").sendMessage("" + getFreeGamesPlayed(p) + getTourneyRoundsPlayed(p));
        return getFreeGamesPlayed(p) + getTourneyRoundsPlayed(p);
    }

    public static int getTotalGamesPlayed(Player p)
    {
        return getTotalGamesPlayed(p.getName());
    }

    public static boolean canJoinTourneys(String p)
    {
        return getGamesTilCanJoinTourneys(p) <= 0;
    }

    public static int getGamesTilCanJoinTourneys(String p)
    {
        return MIN_FREE_GAMES_TO_JOIN_TOURNEYS - getTotalGamesPlayed(p);
    }
}
