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

import osmcb.program.bundlecreators.ACBundleCreator;
import osmcb.program.bundlecreators.BCOSMAND_SQlite;
import osmcb.program.bundlecreators.BCOpenCPN;
import osmcb.program.bundlecreators.BCOpenCPN2;
import osmcb.program.bundlecreators.BCTileStoreDownload;
import osmcb.program.bundlecreators.BCTrekBuddy;
import osmcb.program.bundlecreators.BCTrekBuddyTared;
import osmcb.program.bundlecreators.BundleCreatorName;

@XmlRootElement
@XmlJavaTypeAdapter(BundleOutputFormatAdapter.class)
public class BundleOutputFormat implements Comparable<BundleOutputFormat>
{
	public static List<BundleOutputFormat> FORMATS;
	public static final BundleOutputFormat TILESTORE = createByClass(BCTileStoreDownload.class);

	static
	{
		FORMATS = new ArrayList<BundleOutputFormat>(40);
		FORMATS.add(createByClass(BCTrekBuddy.class));
		FORMATS.add(createByClass(BCTrekBuddyTared.class));
		FORMATS.add(createByClass(BCOpenCPN.class));
		FORMATS.add(createByClass(BCOpenCPN2.class));
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
		FORMATS.add(TILESTORE);
	}

	public static Vector<BundleOutputFormat> getFormatsAsVector()
	{
		return new Vector<BundleOutputFormat>(FORMATS);
	}

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
		BundleCreatorName acName = bundleCreatorClass.getAnnotation(BundleCreatorName.class);
		if (acName == null)
			throw new RuntimeException("ACBundleCreator " + bundleCreatorClass.getName() + " has no name");
		String typeName = acName.type();
		if (typeName == null || typeName.length() == 0)
			typeName = bundleCreatorClass.getSimpleName();
		String name = acName.value();
		return new BundleOutputFormat(bundleCreatorClass, typeName, name);
	}

	private BundleOutputFormat(Class<? extends ACBundleCreator> bundleCreatorClass, String typeName, String name)
	{
		this.bundleCreatorClass = bundleCreatorClass;
		this.typeName = typeName;
		this.name = name;
	}

	@Override
	public String toString()
	{
		return name;
	}

	public Class<? extends ACBundleCreator> getMapCreatorClass()
	{
		return bundleCreatorClass;
	}

	public String getTypeName()
	{
		return typeName;
	}

	public ACBundleCreator createBundleCreatorInstance()
	{
		if (bundleCreatorClass == null)
			return null;
		try
		{
			return bundleCreatorClass.newInstance();
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
}
