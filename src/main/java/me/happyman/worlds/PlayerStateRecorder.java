package me.happyman.worlds;


import me.happyman.utils.FileManager;
import org.bukkit.*;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Iterator;

import static me.happyman.Plugin.*;
import static me.happyman.utils.FileManager.getGeneralPlayerFile;
import static me.happyman.utils.FileManager.getWorldFile;
import static me.happyman.worlds.WorldManager.isBeingDeleted;
import static me.happyman.worlds.WorldType.getWorldType;

public class PlayerStateRecorder
{
    private static class PlayerState
    {
        //These fields actually are secretly used
        private final ItemStack[] items;
        private final ItemStack[] armor;
        private final Collection<PotionEffect> effects;
        private final Location bedLocation;
        private final GameMode gamemode;

        private final Float exp;
        private final Integer level;
        private final Float exhaustion;
        private final Double health;
        private final Float saturation;
        private final Integer foodLevel;
        private final Boolean isFlying;

        protected PlayerState(Player p)
        {
            items = p.getInventory().getContents();
            armor = p.getInventory().getArmorContents();
            effects = p.getActivePotionEffects();
            bedLocation = p.getBedSpawnLocation();
            gamemode = p.getGameMode();

            //Number data
            exp = p.getExp();
            level = p.getLevel();
            exhaustion = p.getExhaustion();
            health = p.getHealth();
            saturation = p.getSaturation();
            foodLevel = p.getFoodLevel();
            isFlying = p.isFlying();
        }

        public static void giveStarterInventory(Player p)
        {
            p.getInventory().clear();
            getWorldType(p.getWorld()).giveStarterItems(p);
        }

        private static abstract class WhatToDoIfFail
        {
            abstract void doFailureRoutine(Player p);
        }

        private static void loadComplexData(Player p, World w, String loadingFrom, Object dataToSet, Method setDataMethod, WhatToDoIfFail failure)
        {
            try
            {
                File f = FileManager.getPlayerFile(p, w, loadingFrom + ".txt");
                if (f.exists())
                {
                    if (setDataMethod != null && dataToSet != null)
                    {
                        FileInputStream inputStream = new FileInputStream(f);
                        BukkitObjectInputStream stream = new BukkitObjectInputStream(inputStream);
                        Object result = stream.readObject();

                        setDataMethod.invoke(dataToSet, result);
                        inputStream.close();
                        stream.close();
                        return;
                    }
                }
                else
                {
                    throw new EOFException("Resetting your data because file was not exist");
                }
            }
            catch (EOFException ex)
            {
                //do nothing
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            catch (ClassNotFoundException e)
            {
                e.printStackTrace();
            }
            catch (IllegalAccessException e)
            {
                e.printStackTrace();
            }
            catch (InvocationTargetException e)
            {
                e.printStackTrace();
            }
            catch (IllegalArgumentException e)
            {
                e.printStackTrace();
            }

            if (failure != null)
            {
                failure.doFailureRoutine(p);
            }
        }

        public static File getFastPlayerDataFile(Player p, World w)
        {
            return FileManager.getPlayerFile(p, w, "", "SimpleData.json");
        }

        public static void loadData(final Player p, final World w)
        {
//        for (Method m : p.getClass().getDeclaredMethods())
//        {
//            if (m.getDisplayName().startsWith("set"))
            try
            {
                loadComplexData(p, w, "items",
                        p.getInventory(), p.getInventory().getClass().getMethod("setContents", ItemStack[].class),
                        new WhatToDoIfFail()
                        {
                            void doFailureRoutine(Player p)
                            {
                                giveStarterInventory(p);
                            }
                        }
                );

                loadComplexData(p, w, "armor",
                        p.getInventory(), p.getInventory().getClass().getMethod("setArmorContents", ItemStack[].class),
                        new WhatToDoIfFail() {
                            @Override
                            void doFailureRoutine(Player p)
                            {
                                p.getInventory().setArmorContents(new ItemStack[] {new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR), new ItemStack(Material.AIR)});
                            }
                        }
                );

                for (PotionEffect effect : p.getActivePotionEffects())
                {
                    p.removePotionEffect(effect.getType());
                }
                loadComplexData(p, w, "effects",
                        p, p.getClass().getMethod("addPotionEffects", Collection.class),
                        new WhatToDoIfFail() {
                            @Override
                            void doFailureRoutine(Player p) {
                                p.getActivePotionEffects().clear();
                            }
                        }
                );

                loadComplexData(p, w, "bedLocation",
                        p, p.getClass().getMethod("setBedSpawnLocation", Location.class),
                        new WhatToDoIfFail()
                        {
                            @Override
                            void doFailureRoutine(Player p)
                            {
                                p.setBedSpawnLocation(w.getSpawnLocation());
                            }
                        }
                );

                loadComplexData(p, w, "gamemode",
                        p, p.getClass().getMethod("setGameMode", GameMode.class),
                        new WhatToDoIfFail() {
                            @Override
                            void doFailureRoutine(Player p)
                            {
                                GameMode defaultGamemode = getWorldType(w).getDefaultGamemode();
                                if (p.getGameMode() != defaultGamemode)
                                {
                                    p.setGameMode(defaultGamemode);
                                }
                            }
                        }
                );

                File simpleFile = getFastPlayerDataFile(p, w);
                FileReader simpleFileReader = new FileReader(simpleFile);
                JSONObject object = (JSONObject)new JSONParser().parse(simpleFileReader);
                simpleFileReader.close();

                try
                {
                    p.setExp(Float.parseFloat("" + object.get("exp")));
                } catch (NumberFormatException ex) { p.setExp(0); }

                try
                {
                    p.setLevel(Integer.parseInt("" + object.get("level")));
                } catch (NumberFormatException ex) { p.setLevel(0); }

                try
                {
                    p.setExhaustion(Float.parseFloat("" + object.get("exhaustion")));
                } catch (NumberFormatException ex) { p.setExhaustion(0); }

                try
                {
                    p.setHealth(Double.parseDouble("" + object.get("health")));
                } catch (NumberFormatException ex) { p.setHealth(20); }

                try
                {
                    p.setSaturation(Float.parseFloat("" + object.get("saturation")));
                } catch (NumberFormatException ex) { p.setSaturation(20); }


                try
                {
                    p.setFoodLevel(Integer.parseInt("" + object.get("foodLevel")));
                } catch (NumberFormatException ex) { p.setFoodLevel(20); }

                try
                {
                    p.setFlying(Boolean.parseBoolean("" + object.get("isFlying")));
                }
                catch (NumberFormatException ex) { p.setFlying(false); }
                catch (IllegalArgumentException ex) {}
            }
            catch (NoSuchMethodException e)
            {
                e.printStackTrace();
            }
            catch (ParseException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }

        private static boolean dataIsSimple(Object value)
        {
            return value instanceof Number || value instanceof Boolean;
        }

        public static void forgetData(Player p, World w)
        {
            FileManager.deleteFile(FileManager.getPlayerDataFolder(p, w));
        }

        public static void forgetData(Player p, World w, String fieldName)
        {
            File f = FileManager.getPlayerFile(p, w, fieldName + ".txt");
            if (f.exists())
            {
                f.delete();
            }
        }

        public static void saveData(Player p, World w)
        {
            if (!p.isDead() && !isBeingDeleted(w) && getWorldType(w).remembersPlayerStates(p))
            {
                //save data
                PlayerState state = new PlayerState(p);
                for (Field field : state.getClass().getDeclaredFields())
                {
                    try
                    {
                        Object value = field.get(state);
                        if (dataIsSimple(value))
                        {
                            File f = getFastPlayerDataFile(p, w);
                            FileReader simpleFileReader = new FileReader(f);
                            JSONObject jObj = (JSONObject)new JSONParser().parse(simpleFileReader);
                            simpleFileReader.close();

                            jObj.put(field.getName(), value);

                            PrintWriter writer = new PrintWriter(f);
                            jObj.writeJSONString(writer);
                            writer.close();
                        }
                        else
                        {
                            File f = FileManager.getPlayerFile(p, w, field.getName() + ".txt");
                            FileOutputStream outputStream = new FileOutputStream(f);
                            BukkitObjectOutputStream serializer = new BukkitObjectOutputStream(outputStream);
                            serializer.writeObject(value);
                            serializer.close();
                            outputStream.close();
                        }

                    }
                    catch (IllegalAccessException e)
                    {
                        e.printStackTrace();
                    }
                    catch (IOException e)
                    {
                        e.printStackTrace();
                    }
                    catch (ParseException e)
                    {
                        e.printStackTrace();
                    }
                }

                //save location
                File locationFile = getLocFile(w);
                try
                {
                    FileReader reader = new FileReader(locationFile);
                    JSONObject object = (JSONObject)new JSONParser().parse(reader);
                    reader.close();

                    Location l = p.getLocation();
                    Iterator<PortalManager.CuboidPortal> portalIterator = PortalManager.portalIterator();
                    while (portalIterator.hasNext())
                    {
                        PortalManager.CuboidPortal portal = portalIterator.next();
                        if (portal.contains(l))
                        {
                            l = portal.getEntrancePoint();
                            break;
                        }
                    }
                    object.put(p.getName(), l.getX() + " " + l.getY() + " " + l.getZ() + " " + l.getYaw() + " " + l.getPitch());

                    PrintWriter writer = new PrintWriter(locationFile);
                    object.writeJSONString(writer);
                    writer.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
                catch (ParseException e)
                {
                    locationFile.delete();
                }

                FileManager.putData(getGeneralPlayerFile(p), PLAYER_LAST_WORLD_DATANAME, w.getName());
            }
        }
    }


    public static final int AUTOSAVE_INTERVAL = 20*150;

    static
    {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(getPlugin(), new Runnable()
        {
            public void run()
            {
                rememberAllPlayerStates();
            }

        }, AUTOSAVE_INTERVAL, AUTOSAVE_INTERVAL);
    }

    private static final String PLAYER_LAST_WORLD_DATANAME = "Lastworld";

    public static World getLastWorld(Player p)
    {
        String worldName = FileManager.getData(getGeneralPlayerFile(p), PLAYER_LAST_WORLD_DATANAME);
        if (isBeingDeleted(worldName))
        {
            return getFallbackWorld();
        }
        else if (worldName != null && worldName.length() > 0)
        {
            return Bukkit.getWorld(worldName);
        }
        return null;
    }

    private static File getLocFile(World w)
    {
        return getWorldFile(w, "PlayerLocations.json");
    }

    public static void loadPlayerState(final Player p, final World w)
    {
        PlayerState.loadData(p, w);
    }

    public static void forgetPlayerState(Player p, World w)
    {
        PlayerState.forgetData(p, w);
        try
        {
            File locationFile = getLocFile(w);
            FileReader reader = new FileReader(locationFile);
            JSONObject object = (JSONObject)new JSONParser().parse(reader);
            reader.close();

            if (object.containsKey(p.getName()))
            {
                object.remove(p.getName());
            }

            PrintWriter writer = new PrintWriter(locationFile);
            object.writeJSONString(writer);
            writer.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (ParseException e)
        {
            e.printStackTrace();
        }

        FileManager.removeEntryWithKey(FileManager.getGeneralPlayerFile(p), PLAYER_LAST_WORLD_DATANAME);
    }

    public static void rememberAllPlayerStates()
    {
        for (Player p : Bukkit.getOnlinePlayers())
        {
            rememberPlayerState(p, p.getWorld());
        }
    }

    public static void rememberPlayerState(Player p, World w)
    {
        PlayerState.saveData(p, w);
    }

    public static Location getLastLocationInWorld(Player p, World w)
    {
        if (w == null || p == null)
        {
            return null;
        }

        if (getWorldType(w).remembersPlayerStates(p))
        {
            File f = getLocFile(w);
            try
            {
                FileReader reader = new FileReader(f);
                JSONObject object = (JSONObject)new JSONParser().parse(reader);
                reader.close();

                String value = (String)object.get(p.getName());
                if (value != null)
                {
                    String[] result = value.split(" ");
                    float[] coords = new float[5];
                    if (result.length == coords.length)
                    {
                        for (int i = 0; i < result.length; i++)
                        {
                            coords[i] = Float.parseFloat(result[i]);
                        }
                        Location l = new Location(w, coords[0], coords[1], coords[2], coords[3], coords[4]);
                        if (l.getBlock().getType().equals(Material.LAVA)
                                || l.getBlock().getRelative(0, 1, 0).getType().isSolid() || l.getBlock().getRelative(0, 1, 0).getType().equals(Material.LAVA))
                        {
                            return w.getHighestBlockAt(l).getLocation().add(0.5, 0, 0.5);
                        }
                        return l;
                    }
                    else
                    {
                        sendErrorMessage("Error when trying to find player " + p.getName() + "'s last location in world " + w.getName() + "! Bad coord format!");
                    }
                }
            }
            catch (NumberFormatException ex)
            {
                ex.printStackTrace();
            }
            catch (ParseException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        return new Location(w, w.getSpawnLocation().getX() + 0.5, w.getSpawnLocation().getY(), w.getSpawnLocation().getZ() + 0.5);
    }
}
