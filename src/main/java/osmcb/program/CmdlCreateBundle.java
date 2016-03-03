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

import osmb.program.IfCommandLine;

public class CmdlCreateBundle implements IfCommandLine
{
	private final String catalogName;
	@SuppressWarnings("unused") // W #unused
	private final File outputDir;

	public CmdlCreateBundle(String catalogName)
	{
		this(catalogName, null);
	}

	public CmdlCreateBundle(String catalogName, String outputDirectory)
	{
		this.catalogName = catalogName;
		if (outputDirectory != null)
		{
			int nNum = 1;
			String strNum;
			File dir = new File(outputDirectory);
			while (dir.isDirectory() || dir.exists())
			{
				System.err.println("Error: Bundle output directory \"" + outputDirectory + "\" already exists.");
				// System.exit(1);
				strNum = String.format("-%d", nNum++);
				dir = new File(outputDirectory + strNum);
			}
			outputDir = dir;
		}
		else
			outputDir = null;
	}

	/**
	 * This creates one bundle and quits osmcb afterwards.
	 */
	public void createBundle()
	{
		// ExecutorService mExec = Executors.newFixedThreadPool(1);
		// try
		// {
		// IfCatalogProfile p = new Catalog(catalogName);
		// if (!p.exists())
		// {
		// System.err.println("Catalog \"" + catalogName + "\" could not be loaded:");
		// System.err.println("File \"" + p.getFile().getAbsolutePath() + "\" does not exist.");
		// System.exit(1);
		// }
		// IfBundle bundle = null;
		// // try
		// // {
		// // // bundle = new Bundle(p.load(), format);
		// // }
		// // catch (JAXBException e)
		// // {
		// // System.err.println("Error loading catalog \"" + catalogName + "\".");
		// // e.printStackTrace();
		// // System.exit(1);
		// // }
		//
		// ACBundleCreator bc = bundle.createBundleCreatorInstance();
		// mExec.execute(bc);
		//
		// // BundleThread bundleThread = new BundleThread(bundle);
		// // if (outputDir != null)
		// // bundleThread.setCustomBundleDir(outputDir);
		// // bundleThread.setQuitOsmcbAfterBundleCreation(true);
		// // bundleThread.start();
		// }
		// catch (Exception e)
		// {
		// GUIExceptionHandler.processException(e);
		// }
	}
}
