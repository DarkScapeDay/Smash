package me.happyman.Other;

import me.happyman.Plugin;
import me.happyman.utils.FileManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ShapelessRecipe;

import java.util.HashMap;

import static me.happyman.Plugin.*;
import static me.happyman.utils.FileManager.getGeneralPlayerFile;

public class SoupManager implements CommandExecutor
{
    public class Soup implements Listener //@TODO: make into item
    {
        private final SoupManager soupManager;

        public Soup(SoupManager soupManager)
        {
            this.soupManager = soupManager;

            getPlugin().getServer().getPluginManager().registerEvents(this, getPlugin());

            getPlugin().getServer().resetRecipes();

            ShapelessRecipe chocolateMilkRecipe;
            chocolateMilkRecipe = new ShapelessRecipe(Plugin.getCustomItemStack(Material.MUSHROOM_SOUP, ChatColor.RESET + "Chocolate Milk"));
            chocolateMilkRecipe.addIngredient(Plugin.getCustomMaterialData(Material.INK_SACK, 3));
            chocolateMilkRecipe.addIngredient(Material.BOWL);
            getPlugin().getServer().addRecipe(chocolateMilkRecipe);

            ShapelessRecipe cactiJuiceRecipe;
            cactiJuiceRecipe = new ShapelessRecipe(Plugin.getCustomItemStack(Material.MUSHROOM_SOUP, ChatColor.RESET + "Cacti Juice"));
            cactiJuiceRecipe.addIngredient(2, Material.CACTUS);
            cactiJuiceRecipe.addIngredient(Material.BOWL);
            getPlugin().getServer().addRecipe(cactiJuiceRecipe);

            ShapelessRecipe insaneRecipe;
            insaneRecipe = new ShapelessRecipe(Plugin.getCustomItemStack(Material.MUSHROOM_SOUP, ChatColor.RESET + "Insane Soup"));
            insaneRecipe.addIngredient(2, Plugin.getCustomMaterialData(Material.MUSHROOM_SOUP, ChatColor.RESET + "Chocolate Milk"));
            getPlugin().getServer().addRecipe(insaneRecipe);
        }


        @EventHandler
        public void drinkEvent(PlayerInteractEvent e)
        {
            //if (e.getSmashDropItem().hasItemMeta() && e.getSmashDropItem().getItemMeta().hasDisplayName())
            //{
            //    //Bukkit.broadcastMessage(e.getSmashDropItem().getItemMeta().getDisplayName());
            //}
            if ((e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) //AIR or BLOCK
                    && e.getItem() != null
                    && e.getItem().getType() == Material.MUSHROOM_SOUP)
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
                            p.playSound(p.getLocation(), Sound.ENTITY_PIG_AMBIENT, (float)0.7, (float)0.3);
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
        
    public static final String soupEffyCmdName = "soupefficiency";
    public static final String SHAME_CMD_NAME = "fatshaming";
    public static HashMap<Player, Float> soupEfficiencyGoals;
    public static HashMap<Player, Boolean> fatShamingPreferences;

    public SoupManager()
    {
        new Soup(this);
        soupEfficiencyGoals = new HashMap<Player, Float>();
        fatShamingPreferences = new HashMap<Player, Boolean>();

        setExecutor(soupEffyCmdName, this);
        setExecutor(SHAME_CMD_NAME, this);
    }

    public void updateQuickSoupEffy(Player p)
    {
        float dater;
        try
        {
            String data = FileManager.getData(getGeneralPlayerFile(p), SoupManager.soupEffyCmdName);
            dater = Float.parseFloat(data);
            if (dater > 1)
            {
                dater /= 100;
            }
            if (!validPercentage(dater))
            {
                dater = 0;
                throw new NumberFormatException("Invalid percentage in file");
            }
        }
        catch (NumberFormatException e)
        {
            dater = 0;
            FileManager.putData(getGeneralPlayerFile(p), SoupManager.soupEffyCmdName, 0);
        }
        soupEfficiencyGoals.put(p, dater);
    }

    public void updateQuickFat(Player p)
    {
        boolean wantsToBeShamed = FileManager.getData(getGeneralPlayerFile(p), SHAME_CMD_NAME).equalsIgnoreCase("true");
        fatShamingPreferences.put(p, wantsToBeShamed);
    }

    public HashMap<Player, Boolean> fatPreferences()
    {
        return fatShamingPreferences;
    }

    public HashMap<Player, Float> soupPreferences()
    {
        return soupEfficiencyGoals;
    }

    public float getSoupPreference(Player p)
    {
        if (!soupPreferences().containsKey(p))
        {
            updateQuickSoupEffy(p);
        }
        return soupPreferences().get(p);
    }

    public boolean getFatPreference(Player p)
    {
        if (!fatPreferences().containsKey(p))
        {
            updateQuickFat(p);
        }
        return fatPreferences().get(p);
    }

    public boolean validPercentage(float i)
    {
        return i <= 1 && i >= 0;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (!(sender instanceof Player))
        {
            sender.sendMessage(ChatColor.RED + "Error! You are not in-game.");
            return true;
        }
        if (matchesCommand(label, soupEffyCmdName))
        {
            Player p = (Player)sender;
            if (args.length == 0)
            {
                displayHelpMessage(sender, label);
            }
            else
            {
                float input;
                try
                {
                    input = Float.valueOf(args[0]);
                    if (input > 1)
                    {
                        input /= 100;
                    }
                    if (!validPercentage(input))
                    {
                        p.sendMessage(ChatColor.RED + "Error! Please enter a valid percentage (0-100%)!");
                        return true;
                    }
                    if (input < 0.01)
                    {
                        input = 0;
                    }
                    String s = String.valueOf(input*100);
                    if (input == 0)
                    {
                        s = "disabled";
                    }
                    else if (s.lastIndexOf(".") <= 3)
                    {
                        s = "" +  Math.round(input*100) + "%";
                    }
                    p.sendMessage(ChatColor.GREEN + "Soup efficiency preference set to " + s + "!");
                }
                catch (NumberFormatException e)
                {
                    if (isTrue(args[0]))
                    {
                        input = 1;
                        FileManager.putData(getGeneralPlayerFile(p), soupEffyCmdName, input);
                        p.sendMessage(ChatColor.GREEN + "Soup notifications set to 100% efficiency!");
                    }
                    else if (isFalse(args[0]))
                    {
                        input = 0;
                        FileManager.putData(getGeneralPlayerFile(p), soupEffyCmdName, 0.0);
                        p.sendMessage(ChatColor.GREEN + "Soup notifications disabled!");
                    }
                    else if ("help".equalsIgnoreCase(args[0]))
                    {
                        displayHelpMessage(sender, soupEffyCmdName);
                        return true;
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.RED + "Error! Please enter a numeric input!");
                        return true;
                    }
                }
                FileManager.putData(getGeneralPlayerFile(p), soupEffyCmdName, input);
                updateQuickSoupEffy(p);
            }
            return true;
        }
        else if (matchesCommand(label, SHAME_CMD_NAME))
        {
            Player p = (Player)sender;
            if ((args.length > 0 && Plugin.isTrue(args[0])) || (args.length == 0 && !Plugin.isTrue(FileManager.getData(getGeneralPlayerFile(p), SHAME_CMD_NAME))))
            {
                FileManager.putData(getGeneralPlayerFile(p), SHAME_CMD_NAME, "true");
                updateQuickFat(p);
                p.sendMessage(ChatColor.GREEN + "Fat shaming enabled!");
            }
            else
            {
                FileManager.putData(getGeneralPlayerFile(p), SHAME_CMD_NAME, "false");
                updateQuickFat(p);
                p.sendMessage(ChatColor.GREEN + "Fat shaming disabled!");
            }
            return true;
        }
        return false;
    }
}
