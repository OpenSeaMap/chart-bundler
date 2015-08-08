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

import java.io.StringWriter;

import osmb.mapsources.IfMapSource;
import osmb.mapsources.MapSourceLoaderInfo;
import osmb.program.map.IfMap;
import osmb.utilities.IFExceptionExtendedInfo;

public class MapCreationException extends Exception implements IFExceptionExtendedInfo
{
	private static final long serialVersionUID = 1L;
	private IfMap map;

	public MapCreationException(String message, IfMap map, Throwable cause)
	{
		super(message, cause);
		this.map = map;
	}

	public MapCreationException(String message, IfMap map)
	{
		super(message);
		this.map = map;
	}

	public MapCreationException(IfMap map, Throwable cause)
	{
		super(cause);
		this.map = map;
	}

	@Override
	public String getExtendedInfo()
	{
		StringWriter sw = new StringWriter();
		if (map != null)
		{
			sw.append(map.getInfoText());
			IfMapSource mapSource = map.getMapSource();
			if (mapSource != null)
			{
				MapSourceLoaderInfo loaderInfo = map.getMapSource().getLoaderInfo();
				if (loaderInfo != null)
				{
					sw.append("\nMap type: " + loaderInfo.getLoaderType());
					if (loaderInfo.getSourceFile() != null)
						sw.append("\nMap implementation: " + loaderInfo.getSourceFile().getName());
					sw.append("\nMap revision: " + loaderInfo.getRevision());
				}
			}
		}
		return sw.toString();
	}

	public IfMap getMap()
	{
		return map;
	}
}
