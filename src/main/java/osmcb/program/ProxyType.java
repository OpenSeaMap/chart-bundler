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

import osmcb.OSMCBStrs;

public enum ProxyType
{
	SYSTEM, //
	APP_SETTINGS, //
	CUSTOM, //
	CUSTOM_W_AUTH;

	// private String text;

	// private ProxyType(String text) {
	// this.text = text;
	// }

	@Override
	public String toString()
	{
		switch (this)
		{
			case SYSTEM:
				return OSMCBStrs.RStr("ProxyType.System");
			case APP_SETTINGS:
				return OSMCBStrs.RStr("ProxyType.AppSettings");
			case CUSTOM:
				return OSMCBStrs.RStr("ProxyType.Custom");
			case CUSTOM_W_AUTH:
				return OSMCBStrs.RStr("ProxyType.CustomAuth");
		}
		return OSMCBStrs.RStr("Undefined");
	}
}
