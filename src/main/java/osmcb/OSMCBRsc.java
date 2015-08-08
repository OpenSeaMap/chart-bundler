package osmcb;

import java.io.InputStream;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

//import osmcb.OSMCBApp;

public class OSMCBRsc
{
	public static Locale currLocale;
	public static String localeLanguage;
	public static String localeCountry;
	private static ResourceBundle STRING_RESOURCE = null;

	public static void initializeRSTRs()
	{
		try
		{
			currLocale = getLocale();
			STRING_RESOURCE = ResourceBundle.getBundle("osmcb.resources.text.loc-nls");
		}
		catch (MissingResourceException e)
		{
		}
	}

	public static void initializeRSTRs(Locale newLocale)
	{
		try
		{
			STRING_RESOURCE = ResourceBundle.getBundle("osmcb.resources.text.loc-nls", newLocale);
			currLocale = newLocale;
		}
		catch (MissingResourceException e)
		{
		}
	}

	/**
	 * seems to translate the locale into language and country
	 */
	public static Locale getLocale()
	{
		Locale defaultLocale = Locale.getDefault();

		localeLanguage = defaultLocale.getLanguage();
		localeCountry = defaultLocale.getCountry();

		// Implement some algorithm to fallback on the implemented locales
		return defaultLocale;
	}

	/**
	 * loads the localized string from the appropriate resource bundle
	 * 
	 * @param key
	 *          - a resource key
	 * @param args
	 *          - additional args to be formatted into the string. This allows for placeholders in the localized string, but not for position exchange
	 * @return - the localized string
	 */
	public static String localizedStringForKey(String key, Object... args)
	{
		if (STRING_RESOURCE == null)
			updateLocalizedStrings();
		String str = null;
		try
		{
			str = STRING_RESOURCE.getString(key);
			if (args.length > 0)
				str = String.format(str, args);
		}
		catch (Exception e)
		{
			str = key;
		}
		// always return a valid string, it might have become null.
		if (str == null)
		{
			str = "";
		}
		return str;
	}

	public static synchronized void updateLocalizedStrings()
	{
		try
		{
			Locale locale = new Locale(localeLanguage, localeCountry);
			// STRING_RESOURCE = ResourceBundle.getBundle("resources", locale, new UTF8Control());
			STRING_RESOURCE = ResourceBundle.getBundle("loc-nls", locale);
		}
		catch (Exception e)
		{
		}
	}

	public static InputStream getResourceAsStream(String name, String extension)
	{
		getLocale();
		InputStream in;
		in = OSMCBRsc.class.getResourceAsStream(String.format("%s_%s_%s.%s", name, localeLanguage, localeCountry, extension));
		if (in != null)
			return in;
		in = OSMCBRsc.class.getResourceAsStream(String.format("%s_%s.%s", name, localeLanguage, extension));
		if (in != null)
			return in;
		in = OSMCBRsc.class.getResourceAsStream(String.format("%s.%s", name, extension));
		return in;
	}
}
