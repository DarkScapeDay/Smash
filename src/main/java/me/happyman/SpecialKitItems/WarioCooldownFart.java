package me.happyman.SpecialKitItems;

import me.happyman.ItemTypes.SmashItemWithCharge;
import me.happyman.utils.SmashEntityTracker;
import org.bukkit.*;
import org.bukkit.entity.Player;

public abstract class WarioCooldownFart extends SmashItemWithCharge
{
    private final WarioFartProperties fartProperties;

    public WarioCooldownFart(String name, short itemDamage, float range, float hDegrees, float maxPower, float chargeAmount, float dischargeAmount, boolean dischargesAutomatically, Sound sound, Float soundPitch, Effect effect, Float effectOffset)
    {
        super(Material.MONSTER_EGG, name,  chargeAmount, dischargeAmount, dischargesAutomatically);
        getItem().setDurability(itemDamage);
        fartProperties = new WarioFartProperties(range, hDegrees, maxPower, sound, soundPitch, effect, effectOffset);
    }

    public WarioCooldownFart(String name, short itemDamage, float range, float hDegrees, float maxPower, float chargeAmount, float dischargeAmount, boolean dischargesAutomatically)
    {
        this(name, itemDamage, range, hDegrees, maxPower, chargeAmount, dischargeAmount, dischargesAutomatically, null, null, null, null);
    }

    public WarioFartProperties getFartProperties()
    {
        return fartProperties;
    }

    @Override
    public void performRightClickAction(Player p)
    {
        if (canUseItem(p))
        {
            if (SmashEntityTracker.isCrouching(p))
            {
                setCharge(p, 0);
                activateTask(p);
                WarioFartManager.performFart(this, p);
                p.getWorld().playSound(p.getLocation(), Sound.ENDERMAN_HIT, 1F, 0.1F);
            }
            else
            {
                p.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "You must be crouching");
            }
        }
    }
}
