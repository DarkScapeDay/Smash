package me.happyman.worlds;

import me.happyman.commands.SmashManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.HashSet;

public class PortalListener implements Listener
{
    protected static final HashMap<Player, Integer> halfMaterializedPlayers = new HashMap<Player, Integer>();
    protected static final HashSet<Player> portalDwellers = new HashSet<Player>();

    public PortalListener()
    {
        Bukkit.getPluginManager().registerEvents(this, SmashManager.getPlugin());
    }

    @EventHandler
    public void listenToAllWhoMayEnter(PlayerMoveEvent e)
    {
        final Player p = e.getPlayer();
        for (final PortalManager.EventPortal portal : PortalManager.getAllPortals())
        {
            boolean isInPortal = portal.containsPlayer(p);
            if (isInPortal && !halfMaterializedPlayers.containsKey(p) && !portalDwellers.contains(p))
            {
                p.setVelocity(new Vector().zero());
                p.sendMessage(ChatColor.GREEN + "" + ChatColor.ITALIC + "Initiating...");
                if (portal.getTpDelayTicks() > 0)
                {
                    int task = Bukkit.getScheduler().scheduleSyncDelayedTask(SmashManager.getPlugin(), new Runnable()
                    {
                        public void run()
                        {
                            halfMaterializedPlayers.remove(p);
                            portal.performTeleportAction(p);
                        }
                    }, portal.getTpDelayTicks());
                    halfMaterializedPlayers.put(p, task);
                }
                else
                {
                    portal.performTeleportAction(p);
                }
                portalDwellers.add(p);
            }
            else if (!isInPortal && portalDwellers.contains(p))
            {
                portalDwellers.remove(p);
                if (halfMaterializedPlayers.containsKey(p))
                {
                    Bukkit.getScheduler().cancelTask(halfMaterializedPlayers.get(p));
                    halfMaterializedPlayers.remove(p);
                    p.sendMessage(ChatColor.YELLOW + "Teleport canceled! Sorry you changed your mind so soon.");
                }
            }

        }
    }
}
