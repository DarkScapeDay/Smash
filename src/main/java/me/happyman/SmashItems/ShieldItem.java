package me.happyman.SmashItems;

import me.happyman.ItemTypes.SmashItemWithCharge;
import me.happyman.commands.SmashManager;
import me.happyman.utils.SmashEntityTracker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import java.util.concurrent.Callable;

public class ShieldItem extends SmashItemWithCharge
{
    public ShieldItem()
    {
        super(Material.GLASS, ChatColor.WHITE + "" + ChatColor.BOLD + "Shield", 0.0018F, 0.0135F, false);
    }

    @Override
    public void setCharging(Player p, boolean charging)
    {
        super.setCharging(p, charging);

        if (charging)
        {
            SmashEntityTracker.setSpeedToCurrentSpeed(p);
        }
        else
        {
            p.setWalkSpeed(0.015F);
            SmashManager.preventJumping(p);
        }
    }

    @Override
    public void cancelTask(Player p)
    {
        super.cancelTask(p);
        SmashManager.resetJumpBoost(p);
    }

    public void performRightClickAction(final Player p)
    {
        if (canUseItem(p))
        {
            activateTask(p);
            setCharging(p, !isCharging(p));
            Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
                public String call()
                {
                    p.updateInventory();
                    return "";
                }
            });
        }
    }
}
