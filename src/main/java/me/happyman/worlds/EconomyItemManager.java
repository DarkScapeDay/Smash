package me.happyman.worlds;

import me.happyman.utils.FileManager;
import me.happyman.utils.InventoryManager;
import me.happyman.utils.Verifier;
import org.apache.commons.io.FilenameUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static me.happyman.Plugin.sendErrorMessage;
import static me.happyman.utils.InventoryManager.getPluralName;
import static me.happyman.worlds.EconomyCurrencyManager.GOLD_COUNT_DATANAME;
import static me.happyman.worlds.WorldType.getWorldType;

public class EconomyItemManager
{
//            private static HashMap<World, HashMap<Material, GuiManager.GuiInventory>> materialEconomy = new HashMap<World, HashMap<Material, GuiManager.GuiInventory>>();
//        private static HashMap<World, HashMap<String, GuiManager.GuiInventory>> uuidEconomy = new HashMap<World, HashMap<String, GuiManager.GuiInventory>>();
//            private final String sellerName;
//        private final String sellerUUID;
//        private final ItemStack sellingItemStack;
//        private final short amount;
//        private final float priceForLot;
//        private final String world;
//        private final File playerPointerFile;
//        private final File materialFolder;
//        private static ArrayList<GuiManager.GuiInventory> getInventories(String playerName, String sellerUUID, Material item, String w)
//        {
//            ArrayList<GuiManager.GuiInventory> result = new ArrayList<GuiManager.GuiInventory>();
//
//            HashMap<Material, GuiManager.GuiInventory> materialEconomyForString = materialEconomy.get(w);
//            if (materialEconomyForString == null)
//            {
//                materialEconomyForString = new HashMap<Material, GuiManager.GuiInventory>();
//                materialEconomy.put(w, materialEconomyForWorld);
//            }
//            GuiManager.GuiInventory inventory = materialEconomyForWorld.get(item);
//            if (inventory == null)
//            {
//                inventory = new GuiManager.GuiInventory(item.colorlessName() + "s");
//                materialEconomyForWorld.put(item, inventory);
//            }
//            result.add(inventory);
//
//            getItemsBeingSold(sellerUUID, w);
//            HashMap<String, GuiManager.GuiInventory> uuidEconomyForString = uuidEconomy.get(w);
//            if (uuidEconomyForString == null)
//            {
//                uuidEconomyForString = new HashMap<String, GuiManager.GuiInventory>();
//                uuidEconomy.put(w, uuidEconomyForWorld);
//            }
//            inventory = uuidEconomyForWorld.get(sellerUUID);
//            if (inventory == null)
//            {
//                inventory = new GuiManager.GuiInventory(playerName + "'s Products");
//                uuidEconomyForWorld.put(sellerUUID, inventory);
//            }
//            result.add(inventory);
//
//            return result;
//        public ItemForPurchase(Player p, Material item, String colorlessName, Map<Enchantment, Integer> enchants,
//                               short itemDurability, short amount, float price)
//        {
//            this(p, item, colorlessName, enchants, itemDurability, amount, price, null);
//        }

//        public ItemForPurchase(File playerPointerFile, File materialFolder)
//        {
//            this.playerPointerFile = playerPointerFile;
//            this.materialFolder = materialFolder;
//        }
//        private ItemForPurchase(GuiManager.GuiInventory guiInv, String playerName, String sellerUUID, String world, ItemStack item, short amount, float price, long timeOfMarketAddition)
//        {
//            super(guiInv, item);
//            this.sellerUUID = sellerUUID;
//            this.sellerName = playerName;
//            this.world = world;
//            this.amount = amount;
//            this.priceForLot = price;
//            this.playerPointerFile = getPlayerPointerFile(sellerUUID, world);
//            this.materialFolder = getMaterialFolder(getPreservedItem().getType(), world, "" + timeOfMarketAddition);
//            this.sellingItemStack = getSellingItemStack(item, price, playerName);
//        }
//        private ItemForPurchase(Player p, Material item, String colorlessName, Map<Enchantment, Integer> enchants,
//                               short itemDurability, short amount, float price, Long timeOfMarketAddition)
//        {
//            super(getInventories(p.getName(), getCapitalNameAndUUID(p), item, p.getWorld()), getSellingItemStack(p, item, colorlessName, enchants, itemDurability, amount, price));
//            this.sellerUUID = getCapitalNameAndUUID(p);
//            this.sellerName = p.getName();
//            this.world = p.getWorld();
//            this.amount = amount;
//            this.priceForLot = price;
//            long time = timeOfMarketAddition == null ? System.currentTimeMillis() : timeOfMarketAddition;
//            this.playerPointerFile = getPlayerPointerFile(sellerUUID, world);
//            this.materialFolder = getMaterialFolder(getPreservedItem().getType(), world, "" + time);
//            sellingItemStack = null;
//        }
//        final ItemForPurchase itefd=  new ItemForPurchase(Bukkit.getAttacker("HappyMan"), Material.DIAMOND, null, null, (short)0, (short)211, 121);
//        itefd.giveToPlayer(Bukkit.getAttacker("HappyMan"));
//        Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable()
//        {
//
//            @Override
//            public void run() {
    //Bukkit.broadcastMessage(ChatColor.YELLOW + getGeneralPlayerData(getPlayerFile("HappyMan", Bukkit.getAttacker("HappyMan").getWorld(), "EconomyItems.json"), ECONOMY_FILE_NAME).toString());
//                itefd.addItemToEconomy();
//                itefd.removeFromEconomyAndGiveToPlayer();
    //Bukkit.broadcastMessage(ChatColor.YELLOW +getGeneralPlayerData(getPlayerFile("HappyMan", Bukkit.getAttacker("HappyMan").getWorld(), "EconomyItems.json"), ECONOMY_FILE_NAME).toString());
//
//            }
//        }, 20);
//   itefd.removeFromEconomyAndGiveToPlayer();
//

    public static final String BUY_CMD = "buy";
    public static final String SELL_CMD = "sell";
    public static final String CONSOLIDATE_COMMAND = "consolidate";
    private static final float MAXIMUM_PRICE = 20000f;
    private static final short MAXIMUM_ITEMS_AT_ONCE = 192;
    private static final String ECONOMY_FOLDER_NAME = "Economy";
    private static final String ECONOMY_POINTER_DATANAME = ECONOMY_FOLDER_NAME;
    private static final String UUID_DATANAME = "sellerUUID";
    private static final String WORLD_DATANAME = "world";
    private static final String PRICE_DATANAME = "price";
    private static final String AMOUNT_DATANAME = "amount";
    private static final short MAXIMUM_MARKET_ITEMS = 100;
    private static final GuiManager.GuiItem allMaterialsGuiItem = new GuiManager.GuiItem(new ItemStack(Material.WORKBENCH/*, 1, (byte)1*/), ChatColor.GOLD + "All Materials")
    {
        @Override
        public void performAction(Player clicker)
        {
            getGlobalMaterialInventory(clicker.getWorld(), true).open(clicker);
        }
    };
    private static final GuiManager.GuiItem allPlayersGuiItem = new GuiManager.GuiItem(new ItemStack(Material.SKULL_ITEM, 1, (byte)3), ChatColor.GOLD + "All Players")
    {
        @Override
        public void performAction(Player clicker)
        {
            getGlobalUUIDInventory(clicker.getWorld(), true).open(clicker);
        }
    };

    private static final HashMap<World, GlobalUUIDInventory> globalSellerUUIDCache = new HashMap<World, GlobalUUIDInventory>();
    private static final HashMap<World, GlobalMaterialInventory> globalMaterialCache = new HashMap<World, GlobalMaterialInventory>();
    private static final HashMap<World, HashMap<String, LocalUUIDInventory>> localUUIDCache = new HashMap<World, HashMap<String, LocalUUIDInventory>>();
    private static final HashMap<World, HashMap<Material, LocalMaterialInventory>> localMaterialCache = new HashMap<World, HashMap<Material, LocalMaterialInventory>>();

    private static final ChatColor INVENTORY_NAME_COLOR = ChatColor.BLACK;

    static class GlobalUUIDInventory extends WorldGuiInventory
    {
        @Override
        public void handleInventoryClose(Player p)
        {
            super.handleInventoryClose(p);
            if (getPlayersWithThisOpenAndTheirPageNumbers().size() == 0)
            {
                globalSellerUUIDCache.remove(getWorld());
            }
        }

        @Override
        public void addFirstItems()
        {
            addFillerItem();
            addFillerItem();
            addFillerItem();
            addItem(allMaterialsGuiItem, false);
            addFillerItem();
            addFillerItem();
            addFillerItem();
            addFillerItem();
            addFillerItem();
        }

        private static String getInventoryTitle()
        {
            return INVENTORY_NAME_COLOR + "All Players' Shops";
        }

        private GlobalUUIDInventory(World world)
        {
            super(getInventoryTitle(), world);

            File[] playerFolders = FileManager.getPlayerDataFolder(world).listFiles();
            if (playerFolders != null)
            {
                for (File playerFolder : playerFolders)
                {
                    final String uuid = FilenameUtils.removeExtension(playerFolder.getName());
                    if (uuid.length() == 0x20 && LocalUUIDInventory.getListOfItemFolders(uuid, world).size() > 0)
                    {
                        addItem(getGlobalUUIDItem(uuid, world), false);
                    }
                }
            }
        }

        @Override
        public String getFailureMessage(Player p)
        {
            return ChatColor.RED + "Well it looks like nobody is selling anything.";
        }
    }

    static class GlobalMaterialInventory extends WorldGuiInventory
    {
        @Override
        public void handleInventoryClose(Player p)
        {
            super.handleInventoryClose(p);
            if (getPlayersWithThisOpenAndTheirPageNumbers().size() == 0)
            {
                globalMaterialCache.remove(getWorld());
            }
        }

        @Override
        void addFirstItems()
        {
            addFillerItem();
            addFillerItem();
            addFillerItem();
            addFillerItem();
            addFillerItem();
            addItem(allPlayersGuiItem, false);
            addFillerItem();
            addFillerItem();
            addFillerItem();
        }

        private static String getInventoryTitle()
        {
            return INVENTORY_NAME_COLOR + "Global Market";
        }

        private GlobalMaterialInventory(World world)
        {
            super(getInventoryTitle(), world);

            File[] folderList = getMotherMaterialFolder(world).listFiles();
            if (folderList != null)
            {
                for (File parentMatFolder : folderList)
                {
                    final Material mat = Material.getMaterial(parentMatFolder.getName());
                    if (mat != null)
                    {
                        addItem(getGlobalMaterialItem(mat, world), false);
                    }
                }
            }
        }

        @Override
        public String getFailureMessage(Player p)
        {
            return ChatColor.RED + "Well it looks like nothing is being sold right now.";
        }
    }

    static class LocalUUIDInventory extends LocalGuiInventory
    {
        private final String uuid;

        @Override
        public void handleInventoryClose(Player p)
        {
            super.handleInventoryClose(p);
            if (getPlayersWithThisOpenAndTheirPageNumbers().size() == 0)
            {
                HashMap<String, LocalUUIDInventory> uuidMap = localUUIDCache.get(getWorld());
                if (uuidMap != null)
                {
                    uuidMap.remove(uuid);
                    if (uuidMap.size() == 0)
                    {
                        localUUIDCache.remove(getWorld());
                    }
                }
                else
                {
                    sendErrorMessage("Error! Something's up with local uuid inventory cache!");
                }
            }
        }

        private static String getInventoryTitle(String sellerName)
        {
            sellerName = UUIDFetcher.getCapitalName(sellerName);
            if (sellerName != null)
            {
                return INVENTORY_NAME_COLOR + sellerName + "'s Shop";
            }
            return INVENTORY_NAME_COLOR + "Erroneous Seller";
        }

        private static List<String> getListOfItemFolders(String uuid, World world)
        {
            return getListOfItemFolders(getPlayerPointerFile(uuid, world, false));
        }

        private static List<String> getListOfItemFolders(File playerPointerFile)
        {
            if (playerPointerFile.exists())
            {
                return FileManager.getDataList(playerPointerFile, ECONOMY_POINTER_DATANAME);
            }
            return new ArrayList<String>();
        }

        private LocalUUIDInventory(String uuid, World world)
        {
            super(getInventoryTitle(uuid), world);
            this.uuid = uuid;
            if (uuid != null)
            {
                final File playerPointerFile = getPlayerPointerFile(uuid, world, false);
                final List<String> directories = getListOfItemFolders(playerPointerFile);

                boolean filesMissing = false;
                for (int i = 0; i < directories.size(); i++)
                {
                    File f = FileManager.getWorldFolder(world, ECONOMY_FOLDER_NAME + "/" + directories.get(i));

                    if (!addItem(f, false))
                    {
                        filesMissing = true;
                        sendErrorMessage("Warning! " + f.getAbsolutePath() + " was missing from " + UUIDFetcher.getCapitalName(uuid) + "'s shop!");
                        directories.remove(i--);
                    }
                }

                if (filesMissing)
                {
                    //Bukkit.broadcastMessage("" + getGeneralPlayerData(playerPointerFile, ECONOMY_FOLDER_NAME));
                    FileManager.putDataList(playerPointerFile, ECONOMY_POINTER_DATANAME, directories);
                }
            }
        }

        @Override
        public String getFailureMessage(Player opener)
        {
            return                              uuid == null ? (ChatColor.RED + "Player not found.") :
                    UUIDFetcher.getUUID(opener).equals(uuid) ? (ChatColor.RED + "You aren't selling anything") :
                                                               (ChatColor.RED + "" + UUIDFetcher.getCapitalName(uuid) + " is currently not selling anything.");//ChatColor.RED + "It looks like " + UUIDFetcher.getCapitalName(uuid) + " isn't selling anything.";
        }

        @Override
        public GuiManager.GuiInventory getFallbackInventory()
        {
            return getGlobalUUIDInventory(getWorld(), true);
        }
    }

    static class LocalMaterialInventory extends LocalGuiInventory
    {
        private final Material mat;

        @Override
        public void handleInventoryClose(Player p)
        {
            super.handleInventoryClose(p);
            if (getPlayersWithThisOpenAndTheirPageNumbers().size() == 0)
            {
                HashMap<Material, LocalMaterialInventory> matMap = localMaterialCache.get(getWorld());
                if (matMap != null)
                {
                    matMap.remove(mat);
                    if (matMap.size() == 0)
                    {
                        localMaterialCache.remove(getWorld());
                    }
                }
                else
                {
                    sendErrorMessage("Error! Something's up with local material inventory cache!");
                }
            }
        }

        private static String getInventoryTitle(Material mat)
        {
            if (mat != null)
            {
                String pluralName = getPluralName(new ItemStack(mat), (short)2).replaceAll("_", " ");
                return pluralName.length() > 1 ? (INVENTORY_NAME_COLOR + "" + Character.toUpperCase(pluralName.charAt(0)) + pluralName.substring(1, pluralName.length())) : pluralName;
            }
            return INVENTORY_NAME_COLOR + "Erroneous Material";
        }

        private static File[] getListOfItemFolders(Material mat, World world)
        {
            File materialFolder = getMaterialFolder(mat, world, "");
            File[] folderList = !materialFolder.exists() ? null : materialFolder.listFiles();
            if (folderList == null)
            {
                return new File[0];
            }
            return folderList;
        }

        private LocalMaterialInventory(Material mat, World world)
        {
            super(getInventoryTitle(mat), world);
            this.mat = mat;
            File[] folderList = getListOfItemFolders(mat, world);

            for (File f : folderList)
            {
                if (!addItem(f, true))
                {
                    File extraDataFile = getExtraDataFile(f, false);
                    FileManager.EntrySet stuff = getRelaventDataMapForInput(extraDataFile);
                    String uuid = stuff.get(UUID_DATANAME);
                    sendErrorMessage("Warning! " + f.getAbsolutePath() + " was missing from the shop of " + mat.name().toLowerCase() + "s!");
                    String worldName = stuff.get(WORLD_DATANAME);
                    File pointerFile = getPlayerPointerFile(uuid, worldName);
                    FileManager.removeDataFromList(pointerFile, ECONOMY_POINTER_DATANAME, getMaterialFolderName(mat) + "/" + f.getName());
                }
            }
        }

        @Override
        public String getFailureMessage(Player p)
        {
            return ChatColor.RED + "No " + InventoryManager.getPluralName(mat, true) + " are being sold!";
        }

        @Override
        public GuiManager.GuiInventory getFallbackInventory()
        {
            return getGlobalMaterialInventory(getWorld(), true);
        }
    }


    static class LocalGuiInventory extends WorldGuiInventory
    {
        private LocalGuiInventory(String title, World world)
        {
            super(title, world);
        }

        boolean addItem(File folderWithThatSweetSweetDataInIt, boolean showWhoIsSelling)
        {
            File[] filesInside = folderWithThatSweetSweetDataInIt.listFiles();
            if (filesInside != null && filesInside.length != 0)
            {
                File dataFile = getExtraDataFile(folderWithThatSweetSweetDataInIt, false);
                File itemstackFile = getStackFile(folderWithThatSweetSweetDataInIt, false);
                if (dataFile.exists() && itemstackFile.exists())
                {
                    try
                    {
                        FileInputStream stream = new FileInputStream(itemstackFile);
                        BukkitObjectInputStream inputStream = new BukkitObjectInputStream(stream);

                        ItemStack item = (ItemStack)inputStream.readObject();
                        inputStream.close();
                        FileManager.EntrySet datas = getRelaventDataMapForInput(dataFile);
                        World world = Bukkit.getWorld(datas.get(WORLD_DATANAME));
                        if (world == null)
                        {
                            sendErrorMessage("Error! Failed to find world when adding an item to a local shop gui inventory!");
                            return false;
                        }
                        addItem(new ItemForPurchase(folderWithThatSweetSweetDataInIt, item, world, Float.valueOf(datas.get(PRICE_DATANAME)), Short.valueOf(datas.get(AMOUNT_DATANAME)), datas.get(UUID_DATANAME), showWhoIsSelling), false);
                        return true;
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    catch (ClassNotFoundException e)
                    {
                        e.printStackTrace();
                    }
                }
            }
            return false;
        }

        @Override
        void addFirstItems()
        {
            addFillerItem();
            addFillerItem();
            addFillerItem();
            addItem(allMaterialsGuiItem, false);
            addFillerItem();
            addItem(allPlayersGuiItem, false);
            addFillerItem();
            addFillerItem();
            addFillerItem();
        }
    }

    static abstract class WorldGuiInventory extends GuiManager.GuiInventory
    {
        private final int numberOfFirstItems;

        @Override
        public int getContentsSize()
        {
            return super.getContentsSize() - numberOfFirstItems;
        }

        abstract void addFirstItems();

        private final World world;

        private WorldGuiInventory(String title, World world)
        {
            super(title);
            addFirstItems();
            this.numberOfFirstItems = getContentsSize();
            this.world = world;
        }

        World getWorld()
        {
            return world;
        }
    }

    private static class ItemForPurchase extends GuiManager.GuiItem
    {
        private final ItemStack preservedLoreItem;
        private final File folder;
        private final World world;
        private final String sellerUUID;
        private final float price;
        private final short amount;

        //Get the data from file and make an inventory
        private static List<String> getLoreLines(String folderName, float price, short amount, String seller, boolean showWhoIsSelling)
        {
            List<String> lores = new ArrayList<String>();
            if (amount > 64)
            {
                lores.add(ChatColor.GOLD + "" + ChatColor.BOLD + "" + amount + " units");
            }
            lores.add(ChatColor.GREEN + "Price: " + (price < 100 ? ("" + price) : ("" + (int)price)) + " " + GOLD_COUNT_DATANAME);
            if (showWhoIsSelling)
            {
                lores.add(ChatColor.WHITE + "" + ChatColor.UNDERLINE + "Seller" + ChatColor.RESET + ": " + UUIDFetcher.getCapitalName(seller));
            }
            if (folderName.length() >= 10)
            {
                lores.add(ChatColor.BLACK + folderName.substring(folderName.length() - 10, folderName.length()));
            }
            return lores;
        }

        private ItemForPurchase(File folderWithTheDataInIt, ItemStack item, World world, float price, short amount, String sellerUUID, boolean showWhoIsSelling)
        {
            super(new ItemStack(item), null, getLoreLines(folderWithTheDataInIt.getName(), price, amount, sellerUUID, showWhoIsSelling));
            this.folder = folderWithTheDataInIt;
            this.preservedLoreItem = item;
            getItem().setAmount(amount > 64 ? 64 : amount);
//            getSpecialItem().setAmount(amount);
            this.sellerUUID = UUIDFetcher.getUUID(sellerUUID);
            this.price = price;
            this.amount = amount;
            this.world = world;
        }

        private ItemStack getPreservedItem()
        {
            return preservedLoreItem;
        }

        public String getSellerUUID()
        {
            return sellerUUID;
        }

        private void removeFromEconomyAndGiveToPlayer(Player buyer)
        {
            //material file
            File itemStackFile = getStackFile(folder, false);
            File dataFile = getExtraDataFile(folder, false);

            //player file
            FileManager.removeDataFromList(getPlayerPointerFile(sellerUUID, world), ECONOMY_POINTER_DATANAME, getItemFolderPath(folder, world));

            if (itemStackFile.exists() && dataFile.exists() && itemStackFile.delete() && dataFile.delete() && folder.delete())
            {
                File parentFolder = folder.getParentFile();
                File[] brothesAndSisters = parentFolder.listFiles();
                if (brothesAndSisters != null && brothesAndSisters.length == 0)
                {
                    parentFolder.delete();
                }

                Player seller = Bukkit.getPlayer(UUIDFetcher.getCapitalName(sellerUUID));
                if (seller != buyer)
                {
                    buyer.sendMessage(ChatColor.GREEN + "Purchase successful!");
                    seller.sendMessage(ChatColor.GREEN + "" + seller.getName() + "Has bought your " + getPluralName() + " from the market!");
                }
                else
                {
                    buyer.sendMessage(ChatColor.GREEN + "Removed " + getPluralName() + " from the market.");
                }
                InventoryManager.giveItem(buyer, getPreservedItem(), amount, true, true);
            }
            else
            {
                buyer.sendMessage(ChatColor.RED + "That " + getPluralName() + " is no longer for sale.");
            }


            WorldGuiInventory globalUUIDInventory = getGlobalUUIDInventory(world, false);
            if (globalUUIDInventory != null && LocalUUIDInventory.getListOfItemFolders(sellerUUID, world).size() == 0) //no items left
            {
                globalUUIDInventory.removeItem(getGlobalUUIDItemStack(sellerUUID), true);
            }

            WorldGuiInventory globalMaterialInventory = getGlobalMaterialInventory(world, false);
            if (globalMaterialInventory != null && LocalMaterialInventory.getListOfItemFolders(getMaterial(), world).length == 0)
            {
                globalMaterialInventory.removeItem(getGlobalMaterialItemStack(getMaterial()), true);
            }

            WorldGuiInventory localUUIDInventory = getLocalUUIDInventory(sellerUUID, world, false);
            if (localUUIDInventory != null)
            {
                localUUIDInventory.removeItem(getItem(), true);
            }

            WorldGuiInventory localMaterialInventory = getLocalMaterialInventory(getMaterial(), world, false);
            if (localMaterialInventory != null)
            {
                localMaterialInventory.removeItem(getItem(), true);
            }
        }

        private boolean canGive(Player p)
        {
            if (!InventoryManager.canGive(p, preservedLoreItem, amount,true))
            {
                Player seller = Bukkit.getPlayer(UUIDFetcher.getCapitalName(sellerUUID));
                if (seller != p)
                {
                    p.sendMessage(ChatColor.RED + "Please make space in your inventory and try buying the " + getPluralName() + " again.");
                }
                else
                {
                    p.sendMessage(ChatColor.RED + "You don't have enough inventory space to take your " + getPluralName() + " back!");
                }
                return false;
            }
            return true;
        }

        @Override
        public void performAction(final Player clicker)
        {
            String sellerName = UUIDFetcher.getCapitalName(sellerUUID);

            if (!clicker.getName().equals(sellerName))
            {
                new Verifier.BooleanVerifier(clicker,  ChatColor.YELLOW + "Buy " + amount + " " + getPluralName() + " from " + sellerName + " for " + (int)price + " " + GOLD_COUNT_DATANAME + "?")
                {
                    @Override
                    public void performYesAction()
                    {
                        float neededGold = price - EconomyCurrencyManager.getGold(clicker);
                        if (neededGold > 0)
                        {
                            String thatThose = amount == 1 ? "that" : "those";
                            clicker.sendMessage(ChatColor.RED + "Sorry, you need " + Math.round(neededGold) + " more " + GOLD_COUNT_DATANAME + " to be able to afford " + thatThose + " " + getPluralName() + ".");
                        }
                        else if (canGive(clicker) && EconomyCurrencyManager.transferGold(clicker, sellerUUID, price))
                        {
                            removeFromEconomyAndGiveToPlayer(clicker);
                        }
                    }

                    @Override
                    public void performNoAction()
                    {
                        clicker.sendMessage(ChatColor.RED + "Purchase cancelled!");
                    }
                };
            }
            else if (canGive(clicker))
            {
                removeFromEconomyAndGiveToPlayer(clicker);
            }
        }
    }

//        ItemStack item = new ItemStack(item);
//            item.setAmount(amount > item.getMaxStackSize() ? item.getMaxStackSize() : amount);
//            item.setDurability(itemDurability);
//
//        ItemMeta meta = item.getItemMeta();
//            meta.setLore(getLoreLines(price, amount, seller));
//            if (enchants != null)
//        {
//            for (Map.Entry<Enchantment, Integer> enchantment : enchants.entrySet())
//            {
//                meta.addEnchant(enchantment.getKey(), enchantment.getValue(), true);
//            }
//        }
//            if (customItemName != null)
//        {
//            meta.setDisplayName(customItemName);
//        }
//            item.setItemMeta(meta);
//            return item;

    //*****************
    //*********************************

    public static WorldGuiInventory getGlobalUUIDInventory(Player player)
    {
        return getGlobalUUIDInventory(player.getWorld());
    }

    private static WorldGuiInventory getGlobalUUIDInventory(World world)
    {
        return getGlobalUUIDInventory(world, true);
    }

    private static WorldGuiInventory getGlobalUUIDInventory(World world, boolean createIfNeeded)
    {
        GlobalUUIDInventory known = globalSellerUUIDCache.get(world);
        if (known == null && createIfNeeded)
        {
            known = new GlobalUUIDInventory(world);
            globalSellerUUIDCache.put(world, known);
        }
        return known;
    }

    private static ItemStack getGlobalUUIDItemStack(String uuid)
    {
        ItemStack result = new ItemStack(Material.getMaterial((int)uuid.charAt(0) - (int)'/'));
        ItemMeta meta = result.getItemMeta();
        meta.setDisplayName(UUIDFetcher.getCapitalName(uuid));
        result.setItemMeta(meta);
        return result;
    }

    private static GuiManager.GuiItem getGlobalUUIDItem(final String uuid, final World world)
    {
        return new GuiManager.GuiItem(getGlobalUUIDItemStack(uuid))
        {
            @Override
            public void performAction(Player clicker)
            {
                getLocalUUIDInventory(uuid, world, true).open(clicker);
            }
        };
    }

    //*****************

    public static WorldGuiInventory getGlobalMaterialInventory(Player player)
    {
        return getGlobalMaterialInventory(player.getWorld());
    }

    private static WorldGuiInventory getGlobalMaterialInventory(World world)
    {
        return getGlobalMaterialInventory(world, true);
    }

    private static WorldGuiInventory getGlobalMaterialInventory(World world, boolean createIfNeeded)
    {
        GlobalMaterialInventory known = globalMaterialCache.get(world);
        if (known == null && createIfNeeded)
        {
            known = new GlobalMaterialInventory(world);
            globalMaterialCache.put(world, known);
        }
        return known;
    }

    private static ItemStack getGlobalMaterialItemStack(Material mat)
    {
        return new ItemStack(mat);
    }

    private static GuiManager.GuiItem getGlobalMaterialItem(final Material mat, final World world)
    {
        return new GuiManager.GuiItem(getGlobalMaterialItemStack(mat))
        {
            @Override
            public void performAction(Player clicker)
            {
                getLocalMaterialInventory(mat, world, true).open(clicker);
            }
        };
    }

    //*****************

    public static WorldGuiInventory getLocalMaterialInventory(Material mat, Player player)
    {
        return getLocalMaterialInventory(mat, player.getWorld());
    }

    private static WorldGuiInventory getLocalMaterialInventory(Material material, World world)
    {
        return getLocalMaterialInventory(material, world, true);
    }

    private static WorldGuiInventory getLocalMaterialInventory(Material material, World world, boolean createIfNeeded)
    {
        HashMap<Material, LocalMaterialInventory> matMap = localMaterialCache.get(world);
        if (matMap == null)
        {
            if (createIfNeeded)
            {
                matMap = new HashMap<Material, LocalMaterialInventory>();
                localMaterialCache.put(world, matMap);
            }
            else
            {
                return null;
            }
        }

        LocalMaterialInventory known = matMap.get(material);
        if (known == null && createIfNeeded)
        {
            known = new LocalMaterialInventory(material, world);
            matMap.put(material, known);
        }
        return known;
    }

    private static ItemForPurchase getLocalMaterialItem(File particularFolder, ItemStack item, World world, float price, short amount, String sellerUUID)
    {
        return new ItemForPurchase(particularFolder, item, world, price, amount, sellerUUID, true);
    }

    //*****************

    public static WorldGuiInventory getLocalUUIDInventory(Player p)
    {
        return getLocalUUIDInventory(p.getName(), p);
    }

    public static WorldGuiInventory getLocalUUIDInventory(String seller, Player p)
    {
        return getLocalUUIDInventory(seller, p.getWorld());
    }

    private static WorldGuiInventory getLocalUUIDInventory(String p, World world)
    {
        String uuid = UUIDFetcher.getUUID(p);
        if (uuid == null)
        {
            return getGlobalUUIDInventory(world, true);
        }
        return getLocalUUIDInventory(p, world, true);
    }

    private static WorldGuiInventory getLocalUUIDInventory(String sellerUUID, World world, boolean createIfNeeded)
    {
        HashMap<String, LocalUUIDInventory> uuidMap = localUUIDCache.get(world);
        if (uuidMap == null)
        {
            if (createIfNeeded)
            {
                uuidMap = new HashMap<String, LocalUUIDInventory>();
                localUUIDCache.put(world, uuidMap);
            }
            else
            {
                return null;
            }
        }

        LocalUUIDInventory known = uuidMap.get(sellerUUID);
        if (known == null && createIfNeeded)
        {
            known = new LocalUUIDInventory(sellerUUID, world);
            uuidMap.put(sellerUUID, known);
        }
        return known;
    }

    private static ItemForPurchase getLocalUUIDItem(File particularFolder, ItemStack item, World world, float price, short amount, String sellerUUID)
    {
        return new ItemForPurchase(particularFolder, item, world, price, amount, sellerUUID, false);
    }

    //*********************************

    public static void addToMarket(Player p, ItemStack item, short amount, float price)
    {
        if (canAmountBeSold(p, item, InventoryManager.getItemTally(p, item), amount))
        {
            if (price <= 0)
            {
                p.sendMessage(ChatColor.RED + "You must sell items for a positive price.");
            }
            else if (price > MAXIMUM_PRICE)
            {
                p.sendMessage(ChatColor.RED + "You can't sell things for more than " + (int)MAXIMUM_PRICE + " " + GOLD_COUNT_DATANAME + ".");
            }
            else
            {
                InventoryManager.removeItem(p, item, amount);

                String sellerUUID = UUIDFetcher.getUUID(p);
                World world = p.getWorld();
                File playerPointerFile = getPlayerPointerFile(sellerUUID, world, true);
                File particularFolder = getMaterialFolder(item.getType(), world, getRandomFolderName(p));

                //material file
                File itemFile = getStackFile(particularFolder, true);
                File dataFile = getExtraDataFile(particularFolder, true);

                if (itemFile.exists() && dataFile.exists() && playerPointerFile.exists() && FileManager.addDataToList(playerPointerFile, ECONOMY_FOLDER_NAME, getItemFolderPath(particularFolder, world)))
                {
                    try
                    {
                        //save the rebellion (itemstack)
                        FileOutputStream stream = new FileOutputStream(itemFile);
                        BukkitObjectOutputStream outputStream = new BukkitObjectOutputStream(stream);
                        outputStream.writeObject(item);
                        stream.close();
                        outputStream.close();

                        //save the dream (additional data)
                        FileManager.setData(dataFile, getRelaventDataMapForOutput(sellerUUID, world, amount, price));


                        //stuff I added later************************
                        Material mat = item.getType();

                        WorldGuiInventory globalUUIDInventory = getGlobalUUIDInventory(world, false);
                        if (globalUUIDInventory != null && !globalUUIDInventory.hasItem(getGlobalUUIDItemStack(sellerUUID)))
                        {
                            globalUUIDInventory.addItem(getGlobalUUIDItem(sellerUUID, world), true);
                        }

                        WorldGuiInventory globalMaterialInventory = getGlobalMaterialInventory(world, false);
                        if (globalMaterialInventory != null && !globalMaterialInventory.hasItem(getGlobalMaterialItemStack(mat)))
                        {
                            globalMaterialInventory.addItem(getGlobalMaterialItem(mat, world), true);
                        }

                        WorldGuiInventory localUUIDInventory = getLocalUUIDInventory(sellerUUID, world, false);
                        if (localUUIDInventory != null)
                        {
                            localUUIDInventory.addItem(getLocalUUIDItem(particularFolder, item, world, price, amount, sellerUUID), true);
                        }

                        WorldGuiInventory localMaterialInventory = getLocalMaterialInventory(mat, world, false);
                        if (localMaterialInventory != null)
                        {
                            localMaterialInventory.addItem(getLocalMaterialItem(particularFolder, item, world, price, amount, sellerUUID), true);
                        }

                        p.sendMessage(ChatColor.GREEN + "Added " + getPluralName(item, amount) + " to the market for " + (price < 1 ? "" + price : (int)((float)price)) + " " + GOLD_COUNT_DATANAME + "!");
                        return;
                        //*******************************************
                    }
                    catch (IOException ex)
                    {
                        ex.printStackTrace();
                    }
                }
                p.sendMessage(ChatColor.RED + "Failed to add your " + amount + " " + getPluralName(item, amount) + " to the market! Contact HappyMan!");
            }
        }
    }

    private static String getRandomFolderName(Player p)
    {
        return p.getName() + System.currentTimeMillis();
    }

    private static FileManager.EntrySet getRelaventDataMapForInput(File dataFile) //don't change the signature, please
    {
        return FileManager.getAllEntries(dataFile);
    }

    private static HashMap<String, Object> getRelaventDataMapForOutput(String uuid, String world, short amount, float price)
    {
        HashMap<String, Object> map = new HashMap<String, Object>();
        map.put(AMOUNT_DATANAME, "" + amount);
        map.put(PRICE_DATANAME, "" + price);
        map.put(UUID_DATANAME, uuid);
        map.put(WORLD_DATANAME, world);
        return map;
    }

    private static HashMap<String, Object> getRelaventDataMapForOutput(String uuid, World world, short amount, float price)
    {
        return getRelaventDataMapForOutput(uuid, world.getName(), amount, price);
    }

    public static boolean canAnyBeSold(Player p, ItemStack item)
    {
        String message = null;
        if (item == null || item.getType().equals(Material.AIR))
        {
            message = ChatColor.RED + "Please don't try to sell the air; we all need it to survive.";
        }
        else if (!getWorldType(p.getWorld()).isAllowedToSellItem(p, item))
        {
            message = ChatColor.RED + "You can't sell things right now!";
        }


        if (message != null)
        {
            p.sendMessage(message);
            return false;
        }
        return true;
    }

    public static boolean canAmountBeSold(Player p, ItemStack item, int tally, Short amount)
    {
        if (!canAnyBeSold(p, item))
        {
            return false;
        }

        String message = null;
        if (amount == null)
        {
            message = ChatColor.RED + "Invalid amount!";
        }
        else if (amount <= 0)
        {
            message = ChatColor.RED + "The set of natural numbers must contain the number of items that you are selling.";
        }
        else if (amount > MAXIMUM_ITEMS_AT_ONCE)
        {
            message = ChatColor.RED + "You can sell at most " + MAXIMUM_ITEMS_AT_ONCE + " items at a time.";
        }
        else
        {
            String pluralName = getPluralName(item, amount);
            if (tally < amount)
            {
                message = ChatColor.RED + "You only have " + tally + " " + pluralName +  "!";
            }
            else
            {
                File f = getPlayerPointerFile(p, false);
                short numberOfItemsOnThere = (short) FileManager.getDataList(f, ECONOMY_POINTER_DATANAME).size();
                if (f.exists() && numberOfItemsOnThere >= MAXIMUM_MARKET_ITEMS)
                {
                    short howManyOverLimit = (short)(numberOfItemsOnThere - MAXIMUM_MARKET_ITEMS);
                    switch (howManyOverLimit)
                    {
                        case 0: message = ChatColor.RED + "You have reached the maximum market items! Please take something down before trying to sell!";
                            break;
                        default:
                            message = ChatColor.RED + "You have exceeded the maximum market items! Please take " + (numberOfItemsOnThere - MAXIMUM_MARKET_ITEMS) + " products down before trying to sell!";
                            break;
                    }
                    getLocalUUIDInventory(UUIDFetcher.getUUID(p), p.getWorld(), true).open(p);
                }
            }
        }
        if (message != null)
        {
            p.sendMessage(message);
            return false;
        }
        return true;
    }

    //*************************

    private static File getPlayerPointerFile(Player p, boolean forceValid)
    {
        return getPlayerPointerFile(p.getName(), p.getWorld().getName(), forceValid);
    }

    private static File getPlayerPointerFile(String p, String world)
    {
        return getPlayerPointerFile(p, world, true);
    }

    private static File getPlayerPointerFile(String p, World world)
    {
        return getPlayerPointerFile(p, world.getName());
    }

    private static File getPlayerPointerFile(String p, String world, boolean forceValid)
    {
        return FileManager.getPlayerFile(p, world, ECONOMY_POINTER_DATANAME + "Items.json", forceValid);
    }

    private static File getPlayerPointerFile(String p, World world, boolean forceValid)
    {
        return getPlayerPointerFile(p, world.getName(), forceValid);
    }

    private static String getMaterialFolderName(Material mat)
    {
        return mat.name();
    }
    
    private static File getMaterialFolder(Material mat, World world, String relativePath)
    {
        return FileManager.getWorldFolder(world, ECONOMY_FOLDER_NAME + "/" + getMaterialFolderName(mat) + (relativePath.length() > 0 ? "/" + relativePath : ""));
    }

    private static File getMotherMaterialFolder(World world)
    {
        return FileManager.getWorldFolder(world, ECONOMY_FOLDER_NAME);
    }

    private static File getStackFile(File materialFolder, boolean forceValidity)
    {
        return FileManager.getSpecificFile(materialFolder, "stack.txt", forceValidity);
    }

    private static File getExtraDataFile(File materialFolder, boolean forceValidity)
    {
        return FileManager.getSpecificFile(materialFolder, "data.json", forceValidity);
    }

    private static String getItemFolderPath(File materialFolder, World world)
    {
        return getItemFolderPath(materialFolder, world.getName());
    }

    private static String getItemFolderPath(File materialFolder, String world)
    {
        String fullPath = materialFolder.getAbsolutePath();
        return fullPath.substring(FileManager.getWorldFolder(world).getAbsolutePath().length() + ECONOMY_FOLDER_NAME.length() + 2, fullPath.length()).replaceAll("\\\\", "/");
    }
    //
//    public static boolean itemsAreSame(ItemStack item1, ItemStack item2)
//    {
//        return itemsAreSame(item1, item2, false);
//    }
//
//    public static boolean itemsAreSame(ItemStack item1, ItemStack item2, boolean allowDurabilityDifferences)
//    {
//        return item1 != null && item2 != null && !item1.getType().equals(Material.AIR) &&
//                item1.getType() == item2.getType() &&
//                (allowDurabilityDifferences || item1.getDurability() == item2.getDurability()) &&
//                ((item1.getEnchantments() == null || item2.getEnchantments() == null) ? item1.getEnchantments() == item2.getEnchantments() :
//                                                                                       item1.getEnchantments().equals(item2.getEnchantments()));
//    }
}

