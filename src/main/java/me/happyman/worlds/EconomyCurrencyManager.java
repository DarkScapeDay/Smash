package me.happyman.worlds;

import me.happyman.utils.FileManager;
import me.happyman.utils.InventoryManager;
import me.happyman.utils.Verifier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;

import java.io.File;

import static me.happyman.Plugin.*;

public class EconomyCurrencyManager
{
    public static final String ADD_CMD = "fabricate";
    public static final String BAL_CMD = "bal";
    public static final String PAY_CMD = "pay";
    private static final Material EMERALD_MATERIAL = Material.EMERALD;
    private static final Material EMERALD_BLOCK_MATERIAL = Material.EMERALD_BLOCK;

    static
    {
        CommandExecutor currencyCommandExecuter = new CommandExecutor()
        {
            // killerEloIncrease = ELO_CHANGE_AT_0_ELO * ((float)Math.exp(-difference* ELO_CHANGE_RATE_SPEED)+ ELO_FARMABILITY_MOD)/(1+ ELO_FARMABILITY_MOD);
            @Override
            public boolean onCommand(final CommandSender sender, Command cmd, String label, String[] args)
            {
                if (matchesCommand(label, ADD_CMD))
                {
                    if (EMERALD_COUNT_DATANAME.toLowerCase().startsWith(label.toLowerCase()) || label.toLowerCase().endsWith(EMERALD_COUNT_DATANAME) || args.length > 2 && EMERALD_COUNT_DATANAME.toLowerCase().startsWith(args[2].toLowerCase()))
                    {
                        if (args.length < 2)
                        {
                            return false;
                        }
                        try
                        {
                            String whoToGiveTo = args[0];
                            final Player p = Bukkit.getPlayer(whoToGiveTo);

                            if (p == null)
                            {
//                                if (!getEmeraldFile(whoToGiveTo, false).exists())
//                                {
                                sender.sendMessage(ChatColor.RED + "Player " + whoToGiveTo + " not found.");
                                return true;
//                                }
                            }

                            short amount = Short.valueOf(args[1]);
                            short amountThatCanBeGiven = getEmeraldsThatCanBeGiven(p);
                            if (amount <= 0)
                            {
                                sender.sendMessage(ChatColor.RED + "Please give a positive number of " + EMERALD_COUNT_DATANAME + ".");
                            }
                            else if (amountThatCanBeGiven <= 0)
                            {
                                sender.sendMessage(ChatColor.YELLOW + "That player has no space.");
                            }
                            else if (amount > amountThatCanBeGiven)
                            {
                                if (sender instanceof Player)
                                {
                                    new Verifier.BooleanVerifier((Player)sender,
                                            ChatColor.YELLOW + "" + p.getName() + " only has space for " + amountThatCanBeGiven + " " + EMERALD_COUNT_DATANAME + ". Give all possible instead?")
                                    {
                                        @Override
                                        public void performYesAction()
                                        {
                                            addMaxEmeralds(p, true);
                                            sender.sendMessage(ChatColor.GREEN + "Gave emeralds to " + p.getName());
                                        }

                                        @Override
                                        public void performNoAction()
                                        {
                                            sender.sendMessage(ChatColor.GREEN + "Giving cancelled.");
                                        }
                                    };
                                }
                                else
                                {
                                    sender.sendMessage(ChatColor.RED + "That player has too many emeralds.");
                                }
                            }
                            else
                            {
                                if (addEmeralds(p, amount))
                                {
                                    sender.sendMessage(ChatColor.GREEN + "Gave " + amount + " " + EMERALD_COUNT_DATANAME + " to " + p.getName());
                                }
                                else
                                {
                                    sender.sendMessage(ChatColor.RED + "Failed to give " + EMERALD_COUNT_DATANAME + ".");
                                }
                            }
                        }
                        catch (NumberFormatException ex)
                        {
                            return false;
                        }
                        return true;
                    }
                    else
                    {
                        if (args.length < 1)
                        {
                            return false;
                        }
                        try
                        {
                            String whoToGiveTo = args[0];
                            Player p = Bukkit.getPlayer(whoToGiveTo);
                            if (p == null)
                            {
                                sender.sendMessage(ChatColor.RED + "Player " + whoToGiveTo + " not found.");
                            }
                            else
                            {
                                float amount = Float.valueOf(args[1]);
                                addGold(p, amount);
                                sender.sendMessage(ChatColor.GREEN + "Gave " + amount + " " + GOLD_COUNT_DATANAME + " to " + p.getName());
                            }
                        }
                        catch (NumberFormatException ex)
                        {
                            return false;
                        }
                        return true;
                    }
                }
                else if (matchesCommand(label, BAL_CMD))
                {
                    if (EMERALD_COUNT_DATANAME.toLowerCase().startsWith(label.toLowerCase()) || label.toLowerCase().endsWith(EMERALD_COUNT_DATANAME) || args.length > 1 && EMERALD_COUNT_DATANAME.toLowerCase().startsWith(args[1].toLowerCase()))
                    {
                        final Player p;
                        final String who;
                        if (args.length < 1)
                        {
                            if (!(sender instanceof Player))
                            {
                                return false;
                            }
                            p = (Player)sender;
                            who = p.getName();
                        }
                        else
                        {
                            String firstArg = args[0];
                            p = Bukkit.getPlayer(firstArg);
                            if (p == null)
                            {
                                who = firstArg;
//                                if (!getEmeraldFile(who, false).exists())
//                                {
                                sender.sendMessage(ChatColor.RED + "Player " + who + " not found.");
                                return true;
//                                }
                            }
                            else
                            {
                                who = p.getName();
                            }
                        }

                        String youHim = sender == p ? "You have" : who + " has";
                        short emeralds = getEmeralds(p);
                        String count = emeralds == 0 ? "no" : ("" + emeralds);
                        sender.sendMessage(ChatColor.GREEN + youHim + " " + count + " " + EMERALD_COUNT_DATANAME);
                        return true;
                    }
                    else
                    {
                        final Player p;
                        final String who;
                        final World w;
                        if (args.length < 1)
                        {
                            if (!(sender instanceof Player))
                            {
                                return false;
                            }
                            p = (Player)sender;
                            who = p.getName();
                            w = p.getWorld();
                        }
                        else
                        {
                            String firstArg = args[0];
                            p = Bukkit.getPlayer(firstArg);
                            if (p == null)
                            {
                                if (!(sender instanceof Player))
                                {
                                    sender.sendMessage(ChatColor.RED + "You aren't in game.");
                                    return false;
                                }
                                who = firstArg;
                                w = ((Player)sender).getWorld();
                                if (!getGoldFile(who, w).exists())
                                {
                                    sender.sendMessage(ChatColor.RED + "Player " + who + " not found.");
                                    return true;
                                }
                            }
                            else
                            {
                                who = p.getName();
                                w = p.getWorld();
                            }
                        }

                        String youHim = sender == p ? "You have" : who + " has";
                        int gold = (int)getGold(who, w);
                        String count = gold == 0 ? "no" : "" + gold;
                        sender.sendMessage(ChatColor.GREEN + youHim + " " + count + " " + GOLD_COUNT_DATANAME);
                        return true;
                    }
                }
                else if (matchesCommand(label, PAY_CMD))
                {
                    if (EMERALD_COUNT_DATANAME.toLowerCase().startsWith(label.toLowerCase()) || label.toLowerCase().endsWith(EMERALD_COUNT_DATANAME) || args.length > 2 && EMERALD_COUNT_DATANAME.toLowerCase().startsWith(args[2].toLowerCase()))
                    {
                        if (args.length < 2 || !(sender instanceof Player))
                        {
                            return false;
                        }
                        Player giver = (Player)sender;
                        String receiverName = args[0];
                        Player receiver = Bukkit.getPlayer(receiverName);
                        if (receiver == null)
                        {
                            if (FileManager.getGeneralPlayerFile(receiverName).exists())
                            {
                                giver.sendMessage(ChatColor.RED + receiverName + " is not online. Therefore, you can't give him emeralds.");
                            }
                            else
                            {
                                giver.sendMessage(ChatColor.RED + "The server is not aware of any player by the colorlessName of \"" + receiverName + "\"...");
                            }
                        }
                        else
                        {
                            try
                            {
                                short amount = Short.valueOf(args[1]);
                                if (getEmeralds(giver) < amount)
                                {
                                    giver.sendMessage(ChatColor.RED + "You don't have " + (int)amount + " " + EMERALD_COUNT_DATANAME + " to give!");
                                }
                                else
                                {
                                    transferEmeralds(giver, receiver, amount, true);
                                }
                            }
                            catch (NumberFormatException ex)
                            {
                                return false;
                            }
                        }
                        return true;
                    }
                    else
                    {
                        if (args.length < 2 || !(sender instanceof Player))
                        {
                            return false;
                        }
                        Player giver = (Player)sender;
                        String receiverName = args[0];
                        Player receiver = Bukkit.getPlayer(receiverName);
                        if (receiver == null)
                        {
                            if (FileManager.getGeneralPlayerFile(receiverName).exists())
                            {
                                giver.sendMessage(ChatColor.RED + receiverName + " is not online. Therefore, you can't give him emeralds.");
                            }
                            else
                            {
                                giver.sendMessage(ChatColor.RED + "The server is not aware of any player by the colorlessName of \"" + receiverName + "\"...");
                            }
                        }
                        else
                        {
                            try
                            {
                                float amount = Float.valueOf(args[1]);
                                transferGold(giver, receiverName, amount);
                            }
                            catch (NumberFormatException ex)
                            {
                                return false;
                            }
                        }
                        return true;
                    }
                }
                return false;
            }
        };


        setExecutor(ADD_CMD, currencyCommandExecuter);
        setExecutor(BAL_CMD, currencyCommandExecuter);
        setExecutor(PAY_CMD, currencyCommandExecuter);
    }

    public static final String GOLD_COUNT_DATANAME = "gold";
    protected static final String EMERALD_COUNT_DATANAME = "emeralds";

    enum CurrencyType
    {
        GOLD, EMERALDS;
    }

//    private static File getEmeraldFile(String p, boolean forceValidity)
//    {
//        return getGeneralPlayerFile(p, forceValidity);//getSpecificFile(DirectoryType.PLAYER_DATA, "", getGeneralPlayerFile(p), forceValidity);
//    }
//
//    private static File getEmeraldFile(Player p)
//    {
//        return getEmeraldFile(p.getName(), true);
//    }
//
//    protected static int getEmeralds(String p)
//    {
//        return getGeneralPlayerFloatDatum(getEmeraldFile(p, true), EMERALD_COUNT_DATANAME);
//    }

    protected static short getEmeralds(String p)
    {
        Player player = Bukkit.getPlayer(UUIDFetcher.getCapitalName(p));
        if (player == null)
        {
            return 0;
        }
        return getEmeralds(player);
    }

    protected static short getEmeraldsThatCanBeGiven(Player p)
    {
        short currentEmeralds = getEmeralds(p);
        short slots = 0;
        short maxStack = (short)(EMERALD_BLOCK_MATERIAL.getMaxStackSize()*9);
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < 36; i++)
        {
            ItemStack item = contents[i];
            if (item == null || item.getType().equals(Material.AIR) || item.getType().equals(EMERALD_MATERIAL) || item.getType().equals(EMERALD_BLOCK_MATERIAL))
            {
                slots++;
            }
        }
        short emeraldsThatCanBeStored = (short)((slots - 1)*9*EMERALD_BLOCK_MATERIAL.getMaxStackSize() + EMERALD_MATERIAL.getMaxStackSize());
        emeraldsThatCanBeStored = emeraldsThatCanBeStored < 0 ? 0 : emeraldsThatCanBeStored;
        return (short)(emeraldsThatCanBeStored - currentEmeralds);
    }
//
//    protected static boolean canGiveEmeralds(Player player, short amount)
//    {
//        return getEmeraldsThatCanBeGiven(player) >= amount;
//    }

    protected static short getEmeralds(Player p)
    {
        return (short)(InventoryManager.getItemTally(p, EMERALD_MATERIAL) + 9*InventoryManager.getItemTally(p, EMERALD_BLOCK_MATERIAL));
    }

//    protected static void setEmeralds(String p, float amount)
//    {
//        putGeneralPlayerDatum(getEmeraldFile(p, true), EMERALD_COUNT_DATANAME, amount);
//    }
//
//    protected static void setEmeralds(Player p, float amount)
//    {
//        setEmeralds(p.getName(), amount);
//    }
//
//    protected static void addEmeralds(String p, float amount)
//    {
//        setEmeralds(p, getEmeralds(p) + amount);
//    }

    protected static boolean addEmeralds(Player p, short amount)
    {
        return addEmeralds(p, amount, true);
    }

    protected static boolean addEmeralds(Player p, short amount, boolean tellThePlayer)
    {
        short currentAmount = getEmeralds(p);
        InventoryManager.removeAllFromPlayer(p, EMERALD_BLOCK_MATERIAL, false);
        InventoryManager.removeAllFromPlayer(p, EMERALD_MATERIAL, false);

        short newAmount = (short)(currentAmount + amount);
        short newEmeralds = (short)((newAmount > 64 ? 54 : 0) + newAmount%9);
        if ((newAmount - newEmeralds)%9 != 0)
        {
            sendErrorMessage("Error! Math error when adding emeralds");
        }
        short newBlocks = (short)((newAmount - newEmeralds)/9);
        boolean okay2 = InventoryManager.giveItem(p, EMERALD_MATERIAL, newEmeralds, true, false);
        boolean okay1 = InventoryManager.giveItem(p, EMERALD_BLOCK_MATERIAL, newBlocks, true, false);
        boolean okay = okay1 && okay2;
        if (tellThePlayer)
        {
            if (okay)
            {
                p.sendMessage(ChatColor.GREEN + "" + amount + " " + EMERALD_COUNT_DATANAME + " received.");
            }
            else
            {
                p.sendMessage(ChatColor.YELLOW + "Some " + EMERALD_COUNT_DATANAME + " received.");
            }
        }
        return okay;
    }

    protected static void addMaxEmeralds(Player p, boolean tellThePlayer)
    {
        Short currentEmeralds = null;
        if (tellThePlayer)
        {
            currentEmeralds = getEmeralds(p);
        }
        InventoryManager.removeAllFromPlayer(p, EMERALD_MATERIAL, false);
        InventoryManager.giveMaxItem(p, EMERALD_BLOCK_MATERIAL, true, false);
        if (tellThePlayer)
        {
            short gain = (short)(getEmeralds(p) - currentEmeralds);
            p.sendMessage(ChatColor.GREEN + "" + gain + " " + EMERALD_COUNT_DATANAME + " received.");
        }
    }

//    protected static void removeEmeralds(String p, float amount)
//    {
//        setEmeralds(p, getEmeralds(p) - amount);
//    }

    protected static void removeEmeralds(Player p, short amount)
    {
        removeEmeralds(p, amount, true);
    }

    protected static void removeEmeralds(Player p, short amount, boolean tellThePlayer)
    {
        short currentAmount = getEmeralds(p);
        short newAmount = (short)(currentAmount - amount);
        if (newAmount < 0)
        {
            sendErrorMessage("Error! Tried to remove too many emeralds (" + amount + ") from " + p.getName());
            newAmount = 0;
        }
        InventoryManager.removeAllFromPlayer(p, EMERALD_BLOCK_MATERIAL, false);
        InventoryManager.removeAllFromPlayer(p, EMERALD_MATERIAL, false);
        short newBlockCount = (short)(newAmount/9);
        short newEmeraldCount = (short)(newAmount%9);
        InventoryManager.giveItem(p, EMERALD_BLOCK_MATERIAL, newBlockCount, true, false);
        InventoryManager.giveItem(p, EMERALD_MATERIAL, newEmeraldCount, true, false);
        if (tellThePlayer)
        {
            p.sendMessage(ChatColor.GREEN + "" + amount + " " + EMERALD_COUNT_DATANAME + " removed.");
        }
    }

    protected static void transferEmeralds(Player giver, Player receiver, short amount)
    {
        transferEmeralds(giver, receiver, amount, true);
    }

    protected static boolean transferEmeralds(final Player giver, final Player receiver, final short amount, final boolean tellThePlayers)
    {
        if (giver.equals(receiver))
        {
            giver.sendMessage(ChatColor.GREEN + "You gave yourself " + amount + " " + EMERALD_COUNT_DATANAME + ".");
            return true;
        }

        if (amount < 0)
        {
            if (tellThePlayers)
            {
                giver.sendMessage(ChatColor.RED + "You must pay a positive amount of " + GOLD_COUNT_DATANAME);
            }
            return false;
        }
        else if (getEmeralds(giver) < amount)
        {
            if (tellThePlayers)
            {
                giver.sendMessage(ChatColor.RED + "You don't have " + (int)amount + " " + EMERALD_COUNT_DATANAME + " to give!");
            }
            return false;
        }

        final short amountThatCanBeGiven = getEmeraldsThatCanBeGiven(receiver);
        if (amountThatCanBeGiven < amount)
        {
            if (tellThePlayers)
            {
                new Verifier.BooleanVerifier(giver,
                        ChatColor.YELLOW + "That player only has space for " + amountThatCanBeGiven + " " + EMERALD_COUNT_DATANAME + ". Give all you can instead?")
                {
                    @Override
                    public void performYesAction()
                    {
                        short emeraldsGiverHas = getEmeralds(giver);
                        short emeraldsThatCanBeGiven = getEmeraldsThatCanBeGiven(receiver);
                        short amountToGive = emeraldsGiverHas < emeraldsThatCanBeGiven ? emeraldsGiverHas : emeraldsThatCanBeGiven;
                        transferEmeralds(giver, receiver, amountToGive, tellThePlayers);
                    }

                    @Override
                    public void performNoAction()
                    {
                        giver.sendMessage(ChatColor.YELLOW + "Giving cancelled.");
                    }
                };
            }
        }
        else
        {
            if (tellThePlayers)
            {
                giver.sendMessage(ChatColor.DARK_GREEN + "You gave " + amount + " " + EMERALD_COUNT_DATANAME + " to " + receiver.getName() + "!");
                receiver.sendMessage(ChatColor.DARK_GREEN + giver.getName() + " gave you " + amount + " " + EMERALD_COUNT_DATANAME + "!");
            }
            removeEmeralds(giver, amount, false);
            addEmeralds(receiver, amount, false);
            return true;
        }
        return false;
    }

    //*********************

    private static File getGoldFile(String p, World world)
    {
        return FileManager.getSimplePlayerDataFile(p, world);
    }

    protected static float getGold(String p, World world)
    {
        return FileManager.getFloatData(getGoldFile(p, world), GOLD_COUNT_DATANAME);
    }

    public static float getGold(Player p)
    {
        return getGold(p.getName(), p.getWorld());
    }

    protected static void setGold(String p, World world, float amount)
    {
        FileManager.putData(getGoldFile(p, world), GOLD_COUNT_DATANAME, amount);
    }

    protected static void setGold(Player p, float amount)
    {
        setGold(p.getName(), p.getWorld(), amount);
    }

    protected static void addGold(String p, World world, float amount)
    {
        setGold(p, world,getGold(p, world) + amount);
    }

    protected static void addGold(Player p, float amount)
    {
        addGold(p.getName(), p.getWorld(), amount);
    }

    protected static void removeGold(Player p, float amount)
    {
        removeGold(p, p.getWorld(), amount);
    }

    protected static void removeGold(Player p, World world, float amount)
    {
        removeGold(p.getName(), world, amount);
    }

    protected static void removeGold(String p, World world, float amount)
    {
        setGold(p, world, getGold(p, world) - amount);
    }

    public static boolean transferGold(Player giver, String receiver, float amount)
    {
        String receiverName = UUIDFetcher.getCapitalName(receiver);
        if (giver.getName().equals(receiverName))
        {
            return true;
        }

        World world = giver.getWorld();
        if (amount < 0)
        {
            giver.sendMessage(ChatColor.RED + "You must pay a positive amount of " + GOLD_COUNT_DATANAME);
        }
        else if (getGold(giver) < amount)
        {
            giver.sendMessage(ChatColor.RED + "You don't have " + (int)amount + " " + GOLD_COUNT_DATANAME + " to give!");
        }
        else
        {
            giver.sendMessage(ChatColor.GREEN + "Gave " + amount + " " + GOLD_COUNT_DATANAME + " to " + receiverName);
            Player receiverOnline = Bukkit.getPlayer(receiverName);
            if (receiverOnline != null)
            {
                receiverOnline.sendMessage(ChatColor.GREEN + "Received " + amount + " " + GOLD_COUNT_DATANAME + " from " + giver.getName());
            }
            addGold(receiver, world, amount);
            removeGold(giver, world, amount);
            return true;
        }
        return false;
    }

    protected static void startWorld(World world)
    {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getNewScoreboard();
        Objective side = scoreboard.registerNewObjective("side", "none");
        side.setDisplayName(WorldManager.getDisplayName(world));

        for (Player p : world.getPlayers())
        {
            p.setScoreboard(scoreboard);
        }
    }
}
