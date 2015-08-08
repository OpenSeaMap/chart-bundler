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
package osmcb;

import osmcb.utilities.OSMCBUtilities;

public class OSMCBOutOfMemoryException extends RuntimeException
{

	long requiredMemory;
	long heapAvailable;

	public OSMCBOutOfMemoryException(long requiredMemory, String message) {
		super(message);
		Runtime r = Runtime.getRuntime();
		heapAvailable = r.maxMemory() - r.totalMemory() + r.freeMemory();
		this.requiredMemory = requiredMemory;
	}

	@Override
	public String getMessage()
	{
		return super.getMessage() + "\nRequired memory: " + getFormattedRequiredMemory() + "\nAvailable free memory: " + OSMCBUtilities.formatBytes(heapAvailable);
	}

	public long getRequiredMemory()
	{
		return requiredMemory;
	}

	public String getFormattedRequiredMemory()
	{
		return OSMCBUtilities.formatBytes(requiredMemory);
	}

}
