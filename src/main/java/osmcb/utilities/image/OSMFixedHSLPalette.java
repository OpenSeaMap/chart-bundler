package osmcb.utilities.image;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * the @class is used to assign the 127 colors available in the TIFF in KAP to the most used colors in the image.
 * It uses a fixed palette in the HSL color space.
 * We use hues spaced by 15°,
 * five steps of Lightness (additional to white and black),
 * and depending on the lightness up to three steps of saturation (at 50% lightness)
 * Then the color used by a pixel can directly be mapped to one of the colors in the palette directly without comparison to the other colors
 * 
 * We need to
 * 1. look up the existing entries in the palette to check if the current color is already in the palette
 * 2. count the number of pixels if more than one is using the current color
 * 3. NOT sort the table according to the number of pixels to find the most and the least used colors to do the matching
 * 4. find the best fitting color to the current one
 * 5. remember the matching when coding the image
 * 
 * @author humbach
 *
 * @param <tPE>
 */
public class OSMFixedHSLPalette implements IFOSMPalette
{
	/**
	 * 20150812 AH v1 initial version
	 */
	private static final long serialVersionUID = 1L;
	protected final Logger log;

	protected class OSMUsageCnt
	{
		private int mID; // the colors ID
		private int mCnt; // the usage count of this color in the image

		OSMUsageCnt(int nID, int nCnt)
		{
			mID = nID;
			mCnt = nCnt;
		}

		int getID()
		{
			return mID;
		}

		int getCnt()
		{
			return mCnt;
		}

		int incCnt()
		{
			return ++mCnt;
		}
	}

	/**
	 * 'index' Map containing all colors found in the map image. It maps the color values to the palette index.
	 */
	private HashMap<OSMColor, Integer> mColorsHM = new HashMap<OSMColor, Integer>();
	/**
	 * Map containing the 'best matching pairs'. It maps the distance and the (target and the source) indices in the palette.
	 * It is sorted by ascending distance.
	 * !This is an insecure usage since it is not guaranteed that there are not two ColorPairs with the same distance!
	 * But even in the very unlikely case, that there are - and they are the least distance of all too, we don't give a damn and use one of the pairs.
	 * */
	private TreeMap<Double, OSMColorPair> mMatchesTM = new TreeMap<Double, OSMColorPair>();
	/**
	 * List containing all colors found in the map image. It is initially sorted by usage count.
	 * It will be resorted if the usage count changes due to mapping.
	 * A new sorting would invalidates the entries in mHM. The modified entries have to be updated via mColorsHM.put()
	 */
	private ArrayList<OSMPaletteEntry> mColorList = new ArrayList<OSMPaletteEntry>(128);

	/**
	 * List containing all colors found in the map image. It is sorted by usage count.
	 */
	private ArrayList<OSMUsageCnt> mColorUseList = new ArrayList<OSMUsageCnt>(128);

	private int mPaletteCnt = 128; // the BSB-KAP allows up to 128 colors, including the unused color 0
	private int mStdColors = 0;

	class OSMColorPair
	{
		int mIdx;
		int mMIdx;

		OSMColorPair(int nIdx, int nMIdx)
		{
			mIdx = nIdx;
			mMIdx = nMIdx;
		}
	};

	/**
	 * creates the palette of the given image with 'adaptive' colors
	 *
	 * @param img
	 */
	public OSMFixedHSLPalette(BufferedImage img)
	{
		log = Logger.getLogger(this.getClass());
		// Init entry 0, which is not used by OpenCPN
		mColorList.add(0, new OSMPaletteEntry(new OSMColor(0), 0, 0));
		// Init the standard colors
		int saturation = 0, hue = 0, lightness = 0;
		for (lightness = 0; lightness <= 100;)
		{
			switch (lightness)
			{
				case 0:
				case 100:
					hue = 0;
					saturation = 0;
					put(new OSMColor(OSMColor.HSL2RGB(hue, saturation, lightness)));
					break;
				case 17:
				case 83:
					hue = 0;
					saturation = 0;
					put(new OSMColor(OSMColor.HSL2RGB(hue, saturation, lightness)));
					saturation = 100;
					for (hue = 15; hue <= 360; hue += 15)
					{
						put(new OSMColor(OSMColor.HSL2RGB(hue, saturation, lightness)));
					}
					break;
				case 33:
				case 67:
					hue = 0;
					saturation = 0;
					put(new OSMColor(OSMColor.HSL2RGB(hue, saturation, lightness)));
					for (saturation = 50; saturation <= 100; saturation += 50)
						for (hue = 15; hue <= 360; hue += 15)
						{
							put(new OSMColor(OSMColor.HSL2RGB(hue, saturation, lightness)));
						}
					break;
				case 50:
					hue = 0;
					saturation = 0;
					put(new OSMColor(OSMColor.HSL2RGB(hue, saturation, lightness)));
					for (saturation = 34; saturation <= 100; saturation += 33)
						for (hue = 15; hue <= 360; hue += 15)
						{
							put(new OSMColor(OSMColor.HSL2RGB(hue, saturation, lightness)));
						}
					break;
			}
			switch (lightness)
			{
				case 0:
					lightness = 17;
					break;
				case 17:
					lightness = 33;
					break;
				case 33:
					lightness = 50;
					break;
				case 50:
					lightness = 67;
					break;
				case 67:
					lightness = 83;
					break;
				case 83:
					lightness = 100;
					break;
				case 100:
					lightness = 101;
					break;
			}
		}
	}

	/**
	 * reduces the palette to mPaletteCnt entries by mapping the least used entries to the mPaletteCnt most used ones.
	 * this is done recursively from the end on.
	 */
	// public void reduceBwd()
	// {
	// // the palette is traversed in reverse sorting order, which is in ascending usage count order
	// for (int nCol = mColorList.size() - 1; nCol >= 2; nCol--)
	// {
	// // OSMPaletteEntry tPE = mCL.remove(nCol);
	// OSMPaletteEntry tPE = mColorList.get(nCol);
	// OSMPaletteEntry tTgt = substituteLim(tPE, 17);
	// if (tTgt != null)
	// {
	// int nTgtCnt = tTgt.getCount();
	// // check if the modified entry has to be moved upwards in the list
	// int nTgtCol = tTgt.getIndex();
	// int nTgtNCol = 0;
	// int nPCol = nTgtCol - 1;
	// if ((nTgtCol > 1) && (mColorList.get(nPCol).getCount() < nTgtCnt))
	// {
	// while ((mColorList.get(nPCol - 1).getCount() < nTgtCnt) && (nPCol - 1 > 0))
	// {
	// nPCol--;
	// }
	// nTgtNCol = nPCol;
	// mColorList.remove(nTgtCol);
	// tTgt.setIndex(nTgtNCol);
	// mColorList.add(nTgtNCol, tTgt);
	// tPE.setMIndex(nTgtNCol);
	// }
	// mColorList.remove(nCol);
	// tPE.setCount(0);
	// mColorList.add(tPE);
	// for (nPCol++; nPCol <= mColorList.size() - 1; nPCol++)
	// {
	// tPE = mColorList.get(nPCol);
	// tPE.setIndex(nPCol);
	// int nMIdx = tPE.getMIndex();
	// if (nMIdx == nCol)
	// tPE.setMIndex(nTgtNCol);
	// else if ((nMIdx >= nTgtNCol) && (nMIdx < nTgtCol))
	// tPE.setMIndex(nMIdx + 1);
	// if (nMIdx > nCol)
	// tPE.setMIndex(nMIdx - 1);
	// }
	// }
	// }
	//
	// for (int nCol = mColorList.size() - 1; nCol >= mPaletteCnt; nCol--)
	// {
	// // OSMPaletteEntry tPE = mCL.remove(nCol);
	// OSMPaletteEntry tPE = mColorList.get(nCol);
	// if (tPE.getCount() > 0)
	// {
	// OSMPaletteEntry tTgt = substitute(tPE);
	// // nCnt = tTgt.map(tPE);
	// int nTgtCnt = tTgt.getCount();
	// // check if the modified entry has to be moved upwards in the list
	// int nTgtCol = mColorList.indexOf(tTgt);
	// int nTgtNCol = 0;
	// int nPCol = nTgtCol - 1;
	// if ((nTgtCol > 1) && (mColorList.get(nPCol).getCount() < nTgtCnt))
	// {
	// while ((mColorList.get(nPCol - 1).getCount() < nTgtCnt) && (nPCol - 1 > 0))
	// {
	// nPCol--;
	// }
	// nTgtNCol = nPCol;
	// mColorList.remove(nTgtCol);
	// tTgt.setIndex(nTgtNCol);
	// mColorList.add(nTgtNCol, tTgt);
	// tPE.setMIndex(nTgtNCol);
	// }
	// mColorList.remove(nCol);
	// tPE.setCount(0);
	// mColorList.add(tPE);
	// // adjust the idx data
	// for (nPCol++; nPCol < mColorList.size(); nPCol++)
	// {
	// tPE = mColorList.get(nPCol);
	// tPE.setIndex(nPCol);
	// int nMIdx = tPE.getMIndex();
	// if (nMIdx == nCol)
	// tPE.setMIndex(nTgtNCol);
	// else if ((nMIdx >= nTgtNCol) && (nMIdx < nTgtCol))
	// tPE.setMIndex(nMIdx + 1);
	// if (nMIdx > nCol)
	// tPE.setMIndex(nMIdx - 1);
	// }
	// }
	// }
	// }

	/**
	 * reduces the palette to mPaletteCnt entries by mapping the least used entries to the mPaletteCnt most used ones.
	 * this is done recursively from the start on.
	 */
	public void reduce()
	{
		int nCLSize = mColorList.size(); // the size of the list will not change, mapped - hence unused - colors will be moved to the end of the list.
		// The palette is traversed in 'normal' order, which is descending in the usage count. This means, often used colors are mapped first, and hopefully
		// prevents color drifting while chained mapping.
		// It stops when the usage count of the color has reached 0, so colors which have already been mapped earlier are skipped.
		// The first round maps neatly matching colors, so the most often used colors move to the front of the palette
		log.debug("reduce(): start");

		for (int nTCol = 1; nTCol < nCLSize - 1; nTCol++)
		{
			OSMPaletteEntry tTPE = mColorList.get(nTCol);
			for (int nSCol = nTCol + 1; nSCol < nCLSize; ++nSCol)
			{
				OSMPaletteEntry tSPE = mColorList.get(nSCol);

				if (tTPE.getColor().qDist(tSPE.getColor()) < 17)
				{
					// map(tTPE, tSPE);
				}
			}
		}

		log.debug("\r\n");
		log.debug("reduce(): first round finished\r\n");

		// second round, map the colors above 127 to the 127 most used colors
		// while (mColorList.get(mPaletteCnt).getCount() != 0)
		// // for (int nSCol = mPaletteCnt; nSCol < nCLSize; nSCol++)
		// {
		// OSMPaletteEntry tSPE = mColorList.get(mPaletteCnt);
		// OSMPaletteEntry tMPE = mColorList.get(mPaletteCnt);
		// double nNewDiff = 0, nDiff = 1e100;
		// if (tSPE.getCount() == -1)
		// break;
		// for (int nTCol = 0; nTCol < mPaletteCnt; nTCol++)
		// {
		// OSMPaletteEntry tTPE = mColorList.get(nTCol);
		// if ((nNewDiff = tSPE.getColor().oDiff(tTPE.getColor())) < nDiff)
		// {
		// nDiff = nNewDiff;
		// tMPE = tTPE;
		// }
		// }
		// map(tMPE, tSPE);
		// }

		log.debug("\r\n");
		log.debug("reduce(): second round finished\r\n");

		// // Second round: now map the colors more different. Here one has to be careful. We use OSMColor.oDiff() instead of OSMColor.dDiff()
		// // For each color in the list the best matching color is found. Then the best color match, i.e. the one with the least difference is mapped.
		// // This is repeated until only the 127 colors in the map remain.
		// // We need a sorted map with the distances and the indices
		// for (int nTCol = 1; nTCol < (nCLSize - 1); nTCol++)
		// {
		// if (mColorList.get(nTCol).getCount() == 0)
		// break;
		// findBestMatch(nTCol);
		// }
		//
		// while (mColorList.get(mPaletteCnt).getCount() > 0)
		// {
		// // Now look for the best overall matching pair
		// Entry<Double, OSMColorPair> tME = mMatchesTM.pollFirstEntry();
		// OSMColorPair tCP = tME.getValue();
		// int nTCol = tCP.mIdx;
		// int nSCol = tCP.mMIdx;
		// if ((nSCol < mColorList.size()) && (nTCol < mColorList.size()))
		// {
		// OSMPaletteEntry tSPE = mColorList.get(nSCol);
		// OSMPaletteEntry tTPE = mColorList.get(nTCol);
		// log.trace("map: " + tSPE + " to " + tTPE + " diff=" + tME.getKey());
		// map(tTPE, tSPE);
		// }
		// else
		// log.debug("map match: Tgt=" + nTCol + " or Src=" + nSCol + " invalid: " + mColorList.size());
		// }
	}

	/**
	 * Looks for the color best matching the current one. Both colors(indices) will be put into the TreeMap mMatchesTM,
	 * which is sorted by the distance of the two colors .
	 * It allows to solve mapping in the sequence of the distances between the color pairs.
	 * 
	 * @param nTCol
	 */
	private void findBestMatch(int nTCol)
	{
		OSMPaletteEntry tTPE = mColorList.get(nTCol);
		OSMPaletteEntry tMPE = tTPE;
		;
		OSMColor tTCol = tTPE.getColor();
		double dDiff = 1e100;
		int nSCol = 0, nMCol = 0;
		int nCLSize = mColorList.size();

		for (nSCol = nTCol + 1; nSCol < nCLSize; nSCol++)
		{
			OSMPaletteEntry tSPE = mColorList.get(nSCol);
			OSMColor tSCol = tSPE.getColor();
			double dNewDiff = 0;

			// if (tSPE.getCount() == 0)
			// break;
			//
			// if ((tSPE.getCount() > 0) && ((dNewDiff = tTCol.oDiff(tSCol)) < dDiff) && (nTCol != nSCol))
			// {
			// tMPE = mColorList.get(nSCol);
			// nMCol = nSCol;
			// dDiff = dNewDiff;
			// log.trace("DIFF: " + tMPE + " to " + tTPE + " oDiff=" + dNewDiff + "; qDiff=" + tTCol.qDiff(tSCol));
			// }
		}
		// Fill list with color distances.
		log.debug("match DIFF: " + tMPE + " to " + tTPE + " oDiff=" + dDiff);
		mMatchesTM.put(dDiff, new OSMColorPair(nTCol, nMCol));
	}

	/**
	 * Looks for the color best matching the current one. Both colors(indices) will be put into the TreeMap mMatchesTM,
	 * which is sorted by the distance of the two colors .
	 * It allows to solve mapping in the sequence of the distances between the color pairs.
	 * 
	 * @param nTCol
	 */
	// private void findBestMatchUp(int nTCol)
	// {
	// OSMPaletteEntry tTPE = mColorList.get(nTCol);
	// OSMPaletteEntry tMPE = tTPE;
	// ;
	// OSMColor tTCol = tTPE.getColor();
	// double dDiff = 1e100;
	// int nSCol = 0, nMCol = 0;
	// int nCLSize = mColorList.size();
	//
	// for (nSCol = nTCol - 1; nSCol > 0; nSCol--)
	// {
	// OSMPaletteEntry tSPE = mColorList.get(nSCol);
	// OSMColor tSCol = tSPE.getColor();
	// double dNewDiff = 0;
	//
	// if (tSPE.getCount() == 0)
	// break;
	//
	// if ((tSPE.getCount() > 0) && ((dNewDiff = tTCol.oDiff(tSCol)) < dDiff) && (nTCol != nSCol))
	// {
	// tMPE = mColorList.get(nSCol);
	// nMCol = nSCol;
	// dDiff = dNewDiff;
	// log.trace("DIFF: " + tTPE + " to " + tMPE + " oDiff=" + dNewDiff + "; qDiff=" + tTCol.qDiff(tSCol));
	// }
	// }
	// // Fill list with color distances.
	// log.debug("match DIFF: " + tTPE + " to " + tMPE + " oDiff=" + dDiff);
	// mMatchesTM.put(dDiff, new OSMColorPair(nMCol, nTCol));
	// }

	/*
	 * (non-Javadoc)
	 * 
	 * @see osmcb.utilities.image.IFOSMPalette#asBSBStr()
	 */
	@Override
	public String asBSBStr()
	{
		String strPal = "";

		OSMPaletteEntry tPE = null;
		for (int nCol = 1; nCol < Math.min(mPaletteCnt, mColorList.size()); nCol++)
		{
			tPE = mColorList.get(nCol);
			strPal += "RGB/" + nCol + "," + tPE.getColor().toStringRGB() + "\r\n";
		}
		return strPal;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see osmcb.utilities.image.IFOSMPalette#toString()
	 */
	@Override
	public String toString()
	{
		String strPal = "\r\n";

		OSMPaletteEntry tPE = null;
		for (int nCol = 1; nCol < mColorList.size(); nCol++)
		{
			tPE = mColorList.get(nCol);
			// strPal += "Palette: Color[" + nCol + "]=(" + tPE.getColor().toStringRGB() + "), Cnt=" + tPE.getCount() + ", IDX=" + tPE.mColIdx + ", MI=" + tPE.mMapIdx
			// + ", HSL=(" + tPE.getColor().toStringHSL() + ")\r\n";
		}
		return strPal;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see osmcb.utilities.image.IFOSMPalette#put(osmcb.utilities.image.OSMColor)
	 */
	@Override
	public int put(OSMColor tCol)
	{
		int nCnt = 1;
		int nID = 0;

		nID = -1;

		if (nID > -1)
		{
			nCnt = mColorUseList.get(nID).incCnt();
		}
		else
		{
			// append the entry to the list, it is used once
			nID = mColorList.size();
			OSMPaletteEntry tPE = new OSMPaletteEntry(tCol);
			mColorList.add(tPE);
			mColorsHM.put(tPE.getColor(), nID);
			mColorUseList.add(new OSMUsageCnt(nID, 1));
		}
		return nCnt;
	}

	@Override
	public int getPID(OSMColor tColor)
	{
		return 0;
	}
	/*
	 * (non-Javadoc)
	 * 
	 * @see osmcb.utilities.image.IFOSMPalette#getIdx(osmcb.utilities.image.OSMColor)
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
	// protected OSMPaletteEntry substitute(OSMPaletteEntry tSPE)
	// {
	// OSMPaletteEntry tTPE = tSPE;
	// double dDiff = 1e200;
	// double nNewDiff = 0;
	// int nSCol = tSPE.getIndex(), nTCol = nSCol - 1;
	// OSMColor tSCol = tSPE.getColor(), tTCol = null;
	//
	// while (nTCol > 0)
	// {
	// if (mColorList.get(nTCol).getMIndex() == -1)
	// {
	// tTCol = mColorList.get(nTCol).getColor();
	// if ((tTCol != tSCol) && (nNewDiff = tTCol.oDiff(tSCol)) < dDiff)
	// {
	// tTPE = mColorList.get(nTCol);
	// dDiff = nNewDiff;
	// log.trace("DIFF: " + tSPE + " to " + tTPE + " diff=" + nNewDiff + "; qDiff=" + tTCol.qDiff(tSCol));
	// }
	// }
	// nTCol--;
	// }
	// map(tTPE, tSPE);
	// return tTPE;
	// }

	/**
	 * performs a linear search for the best matching color. It only tries to substitute with a color which is at least used as often as the original - this
	 * means, it only tests against the entries forward in the palette
	 * it is only matched against unmatched colors to avoid chaining
	 * 
	 * @param tCol1
	 *          - the color to be matched against the existing entries in the palette
	 * @return the best matching entry in the palette
	 */
	// protected OSMPaletteEntry substituteOpt(OSMPaletteEntry tSPE)
	// {
	// OSMPaletteEntry tTPE = tSPE;
	// double dDiff = 1e200;
	// double nNewDiff = 0;
	// int nSCol = tSPE.getIndex(), nTCol = nSCol - 1;
	// OSMColor tSCol = tSPE.getColor(), tTCol = null;
	//
	// while (nTCol > 0)
	// {
	// if (mColorList.get(nTCol).getMIndex() == -1)
	// {
	// tTCol = mColorList.get(nTCol).getColor();
	// if ((tTCol != tSCol) && (nNewDiff = tTCol.oDiff(tSCol)) < dDiff)
	// {
	// tTPE = mColorList.get(nTCol);
	// dDiff = nNewDiff;
	// log.trace("DIFF: " + tSPE + " to " + tTPE + " diff=" + nNewDiff + "; qDiff=" + tTCol.qDiff(tSCol));
	// }
	// }
	// nTCol--;
	// }
	// tTPE.map(tSPE);
	// return tTPE;
	// }
	//
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
	// protected OSMPaletteEntry substituteLim(OSMPaletteEntry tSPE, int nMaxDiff)
	// {
	// OSMPaletteEntry tTPE = null;
	// long nDiff = Integer.MAX_VALUE;
	// long nNewDiff = 0;
	// int nSCol = tSPE.getIndex(), nTCol = nSCol - 1;
	// OSMColor tSCol = tSPE.getColor(), tTCol = null;
	//
	// while ((nTCol > 0) && (nDiff > nMaxDiff))
	// {
	// tTCol = mColorList.get(nTCol).getColor();
	// if ((tTCol != tSCol) && ((nNewDiff = tTCol.qDiff(tSCol)) < nDiff) && (nNewDiff < nMaxDiff))
	// {
	// tTPE = mColorList.get(nTCol);
	// nDiff = nNewDiff;
	// log.trace("DIFF: " + tSPE + " to " + tTPE + " diff=" + nNewDiff);
	// }
	// nTCol--;
	// }
	// if (tTPE != null)
	// {
	// map(tTPE, tSPE);
	// }
	// // else
	// // log.trace(tSPE + " no match");
	//
	// return tTPE;
	// }

	/**
	 * performs a limited linear search for the best matching color. It only tries to substitute with a color which is used less often as the original -
	 * this means, it only tests against the entries upward in the palette.
	 * It is used to make an initial matching affecting only similar colors.
	 * 
	 * @param tSPE
	 *          - the 'source' entry in the palette
	 * @param nMaxDiff
	 *          - only a difference smaller then nMaxDiff will be counted as match
	 * @return the best matching entry in the palette
	 */
	// protected OSMPaletteEntry substituteLimUp(OSMPaletteEntry tSPE, int nMaxDiff)
	// {
	// OSMPaletteEntry tTPE = null;
	// long nDiff = Integer.MAX_VALUE;
	// long nNewDiff = 0;
	// int nSCol = tSPE.getIndex(), nTCol = nSCol + 1;
	// OSMColor tSCol = tSPE.getColor(), tTCol = null;
	//
	// while ((nTCol < mColorList.size()) && (nDiff > nMaxDiff))
	// {
	// tTCol = mColorList.get(nTCol).getColor();
	// if ((tTCol != tSCol) && ((nNewDiff = tTCol.qDiff(tSCol)) < nDiff) && (nNewDiff < nMaxDiff))
	// {
	// tTPE = mColorList.get(nTCol);
	// nDiff = nNewDiff;
	// log.info("DIFF: " + tSPE + " to " + tTPE + " diff=" + nNewDiff);
	// }
	// nTCol++;
	// }
	// if (tTPE != null)
	// {
	// map(tSPE, tTPE);
	// log.info("Mapped: " + tTPE + " to " + tSPE);
	// }
	// // else
	// // log.trace(tSPE + " no match");
	//
	// return tTPE;
	// }

	// protected void map(OSMPaletteEntry tTgt, OSMPaletteEntry tPE)
	// {
	// log.debug("map: " + tPE + " to " + tTgt);
	// tTgt.map(tPE);
	// reorder(tTgt, tPE);
	// }

	// /**
	// * reorders the mColorList. Readjusts the MIdx entries in the list
	// *
	// * @param tTgt
	// * the target entry, which the source is mapped to
	// * @param tPE
	// * the source entry which is mapped to the target
	// */
	// private void reorder(OSMPaletteEntry tTgt, OSMPaletteEntry tSrc)
	// {
	// if (tTgt != null)
	// {
	// int nTgtCnt = tTgt.getCount();
	// int nTgtColor = tTgt.getIndex();
	// int nTestColor = nTgtColor - 1;
	// int nTgtModColor = nTgtColor;
	// // check if the target entry has to be moved upwards in the list
	// if ((nTgtColor > 1) && (mColorList.get(nTestColor).getCount() < nTgtCnt))
	// {
	// // look at the entries upwards in the list
	// while ((mColorList.get(nTestColor - 1).getCount() < nTgtCnt) && (nTestColor - 1 > 0))
	// {
	// nTestColor--;
	// }
	// nTgtModColor = nTestColor;
	// mColorList.remove(nTgtColor);
	// tTgt.setIndex(nTgtModColor);
	// mColorList.add(nTgtModColor, tTgt);
	// tSrc.setMIndex(nTgtModColor);
	// }
	// int nSrcColor = tSrc.getIndex();
	// mColorList.remove(nSrcColor);
	// tSrc.setCount(0);
	// mColorList.add(tSrc);
	// // don't modify the just added entry again, it is already correct
	// for (nTestColor++; nTestColor < mColorList.size() - 1; nTestColor++)
	// {
	// OSMPaletteEntry tTestColor = mColorList.get(nTestColor);
	// tTestColor.setIndex(nTestColor);
	// int nMIdx = tTestColor.getMIndex();
	// if (nMIdx != -1)
	// {
	// if ((nMIdx >= nTgtModColor) && (nMIdx < nTgtColor))
	// tTestColor.setMIndex(nMIdx + 1);
	// else if (nMIdx == nSrcColor)
	// tTestColor.setMIndex(nTgtModColor);
	// else if (nMIdx > nSrcColor)
	// tTestColor.setMIndex(nMIdx - 1);
	// }
	// }
	// }
	// }
}
