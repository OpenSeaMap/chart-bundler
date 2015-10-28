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
import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;

import osmb.mapsources.IfMapSource;
import osmb.program.map.IfMap;
import osmb.program.tiles.TileImageType;
import osmcb.utilities.tar.TarIndex;

public class DownloadedTileProvider implements IfTileProvider
{
	private static final Logger log = Logger.getLogger(DownloadedTileProvider.class);
	public static final String TILE_FILENAME_PATTERN = "x%dy%d";
	protected final TarIndex tarIndex;
	protected final IfMap map;
	protected final TileImageType mapTileType;

	public DownloadedTileProvider(TarIndex tarIndex, IfMap map)
	{
		this.tarIndex = tarIndex;
		this.map = map;
		this.mapTileType = map.getMapSource().getTileImageType();
	}

	@Override
	public byte[] getTileData(int x, int y) throws IOException
	{
		log.trace("Reading tile x=" + x + " y=" + y);
		return tarIndex.getEntryContent(String.format(TILE_FILENAME_PATTERN, x, y));
	}

	@Override
	public BufferedImage getTileImage(int x, int y) throws IOException
	{
		byte[] unconvertedTileData = getTileData(x, y);
		if (unconvertedTileData == null)
			return null;
		return ImageIO.read(new ByteArrayInputStream(unconvertedTileData));
	}

	@Override
	public boolean preferTileImageUsage()
	{
		return false;
	}

	@Override
	public IfMapSource getMapSource()
	{
		return map.getMapSource();
	}
}
