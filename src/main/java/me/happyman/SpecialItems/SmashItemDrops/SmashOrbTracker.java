package me.happyman.SpecialItems.SmashItemDrops;

import me.happyman.Plugin;
import me.happyman.SpecialItems.SmashKitMgt.SmashKitManager;
import me.happyman.utils.ParticlePlayer;
import me.happyman.utils.SmashAttackManager;
import me.happyman.utils.SmashManager;
import me.happyman.worlds.SmashWorldManager;
import me.happyman.worlds.WorldManager;
import me.happyman.worlds.WorldType;
import org.bukkit.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Callable;

import static me.happyman.Plugin.*;

public class SmashOrbTracker implements CommandExecutor
{
    public static class SmashOrb
    {
        private Item item;
        private Integer task;
        private ArrayList<Item> surroundingOrbs;
        private Vector currentVelocity;
        private Location nextLocation;
        private static final float orbMoveSpeed = 0.15F;
        private static final float inertiaMod = 0.5F; //0-1, closer to 1 meaning it won't change velocity as fast
        private final int STARTING_HITS_BEFORE_BREAK = 8;
        private static final int HIT_MS_COOLDOWN = 200;
        private static final float ORB_RADIUS = .3F;
        private static final int ORB_COUNT = 100;//100;
        private static final Random r = new Random();
        private static final ArrayList<Material> ORB_MATERIAL = new ArrayList<Material>(Arrays.asList(Material.GLOWSTONE, Material.COAL_BLOCK));
        private int hitsBeforeBreak;
        private Long timeOfLastHit;
        private final Vector zero;

        protected SmashOrb(Item item, Location startingLocation)
        {
            task = null;
            this.item = item;
            zero = new Vector().zero();
            hitsBeforeBreak = STARTING_HITS_BEFORE_BREAK;
            timeOfLastHit = null;
            surroundingOrbs = new ArrayList<Item>();
            currentVelocity = zero;
            for (int i = 0; i < ORB_COUNT; i++)
            {
                final float[] offset = getRandomOffset();
                surroundingOrbs.add((Item)startingLocation.getWorld().dropItem(
                        startingLocation.clone().add(offset[0], offset[1], offset[2]), new ItemStack(getRandomOrbMaterial()))
                );
            }

            logOrb(this);
        }

        public static Material getRandomOrbMaterial()
        {
            return ORB_MATERIAL.get(r.nextInt(ORB_MATERIAL.size()));
        }

        public static ArrayList<Material> getOrbMaterials()
        {
            return ORB_MATERIAL;
        }

        protected float[] getRandomOffset()
        {
            float[] coords = new float[3];
            for (int coordNum = 0; coordNum < 3; coordNum++)
            {
                coords[coordNum] = (r.nextFloat() - 0.5F)*2F*ORB_RADIUS;
            }
            float distanceFromCenter = (float) SmashManager.getMagnitude(coords[0], coords[1], coords[2]);
            for (int coordNum = 0; coordNum < 3; coordNum++)
            {
                coords[coordNum] = coords[coordNum] * ORB_RADIUS / distanceFromCenter;
            }
            return coords;
        }


        protected ArrayList<Item> getSurroundingOrbs()
        {
            return surroundingOrbs;
        }

        protected Item getCenter()
        {
            return item;
        }

        protected void remove()
        {
            Item orb = getCenter();
            orb.getLocation().getWorld().playEffect(orb.getLocation(), Effect.EXPLOSION, 0, SmashWorldManager.SEARCH_DISTANCE);
            for (Item e : getSurroundingOrbs())
            {
                e.remove();
            }
            if (!orb.isDead())
            {
                orb.remove();
                unlogSmashOrb(orb.getWorld());
            }
            cancelTask();
        }

        public boolean isOnHitCooldown()
        {
            if (timeOfLastHit != null && Plugin.getMillisecond() - timeOfLastHit < HIT_MS_COOLDOWN)
            {
                return true;
            }
            timeOfLastHit = null;
            return false;
        }

        public void hit(Player p)
        {
            if (!isOnHitCooldown())
            {
                chooseNextLocation();
                hitsBeforeBreak--;
                float pitch = 0.5F + (float)(STARTING_HITS_BEFORE_BREAK - hitsBeforeBreak)/STARTING_HITS_BEFORE_BREAK;
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 0.6F, pitch);
                timeOfLastHit = Plugin.getMillisecond();
                if (hitsBeforeBreak <= 0)
                {
                    smash(p);
                }
            }
        }

        private void smash(final Player p)
        {
            if (!SmashKitManager.hasFinalSmashActive(p))
            {
                p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1F, 1F);
                remove();
                SmashKitManager.getKit(p).getProperties().getFinalSmash().give(p);
            }
        }

        protected void act()
        {
            float buffer = .9F;
            for (Entity entity : getCenter().getNearbyEntities(ORB_RADIUS + buffer, ORB_RADIUS + buffer, ORB_RADIUS + buffer))
            {
                if (entity != null && entity instanceof Projectile && entity.getLocation().distance(getCenter().getLocation()) < ORB_RADIUS + buffer)
                {
                    WorldType.AttackSource source = WorldManager.getAttackSource(entity);
                    if (source != null && source.isPlayer())
                    {
                        entity.remove();
                        hit(source.getPlayer());
                    }
                }
            }

            Location currentLoc = getCenter().getLocation();
            if (nextLocation == null || SmashManager.getMagnitude(currentLoc.getX() - nextLocation.getX(), currentLoc.getZ() - nextLocation.getZ()) < 3 || !clear(getLocation(), SmashManager.getUnitDirection(getLocation(), nextLocation)))
            {
                chooseNextLocation();
            }

            if (currentVelocity.equals(zero))
            {
                currentVelocity = SmashManager.getUnitDirection(getCenter().getLocation(), nextLocation).multiply(orbMoveSpeed);
            }
            else
            {
                currentVelocity.multiply(inertiaMod);
                Vector directionToAdd = SmashManager.getUnitDirection(getCenter().getLocation(), nextLocation);
                directionToAdd.multiply(orbMoveSpeed*(1-inertiaMod));
                currentVelocity.add(directionToAdd);
            }

            //centerOrb.teleport(newLocation);
            //Bukkit.getAttacker("HappyMan").sendMessage("setting velocity of Smash orb");
            getCenter().setVelocity(currentVelocity);
            for (Item orb : getSurroundingOrbs())
            {
                //orb.teleport(newLocation.clone().add(offset[0], offset[1], offset[2]));
                orb.setVelocity(currentVelocity);
            }
        }

        //Return true if found a safe location
        private void chooseNextLocation()
        {
            Location currentLoc = getCenter().getLocation();

            Location whereToGo = SmashWorldManager.getRandomItemSpawnLocation(currentLoc.getWorld()).clone().add(0, 7, 0);
            Vector directionOfNextLocation = SmashManager.getUnitDirection(currentLoc, whereToGo);
            int iterator = 0;
            while (!clear(currentLoc, directionOfNextLocation) && iterator < 100)
            {
                whereToGo = SmashWorldManager.getRandomItemSpawnLocation(getCenter().getWorld()).clone().add(0, 7, 0);
                directionOfNextLocation = SmashManager.getUnitDirection(currentLoc, whereToGo);
                iterator++;
            }
            if (iterator == 100)
            {
                nextLocation = currentLoc;
            }
            else
            {
                nextLocation = whereToGo;
            }
        }

        private boolean clear(Location base, Vector directionOfNextLocation)
        {
            base.setDirection(directionOfNextLocation);
            int distance = 60;
            for (int i = 0; i < distance; i++)
            {
                float radians = (float)((float)i%4 / 4 * Math.PI * 2);
                Location whereToCheck1 = ParticlePlayer.getAbsFromRelLocFRU(base,
                        new Vector(i, Math.cos(radians), Math.sin(radians)), false);
                Location whereToCheck2 = ParticlePlayer.getAbsFromRelLocFRU(base,
                        new Vector(i, 0, 0), false);
                if (!whereToCheck1.getBlock().getType().equals(Material.AIR) || !whereToCheck2.getBlock().getType().equals(Material.AIR))
                {
                    return false;
                }
            }
            return true;
        }

        protected void setTask(int task)
        {
            this.task = task;
        }

        private void cancelTask()
        {
            if (task != null)
            {
                Bukkit.getScheduler().cancelTask(task);
            }
        }

        public Location getLocation()
        {
            return getCenter().getLocation();
        }
    }


    private static final String GIVE_ORB_COMMAND = "giveorb";

    private static HashMap<World, SmashOrb> takenOrbWorlds = new HashMap<World, SmashOrb>();
    private static final int ORB_DURATION = 90*20;
    public static final float SMASH_ORB_SPAWN_CHANCE = 0.03F; //This is the absolute chance that an orb will spawn instead when an item supposedly is dropped

    public SmashOrbTracker()
    {
        setExecutor(GIVE_ORB_COMMAND, this);
    }

    public static void createOrb(final Location spawnLocation)
    {
        final World w = spawnLocation.getWorld();
        WorldType.sendMessageToWorld(spawnLocation.getWorld(), ChatColor.DARK_PURPLE + "" + ChatColor.ITALIC + "" + ChatColor.BOLD + "A Final Smash Orb has spawned!");
        for (int i = 0; i < 3; i++)
        {
            for (Player p : w.getPlayers())
            {
                if (!WorldType.isInSpectatorMode(p))
                {
                    WorldManager.playSoundToPlayers(w.getPlayers(), spawnLocation, Sound.ENTITY_ENDERMEN_TELEPORT, .6F, 1F);
                }
            }
        }

        Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable() {
            public String call()
            {
                final SmashOrb tracker = new SmashOrb((Item)w.dropItem(spawnLocation, new ItemStack(SmashOrb.getRandomOrbMaterial())), spawnLocation);

                int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
                    int iteration = 0;
                    @Override
                    public void run() {
                        Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable() {
                            public String call() {
                                if (iteration <= ORB_DURATION)
                                {
                                    if (iteration == ORB_DURATION)
                                    {
                                        tracker.remove();
                                    }
                                    else
                                    {
                                        tracker.act();
                                    }
                                    iteration++;
                                }
                                return "";
                            }
                        });
                    }
                }, 0, 1);
                tracker.setTask(task);
                cancelTaskAfterDelay(task, ORB_DURATION);
                return "";
            }
        });
    }

    /**
     *
     * @return - True if the entity was a non-removed orb
     */
    public static boolean removePossibleOrb(World w)
    {
        if (hasSmashOrb(w))
        {
            takenOrbWorlds.get(w).remove();
            return true;
        }
        return false;
    }

    public static SmashOrb getSmashOrb(World w)
    {
        if (hasSmashOrb(w))
        {
            return takenOrbWorlds.get(w);
        }
        return null;
    }

    public static boolean canCreateOrb(World w)
    {
        if (hasSmashOrb(w))
        {
            return false;
        }
        for (Player p : w.getPlayers())
        {
            if (SmashKitManager.getKit(p).getProperties().getFinalSmash().hasInstanceTaskActive(p))
            {
                return false;
            }
        }
        return true;
    }

    public static boolean hasSmashOrb(World w)
    {
        return takenOrbWorlds.containsKey(w);
    }

    public static void hitSmashOrb(Player p)
    {
        World w = p.getWorld();
        if (hasSmashOrb(w))
        {
            if (!SmashKitManager.hasFinalSmashActive(p))
            {
                getSmashOrb(w).hit(p);
            }
        }
        else
        {
            sendErrorMessage("Error! Tried to hit a Smash Orb in a world that didn't have one!");
        }
    }

    public static boolean isLookingAtSmashOrb(Player p)
    {
        World w = p.getWorld();
        if (hasSmashOrb(w) && !WorldType.isInSpectatorMode(p))
        {
            Entity e = SmashAttackManager.getEntityBeingFaced(p, 4.2F, 5);
            return e != null && e instanceof Item && SmashOrb.getOrbMaterials().contains(((Item)e).getItemStack().getType());
        }
        return false;
    }

    protected static void logOrb(SmashOrb smashOrb)
    {
        takenOrbWorlds.put(smashOrb.getCenter().getWorld(), smashOrb);
    }

    protected static void unlogSmashOrb(World w)
    {
        if (hasSmashOrb(w))
        {
            takenOrbWorlds.remove(w);
        }
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (matchesCommand(label, GIVE_ORB_COMMAND))
        {
            if (!(sender instanceof Player) && args.length == 0)
            {
                sender.sendMessage(ChatColor.RED + "You must specify to whom you would like to give the orb.");
            }
            else
            {
                Player p;
                if (args.length > 0)
                {
                    p = Bukkit.getPlayer(args[0]);
                    if (p == null)
                    {
                        sender.sendMessage(ChatColor.RED +  "Player not found!");
                        return true;
                    }
                }
                else
                {
                    p = (Player)sender;
                }
                if (!SmashWorldManager.isSmashWorld(p.getWorld()))
                {
                    sender.sendMessage(ChatColor.RED + "That player isn't in a Smash world!");
                }
                else
                {
                    SmashKitManager.getKit(p).getProperties().getFinalSmash().give(p);
                }
            }
            return true;
        }
        return false;
    }
}
