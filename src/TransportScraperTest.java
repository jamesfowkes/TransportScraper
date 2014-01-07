import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import net.fowkc.transportscraper.Journey;
import net.fowkc.transportscraper.JourneyManager;
import net.fowkc.transportscraper.NextBusesScraper;
import net.fowkc.transportscraper.TrainTimesScraper;

public class TransportScraperTest
{
	public static void main(String[] args)
	{
		NextBusesScraper busesScraper = new NextBusesScraper();
		TrainTimesScraper trainsScraper = new TrainTimesScraper();
		
		URL urls[] = {
				busesScraper.getURL("ntsawmgw"),
				busesScraper.getURL("ntsdatat"),
				trainsScraper.getURL("BEE", "NOT")
		};
		
		Journey hawthornGroveJourneys[] = new Journey[0];
		Journey padgeRoadJourneys[] = new Journey[1];
		Journey trainJourneys[] = new Journey[2];
		
		try
		{
			hawthornGroveJourneys = busesScraper.parseData("Hawthorn Grove", readURLData(urls[0]));
			padgeRoadJourneys = busesScraper.parseData("Padge Road", readURLData(urls[1]));
			trainJourneys = trainsScraper.parseData("Beeston", readURLData(urls[2]));
		}
		catch (Exception e)
		{
			// This is fine.
		}
		
		JourneyManager journeyManager = new JourneyManager();
		
		journeyManager.addJourneys(hawthornGroveJourneys);
		journeyManager.addJourneys(padgeRoadJourneys);
		journeyManager.addJourneys(trainJourneys);
		
		journeyManager.sortJourneys();
		System.out.println(journeyManager.toString());
		
	}
	
	static String readURLData(URL url) throws IOException
	{
		StringBuilder data = new StringBuilder();

		BufferedReader in = new BufferedReader(
	        new InputStreamReader(url.openStream()));

		String inputLine;
		while ((inputLine = in.readLine()) != null)
	    	data.append(inputLine);
	    
	    in.close();
	
		return data.toString();
	}
}
	