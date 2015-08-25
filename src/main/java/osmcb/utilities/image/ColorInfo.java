package osmcb.utilities.image;

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
	 * Creates a new color info as a mapping to an already known target color
	 * 
	 * @param tMColor
	 */
	public ColorInfo(OSMColor tMColor)
	{
		mMColor = tMColor;
	}

	/**
	 * Creates a new color info with a known usage count
	 * 
	 * @param nCount
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

	public double getDist()
	{
		return mDist;
	}

	public void setDist(double dDist)
	{
		mDist = dDist;
	}

	public int getPIdx()
	{
		return mPIdx;
	}

	public void setPIdx(int nIdx)
	{
		mPIdx = nIdx;
	}

	public OSMColor getMColor()
	{
		return mMColor;
	}

	/**
	 * Maps this color to another color
	 * 
	 * @param tMColor
	 *          The mapping target color
	 * @return The usage count of this color, it will be used to update the usage count of the mapping target color
	 */
	public int setMColor(OSMColor tMColor)
	{
		int oldCount = mCount;
		mCount = 0;
		mMColor = tMColor;
		return oldCount;
	}

	public int getCount()
	{
		return mCount;
	}

	/**
	 * Updates the usage count
	 * 
	 * @param nCount
	 * @return The previous usage count before the update
	 */
	public int setCount(int nCount)
	{
		int oldCount = mCount;
		mCount = nCount;
		mMColor = null;
		return oldCount;
	}

	/**
	 * Updates the usage count by incrementing it by the given increment
	 * 
	 * @param nCount
	 *          The increment to apply to the usage count
	 * @return The previous usage count before the update
	 */
	public int incCount(int nCount)
	{
		int oldCount = mCount;
		mCount += nCount;
		mMColor = null;
		return oldCount;
	}

	/**
	 * Updates the usage count by incrementing it by one
	 * 
	 * @return The previous usage count before the update
	 */
	public int incCount()
	{
		int oldCount = mCount;
		mCount++;
		mMColor = null;
		return oldCount;
	}
}