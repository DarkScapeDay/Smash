package me.happyman.SmashKitMgt;

import me.happyman.commands.SmashManager;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;

public class LeatherArmorSet
{
    private ItemStack[] contents;

    private static final Material HELMET_MATERIAL = Material.LEATHER_HELMET;
    private static final Material CHESTPLATE_MATERIAL = Material.LEATHER_CHESTPLATE;
    private static final Material LEGGINGS_MATERIAL = Material.LEATHER_LEGGINGS;
    private static final Material BOOTS_MATERIAL = Material.LEATHER_BOOTS;

    public LeatherArmorSet(String armorName, int[] helmetRGB, int[] chestplateRGB, int[] leggingsRGB, int[] bootsRGB)
    {
        if (isNotGood(helmetRGB) || isNotGood(chestplateRGB) || isNotGood(leggingsRGB) || isNotGood(bootsRGB))
        {
            SmashManager.getPlugin().sendErrorMessage("Error! Tried to create a leather armor set with less than 3 rgb parameters!");
        }
        contents = new ItemStack[4];
        contents[3] = getColoredArmor(HELMET_MATERIAL, armorName, helmetRGB);
        contents[2] = getColoredArmor(CHESTPLATE_MATERIAL, armorName, chestplateRGB);
        contents[1] = getColoredArmor(LEGGINGS_MATERIAL, armorName, leggingsRGB);
        contents[0] = getColoredArmor(BOOTS_MATERIAL, armorName, bootsRGB);
    }

    private boolean isNotGood(int[] rgbColor)
    {
        return rgbColor != null && rgbColor.length < 3;
    }

    public LeatherArmorSet(String armorName, int red, int green, int blue)
    {
        this(armorName, new int[]{red, green, blue}, new int[]{red, green, blue}, new int[]{red, green, blue}, new int[]{red, green, blue});
    }

    public ItemStack getHelmet()
    {
        return contents[3];
    }

    public ItemStack getChestplate()
    {
        return contents[2];
    }

    public ItemStack getLeggings()
    {
        return contents[1];
    }

    public ItemStack getBoots()
    {
        return contents[0];
    }

    public ItemStack[] getContents()
    {
        return contents;
    }

    private ItemStack getColoredArmor(Material mat, String armorName, int[] rgb)
    {
        if (rgb == null)
        {
            return new ItemStack(Material.AIR);
        }
        ItemStack armor = new ItemStack(mat, 1);
        LeatherArmorMeta meta = (LeatherArmorMeta)armor.getItemMeta();
        meta.setDisplayName(armorName);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        meta.setColor(Color.fromRGB(rgb[0], rgb[1], rgb[2]));
        armor.setItemMeta(meta);
        return armor;
    }
}
