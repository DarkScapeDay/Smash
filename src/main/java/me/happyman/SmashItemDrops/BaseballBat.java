package me.happyman.SmashItemDrops;

import me.happyman.ItemTypes.SmashItemWithCrouchCharge;
import me.happyman.Listeners.SmashItemManager;
import me.happyman.utils.SmashEntityTracker;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

public class BaseballBat extends SmashItemWithCrouchCharge
{

    public BaseballBat()
    {
        super(Material.STICK,  ChatColor.GOLD + "" + ChatColor.BOLD + "Home-run bat", 0.024F, 0.0050F);
        getItem().setAmount(3);
    }
    /*public void setCharge(Player p, float charge)
    {
        putPlayerCharge(p, charge);
        if (isBeingHeld(p))
        {
            setExpToRemaining(p);
        }
        if (charge == 0F)
        {
            cancelAndForget(p);
        }
    }*/

    public void removeBat(Player p)
    {
        setCharge(p, 0);
        if (SmashEntityTracker.isCrouching(p))
        {
            activateTask(p);
        }
        SmashItemManager.removeOneItemFromHand(p);
    }

    public void performRightClickAction(Player p) {}
}
