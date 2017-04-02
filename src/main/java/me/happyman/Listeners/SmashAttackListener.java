package me.happyman.Listeners;

import me.happyman.ItemTypes.SmashItem;
import me.happyman.SmashItemDrops.*;
import me.happyman.SmashKitMgt.SmashKit;
import me.happyman.SmashKitMgt.SmashKitManager;
import me.happyman.commands.SmashManager;
import me.happyman.source;
import me.happyman.utils.SmashEntityTracker;
import me.happyman.utils.SmashWorldManager;
import org.bukkit.*;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.Callable;

public class SmashAttackListener implements Listener
{
    private static final float KNOCKBACK_MODIFIER = 0.0135F;//(float)0.028;
    private static final float KBITERATION_MOD = 3F;
    private static final float KB_TIMES_DAMAGE_EXPONENT_MOD = (float)0.9;
    private static final float POWER_EXPONENT_MOD = 0.4F;

    public static final float DAMAGE_GAIN_FACTOR = 0.45F;

    private static final float SWORD_BUFF = 1.2F;
    private static HashMap<Player, ArrayList<Integer>> kbTasks;

    private static ArrayList<Player> artificiallyShieldedPlayers;
    private static HashMap<Player, Float> finalAttackMods;
    private static HashMap<Player, Float> finalIntakeMods;
    private static int HIT_COOLDOWN_MS = 300;
    private static final boolean PHYSICALLY_HIT_DEFAULT = true;

    public SmashAttackListener(SmashManager manager, source plugin)
    {
        Bukkit.getPluginManager().registerEvents(this, plugin);
        artificiallyShieldedPlayers = new ArrayList<Player>();
        kbTasks = new HashMap<Player, ArrayList<Integer>>();
        finalAttackMods = new HashMap<Player, Float>();
        finalIntakeMods = new HashMap<Player, Float>();
    }

    public static float getFinalAttackMod(Player p)
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

    public static float getFinalIntakeMod(Player p)
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

    public static void attackPlayersInAngleRange(Player p, SmashItem attackingItem, float maxPower, float attackRange, float hAngleDegrees, float vAngleDegrees)
    {
        attackPlayersInAngleRange(p, attackingItem, maxPower, attackRange, hAngleDegrees, vAngleDegrees, false);
    }

    public static void attackPlayersInAngleRange(Player p, SmashItem attackingItem, float maxPower, float attackRange, float hAngleDegrees, float vAngleDegrees, boolean projectile)
    {
        attackPlayersInAngleRange(p, attackingItem, maxPower, attackRange, hAngleDegrees, vAngleDegrees, projectile, projectile, PHYSICALLY_HIT_DEFAULT);
    }

    public static void attackPlayersInAngleRange(Player p, SmashItem attackingItem, float maxPower, float attackRange, float hAngleDegrees, float vAngleDegrees, boolean projectile, boolean physicallyHit)
    {
        attackPlayersInAngleRange(p, attackingItem, maxPower, attackRange, hAngleDegrees, vAngleDegrees, projectile, physicallyHit, false);
    }

    public static void attackPlayersInAngleRange(Player p, SmashItem attackingItem, float maxPower, float attackRange, float hAngleDegrees, float vAngleDegrees, boolean projectile, boolean physicallyHit, boolean ignoreDistance)
    {
        attackPlayersInAngleRange(p, attackingItem, maxPower, attackRange, hAngleDegrees, vAngleDegrees, projectile, physicallyHit, ignoreDistance, false);
    }

    public static void attackPlayersInAngleRange(Player p, SmashItem attackingItem, float maxPower, float attackRange, float hAngleDegrees, float vAngleDegrees, boolean ignoreDistance, boolean projectile, boolean physicallyHit, boolean attackBackward)
    {
        attackPlayersInAngleRange(p, attackingItem.getItem().getItemMeta().getDisplayName(), maxPower, attackRange, hAngleDegrees, vAngleDegrees, projectile, physicallyHit, ignoreDistance, attackBackward);
    }

    public static void attackPlayersInAngleRange(Player p, String itemName, float maxPower, float attackRange, float hAngleDegrees, float vAngleDegrees, boolean projectile, boolean physicallyHit, boolean ignoreDistance, boolean attackBackward)
    {
        if (SmashOrb.isLookingAtSmashOrb(p))
        {
            SmashOrb.hitSmashOrb(p);
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
                        Vector v = SmashManager.getUnitDirection(p.getLocation(), potVictim.getLocation());
                        Vector hV = v.clone();
                        hV.setY(0);
                        Vector vV = v.clone();
                        vV.setX(p.getLocation().getDirection().getX());
                        vV.setZ(p.getLocation().getDirection().getZ());

                        float scale = (float)((1-vV.getY())/(Math.sqrt(vV.getX()*vV.getX() + vV.getZ()*vV.getZ())));
                        vV.setX(scale*vV.getX());
                        vV.setZ(scale*vV.getZ());

                        Vector forwardFace = p.getLocation().getDirection();
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
                            attackPlayer(p, itemName, p.getLocation(), (Player)potVictim, power, projectile, physicallyHit);
                        }
                    }
                }
                if (SmashOrb.hasSmashOrb(p.getWorld()) && SmashOrb.getSmashOrb(p.getWorld()).getLocation().distance(p.getLocation()) < attackRange)
                {
                    SmashOrb.hitSmashOrb(p);
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
                    attackPlayer(p, weaponUsed, entityForLocation.getLocation(), (Player)potVictim, actualPower, projectile, physicalHit);
                    foundAPlayer = true;
                }
            }
        }
        if (p == entityForLocation && SmashOrb.hasSmashOrb(p.getWorld()) && SmashOrb.getSmashOrb(p.getWorld()).getLocation().distance(entityForLocation.getLocation()) < attackRange)
        {
            SmashOrb.hitSmashOrb(p);
        }
        return foundAPlayer;
    }

    public static boolean attackPlayersInRange(Player p, String weaponName, float maxPower, float attackRange, boolean ignoreDistance, boolean projectile, boolean physicalHit)
    {
        return attackPlayersInRange(p, p, weaponName, maxPower, attackRange, ignoreDistance, projectile, physicalHit);
    }

    public static boolean attackPlayersInRange(Player p, SmashItem weapon, float maxPower, float attackRange, boolean ignoreDistance, boolean projectile, boolean physicalHit)
    {
        return attackPlayersInRange(p, weapon.getItem().getItemMeta().getDisplayName(), maxPower, attackRange, ignoreDistance, projectile, physicalHit);
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
    /*
    public static List<Entity> getEntitiesInAngleRange(Player p, float attackRange, float hAngleDegrees, float vAngleDegrees)
    {
        return getEntitiesInAngleRange(p, attackRange, hAngleDegrees, vAngleDegrees, false);
    }

    public static List<Entity> getEntitiesInAngleRange(Player p, float attackRange, float hAngleDegrees, float vAngleDegrees, boolean searchBackward)
    {
        List<Entity> result = new ArrayList<Entity>();
        for (Entity potVictim : p.getNearbyEntities(attackRange, attackRange, attackRange))
        {
            if (!(potVictim instanceof Player && ((Player)potVictim).equals((Player)p)))
            {
                float distance;
                if (potVictim instanceof Player)
                {
                    distance = (float)potVictim.getLocation().distance(p.getLocation());
                }
                else
                {
                    distance = (float)potVictim.getLocation().distance(p.getEyeLocation());
                }
                if (distance <= attackRange)
                {
                    Vector v;
                    if (potVictim instanceof Player)
                    {
                        v = SmashManager.getUnitDirection(p.getLocation(), potVictim.getLocation());
                    }
                    else
                    {
                        v = SmashManager.getUnitDirection(p.getEyeLocation(), potVictim.getLocation());
                    }
                    Vector hV = v.clone();
                    hV.setY(0);
                    Vector vV = v.clone();
                    vV.setX(p.getLocation().getDirection().getX());
                    vV.setZ(p.getLocation().getDirection().getZ());

                    float scale = (float)((1-vV.getY())/(Math.sqrt(vV.getX()*vV.getX() + vV.getZ()*vV.getZ())));
                    vV.setX(scale*vV.getX());
                    vV.setZ(scale*vV.getZ());

                    Vector forwardFace = p.getLocation().getDirection();
                    float hAngle = SmashManager.getAngle(hV, forwardFace)*180/(float)Math.PI;
                    float vAngle = SmashManager.getAngle(vV, forwardFace)*180/(float)Math.PI;
                    if (searchBackward)
                    {
                        hAngle = 180 - hAngle;
                    }

                    if (hAngle <= hAngleDegrees && vAngle <= vAngleDegrees)
                    {
                        result.add(potVictim);
                    }
                }
            }
        }
        return result;
    }
    */

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
        for (Entity e : p.getWorld().getEntities())
        {
            Location actualEntityLocation = e.getLocation().clone();
            if (e instanceof Item)
            {
                actualEntityLocation = getAdjustedItemLocation(actualEntityLocation);
            }
            if (SmashManager.getAngle(SmashManager.getUnitDirection(p.getEyeLocation(), actualEntityLocation), p.getEyeLocation().getDirection()) < angleInRadians
                    && p.getEyeLocation().distance(actualEntityLocation) < range)
            {
                entities.add(e);
            }
        }
        return entities;
    }

    public static Location getAdjustedItemLocation(Location actualEntityLocation)
    {
        return new Location(actualEntityLocation.getWorld(), actualEntityLocation.getX(), actualEntityLocation.getY(), actualEntityLocation.getZ());
    }

    public static void sendEntityTowardLocation(Location l, final Entity e, float mod, boolean gravitationallyForPlayers)
    {
        if (l.distance(e.getLocation()) > .2F)
        {
            Vector direction = SmashManager.getUnitDirection(e.getLocation(), l);
            final Vector moveAdd = new Vector();
            moveAdd.setX(direction.getX()*mod);
            moveAdd.setY(direction.getY()*mod);
            moveAdd.setZ(direction.getZ()*mod);
            moveAdd.multiply(l.distance(e.getLocation())*0.26F);
            if (e instanceof Player && gravitationallyForPlayers)
            {
                Player p = (Player)e;
                Vector currentSpeed =  p.getVelocity();
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
                final Vector newV = currentSpeed;
                Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
                    public String call() {
                        e.setVelocity(newV);
                        return "";
                    }
                });
            }
            else
            {
                Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
                    public String call() {
                        e.setVelocity(moveAdd);
                        return "";
                    }
                });
            }
        }
    }

    @EventHandler
    public void damageEvent(EntityDamageEvent e)
    {
        EntityDamageEvent.DamageCause cause = e.getCause();
        World w = e.getEntity().getWorld();
        if (e.getEntity() instanceof Player)
        {
            Player p = (Player)e.getEntity();
            if (SmashWorldManager.isInSpectatorMode(p))
            {
                e.setCancelled(true);
            }
            else if (SmashWorldManager.isSmashWorld(w))
            {
               if (!e.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_ATTACK) && !e.getCause().equals(EntityDamageEvent.DamageCause.ENTITY_EXPLOSION))
               {
                   e.setDamage(0);
                   if (SmashWorldManager.gameHasStarted(w) && !SmashWorldManager.gameHasEnded(w)
                           && (cause.equals(EntityDamageEvent.DamageCause.FIRE_TICK) || cause.equals(EntityDamageEvent.DamageCause.FIRE) || cause.equals(EntityDamageEvent.DamageCause.CONTACT) || cause.equals(EntityDamageEvent.DamageCause.DROWNING)))
                   {
                       if ((cause.equals(EntityDamageEvent.DamageCause.CONTACT) && isShielded(e.getEntity())) || cause.equals(EntityDamageEvent.DamageCause.DROWNING) || (cause.equals(EntityDamageEvent.DamageCause.FIRE_TICK) || cause.equals(EntityDamageEvent.DamageCause.FIRE)) && (SmashKitManager.isUsingFireImmuneKit((Player)e.getEntity()) || isArtificiallyShielded(e.getEntity())))
                       {
                           if (isArtificiallyShielded(e.getEntity()) && (cause.equals(EntityDamageEvent.DamageCause.FIRE) || cause.equals(EntityDamageEvent.DamageCause.FIRE_TICK)))
                           {
                               ((Player)e.getEntity()).setFireTicks(-20);
                           }
                           e.setCancelled(true);
                       }
                       else
                       {
                           SmashManager.addDamage((Player)e.getEntity(), 4F, true);
                       }
                   }
                   else if (e.getCause().equals(EntityDamageEvent.DamageCause.FALL))
                   {
                       SmashKit currentKit = SmashKitManager.getCurrentKit(p);
                       if (currentKit.hasGroundPound())
                       {
                           currentKit.getGroundPound().performLand(p);
                       }
                       SmashKitManager.restoreAllUsagesAndCharges(p, true);
                       e.setCancelled(true);
                   }
                }
             }
        }
    }

    public static boolean isShielded(Entity e)
    {
        return e instanceof Player && (SmashKitManager.getCurrentKit((Player)e).getShield().isPerformingManualAction((Player)e)
                && SmashKitManager.getCurrentKit((Player)e).getShield().getCharge((Player)e) > 0) || isArtificiallyShielded(e);
    }

    public static boolean isArtificiallyShielded(Entity e)
    {
        return e instanceof Player && artificiallyShieldedPlayers.contains((Player)e);
    }

    public static void forgetArtificiallyShieldedPlayer(Player p)
    {
        if (artificiallyShieldedPlayers.contains(p))
        {
            artificiallyShieldedPlayers.remove(p);
        }
    }

    public static void setArtificiallyShieldedPlayer(Player p)
    {
        if (!artificiallyShieldedPlayers.contains(p))
        {
            artificiallyShieldedPlayers.add(p);
        }
    }

    public static void messageDamager(Player damagedPlayer, Entity damageSource)
    {
        if (damageSource instanceof Player || SmashEntityTracker.hasCulprit(damageSource) && Bukkit.getPlayer(SmashEntityTracker.getCulpritName(damageSource)) != null)
        {
            Player p;
            if (damageSource instanceof Player)
            {
                p = (Player)damageSource;
            }
            else
            {
                p = Bukkit.getPlayer(SmashEntityTracker.getCulpritName(damageSource));
            }

            damagedPlayer.getWorld().playSound(damagedPlayer.getLocation(), Sound.ANVIL_LAND, .5F, 0.9F);
            if (!isArtificiallyShielded(damagedPlayer))
            {
                float charge = SmashKitManager.getCurrentKit(damagedPlayer).getShield().getCharge(damagedPlayer);
                p.sendMessage(ChatColor.GRAY + String.format("%1$.0f", 10.0*Math.round(charge*10)) + "% Shield");
            }
        }
    }

    @EventHandler
    public void damageForAmount(EntityDamageByEntityEvent e)
    {
        Entity damageSource = e.getDamager();
        World w = e.getEntity().getWorld();
        //damageSource.getType().toString()
        if (damageSource instanceof Player && SmashWorldManager.isInSpectatorMode((Player)damageSource) || e.getEntity() instanceof Player && SmashWorldManager.isInSpectatorMode((Player)e.getEntity()))
        {
            if (damageSource instanceof Player && SmashWorldManager.isInSpectatorMode((Player)damageSource))
            {
                ((Player)damageSource).sendMessage(ChatColor.GRAY + "You cannot attack while in spectator mode!");
            }
            e.setCancelled(true);
        }
        else
        {
            if (SmashWorldManager.isSmashWorld(w) && SmashWorldManager.gameHasStarted(w) && !SmashWorldManager.gameHasEnded(w)
                && e.getEntity() instanceof Player
                && (damageSource instanceof Player || damageSource instanceof Arrow || damageSource instanceof Snowball
                    || MonsterEgg.getMonsters().contains(damageSource.getType()) && damageSource.getCustomName() != null
                    || damageSource instanceof Egg || damageSource instanceof Fireball || damageSource instanceof TNTPrimed
                    || SmashEntityTracker.hasCulprit(damageSource)))
            {
                if (SmashEntityTracker.isImmuneEntityOwner((Player)e.getEntity(), damageSource))
                {
                    //Bukkit.broadcastMessage(((Player)e.getEntity()).getDisplayName() + " is immune!");
                    e.setCancelled(true);
                }
                else if (isShielded(e.getEntity()))
                {
                    e.setCancelled(true);
                    if (damageSource instanceof Player)
                    {
                        Player p = (Player)damageSource;
                        if (ItemDropManager.isSmashDropItem(p.getItemInHand()) && ItemDropManager.getSmashDropItem(p.getItemInHand()) instanceof BaseballBat)
                        {
                            ((BaseballBat) ItemDropManager.getSmashDropItem(p.getItemInHand())).removeBat(p);
                        }
                    }
                    messageDamager((Player)e.getEntity(), damageSource);
                }
                else if (isShielded(damageSource))
                {
                    e.setCancelled(true);
                }
                else
                {
                    final Player damagedPlayer = (Player)e.getEntity();
                    float power = (float)e.getDamage();
                    String damagerPlayerName = null;
                    String damageWeaponName = null;
                    if (SmashEntityTracker.hasCulprit(damageSource) || damageSource instanceof Projectile && SmashEntityTracker.hasCulprit((Projectile)damageSource))
                    {
                        damageWeaponName = SmashEntityTracker.getWeaponName(damageSource);
                        damagerPlayerName = SmashEntityTracker.getCulpritName(damageSource);
                        power = SmashEntityTracker.getCulpritDamage(damageSource);
                        //SmashEntityTracker.removeCulprit(damageSource);
                    }
                    else if (damageSource instanceof Projectile && ((Projectile)damageSource).getShooter() instanceof Entity && SmashEntityTracker.hasCulprit((Entity)((Projectile)damageSource).getShooter()))
                    {
                        Entity weapon = (Entity)((Projectile)damageSource).getShooter();
                        damageWeaponName = SmashEntityTracker.getWeaponName(weapon);
                        damagerPlayerName = SmashEntityTracker.getCulpritName(weapon);
                        power = SmashEntityTracker.getCulpritDamage(weapon);
                    }
                    else if (damageSource instanceof Player)
                    {
                        Player p = (Player)damageSource;
                        ItemStack item = p.getItemInHand();
                        if (item.hasItemMeta() && item.getItemMeta().hasDisplayName())
                        {
                            damageWeaponName = item.getItemMeta().getDisplayName();
                        }
                        else
                        {
                            damageWeaponName = p.getItemInHand().getType().toString().toLowerCase();
                        }
                        if (item.getType().toString().toLowerCase().contains("sword"));
                        {
                            power *= SWORD_BUFF;
                        }
                        damagerPlayerName = p.getName();
                        if (ItemDropManager.isSmashDropItem(p.getItemInHand()))
                        {
                            SmashItem itemDrop = ItemDropManager.getSmashDropItem(p.getItemInHand());
                            if (itemDrop instanceof Hammer)
                            {
                                power = 100;
                            }
                            else if (itemDrop instanceof BaseballBat)
                            {
                                power = p.getExp() * 40;
                                ((BaseballBat) ItemDropManager.getSmashDropItem(p.getItemInHand())).removeBat(p);
                            }
                        }
                    }
                    else if (damageSource instanceof Arrow && ((Arrow)damageSource).getShooter() instanceof Player)
                    {
                        power = 7;
                        damageWeaponName = "Bow";
                        damagerPlayerName = ((Player)((Arrow)damageSource).getShooter()).getName();
                    }
                    else
                    {
                        damageWeaponName = "null";
                        damagerPlayerName = damagedPlayer.getName();
                    }
                    damageWeaponName = damageWeaponName.replace("air", "Fist");
                    damageWeaponName = ChatColor.stripColor(damageWeaponName);

                    Location l = damageSource.getLocation();
                    if (damageSource instanceof Player)
                    {
                        l = ((Player) damageSource).getEyeLocation();
                    }
                    attackPlayer(SmashManager.getPlugin().getOnlinePlayer(damagerPlayerName), damageWeaponName, l, damagedPlayer, power, damageSource instanceof Projectile, false);
                }
            }
            if (SmashWorldManager.isSmashWorld(w))
            {
                if (damageSource instanceof Player)
                {
                    SmashMishapPreventor.repairItem((Player)damageSource, ((Player)damageSource).getItemInHand());
                }
                e.setDamage(0);
            }
        }
    }

    public static void cancelKB(Player p)
    {
        if (kbTasks.containsKey(p))
        {
            for (int task : kbTasks.get(p))
            {
                Bukkit.getScheduler().cancelTask(task);
            }
            kbTasks.remove(p);
        }
    }

    public static void attackPlayer(Player damagerPlayer, String damageWeaponName, Location damageSourceLocation, final Player damagedPlayer, float power, boolean wasProjectile)
    {
        attackPlayer(damagerPlayer, damageWeaponName, damageSourceLocation, damagedPlayer, power, wasProjectile, PHYSICALLY_HIT_DEFAULT);
    }

    public static void attackPlayer(Player damagerPlayer, String damageWeaponName, Location damageSourceLocation, final Player damagedPlayer, float power, boolean wasProjectile, boolean physicalHit)
    {
        if (damagerPlayer != null && damageWeaponName != null && damageSourceLocation != null && damagedPlayer != null)
        {
            if (!SmashWorldManager.isInSpectatorMode(damagedPlayer) && !SmashWorldManager.isInSpectatorMode(damagerPlayer))
            {
                if (isShielded(damagedPlayer))
                {
                    messageDamager(damagedPlayer, damagerPlayer);
                }
                else if (!isOnHitCooldown(damagedPlayer))
                {
                    power *= SmashKitManager.getSelectedKit(damagerPlayer).getDamageOutPutMod() * getFinalAttackMod(damagerPlayer);
                /*SmashKit currentKit = SmashKitManager.getCurrentKit(damagedPlayer);
                if (!selectedKit.equals(currentKit))
                {
                    power *= currentKit.getDamageIntakeMod();
                }*/
                /*SmashKit currentKit = SmashKitManager.getCurrentKit(damagerPlayer);
                if (!selectedKit.equals(currentKit))
                {
                    power *= currentKit.getDamageOutPutMod();
                }*/
                    //damaged.setVelocity((new Vector()).zero());
                    int powerSign = 1;
                    if (power < 0)
                    {
                        powerSign = -1;
                    }
                    power = power*powerSign;
                    if (power < 1.5F/DAMAGE_GAIN_FACTOR)
                    {
                        power = 1.5F/DAMAGE_GAIN_FACTOR;
                    }
                    float oldDamage = SmashManager.getDamage(damagedPlayer);

                    SmashManager.addDamage(damagedPlayer, power*powerSign, true);

                    float speed = (float)Math.pow(SmashEntityTracker.getSpeed(damagerPlayer) + 1, 0.3);
                    if (speed < 1.12) speed = 1.12F;
                    float newDamage = SmashManager.getDamage(damagedPlayer);
                    float damageToCalculateKb = (oldDamage + (oldDamage + newDamage)/2)/2;
                    float kbModFromDamage = (((float)Math.pow(speed*damageToCalculateKb+1, KB_TIMES_DAMAGE_EXPONENT_MOD))*((float)Math.pow(power + 1, POWER_EXPONENT_MOD)) + 30)*powerSign*getFinalIntakeMod(damagedPlayer)*SmashKitManager.getSelectedKit(damagedPlayer).getDamageIntakeMod();

                    //damageSourceLocation.setY(damageSourceLocation.getY() + 0.62);

                    knockPlayerBack(kbModFromDamage, damageSourceLocation, damagedPlayer, physicalHit);
                    SmashManager.setLastHitter(damagedPlayer.getName(), damagerPlayer.getName(), ChatColor.stripColor(damageWeaponName), wasProjectile);
                }
            }
        }
        else
        {
            SmashManager.getPlugin().sendErrorMessage("Error! Tried to simulate a player attack when one of the parameters was null!");
        }
    }

    private static boolean isOnHitCooldown(Player damagedPlayer)
    {
        return SmashManager.hasLastHitter(damagedPlayer.getName()) && SmashManager.getPlugin().getMillisecond() - SmashManager.getLastHitter(damagedPlayer).millisecond < HIT_COOLDOWN_MS;
    }

    public static void attackWithCustomKB(Player damagerPlayer, String damageWeaponName, Location damageSourceLocation, final Player damagedPlayer, float customKB, float customDamage, boolean physicallyHit, boolean wasProjectile)
    {
        if (damagerPlayer != null && damageWeaponName != null && damageSourceLocation != null && damagedPlayer != null)
        {
            if (isShielded(damagedPlayer))
            {
                messageDamager(damagedPlayer, damagerPlayer);
            }
            else if (!isOnHitCooldown(damagedPlayer))
            {
                SmashManager.addDamage(damagedPlayer, customDamage, true);
                customKB += 30;
                //Bukkit.getPlayer("HappyMan").sendMessage("custom kb attack");
                knockPlayerBack(customKB, damageSourceLocation, damagedPlayer, physicallyHit);
                SmashManager.setLastHitter(damagedPlayer.getName(), damagerPlayer.getName(), ChatColor.stripColor(damageWeaponName), wasProjectile);
            }
        }
    }

    private static void knockPlayerBack(float customKB, Location damageSourceLocation, final Player damagedPlayer, boolean physicalHit)
    {
        if (customKB != 0)
        {
            Vector unitV = SmashManager.getUnitDirection(damageSourceLocation, damagedPlayer.getEyeLocation());
            final Vector kbVector = new Vector(unitV.getX(), unitV.getY(), unitV.getZ());
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

                if (!kbTasks.containsKey(damagedPlayer))
                {
                    kbTasks.put(damagedPlayer, new ArrayList<Integer>());
                }

                int task = Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable() {
                    int i = 0;
                    public void run()
                    {
                        if (i < iterations && SmashWorldManager.isSmashWorld(damagedPlayer.getWorld()))
                        {
                            Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
                                public String call() {

                                    //Bukkit.getPlayer("HappyMan").sendMessage("knocking " + damagedPlayer.getName() + " back");
                                    damagedPlayer.setVelocity(new Vector(kbVector.getX(), kbVector.getY(), kbVector.getZ()));
                                    return "";
                                }
                            });
                            i++;
                        }
                    }
                }, 0, 0);
                kbTasks.get(damagedPlayer).add(task);
                SmashManager.getPlugin().cancelTaskAfterDelay(task, iterations);
            }
        }
        else
        {
            Bukkit.getScheduler().callSyncMethod(SmashManager.getPlugin(), new Callable() {
                public String call() {

                    //Bukkit.getPlayer("HappyMan").sendMessage("knocking player back not really");
                    damagedPlayer.setVelocity(new Vector().zero());
                    return "";
                }
            });
        }
    }

    @EventHandler
    public void hitSmashOrb(PlayerInteractEvent e)
    {
        if ((e.getAction().equals(Action.LEFT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_BLOCK))
                && SmashOrb.isLookingAtSmashOrb(e.getPlayer()))
        {
            SmashOrb.hitSmashOrb(e.getPlayer());
        }
    }
}
