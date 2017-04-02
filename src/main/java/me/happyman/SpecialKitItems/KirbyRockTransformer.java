package me.happyman.SpecialKitItems;

import me.happyman.ItemTypes.SmashItemWithCharge;
import me.happyman.SmashKitMgt.LeatherArmorSet;
import me.happyman.SmashKitMgt.SmashKitManager;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashAttackListener;
import me.happyman.utils.SmashEntityTracker;
import org.bukkit.*;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.concurrent.Callable;

public class KirbyRockTransformer extends SmashItemWithCharge implements Listener
{
    private HashMap<Player, Integer> revertTransformationTasks;
    private final int STONE_DURATION = 7;
    private final int HITS_BEFORE_REVERT = 7;
    private final float RADIUS = 3;
    private final float DAMAGE = 25;
    private HashMap<Player, Integer> hitsBeforeRevert;

    public KirbyRockTransformer()
    {
        super(Material.BEDROCK, ChatColor.BLACK + "" + ChatColor.BOLD + "Stone", 0.006F, 1F, false);
        revertTransformationTasks = new HashMap<Player, Integer>();
        Bukkit.getPluginManager().registerEvents(this, SmashManager.getPlugin());
        hitsBeforeRevert = new HashMap<Player, Integer>();
    }

    public void cancelTask(Player p)
    {
        super.cancelTask(p);
        revertTransformation(p);
    }

    @Override
    public void performDeselectAction(Player p)
    {
        super.performDeselectAction(p);
        revertTransformation(p);
    }

    private void revertTransformation(Player p)
    {
        if (revertTransformationTasks.containsKey(p))
        {
            Bukkit.getScheduler().cancelTask(revertTransformationTasks.get(p));
            revertTransformationTasks.remove(p);
        }
        SmashKitManager.getCurrentKit(p).applyKitArmor(p);
        SmashAttackListener.forgetArtificiallyShieldedPlayer(p);
        forgetHitsBeforeRevert(p);
        SmashEntityTracker.setSpeedToCurrentSpeed(p);
    }

    private boolean isInStoneMode(Player p)
    {
        return revertTransformationTasks.containsKey(p);
    }

    @Override
    public void performRightClickAction(final Player p)
    {
        Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
            public String call() {
                p.updateInventory();
                return "";
            }
        });
        if (!isInStoneMode(p) && canUseItem(p)) //&& !((Entity)p).isOnGround())
        {
            activateTask(p);
            SmashAttackListener.setArtificiallyShieldedPlayer(p);
            Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
                public String call(){
                    //Bukkit.getPlayer("HappyMan").sendMessage("setting velocity for stone mode");
                    p.setVelocity(new Vector(0, -10, 0));
                    return "";
                }
            });
            setCharge(p, 0);
            p.getWorld().playSound(SmashEntityTracker.getBlockBelowEntity(p).getLocation(), Sound.ANVIL_BREAK, 1F, 0.9F);
            revertTransformationTasks.put(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable() {
                boolean keepDamaging = true;
                int i = 0;
                public void run() {
                    if (i < STONE_DURATION*20)
                    {
                        if (!isBeingHeld(p))
                        {
                            revertTransformation(p);
                        }
                        if (((Entity)p).isOnGround())
                        {
                            keepDamaging = false;
                        }
                        if (keepDamaging)
                        {
                            SmashAttackListener.attackPlayersInRange(p, getItem().getItemMeta().getDisplayName(), DAMAGE, RADIUS);
                        }
                        i++;
                    }
                    else
                    {
                        revertTransformation(p);
                    }
                }
            }, 0, 1));
            p.getEquipment().setArmorContents((new LeatherArmorSet(ChatColor.BLACK + "STONE", 0, 0, 0)).getContents());
            p.setWalkSpeed(0);
            SmashManager.preventJumping(p);
            hitsBeforeRevert.put(p, HITS_BEFORE_REVERT);
        }
    }

    private int getHitsBeforeRevert(Player p)
    {
        if (hitsBeforeRevert.containsKey(p))
        {
            if (hitsBeforeRevert.get(p) == 0)
            {
                forgetHitsBeforeRevert(p);
                return 0;
            }
            return hitsBeforeRevert.get(p);
        }
        return 0;
    }

    private void forgetHitsBeforeRevert(Player p)
    {
        if (hitsBeforeRevert.containsKey(p))
        {
            hitsBeforeRevert.remove(p);
        }
    }

    @EventHandler
    public void revertStoneOnHit(EntityDamageByEntityEvent e)
    {
        if (e.getEntity() instanceof Player)
        {
            Player p = (Player)e.getEntity();
            if (isInStoneMode(p))
            {
                if (getHitsBeforeRevert(p) > 0)
                {
                    hitsBeforeRevert.put(p, hitsBeforeRevert.get(p) - 1);
                }
                if (getHitsBeforeRevert(p) == 0)
                {
                    revertTransformation(p);
                }
            }
        }
    }

    @EventHandler
    public void dontDoAnythingElse(InventoryOpenEvent e)
    {
        if (isInStoneMode((Player)e.getPlayer()))
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void crouchCancel(PlayerToggleSneakEvent e)
    {
        Player p = e.getPlayer();
        if (isInStoneMode(p) && SmashEntityTracker.isCrouching(p))
        {
            revertTransformation(p);
        }
    }
}
