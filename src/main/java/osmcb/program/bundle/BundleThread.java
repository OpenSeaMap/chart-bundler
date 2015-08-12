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

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import osmb.mapsources.IfFileBasedMapSource;
import osmb.mapsources.IfMapSource;
import osmb.mapsources.IfMapSource.LoadMethod;
import osmb.program.map.IfLayer;
import osmb.program.map.IfMap;
import osmb.program.tilestore.ACSiTileStore;
import osmb.utilities.GUIExceptionHandler;
import osmcb.OSMCBApp;
import osmcb.OSMCBSettings;
import osmcb.OSMCBStrs;
import osmcb.program.DirectoryManager;
import osmcb.program.DownloadJobProducerThread;
import osmcb.program.JobDispatcher;
import osmcb.program.PauseResumeHandler;
import osmcb.program.bundlecreators.ACBundleCreator;
import osmcb.program.bundlecreators.tileprovider.DownloadedTileProvider;
import osmcb.program.bundlecreators.tileprovider.FilteredMapSourceProvider;
import osmcb.program.bundlecreators.tileprovider.TileProvider;
import osmcb.program.download.IfDownloadJobListener;
import osmcb.ui.BundleProgress;
import osmcb.utilities.tar.TarIndex;
import osmcb.utilities.tar.TarIndexedArchive;

//public class BundleThread extends Thread implements IfBundleThread, IfDownloadJobListener, BundleCreationController
public class BundleThread extends Thread implements IfBundleThread, IfDownloadJobListener
{
	private static final Logger log = Logger.getLogger(BundleThread.class);
	private static int threadNum = 0;
	private File customBundleDir = null;
	private DownloadJobProducerThread djp = null;
	private JobDispatcher downloadJobDispatcher = null;
	private IfBundle bundle = null;
	private ACBundleCreator bundleCreator = null;
	private PauseResumeHandler pauseResumeHandler = null;
	private int activeDownloads = 0;
	private int jobsCompleted = 0;
	private int jobsRetryError = 0;
	private int jobsPermanentError = 0;
	private int maxDownloadRetries = 1;
	private BundleProgress bProgress = null;

	private synchronized static int getNextThreadNum()
	{
		threadNum++;
		return threadNum;
	}

	public BundleThread(IfBundle bundle) throws BundleTestException
	{
		this(bundle, bundle.getOutputFormat().createBundleCreatorInstance());
	}

	public BundleThread(IfBundle bundle, ACBundleCreator bundleCreator) throws BundleTestException
	{
		super("BundleThread " + getNextThreadNum());
		this.bundle = bundle;
		this.bundleCreator = bundleCreator;
		testBundle();
		ACSiTileStore.getInstance().closeAll();
		maxDownloadRetries = OSMCBSettings.getInstance().getDownloadRetryCount();
		pauseResumeHandler = new PauseResumeHandler();
	}

	@Override
	public void testBundle() throws BundleTestException
	{
		try
		{
			for (IfLayer layer : bundle)
			{
				for (IfMap map : layer)
				{
					IfMapSource mapSource = map.getMapSource();
					if (!bundleCreator.testMapSource(mapSource))
						throw new BundleTestException("The selected bundle output format \"" + bundle.getOutputFormat() + "\" does not support the map source \""
								+ map.getMapSource() + "\"");
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
	}

	@Override
	public void run()
	{
		GUIExceptionHandler.registerForCurrentThread();
		if (bProgress == null)
			bProgress = new BundleProgress(this);
		bProgress.initBundle(bundle);
		bundleCreator.setBundleProgress(bProgress);
		log.info("Starting creation of '" + bundle.getOutputFormat() + "' bundle '" + bundle.getName() + "'");
		if (customBundleDir != null)
			log.debug("Target directory: " + customBundleDir);
		bProgress.setDownloadControlerListener(this);
		try
		{
			log.trace("before bundle creation");
			composeBundle();
			log.debug("Bundle creation finished");
			Thread.sleep(5000);
		}
		catch (OutOfMemoryError e)
		{
			log.error("Out of memory: ", e);
		}
		catch (InterruptedException e)
		{
			log.info("Bundle creation was interrupted by user");
		}
		catch (Exception e)
		{
			log.error("Bundle creation aborted because of an error: ", e);
			GUIExceptionHandler.showExceptionDialog(e);
		}
		System.gc();
	}

	/**
	 * Compose bundle: For each map download the tiles and perform bundle/map creation
	 */
	@Override
	public void composeBundle() throws InterruptedException, IOException
	{
		long totalNrOfOnlineTiles = bundle.calculateTilesToDownload();

		log.debug("composeBundle(): started ");

		for (IfLayer l : bundle)
		{
			log.debug("composeBundle(): layer=" + l.getName() + " zoom=" + l.getZoomLvl());
			for (IfMap m : l)
			{
				// Offline map sources are not relevant for the maximum tile limit.
				if (m.getMapSource() instanceof IfFileBasedMapSource)
					totalNrOfOnlineTiles -= m.calculateTilesToDownload();
			}
		}

		log.info("composeBundle(): tiles=" + totalNrOfOnlineTiles);

		if (totalNrOfOnlineTiles > 500000)
		{
			// NumberFormat f = DecimalFormat.getInstance();
			log.debug("composeBundle(): aborted due to too much tiles to download");
			return;
		}
		try
		{
			log.trace("composeBundle(): before BC.initializeBundle()");
			bundleCreator.initializeBundle(bundle, customBundleDir);
			log.debug("composeBundle(): after BC.initializeBundle()");
		}
		catch (BundleTestException e)
		{
			log.error(OSMCBStrs.RStr("BundleThread.CB.FormatInvalid") + e.getMessage());
			return;
		}

		log.trace("composeBundle(): before OSMCBSettings");
		OSMCBSettings s = (OSMCBSettings) OSMCBApp.getApp().getSettings();

		log.trace("composeBundle(): before JobDispatcher(), bProgress=" + bProgress);
		downloadJobDispatcher = new JobDispatcher(s.getDownloadThreadCount(), pauseResumeHandler, bProgress);
		try
		{
			log.trace("composeBundle(): jobDispatcher running");
			for (IfLayer layer : bundle)
			{
				log.trace("composeBundle(): before BC.initializelayer()");
				bundleCreator.initializeLayer(layer);
				for (IfMap map : layer)
				{
					try
					{
						while (!doMapCreation(map))
							;
					}
					catch (InterruptedException e)
					{
						throw e; // User has aborted
					}
					catch (MapDownloadSkippedException e)
					{
						log.trace(OSMCBStrs.RStr("BundleThread.CB.Continue"));
						// Do nothing and continue with next map
					}
					catch (Exception e)
					{
						log.error("", e);
					}
				}
				bundleCreator.finishLayer(layer);
				log.trace(OSMCBStrs.RStr("BundleThread.CB.LayerFinished"));
			}
		}
		catch (InterruptedException e)
		{
			bundleCreator.abortBundleCreation();
			throw e;
		}
		catch (Error e)
		{
			bundleCreator.abortBundleCreation();
			throw e;
		}
		finally
		{
			// In case of an abort: Stop create new download jobs
			if (djp != null)
				djp.cancel();
			downloadJobDispatcher.terminateAllWorkerThreads();
			if (!bundleCreator.isAborted())
				bundleCreator.finishBundle(bundle);
		}
	}

	/**
	 * 
	 * @param map
	 * @return true if map creation process was finished and false if something went wrong and the user decided to retry map download
	 * @throws Exception
	 */
	public boolean doMapCreation(IfMap map) throws Exception
	{
		log.trace("doMapCreation(): start");
		TarIndex tileIndex = null;
		TarIndexedArchive tileArchive = null;

		// jobsCompleted = 0;
		jobsRetryError = 0;
		jobsPermanentError = 0;

		if (currentThread().isInterrupted())
		{
			log.trace("currentThread().isInterrupted()");
			throw new InterruptedException();
		}

		// Prepare the tile store directory
		// ts.prepareTileStore(iMap.getMapSource());

		/***
		 * In this section of code below, tiles for Bundle are being downloaded and saved in the temporary layer tar file in the system temp directory.
		 **/
		int zoom = map.getZoom();

		final int tileCount = (int) map.calculateTilesToDownload();

		try
		{
			bProgress.initMapCreation(tileCount);
			tileArchive = null;
			TileProvider mapTileProvider;
			if (!(map.getMapSource() instanceof IfFileBasedMapSource))
			{
				// For online maps we download the tiles first and then start creating the map if we are sure we got all tiles
				if (!BundleOutputFormat.TILESTORE.equals(bundle.getOutputFormat()))
				{
					String tempSuffix = "OSMCB_" + bundle.getName() + "_" + zoom + "_";
					File tileArchiveFile = File.createTempFile(tempSuffix, ".tar", DirectoryManager.tempDir);
					// If something goes wrong the temp file only persists until the VM exits
					tileArchiveFile.deleteOnExit();
					log.trace("Writing downloaded tiles to " + tileArchiveFile.getPath());
					tileArchive = new TarIndexedArchive(tileArchiveFile, tileCount);
				}
				else
					log.debug("Downloading to tile store only");

				log.trace("before DownloadJobProducerThread()");
				djp = new DownloadJobProducerThread(this, downloadJobDispatcher, tileArchive, map);
				log.trace("after DownloadJobProducerThread()");

				while (djp.isAlive() || (downloadJobDispatcher.getWaitingJobCount() > 0) || downloadJobDispatcher.isAtLeastOneWorkerActive())
				{
					Thread.sleep(1000);
					if (jobsRetryError > 50)
					{
						// No user available: simply write a message and continue.
						log.error("jobs waiting=" + downloadJobDispatcher.getWaitingJobCount() + " errors=" + jobsRetryError);
					}
				}
				djp = null;
				log.debug("All download jobs have been completed!");
				if (tileArchive != null)
				{
					tileArchive.writeEndofArchive();
					tileArchive.close();
					tileIndex = tileArchive.getTarIndex();
					if (tileIndex.size() < tileCount)
					{
						int missing = tileCount - tileIndex.size();
						log.debug("Expected tile count: " + tileCount + " downloaded tile count: " + tileIndex.size() + " missing: " + missing);
						// int answer = JOptionPane.showConfirmDialog(ap, String.format(OSMCBStrs.getLocalizedString("dlg_download_errors_missing_tile_msg"), missing),
						// OSMCBStrs.getLocalizedString("dlg_download_errors_missing_tile"), JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
						// if (answer != JOptionPane.YES_OPTION)
						// throw new InterruptedException();
					}
				}
				downloadJobDispatcher.cancelOutstandingJobs();
				log.debug("Starting to create map from downloaded tiles");
				mapTileProvider = new DownloadedTileProvider(tileIndex, map);
			}
			else
			{
				// We don't need to download anything. Everything is already stored locally therefore we can just use it
				mapTileProvider = new FilteredMapSourceProvider(map, LoadMethod.DEFAULT);
			}
			bundleCreator.initializeMap(map, mapTileProvider);
			bundleCreator.createMap(map);
			bProgress.finishedMapCreation();
		}
		catch (Error e)
		{
			log.error("Error in createMap: " + e.getMessage(), e);
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
	public void pauseResumeBundleCreation()
	{
		if (pauseResumeHandler.isPaused())
		{
			log.debug("Bundle creation resumed");
			pauseResumeHandler.resume();
		}
		else
		{
			log.debug("Bundle creation paused");
			pauseResumeHandler.pause();
		}
	}

	@Override
	public boolean isPaused()
	{
		return pauseResumeHandler.isPaused();
	}

	public PauseResumeHandler getPauseResumeHandler()
	{
		return pauseResumeHandler;
	}

	/**
	 * Stop listener from {@link AtlasProgressGUI}
	 */
	@Override
	public void abortBundleCreation()
	{
		try
		{
			DownloadJobProducerThread djp_ = djp;
			if (djp_ != null)
				djp_.cancel();
			if (downloadJobDispatcher != null)
				downloadJobDispatcher.terminateAllWorkerThreads();
			pauseResumeHandler.resume();
			this.interrupt();
		}
		catch (Exception e)
		{
			log.error("Exception thrown in stopDownload()" + e.getMessage());
		}
	}

	@Override
	public int getActiveDownloads()
	{
		return activeDownloads;
	}

	@Override
	public synchronized void jobStarted()
	{
		activeDownloads++;
	}

	@Override
	public void jobFinishedSuccessfully(int bytesDownloaded)
	{
		synchronized (this)
		{
			activeDownloads--;
			jobsCompleted++;
		}
	}

	@Override
	public void jobFinishedWithError(boolean retry)
	{
		synchronized (this)
		{
			activeDownloads--;
			if (retry)
				jobsRetryError++;
			else
			{
				jobsPermanentError++;
			}
		}
	}

	@Override
	public int getMaxDownloadRetries()
	{
		return maxDownloadRetries;
	}

	@Override
	public File getCustomBundleDir()
	{
		return customBundleDir;
	}

	@Override
	public void setCustomBundleDir(File customAtlasDir)
	{
		this.customBundleDir = customAtlasDir;
	}

	@Override
	public void setQuitOsmcbAfterBundleCreation(boolean bQuit)
	{
	}

	/**
	 * 
	 * @return the BundleProgress object to collect and display the bundle creations progress data
	 */
	@Override
	public ACBundleProgress getBundleProgress()
	{
		return bProgress;
	}
}
