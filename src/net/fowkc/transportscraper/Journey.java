package net.fowkc.transportscraper;

import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.DateTimeFormat;

public class Journey implements Comparable<Journey>
{

	private String stopName;
	private String provider;
	private DateTime timetabled;
	private DateTime actual;
	
	public Journey(String _stopName, String _provider, DateTime _timetabled, DateTime _actual)
	{
		stopName = _stopName;
		provider = _provider;
		timetabled = _timetabled;
		actual = _actual;
	}
	
	public Journey(String _stopName, String _provider, DateTime _actual)
	{
		stopName = _stopName;
		provider = _provider;
		actual = _actual;
		timetabled = null;
	}
	
	public String stopName()
	{
		return stopName;
	}
	
	public String transportName()
	{
		return provider;
	}
	
	public String toString()
	{
		String s;
		
		if (timetabled == null)
		{
			s = provider + " from " + stopName + " leaving at " + 
				actual.toString(DateTimeFormat.forPattern("HH:mm")) +
				" in " + Integer.toString(this.remainingTime()) + " minutes.";
		}
		else
		{
			s = provider + " from " + stopName + " leaving at " + 
				actual.toString(DateTimeFormat.forPattern("HH:mm")) +
				" in " + Integer.toString(this.remainingTime()) + " minutes. "
				+ Integer.toString(this.delay()) + " minutes late.";
		}
		
		return s;
	}
	
	public int remainingTime()
	{
		Duration d = new Duration(new DateTime(), actual);
	
		return (int) d.getStandardMinutes();
	}
	
	public DateTime leavingAt()
	{
		return actual;
	}
	
	public DateTime timetabledAt()
	{
		return timetabled;
	}
	
	public int delay()
	{
		if (timetabled != null)
		{
			return new Duration(timetabled, actual).toStandardMinutes().getMinutes();
		}
		else
		{
			return 0;
		}
		
	}
	
	public boolean isDelayed()
	{
		return this.delay() > 0;
	}

	public String leavingTimeFormatted(String format)
	{
		return actual.toString(DateTimeFormat.forPattern(format));
	}
	
	public void setNextDay()
	{
		actual.plusDays(1);
		if (timetabled != null)
		{
			timetabled.plusDays(1);
		}
	}
	
	public int compareTo(Journey compareJourney)
	{
		int compareQuantity = ((Journey) compareJourney).remainingTime(); 
 
		//ascending order
		return this.remainingTime() - compareQuantity;
	}
}
