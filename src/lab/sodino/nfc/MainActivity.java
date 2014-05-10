package lab.sodino.nfc;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Locale;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.NfcAdapter.CreateNdefMessageCallback;
import android.nfc.NfcAdapter.OnNdefPushCompleteCallback;
import android.nfc.NfcEvent;
import android.nfc.tech.Ndef;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcelable;
import android.telephony.TelephonyManager;
import android.text.format.Time;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements CreateNdefMessageCallback ,OnNdefPushCompleteCallback{
	private TextView txtInfo;
	private StringBuilder sb = new StringBuilder();
	private NfcAdapter nfcAdapter;
	private boolean isNfcEnable = false;
	private boolean isNdefPushEnabled = false;
	private TextView txtPushContent;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		txtInfo = (TextView)findViewById(R.id.txtInfo);
		txtPushContent = (TextView) findViewById(R.id.txtPushContent);
		
		nfcAdapter = NfcAdapter.getDefaultAdapter(this);
		String result = initNfcAdapter(nfcAdapter);
		sb.append(result);
		
		txtInfo.setText(result);
	}
	
    private String initNfcAdapter(NfcAdapter adapter){
    	StringBuilder sb = new StringBuilder();
    	
    	// nfc 功能是否开启
    	if (nfcAdapter == null) {
    		sb.append("支持NFC功能:"+ false +"\n");
    		return sb.toString();
    	} else {
    		sb.append("支持NFC功能:"+ true +"\n");
    	}
    	isNfcEnable = nfcAdapter.isEnabled();
   		sb.append("NFC是否开启:" + isNfcEnable +"\n");
    	
    	isNdefPushEnabled = nfcAdapter.isNdefPushEnabled();
   		sb.append("Android Beam是否开启：" + isNdefPushEnabled + "\n");
   		
   		nfcAdapter.setNdefPushMessageCallback(this, this);
   		nfcAdapter.setOnNdefPushCompleteCallback(this, this);
   		
    	return sb.toString();
    }

    @Override
    public void onResume() {
    	super.onResume();
    	Log.d("ANDROID_LAB", "onResume...action=" + getIntent().getAction());
    	if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
    		processIntent(getIntent());
    	}
    }
    
    private void processIntent(Intent intent){
    	Log.d("ANDROID_LAB", "processIntent...");
    	Bitmap bitmap = null;
        Parcelable[] ndefMsgs = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
        if (ndefMsgs != null) {
        	Log.d("ANDROID_LAB", "ndefMsgs.size=" + ndefMsgs.length);
        	String str = "";
        	for (int i = 0;i < ndefMsgs.length;i ++) {
        		NdefMessage msg = (NdefMessage) ndefMsgs[i];
        		NdefRecord[] records = msg.getRecords();
        		if (records != null) {
        			Log.d("ANDROID_LAB", "records.size=" + records.length);
        			for (int k = 0;k < records.length; k ++) {
        				NdefRecord record = records[k];
        				byte[] data = record.getPayload();
        				if (data != null) {
        					if (k == 0) {
        						str += new String(data);
        					} else if (k == 1){
        						bitmap = BitmapFactory.decodeByteArray(data, 0, data.length); 
        					}
        				}
            		}
        		}
        	}
        	txtPushContent.setText(str);
        	if (bitmap != null) {
        		txtPushContent.setCompoundDrawablesWithIntrinsicBounds(null, null, null, new BitmapDrawable(bitmap));
        	}
        }
    }
    @Override
    public void onNewIntent(Intent intent) {
    	Log.d("ANDROID_LAB", "onNewIntent...action=" + intent.getAction());
        setIntent(intent);
    }
    
	@Override
	public NdefMessage createNdefMessage(NfcEvent event) {
		Log.d("ANDROID_LAB", "createNdefMessage thread.name=" + Thread.currentThread().getName());
		StringBuilder sb = new StringBuilder();
		sb.append(Build.MODEL+"(");//手机型号
		sb.append(Build.VERSION.SDK_INT +",");//SDK版本号
		sb.append(Build.VERSION.RELEASE+")");//Firmware/OS 版本号
		
		sb.append("发送来一条NFC消息, 时间是");
		Time time = new Time();
		time.setToNow();
		sb.append(time.format("%H:%M:%S"));
		
		Bitmap bmp=BitmapFactory.decodeResource(getResources(), R.drawable.and);
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		bmp.compress(Bitmap.CompressFormat.PNG, 100, baos);
		byte[] bmpData = baos.toByteArray();
		
		NdefRecord[] records = new NdefRecord[2];
		records[0] = NdefRecord.createMime("application/lab.sodino.nfc", sb.toString().getBytes()); //文本消息
		records[1] = NdefRecord.createMime("application/lab.sodino.nfc", bmpData); // 图片消息
//		records[2] = NdefRecord.createApplicationRecord("com.tencent.mobileqq");
		
		NdefMessage msg = new NdefMessage(records);
		
		return msg;
	}
	
	private NdefRecord createMime(String mimeType, byte[] mimeData){
		if (mimeType == null) {
			throw new NullPointerException("mimeType is null");
		}

	        mimeType = normalizeMimeType(mimeType);
	        if (mimeType.length() == 0) throw new IllegalArgumentException("mimeType is empty");
	        int slashIndex = mimeType.indexOf('/');
	        if (slashIndex == 0) throw new IllegalArgumentException("mimeType must have major type");
	        if (slashIndex == mimeType.length() - 1) {
	            throw new IllegalArgumentException("mimeType must have minor type");
	        }
	        // missing '/' is allowed

	        // MIME RFCs suggest ASCII encoding for content-type
	        byte[] typeBytes = null;
			try {
				typeBytes = mimeType.getBytes("US_ASCII");
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
	        return new NdefRecord(NdefRecord.TNF_MIME_MEDIA, typeBytes, null, mimeData);
	}
	
	public static String normalizeMimeType(String type) {
	    if (type == null) {
	        return null;
	    }

	    type = type.trim().toLowerCase(Locale.ROOT);

	    final int semicolonIndex = type.indexOf(';');
	    if (semicolonIndex != -1) {
	        type = type.substring(0, semicolonIndex);
	    }
	    return type;
	}
	@Override
	public void onNdefPushComplete(NfcEvent paramNfcEvent) {
		Log.d("ANDROID_LAB", "onNdefPushComplete thread.name=" + Thread.currentThread().getName());
		Toast.makeText(this, "Push Msg Successed!", Toast.LENGTH_LONG).show();
	}
}