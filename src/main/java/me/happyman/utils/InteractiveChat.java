package me.happyman.utils;

import net.minecraft.server.v1_12_R1.ChatMessageType;
import net.minecraft.server.v1_12_R1.IChatBaseComponent;
import net.minecraft.server.v1_12_R1.PacketPlayOutChat;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.util.Arrays;
import java.util.List;

public class InteractiveChat
{
    public static class MessagePart
    {
        private final String text;
        private final String hoverMessage;
        private final String interactiveClickCommand;

        public MessagePart(String nonInteractiveMessage)
        {
            this(nonInteractiveMessage, null, null);
        }

        public MessagePart(String text, String hoverMessage, String interactiveClickCommand)
        {
            this.text = (text == null || text.length() == 0) ? null : text;
            this.hoverMessage = (hoverMessage == null || hoverMessage.length() == 0) ? null : hoverMessage;
            this.interactiveClickCommand = (interactiveClickCommand == null || interactiveClickCommand.length() == 0) ? null :
                                            interactiveClickCommand.charAt(0) == '/' ? (interactiveClickCommand.length() == 1 ? null :
                                                                                        interactiveClickCommand.substring(1, interactiveClickCommand.length())) :
                                                                                        interactiveClickCommand;
        }
    }

    public static MessagePart[] getMessage(String text)
    {
        return getMessage(text, null, null);
    }

    public static MessagePart[] getMessage(String text, String hoverMessage, String clickCommand)
    {
        return new MessagePart[]
        {
            new MessagePart(text, hoverMessage, clickCommand)
        };
    }

    public static void sendMessage(Player p, String text, String hoverMessage, String clickCommand)
    {
        sendMessage(p, new MessagePart[] { new MessagePart(text, hoverMessage, clickCommand) });
    }

    public static void sendMessage(Player p, List<MessagePart> messages)
    {
        if (messages != null && messages.size() > 0)
        {
            JSONObject mainObj = new JSONObject();
            mainObj.put("text", "");

            JSONArray arr = new JSONArray();
            for (MessagePart message : messages)
            {
                if (message.text != null)
                {
                    JSONObject arrObjInteractive = new JSONObject();
                    arrObjInteractive.put("text", message.text);

                    if (message.hoverMessage != null)
                    {
                        JSONObject hoverObj = new JSONObject();
                        hoverObj.put("action", "show_text");
                        hoverObj.put("value", message.hoverMessage);
                        arrObjInteractive.put("hoverEvent", hoverObj);
                    }

                    if (message.interactiveClickCommand != null)
                    {
                        JSONObject clickObj = new JSONObject();
                        clickObj.put("action", "run_command");
                        clickObj.put("value", "/" + message.interactiveClickCommand);
                        arrObjInteractive.put("clickEvent", clickObj);
                    }
                    arr.add(arrObjInteractive);
                }
            }
            mainObj.put("extra", arr);

            ((CraftPlayer)p).getHandle().playerConnection.sendPacket(new PacketPlayOutChat(IChatBaseComponent.ChatSerializer.a(mainObj.toJSONString()), ChatMessageType.SYSTEM));
        }
    }


    public static void sendMessage(Player p, MessagePart[] messages)
    {
        if (messages != null)
        {
            sendMessage(p, Arrays.asList(messages));
        }
    }

//    public static void sendMessage(Player p, String firstPart, String interactivePart, String lastPart, String hoverMessages, String interactiveMessageClickCommand)
//    {
//        int firstPartLength = firstPart == null ? 0 : firstPart.length();
//        int interactivePartLength = interactivePart == null ? 0 : interactivePart.length();
//        int lastPartLength = lastPart == null ? 0 : lastPart.length();
//        int hoverMessageLength = hoverMessages == null ? 0 : hoverMessages.length();
//        int interactiveCommandLength = interactiveMessageClickCommand == null ? 0 : interactiveMessageClickCommand.length();
//
//        if (firstPartLength + interactivePartLength + lastPartLength > 0)
//        {
//            JSONObject mainObj = new JSONObject();
//            mainObj.put("text", firstPart);
//            JSONArray arr = new JSONArray();
//            if (interactivePartLength > 0)
//            {
//                JSONObject arrObjInteractive = new JSONObject();
//                arrObjInteractive.put("text", interactivePart);
//
//                if (hoverMessageLength > 0)
//                {
//                    JSONObject hoverObj = new JSONObject();
//                    hoverObj.put("action", "show_text");
//                    hoverObj.put("value", hoverMessages);
//                    arrObjInteractive.put("hoverEvent", hoverObj);
//                }
//
//                if (interactiveCommandLength > 0)
//                {
//                    JSONObject clickObj = new JSONObject();
//                    clickObj.put("action", "run_command");
//                    clickObj.put("value", "/" + interactiveMessageClickCommand);
//                    arrObjInteractive.put("clickEvent", clickObj);
//                }
//                arr.add(arrObjInteractive);
//            }
//            if (lastPartLength > 0)
//            {
//                JSONObject arrObjectLast = new JSONObject();
//                arrObjectLast.put("text", lastPart);
//                arr.add(arrObjectLast);
//            }
//            mainObj.put("extra", arr);
//
//            IChatBaseComponent comp = IChatBaseComponent.ChatSerializer.a(mainObj.toJSONString());
//            PacketPlayOutChat packet = new PacketPlayOutChat(comp, (byte)1);
//            ((CraftPlayer)p).getHandle().playerConnection.sendPacket(packet);
//        }
//    }
}
