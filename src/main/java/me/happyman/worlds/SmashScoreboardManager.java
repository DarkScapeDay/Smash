package me.happyman.worlds;

import me.happyman.commands.SmashManager;
import me.happyman.worlds.SmashWorldInteractor;
import me.happyman.worlds.SmashWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

public class SmashScoreboardManager
{
    private static final String START_TIME_SCORE = ChatColor.AQUA + "Time til start:";
    protected static final String FINAL_SCORES = ChatColor.GOLD + "   " + ChatColor.BOLD + "Final Scores";
    protected static HashMap<World, Scoreboard> scoreboards;
    private static HashMap<World, Team> scoreTeams;
    private static HashMap<World, Objective> scoreObjectives;
    private static HashMap<World, Objective> damageObjectives;
    private static HashMap<World, Integer> timeRemaining;

    protected SmashScoreboardManager()
    {
        scoreTeams = new HashMap<World, Team>();
        scoreObjectives = new HashMap<World, Objective>();
        scoreboards = new HashMap<World, Scoreboard>();
        damageObjectives = new HashMap<World, Objective>();
        timeRemaining = new HashMap<World, Integer>();
    }

    public static void updateDamage(Player p)
    {
        if (damageObjectives != null && damageObjectives.containsKey(p.getWorld()))
        {
            damageObjectives.get(p.getWorld()).getScore(p.getName()).setScore(p.getLevel());
        }
    }

    protected static Integer getTimeRemaining(World w)
    {
        if (SmashWorldManager.isSmashWorld(w) && timeRemaining.containsKey(w))
        {
            return timeRemaining.get(w);
        }
        SmashManager.getPlugin().sendErrorMessage("Error! Tried to get the time remaining for a world that hadn't started!");
        return -1;
    }

    protected static String getTimeRemaining(int timeRemaining)
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

    protected static void setTimeRemaining(World w, int timeLeft)
    {
        if (SmashWorldManager.isSmashWorld(w) && SmashWorldManager.gameHasStarted(w))
        {
            timeRemaining.put(w, timeLeft);
            if (timeLeft <= 5 && timeLeft > 0)
            {
                SmashWorldInteractor.sendMessageToWorld(w, ChatColor.GOLD + " " + timeLeft);
                playCountdownSound(w.getPlayers());
            }
            if (!SmashWorldInteractor.isDeathMatchWorld(w))
            {
                setScoreboardTitle(w, ChatColor.GOLD + "" + ChatColor.BOLD + "    Score [" + getTimeRemaining(timeLeft) + "]  ");
            }
        }
    }

    protected static String getScoreboardTitle(World w)
    {
        return scoreObjectives.get(w).getDisplayName();
    }

    protected static void setScoreboardTitle(World w, String newTitle)
    {
        scoreObjectives.get(w).setDisplayName(newTitle);
    }

    protected static Integer getStartTime(World w)
    {
        Integer timeRemaining = getScoreValue(w, START_TIME_SCORE);
        if (timeRemaining != null)
        {
            return getScoreValue(w, START_TIME_SCORE);
        }
        SmashManager.getPlugin().sendErrorMessage(ChatColor.RED + "Tried to get the time remaining for a world that had started!");
        return 0;
    }

    public static boolean hasScoreboard(World w)
    {
        return scoreboards.containsKey(w);
    }

    protected void addStartTime(World w, int seconds)
    {
        if (seconds >= 0)
        {
            setStartTime(w, getStartTime(w) + seconds);
        }
        else
        {
            subtractStartTime(w, -seconds);
        }
    }

    protected static void setStartTime(World w, int seconds)
    {
        setScoreValue(w, START_TIME_SCORE, seconds);
    }

    private static void playCountdownSound(List<Player> players)
    {
        for (Player p : players)
        {
            p.playSound(p.getLocation(), Sound.NOTE_PLING, 0.7F, 0.5F);
        }
    }

    /**
     *
     * @param w
     * @param seconds
     */
    protected static void subtractStartTime(World w, int seconds)
    {
        if (SmashWorldManager.gameHasStarted(w))
        {
            SmashManager.getPlugin().sendErrorMessage("Error! Game in world " + w.getName() + " has already started!");
        }
        else
        {
            int timeLeft = 0;
            if (getStartTime(w) != null)
            {
                timeLeft = getStartTime(w);
            }
            else
            {
                SmashManager.getPlugin().sendErrorMessage("Error! Could not get the start time for world " + w.getName());
            }
            int newTime = timeLeft - seconds;
            setStartTime(w, newTime);
            if (newTime > 0)
            {
                if (newTime < 6 && SmashWorldManager.isStartingSoon(w))
                {
                    playCountdownSound(w.getPlayers());
                    SmashWorldInteractor.sendMessageToWorld(w, ChatColor.GREEN + " " + newTime);
                }
            }
            else if (SmashWorldManager.isStartingSoon(w))
            {
                SmashWorldManager.startGame(w);
            }
            else
            {
                SmashWorldInteractor.sendMessageToWorld(w, ChatColor.YELLOW + "There must be " + SmashWorldManager.getStartRequirement(w) + " players in order for this game to start! :(", false);
                resetStartTime(w);
            }
        }
    }

    protected static void initializeScoreboard(final World w)
    {
        if (w != null && !scoreboards.containsKey(w))
        {
            Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
            scoreboards.put(w, scoreboard);

            Objective rightSide = scoreboard.registerNewObjective("score", "dummy");
            rightSide.setDisplaySlot(DisplaySlot.SIDEBAR);
            scoreObjectives.put(w, rightSide);
            setScoreboardTitle(w, ChatColor.GOLD + "" + ChatColor.BOLD + "    Map: " + SmashWorldInteractor.getMapName(w) + "  ");

            Objective belowName = scoreboard.registerNewObjective("damage", "dummy");
            belowName.setDisplaySlot(DisplaySlot.BELOW_NAME);
            belowName.setDisplayName(ChatColor.WHITE + "%");
            damageObjectives.put(w, belowName);

            Team team = scoreboard.registerNewTeam("scores");
            team.setPrefix(ChatColor.AQUA + "");
            team.setSuffix(ChatColor.AQUA + "");
            scoreTeams.put(w, team);

            resetStartTime(w);

        SmashWorldManager.startStartCountDown(w);

            Runnable r2 = new Runnable()
            {
                public void run()
                {
                    updateScoreboard(w);
                }
            };
            SmashManager.getPlugin().startRunnable(r2);
            SmashManager.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(SmashManager.getPlugin(), r2, 4); //10
        }
        else if (w != null)
        {
            SmashManager.getPlugin().sendErrorMessage("Error! World " + w.getName() + "'s scoreboard was already created!");
        }
    }

    protected static void resetStartTime(World w)
    {
        SmashWorldManager.removeStartOverride(w);
        int waitTime = SmashWorldManager.FREE_WAIT_TIME;
        if (SmashWorldManager.isTourneyWorld(w))
        {
            waitTime = SmashWorldManager.TOURNEY_WAIT_TIME;
        }
        setStartTime(w, waitTime);
    }

    protected static void removeScoreboardIfThere(World w)
    {
        if (scoreboards != null)
        {
            if (scoreboards.containsKey(w) && (!scoreObjectives.containsKey(w) || !damageObjectives.containsKey(w) || !scoreTeams.containsKey(w)))
            {
                SmashManager.getPlugin().sendErrorMessage("Error! Scoreboard was missing one of its properties!");
            }
            else if (scoreboards.containsKey(w))
            {
                SmashWorldManager.cancelTime(w);
                SmashWorldManager.cancelCountdown(w);

                if (timeRemaining.containsKey(w))
                {
                    timeRemaining.remove(w);
                }
                clearEntries(w);
                scoreboards.remove(w);
                scoreObjectives.get(w).getScore(START_TIME_SCORE).setScore(-1);
                scoreObjectives.remove(w);
                scoreObjectives.remove(w);
                scoreTeams.remove(w);
                updateScoreboard(w);
            }
        }
    }

    protected static void clearEntries(World w)
    {
        if (scoreboards.containsKey(w))
        {
            for (String entry : scoreboards.get(w).getEntries())
            {
                removeEntry(w, entry);
            }
        }
        else
        {
            SmashManager.getPlugin().sendErrorMessage("Error! Could not clear entries for scoreboard in world " + w.getName() + " because it did not have a scoreboard!");
        }
    }

    protected static void updateScoreboard(World w)
    {
        for (Player p : w.getPlayers())
        {
            updateScoreboard(p);
        }
    }

    protected static void updateScoreboard(Player p)
    {
        World w = p.getWorld();
        if (scoreboards != null)
        {
            if (!scoreboards.containsKey(w))
            {
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
            else// if (!p.getScoreboard().equals(scoreboards.get(w)))
            {
                p.setScoreboard(scoreboards.get(w));
            }
        }
        else
        {
            p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
        }
    }

    public static Set<String> getScoreboardEntries(World w)
    {
        if (scoreboards.containsKey(w))
        {
            return scoreboards.get(w).getEntries();
        }
        SmashManager.getPlugin().sendErrorMessage("Error! Could not get the scoreboard entries for world " + w.getName() + " because it didn't have a scoreboard!");
        return null;
    }

    protected static String getPlayerEntryName(Player p)
    {
        return getPlayerEntryName(p.getName());
    }

    protected static String getPlayerEntryName(String p)
    {
        return ChatColor.AQUA + p;
    }

    protected static Integer getScoreValue(World w, String scoreName)
    {
        if (scoreObjectives != null && scoreObjectives.containsKey(w))
        {
            return scoreObjectives.get(w).getScore(scoreName).getScore();
        }
        else if (scoreObjectives != null)
        {
            Bukkit.getConsoleSender().sendMessage(SmashManager.getPlugin().loggerPrefix() + ChatColor.RED + "Error! Could not get score value for world " + w.getName() + ", score " + scoreName + " because it did not have the objective");
        }
        else
        {
            Bukkit.getConsoleSender().sendMessage(SmashManager.getPlugin().loggerPrefix() + ChatColor.RED + "Error! Could not get score value for world " + w.getName() + " because there was not the objective");
        }
        return null;
    }

    protected static Integer getPlayerScoreValue(String p, World w)
    {
        return getScoreValue(w, getPlayerEntryName(p));
    }

    public static Integer getPlayerScoreValue(Player p)
    {
        return getPlayerScoreValue(p.getName(), p.getWorld());
    }

    protected static void setScoreValue(World w, String scoreName, int score)
    {
        if (scoreName.equals(START_TIME_SCORE) && !SmashWorldManager.gameHasStarted(w) || SmashWorldManager.gameHasStarted(w) && !SmashWorldManager.gameHasEnded(w))
        {
            if (scoreObjectives != null && scoreObjectives.containsKey(w))
            {
                scoreObjectives.get(w).getScore(scoreName).setScore(score);
            }
            else if (scoreObjectives != null)
            {
                Bukkit.getConsoleSender().sendMessage(SmashManager.getPlugin().loggerPrefix() + ChatColor.RED + "Error! Could not get score value for world " + w.getName() + " because it did not have the objective");
            }
            else
            {
                Bukkit.getConsoleSender().sendMessage(SmashManager.getPlugin().loggerPrefix() + ChatColor.RED + "Error! Could not get score value for world " + w.getName() + " because there was not the objective");
            }
        }
    }

    protected static void setPlayerScoreValue(Player p, int score)
    {
        setPlayerScoreValue(p.getName(), p.getWorld(), score);
    }

    protected static void setPlayerScoreValue(String p, World w, int score)
    {
        setScoreValue(w, getPlayerEntryName(p), score);
    }

    protected static boolean isOnScoreboard(Player p)
    {
        return hasScoreboard(p.getWorld()) && getScoreboardEntries(p.getWorld()).contains(p.getName());
    }

    protected static void removeEntry(World w, String entry)
    {
        if (SmashWorldManager.isSmashWorld(w) && scoreboards != null && scoreboards.containsKey(w))
        {
            scoreboards.get(w).resetScores(entry);
        }
    }
}