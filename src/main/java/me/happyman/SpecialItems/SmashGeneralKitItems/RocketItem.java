package me.happyman.SpecialItems.SmashGeneralKitItems;

import me.happyman.SpecialItems.SpecialItemTypes.SpecialItemWithUsages;
import me.happyman.SpecialItems.UsefulItemStack;
import me.happyman.utils.SmashEntityTracker;
import me.happyman.utils.VelocityModifier;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class RocketItem extends SpecialItemWithUsages
{
    public static final Material ROCKET_MATERIAL = Material.FIREWORK;
    private final float yLaunch;// vLaunch;
    private final float hLaunchMax;
    private static final float RADIANS_LAUNCH_OFF_GROUND_WHEN_CROUCHING = (float)Math.PI/2*.23f;
    private static final float EXTRA_HORIZONTAL_JUMP_FACTOR = 1.27f;
    private static final float RATIO_HORIZONTAL_TO_VERTICAL_LAUNCH = 0.6f;

    public RocketItem(String name, int usages, float blocksUp)
    {
        super(new UsefulItemStack(ROCKET_MATERIAL, name), usages, true);

        Vector v = VelocityModifier.computeInitialLaunch(blocksUp * RATIO_HORIZONTAL_TO_VERTICAL_LAUNCH, blocksUp, blocksUp* RATIO_HORIZONTAL_TO_VERTICAL_LAUNCH);
        yLaunch = (float)v.getY();
        hLaunchMax = (float)Math.sqrt(v.getX()*v.getX() + v.getY()*v.getZ());
//        this.yLaunch = blocksUp/7;
    }

    @Override
    public boolean canBeUsed(Player p)
    {
        return super.canBeUsed(p) && !p.isFlying();
    }

    @Override
    public void performLandAction(Player p)
    {
        performResetAction(p);
    }

    @Override
    public void performRightClickAction(Player p, Block blockClicked)
    {
        super.performRightClickAction(p, blockClicked);
        Vector dir = p.getLocation().getDirection();
        float xDir = (float)dir.getX();
        float zDir = (float)dir.getZ();
        float xLaunch = xDir * hLaunchMax;
        float zLaunch = zDir * hLaunchMax;
        //float vLaunch;
        //float horizontalBoost;
        Vector actualV;
        float speedFactor = SmashEntityTracker.getSpeedFactor(p);
        if (SmashEntityTracker.isCrouching(p))
        {
            float rLaunchSquared = xLaunch*xLaunch + yLaunch*yLaunch + zLaunch*zLaunch;
            float newYLaunch = (float)Math.sqrt(rLaunchSquared)*(float)Math.sin(RADIANS_LAUNCH_OFF_GROUND_WHEN_CROUCHING);
            float newXLaunch =(rLaunchSquared - newYLaunch*newYLaunch)/(1f+zLaunch*zLaunch/(xLaunch*xLaunch));
            newXLaunch = xLaunch < 0 ? -(float)Math.sqrt(newXLaunch) : (float)Math.sqrt( newXLaunch);

            float newZLaunch = newXLaunch*zLaunch/xLaunch;

            speedFactor *= EXTRA_HORIZONTAL_JUMP_FACTOR;
            actualV = new Vector(newXLaunch*speedFactor, newYLaunch*speedFactor, newZLaunch*speedFactor);
            //vLaunch = 1.206f*yLaunch*speedFactor;
            //horizontalBoost = 0.80f*yLaunch*speedFactor;
        }
        else
        {
            actualV = new Vector(xLaunch*speedFactor, yLaunch*speedFactor, zLaunch*speedFactor);
            //vLaunch = 0.5F;
            //horizontalBoost = 2F;
        }
        //p.setVelocity(new Vector(p.getLocation().getDirection().getX()*horizontalBoost, vLaunch, p.getLocation().getDirection().getZ()*horizontalBoost));
        p.setVelocity(actualV);
    }
}
