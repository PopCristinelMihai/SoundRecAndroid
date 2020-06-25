package com.example.soundapp2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.widget.TextView;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.util.Arrays;

import net.sf.javaml.classification.Classifier;
import net.sf.javaml.core.Dataset;
import net.sf.javaml.core.Instance;
import net.sf.javaml.tools.data.FileHandler;

import be.abeel.io.Serial;

public class ClassifierData extends AppCompatActivity {


    ApacheFFT apacheFFT= new ApacheFFT();
    WindowFunction w = new RectangularWindow();
    TextView textClass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_classifier);

        Log.e("TAG","AM AJUNS AICI");

        String filepath= Environment.getExternalStorageDirectory().getPath()+"/"+"SuneteAplicatie"+"/"+"ClassifyThis"+".wav";
        Log.e("LOCATIE",Environment.getExternalStorageDirectory().getPath()+"/"+"SuneteAplicatie"+"/"+"ClassifyThis"+".wav");

        try{

            WavIO wave = readWavBytes(filepath);
            short[] b = new short[wave.myData.length/2];
            for(int i=0;i<wave.myData.length-1;i+=2)
                b[i/2] = twoBytesToShort(wave.myData[i+1],wave.myData[i]);



            double [] dataNew=new double[b.length];

            for(int i=0;i<dataNew.length;i++)
            {
                dataNew[i]=b[i];
            }


            apacheFFT.forward(dataNew,(float)44100,w);

            Spectrum s=apacheFFT.getMagnitudeSpectrum();
            double[] freq=Binning.ComputeBins(s.array(),2205,false);
            for (int i=0;i<freq.length;i++)
            {
                freq[i]=10*Math.log10(10*Math.abs(freq[i]));
            }
            CSVWriter writer = new CSVWriter(new FileWriter(Environment.getExternalStorageDirectory().getPath()+"/SuneteAplicatie"+"/"+"classify_data.csv"),',',CSVWriter.NO_QUOTE_CHARACTER,CSVWriter.NO_ESCAPE_CHARACTER,CSVWriter.RFC4180_LINE_END);



            String[] starr =new String[]{Arrays.toString(freq).replaceAll("[^0-9.,]+","")};


            writer.writeNext(starr);

            writer.close();



            Classifier model = loadModel(Environment.getExternalStorageDirectory().getPath()+"/SuneteAplicatie"+"/"+"Model.dat");



            Dataset dataForClassification =  FileHandler.loadDataset(new File(Environment.getExternalStorageDirectory().getPath()+"/SuneteAplicatie"+"/"+"classify_data.csv"),",");

            for (Instance  inst :dataForClassification ){
                Object predictedClassValue = model.classify(inst);

                textClass=findViewById(R.id.textClass);
                textClass.setText(predictedClassValue.toString());
            }


        }catch (Exception e){
            System.out.println(e.getMessage());
        }


    }
    public static WavIO readWavBytes(String path){
        WavIO wave = new WavIO(path);
        wave.read();
        return wave;
    }


    public Classifier loadModel(String fileName){
        Classifier model = (Classifier) Serial.load(fileName);
        return model;
    }

    static short twoBytesToShort(byte b1, byte b2) {
        return (short) ((b1 << 8) | (b2 & 0xFF));
    }
}

