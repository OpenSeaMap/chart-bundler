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

import osmb.program.map.IfMap;
import osmcb.program.JobDispatcher.Job;
import osmcb.program.bundle.BundleThread;
import osmcb.program.bundlecreators.IfBundleCreator;
import osmcb.utilities.tar.TarIndexedArchive;

/**
 * Creates the jobs for downloading tiles. If the job queue is full it will
 * block on {@link JobDispatcher#addJob(Job)}
 */
public class DownloadJobProducerThread extends Thread
{
	private Logger log = Logger.getLogger(DownloadJobProducerThread.class);
	final JobDispatcher downloadJobDispatcher;
	final Enumeration<Job> jobEnumerator;

	public DownloadJobProducerThread(BundleThread bundleThread, JobDispatcher downloadJobDispatcher, TarIndexedArchive tileArchive, IfMap map)
	{
		this.downloadJobDispatcher = downloadJobDispatcher;
		jobEnumerator = new DownloadJobEnumerator(map, map.getMapSource(), tileArchive, bundleThread);

		start();
	}

	public DownloadJobProducerThread(IfBundleCreator acBundleCreator, JobDispatcher downloadJobDispatcher2, TarIndexedArchive tileArchive, IfMap map)
	{
		downloadJobDispatcher = downloadJobDispatcher2;
		jobEnumerator = new DownloadJobEnumerator(map, map.getMapSource(), tileArchive, null);
	}

	@Override
	public void run()
	{
		try
		{
			while (jobEnumerator.hasMoreElements())
			{
				Job job = jobEnumerator.nextElement();
				downloadJobDispatcher.addJob(job);
				log.trace("Job added: " + job);
			}
			log.debug("All download jobs has been generated");
		}
		catch (InterruptedException e)
		{
			downloadJobDispatcher.cancelOutstandingJobs();
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
