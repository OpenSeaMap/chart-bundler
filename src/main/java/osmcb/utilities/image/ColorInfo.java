package osmcb.utilities.image;

/**
 * This provides information about the {@link OSMColor} and its usage in the image. It allows dynamically matching OSMColors depending on their optical distance
 * and usage.
 * 
 * @author humbach
 */
public class ColorInfo
{
	protected OSMColor mMColor = null; // the mapping target color
	protected int mCount = 0; // the usage count if this color is not mapped
	protected OSMColor mBMColor = null; // the 'best matching' color
	protected double mDist = 1e100; // the 'optical distance' to the best matching color
	protected int mPIdx = 0; // index of this color in the final palette

	/**
	 * Creates an empty color usage info
	 */
	@SuppressWarnings("unused")
	private ColorInfo()
	{
	}

	/**
	 * Creates a new color info as a mapping to an already known target color.
	 * 
	 * @param tMColor
	 */
	public ColorInfo(OSMColor tMColor)
	{
		mMColor = tMColor;
	}

	/**
	 * Creates a new color info with a known usage count.
	 * 
	 * @param nCount
	 *          to be set.
	 */
	public ColorInfo(int nCount)
	{
		mCount = nCount;
	}

	public OSMColor getBestMatch()
	{
		return mBMColor;
	}

	public void setBestMatch(OSMColor tColor)
	{
		mBMColor = tColor;
	}

	/**
	 * @return The 'optical distance' to the best matching color.
	 */
	public double getDist()
	{
		return mDist;
	}

	/**
	 * @param dDist
	 *          The 'optical distance' to the best matching color to be set.
	 */
	public void setDist(double dDist)
	{
		mDist = dDist;
	}

	/**
	 * @return The index of this color in the final palette.
	 */
	public int getPIdx()
	{
		return mPIdx;
	}

	/**
	 * @param nIdx
	 *          The index of this color in the final palette to be set.
	 */
	public void setPIdx(int nIdx)
	{
		mPIdx = nIdx;
	}

	public OSMColor getMColor()
	{
		return mMColor;
	}

	/**
	 * Maps this color to another color.
	 * 
	 * @param tMColor
	 *          The mapping target color.
	 * @return The usage count of this color, it will be used to update the usage count of the mapping target color
	 */
	public int setMColor(OSMColor tMColor)
	{
		int prevCount = mCount;
		mCount = 0;
		mMColor = tMColor;
		return prevCount;
	}

	public int getCount()
	{
		return mCount;
	}

	/**
	 * Updates the usage count.
	 * 
	 * @param nCount
	 * @return The previous usage count before the update.
	 */
	public int setCount(int nCount)
	{
		int prevCount = mCount;
		mCount = nCount;
		mMColor = null;
		return prevCount;
	}

	/**
	 * Updates the usage count by incrementing it by the given increment.
	 * 
	 * @param nCount
	 *          The increment to apply to the usage count.
	 * @return The previous usage count before the update.
	 */
	public int incCount(int nCount)
	{
		int prevCount = mCount;
		mCount += nCount;
		mMColor = null;
		return prevCount;
	}

	/**
	 * Updates the usage count by incrementing it by one.
	 * 
	 * @return The previous usage count before the update.
	 */
	public int incCount()
	{
		int prevCount = mCount;
		mCount++;
		mMColor = null;
		return prevCount;
	}
}
