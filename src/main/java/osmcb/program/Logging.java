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

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.TreeMap;
import java.util.logging.Handler;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.SimpleLayout;
import org.apache.log4j.xml.DOMConfigurator;

import osmb.program.IfApp;
import osmb.program.Juli2Log4jHandler;
import osmb.utilities.GUIExceptionHandler;
import osmb.utilities.OSUtilities;
import osmcb.OSMCBSettings;
import osmcb.OSMCBStrs;

public class Logging
{
	protected static final String CONFIG_FILENAME = "log4j.xml";
	protected static final String LOG_FILENAME = "OpenSeaMap ChartBundler.log";
	protected static File CONFIG_FILE = null;
	public static final Logger LOG = Logger.getLogger("MAC");
	public static final Layout ADVANCED_LAYOUT = new PatternLayout("%d{ISO8601} %-5p [%t] %c{1}: %m%n");
	protected static boolean CONFIGURED = false;
	private static IfApp gApp;

	public static void configureLogging(IfApp myApp)
	{
		// We test for the configuration file, if it exists we use it, otherwise
		// we perform simple logging to the console
		if (!loadLog4JConfigXml())
		{
			configureDefaultErrorLogging();
			Logger logger = Logger.getRootLogger();
			logger.info("log4j.xml not found - enabling default error log to console");
		}
		gApp = myApp;
	}

	/**
	 * try to find log4j config file 'log4j.xml'
	 * 
	 * @return true if log4j.xml found in any path, no info where
	 */
	public static boolean loadLog4JConfigXml()
	{
		if (loadLog4JConfigXml(DirectoryManager.userAppDataDir))
			return true;
		if (loadLog4JConfigXml(DirectoryManager.userSettingsDir))
			return true;
		if (loadLog4JConfigXml(DirectoryManager.currentDir))
			return true;
		if (loadLog4JConfigXml(DirectoryManager.programDir))
			return true;
		return false;
	}

	public static boolean loadLog4JConfigXml(File directory)
	{
		File f = new File(directory, CONFIG_FILENAME);
		if (!f.isFile())
			return false;
		try
		{
			DOMConfigurator.configure(f.getAbsolutePath());
		}
		catch (Exception e)
		{
			System.err.println("Error loading log4j config file \"" + f.getAbsolutePath() + "\"");
			return false;
		}
		Logger logger = Logger.getLogger("LogSystem");
		logger.setLevel(Level.INFO);
		logger.info("Logging configured by \"" + f.getAbsolutePath() + "\"");
		CONFIGURED = true;
		return true;
	}

	public static void configureDefaultErrorLogging()
	{
		Logger.getRootLogger().setLevel(Level.INFO);
		configureConsoleLogging(Level.TRACE, new SimpleLayout());
		configureLogFileLogging(Level.TRACE);
	}

	public static void configureConsoleLogging()
	{
		configureConsoleLogging(Level.ERROR, new SimpleLayout());
	}

	public static void configureConsoleLogging(Level level)
	{
		configureConsoleLogging(level, new SimpleLayout());
	}

	public static void configureConsoleLogging(Level level, Layout layout)
	{
		Logger logger = Logger.getRootLogger();
		ConsoleAppender consoleAppender = new ConsoleAppender(layout);
		if (level != null)
			consoleAppender.setThreshold(level);
		logger.addAppender(consoleAppender);
		CONFIGURED = true;
	}

	public static void configureLogFileLogging(Level level)
	{
		Logger logger = Logger.getRootLogger();
		File logFileDir = DirectoryManager.userAppDataDir;
		String logFilename = new File(logFileDir, LOG_FILENAME).getAbsolutePath();
		Layout layout = new PatternLayout("%d{ISO8601} %-5p [%t] %c{1}: %m%n");
		FileAppender fileAppender;
		try
		{
			fileAppender = new FileAppender(layout, logFilename, false);
			if (level != null)
				fileAppender.setThreshold(level);
			logger.addAppender(fileAppender);
		}
		catch (Exception e)
		{
			Logger log = Logger.getLogger("LogSystem");
			log.error("", e);
		}
	}

	public static void disableLogging()
	{
		Logger logger = Logger.getRootLogger();
		logger.setLevel(Level.OFF);
	}

	public static void enableJAXBLogging()
	{
		java.util.logging.Logger logger;
		Handler h = new Juli2Log4jHandler();
		logger = java.util.logging.Logger.getLogger("javax.xml.bind");
		logger.setLevel(java.util.logging.Level.ALL);
		logger.addHandler(h);
		logger = java.util.logging.Logger.getLogger("com.sun.xml.internal.bind");
		logger.setLevel(java.util.logging.Level.ALL);
		logger.addHandler(h);
	}

	public static void logSystemInfo()
	{
		Logger log = Logger.getLogger("SysInfo");
		OSMCBSettings pSets = (OSMCBSettings) gApp.getSettings();
		if (log.isInfoEnabled())
		{
			String n = System.getProperty("line.separator");
			log.info("Version: " + ProgramInfo.getCompleteTitle());
			log.info("Platform: " + GUIExceptionHandler.prop("os.name") + " (" + GUIExceptionHandler.prop("os.version") + ")");
			log.info("Java VM: " + GUIExceptionHandler.prop("java.vm.name") + " (" + GUIExceptionHandler.prop("java.runtime.version") + ")");
			log.info(OSMCBStrs.RStr("RSTR_Directories") /**/
			    + n + "  currentDir:      " + DirectoryManager.currentDir /**/
			    + n + "  programDir:      " + DirectoryManager.programDir /**/
			    + n + "  tempDir:         " + DirectoryManager.tempDir /**/
			    + n + "  userHomeDir:     " + DirectoryManager.userHomeDir /**/
			    + n + "  userSettingsDir: " + DirectoryManager.userSettingsDir /**/
			    + n + "  userAppDataDir:  " + DirectoryManager.userAppDataDir /**/
			    + n + "  catalogsDir:     " + pSets.getCatalogsDirectory() /**/
			    + n + "  tileStoreDir:    " + pSets.getTileStoreDirectory() /**/
			// + n + " chartBundleDir: " + pSets.getChartBundleOutputDirectory() /**/
			    + n + "  mapSourcesDir:   " + pSets.getMapSourcesDirectory() /**/
			);
			log.info("System console available: " + (System.console() != null));
			String[] args = gApp.getArgs();
			if (args != null)
			{
				log.info("Startup arguments (count=" + args.length + "):");
				for (int i = 0; i < args.length; i++)
					log.info("\t" + i + ": " + args[i]);
			}
			else
				log.info("no arguments given");
		}
		if (log.isDebugEnabled())
		{
			log.debug("Detected operating system: " + OSUtilities.detectOs() + " (" + System.getProperty("os.name") + ")");
			boolean desktopSupport = Desktop.isDesktopSupported();
			log.debug("Desktop support: " + desktopSupport);
			if (desktopSupport)
			{
				Desktop d = Desktop.getDesktop();
				for (Action a : Action.values())
				{
					log.debug("Desktop action " + a + " supported: " + d.isSupported(a));
				}
			}
		}
		if (log.isTraceEnabled())
		{
			Properties props = System.getProperties();
			StringWriter sw = new StringWriter(2 << 13);
			sw.write("System properties:\n");
			TreeMap<Object, Object> sortedProps = new TreeMap<Object, Object>(props);
			for (Entry<Object, Object> entry : sortedProps.entrySet())
			{
				sw.write(entry.getKey() + " = " + entry.getValue() + "\n");
			}
			log.trace(sw.toString());
		}
	}

	/**
	 * returns the first configured {@link FileAppender} or <code>null</code>.
	 * 
	 * @return
	 */
	public static String getLogFile()
	{
		Enumeration<?> enu = Logger.getRootLogger().getAllAppenders();
		while (enu.hasMoreElements())
		{
			Object o = enu.nextElement();
			if (o instanceof FileAppender)
			{
				FileAppender fa = (FileAppender) o;
				return fa.getFile();
			}
		}
		return null;
	}

	public static boolean isCONFIGURED()
	{
		return CONFIGURED;
	}
}
