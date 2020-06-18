
package com.example.soundapp2;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import com.opencsv.CSVWriter;
import androidx.appcompat.app.AppCompatActivity;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.series.BarGraphSeries;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;


import org.apache.commons.math3.complex.Complex;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class FFTActivity extends AppCompatActivity {

    SharedPreferences sharedPreferences;

    ApacheFFT apacheFFT=new ApacheFFT();
    WindowFunction w = new RectangularWindow();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fft);

        sharedPreferences=getSharedPreferences("MyPref", Context.MODE_PRIVATE);
        //String filepath=sharedPreferences.getString("filepath",null);
        String filepath=Environment.getExternalStorageDirectory().getPath()+"/"+"SuneteAplicatie"+"/"+"nou_fisier"+".wav";


        try{



            WavIO wave = readWavBytes(filepath);
            short[] b = new short[wave.myData.length/2];
            for (int i=0;i<wave.myData.length-1;i+=2)
                b[i/2] = twoBytesToShort(wave.myData[i+1],wave.myData[i]);



            double[] dataNew=new double[b.length];

            for(int i=0;i<dataNew.length;i++)
            {
                dataNew[i]=b[i];
            }


            GraphView graph=(GraphView) findViewById(R.id.graph);

            apacheFFT.forward(dataNew,(float)44100,w);

            Spectrum s =apacheFFT.getMagnitudeSpectrum();
            double[] freq=s.array();

            DataPoint[] dataPoints = new DataPoint[4096];
            for(int i=0;i<freq.length;i++)
            {
                dataPoints[i]=new DataPoint(i*44100/4096/2,freq[i]);
            }


            CSVWriter writer=new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory().getPath()+"/SuneteAplicatie"+"/"+"data.csv"));

            String[] starr=new String[] {Arrays.toString(freq)};
            writer.writeNext(starr);
            writer.close();

            FileOutputStream fos2=new FileOutputStream(Environment.getExternalStorageDirectory().getPath()+"/SuneteAplicatie"+"/"+"data2.csv",true);
            PrintWriter pw=new PrintWriter(fos2);


            for(int i=0;i<freq.length;i++)
            {
                pw.println(freq[i]);
            }

            pw.close();

            FileWriter outputfile=new FileWriter(Environment.getExternalStorageDirectory().getPath()+"/SuneteAplicatie"+"/"+"data3.csv");

            CSVWriter rw=new CSVWriter(outputfile);
            List<String[]> data=new ArrayList<String[]>();
            data.add(starr);
            rw.writeAll(data);
            writer.close();

            BarGraphSeries<DataPoint> series = new BarGraphSeries<>(dataPoints);
            graph.getViewport().setScalable(true);
            graph.getViewport().setScrollable(true);
            graph.addSeries(series);




        }catch(Exception e){
            System.out.println(e.getMessage());
        }

        ;

    }

    public static WavIO readWavBytes(String path){
        WavIO wave = new WavIO(path);
        wave.read();
        return wave;
    }

    static short twoBytesToShort(byte b1, byte b2) {
        return (short) ((b1 << 8) | (b2 & 0xFF));
    }

}


