package osmcb.program.bundle;

import osmb.program.catalog.Catalog;
import osmb.program.catalog.IfCatalog;
import osmcb.program.bundlecreators.ACBundleCreator;

public class Bundle extends Catalog implements IfBundle
{
	protected BundleOutputFormat mBOF;

	public Bundle(String catalogName)
	{
		super(catalogName);
	}

	public Bundle(IfCatalog ifCatalog, BundleOutputFormat bundleOutputFormat)
	{
		super(ifCatalog);
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

	@Override
	public ACBundleCreator createBundleCreatorInstance() throws InstantiationException, IllegalAccessException
	{
		ACBundleCreator bc = this.mBOF.getBundleCreatorClass().newInstance();
		// bc.init(this);
		return bc;
	}

	@Override
	public ACBundleCreator createLayerCreatorInstance() throws InstantiationException, IllegalAccessException
	{
		ACBundleCreator bc = this.mBOF.getBundleCreatorClass().newInstance();
		return bc;
	}

	@Override
	public ACBundleCreator createMapCreatorInstance() throws InstantiationException, IllegalAccessException
	{
		ACBundleCreator bc = this.mBOF.getBundleCreatorClass().newInstance();
		return bc;
	}
}
