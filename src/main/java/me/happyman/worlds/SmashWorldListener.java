package me.happyman.worlds;

import me.happyman.utils.FileManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import static me.happyman.Plugin.getOrdinalIndicator;
import static me.happyman.Plugin.getPlugin;
import static me.happyman.utils.FileManager.getGeneralPlayerFile;

public class SmashWorldListener implements Listener
{

    public SmashWorldListener()
    {
        Bukkit.getPluginManager().registerEvents(this, getPlugin());
    }

    public static void displayJoinMessage(Player p)
    {
        getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(getPlugin(), getJoinRunnable(p), 2);
    }

    private static Runnable getJoinRunnable(final Player joiner)
    {
        return new Runnable()
        {
            public void run()
            {
                for (Player player : getPlugin().getServer().getOnlinePlayers())
                {
                    if (!(player.equals(joiner)))
                    {
                        if (joiner.hasPlayedBefore())
                        {
                            //player.sendMessage(joiner.getDisplayName() + ChatColor.GRAY + " has joined the game!");
                        }
                        else
                        {
                            player.sendMessage(ChatColor.BLUE + "" + ChatColor.MAGIC + "W" + ChatColor.RESET + " "
                                    + ChatColor.GOLD + joiner.getName() + " has joined for the first time!!!!! "
                                    + ChatColor.BLUE + ChatColor.MAGIC + "W");
                        }
                    //p.sendMessage(ChatColor.GRAY + "You have joined the game!");
                    }
                }

                FileManager.putData(getGeneralPlayerFile(joiner), SmashStatTracker.NAME_DATANAME, joiner.getName());
                //putGeneralPlayerDatum(joiner, "IP", joiner.getAddress().toString().substring(1, joiner.getAddress().toString().length()));
                int logins;
                try
                {
                    logins = Math.round(Float.valueOf(FileManager.getData(getGeneralPlayerFile(joiner), "Total logins")));
                }
                catch (NumberFormatException e1)
                {
                    logins = 0;
                }
                logins++;
                if (logins == 1)
                {
                    joiner.sendMessage(ChatColor.GOLD + "" + ChatColor.MAGIC + "W" + ChatColor.RESET + " "
                            + ChatColor.GOLD + joiner.getName() + " has joined for the first time!!!!! "
                            + ChatColor.MAGIC + "W");
                    getPlugin().getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "The server welcomes "
                            + joiner.getDisplayName() + ChatColor.YELLOW + " with open arms!");

                    joiner.sendMessage(ChatColor.GOLD + "Welcome! Do /smash to join a Smash game!");
                    if (SmashStatTracker.getFreeGamesPlayed(joiner) < SmashStatTracker.getMinimumFreeGamesToJoinTourneys())
                    {
                        joiner.sendMessage(ChatColor.AQUA + "You'll be able to play in Tournament games after you play through " + (SmashStatTracker.getMinimumFreeGamesToJoinTourneys() + SmashStatTracker.getFreeGamesPlayed(joiner))
                                + " more Freeplay games.");
                    }
                    SmashStatTracker.displayHelpMessage(joiner);
                }
                else
                {
                    Bukkit.getConsoleSender().sendMessage(joiner.getDisplayName()
                            + ChatColor.GRAY + " (" + joiner.getUniqueId() + ") has joined the game!");
                    if (logins % 1000 == 0)
                    {
                        /**/Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.MAGIC + "W" + ChatColor.RESET +
                                " " + ChatColor.GREEN + "CONGRATULATIONS " +
                                joiner.getDisplayName().toUpperCase() + ChatColor.RESET + "" + ChatColor.GREEN +
                                " ON YOUR " + ChatColor.BOLD + logins + ChatColor.RESET + "" + ChatColor.GREEN +
                                getOrdinalIndicator(logins) + " LOGIN!!!! " + ChatColor.BOLD + ";D " +
                                ChatColor.RESET + ChatColor.GOLD + ChatColor.MAGIC + "W");
                        joiner.setItemInHand(new ItemStack(Material.DIAMOND_BLOCK, 64));
                    }
                    else if (logins % 100 == 0)
                    {
                        joiner.sendMessage(ChatColor.GREEN + "Congratulations on your " + logins +
                                getOrdinalIndicator(logins) + " login! Wow!");
                    }
                    else if (logins % 10 == 0)
                    {
                        joiner.sendMessage(ChatColor.GREEN + "Greetings! This is your your " + logins +
                                getOrdinalIndicator(logins) + " login :)");
                    }
                    else
                    {
                        joiner.sendMessage(ChatColor.GREEN + "Greetings!");
                    }
                }
                FileManager.putData(getGeneralPlayerFile(joiner), "Total logins", logins);
            }
        };
    }

}
