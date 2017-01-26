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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;

import osmb.program.ACApp;
import osmcb.OSMCBSettings;
import osmcb.program.bundle.BundleTestException;
import osmcb.program.bundle.IfBundle;
import osmcb.program.bundlecreators.IfBundleCreatorName;
import osmcb.program.bundlecreators.IfMapTileWriter;
import osmcb.utilities.OSMCBUtilities;
import osmcb.utilities.tar.TarArchive;
import osmcb.utilities.tar.TarTmiArchive;

@IfBundleCreatorName(value = "TrekBuddy tared bundle", type = "TaredAtlas")
public class BCTrekBuddyTared extends BCTrekBuddy
{
	public BCTrekBuddyTared()
	{
		super();
		log = Logger.getLogger(this.getClass());
	}

	public BCTrekBuddyTared(IfBundle bundle, File bundleOutputDir)
	{
		super(bundle, bundleOutputDir);
	}

	// protected BCTrekBuddyTared(IfBundle bundle, IfLayer layer, File layerOutputDir)
	// {
	// super(bundle, layer, layerOutputDir);
	// }
	//
	// protected BCTrekBuddyTared(IfBundle bundle, IfLayer layer, IfMap map, File mapOutputDir)
	// {
	// super(bundle, layer, map, mapOutputDir);
	// }
	//
	@Override
	public void initializeBundle() throws IOException, BundleTestException
	{
		File bundleOutputDir = ((OSMCBSettings) ACApp.getApp().getSettings()).getChartBundleOutputDirectory();
		bundleOutputDir = new File(bundleOutputDir, "TrekBuddy-TAR");
		OSMCBUtilities.mkDirs(bundleOutputDir);
		SimpleDateFormat sdf = new SimpleDateFormat(STR_BUFMT);
		String bundleDirName = "OSM-TrekBuddyTar-" + mBundle.getName() + "-" + sdf.format(new Date());
		bundleOutputDir = new File(bundleOutputDir, bundleDirName);
		super.initializeBundle(bundleOutputDir);
	}

	@Override
	public void finishBundle()
	{
		createInfoFile();
		createAtlasTarArchive(mBundle.getName());
		sBundleProgress.finishBundle();
		log.info("bundle='" + mBundle.getName() + "' finished");
	}

	private void createAtlasTarArchive(String name)
	{
		log.trace("Creating " + name + ".tar for bundle in dir \"" + mOutputDir.getPath() + "\"");

		File[] atlasLayerDirs = OSMCBUtilities.listSubDirectories(mOutputDir);
		List<File> atlasMapDirs = new LinkedList<File>();
		for (File dir : atlasLayerDirs)
			OSMCBUtilities.addSubDirectories(atlasMapDirs, dir, 0);

		TarArchive ta = null;
		File crFile = new File(mOutputDir, name + ".tar");
		try
		{
			ta = new TarArchive(crFile, mOutputDir);

			ta.writeFileFromData(name + ".tba", "Bundle 1.0\r\n".getBytes());

			for (File mapDir : atlasMapDirs)
			{
				ta.writeFile(mapDir);
				File mapFile = new File(mapDir, mapDir.getName() + ".map");
				ta.writeFile(mapFile);
				try
				{
					mapFile.delete();
				}
				catch (Exception e)
				{
				}
			}
			ta.writeEndOfArchive();
		}
		catch (IOException e)
		{
			log.error("Failed writing tar file \"" + crFile.getPath() + "\"", e);
		}
		finally
		{
			if (ta != null)
				ta.close();
		}
	}

	protected IfMapTileWriter createMapTileWriter() throws IOException
	{
		return new TarTileWriter();
	}

	private class TarTileWriter implements IfMapTileWriter
	{

		TarArchive ta = null;
		int tileHeight = 256;
		int tileWidth = 256;

		public TarTileWriter()
		{
			super();
			if (parameters != null)
			{
				tileHeight = parameters.getHeight();
				tileWidth = parameters.getWidth();
			}
			File mapTarFile = new File(mOutputDir, mMap.getName() + ".tar");
			log.debug("Writing tiles to tared map: " + mapTarFile);
			try
			{
				ta = new TarTmiArchive(mapTarFile, null);
				ByteArrayOutputStream buf = new ByteArrayOutputStream(8192);
				writeMapFile(buf);
				ta.writeFileFromData(mMap.getName() + ".map", buf.toByteArray());
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

			ta.writeFileFromData("set/" + tileFileName, tileData);
		}

		@Override
		public void finalizeMap()
		{
			try
			{
				ta.writeEndOfArchive();
			}
			catch (IOException e)
			{
				log.error("", e);
			}
			ta.close();
		}
	}
}
