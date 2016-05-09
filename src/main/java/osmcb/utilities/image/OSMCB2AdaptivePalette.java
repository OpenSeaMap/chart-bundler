package osmcb.utilities.image;

import java.awt.image.BufferedImage;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * This @class is used to assign the 127 colors available in the KAP-format to the most used colors in the image.
 * We need to
 * 1. look up the existing entries in the palette to check if the current color is already in the palette. This is done by a lookup in the hashmap for the
 * color value
 * 2. count the number of pixels if more than one pixel is using the current color. The usage count is stored in the color use list, it is possible and even
 * probable that more than one color have the same usage count.
 * 3. sort the table according to the number of pixels to find the most and the least used colors to do the matching
 * 4. find the best fitting color to the current one
 * 5. remember the matching when coding the image. The matching is stored in the hashmap to ensure a quick way to find the index of the mapped color for a pixel
 * 
 * @author humbach
 *
 */
public class OSMCB2AdaptivePalette implements IfOSMPalette
{
	protected static Logger log = Logger.getLogger(OSMCB2AdaptivePalette.class);

	/*
	 * 1 HashMap with <color, pIndex> - allows to fast find the palette index of a color by value. pIndex is [-1...127]. -1 means mapped to another color.
	 * 2 SortedList with <usage count, color> - sorted by usage count. If a color is mapped, aka the usage count is 0 it is removed from this list.
	 * 3 SortedList with best matching pairs <distance, color1, color2> - sorted by distance. There is one entry for each color1 in the list.
	 */

	private class OSMCB2PaletteInfo implements Comparable
	{
		private Integer mPIdx; // color index in the final palette
		private Integer mCID; // the colors ID in the color map

		public OSMCB2PaletteInfo(Integer nPIdx, Integer nCID)
		{
			mPIdx = nPIdx;
			mCID = nCID;
		}

		@Override
		public int compareTo(Object obj)
		{
			if (obj instanceof OSMCB2PaletteInfo)
			{
				OSMCB2PaletteInfo tPI = (OSMCB2PaletteInfo) obj;
				if (mPIdx < tPI.mPIdx)
					return -1;
				else if (mPIdx > tPI.mPIdx)
					return 1;
			}
			else
				throw new ClassCastException("Not a palette info");
			return 0;
		}
	}

	private class OSMCB2ColorPair
	{
		OSMColor mCol;
		OSMColor mMCol;

		OSMCB2ColorPair(OSMColor tCol, OSMColor tMCol)
		{
			mCol = tCol;
			mMCol = tMCol;
		}
	};

	private static final int STD_COLOR_COUNT = 32;

	// instance data
	/**
	 * 'index' Map containing all colors found in the map image. It maps the color values to the color list index (aka cID).
	 */
	private OSMCB2ColorMap mColorsHM = new OSMCB2ColorMap(STD_COLOR_COUNT);
	// /**
	// * List containing all colors found in the map image. It is sorted by usage count.
	// * It will be resorted if the usage count changes due to mapping.
	// */
	// private ArrayList<OSMCB2PaletteInfo> mColorPaletteList = new ArrayList<OSMCB2PaletteInfo>(128);

	/**
	 * Map containing the 'best matching pairs'. It maps the distance and the (target and the source) indices in the palette.
	 * It is sorted by ascending distance.
	 * !This is an insecure usage since it is not guaranteed that there are not two ColorPairs with the same distance!
	 * But even in the very unlikely case, that there are - and they are the least distance of all too, we don't give a damn and use one of the pairs.
	 * Do we loose the other pair in that case????
	 */
	private TreeMap<Double, OSMCB2ColorPair> mMatchesTM = new TreeMap<Double, OSMCB2ColorPair>();

	/**
	 * Supposedly the color IDs start with 1 in bsb/kap format.
	 * OpenCPN uses color 0 as well (2016-01).
	 */
	private int mStartColorIdx = 0;
	private int mPaletteCnt = 128; // the BSB-KAP allows up to 128 colors, including the (supposedly unused) color 0
	private int mStdColors = 32;

	public OSMCB2AdaptivePalette()
	{
		// adjust to actual implementation
		log = Logger.getLogger(this.getClass());
	}

	/**
	 * Creates the palette of the given image with 'adaptive' colors
	 *
	 * @param img
	 *          - The image for which to create the palette.
	 */
	public OSMCB2AdaptivePalette(BufferedImage img)
	{
		this();
		try
		{
			// Initialize some (STD_COLOR_COUNT) standard colors
			addPixel(new OSMColor(255, 255, 255));
			addPixel(new OSMColor(0, 255, 255));
			addPixel(new OSMColor(255, 0, 255));
			addPixel(new OSMColor(255, 255, 0));
			addPixel(new OSMColor(0, 0, 255));
			addPixel(new OSMColor(0, 255, 0));
			addPixel(new OSMColor(255, 0, 0));
			addPixel(new OSMColor(0, 0, 0));
			addPixel(new OSMColor(127, 127, 127));
			addPixel(new OSMColor(181, 208, 208));
			addPixel(new OSMColor(228, 198, 171));
			addPixel(new OSMColor(177, 139, 190));
			addPixel(new OSMColor(235, 219, 232));
			addPixel(new OSMColor(181, 181, 146));
			addPixel(new OSMColor(241, 238, 232));
			addPixel(new OSMColor(173, 208, 158));
			addPixel(new OSMColor(238, 237, 229));
			addPixel(new OSMColor(178, 211, 163));
			addPixel(new OSMColor(228, 233, 218));
			addPixel(new OSMColor(193, 218, 180));
			addPixel(new OSMColor(220, 229, 210));
			addPixel(new OSMColor(185, 214, 171));
			addPixel(new OSMColor(190, 216, 177));
			addPixel(new OSMColor(213, 226, 202));
			addPixel(new OSMColor(232, 234, 222));
			addPixel(new OSMColor(172, 204, 198));
			addPixel(new OSMColor(181, 212, 169));
			addPixel(new OSMColor(201, 221, 189));
			addPixel(new OSMColor(197, 219, 185));
			addPixel(new OSMColor(204, 222, 193));
			addPixel(new OSMColor(209, 224, 197));
			addPixel(new OSMColor(164, 204, 149));

			for (int y = 0; y < img.getHeight(); ++y)
			{
				for (int x = 0; x < img.getWidth(); ++x)
				{
					addPixel(new OSMColor(img.getRGB(x, y)));
				}
			}
			log.debug("Palette[" + mColorsHM.getUsedColors() + "] after put()");
			test();
			// log.debug("Colors:" + toString());

			// in the possible case the image contains less than 128 color adjust the palette size
			if (mColorsHM.size() < mPaletteCnt)
				mPaletteCnt = mColorsHM.size();

			// if more colors are used than find place in the palette reduce the number of colors by mapping
			if (mColorsHM.getUsedColors() > mPaletteCnt)
			{
				reduce();
				log.debug("Palette[" + mColorsHM.getUsedColors() + "] after reduce()");
				test();
			}
			else
				log.debug("Palette[" + mColorsHM.getUsedColors() + "] no reduction neccessary");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			log.error("Exception while constructing palette. " + e.getMessage());
		}
	}

	/**
	 * reduces the palette to mPaletteCnt entries by mapping the least used entries to the mPaletteCnt most used ones.
	 * this is done recursively from the start on.
	 */
	public void reduce()
	{
		// The palette is traversed in 'normal' order, which is descending in the usage count. This means, often used colors are mapped first, and hopefully
		// prevents color drifting while chained mapping.
		// It stops when the usage count of the color has reached 0, so colors which have already been mapped earlier are skipped.

		// The first round maps neatly matching colors, so the most often used colors move to the front of the palette
		log.debug("reduce(): start");
		OSMColor tTgtColor = null;
		OSMColor tSrcColor = null;
		for (int nTCol = 1; nTCol < mColorsHM.getUsedColors() - 1; nTCol++)
		{
			tTgtColor = mColorsHM.getColorByPIdx(nTCol);
			int nSCol = Math.max(STD_COLOR_COUNT + 1, nTCol + 1);
			while (nSCol < mColorsHM.getUsedColors())
			{
				tSrcColor = mColorsHM.getColorByPIdx(nSCol);
				if (tTgtColor.qDist(tSrcColor) < 17)
				{
					log.trace("colors to map(r1): Src=" + tSrcColor.toStringRGB() + ", " + nSCol + ", Tgt=" + tTgtColor.toStringRGB() + ", " + nTCol);
					mColorsHM.map(tSrcColor, tTgtColor);
				}
				else
				{
					++nSCol;
				}
			}
		}

		log.debug("\r\n");
		log.debug("Palette[" + mColorsHM.getUsedColors() + "] after first round()");
		test();

		// // Second round: now map the colors more different. Here one has to be careful. We use OSMColor.oDiff() instead of OSMColor.dDiff()
		// // For each color in the list the best matching color is found. Then the best color match, i.e. the one with the least difference is mapped.
		// // This is repeated until only the 127 colors in the map remain.
		// // We need a sorted map with the distances and the indices
		for (int nTCol = 1; nTCol < (mColorsHM.getUsedColors() - 1); nTCol++)
		{
			findBestMatch(nTCol, false);
		}

		// log.trace("mapped colors[" + nSrcColor + "] of [" + nTstColor + "]-" + mColorsHM.getMColorCnt());
		// Now crawl over the distance map and match the color. After each match the distance map must be corrected.
		// Since the mapping does not produce new colors, the best matches can only become worse when the source color is mapped and hence removed from the used
		// colors list.
		while (mColorsHM.getUsedColors() > mPaletteCnt)
		{
			log.trace("colors in usage list=" + mColorsHM.getUsedColors() + ", matches=" + mMatchesTM.size());
			if (mMatchesTM.size() > 0)
			{
				Entry<Double, OSMCB2ColorPair> tME = mMatchesTM.pollFirstEntry();
				OSMCB2ColorPair tCP = tME.getValue();
				tSrcColor = tCP.mCol;
				tTgtColor = tCP.mMCol;
				log.trace("pair=(Src(" + tSrcColor.toStringRGB() + "), Tgt(" + tTgtColor.toStringRGB() + "), Dist=" + tME.getKey() + ")");
				if (mColorsHM.getCnt(tSrcColor) > 0)
				{
					if (mColorsHM.getCnt(tTgtColor) > 0)
					{
						log.trace("colors to map(r2): Src(" + tSrcColor.toStringRGB() + "), Tgt(" + tTgtColor.toStringRGB() + ")");
						mColorsHM.map(tSrcColor, tTgtColor);
						findBestMatch(mColorsHM.getPIdx(tTgtColor), true);
					}
				}
			}
			else
			{
				log.debug("not enough matches in list");
				break;
			}
		}
		log.debug("\r\n");
		log.debug("Palette[" + mColorsHM.getUsedColors() + "] after reduce():");
		test();
		log.debug("----\r\n");
	}

	/**
	 * Looks for the color best matching the current one. Both colors(indices) will be put into the TreeMap mMatchesTM,
	 * which is sorted by the distance of the two colors .
	 * It allows to solve mapping in the sequence of the distances between the color pairs.
	 * 
	 * @param nTgtPIdx
	 *          - Palette index of the color to find a match for.
	 */
	private void findBestMatch(int nTgtPIdx, boolean bAll)
	{
		Integer tTCID = mColorsHM.getCIDByPIdx(nTgtPIdx);
		OSMColor tTCol = mColorsHM.getColor(tTCID);
		OSMColor tMCol = null;
		double dDiff = 1e100;
		int nSrcPIdx = nTgtPIdx + 1;

		if ((nTgtPIdx < STD_COLOR_COUNT) || (bAll))
			nSrcPIdx = STD_COLOR_COUNT;
		// step over ...
		for (; nSrcPIdx < mColorsHM.getUsedColors(); nSrcPIdx++)
		{
			// OSMCB2PaletteEntry tSPE = mColorList.get(nSCol);
			Integer tSCID = mColorsHM.getCIDByPIdx(nSrcPIdx);
			OSMColor tSCol = mColorsHM.getColor(tSCID);
			double dNewDiff = 0;

			// stop when unused colors encountered
			if (mColorsHM.getCnt(tSCID) == 0)
				break;

			// skip self testing
			if (nTgtPIdx == nSrcPIdx)
				continue;

			// check again if the src color is not already matched, if ok, test if it is a new best distance
			if ((mColorsHM.getCnt(tSCID) > 0) && ((dNewDiff = tTCol.oDist(tSCol)) < dDiff) && (nTgtPIdx != nSrcPIdx))
			{
				// remember the match source color
				tMCol = mColorsHM.getColorByPIdx(nSrcPIdx);
				dDiff = dNewDiff;
				log.trace("DIFF: Src(" + tMCol.toStringRGB() + ") to Tgt(" + tTCol.toStringRGB() + ") oDiff=" + dNewDiff + "; qDiff=" + tTCol.qDist(tMCol));
			}
		}

		if (tMCol == null)
			log.debug("match null");
		else
		// Fill list with color distances.
		if (bAll)
			log.trace("match DIFF: Src(" + tMCol.toStringRGB() + ") to Tgt(" + tTCol.toStringRGB() + ") oDiff=" + dDiff);
		if (mMatchesTM.containsKey(dDiff))
		{
			// evade collision
			log.debug("optical distance already in map. " + dDiff);
			dDiff += (mMatchesTM.higherKey(dDiff) - dDiff) / 2;
		}
		mMatchesTM.put(dDiff, new OSMCB2ColorPair(tMCol, tTCol));
	}

	@Override
	public int put(OSMColor tColor)
	{
		return addPixel(tColor);
	}

	/**
	 * This adds a pixel to the palette. I.e. the color is added to the map if it is not yet there or if it is already in the map the usage count is increased.
	 * 
	 * @param tColor
	 * @return The color ID of the new color.
	 */
	public int addPixel(OSMColor tColor)
	{
		Integer nCID = mColorsHM.addPixel(tColor);
		return nCID;
	}

	@Override
	public int getPIdx(OSMColor tColor)
	{
		return mColorsHM.getMPIdx(tColor);
	}

	/**
	 * This creates a specific String in the format required by the BSB-KAP file format.
	 * It runs at most over the first mPaletteCnt entries in the palette
	 * It ends with a 0x0D,0x0A sequence("\r\n").
	 */
	@Override
	public String asBSBStr()
	{
		String strPal = "";

		for (int nCol = 1; nCol < Math.min(mPaletteCnt, mColorsHM.getUsedColors()); nCol++)
		{
			strPal += "RGB/" + nCol + "," + mColorsHM.getColorByPIdx(nCol).toStringRGB() + "\r\n";
		}
		return strPal;
	}

	/**
	 * This creates a specific String in a format suited for logging or tracing. It lists all colors in the palette.
	 */
	@Override
	public String toString()
	{
		Integer nColors = mColorsHM.getUsedColors();
		String strPal = "\r\n";
		strPal += "Palette: cnt=" + nColors + "\r\n";
		for (int nCol = 0; nCol < nColors; nCol++)
		{
			OSMColor tColor = mColorsHM.getColorByPIdx(nCol);
			if (nCol < mPaletteCnt)
				strPal += "PIdx[" + nCol + "]=" + tColor.toStringKmpl() + ", CID=" + mColorsHM.getCID(tColor) + ", Cnt=" + mColorsHM.getCnt(tColor) + ", unmapped)";
			else if (mColorsHM.getMCID(tColor) != null)
				strPal += "PIdx[" + nCol + "]=" + tColor.toStringKmpl() + ", CID=" + mColorsHM.getCID(tColor) + ", Cnt=" + mColorsHM.getCnt(tColor) + ", mapped to=RGB("
				    + mColorsHM.getColor(mColorsHM.getMCID(tColor)).toStringRGB() + ")";
			else
				strPal += "PIdx[" + nCol + "]=" + tColor.toStringKmpl() + ", CID=" + mColorsHM.getCID(tColor) + ", Cnt=" + mColorsHM.getCnt(tColor)
				    + ", mapped to=null";
			strPal += "\r\n";
		}
		return strPal;
	}

	private void test()
	{
		boolean bOK = true;
		int nSrcPIdx = 0;
		int nTstCnt = mColorsHM.getCnt(mColorsHM.getCIDByPIdx(nSrcPIdx));
		int nSrcCnt = 0;
		// step over all following colors, i.e. all colors less used
		for (nSrcPIdx = STD_COLOR_COUNT; nSrcPIdx < mColorsHM.getUsedColors(); nSrcPIdx++)
		{
			nSrcCnt = mColorsHM.getCnt(mColorsHM.getCIDByPIdx(nSrcPIdx));
			if (nSrcCnt > nTstCnt)
				bOK = false;
			nTstCnt = nSrcCnt;
		}

		if (!bOK)
		{
			log.debug("\r\n");
			log.debug("Palette[" + mColorsHM.getUsedColors() + "] invalid");
			log.debug("Colors:" + toString());
		}

	}

	// several 'old' tries, in different states of outdated. I don't want to discard the code, maybe in some future we will reuse them...

	/**
	 * reduces the palette to mPaletteCnt entries by mapping the least used entries to the mPaletteCnt most used ones.
	 * this is done recursively from the end on.
	 */
	/*
	 * public void reduceBwd()
	 * {
	 * // the palette is traversed in reverse sorting order, which is in ascending usage count order
	 * for (int nCol = mColorPaletteList.size() - 1; nCol >= 2; nCol--)
	 * {
	 * // OSMPaletteEntry tPE = mCL.remove(nCol);
	 * OSMCB2PaletteEntry tPE = mColorPaletteList.get(nCol);
	 * OSMCB2PaletteEntry tTgt = substituteLim(tPE, 17);
	 * if (tTgt != null)
	 * {
	 * int nTgtCnt = tTgt.getCount();
	 * // check if the modified entry has to be moved upwards in the list
	 * int nTgtCol = tTgt.getIndex();
	 * int nTgtNCol = 0;
	 * int nPCol = nTgtCol - 1;
	 * if ((nTgtCol > 1) && (mColorList.get(nPCol).getCount() < nTgtCnt))
	 * {
	 * while ((mColorList.get(nPCol - 1).getCount() < nTgtCnt) && (nPCol - 1 > 0))
	 * {
	 * nPCol--;
	 * }
	 * nTgtNCol = nPCol;
	 * mColorList.remove(nTgtCol);
	 * tTgt.setIndex(nTgtNCol);
	 * mColorList.add(nTgtNCol, tTgt);
	 * tPE.setMIndex(nTgtNCol);
	 * }
	 * mColorList.remove(nCol);
	 * tPE.setCount(0);
	 * mColorList.add(tPE);
	 * for (nPCol++; nPCol <= mColorList.size() - 1; nPCol++)
	 * {
	 * tPE = mColorList.get(nPCol);
	 * tPE.setIndex(nPCol);
	 * int nMIdx = tPE.getMIndex();
	 * if (nMIdx == nCol)
	 * tPE.setMIndex(nTgtNCol);
	 * else if ((nMIdx >= nTgtNCol) && (nMIdx < nTgtCol))
	 * tPE.setMIndex(nMIdx + 1);
	 * if (nMIdx > nCol)
	 * tPE.setMIndex(nMIdx - 1);
	 * }
	 * }
	 * }
	 * 
	 * for (int nCol = mColorList.size() - 1; nCol >= mPaletteCnt; nCol--)
	 * {
	 * // OSMPaletteEntry tPE = mCL.remove(nCol);
	 * OSMCB2PaletteEntry tPE = mColorList.get(nCol);
	 * if (tPE.getCount() > 0)
	 * {
	 * OSMCB2PaletteEntry tTgt = substitute(tPE);
	 * // nCnt = tTgt.map(tPE);
	 * int nTgtCnt = tTgt.getCount();
	 * // check if the modified entry has to be moved upwards in the list
	 * int nTgtCol = mColorList.indexOf(tTgt);
	 * int nTgtNCol = 0;
	 * int nPCol = nTgtCol - 1;
	 * if ((nTgtCol > 1) && (mColorList.get(nPCol).getCount() < nTgtCnt))
	 * {
	 * while ((mColorList.get(nPCol - 1).getCount() < nTgtCnt) && (nPCol - 1 > 0))
	 * {
	 * nPCol--;
	 * }
	 * nTgtNCol = nPCol;
	 * mColorList.remove(nTgtCol);
	 * tTgt.setIndex(nTgtNCol);
	 * mColorList.add(nTgtNCol, tTgt);
	 * tPE.setMIndex(nTgtNCol);
	 * }
	 * mColorList.remove(nCol);
	 * tPE.setCount(0);
	 * mColorList.add(tPE);
	 * // adjust the idx data
	 * for (nPCol++; nPCol < mColorList.size(); nPCol++)
	 * {
	 * tPE = mColorList.get(nPCol);
	 * tPE.setIndex(nPCol);
	 * int nMIdx = tPE.getMIndex();
	 * if (nMIdx == nCol)
	 * tPE.setMIndex(nTgtNCol);
	 * else if ((nMIdx >= nTgtNCol) && (nMIdx < nTgtCol))
	 * tPE.setMIndex(nMIdx + 1);
	 * if (nMIdx > nCol)
	 * tPE.setMIndex(nMIdx - 1);
	 * }
	 * }
	 * }
	 * }
	 */

	/**
	 * performs a linear search for the best matching color. It only tries to substitute with a color which is at least used as often as the original - this
	 * means, it only tests against the entries forward in the palette
	 * it is only matched against unmatched colors to avoid chaining
	 * 
	 * @param tCol1
	 *          - the color to be matched against the existing entries in the palette
	 * @return the best matching entry in the palette
	 */
	/*
	 * protected OSMCB2PaletteEntry substitute(OSMCB2PaletteEntry tSPE)
	 * {
	 * OSMCB2PaletteEntry tTPE = tSPE;
	 * double dDiff = 1e200;
	 * double nNewDiff = 0;
	 * int nSCol = tSPE.getIndex(), nTCol = nSCol - 1;
	 * OSMColor tSCol = tSPE.getColor(), tTCol = null;
	 * 
	 * while (nTCol > 0)
	 * {
	 * if (mColorList.get(nTCol).getMIndex() == -1)
	 * {
	 * tTCol = mColorList.get(nTCol).getColor();
	 * if ((tTCol != tSCol) && (nNewDiff = tTCol.oDist(tSCol)) < dDiff)
	 * {
	 * tTPE = mColorList.get(nTCol);
	 * dDiff = nNewDiff;
	 * log.trace("DIFF: " + tSPE + " to " + tTPE + " diff=" + nNewDiff + "; qDiff=" + tTCol.qDist(tSCol));
	 * }
	 * }
	 * nTCol--;
	 * }
	 * map(tTPE, tSPE);
	 * return tTPE;
	 * }
	 */

	/**
	 * performs a limited linear search for the best matching color. It only tries to substitute with a color which is at least used as often as the original -
	 * this means, it only tests against the entries forward in the palette.
	 * It is used to make a initial matching affecting only near colors.
	 * 
	 * @param tSPE
	 *          - the 'source' entry in the palette
	 * @param nMaxDiff
	 *          - only a difference smaller then nMaxDiff will be counted as match
	 * @return the best matching entry in the palette
	 */
	/*
	 * protected OSMCB2PaletteEntry substituteLim(OSMCB2PaletteEntry tSPE, int nMaxDiff)
	 * {
	 * OSMCB2PaletteEntry tTPE = null;
	 * long nDiff = Integer.MAX_VALUE;
	 * long nNewDiff = 0;
	 * int nSCol = tSPE.getIndex(), nTCol = nSCol - 1;
	 * OSMColor tSCol = tSPE.getColor(), tTCol = null;
	 * 
	 * while ((nTCol > 0) && (nDiff > nMaxDiff))
	 * {
	 * tTCol = mColorList.get(nTCol).getColor();
	 * if ((tTCol != tSCol) && ((nNewDiff = tTCol.qDist(tSCol)) < nDiff) && (nNewDiff < nMaxDiff))
	 * {
	 * tTPE = mColorList.get(nTCol);
	 * nDiff = nNewDiff;
	 * log.trace("DIFF: " + tSPE + " to " + tTPE + " diff=" + nNewDiff);
	 * }
	 * nTCol--;
	 * }
	 * if (tTPE != null)
	 * {
	 * map(tTPE, tSPE);
	 * }
	 * // else
	 * // log.trace(tSPE + " no match");
	 * 
	 * return tTPE;
	 * }
	 */

	/**
	 * performs a limited linear search for the best matching color. It only tries to substitute with a color which is at least used as often as the original -
	 * this means, it only tests against the entries forward in the palette.
	 * It is used to make a initial matching affecting only near colors.
	 * 
	 * @param tSPE
	 *          - the 'source' entry in the palette
	 * @param nMaxDiff
	 *          - only a difference smaller then nMaxDiff will be counted as match
	 * @return the best matching entry in the palette
	 */
	/*
	 * protected OSMCB2PaletteEntry substituteLim(OSMCB2PaletteEntry tSPE, int nMaxDiff)
	 * {
	 * OSMCB2PaletteEntry tTPE = null;
	 * long nDiff = Integer.MAX_VALUE;
	 * long nNewDiff = 0;
	 * int nSCol = tSPE.getIndex(), nTCol = nSCol - 1;
	 * OSMColor tSCol = tSPE.getColor(), tTCol = null;
	 * 
	 * while ((nTCol > 0) && (nDiff > nMaxDiff))
	 * {
	 * tTCol = mColorList.get(nTCol).getColor();
	 * if ((tTCol != tSCol) && ((nNewDiff = tTCol.qDist(tSCol)) < nDiff) && (nNewDiff < nMaxDiff))
	 * {
	 * tTPE = mColorList.get(nTCol);
	 * nDiff = nNewDiff;
	 * log.trace("DIFF: " + tSPE + " to " + tTPE + " diff=" + nNewDiff);
	 * }
	 * nTCol--;
	 * }
	 * if (tTPE != null)
	 * {
	 * map(tTPE, tSPE);
	 * }
	 * // else
	 * // log.trace(tSPE + " no match");
	 * 
	 * return tTPE;
	 * }
	 */

	/**
	 * performs a linear search for the best matching color. It only tries to substitute with a color which is at least used as often as the original - this
	 * means, it only tests against the entries forward in the palette
	 * it is only matched against unmatched colors to avoid chaining
	 * 
	 * @param tCol1
	 *          - the color to be matched against the existing entries in the palette
	 * @return the best matching entry in the palette
	 */
	/*
	 * protected OSMCB2PaletteEntry substituteOpt(OSMCB2PaletteEntry tSPE)
	 * {
	 * OSMCB2PaletteEntry tTPE = tSPE;
	 * double dDiff = 1e200;
	 * double nNewDiff = 0;
	 * int nSCol = tSPE.getIndex(), nTCol = nSCol - 1;
	 * OSMColor tSCol = tSPE.getColor(), tTCol = null;
	 * 
	 * while (nTCol > 0)
	 * {
	 * if (mColorList.get(nTCol).getMIndex() == -1)
	 * {
	 * tTCol = mColorList.get(nTCol).getColor();
	 * if ((tTCol != tSCol) && (nNewDiff = tTCol.oDist(tSCol)) < dDiff)
	 * {
	 * tTPE = mColorList.get(nTCol);
	 * dDiff = nNewDiff;
	 * log.trace("DIFF: " + tSPE + " to " + tTPE + " diff=" + nNewDiff + "; qDiff=" + tTCol.qDist(tSCol));
	 * }
	 * }
	 * nTCol--;
	 * }
	 * tTPE.map(tSPE);
	 * return tTPE;
	 * }
	 */

	/**
	 * performs a limited linear search for the best matching color. It only tries to substitute with a color which is used less often than the original -
	 * this means, it only tests against the entries upward in the palette.
	 * It is used to make an initial matching affecting only similar colors.
	 * 
	 * @param tSPE
	 *          - the 'source' entry in the palette
	 * @param nMaxDiff
	 *          - only a difference smaller then nMaxDiff will be counted as match
	 * @return the best matching entry in the palette
	 */
	/*
	 * protected OSMCB2PaletteEntry substituteLimUp(OSMCB2PaletteEntry tSPE, int nMaxDiff)
	 * {
	 * OSMCB2PaletteEntry tTPE = null;
	 * long nDiff = Integer.MAX_VALUE;
	 * long nNewDiff = 0;
	 * int nSCol = tSPE.getIndex(), nTCol = nSCol + 1;
	 * OSMColor tSCol = tSPE.getColor(), tTCol = null;
	 * 
	 * while ((nTCol < mColorList.size()) && (nDiff > nMaxDiff))
	 * {
	 * tTCol = mColorList.get(nTCol).getColor();
	 * if ((tTCol != tSCol) && ((nNewDiff = tTCol.qDist(tSCol)) < nDiff) && (nNewDiff < nMaxDiff))
	 * {
	 * tTPE = mColorList.get(nTCol);
	 * nDiff = nNewDiff;
	 * log.info("DIFF: " + tSPE + " to " + tTPE + " diff=" + nNewDiff);
	 * }
	 * nTCol++;
	 * }
	 * if (tTPE != null)
	 * {
	 * map(tSPE, tTPE);
	 * log.info("Mapped: " + tTPE + " to " + tSPE);
	 * }
	 * // else
	 * // log.trace(tSPE + " no match");
	 * 
	 * return tTPE;
	 * }
	 */

	/**
	 * Looks for the color best matching the current one. Both colors(indices) will be put into the TreeMap mMatchesTM,
	 * which is sorted by the distance of the two colors .
	 * It allows to solve mapping in the sequence of the distances between the color pairs.
	 * 
	 * @param nTCol
	 */
	/*
	 * @SuppressWarnings("unused") // W #unused
	 * private void findBestMatch(int nTCol)
	 * {
	 * OSMCB2PaletteEntry tTPE = mColorList.get(nTCol);
	 * OSMCB2PaletteEntry tMPE = tTPE;
	 * 
	 * OSMColor tTCol = tTPE.getColor();
	 * double dDiff = 1e100;
	 * int nSCol = 0, nMCol = 0;
	 * int nCLSize = mColorList.size();
	 * 
	 * for (nSCol = nTCol + 1; nSCol < nCLSize; nSCol++)
	 * {
	 * OSMCB2PaletteEntry tSPE = mColorList.get(nSCol);
	 * OSMColor tSCol = tSPE.getColor();
	 * double dNewDiff = 0;
	 * 
	 * if (tSPE.getCount() == 0)
	 * break;
	 * 
	 * if ((tSPE.getCount() > 0) && ((dNewDiff = tTCol.oDist(tSCol)) < dDiff) && (nTCol != nSCol))
	 * {
	 * tMPE = mColorList.get(nSCol);
	 * nMCol = nSCol;
	 * dDiff = dNewDiff;
	 * log.trace("DIFF: " + tMPE + " to " + tTPE + " oDiff=" + dNewDiff + "; qDiff=" + tTCol.qDist(tSCol));
	 * }
	 * }
	 * // Fill list with color distances.
	 * log.debug("match DIFF: " + tMPE + " to " + tTPE + " oDiff=" + dDiff);
	 * mMatchesTM.put(dDiff, new OSMCB2ColorPair(nTCol, nMCol));
	 * }
	 */

	/**
	 * Looks for the color best matching the current one. Both colors(indices) will be put into the TreeMap mMatchesTM,
	 * which is sorted by the distance of the two colors .
	 * It allows to solve mapping in the sequence of the distances between the color pairs.
	 * 
	 * @param nTCol
	 */
	/*
	 * private void findBestMatchUp(int nTCol)
	 * {
	 * OSMCB2PaletteEntry tTPE = mColorList.get(nTCol);
	 * OSMCB2PaletteEntry tMPE = tTPE;
	 * ;
	 * OSMColor tTCol = tTPE.getColor();
	 * double dDiff = 1e100;
	 * int nSCol = 0, nMCol = 0;
	 * int nCLSize = mColorList.size();
	 * 
	 * for (nSCol = nTCol - 1; nSCol > 0; nSCol--)
	 * {
	 * OSMCB2PaletteEntry tSPE = mColorList.get(nSCol);
	 * OSMColor tSCol = tSPE.getColor();
	 * double dNewDiff = 0;
	 * 
	 * if (tSPE.getCount() == 0)
	 * break;
	 * 
	 * if ((tSPE.getCount() > 0) && ((dNewDiff = tTCol.oDist(tSCol)) < dDiff) && (nTCol != nSCol))
	 * {
	 * tMPE = mColorList.get(nSCol);
	 * nMCol = nSCol;
	 * dDiff = dNewDiff;
	 * log.trace("DIFF: " + tTPE + " to " + tMPE + " oDiff=" + dNewDiff + "; qDiff=" + tTCol.qDist(tSCol));
	 * }
	 * }
	 * // Fill list with color distances.
	 * log.debug("match DIFF: " + tTPE + " to " + tMPE + " oDiff=" + dDiff);
	 * mMatchesTM.put(dDiff, new OSMCB2ColorPair(nMCol, nTCol));
	 * }
	 */

	/**
	 * reorders the mColorList. Readjusts the MIdx entries in the list
	 *
	 * @param tTgt
	 *          the target entry, which the source is mapped to
	 * @param tPE
	 *          the source entry which is mapped to the target
	 */
	/*
	 * private void reorder(OSMCB2PaletteEntry tTgt, OSMCB2PaletteEntry tSrc)
	 * {
	 * if (tTgt != null)
	 * {
	 * int nTgtCnt = tTgt.getCount();
	 * int nTgtColor = tTgt.getIndex();
	 * int nTestColor = nTgtColor - 1;
	 * int nTgtModColor = nTgtColor;
	 * // check if the target entry has to be moved upwards in the list
	 * if ((nTgtColor > 1) && (mColorList.get(nTestColor).getCount() < nTgtCnt))
	 * {
	 * // look at the entries upwards in the list
	 * while ((mColorList.get(nTestColor - 1).getCount() < nTgtCnt) && (nTestColor - 1 > 0))
	 * {
	 * nTestColor--;
	 * }
	 * nTgtModColor = nTestColor;
	 * mColorList.remove(nTgtColor);
	 * tTgt.setIndex(nTgtModColor);
	 * mColorList.add(nTgtModColor, tTgt);
	 * tSrc.setMIndex(nTgtModColor);
	 * }
	 * int nSrcColor = tSrc.getIndex();
	 * mColorList.remove(nSrcColor);
	 * tSrc.setCount(0);
	 * mColorList.add(tSrc);
	 * // don't modify the just added entry again, it is already correct
	 * for (nTestColor++; nTestColor < mColorList.size() - 1; nTestColor++)
	 * {
	 * OSMCB2PaletteEntry tTestColor = mColorList.get(nTestColor);
	 * tTestColor.setIndex(nTestColor);
	 * int nMIdx = tTestColor.getMIndex();
	 * if (nMIdx != -1)
	 * {
	 * if ((nMIdx >= nTgtModColor) && (nMIdx < nTgtColor))
	 * tTestColor.setMIndex(nMIdx + 1);
	 * else if (nMIdx == nSrcColor)
	 * tTestColor.setMIndex(nTgtModColor);
	 * else if (nMIdx > nSrcColor)
	 * tTestColor.setMIndex(nMIdx - 1);
	 * }
	 * }
	 * }
	 * }
	 */

}
