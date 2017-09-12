package me.happyman.worlds;

import me.happyman.utils.FileManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static me.happyman.Plugin.*;
import static me.happyman.utils.FileManager.getGeneralPlayerFile;
import static me.happyman.utils.FileManager.getIntData;
import static me.happyman.worlds.SmashStatTracker.*;

public class StatCommandExecutor implements CommandExecutor
{
    // killerEloIncrease = ELO_CHANGE_AT_0_ELO * ((float)Math.exp(-difference* ELO_CHANGE_RATE_SPEED)+ ELO_FARMABILITY_MOD)/(1+ ELO_FARMABILITY_MOD);
    static final String STAT_CMD = "statistics";

    public StatCommandExecutor()
    {
        setExecutor(STAT_CMD, this);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (matchesCommand(label, STAT_CMD))
        {
            String p;

            if (args.length == 0)
            {
                if (!(sender instanceof Player))
                {
                    sendErrorMessage("You are not a player!");
                    return true;
                }
                else
                {
                    p = ((Player)sender).getName();
                }
            }
            else
            {
                /*List<String> offlines = new ArrayList<String>();
                boolean foundPlayer = Bukkit.getAttacker(args[0]) != null;
                for (OfflinePlayer offline : Bukkit.getOfflinePlayers())
                {
                    offlines.add(offline.getDisplayName());
                }*/
                p = args[0];
            }


            File statFile = getGeneralPlayerFile(p);
            if (!statFile.exists())
            {
                sender.sendMessage(ChatColor.YELLOW + "Server was unable to find " + p + "'s stats.");
                return true;
            }
            List<String> statLines = new ArrayList<String>();
            String prefix = ChatColor.DARK_GRAY + " | ";
            statLines.add(prefix + ChatColor.AQUA + "Tournaments played: " + getIntData(statFile, TOURNEY_GAMES_PLAYED)); //@TODO: datumtype?
            statLines.add(prefix + ChatColor.AQUA + "Tournaments won: " + getIntData(statFile, TOURNEY_WINS));
            statLines.add(prefix + ChatColor.GOLD + "Free Games played: " + getIntData(statFile, FREE_GAMES_PLAYED));
            statLines.add(prefix + ChatColor.GOLD + "Free Games won: " + getIntData(statFile, FREE_WINS));
            statLines.add(prefix + ChatColor.YELLOW + "KO's Dealt/Received: " + getIntData(statFile, KO_DEALT_SCORE) + "/" + getIntData(statFile, KO_RECEIVED_SCORE));
            statLines.add(prefix + ChatColor.YELLOW + "Times fallen off the map: " + getIntData(statFile, FALLEN_OUT_SCORE));//@TODO: make methods full file reading instead
            if (canJoinTourneys(p))
            {
                statLines.add(prefix + ChatColor.GRAY + "Avg end-game score: " + String.format("%.2f", 1.0 * FileManager.getIntData(statFile, POINTS_ACCUMULATED_DATANAME) / getPointBasedGamePlayed(statFile, p)));
                statLines.add(prefix + ChatColor.GREEN + "" + ChatColor.BOLD + "Elo" + ChatColor.RESET + "" + ChatColor.GREEN + ": " + Math.round(getEloScore(p)));
            }

            short emeralds = EconomyCurrencyManager.getEmeralds(p);
            if (emeralds != 0)
            {
                statLines.add(prefix + ChatColor.DARK_GREEN + "" + ChatColor.ITALIC + EconomyCurrencyManager.EMERALD_COUNT_DATANAME + ": " + emeralds);
            }

            int longestLength = 0;
            for (String line : statLines)
            {
                if (line.length() > longestLength)
                {
                    longestLength = line.length();
                }
            }
            String playerFolderName = statFile.getParentFile().getName();
            String firstText = "Stats for " + playerFolderName.substring(0, playerFolderName.length() - 33);
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
