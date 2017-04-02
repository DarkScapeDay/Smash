package me.happyman.SmashItemDrops;

import me.happyman.ItemTypes.SmashItemWithTask;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashItemManager;
import me.happyman.utils.SmashWorldManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

public class Hammer extends SmashItemWithTask implements Listener
{
    private final List<Integer> cs;
    private final List<Integer> es;
    private final List<Integer> gs;
    private final HashMap<Sound, Float> instruments;
    private Random r;

    private final int HAMMER_DURATION = 14; //Seconds
    private final int TEMPO_MOD = 1;
    private final int TOTAL_NOTES = 33*TEMPO_MOD;

    public Hammer()
    {
        super(Material.IRON_AXE, ChatColor.GOLD + "" + ChatColor.BOLD + "HAMMER");

        r = new Random();
        instruments = new HashMap<Sound, Float>();

        instruments.put(Sound.NOTE_BASS, 2F);
        instruments.put(Sound.NOTE_PIANO, 2F);
        instruments.put(Sound.CHICKEN_EGG_POP, 2F);
        instruments.put(Sound.ANVIL_LAND, 1.35F);
        instruments.put(Sound.BURP, 2F);

        cs = new ArrayList<Integer>();
        es = new ArrayList<Integer>();
        gs = new ArrayList<Integer>();

        cs.add(0*TEMPO_MOD);

        cs.add(2*TEMPO_MOD);
        cs.add(3*TEMPO_MOD);
        cs.add(4*TEMPO_MOD);

        cs.add(6*TEMPO_MOD);

        es.add(8*TEMPO_MOD);

        cs.add(10*TEMPO_MOD);

        es.add(12*TEMPO_MOD);

        cs.add(14*TEMPO_MOD);

        es.add(16*TEMPO_MOD);

        es.add(18*TEMPO_MOD);
        es.add(19*TEMPO_MOD);
        es.add(20*TEMPO_MOD);

        es.add(22*TEMPO_MOD);

        gs.add(25*TEMPO_MOD);

        es.add(27*TEMPO_MOD);

        gs.add(29*TEMPO_MOD);

        es.add(31*TEMPO_MOD);

        Bukkit.getPluginManager().registerEvents(this, SmashManager.getPlugin());
    }

    @Override
    public void cancelTask(Player p)
    {
        if (isWieldingHammer(p))
        {
            super.cancelTask(p);
            SmashItemManager.resetSpecialWielder(p);
        }
        else
        {
            SmashManager.getPlugin().sendErrorMessage("Error! Tried to reset hammer items for " + p.getName() + " but we already did!");
        }
    }

    @Override
    public void activateTask(final Player p)
    {
        if (!isWieldingHammer(p))
        {
            p.setAllowFlight(false);
            final World w = p.getWorld();

            SmashItemManager.setSpecialWielder(p, this);
            p.getInventory().clear();
            for (int i = 0; i < 9; i++)
            {
                p.getInventory().setItem(i, getItem());
            }
            p.setExp(1);
            int max_cycles = HAMMER_DURATION*20 + 20;
            final int adjusted_cycles = max_cycles - max_cycles % TOTAL_NOTES;
            List<Sound> sounds = new ArrayList<Sound>(instruments.keySet());
            final Sound randomSound = sounds.get(r.nextInt(sounds.size()));

            putTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(),
                    new Runnable()
                    {
                        int i = 0;
                        public void run()
                        {
                            if (SmashItemManager.isSpecialWielder(p))
                            {
                                if (i == adjusted_cycles || !SmashWorldManager.gameHasStarted(w) || SmashWorldManager.gameHasEnded(w))
                                {
                                    cancelTask(p);
                                }
                                else
                                {
                                    p.setExp(1 - (float)1.0*(1+i)/adjusted_cycles);
                                    int note = i % TOTAL_NOTES;
                                    float pitch = 0;
                                    if (cs.contains(note))
                                    {
                                        pitch = (float)0.707; //C
                                    }
                                    else if (es.contains(note))
                                    {
                                        pitch = (float)0.891;
                                    }
                                    else if (gs.contains(note))
                                    {
                                        pitch = (float)1.059;
                                    }
                                    if (pitch != 0)
                                    {
                                        float iterations = instruments.get(randomSound);
                                        float volume = iterations%1;
                                        if (volume == 0)
                                        {
                                            volume = 1F;
                                        }
                                        for (int i = 0; i < iterations; i++)
                                        {
                                            p.getWorld().playSound(p.getLocation(), randomSound, volume, pitch); //Halloween: pitch*0.5F
                                        }
                                    }
                                    i++;
                                }
                            }
                        }
                    } ,0, 1));

        }
        else
        {
            SmashManager.getPlugin().sendErrorMessage("Error! We already did have a Hammer task for " + p.getName());
        }
    }

    @EventHandler
    public void pickupItem(PlayerPickupItemEvent e)
    {
        final Player p = e.getPlayer();
        final World w = p.getWorld();

        if (SmashWorldManager.isSmashWorld(w) && !isWieldingHammer(p) && !SmashWorldManager.isInSpectatorMode(p) && SmashWorldManager.gameHasStarted(w) && !SmashWorldManager.gameHasEnded(w) && isThis(e.getItem().getItemStack()))
        {
            e.setCancelled(true);
            e.getItem().remove();
            activateTask(e.getPlayer());

            Block block = e.getItem().getLocation().getBlock();
            if (block.getType().equals(ItemDropManager.MATERIAL_FOR_ITEM_DROPS))
            {
                block.setType(Material.AIR);
            }
        }
    }

    @EventHandler
    public void preventHammerCrafting(CraftItemEvent e)
    {
        if (SmashItemManager.isSpecialWielder((Player)e.getWhoClicked()) || ((Player)e.getWhoClicked()).getInventory().contains(getItem().getType()))
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void dontDropTheItems(final PlayerDropItemEvent e)
    {
        if (inventoryContains(e.getPlayer()))
        {
            e.getItemDrop().setItemStack(new ItemStack(Material.STONE));
            Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
                public String call() {
                    e.getPlayer().getInventory().remove(Material.STONE);
                    return "";
                }
            });
        }
    }

    public static boolean isWieldingHammer(Player p)
    {
        return SmashItemManager.isSpecialWielder(p) && SmashItemManager.getWieldedItem(p) instanceof Hammer;
    }

    @EventHandler
    public void chatControl(AsyncPlayerChatEvent e)
    {
        Player p = e.getPlayer();
        if (isWieldingHammer(p))
        {
            p.sendMessage(ChatColor.GOLD + "You are in Hammer mode! You cannot chat!");
        }
    }

    @EventHandler
    public void preventMostHammerMovement(InventoryClickEvent e)
    {
        if (isThis( e.getCurrentItem()))
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void preventMovingHammersAround(final PlayerInteractEvent e)
    {
        if (isThis(e.getItem()))
        {
            e.setCancelled(true);
        }
    }

    public void performRightClickAction(Player p) {}
}
