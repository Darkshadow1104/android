package Cosylab.tensorflow.lite.examples.detection;


import android.content.Context;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Scanner;

public class ReadWriteFromFile {
    static LocalDateTime localTime;

    public ReadWriteFromFile(LocalDateTime localTime){
        this.localTime = localTime;
    }

    public static void writeToFile(String data, Context context) {
        try {
            /*OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("config.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();*/

            File d = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            //File file = null;

            File file = new File(d, String.valueOf(localTime) + ".txt");
                    //File(d, "data.txt");
            FileWriter writer = new FileWriter(file);
            writer.append("\n");
            writer.append(data);
            writer.close();

        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }
    public double readfiles(Context context) throws IOException {
        String localdate = null;
        double myans = 0;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            localdate = String.valueOf(LocalDate.now());
        }

        File d = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File fileslist[] = d.listFiles();
        Scanner sc = null;
        StringBuffer sb  = new StringBuffer();
        for (File file:fileslist){
            String filename = file.getName();
            String extractnames = "";
            //char ch = 0;
            int i = 0;
            if(filename.charAt(0)!='2'){
                continue;
            }
            else{
                while(filename.charAt(i)!='T'){
                    char  ch = filename.charAt(i);
                    extractnames = extractnames + ch;
                    i++;
                }
            }

            i = 0;
            if(extractnames.equals(localdate)){
                //sc= new Scanner(file);
                String input = "";
                FileReader reader = new FileReader(file);
                char[] array = new char[8];
                reader.read(array);
                array[7] = '\0';
                for(int j = 0; j<array.length - 1; j++){
                    input = input + array[j];
                }
                double ans = Double.parseDouble(input);
                myans = myans + ans;
            }

        }

        return myans;
    }

    public static void writemaximumcalories(String data,Context context){
        try {

            File d = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            File file = new File(d,  "maximumcalories"+".txt");
            FileWriter writer = new FileWriter(file);
            writer.append("\n");
            writer.append(data);
            writer.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }


    public static double readmaximumcalories(Context context) {

        String ret = "";
        double maxcalories = 0;
        try {
            File d = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
            File file  = new File(d, "maximumcalories"+".txt");
            FileReader reader = new FileReader(file);
            /*int ch;
            StringBuilder builder = new StringBuilder();
            while ((ch = reader.read())!=-1){
                     builder.append((char)ch);
            }
            Toast.makeText(context, "Data:" + builder.toString(), Toast.LENGTH_SHORT).show();*/
            char[] array = new char[8];
            reader.read(array);
            array[7] = '\0';
            for(int i = 0; i<array.length - 1; i++){
                ret = ret + array[i];
            }
            maxcalories = Double.parseDouble(ret);
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return maxcalories;
    }

    public ArrayList<Double> getthevaluesforgraph(Context context) throws IOException {
        ArrayList<Double> arr= new ArrayList<>();
        File d = context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        File fileslist[] = d.listFiles();
        for (File file:fileslist){
            String filename = file.getName();
            if(filename.equals("maximumcalories.txt")) {
               continue;
           }
               else{
                double myans = 0;
                String input = "";
                FileReader reader = new FileReader(file);
                char[] array = new char[8];
                reader.read(array);
                array[7] = '\0';
                for(int j = 0; j<array.length - 1; j++){
                    input = input + array[j];
                }
                double ans = Double.parseDouble(input);
                myans = myans + ans;
                arr.add(myans);
               }
           }
        return arr;
        }



    }

