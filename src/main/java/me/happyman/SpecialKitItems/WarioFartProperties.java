package me.happyman.SpecialKitItems;

import org.bukkit.Effect;
import org.bukkit.Sound;

public class WarioFartProperties
{
    public final float FART_RANGE;
    public final float FART_HDEGREES;
    public final float FART_VDEGREES;
    public final float HIGHEST_FART_POWER;
    public final Sound SOUND;
    public final Float SOUND_PITCH;
    public final Effect EFFECT;
    public final Float OFFSET;
    public final float ANGLE_THAT_COUNTS_AS_DOWN = 30F;
    public final float PROXIMITY_NERF = 0.5F;

    public WarioFartProperties(float range, float hDegrees, float maxPower, Sound sound, Float pitch, Effect effect, Float offset)
    {
        FART_RANGE = range;
        FART_HDEGREES = hDegrees;
        FART_VDEGREES = hDegrees;
        HIGHEST_FART_POWER = maxPower;
        SOUND = sound;
        SOUND_PITCH = pitch;
        EFFECT = effect;
        OFFSET = offset;
    }
}
