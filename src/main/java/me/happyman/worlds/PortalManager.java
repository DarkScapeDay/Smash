package me.happyman.worlds;

import me.happyman.commands.SmashManager;
import me.happyman.source;
import me.happyman.utils.DirectoryType;
import org.bukkit.*;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.util.Vector;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static me.happyman.worlds.PortalListener.halfMaterializedPlayers;
import static me.happyman.worlds.PortalListener.portalDwellers;
import static me.happyman.worlds.SmashWorldInteractor.*;

public class PortalManager implements CommandExecutor
{
    private static final float ENTRANCE_OFFSET = 1f;
    public static final String REGISTER_PORTAL_COMMAND ="registerportal";
    public static final String UNREGISTER_PORTAL_COMMAND = "unregisterportal";
    public static final String LIST_PORTAL_COMMAND = "listportals";
    public static final String REFRESH_PORTAL_COMMAND = "refreshportal";

    private static HashMap<String, EventPortal> allPortals = new HashMap<String, EventPortal>();
    private static final String PORTAL_REGISTRY_FILE_DELIMITER = ", ";
    private static ArrayList<String> portalRegistry = new  ArrayList<String>();
    private static final Material DEFAULT_MATERIAL = Material.OBSIDIAN;

    private static source plugin;

    public PortalManager(source plugin)
    {
        this.plugin = plugin;
        plugin.setExecutor(LIST_PORTAL_COMMAND, this);
        plugin.setExecutor(REGISTER_PORTAL_COMMAND, this);
        plugin.setExecutor(UNREGISTER_PORTAL_COMMAND, this);
        plugin.setExecutor(REFRESH_PORTAL_COMMAND, this);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            public void run()
            {
                initialize();
                new PortalListener();
            }
        }, 2);
    }

    public void makePortals()
    {
        logPortal(new EventPortal("Sean", Bukkit.getWorld("Stuffland"), -63.5f, 51, 1131.5f, 9, 9, 1,
                PortalDirection.NORTH_SOUTH, new Material[]{Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK, Material.DIAMOND_ORE}, Material.THIN_GLASS)
        {
            @Override
            public void performTeleportAction(Player p)
            {
                openWorldGui(p);
            }
        });
    }

    public void initialize()
    {
        //get portal registry list
        try
        {
            File regFile = getPortalRegistryFile();
            Scanner s = new Scanner(regFile);
            while (s.hasNextLine())
            {
                try
                {
                    portalRegistry = new ArrayList<String>(Arrays.asList(s.nextLine().split(PORTAL_REGISTRY_FILE_DELIMITER)));
                }
                catch (NoSuchElementException ex)
                {
                    portalRegistry = new ArrayList<String>();
                }
            }
        }
        catch(IOException ex)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED +"Error! Unable to complete portal registry cache!");
        }

        makePortals();

        //uncache non-existent portals
        File f = getPortalCacheFile();
        if (f == null)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Error! Could not find the portal_cache file!");
            return;
        }
        JSONArray array = getJSONArray(f);
        if (array == null)
        {
            Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Could not fetch the JSON array for the portals!");
        }

        for (int i = 0; i < array.size(); i++)
        {
            Object curItem = array.get(i);
            if (curItem instanceof JSONObject)
            {
                JSONObject curJSONObject = (JSONObject) curItem;
                if (curJSONObject.containsKey("id"))
                {
                    String id = (String) curJSONObject.get("id");
                    if (!allPortals.containsKey(id) /*uncache non-existent portals*/)
                    {
                        array.remove(i--);
                    }
                }
            }
        }
        saveJSONArray(array, f);

        //unregister non-existent portals
        try
        {
            File reg = getPortalRegistryFile();
            List<String> idsToKeep = new ArrayList<String>();
            for (String id : portalRegistry)
            {
                if (allPortals.containsKey(id))
                {
                    idsToKeep.add(id);
                }
            }

            PrintWriter writer = new PrintWriter(reg);
            String whatToPrint = "";
            for (String id : idsToKeep)
            {
                whatToPrint += id + PORTAL_REGISTRY_FILE_DELIMITER;
            }
            if (whatToPrint.length() >= PORTAL_REGISTRY_FILE_DELIMITER.length())
            {
                whatToPrint = whatToPrint.substring(0, whatToPrint.length() - PORTAL_REGISTRY_FILE_DELIMITER.length());
            }
            writer.print(whatToPrint);
            writer.close();
        }
        catch(FileNotFoundException ex)
        {
            SmashManager.getPlugin().sendErrorMessage(ChatColor.RED + "File not found for portal registry!");
        }
    }

    public static HashSet<EventPortal> getAllPortals()
    {
        return new HashSet<EventPortal>(allPortals.values());
    }

    public static IDState setRegistered(String portalID, boolean register)
    {
        if (!allPortals.containsKey(portalID))
        {
            return IDState.NONEXISTENT;
        }
        else if (register && isRegistered(portalID) || !register && !isRegistered(portalID))
        {
            return IDState.UNCHANGED;
        }
        if (register)
        {
            registerPortal(portalID);
        }
        else
        {
            unregisterPortal(portalID);
        }
        return IDState.CHANGED;
    }

    public static EventPortal getPortal(String id)
    {
        if (allPortals.containsKey(id))
        {
            return allPortals.get(id);
        }
        return null;
    }

    private static void registerPortal(String id)
    {
        EventPortal portal = getPortal(id);
        if (portal != null && !isRegistered(id))
        {
            portalRegistry.add(id);
            try
            {
                File f = getPortalRegistryFile();
                Scanner s = new Scanner(f);
                List<String> ids = new ArrayList<String>();
                while (s.hasNextLine())
                {
                    String[] idsInLine;
                    try
                    {
                        idsInLine = s.nextLine().split(PORTAL_REGISTRY_FILE_DELIMITER);
                    }
                    catch (NoSuchElementException ex) {
                        idsInLine = new String[] {};
                    }
                    for (String curId : idsInLine)
                    {
                        ids.add(curId);
                    }
                }
                ids.add(id);

                PrintWriter writer = new PrintWriter(f);
                String whatToPrint = "";
                while (ids.size() > 0)
                {
                    whatToPrint += ids.get(0) + PORTAL_REGISTRY_FILE_DELIMITER;
                    ids.remove(0);
                }
                if (whatToPrint.length() >= PORTAL_REGISTRY_FILE_DELIMITER.length())
                {
                    whatToPrint = whatToPrint.substring(0, whatToPrint.length() - PORTAL_REGISTRY_FILE_DELIMITER.length());
                }
                writer.print(whatToPrint);
                writer.close();
            }
            catch(IOException ex)
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Error! Couldn't register " + id + ", a portal...");
            }
            portal.build();
        }
    }

    public File getPortalCacheFile()
    {
        File f = SmashManager.getPlugin().getSpecificFile(DirectoryType.SERVER_DATA, "", "portal_cache.json");
        try
        {
            if (!f.exists() && !f.createNewFile())
            {
                throw new IOException("Could not get cache file!");
            }
        }
        catch (IOException ex)
        {
            SmashManager.getPlugin().sendErrorMessage(ChatColor.RED + "Error! Could not write to file for portal data!");
            return null;
        }
        return f;
    }

    private static void unregisterPortal(String id)
    {
        EventPortal portal = getPortal(id);
        boolean registered = isRegistered(id);
        boolean exists = portal != null;

        if (!exists || registered)
        {
            try
            {
                File f = getPortalRegistryFile();
                if (f == null) return;
                Scanner s = new Scanner(f);
                List<String> idsToPrint = new ArrayList<String>();
                while (s.hasNextLine())
                {
                    String[] vals = s.nextLine().split(PORTAL_REGISTRY_FILE_DELIMITER);
                    for (String val : vals)
                    {
                        if (allPortals.containsKey(val) && !val.equals(id))
                        {
                            idsToPrint.add(val);
                        }
                    }
                }
                PrintWriter writer = new PrintWriter(f);
                String whatToPrint = "";
                for (String curId : idsToPrint)
                {
                    whatToPrint += curId + PORTAL_REGISTRY_FILE_DELIMITER;
                }
                if (whatToPrint.length() >= PORTAL_REGISTRY_FILE_DELIMITER.length())
                {
                    whatToPrint = whatToPrint.substring(0, whatToPrint.length() - PORTAL_REGISTRY_FILE_DELIMITER.length());
                }
                writer.print(whatToPrint);
                writer.close();
            }
            catch(IOException ex)
            {
                Bukkit.getConsoleSender().sendMessage(ChatColor.RED + "Error! Couldn't unregister " + id + ", a portal...");
            }
        }

        if (exists && registered)
        {
            portalRegistry.remove(id);
            portal.destroy();
        }
    }

    private static boolean portalExists(String id)
    {
        return allPortals.containsKey(id);
    }

    private static IDState refreshPortal(String id)
    {
        if (portalExists(id))
        {
            getPortal(id).refresh();
            return IDState.REFRESHED;
        }
        return IDState.NONEXISTENT;
    }

    private static File getPortalRegistryFile()
    {
        File f =  plugin.getSpecificFile(DirectoryType.SERVER_DATA, "", "portal_registry.txt");
        try
        {
            if (!f.exists() && !f.createNewFile())
            {
                throw new IOException("Unable to find or create portal registry file!");
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
            return null;
        }
        return f;
    }

    public static boolean isRegistered(String portalId)
    {
        return portalRegistry.contains(portalId);
    }

    public static JSONArray getJSONArray(File f)
    {
        try
        {
            Scanner s = new Scanner(f);
            String fileOriginalContents = "";
            if (s.hasNextLine())
            {
                fileOriginalContents += s.nextLine();
            }
            while (s.hasNextLine())
            {
                fileOriginalContents += "\n" + s.nextLine();
            }
            if (fileOriginalContents.length() < 2 || fileOriginalContents.charAt(0) != '[' || fileOriginalContents.charAt(fileOriginalContents.length() - 1) != ']')
            {
                fileOriginalContents = "[]";
            }

            return (JSONArray)new JSONParser().parse(fileOriginalContents);
        }
        catch (ParseException ex)
        {
            ex.printStackTrace();
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        return null;
    }

    public static void saveJSONArray(JSONArray arr, File f)
    {
        try
        {
            PrintWriter writer = new PrintWriter(f);
            arr.writeJSONString(writer);
            writer.close();
        }
        catch (IOException ex)
        {//impossible to reach this block
            plugin.sendErrorMessage(ChatColor.RED + "Error! Could not write to file for portal data!");
        }

    }

    //make sure to remember that 0s are on the edges of blocks, and 0.5s are in the middle to the southeast
    //.0 -> use even dimension, .5 -> use odd dimension
    public static void logPortal(EventPortal portal)
    {
        allPortals.put(portal.id, portal);
    }

    public static void cancelPortalTasks(Player p)
    {
        for (EventPortal portal : allPortals.values())
        {
            portal.cancelAndForget(p);
        }
    }

    public static String getPortalListForOutput()
    {
        String portalList = "";
        for (EventPortal portal : allPortals.values())
        {
            if (portal.isRegistered())
            {
                portalList += ChatColor.GREEN + portal.id + "(" + portal.centerX + ", " + portal.centerY + ", " + portal.centerZ + ")" + ChatColor.WHITE + ", ";
            }
            else
            {
                portalList += ChatColor.RED + portal.id + ChatColor.WHITE + ", ";
            }
        }
        if (portalList.length() >= PORTAL_REGISTRY_FILE_DELIMITER.length())
        {
            portalList = portalList.substring(0, portalList.length() - PORTAL_REGISTRY_FILE_DELIMITER.length());
        }
        return portalList;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (plugin.matchesCommand(label, LIST_PORTAL_COMMAND))
        {
            if (portalRegistry.size() <= 0)
            {
                sender.sendMessage(ChatColor.YELLOW + "There are no portals.");
            }
            sender.sendMessage(ChatColor.GRAY + "List of portals: " + getPortalListForOutput());
            return true;
        }
        else
        {
            boolean register = plugin.matchesCommand(label, REGISTER_PORTAL_COMMAND);
            boolean unregister = plugin.matchesCommand(label, UNREGISTER_PORTAL_COMMAND);
            if (register || unregister || plugin.matchesCommand(label, REFRESH_PORTAL_COMMAND))
            {
                if (args.length < 1 || args.length > 2)
                {
                    return false;
                }
                try
                {
                    int i = 0;
                    String portalID = args[i++];
                    IDState state;
                    if (register || unregister)
                    {
                        state = setRegistered(portalID, register);
                    }
                    else
                    {
                        state = refreshPortal(portalID);
                    }
                    String r;
                    if (register)
                    {
                        r = "registered";
                    }
                    else
                    {
                        r = "unregistered";
                    }
                    if (state.equals(IDState.NONEXISTENT))
                    {
                        sender.sendMessage(ChatColor.RED + "Portal " + portalID + " not found! Here is the list of portals: " + getPortalListForOutput());
                        return true;
                    }
                    else if (state.equals(IDState.UNCHANGED))
                    {
                        sender.sendMessage(ChatColor.RED + "Portal " + portalID + " was already " + r + "!");
                    }
                    else if (state.equals(IDState.CHANGED))
                    {
                        sender.sendMessage(ChatColor.GREEN + "Set portal " + portalID + " to be " + r + ".");
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.GREEN + "Refreshed portal " + portalID);
                    }
                    boolean teleport = false;
                    if (args.length - i > 0)
                    {
                        String tp = args[i++];
                        teleport = tp.length() > 0 && Character.toLowerCase(tp.charAt(0)) == 't';
                    }
                    if (teleport && sender instanceof Player && allPortals.containsKey(portalID))
                    {
                        Player p = (Player) sender;
                        p.teleport(allPortals.get(portalID).getEntrancePoint());
                    }
                }
                catch (NumberFormatException ex)
                {
                    sender.sendMessage("Invalid bool");
                }
                return true;
            }
        }
        return false;
    }

    private enum PortalDirection
    {
        NORTH_SOUTH, EAST_WEST, UP_DOWN;
    }

    private enum IDState
    {
        NONEXISTENT, UNCHANGED, CHANGED, REFRESHED;
    }


    public abstract class EventPortal implements Listener
    {
        private final Random r = new Random();
        private final Material[] buildingMaterials;
        private final Material portalCenterBlock;
        private final long tpDelayTicks;
        private final World w;
        private final PortalDirection orientation;
        private final HashMap<Block, Material> originalMaterials = new HashMap<Block, Material>();
        protected final String id;
        private final int westI;
        private final int eastI;
        private final int bottomJ;
        private final int topJ;
        private final int northK;
        private final int southK;
        private final float xMin;
        private final float yMin;
        private final float zMin;
        private final float centerX;
        private final float centerY;
        private final float centerZ;
        private final int width;
        private final int height;
        private final int depth;
        private boolean isBuilt;
        private final Location entrancePoint;

        public EventPortal(String id, World w, float centerX, int baseY, float centerZ,
                           int width, int height, int depth, PortalDirection orientation, Material[] buildingMaterials, Material portalCenterBlock)
        {
            this(id, w, centerX, baseY, centerZ, width, height, depth, orientation, 0, buildingMaterials, portalCenterBlock);
        }

        public EventPortal(String id, World w, float centerX, int baseY, float centerZ,
                           int width, int height, int depth, PortalDirection orientation, Material[] buildingMaterials)
        {
            this(id, w, centerX, baseY, centerZ, width, height, depth, orientation, buildingMaterials, Material.AIR);
        }

        public EventPortal(String id, World w, float centerX, int baseY, float centerZ,
                           int width, int height, int depth, PortalDirection orientation)
        {
            this(id, w, centerX, baseY, centerZ, width, height, depth, orientation, new Material[] {DEFAULT_MATERIAL}, Material.AIR);
        }

        public EventPortal(String id, World w, float centerX, int baseY, float centerZ,
                           int width, int height, int depth, PortalDirection orientation, int tpTickDelay, Material[] buildingMaterials, Material portalCenterBlock)
        {
            this.tpDelayTicks = tpTickDelay;
            this.centerX = centerX;
            this.centerY = (float)baseY - 1f + (float)height/2;
            this.centerZ = centerZ;
            this.depth = depth;
            this.height = height;
            this.width = width;
            this.id = id;

            xMin = centerX - (float)width/2;
            float westIf = xMin + 0.5f;
            westI = (int)westIf;
            eastI = (int)(2*centerX - westIf);

            yMin = centerY - (float)height/2;
            float bottomJf = yMin + 0.5f;
            bottomJ = (int)bottomJf;
            topJ =  (int)(2*centerY - bottomJf);

            zMin = centerZ - (float)depth/2;
            float northKf = zMin + 0.5f;
            northK = (int)northKf;//the oppressed
            southK =  (int)(2*centerZ - northKf);//the free


            Block testBlock;
            float offset = -ENTRANCE_OFFSET;

            switch (orientation)
            {
                case NORTH_SOUTH:
                    testBlock = w.getBlockAt((int)centerX, baseY, (int)(centerZ - 1));
                    offset = testBlock.getType().isSolid() ? -offset : offset;
                    entrancePoint = new Location(w, centerX, baseY, centerZ + offset);
                    break;
                case UP_DOWN:

                    entrancePoint =
                            !w.getBlockAt((int)centerX, baseY, northK).getType().isSolid() ? new Location(w, centerX, baseY, centerZ - ENTRANCE_OFFSET) :
                            !w.getBlockAt(westI, baseY, (int)centerZ).getType().isSolid() ? new Location(w, centerX - ENTRANCE_OFFSET, baseY, centerZ) :
                            !w.getBlockAt((int)centerX, baseY, southK).getType().isSolid() ? new Location(w, centerX, baseY, centerZ + ENTRANCE_OFFSET) :
                            new Location(w, centerX + ENTRANCE_OFFSET, baseY, centerZ);
                    break;
                case EAST_WEST:
                    testBlock = w.getBlockAt((int)(centerX - 1), baseY, (int)centerZ);
                    offset = testBlock.getType().isSolid() ? -offset : offset;
                    entrancePoint = new Location(w, centerX + offset, baseY, centerZ);
                    break;
                default:
                    entrancePoint = new Location(w, centerX, baseY, centerZ);
                    break;
            }
            entrancePoint.setDirection(new Vector(centerX, baseY, centerZ).subtract(entrancePoint.toVector()));


            this.w = w;
            this.orientation = orientation;

            this.buildingMaterials = buildingMaterials == null ? new Material[] {DEFAULT_MATERIAL} : buildingMaterials;
            this.portalCenterBlock = portalCenterBlock == null ? Material.AIR : portalCenterBlock;

            //log the original materials to the cache
            isBuilt = false;
            for (Object curObj : getJSONArray(getPortalCacheFile()))
            {
                if (curObj instanceof JSONObject)
                {
                    JSONObject curJason = (JSONObject)curObj;
                    if (curJason.get("id").equals(id))
                    {
                        Set<Map.Entry<Object, Object>> entrySet = curJason.entrySet();
                        for (Map.Entry<Object, Object> entry : entrySet)
                        {
                            String coordString = (String)entry.getKey();
                            String[] coords = coordString.split(",");
                            try
                            {
                                if (coords.length == 3)
                                {
                                    Block curBlock = w.getBlockAt((int)((float)Float.valueOf(coords[0])), (int)((float)Float.valueOf(coords[1])), (int)((float)Float.valueOf(coords[2])));
                                    originalMaterials.put(curBlock, Material.getMaterial((String)entry.getValue()));
                                }
                            }
                            catch (NumberFormatException ex)
                            {
                                ex.printStackTrace();
                            }
                        }
                        isBuilt = true;
                        break;
                    }
                }
            }

            if (!isRegistered() && isBuilt)
            {
                destroy();
            }
            else if (isRegistered() && !isBuilt)
            {
                build();
            }
        }

        public boolean valid()
        {
            return w != null;
        }

        public abstract void performTeleportAction(Player p);

        public boolean containsPlayer(Player p)
        {
            if (!isRegistered())
            {
                return false;
            }

            Location l = p.getLocation();
            float pX = (float)l.getX();
            float pY = (float)l.getY();
            float pZ = (float)l.getZ();

            switch (orientation)
            {
                case EAST_WEST: return Math.abs(pX - centerX)*2 <= width && Math.abs(pY - centerY)*2 + 2f <= height && Math.abs(pZ - centerZ)*2 + 2f <= depth;
                case NORTH_SOUTH: return Math.abs(pX - centerX)*2 + 2f <= width && Math.abs(pY - centerY)*2 + 2f <= height && Math.abs(pZ - centerZ)*2 <= depth;
                case UP_DOWN: return Math.abs(pX - centerX)*2 + 2f <= width && Math.abs(pY - centerY)*2 <= height && Math.abs(pZ - centerZ)*2 + 2f <= depth;
                default:
                    SmashManager.getPlugin().sendErrorMessage(ChatColor.RED+ "Error! Had an invalid orientation when deciding if " + p.getLocation().toVector().toString() + " was in portal " + id + " or not.");
                    return false;
            }

        }

        public boolean isRegistered()
        {
            return PortalManager.isRegistered(id);
        }

        public boolean isBuilt()
        {
            return isBuilt;
        }

        private void destroy()
        {
            destroy(false);
        }

        public void destroy(boolean justBlocks)
        {
            if (!valid())
            {
                SmashManager.getPlugin().sendErrorMessage(ChatColor.RED + "Error! Invalid world for portal " + id);
                return;
            }
            if (isBuilt())
            {
                File f = getPortalCacheFile();
                JSONArray mainArray = getJSONArray(f);
                for (Object obj : mainArray)
                {
                    if (obj instanceof JSONObject)
                    {
                        JSONObject jObj = (JSONObject)obj;
                        if (jObj.containsKey("id") && jObj.get("id").equals(id))
                        {
                            for (Object val : jObj.entrySet())
                            {
                                Map.Entry<String, String> entry = (Map.Entry<String, String>)val;
                                String loc = entry.getKey();
                                String mat = entry.getValue();
                                String[] locArray = loc.split(",");
                                if (locArray.length == 3)
                                {
                                    Block b = w.getBlockAt((int)(float)Float.valueOf(locArray[0]), (int)(float)Float.valueOf(locArray[1]), (int)(float)Float.valueOf(locArray[2]));
                                    b.setType(Material.getMaterial(mat));
                                }
                            }
                        }
                    }
                }

                if (!justBlocks)
                {
                    removeCacheData();
                    originalMaterials.clear();
                }
                isBuilt = false;
            }
            if (isRegistered())
            {
                unregisterPortal(id);
            }
        }

        public void build()
        {
            if (!valid())
            {
                SmashManager.getPlugin().sendErrorMessage(ChatColor.RED + "Error! Invalid world for portal " + id);
                return;
            }
            if (!isBuilt())
            {
                File f = null;
                JSONArray mainArray = null;
                JSONObject jasonVoorhees = null;
                boolean newBuild = false;
                f = getPortalCacheFile();
                mainArray = getJSONArray(f);

                for (Object obj : mainArray)
                {
                    if (obj instanceof JSONObject && ((JSONObject)obj).containsKey("id") && ((JSONObject)obj).get("id").equals(id))
                    {
                        jasonVoorhees = (JSONObject)obj;
                        break;
                    }
                }
                if (jasonVoorhees == null)
                {
                    jasonVoorhees = new JSONObject();
                    newBuild = true;
                    jasonVoorhees.put("id", id);
                }

                for (int i = westI; i <= eastI; i++)
                {
                    for (int j = bottomJ; j <= topJ; j++)
                    {
                        for (int k = northK; k <= southK ; k++)
                        {
                            Block blockToChange = w.getBlockAt(i, j, k);
                            if (!originalMaterials.containsKey(blockToChange))
                            {
                                originalMaterials.put(blockToChange, blockToChange.getType());
                            }
                            String blockLocation = blockToChange.getLocation().toVector().toString();
                            if (newBuild || !jasonVoorhees.containsKey(blockLocation))
                            {
                                jasonVoorhees.put(blockLocation, blockToChange.getType().toString());
                            }
                            if (isEdge(i, j, k, orientation))
                            {
                                Material chosenMaterial = buildingMaterials[r.nextInt(buildingMaterials.length)];
                                blockToChange.setType(chosenMaterial);
                            }
                            else
                            {
                                blockToChange.setType(portalCenterBlock);
                            }
                        }
                    }
                }

                if (newBuild)
                {
                    mainArray.add(jasonVoorhees);
                }
                saveJSONArray(mainArray, f);
                isBuilt = true;
            }
            if (!isRegistered())
            {
                registerPortal(id);
            }
        }

        private boolean isEdge(int i, int j, int k, PortalDirection dir)
        {
            switch (dir)
            {
                case UP_DOWN: return i == westI || i == eastI || k == northK || k == southK;
                case NORTH_SOUTH: return i == westI || i == eastI || j == bottomJ || j == topJ;
                case EAST_WEST: return j == bottomJ || j == topJ || k == northK || k == southK;
                default: return true;
            }
        }

        private void removePortalBlock(World w, int x, int y, int z)
        {
            Block blockToChange = w.getBlockAt(x, y, z);
            blockToChange.setType(getOriginalBlock(blockToChange));
        }

        private Material getOriginalBlock(Block blockToChange)
        {
            if (originalMaterials.containsKey(blockToChange))
            {
                return originalMaterials.get(blockToChange);
            }
//            else
//            {
//                File f = getPortalCacheFile();
//                if (f == null)
//                {
//                    SmashManager.getPlugin().sendErrorMessage(ChatColor.RED + "Error! Could not get portal cache file!");
//                    return Material.AIR;
//                }
//
//                JSONArray arr = getJSONArray(f);
//                if (arr == null)
//                {
//                    SmashManager.getPlugin().sendErrorMessage(ChatColor.RED + "Error! Could not get JSON array!");
//                    return Material.AIR;
//                }
//
//                for (Object curObj : arr)
//                {
//                    if (curObj instanceof JSONObject)
//                    {
//                        JSONObject curJason = (JSONObject)curObj;
//                        if (curJason.get("id").equals(id) && curJason.containsKey(blockToChange.getLocation().toVector().toString()))
//                        {
//                            return Material.getMaterial((String)curJason.get(blockToChange.getLocation().toVector().toString()));
//                        }
//                    }
//                }
//            }
            return Material.AIR;
        }

        public void cancelAndForget(Player p)
        {
            if (halfMaterializedPlayers.containsKey(p))
            {
                Bukkit.getScheduler().cancelTask(halfMaterializedPlayers.get(p));
                halfMaterializedPlayers.remove(p);
            }
            if (portalDwellers.contains(p))
            {
                portalDwellers.remove(p);
            }
        }

        private void removeCacheData()
        {
            File f = getPortalCacheFile();
            if (f == null)
            {
                plugin.sendErrorMessage(ChatColor.RED + "Error! Could not find the portal_cache file!");
                return;
            }
            JSONArray array = getJSONArray(f);
            if (array == null)
            {
                plugin.sendErrorMessage(ChatColor.RED + "Could not fetch the JSON array for the portals!");
                return;
            }

            for (int i = 0; i < array.size(); i++)
            {
                Object curItem = array.get(i);
                if (curItem instanceof JSONObject)
                {
                    JSONObject curJSONObject = (JSONObject) curItem;
                    if (curJSONObject.containsKey("id") && curJSONObject.get("id").equals(id))
                    {
                        array.remove(i--);
                    }
                }
            }
            saveJSONArray(array, f);
        }

        public PortalDirection getOrientation()
        {
            return orientation;
        }

        public Location getEntrancePoint() {
            return entrancePoint;
        }

        public void refresh()
        {
            destroy(true);
            build();
        }

        public long getTpDelayTicks() {
            return tpDelayTicks;
        }
    }


}