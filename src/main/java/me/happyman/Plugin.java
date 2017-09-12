package me.happyman;

import me.happyman.Other.*;
import me.happyman.SpecialItems.MetaItems;
import me.happyman.SpecialItems.UsefulItemStack;
import me.happyman.utils.*;
import me.happyman.utils.Music.Song;
import me.happyman.worlds.PlayerStateRecorder;
import me.happyman.worlds.UUIDFetcher;
import me.happyman.worlds.WorldManager;
import me.happyman.worlds.WorldType;
import net.minecraft.server.v1_12_R1.EnumParticle;
import net.minecraft.server.v1_12_R1.PacketPlayOutWorldParticles;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import protocolsupport.api.ProtocolSupportAPI;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import static me.happyman.utils.FileManager.getRoot;
import static me.happyman.worlds.WorldType.forceTp;
import static me.happyman.worlds.WorldType.sendMessageToWorld;

public class Plugin extends JavaPlugin
{

    private static String getAuthor()
    {
        return "HappyMan"; //EngineeringEntity@gmail.com
    }

    private static final String MSG_COMMAND = "msg";
    private static final String CHAT_COMMAND = "chat";
    private static final String REPLY_COMMAND = "r";
    private static final String JUMP_COMMAND = "jump";
    private static final String PLAY_PARTICLE_CMD = "playparticle";
    private static final String KILL_ENTITIES_COMMAND = "killentities";
    private static final HashMap<CommandSender, CommandSender> whoToReplyTo = new HashMap<CommandSender, CommandSender>();
    private static String FALLBACK_WORLD_NAME = null;
    private static World FALLBACK_WORLD = null; //The world which is used for emergencies
    private static Location FALLBACK_LOCATION;
    private static Plugin instance;
    private static boolean initialized = false;
    private static final int SHUTDOWN_DELAY = 3;
    private static boolean disabled = false;

    public static Plugin getPlugin()
    {
        return instance;
    }

    private static void initialize()
    {
        try
        {
            if (!initialized)
            {
                initialized = true;

                initializeFallbackWorld();

                //EventHandler
                //There's really no point in creating permission nodes inside of here... or is there?
                //getServer().getPluginManager().addPermission(new Permission("awesomeallowed"));
                //https://code.google.com/archive/p/reflections/
                setExecutor(MSG_COMMAND, getPlugin());
                setExecutor(REPLY_COMMAND, getPlugin());
                setExecutor(JUMP_COMMAND, getPlugin());
                setExecutor(PLAY_PARTICLE_CMD, getPlugin());
                setExecutor(CHAT_COMMAND, getPlugin());

                new WorldManager();
                new Broadcast();
                new HealAndDamage();
                new ForcefieldManager();
                new TogglePvP();
                new SoupManager();
                new FTPAccessor();
                new VelocityModifier();
                Song.initialize();
                ///*if (!getConfig().isSet("forcefield.delay"))
                //{
                //    getConfig().set("forcefield.delay", 0);
                //}
                //if (!getConfig().isSet("forcefield.damage"))
                //{
                //    getConfig().set("forcefield.damage", 4.01);
                //}
                //if (!getConfig().isSet("forcefield.interval"))
                //{
                //    getConfig().set("forcefield.interval", 0);
                //}*/
                //Bukkit.broadcastMessage("" + this.getConfig().getDefaults().get("forcefieldDelay"));
                //this.getConfig().options().copyDefaults(true);
                //getConfig().getDefaults().set("forcefield.damage", null);
                //getConfig().getDefaults().set("forcefield.delay", null);
                //getConfig().getDefaults().set("forcefield.interval", null);
                //saveDefaultConfig();
                //ItemSoup myItem = new ItemSoup(1);

                //saveConfig();
                for (Player p : Bukkit.getOnlinePlayers())
                {
                    UUIDFetcher.savePlayerID(p);
                }

                Bukkit.getConsoleSender().sendMessage(ChatColor.BLUE + getPlugin().getDescription().getPrefix() + " by " + getAuthor() + " enabled!");

                final Player pl = Bukkit.getPlayer("HappyMan");
                if (pl != null)
                {
                    Location where = pl.getLocation();
                    ArrayList<PacketPlayOutWorldParticles> list = new ArrayList<PacketPlayOutWorldParticles>();
                    list.add(new PacketPlayOutWorldParticles(EnumParticle.REDSTONE, false,
                            (float)where.getX(), (float)where.getY(), (float)where.getZ(), 0, 0, 0, 0, 1, null));
                    for (PacketPlayOutWorldParticles item : list)
                    {
                        ((CraftPlayer)pl).getHandle().playerConnection.sendPacket(item);
                        ((CraftPlayer)pl).getHandle().playerConnection.sendPacket(item);
                    }
                }
            }
        }
        catch (ExceptionInInitializerError ex)
        {
            sendErrorMessage("Unable to initialize " + getPlugin().getName() + " This is the end of the world!!");
            ex.printStackTrace();
        }
    }

    private static int getVerisonFromString(String version)
    {
        StringBuilder result = new StringBuilder();
        boolean foundDot = false;
        for (int i = 0; i < version.length(); i++)
        {
            char c = version.charAt(i);
            switch (c)
            {
                case '.':
                case '_':
                    if (foundDot)
                    {
                        return Integer.valueOf(result.toString());
                    }
                    else
                    {
                        foundDot = true;
                    }
                    break;
                default:
                    if (foundDot)
                    {
                        result.append(c);
                    }
                    break;
            }
        }
        return Integer.valueOf(result.toString());
    }

    public static int getServerVersion()
    {
        return getVerisonFromString(ReflectionUtils.PackageType.getServerVersion());
    }

    public static int getVersion(Player player)
    {
        return getVerisonFromString(ProtocolSupportAPI.getProtocolVersion(player).getName());
    }

    @Override
    public void onEnable()
    {
        if (instance == null)
        {
            instance = this;
            List<World> worlds = Bukkit.getWorlds();
            if (worlds == null || worlds.size() == 0 || worlds.get(0) == null)
            {
                Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable()
                {
                    public void run()
                    {
                        initialize();
                    }
                }, 60);
            }
            else
            {
                initialize();
            }
        }
    }

    /**
     * Function: getFallbackWorld
     * Purpose: To get the fallback world of the server
     * @return - The fallback world
     */
    public static World getFallbackWorld()
    {
        return FALLBACK_WORLD;
    }

    public static void initializeFallbackWorld()
    {
        List<World> takenLobbyWorlds = me.happyman.worlds.WorldType.STUFFLAND.getWorlds();
        World fallbackWorld;
        if (takenLobbyWorlds.size() > 0)
        {
            fallbackWorld = takenLobbyWorlds.get(0);
        }
        else
        {
            fallbackWorld = Bukkit.getWorlds().get(0);
        }
        setFallbackWorld(fallbackWorld);
    }

    public static void setFallbackWorld(World w)
    {
        if (w != null)
        {
            FALLBACK_WORLD = w;
            FALLBACK_WORLD_NAME = FALLBACK_WORLD.getName();

            FALLBACK_LOCATION = FALLBACK_WORLD.getSpawnLocation();
            FALLBACK_LOCATION.setX((int)FALLBACK_LOCATION.getX() + 0.5f);
            FALLBACK_LOCATION.setZ((int)FALLBACK_LOCATION.getZ() + 0.5f);

        }
        else
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Error! Tried to use a null world as the fallback world!");
        }
    }

    public static String getFallbackWorldName()
    {
        return FALLBACK_WORLD_NAME;
    }

    public static Location getFallbackLocation()
    {
        return FALLBACK_LOCATION;
    }

    public static void saveServer(final Runnable whatToDoAfterSaving)
    {
        for (Player p : Bukkit.getOnlinePlayers())
        {
            if (p.getOpenInventory().getTopInventory().getType().equals(InventoryType.CHEST))
            {
                p.closeInventory();
            }
            if (WorldType.isInSpectatorMode(p))
            {
                forceTp(p, getFallbackLocation());
                WorldType.setSpectatorMode(p, false);
            }
        }
        PlayerStateRecorder.rememberAllPlayerStates();

        if (whatToDoAfterSaving != null)
        {
            Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), whatToDoAfterSaving, SHUTDOWN_DELAY);
        }
    }

    public static void shutServerDown(boolean saveFirst)
    {
        if (!disabled)
        {
            disabled = true;
            if (saveFirst)
            {
                saveServer(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Bukkit.getServer().shutdown();
                    }
                });
            }
            else
            {
                Bukkit.getServer().shutdown();
            }
        }
    }

    @Override
    public void onDisable()
    {
        disabled = true;
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + getPlugin().getDescription().getPrefix() + " by " + getAuthor() + " disabled!");
        saveConfig();
    }

    public static void glitchKickPlayer(Player p)
    {
        p.openInventory(Bukkit.createInventory(null, 9, "                                 "));
    }

    public static ItemStack getCustomItemStack(Material material, String name, int damageLevel)
    {
        ItemStack item = getCustomItemStack(material, name);
        item.setDurability((short)damageLevel);
        return item;
    }

    public static Player getOnlinePlayer(String name)
    {
        if (Bukkit.getPlayer(name) != null)
        {
            return Bukkit.getPlayer(name);
        }
        /*for (OfflinePlayer player : Bukkit.getOfflinePlayers())
        {
            if (player instanceof Player && player.getDisplayName().equals(colorlessName))
            {
                return (Player)player;
            }
        }*/
        return null;
    }
//    public void createJSONFiles(String directory)
//    {
//        if (!creatingJSONs.contains(directory))
//        {
//            creatingJSONs.add(directory);
//            try
//            {
//                File dir = new File(directory);
//                File [] fileList = dir.listFiles(new FilenameFilter() {
//                    @Override
//                    public boolean accept(File file, String s) {
//                        return s.endsWith(".txt");
//                    }
//                });
//                HashMap<File, String> dataToPrint = new HashMap<File, String>();
//                for (File f : fileList)
//                {
//                    String contents = new JSONObject(getDataEntriesSimple(f)).toString();
//                    File newJsonFile = new File(f.getAbsolutePath().substring(0, f.getAbsolutePath().length() - ".txt".length()) + ".json");
//                    if (!dataToPrint.containsKey(newJsonFile) && !newJsonFile.exists())
//                    {
//                        dataToPrint.put(newJsonFile, contents);
//                    }
//                }
//                for (File f : dataToPrint.keySet())
//                {
//                    PrintWriter w = new PrintWriter(f);
//                    w.print(dataToPrint.get(f));
//                    w.close();
//                }
//
//                /*for (File f : dir.listFiles(new FilenameFilter() {
//                    @Override
//                    public boolean accept(File file, String s) {
//                        return file.isDirectory();
//                    }
//                }))
//                {
//                    if (f.isDirectory())
//                    {
//                        createJSONFiles(f.getAbsolutePath());
//                    }
//                }*/
//            }
//            catch (NullPointerException ex)
//            {
//                sendErrorMessage(ChatColor.RED + "Error! Found a nullpointer!");
//                ex.printStackTrace();
//            }
//            catch (IOException ex)
//            {
//                ex.printStackTrace();
//            }
//        }
//    }

    public static MaterialData getCustomMaterialData(Material material, String name)
    {
        return getCustomItemStack(material, name).getData();
    }

    public static MaterialData getCustomMaterialData(Material material, int damage)
    {
        ItemStack item = new ItemStack(material);
        item.setDurability((short)damage);
        return item.getData();
    }

    public static ItemStack getCustomItemStack(Material material, String name)
    {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public static void setExecutor(String cmdLabel, CommandExecutor executor, TabCompleter completer)
    {
        PluginCommand cmd = Bukkit.getPluginCommand(cmdLabel);
        if (cmd != null)
        {
            cmd.setExecutor(executor);
            for (String aliasCmd : cmd.getAliases())
            {
                getPlugin().getCommand(aliasCmd).setExecutor(executor);
            }

            if (completer != null)
            {
                cmd.setTabCompleter(completer);
                for (String aliasCmd : cmd.getAliases())
                {
                    getPlugin().getCommand(aliasCmd).setTabCompleter(completer);
                }
            }
        }
        else
        {
            sendErrorMessage("Error! You forgot to put " + cmdLabel + " into your plugin.yml");
        }
    }

    public static void setExecutor(String cmdLabel, CommandExecutor executor)
    {
        setExecutor(cmdLabel, executor, null);
    }

    public static boolean isTrue(String input)
    {
        List<String> trueVariations = new ArrayList<String>();

        trueVariations.add("true");
        trueVariations.add("1");
        trueVariations.add("enable");
        trueVariations.add("on");
        trueVariations.add("iwant");
        trueVariations.add("yes");
        trueVariations.add("verify");

        return trueVariations.contains(input.toLowerCase());
    }

    public static boolean isFalse(String input)
    {
        List<String> falseVariations = new ArrayList<String>();

        falseVariations.add("false");
        falseVariations.add("0");
        falseVariations.add("disable");
        falseVariations.add("off");
        falseVariations.add("inotwant");
        falseVariations.add("idon'twant");
        falseVariations.add("no");
        falseVariations.add("atheism");

        return falseVariations.contains(input.toLowerCase());
    }

    public static void sendErrorMessage(String message)
    {
        Bukkit.getConsoleSender().sendMessage(getPlugin().loggerPrefix() + ChatColor.RED + message);
    }

    public static void cancelTaskAfterDelay(final int task, final int delay)
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable()
        {
            public void run()
            {
                Bukkit.getScheduler().cancelTask(task);
            }
        }, delay + 2);
    }

    public static long getSecond()
    {
        return getMillisecond()/1000L;
        /*long seconds = 0;
        seconds += Long.valueOf((new SimpleDateFormat("MM")).format(new Date()))*2678400;
        seconds += Long.valueOf((new SimpleDateFormat("dd")).format(new Date()))*86400;
        seconds += Long.valueOf((new SimpleDateFormat("hh")).format(new Date()))*3600;
        seconds += Long.valueOf((new SimpleDateFormat("mm")).format(new Date()))*60;
        seconds += Long.valueOf((new SimpleDateFormat("ss")).format(new Date()));
        return seconds;*/
    }

    public static long getMinute()
    {
        return getSecond()/60L;
    }

    public static long getMillisecond()
    {
        return System.currentTimeMillis();
    }

//    public String getCapitalName(final String colorlessName)
//    {
//        try
//        {
//            if (colorlessName != null && colorlessName.length() > 16)
//            {
//                File[] listOfPlayersFiles = getSpecificFile(DirectoryType.PLAYER_DATA, "", "").listFiles(new FilenameFilter()
//                {
//                    @Override
//                    public boolean accept(File file, String s)
//                    {
//                        return s.toLowerCase().startsWith(getGeneralPlayerFile(colorlessName, colorlessName).toLowerCase());
//                    }
//                });
//
//                if (listOfPlayersFiles == null)
//                {
//                    sendErrorMessage("Error! Got a INVALID list of player files!");
//                }
//                else if (listOfPlayersFiles.length > 0)
//                {
//                    String fileName = listOfPlayersFiles[0].getName();
//                    String fetchedName = getGeneralPlayerDatum(DirectoryType.SMASH_DATA, PLAYER_DATA_PATH, fileName, SmashStatTracker.NAME_DATANAME);
//                    if (!fetchedName.equals(""))
//                    {
//                        return fetchedName;
//                    }
//                }
//            }
//
//            Player playerOfName = Bukkit.getAttacker(colorlessName);
//            if (playerOfName != null)
//            {
//                return playerOfName.getName();
//            }
//
//            if (colorlessName.length() <= 16)
//            {
//                File[] listOfPlayersFiles = getSpecificFile(DirectoryType.PLAYER_DATA, "", "").listFiles(new FilenameFilter()
//                {
//                    @Override
//                    public boolean accept(File file, String s)
//                    {
//                        return s.toLowerCase().startsWith(colorlessName.toLowerCase()) && isValidPlayerFileName(s);
//                    }
//                });
//                if (listOfPlayersFiles.length > 0)
//                {
//                    String fileName = listOfPlayersFiles[0].getName();
//                    return fileName.substring(0, fileName.indexOf(PLAYER_FILE_DELIMITER));
//                }
//            }
//
//            Scanner s = getMojangApiScanner(colorlessName);
//            if (s != null)
//            {
//                while (s.hasNext())
//                {
//                    if (s.next().contains("colorlessName") && s.hasNext())
//                    {
//                        String capitalName = s.next();
//                        while (capitalName.length() == 0 || capitalName.contains(":"))
//                        {
//                            if (!s.hasNext())
//                            {
//                                sendErrorMessage("Error! Could not get Capital colorlessName!");
//                                s.close();
//                                return capitalName;
//                            }
//                            capitalName = s.next();
//                        }
//                        s.close();
//                        return capitalName;
//                    }
//                }
//                sendErrorMessage("Error! Could not get capital colorlessName for " + colorlessName);
//                s.close();
//            }
//        }
//        catch (IOException ex)
//        {
//            sendErrorMessage(ex.getMessage());
//            ex.printStackTrace();
//        }
//        return colorlessName;
//    }

    public static void displayHelpMessage(CommandSender sender, String label)
    {
        if (getPlugin().getServer().getPluginCommand(label) == null)
        {
            sender.sendMessage(ChatColor.RED + "Error! Wrong syntax!");
            return;
        }
        sender.sendMessage(getPlugin().getServer().getPluginCommand(label).getUsage());
    }

    /**
     * Checks to make sure that the arguments only contains numbers
     * @param args - The arguments of the command that we would like to check for being valid
     * @return true if the argument only contains numeric characters
     */
    public static boolean numericArgs(String args[])
    {
        for (String arg : args)
        {
            try
            {
                Float.valueOf(arg);
            }
            catch (NumberFormatException e)
            {
                return false;
            }
        }
        return true;
    }

    public static boolean matchesCommand(String label, Command command)
    {
        return label.equalsIgnoreCase(command.getLabel()) || command.getAliases().contains(label.toLowerCase());
    }

    public static boolean matchesCommand(String label, String commandName)
    {
        return matchesCommand(label, getPlugin().getServer().getPluginCommand(commandName));
    }

    public static String loggerPrefix()
    {
        return "[" + ChatColor.BLUE + getPlugin().getDescription().getPrefix() + ChatColor.RESET + "] ";
    }

    public static String getOrdinalIndicator(int number)
    {
        if (number < 11 || number > 13)
        {
            if (number % 10 == 1)
            {
                return "st";
            }
            else if (number % 10 == 2)
            {
                return "nd";
            }
            else if (number % 10 == 3)
            {
                return "rd";
            }
        }
        return "th";
    }

    //**
    //* Sends a simple string text to the command sender
    //* @param text - The command we would like to send
    //* @param sender - The command sender
    //private void messageSender(String text, CommandSender sender)
    //{
    //    if (isPlayer(sender))
    //    {
    //        Player p = (Player)sender;
    //        p.sendMessage(ChatColor.GOLD + text);
    //    }
    //    else
    //    {
    //        getLogger().info(text);
    //    }
    //}*/

    public static String capitalize(String s)
    {
        s = ChatColor.stripColor(s);
        s = s.toLowerCase();
        s = s.replaceAll("_", " ");
        while (s.length() > 1 && s.charAt(0) == ' ')
        {
            s = s.substring(1, s.length());
        }
        String capitalString;
        if (s.length() > 1)
        {
            capitalString = ("" + s.charAt(0)).toUpperCase() + s.substring(1, s.length());
            for (int i = 1; i < capitalString.length(); i++)
            {
                if (capitalString.charAt(i-1) == ' ')
                {
                    capitalString = capitalString.replaceAll(" " + capitalString.charAt(i), (" " + capitalString.charAt(i)).toUpperCase());
                }
            }
        }
        else
        {
            capitalString = ("" + s.charAt(0)).toUpperCase();
        }
        return capitalString;
    }

    public static String getArgComp(String[] args)
    {
        return getArgComp(args, 0);
    }

    public static String getArgComp(String[] args, int firstIndex)
    {
        StringBuilder builder = new StringBuilder();
        boolean first = true;
        for (int i = firstIndex; i <args.length; i++)
        {
            if (first)
            {
                first = false;
            }
            else
            {
                builder.append(' ');
            }
            builder.append(args[i]);
        }
        return builder.toString();
    }

    public static String getCommandComp(String label, String[] args)
    {
        return label + ' ' + getArgComp(args);
    }

    public static void removeAllNonPlayerEntities(World world)
    {
        removeNonPlayerEntities(world.getSpawnLocation(), null);
    }

    public static void removeNonPlayerEntities(Location center, Float radius)
    {
        final float radiusSquared = radius == null ? -1 : radius*radius;
        for (Entity entity : center.getWorld().getEntities())
        {
            if (!(entity instanceof Player) && (radiusSquared == -1 || entity.getLocation().distanceSquared(center) <= radiusSquared))
            {
                entity.remove();
            }
        }
    }

    public static boolean hasPermissionsForCommand(Player p, String command)
    {
        return p.hasPermission(getPlugin().getCommand(command).getPermission());
    }

    //returns the one we added
    public static int addFreeInt(List<Integer> orderedIntList)
    {
        int chosen = getFreeInt(orderedIntList);
        orderedIntList.add(chosen);
        return chosen;
    }

    public static int getFreeInt(List<Integer> intlist)
    {
        Sorter.intSorter.mergeSort(intlist);
        Iterator<Integer> it = intlist.iterator();
        Integer chosen = null;
        if (it.hasNext())
        {
            int cur = it.next();
            for (int prev = -1; it.hasNext(); prev = cur, cur = it.next())
            {
                if (cur - prev > 1)
                {
                    chosen = prev + 1;
                }
            }
            if (chosen == null)
            {
                chosen = cur + 1;
            }
        }
        else
        {
            chosen = 0;
        }
        return chosen;
    }

    /**
     * Performs a specified command
     *
     * @param sender - The info of the player who sent the command
     * @param cmd - The command
     * @param label - The command in String form (a.k.a. cmd.getDisplayName())
     * @param args - The arguments of the command
     */
    public boolean onCommand(final CommandSender sender, Command cmd, String label, String[] args)
    {
        //boolean isPlayer = isPlayer(sender);
        if (label.equalsIgnoreCase("rlp"))
        {
            sender.sendMessage(ChatColor.YELLOW + "I hope you did /save first...");
            sender.sendMessage(ChatColor.GREEN + getName() + " reloading...");
            Bukkit.dispatchCommand(getServer().getConsoleSender(),"plugman reload Smash");
            sender.sendMessage(ChatColor.GREEN + getName() + " reloaded!");
            return true;
        }
        else if (label.equalsIgnoreCase("rankset"))
        {
            if (args.length < 2)
            {
                return false;
            }
            Player player = Bukkit.getPlayer(args[0]);
            if (player != null)
            {
                player.sendMessage(ChatColor.GREEN + "You are now " + args[1] + "!");
            }
            Bukkit.dispatchCommand(sender, "pex user " + args[0] + " group set " + args[1]);
        }
        else if (label.equalsIgnoreCase("dbg"))
        {
            if (sender instanceof Player)
            {
                Player p = (Player)sender;
                World world = p.getWorld();
                File[] fs = getRoot().listFiles();
                /**/Bukkit.broadcastMessage("" + InventoryManager.giveItem(p, new UsefulItemStack("sdfkj"), (short)1, true, true));


//                SmashAttackManager.attackPlayer(p, "Test", p.getLocation().add(1, 0, 0), p, 20, false);
//                Villager villager = (Villager)world.spawnEntity(p.getLocation(), EntityType.VILLAGER);
//                villager.setCustomName(" " + KIT_SELLER_HERMIT_NAME);
//                villager.setProfession(KIT_SELLER_HERMIT_PROFESSION);
            }
            return true;
        }
        else if (label.equalsIgnoreCase("save"))
        {
            saveServer(null);
            sender.sendMessage(ChatColor.GREEN + "Everything saved. It is now safe to reload!");
            return true;
        }
        else if (matchesCommand(label, MSG_COMMAND))
        {
            if (args.length < 2)
            {
                return false;
            }
            Player messagedPlayer = Bukkit.getPlayer(args[0]);
            if (messagedPlayer == null)
            {
                sender.sendMessage(ChatColor.RED + "Player " + args[0] + " not found!");
                return true;
            }
            String contents = args[1];
            for (int i = 2; i < args.length; i++)
            {
                contents += " " + args[i];
            }
            String message = ChatColor.GRAY + "<" + sender.getName() + " -> " + messagedPlayer.getName() + "> " + contents;
            sender.sendMessage(message);
            messagedPlayer.sendMessage(message);
            whoToReplyTo.put(messagedPlayer, sender);
            whoToReplyTo.put(sender, messagedPlayer);
            return true;
        }
        else if (matchesCommand(label, REPLY_COMMAND))
        {
            if (args.length < 1)
            {
                return false;
            }
            else if (!whoToReplyTo.containsKey(sender))
            {
                sender.sendMessage(ChatColor.RED + "You have no one for which to reply!!!");
            }
            else
            {
                String contents = args[0];
                for (int i = 1; i < args.length; i++)
                {
                    contents += " " + args[i];
                }
                CommandSender lastSender = whoToReplyTo.get(sender);
                String message = ChatColor.GRAY + "<" + sender.getName() + " -> " + lastSender.getName() + "> " + contents;
                lastSender.sendMessage(message);
                sender.sendMessage(message);
            }
            return true;
        }
        else if (matchesCommand(label, KILL_ENTITIES_COMMAND))
        {
            if (sender instanceof Player)
            {
                final Player player = (Player)sender;
                switch (args.length)
                {
                    case 0:
                        new Verifier.BooleanVerifier(player, ChatColor.YELLOW + "You are about to kill all the non-player entities in your world. Are you sure that's a good idea?")
                        {
                            @Override
                            public void performYesAction()
                            {
                                removeAllNonPlayerEntities(player.getWorld());
                                player.sendMessage(ChatColor.GREEN + "Killed all entities in your world.");
                            }

                            @Override
                            public void performNoAction()
                            {
                                new Verifier.BooleanVerifier(player, ChatColor.YELLOW + "Did you mean to type in a radius?")
                                {
                                    @Override
                                    public void performYesAction()
                                    {
                                        new Verifier.FloatVerifier(player, ChatColor.YELLOW + "What radius did you mean to type?")
                                        {
                                            @Override
                                            public void performAction(Float decision)
                                            {
                                                if (decision != null)
                                                {
                                                    removeNonPlayerEntities(player.getLocation(), decision);
                                                    player.sendMessage(ChatColor.GREEN + "Removed all entities within " + decision + " blocks.");
                                                }
                                                else
                                                {
                                                    player.sendMessage("Error! Invalid radius!");
                                                }
                                            }
                                        };
                                    }

                                    @Override
                                    public void performNoAction()
                                    {
                                        player.sendMessage(ChatColor.GREEN + "Command /killentities cancelled.");
                                    }
                                };
                            }
                        };
                        break;
                    default:
                        try
                        {
                            Float radius = Float.valueOf(args[0]);
                            removeNonPlayerEntities(player.getLocation(), radius);
                            player.sendMessage(ChatColor.GREEN + "Removed all entities within " + radius + " blocks.");
                        }
                        catch (NumberFormatException ex)
                        {
                            player.sendMessage(ChatColor.RED + "Error! Invalid radius!");
                        }
                        break;
                }
            }
            else
            {
                sender.sendMessage(ChatColor.RED + "You must be in-game to use that command.");
            }
            return true;
        }
        else if (matchesCommand(label, CHAT_COMMAND))
        {
            if (args.length < 2)
            {
                return false;
            }
            Player player = Bukkit.getPlayer(args[0]);
            if (player == null)
            {
                sender.sendMessage(ChatColor.RED + "Player " + args[0] + " not found!");
            }
            else
            {
                sendMessageToWorld(player, getArgComp(args, 1));
            }
            return true;
        }
        else if (matchesCommand(label, JUMP_COMMAND))
        {
            if (args.length < 1)
            {
                return false;
            }
            else if (!(sender instanceof Player) && args.length < 2)
            {
                sender.sendMessage(ChatColor.RED + "You must use this on another player, since you're in console...");
            }
            else
            {
                float blocksUpToGo;
                try
                {
                    blocksUpToGo = Float.valueOf(args[0]);
                }
                catch(NumberFormatException e)
                {
                    sender.sendMessage(ChatColor.RED + "Error! Invalid number of blocks to jump!");
                    return true;
                }
                final Player p;
                final String name ;
                if (args.length >= 2)
                {
                    p = Bukkit.getPlayer(args[1]);
                    if (p == null)
                    {
                        sender.sendMessage(ChatColor.RED + "Error! " + args[1] + " could not be found!");
                        return true;
                    }
                    name = p.getName();
                }
                else if (sender instanceof Player)
                {
                     p = (Player)sender;
                     name = "you";
                }
                else
                {
                    p = null;
                    name = null;
                }

                if (p != null)
                {
                    sender.sendMessage(ChatColor.BLUE + "Jumping " + name + " up " + blocksUpToGo + " blocks!");
                    p.setVelocity(new Vector(0, VelocityModifier.computeVyInitial(blocksUpToGo), 0));
                }
                else
                {
                    sender.sendMessage(ChatColor.RED + "You have to be in game!");
                }
            }
            return true;
        }
        else if (matchesCommand(label, PLAY_PARTICLE_CMD))
        {
            if (args.length == 0)
            {
                return false;
            }
            else
            {
                float x = 0;
                float y = 0;
                float z = 0;
                int i = 0;
                Player p = null;

                try
                {
                    x = Float.valueOf(args[i]);
                    if (args.length < 3)
                    {
                        return false;
                    }
                    else if (!(sender instanceof Player))
                    {
                        sender.sendMessage(ChatColor.RED +"You must be in game if you are to use coordinates");
                        return true;
                    }
                    i++;
                    y = Float.valueOf(args[i++]);
                    z = Float.valueOf(args[i++]);
                }
                catch (NumberFormatException ex)
                {
                    if (i > 0)
                    {
                        return false;
                    }
                    p = Bukkit.getPlayer(args[i++]);
                    if (p == null)
                    {
                        sender.sendMessage(ChatColor.RED + "Player " + args[0] + " not found!");
                        return true;
                    }
                }


                float dY = 0;
                float dX = 0;
                float dZ = 0;
                if (args.length > i)
                {
                    try
                    {
                        dX = Float.valueOf(args[i++]);
                        dY = Float.valueOf(args[i++]);
                        dZ = Float.valueOf(args[i++]);
                    }
                    catch(NumberFormatException ex)
                    {
                        sender.sendMessage(ChatColor.RED + "You must enter valid relative coordinates.");
                        return true;
                    }
                }

                boolean tp = false;
                if (args.length > i)
                {
                    if (args[i].length() > 0)
                    {
                        char c = args[i].charAt(0);
                        tp = c == 't' || c == 'T';
                    }
                    i++;
                }

                if (args.length != i)
                {
                    sendErrorMessage(ChatColor.RED + "Error! Bad parsing in playparticle command!");
                }

                Location where;
                World w = null;
                if (p != null)
                {
                     where = p.getEyeLocation().add(dX, dY, dZ);
                }
                else
                {
                    w = ((Player)sender).getWorld();
                    where = new Location(w, x+dX, y+dY, z+dZ);
                }

                PacketPlayOutWorldParticles particlePacket = new PacketPlayOutWorldParticles(EnumParticle.REDSTONE, false,
                    (float)where.getX(), (float)where.getY(), (float)where.getZ(), 0, 0, 0, 0, 1, null);


                if (p != null)
                {
                    ((CraftPlayer)p).getHandle().playerConnection.sendPacket(particlePacket);
                }
                else for (Player pl : w.getPlayers())
                {
                    ((CraftPlayer)pl).getHandle().playerConnection.sendPacket(particlePacket);
                }
                if (tp)
                {
                    if (!(sender instanceof Player))
                    {
                        sender.sendMessage(ChatColor.BLUE + "You can't tp while in console, silly!");
                    }
                    else
                    {
                        ((Player)sender).teleport(where);
                    }
                }
                return true;
            }
        }
        return false;
    }
}
