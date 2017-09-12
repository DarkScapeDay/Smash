package me.happyman.SpecialItems.SpecialItemTypes;

import me.happyman.SpecialItems.SpecialItem;
import me.happyman.SpecialItems.UsefulItemStack;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Vector;

import java.util.List;
import java.util.concurrent.Callable;

import static me.happyman.Plugin.getPlugin;

public class CompassItem extends SpecialItem
{
    private final TargetMode[] targetModes;
    private static final String targetModePrefix = ChatColor.BLACK + "Target Mode "; //don't change this

    public CompassItem(String lore, TargetMode[] targetModes)
    {
        super(new UsefulItemStack(Material.COMPASS, ChatColor.GRAY + "" + ChatColor.BOLD + "Tracking Compass", lore));
        this.targetModes = targetModes;

        List<String> lores = getItemStack().getItemMeta().getLore();
        lores.add(targetModePrefix + 2);
        ItemMeta meta = getItemStack().getItemMeta();
        meta.setLore(lores);
        getItemStack().setItemMeta(meta);
    }

    public CompassItem(String lore, TargetMode targetMode)
    {
        this(lore, new TargetMode[] {targetMode});
    }

    private void setTargetMode(Player player, int mode)
    {
        ItemStack itemClicked = player.getItemInHand();
        if (isThis(itemClicked))
        {
            if (mode < 0)
            {
                mode = 0;
            }
            else if (mode >= targetModes.length)
            {
                mode = targetModes.length - 1;
            }

            TargetMode newMode = targetModes[mode];
            if (newMode != null)
            {
                newMode.guidePlayer(player);
            }

            ItemMeta meta = itemClicked.getItemMeta();
            List<String> lores = meta.getLore();
            String lastLine = lores.get(lores.size() - 1);
            if (!lastLine.startsWith(targetModePrefix))
            {
                if (ChatColor.stripColor(lastLine).equalsIgnoreCase(ChatColor.stripColor(targetModePrefix)))
                {
                    lores.remove(lores.size() - 1);
                }
                lores.add(targetModePrefix + mode);
            }
            else
            {
                lores.set(lores.size() - 1, targetModePrefix + mode);
            }
            meta.setLore(lores);
            itemClicked.setItemMeta(meta);


            player.setItemInHand(itemClicked);
        }
    }

    private TargetMode getTargetMode(ItemStack itemClicked)
    {
        return targetModes[getTargetModeIndex(itemClicked)];
    }

    private int getTargetModeIndex(ItemStack itemClicked)
    {
        if (isThis(itemClicked))
        {
            List<String> lores = itemClicked.getItemMeta().getLore();
            String lastLine = lores.get(lores.size() - 1);
            if (lastLine.startsWith(targetModePrefix))
            {
                try
                {
                    int result = Integer.valueOf(lastLine.substring(targetModePrefix.length(), lastLine.length()));
                    return result < 0 || result >= targetModes.length ? 0 : result;
                }
                catch (NumberFormatException ex)
                {
                    return 0;
                }
            }
        }
        return 0;
    }

//    private static final Target noTarget = new Target(null,  ChatColor.GRAY + "" + ChatColor.BOLD + "No targets found");
    public static class Target
    {
        private final Location location;
        private final String compassName;

        public Target(Location location)
        {
            this(location, ChatColor.BLUE + "" + ChatColor.BOLD + "Tracking " + (location == null ? "nowhere" :
                    ((location.getWorld().getSpawnLocation().toVector().distanceSquared(new Vector(location.getX(), location.getY(), location.getZ())) < 25 ?
                            "Spawn" : (location.getBlockX() + ", " + location.getBlockZ())) + "!")));
        }

        public Target(Player target)
        {
            this(target == null ? null : target.getLocation(), ChatColor.GOLD + "" + ChatColor.BOLD + "Tracking " + (target == null ? "nobody" : (target.getName() + "!")));
        }

        public Target(Location location, String compassName)
        {
            this.location = location;
            this.compassName = location == null ? ChatColor.GRAY + "" + ChatColor.BOLD + "No targets found" : compassName;
        }
    }

    public abstract static class TargetMode
    {
        public abstract Target getTarget(Player targetter);

        private final void guidePlayer(final Player targetter)
        {
            Target target = getTarget(targetter);
            if (target == null)
            {
                target = new Target((Location)null);
            }
            if (target.location != null)
            {
                targetter.setCompassTarget(target.location);
            }
            if (target.compassName != null)
            {
                final ItemStack compassInHand = targetter.getItemInHand();
                final ItemMeta meta = compassInHand.getItemMeta();
                if (meta.getDisplayName().equals(target.compassName))
                {
                    final String origName = target.compassName;
                    meta.setDisplayName(" " + origName + " ");
                    compassInHand.setItemMeta(meta);
                    targetter.setItemInHand(compassInHand);

                    Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
                    {
                        public String call()
                        {
                            meta.setDisplayName(origName);
                            compassInHand.setItemMeta(meta);
                            targetter.setItemInHand(compassInHand);
                            return "";
                        }
                    });
                }
                else
                {
                    meta.setDisplayName(target.compassName);
                    compassInHand.setItemMeta(meta);
                    targetter.setItemInHand(compassInHand);
                }
            }
        }
    }

    @Override
    public final boolean isThis(ItemStack item)
    {
        if (item != null && item.getType() == getItemStack().getType() && item.hasItemMeta() && item.getItemMeta().hasLore())
        {
            List<String> lores = item.getItemMeta().getLore();
            if (lores.size() > 0 && lores.get(lores.size() - 1).startsWith(targetModePrefix))
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public final void performLeftClickAction(Player p, Block blockClicked)
    {
        super.performLeftClickAction(p, blockClicked);
        if (targetModes.length > 1)
        {
            setTargetMode(p, (getTargetModeIndex(p.getItemInHand()) + 1) % targetModes.length);
        }
    }

    @Override
    public final void performRightClickAction(Player p, Block blockClicked)
    {
        super.performRightClickAction(p, blockClicked);
        TargetMode mode = getTargetMode(p.getItemInHand());
        if (mode != null)
        {
            mode.guidePlayer(p);
        }
    }
}
