package osmcb.utilities.image;

import java.awt.Color;
import org.apache.log4j.Logger;

/**
 * this extends the java.awt.color with specialized comparison and differences
 * 
 * @author humbach
 *
 */
public class OSMColor extends Color
{
	protected Logger log;

	public OSMColor(int rgb)
	{
		super(rgb);
		log = Logger.getLogger(this.getClass());
	}

	public OSMColor(int r, int g, int b)
	{
		super(r, g, b);
		log = Logger.getLogger(this.getClass());
	}

	/**
	 * calculates the linear difference between two colors
	 * 
	 * @param cCol
	 * @return
	 */
	public int diff(OSMColor cCol)
	{
		int nDiff = 0;
		nDiff += Math.abs(getBlue() - cCol.getBlue());
		nDiff += Math.abs(getGreen() - cCol.getGreen());
		nDiff += Math.abs(getRed() - cCol.getRed());
		return nDiff;
	}

	/**
	 * calculates the linear difference between two colors with alpha channel
	 * 
	 * @param cCol
	 * @return
	 * 
	 */
	public int diffAlpha(OSMColor cCol)
	{
		int nDiff = 0;
		nDiff += Math.abs(getBlue() - cCol.getBlue());
		nDiff += Math.abs(getGreen() - cCol.getGreen());
		nDiff += Math.abs(getRed() - cCol.getRed());
		nDiff += Math.abs(getAlpha() - cCol.getAlpha());
		return nDiff;
	}

	/**
	 * calculates the quadratic difference between two colors
	 * 
	 * @param cCol
	 *          the 'other' color
	 * @return the quadratic difference
	 */
	public long qDiff(OSMColor cCol)
	{
		int nDiff = (getBlue() - cCol.getBlue()) * (getBlue() - cCol.getBlue());
		nDiff += (getGreen() - cCol.getGreen()) * (getGreen() - cCol.getGreen());
		nDiff += (getRed() - cCol.getRed()) * (getRed() - cCol.getRed());
		return nDiff;
	}

	/**
	 * calculates the quadratic difference between two colors with alpha channel
	 * 
	 * @param cCol
	 *          the 'other' color
	 * @return the quadratic difference
	 * 
	 */
	public long qDiffAlpha(OSMColor cCol)
	{
		int nDiff = (getBlue() - cCol.getBlue()) * (getBlue() - cCol.getBlue());
		nDiff += (getGreen() - cCol.getGreen()) * (getGreen() - cCol.getGreen());
		nDiff += (getRed() - cCol.getRed()) * (getRed() - cCol.getRed());
		nDiff += (getAlpha() - cCol.getAlpha()) * (getAlpha() - cCol.getAlpha());
		return nDiff;
	}

	/**
	 * calculates the optical difference between two colors
	 * 
	 * @param cCol
	 *          the 'other' color
	 * @return the optical difference
	 */
	// public double oDiff(OSMColor cCol)
	// {
	// double dDiff = getBlue() * cCol.getBlue() + getGreen() * cCol.getGreen() + getRed() * cCol.getRed();
	// dDiff /= Math.sqrt(getBlue() * getBlue() + getGreen() * getGreen() + getRed() * getRed())
	// * Math.sqrt(cCol.getBlue() * cCol.getBlue() + cCol.getGreen() * cCol.getGreen() + cCol.getRed() * cCol.getRed());
	// dDiff = (1.0 - dDiff) * 256.0;
	// return dDiff;
	// }
	public double oDiff(OSMColor cCol)
	{
		double dRDiff = getRed() - cCol.getRed();
		double dGDiff = getGreen() - cCol.getGreen();
		double dBDiff = getBlue() - cCol.getBlue();
		double dDiff = Math.abs((getBlue() * dBDiff + getGreen() * dGDiff + getRed() * dRDiff) / (length() * Math.sqrt(qDiff(cCol))));
		if (dDiff > 1)
		{
			log.debug("oDiff(): d-Diff=" + dDiff + "; qDiff=" + qDiff(cCol));
			dDiff = 1.0;
		}
		// double oDiff = (1.0 - dDiff) * (qDiff(cCol) * qDiff(cCol)) / cCol.length();
		double oDiff = (2.0 - dDiff) * Math.sqrt(qDiff(cCol)) / cCol.length();
		if (oDiff <= 0)
			log.info("oDiff(): d-Diff=" + dDiff + "; o-Diff=" + oDiff + "; qDiff=" + qDiff(cCol));
		return oDiff;
	}

	protected double length()
	{
		double dLng = Math.sqrt((getBlue() * getBlue()) + (getGreen() * getGreen()) + (getRed() * getRed()));
		return dLng;
	}

	String toStringRGB()
	{
		String str = getRed() + "," + getBlue() + "," + getGreen();
		return str;
	}

	String toStringRGBA()
	{
		String str = getRed() + "," + getBlue() + "," + getGreen() + "," + getAlpha();
		return str;
	}

	public boolean matches(int nRGB)
	{
		return ((getRed() == ((nRGB & 0xFF0000) >> 16)) && (getGreen() == ((nRGB & 0xFF00) >> 8)) && (getBlue() == ((nRGB & 0xFF))));
	}

}
