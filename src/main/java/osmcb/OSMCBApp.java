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
import java.util.List;

import javax.imageio.ImageIO;
import javax.xml.bind.JAXBException;

import osmb.mapsources.DefaultMapSourcesManager;
import osmb.program.ACConsoleApp;
import osmb.program.ACSettings;
import osmb.program.EnvironmentSetup;
import osmb.program.JArgs.Option.StringOption;
import osmb.program.JArgs.OptionException;
import osmb.program.JobDispatcher;
import osmb.program.catalog.Catalog;
import osmb.program.tilestore.ACTileStore;
import osmb.utilities.GUIExceptionHandler;
import osmcb.program.Logging;
import osmcb.program.ProgramInfo;
import osmcb.program.bundle.Bundle;
import osmcb.program.bundle.BundleOutputFormat;
import osmcb.program.bundlecreators.ACBundleCreator;
import osmcb.utilities.OSMCBUtilities;

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
			ACTileStore.initialize();
			EnvironmentSetup.upgrade();
			Logging.logSystemInfo();

			// log.info(FileSystemProvider.installedProviders());
			// no 7zip fs available

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
		List<String> lCatalogs = OSMCBUtilities.listCatalogs(((OSMCBSettings) gApp.getSettings()).getCatalogsDirectory().toPath());
		while (!lCatalogs.isEmpty())
		{
			try
			{
				String strCat = lCatalogs.remove(0);
				// createBundle(mCmdlParser.getOptionValue(new StringOption('c', "create"), "OSM-Std"),
				// mCmdlParser.getOptionValue(new StringOption('f', "format"), "OpenCPN-KAP"));
				log.info(strCat);
				// createBundle(strCat, mCmdlParser.getOptionValue(new StringOption('f', "format"), "OpenCPN-KAP"));
				wait();
			}
			catch (Exception e)
			{
				GUIExceptionHandler.processException(e);
			}
		}
	}

	/**
	 * This tries to create one bundle (One catalog for one format). In the near future it will check a specified directory for catalogs and automatically create
	 * all catalogs for all formats available.
	 * 
	 * @param catalogName
	 * @param strBundleFormat
	 */
	public void createBundle(String catalogName, String strBundleFormat)
	{
		try
		{
			JobDispatcher mBCExec = new JobDispatcher();
			Catalog cat = Catalog.load(new File(ACSettings.getInstance().getCatalogsDirectory(), Catalog.getCatalogFileName(catalogName)));
			Bundle bundle = new Bundle(cat, BundleOutputFormat.getFormatByName(strBundleFormat));
			ACBundleCreator bundleCreator = bundle.createBundleCreatorInstance();
			bundleCreator.init(bundle, null);
			mBCExec.execute(bundleCreator);
			mBCExec.shutdown();
			while (!mBCExec.isTerminated())
			{
				Thread.sleep(1000);
				log.debug("still running");
			}
			log.debug("bundle creator thread shutdown.");
			ACTileStore.getInstance().closeAll();
		}
		catch (Exception e)
		{
			// System.err.println("Error loading catalog \"" + catalogName + "\".");
			GUIExceptionHandler.processException(e);
			ACTileStore.getInstance().closeAll();
		}
	}

	/**
	 * the commandline gets parsed for the following: -c[reate] create a specified bundle and exit after creation -d[irectory] directory with settings.xml run as
	 * service/daemon -h[elp] shows a short help file
	 */
	@Override
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
