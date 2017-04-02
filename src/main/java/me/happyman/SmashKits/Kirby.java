package me.happyman.SmashKits;

import me.happyman.ItemTypes.*;
import me.happyman.SmashKitMgt.*;
import me.happyman.SpecialKitItems.KirbyInhaler;
import me.happyman.SpecialKitItems.KirbyRockTransformer;
import me.happyman.SpecialKitItems.KirbySword;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashAttackListener;
import me.happyman.utils.ParticleEffect;
import me.happyman.utils.SmashEntityTracker;
import me.happyman.utils.SmashWorldManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Random;

public class Kirby extends SmashKit
{
    private final KirbyInhaler inhaler;
    private final SmashItem reverser = new SmashItem(Material.REDSTONE_BLOCK, ChatColor.LIGHT_PURPLE + "Back to " + getName())
    {
        public void performRightClickAction(Player p)
        {
            KirbyInhaler.resetToKirby(p, false);
            p.playSound(p.getLocation(), Sound.ZOMBIE_REMEDY, 1F, 1F);
        }
    };

    public Kirby()
    {
        super(
            new SmashKitProperties(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Murphy", Material.INK_SACK, (short) 9, ChatColor.LIGHT_PURPLE + "" + ChatColor.ITALIC + "Hiii!",
                new FinalSmash(10, new Sound[] {Sound.LAVA_POP}, new float[] {2F}) {

                    private static final int MAX_DELAY_TO_START_MOVING = 20;
                    private static final int HOW_LONG_IT_TAKES_TO_MOVE_IN = 30;
                    private static final int HOW_LONG_TO_COOK = 30;
                    private static final int range = 70;
                    private static final int damagePercent = 34;
                    private static final int PARTICLE_COUNT = 100;
                    private final ParticleEffect.OrdinaryColor bowlColor = SmashManager.getParticleColor(150, 0, 0);
                    private final ParticleEffect.OrdinaryColor hatColor = SmashManager.getParticleColor(255, 255, 255);
                    private final float hatHeight = 1;
                    private final Vector zero = new Vector().zero();
                    private final Random r = new Random();

                    @Override
                    protected void performFinalSmashAbility(final Player p) {

                        addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable() {
                            Vector flatFacing = SmashManager.getVectorOfYaw(p);
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
                                            if (!victim.equals(p) && !SmashWorldManager.isInSpectatorMode(victim) && victim.getLocation().distance(p.getLocation()) < range)
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
                                            SmashManager.playBasicParticle(new Location(p.getWorld(),
                                                    potLocation.getX() + Math.cos(randomRad)/2,
                                                    potLocation.getY() + r.nextFloat(),
                                                    potLocation.getZ() + Math.sin(randomRad)/2), bowlColor, false);
                                        }
                                        else if ((float)i / PARTICLE_COUNT < 0.65)
                                        {
                                            SmashManager.playBasicParticle(new Location(p.getWorld(),
                                                    potLocation.getX() + Math.sin(randomRad)/2*r.nextFloat(),
                                                    potLocation.getY(),
                                                    potLocation.getZ() + Math.cos(randomRad)/2*r.nextFloat()), bowlColor, false);
                                        }
                                        else if ((float)i / PARTICLE_COUNT < 0.9)
                                        {
                                            SmashManager.playBasicParticle(SmashManager.getAbsFromRelLocFRU(hatBaseLocation,
                                                    (float)Math.cos(randomRad)/4.5F,
                                                    (float)Math.sin(randomRad)/4.5F,
                                                    r.nextFloat()*hatHeight*0.7F, false), hatColor, false);
                                        }
                                        else
                                        {
                                            SmashManager.playBasicParticle(SmashManager.getAbsFromRelLocFRU(hatBaseLocation,
                                                    (float)Math.cos(randomRad)/2.5F,
                                                    (float)Math.sin(randomRad)/2.5F,
                                                    hatHeight*0.3F*r.nextFloat() + 0.7F, false), hatColor, false);

                                        }
                                    }
                                    if (iteration % (r.nextInt(3) + 3) == 0)
                                    {
                                        potLocation.getWorld().playEffect(potLocation.clone().add(0, 1, 0), Effect.LAVA_POP, 0, SmashWorldManager.SEARCH_DISTANCE);
                                        playSmashSound(potLocation);
                                    }

                                    //Bukkit.getPlayer("HappyMan").sendMessage("setting " + p.getName() + "'s Velocity to 0");
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
                                                SmashAttackListener.sendEntityTowardLocation(new Location(p.getWorld(),
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
                                                SmashAttackListener.sendEntityTowardLocation(potLocation, victim, .2F, false);
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
                                                SmashAttackListener.attackPlayer(p, "Cook Murphy", potDmgLoc, victim, damagePercent /SmashAttackListener.DAMAGE_GAIN_FACTOR, false);
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
            new KirbySword(),
            new RocketItem(ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Jump", 5, 0.757F) {}
        );
        inhaler = new KirbyInhaler(this);
        addItem(inhaler);
        addItem(new KirbyRockTransformer());
        addSecretItem(reverser);
    }

    public KirbyInhaler getInhaler()
    {
        return inhaler;
    }

    public SmashItem getReverser()
    {
        return reverser;
    }
}
