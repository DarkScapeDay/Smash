package me.happyman.SpecialKitItems;

import me.happyman.SmashKitMgt.SmashKitManager;
import me.happyman.SmashKits.FinalSmash;
import me.happyman.commands.SmashManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.Random;

public class MarioFinalSmash extends FinalSmash
{
    private static final MarioFireball fireball = new MarioFireball();
    private static final int POWER = 20;
    private static final int waves = 16;//3;
    private static final int delayTicksBetweenWaves = 6;//15;
    private static final int numPerWave = 15;
    private static final float degreesOfWaveApprox = 30F;
    private final Random r = new Random();

    @Override
    protected void performFinalSmashAbility(final Player p) {
        final String kitName = SmashKitManager.getSelectedKit(p).getName();
        addTask(p, Bukkit.getScheduler().scheduleSyncRepeatingTask(SmashManager.getPlugin(), new Runnable() {
            int iteration = 0;
            int wavesSpawned = 0;
            public void run()
            {
                if (iteration % delayTicksBetweenWaves == 0 && wavesSpawned < waves)
                {
                    wavesSpawned++;
                    for (float i = -degreesOfWaveApprox; i <= degreesOfWaveApprox; i += degreesOfWaveApprox*2/numPerWave)
                    {
                        Vector dir = SmashManager.getAbsOffsetFromRelLocFUR(p.getLocation(), new Vector(1.4*Math.cos(i*Math.PI/180), 1.4*p.getLocation().getDirection().getY(), 1.8*Math.sin(i*Math.PI/180)), true);
                        fireball.launchTNT(p, dir, POWER, kitName + " Finale");
                    }
                }
                iteration++;
            }
        }, 0, 1));
    }

    @Override
    protected void endFinalSmashAbility(Player p) {
    }

    public static MarioFireball getFireball()
    {
        return fireball;
    }
}
