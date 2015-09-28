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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import osmb.mapsources.IfFileBasedMapSource;
import osmb.mapsources.IfMapSource;
import osmb.mapsources.IfMapSource.LoadMethod;
import osmb.mapsources.mapspace.MercatorPower2MapSpace;
import osmb.program.map.IfLayer;
import osmb.program.map.IfMap;
import osmb.program.map.IfMapSpace;
import osmb.program.map.IfMapSpace.ProjectionCategory;
import osmb.program.tiles.TileImageParameters;
import osmb.utilities.Charsets;
import osmcb.OSMCBSettings;
import osmcb.OSMCBStrs;
import osmcb.program.DirectoryManager;
import osmcb.program.bundle.ACBundleProgress;
import osmcb.program.bundle.BundleOutputFormat;
import osmcb.program.bundle.BundleTestException;
import osmcb.program.bundle.IfBundle;
import osmcb.program.bundle.MapCreationException;
import osmcb.program.bundlecreators.tileprovider.DownloadedTileProvider;
import osmcb.program.bundlecreators.tileprovider.FilteredMapSourceProvider;
import osmcb.program.bundlecreators.tileprovider.TileProvider;
import osmcb.utilities.OSMCBUtilities;
import osmcb.utilities.tar.TarIndex;
import osmcb.utilities.tar.TarIndexedArchive;

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
public class ACBundleCreator implements Runnable, IfBundleCreator
{
	public static final Charset TEXT_FILE_CHARSET = Charsets.ISO_8859_1;
	protected static ExecutorService mExec;

	protected static ACBundleProgress sBundleProgress = null; // all messages regarding the progress go there
	private static AtomicInteger sActiveJobs = new AtomicInteger(0);
	private static AtomicInteger sJobsCompleted = new AtomicInteger(0);
	private static AtomicInteger sJobsRetryError = new AtomicInteger(0);
	private static AtomicInteger sJobsPermanentError = new AtomicInteger(0);

	protected final Logger log;

	protected IfBundle mBundle = null;
	protected File mBundleDir; // base directory for all output of the bundle
	protected IfLayer mLayer = null;
	protected File mLayerDir; // base directory for all output of the layer
	protected IfMap mMap = null;
	protected File mMapDir; // base directory for all output of the map

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
	protected BundleOutputFormat bundleOutputFormat;
	protected TileProvider mapDlTileProvider;
	protected File mOutputDir = null;
	/**
	 * way out
	 */
	private boolean aborted = false;
	// private DownloadJobProducerThread djp;
	// private JobDispatcher downloadJobDispatcher;

	/**
	 * necessary for instantiation via newInstance()
	 */
	public ACBundleCreator()
	{
		log = Logger.getLogger(this.getClass());
	};

	/**
	 * Top-level constructor - this is the starting point for the whole bundle. It recursively executes threads for each layer. The number of layers is 'natural'
	 * limit for the the number of threads here.
	 */
	public ACBundleCreator(IfBundle bundle, File bundleOutputDir)
	{
		log = Logger.getLogger(this.getClass());
		mBundle = bundle;
		mOutputDir = bundleOutputDir;
		// mExec = Executors.newFixedThreadPool(mBundle.getLayerCount());
		log.debug("bundle pool threads=" + mBundle.getLayerCount());
	};

	public void init(IfBundle bundle)
	{
		mBundle = bundle;
		mOutputDir = null;
		mExec = Executors.newFixedThreadPool(mBundle.getLayerCount());
		log.debug("bundle '" + mBundle.getName() + "' pool for layers=" + mBundle.getLayerCount());
	};

	/**
	 * Layer-level constructor - this creates one layer and therefore executes threads for each map in this layer. The number of concurrent threads is limited.
	 */
	protected ACBundleCreator(IfBundle bundle, IfLayer layer, File layerOutputDir)
	{
		log = Logger.getLogger(this.getClass());
		mBundle = bundle;
		mLayer = layer;
		mOutputDir = layerOutputDir;
		// limit the number of threads, maybe we will get the limit value from settings in the future
		// mExec = Executors.newFixedThreadPool(Math.min(50, mLayer.getMapCount()));
		log.debug("layer '" + mLayer.getName() + "' pool for maps=" + mLayer.getMapCount());
	};

	/**
	 * Map-level constructor - this creates on map. If first downloads all tiles covered in this map and after successful download creates the map.
	 * To download each tile a separate thread is executed. The number of concurrent threads is limited.
	 */
	public ACBundleCreator(IfBundle bundle, IfLayer layer, IfMap map, File mapOutputDir)
	{
		log = Logger.getLogger(this.getClass());
		mBundle = bundle;
		mLayer = layer;
		mMap = map;
		mOutputDir = mapOutputDir;
		// limit the number of threads, maybe we will get the limit value from settings in the future
		// explicit limiting because getTileCount is a long
		int nThreads = 50;
		if (mMap.getTileCount() < nThreads)
			nThreads = (int) mMap.getTileCount();
		// mExec = Executors.newFixedThreadPool(nThreads);
		log.debug("map pool threads=" + nThreads);
	};

	/**
	 * * @see {@link osmcb.program.bundlecreators.IfBundleCreator#run()}
	 */
	@Override
	public void run()
	{
		log.debug(OSMCBStrs.RStr("BundleThread.CB.RunCalled"));
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
		// log.trace("creation of bundle='" + mBundle.getName() + "' started with some tests");
		log.debug("BundleThread.CB.ModeUnknown", mBundle.getName());
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
				Thread.sleep(1000);
				log.debug("running jobs=" + sActiveJobs);
			}
			finishBundle();
			jobFinishedSuccessfully(0);
			log.debug("after BC.initializeBundle()");
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
		try
		{
			initializeLayer();
			createLayer();
			// wait for the layer creation to finish
			// while (sActiveJobs.get() > 0)
			// wait(1000);
			mExec.shutdown();
			while (!mExec.isTerminated())
			{
				Thread.sleep(1000);
				log.debug("running jobs=" + sActiveJobs);
			}
			finishLayer();
			jobFinishedSuccessfully(0);
		}
		catch (IOException | InterruptedException e)
		{
			e.printStackTrace();
			jobFinishedWithError(false);
		}
	}

	/**
	 * Is called for each map. No tests are performed since the bundle is already declared as ok.
	 * It actually creates the layer in a three step process.
	 * - initializeMap(); creates the necessary directories etc...
	 * - downloadMapTiles(); downloads all tiles which are not yet in the tile store available.
	 * - createMap(); actually build a map (usually one file, but some formats handle that different) from the tiles.
	 * - finishMap();
	 */
	protected void runMap()
	{
		try
		{
			initializeMap();
			// download all necessary tiles. They should go directly into the tile store...
			downloadMapTiles();
			// wait here for the download of all tiles to finish
			// while (sActiveJobs.get() > 0)
			// wait(1000);
			createMap();
			// wait for the map creation to finish
			finishMap();
			jobFinishedSuccessfully(0);
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
		sBundleProgress.setJobs(sActiveJobs.incrementAndGet());
	}

	public void jobFinishedSuccessfully(int bytesDownloaded)
	{
		sActiveJobs.decrementAndGet();
		sJobsCompleted.incrementAndGet();
	}

	public void jobFinishedWithError(boolean retry)
	{
		sActiveJobs.decrementAndGet();
		if (retry)
			sJobsRetryError.incrementAndGet();
		else
		{
			sJobsPermanentError.incrementAndGet();
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
			// DownloadJobProducerThread djp_ = djp;
			// if (djp_ != null)
			// djp_.cancel();
			// if (downloadJobDispatcher != null)
			// downloadJobDispatcher.terminateAllWorkerThreads();
			// // pauseResumeHandler.resume();
			// // this.interrupt();
			abort();
		}
		catch (Exception e)
		{
			log.error("Exception thrown in stopDownload()" + e.getMessage());
		}
		this.aborted = true;
		log.trace("bundle='" + mBundle.getName() + "' aborted");
	}

	protected void abort()
	{

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
		File crtba = new File(mBundleDir.getAbsolutePath(), "UserAgreement-OpenSeaMap.txt");
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
		mBundleDir = bundleOutputDir;
		OSMCBUtilities.mkDirs(mBundleDir);
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
			// mExec.execute(layerCreator);
			mExec.execute(this);
			jobStarted();
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
			IfBundleCreator mapCreator = new ACBundleCreator(mBundle, mLayer, tMap, mapOutputDir);
			mExec.execute(mapCreator);
			jobStarted();
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
	 * 
	 * @param map
	 * @return true if map creation process was finished and false if something
	 *         went wrong and the user decided to retry map download
	 * @throws Exception
	 */
	@Override
	public boolean downloadMapTiles() throws Exception
	{
		log.trace("start");
		TarIndex tileIndex = null;
		TarIndexedArchive tileArchive = null;

		sJobsCompleted.set(0);
		sJobsRetryError.set(0);
		sJobsPermanentError.set(0);

		jobStarted();
		// if (currentThread().isInterrupted()) {
		// log.trace("currentThread().isInterrupted()");
		// throw new InterruptedException();
		// }

		// Prepare the tile store directory
		// ts.prepareTileStore(iMap.getMapSource());

		// In this section of code below, tiles for the map are being downloaded and saved in the temporary layer tar file in the system temp directory.
		int zoom = mMap.getZoom();

		final int tileCount = (int) mMap.calculateTilesToDownload();

		try
		{
			sBundleProgress.initMapDownload(mMap);
			tileArchive = null;
			TileProvider mapTileProvider;
			if (!(mMap.getMapSource() instanceof IfFileBasedMapSource))
			{
				// For online maps we download the tiles first and then start creating the map if we are sure we got all tiles
				if (!BundleOutputFormat.TILESTORE.equals(mBundle.getOutputFormat()))
				{
					// mExec.execute(arg0);
					String tempSuffix = "OSMCB_" + mBundle.getName() + "_" + zoom + "_";
					File tileArchiveFile = File.createTempFile(tempSuffix, ".tar", DirectoryManager.tempDir);
					// If something goes wrong the temp file only persists until
					// the VM exits
					tileArchiveFile.deleteOnExit();
					log.trace("Writing downloaded tiles to " + tileArchiveFile.getPath());
					tileArchive = new TarIndexedArchive(tileArchiveFile, tileCount);
				}
				else
					log.debug("Downloading to tile store only");

				// log.trace("before DownloadJobProducerThread()");
				// djp = new DownloadJobProducerThread(this, downloadJobDispatcher, tileArchive, mMap);
				// log.trace("after DownloadJobProducerThread()");
				//
				// while (djp.isAlive() || (downloadJobDispatcher.getWaitingJobCount() > 0) || downloadJobDispatcher.isAtLeastOneWorkerActive())
				// {
				// Thread.sleep(1000);
				// if (jobsRetryError > 50)
				// {
				// // No user available: simply write a message and
				// // continue.
				// log.error("jobs waiting=" + downloadJobDispatcher.getWaitingJobCount() + " errors=" + jobsRetryError);
				// }
				// }
				// djp = null;
				// log.debug("All download jobs have been completed!");
				// if (tileArchive != null)
				// {
				// tileArchive.writeEndofArchive();
				// tileArchive.close();
				// tileIndex = tileArchive.getTarIndex();
				// if (tileIndex.size() < tileCount)
				// {
				// int missing = tileCount - tileIndex.size();
				// log.debug("Expected tile count: " + tileCount + " downloaded tile count: " + tileIndex.size() + " missing: " + missing);
				// }
				// }
				// downloadJobDispatcher.cancelOutstandingJobs();
				log.debug("Starting to create map from downloaded tiles");
				mapTileProvider = new DownloadedTileProvider(tileIndex, mMap);
			}
			else
			{
				// We don't need to download anything. Everything is already stored locally therefore we just use it
				mapTileProvider = new FilteredMapSourceProvider(mMap, LoadMethod.DEFAULT);
			}
		}
		catch (Error e)
		{
			log.error("Error: " + e.getMessage(), e);
			throw e;
		}
		finally
		{
			if (tileIndex != null)
				tileIndex.closeAndDelete();
			else if (tileArchive != null)
				tileArchive.delete();
		}
		return true;
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
		return sActiveJobs;
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
