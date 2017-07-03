package me.happyman.SmashKits;

import me.happyman.ItemTypes.RocketItem;
import me.happyman.SmashItems.NormalSword;
import me.happyman.SmashKitMgt.*;
import me.happyman.SpecialKitItems.WarioCooldownFart;
import me.happyman.SpecialKitItems.WarioCrouchFart;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashAttackListener;
import me.happyman.utils.SmashEntityTracker;
import me.happyman.worlds.SmashWorldManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.util.Vector;

import java.util.HashMap;

public class Wario extends SmashKit implements Listener
{
    public Wario()
    {
        super(
            new SmashKitProperties(ChatColor.YELLOW + "" + ChatColor.BOLD + "Warly", Material.INK_SACK, (short) 11, ChatColor.DARK_AQUA + "" + ChatColor.ITALIC + "Wah!",
                new FinalSmash()
                {
                    private static final int HIT_COOLDOWN = 10;
                    private final float POWER_AT_CENTER = 100F/SmashAttackListener.DAMAGE_GAIN_FACTOR;

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
                                    SmashAttackListener.attackPlayer(p, "Nuclear Fart", p.getLocation(), victim, POWER_AT_CENTER*10/(10 + (float)victim.getLocation().distance(p.getLocation())), false);
                                }
                                centerLocation.getWorld().playEffect(explosionLocation, Effect.EXPLOSION_LARGE, 0, SmashWorldManager.SEARCH_DISTANCE);
                            }
                            playExplosionDisk(p, centerLocation, radius - 1);
                        }
                    }

                    @Override
                    protected void performFinalSmashAbility(final Player p)
                    {
                        SmashAttackListener.setArtificiallyShieldedPlayer(p);
                        SmashEntityTracker.setSpeedFactor(p, 0F);
                        SmashManager.preventJumping(p);
                        addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable() {
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
                                            p.getWorld().playSound(p.getLocation(), Sound.AMBIENCE_THUNDER, 1F, 1F);
                                        }
                                        for (float vert = 0; vert < height; vert += POLYGONAL_EXPLOSIONS_PER_BLOCK)
                                        {
                                            p.getWorld().playEffect(p.getLocation().clone().add(0, vert, 0), Effect.EXPLOSION_LARGE, 0, SmashWorldManager.SEARCH_DISTANCE);
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
                                        cancelTask(p);
                                    }
                                    iteration++;
                                }
                            }
                        }, 0, 1));
                    }

                    @Override
                    protected void endFinalSmashAbility(Player p)
                    {
                        SmashAttackListener.forgetArtificiallyShieldedPlayer(p);
                        SmashEntityTracker.resetSpeedAlteredPlayer(p);
                        SmashManager.resetJumpBoost(p);
                    }
                }, 1F, 1F, false),
            new LeatherArmorSet(ChatColor.YELLOW + "" + ChatColor.BOLD + "Wario's Outfit", new int[]{255, 235, 50}, new int[]{32, 134, 183}, new int[]{253, 128, 163}, new int[]{88, 80, 119}),
            new NormalSword(Material.IRON_SWORD),
            new RocketItem(ChatColor.YELLOW + "" + ChatColor.BOLD + "Double-Jump", 2, 0.98F) {}
        );
        addItem(new WarioCooldownFart(ChatColor.DARK_GRAY + "" + ChatColor.BOLD + "Fart Bomb", (short)120, 10F, 90F, 80F,
                0.01F, 1F, false, Sound.AMBIENCE_THUNDER, 0.9F, Effect.EXPLOSION_HUGE, 3F) {});
        addItem(new WarioCrouchFart(ChatColor.GRAY + "" + ChatColor.BOLD + "Silent But Deadly", (short)60, 10F, 90F, 160F,
                0.02F, 0.007F) {});
    }
}