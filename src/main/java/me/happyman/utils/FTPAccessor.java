package me.happyman.utils;

import me.happyman.source;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.FileInputStream;
import java.io.IOException;

public class FTPAccessor
{
    private static final int RECONNECT_DELAY = 200;
    private static final String CREDENTIAL_FILENAME = "credentials.txt";
    private static source plugin;
    private static boolean isWorking = false;
    private static final FTPClient client = new FTPClient();
    private static Integer connectionAttemptTask = null;

    public FTPAccessor(source plugin)
    {
        connectionAttemptTask = -1;
        FTPAccessor.plugin = plugin;
        establishConnection();
        connectionAttemptTask = null;
    }

    private static void establishConnection()
    {
        if (!isWorking)
        {
            try
            {
                if (!plugin.hasFile(DirectoryType.SERVER_DATA, "", CREDENTIAL_FILENAME))
                {
                    throw new IOException("You must put into your plugins/Smash/ServerData a "  + CREDENTIAL_FILENAME + " containing your FTP website login credentials like this:\n\tHostname: <hostname>\n\tUsername: <username>\n\tPassword: <password>");
                }
                String hostname = plugin.getDatumSimple(DirectoryType.SERVER_DATA, "", CREDENTIAL_FILENAME, "Hostname");
                String username = plugin.getDatumSimple(DirectoryType.SERVER_DATA, "", CREDENTIAL_FILENAME, "Username");
                String password = plugin.getDatumSimple(DirectoryType.SERVER_DATA, "", CREDENTIAL_FILENAME, "Password");
                if (hostname.length() > 1 && username.equals("") || username.equals("*") || password.equals("") || password.equals("*"))
                {
                    throw new IOException("Credentials file not filled out!");
                }
                client.connect(hostname, 21);
                if (!client.isConnected())
                {
                    throw new IOException("Unable to resolve hostname!");
                }
                client.enterRemotePassiveMode();
                if (!client.login(username, password))
                {
                    throw new IOException("Please verify login credentials!");
                }
                Bukkit.getConsoleSender().sendMessage(plugin.loggerPrefix() + ChatColor.GREEN + "FTP connection established!");
                isWorking = true;
            }
            catch (IOException ex)
            {
                plugin.sendErrorMessage("Error! FTP connection failed! " + ex.getMessage());
                isWorking = false;
            }
        }
    }

    public static boolean isWorking()
    {
        if (!isWorking)
        {
            retryConnect();
        }
        return isWorking;
    }

    public static void retryConnect()
    {
        if (!isWorking && connectionAttemptTask == null)
        {
            connectionAttemptTask = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable()
            {
                public void run()
                {
                    establishConnection();
                    connectionAttemptTask = null;
                }
            }, RECONNECT_DELAY);
        }
    }

    public static void copyTextFileIntoFTP(DirectoryType sourceDirectory, String relativePath, String sourceFileName, String relPathInFTP)
    {
        if (!isWorking)
        {
            plugin.sendErrorMessage("Error! FTP connection has not been established!");
        }
        else
        {
            if (!sourceFileName.contains(".") && sourceFileName.length() > 0)
            {
                sourceFileName += ".json";
            }

            String absoluteSourceDirectory = plugin.getAbsolutePath(sourceDirectory, relativePath) + sourceFileName;
            String absoluteDestinationDirectory = source.makeForwardAndSandwichInSlashes(relPathInFTP) + sourceFileName;

            try
            {
                if (!client.storeFile(absoluteDestinationDirectory, new FileInputStream(absoluteSourceDirectory)))
                {
                    plugin.sendErrorMessage("Unable to store the file " + sourceFileName + " to the FTP! Be sure that the directory "
                    + ChatColor.YELLOW + absoluteDestinationDirectory.substring(0, absoluteDestinationDirectory.lastIndexOf('/')) + ChatColor.RED + " exists.");
                }
            }
            catch (IOException ex)
            {
                plugin.sendErrorMessage("Could not find the file " + sourceFileName + " when trying to send to the website!");
            }
        }
    }

    public static void saveProfile(Player p)
    {
        saveProfile(p.getName());
    }

    public static void saveProfile(String p)
    {
        if (!isWorking)
        {
            plugin.sendErrorMessage("Error! Attempted to save a player profile while FTP was not established!");
            establishConnection();
        }
        else
        {
            String relPath = "/public_html/documents/player_data";
            String uuid = plugin.getUUID(p);
            try
            {
                FTPFile[] fileList = client.listFiles("/" + relPath);
                for (int i = 0; i < fileList.length; i++)
                {
                    if (fileList[i].getName().contains(uuid))
                    {
                        client.deleteFile("/" + relPath + "/" + fileList[i].getName());
                        break;
                    }
                }
            }
            catch (IOException ex)
            {
                plugin.sendErrorMessage(ChatColor.RED + "Error! FTP " + ex.getMessage().toLowerCase() + "!");
                isWorking = false;
            }
            copyTextFileIntoFTP(DirectoryType.PLAYER_DATA, "", plugin.getPlayerFileName(p), relPath);
        }
    }
}