package me.happyman.SpecialItems.SmashItemDrops;

import me.happyman.SpecialItems.SpecialItem;
import me.happyman.SpecialItems.UsefulItemStack;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.util.Vector;

public class SwitcherBall extends SpecialItem
{
    public SwitcherBall()
    {
        super(new UsefulItemStack(Material.SNOW_BALL, ChatColor.WHITE + "" + ChatColor.BOLD + "Switcher Ball", new Enchantment[] {Enchantment.ARROW_INFINITE}, new int[] {1}));
    }

    @Override
    public void performRangeHit(LivingEntity shooter, Entity victim)
    {
        super.performRangeHit(shooter, victim);

        final Location tempShooterLocation = shooter.getLocation().clone();
        final Vector tempShooterVelocity = shooter.getVelocity().clone();

        shooter.teleport(victim.getLocation());
        shooter.setVelocity(victim.getVelocity());

        victim.teleport(tempShooterLocation);
        victim.setVelocity(tempShooterVelocity);
    }

    @Override
    public float getRangeDamage(EntityDamageByEntityEvent event)
    {
        return 4f;
    }
}
