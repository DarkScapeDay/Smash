package me.happyman.Other;


import me.happyman.utils.FileManager;
import me.happyman.utils.Verifier;
import me.happyman.worlds.WorldManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static me.happyman.Plugin.*;

public class ForcefieldManager implements CommandExecutor
{
    private static final String ENABLE_FF_CMD = "enableforcefield";
    private static final String REMOVE_FF_CMD = "removeforcefield";
    private static final String DISABLE_FF_CMD = "disableforcefield";
    private static final String ADD_FF_EXEMPT_TEAM_CMD = "ffexempt";
    private static final HashMap<World, Forcefield> forcefields = new HashMap<World, Forcefield>();
    private static final String SETTING_FILE = "ForcefieldExemptTeams.txt";
    private static final List<String> exemptTeams = new ArrayList<String>();

    public static boolean isOutsideForcefield(Player p)
    {
        Forcefield forcefield = forcefields.get(p.getWorld());
        if (forcefield != null)
        {
            return forcefield.isOutsideBounds(p);
        }
        return false;
    }

    public ForcefieldManager()
    {
        try
        {
            File f = getSettingFile();
            Scanner scanner = new Scanner(f);
            scanner.useDelimiter("\\Z");
            String[] stringList = scanner.next().split(", ");
            exemptTeams.addAll(Arrays.asList(stringList));
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        catch (NoSuchElementException ex) {}

        setExecutor(ENABLE_FF_CMD, this);
        setExecutor(ADD_FF_EXEMPT_TEAM_CMD, this);
        setExecutor(DISABLE_FF_CMD, this);
        setExecutor(REMOVE_FF_CMD, this);
    }

    private static File getSettingFile()
    {
        return FileManager.getServerDataFile("", SETTING_FILE, true);
    }

    private static List<String> getExemptTeams()
    {
        return exemptTeams;
    }

    public static abstract class Forcefield
    {
        public static final float DEFAULT_DAMAGE = 7;
        public static final int DEFAULT_TICK_INTERVAL = 1;
        private static final long WARNING_TICK_INTERVAL = 10;
        private static Integer numberEnabled = 0;
        private final float centerX;
        private final float centerY;
        private final float centerZ;
        private final float damage;
        private final int tickInterval;
        private final World world;
        private Integer damageTask;
        private static Integer warningTask;

        private Forcefield(World world, float centerX, float centerY, float centerZ)
        {
            this(world, DEFAULT_DAMAGE, DEFAULT_TICK_INTERVAL, centerX, centerY, centerZ);
        }

        private Forcefield(World world, float damage, int tickInterval, float centerX, float centerY, float centerZ)
        {
            if (hasForcefield(world))
            {
                sendErrorMessage("Error! " + world.getName() + " already had a forcefield!");
            }
            else
            {
                setEnabled(true);
                forcefields.put(world, this);
            }

            this.world = world;
            this.damage = damage;
            this.tickInterval = tickInterval;
            this.centerX = centerX;
            this.centerY = centerY;
            this.centerZ = centerZ;
        }

        float getCenterX()
        {
            return centerX;
        }

        float getCenterY()
        {
            return centerY;
        }

        float getCenterZ()
        {
            return centerZ;
        }

        public abstract boolean isOutsideBounds(Player p);

        void setEnabled(boolean enable)
        {
            if (enable)
            {
                damageTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
                {
                    public void run()
                    {
                        for (Player p : world.getPlayers())
                        {
                            if (shouldDamagePlayerInWorld(p))
                            {
                                p.damage(damage);
                            }
                        }
                    }
                }, 0, tickInterval);


                if (warningTask == null)
                {
                    numberEnabled = 1;
                    warningTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
                    {
                        public void run()
                        {
                            for (Player p : Bukkit.getOnlinePlayers())
                            {
                                Forcefield forcefield = forcefields.get(p.getWorld());
                                if (forcefield != null && forcefield.shouldDamagePlayerInWorld(p))
                                {
                                    p.sendMessage(getWarningMessage(p));
                                }
                            }
                        }
                    }, 0, WARNING_TICK_INTERVAL);
                }
                else
                {
                    numberEnabled++;
                }
            }
            else
            {
                if (damageTask != null)
                {
                    Bukkit.getScheduler().cancelTask(damageTask);
                    damageTask = null;
                }
                if (numberEnabled <= 0)
                {
                    numberEnabled = 0;
                    if (warningTask != null)
                    {
                        Bukkit.getScheduler().cancelTask(warningTask);
                        warningTask = null;
                    }
                }
            }
        }

        public String getWarningMessage(Player p)
        {
            return ChatColor.RED + "You are outside the forcefield! RUN!!!";
        }

        private boolean shouldDamagePlayerInWorld(Player p)
        {
            for (String exemptTeam : getExemptTeams())
            {
                for (Team playerTeam : p.getScoreboard().getTeams())
                {
                    if (playerTeam.getName().equals(exemptTeam))
                    {
                        return false;
                    }
                }
            }

            return isOutsideBounds(p) && !p.isDead();
        }

        private boolean isEnabled()
        {
            return damageTask != null;
        }
    }

    public static class SphereForcefield extends Forcefield
    {
        private final float radius;

        SphereForcefield(World world, float centerX, float centerY, float centerZ, float radius)
        {
            super(world, centerX, centerY, centerZ);
            this.radius = radius;
        }

        SphereForcefield(World world, float damage, int interval, float centerX, float centerY, float centerZ, float radius)
        {
            super(world, damage, interval, centerX, centerY, centerZ);
            this.radius = radius;
        }

        @Override
        public boolean isOutsideBounds(Player p)
        {
            float x = (float)p.getLocation().getX() - getCenterX();
            float y = (float)p.getLocation().getY() - getCenterY();
            float z = (float)p.getLocation().getZ() - getCenterZ();
            return Math.sqrt(x*x + y*y + z*z) > radius;
        }
    }

    public static class CylinderForcefield extends Forcefield
    {
        private final float radius;
        private final float height;

        CylinderForcefield(World world, float centerX, float centerY, float centerZ, float radius, float height)
        {
            super(world, centerX, centerY, centerZ);
            this.radius = radius;
            this.height = height;
        }

        CylinderForcefield(World world, float damage, int interval, float centerX, float centerY, float centerZ, float radius, float height)
        {
            super(world, damage, interval, centerX, centerY, centerZ);
            this.radius = radius;
            this.height = height;
        }

        @Override
        public boolean isOutsideBounds(Player p)
        {
            float x = (float)p.getLocation().getX() - getCenterX();
            float y = (float)p.getLocation().getY() - getCenterY();
            float z = (float)p.getLocation().getZ() - getCenterZ();
            return Math.sqrt(x*x + z*z) > radius || Math.abs(y) > height/2;
        }
    }

    public static class CenterSquareForcefield extends BoxForcefield
    {
        public CenterSquareForcefield(World world, float damage, int interval, float maxDistance, float yMax)
        {
            super(world, damage, interval, 0, yMax/2, 0, maxDistance, yMax/2, maxDistance);
        }

        public CenterSquareForcefield(World world, float maxDistance, float yMax)
        {
            super(world,0, yMax/2, 0, maxDistance, yMax/2, maxDistance);
        }
    }

    public static class BoxForcefield extends Forcefield
    {
        private final float xD;
        private final float yD;
        private final float zD;

        public BoxForcefield(World world, float damage, int interval, double xMin, float xMax, float yMin, float yMax, float zMin, float zMax)
        {
            this(world, damage, interval,
                    (float)((xMin + xMax)/2),
                    (float)((yMin + yMax)/2),
                    (float)((zMin + zMax)/2),
                    (float)((xMax - xMin)/2),
                    (float)((yMax - yMin)/2),
                    (float)((zMax - zMin)/2));
        }

        public BoxForcefield(World world, double xMin, float xMax, float yMin, float yMax, float zMin, float zMax)
        {
            this(world,
                (float)((xMin + xMax)/2),
                (float)((yMin + yMax)/2),
                (float)((zMin + zMax)/2),
                (float)((xMax - xMin)/2),
                (float)((yMax - yMin)/2),
                (float)((zMax - zMin)/2));
        }

        public BoxForcefield(World world, float damage, int interval, float centerX, float centerY, float centerZ, float xD, float yD, float zD)
        {
            super(world, damage, interval, centerX, centerY, centerZ);

            this.xD = xD;
            this.yD = yD;
            this.zD = zD;
        }

        public BoxForcefield(World world, float centerX, float centerY, float centerZ, float xD, float yD, float zD)
        {
            super(world, centerX, centerY, centerZ);
            this.xD = xD;
            this.yD = yD;
            this.zD = zD;
        }

        @Override
        public boolean isOutsideBounds(Player p)
        {
            return Math.abs((float)p.getLocation().getX() - getCenterX()) > xD ||
                   Math.abs((float)p.getLocation().getY() - getCenterY()) > yD ||
                   Math.abs((float)p.getLocation().getZ() - getCenterZ()) > zD;
        }
    }

    private static void setAllForcefieldsEnabled(boolean enabled)
    {
        for (Forcefield forcefield : forcefields.values())
        {
            forcefield.setEnabled(enabled);
        }
    }

    private static boolean setForcefieldEnabled(World w, boolean enabled)
    {
        Forcefield forcefield = forcefields.get(w);
        if (forcefield != null)
        {
            forcefield.setEnabled(enabled);
            return true;
        }
        return false;
    }

    private static boolean forcefieldIsEnabled(World w)
    {
        Forcefield forcefield = forcefields.get(w);
        return forcefield != null && forcefield.isEnabled();
    }

    private static Forcefield enableDefaultForcefield(World world)
    {
        return new BoxForcefield(world, (double)-500, 500, 0, 128, -500, 500);
    }

    private static boolean hasForcefield(World w)
    {
        return getForcefield(w) != null;
    }

    private static void removeAllForcefields()
    {
        setAllForcefieldsEnabled(false);
        forcefields.clear();
    }

    public static boolean removeForcefield(World w)
    {
        Forcefield forcefield = forcefields.get(w);
        if (forcefield != null)
        {
            forcefield.setEnabled(false);
            forcefields.remove(w);
            return true;
        }
        return false;
    }

    private static Forcefield getForcefield(World world)
    {
        return world == null ? null : forcefields.get(world);
    }

    public boolean onCommand(final CommandSender sender, Command cmd, String label, String[] args)
    {
        if (matchesCommand(label, ADD_FF_EXEMPT_TEAM_CMD))
        {
            if (!(args.length == 1 && args[0].equalsIgnoreCase("list")))
            {
                if (args.length <= 1)
                {
                    displayHelpMessage(sender, label);
                }
                else if (Bukkit.getScoreboardManager().getMainScoreboard().getTeam(args[1]) == null)
                {
                    sender.sendMessage(ChatColor.RED + "Team not found!");
                }
                else if (args[0].equalsIgnoreCase("add"))
                {
                    if (exemptTeams.contains(args[1]))
                    {
                        sender.sendMessage(ChatColor.GOLD + "Team was already exempt");
                    }
                    else
                    {
                        exemptTeams.add(args[1]);
                        FileManager.putData(getSettingFile(), args[1], "true");
                        sender.sendMessage(ChatColor.GOLD + "Team " + args[1] + " exempted from forcefield damage.");
                    }
                }
                else if (args[0].equalsIgnoreCase("remove"))
                {
                    if (exemptTeams.contains(args[1]))
                    {
                        exemptTeams.remove(args[1]);
                        FileManager.putData(getSettingFile(), args[1], "false");
                        sender.sendMessage(ChatColor.GOLD + "Team " + args[1] + " can take forcefield damage now.");
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.GOLD + "Team was already removed from being exempt");
                    }
                }
            }
            else
            {
                sender.sendMessage(ChatColor.YELLOW + "Scoreboard teams exempt from forcefield damage:");
                int i;
                for (i = 0; i < getExemptTeams().size(); i++)
                {
                    ChatColor c = ChatColor.YELLOW;
                    if (i % 2 == 1)
                    {
                        c = ChatColor.RED;
                    }
                    else if (i % 2 == 0)
                    {
                        c = ChatColor.GOLD;
                    }
                    sender.sendMessage(c + getExemptTeams().get(i));
                }
                if (i == 0)
                {
                    sender.sendMessage(ChatColor.RED + ("Well it looks like there aren't any.").toUpperCase());
                }
            }
            return true;
        }
        else if (matchesCommand(label, ENABLE_FF_CMD))
        {
            try
            {
                int i = 0;
                //            subArgs = new String[args.length - 1];
                //            for (int k = 1; k < args.length; k++)
                //            {
                //                subArgs[k-1] = args[k];
                //            }
                final String where;
                World w;
                if (args.length <= i)
                {
                    if (!(sender instanceof Player))
                    {
                        return false;
                    }
                    w = ((Player)sender).getWorld();
                    where = w.getName();
                }
                else
                {
                    where = args[i++];
                    if (where.equalsIgnoreCase("all"))
                    {
                        for (World world : Bukkit.getWorlds())
                        {
                            setForcefieldEnabled(world, true);
                        }
                        sender.sendMessage(ChatColor.GREEN + "Forcefields in all worlds that have them enabled!");
                        return true;
                    }
                    w = Bukkit.getWorld(where);
                }

                final Location l;
                if (w == null)
                {
                    Player p = Bukkit.getPlayer(where);
                    if (p == null)
                    {
                        if (!(sender instanceof Player))
                        {
                            return false;
                        }
                        i--;
                        p = (Player)sender;
                    }
                    l = p.getLocation();
                    w = l.getWorld();
                }
                else
                {
                    if (!(sender instanceof Player))
                    {
                        return false;
                    }
                    w = ((Player)sender).getWorld();
                    l = w.getSpawnLocation();
                }

                if (args.length - i == 5 && args.length - i == 6)
                {
                    l.setX(Double.valueOf(args[i++]));
                    l.setY(Double.valueOf(args[i++]));
                    l.setZ(Double.valueOf(args[i++]));
                }

                final String worldName = WorldManager.getDisplayName(w);

                Forcefield existingFF = getForcefield(w);
                if (existingFF != null)
                {
                    existingFF.setEnabled(true);
                    sender.sendMessage(ChatColor.YELLOW + "Forcefield in " + worldName + ChatColor.YELLOW + " enabled!");
                    return true;
                }
                else
                {
                    sender.sendMessage(ChatColor.YELLOW + "Forcefield in " + worldName + ChatColor.YELLOW + " was not found!");
                }

                int remainingArgsLength = args.length - i;
                if (remainingArgsLength == 0)
                {
                    if (!(sender instanceof Player))
                    {
                        sender.sendMessage(ChatColor.RED + "You can't enable a default forcefield just because I'm racist against the console!");
                        return true;
                    }

                    final World finalWorld = w;
                    new Verifier.BooleanVerifier((Player)sender, ChatColor.YELLOW + "This world has no forcefield right now. Enable default forcefield?")
                    {
                        @Override
                        public void performYesAction()
                        {
                            enableDefaultForcefield(finalWorld);
                            sender.sendMessage(ChatColor.GREEN + "Enabled default forcefield in " + worldName + ChatColor.GREEN + ".");
                        }

                        @Override
                        public void performNoAction()
                        {
                            sender.sendMessage(ChatColor.GREEN + "Command cancelled!");
                        }
                    };
                    return true;
                }

                Forcefield ff = null;
                if (remainingArgsLength >= 6)
                {
                    float xMin = Float.valueOf(args[i++]);
                    float xMax = Float.valueOf(args[i++]);
                    float yMin = Float.valueOf(args[i++]);
                    float yMax = Float.valueOf(args[i++]);
                    float zMin = Float.valueOf(args[i++]);
                    float zMax = Float.valueOf(args[i++]);

                    ff = new BoxForcefield(l.getWorld(), (double)xMin, xMax, yMin, yMax, zMin, zMax);
                }
                else if (remainingArgsLength >= 2)
                {
                    float cRadius = Float.valueOf(args[i++]);
                    float height = Float.valueOf(args[i++]);
                    float ffdamage = Float.valueOf(args[i++]);
                    int ffinterval = Math.round(Float.valueOf(args[i++]));
                    ff = new CylinderForcefield(l.getWorld(), ffdamage, ffinterval, (float)l.getX(), (float)l.getY(), (float)l.getZ(), cRadius, height);
                }
                else if (remainingArgsLength >= 1)
                {
                    float sRadius = Float.valueOf(args[i++]);
                    float ffdamage = Float.valueOf(args[i++]);
                    int interval = Math.round(Float.valueOf(args[i++]));
                    ff = new SphereForcefield(l.getWorld(), ffdamage, interval, (float)l.getX(), (float)l.getY(), (float)l.getZ(), sRadius);
                }

                if (ff != null)
                {
                    sender.sendMessage(ChatColor.GOLD + "Forcefield enabled in " + worldName + "!");
                }
                else
                {
                    sender.sendMessage(ChatColor.RED + "Unable to instantiate forcefield in " + worldName);
                    sendErrorMessage("Error! Got a null forcefield!");
                }
            }
            catch (NumberFormatException ex)
            {
                return false;
            }
            catch (ArrayIndexOutOfBoundsException ex)
            {
                return false;
            }
            //        if (locationsToSurround.size() > 1)
            //        {
            //            sender.sendMessage(ChatColor.GOLD + "Default forcefield enabled in multiple worlds!");
            //        }
            //        else if (locationsToSurround.size() == 1)
            //        {
            //            sender.sendMessage(ChatColor.GOLD + "Default forcefield enabled in " + locationsToSurround.get(0).getWorld().getName() + "!");
            //        }


            return true;
        }
        else if (matchesCommand(label, DISABLE_FF_CMD) || matchesCommand(label, REMOVE_FF_CMD))
        {
            if (args.length == 0 && !(sender instanceof Player))
            {
                sender.sendMessage(ChatColor.RED + "Error! You must specify which world you want to disable the forcefield for (or all if you want to shut them all down)!");
            }
            else
            {
                World world;
                if (args.length == 0)
                {
                    world = ((Player)sender).getWorld();
                }
                else
                {
                    String onlyArg = args[0];
                    if (onlyArg.equalsIgnoreCase("all"))
                    {
                        setAllForcefieldsEnabled(false);
                        sender.sendMessage(ChatColor.GREEN + "Forcefields in all worlds disabled!");
                        return true;
                    }
                    world = Bukkit.getWorld(onlyArg);
                    if (world == null)
                    {
                        Player player = Bukkit.getPlayer(onlyArg);
                        if (player != null)
                        {
                            world = player.getWorld();
                        }
                        else
                        {
                            sender.sendMessage(ChatColor.RED + "Error! Could not find a world or player by that colorlessName!");
                            return true;
                        }
                    }
                }

                if (matchesCommand(label, DISABLE_FF_CMD))
                {
                    if (setForcefieldEnabled(world, false))
                    {
                        sender.sendMessage(ChatColor.GOLD + "Forcefield disabled in " + WorldManager.getDisplayName(world) + "!");
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.GOLD + "Forcefield was already disabled!");
                    }
                }
                else
                {
                    if (removeForcefield(world))
                    {
                        sender.sendMessage(ChatColor.GREEN + "Forcefield in " + WorldManager.getDisplayName(world) + " removed.");
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.GREEN + "There already wasn't a forcefield in " + WorldManager.getDisplayName(world) + ".");
                    }
                }
            }
            return true;
        }
        return false;
    }

//    public static boolean enableForcefieldCmd(final CommandSender sender, String[] args)
//    {
//        try
//        {
//            int i = 0;
//    //            subArgs = new String[args.length - 1];
//    //            for (int k = 1; k < args.length; k++)
//    //            {
//    //                subArgs[k-1] = args[k];
//    //            }
//            final String where;
//            World w;
//            if (args.length <= i)
//            {
//                if (!(sender instanceof Player))
//                {
//                    return false;
//                }
//                w = ((Player)sender).getWorld();
//                where = w.getName();
//            }
//            else
//            {
//                where = args[i++];
//                if (where.equalsIgnoreCase("all"))
//                {
//                    for (World world : Bukkit.getWorlds())
//                    {
//                        setForcefieldEnabled(world, true);
//                    }
//                    sender.sendMessage(ChatColor.GREEN + "Forcefields in all worlds that have them enabled!");
//                    return true;
//                }
//                w = Bukkit.getWorld(where);
//            }
//
//            final Location l;
//            if (w == null)
//            {
//                Player p = Bukkit.getAttacker(where);
//                if (p == null)
//                {
//                    if (!(sender instanceof Player))
//                    {
//                        return false;
//                    }
//                    i--;
//                    p = (Player)sender;
//                }
//                l = p.getLocation();
//                w = l.getWorld();
//            }
//            else
//            {
//                if (!(sender instanceof Player))
//                {
//                    return false;
//                }
//                w = ((Player)sender).getWorld();
//                l = w.getSpawnLocation();
//            }
//
//            if (args.length - i == 5 && args.length - i == 6)
//            {
//                l.setX(Double.valueOf(args[i++]));
//                l.setY(Double.valueOf(args[i++]));
//                l.setZ(Double.valueOf(args[i++]));
//            }
//
//            final String worldName = WorldManager.getDisplayName(w);
//
//            Forcefield existingFF = getForcefield(w);
//            if (existingFF != null)
//            {
//                if (existingFF.setEnabled(true))
//                {
//                    sender.sendMessage(ChatColor.YELLOW + "Forcefield in " + worldName + ChatColor.YELLOW + " enabled (there was already one there)!");
//                }
//                else
//                {
//                    sender.sendMessage(ChatColor.YELLOW + "Forcefield in " + worldName + ChatColor.YELLOW + " was already enabled!");
//                }
//                return true;
//            }
//
//            int remainingArgsLength = args.length - i;
//            if (remainingArgsLength == 0)
//            {
//                if (!(sender instanceof Player))
//                {
//                    sender.sendMessage(ChatColor.RED + "You can't enable a default forcefield just because I'm racist against the console!");
//                    return true;
//                }
//
//                sender.sendMessage(ChatColor.YELLOW + "This world has no forcefield right now. Enable default forcefield?");
//                final World finalWorld = w;
//                Verifier.bindVerifier((Player)sender, new Verifier.VerifierAction()
//                {
//                    @Override
//                    public void performAction(String decision)
//                    {
//                        if (Verifier.getBooleanValue(decision))
//                        {
//                            setToDefaultForcefield(finalWorld, true);
//                            sender.sendMessage(ChatColor.GREEN + "Enabled default forcefield in " + worldName + ChatColor.GREEN + ".");
//                        }
//                        else
//                        {
//                            sender.sendMessage(ChatColor.GREEN + "Command cancelled!");
//                        }
//                    }
//                });
//                return true;
//            }
//
//            Forcefield ff = null;
//            if (remainingArgsLength >= 6)
//            {
//                float xMin = Float.valueOf(args[i++]);
//                float xMax = Float.valueOf(args[i++]);
//                float yMin = Float.valueOf(args[i++]);
//                float yMax = Float.valueOf(args[i++]);
//                float zMin = Float.valueOf(args[i++]);
//                float zMax = Float.valueOf(args[i++]);
//
//                ff = new BoxForcefield(l.getWorld(), (double)xMin, xMax, yMin, yMax, zMin, zMax);
//            }
//            else if (remainingArgsLength >= 2)
//            {
//                float cRadius = Float.valueOf(args[i++]);
//                float height = Float.valueOf(args[i++]);
//                ff = new CylinderForcefield(l, cRadius, height);
//            }
//            else if (remainingArgsLength >= 1)
//            {
//                float sRadius = Float.valueOf(args[i++]);
//                ff = new SphereForcefield(l, sRadius);
//            }
//
//            if (ff != null)
//            {
//                float ffdamage = Float.valueOf(args[i++]);
//                ff.setDamage(ffdamage);
//                int interval = Math.round(Float.valueOf(args[i++]));
//                ff.setInterval(interval);
//                ff.setEnabled(true);
//                sender.sendMessage(ChatColor.GOLD + "Forcefield enabled in " + worldName + "!");
//            }
//            else
//            {
//                sender.sendMessage(ChatColor.RED + "Unable to instantiate forcefield in " + worldName);
//                sendErrorMessage("Error! Got a null forcefield!");
//            }
//        }
//        catch (NumberFormatException ex)
//        {
//            return false;
//        }
//        catch (ArrayIndexOutOfBoundsException ex)
//        {
//            return false;
//        }
//    //        if (locationsToSurround.size() > 1)
//    //        {
//    //            sender.sendMessage(ChatColor.GOLD + "Default forcefield enabled in multiple worlds!");
//    //        }
//    //        else if (locationsToSurround.size() == 1)
//    //        {
//    //            sender.sendMessage(ChatColor.GOLD + "Default forcefield enabled in " + locationsToSurround.get(0).getWorld().getName() + "!");
//    //        }
//
//
//        return true;
//    }
//
//    enum ForceFieldShape
//    {
//        SPHERE, CYLINDER, BOX;
//    }
}