package osmcb.utilities.image;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;

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
public class OSMAdaptivePaletteList implements IfOSMPalette
{
	protected static Logger log = Logger.getLogger(OSMAdaptivePaletteList.class);

	/*
	 * 1 HashMap with <color, pIndex> - allows to fast find the palette index of a color by value. pIndex is [-1...127]. -1 means mapped to another color.
	 * 2 SortedList with <usage count, color> - sorted by usage count. If a color is mapped, aka the usage count is 0 it is removed from this list.
	 * 3 SortedList with best matching pairs <distance, color1, color2> - sorted by distance. There is one entry for each color1 in the list.
	 */

	protected class OSMColorUseInfo
	{
		int mUsageCnt = 0;
		OSMColor mColor = null;

		public OSMColorUseInfo()
		{
			mUsageCnt = 1;
		}

		public OSMColorUseInfo(OSMColor tColor)
		{
			this();
			mColor = tColor;
		}

		/**
		 * @return The usage count after incrementing.
		 */
		protected int incUsageCnt()
		{
			return ++mUsageCnt;
		}
	}

	/**
	 * SortedList with <usage count, color> - sorted by usage count. If a color is mapped, aka the usage count is 0, it is removed from this list.
	 * 
	 * @author humbach
	 */
	protected class OSMColorUsageList extends ArrayList<OSMColorUseInfo>
	{
		protected OSMColorUsageList(int nSize)
		{
			super(nSize);
		}

		/**
		 * This adds the color to the list. If it did not exist there yet, it gets the usage count 1, else the current count is incremented by 1.
		 * 
		 * @param tColor
		 *          The color to be used.
		 */
		protected int add(OSMColor tColor)
		{
			int nCnt = 1;
			if (this.contains(tColor))
			{
				nCnt = get(indexOf(tColor)).incUsageCnt();
			}
			else
				add(new OSMColorUseInfo(tColor));
			return nCnt;
		}
	}

	class OSMMatchingColorPair
	{
		private double mDist = 1e200;
		private int mSrcColor = 0;
		private int mTgtColor = 0;

		OSMMatchingColorPair(double dist, int nSrcColor, int nTgtColor)
		{
			mDist = dist;
			mSrcColor = nSrcColor;
			mTgtColor = nTgtColor;
		}

		// OSMMatchingColorPair(double dist, OSMColor srcColor, OSMColor tgtColor)
		// {
		// mDist = dist;
		// mSrcColor = srcColor;
		// mTgtColor = tgtColor;
		// }
	};

	/**
	 * SortedList with best matching pairs <distance, color1, color2> - sorted by distance. There is one entry for each color1 in the list.
	 */
	protected class OSMMatchingColorList extends ArrayList<OSMMatchingColorPair>
	{

	}

	// instance data
	/**
	 * Supposedly the color IDs start with 1 in bsb/kap format.
	 * OpenCPN uses color 0 as well (2016-01).
	 */
	private int mStartColorIdx = 0;
	private int mPaletteCnt = 128; // the BSB-KAP allows up to 128 colors, including the (supposedly unused) color 0
	private int mStdColors = 64;

	/**
	 * 'index' Map containing all colors found in the map image. It maps the color values to the palette index.
	 * Index is [0...127].
	 * 1 HashMap with <color, pIndex> - allows to fast find the palette index of a color by value. pIndex is [-1...127]. -1 means mapped to another color.
	 */
	protected HashMap<OSMColor, Integer> mColorsHM = new HashMap<OSMColor, Integer>();

	protected OSMColorUsageList mColorUseList = new OSMColorUsageList(mPaletteCnt);

	protected OSMMatchingColorList mColorMatchList = new OSMMatchingColorList();

	public OSMAdaptivePaletteList()
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
	public OSMAdaptivePaletteList(BufferedImage img)
	{
		this();
		try
		{
			// Initialize some (64) standard colors
			put(new OSMColor(0, 0, 0), 0);
			mColorsHM.put(new OSMColor(255, 0, 0), 1);
			mColorsHM.put(new OSMColor(0, 255, 0), 2);
			mColorsHM.put(new OSMColor(0, 0, 255), 3);
			mColorsHM.put(new OSMColor(255, 255, 0), 4);
			mColorsHM.put(new OSMColor(0, 255, 255), 5);
			mColorsHM.put(new OSMColor(255, 0, 255), 6);
			mColorsHM.put(new OSMColor(255, 255, 255), 7);
			mColorsHM.put(new OSMColor(127, 127, 127), 8);
			mColorsHM.put(new OSMColor(181, 208, 208), 9);
			mColorsHM.put(new OSMColor(228, 198, 171), 10);
			mColorsHM.put(new OSMColor(177, 139, 190), 11);
			mColorsHM.put(new OSMColor(235, 219, 232), 12);
			mColorsHM.put(new OSMColor(181, 181, 146), 13);
			mColorsHM.put(new OSMColor(241, 238, 232), 14);
			mColorsHM.put(new OSMColor(173, 208, 158), 15);
			mColorsHM.put(new OSMColor(238, 237, 229), 16);
			mColorsHM.put(new OSMColor(178, 211, 163), 17);
			mColorsHM.put(new OSMColor(228, 233, 218), 18);
			mColorsHM.put(new OSMColor(193, 218, 180), 19);
			mColorsHM.put(new OSMColor(220, 229, 210), 20);
			mColorsHM.put(new OSMColor(185, 214, 171), 21);
			mColorsHM.put(new OSMColor(190, 216, 177), 22);
			mColorsHM.put(new OSMColor(213, 226, 202), 23);
			mColorsHM.put(new OSMColor(232, 234, 222), 24);
			mColorsHM.put(new OSMColor(172, 204, 198), 25);
			mColorsHM.put(new OSMColor(181, 212, 169), 26);
			mColorsHM.put(new OSMColor(201, 221, 189), 27);
			mColorsHM.put(new OSMColor(197, 219, 185), 28);
			mColorsHM.put(new OSMColor(204, 222, 193), 29);
			mColorsHM.put(new OSMColor(209, 224, 197), 30);
			mColorsHM.put(new OSMColor(164, 204, 149), 31);
			mColorsHM.put(new OSMColor(217, 228, 205), 32);
			mColorsHM.put(new OSMColor(225, 232, 214), 33);
			mColorsHM.put(new OSMColor(197, 216, 213), 34);
			mColorsHM.put(new OSMColor(234, 236, 225), 35);
			mColorsHM.put(new OSMColor(245, 195, 120), 36);
			mColorsHM.put(new OSMColor(229, 116, 140), 37);
			mColorsHM.put(new OSMColor(245, 201, 135), 38);
			mColorsHM.put(new OSMColor(220, 220, 220), 39);
			mColorsHM.put(new OSMColor(242, 232, 216), 40);
			mColorsHM.put(new OSMColor(185, 211, 200), 41);
			mColorsHM.put(new OSMColor(244, 213, 167), 42);
			mColorsHM.put(new OSMColor(151, 151, 149), 43);
			mColorsHM.put(new OSMColor(123, 123, 123), 44);
			mColorsHM.put(new OSMColor(185, 210, 209), 45);
			mColorsHM.put(new OSMColor(226, 229, 225), 46);
			mColorsHM.put(new OSMColor(244, 219, 183), 47);
			mColorsHM.put(new OSMColor(229, 232, 227), 48);
			mColorsHM.put(new OSMColor(219, 227, 220), 49);
			mColorsHM.put(new OSMColor(226, 228, 219), 50);
			mColorsHM.put(new OSMColor(248, 247, 244), 51);
			mColorsHM.put(new OSMColor(203, 219, 217), 52);
			mColorsHM.put(new OSMColor(243, 226, 200), 53);
			mColorsHM.put(new OSMColor(168, 168, 165), 54);
			mColorsHM.put(new OSMColor(134, 136, 133), 55);
			mColorsHM.put(new OSMColor(245, 205, 147), 56);
			mColorsHM.put(new OSMColor(201, 200, 197), 57);
			mColorsHM.put(new OSMColor(245, 209, 154), 58);
			mColorsHM.put(new OSMColor(184, 184, 181), 59);
			mColorsHM.put(new OSMColor(233, 234, 229), 60);
			mColorsHM.put(new OSMColor(211, 215, 202), 61);
			mColorsHM.put(new OSMColor(149, 197, 137), 62);
			mColorsHM.put(new OSMColor(213, 224, 220), 63);
			for (int y = 0; y < img.getHeight(); ++y)
			{
				for (int x = 0; x < img.getWidth(); ++x)
				{
					put(new OSMColor(img.getRGB(x, y)));
				}
			}
			log.debug("Palette[" + mColorsHM.size() + "] after put()");
			// log.debug("Colors:" + toString());

			if (mColorUseList.size() > mPaletteCnt)
			{
				reduce();
				log.debug("Palette[" + mColorsHM.size() + "] after reduce()");
				// log.trace("Colors:" + toString());
				// log.trace("BSB:" + asBSBStr());
			}
			else
				log.debug("Palette[" + mColorsHM.size() + "] no reduction neccessary");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			log.error("Exception while constructing palette. " + e.getMessage());
		}
	}

	/**
	 * Reduces the palette to mPaletteCnt entries by mapping the least used entries to the mPaletteCnt most used ones.
	 * This is done recursively from the start on.
	 */
	protected void reduce()
	{
		// The palette is traversed in 'normal' order, which is descending in the usage count. This means, often used colors are mapped first, and hopefully
		// prevents 'color drifting' while chained mapping.
		// It stops when the usage count of the color has reached 0, so colors which have already been mapped earlier are skipped.
		// The first round maps neatly matching colors, so the most often used colors move to the front of the palette.

		// The HashMap does not 'reorder' when colors have been matched, so it goes trough all colors every time.
		// Either we find a way to update the HashMap and both iterators or we need a 'specialized' structure here.
		log.trace("start");

		for (int nTgtColor = mStartColorIdx; nTgtColor < mColorUseList.size(); nTgtColor++)
		{
			// Map.Entry<OSMColor, ColorInfo> tPE = iColor.next();
			// OSMColor tColor = tPE.getKey();
			// if (tPE.getValue().getMColor() != null)
			// log.trace("tgt color=[" + nSrcColor + "], " + tColor.toStringKmpl() + ", cnt=" + tPE.getValue().getCount() + " mapped to= "
			// + tPE.getValue().getMColor().toStringRGB());

			if (mColorUseList.get(nTgtColor).mUsageCnt > 0)
			{
				OSMColor tTgtColor = mColorUseList.get(nTgtColor).mColor;
				// log.trace("tgt color=[" + nSrcColor + "], " + tColor.toStringKmpl() + ", cnt=" + tPE.getValue().getCount());

				// for (Iterator<Map.Entry<OSMColor, ColorInfo>> iSrcColor = mColorsHM.getLessUsageIt(tPE); iSrcColor.hasNext();)
				for (int nSrcColor = nTgtColor + 1; nSrcColor < mColorUseList.size(); nSrcColor++)
				{
					// Map.Entry<OSMColor, ColorInfo> tSPE = iSrcColor.next();
					// if (tSPE != tPE)
					// {
					OSMColor tSrcColor = mColorUseList.get(nSrcColor).mColor;
					if ((mColorUseList.get(nSrcColor).mUsageCnt > 0) && (tTgtColor.qDist(tSrcColor) < 17))
					{
						// map the source color to the target
						map(tSrcColor, tTgtColor);
					}
					else
					{
						if (mColorUseList.get(nSrcColor).mUsageCnt == 0)
							log.debug("source color unused");
					}
				}
				// log.trace("tgt after=[" + nSrcColor + "], " + tColor.toStringKmpl() + ", cnt=" + tPE.getValue().getCount());
			}
			else
			{
				log.debug("target color unused");
			}
			// nSrcColor++;
		}

		log.trace("\r\n");
		log.debug("Palette[" + mColorsHM.size() + "] after first round()");
		// log.debug("Colors:" + toString());

		// Second round: now map the colors more different. Here one has to be careful. We use OSMColor.oDiff() instead of OSMColor.dDiff()
		// For each color in the list the best matching color is found. Then the best color match, i.e. the one with the least difference is mapped.
		// This is repeated until only the 127 colors in the map remain.
		// We need a sorted map with the distances and the indices

		for (int nTgtColor = mStartColorIdx; nTgtColor < mColorUseList.size(); nTgtColor++)
		{
			double dBestDist = findBestMatch(nTgtColor, true);
		}

		log.trace("\r\n");
		log.debug("Palette[" + mColorsHM.size() + "] after findBestMatch()");
		// log.trace("Colors:" + toString());

		int nTstColor = mStartColorIdx;

		// while ((mColorUseList.size() >= 128) && (nTstColor < 20000))
		while (mColorUseList.size() >= 128)
		{
			OSMMatchingColorPair tPair = mColorMatchList.get(0);
			int nTgtColor = tPair.mTgtColor;

			// map(tPair.mSrcColor, tPair.mTgtColor);
			findBestMatch(nTgtColor, true);
			log.trace("tgt rematch");
			// }
			// else
			// {
			// if (tCI.getCount() >= mColorsHM.getCount(tCBM))
			// {
			// // map best match to this color
			// tCI.incCount(mColorsHM.get(tCBM).setMColor(tPE.getKey()));
			// // log.trace("tgt < color[" + nTstColor + "]=RGB(" + tPE.getKey().toStringRGB() + "), HSL(" + tPE.getKey().toStringHSL() + "), cnt=" + tCI.getCount()
			// // + ", < best match mapped=RGB(" + tCBM.toStringRGB() + "), m-cnt=" + mColorsHM.getCount(tCBM) + ", dist=" + tCI.getDist());
			// findBestMatch(tPE, true);
			// ++nSrcColor;
			// }
			// else
			// {
			// // map this color to the best match
			// // tPE.getValue().incCount(mColorsHM.get(tPE.getValue().getBestMatch()).setMColor(tPE.getKey()));
			// mColorsHM.get(tCBM).incCount(tCI.setMColor(tCBM));
			// log.trace("tgt > color[" + nTstColor + "]=RGB(" + tPE.getKey().toStringRGB() + "), HSL(" + tPE.getKey().toStringHSL() + "), cnt=" + tCI.getCount()
			// + ", mapped to RGB(" + tCBM.toStringRGB() + "), m-cnt=" + mColorsHM.getCount(tCBM) + ", dist=" + tCI.getDist());
			// ++nSrcColor;
			// }
			// mColorsHM.decMColorCnt();
			// }
			// ++nTstColor;
			// }
			// log.trace("mapped colors[" + nSrcColor + "] of [" + nTstColor + "]-" + mColorsHM.getMColorCnt());
			//
			// // collect the actual indices in the final palette
			// if (mColorUseList.size() < 128)
			// {
			// for (int nSrcColor = 0; nSrcColor < mColorUseList.size(); nSrcColor++)
			// {
			//
			// }
			// }
			// nSrcColor = mStartColorIdx;
			// for (Iterator<Map.Entry<OSMColor, ColorInfo>> iColor = mColorsHM.getUsageIt(); iColor.hasNext();)
			// {
			// Map.Entry<OSMColor, ColorInfo> tPE = iColor.next();
			// if (nSrcColor < mPaletteCnt)
			// {
			// tPE.getValue().setPIdx(nSrcColor);
			// }
			// else
			// {
			// int nIdx = 0;
			// OSMColor tColor = tPE.getKey();
			// OSMColor tMColor = null;
			// while ((tMColor = mColorsHM.get(tColor).getMColor()) != null)
			// {
			// tColor = tMColor;
			// }
			// nIdx = mColorsHM.get(tColor).getPIdx();
			// tPE.getValue().setPIdx(nIdx);
			// if (tColor != tPE.getKey())
			// tPE.getValue().setMColor(tColor);
			// }
			// ++nSrcColor;
			// }
		}
		log.trace("\r\n");
		log.debug("Palette[" + mColorsHM.size() + "] after reduce():");
		// log.trace("Colors:" + toString());
	}

	/**
	 * Looks for the color best matching the current one.
	 * Both colors(indices) will be put into the TreeMap mMatchesTM, which is sorted by the distance of the two colors .
	 * It allows to solve mapping in the sequence of the distances between the color pairs.
	 * 
	 * @param nTgtColor
	 *          ID of the target color
	 */
	private double findBestMatch(int nTstColor, boolean bAll)
	{
		OSMColor tTstColor = mColorUseList.get(nTstColor).mColor;
		double dNewDiff = 0.0;
		double dDist = 1e200; // some sufficient high number

		if (mColorUseList.get(nTstColor).mUsageCnt > 0)
		{
			int nSrcColor = 0;
			int nBestMatch = 0;
			if (bAll)
				nSrcColor = mStartColorIdx;
			else
				nSrcColor = nTstColor + 1;
			for (; nSrcColor < mColorUseList.size(); nSrcColor++)
			{
				if (mColorUseList.get(nSrcColor).mUsageCnt > 0)
				{
					OSMColor tSrcColor = mColorUseList.get(nSrcColor).mColor;
					dNewDiff = tSrcColor.oDist(tTstColor);
					if (dNewDiff < dDist)
					{
						nBestMatch = nSrcColor;
						dDist = dNewDiff;
					}
				}
			}
			// Fill list with color distances.
			mColorMatchList.add(new OSMMatchingColorPair(dDist, nTstColor, nBestMatch));
		}
		else
		{
			log.trace("test color already mapped");
		}
		return dDist;
	}

	/**
	 * 
	 * @param tSrcColor
	 *          The color to be mapped.
	 * @param tTgtColor
	 *          The color to be used instead of the original color.
	 */
	public void map(OSMColor tSrcColor, OSMColor tTgtColor)
	{
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

		for (int nColor = mStartColorIdx; nColor < mPaletteCnt; nColor++)
		{
			strPal += "RGB/" + nColor + "," + mColorUseList.get(nColor).mColor.toStringRGB() + "\r\n";
		}
		return strPal;
	}

	/**
	 * This creates a specific String in a format suited for logging or tracing. It lists all colors in the palette.
	 */
	@Override
	public String toString()
	{
		String strPal = "\r\n";
		strPal += "Palette: cnt=" + mColorUseList.size();
		for (int nColor = mStartColorIdx; nColor < mColorUseList.size(); nColor++)
		{
			OSMColor tColor = mColorUseList.get(nColor).mColor;
			int nIdx = mColorsHM.get(tColor);
			if (nColor != nIdx)
				strPal += "[" + nColor + "]=" + tColor.toStringKmpl() + ", mapped to=RGB(" + mColorUseList.get(nIdx).mColor.toStringRGB() + ")";
			else
				strPal += "[" + nColor + "]=" + tColor.toStringKmpl() + ", unmapped)";
			strPal += "\r\n";
		}
		return strPal;
	}

	/**
	 * places a color in the palette. If the color is not yet included, a new entry is added, else the count of the existing entry is incremented
	 * 
	 * @param tCol
	 * @param idx
	 *          Index in the palette to put the color into. All following colors will be shifted by one.
	 * @return usage count of the color
	 */
	public int put(OSMColor tCol, int idx)
	{
		int nCnt = mColorsHM.put(tCol, idx);
		return nCnt;
	}

	/**
	 * Places the specified color in the palette.
	 */
	@Override
	public int put(OSMColor tCol)
	{
		mColorUseList.add(tCol);
		return mColorsHM.put(tCol, null);
	}

	@Override
	public int getPID(OSMColor tColor)
	{
		return mColorsHM.get(tColor);
	}
}
