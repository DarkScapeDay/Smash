package me.happyman.SpecialItems.SpecialItemTypes;

import com.sun.istack.internal.NotNull;
import me.happyman.SpecialItems.SpecialItem;
import me.happyman.SpecialItems.UsefulItemStack;
import me.happyman.WorldGenerationTools;
import me.happyman.utils.FileManager;
import me.happyman.utils.InventoryManager;
import me.happyman.worlds.UUIDFetcher;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.io.File;
import java.util.*;
import java.util.concurrent.Callable;

import static me.happyman.Plugin.*;
import static me.happyman.worlds.WorldType.getWorldType;

public abstract class PlaceableItem extends SpecialItem implements Listener
{
    private final String placedBlockFileName;
    private final Map<World, List<Integer>> takenNumbers = new HashMap<World, List<Integer>>();
    private static final Map<World, List<PlacedBlock>> knownMines = new HashMap<World, List<PlacedBlock>>();
    private static final List<Material> knownMineTypes = new ArrayList<Material>();
    private final Material[] placedBlockMaterials;
    private static final Random random = new Random();
    private final String placementMessage;
    private final boolean blocksAreGoThrough;
    private static final Set<Material> MATERIALS_THAT_CAN_BECOME_BLOCK = new HashSet<Material>();
    static
    {
        MATERIALS_THAT_CAN_BECOME_BLOCK.add(Material.AIR);
        MATERIALS_THAT_CAN_BECOME_BLOCK.add(Material.LONG_GRASS);
        MATERIALS_THAT_CAN_BECOME_BLOCK.add(Material.YELLOW_FLOWER);
        MATERIALS_THAT_CAN_BECOME_BLOCK.add(Material.RED_ROSE);
        MATERIALS_THAT_CAN_BECOME_BLOCK.add(Material.DEAD_BUSH);
        MATERIALS_THAT_CAN_BECOME_BLOCK.add(Material.SNOW);
        MATERIALS_THAT_CAN_BECOME_BLOCK.add(Material.WEB);
        MATERIALS_THAT_CAN_BECOME_BLOCK.add(Material.VINE);
        MATERIALS_THAT_CAN_BECOME_BLOCK.add(Material.LADDER);
        MATERIALS_THAT_CAN_BECOME_BLOCK.add(Material.LADDER);
        MATERIALS_THAT_CAN_BECOME_BLOCK.add(Material.STONE_PLATE);
        MATERIALS_THAT_CAN_BECOME_BLOCK.add(Material.WOOD_PLATE);
        MATERIALS_THAT_CAN_BECOME_BLOCK.add(Material.WATER);
        MATERIALS_THAT_CAN_BECOME_BLOCK.add(Material.STATIONARY_WATER);
        MATERIALS_THAT_CAN_BECOME_BLOCK.add(Material.LAVA);
        MATERIALS_THAT_CAN_BECOME_BLOCK.add(Material.STATIONARY_LAVA);
        MATERIALS_THAT_CAN_BECOME_BLOCK.add(Material.TORCH);
        MATERIALS_THAT_CAN_BECOME_BLOCK.add(Material.REDSTONE);
        MATERIALS_THAT_CAN_BECOME_BLOCK.add(Material.REDSTONE_TORCH_ON);
        MATERIALS_THAT_CAN_BECOME_BLOCK.add(Material.REDSTONE_TORCH_OFF);
    }

    static
    {
        Listener placeableItemListener = new Listener()
        {
            @EventHandler
            public void handleBlockBreak(BlockBreakEvent event)
            {
                if (!event.isCancelled())
                {
                    Block blockBroken = event.getBlock();
                    Player player = event.getPlayer();
                    PlacedBlock block = getKnownBlock(blockBroken);
                    if (block != null)
                    {
                        PlaceableItem owner = block.getOwner();
                        if (owner.performBrokenAction(block, player))
                        {
                            event.setCancelled(true);
                        }
                        else
                        {
                            forgetBlock(block);
                        }
                    }
                }
            }
            @EventHandler
            public void playerSteppedOnIt(PlayerInteractEvent event)
            {
                if (event.getAction() == Action.PHYSICAL)
                {
                    PlacedBlock block = getKnownBlock(event.getClickedBlock());
                    if (block != null)
                    {
                        if (block.getOwner().performSteppedOn(block, event.getPlayer()))
                        {
                            event.setCancelled(true);
                        }
                        else
                        {
                            forgetBlock(block);
                        }
                    }
                }
            }
            @EventHandler
            public void creatureWasDumb(EntityInteractEvent event)
            {
                if (!(event.getEntity() instanceof Player) && event.getEntity() instanceof LivingEntity)
                {
                    PlacedBlock block = getKnownBlock(event.getEntity().getLocation().getBlock());
                    if (block != null)
                    {
                        if (!block.getOwner().performSteppedOn(block, (LivingEntity)event.getEntity()))
                        {
                            forgetBlock(block);
                        }
                    }
                }
            }
            @EventHandler
            public void hitMineProjEvent(final ProjectileHitEvent event)
            {
                if (event.getEntity().getShooter() instanceof LivingEntity)
                {
                    final LivingEntity shooter = (LivingEntity)event.getEntity().getShooter();

                    Block hitBlock = event.getHitBlock();
                    for (PlacedBlock knownBlock : getKnownBlocks(event.getEntity().getWorld()))
                    {
                        if (isKnownBlock(hitBlock, knownBlock))
                        {
                            if (!knownBlock.getOwner().performHitByProjectileEvent(knownBlock, shooter))
                            {
                                forgetBlock(knownBlock);
                            }
                            break;
                        }
                        else
                        {
                            Bukkit.getScheduler().callSyncMethod(getPlugin(), new Callable()
                            {
                                public String call()
                                {
                                    PlacedBlock arrowLocationBlock = getKnownBlock(event.getEntity().getLocation().getBlock());
                                    if (arrowLocationBlock != null && arrowLocationBlock.getOwner().blocksAreGoThrough())
                                    {
                                        if (!arrowLocationBlock.getOwner().performHitByProjectileEvent(arrowLocationBlock, shooter))
                                        {
                                            forgetBlock(arrowLocationBlock);
                                        }
                                    }
                                    return "";
                                }
                            });
                        }
                    }
                }
            }
        };

        Bukkit.getPluginManager().registerEvents(placeableItemListener, getPlugin());
    }


    @Override
    public final void performLoadWorldAction(World world)
    {
        super.performLoadWorldAction(world);
        File f = getMineLocationFile(world, false);
        if (f.exists())
        {
            for (FileManager.Entry entry : FileManager.getAllEntries(f))
            {
                PlacedBlock placedBlock = deserialize(world, entry.getKey(), entry.getValue());

                if (placedBlock == null)
                {
                    sendErrorMessage("Error! Could not get proper data from " + placedBlockFileName + " in " + world.getName());
                }
                else
                {
                    cacheBlock(world, placedBlock);
                }
            }
        }
    }

    public PlaceableItem(UsefulItemStack item, String placementMessage, String placedBlockFileName)
    {
        this(item, placementMessage, placedBlockFileName, null);
    }

    public PlaceableItem(UsefulItemStack item, String placementMessage, String placedBlockFileName, Material[] alternativeBlockMaterials)
    {
        super(item);
        this.placementMessage = placementMessage;
        this.blocksAreGoThrough = MATERIALS_THAT_CAN_BECOME_BLOCK.contains(item.getType());
        if (!placedBlockFileName.endsWith(".json"))
        {
            int dotIndex = placedBlockFileName.lastIndexOf('.');
            if (dotIndex != -1)
            {
                placedBlockFileName = placedBlockFileName.substring(0, dotIndex);
            }
            placedBlockFileName = placedBlockFileName + ".json";
        }
        this.placedBlockFileName = placedBlockFileName;
        if (alternativeBlockMaterials == null)
        {
            alternativeBlockMaterials = new Material[] {};
        }
        this.placedBlockMaterials = new Material[alternativeBlockMaterials.length + 1];
        this.placedBlockMaterials[0] = item.getType();
        for (int pb = 1, alt = 0; pb < placedBlockMaterials.length && alt < alternativeBlockMaterials.length; pb++, alt++)
        {
            this.placedBlockMaterials[pb] = alternativeBlockMaterials[alt];
        }
        for (Material mat : placedBlockMaterials)
        {
            if (!knownMineTypes.contains(mat))
            {
                knownMineTypes.add(mat);
            }
        }
    }

    protected class PlacedBlock
    {
        private final PlaceableItem owner;
        private final Block block;
        private final String culplritUUID;
        private final Material originalMaterial;
        private final int number;
        private final Object additionalData;

        private PlacedBlock(Block block, Material originalMaterial, Player culprit, int number, Object additionalData)
        {
            this(block, originalMaterial, culprit.getName(), number, additionalData);
        }

        private PlacedBlock(Block block, Material originalMaterial, String placer, int number, Object additionalData)
        {
            this.owner = PlaceableItem.this;
            UUIDFetcher.PlayerID who = UUIDFetcher.getUUIDAndCapitalName(placer);
            this.block = block;
            this.culplritUUID = who.getUUID();
            this.originalMaterial = originalMaterial;
            this.number = number;
            this.additionalData = additionalData;
        }

        protected PlaceableItem getOwner()
        {
            return owner;
        }

        private void setToOriginalMaterial()
        {
            block.setType(originalMaterial == null ? Material.AIR : originalMaterial);
        }

        private int getNumber()
        {
            return number;
        }

        public Block getBlock()
        {
            return block;
        }

        public Location getLocation()
        {
            return block.getLocation();
        }

        public World getWorld()
        {
            return block.getWorld();
        }

        public String getPlacerName()
        {
            return UUIDFetcher.getCapitalName(culplritUUID);
        }

        protected Object getAdditionalData()
        {
            return additionalData;
        }

        private String serialize(Player placer)
        {
            StringBuilder dataBuilder = new StringBuilder();
            dataBuilder.append(getCoords());
            dataBuilder.append(" ");
            dataBuilder.append(culplritUUID);
            dataBuilder.append(" ");
            dataBuilder.append(originalMaterial == null ? "null" : originalMaterial.name());
            String[] additionalData = owner.serializeAdditionalData(placer);
            if (additionalData != null)
            {
                for (String s : additionalData)
                {
                    dataBuilder.append(" ");
                    dataBuilder.append(s == null ? '\0' : s);
                }
            }
            return dataBuilder.toString();
        }

        public String getCoords()
        {
            return String.format("%1$d %2$d %3$d", block.getX(), block.getY(), block.getZ());
        }
    }

    private PlacedBlock deserialize(World world, String key, String value)
    {
        String[] parts = value.split(" ");
        if (parts.length >= 5)
        {
            try
            {
                Block block = world.getBlockAt(Integer.valueOf(parts[0]), Integer.valueOf(parts[1]), Integer.valueOf(parts[2]));

                String[] lastParts = new String[parts.length - 5];
                for (int lastI = 0, firstI = 5; lastI < lastParts.length && firstI < parts.length; lastI++, firstI++)
                {
                    lastParts[lastI] = parts[firstI];
                }

                return new PlacedBlock(block, Material.getMaterial(parts[4]), parts[3], Integer.valueOf(key), deserializeAdditionalData(lastParts));
            }
            catch (NumberFormatException ex)
            {}
        }
        return null;
    }

    protected String[] serializeAdditionalData(Player placer)
    {
        return null;
    }

    protected Object deserializeAdditionalData(String[] additionalDatas)
    {
        return null;
    }

    private Object getAdditionalData(Player placer)
    {
        return deserializeAdditionalData(serializeAdditionalData(placer));
    }

    public static void clearAllBlocks(World w)
    {
        List<PlacedBlock> placedBlocks = knownMines.get(w);
        if (placedBlocks != null)
        {
            List<File> filesToBeDeleted = new ArrayList<File>();
            for (PlacedBlock b : new ArrayList<PlacedBlock>(placedBlocks))
            {
                PlaceableItem item = b.getOwner();
                b.setToOriginalMaterial();
                b.getOwner().performMiscClearanceAction(b);
                File pertainentFile = item.getMineLocationFile(w, false);
                if (pertainentFile.exists() && !filesToBeDeleted.contains(pertainentFile))
                {
                    filesToBeDeleted.add(pertainentFile);
                }
            }
            for (File f : filesToBeDeleted)
            {
                FileManager.deleteFile(f);
            }
            knownMines.remove(w);
        }
    }

    private static void forgetBlock(@NotNull PlacedBlock block)
    {
        List<Integer> knownNumbers = block.getOwner().takenNumbers.get(block.getWorld());
        if (knownNumbers != null)
        {
            knownNumbers.remove((Integer)block.getNumber());
        }

        List<PlacedBlock> listOfBlocks = knownMines.get(block.getWorld());
        if (listOfBlocks != null)
        {
            listOfBlocks.remove(block);
            if (listOfBlocks.size() == 0)
            {
                knownMines.remove(block.getWorld());
            }
        }
        FileManager.removeEntryWithKey(block.getOwner().getMineLocationFile(block.getWorld(), true), "" + block.getNumber());
    } //return true if the block should actually be blown up


    public static void performExploded(Block b)
    {
        PlacedBlock block = getKnownBlock(b);
        if (block != null)
        {
            forgetBlock(block);
            block.getOwner().performMiscClearanceAction(block);
        }
    }
    protected abstract void performMiscClearanceAction(@NotNull PlacedBlock block);



    protected abstract boolean performBrokenAction(@NotNull PlacedBlock block, Player player); //return true if breaking should be cancelled and remember the block


    protected abstract boolean performSteppedOn(@NotNull PlacedBlock block, LivingEntity who); //return true if event should remember the block


    protected abstract boolean performHitByProjectileEvent(@NotNull PlacedBlock block, LivingEntity shooter); //return true if event should remember the block




    private boolean blocksAreGoThrough()
    {
        return blocksAreGoThrough;
    }

    private File getMineLocationFile(World world, boolean forceValid)
    {
        return FileManager.getWorldFile(world, "", placedBlockFileName, forceValid);
    }

    private void cacheBlock(World world, PlacedBlock block)
    {
        List<Integer> knownNumbers = takenNumbers.get(world);
        if (knownNumbers == null)
        {
            knownNumbers = new ArrayList<Integer>();
            takenNumbers.put(world, knownNumbers);
        }
        knownNumbers.add(block.getNumber());

        List<PlacedBlock> knownBlocks = knownMines.get(world);
        if (knownBlocks == null)
        {
            knownBlocks = new ArrayList<PlacedBlock>();
            knownMines.put(world, knownBlocks);
        }
        knownBlocks.add(block);
        knownMineTypes.add(block.getBlock().getType());
    }

    private static List<PlacedBlock> getKnownBlocks(World world)
    {
        List<PlacedBlock> result = knownMines.get(world);
        return result == null ? new ArrayList<PlacedBlock>() : new ArrayList<PlacedBlock>(result);
    }

    boolean canPlaceOnBlock(Player p, Block baseBlock)
    {
        return baseBlock != null && baseBlock.getType().isBlock() &&
                !(getWorldType(baseBlock).hasGenerator() && WorldGenerationTools.buildingIsPrevented(baseBlock)) &&
                MATERIALS_THAT_CAN_BECOME_BLOCK.contains(baseBlock.getRelative(0, 1, 0).getType()) &&
                !MATERIALS_THAT_CAN_BECOME_BLOCK.contains(baseBlock.getType());
    }

    private Block getRandomOffsetBlock(Player placer, final Block centerBaseBlock, final int amountBeingPlaced)
    {
        int maxOffset = (int)Math.round((Math.sqrt(amountBeingPlaced)/2 - 0.5)*1.3f);
        if (maxOffset < 1)
        {
            maxOffset = 1;
        }
        int xOff = random.nextInt(1 + 2*maxOffset) - maxOffset;
        int zOff = random.nextInt(1 + 2*maxOffset) - maxOffset;
        if (xOff == 0 && zOff == 0)
        {
            return getRandomOffsetBlock(placer, centerBaseBlock, amountBeingPlaced);
        }
        int centerY = centerBaseBlock.getLocation().toVector().getBlockY();
        Block choice = centerBaseBlock.getRelative(xOff, 0, zOff);
        if (!canPlaceOnBlock(placer, choice))
        {
            for (int up = 1, down = -1; up < maxOffset || down > -maxOffset; up++, down--)
            {
                if (centerY + up < centerBaseBlock.getWorld().getMaxHeight())
                {
                    choice = centerBaseBlock.getRelative(xOff, up, xOff);
                    if (canPlaceOnBlock(placer, choice))
                    {
                        break;
                    }
                }
                if (centerY + down > 0)
                {
                    choice = centerBaseBlock.getRelative(xOff, down, zOff);
                    if (canPlaceOnBlock(placer, choice))
                    {
                        break;
                    }
                }
            }
        }
        return choice;
    }

    protected final Block placeOne(Player culprit, Block baseBlock)
    {
        return placeOne(culprit, baseBlock, culprit.getItemInHand().getType(), true);
    }

    private Block placeOne(Player placer, Block baseBlock, Material typeInHand, boolean retail)
    {
        if (canPlaceOnBlock(placer, baseBlock))
        {
            Block whereToPutMine = baseBlock.getRelative(0, 1, 0);
            if (retail)
            {
                removeOne(placer);
                placer.sendMessage(placementMessage);
            }
            World world = whereToPutMine.getWorld();
            List<Integer> knownNumbers = takenNumbers.get(world);
            if (knownNumbers == null)
            {
                knownNumbers = new ArrayList<Integer>();
                takenNumbers.put(world, knownNumbers);
            }
            int mineID = getFreeInt(knownNumbers);
            PlacedBlock placedBlock = new PlacedBlock(whereToPutMine, whereToPutMine.getType(), placer, mineID, getAdditionalData(placer));
            cacheBlock(baseBlock.getWorld(), placedBlock);
            FileManager.putData(getMineLocationFile(world, true), "" + mineID, placedBlock.serialize(placer));
            whereToPutMine.setType(typeInHand);
            return whereToPutMine;
        }
        else if (retail)
        {
            placer.sendMessage(ChatColor.RED + "You can't place a block there!");
        }
        return null;
    }

    protected final void placeAllInHand(final Player p, Block baseBlock)
    {
        ItemStack itemInhand = p.getItemInHand();
        final Material matInHand = itemInhand.getType();
        final int oldAmount = itemInhand.getAmount();
        final int maxAmount = oldAmount;// getItemStack().getAmount();
        final int amountThrown = oldAmount < maxAmount ? oldAmount : maxAmount;
        //Bukkit.getAttacker("HappyMan").sendMessage("setting velocity of minePlacer");

        final List<Block> whereToPlaceMines = new ArrayList<Block>();
        if (canPlaceOnBlock(p, baseBlock))
        {
            whereToPlaceMines.add(baseBlock);
        }
        for (int it = 0; whereToPlaceMines.size() < amountThrown && it < amountThrown*2; it++)
        {
            Block offsetBlock = getRandomOffsetBlock(p, baseBlock, amountThrown);
            if (canPlaceOnBlock(p, offsetBlock) && !whereToPlaceMines.contains(offsetBlock))
            {
                whereToPlaceMines.add(offsetBlock);
            }
        }

        if (whereToPlaceMines.size() > 0)
        {
            final Item item = p.getWorld().dropItem(p.getLocation(), itemInhand);
            InventoryManager.removeItemsFromHand(p, amountThrown);//after dropping

            final Vector v = p.getLocation().getDirection();
            final float vMod = 1.8F;
            v.setX(v.getX() * vMod);
            v.setY(v.getY() * vMod);
            v.setZ(v.getZ() * vMod);
            item.setVelocity(v);

            Bukkit.getScheduler().scheduleSyncDelayedTask(getPlugin(), new Runnable()
            {
                public void run()
                {
                    item.remove();
                    for (Block b : whereToPlaceMines)
                    {
                        placeOne(p, b, matInHand, false);
                    }
                }
            }, Math.round(p.getLocation().distance(baseBlock.getLocation())/1.33));
            p.sendMessage(placementMessage);
        }
        else
        {
            p.sendMessage(ChatColor.RED + "You can't place blocks there!");
        }
    }

    protected final void placeAllInHandWhereLooking(Player p, int range)
    {
        placeAllInHand(p, p.getTargetBlock(MATERIALS_THAT_CAN_BECOME_BLOCK, range));
    }

    @Override
    public void performLeftClickAction(Player p, Block blockClicked)
    {
        super.performLeftClickAction(p, blockClicked);
        p.getItemInHand().setType(nextMineMaterial(p.getItemInHand().getType()));
    }

    private Material nextMineMaterial(Material currentMat)
    {
        for (int i = 0; i < placedBlockMaterials.length; i++)
        {
            if (placedBlockMaterials[i] == currentMat)
            {
                return placedBlockMaterials[(i + 1) % placedBlockMaterials.length];
            }
        }
        if (placedBlockMaterials.length > 0)
        {
            return placedBlockMaterials[0];
        }
        sendErrorMessage("Error! Did not have any materials to be placed with " + getClass().getSimpleName() + "!");
        return Material.STONE;
    }

    protected static final boolean isKnownBlock(Block block, PlacedBlock knownBlock)
    {
        return block != null && knownBlock.getBlock().equals(block);
    }

    protected static PlacedBlock getKnownBlock(Block potentialPlacedBlock)
    {
        if (potentialPlacedBlock != null && knownMineTypes.contains(potentialPlacedBlock.getType()))
        {
            for (PlacedBlock block : getKnownBlocks(potentialPlacedBlock.getWorld()))
            {
                if (block.getBlock().equals(potentialPlacedBlock))
                {
                    return block;
                }
            }
        }
        return null;
    }
}
