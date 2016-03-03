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

import java.io.File;
import java.io.IOException;

import osmb.mapsources.ACMapSource;
import osmb.program.tiles.Tile;
import osmb.program.tiles.TileLoader;
import osmb.program.tilestore.berkeleydb.TileDbEntry;
import osmb.utilities.OSMBStrs;
import osmcb.program.bundle.IfBundle;
import osmcb.program.bundle.MapCreationException;

@IfBundleCreatorName(value = "Tile store download only", type = "TILESTORE")
public class BCTileStoreDownload extends ACBundleCreator
{
	public BCTileStoreDownload()
	{
		super();
	}

	public BCTileStoreDownload(IfBundle bundle, File bundleOutputDir)
	{
		super();
		init(bundle, bundleOutputDir);
	}

	public BCTileStoreDownload(IfBundle bundle)
	{
		super();
		init(bundle, null);
	}

	@Override
	public boolean testMapSource(ACMapSource mapSource)
	{
		return true;
	}

	@Override
	protected void runMap()
	{
		log.trace(OSMBStrs.RStr("START") + " [" + Thread.currentThread().getName() + "], pool=" + mExec.toString());
		try
		{
			sTS.prepareTileStore(mMap.getMapSource());
			initializeMap();
			// load all necessary tiles. They should go directly into the tile store...
			loadMapTiles();
			// wait here for the loading of all tiles to finish
			mExec.shutdown();
			while (!mExec.isTerminated())
			{
				log.trace("running jobs=" + mExec.getCompletedTaskCount() + ", " + mExec.getActiveCount());
				Thread.sleep(1000);
			}
			log.debug("after shutdown(), completed tasks=" + mExec.getCompletedTaskCount() + ", total jobs=" + mExec.getTaskCount());
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (MapCreationException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			jobFinishedWithError(false);
		}
	}

	/**
	 * This loads the map tiles from the tile store. In any case it first retrieves the tile from the store.
	 * If there is no tile or the tile in the store is expired, it dispatches a new job to download the tile into the store.
	 * This procedure achieves:
	 * 1. The bundle creation does not stall at 'missing' tiles.
	 * 2. The tiles wanted are downloaded with priority into the tile store and will be used next time.
	 * 3. We can use a lot of threads to fetch tiles from the store, while not overburdening the download server(s) by limiting the
	 * number of threads which actually access the server.
	 */
	@Override
	public boolean loadMapTiles() throws Exception
	{
		log.trace(OSMBStrs.RStr("START"));
		log.debug("start map=" + mMap.getName() + ", TileStore=" + sTS);

		if (Thread.currentThread().isInterrupted())
		{
			log.debug("currentThread().isInterrupted()");
			throw new InterruptedException();
		}

		final int tileCount = (int) mMap.calculateTilesToLoad();

		try
		{
			log.debug("download tiles=" + tileCount);
			// sBundleProgress.initMapDownload(mMap);
			TileLoader tl = new TileLoader(this, sTC);

			for (int tileX = mMap.getMinTileCoordinate().x; tileX <= mMap.getMaxTileCoordinate().x; ++tileX)
			{
				for (int tileY = mMap.getMinTileCoordinate().y; tileY <= mMap.getMaxTileCoordinate().y; ++tileY)
				{
					log.debug("tiles=" + sScheduledTiles.incrementAndGet() + " of " + mBundle.calculateTilesToLoad());
					mExec.execute(tl.createTileLoaderJob(mMap.getMapSource(), tileX, tileY, mMap.getZoom()));
				}
			}
		}
		catch (Error e)
		{
			log.error("Error: " + e.getMessage(), e);
			throw e;
		}
		finally
		{
			// sBundleProgress.finishMapDownload(mMap);
		}
		return true;
	}

	@Override
	public void tileLoadingFinished(Tile tile, boolean success)
	{
		log.trace(OSMBStrs.RStr("START"));
		// old berkely tile store
		TileDbEntry tTSE = new TileDbEntry(tile.getXtile(), tile.getYtile(), tile.getZoom(), tile.getImage());
		sTS.putTile(tTSE, tile.getSource());
		sTC.addTile(tile);
		int nTiles = sDownloadedTiles.incrementAndGet();
		log.debug("tiles=" + nTiles + " of " + mBundle.calculateTilesToLoad());
		// info at 0.5% steps
		if (nTiles % (mBundle.calculateTilesToLoad() / 200) == 0)
			log.info("tiles=" + nTiles + " of " + mBundle.calculateTilesToLoad() + ", " + nTiles / (mBundle.calculateTilesToLoad() / 200) * 0.5 + "%");
		sBundleProgress.finishTileDownload(tile);
		if (!success)
			log.debug("tile=" + tile + " loaded=" + success);
		else
			log.trace("tile=" + tile + " loaded=" + success);
		// new SQLite tile store
		sNTS.putTile(tile, tile.getSource());
	}
}
