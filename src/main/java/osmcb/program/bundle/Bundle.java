package osmcb.program.bundle;

import java.util.LinkedList;
import java.util.List;

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
		protected List<IfMap> maps = new LinkedList<IfMap>();
	}

	protected BundleOutputFormat mBOF;

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
	}

	@Override
	public boolean isInvalid()
	{
		boolean bOK = (mBOF != null);
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
}
