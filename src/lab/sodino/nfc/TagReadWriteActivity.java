package lab.sodino.nfc;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import android.app.Activity;
import android.content.Intent;
import android.nfc.FormatException;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.format.Time;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

/**
 * @author Sodino E-mail:sodino@qq.com
 * @version Time：2014年5月6日 上午10:20:07
 */
public class TagReadWriteActivity extends Activity{
	TextView txtInfo;
	public void onCreate(Bundle savedInstanceState){
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_tag);
		txtInfo = (TextView)findViewById(R.id.txtInfo);
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
			String readResult = readFromTag(getIntent());
			
			txtInfo.setText(readResult);
			
			Date date = new Date();
			date.getTime();
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.SIMPLIFIED_CHINESE);
			String strDate = sdf.format(date);
			
			Tag tag = getIntent().getParcelableExtra(NfcAdapter.EXTRA_TAG);
			Ndef ndef = Ndef.get(tag);
			try {
				ndef.connect();
				NdefRecord ndefRecord = createTextRecord("WriteNFCTime:" + strDate, Locale.US, true);
				NdefRecord[] records = { ndefRecord };
				NdefMessage ndefMessage = new NdefMessage(records);
				ndef.writeNdefMessage(ndefMessage);
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (FormatException e) {
				e.printStackTrace();
			}
		}
	}
	
	public NdefRecord createTextRecord(String payload, Locale locale, boolean encodeInUtf8) {
	    byte[] langBytes = locale.getLanguage().getBytes(Charset.forName("US-ASCII"));
	    Charset utfEncoding = encodeInUtf8 ? Charset.forName("UTF-8") : Charset.forName("UTF-16");
	    byte[] textBytes = payload.getBytes(utfEncoding);
	    int utfBit = encodeInUtf8 ? 0 : (1 << 7);
	    char status = (char) (utfBit + langBytes.length);
	    byte[] data = new byte[1 + langBytes.length + textBytes.length];
	    data[0] = (byte) status;
	    System.arraycopy(langBytes, 0, data, 1, langBytes.length);
	    System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);
	    NdefRecord record = new NdefRecord(NdefRecord.TNF_WELL_KNOWN, NdefRecord.RTD_TEXT, new byte[0], data);
	    return record;
	}
	
	private String readFromTag(Intent intent){
	    Parcelable[] rawArray = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
	    NdefMessage mNdefMsg = (NdefMessage)rawArray[0];
	    NdefRecord mNdefRecord = mNdefMsg.getRecords()[0];
	    String readResult = "";
        if(mNdefRecord != null){
            readResult = parse(mNdefRecord);
         }
	    return readResult;
	 }
	
    public static String parse(NdefRecord record) {
    	String readResult = "";
        try {
            byte[] payload = record.getPayload();

            /*
             * payload[0] contains the "Status Byte Encodings" field, per
             * the NFC Forum "Text Record Type Definition" section 3.2.1.
             *
             * bit7 is the Text Encoding Field.
             *
             * if (Bit_7 == 0): The text is encoded in UTF-8
             * if (Bit_7 == 1): The text is encoded in UTF16
             *
             * Bit_6 is reserved for future use and must be set to zero.
             *
             * Bits 5 to 0 are the length of the IANA language code.
             */

            String textEncoding = ((payload[0] & 0200) == 0) ? "UTF-8" : "UTF-16";
            int languageCodeLength = payload[0] & 0077;

            String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            String text = new String(payload,
                    languageCodeLength + 1,
                    payload.length - languageCodeLength - 1,
                    textEncoding);
            
            readResult = text;
        } catch (UnsupportedEncodingException e) {
            // should never happen unless we get a malformed tag.
            throw new IllegalArgumentException(e);
        }
        return readResult;
    }
}
