package me.happyman.SmashKits;

import me.happyman.ItemTypes.*;
import me.happyman.SmashItemDrops.Hammer;
import me.happyman.SmashKitMgt.*;
import me.happyman.SpecialKitItems.PitBow;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashAttackListener;
import me.happyman.utils.ParticleEffect;
import me.happyman.utils.SmashEntityTracker;
import me.happyman.utils.SmashWorldManager;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.*;

public class Pit extends SmashKit
{
    private static List<Player> playersWhoCantUseRockets = new ArrayList<Player>();;
    private static final List<ParticleEffect.OrdinaryColor> colors = new ArrayList<ParticleEffect.OrdinaryColor>();;
    private static Random r = new Random();

    public Pit()
    {
        super(
            new SmashKitProperties(ChatColor.AQUA + "" + ChatColor.BOLD + "Lit", Material.GOLD_HOE, ChatColor.WHITE + "" + ChatColor.ITALIC + "The fight is on!",
                new FinalSmash(30, new Sound[] {Sound.WITHER_DEATH}, new float[] {3F}, new float[] {.1F})
                {
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
                    List<ParticleEffect.OrdinaryColor> whiteWingColors = new ArrayList<ParticleEffect.OrdinaryColor>(Arrays.asList(SmashManager.getParticleColor(255, 255, 255)));

                    protected void performFinalSmashAbility(final Player p)
                    {
                        final int intervalTicks = Math.round((float)4*20/armySize);
                        final World w = p.getWorld();
                        addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable() {
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

                        addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable() {
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
                                            Pit.playWing(e, iteration, whiteWingColors);
                                            if (!targetPlayers.containsKey(e))
                                            {
                                                List<Player> avaliablePlayers = w.getPlayers();
                                                for (int i = 0; i < avaliablePlayers.size(); i++)
                                                {
                                                    Player candidate = avaliablePlayers.get(i);
                                                    if (!targetPlayers.containsValue(candidate) && !candidate.equals(p) && !SmashWorldManager.isInSpectatorMode(candidate))
                                                    {
                                                        targetPlayers.put(e, candidate);
                                                        w.playSound(e.getLocation(), Sound.GHAST_FIREBALL, 1, 0.2F);
                                                    }
                                                }
                                            }
                                            if (targetPlayers.containsKey(e) && iteration >= intervalTicks*armySize)
                                            {
                                                Player target = targetPlayers.get(e);
                                                e.setTarget(target);
                                                if (e.getLocation().distance(target.getLocation()) < 1)
                                                {
                                                    if (!SmashAttackListener.isShielded(target))
                                                    {
                                                        SmashAttackListener.attackPlayer(p, "Palutena's Army", e.getLocation(), target, 20/SmashAttackListener.DAMAGE_GAIN_FACTOR, false);
                                                    }
                                                    for (int i = 0; i < 3; i++)
                                                    {
                                                        w.playSound(e.getLocation(), Sound.FIREWORK_BLAST, 1, 1);
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

                        SmashAttackListener.setArtificiallyShieldedPlayer(p);
                        final boolean onGround = ((Entity)p).isOnGround() || p.getLocation().getY() % 0.5 < 0.001;
                        final float oldFactor = SmashEntityTracker.getSpeedFactor(p);
                        if (onGround)
                        {
                            SmashEntityTracker.setSpeedFactor(p, 0);
                        }
                        Bukkit.getScheduler().scheduleSyncDelayedTask(SmashManager.getPlugin(), new Runnable() {
                            public void run()
                            {
                                if (onGround)
                                {
                                    SmashEntityTracker.setSpeedFactor(p, oldFactor);
                                }
                                SmashAttackListener.forgetArtificiallyShieldedPlayer(p);
                            }
                        }, 20);

                        SmashWorldManager.sendMessageToWorld(p.getWorld(), "<" + p.getDisplayName() + "> " + ChatColor.WHITE + "" + ChatColor.ITALIC + "All troops, move out!");


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
            new LeatherArmorSet(ChatColor.YELLOW + "" + ChatColor.BOLD + "Lit's Outfit", new int[]{113, 76, 58}, new int[]{230, 245, 251}, null, new int[]{74, 55, 40}),
            new PitBow(),
            new RocketItem(ChatColor.YELLOW + "" + ChatColor.BOLD + "Triple-jump", 3, 1F)
            {
                @Override
                public boolean canUseItem(Player p)
                {
                    return !playersWhoCantUseRockets.contains(p) && super.canUseItem(p);
                }

                @Override
                public void restoreUsages(Player p)
                {
                    if (playersWhoCantUseRockets.contains(p) && !p.isFlying())
                    {
                        playersWhoCantUseRockets.remove(p);
                    }
                    super.restoreUsages(p);
                }
            }
        );

        for (int i = 0; i <= 200; i += 5)
        {
            colors.add(SmashManager.getParticleColor(0, i, 255));
        }

        addItem(new SmashItemWithCharge(Material.FEATHER, ChatColor.WHITE + "" + ChatColor.BOLD + "" + ChatColor.ITALIC + "Wings of Icarus", 0.0018F, 0.015F, false)
        {
            public void cancelTask(Player p)
            {
                super.cancelTask(p);
                if (!SmashWorldManager.isSmashWorld(p.getWorld()) && !SmashWorldManager.isInSpectatorMode(p))
                {
                    p.setAllowFlight(false);
                }
            }

            private void increaseCharge(Player p)
            {
                if (getCharge(p) + getChargeAmount() < 1)
                {
                    setCharge(p, getCharge(p) + getChargeAmount());
                }
                else
                {
                    setCharge(p, 1F);
                    cancelTask(p);
                }
            }

            @Override
            public void setExpToRemaining(Player p)
            {
                if (!Hammer.isWieldingHammer(p))
                {
                    super.setExpToRemaining(p);
                }
            }

            public void setCharge(Player p, float charge)
            {
                putPlayerCharge(p, charge);
                if (!isCharging(p) || isBeingHeld(p))
                {
                    setExpToRemaining(p);
                }
                if (charge == 1F)
                {
                    cancelTask(p);
                }
                else if (charge == 0F)
                {
                    setCharging(p, true);
                }
            }

            public void decreaseCharge(Player p)
            {
                if (getCharge(p) - getDischargeAmount() > 0)
                {
                    setCharge(p, getCharge(p) - getDischargeAmount());
                }
                else
                {
                    setCharge(p, 0F);
                    setCharging(p, true);
                    if (!SmashKitManager.canChangeKit(p))
                    {
                        p.setAllowFlight(false);
                    }
                }
            }

            public void activateTask(final Player p)
            {
                if (!playersWhoCantUseRockets.contains(p))
                {
                    playersWhoCantUseRockets.add(p);
                }
                p.setAllowFlight(true);
                p.setFlySpeed(0.12F); //0.059 0.61
                setCharging(p, false);
                if (!hasTaskActive(p))
                {
                    putTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable() {
                        int iteration = 0;
                        public void run()
                        {
                            if (!isCharging(p))
                            {
                                if (!Hammer.isWieldingHammer(p))
                                {
                                    playWing(p, iteration, colors);
                                }
                                iteration++;
                                decreaseCharge(p);
                            }
                            else
                            {
                                increaseCharge(p);
                            }
                        }
                    }, 0, 1));
                }
            }

            public void performDeselectAction(Player p)
            {
                p.setExp(0);
            }

            public void performRightClickAction(Player p)
            {
                if (getCharge(p) == 1 && canUseItem(p))
                {
                    activateTask(p);
                }
            }
        });

        addHiddenItem(new SmashItem(Material.ARROW, ChatColor.GRAY + "Arrow") {
            public void performRightClickAction(Player p) {
            }
        });
    }

    public static void playWing(Entity e, int iteration, List<ParticleEffect.OrdinaryColor> colors)
    {
        Vector facingVector = SmashManager.getVectorOfYaw(e);
        float backwardMod = 0.3F;
        float angle = 2F*(float)Math.PI*(iteration%30)/30;
        float angleMod1 = (float)Math.sin(angle);
        float angleMod2 = (float)Math.sin(angle+Math.PI/2);
        //Bukkit.broadcastMessage("" + tipMod);
        for (int i = 0; i < 40; i++)
        {
            float wingOffset = (r.nextFloat() - 0.5F)*2;
            wingOffset += 0.05F*SmashManager.getSign(wingOffset);
            float wingFactor = Math.abs(wingOffset);
            float furtherBackMod = (float)Math.pow((2.5F+angleMod2)/3*wingFactor, 2.3F);
            SmashManager.playBasicParticle(e.getLocation().add(-facingVector.getX()*(backwardMod+furtherBackMod) - facingVector.getZ()*wingOffset*2.5,
                    1.3F + (r.nextFloat() - 0.5F)*2*(1F-wingFactor)*0.5 + Math.pow(wingFactor, 2.3F)*((2.5+angleMod1)/3),
                    -facingVector.getZ()*(backwardMod+furtherBackMod) + facingVector.getX()*wingOffset*2.5), colors.get(r.nextInt(colors.size())), false);
        }
        if (iteration % 13 == 3)
        {
            e.getWorld().playSound(e.getLocation(), Sound.ENDERDRAGON_WINGS, 0.5F, 1.12F);
        }
        if (e instanceof Player && iteration % 18 == 0)
        {
            e.getWorld().playSound(e.getLocation(), Sound.LEVEL_UP, 0.1F, 2F);
        }
    }
}
