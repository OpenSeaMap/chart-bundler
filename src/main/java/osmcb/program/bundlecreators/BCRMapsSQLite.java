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
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import osmb.mapsources.IfMapSource;
import osmb.program.ACApp;
import osmb.program.map.IfMap;
import osmb.program.map.IfMapSpace;
import osmb.program.map.IfMapSpace.ProjectionCategory;
import osmb.program.tiles.SQLiteLoader;
import osmb.program.tiles.TileImageParameters;
import osmcb.OSMCBSettings;
import osmcb.program.bundle.BundleTestException;
import osmcb.program.bundle.IfBundle;
import osmcb.program.bundle.MapCreationException;
import osmcb.program.bundlecreators.tileprovider.ConvertedRawTileProvider;
import osmcb.utilities.OSMCBUtilities;

/**
 * Bundle/Map creator for "BigPlanet-Maps application for Android" (offline SQLite maps) http://code.google.com/p/bigplanet/
 * <p>
 * Some source parts are taken from the "android-map.blogspot.com Version of MobAC": http://code.google.com/p/android-map/
 * </p>
 * <p>
 * Additionally the created BigPlanet SQLite database has one additional table containing special info needed by the Android application <a
 * href="http://robertdeveloper.blogspot.com/search/label/rmaps.release" >RMaps</a>.<br>
 * (Database statements: {@link #RMAPS_TABLE_INFO_DDL} and {@link #RMAPS_UPDATE_INFO_SQL} ).<br>
 * Changes made by <a href="mailto:robertk506@gmail.com">Robert</a>, author of RMaps.
 * <p>
 */
@BundleCreatorName(value = "RMaps SQLite DB", type = "RMaps")
// @SupportedTIParameters(names = {Name.format})
public class BCRMapsSQLite extends ACBundleCreator implements IfRequiresSQLite
{
	private static final int MAX_BATCH_SIZE = 1000;
	private static final String TABLE_DDL = "CREATE TABLE IF NOT EXISTS tiles (x int, y int, z int, s int, image blob, PRIMARY KEY (x,y,z,s))";
	private static final String INDEX_DDL = "CREATE INDEX IF NOT EXISTS IND on tiles (x,y,z,s)";
	private static final String INSERT_SQL = "INSERT or REPLACE INTO tiles (x,y,z,s,image) VALUES (?,?,?,0,?)";
	private static final String RMAPS_TABLE_INFO_DDL = "CREATE TABLE IF NOT EXISTS info AS SELECT 99 AS minzoom, 0 AS maxzoom";
	private static final String RMAPS_CLEAR_INFO_SQL = "DELETE FROM info;";
	private static final String RMAPS_UPDATE_INFO_MINMAX_SQL = "INSERT INTO info (minzoom,maxzoom) VALUES (?,?);";
	private static final String RMAPS_INFO_MAX_SQL = "SELECT DISTINCT z FROM tiles ORDER BY z DESC LIMIT 1;";
	private static final String RMAPS_INFO_MIN_SQL = "SELECT DISTINCT z FROM tiles ORDER BY z ASC LIMIT 1;";
	protected File databaseFile;
	protected Connection conn = null;
	protected PreparedStatement prepStmt;

	public BCRMapsSQLite()
	{
		super();
		SQLiteLoader.loadSQLiteOrShowError();
	}

	@Override
	public boolean testMapSource(IfMapSource mapSource)
	{
		IfMapSpace mapSpace = mapSource.getMapSpace();
		boolean correctTileSize = (256 == mapSpace.getTileSize());
		ProjectionCategory pc = mapSpace.getProjectionCategory();
		boolean correctProjection = (ProjectionCategory.SPHERE.equals(pc) || ProjectionCategory.ELLIPSOID.equals(pc));
		return correctTileSize && correctProjection;
	}

	@Override
	public void initializeBundle(IfBundle bundle, File customBundleDir) throws IOException, BundleTestException
	{
		File bundleOutputDir = ((OSMCBSettings) ACApp.getApp().getSettings()).getChartBundleOutputDirectory();
		bundleOutputDir = new File(bundleOutputDir, "OsmAnd-sqlitedb");
		OSMCBUtilities.mkDirs(bundleOutputDir);

		// if (customBundleDir == null)
		{
			SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd-hhmmss");
			String bundleDirName = "OSM(OsmAnd-sqlitedb)-" + bundle.getName() + "-" + sdf.format(new Date());
			customBundleDir = new File(bundleOutputDir, bundleDirName);
		}
		super.initializeBundle(bundle, customBundleDir);
		databaseFile = new File(bundleDir, getDatabaseFileName());
		log.debug("SQLite Database file: " + databaseFile);
	}

	@Override
	public void createMap(IfMap map) throws MapCreationException, InterruptedException
	{
		try
		{
			OSMCBUtilities.mkDir(bundleDir);
		}
		catch (IOException e)
		{
			throw new MapCreationException(map, e);
		}
		try
		{
			SQLiteLoader.loadSQLite();
		}
		catch (SQLException e)
		{
			throw new MapCreationException(SQLiteLoader.getMsgSqliteMissing(), map, e);
		}
		try
		{
			openConnection();
			initializeDB();
			createTiles();
		}
		catch (SQLException e)
		{
			throw new MapCreationException("Error creating SQL database \"" + databaseFile + "\": " + e.getMessage(), map, e);
		}
	}

	protected void openConnection() throws SQLException
	{
		if (conn == null || conn.isClosed())
		{
			String url = "jdbc:sqlite:/" + databaseFile.getAbsolutePath();
			conn = DriverManager.getConnection(url);
		}
	}

	@Override
	public void abortBundleCreation() throws IOException
	{
		SQLiteLoader.closeConnection(conn);
		conn = null;
		super.abortBundleCreation();
	}

	@Override
	public void finishBundle(IfBundle bundle)
	{
		SQLiteLoader.closeConnection(conn);
		conn = null;
		super.finishBundle(bundle);
	}

	protected void initializeDB() throws SQLException
	{
		Statement stat = conn.createStatement();
		stat.executeUpdate(TABLE_DDL);
		stat.executeUpdate(INDEX_DDL);
		createInfoTable(stat);

		stat.executeUpdate("CREATE TABLE IF NOT EXISTS android_metadata (locale TEXT)");
		if (!(stat.executeQuery("SELECT * FROM android_metadata").next()))
		{
			String locale = Locale.getDefault().toString();
			stat.executeUpdate("INSERT INTO android_metadata VALUES ('" + locale + "')");
		}
		stat.close();
	}

	protected void createInfoTable(Statement stat) throws SQLException
	{
		stat.executeUpdate(RMAPS_TABLE_INFO_DDL);
	}

	protected void createTiles() throws InterruptedException, MapCreationException
	{
		int maxMapProgress = 2 * (xMax - xMin + 1) * (yMax - yMin + 1);
		// bundleProgress.initMapCreation(maxMapProgress);
		TileImageParameters param = map.getParameters();
		if (param != null)
			mapDlTileProvider = new ConvertedRawTileProvider(mapDlTileProvider, param.getFormat());
		try
		{
			conn.setAutoCommit(false);
			int batchTileCount = 0;
			int tilesWritten = 0;
			Runtime r = Runtime.getRuntime();
			long heapMaxSize = r.maxMemory();
			prepStmt = conn.prepareStatement(getTileInsertSQL());
			for (int x = xMin; x <= xMax; x++)
			{
				for (int y = yMin; y <= yMax; y++)
				{
					checkUserAbort();
					// bundleProgress.incMapCreationProgress();
					try
					{
						byte[] sourceTileData = mapDlTileProvider.getTileData(x, y);
						if (sourceTileData != null)
						{
							writeTile(x, y, zoom, sourceTileData);
							tilesWritten++;
							long heapAvailable = heapMaxSize - r.totalMemory() + r.freeMemory();

							batchTileCount++;
							if ((heapAvailable < HEAP_MIN) || (batchTileCount >= MAX_BATCH_SIZE))
							{
								log.trace("Executing batch containing " + batchTileCount + " tiles");
								prepStmt.executeBatch();
								prepStmt.clearBatch();
								System.gc();
								conn.commit();
								// bundleProgress.incMapCreationProgress(batchTileCount);
								batchTileCount = 0;
							}
						}
					}
					catch (IOException e)
					{
						throw new MapCreationException(map, e);
					}
				}
			}
			prepStmt.executeBatch();
			prepStmt.clearBatch();
			System.gc();
			if (tilesWritten > 0)
				updateTileMetaInfo();
			log.trace("Final commit containing " + batchTileCount + " tiles");
			conn.commit();
			// bundleProgress.setMapCreationProgress(maxMapProgress);
		}
		catch (SQLException e)
		{
			throw new MapCreationException(map, e);
		}
	}

	protected void updateTileMetaInfo() throws SQLException
	{
		Statement stat = conn.createStatement();
		ResultSet rs = stat.executeQuery(RMAPS_INFO_MAX_SQL);
		if (!rs.next())
			throw new SQLException("failed to retrieve max tile zoom info");
		int max = rs.getInt(1);
		rs.close();
		rs = stat.executeQuery(RMAPS_INFO_MIN_SQL);
		if (!rs.next())
			throw new SQLException("failed to retrieve min tile zoom info");
		int min = rs.getInt(1);
		rs.close();
		PreparedStatement ps = conn.prepareStatement(RMAPS_UPDATE_INFO_MINMAX_SQL);
		ps.setInt(1, min);
		ps.setInt(2, max);

		stat.execute(RMAPS_CLEAR_INFO_SQL);
		ps.execute();
		stat.close();
		ps.close();
	}

	protected void writeTile(int x, int y, int z, byte[] tileData) throws SQLException, IOException
	{
		prepStmt.setInt(1, x);
		prepStmt.setInt(2, y);
		prepStmt.setInt(3, 17 - z);
		prepStmt.setBytes(4, tileData);
		prepStmt.addBatch();
	}

	protected String getDatabaseFileName()
	{
		return bundle.getName() + ".sqlitedb";
	}

	protected String getTileInsertSQL()
	{
		return INSERT_SQL;
	}

	@Override
	public void createInfoFile()
	{
		// TODO Auto-generated method stub

	}
}
