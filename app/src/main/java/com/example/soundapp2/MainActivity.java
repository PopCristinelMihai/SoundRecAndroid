package com.example.soundapp2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import android.media.AudioRecord;
import android.widget.EditText;

public class MainActivity extends AppCompatActivity {

    boolean isRecording=false;
    AudioRecord recorder=null;
    Thread recorderThread=null;
    private static final int bitsPerSample = 16;
    private static final int nChannels = 1;
    private static final int sampleRate = 44100;
    private static final int aEncoding = AudioFormat.ENCODING_PCM_16BIT;
    private int bufferSize=0;
    private static final String rawFile="rawFile.raw";
    Button record,stop_record;
    EditText editText;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bufferSize=AudioRecord.getMinBufferSize(sampleRate,nChannels,aEncoding);
        record=findViewById(R.id.record);
        stop_record=findViewById(R.id.stop_record);
        setClick();

    }

    private void setClick()
    {
        ((Button)findViewById(R.id.record)).setOnClickListener(btnClick);
        ((Button)findViewById(R.id.stop_record)).setOnClickListener(btnClick);
    }


    public void startRecord() {
        recorder=new AudioRecord(MediaRecorder.AudioSource.MIC,sampleRate,nChannels,aEncoding,AudioRecord.getMinBufferSize(sampleRate,nChannels,aEncoding));

        recorder.startRecording();
        isRecording=true;

        recorderThread=new Thread(new Runnable() {
            @Override
            public void run() {
                writeToFile();
            }
        },"Record Thread");
        recorderThread.start();
    }

    public String getTempFilename()
    {
        String path=Environment.getExternalStorageDirectory().getPath();
        File file=new File(path,"SuneteAplicatie");
        if(!file.exists()){
            file.mkdirs();
        }
        File tempFile=new File(path,rawFile);

        if(tempFile.exists())
            tempFile.delete();
        return (file.getAbsolutePath()+"/"+rawFile);
    }

    public String getFilename()
    {
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref",0);
        SharedPreferences.Editor editor=pref.edit();
        editText = findViewById(R.id.editText);
        int counter=1;
        String path=Environment.getExternalStorageDirectory().getAbsolutePath();
        File file=new File(path,"SuneteAplicatie"+"/"+editText.getText().toString().toUpperCase());




        if(!file.exists()){
            file.mkdirs();
        }

        File file2=new File(file.getAbsolutePath()+"/"+editText.getText()+counter+".wav");
        while (file2.exists())
        {
            counter++;
            file2=new File(file.getAbsolutePath()+"/"+editText.getText()+counter+".wav");
        }


        Log.i("TAG",file.getAbsolutePath()+"/"+editText.getText()+".wav");
        editor.putString("filepath",Environment.getExternalStorageDirectory().getPath()+"/"+"SuneteAplicatie"+"/"+editText.getText().toString().toUpperCase()+"/"+editText.getText().toString().toUpperCase()+counter+".wav");
        editor.commit();
        return(file.getAbsolutePath()+"/"+editText.getText().toString().toUpperCase()+counter+".wav");
    }



    public void writeToFile()
    {
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;

        try{
            os=new FileOutputStream(filename);
        }catch (FileNotFoundException fe){
         fe.printStackTrace();
        }

        int read=0;
        while(isRecording){

            read=recorder.read(data,0,bufferSize);

            try{
                os.write(data);
            }catch(IOException ioe){
                ioe.printStackTrace();
            }

        }
        try{
            os.close();
        }catch (IOException ioe2){
            ioe2.printStackTrace();
        }

    }
    private void stopRecording()
    {
        isRecording=false;
        recorder.stop();
        recorder.release();

        recorder=null;
        recorderThread=null;

        copyRAWtoFile(getTempFilename(),getFilename());
        File file = new File(getTempFilename());
        file.delete();

    }

    private void copyRAWtoFile(String inFile,String outFile)
    {
        FileInputStream in = null;
        FileOutputStream out=null;
        long totalAudioLen=0;
        long totalDataLen= totalAudioLen+36;
        long longSampleRate=sampleRate;
        int channels=1;
        long byteRate=bitsPerSample*sampleRate*channels/8;

        byte[] data=new byte[bufferSize];
        byte[] header=new byte[44];

        try{
            in=new FileInputStream(inFile);
            out=new FileOutputStream(outFile);
            totalAudioLen=in.getChannel().size();
            totalDataLen=totalAudioLen+36;

            Log.i("TAG","File size:" + totalDataLen);

            header[0] = 'R';  // RIFF/WAVE header
            header[1] = 'I';
            header[2] = 'F';
            header[3] = 'F';
            header[4] = (byte) (totalDataLen & 0xff);
            header[5] = (byte) ((totalDataLen >> 8) & 0xff);
            header[6] = (byte) ((totalDataLen >> 16) & 0xff);
            header[7] = (byte) ((totalDataLen >> 24) & 0xff);
            header[8] = 'W';
            header[9] = 'A';
            header[10] = 'V';
            header[11] = 'E';
            header[12] = 'f';  // 'fmt ' chunk
            header[13] = 'm';
            header[14] = 't';
            header[15] = ' ';
            header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
            header[17] = 0;
            header[18] = 0;
            header[19] = 0;
            header[20] = 1;  // format = 1
            header[21] = 0;
            header[22] = (byte) channels;
            header[23] = 0;
            header[24] = (byte) (longSampleRate & 0xff);
            header[25] = (byte) ((longSampleRate >> 8) & 0xff);
            header[26] = (byte) ((longSampleRate >> 16) & 0xff);
            header[27] = (byte) ((longSampleRate >> 24) & 0xff);
            header[28] = (byte) (byteRate & 0xff);
            header[29] = (byte) ((byteRate >> 8) & 0xff);
            header[30] = (byte) ((byteRate >> 16) & 0xff);
            header[31] = (byte) ((byteRate >> 24) & 0xff);
            header[32] = (byte) (2 * 16 / 8);  // block align
            header[33] = 0;
            header[34] = bitsPerSample;  // bits per sample
            header[35] = 0;
            header[36] = 'd';
            header[37] = 'a';
            header[38] = 't';
            header[39] = 'a';
            header[40] = (byte) (totalAudioLen & 0xff);
            header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
            header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
            header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

            out.write(header,0,44);

            while(in.read(data)!=-1){
                out.write(data);
            }

            in.close();
            out.close();

        }catch(FileNotFoundException fe){
            fe.printStackTrace();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }

    }

    public void openFFTActivity()
    {
        Intent intent = new Intent(this,FFTActivity.class);
        startActivity(intent);
    }

    private View.OnClickListener btnClick=new View.OnClickListener(){
        @Override
        public void onClick(View v){
            switch (v.getId()){
                case R.id.record:{
                    Log.i("TAG","Started Recording");

                    startRecord();

                    break;
                }
                case R.id.stop_record:{
                    Log.i("TAG","Stoped Recording");

                    stopRecording();

                    openFFTActivity();
                    break;
                }

            }
        }
    };



}

