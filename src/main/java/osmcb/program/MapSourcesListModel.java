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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import javax.swing.AbstractListModel;

import osmb.mapsources.ACMapSource;

public class MapSourcesListModel extends AbstractListModel<Object> // W ? <IfMapSource>
{
	private static final long serialVersionUID = 1L;

	ArrayList<ACMapSource> mapSources;

	public MapSourcesListModel(Vector<ACMapSource> source)
	{
		this.mapSources = new ArrayList<ACMapSource>(source);
	}

	@Override
	public Object getElementAt(int index) // W ? <IfMapSource>
	{
		return mapSources.get(index);
	}

	@Override
	public int getSize()
	{
		return mapSources.size();
	}

	public Vector<ACMapSource> getVector()
	{
		return new Vector<ACMapSource>(mapSources);
	}

	public ACMapSource removeElement(int index)
	{
		fireIntervalRemoved(this, index, index);
		return mapSources.remove(index);
	}

	public void addElement(ACMapSource element)
	{
		mapSources.add(element);
		fireIntervalAdded(this, mapSources.size(), mapSources.size());
	}

	public boolean moveUp(int index)
	{
		if (index < 1)
			return false;
		ACMapSource ms = mapSources.remove(index - 1);
		mapSources.add(index, ms);
		fireContentsChanged(this, index - 1, index);
		return true;
	}

	public boolean moveDown(int index)
	{
		if (index + 1 >= mapSources.size())
			return false;
		ACMapSource ms = mapSources.remove(index + 1);
		mapSources.add(index, ms);
		fireContentsChanged(this, index, index + 1);
		return true;
	}

	public void sort()
	{
		Collections.sort(mapSources, new Comparator<ACMapSource>()
		{
			@Override
			public int compare(ACMapSource o1, ACMapSource o2)
			{
				return o1.toString().compareTo(o2.toString());
			}
		});
		fireContentsChanged(mapSources, 0, mapSources.size());
	}
}
