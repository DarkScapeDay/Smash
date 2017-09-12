package me.happyman.SpecialItems.SmashItemDrops;

import me.happyman.SpecialItems.SpecialItemTypes.SpecialItemWithCrouchCharge;
import me.happyman.SpecialItems.UsefulItemStack;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import static me.happyman.utils.SmashEntityTracker.isCrouching;

public class BaseballBat extends SpecialItemWithCrouchCharge
{
    public BaseballBat(int amount)
    {
        super(new UsefulItemStack(Material.STICK,  ChatColor.GOLD + "" + ChatColor.BOLD + "Home-run bat", amount), 0.024F, 0.0050F);
    }

    @Override
    public float getMeleeDamage(EntityDamageByEntityEvent event)
    {
        return event.getDamager() instanceof Player ? 40f*getCharge((Player)event.getDamager()) : 40;
    }

    @Override
    public void performMeleeHit(LivingEntity attacker, Entity victim)
    {
        super.performMeleeHit(attacker, victim);
        if (attacker instanceof Player)
        {
            Player player = (Player)attacker;
            removeOne(player);
            setCharging(player, isCrouching(player));
        }
    }
}
