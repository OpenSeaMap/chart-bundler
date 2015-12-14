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
package osmcb.program;

import java.io.File;
import java.util.Properties;

import osmcb.utilities.OSMCBUtilities;

/**
 * Provides a default initialization of the common directories used within OpenSeaMap ChartBundler:
 * <ul>
 * <li>current directory</li>
 * <li>program directory</li>
 * <li>user home directory</li>
 * <li>user settings directory</li>
 * <li>temporary directory</li>
 * </ul>
 * 
 * Usually this information is changed when loading settings.xml succeeded. Therefore the program does not use these values directly, but rather by accessing
 * OSMCBSettings.getInstance()...
 */
public class DirectoryManager extends osmb.program.DirectoryManager
{
	@SuppressWarnings("unused") // W #unused
	private static Properties dirConfig = null;

	static
	{
		currentDir = new File(System.getProperty("user.dir"));
		userHomeDir = new File(System.getProperty("user.home"));
		programDir = getProgramDir();

		userAppDataDir = getUserAppDataDir();
		tempDir = new File(System.getProperty("java.io.tmpdir"));

		toolsDir = new File(programDir, "tools");
		userSettingsDir = programDir;

		catalogsDir = new File(programDir, "catalogs");
		mapSourcesDir = new File(programDir, "mapsources");
		tileStoreDir = new File(programDir, "tilestore");
		bundlesDir = new File(programDir, "bundles");
	}

	public static void initialize(File programDir)
	{

		if (currentDir == null || userAppDataDir == null || tempDir == null || programDir == null)
			throw new RuntimeException("DirectoryManager failed");
	}

	/**
	 * Returns the directory from which this java program is executed
	 * 
	 * @return
	 */
	private static File getProgramDir()
	{
		File f = null;
		try
		{
			// f = OSMCBUtilities.getClassLocation(DirectoryManager.class);
			f = OSMCBUtilities.getClassLocation(DirectoryManager.class);
		}
		catch (Exception e)
		{
			System.err.println(e.getMessage());
			return currentDir;
		}
		if ("bin".equals(f.getName())) // remove the bin dir -> this usually happens only in a development environment
			return f.getParentFile();
		else
			return f;
	}

	/**
	 * Returns the directory where the application saves it's settings. Should be overridden for any OS the app shall know about
	 * 
	 * Examples:
	 * <ul>
	 * <li>English Windows XP:<br>
	 * <tt>C:\Document and OSMCBSettings\%username%\Application Data\%appname%</tt>
	 * <li>Vista, W7, W8:<br>
	 * <tt>C:\Users\%username%\Application Data\%appname%</tt>
	 * <li>Linux:<br>
	 * <tt>/home/$username$/.%appname%</tt></li>
	 * </ul>
	 * 
	 * @return
	 */
	private static File getUserAppDataDir()
	{
		String appData = System.getenv("APPDATA");
		if (appData != null)
		{
			// seems to be Windows
			File appDataDir = new File(appData);
			if (appDataDir.isDirectory())
			{
				//
				File osmcbDataDir = new File(appData, "OSeaM ChartBundler");
				if (!osmcbDataDir.exists() && !osmcbDataDir.mkdir())
					throw new RuntimeException("Unable to create directory \"" + osmcbDataDir.getAbsolutePath() + "\"");
				return osmcbDataDir;
			}
		}
		else
		{
			// seems to be Linux
			File userDir = new File(System.getProperty("user.home"));
			File osmcbDataDir = new File(userDir, ".osmcb");
			if (!osmcbDataDir.exists() && !osmcbDataDir.mkdir())
				throw new RuntimeException("Unable to create directory \"" + osmcbDataDir.getAbsolutePath() + "\"");
			return osmcbDataDir;
		}
		return null;
	}

	private DirectoryManager() {
		super();
	}
}
