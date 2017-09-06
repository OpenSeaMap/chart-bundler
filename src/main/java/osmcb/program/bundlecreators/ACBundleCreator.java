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

import osmb.mapsources.ACMapSource;
import osmb.mapsources.IfFileBasedMapSource;
import osmb.mapsources.MP2MapSpace;
import osmb.mapsources.TileAddress;
//W #mapSpace import osmb.mapsources.MP2MapSpace;
//W #mapSpace import osmb.mapsources.mapspace.MercatorPower2MapSpace;
import osmb.program.JobDispatcher;
import osmb.program.map.IfLayer;
import osmb.program.map.IfMap;
//W #mapSpaceimport osmb.program.map.IfMapSpace;
//W #mapSpace import osmb.program.map.IfMapSpace.ProjectionCategory;
import osmb.program.tiles.IfTileLoaderListener;
import osmb.program.tiles.MemoryTileCache;
import osmb.program.tiles.Tile;
import osmb.program.tiles.TileImageParameters;
import osmb.program.tiles.TileLoader;
import osmb.program.tilestore.ACTileStore;
import osmb.program.tilestore.TileStoreException;
import osmb.program.tilestore.berkeleydb.TileDbEntry;
import osmb.program.tilestore.sqlitedb.SQLiteDbTileStore;
import osmb.utilities.Charsets;
import osmb.utilities.OSMBStrs;
import osmcb.OSMCBSettings;
import osmcb.OSMCBStrs;
import osmcb.program.bundle.ACBundleProgress;
import osmcb.program.bundle.BundleTestException;
import osmcb.program.bundle.IfBundle;
import osmcb.program.bundle.MapCreationException;
import osmcb.ui.BundleProgress;
import osmcb.utilities.OSMCBUtilities;

/**
 * Abstract base class for all ACBundleCreator implementations.
 * <p>
 * From the view of software structure, this should be three classes: ACBundleCreator, ACLayerCreator, ACMapCreator.
 * But prioritizing convenience in implementing an actual bundle format these three classes are merged into one BundleCreator class.
 * It features three constructors for the three 'flavors' of BundleCreators, which are bundle, layer and map.
 * <p>
 * 
 * The glue, common to all implementations, is implemented in this ACBundleCreator class and should NOT be overridden by implementations.
 * 
 */
public class ACBundleCreator implements Runnable, IfTileLoaderListener
{
	// static/class data
	protected static Logger log = Logger.getLogger(ACBundleCreator.class);

	public static final Charset TEXT_FILE_CHARSET = Charsets.ISO_8859_1;
	/**
	 * This is a default date/time format for naming bundles etc.
	 */
	protected static final String STR_BUFMT = "yyyyMMdd-HHmm";

	protected static MemoryTileCache sTC = new MemoryTileCache();
	protected static ACTileStore sTS = ACTileStore.getInstance();
	protected static SQLiteDbTileStore sNTS = null; // the 'new' SQLite tile store

	private static AtomicInteger sCompletedMaps = new AtomicInteger(0);
	protected static AtomicInteger sScheduledTiles = new AtomicInteger(0);
	protected static AtomicInteger sDownloadedTiles = new AtomicInteger(0);

	protected static ACBundleProgress sBundleProgress = null; // all messages regarding the progress go there

	/**
	 * somehow the {@link ACBundleProgress} displays or logs the creation progress.
	 * 
	 * @param bP
	 */
	public static void setBundleProgress(ACBundleProgress bP)
	{
		sBundleProgress = bP;
	}

	protected static int sTileSize = MP2MapSpace.TECH_TILESIZE;

	// instance data

	private AtomicInteger mActiveJobs = new AtomicInteger(0);
	private AtomicInteger mJobsCompleted = new AtomicInteger(0);
	private AtomicInteger mJobsRetryError = new AtomicInteger(0);
	private AtomicInteger mJobsPermanentError = new AtomicInteger(0);

	// protected ExecutorService mExec = null;
	protected JobDispatcher mExec = null;
	protected IfBundle mBundle = null;
	protected IfLayer mLayer = null;
	protected IfMap mMap = null;

	// protected PauseResumeHandler pauseResumeHandler = null;

	/**
	 * Custom tile processing parameters. <code>null</code> if disabled in GUI
	 */
	protected TileImageParameters parameters = null;
	// protected BundleOutputFormat bundleOutputFormat = null;
	// protected IfTileP0rovider mapDlTileProvider = null;
	protected File mOutputDir = null;
	/**
	 * way out
	 */
	// private boolean aborted = false;
	// private DownloadJobProducerThread djp;
	// private JobDispatcher downloadJobDispatcher;

	/**
	 * necessary for instantiation via newInstance()
	 * Top-level constructor - this is the starting point for the whole bundle. It recursively executes threads for each layer. The number of layers is the
	 * 'natural' limit for the the number of threads here.
	 */
	public ACBundleCreator()
	{
	};

	/**
	 * The thread pools will be created for each creation path in the init method.
	 * This init() is used when initializing a bundle creation.
	 * 
	 * @param bundle
	 */
	public void init(IfBundle bundle, File bundleOutputDir)
	{
		log.trace(OSMBStrs.RStr("START"));
		mBundle = bundle;
		mOutputDir = bundleOutputDir;
		int nThreads = 5;
		if (mBundle.getLayerCount() > 0)
		{
			if (mBundle.getLayerCount() < nThreads)
				nThreads = mBundle.getLayerCount();
			mExec = new JobDispatcher(nThreads);
			// mTileCount = bundle.calculateTilesToLoad();
			// mMapCount = bundle.calcMapsToCompose();
			log.trace("bundle '" + mBundle.getName() + "' pool for layers=" + mExec.getMaximumPoolSize() + ", " + mExec.toString());
		}
		else
			log.warn("bundle '" + mBundle.getName() + "' contains no layers");
	};

	/**
	 * This init() is used when initializing a layer creation.
	 * 
	 * @param bundle
	 * @param layer
	 * @param layerOutputDir
	 */
	public void init(IfBundle bundle, IfLayer layer, File layerOutputDir)
	{
		log.trace(OSMBStrs.RStr("START"));
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
	 * This init() is used when initializing a map creation.
	 * 
	 * @param bundle
	 * @param layer
	 * @param map
	 * @param mapOutputDir
	 */
	public void init(IfBundle bundle, IfLayer layer, IfMap map, File mapOutputDir)
	{
		log.trace(OSMBStrs.RStr("START"));
		mBundle = bundle;
		mLayer = layer;
		mMap = map;
		mOutputDir = mapOutputDir;
		if (sNTS == null)
			try
			{
				sNTS = SQLiteDbTileStore.prepareTileStore(map.getMapSource());
			}
			catch (TileStoreException e)
			{
				log.error("init of " + map + " failed", e);
				e.printStackTrace();
			}

		// limit the number of threads, maybe we will get the limit value from settings in the future
		// we will need some kind of dynamic max thread limit, depending on the number of remaining maps and layers
		int nThreads = 4;
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
		log.trace(OSMBStrs.RStr("START"));
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
		log.trace(OSMBStrs.RStr("START") + " [" + Thread.currentThread().getName() + "], pool=" + mExec.toString());
		log.trace("test of bundle='" + mBundle.getName() + "' successful");

		// actually create a bundle. The tiles will be downloaded when the map requests them.
		try
		{
			boolean bCreate = false;
			log.trace("before BC.initializeBundle()");
			initializeBundle();
			Path pOutDir = mOutputDir.toPath();
			log.info("++++ Bundle name='" + mBundle.getBaseName() + "', dir='" + pOutDir.getParent() + "' +++++");
			TreeSet<DirEntry> tBundles = OSMCBUtilities.listBundles(pOutDir.getParent(), mBundle.getBaseName());
			if (tBundles.size() > 1)
			{
				DirEntry tDE = tBundles.pollFirst();
				tDE = tBundles.pollFirst();
				log.debug(
				    "bundle path='" + tDE.GetPathStr() + "', date=" + tDE.GetDateStr() + ", catalog date=" + Files.getLastModifiedTime(mBundle.getFile().toPath()));
				if (tDE.GetDate().compareTo(Files.getLastModifiedTime(mBundle.getFile().toPath())) < 0)
				{
					bCreate = true;
					log.info("create bundle name='" + mBundle.getBaseName() + "', date=" + tDE.GetDateStr() + ", catalog date="
					    + Files.getLastModifiedTime(mBundle.getFile().toPath()));
				}
				else
				{
					long nMillis = new Date().getTime() - OSMCBSettings.getInstance().getBundleUpdateDays() * 86400 * 1000;
					FileTime tTest = FileTime.fromMillis(nMillis);
					if (tTest.compareTo(tDE.GetDate()) < 0)
					{
						bCreate = true;
						log.info("create bundle name='" + mBundle.getBaseName() + "', date=" + tDE.GetDateStr() + ", test date=" + tTest);
					}
				}
			}
			else
				bCreate = true;
			if (bCreate)
			{
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
				createGeoJson();
				jobFinishedSuccessfully(0);
				log.debug("bundle '" + mBundle.getName() + "' finished");
			}
			else
				log.info("bundle '" + mBundle.getName() + "' skipped");
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
		log.trace(OSMBStrs.RStr("START") + " [" + Thread.currentThread().getName() + "], pool=" + mExec.toString());
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
			log.info("layer='" + mLayer.getName() + "' finished");
			// log.info("layer '" + mLayer.getName() + "' of " + mBundle.getLayers() + " finished");
		}
		catch (IOException | InterruptedException e)
		{
			e.printStackTrace();
			jobFinishedWithError(false);
		}
	}

	/**
	 * Is called for each map. No tests are performed because the bundle is already declared for being ok.
	 * It actually creates the layer in a three step process.
	 * - initializeMap(); creates the necessary directories etc...
	 * - downloadMapTiles(); downloads all tiles which are not yet in the tile store available.
	 * - createMap(); actually build a map (usually one single file, but some bundle formats handle that different) from the tiles.
	 * - finishMap(); do necessary clean up and packaging.
	 */
	protected void runMap()
	{
		log.trace(OSMBStrs.RStr("START") + " [" + Thread.currentThread().getName() + "], pool=" + mExec.toString());
		try
		{
			if (sNTS == null)
				log.error("no tile store init yet");
			sNTS.prepareTileStore(mMap.getMapSource());
			// mMap.getMapSource().initialize();
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
			// create the map from all downloaded tiles
			createMap();
			// wait for the map creation to finish
			finishMap();
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

	// public void jobStarted()
	// {
	// sBundleProgress.setJobs(mActiveJobs.incrementAndGet());
	// }
	//
	public void jobFinishedSuccessfully(int bytesDownloaded)
	{
		log.trace(OSMBStrs.RStr("START"));
		mActiveJobs.decrementAndGet();
		mJobsCompleted.incrementAndGet();
	}

	public void jobFinishedWithError(boolean retry)
	{
		log.trace(OSMBStrs.RStr("START"));
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
		log.trace(OSMBStrs.RStr("START"));
		mExec.shutdown();
	}

	/**
	 *
	 */
	final public void abortBundleCreation()
	{
		log.trace(OSMBStrs.RStr("START"));
		try
		{
			shutdown();
		}
		catch (Exception e)
		{
			log.error("Exception thrown in stopDownload()" + e.getMessage());
		}
		// this.aborted = true;
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
		log.trace(OSMBStrs.RStr("START"));
		try
		{
			if (mBundle.getLayerCount() < 1)
				throw new BundleTestException("The bundle '" + mBundle.getName() + "' contains no layers");
			for (IfLayer layer : mBundle)
			{
				for (IfMap map : layer)
				{
					if (!testMapSource(map.getMapSource()))
						throw new BundleTestException(
						    "The selected bundle output format \"" + mBundle.getOutputFormat() + "\" does not support the map source \"" + map.getMapSource() + "\"");
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
	 * Test if the {@link ACBundleCreator} instance supports the selected {@link IfMapSource}. This test has to be performed and implemented by the creator
	 * instance.
	 * 
	 * @param mapSource
	 * @return <code>true</code> if supported otherwise <code>false</code>
	 * @see AtlasCreator
	 */
	protected boolean testMapSource(ACMapSource mapSource)
	{
		// W #mapSpace ???
		return true; // TODO ???
		// IfMapSpace mapSpace = mapSource.getMapSpace();
		// return (mapSpace instanceof MercatorPower2MapSpace && ProjectionCategory.SPHERE.equals(mapSpace.getProjectionCategory()));
	}

	/**
	 * This creates a standard disclaimer. In the future this disclaimer should come from some file, so that it can be updated without a revision of the code
	 * 
	 * @return
	 */
	public String createGeneralDisclaimer()
	{
		log.trace(OSMBStrs.RStr("START"));
		String strDisclaimer = "";
		strDisclaimer += "This Charts bundle is useable for testing ONLY, it is in no way fit for navigational purposes.\r\n";
		strDisclaimer += "\r\n";
		strDisclaimer += "OpenSeaMap does not give any warranty, that the data shown in this map are current or correct.\r\n";
		strDisclaimer += "Even if you use it for testing, any damage resulting from this test will be solely your responsibility.\r\n";

		strDisclaimer += "By using this chart you acknowledge that you have read, understood\r\n";
		strDisclaimer += "and accepted the terms and conditions stated in the User Agreement:\r\n";
		strDisclaimer += "http://www.openseamap.org/legal/disclaimer.html\r\n";
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
		log.trace(OSMBStrs.RStr("START"));
		createInfoFile("OpenSeaMap General Charts Bundle 0.1\r\n");
	}

	/**
	 * This combines the specified text with the standard disclaimer. See {@link #createGeneralDisclaimer()}
	 * 
	 * @param strBundleDescription
	 */
	public void createInfoFile(String strBundleDescription)
	{
		log.trace(OSMBStrs.RStr("START"));
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

	/**
	 * This creates a GeoJson containing the complete bundle
	 * 
	 * supposed contents (as of 2017-08):
	 * {
	 * "type": "Feature",
	 * "bbox": [-11.25000000, 48.92249926, 22.50000000, 66.51326044],
	 * "geometry": {
	 * "type": "GeometryCollection",
	 * "geometries": [{
	 * "type": "Polygon",
	 * "coordinates": [
	 * [
	 * [-11.25, 48.92249926],
	 * [22.5, 48.92249926],
	 * [22.5, 66.51326044],
	 * [-11.25, 66.51326044],
	 * [-11.25, 48.92249926]
	 * ]
	 * ]
	 * }, {
	 * "type": "Polygon",
	 * "coordinates": [
	 * [
	 * [2.81250000, 50.73645514],
	 * [5.62500000, 50.73645514],
	 * [5.62500000, 62.91523304],
	 * [2.81250000, 62.91523304],
	 * [2.81250000, 50.73645514]
	 * ]
	 * ]
	 * }]
	 * },
	 * "properties": {
	 * "kind": "bundle",
	 * "name": "NorthSea",
	 * "date": "20170122-0847"
	 * }
	 * }
	 * 
	 */
	protected void createGeoJson()
	{
		log.trace(OSMBStrs.RStr("START"));
		String strDatePart = new SimpleDateFormat(STR_BUFMT).format(new Date());
		File crtba = new File(mOutputDir.getAbsolutePath(), mBundle.getBaseName() + "-" + strDatePart + ".json");
		try
		{
			FileWriter fw = new FileWriter(crtba);
			JsonGenerator tJGen = Json.createGenerator(fw);
			tJGen.writeStartObject();
			tJGen.write("type", "Feature");
			// the bundles bounding box
			IfLayer topLayer = mBundle.getLayer(0);
			tJGen.writeStartArray("bbox").write(topLayer.getMinLon()).write(topLayer.getMinLat()).write(topLayer.getMaxLon()).write(topLayer.getMaxLat()).writeEnd();
			tJGen.writeStartObject("geometry").write("type", "GeometryCollection");
			tJGen.writeStartArray("geometries");
			for (IfLayer layer : mBundle)
			{
				for (IfMap map : layer)
				{
					// mapwise
					tJGen.writeStartObject().write("type", "Polygon");
					tJGen.writeStartArray("coordinates");
					tJGen.writeStartArray();
					tJGen.writeStartArray();
					tJGen.writeStartArray().write(map.getMinLon()).write(map.getMinLat()).writeEnd();
					tJGen.writeStartArray().write(map.getMaxLon()).write(map.getMinLat()).writeEnd();
					tJGen.writeStartArray().write(map.getMaxLon()).write(map.getMaxLat()).writeEnd();
					tJGen.writeStartArray().write(map.getMinLon()).write(map.getMaxLat()).writeEnd();
					tJGen.writeStartArray().write(map.getMinLon()).write(map.getMinLat()).writeEnd();
					tJGen.writeEnd();
					tJGen.writeEnd();
					tJGen.writeEnd();
					tJGen.writeEnd();
				}
			}
			tJGen.writeEnd();
			tJGen.writeEnd();
			// the bundles data - synchronized with the dir and file names
			tJGen.writeStartObject("properties").write("kind", "bundle").write("name", mBundle.getName()).write("date", strDatePart).writeEnd();
			tJGen.writeEnd().close();
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
	public void initializeBundle() throws IOException, BundleTestException
	{
		log.trace(OSMBStrs.RStr("START"));
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
		log.trace(OSMBStrs.RStr("START"));
		if (bundleOutputDir == null)
		{
			bundleOutputDir = OSMCBSettings.getInstance().getChartBundleOutputDirectory();
			SimpleDateFormat sdf = new SimpleDateFormat(STR_BUFMT);
			String bundleDirName = mBundle.getName() + "-" + sdf.format(new Date());
			bundleOutputDir = new File(bundleOutputDir, bundleDirName);
		}
		mOutputDir = bundleOutputDir;
		log.trace("bundle='" + mBundle.getName() + "' initialized");
	}

	/**
	 * 
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void createBundle() throws IOException, InterruptedException
	{
		log.trace(OSMBStrs.RStr("START"));
		File layerOutputDir = mOutputDir;
		OSMCBUtilities.mkDirs(mOutputDir);
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
	public void finishBundle()
	{
		log.trace(OSMBStrs.RStr("START"));
		createInfoFile();
		sBundleProgress.finishBundle();
		log.info("bundle='" + mBundle.getName() + "' finished");
	}

	public void initializeLayer() throws IOException, InterruptedException
	{
		log.trace(OSMBStrs.RStr("START"));
		sBundleProgress.initLayer(mLayer);
		log.trace("layer='" + mLayer.getName() + "' initialized");
	}

	public void createLayer() throws IOException, InterruptedException
	{
		log.trace(OSMBStrs.RStr("START"));
		File mapOutputDir = mOutputDir;
		for (IfMap tMap : mLayer)
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
		log.trace("layer='" + mLayer.getName() + "' created");
	}

	public void finishLayer() throws IOException
	{
		log.trace(OSMBStrs.RStr("START"));
		sBundleProgress.finishLayer(mLayer);
		log.info("layer='" + mLayer.getName() + "' finished");
		// mLayer = null;
	}

	public void initializeMap() throws IOException
	{
		log.trace(OSMBStrs.RStr("START"));
		// int tileSize = MP2MapSpace.getTileSize(); // #mapSpace mMap.getMapSource().getMapSpace().getTileSize();
		// xMin = mMap.getMinPixelCoordinate().x / tileSize;
		// xMax = mMap.getMaxPixelCoordinate().x / tileSize;
		// yMin = mMap.getMinPixelCoordinate().y / tileSize;
		// yMax = mMap.getMaxPixelCoordinate().y / tileSize;

		// Thread t = Thread.currentThread();
		// if (!(t instanceof BundleThread))
		// throw new RuntimeException("Calling thread must be BundleThread!");
		// BundleThread at = (BundleThread) t;
		// mBundleProgress = at.getBundleProgress();
		log.trace("map='" + mMap.getName() + "' initialized");
	}

	/**
	 * This loads the map tiles from the tile store. In any case it first retrieves the tile from the store.
	 * If there is no tile or the tile in the store is expired, it dispatches a new job to download the tile into the store.
	 * This procedure achieves:
	 * 1. The bundle creation does not stall at 'missing' tiles.
	 * 2. The tiles wanted are downloaded with priority into the tile store and will be used next time.
	 * 3. We can use a lot of threads to fetch tiles from the store, while not overburdening the download server(s) by limiting the
	 * number of threads which actually access the server.
	 */
	public boolean loadMapTiles() throws Exception
	{
		log.trace(OSMBStrs.RStr("START"));
		log.debug("start map=" + mMap.getName() + ", TileStore=" + sTS);

		if (Thread.currentThread().isInterrupted())
		{
			log.debug("currentThread().isInterrupted()");
			throw new InterruptedException();
		}

		final int tileCount = (int) mMap.calculateTilesToLoad();

		try
		{
			log.debug("download tiles=" + tileCount);
			sBundleProgress.initMapDownload(mMap);
			// we download only from online map sources, not from file based map sources
			if (!(mMap.getMapSource() instanceof IfFileBasedMapSource))
			{
				TileLoader tl = new TileLoader(this, sTC);

				log.trace("TileLoader instanciated");
				for (int tileX = mMap.getMinTileCoordinate().x; tileX <= mMap.getMaxTileCoordinate().x; ++tileX)
				{
					for (int tileY = mMap.getMinTileCoordinate().y; tileY <= mMap.getMaxTileCoordinate().y; ++tileY)
					{
						log.debug("tiles=" + sScheduledTiles.incrementAndGet() + " of " + mBundle.calculateTilesToLoad());
						mExec.execute(tl.createTileLoaderJob(mMap.getMapSource(), new TileAddress(tileX, tileY, mMap.getZoom())));
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

	public void createMap() throws MapCreationException, InterruptedException
	{
		log.trace(OSMBStrs.RStr("START"));
		// wait for all threads to finish
		log.trace("map='" + mMap.getName() + "' created");
	}

	public void finishMap()
	{
		log.trace(OSMBStrs.RStr("START"));
		sBundleProgress.finishMap(mMap);
		log.info("map '" + mMap.getName() + "', " + sCompletedMaps.incrementAndGet() + " of " + mBundle.calcMapsToCompose() + " finished, tiles="
		    + sDownloadedTiles.incrementAndGet() + " of " + mBundle.calculateTilesToLoad());
		mMap = null;
	}

	public IfBundle getBundle()
	{
		return mBundle;
	}

	@Override
	public void tileLoadingFinished(Tile tile, boolean success)
	{
		log.trace(OSMBStrs.RStr("START"));
		TileDbEntry tTSE = new TileDbEntry(tile.getXtile(), tile.getYtile(), tile.getZoom(), tile.getImage());
		sTS.putTile(tTSE, tile.getSource());
		sTC.addTile(tile);
		int nTiles = sDownloadedTiles.incrementAndGet();
		log.debug("tiles=" + nTiles + " of " + mBundle.calculateTilesToLoad());
		// info at 0.5% steps
		if (nTiles % (mBundle.calculateTilesToLoad() / 200.0) == 0)
			log.info("tiles=" + nTiles + " of " + mBundle.calculateTilesToLoad() + ", " + nTiles / (mBundle.calculateTilesToLoad() / 200.0) * 0.5 + "%");
		sBundleProgress.finishTileDownload(tile);
		if (!success)
			log.debug("tile=" + tile + " loaded=" + success);
		else
			log.trace("tile=" + tile + " loaded=" + success);
	}

	@Override
	public void tileDownloaded(Tile tile, int size)
	{
		log.info(tile + " loaded from online map source, size=" + size);
	}

	@Override
	public void tileLoadedFromCache(Tile tile, int size)
	{
		log.info(tile + " loaded from mtc, size=" + size);
	}

	public static MemoryTileCache getTileImageCache()
	{
		return sTC;
	}
}
