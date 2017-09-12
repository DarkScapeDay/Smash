package me.happyman.worlds;

import me.happyman.Plugin;
import me.happyman.utils.SmashManager;
import me.happyman.utils.Verifier;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

import static me.happyman.Plugin.*;
import static me.happyman.utils.FileManager.getRoot;
import static me.happyman.utils.FileManager.getWorldFolder;
import static me.happyman.worlds.WorldType.getWorldType;

public class WorldManager implements CommandExecutor
{
    protected static final int MOTD_PERIOD = 240; //The period between a random motd message being sent to non-started Smash worlds
    //You probable shouldn't change these ones.
    public static final String LIST_WORLD_CMD = "listworlds"; //This lists all Smash worlds
    public static final String LIST_MAP_CMD = "listmaps"; //This lists the maps that you can use when creating a world
    public static final String FIND_CMD = "find"; //Gets the world you're in (or another player)
    public static final String JOIN_CMD = "join"; //Sends the sender to the world if that's okay
    public static final String NAV_COMMAND = "nav"; //Excecuting this command will open the Smash gui if that's okay
    public static final String DELETE_WORLD_CMD = "deleteworld"; //This deletes a world
    public static final String LEAVE_CMD = "leave"; //This causes you to leave the game you're in
    public static final String DEATHMATCH_PREFERENCE_DATANAME = "Deathmatch"; //The colorlessName of the data which is for remembering people's deathmatch preferences
    static final String CREATE_WORLD_CMD = "createworld"; //This creates a world

    private static final ArrayList<String> lockedWorlds = new ArrayList<String>();
    protected static final ArrayList<String> motdMessages = new ArrayList<String>(); //The list of messages that can be displayed after each motd period (just one, please)

//    public static final ItemStack SMASH_GUI_ITEM = Plugin.getCustomItemStack(Material.BRICK, ChatColor.YELLOW + "" + ChatColor.BOLD + "Smash Worlds");

    public WorldManager()
    {
        new SmashWorldManager();
        new StatCommandExecutor();
        setExecutor(NAV_COMMAND, this);
        setExecutor(LEAVE_CMD, this);
        setExecutor(DELETE_WORLD_CMD, this);
        setExecutor(LIST_WORLD_CMD, this);
        setExecutor(LIST_MAP_CMD, this);
        setExecutor(JOIN_CMD, this, new TabCompleter()
        {
            @Override
            public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args)
            {
                if (sender instanceof Player && Plugin.matchesCommand(label, JOIN_CMD))
                {
                    List<String> completions = new ArrayList<String>();
//                String cur = null;
//                if (args.length > 0)
//                {
//                    cur = args[0].toLowerCase();
//                }
                    if (args.length == 0)
                    {
                        return completions;
                    }

                    for (World w : Bukkit.getWorlds())
                    {
                        String whatToAdd;
                        WorldType type = getWorldType(w);
                        if (type.isValid())
                        {
                            whatToAdd = ChatColor.RESET + "" + type.getDisplayName(w) + ChatColor.WHITE;
                            completions.add(whatToAdd);
                        }
//                    else
//                    {
//                        ChatColor color = ChatColor.GRAY;
//                        if (getFallbackWorld() != null && getFallbackWorld().equals(w))
//                        {
//                            color = ChatColor.DARK_PURPLE;
//                        }
//                        whatToAdd = color + w.getName();
//                    }
                    }

                    if (completions.size() > 0)
                    {
                        completions.add(0, "");
                    }
                    return completions;
                }
                return null;
            }
        });
        setExecutor(WorldManager.CREATE_WORLD_CMD,
        new CommandExecutor()
        {
            @Override
            public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
            {
                if (args.length <= 0)
                {
                    return false;
                }
                String[] newArgs = new String[args.length - 1];
                for (int newArgIndex = 0, oldArgIndex = 1; oldArgIndex < args.length; oldArgIndex++, newArgIndex++)
                {
                    newArgs[newArgIndex] = args[oldArgIndex];
                }
                return getWorldType(args[0]).performCreateWorldCommand(sender, newArgs);
            }
        },
        new TabCompleter()
        {
            @Override
            public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings)
            {
                List<String> result = new ArrayList<String>();
                for (WorldType type : WorldType.values())
                {
                    if (type.isValid())
                    {
                        result.add(type.name());
                    }
                }
                return result;
            }
        });


        for (final WorldType type : WorldType.values())
        {
            if (type.isValid())
            {
                File folder = getRoot();
                File[] worldList = folder.listFiles(new FilenameFilter()
                {
                    public boolean accept(File file, String s)
                    {
                        return file.isDirectory() && getWorldType(s) == type;
                    }
                });
                if (worldList != null)
                {
                    for (File worldFile : worldList)
                    {
                        String worldName = worldFile.getName();
                        if (Bukkit.getWorld(worldName) == null)
                        {
                            if (type.createWorld(worldName) == null)
                            {
                                worldFile.delete();
                            }
                        }
                    }
                }
            }
        }
        for (final World w : Bukkit.getWorlds())
        {
            WorldType type = getWorldType(w);
            if (type.isValid() && !type.getWorlds().contains(w))
            {
                type.performLoadWorldAction(w, false);
            }
            for (Player p : w.getPlayers())
            {
                WorldType.setSpectatorMode(p, false);
            }
        }
        for (final Player p : Bukkit.getOnlinePlayers())
        {
            if (getLastWorld(p) == null)
            {
                resetPlayer(p);
                p.teleport(getFallbackLocation());
            }

//            WorldManager.getTabList(p).clearCustomTabs();
        }
    }

    public static void resetPlayer(Player p)
    {
        getWorldType(p.getWorld()).performResetAction(p);
    }

    public static void setAttackSource(Entity entity, WorldType.AttackSource item)
    {
        getWorldType(entity.getWorld()).setAttackSource(entity, item);
    }

    public static WorldType.AttackSource getAttackSource(Entity entity)
    {
        return getWorldType(entity.getWorld()).getAttackSource(entity);
    }

    public static boolean isBeingDeleted(String world)
    {
        return getWorldType(world).isBeingDeleted(world);
    }

    public static boolean isBeingDeleted(World world)
    {
        return isBeingDeleted(world.getName());
    }

    protected static void setLocked(World w, boolean locked)
    {
        setLocked(w.getName(), locked);
    }

    protected static void setLocked(String w, boolean locked)
    {
        if (!lockedWorlds.contains(w) && locked)
        {
            lockedWorlds.add(w);
        }
        else if (lockedWorlds.contains(w) && !locked)
        {
            lockedWorlds.remove(w);
        }
    }

    protected static boolean worldIsLocked(World w)
    {
        return lockedWorlds.contains(w.getName());
    }

    public static void sendPlayerToLastWorld(Player p, boolean forcefully)
    {
        MainListener.sendPlayerToWorld(p, PlayerStateRecorder.getLastWorld(p), forcefully);
    }

    public static World getLastWorld(Player p)
    {
        return PlayerStateRecorder.getLastWorld(p);
    }

    public static String smashifyArgument(String arg)
    {
        arg = arg.toLowerCase();
        for (WorldType type : WorldType.values())
        {
            String whatShouldBe = ChatColor.stripColor(type.getDrainWorldPrefix()).toLowerCase();
            if (whatShouldBe.startsWith(arg) && !arg.startsWith(whatShouldBe))
            {
                arg = type.getDrainWorldPrefix() + arg;
            }
        }
        return arg;
    }

    public static String getDisplayName(World world)
    {
        return getWorldType(world).getDisplayName(world);
    }

    private static void addTabEntry(Player p, String entry)
    {
//        entry = ChatColor.stripColor(entry);
//        PlayerList playerList = getTabList(p);
//        List<String> oldTabs = playerList.getCustomTabs();
//        if (!oldTabs.contains(entry) && oldTabs.size() < 20)
//        {
//            int j = oldTabs.size();
//            oldTabs.add(entry);
//            for (int i = 0; i < oldTabs.size(); i++)
//            {
//                while (playerList.getTabName(j) != null)
//                {
//                    j++;
//                }
//                if (j < 20)
//                {
//                    playerList.updateSlot(j, getSpectralEntry(entry));
//                }
//            }
//        }
    }

    private static String getSpectralEntry(String entry)
    {
        return ChatColor.GRAY + "" + ChatColor.ITALIC + entry;
    }

//    public static PlayerList getTabList(Player p)
//    {
//        if (!tabLists.containsKey(p))
//        {
//            tabLists.put(p, new PlayerList(p, PlayerList.SIZE_DEFAULT));
//        }
//        return tabLists.get(p);
//    }

    private static void removeTabEntry(Player p, String entry)
    {
//        entry = ChatColor.stripColor(entry);
//        PlayerList playerList = getTabList(p);
//        List<String> oldTabs = playerList.getCustomTabs();
//        List<String> tabNames = new ArrayList<String>();
//        for (int i = 0; i < playerList.getCustomTabs().size(); i++)
//        {
//            tabNames.add(playerList.getTabName(i));
//        }
//        if (oldTabs.contains(entry))
//        {
//            playerList.clearCustomTabs();
//            int j = 0;
//            for (int i = 0; i < oldTabs.size(); i++)
//            {
//                while (j < tabNames.size() - 1 && tabNames.get(j) == null)
//                {
//                    j++;
//                }
//                if (!oldTabs.get(i).equals(entry))
//                {
//                    playerList.updateSlot(j, getSpectralEntry(oldTabs.get(i)));
//                }
//                j++;
//            }
//        }
    }

    private static void addAllTabEntries(Player p)
    {
        for (Player online : Bukkit.getOnlinePlayers())
        {
            if (!WorldType.isInSpectatorMode(online))
            {
                addTabEntry(online, p.getName());
            }
        }
    }

    private static void removeAllTabEntries(Player p)
    {
        for (Player online : Bukkit.getOnlinePlayers())
        {
            if (!WorldType.isInSpectatorMode(online))
            {
                removeTabEntry(online, p.getName());
            }
        }
    }

    /**
     * Function: playSoundToPlayers
     * Purpose: To play a certain sound of a certain pitch and volume in the relative directoin of the sound Plugin player and a certain number of times with a certain delay between those times (just trust me, it's cool)
     * @param playerToExclude - The player who is "generating" the sound
     * @param players - The list of players to whom to play the sound (typically all the players in a Smash world). This can include the sound Plugin player.
     * @param sound - The sound that we want to play to the players
     * @param volume - The volume of the sound (max is 1)
     * @param pitch - The pitch of the sound (max is 1)
     * @param ticksBetweenCalls - The number of ticks between sound calls
     * @param timesToCall - The number of times that the sound should be played
     */
    public static void playSoundToPlayers(final Player playerToExclude, Location soundSourceLocation, final List<Player> players, final Sound sound, final float volume, final float pitch, final int ticksBetweenCalls, final int timesToCall)
    {
        for (final Player player : players)
        {
            Location soundLocation = player.getLocation();
            if (playerToExclude == null || !player.equals(playerToExclude))
            {
                Vector relativeDirection = SmashManager.getUnitDirection(player.getLocation(), soundSourceLocation);
                int newDistance = 7; //The number of blocks away we want the sound to be
                soundLocation.setX(soundLocation.getX() + relativeDirection.getX() * newDistance);
                soundLocation.setY(soundLocation.getY() + relativeDirection.getY() * newDistance);
                soundLocation.setZ(soundLocation.getZ() + relativeDirection.getZ() * newDistance);
            }
            final Location actualLocation = soundLocation;
            for (int i = 0; i < timesToCall; i++)
            {
                Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
                    public void run()
                    {
                        player.playSound(actualLocation, sound, volume, pitch);
                    }
                }, i*ticksBetweenCalls);
            }
        }
    }

    /**
     * Function: playSoundToPlayers
     * Purpose: To play a certain sound of a certain pitch and volume in the relative direction of the sound Plugin player (just trust me, it's cool)
     * @param playerToExclude - The player who is "generating" the sound
     * @param players - The list of players to whom to play the sound (typically all the players in a Smash world). This can include the sound Plugin player.
     * @param sound - The sound that we want to play to the players
     * @param volume - The volume of the sound (max is 1)
     * @param pitch - The pitch of the sound (max is 1)
     */
    public static void playSoundToPlayers(Player playerToExclude, List<Player> players, Sound sound, float volume, float pitch)
    {
        playSoundToPlayers(playerToExclude, playerToExclude.getLocation(), players, sound, volume, pitch, 0, 1);
    }

    public static void playSoundToPlayers(List<Player> players, Location soundSourceLocation, Sound sound, float volume, float pitch)
    {
        playSoundToPlayers(null, soundSourceLocation, players, sound, volume, pitch, 0, 1);
    }

    //    /**
//     * Function: getSmashGui
//     * Purpose: To get the inventory that shows the worlds that a player can join with items representing differnet worlds.
//     * This inventory also includes items for creating worlds for staff members
//     *
//     * @param p - The player for which to get the world gui
//     * @return - The inventory of the world gui
//     */
//    private static KeptInventory getSmashGui(Player p)
//    {
//        ArrayList<ItemStack> representingItems = new ArrayList<ItemStack>();
//        for (World w : getAvaliableWorlds(p))
//        {
//            representingItems.add(getWorldRepresenter(w, p));
//        }
//
//        if (hasSpecialPermissions(p))
//        {
//            for (WorldType type : WorldType.values())
//            {
//                if (type.isValid() && type.canCreateWorlds())
//                {
//                    ItemStack[] creationItems = generateWorldCreatorItems(type);
//                    representingItems.addAll(Arrays.asList(creationItems));
//                }
//            }
//        }
//
//        int worldCount = representingItems.size();
//        int rows = worldCount/9 + 1;
//        if (worldCount % 9 == 0)
//        {
//            rows -= 1;
//        }
//        KeptInventory worldInventory = Bukkit.createInventory(null, rows*9, getWorldGuiLabel(p));
//        for (int i = 0; i < representingItems.size(); i++)
//        {
//            worldInventory.setItem(i, representingItems.get(i));
//        }
//        return worldInventory;
//    }

//    /**
//     * Function: hasWorldGuiOpen
//     * Purpose: To determine if a player has a world gui open
//     *
//     * @param p - The player for which to check if he has the world gui
//     * @return - True if the player has a world gui open
//     */
//    public static boolean hasWorldGuiOpen(Player p)
//    {
//        return p.getOpenInventory().getTitle().equals(getWorldGuiLabel(p));
//    }


    //    private static void hideSpectatorsFromPlayers(List<Player> players)
//    {
//        for (Player p : players)
//        {
//            if (isInSpectatorMode(p))
//            {
//                hideSpectatorFromPlayers(p, players);
//            }
//        }
//    }

    //
//    public static String getShortWorldName(World w)
//    {
//        return getShortWorldName(w.getName());
//    }

//    public static String getShortWorldName(String w)
//    {
//        WorldType category = getWorldType(w);
//        String s = w.replace(category.getDrainWorldPrefix(), "");
//        String whatToUseReplace = "";
//        if (s.length() < 6)
//        {
//            whatToUseReplace = "Game";
//        }
//        s = s.replaceAll(WORLD_INDICATOR, whatToUseReplace);
//        return s;
//    }

    //    public static boolean isWorldRepresenter(ItemStack itemClicked)
//    {
//        if (!itemClicked.getItemMeta().hasDisplayName())
//        {
//            return false;
//        }
//        String name = itemClicked.getItemMeta().getDisplayName();
//        for (WorldType type : WorldType.values())
//        {
//            if (type.isValid() && name.contains(type.getDisplayIndicator()))
//            {
//                return true;
//            }
//        }
//        return !name.toLowerCase().contains("create") && (name.toLowerCase().contains("world"));
//    }

//    static boolean isWorldCreator(ItemStack itemClicked)
//    {
//        if (!itemClicked.hasItemMeta() && !itemClicked.getItemMeta().hasDisplayName())
//        {
//            return false;
//        }
//        String itemName = itemClicked.getItemMeta().getDisplayName();
//        if (!itemName.contains(WORLD_CREATION_ITEM_PREFIX))
//        {
//            return false;
//        }
//        for (WorldType type : WorldType.values())
//        {
//            if (type.isValid() && itemName.contains(type.getDisplayIndicator()))
//            {
//                return true;
//            }
//        }
//        return false;
//    }

//    /**
//     * Function:
//     * Purpose:
//     *
//     * @param item
//     * @return
//     */
//    public static World getWorldRepresented(ItemStack item)
//    {
//        if (isWorldCreator(item))
//        {
//            for (WorldType type : WorldType.values())
//            {
//                World representedWorld = type.getWorldRepresented(item);
//                if (representedWorld != null)
//                {
//                    return representedWorld;
//
//                    Integer tourneyLevel = null;
//                    if (type.hasTourneyLevel())
//                    {
//                        if (!item.hasItemMeta() || !item.getItemMeta().hasLore() || item.getItemMeta().getLore().size() < 1)
//                        {
//                            sendErrorMessage("Error! Could not get a new world from item " + item.getItemMeta().getDisplayName());
//                            return null;
//                        }
//                        else
//                        {
//                            String lore = ChatColor.stripColor(item.getItemMeta().getLore().get(0));
//                            String roundNumberString = "";
//                            for (int i = 0; i < lore.length(); i++)
//                            {
//                                char c = lore.charAt(i);
//                                if (Character.isDigit(c))
//                                {
//                                    roundNumberString += c;
//                                }
//                                else
//                                {
//                                    break;
//                                }
//                            }
//                            if (roundNumberString.equals(""))
//                            {
//                                sendErrorMessage("Error! Could not get the round number from item " + item.getItemMeta().getDisplayName());
//                                return null;
//                            }
//                            tourneyLevel = Integer.valueOf(roundNumberString);
//                        }
//                    }
//                    return type.createWorld(tourneyLevel);
//                }
//            }
//            return null;
//        }
//
//        String name = ChatColor.stripColor(item.getItemMeta().getDisplayName());
//        int worldNumber = -1;
//        WorldType worldType = WorldType.INVALID;
//        for (WorldType type : WorldType.values())
//        {
//            if (type.isValid() && name.contains(ChatColor.stripColor(type.getDisplayIndicator())))
//            {
//                if (type.getMaxWorlds() > 1)
//                {
//                    int firstIndex = name.length() - 1;
//                    while (Character.isDigit(name.charAt(firstIndex - 1)))
//                    {
//                        firstIndex--;
//                    }
//                    String numberOfWorldString = name.substring(firstIndex, name.length());
//
//                    worldNumber = Integer.valueOf(numberOfWorldString);
//                }
//                else
//                {
//                    worldNumber = WorldType.LOWEST_WORLD_NUM;
//                }
//
//                worldType = type;
//                break;
//            }
//        }
//
//        if (worldType.isValid())
//        {
//            World w = getWorld(worldType, worldNumber);
//            if (w == null)
//            {
//                sendErrorMessage("Error! The world that that item represents could not be found!");
//                return getFallbackWorld();
//            }
//            return w;
//        }
//
//        return null;
//    }


    @Override
    public boolean onCommand(final CommandSender sender, Command cmd, final String label, String[] args)
    {
        if (matchesCommand(label, WorldManager.NAV_COMMAND))
        {
            if (sender instanceof Player)
            {
                WorldType.openSmashGui((Player)sender, true);
            }
            else
            {
                sendErrorMessage("You are not in game!");
            }
            return true;
        }
        else if (matchesCommand(label, WorldManager.LEAVE_CMD))
        {
            if (!(sender instanceof Player))
            {
                sendErrorMessage(ChatColor.RED + "You are not in-game. Therefore, you are not in a Smash game.");
                return true;
            }
            Player p = (Player)sender;

            World lastWorld = WorldManager.getLastWorld(p);
            if (getWorldType(p.getWorld()).isValid() && lastWorld != p.getWorld())
            {
                MainListener.sendPlayerToWorld(p, lastWorld, false);
            }
            else
            {
                MainListener.sendPlayerToWorld(p, getFallbackWorld(), false);
            }
            return true;
        }
        else if (matchesCommand(label, WorldManager.DELETE_WORLD_CMD))
        {
            if (args.length == 0)
            {
                if (!(sender instanceof Player))
                {
                    sendErrorMessage("You must specify which world to delete, since you're not in one");
                    WorldType.listWorlds(sender);
                }
                else
                {
                    final Player p = (Player)sender;

                    new Verifier.BooleanVerifier(p,
                            ChatColor.YELLOW + "Do you really want to delete the world you're in right now (" + getWorldType(p.getWorld()).getDisplayName(p.getWorld()) + ChatColor.YELLOW + ")?")
                    {
                        @Override
                        public void performYesAction()
                        {
                            if (WorldType.deleteWorld(p.getWorld()))
                            {
                                p.sendMessage(ChatColor.GREEN + "You deleted the world you were in.");
                            }
                            else
                            {
                                p.sendMessage(ChatColor.YELLOW + "You cannot delete the world you are in...");
                            }
                        }

                        @Override
                        public void performNoAction()
                        {
                            p.sendMessage(ChatColor.GREEN + "Command /" + label + " cancelled!");
                        }
                    };
                }
            }
            else
            {
                if (args[0].equalsIgnoreCase("help"))
                {
                    return false;
                }
                World w = Bukkit.getWorld(args[0]);
                String smashingArg = w == null ? WorldManager.smashifyArgument(args[0]) : w.getName();
                w = w == null ? Bukkit.getWorld(smashingArg) : w;
                if (w == null)
                {
                    File f = getWorldFolder(smashingArg);
                    if (f.exists())
                    {
                        sender.sendMessage(ChatColor.GREEN + "World wasn't found, save for its folder");
                        f.delete();
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.RED + "World " + args[0] + " not found!");
                    }
                }
                else
                {
                    if (WorldType.deleteWorld(w))
                    {
                        sender.sendMessage(ChatColor.GREEN + "You deleted world " + getWorldType(w).getDisplayName(w) + ChatColor.GREEN + ".");
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.YELLOW + "You were not allowed to delete " + w.getName());
                    }
                }
            }
            return true;
        }
        else if (matchesCommand(label, WorldManager.LIST_WORLD_CMD))
        {
            if (getFallbackWorld() != null)
            {
                sender.sendMessage(ChatColor.BLUE + "" + ChatColor.ITALIC + "Fallback world: " + getFallbackWorld().getName());
            }
            sender.sendMessage(ChatColor.GREEN + "" + ChatColor.BOLD + "World List:");
            WorldType.listWorlds(sender);
            return true;
        }
        else if (matchesCommand(label, WorldManager.JOIN_CMD))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage(ChatColor.RED + "You are not in game!");
            }
            else if (args.length == 0)
            {
                sender.sendMessage(ChatColor.GREEN + "You are currently in " + getWorldType(((Player)sender).getWorld()).getDisplayName(((Player)sender).getWorld()));
                sender.sendMessage(ChatColor.GREEN + "Example: /" + WorldManager.JOIN_CMD + " smash1");
                WorldType.listWorlds(sender);
            }
            else if (args[0].equalsIgnoreCase("help"))
            {
                return false;
            }
            else
            {
                String argComp = "";
                for (String s : args)
                {
                    argComp += " " + s;
                }
                if (argComp.length() > 0)
                {
                    argComp = argComp.substring(1, argComp.length());
                }

                World destination = Bukkit.getWorld(argComp);
                if (destination == null)
                {
                    argComp = argComp.toLowerCase();
                    WorldType chosenType = WorldType.INVALID;

                    typeLoop:
                    for (WorldType worldType : WorldType.values())
                    {
                        if (worldType.isValid())
                        {
                            if (ChatColor.stripColor(worldType.getDrainWorldPrefix()).toLowerCase().startsWith(argComp))
                            {
                                chosenType = worldType;
                                break;
                            }
                            for (World world : worldType.getWorlds())
                            {
                                if (ChatColor.stripColor(worldType.getDisplayPrefix(world)).toLowerCase().startsWith(argComp))
                                {
                                    chosenType = worldType;
                                    break typeLoop;
                                }
                            }
                        }
                    }

                    if (chosenType.isValid())
                    {
                        try
                        {
                            int chosenWorldNum;

                            if (chosenType.getMaxWorlds() == 1)
                            {
                                chosenWorldNum = WorldType.LOWEST_WORLD_NUM;
                            }
                            else
                            {
                                StringBuilder inputWorldNum = new StringBuilder();
                                for (int i = 0; i < argComp.length(); i++)
                                {
                                    char c = argComp.charAt(i);

                                    if (!Character.isDigit(c))
                                    {
                                        if (inputWorldNum.toString().length() > 0)
                                        {
                                            break;
                                        }
                                    }
                                    else
                                    {
                                        inputWorldNum.append(c);
                                    }
                                }
                                chosenWorldNum = Integer.valueOf(inputWorldNum.toString());
                            }

                            destination = chosenType.getWorld(chosenWorldNum);
                        }
                        catch (NumberFormatException ex)
                        {
                            ex.printStackTrace();
                        }
                    }
                }

                if (destination == null)
                {
                    sender.sendMessage(ChatColor.RED + "World \"" + argComp + "\" does not exist!");
                }
                else
                {
                    MainListener.sendPlayerToWorld((Player)sender, destination, false);
                }
            }
            return true;
        }
        return false;
    }
}
