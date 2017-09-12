package me.happyman.worlds;


import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.util.Vector;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static me.happyman.Plugin.*;
import static me.happyman.utils.FileManager.getServerDataFile;
import static me.happyman.worlds.MainListener.sendPlayerToWorld;

public class PortalManager implements CommandExecutor
{
    private static class PortalListener implements Listener
    {
        private PortalListener()
        {
            Bukkit.getPluginManager().registerEvents(this, getPlugin());
        }

        @EventHandler
        public void listenToAllWhoMayEnter(PlayerMoveEvent e)
        {
            final Player p = e.getPlayer();
            Iterator<CuboidPortal> portalIterator = PortalEnum.iterator();
            while (portalIterator.hasNext())
            {
                final CuboidPortal portal = portalIterator.next();
                final boolean isInPortal = portal.containsPlayer(p);
                final boolean isDweller = portal.hasDweller(p);
                if (isInPortal && !halfMaterializedPlayers.containsKey(p) && !isDweller)
                {
                    portal.setDwelling(p, true);
                    p.setVelocity(new Vector().zero());
                    p.sendMessage(ChatColor.GREEN + "" + ChatColor.ITALIC + "Initiating...");
                    if (portal.getTpDelayTicks() > 0)
                    {
                        int task = Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable()
                        {
                            public void run()
                            {
                                halfMaterializedPlayers.remove(p);
                                portal.performTeleportAction(p);
                            }
                        }, portal.getTpDelayTicks());
                        halfMaterializedPlayers.put(p, task);
                    }
                    else
                    {
                        portal.performTeleportAction(p);
                    }
                }
                else if (!isInPortal && isDweller)
                {
                    portal.setDwelling(p, false);
                    if (halfMaterializedPlayers.containsKey(p))
                    {
                        Bukkit.getScheduler().cancelTask(halfMaterializedPlayers.get(p));
                        halfMaterializedPlayers.remove(p);
                        p.sendMessage(ChatColor.YELLOW + "Teleport canceled! Sorry you changed your mind so soon.");
                    }
                }
            }
        }
    }

    private static final HashMap<Player, Integer> halfMaterializedPlayers = new HashMap<Player, Integer>();

    private static final String REGISTER_PORTAL_COMMAND ="registerportal";
    private static final String UNREGISTER_PORTAL_COMMAND = "unregisterportal";
    private static final String LIST_PORTAL_COMMAND = "listportals";
    private static final String REFRESH_PORTAL_COMMAND = "refreshportal";

//    private static final HashMap<String, CuboidPortal> allPortals = new HashMap<String, CuboidPortal>();
    private static final String PORTAL_REGISTRY_FILE_DELIMITER = ", ";
    private static ArrayList<String> portalRegistry = new  ArrayList<String>();
    private static final Material DEFAULT_MATERIAL = Material.OBSIDIAN;
    private static final Vector centerLocation = getFallbackLocation().toVector();
    private static final float centerX = (float)centerLocation.getX();
    private static final int centerY = (int)centerLocation.getY();
    private static final float centerZ = (float)centerLocation.getZ();
    private static final int offset_from_center = 13;
    private static final int portal_widths = 9;
    private static final int portal_depths = 1;

    protected PortalManager()
    {
        setExecutor(LIST_PORTAL_COMMAND, this);

        TabCompleter portalTabCompleter = new TabCompleter()
        {
            @Override
            public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args)
            {
                List<String> completions = new ArrayList<String>();;

                boolean refreshing = matchesCommand(label, REFRESH_PORTAL_COMMAND);

                if (refreshing || matchesCommand(label, UNREGISTER_PORTAL_COMMAND))
                {
                    Iterator<CuboidPortal> portalIterator = PortalEnum.iterator();
                    while (portalIterator.hasNext())
                    {
                        CuboidPortal p = portalIterator.next();
                        if (p.isRegistered())
                        {
                            completions.add(p.getId());
                        }
                    }
                }

                if (refreshing || matchesCommand(label, REGISTER_PORTAL_COMMAND))
                {
                    Iterator<CuboidPortal> portalIterator = PortalEnum.iterator();
                    while (portalIterator.hasNext())
                    {
                        CuboidPortal p = portalIterator.next();
                        if (!p.isRegistered())
                        {
                            completions.add(p.getId());
                        }
                    }
                }

                if (completions.size() == 0)
                {
                    return null;
                }
                return completions;
            }
        };

        setExecutor(REGISTER_PORTAL_COMMAND, this, portalTabCompleter);
        setExecutor(UNREGISTER_PORTAL_COMMAND, this, portalTabCompleter);
        setExecutor(REFRESH_PORTAL_COMMAND, this, portalTabCompleter);
        Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable()
        {
            public void run()
            {
                initialize();
                new PortalListener();
            }
        }, 2);
    }


    static
    {
        try
        {
            Scanner s = new Scanner(getPortalRegistryFile());
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
    }
    private enum PortalEnum //you can change enum name, but not id
    {
        FP_WARP(new CuboidPortal("Freeplay_Portal", getFallbackWorld(), centerX, centerY, centerZ + offset_from_center, portal_widths, portal_widths, portal_depths,
                PortalDirection.NORTH_SOUTH, 0, new Material[] {Material.GOLD_BLOCK, Material.GOLD_BLOCK, Material.GOLD_BLOCK, Material.GOLD_ORE}, Material.AIR, true)
        {
            @Override
            public void performTeleportAction(Player p)
            {
                WorldType.setTourneyPreferer(p, false);
                if (!sendPlayerToWorld(p, WorldType.SMASH.getBestWorld(p), true))
                {
                    p.sendMessage(ChatColor.RED + "There are no worlds available right now");
                }
            }
        }),
        T_WARP(new CuboidPortal("Tourney_Portal", getFallbackWorld(), centerX + offset_from_center, centerY, centerZ, portal_depths, portal_widths, portal_widths,
                PortalDirection.EAST_WEST, 0, new Material[] {Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK, Material.DIAMOND_BLOCK, Material.DIAMOND_ORE}, Material.AIR, true)
        {
            public void performTeleportAction(Player p)
            {
                WorldType.setTourneyPreferer(p, true);
                if (!sendPlayerToWorld(p, WorldType.SMASH.getBestWorld(p), true))
                {
                    p.sendMessage(ChatColor.RED + "There are no worlds available right now");
                }
            }
        }),
        META_WARP(new CuboidPortal("Meta_World_Portal", getFallbackWorld(), centerX - offset_from_center, centerY, centerZ, portal_depths, portal_widths, portal_widths,
                 PortalDirection.EAST_WEST, 0, new Material[] {Material.IRON_BLOCK, Material.IRON_BLOCK, Material.IRON_BLOCK, Material.IRON_ORE}, Material.AIR, true)
        {
            @Override
            public void performTeleportAction(Player p)
            {
                if (!sendPlayerToWorld(p, WorldType.META.getBestWorld(p), true))
                {
                    p.sendMessage(ChatColor.RED + "There are no worlds available right now");
                }
            }
        });

        private final CuboidPortal portal;

        PortalEnum(CuboidPortal portal)
        {
            this.portal = portal;
        }

        public CuboidPortal getPortal()
        {
            return portal;
        }

        public static void initialize() {}

        public static Iterator<CuboidPortal> iterator()
        {
            return new Iterator<CuboidPortal>()
            {
                PortalEnum[] valueArray = values();
                int index = 0;

                @Override
                public boolean hasNext()
                {
                    return index < valueArray.length;
                }

                @Override
                public CuboidPortal next()
                {
                    return valueArray[index++].getPortal();
                }

                @Override
                public void remove() {

                }
            };
        }
    }

    public static Iterator<CuboidPortal> portalIterator()
    {
        return PortalEnum.iterator();
    }

    public void initialize()
    {
        PortalEnum.initialize();
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
                    if (!portalExists(id) /*uncache non-existent portals*/)
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
                if (portalExists(id))
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
            sendErrorMessage(ChatColor.RED + "File not found for portal registry!");
        }
    }

    private static IDState setRegistered(String portalID, boolean register)
    {
        if (!portalExists(portalID))
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

    private static CuboidPortal getPortal(String id)
    {
        for (PortalEnum portalEnumValue : PortalEnum.values())
        {
            if (portalEnumValue.getPortal().getId().equals(id))
            {
                return portalEnumValue.getPortal();
            }
        }
        return null;
    }

    private static void registerPortal(String id)
    {
        CuboidPortal portal = getPortal(id);
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

    private static File getPortalCacheFile()
    {
        return getServerDataFile("", "portal_cache.json", true);
    }

    private static void unregisterPortal(String id)
    {
        CuboidPortal portal = getPortal(id);
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
                        if (portalExists(val) && !val.equals(id))
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
        return getPortal(id) != null;
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
        return getServerDataFile("", "portal_registry.txt", true);
    }

    private static boolean isRegistered(String portalId)
    {
        return portalRegistry.contains(portalId);
    }

    private static JSONArray getJSONArray(File f)
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

    private static void saveJSONArray(JSONArray arr, File f)
    {
        try
        {
            PrintWriter writer = new PrintWriter(f);
            arr.writeJSONString(writer);
            writer.close();
        }
        catch (IOException ex)
        {//impossible to reach this block
            sendErrorMessage(ChatColor.RED + "Error! Could not write to file for portal data!");
        }

    }

    protected static void cancelPortalTasks(Player p)
    {
        Iterator<CuboidPortal> portalIterator = PortalEnum.iterator();
        while (portalIterator.hasNext())
        {
            portalIterator.next().cancelAndForget(p);
        }
    }

    private static String getPortalListForOutput()
    {
        String portalList = "";
        Iterator<CuboidPortal> portalIterator = PortalEnum.iterator();
        while (portalIterator.hasNext())
        {
            CuboidPortal portal = portalIterator.next();
            if (portal.isRegistered())
            {
                portalList += ChatColor.GREEN + portal.getId() + "(" + portal.centerX + ", " + portal.centerY + ", " + portal.centerZ + ")" + ChatColor.WHITE + ", ";
            }
            else
            {
                portalList += ChatColor.RED + portal.getId() + ChatColor.WHITE + ", ";
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
        if (matchesCommand(label, LIST_PORTAL_COMMAND))
        {
            if (portalRegistry.size() <= 0)
            {
                if (!PortalEnum.iterator().hasNext())
                {
                    sender.sendMessage(ChatColor.YELLOW + "There are no portals.");
                }
                else
                {
                    sender.sendMessage(ChatColor.YELLOW + "There are no "+ ChatColor.ITALIC + "registered" + ChatColor.RESET + "" + ChatColor.YELLOW + " portals. Use /addportal to enable them.");
                }
            }
            sender.sendMessage(ChatColor.GRAY + "List of portals: " + getPortalListForOutput());
            return true;
        }
        else
        {
            boolean register = matchesCommand(label, REGISTER_PORTAL_COMMAND);
            boolean unregister = matchesCommand(label, UNREGISTER_PORTAL_COMMAND);
            if (register || unregister || matchesCommand(label, REFRESH_PORTAL_COMMAND))
            {
                if (args.length < 1 || args.length > 2)
                {
                    return false;
                }
                try
                {
                    int i = 0;
                    String portalID = args[i++];
                    boolean refreshAll = portalID.equalsIgnoreCase("all");
                    if (!refreshAll)
                    {
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
                        if (state == IDState.NONEXISTENT)
                        {
                            sender.sendMessage(ChatColor.RED + "CuboidPortal " + portalID + " not found! Here is the list of portals: " + getPortalListForOutput());
                            return true;
                        }
                        else if (state == IDState.UNCHANGED)
                        {
                            sender.sendMessage(ChatColor.RED + "CuboidPortal " + portalID + " was already " + r + "!");
                        }
                        else if (state == IDState.CHANGED)
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
                        if (teleport && sender instanceof Player && portalExists(portalID))
                        {
                            Player p = (Player) sender;
                            p.teleport(getPortal(portalID).getEntrancePoint());
                        }

                    }
                    else
                    {
                        Iterator<CuboidPortal> portalIterator = PortalEnum.iterator();
                        int portalCount = 0;
                        while (portalIterator.hasNext())
                        {
                            portalCount++;
                            portalIterator.next().refresh();
                        }
                        sender.sendMessage(ChatColor.GREEN + "Refreshed all " + portalCount + " portals!");
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

    abstract static class CuboidPortal
    {
        private final String id;
        private final List<Player> dwellers;
        private static final float ENTRANCE_OFFSET = 3f;
        private final Random r = new Random();
        private final Material[] buildingMaterials;
        private final Material portalCenterBlock;
        private final long tpDelayTicks;
        private final World world;
        private final PortalDirection orientation;
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

        public CuboidPortal(String id, World w, float centerX, int baseY, float centerZ,
                            int width, int height, int depth, PortalDirection orientation, Material[] buildingMaterials, Material portalCenterBlock)
        {
            this(id, w, centerX, baseY, centerZ, width, height, depth, orientation, 0, buildingMaterials, portalCenterBlock, true);
        }

        public CuboidPortal(String id, World w, float centerX, int baseY, float centerZ,
                            int width, int height, int depth, PortalDirection orientation, Material[] buildingMaterials)
        {
            this(id, w, centerX, baseY, centerZ, width, height, depth, orientation, buildingMaterials, Material.AIR);
        }

        public CuboidPortal(String id, World w, float centerX, int baseY, float centerZ,
                            int width, int height, int depth, PortalDirection orientation)
        {
            this(id, w, centerX, baseY, centerZ, width, height, depth, orientation, new Material[] {DEFAULT_MATERIAL}, Material.AIR);
        }

        public CuboidPortal(String id, World w, Vector center, int width, int height, int depth, PortalDirection orientation, int tpDelayTicks, Material[] buildingMaterials, Material portalCenterBlock)
        {
            this(id, w, (float)center.getX(), (int)center.getY(), (float)center.getZ(), width, height, depth, orientation, tpDelayTicks, buildingMaterials, portalCenterBlock, true);
        }

        private CuboidPortal(String id, World w, float centerX, int baseY, float centerZ,
                             int width, int height, int depth, PortalDirection orientation, int tpTickDelay, Material[] buildingMaterials, Material portalCenterBlock, boolean buildOnCreate)
        {
            this.dwellers = new ArrayList<Player>();

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
            westI = getBlockCoord(westIf);
            float eastIf = 2*centerX - westIf;
            eastI = getBlockCoord(eastIf);

            yMin = centerY - (float)height/2;
            float bottomJf = yMin + 0.5f;
            bottomJ = getBlockCoord(bottomJf);
            float topJf = 2*centerY - bottomJf;
            topJ = getBlockCoord(topJf);

            zMin = centerZ - (float)depth/2;
            float northKf = zMin + 0.5f;
            northK = getBlockCoord(northKf);;//the oppressed
            float southKf = 2*centerZ - northKf;
            southK = getBlockCoord(southKf);//the free


            Block testBlock;
            float offset = -ENTRANCE_OFFSET;

            switch (orientation)
            {
                case NORTH_SOUTH:
                    testBlock = w.getBlockAt(getBlockCoord(centerX), baseY, getBlockCoord(centerZ - 1));
                    offset = (testBlock.getType().isSolid() || !testBlock.getRelative(0, -1, 0).getType().isSolid()) ? -offset : offset;
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
                    testBlock = w.getBlockAt(getBlockCoord(centerX - 1), baseY, getBlockCoord(centerZ));
                    offset = (testBlock.getType().isSolid() || !testBlock.getRelative(0, -1, 0).getType().isSolid()) ? -offset : offset;
                    entrancePoint = new Location(w, centerX + offset, baseY, centerZ);
                    break;
                default:
                    entrancePoint = new Location(w, centerX, baseY, centerZ);
                    break;
            }
            entrancePoint.setDirection(new Vector(centerX, baseY, centerZ).subtract(entrancePoint.toVector()));


            this.world = w;
            this.orientation = orientation;

            this.buildingMaterials = buildingMaterials == null ? new Material[] {DEFAULT_MATERIAL} : buildingMaterials;
            this.portalCenterBlock = portalCenterBlock == null ? Material.AIR : portalCenterBlock;

            //log the original materials to the cache
            if (buildOnCreate)
            {
                isBuilt = false;
                for (Object curObj : getJSONArray(getPortalCacheFile()))
                {
                    if (curObj instanceof JSONObject)
                    {
                        JSONObject curJason = (JSONObject)curObj;
                        Object idValue = curJason.get("id");
                        if (idValue != null && idValue.equals(id))
                        {
//                            Set<Map.Entry<Object, Object>> entrySet = curJason.entrySet();
//                            for (Map.Entry<Object, Object> entry : entrySet)
//                            {
//                                String coordString = (String)entry.getKey();
//                                String[] coords = coordString.split(",");
//                                try
//                                {
//                                    if (coords.length == 3)
//                                    {
//                                        Block curBlock = w.getBlockAt((int)((float)Float.valueOf(coords[0])), (int)((float)Float.valueOf(coords[1])), (int)((float)Float.valueOf(coords[2])));
//                                        //curBlock.setType(Material.getMaterial((String)entry.getValue()));
//                                    }
//                                }
//                                catch (NumberFormatException ex)
//                                {
//                                    ex.printStackTrace();
//                                }
//                            }
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
            else
            {
                isBuilt = true;
            }

//            //logPortal log portal LOG THE FREAKING PORTAL
//            allPortals.put(id, this);
        }

        public void setDwelling(Player p, boolean inside)
        {
            boolean alreadyInside = hasDweller(p);
            if (alreadyInside && !inside)
            {
                dwellers.remove(p);
            }
            else if (!alreadyInside && inside)
            {
                dwellers.add(p);
            }
        }

        public boolean hasDweller(Player p)
        {
            return dwellers.contains(p);
        }

        private boolean valid()
        {
            return world != null;
        }

        public abstract void performTeleportAction(Player p);

        private int getBlockCoord(float blockCoord)
        {
            return blockCoord < 0 ? (int)blockCoord - 1 : (int)blockCoord;
        }

        public boolean contains(Location l)
        {
            if (!isRegistered())
            {
                return false;
            }

            float pX = (float)l.getX();
            float pY = (float)l.getY();
            float pZ = (float)l.getZ();

            switch (orientation)
            {
                case EAST_WEST: return Math.abs(pX - centerX)*2 <= width && Math.abs(pY - centerY)*2 + 2f <= height && Math.abs(pZ - centerZ)*2 + 2f <= depth;
                case NORTH_SOUTH: return Math.abs(pX - centerX)*2 + 2f <= width && Math.abs(pY - centerY)*2 + 2f <= height && Math.abs(pZ - centerZ)*2 <= depth;
                case UP_DOWN: return Math.abs(pX - centerX)*2 + 2f <= width && Math.abs(pY - centerY)*2 <= height && Math.abs(pZ - centerZ)*2 + 2f <= depth;
                default:
                    sendErrorMessage(ChatColor.RED + "Error! Had an invalid orientation when deciding if " + l.toVector().toString() + " was in portal " + id + " or not.");
                    return false;
            }
        }


        public boolean containsPlayer(Player p)
        {
            return contains(p.getLocation());
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
                sendErrorMessage(ChatColor.RED + "Error! Invalid world for portal " + id);
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
                                    Block b = world.getBlockAt((int)(float)Float.valueOf(locArray[0]), (int)(float)Float.valueOf(locArray[1]), (int)(float)Float.valueOf(locArray[2]));
                                    b.setType(Material.getMaterial(mat));
                                }
                            }
                        }
                    }
                }

                if (!justBlocks)
                {
                    removeCacheData();
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
                sendErrorMessage(ChatColor.RED + "Error! Invalid world for portal " + id);
                return;
            }
            if (!isBuilt())
            {
                JSONObject jasonVoorhees = null;
                boolean newBuild = false;
                File f = getPortalCacheFile();
                JSONArray mainArray = getJSONArray(f);

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
                            Block blockToChange = world.getBlockAt(i, j, k);
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

        public void cancelAndForget(Player p)
        {
            if (halfMaterializedPlayers.containsKey(p))
            {
                Bukkit.getScheduler().cancelTask(halfMaterializedPlayers.get(p));
                halfMaterializedPlayers.remove(p);
            }
            if (dwellers.contains(p))
            {
                dwellers.remove(p);
            }
        }

        private void removeCacheData()
        {
            File f = getPortalCacheFile();
            if (f == null)
            {
                sendErrorMessage(ChatColor.RED + "Error! Could not find the portal_cache file!");
                return;
            }
            JSONArray array = getJSONArray(f);
            if (array == null)
            {
                sendErrorMessage(ChatColor.RED + "Could not fetch the JSON array for the portals!");
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

        public Location getEntrancePoint()
        {
            return entrancePoint;
        }

        public void refresh()
        {
            destroy(true);
            build();
        }

        public long getTpDelayTicks()
        {
            return tpDelayTicks;
        }

        public String getId()
        {
            return id;
        }
    }
}