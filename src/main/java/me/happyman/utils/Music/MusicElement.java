package me.happyman.utils.Music;

import me.happyman.utils.FileManager;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class MusicElement
{
    private static final Sound DEFAULT_SOUND = Sound.BLOCK_NOTE_PLING;
    private static final float DEFAULT_VOLUME = 1f;
    private static final float DEFAULT_PITCH = 1f;
    public static final int DEFAULT_MUSIC_TICK_DURATION = 5;
    private final Sound sound;
    private final float volume;
    private final float pitch;
    private final int tickDuration;

    public MusicElement(Note note, UsefulInstrument instrument, float volume, int tickDuration)
    {
        this(instrument.getSound(), volume, note.getPitch(), tickDuration);

    }

    public MusicElement(Sound sound, Float volume, Float pitch, Integer tickDuration)
    {
        this.pitch = pitch == null ? DEFAULT_PITCH :
                     pitch < 0.5f ? 0.5f :
                     pitch > 2f ? 2f : pitch;
        this.sound = sound == null ? DEFAULT_SOUND : sound;
        this.volume = volume == null ? DEFAULT_VOLUME :
                      volume > 1f ? 1f :
                      volume < 0f ? 0f : volume;
        this.tickDuration = tickDuration == null ? DEFAULT_MUSIC_TICK_DURATION :
                                                   tickDuration;
    }

    public MusicElement(Note note, UsefulInstrument instrument, int tickDurationP)
    {
        this(note, instrument, DEFAULT_VOLUME, tickDurationP);
    }


    public void play(Player player, boolean privateToPlayer)
    {
        if (privateToPlayer)
        {
            player.playSound(player.getLocation(), sound, volume, pitch);
        }
        else
        {
            play(player.getLocation());
        }
    }

    public void play(Location location)
    {
        location.getWorld().playSound(location, sound, volume, pitch);
    }

    public String serialize()
    {
        return sound.name() + "," + volume + "," + pitch + "," + tickDuration;
    }

    public static MusicElement deserialize(String data)
    {
        String[] datas = data.split(",");
        Sound resultSound = null;
        int dataIndex = 0;
        if (datas.length > dataIndex)
        {
            String soundData = datas[dataIndex++];
            for (Sound sound : Sound.values())
            {
                if (sound.name().equals(soundData))
                {
                    resultSound = sound;
                    break;
                }
            }
        }
        Float resultVolume = FileManager.getFloatValue(datas, dataIndex++);;
        Float resultPitch =  FileManager.getFloatValue(datas, dataIndex++);
        Integer resultTickDuration = FileManager.getIntegerValue(datas, dataIndex++);

        return new MusicElement(resultSound, resultVolume, resultPitch, resultTickDuration);
    }

    public int getTickDuration()
    {
        return tickDuration;
    }
}
