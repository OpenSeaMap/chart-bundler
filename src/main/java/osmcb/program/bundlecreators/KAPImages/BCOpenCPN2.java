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

import osmb.exceptions.InvalidNameException;
import osmb.program.ACApp;
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
	protected static final String STR_BUNDLE_TYPE = "OpenCPN2-KAP";
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
	 * Creates a format specific directory for all OpenCPN-KAP bundles.
	 * 
	 * @throws InvalidNameException
	 */
	@Override
	public void initializeBundle() throws IOException, BundleTestException, InvalidNameException
	{
		sLog.trace("START");
		File bundleOutputDir = ((OSMCBSettings) ACApp.getApp().getSettings()).getChartBundleOutputDirectory();
		bundleOutputDir = new File(bundleOutputDir, "OpenCPN2-KAP");
		OSMCBUtilities.mkDirs(bundleOutputDir);
		SimpleDateFormat sdf = new SimpleDateFormat(STR_BUFMT);
		mBundle.setBaseName("OSM-" + STR_BUNDLE_TYPE + "-" + mBundle.getName());
		String bundleDirName = mBundle.getBaseName() + "-" + sdf.format(new Date());
		bundleOutputDir = new File(bundleOutputDir, bundleDirName);
		sCompletedMaps.set(0);
		sDownloadedTiles.set(0);
		super.initializeBundle(bundleOutputDir);
	}

	@Override
	public void finishBundle()
	{
		sLog.trace(OSMBStrs.RStr("START"));
		super.finishBundle();
		sLog.debug("Bundle dir='" + mOutputDir + "'");
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
		sLog.trace("START");
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
		sLog.trace("layer='" + mLayer.getName() + "' created");
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
		sLog.trace("START");
		// only do maps in even zoom layers
		if ((mMap.getZoom() & 0x1) == 0x0)
		{
			super.createMap();
		}
	}
}
