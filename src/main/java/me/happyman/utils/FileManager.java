package me.happyman.utils;

import me.happyman.Plugin;
import me.happyman.worlds.UUIDFetcher;
import org.apache.commons.io.FileUtils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.util.*;

import static me.happyman.worlds.UUIDFetcher.getUUID;
import static me.happyman.worlds.UUIDFetcher.getUUIDAndCapitalName;

//**********DATA MANAGEMENT STARTS HERE*************
public class FileManager
{
    private static final String DATA_FILE_EXTENSION = ".json";
    private static final char PLAYER_FILE_DELIMITER = ' ';
    private static final String WORLD_PLAYER_FOLDER_PATH = "playerdata";
    private static final String ROOT_FOLDER_ABS_PATH_WITH_SLASH;
    private static final String PLAYER_DATA_PATH;
    private static final String SERVER_DATA_PATH;
    private static final List<File> filesBeingRenamed = new ArrayList<File>();

    static
    {
        ROOT_FOLDER_ABS_PATH_WITH_SLASH = Plugin.getPlugin().getDataFolder().getAbsolutePath().substring(0, Plugin.getPlugin().getDataFolder().getAbsolutePath().length() - Plugin.getPlugin().getDataFolder().getPath().length()).replaceAll("\\\\", "/"); //dataFolderAbsPath.length() == 0f ? "" : dataFolderAbsPath.substring(0, dataFolderAbsPath.length() - (dataFolderPath.length() + 1));
        final String dataFolderPath = Plugin.getPlugin().getDataFolder().getPath().replaceAll("\\\\", "/");
        PLAYER_DATA_PATH = dataFolderPath + "/PlayerData";
        SERVER_DATA_PATH = dataFolderPath + "/ServerData";
    }

    public static File getSimplePlayerDataFile(String p, World world)
    {
        return getPlayerFile(p, world, "", "PublicData.json", true);
    }

    public static File getSimplePlayerDataFile(Player p, World w)
    {
        return getSimplePlayerDataFile(UUIDFetcher.getUUID(p), w);
    }

    public static File getSimplePlayerDataFile(Player p)
    {
        return getSimplePlayerDataFile(UUIDFetcher.getUUID(p), p.getWorld());
    }

    public static File getSpecificFile(File parentFolder, String fileName, boolean forceValidity)
    {
        return getSpecificFile(parentFolder, "", fileName, forceValidity);
    }

    public static File getSpecificFile(File parentFolder, String relativePath, String fileName, boolean forceValidity)
    {
        File result = null;
        if ((parentFolder.exists() || forceValidity && parentFolder.mkdirs()) && (relativePath.length() == 0))
        {
            File[] files =  parentFolder.listFiles();
            if (files != null)
            {
                for (File f : files)
                {
                    if (f.getName().equals(fileName))
                    {
                        result = f;
                        break;
                    }
                }
            }
        }
        if (result == null)
        {
            relativePath = desandwichPathInSlashes(relativePath);
            result = new File(parentFolder.getAbsolutePath() + '/' + relativePath + (relativePath.length() == 0 ? "" : '/') + fileName);
        }
        if (forceValidity)
        {
            forceValidity(result);
        }
        return result;
    }

    private static File getSpecificFile(File parentFolder, String fileName)
    {
        return getSpecificFile(parentFolder, fileName, true);
    }

    public static File getSpecificFile(String relativePath, String fileName)
    {
        return getSpecificFile(relativePath, fileName, true);
    }

    public static File getServerDataFile(String relativePath, String fileName, boolean forceValid)
    {
        return getSpecificFile(SERVER_DATA_PATH, relativePath, fileName, forceValid);
    }

    private static File getSpecificFile(String path1, String path2, String fileName, boolean forceValidity)
    {
        if (fileName == null)
        {
            return null;
        }
        path2 = desandwichPathInSlashes(path2);//@TODO: should we check for length 0 of root?
        File result = new File(ROOT_FOLDER_ABS_PATH_WITH_SLASH + path1 + (path1.length() == 0 ? "" : '/') + path2 + (path2.length() == 0 ? "" : '/') + fileName +
            (fileName.length() != 0 && !fileName.contains(".") ? DATA_FILE_EXTENSION : ""));
        if (forceValidity)
        {
            forceValidity(result);
        }
        return result;
    }

    public static File getRoot()
    {
        return getSpecificFile("", "", false);
    }

    private static File getSpecificFile(String relativePath, String fileName, boolean forceValidity)
    {
        return getSpecificFile("", relativePath, fileName, forceValidity);
    }

    public static String desandwichPathInSlashes(String relativePath)
    {
        relativePath = relativePath.replaceAll("\\\\", "/");
        if (relativePath.length() != 0 && relativePath.charAt(0) == '/')
        {
            relativePath = relativePath.substring(1, relativePath.length());
            if (relativePath.length() != 0 && relativePath.charAt(relativePath.length() - 1) == '/')
            {
                relativePath = relativePath.substring(0, relativePath.length() - 1);
            }
        }
        return relativePath;
    }

    private static void forceValidity(File result)
    {
        if (result != null && !result.isDirectory())
        {
            try
            {
                boolean good = result.exists();
                if (!good)
                {
                    result.getParentFile().mkdirs();
                    good = result.createNewFile();
                }

                if (good && result.getName().endsWith(".json"))
                {
                    FileInputStream inputStream = new FileInputStream(result);
                    int read = inputStream.read();
                    inputStream.close();
                    if (read == -1)
                    {
                        FileOutputStream outputStream = new FileOutputStream(result);
                        outputStream.write('{');
                        outputStream.write('}');
                        outputStream.close();
                    }
                }
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
    }

//    public static void syncPlayerFile(File newerFile)
//    {
//        String fileName = newerFile.getName();
//        //check player file against others
//        if (newerFile.exists() && fileName.length() > 0 && newerFile.isFile())
//        {
//            try
//            {
//                final String extensionlessFileName = fileName.substring(0, fileName.length() - DATA_FILE_EXTENSION.length());
//                if (!isValidPlayerFileName(extensionlessFileName))
//                {
//                    throw new IOException("Error! Tried to create an invalid player file... \"" + fileName + "\".");
//                }
//
//                FilenameFilter filter = new FilenameFilter()
//                {
//                    @Override
//                    public boolean accept(File file, String s)
//                    {
//                        return !s.startsWith(extensionlessFileName) && s.contains(extensionlessFileName.substring(extensionlessFileName.indexOf(PLAYER_FILE_DELIMITER) + 1, extensionlessFileName.length()));
//                    }
//                };
//                final File[] extraFileList = newerFile.getParentFile().listFiles(filter); //list of files that have the uuid
//                if (extraFileList != null && extraFileList.length > 0)
//                {
//                    if (!newerFile.exists())
//                    {
//                        if (newerFile.createNewFile())
//                        {
//                            List<String> lines = new ArrayList<String>();
//                            Scanner stream = new Scanner(extraFileList[0]);
//                            while (stream.hasNextLine())
//                            {
//                                lines.add(stream.nextLine());
//                            }
//                            stream.close();
//                            PrintWriter str = new PrintWriter(newerFile);
//                            while (!lines.isEmpty())
//                            {
//                                str.println(lines.get(0));
//                                lines.remove(0);
//                            }
//                            str.close();
//                        }
//                        else
//                        {
//                            throw new IOException("Error! Was not able to create " + newerFile.getName() + "!");
//                        }
//                    }
//
//                    final File finalResult = newerFile;
//                    Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin.getPlugin(), new Runnable()
//                    {
//                        public void run()
//                        {
//                            for (File f : extraFileList)
//                            {
//                                File fileToDelete = new File(f.getAbsolutePath());
//                                if (!fileToDelete.getAbsolutePath().equals(finalResult.getAbsolutePath()))
//                                {
//                                    fileToDelete.delete();
//                                }
//                            }
//                        }
//                    }, 10);
//                }
//            }
//            catch(IOException ex)
//            {
//                ex.printStackTrace();
//            }
//        }
//    }

    //    public HashMap<String, String> getDataEntriesSimple(DirectoryType dir, String relativePath, String fileName)
//    {
//        return getDataEntriesSimple(getSpecificFile(relativePath, fileName));
//    }
//
//    public HashMap<String, String> getDataEntriesSimple(File f)
//    {
//        try
//        {
//            Scanner keyReader = new Scanner(f);
//            HashMap<String, String> entries = new HashMap<String, String>();
//            while (keyReader.hasNextLine())
//            {
//                String line = keyReader.nextLine();
//                int colonIndex = line.indexOf(':');
//
//                int firstNonSpace;
//                for (firstNonSpace = colonIndex + 1; firstNonSpace < line.length() && line.charAt(firstNonSpace) == ' '; firstNonSpace++);
//                if (colonIndex != -1 && firstNonSpace != line.length())
//                {
//                    entries.put(line.substring(0, colonIndex), line.substring(firstNonSpace, line.length()));
//                }
//            }
//            keyReader.close();
//            return entries;
//        }
//        catch (IOException ex) {}
//        return null;
//    }

//    private static String tryGettingNameFromFirstItemInFileList(File[] fileList)
//    {
//        if (fileList == null)
//        {
//            Plugin.sendErrorMessage("Error! Got a INVALID list of player files!");
//        }
//        else if (fileList.length > 0)
//        {
//            String fileName = fileList[0].getName();
//            int lengthOfName = 0;
//            for (; lengthOfName < fileName.length() && Character.isLetterOrDigit(fileName.charAt(lengthOfName)); lengthOfName++);
//            if (lengthOfName <= 16 && lengthOfName > 0)
//            {
//                return fileName.substring(0, lengthOfName);
//            }
//        }
//        return null;
//    }

//    public static String getAbsolutePath(DirectoryType dir, String relativePath)
//    {
//        relativePath = makeIntoAbsolute(relativePath);
//        final String path;
//        switch (dirif (dir.equals(DirectoryType.PLAYER_DATA))
//        {
//            path = dataFolderPath + "/" + PLAYER_DATA_PATH + relativePath;
//        }
//        else if (dir.equals(DirectoryType.SERVER_DATA))
//        {
//            path = dataFolderPath + "/" + SERVER_DATA_PATH + relativePath;
//        }
//        else
//        {
//            path = dataFolderPath.substring(0, dataFolderPath.length() - (Plugin.getPlugin().getDataFolder().getPath().length() + 1)) + relativePath;
//        }
//        return path;
//    }

    public static boolean hasFile(String relativePath, String fileName)
    {
        if (!fileName.endsWith(DATA_FILE_EXTENSION) && !fileName.contains("."))
        {
            fileName = fileName + DATA_FILE_EXTENSION;
        }
        return getSpecificFile(relativePath, fileName).exists();
    }

    private static boolean isValidPlayerFileName(String fileName)
    {
        if (fileName.endsWith(DATA_FILE_EXTENSION))
        {
            fileName = fileName.substring(0, fileName.length() - DATA_FILE_EXTENSION.length());
        }
        int spaceindex = fileName.indexOf(' ');
        return fileName.contains("" + PLAYER_FILE_DELIMITER) && spaceindex == fileName.lastIndexOf(PLAYER_FILE_DELIMITER) && fileName.length() - (spaceindex + 1) == 32;
    }

    public static File getWorldFile(World w, String fileName)
    {
        return getWorldFile(w, "", fileName);
    }

    public static File getWorldFile(World w, String relativePath, String fileName)
    {
        return getWorldFile(w.getName(), relativePath, fileName);
    }

    public static File getWorldFile(String world, String relativePath, String fileName)
    {
        return getWorldFile(world, relativePath, fileName, true);
    }

    public static File getWorldFile(World world, String relativePath, String fileName, boolean forceValid)
    {
        return getWorldFile(world.getName(), relativePath, fileName, forceValid);
    }

    public static File getWorldFile(String world, String relativePath, String fileName, boolean forceValid)
    {
        return getSpecificFile(world, relativePath, fileName, forceValid);
    }

    public static File getWorldFolder(String world)
    {
        return getWorldFolder(world, "");
    }

    public static File getWorldFolder(World w)
    {
        return getWorldFolder(w.getName());
    }

    public static File getWorldFolder(World w, String relativePath)
    {
        return getWorldFolder(w.getName(), relativePath);
    }

    public static File getWorldFolder(String world, String relativePath)
    {
        return getSpecificFile(world, relativePath, "", false);
    }


    public static File getPlayerDataFolder(Player p, World w)
    {
        return getPlayerDataFolder(p, w, "");
    }

    public static File getPlayerDataFolder(Player p, World w, String relativePath)
    {
        return getPlayerDataFolder(p.getName(), w, relativePath);
    }

    public static File getPlayerDataFolder(String p, World w, String relativePath)
    {
        return getPlayerFile(p, w, relativePath, "", false);
    }

    public static File getPlayerFile(Player p, World world, String relativePath, String fileName, boolean forceValid)
    {
        return getPlayerFile(getUUID(p), world, relativePath, fileName, forceValid);
    }

    public static File getPlayerFile(String p, World world, String relativePath, String fileName)
    {
        return getPlayerFile(p, world, relativePath, fileName, true);
    }

    public static File getPlayerDataFolder(World w)
    {
        return getSpecificFile(w.getName() + "/" + WORLD_PLAYER_FOLDER_PATH, "", false);
    }

    public static File getPlayerFile(String p, World world, String relativePath, String fileName, boolean forceValid)
    {
        return getWorldFile(world.getName(), WORLD_PLAYER_FOLDER_PATH + "/" + getUUID(p) + '/' + relativePath, fileName, forceValid);
    }

    public static File getPlayerFile(Player p, World w, String relativePath, String fileName)
    {
        return getPlayerFile(getUUID(p), w, relativePath, fileName);
    }

    public static File getPlayerFile(String p, World w, String fileName)
    {
        return getPlayerFile(p, w, "", fileName);
    }

    public static File getPlayerFile(Player p, World w, String fileName)
    {
        return getPlayerFile(getUUID(p), w, fileName);
    }

    public static void clearFile(File f)
    {
        printLinesToFile(f, new ArrayList<String>());
    }

    public static void deleteFile(File f)
    {
        if (f.exists())
        {
            if (f.isDirectory())
            {
                File[] innerFiles = f.listFiles();
                if (innerFiles != null)
                {
                    for (File innerFile : innerFiles)
                    {
                        deleteFile(innerFile);
                    }
                }
            }
            f.delete();
        }
    }

    public static String serializeLocation(Location l)
    {
        return l.getWorld().getName() + " " + l.getX() + " " + l.getY() + " " + l.getZ() + " " + l.getYaw() + " " + l.getPitch();
    }

    public static List<String> serializeLocations(List<Location> locations)
    {
        List<String> result = new ArrayList<String>();
        for (Location l : locations)
        {
            result.add(serializeLocation(l));
        }
        return result;
    }

    public static Location deserializeLocation(World world, String value)
    {
        try
        {
            String[] datas = value.split(" ");
            int index = 0;

            world = world == null ? Bukkit.getWorld(datas[index++]) : world;
            if (world != null)
            {
                double x = Double.valueOf(datas[index++]);
                double y = Double.valueOf(datas[index++]);
                double z = Double.valueOf(datas[index++]);
                float yaw = Float.valueOf(datas[index++]);
                float pitch = Float.valueOf(datas[index]);

                return new Location(world, x, y, z, yaw, pitch);
            }
        }
        catch (NumberFormatException ex)
        {}
        catch (ArrayIndexOutOfBoundsException ex)
        {}
        return null;
    }

    public static Location deserializeLocation(String value)
    {
        return deserializeLocation(null, value);
    }

    public static List<Location> deserializeLocations(List<String> values)
    {
        List<Location> result = new ArrayList<Location>();
        for (String value : values)
        {
            Location readLocation = deserializeLocation(value);
            if (readLocation != null)
            {
                result.add(readLocation);
            }
        }
        return result;
    }

    public static class Entry
    {
        private final String key;
        private final String value;

        private Entry(String key, String value)
        {
            this.key = key;
            this.value = value;
        }

        public String getKey()
        {
            return key;
        }

        public String getValue()
        {
            return value;
        }
    }

    public static class EntrySet extends HashSet<Entry>
    {
        public String get(String key)
        {
            for (Entry entry : this)
            {
                if (entry.getKey().equals(key))
                {
                    return entry.getValue();
                }
            }
            return null;
        }
    }

    public static EntrySet getAllEntries(File f)
    {
        EntrySet result = new EntrySet();
        try
        {
            FileReader reader = new FileReader(f);
            JSONObject obj = (JSONObject)new JSONParser().parse(reader);
            reader.close();

            for (Map.Entry<String, Object> entry : (Set<Map.Entry<String, Object>>)obj.entrySet())
            {
                Object value = entry.getValue();
                if (value instanceof String || value instanceof Number)
                {
                    result.add(new Entry(entry.getKey(), "" + value));
                }
            }
            return result;
        }
        catch (ParseException e)
        {
            //createJSONFiles(f.getParent());
            e.printStackTrace();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return result;
    }

    public static void renameAllKeys(String oldName, String newName)
    {
        renameFolderKeys(getRoot(), newName, oldName);
    }

    public static void removeEntryWithKey(File file, String dataName)
    {
        renameKey(file, dataName, null);
    }

    public static void renameKey(File file, String oldKey, String newKey)
    {
        try
        {
            FileReader reader = new FileReader(file);
            JSONObject jObj = (JSONObject)new JSONParser().parse(reader);
            reader.close();

            Object value = jObj.get(oldKey);
            if (value != null)
            {
                jObj.remove(oldKey);
                if (newKey != null && newKey.length() != 0)
                {
                    jObj.put(newKey, value);
                }

                PrintWriter writer = new PrintWriter(file);
                jObj.writeJSONString(writer);
                writer.close();
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    public static void removeEntryWithValue(File file, String value)
    {
        renameValue(file, value, null);
    }

    private static void renameValue(File file, String oldValue, String newValue)
    {
        if (file.exists())
        {
            try
            {
                FileReader reader = new FileReader(file);
                JSONObject jObj = (JSONObject)new JSONParser().parse(reader);
                reader.close();


                for (Map.Entry<String, String> entry : new HashSet<Map.Entry<String, String>>(jObj.entrySet()))
                {
                    if (entry.getValue().equals(oldValue))
                    {
                        if (newValue == null || newValue.length() == 0)
                        {
                            jObj.remove(entry.getKey());
                        }
                        else
                        {
                            jObj.put(entry.getKey(), newValue);
                        }
                    }
                }

                PrintWriter writer = new PrintWriter(file);
                jObj.writeJSONString(writer);
                writer.close();
            }
            catch (ParseException ex)
            {
                ex.printStackTrace();
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
    }

    private static void renameFolderKeys(File folder, String oldName, String newName)
    {
        if (folder.isDirectory())
        {
            File[] files = folder.listFiles();
            if (files != null)
            {
                for (File containedFile : files)
                {
                    if (containedFile.isFile())
                    {
                        renameKey(containedFile, oldName, newName);
                    }
                    else if (containedFile.isDirectory())
                    {
                        renameFolderKeys(containedFile, oldName, newName);
                    }
                }
            }
        }
        else
        {
            renameKey(folder, oldName, newName);
        }
    }

    public static int incrementStatistic(File f, String dataName)
    {
        int dataValue;
        try
        {
            dataValue = Integer.valueOf(getData(f, dataName));
        }
        catch (NumberFormatException e)
        {
            dataValue = 0;
        }
        int incrementation = dataValue + 1;
        putData(f, dataName, incrementation);
        return incrementation;
    }

    public static File getPlayerFile(String p, String fileName, boolean validate)
    {
        return getPlayerFile(p, "", fileName, validate);
    }

//    public static String getIdealPlayerFolderName(Player p)
//    {
//        return getIdealPlayerFolderName(getUUIDAndCapitalName(p));
//    }
//
//    public static String getIdealPlayerFolderName(String p)
//    {
//        return getIdealPlayerFolderName(getUUIDAndCapitalName(p));
//    }

    public static String getIdealPlayerFolderName(UUIDFetcher.PlayerID id)
    {
        return id.getCapitalName() + PLAYER_FILE_DELIMITER + id.getUUID();
    }

    public static void renamedFileAfterDelay(final File f, final String newName)
    {
        if (!filesBeingRenamed.contains(f))
        {
            filesBeingRenamed.add(f);
            Bukkit.getScheduler().scheduleSyncDelayedTask(Plugin.getPlugin(), new Runnable()
            {
                @Override
                public void run()
                {
                    if (f.getName().equals(newName))
                    {
                        return;
                    }
                    File copy = new File(f.getAbsolutePath().substring(0, f.getAbsolutePath().length() - f.getName().length()) + newName);
                    try
                    {
                        if (f.isDirectory())
                        {
                            FileUtils.copyDirectory(f, copy);
                        }
                        else
                        {
                            FileUtils.copyFile(f, copy);
                        }
                    }
                    catch (IOException e)
                    {
                        return;
                    }
                    deleteFile(f);
                    filesBeingRenamed.remove(f);
                }
            }, 10);

        }
    }

    public static File getPlayerFile(String p, String relativePath, String fileName, boolean forceValid)
    {
        File mainFolder = getSpecificFile(PLAYER_DATA_PATH, "", false);
        File defaultFile = new File("?");
        UUIDFetcher.PlayerID id = getUUIDAndCapitalName(p);
        String idealName = id.getCapitalName() + PLAYER_FILE_DELIMITER + id.getUUID();
        if (mainFolder.exists())
        {
            File[] playerFolderList = mainFolder.listFiles();
            if (playerFolderList != null)
            {
                if (!id.isValid())
                {
                    return defaultFile;
                }
                for (File f : playerFolderList)
                {
                    if (f.isDirectory() && f.getName().endsWith(id.getUUID()))
                    {
                        if (!f.getName().startsWith(id.getCapitalName() + PLAYER_FILE_DELIMITER))
                        {
                            renamedFileAfterDelay(f, idealName);
                        }
                        return getSpecificFile(f, relativePath, fileName, forceValid);
                    }
                }
            }
        }
        return getSpecificFile(PLAYER_DATA_PATH, idealName, fileName, forceValid);
    }
    
    public static File getPlayerFile(Player p, String relativePath, String fileName, boolean forceValid)
    {
        return getPlayerFile(getUUID(p), relativePath, fileName, forceValid);
    }

    public static File getGeneralPlayerFile(String p, boolean forceValid)
    {
        return getPlayerFile(p, "Stats.json", forceValid);
    }

    public static File getGeneralPlayerFile(Player p, boolean forceValid)
    {
        return getGeneralPlayerFile(p.getName(), forceValid);
    }

    public static File getGeneralPlayerFile(String p)
    {
        return getGeneralPlayerFile(p, true);
    }

    public static File getGeneralPlayerFile(Player p)
    {
        return getGeneralPlayerFile(p.getName());
    }


    public static int getIntData(File f, String dataName)
    {
        int stat;
        try
        {
            stat = Integer.valueOf(getData(f, dataName));
        }
        catch (NumberFormatException e)
        {
            stat = 0;
            putData(f, dataName, stat);
        }
        return stat;
    }

    public static float getFloatData(File f, String dataName)
    {
        float stat;
        try
        {
            stat = Float.valueOf(getData(f, dataName));
        }
        catch (NumberFormatException e)
        {
            stat = 0;
            putData(f, dataName, stat);
        }
        return stat;
    }

    public static float getFloatData(String relativePath, String fileName, String dataName)
    {
        return getFloatData(getSpecificFile(relativePath, fileName), dataName);
    }

    public static String getSimpleData(File credentialFile, String key)
    {
        Scanner searcher = getScannerForFile(credentialFile);
        if (searcher == null)
        {
            return "";
        }
        while (searcher.hasNextLine())
        {
            String line = searcher.nextLine();
            if (line.startsWith(key + ": "))
            {
                searcher.close();
                return line.substring(key.length() + 2, line.length());
            }
        }
        searcher.close();
        return "";
    }

    public static String getSimpleData(String relativePath, String fileName, String dataName)
    {
        return getSimpleData(getSpecificFile(relativePath, fileName), dataName);
    }

    public static String getData(File f, String dataName)
    {
        String result = "";
        if (f.exists())
        {
            try
            {
                FileReader reader = new FileReader(f);
                JSONObject obj = (JSONObject)new JSONParser().parse(reader);
                reader.close();
                if (obj.containsKey(dataName))
                {
                    result = obj.get(dataName).toString();
                }
            }
            catch (ParseException e)
            {
                e.printStackTrace();
                //createJSONFiles(f.getParent());
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return result;
    }

    public static String getData(String relativePath, String fileName, String dataName)
    {
        return getData(getSpecificFile(relativePath, fileName), dataName);
    }

    public static ArrayList<String> getAllValues(File f)
    {
        if (f.exists())
        {
            try
            {
                FileReader reader = new FileReader(f);
                JSONObject jObj = (JSONObject)new JSONParser().parse(reader);
                reader.close();
                return new ArrayList<String>(jObj.values());
            }
            catch (ParseException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            Plugin.sendErrorMessage("Error! Could not get values from file " + f.getName());
        }
        return new ArrayList<String>();
    }

    public static ArrayList<String> getAllValues(String path, String fileName)
    {
        return getAllValues(getSpecificFile(path, fileName));
    }

//    public static List<ArrayList<String>> getAllDataSimple(File f)
//    {
//        Scanner lineReader = getScannerForFile(f);
//        if (lineReader == null)
//        {
//            return null;
//        }
//        List<ArrayList<String>> dataList = new ArrayList<ArrayList<String>>();
//        while (lineReader.hasNextLine())
//        {
//            Scanner lineScanner = new Scanner(lineReader.nextLine());
//            if (lineScanner.hasNext(".*:"))
//            {
//                lineScanner.next(".*:");
//                ArrayList<String> lineList = new ArrayList<String>();
//                while (lineScanner.hasNext())
//                {
//                    lineList.add(lineScanner.next().replaceAll(",", ""));
//                }
//                dataList.add(lineList);
//            }
//            lineScanner.close();
//        }
//        lineReader.close();
//        return dataList;
//    }

    public static ArrayList<String> getDataList(File f, String dataName)
    {
        String objectHere = getData(f, dataName);
//            FileReader reader =  new FileReader(f);
//            JSONObject obj = (JSONObject)new JSONParser().parse(reader);
//            reader.close();
//            String objectHere = (String)obj.get(dataName);
        final ArrayList<String> result;
        if (objectHere.length() > 0)
        {
            if (objectHere.charAt(0) == '[' && objectHere.charAt(objectHere.length() - 1) == ']')
            {
                if (objectHere.length() > 2)
                {
                    result = new ArrayList<String>(Arrays.asList(objectHere.substring(1, objectHere.length() - 1).split(", ")));
                }
                else
                {
                    result = new ArrayList<String>();
                }
            }
            else
            {
                result = new ArrayList<String>();
                result.add(objectHere);
            }
        }
        else
        {
            result = new ArrayList<String>();
        }
        return result;
    }

    public static void setData(File f, HashMap<String, Object> map)
    {
        try
        {
            if (f.exists())
            {
                FileReader reader = new FileReader(f);
                JSONObject jsonObj = (JSONObject)new JSONParser().parse(reader);
                reader.close();
                for (Map.Entry<String, Object> entry : map.entrySet())
                {
                    jsonObj.put(entry.getKey(), entry.getValue());
                }
                PrintWriter writer = new PrintWriter(f);
                jsonObj.writeJSONString(writer);
                writer.close();
            }
        }
        catch (ParseException ex)
        {
            ex.printStackTrace();
            Plugin.sendErrorMessage(ChatColor.RED + "Error! File " + f.getName() + " had an invalid json format!");
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    public static void putData(File f, String dataName, Object dataValue)
    {
        forceValidity(f);
        try
        {
            FileReader reader = new FileReader(f);
            JSONObject jsonObj = (JSONObject)new JSONParser().parse(reader);
            reader.close();

            jsonObj.put(dataName, "" + dataValue);

            PrintWriter writer = new PrintWriter(f);
            jsonObj.writeJSONString(writer);
            writer.close();
        }
        catch (ParseException ex)
        {
            ex.printStackTrace();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
    }

    public static void putDataList(File f, String dataName, List<String> data)
    {
        putData(f, dataName, Arrays.asList(data.toArray()).toString());
        //        try
        //        {
        //            FileReader reader = new FileReader(f);
        //            JSONObject obj = (JSONObject)new JSONParser().parse(reader);
        //            reader.close();
        ////            String value = "";
        ////            for (String s : data)
        ////            {
        ////                value += ", " + s;
        ////            }
        ////            if (value.length() > ", ".length())
        ////            {
        ////                value = value.substring(", ".length(), value.length());
        ////            }
        //            obj.put(dataName, data.toArray());
        //
        //            PrintWriter writer = new PrintWriter(f);
        //            obj.writeJSONString(writer);
        //            writer.close();
        //        }
        //        catch (ParseException e)
        //        {
        //            e.printStackTrace();
        //        }
        //        catch (IOException e)
        //        {
        //            e.printStackTrace();
        //        }
    }

    public static boolean addDataToList(File f, String dataName, String newData)
    {
        ArrayList<String> data = getDataList(f, dataName);
        if (!data.contains(newData))
        {
            data.add(newData);
            putDataList(f, dataName, data);
            return true;
        }
        return false;
    }

    public static boolean removeDataFromList(File file, String dataKey, String value)
    {
        ArrayList<String> data = getDataList(file, dataKey);
        if (data.contains(value))
        {
            data.remove(value);
            putDataList(file, dataKey, data);
            return true;
        }
        return false;
    }

    public static void putDataListSimple(File f, String dataName, List<String> data)
    {
        String dataAsString = "";
        for (int i = 0; i < data.size() - 1; i++)
        {
            dataAsString += data.get(i) + ", ";
        }
        dataAsString += data.get(data.size() - 1);
        putData(f, dataName, dataAsString);
    }

    public static void printLinesToFile(File file, List<String> dataList)
    {
        PrintWriter printer;
        try
        {
            printer = new PrintWriter(file);
        }
        catch (FileNotFoundException e0) //couldn't find file (impossible)
        {
            Bukkit.getConsoleSender().sendMessage(Plugin.loggerPrefix() + ChatColor.RED + "Could not find file " + file.getName() + "!");
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
    
    public static List<String> readLinesFromFile(File file)
    {
        List<String> result = new ArrayList<String>();
        if (file.exists())
        {
            try
            {
                Scanner scanner = new Scanner(file);
                while (scanner.hasNextLine())
                {
                    result.add(scanner.nextLine());
                }
                scanner.close();
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
        return result;
    }

    private static Scanner getScannerForFile(File file)
    {
        try
        {
            forceValidity(file);
            return new Scanner(file);
        }
        catch (FileNotFoundException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static String putFileContents(File f, Object contents)
    {
        forceValidity(f);
        String stringContent = contents.toString();
        try
        {
            FileOutputStream outputStream = new FileOutputStream(f);
            for (int i = 0; i < stringContent.length(); i++)
            {
                outputStream.write(stringContent.charAt(i));
            }
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static String readFileContents(File f)
    {
        if (f.exists())
        {
            try
            {
                FileInputStream stream = new FileInputStream(f);
                StringBuilder builder = new StringBuilder();
                for (int next = stream.read(); next != -1; next = stream.read())
                {
                    builder.append((char)next);
                }
                return builder.toString();
            }
            catch (IOException ex)
            {
                ex.printStackTrace();
            }
        }
        return "";
    }
    
    public static Float getFloatValue(String[] strings, int index)
    {
        return index >= 0 && index < strings.length ? getFloatValue(strings[index]) : null;
    }
    
    public static Float getFloatValue(String str)
    {
        try
        {
            return Float.valueOf(str);
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }
    
    public static Integer getIntegerValue(String[] strings, int index)
    {
        return index >= 0 && index < strings.length ? getIntegerValue(strings[index]) : null;
    }
    
    public static Integer getIntegerValue(String str)
    {
        try
        {
            return Integer.valueOf(str);
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }
}
