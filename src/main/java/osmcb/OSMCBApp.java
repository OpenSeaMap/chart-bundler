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
package osmcb;

import java.io.File;

import javax.imageio.ImageIO;
import javax.xml.bind.JAXBException;

import osmb.mapsources.DefaultMapSourcesManager;
import osmb.program.ACConsoleApp;
import osmb.program.ACSettings;
import osmb.program.EnvironmentSetup;
import osmb.program.JArgs.Option.StringOption;
import osmb.program.JArgs.OptionException;
import osmb.program.catalog.Catalog;
import osmb.program.catalog.IfCatalog;
import osmb.program.tilestore.ACSiTileStore;
import osmb.utilities.GUIExceptionHandler;
import osmcb.program.Logging;
import osmcb.program.ProgramInfo;
import osmcb.program.bundle.Bundle;
import osmcb.program.bundle.BundleOutputFormat;
import osmcb.program.bundle.BundleThread;

/**
 * main starter class
 */
public class OSMCBApp extends ACConsoleApp
{
	static public OSMCBApp getApp()
	{
		return (OSMCBApp) gApp;
	}

	@Override
	public ACSettings getSettings()
	{
		if (pSets == null)
			pSets = OSMCBSettings.getInstance();
		return pSets;
	}

	public OSMCBApp()
	{
		try
		{
			gApp = this;
		}
		catch (Throwable t)
		{
			System.exit(1);
		}
	}

	@Override
	public int runWork()
	{
		try
		{
			OSMCBRsc.initializeRSTRs();
			findProgramDir();
			parseCommandLine();

			// DirectoryManager.initialize();
			Logging.configureLogging(this);

			// MySocketImplFactory.install();
			ProgramInfo.initialize(); // Load revision info
			// Logging.logSystemInfo();
			// Logging.logSystemProperties();
			ImageIO.setUseCache(false);

			EnvironmentSetup.checkSettingsSetup();
			pSets = OSMCBSettings.loadOrQuit();
			EnvironmentSetup.checkMemory();
			Logging.logSystemInfo();

			// EnvironmentSetup.copyMapPacks();
			DefaultMapSourcesManager.initialize();
			ACSiTileStore.initialize();
			EnvironmentSetup.upgrade();
			Logging.logSystemInfo();
			runWithoutMainGUI();
			Thread.sleep(20000);
			return 0;
		}
		catch (Throwable t)
		{
			return -1;
		}
	}

	private void runWithoutMainGUI()
	{
		if (true)
		{
			try
			{
				createBundle(mCmdlParser.getOptionValue(new StringOption('c', "create"), "OSM-Std"),
						mCmdlParser.getOptionValue(new StringOption('f', "format"), "OpenCPN-KAP"));
			}
			catch (Exception e)
			{
				GUIExceptionHandler.processException(e);
			}
		}
	}

	public void createBundle(String catalogName, String strBundleFormat)
	{
		try
		{
			IfCatalog cat = Catalog.load(new File(ACSettings.getInstance().getCatalogsDirectory(), Catalog.getCatalogFileName(catalogName)));
			Bundle bundle = new Bundle(cat, BundleOutputFormat.getFormatByName(strBundleFormat));
			BundleThread bundleThread = new BundleThread(bundle);
			if (pSets.getChartBundleOutputDirectory() != null)
				bundleThread.setCustomBundleDir(pSets.getChartBundleOutputDirectory());
			bundleThread.setQuitOsmcbAfterBundleCreation(true);
			bundleThread.start();
		}
		catch (Exception e)
		{
			// System.err.println("Error loading catalog \"" + catalogName + "\".");
			GUIExceptionHandler.processException(e);
		}
	}

	/**
	 * the commandline gets parsed for the following: -c[reate] create a specified bundle and exit after creation -d[irectory] directory with settings.xml run as
	 * service/daemon -h[elp] shows a short help file
	 */
	@Override
	// protected void parseCommandLine()
	// {
	// if ((ARGS != null) && (ARGS.length >= 2))
	// {
	// if (OSMCBStrs.RStr("OSMCBApp.create").equalsIgnoreCase(ARGS[0]) || OSMCBStrs.RStr("OSMCBApp.c").equalsIgnoreCase(ARGS[0]))
	// {
	// if (ARGS.length > 2)
	// cmdl = new CmdlCreateBundle(ARGS[1], ARGS[2]);
	// else
	// cmdl = new CmdlCreateBundle(ARGS[1]);
	// return;
	// }
	// if (OSMCBStrs.RStr("OSMCBApp.format").equalsIgnoreCase(ARGS[0]) || OSMCBStrs.RStr("OSMCBApp.f").equalsIgnoreCase(ARGS[0]))
	// {
	// if (ARGS.length > 2)
	// cmdl = new CmdlCreateBundle(ARGS[1], ARGS[2]);
	// else
	// cmdl = new CmdlCreateBundle(ARGS[1]);
	// return;
	// }
	// }
	// }
	protected void parseCommandLine()
	{
		mCmdlParser = getCmdlParser();
		StringOption optCreate = new StringOption('c', "create");
		mCmdlParser.addOption(optCreate);
		StringOption optFormat = new StringOption('f', "format");
		mCmdlParser.addOption(optFormat);

		try
		{
			mCmdlParser.parse(ARGS);
		}
		catch (OptionException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	protected void loadSettings()
	{
		try
		{
			pSets = OSMCBSettings.load();
		}
		catch (JAXBException e)
		{
			e.printStackTrace();
		}
	}
}
