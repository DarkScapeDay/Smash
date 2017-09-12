package me.happyman.SpecialItems.SmashItemDrops;

import me.happyman.SpecialItems.SmashKitMgt.SmashKitManager;
import me.happyman.SpecialItems.SpecialItemTypes.SpecialItemWithTask;
import me.happyman.SpecialItems.UsefulItemStack;
import me.happyman.utils.InventoryKeep;
import me.happyman.worlds.SmashWorldManager;
import me.happyman.worlds.WorldType;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import static me.happyman.Plugin.getPlugin;

public class Hammer extends SpecialItemWithTask implements Listener
{
    private final List<Integer> cs;
    private final List<Integer> es;
    private final List<Integer> gs;
    private final HashMap<Sound, Float> instruments;
    private final Random r;

    private static ArrayList<Player> hammerWielders = new ArrayList<Player>();

    private static final int HAMMER_DURATION_SECONDS = 14; //Seconds
    private static final int TEMPO_MOD = 1;
    private static final int TOTAL_NOTES = 33*TEMPO_MOD;
    private static final int MAX_CYCLES = HAMMER_DURATION_SECONDS * 20 + 20;
    private static final int ADJUSTED_CYCLES = MAX_CYCLES - MAX_CYCLES % TOTAL_NOTES;

    public Hammer()
    {
        super(new UsefulItemStack(Material.IRON_AXE, ChatColor.GOLD + "" + ChatColor.BOLD + "HAMMER"));

        r = new Random();
        instruments = new HashMap<Sound, Float>();

        instruments.put(Sound.BLOCK_NOTE_BASS, 2F);
        instruments.put(Sound.BLOCK_NOTE_PLING, 2F);
        instruments.put(Sound.ENTITY_CHICKEN_EGG, 2F);
        instruments.put(Sound.BLOCK_ANVIL_LAND, 1.35F);
        instruments.put(Sound.ENTITY_PLAYER_BURP, 2F);

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

        Bukkit.getPluginManager().registerEvents(this, getPlugin());
    }

    @Override
    public void performResetAction(Player p)
    {
        super.performResetAction(p);
        if (hammerWielders.contains(p))
        {
            hammerWielders.remove(p);
        }
    }

    public static boolean isWieldingHammer(Player p)
    {
        return hammerWielders.contains(p);
    }

    private static void setWielding(Player p, boolean wielding)
    {
        if (!hammerWielders.contains(p) && wielding)
        {
            hammerWielders.add(p);
        }
        else if (hammerWielders.contains(p) && !wielding)
        {
            hammerWielders.remove(p);
        }
    }

    @EventHandler
    public void pickupItem(PlayerPickupItemEvent e)
    {
        final Player p = e.getPlayer();
        final World w = p.getWorld();

        if (isThis(e.getItem().getItemStack()))
        {
            e.setCancelled(true);
            if (SmashWorldManager.isSmashWorld(w) && !SmashKitManager.getFinalSmash(p).inventoryContains(p) && !isWieldingHammer(p) && !WorldType.isInSpectatorMode(p) && SmashWorldManager.gameIsInProgress(w))
            {
                setWielding(p, true);
                e.getItem().remove();
                InventoryKeep.setTempInv(p, ADJUSTED_CYCLES, true);
                p.setAllowFlight(false);
                p.getInventory().clear();
                p.setExp(1);
                List<Sound> sounds = new ArrayList<Sound>(instruments.keySet());
                final Sound randomSound = sounds.get(r.nextInt(sounds.size()));
                addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
                {
                    private int iteration = 0;
                    public void run()
                    {
                        if (!isWieldingHammer(p))
                        {
                            p.setExp(0);
                        }
                        else
                        {
                            if (iteration < ADJUSTED_CYCLES)
                            {
                                int note = iteration % TOTAL_NOTES;
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
                                p.setExp((float)(ADJUSTED_CYCLES - iteration)/ADJUSTED_CYCLES);
                                iteration++;
                            }
                            else if (iteration == ADJUSTED_CYCLES)
                            {
                                p.setExp(0);
                                setWielding(p, false);
                                iteration++;
                                performResetAction(p);
                            }
                        }

                    }
                }, 0, 1));

                for (int i = 0; i < 9; i++)
                {
                    p.getInventory().setItem(i, getItemStack());
                }
                e.getItem().getLocation().getBlock().setType(Material.AIR);
            }
        }
    }

    @EventHandler
    public void preventHammerCrafting(CraftItemEvent e)
    {
        if (Hammer.isWieldingHammer((Player)e.getWhoClicked()))
        {
            e.setCancelled(true);
        }
    }


    @Override
    public boolean performDropAction(Player p)
    {
        return true;
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
        if (isThis(e.getCurrentItem()))
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

    @Override
    public float getMeleeDamage(EntityDamageByEntityEvent event)
    {
        return 100;
    }

    @Override
    public void performRightClickAction(Player p, Block blockClicked)
    {
        super.performRightClickAction(p, blockClicked);
        WorldType.sendMessageToWorld(p, ChatColor.GOLD + "" + ChatColor.BOLD, ChatColor.RED + "" + ChatColor.BOLD + "IT'S HAMMER TIME!!!");
    }

    @Override
    public void setExpToRemaining(Player p) {
        p.setExp(1);
    }
}
