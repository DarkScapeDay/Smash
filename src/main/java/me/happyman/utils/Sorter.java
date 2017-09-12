package me.happyman.utils;

import com.mysql.fabric.xmlrpc.base.Array;
import org.bukkit.Bukkit;

import java.util.Arrays;
import java.util.List;

public abstract class Sorter<T>
{
    public static final Sorter<Integer> intSorter = new Sorter<Integer>()
    {
        @Override
        public boolean compare(Integer thing1, Integer thing2)
        {
            return thing1 > thing2;
        }
    };

    /**
     * @return True if the first one is bigger
     */
    public abstract boolean compare(T thing1, T thing2);

    public void mergeSort(List<T> entries)
    {
        T[] arrayVersion = (T[])entries.toArray();
        mergeSort(arrayVersion);
        for (int i = 0; i < arrayVersion.length; i++)
        {
            entries.set(i, arrayVersion[i]);
        }
    }

    public void mergeSort(T[] entries)
    {
        mergeSort(entries, 0, entries.length - 1);
    }

    private void mergeSort(T[] entries, int first, int last)
    {
        if (first < last && entries.length > 0)
        {
            int mid = (first + last)/2;
            mergeSort(entries, first, mid);
            mergeSort(entries, mid + 1, last);
            merge(entries, first, mid, last);
        }
    }

    private void merge(T[] entries, int first, int mid, int last)
    {
        Object[] temp = new Object[entries.length];
        int first1 = first;
        int first2 = mid + 1;
        int index = first;

        while (first1 <= mid && first2 <= last)
        {
            T left = entries[first1];
            T right = entries[first2];
            final T nextValue;
            if (compare(left, right))
            {
                nextValue = right;
                first2++;
            }
            else
            {
                nextValue = left;
                first1++;
            }
            temp[index++] = nextValue;
        }
        while (first1 <= mid)
        {
            temp[index++] = entries[first1++];
        }
        while (first2 <= last)
        {
            temp[index++] = entries[first2++];
        }
        for (index = first; index <= last; index++)
        {
            entries[index] = (T)temp[index];
        }
    }
}
