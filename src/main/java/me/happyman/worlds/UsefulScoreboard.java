package me.happyman.worlds;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.util.ArrayList;
import java.util.List;

import static me.happyman.Plugin.sendErrorMessage;

public class UsefulScoreboard
{
    private Scoreboard scoreboard;
    private Objective belowNameObjective;
    private Objective sideObjective;
    private String belowNameScoreSuffix;
    private String sideTitle;
    private final Player owner;
    private final World world;

    public UsefulScoreboard(Player owner, String belowNameScoreSuffix)
    {
        this(owner, null, belowNameScoreSuffix);
    }

    public UsefulScoreboard(Player owner, String title, String belowNameScoreSuffix)
    {
        this(owner, null, title, belowNameScoreSuffix);
    }

    public UsefulScoreboard(World world, String belowNameScoreSuffix)
    {
        this(world, null, belowNameScoreSuffix);
    }

    public UsefulScoreboard(World world, String title, String belowNameScoreSuffix)
    {
        this(null, world, title, belowNameScoreSuffix);
    }

    private UsefulScoreboard(Player owner, World world, String title, String belowNameScoreSuffix)
    {
        if (owner == null && world == null/* || owner != null && world != null*/)
        {
            sendErrorMessage(ChatColor.RED + "What are you doing with scoreboards?");
        }
        getScoreboard();
        this.owner = owner;
        this.world = world;
        this.sideTitle = title == null ? "Scores" : title;
        this.belowNameScoreSuffix =  ChatColor.WHITE + (belowNameScoreSuffix == null ? "" : belowNameScoreSuffix);
        belowNameObjective = null;
        sideObjective = null;
    }

    private Scoreboard getScoreboard()
    {
        if (scoreboard == null)
        {
            scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
//            Team team = scoreboard.registerNewTeam(ChatColor.AQUA + "scores");
//            team.setPrefix(ChatColor.AQUA + "");
//            team.setSuffix(ChatColor.AQUA + "");
        }
        return scoreboard;
    }

    public void destroy()
    {
//        for (String entry : scoreboard.getEntries())
//        {
//            scoreboard.resetScores(entry);
//        }
        if (owner == null)
        {
            for (Player p : world.getPlayers())
            {
                setVisible(p, false);
            }
        }
        else
        {
            setVisible(owner, false);
        }
        scoreboard = null;
        sideObjective = null;
        belowNameObjective = null;
        sideTitle = null;
    }

    private boolean hasOwner()
    {
        return owner != null;
    }

    public void setVisible(Player p, boolean using)
    {
        if (scoreboard != null)
        {
            boolean currentScoreboard = p.getScoreboard().equals(scoreboard);
            if (using && scoreboard != null && !currentScoreboard)
            {
                p.setScoreboard(scoreboard);
            }
            else if (!using && currentScoreboard)
            {
                p.setScoreboard(Bukkit.getScoreboardManager().getMainScoreboard());
            }
        }
        else
        {
            sendErrorMessage("Error! Tried to set an unused scoreboard to be visible!");
        }
    }

    public void setVisibleToAllUsers(boolean using)
    {
        if (hasOwner())
        {
            setVisible(owner, using);
        }
        else
        {
            for (Player p : world.getPlayers())
            {
                setVisible(p, using);
            }
        }
    }

    public String getScoreboardTitle()
    {
        return sideObjective == null ? null : sideObjective.getDisplayName();
    }

    public void setSideTitle(String title)
    {
        this.sideTitle = title;
        if (sideObjective != null)
        {
            sideObjective.setDisplayName(title);
        }
    }

    public void removeSideEntry(String entry, boolean isFancy)
    {
        if (scoreboard != null)
        {
            if (!isFancy)
            {
                String colorlessEntry = ChatColor.stripColor(entry);
                for (String entryHere : scoreboard.getEntries())
                {
                    if (colorlessEntry.equals(ChatColor.stripColor(entryHere)))
                    {
                        setSideScore(entryHere, (Integer)null);
                        break;
                    }
                }
            }
            else
            {
                ArrayList<String> entries = new ArrayList<String>(scoreboard.getEntries());
                mergeSort(entries, 0, entries.size() - 1);

                for (int i = 0; i < entries.size(); i++)
                {
                    String entryHere = entries.get(i);
                    if (entryHere.equals(entry))
                    {
                        entries.remove(i--);
                        if (i < entries.size())
                        {
                            entries.remove(i);
                        }
                        setToFancyEntries(entries, null);
                        break;
                    }
                }
            }
        }
        else
        {
            sendErrorMessage("Error! There were no scoreboard!");
        }
    }

    public void clearEntries()
    {
        if (scoreboard != null)
        {
            List<String> entries = new ArrayList<String>(scoreboard.getEntries());
            for (int i = 0; i < entries.size(); i++)
            {
                removeSideEntry(entries.get(i), false);
            }
        }
    }

    public boolean hasSideScoreboard()
    {
        return scoreboard != null && scoreboard.getEntries().size() > 0;
    }

    public List<String> getColorlessEntries()
    {
        if (scoreboard == null)
        {
            return null;
        }
        List<String> result = new ArrayList<String>();
        for (String entry : scoreboard.getEntries())
        {
            result.add(ChatColor.stripColor(entry));
        }
        return result;
    }

    private void mergeSort(ArrayList<String> entries, int first, int last)
    {
        if (first < last && entries.size() > 0)
        {
            int mid = (first + last)/2;
            mergeSort(entries, first, mid);
            mergeSort(entries, mid + 1, last);
            merge(entries, first, mid, last);
        }
    }

    private void merge(ArrayList<String> entries, int first, int mid, int last)
    {
        String[] temp = new String[entries.size()];
        int first1 = first;
        int first2 = mid + 1;
        int index = first;

        for (; first1 <= mid && first2 <= last; index++)
        {
            String whatsNext;
            if (sideObjective.getScore(entries.get(first1)).getScore() > sideObjective.getScore(entries.get(first2)).getScore())
            {
                whatsNext = entries.get(first1++);
            }
            else
            {
                whatsNext = entries.get(first2++);
            }
            temp[index] = whatsNext;
        }
        for (; first1 <= mid; first1++, index++)
        {
            temp[index] = entries.get(first1);
        }
        for (; first2 <= last; first2++, index++)
        {
            temp[index] = entries.get(first2);
        }
        for (index = first; index <= last; index++)
        {
            entries.set(index, temp[index]);
        }
    }

    public void setSideScore(String key, String value)
    {
        if (belowNameObjective == null)
        {
            ArrayList<String> entries = new ArrayList<String>(getScoreboard().getEntries());
            String entryToRemove = null;
            mergeSort(entries, 0, entries.size() - 1);

            boolean foundIt = false;
            for (int i = 0; i < entries.size(); i++)
            {
                String entry = entries.get(i);
                if (entry.equals(key))
                {
                    i++;
                    if (i < entries.size())
                    {
                        entryToRemove = entries.get(i);
                    }
                    entries.add(i, value);
                    foundIt = true;
                    break;
                }
            }

            if (!foundIt)
            {
                entries.add(key);
                entries.add(value);
            }

            setToFancyEntries(entries, entryToRemove);
        }
        else
        {
            sendErrorMessage("Error! Tried to do a key-value score on a scoreboard that had below name stuff!");
        }
    }

    private void setToFancyEntries(final List<String> entries, String entryToRemove)
    {
        for (int i = 0, displayedIndex = entries.size() - 1; i < entries.size(); i++, displayedIndex--)
        {
            String entry = entries.get(i);
            if (entryToRemove != null && entry.equals(entryToRemove))
            {
                scoreboard.resetScores(entry);
                displayedIndex++;
            }
            else
            {
                setSideScore(entry, displayedIndex);
            }
        }
    }

    public Integer getSideScore(String entry)
    {
        if (scoreboard != null && sideObjective != null)
        {
            String colorlessEntry = ChatColor.stripColor(entry);
            for (String entryHere : scoreboard.getEntries())
            {
                if (colorlessEntry.equals(ChatColor.stripColor(entryHere)))
                {
                    return sideObjective.getScore(entry).getScore();
                }
            }
        }
        return null;
    }

    public Integer getSideScore(Player p)
    {
        return getSideScore(p.getName());
    }

    public void addSideScore(String entry, int value)
    {
        Integer currentScore = getSideScore(entry);
        setSideScore(entry, currentScore == null ? value : (currentScore + value));
    }

    public void setSideScore(ChatColor entryColor, Player p, Integer score)
    {
        setSideScore(entryColor + p.getName(), score);
    }

    public void setSideScore(final String entry, final Integer score)
    {
        if (score != null)
        {
            if (sideObjective == null)
            {
                Objective existing = getScoreboard().getObjective("score");
                sideObjective = existing == null ? getScoreboard().registerNewObjective("score", "dummy") : existing;
                sideObjective.setDisplaySlot(DisplaySlot.SIDEBAR);
                sideObjective.setDisplayName(sideTitle);
            }
            sideObjective.getScore(entry).setScore(score);
        }
        else if (scoreboard != null)
        {
            scoreboard.resetScores(entry);
        }
    }

    public void removeBelowNameEntry(Player entry)
    {
        setBelowNameScore(entry, null);
    }

    public void setBelowNameScore(Player entry, Integer score)
    {
        if (!hasOwner())
        {
            if (score != null)
            {
                if (belowNameObjective == null)
                {
                    Objective existing = getScoreboard().getObjective("dummy");
                    belowNameObjective = existing == null ? getScoreboard().registerNewObjective("dummy", "dummy") : existing;
                    belowNameObjective.setDisplaySlot(DisplaySlot.BELOW_NAME);
                    belowNameObjective.setDisplayName(belowNameScoreSuffix);
                }
                belowNameObjective.getScore(entry.getName()).setScore(score);
            }
            else if (scoreboard != null)
            {
                scoreboard.resetScores(entry.getName());
            }
        }
        else
        {
            sendErrorMessage("Error! You can't have below name scores for personal scoreboards.");
        }
    }
}
