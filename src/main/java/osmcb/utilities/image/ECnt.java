package osmcb.utilities.image;

import java.util.Map.Entry;

public class ECnt implements Entry<Integer, Integer>
{
	protected int mID; // IDs are unique
	protected int mCount; // counts are not unique

	ECnt(int nID, int nCount)
	{
		mID = nID;
		mCount = nCount;
	}

	@Override
	public Integer getKey()
	{
		return mID;
	}

	public Integer getID()
	{
		return mID;
	}

	@Override
	public Integer getValue()
	{
		return mCount;
	}

	public Integer getCount()
	{
		return mCount;
	}

	@Override
	public Integer setValue(Integer newCount)
	{
		int nCount = mCount;
		mCount = newCount;
		return nCount;
	}
}
