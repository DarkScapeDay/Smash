package me.happyman.utils;

import me.happyman.SpecialItems.SmashItemDrops.Hammer;
import me.happyman.SpecialItems.SmashItemDrops.ItemDropManager;
import me.happyman.SpecialItems.SmashKitMgt.SmashKitManager;
import me.happyman.worlds.WorldType;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Chicken;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBucketFillEvent;
import org.bukkit.inventory.ItemStack;

import static me.happyman.Plugin.getPlugin;
import static me.happyman.worlds.SmashWorldManager.isSmashWorld;

public class SmashMishapPreventor implements Listener
{
    public SmashMishapPreventor()
    {
        Bukkit.getPluginManager().registerEvents(this, getPlugin());
    }

    @EventHandler
    public void preventHunger(FoodLevelChangeEvent e)
    {
        if (e.getEntity() instanceof Player)
        {
            Player p = (Player) e.getEntity();
            WorldType type = WorldType.getWorldType(p.getWorld().getName());
            if ( type.isFightingHunger() || WorldType.isInSpectatorMode(p))
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
    public void preventChickenSpawns(CreatureSpawnEvent e)
    {
        if (isSmashWorld(e.getEntity().getWorld()) && e.getSpawnReason().equals(CreatureSpawnEvent.SpawnReason.EGG) && e.getEntity() instanceof Chicken)
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void preserveItemspawns(ItemDespawnEvent e)
    {
        World w = e.getEntity().getWorld();
        if (isSmashWorld(w) && ItemDropManager.isSmashDropItem(e.getEntity().getItemStack()))
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void preventLiquidTaking(PlayerBucketFillEvent e)
    {
        if (isSmashWorld(e.getPlayer().getWorld()))
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void dontBurnTheUndeadsBurnThePlayers(EntityCombustEvent e)
    {
        EntityType entityType = e.getEntityType();
        World w = e.getEntity().getWorld();
        if(isSmashWorld(w) && (entityType == EntityType.ZOMBIE || entityType == EntityType.SKELETON
                    || e.getEntity() instanceof Player && SmashKitManager.getKit((Player)e.getEntity()).isImmuneToFire()))
        {
            e.setCancelled(true);
        }
    }

    @EventHandler
    public void dontDropExp(EntityDeathEvent e)
    {
        EntityType entityType = e.getEntityType();
        if (isSmashWorld(e.getEntity().getWorld()))
        {
            e.setDroppedExp(0);
            e.getDrops().clear();
        }
    }

    @EventHandler
    public void smashChatControl(AsyncPlayerChatEvent e)
    {
        Player p = e.getPlayer();
        if (isSmashWorld(p.getWorld()))
        {
            e.setCancelled(true);
            if (!Hammer.isWieldingHammer(p))
            {
                WorldType.sendMessageToWorld(p, e.getMessage());
            }
        }
    }

    @EventHandler
    public void whyDoFistsDoFourHeartsLetsAddARetardedWorkaround(EntityDamageByEntityEvent e)
    {
        if (e.getDamager() instanceof Player && e.getDamage() >= 4)
        {
            ItemStack item = ((Player)e.getDamager()).getItemInHand();
            if (item == null || item.getType().equals(Material.AIR))
            {
                e.setDamage(1);
            }
        }
    }
}
