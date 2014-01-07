package net.fowkc.transportscraper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.joda.time.DateTime;

public class JourneyManager
{

	private List<Journey> journeys;
	
	public JourneyManager()
	{
		journeys = new ArrayList<Journey>();
	}
	
	public void addJourneys(Journey[] newJourneys)
	{
		DateTime nowMinusFiteenMins = new DateTime().minusMinutes(15);
		
		if (newJourneys != null)
		{
			for (Journey j : newJourneys)
			{
				if (j != null)
				{
					if (j.leavingAt().isBefore(nowMinusFiteenMins))
					{
						j.setNextDay();
					}
					journeys.add(j);
				}
			}
		}
	}
	
	public void addJourneys(List<Journey> newJourneys)
	{
		for (Journey j : newJourneys)
		{
			if (j != null)
			{
				journeys.add(j);
			}
		}
	}
	
	public List<Journey> journeys()
	{
		return journeys;
	}
	
	public void clearJourneys()
	{
		journeys = new ArrayList<Journey>();
	}
	
	public boolean clearJourneysWithLessThanMinutesRemaining(int minutes)
	{
		boolean journeyRemoved = false;
		
		Iterator<Journey> i = journeys.iterator();
		while (i.hasNext())
		{
			Journey j = i.next();
			if (j.remainingTime() <= minutes)
			{
				i.remove();
				journeyRemoved = true;
			}
		}
		
		return journeyRemoved;
	}
	
	public String toString()
	{
		StringBuilder rtn = new StringBuilder();
		
		
		for (Journey j : journeys)
		{
			rtn.append( j.toString() );
			rtn.append( System.getProperty("line.separator") );
		}
		
		return rtn.toString();
	}
	
	public void sortJourneys()
	{
		Collections.sort(journeys);
	}
}