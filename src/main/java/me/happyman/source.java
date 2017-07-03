package me.happyman;

import me.happyman.commands.*;
import me.happyman.Listeners.VerifierHandler;
import me.happyman.utils.*;
import me.happyman.worlds.PortalManager;
import me.happyman.worlds.SmashWorldManager;
import net.minecraft.server.v1_8_R3.EnumParticle;
import net.minecraft.server.v1_8_R3.PacketPlayOutWorldParticles;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.net.URL;
import java.util.*;
import java.text.SimpleDateFormat;

public class source extends JavaPlugin
{
    private static final String DATA_FILE_EXTENSION = ".json";

    private static final String MSG_COMMAND = "msg";
    private static final String REPLY_COMMAND = "r";
    private static final String JUMP_COMMAND = "jump";
    private static final String PLAY_PARTICLE_CMD = "playparticle";
    private static final char PLAYER_FILE_DELIMITER = ' ';
    private static final String PLAYER_DATA_PATH = "PlayerData";
    private static final String SERVER_DATA_PATH = "ServerData";
    private static HashMap<CommandSender, CommandSender> whoToReplyTo;

    @Override
    public void onEnable()
    {
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable()
        {
            public void run()
            {
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
        }, 20);



        //EventHandler
        //There's really no point in creating permission nodes inside of here... or is there?
        //getServer().getPluginManager().addPermission(new Permission("awesomeallowed"));
        //https://code.google.com/archive/p/reflections/
        setExecutor(MSG_COMMAND, this);
        setExecutor(REPLY_COMMAND, this);
        setExecutor(JUMP_COMMAND, this);
        setExecutor(PLAY_PARTICLE_CMD, this);
        new Broadcast(this);
        new DisableWeather(this);
        new HealAndDamage(this);
        new ForcefieldManager(this);
        new TogglePvP(this);
        new SoupManager(this);
        new VerifierHandler(this);
        new SmashManager(this);
        new FTPAccessor(this);
        new PortalManager(this);
        new VelocityModifier(this);
        whoToReplyTo = new HashMap<CommandSender, CommandSender>();
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
        FTPAccessor.saveProfile("HappyMan");
    }

    @Override
    public void onDisable()
    {
        SmashWorldManager.performDisable();
        saveConfig();
        Bukkit.getConsoleSender().sendMessage(ChatColor.RED + getDescription().getPrefix() + " by HappyMan disabled!");
    }
    //Uncomment the method body if you Run into trouble with your Runnables
    public static void startRunnable(Runnable r)
    {
        //Thread thread = new Thread(r);
        //thread.start();
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
            if (player instanceof Player && player.getName().equals(name))
            {
                return (Player)player;
            }
        }*/
        return null;
    }

    public void createJSONFiles(String directory)
    {
        try
        {
            File dir = new File(directory);
            File [] fileList = dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    return s.endsWith(".txt");
                }
            });
            HashMap<File, String> dataToPrint = new HashMap<File, String>();
            for (File f : fileList)
            {
                Bukkit.broadcastMessage(f.getName());
                String contents = new JSONObject(getDataEntriesSimple(f)).toString();
                File newJsonFile = new File(f.getAbsolutePath().substring(0, f.getAbsolutePath().length() - ".txt".length()) + ".json");
                if (!dataToPrint.containsKey(newJsonFile) && !newJsonFile.exists())
                {
                    dataToPrint.put(newJsonFile, contents);
                }
            }
            for (File f : dataToPrint.keySet())
            {
                PrintWriter w = new PrintWriter(f);
                w.print(dataToPrint.get(f));
                w.close();
            }


            for (File f : dir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File file, String s) {
                    return file.isDirectory();
                }
            }))
            {
                if (f.isDirectory())
                {
                    createJSONFiles(f.getAbsolutePath());
                }
            }
        }
        catch (NullPointerException ex)
        {
            sendErrorMessage(ChatColor.RED + "Error! Found a nullpointer!");
            ex.printStackTrace();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

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

    public void setExecutor(String cmdLabel, CommandExecutor executor)
    {
        PluginCommand cmd = getCommand(cmdLabel);
        cmd.setExecutor(executor);
        for (String s : cmd.getAliases())
        {
            getCommand(s).setExecutor(executor);
        }
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

    public String getUUID(final String p)
    {
        if (p.length() > 20)
        {
            return p;
        }
        if (Bukkit.getPlayer(p) != null)
        {
            return Bukkit.getPlayer(p).getUniqueId().toString();
        }
        File[] listOfPlayersFiles = getSpecificFile(DirectoryType.PLAYER_DATA, "", "").listFiles(new FilenameFilter()
        {
            @Override
            public boolean accept(File file, String s)
            {
                return s.toLowerCase().startsWith(p.toLowerCase()) && isValidPlayerFileName(s);
            }
        });
        if (listOfPlayersFiles.length > 0)
        {
            String fileName = listOfPlayersFiles[0].getName();
            return fileName.substring(fileName.indexOf(PLAYER_FILE_DELIMITER) + 1, fileName.length() - DATA_FILE_EXTENSION.length());
        }
        try
        {
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
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            sendErrorMessage("Error! Could not get unique id for " + p);
        }
        return null;
    }

    private Scanner getMojangApiScanner(String playerName) throws IOException
    {
            //JSONObject obj = (JSONObject)(new JSONParser()).parse((new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName)).openConnection().getInputStream().toString());
            //Bukkit.broadcastMessage((new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName)).openConnection().getInputStream().toString());

        Scanner s = null;
        if (playerName.length() > 20)
        {
            sendErrorMessage("Error! We cannot accept a UUID input for the Mojang API scanner!");
            throw new IOException();
        }

        s = new Scanner(new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName).openConnection().getInputStream());
        s.useDelimiter("\"");
        return s;
       /* catch (ParseException ex)
        {
            sendErrorMessage("Error! Failed to parse JSON when getting UUID.");
        }*/
    }

    public String getCapitalName(final String name, final String uuid)
    {
        try
        {
            if (uuid != null && uuid.length() > 16)
            {
                File[] listOfPlayersFiles = getSpecificFile(DirectoryType.PLAYER_DATA, "", "").listFiles(new FilenameFilter()
                {
                    @Override
                    public boolean accept(File file, String s)
                    {
                        return s.toLowerCase().contains(uuid.toLowerCase());
                    }
                });

                if (listOfPlayersFiles.length > 0)
                {
                    String fileName = listOfPlayersFiles[0].getName();
                    String fetchedName = getDatum(DirectoryType.SMASH_DATA, PLAYER_DATA_PATH, fileName, SmashStatTracker.NAME_DATANAME);
                    if (!fetchedName.equals(""))
                    {
                        return fetchedName;
                    }
                }
            }

            if (Bukkit.getPlayer(name) != null)
            {
                return Bukkit.getPlayer(name).getName();
            }

            if (name.length() <= 16)
            {
                File[] listOfPlayersFiles = getSpecificFile(DirectoryType.PLAYER_DATA, "", "").listFiles(new FilenameFilter()
                {
                    @Override
                    public boolean accept(File file, String s)
                    {
                        return s.toLowerCase().startsWith(name.toLowerCase()) && isValidPlayerFileName(s);
                    }
                });
                if (listOfPlayersFiles.length > 0)
                {
                    String fileName = listOfPlayersFiles[0].getName();
                    return fileName.substring(0, fileName.indexOf(PLAYER_FILE_DELIMITER));
                }
            }

            Scanner s = getMojangApiScanner(name);
            if (s != null)
            {
                while (s.hasNext())
                {
                    if (s.next().contains("name") && s.hasNext())
                    {
                        String capitalName = s.next();
                        while (capitalName.length() == 0 || capitalName.contains(":"))
                        {
                            if (!s.hasNext())
                            {
                                sendErrorMessage("Error! Could not get Capital name!");
                                return capitalName;
                            }
                            capitalName = s.next();
                        }
                        return capitalName;
                    }
                }
                sendErrorMessage("Error! Could not get capital name for " + name);
            }
        }
        catch (IOException ex)
        {
            sendErrorMessage(ex.getMessage());
            ex.printStackTrace();
        }
        return name;
    }

    public static String makeForwardAndSandwichInSlashes(String relativePath)
    {
        relativePath = relativePath.replaceAll("\\\\", "/");
        return ((relativePath.length() > 0 && relativePath.charAt(0) != '/' ? "/": "") + relativePath + "/");
    }

    public String getAbsolutePath(DirectoryType dir, String relativePath)
    {
        relativePath = makeForwardAndSandwichInSlashes(relativePath);
        String path;
        if (dir.equals(DirectoryType.SMASH_DATA))
        {
            path = getDataFolder().getAbsolutePath() + relativePath;
        }
        else if (dir.equals(DirectoryType.PLAYER_DATA))
        {
            path = getDataFolder().getAbsolutePath() + "/" + PLAYER_DATA_PATH + relativePath;
        }
        else if (dir.equals(DirectoryType.SERVER_DATA))
        {
            path = getDataFolder().getAbsolutePath() + "/" + SERVER_DATA_PATH + relativePath;
        }
        else
        {
            path = getDataFolder().getAbsolutePath().substring(0, getDataFolder().getAbsolutePath().length() - (getDataFolder().getPath().length() + 1)) + relativePath;
        }

        return path;
    }

    public boolean hasFile(String player)
    {
        return hasFile(DirectoryType.PLAYER_DATA, "", getPlayerFileName(player));
    }

    private boolean isValidPlayerFileName(String fileName)
    {
        if (fileName.endsWith(DATA_FILE_EXTENSION))
        {
            fileName = fileName.substring(0, fileName.length() - DATA_FILE_EXTENSION.length());
        }
        int spaceindex = fileName.indexOf(' ');
        return fileName.contains("" + PLAYER_FILE_DELIMITER) && spaceindex == fileName.lastIndexOf(PLAYER_FILE_DELIMITER) && fileName.length() - (spaceindex + 1) == 36;
    }

    public File getSpecificFile(DirectoryType dir, String relativePath, String fileName)
    {
        if (!fileName.contains(".") && fileName.length() > 0)
        {
            fileName = fileName + DATA_FILE_EXTENSION;
        }
        File result = new File(getAbsolutePath(dir, relativePath) + fileName);
        if (dir.equals(DirectoryType.PLAYER_DATA) && fileName != null && fileName.length() > 0)
        {
            final String extensionlessFileName = fileName.substring(0, fileName.length() - DATA_FILE_EXTENSION.length());
            try
            {
                if (!isValidPlayerFileName(extensionlessFileName))
                {
                    throw new IOException("Error! Tried to create an invalid player file... \"" + fileName + "\".");
                }

                FilenameFilter filter = new FilenameFilter()
                {
                    @Override
                    public boolean accept(File file, String s)
                    {
                        return !s.startsWith(extensionlessFileName) && s.contains(extensionlessFileName.substring(extensionlessFileName.indexOf(PLAYER_FILE_DELIMITER) + 1, extensionlessFileName.length()));
                    }
                };
                final File[] extraFileList = getSpecificFile(dir, relativePath, "").listFiles(filter); //list of files that have the uuid
                if (extraFileList.length > 0)
                {
                    if (!result.exists())
                    {
                        if (result.createNewFile())
                        {
                            List<String> lines = new ArrayList<String>();
                            Scanner stream = new Scanner(extraFileList[0]);
                            while (stream.hasNextLine())
                            {
                                lines.add(stream.nextLine());
                            }
                            PrintWriter str = new PrintWriter(result);
                            while (!lines.isEmpty())
                            {
                                str.println(lines.get(0));
                                lines.remove(0);
                            }
                            str.close();
                        }
                        else
                        {
                            throw new IOException("Error! Was not able to create " + result.getName() + "!");
                        }
                    }

                    final File finalResult = result;
                    Bukkit.getScheduler().scheduleSyncDelayedTask(this, new Runnable()
                    {
                        public void run()
                        {
                            for (int i = 0; i < extraFileList.length; i++)
                            {
                                File fileToDelete = new File(extraFileList[i].getAbsolutePath());
                                if (!fileToDelete.getAbsolutePath().equals(finalResult.getAbsolutePath()))
                                {
                                    fileToDelete.delete();
                                }
                            }
                        }
                    }, 10);
                }
            }
            catch (IOException ex)
            {
                sendErrorMessage(ex.getMessage());
                for (StackTraceElement elt : ex.getStackTrace())
                {
                    sendErrorMessage(elt.toString());
                }
            }
        }
        return result;

    }

    public File getTextFileSimple(DirectoryType dir, String relativePath, final String fileName)
    {
        if (!fileName.endsWith(".txt"))
        {
            return getSpecificFile(dir, relativePath, fileName + ".txt");
        }
        return getSpecificFile(dir, relativePath, fileName);
    }

    public void clearFile(DirectoryType dir, String relativePath, String fileName)
    {
        printLinesToFile(getSpecificFile(dir, relativePath, fileName), new ArrayList<String>());
    }

    public void deleteFile(DirectoryType dir, String relativePath, String fileName)
    {
        File f = getSpecificFile(dir, relativePath, fileName);
        if (f.exists() && !f.delete())
        {
            sendErrorMessage("Unable to delete file " + fileName + " in " + relativePath + "!");
        }
    }

    public HashMap<String, String> getDataEntriesSimple(DirectoryType dir, String relativePath, String fileName)
    {
        return getDataEntriesSimple(getSpecificFile(dir, relativePath, fileName));
    }

    public HashMap<String, String> getDataEntriesSimple(File f)
    {
        try
        {
            Scanner keyReader = new Scanner(f);
            HashMap<String, String> entries = new HashMap<String, String>();
            while (keyReader.hasNextLine())
            {
                String line = keyReader.nextLine();
                int colonIndex = line.indexOf(':');

                int firstNonSpace;
                for (firstNonSpace = colonIndex + 1; firstNonSpace < line.length() && line.charAt(firstNonSpace) == ' '; firstNonSpace++);
                if (colonIndex != -1 && firstNonSpace != line.length())
                {
                    entries.put(line.substring(0, colonIndex), line.substring(firstNonSpace, line.length()));
                }
            }
            return entries;
        }
        catch (IOException ex) {}
        return null;
    }

    public void renameAllData(String oldName, String newName)
    {
        changeFolderDataName(DirectoryType.ROOT, newName, "", oldName);
    }

    public void renameData(DirectoryType dir, String relativePath, String oldName, String newName)
    {
        changeFolderDataName(dir, relativePath, oldName, newName);
    }

    public void removeAllData(DirectoryType dir, String dataName)
    {
        changeFolderDataName(dir, "", dataName, null);
    }

    public void removeData(DirectoryType dir, String path, String dataName)
    {
        changeFolderDataName(dir, path, dataName, null);
    }

    private void changeFileDataName(File file, String oldName, String newName)
    {
        changeFileData(file, oldName, newName, false);
    }

    private void changeFileData(File file, String oldName, String newName, boolean changeKeys)
    {
        FileReader reader = null;
        try
        {
            if (file.getName().endsWith(DATA_FILE_EXTENSION))
            {
                reader = new FileReader(file);
                JSONObject jObj = (JSONObject)new JSONParser().parse(reader);
                if (jObj.containsKey(oldName))
                {
                    if (changeKeys)
                    {
                        Object value = jObj.get(oldName);
                        jObj.remove(oldName);
                        if (newName != null)
                        {
                            jObj.put(newName, value);
                        }
                    }
                    else
                    {
                        Set<Map.Entry<String, String>> entries = jObj.entrySet();
                        for (Map.Entry<String, String> entry : entries)
                        {
                             if (entry.getValue().contains(oldName))
                             {
                                 if (newName != null)
                                 {
                                     jObj.put(entry.getKey(), entry.getValue().replaceAll(oldName, newName));
                                 }
                                 else
                                 {
                                     jObj.put(entry.getKey(), entry.getValue().replaceAll(oldName, ""));
                                 }
                             }
                        }
                    }
                    PrintWriter writer = new PrintWriter(file);
                    jObj.writeJSONString(writer);
                    writer.close();
                }
//            List<String> dataList;
//            try
//            {
//                Scanner scanner = new Scanner(file);
//                dataList = new ArrayList<String>();
//                while (scanner.hasNextLine())
//                {
//                    dataList.add(scanner.nextLine());
//                }
//                for (int i = 0; i < dataList.size(); i++)
//                {
//                    if (!changeKeys)
//                    {
//                        if (dataList.get(i).contains(oldName + ":"))
//                        {
//                            if (newName == null)
//                            {
//                                dataList.remove(i);
//                                i--;
//                            }
//                            else
//                            {
//                                String s = dataList.get(i);
//                                s = s.replaceAll(oldName, newName);
//                                dataList.remove(i);
//                                dataList.add(i, s);
//                            }
//                        }
//                    }
//                    else
//                    {
//                        String s = dataList.get(i);
//                        s = s.replaceAll(oldName, newName);
//                        dataList.remove(i);
//                        dataList.add(i, s);
//                    }
//                }
//
//                printLinesToFile(file, dataList);
//            }
//            catch (FileNotFoundException e)
//            {
//                sendErrorMessage("Error! Tried to rewrite data in " + file.getName() + ", but it was missing!");
//            }
            }
        }
        catch (ParseException ex)
        {
            ex.printStackTrace();
        }
        catch (FileNotFoundException ex)
        {
            ex.printStackTrace();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        if (reader != null)
        {
            try
            {
                reader.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    private void changeFolderDataName(DirectoryType dir, String relativePath, String oldName, String newName)
    {
        changeFolderData(dir, relativePath, oldName, newName, false);
    }

    private void changeFolderData(DirectoryType dir, String relativePath, String oldName, String newName, boolean changeKeys)
    {
        changeFolderData(getSpecificFile(dir, relativePath, ""), oldName, newName, changeKeys);
    }

    private void changeFolderData(File dir, String oldName, String newName, boolean changeKeys)
    {
        for (File containedFile : dir.listFiles())
        {
            if (containedFile.isFile())
            {
                changeFileData(containedFile, oldName, newName, changeKeys);
            }
            else if (containedFile.isDirectory())
            {
                changeFolderData(containedFile, oldName, newName, changeKeys);
            }
        }
    }

    public void deleteFolder(File directory)
    {
        if (directory != null && directory.listFiles() != null)
        {
            for (File innerFile : directory.listFiles())
            {
                deleteFolder(innerFile);
            }
        }
        directory.delete();
    }

    public int incrementStatistic(DirectoryType dir, String relativePath, String fileName, String dataName)
    {
        int dataValue;
        try
        {
            dataValue = Integer.valueOf(getDatum(dir, relativePath, fileName, dataName));
        }
        catch (NumberFormatException e)
        {
            dataValue = 0;
        }
        int incrementation = dataValue + 1;
        putDatum(dir, relativePath, fileName, dataName, incrementation);
        return incrementation;
    }

    public String getPlayerFileName(String playerUsername)
    {
        try
        {
            if (playerUsername.length() > 16)
            {
                if (isValidPlayerFileName(playerUsername))
                {
                    return playerUsername;
                }
                else
                {
                    throw new Exception("Error! Attempted to create an improperly formatted player file, " + playerUsername + "!");
                }
            }
            String uuid = getUUID(playerUsername);
            return getCapitalName(playerUsername, uuid) + PLAYER_FILE_DELIMITER + uuid;
        }
        catch (Exception ex)
        {
            sendErrorMessage(ex.getMessage());
            for (StackTraceElement elt : ex.getStackTrace())
            {
                sendErrorMessage(elt.toString());
            }
        }
        return null;
    }

    public int incrementStatistic(String playerUsername, String dataName)
    {
        return incrementStatistic(DirectoryType.PLAYER_DATA, "", getPlayerFileName(playerUsername), dataName);
    }

    public int incrementStatistic(Player p, String dataName)
    {
        return incrementStatistic(p.getName(), dataName);
    }

    public List<String> addDatumEntry(DirectoryType dir, String relativePath, String fileName, String dataName, String newDatum)
    {
        List<String> data = getData(dir, relativePath, fileName, dataName);
        data.add(newDatum);
        putData(dir, relativePath, fileName, dataName, data);
        return data;
    }

    public List<String> addDatumEntry(Player p, String dataName, String newDatum)
    {
        return addDatumEntry(DirectoryType.PLAYER_DATA, "", getPlayerFileName(p.getName()), dataName, newDatum);
    }

    public List<String> addDatumEntry(String player, String dataName, String newDatum)
    {
        return addDatumEntry(DirectoryType.PLAYER_DATA, "", getPlayerFileName(player), dataName, newDatum);
    }

    public int getStatistic(DirectoryType dir, String relativePath, String fileName, String dataName)
    {
        int stat;
        try
        {
            stat = Integer.valueOf(getDatum(dir, relativePath, fileName, dataName));
        }
        catch (NumberFormatException e)
        {
            stat = 0;
            putDatum(dir, relativePath, fileName, dataName, stat);
        }
        return stat;
    }

    public int getStatistic(Player p, String dataName)
    {
        return getStatistic(p.getName(), dataName);
    }

    public int getStatistic(String playerUsername, String dataName)
    {
        return getStatistic(DirectoryType.PLAYER_DATA, "", getPlayerFileName(playerUsername), dataName);
    }

    public String getDatumSimple(DirectoryType dir, String relativePath, String fileName, String dataName)
    {
        Scanner searcher = getScannerForFile(dir, relativePath, fileName);
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

    public String getDatum(DirectoryType dir, String relativePath, String fileName, String dataName)
    {
        String result = "";
        FileReader reader = null;
        try
        {
            reader = new FileReader(getSpecificFile(dir, relativePath, fileName));
            JSONObject obj = (JSONObject)new JSONParser().parse(reader);
            if (obj.containsKey(dataName))
            {
                result = obj.get(dataName).toString();
            }
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        if (reader != null)
        {
            try
            {
                reader.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return result;
    }

    public String getDatum(Player p, String dataName)
    {
        return getDatum(p.getName(), dataName);
    }

    public String getDatum(String playerUsername, String dataName)
    {
        return getDatum(DirectoryType.PLAYER_DATA, "", getPlayerFileName(playerUsername), dataName);
    }

    /*
     * Gets a list of all of the values
     */
    public List<ArrayList<String>> getAllDataSimple(DirectoryType dir, String relativePath, String fileName)
    {
        File f = getSpecificFile(dir, relativePath, fileName);
        Scanner lineReader = getScannerForFile(dir, relativePath, f);
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

    public List<String> getData(DirectoryType dir, String relativePath, String fileName, String dataName)
    {
        return Arrays.asList(getDatum(dir, relativePath, fileName, dataName).split(", "));
    }

    public List<String> getData(Player p, String dataName)
    {
        return getData(p.getName(), dataName);
    }

    private List<String> getData(String playerUsername, String dataName)
    {
        return getData(DirectoryType.PLAYER_DATA, "", getPlayerFileName(playerUsername), dataName);
    }

    public boolean hasFile(DirectoryType dir, String relativePath, String fileName)
    {
        if (!fileName.endsWith(DATA_FILE_EXTENSION) && !fileName.contains("."))
        {
            fileName = fileName + DATA_FILE_EXTENSION;
        }
        return getSpecificFile(dir, relativePath, fileName).exists();
    }

    public void putDatum(DirectoryType dir, String folderName, String fileName, String dataName, Object dataValue)
    {
        File f = getSpecificFile(dir, folderName, fileName);
        FileReader reader = null;
        try
        {
            reader = new FileReader(f);
            JSONObject jsonObj = (JSONObject)new JSONParser().parse(reader);
            jsonObj.put(dataName, dataValue);
            PrintWriter writer = new PrintWriter(f);
            jsonObj.writeJSONString(writer);
            writer.close();
        }
        catch (FileNotFoundException ex)
        {
            sendErrorMessage(ChatColor.RED + "File " + f.getName() + " not found!!");
        }
        catch (ParseException ex)
        {
            sendErrorMessage(ChatColor.RED + "Error! File " + f.getName() + " had an invalid json format!");
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        if (reader != null)
        {
            try
            {
                reader.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        /*Scanner lineReader = getScannerForFile(dir, folderName, f);

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

        printLinesToFile(f, lines);*/
    }

    public void putDatum(Player p, String datumName, Object dataValue)
    {
        putDatum(p.getName(), datumName, dataValue);
    }

    public void putDatum(String playerUsername, String datumName, Object dataValue)
    {
        putDatum(DirectoryType.PLAYER_DATA, "", getPlayerFileName(playerUsername), datumName, dataValue);
    }

    public void putPluginDatum(DirectoryType dir, String relativePath, String fileName, String dataName, Object dataValue)
    {
        putDatum(dir, relativePath, fileName, dataName, dataValue);
    }

    public void putDataSimple(DirectoryType dir, String relativePath, String fileName, String dataName, List<String> data)
    {
        String dataAsString = "";
        for (int i = 0; i < data.size() - 1; i++)
        {
            dataAsString += data.get(i) + ", ";
        }
        dataAsString += data.get(data.size() - 1);
        putDatum(dir, relativePath, fileName, dataName, dataAsString);
    }

    public void putData(DirectoryType dir, String relativePath, String fileName, String dataName, List<String> data)
    {
        File f = getSpecificFile(dir, relativePath, fileName);
        FileReader reader = null;
        try
        {
            reader = new FileReader(f);
            JSONObject obj = (JSONObject)new JSONParser().parse(reader);
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        if (reader != null)
        {
            try
            {
                reader.close();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

    public void printLinesToFile(DirectoryType dir, String relativePath, String fileName, List<String> dataList)
    {
        printLinesToFile(getSpecificFile(dir, relativePath, fileName), dataList);
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

    private Scanner getScannerForFile(DirectoryType dir, String relativePath, String fileName)
    {
        return getScannerForFile(dir, relativePath, getSpecificFile(dir, relativePath, fileName));
    }

    private Scanner getScannerForFile(DirectoryType dir, String relativePath, File file)
    {
        String path = getAbsolutePath(dir, relativePath);
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
                    new File(path).mkdir();
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
            sender.sendMessage(ChatColor.GREEN + "Plugin reloading!");
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
                Player p;
                String name;
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
                else
                {
                     p = (Player)sender;
                     name = "you";
                }
                sender.sendMessage(ChatColor.BLUE + "Jumping " + name + " up " + blocksUpToGo + " blocks!");

                p.setVelocity(new Vector(0, VelocityModifier.computeVyInitial(blocksUpToGo), 0));
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
