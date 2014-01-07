package net.fowkc.transportscraper;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

public class TrainTimesScraper
{

	private static final String url = "http://traintimes.org.uk/live/%s/%s";
	
	public TrainTimesScraper() {}
	
	public URL getURL(String _from, String _to)
	{
		try {
			return new URL(String.format(url, _from, _to));
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Journey[] parseData(String fromName, String rawData)
	{
		try	
		{
			ScrapedTrainData data = new ScrapedTrainData(rawData);
			return getJourneys(fromName, data.getRequestDateString(), data.getJourneyStrings() );
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return null;
		}
	}
	
	Journey[] getJourneys(String fromName, String reqDate, String[] scrapedJourneys)
	{
		Journey journeys[] = new Journey[scrapedJourneys.length];
		
		for (int i = 0; i < journeys.length; i++)
		{
			try
			{
				DateTime journeyTimetabled = getJourneyTimetabledDateTime(reqDate, scrapedJourneys[i]);
				DateTime journeyActual = null;
				try
				{
					journeyActual = getJourneyActualDateTime(reqDate, scrapedJourneys[i]);
				}
				catch (IllegalArgumentException ex)
				{
					// This is fine
				}
				
				if (journeyActual != null)
				{
					journeys[i] = new Journey(fromName, "Train", journeyTimetabled, journeyActual);
				}
				else
				{
					journeys[i] = new Journey(fromName, "Train", journeyTimetabled);
				}
			}
			catch (IllegalArgumentException ex)
			{
				ex.printStackTrace();
				journeys[i] = null;
			}
		}
		
		return journeys;
	}
	
	private DateTime getJourneyActualDateTime(String startDate, String journeyTime) throws IllegalArgumentException
	{
		return datetimeFromString(startDate, journeyTime.substring(5, 10));
	}
	
	private DateTime getJourneyTimetabledDateTime(String startDate, String journeyTime) throws IllegalArgumentException
	{
		return datetimeFromString(startDate, journeyTime.substring(0, 5));
	}
	
	
	private DateTime datetimeFromString(String startDate, String journeyTime) throws IllegalArgumentException
	{
		DateTime leaving;
		
		try
		{
			DateTimeFormatter ft = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm");
			String datetimeString = startDate.trim().substring(0, 10) + " " + journeyTime.substring(0, 5);
			leaving = ft.parseDateTime(datetimeString);
		}
		catch (IllegalArgumentException ex)
		{
			throw ex;
		}
		
		return leaving;
	}
}

class ScrapedTrainData
{
	enum State
	{
		FINDING_TABLE,
		FINDING_DATA_ROW,
		FINDING_FIRST_TD,
		CACHING_JOURNEY,
		FINISHED
	};
	
	private State state;
	private StringBuilder dataCache;
	private String requestDate;
	private List<String> journeyStrings;
	
	public String getRequestDateString()
	{
		return requestDate;
	}
	
	public String[] getJourneyStrings()
	{
		return journeyStrings.toArray(new String[journeyStrings.size()]);
	}
	
	public ScrapedTrainData(String data) throws Exception 
	{
		state = State.FINDING_TABLE;
		dataCache = new StringBuilder();
		journeyStrings = new ArrayList<String>();
		requestDate = new DateTime().toString(DateTimeFormat.forPattern("dd/MM/yyyy"));
		
		SAXParserImpl saxParser = SAXParserImpl.newInstance(null);
	
		DefaultHandler handler = new DefaultHandler()
		{
			public void startElement(String uri, String localName, String name, Attributes a)
			{
				switch(state)
				{
				case FINDING_TABLE:
					if ( eleIsStartOfTable(name, a) )
					{
						state = State.FINDING_DATA_ROW;
					}
					break;
				case FINDING_DATA_ROW:
					if ( eleIsStartOfRow(name, a) )
					{
						state = State.FINDING_FIRST_TD;
					}
					break;
				case FINDING_FIRST_TD:
					if ( eleIsTableCell(name, a) )
					{
						state = State.CACHING_JOURNEY;
					}
				case CACHING_JOURNEY:
				case FINISHED:
					break;
				}
			}
			
			public void endElement(String uri, String localName, String qName)
			{
				switch(state)
				{
				case FINDING_TABLE:
					break;
				case FINDING_DATA_ROW:
					if ( localName.equalsIgnoreCase("table") )
					{
						state = State.FINISHED;
					}
					break;
				case CACHING_JOURNEY:
					if ( localName.equalsIgnoreCase("td") )
					{
						storeJourneyAndClearBuffer();
						state = State.FINDING_DATA_ROW;
					}
					break;
				case FINDING_FIRST_TD:
				case FINISHED:
					break;
				}
			}
			
			public void characters(char ch[], int start, int length)
			{
				if (state == State.CACHING_JOURNEY)
				{
					dataCache.append(ch, start, length);
				}
			}
		};
		
        saxParser.parse(new InputSource(new StringReader(data)), handler);
	}
	
	private Boolean eleIsStartOfTable(String name, Attributes a)
	{
		return name.equalsIgnoreCase("table") && a.getValue("id").equalsIgnoreCase("liveboard");
	}
	
	private Boolean eleIsStartOfRow(String name, Attributes a)
	{
		return name.equalsIgnoreCase("tr");
	}
	
	private Boolean eleIsTableCell(String name, Attributes a)
	{
		return name.equalsIgnoreCase("td");
	}
	
	private void storeJourneyAndClearBuffer()
	{
		String newString = dataCache.toString().replaceAll("\u00a0"," ");
		journeyStrings.add( newString );
		dataCache = new StringBuilder();
	}
}