package me.happyman.worlds;

import me.happyman.utils.InventoryManager;
import me.happyman.utils.Verifier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.*;
import java.util.concurrent.Callable;

import static me.happyman.Plugin.getPlugin;
import static me.happyman.Plugin.sendErrorMessage;

public class GuiManager
{
    private static final HashMap<Player, GuiInventory> openGuis = new HashMap<Player, GuiInventory>();
    private static final ItemStack previousPageItem = new ItemStack(Material.ARROW);//, 1, (short)0, (byte)7);
    private static final ItemStack previousManyPageItem = new ItemStack(Material.COMPASS);//, 1, (short)0, (byte)7);
    private static final ItemStack nextPageItem = new ItemStack(Material.ARROW);//, 1, (short)0, (byte)7);
    private static final ItemStack nextManyPageItem = new ItemStack(Material.COMPASS);//, 1, (short)0, (byte)7);
    private static final ItemStack pageJumpItem = new ItemStack(Material.PAPER);
    private static final int PAGES_TO_JUMP_MANY = 5;

    static
    {
        ItemMeta meta = previousPageItem.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "‹ Previous");
        previousPageItem.setItemMeta(meta);

        meta = nextPageItem.getItemMeta();
        meta.setDisplayName(ChatColor.GREEN + "" + ChatColor.BOLD + "Next ›");
        nextPageItem.setItemMeta(meta);

        meta = previousManyPageItem.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD  + "« " + PAGES_TO_JUMP_MANY + " Back");
        previousManyPageItem.setItemMeta(meta);

        meta = nextManyPageItem.getItemMeta();
        meta.setDisplayName(ChatColor.RED + "" + ChatColor.BOLD + PAGES_TO_JUMP_MANY + " Forward »");
        nextManyPageItem.setItemMeta(meta);

        meta = pageJumpItem.getItemMeta();
        meta.setDisplayName(ChatColor.GRAY + "" + ChatColor.BOLD + "Goto");
        pageJumpItem.setItemMeta(meta);


        Listener guiListener = new Listener()
        {
            @EventHandler
            private void onDrop(final PlayerDropItemEvent event)
            {
                final Player p = event.getPlayer();
                GuiInventory inv = openGuis.get(p);
                if (inv == null)
                {
                    return;
                }
                final ItemStack droppedItem = event.getItemDrop().getItemStack();
                final short amount = (short)droppedItem.getAmount();
                if (InventoryManager.canGive(p, droppedItem, amount, false))
                {
                    event.setCancelled(true);
                    Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
                    {
                        public String call()
                        {
                            InventoryManager.removeItem(p, droppedItem, amount, false);
                            p.setItemOnCursor(droppedItem);
                            return "";
                        }
                    });
                }
            }

            @EventHandler
            private void onClick(InventoryDragEvent event)
            {
                if (event.getWhoClicked() instanceof Player)
                {
                    final Player p = (Player)event.getWhoClicked();
                    GuiInventory inv = openGuis.get(p);
                    if (inv != null)
                    {
                        boolean cancel = false;
                        Map<Integer, ItemStack> newItemMap = event.getNewItems();
                        for (Integer slot : event.getInventorySlots())
                        {
                            if (inv.handleInventoryClick(event.getInventory(), slot, p, false, newItemMap.get(slot)))
                            {
                                cancel = true;
                            }
                        }

                        if (cancel)
                        {
                            event.setCancelled(true);
                        }
                    }
                }
            }

            @EventHandler
            private void onClick(InventoryClickEvent event)
            {
                if (event.getWhoClicked() instanceof Player && event.getAction() != InventoryAction.NOTHING)
                {
                    Player p = (Player)event.getWhoClicked();
                    GuiInventory inv = openGuis.get(p);
                    if (inv == null)
                    {
                        return;
                    }

                    Inventory clickedInventory = event.getClickedInventory();
                    int slot = event.getSlot();
                    switch (event.getAction())
                    {
                        case MOVE_TO_OTHER_INVENTORY:
                            if (clickedInventory != p.getOpenInventory().getTopInventory() && event.isShiftClick())
                            {
                                clickedInventory = p.getOpenInventory().getTopInventory();
                                for (int i = 0; i < clickedInventory.getSize(); i++)
                                {
                                    ItemStack curItem = clickedInventory.getItem(i);
                                    if (curItem == null || curItem.getType() == Material.AIR)
                                    {
                                        slot = i;
                                        break;
                                    }
                                }
                            }
                            break;
                        case COLLECT_TO_CURSOR:
                            event.setCancelled(true);
                            return;
                    }

                    final boolean leftClick;
                    switch (event.getAction())
                    {
                        case COLLECT_TO_CURSOR:
                        case DROP_ALL_CURSOR:
                        case DROP_ALL_SLOT:
                        case PICKUP_ALL:
                        case PLACE_ALL:
                            leftClick = true;
                            break;
                        default:
                            leftClick = false;
                            break;
                    }
                    if (clickedInventory == p.getOpenInventory().getTopInventory() && inv.handleInventoryClick(clickedInventory, slot, p, leftClick, event.getCurrentItem()))
                    {
                        event.setCancelled(true);
                    }
                }
            }

            @EventHandler
            private void onInvClose(InventoryCloseEvent event)
            {
                if (event.getPlayer() instanceof Player)
                {
                    Player p = (Player)event.getPlayer();
                    GuiInventory curInv = openGuis.get(p);
                    if (curInv != null && !curInv.isBeingReopened(p))
                    {
                        curInv.handleInventoryClose(p);
                    }
                }
            }
        };
        Bukkit.getPluginManager().registerEvents(guiListener, getPlugin());
    }

    public abstract static class GuiItem
    {
        private ItemStack item;
        private String itemName;
        private GuiInventory guiInv; //needed because you want to be able to update or remove items from the item itself
        private final String pluralName;
        private final boolean allowTaking;

        protected GuiItem(ItemStack item)
        {
            this(item, null);
        }

        GuiItem(ItemStack item, String name, boolean allowTaking)
        {
            if (item == null)
            {
                item = new ItemStack(Material.AIR);
            }
            this.allowTaking = allowTaking;
            this.item = item;
            if (name != null)
            {
                ItemMeta meta = this.item.getItemMeta();
                meta.setDisplayName(ChatColor.RESET + name);
                this.item.setItemMeta(meta);
            }
            this.itemName = InventoryManager.getColorlessItemName(item);
            this.pluralName = InventoryManager.getPluralName(getItem(), (short)getItem().getAmount());
        }

        GuiItem(ItemStack item, String name)
        {
            this(item, name, false);
        }

        GuiItem(Material mat, String name)
        {
            this(new ItemStack(mat), name);
        }

        GuiItem(ItemStack item, String name, String[] metas)
        {
            this(item, name, Arrays.asList(metas));
        }

        GuiItem(ItemStack item, String name, List<String> loreLines)
        {
            this(item, name);
            if (loreLines != null && loreLines.size() > 0)
            {
                ItemMeta meta = this.item.getItemMeta();
                meta.setLore(loreLines);
                this.item.setItemMeta(meta);
            }
        }

        GuiItem(Material mat)
        {
            this(mat, null);
        }

        private boolean canBePickedUp()
        {
            return allowTaking;
        }

        String getPluralName()
        {
            return pluralName;
        }

        public ItemStack getItem()
        {
            return item;
        }

        public void setItem(ItemStack item)
        {
            if (item == null || !this.item.equals(item))
            {
                this.item = item == null ? new ItemStack(Material.AIR) : item;
                itemName = InventoryManager.getColorlessItemName(this.item);
            }
        }

        public void setMaterial(Material newMaterial, boolean refresh)
        {
            if (getMaterial() != newMaterial)
            {
                item.setType(newMaterial);
                if (refresh)
                {
                    refreshInventory();
                }
            }
        }

        public void setAmount(int newAmount, boolean refresh)
        {
            if (item.getAmount() != newAmount)
            {
                item.setAmount(newAmount);
                if (refresh)
                {
                    refreshInventory();
                }
            }
        }

        Material getMaterial()
        {
            return item.getType();
        }

        public void setName(String newName, boolean refresh)
        {
            if (!itemName.equals(newName))
            {
                ItemMeta meta = item.getItemMeta();
                meta.setDisplayName(newName);
                itemName = InventoryManager.getColorlessItemName(item);
                item.setItemMeta(meta);
                if (refresh)
                {
                    refreshInventory();
                }
            }
        }

        private void refreshInventory()
        {
            guiInv.refresh();
        }

        public String getName()
        {
            return itemName;
        }

        public void setLore(String[] newLore, boolean refresh)
        {
            setLore(Arrays.asList(newLore), refresh);
        }

        public void setLore(List<String> newLore, boolean refresh)
        {
            newLore = newLore == null ? new ArrayList<String>() : newLore;
            boolean same;
            if (item.getItemMeta().hasLore())
            {
                same = false;
            }
            else
            {
                same = newLore.size() == item.getItemMeta().getLore().size();
                if (same)
                {
                    List<String> currentLore = item.getItemMeta().getLore();
                    for (int i = 0; i < newLore.size() && i < currentLore.size(); i++)
                    {
                        if (!newLore.get(i).equals(currentLore.get(i)))
                        {
                            same = false;
                            break;
                        }
                    }
                }
            }
            if (!same)
            {
                ItemMeta meta = item.getItemMeta();
                meta.setLore(newLore);
                item.setItemMeta(meta);
                if (refresh)
                {
                    refreshInventory();
                }
            }
        }

        List<String> getLore()
        {
            return item.getItemMeta().getLore();
        }

        void removeFromGui()
        {
            guiInv.removeItem(this, true);
        }

        public abstract void performAction(Player clicker);

        private void setGuiInv(GuiInventory inventory)
        {
            this.guiInv = inventory;
        }
    }

    public static class GuiInventory
    {
        private static final String PAGE_NUMBER_PREFIX = " [";
        private static final String PAGE_NUMBER_SEPERATOR = "/";
        private static final String PAGE_SHOWER_POSTFIX = "]";
        private static final int MAX_ROWS = 6;

        private final ArrayList<GuiItem> contents;
        private String invTitle;
        private final ArrayList<Player> openers;
        private static final GuiItem fillerItem = new GuiItem(Material.THIN_GLASS) {
            @Override
            public void performAction(Player clicker) {}
        };
        private final boolean uniformPageSizes;

        private final List<PlayerAndPage> playersWithThisOpenAndPages;

        public void addAll(GuiItem[] items, boolean refresh)
        {
            addAll(Arrays.asList(items), refresh);
        }

        public void addAll(List<GuiItem> items, boolean refresh)
        {
            for (GuiItem item : items)
            {
                addItem(item, false);
            }
            if (refresh)
            {
                refresh();
            }
        }

        public void setTitle(String newTitle)
        {
            this.invTitle = newTitle == null ? invTitle : newTitle;
            refresh();
        }

        public boolean hasThisOpen(Player p)
        {
            return getPlayerWithThisOpen(p) != null;
        }

        private static class PlayerAndPage
        {
            int page;
            final Player player;

            PlayerAndPage(Player player, int page)
            {
                this.player = player;
                this.page = page;
            }
        }

        public GuiInventory(String title)
        {
            this(title, true);
        }

        public GuiInventory(String title, boolean uniformPageSizes)
        {
            this.playersWithThisOpenAndPages = new ArrayList<PlayerAndPage>();
            this.invTitle = title;
            this.contents = new ArrayList<GuiItem>();
            this.openers = new ArrayList<Player>();
            this.uniformPageSizes = uniformPageSizes;
        }

        public ArrayList<GuiItem> getContents()
        {
            return new ArrayList<GuiItem>(contents);
        }

        public void handleInventoryClose(Player playerWhoClosedIt)
        {
            openGuis.remove(playerWhoClosedIt);
            for (int i = 0; i < playersWithThisOpenAndPages.size(); i++)
            {
                if (playersWithThisOpenAndPages.get(i).player == playerWhoClosedIt)
                {
                    playersWithThisOpenAndPages.remove(i);
                    return;
                }
            }
        }

        public void giveContentsToPlayer(final Player p)
        {
            for (final GuiManager.GuiItem item : getContents())
            {
                if (item != null && item.getItem().getType() != Material.AIR)
                {
                    if (InventoryManager.canGive(p, item.getItem(), true))
                    {
                        InventoryManager.giveItem(p, item.getItem(), true, false);
                    }
                    else
                    {
                        p.getWorld().dropItemNaturally(p.getLocation(), item.getItem());
                    }
                }
            }
            Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
            {
                public String call()
                {
                    p.updateInventory();
                    return "";
                }
            });
        }

        public void setSize(int size, boolean refresh)
        {
            size = size < 0 ? 0 : size;
            boolean needToRefresh = true;
            if (contents.size() < size)
            {
                addAirItem(size - 1);
            }
            else if (size > 0)
            {
                while (contents.size() > size)
                {
                    contents.remove(contents.size() - 1);
                }
            }
            else if (size == 0)
            {
                contents.clear();
            }
            else
            {
                needToRefresh = false;
            }
            if (needToRefresh && refresh)
            {
                refresh();
            }
        }

        public void addItem(GuiItem item, boolean refreshNeededGuis, final Integer index)
        {
            item = item == null ? getAirItem() : item;
            if (index == null)
            {
                contents.add(item);
            }
            else
            {
                if (index > contents.size())
                {
                    while (index > contents.size())
                    {
                        int size1 = contents.size();
                        addAirItem();
                        if (size1 + 1 != contents.size())
                        {
                            sendErrorMessage("Error! Adding air triggered breakpoint!");
                            break;
                        }
                    }
                }
                else if (index < contents.size())
                {
                    contents.remove((int)index);
                }
                contents.add(index, item);
            }

            if (item != null)
            {
                item.setGuiInv(this);
            }

            if (refreshNeededGuis)
            {
                refresh();
            }
        }

        public final void addItem(GuiItem item, boolean refreshNeededGuis)
        {
            addItem(item, refreshNeededGuis, null);
        }

        public int getContentsSize()
        {
            return contents.size();
        }

        public boolean hasItem(ItemStack item)
        {
            for (GuiItem content : contents)
            {
                if (content != null && content.getItem().equals(item))
                {
                    return true;
                }
            }
            return false;
        }

        public void addFillerItem()
        {
            addItem(fillerItem, false);
        }

        public void addAirItem()
        {
            addAirItem(null);
        }

        public GuiItem getAirItem()
        {
            return new GuiItem(new ItemStack(Material.AIR), null, true)
            {
                @Override
                public void performAction(Player clicker)
                {
                    //do nothing
                }
            };
        }


        public final void addAirItem(Integer index)
        {
            addItem(getAirItem(), false, index);
        }

        public final void addItem(ItemStack item)
        {
            addItem(item, null);
        }

        public final void addItem(ItemStack item, Integer index)
        {
            addItem(item, index, false);
        }

        public final void addItem(ItemStack item, Integer index, boolean refresh)
        {
            addItem(new GuiItem(item, null, true)
            {
                @Override
                public void performAction(Player clicker)
                {
                    //do nothing
                }
            }, refresh, index);
        }

        public void removeItem(int index, boolean refresh)
        {
            if (index >= contents.size() || index < 0)
            {
                sendErrorMessage("Error! Tried to remove an item at index " + index +
                        " when there were only " + contents.size() + " items!!");
                return;
            }
            contents.remove(index);
            if (refresh)
            {
                refresh();
            }
        }

        public final void removeItem(ItemStack item, boolean refresh)
        {
            for (int i = 0; i < contents.size(); i++)
            {
                GuiItem guiItem = contents.get(i);
                if (guiItem != null && guiItem.getItem().equals(item))
                {
                    removeItem(i, refresh);
                    break;
                }
            }
        }

        public final void removeItem(GuiItem item, boolean refresh)
        {
            for (int i = 0; i < contents.size(); i++)
            {
                if (contents.get(i) == item)
                {
                    removeItem(i, refresh);
                    break;
                }
            }
        }

        public final void clearContents(boolean refresh)
        {
            contents.clear();
            if (refresh)
            {
                refresh();
            }
        }

        private final int getNumberOfPages()
        {
            return getLastPage() + 1;
        }

        private final int getLastPage()
        {
            return contents.size() <= 54 ? 0 : (contents.size() - 1)/45;
        }

        public boolean open(final Player player)
        {
            return open(player, 0);
        }

        private boolean open(final Player player, int zeroIndexedPage)
        {
            if (getContentsSize() > 0)
            {
                int maxSlotsInNonOnePage = (MAX_ROWS - 1)*9;
                int numberOfPages = getNumberOfPages();
                if (zeroIndexedPage < 0)
                {
                    zeroIndexedPage = 0;
                }
                else if (zeroIndexedPage >= numberOfPages)
                {
                    zeroIndexedPage = numberOfPages - 1;
                }
                boolean onePager = numberOfPages == 1;
                boolean firstPage = zeroIndexedPage == 0;
                boolean lastPage = zeroIndexedPage == numberOfPages - 1;
                int firstItem = zeroIndexedPage*maxSlotsInNonOnePage;
                int itemCountOnPage = lastPage ? contents.size() - firstItem : maxSlotsInNonOnePage;
                int rowsOnPage =                   onePager ? 1 + (itemCountOnPage - 1)/9  :
                                     uniformPageSizes ? MAX_ROWS                     : 2 + (itemCountOnPage - 1)/9;
                //1 + (itemCountOnPage - 1)/9 + (onePager ? 0 : 1);
                int pageSize = rowsOnPage*9;

                ItemStack[] itemContents = new ItemStack[pageSize];
                if (!onePager)
                {
                    int manySqueeze = numberOfPages > PAGES_TO_JUMP_MANY*2 ? 1 : 0;
                    ItemStack fillerItemStack = uniformPageSizes ? null : new ItemStack(Material.WOOL, 1, (short)0, (byte)7);
                    if (fillerItemStack != null)
                    {
                        for (int i = itemContents.length - (8 - manySqueeze); i < itemContents.length - (1 + manySqueeze); i++)
                        {
                            itemContents[i] = fillerItemStack;
                        }
                    }
                    itemContents[itemContents.length - (9 - manySqueeze)] = firstPage ? fillerItemStack : previousPageItem;
                    itemContents[itemContents.length - (1 + manySqueeze)] = lastPage ? fillerItemStack : nextPageItem;
                    if (manySqueeze != 0)
                    {
                        itemContents[itemContents.length - 9] = !firstPage  /*zeroIndexedPage >= PAGES_TO_JUMP_MANY*/              ? previousManyPageItem : fillerItemStack;
                        itemContents[itemContents.length - 1] =  !lastPage/*zeroIndexedPage < numberOfPages - PAGES_TO_JUMP_MANY*/ ? nextManyPageItem     : fillerItemStack;
                    }

                    if (numberOfPages > 20)
                    {
                        itemContents[itemContents.length - 5] = pageJumpItem;
                    }
                }

                for (int i = firstItem, contentIndex = 0; contentIndex < itemCountOnPage && i < contents.size(); i++, contentIndex++)
                {
                    GuiItem itemHere = contents.get(i);
                    itemContents[contentIndex] = itemHere == null ? new ItemStack(Material.AIR) : itemHere.getItem();
                }

                String actualTitle = invTitle + (numberOfPages > 1 ? PAGE_NUMBER_PREFIX + (zeroIndexedPage + 1) + PAGE_NUMBER_SEPERATOR + numberOfPages + PAGE_SHOWER_POSTFIX : "");
                if (actualTitle.length() > 32)
                {
                    sendErrorMessage("Error! \"" + actualTitle + "\" is " + (actualTitle.length() - 32) + " characters too long!");
                    actualTitle = actualTitle.substring(actualTitle.length() - 32, actualTitle.length());
                }
                final Inventory inv = Bukkit.createInventory(player, pageSize, actualTitle);
                inv.setContents(itemContents);
                openers.add(player);
                player.openInventory(inv);
                openers.remove(player);

                if (!openGuis.containsKey(player))
                {
                    openGuis.put(player, this);
                }
                PlayerAndPage playerAndPage = getPlayerWithThisOpen(player);
                if (playerAndPage == null)
                {
                    playersWithThisOpenAndPages.add(new PlayerAndPage(player, zeroIndexedPage));
                }
                else
                {
                    playerAndPage.page = zeroIndexedPage;
                }
                return true;
            }
            player.closeInventory();
            GuiInventory fallback = getFallbackInventory();
            if (fallback != null)
            {
                fallback.open(player);
            }
            else
            {
                String failure = getFailureMessage(player);
                if (failure != null && failure.length() > 0)
                {
                    player.sendMessage(failure);
                }
            }
            return false;
        }

        public String getFailureMessage(Player opener)
        {
            return ChatColor.RED + "There is nothing avaliable.";
        }

        public GuiInventory getFallbackInventory()
        {
            return null;
        }

        private boolean isBeingReopened(Player p)
        {
            return openers.contains(p);
        }

        protected List<PlayerAndPage> getPlayersWithThisOpenAndTheirPageNumbers()
        {
            return Collections.unmodifiableList(playersWithThisOpenAndPages);
        }

        public List<Player> getPlayersWithThisOpen()
        {
            List<Player> result = new ArrayList<Player>();
            for (PlayerAndPage playerAndPage : playersWithThisOpenAndPages)
            {
                result.add(playerAndPage.player);
            }
            return result;
        }

        private PlayerAndPage getPlayerWithThisOpen(Player player)
        {
            for (PlayerAndPage playerWithOpen : playersWithThisOpenAndPages)
            {
                if (playerWithOpen.player == player)
                {
                    return playerWithOpen;
                }
            }
            return null;
        }

        private final boolean handleInventoryClick(Inventory invClicked, int slot, final Player p, boolean leftClick, ItemStack currentItem)
        {
            int indexOfItem = getIndexOfItemInContents(p, slot);
            GuiItem guiItem = indexOfItem == -1 ? null : contents.get(indexOfItem);
            if (guiItem == null)
            {
                if (slot >= 0 && slot < invClicked.getSize())
                {
                    ItemStack item = invClicked.getItem(slot);
                    if (item != null)
                    {
                        if (item.equals(nextPageItem))
                        {
                            open(p, getRelativePage(p, 1));
                        }
                        else if (item.equals(previousPageItem))
                        {
                            open(p, getRelativePage(p, -1));
                        }
                        else if (item.equals(nextManyPageItem))
                        {
                            open(p, getRelativePage(p, PAGES_TO_JUMP_MANY));
                        }
                        else if (item.equals(previousManyPageItem))
                        {
                            open(p, getRelativePage(p, -PAGES_TO_JUMP_MANY));
                        }
                        else if (item.equals(pageJumpItem))
                        {
                            p.closeInventory();
                            new Verifier.IntegerVerifier(p, ChatColor.YELLOW + "Enter the page number you'd like to go to:", ChatColor.RED + "Invalid page number!")
                            {
                                @Override
                                public void performAction(Integer decision)
                                {
                                    open(p, decision - 1);
                                }
                            };
                        }
                    }
                }
                return true;
            }
            return handleInventoryClick(invClicked, slot, indexOfItem, p, guiItem, leftClick, currentItem);
        }

        protected boolean handleInventoryClick(Inventory invClicked, int invSlot, int guiContentSlot, final Player p, GuiItem guiItem, boolean leftClick, ItemStack currentItem)
        {
//                if (invClicked != p.getOpenInventory().getTopInventory())
//                {
//                    return true;
//                }
            guiItem.performAction(p);
            return !guiItem.allowTaking;
//                for (GuiItem guiItem : contents)
//                {
//                    if (guiItem != null && guiItem.getSpecialItem().equals(item))
//                    {
//                        guiItem.performAction(p);
//                        if (!guiItem.allowTaking)
//                        {
//                            event.setCancelled(true);
//                        }
//                        return;
//                    }
//                }
        }

        private int getIndexOfItemInContents(Player p, int slotInPage)
        {
            int page = getPage(p);
            int lastPage = getLastPage();
            final int result;
            if (lastPage == 0)
            {
                result = slotInPage;
            }
            else if (lastPage > 0)
            {
                result = slotInPage >= 45 ? -1 : (page*45 + slotInPage);
            }
            else
            {
                sendErrorMessage("Negative pages?");
                result = -1;
            }
            return result >= contents.size() ? -1 : result;
        }

        protected int getPage(Player p)
        {
            PlayerAndPage playerAndPage = getPlayerWithThisOpen(p);
            if (playerAndPage != null)
            {
                return playerAndPage.page;
            }
//            try
//            {
//                String title = p.getOpenInventory().getTopInventory().getTitle();
//                if (!title.startsWith(invTitle))
//                {
//                    sendErrorMessage("Error! Tried to get the page of an improper inventory: " + title + " for " + p.getName());
//                    return 0;
//                }
//                else if (title.length() == invTitle.length())
//                {
//                    return 0;
//                }
//                StringBuilder builder = new StringBuilder();
//                for (int i = invTitle.length() + PAGE_NUMBER_PREFIX.length(); i < title.length(); i++)
//                {
//                    char c = title.charAt(i);
//                    if (c == PAGE_NUMBER_SEPERATOR.charAt(0))
//                    {
//                        break;
//                    }
//                    builder.append(c);
//                }
//                int newPage = Integer.valueOf(builder.toString()) - 1 + relativePage;
//                int numberOfPages = getNumberOfPages();
//                return newPage < 0 ? 0 :
//                       newPage >= numberOfPages ? numberOfPages :
//                       newPage;
//            }
//            catch (NullPointerException ex) {}
//            catch (NumberFormatException ex)
//            {
//                ex.printStackTrace();
//            }
            sendErrorMessage("Error! Didn't know which page " + p.getName() + " was on!");
            return 0;
        }

        private int getRelativePage(Player p, int relativePage)
        {
            return getPage(p) + relativePage;
//            try
//            {
//                String title = p.getOpenInventory().getTopInventory().getTitle();
//                if (!title.startsWith(invTitle))
//                {
//                    sendErrorMessage("Error! Tried to get the page of an improper inventory: " + title + " for " + p.getName());
//                    return 0;
//                }
//                else if (title.length() == invTitle.length())
//                {
//                    return 0;
//                }
//                StringBuilder builder = new StringBuilder();
//                for (int i = invTitle.length() + PAGE_NUMBER_PREFIX.length(); i < title.length(); i++)
//                {
//                    char c = title.charAt(i);
//                    if (c == PAGE_NUMBER_SEPERATOR.charAt(0))
//                    {
//                        break;
//                    }
//                    builder.append(c);
//                }
//                int newPage = Integer.valueOf(builder.toString()) - 1 + relativePage;
//                int numberOfPages = getNumberOfPages();
//                return newPage < 0 ? 0 :
//                       newPage >= numberOfPages ? numberOfPages :
//                       newPage;
//            }
//            catch (NullPointerException ex) {}
//            catch (NumberFormatException ex)
//            {
//                ex.printStackTrace();
//            }
        }

        public void refresh()
        {
            List<PlayerAndPage> playersWithThisOpenCopy = getPlayersWithThisOpenAndTheirPageNumbers();
            for (int i = 0; i < playersWithThisOpenCopy.size(); i++)
            {
                PlayerAndPage player = playersWithThisOpenCopy.get(i);
                open(player.player, player.page);
            }
        }

        private String getInventoryTitle()
        {
            return invTitle;
        }
    }

    public static void forgetGui(Player p)
    {
        GuiInventory openGui = openGuis.get(p);
        if (openGui != null)
        {
            openGui.handleInventoryClose(p);
            openGuis.remove(p);
        }
    }
}