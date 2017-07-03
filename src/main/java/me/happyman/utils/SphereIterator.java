package me.happyman.utils;

import org.bukkit.block.Block;

public abstract class SphereIterator<T>
{
    private T data;

    public SphereIterator()
    {
        initializeData();
    }

    protected abstract void initializeData();

    public void iterateThroughSphere(Block center, int enclosingRadius)
    {
        boolean reverseJ = false;
        boolean reverseK = false;
        Block blockToCheck = center.getRelative(-enclosingRadius, -enclosingRadius, -enclosingRadius);
        for (int i = 0; i <= enclosingRadius*2; i++, blockToCheck = blockToCheck.getRelative(1, 0, 0))
        {
            for (int j = 0; j <= enclosingRadius*2; j++, blockToCheck = reverseJ ? blockToCheck.getRelative(0, -1, 0) : blockToCheck.getRelative(0, 1, 0))
            {
                for (int k = 0; k <= enclosingRadius*2; k++, blockToCheck = reverseK ? blockToCheck.getRelative(0, 0, -1) : blockToCheck.getRelative(0, 0, 1))
                {
                    if (blockToCheck.getLocation().distance(center.getLocation()) < enclosingRadius)
                    {
                        doStuff(center, blockToCheck);
                    }
                }
                reverseK = !reverseK;
            }
            reverseJ = !reverseJ;
        }
    }

    protected T getData()
    {
        return data;
    }

    protected void setData(T data)
    {
        this.data = data;
    }

    protected abstract void doStuff(Block center, Block blockInSphere);
}
