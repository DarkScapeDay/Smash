package me.happyman.SmashKits;

import me.happyman.ItemTypes.RocketItem;
import me.happyman.ItemTypes.SmashItemWithUsages;
import me.happyman.SmashItems.NormalSword;
import me.happyman.SmashKitMgt.LeatherArmorSet;
import me.happyman.SmashKitMgt.SmashKit;
import me.happyman.SmashKitMgt.SmashKitManager;
import me.happyman.SmashKitMgt.SmashKitProperties;
import me.happyman.SpecialKitItems.SonicHoming;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashAttackListener;
import me.happyman.utils.ParticleEffect;
import me.happyman.utils.SmashEntityTracker;
import me.happyman.worlds.SmashWorldInteractor;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Callable;

public class Sonic extends SmashKit
{
    public Sonic()
    {
        super
        (
            new SmashKitProperties(ChatColor.AQUA + "" + ChatColor.BOLD + "Sonic", Material.INK_SACK, (short)4, ChatColor.BLUE + "" + ChatColor.ITALIC + "You're too slow!",
                new FinalSmash(15, null, null)
                {
                    private final float ATTACK_RANGE = 2F;
                    private final float ATTACK_POWER = 35F/SmashAttackListener.DAMAGE_GAIN_FACTOR;
                    private final ParticleEffect.OrdinaryColor yellowColor = SmashManager.getParticleColor(255, 255, 10);
                    private Random r = new Random();
                    private final int PARTICLE_COUNT = 100;
                    public static final float PARTICLE_RAD = 2F;

                    protected void performFinalSmashAbility(final Player p)
                    {
                        SmashAttackListener.setArtificiallyShieldedPlayer(p);
                        SmashWorldInteractor.allowFullflight(p, true);
                        SmashEntityTracker.setSpeedFactor(p, 80);
                        addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable() {
                            private HashMap<Player, Integer> tickHitTimes = new HashMap<Player, Integer>();
                            private int iteration = 0;
                            @Override
                            public void run() {
                                for (Player victim : p.getWorld().getPlayers())
                                {
                                    if (!(tickHitTimes.containsKey(victim) && iteration - tickHitTimes.get(victim) < 10) && !victim.equals(p) && !SmashWorldInteractor.isInSpectatorMode(victim) && victim.getLocation().distance(p.getLocation()) < ATTACK_RANGE)
                                    {
                                        tickHitTimes.put(victim, iteration);
                                        SmashAttackListener.attackPlayer(p, ChatColor.stripColor("Super " + SmashKitManager.getSelectedKit(p).getName()), p.getLocation(), victim, ATTACK_POWER, false);
                                    }
                                }
                                for (int i = 0; i < PARTICLE_COUNT; i++)
                                {
                                    float pitch = (float)(r.nextFloat()*Math.PI - Math.PI/2);
                                    float yaw = (float)(r.nextFloat()*2*Math.PI);
                                    SmashManager.playBasicParticle(
                                            SmashManager.getAbsFromRelLocFRU(p.getEyeLocation(),
                                                    (float)(Math.cos(pitch)*Math.cos(yaw)*PARTICLE_RAD),
                                                    (float)(Math.cos(pitch)*Math.sin(yaw)*PARTICLE_RAD),
                                                    (float)(Math.sin(pitch)*PARTICLE_RAD), false),
                                            yellowColor, true);
                                }
                                if (!p.isFlying())
                                {
                                    SmashWorldInteractor.allowFullflight(p, true);
                                }
                                iteration++;
                            }
                        }, 0, 1));
                    }

                    @Override
                    protected void endFinalSmashAbility(Player p)
                    {
                        SmashEntityTracker.resetSpeedAlteredPlayer(p);
                        SmashAttackListener.forgetArtificiallyShieldedPlayer(p);
                        p.setAllowFlight(false);
                    }
                }, 1, 1, false), new LeatherArmorSet(ChatColor.BLUE + "Sonic", new int[] {42, 82, 198}, new int[] {240, 219, 165}, new int[] {36, 95, 83}, new int[] {202, 42, 64}),
            new NormalSword(Material.IRON_SWORD),
            new RocketItem(ChatColor.BLUE + "" + ChatColor.BOLD + "Double-jump Rocket", 2, 1) {}
        );
        addItem(
            new SmashItemWithUsages(Material.IRON_INGOT, ChatColor.LIGHT_PURPLE + "" + ChatColor.BOLD + "Spring", 1, true)
            {
                @Override
                public void performAction(final Player p)
                {
                    addUsage(p);
                    Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
                        public String call()
                        {
                            p.setVelocity(new Vector(0, 3, 0));
                            return "";
                        }
                    });
                }
            }
        );
        addItem(new SonicHoming());
    }
}
