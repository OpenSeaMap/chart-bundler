package osmcb.program.bundle;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import osmb.program.catalog.Catalog;
import osmb.program.map.IfLayer;
import osmb.program.map.IfMap;
import osmcb.program.bundlecreators.ACBundleCreator;

public class Bundle extends Catalog implements IfBundle
{
	protected class MapDescriptor
	{
		@XmlAttribute
		protected String name = null;
		@XmlAttribute
		protected String number = null;
	}

	@XmlRootElement
	protected class BundleDescriptor
	{
		@XmlAttribute
		protected String getName()
		{
			return name;
		};

		protected int version = 1;

		@XmlElements(
		{ @XmlElement(name = "map", type = MapDescriptor.class) })
		protected List<IfMap> maps = new LinkedList<>();
	}

	public static final String BUNDLE_NAME_REGEX = "([\\w_]+)";
	public static final String BUNDLE_APP_REGEX = "([\\w_]+)";
	public static final String BUNDLE_FMT_REGEX = "([\\w_]+)";
	public static final String TIMESTAMP_REGEX = "(\\d{8})-(\\d{4})";
	public static final String BUNDLE_FILENAME_PREFIX = "OSM";
	public static final Pattern BUNDLE_FILENAME_PATTERN = Pattern
	    .compile(BUNDLE_FILENAME_PREFIX + BUNDLE_APP_REGEX + BUNDLE_FMT_REGEX + BUNDLE_NAME_REGEX + "(" + TIMESTAMP_REGEX + ")");

	protected BundleOutputFormat mBOF;

	/*
	 * The bundles name is a multipart name. It consists of:
	 * - OSM as literal
	 * - App field
	 * - Map file format
	 * - Region name
	 * - Date
	 * - Time
	 * The baseName is the part of the name containing app, map format and region name.
	 */
	protected String strBaseName;

	protected Date dtCreationDate;

	/**
	 * Should never be used
	 * 
	 * @param catalogName
	 */
	private Bundle(String catalogName)
	{
		super(catalogName);
	}

	/**
	 * This is the 'normal' constructor for a usable Bundle. It will need a Catalog and a BundleOutputFormat to produce any output.
	 * 
	 * @param catalog
	 * @param bundleOutputFormat
	 */
	public Bundle(Catalog catalog, BundleOutputFormat bundleOutputFormat)
	{
		super(catalog);
		mBOF = bundleOutputFormat;
		// mBOF.getName();
		strBaseName = "###";
	}

	@Override
	public boolean isInvalid()
	{
		boolean bOK = (mBOF == null);
		return bOK;
	}

	@Override
	public void setOutputFormat(BundleOutputFormat bundleOutputFormat)
	{
		mBOF = bundleOutputFormat;
	}

	@Override
	public BundleOutputFormat getOutputFormat()
	{
		return mBOF;
	}

	@Override
	public String getBaseName()
	{
		return strBaseName;
	}

	@Override
	public void setBaseName(String newBaseName)
	{
		strBaseName = newBaseName;
	}

	/**
	 * @see osmb.program.catalog.Catalog#calculateTilesToLoad()
	 */
	@Override
	public long calculateTilesToLoad()
	{
		long tiles = 0;

		for (IfLayer layer : layers)
		{
			if (mBOF.filterLayers(layer))
				tiles += layer.calculateTilesToLoad();
		}
		log.trace("catalog=" + getName() + ", tiles=" + tiles);
		return tiles;
	}

	/**
	 * @see osmb.program.catalog.Catalog#calcMapsToCompose()
	 */
	@Override
	public long calcMapsToCompose()
	{
		long nMaps = 0;
		for (IfLayer layer : layers)
		{
			if (mBOF.filterLayers(layer))
				nMaps += layer.getMapCount();
		}
		log.trace("catalog=" + getName() + ", maps=" + nMaps);
		return nMaps;
	}

	/**
	 * The class to be instantiated is specified by the {@link BundleOutputFormat}
	 */
	@Override
	public ACBundleCreator createBundleCreatorInstance() throws InstantiationException, IllegalAccessException
	{
		ACBundleCreator bc = this.mBOF.createBundleCreatorInstance();
		return bc;
	}

	/**
	 * The class to be instantiated is specified by the {@link BundleOutputFormat}
	 */
	@Override
	public ACBundleCreator createLayerCreatorInstance() throws InstantiationException, IllegalAccessException
	{
		ACBundleCreator bc = this.mBOF.createBundleCreatorInstance();
		return bc;
	}

	/**
	 * The class to be instantiated is specified by the {@link BundleOutputFormat}
	 */
	@Override
	public ACBundleCreator createMapCreatorInstance() throws InstantiationException, IllegalAccessException
	{
		ACBundleCreator bc = this.mBOF.createBundleCreatorInstance();
		return bc;
	}

	@Override
	public void SetDate(Date tCrDate)
	{
		dtCreationDate = tCrDate;
	}

	@Override
	public Date getDate()
	{
		return dtCreationDate;
	}
}
