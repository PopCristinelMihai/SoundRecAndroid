
package com.example.soundapp2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;



import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;


public class FFTActivity extends AppCompatActivity {
    private static final String TAG ="";
    private long myDataSize;
    private String myPath;
    private long myChunkSize;
    private long mySubChunk1Size;
    private int myFormat;
    private long myChannels;
    private long mySampleRate;
    private long myByteRate;
    private int myBlockAlign;
    private int myBitsPerSample;

    SharedPreferences sharedPreferences;

    ApacheFFT apacheFFT=new ApacheFFT();
    File fileIn ;//= new File(Environment.getExternalStorageDirectory().getPath()+"/"+"SuneteAplicatie"+"/"+"test"+".wav");

    DataInputStream inFile=null;
    public byte[] myData;
    WindowFunction w = new HanningWindow();




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fft);
        byte[] tmpLong=new byte[4];
        byte[] tmpInt = new byte[2];
        File temp;
        sharedPreferences=getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        String filepath=sharedPreferences.getString("filepath",null);

        try{

            fileIn=new File(filepath);
            inFile= new DataInputStream(new FileInputStream(fileIn));



            String chunkID = "" + (char)inFile.readByte() + (char)inFile.readByte() + (char)inFile.readByte() + (char)inFile.readByte();




            inFile.read(tmpLong); // read the ChunkSize
            myChunkSize = byteArrayToLong(tmpLong);

            String format = "" + (char)inFile.readByte() + (char)inFile.readByte() + (char)inFile.readByte() + (char)inFile.readByte();

            // print what we've read so far
            //System.out.println("chunkID:" + chunkID + " chunk1Size:" + myChunkSize + " format:" + format); // for debugging only
            Log.i("TAG","==========================================");

            Log.i("TAG","chunkID:" + chunkID + " chunk1Size:" + myChunkSize + " format:" + format);


            String subChunk1ID = "" + (char)inFile.readByte() + (char)inFile.readByte() + (char)inFile.readByte() + (char)inFile.readByte();

            inFile.read(tmpLong); // read the SubChunk1Size
            mySubChunk1Size = byteArrayToLong(tmpLong);

            Log.i("TAG","AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAASTA este SUBCHUNKSIZE");
            inFile.read(tmpInt); // read the audio format.  This should be 1 for PCM
            myFormat = byteArrayToInt(tmpInt);

            inFile.read(tmpInt); // read the # of channels (1 or 2)
            myChannels = byteArrayToInt(tmpInt);

            inFile.read(tmpLong); // read the samplerate
            mySampleRate = byteArrayToLong(tmpLong);

            inFile.read(tmpLong); // read the byterate
            myByteRate = byteArrayToLong(tmpLong);

            inFile.read(tmpInt); // read the blockalign
            myBlockAlign = byteArrayToInt(tmpInt);

            inFile.read(tmpInt); // read the bitspersample
            myBitsPerSample = byteArrayToInt(tmpInt);




            // print what we've read so far
            Log.i("TAG","SubChunk1ID:" + subChunk1ID + " SubChunk1Size:" + mySubChunk1Size + " AudioFormat:" + myFormat + " Channels:" + myChannels + " SampleRate:" + mySampleRate);


            // read the data chunk header - reading this IS necessary, because not all wav files will have the data chunk here - for now, we're just assuming that the data chunk is here
            String dataChunkID = "" + (char)inFile.readByte() + (char)inFile.readByte() + (char)inFile.readByte() + (char)inFile.readByte();

            inFile.read(tmpLong); // read the size of the data
            myDataSize = byteArrayToLong(tmpLong);


            Log.i("TAG","MYYYYYY DAAAAAAAAAAATAAAAAA SIIIIIIZE ISSSSSS   "+ myDataSize);





            double[] dataNew=new double[(int)myDataSize/2];
            myData = new byte[(int)myDataSize];
           for(int i=0;i<myDataSize/2;i++) {
               short val =(short)((inFile.readByte()&0xFF )|(inFile.readByte()&0xFF)<<8);
               //Log.i("TAG","ASTA E SHORT VALLLLLLLLLL" + val);
               dataNew[i]=(double) val;
           }
            //for(int j=0;j<dataNew.length;j++) {
            //    Log.i("TAG", "MY DATA IS HERE" + dataNew[j]);
            //}

            Log.i("TAG","ASTA E DIMENSIUNEA DATELOR " +myDataSize);
            Log.i("TAG","ASTA E DIMENSIUNEA LA DATA PRELEVATA" + dataNew.length);

            double valMax=0;
            int indexMax=0;

            int indexInterest=0;

            for(int q=0;q<dataNew.length;q++)
            {
                if (dataNew[q]>valMax){
                    valMax=dataNew[q];
                    indexMax=q;
            }

            }

            double[] arrayInterest=new double[dataNew.length-indexMax];

            while(dataNew[indexMax]>valMax*0.1)
            {
                arrayInterest[indexInterest]=dataNew[indexMax];
                indexMax++;
                indexInterest++;
            }


            GraphView graph=(GraphView) findViewById(R.id.graph);

            apacheFFT.forward(arrayInterest,(float)44100,w);
            Spectrum s=apacheFFT.getMagnitudeSpectrum();
            double[] freq=s.array();
            double[] array=new double[freq.length];

            DataPoint[] dataPoints = new DataPoint[4096];
            for(int i=0;i<freq.length;i++)
            {
                array[i]=(10*Math.log10(Math.abs(freq[i])));
                dataPoints[i]=new DataPoint(i,Math.abs(array[i]));

            }

            LineGraphSeries<DataPoint> series = new LineGraphSeries<DataPoint>(dataPoints);
            graph.addSeries(series);

for(int i=0;i<array.length;i++)
            Log.i("TAG","ASTEA SUNT FRECVENTELE" + Math.abs(array[i]));

            Log.i("TAG","MARIMEA LA FRECVENTE" + freq.length);





        }catch(Exception e){
            System.out.println(e.getMessage());
        }

        ;

    }

    public static int byteArrayToInt(byte[] b)
    {
        int start = 0;
        int low = b[start] & 0xff;
        int high = b[start+1] & 0xff;
        return (int)( high << 8 | low );
    }

    public static long byteArrayToLong(byte[] b)
    {
        int start = 0;
        int i = 0;
        int len = 4;
        int cnt = 0;
        byte[] tmp = new byte[len];
        for (i = start; i < (start + len); i++)
        {
            tmp[cnt] = b[i];
            cnt++;
        }
        long accum = 0;
        i = 0;
        for ( int shiftBy = 0; shiftBy < 32; shiftBy += 8 )
        {
            accum |= ( (long)( tmp[i] & 0xff ) ) << shiftBy;
            i++;
        }
        return accum;
    }


}



