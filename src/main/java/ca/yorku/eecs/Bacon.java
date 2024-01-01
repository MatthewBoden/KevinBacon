package ca.yorku.eecs;

import java.util.*;

// Goal of Bacon class: store the calculated baconNumber/baconPath data 
public class Bacon {

	// Private attributes
	private int baconNumber;
	private List<String> baconPath;
	
	// Initialize attributes to provided values
	public Bacon(int baconNumber, List<String> baconPath)
	{
		this.setBaconNumber(baconNumber);
		this.baconPath = baconPath;
	}
	
	// Set methods
	public void setBaconNumber(int baconNumber)
	{
		this.baconNumber = baconNumber;
	}
	
	public void setBaconPath(List<String> baconPath)
	{
		// Iterate over baconPath and add to current baconPath variable
		ListIterator<String> baconIterator = baconPath.listIterator();
		while (baconIterator.hasNext())
		{
			this.baconPath.add(baconIterator.next());
		}
	}
	
	// Get methods
	public int getBaconNumber()
	{
		return this.baconNumber;
	}
	
	public String getBaconPath()
	{
		return this.baconPath.toString();
	}
}
