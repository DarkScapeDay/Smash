package me.happyman.SpecialItems;

import me.happyman.SpecialItems.SpecialItemTypes.*;
import me.happyman.WorldMysteries;
import me.happyman.utils.FileManager;
import me.happyman.utils.Music.MusicElement;
import me.happyman.utils.Music.Note;
import me.happyman.utils.Music.Song;
import me.happyman.utils.Music.UsefulInstrument;
import me.happyman.utils.ParticleEffect;
import me.happyman.utils.ParticlePlayer;
import me.happyman.utils.Verifier;
import me.happyman.worlds.GuiManager;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.SpawnerSpawnEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;
import org.bukkit.util.io.BukkitObjectInputStream;
import org.bukkit.util.io.BukkitObjectOutputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;

import static me.happyman.Plugin.*;
import static me.happyman.worlds.WorldType.getWorldType;

public enum MetaItems //you can change enum entry name, but not item name
{
    MYSTERY_ROCKET(1f, new SpecialItemWithCharge(new UsefulItemStack(Material.FIREWORK, ChatColor.GREEN + "" + ChatColor.BOLD + "Mystery Rocket",
            ChatColor.GRAY + "" + ChatColor.ITALIC + "You might want to be next\nto a body of water before\n use.")/*, new SpecialItem[] {}*/,
            0.01f, 0.05f, SpecialItemWithCharge.ChargingMode.CHARGE_AUTOMATICALLY)
    {
        private final ParticleEffect.OrdinaryColor[] rocketColors = new ParticleEffect.OrdinaryColor[]
        {
            ParticlePlayer.getParticleColor(255, 0, 0),
            ParticlePlayer.getParticleColor(255, 10, 10),
            ParticlePlayer.getParticleColor(255, 10, 40),
            ParticlePlayer.getParticleColor(200, 40, 70)
        };

        @Override
        public boolean performHeldLandAction(Player p)
        {
            super.performHeldLandAction(p);
            return true;
        }

        @Override
        public void performRightClickAction(final Player p, Block blockClicked)
        {
            super.performRightClickAction(p, blockClicked);
            p.playSound(p.getLocation(), Sound.ENTITY_BLAZE_AMBIENT, 0.1f, 0.1f);
            final Vector newV = p.getVelocity().add(new Vector(0f, 0.3f, 0f));
            Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
            {
                public String call()
                {
                    p.setVelocity(newV);
                    Vector currentLocation = p.getLocation().toVector();
                    for (float yCoord = 0; yCoord < 4f; yCoord += 0.1f)
                    {
                        float distanceFromCenter = yCoord * 0.5f;
                        float actualDistanceFromCenter = r.nextFloat() * distanceFromCenter;
                        float randomAngleRad = r.nextFloat() * (float) Math.PI * 2f;
                        float xCoord = actualDistanceFromCenter * (float) Math.cos(randomAngleRad);
                        float zCoord = actualDistanceFromCenter * (float) Math.sin(randomAngleRad);

                        ParticlePlayer.playBasicParticle(new Location(p.getWorld(), currentLocation.getX() + xCoord, currentLocation.getY() - yCoord, currentLocation.getZ() + zCoord), rocketColors);
                    }
                    return "";
                }
            });
        }
    }),
    EXPANDED_INV(1f, new SpecialItem(
            new UsefulItemStack(Material.ENDER_CHEST, ChatColor.GRAY + "" + ChatColor.BOLD + "Backpack", ChatColor.GREEN + "Extra Space for your\nhoarding needs!"))
    {
        private final SpecialItem sdf = this;
        class Backpack extends GuiManager.GuiInventory
        {
            private static final String BACKPACK_PATH = "backpack";
            private static final String ITEM_FILE_PREFIX = "item";
            private static final String ITEM_FILE_SUFFIX_WITH_EXTENSION = ".txt";
            private final List<Integer> changedIndeces;

            Backpack(Player p, final int rows)
            {
                super(ChatColor.BLACK + "" + (rows * 9) + "-slot Backpack", false);
                changedIndeces = new ArrayList<Integer>();
                File[] itemsToLoad = getFolder(p).listFiles();
                final int invSize = rows * 9;
                if (itemsToLoad != null && itemsToLoad.length > 0)
                {
                    for (File f : itemsToLoad)
                    {
                        String fileName = f.getName();
                        StringBuilder builder = new StringBuilder();
                        for (int i = ITEM_FILE_PREFIX.length(); i < fileName.length() - ITEM_FILE_SUFFIX_WITH_EXTENSION.length(); i++)
                        {
                            builder.append(fileName.charAt(i));
                        }
                        String indexString = builder.toString();
                        if (indexString != null)
                        {
                            try
                            {
                                int index = Short.valueOf(indexString);
                                if (index < invSize)
                                {
                                    FileInputStream stream = new FileInputStream(f);
                                    BukkitObjectInputStream objectInputStream = new BukkitObjectInputStream(stream);
                                    Object whatWeGot = objectInputStream.readObject();
                                    stream.close();
                                    objectInputStream.close();
                                    if (whatWeGot instanceof ItemStack)
                                    {
                                        addItem((ItemStack) whatWeGot, index);
                                    }
                                    else
                                    {
                                        f.delete();
                                    }
                                }
                            }
                            catch (NumberFormatException ex)
                            {
                                ex.printStackTrace();
                            }
                            catch (IOException ex)
                            {
                                ex.printStackTrace();
                            }
                            catch (ClassNotFoundException ex)
                            {
                                ex.printStackTrace();
                            }
                        }
                    }
                }
                setSize(invSize, false);
            }

            @Override
            protected boolean handleInventoryClick(final Inventory invClicked, final int invSlot, final int guiContentSlot, Player p, final GuiManager.GuiItem guiItem, boolean leftClick, ItemStack currentItem)
            {
                boolean wantedToCancel = super.handleInventoryClick(invClicked, invSlot, guiContentSlot, p, guiItem, leftClick, currentItem);
                if (!changedIndeces.contains(guiContentSlot))
                {
                    changedIndeces.add(guiContentSlot);
                }
                Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
                {
                    public String call()
                    {
                        guiItem.setItem(invClicked.getItem(invSlot));
                        return "";
                    }
                });
                return wantedToCancel;
            }

            private File getFolder(Player p)
            {
                return FileManager.getPlayerDataFolder(p, p.getWorld(), BACKPACK_PATH);
            }

            private File getItemFile(Player p, int index, boolean forceValid)
            {
                return FileManager.getPlayerFile(p, p.getWorld(), BACKPACK_PATH, ITEM_FILE_PREFIX + index + ITEM_FILE_SUFFIX_WITH_EXTENSION, forceValid);
            }

            @Override
            public void handleInventoryClose(Player p)
            {
                super.handleInventoryClose(p);
                ArrayList<GuiManager.GuiItem> invContents = changedIndeces.size() == 0 ? null : getContents();
//                    File[] itemsToDelete = getFolder(p).listFiles();
//                    if (itemsToDelete != null)
//                    {
//                        for (File f : itemsToDelete)
//                        {
//                            if (!f.delete())
//                            {
//                                sendErrorMessage("Failed to delete " + f.getAbsolutePath());
//                            }
//                        }
//                    }
//
//                    for (int i = 0; i < invContents.size(); i++)
//                    {
//                        GuiManager.GuiItem curItem = invContents.get(i);
//                        if (curItem != null && curItem.getSpecialItem().getType() != Material.AIR)
//                        {
//                            File file = getItemFile(p, i);
//                            try
//                            {
//                                FileOutputStream stream = new FileOutputStream(file);
//                                BukkitObjectOutputStream outputStream = new BukkitObjectOutputStream(stream);
//                                outputStream.writeObject(curItem.getSpecialItem());
//                                stream.close();
//                                outputStream.close();
//                            }
//                            catch (IOException e)
//                            {
//                                e.printStackTrace();
//                            }
//                        }
//                    }
                for (Integer index : changedIndeces)
                {
                    File f = getItemFile(p, index, true);
                    GuiManager.GuiItem curItem = invContents.get(index);
                    if (curItem == null || curItem.getItem().getType() == Material.AIR || curItem.getItem().getAmount() == 0)
                    {
                        f.delete();
                    }
                    else
                    {
                        try
                        {
                            FileOutputStream stream = new FileOutputStream(f);
                            BukkitObjectOutputStream outputStream = new BukkitObjectOutputStream(stream);

                            outputStream.writeObject(curItem.getItem());

                            stream.close();
                            outputStream.close();
                        }
                        catch (IOException e)
                        {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        private static final int ROWS_PER_ITEM = 3;

        @Override
        public void performRightClickAction(Player p, Block blockClicked)
        {
            if (isBeingHeld(p))
            {
                int amount = p.getItemInHand().getAmount();
                new Backpack(p, amount * ROWS_PER_ITEM).open(p);
            }
        }
    }),
    KEY_ENETERER(0f, new SpecialItem(
            new UsefulItemStack(Material.BOOK, ChatColor.GRAY + "" + ChatColor.BOLD + "Ragre Bbqr",
            ChatColor.WHITE + "Gur zlfgrevrf bs gur\nhavirefr yvr jvguva."))
    {
        private final Material clickyItem = Material.EMERALD;
        private final int[] mysteryAmounts = WorldMysteries.getMysteryEntryValues();

        class KeyEntryInv extends GuiManager.GuiInventory
        {
            public KeyEntryInv()
            {
                super(coloredName());
                setSize(mysteryAmounts.length, false);
            }

            @Override
            protected boolean handleInventoryClick(final Inventory invClicked, final int invSlot, int guiContentSlot, final Player p, final GuiManager.GuiItem guiItem, boolean leftClick, ItemStack currentItem)
            {
                super.handleInventoryClick(invClicked, invSlot, guiContentSlot, p, guiItem, leftClick, currentItem);
                Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
                {
                    public String call()
                    {
                        guiItem.setItem(invClicked.getItem(invSlot));
                        ItemStack[] newContents = invClicked.getContents();
                        for (int i = 0; i < newContents.length; i++)
                        {
                            ItemStack newItem = newContents[i];
                            if (newItem == null || newItem.getType() != clickyItem || newItem.getAmount() != mysteryAmounts[i])
                            {
                                return "";
                            }
                        }
                        //you did it
                        p.closeInventory();
                        performYouSolvedAllMysteriesAction(p);
                        return "";
                    }
                });
                return false;
            }

            @Override
            public void handleInventoryClose(final Player playerWhoClosedIt)
            {
                super.handleInventoryClose(playerWhoClosedIt);
                giveContentsToPlayer(playerWhoClosedIt);
            }
        }

        private void performYouSolvedAllMysteriesAction(Player p)
        {
            p.sendMessage("Good job. You did it.");
        }

        @Override
        public void performRightClickAction(Player p, Block blockClicked)
        {
            super.performRightClickAction(p, blockClicked);
            new KeyEntryInv().open(p);
        }
    }),
    MINE(1f, new MinePlacer(new MinePlacer.MineItemStack(), ChatColor.BLUE + "Mine placed! Be very careful! Mines are incredibly deadly!", false, 300)),
    WARP_ADDER(1f, new SpecialItem(
            new UsefulItemStack(Material.NAME_TAG, ChatColor.BLUE + "" + ChatColor.BOLD + "Warp Ticket", ChatColor.GOLD + "Use this to increase your\nmax warps by 1"))
    {
        @Override
        public void performRightClickAction(final Player p, Block blockClicked)
        {
            super.performRightClickAction(p, blockClicked);
            new Verifier.BooleanVerifier(p, ChatColor.YELLOW + "Spend this ticket to unlock a warp?")
            {
                @Override
                public void performYesAction()
                {
                    if (getWorldType(p.getWorld()).incrementMaxWarps(p))
                    {
                        p.sendMessage(ChatColor.GREEN + "Unlocked a new warp! You can now create up to " +  getWorldType(p.getWorld()).getMaxWarps(p) + " warps.");
                        removeOne(p);
                    }
                    else
                    {
                        p.sendMessage(ChatColor.GREEN + "You already reached the maximum warps (" +  getWorldType(p.getWorld()).getMaxWarps(p) + "). Congratulations!");
                    }
                }

                @Override
                public void performNoAction()
                {
                    p.sendMessage(ChatColor.RED + "Ticked usage cancelled!");
                }
            };
        }
    }),
    PLAYER_TRACKER(0.5f, new CompassItem(ChatColor.GREEN + "Track the nearest player\n...this is very OP", new CompassItem.TargetMode[]
    {
        new CompassItem.TargetMode()
        {
            @Override
            public CompassItem.Target getTarget(Player targetter)
            {
                float nearestDSquare = -1;
                Player nearest = null;
                for (Player player : targetter.getWorld().getPlayers())
                {
                    if (player != targetter)
                    {
                        float distanceSquare = (float)player.getLocation().distanceSquared(targetter.getLocation());
                        if (nearest == null || distanceSquare < nearestDSquare)
                        {
                            nearestDSquare = distanceSquare;
                            nearest = player;
                        }
                    }
                }
                return nearest == null ? null : new CompassItem.Target(nearest.getLocation(), ChatColor.GOLD + "Targetting " + nearest.getName());
            }
        },
        new CompassItem.TargetMode()
        {
            @Override
            public CompassItem.Target getTarget(Player targetter)
            {
                return new CompassItem.Target(targetter.getWorld().getSpawnLocation());
            }
        }
    })),
    PLAYER_SYNCHRONIZER(1f, new CompanyItem()), //this item is special
    MUSIC_BOX(1f, new SpecialItem(new UsefulItemStack(Material.NOTE_BLOCK, "§5§lM§6§lu§a§ls§4§li§1§lc §8§lB§b§lo§e§lx"))
    {
        class MusicMaker extends GuiManager.GuiInventory
        {
            private final GuiManager.GuiItem MAIN_PAGE_GOOR = new GuiManager.GuiItem(new UsefulItemStack(getItemStack()).setName(ChatColor.GOLD + "" + ChatColor.BOLD + "Select Instrument"))
            {
                @Override
                public void performAction(Player clicker)
                {
                    gotoInstrumentSelectorPage();
                }
            };
            private final Song song;
            private int tickDurationP = MusicElement.DEFAULT_MUSIC_TICK_DURATION;
            public static final int MAX_DELAY = 80;

            private void showFinalPossibilities(final Player p)
            {
                if (song.size() > 0)
                {
                    new Verifier.ListVerifier(p, ChatColor.LIGHT_PURPLE + "What would you like to do now?", new Verifier.ListVerifier.Possibility[]
                    {
                        new Verifier.ListVerifier.Possibility(ChatColor.GREEN + "Playback song")
                        {
                            @Override
                            public void excercise()
                            {
                                if (song.isBeingPlayed())
                                {
                                    new Verifier.BooleanVerifier(p, ChatColor.RED + "This song is currently being played! Stop it?")
                                    {
                                        @Override
                                        public void performYesAction()
                                        {
                                            song.stopPlaying(p);
                                            showFinalPossibilities(p);
                                        }

                                        @Override
                                        public void performNoAction()
                                        {
                                            showFinalPossibilities(p);
                                        }
                                    };
                                }
                                else
                                {
                                    song.play(p, true, new Song.ActionSongEnded()
                                    {
                                        @Override
                                        public void doStuff()
                                        {
                                            showFinalPossibilities(p);
                                        }
                                    });
                                }
                            }
                        },
                        new Verifier.ListVerifier.Possibility(ChatColor.GREEN + "Save song")
                        {
                            @Override
                            public void excercise()
                            {
                                new Verifier.StringVerifier(p, ChatColor.GRAY + "What would you like to call this song?")
                                {
                                    @Override
                                    public void performAction(String decision)
                                    {
                                        song.save(p, decision);
                                    }
                                };
                            }
                        },
                        new Verifier.ListVerifier.Possibility(ChatColor.GREEN + "Forget I ever composed this song!")
                        {
                            @Override
                            public void excercise()
                            {
                                new Verifier.BooleanVerifier(p, ChatColor.YELLOW + "Are you sure you don't want anyone to remember it?")
                                {
                                    @Override
                                    public void performYesAction()
                                    {
                                        p.sendMessage(ChatColor.RED + "Song erased from memory!");
                                    }

                                    @Override
                                    public void performNoAction()
                                    {
                                        showFinalPossibilities(p);
                                    }
                                };
                            }
                        }
                    });
                }
            }

            @Override
            public void handleInventoryClose(final Player playerWhoClosedIt)
            {
                super.handleInventoryClose(playerWhoClosedIt);
                showFinalPossibilities(playerWhoClosedIt);
            }

            MusicMaker()
            {
                super(ChatColor.GRAY + "" + ChatColor.BOLD + "Select instrument");
                this.song = new Song();
                gotoInstrumentSelectorPage();
            }

            private void gotoNoteAdderPage(final UsefulInstrument instrument)
            {
                setTitle(instrument.getDisplayName());
                clearContents(false);
                for (final Note note : Note.values())
                {
                    addItem(new GuiManager.GuiItem(new UsefulItemStack(instrument.getMaterial(), ChatColor.WHITE + "" + ChatColor.BOLD + note.getName()))
                    {
                        @Override
                        public void performAction(Player clicker)
                        {
                            MusicElement newElement = new MusicElement(note, instrument, tickDurationP);
                            newElement.play(clicker, true);
                            song.add(newElement);
                        }
                    }, false);
                    if (note.isAtEndOfOctave())
                    {
                        int breaker = 0;
                        while (getContentsSize() % 9 != 0 && breaker < 9)
                        {
                            breaker++;
                            if (breaker >= 9)
                            {
                                sendErrorMessage("Error! Didn't grow contents fast enough for the note adder in the music box!");
                                break;
                            }
                            addAirItem();
                        }
                    }
                }
                int bottomMid = ((getContentsSize() - 1)/9 + 1)*9 + 4;
                addItem(MAIN_PAGE_GOOR, false, bottomMid - 4);
                //@TODO: add delay changer item (and make delay awesome)
                final GuiManager.GuiItem indictorThing = new GuiManager.GuiItem(new UsefulItemStack(Material.ANVIL, "Delay (0.05-second periods)", tickDurationP))
                {
                    @Override
                    public void performAction(Player clicker) {}
                };
                addItem(new GuiManager.GuiItem(new UsefulItemStack(Material.WOOL, "Decrease", (byte)14))
                {
                    @Override
                    public void performAction(Player clicker)
                    {
                        tickDurationP--;
                        if (tickDurationP < 1)
                        {
                            tickDurationP = 1;
                        }
                        indictorThing.setAmount(tickDurationP, true);
                    }
                }, false, bottomMid - 1);
                addItem(indictorThing, false, bottomMid);
                addItem(new GuiManager.GuiItem(new UsefulItemStack(Material.WOOL, "Increase", (byte)5))
                {
                    @Override
                    public void performAction(Player clicker)
                    {
                        tickDurationP++;
                        if (tickDurationP > MAX_DELAY)
                        {
                            tickDurationP = MAX_DELAY;
                        }
                        indictorThing.setAmount(tickDurationP, true);
                    }
                }, false, bottomMid + 1);
                addItem(new GuiManager.GuiItem(new UsefulItemStack(Material.SKULL_ITEM, ChatColor.AQUA + "Undo", (byte)4))
                {
                    @Override
                    public void performAction(Player clicker)
                    {
                        song.removeLastElement();
                        clicker.playSound(clicker.getLocation(), Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 0.2f, 0.8f);
                    }
                }, false, bottomMid + 3);
                addItem(new GuiManager.GuiItem(new UsefulItemStack(Material.DIAMOND_SWORD, ChatColor.AQUA + "Play current progress"))
                {
                    @Override
                    public void performAction(Player clicker)
                    {
                        song.play(clicker, true);
                    }
                }, false, bottomMid + 4);
                refresh();
            }

            private void gotoInstrumentSelectorPage()
            {
                setTitle(ChatColor.GRAY + "" + ChatColor.BOLD + "Select instrument");
                clearContents(false);
                for (final UsefulInstrument instrument : UsefulInstrument.values())
                {
                    addItem(new GuiManager.GuiItem(new UsefulItemStack(instrument.getMaterial(), instrument.getDisplayName()))
                    {
                        @Override
                        public void performAction(Player clicker)
                        {
                            gotoNoteAdderPage(instrument);
                        }
                    }, false);
                }
                refresh();
            }
        }

        @Override
        public void performRightClickAction(Player p, Block blockClicked)
        {
            super.performRightClickAction(p, blockClicked);
            new MusicMaker().open(p);
        }
    }),
    MOB_SPAWNER(1f, new MonsterSpawner()),
    BREAD_THAT_CANT_BE_SOLD(0f, new SpecialItem(new UsefulItemStack(Material.BREAD, ChatColor.GRAY + "" + ChatColor.BOLD + "Noob Bread"))
    {
        @Override
        public boolean canBeSold()
        {
            return false;
        }
    }),
    DEATH_LOCATION_PEEKER(0f, new SpecialItem(new UsefulItemStack(Material.BONE, ChatColor.GREEN + "" + ChatColor.BOLD + "Death Location Seer"))
    {
        @Override
        public void performRightClickAction(Player p, Block blockClicked)
        {
            //@TODO: make this work
            super.performRightClickAction(p, blockClicked);
        }
    });

    private static final String GIVE_SPECIAL_CMD = "givespecial";
    static
    {
        setExecutor(GIVE_SPECIAL_CMD,
                new CommandExecutor()
                {
                    @Override
                    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
                    {
                        if (args.length < 2)
                        {
                            return false;
                        }
                        Player whoToGiveTo = Bukkit.getPlayer(args[0]);
                        if (whoToGiveTo == null)
                        {
                            sender.sendMessage(ChatColor.RED + "Player " + args[0] + " not found.");
                        }
                        else
                        {
                            StringBuilder builder = new StringBuilder();
                            int i = 1;
                            for (; i < args.length - 1; i++)
                            {
                                builder.append(args[i]);
                                builder.append("_");
                            }
                            builder.append(args[i]);
                            String nameComp = builder.toString();

                            Iterator<SpecialItem> specialItemIterator = getWorldType(whoToGiveTo).getSpecialItemIterator(whoToGiveTo);
                            boolean all = nameComp.equalsIgnoreCase("all");

                            if (!all)
                            {
                                while (specialItemIterator.hasNext())
                                {
                                    SpecialItem specialItem = specialItemIterator.next();
                                    if (specialItem.colorlessName().replaceAll(" ", "_").equals(nameComp))
                                    {
                                        sender.sendMessage(specialItem.give(whoToGiveTo) ? (ChatColor.GREEN + "Gave " + whoToGiveTo.getName() + " a(n) " + specialItem.coloredName() + ChatColor.RESET + "" + ChatColor.GREEN + ".") :
                                                (ChatColor.RED + whoToGiveTo.getName() + " doesn't have enough space for " + specialItem.coloredName() + "."));
                                        return true;
                                    }
                                }
                                sender.sendMessage(ChatColor.RED + "Item " + nameComp + " not found.");
                            }
                            else
                            {
                                while (specialItemIterator.hasNext())
                                {
                                    specialItemIterator.next().give(whoToGiveTo);
                                }
                                sender.sendMessage(ChatColor.GREEN + "Gave " + whoToGiveTo.getName() + " all special items (at least all for which he had space).");
                            }
                        }
                        return true;
                    }
                },
                new TabCompleter()
                {
                    @Override
                    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args)
                    {
                        ArrayList<String> completes = new ArrayList<String>();
                        if (args.length == 1)
                        {
                            for (Player p : Bukkit.getOnlinePlayers())
                            {
                                if (p.getName().toLowerCase().startsWith(args[0].toLowerCase()))
                                {
                                    completes.add(p.getName());
                                }
                            }
                        }
                        else if (args.length > 1 )
                        {
                            Player who = Bukkit.getPlayer(args[0]);
                            if (who == null && sender instanceof Player)
                            {
                                who = (Player)sender;
                            }

                            StringBuilder builder = new StringBuilder();
                            int i = 1;
                            for (; i < args.length - 1; i++)
                            {
                                builder.append(args[i]);
                                builder.append(" ");
                            }
                            builder.append(args[i]);
                            String nameComp = builder.toString();

                            Iterator<SpecialItem> specialItemIterator = getWorldType(who).getSpecialItemIterator(who);
                            while (specialItemIterator.hasNext())
                            {
                                SpecialItem specialItem = specialItemIterator.next();
                                if (specialItem.colorlessName().toLowerCase().startsWith(nameComp.toLowerCase()))
                                {
                                    completes.add(specialItem.colorlessName().replaceAll(" ", "_"));
                                }
                            }
                            if (completes.size() > 0)
                            {
                                completes.add(0, "");
                            }
                        }
                        return completes;
                    }
                });
    }

    private static class MyStaticData
    {
        private static float totalChance = 0f;
    }
    private static final Random r = new Random();
    private final SpecialItem specialItem;
    private final float chanceThreshold;
    private final float spawnChance;

    MetaItems(float spawnChance, SpecialItem specialItem)
    {
        this.specialItem = specialItem;
        MyStaticData.totalChance += spawnChance;
        this.spawnChance = spawnChance;
        this.chanceThreshold = MyStaticData.totalChance;
    }

    public SpecialItem getSpecialItem()
    {
        return specialItem;
    }

    public static SpecialItem getSpecialItem(LivingEntity p, ItemStack item)
    {
        return getWorldType(p.getWorld()).getSpecialItem(p, item);
    }

    public static SpecialItem getHeldSpecialItem(LivingEntity p)
    {
        return getWorldType(p.getWorld()).getHeldSpecialItem(p);
    }

    public static MetaItems getRandomSpecialItem()
    {
        float randomFloat = r.nextFloat() * MyStaticData.totalChance;
        for (MetaItems specialItemWithSpawnChance : values())
        {
            if (randomFloat <= specialItemWithSpawnChance.chanceThreshold)
            {
                return specialItemWithSpawnChance;
            }
        }
        return values().length > 0 ? values()[0] : null;
    }

    public float getSpawnChance()
    {
        return spawnChance;
    }

    public static void initialize() {}
}
