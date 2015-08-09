package osmcb.program.bundle;

import osmb.program.catalog.Catalog;
import osmb.program.catalog.IfCatalog;

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
		// this.layers = ifCatalog.
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
}
