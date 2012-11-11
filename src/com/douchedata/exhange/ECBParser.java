package com.douchedata.exhange;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

public class ECBParser {
	public static final String ITEM = "Cube";
	public static final String DATE = "time";
	public static final String POST = "currency";
	public static final String POST2 = "rate";
	private XmlPullParser parser;
	
	public ECBParser() throws XmlPullParserException {	
		parser = XmlPullParserFactory.newInstance().newPullParser();
	}
	
	public String getDateString() {
		SimpleDateFormat newFormat = new SimpleDateFormat("yyyy-MM-dd");
		String dateString = newFormat.format(new Date());
		return dateString;
	}
	
	/**
	 * Parses the XML stream and takes currencies and rates and stores
	 * them in an ExchangeRates object.
	 * 
	 * The function returns true if the rates are up to date.
	 */
	public boolean parse(InputStream stream, ExchangeRates exchangeRates) throws XmlPullParserException, IOException {
		parser.setInput(stream, null);
		int parseEvent = parser.getEventType();
		boolean upToDate = false;

		while (parseEvent != XmlPullParser.END_DOCUMENT) {
			switch (parseEvent) {
			case XmlPullParser.START_DOCUMENT:
				//
				break;
			case XmlPullParser.END_DOCUMENT:
				//
				break;
			case XmlPullParser.START_TAG:
				String tagName = parser.getName();

				if (tagName.equalsIgnoreCase(ITEM)) {
					String currency = parser.getAttributeValue(null, POST);
					if (currency != null)
						exchangeRates.addCurrency(currency);

					String rate = parser.getAttributeValue(null, POST2);
					if (rate != null) {
						Double num = Double.valueOf(rate);
						exchangeRates.addRate(num);
					}
					
					// Is the XML file up to date?
					String date = parser.getAttributeValue(null, DATE);
					if (date != null) {
						if (date.equalsIgnoreCase(this.getDateString()))
							upToDate = true;
					}
				}
				break;
			case XmlPullParser.END_TAG:
				//
				break;
			case XmlPullParser.TEXT:
				//
				break;
			default:
				//
			}

			parseEvent = parser.next();
		}
		stream.close();
		return upToDate;
	}
}