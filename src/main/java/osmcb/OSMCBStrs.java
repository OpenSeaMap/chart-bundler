package osmcb;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.log4j.Logger;

public class OSMCBStrs
{
	private static final Logger log = Logger.getLogger(OSMCBStrs.class);

	private static final String BUNDLE_NAME = "osmcb.resources.text.loc-nls";
	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	private OSMCBStrs()
	{
		// sLog.info("RSC bundle='" + (RESOURCE_BUNDLE). + "'");
		log.info("RSC bundle='" + BUNDLE_NAME + "'");
	}

	public static String RStr(String key)
	{
		try
		{
			return RESOURCE_BUNDLE.getString(key);
		}
		catch (MissingResourceException e)
		{
			// sLog.info("RSC bundle='" + RESOURCE_BUNDLE.getBaseBundleName() + "' missing");
			log.info("RSC key='" + key + "' from bundle='" + BUNDLE_NAME + "' missing");
			return '!' + key + '!';
		}
	}
}
