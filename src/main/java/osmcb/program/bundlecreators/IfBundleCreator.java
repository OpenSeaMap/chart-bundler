package osmcb.program.bundlecreators;

import java.io.IOException;

import osmcb.program.bundle.BundleTestException;
import osmcb.program.bundle.MapCreationException;

public interface IfBundleCreator extends Runnable
{
	/**
	 * @see java.lang.Runnable#run()
	 */
	@Override
	void run();

	// Bundle actions. These has to be overridden by the actual implementation
	/**
	 * Usually the implementation calls initializeBundle(String bundleDirName) after providing the directory name
	 * 
	 * @throws IOException
	 * @throws BundleTestException
	 */
	void initializeBundle() throws IOException, BundleTestException;

	void createBundle() throws IOException, InterruptedException;

	/**
	 * usually does nothing. The actual action will be taken by the instance
	 */
	void finishBundle();

	// Layer actions. These has to be overridden by the actual implementation
	public void initializeLayer() throws IOException, InterruptedException;

	public void createLayer() throws IOException, InterruptedException;

	public void finishLayer() throws IOException;

	// Map actions. These has to be overridden by the actual implementation
	public void initializeMap() throws IOException;

	public void createMap() throws MapCreationException, InterruptedException;

	public void finishMap();

	boolean loadMapTiles() throws Exception;
}