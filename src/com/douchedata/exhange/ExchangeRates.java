package com.douchedata.exhange;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import android.util.Log;

public class ExchangeRates {
	private Date date;
	private ArrayList<String> currencies;
	private ArrayList<Double> rates;
	String FILENAME = "saved_exchange_rates";
	
	public ExchangeRates() {
		this.date = new Date();
		this.currencies = new ArrayList<String>();
		this.rates = new ArrayList<Double>();
		addCurrency("EUR");
		addRate((double) 1);
	}
	
	public Double convert(int from, int to, Double amount) {
		Double euro = amount / rates.get(from);
		Double result = euro * rates.get(to);
		result = round(result, 3);
		return result;
	}
	
	private static double round(double value, int places) {
	    if (places < 0) throw new IllegalArgumentException();

	    long factor = (long) Math.pow(10, places);
	    value = value * factor;
	    long tmp = Math.round(value);
	    return (double) tmp / factor;
	}
	
	public void addCurrency(String c) {
		currencies.add(c);
	}
	
	public void addRate(Double r) {
		rates.add(r);
	}
	
	public Double getRate(int n) {
		return rates.get(n);
	}
	
	public String getCurrency(int n) {
		return currencies.get(n);
	}
	
	public String getDateString() {
		SimpleDateFormat newFormat = new SimpleDateFormat("yyyy-MM-dd");
		String dateString = newFormat.format(this.date);
		return dateString;
	}
	
	public ArrayList<String> getCurrencyList() {
		return currencies;
	}
	
	public void toXml() {
		StringBuilder builder = new StringBuilder();
		
		// Remove EUR
		currencies.remove(0);
		rates.remove(0);
		
		builder.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
		builder.append("<Cube>\n");
		
		for (String s : currencies) {
			builder.append("  <Cube ");
			builder.append("currency='" + s + "' ");
			builder.append("rate='" + rates.remove(0).toString() + "'");
			builder.append("/>\n");
		}
		
		builder.append("</Cube>");
		Log.v("xml", builder.toString());
	}
}
