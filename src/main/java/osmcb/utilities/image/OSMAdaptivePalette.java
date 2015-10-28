package osmcb.utilities.image;

import java.awt.image.BufferedImage;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * the @class is used to assign the 127 colors available in the KAP-format to the most used colors in the image.
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
public class OSMAdaptivePalette implements IFOSMPalette
{
	/**
	 * 20140112 AH v1 initial version
	 */
	private static final long serialVersionUID = 1L;
	protected final Logger log;

	/**
	 * 'index' Map containing all colors found in the map image. It maps color value to color IDs.
	 * It contains one entry for each color found in the image.
	 */
	private OSMCBColorMap mColorsHM = new OSMCBColorMap();

	/**
	 * Map containing the 'best matching pairs'. It maps the distance and the (target and the source) indices in the palette.
	 * It is sorted by ascending distance.
	 * !This is an insecure usage since it is not guaranteed that there are not two ColorPairs with the same distance!
	 * But even in the very unlikely case, that there are - and they are the least distance of all too, we don't give a damn and use one of the pairs.
	 * */

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
		// Init some standard colors
		mColorsHM.put(new OSMColor(0, 0, 0), new ColorInfo(21000000));
		mColorsHM.put(new OSMColor(255, 0, 0), new ColorInfo(20000000));
		mColorsHM.put(new OSMColor(0, 255, 0), new ColorInfo(19000000));
		mColorsHM.put(new OSMColor(0, 0, 255), new ColorInfo(18000000));
		mColorsHM.put(new OSMColor(255, 255, 0), new ColorInfo(17000000));
		mColorsHM.put(new OSMColor(0, 255, 255), new ColorInfo(16000000));
		mColorsHM.put(new OSMColor(255, 0, 255), new ColorInfo(15000000));
		mColorsHM.put(new OSMColor(255, 255, 255), new ColorInfo(14000000));
		mColorsHM.put(new OSMColor(181, 208, 208), new ColorInfo(13000000));
		mColorsHM.put(new OSMColor(228, 198, 171), new ColorInfo(12000000));
		mColorsHM.put(new OSMColor(177, 139, 190), new ColorInfo(11000000));
		mColorsHM.put(new OSMColor(127, 127, 127), new ColorInfo(10000000));
		for (int y = 0; y < img.getHeight(); ++y)
		{
			for (int x = 0; x < img.getWidth(); ++x)
			{
				put(new OSMColor(img.getRGB(x, y)));
			}
		}
		log.trace("Palette[" + mColorsHM.size() + "," + mColorsHM.getMColorCnt() + "] after put()");
		log.trace("Colors:" + toString());

		mColorsHM.usageSet();

		if (mColorsHM.size() > mPaletteCnt)
		{
			reduce();
			log.trace("Palette[" + mColorsHM.size() + "," + mColorsHM.getMColorCnt() + "] after reduce()");
			log.trace("Colors:" + toString());
			log.trace("BSB:" + asBSBStr());
		}
		else
			log.trace("Palette[" + mColorsHM.size() + "," + mColorsHM.getMColorCnt() + "] no reduction neccessary");
	}

	/**
	 * reduces the palette to mPaletteCnt entries by mapping the least used entries to the mPaletteCnt most used ones.
	 * this is done recursively from the start on.
	 */
	protected void reduce()
	{
		int nCLSize = mColorsHM.size(); // the size of the list will not change, mapped - hence unused - colors will be moved to the end of the list.
		// The palette is traversed in 'normal' order, which is descending in the usage count. This means, often used colors are mapped first, and hopefully
		// prevents color drifting while chained mapping.
		// It stops when the usage count of the color has reached 0, so colors which have already been mapped earlier are skipped.
		// The first round maps neatly matching colors, so the most often used colors move to the front of the palette
		log.trace("start");

		int nSrcColor = 1;
		for (Iterator<Map.Entry<OSMColor, ColorInfo>> iColor = mColorsHM.getUsageIt(); iColor.hasNext();)
		{
			Map.Entry<OSMColor, ColorInfo> tPE = iColor.next();
			OSMColor tColor = tPE.getKey();
			if (tPE.getValue().getMColor() != null)
				log.trace("tgt color=[" + nSrcColor + "], RGB(" + tColor.toStringRGB() + "), HSL(" + tColor.toStringHSL() + "), cnt=" + tPE.getValue().getCount()
						+ " mapped to= " + tPE.getValue().getMColor().toStringRGB());
			else
			{
				log.trace("tgt color=[" + nSrcColor + "], color=RGB(" + tColor.toStringRGB() + "), HSL(" + tColor.toStringHSL() + "), cnt=" + tPE.getValue().getCount());

				for (Iterator<Map.Entry<OSMColor, ColorInfo>> iSrcColor = mColorsHM.getLessUsageIt(tPE); iSrcColor.hasNext();)
				{
					Map.Entry<OSMColor, ColorInfo> tSPE = iSrcColor.next();
					if (tSPE != tPE)
					{
						if ((tSPE.getValue().getCount() > 0) && (tPE.getKey().qDist(tSPE.getKey()) < 17))
						{
							// map the source color to the target
							tPE.getValue().incCount(tSPE.getValue().setMColor(tPE.getKey()));
							if (tSPE.getValue().getMColor() != null)
								log.trace("src color=RGB(" + tSPE.getKey().toStringRGB() + "), HSL(" + tSPE.getKey().toStringHSL() + "), cnt=" + tSPE.getValue().getCount()
										+ " mapped to= " + tSPE.getValue().getMColor().toStringRGB());
							else
								log.trace("src color=RGB(" + tSPE.getKey().toStringRGB() + "), HSL(" + tSPE.getKey().toStringHSL() + "), cnt=" + tSPE.getValue().getCount());
							mColorsHM.decMColorCnt();
						}
					}
				}
				log.trace("tgt after=[" + nSrcColor + "], color=RGB(" + tColor.toStringRGB() + "), HSL(" + tColor.toStringHSL() + "), cnt=" + tPE.getValue().getCount());
			}
			nSrcColor++;
		}

		log.trace("\r\n");
		log.debug("Palette[" + mColorsHM.size() + "," + mColorsHM.getMColorCnt() + "] after first round()");
		log.trace("Colors:" + toString());

		// Second round: now map the colors more different. Here one has to be careful. We use OSMColor.oDiff() instead of OSMColor.dDiff()
		// For each color in the list the best matching color is found. Then the best color match, i.e. the one with the least difference is mapped.
		// This is repeated until only the 127 colors in the map remain.
		// We need a sorted map with the distances and the indices

		nSrcColor = 0;
		for (Iterator<Map.Entry<OSMColor, ColorInfo>> iColor = mColorsHM.getUsageIt(); iColor.hasNext();)
		{
			findBestMatch(iColor.next(), false);
		}

		log.trace("\r\n");
		log.debug("Palette[" + mColorsHM.size() + "," + mColorsHM.getMColorCnt() + "] after findBestMatch()");
		log.trace("Colors:" + toString());

		nSrcColor = 0;
		int nTstColor = 1;

		while ((mColorsHM.getMColorCnt() > 127) && (nTstColor < 20000))
		{
			Map.Entry<OSMColor, ColorInfo> tPE = mColorsHM.getBestODist();
			ColorInfo tCI = tPE.getValue();
			OSMColor tCBM = tCI.getBestMatch();
			log.trace("tgt color[" + nTstColor + "]=RGB(" + tPE.getKey().toStringRGB() + "), HSL(" + tPE.getKey().toStringHSL() + "), cnt="
					+ tPE.getValue().getCount());
			if ((tCI.getCount() == 0) || (tCI.getMColor() != null) || (mColorsHM.getMColor(tCBM) != null))
			{
				if (tCI.getMColor() != null)
					log.trace("tgt already mapped to= " + tCI.getMColor().toStringRGB());
				if (mColorsHM.getMColor(tCBM) != null)
					log.trace("best match already mapped to= " + mColorsHM.getMColor(tCBM).toStringRGB());
				findBestMatch(tPE, true);
				log.trace("tgt rematch");
			}
			else
			{
				if (tCI.getCount() >= mColorsHM.getCount(tCBM))
				{
					// map best match to this color
					tCI.incCount(mColorsHM.get(tCBM).setMColor(tPE.getKey()));
					log.trace("tgt < color[" + nTstColor + "]=RGB(" + tPE.getKey().toStringRGB() + "), HSL(" + tPE.getKey().toStringHSL() + "), cnt=" + tCI.getCount()
							+ ", < best match mapped=RGB(" + tCBM.toStringRGB() + "), m-cnt=" + mColorsHM.getCount(tCBM) + ", dist=" + tCI.getDist());
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
		nSrcColor = 1;
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
		log.trace("Colors:" + toString());
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
			log.trace("tgt color=RGB(" + tSrcColor.toStringRGB() + "), HSL(" + tSrcColor.toStringHSL() + "), cnt=" + tPE.getValue().getCount());

			if (bAll)
				iSrcColor = mColorsHM.getUsageIt();
			else
				iSrcColor = mColorsHM.getLessUsageIt(tPE);
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
				log.trace("best match: color=RGB(" + tSrcColor.toStringRGB() + "), HSL(" + tSrcColor.toStringHSL() + "), cnt=" + tPE.getValue().getCount()
						+ " to color=RGB(" + tMPE.getKey().toStringRGB() + "), HSL(" + tMPE.getKey().toStringHSL() + "), cnt=" + tMPE.getValue().getCount() + ", diff="
						+ dDist);

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
		else
		{
			log.trace("tgt color already mapped");
			// tPE.getValue().setBestMatch(tPE.getKey());
			tPE.getValue().setDist(1e100);
		}
	}

	public void map(Map.Entry<OSMColor, ColorInfo> tTgtColor, Map.Entry<OSMColor, ColorInfo> tSrcColor)
	{

	}

	/**
	 * this creates a specific String in the format required by the BSB-KAP file format
	 * it runs at most over the first mPaletteCnt entries in the palette
	 * it ends with a 0x0D,0x0A sequence("\r\n").
	 */
	@Override
	public String asBSBStr()
	{
		String strPal = "";
		int nCol = 1;

		for (Iterator<Map.Entry<OSMColor, ColorInfo>> iColor = mColorsHM.getUsageIt(); iColor.hasNext() && (nCol < 128);)
		{
			strPal += "RGB/" + nCol++ + "," + iColor.next().getKey().toStringRGB() + "\r\n";
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

		int nColor = 1;
		// for (Map.Entry<OSMColor, ColorInfo> tPE : mColorsHM.entrySet())
		for (Iterator<Map.Entry<OSMColor, ColorInfo>> iColor = mColorsHM.getUsageIt(); iColor.hasNext();)
		{
			Map.Entry<OSMColor, ColorInfo> tPE = iColor.next();
			strPal += "Palette: Color[" + nColor + "]=RGB(" + tPE.getKey().toStringRGB() + ")" + ", HSL(" + tPE.getKey().toStringHSL() + "), cnt="
					+ tPE.getValue().getCount();
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
			nColor++;
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
	public int getPID(OSMColor tColor)
	{
		int nIdx = 0;
		// OSMColor tMColor = null;
		// while ((tMColor = mColorsHM.get(tColor).getMColor()) != null)
		// {
		// tColor = tMColor;
		// }
		nIdx = mColorsHM.get(tColor).getPIdx();
		return nIdx;
	}
}
