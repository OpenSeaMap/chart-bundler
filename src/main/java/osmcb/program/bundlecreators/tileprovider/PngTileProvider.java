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

import osmb.mapsources.TileAddress;
import osmb.program.tiledatawriter.IfTileImageDataWriter;
import osmb.program.tiledatawriter.TileImagePngDataWriter;
import osmb.program.tiles.IfTileProvider;
import osmb.program.tiles.Tile;
import osmb.program.tiles.TileImageType;
import osmcb.utilities.OSMCBUtilities;

/**
 * A tile provider for bundle formats that only allow PNG images. Each image processed is checked
 */
public class PngTileProvider extends FilterTileProvider
{
	final IfTileImageDataWriter writer;

	public PngTileProvider(IfTileProvider tileProvider)
	{
		super(tileProvider);
		writer = new TileImagePngDataWriter();
		writer.initialize();
	}

	@Override
	public byte[] loadTileData(TileAddress tAddr)
	{
		byte[] data = null;
		try
		{
			{
				data = loadTileData(tAddr);
				if (OSMCBUtilities.getImageType(data) == TileImageType.PNG)
					return data;
			}
			ByteArrayOutputStream buffer = new ByteArrayOutputStream(32000);
			BufferedImage image = loadTileImage(tAddr);
			if (image != null)
			{
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
