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
package osmcb.program.bundlecreators.KAPImages;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import osmb.program.ACApp;
import osmb.program.map.IfLayer;
import osmb.program.map.IfMap;
import osmb.utilities.OSMBStrs;
import osmcb.OSMCBSettings;
import osmcb.program.bundle.BundleTestException;
import osmcb.program.bundle.IfBundle;
import osmcb.program.bundle.MapCreationException;
import osmcb.program.bundlecreators.ACBundleCreator;
import osmcb.program.bundlecreators.IfBundleCreatorName;
import osmcb.utilities.OSMCBUtilities;

/**
 * BCOpenCPN2 does nearly the same as BCOpenCPN.
 * The difference is that this only uses the even zoom layers. This is done to decrement the bundles size and the number of maps included.
 * OpenCPN can effectively handle bundles with a wider spacing of zoom level. It is not necessary to have all zoom levels in the bundle.
 * 
 * @author humbach
 *
 */
@IfBundleCreatorName(value = "OpenCPN KAP bundle", type = "OpenCPN2")
// @SupportedTIParameters(names = {Name.format, Name.height, Name.width})
public class BCOpenCPN2 extends BCOpenCPN
{
	// protected File layerDir = null;
	// protected File mapDir = null;

	// protected MapTileWriter mapTileWriter;

	public BCOpenCPN2()
	{
		super();
	}

	public BCOpenCPN2(IfBundle bundle, File bundleOutputDir)
	{
		super(bundle, bundleOutputDir);
	}

	// general bundle actions
	/**
	 * chance to test a bundle before starting the creation
	 * 
	 * @throws BundleTestException
	 */
	@Override
	protected void testBundle() throws BundleTestException
	{
		log.trace(OSMBStrs.RStr("START"));
		try
		{
			for (IfLayer layer : mBundle)
			{
				for (IfMap map : layer)
				{
					if ((map.getZoom() & 0x1) == 0x0)
					{
						if (!testMapSource(map.getMapSource()))
							throw new BundleTestException(
							    "The selected bundle output format \"" + mBundle.getOutputFormat() + "\" does not support the map source \"" + map.getMapSource() + "\"");
					}
				}
			}
			log.info("bundle with " + mBundle.calcMapsToCompose() + " maps, " + mBundle.calculateTilesToLoad() + " tiles");
			log.trace("bundle successfully tested");
		}
		catch (BundleTestException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new BundleTestException(e);
		}
		log.trace("bundle='" + mBundle.getName() + "' tested");
	}

	/**
	 * Creates a format specific directory for all OpenCPN-KAP bundles.
	 */
	@Override
	public void initializeBundle() throws IOException, BundleTestException
	{
		log.trace("START");
		File bundleOutputDir = ((OSMCBSettings) ACApp.getApp().getSettings()).getChartBundleOutputDirectory();
		bundleOutputDir = new File(bundleOutputDir, "OpenCPN-KAP2");
		OSMCBUtilities.mkDirs(bundleOutputDir);
		SimpleDateFormat sdf = new SimpleDateFormat(STR_BUFMT);
		String bundleDirName = "OSM-OpenCPN-KAP2-" + mBundle.getName() + "-" + sdf.format(new Date());
		bundleOutputDir = new File(bundleOutputDir, bundleDirName);
		super.initializeBundle(bundleOutputDir);
	}

	@Override
	public void finishBundle()
	{
		log.trace(OSMBStrs.RStr("START"));
		super.finishBundle();
		log.debug("Bundle dir='" + mOutputDir + "'");
		// zip bundle 'mOutputDir' is the root dir for this bundle
		// try
		// {
		// OsmbLzma.encode7z(mOutputDir.getAbsolutePath(), mOutputDir + ".7z");
		// }
		// catch (IOException e)
		// {
		// log.error("7zip excepted '" + mOutputDir.getAbsolutePath() + "'");
		// e.printStackTrace();
		// }
	}

	@Override
	public void createInfoFile()
	{
		createInfoFile("OpenSeaMap Charts KAP Bundle 0.1\r\n");
	}

	@Override
	public void initializeLayer() throws IOException
	{
		sBundleProgress.initLayer(mLayer);
	}

	@Override
	public void createLayer() throws IOException, InterruptedException
	{
		log.trace("START");
		File mapOutputDir = mOutputDir;
		for (IfMap tMap : mLayer)
		{
			if ((tMap.getZoom() & 0x1) == 0x0)
			{
				ACBundleCreator mapCreator;
				try
				{
					mapCreator = mBundle.createMapCreatorInstance();
					mapCreator.init(mBundle, mLayer, tMap, mapOutputDir);
					mExec.execute(mapCreator);
				}
				catch (InstantiationException | IllegalAccessException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		log.trace("layer='" + mLayer.getName() + "' created");
	}

	@Override
	public void finishLayer() throws IOException
	{
		sBundleProgress.finishLayer(mLayer);
	}

	/**
	 * put all tiles together in one file per map. this file is in .kap format
	 */
	@Override
	public void createMap() throws MapCreationException, InterruptedException
	{
		log.trace("START");
		// only do maps in even zoom layers
		if ((mMap.getZoom() & 0x1) == 0x0)
		{
			super.createMap();
		}
	}
}
