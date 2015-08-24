/**
 * 
 */
package osmcb.utilities.image;

import java.util.ArrayList;

/**
 * List containing all colors found in the map image. It is sorted by usage count.
 * Its original purpose is the mapping of the 'unlimited' colors in map/tile to the limited colors in palette image like the one found in a bsb-kap file.
 * 
 * @author humbach
 * @param <OSMPaletteEntry>
 *
 */
public class OSMColorList<OSMPaletteEntry> extends ArrayList<OSMPaletteEntry>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private int mPaletteCnt = 128; // the BSB-KAP allows up to 128 colors, including the unused color 0
	private int mStdColors = 0;

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.ArrayList#size()
	 */
	@Override
	public int size()
	{
		return super.size();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.ArrayList#contains(java.lang.Object)
	 */
	@Override
	public boolean contains(Object o)
	{
		return super.contains(o);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.ArrayList#indexOf(java.lang.Object)
	 */
	@Override
	public int indexOf(Object o)
	{
		return super.indexOf(o);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.ArrayList#lastIndexOf(java.lang.Object)
	 */
	@Override
	public int lastIndexOf(Object o)
	{
		return super.lastIndexOf(o);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.ArrayList#get(int)
	 */
	@Override
	public OSMPaletteEntry get(int index)
	{
		return super.get(index);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.ArrayList#set(int, java.lang.Object)
	 */
	@Override
	public OSMPaletteEntry set(int index, OSMPaletteEntry element)
	{
		// TODO Auto-generated method stub
		return super.set(index, element);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.ArrayList#add(java.lang.Object)
	 */
	@Override
	public boolean add(OSMPaletteEntry e)
	{
		return super.add(e);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.ArrayList#add(int, java.lang.Object)
	 */
	@Override
	public void add(int index, OSMPaletteEntry element)
	{
		super.add(index, element);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.ArrayList#remove(int)
	 */
	@Override
	public OSMPaletteEntry remove(int index)
	{
		// TODO Auto-generated method stub
		return super.remove(index);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.util.ArrayList#remove(java.lang.Object)
	 */
	@Override
	public boolean remove(Object o)
	{
		// TODO Auto-generated method stub
		return super.remove(o);
	}

}
