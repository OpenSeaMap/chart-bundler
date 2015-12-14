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
package osmcb.program.tilefilter;

import java.awt.Polygon;

import osmb.mapsources.IfMapSource;
import osmb.mapsources.MP2MapSpace;
import osmb.program.map.MapPolygon;
import osmb.program.tiles.IfTileFilter;

public class PolygonTileFilter implements IfTileFilter
{
	private final Polygon polygon;
	private final int tileSize;
	private final int polygonZoom;

	public PolygonTileFilter(MapPolygon map)
	{
		this(map.getPolygon(), map.getZoom(), map.getMapSource());
	}

	public PolygonTileFilter(Polygon polygon, int polygonZoom, IfMapSource mapSource)
	{
		super();
		this.polygon = polygon;
		this.polygonZoom = polygonZoom;
		this.tileSize = MP2MapSpace.getTileSize(); // #mapSpace  mapSource.getMapSpace().getTileSize();
	}

	/**
	 * returns <code>true</code> if the tile and the polygon intersect
	 */
	@Override
	public boolean testTile(int x, int y, int zoom, IfMapSource mapSource)
	{
		if (polygonZoom != zoom)
			throw new RuntimeException("Wrong zoom level!");
		int tileCoordinateX = x * tileSize;
		int tileCoordinateY = y * tileSize;
		return polygon.intersects(tileCoordinateX, tileCoordinateY, tileSize, tileSize);
	}
}
