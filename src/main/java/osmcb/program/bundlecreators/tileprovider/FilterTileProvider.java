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

import org.apache.log4j.Logger;

import osmb.program.tiles.IfTileProvider;

/**
 * Base implementation of an {@link IfTileProvider} that changes somehow the images, e.g. combines two layers to one or paints something onto a tile image.
 */
public abstract class FilterTileProvider implements IfTileProvider
{
	protected static Logger log;

	protected final IfTileProvider tileProvider;

	public FilterTileProvider(IfTileProvider tileProvider)
	{
		log = Logger.getLogger(this.getClass());
		this.tileProvider = tileProvider;
	}
}
