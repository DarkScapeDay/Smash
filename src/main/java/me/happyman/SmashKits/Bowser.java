package me.happyman.SmashKits;

import me.happyman.ItemTypes.GroundPound;
import me.happyman.ItemTypes.RocketItem;
import me.happyman.ItemTypes.SmashItemWithCharge;
import me.happyman.SmashKitMgt.*;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashAttackListener;
import me.happyman.utils.SmashEntityTracker;
import me.happyman.worlds.SmashWorldInteractor;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.util.Vector;

public class Bowser extends SmashKit
{
    public Bowser()
    {
        super(
            new SmashKitProperties(ChatColor.GREEN + "" + ChatColor.BOLD + "Browser", Material.RED_ROSE, ChatColor.DARK_GREEN + "" + ChatColor.ITALIC + "Roooar!",
                new FinalSmash(10, new Sound[] {Sound.BLAZE_DEATH}, new float[] {7}, new float[] {0.1F})
                {
                    @Override
                    protected void performFinalSmashAbility(final Player p)
                    {
                        final World w = p.getWorld();
                        SmashWorldInteractor.sendMessageToWorld(w, "<" + p.getDisplayName() + "> " + ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "ROOOOAAARRR!!!");
                        SmashAttackListener.setFinalAttackMod(p, 3F);
                        SmashAttackListener.setFinalIntakeMod(p, 0F);

                        int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable() {
                            int iteration = 0;
                            @Override
                            public void run() {
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
                                    SmashManager.playBasicParticle(loc1, SmashManager.getParticleColor(255, 0, 0), false);
                                    SmashManager.playBasicParticle(loc2, SmashManager.getParticleColor(0, 255, 0), false);
                                    SmashManager.playBasicParticle(loc3, SmashManager.getParticleColor(255, 0, 0), false);
                                    SmashManager.playBasicParticle(loc4, SmashManager.getParticleColor(0, 255, 0), false);
                                    iteration++;
                                }
                            }
                        }, 0, 1);
                        SmashManager.getPlugin().cancelTaskAfterDelay(task, getAbilityDurationSeconds()*20);
                    }

                    @Override
                    protected void endFinalSmashAbility(Player p)
                    {
                        SmashAttackListener.forgetFinalAttackMod(p);
                        SmashAttackListener.forgetFinalIntakeMod(p);
                    }
                },0.8F, 1.1F, true),
            new LeatherArmorSet(ChatColor.YELLOW + "" + ChatColor.BOLD + "Browser's Shell", new int[] {241, 72, 31}, new int[] {89, 149, 49}, new int[] {37, 160, 53}, new int[] {37, 160, 53}),
            new SmashItemWithCharge(Material.DIAMOND_SWORD, ChatColor.DARK_GREEN + "" + ChatColor.BOLD + "Sword", 0.01538F, 1F, false)
            {
                public void performRightClickAction(Player p)
                {
                    if (canUseItem(p))
                    {
                        activateTask(p);
                        setCharging(p, true);
                        decreaseCharge(p);
                        Vector v = p.getEyeLocation().getDirection();
                        Fireball e = (Fireball)p.getWorld().spawnEntity(SmashManager.getSafeProjLaunchLocation(p), EntityType.FIREBALL);

                        float vMod = 3.45F;
                        v.setX(v.getX() * vMod);
                        v.setY(v.getY() * vMod);
                        v.setZ(v.getZ() * vMod);
                        //Bukkit.getPlayer("HappyMan").sendMessage("setting v for browser's breath");
                        e.setVelocity(v);
                        p.getWorld().playSound(p.getLocation(), Sound.GHAST_FIREBALL, 1, 0.2F);
                        SmashEntityTracker.addCulprit(p, e, "Fire Breath", 25);
                    }
                }
            },
            new RocketItem(ChatColor.RED + "   " + ChatColor.BOLD + "Double-Jump", 2, 0.9248F) {},
            new GroundPound(Material.CLAY_BRICK, ChatColor.RED + "" + ChatColor.BOLD + "Ground Pound", 13,  7, Sound.ZOMBIE_WOOD, 1F) {}
            );
    }
}
