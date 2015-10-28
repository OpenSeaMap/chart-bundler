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
package osmcb.program.bundlecreators.TrekBuddy;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.imageio.ImageIO;

import osmb.mapsources.IfMapSource;
import osmb.mapsources.IfMapSource.LoadMethod;
import osmb.mapsources.mapspace.MercatorPower2MapSpace;
import osmb.program.ACApp;
import osmb.program.map.IfLayer;
import osmb.program.map.IfMapSpace;
import osmb.program.map.IfMapSpace.ProjectionCategory;
import osmb.program.tiles.TileException;
import osmb.utilities.geo.GeoUtils;
import osmcb.OSMCBSettings;
import osmcb.program.bundle.BundleTestException;
import osmcb.program.bundle.IfBundle;
import osmcb.program.bundle.MapCreationException;
import osmcb.program.bundlecreators.ACBundleCreator;
import osmcb.program.bundlecreators.IfBundleCreatorName;
import osmcb.program.bundlecreators.IfMapTileWriter;
import osmcb.utilities.OSMCBUtilities;

@IfBundleCreatorName(value = "TrekBuddy untared bundle", type = "UntaredAtlas")
// @SupportedTIParameters(names = {Name.format, Name.height, Name.width})
public class BCTrekBuddy extends ACBundleCreator
{
	protected static final String FILENAME_PATTERN = "t_%d_%d.%s";
	// protected File layerDir = null;
	// protected File mapDir = null;
	protected IfMapTileWriter mapTileWriter;

	public BCTrekBuddy()
	{
		super();
	}

	public BCTrekBuddy(IfBundle bundle, File bundleOutputDir)
	{
		super();
		init(bundle, bundleOutputDir);
	}

	// protected BCTrekBuddy(IfBundle bundle, IfLayer layer, File layerOutputDir)
	// {
	// super(bundle, layer, layerOutputDir);
	// }
	//
	// protected BCTrekBuddy(IfBundle bundle, IfLayer layer, IfMap map, File mapOutputDir)
	// {
	// super(bundle, layer, map, mapOutputDir);
	// }
	//
	@Override
	public boolean testMapSource(IfMapSource mapSource)
	{
		IfMapSpace mapSpace = mapSource.getMapSpace();
		return (mapSpace instanceof MercatorPower2MapSpace && ProjectionCategory.SPHERE.equals(mapSpace.getProjectionCategory()));
		// TODO supports Mercator ellipsoid?
	}

	@Override
	public void initializeBundle() throws IOException, BundleTestException
	{
		File bundleOutputDir = ((OSMCBSettings) ACApp.getApp().getSettings()).getChartBundleOutputDirectory();
		bundleOutputDir = new File(bundleOutputDir, "TrekBuddy-Atlas");
		OSMCBUtilities.mkDirs(bundleOutputDir);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-hhmmss");
		String bundleDirName = "OSM-TrekBuddy-" + mBundle.getName() + "-" + sdf.format(new Date());
		bundleOutputDir = new File(bundleOutputDir, bundleDirName);
		super.initializeBundle(bundleOutputDir);
	}

	/**
	 * Originally with the default name 'cr' aka 'czech republic' as Kruch used it,
	 * This name should in fact be a reasonable name for the bundle. Thus said we use the bundles name for the packed file.
	 */
	@Override
	public void finishBundle()
	{
		createAtlasTbaFile(mBundle.getName());
	}

	// public void initializeMap(TileProvider mapTileProvider)

	/*
	 * (non-Javadoc)
	 * 
	 * @see osmcb.program.bundlecreators.ACBundleCreator#initializeLayer()
	 */
	@Override
	public void initializeLayer() throws IOException, InterruptedException
	{
		log.trace("initializeLayer() called, layer='" + mLayer.getName() + "', dir='" + mOutputDir);
		mOutputDir = new File(mOutputDir, mLayer.getName());
		OSMCBUtilities.mkDirs(mOutputDir);
		super.initializeLayer();
	}

	@Override
	public void initializeMap() throws IOException
	{
		try
		{
			super.initializeMap();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		IfLayer layer = mMap.getLayer();
		mOutputDir = new File(mOutputDir, mMap.getName());
		OSMCBUtilities.mkDirs(mOutputDir);
	}

	protected void writeMapFile() throws IOException
	{
		File mapFile = new File(mOutputDir, mMap.getName() + ".map");
		FileOutputStream mapFileStream = null;
		try
		{
			mapFileStream = new FileOutputStream(mapFile);
			writeMapFile(mapFileStream);
		}
		finally
		{
			OSMCBUtilities.closeStream(mapFileStream);
		}
	}

	protected void writeMapFile(OutputStream stream) throws IOException
	{
		writeMapFile("t_." + mMap.getMapSource().getTileImageType().getFileExt(), stream);
	}

	protected void writeMapFile(String imageFileName, OutputStream stream) throws IOException
	{
		log.trace("Writing map file");
		OutputStreamWriter mapWriter = new OutputStreamWriter(stream, TEXT_FILE_CHARSET);

		IfMapSpace mapSpace = mMap.getMapSource().getMapSpace();

		double longitudeMin = mapSpace.cXToLon(mMap.getXMin() * tileSize, mMap.getZoom());
		double longitudeMax = mapSpace.cXToLon((mMap.getXMax() + 1) * tileSize - 1, mMap.getZoom());
		double latitudeMin = mapSpace.cYToLat((mMap.getYMax() + 1) * tileSize - 1, mMap.getZoom());
		double latitudeMax = mapSpace.cYToLat(mMap.getYMin() * tileSize, mMap.getZoom());

		int width = (mMap.getXMax() - mMap.getXMin() + 1) * tileSize;
		int height = (mMap.getYMax() - mMap.getYMin() + 1) * tileSize;

		mapWriter.write(prepareMapString(imageFileName, longitudeMin, longitudeMax, latitudeMin, latitudeMax, width, height));
		mapWriter.flush();
	}

	@Override
	public void createMap() throws MapCreationException, InterruptedException
	{
		try
		{
			log.debug("Creating map");
			// OSMCBUtilities.mkDirs(mMapDir);

			// write the .map file containing the calibration points
			writeMapFile();

			// This means there should not be any resizing of the tiles.
			mapTileWriter = createMapTileWriter();

			createTiles();

			mapTileWriter.finalizeMap();
		}
		catch (MapCreationException e)
		{
			throw e;
		}
		catch (InterruptedException e)
		{
			throw e;
		}
		catch (Exception e)
		{
			throw new MapCreationException(mMap, e);
		}
	}

	protected IfMapTileWriter createMapTileWriter() throws IOException
	{
		return new FileTileWriter();
	}

	protected void createTiles() throws InterruptedException, MapCreationException
	{
		int tilex = 0;
		int tiley = 0;

		// bundleProgress.initMapCreation((xMax - xMin + 1) * (yMax - yMin + 1));

		ImageIO.setUseCache(false);
		byte[] emptyTileData = OSMCBUtilities.createEmptyTileData(mMap.getMapSource());
		String tileType = mMap.getMapSource().getTileImageType().getFileExt();
		for (int x = mMap.getXMin() / 256; x <= mMap.getXMax() / 256; x++)
		{
			tiley = 0;
			for (int y = mMap.getYMin() / 256; y <= mMap.getYMax() / 256; y++)
			{
				// checkUserAbort();
				// bundleProgress.incMapCreationProgress();
				try
				{
					// byte[] sourceTileData = mapDlTileProvider.getTileData(x, y);
					byte[] sourceTileData = mMap.getMapSource().getTileData(mMap.getZoom(), x, y, LoadMethod.DEFAULT);
					if (sourceTileData != null)
					{
						mapTileWriter.writeTile(tilex, tiley, tileType, sourceTileData);
					}
					else
					{
						log.trace(String.format("Tile x=%d y=%d not found in tile archive - creating default", tilex, tiley));
						mapTileWriter.writeTile(tilex, tiley, tileType, emptyTileData);
					}
				}
				catch (IOException | TileException e)
				{
					throw new MapCreationException("Error writing tile image: " + e.getMessage(), mMap, e);
				}
				tiley++;
			}
			tilex++;
		}
	}

	private class FileTileWriter implements IfMapTileWriter
	{
		File setFolder;
		Writer setFileWriter;

		int tileHeight = 256;
		int tileWidth = 256;

		public FileTileWriter() throws IOException
		{
			super();
			setFolder = new File(mOutputDir, "set");
			OSMCBUtilities.mkDir(setFolder);
			log.debug("Writing tiles to set folder: " + setFolder);
			File setFile = new File(mOutputDir, mMap.getName() + ".set");
			if (parameters != null)
			{
				tileHeight = parameters.getHeight();
				tileWidth = parameters.getWidth();
			}
			try
			{
				setFileWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(setFile), TEXT_FILE_CHARSET));
			}
			catch (IOException e)
			{
				log.error("", e);
			}
		}

		@Override
		public void writeTile(int tilex, int tiley, String imageFormat, byte[] tileData) throws IOException
		{
			String tileFileName = String.format(FILENAME_PATTERN, (tilex * tileWidth), (tiley * tileHeight), imageFormat);

			File f = new File(setFolder, tileFileName);
			FileOutputStream out = new FileOutputStream(f);
			setFileWriter.write(tileFileName + "\r\n");
			try
			{
				out.write(tileData);
			}
			finally
			{
				OSMCBUtilities.closeStream(out);
			}
		}

		@Override
		public void finalizeMap()
		{
			try
			{
				setFileWriter.flush();
			}
			catch (IOException e)
			{
				log.error("", e);
			}
			OSMCBUtilities.closeWriter(setFileWriter);
		}
	}

	protected String prepareMapString(String fileName, double lonMin, double lonMax, double latMin, double latMax, int width, int height)
	{
		StringBuffer sbMap = new StringBuffer();

		sbMap.append("OziExplorer Map Data File Version 2.2\r\n");
		sbMap.append(fileName + "\r\n");
		sbMap.append(fileName + "\r\n");
		sbMap.append("1 ,Map Code,\r\n");
		sbMap.append("WGS 84, WGS 84, 0.0000, 0.0000, WGS 84\r\n");
		sbMap.append("Reserved 1\r\n");
		sbMap.append("Reserved 2\r\n");
		sbMap.append("Magnetic Variation,,,E\r\n");
		sbMap.append("Map Projection, Mercator, PolyCal, No, AutoCalOnly, No, BSBUseWPX, No\r\n");

		String strLatMax = GeoUtils.getDegMinFormat(latMax, true);
		String strLatMin = GeoUtils.getDegMinFormat(latMin, true);
		String strLonMax = GeoUtils.getDegMinFormat(lonMax, false);
		String strLonMin = GeoUtils.getDegMinFormat(lonMin, false);

		String pointLine = "Point%02d,xy, %4s, %4s,in, deg, %1s, %1s, grid, , , ,N\r\n";

		sbMap.append(String.format(pointLine, 1, 0, 0, strLatMax, strLonMin));
		sbMap.append(String.format(pointLine, 2, width - 1, 0, strLatMax, strLonMax));
		sbMap.append(String.format(pointLine, 3, width - 1, height - 1, strLatMin, strLonMax));
		sbMap.append(String.format(pointLine, 4, 0, height - 1, strLatMin, strLonMin));

		for (int i = 5; i <= 30; i++)
		{
			String s = String.format(pointLine, i, "", "", "", "");
			sbMap.append(s);
		}
		sbMap.append("Projection Setup,,,,,,,,,,\r\n");
		sbMap.append("Map Feature = MF ; Map Comment = MC     These follow if they exist\r\n");
		sbMap.append("Track File = TF      These follow if they exist\r\n");
		sbMap.append("Moving Map Parameters = MM?    These follow if they exist\r\n");

		sbMap.append("MM0, Yes\r\n");
		sbMap.append("MMPNUM, 4\r\n");

		String mmpxLine = "MMPXY, %d, %5d, %5d\r\n";

		sbMap.append(String.format(mmpxLine, 1, 0, 0));
		sbMap.append(String.format(mmpxLine, 2, width - 1, 0));
		sbMap.append(String.format(mmpxLine, 3, width - 1, height - 1));
		sbMap.append(String.format(mmpxLine, 4, 0, height - 1));

		String mpllLine = "MMPLL, %d, %2.6f, %2.6f\r\n";

		sbMap.append(String.format(Locale.ENGLISH, mpllLine, 1, lonMin, latMax));
		sbMap.append(String.format(Locale.ENGLISH, mpllLine, 2, lonMax, latMax));
		sbMap.append(String.format(Locale.ENGLISH, mpllLine, 3, lonMax, latMin));
		sbMap.append(String.format(Locale.ENGLISH, mpllLine, 4, lonMin, latMin));

		sbMap.append("MOP, Map Open Position, 0, 0\r\n");

		// The simple variant for calculating mm1b
		// http://www.trekbuddy.net/forum/viewtopic.php?t=3755&postdays=0&postorder=asc&start=286
		double mm1b = (lonMax - lonMin) * 111319;
		mm1b *= Math.cos(Math.toRadians((latMax + latMin) / 2.0)) / width;

		sbMap.append(String.format(Locale.ENGLISH, "MM1B, %2.6f\r\n", mm1b));

		sbMap.append("IWH, Map Image Width/Height, " + width + ", " + height + "\r\n");

		return sbMap.toString();
	}

	public void createAtlasTbaFile(String name)
	{
		File crtba = new File(mOutputDir.getAbsolutePath(), name + ".tba");
		try
		{
			FileWriter fw = new FileWriter(crtba);
			fw.write("Bundle 1.0\r\n");
			fw.close();
		}
		catch (IOException e)
		{
			log.error("", e);
		}
	}

	@Override
	public void createInfoFile()
	{
		// Currently do nothing for this output format. Check with Kruch, if some description is possible within TrekBuddy atlas format
	}
}