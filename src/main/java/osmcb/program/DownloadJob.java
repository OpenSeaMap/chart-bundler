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

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import org.apache.log4j.Logger;

import osmb.mapsources.IfMapSource;
import osmb.mapsources.IfMapSource.LoadMethod;
import osmb.program.tiles.DownloadFailedException;
import osmb.program.tiles.UnrecoverableDownloadException;
import osmcb.program.JobDispatcher.Job;
import osmcb.program.bundlecreators.tileprovider.DownloadedTileProvider;
import osmcb.program.download.IfDownloadJobListener;
import osmcb.utilities.tar.TarIndexedArchive;

public class DownloadJob implements Job
{
	static Logger log = Logger.getLogger(DownloadJob.class);
	int errorCounter = 0;
	final IfMapSource mapSource;
	final int xValue;
	final int yValue;
	final int zoomValue;
	final TarIndexedArchive tileArchive;
	final IfDownloadJobListener listener;

	public DownloadJob(IfMapSource mapSource, int xValue, int yValue, int zoomValue, TarIndexedArchive tileArchive, IfDownloadJobListener listener)
	{
		this.mapSource = mapSource;
		this.xValue = xValue;
		this.yValue = yValue;
		this.zoomValue = zoomValue;
		this.tileArchive = tileArchive;
		this.listener = listener;
	}

	@Override
	public void run(JobDispatcher dispatcher) throws Exception
	{
		try
		{
			// Thread.sleep(1500);
			listener.jobStarted();
			byte[] tileData = mapSource.getTileData(zoomValue, xValue, yValue, LoadMethod.DEFAULT);
			String tileFileName = String.format(DownloadedTileProvider.TILE_FILENAME_PATTERN, xValue, yValue);
			if (tileArchive != null)
			{
				synchronized (tileArchive)
				{
					tileArchive.writeFileFromData(tileFileName, tileData);
				}
			}
			listener.jobFinishedSuccessfully(tileData.length);
		}
		catch (UnrecoverableDownloadException e)
		{
			listener.jobFinishedWithError(false);
			log.error("Download of tile z" + zoomValue + "_x" + xValue + "_y" + yValue + " failed with an unrecoverable error: " + e.getCause());
		}
		catch (InterruptedException e)
		{
			throw e;
		}
		// catch (StopAllDownloadsException e)
		// {
		// throw e;
		// }
		catch (SocketTimeoutException e)
		{
			processError(dispatcher, e);
		}
		catch (ConnectException e)
		{
			processError(dispatcher, e);
		}
		catch (DownloadFailedException e)
		{
			processError(dispatcher, e);
		}
		catch (Exception e)
		{
			processError(dispatcher, e);
			throw e;
		}
	}

	private void processError(JobDispatcher dispatcher, Exception e)
	{
		errorCounter++;
		// Reschedule job to try it later again
		if (errorCounter <= listener.getMaxDownloadRetries())
		{
			listener.jobFinishedWithError(true);
			log.warn("Download of tile z" + zoomValue + "_x" + xValue + "_y" + yValue + " failed: \"" + e.getMessage() + "\" (tries: " + errorCounter
					+ ") - rescheduling download job");
			dispatcher.addErrorJob(this);
		}
		else
		{
			listener.jobFinishedWithError(false);
			log.error("Download of tile z" + zoomValue + "_x" + xValue + "_y" + yValue + " failed again: \"" + e.getMessage() + "\". Retry limit reached, "
					+ "job will not be rescheduled (no further try)");
		}
	}

	@Override
	public String toString()
	{
		return "DownloadJob x=" + xValue + " y=" + yValue + " z=" + zoomValue;
	}

}
