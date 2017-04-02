package me.happyman.Listeners;

import me.happyman.commands.SoupManager;
import me.happyman.source;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.Material;
import org.bukkit.inventory.*;

public class Soup implements Listener
{
    private final SoupManager soupManager;

    public Soup(SoupManager soupManager, source plugin)
    {
        this.soupManager = soupManager;

        plugin.getServer().getPluginManager().registerEvents(this, plugin);

        plugin.getServer().resetRecipes();

        ShapelessRecipe chocolateMilkRecipe;
        chocolateMilkRecipe = new ShapelessRecipe(plugin.getCustomItemStack(Material.MUSHROOM_SOUP, ChatColor.RESET + "Chocolate Milk"));
        chocolateMilkRecipe.addIngredient(plugin.getCustomMaterialData(Material.INK_SACK, 3));
        chocolateMilkRecipe.addIngredient(Material.BOWL);
        plugin.getServer().addRecipe(chocolateMilkRecipe);

        ShapelessRecipe cactiJuiceRecipe;
        cactiJuiceRecipe = new ShapelessRecipe(plugin.getCustomItemStack(Material.MUSHROOM_SOUP, ChatColor.RESET + "Cacti Juice"));
        cactiJuiceRecipe.addIngredient(2, Material.CACTUS);
        cactiJuiceRecipe.addIngredient(Material.BOWL);
        plugin.getServer().addRecipe(cactiJuiceRecipe);

        ShapelessRecipe insaneRecipe;
        insaneRecipe = new ShapelessRecipe(plugin.getCustomItemStack(Material.MUSHROOM_SOUP, ChatColor.RESET + "Insane Soup"));
        insaneRecipe.addIngredient(2, plugin.getCustomMaterialData(Material.MUSHROOM_SOUP, ChatColor.RESET + "Chocolate Milk"));
        plugin.getServer().addRecipe(insaneRecipe);
    }


    @EventHandler
    public void drinkEvent(PlayerInteractEvent e)
    {
        //if (e.getSmashDropItem().hasItemMeta() && e.getSmashDropItem().getItemMeta().hasDisplayName())
        //{
        //    //Bukkit.broadcastMessage(e.getSmashDropItem().getItemMeta().getDisplayName());
        //}
        if (e.getAction().toString().startsWith("RIGHT_CLICK") //AIR or BLOCK
                && e.getItem() != null
                && e.getItem().getType().equals(Material.MUSHROOM_SOUP))
        {
            final double soupHealAmount = 7;
            final float soupSaturationAmount = (float)1.4;
            Player p = e.getPlayer();
            float soupEfficiencyGoal = soupManager.getSoupPreference(p);

            if (p.getHealth() > p.getMaxHealth() - 0.5)
            {
                if (p.getFoodLevel() < 20)
                {
                    p.setSaturation(p.getSaturation() + soupSaturationAmount);
                    p.setFoodLevel(p.getFoodLevel() + (int)soupHealAmount);
                }
                else
                {
                    if (soupManager.getFatPreference(p))
                    {
                        p.sendMessage(ChatColor.GRAY + "You're already full, fatty!");
                        p.playSound(p.getLocation(), Sound.PIG_IDLE, (float)0.7, (float)0.3);
                    }
                    e.setCancelled(true);
                    return;
                }
            }
            else if (p.getHealth() > p.getMaxHealth() - soupHealAmount)
            {
                if (p.getHealth() > p.getMaxHealth() - soupHealAmount*soupEfficiencyGoal)
                {
                    p.sendMessage(ChatColor.GRAY + "You used that soup a little early...");
                }
                p.setHealth(p.getMaxHealth());
            }
            else
            {
                p.setHealth(p.getHealth() + soupHealAmount);
            }
            e.getPlayer().getItemInHand().setType(Material.BOWL);
            e.getPlayer().getItemInHand().setItemMeta(null);
            e.setCancelled(true);
        }
    }
}
