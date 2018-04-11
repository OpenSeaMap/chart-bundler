package osmcb.utilities.image;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import osmb.utilities.OSMBStrs;
import osmcb.OSMCBStrs;

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
public class OSMCB3AdaptivePalette implements IfOSMPalette
{
	protected static Logger log = Logger.getLogger(OSMCB3AdaptivePalette.class);

	/*
	 * 1 HashMap with <color, pIndex> - allows to fast find the palette index of a color by value. pIndex is [-1...127]. -1 means mapped to another color.
	 * 2 SortedList with <usage count, color> - sorted by usage count. If a color is mapped, aka the usage count is 0, it is removed from this list.
	 */

	// 'helper' classes
	/**
	 * The OSMCB3ColorMap encapsulates internally different structures to enable:
	 * 1. A quick access to the index in the palette with a given color value
	 * 
	 * @author humbach
	 */
	public class OSMCB3ColorMap
	{
		/**
		 * This comparator sorts the colors according to their usage count with the most used colors listed first.
		 * Colors with the same usage count are ordered by their rgb color value.
		 */
		final Comparator<OSMCB3ColorInfo> BYCOUNTDESC = new Comparator<OSMCB3ColorInfo>()
		{
			@Override
			public int compare(OSMCB3ColorInfo o1, OSMCB3ColorInfo o2)
			{
				int nRes = 0;
				if ((o1.getPIdx() < mStdColors) || (o2.getPIdx() < mStdColors))
				{
					if (o1.getPIdx() > o2.getPIdx())
						nRes = 1;
					else if (o1.getPIdx() < o2.getPIdx())
						nRes = -1;
				}
				else
				{
					if (o1.getCount() < o2.getCount())
						nRes = 1;
					else if (o1.getCount() > o2.getCount())
						nRes = -1;
					else
					{
						if (o1.getColor().getRGB() < o2.getColor().getRGB())
							nRes = -1;
						else if (o1.getColor().getRGB() > o2.getColor().getRGB())
							nRes = 1;
					}
				}
				return nRes;
			}
		};

		// 'helper' classes
		private class OSMCB3ColorInfo
		{
			private OSMColor mColor; // color value
			private Integer mCnt = 1; // usage count, the default is 1
			private OSMColor mTgtColor = null; // color value of the final target color this color is mapped to
			private Integer mPIdx = 0; // index of the color in the palette

			private OSMCB3ColorInfo(OSMColor tColor, Integer nCnt)
			{
				mColor = tColor;
				mCnt = nCnt;
			}

			private OSMColor getColor()
			{
				return mColor;
			}

			/**
			 * This sets the target color this one is mapped to.
			 * 
			 * @param tColor
			 */
			private void setTgtColor(OSMColor tColor)
			{
				mTgtColor = tColor;
			}

			/**
			 * @return The target color this one is mapped to.
			 */
			private OSMColor getTgtColor()
			{
				return mTgtColor;
			}

			/**
			 * 
			 * @param nCnt
			 *          The count to be set.
			 * @return The usage count after the incrementation.
			 */
			private int setCount(int nCnt)
			{
				mCnt = nCnt;
				return mCnt;
			}

			/**
			 * @param nCnt
			 *          The count to be added to the current.
			 * @return The usage count after the incrementation.
			 */
			private int addCount(int nCnt)
			{
				mCnt += nCnt;
				return mCnt;
			}

			/**
			 * @return The usage count
			 **/
			private int getCount()
			{
				return mCnt;
			}

			/**
			 * @param nIdx
			 *          The index in the usage list to be set for this color.
			 */
			private void setPIdx(int nIdx)
			{
				mPIdx = nIdx;
			}

			/**
			 * @return The index in the usage list. The first 128 entries are used in the final palette.
			 */
			private int getPIdx()
			{
				return mPIdx;
			}

			@Override
			public String toString()
			{
				String str = mColor.toStringKmpl() + ", Cnt=" + mCnt + ", Pidx=" + mPIdx;
				if (mTgtColor != null)
					str += ", mapped to " + mTgtColor.toStringKmpl();
				return str;
			}
		}

		// instance data
		/**
		 * Indicates that colors have been moved in the usage list without modifying their palette index entries in the hash map.
		 */
		private boolean mDirty = false;

		/**
		 * Map of ColorInfos, key is the color value.
		 * Maps color value to ColorInfo. Allows quick access to the ColorInfo by color value.
		 */
		private Map<OSMColor, OSMCB3ColorInfo> mHM = null;

		/**
		 * List of ColorInfos sorted by descending usage count.
		 */
		private List<OSMCB3ColorInfo> mUL = null;
		/**
		 * The number of colors currently used in the color map, i.e. colors unmapped and with (an usage count > 0)
		 */
		private int mUsedColors = 0;

		public OSMCB3ColorMap()
		{
			log = Logger.getLogger(this.getClass());
			mHM = new HashMap<OSMColor, OSMCB3ColorInfo>();
			mUL = new ArrayList<OSMCB3ColorInfo>();
		}

		/**
		 * Adds the specified color to the end of the map if it does not yet exist. A usage count of 1 is assumed.
		 * Otherwise the usage count of the existing entry is incremented by 1.
		 * 
		 * @param tColor
		 * @return The new color ID (or the current ID if the color already existed in map before).
		 */
		public void addOnePixel(OSMColor tColor)
		{
			log.trace(OSMBStrs.RStr("START"));
			addPixels(tColor, 1);
		}

		/**
		 * Adds the specified color to the end of the map if it does not yet exist. A usage count of nCnt is assumed.
		 * Otherwise the usage count of the existing entry is incremented by nCnt.
		 * 
		 * @param tColor
		 * @param nCnt
		 */
		public void addPixels(OSMColor tColor, Integer nCnt)
		{
			log.trace(OSMBStrs.RStr("START"));
			// boolean bFound = false;
			OSMCB3ColorInfo tCI = null;
			if (mHM.containsKey(tColor))
			{
				if (log.isTraceEnabled())
					log.trace("color RGB(" + tColor.toStringRGB() + ") should be in map, increment usage count");
				try
				{
					tCI = mHM.get(tColor);
					if (tCI != null)
					{
						tCI.addCount(nCnt);
						if (log.isTraceEnabled())
							log.trace("usage incremented " + tCI.toString() + " by " + nCnt);
						// else if (sLog.isDebugEnabled())
						// sLog.debug("usage incremented " + tColor.toStringKmpl() + ", Cnt=" + nCnt);
						// repositionColor(tColor);
					}
					else
						log.debug("Unused color=" + tColor);
				}
				// strange things may happen
				catch (IndexOutOfBoundsException e)
				{
					log.debug(OSMCBStrs.RStr("ColorMap.NoColor"));
				}
				catch (Exception e)
				{
					log.debug(e + ", " + e.getCause() + ", " + e.getMessage() + ", " + mUL + ", Cnt=" + nCnt);
				}
			}
			else
			{
				tCI = new OSMCB3ColorInfo(tColor, nCnt);
				tCI.setPIdx(mUL.size());
				mHM.put(tColor, tCI);
				mUL.add(tCI);
				if (log.isTraceEnabled())
					log.trace(tCI.toString() + " added: Cnt=" + nCnt);
				// else if (sLog.isDebugEnabled())
				// sLog.debug(tColor.toStringKmpl() + " added: Cnt=" + nCnt);
				mUsedColors++;
			}
			mDirty = true;
		}

		/**
		 * @param nIdx
		 *          The index in the usage list, which is also the index in the palette.
		 * @return The color at the position nIdx in the final palette.
		 */
		public OSMColor get(int nIdx)
		{
			return mUL.get(nIdx).getColor();
		}

		/**
		 * The index in the palette is the position of the specified color in the usage list. Only the 128 first colors are actually used in the BSB/KAP file.
		 * 
		 * @param tColor
		 * @return The ID of the specified color or null if the color does not exist in map.
		 */
		public int getPIdx(OSMColor tColor)
		{
			log.trace(OSMBStrs.RStr("START"));
			OSMCB3ColorInfo tCI = mHM.get(tColor);
			int nPIdx = 0;
			// readjust the index entries in mHM if necessary.
			if (mDirty)
				readjustPIdxs();
			nPIdx = mHM.get(tColor).getPIdx();
			while (nPIdx >= mUsedColors)
			{
				tColor = mHM.get(tColor).getTgtColor();
				nPIdx = mHM.get(tColor).getPIdx();
			}
			tCI.setPIdx(nPIdx);
			return nPIdx;
		}

		private void reorderUL()
		{
			mUL.sort(BYCOUNTDESC);
		}

		private int compare(OSMCB3ColorInfo tCI1, OSMCB3ColorInfo tCI2)
		{
			return 0;
		}

		/**
		 * This walks over the usage list and sets the mPIdx entries in mHM accordingly.
		 */
		private void readjustPIdxs()
		{
			for (int nIdx = 0; nIdx < mUL.size(); ++nIdx)
			{
				if (log.isTraceEnabled())
				{
					log.trace("PIdxOld=" + mUL.get(nIdx).getPIdx() + ", PIdxNew=" + nIdx);
				}
				mUL.get(nIdx).setPIdx(nIdx);
			}
			mDirty = false;
		}

		/**
		 * @param tColor
		 * @return The usage count of the specified color.
		 */
		public Integer getCnt(OSMColor tColor)
		{
			log.trace(OSMBStrs.RStr("START"));
			Integer nCnt = 0;
			try
			{
				if (mHM.containsKey(tColor))
					nCnt = mHM.get(tColor).getCount();
			}
			catch (IndexOutOfBoundsException e)
			{
				log.debug(OSMCBStrs.RStr("ColorMap.NoColor"));
			}
			return nCnt;
		}

		/**
		 * @return The total count of colors known to this ColorMap.
		 */
		public int size()
		{
			return mHM.size();
		}

		/**
		 * @return The number of colors currently used in this ColorMap.
		 */
		public int getUsedColors()
		{
			// return mUL.size();
			return mUsedColors;
		}

		/**
		 * @return The number of colors in the final palette.
		 */
		public int getPalColors()
		{
			return mPaletteCnt;
		}

		/**
		 * It moves tgtColor forward in the usage index and srcColor will be removed from the index.
		 */
		public void map(OSMColor srcColor, OSMColor tgtColor)
		{
			// readjust pIdx entries if necessary
			if (mDirty)
				readjustPIdxs();
			// change usage count of srcColor and tgtColor
			OSMCB3ColorInfo tSrcInf = mHM.get(srcColor);
			mHM.get(tgtColor).addCount(tSrcInf.getCount());
			tSrcInf.setTgtColor(tgtColor);
			tSrcInf.setCount(0);
			--mUsedColors;

			// remove tSrcColor and push it to the end of mUL
			int nSrcIdx = tSrcInf.getPIdx();
			// tSrcInf.setPIdx(mUL.size() - 1);
			mUL.add(mUL.remove(nSrcIdx));
			for (; nSrcIdx < mUL.size(); ++nSrcIdx)
				mUL.get(nSrcIdx).setPIdx(nSrcIdx);

			// move tTgtColor forward in usage list
			repositionColor(tgtColor);
		}

		private void repositionColor(OSMColor srcColor)
		{
			// move tTgtColor forward in usage list
			OSMCB3ColorInfo tSrcInf = mHM.get(srcColor);
			int nSrcIdx = tSrcInf.getPIdx();
			if (nSrcIdx != mUL.indexOf(tSrcInf))
			{
				// sLog.debug("PIdx wrong, PIdx=" + nTgtIdx + ", pos=" + mUL.indexOf(tTgtInf));
				tSrcInf.setPIdx(mUL.indexOf(tSrcInf));
			}
			// reposition only non-standard colors (>= mStdColorCount)
			if (nSrcIdx >= mStdColors)
			{
				int nTgtCnt = tSrcInf.getCount();
				int nTestIdx = nSrcIdx;
				// find the new position and adjust PIdx entries on the way
				while (--nTestIdx >= mStdColors)
				{
					if (mUL.get(nTestIdx).getCount() >= nTgtCnt)
						break;
					// these will be moved one up
					mUL.get(nTestIdx).setPIdx(nTestIdx + 1);
				}
				++nTestIdx;
				// did it move at all?
				if (nTestIdx < nSrcIdx)
				{
					tSrcInf = mUL.remove(nSrcIdx);
					if (nTestIdx != mUL.get(nTestIdx).getPIdx() - 1)
						log.debug("Tgt=" + mUL.get(nTestIdx).toString() + ", test idx=" + nTestIdx + ", Src=" + tSrcInf.toString());
					else
						log.debug("Tgt=" + mUL.get(nTestIdx).toString() + ", Src=" + tSrcInf.toString() + ", tgt idx=" + nTestIdx);
					tSrcInf.setPIdx(nTestIdx);
					mUL.add(nTestIdx, tSrcInf);

					log.debug("C(n-1)=" + mUL.get(nTestIdx - 1).toString() + ", C(n)=" + mUL.get(nTestIdx).toString() + ", C(n+1)=" + mUL.get(nTestIdx + 1).toString());
				}
				else
					log.debug("color not moved: Src=" + mUL.get(nSrcIdx).toString());
			}
		}

		public int getCnt(int nPIdx)
		{
			return mUL.get(nPIdx).getCount();
		}

	}

	// private static final int STD_COLOR_COUNT = 32;

	// instance data
	/**
	 * 'index' Map containing all colors found in the map image. It maps the color values to the color list index (aka cID).
	 */
	private OSMCB3ColorMap mColorMap = new OSMCB3ColorMap();

	/**
	 * A certain number of colors will be used as standard colors in all palettes, invariant of the actually used colors in the image
	 */
	private int mStdColors = 1;
	/**
	 * This is the number of actually used colors in the final palette.
	 * The BSB-KAP allows up to 128 colors, including the (supposedly unused) color 0. Eventually not all 128 possible colors are used in the current map image.
	 */
	private int mPaletteCnt = 128;

	public OSMCB3AdaptivePalette()
	{
		// adjust to actual implementation
		log = Logger.getLogger(this.getClass());
	}

	/**
	 * Creates the palette of the given image with 'adaptive' colors and some standard colors
	 *
	 * @param img
	 *          - The image for which to create the palette.
	 */
	public OSMCB3AdaptivePalette(BufferedImage img)
	{
		this();
		log.trace(OSMBStrs.RStr("START"));
		try
		{
			// Initialize some standard colors, mStdColorCount will be dynamically registered after this
			addPixel(new OSMColor(255, 255, 255));
			addPixel(new OSMColor(0, 255, 255));
			addPixel(new OSMColor(255, 0, 255));
			addPixel(new OSMColor(255, 255, 0));
			addPixel(new OSMColor(0, 0, 255));
			addPixel(new OSMColor(0, 255, 0));
			addPixel(new OSMColor(255, 0, 0));
			addPixel(new OSMColor(0, 0, 0));
			addPixel(new OSMColor(127, 127, 127));
			addPixel(new OSMColor(145, 145, 145));
			addPixel(new OSMColor(181, 208, 208));
			addPixel(new OSMColor(228, 198, 171));
			addPixel(new OSMColor(177, 139, 190));
			addPixel(new OSMColor(235, 219, 232));
			addPixel(new OSMColor(181, 181, 146));
			addPixel(new OSMColor(241, 238, 232));
			addPixel(new OSMColor(238, 237, 229));
			addPixel(new OSMColor(228, 233, 218));
			addPixel(new OSMColor(220, 229, 210));
			addPixel(new OSMColor(173, 208, 158));
			addPixel(new OSMColor(178, 211, 163));
			addPixel(new OSMColor(185, 214, 171));
			addPixel(new OSMColor(190, 216, 177));
			addPixel(new OSMColor(193, 218, 180));
			addPixel(new OSMColor(213, 226, 202));
			addPixel(new OSMColor(232, 234, 222));
			addPixel(new OSMColor(172, 204, 198));
			addPixel(new OSMColor(181, 212, 169));
			addPixel(new OSMColor(197, 219, 185));
			addPixel(new OSMColor(201, 221, 189));
			addPixel(new OSMColor(204, 222, 193));
			addPixel(new OSMColor(209, 224, 197));
			addPixel(new OSMColor(164, 204, 149));
			addPixel(new OSMColor(248, 178, 156));
			addPixel(new OSMColor(0, 146, 217));
			addPixel(new OSMColor(216, 208, 200));
			addPixel(new OSMColor(137, 210, 174));
			mStdColors = mColorMap.size();

			for (int y = 0; y < img.getHeight(); ++y)
			{
				for (int x = 0; x < img.getWidth(); ++x)
				{
					mColorMap.addPixels(new OSMColor(img.getRGB(x, y)), 1);
					// if (x == 0)
					// {
					// tCol = new OSMColor(img.getRGB(x, y));
					// nCnt++;
					// }
					// else
					// {
					// if ((tNCol = new OSMColor(img.getRGB(x, y))) == tCol)
					// nCnt++;
					// else
					// {
					// mColorsHM.addPixel(tCol, nCnt);
					// tCol = tNCol;
					// nCnt = 1;
					// }
					// }
				}
			}
			mColorMap.reorderUL();
			log.debug("Palette[" + mColorMap.getUsedColors() + "] after put()");
			if (log.isDebugEnabled())
			{
				log.debug("Colors:" + toString());
				testPalette();
			}

			// in the possible case the image contains less than 128 colors adjust the palette size
			if (mColorMap.size() < mPaletteCnt)
				mPaletteCnt = mColorMap.size();

			// if more colors are used than find place in the palette, reduce the number of colors by mapping
			if (mColorMap.getUsedColors() > mPaletteCnt)
			{
				reduce();
				log.debug("Palette[" + mColorMap.getUsedColors() + "] after reduce()");
				if (log.isDebugEnabled())
				{
					testPalette();
				}
			}
			else
				log.debug("Palette[" + mColorMap.getUsedColors() + "] no reduction neccessary");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			log.error("Exception while constructing palette. " + e.getMessage());
		}
	}

	/**
	 * reduces the palette to mPaletteCnt entries by mapping the least used entries to the mPaletteCnt most used ones.
	 */
	public void reduce()
	{
		// The palette is traversed in 'reverse' order, which is ascending in the usage count. This means, seldom used colors are mapped first, and hopefully
		// prevents color drifting while chained mapping.
		// It stops when the number of colors used in the palette has fallen below mPaletteCnt.
		log.trace(OSMBStrs.RStr("START"));

		// The first round maps neatly matching colors, so the most often used colors move to the front of the palette
		OSMColor tTgtColor = null;
		OSMColor tSrcColor = null;
		for (int nTCol = 0; nTCol < mColorMap.getUsedColors() - 1; nTCol++)
		{
			tTgtColor = mColorMap.get(nTCol);
			int nSCol = Math.max(mStdColors, nTCol + 1);
			while (nSCol < mColorMap.getUsedColors())
			{
				tSrcColor = mColorMap.get(nSCol);
				if (tTgtColor.qDiff(tSrcColor) < 17)
				{
					if (log.isTraceEnabled())
						log.trace("colors to map(r1): Src=" + tSrcColor.toStringRGB() + ", " + nSCol + " to Tgt=" + tTgtColor.toStringRGB() + ", " + nTCol);
					if (log.isDebugEnabled())
						log.debug("colors to map(r1): Src=" + tSrcColor.toStringRGB() + ", " + nSCol + " to Tgt=" + tTgtColor.toStringRGB() + ", " + nTCol);
					mColorMap.map(tSrcColor, tTgtColor);
				}
				else
				{
					++nSCol;
				}
			}
		}
		if (log.isDebugEnabled())
		{
			log.debug("\r\n");
			log.debug("Palette[" + mColorMap.getUsedColors() + "] after first round()");
			testPalette();
		}

		// Second round: now map the colors more different.
		// Map the least used color to the best matching one of the first mPaletteCnt colors.
		// This is repeated until at most mPaletteCnt colors in the map remain.
		// We need a sorted map with the distances and the indices
		while (mColorMap.getUsedColors() > mPaletteCnt)
		{
			int nSrcPIdx = mColorMap.getUsedColors() - 1;
			if (log.isTraceEnabled())
				log.trace("colors in usage list=" + nSrcPIdx);
			Integer nTgtPIdx = findBestMatchInPalette(nSrcPIdx);
			if (nTgtPIdx != null)
			{
				tSrcColor = mColorMap.get(nSrcPIdx);
				tTgtColor = mColorMap.get(nTgtPIdx);

				if (log.isTraceEnabled())
					log.trace("colors to map(r2): Src=" + tSrcColor.toStringRGB() + ", " + nSrcPIdx + " to Tgt=" + tTgtColor.toStringRGB() + ", " + nTgtPIdx);
				mColorMap.map(tSrcColor, tTgtColor);
			}
			else
				break;
		}
		if (log.isDebugEnabled())
		{
			log.debug("\r\n");
			log.debug("Palette[" + mColorMap.getUsedColors() + "] after reduce():");
			testPalette();
			log.debug("----\r\n");
		}
	}

	/**
	 * Looks for the color best matching the current one.
	 */
	private Integer findBestMatchInPalette(int nSrcPIdx)
	{
		// Integer tSCID = mColorsHM.getCIDByPIdx(nSrcPIdx);
		// OSMColor tSCol = mColorsHM.getColor(tSCID);
		OSMColor tSCol = mColorMap.get(nSrcPIdx);
		OSMColor tMCol = null;
		Integer nMPIdx = null;
		double dDiff = 1e100;
		log.trace(OSMBStrs.RStr("START"));

		for (int nTgtPIdx = 0; nTgtPIdx < mPaletteCnt; nTgtPIdx++)
		{
			// Integer tTCID = mColorsHM.getCIDByPIdx(nTgtPIdx);
			OSMColor tTCol = mColorMap.get(nTgtPIdx);
			double dNewDiff = 0.0;

			// // stop when unused colors encountered
			// if (mColorsHM.getCnt(tTCID) == 0)
			// break;

			// stop when self testing. This happens when there are less than mPaletteCnt colors in the palette used and nSrcPIdx < mPaletteCnt.
			if (nTgtPIdx == nSrcPIdx)
				break;

			// test if it is a new best distance
			if ((dNewDiff = tSCol.oDist(tTCol)) < dDiff)
			{
				// remember the matching target color
				tMCol = mColorMap.get(nTgtPIdx);
				nMPIdx = nTgtPIdx;
				dDiff = dNewDiff;
				if (log.isTraceEnabled())
					log.trace("DIFF: Src(" + tMCol.toStringRGB() + ") to Tgt(" + tTCol.toStringRGB() + ") oDiff=" + dNewDiff + "; qDiff=" + tTCol.qDiff(tMCol));
			}
		}

		if (tMCol == null)
			log.debug("match null");
		return nMPIdx;
	}

	/**
	 * This adds a pixel to the palette. I.e. the color is added to the map if it is not yet there or if it is already in the map the usage count is increased.
	 * 
	 * @param tColor
	 * @return The color ID of the new color.
	 */
	@Override
	public int put(OSMColor tColor)
	{
		put(tColor, 1);
		return 1;
	}

	/**
	 * This adds nCnt pixels of the same color to the palette. I.e. the color is added to the map if it is not yet there or if it is already in the map the
	 * usage
	 * count is increased.
	 * 
	 * @param tColor
	 * @param nCnt
	 *          The number of same color pixels to add to the palette
	 * @return The color index of the (new) color.
	 */
	public void put(OSMColor tColor, int nCnt)
	{
		mColorMap.addPixels(tColor, nCnt);
	}

	/**
	 * This adds one pixel to the palette. I.e. the color is added to the map if it is not yet there or if it is already in the map the usage count is
	 * increased.
	 * 
	 * @param tColor
	 * @return The color ID of the new color.
	 */
	public void addPixel(OSMColor tColor)
	{
		put(tColor, 1);
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
		log.trace(OSMBStrs.RStr("START"));

		for (int nCol = 1; nCol < mPaletteCnt; nCol++)
		{
			strPal += "RGB/" + nCol + "," + mColorMap.get(nCol).toStringRGB() + "\r\n";
		}
		return strPal;
	}

	/**
	 * This creates a specific String in a format suited for logging or tracing. It lists all colors in the palette.
	 */
	@Override
	public String toString()
	{
		Integer nColors = mColorMap.getUsedColors();
		String strPal = "\r\n";
		log.trace(OSMBStrs.RStr("START"));
		strPal += "Palette: cnt=" + nColors + "\r\n";
		for (int nCol = 0; nCol < nColors; nCol++)
		{
			OSMColor tColor = mColorMap.get(nCol);
			if (nCol < mPaletteCnt)
				strPal += "PIdx[" + nCol + "]=" + tColor.toStringKmpl() + ", Cnt=" + mColorMap.getCnt(tColor) + ", unmapped)";
			else
				strPal += "PIdx[" + nCol + "]=" + tColor.toStringKmpl() + ", Cnt=" + mColorMap.getCnt(tColor) + ", mapped to=null";
			strPal += "\r\n";
		}
		return strPal;
	}

	private void testPalette()
	{
		log.trace(OSMBStrs.RStr("START"));
		if (log.isDebugEnabled())
		{
			boolean bOK = true;
			int nSrcPIdx = 0;
			int nTstCnt = mColorMap.getCnt(mStdColors);
			int nSrcCnt = 0;
			// if dirty, readjust PIdx entries in color map
			if (mColorMap.mDirty)
				mColorMap.readjustPIdxs();
			// step over all following colors, i.e. all colors less used
			for (nSrcPIdx = mStdColors; nSrcPIdx < mColorMap.getUsedColors(); nSrcPIdx++)
			{
				nSrcCnt = mColorMap.getCnt(nSrcPIdx);
				if (nSrcCnt > nTstCnt)
					bOK = false;
				nTstCnt = nSrcCnt;
			}

			if (!bOK)
			{
				log.debug("\r\n");
				log.debug("Palette[" + mColorMap.getUsedColors() + "] dirty");
				log.debug("Colors:" + toString());
			}
		}

	}

	@Override
	public int getPIdx(OSMColor tColor)
	{
		return mColorMap.getPIdx(tColor);
	}
}
