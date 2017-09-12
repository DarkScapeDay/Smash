package me.happyman.utils;

import me.happyman.worlds.UUIDFetcher;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static me.happyman.Plugin.*;

public class FTPAccessor
{
    private static final int RECONNECT_DELAY = 200;
    private static final String CREDENTIAL_FILENAME = "credentials.txt";
    private static boolean isWorking = false;
    private static final FTPClient client = new FTPClient();
    private static Integer connectionAttemptTask = null;

    public FTPAccessor()
    {
        connectionAttemptTask = -1;
        establishConnection();
        connectionAttemptTask = null;
    }

    private static void establishConnection()
    {
        if (!isWorking)
        {
            try
            {
                File credentialFile = FileManager.getServerDataFile("", CREDENTIAL_FILENAME, false);
                if (!credentialFile.exists())
                {
                    throw new IOException("You must put into your plugins/Smash/ServerData a "  + CREDENTIAL_FILENAME +
                            " containing your FTP website login credentials like this:\n\tHostname: <hostname>\n\tUsername: <username>\n\tPassword: <password>");
                }
                String hostname = FileManager.getSimpleData(credentialFile, "Hostname");
                String username = FileManager.getSimpleData(credentialFile, "Username");
                String password = FileManager.getSimpleData(credentialFile, "Password");
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
                Bukkit.getConsoleSender().sendMessage(loggerPrefix() + ChatColor.GREEN + "FTP connection established!");
                isWorking = true;
            }
            catch (IOException ex)
            {
                sendErrorMessage("Error! FTP connection failed! " + ex.getMessage());
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
            connectionAttemptTask = Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable()
            {
                public void run()
                {
                    establishConnection();
                    connectionAttemptTask = null;
                }
            }, RECONNECT_DELAY);
        }
    }

    public static void copyTextFileIntoFTP(File fileToCopy, String relPathInFTP)
    {//
        if (!isWorking)
        {
            sendErrorMessage("Error! FTP connection has not been established!");
        }
        else
        {
            boolean bad = false;
            if (fileToCopy != null && fileToCopy.exists())
            {
                String absoluteDestinationDirectory = FileManager.desandwichPathInSlashes(relPathInFTP) + '/' + fileToCopy.getName();

                try
                {
                    if (!client.storeFile(absoluteDestinationDirectory, new FileInputStream(absoluteDestinationDirectory)))
                    {
                        sendErrorMessage("Unable to store the file " + fileToCopy.getName() + " to the FTP! Be sure that the directory " +
                                ChatColor.YELLOW + relPathInFTP + ChatColor.RED + " exists over there.");
                    }
                }
                catch (IOException ex)
                {
                    ex.printStackTrace();
                    bad = true;
                }

            }
            if (bad)
            {
                sendErrorMessage("Could not find the file when trying to send to the website!");
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
            sendErrorMessage("Error! Attempted to saveData a player profile while FTP was not established!");
            establishConnection();
        }
        else
        {
            String relPath = "/public_html/documents/player_data";
            String uuid = UUIDFetcher.getUUID(p);
            if (uuid != null)
            {
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
                    sendErrorMessage(ChatColor.RED + "Error! FTP " + ex.getMessage().toLowerCase() + "!");
                    isWorking = false;
                }
                copyTextFileIntoFTP(FileManager.getGeneralPlayerFile(p), relPath);
            }
        }
    }
}