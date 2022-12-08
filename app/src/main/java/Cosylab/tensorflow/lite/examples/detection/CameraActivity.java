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
import android.widget.ProgressBar;
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
/*
* In the below code "CameraActivity extends AppComatActivity" means when user open an application then this activvity will create first.
* As or very first activity is video frame hence we need to use "implements OnImageAvailableListener"
* And inside it we are using "View.OnclickListner" because this view wil going to captured pictures.
* */
public abstract class CameraActivity extends AppCompatActivity
    implements OnImageAvailableListener,
        Camera.PreviewCallback,
//        CompoundButton.OnCheckedChangeListener,
        View.OnClickListener {
  private static final Logger LOGGER = new Logger();

  private static final int PERMISSIONS_REQUEST = 1; //This is somthing like request code which will required to get the permission of storage.
  protected int currentprogress = 0;  //Don't worry about this I have not used it.
  protected TextView progress_percentage; // This is the TextView for the calories changes after capturing images.
  protected ProgressBar progressBar, progressBar2, progressBar3; //This is the declaration for the progressbar (Level indicator). Don't worry about the progressBar2 and progressBar3 I have not used both this in code.
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
  protected Button button;
  protected TableLayout table;
  //protected TextView resulttv2;
  protected FloatingActionButton floatingActionButton, floatingActionButton2;
  protected Animation fabOpen, fabClose, rotateForward, rotateBackward;
  protected boolean isOpen = false;
  protected boolean table_flag = false;
  protected String[] stored_ans = new String[20];
  HashMap<String, double[]> map = new HashMap<>();
  /*
  * DATA IS TAKEN FROM "https://www.nutritionix.com/food/idli"
  * As RecipeDB has some wrong dataset that's I have taken the value for each product from the above mentioned link.
  * "Data_base" is the double typed 2D array which storing the values for calories, protien, fat and carbohydrates.
  * This 2D array has size of 20*4 (20 rows and 4 columns).
  * If you visit to the folder name assets and customclasses.txt file then youe will see the ranking for each food class there.
  * According to their ranking I have stored dataset of calories, protien, fat and carbohydrates here in this "Data_base" array.
  * "Dhokla", "Khandvi", "kabab", "Gatte". Don't have data on "https://www.nutritionix.com/food."
  * "Dahipuri" data taken from "https://www.tarladalal.com/calories-for-dahi-puri-2807#:~:text=One%20plate%20of%20Dahi%20Puri%20gives%20367%20calories.,adult%20diet%20of%202%2C000%20calories."
  * In "Dahipuri" one plate data was given, in one plate 6 dahipuri come so I divided whole data by 6 and set here for one dahipuri data in dataset.
  * For "EggBhurji" data is taken from "https://www.fatsecret.co.za/calories-nutrition/generic/egg-bhurji".
  * for "MuttarMashroom" data is taken from "https://www.tarladalal.com/calories-for-mushroom-mutter-masala-6400".
  * for "Thupka" data is taken from "https://www.stltoday.com/lifestyles/food-and-cooking/recipes/thukpa-tibetan-soup/article_34d102fd-8b47-58e8-a576-8297b2405756.html#:~:text=Per%20serving%3A%20439%20calories%3B%2015g,%3B%2068mg%20calcium%3B%20468mg%20potassium.".
  * for "Gatte" data is taken from "https://www.tarladalal.com/calories-for-gatte-ki-subzi-4344#:~:text=One%20serving%20of%20Gatte%20ki%20Subzi%20gives%20385%20calories.,fat%20which%20is%20213%20calories.".
  * for "Khandvi" data is taken from "https://recipes.sparkpeople.com/recipe-calories.asp?recipe=2132534".
  * This data for "Khandvi" for 4 pices but I have divided this data by 4 and got data for 1 khandvi.
  * */
  double[][] Data_base = {{120,	3.1	,3.7,	18},
          {120	,1.7,	1.8,	25},
          {292	,20,	9.4	,31},
          {160,	4,	5,	25
          },
          {365	,22,	29,	3.6
          },
          {158	,2.9,	0.2,	35
          },
          {238	,11,	3.4,	41
          },
          {323	,21,	25,	1.4
          },
          {
                  205,	4.3,	0.4,	45

          },
          {
                  274.96,	12.6452,	8.7097,	40.9368

          },
          {
                  252	,13	,6.5	,38

          },
          {
                  141	,2.3	,9.8	,12

          },
          {
                  281	,7.7	,16	,29

          },
          {
                  222	,14	,4.2	,34

          },
          {
                  260	,14	,1.3	,53

          },
          {
                  29	,1.3	,1.1	,3.4

          },

          {
                  149	,1.9	,7.3	,20

          },
          {
                  58	,1.6	,0.4	,12

          },
          {
                  135	,4.4	,8.4	,11

          },
          {
                  168	,3.9	,3.7	,29

          },
          {
            150 ,1.3 ,3.5, 29
          },
          {
            261, 3.5, 17, 24
          },
          {
                  390,12,11,63
          },
          {
                  152,5.7,7.4,16
          }
          ,
          {
                  195,3,11,21
          },
          {
                  307,36,15,6.1
          },
          {
                  61,3.4,2.2, 7.2
          },
          {
                  262,8.4,10.6,34
          }
          ,
          {
                  83,1.5,5.3,7.7
          },
          {
                  263,7.5,9.5,37
          },
          {
                  220,6,7.7,33
          },
          {
                  127,7,2,21
          }
          ,
          {
                  320,5,18,36
          },
          {
                  196,5.2,17,9.6
          },
          {
                  240,3,20,11
          },
          {
                  439,23,17,53
          }
          ,
          {
                  35.8,1.33,1.3,3.68
          },
          {
                  182,13,14,2
          },
          {
                  128,2.8,7.1,14
          },
          {
                  64,2.2,3.1,8.7
          }
          ,
          {
                  94,1.5,2.4,17
          },
          {
                  385,11.6,23.7,25.8
          }
          ,
          {
                  113,5,5,12
          }
          ,
          {
                  0,0,0,0
          }
          ,
          {
                  242,3.8,14,27
          }
          ,
          {
                  6,1.1,0.1,0.1
          }
          ,
          {
                  115,6.2,1.9,28
          }
          ,
          {
                  61.16,1.23,3.68,4.96
          }
          ,
          {
                  109,7.5,5.5,7.8
          }
          ,
          {
                  250,21,18,0
          }
          ,
          {
                  111,2.5,5.5,15
          }
          ,
          {
                  198,9.01,12.05,15.18
          }
          ,
          {
                  221,4.9,2.9,43
          }
          ,
          {
                  330,4,26,20
          }
          ,
          {
                  123,7.8,2.1,18.8
          }
          ,
          {
                  275,5.6,13,35
          }
          ,
          {
                  120,1,5,19
          }
          ,
          {
                  139,2.8,5.7,20
          }
          ,
          {
                  291,33,12,11
          }
          ,
          {
                  263,31,12,6.1
          },
          {
              69, 1.9, 2.7, 11
          }

  };

  /** Current indices of device and model. */
  /*
  * Neglect this all three variables (currentDevice, currentModel, and currentNumThreads).
  * */
  int currentDevice = -1;
  int currentModel = -1;
  int currentNumThreads = -1;
/*
* Neglect this deviceString array list as well.
* */
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
    /*
    * This is the hash map I have build here.
    * This map storing an array having values of calories, fat , protein and carbohydrates corresponds to dish it belongs.
    *
    *
    * */
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
    map.put("Jalebi", Data_base[20]);
    map.put("Samosa", Data_base[21]);
    map.put("Paobhaji", Data_base[22]);
    map.put("Dhokla", Data_base[23]);
    map.put("Barfi", Data_base[24]);
    map.put("Fishcurry", Data_base[25]);
    map.put("Momos", Data_base[26]);
    map.put("Kheer", Data_base[27]);
    map.put("Kachori", Data_base[28]);
    map.put("Vadapav", Data_base[29]);
    map.put("Rasmalai", Data_base[30]);
    map.put("Kalachana", Data_base[31]);
    map.put("Chaat", Data_base[32]);
    map.put("Saag", Data_base[33]);
    map.put("Dumaloo", Data_base[34]);
    map.put("Thupka", Data_base[35]);
    map.put("Khandvi", Data_base[36]);
    map.put("Kabab", Data_base[37]);
    map.put("Thepla", Data_base[38]);
    map.put("Rasam", Data_base[39]);
    map.put("Appam", Data_base[40]);
    map.put("Gatte", Data_base[41]);
      map.put("Kadhipakora", Data_base[42]);
      map.put("Ghewar", Data_base[43]);
      map.put("Aloomatter", Data_base[44]);
      map.put("Prawns", Data_base[45]);
      map.put("Sandwich", Data_base[46]);
      map.put("Dahipuri", Data_base[47]);
      map.put("Haleem", Data_base[48]);
      map.put("Mutton", Data_base[49]);
      map.put("Aloogobi", Data_base[50]);
      map.put("Eggbhurji", Data_base[51]);
      map.put("Lemonrice", Data_base[52]);
      map.put("Bhindimasala", Data_base[53]);
      map.put("Matarmushroom", Data_base[54]);
      map.put("Gajarkahalwa", Data_base[55]);
      map.put("Motichoorladoo", Data_base[56]);
      map.put("Ragiroti", Data_base[57]);
      map.put("Chickentikka", Data_base[58]);
      map.put("Tandoorichicken", Data_base[59]);
      map.put("Lauki", Data_base[60]);

    /*
    * "hasPermission() is the function, please find the function below in this code."
    * "requestPermission() is the function, please find the function below in this code.
    * */
    if (hasPermission()) {
      /*
      * "setFragment()" is the function, please find the function below in this code.
      * */
      setFragment();
    } else {
      /*
      * "requestPermission() is the function, please find the function below in this code.
      * */
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

    /*
    * Don't look at this code.
    * This "deviceAdapter" adapter is no connection with this code.
    * So don't be confuse.
    *
    * */
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
  /*
  * This getModelString() we are not using anywhere here.
  *
  * */
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
 /*
 * "hasPermission()" is function which is build to get the camera perission from the user.
 * */
  private boolean hasPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

      return checkSelfPermission(PERMISSION_CAMERA) == PackageManager.PERMISSION_GRANTED;


    } else {
      return true;
    }
  }
/*
*
* "requestPermission()" here we are requesting the permission.
* */
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
  /*
  * Here we are checking if the hardware of the user mobile supported or not.
  * */
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

  /*
  * Here we setting the fragment.
  * The name of the fragment is CameraConnectionFragment.
  * Whatever buttons, progressbar, gesture control you are seeing it is on the fragment only.
  * We are calling the fragment from this CameraActivity.
  *
  *
  * */
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
    /*if (v.getId() == 0) {
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
    }*/
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
