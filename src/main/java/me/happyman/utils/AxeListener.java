package me.happyman.utils;

import me.happyman.source;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import java.util.HashMap;

public class AxeListener implements Listener
{
    private final source plugin;
    private static HashMap<Player, Block> pos2Selections = new HashMap<Player, Block>();
    private static HashMap<Player, Block> pos1Selections = new HashMap<Player, Block>();

    public AxeListener(source plugin)
    {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
    }

    public static boolean hasSelections(Player p)
    {
        return pos2Selections.containsKey(p) && pos1Selections.containsKey(p);
    }

    public static Block getPos1(Player p)
    {
        return pos1Selections.get(p);
    }

    public static Block getPos2(Player p)
    {
        return pos2Selections.get(p);
    }

    @EventHandler
    public void onClickAxe(PlayerInteractEvent e)
    {
        Action action = e.getAction();
        Player p = e.getPlayer();
        if (e.getItem() != null && e.getItem().getType().equals(Material.GOLD_AXE) && (action.equals(Action.RIGHT_CLICK_BLOCK) || action.equals(Action.LEFT_CLICK_BLOCK)) && p.hasPermission("minecraft.command.op") && p.getGameMode().equals(GameMode.CREATIVE))
        {
            e.setCancelled(true);
            HashMap<Player, Block> posToSelect = null;
            Block pos = e.getClickedBlock().getRelative(0, 1, 0);
            if (A_StarSearch.isAllowedAStarBlock(pos))
            {
                String message = "";
                if (action.equals(Action.RIGHT_CLICK_BLOCK))
                {
                    message = ChatColor.GREEN + "Set destination for A*";
                    posToSelect = pos2Selections;
                }
                else if (action.equals(Action.LEFT_CLICK_BLOCK))
                {
                    message = ChatColor.GREEN + "Set start location for A*";
                    posToSelect = pos1Selections;
                }
                if (posToSelect != null)
                {
                    posToSelect.put(p, pos);
                    if (pos1Selections.containsKey(p) && pos2Selections.containsKey(p))
                    {
                        if (pos2Selections.get(p).getWorld() != pos1Selections.get(p).getWorld())
                        {
                            if (posToSelect == pos1Selections)
                            {
                                pos2Selections.remove(p);
                            }
                            else
                            {
                                pos1Selections.remove(p);
                            }
                        }
                        else if (A_StarSearch.tooFarForParkour(pos1Selections.get(p), pos2Selections.get(p)))
                        {
                            message += " (" + ChatColor.LIGHT_PURPLE + "too far for parkour " + ChatColor.GREEN + ")";
                        }
                    }
                }
                else
                {
                    message = ChatColor.RED + "Internal error has occurred";
                }
                p.sendMessage(message + ".");
            }
            else
            {
                p.sendMessage(ChatColor.GRAY + "You have to select somewhere that has non-solid above it.");
            }
        }
    }

}
