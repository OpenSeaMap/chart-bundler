package osmcb.utilities.image;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * the @class is used to assign the 127 colors available in the TIFF in KAP to the most used colors in the image.
 * We need to
 * 1. look up the existing entries in the palette to check if the current color is already in the palette
 * 2. count the number of pixels if more than one is using the current color
 * 3. sort the table according to the number of pixels to find the most and the least used colors to do the matching
 * 4. find the best fitting color to the current one
 * 5. remember the matching when coding the image
 * 
 * @author humbach
 *
 * @param <tPE>
 */
public class OSMAdaptivePalette
{
	/**
	 * 20140112 AH v1 initial version
	 */
	private static final long serialVersionUID = 1L;
	protected final Logger log;
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
	public OSMAdaptivePalette(BufferedImage img)
	{
		log = Logger.getLogger(this.getClass());
		// Init entry 0, which is not used by OpenCPN
		mColorList.add(0, new OSMPaletteEntry(new OSMColor(0), 0, 0));
		// Init some standard colors
		for (int y = 0; y < img.getHeight(); ++y)
		{
			for (int x = 0; x < img.getWidth(); ++x)
			{
				put(new OSMColor(img.getRGB(x, y)));
			}
		}
		log.debug("Palette[" + mColorList.size() + "] after put():" + toString());
		// populate the hash map with the initial color/index mapping
		for (OSMPaletteEntry tPE : mColorList)
		{
			mColorsHM.put(tPE.getColor(), tPE.getIndex());
		}
		if (mColorList.size() > mPaletteCnt)
		{
			reduceBwd();
			log.debug("Palette[" + mColorList.size() + "] after reduce():" + toString());

			// populate the hash map with the modified color/index mapping
			for (OSMPaletteEntry tPE : mColorList)
			{
				if (tPE.getMIndex() != -1)
					mColorsHM.put(tPE.getColor(), tPE.getMIndex());
				else
					mColorsHM.put(tPE.getColor(), tPE.getIndex());
			}
		}
		else
			log.debug("Palette[" + mColorList.size() + "] no reduction neccessary");
	}

	/**
	 * reduces the palette to mPaletteCnt entries by mapping the least used entries to the mPaletteCnt most used ones.
	 * this is done recursively from the end on.
	 */
	public void reduceBwd()
	{
		int nPCol = 0;
		int nTgtCol = 0;
		int nTgtNCol = 0;
		int nTgtCnt = 0;
		// the palette is traversed in reverse sorting order, which is in ascending usage count order
		for (int nCol = mColorList.size() - 1; nCol >= 2; nCol--)
		{
			// OSMPaletteEntry tPE = mCL.remove(nCol);
			OSMPaletteEntry tPE = mColorList.get(nCol);
			OSMPaletteEntry tTgt = substituteLim(tPE, 17);
			if (tTgt != null)
			{
				nTgtCnt = tTgt.getCount();
				// check if the modified entry has to be moved upwards in the list
				nTgtCol = tTgt.getIndex();
				nPCol = nTgtCol - 1;
				if ((nTgtCol > 1) && (mColorList.get(nPCol).getCount() < nTgtCnt))
				{
					while ((mColorList.get(nPCol - 1).getCount() < nTgtCnt) && (nPCol - 1 > 0))
					{
						nPCol--;
					}
					nTgtNCol = nPCol;
					mColorList.remove(nTgtCol);
					tTgt.setIndex(nTgtNCol);
					mColorList.add(nTgtNCol, tTgt);
					tPE.setMIndex(nTgtNCol);
				}
				mColorList.remove(nCol);
				tPE.setCount(0);
				mColorList.add(tPE);
				for (nPCol++; nPCol <= mColorList.size() - 1; nPCol++)
				{
					tPE = mColorList.get(nPCol);
					tPE.setIndex(nPCol);
					int nMIdx = tPE.getMIndex();
					if (nMIdx == nCol)
						tPE.setMIndex(nTgtNCol);
					else if ((nMIdx >= nTgtNCol) && (nMIdx < nTgtCol))
						tPE.setMIndex(nMIdx + 1);
					if (nMIdx > nCol)
						tPE.setMIndex(nMIdx - 1);
				}
			}
		}

		for (int nCol = mColorList.size() - 1; nCol >= mPaletteCnt; nCol--)
		{
			// OSMPaletteEntry tPE = mCL.remove(nCol);
			OSMPaletteEntry tPE = mColorList.get(nCol);
			if (tPE.getCount() > 0)
			{
				OSMPaletteEntry tTgt = substitute(tPE);
				// nCnt = tTgt.map(tPE);
				nTgtCnt = tTgt.getCount();
				// check if the modified entry has to be moved upwards in the list
				nTgtCol = mColorList.indexOf(tTgt);
				nPCol = nTgtCol - 1;
				if ((nTgtCol > 1) && (mColorList.get(nPCol).getCount() < nTgtCnt))
				{
					while ((mColorList.get(nPCol - 1).getCount() < nTgtCnt) && (nPCol - 1 > 0))
					{
						nPCol--;
					}
					nTgtNCol = nPCol;
					mColorList.remove(nTgtCol);
					tTgt.setIndex(nTgtNCol);
					mColorList.add(nTgtNCol, tTgt);
					tPE.setMIndex(nTgtNCol);
				}
				mColorList.remove(nCol);
				tPE.setCount(0);
				mColorList.add(tPE);
				// adjust the idx data
				for (nPCol++; nPCol < mColorList.size(); nPCol++)
				{
					tPE = mColorList.get(nPCol);
					tPE.setIndex(nPCol);
					int nMIdx = tPE.getMIndex();
					if (nMIdx == nCol)
						tPE.setMIndex(nTgtNCol);
					else if ((nMIdx >= nTgtNCol) && (nMIdx < nTgtCol))
						tPE.setMIndex(nMIdx + 1);
					if (nMIdx > nCol)
						tPE.setMIndex(nMIdx - 1);
				}
			}
		}
	}

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
		Entry<Integer, OSMColor> c = null;
		for (int nTCol = 1; nTCol < nCLSize - 1; nTCol++)
		{
			OSMPaletteEntry tTPE = mColorList.get(nTCol);
			for (int nSCol = nTCol + 1; nSCol < nCLSize; ++nSCol)
			{
				OSMPaletteEntry tSPE = mColorList.get(nSCol);

				if (tSPE.getCount() == 0)
					break;

				if (tTPE.getColor().qDiff(tSPE.getColor()) < 17)
				{
					tTPE.map(tSPE);
				}
			}
			// reached the unused (or already mapped) colors
			if (tTPE.getCount() == 0)
				break;
		}

		// Second round: now map the colors more different. Here one has to be careful. We use OSMColor.oDiff() instead of OSMColor.dDiff()
		// For each color in the list the best matching color is found. Then the best color match, i.e. the one with the least difference is mapped.
		// This is repeated until only the 127 colors in the map remain.
		// We need a sorted map with the distances and the indices
		for (int nTCol = 1; nTCol < (nCLSize - 1); nTCol++)
		{
			findBestMatch(nTCol);
		}

		// Now look for the best overall matching pair
		// OSMColorPair tCP = mTM.ceilingEntry(0.0).getValue();
		Entry<Double, OSMColorPair> tME = mMatchesTM.pollFirstEntry();
		OSMColorPair tCP = tME.getValue();
		int nTCol = tCP.mIdx;
		int nSCol = tCP.mMIdx;
		if ((nSCol < mColorList.size()) && (nTCol < mColorList.size()))
		{
			OSMPaletteEntry tSPE = mColorList.get(nSCol); // oob!
			OSMPaletteEntry tTPE = mColorList.get(nTCol);
			log.info("map: " + tSPE + " to " + tTPE + " diff=" + tME.getKey());
			mColorList.get(nTCol).map(tSPE);
		}
		else
			log.debug("map match: Tgt=" + nTCol + " or Src=" + nSCol + " invalid: " + mColorList.size());
		// Rebuild the entry for the current color.
		findBestMatch(nTCol);
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
		// OSMColor tMCol;
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

			if ((tSPE.getCount() > 0) && ((dNewDiff = tTCol.oDiff(tSCol)) < dDiff) && (nTCol != nSCol))
			{
				tMPE = mColorList.get(nSCol);
				nMCol = nSCol;
				dDiff = dNewDiff;
				log.trace("DIFF: " + tMPE + " to " + tTPE + " oDiff=" + dNewDiff + "; qDiff=" + tTCol.qDiff(tSCol));
			}
		}
		// Fill list with color distances.
		log.debug("match DIFF: " + tMPE + " to " + tTPE + " oDiff=" + dDiff);
		mMatchesTM.put(dDiff, new OSMColorPair(nTCol, nMCol));
	}

	/**
	 * this creates a specific String in the format required by the BSB-KAP file format
	 * it runs at most over the first mPaletteCnt entries in the palette
	 * it ends with a 0x0D,0x0A sequence("\r\n").
	 */
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

	/**
	 * this creates a specific String in a format suited for logging or tracing
	 */
	@Override
	public String toString()
	{
		String strPal = "\r\n";

		OSMPaletteEntry tPE = null;
		for (int nCol = 1; nCol < mColorList.size(); nCol++)
		{
			tPE = mColorList.get(nCol);
			strPal += "Palette: Color[" + nCol + "]=(" + tPE.getColor().toStringRGB() + "), Cnt=" + tPE.getCount() + ", IDX=" + tPE.mColIdx + ", MI=" + tPE.mMapIdx
					+ "\r\n";
		}
		return strPal;
	}

	/**
	 * places a color in the palette. If the color is not yet included, a new entry is added, else the count of the existing entry is incremented
	 * 
	 * @param tCol
	 * @return usage count of the color
	 */
	public int put(OSMColor tCol)
	{
		int nCnt = 1;
		int nCol = 0;
		OSMPaletteEntry tPE = null;
		int nSize = mColorList.size();

		for (nCol = 1; nCol < nSize; nCol++)
		{
			tPE = mColorList.get(nCol);
			if (tPE.getColor().getRGB() == tCol.getRGB())
			{
				nCnt = tPE.getCount() + 1;
				tPE.setCount(nCnt);
				int nPCol = nCol - 1;
				// check if the modified entry has to be moved upwards in the list
				if ((nCol > 1) && (mColorList.get(nPCol).getCount() < nCnt))
				{
					while ((mColorList.get(nPCol - 1).getCount() < nCnt) && (nPCol - 1 > 0))
					{
						nPCol--;
					}
					mColorList.remove(nCol);
					tPE.setIndex(nPCol);
					mColorList.add(nPCol, tPE);
					log.trace("Moved: " + tPE.toString());
					for (nPCol++; nPCol < nSize; nPCol++)
					{
						mColorList.get(nPCol).setIndex(nPCol);
					}
				}
				break;
			}
		}
		if ((mColorList.size() == 0) || (nCol == mColorList.size()))
		{
			// append the entry to the list, it is used once
			tPE = new OSMPaletteEntry(tCol, 1, nCol);
			mColorList.add(tPE);
		}
		return nCnt;
	}

	/**
	 * returns the color index in the palette for the given color. Be aware that KAP palettes start with color number 1 not 0
	 * 
	 * @param tCol
	 * @return
	 */
	public int getIdx(OSMColor tCol)
	{
		int nIdx = 1;
		if (mColorsHM.containsKey(tCol))
			nIdx = mColorsHM.get(tCol);
		return nIdx;
	}

	/**
	 * returns the mapped color index in the palette for the given color. Be aware that KAP palettes start with color number 1 not 0
	 * 
	 * @param tCol
	 * @return
	 */
	public int getMIdx(OSMColor tCol)
	{
		int nIdx = 0;
		if ((nIdx = mColorList.get(getIdx(tCol)).getMIndex()) == -1)
			nIdx = getIdx(tCol);
		return nIdx;
	}

	/**
	 * performs a linear search for the best matching color. It only tries to substitute with a color which is at least used as often as the original - this
	 * means, it only tests against the entries forward in the palette
	 * it is only matched against unmatched colors to avoid chaining
	 * 
	 * @param tCol1
	 *          - the color to be matched against the existing entries in the palette
	 * @return the best matching entry in the palette
	 */
	protected OSMPaletteEntry substitute(OSMPaletteEntry tSPE)
	{
		OSMPaletteEntry tTPE = tSPE;
		double dDiff = 1e200;
		double nNewDiff = 0;
		int nSCol = tSPE.getIndex(), nTCol = nSCol - 1;
		OSMColor tSCol = tSPE.getColor(), tTCol = null;

		while (nTCol > 0)
		{
			if (mColorList.get(nTCol).getMIndex() == -1)
			{
				tTCol = mColorList.get(nTCol).getColor();
				if ((tTCol != tSCol) && (nNewDiff = tTCol.oDiff(tSCol)) < dDiff)
				{
					tTPE = mColorList.get(nTCol);
					dDiff = nNewDiff;
					log.trace("DIFF: " + tSPE + " to " + tTPE + " diff=" + nNewDiff + "; qDiff=" + tTCol.qDiff(tSCol));
				}
			}
			nTCol--;
		}
		tTPE.map(tSPE);
		log.debug("Mapped: " + tSPE + " to " + tTPE);
		return tTPE;
	}

	/**
	 * performs a linear search for the best matching color. It only tries to substitute with a color which is at least used as often as the original - this
	 * means, it only tests against the entries forward in the palette
	 * it is only matched against unmatched colors to avoid chaining
	 * 
	 * @param tCol1
	 *          - the color to be matched against the existing entries in the palette
	 * @return the best matching entry in the palette
	 */
	protected OSMPaletteEntry substituteOpt(OSMPaletteEntry tSPE)
	{
		OSMPaletteEntry tTPE = tSPE;
		double dDiff = 1e200;
		double nNewDiff = 0;
		int nSCol = tSPE.getIndex(), nTCol = nSCol - 1;
		OSMColor tSCol = tSPE.getColor(), tTCol = null;

		while (nTCol > 0)
		{
			if (mColorList.get(nTCol).getMIndex() == -1)
			{
				tTCol = mColorList.get(nTCol).getColor();
				if ((tTCol != tSCol) && (nNewDiff = tTCol.oDiff(tSCol)) < dDiff)
				{
					tTPE = mColorList.get(nTCol);
					dDiff = nNewDiff;
					log.trace("DIFF: " + tSPE + " to " + tTPE + " diff=" + nNewDiff + "; qDiff=" + tTCol.qDiff(tSCol));
				}
			}
			nTCol--;
		}
		tTPE.map(tSPE);
		log.debug("Mapped: " + tSPE + " to " + tTPE);
		return tTPE;
	}

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
	protected OSMPaletteEntry substituteLim(OSMPaletteEntry tSPE, int nMaxDiff)
	{
		OSMPaletteEntry tTPE = null;
		long nDiff = Integer.MAX_VALUE;
		long nNewDiff = 0;
		int nSCol = tSPE.getIndex(), nTCol = nSCol - 1;
		OSMColor tSCol = tSPE.getColor(), tTCol = null;

		while ((nTCol > 0) && (nDiff > nMaxDiff))
		{
			tTCol = mColorList.get(nTCol).getColor();
			if ((tTCol != tSCol) && ((nNewDiff = tTCol.qDiff(tSCol)) < nDiff) && (nNewDiff < nMaxDiff))
			{
				tTPE = mColorList.get(nTCol);
				nDiff = nNewDiff;
				log.trace("DIFF: " + tSPE + " to " + tTPE + " diff=" + nNewDiff);
			}
			nTCol--;
		}
		if (tTPE != null)
		{
			tTPE.map(tSPE);
			log.debug("Mapped: " + tSPE + " to " + tTPE);
		}
		// else
		// log.trace(tSPE + " no match");

		return tTPE;
	}

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
	protected OSMPaletteEntry substituteLimUp(OSMPaletteEntry tSPE, int nMaxDiff)
	{
		OSMPaletteEntry tTPE = null;
		long nDiff = Integer.MAX_VALUE;
		long nNewDiff = 0;
		int nSCol = tSPE.getIndex(), nTCol = nSCol + 1;
		OSMColor tSCol = tSPE.getColor(), tTCol = null;

		while ((nTCol < mColorList.size()) && (nDiff > nMaxDiff))
		{
			tTCol = mColorList.get(nTCol).getColor();
			if ((tTCol != tSCol) && ((nNewDiff = tTCol.qDiff(tSCol)) < nDiff) && (nNewDiff < nMaxDiff))
			{
				tTPE = mColorList.get(nTCol);
				nDiff = nNewDiff;
				log.info("DIFF: " + tSPE + " to " + tTPE + " diff=" + nNewDiff);
			}
			nTCol++;
		}
		if (tTPE != null)
		{
			tSPE.map(tTPE);
			log.info("Mapped: " + tTPE + " to " + tSPE);
		}
		// else
		// log.trace(tSPE + " no match");

		return tTPE;
	}

	/**
	 * 
	 * @param tTgt
	 * @param tPE
	 */
	private void reorder(OSMPaletteEntry tTgt, OSMPaletteEntry tPE)
	{
		if (tTgt != null)
		{
			int nTgtCnt = tTgt.getCount();
			// check if the modified entry has to be moved upwards in the list
			int nTgtCol = tTgt.getIndex();
			int nPCol = nTgtCol - 1;
			int nTgtNCol = 0;
			if ((nTgtCol > 1) && (mColorList.get(nPCol).getCount() < nTgtCnt))
			{
				while ((mColorList.get(nPCol - 1).getCount() < nTgtCnt) && (nPCol - 1 > 0))
				{
					nPCol--;
				}
				nTgtNCol = nPCol;
				mColorList.remove(nTgtCol);
				tTgt.setIndex(nTgtNCol);
				mColorList.add(nTgtNCol, tTgt);
				tPE.setMIndex(nTgtNCol);
			}
			int nCol = tPE.getIndex();
			mColorList.remove(nCol);
			tPE.setCount(0);
			mColorList.add(tPE);
			for (nPCol++; nPCol <= mColorList.size() - 1; nPCol++)
			{
				tPE = mColorList.get(nPCol);
				tPE.setIndex(nPCol);
				int nMIdx = tPE.getMIndex();
				if (nMIdx == nCol)
					tPE.setMIndex(nTgtNCol);
				else if ((nMIdx >= nTgtNCol) && (nMIdx < nTgtCol))
					tPE.setMIndex(nMIdx + 1);
				if (nMIdx > nCol)
					tPE.setMIndex(nMIdx - 1);
			}
		}
	}
}
