package me.happyman.SmashKitMgt;

import me.happyman.SmashKits.FinalSmash;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Arrays;

public class SmashKitProperties
{
    private final String name;
    private final ItemStack kitRepresenter;
    private final boolean preventBurning;
    private final float damageIntakeMod;
    private final float damageOutPutMod;
    private final FinalSmash finalSmash;

    public SmashKitProperties(String coloredName, Material representingMaterial, String textDescription, FinalSmash finalSmash)
    {
        this(coloredName, representingMaterial, textDescription, finalSmash, 1);
    }

    public SmashKitProperties(String coloredName, Material representingMaterial, String textDescription, FinalSmash finalSmash, float damageIntakeMod)
    {
        this(coloredName, representingMaterial, textDescription, finalSmash, damageIntakeMod, false);
    }

    public SmashKitProperties(String coloredName, Material representingMaterial, String textDescription, FinalSmash finalSmash, float dmgIntakeMod, boolean preventBurning)
    {
        this(coloredName, representingMaterial, textDescription, finalSmash, dmgIntakeMod, 1F, preventBurning);
    }

    public SmashKitProperties(String coloredName, Material representingMaterial, String textDescription, FinalSmash finalSmash, float dmgIntakeMod, float dmgOutPutMod)
    {
        this(coloredName, representingMaterial, textDescription, finalSmash, dmgIntakeMod, dmgOutPutMod, false);
    }

    public SmashKitProperties(String coloredName, Material representingMaterial, String textDescription, FinalSmash finalSmash, float dmgIntakeMod, float dmgOutPutMod, boolean preventBurning)
    {
        this.name = ChatColor.stripColor(coloredName);
        ItemStack item = new ItemStack(representingMaterial);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(coloredName);
        meta.setLore(new ArrayList<String>(Arrays.asList(textDescription)));
        item.setItemMeta(meta);
        kitRepresenter = item;
        this.preventBurning = preventBurning;
        this.damageIntakeMod = dmgIntakeMod;
        this.damageOutPutMod = dmgOutPutMod;
        this.finalSmash = finalSmash;
    }

    public SmashKitProperties(String coloredName, Material representingMaterial, short matDamage, String textDescription, FinalSmash finalSmash, float dmgIntakeMod, float dmgOutPutMod, boolean preventBurning)
    {
        this.name = ChatColor.stripColor(coloredName);
        ItemStack item = new ItemStack(representingMaterial);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(coloredName);
        meta.setLore(new ArrayList<String>(Arrays.asList(textDescription)));
        item.setItemMeta(meta);
        item.setDurability(matDamage);
        kitRepresenter = item;
        this.preventBurning = preventBurning;
        this.damageIntakeMod = dmgIntakeMod;
        this.damageOutPutMod = dmgOutPutMod;
        this.finalSmash = finalSmash;
    }

    public float getDamageOutPutMod()
    {
        return damageOutPutMod;
    }

    public float getDamageIntakeMod()
    {
        return damageIntakeMod;
    }

    public boolean isImmuneToFire()
    {
        return preventBurning;
    }

    public String getName()
    {
        return name;
    }

    public ItemStack getKitRepresenter()
    {
        return kitRepresenter;
    }

    public FinalSmash getFinalSmash()
    {
        return finalSmash;
    }
}
