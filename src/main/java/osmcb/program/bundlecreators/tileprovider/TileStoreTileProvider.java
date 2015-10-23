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
package osmcb.program.bundlecreators.tileprovider;

import osmb.mapsources.IfMapSource;
import osmb.mapsources.IfMapSource.LoadMethod;

/**
 * A {@link IfTileProvider} implementation that retrieves all tiles from the tile store (if the <code>mapSource</code> supports that).
 */
public class TileStoreTileProvider extends MapSourceProvider
{
	public TileStoreTileProvider(IfMapSource mapSource, int zoom)
	{
		super(mapSource, zoom, LoadMethod.CACHE);
	}
}
