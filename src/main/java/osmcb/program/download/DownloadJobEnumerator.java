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
package osmcb.program.download;

import java.awt.Point;
import java.util.Enumeration;

import osmb.mapsources.IfMapSource;
import osmb.program.map.IfMap;
import osmb.program.tiles.IfTileFilter;
import osmcb.program.DownloadJob;
import osmcb.program.JobDispatcher.Job;
import osmcb.utilities.tar.TarIndexedArchive;

/**
 * Enumerates / creates the download jobs for a regular rectangle single layer map.
 */
public class DownloadJobEnumerator implements Enumeration<Job>
{
	final protected IfTileFilter tileFilter;
	final protected IfDownloadJobListener listener;
	final protected int xMin;
	final protected int xMax;
	final protected int yMax;
	final protected int zoom;
	final protected IfMapSource mapSource;
	final protected TarIndexedArchive tileArchive;

	protected int x, y;
	protected Job nextJob;

	/**
	 * This enumerator is the unfolded version for two encapsulated loops:
	 * 
	 * <pre>
	 * for (int y = yMin; y &lt;= yMax; y++)
	 * {
	 * 	for (int x = xMin; x &lt;= xMax; x++)
	 * 	{
	 * 		DownloadJob job = new DownloadJob(downloadDestinationDir, tileSource, x, y, zoom, BundleThread.this);
	 * 	}
	 * }
	 * </pre>
	 * 
	 * @param map
	 * @param tileArchive
	 * @param listener
	 */
	public DownloadJobEnumerator(IfMap map, IfMapSource mapSource, TarIndexedArchive tileArchive, IfDownloadJobListener listener)
	{
		this.tileFilter = map.getTileFilter();
		this.listener = listener;
		Point minCoord = map.getMinTileCoordinate();
		Point maxCoord = map.getMaxTileCoordinate();
		int tileSize = map.getMapSource().getMapSpace().getTileSize();
		this.xMin = minCoord.x / tileSize;
		this.xMax = maxCoord.x / tileSize;
		int yMin = minCoord.y / tileSize;
		this.yMax = maxCoord.y / tileSize;
		this.zoom = map.getZoom();
		this.tileArchive = tileArchive;
		this.mapSource = mapSource;
		y = yMin;
		x = xMin;

		nextJob = new DownloadJob(mapSource, x, y, zoom, tileArchive, listener);
		if (!tileFilter.testTile(x, y, zoom, mapSource))
			nextElement();
	}

	@Override
	public boolean hasMoreElements()
	{
		return (nextJob != null);
	}

	@Override
	public Job nextElement()
	{
		Job job = nextJob;
		boolean filter = false;
		do
		{
			x++;
			if (x > xMax)
			{
				y++;
				x = xMin;
				if (y > yMax)
				{
					nextJob = null;
					return job;
				}
			}
			filter = tileFilter.testTile(x, y, zoom, mapSource);
		} while (!filter);
		nextJob = new DownloadJob(mapSource, x, y, zoom, tileArchive, listener);
		return job;
	}
}
