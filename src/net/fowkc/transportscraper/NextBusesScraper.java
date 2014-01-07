package net.fowkc.transportscraper;

import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

import org.ccil.cowan.tagsoup.jaxp.SAXParserImpl;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.DefaultHandler;

public class NextBusesScraper
{
	public class NextBusesScraperException extends Exception {
		private static final long serialVersionUID = -7560594010501437884L;
		public NextBusesScraperException() { super(); }
		  public NextBusesScraperException(String message) { super(message); }
		  public NextBusesScraperException(String message, Throwable cause) { super(message, cause); }
		  public NextBusesScraperException(Throwable cause) { super(cause); }
		}

	private static final String url = "http://www.nextbuses.mobi/WebView/BusStopSearch/BusStopSearchResults/%s?currentPage=0";
	
	public NextBusesScraper() {}
	
	public URL getURL(String stopID)
	{
		try {
			return new URL(String.format(url, stopID));
		} catch (MalformedURLException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public Journey[] parseData(String stopName, String rawData) throws Exception
	{
		try
		{
			ScrapedBusData data = new ScrapedBusData(rawData);
			return getJourneys(stopName, data.getRequestDateString(), data.getJourneyStrings() );
		}
		catch (NullPointerException ex)
		{
			throw new NextBusesScraperException(rawData);
		}
		catch (Exception ex)
		{
			throw ex;
		}
	}
	
	Journey[] getJourneys(String stopName, String scrapedReqDateString, String[] scrapedJourneys) throws ParseException
	{
		Journey journeys[] = new Journey[scrapedJourneys.length/2];
		
		for (int i = 0; i < journeys.length; i++)
		{
			DateTime journeyStartDate = buildJourneyStartTime(scrapedReqDateString, scrapedJourneys[(i*2)+1]);
			journeys[i] = new Journey(stopName, scrapedJourneys[i*2], journeyStartDate);
		}
		
		return journeys;
	}
	
	private DateTime buildJourneyStartTime(String startDate, String journeyTime) throws IllegalArgumentException
	{
		
		DateTime leaving;
		
		try
		{
			DateTimeFormatter ft = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm");
			String datetimeString = startDate.trim().substring(0, 10) + 
					" " + 
					journeyTime.substring(journeyTime.length() - 5);
						
			leaving = ft.parseDateTime(datetimeString);
		}
		catch (IllegalArgumentException ex1)
		{
			try
			{
				DateTimeFormatter ft = DateTimeFormat.forPattern("dd/MM/yyyy HH:mm (EEE)");
				String datetimeString = 
						startDate.trim().substring(0, 10) + 
						" " + 
						journeyTime.substring(journeyTime.length() - 11);
				leaving = ft.parseDateTime(datetimeString);
				leaving.plusDays(1);
			}
			catch (IllegalArgumentException ex2)
			{
				throw ex2;
			}
		}
		return leaving;
	}
}

class ScrapedBusData
{
	enum State
	{
		FINDING_REQ_DATE,
		FINDING_TABLE,
		FINDING_DATA,
		CACHING_DATE,
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
	
	public ScrapedBusData(String data) throws Exception 
	{
		state = State.FINDING_REQ_DATE;
		dataCache = new StringBuilder();
		journeyStrings = new ArrayList<String>();
		
		SAXParserImpl saxParser = SAXParserImpl.newInstance(null);
	
		DefaultHandler handler = new DefaultHandler()
		{
			public void startElement(String uri, String localName, String name, Attributes a)
			{
				switch(state)
				{
				case FINDING_REQ_DATE:
					if ( eleIsDateTime(name, a) )
					{
						state = State.CACHING_DATE;
					}
				case FINDING_TABLE:
					if ( eleIsStartOfTable(name, a) )
					{
						state = State.FINDING_DATA;
					}
					break;
				case FINDING_DATA:
					if ( eleIsStartOfData(name, a) )
					{
						state = State.CACHING_JOURNEY;
					}
					break;
				case CACHING_JOURNEY:
				case CACHING_DATE:
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
				case FINDING_DATA:
					if ( localName.equalsIgnoreCase("table") )
					{
						state = State.FINISHED;
					}
					break;
				case CACHING_DATE:
					if ( localName.equalsIgnoreCase("br") )
					{
						storeDateAndClearBuffer();
						state = State.FINDING_TABLE;
					}
				case CACHING_JOURNEY:
					if ( localName.equalsIgnoreCase("p") )
					{
						storeJourneyAndClearBuffer();
						state = State.FINDING_DATA;
					}
					break;
				case FINDING_REQ_DATE:
				case FINISHED:
					break;
				}
			}
			
			public void characters(char ch[], int start, int length)
			{
				if ((state == State.CACHING_JOURNEY) || (state == State.CACHING_DATE))
				{
					dataCache.append(ch, start, length);
				}
			}
		};
		
        saxParser.parse(new InputSource(new StringReader(data)), handler);
	}
	
	private Boolean eleIsDateTime(String name, Attributes a)
	{
		return name.equalsIgnoreCase("h5");
	}
	
	private Boolean eleIsStartOfTable(String name, Attributes a)
	{
		return name.equalsIgnoreCase("table") && a.getValue("class").equalsIgnoreCase("BusStops");
	}
	
	private Boolean eleIsStartOfData(String name, Attributes a)
	{
		return name.equalsIgnoreCase("p") && a.getValue("class").equalsIgnoreCase("Stops");
	}
	
	private void storeDateAndClearBuffer()
	{
		String newString = dataCache.toString();
		requestDate = newString;
		dataCache = new StringBuilder();
	}
	
	private void storeJourneyAndClearBuffer()
	{
		String newString = dataCache.toString().replaceAll("\u00a0"," ");
		journeyStrings.add( newString );
		dataCache = new StringBuilder();
	}
}