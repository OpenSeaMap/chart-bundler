/**
 * 
 */
package osmcb.utilities.image;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.log4j.Logger;

/**
 * This is the basic storage for all colors found in an image.
 * By itself it allows fast location of a color by its color value.
 * It is supported by additional
 * 
 * @author humbach
 *
 */
public class OSMCBColorMap extends HashMap<OSMColor, ColorInfo>
{
	/**
	 * AH 2015-08-15 Initial version
	 */
	private static final long serialVersionUID = 1L;
	protected final Logger log;

	/**
	 * This comparator sorts the colors according to their usage count with the most used colors listed first.
	 * Colors with the same usage count are ordered by their rgb color value.
	 */
	static final Comparator<Entry<OSMColor, ColorInfo>> BYCOUNTDESC = new Comparator<Entry<OSMColor, ColorInfo>>()
	{
		@Override
		public int compare(Entry<OSMColor, ColorInfo> o1, Entry<OSMColor, ColorInfo> o2)
		{
			int nRes = 0;
			if (o1.getValue().getCount() < o2.getValue().getCount())
				nRes = 1;
			else if (o1.getValue().getCount() > o2.getValue().getCount())
				nRes = -1;
			else
			{
				if (o1.getKey().getRGB() < o2.getKey().getRGB())
					nRes = -1;
				else if (o1.getKey().getRGB() > o2.getKey().getRGB())
					nRes = 1;
			}
			return nRes;
		}
	};

	/**
	 * This comparator sorts the colors according to the optical distance to their best match.
	 * Colors with the same optical distance are ordered by their rgb color value.
	 */
	static final Comparator<Entry<OSMColor, ColorInfo>> BYDIST = new Comparator<Entry<OSMColor, ColorInfo>>()
	{
		@Override
		public int compare(Entry<OSMColor, ColorInfo> o1, Entry<OSMColor, ColorInfo> o2)
		{
			int nRes = 0;
			if (o1.getValue().getDist() < o2.getValue().getDist())
				nRes = -1;
			else if (o1.getValue().getDist() > o2.getValue().getDist())
				nRes = 1;
			else
			{
				if (o1.getKey().getRGB() < o2.getKey().getRGB())
					nRes = -1;
				else if (o1.getKey().getRGB() > o2.getKey().getRGB())
					nRes = 1;
			}
			return nRes;
		}
	};

	// instance data
	private int mUnmappedColors = 0;
	private SortedSet<Entry<OSMColor, ColorInfo>> mColorsUse = new TreeSet<>(BYCOUNTDESC);

	public OSMCBColorMap()
	{
		log = Logger.getLogger(this.getClass());
	}

	public OSMColor getMColor(OSMColor tColor)
	{
		if (this.get(tColor) != null)
			return this.get(tColor).mMColor;
		else
			return null;
	}

	public int getMColorCnt()
	{
		return mUnmappedColors;
	}

	public int decMColorCnt()
	{
		return mUnmappedColors--;
	}

	/*
	 * @see java.util.HashMap#put(java.lang.Object, java.lang.Object)
	 */
	@Override
	public ColorInfo put(OSMColor tColor, ColorInfo value)
	{
		if (value.getMColor() == null)
			++mUnmappedColors;
		return super.put(tColor, value);
	}

	public ColorInfo mapColor(OSMColor tColor, OSMColor tMColor)
	{
		ColorInfo tCI = get(tColor);
		tCI.setMColor(tMColor);
		put(tColor, tCI);
		mUnmappedColors--;
		return tCI;
	}

	/**
	 * Adds a new color to the list. It is assumed that the usage count is one.
	 * 
	 * @param tColor
	 * @return The number of colors in the list
	 */
	public int add(OSMColor tColor)
	{
		if (this.containsKey(tColor))
			get(tColor).incCount();
		else
		{
			put(tColor, new ColorInfo(1));
		}
		return this.size();
	}

	public int getCount(OSMColor tColor)
	{
		int nCount = 0;
		if (this.containsKey(tColor))
			nCount = this.get(tColor).getCount();
		return nCount;
	}

	/**
	 * 
	 * @return
	 */
	SortedSet<Entry<OSMColor, ColorInfo>> usageSet()
	{
		mColorsUse.clear();
		mColorsUse.addAll(this.entrySet());
		return mColorsUse;
	}

	/**
	 * @return This iterates over all colors in order of descending usage count
	 */
	public Iterator<Entry<OSMColor, ColorInfo>> getUsageIt()
	{
		usageSet();
		return mColorsUse.iterator();
	}

	/**
	 * @return This iterates over all colors following the current color in order of descending usage count
	 */
	public Iterator<Entry<OSMColor, ColorInfo>> getLessUsageIt(Entry<OSMColor, ColorInfo> tPE)
	{
		SortedSet<Entry<OSMColor, ColorInfo>> tColorsLessUsed = new TreeSet<>(BYCOUNTDESC);
		tColorsLessUsed.addAll(mColorsUse.tailSet(tPE));
		return tColorsLessUsed.iterator();
	}

	/**
	 * @return This iterates over all colors ascending with the 'optical distance' to their best match
	 */
	public Iterator<Entry<OSMColor, ColorInfo>> getODistIt()
	{
		SortedSet<Entry<OSMColor, ColorInfo>> tColorsDist = new TreeSet<>(BYDIST);
		tColorsDist.addAll(mColorsUse);
		log.trace("dist set[" + tColorsDist.size() + "]");
		return tColorsDist.iterator();
	}

	/**
	 * @return The best color match with the least 'optical distance'
	 */
	public Entry<OSMColor, ColorInfo> getBestODist()
	{
		SortedSet<Entry<OSMColor, ColorInfo>> tColorsDist = new TreeSet<>(BYDIST);
		tColorsDist.addAll(mColorsUse);
		log.trace("dist set[" + tColorsDist.size() + "]");
		return tColorsDist.first();
	}
}
