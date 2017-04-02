package me.happyman.SmashItems;

import me.happyman.ItemTypes.SwordItemWithUsages;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class NormalSword extends SwordItemWithUsages
{
    public NormalSword(Material swordType)
    {
        super(swordType);
    }

    public NormalSword()
    {
        super(Material.IRON_SWORD);
    }

    public void performAction(Player p)
    {
        /*Vector v = p.getVelocity();
        Vector dir = p.getLocation().getDirection();
        v.setX(v.getX() + dir.getX()*5);
        v.setY(-0.2);
        v.setZ(v.getZ() + dir.getZ()*5);
        p.setVelocity(v);*/
        //p.setVelocity(new Vector(0, -.2, 0));
        if (((Entity)p).isOnGround())
        {
            addUsage(p);
            Vector v = p.getLocation().getDirection();
            //Vector v = smashManager.getUnitDirection(p.getLocation(), p.getTargetBlock(new HashSet<Material>(Arrays.asList(Material.AIR)), 4).getLocation());
            //Bukkit.getPlayer("HappyMan").sendMessage("setting velocity from \"Normal\" Sword");
            p.setVelocity(new Vector(v.getX()*5.5, 0.5, v.getZ()*5.5));
        }
    }
}
