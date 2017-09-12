package me.happyman.worlds;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static me.happyman.Plugin.sendErrorMessage;

public class UUIDFetcher
{
    public static void forgetPlayerID(Player p)
    {
        for (int i = 0; i < cache.size(); i++)
        {
            if (cache.get(i).getCapitalName().equals(p.getName()))
            {
                cache.remove(i);
                break;
            }
        }
    }

    public static class PlayerID
    {
        private final String capitalName;
        private final String uuid;

        private PlayerID(String capitalName, String uuid)
        {
            this.uuid = uuid;
            this.capitalName = capitalName;
        }

        private PlayerID(String something)
        {
            this(something != null && something.length() <= 0x10 ? something : null, something != null && something.length() == 0x20 ? something : null);
        }

        public PlayerID(Player p)
        {
            this(p.getName(), p.getUniqueId().toString().replaceAll("-", ""));
        }

        public String getUUID()
        {
            return uuid;
        }

        public String getCapitalName()
        {
            return capitalName;
        }

        @Override
        public String toString()
        {
            return "Name: " + getCapitalName() + ", " + "UUID: " + getUUID();
        }

        public boolean isValid()
        {
            return uuid != null && uuid.length() == 0x20 && capitalName != null && capitalName.length() <= 0x10;
        }
    }

    private static final List<PlayerID> cache = new ArrayList<PlayerID>();
    private static final String[] URLS_TO_TRY = new String[]
    {
        //starts with ones that can be read from username more likely, ends with ones that can be read from UUID more likely
        "https://api.mojang.com/users/profiles/minecraft/",
        "https://sessionserver.mojang.com/session/minecraft/profile/"
    };

    public static String getUUID(Player p)
    {
        return p.getUniqueId().toString().replaceAll("-", "");
    }

    public static String getUUID(String p)
    {
        if (p == null)
        {
            sendErrorMessage("Error! Tried to get uuid from nothing!");
            return null;
        }
        if (p.length() > 0x10)
        {
            if (p.length() != 0x20)
            {
                sendErrorMessage("Error! Tried to get uuid from " + p);
                return null;
            }
            return p;
        }
        return getUUIDAndCapitalName(p).getUUID();
    }

    public static String getCapitalName(String p)
    {
        return getUUIDAndCapitalName(p).getCapitalName();
    }

    private static PlayerID readPlayerIDFromURL(String nameOrUUID, String urlPrefix)
    {
        try
        {
            URL url = new URL(urlPrefix + nameOrUUID);
            InputStream reader = url.openConnection().getInputStream();

            for (byte i = 0; i < 7; i++)
            {
                if (reader.read() == -1)
                {
                    return null;
                }
            }

            StringBuilder uuidBuilder = new StringBuilder();
            for (byte i = 0; i < 32; i++)
            {
                int read = reader.read();
                if (read == -1)
                {
                    return null;
                }
                uuidBuilder.append((char)read);
            }

            for (byte i = 0; i < 10; i++)
            {
                if (reader.read() == -1)
                {
                    return null;
                }
            }

            StringBuilder nameBuilder = new StringBuilder();
            int read = reader.read();
            if (read == -1)
            {
                return new PlayerID(nameOrUUID);
            }
            char c = (char)read;
            do
            {
                nameBuilder.append(c);
                read = reader.read();
                if (read == -1)
                {
                    return null;
                }
                c = (char)read;
            } while (c != '"');

            PlayerID result = new PlayerID(nameBuilder.toString(), uuidBuilder.toString());
            savePlayerID(result);
            return result;
        }
        catch (IOException ex)
        {
//            if (!ex.getMessage().contains("429"))
//            {
//                ex.printStackTrace();
//            }
//            else
//            {
//                sendErrorMessage(ex.getMessage());
//            }
        }
        return null;
    }

    public static void savePlayerID(Player p)
    {
        savePlayerID(p.getName(), p.getUniqueId().toString().replaceAll("-", ""));
    }

    private static void savePlayerID(PlayerID id) throws IllegalArgumentException
    {
        if (id.isValid())
        {
            for (PlayerID knownID : cache)
            {
                if (knownID.getUUID().equals(id.getUUID()))
                {
                    return;
                }
            }
            cache.add(id);
        }
        else
        {
            throw new IllegalArgumentException(ChatColor.RED + "Error! " + PlayerID.class.getSimpleName() + " " + id.toString() + " was not valid!");
        }
    }

    public static void savePlayerID(String capitolName, String uuid)
    {
        savePlayerID(new PlayerID(capitolName, uuid));
    }

    public static PlayerID getUUIDAndCapitalName(Player p)
    {
        PlayerID result = new PlayerID(p);
        savePlayerID(result);
        return result;
    }

    public static PlayerID getUUIDAndCapitalName(String nameOrUUID)
    {
        if (nameOrUUID != null)
        {
            if (nameOrUUID.length() <= 0x10)
            {
                nameOrUUID = nameOrUUID.toLowerCase();
                for (PlayerID id : cache)
                {
                    if (id.getCapitalName().toLowerCase().equals(nameOrUUID))
                    {
                        return id;
                    }
                }

                for (String urlPrefix : URLS_TO_TRY)
                {
                    PlayerID readResult = readPlayerIDFromURL(nameOrUUID, urlPrefix);
                    if (readResult != null)
                    {
                        return readResult;
                    }
                }
            }
            else if (nameOrUUID.length() == 0x20)
            {
                for (PlayerID id : cache)
                {
                    if (id.getUUID().equals(nameOrUUID))
                    {
                        return id;
                    }
                }

                for (int i = URLS_TO_TRY.length - 1; i >= 0; i--)
                {
                    String urlPrefix = URLS_TO_TRY[i];
                    PlayerID readResult = readPlayerIDFromURL(nameOrUUID, urlPrefix);
                    if (readResult != null)
                    {
                        return readResult;
                    }
                }
            }
        }

        return new PlayerID(nameOrUUID);
    }
}
