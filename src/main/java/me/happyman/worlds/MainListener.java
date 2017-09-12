package me.happyman.worlds;

import me.happyman.SpecialItems.SmashKitMgt.SmashKitManager;
import me.happyman.utils.Verifier;
import org.bukkit.*;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.event.weather.WeatherChangeEvent;

import java.util.ArrayList;
import java.util.List;

import static me.happyman.Plugin.*;
import static me.happyman.worlds.WorldType.*;

public class MainListener implements Listener
{
    static final String FOOD_CMD = "food";
    static final String START_CMD = "start"; //This starts the game
    static final String SPECTATE_COMMAND = "spectate";
    static final String RESET_CMD = "reset"; //This resets a Smash game (don't use it during a game unless you're a jerk or there's a bug)
    static final String READY_UP_CMD = "ready";
    static final String VOTE_KICK_CMD = "votekick";
    static final String END_GAME_CMD = "end";
    static final String SPAWN_CMD = "spawn"; //This causes you to leave the game you're in
    static final String GET_MODE_CMD = "mode"; //Gets the mode of the Smash world you're in (point-based or deathmatch)
    static final String WARP_COMMAND = "warp"; //This creates a world

    private static final CommandExecutor playerCommandHandler = new CommandExecutor()
    {
        @Override
        public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
        {
            if (args.length > 0 && args[0].equalsIgnoreCase("help"))
            {
                return false;
            }
            if (sender instanceof Player)
            {
                return getWorldType(((Player)sender).getWorld()).handleCommand((Player)sender, label, args);
            }
            sender.sendMessage(ChatColor.RED + "You must be in-game to use this command!");
            return true;
        }
    };

    static
    {
        setExecutor(FOOD_CMD, playerCommandHandler);
        setExecutor(EconomyItemManager.BUY_CMD, playerCommandHandler, new TabCompleter()
        {
            @Override
            public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args)
            {
                List<String> results = new ArrayList<String>();
                if (args.length == 1)
                {
                    if (sender instanceof Player)
                    {
                        for (Player fellow : ((Player)sender).getWorld().getPlayers())
                        {
                            if (fellow.getName().toLowerCase().startsWith(args[0].toLowerCase()))
                            {
                                results.add(fellow.getName());
                            }
                        }
                    }
                    for (Material mat : Material.values())
                    {
                        if (args[0].length() > 0 && mat.name().startsWith(args[0].toUpperCase()) && !mat.equals(Material.AIR))
                        {
                            results.add(mat.name().toLowerCase());
                        }
                    }
                }
                return results;
            }
        });
        setExecutor(EconomyItemManager.SELL_CMD, playerCommandHandler, new TabCompleter() {
            @Override
            public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args)
            {
                List<String> result = new ArrayList<String>();
                if (args.length == 1)
                {
                    result.add("all");
                    result.add("half");
                }
                return result;
            }
        });

        setExecutor(EconomyItemManager.CONSOLIDATE_COMMAND, playerCommandHandler);
        setExecutor(SPECTATE_COMMAND, playerCommandHandler);
        setExecutor(RESET_CMD, playerCommandHandler);
        setExecutor(READY_UP_CMD, playerCommandHandler);
        setExecutor(VOTE_KICK_CMD, playerCommandHandler);
        setExecutor(END_GAME_CMD, playerCommandHandler);
        setExecutor(START_CMD, playerCommandHandler);
        setExecutor(GET_MODE_CMD, playerCommandHandler);
        setExecutor(SPAWN_CMD, playerCommandHandler);
        setExecutor(WARP_COMMAND, playerCommandHandler, new TabCompleter()
        {
            @Override
            public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args)
            {
                List<String> result = new ArrayList<String>();
                if (args.length == 1 && sender instanceof Player)
                {
                    for (WorldType.Warp warp : WorldType.Warp.getWarps((Player)sender, false))
                    {
                        result.add(warp.getName());
                    }
                    result.addAll(WorldType.Warp.warpCreateVariations);
                    result.addAll(WorldType.Warp.warpRemoveVariations);
                    result.add("list");
                }
                return result;
            }
        });
    }

    private static class SignClickHandler
    {
        private static abstract class SignAction
        {
            protected abstract void performAction(Player p);
        }

        private enum Signs //can change enum names, but not sign text
        {
            TOURNEY_SIGN("Tournaments", STUFFLAND, new SignAction()
            {
                @Override
                protected void performAction(Player p)
                {
                    WorldType.setTourneyPreferer(p, true);
                    sendPlayerToWorld(p, WorldType.SMASH.getBestWorld(p), true);
                }
            }),
            FREEPLAY_SIGN("Freeplay", STUFFLAND, new SignAction()
            {
                @Override
                protected void performAction(Player p)
                {
                    WorldType.setTourneyPreferer(p, false);
                    sendPlayerToWorld(p, WorldType.SMASH.getBestWorld(p), true);
                }
            }),
            META_SIGN("Meta World", STUFFLAND, new SignAction()
            {
                @Override
                protected void performAction(Player p)
                {
                    sendPlayerToWorld(p, WorldType.META.getBestWorld(p), true);
                }
            }),
            ALT_SPAWN_SIGN("Alternate Spawn", STUFFLAND, new SignAction()
            {
                @Override
                protected void performAction(Player p)
                {
                    forceTp(p, new Location(p.getWorld(), 34.5, 42.001, 1021.5));
                }
            });

            private final String textOnSign;
            private final SignAction action;
            private final WorldType worldType;

            Signs(String textOnSign, WorldType validType, SignAction action)
            {
                this.textOnSign = textOnSign;
                this.action = action;
                this.worldType = validType;
            }

            private static void performSignAction(Player player, BlockState block)
            {
                if (block instanceof Sign)
                {
                    Sign sign = (Sign)block;
                    WorldType clickedWorldType = getWorldType(sign.getWorld());
                    for (String s : sign.getLines())
                    {
                        for (Signs keyWordWithAction : values())
                        {
                            if (keyWordWithAction.action != null && keyWordWithAction.worldType == clickedWorldType
                                    && keyWordWithAction.textOnSign.equals(s))
                            {
                                keyWordWithAction.action.performAction(player);
                            }
                        }
                    }
                }
            }
        }

        private static void handleSignClick(PlayerInteractEvent e)
        {
            switch (e.getAction())
            {
                case LEFT_CLICK_BLOCK:
                case RIGHT_CLICK_BLOCK:
                    Signs.performSignAction(e.getPlayer(), e.getClickedBlock().getState());
            }
        }
    }

    public MainListener()
    {
        Bukkit.getPluginManager().registerEvents(this, getPlugin());
    }

    @EventHandler
    public void preventMobsTargettingOwners(EntityTargetLivingEntityEvent e)
    {
        if (e.getEntity().getCustomName() != null && e.getTarget() != null && ChatColor.stripColor(e.getEntity().getCustomName()).startsWith(e.getTarget().getName() + "'"))
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void preventItemDurabilityLoss(final PlayerItemDamageEvent e)
    {
        getWorldType(e.getPlayer().getWorld()).handlePlayerItemDamageEvent(e);
    }

    @EventHandler
    public static void damageEvent(EntityDamageEvent event)
    {
        if (event.getEntity() instanceof Player)
        {
            if (WorldType.isInSpectatorMode((Player)event.getEntity()))
            {
                event.setCancelled(true);
            }
        }
        getWorldType(event.getEntity().getWorld()).performDamage(event);
    }

    @EventHandler
    public static void damageByEntityEvent(EntityDamageByEntityEvent event)
    {
        //damageSource.getType().toString()
        getWorldType(event.getEntity().getWorld()).performEntityDamageByEntity(event);
    }

    @EventHandler
    public static void interactEvent(PlayerInteractEvent event)
    {
        SignClickHandler.handleSignClick(event);
        getWorldType(event.getPlayer().getWorld()).performPlayerInteract(event);
    }

    @EventHandler
    public void handlePlayerItemHeld(final PlayerItemHeldEvent event)
    {
        getWorldType(event.getPlayer().getWorld()).handlePlayerItemHeld(event);
    }

    @EventHandler
    public void handlePickupItem(EntityPickupItemEvent event)
    {
        getWorldType(event.getItem().getWorld()).handlePickupItem(event);
    }

    @EventHandler
    public void handleToggleSneak(PlayerToggleSneakEvent event)
    {
        getWorldType(event.getPlayer().getWorld()).handleToggleSneak(event);
    }

    @EventHandler
    public static void performProjectileHit(ProjectileHitEvent event)
    {
        getWorldType(event.getEntity().getWorld()).performProjectileHit(event);
    }

    @EventHandler
    public static void performCommandExecution(PlayerCommandPreprocessEvent event)
    {
        getWorldType(event.getPlayer().getWorld()).performCommand(event);
    }

    @EventHandler
    public void statCommandSenderForConsol(ServerCommandEvent e)
    {
        String commandSent = e.getCommand();
        if (commandSent.startsWith("stats"))
        {
            e.setCancelled(true);
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandSent.replaceAll("stats", "statistics"));
        }
    }

    @EventHandler
    public static void performBlockBreak(BlockBreakEvent event)
    {
        getWorldType(event.getBlock().getWorld()).performBlockBreak(event);
    }

    @EventHandler
    public static void performBlockPlace(BlockPlaceEvent event)
    {
        getWorldType(event.getBlock().getWorld()).performBlockPlace(event);
    }

    @EventHandler
    public static void performBlockExplode(EntityExplodeEvent event)
    {
        getWorldType(event.getEntity().getWorld()).performBlockExplode(event);
    }

    @EventHandler
    public static void performBlockBurn(BlockBurnEvent event)
    {
        getWorldType(event.getBlock().getWorld()).performBlockBurn(event);
    }

    @EventHandler
    public static void performPlayerMove(PlayerMoveEvent event)
    {
        getWorldType(event.getTo().getWorld()).performPlayerMove(event);
    }

    @EventHandler
    public void rainEvent(WeatherChangeEvent event)
    {
        getWorldType(event.getWorld()).performWeatherChange(event);
    }

    @EventHandler
    public void performPlayerInteractEntity(PlayerInteractEntityEvent event)
    {
        getWorldType(event.getPlayer().getWorld()).performPlayerInteractEntity(event);
    }

    @EventHandler
    public void deathEvent(PlayerRespawnEvent event)
    {
        getWorldType(event.getRespawnLocation().getWorld()).handleJoin(event.getPlayer(), true, event.getRespawnLocation().getWorld(), false);
    }

    @EventHandler
    public void dontDropTheItems(PlayerDropItemEvent event)
    {
        WorldType type = WorldType.getWorldType(event.getPlayer().getWorld());
        type.performDropItem(event);
    }

    //Maybe just have people tp to themselves on join
    @EventHandler
    public void playerJoins(PlayerJoinEvent event)
    {
        event.setJoinMessage("");
        final Player p = event.getPlayer();
        World w = WorldManager.getLastWorld(p);
        UUIDFetcher.savePlayerID(p.getName(), p.getUniqueId().toString().replaceAll("-", ""));

        //Bukkit.broadcastMessage("recorded last world: " + w);
        //Bukkit.broadcastMessage("current world: " + p.getWorld());
        if (w == null)
        {
            w = getFallbackWorld();
            sendPlayerToWorld(p, w, true);
//            getWorldType(w).handleJoin(p, true, w, false);
        }
        else
        {
            PlayerStateRecorder.loadPlayerState(p, p.getWorld());
            WorldType.handleWorldTransfer(p, null, p.getWorld(), false);
        }
    }

    @EventHandler
    public void playerLeaves(PlayerQuitEvent event)
    {
        event.setQuitMessage("");
        Player p = event.getPlayer();
        PlayerStateRecorder.rememberPlayerState(p, p.getWorld());
        SmashKitManager.unloadSelectedKit(p);
        SmashStatTracker.forgetOldElo(p.getName());
        Verifier.forcablyReleaseVerifier(p);
        UUIDFetcher.forgetPlayerID(p);
        GuiManager.forgetGui(p);
        WorldType.handleWorldTransfer(p, p.getWorld(), null, false);
    }

    @EventHandler
    public void performProjectileLaunch(ProjectileLaunchEvent event)
    {
        getWorldType(event.getEntity().getWorld()).performProjectileLaunch(event);
    }

    @EventHandler
    public void performBlockIgnite(BlockIgniteEvent event)
    {
        getWorldType(event.getBlock().getWorld()).performBlockIgnite(event);
    }

    @EventHandler
    public void preventAccumulation(BlockFormEvent event)
    {
        getWorldType(event.getBlock().getWorld()).performBlockForm(event);
    }

    @EventHandler
    public void preventLiquidPlacement(PlayerBucketEmptyEvent event)
    {
        getWorldType(event.getPlayer().getWorld()).performLiquidPlacement(event);
    }
//    @EventHandler
//    public void openSmashGuiByItem(PlayerInteractEvent event)
//    {
//        if ((event.getAction().equals(Action.RIGHT_CLICK_BLOCK) || event.getAction().equals(Action.RIGHT_CLICK_AIR))
//                && WorldType.isGuiOpener(event.getSpecialItem()))
//        {
//            WorldType.openSmashGui(event.getAttacker());
//        }
//    }

    public static boolean sendPlayerToWorld(final Player p, World w2, boolean forcefully)
    {
        return getWorldType(w2).sendPlayerToWorld(p, w2, forcefully, false);
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
                Player playerToTeleport = e.getAttacker();
                Player playerToTeleportTo = Bukkit.getAttacker(arg1);
                if (playerToTeleportTo != null)
                {
                    if (s.hasNext())
                    {
                        String arg2 = s.next();
                        playerToTeleport = playerToTeleportTo;
                        playerToTeleportTo = Bukkit.getAttacker(arg2);
                    }
                    if (playerToTeleportTo != null)
                    {
                        World fromWorld = playerToTeleport.getWorld();
                        World toWorld = playerToTeleportTo.getWorld();
                        if (!fromWorld.equals(toWorld) && (SmashWorldManager.isSmashWorld(playerToTeleport.getWorld()) || SmashWorldManager.isSmashWorld(playerToTeleportTo.getWorld())))
                        {
                            e.setCancelled(true);
                            e.getAttacker().sendMessage("Teleported " + playerToTeleport.getDisplayName() + " to " + playerToTeleportTo.getDisplayName() + ", and at least one of them was in a Smash world");
                            SmashWorldManager.sendPlayerToWorld(playerToTeleport, playerToTeleportTo.getWorld());
                            playerToTeleport.teleport(playerToTeleportTo);
                        }
                    }
                }
            }
        }
    }*/

    @EventHandler
    public static void preventConsolodation(ItemMergeEvent event)
    {
        getWorldType(event.getEntity().getWorld()).performConsolidate(event);
    }

    @EventHandler
    public static void changeWorld(PlayerChangedWorldEvent e)
    {
        Player p = e.getPlayer();
        World oldWorld = e.getFrom();
        World newWorld = e.getPlayer().getWorld();
        WorldType.handleWorldTransfer(p, oldWorld, newWorld, false);
    }

    @EventHandler
    public static void handleDeath(PlayerDeathEvent e)
    {
        getWorldType(e.getEntity().getWorld()).handlePlayerDeath(e);
    }

    @EventHandler
    public static void handlePlayerTeleport(final PlayerTeleportEvent event)
    {
        WorldType.handlePlayerTeleportEvent(event);
    }

    /*@EventHandler
    public void dontClose(InventoryCloseEvent e)
    {
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, getCloseRunnable(e), 2);
    }

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
    }*/

    /*@EventHandler
    public void melonLoad(ChunkLoadEvent e)
    {
        Chunk chunk = e.getChunk();
        World world = e.getWorld();
        if (world.getDisplayName().equals(CustomWorldGenerator.META_WORLD_NAME))
        {
            int bx = chunk.getX() << 4;
            int bz = chunk.getZ() << 4;
            //Bukkit.broadcastMessage("bz:" + bz + " c.getZ(): " + c.getZ());
            for (int xx = bx; xx < bx + 16; xx++)
            {
                for (int zz = bz; zz < bz + 16; zz++)
                {
                    for (int yy = 0; yy < 128; yy++)
                    {
                        int typeId = world.getBlockTypeIdAt(xx, yy, zz);
                        if (typeId == 0)
                        {
                            //Bukkit.broadcastMessage("Found 1");
                            // world.getBlockAt(xx, yy, zz).setType(Material.STONE);
                        }
                    }
                }
            }
        }
    }*/

}
