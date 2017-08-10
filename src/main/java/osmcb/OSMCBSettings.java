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

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.ValidationEvent;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.XmlAccessOrder;
import javax.xml.bind.annotation.XmlAccessorOrder;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;

import osmb.program.ACSettings;
import osmb.utilities.stream.ThrottledInputStream;
import osmcb.program.ProgramInfo;
import osmcb.program.ProxyType;
import osmcb.utilities.OSMCBUtilities;

// some sort of singleton implementation

@XmlRootElement
@XmlAccessorOrder(XmlAccessOrder.ALPHABETICAL)
public class OSMCBSettings extends ACSettings
{
	// class data / statics
	protected static Logger initLogger()
	{
		return log = Logger.getLogger(OSMCBSettings.class);
	}

	protected static final String SYSTEM_PROXY_HOST = System.getProperty("http.proxyHost");
	protected static final String SYSTEM_PROXY_PORT = System.getProperty("http.proxyPort");

	// instance data, usually all protected
	// esp. this classes instances are load from a xml-file by loadOrQuit()
	// public String elementName = null;
	protected boolean cfgCustomTileProcessing = false;

	/**
	 * Development mode enabled/disabled
	 * <p>
	 * In development mode one additional iMap source is available for using OSMCB Debug TileServer
	 * </p>
	 */
	@XmlElement
	protected boolean cfgDevMode = false;

	/**
	 * Network settings
	 */
	protected ProxyType cfgProxyType = ProxyType.CUSTOM;

	public boolean getDevMode()
	{
		return cfgDevMode;
	}

	public void setDevMode(boolean cfgDevMode)
	{
		this.cfgDevMode = cfgDevMode;
	}

	protected String cfgCustomProxyHost = "";
	protected String cfgCustomProxyPort = "";
	protected String cfgCustomProxyUserName = "";
	protected String cfgCustomProxyPassword = "";
	protected long cfgBandwidthLimit = 0;

	// @XmlElementWrapper(name = "mapSourcesDisabled")
	// @XmlElement(name = "mapSource")
	// protected Vector<String> cfgMapSourcesDisabled = new Vector<String>();
	//
	// @XmlElementWrapper(name = "mapSourcesEnabled")
	// @XmlElement(name = "mapSource")
	// protected Vector<String> cfgMapSourcesEnabled = new Vector<String>();

	// protected boolean cfgIgnoreDlErrors = false;

	/**
	 * the time between updates for the bundle creation in days
	 */
	@XmlElement(name = "bundleUpdateTime")
	protected int cfgBundleUpdateTime = 60;

	public int getBundleUpdateDays()
	{
		return cfgBundleUpdateTime;
	}

	public int getBundleUpdateHours()
	{
		return cfgBundleUpdateTime * 24;
	}

	/**
	 * constructor should provide default values for every element
	 */
	protected OSMCBSettings()
	{
		super();
	}

	public static OSMCBSettings getInstance()
	{
		if (instance == null)
			instance = new OSMCBSettings();
		return (OSMCBSettings) instance;
	}

	public static OSMCBSettings load() throws JAXBException
	{
		OSMCBSettings s = null;
		try
		{
			JAXBContext context = JAXBContext.newInstance(OSMCBSettings.class);
			Unmarshaller um = context.createUnmarshaller();
			um.setEventHandler(new ValidationEventHandler()
			{
				@Override
				public boolean handleEvent(ValidationEvent event)
				{
					log.warn("Problem on loading settings.xml: " + event.getMessage());
					return true;
				}
			});
			s = (OSMCBSettings) um.unmarshal(getFile());
			s.getWgsGrid().checkValues();
			SETTINGS_LAST_MODIFIED = getFile().lastModified();
			OSMCBRsc.updateLocalizedStrings();
		}
		finally
		{
			instance = s;
		}
		return (OSMCBSettings) instance;
	}

	public static void save() throws JAXBException
	{
		getInstance().cfgVersion = ProgramInfo.getVersion();
		JAXBContext context = JAXBContext.newInstance(OSMCBSettings.class);
		Marshaller m = context.createMarshaller();
		m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
		ByteArrayOutputStream bo = null;
		FileOutputStream fo = null;
		try
		{
			// First we write to a buffer and if that works be write the buffer
			// to disk. Direct writing to file may result in an defect xml file
			// in case of an error
			bo = new ByteArrayOutputStream();
			m.marshal(getInstance(), bo);
			fo = new FileOutputStream(getFile());
			fo.write(bo.toByteArray());
			fo.close();
			SETTINGS_LAST_MODIFIED = getFile().lastModified();
		}
		catch (IOException e)
		{
			throw new JAXBException(e);
		}
		finally
		{
			OSMCBUtilities.closeStream(fo);
		}
	}

	public static OSMCBSettings loadOrQuit()
	{
		try
		{
			load();
		}
		catch (JAXBException e)
		{
			// log.error(e);
			// JOptionPane
			// .showMessageDialog(null, OSMCBStrs.RStr(OSMCBStrs.RStr("msg_settings_file_can_not_parse")), OSMCBStrs.RStr("Error"), JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
		return (OSMCBSettings) instance;
	}

	public boolean isCustomTileSize()
	{
		return cfgCustomTileProcessing;
	}

	public void setCustomTileSize(boolean customTileSize)
	{
		this.cfgCustomTileProcessing = customTileSize;
	}

	public ProxyType getProxyType()
	{
		return cfgProxyType;
	}

	public void setProxyType(ProxyType proxyType)
	{
		this.cfgProxyType = proxyType;
	}

	public String getCustomProxyHost()
	{
		return cfgCustomProxyHost;
	}

	public String getCustomProxyPort()
	{
		return cfgCustomProxyPort;
	}

	public void setCustomProxyHost(String proxyHost)
	{
		this.cfgCustomProxyHost = proxyHost;
	}

	public void setCustomProxyPort(String proxyPort)
	{
		this.cfgCustomProxyPort = proxyPort;
	}

	public String getCustomProxyUserName()
	{
		return cfgCustomProxyUserName;
	}

	public void setCustomProxyUserName(String customProxyUserName)
	{
		this.cfgCustomProxyUserName = customProxyUserName;
	}

	public String getCustomProxyPassword()
	{
		return cfgCustomProxyPassword;
	}

	public void setCustomProxyPassword(String customProxyPassword)
	{
		this.cfgCustomProxyPassword = customProxyPassword;
	}

	public void applyProxySettings()
	{
		boolean useSystemProxies = false;
		String newProxyHost = null;
		String newProxyPort = null;
		Authenticator newAuthenticator = null;
		switch (cfgProxyType)
		{
			case SYSTEM:
				log.info("Applying proxy configuration: system settings");
				useSystemProxies = true;
				break;
			case APP_SETTINGS:
				newProxyHost = SYSTEM_PROXY_HOST;
				newProxyPort = SYSTEM_PROXY_PORT;
				log.info("Applying proxy configuration: host=" + newProxyHost + " port=" + newProxyPort);
				break;
			case CUSTOM:
				newProxyHost = cfgCustomProxyHost;
				newProxyPort = cfgCustomProxyPort;
				log.info("Applying proxy configuration: host=" + newProxyHost + " port=" + newProxyPort);
				break;
			case CUSTOM_W_AUTH:
				newProxyHost = cfgCustomProxyHost;
				newProxyPort = cfgCustomProxyPort;
				newAuthenticator = new Authenticator()
				{
					@Override
					protected PasswordAuthentication getPasswordAuthentication()
					{
						return new PasswordAuthentication(cfgCustomProxyUserName, cfgCustomProxyPassword.toCharArray());
					}
				};
				log.info("Applying proxy configuration: host=" + newProxyHost + " port=" + newProxyPort + " user=" + cfgCustomProxyUserName);
				break;
		}
		OSMCBUtilities.setHttpProxyHost(newProxyHost);
		OSMCBUtilities.setHttpProxyPort(newProxyPort);
		Authenticator.setDefault(newAuthenticator);
		System.setProperty("java.net.useSystemProxies", Boolean.toString(useSystemProxies));
	}

	public long getBandwidthLimit()
	{
		return cfgBandwidthLimit;
	}

	public void setBandwidthLimit(long bandwidthLimit)
	{
		this.cfgBandwidthLimit = bandwidthLimit;
		ThrottledInputStream.setBandwidth(bandwidthLimit);
	}

	@Override
	public String getVersion()
	{
		return cfgVersion;
	}

	// @XmlTransient
	// public File getChartBundleOutputDirectory()
	// {
	// String dirSetting = directories.bundleOutputDirectory;
	// File bundlessDir;
	// if (dirSetting == null || dirSetting.trim().length() == 0)
	// bundlessDir = DirectoryManager.bundlesDir;
	// else
	// bundlessDir = new File(dirSetting);
	// return bundlessDir;
	// }
}
