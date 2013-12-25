package com.example.bluetoothreciever;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.EditText;  
import android.widget.Button;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity
{
TextView myLabel;
EditText myTextbox;
BluetoothAdapter mBluetoothAdapter;
BluetoothSocket mmSocket;
BluetoothDevice mmDevice;
OutputStream mmOutputStream;
InputStream mmInputStream;
Thread workerThread;
byte[] readBuffer;
int readBufferPosition;
int counter;
volatile boolean stopWorker;
String timeStamps;
SimpleDateFormat sdf;

MediaRecorder recorder;
File audiofile = null;
File file;
FileOutputStream writer;
Date startTime;

@Override
public void onCreate(Bundle savedInstanceState)
{
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);

    Button openButton = (Button)findViewById(R.id.open);
    //Button sendButton = (Button)findViewById(R.id.send);
    Button closeButton = (Button)findViewById(R.id.close);
    /*ScrollView scroller = new ScrollView(this);
    TextView tv=(TextView)findViewById(R.id.label);
    scroller.addView(tv);
    */
    myLabel = (TextView)findViewById(R.id.label);
    

    //Open Button
    openButton.setOnClickListener(new View.OnClickListener()
    {
        public void onClick(View v)
        {
            try 
            {
                findBT();
                openBT();
            }
            catch (IOException ex) { }
        }
    });

    //Send Button
    /*sendButton.setOnClickListener(new View.OnClickListener()
    {
        public void onClick(View v)
        {
            try 
            {
                sendData();
            }
            catch (IOException ex) { }
        }
    });
     */
    
    //Close button
    closeButton.setOnClickListener(new View.OnClickListener()
    {
        public void onClick(View v)
        {
            try 
            {
                closeBT();
            }
            catch (IOException ex) { }
        }
    });
}

void findBT()
{
    mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    if(mBluetoothAdapter == null)
    {
        myLabel.setText("No bluetooth adapter available");
        Log.d("varma","No bluetooth adapter available");
    }

    if(!mBluetoothAdapter.isEnabled())
    {
        Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBluetooth, 0);
    }

    Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
    if(pairedDevices.size() > 0)
    {
        for(BluetoothDevice device : pairedDevices)
        {
            if(device.getName().equals("BTBee Pro")) 
            {
                mmDevice = device;
                break;
            }
        }
    }
    myLabel.setText("Bluetooth Device Found");
    Log.d("varma","Bluetooth adapter available");
}

void openBT() throws IOException
{
    UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
   
    mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);        
    mmSocket.connect();
    mmOutputStream = mmSocket.getOutputStream();
    mmInputStream = mmSocket.getInputStream();

    beginListenForData();

    myLabel.setText("Bluetooth Opened");
    
    startRecording();
}

void beginListenForData()
{
    final Handler handler = new Handler(); 
    final byte delimiter = 10; //This is the ASCII code for a newline character

    stopWorker = false;
    readBufferPosition = 0;
    readBuffer = new byte[1024];
    workerThread = new Thread(new Runnable()
    {
        public void run()
        {                
           while(!Thread.currentThread().isInterrupted() && !stopWorker)
           {
                try 
                {
                    int bytesAvailable = mmInputStream.available();                        
                    if(bytesAvailable > 0)
                    {
                        byte[] packetBytes = new byte[bytesAvailable];
                        mmInputStream.read(packetBytes);
                        for(int i=0;i<bytesAvailable;i++)
                        {
                            byte b = packetBytes[i];
                            if(b == delimiter)
                            {
                            byte[] encodedBytes = new byte[readBufferPosition];
                            System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                            final String data = new String(encodedBytes, "US-ASCII");
                            readBufferPosition = 0;

                                handler.post(new Runnable()
                                {
                                    public void run()
                                    {
                                        myLabel.setText(data);
                                       
                                        
                                        try{
                                        	Date curDate = new Date();
                                        	String stringDate = sdf.format(curDate);
                                        	long diff = curDate.getTime() - startTime.getTime();  
                                        	BufferedWriter buf = new BufferedWriter(new FileWriter(file, true)); 
                                            buf.append(diff+":"+data);
                                            buf.newLine();
                                            buf.close();
                                            
                                        	Log.d("VARMA",data);
                                        	
                                        }
                                        catch(Exception e) { Log.e("ERROR","ERROR WRITING TO FILE"); Log.e("VARMA", "exception", e); }
                                       
                                    }
                                });
                            }
                            else
                            {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                } 
                catch (IOException ex) 
                {
                    stopWorker = true;
                }
           }
        }
    });

    workerThread.start();
}

void sendData() throws IOException
{
    String msg = myTextbox.getText().toString();
    msg += "\n";
    mmOutputStream.write(msg.getBytes());
    myLabel.setText("Data Sent");
}

void closeBT() throws IOException
{
    stopWorker = true;
    mmOutputStream.close();
    mmInputStream.close();
    mmSocket.close();
    myLabel.setText("Bluetooth Closed");
    stopRecording();
}


public void startRecording() throws IOException {

  File sampleDir = Environment.getExternalStorageDirectory();
  sdf = new SimpleDateFormat("yyyy-MM-dd-hh-mm-ss");
  startTime = new Date();
  String stringDate = sdf.format(startTime);
  Log.d("DEBUG",stringDate);
  try {
    audiofile = File.createTempFile("audio_"+stringDate+"_", ".mp3", sampleDir);
    File sdCard = Environment.getExternalStorageDirectory();
    File dir = new File (sdCard.getAbsolutePath());
    file = new File(dir, "timeStamps"+stringDate+".txt");
    Log.d("VARMA",file.toString());
    
  } catch (IOException e) {
    Log.e("VARMA", "sdcard access error");
    return;
  }
  
  if (!file.exists()) {
	   file.createNewFile();
	}

 //writer = openFileOutput(file.getName(), Context.MODE_APPEND);

  recorder = new MediaRecorder();
  recorder.setAudioSource(MediaRecorder.AudioSource.MIC);
  recorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
  recorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
  recorder.setOutputFile(audiofile.getAbsolutePath());
  recorder.prepare();
  recorder.start();
}

public void stopRecording() {
  recorder.stop();
  recorder.release();
  try{
	  //writer.close();
  }
  catch(Exception e){}
  
  addRecordingToMediaLibrary();
}

protected void addRecordingToMediaLibrary() {
  ContentValues values = new ContentValues(4);
  long current = System.currentTimeMillis();
  values.put(MediaStore.Audio.Media.TITLE, "audio" + audiofile.getName());
  values.put(MediaStore.Audio.Media.DATE_ADDED, (int) (current / 1000));
  values.put(MediaStore.Audio.Media.MIME_TYPE, "audio/3gpp");
  values.put(MediaStore.Audio.Media.DATA, audiofile.getAbsolutePath());
  ContentResolver contentResolver = getContentResolver();

  Uri base = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
  Uri newUri = contentResolver.insert(base, values);

  sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, newUri));
  Toast.makeText(this, "Added File " + newUri, Toast.LENGTH_LONG).show();
}


}