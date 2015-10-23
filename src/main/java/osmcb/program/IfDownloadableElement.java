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
package osmcb.program;

import java.util.Enumeration;

import osmb.program.IfJob;
import osmb.program.map.IfLayer;
import osmb.program.map.IfMap;
import osmcb.program.bundle.IfBundle;
import osmcb.program.download.IfDownloadJobListener;
import osmcb.utilities.tar.TarIndexedArchive;

/**
 * Classes that implement this interface identify themselves as responsible for specifying what tiles should be downloaded.
 * 
 * In general this interface should be implemented in combination with {@link IfMap}, {@link IfLayer} or {@link IfBundle}.
 * 
 */
public interface IfDownloadableElement
{
	/**
	 * 
	 * @param tileArchive
	 * @param listener
	 * @return An enumeration that returns {@link IfJob} objects. Each job should download one map tile from the providing web server (or from the tile cache).
	 */
	public Enumeration<IfJob> getDownloadJobs(TarIndexedArchive tileArchive, IfDownloadJobListener listener);
}
