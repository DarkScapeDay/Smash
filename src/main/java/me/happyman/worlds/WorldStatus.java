package me.happyman.worlds;

import org.bukkit.ChatColor;

public class WorldStatus
{
    public enum JoinableStatus
    {
        JOINABLE(ChatColor.LIGHT_PURPLE + "" +  ChatColor.MAGIC + "H" + ChatColor.RESET + "" + ChatColor.GREEN + " Click to join! " + ChatColor.LIGHT_PURPLE + "" + ChatColor.MAGIC + "H"),
        IN_PROGRESS(ChatColor.GREEN + "Click to watch!"),
        NOT_JOINABLE(null);

        private String hoverMessage;

        private JoinableStatus(String hoverMessage)
        {
            this.hoverMessage = hoverMessage;
        }

        public String getHoverMessage()
        {
            return hoverMessage;
        }
    }

    private final String message;
    private final JoinableStatus status;

    public WorldStatus(String message, JoinableStatus status)
    {
        this.message = message;
        this.status = status;
    }

    public String getMessage()
    {
        return message;
    }

    public JoinableStatus getJoinableStatus()
    {
        return status;
    }
}
