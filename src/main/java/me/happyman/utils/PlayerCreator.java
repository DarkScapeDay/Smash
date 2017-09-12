package me.happyman.utils;

import me.happyman.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Random;

public class PlayerCreator implements CommandExecutor
{
    private static Random r = new Random();
    private static final String CREATE_PLAYER_CMD = "createplayer";
    private final Plugin plugin;

    public PlayerCreator(Plugin plugin)
    {
        this.plugin = plugin;
        plugin.setExecutor(CREATE_PLAYER_CMD, this);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (label.equalsIgnoreCase(CREATE_PLAYER_CMD))
        {
            if (!(sender instanceof Player) && args.length == 0)
            {
                sender.sendMessage(ChatColor.RED + "You must specify which world or which player you want to create the player.");
            }
            else if (args.length >= 1)
            {
                Player targetP = Bukkit.getPlayer(args[0]);
                if (targetP == null)
                {
                    sender.sendMessage(ChatColor.RED + "Error! Could not find " + args[0]);
                }
                else
                {
                    createFakePlayer(targetP);
                }
            }
            else
            {
                createFakePlayer(((Player)sender).getWorld());
            }
            return true;
        }
        return false;
    }
    public static Player createPlayer(Location l)
    {
        return null;
        //return new CraftPlayer((CraftServer)Bukkit.getServer(), new EntityPlayer());
    }

    public static  void createFakePlayer(Player targetP)
    {
        if (targetP.getWorld().getPlayers().size() > 0)
        {
            targetP.getWorld().getPlayers().get(r.nextInt(targetP.getWorld().getPlayers().size()));
        }
    }

    public static void createFakePlayer(World w)
    {

    }
}
