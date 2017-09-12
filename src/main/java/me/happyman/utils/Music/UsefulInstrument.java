package me.happyman.utils.Music;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.Sound;

public enum UsefulInstrument
{
    PIANO(Sound.BLOCK_NOTE_PLING, Material.BONE, ChatColor.WHITE + "" + ChatColor.BOLD + "Piano"),
    HARP(Sound.BLOCK_NOTE_HARP, Material.STRING, ChatColor.WHITE + "" + ChatColor.BOLD +  "Harp"),
    DRUM(Sound.BLOCK_NOTE_BASEDRUM, Material.LEATHER, ChatColor.GRAY + "" + ChatColor.BOLD + "Drum"),
    BASS(Sound.BLOCK_NOTE_BASS, Material.BEDROCK, ChatColor.GRAY + "" + ChatColor.BOLD + "Bass"),
    HAT(Sound.BLOCK_NOTE_HAT, Material.GOLD_INGOT, ChatColor.GRAY + "" + ChatColor.BOLD + "Hat"),
    SNARE(Sound.BLOCK_NOTE_SNARE, Material.IRON_INGOT, ChatColor.GRAY + "" + ChatColor.BOLD + "Snare");

    private final Sound sound;
    private final Material material;
    private final String displayName;

    UsefulInstrument(Sound sound, Material mat, String displayName)
    {
        this.sound = sound;
        this.material = mat;
        this.displayName = displayName;
    }

    public Sound getSound()
    {
        return sound;
    }

    public String getDisplayName()
    {
        return displayName;
    }

    public Material getMaterial()
    {
        return material;
    }
}
