package osmcb.utilities.image;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import osmb.utilities.OSMBStrs;
import osmcb.OSMCBStrs;

public class OSMCB2ColorMap
{
	// 'helper' classes
	private class OSMCB2ColorInfo
	{
		private OSMColor mColor; // color value
		private Integer mCID; // color ID
		private Integer mCnt = 1; // usage count, the default is 1
		private Integer mMCID = null; // ID of the target color this color is mapped to

		private OSMCB2ColorInfo(OSMColor tColor, Integer nCID, Integer nCnt)
		{
			mColor = tColor;
			mCID = nCID;
			mCnt = nCnt;
		}

		/**
		 * @param nCnt
		 *          The incremental count to be added to the current.
		 * @return The usage count after the incrementation.
		 */
		// public int incCnt(int nCnt)
		// {
		// mCnt += nCnt;
		// return mCnt;
		// }

		/**
		 * This sets the target colors ID this one is mapped to.
		 * 
		 * @param nMCID
		 */
		private void setMCID(Integer nMCID)
		{
			mMCID = nMCID;
		}

		@Override
		public String toString()
		{
			String str = "(RGB(" + mColor.toStringRGB() + "), CID=" + mCID + ", Cnt=" + mCnt + ")";
			return str;
		}
	}

	private class OSMCB2UsageInfo
	{
		private Integer mCnt;
		private Integer mCID;

		private OSMCB2UsageInfo(Integer nCnt, Integer nCID)
		{
			mCnt = nCnt;
			mCID = nCID;
		}

		private Integer setCnt(Integer nCnt)
		{
			Integer nOldCnt = mCnt;
			mCnt = nCnt;
			return nOldCnt;
		}

		private Integer incCnt()
		{
			++mCnt;
			return mCnt;
		}

		@Override
		public String toString()
		{
			String str = "(CID=" + mCID + ", Cnt=" + mCnt + ")";
			return str;
		}
	}

	/**
	 * List with index entries
	 * 
	 * @author humbach
	 *
	 */
	private class OSMCB2UsageList extends ArrayList<OSMCB2UsageInfo>
	{
		// private List<OSMCB2UsageInfo> mIdx = new ArrayList<OSMCB2UsageInfo>();

		private Integer insertBefore(Integer nCID, OSMCB2UsageInfo tUInfo)
		{
			Integer nIdx = indexByCID(nCID);
			// mIdx.add(nIdx, tUInfo);
			add(nIdx, tUInfo);
			return nIdx;
		}

		private Integer insertAfter(Integer nCID, OSMCB2UsageInfo tUInfo)
		{
			Integer nIdx = indexByCID(nCID) + 1;
			add(nIdx, tUInfo);
			return nIdx;
		}

		/**
		 * appends the specified usage info to the end of the list
		 * 
		 * @param tUInfo
		 *          The new usage info
		 */
		private void append(OSMCB2UsageInfo tUInfo)
		{
			add(tUInfo);
		}

		private void removeByCID(Integer nCID)
		{
			int nIdx = indexByCID(nCID);
			if (nIdx >= mStdColorCount)
				remove(nIdx);
		}

		/**
		 * @param nCID
		 *          The ID of the color to be retrieved.
		 * @return The usage info by color ID
		 */
		private OSMCB2UsageInfo getByCID(Integer nCID)
		{
			// find nCID in list
			for (OSMCB2UsageInfo nInfo : this)
			{
				if (nInfo.mCID.equals(nCID))
					return nInfo;
			}
			return null;
		}

		/**
		 * @param nCID
		 *          The color ID to look for
		 * @return The index in the usage list of the specified color or null if the color has no index in the palette.
		 */
		private Integer indexByCID(Integer nCID)
		{
			// Integer nIdx;
			// for (nIdx = 0; nIdx < size(); nIdx++)
			for (OSMCB2UsageInfo nInfo : this)
			{
				if (nInfo.mCID.equals(nCID))
					return indexOf(nInfo);
			}
			log.debug("color id=" + nCID + " not found");
			return null;
		}

		/**
		 * This increments the usage count of the specified color by 1
		 * 
		 * @param nCID
		 *          The color ID of the color to be modified
		 * @return The usage count after the incrementation
		 */
		private Integer incCntByCID(int nCID)
		{
			return incCntByCID(nCID, 1);
		}

		/**
		 * This increases the usage count of the specified color by a specified number
		 * 
		 * @param nCID
		 *          The color ID of the color to be modified
		 * @param nCnt
		 *          The number to increase the usage count by
		 * @return The usage count after the incrementation
		 */
		private Integer incCntByCID(int nCID, Integer nCnt)
		{
			OSMCB2UsageInfo tUInf = new OSMCB2UsageInfo(nCnt, nCID);
			int nIdx = 0;
			int nTgtIdx = 0;
			boolean bFound = false;
			for (nIdx = 0; nIdx < size(); ++nIdx)
			{
				tUInf = get(nIdx);
				if (nCID == tUInf.mCID)
				{
					tUInf.mCnt += nCnt;
					nCnt = tUInf.mCnt;
					bFound = true;
					break;
				}
			}
			if (bFound && (nIdx >= mStdColorCount))
			{
				for (nTgtIdx = nIdx; nTgtIdx > mStdColorCount + 1; --nTgtIdx)
				{
					if (get(nTgtIdx - 1).mCnt > nCnt)
						break;
				}
				remove(nIdx);
				log.trace("removed: UI[" + nIdx + "], found=" + bFound);
				add(nTgtIdx, tUInf);
				log.trace("moved: UI[" + nTgtIdx + "]=" + get(nTgtIdx) + ", found=" + bFound);
			}
			if (!bFound)
			{
				add(nTgtIdx, tUInf);
				log.trace("added: UI[" + nTgtIdx + "]=" + get(nTgtIdx) + ", found=" + bFound);
			}
			return nCnt;
		}

		private int search2Add(Integer nCnt)
		{
			Integer nIdxH = size() - 1;
			Integer nIdx = nIdxH / 2;
			Integer nStep = (size() + 1) / 4;
			// adjusted binary search
			while (nStep >= 1)
			{
				Integer nTCnt = get(nIdx).mCnt;
				if (nTCnt > nCnt)
				{
					nIdx += nStep;
				}
				else
				{
					nIdx -= nStep;
				}
				// next step
				if (nStep > 1)
					nStep /= 2;
				else
				{
					if (get(nIdx).mCnt >= nCnt)
						++nIdx;
				}
			}
			return nIdx;
		}

		private int search2Add2(Integer nCnt)
		{
			Integer nIdx = 0;
			Integer nIdxH = size() - 1;
			Integer nIdxL = 0;
			Integer nTCnt = 0;
			// binary search
			do
			{
				nIdx = nIdxL + (nIdxH - nIdxL) / 2;
				nTCnt = get(nIdx).mCnt;
				if (nTCnt > nCnt)
					nIdxL = nIdx;
				else
					nIdxH = nIdx;
				if (nIdx == 0)
					break;
			} while ((nTCnt != nCnt) && (nIdxL < (nIdxL + (nIdxH - nIdxL) / 2)));
			// } while ((nTCnt != nCnt) && (nIdxL < (nIdxH - 1))); // misses idx 0

			if (nTCnt > nCnt)
			{
				nIdx++;
			}
			if (nIdx > 0)
				log.debug("Cnt=" + nCnt + ": I-1[" + (nIdx - 1) + "]=" + get(nIdx - 1).mCnt + "," + get(nIdx - 1).mCID + ", I[" + nIdx + "]=" + get(nIdx).mCnt + ","
				    + get(nIdx).mCID + ", I+1[" + (nIdx + 1) + "]=" + get(nIdx + 1).mCnt + "," + get(nIdx + 1).mCID);
			else
				log.debug("Cnt=" + nCnt + ": I[" + nIdx + "]=" + get(nIdx).mCnt + "," + get(nIdx).mCID + ", I+1[" + (nIdx + 1) + "]=" + get(nIdx + 1).mCnt + ","
				    + get(nIdx + 1).mCID);
			return nIdx;
		}

		public String toSimpleString()
		{
			String strDesc = null;
			strDesc = this.getClass().getSimpleName() + "[" + this.size() + "]";
			return strDesc;
		}
	}

	// class/static data
	protected static Logger log = Logger.getLogger(OSMCB2ColorMap.class);
	static int mStdColorCount = 32;

	// instance data
	/**
	 * Maps color value to color list ID. Allows fast access to the CID by color value.
	 */
	private Map<OSMColor, Integer> mHM = null;
	/**
	 * Stores the color info about each color. Esp. used to access the color by its CID. The CID is the index in the list.
	 */
	private List<OSMCB2ColorInfo> mCL = null;

	/**
	 * Sorts over the CIDs in descending usage count
	 */
	private OSMCB2UsageList mUL = null;

	public OSMCB2ColorMap()
	{
		this(mStdColorCount);
	}

	public OSMCB2ColorMap(int nColors)
	{
		log = Logger.getLogger(this.getClass());
		mStdColorCount = nColors;
		mHM = new HashMap<OSMColor, Integer>(nColors);
		mCL = new ArrayList<OSMCB2ColorInfo>(nColors);
		mUL = new OSMCB2UsageList();
	}

	/**
	 * Adds one pixel of the specified color to the end of the map if it does not yet exist. A usage count of 1 is assumed.
	 * Otherwise the usage count of the existing entry is incremented by 1.
	 * 
	 * @param tColor
	 * @return The new color ID (or the current ID if the color already existed in map before).
	 */
	public int addPixel(OSMColor tColor)
	{
		log.trace(OSMBStrs.RStr("START"));
		return addPixels(tColor, 1);
	}

	/**
	 * Adds nCnt pixels of the specified color to the map if it does not yet exist. A usage count of nCnt is assumed.
	 * Otherwise the usage count of the existing entry is incremented by nCnt.
	 * 
	 * @param tColor
	 * @param nCnt
	 * @return The new color ID (or the current ID if the color already existed in map before).
	 */
	public int addPixels(OSMColor tColor, Integer nCnt)
	{
		log.trace(OSMBStrs.RStr("START"));
		int nCID = mHM.size();
		if (mHM.containsKey(tColor))
		{
			if (log.isTraceEnabled())
				log.trace("color RGB(" + tColor.toStringRGB() + ") should be in map, increment usage count");
			try
			{
				nCID = mHM.get(tColor);
				OSMCB2ColorInfo tCI = mCL.get(nCID);
				if (tCI != null)
				{
					tCI.mCnt += nCnt;
					mUL.incCntByCID(nCID, nCnt);
					if (log.isTraceEnabled())
						log.trace("usage incremented RGB(" + tColor.toStringRGB() + "): " + mCL + ", " + mUL + ", ID=" + nCID + ", Cnt=" + nCnt);
				}
				else
					log.debug("Unused color=" + tColor);
			}
			catch (IndexOutOfBoundsException e)
			{
				log.debug(OSMCBStrs.RStr("ColorMap.NoColor"));
			}
			catch (Exception e)
			{
				log.debug(e + ", " + e.getCause() + ", " + e.getMessage() + ", " + mCL + ", " + mUL + ", ID=" + nCID + ", Cnt=" + nCnt);
			}
		}
		else
		{
			mHM.put(tColor, nCID);
			mCL.add(new OSMCB2ColorInfo(tColor, nCID, nCnt));
			mUL.append(new OSMCB2UsageInfo(nCnt, nCID));
			if (log.isTraceEnabled())
				log.trace("RGB(" + tColor.toStringRGB() + ") added: " + mCL + ", " + mUL + ", ID=" + nCID + ", Cnt=" + nCnt);
			else if (log.isDebugEnabled())
				log.debug("color added: ID=" + nCID + ", Cnt=" + nCnt);
		}
		return nCID;
	}

	/**
	 * @param tColor
	 * @return The ID of the specified color or null if the color does not exist in map.
	 */
	public Integer getCID(OSMColor tColor)
	{
		log.trace(OSMBStrs.RStr("START"));
		return mHM.get(tColor);
	}

	/**
	 * @param nCID
	 * @return The color value of the color at the specified position in the map.
	 */
	public OSMColor getColor(Integer nCID)
	{
		log.trace(OSMBStrs.RStr("START"));
		OSMColor tColor = null;
		try
		{
			tColor = mCL.get(nCID).mColor;
		}
		catch (IndexOutOfBoundsException e)
		{
			log.debug(OSMCBStrs.RStr("ColorMap.NoColor"));
		}
		catch (NullPointerException e)
		{
			log.debug(OSMCBStrs.RStr("ColorMap.NoColor") + ", " + e);
		}
		return tColor;
	}

	/**
	 * @param tColor
	 * @return The ID of the specified color or null if the color does not exist in map.
	 */
	public Integer getPIdx(OSMColor tColor)
	{
		// log.trace(OSMBStrs.RStr("START"));
		return mUL.indexByCID(getCID(tColor));
	}

	/**
	 * @param tColor
	 * @return The palette index of the specified color or of the color it is finally mapped to.
	 */
	public Integer getMPIdx(OSMColor tColor)
	{
		log.trace(OSMBStrs.RStr("START"));
		Integer nCID = getMCID(tColor);
		if (nCID == null)
			nCID = getCID(tColor);
		Integer nPIdx = mUL.indexByCID(nCID);
		if (nPIdx == null)
		{
			nPIdx = 0;
			log.debug("no mPIdx found for=" + tColor.toStringRGB());
		}
		return nPIdx;
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
				nCnt = mCL.get(mHM.get(tColor)).mCnt;
		}
		catch (IndexOutOfBoundsException e)
		{
			log.debug(OSMCBStrs.RStr("ColorMap.NoColor"));
		}
		return nCnt;
	}

	/**
	 * @param nCID
	 * @return The usage count of the specified color.
	 */
	public Integer getCnt(Integer nCID)
	{
		log.trace(OSMBStrs.RStr("START"));
		Integer nCnt = 0;
		try
		{
			nCnt = mCL.get(nCID).mCnt;
		}
		catch (IndexOutOfBoundsException e)
		{
			log.debug(OSMCBStrs.RStr("ColorMap.NoColor"));
		}
		return nCnt;
	}

	/**
	 * @param tColor
	 * @return The final mapping target color ID of the specified color.
	 */
	public Integer getMCID(OSMColor tColor)
	{
		log.trace(OSMBStrs.RStr("START"));
		Integer nMCID = getCID(tColor);
		try
		{
			Integer nTgtCID = 0;
			while ((nTgtCID = mCL.get(nMCID).mMCID) != null)
			{
				nMCID = nTgtCID;
			}
		}
		catch (IndexOutOfBoundsException e)
		{
			log.debug(OSMCBStrs.RStr("ColorMap.NoColor"));
		}
		return nMCID;
	}

	public int size()
	{
		if (mHM.size() != mCL.size())
			log.error("lists not synched. HM[" + mHM.size() + "], CL[" + mCL.size() + "]");
		return mHM.size();
	}

	public int getUsedColors()
	{
		return mUL.size();
	}

	/**
	 * @param nIdx
	 * @return The color at the position nIdx in the final palette
	 */
	public OSMColor getColorByPIdx(int nIdx)
	{
		return getColor(mUL.get(nIdx).mCID);
	}

	/**
	 * @param nIdx
	 * @return The color ID of the color at the position nIdx in the final palette
	 */
	public Integer getCIDByPIdx(int nIdx)
	{
		return mUL.get(nIdx).mCID;
	}

	/**
	 * It moves tgtColor forward in the usage index and srcColor will be removed from the index.
	 * 
	 * @param srcColor
	 * @param tgtColor
	 */
	public void map(OSMColor srcColor, OSMColor tgtColor)
	{
		Integer nTCID = getCID(tgtColor);
		Integer nSCID = getCID(srcColor);
		mUL.removeByCID(nSCID);
		OSMCB2ColorInfo tSInf = mCL.get(nSCID);
		tSInf.mMCID = nTCID;
		int nCnt = tSInf.mCnt;
		tSInf.mCnt = 0;
		mCL.get(nTCID).mCnt += nCnt;
		mUL.incCntByCID(nTCID, nCnt);
	}
}
