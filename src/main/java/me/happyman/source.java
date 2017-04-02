package me.happyman;

import me.happyman.commands.*;
import me.happyman.Listeners.VerifierHandler;
import me.happyman.utils.SmashWorldManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import java.util.*;
import java.text.SimpleDateFormat;

public class source extends JavaPlugin
{
    private final String dataExtension = ".txt";
    private final String PLAYER_DATA_FOLDER = "PlayerData";
    public final String SETTING_FOLDER = "ServerData";
    private final String SERVER_PATH = getDataFolder().getAbsolutePath().substring(0, getDataFolder().getAbsolutePath().length() - getDataFolder().getPath().length());
    private final String PLUGIN_DATAPATH = getDataFolder().getPath() + "/";

    private final String MSG_COMMAND = "msg";
    private final String REPLY_COMMAND = "r";
    private HashMap<CommandSender, CommandSender> lastSenders;

    @Override
    public void onEnable()
    {
        //EventHandler
        //There's really no point in creating permission nodes inside of here... or is there?
        //getServer().getPluginManager().addPermission(new Permission("awesomeallowed"));
        //https://code.google.com/archive/p/reflections/
        setExecutor(MSG_COMMAND, this);
        setExecutor(REPLY_COMMAND, this);
        new Broadcast(this);
        new DisableWeather(this);
        new HealAndDamage(this);
        new ForcefieldManager(this);
        new TogglePvP(this);
        new SoupManager(this);
        new VerifierHandler(this);
        new SmashManager(this);
        lastSenders = new HashMap<CommandSender, CommandSender>();
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

        saveConfig();
        Bukkit.getConsoleSender().sendMessage(ChatColor.BLUE + getDescription().getPrefix() + " by HappyMan enabled!");
    }

    @Override
    public void onDisable()
    {
        SmashWorldManager.performDisable();
        saveConfig();
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + getDescription().getPrefix() + " by HappyMan disabled!");
    }

    //Uncomment the method body if you Run into trouble with your Runnables
    public void startRunnable(Runnable r)
    {
        //Thread thread = new Thread(r);
        //thread.start();
    }

    public ItemStack getCustomItemStack(Material material, String name, int damageLevel)
    {
        ItemStack item = getCustomItemStack(material, name);
        item.setDurability((short)damageLevel);
        return item;
    }

    public Player getOnlinePlayer(String name)
    {
        if (Bukkit.getPlayer(name) != null)
        {
            return Bukkit.getPlayer(name);
        }
        /*for (OfflinePlayer player : Bukkit.getOfflinePlayers())
        {
            if (player instanceof Player && player.getName().equals(name))
            {
                return (Player)player;
            }
        }*/
        return null;
    }

    public MaterialData getCustomMaterialData(Material material, String name)
    {
        return getCustomItemStack(material, name).getData();
    }

    public MaterialData getCustomMaterialData(Material material, int damage)
    {
        ItemStack item = new ItemStack(material);
        item.setDurability((short)damage);
        return item.getData();
    }

    public ItemStack getCustomItemStack(Material material, String name)
    {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }

    public void setExecutor(String cmdLabel, CommandExecutor executor)
    {
        PluginCommand cmd = getCommand(cmdLabel);
        cmd.setExecutor(executor);
        for (String s : cmd.getAliases())
        {
            getCommand(s).setExecutor(executor);
        }
    }

    public boolean isTrue(String input)
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

    public boolean isFalse(String input)
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

    public void sendErrorMessage(String message)
    {
        Bukkit.getConsoleSender().sendMessage(loggerPrefix() + ChatColor.RED + message);
    }

    public void cancelTaskAfterDelay(final int task, final int delay)
    {
        Runnable r = new Runnable()
        {
            public void run()
            {
                getServer().getScheduler().cancelTask(task);
            }
        };
        startRunnable(r);
        getServer().getScheduler().scheduleSyncDelayedTask(this, r, delay + 2);
    }

    //**********DATA MANAGEMENT STARTS HERE*************
    public long getSecond()
    {
        long seconds = 0;
        seconds += Long.valueOf((new SimpleDateFormat("MM")).format(new Date()))*2678400;
        seconds += Long.valueOf((new SimpleDateFormat("dd")).format(new Date()))*86400;
        seconds += Long.valueOf((new SimpleDateFormat("hh")).format(new Date()))*3600;
        seconds += Long.valueOf((new SimpleDateFormat("mm")).format(new Date()))*60;
        seconds += Long.valueOf((new SimpleDateFormat("ss")).format(new Date()));
        return seconds;
    }

    public long getMinute()
    {
        return getSecond()/60;
    }

    public long getMillisecond()
    {
        long milliseconds = 0;
        //milliseconds += Long.valueOf((new SimpleDateFormat("MM")).format(new Date()))*2678400000L;
        //milliseconds += Long.valueOf((new SimpleDateFormat("dd")).format(new Date()))*86400000L;
        milliseconds += Long.valueOf((new SimpleDateFormat("hh")).format(new Date()))*3600000L;
        milliseconds += Long.valueOf((new SimpleDateFormat("mm")).format(new Date()))*60000L;
        milliseconds += Long.valueOf((new SimpleDateFormat("ss")).format(new Date()))*1000L;
        milliseconds += Long.valueOf((new SimpleDateFormat("SSS")).format(new Date()));
        return milliseconds;
    }

    private String getUUID(String p)
    {
        if (Bukkit.getPlayer(p) != null)
        {
            return Bukkit.getPlayer(p).getUniqueId().toString();
        }
        Scanner s = getMojangApiScanner(p);
        if (s != null)
        {
            while (s.hasNext())
            {
                if (s.next().contains("id") && s.hasNext())
                {
                    String uuid = s.next();
                    if (uuid.length() < 10)
                    {
                        if (!s.hasNext())
                        {
                            sendErrorMessage("Error! Could not get UUID!");
                            //return null;
                        }
                        uuid = s.next();
                    }
                    if (!uuid.contains("-"))
                    {
                        uuid = uuid.substring(0, 8) + "-" + uuid.substring(8, 12) + "-" + uuid.substring(12, 16) + "-" + uuid.substring(16, 20) + "-" + uuid.substring(20, uuid.length());
                    }
                    return uuid;
                }
            }
        }
        sendErrorMessage("Error! Could not get unique id for " + p);
        return null;
    }

    private Scanner getMojangApiScanner(String playerName)
    {
        try
        {
            Scanner s = new Scanner(new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName).openConnection().getInputStream());
            s.useDelimiter("\"");
            return s;
        }
        catch (IOException e)
        {
            sendErrorMessage("Error! Couldn't get scanner for " + playerName);
        }
        return null;
    }

    public String getCapitalName(String p)
    {
        if (Bukkit.getPlayer(p) != null)
        {
            return Bukkit.getPlayer(p).getName();
        }
        Scanner s = getMojangApiScanner(p);
        if (s != null)
        {
            while (s.hasNext())
            {
                if (s.next().contains("name") && s.hasNext())
                {
                    String name = s.next();
                    while (name.length() == 0 || name.contains(":"))
                    {
                        if (!s.hasNext())
                        {
                            sendErrorMessage("Error! Could not get Capital name!");
                            return p;
                        }
                        name = s.next();
                    }
                    return name;
                }
            }
            sendErrorMessage("Error! Could not get capital name for " + p);
        }
        return p;
    }

    private String getPathOfPluginFolder(String folderName)
    {
        return getPluginDataPath() + folderName + "/" ;
    }

    public String getPluginDataPath()
    {
        return PLUGIN_DATAPATH;
    }

    private String getFullFileName(String fileName)
    {
        return fileName + dataExtension;
    }

    public String getServerPath()
    {
        return SERVER_PATH;
    }

    public String getPathOfFolder(String path)
    {
        return getServerPath() + path + "/";
    }

    public List<String> getDataKeys(String folderName, String fileName)
    {
        Scanner keyReader = getScannerForFile(folderName, fileName);
        ArrayList<String> keys = new ArrayList<String>();
        if (keyReader == null)
        {
            return keys;
        }
        while (keyReader.hasNextLine())
        {
            Scanner line = new Scanner(keyReader.nextLine());
            if (line.hasNext(".*:"))
            {
                String keyWithColon = line.next(".*:");
                keys.add(keyWithColon.substring(0, keyWithColon.length() - 1));
                //if (keyReader.hasNext(".\n"
            }
        }
        return keys;
    }

    public List<String> getPluginDataKeys(String folderName, String fileName)
    {
        return getDataKeys(getPathOfPluginFolder(folderName), fileName);
    }

    public void renameRootDataName(String oldName, String newName)
    {
        changeFolderDataName(getServerPath(), oldName, newName);
    }

    public void renameFolderData(String fullPath, String oldName, String newName)
    {
        changeFolderDataName(fullPath, oldName, newName);
    }

    public void renamePluginFolderDataName(String oldName, String newName)
    {
        changeFolderDataName(getPluginDataPath(), oldName, newName);
    }

    public void renamePlayerFolderDataName(String oldName, String newName)
    {
        remamePluginFolderDataName(PLAYER_DATA_FOLDER, oldName, newName);
    }

    public void remamePluginFolderDataName(String folderName, String oldName, String newName)
    {
        changeFolderDataName(getPathOfPluginFolder(folderName), oldName, newName);
    }

    public void removeFolderData(String name)
    {
        changeFolderDataName(getServerPath(), name, null);
    }

    public void removeFileData(String folderName, String fileName, String name)
    {
        changeFileDataName(folderName, fileName, name, null);
    }

    public void removeFileData(String fullPath, String name)
    {
        changeFolderDataName(fullPath, name, null);
    }

    public void removePluginFolderData(String name)
    {
        changeFolderDataName(getPluginDataPath(), name, null);
    }

    public void removePlayerFolderData(String name)
    {
        removePluginFolderData(PLAYER_DATA_FOLDER, name);
    }

    public void removePluginFolderData(String folderName, String name)
    {
        changeFolderDataName(getPathOfPluginFolder(folderName), name, null);
    }

    public void replacePluginFolderText(String folderName, String wordToChange, String wordToChangeTo)
    {
        changeFolderData(getPathOfPluginFolder(folderName), wordToChange, wordToChangeTo, true);
    }

    public void replacePluginFolderText(String wordToChange, String wordToChangeTo)
    {
        changeFolderData(getPluginDataPath(), wordToChange, wordToChangeTo, true);
    }

    private void changeFileDataName(File file, String oldName, String newName)
    {
        changeFileData(file, oldName, newName, false);
    }

    private void changeFileData(File file, String oldName, String newName, boolean justReplaceAllWords)
    {
        if (file.getName().endsWith(dataExtension))
        {
            List<String> dataList;
            try
            {
                Scanner scanner = new Scanner(file);
                dataList = new ArrayList<String>();
                while (scanner.hasNextLine())
                {
                    dataList.add(scanner.nextLine());
                }
                for (int i = 0; i < dataList.size(); i++)
                {
                    if (!justReplaceAllWords)
                    {
                        if (dataList.get(i).contains(oldName + ":"))
                        {
                            if (newName == null)
                            {
                                dataList.remove(i);
                                i--;
                            }
                            else
                            {
                                String s = dataList.get(i);
                                s = s.replaceAll(oldName, newName);
                                dataList.remove(i);
                                dataList.add(i, s);
                            }
                        }
                    }
                    else
                    {
                        String s = dataList.get(i);
                        s = s.replaceAll(oldName, newName);
                        dataList.remove(i);
                        dataList.add(i, s);
                    }
                }

                printLinesToFile(file, dataList);
            }
            catch (FileNotFoundException e)
            {
                sendErrorMessage("Error! Tried to rewrite data in " + file.getName() + ", but it was missing!");
            }
        }
    }

    private void changeFolderDataName(String fullDataPath, String oldName, String newName)
    {
        changeFolderData(fullDataPath, oldName, newName, false);
    }

    private void changeFolderData(String fullDataPath, String oldName, String newName, boolean justChangeAllTheWords)
    {
        File[] folderList = (new File(fullDataPath)).listFiles();
        for (File innerFolder : folderList)
        {
            changeFileData(innerFolder, oldName, newName, justChangeAllTheWords);
            if (innerFolder.listFiles() != null)
            {
                for (File dataFile : innerFolder.listFiles())
                {
                    if (dataFile != null)
                    {
                        changeFileData(dataFile, oldName, newName, justChangeAllTheWords);
                    }
                }
            }
        }
    }

    private void changeFileDataName(String folderName, String fileName, String oldName, String newName)
    {
        changeFileDataName(getDataFile(folderName, fileName), oldName, newName);
    }

    public void deleteFolder(String fullFolderPath)
    {
        deleteFolder(new File(fullFolderPath));
    }

    public void clearFile(String folderName, String fileName)
    {
        printLinesToFile(getDataFile(folderName, fileName), new ArrayList<String>());
    }

    public void deleteFile(String folderName, String fileName)
    {
        File f = new File(getPathAndFileName(folderName, fileName));
        if (!f.exists())
        {
            sendErrorMessage("Could not find file " + getFullFileName(fileName) + " in " + folderName);
        }
        else if (!f.delete())
        {
            sendErrorMessage("Error! Unable to delete file " + fileName + " in " + folderName + "!");
        }
    }

    public void deleteFolder(File f)
    {
        if (f != null && f.listFiles() != null)
        {
            for (File innerFile : f.listFiles())
            {
                if (innerFile != null && innerFile.listFiles() != null)
                {
                    for (File deepFile : innerFile.listFiles())
                    {
                        if (deepFile != null)
                            deepFile.delete();
                    }
                }
                if (innerFile != null)
                {
                    innerFile.delete();
                }
            }
        }
        f.delete();
    }

    public int incrementStatistic(String folderName, String fileName, String dataName)
    {
        int dataValue;
        try
        {
            dataValue = Integer.valueOf(getDatum(folderName, fileName, dataName));
        }
        catch (NumberFormatException e)
        {
            dataValue = 0;
        }
        int incrementation = dataValue + 1;
        putDatum(folderName, fileName, dataName, incrementation);
        return incrementation;
    }

    public int incrementPluginStatistic(String folderName, String fileName, String dataName)
    {
        return incrementStatistic(getPathOfPluginFolder(folderName), fileName, dataName);
    }

    public int incrementPluginStatistic(String p, String dataName)
    {
        return incrementPluginStatistic(PLAYER_DATA_FOLDER, getUUID(p) + "", dataName);
    }

    public int incrementPluginStatistic(Player p, String dataName)
    {
        return incrementPluginStatistic(p.getName(), dataName);
    }

    public List<String> addDatumEntry(String folderName, String fileName, String dataName, String newDatum)
    {
        List<String> data = getData(folderName, fileName, dataName);
        data.add(newDatum);
        putData(folderName, fileName, dataName, data);
        return data;
    }

    public List<String> addPluginDatumEntry(String folderName, String fileName, String dataName, String newDatum)
    {
        return addDatumEntry(getPathOfPluginFolder(folderName), fileName, dataName, newDatum);
    }

    public List<String> addPluginDatumEntry(Player p, String dataName, String newDatum)
    {
        return addPluginDatumEntry(p.getName(), dataName, newDatum);
    }

    public List<String> addPluginDatumEntry(String p, String dataName, String newDatum)
    {
        return addPluginDatumEntry(PLAYER_DATA_FOLDER, getUUID(p), dataName, newDatum);
    }

    public int getStatistic(String folderName, String fileName, String dataName)
    {
        int stat;
        try
        {
            stat = Integer.valueOf(getDatum(folderName, fileName, dataName));
        }
        catch (NumberFormatException e)
        {
            stat = 0;
            putDatum(folderName, fileName, dataName, stat);
        }
        return stat;
    }

    public int getPluginStatistic(String folderName, String fileName, String dataName)
    {
        return getStatistic(getPathOfPluginFolder(folderName), fileName, dataName);
    }

    public int getPluginStatistic(String p, String dataName)
    {
        return getPluginStatistic(PLAYER_DATA_FOLDER, getUUID(p), dataName);
    }

    public int getPluginStatistic(Player p, String dataName)
    {
        return getPluginStatistic(p.getName(), dataName);
    }

    public File getDataFile(String folderName, String fileName)
    {
        return new File(getPathAndFileName(folderName, fileName));
    }

    public String getDatum(String folderName, String fileName, String dataName)
    {
        Scanner searcher = getScannerForFile(folderName, fileName);
        if (searcher == null)
        {
            return "";
        }
        while (searcher.hasNextLine())
        {
            String line = searcher.nextLine();
            if (line.startsWith(dataName + ": "))
            {
                return line.substring(dataName.length() + 2, line.length());
            }
        }
        return "";
    }

    public String getPluginDatum(String folderName, String fileName, String dataName)
    {
        return getDatum(getPathOfPluginFolder(folderName), fileName, dataName);
    }

    public String getPluginDatum(Player p, String dataName)
    {
        return getPluginDatum(p.getName(), dataName);
    }

    public String getPluginDatum(String p, String dataName)
    {
        return getPluginDatum(PLAYER_DATA_FOLDER, getUUID(p), dataName);
    }

    public List<ArrayList<String>> getAllData(String folderName, String fileName)
    {
        File f = getDataFile(folderName, fileName);
        Scanner lineReader = getScannerForFile(folderName, f);
        if (lineReader == null)
        {
            return null;
        }
        List<ArrayList<String>> dataList = new ArrayList<ArrayList<String>>();
        while (lineReader.hasNextLine())
        {
            Scanner lineScanner = new Scanner(lineReader.nextLine());
            if (lineScanner.hasNext(".*:"))
            {
                lineScanner.next(".*:");
                ArrayList<String> lineList = new ArrayList<String>();
                while (lineScanner.hasNext())
                {
                    lineList.add(lineScanner.next().replaceAll(",", ""));
                }
                dataList.add(lineList);
            }
        }
        return dataList;
    }

    public List<String> getData(String folderName, String fileName, String dataName)
    {
        return new ArrayList<String>(Arrays.asList(getDatum(folderName, fileName, dataName).split(", ")));
    }

    public List<String> getPluginData(String folderName, String fileName, String dataName)
    {
        return getData(getPathOfPluginFolder(folderName), fileName, dataName);
    }

    public List<String> getPluginData(Player p, String dataName)
    {
        return getPluginData(p.getName(), dataName);
    }

    public List<String> getPluginData(String p, String dataName)
    {
        return getPluginData(PLAYER_DATA_FOLDER, getUUID(p), dataName);
    }

    public boolean hasFile(String folderName, String fileName)
    {
        return (new File(getPathAndFileName(folderName, fileName)).exists());
    }

    public boolean hasPluginFile(String folderName, String fileName)
    {
        return hasFile(getPathOfPluginFolder(folderName), fileName);
    }

    public boolean playerHasFile(String playerName)
    {
        return hasPluginFile(PLAYER_DATA_FOLDER, getUUID(playerName));
    }

    public String getPathAndFileName(String folderName, String fileName)
    {
        return getPathOfFolder(folderName) + getFullFileName(fileName);
    }

    public void putDatum(String folderName, String fileName, String dataName, Object dataValue)
    {
        File f = getDataFile(folderName, fileName);
        Scanner lineReader = getScannerForFile(folderName, f);

        if (lineReader == null)
        {
            return;
        }
        List<String> lines = new ArrayList<String>();
        String newDataLine = dataName + ": " + dataValue;

        while (lineReader.hasNextLine())
        {
            String line = lineReader.nextLine();
            if (line.contains(dataName + ":"))
            {
                line = newDataLine;
            }
            lines.add(line);
        }
        if (!(lines.contains(newDataLine)))
        {
            lines.add(newDataLine);
        }

        printLinesToFile(f, lines);
    }

    public void putPluginDatum(String folderName, String fileName, String dataName, Object dataValue)
    {
        putDatum(getPathOfPluginFolder(folderName), fileName, dataName, dataValue);
    }

    public void putPluginDatum(String p, String dataName, Object dataValue)
    {
        putPluginDatum(PLAYER_DATA_FOLDER, getUUID(p), dataName, dataValue);
    }

    public void putPluginDatum(Player p, String dataName, Object dataValue)
    {
        putPluginDatum(p.getName(), dataName, dataValue);
    }

    public void putData(String folderName, String fileName, String dataName, List<String> data)
    {
        String dataAsString = "";
        for (int i = 0; i < data.size() - 1; i++)
        {
            dataAsString += data.get(i) + ", ";
        }
        dataAsString += data.get(data.size() - 1);
        putDatum(folderName, fileName, dataName, dataAsString);
    }

    public void putPluginData(String folderName, String fileName, String dataName, List<String> data)
    {
        putData(getPathOfPluginFolder(folderName), fileName, dataName, data);
    }

    public void putPluginData(Player p, String dataName, List<String> data)
    {
        putPluginData(p.getName(), dataName, data);
    }

    public void putPluginData(String p, String dataName, List<String> data)
    {
        putPluginData(PLAYER_DATA_FOLDER, getUUID(p), dataName, data);
    }

    public void printLinesToFile(String folderName, String fileName, List<String> dataList)
    {
        printLinesToFile(getDataFile(folderName, fileName), dataList);
    }

    private void printLinesToFile(File file, List<String> dataList)
    {
        PrintWriter printer;
        try
        {
            printer = new PrintWriter(file);
        }
        catch (FileNotFoundException e0) //couldn't find file (impossible)
        {
            Bukkit.getConsoleSender().sendMessage(loggerPrefix() + ChatColor.RED + "Could not find file " + file.getName() + "!");
            return;
        }
        int i;
        for (i = 0; i < dataList.size() - 1; i++)
        {
            printer.println(dataList.get(i));
        }
        if (dataList.size() > 0)
        {
            printer.print(dataList.get(i));
        }
        printer.close();
    }

    private Scanner getScannerForFile(String folderName, String fileName)
    {
        return getScannerForFile(folderName, getDataFile(folderName, fileName));
    }

    private Scanner getScannerForFile(String folderName, File file)
    {
        String path = getPathOfFolder(folderName);
        try
        {
            return new Scanner(file);
        }
        catch (FileNotFoundException e)
        {
            try //maybe the file was missing...
            {
                PrintWriter w = new PrintWriter(path + file.getName());
                w.close();

                return new Scanner(file);
            }
            catch (FileNotFoundException e1) //couldn't create file...
            {
                try //maybe the folder was missing...
                {
                    File f;
                    (f = new File(path)).mkdir();

                    PrintWriter w = new PrintWriter(path + file.getName());
                    w.close();

                    return new Scanner(file);
                }
                catch (FileNotFoundException e2) //couldn't create folder and put a file in it
                {
                    Bukkit.getConsoleSender().sendMessage(loggerPrefix() + ChatColor.BLUE + "Error in reading new file, " + file.getName() + "!");
                    return null;
                }
            }
        }
    }

    /**
     * Displays the help message that corresponds to the given command stored in the HashMap usages
     *
     * @param sender - The player who attempted to execute the command
     * @param label - The command itself as a string
     */
    //**********DATA MANAGEMENT ENDS HERE*************

    public void displayHelpMessage(CommandSender sender, String label)
    {
        if (getServer().getPluginCommand(label) == null)
        {
            sender.sendMessage(ChatColor.RED + "Error! Wrong syntax!");
            return;
        }
        sender.sendMessage(getServer().getPluginCommand(label).getUsage());
    }

    /**
     * Checks to make sure that the arguments only contains numbers
     * @param args - The arguments of the command that we would like to check for being valid
     * @return true if the argument only contains numeric characters
     */
    public boolean numericArgs(String args[])
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

    public boolean matchesCommand(String label, String commandName)
    {
        return label.equalsIgnoreCase(commandName) || getServer().getPluginCommand(commandName).getAliases().contains(label.toLowerCase());
    }

    public String loggerPrefix()
    {
        return "[" + ChatColor.BLUE + getDescription().getPrefix() + ChatColor.RESET + "] ";
    }

    public String getOrdinalIndicator(int number)
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
    //* Sends a simple string message to the command sender
    //* @param message - The command we would like to send
    //* @param sender - The command sender
    //private void messageSender(String message, CommandSender sender)
    //{
    //    if (isPlayer(sender))
    //    {
    //        Player p = (Player)sender;
    //        p.sendMessage(ChatColor.GOLD + message);
    //    }
    //    else
    //    {
    //        getLogger().info(message);
    //    }
    //}*/

    public String capitalize(String s)
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

    /**
     * Performs a specified command
     *
     * @param sender - The info of the player who sent the command
     * @param cmd - The command
     * @param label - The command in String form (a.k.a. cmd.getName())
     * @param args - The arguments of the command
     */
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        //boolean isPlayer = isPlayer(sender);
        if (label.equalsIgnoreCase("rlp"))
        {
            String command = "plugman reload Smash";
            Bukkit.dispatchCommand(getServer().getConsoleSender(), command);
            //p.performCommand(command); // Bukkit.dispatchCommand(sender, command);
            sender.sendMessage(ChatColor.GREEN + "Plugin reloaded!");
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
            lastSenders.put(messagedPlayer, sender);
            lastSenders.put(sender, messagedPlayer);
            return true;
        }
        else if (matchesCommand(label, REPLY_COMMAND))
        {
            if (args.length < 1)
            {
                return false;
            }
            else if (!lastSenders.containsKey(sender))
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
                CommandSender lastSender = lastSenders.get(sender);
                String message = ChatColor.GRAY + "<" + sender.getName() + " -> " + lastSender.getName() + "> " + contents;
                lastSender.sendMessage(message);
                sender.sendMessage(message);
            }
            return true;
        }
        return false;
    }
}
