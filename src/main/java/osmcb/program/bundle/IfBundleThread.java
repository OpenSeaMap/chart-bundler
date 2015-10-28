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

import osmcb.program.bundlecreators.IfBundleCreator;

/**
 * Thread to create bundles
 * 
 * @author humbach
 * 
 */
public interface IfBundleThread
{
	// static final Logger log;
	File customBundleDir = null;
	// DownloadJobProducerThread djp = null;
	// JobDispatcher downloadJobDispatcher = null;
	IfBundle bundle = null;
	IfBundleCreator bundleCreator = null;
	// PauseResumeHandler pauseResumeHandler = null;
	int activeDownloads = 0;
	int jobsCompleted = 0;
	int jobsRetryError = 0;
	int jobsPermanentError = 0;
	int maxDownloadRetries = 1;
	ACBundleProgress bProgress = null;

	// void testBundle() throws BundleTestException;

	public void run();

	/**
	 * Create bundle: For each Layer and for each map download the tiles and perform bundle/map creation
	 */
	// public void composeBundle() throws InterruptedException, IOException;

	// public void pauseResumeBundleCreation();

	// public boolean isPaused();

	// public PauseResumeHandler getPauseResumeHandler();

	/**
	 * Stop listener from {@link AtlasProgressGUI}
	 */
	// public void abortBundleCreation();

	// public int getActiveDownloads();
	//
	// public void jobStarted();
	//
	// public void jobFinishedSuccessfully(int bytesDownloaded);
	//
	// public void jobFinishedWithError(boolean retry);
	//
	// public int getMaxDownloadRetries();
	//
	// public File getCustomBundleDir();
	//
	// public void setCustomBundleDir(File customAtlasDir);
	//
	// public void setQuitOsmcbAfterBundleCreation(boolean bQuit);

	/**
	 * 
	 * @return the BundleProgress object to collect and display the bundles creation progress data
	 */
	public ACBundleProgress getBundleProgress();
}
