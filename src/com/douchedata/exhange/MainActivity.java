package com.douchedata.exhange;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import org.xmlpull.v1.XmlPullParserException;

import android.os.*;
import android.app.*;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.*;
import android.widget.*;
import android.widget.AdapterView.OnItemSelectedListener;

public class MainActivity extends Activity {
	private TextView resultsText;
	private Spinner fromSpinner;
	private Spinner toSpinner;
	private ArrayAdapter<String> currencyAdapter;
	private EditText amountEdit;
	
	private ExchangeRates exchangeRates;
	public static final String BANKURL = "http://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		this.exchangeRates = new ExchangeRates();
		
		resultsText = (TextView) findViewById(R.id.resultsTextView);
		fromSpinner = (Spinner) findViewById(R.id.spinner1);
		toSpinner = (Spinner) findViewById(R.id.spinner2);
		amountEdit = (EditText) findViewById(R.id.editText1);
		
		updateResults();
		
		// Read the rates from the SD card and download it from
		// the ECB if it's not up to date
		try {
			ECBParser parser = new ECBParser();
			boolean upToDate = parser.parse(readFromSd(), exchangeRates);
//			upToDate = !upToDate;
			if (upToDate) {
				// Up to date, continue using rates from SD card.
				showToast("From SD");
				showToast("parser: " + parser.getDateString());
				connectListeners();
				currencyAdapter.notifyDataSetChanged();
			} else if (!upToDate) {
				// Not up to date, download rates from ECB.
				showToast("From Network");
				new DownloadXmlTask().execute(BANKURL);
				showToast("parser: " + parser.getDateString());
			}
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Asynchronously update exchange rates, either from the SD card or the ECB.
	 */
	private class DownloadXmlTask extends AsyncTask<String, Void, ExchangeRates> {
		protected ExchangeRates doInBackground(String... url) {
			try {
				exchangeRates = loadXmlFromNetwork(BANKURL);
					
				} catch (XmlPullParserException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			
			return exchangeRates;
		}
		
		// onPostExecute displays the results of the AsyncTask.
		protected void onPostExecute(ExchangeRates exchangeRates) {
			connectListeners();
			currencyAdapter.notifyDataSetChanged();
		}
	}

	/**
	 * Saves the stream to SD card and invokes an ECBparser object.
	 */
	private ExchangeRates loadXmlFromNetwork(String urlString) throws XmlPullParserException, IOException {
		InputStream stream = null;
		ECBParser parser = new ECBParser();
		
		try {
			stream = downloadStream(urlString);
			saveToSd(stream);
			parser.parse(readFromSd(), exchangeRates);
		} finally {
			if (stream != null) {
				stream.close();
			}
		}

		return exchangeRates;
	}
	
	/**
	 * Given a string representation of a URL, sets up a connection and gets
	 * an input stream.
	 */
	private InputStream downloadStream(String urlString) throws IOException {
		URL url = new URL(urlString);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setReadTimeout(10000 /* milliseconds */);
		conn.setConnectTimeout(15000 /* milliseconds */);
		conn.setRequestMethod("GET");
		conn.setDoInput(true);
		// Starts the query
		conn.connect();
		InputStream stream = conn.getInputStream();
		return stream;
	}

	/**
	 * Save the exchange rate file to the SD card
	 */
	private void saveToSd(InputStream stream) {
		String fileName = "exchange_rates.xml";

		File sdCard = android.os.Environment.getExternalStorageDirectory();               
		File dir = new File (sdCard.getAbsolutePath() + "/Android/data/com.douchedata.exchange/files/");
		File file = new File(dir, fileName);
		
		try {
			if ((file.exists() != false))
				file.createNewFile();
			
			FileOutputStream fos = new FileOutputStream(file);
			
			byte[] buffer = new byte[1024];
			
			int len = 0;
			while ((len = stream.read(buffer)) > 0) {
				fos.write(buffer, 0, len);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Load a saved version of exchange rates from the SD card.
	 */
	private InputStream readFromSd() {
		String fileName = "exchange_rates.xml";
		FileInputStream stream = null;
		File sdCard = android.os.Environment.getExternalStorageDirectory();               
		File dir = new File (sdCard.getAbsolutePath() + "/Android/data/com.douchedata.exchange/files/");
		Log.v("sd", dir.getAbsolutePath());
		File file = new File(dir, fileName);

		if (dir.exists() == false) {
			dir.mkdirs();
		}
		
		try {
			if (file.exists())
				stream = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return stream;
	}


	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	/**
	 * Reads entries and sends it to ExchangeRates for conversion.
	 * Updates the TextView with the resulting String.
	 */
	public void updateResults() {
		// Do not update when there is no amount entered
		if (amountEdit.getText().toString().equalsIgnoreCase(""))
			return;
		
		// Transform .42 to 0.42
		if (amountEdit.getText().toString().equalsIgnoreCase(".")) {
			amountEdit.setText("0.");
			amountEdit.setSelection(amountEdit.getText().length());
		}
			
		int from = (int) fromSpinner.getSelectedItemId();
		int to = (int) toSpinner.getSelectedItemId();
		Double amount = Double.parseDouble(amountEdit.getText().toString());
		Double result = exchangeRates.convert(from, to, amount);
		
		// Build results string and update the view
		String text = "";
		text += exchangeRates.getCurrency(from) + " ";
		text += amount + "\n";
		text += exchangeRates.getCurrency(to) + " ";
		text += result.toString();
		resultsText.setText(text);
	}
	
	public void updateRates(View v) {
		new DownloadXmlTask().execute(BANKURL);
	}
	
	public void clickConvertButton(View v) {
		updateResults();
	}
	
	public void showToast(String msg) {
		Toast toast = Toast.makeText(this, msg, Toast.LENGTH_SHORT);
		toast.show();
	}

	/**
	 * Setup ArrayAdapter from currency and connect to Spinners,
	 * the same for EditText amount.
	 */
	private void connectListeners() {
		currencyAdapter =
				new ArrayAdapter<String>(
						this, 
						android.R.layout.simple_spinner_dropdown_item,
						exchangeRates.getCurrencyList()
						);
		
		fromSpinner.setAdapter(currencyAdapter);
		toSpinner.setAdapter(currencyAdapter);
		
		fromSpinner.setOnItemSelectedListener(new OnCurrencySelectedListener());
		toSpinner.setOnItemSelectedListener(new OnCurrencySelectedListener());
		amountEdit.addTextChangedListener(new EditTextListener());
	}
	
	/**
	 * Listener for Spinners
	 */
	public class OnCurrencySelectedListener implements OnItemSelectedListener {
		@Override
		public void onItemSelected(AdapterView<?> parent, View view, int pos,
				long id) {
			updateResults();
		}
		@Override
		public void onNothingSelected(AdapterView<?> arg0) {
			updateResults();
		}
	}
	
	/**
	 * Automatically updates the view on text entry
	 */
	public class EditTextListener implements TextWatcher {
		public void afterTextChanged(Editable s) {
			updateResults();
		}
		public void beforeTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
		public void onTextChanged(CharSequence arg0, int arg1, int arg2, int arg3) { }
	}
}
