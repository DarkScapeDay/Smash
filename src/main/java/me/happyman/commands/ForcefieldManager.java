package me.happyman.commands;

import me.happyman.source;
import me.happyman.utils.DirectoryType;
import me.happyman.utils.Forcefield;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ForcefieldManager extends CommandVerifier implements CommandExecutor
{
    private final String enableCmd;
    private final String disableCmd;
    private final String addTeamCmd;
    private HashMap<World, Forcefield> worldForcefields;
    private final String settingFile = "ForcefieldExemptTeams.txt";
    private static List<String> exemptTeams;

    public ForcefieldManager(source plugin)
    {
        super(plugin);

        worldForcefields = new HashMap<World, Forcefield>();
        exemptTeams = new ArrayList<String>();
        try
        {
            File f = plugin.getSpecificFile(DirectoryType.SERVER_DATA, "", settingFile);
            Scanner scanner = new Scanner(f);
            scanner.useDelimiter("\\Z");
            String[] stringList = scanner.next().split(", ");
            for (String team : stringList)
            {
                exemptTeams.add(team);
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        catch (NoSuchElementException ex) {}

        for (World world : Bukkit.getWorlds())
        {
            worldForcefields.put(world, new Forcefield(plugin));
        }
        enableCmd = "enableforcefield";
        plugin.setExecutor(enableCmd, this);
        addTeamCmd = "ffexempt";
        plugin.setExecutor(addTeamCmd, this);
        disableCmd = "disableforcefield";
        plugin.setExecutor(disableCmd, this);
    }

    public static List<String> getExemptTeams()
    {
        return exemptTeams;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        Player p = null;
        if (sender instanceof Player)
        {
            p = (Player)sender;
        }
        if (plugin.matchesCommand(label, addTeamCmd))
        {
            if (!(args.length == 1 && args[0].equalsIgnoreCase("list")))
            {
                if (args.length <= 1)
                {
                    plugin.displayHelpMessage(sender, label);
                }
                else if (Bukkit.getScoreboardManager().getMainScoreboard().getTeam(args[1]) == null)
                {
                    sender.sendMessage(ChatColor.RED + "Team not found!");
                }
                else if (args[0].equalsIgnoreCase("add"))
                {
                    if (exemptTeams.contains(args[1]))
                    {
                        sender.sendMessage(ChatColor.GOLD + "Team was already exempt");
                    }
                    else
                    {
                        exemptTeams.add(args[1]);
                        plugin.putPluginDatum(DirectoryType.SERVER_DATA, "", settingFile, args[1], "true");
                        sender.sendMessage(ChatColor.GOLD + "Team " + args[1] + " exempted from forcefield damage.");
                    }
                }
                else if (args[0].equalsIgnoreCase("remove"))
                {
                    if (exemptTeams.contains(args[1]))
                    {
                        exemptTeams.remove(args[1]);
                        plugin.putPluginDatum(DirectoryType.SERVER_DATA, "", settingFile, args[1], "false");
                        sender.sendMessage(ChatColor.GOLD + "Team " + args[1] + " can take forcefield damage now.");
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.GOLD + "Team was already removed from being exempt");
                    }
                }
            }
            else
            {
                sender.sendMessage(ChatColor.YELLOW + "Scoreboard teams exempt from forcefield damage:");
                int i;
                for (i = 0; i < getExemptTeams().size(); i++)
                {
                    ChatColor c = ChatColor.YELLOW;
                    if (i % 2 == 1)
                    {
                        c = ChatColor.RED;
                    }
                    else if (i % 2 == 0)
                    {
                        c = ChatColor.GOLD;
                    }
                    sender.sendMessage(c + getExemptTeams().get(i));
                }
                if (i == 0)
                {
                    sender.sendMessage(ChatColor.RED + ("Well it looks like there aren't any.").toUpperCase());
                }
            }
            return true;
        }
        else if (plugin.matchesCommand(label, enableCmd))
        {
            boolean enableFFAnyway = false;
            if (getDecisions().containsKey(p))
            {
                if (plugin.isTrue(getDecision(p)))
                {
                    enableFFAnyway = true;
                    releaseVerifier(p);
                }
                else if (plugin.isFalse(getDecision(p)))
                {
                    sender.sendMessage(ChatColor.GREEN + "Command /" + label + " cancelled!");
                    releaseVerifier(p);
                    return true;
                }
                else
                {
                    sender.sendMessage(ChatColor.RED + "Error! Please enter \"yes\" or \"no\"!");
                    return true;
                }
            }

            Player playerToSurround = null;
            World world = null;
            String[] subArgs = null;
            boolean allWorlds = false;
            if (args.length != 0)
            {
                world = Bukkit.getWorld(args[0]);
                allWorlds = args[0].equalsIgnoreCase("all");
                if (!allWorlds)
                {
                    for (Player player : Bukkit.getOnlinePlayers())
                    {
                        if (player.getName().equalsIgnoreCase(args[0]))
                        {
                            playerToSurround = player;
                            world = playerToSurround.getWorld();
                            break;
                        }
                    }
                }
                subArgs = new String[args.length - 1];
                for (int i = 1; i < args.length; i++)
                {
                    subArgs[i-1] = args[i];
                }
            }

            int normalMinArgs = 7; //ef Happyman r d i
            if (args.length == 0 || args.length != 1 && (playerToSurround == null && (args.length > normalMinArgs + 2 || args.length < normalMinArgs)
                    || playerToSurround != null && (args.length > normalMinArgs - 2 || args.length < normalMinArgs - 3))
                    || args[0].equalsIgnoreCase("help") || !plugin.numericArgs(subArgs))
            {
                plugin.displayHelpMessage(sender, label);
            }
            else if (world == null && !allWorlds)
            {
                sender.sendMessage(ChatColor.RED + "Error! Could not find a world or player by that name!");
            }
            else
            {
                if ((allWorlds || !worldForcefields.get(world).isEnabled()) && args.length != 1 || enableFFAnyway || !(sender instanceof Player))
                {
                    if (args.length != 1)
                    {
                        float[] params;
                        if (playerToSurround != null)
                        {
                            if (playerToSurround.getLocation() == null)
                            {
                                sender.sendMessage(ChatColor.RED + "Error! idk where that player is!");
                                return true;
                            }
                            params = new float[args.length];
                            params[0] = (float)playerToSurround.getLocation().getX();
                            params[1] = (float)playerToSurround.getLocation().getY();
                            params[2] = (float)playerToSurround.getLocation().getZ();
                            for (int i = 1; i < args.length - 2; i++)
                            {
                                params[i+2] = Float.valueOf(args[i]);
                                //enableforcefield HappyMan r dmg int
                            }
                        }
                        else
                        {
                            params = new float[args.length - 3];
                            for (int i = 1; i < args.length - 2; i++)
                            {
                                params[i-1] = Float.valueOf(args[i]);
                            }
                            if (params.length == 4 && params[3] < 0 //sphere
                                || params.length == 5 && params[3] < 0) //cylinder
                            {
                                sender.sendMessage(ChatColor.RED + "Error! Radius cannot be negative!");
                                return true;
                            }
                            else if (params.length == 5 && params[4] <= 0)
                            {
                                sender.sendMessage(ChatColor.RED + "Error! Cylinder height must be postive!");
                                return true;
                            }
                            else if (params.length == 6 && (params[0] > params[1] || params[2] > params[3] || params[4] > params[5]))
                            {
                                sender.sendMessage(ChatColor.RED + "Error! Mins cannot be greater than maxes!");
                                return true;
                            }
                        }
                        float ffdamage = Float.valueOf(args[args.length - 2]);
                        plugin.getConfig().set("forcefield.damage", ffdamage);
                        int interval = Math.round(Float.valueOf(args[args.length - 1]));
                        plugin.getConfig().set("forcefield.interval", interval);
                        //int delay = Math.round(Float.valueOf(args[args.length - 1]));
                        //plugin.getConfig().set("forcefield.delay", delay);

                        //int ffdamage = plugin.getConfig().getInt("forcefield.damage"); //7
                        //int interval = plugin.getConfig().getInt("forcefield.interval");
                        //int delay = plugin.getConfig().getInt("forcefield.delay");
                        plugin.saveConfig();

                        String s = "";
                        String s1 = "";
                        if (allWorlds)
                        {
                            s = "s";
                            s1 = "all worlds";
                            for (int i = 0; i < worldForcefields.size(); i++)
                            {
                                worldForcefields.get(Bukkit.getWorlds().get(i)).enableForcefield(Bukkit.getWorlds().get(i), params, ffdamage, interval, 0);
                            }
                        }
                        else
                        {
                            s1 = world.getName();
                            worldForcefields.get(world).enableForcefield(world, params, ffdamage, interval, 0);
                        }
                        if (enableFFAnyway || allWorlds)
                        {
                            sender.sendMessage(ChatColor.GOLD + "Forcefield" + s + " re-enabled in " + s1 + "!");
                        }
                        else
                        {
                            sender.sendMessage(ChatColor.GOLD + "Forcefield" + s + " enabled in " + s1 + "!");
                        }
                    }
                    else
                    {
                        if (allWorlds)
                        {
                            sender.sendMessage(ChatColor.GOLD + "Default forcefield enabled in all worlds!");
                            for (World w : Bukkit.getWorlds())
                            {
                                worldForcefields.get(w).enableForcefield(w);
                            }
                        }
                        else
                        {
                            sender.sendMessage(ChatColor.GOLD + "Default forcefield enabled in " + world.getName() + "!");
                            worldForcefields.get(world).enableForcefield(world);
                        }
                    }
                }
                else
                {
                    if (args.length != 1)
                    {
                        sender.sendMessage(ChatColor.YELLOW + "Forcefield is already active in " + world.getName() + "! Enable forcefield anyway?");
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.YELLOW + "You are about to enable a default forcefield. Do you really want to do that?");
                    }
                    bindVerifier(p, label, args);
                }
            }
            return true;
        }
        else if (plugin.matchesCommand(label, disableCmd))
        {
            if (args.length == 0)
            {
                sender.sendMessage(ChatColor.RED + "Error! You must specify which world you want to disable the forcefield for (or all if you want to shut them all down)!");
            }
            else
            {
                Player player = Bukkit.getPlayer(args[0]);
                World world = Bukkit.getWorld(args[0]);
                if (player != null)
                {
                    world = player.getWorld();
                }
                Forcefield ff = worldForcefields.get(world);
                if (args[0].equalsIgnoreCase("all"))
                {
                    for (World w : Bukkit.getWorlds())
                    {
                        worldForcefields.get(w).disableForcefield();
                    }
                    sender.sendMessage(ChatColor.GREEN + "Forcefields in all worlds disabled!");
                }
                else if (world == null)
                {
                    sender.sendMessage(ChatColor.RED + "Error! Could not find a world or player by that name!");
                }
                else if (!ff.isEnabled())
                {
                    sender.sendMessage(ChatColor.GOLD + "Forcefield was already disabled!");
                }
                else
                {
                    worldForcefields.get(world).disableForcefield();
                    sender.sendMessage(ChatColor.GOLD + "Forcefield disabled in " + world.getName() + "!");
                }
            }
            return true;
        }
        return false;
    }
}