package me.happyman.utils.Music;

import me.happyman.Plugin;
import me.happyman.utils.FileManager;
import me.happyman.utils.InteractiveChat;
import me.happyman.utils.Verifier;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static me.happyman.Plugin.getArgComp;
import static me.happyman.Plugin.getPlugin;
import static me.happyman.utils.FileManager.*;

public class Song
{
    private static final String SONG_COMMAND = "song";
    private static final String SONG_FOLDER_NAME = "Songs";
    private static final String PLAY_ARG = "play";
    private static final String STOP_ARG = "stop";
    private static final String LIST_ARG = "list";
    private static final String DELETE_ARG = "delete";
    private static final String MAKE_INTRO_ARG = "intro";
    private static final String REMOVE_INTRO_ARG = "removeintro";
    private static final String INTRO_SONG_DATANAME = "introsong";
    private static final String SONG_EXTENSION = ".hsong";

    private static final HashMap<Player, Song> songPlayers = new HashMap<Player, Song>();
//    private static final long MILLISECONDS_REJECT = 100;

    private static File getIntroSongFile(Player p)
    {
        return getGeneralPlayerFile(p);
    }

    private static void setIntroSong(Player p, File song)
    {
        putData(getIntroSongFile(p), INTRO_SONG_DATANAME, getSongName(song));
    }

    private static void removeIntroSong(Player p)
    {
        p.sendMessage(ChatColor.GREEN + "Removed intro song (if you had one)");
        removeEntryWithKey(getIntroSongFile(p), INTRO_SONG_DATANAME);
    }

    public static void playIntroSong(Player p)
    {
        String fave = getData(getIntroSongFile(p), INTRO_SONG_DATANAME);
        Song whatToPlay = fave != null && fave.length() > 0 ? Song.read(getSavedSongFile(p, fave, false)) : null;
        if (whatToPlay != null)
        {
            whatToPlay.play(p, false);
        }
    }

    public static void initialize() {};

    static //@TODO: maybe make these Meta commands
    {
        Plugin.setExecutor(SONG_COMMAND, new CommandExecutor()
        {
            @Override
            public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
            {
                if (!(sender instanceof Player))
                {
                    sender.sendMessage(ChatColor.RED + "You can hear? I didn't hear that news.");
                    return true;
                }
                else if (args.length == 0 || args.length == 1 && args[0].equalsIgnoreCase("help"))
                {
                    return false;
                }
                Player p = (Player)sender;
                int argIndex = 0;
                String whatToDoArg = args[argIndex++].toLowerCase();
                if (args.length >= 2)
                {
                    String songName = getArgComp(args, argIndex);
                    if (whatToDoArg.equals(PLAY_ARG))
                    {
                        Song song = getSavedSong(p, songName);
                        if (song == null)
                        {
                            p.sendMessage(ChatColor.RED + "Song " + songName + " not found!");
                        }
                        else
                        {
                            song.play(p, false);
                        }
                    }
                    else if (whatToDoArg.equals(DELETE_ARG))
                    {
                        File f = getSavedSongFile(p, songName, false);
                        if (!f.exists())
                        {
                            p.sendMessage(ChatColor.RED + "Song " + songName + " was already deleted!");
                        }
                        else if (f.delete())
                        {
                            p.sendMessage(ChatColor.GREEN + "Song " + songName + " has been deleted!");
                        }
                        else
                        {
                            p.sendMessage(ChatColor.RED + "Failed to delete song " + songName + "!");
                        }
                    }
                    else if (whatToDoArg.equals(MAKE_INTRO_ARG))
                    {
                        File f = getSavedSongFile(p, songName, false);
                        if (!f.exists())
                        {
                            p.sendMessage(ChatColor.RED + "Song not found!");
                        }
                        else
                        {
                            setIntroSong(p, f);
                            p.sendMessage(ChatColor.GREEN + "Intro set to " + songName + "!");
                        }
                    }
                    else
                    {
                        return false;
                    }
                    return true;
                }
                else if (whatToDoArg.equals(LIST_ARG))
                {
                    File[] knownSongs = getSavedSongs(p);
                    p.sendMessage(ChatColor.LIGHT_PURPLE + "♩♬ Your saved songs ♪♫");
                    for (File f : knownSongs)
                    {
                        InteractiveChat.sendMessage(p, ChatColor.GRAY + getSongName(f), ChatColor.GRAY + "Click to play!", SONG_COMMAND + " " + PLAY_ARG + " " + getSongName(f));
                    }
                }
                else if (whatToDoArg.equals(REMOVE_INTRO_ARG))
                {
                    removeIntroSong(p);
                }
                else if (whatToDoArg.equals(STOP_ARG))
                {
                    if (isPlayingSong(p))
                    {
                        stopPlayingSong(p);
                    }
                    else
                    {
                        p.sendMessage(ChatColor.RED + "You aren't playing a song right now.");
                    }
                }
                else
                {
                    return false;
                }
                return true;
            }
        },
        new TabCompleter()
        {
            @Override
            public List<String> onTabComplete(CommandSender commandSender, Command command, String s, String[] strings)
            {
                List<String> argsPossible = new ArrayList<String>();
                switch (strings.length)
                {
                    case 0:
                    case 1:
                        argsPossible.add(PLAY_ARG);
                        argsPossible.add(DELETE_ARG);
                        argsPossible.add(STOP_ARG);
                        argsPossible.add(MAKE_INTRO_ARG);
                        argsPossible.add(REMOVE_INTRO_ARG);
                        argsPossible.add(LIST_ARG);
                        break;
                    case 2:
                        if (commandSender instanceof Player)
                        {
                            Player p = (Player)commandSender;
                            for (File f : getSavedSongs(p))
                            {
                                argsPossible.add(getSongName(f));
                            }
                        }
                }
                return argsPossible;
            }
        });
    }

    private static String getSongName(File f)
    {
        return f.getName().substring(0, f.getName().length() - SONG_EXTENSION.length());
    }

    private final List<MusicElement> musicElements;

    private static Song getSavedSong(Player p, String songName)
    {
        return read(getSavedSongFile(p, songName, false));
    }

    private static File getSavedSongFile(Player p, String songName, boolean forceValid)
    {
        return FileManager.getPlayerFile(p, SONG_FOLDER_NAME, songName + SONG_EXTENSION, forceValid);
    }

    private static File[] getSavedSongs(Player p)
    {
        File[] result = FileManager.getPlayerFile(p, SONG_FOLDER_NAME, "", false).listFiles();
        return result == null ? new File[0] : result;
    }

    public static Song read(File f)
    {
        if (f != null && f.exists())
        {
            List<String> serializedDatas = FileManager.readLinesFromFile(f);
            List<MusicElement> musicElements = new ArrayList<MusicElement>();
            for (String line : serializedDatas)
            {
                musicElements.add(MusicElement.deserialize(line));
            }
            return new Song(musicElements);
        }
        return null;
    }

    public Song()
    {
        this.musicElements = new ArrayList<MusicElement>();
    }

    private Song(List<MusicElement> musicElements)
    {
        this.musicElements = musicElements;
    }

    private Long timeOfLastNoteAdded = null;
    public void add(MusicElement element)
    {
//        Long currentTime = timeOfLastNoteAdded == null ? null : System.currentTimeMillis();
//        if (currentTime == null || timeOfLastNoteAdded + MILLISECONDS_REJECT < currentTime)
//        {
        musicElements.add(element);
//            timeOfLastNoteAdded = currentTime;
//        }
    }

    public void removeLastElement()
    {
        if (musicElements.size() > 0)
        {
            musicElements.remove(musicElements.size() - 1);
        }
    }

    public int size()
    {
        return musicElements.size();
    }

    public void removeElement(MusicElement element)
    {
        if (musicElements.contains(element))
        {
            musicElements.remove(element);
        }
    }

    public void removeElement(int index)
    {
        if (index < size())
        {
            musicElements.remove(index);
        }
    }

    private void print(File f)
    {
        List<String> serializedDatas = new ArrayList<String>();
        for (MusicElement elt : musicElements)
        {
            serializedDatas.add(elt.serialize());
        }
        printLinesToFile(f, serializedDatas);
    }

    private void print(final Player p, String name)
    {
        p.sendMessage(ChatColor.GREEN + "Song saved! Use /song to use it!");
        print(getSavedSongFile(p, name, true));
    }

    public void save(final Player p, final String name)
    {
        if (!getSavedSongFile(p, name, false).exists())
        {
            print(p, name);
        }
        else
        {
            new Verifier.BooleanVerifier(p, ChatColor.LIGHT_PURPLE + "Do you want to overwrite your existing song called " + name + "?")
            {
                @Override
                public void performYesAction()
                {
                    print(p, name);
                }

                @Override
                public void performNoAction()
                {
                    new Verifier.BooleanVerifier(p, ChatColor.LIGHT_PURPLE + "Do you want to call it something else (if not it will be erased)?")
                    {
                        @Override
                        public void performYesAction()
                        {
                            new Verifier.StringVerifier(p, ChatColor.LIGHT_PURPLE + "What do you want to call it instead?")
                            {
                                @Override
                                public void performAction(String decision)
                                {
                                    save(p, name);
                                }
                            };
                        }

                        @Override
                        public void performNoAction()
                        {
                            p.sendMessage(ChatColor.RED + "Song thrown in the garbage song " + name + " cancelled");
                        }
                    };
                }
            };
        }
    }

    private Integer playingTask = null;

    public boolean isBeingPlayed()
    {
        return playingTask != null;
    }

    public static void stopPlayingSong(Player p)
    {
        Song currentSong = songPlayers.get(p);
        if (currentSong != null)
        {
            currentSong.stopPlaying(p);
        }
    }

    public static boolean isPlayingSong(Player p)
    {
        return songPlayers.containsKey(p);
    }

    public void stopPlaying(Player who)
    {
        who.sendMessage(ChatColor.GREEN + "Song done :)");
        if (playingTask != null)
        {
            Bukkit.getScheduler().cancelTask(playingTask);
            playingTask = null;
        }
        songPlayers.remove(who);
    }

    public abstract static class ActionSongEnded
    {
        public abstract void doStuff();
    }

    private void playElement(final Player player, final boolean privateToPlayer, final int index, final ActionSongEnded whatToDoIfActuallyFinished)
    {
        if (index < size() && index >= 0)
        {
            final MusicElement curElement = musicElements.get(index);
            curElement.play(player, privateToPlayer);
            playingTask = Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable()
            {
                public void run()
                {
                    playElement(player, privateToPlayer, index + 1, whatToDoIfActuallyFinished);
                }
            }, curElement.getTickDuration());
        }
        else
        {
            stopPlaying(player);
            if (whatToDoIfActuallyFinished != null)
            {
                whatToDoIfActuallyFinished.doStuff();
            }
        }
    }

    public void play(final Player p, final boolean privateToPlayer)
    {
        play(p, privateToPlayer, null);
    }

    public void play(final Player p, final boolean privateToPlayer, final ActionSongEnded whatToDoAfterFinished)
    {
        if (!songPlayers.containsKey(p))
        {
            songPlayers.put(p, this);
            p.sendMessage(ChatColor.GREEN + "Playing song!");
            playElement(p, privateToPlayer, 0, whatToDoAfterFinished);
        }
        else
        {
            new Verifier.BooleanVerifier(p, ChatColor.RED + "You are already playing a song! Cancel it and play the next one?")
            {
                @Override
                public void performYesAction()
                {
                    stopPlaying(p);
                    play(p, privateToPlayer, whatToDoAfterFinished);
                    p.sendMessage(ChatColor.GREEN + "Will do.");
                }

                @Override
                public void performNoAction()
                {
                    if (isPlayingSong(p))
                    {
                        p.sendMessage(ChatColor.GREEN + "Continuing as normal.");
                    }
                    else
                    {
                        p.sendMessage(ChatColor.GREEN + "Well it looks like the song just ended. Let's start the one you wanted to play next.");
                        play(p, privateToPlayer, whatToDoAfterFinished);
                    }
                }
            };
        }
    }

//    private void playElement(final Location location, final int index)
//    {
//        if (index < size() && index >= 0)
//        {
//            final MusicElement curElement = musicElements.get(index);
//            curElement.play(location);
//            playingTask = Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable()
//            {
//                public void run()
//                {
//                    playElement(location, index + 1);
//                }
//            }, curElement.getTickDuration());
//        }
//        else
//        {
//            stopPlaying();
//        }
//    }
//
//    private void play(Location location)
//    {
//        playElement(location, 0);
//    }
}
