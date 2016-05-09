package osmcb.utilities.image;

import java.util.HashMap;

import org.apache.log4j.Logger;

public class OSMCBColorList extends HashMap<OSMColor, ColorInfo>
{
	protected static Logger log = Logger.getLogger(OSMCBColorMap.class);;

	// instance data
	private int mUnmappedColors = 0;

	public OSMCBColorList()
	{
		// adjust the logger to the actual implementation
		log = Logger.getLogger(this.getClass());
	}

	public OSMColor getMColor(OSMColor tColor)
	{
		ColorInfo tCInfo = get(tColor);
		if (tCInfo != null)
			return tCInfo.mMColor;
		else
			return null;
	}

	/**
	 * @return The number of unmapped colors.
	 */
	public int getMColorCnt()
	{
		return mUnmappedColors;
	}

	/**
	 * reduce the number of unmapped colors by 1
	 * 
	 * @return The number of unmapped colors before the reduction.
	 */
	public int decMColorCnt()
	{
		return mUnmappedColors--;
	}

	/*
	 *
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
	 *          The new color.
	 * @return The number of colors in the list after the addition
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

	/**
	 * @param tColor
	 *          The color whose usage count is to be retrieved.
	 * @return The usage count of the specified color.
	 */
	public int getCount(OSMColor tColor)
	{
		int nCount = 0;
		if (this.containsKey(tColor))
			nCount = this.get(tColor).getCount();
		return nCount;
	}
}
