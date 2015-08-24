package osmcb.utilities.image;

/**
 * Note: this class has a natural ordering that is inconsistent with equals.
 * The ordering is descending on the usage count, while equality is based on the color value.
 * 
 * @author humbach
 *
 */
public class OSMPaletteEntry implements Comparable
{
	/**
	 * 20140112 AH v1 initial version
	 */
	private static final long serialVersionUID = 1L;

	protected OSMColor mColor; // the color in the image
	// protected int mCount; // the number of pixels of this color in the image
	// protected int mColIdx; // the color index in the palette
	protected int mMapIdx = -1; // index of the color used in the mapped image

	@SuppressWarnings("unused")
	private OSMPaletteEntry()
	{
		mColor = new OSMColor(0);
		// mCount = 0;
		// mColIdx = 0;
	}

	/**
	 * this initially creates each color as its own entry. the map method is used to map colors in the image to colors in the mapped image.
	 * only the first 127 entries are used in a BSB KAP TIFF image.
	 * 
	 * @param tCol
	 * @param nCnt
	 */
	public OSMPaletteEntry(OSMColor tCol, int nCnt, int nIdx)
	{
		mColor = tCol;
		// mCount = nCnt;
		// mColIdx = nIdx;
	}

	public OSMPaletteEntry(OSMColor tCol)
	{
		mColor = tCol;
	}

	/**
	 * maps another color to the current one and adjusts the usage counts of both colors
	 * 
	 * @param tPE
	 *          - the color to be mapped to this
	 * @return the new usage count of this
	 */
	// public int map(OSMPaletteEntry tPE)
	// {
	// tPE.setMIndex(mColIdx);
	// mCount += tPE.getCount();
	// tPE.setCount(0);
	// return mCount;
	// }

	// public int getCount()
	// {
	// return mCount;
	// }

	/**
	 * 
	 * @param nCnt
	 * @return the new usage count of this
	 */
	// public int setCount(int nCnt)
	// {
	// mCount = nCnt;
	// return mCount;
	// }

	// public int getIndex()
	// {
	// return mColIdx;
	// }

	// public int setIndex(int nIdx)
	// {
	// mColIdx = nIdx;
	// return mColIdx;
	// }

	public int getMIndex()
	{
		return mMapIdx;
	}

	public int setMIndex(int nIdx)
	{
		mMapIdx = nIdx;
		return mMapIdx;
	}

	OSMColor getColor()
	{
		return mColor;
	}

	@Override
	public String toString()
	{
		// return ("PE: Col=(" + getColor().toStringRGB() + "), Cnt=" + getCount() + ", Idx=" + mColIdx + ", MIdx=" + mMapIdx);
		return ("PE: Col=(" + getColor().toStringRGB() + "), MIdx=" + mMapIdx);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((mColor == null) ? 0 : mColor.hashCode());
		return result;
	}

	/*
	 * two palette entries are equal if they have the same color values
	 * 
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
		{
			return true;
		}
		if (obj == null)
		{
			return false;
		}
		if (getClass() != obj.getClass())
		{
			return false;
		}
		OSMPaletteEntry other = (OSMPaletteEntry) obj;
		if (mColor == null)
		{
			if (other.mColor != null)
			{
				return false;
			}
		}
		else if (!mColor.equals(other.mColor))
		{
			return false;
		}
		return true;
	}

	/**
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(Object obj)
	{
		if (this == obj)
		{
			return 0;
		}
		if (obj == null)
		{
			return 1;
		}
		if (getClass() != obj.getClass())
		{
			return 1;
		}
		return 1;
	}

	public int getMID(OSMColor tColor)
	{
		return mMapIdx;
	}
}
