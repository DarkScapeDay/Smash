package me.happyman.utils;

import me.happyman.Plugin;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import static me.happyman.Plugin.getPlugin;

public class Verifier implements Listener
{
    private static final HashMap<Player, GenericVerifier> verifiers = new HashMap<Player, GenericVerifier>();
    static
    {
        Bukkit.getPluginManager().registerEvents(new Listener()
        {
            @EventHandler
            public void commandEvent(PlayerCommandPreprocessEvent event)
            {
                Player p = event.getPlayer();
                GenericVerifier action = verifiers.get(p);
                if (action != null)
                {
                    event.setCancelled(true);
                    action.acceptInput(p, event.getMessage().substring(event.getMessage().length() > 0 ? 1 : 0, event.getMessage().length()));
                }
            }

            @EventHandler
            public void VerifierTypingEvent(AsyncPlayerChatEvent event)
            {
                Player p = event.getPlayer();
                GenericVerifier action = verifiers.get(p);
                if (action != null)
                {
                    action.acceptInput(p, event.getMessage().length() > 1 ? event.getMessage().substring(0, event.getMessage().length() - 1) : "");
                    event.setCancelled(true);
                }
            }
        }, getPlugin());
    }

    public static void forcablyReleaseVerifier(Player p)
    {
        verifiers.remove(p);
    }

    private abstract static class GenericVerifier<T>
    {
        private final String errorMessage;

        GenericVerifier(Player p, String inquiry)
        {
            this(p, inquiry, ChatColor.RED + "Invalid input!");
        }

        GenericVerifier(Player p, String inquiry, String errorMessage)
        {
            verifiers.put(p, this);
            if (inquiry != null && inquiry.length() > 0)
            {
                p.sendMessage(inquiry);
            }
            this.errorMessage = errorMessage;
        }

        abstract void parseInput(Player p, String decision);

//        {
//            try
//            {
//                if (decisionParseMethod == String.class)
//                {
//                    performAction(decision);
//                    return;
//                }
//                Method[] methods = decisionParseMethod.getMethods();
//
//                for (Method method : methods)
//                {
//                    if (method.getName().equals("valueOf"))
//                    {
//                        Class[] parameterTypes = method.getParameterTypes();
//                        if (parameterTypes.length == 1 && parameterTypes[0] == String.class)
//                        {
//                            try
//                            {
//                                Object result = method.invoke(decision);
//                                if (result.getClass() == decisionParseMethod)
//                                {
//                                    performAction(decision);
//                                }
//                                else
//                                {
//                                    sendErrorMessage("Error! Got a different class in the return type than the input type!");
//                                }
//                            }
//                            catch (NumberFormatException ex)
//                            {
//                                p.sendMessage(ChatColor.RED + "Invalid input!");
//                            }
//                            return;
//                        }
//                    }
//                }
//
//                performAction(decision);
//            }
//            catch (ClassCastException ex)
//            {
//                ex.printStackTrace();
//            }
//            catch (IllegalAccessException e)
//            {
//                e.printStackTrace();
//            }
//            catch (InvocationTargetException e)
//            {
//                e.printStackTrace();
//            }
//        }

        public abstract void performAction(T decision);

        String getErrorMessage()
        {
            return errorMessage;
        }

        void performFailAction(Player p)
        {
            if (errorMessage != null && errorMessage.length() > 0)
            {
                p.sendMessage(errorMessage);
            }
        }

        private final void acceptInput(Player p, String decision)
        {
            verifiers.remove(p);
            parseInput(p, decision);
        }
    }

    public abstract static class StringVerifier extends GenericVerifier<String>
    {
        protected boolean getBooleanValue(String str)
        {
            return BooleanVerifier.getBooleanValue(str);
        }

        protected Double getDoubleValue(String str)
        {
            return DoubleVerifier.getDoubleValue(str);
        }

        protected Float getFloatValue(String str)
        {
            return FloatVerifier.getFloatValue(str);
        }

        protected Integer getIntValue(String str)
        {
            return IntegerVerifier.getIntValue(str);
        }

        protected Short getShortValue(String str)
        {
            return ShortVerifier.getShortValue(str);
        }

        protected StringVerifier(Player p, String inquiry)
        {
            super(p, inquiry);
        }

        @Override
        void parseInput(Player p, String decision)
        {
            if (decision == null)
            {
                performFailAction(p);
            }
            else
            {
                performAction(decision);
            }
        }

        public abstract void performAction(String decision);
    }

    public abstract static class DoubleVerifier extends GenericVerifier<Double>
    {
        static Double getDoubleValue(String str)
        {
        try
        {
            return str == null ? null : Double.valueOf(str);
        }
        catch (NumberFormatException ex)
        {
            return null;
        }
    }

        protected DoubleVerifier(Player p, String inquiry)
        {
            super(p, inquiry);
        }

        protected DoubleVerifier(Player p, String inquiry, String errorMessage)
        {
            super(p, inquiry, errorMessage);
        }

        @Override
        void parseInput(Player p, String decision)
        {
            Double decisionDouble = getDoubleValue(decision);
            if (decisionDouble == null)
            {
                performFailAction(p);
            }
            else
            {
                performAction(decisionDouble);
            }
        }
    }

    public abstract static class FloatVerifier extends GenericVerifier<Float>
    {
        static Float getFloatValue(String str)
        {
            try
            {
                return str == null ? null : Float.valueOf(str);
            }
            catch (NumberFormatException ex)
            {
                return null;
            }
        }

        protected FloatVerifier(Player p, String inquiry)
        {
            super(p, inquiry);
        }

        protected FloatVerifier(Player p, String inquiry, String errorMessage)
        {
            super(p, inquiry, errorMessage);
        }

        @Override
        void parseInput(Player p, String decision)
        {
            Float decisionFloat = getFloatValue(decision);
            if (decisionFloat == null)
            {
                performFailAction(p);
            }
            else
            {
                performAction(decisionFloat);
            }
        }
    }

    public abstract static class BooleanVerifier extends GenericVerifier<Boolean>
    {
        static boolean getBooleanValue(String str)
        {
            return str != null && Plugin.isTrue(str);
        }

        private static final String YES_STRING = ChatColor.GREEN + "" + ChatColor.BOLD + "Yes";
        private static final String NO_STRING = ChatColor.RED + "" + ChatColor.BOLD + "No";
        private static final InteractiveChat.MessagePart[] yesNoSelector = new InteractiveChat.MessagePart[]
        {
            new InteractiveChat.MessagePart(" ", null, null),
            new InteractiveChat.MessagePart(YES_STRING, null, "Yes"),
            new InteractiveChat.MessagePart("  ", null, null),
            new InteractiveChat.MessagePart(NO_STRING, null, "No")
        };

        protected BooleanVerifier(Player p, String inquiry)
        {
            super(p, inquiry);
            InteractiveChat.sendMessage(p, yesNoSelector);
        }

        @Override
        void parseInput(Player p, String decision)
        {
            performAction(getBooleanValue(decision));
        }

        @Override
        public final void performAction(Boolean decision)
        {
            if (decision)
            {
                performYesAction();
            }
            else
            {
                performNoAction();
            }
        }

        public abstract void performYesAction();

        public abstract void performNoAction();
    }

    public abstract static class ShortVerifier extends GenericVerifier<Short>
    {
        static Short getShortValue(String str)
        {
            try
            {
                return str == null ? null : Short.valueOf(str);
            }
            catch (NumberFormatException ex)
            {
                return null;
            }
        }

        protected ShortVerifier(Player p, String inquiry)
        {
            super(p, inquiry);
        }

        protected ShortVerifier(Player p, String inquiry, String errorMessage)
        {
            super(p, inquiry, errorMessage);
        }

        @Override
        void parseInput(Player p, String decision)
        {
            Short decisionShort = getShortValue(decision);
            if (decisionShort == null)
            {
                performFailAction(p);
            }
            else
            {
                performAction(decisionShort);
            }
        }
    }

    public abstract static class IntegerVerifier extends GenericVerifier<Integer>
    {
        static Integer getIntValue(String str)
        {
            try
            {
                return str == null ? null : Integer.valueOf(str);
            }
            catch (NumberFormatException ex)
            {
                return null;
            }
//        Float decisionFloat = getFloatValue(decision);
//        return decisionFloat == null ? null : decisionFloat.intValue();
        }

        protected IntegerVerifier(Player p, String inquiry)
        {
            super(p, inquiry);
        }

        protected IntegerVerifier(Player p, String inquiry, String errorMessage)
        {
            super(p, inquiry, errorMessage);
        }

        @Override
        void parseInput(Player p, String decision)
        {
            Integer decisionInt = getIntValue(decision);
            if (decisionInt == null)
            {
                performFailAction(p);
            }
            else
            {
                performAction(decisionInt);
            }
        }
    }

    public static class ListVerifier extends IntegerVerifier
    {
        private final List<Possibility> options;

        public abstract static class Possibility
        {
            private final String optionText;

            public Possibility(String optionText)
            {
                this.optionText = optionText;
            }

            public abstract void excercise();
        }

        public abstract static class YesPossibility extends Possibility
        {
            public YesPossibility()
            {
                super(BooleanVerifier.YES_STRING);
            }

            @Override
            public abstract void excercise();
        }

        public abstract static class NoPossibility extends Possibility
        {
            public NoPossibility()
            {
                super(BooleanVerifier.NO_STRING);
            }

            @Override
            public abstract void excercise();
        }

        public ListVerifier(Player p, String inquiry, Possibility[] options)
        {
            this(p, inquiry, Arrays.asList(options));
        }

        protected ListVerifier(Player p, String inquiry, List<Possibility> options)
        {
            super(p, inquiry, ChatColor.RED + "Error! Your options are 1-" + options.size() + ". Please try again.");
            this.options = options;

            int alphabetInt = 1;
            int chatColorInt = 6;
            for (int i = 0; i < options.size(); i++, alphabetInt++, chatColorInt = ((chatColorInt - 5) % 9 + 6))
            {
                InteractiveChat.sendMessage(p, "§" + chatColorInt + "" + ChatColor.BOLD + alphabetInt + ": " + options.get(i).optionText,
                        "§" + chatColorInt + "Click to select", "" + alphabetInt);
            }
        }

        @Override
        public final void performAction(Integer index)
        {
            options.get(index).excercise();
        }

        @Override
        public void performFailAction(Player p)
        {
            super.performFailAction(p);
            verifiers.put(p, this);
        }

        @Override
        void parseInput(Player p, String decision)
        {
            Integer decisionInt = getIntValue(decision);
            if (decisionInt == null || decisionInt < 1 || decisionInt > options.size())
            {
                performFailAction(p);
            }
            else
            {
                performAction(decisionInt - 1);
            }
        }
    }

    public static boolean isVerifier(Player p)
    {
        return verifiers.containsKey(p);
    }

    //    public static void bindVerifier(Player p, GenericVerifier actionsToPerformAfterVerification)
//    {
//        if (actionsToPerformAfterVerification != null)
//        {
//            verifiers.put(p, actionsToPerformAfterVerification);
//        }
//        else
//        {
//            sendErrorMessage(ChatColor.RED + "Error! Tried to bind a null verifier!");
//        }
//    }
//
//    public static void bindVerifier(Player p, GenericVerifier actionsToPerformAfterVerification, String inquiry, boolean sendYesOrNoPrompt)
//    {
//        if (inquiry != null)
//        {
//            p.sendMessage(inquiry);
//        }
//        if (sendYesOrNoPrompt)
//        {
//            sendYesOrNoPrompt(p);
//        }
//        bindVerifier(p, actionsToPerformAfterVerification);
//    }
//
//    public static void bindVerifier(Player p, GenericVerifier actionsToPerformAfterVerification, String inquiry, String[] options)
//    {
//        if (inquiry != null)
//        {
//            p.sendMessage(inquiry);
//        }
//        int alphabetInt = 1;
//        int chatColorInt = 6;
//        for (int i = 0; i < options.length; i++, alphabetInt++, chatColorInt = ((chatColorInt - 5) % 9 + 6))
//        {
//            InteractiveChat.sendMessage(p, "§" + chatColorInt + options[i], "§" + chatColorInt + "Click to select", "" + alphabetInt);
//        }
//        bindVerifier(p, actionsToPerformAfterVerification);
//    }
}
