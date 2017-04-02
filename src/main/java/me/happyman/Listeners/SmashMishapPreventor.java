package me.happyman.Listeners;

import me.happyman.SmashItemDrops.Hammer;
import me.happyman.SmashItemDrops.ItemDropManager;
import me.happyman.SmashKitMgt.SmashKitManager;
import me.happyman.commands.HealAndDamage;
import me.happyman.commands.SmashManager;
import me.happyman.source;
import me.happyman.utils.SmashStatTracker;
import me.happyman.utils.SmashWorldManager;
import me.happyman.utils.Verifier;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.*;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.*;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.projectiles.ProjectileSource;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Scanner;

public class SmashMishapPreventor implements Listener
{
    private SmashManager smashManager;
    private source plugin;
    private static HashMap<Player, HashMap<ItemStack, Integer>> repairTasks;

    public SmashMishapPreventor(SmashManager smashManager, source plugin)
    {
        this.smashManager = smashManager;
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        repairTasks = new HashMap<Player, HashMap<ItemStack, Integer>>();
    }


    @EventHandler
    public void projLaunch(ProjectileLaunchEvent e)
    {
        ProjectileSource shooter = e.getEntity().getShooter();
        if (shooter instanceof Player && SmashWorldManager.isInSpectatorMode((Player)shooter) && !SmashWorldManager.hasSpecialPermissions((Player)shooter))
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void preventLiquidPlacement(PlayerBucketEmptyEvent e)
    {
        if (SmashWorldManager.isSmashWorld(e.getPlayer().getWorld()))
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void preventHunger(FoodLevelChangeEvent e)
    {
        if (e.getEntity() instanceof Player)
        {
            Player p = (Player) e.getEntity();
            if (SmashWorldManager.isSmashWorld(p.getWorld()) || SmashWorldManager.isInSpectatorMode(p))
            {
                e.setCancelled(true);
                if (p.getFoodLevel() < 20)
                {
                    p.setFoodLevel(20);
                }
            }
        }
    }

    @EventHandler
    public void dontLetPeopleOpenDoors(PlayerInteractEvent e)
    {
        if (SmashWorldManager.isSmashWorld(e.getPlayer().getWorld()) && e.getAction().equals(Action.RIGHT_CLICK_BLOCK) && e.getClickedBlock().getType().toString().toLowerCase().contains("door"))
        {
            e.getPlayer().sendMessage(ChatColor.RED + "The door is locked!");
            e.setCancelled(true);

            for (Player p : e.getPlayer().getWorld().getPlayers())
            {
                if (p.getLocation().getBlock().getType().toString().toLowerCase().contains("door"))
                {
                    if (!p.equals(e.getPlayer()))
                    {
                        p.teleport(e.getPlayer());
                    }
                    else
                    {
                        Location l = p.getLocation();
                        Vector x = l.getDirection();
                        p.teleport(new Location(p.getWorld(), l.getX() + x.getX(), l.getY(), l.getZ() + x.getZ()));
                    }
                }
            }
        }
    }

    @EventHandler
    public void preventChickenSpawns(CreatureSpawnEvent e)
    {
        if (SmashWorldManager.isSmashWorld(e.getEntity().getWorld()) && e.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.EGG) && e.getEntity() instanceof Chicken)
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void preserveItemspawns(ItemDespawnEvent e)
    {
        World w = e.getEntity().getWorld();
        if (SmashWorldManager.isSmashWorld(w) && ItemDropManager.isSmashDropItem(e.getEntity().getItemStack()))
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void preventItemExplosions(final EntityDamageByEntityEvent e)
    {
        if (e.getEntity() instanceof Item && SmashWorldManager.isSmashWorld(e.getEntity().getWorld())
                && ItemDropManager.isSmashDropItem(((Item)e.getEntity()).getItemStack()))
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void preventBlockBreaking(BlockBreakEvent e)
    {
        if (SmashWorldManager.isSmashWorld(e.getBlock().getWorld()))
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void preventBlockPlacing(final BlockPlaceEvent e)
    {
        if (SmashWorldManager.isSmashWorld(e.getPlayer().getWorld()) || e.getItemInHand().equals(SmashWorldListener.SMASH_GUI_ITEM))
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void preventLiquidTaking(PlayerBucketFillEvent e)
    {
        if (SmashWorldManager.isSmashWorld(e.getPlayer().getWorld()))
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void statAndHelpAndReadyCommandSender(PlayerCommandPreprocessEvent e)
    {
        String commandSent = e.getMessage().substring(1, e.getMessage().length()).toLowerCase();
        Scanner s = new Scanner(commandSent);
        if (s.hasNext())
        {
            String label = s.next();
            if (label.equals("stats"))
            {
                e.setCancelled(true);
                e.getPlayer().performCommand(commandSent.replaceAll("stats", "statistics"));
            }
            else if (label.equals("help"))
            {
                e.setCancelled(true);
                SmashStatTracker.displayHelpMessage(e.getPlayer());
            }
            else if (label.equals("start") && !SmashWorldManager.hasSpecialPermissions(e.getPlayer()))
            {
                e.setCancelled(true);
                Bukkit.dispatchCommand(e.getPlayer(), "ready");
            }
            else if (plugin.matchesCommand(label, HealAndDamage.DAMAGE_CMD_NAME))
            {
                Player p = e.getPlayer();
                if (s.hasNext())
                {
                    String pl = s.next();
                    if (Bukkit.getPlayer(pl) != null)
                    {
                        p = Bukkit.getPlayer(pl);
                    }
                }
                if (SmashWorldManager.isSmashWorld(p.getWorld()))
                {
                    e.setCancelled(true);
                    e.getPlayer().sendMessage(ChatColor.RED + "That player is in a Smash world and cannot be harmed! Try /dmg!");
                }
            }
        }
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
    public void dontDropTheItems(PlayerDropItemEvent e)
    {
        if (SmashWorldManager.isSmashWorld(e.getPlayer().getWorld())
                && e.getItemDrop().getItemStack().hasItemMeta()
                && e.getItemDrop().getItemStack().getItemMeta().hasDisplayName()
                || SmashWorldListener.SMASH_GUI_ITEM != null && e.getItemDrop().getItemStack().equals(SmashWorldListener.SMASH_GUI_ITEM))
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void dontBurnTheUndeadsBurnThePlayers(EntityCombustEvent e)
    {
        EntityType entityType = e.getEntityType();
        World w = e.getEntity().getWorld();
        if(SmashWorldManager.isSmashWorld(w) && (entityType.equals(EntityType.ZOMBIE) || entityType.equals(EntityType.SKELETON)
                    || e.getEntity() instanceof Player && SmashKitManager.isUsingFireImmuneKit((Player)e.getEntity())))
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void dontDropExp(EntityDeathEvent e)
    {
        EntityType entityType = e.getEntityType();
        if (SmashWorldManager.isSmashWorld(e.getEntity().getWorld()))
        {
            e.setDroppedExp(0);
            e.getDrops().clear();
        }
    }

    @EventHandler
    public void preventItemDurabilityLoss(final PlayerItemDamageEvent e)
    {
        //Bukkit.broadcastMessage("item damage event");
        if (SmashWorldManager.isSmashWorld(e.getPlayer().getWorld()))
        {
          //  e.setCancelled(true);
            repairItem(e.getPlayer(), e.getItem());
            e.getItem().setDurability((short)-1);///(short)-1);

            //smashManager.getSmashItemManager().getKitManager().setToKitItems(e.getPlayer(), true);
        }
    }

    public static void cancelRepairs(Player p)
    {
        if (repairTasks.containsKey(p))
        {
            for (int task : repairTasks.get(p).values())
            {
                Bukkit.getScheduler().cancelTask(task);
            }
            repairTasks.remove(p);
        }
    }

    public static void repairItem(final Player p, final ItemStack item)
    {
        if (!repairTasks.containsKey(p))
        {
            repairTasks.put(p, new HashMap<ItemStack, Integer>());
        }
        if (repairTasks.containsKey(p) && !repairTasks.get(p).containsKey(item))
        {
            repairTasks.get(p).put(item, Bukkit.getScheduler().scheduleSyncDelayedTask(SmashManager.getPlugin(), new Runnable() {
                public void run()
                {
                    if ( //&& SmashKitManager.isKitItem(p, item)// && p.getItemInHand().hasItemMeta() && p.getItemInHand().getItemMeta().hasDisplayName() &&
                        !p.getItemInHand().equals(item) && p.getInventory().contains(item) && !SmashWorldManager.isDead(p))
                    {
                        item.setDurability((short)-1);
                        /*Bukkit.getScheduler().callSyncMethod(SmashItemManager.getPlugin(), new Callable() {
                            public String call()
                            {
                                p.updateInventory();
                                return "";
                            }
                        });*/
                    }
                    repairTasks.get(p).remove(item);
                }
            }, 60));
        }
    }

    @EventHandler
    public void preventDMs(PlayerDeathEvent e)
    {
        String message = e.getDeathMessage();
        e.setDeathMessage("");
        for (Player p : Bukkit.getOnlinePlayers())
        {
            if (!SmashWorldManager.isSmashWorld(p.getWorld()))
            {
                p.sendMessage(message);
            }
        }
    }


    @EventHandler
    public void preventBlockExplosions(final EntityExplodeEvent e)
    {
        if (SmashWorldManager.isSmashWorld(e.getEntity().getWorld()))
        {
            e.blockList().clear();
            /*final ArrayList<Material> oldMatList = new ArrayList<Material>();
            for (int i = 0; i < e.blockList().size(); i++)
            {
                oldMatList.add(e.blockList().get(i).getType());
            }
            Callable r = new Callable()
            {
                public String call()
                {
                    for (int i = 0; i < e.blockList().size() && Bukkit.getWorlds().contains(e.getEntity().getWorld()); i++)
                    {
                        e.blockList().get(i).getLocation().getBlock().setType(oldMatList.get(i));
                    }
                    return "";
                }
            };
            plugin.getServer().getScheduler().callSyncMethod(plugin, r);*/
        }
    }

    private void checkBlock(Block b)
    {
        if (b.getType().equals(Material.FIRE))
        {
            b.setType(Material.AIR);
        }
    }

    @EventHandler
    public void preventBlockBurning(BlockBurnEvent e)
    {
        if (SmashWorldManager.isSmashWorld(e.getBlock().getLocation().getWorld()))
        {
            e.setCancelled(true);
            checkBlock(e.getBlock().getRelative(0, 1, 0));
            /*checkBlock(e.getBlock().getRelative(0, 0, -1));
            checkBlock(e.getBlock().getRelative(0, 0, 1));
            checkBlock(e.getBlock().getRelative(0, -1, 0));
            checkBlock(e.getBlock().getRelative(0, 1, 0));
            checkBlock(e.getBlock().getRelative(-1, 0, 0));
            checkBlock(e.getBlock().getRelative(1, 0, 0));


            checkBlock(e.getBlock().getRelative(0, 1, 1));
            checkBlock(e.getBlock().getRelative(0, -1, 1));
            checkBlock(e.getBlock().getRelative(1, -1, 0));
            checkBlock(e.getBlock().getRelative(-1, 1, 0));
            checkBlock(e.getBlock().getRelative(1, 0, -1));
            checkBlock(e.getBlock().getRelative(-1, 0, 1));

            checkBlock(e.getBlock().getRelative(1, 1, 1));
            checkBlock(e.getBlock().getRelative(1, 1, -1));
            checkBlock(e.getBlock().getRelative(1, -1, 1));
            checkBlock(e.getBlock().getRelative(1, -1, -1));
            checkBlock(e.getBlock().getRelative(-1, 1, 1));
            checkBlock(e.getBlock().getRelative(-1, 1, -1));
            checkBlock(e.getBlock().getRelative(-1, -1, 1));
            checkBlock(e.getBlock().getRelative(-1, -1, -1));*/
        }
    }

    @EventHandler
    public void preventFireSpread(BlockIgniteEvent e)
    {
        if (e.getCause().equals(BlockIgniteEvent.IgniteCause.SPREAD) && SmashWorldManager.isSmashWorld(e.getBlock().getWorld()))
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void preventConsolodation(ItemMergeEvent e)
    {
        e.setCancelled(true);
    }

    @EventHandler
    public void smashChatControl(AsyncPlayerChatEvent e)
    {
        Player p = e.getPlayer();

        String messageSent = "<" + p.getDisplayName() + "> " + e.getMessage();

        e.setCancelled(true);
        if (!Hammer.isWieldingHammer(p) && !Verifier.isVerifier(p))
        {
            for (Player player : p.getWorld().getPlayers())
            {
                player.sendMessage(messageSent);
            }
        }
    }
}
