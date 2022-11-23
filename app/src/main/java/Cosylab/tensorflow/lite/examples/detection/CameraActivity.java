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
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.Image.Plane;
import android.media.ImageReader;
import android.media.ImageReader.OnImageAvailableListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Trace;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import android.service.controls.actions.FloatAction;
import android.util.Size;
import android.view.Surface;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.tensorflow.lite.examples.detection.R;

import Cosylab.tensorflow.lite.examples.detection.env.ImageUtils;
import Cosylab.tensorflow.lite.examples.detection.env.Logger;
import Cosylab.tensorflow.lite.examples.detection.tflite.Classifier;
import Cosylab.tensorflow.lite.examples.detection.tracking.DataAdapter;

public abstract class CameraActivity extends AppCompatActivity
    implements OnImageAvailableListener,
        Camera.PreviewCallback,
//        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {
  private static final Logger LOGGER = new Logger();

  private static final int PERMISSIONS_REQUEST = 1;
  protected RecyclerView recyclerView;
  protected DataAdapter dataAdapter;
  private static final String PERMISSION_CAMERA = Manifest.permission.CAMERA;
  private static final String ASSET_PATH = "";
  protected int previewWidth = 0;
  protected int previewHeight = 0;
  private boolean debug = false;
  protected Handler handler;
  private HandlerThread handlerThread;
  private boolean useCamera2API;
  private boolean isProcessingFrame = false;
  private byte[][] yuvBytes = new byte[3][];
  private int[] rgbBytes = null;
  private int yRowStride;
  protected int defaultModelIndex = 0;
  protected int defaultDeviceIndex = 0;
  private Runnable postInferenceCallback;
  private Runnable imageConverter;
  protected ArrayList<String> modelStrings = new ArrayList<String>();

  private LinearLayout bottomSheetLayout;
  private LinearLayout gestureLayout;
  private BottomSheetBehavior<LinearLayout> sheetBehavior;

  protected TextView frameValueTextView, cropValueTextView, inferenceTimeTextView;
  protected ImageView bottomSheetArrowImageView;
  //private ImageView plusImageView, minusImageView;
  protected ListView deviceView;
  protected TextView threadsTextView;
  protected TextView modelView;
  /*protected TextView resulttv;
  protected TextView resulttv2;
  protected TextView resulttv3;
  protected TextView resulttv4;
  protected TextView resulttv5;
  protected TextView resulttv6;
  protected TextView resulttv7;
  protected TextView resulttv8;
  protected TextView resulttv9;
  protected TextView resulttv10;
  protected TextView resulttv11;
  protected TextView resulttv12;
  protected TextView resulttv13;
  protected TextView resulttv14;
  protected TextView resulttv15;
  protected TextView resulttv16;
  protected TextView resulttv17;
  protected TextView resulttv18;
  protected TextView resulttv19;
  protected TextView resulttv20;
  protected TextView calorie1;
  protected TextView calorie2;
  protected TextView calorie3;
  protected TextView calorie4;
  protected TextView calorie5;
  protected TextView calorie6;
  protected TextView calorie7;
  protected TextView calorie8;
  protected TextView calorie9;
  protected TextView calorie10;
  protected TextView calorie11;
  protected TextView calorie12;
  protected TextView calorie13;
  protected TextView calorie14;
  protected TextView calorie15;
  protected TextView calorie16;
  protected TextView calorie17;
  protected TextView calorie18;
  protected TextView calorie19;
  protected TextView calorie20;
  protected TextView protien1;
  protected TextView protien2;
  protected TextView protien3;
  protected TextView protien4;
  protected TextView protien5;
  protected TextView protien6;
  protected TextView protien7;
  protected TextView protien8;
  protected TextView protien9;
  protected TextView protien10;
  protected TextView protien11;
  protected TextView protien12;
  protected TextView protien13;
  protected TextView protien14;
  protected TextView protien15;
  protected TextView protien16;
  protected TextView protien17;
  protected TextView protien18;
  protected TextView protien19;
  protected TextView protien20;
  protected TextView fat1;
  protected TextView fat2;
  protected TextView fat3;
  protected TextView fat4;
  protected TextView fat5;
  protected TextView fat6;
  protected TextView fat7;
  protected TextView fat8;
  protected TextView fat9;
  protected TextView fat10;
  protected TextView fat11;
  protected TextView fat12;
  protected TextView fat13;
  protected TextView fat14;
  protected TextView fat15;
  protected TextView fat16;
  protected TextView fat17;
  protected TextView fat18;
  protected TextView fat19;
  protected TextView fat20;
  protected TextView carbohydrates1;
  protected TextView carbohydrates2;
  protected TextView carbohydrates3;
  protected TextView carbohydrates4;
  protected TextView carbohydrates5;
  protected TextView carbohydrates6;
  protected TextView carbohydrates7;
  protected TextView carbohydrates8;
  protected TextView carbohydrates9;
  protected TextView carbohydrates10;
  protected TextView carbohydrates11;
  protected TextView carbohydrates12;
  protected TextView carbohydrates13;
  protected TextView carbohydrates14;
  protected TextView carbohydrates15;
  protected TextView carbohydrates16;
  protected TextView carbohydrates17;
  protected TextView carbohydrates18;
  protected TextView carbohydrates19;
  protected TextView carbohydrates20;*/
  protected Button button;
  protected TableLayout table;
  //protected TextView resulttv2;
  protected FloatingActionButton floatingActionButton, floatingActionButton2;
  protected Animation fabOpen, fabClose, rotateForward, rotateBackward;
  protected boolean isOpen = false;
  protected boolean table_flag = false;
  protected String[] stored_ans = new String[20];
  HashMap<String, double[]> map = new HashMap<>();
  double[][] Data_base = {{1292.62, 18.5243, 117.9331, 55.3456},
          {2845.6038,	46.5218	, 48.0386,	575.0692},
          {1254.09,	20.2032,	102.1588,	74.3688},
          {1133.847,	24.8658,	78.4866,	100.3401
          },
          {4308.56,	122.976,	345.6608,	186.6112
          },
          {102319, 	2094.2168,	914.3123,	21219.4843
          },
          {65.84,	1.9304,	0.2016,	15.2376
          },
          {412.096,	14.2451,	35.2393,	10.8713
          },
          {
                  2036.85,	41.847,	17.76,	423.1875

          },
          {
                  274.96,	12.6452,	8.7097,	40.9368

          },
          {
                  1948.3337,	136.0903,	11.139,	339.9654

          },
          {
                  260.064,	7.9439,	7.8667,	43.182

          },
          {
                  1348.031,	52.3082,	53.3953,	181.8791

          },
          {
                  594.4435,	44.6226,	4.5177,	123.7203

          },
          {
                  335.1333,	48.8644,	6.272,	21.2503

          },
          {
                  741.5895,	5.0309,	52.2894,	63.7672

          },

          {
                  334072.555,	585.2818,	648.6035,	81529.6494

          },
          {
                  2036.85,	41.847,	17.76,	423.1875

          },
          {
                  787.644,	17.5497,	4.0635,	170.7772

          },
          {
                  4213.3713,	39.846,	432.9493,	110.1684

          }
  };

  /** Current indices of device and model. */
  int currentDevice = -1;
  int currentModel = -1;
  int currentNumThreads = -1;

  ArrayList<String> deviceStrings = new ArrayList<String>();

  @Override
  protected void onCreate(final Bundle savedInstanceState) {
    LOGGER.d("onCreate " + this);
    super.onCreate(null);
    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    setContentView(R.layout.tfe_od_activity_camera);
    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayShowTitleEnabled(false);
    //Take permission for the storage of the screen short.
    //verifyStoragePermission(this);

    map.put("Indian bread", Data_base[0]);
    map.put("Rasgulla", Data_base[1]);
    map.put("Biryani", Data_base[2]);
    map.put("Uttapam", Data_base[3]);
    map.put("Paneer", Data_base[4]);
    map.put("Poha", Data_base[5]);
    map.put("Khichdi", Data_base[6]);
    map.put("Omelette", Data_base[7]);
    map.put("Plain rice", Data_base[8]);
    map.put("Dal makhani", Data_base[9]);
    map.put("Rajma", Data_base[10]);
    map.put("Poori", Data_base[11]);
    map.put("Chole", Data_base[12]);
    map.put("Dal", Data_base[13]);
    map.put("Sambhar", Data_base[14]);
    map.put("Papad", Data_base[15]);
    map.put("Gulab jamun", Data_base[16]);
    map.put("Idli", Data_base[17]);
    map.put("Vada", Data_base[18]);
    map.put("Dosa", Data_base[19]);


    if (hasPermission()) {
      setFragment();
     /* ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
              Manifest.permission.READ_EXTERNAL_STORAGE}, PackageManager.PERMISSION_GRANTED);*/
      //resulttv = (LinearLayout) findViewById(R.id.bottom_sheet_layout);

    } else {
      requestPermission();
    }

   /* ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE}, 100);*/

    /*threadsTextView = findViewById(R.id.threads);
    currentNumThreads = Integer.parseInt(threadsTextView.getText().toString().trim());*/
    /*plusImageView = findViewById(R.id.plus);
    minusImageView = findViewById(R.id.minus);*/
    //deviceView = findViewById(R.id.device_list);
    deviceStrings.add("Wts file");
    /*deviceStrings.add("GPU");
    deviceStrings.add("NNAPI");*/
    //deviceView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    ArrayAdapter<String> deviceAdapter =
            new ArrayAdapter<>(
                    CameraActivity.this , R.layout.deviceview_row, R.id.deviceview_row_text, deviceStrings);
   //deviceView.setAdapter(deviceAdapter);
    //deviceView.setItemChecked(defaultDeviceIndex, true);
    currentDevice = defaultDeviceIndex;
    /*deviceView.setOnItemClickListener(
            new AdapterView.OnItemClickListener() {
              @Override
              public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                updateActiveModel();
              }
            });*/




    /*table = (TableLayout) findViewById(R.id.table);
    resulttv = (TextView) findViewById(R.id.textview1);
    resulttv2 = (TextView) findViewById(R.id.textview2);
    resulttv3 = (TextView) findViewById(R.id.textview3);
    resulttv4 = (TextView) findViewById(R.id.textview4);
    resulttv5 = (TextView) findViewById(R.id.textview5);
    resulttv6 = (TextView) findViewById(R.id.textview6);
    resulttv7 = (TextView) findViewById(R.id.textview7);
    resulttv8 = (TextView) findViewById(R.id.textview8);
    resulttv9 = (TextView) findViewById(R.id.textview9);
    resulttv10 = (TextView) findViewById(R.id.textview10);
    resulttv11 = (TextView) findViewById(R.id.textview11);
    resulttv12 = (TextView) findViewById(R.id.textview12);
    resulttv13 = (TextView) findViewById(R.id.textview13);
    resulttv14 = (TextView) findViewById(R.id.textview14);
    resulttv15 = (TextView) findViewById(R.id.textview15);
    resulttv16 = (TextView) findViewById(R.id.textview16);
    resulttv17 = (TextView) findViewById(R.id.textview17);
    resulttv18 = (TextView) findViewById(R.id.textview18);
    resulttv19 = (TextView) findViewById(R.id.textview19);
    resulttv20 = (TextView) findViewById(R.id.textview20);
    ///////////////////////////////////////////////////
    calorie1 = (TextView) findViewById(R.id.calorie_textview1);
    calorie2 = (TextView) findViewById(R.id.calorie_textview2);
    calorie3 = (TextView) findViewById(R.id.calorie_textview3);
    calorie4 = (TextView) findViewById(R.id.calorie_textview4);
    calorie5 = (TextView) findViewById(R.id.calorie_textview5);
    calorie6 = (TextView) findViewById(R.id.calorie_textview6);
    calorie7 = (TextView) findViewById(R.id.calorie_textview7);
    calorie8 = (TextView) findViewById(R.id.calorie_textview8);
    calorie9 = (TextView) findViewById(R.id.calorie_textview9);
    calorie10 = (TextView) findViewById(R.id.calorie_textview10);
    calorie11 = (TextView) findViewById(R.id.calorie_textview11);
    calorie12 = (TextView) findViewById(R.id.calorie_textview12);
    calorie13 = (TextView) findViewById(R.id.calorie_textview13);
    calorie14 = (TextView) findViewById(R.id.calorie_textview14);
    calorie15 = (TextView) findViewById(R.id.calorie_textview15);
    calorie16 = (TextView) findViewById(R.id.calorie_textview16);
    calorie17 = (TextView) findViewById(R.id.calorie_textview17);
    calorie18 = (TextView) findViewById(R.id.calorie_textview18);
    calorie19 = (TextView) findViewById(R.id.calorie_textview19);
    calorie20 = (TextView) findViewById(R.id.calorie_textview20);
    /////////////////////////////////////////////////////////////
    protien1 = (TextView) findViewById(R.id.protien_textview1);
    protien2 = (TextView) findViewById(R.id.protien_textview2);
    protien3 = (TextView) findViewById(R.id.protien_textview3);
    protien4 = (TextView) findViewById(R.id.protien_textview4);
    protien5 = (TextView) findViewById(R.id.protien_textview5);
    protien6 = (TextView) findViewById(R.id.protien_textview6);
    protien7 = (TextView) findViewById(R.id.protien_textview7);
    protien8 = (TextView) findViewById(R.id.protien_textview8);
    protien9 = (TextView) findViewById(R.id.protien_textview9);
    protien10 = (TextView) findViewById(R.id.protien_textview10);
    protien11 = (TextView) findViewById(R.id.protien_textview11);
    protien12 = (TextView) findViewById(R.id.protien_textview12);
    protien13 = (TextView) findViewById(R.id.protien_textview13);
    protien14 = (TextView) findViewById(R.id.protien_textview14);
    protien15 = (TextView) findViewById(R.id.protien_textview15);
    protien16 = (TextView) findViewById(R.id.protien_textview16);
    protien17 = (TextView) findViewById(R.id.protien_textview17);
    protien18 = (TextView) findViewById(R.id.protien_textview18);
    protien19 = (TextView) findViewById(R.id.protien_textview19);
    protien20 = (TextView) findViewById(R.id.protien_textview20);
    /////////////////////////////////////////////////////////////
    fat1 = (TextView) findViewById(R.id.fat_textview1);
    fat2 = (TextView) findViewById(R.id.fat_textview2);
    fat3 = (TextView) findViewById(R.id.fat_textview3);
    fat4 = (TextView) findViewById(R.id.fat_textview4);
    fat5 = (TextView) findViewById(R.id.fat_textview5);
    fat6 = (TextView) findViewById(R.id.fat_textview6);
    fat7 = (TextView) findViewById(R.id.fat_textview7);
    fat8 = (TextView) findViewById(R.id.fat_textview8);
    fat9 = (TextView) findViewById(R.id.fat_textview9);
    fat10 = (TextView) findViewById(R.id.fat_textview10);
    fat11 = (TextView) findViewById(R.id.fat_textview11);
    fat12 = (TextView) findViewById(R.id.fat_textview12);
    fat13 = (TextView) findViewById(R.id.fat_textview13);
    fat14 = (TextView) findViewById(R.id.fat_textview14);
    fat15 = (TextView) findViewById(R.id.fat_textview15);
    fat16 = (TextView) findViewById(R.id.fat_textview16);
    fat17 = (TextView) findViewById(R.id.fat_textview17);
    fat18 = (TextView) findViewById(R.id.fat_textview18);
    fat19 = (TextView) findViewById(R.id.fat_textview19);
    fat20 = (TextView) findViewById(R.id.fat_textview20);
    /////////////////////////////////////////////////////////////
    carbohydrates1 = (TextView) findViewById(R.id.carbohydrates_textview1);
    carbohydrates2 = (TextView) findViewById(R.id.carbohydrates_textview2);
    carbohydrates3 = (TextView) findViewById(R.id.carbohydrates_textview3);
    carbohydrates4 = (TextView) findViewById(R.id.carbohydrates_textview4);
    carbohydrates5 = (TextView) findViewById(R.id.carbohydrates_textview5);
    carbohydrates6 = (TextView) findViewById(R.id.carbohydrates_textview6);
    carbohydrates7 = (TextView) findViewById(R.id.carbohydrates_textview7);
    carbohydrates8 = (TextView) findViewById(R.id.carbohydrates_textview8);
    carbohydrates9 = (TextView) findViewById(R.id.carbohydrates_textview9);
    carbohydrates10 = (TextView) findViewById(R.id.carbohydrates_textview10);
    carbohydrates11 = (TextView) findViewById(R.id.carbohydrates_textview11);
    carbohydrates12 = (TextView) findViewById(R.id.carbohydrates_textview12);
    carbohydrates13 = (TextView) findViewById(R.id.carbohydrates_textview13);
    carbohydrates14 = (TextView) findViewById(R.id.carbohydrates_textview14);
    carbohydrates15 = (TextView) findViewById(R.id.carbohydrates_textview15);
    carbohydrates16 = (TextView) findViewById(R.id.carbohydrates_textview16);
    carbohydrates17 = (TextView) findViewById(R.id.carbohydrates_textview17);
    carbohydrates18 = (TextView) findViewById(R.id.carbohydrates_textview18);
    carbohydrates19 = (TextView) findViewById(R.id.carbohydrates_textview19);
    carbohydrates20 = (TextView) findViewById(R.id.carbohydrates_textview20);*/


    bottomSheetLayout = findViewById(R.id.bottom_sheet_layout);
    gestureLayout = findViewById(R.id.gesture_layout);
    sheetBehavior = BottomSheetBehavior.from(bottomSheetLayout);
    bottomSheetArrowImageView = findViewById(R.id.bottom_sheet_arrow);
    //modelView = findViewById((R.id.model_list));
    /*floatingActionButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        //Toast.makeText(CameraActivity.this, "clicked", Toast.LENGTH_SHORT).show();
        if(isOpen){
          floatingActionButton.startAnimation(rotateForward);
          floatingActionButton2.startAnimation(fabClose);
          floatingActionButton2.setClickable(false);
          isOpen = false;
        }
        else{
          floatingActionButton.startAnimation(rotateBackward);
          floatingActionButton2.startAnimation(fabOpen);
          floatingActionButton2.setClickable(true);
          isOpen = true;

        }
        Toast.makeText(CameraActivity.this, "clicked", Toast.LENGTH_SHORT).show();
      }
    });

    floatingActionButton2.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        if(isOpen){
          floatingActionButton.startAnimation(rotateForward);
          floatingActionButton2.startAnimation(fabClose);
          floatingActionButton2.setClickable(false);
          isOpen = false;
        }
        else{
          floatingActionButton.startAnimation(rotateBackward);
          floatingActionButton2.startAnimation(fabOpen);
          floatingActionButton2.setClickable(true);
          isOpen = true;

        }
        Toast.makeText(CameraActivity.this, "Edit_text clicked", Toast.LENGTH_SHORT).show();
      }
    });*/
    modelStrings = getModelStrings(getAssets(), ASSET_PATH);
    //modelView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
    ArrayAdapter<String> modelAdapter =
            new ArrayAdapter<>(
                    CameraActivity.this , R.layout.listview_row, R.id.listview_row_text, modelStrings);
    //modelView.setAdapter(modelAdapter);
    //modelView.setItemChecked(defaultModelIndex, true);
    currentModel = defaultModelIndex;
    /*modelView.setOnItemClickListener(
            new AdapterView.OnItemClickListener() {
              @Override
              public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                updateActiveModel();
              }
            });*/

    ViewTreeObserver vto = gestureLayout.getViewTreeObserver();
    vto.addOnGlobalLayoutListener(
        new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
              gestureLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            } else {
              gestureLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
            //                int width = bottomSheetLayout.getMeasuredWidth();
            int height = gestureLayout.getMeasuredHeight();

            sheetBehavior.setPeekHeight(height);
          }
        });
    sheetBehavior.setHideable(false);

    sheetBehavior.setBottomSheetCallback(
        new BottomSheetBehavior.BottomSheetCallback() {
          @Override
          public void onStateChanged(@NonNull View bottomSheet, int newState) {
            switch (newState) {
              case BottomSheetBehavior.STATE_HIDDEN:
                break;
              case BottomSheetBehavior.STATE_EXPANDED:
                {
                  bottomSheetArrowImageView.setImageResource(R.drawable.arrow_down);
                }
                break;
              case BottomSheetBehavior.STATE_COLLAPSED:
                {
                  bottomSheetArrowImageView.setImageResource(R.drawable.arrow_up);
                }
                break;
              case BottomSheetBehavior.STATE_DRAGGING:
                break;
              case BottomSheetBehavior.STATE_SETTLING:
                bottomSheetArrowImageView.setImageResource(R.drawable.arrow_up);
                break;
            }
          }

          @Override
          public void onSlide(@NonNull View bottomSheet, float slideOffset) {}
        });

    /*frameValueTextView = findViewById(R.id.frame_info);
    cropValueTextView = findViewById(R.id.crop_info);
    inferenceTimeTextView = findViewById(R.id.inference_info);*/

    //plusImageView.setOnClickListener(this);
    //minusImageView.setOnClickListener(this);
    /*button.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        takeScreenShot(getWindow().getDecorView().getRootView(), "result");

      }
    });*/
  }


  /*protected static File takeScreenShot(View view, String filename){
    Date date = new Date();
    CharSequence format  = DateFormat.getTimeInstance().format(date);
    //.format()
    try{
      String dirPath= Environment.getExternalStorageDirectory().toString() + "/imagestostored";
      File fileDir = new File(dirPath);
      if(!fileDir.exists()){
        boolean mkdir = fileDir.mkdir();

      }

      String path = dirPath+"/"+filename+"-"+format+".jpeg";

      view.setDrawingCacheEnabled(true);
      Bitmap bitmap1 = Bitmap.createBitmap(view.getDrawingCache());
              //


      File imageFile = new File(path);
      FileOutputStream fileOutputStream = new FileOutputStream(imageFile);

      int quality = 100;
      bitmap1.compress(Bitmap.CompressFormat.JPEG, quality, fileOutputStream);
      fileOutputStream.flush();
      fileOutputStream.close();
      return imageFile;

    } catch (FileNotFoundException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;


  }

  protected static final int REQUEST_EXTERNAL_STORAGE=1;
  protected static String[] PERMISSION_STORAGE={
          Manifest.permission.WRITE_EXTERNAL_STORAGE,
          Manifest.permission.READ_EXTERNAL_STORAGE
  };

  public static void verifyStoragePermission(Activity activity){
    int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
    if(permission!=PackageManager.PERMISSION_GRANTED){
             ActivityCompat.requestPermissions(activity, PERMISSION_STORAGE, REQUEST_EXTERNAL_STORAGE);
    }
  }*/

  protected ArrayList<String> getModelStrings(AssetManager mgr, String path){
    ArrayList<String> res = new ArrayList<String>();
    try {
      String[] files = mgr.list(path);
      for (String file : files) {
        String[] splits = file.split("\\.");
        if (splits[splits.length - 1].equals("tflite")) {
          res.add(file);
        }
      }

    }
    catch (IOException e){
      System.err.println("getModelStrings: " + e.getMessage());
    }
    return res;
  }

  protected int[] getRgbBytes() {
    imageConverter.run();
    return rgbBytes;
  }

  protected int getLuminanceStride() {
    return yRowStride;
  }

  protected byte[] getLuminance() {
    return yuvBytes[0];
  }

  /** Callback for android.hardware.Camera API */
  @Override
  public void onPreviewFrame(final byte[] bytes, final Camera camera) {
    if (isProcessingFrame) {
      LOGGER.w("Dropping frame!");
      return;
    }

    try {
      // Initialize the storage bitmaps once when the resolution is known.
      if (rgbBytes == null) {
        Camera.Size previewSize = camera.getParameters().getPreviewSize();
        previewHeight = previewSize.height;
        previewWidth = previewSize.width;
        rgbBytes = new int[previewWidth * previewHeight];
        onPreviewSizeChosen(new Size(previewSize.width, previewSize.height), 90);
      }
    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      return;
    }

    isProcessingFrame = true;
    yuvBytes[0] = bytes;
    yRowStride = previewWidth;

    imageConverter =
        new Runnable() {
          @Override
          public void run() {
            ImageUtils.convertYUV420SPToARGB8888(bytes, previewWidth, previewHeight, rgbBytes);
          }
        };

    postInferenceCallback =
        new Runnable() {
          @Override
          public void run() {
            camera.addCallbackBuffer(bytes);
            isProcessingFrame = false;
          }
        };
    processImage();

  }

  /** Callback for Camera2 API */
  @Override
  public void onImageAvailable(final ImageReader reader) {
    // We need wait until we have some size from onPreviewSizeChosen
    if (previewWidth == 0 || previewHeight == 0) {
      return;
    }
    if (rgbBytes == null) {
      rgbBytes = new int[previewWidth * previewHeight];
    }
    try {
      final Image image = reader.acquireLatestImage();

      if (image == null) {
        return;
      }

      if (isProcessingFrame) {
        image.close();
        return;
      }
      isProcessingFrame = true;
      Trace.beginSection("imageAvailable");
      final Plane[] planes = image.getPlanes();
      fillBytes(planes, yuvBytes);
      yRowStride = planes[0].getRowStride();
      final int uvRowStride = planes[1].getRowStride();
      final int uvPixelStride = planes[1].getPixelStride();

      imageConverter =
          new Runnable() {
            @Override
            public void run() {
              ImageUtils.convertYUV420ToARGB8888(
                  yuvBytes[0],
                  yuvBytes[1],
                  yuvBytes[2],
                  previewWidth,
                  previewHeight,
                  yRowStride,
                  uvRowStride,
                  uvPixelStride,
                  rgbBytes);
            }
          };

      postInferenceCallback =
          new Runnable() {
            @Override
            public void run() {
              image.close();
              isProcessingFrame = false;
            }
          };

      processImage();

    } catch (final Exception e) {
      LOGGER.e(e, "Exception!");
      Trace.endSection();
      return;
    }
    Trace.endSection();
  }

  @Override
  public synchronized void onStart() {
    LOGGER.d("onStart " + this);
    super.onStart();
  }

  @Override
  public synchronized void onResume() {
    LOGGER.d("onResume " + this);
    super.onResume();

    handlerThread = new HandlerThread("inference");
    handlerThread.start();
    handler = new Handler(handlerThread.getLooper());
  }

  @Override
  public synchronized void onPause() {
    LOGGER.d("onPause " + this);

    handlerThread.quitSafely();
    try {
      handlerThread.join();
      handlerThread = null;
      handler = null;
    } catch (final InterruptedException e) {
      LOGGER.e(e, "Exception!");
    }

    super.onPause();
  }

  @Override
  public synchronized void onStop() {
    LOGGER.d("onStop " + this);
    super.onStop();
  }

  @Override
  public synchronized void onDestroy() {
    LOGGER.d("onDestroy " + this);
    super.onDestroy();
  }

  protected synchronized void runInBackground(final Runnable r) {
    if (handler != null) {
      handler.post(r);
    }
  }

  @Override
  public void onRequestPermissionsResult(
      final int requestCode, final String[] permissions, final int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == PERMISSIONS_REQUEST) {
      if (allPermissionsGranted(grantResults)) {
        setFragment();
      } else {
        requestPermission();
      }
    }
  }

  private static boolean allPermissionsGranted(final int[] grantResults) {
    for (int result : grantResults) {
      if (result != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  private boolean hasPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

      return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;


    } else {
      return true;
    }
  }

  private void requestPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (shouldShowRequestPermissionRationale(PERMISSION_CAMERA)) {
        Toast.makeText(
                CameraActivity.this,
                "Camera permission is required for this demo",
                Toast.LENGTH_LONG)
            .show();
      }
      requestPermissions(new String[] {PERMISSION_CAMERA}, PERMISSIONS_REQUEST);
    }
  }

  // Returns true if the device supports the required hardware level, or better.
  private boolean isHardwareLevelSupported(
      CameraCharacteristics characteristics, int requiredLevel) {
    int deviceLevel = characteristics.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
    if (deviceLevel == CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY) {
      return requiredLevel == deviceLevel;
    }
    // deviceLevel is not LEGACY, can use numerical sort
    return requiredLevel <= deviceLevel;
  }

  private String chooseCamera() {
    final CameraManager manager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
    try {
      for (final String cameraId : manager.getCameraIdList()) {
        final CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);

        // We don't use a front facing camera in this sample.
        final Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
        if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
          continue;
        }

        final StreamConfigurationMap map =
            characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);

        if (map == null) {
          continue;
        }

        // Fallback to camera1 API for internal cameras that don't have full support.
        // This should help with legacy situations where using the camera2 API causes
        // distorted or otherwise broken previews.
        useCamera2API =
            (facing == CameraCharacteristics.LENS_FACING_EXTERNAL)
                || isHardwareLevelSupported(
                    characteristics, CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
        LOGGER.i("Camera API lv2?: %s", useCamera2API);
        return cameraId;
      }
    } catch (CameraAccessException e) {
      LOGGER.e(e, "Not allowed to access camera");
    }

    return null;
  }

  protected void setFragment() {
    String cameraId = chooseCamera();

    Fragment fragment;
    if (useCamera2API) {

      CameraConnectionFragment camera2Fragment =
          CameraConnectionFragment.newInstance(
              new CameraConnectionFragment.ConnectionCallback() {
                @Override
                public void onPreviewSizeChosen(final Size size, final int rotation) {
                  previewHeight = size.getHeight();
                  previewWidth = size.getWidth();
                  CameraActivity.this.onPreviewSizeChosen(size, rotation);
                }
              },
              this,
              getLayoutId(),
              getDesiredPreviewFrameSize());

      camera2Fragment.setCamera(cameraId);
      fragment = camera2Fragment;
    } else {
      fragment =
          new LegacyCameraConnectionFragment(this, getLayoutId(), getDesiredPreviewFrameSize());
    }

    getFragmentManager().beginTransaction().replace(R.id.container, fragment).commit();
  }

  protected void fillBytes(final Plane[] planes, final byte[][] yuvBytes) {
    // Because of the variable row stride it's not possible to know in
    // advance the actual necessary dimensions of the yuv planes.
    for (int i = 0; i < planes.length; ++i) {
      final ByteBuffer buffer = planes[i].getBuffer();
      if (yuvBytes[i] == null) {
        LOGGER.d("Initializing buffer %d at size %d", i, buffer.capacity());
        yuvBytes[i] = new byte[buffer.capacity()];
      }
      buffer.get(yuvBytes[i]);
    }
  }

  public boolean isDebug() {
    return debug;
  }

  protected void readyForNextImage() {
    if (postInferenceCallback != null) {
      postInferenceCallback.run();
    }
  }

  protected int getScreenOrientation() {
    switch (getWindowManager().getDefaultDisplay().getRotation()) {
      case Surface.ROTATION_270:
        return 270;
      case Surface.ROTATION_180:
        return 180;
      case Surface.ROTATION_90:
        return 90;
      default:
        return 0;
    }
  }

//  @Override
//  public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
//    setUseNNAPI(isChecked);
//    if (isChecked) apiSwitchCompat.setText("NNAPI");
//    else apiSwitchCompat.setText("TFLITE");
//  }

  @Override
  public void onClick(View v) {
    if (v.getId() == 0) {
      String threads = threadsTextView.getText().toString().trim();
      int numThreads = Integer.parseInt(threads);
      if (numThreads >= 9) return;
      numThreads++;
      threadsTextView.setText(String.valueOf(numThreads));
      setNumThreads(numThreads);
    } else if (v.getId() == 0) {
      String threads = threadsTextView.getText().toString().trim();
      int numThreads = Integer.parseInt(threads);
      if (numThreads == 1) {
        return;
      }
      numThreads--;
      threadsTextView.setText(String.valueOf(numThreads));
      setNumThreads(numThreads);
    }
  }

  protected void showFrameInfo(String frameInfo) {
   // frameValueTextView.setText(frameInfo);
  }

  protected void showCropInfo(String cropInfo) {
    //cropValueTextView.setText(cropInfo);
  }

  protected void showInference(String inferenceTime) {
   // inferenceTimeTextView.setText(inferenceTime);
  }

  protected abstract void updateActiveModel();
  protected abstract void processImage();

  protected abstract void onPreviewSizeChosen(final Size size, final int rotation);

  protected abstract int getLayoutId();

  protected abstract Size getDesiredPreviewFrameSize();

  protected abstract void setNumThreads(int numThreads);

  protected abstract void setUseNNAPI(boolean isChecked);



}
