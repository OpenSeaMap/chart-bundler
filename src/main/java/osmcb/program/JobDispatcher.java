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

import java.io.FileNotFoundException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import osmb.mapsources.IfMapSourceListener;
import osmb.program.tilestore.berkeleydb.DelayedInterruptThread;
import osmcb.OSMCBStrs;
import osmcb.program.bundle.StopAllDownloadsException;

/**
 * Controls the worker threads that are downloading the iMap tiles in parallel. Additionally the job queue containing the unprocessed tile download jobs can be
 * accessed via this class.
 */
public class JobDispatcher
{
	private static Logger log = Logger.getLogger(JobDispatcher.class);
	protected int maxJobsInQueue = 100; // AHTODO put queue sizes/levels in settings.xml
	protected int minJobsInQueue = 50;
	protected WorkerThread[] workers;
	protected PauseResumeHandler pauseResumeHandler = null;
	protected IfMapSourceListener mapSourceListener = null;
	protected BlockingQueue<Job> jobQueue = new LinkedBlockingQueue<Job>();

	public JobDispatcher(int threadCount, PauseResumeHandler pauseResumeHandler, IfMapSourceListener mapSourceListener)
	{
		this.pauseResumeHandler = pauseResumeHandler;
		this.mapSourceListener = mapSourceListener;
		workers = new WorkerThread[threadCount];
		for (int i = 0; i < threadCount; i++)
			workers[i] = new WorkerThread(i);
		log.trace("this =" + this + ", mapSourceListener=" + mapSourceListener);
	}

	@Override
	protected void finalize() throws Throwable
	{
		terminateAllWorkerThreads();
		super.finalize();
	}

	public void terminateAllWorkerThreads()
	{
		cancelOutstandingJobs();
		log.trace(OSMCBStrs.RStr("JobDispatcher.KillAllThreads"));
		for (int i = 0; i < workers.length; i++)
		{
			try
			{
				WorkerThread w = workers[i];
				if (w != null)
				{
					w.interrupt();
				}
				workers[i] = null;
			}
			catch (Exception e)
			{
				// We don't care about exception here
			}
		}
	}

	public void cancelOutstandingJobs()
	{
		log.trace(OSMCBStrs.RStr("JobDispatcher.CancelThreads"));
		jobQueue.clear();
	}

	/**
	 * Blocks if more than 100 jobs are already scheduled.
	 * 
	 * @param job
	 * @throws InterruptedException
	 */
	public void addJob(Job job) throws InterruptedException
	{
		log.trace(OSMCBStrs.RStr("JobDispatcher.AddJob"));
		log.trace("this =" + this + ", mapSourceListener=" + mapSourceListener);
		while (jobQueue.size() > maxJobsInQueue)
		{
			Thread.sleep(200);
			if ((jobQueue.size() < minJobsInQueue) && (maxJobsInQueue < 2000))
			{
				// System and download connection is very fast - we have to
				// increase the maximum job count in the queue
				maxJobsInQueue *= 2;
				minJobsInQueue *= 2;
			}
		}
		jobQueue.put(job);
	}

	/**
	 * Adds the job to the job-queue and returns. This method will never block!
	 * 
	 * @param job
	 */
	public void addErrorJob(Job job)
	{
		try
		{
			jobQueue.put(job);
		}
		catch (InterruptedException e)
		{
			// Can never happen with LinkedBlockingQueue
		}
	}

	public int getWaitingJobCount()
	{
		return jobQueue.size();
	}

	public static interface Job
	{
		public void run(JobDispatcher dispatcher) throws Exception;
	}

	public boolean isAtLeastOneWorkerActive()
	{
		for (int i = 0; i < workers.length; i++)
		{
			WorkerThread w = workers[i];
			if (w != null)
			{
				if ((!w.idle) && (w.getState() != Thread.State.WAITING))
					return true;
			}
		}
		log.debug(OSMCBStrs.RStr("JobDispatcher.AllIdle"));
		return false;
	}

	/**
	 * Each worker thread takes the first job from the job queue and executes it. If the queue is empty the worker blocks, waiting for the next job.
	 */
	protected class WorkerThread extends DelayedInterruptThread implements IfMapSourceListener
	{
		Job job = null;

		boolean idle = true;

		private Logger log = Logger.getLogger(WorkerThread.class);

		public WorkerThread(int threadNum)
		{
			super(String.format(OSMCBStrs.RStr("JobDispatcher.ThreadName"), threadNum));
			setDaemon(true);
			start();
		}

		@Override
		public void run()
		{
			try
			{
				executeJobs();
			}
			catch (InterruptedException e)
			{
			}
			log.trace(OSMCBStrs.RStr("JobDispatcher.terminating"));
		}

		protected void executeJobs() throws InterruptedException
		{
			while (!isInterrupted())
			{
				try
				{
					pauseResumeHandler.pauseWait();
					idle = true;
					job = jobQueue.take();
					idle = false;
				}
				catch (InterruptedException e)
				{
					return;
				}
				if (job == null)
					return;
				try
				{
					job.run(JobDispatcher.this);
					job = null;
				}
				catch (InterruptedException e)
				{
				}
				catch (StopAllDownloadsException e)
				{
					JobDispatcher.this.terminateAllWorkerThreads();
					JobDispatcher.this.cancelOutstandingJobs();
					log.warn(OSMCBStrs.RStr("JobDispatcher.AllStopped") + e.getMessage());
					return;
				}
				catch (FileNotFoundException e)
				{
					log.error(OSMCBStrs.RStr("JobDispatcher.FailedDownload") + e.getMessage());
				}
				catch (Exception e)
				{
					log.error(OSMCBStrs.RStr("JobDispatcher.UnknownError"), e);
				}
				catch (OutOfMemoryError e)
				{
					log.error(OSMCBStrs.RStr("JobDispatcher.empty"), e);
					Thread.sleep(5000);
					System.gc();
				}
			}
		}

		@Override
		public void tileDownloaded(int size)
		{
			log.trace("this =" + this + ", mapSourceListener=" + mapSourceListener);
			if (mapSourceListener != null)
				mapSourceListener.tileDownloaded(size);
			else
				log.error("mapSourceListener == null");
		}

		@Override
		public void tileLoadedFromCache(int size)
		{
			log.trace("this =" + this + ", mapSourceListener=" + mapSourceListener);
			if (mapSourceListener != null)
				mapSourceListener.tileLoadedFromCache(size);
			else
				log.error("mapSourceListener == null");
		}
	}
}
