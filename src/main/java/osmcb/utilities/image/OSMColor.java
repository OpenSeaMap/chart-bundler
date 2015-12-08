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
	/**
	 * takes the hsl value in the range 0..1. If you have rgb values in 1..255 use HSL2RGB(int, int, int)
	 */
	public static int HSL2RGB(double hue, double sat, double light)
	{
		int rgb = 0;
		double v;
		double r, g, b;

		hue = Math.max(0.0, Math.min(1.0, hue));
		sat = Math.max(0.0, Math.min(1.0, sat));
		light = Math.max(0.0, Math.min(1.0, light));

		r = light; // default to gray
		g = light;
		b = light;
		v = (light <= 0.5) ? (light * (1.0 + sat)) : (light + sat - light * sat);
		if (v > 0)
		{
			double m;
			double sv;
			int sextant;
			double fract, vsf, mid1, mid2;

			m = light + light - v;
			sv = (v - m) / v;
			hue *= 6.0;
			sextant = (int) hue;
			fract = hue - sextant;
			vsf = v * sv * fract;
			mid1 = m + vsf;
			mid2 = v - vsf;
			switch (sextant)
			{
				case 0:
					r = v;
					g = mid1;
					b = m;
					break;
				case 1:
					r = mid2;
					g = v;
					b = m;
					break;
				case 2:
					r = m;
					g = v;
					b = mid1;
					break;
				case 3:
					r = m;
					g = mid2;
					b = v;
					break;
				case 4:
					r = mid1;
					g = m;
					b = v;
					break;
				case 5:
					r = v;
					g = m;
					b = mid2;
					break;
			}
		}
		rgb |= (((byte) (r * 256.0)) << 16) & 0xFF0000;
		rgb |= (((byte) (g * 256.0)) << 8) & 0xFF00;
		rgb |= (((byte) (b * 256.0)) << 0) & 0xFF;
		return rgb;
	}

	/**
	 * Takes the hsl value as integers in the range hue [1..360], sat and light [0..100].
	 * If you have hsl values in [0..1] use HSL2RGB(double, double, double)
	 */
	public static int HSL2RGB(int hue, int sat, int light)
	{
		hue = Math.max(1, Math.min(360, hue));
		sat = Math.max(0, Math.min(100, sat));
		light = Math.max(0, Math.min(100, light));
		return HSL2RGB(hue / 360.0, sat / 100.0, light / 100.0);
	}

	// instance data
	protected Logger log;

	// color values in hsl color space
	protected int mHue = 0;
	protected int mSat = 0;
	protected int mLight = 0;

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
	 * calculates the linear difference between two colors in the rgb color space
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
	 * calculates the linear difference between two colors with alpha channel in the rgb color space
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
	 * Calculates the quadratic distance between two colors in the rgb color space
	 * 
	 * @param cCol
	 *          the 'other' color
	 * @return The quadratic distance in the range [0..3*255*255]
	 */
	public long qDist(OSMColor cCol)
	{
		int nDist = (getBlue() - cCol.getBlue()) * (getBlue() - cCol.getBlue());
		nDist += (getGreen() - cCol.getGreen()) * (getGreen() - cCol.getGreen());
		nDist += (getRed() - cCol.getRed()) * (getRed() - cCol.getRed());
		return nDist;
	}

	/**
	 * calculates the quadratic difference between two colors with alpha channel in the rgb color space
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
	 * Calculates an 'optical distance' between two colors
	 * 
	 * @param cCol
	 *          The 'other' color to be compared with this
	 * @return The optical distance
	 */
	public double oDist(OSMColor cCol)
	{
		double dRDiff = getRed() - cCol.getRed();
		double dGDiff = getGreen() - cCol.getGreen();
		double dBDiff = getBlue() - cCol.getBlue();
		double dDiff = 0.0;
		if (length() > 0.0)
		{
			dDiff = Math.abs((getBlue() * dBDiff + getGreen() * dGDiff + getRed() * dRDiff) / (length() * Math.sqrt(qDist(cCol))));
			if (dDiff > 1.0)
			{
				if (dDiff > 1.0000000000000002)
					log.debug("d-Diff=" + dDiff + "; q-Dist=" + qDist(cCol));
				dDiff = 1.0;
			}
		}
		// double oDist = (2.0 - dDiff) * Math.sqrt(qDist(cCol)) / cCol.length();
		double oDist = (2.0 - dDiff) * Math.sqrt(qDist(cCol));
		if (oDist <= 0)
			log.debug("d-Diff=" + dDiff + "; o-Dist=" + oDist + "; q-Dist=" + qDist(cCol));
		return oDist;
	}

	/**
	 * @return The length in the range [0..sqrt(3)*255]
	 */
	protected double length()
	{
		double dLng = Math.sqrt((getBlue() * getBlue()) + (getGreen() * getGreen()) + (getRed() * getRed()));
		return dLng;
	}

	public String toStringRGB()
	{
		String str = getRed() + "," + getGreen() + "," + getBlue();
		return str;
	}

	public String toStringRGBA()
	{
		String str = getRed() + "," + getGreen() + "," + getBlue() + "," + getAlpha();
		return str;
	}

	public String toStringKmpl()
	{
		String str = "color=RGB(" + toStringRGB() + "), HSL(" + toStringHSL() + ")";
		return str;
	}

	public boolean equals(int nRGB)
	{
		return ((getRed() == ((nRGB & 0xFF0000) >> 16)) && (getGreen() == ((nRGB & 0xFF00) >> 8)) && (getBlue() == ((nRGB & 0xFF))));
	}

	public boolean equals(OSMColor tColor)
	{
		return ((getRed() == tColor.getRed()) && (getGreen() == tColor.getGreen()) && (getBlue() == tColor.getBlue()));
	}

	/**
	 * light = min(r,g,b) + max(r,g,b) / 2 // why not r+g+b/3 ????
	 * sat = max(r,g,b) - min(r,g,b)
	 * 
	 * 
	 */
	public String toStringHSL()
	{
		String strHSL = null;
		double red = getRed() / 255.0;
		double green = getGreen() / 255.0;
		double blue = getBlue() / 255.0;
		double v;
		double m;
		double vm;
		double r2, g2, b2;

		double hue = 0; // default to black
		double sat = 0;
		double light = 0;

		v = Math.max(Math.max(red, green), blue);
		m = Math.min(Math.min(red, green), blue);
		light = (m + v) / 2.0;
		if (light <= 0.0)
		{
			return "0,0,0";
		}
		vm = v - m;
		sat = vm;
		if (sat > 0.0)
		{
			sat /= (light <= 0.5) ? (v + m) : (2.0 - v - m);
		}
		else
		{
			return "0,0," + Math.round(light * 100.0);
		}
		r2 = (v - red) / vm;
		g2 = (v - green) / vm;
		b2 = (v - blue) / vm;
		if (red == v)
		{
			hue = (green == m ? 5.0 + b2 : 1.0 - g2);
		}
		else if (green == v)
		{
			hue = (blue == m ? 1.0 + r2 : 3.0 - b2);
		}
		else
		{
			hue = (red == m ? 3.0 + g2 : 5.0 - r2);
		}
		hue /= 6.0;
		// strHSL = hue + "," + sat + "," + light;
		long nHue = Math.round(hue * 360.0);
		long nSat = Math.round(sat * 100.0);
		long nLight = Math.round(light * 100.0);
		strHSL = nHue + "," + nSat + "," + nLight;
		return strHSL;
	}

	/**
	 * light = min(r,g,b) + max(r,g,b) / 2 // why not r+g+b/3 ????
	 * sat = max(r,g,b) - min(r,g,b)
	 * 
	 * 
	 */
	public int getHSL()
	{
		int nHSL = 0;
		double red = getRed() / 255.0;
		double green = getGreen() / 255.0;
		double blue = getBlue() / 255.0;
		double v;
		double m;
		double vm;
		double r2, g2, b2;

		double hue = 0; // default to black
		double sat = 0;
		double light = 0;

		v = Math.max(Math.max(red, green), blue);
		m = Math.min(Math.min(red, green), blue);
		light = (m + v) / 2.0;
		if (light <= 0.0)
		{
			return 0;
		}
		vm = v - m;
		sat = vm;
		if (sat > 0.0)
		{
			sat /= (light <= 0.5) ? (v + m) : (2.0 - v - m);
		}
		else
		{
			long nLight = Math.round(light * 100.0);
			nHSL |= (nLight) & 0xFF;
			return nHSL;
		}
		r2 = (v - red) / vm;
		g2 = (v - green) / vm;
		b2 = (v - blue) / vm;
		if (red == v)
		{
			hue = (green == m ? 5.0 + b2 : 1.0 - g2);
		}
		else if (green == v)
		{
			hue = (blue == m ? 1.0 + r2 : 3.0 - b2);
		}
		else
		{
			hue = (red == m ? 3.0 + g2 : 5.0 - r2);
		}
		hue /= 6.0;
		// strHSL = hue + "," + sat + "," + light;
		long nHue = Math.round(hue * 360.0);
		long nSat = Math.round(sat * 100.0);
		long nLight = Math.round(light * 100.0);
		nHSL |= (nHue << 16) & 0xFF0000;
		nHSL |= (nSat << 8) & 0xFF00;
		nHSL |= (nLight) & 0xFF;
		return nHSL;
	}

	/**
	 * @return the hue value
	 */
	public int getHue()
	{
		return mHue;
	}

	/**
	 * @param mHue
	 *          the hue value to set, range [1..360]
	 */
	public void setHue(int nHue)
	{
		nHue = Math.max(1, Math.min(360, nHue));
		this.mHue = nHue;
	}

	/**
	 * @return the saturation value
	 */
	public int getSat()
	{
		return mSat;
	}

	/**
	 * @param mSat
	 *          the saturation value to set, range [0..100]
	 */
	public void setSat(int nSat)
	{
		nSat = Math.max(0, Math.min(100, nSat));
		this.mSat = nSat;
	}

	/**
	 * @return the lightness value
	 */
	public int getLight()
	{
		return mLight;
	}

	/**
	 * @param mLight
	 *          the lightness value to set, range [0..100]
	 */
	public void setLight(int nLight)
	{
		nLight = Math.max(0, Math.min(100, nLight));
		this.mLight = nLight;
	}

}
