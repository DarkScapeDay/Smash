package me.happyman.utils;

import me.happyman.Plugin;
import me.happyman.SpecialItems.SmashItemDrops.SmashOrbTracker;
import me.happyman.SpecialItems.SmashKitMgt.SmashKitManager;
import me.happyman.SpecialItems.SpecialItem;
import me.happyman.worlds.SmashWorldManager;
import me.happyman.worlds.WorldManager;
import me.happyman.worlds.WorldType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Callable;

import static me.happyman.Plugin.getPlugin;
import static me.happyman.Plugin.sendErrorMessage;
import static me.happyman.SpecialItems.SmashKitMgt.SmashKitManager.getRemainingShield;
import static me.happyman.SpecialItems.SmashKitMgt.SmashKitManager.isShielded;
import static me.happyman.worlds.WorldType.getWorldType;

public class SmashAttackManager
{
    private static final float KNOCKBACK_MODIFIER = 0.0135F;//(float)0.028;
    private static final float KBITERATION_MOD = 3F;
    private static final float KB_TIMES_DAMAGE_EXPONENT_MOD = (float)0.9;
    private static final float POWER_EXPONENT_MOD = 0.4F;
    private static final HashMap<Player, Float> actualDamages  = new HashMap<Player, Float>();;

    private static final float DAMAGE_GAIN_FACTOR = 0.45F;

    public static final float SWORD_BUFF = 1.2F;
    //private static HashMap<Player, ArrayList<Integer>> kbTasks;

    private static final ArrayList<Player> artificiallyShieldedPlayers = new ArrayList<Player>();
    private static final HashMap<Player, Float> finalAttackMods = new HashMap<Player, Float>();
    private static final HashMap<Player, Float> finalIntakeMods = new HashMap<Player, Float>();
    private static int HIT_COOLDOWN_MS = 300;
    private static final boolean PHYSICALLY_HIT_DEFAULT = true;

    private static float getFinalAttackMod(Player p)
    {
        return getMod(finalAttackMods, p);
    }

    public static void forgetFinalAttackMod(Player p)
    {
        forgetMod(finalAttackMods, p);
    }

    public static void setFinalAttackMod(Player p, float mod)
    {
        setMod(finalAttackMods, p, mod);
    }

    private static float getFinalIntakeMod(Player p)
    {
        return getMod(finalIntakeMods, p);
    }

    public static void forgetFinalIntakeMod(Player p)
    {
        forgetMod(finalIntakeMods, p);
    }

    public static void setFinalIntakeMod(Player p, float mod)
    {
        setMod(finalIntakeMods, p, mod);
    }

    private static float getMod(HashMap<Player, Float> finalMods, Player p)
    {
        if (!finalMods.containsKey(p))
        {
            return 1F;
        }
        return finalMods.get(p);
    }

    private static void forgetMod(HashMap<Player, Float> finalMods, Player p)
    {
        if (finalMods.containsKey(p))
        {
            finalMods.remove(p);
        }
    }

    private static void setMod(HashMap<Player, Float> finalMods, Player p, float mod)
    {
        finalMods.put(p, mod);
    }

    public static void attackPlayersInAngleRange(Player p, SpecialItem attackingItem, float maxPower, float attackRange, float hAngleDegrees, float vAngleDegrees)
    {
        attackPlayersInAngleRange(p, attackingItem, maxPower, attackRange, hAngleDegrees, vAngleDegrees, PHYSICALLY_HIT_DEFAULT);
    }

    public static void attackPlayersInAngleRange(Player p, SpecialItem attackingItem, float maxPower, float attackRange, float hAngleDegrees, float vAngleDegrees, boolean physicallyHit)
    {
        attackPlayersInAngleRange(p, attackingItem, maxPower, attackRange, hAngleDegrees, vAngleDegrees, physicallyHit, false);
    }

    public static void attackPlayersInAngleRange(Player p, SpecialItem attackingItem, float maxPower, float attackRange, float hAngleDegrees, float vAngleDegrees, boolean physicallyHit, boolean ignoreDistance)
    {
        attackPlayersInAngleRange(p, attackingItem, maxPower, attackRange, hAngleDegrees, vAngleDegrees, physicallyHit, ignoreDistance, false);
    }

    public static void attackPlayersInAngleRange(Player p, SpecialItem attackingItem, float maxPower, float attackRange, float hAngleDegrees, float vAngleDegrees, boolean ignoreDistance, boolean physicallyHit, boolean attackBackward)
    {
        attackPlayersInAngleRange(p, attackingItem.getItemStack().getItemMeta().getDisplayName(), maxPower, attackRange, hAngleDegrees, vAngleDegrees, physicallyHit, ignoreDistance, attackBackward);
    }

    public static void attackPlayersInAngleRange(Player p, String itemName, float maxPower, float attackRange, float hAngleDegrees, float vAngleDegrees, boolean physicallyHit, boolean ignoreDistance, boolean attackBackward)
    {
        if (SmashOrbTracker.isLookingAtSmashOrb(p))
        {
            SmashOrbTracker.hitSmashOrb(p);
        }
        else
        {
            for (Entity potVictim : p.getNearbyEntities(attackRange, attackRange, attackRange))
            {
                if (potVictim instanceof Player && !((Player)potVictim).equals((Player)p))
                {
                    float distance = (float) potVictim.getLocation().distance(p.getLocation());
                    if (distance <= attackRange)
                    {
                        org.bukkit.util.Vector v = SmashManager.getUnitDirection(p.getLocation(), potVictim.getLocation());
                        org.bukkit.util.Vector hV = v.clone();
                        hV.setY(0);
                        org.bukkit.util.Vector vV = v.clone();
                        vV.setX(p.getLocation().getDirection().getX());
                        vV.setZ(p.getLocation().getDirection().getZ());

                        float scale = (float)((1-vV.getY())/(Math.sqrt(vV.getX()*vV.getX() + vV.getZ()*vV.getZ())));
                        vV.setX(scale*vV.getX());
                        vV.setZ(scale*vV.getZ());

                        org.bukkit.util.Vector forwardFace = p.getLocation().getDirection();
                        float hAngle = SmashManager.getAngle(hV, forwardFace)*180/(float)Math.PI;
                        float vAngle = SmashManager.getAngle(vV, forwardFace)*180/(float)Math.PI;
                        if (attackBackward)
                        {
                            hAngle = 180 - hAngle;
                        }
                        float power = maxPower;
                        if (!ignoreDistance)
                        {
                            power *= (attackRange - distance)*(hAngleDegrees - hAngle)/(attackRange*hAngleDegrees);
                        }
                        if (hAngle <= hAngleDegrees && vAngle <= vAngleDegrees)
                        {
                            attackPlayer(p, itemName, p.getLocation(), (Player)potVictim, power, physicallyHit);
                        }
                    }
                }
                if (SmashOrbTracker.hasSmashOrb(p.getWorld()) && SmashOrbTracker.getSmashOrb(p.getWorld()).getLocation().distance(p.getLocation()) < attackRange)
                {
                    SmashOrbTracker.hitSmashOrb(p);
                }
            }
        }
    }

    //True if successfully attacked someone
    public static boolean attackPlayersInRange(Entity entityForLocation, Player p, String weaponUsed, float maxPower, float attackRange, boolean ignoreDistance, boolean projectile, boolean physicalHit)
    {
        boolean foundAPlayer = false;
        for (Entity potVictim : entityForLocation.getNearbyEntities(attackRange, attackRange, attackRange))
        {
            if (potVictim instanceof Player && !potVictim.equals(p))
            {
                float distance = (float) potVictim.getLocation().distance(entityForLocation.getLocation());
                if (distance < attackRange)
                {
                    float actualPower = maxPower;
                    if (!ignoreDistance)
                    {
                        actualPower *= (attackRange - distance)/attackRange;
                    }
                    attackPlayer(p, weaponUsed, entityForLocation.getLocation(), (Player)potVictim, actualPower, physicalHit);
                    foundAPlayer = true;
                }
            }
        }
        if (p == entityForLocation && SmashOrbTracker.hasSmashOrb(p.getWorld()) && SmashOrbTracker.getSmashOrb(p.getWorld()).getLocation().distance(entityForLocation.getLocation()) < attackRange)
        {
            SmashOrbTracker.hitSmashOrb(p);
        }
        return foundAPlayer;
    }

    public static boolean attackPlayersInRange(Player p, String weaponName, float maxPower, float attackRange, boolean ignoreDistance, boolean projectile, boolean physicalHit)
    {
        return attackPlayersInRange(p, p, weaponName, maxPower, attackRange, ignoreDistance, projectile, physicalHit);
    }

    public static boolean attackPlayersInRange(Player p, SpecialItem weapon, float maxPower, float attackRange, boolean ignoreDistance, boolean projectile, boolean physicalHit)
    {
        return attackPlayersInRange(p, weapon.getItemStack().getItemMeta().getDisplayName(), maxPower, attackRange, ignoreDistance, projectile, physicalHit);
    }

    public static boolean attackPlayersInRange(Player p, String weaponName, float maxPower, float attackRange, boolean ignoreDistance, boolean projectile)
    {
        return attackPlayersInRange(p, weaponName, maxPower, attackRange, ignoreDistance, projectile, PHYSICALLY_HIT_DEFAULT);
    }

    public static boolean attackPlayersInRange(Player p, String item, float maxPower, float attackRange, boolean ignoreDistance)
    {
        return attackPlayersInRange(p, item, maxPower, attackRange, ignoreDistance, false);
    }

    public static boolean attackPlayersInRange(Player p, String item, float power, float attackRange)
    {
        return attackPlayersInRange(p, item, power, attackRange, true);
    }

    //    public static List<Entity> getEntitiesInAngleRange(Player p, float attackRange, float hAngleDegrees, float vAngleDegrees)
//    {
//        return getEntitiesInAngleRange(p, attackRange, hAngleDegrees, vAngleDegrees, false);
//    }
//
//    public static List<Entity> getEntitiesInAngleRange(Player p, float attackRange, float hAngleDegrees, float vAngleDegrees, boolean searchBackward)
//    {
//        List<Entity> result = new ArrayList<Entity>();
//        for (Entity potVictim : p.getNearbyEntities(attackRange, attackRange, attackRange))
//        {
//            if (!(potVictim instanceof Player && ((Player)potVictim).equals((Player)p)))
//            {
//                float distance;
//                if (potVictim instanceof Player)
//                {
//                    distance = (float)potVictim.getLocation().distance(p.getLocation());
//                }
//                else
//                {
//                    distance = (float)potVictim.getLocation().distance(p.getEyeLocation());
//                }
//                if (distance <= attackRange)
//                {
//                    Vector v;
//                    if (potVictim instanceof Player)
//                    {
//                        v = SmashManager.getUnitDirection(p.getLocation(), potVictim.getLocation());
//                    }
//                    else
//                    {
//                        v = SmashManager.getUnitDirection(p.getEyeLocation(), potVictim.getLocation());
//                    }
//                    Vector hV = v.clone();
//                    hV.setY(0);
//                    Vector vV = v.clone();
//                    vV.setX(p.getLocation().getDirection().getX());
//                    vV.setZ(p.getLocation().getDirection().getZ());
//
//                    float scale = (float)((1-vV.getY())/(Math.sqrt(vV.getX()*vV.getX() + vV.getZ()*vV.getZ())));
//                    vV.setX(scale*vV.getX());
//                    vV.setZ(scale*vV.getZ());
//
//                    Vector forwardFace = p.getLocation().getDirection();
//                    float hAngle = SmashManager.getAngle(hV, forwardFace)*180/(float)Math.PI;
//                    float vAngle = SmashManager.getAngle(vV, forwardFace)*180/(float)Math.PI;
//                    if (searchBackward)
//                    {
//                        hAngle = 180 - hAngle;
//                    }
//
//                    if (hAngle <= hAngleDegrees && vAngle <= vAngleDegrees)
//                    {
//                        result.add(potVictim);
//                    }
//                }
//            }
//        }
//        return result;
//    }

    public static Entity getEntityBeingFaced(Player p)
    {
        return getEntityBeingFaced(p, 4, 45);
    }

    public static Entity getEntityBeingFaced(Player p, float range, float angleInDegrees)
    {
        Entity result = null;
        for (Entity e : getEntitiesInAngleRange(p, range, angleInDegrees*(float)Math.PI/180F))
        {
            if (result == null || e.getLocation().distance(p.getLocation()) < result.getLocation().distance(p.getLocation()))
            {
                result = e;
            }
        }
        return result;
    }

    private static ArrayList<Entity> getEntitiesInAngleRange(Player p, float range, float angleInRadians)
    {
        ArrayList<Entity> entities = new ArrayList<Entity>();
        for (Entity entity : p.getWorld().getEntities())
        {
            Location actualEntityLocation = entity.getLocation().clone();
            if (SmashManager.getAngle(SmashManager.getUnitDirection(p.getEyeLocation(), actualEntityLocation), p.getEyeLocation().getDirection()) < angleInRadians
                    && p.getEyeLocation().distance(actualEntityLocation) < range)
            {
                entities.add(entity);
            }
        }
        return entities;
    }

//    public static Location getAdjustedItemLocation(Location actualEntityLocation)
//    {
//        return new Location(actualEntityLocation.getWorld(), actualEntityLocation.getX(), actualEntityLocation.getY(), actualEntityLocation.getZ());
//    }

    public static void sendEntityTowardLocation(Location l, final Entity e, float mod, boolean gravitationallyForPlayers)
    {
        if (l.distance(e.getLocation()) > .2F)
        {
            org.bukkit.util.Vector direction = SmashManager.getUnitDirection(e.getLocation(), l);
            final org.bukkit.util.Vector moveAdd = new org.bukkit.util.Vector();
            moveAdd.setX(direction.getX()*mod);
            moveAdd.setY(direction.getY()*mod);
            moveAdd.setZ(direction.getZ()*mod);
            moveAdd.multiply(l.distance(e.getLocation())*0.26F);
            if (e instanceof Player && gravitationallyForPlayers)
            {
                Player p = (Player)e;
                org.bukkit.util.Vector currentSpeed =  p.getVelocity();
                float originalMag = (float)SmashManager.getMagnitude(currentSpeed);
                currentSpeed.add(moveAdd.multiply(originalMag*5));
                if (SmashManager.getMagnitude(moveAdd) > originalMag*2)
                {
                    currentSpeed = moveAdd;
                }
                else
                {
                    currentSpeed.multiply(originalMag/SmashManager.getMagnitude(currentSpeed));
                }
                final org.bukkit.util.Vector newV = currentSpeed;
                Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable() {
                    public String call() {
                        e.setVelocity(newV);
                        return "";
                    }
                });
            }
            else
            {
                Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable() {
                    public String call() {
                        e.setVelocity(moveAdd);
                        return "";
                    }
                });
            }
        }
    }

    /**
     * Negative values can be input to remove damage.
     */
    public static void addDamage(Player p, float damage, boolean multiplyByDamageGain)
    {
        if (SmashWorldManager.gameIsInProgress(p.getWorld()))// || !adjust)
        {
            if (p.getLevel() + damage <= 0)
            {
                clearDamage(p);
            }
            else
            {
                if (multiplyByDamageGain)
                {
                    damage *= DAMAGE_GAIN_FACTOR;
                }
                actualDamages.put(p, getDamage(p) + damage);
                p.setLevel(Math.round(actualDamages.get(p)));
                updateDamage(p);
            }
        }
    }

    private static void updateDamage(Player p)
    {
        getWorldType(p.getWorld()).setWorldBelowNameScore(p, p.getLevel());
    }

    public static float getDamage(Player p)
    {
        if (!actualDamages.containsKey(p))
        {
            actualDamages.put(p, (float)0);
        }
        return actualDamages.get(p);
    }

    public static void clearDamage(Player p)
    {
        actualDamages.put(p, (float)0);
        p.setLevel(0);
        updateDamage(p);
    }

    public static boolean isArtificiallyShielded(Player player)
    {
        return artificiallyShieldedPlayers.contains(player);
    }

    public static void forgetArtificiallyShieldedPlayer(Player player)
    {
        if (artificiallyShieldedPlayers.contains(player))
        {
            artificiallyShieldedPlayers.remove(player);
        }
    }

    public static void setArtificiallyShieldedPlayer(Player player)
    {
        if (!artificiallyShieldedPlayers.contains(player))
        {
            artificiallyShieldedPlayers.add(player);
        }
    }

    public static void notifyOfShield(Entity damageSource, Player victim)
    {
        WorldType.AttackSource source = WorldManager.getAttackSource(damageSource);
        if (source != null && !source.isLiving())
        {
            LivingEntity attacker = source.getLivingEntity();
            if (attacker instanceof Player)
            {
                victim.getWorld().playSound(victim.getLocation(), Sound.BLOCK_ANVIL_LAND, .5F, 0.9F);
                if (isShielded(victim) && !isArtificiallyShielded(victim))
                {
                    attacker.sendMessage(ChatColor.GRAY + String.format("%1$.0f", 10.0*Math.round(getRemainingShield(victim)*10)) + "% Shield");
                }
            }
        }
    }

    public static void attackPlayer(Player damagerPlayer, String damageWeaponName, Location damageSourceLocation, final Player damagedPlayer, float power, boolean physicalHit)
    {
        if (damagerPlayer != null && damageWeaponName != null && damageSourceLocation != null && damagedPlayer != null)
        {
            if (getWorldType(damageSourceLocation.getWorld()).playerCanDamagePlayer(damagerPlayer, damagedPlayer))
            {
                power *= SmashKitManager.getKit(damagerPlayer).getDamageOutPutMod() * getFinalAttackMod(damagerPlayer);
                /*SmashKit currentKit = SmashKitManager.getMaskKit(damagedPlayer);
                if (!selectedKit.equals(currentKit))
                {
                    power *= currentKit.getDamageIntakeMod();
                }*/
                /*SmashKit currentKit = SmashKitManager.getMaskKit(damagerPlayer);
                if (!selectedKit.equals(currentKit))
                {
                    power *= currentKit.getDamageOutputMod();
                }*/
                    //damaged.setVelocity((new Vector()).zero());
                int powerSign = power < 0 ? -1 : 1;
                power = power*powerSign;
                if (power < 1.5F/DAMAGE_GAIN_FACTOR)
                {
                    power = 1.5F/DAMAGE_GAIN_FACTOR;
                }
                float oldDamage = getDamage(damagedPlayer);

                addDamage(damagedPlayer, power*powerSign, true);

                float speed = (float)Math.pow(SmashEntityTracker.getSpeed(damagerPlayer) + 1, 0.3);
                if (speed < 1.12) speed = 1.12F;
                float newDamage = getDamage(damagedPlayer);
                float damageToCalculateKb = (oldDamage + (oldDamage + newDamage)/2)/2;
                float kbModFromDamage = (((float)Math.pow(speed*damageToCalculateKb+1, KB_TIMES_DAMAGE_EXPONENT_MOD))*((float)Math.pow(power + 1, POWER_EXPONENT_MOD)) + 30)*powerSign*getFinalIntakeMod(damagedPlayer)* SmashKitManager.getKit(damagedPlayer).getDamageIntakeMod();

                //damageSourceLocation.setY(damageSourceLocation.getY() + 0.62);

                knockPlayerBack(kbModFromDamage, damageSourceLocation, damagedPlayer, physicalHit);
                SmashManager.setLastHitter(damagedPlayer.getName(), damagerPlayer.getName(), ChatColor.stripColor(damageWeaponName));
            }
        }
        else
        {
            sendErrorMessage("Error! Tried to simulate a player attack when one of the parameters was null!");
        }
    }

    public static boolean isOnHitCooldown(Player damagedPlayer)
    {
        return SmashManager.hasLastHitter(damagedPlayer.getName()) && Plugin.getMillisecond() - SmashManager.getLastHitter(damagedPlayer).millisecond < HIT_COOLDOWN_MS;
    }

    public static void attackWithCustomKB(Player damagerPlayer, String damageWeaponName, Location damageSourceLocation, final Player damagedPlayer, float customKB, float customDamage, boolean physicallyHit)
    {
        if (damagerPlayer != null && damageWeaponName != null && damageSourceLocation != null && damagedPlayer != null)
        {
            if (getWorldType(damagerPlayer.getWorld()).playerCanDamagePlayer(damagerPlayer, damagedPlayer))
            {
                addDamage(damagedPlayer, customDamage, true);
                customKB += 30;
                //Bukkit.getAttacker("HappyMan").sendMessage("custom kb attack");
                knockPlayerBack(customKB, damageSourceLocation, damagedPlayer, physicallyHit);
                SmashManager.setLastHitter(damagedPlayer.getName(), damagerPlayer.getName(), ChatColor.stripColor(damageWeaponName));
            }
        }
        else
        {
            sendErrorMessage("Error! Tried to attack with custom KB witha null parameter!");
        }
    }

    private static void knockPlayerBack(float customKB, Location damageSourceLocation, final Player damagedPlayer, boolean physicalHit)
    {
        if (customKB != 0)
        {
            org.bukkit.util.Vector unitV = SmashManager.getUnitDirection(damageSourceLocation, damagedPlayer.getEyeLocation());
            final org.bukkit.util.Vector kbVector = new org.bukkit.util.Vector(unitV.getX(), unitV.getY(), unitV.getZ());
            kbVector.setX(kbVector.getX() * customKB * KNOCKBACK_MODIFIER);
            kbVector.setY(kbVector.getY() * customKB * KNOCKBACK_MODIFIER);
            kbVector.setZ(kbVector.getZ() * customKB * KNOCKBACK_MODIFIER);

            float flatLaunchMagnitude = (float)SmashManager.getMagnitude(kbVector.getX(), kbVector.getZ());
            float launchMagnitude = (float)SmashManager.getMagnitude(flatLaunchMagnitude, kbVector.getY());
            float launchAngle = (float)Math.atan(kbVector.getY() / flatLaunchMagnitude);
            if (kbVector.getX() < 0)
            {
                launchAngle = launchAngle + (float)Math.PI / 2;
            }
            float degreesToRad = (float)Math.PI / 180;
            boolean tooHigh = launchAngle > 22*degreesToRad;
            boolean tooLow = kbVector.getY() < 0 && (((Entity)damagedPlayer).isOnGround() || damagedPlayer.getLocation().getY() % 0.5 < 0.0001F);
            if (tooHigh || tooLow)
            {
                float transverseAngle = (float)Math.acos(kbVector.getX() / flatLaunchMagnitude);
                if (kbVector.getZ() < 0)
                {
                    if (kbVector.getX() < 0)
                    {
                        transverseAngle = (float)Math.PI*2 - transverseAngle;
                    }
                    else
                    {
                        transverseAngle = -transverseAngle;
                    }
                }
                Random r = new Random();
                if (tooHigh)
                {
                    launchAngle /= Math.pow((r.nextFloat()*100+1), 0.5);
                }
                else
                {
                    launchAngle = (float)(5.0); //* Math.PI /180);
                }
                float newFlatMagnitude = launchMagnitude * (float)Math.cos(launchAngle);
                kbVector.setX(kbVector.getX() * newFlatMagnitude / flatLaunchMagnitude);
                kbVector.setY(launchMagnitude * Math.sin(launchAngle));
                kbVector.setZ(newFlatMagnitude * Math.sin(transverseAngle));
            }

            final int iterations = Math.round(launchMagnitude*KBITERATION_MOD);

            if (iterations > 0)
            {
                if (physicalHit)
                {
                    damagedPlayer.damage(0);
                }

                /*if (!kbTasks.containsKey(damagedPlayer))
                {
                    kbTasks.put(damagedPlayer, new ArrayList<Integer>());
                }

                int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable() {
                    int i = 0;
                    public static void run()
                    {
                        if (i < iterations && SmashWorldManager.isSmashWorld(damagedPlayer.getWorld()))
                        {
                            Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable() {
                                public static String call() {

                                    //Bukkit.getAttacker("HappyMan").sendMessage("knocking " + damagedPlayer.getDisplayName() + " back");
                                    damagedPlayer.setVelocity(new Vector(kbVector.getX(), kbVector.getY(), kbVector.getZ()));
                                    return "";
                                }
                            });
                            i++;
                        }
                    }
                }, 0, 0);
                kbTasks.get(damagedPlayer).add(task);
                cancelTaskAfterDelay(task, iterations);*/
                VelocityModifier.setPlayerVelocity(damagedPlayer, kbVector);
            }
        }
        else
        {
            Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable() {
                public String call() {

                    //Bukkit.getAttacker("HappyMan").sendMessage("knocking player back not really");
                    damagedPlayer.setVelocity(new org.bukkit.util.Vector().zero());
                    return "";
                }
            });
        }
    }

}
