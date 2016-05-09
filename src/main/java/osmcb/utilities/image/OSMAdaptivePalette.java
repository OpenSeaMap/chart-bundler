package osmcb.utilities.image;

import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.Map;

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
public class OSMAdaptivePalette implements IfOSMPalette
{
	protected static Logger log = Logger.getLogger(OSMAdaptivePalette.class);

	/**
	 * 'index' Map containing all colors found in the map image. It maps color value to color IDs.
	 * It contains one entry for each color found in the image.
	 */
	private OSMCBColorMap mColorsHM = new OSMCBColorMap();

	/**
	 * Supposedly the color IDs start with 1 in bsb/kap format.
	 * OpenCPN uses color 0 as well (2016-01).
	 */
	private int mStartColorIdx = 0;
	private int mPaletteCnt = 128; // the BSB-KAP allows up to 128 colors, including the (supposedly unused) color 0
	@SuppressWarnings("unused") // W #unused
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
	 * Creates the palette of the given image with 'adaptive' colors
	 *
	 * @param img
	 *          - The image for which to create the palette.
	 */
	public OSMAdaptivePalette(BufferedImage img)
	{
		try
		{
			// Initialize some standard colors
			mColorsHM.put(new OSMColor(0, 0, 0), new ColorInfo(2047000000));
			mColorsHM.put(new OSMColor(255, 0, 0), new ColorInfo(2046000000));
			mColorsHM.put(new OSMColor(0, 255, 0), new ColorInfo(2045000000));
			mColorsHM.put(new OSMColor(0, 0, 255), new ColorInfo(2044000000));
			mColorsHM.put(new OSMColor(255, 255, 0), new ColorInfo(2043000000));
			mColorsHM.put(new OSMColor(0, 255, 255), new ColorInfo(2042000000));
			mColorsHM.put(new OSMColor(255, 0, 255), new ColorInfo(2041000000));
			mColorsHM.put(new OSMColor(255, 255, 255), new ColorInfo(2040000000));
			mColorsHM.put(new OSMColor(127, 127, 127), new ColorInfo(2039000000));
			mColorsHM.put(new OSMColor(181, 208, 208), new ColorInfo(2038000000));
			mColorsHM.put(new OSMColor(228, 198, 171), new ColorInfo(2037000000));
			mColorsHM.put(new OSMColor(177, 139, 190), new ColorInfo(2036000000));
			// mColorsHM.put(new OSMColor(235, 219, 232), new ColorInfo(2035000000));
			// mColorsHM.put(new OSMColor(181, 181, 146), new ColorInfo(2034000000));
			// mColorsHM.put(new OSMColor(241, 238, 232), new ColorInfo(2033000000));
			// mColorsHM.put(new OSMColor(173, 208, 158), new ColorInfo(2033000000));
			// mColorsHM.put(new OSMColor(238, 237, 229), new ColorInfo(2032000000));
			// mColorsHM.put(new OSMColor(178, 211, 163), new ColorInfo(2031000000));
			// mColorsHM.put(new OSMColor(228, 233, 218), new ColorInfo(2030000000));
			// mColorsHM.put(new OSMColor(193, 218, 180), new ColorInfo(2029000000));
			// mColorsHM.put(new OSMColor(220, 229, 210), new ColorInfo(2028000000));
			// mColorsHM.put(new OSMColor(185, 214, 171), new ColorInfo(2027000000));
			// mColorsHM.put(new OSMColor(190, 216, 177), new ColorInfo(2026000000));
			// mColorsHM.put(new OSMColor(213, 226, 202), new ColorInfo(2025000000));
			// mColorsHM.put(new OSMColor(232, 234, 222), new ColorInfo(2024000000));
			// mColorsHM.put(new OSMColor(172, 204, 198), new ColorInfo(2023000000));
			// mColorsHM.put(new OSMColor(181, 212, 169), new ColorInfo(2022000000));
			// mColorsHM.put(new OSMColor(201, 221, 189), new ColorInfo(2021000000));
			// mColorsHM.put(new OSMColor(197, 219, 185), new ColorInfo(2020000000));
			// mColorsHM.put(new OSMColor(204, 222, 193), new ColorInfo(2019000000));
			// mColorsHM.put(new OSMColor(209, 224, 197), new ColorInfo(2018000000));
			// mColorsHM.put(new OSMColor(164, 204, 149), new ColorInfo(2017000000));
			// mColorsHM.put(new OSMColor(217, 228, 205), new ColorInfo(2016000000));
			// mColorsHM.put(new OSMColor(225, 232, 214), new ColorInfo(2015000000));
			// mColorsHM.put(new OSMColor(197, 216, 213), new ColorInfo(2014000000));
			// mColorsHM.put(new OSMColor(234, 236, 225), new ColorInfo(2013000000));
			// mColorsHM.put(new OSMColor(245, 195, 120), new ColorInfo(2012000000));
			// mColorsHM.put(new OSMColor(229, 116, 140), new ColorInfo(2011000000));
			// mColorsHM.put(new OSMColor(245, 201, 135), new ColorInfo(2010000000));
			// mColorsHM.put(new OSMColor(220, 220, 220), new ColorInfo(2009000000));
			// mColorsHM.put(new OSMColor(242, 232, 216), new ColorInfo(2008000000));
			// mColorsHM.put(new OSMColor(185, 211, 200), new ColorInfo(2007000000));
			// mColorsHM.put(new OSMColor(244, 213, 167), new ColorInfo(2006000000));
			// mColorsHM.put(new OSMColor(151, 151, 149), new ColorInfo(2005000000));
			// mColorsHM.put(new OSMColor(123, 123, 123), new ColorInfo(2004000000));
			// mColorsHM.put(new OSMColor(185, 210, 209), new ColorInfo(2003000000));
			// mColorsHM.put(new OSMColor(226, 229, 225), new ColorInfo(2002000000));
			// mColorsHM.put(new OSMColor(244, 219, 183), new ColorInfo(2001000000));
			// mColorsHM.put(new OSMColor(229, 232, 227), new ColorInfo(2000000000));
			// mColorsHM.put(new OSMColor(219, 227, 220), new ColorInfo(1999000000));
			// mColorsHM.put(new OSMColor(226, 228, 219), new ColorInfo(1998000000));
			// mColorsHM.put(new OSMColor(248, 247, 244), new ColorInfo(1997000000));
			// mColorsHM.put(new OSMColor(203, 219, 217), new ColorInfo(1996000000));
			// mColorsHM.put(new OSMColor(243, 226, 200), new ColorInfo(1995000000));
			// mColorsHM.put(new OSMColor(168, 168, 165), new ColorInfo(1994000000));
			// mColorsHM.put(new OSMColor(134, 136, 133), new ColorInfo(1993000000));
			// mColorsHM.put(new OSMColor(245, 205, 147), new ColorInfo(1992000000));
			// mColorsHM.put(new OSMColor(201, 200, 197), new ColorInfo(1991000000));
			// mColorsHM.put(new OSMColor(245, 209, 154), new ColorInfo(1990000000));
			// mColorsHM.put(new OSMColor(184, 184, 181), new ColorInfo(1989000000));
			// mColorsHM.put(new OSMColor(233, 234, 229), new ColorInfo(1988000000));
			// mColorsHM.put(new OSMColor(211, 215, 202), new ColorInfo(1987000000));
			// mColorsHM.put(new OSMColor(149, 197, 137), new ColorInfo(1986000000));
			// mColorsHM.put(new OSMColor(213, 224, 220), new ColorInfo(1985000000));
			for (int y = 0; y < img.getHeight(); ++y)
			{
				for (int x = 0; x < img.getWidth(); ++x)
				{
					put(new OSMColor(img.getRGB(x, y)));
				}
			}
			log.debug("Palette[" + mColorsHM.size() + "," + mColorsHM.getMColorCnt() + "] after put()");
			// log.debug("Colors:" + toString());

			mColorsHM.usageSet();

			if (mColorsHM.size() > mPaletteCnt)
			{
				reduce();
				log.debug("Palette[" + mColorsHM.size() + "," + mColorsHM.getMColorCnt() + "] after reduce()");
				// log.trace("Colors:" + toString());
				// log.trace("BSB:" + asBSBStr());
			}
			else
				log.debug("Palette[" + mColorsHM.size() + "," + mColorsHM.getMColorCnt() + "] no reduction neccessary");
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

		int nSrcColor = mStartColorIdx;
		for (Iterator<Map.Entry<OSMColor, ColorInfo>> iColor = mColorsHM.getUsageIt(); iColor.hasNext();)
		{
			Map.Entry<OSMColor, ColorInfo> tPE = iColor.next();
			if (tPE.getValue().getMColor() == null)
			{
				// log.trace("tgt color=[" + nSrcColor + "], " + tColor.toStringKmpl() + ", cnt=" + tPE.getValue().getCount());

				for (Iterator<Map.Entry<OSMColor, ColorInfo>> iSrcColor = mColorsHM.getLessUsageIt(tPE); iSrcColor.hasNext();)
				{
					Map.Entry<OSMColor, ColorInfo> tSPE = iSrcColor.next();
					if (tSPE != tPE)
					{
						if ((tSPE.getValue().getCount() > 0) && (tPE.getKey().qDist(tSPE.getKey()) < 17))
						{
							// map the source color to the target
							// tPE.getValue().incCount(tSPE.getValue().setMColor(tPE.getKey()));
							ColorInfo ci = tPE.getValue();
							ci.incCount(tSPE.getValue().setMColor(tPE.getKey()));
							// tPE.setValue(ci);
							// if (tSPE.getValue().getMColor() != null)
							// log.trace(
							// "src " + tSPE.getKey().toStringKmpl() + ", cnt=" + tSPE.getValue().getCount() + " mapped to= " + tSPE.getValue().getMColor().toStringRGB());
							// else
							// log.trace("src " + tSPE.getKey().toStringKmpl() + ", cnt=" + tSPE.getValue().getCount());
							mColorsHM.decMColorCnt();
						}
						// else if (tSPE.getValue().getCount() == 0)
						// {
						// log.debug("src " + tSPE.getKey().toStringKmpl() + ", tgt " + tPE.getKey().toStringKmpl() + ", last color=" + nSrcColor + ", cnt[src]=0");
						// // break;
						// }
					}
				}
				// log.trace("tgt after=[" + nSrcColor + "], " + tColor.toStringKmpl() + ", cnt=" + tPE.getValue().getCount());
			}
			else
			{
				// OSMColor tColor = tPE.getKey();
				// log.trace("tgt color=[" + nSrcColor + "], " + tColor.toStringKmpl() + ", cnt=" + tPE.getValue().getCount() + " mapped to= "
				// + tPE.getValue().getMColor().toStringRGB());
			}

			nSrcColor++;
		}

		log.trace("\r\n");
		log.debug("Palette[" + mColorsHM.size() + "," + mColorsHM.getMColorCnt() + "] after first round()");
		// log.debug("Colors:" + toString());

		// Second round: now map the colors more different. Here one has to be careful. We use OSMColor.oDiff() instead of OSMColor.dDiff()
		// For each color in the list the best matching color is found. Then the best color match, i.e. the one with the least difference is mapped.
		// This is repeated until only the 127 colors in the map remain.
		// We need a sorted map with the distances and the indices

		for (Iterator<Map.Entry<OSMColor, ColorInfo>> iColor = mColorsHM.getUsageIt(); iColor.hasNext();)
		{
			findBestMatch(iColor.next(), false);
		}

		log.trace("\r\n");
		log.debug("Palette[" + mColorsHM.size() + "," + mColorsHM.getMColorCnt() + "] after findBestMatch()");
		// log.trace("Colors:" + toString());

		nSrcColor = 0;
		int nTstColor = mStartColorIdx;

		while ((mColorsHM.getMColorCnt() > 128) && (nTstColor < 20000))
		{
			Map.Entry<OSMColor, ColorInfo> tPE = mColorsHM.getBestODist();
			ColorInfo tCI = tPE.getValue();
			OSMColor tCBM = tCI.getBestMatch();
			// log.trace("tgt color[" + nTstColor + "]=" + tPE.getKey().toStringKmpl() + ", cnt=" + tPE.getValue().getCount());
			if ((tCI.getCount() == 0) || (tCI.getMColor() != null) || (mColorsHM.getMColor(tCBM) != null))
			{
				// if (tCI.getMColor() != null)
				// log.trace("tgt already mapped to= " + tCI.getMColor().toStringRGB());
				// if (mColorsHM.getMColor(tCBM) != null)
				// log.trace("best match already mapped to= " + mColorsHM.getMColor(tCBM).toStringRGB());
				findBestMatch(tPE, true);
				log.trace("tgt rematch");
			}
			else
			{
				if (tCI.getCount() >= mColorsHM.getCount(tCBM))
				{
					// map best match to this color
					tCI.incCount(mColorsHM.get(tCBM).setMColor(tPE.getKey()));
					// log.trace("tgt < color[" + nTstColor + "]=RGB(" + tPE.getKey().toStringRGB() + "), HSL(" + tPE.getKey().toStringHSL() + "), cnt=" + tCI.getCount()
					// + ", < best match mapped=RGB(" + tCBM.toStringRGB() + "), m-cnt=" + mColorsHM.getCount(tCBM) + ", dist=" + tCI.getDist());
					findBestMatch(tPE, true);
					++nSrcColor;
				}
				else
				{
					// map this color to the best match
					// tPE.getValue().incCount(mColorsHM.get(tPE.getValue().getBestMatch()).setMColor(tPE.getKey()));
					mColorsHM.get(tCBM).incCount(tCI.setMColor(tCBM));
					log.trace("tgt > color[" + nTstColor + "]=RGB(" + tPE.getKey().toStringRGB() + "), HSL(" + tPE.getKey().toStringHSL() + "), cnt=" + tCI.getCount()
					    + ", mapped to RGB(" + tCBM.toStringRGB() + "), m-cnt=" + mColorsHM.getCount(tCBM) + ", dist=" + tCI.getDist());
					++nSrcColor;
				}
				mColorsHM.decMColorCnt();
			}
			++nTstColor;
		}
		log.trace("mapped colors[" + nSrcColor + "] of [" + nTstColor + "]-" + mColorsHM.getMColorCnt());

		// collect the actual indices in the final palette
		nSrcColor = mStartColorIdx;
		for (Iterator<Map.Entry<OSMColor, ColorInfo>> iColor = mColorsHM.getUsageIt(); iColor.hasNext();)
		{
			Map.Entry<OSMColor, ColorInfo> tPE = iColor.next();
			if (nSrcColor < mPaletteCnt)
			{
				tPE.getValue().setPIdx(nSrcColor);
			}
			else
			{
				int nIdx = 0;
				OSMColor tColor = tPE.getKey();
				OSMColor tMColor = null;
				while ((tMColor = mColorsHM.get(tColor).getMColor()) != null)
				{
					tColor = tMColor;
				}
				nIdx = mColorsHM.get(tColor).getPIdx();
				tPE.getValue().setPIdx(nIdx);
				if (tColor != tPE.getKey())
					tPE.getValue().setMColor(tColor);
			}
			++nSrcColor;
		}

		log.trace("\r\n");
		log.debug("Palette[" + mColorsHM.size() + "," + mColorsHM.getMColorCnt() + "] after reduce():");
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
	private void findBestMatch(Map.Entry<OSMColor, ColorInfo> tPE, boolean bAll)
	{
		OSMColor tSrcColor = tPE.getKey();
		Map.Entry<OSMColor, ColorInfo> tMPE = null;
		double dNewDiff = 0.0;
		double dDist = 1e100; // some sufficient high number

		if (tPE.getValue().mMColor == null)
		{
			Iterator<Map.Entry<OSMColor, ColorInfo>> iSrcColor;
			// log.trace("tgt " + tSrcColor.toStringKmpl() + ", cnt=" + tPE.getValue().getCount());

			if (bAll)
				iSrcColor = mColorsHM.getUsageIt();
			else
				iSrcColor = mColorsHM.getLessUsageIt(tPE);
			if (iSrcColor.hasNext())
			{
				while (iSrcColor.hasNext())
				{
					Map.Entry<OSMColor, ColorInfo> tSPE = iSrcColor.next();
					if ((tSPE != tPE) && (tSPE.getValue().mMColor == null))
					{
						dNewDiff = tSrcColor.oDist(tSPE.getKey());
						if (dNewDiff < dDist)
						{
							tMPE = tSPE;
							dDist = dNewDiff;
						}
					}
				}
				if (tMPE != null)
				{
					// log.trace("best match: " + tPE.getKey().toStringKmpl() + ", cnt=" + tPE.getValue().getCount() + " to " + tMPE.getKey().toStringKmpl() + ", cnt="
					// + tMPE.getValue().getCount() + ", diff=" + dDist);

					// Fill list with color distances.
					tPE.getValue().setBestMatch(tMPE.getKey());
					tPE.getValue().setDist(dDist);
				}
				else
				{
					log.debug("no match found");
					// tPE.getValue().setBestMatch(tPE.getKey());
					tPE.getValue().setDist(1e100);
				}
			}
		}
		else
		{
			log.trace("tgt color already mapped");
			// tPE.getValue().setBestMatch(tPE.getKey());
			tPE.getValue().setDist(1e100);
		}
	}

	public void map(Map.Entry<OSMColor, ColorInfo> tTgtColor, Map.Entry<OSMColor, ColorInfo> tSrcColor)
	{
		log.error("map() called");
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
		int nCol = mStartColorIdx;

		Iterator<Map.Entry<OSMColor, ColorInfo>> iColor;
		for (iColor = mColorsHM.getUsageIt(); iColor.hasNext() && (nCol < 128);)
		{
			strPal += "RGB/" + nCol++ + "," + iColor.next().getKey().toStringRGB() + "\r\n";
		}
		iColor = null;
		return strPal;
	}

	/**
	 * This creates a specific String in a format suited for logging or tracing. It lists all colors in the palette.
	 */
	@Override
	public String toString()
	{
		String strPal = "\r\n";

		// int nColor = mStartColorIdx;
		Iterator<Map.Entry<OSMColor, ColorInfo>> iColor = null;
		try
		{
			for (iColor = mColorsHM.getUsageIt(); iColor.hasNext();)
			{
				Map.Entry<OSMColor, ColorInfo> tPE = iColor.next();
				strPal += "Palette: " + tPE.getKey().toStringKmpl() + ", cnt=" + tPE.getValue().getCount();
				if (tPE.getValue().getMColor() != null)
					strPal += ", mapped to=RGB(" + tPE.getValue().getMColor().toStringRGB() + ")";
				else
					strPal += ", unmapped";

				if (tPE.getValue().getBestMatch() != null)
					strPal += ", best match=RGB(" + tPE.getValue().getBestMatch().toStringRGB() + ")";
				else
					strPal += ", unmatched";

				strPal += ", PIdx=" + tPE.getValue().getPIdx();

				strPal += "\r\n";
				// nColor++;
			}
		}
		catch (Throwable t)
		{
			log.error("---- Error in toString() ----");
			t.printStackTrace();
		}
		finally
		{
			iColor = null;
		}
		return strPal;
	}

	/**
	 * places a color in the palette. If the color is not yet included, a new entry is added, else the count of the existing entry is incremented
	 * 
	 * @param tCol
	 * @return usage count of the color
	 */
	@Override
	public int put(OSMColor tCol)
	{
		int nCnt = mColorsHM.add(tCol);
		return nCnt;
	}

	@Override
	public int getPIdx(OSMColor tColor)
	{
		int nIdx = mColorsHM.get(tColor).getPIdx();
		return nIdx;
	}
}
