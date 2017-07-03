package me.happyman.utils;

import me.happyman.source;
import net.minecraft.server.v1_8_R3.EntityPlayer;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.conversations.Conversation;
import org.bukkit.conversations.ConversationAbandonedEvent;
import org.bukkit.craftbukkit.v1_8_R3.CraftServer;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.*;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.*;
import org.bukkit.map.MapView;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.permissions.PermissionAttachmentInfo;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scoreboard.Scoreboard;
import org.bukkit.util.*;
import org.bukkit.util.Vector;

import java.net.InetSocketAddress;
import java.util.*;

public class PlayerCreator implements CommandExecutor
{
    private static Random r = new Random();
    private static final String CREATE_PLAYER_CMD = "createplayer";
    private final source plugin;

    public PlayerCreator(source plugin)
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
