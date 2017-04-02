package me.happyman.SmashKits;

import me.happyman.ItemTypes.*;
import me.happyman.SmashKitMgt.*;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashAttackListener;
import me.happyman.utils.ParticleEffect;
import me.happyman.utils.SmashEntityTracker;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LightningStrike;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

public class Pikachu extends SmashKit
{



    public Pikachu()
    {
        super(
            new SmashKitProperties(ChatColor.YELLOW + "" + ChatColor.BOLD + "Pika", Material.BLAZE_ROD, ChatColor.YELLOW + "" + ChatColor.ITALIC + "PIIIIIIIKKKKAAAAAAAAAAAAAAAAAAAAAA!",
                new FinalSmash(10, new Sound[] {Sound.NOTE_BASS}, new float[] {2F}, new float[] {0.55F})
                {
                    private final float POWER = 11F/SmashAttackListener.DAMAGE_GAIN_FACTOR;
                    private final float PARICLE_NUM = 150;
                    private final Random r = new Random();
                    private final ParticleEffect.OrdinaryColor blueColor = SmashManager.getParticleColor(14, 123, 252);
                    private final ParticleEffect.OrdinaryColor yellowColor = SmashManager.getParticleColor(255, 255, 28);
                    private final float ABILITY_RADIUS = 3F;
                    private final float SPEED_BOOST = 3F;
                    private final float PARTICLE_OFFSET_MOD = 0.3F;
                    private final int HIT_COOLDOWN = 10;
                    private ArrayList<Player> speedAltered = new ArrayList<Player>();

                    @Override
                    protected void performFinalSmashAbility(final Player p)
                    {
                        SmashAttackListener.setArtificiallyShieldedPlayer(p);
                        p.setAllowFlight(true);
                        p.setFlying(true);
                        if (SmashEntityTracker.multiplySpeedFactor(p, SPEED_BOOST) && !speedAltered.contains(p))
                        {
                            speedAltered.add(p);
                        }

                        addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable() {
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

                                    SmashManager.playBasicParticle(SmashManager.getAbsFromRelLocFRU(p.getEyeLocation(),
                                            ABILITY_RADIUS*(float)Math.cos(pitch)*(float)Math.cos(yaw)*randomOffset,
                                            ABILITY_RADIUS*(float)Math.cos(pitch)*(float)Math.sin(yaw)*randomOffset,
                                            ABILITY_RADIUS*(float)Math.sin(pitch)*randomOffset, true), blueColor, true);
                                    SmashManager.playBasicParticle(SmashManager.getAbsFromRelLocFRU(p.getEyeLocation(),
                                            ABILITY_RADIUS/1.3F*(float)Math.cos(pitch)*(float)Math.cos(yaw)*randomOffset,
                                            ABILITY_RADIUS/1.3F*(float)Math.cos(pitch)*(float)Math.sin(yaw)*randomOffset,
                                            ABILITY_RADIUS/1.3F*(float)Math.sin(pitch)*randomOffset, true), yellowColor, true);
                                }

                                SmashAttackListener.attackPlayersInRange(p, "Volt Tackle", POWER, ABILITY_RADIUS, false);
                                iteration++;
                            }
                        }, 0, 1));
                    }

                    @Override
                    protected void endFinalSmashAbility(Player p)
                    {
                        SmashAttackListener.forgetArtificiallyShieldedPlayer(p);
                        p.setAllowFlight(false);
                        if (speedAltered.contains(p))
                        {
                            SmashEntityTracker.multiplySpeedFactor(p, 1F/SPEED_BOOST);
                            speedAltered.remove(p);
                        }
                    }
                }, 1.1F, 1.1F, true),
            new LeatherArmorSet(ChatColor.YELLOW + "" + ChatColor.BOLD + "Pika", 255, 255, 0),
            new SwordItemWithUsages(Material.GOLD_SWORD, ChatColor.YELLOW + "" + ChatColor.BOLD + "Sword", new Enchantment[] {Enchantment.DAMAGE_ALL, Enchantment.DURABILITY}, new int[] {2, 10})
            {
                public void performAction(Player p) {};
            },
            new RocketItem(ChatColor.YELLOW + "" + ChatColor.BOLD + "Double-Jump", 2, 1F)
            {
                @Override
                public boolean canUseItem(Player p)
                {
                    return super.canUseItem(p) && !SmashKitManager.hasFinalSmashActive(p);
                }
            }
        );
        addItem(
            new SmashItemWithCharge(Material.BLAZE_ROD, ChatColor.WHITE + "" + ChatColor.ITALIC + "" + ChatColor.BOLD + "Thunderbolt", 0.0068F, 1F, false)
            {
                @Override
                public void performRightClickAction(Player p)
                {
                    if (canUseItem(p))
                    {
                        World w = p.getWorld();
                        setCharge(p, 0);
                        activateTask(p);
                        Location l = p.getEyeLocation();
                        l.setX(l.getX() - 0.5);
                        l.setZ(l.getZ() - 0.5);
                        Block b = w.getHighestBlockAt(p.getLocation()).getRelative(0, -1, 0);
                        if (!b.getType().equals(Material.AIR))
                        {
                            l.setY(b.getY() + 1);
                        }
                        LightningStrike lightning = p.getWorld().strikeLightning(l);
                        SmashEntityTracker.addCulprit(p, lightning, "Thunderbolt", 13, true);
                    }
                }
            }
        );
    }
}
