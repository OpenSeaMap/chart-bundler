package osmcb.program.bundle;

import osmb.program.catalog.Catalog;
import osmcb.program.bundlecreators.ACBundleCreator;

public class Bundle extends Catalog implements IfBundle
{
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
