package me.happyman.Listeners;

import me.happyman.commands.SmashManager;
import me.happyman.SmashKitMgt.SmashKitManager;
import me.happyman.utils.SmashStatTracker;
import me.happyman.utils.SmashWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.inventory.ItemStack;

public class SmashWorldListener implements Listener
{

    public static ItemStack SMASH_GUI_ITEM = SmashManager.getPlugin().getCustomItemStack(Material.BRICK, ChatColor.YELLOW + "" + ChatColor.BOLD + "Smash Worlds");;

    public SmashWorldListener()
    {
        Bukkit.getPluginManager().registerEvents(this, SmashManager.getPlugin());
    }

    public static void displayJoinMessage(Player p)
    {
        SmashManager.getPlugin().getServer().getScheduler().scheduleSyncDelayedTask(SmashManager.getPlugin(), getJoinRunnable(p), 2);
    }

    private static Runnable getJoinRunnable(final Player joiner)
    {
        return new Runnable()
        {
            public void run()
            {
                for (Player player : SmashManager.getPlugin().getServer().getOnlinePlayers())
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
                SmashManager.getPlugin().putPluginDatum(joiner, "Name", joiner.getName());
                SmashManager.getPlugin().putPluginDatum(joiner, "IP", joiner.getAddress().toString().substring(1, joiner.getAddress().toString().length()));
                int logins;
                try
                {
                    logins = Math.round(Float.valueOf(SmashManager.getPlugin().getPluginDatum(joiner, "Total logins")));
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
                    SmashManager.getPlugin().getServer().getConsoleSender().sendMessage(ChatColor.YELLOW + "The server welcomes "
                            + joiner.getDisplayName() + ChatColor.YELLOW + " with open arms!");

                    joiner.sendMessage(ChatColor.GOLD + "Welcome! Do /smash to join a Smash game!");
                    if (SmashStatTracker.getFreeGamesPlayed(joiner) < SmashWorldManager.MIN_FREE_GAMES_TO_JOIN_TOURNEYS)
                    {
                        joiner.sendMessage(ChatColor.AQUA + "You'll be able to play in Tournament games after you play through " + (SmashWorldManager.MIN_FREE_GAMES_TO_JOIN_TOURNEYS + SmashStatTracker.getFreeGamesPlayed(joiner))
                                + " more Freeplay games.");
                    }
                    SmashStatTracker.displayHelpMessage(joiner);
                }
                else
                {
                    SmashManager.getPlugin().getServer().getConsoleSender().sendMessage(joiner.getDisplayName()
                            + ChatColor.GRAY + " (" + joiner.getUniqueId() + ") has joined the game!");
                    if (logins % 1000 == 0)
                    {
                        Bukkit.broadcastMessage(ChatColor.GOLD + "" + ChatColor.MAGIC + "W" + ChatColor.RESET +
                                " " + ChatColor.GREEN + "CONGRATULATIONS " +
                                joiner.getDisplayName().toUpperCase() + ChatColor.RESET + "" + ChatColor.GREEN +
                                " ON YOUR " + ChatColor.BOLD + logins + ChatColor.RESET + "" + ChatColor.GREEN +
                                SmashManager.getPlugin().getOrdinalIndicator(logins) + " LOGIN!!!! " + ChatColor.BOLD + ";D " +
                                ChatColor.RESET + ChatColor.GOLD + ChatColor.MAGIC + "W");
                        joiner.setItemInHand(new ItemStack(Material.DIAMOND_BLOCK, 64));
                    }
                    else if (logins % 100 == 0)
                    {
                        joiner.sendMessage(ChatColor.GREEN + "Congratulations on your " + logins +
                                SmashManager.getPlugin().getOrdinalIndicator(logins) + " login! Wow!");
                    }
                    else if (logins % 10 == 0)
                    {
                        joiner.sendMessage(ChatColor.GREEN + "Greetings! This is your your " + logins +
                                SmashManager.getPlugin().getOrdinalIndicator(logins) + " login :)");
                    }
                    else
                    {
                        joiner.sendMessage(ChatColor.GREEN + "Greetings!");
                    }
                }
                SmashManager.getPlugin().putPluginDatum(joiner, "Total logins", logins);
            }
        };
    }

    @EventHandler
    public void deathEvent(PlayerRespawnEvent e)
    {
        SmashWorldManager.resetPlayer(e.getPlayer());
    }

    @EventHandler
    public void warpHandler(InventoryClickEvent e)
    {
        ItemStack itemClicked = e.getCurrentItem();
        if (itemClicked != null && itemClicked.hasItemMeta() && itemClicked.getItemMeta().hasDisplayName() && e.getWhoClicked() instanceof Player)
        {
            Player p = (Player) e.getWhoClicked();

            if (SMASH_GUI_ITEM != null && itemClicked.equals(SMASH_GUI_ITEM))
            {
                SmashWorldManager.openWorldGui(p);
            }
            else if (SmashWorldManager.getWorldRepresented(itemClicked) != null)
            {
                p.closeInventory();
                SmashWorldManager.sendPlayerToWorld(p, SmashWorldManager.getWorldRepresented(itemClicked));
                e.setCancelled(true);
            }
            else if (itemClicked.equals(SmashWorldManager.getFreeplayWorldCreationItem()) || itemClicked.equals(SmashWorldManager.getTourneyWorldCreationItem(SmashStatTracker.getTourneyLevel(p))))
            {
                p.closeInventory();
                if (itemClicked.equals(SmashWorldManager.getTourneyWorldCreationItem(SmashStatTracker.getTourneyLevel(p))))
                {
                    SmashWorldManager.createNextTourneyWorld(p);
                }
                else
                {
                    SmashWorldManager.createNextFreeplayWorld(p);
                }
                e.setCancelled(true);
            }
        }
    }

    //Maybe just have people tp to themselves on join
    @EventHandler
    public void playerJoins(PlayerJoinEvent e)
    {
        e.setJoinMessage("");
        final Player p = e.getPlayer();
        if (SmashWorldManager.getLastPlayerLocation(p) == null)
        {
            SmashWorldManager.sendPlayerToWorld(p, SmashWorldManager.getFallbackWorld());
        }
        SmashWorldManager.handleJoin(p, p.getWorld(), true, false);
    }

    @EventHandler
    public void playerLeaves(PlayerQuitEvent e)
    {
        e.setQuitMessage("");
        Player p = e.getPlayer();
        SmashStatTracker.addScoreToTotalPoints(p);
        SmashKitManager.unloadSelectedKit(p);
        SmashStatTracker.forgetOldElo(p.getName());
        SmashWorldManager.handleLeave(p, p.getWorld(), true, false);
    }

    @EventHandler
    public void openSmashGuiByItem(PlayerInteractEvent e)
    {
        if ((e.getAction().equals(Action.RIGHT_CLICK_BLOCK) || e.getAction().equals(Action.RIGHT_CLICK_AIR))
                && SMASH_GUI_ITEM != null && e.getItem() != null && e.getItem().equals(SMASH_GUI_ITEM))
        {
            SmashWorldManager.openWorldGui(e.getPlayer());
        }
    }

    /* @EventHandler
    public void preventDirectTp(PlayerCommandPreprocessEvent e)
    {
        String commandSent = e.getMessage().substring(1, e.getMessage().length()).toLowerCase();
        if (commandSent.startsWith("tp "))
        {
            Scanner s = new Scanner(commandSent);
            s.next();
            if (s.hasNext())
            {
                String arg1 = s.next();
                Player playerToTeleport = e.getPlayer();
                Player playerToTeleportTo = Bukkit.getPlayer(arg1);
                if (playerToTeleportTo != null)
                {
                    if (s.hasNext())
                    {
                        String arg2 = s.next();
                        playerToTeleport = playerToTeleportTo;
                        playerToTeleportTo = Bukkit.getPlayer(arg2);
                    }
                    if (playerToTeleportTo != null)
                    {
                        World fromWorld = playerToTeleport.getWorld();
                        World toWorld = playerToTeleportTo.getWorld();
                        if (!fromWorld.equals(toWorld) && (SmashWorldManager.isSmashWorld(playerToTeleport.getWorld()) || SmashWorldManager.isSmashWorld(playerToTeleportTo.getWorld())))
                        {
                            e.setCancelled(true);
                            e.getPlayer().sendMessage("Teleported " + playerToTeleport.getName() + " to " + playerToTeleportTo.getName() + ", and at least one of them was in a Smash world");
                            SmashWorldManager.sendPlayerToWorld(playerToTeleport, playerToTeleportTo.getWorld());
                            playerToTeleport.teleport(playerToTeleportTo);
                        }
                    }
                }
            }
        }
    }*/



    @EventHandler
    public void changeWorld(PlayerChangedWorldEvent e)
    {
        Player p = e.getPlayer();
        World oldWorld = e.getFrom();
        World newWorld = e.getPlayer().getWorld();
        SmashWorldManager.handleLeave(p, oldWorld, false, false);
        SmashWorldManager.handleJoin(p, newWorld, false, false);
    }

/*
    @EventHandler
    public void dontClose(InventoryCloseEvent e)
    {
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, getCloseRunnable(e), 2);
    }
*/
/*
    @EventHandler
    public void deathEvent(PlayerTeleportEvent e)
    {
        if (e.getEntity() instanceof Player)
        {
            Player p = (Player)e.getEntity();
            Player killer = (Player)e.getLastHitter
            if (isSmashWorld(p.getWorld()))
            {

                e.setDeathMessage(p );
            }
        }
    }
*/

//    @EventHandler
//    public void melonLoad(ChunkLoadEvent e)
//    {
//        Chunk c = e.getChunk();
//        World world = e.getWorld();
//        int bx = c.getX() << 4;
//        int bz = c.getZ() << 4;
//        //Bukkit.broadcastMessage("bz:" + bz + " c.getZ(): " + c.getZ());
//        for (int xx = bx; xx < bx + 16; xx++)
//        {
//            for (int zz = bz; zz < bz + 16; zz++)
//            {
//                for (int yy = 0; yy < 128; yy++)
//                {
//                    int typeId = world.getBlockTypeIdAt(xx, yy, zz);
//                    if (typeId == 103)
//                    {
//                        //Bukkit.broadcastMessage("Found 1");
//                       // world.getBlockAt(xx, yy, zz).setType(Material.STONE);
//                    }
//                }
//            }
//        }
//    }
}
