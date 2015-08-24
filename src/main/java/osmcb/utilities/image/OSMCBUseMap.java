package osmcb.utilities.image;

import java.util.Comparator;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

public class OSMCBUseMap extends HashMap<Integer, ECnt>
{
	/**
	 * AH 2015-08-15 Initial version
	 */
	private static final long serialVersionUID = 1L;

	static final Comparator<ECnt> BYCOUNT = new Comparator<ECnt>()
	{
		@Override
		public int compare(ECnt o1, ECnt o2)
		{
			int nRes = 0;
			if (o1.getCount() < o2.getCount())
				nRes = -1;
			else if (o1.getCount() > o2.getCount())
				nRes = 1;
			else
			{
				if (o1.getID() < o2.getID())
					nRes = -1;
				else if (o1.getID() > o2.getID())
					nRes = 1;
			}
			return nRes;
		}
	};

	public int getCnt(int nID)
	{
		if (get(nID) != null)
			return get(nID).getCount();
		else
			return -1;
	}

	/**
	 * Increments the usage count by one
	 * 
	 * @return The new usage count after the operation.
	 */
	int incCnt(int nID)
	{
		// put(nID, get(nID).getCount() + 1);
		return get(nID).getCount();
	}

	/**
	 * Increments the usage count by a given value
	 * 
	 * @param nCnt
	 *          The number to be added to the current usage count.
	 * @return The new usage count after the operation.
	 */
	int incCnt(int nID, int nCnt)
	{
		// put(nID, get(nID).getCount() + nCnt);
		return get(nID).getCount();
	}

	/**
	 * Clears the usage count and returns the previous count.
	 * 
	 * @return The usage count before the clear operation.
	 */
	int clrCnt(int nID)
	{
		return 0; // put(nID, 0);
	}

	SortedSet<ECnt> cntSet()
	{
		SortedSet<ECnt> st = new TreeSet<>(BYCOUNT);
		st = (SortedSet<ECnt>) this.values();
		return st;
	}
}
