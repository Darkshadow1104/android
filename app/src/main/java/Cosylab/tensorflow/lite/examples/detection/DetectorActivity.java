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
import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.util.Size;
import android.util.TypedValue;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.ArrayList;
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
import Cosylab.tensorflow.lite.examples.detection.tracking.MultiBoxTracker;

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
                                           /* //k = 0;
                                           // Log.e("CHECK", "Stored_ans: " + );
                                            //resulttv.setText((String) stored_ans[0].toString());*/

                                            /*
                                            if(results.size()==1){
                                                String x = (String) results.get(0).toString();
                                                int i = 4;
                                                while(x.charAt(i)!=' ' || x.charAt(i + 1)!='('){
                                                    i++;

                                                }
                                                String y = (String) x.substring(4, i);
                                                //String two_char = x.substring(1,3);
                                                button = (ImageButton) findViewById(R.id.button);
                                                button.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        takeScreenShot(croppedBitmap, "result");
                                                        //resulttv.setText("My_answer");
                                                        resulttv.setText((String) y);

                                                    }
                                                });
                                                //getResult(results.size(), results);

                                            }
                                            */

                                                //String[] two_obj= new String[results.size()];
                                                TextView[] two_obj = new TextView[20];
                                                two_obj[0] = resulttv;
                                                two_obj[1] = resulttv2;
                                                two_obj[2] = resulttv3;
                                                two_obj[3] = resulttv4;
                                                two_obj[4] = resulttv5;
                                                two_obj[5] = resulttv6;
                                                two_obj[6] = resulttv7;
                                                two_obj[7] = resulttv8;
                                                two_obj[8] = resulttv9;
                                                two_obj[9] = resulttv10;
                                                two_obj[10] = resulttv11;
                                                two_obj[11] = resulttv12;
                                                two_obj[12] = resulttv13;
                                                two_obj[13] = resulttv14;
                                                two_obj[14] = resulttv15;
                                                two_obj[15] = resulttv16;
                                                two_obj[16] = resulttv17;
                                                two_obj[17] = resulttv18;
                                                two_obj[18] = resulttv19;
                                                two_obj[19] = resulttv20;
                                            TextView[] calorie_obj = new TextView[20];
                                            calorie_obj[0] = calorie1;
                                            calorie_obj[1] = calorie2;
                                            calorie_obj[2] = calorie3;
                                            calorie_obj[3] = calorie4;
                                            calorie_obj[4] = calorie5;
                                            calorie_obj[5] = calorie6;
                                            calorie_obj[6] = calorie7;
                                            calorie_obj[7] = calorie8;
                                            calorie_obj[8] = calorie9;
                                            calorie_obj[9] = calorie10;
                                            calorie_obj[10] = calorie11;
                                            calorie_obj[11] = calorie12;
                                            calorie_obj[12] = calorie13;
                                            calorie_obj[13] = calorie14;
                                            calorie_obj[14] = calorie15;
                                            calorie_obj[15] = calorie16;
                                            calorie_obj[16] = calorie17;
                                            calorie_obj[17] = calorie18;
                                            calorie_obj[18] = calorie19;
                                            calorie_obj[19] = calorie20;

                                            TextView[] protien_obj = new TextView[20];
                                            protien_obj[0] = protien1;
                                            protien_obj[1] = protien2;
                                            protien_obj[2] = protien3;
                                            protien_obj[3] = protien4;
                                            protien_obj[4] = protien5;
                                            protien_obj[5] = protien6;
                                            protien_obj[6] = protien7;
                                            protien_obj[7] = protien8;
                                            protien_obj[8] = protien9;
                                            protien_obj[9] = protien10;
                                            protien_obj[10] = protien11;
                                            protien_obj[11] = protien12;
                                            protien_obj[12] = protien13;
                                            protien_obj[13] = protien14;
                                            protien_obj[14] = protien15;
                                            protien_obj[15] = protien16;
                                            protien_obj[16] = protien17;
                                            protien_obj[17] = protien18;
                                            protien_obj[18] = protien19;
                                            protien_obj[19] = protien20;

                                            TextView[] fat_obj = new TextView[20];
                                            fat_obj[0] = fat1;
                                            fat_obj[1] = fat2;
                                            fat_obj[2] = fat3;
                                            fat_obj[3] = fat4;
                                            fat_obj[4] = fat5;
                                            fat_obj[5] = fat6;
                                            fat_obj[6] = fat7;
                                            fat_obj[7] = fat8;
                                            fat_obj[8] = fat9;
                                            fat_obj[9] = fat10;
                                            fat_obj[10] = fat11;
                                            fat_obj[11] = fat12;
                                            fat_obj[12] = fat13;
                                            fat_obj[13] = fat14;
                                            fat_obj[14] = fat15;
                                            fat_obj[15] = fat16;
                                            fat_obj[16] = fat17;
                                            fat_obj[17] = fat18;
                                            fat_obj[18] = fat19;
                                            fat_obj[19] = fat20;

                                            TextView[] carbohydrates_obj = new TextView[20];
                                            carbohydrates_obj[0] = carbohydrates1;
                                            carbohydrates_obj[1] = carbohydrates2;
                                            carbohydrates_obj[2] = carbohydrates3;
                                            carbohydrates_obj[3] = carbohydrates4;
                                            carbohydrates_obj[4] = carbohydrates5;
                                            carbohydrates_obj[5] = carbohydrates6;
                                            carbohydrates_obj[6] = carbohydrates7;
                                            carbohydrates_obj[7] = carbohydrates8;
                                            carbohydrates_obj[8] = carbohydrates9;
                                            carbohydrates_obj[9] = carbohydrates10;
                                            carbohydrates_obj[10] = carbohydrates11;
                                            carbohydrates_obj[11] = carbohydrates12;
                                            carbohydrates_obj[12] = carbohydrates13;
                                            carbohydrates_obj[13] = carbohydrates14;
                                            carbohydrates_obj[14] = carbohydrates15;
                                            carbohydrates_obj[15] = carbohydrates16;
                                            carbohydrates_obj[16] = carbohydrates17;
                                            carbohydrates_obj[17] = carbohydrates18;
                                            carbohydrates_obj[18] = carbohydrates19;
                                            carbohydrates_obj[19] = carbohydrates20;

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

                                                button = (ImageButton) findViewById(R.id.button);
                                                button.setOnClickListener(new View.OnClickListener() {
                                                    @Override
                                                    public void onClick(View view) {
                                                        //takeScreenShot(croppedBitmap, "result");
                                                        //resulttv.setText("My_answer");
                                                        //resulttv.setText((String) y);
                                                        for(int j = 0; j<results.size(); j++){
                                                            String x = (String) results.get(j).toString();
                                                            int i = 4;
                                                            while(x.charAt(i)!=' ' || x.charAt(i + 1)!='('){
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
                                                            if(set.getValue()!=0){
                                                                two_obj[l].setText((String) set.getKey() + " x" + (String) set.getValue().toString());
                                                                //float f=Float.parseFloat(map.get(set.getKey())[0]);
                                                                calorie_obj[l].setText(String.format("%.2f", map.get(set.getKey())[0]* set.getValue()));
                                                                protien_obj[l].setText(String.format("%.2f", map.get(set.getKey())[1]* set.getValue()));
                                                                fat_obj[l].setText(String.format("%.2f", map.get(set.getKey())[2]* set.getValue()));
                                                                carbohydrates_obj[l].setText(String.format("%.2f", map.get(set.getKey())[3]* set.getValue()));
                                                                l++;
                                                                object.put(set.getKey(), 0);

                                                            }



                                                        }
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
