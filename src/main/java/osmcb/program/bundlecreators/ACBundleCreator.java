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
package osmcb.program.bundlecreators;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.EnumSet;

import org.apache.log4j.Logger;

import osmb.mapsources.IfMapSource;
import osmb.program.map.IfLayer;
import osmb.program.map.IfMap;
import osmb.program.tiles.TileImageFormat;
import osmb.program.tiles.TileImageParameters;
import osmb.utilities.Charsets;
import osmcb.OSMCBSettings;
import osmcb.program.bundle.ACBundleProgress;
import osmcb.program.bundle.BundleOutputFormat;
import osmcb.program.bundle.BundleTestException;
import osmcb.program.bundle.BundleThread;
import osmcb.program.bundle.IfBundle;
import osmcb.program.bundle.MapCreationException;
import osmcb.program.bundlecreators.tileprovider.TileProvider;
import osmcb.utilities.OSMCBUtilities;

/**
 * Abstract base class for all ACBundleCreator implementations.
 * 
 * The general call schema is as follows:
 * <ol>
 * <li>ACBundleCreator instantiation via {@link BundleOutputFormat#createAtlasCreatorInstance()}</li>
 * <li>ACBundleCreator bundle initialization via {@link #startBundleCreation(IfBundle, File)}</li>
 * <li>n times {@link #initializeMap(IfMap, TileProvider)} followed by {@link #createMap()}</li>
 * <li>ACBundleCreator bundle finalization via {@link #finishBundleCreation()}</li>
 * </ol>
 */
public abstract class ACBundleCreator
{
	public static final Charset TEXT_FILE_CHARSET = Charsets.ISO_8859_1;
	protected final Logger log;

	/************************************************************/
	/** bundle specific fields **/
	/************************************************************/
	protected IfBundle bundle;
	protected File bundleDir;
	protected ACBundleProgress bundleProgress = null;
	// protected PauseResumeHandler pauseResumeHandler = null;

	/************************************************************/
	/** iMap specific fields **/
	/************************************************************/
	protected IfMap map;
	protected int xMin;
	protected int xMax;
	protected int yMin;
	protected int yMax;
	protected int zoom;
	protected IfMapSource mapSource;
	protected int tileSize;

	/**
	 * Custom tile processing parameters. <code>null</code> if disabled in GUI
	 */
	protected TileImageParameters parameters;
	protected BundleOutputFormat bundleOutputFormat;
	protected TileProvider mapDlTileProvider;
	private boolean aborted = false;

	/**
	 * Default constructor - initializes the logging environment
	 */
	protected ACBundleCreator()
	{
		log = Logger.getLogger(this.getClass());
	};

	// /**
	// * @param customBundlesDir
	// * if not <code>null</code> the customBudlesDir is used instead of the generated bundle directory name
	// * @throws InterruptedException
	// * @see AtlasCreator
	// */
	// public void startBundleCreation(IfBundle bundle, File customBundlesDir) throws BundleTestException, IOException, InterruptedException
	// {
	// this.bundle = bundle;
	// testBundle();
	//
	// if (customBundlesDir == null)
	// {
	// // No explicit bundle output directory has been set - generate a probably unique directory name
	// SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
	// String bundleDirName = bundle.getName() + "-" + sdf.format(new Date());
	// File bundleOutputDir = OSMCBSettings.getInstance().getChartBundleOutputDirectory();
	// bundleDir = new File(bundleOutputDir, bundleDirName);
	// }
	// else
	// bundleDir = customBundlesDir;
	// OSMCBUtilities.mkDirs(bundleDir);
	// }

	/**
	 * chance to test a bundle before starting the creation
	 * 
	 * @throws BundleTestException
	 */
	protected void testBundle() throws BundleTestException
	{
	}

	public void setBundleProgress(ACBundleProgress bP)
	{
		this.bundleProgress = bP;
	}

	public void abortBundleCreation() throws IOException
	{
		this.aborted = true;
	}

	public boolean isAborted()
	{
		return aborted;
	}

	/**
	 * Test if the {@link ACBundleCreator} instance supports the selected {@link IfMapSource}. This test has to be performed and implemented by the creator
	 * instance.
	 * 
	 * @param mapSource
	 * @return <code>true</code> if supported otherwise <code>false</code>
	 * @see AtlasCreator
	 */
	public abstract boolean testMapSource(IfMapSource mapSource);

	// Bundle actions
	public void initializeBundle(IfBundle bundle, File customBundleDir) throws IOException, BundleTestException
	{
		this.bundle = bundle;
		testBundle();

		if (customBundleDir == null)
		{
			// No explicit bundle output directory has been set - generate a probably unique directory name
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd_HHmmss");
			String bundleDirName = bundle.getName() + "-" + sdf.format(new Date());
			File bundleOutputDir = OSMCBSettings.getInstance().getChartBundleOutputDirectory();
			bundleDir = new File(bundleOutputDir, bundleDirName);
		}
		else
			bundleDir = customBundleDir;
		OSMCBUtilities.mkDirs(bundleDir);
	}

	public void finishBundle(IfBundle bundle)
	{

	}

	// Layer actions
	public void initializeLayer(IfLayer layer) throws IOException
	{
		log.trace("initializeLayer(): '" + layer.getName() + "' started");
	}

	public void createLayer(IfLayer layer) throws IOException, InterruptedException
	{

	}

	public void finishLayer(IfLayer layer) throws IOException
	{

	}

	// Map actions
	/**
	 * @throws IOException
	 * @see ACBundleCreator
	 */
	public void initializeMap(IfMap map, TileProvider mapTileProvider) throws IOException
	{
		IfLayer layer = map.getLayer();
		if (mapTileProvider == null)
			throw new NullPointerException();
		this.mapDlTileProvider = mapTileProvider;
		this.map = map;
		this.mapSource = map.getMapSource();
		this.tileSize = mapSource.getMapSpace().getTileSize();
		this.parameters = map.getParameters();
		xMin = map.getMinTileCoordinate().x / tileSize;
		xMax = map.getMaxTileCoordinate().x / tileSize;
		yMin = map.getMinTileCoordinate().y / tileSize;
		yMax = map.getMaxTileCoordinate().y / tileSize;
		this.zoom = map.getZoom();
		// this.bundleOutputFormat = layer.getBundle().getOutputFormat();

		Thread t = Thread.currentThread();
		if (!(t instanceof BundleThread))
			throw new RuntimeException("Calling thread must be BundleThread!");
		BundleThread at = (BundleThread) t;
		bundleProgress = at.getBundleProgress();
		// pauseResumeHandler = at.getPauseResumeHandler();
	}

	public void createMap(IfMap map) throws MapCreationException, InterruptedException
	{

	}

	/**
	 * @throws InterruptedException
	 * @see AtlasCreator
	 */
	public abstract void createMap() throws MapCreationException, InterruptedException;

	/**
	 * Checks if the user has aborted bundle creation and if <code>true</code> an {@link InterruptedException} is thrown.
	 * 
	 * @throws InterruptedException
	 */
	public void checkUserAbort() throws InterruptedException
	{
		if (Thread.currentThread().isInterrupted())
			throw new InterruptedException();
		// pauseResumeHandler.pauseWait();
	}

	// public BundleProgress getBundleProgress() {
	// return BundleProgress;
	// }
	//
	public int getXMin()
	{
		return xMin;
	}

	public int getXMax()
	{
		return xMax;
	}

	public int getYMin()
	{
		return yMin;
	}

	public int getYMax()
	{
		return yMax;
	}

	public IfMap getMap()
	{
		return map;
	}

	public TileImageParameters getParameters()
	{
		return parameters;
	}

	public TileProvider getMapDlTileProvider()
	{
		return mapDlTileProvider;
	}

	/**
	 * Tests all maps of the currently active bundle if a custom tile image format has been specified and if the specified format is equal to the
	 * <code>allowedFormat</code>.
	 * 
	 * @param allowedFormat
	 * @throws AtlasTestException
	 */
	protected void performTestBundleTileFormat(EnumSet<TileImageFormat> allowedFormats) throws BundleTestException
	{
		for (IfLayer layer : bundle)
		{
			for (IfMap map : layer)
			{
				TileImageParameters parameters = map.getParameters();
				if (parameters == null)
					continue;
				if (!allowedFormats.contains(parameters.getFormat()))
					throw new BundleTestException("Selected custom tile format not supported - only the following format(s) are supported: " + allowedFormats, map);
			}
		}
	}

	protected void performTest_MaxMapZoom(int maxZoom) throws BundleTestException
	{
		for (IfLayer layer : bundle)
		{
			for (IfMap map : layer)
			{
				if (map.getZoom() > maxZoom)
					throw new BundleTestException("Maximum zoom is " + maxZoom + " for this bundle format", map);
			}
		}
	}

}
