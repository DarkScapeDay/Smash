package me.happyman.SmashKitMgt;

import me.happyman.ItemTypes.SmashItem;
import me.happyman.ItemTypes.SmashItemWithCharge;
import me.happyman.ItemTypes.SmashItemWithUsages;
import me.happyman.SmashItems.CompassItem;
import me.happyman.SmashItems.ShieldItem;
import me.happyman.SmashKits.*;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashKitListener;
import me.happyman.source;
import me.happyman.utils.DirectoryType;
import me.happyman.worlds.SmashWorldInteractor;
import me.happyman.worlds.SmashWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.*;

public class SmashKitManager implements CommandExecutor
{
    private static source plugin;
    private final SmashManager smashManager;

    private static final int HIGHEST_KIT_ROTATION_LEVEL = 0;
    private static final String ROTATION_LEVEL_DATANAME = "Kit rotation level";
    public static final String KIT_CMD = "kit";
    private static final String KIT_LIST_CMD = "kitlist";
    private static final String OWNED_KITS_DATANAME = "Smash Kits";
    private static final String SELECED_KIT_DATANAME = "Selected kit";
    private static HashMap<Player, SmashKit> selectedKits;
    private static ArrayList<Player> openKitGuis;
    private static List<String> kitNames;
    private static HashMap<ItemStack, String> kitRepresenters;
    private static HashMap<String, SmashKit> nameToKitMap;
    private static List<String> alternateNoneKitNames;
    private static List<Player> kirbies;

    private static final ShieldItem shield = new ShieldItem();
    private static final CompassItem compass = new CompassItem();

    private static SmashKit DEFAULT_KIT;

    public SmashKitManager(SmashManager smashManager, source plugIn)
    {
        plugin = plugIn;
        this.smashManager = smashManager;
        plugin.setExecutor(KIT_CMD, this);
        plugin.setExecutor(KIT_LIST_CMD, this);
        new SmashKitListener();

        openKitGuis = new ArrayList<Player>();
        selectedKits = new HashMap<Player, SmashKit>();
        kitRepresenters = new HashMap<ItemStack, String>();
        nameToKitMap = new HashMap<String, SmashKit>();
        alternateNoneKitNames = new ArrayList<String>();
        kirbies = new ArrayList<Player>();

        DEFAULT_KIT = new Mario();
        alternateNoneKitNames.add("None");
        alternateNoneKitNames.add("Default");
        alternateNoneKitNames.add("Normal");
        alternateNoneKitNames.add("default");
        for (String name : alternateNoneKitNames)
        {
            nameToKitMap.put(name, DEFAULT_KIT);
        }
        new Bowser();
        new Pit();
        new Kirby();
        new Pikachu();
        new Wario();
        new Ganondorf();
        new Fox();
        new Sonic();

        kitNames = new ArrayList<String>(kitRepresenters.values());
        for (int i = 1; i < kitNames.size(); i++)
        {
            if (kitNames.get(i).compareTo(kitNames.get(i-1)) < 0)
            {
                for (int j = 0; j < i; j++)
                {
                    if (kitNames.get(i).compareTo(kitNames.get(j)) < 0)
                    {
                        kitNames.add(j, kitNames.get(i));
                        kitNames.remove(i+1);
                        break;
                    }
                }
            }
        }
    }

    public static ShieldItem getShield()
    {
        return shield;
    }

    public static CompassItem getCompass()
    {
        return compass;
    }

    public static void logSmashKit(SmashKit kit)
    {
        nameToKitMap.put(kit.getName(), kit);
        kitRepresenters.put(kit.getKitRepresenter(), kit.getName());
    }

    public static int getRotationLevel(Player p)
    {
        return plugin.getStatistic(p, ROTATION_LEVEL_DATANAME);
    }

    public static void setRotationLevel(Player p, int level)
    {
        plugin.putDatum(p, ROTATION_LEVEL_DATANAME, level);
    }

    public static void givePlayerKit(Player p, String kitName)
    {
        if (isSmashKit(kitName) && !plugin.getData(p, OWNED_KITS_DATANAME).contains(plugin.capitalize(kitName)))
        {
            plugin.addDatumEntry(p, OWNED_KITS_DATANAME, plugin.capitalize(kitName));
        }
    }

    public static source getPlugin()
    {
        return plugin;
    }

    public static SmashKit getSelectedKit(Player p)
    {
        if (!selectedKits.containsKey(p))
        {
            SmashKit currentKit = getKit(plugin.getDatum(p, SELECED_KIT_DATANAME));
            if (currentKit != null && canUseKit(p, currentKit.getName()))
            {
                setSelectedKit(p, currentKit, true);
            }
            else
            {
                setSelectedKit(p, DEFAULT_KIT, true);
            }
        }
        return selectedKits.get(p);
    }

    public static SmashKit getCurrentKit(Player p)
    {
        SmashKit selectedKit = getSelectedKit(p);
        if (selectedKit instanceof Kirby && ((Kirby)selectedKit).getInhaler().hasMaskKit(p))
        {
            selectedKit = ((Kirby)selectedKit).getInhaler().getMaskKit(p);
        }
        return selectedKit;
    }

    public static String getSelectedKitName(Player p)
    {
        String prefix = "";
        if (!getSelectedKit(p).equals(getCurrentKit(p)))
        {
            prefix = getSelectedKit(p).getName() + "-";
        }
        return prefix + getCurrentKit(p).getName();
    }

    private static void setSelectedKit(Player p, SmashKit kit, boolean loading)
    {
        if (!loading)
        {
            plugin.putDatum(p, SELECED_KIT_DATANAME, kit.getName());
            if (SmashWorldManager.isSmashWorld(p.getWorld()))
            {
                kit.applyKitInventory(p);
                if (!SmashWorldManager.gameHasStarted(p.getWorld()))
                {
                    SmashWorldInteractor.sendMessageToWorld(p.getWorld(), "<" + p.getDisplayName() + ChatColor.RESET + "> " + kit.getDescription());
                }
            }
            else
            {
                p.sendMessage(ChatColor.GREEN + "You have selected kit " + kit.getName() + ".");
            }
        }
        if (hasKitGuiOpen(p))
        {
            p.closeInventory();
        }
        selectedKits.put(p, kit);
        /*if (kit instanceof Kirby && !kirbies.contains(p))
        {
            kirbies.add(p);
        }
        else if (!(kit instanceof Kirby) && kirbies.contains(p))
        {
            kirbies.remove(p);
        }*/
    }

    /*public static boolean isKirbyTheif(Player p)
    {
        return kirbies.contains(p) && !getSelectedKit(p).equals(getCurrentKit(p));
    }*/

    public static void unloadSelectedKit(Player p)
    {
        if (selectedKits.containsKey(p))
        {
            plugin.putDatum(p, SELECED_KIT_DATANAME, selectedKits.get(p).getName());
            selectedKits.remove(p);
        }
    }

    public static List<String> getPersonallyOwnedKits(Player p)
    {
        List<String> ownedList = plugin.getData(p, OWNED_KITS_DATANAME);
        return ownedList;
    }

    public List<ItemStack> getKitRepresenters(Player p)
    {
        List<ItemStack> kitRepresenters = new ArrayList<ItemStack>();
        List<String> ownedKits = getPersonallyOwnedKits(p);
        List<String> otherwiseAvaliableKits = getRotationKits(p);
        for (String kit : kitNames)
        {
            if (ownedKits.contains(kit) || otherwiseAvaliableKits.contains(kit))
            {
                kitRepresenters.add(nameToKitMap.get(kit).getKitRepresenter());
            }
        }
        return kitRepresenters;
    }

    public static SmashKit getKit(String name)
    {
        if (isSmashKit(name))
        {
            return nameToKitMap.get(name);
        }
        return null;
    }

    public static boolean isSmashKit(String name)
    {
        return nameToKitMap.containsKey(name);
    }

    public static void deselectKit(Player p)
    {
        if (selectedKits.containsKey(p))
        {
            selectedKits.remove(p);
        }
    }

    public static boolean canChangeKit(Player p)
    {
        return !SmashWorldManager.isSmashWorld(p.getWorld()) || !SmashWorldManager.gameHasStarted(p.getWorld()) || SmashWorldManager.gameHasEnded(p.getWorld()) || SmashWorldInteractor.isInSpectatorMode(p);
    }

    public static boolean isUsingFireImmuneKit(Player p)
    {
        return getCurrentKit(p).isImmuneToFire() && SmashWorldManager.isSmashWorld(p.getWorld());
    }

    public static SmashItem getKitItem(Player p, ItemStack item)
    {
        if (getCurrentKit(p).hasItem(item))
        {
            return getCurrentKit(p).getItem(item);
        }
        else if (getSelectedKit(p).hasItem(item))
        {
            return getSelectedKit(p).getItem(item);
        }
        plugin.sendErrorMessage("Error! Tried to get a SmashItem that wasn't for a person who had a kit!");
        return null;
    }

    /*
    public static SmashItem getKitItem(Player p, ItemStack item)
    {
        if (hasKitSelected(p))
        {
            for (int i = 0; i < 9; i++)
            {
                if (getSelectedKit(p).getContents()[i] != null)
                {
                    SmashItem kitItem = SmashKitManager.getSelectedKit(p).getItem(getSelectedKit(p).getContents()[i]);
                    if (kitItem.getItem().equals(item))
                    {
                        return kitItem;
                    }
                }
            }
        }
        plugin.sendErrorMessage("Error! Tried to get a SmashItem that wasn't for a person who had a kit!");
        return null;
    }*/

    public static SmashItem getHeldKitItem(Player p)
    {
        if (SmashKitManager.getCurrentKit(p).hasItem(p.getItemInHand()))
        {
            return getCurrentKit(p).getItem(p.getItemInHand());
        }
        else if (SmashKitManager.getSelectedKit(p).hasItem(p.getItemInHand()))
        {
            return getSelectedKit(p).getItem(p.getItemInHand());
        }
        return null;
    }

    /*public static boolean isItemWithTask(SmashItem item)
    {
        return item instanceof SmashItemWithTask;
    }

    public static boolean isItemWithCharge(SmashItem item)
    {
        return item instanceof SmashItemWithCharge;
    }

    public static boolean isItemWithUsages(SmashItem item)
    {
        return item instanceof SmashItemWithUsages;
    }*/

    public static void setKit(Player p, String kitName)
    {
        if (isSmashKit(kitName))
        {
            SmashKit kit = getKit(kitName);
            if (!canChangeKit(p))
            {
                p.sendMessage(ChatColor.RED + "You cannot choose a kit whilst the game is in progress!");
            }
            else if (!canUseKit(p, kitName))
            {
                p.sendMessage(ChatColor.RED + "You don't own " + kitName + ". Do /kitlist to see which kits you can use.");
            }
            else
            {
                setSelectedKit(p, kit, false);
            }
        }
        else
        {
            p.sendMessage(ChatColor.RED + "That is not a Smash kit!");
        }
    }

    public void openKitGui(Player p)
    {
        List<ItemStack> kitRepresenters = getKitRepresenters(p);
        int size = kitRepresenters.size();
        int rows = size/9 + 1;
        if (size % 9 == 0)
        {
            rows -= 1;
        }
        if (!openKitGuis.contains(p))
        {
            openKitGuis.add(p);
        }
        else
        {
            plugin.sendErrorMessage("Error! Someone who already had their kit inventory open opened it again somehow!");
        }
        Inventory kitInv = Bukkit.createInventory(p, rows*9, ChatColor.BLUE + "\u258E Select your Smash Kit!");
        for (int i = 0; i < kitRepresenters.size(); i++)
        {
            kitInv.setItem(i, kitRepresenters.get(i));
        }
        p.openInventory(kitInv);
    }

    public static boolean hasKitGuiOpen(Player p)
    {
        return openKitGuis.contains(p);
    }

    public static void forgetOpenKitGui(Player p)
    {
        if (hasKitGuiOpen(p))
        {
            openKitGuis.remove(p);
        }
    }

    public static List<String> getKits()
    {
        return kitNames;
    }

    public static boolean canUseKit(Player p, String capitalizedKitName)
    {
        return getPersonallyOwnedKits(p).contains(capitalizedKitName) || getRotationKits(p).contains(capitalizedKitName) || alternateNoneKitNames.contains(capitalizedKitName);
    }

    public static String getKitRepresented(ItemStack representer)
    {
        if (isKitRepresenter(representer))
        {
            return kitRepresenters.get(representer);
        }
        plugin.sendErrorMessage("Error! Tried to get the kit represented by a non-kit-selection item!");
        return null;
    }

    public static boolean isKitRepresenter(ItemStack representer)
    {
        return kitRepresenters.containsKey(representer);
    }

    public static List<String> getRotationKits(int rotationLevel)
    {
        List<String> rotationKits = new ArrayList<String>();
        for (int i = 0; i <= rotationLevel; i++)
        {
            for (String s : plugin.getData(DirectoryType.SERVER_DATA, "", "Rotation Kits", "Rotation level " + rotationLevel))
            {
                rotationKits.add(s);
            }
        }
        return rotationKits;
    }

    public String getAvaliableKitString(Player p)
    {
        String kitList = "";
        List<String> avaliableKitList = getAvaliableKitList(p);
        for (int i = 0; i < avaliableKitList.size(); i++)
        {
            if (i < avaliableKitList.size() - 1)
            {
                kitList += avaliableKitList.get(i) + " ";
            }
            else
            {
                kitList += avaliableKitList.get(i);
            }
        }
        return kitList;
    }

    public List<String> getAvaliableKitList(Player p)
    {
        List<String> avaliableKits = new ArrayList<String>();
        int rotationLevel = getRotationLevel(p);
        List<String> freeKits = null;
        List<String> vipKits = null;
        List<String> mvpKits = null;
        List<String> proKits = null;
        if (rotationLevel >= 0)
        {
            freeKits = getRotationKits(0);
            if (rotationLevel >= 1)
            {
                vipKits = getRotationKits(1);
                if (rotationLevel >= 2)
                {
                    mvpKits = getRotationKits(2);
                    if (rotationLevel >= HIGHEST_KIT_ROTATION_LEVEL)
                    {
                        proKits = getRotationKits(3);
                    }
                }
            }
        }
        List<String> ownedKits = getPersonallyOwnedKits(p);
        for (String kit : kitNames)
        {
            String color = null;
            if (ownedKits.contains(kit))
            {
                color = "" + ChatColor.GREEN;
            }
            else if (freeKits != null && freeKits.contains(kit))
            {
                color = "" + ChatColor.AQUA;
            }
            else if (vipKits != null && vipKits.contains(kit))
            {
                color = ChatColor.GREEN + "" + ChatColor.ITALIC;
            }
            else if (mvpKits != null && mvpKits.contains(kit))
            {
                color = ChatColor.BLUE + "" + ChatColor.ITALIC;
            }
            else if (proKits != null && proKits.contains(kit))
            {
                color = ChatColor.GOLD + "" + ChatColor.ITALIC;
            }
            else
            {
                color = "" + ChatColor.RED;
            }
            avaliableKits.add(color + kit);
        }
        return avaliableKits;
    }

    private static List<String> getRotationKits(Player p)
    {
        return getRotationKits(getRotationLevel(p));
    }


    public static void restoreAllUsagesAndCharges(Player p, boolean justTheRockets)
    {
        if (SmashWorldManager.canRefuelNow(p))
        {
            SmashKit currentKit = getCurrentKit(p);
            SmashKit selectedKit = getSelectedKit(p);
            for (SmashItem item : getCurrentKit(p).getItems())
            {
                restoreUsagesCharges(p, item, justTheRockets);
            }
            if (!selectedKit.equals(currentKit))
            {
                for (SmashItem item : getSelectedKit(p).getItems())
                {
                    restoreUsagesCharges(p, item, justTheRockets);
                }
            }
            if (justTheRockets)
            {
                SmashWorldManager.resetTimeTilRefuel(p);
            }
        }
    }

    public static void restoreUsagesCharges(Player p, SmashItem item, boolean justTheRockets)
    {
        if (item instanceof SmashItemWithCharge && !justTheRockets)
        {
            ((SmashItemWithCharge)item).resetCharge(p);
        }
        else if (item instanceof SmashItemWithUsages && (!justTheRockets || ((SmashItemWithUsages) item).rechargesOnLand()))
        {
            ((SmashItemWithUsages)item).restoreUsages(p);
        }
    }

    public static boolean kitHasItem(Player p, ItemStack item)
    {
        return getCurrentKit(p).hasItem(item) || !getSelectedKit(p).equals(getCurrentKit(p)) && getSelectedKit(p).hasItem(item);
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (plugin.matchesCommand(label, KIT_CMD))
        {
            if (!(sender instanceof Player) || args.length > 0 && (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("help")))
            {
                if (!(sender instanceof Player))
                {
                    sender.sendMessage(ChatColor.GREEN + "" + ChatColor.UNDERLINE + "Here are all the Smash kits: ");
                    for (String kit : kitNames)
                    {
                        sender.sendMessage(ChatColor.GOLD + kit);
                    }
                }
                else
                {
                    sender.sendMessage(ChatColor.GREEN + "Smash Kits:");
                    sender.sendMessage(getAvaliableKitString((Player)sender));
                }
                return true;
            }
            else if (args.length == 0)
            {
                openKitGui((Player)sender);
            }
            else
            {
                args[0] = plugin.capitalize(args[0]);
                setKit((Player)sender, plugin.capitalize( args[0]));
            }
            return true;
        }
        else if (plugin.matchesCommand(label, KIT_LIST_CMD))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage(ChatColor.GREEN + "" + ChatColor.UNDERLINE + "Here are all the Smash kits: ");
                for (String kit : kitNames)
                {
                    sender.sendMessage(ChatColor.GOLD + kit);
                }
            }
            else
            {
                sender.sendMessage(ChatColor.GREEN + "" +  ChatColor.BOLD + "List of Smash Kits:");
                sender.sendMessage(getAvaliableKitString((Player)sender));
            }
            return true;
        }
        return false;
    }

    public static boolean hasFinalSmashActive(Player p)
    {
        return getSelectedKit(p).getProperties().getFinalSmash().hasFinalSmashActive(p);
    }
}
