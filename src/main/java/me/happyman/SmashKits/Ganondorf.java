package me.happyman.SmashKits;

import me.happyman.ItemTypes.RocketItem;
import me.happyman.ItemTypes.SmashItem;
import me.happyman.SmashKitMgt.LeatherArmorSet;
import me.happyman.SmashKitMgt.SmashKit;
import me.happyman.SmashKitMgt.SmashKitProperties;
import me.happyman.SpecialKitItems.GanondorfEgg;
import me.happyman.commands.SmashManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;

public class Ganondorf extends SmashKit
{
    public Ganondorf()
    {
        super(
            new SmashKitProperties(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Jokendork", Material.PAPER, ChatColor.GRAY + "" + ChatColor.ITALIC + "Oooh-yeah!",
                new FinalSmash(6, null, null)
                {
                    @Override
                    protected void performFinalSmashAbility(final Player p)
                    {
                        addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable() {
                            int iteration = 0;
                            public void run()
                            {
                                if (SmashManager.getDamage(p) >= 8900 || iteration >= 100)
                                {
                                    SmashManager.addDamage(p, 9001 - SmashManager.getDamage(p), false);
                                    cancelTask(p);
                                }
                                else
                                {
                                    SmashManager.addDamage(p, 87, false);
                                }
                                iteration++;
                            }
                        }, 0, 1));
                    }

                    @Override
                    protected void endFinalSmashAbility(Player p)
                    {

                    }
                }, 5F, 0.7F),
            new LeatherArmorSet(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Jokendork's Armor", new int[]{251, 89, 51}, new int[]{87, 87, 115}, new int[]{69, 62, 70}, new int[]{41, 47, 63}),
            new SmashItem(Material.WOOD_SWORD, ChatColor.LIGHT_PURPLE + "Jokendork's sword", new Enchantment[]{Enchantment.DURABILITY}, new int[]{100}) {
                public void performRightClickAction(Player p) {
                }
            },
            new RocketItem(ChatColor.RED + "" + ChatColor.BOLD + "   Single-Jump", 1, 0.8F) {}
        );
        addItem(new GanondorfEgg());
    }
}
