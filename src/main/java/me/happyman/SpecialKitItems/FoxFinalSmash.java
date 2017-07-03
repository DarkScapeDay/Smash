package me.happyman.SpecialKitItems;

import me.happyman.ItemTypes.SmashItem;
import me.happyman.ItemTypes.SmashItemWithCharge;
import me.happyman.SmashKits.FinalSmash;
import me.happyman.commands.SmashManager;
import me.happyman.Listeners.SmashAttackListener;
import me.happyman.utils.ParticleEffect;
import me.happyman.utils.SmashEntityTracker;
import me.happyman.worlds.SmashWorldInteractor;
import me.happyman.worlds.SmashWorldManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;

public class FoxFinalSmash extends FinalSmash implements Listener
{
    private static final int TANK_SIZE = 3;
    private static final float SQUASH_POWER = 40;
    private final float downOffset = 1F;
    private final float hoverHeight = 2.4F + downOffset;
    private final float distanceBetweenParticles = 0.5F;
    private Vector whereToShootRelFUR = new Vector(4.5F, -downOffset, 0F);
    private HashMap<List<Vector>, Pariclly> whereToPutParticles = new HashMap<List<Vector>, Pariclly>();
    private HashMap<Player, List<Item>> shots = new HashMap<Player, List<Item>>();
    private final int shotLifeLingerSeconds = 2;
    private final float playerHitRange = 3;
    private final int firePower = 70;
    private Random r = new Random();

    private final SmashItemWithCharge firer = new SmashItemWithCharge(Material.FIREWORK_CHARGE, "Fire!", .1F, 1F, false)
    {
        @Override
        public void performRightClickAction(final Player p)
        {
            if (canUseItem(p))
            {
                activateTask(p);
                setCharging(p, true);
                decreaseCharge(p);
                Location whereToSpawnShot = SmashManager.getAbsFromRelLocFRU(getTurretLoc(p), whereToShootRelFUR, true);

                for (int i = 0; i < 2; i++)
                {
                    whereToSpawnShot.getWorld().playSound(whereToSpawnShot, Sound.FIREWORK_BLAST, 1, 0.2F);
                }

                whereToSpawnShot.getWorld().playEffect(whereToSpawnShot, Effect.FLAME,0, SmashWorldManager.SEARCH_DISTANCE);
                final Item itemToTrack = p.getWorld().dropItem(whereToSpawnShot, new ItemStack(Material.STONE));
                if (!shots.containsKey(p))
                {
                    shots.put(p, new ArrayList<Item>());
                }
                shots.get(p).add(itemToTrack);
                addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable()
                {
                    Vector currentV = getTurretLoc(p).getDirection().multiply(1.5F);

                    public void run()
                    {
                        if (!itemToTrack.isDead())
                        {
                            if (SmashAttackListener.attackPlayersInRange(itemToTrack, p, "Landmaster", firePower, playerHitRange, true, false, false)
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
        }
    };

    private class Pariclly
    {
        private ParticleEffect.OrdinaryColor color;
        private boolean useFlatDir;

        public Pariclly(int red, int green, int blue, boolean useFlatDir)
        {
            color = SmashManager.getParticleColor(red, green, blue);
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

    public FoxFinalSmash()
    {
        super(18, null, null);
        Bukkit.getPluginManager().registerEvents(this, SmashManager.getPlugin());
    }

    public SmashItem getFirer()
    {
        return firer;
    }

    @Override
    protected void performFinalSmashAbility(final Player p)
    {
        SmashAttackListener.setArtificiallyShieldedPlayer(p);
        firer.give(p);
        if (whereToPutParticles.size() == 0)
        {
                            /*
                            //Barrel
                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                                new Vector(7.5F, 0F, -1.75F),
                                new Vector(10.5F, 0F, -1.75F),
                                new Vector(7.5F, 0.375F, -1.75F), distanceBetweenParticles, true),
                                new Pariclly(235, 235, 245, false));
                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                                new Vector(7.5F, 0F, -0.75F),
                                new Vector(10.5F, 0F, -0.75F),
                                new Vector(7.5F, 0.375F, -0.75F), distanceBetweenParticles, true),
                                new Pariclly(235, 235, 245, false));
                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                                new Vector(7.5F, 0.375F, -0.75F),
                                new Vector(10.5F, 0.33F, -0.75F),
                                new Vector(7.5F, 0.375F, -1.75F), distanceBetweenParticles, true),
                                new Pariclly(235, 235, 245, false));
                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                                new Vector(10.5F, 0.33F, -0.75F),
                                new Vector(10.5F, 0, -0.75F),
                                new Vector(10.5F, 0.33F, -1.75F), distanceBetweenParticles, true),
                                new Pariclly(235, 235, 245, false));
                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                                new Vector(2.5F, 0.25F, -1F),
                                new Vector(7.5F, 0.25F, -1F),
                                new Vector(2.5F, 0.25F, -1.5F), distanceBetweenParticles, true),
                                new Pariclly(95, 94, 99, false));
                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                                new Vector(2.5F, 0F, -1F),
                                new Vector(7.5F, 0, -1F),
                                new Vector(2.5F, 0.25F, -1F), distanceBetweenParticles, true),
                                new Pariclly(95, 94, 99, false));
                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                                new Vector(2.5F, 0F, -1.5F),
                                new Vector(7.5F, 0, -1.5F),
                                new Vector(2.5F, 0.25F, -1.5F), distanceBetweenParticles, true),
                                new Pariclly(95, 94, 99, false));
                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                                new Vector(1F, 0.5F, -0.75F),
                                new Vector(2.5F, 0.4F, -0.75F),
                                new Vector(1F, 0.5F, -1.75F), distanceBetweenParticles, true),
                                new Pariclly(241, 241, 249, false));
                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                                new Vector(1F, 0.5F, -1.75F),
                                new Vector(2.5F, 0.4F, -1.75F),
                                new Vector(1F, 0, -1.75F), distanceBetweenParticles, true),
                                new Pariclly(83, 84, 88, false));
                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                                new Vector(1F, 0.5F, -0.75F),
                                new Vector(2.5F, 0.4F, -0.75F),
                                new Vector(1, 0, -0.75F), distanceBetweenParticles, true),
                                new Pariclly(193, 191, 202, false));

                            //Beside barrel mount side
                            whereToPutParticles.put(SmashManager.getParticleTriangleFRUVectors(
                                new Vector(1F, 0.5F, -0.4F),
                                new Vector(2F, 0.4F, -3.4F),
                                new Vector(1.7F, 1.0F, -2.5F), distanceBetweenParticles, true),
                                new Pariclly(254, 255, 250, true));
                            //Above barrel mount front
                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                                new Vector(1F, 0, -0.4F),
                                new Vector(1.1167F, 0, -0.75F),
                                new Vector(1F, 0.5F, -0.4F), distanceBetweenParticles, true),
                                new Pariclly(254, 255, 250, true));
                            //Below barrel mount front
                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                                new Vector(1.72F, 0, -2.56F),
                                new Vector(2F, 0, -3.4F),
                                new Vector(1.72F, 0.5F, -2.56F), distanceBetweenParticles, true),
                                new Pariclly(228, 228, 238, true));
                            //Below barrel side mount supports tops
                            whereToPutParticles.put(SmashManager.getParticleTriangleFRUVectors(
                                    new Vector(2F, 0.4F, -3.4F),
                                    new Vector(1.7F, 1.0F, -2.5F),
                                    new Vector(1.85F, 1.25F, -2.95F), distanceBetweenParticles, true),
                                    new Pariclly(228, 228, 238, true));
                            //Below barrel side mount supports mains
                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                                    new Vector(1.85F, 1.25F, -2.95F),
                                    new Vector(2F, 0.4F, -3.4F),
                                    new Vector(2.18333, 1.5F, -3.95F), distanceBetweenParticles, true),
                                    new Pariclly(220, 219, 233, true));
                            //Below barrel middle gray area
                            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                                    new Vector(2F, 0.4F, -3.4F),
                                    new Vector(2.3333F, 0.65F, -4.4F),
                                    new Vector(2F, -0.25, -3.4F), distanceBetweenParticles, true),
                                    new Pariclly(108, 109, 114, true));
                            //The long parts below those white support things pointing down
                            whereToPutParticles.put(SmashManager.getParticleTriangleFRUVectors(
                                    new Vector(2.3333F, 0.65F, -4.4F),
                                    new Vector(2.18333, 1.5F, -3.95F),
                                    new Vector(2.5, 1.5F, -5.4F), distanceBetweenParticles, true),
                                    new Pariclly(220, 0, 233, true));
                            */

            //barrel_tip
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(5.65F, 0.6F, 0.15F),
                    new Vector(5.65F, 0.6F, 0F),
                    new Vector(5.65F, 0.3F, 0.15F), distanceBetweenParticles,true),
                    new Pariclly(112, 116, 128, false));
            //barrel_top
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(5.65F, 0.6F, 0F),
                    new Vector(5.65F, 0.6F, 0.15F),
                    new Vector(0.9F, 0.6F, 0F), distanceBetweenParticles,true),
                    new Pariclly(94, 95, 99, false));
            //barrel_side
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(5.65F, 0.3F, 0.15F),
                    new Vector(5.65F, 0.6F, 0.15F),
                    new Vector(0.9F, 0.3F, 0.15F), distanceBetweenParticles,true),
                    new Pariclly(82, 82, 84, false));
            //barrel_bottom
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(5.65F, 0.3F, 0F),
                    new Vector(5.65F, 0.3F, 0.15F),
                    new Vector(0.9F, 0.3F, 0F), distanceBetweenParticles,true),
                    new Pariclly(56, 56, 58, false));
            //turret_front_t
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(0.9F, .8F, 0F),
                    new Vector(0.958F, .7F, 0F),
                    new Vector(.9F, .8F, .25F), distanceBetweenParticles,true),
                    new Pariclly(231, 232, 252, false));
            //turret_front
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(.9F, .8F, .25F),
                    new Vector(1.362F, 0F, .25F),
                    new Vector(.9F, .8F, 1F), distanceBetweenParticles,true),
                    new Pariclly(255, 255, 255, false));
            //turret_front_h
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(1.246F, .2F, 0F),
                    new Vector(1.362F, 0F, 0F),
                    new Vector(1.246F, .2F, .25F), distanceBetweenParticles,true),
                    new Pariclly(224, 224, 236, false));
            //turret_top
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(.511F, .8F, 0F),
                    new Vector(.9F, .8F, 0F),
                    new Vector(.511F, .8F, 1F), distanceBetweenParticles,true),
                    new Pariclly(251, 255, 255, false));
            //turret_side
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(.9F, .8F, 1F),
                    new Vector(.511F, .8F, 1F),
                    new Vector(.9F, 0F, 1F), distanceBetweenParticles,true),
                    new Pariclly(230, 230, 242, false));
            //turret_side_front
            whereToPutParticles.put(SmashManager.getParticleTriangleFRUVectors(
                    new Vector(.9F, 0F, 1F),
                    new Vector(.9F, .8F, 1F),
                    new Vector(1.362F, 0F, 1F), distanceBetweenParticles,true),
                    new Pariclly(230, 228,239, false));
            //turret_side_below
            whereToPutParticles.put(SmashManager.getParticleTriangleFRUVectors(
                    new Vector(1.362F, 0F, 1F),
                    new Vector(.9F, 0F, 1F),
                    new Vector(1.362F, -.142F, 1F), distanceBetweenParticles,true),
                    new Pariclly(206, 206, 216, false));
            //turret_front_d
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(1.362F, -.142F, 0F),
                    new Vector(1.362F, 0F, 0F),
                    new Vector(1.362F, -.142F, 1F), distanceBetweenParticles,true),
                    new Pariclly(117, 123, 135, false));
            //top
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(.9F, 0F, 1F),
                    new Vector(.9F, 0F, 2F),
                    new Vector(-.9F, 0F, 1F), distanceBetweenParticles,true),
                    new Pariclly(213, 215, 227, true));
            //top_h
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(-.5F, 0F, 0F),
                    new Vector(-.5F, 0F, 1F),
                    new Vector(-.9F, 0F, 0F), distanceBetweenParticles,true),
                    new Pariclly(213, 215, 227, true));
            //top_back
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(-.9F, 0F, 2F),
                    new Vector(-.9F, 0F, 0F),
                    new Vector(-3.5F, -.8F, 2F), distanceBetweenParticles,true),
                    new Pariclly(238, 243, 255, true));
            //front_top
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(.9F, 0F, 1F),
                    new Vector(3.5F, -.8F, 1F),
                    new Vector(.9F, 0F, 2F), distanceBetweenParticles,true),
                    new Pariclly(84, 142, 242, true));
            //front_top_l
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(3.5F, -.8F, 0F),
                    new Vector(3.5F, -.8F, 1F),
                    new Vector(1.362F, -.142F, 0F), distanceBetweenParticles,true),
                    new Pariclly(89, 91, 112, true));
            //front
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(3.5F, -1.8F, 0F),
                    new Vector(3.5F, -.8F, 0F),
                    new Vector(3.5F, -1.8F, 2F), distanceBetweenParticles,true),
                    new Pariclly(226, 226, 238, true));
            //side_front_t
            whereToPutParticles.put(SmashManager.getParticleTriangleFRUVectors(
                    new Vector(.9F, -.8F, 2F),
                    new Vector(.9F, 0F, 2F),
                    new Vector(3.5F, -.8F, 2F), distanceBetweenParticles,true),
                    new Pariclly(251, 197, 26, true));
            //side_front_d
            whereToPutParticles.put(SmashManager.getParticleTriangleFRUVectors(
                    new Vector(.9F, -1.8F, 2F),
                    new Vector(3.5F, -1.8F, 2F),
                    new Vector(.9F, -2.6F, 2F), distanceBetweenParticles,true),
                    new Pariclly(54, 52, 57, true));
            //side_t
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(.9F, 0F, 2F),
                    new Vector(.9F, -.8F, 2F),
                    new Vector(-.9F, 0F, 2F), distanceBetweenParticles,true),
                    new Pariclly(182, 28, 38, true));
            //side_d
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(.9F, -1.8F, 2F),
                    new Vector(-.9F, -1.8F, 2F),
                    new Vector(.9F, -2.6F, 2F), distanceBetweenParticles,true),
                    new Pariclly(104, 105, 110, true));
            //side_back_t
            whereToPutParticles.put(SmashManager.getParticleTriangleFRUVectors(
                    new Vector(-.9F, -.8F, 2F),
                    new Vector(-.9F, 0, 2F),
                    new Vector(-3.5F, -.8F, 2F), distanceBetweenParticles,true),
                    new Pariclly(63, 62, 70, true));
            //side_back_d
            whereToPutParticles.put(SmashManager.getParticleTriangleFRUVectors(
                    new Vector(-.9F, -1.8F, 2F),
                    new Vector(-3.5F, -1.8F, 2F),
                    new Vector(-.9F, -2.6F, 2F), distanceBetweenParticles,true),
                    new Pariclly(63, 62, 70, true));
            //side
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(3.5F, -.8F, 2F),
                    new Vector(3.5F, -1.8F, 2F),
                    new Vector(-3.5F, -.8F, 2F), distanceBetweenParticles,true),
                    new Pariclly(144, 145, 150, true));
            //back
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(-3.5F, -.8F, 0F),
                    new Vector(-3.5F, -.8F, 2F),
                    new Vector(-3.5F, -1.8F, 0F), distanceBetweenParticles,true),
                    new Pariclly(183, 183, 195, true));
            //back_d
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(-.9F, -2.6F, 0F),
                    new Vector(-3.5F, -1.8F, 0F),
                    new Vector(-.9F, -2.6F, 2F), distanceBetweenParticles,true),
                    new Pariclly(154, 153, 161, true));
            //front_bottom
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(.9F, -2.6F, 0F),
                    new Vector(3.5F, -1.8F, 0F),
                    new Vector(.9F, -2.6F, 2F), distanceBetweenParticles,true),
                    new Pariclly(139, 138, 144, true));
            //tire_front
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(2.571F, -2.193F, 2F),
                    new Vector(2.571F, -2.607F, 2F),
                    new Vector(2.571F, -2.193F, 2.75F), distanceBetweenParticles,true),
                    new Pariclly(38, 20, 18, true));
            //tire_front_t
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(2.279F, -1.9F, 2F),
                    new Vector(2.571F, -2.193F, 2F),
                    new Vector(2.279F, -1.9F, 2.75F), distanceBetweenParticles,true),
                    new Pariclly(249, 250, 254, true));
            //tire_front_d
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(2.279F, -2.9F, 2F),
                    new Vector(2.571F, -2.607F, 2F),
                    new Vector(2.279F, -2.9F, 2.75F), distanceBetweenParticles,true),
                    new Pariclly(36, 34, 35, true));
            //tire_right_front
            whereToPutParticles.put(SmashManager.getParticleTriangleFRUVectors(
                    new Vector(2.571F, -2.193F, 2.75F),
                    new Vector(2.279F, -1.9F, 2.75F),
                    new Vector(2.279F, -2.193F, 2.75F), distanceBetweenParticles,true),
                    new Pariclly(224, 224, 236, true));
            //tire_right_top
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(2.279F, -1.9F, 2.75F),
                    new Vector(2.279F, -2.193F, 2.75F),
                    new Vector(-3.136F, -1.9F, 2.75F), distanceBetweenParticles,true),
                    new Pariclly(228, 227, 233, true));
            //tire_right_back
            whereToPutParticles.put(SmashManager.getParticleTriangleFRUVectors(
                    new Vector(-3.429F, -2.193F, 2.75F),
                    new Vector(-3.136F, -2.193F, 2.75F),
                    new Vector(-3.136F, -1.9F, 2.75F), distanceBetweenParticles,true),
                    new Pariclly(195, 195, 203, true));
            //tire_right_mid
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(-3.429F, -2.193F, 2.75F),
                    new Vector(-3.429F, -2.607F, 2.75F),
                    new Vector(2.571F, -2.193F, 2.75F), distanceBetweenParticles,true),
                    new Pariclly(65, 56, 49, true));
            //tire_right_back_d
            whereToPutParticles.put(SmashManager.getParticleTriangleFRUVectors(
                    new Vector(-3.429F, -2.607F, 2.75F),
                    new Vector(-3.136F, -2.607F, 2.75F),
                    new Vector(-3.136F, -2.9F, 2.75F), distanceBetweenParticles,true),
                    new Pariclly(53, 49, 46, true));
            //tire_right_bottom
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(-3.136F, -2.9F, 2.75F),
                    new Vector(-3.136F, -2.607F, 2.75F),
                    new Vector(2.279F, -2.9F, 2.75F), distanceBetweenParticles,true),
                    new Pariclly(62, 51, 45, true));
            //tire_right_front_d
            whereToPutParticles.put(SmashManager.getParticleTriangleFRUVectors(
                    new Vector(2.279F, -2.607F, 2.75F),
                    new Vector(2.571F, -2.607F, 2.75F),
                    new Vector(2.279F, -2.9F, 2.75F), distanceBetweenParticles,true),
                    new Pariclly(50, 41, 44, true));
            //tire_back_t
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(-3.136F, -1.9F, 2F),
                    new Vector(-3.136F, -1.9F, 2.75F),
                    new Vector(-3.429F, -2.193F, 2F), distanceBetweenParticles,true),
                    new Pariclly(249, 250, 254, true));
            //tire_back
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(-3.429F, -2.193F, 2.75F),
                    new Vector(-3.429F, -2.193F, 2F),
                    new Vector(-3.429F, -2.607F, 2.75F), distanceBetweenParticles,true),
                    new Pariclly(38, 20, 18, true));
            //tire_back_d
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(-3.429, -2.607F, 2F),
                    new Vector(-3.429, -2.607F, 2.75F),
                    new Vector(-3.136F, -2.9F, 2F), distanceBetweenParticles,true),
                    new Pariclly(36, 34, 35, true));
            //tire_bottom
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(2.279F, -2.9F, 2F),
                    new Vector(2.279F, -2.9F, 2.75F),
                    new Vector(-3.136F, -2.9F, 2F), distanceBetweenParticles,true),
                    new Pariclly(32, 31, 29, true));
            //tire_top
            whereToPutParticles.put(SmashManager.getParticleParallelogramFRUVectors(
                    new Vector(2.279F, -1.9F, 2F),
                    new Vector(2.279F, -1.9F, 2.75F),
                    new Vector(-3.136F, -1.9F, 2F), distanceBetweenParticles,true),
                    new Pariclly(245, 245, 253, true));
        }

        SmashWorldInteractor.allowFullflight(p, true);
        p.setFlySpeed(p.getFlySpeed()*10F);

        World w = p.getWorld();
        SmashWorldInteractor.sendMessageToWorld(w, "<" + p.getDisplayName() + "> " + ChatColor.GOLD + "" + ChatColor.BOLD + "Landmaster!");

        addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable() {
            public static final int HIT_COOLDOWN = 10;
            private HashMap<Player, Integer> tickHitTimes = new HashMap<Player, Integer>();
            int iteration = 0;
            public void run()
            {
                //Bukkit.broadcastMessage("" + SmashManager.getRelFromAbsLoc(new Location(p.getWorld(), 0, 0, 0).setDirection(p.getLocation().getDirection()), new Vector(1, 0, 0), false).toVector().toString());
                //Bukkit.broadcastMessage("" + SmashManager.getAbsFromRelLocFRU(new Location(p.getWorld(), 0, 0, 0).setDirection(p.getLocation().getDirection()), new Vector(1, 0, 0), false).toVector().toString());
                Block b = null;

                for (Entity e : p.getNearbyEntities(TANK_SIZE, hoverHeight, TANK_SIZE))
                {
                    if (e instanceof Player && (!tickHitTimes.containsKey(e) || iteration - tickHitTimes.get(e) >= HIT_COOLDOWN) && !e.equals(p) && !SmashWorldInteractor.isInSpectatorMode((Player)e) && p.getLocation().getY() - e.getLocation().getY() > 3)
                    {
                        tickHitTimes.put((Player)e, iteration);
                        Location attackLoc = p.getLocation().clone();
                        attackLoc.setY(attackLoc.getY() - 2);
                        SmashAttackListener.attackPlayer(p, "Landmaster", attackLoc, (Player)e, SQUASH_POWER, false);
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
                    Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
                        public String call()
                        {
                            //Bukkit.getPlayer("HappyMan").sendMessage("setting velocity from fox's final Smash");
                            p.setVelocity(new Vector(0, (float)(0.1F*(bb.getLocation().getY() + hoverHeight - p.getLocation().getY())), 0));
                            return "";
                        }
                    });
                }
                if (iteration % 12 == 0)
                {
                    p.getWorld().playSound(p.getLocation(), Sound.ZOMBIE_REMEDY, 0.7F,  (r.nextFloat()-0.5F)*0.04F + 0.2F);
                }

                for (List<Vector> vectorLists : whereToPutParticles.keySet())
                {
                    boolean useFlatDir = whereToPutParticles.get(vectorLists).isUseFlatDir();
                    ParticleEffect.OrdinaryColor color = whereToPutParticles.get(vectorLists).getColor();
                    for (Vector v : vectorLists)
                    {
                        Location baseLoc = getTurretLoc(p, useFlatDir);
                        SmashManager.playBasicParticle(
                                SmashManager.getAbsFromRelLocFRU(baseLoc, (float)v.getX(), (float)v.getY(), (float)v.getZ() - downOffset, useFlatDir),
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
        item.getWorld().playEffect(item.getLocation(), Effect.EXPLOSION_LARGE, 0, SmashWorldManager.SEARCH_DISTANCE);
        item.getWorld().playSound(item.getLocation(), Sound.EXPLODE, 1F, 1F);
        item.remove();
    }

    @Override
    protected void endFinalSmashAbility(final Player p)
    {
        SmashAttackListener.forgetArtificiallyShieldedPlayer(p);
        p.setFlySpeed(p.getFlySpeed()*0.1F);
        if (!SmashWorldInteractor.isInSpectatorMode(p) && !(SmashWorldManager.isSmashWorld(p.getWorld()) && (!SmashWorldManager.gameHasStarted(p.getWorld())) || SmashWorldManager.gameHasEnded(p.getWorld())))
        {
            p.setAllowFlight(false);
        }
        firer.remove(p);
        if (shots.containsKey(p))
        {
            for (Item item : shots.get(p))
            {
                if (item.isOnGround())
                {
                    explodeShell(item);
                }
            }
            Bukkit.getScheduler().scheduleSyncDelayedTask(SmashManager.getPlugin(), new Runnable() {
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
}
