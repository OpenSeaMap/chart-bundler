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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import osmb.program.tilestore.ACSiTileStore;
import osmb.utilities.GUIExceptionHandler;
import osmcb.OSMCBSettings;
import osmcb.program.PauseResumeHandler;
import osmcb.program.bundlecreators.ACBundleCreator;
import osmcb.program.download.IfDownloadJobListener;
import osmcb.ui.BundleProgress;

/**
 * Each bundleThread creates one distinct bundle, which is a certain 'chart
 * bundle catalog' to be composed in one 'output format'. To improve the overall
 * performance this should be executed by several threads. The division in
 * threads by layers is easy and safe, while the division by maps, which would
 * be better performing poses some concurrency questions.
 * 
 * @author humbach
 *
 */
// public class BundleThread extends Thread implements IfBundleThread,
// IfDownloadJobListener, BundleCreationController
public class BundleThread extends Thread implements IfBundleThread, IfDownloadJobListener
{
	private static final Logger log = Logger.getLogger(BundleThread.class);

	private ExecutorService mExec;
	private ACBundleCreator mBundleCreator = null;

	private static AtomicInteger threadNum = new AtomicInteger(0);
	private File customBundleDir = null;
	private PauseResumeHandler pauseResumeHandler = null;
	private int activeDownloads = 0;
	private int maxDownloadRetries = 1;
	private BundleProgress bProgress = null;

	private static int getNextThreadNum()
	{
		return threadNum.addAndGet(1);
	}

	public BundleThread(IfBundle bundle) throws BundleTestException, InstantiationException, IllegalAccessException
	{
		// this(bundle.getOutputFormat().createBundleCreatorInstance());
		this(bundle.createBundleCreatorInstance());
	}

	private BundleThread(ACBundleCreator bundleCreator) throws BundleTestException
	{
		super("BundleThread " + getNextThreadNum());
		mBundleCreator = bundleCreator;
		// testBundle();
		ACSiTileStore.getInstance().closeAll();
		maxDownloadRetries = OSMCBSettings.getInstance().getDownloadRetryCount();
		pauseResumeHandler = new PauseResumeHandler();
		mExec = Executors.newSingleThreadExecutor();
	}

	@Override
	public void run()
	{
		try
		{
			IfBundle bundle = mBundleCreator.getBundle();
			GUIExceptionHandler.registerForCurrentThread();
			if (bProgress == null)
				bProgress = new BundleProgress(this);
			bProgress.initBundle(bundle);
			mBundleCreator.setBundleProgress(bProgress);
			log.info("Starting creation of '" + bundle.getOutputFormat() + "' bundle '" + bundle.getName() + "'");
			if (customBundleDir != null)
				log.debug("Target directory: " + customBundleDir);
			bProgress.setDownloadControlerListener(this);
			log.trace("before bundle creation");
			mBundleCreator.run();
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

	// @Override
	// public void pauseResumeBundleCreation()
	// {
	// if (pauseResumeHandler.isPaused())
	// {
	// log.debug("Bundle creation resumed");
	// pauseResumeHandler.resume();
	// }
	// else
	// {
	// log.debug("Bundle creation paused");
	// pauseResumeHandler.pause();
	// }
	// }

	// @Override
	public boolean isPaused()
	{
		return pauseResumeHandler.isPaused();
	}

	public PauseResumeHandler getPauseResumeHandler()
	{
		return pauseResumeHandler;
	}

	@Override
	public int getMaxDownloadRetries()
	{
		return maxDownloadRetries;
	}

	// @Override
	public File getCustomBundleDir()
	{
		return customBundleDir;
	}

	// @Override
	public void setCustomBundleDir(File customAtlasDir)
	{
		this.customBundleDir = customAtlasDir;
	}

	// @Override
	public void setQuitOsmcbAfterBundleCreation(boolean bQuit)
	{
	}

	/**
	 * @return the BundleProgress UI-object to collect and display the bundles creation progress data
	 */
	@Override
	public ACBundleProgress getBundleProgress()
	{
		return bProgress;
	}

	@Override
	public void jobStarted()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void jobFinishedSuccessfully(int bytesDownloaded)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void jobFinishedWithError(boolean retry)
	{
		// TODO Auto-generated method stub

	}
}
