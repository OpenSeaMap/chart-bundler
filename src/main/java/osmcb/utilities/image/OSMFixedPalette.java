package osmcb.utilities.image;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import org.apache.log4j.Logger;

/**
 * the OSMPalette is used to assign the 127 colors available in the TIFF in KAP to the most used colors in the image.
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
public class OSMFixedPalette
{
	/**
	 * 20140112 AH v1 initial version
	 */
	private static final long serialVersionUID = 1L;
	protected final Logger log;
	private HashMap<OSMColor, Integer> mHM = new HashMap<OSMColor, Integer>();
	private ArrayList<OSMPaletteEntry> mCL = new ArrayList<OSMPaletteEntry>(128);
	private int mPaletteCnt = 128; // the BSB-KAP TIFF allows up to 128 colors
	private int mStdColors = 0;

	/**
	 * creates a fixed palette with 125 colors
	 * 
	 * @param img
	 */
	public OSMFixedPalette(BufferedImage img)
	{
		int nCV[] =
		{
				0, 77, 128, 179, 255
		};
		int nIdx = 0;
		log = Logger.getLogger(this.getClass());
		// Init entry 0, which is not used by OpenCPN
		mCL.add(nIdx, new OSMPaletteEntry(new OSMColor(0), 0, nIdx));
		nIdx++;
		for (int nRed = 0; nRed < 5; nRed++)
		{
			for (int nGreen = 0; nGreen < 5; nGreen++)
			{
				for (int nBlue = 0; nBlue < 5; nBlue++)
				{
					OSMColor tCol = new OSMColor(nCV[nRed], nCV[nGreen], nCV[nBlue]);
					mCL.add(nIdx, new OSMPaletteEntry(tCol, 0, nIdx));
					mHM.put(tCol, nIdx);
					nIdx++;
				}
			}
		}
		for (; nIdx < mPaletteCnt; nIdx++)
		{
			// add padding entries, which were not used by OpenCPN
			mCL.add(nIdx, new OSMPaletteEntry(new OSMColor(0), 0, nIdx));
		}

		// append the images colors
		for (int y = 0; y < img.getHeight(); ++y)
		{
			for (int x = 0; x < img.getWidth(); ++x)
			{
				put(new OSMColor(img.getRGB(x, y)));
			}
		}

		log.info("Palette[" + mCL.size() + "] after put():" + toString() + "HM[" + mHM.size() + "]");
		if (mCL.size() > mPaletteCnt)
		{
			mapColors();
			log.trace("Palette[" + mCL.size() + "] after mapColors():" + toString());
		}
		else
			log.info("Palette[" + mCL.size() + "] no reduction neccessary");

		// populate the hash map with the color/index mapping
		for (int nCol = mPaletteCnt; nCol < mCL.size(); nCol++)
		{
			OSMPaletteEntry tPE = mCL.get(nCol);
			mHM.put(tPE.getColor(), tPE.getMIndex());
		}
	}

	/**
	 * maps the colors to the first 127 palette entries
	 */
	public void mapColors()
	{
		for (int nCol = mCL.size() - 1; nCol >= mPaletteCnt; nCol--)
		{
			int nRed = mCL.get(nCol).getColor().getRed();
			if (nRed < 26)
				nRed = 0;
			else if (nRed < 90)
				nRed = 77;
			else if (nRed < 166)
				nRed = 128;
			else if (nRed < 230)
				nRed = 179;
			else
				nRed = 255;

			int nGreen = mCL.get(nCol).getColor().getGreen();
			if (nGreen < 26)
				nGreen = 0;
			else if (nGreen < 90)
				nGreen = 77;
			else if (nGreen < 166)
				nGreen = 128;
			else if (nGreen < 230)
				nGreen = 179;
			else
				nGreen = 255;

			int nBlue = mCL.get(nCol).getColor().getBlue();
			if (nBlue < 26)
				nBlue = 0;
			else if (nBlue < 90)
				nBlue = 77;
			else if (nBlue < 166)
				nBlue = 128;
			else if (nBlue < 230)
				nBlue = 179;
			else
				nBlue = 255;

			OSMColor tTCol = new OSMColor(nRed, nGreen, nBlue);
			int nMIdx = mHM.get(tTCol);
			mCL.get(nCol).setMIndex(nMIdx);
		}
	}

	/**
	 * this creates a specific String in the format required by the BSB KAP file format
	 * it runs 'only' over the first mPaletteCnt entries in the palette
	 */
	public String asBSBStr()
	{
		String strPal = "";

		OSMPaletteEntry tPE = null;
		for (int nCol = 1; nCol < Math.min(mPaletteCnt, mCL.size()); nCol++)
		{
			tPE = mCL.get(nCol);
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
		for (int nCol = 1; nCol < mCL.size(); nCol++)
		{
			tPE = mCL.get(nCol);
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
		int nSize = mCL.size();

		for (nCol = 1; nCol < nSize; nCol++)
		{
			tPE = mCL.get(nCol);
			if (tPE.getColor().getRGB() == tCol.getRGB())
			{
				nCnt = tPE.getCount() + 1;
				tPE.setCount(nCnt);
				break;
			}
		}
		if ((mCL.size() == 0) || (nCol == mCL.size()))
		{
			// append the entry to the list, it is used once
			tPE = new OSMPaletteEntry(tCol, 1, nCol);
			mCL.add(tPE);
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
		if (mHM.containsKey(tCol))
			nIdx = mHM.get(tCol);
		return nIdx;
	}
}
