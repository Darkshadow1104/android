/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package Cosylab.tensorflow.lite.examples.detection;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.tensorflow.lite.examples.detection.R;

import Cosylab.tensorflow.lite.examples.detection.customview.OverlayView;
import Cosylab.tensorflow.lite.examples.detection.env.BorderedText;
import Cosylab.tensorflow.lite.examples.detection.env.ImageUtils;
import Cosylab.tensorflow.lite.examples.detection.env.Logger;
import Cosylab.tensorflow.lite.examples.detection.tflite.Classifier;
import Cosylab.tensorflow.lite.examples.detection.tflite.DetectorFactory;
import Cosylab.tensorflow.lite.examples.detection.tflite.YoloV5Classifier;
import Cosylab.tensorflow.lite.examples.detection.tracking.DataAdapter;
import Cosylab.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;
import de.codecrafters.tableview.TableView;
import de.codecrafters.tableview.toolkit.SimpleTableDataAdapter;
import de.codecrafters.tableview.toolkit.SimpleTableHeaderAdapter;

/**
 * An activity that uses a TensorFlowMultiBoxDetector and ObjectTracker to detect and then track
 * objects.
 */
public class DetectorActivity extends CameraActivity implements OnImageAvailableListener {

    private static final Logger LOGGER = new Logger();

    private static final DetectorMode MODE = DetectorMode.TF_OD_API;
    private static final float MINIMUM_CONFIDENCE_TF_OD_API = 0.3f;
    private static final boolean MAINTAIN_ASPECT = true;
    private static final Size DESIRED_PREVIEW_SIZE = new Size(640, 640);
    private static final boolean SAVE_PREVIEW_BITMAP = false;
    private static final float TEXT_SIZE_DIP = 10;
    OverlayView trackingOverlay;
    private Integer sensorOrientation;

    private YoloV5Classifier detector;

    private long lastProcessingTimeMs;
    private Bitmap rgbFrameBitmap = null;
    private Bitmap croppedBitmap = null;
    private Bitmap cropCopyBitmap = null;

    private boolean computingDetection = false;

    private long timestamp = 0;

    private Matrix frameToCropTransform;
    private Matrix cropToFrameTransform;

    private MultiBoxTracker tracker;

    private BorderedText borderedText;
    //private TextView resulttv;
    /*private int currentNumThreads = 0;
    private TextView threadsTextView;*/


    @Override
    public void onPreviewSizeChosen(final Size size, final int rotation) {
        final float textSizePx =
                TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, TEXT_SIZE_DIP, getResources().getDisplayMetrics());
        borderedText = new BorderedText(textSizePx);
        borderedText.setTypeface(Typeface.MONOSPACE);

        tracker = new MultiBoxTracker(this);

        final int modelIndex = 0;
        //modelView.getCheckedItemPosition();
        final String modelString = modelStrings.get(modelIndex);

        try {
            detector = DetectorFactory.getDetector(getAssets(), modelString);
        } catch (final IOException e) {
            e.printStackTrace();
            LOGGER.e(e, "Exception initializing classifier!");
            Toast toast =
                    Toast.makeText(
                            getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
            toast.show();
            finish();
        }

        int cropSize = detector.getInputSize();

        previewWidth = size.getWidth();
        previewHeight = size.getHeight();

        sensorOrientation = rotation - getScreenOrientation();
        LOGGER.i("Camera orientation relative to screen canvas: %d", sensorOrientation);

        LOGGER.i("Initializing at size %dx%d", previewWidth, previewHeight);
        rgbFrameBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Config.ARGB_8888);
        croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

        frameToCropTransform =
                ImageUtils.getTransformationMatrix(
                        previewWidth, previewHeight,
                        cropSize, cropSize,
                        sensorOrientation, MAINTAIN_ASPECT);

        cropToFrameTransform = new Matrix();
        frameToCropTransform.invert(cropToFrameTransform);

        trackingOverlay = (OverlayView) findViewById(R.id.tracking_overlay);
        trackingOverlay.addCallback(
                new OverlayView.DrawCallback() {
                    @Override
                    public void drawCallback(final Canvas canvas) {
                        tracker.draw(canvas);
                        if (isDebug()) {
                            tracker.drawDebug(canvas);
                        }
                    }
                });

        tracker.setFrameConfiguration(previewWidth, previewHeight, sensorOrientation);
    }

    protected void updateActiveModel() {
        // Get UI information before delegating to background
      final int modelIndex = 0;
      //modelView.getCheckedItemPosition();
        final int deviceIndex = deviceView.getCheckedItemPosition();
        String threads = threadsTextView.getText().toString().trim();
        final int numThreads = Integer.parseInt(threads);

        handler.post(() -> {
            if (modelIndex == currentModel && deviceIndex == currentDevice
                    && numThreads == currentNumThreads) {
                return;
            }
            currentModel = modelIndex;
            currentDevice = deviceIndex;
            currentNumThreads = numThreads;

            // Disable classifier while updating
            if (detector != null) {
                detector.close();
                detector = null;
            }

            // Lookup names of parameters.
            String modelString = modelStrings.get(modelIndex);
            String device = deviceStrings.get(deviceIndex);

            LOGGER.i("Changing model to " + modelString + " device " + device);

            // Try to load model.

            try {
                detector = DetectorFactory.getDetector(getAssets(), modelString);
                // Customize the interpreter to the type of device we want to use.
                if (detector == null) {
                    return;
                }
            }
            catch(IOException e) {
                e.printStackTrace();
                LOGGER.e(e, "Exception in updateActiveModel()");
                Toast toast =
                        Toast.makeText(
                                getApplicationContext(), "Classifier could not be initialized", Toast.LENGTH_SHORT);
                toast.show();
                finish();
            }


            if (device.equals("CPU")) {
                detector.useCPU();
            } else if (device.equals("GPU")) {
                detector.useGpu();
            } else if (device.equals("NNAPI")) {
                detector.useNNAPI();
            }
            detector.setNumThreads(numThreads);

            int cropSize = detector.getInputSize();
            croppedBitmap = Bitmap.createBitmap(cropSize, cropSize, Config.ARGB_8888);

            frameToCropTransform =
                    ImageUtils.getTransformationMatrix(
                            previewWidth, previewHeight,
                            cropSize, cropSize,
                            sensorOrientation, MAINTAIN_ASPECT);

            cropToFrameTransform = new Matrix();
            frameToCropTransform.invert(cropToFrameTransform);
        });
    }


    @Override
    protected void processImage() {
        ++timestamp;
        final long currTimestamp = timestamp;
        trackingOverlay.postInvalidate();

        // No mutex needed as this method is not reentrant.
        if (computingDetection) {
            readyForNextImage();
            return;
        }
        computingDetection = true;
        LOGGER.i("Preparing image " + currTimestamp + " for detection in bg thread.");

        rgbFrameBitmap.setPixels(getRgbBytes(), 0, previewWidth, 0, 0, previewWidth, previewHeight);

        readyForNextImage();

        final Canvas canvas = new Canvas(croppedBitmap);
        canvas.drawBitmap(rgbFrameBitmap, frameToCropTransform, null);
        // For examining the actual TF input.
        if (SAVE_PREVIEW_BITMAP) {
            ImageUtils.saveBitmap(croppedBitmap);
        }
        //final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);



        runInBackground(
                new Runnable() {


                    @Override
                    public void run() {
                        LOGGER.i("Running detection on image " + currTimestamp);
                        final long startTime = SystemClock.uptimeMillis();
                        final List<Classifier.Recognition> results = detector.recognizeImage(croppedBitmap);
                        lastProcessingTimeMs = SystemClock.uptimeMillis() - startTime;

                        Log.e("CHECK", "run: " + results.size());
                        /*if(results.size()!=0){
                            Log.e("CHECK", "Nitesh_Answer: " + results.get(0));
                            //stored_ans.add((CharSequence) results.get(0));
                            CharSequence x = (CharSequence) results.get(0);
                            resulttv.setText((char) x.charAt(0));
                        }*/
                       // Log.e("CHECK", "Nitesh_Answer: " + results.get(0));

                        cropCopyBitmap = Bitmap.createBitmap(croppedBitmap);
                        final Canvas canvas = new Canvas(cropCopyBitmap);
                        final Paint paint = new Paint();
                        paint.setColor(Color.RED);
                        paint.setStyle(Style.STROKE);
                        paint.setStrokeWidth(2.0f);

                        float minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                        switch (MODE) {
                            case TF_OD_API:
                                minimumConfidence = MINIMUM_CONFIDENCE_TF_OD_API;
                                break;
                        }

                        final List<Classifier.Recognition> mappedRecognitions =
                                new LinkedList<Classifier.Recognition>();

                        for (final Classifier.Recognition result : results) {
                            final RectF location = result.getLocation();
                            if (location != null && result.getConfidence() >= minimumConfidence) {
                                canvas.drawRect(location, paint);

                                cropToFrameTransform.mapRect(location);

                                result.setLocation(location);
                                mappedRecognitions.add(result);

                            }
                        }

                        tracker.trackResults(mappedRecognitions, currTimestamp);
                        trackingOverlay.postInvalidate();

                        computingDetection = false;

                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        /*showFrameInfo(previewWidth + "x" + previewHeight);
                                        showCropInfo(cropCopyBitmap.getWidth() + "x" + cropCopyBitmap.getHeight());
                                        showInference(lastProcessingTimeMs + "ms");*/
                                    }
                                });



                        runOnUiThread(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        if(results.size()!=0){
                                            Log.e("CHECK", "Nitesh_Answer: " + results.get(0));

                                            //stored_ans.add((CharSequence) results.get(0));
                                            int k = 0;
                                            while(k<results.size()){
                                                stored_ans[k] = (String) results.get(k).toString();
                                                k++;
                                            }

                                                recyclerView = (RecyclerView) findViewById(R.id.recycler_view);


                                            Map<String, Integer> object = new HashMap<>();
                                            object.put("Indian bread", 0);
                                            object.put("Rasgulla", 0);
                                            object.put("Biryani", 0);
                                            object.put("Uttapam", 0);
                                            object.put("Paneer", 0);
                                            object.put("Poha", 0);
                                            object.put("Khichdi", 0);
                                            object.put("Omelette", 0);
                                            object.put("Plain rice", 0);
                                            object.put("Dal makhani", 0);
                                            object.put("Rajma", 0);
                                            object.put("Poori", 0);
                                            object.put("Chole", 0);
                                            object.put("Dal", 0);
                                            object.put("Sambhar", 0);
                                            object.put("Papad", 0);
                                            object.put("Gulab jamun", 0);
                                            object.put("Idli", 0);
                                            object.put("Vada", 0);
                                            object.put("Dosa", 0);
                                            object.put("Jalebi", 0);
                                            object.put("Samosa", 0);
                                            object.put("Paobhaji", 0);
                                            object.put("Dhokla", 0);
                                            object.put("Barfi", 0);
                                            object.put("Fishcurry", 0);
                                            object.put("Momos", 0);
                                            object.put("Kheer", 0);
                                            object.put("Kachori", 0);
                                            object.put("Vadapav", 0);
                                            object.put("Rasmalai", 0);
                                            object.put("Kalachana", 0);
                                            object.put("Chaat", 0);
                                            object.put("Saag", 0);
                                            object.put("Dumaloo", 0);
                                            object.put("Thupka", 0);
                                            object.put("Khandvi", 0);
                                            object.put("Kabab", 0);
                                            object.put("Thepla", 0);
                                            object.put("Rasam", 0);
                                            object.put("Appam", 0);
                                            object.put("Gatte", 0);
                                            object.put("Kadhipakora", 0);
                                            object.put("Ghewar", 0);
                                            object.put("Aloomatter", 0);
                                            object.put("Prawns", 0);
                                            object.put("Sandwich", 0);
                                            object.put("Dahipuri", 0);
                                            object.put("Haleem", 0);
                                            object.put("Mutton", 0);
                                            object.put("Aloogobi", 0);
                                            object.put("Eggbhurji", 0);
                                            object.put("Lemonrice", 0);
                                            object.put("Bhindimasala", 0);
                                            object.put("Matarmushroom", 0);
                                            object.put("Gajarkahalwa", 0);
                                            object.put("Motichoorladoo", 0);
                                            object.put("Ragiroti", 0);
                                            object.put("Chickentikka", 0);
                                            object.put("Tandoorichicken", 0);
                                            object.put("Lauki", 0);


                                            String[][] data = new String[61][5];
                                            /*String[] headers = {"Dish", "Calories (Kcal)", "Protein (g)", "Fat (g)", "Carbohydrates (g)"};
                                            String[][] data = new String[20][5];
                                            tableview.setHeaderAdapter(new SimpleTableHeaderAdapter(DetectorActivity.this, headers));
                                            tableview.setHeaderBackgroundColor(Color.parseColor("#2ecc71"));
                                            tableview.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
                                            tableview.setPivotX(10);*/
                                            progressBar = findViewById(R.id.progrssbar);
                                            /*progressBar2 = findViewById(R.id.progrssbar1);
                                            progressBar3 = findViewById(R.id.progrssbar2);*/
                                            progress_percentage = findViewById(R.id.textView4);
                                           /*progressBar = findViewById(R.id.progrssbar);
                                            double total_calories_consume_till_now_in_one_daya_1 = 0;
                                            try {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    total_calories_consume_till_now_in_one_daya_1 = new ReadWriteFromFile(LocalDateTime.now()).readfiles(DetectorActivity.this);
                                                }
                                            } catch (IOException e) {
                                                e.printStackTrace();
                                            }
                                            Log.d("Total_calories", String.valueOf(total_calories_consume_till_now_in_one_daya_1));


                                            double max_calories_per_day_1 = 0;
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                max_calories_per_day_1 = new ReadWriteFromFile(LocalDateTime.now()).readmaximumcalories(DetectorActivity.this);
                                            }
                                            Log.d(",Max_calorie_Capacity", String.valueOf(max_calories_per_day_1));
                                          //  progressBar = findViewById(R.id.progress_bar);
                                            progressBar.setMax((int) max_calories_per_day_1);
                                            progressBar.setProgress((int) total_calories_consume_till_now_in_one_daya_1);*/
                                            //setContentView(R.layout.tfe_od_camera_connection_fragment_tracking);

                                                button = (Button) findViewById(R.id.button);
                                                button.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        //takeScreenShot(croppedBitmap, "result");
                                                        //resulttv.setText("My_answer");
                                                        //resulttv.setText((String) y);
                                                        /*
                                                        *
                                                        * Check whether calorie capacity per day is 0 or not.
                                                        * */
                                                        double max_calories_per_day_2 = 0;
                                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                            max_calories_per_day_2 = new ReadWriteFromFile(LocalDateTime.now()).readmaximumcalories(DetectorActivity.this);
                                                        }
                                                        if(max_calories_per_day_2==0){
                                                            Toast.makeText(DetectorActivity.this, "Please 1st enter your details", Toast.LENGTH_SHORT).show();
                                                        }
                                                        else {
                                                            for (int j = 0; j < results.size(); j++) {
                                                                String x = (String) results.get(j).toString();
                                                                int i = 4;
                                                                while (x.charAt(i) != ' ' || x.charAt(i + 1) != '(') {
                                                                    i++;

                                                                }
                                                                String y = (String) x.substring(4, i);
                                                                int count = object.containsKey(y) ? object.get(y) : 0;
                                                                object.put(y, count + 1);
                                                                //String two_char = x.substring(1,3);
                                                                //JSONObject jsonObject = new JSONObject();


                                                            /*two_obj[j].setText((String) y);
                                                            calorie_obj[j].setText(map.get(two_obj[j].getText())[0]);
                                                            protien_obj[j].setText(map.get(two_obj[j].getText())[1]);
                                                            fat_obj[j].setText(map.get(two_obj[j].getText())[2]);
                                                            carbohydrates_obj[j].setText(map.get(two_obj[j].getText())[3]);*/


                                                            }

                                                            // Getting an iterator
                                                            //Iterator object_Iterator = object.entrySet().iterator();
                                                            int l = 0;
                                                            for (Map.Entry<String, Integer> set :
                                                                    object.entrySet()) {

                                                                // Printing all elements of a Map
                                                            /*System.out.println(set.getKey() + " = "
                                                                    + set.getValue());*/
                                                                if (set.getValue() != 0) {
                                                                    //two_obj[l].setText((String) set.getKey() + " x" + (String) set.getValue().toString());
                                                                    data[l][0] = (String) set.getKey() + " x" + (String) set.getValue().toString();

                                                                    //calorie_obj[l].setText(String.format("%.2f", map.get(set.getKey())[0]* set.getValue()));
                                                                    data[l][1] = String.format("%.2f", map.get(set.getKey())[0] * set.getValue());
                                                                    // protien_obj[l].setText(String.format("%.2f", map.get(set.getKey())[1]* set.getValue()));
                                                                    data[l][2] = String.format("%.2f", map.get(set.getKey())[1] * set.getValue());
                                                                    // fat_obj[l].setText(String.format("%.2f", map.get(set.getKey())[2]* set.getValue()));
                                                                    data[l][3] = String.format("%.2f", map.get(set.getKey())[2] * set.getValue());
                                                                    // carbohydrates_obj[l].setText(String.format("%.2f", map.get(set.getKey())[3]* set.getValue()));
                                                                    data[l][4] = String.format("%.2f", map.get(set.getKey())[3] * set.getValue());
                                                                    l++;
                                                                    object.put(set.getKey(), 0);


                                                                }


                                                                //tableview.setDataAdapter(new SimpleTableDataAdapter(DetectorActivity.this, Newdata));


                                                            }
                                                            String[][] Newdata = new String[l][5];
                                                            for (int i = 0; i < l; i++) {
                                                                for (int j = 0; j < 5; j++) {
                                                                    Newdata[i][j] = data[i][j];
                                                                }
                                                            }

                                                            /*
                                                             * Here In the Below code We are creating the recyclerview
                                                             * Which is usefull to show the answer of table in the table view.
                                                             * Two classes has been created for it 1st one is DataModel and 2nd one is DataAdapter.
                                                             *
                                                             * */

                                                            recyclerView.setHasFixedSize(true);
                                                            recyclerView.setLayoutManager(new LinearLayoutManager(DetectorActivity.this));
                                                            List<DataModel> dataModelList = new ArrayList<>();
                                                            int Number_of_columns = 5;
                                                            int Number_of_rows = l;
                                                            for (int i = 0; i < Number_of_rows; i++) {
                                                                String[] dataarray = new String[Number_of_columns];
                                                                for (int j = 0; j < Number_of_columns; j++) {
                                                                    dataarray[j] = Newdata[i][j];
                                                                }
                                                                DataModel mydatamodel = new DataModel(dataarray);
                                                                dataModelList.add(mydatamodel);
                                                            }
                                                            dataAdapter = new DataAdapter(DetectorActivity.this, dataModelList);
                                                            recyclerView.setAdapter(dataAdapter);

                                                            /*
                                                             * Here in the below code we are taking the screenshort.
                                                             * The screenshort function is build inside the fragment.
                                                             * If you want to see how the function works please see the fragment name as "Cameraconnection Fragment"
                                                             *
                                                             * */

                                                            CameraConnectionFragment fragment = (CameraConnectionFragment) getFragmentManager().findFragmentById(R.id.container);
                                                            fragment.takescreenshort(view);
                                                            //fragment.setRecycleerView(Newdata, l);
                                                            //fragment.takescreenshort(view);
                                                            //fragment.takescreenshort(view);
                                                        /*CameraConnectionFragment fragment = (CameraConnectionFragment) getFragmentManager().findFragmentById(R.id.container);
                                                        fragment.takePicture();*/

                                                            /* takeScreenShot(getWindow().getDecorView().getRootView(), "result");*/

                                                        /*table.setColumnCollapsed(0, table_flag);
                                                        table.setColumnCollapsed(1, table_flag);
                                                        table.setColumnCollapsed(2, table_flag);
                                                        table.setColumnCollapsed(3, table_flag);

                                                        if(table_flag){
                                                            table_flag = false;

                                                        }
                                                        else{
                                                            table_flag = true;
                                                        }*/

                                                        /*ActivityCompat.requestPermissions(DetectorActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                                                Manifest.permission.READ_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);

                                                        View view1 = getWindow().getDecorView().getRootView();
                                                        Bitmap bitmap = Bitmap.createBitmap(view1.getWidth(), view1.getHeight(), Config.ARGB_8888);
                                                        Canvas canvas = new Canvas(bitmap);
                                                        view1.draw(canvas);

                                                        File fileScreenshot = new File(DetectorActivity.this.getExternalFilesDir(Environment.DIRECTORY_PICTURES), Calendar.getInstance().getTime().toString() + ".jpeg");
                                                        FileOutputStream fileOutputStream = null;
                                                        try{
                                                            fileOutputStream = new FileOutputStream(fileScreenshot);
                                                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fileOutputStream);
                                                            fileOutputStream.flush();
                                                            fileOutputStream.close();
                                                        } catch (Exception e) {
                                                            e.printStackTrace();
                                                        }*/

                                                            /*
                                                             * Here in the below code we are calculating the total calories of all the food detect in the one photo.
                                                             * Here we are storing the total calories in the "mycalorie" variable.
                                                             * After that we are calling the "RunningThread" class in which we have build the function name as "writeToFile()"
                                                             * By calling this function here we are storing the total calories in one photo in Data-formate.txt file.

                                                             * */

                                                            double mycalorie = 0;
                                                            for (int d = 0; d < l; d++) {
                                                                mycalorie = mycalorie + Double.parseDouble(Newdata[d][1]);
                                                            }
                                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                                new ReadWriteFromFile(LocalDateTime.now()).writeToFile(String.valueOf(mycalorie), DetectorActivity.this);

                                                            }
                                                            /*
                                                             * Here in the below code we are just calculating today's calories consume till now.
                                                             * And the value of the total calories is storing in the variable name as "total_calories_consume_till_now_in_one_daya".
                                                             * To read more about the Function of the code please visit to the file name as "ReadWriteFromFile".
                                                             *
                                                             * */
                                                            double total_calories_consume_till_now_in_one_daya = 0;
                                                            try {
                                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                                    total_calories_consume_till_now_in_one_daya = new ReadWriteFromFile(LocalDateTime.now()).readfiles(DetectorActivity.this);
                                                                }
                                                            } catch (IOException e) {
                                                                e.printStackTrace();
                                                            }
                                                            Log.d("Total_calories", String.valueOf(total_calories_consume_till_now_in_one_daya));
                                                            /*
                                                             * Here in the below code we are getting the maximum capacity of a person per day.(In form of calories)
                                                             * */
                                                            double max_calories_per_day = 0;
                                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                                max_calories_per_day = new ReadWriteFromFile(LocalDateTime.now()).readmaximumcalories(DetectorActivity.this);
                                                            }
                                                            Log.d(",Max_calorie_Capacity", String.valueOf(max_calories_per_day));
                                                            /*
                                                             *
                                                             * */
                                                            /*progressBar.setMax((int) max_calories_per_day);
                                                            progressBar.setProgress((int) total_calories_consume_till_now_in_one_daya);*/
                                                            progress_percentage.setText(String.valueOf(total_calories_consume_till_now_in_one_daya) + " Kcal");
                                                            /*double greencolor = (max_calories_per_day * 33 )/100;
                                                            double orangecolor = (max_calories_per_day * 66 )/100;
                                                            double Redcolor = (max_calories_per_day * 100 )/100;
                                                            progressBar.setMax((int) greencolor);
                                                            progressBar2.setMax((int) orangecolor);
                                                            progressBar3.setMax((int) Redcolor);
                                                            if(total_calories_consume_till_now_in_one_daya<=greencolor){
                                                                progressBar.setProgress((int) total_calories_consume_till_now_in_one_daya);
                                                            }
                                                            else if(total_calories_consume_till_now_in_one_daya>greencolor && total_calories_consume_till_now_in_one_daya<=orangecolor){
                                                                progressBar.setProgress((int) greencolor);
                                                                progressBar2.setProgress((int) total_calories_consume_till_now_in_one_daya);
                                                            }
                                                            else if(total_calories_consume_till_now_in_one_daya>orangecolor  && total_calories_consume_till_now_in_one_daya<=Redcolor){
                                                                progressBar.setProgress((int) greencolor);
                                                                progressBar2.setProgress((int) orangecolor);
                                                                progressBar3.setProgress((int) total_calories_consume_till_now_in_one_daya);
                                                            }*/
                                                            progressBar.setMax((int) max_calories_per_day);
                                                            progressBar.setProgress((int) total_calories_consume_till_now_in_one_daya);

                                                        }
                                                    }

                                                });


                                }


                                    }



                                }
                        );



                    }

                }
                );


    }


    @Override
    protected int getLayoutId() {
        return R.layout.tfe_od_camera_connection_fragment_tracking;
    }

    @Override
    protected Size getDesiredPreviewFrameSize() {
        return DESIRED_PREVIEW_SIZE;
    }

    // Which detection model to use: by default uses Tensorflow Object Detection API frozen
    // checkpoints.
    private enum DetectorMode {
        TF_OD_API;
    }

    @Override
    protected void setUseNNAPI(final boolean isChecked) {
        runInBackground(() -> detector.setUseNNAPI(isChecked));
    }

    @Override
    protected void setNumThreads(final int numThreads) {
        runInBackground(() -> detector.setNumThreads(numThreads));
    }
}
