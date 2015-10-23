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
package osmcb.program;

import java.util.Enumeration;

import org.apache.log4j.Logger;

import osmb.program.IfJob;
import osmb.program.JobDispatcher;
import osmb.program.map.IfMap;
import osmcb.program.bundlecreators.IfBundleCreator;
import osmcb.utilities.tar.TarIndexedArchive;

/**
 * Creates the jobs for downloading tiles. If the job queue is full it will
 * block on {@link JobDispatcher#addJob(IfJob)}
 */
public class DownloadJobProducerThread extends Thread
{
	private Logger log = Logger.getLogger(DownloadJobProducerThread.class);
	final JobDispatcher mDLJobDispatcher = JobDispatcher.getInstance();
	final Enumeration<IfJob> jobEnumerator;

	// public DownloadJobProducerThread(BundleThread bundleThread, JobDispatcher downloadJobDispatcher, TarIndexedArchive tileArchive, IfMap map)
	// {
	// this.downloadJobDispatcher = downloadJobDispatcher;
	// jobEnumerator = new DownloadJobEnumerator(map, map.getMapSource(), tileArchive, bundleThread);
	//
	// start();
	// }

	public DownloadJobProducerThread(IfBundleCreator acBundleCreator, TarIndexedArchive tileArchive, IfMap map)
	{
		jobEnumerator = new DownloadJobEnumerator(map, map.getMapSource(), tileArchive, null);
	}

	@Override
	public void run()
	{
		try
		{
			while (jobEnumerator.hasMoreElements())
			{
				IfJob job = jobEnumerator.nextElement();
				mDLJobDispatcher.addJob(job);
				log.trace("Job added: " + job);
			}
			log.debug("All download jobs have been generated");
		}
		// catch (InterruptedException e)
		catch (Exception e)
		{
			mDLJobDispatcher.cancelOutstandingJobs();
			log.error("Download job generation interrupted");
		}
	}

	public void cancel()
	{
		try
		{
			interrupt();
		}
		catch (Exception e)
		{
		}
	}
}
