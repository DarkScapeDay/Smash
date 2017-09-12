package me.happyman.SpecialItems.SmashKitMgt;

import me.happyman.SpecialItems.SmashGeneralKitItems.RocketItem;
import me.happyman.SpecialItems.SmashItemDrops.Hammer;
import me.happyman.SpecialItems.SmashItemDrops.ItemDropManager;
import me.happyman.SpecialItems.SmashItemDrops.SmashOrbTracker;
import me.happyman.SpecialItems.SpecialItem;
import me.happyman.SpecialItems.SpecialItemTypes.*;
import me.happyman.SpecialItems.UsefulItemStack;
import me.happyman.utils.*;
import me.happyman.worlds.SmashWorldManager;
import me.happyman.worlds.UsefulScoreboard;
import me.happyman.worlds.WorldType;
import org.bukkit.*;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;

import static me.happyman.Plugin.*;
import static me.happyman.utils.FileManager.getGeneralPlayerFile;
import static me.happyman.utils.SmashAttackManager.*;
import static me.happyman.worlds.SmashWorldManager.*;
import static me.happyman.worlds.WorldManager.setAttackSource;
import static me.happyman.worlds.WorldType.getWorldType;

public class SmashKitManager implements CommandExecutor
{
    public enum SmashKit implements Listener //you can't change enum name, but you can change colored name
    {
        Blizzard10(
                new SmashKitProperties(ChatColor.AQUA + "" +  ChatColor.BOLD + "Blizzard", Material.ICE, ChatColor.BLUE + "" + ChatColor.ITALIC + "Freeze thine enemy.",
                new FinalSmash(30, new Sound[] {Sound.ENTITY_WITHER_SPAWN}, new float[] {1}, new float[] {1})
                {
                    private static final int RANGE = SEARCH_DISTANCE *3;
                    private final int TIME_TO_SET_TIME_TICKS = 4*20;
                    private static final long DARK_TIME = 15000;
                    private static final float ATTACK_POWER = 2;
                    private final Random r = new Random();

                    @Override
                    protected void performFinalSmashAbility(final Player p)
                    {
                        final World w = p.getWorld();
                        WorldType.allowWeatherChanges(w, true);
                        WorldType.sendMessageToWorld(p, ChatColor.AQUA + "" + ChatColor.BOLD, ChatColor.BLUE + "" + ChatColor.BOLD + "WEATHER WARNING! INCOMING STORM!");
                        final Vector windSource = new Vector(r.nextFloat() - 0.5f, 0, r.nextFloat() - 0.5f).normalize().multiply(3);
                        String originDescription;
                        float windAngle = (float)Math.atan(windSource.getZ()/windSource.getX());
                        if (windSource.getX() < 0)
                        {
                            windAngle += Math.PI;
                        }

                        while (windAngle < 0)
                        {
                            windAngle += Math.PI * 2;
                        }

                        if (windAngle < Math.PI/8 || windAngle > Math.PI*15/8)
                        {
                            originDescription = "East";
                        }
                        else if (windAngle < Math.PI*3/8)
                        {
                            originDescription = "South-East";
                        }
                        else if (windAngle < Math.PI*5/8)
                        {
                            originDescription = "South";
                        }
                        else if (windAngle < Math.PI*7/8)
                        {
                            originDescription = "South-West";
                        }
                        else if (windAngle < Math.PI*9/8)
                        {
                            originDescription = "West";
                        }
                        else if (windAngle < Math.PI*11/8)
                        {
                            originDescription = "North-West";
                        }
                        else if (windAngle < Math.PI*13/8)
                        {
                            originDescription = "North";
                        }
                        else
                        {
                            originDescription = "North-East";
                        }

                        WorldType.sendMessageToWorld(w, ChatColor.BLUE + "" + ChatColor.ITALIC + "The wind will be coming from the " + originDescription);
                        setBiome(w, Biome.TAIGA_COLD);

                        addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
                        {
                            int i = 0;
                            public void run()
                            {
                                if (i < TIME_TO_SET_TIME_TICKS)
                                {
                                    float percent = (float)(i + 1)/TIME_TO_SET_TIME_TICKS;
                                    long interpolationTime = (long)((1 - percent) * DEFAULT_WORLD_TIME + percent * DARK_TIME);
                                    w.setTime(interpolationTime);
                                }
                                else
                                {
                                    if (i == TIME_TO_SET_TIME_TICKS)
                                    {
                                        w.setStorm(true);
                                    }
                                    for (Player player : w.getPlayers())
                                    {
                                        if (getCurrentKit(player) != Blizzard10)
                                        {
                                            attackPlayer(p, "Final Blizzard", player.getLocation().add(windSource), player, r.nextFloat()*ATTACK_POWER, false);
                                        }
                                    }

                                }
                                i++;
                            }
                        }, 0, 1));
                    }

                    @Override
                    protected void endFinalSmashAbility(final Player p)
                    {
                        final World w = p.getWorld();
                        int resetTask = Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
                        {
                            int i = 0;
                            public void run()
                            {
                                if (i < TIME_TO_SET_TIME_TICKS)
                                {
                                    float percent = (float)i/TIME_TO_SET_TIME_TICKS;
                                    long interpolationTime = (long)((1 - percent) * DARK_TIME + percent * DEFAULT_WORLD_TIME);
                                    w.setTime(interpolationTime);
                                }
                                else if (i == TIME_TO_SET_TIME_TICKS)
                                {
                                    w.setTime(DEFAULT_WORLD_TIME);
                                }
                                i++;
                            }
                        }, 0, 1);
                        cancelTaskAfterDelay(resetTask, TIME_TO_SET_TIME_TICKS);
                        w.setStorm(false);
                        setBiome(w, Biome.PLAINS);
                    }
                }),
                new LeatherArmorSet(ChatColor.AQUA + "Raiment of the Blizzard", 122, 237, 250),
                new SpecialSword.NormalSpecialSword(Material.DIAMOND_SWORD),
                new RocketItem(ChatColor.AQUA + "Double-jump", 2, 7.1f))
                {
                    @Override
                    protected void addAdditionalItems()
                    {
                        addItem(new SpecialItemWithCharge(
                                new UsefulItemStack(Material.SNOW_BLOCK, ChatColor.WHITE + "" + ChatColor.BOLD + "Blizzard Breath", new Enchantment[] {Enchantment.DAMAGE_ALL}, new int[] {1}),
                                1f/(5.5f*20), 0.333f, SpecialItemWithCharge.ChargingMode.CHARGE_AUTOMATICALLY)
                        {
                            @Override
                            public void performResetAction(Player p)
                            {
                                super.performResetAction(p);
                                if (frozenPlayers.containsKey(p.getWorld()))
                                {
                                    for (FrozenPlayer frozenPlayer : frozenPlayers.get(p.getWorld()))
                                    {
                                        unfreezePlayer(frozenPlayer.getPlayer());
                                    }
                                    frozenPlayers.remove(p.getWorld());
                                }
                            }

                            @Override
                            public void performRightClickAction(final Player p, Block blockClicked)
                            {
                                super.performRightClickAction(p, blockClicked);
                                p.updateInventory();
                                Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
                                {
                                    public String call()
                                    {
                                        p.updateInventory();
                                        return "";
                                    }
                                });

                                blastIce(p, 1f, TICKS_TO_PERFORM, .488f, RANGE, MAX_PARTICLES_PER_TICK);
                            }
                        });
                    }

                    class FrozenPlayer
                    {
                        private final Player p;
                        private final InventoryKeep.KeptInventory inv;
                        private final int unfreezeTask;

                        public FrozenPlayer(Player p, InventoryKeep.KeptInventory inv, int unfreezeTask)
                        {
                            this.p = p;
                            this.inv = inv;
                            this.unfreezeTask = unfreezeTask;
                        }
                        public Player getPlayer()
                        {
                            return p;
                        }
                        public InventoryKeep.KeptInventory getInv()
                        {
                            return inv;
                        }
                        public int getUnfreezeTask()
                        {
                            return unfreezeTask;
                        }
                    }

                    private final HashMap<World, List<FrozenPlayer>> frozenPlayers = new HashMap<World, List<FrozenPlayer>>();

                    private final int EXPLOSION_TICKS_DURATION = 10;
                    private final float EXPLOSION_RANGE = 3;
                    private final int EXPLOSION_PARTICLES_PER_TICK = 50;
                    private final Random r = new Random();
                    private final String ICE_BREATH_WEAPON_NAME = "Ice Breath";
                    private final int TICKS_TO_PERFORM = 30;
                    private final float RANGE = 10;
                    private final int MAX_PARTICLES_PER_TICK = 130;
                    private final float MAX_ICE_POWER = 10;
                    private final float FREEZE_SPEED_FACTOR = 0;
                    private final int FREEZE_TIMEOUT_TICKS = 20;
                    private final float FREEZE_NEIGHBOR_RANGE = 1.2f;

                    private boolean isFrozen(Player p)
                    {
                        if (frozenPlayers.containsKey(p.getWorld()))
                        {
                            List<FrozenPlayer> frozenPlayersInWorld = frozenPlayers.get(p.getWorld());
                            if (frozenPlayersInWorld.size() == 0)
                            {
                                frozenPlayers.remove(p.getWorld());
                            }
                            else
                            {
                                for (FrozenPlayer frozen : frozenPlayersInWorld)
                                {
                                    if (frozen.getPlayer().equals(p))
                                    {
                                        return true;
                                    }
                                }
                            }
                        }
                        return false;
                    }

                    private boolean unfreezePlayer(Player p)
                    {
                        if (isFrozen(p))
                        {
                            List<FrozenPlayer> frozenList = frozenPlayers.get(p.getWorld());
                            FrozenPlayer result = null;
                            for (int i = 0; i < frozenList.size(); i++)
                            {
                                FrozenPlayer elt = frozenList.get(i);
                                if (elt.getPlayer().equals(p))
                                {
                                    result = elt;

                                    SmashEntityTracker.resetSpeedFactor(p);
                                    SmashManager.resetJumpBoost(p);
                                    elt.getInv().setExpired();
                                    Bukkit.getScheduler().cancelTask(elt.getUnfreezeTask());
                                    frozenList.remove(i);

                                    break;
                                }
                            }

                            if (result == null)
                            {
                                sendErrorMessage("Error! Could not get frozen player " + p.getName());
                            }
                            return true;
                        }
                        return false;
                    }

                    private void freezePlayer(final Player p, final int tickDuration)
                    {
                        if (!isFrozen(p))
                        {
                            ItemStack[] origContents = p.getInventory().getArmorContents().clone();
                            origContents[3] = new ItemStack(Material.ICE);
                            InventoryKeep.KeptInventory inv = InventoryKeep.setTempInv(p, null, origContents, FREEZE_TIMEOUT_TICKS, false);

                            if (!frozenPlayers.containsKey(p.getWorld()))
                            {
                                frozenPlayers.put(p.getWorld(), new ArrayList<FrozenPlayer>());
                            }
                            int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
                            {
                                int i = 0;
                                public void run()
                                {
                                    if (i < tickDuration)
                                    {
                                        for (Player neighbor : p.getWorld().getPlayers())
                                        {
                                            if (!neighbor.equals(p) && neighbor.getLocation().distance(p.getLocation()) < FREEZE_NEIGHBOR_RANGE && !isFrozen(neighbor))
                                            {
                                                freezePlayer(neighbor, FREEZE_TIMEOUT_TICKS);
                                            }
                                        }
                                        i++;
                                    }
                                    else if (i == tickDuration)
                                    {
                                        explodeFrozenPlayer(p, 2);
                                        i++;
                                    }
                                }
                            }, 0,1);
                            frozenPlayers.get(p.getWorld()).add(new FrozenPlayer(p, inv, task));
                            SmashEntityTracker.setSpeedFactor(p, FREEZE_SPEED_FACTOR);
                            SmashManager.preventJumping(p);
                        }
                    }

                    @EventHandler
                    public void attackingFrozenPlayer(EntityDamageEvent e)
                    {
                        if (e.getEntity() instanceof Player)
                        {
                            Player p = (Player)e.getEntity();
                            if (isFrozen(p))
                            {
                                explodeFrozenPlayer(p, 10);
                            }
                        }
                    }

                    private void explodeFrozenPlayer(final Player p, float damageToAdd)
                    {
                        if (unfreezePlayer(p))
                        {
                            addDamage(p, damageToAdd, false);
                            p.getWorld().playSound(p.getLocation(), Sound.BLOCK_GLASS_BREAK, 1, .3f);
                            Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
                            {
                                int it;
                                public void run()
                                {
                                    if (it < EXPLOSION_TICKS_DURATION)
                                    {
                                        float percentComplete = (float)it / EXPLOSION_TICKS_DURATION;
                                        float currentRad = percentComplete * EXPLOSION_RANGE;
                                        Location baseLocation = p.getEyeLocation();
                                        for (int i = 0; i < EXPLOSION_PARTICLES_PER_TICK; i++)
                                        {
                                            float randomPitchRad = (r.nextFloat() - 0.5f) * (float)Math.PI;
                                            float randomYawRad = r.nextFloat() * (float)Math.PI * 2;
                                            float innerRad = r.nextFloat() * currentRad;
                                            float xLoc = innerRad * (float)Math.cos(randomPitchRad) * (float)Math.cos(randomYawRad);
                                            float yLoc = innerRad * (float)Math.sin(randomPitchRad);
                                            float zLoc = innerRad * (float)Math.cos(randomPitchRad) * (float)Math.sin(randomYawRad);

                                            ParticlePlayer.playBasicParticle(new Location(baseLocation.getWorld(),
                                                    baseLocation.getX() + xLoc,
                                                    baseLocation.getY() + yLoc,
                                                    baseLocation.getZ() + zLoc), ParticlePlayer.getParticleColor(122, 237, 250), false);
                                        }
                                        it++;
                                    }
                                }
                            }, 0, 1);
                        }
                    }

                    private void blastIce(final Player iceMaster, final float powerMod, final int tickDuration, final float angleInRadians, final float range, final float maxParticlesInOneTick)
                    {
                        final Location originalLocation = iceMaster.getEyeLocation();

                        if (!frozenPlayers.containsKey(iceMaster.getWorld()))
                        {
                            frozenPlayers.put(iceMaster.getWorld(), new ArrayList<FrozenPlayer>());
                        }

                        iceMaster.getWorld().playSound(iceMaster.getLocation(), Sound.ENTITY_WITHER_SHOOT, .2f, .4f);
                        Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
                        {
                            int iteration = 3;
                            public void run()
                            {
                                if (iteration < tickDuration)
                                {
                                    Vector boxMin = null;
                                    Vector boxMax = null;
                                    float percentComplete = (float)iteration/tickDuration;
                                    float currentRange = percentComplete * range;
                                    float attenuationFactor = (float)Math.pow(1-percentComplete, 2);

                                    for (int i = 0; i < maxParticlesInOneTick*attenuationFactor; i++)
                                    {
                                        float angleAtWhichParticleAppear = r.nextFloat()*(float)Math.PI*2;
                                        float rightUpDistance = currentRange*(float)Math.sin((r.nextFloat()-0.5f)*angleInRadians);

                                        float rightOffset = rightUpDistance*(float)Math.cos(angleAtWhichParticleAppear);
                                        float upOffset = rightUpDistance*(float)Math.sin(angleAtWhichParticleAppear);
                                        float forwardOffset = (float)Math.sqrt(currentRange*currentRange - rightOffset*rightOffset - upOffset*upOffset) - 0.5f + r.nextFloat();

                                        Location particleLocation = ParticlePlayer.getAbsFromRelLocFRU(originalLocation,
                                                forwardOffset,
                                                rightOffset,
                                                upOffset,
                                                false);
                                        //Bukkit.broadcastMessage("" + iceShooter.getLocation().getPitch

                                        if (boxMin == null || boxMax == null)
                                        {
                                            boxMin = particleLocation.toVector();
                                            boxMax = particleLocation.toVector();
                                        }

                                        if (particleLocation.getX() < boxMin.getX())
                                        {
                                            boxMin.setX(particleLocation.getX());
                                        }
                                        else if (particleLocation.getX() > boxMax.getX())
                                        {
                                            boxMax.setX(particleLocation.getX());
                                        }

                                        if (particleLocation.getY() < boxMin.getY())
                                        {
                                            boxMin.setY(particleLocation.getY());
                                        }
                                        else if (particleLocation.getY() > boxMax.getY())
                                        {
                                            boxMax.setY(particleLocation.getY());
                                        }

                                        if (particleLocation.getZ() < boxMin.getZ())
                                        {
                                            boxMin.setZ(particleLocation.getZ());
                                        }
                                        else if (particleLocation.getZ() > boxMax.getZ())
                                        {
                                            boxMax.setZ(particleLocation.getZ());
                                        }

                                        ParticlePlayer.playBasicParticle(particleLocation, ParticlePlayer.getParticleColor(255, 255, 255), false);
                                    }


                                    if (boxMin != null && boxMax != null)
                                    {
                                        for (Player p : iceMaster.getWorld().getPlayers())
                                        {
                                            Vector whereThePlayerFreakingIs = p.getEyeLocation().toVector();
                                            if (!p.equals(iceMaster) &&
                                                    whereThePlayerFreakingIs.getX() > boxMin.getX() && whereThePlayerFreakingIs.getX() < boxMax.getX() &&
                                                    whereThePlayerFreakingIs.getY() > boxMin.getY() && whereThePlayerFreakingIs.getY() < boxMax.getY() &&
                                                    whereThePlayerFreakingIs.getZ() > boxMin.getZ() && whereThePlayerFreakingIs.getZ() < boxMax.getZ())
                                            {
                                                attackPlayer(iceMaster, ICE_BREATH_WEAPON_NAME, originalLocation, p, powerMod*MAX_ICE_POWER*attenuationFactor, true);
                                                freezePlayer(p, FREEZE_TIMEOUT_TICKS);
                                            }
                                        }
                                    }
                                    //SmashAttackManager.attackPlayersInRange(
                                    iteration++;
                                }
                            }
                        }, 0, 1);
                    }

            /*    public BlizzardTen(new SmashKitProperties(, LeatherArmorSet leatherArmor, SpecialItem sword, SpecialItemWithUsages rocket) {
                    super(properties, leatherArmor, sword, rocket);
                }*/
                },
        Bowser(
                new SmashKitProperties(ChatColor.GREEN + "" + ChatColor.BOLD + "Browser", Material.RED_ROSE, ChatColor.DARK_GREEN + "" + ChatColor.ITALIC + "Roooar!",
                new FinalSmash(10, new Sound[] {Sound.ENTITY_BLAZE_DEATH}, new float[] {7}, new float[] {0.1F})
                {
                    @Override
                    protected void performFinalSmashAbility(final Player p)
                    {
                        final World w = p.getWorld();
                        WorldType.sendMessageToWorld(p, ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "ROOOOAAARRR!!!");
                        setFinalAttackMod(p, 3F);
                        setFinalIntakeMod(p, 0F);

                        int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
                            int iteration = 0;
                            @Override
                            public void run()
                            {
                                if (iteration < getAbilityDurationSeconds()*20)
                                {
                                    float modulus = (float)(iteration%20) / 20;
                                    float xOffset = (float)Math.cos((float)Math.PI*2*modulus*2);
                                    float yOffset = modulus;
                                    float zOffset = (float)Math.sin((float)Math.PI*2*modulus*2);
                                    Location loc1 = new Location(w, p.getLocation().getX() + xOffset, p.getLocation().getY() + yOffset, p.getLocation().getZ() + zOffset);
                                    Location loc2 = new Location(w, p.getLocation().getX() - xOffset, p.getLocation().getY() + yOffset, p.getLocation().getZ() - zOffset);
                                    Location loc3 = new Location(w, p.getLocation().getX() + xOffset, p.getLocation().getY() + yOffset + 1, p.getLocation().getZ() + zOffset);
                                    Location loc4 = new Location(w, p.getLocation().getX() - xOffset, p.getLocation().getY() + yOffset + 1, p.getLocation().getZ() - zOffset);
                                    ParticlePlayer.playBasicParticle(loc1, ParticlePlayer.getParticleColor(255, 0, 0), false);
                                    ParticlePlayer.playBasicParticle(loc2, ParticlePlayer.getParticleColor(0, 255, 0), false);
                                    ParticlePlayer.playBasicParticle(loc3, ParticlePlayer.getParticleColor(255, 0, 0), false);
                                    ParticlePlayer.playBasicParticle(loc4, ParticlePlayer.getParticleColor(0, 255, 0), false);
                                    iteration++;
                                }
                            }
                        }, 0, 1);
                        cancelTaskAfterDelay(task, getAbilityDurationSeconds()*20);
                    }

                    @Override
                    protected void endFinalSmashAbility(Player p)
                    {
                        forgetFinalAttackMod(p);
                        forgetFinalIntakeMod(p);
                    }
                },0.8F, 1.1F, true),
                new LeatherArmorSet(ChatColor.YELLOW + "" + ChatColor.BOLD + "Browser's Shell", new int[] {241, 72, 31}, new int[] {89, 149, 49}, new int[] {37, 160, 53}, new int[] {37, 160, 53}),
                new SpecialItemWithCharge(
                        new UsefulItemStack(Material.DIAMOND_SWORD, ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Sword"),
                        0.01538F, 1F, SpecialItemWithCharge.ChargingMode.CHARGE_AUTOMATICALLY)
                {
                    @Override
                    public void performRightClickAction(Player p, Block blockClicked)
                    {
                        super.performRightClickAction(p, blockClicked);
                        Vector v = p.getEyeLocation().getDirection();
                        Fireball fireball = (Fireball)p.getWorld().spawnEntity(SmashManager.getSafeProjLaunchLocation(p), EntityType.FIREBALL);

                        float vMod = 3.45F;
                        v.setX(v.getX() * vMod);
                        v.setY(v.getY() * vMod);
                        v.setZ(v.getZ() * vMod);
                        //Bukkit.getAttacker("HappyMan").sendMessage("setting v for browser's breath");
                        fireball.setVelocity(v);
                        p.getWorld().playSound(p.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1, 0.2F);
                        setAttackSource(fireball, new WorldType.AttackSource(new WorldType.AttackSource.AttackCulprit(p, this, "Fire Breath"), true));
                    }

                    @Override
                    public float getRangeDamage(EntityDamageByEntityEvent event)
                    {
                        return 25;
                    }
                },
                new RocketItem(ChatColor.RED + "   " + ChatColor.BOLD + "Double-Jump", 2, 6.5f),
                new GroundPound(Material.CLAY_BRICK, 13,  7, Sound.ENTITY_ZOMBIE_ATTACK_DOOR_WOOD, 1F))
                {
                    @Override
                    protected void addAdditionalItems() {}
                },
        Fox(
                new SmashKitProperties(ChatColor.GOLD + "" + ChatColor.BOLD + "Fox", Material.INK_SACK, (short) 14, ChatColor.GOLD + "" + ChatColor.ITALIC + "Here I come!",
                new FinalSmash(18, null, null, null)
                {
                    private static final int TANK_SIZE = 3;
                    private static final float SQUASH_POWER = 40;
                    private final float downOffset = 1F;
                    private final float hoverHeight = 2.4F + downOffset;
                    private final float distanceBetweenParticles = 0.5F;
                    private Vector whereToShootRelFUR = new Vector(4.5F, -downOffset, 0F);
                    private HashMap<List<Vector>, ParticleData> whereToPutParticles = new HashMap<List<Vector>, ParticleData>();
                    private HashMap<Player, List<Item>> shots = new HashMap<Player, List<Item>>();
                    private final int shotLifeLingerSeconds = 2;
                    private final float playerHitRange = 3;
                    private final int firePower = 70;
                    private Random r = new Random();

                    @Override
                    public SpecialItem[] getSecretItems()
                    {
                        return new SpecialItem[]
                        {
                            firer
                        };
                    }

                    private final SpecialItemWithCharge firer = new SpecialItemWithCharge(new UsefulItemStack(Material.FIREWORK_CHARGE, "Fire!"), .1F, 1F, ChargingMode.CHARGE_AUTOMATICALLY)
                    {
                        @Override
                        public void performRightClickAction(final Player p, Block blockClicked)
                        {
                            super.performRightClickAction(p, blockClicked);
                            Location whereToSpawnShot = ParticlePlayer.getAbsFromRelLocFRU(getTurretLoc(p), whereToShootRelFUR, true);

                            for (int i = 0; i < 2; i++)
                            {
                                whereToSpawnShot.getWorld().playSound(whereToSpawnShot, Sound.ENTITY_FIREWORK_BLAST, 1, 0.2F);
                            }

                            whereToSpawnShot.getWorld().playEffect(whereToSpawnShot, Effect.FLAME,0, SEARCH_DISTANCE);
                            final Item itemToTrack = p.getWorld().dropItem(whereToSpawnShot, new ItemStack(Material.STONE));
                            if (!shots.containsKey(p))
                            {
                                shots.put(p, new ArrayList<Item>());
                            }
                            shots.get(p).add(itemToTrack);
                            addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
                            {
                                Vector currentV = getTurretLoc(p).getDirection().multiply(1.5F);

                                public void run()
                                {
                                    if (!itemToTrack.isDead())
                                    {
                                        if (attackPlayersInRange(itemToTrack, p, "Landmaster", firePower, playerHitRange, true, false, false)
                                                || itemToTrack.isOnGround())
                                        {
                                            explodeShell(itemToTrack);
                                        }
                                        else
                                        {
                                            itemToTrack.setVelocity(currentV);
                                            currentV.setY(currentV.getY() - 0.02);
                                        }
                                    }
                                }
                            }, 0, 1));
                        }
                    };

                    class ParticleData
                    {
                        private ParticleEffect.OrdinaryColor color;
                        private boolean useFlatDir;

                        public ParticleData(int red, int green, int blue, boolean useFlatDir)
                        {
                            color = ParticlePlayer.getParticleColor(red, green, blue);
                            this.useFlatDir = useFlatDir;
                        }

                        public boolean isUseFlatDir()
                        {
                            return useFlatDir;
                        }

                        public ParticleEffect.OrdinaryColor getColor()
                        {
                            return color;
                        }
                    }

                    @Override
                    protected void performFinalSmashAbility(final Player p)
                    {
                        setArtificiallyShieldedPlayer(p);
                        if (whereToPutParticles.size() == 0)
                        {
                            //Barrel
                            //                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                            //                                new Vector(7.5F, 0F, -1.75F),
                            //                                new Vector(10.5F, 0F, -1.75F),
                            //                                new Vector(7.5F, 0.375F, -1.75F), distanceBetweenParticles, true),
                            //                                new ParticleData(235, 235, 245, false));
                            //                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                            //                                new Vector(7.5F, 0F, -0.75F),
                            //                                new Vector(10.5F, 0F, -0.75F),
                            //                                new Vector(7.5F, 0.375F, -0.75F), distanceBetweenParticles, true),
                            //                                new ParticleData(235, 235, 245, false));
                            //                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                            //                                new Vector(7.5F, 0.375F, -0.75F),
                            //                                new Vector(10.5F, 0.33F, -0.75F),
                            //                                new Vector(7.5F, 0.375F, -1.75F), distanceBetweenParticles, true),
                            //                                new ParticleData(235, 235, 245, false));
                            //                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                            //                                new Vector(10.5F, 0.33F, -0.75F),
                            //                                new Vector(10.5F, 0, -0.75F),
                            //                                new Vector(10.5F, 0.33F, -1.75F), distanceBetweenParticles, true),
                            //                                new ParticleData(235, 235, 245, false));
                            //                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                            //                                new Vector(2.5F, 0.25F, -1F),
                            //                                new Vector(7.5F, 0.25F, -1F),
                            //                                new Vector(2.5F, 0.25F, -1.5F), distanceBetweenParticles, true),
                            //                                new ParticleData(95, 94, 99, false));
                            //                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                            //                                new Vector(2.5F, 0F, -1F),
                            //                                new Vector(7.5F, 0, -1F),
                            //                                new Vector(2.5F, 0.25F, -1F), distanceBetweenParticles, true),
                            //                                new ParticleData(95, 94, 99, false));
                            //                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                            //                                new Vector(2.5F, 0F, -1.5F),
                            //                                new Vector(7.5F, 0, -1.5F),
                            //                                new Vector(2.5F, 0.25F, -1.5F), distanceBetweenParticles, true),
                            //                                new ParticleData(95, 94, 99, false));
                            //                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                            //                                new Vector(1F, 0.5F, -0.75F),
                            //                                new Vector(2.5F, 0.4F, -0.75F),
                            //                                new Vector(1F, 0.5F, -1.75F), distanceBetweenParticles, true),
                            //                                new ParticleData(241, 241, 249, false));
                            //                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                            //                                new Vector(1F, 0.5F, -1.75F),
                            //                                new Vector(2.5F, 0.4F, -1.75F),
                            //                                new Vector(1F, 0, -1.75F), distanceBetweenParticles, true),
                            //                                new ParticleData(83, 84, 88, false));
                            //                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                            //                                new Vector(1F, 0.5F, -0.75F),
                            //                                new Vector(2.5F, 0.4F, -0.75F),
                            //                                new Vector(1, 0, -0.75F), distanceBetweenParticles, true),
                            //                                new ParticleData(193, 191, 202, false));
                            //
                            //                            //Beside barrel mount side
                            //                            whereToPutParticles.put(SmashManager.getParticleTriangleFRUVectors(
                            //                                new Vector(1F, 0.5F, -0.4F),
                            //                                new Vector(2F, 0.4F, -3.4F),
                            //                                new Vector(1.7F, 1.0F, -2.5F), distanceBetweenParticles, true),
                            //                                new ParticleData(254, 255, 250, true));
                            //                            //Above barrel mount front
                            //                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                            //                                new Vector(1F, 0, -0.4F),
                            //                                new Vector(1.1167F, 0, -0.75F),
                            //                                new Vector(1F, 0.5F, -0.4F), distanceBetweenParticles, true),
                            //                                new ParticleData(254, 255, 250, true));
                            //                            //Below barrel mount front
                            //                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                            //                                new Vector(1.72F, 0, -2.56F),
                            //                                new Vector(2F, 0, -3.4F),
                            //                                new Vector(1.72F, 0.5F, -2.56F), distanceBetweenParticles, true),
                            //                                new ParticleData(228, 228, 238, true));
                            //                            //Below barrel side mount supports tops
                            //                            whereToPutParticles.put(SmashManager.getParticleTriangleFRUVectors(
                            //                                    new Vector(2F, 0.4F, -3.4F),
                            //                                    new Vector(1.7F, 1.0F, -2.5F),
                            //                                    new Vector(1.85F, 1.25F, -2.95F), distanceBetweenParticles, true),
                            //                                    new ParticleData(228, 228, 238, true));
                            //                            //Below barrel side mount supports mains
                            //                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                            //                                    new Vector(1.85F, 1.25F, -2.95F),
                            //                                    new Vector(2F, 0.4F, -3.4F),
                            //                                    new Vector(2.18333, 1.5F, -3.95F), distanceBetweenParticles, true),
                            //                                    new ParticleData(220, 219, 233, true));
                            //                            //Below barrel middle gray area
                            //                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                            //                                    new Vector(2F, 0.4F, -3.4F),
                            //                                    new Vector(2.3333F, 0.65F, -4.4F),
                            //                                    new Vector(2F, -0.25, -3.4F), distanceBetweenParticles, true),
                            //                                    new ParticleData(108, 109, 114, true));
                            //                            //The long parts below those white support things pointing down
                            //                            whereToPutParticles.put(SmashManager.getParticleTriangleFRUVectors(
                            //                                    new Vector(2.3333F, 0.65F, -4.4F),
                            //                                    new Vector(2.18333, 1.5F, -3.95F),
                            //                                    new Vector(2.5, 1.5F, -5.4F), distanceBetweenParticles, true),
                            //                                    new ParticleData(220, 0, 233, true));
                        /**/
                            //barrel_tip
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(5.65F, 0.6F, 0.15F),
                                    new Vector(5.65F, 0.6F, 0F),
                                    new Vector(5.65F, 0.3F, 0.15F), distanceBetweenParticles,true),
                                    new ParticleData(112, 116, 128, false));
                            //barrel_top
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(5.65F, 0.6F, 0F),
                                    new Vector(5.65F, 0.6F, 0.15F),
                                    new Vector(0.9F, 0.6F, 0F), distanceBetweenParticles,true),
                                    new ParticleData(94, 95, 99, false));
                            //barrel_side
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(5.65F, 0.3F, 0.15F),
                                    new Vector(5.65F, 0.6F, 0.15F),
                                    new Vector(0.9F, 0.3F, 0.15F), distanceBetweenParticles,true),
                                    new ParticleData(82, 82, 84, false));
                            //barrel_bottom
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(5.65F, 0.3F, 0F),
                                    new Vector(5.65F, 0.3F, 0.15F),
                                    new Vector(0.9F, 0.3F, 0F), distanceBetweenParticles,true),
                                    new ParticleData(56, 56, 58, false));
                            //turret_front_t
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(0.9F, .8F, 0F),
                                    new Vector(0.958F, .7F, 0F),
                                    new Vector(.9F, .8F, .25F), distanceBetweenParticles,true),
                                    new ParticleData(231, 232, 252, false));
                            //turret_front
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(.9F, .8F, .25F),
                                    new Vector(1.362F, 0F, .25F),
                                    new Vector(.9F, .8F, 1F), distanceBetweenParticles,true),
                                    new ParticleData(255, 255, 255, false));
                            //turret_front_h
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(1.246F, .2F, 0F),
                                    new Vector(1.362F, 0F, 0F),
                                    new Vector(1.246F, .2F, .25F), distanceBetweenParticles,true),
                                    new ParticleData(224, 224, 236, false));
                            //turret_top
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(.511F, .8F, 0F),
                                    new Vector(.9F, .8F, 0F),
                                    new Vector(.511F, .8F, 1F), distanceBetweenParticles,true),
                                    new ParticleData(251, 255, 255, false));
                            //turret_side
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(.9F, .8F, 1F),
                                    new Vector(.511F, .8F, 1F),
                                    new Vector(.9F, 0F, 1F), distanceBetweenParticles,true),
                                    new ParticleData(230, 230, 242, false));
                            //turret_side_front
                            whereToPutParticles.put(ParticlePlayer.getParticleTriangleFRUVectors(
                                    new Vector(.9F, 0F, 1F),
                                    new Vector(.9F, .8F, 1F),
                                    new Vector(1.362F, 0F, 1F), distanceBetweenParticles,true),
                                    new ParticleData(230, 228,239, false));
                            //turret_side_below
                            whereToPutParticles.put(ParticlePlayer.getParticleTriangleFRUVectors(
                                    new Vector(1.362F, 0F, 1F),
                                    new Vector(.9F, 0F, 1F),
                                    new Vector(1.362F, -.142F, 1F), distanceBetweenParticles,true),
                                    new ParticleData(206, 206, 216, false));
                            //turret_front_d
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(1.362F, -.142F, 0F),
                                    new Vector(1.362F, 0F, 0F),
                                    new Vector(1.362F, -.142F, 1F), distanceBetweenParticles,true),
                                    new ParticleData(117, 123, 135, false));
                            //top
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(.9F, 0F, 1F),
                                    new Vector(.9F, 0F, 2F),
                                    new Vector(-.9F, 0F, 1F), distanceBetweenParticles,true),
                                    new ParticleData(213, 215, 227, true));
                            //top_h
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(-.5F, 0F, 0F),
                                    new Vector(-.5F, 0F, 1F),
                                    new Vector(-.9F, 0F, 0F), distanceBetweenParticles,true),
                                    new ParticleData(213, 215, 227, true));
                            //top_back
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(-.9F, 0F, 2F),
                                    new Vector(-.9F, 0F, 0F),
                                    new Vector(-3.5F, -.8F, 2F), distanceBetweenParticles,true),
                                    new ParticleData(238, 243, 255, true));
                            //front_top
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(.9F, 0F, 1F),
                                    new Vector(3.5F, -.8F, 1F),
                                    new Vector(.9F, 0F, 2F), distanceBetweenParticles,true),
                                    new ParticleData(84, 142, 242, true));
                            //front_top_l
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(3.5F, -.8F, 0F),
                                    new Vector(3.5F, -.8F, 1F),
                                    new Vector(1.362F, -.142F, 0F), distanceBetweenParticles,true),
                                    new ParticleData(89, 91, 112, true));
                            //front
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(3.5F, -1.8F, 0F),
                                    new Vector(3.5F, -.8F, 0F),
                                    new Vector(3.5F, -1.8F, 2F), distanceBetweenParticles,true),
                                    new ParticleData(226, 226, 238, true));
                            //side_front_t
                            whereToPutParticles.put(ParticlePlayer.getParticleTriangleFRUVectors(
                                    new Vector(.9F, -.8F, 2F),
                                    new Vector(.9F, 0F, 2F),
                                    new Vector(3.5F, -.8F, 2F), distanceBetweenParticles,true),
                                    new ParticleData(251, 197, 26, true));
                            //side_front_d
                            whereToPutParticles.put(ParticlePlayer.getParticleTriangleFRUVectors(
                                    new Vector(.9F, -1.8F, 2F),
                                    new Vector(3.5F, -1.8F, 2F),
                                    new Vector(.9F, -2.6F, 2F), distanceBetweenParticles,true),
                                    new ParticleData(54, 52, 57, true));
                            //side_t
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(.9F, 0F, 2F),
                                    new Vector(.9F, -.8F, 2F),
                                    new Vector(-.9F, 0F, 2F), distanceBetweenParticles,true),
                                    new ParticleData(182, 28, 38, true));
                            //side_d
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(.9F, -1.8F, 2F),
                                    new Vector(-.9F, -1.8F, 2F),
                                    new Vector(.9F, -2.6F, 2F), distanceBetweenParticles,true),
                                    new ParticleData(104, 105, 110, true));
                            //side_back_t
                            whereToPutParticles.put(ParticlePlayer.getParticleTriangleFRUVectors(
                                    new Vector(-.9F, -.8F, 2F),
                                    new Vector(-.9F, 0, 2F),
                                    new Vector(-3.5F, -.8F, 2F), distanceBetweenParticles,true),
                                    new ParticleData(63, 62, 70, true));
                            //side_back_d
                            whereToPutParticles.put(ParticlePlayer.getParticleTriangleFRUVectors(
                                    new Vector(-.9F, -1.8F, 2F),
                                    new Vector(-3.5F, -1.8F, 2F),
                                    new Vector(-.9F, -2.6F, 2F), distanceBetweenParticles,true),
                                    new ParticleData(63, 62, 70, true));
                            //side
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(3.5F, -.8F, 2F),
                                    new Vector(3.5F, -1.8F, 2F),
                                    new Vector(-3.5F, -.8F, 2F), distanceBetweenParticles,true),
                                    new ParticleData(144, 145, 150, true));
                            //back
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(-3.5F, -.8F, 0F),
                                    new Vector(-3.5F, -.8F, 2F),
                                    new Vector(-3.5F, -1.8F, 0F), distanceBetweenParticles,true),
                                    new ParticleData(183, 183, 195, true));
                            //back_d
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(-.9F, -2.6F, 0F),
                                    new Vector(-3.5F, -1.8F, 0F),
                                    new Vector(-.9F, -2.6F, 2F), distanceBetweenParticles,true),
                                    new ParticleData(154, 153, 161, true));
                            //front_bottom
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(.9F, -2.6F, 0F),
                                    new Vector(3.5F, -1.8F, 0F),
                                    new Vector(.9F, -2.6F, 2F), distanceBetweenParticles,true),
                                    new ParticleData(139, 138, 144, true));
                            //tire_front
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(2.571F, -2.193F, 2F),
                                    new Vector(2.571F, -2.607F, 2F),
                                    new Vector(2.571F, -2.193F, 2.75F), distanceBetweenParticles,true),
                                    new ParticleData(38, 20, 18, true));
                            //tire_front_t
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(2.279F, -1.9F, 2F),
                                    new Vector(2.571F, -2.193F, 2F),
                                    new Vector(2.279F, -1.9F, 2.75F), distanceBetweenParticles,true),
                                    new ParticleData(249, 250, 254, true));
                            //tire_front_d
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(2.279F, -2.9F, 2F),
                                    new Vector(2.571F, -2.607F, 2F),
                                    new Vector(2.279F, -2.9F, 2.75F), distanceBetweenParticles,true),
                                    new ParticleData(36, 34, 35, true));
                            //tire_right_front
                            whereToPutParticles.put(ParticlePlayer.getParticleTriangleFRUVectors(
                                    new Vector(2.571F, -2.193F, 2.75F),
                                    new Vector(2.279F, -1.9F, 2.75F),
                                    new Vector(2.279F, -2.193F, 2.75F), distanceBetweenParticles,true),
                                    new ParticleData(224, 224, 236, true));
                            //tire_right_top
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(2.279F, -1.9F, 2.75F),
                                    new Vector(2.279F, -2.193F, 2.75F),
                                    new Vector(-3.136F, -1.9F, 2.75F), distanceBetweenParticles,true),
                                    new ParticleData(228, 227, 233, true));
                            //tire_right_back
                            whereToPutParticles.put(ParticlePlayer.getParticleTriangleFRUVectors(
                                    new Vector(-3.429F, -2.193F, 2.75F),
                                    new Vector(-3.136F, -2.193F, 2.75F),
                                    new Vector(-3.136F, -1.9F, 2.75F), distanceBetweenParticles,true),
                                    new ParticleData(195, 195, 203, true));
                            //tire_right_mid
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(-3.429F, -2.193F, 2.75F),
                                    new Vector(-3.429F, -2.607F, 2.75F),
                                    new Vector(2.571F, -2.193F, 2.75F), distanceBetweenParticles,true),
                                    new ParticleData(65, 56, 49, true));
                            //tire_right_back_d
                            whereToPutParticles.put(ParticlePlayer.getParticleTriangleFRUVectors(
                                    new Vector(-3.429F, -2.607F, 2.75F),
                                    new Vector(-3.136F, -2.607F, 2.75F),
                                    new Vector(-3.136F, -2.9F, 2.75F), distanceBetweenParticles,true),
                                    new ParticleData(53, 49, 46, true));
                            //tire_right_bottom
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(-3.136F, -2.9F, 2.75F),
                                    new Vector(-3.136F, -2.607F, 2.75F),
                                    new Vector(2.279F, -2.9F, 2.75F), distanceBetweenParticles,true),
                                    new ParticleData(62, 51, 45, true));
                            //tire_right_front_d
                            whereToPutParticles.put(ParticlePlayer.getParticleTriangleFRUVectors(
                                    new Vector(2.279F, -2.607F, 2.75F),
                                    new Vector(2.571F, -2.607F, 2.75F),
                                    new Vector(2.279F, -2.9F, 2.75F), distanceBetweenParticles,true),
                                    new ParticleData(50, 41, 44, true));
                            //tire_back_t
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(-3.136F, -1.9F, 2F),
                                    new Vector(-3.136F, -1.9F, 2.75F),
                                    new Vector(-3.429F, -2.193F, 2F), distanceBetweenParticles,true),
                                    new ParticleData(249, 250, 254, true));
                            //tire_back
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(-3.429F, -2.193F, 2.75F),
                                    new Vector(-3.429F, -2.193F, 2F),
                                    new Vector(-3.429F, -2.607F, 2.75F), distanceBetweenParticles,true),
                                    new ParticleData(38, 20, 18, true));
                            //tire_back_d
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(-3.429, -2.607F, 2F),
                                    new Vector(-3.429, -2.607F, 2.75F),
                                    new Vector(-3.136F, -2.9F, 2F), distanceBetweenParticles,true),
                                    new ParticleData(36, 34, 35, true));
                            //tire_bottom
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(2.279F, -2.9F, 2F),
                                    new Vector(2.279F, -2.9F, 2.75F),
                                    new Vector(-3.136F, -2.9F, 2F), distanceBetweenParticles,true),
                                    new ParticleData(32, 31, 29, true));
                            //tire_top
                            whereToPutParticles.put(ParticlePlayer.getParticleParallelogramFRUVectors(
                                    new Vector(2.279F, -1.9F, 2F),
                                    new Vector(2.279F, -1.9F, 2.75F),
                                    new Vector(-3.136F, -1.9F, 2F), distanceBetweenParticles,true),
                                    new ParticleData(245, 245, 253, true));
                        }

                        WorldType.allowFullflight(p, true);
                        p.setFlySpeed(p.getFlySpeed()*10F);

                        WorldType.sendMessageToWorld(p, ChatColor.GOLD + "" + ChatColor.BOLD + "Landmaster!");

                        addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
                            public static final int HIT_COOLDOWN = 10;
                            private HashMap<Player, Integer> tickHitTimes = new HashMap<Player, Integer>();
                            int iteration = 0;
                            public void run()
                            {
                                //Bukkit.broadcastMessage("" + SmashManager.getFRUFromAbsLoc(new Location(p.getWorld(), 0, 0, 0).setDirection(p.getLocation().getDirection()), new Vector(1, 0, 0), false).toVector().toString());
                                //Bukkit.broadcastMessage("" + SmashManager.getAbsFromRelLocFRU(new Location(p.getWorld(), 0, 0, 0).setDirection(p.getLocation().getDirection()), new Vector(1, 0, 0), false).toVector().toString());
                                Block b = null;

                                for (Entity e : p.getNearbyEntities(TANK_SIZE, hoverHeight, TANK_SIZE))
                                {
                                    if (e instanceof Player && (!tickHitTimes.containsKey(e) || iteration - tickHitTimes.get(e) >= HIT_COOLDOWN) && !e.equals(p) && !WorldType.isInSpectatorMode((Player)e) && p.getLocation().getY() - e.getLocation().getY() > 3)
                                    {
                                        tickHitTimes.put((Player)e, iteration);
                                        Location attackLoc = p.getLocation().clone();
                                        attackLoc.setY(attackLoc.getY() - 2);
                                        attackPlayer(p, "Landmaster", attackLoc, (Player)e, SQUASH_POWER, false);
                                    }
                                }
                                for (int i = -TANK_SIZE; i <= TANK_SIZE; i += TANK_SIZE)
                                {
                                    for (int j = -TANK_SIZE; j <= TANK_SIZE; j += TANK_SIZE)
                                    {
                                        Block lBlock = p.getLocation().clone().getBlock().getRelative(i, 0, j);
                                        Block tentativeBlock = SmashEntityTracker.getBlockBelowLocation(lBlock.getLocation());
                                        if (!tentativeBlock.equals(lBlock) && (b == null || tentativeBlock.getLocation().getY() > b.getLocation().getY()))
                                        {
                                            b = tentativeBlock;
                                        }
                                    }
                                }
                                if (b == null || p.getLocation().getY() - hoverHeight - b.getLocation().getY() > 1.2)
                                {
                                    if (p.getAllowFlight())
                                    {
                                        p.setAllowFlight(false);
                                    }
                                }
                                else
                                {
                                    if (!p.isFlying() || !p.getAllowFlight())
                                    {
                                        p.setAllowFlight(true);
                                        p.setFlying(true);
                                    }
                                    final Block bb = b;
                                    Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable() {
                                        public String call()
                                        {
                                            //Bukkit.getAttacker("HappyMan").sendMessage("setting velocity from fox's final Smash");
                                            p.setVelocity(new Vector(0, (float)(0.1F*(bb.getLocation().getY() + hoverHeight - p.getLocation().getY())), 0));
                                            return "";
                                        }
                                    });
                                }
                                if (iteration % 12 == 0)
                                {
                                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 0.7F,  (r.nextFloat()-0.5F)*0.04F + 0.2F);
                                }

                                for (List<Vector> vectorLists : whereToPutParticles.keySet())
                                {
                                    boolean useFlatDir = whereToPutParticles.get(vectorLists).isUseFlatDir();
                                    ParticleEffect.OrdinaryColor color = whereToPutParticles.get(vectorLists).getColor();
                                    for (Vector v : vectorLists)
                                    {
                                        Location baseLoc = getTurretLoc(p, useFlatDir);
                                        ParticlePlayer.playBasicParticle(
                                                ParticlePlayer.getAbsFromRelLocFRU(baseLoc, (float)v.getX(), (float)v.getY(), (float)v.getZ() - downOffset, useFlatDir),
                                                color, false);
                                    }
                                }
                                iteration++;
                            }
                        }, 0, 1));
                    }

                    private Location getTurretLoc(Player p)
                    {
                        return getTurretLoc(p, false);
                    }

                    private Location getTurretLoc(Player p, boolean actuallyDont)
                    {
                        Location result = p.getEyeLocation().clone();
                        if (Math.abs(result.getPitch()) > 20 && !actuallyDont)
                        {
                            result.setPitch(20*result.getPitch()/Math.abs(result.getPitch()));
                        }
                        return result;
                    }

                    public void explodeShell(Item item)
                    {
                        item.remove();
                        item.getWorld().playEffect(item.getLocation(), Effect.EXPLOSION_LARGE, 0, SEARCH_DISTANCE);
                        item.getWorld().playSound(item.getLocation(), Sound.ENTITY_GENERIC_EXPLODE, 1F, 1F);
                        item.remove();
                    }

                    @Override
                    protected void endFinalSmashAbility(final Player p)
                    {
                        forgetArtificiallyShieldedPlayer(p);
                        p.setFlySpeed(p.getFlySpeed()*0.1F);
                        if (!WorldType.isInSpectatorMode(p))
                        {
                            p.setAllowFlight(false);
                        }
                        if (shots.containsKey(p))
                        {
                            for (Item item : shots.get(p))
                            {
                                if (item.isOnGround())
                                {
                                    explodeShell(item);
                                }
                            }
                            Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
                                @Override
                                public void run()
                                {
                                    if (shots.containsKey(p))
                                    {
                                        for (Item item : shots.get(p))
                                        {
                                            explodeShell(item);
                                        }
                                        shots.remove(p);
                                    }
                                }
                            }, shotLifeLingerSeconds*20);
                        }
                    }
                }, 1, 1, false),
                new LeatherArmorSet(ChatColor.GOLD + "" + ChatColor.BOLD + "Fox's Armor", null, new int[]{121, 127, 125}, new int[]{61, 71, 46}, new int[]{122, 57, 63}),
                null,
                new RocketItem(ChatColor.RED+ "" + ChatColor.BOLD + "Double-Jump", 2, 7.2f)
                {
                    @Override
                    public boolean canBeUsed(Player p)
                    {
                        return super.canBeUsed(p) && !hasFinalSmashActive(p);
                    }
                })
                {
                    private final ParticleEffect.OrdinaryColor laserColor = new ParticleEffect.OrdinaryColor(0, 255, 0);
                    @Override
                    public void addAdditionalItems()
                    {
                        addItem(new SpecialItemWithCharge(
                                new UsefulItemStack(Material.FLINT, ChatColor.BLACK + "" + ChatColor.BOLD + "Laser Gun"), 0.0016666F, 0.066666F, SpecialItemWithCharge.ChargingMode.CHARGE_AUTOMATICALLY)
                        {
                            private final int LASER_POWER = 6;
                            private final float LASER_SPEED = 60F;

                            private void playLaserSound(Location l, int numOfTimes)
                            {
                                for (int m = 0; m < numOfTimes; m++)
                                {
                                    l.getWorld().playSound(l, Sound.ENTITY_FIREWORK_LAUNCH, .2F, 0.15F);
                                }
                            }

                            @Override
                            public void performRightClickAction(Player p, Block blockClicked)
                            {
                                super.performRightClickAction(p, blockClicked);
                                final Vector v = p.getLocation().getDirection().multiply(LASER_SPEED);
                                Location launchLoc = SmashManager.getSafeProjLaunchLocation(p);
                                Vector facing = p.getLocation().getDirection().multiply(0.2);
                                double i = launchLoc.getX();
                                double j = launchLoc.getY();
                                double k = launchLoc.getZ();

                                final Arrow laser = (Arrow)p.getWorld().spawnEntity(launchLoc, EntityType.ARROW);
                                playLaserSound(launchLoc, 1);
                                for (int x = 0; x < 500; x++)
                                {
                                    i += facing.getX();
                                    j += facing.getY();
                                    k += facing.getZ();
                                    Location lForT = new Location(launchLoc.getWorld(), i, j, k);
                                    if (!lForT.getBlock().getType().equals(Material.AIR))
                                    {
                                        break;
                                    }
                                    if (x % 7 == 0)
                                    {
                                        playLaserSound(lForT, 1);
                                    }
                                    ParticlePlayer.playBasicParticle(lForT, laserColor, true);
                                }
                                int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
                                {
                                    int i = 0;
                                    @Override
                                    public void run() {
                                        if (i < SmashEntityTracker.ARROW_TRACKING_TIME - 1)
                                        {
                                            laser.setVelocity(v);
                                        }
                                        else
                                        {
                                            laser.remove();
                                        }
                                    }
                                }, 0, 1);
                                cancelTaskAfterDelay(task, SmashEntityTracker.ARROW_TRACKING_TIME);

                                setAttackSource(laser, new WorldType.AttackSource(new WorldType.AttackSource.AttackCulprit(p, this), true));
                            }

                            @Override
                            public float getRangeDamage(EntityDamageByEntityEvent event)
                            {
                                return LASER_POWER;
                            }
                        });
                        addItem(new SpecialItemWithCharge(
                                new UsefulItemStack(Material.FENCE, ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Kick"),
                                0.14F, 1F, SpecialItemWithCharge.ChargingMode.CHARGE_AUTOMATICALLY)
                        {
                            final float KICK_RANGE = 2.5F;
                            final float KICK_ANGLE = 70F;
                            final float KICK_POWER = 15F;

                            @Override
                            public void performRightClickAction(final Player p, Block blockClicked)
                            {
                                super.performRightClickAction(p, blockClicked);
                                p.updateInventory();
                                Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable() {
                                    public String call() {
                                        p.updateInventory();
                                        return "";
                                    }
                                });
                                if (canBeUsed(p))
                                {
                                    p.getWorld().playSound(p.getLocation(), Sound.ENTITY_BAT_TAKEOFF, 1F, 0.2F);
                                    attackPlayersInAngleRange(p, this, KICK_POWER, KICK_RANGE, KICK_ANGLE, KICK_ANGLE);
                                }
                            }
                        });
                    }
                },
        //        Ganondorf(
        //                new SmashKitProperties(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Jokendork", Material.PAPER, ChatColor.GRAY + "" + ChatColor.ITALIC + "Oooh-yeah!",
        //                        new FinalSmash(6, null, null)
        //                        {
        //                            @Override
        //                            protected void addItems()
        //                            {
        //                            }
        //
        //                            @Override
        //                            protected void performFinalSmashAbility(final Player p)
        //                            {
        //                                addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
        //                                    int iteration = 0;
        //                                    public void run()
        //                                    {
        //                                        if (SmashAttackManager.getDamage(p) >= 8900 || iteration >= 100)
        //                                        {
        //                                            SmashAttackManager.addDamage(p, 9001 - SmashAttackManager.getDamage(p), false);
        //                                            performResetAction(p);
        //                                        }
        //                                        else
        //                                        {
        //                                            SmashAttackManager.addDamage(p, 87, false);
        //                                        }
        //                                        iteration++;
        //                                    }
        //                                }, 0, 1));
        //                            }
        //
        //                            @Override
        //                            protected void endFinalSmashAbility(Player p)
        //                            {
        //
        //                            }
        //                        }, 5F, 0.7F),
        //                new LeatherArmorSet(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Jokendork's Armor", new int[]{251, 89, 51}, new int[]{87, 87, 115}, new int[]{69, 62, 70}, new int[]{41, 47, 63}),
        //                new SpecialItem(Material.WOOD_SWORD, ChatColor.LIGHT_PURPLE + "Jokendork's sword", new Enchantment[]{Enchantment.DURABILITY}, new int[]{100}) {
        //                },
        //                new RocketItem(ChatColor.RED + "" + ChatColor.BOLD + "   Single-Jump", 1, 4f))
        //                {
        //                    class GanondorfEgg extends SpecialItem implements Listener
        //                    {
        //                        GanondorfEgg()
        //                        {
        //                            super(Material.EGG, ChatColor.YELLOW + "Egg", ChatColor.GRAY + "Maybe this kit isn't so bad after all...", true);
        //                            Bukkit.getPluginManager().registerEvents(this, getPlugin());
        //                        }
        //
        //                        public void performRightClickAction(final Player p) {}
        //
        //                        @EventHandler
        //                        public void throwEgg(ProjectileLaunchEvent e)
        //                        {
        //                            if (e.getEntity().getShooter() instanceof Player && isThis(((Player)e.getEntity().getShooter()).getItemInHand()))
        //                            {
        //                                final Player p = (Player)e.getEntity().getShooter();
        //                                p.setItemInHand(getItemStack());
        //                                Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable() {
        //                                    public String call() {
        //                                        p.setItemInHand(getItemStack());
        //                                        p.updateInventory();
        //                                        return "";
        //                                    }
        //                                });
        //                                SmashEntityTracker.addCulprit(p, e.getEntity(), getItemStack().getItemMeta().getDisplayName(), 5);
        //                            }
        //                        }
        //                    }
        //
        //                    @Override
        //                    public void addAdditionalItems()
        //                    {
        //                        addItem(new GanondorfEgg());
        //                    }
        //                },
        Kirby(
                new SmashKitProperties(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Murphy", Material.INK_SACK, (short) 9, ChatColor.LIGHT_PURPLE + "" + ChatColor.ITALIC + "Hiii!",
                new FinalSmash(10, new Sound[] {Sound.BLOCK_LAVA_POP}, new float[] {2F})
                {

                    private static final int MAX_DELAY_TO_START_MOVING = 20;
                    private static final int HOW_LONG_IT_TAKES_TO_MOVE_IN = 30;
                    private static final int HOW_LONG_TO_COOK = 30;
                    private static final int range = 70;
                    private static final int damagePercent = 34;
                    private static final int PARTICLE_COUNT = 100;
                    private final ParticleEffect.OrdinaryColor bowlColor = ParticlePlayer.getParticleColor(150, 0, 0);
                    private final ParticleEffect.OrdinaryColor hatColor = ParticlePlayer.getParticleColor(255, 255, 255);
                    private final float hatHeight = 1;
                    private final Vector zero = new Vector().zero();
                    private final Random r = new Random();

                    @Override
                    protected void performFinalSmashAbility(final Player p)
                    {
                        addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
                            Vector flatFacing = ParticlePlayer.getVectorOfYaw(p);
                            Location potLocation = new Location(p.getWorld(),
                                    p.getLocation().getX() + flatFacing.getX(),
                                    p.getLocation().getY() + 0.3,
                                    p.getLocation().getZ() + flatFacing.getZ());
                            HashMap<Player, Integer> delayToStartTicks = new HashMap<Player, Integer>();
                            HashMap<Player, Float> as = new HashMap<Player, Float>();
                            HashMap<Player, Vector> originalHVectors = new HashMap<Player, Vector>();
                            HashMap<Player, Float> originalDistances = new HashMap<Player, Float>();
                            int iteration = 0;
                            final float x = (float) potLocation.getX();
                            final float y = (float) potLocation.getY();
                            final float z = (float) potLocation.getZ();
                            public void run() {

                                if (iteration <= HOW_LONG_IT_TAKES_TO_MOVE_IN + MAX_DELAY_TO_START_MOVING + HOW_LONG_TO_COOK)
                                {
                                    if (iteration == 0)
                                    {
                                        for (Player victim : p.getWorld().getPlayers())
                                        {
                                            if (!victim.equals(p) && !WorldType.isInSpectatorMode(victim) && victim.getLocation().distance(p.getLocation()) < range)
                                            {
                                                delayToStartTicks.put(victim, r.nextInt(MAX_DELAY_TO_START_MOVING));
                                            }
                                        }
                                        SmashEntityTracker.setSpeedFactor(p, 0F);
                                        SmashManager.preventJumping(p);
                                    }

                                    Location hatBaseLocation = p.getLocation().clone().add(0, 1.9, 0);
                                    for (int i = 0; i < PARTICLE_COUNT; i++)
                                    {
                                        float randomRad = (float)Math.PI*2 * r.nextFloat();
                                        if ((float)i / PARTICLE_COUNT < 0.5)
                                        {
                                            ParticlePlayer.playBasicParticle(new Location(p.getWorld(),
                                                    potLocation.getX() + Math.cos(randomRad)/2,
                                                    potLocation.getY() + r.nextFloat(),
                                                    potLocation.getZ() + Math.sin(randomRad)/2), bowlColor, false);
                                        }
                                        else if ((float)i / PARTICLE_COUNT < 0.65)
                                        {
                                            ParticlePlayer.playBasicParticle(new Location(p.getWorld(),
                                                    potLocation.getX() + Math.sin(randomRad)/2*r.nextFloat(),
                                                    potLocation.getY(),
                                                    potLocation.getZ() + Math.cos(randomRad)/2*r.nextFloat()), bowlColor, false);
                                        }
                                        else if ((float)i / PARTICLE_COUNT < 0.9)
                                        {
                                            ParticlePlayer.playBasicParticle(ParticlePlayer.getAbsFromRelLocFRU(hatBaseLocation,
                                                    (float)Math.cos(randomRad)/4.5F,
                                                    (float)Math.sin(randomRad)/4.5F,
                                                    r.nextFloat()*hatHeight*0.7F, false), hatColor, false);
                                        }
                                        else
                                        {
                                            ParticlePlayer.playBasicParticle(ParticlePlayer.getAbsFromRelLocFRU(hatBaseLocation,
                                                    (float)Math.cos(randomRad)/2.5F,
                                                    (float)Math.sin(randomRad)/2.5F,
                                                    hatHeight*0.3F*r.nextFloat() + 0.7F, false), hatColor, false);

                                        }
                                    }
                                    if (iteration % (r.nextInt(3) + 3) == 0)
                                    {
                                        potLocation.getWorld().playEffect(potLocation.clone().add(0, 1, 0), Effect.LAVA_POP, 0, SEARCH_DISTANCE);
                                        playSmashSound(potLocation);
                                    }

                                    //Bukkit.getAttacker("HappyMan").sendMessage("setting " + p.getDisplayName() + "'s Velocity to 0");
                                    p.setVelocity(zero);
                                    if (iteration < HOW_LONG_IT_TAKES_TO_MOVE_IN + MAX_DELAY_TO_START_MOVING + HOW_LONG_TO_COOK)
                                    {
                                        for (Player victim : delayToStartTicks.keySet())
                                        {
                                            int virtualIteration = iteration - delayToStartTicks.get(victim);
                                            if (virtualIteration >= 0 && virtualIteration < HOW_LONG_IT_TAKES_TO_MOVE_IN)
                                            {
                                                float deltaX = x - (float)victim.getLocation().getX();
                                                float deltaZ = z - (float)victim.getLocation().getZ();
                                                float magOfDistance = (float)Math.sqrt(deltaX*deltaX + deltaZ*deltaZ);
                                                if (iteration - delayToStartTicks.get(victim) == 0)
                                                {
                                                    originalHVectors.put(victim, new Vector(-deltaX, 0, -deltaZ));
                                                    originalDistances.put(victim, magOfDistance);
                                                    as.put(victim, -(y - (float)victim.getLocation().getY() + magOfDistance)/(magOfDistance*magOfDistance));
                                                }
                                                float distanceHFromKirbyPercent = 1F - (float)virtualIteration/HOW_LONG_IT_TAKES_TO_MOVE_IN;
                                                float distanceHFromKirby = distanceHFromKirbyPercent*originalDistances.get(victim);
                                                sendEntityTowardLocation(new Location(p.getWorld(),
                                                        x + distanceHFromKirbyPercent*originalHVectors.get(victim).getX(),
                                                        y + as.get(victim)*distanceHFromKirby*distanceHFromKirby + distanceHFromKirby,
                                                        z + distanceHFromKirbyPercent*originalHVectors.get(victim).getZ()), victim, originalDistances.get(victim)/20, false);
                                            }
                                            else
                                            {
                                                if (virtualIteration == HOW_LONG_IT_TAKES_TO_MOVE_IN)
                                                {
                                                    playSmashSound(potLocation);
                                                    playSmashSound(potLocation);
                                                }
                                                sendEntityTowardLocation(potLocation, victim, .2F, false);
                                            }
                                        }
                                    }
                                    else
                                    {
                                        Location potDmgLoc = potLocation.clone();
                                        potDmgLoc.setY(potLocation.getY() - 4);
                                        for (Player victim : delayToStartTicks.keySet())
                                        {
                                            if (victim.getLocation().distance(potLocation) <= 3)
                                            {
                                                attackPlayer(p, "Cook Murphy", potDmgLoc, victim, damagePercent*2, false);
                                            }
                                        }
                                        allowMovement(p);
                                    }
                                }
                                iteration++;
                            }
                        }, 0, 1));
                    }

                    private void allowMovement(Player p)
                    {
                        SmashManager.resetJumpBoost(p);
                        SmashEntityTracker.resetSpeedAlteredPlayer(p);
                    }

                    @Override
                    protected void endFinalSmashAbility(Player p) {
                        allowMovement(p);
                    }
                }, 1F, 1F, false),
                new LeatherArmorSet(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Murphy", new int[]{255, 190, 210}, new int[]{255, 190, 220}, new int[]{244, 174, 213}, new int[]{221, 71, 46}),
                new RocketItem(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Jump", 5, 5.1f))
                {
                    class KirbyInhaler extends SpecialItemWithTask
                    {
                        private static final int MASK_KIT_DURATION = 45;

                        private final int INHALE_RANGE = 12;
                        private final float MAX_INHALE_ACCELERATION = 0.26F;
                        private final float NOSEBLEED_ZONE = 1.6F;
                        private final float INHALE_SLOWDOWN = 0.4F;
                        private static final float MUTATION_NERF = 0.5F;

                        private HashMap<Player, Integer> inhalers;

                        public KirbyInhaler()
                        {
                            super(new UsefulItemStack(Material.GLASS_BOTTLE, ChatColor.GRAY + "" + ChatColor.BOLD + "Inhale"));
                            inhalers = new HashMap<Player, Integer>();
                        }

                        @Override
                        public void setExpToRemaining(Player p)
                        {
                            p.setExp(0);
                        }

                        private void performSwallow(final Player p, final LivingEntity target)
                        {
                            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_PLAYER_BURP, 1F, 1F);
                            final Random r = new Random();
                            if (target instanceof Player)
                            {
                                Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
                                {
                                    public String call()
                                    {
                                        attackWithCustomKB(p, "Swallow", new Location(p.getWorld(), (r.nextFloat() - 0.5), p.getEyeLocation().getY() - 0.4, (r.nextFloat() - 0.5)), (Player)target, 30, 50, true);
                                        return "";
                                    }
                                });
                                if (!(getKit((Player)target).equals(SmashKit.Kirby)))
                                {
                                    setFinalAttackMod(p, MUTATION_NERF);
                                    SmashItemManager.setItemSlotForKitChange(p);

                                    InventoryKeep.setTempInv(p, MASK_KIT_DURATION, false);

                                    SmashKit targetKit = getKit((Player)target);
                                    targetKit.applyKitInventory(p, false);
                                    setMaskKit(p, targetKit);
                                    boolean foundRocket = false;
                                    boolean foundAir = false;
                                    for (int i = 0; i < 36; i++)
                                    {
                                        if (!foundRocket && getRocket().isThis(p.getInventory().getItem(i)))
                                        {
                                            p.getInventory().setItem(i, getRocket().getItemStack());
                                            foundRocket = true;
                                        }
                                        else if (!foundAir && (p.getInventory().getItem(i) == null || p.getInventory().getItem(i).getType().equals(Material.AIR)))
                                        {
                                            p.getInventory().setItem(i, reverser.getItemStack());
                                            foundAir = true;
                                        }
                                        if (foundRocket && foundAir)
                                        {
                                            break;
                                        }
                                    }
                                    p.getEquipment().setChestplate(Kirby.getArmor()[2]);
                                    p.getEquipment().setLeggings(Kirby.getArmor()[1]);
                                    addTask(p, Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
                                        public void run() {
                                            resetToKirby(p, false);
                                        }
                                    }, MASK_KIT_DURATION*20));
                                }
                            }
                            else
                            {
                                //Bukkit.getAttacker("HappyMan").sendMessage("setting velocity from inhaler");
                                target.setVelocity(new Vector((r.nextFloat() - 0.5)*2, 1.8, (r.nextFloat() - 0.5)*2));
                                target.damage(8);
                            }
                        }

                        public void resetToKirby(final Player p, boolean clearItemDrops)
                        {
                            forgetFinalAttackMod(p);
                            removeKitMask(p);
                            //SmashKitManager.getKit(p).applyKitInventory(p, clearItemDrops);
                        }
                        //        public static void resetToKirby(final Player p, boolean clearItemDrops)
                        //        {
                        //            SmashAttackManager.forgetFinalAttackMod(p);
                        //            if (!Hammer.isWieldingHammer(p))
                        //            {
                        //                if (maskKit.containsKey(p))
                        //                {
                        //                    maskKit.remove(p);
                        //                }
                        //                SmashKitManager.getKit(p).applyKitInventory(p, clearItemDrops);
                        //            }
                        //            else
                        //            {
                        //                Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable()
                        //                {
                        //                    public void run()
                        //                    {
                        //                        resetToKirby(p, false);
                        //                    }
                        //                }, 2);
                        //            }
                        //        }

                        public void performResetAction(Player p)
                        {
                            super.performResetAction(p);
                            resetToKirby(p, true);
                        }

                        public void trackPlayerCharge(final Player p)
                        {
                            addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
                            {
                                int i = 0;
                                public void run()
                                {
                                    if (isBeingHeld(p))
                                    {
                                        if (i < 4)
                                        {
                                            final LivingEntity target = SmashManager.getNearestEntityExcept(p, p, true, 30*(float)Math.PI/180, true);
                                            if (target != null)
                                            {
                                                float distance = (float)p.getLocation().distance(target.getLocation());
                                                int maxSpeed = 10;
                                                if (distance > NOSEBLEED_ZONE && (!(target instanceof Player) || SmashEntityTracker.getSpeed((Player)target) < maxSpeed) && distance <= INHALE_RANGE && distance > 0)
                                                {
                                                    sendEntityTowardLocation(p.getLocation(), target, MAX_INHALE_ACCELERATION*distance/INHALE_RANGE, true);
                                                }
                                                if (distance <= NOSEBLEED_ZONE)
                                                {
                                                    performSwallow(p, target);
                                                    i = 4;
                                                }
                                            }
                                            i++;
                                        }
                                    }
                                }
                            }, 0, 1));
                        }

                        public void performRightClickAction(final Player p, Block blockClicked) //This can happen up to every 0.2 seconds or 4 ticks
                        {
                            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_CAT_HISS, 0.5F, 1F);
                            p.setWalkSpeed(INHALE_SLOWDOWN*0.2F);
                            cancelInhaler(p, false);
                            inhalers.put(p, Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
                                public void run() {
                                    cancelInhaler(p, true);
                                }
                            }, 7));
                            trackPlayerCharge(p);
                        }

                        private void cancelInhaler(Player p, boolean resetWalkSpeed)
                        {
                            if (inhalers.containsKey(p))
                            {
                                Bukkit.getScheduler().cancelTask(inhalers.get(p));
                            }
                            if (resetWalkSpeed)
                            {
                                SmashEntityTracker.setSpeedToCurrentSpeed(p);
                            }
                        }

                        @Override
                        public void performDeselectAction(Player p)
                        {
                            cancelInhaler(p, true);
                        }
                    }

                    private KirbyInhaler inhaler;
                    private SpecialItem reverser;

                    @Override
                    protected void addAdditionalItems()
                    {
                        inhaler = new KirbyInhaler();
                        reverser = new SpecialItem(new UsefulItemStack(Material.REDSTONE_BLOCK, ChatColor.LIGHT_PURPLE + "Back to " + getDisplayName()))
                        {
                            @Override
                            public void performRightClickAction(Player p, Block blockClicked)
                            {
                                super.performRightClickAction(p, blockClicked);
                                inhaler.resetToKirby(p, false);
                                p.playSound(p.getLocation(), Sound.ENTITY_ZOMBIE_VILLAGER_CURE, 1F, 1F);
                            }
                        };

                        addItem(new KirbySword(), 0);
                        addItem(inhaler);
                        addItem(new KirbyRockTransformer());
                        addSecretItem(reverser);
                    }

                    class KirbyRockTransformer extends SpecialItemWithCharge implements Listener
                    {
                        private HashMap<Player, Integer> revertTransformationTasks;
                        private final int STONE_DURATION = 7;
                        private final int HITS_BEFORE_REVERT = 7;
                        private final float RADIUS = 3;
                        private final float DAMAGE = 25;
                        private HashMap<Player, Integer> hitsBeforeRevert;

                        public KirbyRockTransformer()
                        {
                            super(new UsefulItemStack(Material.BEDROCK, ChatColor.BLACK + "" + ChatColor.BOLD + "Stone"), 0.006F, 1F, ChargingMode.CHARGE_AUTOMATICALLY);
                            revertTransformationTasks = new HashMap<Player, Integer>();
                            Bukkit.getPluginManager().registerEvents(this, getPlugin());
                            hitsBeforeRevert = new HashMap<Player, Integer>();
                        }

                        public void performResetAction(Player p)
                        {
                            super.performResetAction(p);
                            revertTransformation(p);
                        }

                        @Override
                        public void performDeselectAction(Player p)
                        {
                            super.performDeselectAction(p);
                            revertTransformation(p);
                        }

                        private void revertTransformation(Player p)
                        {
                            if (revertTransformationTasks.containsKey(p))
                            {
                                Bukkit.getScheduler().cancelTask(revertTransformationTasks.get(p));
                                revertTransformationTasks.remove(p);
                            }
                            SmashKit kit = getCurrentKit(p);
                            if (kit != null)
                            {
                                kit.applyKitArmor(p);
                            }
                            forgetArtificiallyShieldedPlayer(p);
                            forgetHitsBeforeRevert(p);
                            SmashEntityTracker.setSpeedToCurrentSpeed(p);
                        }

                        private boolean isInStoneMode(Player p)
                        {
                            return revertTransformationTasks.containsKey(p);
                        }

                        @Override
                        public boolean canBeUsed(Player p)
                        {
                            return !isInStoneMode(p) && super.canBeUsed(p);
                        }

                        @Override
                        public void performRightClickAction(final Player p, Block blockClicked)
                        {
                            super.performRightClickAction(p, blockClicked);

                            setArtificiallyShieldedPlayer(p);
                            Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
                            {
                                public String call()
                                {
                                    //Bukkit.getAttacker("HappyMan").sendMessage("setting velocity for stone mode");
                                    p.setVelocity(new Vector(0, -10, 0));
                                    return "";
                                }
                            });
                            p.getWorld().playSound(SmashEntityTracker.getBlockBelowEntity(p).getLocation(), Sound.BLOCK_ANVIL_BREAK, 1F, 0.9F);
                            revertTransformationTasks.put(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
                                boolean keepDamaging = true;
                                int i = 0;
                                public void run() {
                                    if (i < STONE_DURATION*20)
                                    {
                                        if (!isBeingHeld(p))
                                        {
                                            revertTransformation(p);
                                        }
                                        if (((Entity)p).isOnGround())
                                        {
                                            keepDamaging = false;
                                        }
                                        if (keepDamaging)
                                        {
                                            attackPlayersInRange(p, getItemStack().getItemMeta().getDisplayName(), DAMAGE, RADIUS);
                                        }
                                        i++;
                                    }
                                    else
                                    {
                                        revertTransformation(p);
                                    }
                                }
                            }, 0, 1));
                            p.getEquipment().setArmorContents((new LeatherArmorSet(ChatColor.BLACK + "STONE", 0, 0, 0)).getContents());
                            p.setWalkSpeed(0);
                            SmashManager.preventJumping(p);
                            hitsBeforeRevert.put(p, HITS_BEFORE_REVERT);
                        }

                        private int getHitsBeforeRevert(Player p)
                        {
                            if (hitsBeforeRevert.containsKey(p))
                            {
                                if (hitsBeforeRevert.get(p) == 0)
                                {
                                    forgetHitsBeforeRevert(p);
                                    return 0;
                                }
                                return hitsBeforeRevert.get(p);
                            }
                            return 0;
                        }

                        private void forgetHitsBeforeRevert(Player p)
                        {
                            if (hitsBeforeRevert.containsKey(p))
                            {
                                hitsBeforeRevert.remove(p);
                            }
                        }

                        @EventHandler
                        public void revertStoneOnHit(EntityDamageByEntityEvent e)
                        {
                            if (e.getEntity() instanceof Player)
                            {
                                Player p = (Player)e.getEntity();
                                if (isInStoneMode(p))
                                {
                                    if (getHitsBeforeRevert(p) > 0)
                                    {
                                        hitsBeforeRevert.put(p, hitsBeforeRevert.get(p) - 1);
                                    }
                                    if (getHitsBeforeRevert(p) == 0)
                                    {
                                        revertTransformation(p);
                                    }
                                }
                            }
                        }

                        @EventHandler
                        public void dontDoAnythingElse(InventoryOpenEvent e)
                        {
                            if (isInStoneMode((Player)e.getPlayer()))
                            {
                                e.setCancelled(true);
                            }
                        }

                        @EventHandler
                        public void crouchCancel(PlayerToggleSneakEvent e)
                        {
                            Player p = e.getPlayer();
                            if (isInStoneMode(p) && SmashEntityTracker.isCrouching(p))
                            {
                                revertTransformation(p);
                            }
                        }
                    }

                    class KirbySword extends SpecialItemWithCharge implements Listener
                    {
                        private final HashMap<Player, Integer> finalCutterTasks;
                        private final HashMap<Entity, Integer> fallingTasks;

                        public KirbySword()
                        {
                            super(new UsefulItemStack(Material.IRON_SWORD, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Sword"), 0.1F, 1F, ChargingMode.CHARGE_AUTOMATICALLY);//, new Enchantment[]{Enchantment.DAMAGE_ALL}, new int[]{8});
                            Bukkit.getPluginManager().registerEvents(this, getPlugin());
                            finalCutterTasks = new HashMap<Player, Integer>();
                            fallingTasks = new HashMap<Entity, Integer>();
                        }

                        @Override
                        public void performResetAction(Player p)
                        {
                            super.performResetAction(p);
                            if (canChangeKit(p))
                            {
                                cancelFinalCutter(p);
                                cancelFallingTask(p);
                            }
                        }

                        @Override
                        public void performDeselectAction(Player p)
                        {
                            super.performDeselectAction(p);
                            if (canChangeKit(p))
                            {
                                cancelFinalCutter(p);
                                cancelFallingTask(p);
                            }
                        }

                        private void cancelFinalCutter(Player p)
                        {
                            if (finalCutterTasks.containsKey(p))
                            {
                                Bukkit.getScheduler().cancelTask(finalCutterTasks.get(p));
                                finalCutterTasks.remove(p);
                            }
                        }

                        private void cancelFallingTask(Player p)
                        {
                            if (fallingTasks.containsKey(p))
                            {
                                Bukkit.getScheduler().cancelTask(fallingTasks.get(p));
                                fallingTasks.remove(p);
                            }
                        }

                        @Override
                        public boolean canBeUsed(Player p)
                        {
                            return super.canBeUsed(p) && !fallingTasks.containsKey(p);
                        }

                        @Override
                        public void performRightClickAction(final Player p, Block blockClicked)
                        {
                            super.performRightClickAction(p, blockClicked);
                            //Bukkit.getAttacker("HappyMan").sendMessage("setting velocity for Kirby final cutter");
                            p.setVelocity(new Vector(0, 1.42, 0));
                            fallingTasks.put(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
                            {
                                int i = 0;
                                int delay = 15;
                                public void run()
                                {
                                    if (i > 3 && p.getLocation().getY() % 0.5 < 0.20001 && p.getLocation().getY() % 0.5 > 0.1999)
                                    {
                                        i = delay;
                                    }
                                    if (i < delay)
                                    {
                                        i++;
                                    }
                                    else if (i == delay)
                                    {
                                        //Bukkit.getAttacker("HappyMan").sendMessage("setting velocity for final cutter");
                                        p.setVelocity(new Vector(0, -4, 0));
                                        i++;
                                    }
                                }
                            }, 0, 1));
                        }

                        @EventHandler
                        public void finalCutter(EntityDamageEvent e)
                        {
                            if (e.getCause().equals(EntityDamageEvent.DamageCause.FALL) && fallingTasks.containsKey(e.getEntity()))
                            {
                                final Player p = (Player)e.getEntity();
                                p.getWorld().playSound(p.getLocation(), Sound.BLOCK_LAVA_POP, 1F, 1F);
                                final Item sword = p.getWorld().dropItem(p.getLocation().add(0, 0.2, 0), getItemStack());

                                final Vector v = ParticlePlayer.getVectorOfYaw(p, 1.2F).setY(0.09F);
                                final int life = 17;
                                setCharge(p, 0);
                                cancelFinalCutter(p);
                                Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
                                    public void run() {
                                        sword.remove();
                                    }
                                }, life);
                                finalCutterTasks.put(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
                                    int i = 0;
                                    List<Location> locations = new ArrayList<Location>();
                                    public void run()
                                    {
                                        if (i <= life)
                                        {
                                            for (Entity e : sword.getNearbyEntities(1, 0.2, 1))
                                            {
                                                if (e instanceof Player && !e.equals(p))
                                                {
                                                    locations.add(0, sword.getLocation());
                                                    if (locations.size() > 3)
                                                    {
                                                        locations.remove(3);
                                                    }
                                                    attackPlayer(p, "Final Cutter", locations.get(locations.size() - 1), (Player)e, 12, true);
                                                }
                                            }
                                            //Bukkit.getAttacker("HappyMan").sendMessage("setting velocity of final cutter");
                                            sword.setVelocity(v);
                                            i++;
                                        }
                                    }
                                }, 0, 1));
                                cancelFallingTask(p);
                            }
                        }

                        @Override
                        public boolean performItemPickup(Player p, Location whereTheItemWasFound)
                        {
                            super.performItemPickup(p, whereTheItemWasFound);
                            return true;
                        }

                /*public static Pig spawnLargeItem(Location l, ItemStack item)
                {
                    Giant giant = (Giant)l.getWorld().spawn(l, Giant.class); //.spawnEntity(l.clone().add(0, 7, 0), EntityType.GIANT);
                    //giant.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 100000, 2, false, false));
                    giant.getEquipment().setItemInHand(item);

                    Pig pig = (Pig)l.getWorld().spawnEntity(l, EntityType.PIG);
                    //pig.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 10000, 1, false, false));
                    pig.setPassenger(giant);

                    return pig;

                }*/
                    }

                },
        Mario(
                new SmashKitProperties(ChatColor.RED + "" + ChatColor.BOLD + "Marly", Material.IRON_SWORD, ChatColor.BLUE + "" + ChatColor.ITALIC + "Wahoo!",
                new FinalSmash()
                {
                    private static final int POWER = 20;
                    private static final int waves = 16;//3;
                    private static final int delayTicksBetweenWaves = 6;//15;
                    private static final int numPerWave = 15;
                    private static final float degreesOfWaveApprox = 30F;

                    @Override
                    protected void performFinalSmashAbility(final Player p)
                    {
                        final String kitName = getKit(p).getDisplayName();
                        addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
                        {
                            int iteration = 0;
                            int wavesSpawned = 0;
                            public void run()
                            {
                                if (iteration % delayTicksBetweenWaves == 0 && wavesSpawned < waves)
                                {
                                    wavesSpawned++;
                                    for (float i = -degreesOfWaveApprox; i <= degreesOfWaveApprox; i += degreesOfWaveApprox*2/numPerWave)
                                    {
                                        Vector dir = ParticlePlayer.getAbsOffsetFromRelLocFUR(p.getLocation(), new Vector(1.4*Math.cos(i*Math.PI/180), 1.4*p.getLocation().getDirection().getY(), 1.8*Math.sin(i*Math.PI/180)), true);
                                        FireballLauncher.launchTNT(p, dir, POWER, kitName + " Finale");
                                    }
                                }
                                iteration++;
                            }
                        }, 0, 1));
                    }

                    @Override
                    protected void endFinalSmashAbility(Player p) {}
                }),
                new LeatherArmorSet(ChatColor.GOLD + "" + ChatColor.BOLD + "Marly's Outfit", new int[]{255, 0, 0}, new int[]{0, 0, 255}, new int[]{255, 0, 0}, new int[]{255, 0, 0}),
                new SpecialSword.NormalSpecialSword(),
                new RocketItem(ChatColor.RED + "   " + ChatColor.BOLD + "Double-Jump", 2, 7.6f))
                {
                    @Override
                    protected void addAdditionalItems()
                    {
                        addItem(new FireballLauncher());
                    }
                },
        Pikachu(
                new SmashKitProperties(ChatColor.YELLOW + "" + ChatColor.BOLD + "Pika", Material.BLAZE_ROD, ChatColor.YELLOW + "" + ChatColor.ITALIC + "PIIIIIIIKKKKAAAAAAAAAAAAAAAAAAAAAA!",
                new FinalSmash(10, new Sound[] {Sound.BLOCK_NOTE_BASS}, new float[] {2F}, new float[] {0.55F})
                {
                    private final float POWER = 14F;
                    private final float PARICLE_NUM = 150;
                    private final Random r = new Random();
                    private final ParticleEffect.OrdinaryColor blueColor = ParticlePlayer.getParticleColor(14, 123, 252);
                    private final ParticleEffect.OrdinaryColor yellowColor = ParticlePlayer.getParticleColor(255, 255, 28);
                    private final float ABILITY_RADIUS = 3F;
                    private final float SPEED_BOOST = 3F;
                    private final float PARTICLE_OFFSET_MOD = 0.3F;
                    private final int HIT_COOLDOWN = 10;
                    private ArrayList<Player> speedAltered = new ArrayList<Player>();

                    @Override
                    protected void performFinalSmashAbility(final Player p)
                    {
                        setArtificiallyShieldedPlayer(p);
                        p.setAllowFlight(true);
                        p.setFlying(true);
                        if (SmashEntityTracker.multiplySpeedFactor(p, SPEED_BOOST) && !speedAltered.contains(p))
                        {
                            speedAltered.add(p);
                        }

                        addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
                            private HashMap<Player, Integer> tickOfHit = new HashMap<Player, Integer>();
                            int iteration = 0;
                            public void run()
                            {
                                playSmashSound(p.getEyeLocation(), 1F + (r.nextFloat() - 0.5F)*0.03F);
                                float pitch;
                                float yaw;
                                float randomOffset = (float)Math.pow((1 + r.nextFloat()* PARTICLE_OFFSET_MOD), 3);
                                for (int i = 0; i < PARICLE_NUM; i++)
                                {
                                    pitch = (r.nextFloat() - 0.5F)*(float)Math.PI;
                                    yaw = (r.nextFloat() - 0.5F)*(float)Math.PI*2;

                                    ParticlePlayer.playBasicParticle(ParticlePlayer.getAbsFromRelLocFRU(p.getEyeLocation(),
                                            ABILITY_RADIUS*(float)Math.cos(pitch)*(float)Math.cos(yaw)*randomOffset,
                                            ABILITY_RADIUS*(float)Math.cos(pitch)*(float)Math.sin(yaw)*randomOffset,
                                            ABILITY_RADIUS*(float)Math.sin(pitch)*randomOffset, true), blueColor, true);
                                    ParticlePlayer.playBasicParticle(ParticlePlayer.getAbsFromRelLocFRU(p.getEyeLocation(),
                                            ABILITY_RADIUS/1.3F*(float)Math.cos(pitch)*(float)Math.cos(yaw)*randomOffset,
                                            ABILITY_RADIUS/1.3F*(float)Math.cos(pitch)*(float)Math.sin(yaw)*randomOffset,
                                            ABILITY_RADIUS/1.3F*(float)Math.sin(pitch)*randomOffset, true), yellowColor, true);
                                }

                                attackPlayersInRange(p, "Volt Tackle", POWER, ABILITY_RADIUS, false);
                                iteration++;
                            }
                        }, 0, 1));
                    }

                    @Override
                    protected void endFinalSmashAbility(Player p)
                    {
                        forgetArtificiallyShieldedPlayer(p);
                        p.setAllowFlight(false);
                        if (speedAltered.contains(p))
                        {
                            SmashEntityTracker.multiplySpeedFactor(p, 1F/SPEED_BOOST);
                            speedAltered.remove(p);
                        }
                    }
                }, 1.1F, 1.1F, true),
                new LeatherArmorSet(ChatColor.YELLOW + "" + ChatColor.BOLD + "Pika", 255, 255, 0),
                new SpecialSword(new UsefulItemStack(Material.GOLD_SWORD, ChatColor.YELLOW + "" + ChatColor.BOLD + "Sword", new Enchantment[] {Enchantment.DAMAGE_ALL, Enchantment.DURABILITY}, new int[] {2, 10}))
                {
                    //@TODO: Do something cool here... or not
                    @Override
                    public void performRightClickAction(Player p, Block blockClicked)
                    {
                        super.performRightClickAction(p, blockClicked);
                    }
                },
                new RocketItem(ChatColor.YELLOW + "" + ChatColor.BOLD + "Double-Jump", 2, 7.2f)
                {
                    @Override
                    public boolean canBeUsed(Player p)
                    {
                        return super.canBeUsed(p) && !FinalSmash.hasFinalSmashActive(p);
                    }
                })
                {
                    @Override
                    protected void addAdditionalItems()
                    {
                        addItem(new SpecialItemWithCharge(
                                new UsefulItemStack(Material.BLAZE_ROD, ChatColor.WHITE + "" + ChatColor.ITALIC + "" + ChatColor.BOLD + "Thunderbolt"),
                                0.0068F, 1F, SpecialItemWithCharge.ChargingMode.CHARGE_AUTOMATICALLY)
                        {
                            @Override
                            public void performRightClickAction(Player p, Block blockClicked)
                            {
                                super.performRightClickAction(p, blockClicked);
                                World w = p.getWorld();
                                Location l = p.getEyeLocation();
                                l.setX(l.getX() - 0.5);
                                l.setZ(l.getZ() - 0.5);
                                Block b = w.getHighestBlockAt(p.getLocation()).getRelative(0, -1, 0);
                                if (!b.getType().equals(Material.AIR))
                                {
                                    l.setY(b.getY() + 1);
                                }
                                LightningStrike lightning = p.getWorld().strikeLightning(l);
                                setAttackSource(lightning, new WorldType.AttackSource(new WorldType.AttackSource.AttackCulprit(p, this), true));
                            }

                            @Override
                            public float getRangeDamage(EntityDamageByEntityEvent event)
                            {
                                return 13;
                            }
                        });
                    }
                },
        Pit(
                new SmashKitProperties(ChatColor.AQUA + "" + ChatColor.BOLD + "Lit", Material.GOLD_HOE, ChatColor.WHITE + "" + ChatColor.ITALIC + "The fight is on!",
                new FinalSmash(30, new Sound[] {Sound.ENTITY_WITHER_DEATH}, new float[] {3F}, new float[] {.1F})
                {
                    private final Random r = new Random();
                    private HashMap<Player, List<Monster>> palutenasArmy = new HashMap<Player, List<Monster>>();
                    private final int armySize = 5;
                    Vector zeroVector = new Vector(0, 0.009, 0);
                    ItemStack[] centurionArmor = new ItemStack[] {
                            new ItemStack(Material.GOLD_BOOTS),
                            new ItemStack(Material.GOLD_LEGGINGS),
                            new ItemStack(Material.GOLD_CHESTPLATE),
                            new ItemStack(Material.GOLD_HELMET)};
                    ItemStack centurionWeapon = new ItemStack(Material.GOLD_SWORD);
                    PotionEffect invisEffect = new PotionEffect(PotionEffectType.INVISIBILITY, 1000000, 1);
                    List<ParticleEffect.OrdinaryColor> whiteWingColors = new ArrayList<ParticleEffect.OrdinaryColor>(Arrays.asList(ParticlePlayer.getParticleColor(255, 255, 255)));

                    protected void performFinalSmashAbility(final Player p)
                    {
                        final int intervalTicks = Math.round((float)4*20/armySize);
                        final World w = p.getWorld();
                        addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
                            int numSpawned = 0;
                            @Override
                            public void run() {
                                if (numSpawned < armySize && SmashWorldManager.isSmashWorld(w))
                                {
                                    if (!palutenasArmy.containsKey(p))
                                    {
                                        palutenasArmy.put(p, new ArrayList<Monster>());
                                    }
                                    Location centurionSpawnLocation = SmashWorldManager.getRandomPlayerSpawnLocation(w).clone().add(r.nextInt(8) - 4, 10, r.nextInt(8));
                                    Monster centurion = (Monster)w.spawnEntity(centurionSpawnLocation, EntityType.SKELETON);
                                    centurion.addPotionEffect(invisEffect);
                                    palutenasArmy.get(p).add(centurion);
                                    centurion.getEquipment().setArmorContents(centurionArmor);
                                    centurion.getEquipment().setItemInHand(centurionWeapon);
                                    numSpawned++;
                                }
                            }
                        }, 0, intervalTicks));

                        addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
                            HashMap<Monster, Player> targetPlayers = new HashMap<Monster, Player>();
                            int iteration = 0;

                            @Override
                            public void run() {
                                if (palutenasArmy.containsKey(p))
                                {
                                    for (Monster e : palutenasArmy.get(p))
                                    {
                                        if (!e.isDead())
                                        {
                                            ParticlePlayer.playWing(e, iteration, whiteWingColors);
                                            if (!targetPlayers.containsKey(e))
                                            {
                                                List<Player> avaliablePlayers = w.getPlayers();
                                                for (int i = 0; i < avaliablePlayers.size(); i++)
                                                {
                                                    Player candidate = avaliablePlayers.get(i);
                                                    if (!targetPlayers.containsValue(candidate) && !candidate.equals(p) && !WorldType.isInSpectatorMode(candidate))
                                                    {
                                                        targetPlayers.put(e, candidate);
                                                        w.playSound(e.getLocation(), Sound.ENTITY_GHAST_SHOOT, 1, 0.2F);
                                                    }
                                                }
                                            }
                                            if (targetPlayers.containsKey(e) && iteration >= intervalTicks*armySize)
                                            {
                                                Player target = targetPlayers.get(e);
                                                e.setTarget(target);
                                                if (e.getLocation().distance(target.getLocation()) < 1)
                                                {
                                                    if (!isShielded(target))
                                                    {
                                                        attackPlayer(p, "Palutena's Army", e.getLocation(), target, 40, false);
                                                    }
                                                    for (int i = 0; i < 3; i++)
                                                    {
                                                        w.playSound(e.getLocation(), Sound.ENTITY_FIREWORK_BLAST, 1, 1);
                                                    }
                                                    e.remove();
                                                    targetPlayers.remove(e);
                                                }
                                                else
                                                {
                                                    e.setVelocity(SmashManager.getUnitDirection(e.getLocation(), target.getLocation()).multiply(3));
                                                }
                                            }
                                            else
                                            {
                                                e.setVelocity(zeroVector);
                                            }
                                        }
                                    }
                                }
                                iteration++;
                            }
                        }, 0, 1));

                        setArtificiallyShieldedPlayer(p);
                        final boolean onGround = ((Entity)p).isOnGround() || p.getLocation().getY() % 0.5 < 0.001;
                        final float oldFactor = SmashEntityTracker.getSpeedFactor(p);
                        if (onGround)
                        {
                            SmashEntityTracker.setSpeedFactor(p, 0);
                        }
                        Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable() {
                            public void run()
                            {
                                if (onGround)
                                {
                                    SmashEntityTracker.setSpeedFactor(p, oldFactor);
                                }
                                forgetArtificiallyShieldedPlayer(p);
                            }
                        }, 20);

                        WorldType.sendMessageToWorld(p, ChatColor.ITALIC + "All troops, move out!");


                    }

                    @Override
                    protected void endFinalSmashAbility(Player p)
                    {
                        if (palutenasArmy.containsKey(p))
                        {
                            for (Entity e : palutenasArmy.get(p))
                            {
                                e.remove();
                            }
                            palutenasArmy.remove(p);
                        }
                    }
                }, 1.4F),
                new LeatherArmorSet(ChatColor.YELLOW + "" + ChatColor.BOLD + "Lit's Outfit", new int[]{113, 76, 58}, new int[]{230, 245, 251}, null, new int[]{74, 55, 40}))
                {
                    class PitBow extends SpecialItem implements Listener
                    {
                        private final ParticleEffect.OrdinaryColor color = new ParticleEffect.OrdinaryColor(0, 0, 255);;

                        PitBow()
                        {
                            super(new UsefulItemStack(Material.BOW, ChatColor.YELLOW + "" + ChatColor.BOLD + "Bow of Light", new Enchantment[] {Enchantment.DAMAGE_ALL, Enchantment.ARROW_INFINITE}, new int[] {7, 1}));
                            Bukkit.getPluginManager().registerEvents(this, getPlugin());
                        }

                        @EventHandler
                        public void fancy(final ProjectileLaunchEvent e)
                        {
                            if (e.getEntity() instanceof Arrow && e.getEntity().getShooter() instanceof Player && isBeingHeld((Player)e.getEntity().getShooter()))
                            {
                                int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        ParticlePlayer.playBasicParticle(e.getEntity(), color, false);
                                    }
                                }, 0, 0);
                                cancelTaskAfterDelay(task, SmashEntityTracker.ARROW_TRACKING_TIME);
                            }
                        }
                    }
                    @Override
                    protected void addAdditionalItems()
                    {
                        addItem(new PitBow());
                        addItem(new RocketItem(ChatColor.YELLOW + "" + ChatColor.BOLD + "Triple-jump", 3, 7.15f));

                        final List<ParticleEffect.OrdinaryColor> colors = new ArrayList<ParticleEffect.OrdinaryColor>();

                        for (int i = 0; i <= 200; i += 5)
                        {
                            colors.add(ParticlePlayer.getParticleColor(0, i, 255));
                        }

                        addItem(new SpecialItemWithContCharge(
                                new UsefulItemStack(Material.FEATHER, ChatColor.WHITE + "" + ChatColor.BOLD + "" + ChatColor.ITALIC + "Wings of Icarus"),
                                0.0018F, 1f/(3.2f*20f), SpecialItemWithCharge.ChargingMode.CHARGE_AUTOMATICALLY, false)
                        {
                            public void performResetAction(Player p)
                            {
                                super.performResetAction(p);
                                if (!WorldType.isInSpectatorMode(p))
                                {
                                    p.setAllowFlight(false);
                                }
                            }

                            @Override
                            public void setExpToRemaining(Player p)
                            {
                                super.setExpToRemaining(p);
                            }

                            public void setCharge(Player p, float charge)
                            {
                                super.setCharge(p, charge);
                                if (charge == 1F)
                                {
                                    performResetAction(p);
                                }
                                else if (charge == 0F)
                                {
                                    cancelTasks(p);
                                }
                            }

                            @Override
                            public void performDeactivationAction(Player p)
                            {
                                p.setAllowFlight(false);
                            }

                            @Override
                            public void performActivationAction(final Player p)
                            {
                                p.setAllowFlight(true);
                                p.setFlySpeed(0.12F); //0.059 0.61
//                                    setCharging(p, false);
                                addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
                                {
                                    int iteration = 0;
                                    public void run()
                                    {
                                        if (Hammer.isWieldingHammer(p))
                                        {
                                            cancelTasks(p);
                                        }
                                        else
                                        {
                                            ParticlePlayer.playWing(p, iteration, colors);
                                        }
                                        iteration++;
                                    }
                                }, 0, 1));
                            }
                        });

                        addHiddenItem(new SpecialItem(new UsefulItemStack(Material.ARROW, ChatColor.GRAY + "Arrow")));
                    }
                },
        Sonic(
                new SmashKitProperties(ChatColor.AQUA + "" + ChatColor.BOLD + "Sonic", Material.INK_SACK, (short)4, ChatColor.BLUE + "" + ChatColor.ITALIC + "You're too slow!",
                new FinalSmash(15, null, null)
                {
                    private final float ATTACK_RANGE = 2F;
                    private final float ATTACK_POWER = 70F;
                    private final ParticleEffect.OrdinaryColor yellowColor = ParticlePlayer.getParticleColor(255, 255, 10);
                    private Random r = new Random();
                    private final int PARTICLE_COUNT = 100;
                    public static final float PARTICLE_RAD = 2F;

                    protected void performFinalSmashAbility(final Player p)
                    {
                        setArtificiallyShieldedPlayer(p);
                        WorldType.allowFullflight(p, true);
                        SmashEntityTracker.setSpeedFactor(p, 80);
                        addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
                            private HashMap<Player, Integer> tickHitTimes = new HashMap<Player, Integer>();
                            private int iteration = 0;
                            @Override
                            public void run() {
                                for (Player victim : p.getWorld().getPlayers())
                                {
                                    if (!(tickHitTimes.containsKey(victim) && iteration - tickHitTimes.get(victim) < 10) && !victim.equals(p) && !me.happyman.worlds.WorldType.isInSpectatorMode(victim) && victim.getLocation().distance(p.getLocation()) < ATTACK_RANGE)
                                    {
                                        tickHitTimes.put(victim, iteration);
                                        attackPlayer(p, ChatColor.stripColor("Super " + toString()), p.getLocation(), victim, ATTACK_POWER, false);
                                    }
                                }
                                for (int i = 0; i < PARTICLE_COUNT; i++)
                                {
                                    float pitch = (float)(r.nextFloat()*Math.PI - Math.PI/2);
                                    float yaw = (float)(r.nextFloat()*2*Math.PI);
                                    ParticlePlayer.playBasicParticle(
                                            ParticlePlayer.getAbsFromRelLocFRU(p.getEyeLocation(),
                                                    (float)(Math.cos(pitch)*Math.cos(yaw)*PARTICLE_RAD),
                                                    (float)(Math.cos(pitch)*Math.sin(yaw)*PARTICLE_RAD),
                                                    (float)(Math.sin(pitch)*PARTICLE_RAD), false),
                                            yellowColor, true);
                                }
                                if (!p.isFlying())
                                {
                                    WorldType.allowFullflight(p, true);
                                }
                                iteration++;
                            }
                        }, 0, 1));
                    }

                    @Override
                    protected void endFinalSmashAbility(Player p)
                    {
                        SmashEntityTracker.resetSpeedAlteredPlayer(p);
                        forgetArtificiallyShieldedPlayer(p);
                        p.setAllowFlight(false);
                    }
                }, 1, 1, false), new LeatherArmorSet(ChatColor.BLUE + "Sonic", new int[] {42, 82, 198}, new int[] {240, 219, 165}, new int[] {36, 95, 83}, new int[] {202, 42, 64}),
                new SpecialSword.NormalSpecialSword(Material.IRON_SWORD),
                new RocketItem(ChatColor.BLUE + "" + ChatColor.BOLD + "Double-jump Rocket", 2, 6.71f))
                {
                    class SonicHoming extends SpecialItemWithCharge implements Listener
                    {
                        private HashMap<Player, Integer> movementTasks;
                        private final int HOW_LONG_IT_TAKES_TICKS = 1*20;
                        private final float ATTACK_RANGE = 9F;
                        private final float FINAL_RANGE = 1.3F;
                        private final float ATTACK_POWER = 14F;
                        private final float SLOPE_TO_WAYPOINT = 1F;
                        private final int PARTICLE_COUNT = 60;
                        private static final float PARTICLE_RAD = 0.5F;
                        private Random r = new Random();
                        private final ParticleEffect.OrdinaryColor blueColor = ParticlePlayer.getParticleColor(0, 0, 255);
                        private static final float MIN_RISE = 5F;

                        public SonicHoming()
                        {
                            super(new UsefulItemStack(Material.SLIME_BALL, ChatColor.BLUE + "" + ChatColor.BOLD + "Homing Attack", new Enchantment[] {Enchantment.DIG_SPEED}, new int[] {9001}),
                                    .025F, 1, ChargingMode.CHARGE_AUTOMATICALLY);
                            movementTasks = new HashMap<Player, Integer>();
                        }

                        @Override
                        public boolean canBeUsed(Player p)
                        {
                            return super.canBeUsed(p) && !movementTasks.containsKey(p);
                        }

                        @Override
                        public void performRightClickAction(final Player p, Block blockClicked)
                        {
                            super.performRightClickAction(p, blockClicked);
                            final Player targetPlayer = SmashManager.getNearestPlayerToPlayer(p);
                            if (targetPlayer != null)
                            {
                                final Location targetLocation = targetPlayer.getLocation();
                                if (p.getLocation().distance(targetLocation) <= ATTACK_RANGE)
                                {
                                    float flatD = (float)Math.sqrt(Math.pow(targetLocation.getX() - p.getLocation().getX(), 2) + Math.pow(targetLocation.getY() - p.getLocation().getY(), 2));
                                    final Location wayPoint = p.getLocation().clone();
                                    wayPoint.setY(targetLocation.getY() + SLOPE_TO_WAYPOINT*flatD);
                                    if (wayPoint.getY() - p.getLocation().getY() < MIN_RISE)
                                    {
                                        wayPoint.setY(p.getLocation().getY() + MIN_RISE);
                                    }
                                    int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
                                        Location currentTarget = wayPoint;
                                        boolean takenCareOfThankYou = false;

                                        private boolean isCloseToTarget(Player p)
                                        {
                                            return currentTarget != null && p.getLocation().distance(currentTarget) < 0.5F;
                                        }

                                        public void run()
                                        {
                                            if (currentTarget != null)
                                            {
                                                for (int i = 0; i < PARTICLE_COUNT; i++)
                                                {
                                                    float pitch = (float)(r.nextFloat()*Math.PI - Math.PI/2);
                                                    float yaw = (float)(r.nextFloat()*2*Math.PI);
                                                    ParticlePlayer.playBasicParticle(
                                                            ParticlePlayer.getAbsFromRelLocFRU(p.getEyeLocation(),
                                                                    (float)(Math.cos(pitch)*Math.cos(yaw)*PARTICLE_RAD),
                                                                    (float)(Math.cos(pitch)*Math.sin(yaw)*PARTICLE_RAD),
                                                                    (float)(Math.sin(pitch)*PARTICLE_RAD), false),
                                                            blueColor, true);
                                                }

                                                sendEntityTowardLocation(currentTarget, p, 1, false);
                                                if (isCloseToTarget(p))
                                                {
                                                    if (currentTarget == wayPoint)
                                                    {
                                                        currentTarget = targetLocation;
                                                    }
                                                    else if (currentTarget == targetLocation)
                                                    {
                                                        currentTarget = null;
                                                    }
                                                }

                                                if (attackPlayersInRange(p, "Homing Attack", ATTACK_POWER, FINAL_RANGE))
                                                {
                                                    currentTarget = null;
                                                }
                                            }
                                            else if (!takenCareOfThankYou)
                                            {
                                                movementTasks.remove(p);
                                                takenCareOfThankYou = true;
                                            }
                                        }
                                    }, 0, 1);
                                    movementTasks.put(p, task);
                                    cancelTaskAfterDelay(task, HOW_LONG_IT_TAKES_TICKS);
                                }
                            }
                        }

                        @EventHandler
                        public void oopsGotHit(EntityDamageByEntityEvent e)
                        {
                            if (e.getEntity() instanceof Player)
                            {
                                Player p = (Player)e.getEntity();
                                if (movementTasks.containsKey(p))
                                {
                                    Bukkit.getScheduler().cancelTask(movementTasks.get(p));
                                    movementTasks.remove(p);
                                }
                            }
                        }
                    }

                    @Override
                    public void addAdditionalItems()
                    {
                        addItem(new SpecialItemWithUsages(
                                new UsefulItemStack(Material.IRON_INGOT, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Spring"), 1, true)
                        {
                            @Override
                            public void performRightClickAction(final Player p, Block blockClicked)
                            {
                                addUsage(p);
                                Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
                                {
                                    public String call()
                                    {
                                        p.setVelocity(new Vector(0, 3, 0));
                                        return "";
                                    }
                                });
                            }
                        });
                        addItem(new SonicHoming());
                    }
                },
        Wario(
                new SmashKitProperties(ChatColor.YELLOW + "" + ChatColor.BOLD + "Warly", Material.INK_SACK, (short) 11, ChatColor.DARK_AQUA + "" + ChatColor.ITALIC + "Wah!",
                new FinalSmash()
                {
                    private static final int HIT_COOLDOWN = 10;
                    private final float POWER_AT_CENTER = 140F;

                    private final float EXPLOSIONS_PER_REVOLUTION_PER_RADIUS = 1;
                    private final float POLYGONAL_EXPLOSIONS_PER_BLOCK = 1F;

                    private final int DURATION = 100;

                    private final float MIN_BOTTOM_RAD = 1F;
                    private final float MAX_BOTTOM_RAD = 10F;
                    private final float BOTTOM_RAD_INCREMENT = (MAX_BOTTOM_RAD - MIN_BOTTOM_RAD)/DURATION;

                    private final float MIN_HEIGHT = 2F;
                    private final float MAX_HEIGHT = 15F;
                    private final float HEIGHT_INCREMENT = (MAX_HEIGHT - MIN_HEIGHT)/DURATION;

                    private final float TOP_RAD_ACTIVATION_TIME = 0.333F*DURATION;
                    private final float MIN_TOP_RAD = MIN_BOTTOM_RAD;
                    private final float MAX_TOP_RAD = 5F;
                    private final float TOP_RAD_INCREMENT = (MAX_TOP_RAD - MIN_TOP_RAD)/(DURATION - TOP_RAD_ACTIVATION_TIME);

                    private Vector zero = new Vector().zero();

                    private void playExplosionDisk(Player p, Location centerLocation, float radius)
                    {
                        if (radius > 0)
                        {
                            for (float rad = 0; rad < (float)Math.PI*2; rad += 2*(float)Math.PI/(radius*EXPLOSIONS_PER_REVOLUTION_PER_RADIUS))
                            {
                                Location explosionLocation = centerLocation.clone().add(radius*Math.cos(rad), 0, radius*Math.sin(rad));
                                for (Player victim : centerLocation.getWorld().getPlayers())
                                {
                                    attackPlayer(p, "Nuclear Fart", p.getLocation(), victim, POWER_AT_CENTER*10/(10 + (float)victim.getLocation().distance(p.getLocation())), false);
                                }
                                centerLocation.getWorld().playEffect(explosionLocation, Effect.EXPLOSION_LARGE, 0, SEARCH_DISTANCE);
                            }
                            playExplosionDisk(p, centerLocation, radius - 1);
                        }
                    }

                    @Override
                    protected void performFinalSmashAbility(final Player p)
                    {
                        setArtificiallyShieldedPlayer(p);
                        SmashEntityTracker.setSpeedFactor(p, 0F);
                        SmashManager.preventJumping(p);
                        addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
                            private HashMap<Player, Integer> tickHitTimes = new HashMap<Player, Integer>();
                            int iteration = 0;
                            float bottomRad = MIN_BOTTOM_RAD;
                            float height = MIN_HEIGHT;
                            float topRad = MIN_TOP_RAD;
                            public void run() {
                                if (iteration <= DURATION)
                                {
                                    if (iteration < DURATION)
                                    {
                                        p.setVelocity(zero);

                                        if (iteration % 3 == 0)
                                        {
                                            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_LIGHTNING_THUNDER, 1F, 1F);
                                        }
                                        for (float vert = 0; vert < height; vert += POLYGONAL_EXPLOSIONS_PER_BLOCK)
                                        {
                                            p.getWorld().playEffect(p.getLocation().clone().add(0, vert, 0), Effect.EXPLOSION_LARGE, 0, SEARCH_DISTANCE);
                                        }
                                        playExplosionDisk(p, p.getLocation().clone().add(0, 0, 0), bottomRad);
                                        playExplosionDisk(p, p.getLocation().clone().add(0, height, 0), topRad);

                                        bottomRad += BOTTOM_RAD_INCREMENT;
                                        height += HEIGHT_INCREMENT;
                                        if (iteration >= TOP_RAD_ACTIVATION_TIME)
                                        {
                                            topRad += TOP_RAD_INCREMENT;
                                        }
                                    }
                                    else
                                    {
                                        performResetAction(p);
                                    }
                                    iteration++;
                                }
                            }
                        }, 0, 1));
                    }

                    @Override
                    protected void endFinalSmashAbility(Player p)
                    {
                        forgetArtificiallyShieldedPlayer(p);
                        SmashEntityTracker.resetSpeedAlteredPlayer(p);
                        SmashManager.resetJumpBoost(p);
                    }
                }, 1F, 1F, false),
                new LeatherArmorSet(ChatColor.YELLOW + "" + ChatColor.BOLD + "Wario's Outfit", new int[]{255, 235, 50}, new int[]{32, 134, 183}, new int[]{253, 128, 163}, new int[]{88, 80, 119}),
                new SpecialSword.NormalSpecialSword(Material.IRON_SWORD),
                new RocketItem(ChatColor.YELLOW + "" + ChatColor.BOLD + "Double-Jump", 2, 6.5f))
                {
                    class WarioCooldownFart extends SpecialItemWithCharge
                    {
                        private final WarioFartProperties fartProperties;

                        public WarioCooldownFart(String name, short itemDamage, float range, float hDegrees, float maxPower, float chargeAmount, float dischargeAmount, ChargingMode mode, Sound sound, Float soundPitch, Effect effect, Float effectOffset)
                        {
                            super(new UsefulItemStack(Material.MONSTER_EGG, name),  chargeAmount, dischargeAmount, mode);
                            getItemStack().setDurability(itemDamage);
                            fartProperties = new WarioFartProperties(range, hDegrees, maxPower, sound, soundPitch, effect, effectOffset);
                        }

                        public WarioCooldownFart(String name, short itemDamage, float range, float hDegrees, float maxPower, float chargeAmount, float dischargeAmount, ChargingMode mode)
                        {
                            this(name, itemDamage, range, hDegrees, maxPower, chargeAmount, dischargeAmount, mode, null, null, null, null);
                        }

                        public WarioFartProperties getFartProperties()
                        {
                            return fartProperties;
                        }

                        @Override
                        public boolean canBeUsed(Player p)
                        {
                            if (super.canBeUsed(p))
                            {
                                if (!SmashEntityTracker.isCrouching(p))
                                {
                                    p.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "You must be crouching");
                                }
                                else
                                {
                                    return true;
                                }
                            }
                            return false;
                        }

                        @Override
                        public void performRightClickAction(Player p, Block blockClicked)
                        {
                            super.performRightClickAction(p, blockClicked);
                            performFart(this, p);
                            p.getWorld().playSound(p.getLocation(), Sound.ENTITY_ENDERMEN_HURT, 1F, 0.1F);
                        }
                    }

                    class WarioCrouchFart extends SpecialItemWithCrouchCharge implements Listener
                    {
                        private WarioFartProperties fartProperties;

                        WarioCrouchFart(String name, short itemDamage, float range, float hDegrees, float maxPower, float chargeAmount, float dischargeAmount, Sound sound, Float soundPitch, Effect effect, Float effectOffset)
                        {
                            super(new UsefulItemStack(Material.MONSTER_EGG, name),  chargeAmount, dischargeAmount);
                            getItemStack().setDurability(itemDamage);
                            fartProperties = new WarioFartProperties(range, hDegrees, maxPower, sound, soundPitch, effect, effectOffset);
                        }

                        WarioCrouchFart(String name, short itemDamage, float range, float hDegrees, float maxPower, float chargeAmount, float dischargeAmount)
                        {
                            this(name, itemDamage, range, hDegrees, maxPower, chargeAmount, dischargeAmount, null, null, null, null);
                        }

                        WarioFartProperties getFartProperties()
                        {
                            return fartProperties;
                        }
//
//                        @Override
//                        protected void decreaseCharge(Player p)
//                        {
//                            if (super.canBeUsed(p))
//                            {
//                                if (!SmashEntityTracker.isCrouching(p))
//                                {
//                                    super.decreaseCharge(p);
//                                }
//                            }
//                            else
//                            {
//                                setCharge(p, 0F);
//                            }
//                        }

                        @Override
                        public boolean canBeUsed(Player p)
                        {
                            if (super.canBeUsed(p))
                            {
                                if (!SmashEntityTracker.isCrouching(p))
                                {
                                    p.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "You must be crouching");
                                }
                                else
                                {
                                    return true;
                                }
                            }
                            return false;
                        }

                        @Override
                        public void performRightClickAction(Player p, Block blockClicked)
                        {
                            float charge = getCharge(p);
                            if (charge >= 0.3F)
                            {
                                performFart(this, p, charge);
                            }
                            else
                            {
                                p.sendMessage(ChatColor.GRAY + "" + ChatColor.ITALIC + "Your fart is ineffective");
                            }
                            super.performRightClickAction(p, blockClicked);
                        }
                    }

                    private void performFart(SpecialItemWithCharge item, Player p)
                    {
                        performFart(item, p, 1F);
                    }

                    private void performFart(SpecialItemWithCharge item, Player p, Float modifier)
                    {
                        WarioFartProperties fartProperties = null;
                        if (item instanceof WarioCooldownFart)
                        {
                            fartProperties = ((WarioCooldownFart)item).getFartProperties();
                        }
                        else if (item instanceof WarioCrouchFart)
                        {
                            fartProperties = ((WarioCrouchFart)item).getFartProperties();
                        }
                        float angle = SmashManager.getAngle(p.getLocation().getDirection(), new Vector(0, 1, 0))*180/(float)Math.PI;
                        boolean isLookingUpOrDown = angle > 180 - fartProperties.ANGLE_THAT_COUNTS_AS_DOWN || angle < fartProperties.ANGLE_THAT_COUNTS_AS_DOWN;
                        boolean proximityFart = ((Entity)p).isOnGround() && isLookingUpOrDown;

                        if (fartProperties.SOUND != null && fartProperties.SOUND_PITCH != null)
                        {
                            p.getWorld().playSound(p.getLocation(), fartProperties.SOUND, 1, 0.2F);
                        }
                        if (fartProperties.EFFECT != null && fartProperties.OFFSET != null)
                        {
                            Location l = p.getLocation().clone();
                            if (!proximityFart)
                            {
                                l.setX(l.getX() - l.getDirection().getX()*fartProperties.OFFSET);
                                l.setY(l.getY() - l.getDirection().getY()*fartProperties.OFFSET);
                                l.setZ(l.getZ() - l.getDirection().getZ()*fartProperties.OFFSET);
                            }
                            p.getWorld().playEffect(l, Effect.EXPLOSION_HUGE, 0, SEARCH_DISTANCE);
                        }
                        float fartRange = fartProperties.FART_RANGE;
                        if (proximityFart)
                        {
                            modifier *= fartProperties.PROXIMITY_NERF;
                            fartRange = fartProperties.FART_RANGE/2.3F;
                            attackPlayersInRange(p, item.getItemStack().getItemMeta().getDisplayName(), fartProperties.HIGHEST_FART_POWER*modifier, fartRange, false);
                        }
                        else
                        {
                            attackPlayersInAngleRange(p, item, fartProperties.HIGHEST_FART_POWER*modifier, fartRange, fartProperties.FART_HDEGREES, fartProperties.FART_VDEGREES, true);
                        }
                    }

                    class WarioFartProperties
                    {
                        final float FART_RANGE;
                        final float FART_HDEGREES;
                        final float FART_VDEGREES;
                        final float HIGHEST_FART_POWER;
                        final Sound SOUND;
                        final Float SOUND_PITCH;
                        final Effect EFFECT;
                        final Float OFFSET;
                        final float ANGLE_THAT_COUNTS_AS_DOWN = 30F;
                        final float PROXIMITY_NERF = 0.5F;

                        WarioFartProperties(float range, float hDegrees, float maxPower, Sound sound, Float pitch, Effect effect, Float offset)
                        {
                            FART_RANGE = range;
                            FART_HDEGREES = hDegrees;
                            FART_VDEGREES = hDegrees;
                            HIGHEST_FART_POWER = maxPower;
                            SOUND = sound;
                            SOUND_PITCH = pitch;
                            EFFECT = effect;
                            OFFSET = offset;
                        }
                    }

                    @Override
                    protected void addAdditionalItems()
                    {
                        addItem(new WarioCooldownFart(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Fart Bomb", (short)120, 10F, 90F, 80F,
                                0.01F, 1F, SpecialItemWithCharge.ChargingMode.CHARGE_AUTOMATICALLY, Sound.ENTITY_LIGHTNING_THUNDER, 0.9F, Effect.EXPLOSION_HUGE, 3F));
                        addItem(new WarioCrouchFart(ChatColor.GRAY + "" + ChatColor.BOLD + "Silent But Deadly", (short)60, 10F, 90F, 160F,
                                0.02F, 0.007F));
                    }
                };

        private final ItemStack[] contents;
        private final ItemStack[] armor;
        private final HashMap<String, SpecialItem> items;
        private final String colorlessDisplayName;

        private final SmashKitProperties properties;
        private static final int KIT_COST = 10000;

        protected abstract void addAdditionalItems();

        //    private final CompassItem compass = ;
        //    private final ShieldItem shield =;
        SmashKit(SmashKitProperties properties, LeatherArmorSet leatherArmorSet)
        {
            this(properties, leatherArmorSet, null);
        }

        SmashKit(SmashKitProperties properties, LeatherArmorSet leatherArmor, RocketItem rocket)
        {
            this(properties, leatherArmor, null, rocket);
        }

        SmashKit(SmashKitProperties properties, LeatherArmorSet leatherArmor, SpecialItem sword, RocketItem rocket)
        {
            this(properties, leatherArmor, sword, rocket, null);
        }

        SmashKit(SmashKitProperties properties, LeatherArmorSet leatherArmor, SpecialItem sword, RocketItem rocket, GroundPound groundPound)
        {
            this.contents = new ItemStack[36];
            this.properties = properties;
            this.colorlessDisplayName = ChatColor.stripColor(properties.getKitRepresenter().getItemMeta().getDisplayName());

            items = new HashMap<String, SpecialItem>();

            if (sword != null)
            {
                addItem(sword);
            }

            if (rocket != null)
            {
                addItem(rocket);
            }

            if (groundPound != null)
            {
                addItem(groundPound);
            }

            addItem(SHIELD_ITEM, 7);
            addItem(COMPASS_ITEM, 8);

            if (properties.getFinalSmash() != null)
            {
                addSecretItem(properties.getFinalSmash());
                SpecialItem[] finalSmashItems = properties.getFinalSmash().getSecretItems();
                if (finalSmashItems != null)
                {
                    for (SpecialItem item : properties.getFinalSmash().getSecretItems())
                    {
                        addSecretItem(item);
                    }
                }
            }

            this.armor = new ItemStack[4];
            this.armor[3] = leatherArmor.getHelmet();
            this.armor[2] = leatherArmor.getChestplate();
            this.armor[1] = leatherArmor.getLeggings();
            this.armor[0] = leatherArmor.getBoots();

            addAdditionalItems();
            Bukkit.getServer().getPluginManager().registerEvents(this, getPlugin());
        }

        public String getDescription()
        {
            return properties.getKitRepresenter().getItemMeta().getLore().get(0);
        }

        protected void addHiddenItem(SpecialItem item)
        {
            int i = 9;
            while (contents[i] != null && i < 36)
            {
                i++;
            }
            if (contents[i] == null)
            {
                contents[i] = item.getItemStack();
            }
            items.put(item.getItemStack().getItemMeta().getDisplayName(), item);
        }

        protected void addItem(SpecialItem item, Integer invSlot)
        {
            if (invSlot != null)
            {
                if (invSlot < 7)
                {
                    for (int later = 6, earlier = 5; earlier >= invSlot; later--, earlier--)
                    {
                        contents[later] = contents[earlier];
                    }
                }
                else if (invSlot > 8)
                {
                    for (int later = contents.length - 1, earlier = later - 1; earlier >= invSlot; earlier--, later--)
                    {
                        contents[later] = contents[earlier];
                    }
                }
                contents[invSlot] = item.getItemStack();
            }
            items.put(item.getItemStack().getItemMeta().getDisplayName(), item);
        }


        protected void addSecretItem(SpecialItem item)
        {
            addItem(item, null);
        }
        //    @Override
        //    public boolean equals(Object other)
        //    {
        //        return other instanceof SmashKit && ((SmashKit)other).getDisplayName().equals(getDisplayName());
        //    }

        protected void addItem(SpecialItem item)
        {
            int i = 0;
            while (contents[i] != null && i < 9)
            {
                i++;
            }
            if (contents[i] == null)
            {
                contents[i] = item.getItemStack();
            }
            items.put(item.getItemStack().getItemMeta().getDisplayName(), item);
        }

        public void applyKitInventory(Player p)
        {
            applyKitInventory(p, true);
        }

        public void applyKitInventory(final Player p, boolean clearItemDrops)
        {
            if (clearItemDrops)
            {
                ItemStack[] contents = p.getInventory().getContents();
                for (int i = 0; i < 36; i++)
                {
                    ItemStack item = contents[i];
                    if (getProperties().getFinalSmash().isThis(item))
                    {
                        p.getInventory().remove(item);
                    }
                }
                p.getInventory().setContents(contents);
                applyKitArmor(p);
                Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
                {
                    public String call()
                    {
                        p.updateInventory();
                        return "";
                    }
                });
            }
            else
            {
                final List<ItemStack> itemDropItems = new ArrayList<ItemStack>();
                ItemStack[] contents = p.getInventory().getContents();
                for (int i = 0; i < 36; i++)
                {
                    ItemStack item = contents[i];
                    if (ItemDropManager.isSmashDropItem(item))
                    {
                        itemDropItems.add(item);
                    }
                }
                        /*for (int i = 0; i < 9; i++)
                        {
                            if (!(p.getItemInHand().hasItemMeta() && p.getItemInHand().getItemMeta().hasDisplayName() && p.getItemInHand().getItemMeta().getDisplayName().equals(getShield().getItemStack().getItemMeta().getDisplayName())))
                            {
                                //Bukkit.getAttacker("HappyMan").sendMessage("Set item at " + i + " to " + getDisplayName() + "'s");
                                p.getInventory().setItem(i, contents[i]);
                            }
                        }
                        p.getInventory().setArmorContents(armor);*/
                applyKitInventory(p, true);
                Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
                {
                    public String call()
                    {
                        p.updateInventory();
                        return "";
                    }
                });
                Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable() {
                    public String call() {
                        for (int i = 0; i < 36  && itemDropItems.size() > 0; i++)
                        {
                            if (p.getInventory().getItem(i) == null || p.getInventory().getItem(i).getType().equals(Material.AIR))
                            {
                                p.getInventory().setItem(i, itemDropItems.get(0));
                                itemDropItems.remove(0);
                            }
                        }
                        return "";
                    }
                });
            }
        }

        public void applyKitArmor(Player p)
        {
            p.getInventory().setArmorContents(armor);
        }

        public boolean isImmuneToFire()
        {
            return properties.isImmuneToFire();
        }

        public List<SpecialItem> getSmashItems()
        {
            return new ArrayList<SpecialItem>(items.values());
        }

        public ItemStack[] getContents()
        {
            return contents;
        }

        public ItemStack[] getArmor()
        {
            return armor;
        }

        public boolean isActive(Player p)
        {
            return !canChangeKit(p);
        }

        public ItemStack getKitRepresenter()
        {
            return properties.getKitRepresenter();
        }

        public Material getKitRepresentingMaterial()
        {
            return properties.getKitRepresenter().getType();
        }

        public float getDamageIntakeMod()
        {
            return properties.getDamageIntakeMod();
        }

        public SmashKitProperties getProperties()
        {
            return properties;
        }

        public float getDamageOutPutMod()
        {
            return properties.getDamageOutputMod();
        }

        public SpecialItem getItem(ItemStack item)
        {
            if (item != null)
            {
                try
                {
                    String displayName = item.getItemMeta().getDisplayName();
                    return items.get(displayName);
                }
                catch (NullPointerException ex) {}
            }
            //            if (properties.getFinalSmash().getItemStack().equals(item))
            //            {
            //                return properties.getFinalSmash();
            //            }
            //            for (SpecialItem sitem : items.values())
            //            {
            //                //Bukkit.broadcastMessage("found " + sitem.getItemStack().getItemMeta().getDisplayName());
            //                if (sitem.isThis(item))
            //                {
            //                    return sitem;
            //                }
            //            }
            return null;
        }

        public SpecialItem getRocket()
        {
            for (int i = 1; i >= 0; i--)
            {
                if (contents[i].getType().equals(RocketItem.ROCKET_MATERIAL))
                {
                    return items.get(contents[i].getItemMeta().getDisplayName());
                }
            }
            return null;
        }

        public String getName()
        {
            return name();
        }

        public String getDisplayName()
        {
            return colorlessDisplayName;
        }

        public int getCost()
        {
            return KIT_COST;
        }
    }

    private class SmashKitListener implements Listener
    {
        private SmashKitListener()
        {
            Bukkit.getPluginManager().registerEvents(this, getPlugin());
        }

        //*****************

        @EventHandler
        public void closeInvEvent(InventoryCloseEvent e)
        {
            if (SmashKitManager.hasKitGuiOpen((Player)e.getPlayer()))
            {
                SmashKitManager.forgetOpenKitGui((Player)e.getPlayer());
            }
        }

        @EventHandler
        public void clickRepresenterEvent(InventoryClickEvent e)
        {
            if (e.getWhoClicked() instanceof Player && e.getClickedInventory() != null && e.getClickedInventory().getName().equals(KIT_SELECTION_GUI_NAME))
            {
                Player p = (Player)e.getWhoClicked();
                SmashKit kitRepresented = getKitRepresented(e.getCurrentItem());
                if (kitRepresented != null)
                {
                    e.setCancelled(true);
                    SmashKitManager.setKit(p, kitRepresented);
                }
            }
        }
    }

    private static final SpecialItemWithContCharge SHIELD_ITEM = new SpecialItemWithContCharge(
            new UsefulItemStack(Material.GLASS, ChatColor.WHITE + "" + ChatColor.BOLD + "Shield"),
            0.0018F, 0.0135F, SpecialItemWithCharge.ChargingMode.CHARGE_AUTOMATICALLY, true)
    {
        @Override
        public void performDeactivationAction(Player p)
        {
            SmashEntityTracker.setSpeedToCurrentSpeed(p);
            SmashManager.resetJumpBoost(p);
        }

        @Override
        public void performActivationAction(Player p)
        {
            p.setVelocity(new Vector().zero());
            p.setWalkSpeed(0.015F);
            SmashManager.preventJumping(p);
        }

//        @Override
//        public void performRightClickAction(final Player p)
//        {
//            super.performRightClickAction(p);
//            setCharging(p, !isCharging(p));
//        }
    };

    private static final SpecialItem COMPASS_ITEM = new CompassItem(ChatColor.GRAY + "Tracks the player with the highest score", new CompassItem.TargetMode()
    {
        private Player getPlayerWithHighScore(Player targetter)
        {
            Player highestPlayer = null;
            UsefulScoreboard scoreboard = getWorldType(targetter.getWorld()).getWorldScoreboard(targetter.getWorld());
            int highestScore = 0;
            for (Player p : targetter.getWorld().getPlayers())
            {
                Integer scoreOfTargetted = scoreboard.getSideScore(p.getName());
                if (!p.equals(targetter) && (highestPlayer == null || (scoreOfTargetted != null &&
                        scoreOfTargetted > highestScore)))
                {
                    highestScore = scoreOfTargetted;
                    highestPlayer = p;
                }
            }
            return highestPlayer;
        }

        @Override
        public CompassItem.Target getTarget(Player targetter)
        {
            if (SmashOrbTracker.hasSmashOrb(targetter.getWorld()))
            {
                return new CompassItem.Target(SmashOrbTracker.getSmashOrb(targetter.getWorld()).getLocation(), ChatColor.GOLD + "Tracking Final Smash orb!");
            }
            else
            {
                Player closestPlayer = getPlayerWithHighScore(targetter);
                if (closestPlayer != null)
                {
                    return new CompassItem.Target(closestPlayer);
                }
            }
            return null;
        }
    })
    {
        @Override
        public boolean canBeUsed(Player p)
        {
            return super.canBeUsed(p) && SmashWorldManager.isSmashWorld(p.getWorld());
        }
    };

    public static final String SMASH_KIT_CMD = "kit";
    public static final String ADD_KIT_CMD = "givekit";
    public static final String REMOVE_KIT_CMD = "removekit";
    public static final String SET_ROTATION_LEVEL_OF_KIT_CMD = "setrotation";
    public static final String GET_ROTATION_LEVEL_OF_KIT_CMD = "getrotation";
    private static final String ROTATION_LEVEL_PREFIX = "Rotation level";
    private static final String KIT_SELECTION_GUI_NAME = ChatColor.BLUE + "\u258E Select your Smash Kit!";
    private static final int HIGHEST_KIT_ROTATION_LEVEL = 3;
    private static final String PLAYER_ROTATION_LEVEL_DATANAME = "Kit rotation level";
    private static final String KIT_LIST_CMD = "kitlist";
    public static final String OWNED_KITS_DATANAME = "Smash Kits";
    private static final String SELECED_KIT_DATANAME = "Selected kit";
    private static final HashMap<Player, SmashKit> selectedKits = new HashMap<Player, SmashKit>();
    private static final ArrayList<Player> openKitGuis = new ArrayList<Player>();
    private static final List<String> alternateNoneKitNames = new ArrayList<String>();
    private static final HashMap<Player, SmashKit> maskKits = new HashMap<Player, SmashKit>();

    public SmashKitManager()
    {
        TabCompleter kitCommandsTabCompleter = new TabCompleter() {
            @Override
            public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args)
            {
                if (args.length < 2)
                {
                    if (sender instanceof Player && matchesCommand(label, SMASH_KIT_CMD))
                    {
                        List<String> completions = new ArrayList<String>();
                        for (SmashKit kit : getAvaliableKits((Player)sender))
                        {
                            if (args.length == 0  || kit.getDisplayName().toLowerCase().startsWith(args[0].toLowerCase()) || kit.getName().toLowerCase().startsWith(args[0].toLowerCase()))
                            {
                                completions.add(kit.getDisplayName());
                            }
                        }
                        return completions;
                    }
                    else if (matchesCommand(label, ADD_KIT_CMD) || matchesCommand(label, REMOVE_KIT_CMD)
                            || matchesCommand(label, SET_ROTATION_LEVEL_OF_KIT_CMD) || matchesCommand(label, GET_ROTATION_LEVEL_OF_KIT_CMD))
                    {
                        List<String> completions = new ArrayList<String>();
                        for (SmashKit kit : SmashKit.values())
                        {
                            if (args.length == 0 || kit.getDisplayName().toLowerCase().startsWith(args[0].toLowerCase()) || kit.getName().toLowerCase().startsWith(args[0].toLowerCase()))
                            {
                                completions.add(kit.getDisplayName());
                            }
                        }
                        return completions;
                    }
                }
                return null;
            }
        };
        setExecutor(SMASH_KIT_CMD, this, kitCommandsTabCompleter);
        setExecutor(KIT_LIST_CMD, this);

        setExecutor(ADD_KIT_CMD, this, kitCommandsTabCompleter);
        setExecutor(REMOVE_KIT_CMD, this, kitCommandsTabCompleter);
        setExecutor(SET_ROTATION_LEVEL_OF_KIT_CMD, this, kitCommandsTabCompleter);
        setExecutor(GET_ROTATION_LEVEL_OF_KIT_CMD, this, kitCommandsTabCompleter);

        new SmashKitListener();

        alternateNoneKitNames.add("None");
        alternateNoneKitNames.add("Default");
        alternateNoneKitNames.add("Normal");
        alternateNoneKitNames.add("default");
        alternateNoneKitNames.add("");
        //        for (String colorlessName : alternateNoneKitNames)
        //        {
        //            nameToKitMap.put(colorlessName, DEFAULT_KIT);
        //        }
        //
        //        kitNames = new ArrayList<String>(kitRepresenters.values());
        //        for (int i = 1; i < kitNames.size(); i++)
        //        {
        //            if (kitNames.get(i).compareTo(kitNames.get(i-1)) < 0)
        //            {
        //                for (int j = 0; j < i; j++)
        //                {
        //                    if (kitNames.get(i).compareTo(kitNames.get(j)) < 0)
        //                    {
        //                        kitNames.add(j, kitNames.get(i));
        //                        kitNames.remove(i+1);
        //                        break;
        //                    }
        //                }
        //            }
        //        }
    }

    public static boolean isShielded(Player p)
    {
        return !SHIELD_ITEM.isCharging(p) || isArtificiallyShielded(p);
    }

    public static float getRemainingShield(Player p)
    {
        return SHIELD_ITEM.getCharge(p);
    }

    public static SmashKit getDefaultKit()
    {
        return SmashKit.Mario;
    }

    public static void removeKitMask(Player p)
    {
        if (hasMaskKit(p))
        {
            maskKits.remove(p);
        }
    }

    public static void setMaskKit(Player p, SmashKit maskKit)
    {
        if (maskKit != null)
        {
            maskKits.put(p, maskKit);
        }
        else
        {
            removeKitMask(p);
        }
    }

    public static SmashKit getMaskKit(Player p)
    {
        return maskKits.get(p);
    }

    public static boolean hasMaskKit(Player p)
    {
        return maskKits.containsKey(p);
    }

    public static int getRotationLevel(Player p)
    {
        return FileManager.getIntData(getGeneralPlayerFile(p), PLAYER_ROTATION_LEVEL_DATANAME);
    }

    public static void setRotationLevel(Player p, int level)
    {
        FileManager.putData(getGeneralPlayerFile(p), PLAYER_ROTATION_LEVEL_DATANAME, level);
    }

    //    public static void givePlayerKit(Player p, String kitName)
    //    {
    //        SmashKit kit = getKit(kitName);
    //        if (isSmashKit(kitName)/* && !getGeneralPlayerData(p, OWNED_KITS_DATANAME).contains(capitalize(kitName))*/)
    //        {
    //            addGeneralPlayerDatumEntry(p, OWNED_KITS_DATANAME, capitalize(kitName));
    //        }
    //    }

    public static boolean givePlayerKit(String p, SmashKit kit)
    {
        return FileManager.addDataToList(getGeneralPlayerFile(p), OWNED_KITS_DATANAME, kit.getName());
    }

    public static boolean givePlayerKit(Player p, SmashKit kit)
    {
        return givePlayerKit(p.getName(), kit);
    }

    public static boolean removePlayerKit(String p, SmashKit kit)
    {
        return FileManager.removeDataFromList(getGeneralPlayerFile(p), OWNED_KITS_DATANAME, kit.getName());
    }

    public static boolean removePlayerKit(Player p, SmashKit kit)
    {
        return removePlayerKit(p.getName(), kit);
    }

    public static SmashKit getKit(Player p)
    {
        if (!selectedKits.containsKey(p))
        {
            SmashKit currentKit = getKit(FileManager.getData(getGeneralPlayerFile(p), SELECED_KIT_DATANAME));
            currentKit = currentKit != null ? currentKit : getDefaultKit();
            FileManager.putData(getGeneralPlayerFile(p), SELECED_KIT_DATANAME, currentKit.getName());
            selectedKits.put(p, currentKit);
        }
        return selectedKits.get(p);
    }

    public static SmashKit getCurrentKit(Player p)
    {
        SmashKit maskKit = maskKits.get(p);
        if (maskKit != null)
        {
            return maskKit;
        }
        return getKit(p);
    }

    public static String getKitNameOutput(Player p)
    {
        SmashKit kit = getKit(p);
        String kitNameOutput = kit.getDisplayName();
        SmashKit maskKit = getMaskKit(p);
        if (maskKit != null)
        {
            kitNameOutput += "-" + maskKit.getDisplayName();
        }
        return kitNameOutput;
    }

            /*public static boolean isKirbyTheif(Player p)
            {
                return kirbies.contains(p) && !getKit(p).equals(getMaskKit(p));
            }*/

    public static void unloadSelectedKit(Player p)
    {
        if (selectedKits.containsKey(p))
        {
            FileManager.putData(getGeneralPlayerFile(p), SELECED_KIT_DATANAME, selectedKits.get(p).toString());
            selectedKits.remove(p);
        }
    }

    public static FinalSmash getFinalSmash(Player p)
    {
        SmashKit kit = getKit(p);
        if (kit != null)
        {
            return kit.getProperties().getFinalSmash();
        }
        sendErrorMessage("Error! Could not get final Smash for " + p.getName());
        return null;
    }

    private List<ItemStack> getKitRepresenters(Player p)
    {
        List<ItemStack> kitRepresenters = new ArrayList<ItemStack>();
        List<String> ownedKits = getPersonallyOwnedKits(p);
        List<String> otherwiseAvaliableKits = getAvaliableRotationKits(p);
        for (SmashKit kit : SmashKit.values())
        {
            if (ownedKits.contains(kit.getName()) || otherwiseAvaliableKits.contains(kit.getName()))
            {
                kitRepresenters.add(kit.getKitRepresenter());
            }
        }
        return kitRepresenters;
    }

    public static SmashKit getKit(String name, boolean allowDisplayNames)
    {
        for (SmashKit kit : SmashKit.values())
        {
            if (!allowDisplayNames && kit.getName().equals(name) || allowDisplayNames && kit.getName().equalsIgnoreCase(name))
            {
                return kit;
            }
        }
        if (allowDisplayNames)
        {
            for (SmashKit kit : SmashKit.values())
            {
                if (kit.getDisplayName().equalsIgnoreCase(name))
                {
                    return kit;
                }
            }
            for (String noneName : alternateNoneKitNames)
            {
                if (noneName.equalsIgnoreCase(name))
                {
                    return getDefaultKit();
                }
            }
        }
        return null;
    }

    public static SmashKit getKit(String name)
    {
        return getKit(name, false);
    }

    public static boolean canChangeKit(Player p)
    {
        return !SmashWorldManager.isSmashWorld(p.getWorld()) || !SmashWorldManager.gameHasStarted(p.getWorld()) || SmashWorldManager.gameHasEnded(p.getWorld()) || WorldType.isInSpectatorMode(p);
    }

    private static void setKit(Player p, SmashKit kit)
    {
        if (kit == null)
        {
            p.sendMessage(ChatColor.RED + "That is not a Smash kit!");
        }
        else if (!canChangeKit(p))
        {
            p.sendMessage(ChatColor.RED + "You cannot choose a kit whilst the game is in progress!");
        }
        else if (!getPersonallyOwnedKits(p).contains(kit.getName()) && !getAvaliableRotationKits(p).contains(kit.getName()))
        {
            p.sendMessage(ChatColor.RED + "You don't own " + kit.getDisplayName() + ChatColor.RESET + "" + ChatColor.RED + ". Do /kitlist to see which kits you can use.");
        }
        else
        {
            FileManager.putData(getGeneralPlayerFile(p), SELECED_KIT_DATANAME, kit.getName());
            if (SmashWorldManager.isSmashWorld(p.getWorld()) && !SmashWorldManager.gameIsInProgress(p.getWorld()))
            {
                WorldType.sendMessageToWorld(p.getWorld(), kit.getDescription());
                kit.applyKitInventory(p);
            }
            else
            {
                p.sendMessage(ChatColor.GREEN + "You have selected kit " + kit.getDisplayName() + ".");
            }
            if (hasKitGuiOpen(p))
            {
                p.closeInventory();
            }
            selectedKits.put(p, kit);
        }
    }

    public void openKitGui(Player p)
    {
        List<ItemStack> kitRepresenters = getKitRepresenters(p);
        int size = kitRepresenters.size();
        int rows = size/9 + 1;
        if (size % 9 == 0)
        {
            rows -= 1;
        }
        if (!openKitGuis.contains(p))
        {
            openKitGuis.add(p);
        }
        else
        {
            sendErrorMessage("Error! Someone who already had their kit inventory open opened it again somehow!");
        }
        Inventory kitInv = Bukkit.createInventory(p, rows*9, KIT_SELECTION_GUI_NAME);
        for (int i = 0; i < kitRepresenters.size(); i++)
        {
            kitInv.setItem(i, kitRepresenters.get(i));
        }
        p.openInventory(kitInv);
    }

    public static boolean hasKitGuiOpen(Player p)
    {
        return openKitGuis.contains(p);
    }

    private static void forgetOpenKitGui(Player p)
    {
        if (hasKitGuiOpen(p))
        {
            openKitGuis.remove(p);
        }
    }

    private static SmashKit getKitRepresented(ItemStack representer)
    {
        for (SmashKit kit : SmashKit.values())
        {
            if (kit.getKitRepresenter().getItemMeta().getDisplayName().equals(representer.getItemMeta().getDisplayName()))
            {
                return kit;
            }
        }
        return null;
    }

    public static boolean setRotationLevel(SmashKit kit, int newLevel)
    {
        int currentLevel = getRotationLevelFromFile(kit);
        if (newLevel > HIGHEST_KIT_ROTATION_LEVEL)
        {
            newLevel = HIGHEST_KIT_ROTATION_LEVEL;
        }
        if (currentLevel != newLevel)
        {
            FileManager.removeDataFromList(getRotationLevelFile(), getRotationLevelDataname(currentLevel), kit.getName());
            if (newLevel > 0)
            {
                FileManager.addDataToList(getRotationLevelFile(), getRotationLevelDataname(newLevel), kit.getName());
            }
            return true;
        }
        return false;
    }

    public static int getRotationLevelFromFile(SmashKit kit)
    {
        int i;
        for (i = HIGHEST_KIT_ROTATION_LEVEL; i > 0 && !getKitsAtRotationLevel(i).contains(kit.getName()); i--);
        return i;
    }

    public String getRotationLevelDescription(SmashKit kit)
    {
        return getRotationLevelDescription(getRotationLevelFromFile(kit));
    }

    public String getRotationLevelDescription(int level)
    {
        if (level == 0)
        {
            return "Free";
        }
        return "" + level;
    }

    private static List<String> getAllKitNames()
    {
        List<String> kitNames = new ArrayList<String>();
        for (SmashKit kit : SmashKit.values())
        {
            kitNames.add(kit.getName());
        }
        return kitNames;
    }

    private static List<String> getAvaliableRotationKits(int rotationLevel)
    {
        List<String> allowedKits = getAllKitNames();
        for (int i = HIGHEST_KIT_ROTATION_LEVEL; i > rotationLevel; i--)
        {
            allowedKits.removeAll(getKitsAtRotationLevel(i));
        }
        if (!allowedKits.contains(getDefaultKit().getName()))
        {
            allowedKits.add(0, getDefaultKit().getName());
        }
        return allowedKits;
    }

    private static List<String> getKitsAtRotationLevel(int rotatationLevel)
    {
        return FileManager.getDataList(getRotationLevelFile(), getRotationLevelDataname(rotatationLevel));
    }

    private static List<String> getPersonallyOwnedKits(Player p)
    {
        return FileManager.getDataList(getGeneralPlayerFile(p), OWNED_KITS_DATANAME);
    }

    private static File getRotationLevelFile()
    {
        return FileManager.getServerDataFile("", "Rotation Kits", true);
    }

    public static String getRotationLevelDataname(int rotationLevel)
    {
        return ROTATION_LEVEL_PREFIX + rotationLevel;
    }

    private void sendAvaliableKitList(Player p)
    {
        ArrayList<InteractiveChat.MessagePart> messages = new ArrayList<InteractiveChat.MessagePart>();
        String commas = "";
        for (String kit : getAvaliableKitsForOutput(p))
        {
            messages.add(new InteractiveChat.MessagePart(commas + kit, null, "kit " + ChatColor.stripColor(kit)));
            commas = " ";
        }
        InteractiveChat.sendMessage(p, messages);
    }

    public List<String> getAvaliableKitsForOutput(Player p)
    {
        //        List<String> avaliableKits = new ArrayList<String>();
        //        int rotationLevel = getRotationLevelFromFile(p);
        //        List<String> freeKits = null;
        //        List<String> vipKits = null;
        //        List<String> mvpKits = null;
        //        List<String> proKits = null;
        //        if (rotationLevel >= 0)
        //        {
        //            freeKits = getAvaliableRotationKits(0);
        //            if (rotationLevel >= 1)
        //            {
        //                vipKits = getAvaliableRotationKits(1);
        //                if (rotationLevel >= 2)
        //                {
        //                    mvpKits = getAvaliableRotationKits(2);
        //                    if (rotationLevel >= HIGHEST_KIT_ROTATION_LEVEL)
        //                    {
        //                        proKits = getAvaliableRotationKits(3);
        //                    }
        //                }
        //            }
        //        }
        List<String> result = new ArrayList<String>();
        List<String> ownedKits = getPersonallyOwnedKits(p);
        List<String> rotationKits = getAvaliableRotationKits(p);
        for (SmashKit kit : SmashKit.values())
        {
            String color;
            String kitName = kit.getName();
            if (ownedKits.contains(kitName))
            {
                color = "" + ChatColor.GREEN;
            }
            else if (rotationKits.contains(kitName))
            {
                color = "" + ChatColor.AQUA;
            }
            //            else if (freeKits != null && freeKits.contains(kitName))
            //            {
            //                color = "" + ChatColor.AQUA;
            //            }
            //            else if (vipKits != null && vipKits.contains(kitName))
            //            {
            //                color = ChatColor.GREEN + "" + ChatColor.ITALIC;
            //            }
            //            else if (mvpKits != null && mvpKits.contains(kitName))
            //            {
            //                color = ChatColor.BLUE + "" + ChatColor.ITALIC;
            //            }
            //            else if (proKits != null && proKits.contains(kitName))
            //            {
            //                color = ChatColor.GOLD + "" + ChatColor.ITALIC;
            //            }
            else
            {
                color = "" + ChatColor.RED;
            }
            result.add(color + kit.getDisplayName());
        }
        return result;
    }

    public static List<SmashKit> getUnavaliableKits(Player p)
    {
        List<SmashKit> result = new ArrayList<SmashKit>();
        List<String> ownedKits = getPersonallyOwnedKits(p);
        List<String> rotationKits = getAvaliableRotationKits(p);

        for (SmashKit kit : SmashKit.values())
        {
            if (!ownedKits.contains(kit.getName()) && !rotationKits.contains(kit.getName()))
            {
                result.add(kit);
            }
        }
        return result;
    }

    public static List<SmashKit> getAvaliableKits(Player p)
    {
        List<SmashKit> result = new ArrayList<SmashKit>();
        List<String> ownedKits = getPersonallyOwnedKits(p);
        List<String> rotationKits = getAvaliableRotationKits(p);

        for (SmashKit kit : SmashKit.values())
        {
            if (ownedKits.contains(kit.getName()) || rotationKits.contains(kit.getName()))
            {
                result.add(kit);
            }
        }
        return result;
    }

    private static List<String> getAvaliableRotationKits(Player p)
    {
        return getAvaliableRotationKits(getRotationLevel(p));
    }

    public static boolean hasFinalSmashActive(Player p)
    {
        return FinalSmash.hasFinalSmashActive(p);
    }

    public static List<SpecialItem> getKitItems(Player p)
    {
        SmashKit kit = SmashKitManager.getKit(p);
        SmashKit maskKit = SmashKitManager.getMaskKit(p);
        List<SpecialItem> items = kit.getSmashItems();
        if (maskKit != null && kit != maskKit)
        {
            items.addAll(maskKit.getSmashItems());
        }
        return items;
    }

    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
    {
        if (matchesCommand(label, SMASH_KIT_CMD))
        {
            if (!(sender instanceof Player) || args.length > 0 && (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("help")))
            {
                if (!(sender instanceof Player))
                {
                    sender.sendMessage(ChatColor.GREEN + "" + ChatColor.UNDERLINE + "Here are all the Smash kits: ");
                    for (SmashKit kit : SmashKit.values())
                    {
                        sender.sendMessage(ChatColor.GOLD + kit.getDisplayName());
                    }
                }
                else
                {
                    sender.sendMessage(ChatColor.GREEN + "Smash Kits:");
                    sendAvaliableKitList((Player)sender);
                }
                return true;
            }
            else if (args.length == 0)
            {
                openKitGui((Player)sender);
            }
            else
            {
                for (SmashKit kit : SmashKit.values())
                {
                    if (kit.getDisplayName().equalsIgnoreCase(args[0]) || kit.getName().equalsIgnoreCase(args[0]))
                    {
                        setKit((Player)sender, kit);
                        return true;
                    }
                }
                sender.sendMessage(ChatColor.RED + "Kit " + args[0] + " not found.");
            }
            return true;
        }
        else if (matchesCommand(label, KIT_LIST_CMD))
        {
            if (!(sender instanceof Player))
            {
                sender.sendMessage(ChatColor.GREEN + "" + ChatColor.UNDERLINE + "Here are all the Smash kits: ");
                for (SmashKit kit : SmashKit.values())
                {
                    sender.sendMessage(ChatColor.GOLD + kit.getDisplayName());
                }
            }
            else
            {
                sender.sendMessage(ChatColor.GREEN + "" +  ChatColor.BOLD + "List of Smash Kits:");
                sendAvaliableKitList((Player)sender);
            }
            return true;
        }
        else if (matchesCommand(label, ADD_KIT_CMD))
        {
            if (args.length < 2)
            {
                return false;
            }
            String who = args[1];
            Player test = Bukkit.getPlayer(who);
            if (test != null)
            {
                who = test.getName();
            }
            else
            {
                if (!FileManager.getGeneralPlayerFile(who, false).exists())
                {
                    sender.sendMessage(ChatColor.RED + "Player " + who + " not found.");
                    return true;
                }
            }

            String kitNameInput = args[0];
            SmashKit kit = getKit(kitNameInput, true);

            if (kit != null)
            {
                if (givePlayerKit(who, kit))
                {
                    sender.sendMessage(ChatColor.GREEN + "Gave the kit " + kit.getDisplayName() + " to " + who + "!");
                }
                else
                {
                    sender.sendMessage(ChatColor.GREEN + who + " already owned " + kit.getDisplayName() + ".");
                }
            }
            else
            {
                sender.sendMessage(ChatColor.RED + "Kit " + kitNameInput + " does not exist!");
            }
            return true;
        }
        else if (matchesCommand(label, REMOVE_KIT_CMD))
        {
            if (args.length < 2)
            {
                return false;
            }
            String who = args[1];
            Player test = Bukkit.getPlayer(who);
            if (test != null)
            {
                who = test.getName();
            }
            else
            {
                if (!FileManager.getGeneralPlayerFile(who, false).exists())
                {
                    sender.sendMessage(ChatColor.RED + "Player " + who + " not found.");
                    return true;
                }
            }

            String kitNameInput = args[0];
            SmashKit kit = getKit(kitNameInput, true);

            if (kit != null)
            {
                if (removePlayerKit(who, kit))
                {
                    sender.sendMessage(ChatColor.GREEN + "Took the kit " + kit.getDisplayName() + " away from " + who + "!");
                }
                else
                {
                    sender.sendMessage(ChatColor.GREEN + who + " already didn't own " + kit.getDisplayName() + ".");
                }
            }
            else
            {
                sender.sendMessage(ChatColor.RED + "Kit " + kitNameInput + " does not exist!");
            }
            return true;
        }
        else if (matchesCommand(label, SET_ROTATION_LEVEL_OF_KIT_CMD))
        {
            if (args.length < 2)
            {
                return false;
            }
            String kitName = args[0];
            SmashKit kit = getKit(kitName, true);
            if (kit == null)
            {
                sender.sendMessage(ChatColor.RED + kitName + " not found.");
            }
            else
            {
                String levelInput = args[1];
                try
                {
                    if (setRotationLevel(kit, Integer.valueOf(levelInput)))
                    {
                        sender.sendMessage(ChatColor.GREEN + "Set the " + ROTATION_LEVEL_PREFIX + " of " + kit.getDisplayName() + " to " + levelInput);
                    }
                    else
                    {
                        sender.sendMessage(ChatColor.RED + kit.getDisplayName() + " was already at rotation level " + ROTATION_LEVEL_PREFIX + "." + levelInput);

                    }
                }
                catch(NumberFormatException ex)
                {
                    sender.sendMessage(ChatColor.RED + "Did not recognize " + levelInput + " as an integer.");
                }
            }
            return true;
        }
        else if (matchesCommand(label, GET_ROTATION_LEVEL_OF_KIT_CMD))
        {
            if (args.length < 1)
            {
                return false;
            }
            String kitName = args[0];
            SmashKit kit = getKit(kitName, true);
            if (kit == null)
            {
                sender.sendMessage(ChatColor.RED + kitName + " not found.");
            }
            else
            {
                sender.sendMessage(ChatColor.GREEN + kit.getDisplayName() + "'s " + ROTATION_LEVEL_PREFIX + ": " + getRotationLevelDescription(kit));
            }
            return true;
        }
        return false;
    }
}