package osmcb.utilities.image;

public interface IfOSMPalette
{
	/**
	 * this creates a specific String in the format required by the BSB-KAP file format
	 * it runs at most over the first mPaletteCnt entries in the palette
	 * it ends with a 0x0D,0x0A sequence("\r\n").
	 */
	public abstract String asBSBStr();

	/**
	 * this creates a specific String in a format suited for logging or tracing
	 */
	@Override
	public abstract String toString();

	/**
	 * places a color in the palette. If the color is not yet included, a new entry is added, else the count of the existing entry is incremented
	 * 
	 * @param tCol
	 * @return usage count of the color
	 */
	public abstract int put(OSMColor tCol);

	/**
	 * 
	 * @param tColor
	 * @return The index in the final color palette
	 */
	public abstract int getPID(OSMColor tColor);
}