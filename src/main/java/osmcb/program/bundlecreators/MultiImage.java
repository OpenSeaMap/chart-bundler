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
package osmcb.program.bundlecreators;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;

import osmb.mapsources.IfMapSource;
import osmb.mapsources.MP2MapSpace;
import osmb.program.map.BoundingRect;
import osmb.program.map.IfMap;
// W #mapSpace import osmb.program.map.IfMapSpace;
import osmb.program.tiles.IfTileProvider;
import osmcb.program.bundle.MapCreationException;
import osmcb.utilities.collections.SoftHashMap;

/**
 * CalibratedImage that gets its data from a set of other CalibratedImage2
 * 
 */
public class MultiImage
{
	private static final Logger log = Logger.getLogger(MultiImage.class);

	private final IfMap map;
// W #mapSpace //	private final IfMapSource mapSource;
	private final int zoom;
	private final IfTileProvider tileProvider;
	private SoftHashMap<TileKey, OsmcbTile> cache;

	public MultiImage(IfMapSource mapSource, IfTileProvider tileProvider, IfMap map)
	{
// W #mapSpace // this.mapSource = mapSource;
		this.tileProvider = tileProvider;
		this.zoom = map.getZoom();
		this.map = map;
		cache = new SoftHashMap<TileKey, OsmcbTile>(400);
	}

	public BufferedImage getSubImage(BoundingRect area, int width, int height) throws MapCreationException
	{
		if (log.isTraceEnabled())
			log.trace(String.format("getSubImage %d %d %s", width, height, area));

		// W #mapSpace IfMapSpace mapSpace = mapSource.getMapSpace();
		int tilesize = MP2MapSpace.getTileSize(); // W #mapSpace mapSpace.getTileSize();

		int xMax =  MP2MapSpace.cLonToX(area.getEast(), zoom) / tilesize; // W #mapSpace mapSource.getMapSpace().cLonToX(area.getEast(), zoom) / tilesize;
		int xMin =  MP2MapSpace.cLonToX(area.getWest(), zoom) / tilesize; // W #mapSpace mapSource.getMapSpace().cLonToX(area.getWest(), zoom) / tilesize;
		int yMax =  MP2MapSpace.cLatToY(-area.getSouth(), zoom) / tilesize; // W #mapSpace mapSource.getMapSpace().cLatToY(-area.getSouth(), zoom) / tilesize;
		int yMin =  MP2MapSpace.cLatToY(-area.getNorth(), zoom) / tilesize; // W #mapSpace mapSource.getMapSpace().cLatToY(-area.getNorth(), zoom) / tilesize;

		BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

		Graphics2D graph = result.createGraphics();
		try
		{
			graph.setColor(Color.WHITE);
			graph.fillRect(0, 0, width, height);

			for (int x = xMin; x <= xMax; x++)
			{
				for (int y = yMin; y <= yMax; y++)
				{
					TileKey key = new TileKey(x, y);
					OsmcbTile image = cache.get(key);
					if (image == null)
					{
						image = new OsmcbTile(tileProvider, x, y, zoom); // W #mapSpace (tileProvider, mapSpace, x, y, zoom);
						cache.put(key, image);
					}
					image.drawSubImage(area, result);
				}
			}
		}
		catch (Throwable t)
		{
			throw new MapCreationException(map, t);
		}
		finally
		{
			graph.dispose();
		}
		return result;
	}

	protected static class TileKey
	{
		int x;
		int y;

		public TileKey(int x, int y)
		{
			this.x = x;
			this.y = y;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + x;
			result = prime * result + y;
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TileKey other = (TileKey) obj;
			if (x != other.x)
				return false;
			if (y != other.y)
				return false;
			return true;
		}

	}
}
