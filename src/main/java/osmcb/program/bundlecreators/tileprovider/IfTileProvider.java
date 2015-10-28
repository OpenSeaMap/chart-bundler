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

import java.awt.image.BufferedImage;
import java.io.IOException;

import osmb.mapsources.IfMapSource;

/**
 * A TileProvider provides image tiles, i.e. 256x256 pixel squares, from a specified map source.
 * The map source is usually an online source and the already downloaded tiles are hold in a local tile store.
 * 
 * @author humbach
 *
 */
public interface IfTileProvider
{
	public byte[] getTileData(int x, int y) throws IOException;

	public BufferedImage getTileImage(int x, int y) throws IOException;

	public IfMapSource getMapSource();

	/**
	 * Indicates if subsequent filter in the filter-chain should prefer the {@link #getTileImage(int, int)} or {@link #getTileData(int, int)} method.
	 * 
	 * @return
	 */
	public boolean preferTileImageUsage();
}
