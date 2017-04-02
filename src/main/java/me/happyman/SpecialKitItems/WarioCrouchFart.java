package me.happyman.SpecialKitItems;

import me.happyman.ItemTypes.SmashItemWithCrouchCharge;
import me.happyman.utils.SmashEntityTracker;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

public abstract class WarioCrouchFart extends SmashItemWithCrouchCharge implements Listener
{
    private WarioFartProperties fartProperties;

    public WarioCrouchFart(String name, short itemDamage, float range, float hDegrees, float maxPower, float chargeAmount, float dischargeAmount, Sound sound, Float soundPitch, Effect effect, Float effectOffset)
    {
        super(Material.MONSTER_EGG, name,  chargeAmount, dischargeAmount);
        getItem().setDurability(itemDamage);
        fartProperties = new WarioFartProperties(range, hDegrees, maxPower, sound, soundPitch, effect, effectOffset);
    }

    public WarioCrouchFart(String name, short itemDamage, float range, float hDegrees, float maxPower, float chargeAmount, float dischargeAmount)
    {
        this(name, itemDamage, range, hDegrees, maxPower, chargeAmount, dischargeAmount, null, null, null, null);
    }

    public WarioFartProperties getFartProperties()
    {
        return fartProperties;
    }


    public void decreaseCharge(Player p)
    {
        if (canUseItem(p))
        {
            if (!SmashEntityTracker.isCrouching(p))
            {
                setCharge(p, getCharge(p) - getDischargeAmount());
            }
        }
        else
        {
            setCharge(p, 0F);
        }
    }

    @Override
    public void performRightClickAction(Player p)
    {
        if (SmashEntityTracker.isCrouching(p))
        {
            float charge = getCharge(p);
            setCharge(p, 0);
            if (charge >= 0.3F)
            {
                WarioFartManager.performFart(this, p, charge);
            }
            else
            {
                p.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Your fart is ineffective");
            }
        }
        else
        {
            p.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "You must be crouching");
        }
    }
}
