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
package osmcb.externaltools;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlList;
import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;

import osmb.utilities.GUIExceptionHandler;

@XmlRootElement(name = "ExternalTool")
public class ExternalToolDef implements ActionListener
{
	private static final Logger log = Logger.getLogger(ExternalToolDef.class);

	/**
	 * Name used for the menu entry in OSMCB
	 */
	public String name;

	/**
	 * For starting a commandline-script on Windows use <code>cmd /c start mybatch.cmd</code>
	 */
	public String command;

	public boolean debug = false;

	@XmlList
	public List<ToolParameters> parameters = new ArrayList<ToolParameters>();

	// private boolean mapSelNull(MapSelection mapSel)
	// {
	// if (mapSel != null)
	// return false;
	// return true;
	// }
	//
	public void actionPerformed(ActionEvent e)
	{
		try
		{
			String executeCommand = command;
			for (ToolParameters param: parameters)
			{
				String add = "";
				if (add.indexOf(' ') >= 0)
					add = "\"" + add + "\"";
				executeCommand += " " + add;
			}
			if (debug)
			{
				return;
			}
			log.debug("Executing " + executeCommand);
			Runtime.getRuntime().exec(executeCommand);
		}
		catch (Exception e1)
		{
			GUIExceptionHandler.processException(e1);
		}
	}
}
