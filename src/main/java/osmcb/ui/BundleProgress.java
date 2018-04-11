/*******************************************************************************
 * Copyright (c) MOBAC developers
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
package osmcb.ui;

import java.util.ArrayList;
import java.util.Timer;

import org.apache.log4j.Logger;

import osmb.program.map.IfMap;
import osmcb.OSMCBStrs;
import osmcb.program.bundle.ACBundleProgress;
import osmcb.program.bundlecreators.ACBundleCreator;

/**
 * A window showing the progress while {@link BundleThread} downloads and processes the map tiles.
 * 
 */
public class BundleProgress extends ACBundleProgress
{
	@SuppressWarnings("unused") // W #unused -> no need for serialVersionUID
	private static final long serialVersionUID = -1L;
	
	private static Logger log = Logger.getLogger(BundleProgress.class);
	
	@SuppressWarnings("unused") // W #unused
	private static final Timer TIMER = new Timer(true);

	// private JProgressBar atlasProgressBar;
	// private JProgressBar mapDownloadProgressBar;
	// private JProgressBar mapCreationProgressBar;
	// private Container background;

	private IfBCControler downloadControlListener = null;
	// private BundleThread bundleThread = null;
	@SuppressWarnings("unused") // W #unused
	private ArrayList<MapInfo> mapInfos = null;
	public String timeLeftTotal;
	public String bps;

	// private static String TEXT_MAP_DOWNLOAD = OSMCBStrs.RStr("dlg_download_zoom_level_progress");
	// private static String TEXT_PERCENT = OSMCBStrs.RStr("dlg_download_done_percent");
	// private static String TEXT_TENTHPERCENT = OSMCBStrs.RStr("dlg_download_done_tenthpercent");

	public BundleProgress(ACBundleCreator acBundleCreator)
	{
		super(acBundleCreator);
	}

	// private void printData()
	// {
	// sLog.info(OSMCBStrs.RStr("BundleProgress.Data"));
	// if (data.iBundle != null)
	// sLog.info("bundle: '" + data.iBundle.getName() + "; level " + data.mapCurrent + " of " + data.mapsTotal + "; map " + data.mapCurrent + " of "
	// + data.mapsTotal + "; tiles " + data.tilesDLMap + " of " + data.tilesTotal + "; bytes downloaded " + data.bytesDLTotal + ", from cache "
	// + data.bytesCachedTotal + " last tile " + data.bytesLastTile);
	// }

	// @Override
	// public void initBundle(IfBundle bundle)
	// {
	// data.bundle = bundle;
	// if (bundle.getOutputFormat().equals(BundleOutputFormat.TILESTORE))
	// data.tilesTotal = (int) bundle.calculateTilesToDownload();
	// else
	// data.tilesTotal = (int) bundle.calculateTilesToDownload() * 2;
	// int mapCount = 0;
	// int tileCount = 0;
	// mapInfos = new ArrayList<MapInfo>(100);
	// for (IfLayer layer : bundle)
	// {
	// mapCount += layer.getMapCount();
	// for (IfMap iMap : layer)
	// {
	// int before = tileCount;
	// int mapTiles = (int) iMap.calculateTilesToDownload();
	// tileCount += mapTiles + mapTiles;
	// mapInfos.add(new MapInfo(iMap, before, tileCount));
	// }
	// }
	// mapInfos.trimToSize();
	// data.mapsTotal = mapCount;
	//
	// initialTotalTime = System.currentTimeMillis();
	// initialMapDownloadTime = -1;
	// sLog.trace(OSMCBStrs.RStr("BundleProgress.Init"));
	// printData();
	// }

	/**
	 * called by BundleThread NO
	 * 
	 * @param map
	 */
	@Override
	public void initMapDownload(IfMap map)
	{
		// data.totalProgress = data.mapInfo.tileCountOnStart;
		data.map = map;
		data.tilesDLMap = (int) map.calculateTilesToLoad();
		initialMapDownloadTime = System.currentTimeMillis();
		data.prevMapsPermanentErrors += data.mapPermanentErrors;
		data.prevMapsRetryErrors += data.mapRetryErrors;
		data.mapCreationProgress = 0;
		data.mapDownloadProgress = 0;
		log.trace(OSMCBStrs.RStr("BundleProgress.NextMap"));
		printData();
	}

	/**
	 * Initialize the GUI progress bars
	 * 
	 * @param maxTilesToProcess
	 */
	public void initMapCreation(int maxTilesToProcess)
	{
		data.mapCurrent++;
		data.mapCreationProgress = 0;
		data.mapCreationMax = maxTilesToProcess;
		initialMapDownloadTime = -1;
		log.trace(OSMCBStrs.RStr("BundleProgress.InitMap"));
	}

	public void finishedMapCreation()
	{
		data.mapCreationProgress = 100;
		log.trace(OSMCBStrs.RStr("BundleProgress.MapFinished"));
	}

	@Override
	public void setErrorCounter(int retryErrors, int permanentErrors)
	{
		data.mapRetryErrors = retryErrors;
		data.mapPermanentErrors = permanentErrors;
		// updateGUI();
	}

	@Override
	public void incMapDownloadProgress()
	{
		data.mapDownloadProgress++;
		// data.totalProgress++;
		// updateGUI();
	}

	@Override
	public void incMapCreationProgress()
	{
		setMapCreationProgress(data.mapCreationProgress + 1);
	}

	@Override
	public void incMapCreationProgress(int stepSize)
	{
		setMapCreationProgress(data.mapCreationProgress + stepSize);
	}

	@Override
	public void setMapCreationProgress(int progress)
	{
		data.mapCreationProgress = progress;
		// data.totalProgress = data.mapInfo.tileCountOnStart + data.mapInfo.mapTiles
		// + (int) (((long) data.mapInfo.mapTiles) * data.mapCreationProgress / data.mapCreationMax);
		printData();
	}

	// public boolean ignoreDownloadErrors()
	// {
	// return ignoreDlErrors.isSelected();
	// }
	//
	/**
	 * called by JobDispatcher.WorkerThread
	 */
	@Override
	public void tileDownloaded(int size)
	{
		synchronized (data)
		{
			data.bytesDLTotal += size;
			data.bytesLastTile = size;
			data.tilesDLMap++;
		}
		printData();
	}

	/**
	 * called by JobDispatcher.WorkerThread
	 */
	@Override
	public void tileLoadedFromCache(int size)
	{
		synchronized (data)
		{
			data.bytesCachedTotal += size;
			data.bytesLastTile = size;
			data.tilesDLMap++;
		}
		printData();
	}

	@SuppressWarnings("unused") // W #unused
	private void avgBytesPerTile(int size)
	{
		data.bytesAvgTile = 0;
	}

	@SuppressWarnings("unused") // W #unused
	private String formatTime(long longSeconds)
	{
		String timeString = "";

		if (longSeconds < 0)
		{
			timeString = OSMCBStrs.RStr("BundleProgress.TimeUnknown");
		}
		else
		{
			int minutes = (int) (longSeconds / 60);
			int seconds = (int) (longSeconds % 60);
			if (minutes > 0)
				timeString += Integer.toString(minutes) + " " + (minutes == 1 ? OSMCBStrs.RStr("minute") : OSMCBStrs.RStr("minutes")) + " ";
			timeString += Integer.toString(seconds) + " " + (seconds == 1 ? OSMCBStrs.RStr("second") : OSMCBStrs.RStr("seconds"));
		}
		return timeString;
	}

	// public void setZoomLevel(int theZoomLevel)
	// {
	// mapDownloadTitle.setText(TEXT_MAP_DOWNLOAD + Integer.toString(theZoomLevel));
	// }
	//
	public void bundleCreationFinished()
	{
		finished = true;
		downloadControlListener = null;
	}

	@Override
	public IfBCControler getDownloadControlListener()
	{
		return downloadControlListener;
	}

	// public void setDownloadControlerListener(BundleThread bundleThread)
	// {
	// sLog.trace("BP: setDownloadControlerListener()");
	// // this.downloadControlListener = (IfBCControler) bundleThread;
	// }
}
