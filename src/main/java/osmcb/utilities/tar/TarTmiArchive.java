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
package osmcb.utilities.tar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;

import osmcb.program.bundlecreators.ACBundleCreator;
import osmcb.utilities.OSMCBUtilities;

/**
 * 
 * Extended version of {@link TarArchive} that automatically creates the BCTrekBuddy tmi-file while writing the archive entries.
 * 
 * @author r_x
 * 
 * @see <a href="http://www.linuxtechs.net/kruch/tb/forum/viewtopic.php?t=897"> BCTrekBuddy tmi iMap tar index file description< /a>
 */
public class TarTmiArchive extends TarArchive
{
	Writer tmiWriter;

	public TarTmiArchive(File tarFile, File baseDir) throws IOException {
		super(tarFile, baseDir);
		String tmiFilename = tarFile.getAbsolutePath();
		if (tmiFilename.toLowerCase().endsWith(".tar"))
			tmiFilename = tmiFilename.substring(0, tmiFilename.length() - 4);

		tmiWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(tmiFilename + ".tmi"), ACBundleCreator.TEXT_FILE_CHARSET));
	}

	@Override
	public void writeEndOfArchive() throws IOException
	{
		super.writeEndOfArchive();
		tmiWriter.flush();
	}

	@Override
	public void close()
	{
		super.close();
		OSMCBUtilities.closeWriter(tmiWriter);
	}

	@Override
	protected void writeTarHeader(TarHeader th) throws IOException
	{
		long streamPos = getTarFilePos();
		int block = (int) (streamPos >> 9);
		String line = String.format("block %10d: %s\n", new Object[] {block, th.getFileName()});
		tmiWriter.write(line);
		super.writeTarHeader(th);
	}
}
