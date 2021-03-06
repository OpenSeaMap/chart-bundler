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
package osmcb.program.bundle;

import java.util.ArrayList;
import java.util.Timer;

import org.apache.log4j.Logger;

import osmb.mapsources.IfMapSourceListener;
import osmb.program.map.IfLayer;
import osmb.program.map.IfMap;
import osmb.program.tiles.Tile;
import osmb.utilities.OSMBStrs;
import osmcb.OSMCBStrs;
import osmcb.program.bundlecreators.ACBundleCreator;

/**
 * A window showing the progress while XXX downloads and processes the map tiles.
 * 
 */
public class ACBundleProgress implements IfMapSourceListener
{
	// @SuppressWarnings("unused") // W #unused -> no need for serialVersionUID
	// private static final long serialVersionUID = -1L;

	protected static Logger log = Logger.getLogger(ACBundleProgress.class);

	protected static final Timer TIMER = new Timer(true);
	// private JProgressBar atlasProgressBar;
	// private JProgressBar mapDownloadProgressBar;
	// private JProgressBar mapCreationProgressBar;
	// private Container background;
	protected long initialTotalTime;
	protected long initialMapDownloadTime;

	public static class Data
	{
		public IfBundle bundle;
		public IfLayer layer;
		public IfMap map;
		public MapInfo mapInfo;
		public long bytesDLTotal = 0;
		public long bytesCachedTotal = 0;
		public int bytesAvgTile = 0;
		public int bytesLastTile = 0;
		public int tileCurrent = 0;
		public int tilesTotal = 0;
		public int tilesDLMap = 0;
		public int mapDownloadProgress = 0;
		public int mapCreationProgress = 0;
		public int mapCurrent = 0;
		public int mapsTotal = 0;
		public int layerCurrent = 0;
		public int layersTotal = 0;
		public int bundleProgress = 0;

		public long mapCreationMax = 0;
		public int mapRetryErrors = 0;
		public int mapPermanentErrors = 0;
		public int prevMapsRetryErrors = 0;
		public int prevMapsPermanentErrors = 0;
		public int totalProgressTenthPercent = 0;
		public boolean paused = false;
	}

	protected final Data data = new Data();
	protected boolean aborted = false;
	protected boolean finished = false;
	protected IfBCControler downloadControlListener = null;
	// protected IfBundleThread bundleThread = null;
	protected ACBundleCreator mBC = null;
	protected ArrayList<MapInfo> mapInfos = null;
	public String timeLeftTotal;
	public String bps;

	// private static String TEXT_MAP_DOWNLOAD = OSMCBStrs.RStr("dlg_download_zoom_level_progress");
	// private static String TEXT_PERCENT = OSMCBStrs.RStr("dlg_download_done_percent");
	// private static String TEXT_TENTHPERCENT = OSMCBStrs.RStr("dlg_download_done_tenthpercent");

	// public ACBundleProgress(IfBundleThread bundleThread)
	// {
	// this.mBC = bundleThread;
	// initialTotalTime = System.currentTimeMillis();
	// initialMapDownloadTime = System.currentTimeMillis();
	// }

	// progress calculation
	protected long mTileCount = 0;
	protected long mTilesFinished = 0;
	protected long mMapCount = 0;
	protected long mMapsFinished = 0;

	public ACBundleProgress(ACBundleCreator bc)
	{
		this.mBC = bc;
		initialTotalTime = System.currentTimeMillis();
		initialMapDownloadTime = System.currentTimeMillis();
	}

	protected void printData()
	{
		if (data.bundle != null)
			log.info(OSMCBStrs.RStr("BundleProgress.Data") + " '" + data.bundle.getName() + "'; layer '" + (data.layer != null ? data.layer.getName() : "") + "' "
			    + data.layerCurrent + " of " + data.layersTotal + "; map " + data.mapCurrent + " of " + data.mapsTotal + "; tiles " + data.tilesDLMap + " of "
			    + data.tilesTotal + "; bytes downloaded " + data.bytesDLTotal + ", from cache " + data.bytesCachedTotal + " last tile " + data.bytesLastTile);
	}

	public void initBundle(IfBundle bundle)
	{
		data.bundle = bundle;
		data.tilesTotal = (int) bundle.calculateTilesToLoad();
		int mapCount = 0;
		int tileCount = 0;
		data.layersTotal = bundle.getLayerCount();
		data.layerCurrent = 0;
		mapInfos = new ArrayList<MapInfo>(100);
		for (IfLayer layer : bundle)
		{
			mapCount += layer.getMapCount();
			for (IfMap map : layer)
			{
				int before = tileCount;
				int mapTiles = (int) map.calculateTilesToLoad();
				tileCount += mapTiles + mapTiles;
				mapInfos.add(new MapInfo(map, before, tileCount));
			}
		}
		mapInfos.trimToSize();
		data.mapsTotal = mapCount;

		initialTotalTime = System.currentTimeMillis();
		initialMapDownloadTime = -1;
		log.trace(OSMBStrs.RStr("BundleProgress.Init"));
		printData();
		log.trace(OSMBStrs.RStr("BundleProgress.Init"));
	}

	public void finishBundle()
	{
		finished = true;
		downloadControlListener = null;
		printData();
	}

	public void initLayer(IfLayer layer)
	{
		data.layerCurrent++;
		data.layer = layer;
		printData();
	}

	public void finishLayer(IfLayer layer)
	{
		printData();
		log.trace(OSMCBStrs.RStr("BundleProgress.LayerFinished"));
	}

	/**
	 * Initialize the GUI progress bars
	 * 
	 * @param l
	 */
	public void initMap(long l)
	{
		data.mapCurrent++;
		data.mapCreationProgress = 0;
		data.mapCreationMax = l;
		initialMapDownloadTime = -1;
		printData();
		log.trace(OSMCBStrs.RStr("BundleProgress.InitMap"));
	}

	/**
	 * called by {@link ACBundleCreator}
	 * 
	 * @param mMap
	 */
	public void initMapDownload(IfMap mMap)
	{
		int index = mapInfos.indexOf(new MapInfo(mMap, 0, 0));
		data.mapInfo = mapInfos.get(index);
		data.bundleProgress = data.mapInfo.tileCountOnStart;
		data.map = mMap;
		data.tilesDLMap = (int) mMap.calculateTilesToLoad();
		initialMapDownloadTime = System.currentTimeMillis();
		data.prevMapsPermanentErrors += data.mapPermanentErrors;
		data.prevMapsRetryErrors += data.mapRetryErrors;
		data.mapDownloadProgress = 0;
		data.mapCurrent = index + 1;
		printData();
		log.trace(OSMCBStrs.RStr("BundleProgress.NextMap"));
	}

	public void finishMapDownload(IfMap map)
	{
		data.mapDownloadProgress = 100;
		printData();
	}

	public void finishMap(IfMap mMap)
	{
		mMapsFinished++;
		data.mapCreationProgress = 100;
		printData();
		log.trace(OSMCBStrs.RStr("BundleProgress.MapFinished"));
	}

	public void initTileDownload(Tile tile)
	{
		printData();
		log.debug("start tile load " + tile);
	}

	public void finishTileDownload(Tile tile)
	{
		data.mapDownloadProgress = 100;
		printData();
	}

	public void setErrorCounter(int retryErrors, int permanentErrors)
	{
		data.mapRetryErrors = retryErrors;
		data.mapPermanentErrors = permanentErrors;
		// updateGUI();
	}

	public void incMapDownloadProgress()
	{
		data.mapDownloadProgress++;
		data.bundleProgress++;
		// updateGUI();
	}

	public void incMapCreationProgress()
	{
		setMapCreationProgress(data.mapCreationProgress + 1);
	}

	public void incMapCreationProgress(int stepSize)
	{
		setMapCreationProgress(data.mapCreationProgress + stepSize);
	}

	public void setMapCreationProgress(int progress)
	{
		data.mapCreationProgress = progress;
		data.bundleProgress = data.mapInfo.tileCountOnStart + data.mapInfo.mapTiles
		    + (int) (((long) data.mapInfo.mapTiles) * data.mapCreationProgress / data.mapCreationMax);
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
			timeString = OSMCBStrs.RStr("dlg_download_time_unknown");
		}
		else
		{
			int minutes = (int) (longSeconds / 60);
			int seconds = (int) (longSeconds % 60);
			if (minutes > 0)
				timeString += Integer.toString(minutes) + " " + (minutes == 1 ? OSMBStrs.RStr("minute") : OSMBStrs.RStr("minutes")) + " ";
			timeString += Integer.toString(seconds) + " " + (seconds == 1 ? OSMBStrs.RStr("second") : OSMBStrs.RStr("seconds"));
		}
		return timeString;
	}

	// public void setZoomLevel(int theZoomLevel)
	// {
	// mapDownloadTitle.setText(TEXT_MAP_DOWNLOAD + Integer.toString(theZoomLevel));
	// }
	//
	public IfBCControler getDownloadControlListener()
	{
		return downloadControlListener;
	}

	public void setDownloadControlerListener(IfBundleThread bundleThread)
	{
		log.debug("BP: setDownloadControlerListener()");
		// this.downloadControlListener = (IfBCControler) bundleThread;
	}

	protected static class MapInfo
	{
		final IfMap map;
		public final int tileCountOnStart;
		final int tileCountOnEnd;
		public final int mapTiles;

		public MapInfo(IfMap map, int tileCountOnStart, int tileCountOnEnd)
		{
			super();
			this.map = map;
			this.tileCountOnStart = tileCountOnStart;
			this.tileCountOnEnd = tileCountOnEnd;
			this.mapTiles = (int) map.calculateTilesToLoad();
		}

		@Override
		public int hashCode()
		{
			return map.hashCode();
		}

		@Override
		public boolean equals(Object obj)
		{
			if (!(obj instanceof MapInfo))
				return false;
			return map.equals(((MapInfo) obj).map);
		}
	}

	public static interface IfBCControler
	{
		public void abortBundleCreation();

		public void pauseResumeBundleCreation();

		public boolean isPaused();
	}

}
