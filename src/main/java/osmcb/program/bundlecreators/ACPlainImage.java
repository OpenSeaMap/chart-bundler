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

import java.awt.image.BufferedImage;
import java.io.File;

import osmb.program.map.IfLayer;
import osmb.program.map.IfMap;
import osmb.program.tiles.TileException;
import osmcb.program.bundle.BundleTestException;
import osmcb.program.bundle.IfBundle;
import osmcb.program.bundle.MapCreationException;

/**
 * This composes a plain tile store containing each map as a png-image
 * 
 * @author humbach
 *
 */
public abstract class ACPlainImage extends ACBundleCreator
{
	public ACPlainImage(IfBundle bundle, File bundleOutputDir)
	{
		super();
		init(bundle, bundleOutputDir);
	}

	// protected ACPlainImage(IfBundle bundle, IfLayer layer, File layerOutputDir)
	// {
	// super(bundle, layer, layerOutputDir);
	// }
	//
	// protected ACPlainImage(IfBundle bundle, IfLayer layer, IfMap map, File mapOutputDir)
	// {
	// super(bundle, layer, map, mapOutputDir);
	// }
	//
	/**
	 * @see osmcb.program.bundlecreators.ACBundleCreator#run()
	 */
	@Override
	public void run()
	{
	}

	/**
	 * @see osmcb.program.bundlecreators.ACBundleCreator#createInfoFile()
	 */
	@Override
	public void createInfoFile()
	{
		super.createInfoFile("OpenSeaMap Charts Plain Image Bundle 0.1\r\n");
	}

	@Override
	protected void testBundle() throws BundleTestException
	{
		Runtime r = Runtime.getRuntime();
		long heapMaxSize = r.maxMemory();
		int maxMapSize = (int) (Math.sqrt(heapMaxSize / 3d) * 0.8); // reduce
		// maximum
		// by 20%
		maxMapSize = (maxMapSize / 100) * 100; // round by 100;
		for (IfLayer layer : mBundle)
		{
			for (IfMap map : layer)
			{
				int w = map.getMaxPixelCoordinate().x - map.getMinPixelCoordinate().x;
				int h = map.getMaxPixelCoordinate().y - map.getMinPixelCoordinate().y;
				if (w > maxMapSize || h > maxMapSize)
					throw new BundleTestException("Map size too large for memory (is: " + Math.max(w, h) + " max:  " + maxMapSize + ")", map);
			}
		}
	}

	@Override
	public void createMap() throws MapCreationException, InterruptedException
	{
		try
		{
			createImage();
		}
		catch (InterruptedException e)
		{
			throw e;
		}
		catch (MapCreationException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new MapCreationException(mMap, e);
		}
	}

	/**
	 * @return maximum image height and width. In case an image is larger it
	 *         will be scaled to fit.
	 */
	protected int getMaxImageSize()
	{
		return Integer.MAX_VALUE;
	}

	protected int getBufferedImageType()
	{
		return BufferedImage.TYPE_4BYTE_ABGR;
	}

	protected void createImage() throws InterruptedException, MapCreationException, TileException
	{
		/*
		 * // bundleProgress.initMapCreation((xMax - xMin + 1) * (yMax - yMin +
		 * // 1));
		 * ImageIO.setUseCache(false);
		 * 
		 * int mapWidth = (mMap.getXMax() - mMap.getXMin() + 1) * sTileSize;
		 * int mapHeight = (mMap.getYMax() - mMap.getYMin() + 1) * sTileSize;
		 * 
		 * int maxImageSize = getMaxImageSize();
		 * int imageWidth = Math.min(maxImageSize, mapWidth);
		 * int imageHeight = Math.min(maxImageSize, mapHeight);
		 * 
		 * int len = Math.max(mapWidth, mapHeight);
		 * double scaleFactor = 1.0;
		 * boolean scaleImage = (len > maxImageSize);
		 * if (scaleImage)
		 * {
		 * scaleFactor = (double) getMaxImageSize() / (double) len;
		 * if (mapWidth != mapHeight)
		 * {
		 * // Map is not rectangle -> adapt height or width
		 * if (mapWidth > mapHeight)
		 * imageHeight = (int) (scaleFactor * mapHeight);
		 * else
		 * imageWidth = (int) (scaleFactor * mapWidth);
		 * }
		 * }
		 * if (imageHeight < 0 || imageWidth < 0)
		 * throw new MapCreationException("Invalid map size: (width/height: " + imageWidth + "/" + imageHeight + ")", mMap);
		 * long imageSize = 3l * (imageWidth) * (imageHeight);
		 * if (imageSize > Integer.MAX_VALUE)
		 * throw new MapCreationException("Map image too large: (width/height: " + imageWidth + "/" + imageHeight + ") - reduce the map size and try again", mMap);
		 * BufferedImage tileImage = OSMBUtilities.safeCreateBufferedImage(imageWidth, imageHeight, BufferedImage.TYPE_3BYTE_BGR);
		 * Graphics2D graphics = tileImage.createGraphics();
		 * try
		 * {
		 * if (scaleImage)
		 * {
		 * graphics.setTransform(AffineTransform.getScaleInstance(scaleFactor, scaleFactor));
		 * graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		 * }
		 * int lineY = 0;
		 * for (int y = mMap.getYMin(); y <= mMap.getYMax(); y++)
		 * {
		 * int lineX = 0;
		 * for (int x = mMap.getXMin(); x <= mMap.getXMax(); x++)
		 * {
		 * // bundleProgress.incMapCreationProgress();
		 * try
		 * {
		 * // byte[] sourceTileData = mapDlTileProvider.getTileData(x, y);
		 * byte[] sourceTileData = mMap.getMapSource().getTileData(mMap.getZoom(), x, y, null);
		 * if (sourceTileData != null)
		 * {
		 * BufferedImage tile = ImageIO.read(new ByteArrayInputStream(sourceTileData));
		 * graphics.drawImage(tile, lineX, lineY, sTileSize, sTileSize, Color.WHITE, null);
		 * }
		 * }
		 * catch (IOException e)
		 * {
		 * sLog.error("", e);
		 * }
		 * lineX += sTileSize;
		 * }
		 * lineY += sTileSize;
		 * }
		 * }
		 * finally
		 * {
		 * graphics.dispose();
		 * }
		 * writeTileImage(tileImage);
		 */ }

	protected abstract void writeTileImage(BufferedImage tileImage) throws MapCreationException;
}
