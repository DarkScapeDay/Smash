package me.happyman.SpecialItems.SpecialItemTypes;

import me.happyman.SpecialItems.SpecialItem;
import me.happyman.SpecialItems.UsefulItemStack;
import me.happyman.utils.Verifier;
import me.happyman.worlds.GuiManager;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static me.happyman.worlds.WorldType.forceTp;

public class CompanyItem extends SpecialItem
{
    private static final String ITEM_PREFIX = ChatColor.GOLD + "" + ChatColor.BOLD + "Group ";
    private static final float ALL_GROUPS_SPAWN_CHANCE = 1f/(Collective.values().length*3);

    private enum Collective
    {
        TEAM_BLUE(Material.WOOL, 11),
        TEAM_RED(Material.WOOL, 14),
        TEAM_LIME(Material.WOOL, 5),
        TEAM_ORANGE(Material.WOOL, 1),
        TEAM_BLACK(Material.WOOL, 15),
        TEAM_YELLOW(Material.WOOL, 4),
        TEAM_PINK(Material.WOOL, 6),
        TEAM_PURPLE(Material.WOOL, 10);

        public List<Player> getCollectivists()
        {
            if (inv != null)
            {
                return inv.getPlayersWithThisOpen();
            }
            return new ArrayList<Player>();
        }

        public static AllCollectivesGui getAllCollectivesGui()
        {
            if (invAll == null)
            {
                invAll = new AllCollectivesGui();
            }
            return invAll;
        }

        public final UsefulItemStack item;
        public SingleCollectiveGui inv = null;
        public static AllCollectivesGui invAll = null;

        public SingleCollectiveGui getInventory()
        {
            if (inv == null)
            {
                inv = new SingleCollectiveGui();
            };
            return inv;
        }

        static class TeleportGui extends GuiManager.GuiInventory
        {
            TeleportGui(String coloredName)
            {
                super(coloredName);
            }

            public void removeItem(Player player)
            {
                ItemStack itemToRemove = getPlayerRepresenter(player);
                super.removeItem(itemToRemove, true);
                if (invAll != null)
                {
                    invAll.removeItem(itemToRemove, true);
                    if (invAll.getContentsSize() == 0)
                    {
                        invAll = null;
                    }
                }
            }

            ItemStack getPlayerRepresenter(Player partOfTheCollective)
            {
                return new UsefulItemStack(Material.SKULL_ITEM, partOfTheCollective.getDisplayName(), (byte)3);
            }

            void addItem(final Player collectivist)
            {
                GuiManager.GuiItem itemToAdd = new GuiManager.GuiItem(getPlayerRepresenter(collectivist))
                {
                    @Override
                    public void performAction(final Player clicker)
                    {
                        if (collectivist == clicker)
                        {
                            clicker.sendMessage(ChatColor.GRAY + "That is you.");
                        }
                        else
                        {
                            clicker.closeInventory();
                            new Verifier.BooleanVerifier(collectivist, ChatColor.GRAY + "Teleport to " + clicker.getName() + "?")
                            {
                                @Override
                                public void performYesAction()
                                {
                                    forceTp(clicker, collectivist);
                                }

                                @Override
                                public void performNoAction()
                                {
                                    clicker.sendMessage(ChatColor.GRAY + "Perhaps later you will change your mind.");
                                }
                            };
                        }
                    }
                };
                addItem(itemToAdd, true);
                if (invAll != null)
                {
                    invAll.addItem(itemToAdd, true);
                }
            }
        }

        static class AllCollectivesGui extends TeleportGui
        {
            AllCollectivesGui()
            {
                super(ChatColor.BLACK + "" + ChatColor.BOLD + "All groups");
                for (Collective collective : values())
                {
                    if (collective.inv != null)
                    {
                        addAll(collective.inv.getContents(), false);
                    }
                }
            }

            @Override
            public String getFailureMessage(Player opener)
            {
                return ChatColor.GRAY + "No one is in a Collective right now.";
            }
        }

        class SingleCollectiveGui extends TeleportGui
        {
            SingleCollectiveGui()
            {
                super(Collective.this.getItem().getColoredName());
            }

            @Override
            public void handleInventoryClose(Player playerWhoClosedIt)
            {
                super.handleInventoryClose(playerWhoClosedIt);
                playerWhoClosedIt.sendMessage(ChatColor.GRAY + "You have left Collective " + getId());
                removeItem(playerWhoClosedIt);
                if (getContentsSize() == 0)
                {
                    inv = null;
                }
            }

            @Override
            void addItem(Player collectivist)
            {
                super.addItem(collectivist);
                collectivist.sendMessage(ChatColor.GRAY + "Welcome. You are now part of Collective " + getId() + ".");
            }
        }

        int getId()
        {
            return ordinal() + 2;
        }

        static int getIndex(int id)
        {
            return id - 2;
        }

        Collective(Material mat, int data)
        {
            this(mat, (byte)(data % Byte.MAX_VALUE));
        }

        Collective(Material mat)
        {
            this(mat, 0);
        }

        Collective(Material mat, byte data)
        {
            this.item = new UsefulItemStack(mat, ITEM_PREFIX + getId(), ChatColor.GRAY + "Anyone in the collective\nmay meet.", data);
        }

        public UsefulItemStack getItem()
        {
            return item;
        }
    }


    private static final Random r = new Random();
    private static final SpecialItem ALL_GROUPS_ITEM = new SpecialItem(
            new UsefulItemStack(Material.WOOL, ChatColor.WHITE + "" + ChatColor.BOLD + "ALL GROUPS", ChatColor.GRAY + "This item allows you to\nteleport to anyone in any\ncollective..."))
    {
        @Override
        public void performRightClickAction(Player p, Block blockClicked)
        {
            super.performRightClickAction(p, blockClicked);
            Collective.getAllCollectivesGui().open(p);
        }
    };

    @Override
    public ItemStack getItemStack() //actually gets a random one
    {
        if (r.nextFloat() < ALL_GROUPS_SPAWN_CHANCE)
        {
            return  ALL_GROUPS_ITEM.getItemStack();
        }
        else
        {
            return Collective.values()[r.nextInt(Collective.values().length)].item;
        }
    }

    @Override
    public boolean isThis(ItemStack item)
    {
        return item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName() &&
               (item.getItemMeta().getDisplayName().startsWith(ITEM_PREFIX) ||
                item.getItemMeta().getDisplayName().equals(ALL_GROUPS_ITEM.coloredName()));
    }

    private static Collective getGroup(ItemStack item)
    {
        if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName())
        {
            String displayName = item.getItemMeta().getDisplayName();
            try
            {
                int result = Collective.getIndex(Integer.valueOf(displayName.substring(ITEM_PREFIX.length(), displayName.length())));
                Collective[] valuesThere = Collective.values();
                if (result >= 0 && result < valuesThere.length)
                {
                    return valuesThere[result];
                }
            }
            catch (NumberFormatException ex)
            {}
        }
        return null;
    }

    public CompanyItem()
    {
        super(new UsefulItemStack("Company"));
    }

    @Override
    public void performRightClickAction(Player p, Block blockClicked)
    {
        super.performRightClickAction(p, blockClicked);
        ItemStack itemInHand = p.getItemInHand();
        if (ALL_GROUPS_ITEM.isThis(itemInHand))
        {
            ALL_GROUPS_ITEM.performRightClickAction(p, blockClicked);
        }
        else
        {
            Collective group = getGroup(itemInHand);
            if (group != null)
            {
                Collective.SingleCollectiveGui inv = group.getInventory();
                inv.addItem(p);
                inv.open(p);
            }
            else
            {
                p.sendMessage(ChatColor.RED + "Could not find your group!");
            }
        }
    }
}
