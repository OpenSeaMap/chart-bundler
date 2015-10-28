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
package osmcb.program.bundle;

import osmb.program.map.IfLayer;
import osmb.program.map.IfMap;

public class BundleTestException extends Exception
{
	public BundleTestException(String message, IfMap map)
	{
		super(message + "\nError caused by iMap \"" + map.getName() + "\" on layer \"" + map.getLayer().getName() + "\"");
	}

	public BundleTestException(String message, IfLayer layer)
	{
		super(message + "\nError caused by layer \"" + layer.getName() + "\"");
	}

	public BundleTestException(String message)
	{
		super(message);
	}

	public BundleTestException(Throwable cause)
	{
		super(cause);
	}

	public BundleTestException(String message, Throwable cause)
	{
		super(message, cause);
	}
}
