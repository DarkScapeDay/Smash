package me.happyman.SpecialKitItems;

import me.happyman.ItemTypes.SmashItemWithCharge;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashAttackListener;
import me.happyman.worlds.SmashWorldManager;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class WarioFartManager
{

    public static void performFart(SmashItemWithCharge item, Player p)
    {
        performFart(item, p, 1F);
    }

    public static void performFart(SmashItemWithCharge item, Player p, Float modifier)
    {
        WarioFartProperties fartProperties = null;
        if (item instanceof WarioCooldownFart)
        {
            fartProperties = ((WarioCooldownFart)item).getFartProperties();
        }
        else if (item instanceof WarioCrouchFart)
        {
            fartProperties = ((WarioCrouchFart)item).getFartProperties();
        }
        float angle = SmashManager.getAngle(p.getLocation().getDirection(), new Vector(0, 1, 0))*180/(float)Math.PI;
        boolean isLookingUpOrDown = angle > 180 - fartProperties.ANGLE_THAT_COUNTS_AS_DOWN || angle < fartProperties.ANGLE_THAT_COUNTS_AS_DOWN;
        boolean proximityFart = ((Entity)p).isOnGround() && isLookingUpOrDown;

        if (fartProperties.SOUND != null && fartProperties.SOUND_PITCH != null)
        {
            p.getWorld().playSound(p.getLocation(), fartProperties.SOUND, 1, 0.2F);
        }
        if (fartProperties.EFFECT != null && fartProperties.OFFSET != null)
        {
            Location l = p.getLocation().clone();
            if (!proximityFart)
            {
                l.setX(l.getX() - l.getDirection().getX()*fartProperties.OFFSET);
                l.setY(l.getY() - l.getDirection().getY()*fartProperties.OFFSET);
                l.setZ(l.getZ() - l.getDirection().getZ()*fartProperties.OFFSET);
            }
            p.getWorld().playEffect(l, Effect.EXPLOSION_HUGE, 0, SmashWorldManager.SEARCH_DISTANCE);
        }
        float fartRange = fartProperties.FART_RANGE;
        if (proximityFart)
        {
            modifier *= fartProperties.PROXIMITY_NERF;
            fartRange = fartProperties.FART_RANGE/2.3F;
            SmashAttackListener.attackPlayersInRange(p, item.getItem().getItemMeta().getDisplayName(), fartProperties.HIGHEST_FART_POWER*modifier, fartRange, false);
        }
        else
        {
            SmashAttackListener.attackPlayersInAngleRange(p, item, fartProperties.HIGHEST_FART_POWER*modifier, fartRange, fartProperties.FART_HDEGREES, fartProperties.FART_VDEGREES, true);
        }
    }
}
