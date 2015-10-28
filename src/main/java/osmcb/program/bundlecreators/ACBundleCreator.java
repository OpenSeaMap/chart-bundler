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
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import osmb.mapsources.IfFileBasedMapSource;
import osmb.mapsources.IfMapSource;
import osmb.mapsources.mapspace.MercatorPower2MapSpace;
import osmb.program.JobDispatcher;
import osmb.program.map.IfLayer;
import osmb.program.map.IfMap;
import osmb.program.map.IfMapSpace;
import osmb.program.map.IfMapSpace.ProjectionCategory;
import osmb.program.tiles.IfTileLoaderListener;
import osmb.program.tiles.IfTileProvider;
import osmb.program.tiles.MemoryTileCache;
import osmb.program.tiles.Tile;
import osmb.program.tiles.TileImageParameters;
import osmb.program.tiles.TileLoader;
import osmb.program.tilestore.ACSiTileStore;
import osmb.program.tilestore.berkeleydb.TileDbEntry;
import osmb.utilities.Charsets;
import osmcb.OSMCBSettings;
import osmcb.OSMCBStrs;
import osmcb.program.bundle.ACBundleProgress;
import osmcb.program.bundle.BundleOutputFormat;
import osmcb.program.bundle.BundleTestException;
import osmcb.program.bundle.IfBundle;
import osmcb.program.bundle.MapCreationException;
import osmcb.ui.BundleProgress;
import osmcb.utilities.OSMCBUtilities;

/**
 * Abstract base class for all ACBundleCreator implementations.
 * 
 * From the view of software structure, this should be three classes: ACBundleCreator, ACLayerCreator, ACMapCreator.
 * But prioritizing convenience in implementing an actual bundle format these three classes are merged into one BundleCreator class.
 * It features three constructors for the three 'flavours' of BundleCreators.
 * 
 * The glue, common to all implementations, is implemented in this ACBundleCreator class and should not be overridden by implementations.
 * 
 */
public class ACBundleCreator implements IfBundleCreator, IfTileLoaderListener
{
	public static final Charset TEXT_FILE_CHARSET = Charsets.ISO_8859_1;

	protected static ACBundleProgress sBundleProgress = null; // all messages regarding the progress go there
	private AtomicInteger mActiveJobs = new AtomicInteger(0);
	private AtomicInteger mJobsCompleted = new AtomicInteger(0);
	private AtomicInteger mJobsRetryError = new AtomicInteger(0);
	private AtomicInteger mJobsPermanentError = new AtomicInteger(0);

	protected final Logger log;

	// protected ExecutorService mExec = null;
	protected JobDispatcher mExec = null;
	protected IfBundle mBundle = null;
	protected IfLayer mLayer = null;
	protected IfMap mMap = null;

	protected ACSiTileStore mTS = ACSiTileStore.getInstance();

	protected int tileSize = 256;
	protected long mTileCount = 0;
	// protected PauseResumeHandler pauseResumeHandler = null;

	/**
	 * fields specific to the current map
	 */
	// protected int xMin;
	// protected int xMax;
	// protected int yMin;
	// protected int yMax;
	// protected int zoom;
	// protected IfMapSource mapSource;

	/**
	 * Custom tile processing parameters. <code>null</code> if disabled in GUI
	 */
	protected TileImageParameters parameters = null;
	protected BundleOutputFormat bundleOutputFormat = null;
	protected IfTileProvider mapDlTileProvider = null;
	protected File mOutputDir = null;
	/**
	 * way out
	 */
	private boolean aborted = false;
	// private DownloadJobProducerThread djp;
	// private JobDispatcher downloadJobDispatcher;

	/**
	 * necessary for instantiation via newInstance()
	 * Top-level constructor - this is the starting point for the whole bundle. It recursively executes threads for each layer. The number of layers is the
	 * 'natural'
	 * limit for the the number of threads here.
	 */
	public ACBundleCreator()
	{
		log = Logger.getLogger(this.getClass());
	};

	/**
	 * The thread pools will be created for each generation in the init method.
	 * This init() is used when initializing a bundle creation
	 * 
	 * @param bundle
	 */
	public void init(IfBundle bundle, File bundleOutputDir)
	{
		mBundle = bundle;
		mOutputDir = bundleOutputDir;
		mExec = new JobDispatcher(mBundle.getLayerCount());
		log.trace("bundle '" + mBundle.getName() + "' pool for layers=" + mExec.getMaximumPoolSize() + ", " + mExec.toString());
	};

	/**
	 * This init() is used when initializing a layer creation
	 * 
	 * @param bundle
	 * @param layer
	 * @param layerOutputDir
	 */
	public void init(IfBundle bundle, IfLayer layer, File layerOutputDir)
	{
		mBundle = bundle;
		mLayer = layer;
		mOutputDir = layerOutputDir;
		// limit the number of threads, maybe we will get the limit value from settings in the future
		// we will need some kind of dynamic max thread limit, depending on the number of remaining maps and layers
		int nThreads = 10;
		if (mLayer.getMapCount() < nThreads)
			nThreads = mLayer.getMapCount();
		mExec = new JobDispatcher(nThreads);
		log.trace("layer '" + mLayer.getName() + "' pool for maps=" + mExec.getMaximumPoolSize() + ", " + mExec.toString());
	};

	/**
	 * This init() is used when initializing a map creation
	 * 
	 * @param bundle
	 * @param layer
	 * @param map
	 * @param mapOutputDir
	 */
	public void init(IfBundle bundle, IfLayer layer, IfMap map, File mapOutputDir)
	{
		mBundle = bundle;
		mLayer = layer;
		mMap = map;
		mOutputDir = mapOutputDir;
		// limit the number of threads, maybe we will get the limit value from settings in the future
		// we will need some kind of dynamic max thread limit, depending on the number of remaining maps and layers
		int nThreads = 10;
		if (mMap.getTileCount() < nThreads)
			nThreads = (int) mMap.getTileCount();
		mExec = new JobDispatcher(nThreads);
		log.trace("map '" + mMap.getName() + "' pool for tiles=" + mExec.getMaximumPoolSize() + ", " + mExec.toString());
		// log.debug("map pool threads=" + nThreads);
	};

	/**
	 * * @see {@link osmcb.program.bundlecreators.IfBundleCreator#run()}
	 */
	@Override
	public void run()
	{
		log.trace(OSMCBStrs.RStr("BundleThread.CB.RunCalled"));
		if (sBundleProgress == null)
			sBundleProgress = new BundleProgress(this);
		try
		{
			if (mMap != null)
			{
				runMap();
			}
			else if (mLayer != null)
			{
				runLayer();
			}
			else if (mBundle != null)
			{
				runBundle();
			}
			else
				log.error(OSMCBStrs.RStr("BundleThread.CB.ModeUnknown"));
		}
		catch (Exception e)
		{
			log.error(OSMCBStrs.RStr("BundleCreator.Interrupted"));
			e.printStackTrace();
		}
		System.gc();
	}

	/**
	 * Should be called only once. Provides the top level for the bundle creation. In a first step it performs some tests if the specified data are ok, then it
	 * actually creates the bundle in a three step process.
	 * - initializeBundle(); creates the necessary directories etc...
	 * - createBundle(); loops over all layer in this bundle and starts a new thread for each layer
	 * - finishBundle();
	 */
	protected void runBundle()
	{
		log.trace(OSMCBStrs.RStr("BundleThread.CB.RunBundleCalled") + " [" + Thread.currentThread().getName() + "], pool=" + mExec.toString());
		// log.trace("creation of bundle='" + mBundle.getName() + "' started with some tests");
		// log.debug(OSMCBStrs.RStr("BundleThread.CB.ModeUnknown", mBundle.getName()));
		// do some top-level preflight checks
		try
		{
			testBundle();
		}
		catch (BundleTestException e)
		{
			log.error(OSMCBStrs.RStr("BundleThread.CB.BundleTestFailed") + e.getMessage());
			return;
		}
		log.trace("test of bundle='" + mBundle.getName() + "' successful");

		// actually create a bundle. The tiles will be downloaded when the map requests them.
		try
		{
			log.trace("before BC.initializeBundle()");
			initializeBundle();
			createBundle();
			// wait for the bundle creation to finish
			mExec.shutdown();
			while (!mExec.isTerminated())
			{
				log.trace("running jobs=" + mActiveJobs + ", " + mExec.getActiveCount() + ", total jobs=" + mExec.getTaskCount());
				Thread.sleep(1000);
			}
			log.debug("after shutdown(), completed tasks=" + mExec.getCompletedTaskCount() + ", total jobs=" + mExec.getTaskCount());
			finishBundle();
			jobFinishedSuccessfully(0);
			log.debug("bundle '" + mBundle.getName() + "' finished");
		}
		catch (BundleTestException e)
		{
			log.error(OSMCBStrs.RStr("BundleThread.CB.FormatInvalid") + e.getMessage());
			return;
		}
		catch (IOException e)
		{
			log.error(OSMCBStrs.RStr("BundleThread.CB.IOException") + e.getMessage());
			return;
		}
		catch (InterruptedException e)
		{
			log.error(OSMCBStrs.RStr("BundleThread.CB.IOException") + e.getMessage());
			return;
		}
	}

	/**
	 * Is called for each layer. No tests are performed since the bundle is already declared as ok.
	 * It actually creates the layer in a three step process.
	 * - initializeLayer(); creates the necessary directories etc...
	 * - createLayer(); Loops over all maps in this layer
	 * - finishLayer();
	 */
	protected void runLayer()
	{
		log.trace(OSMCBStrs.RStr("BundleThread.CB.RunLayerCalled") + " [" + Thread.currentThread().getName() + "], pool=" + mExec.toString());
		try
		{
			initializeLayer();
			log.trace("after initializeLayer()");
			createLayer();
			// wait for the layer creation to finish
			log.trace("after createLayer()");
			mExec.shutdown();
			while (!mExec.isTerminated())
			{
				log.trace("running jobs=" + mActiveJobs + ", " + mExec.getActiveCount());
				Thread.sleep(1000);
			}
			log.debug("after shutdown(), completed tasks=" + mExec.getCompletedTaskCount() + ", total jobs=" + mExec.getTaskCount());
			finishLayer();
			log.trace("after finishLayer()");
			jobFinishedSuccessfully(0);
			log.debug("layer '" + mLayer.getName() + "' finished");
		}
		catch (IOException | InterruptedException e)
		{
			e.printStackTrace();
			jobFinishedWithError(false);
		}
	}

	/**
	 * Is called for each map. No tests are performed because the bundle is already declared for ok.
	 * It actually creates the layer in a three step process.
	 * - initializeMap(); creates the necessary directories etc...
	 * - downloadMapTiles(); downloads all tiles which are not yet in the tile store available.
	 * - createMap(); actually build a map (usually one file, but some formats handle that different) from the tiles.
	 * - finishMap();
	 * 
	 * This is the 'synchronized' version of runMap. It waits for completion of all downloads before starting to actually create the map.
	 * For performance issues, it seems reasonable to have a 'unsynchronized' version, which does only use the current tiles in the tile store.
	 * Then there has to be a 'partnered' action, which schedules tiles for download, downloads them and updates the entries in the tile store independent of the
	 * current bundle creation. We even might implement a completely separate process for updating the tile stores entries.
	 */
	protected void runMap()
	{
		log.trace(OSMCBStrs.RStr("BundleThread.CB.RunMapCalled") + " [" + Thread.currentThread().getName() + "], pool=" + mExec.toString());
		try
		{
			mTS.prepareTileStore(mMap.getMapSource());
			initializeMap();
			// load all necessary tiles. They should go directly into the tile store...
			loadMapTiles();
			// wait here for the loading of all tiles to finish
			mExec.shutdown();
			while (!mExec.isTerminated())
			{
				log.trace("running jobs=" + mExec.getCompletedTaskCount() + ", " + mExec.getActiveCount());
				Thread.sleep(1000);
			}
			log.debug("after shutdown(), completed tasks=" + mExec.getCompletedTaskCount() + ", total jobs=" + mExec.getTaskCount());
			// only if the output format is not 'tilestore only' we actually create a map
			if (!BundleOutputFormat.TILESTORE.equals(mBundle.getOutputFormat()))
			{
				// create the map from all downloaded tiles
				createMap();
				// wait for the map creation to finish
				finishMap();
				log.debug("map '" + mMap.getName() + "' finished");
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (MapCreationException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			jobFinishedWithError(false);
		}
	}

	/**
	 * somehow the {@link ACBundleProgress} displays or logs the creation progress.
	 * 
	 * @param bP
	 */
	final static public void setBundleProgress(ACBundleProgress bP)
	{
		sBundleProgress = bP;
	}

	public void jobStarted()
	{
		sBundleProgress.setJobs(mActiveJobs.incrementAndGet());
	}

	public void jobFinishedSuccessfully(int bytesDownloaded)
	{
		mActiveJobs.decrementAndGet();
		mJobsCompleted.incrementAndGet();
	}

	public void jobFinishedWithError(boolean retry)
	{
		mActiveJobs.decrementAndGet();
		if (retry)
			mJobsRetryError.incrementAndGet();
		else
		{
			mJobsPermanentError.incrementAndGet();
		}
	}

	/**
	 * Shutdown the complete creation process. Terminate all running threads. This works on three different thread pools depending on the call level.
	 */
	public void shutdown()
	{
		mExec.shutdown();
	}

	/**
	 *
	 */
	final public void abortBundleCreation()
	{
		try
		{
			shutdown();
		}
		catch (Exception e)
		{
			log.error("Exception thrown in stopDownload()" + e.getMessage());
		}
		this.aborted = true;
		log.trace("bundle='" + mBundle.getName() + "' aborted");
	}

	// general bundle actions
	/**
	 * chance to test a bundle before starting the creation
	 * 
	 * @throws BundleTestException
	 */
	protected void testBundle() throws BundleTestException
	{
		try
		{
			for (IfLayer layer : mBundle)
			{
				for (IfMap map : layer)
				{
					if (!testMapSource(map.getMapSource()))
						throw new BundleTestException(
						    "The selected bundle output format \"" + mBundle.getOutputFormat() + "\" does not support the map source \"" + map.getMapSource() + "\"");
				}
			}
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
	 * Test if the {@link ACBundleCreator} instance supports the selected {@link IfMapSource}. This test has to be performed and implemented by the creator
	 * instance.
	 * 
	 * @param mapSource
	 * @return <code>true</code> if supported otherwise <code>false</code>
	 * @see AtlasCreator
	 */
	protected boolean testMapSource(IfMapSource mapSource)
	{
		IfMapSpace mapSpace = mapSource.getMapSpace();
		return (mapSpace instanceof MercatorPower2MapSpace && ProjectionCategory.SPHERE.equals(mapSpace.getProjectionCategory()));
	}

	/**
	 * This creates a standard disclaimer. In the future this disclaimer should come from some file, so that it can be updated without a revision of the code
	 * 
	 * @return
	 */
	public String createGeneralDisclaimer()
	{
		String strDisclaimer = "";
		strDisclaimer += "This Charts are useable for testing ONLY, it is in no way fit for navigational purposes.\r\n";
		strDisclaimer += "\r\n";
		strDisclaimer += "OpenSeaMap does not give any warranty, that the data shown in this map are real.\r\n";
		strDisclaimer += "Even if you use it for testing, any damage resulting from this test will be solely your responsibility.\r\n";
		return strDisclaimer;
	}

	/**
	 * Currently it creates a simple user agreement. The bundle info file is a simple description and license information about the charts in this bundle.
	 * This is usually overwritten in the implementation and calls {@link #createInfoFile(String)}
	 * 
	 * In the future it should also create a file containing some information about the bundles contents. This text should be composed of one part incorporated in
	 * the catalog, containing information about what is included in the catalog; and a second part coming from the bundle format stating which format it is and
	 * suited for which application.
	 */
	public void createInfoFile()
	{
		createInfoFile("OpenSeaMap General Charts Bundle 0.1\r\n");
	}

	/**
	 * This combines the specified text with the standard disclaimer. See {@link #createGeneralDisclaimer()}
	 * 
	 * @param strBundleDescription
	 */
	public void createInfoFile(String strBundleDescription)
	{
		File crtba = new File(mOutputDir.getAbsolutePath(), "UserAgreement-OpenSeaMap.txt");
		try
		{
			FileWriter fw = new FileWriter(crtba);
			fw.write(strBundleDescription);
			fw.write(createGeneralDisclaimer());
			fw.close();
		}
		catch (IOException e)
		{
			log.error("", e);
		}
	}

	// Bundle actions
	/**
	 * @see osmcb.program.bundlecreators.IfBundleCreator#initializeBundle()
	 */
	@Override
	public void initializeBundle() throws IOException, BundleTestException
	{
		initializeBundle(null);
	}

	/**
	 * A fallback implementation. At least create a directory where all bundle output is placed. A new directory is created following a general naming scheme.
	 * This may not be appropriate for some output formats. In that case they have to override InitializeBundle()
	 * 
	 * @param bundle
	 * @throws IOException
	 * @throws BundleTestException
	 */
	// public void initializeBundle(String bundleDirName) throws IOException, BundleTestException
	public void initializeBundle(File bundleOutputDir) throws IOException, BundleTestException
	{
		if (bundleOutputDir == null)
		{
			bundleOutputDir = OSMCBSettings.getInstance().getChartBundleOutputDirectory();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-HHmmss");
			String bundleDirName = mBundle.getName() + "-" + sdf.format(new Date());
			bundleOutputDir = new File(bundleOutputDir, bundleDirName);
		}
		mOutputDir = bundleOutputDir;
		OSMCBUtilities.mkDirs(mOutputDir);
		log.trace("bundle='" + mBundle.getName() + "' initialized");
	}

	/**
	 * @see osmcb.program.bundlecreators.IfBundleCreator#createBundle()
	 */
	@Override
	public void createBundle() throws IOException, InterruptedException
	{
		File layerOutputDir = mOutputDir;
		for (IfLayer tLayer : mBundle.getLayers())
		{
			// IfBundleCreator layerCreator = new ACBundleCreator(mBundle, tLayer, layerOutputDir);
			ACBundleCreator layerCreator;
			try
			{
				layerCreator = mBundle.createLayerCreatorInstance();
				layerCreator.init(mBundle, tLayer, layerOutputDir);
				// IfBundleCreator layerCreator = new ACBundleCreator(mBundle, tLayer, layerOutputDir);
				mExec.execute(layerCreator);
				// mExec.execute(this);
				// jobStarted();
			}
			catch (InstantiationException | IllegalAccessException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		log.trace("bundle='" + mBundle.getName() + "' created");
	}

	/**
	 * @see osmcb.program.bundlecreators.IfBundleCreator#finishBundle()
	 */
	@Override
	public void finishBundle()
	{
		createInfoFile();
		sBundleProgress.finishBundle();
		log.trace("bundle='" + mBundle.getName() + "' finished");
	}

	@Override
	public void initializeLayer() throws IOException, InterruptedException
	{
		sBundleProgress.initLayer(mLayer);
		log.trace("layer='" + mLayer.getName() + "' initialized");
	}

	@Override
	public void createLayer() throws IOException, InterruptedException
	{
		File mapOutputDir = mOutputDir;
		for (IfMap tMap : mLayer)
		{
			ACBundleCreator mapCreator;
			try
			{
				mapCreator = mBundle.createMapCreatorInstance();
				mapCreator.init(mBundle, mLayer, tMap, mapOutputDir);
				mExec.execute(mapCreator);
				jobStarted();
			}
			catch (InstantiationException | IllegalAccessException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		log.trace("layer='" + mLayer.getName() + "' created");
	}

	@Override
	public void finishLayer() throws IOException
	{
		sBundleProgress.finishLayer(mLayer);
		log.trace("layer='" + mLayer.getName() + "' finished");
	}

	@Override
	public void initializeMap() throws IOException
	{
		int tileSize = mMap.getMapSource().getMapSpace().getTileSize();
		// xMin = mMap.getMinTileCoordinate().x / tileSize;
		// xMax = mMap.getMaxTileCoordinate().x / tileSize;
		// yMin = mMap.getMinTileCoordinate().y / tileSize;
		// yMax = mMap.getMaxTileCoordinate().y / tileSize;

		Thread t = Thread.currentThread();
		// if (!(t instanceof BundleThread))
		// throw new RuntimeException("Calling thread must be BundleThread!");
		// BundleThread at = (BundleThread) t;
		// mBundleProgress = at.getBundleProgress();
		log.trace("map='" + mMap.getName() + "' initialized");
	}

	/**
	 * This loads the map tiles from the tile store. In any case it retrieves at first the tile from the store.
	 * If there is no tile or the tile in the store is expired, it dispatches a new job to download the tile into the store.
	 * This procedure achieves: 1. The bundle creation does not stall at 'missing' tiles. 2. The tiles wanted are downloaded with priority into the tile store
	 * and will be used next time. 3. We can use a lot of threads to fetch tiles from the store, while not overburdening the download server(s) by limiting the
	 * number of threads which actually access the server.
	 */
	@Override
	public boolean loadMapTiles() throws Exception
	{
		log.debug("start map='" + mMap.getName() + "', TileStore=" + mTS);

		if (Thread.currentThread().isInterrupted())
		{
			log.debug("currentThread().isInterrupted()");
			throw new InterruptedException();
		}

		final int tileCount = (int) mMap.calculateTilesToDownload();

		try
		{
			log.debug("load " + tileCount + " tiles");
			sBundleProgress.initMapDownload(mMap);
			// we download only from online map sources, not from file based map sources
			if (!(mMap.getMapSource() instanceof IfFileBasedMapSource))
			{
				TileLoader tl = new TileLoader(this);

				log.trace("TileLoader instanciated");
				for (int tileX = mMap.getMinTileCoordinate().x / IfMapSpace.TECH_TILESIZE; tileX < mMap.getMaxTileCoordinate().x / IfMapSpace.TECH_TILESIZE; ++tileX)
				{
					for (int tileY = mMap.getMinTileCoordinate().y / IfMapSpace.TECH_TILESIZE; tileY < mMap.getMaxTileCoordinate().y / IfMapSpace.TECH_TILESIZE; ++tileY)
					{
						mExec.execute(tl.createTileLoaderJob(mMap.getMapSource(), tileX, tileY, mMap.getZoom()));
						log.trace("Job started X=" + tileX + ", Y=" + tileY + ", threads=" + mExec.getActiveCount());
					}
				}
			}
		}
		catch (Error e)
		{
			log.error("Error: " + e.getMessage(), e);
			throw e;
		}
		finally
		{
			sBundleProgress.finishMapDownload(mMap);
		}
		return true;
	}

	@Override
	public void tileLoadingFinished(Tile tile, boolean success)
	{
		TileDbEntry tTSE = new TileDbEntry(tile.getXtile(), tile.getYtile(), tile.getZoom(), tile.getImage());
		mTS.putTile(tTSE, tile.getSource());
		log.trace("tile=" + tile + " loaded=" + success);
	}

	@Override
	public MemoryTileCache getTileImageCache()
	{
		log.trace("MemoryTileCache requested");
		return null;
	}

	@Override
	public void createMap() throws MapCreationException, InterruptedException
	{
		// wait for all threads to finish
		log.trace("map='" + mMap.getName() + "' created");
	}

	@Override
	public void finishMap()
	{
		sBundleProgress.finishMap(mMap);
		log.trace("map='" + mMap.getName() + "' finished");
	}

	public AtomicInteger getActiveDownloads()
	{
		return mActiveJobs;
	}

	public IfBundle getBundle()
	{
		return mBundle;
	}

	public boolean waitCreation()
	{
		boolean bOk = false;

		return bOk;
	}
}
