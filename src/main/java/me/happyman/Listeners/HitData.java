package me.happyman.Listeners;

public class HitData
{
    public final String killerName;
    public final String weaponName;
    public final String [] hitterWeaponArray;
    public final long millisecond;

    public HitData(String killerName, String weaponName, long millisecond)
    {
        this.killerName = killerName;
        this.weaponName = weaponName;
        this.hitterWeaponArray = new String[] {killerName, weaponName};
        this.millisecond = millisecond;
    }
}
