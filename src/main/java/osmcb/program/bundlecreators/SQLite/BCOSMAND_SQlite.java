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
package osmcb.program.bundlecreators.SQLite;

import osmb.mapsources.IfMapSource;
import osmb.mapsources.mapspace.MercatorPower2MapSpace;
import osmcb.program.bundle.IfBundle;
import osmcb.program.bundlecreators.IfBundleCreatorName;

@IfBundleCreatorName(value = "OsmAnd SQLite DB", type = "OSMAND_SQlite")
public class BCOSMAND_SQlite extends BCRMapsSQLite
{
	public BCOSMAND_SQlite(IfBundle bundle)
	{
		super(bundle);
	}

	@Override
	public boolean testMapSource(IfMapSource mapSource)
	{
		return MercatorPower2MapSpace.INSTANCE_256.equals(mapSource.getMapSpace());
	}
}
