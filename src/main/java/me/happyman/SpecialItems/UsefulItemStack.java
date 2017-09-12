package me.happyman.SpecialItems;

import me.happyman.utils.InventoryManager;
import org.apache.commons.lang.Validate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static me.happyman.Plugin.sendErrorMessage;

public class UsefulItemStack extends ItemStack
{

    public String getColoredName()
    {
        return InventoryManager.getItemName(this);
    }

    public UsefulItemStack setName(String uniqueName) throws NullPointerException
    {
        Validate.notNull(uniqueName);
        ItemMeta meta = getItemMeta();
        if (uniqueName.equals(ChatColor.stripColor(uniqueName)))
        {
            uniqueName = ChatColor.WHITE + uniqueName;
        }
        meta.setDisplayName(uniqueName);
        setItemMeta(meta);
        return this;
    }

    public UsefulItemStack(String justName)
    {
        super(Material.STONE);
        setName(justName);
    }

    public UsefulItemStack setLore(String lore)
    {
        if (lore != null && lore.length() > 0)
        {
            ItemMeta trackerMeta = getItemMeta();

            ArrayList<String> loreLines = new ArrayList<String>();
            StringBuilder builder = new StringBuilder();
            char currentColorCode = '5';
            List<Character> currentStyleCodes = new ArrayList<Character>();
            for (int i = 0; i < lore.length(); i++)
            {
                char c = lore.charAt(i);
                switch (c)
                {
                    case '\n':
                        String builtString = builder.toString();
                        if (builtString.length() > 0)
                        {
                            loreLines.add(builtString);
                        }
                        builder = new StringBuilder();
                        builder.append('§');
                        builder.append(currentColorCode);
                        for (Character character : currentStyleCodes)
                        {
                            builder.append('§');
                            builder.append(character);
                        }
                        break;
                    case '§':
                        builder.append(c);
                        i++;
                        if (i < lore.length())
                        {
                            c = lore.charAt(i);
                            if (c >= '0' && c < 'k')
                            {
                                currentColorCode = c;
                            }
                            else if (c >= 'k' && c < 'r')
                            {
                                if (!currentStyleCodes.contains(c))
                                {
                                    currentStyleCodes.add(c);
                                }
                            }
                            else if (c == 'r')
                            {
                                currentColorCode = 'f';
                                currentStyleCodes.clear();
                            }
                            builder.append(c);
                        }
                        break;
                    default:
                        builder.append(c);
                        break;
                }
            }
            String builtString = builder.toString();
            if (builtString.length() > 0)
            {
                loreLines.add(builder.toString());
            }

            trackerMeta.setLore(loreLines);
            setItemMeta(trackerMeta);
        }
        return this;
    }

    public UsefulItemStack(Material mat, String uniqueName, String lore, byte data)
    {
        super(mat, 1, (short)0, data == 0 ? null : data);
        setName(uniqueName);
        setLore(lore);
    }

    public UsefulItemStack setEnchants(Enchantment[] enchantments, int[] levels)
    {
        if (enchantments.length != levels.length)
        {
            sendErrorMessage("Error! Enchants and levels for " + getItemMeta().getDisplayName() + " were not just not the same any more!");
        }
        else
        {
            boolean tooMuch = false;
            Map<Enchantment, Integer> enchMap = new HashMap<Enchantment, Integer>();
            for (int i = 0; i < enchantments.length && i < levels.length; i++) {
                Enchantment enchant = enchantments[i];
                int level = levels[i];
                if (level > enchant.getMaxLevel() || level <= 0) {
                    tooMuch = true;
                    break;
                }
                enchMap.put(enchant, level);
            }

            if (!tooMuch)
            {
                if (enchMap.size() > 0)
                {
                    addUnsafeEnchantments(enchMap);
                }
            }
            else
            {
                ItemMeta meta = getItemMeta();
                for (int i = 0; i < enchantments.length && i < levels.length; i++)
                {
                    meta.addEnchant(enchantments[i], levels[i], true);
                }
                setItemMeta(meta);
            }
        }
        return this;
    }

    public UsefulItemStack(Material mat, String uniqueName)
    {
        super(mat);
        setName(uniqueName);
    }

    public UsefulItemStack(Material mat, String uniqueName, String lore)
    {
        super(mat);
        setName(uniqueName);
        setLore(lore);
    }

    public UsefulItemStack(Material mat, String uniqueName, int amount)
    {
        super(mat, amount);
        setName(uniqueName);
    }

    public UsefulItemStack(Material mat, String uniqueName, String lore, Enchantment[] enchants, int[] levels)
    {
        super(mat);
        setName(uniqueName);
        setEnchants(enchants, levels);
        setLore(lore);
    }

    public UsefulItemStack(Material mat, String uniqueName, Enchantment[] enchants, int[] levels)
    {
        super(mat);
        setName(uniqueName);
        setEnchants(enchants, levels);
    }

    public UsefulItemStack(Material mat, String uniqueName, byte data)
    {
        super(mat, 1, (short)0, data == 0 ? null : data);
        setName(uniqueName);
    }

    public UsefulItemStack(Material mat, String uniqueName, int amount, short damage)
    {
        super(mat, amount, damage);
        setName(uniqueName);
    }

    public UsefulItemStack(Material mat, String uniqueName, int amount, short damage, byte data)
    {
        super(mat, amount, damage, data == 0 ? null : data);
        setName(uniqueName);
    }

    public UsefulItemStack(UsefulItemStack stack) throws IllegalArgumentException
    {
        super(stack);
    }

    public UsefulItemStack(ItemStack stack) throws IllegalArgumentException
    {
        super(stack);
    }
}
