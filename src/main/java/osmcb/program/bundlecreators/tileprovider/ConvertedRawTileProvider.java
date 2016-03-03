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
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import osmb.mapsources.TileAddress;
import osmb.program.tiledatawriter.IfTileImageDataWriter;
import osmb.program.tiles.IfTileProvider;
import osmb.program.tiles.Tile;
import osmb.program.tiles.TileImageFormat;

/**
 * Loads a tile from the underlying {@link IfTileProvider}, loads the tile to memory, converts it to the desired {@link TileImageFormat} and returns the binary
 * representation of the image in the specified format.
 */
public class ConvertedRawTileProvider extends FilterTileProvider
{
	private IfTileImageDataWriter writer;

	public ConvertedRawTileProvider(IfTileProvider tileProvider, TileImageFormat tileImageFormat)
	{
		super(tileProvider);
		writer = tileImageFormat.getDataWriter();
		writer.initialize();
		ImageIO.setUseCache(false);
	}

	@Override
	public byte[] loadTileData(TileAddress tAddr)
	{
		byte[] data = null;
		BufferedImage image = loadTileImage(tAddr);
		try
		{
			if (image != null)
			{
				ByteArrayOutputStream buffer = new ByteArrayOutputStream(32000);
				writer.processImage(image, buffer);
				data = buffer.toByteArray();
			}
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return data;
	}

	@Override
	public BufferedImage loadTileImage(TileAddress tAddr)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Tile loadTile(TileAddress tAddr)
	{
		// TODO Auto-generated method stub
		return null;
	}

	// @Override
	// public boolean preferTileImageUsage()
	// {
	// return true;
	// }
}
