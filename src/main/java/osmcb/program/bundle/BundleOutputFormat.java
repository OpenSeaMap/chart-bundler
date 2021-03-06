/*******************************************************************************
 * Copyright (c) OSMCB developers
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package osmcb.program.bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Vector;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

import org.apache.log4j.Logger;

import osmb.program.map.IfLayer;
import osmcb.program.bundlecreators.ACBundleCreator;
import osmcb.program.bundlecreators.BCTileStoreDownload;
import osmcb.program.bundlecreators.IfBundleCreatorName;
import osmcb.program.bundlecreators.KAPImages.BCOpenCPN;
import osmcb.program.bundlecreators.KAPImages.BCOpenCPN2;
import osmcb.program.bundlecreators.KAPImages.BCOpenCPNZip;
import osmcb.program.bundlecreators.SQLite.BCOSMAND_SQlite;
import osmcb.program.bundlecreators.TrekBuddy.BCTrekBuddy;
import osmcb.program.bundlecreators.TrekBuddy.BCTrekBuddyTared;

/**
 * The BundleOutputFormat allows the selection of the actual class to be used by its name.
 */
@XmlRootElement
@XmlJavaTypeAdapter(BundleOutputFormatAdapter.class)
public class BundleOutputFormat implements Comparable<BundleOutputFormat>
{
	protected static Logger sLog = Logger.getLogger(BundleOutputFormatAdapter.class);
	public static List<BundleOutputFormat> FORMATS;

	static
	{
		FORMATS = new ArrayList<BundleOutputFormat>(40);
		FORMATS.add(createByClass(BCTileStoreDownload.class));
		FORMATS.add(createByClass(BCTrekBuddy.class));
		FORMATS.add(createByClass(BCTrekBuddyTared.class));
		FORMATS.add(createByClass(BCOpenCPN.class));
		FORMATS.add(createByClass(BCOpenCPN2.class));
		FORMATS.add(createByClass(BCOpenCPNZip.class));
		FORMATS.add(createByClass(BCOSMAND_SQlite.class));
		// FORMATS.add(createByClass(BCAFTrack.class));
		// FORMATS.add(createByClass(BCAlpineQuestMap.class));
		// FORMATS.add(createByClass(BCAndNav.class));
		// FORMATS.add(createByClass(BCBackCountryNavigator.class));
		// FORMATS.add(createByClass(BCBigPlanetTracks.class));
		// FORMATS.add(createByClass(BCCacheBox.class));
		// FORMATS.add(createByClass(BCCacheWolf.class));
		// FORMATS.add(createByClass(BCGalileo.class));
		// FORMATS.add(createByClass(BCGarminCustom.class));
		// FORMATS.add(createByClass(BCGCLive.class));
		// FORMATS.add(createByClass(BCGlopus.class));
		// FORMATS.add(createByClass(BCGlopusMapFile.class));
		// FORMATS.add(createByClass(BCGoogleEarthOverlay.class));
		// FORMATS.add(createByClass(BCGpsSportsTracker.class));
		// FORMATS.add(createByClass(BCIPhone3MapTiles5.class));
		// FORMATS.add(createByClass(BCMagellanRmp.class));
		// FORMATS.add(createByClass(BCMaplorer.class));
		// FORMATS.add(createByClass(BCMaverick.class));
		// FORMATS.add(createByClass(BCMBTiles.class));
		// FORMATS.add(createByClass(BCMGMaps.class));
		// FORMATS.add(createByClass(BCMobileTrailExplorer.class));
		// FORMATS.add(createByClass(BCMobileTrailExplorerCache.class));
		// FORMATS.add(createByClass(BCNaviComputer.class));
		// FORMATS.add(createByClass(BCNFComPass.class));
		// FORMATS.add(createByClass(BCOruxMaps.class));
		// FORMATS.add(createByClass(BCOruxMapsSqlite.class));
		// FORMATS.add(createByClass(BCOSMAND.class));
		// FORMATS.add(createByClass(BCOSMAND_SQlite.class));
		// FORMATS.add(createByClass(BCOsmdroid.class));
		// FORMATS.add(createByClass(BCOsmdroidGEMF.class));
		// FORMATS.add(createByClass(BCOsmdroidSQLite.class));
		// FORMATS.add(createByClass(BCOSMTracker.class));
		// FORMATS.add(createByClass(BCOzi.class));
		// FORMATS.add(createByClass(PaperAtlasPdf.class));
		// FORMATS.add(createByClass(PaperAtlasPng.class));
		// FORMATS.add(createByClass(BCPathAway.class));
		// FORMATS.add(createByClass(BCPNGWorldfile.class));
		// FORMATS.add(createByClass(BCRMapsSQLite.class));
		// FORMATS.add(createByClass(BCRunGPSAtlas.class));
		// FORMATS.add(createByClass(BCSportsTracker.class));
		// FORMATS.add(createByClass(BCTomTomRaster.class));
		// FORMATS.add(createByClass(BCTTQV.class));
		// FORMATS.add(createByClass(BCTwoNavRMAP.class));
		// FORMATS.add(createByClass(BCUblox.class));
		// FORMATS.add(createByClass(BCViewranger.class));
	}

	/**
	 * This is never used.
	 * 
	 * @return
	 */
	public static Vector<BundleOutputFormat> getFormatsAsVector()
	{
		return new Vector<BundleOutputFormat>(FORMATS);
	}

	/**
	 * This actually returns the BundleOutputFormat object specified by its name.
	 */
	public static BundleOutputFormat getFormatByName(String Name)
	{
		for (BundleOutputFormat af : FORMATS)
		{
			if (af.getTypeName().equals(Name))
				return af;
		}
		throw new NoSuchElementException("Unknown bundle format: \"" + Name + "\"");
	}

	private Class<? extends ACBundleCreator> bundleCreatorClass;
	private String typeName;
	private String name;

	private static BundleOutputFormat createByClass(Class<? extends ACBundleCreator> bundleCreatorClass)
	{
		IfBundleCreatorName acName = bundleCreatorClass.getAnnotation(IfBundleCreatorName.class);
		if (acName == null)
			throw new RuntimeException("ACBundleCreator '" + bundleCreatorClass.getName() + "' has no name");
		String typeName = acName.type();
		if (typeName == null || typeName.length() == 0)
			typeName = bundleCreatorClass.getSimpleName();
		String name = acName.value();
		return new BundleOutputFormat(bundleCreatorClass, typeName, name);
	}

	private BundleOutputFormat(Class<? extends ACBundleCreator> bundleCreatorClass, String typeName, String name)
	{
		sLog = Logger.getLogger(this.getClass());
		this.setBundleCreatorClass(bundleCreatorClass);
		this.typeName = typeName;
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}

	/**
	 * Seems never to be used.
	 * 
	 * @return
	 */
	public Class<? extends ACBundleCreator> getMapCreatorClass()
	{
		return getBundleCreatorClass();
	}

	public String getTypeName()
	{
		return typeName;
	}

	/**
	 * This actually creates the bundle creator instance, which then in turn does the bundle creation.
	 * 
	 * @return An instance of a subclass of ACBundleCreator.
	 */
	public ACBundleCreator createBundleCreatorInstance()
	{
		if (getBundleCreatorClass() == null)
			return null;
		try
		{
			return getBundleCreatorClass().newInstance();
		}
		catch (Throwable t)
		{
			throw new RuntimeException(t);
		}
	}

	@Override
	public int compareTo(BundleOutputFormat o)
	{
		return getTypeName().compareTo(o.toString());
	}

	public Class<? extends ACBundleCreator> getBundleCreatorClass()
	{
		return bundleCreatorClass;
	}

	public void setBundleCreatorClass(Class<? extends ACBundleCreator> bundleCreatorClass)
	{
		this.bundleCreatorClass = bundleCreatorClass;
	}

	public boolean filterLayers(IfLayer layer)
	{
		boolean bOK = true;
		if (BCOpenCPN2.class == bundleCreatorClass)
			bOK = ((layer.getZoomLvl() & 0x1) == 0x0);
		return bOK;
	}
}
