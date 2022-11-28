package Cosylab.tensorflow.lite.examples.detection;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.lite.examples.detection.R;

public class TakingInputFromUser extends AppCompatActivity {

    public EditText height;
    public EditText weight, age;
    public double main_BMR;
    public Button Done_button;
    //Float BMI_Index;
    public Spinner spinnerheight, spinnerweight, spinnersex, spinneractivity;
    public double height_value, weight_value;
    public ProgressBar progressBar;
    public TextView progressText;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_taking_input_from_user);
        /*height = (EditText) findViewById(R.id.textView4);
        weight = (EditText) findViewById(R.id.textView7);
        String Height = (String) height.getText().toString();
        String Weight = (String) weight.getText().toString();
        Float HEIGHT = (Float) Float.parseFloat(Height); // IN CM
        Float WEIGHT = (Float) Float.parseFloat(Weight); // IN KG
        //Converting the values from cm to m.
        HEIGHT = HEIGHT/100; //CONVERTED IN THE METER.
        //The formula for the BMI is BMI = weight (kg) / (height (m) )^2
        BMI_Index =  (Float) WEIGHT/(HEIGHT*HEIGHT);*/
        progressText = (TextView) findViewById(R.id.percentage_increase);
        height = (EditText) findViewById(R.id.textView9);
        weight = (EditText) findViewById(R.id.textView8);
        age = (EditText) findViewById(R.id.textView10);
        spinnerheight = findViewById(R.id.spinner2);
        spinnerweight = findViewById(R.id.spinner);
        spinnersex = findViewById(R.id.spinner4);
        spinneractivity = findViewById(R.id.spinner5);
        Done_button = (Button) findViewById(R.id.button2);
        String[] height_units = getResources().getStringArray(R.array.Height_units);
        String[] weight_units = getResources().getStringArray(R.array.Weight_units);
        String[] sex_units = getResources().getStringArray(R.array.Sex_units);
        String[] activity = getResources().getStringArray(R.array.Activity);
        ArrayAdapter adapter_height = new ArrayAdapter(TakingInputFromUser.this, android.R.layout.simple_spinner_item, height_units);
        ArrayAdapter adapter_weight = new ArrayAdapter(TakingInputFromUser.this, android.R.layout.simple_spinner_item, weight_units);
        ArrayAdapter adapter_sex = new ArrayAdapter(TakingInputFromUser.this, android.R.layout.simple_spinner_item, sex_units);
        ArrayAdapter adapter_activity = new ArrayAdapter(TakingInputFromUser.this, android.R.layout.simple_spinner_item,activity);
        adapter_height.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter_weight.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter_sex.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        adapter_activity.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerheight.setAdapter(adapter_height);
        spinnerweight.setAdapter(adapter_weight);
        spinnersex.setAdapter(adapter_sex);
        spinneractivity.setAdapter(adapter_activity);



        Done_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(height.length()==0){
                    Toast.makeText(TakingInputFromUser.this, "Please enter your height", Toast.LENGTH_SHORT).show();
                }
                else if(weight.length()==0){
                    Toast.makeText(TakingInputFromUser.this, "Please enter your weight", Toast.LENGTH_SHORT).show();
                }
                else if(age.length()==0){
                    Toast.makeText(TakingInputFromUser.this, "Please enter your age", Toast.LENGTH_SHORT).show();
                }
                else if(height.length()==0 && weight.length()==0 && age.length()==0){
                    Toast.makeText(TakingInputFromUser.this,"Please enter height, weight and age" , Toast.LENGTH_SHORT).show();
                }
                else if(height.length()==0 && weight.length()==0){
                    Toast.makeText(TakingInputFromUser.this,"Please enter height and weight" , Toast.LENGTH_SHORT).show();
                }
                else if(height.length()==0 && age.length()==0){
                    Toast.makeText(TakingInputFromUser.this,"Please enter height and age" , Toast.LENGTH_SHORT).show();
                }
                else if(weight.length()==0 && age.length()==0){
                    Toast.makeText(TakingInputFromUser.this,"Please enter weight and age" , Toast.LENGTH_SHORT).show();
                }
                else {
                    String sex = spinnersex.getSelectedItem().toString();

                    String height_spinner =  spinnerheight.getSelectedItem().toString();
                    Log.d("height_spinner", String.valueOf(height_spinner));
                    String weight_spinner = spinnerweight.getSelectedItem().toString();
                    String activity_spinner = spinneractivity.getSelectedItem().toString();
                    height_value = (double) Double.parseDouble(height.getText().toString());
                    weight_value = (double) Double.parseDouble(weight.getText().toString());
                    int age_value = (int) Double.parseDouble(age.getText().toString());
                    if(height_spinner.equals("inch")){
                        /*
                         * Converting height from "inch" to "cm".
                         * */
                        height_value = height_value * 2.54;
                        Log.d("height_value", String.valueOf(height_value));

                    }
                    if(weight_spinner.equals("pound")){
                        /*
                         * Converting weight from "pound" to "kg".
                         * */
                        weight_value = weight_value * 0.4536;

                    }


                    if(sex.equals("Male")){


                        main_BMR = BMR_Men(weight_value, height_value, age_value);


                    }
                    else{

                        main_BMR = BMR_Women(weight_value, height_value, age_value);

                    }
                    /*
                     * HERE WE HAVE CALCULATED THE CALORIES REQUIRED FOR THE MAN OR WOMEN.
                     * WE HAVE USE THE FORMULA GIVEN BELOW.
                     * Sedentary (little or no exercise): calories = BMR × 1.2;
                     * Lightly active (light exercise/sports 1-3 days/week): calories = BMR × 1.375;
                     * Moderately active (moderate exercise/sports 3-5 days/week): calories = BMR × 1.55;
                     * Very active (hard exercise/sports 6-7 days a week): calories = BMR × 1.725;
                     * If you are extra active (very hard exercise/sports & a physical job): calories = BMR × 1.9
                     * */
                    double mycalorie = Required_calories(main_BMR, activity_spinner);

                    Log.d("Nitesh Calories", String.valueOf(mycalorie));
                    //progressText.setText(String.valueOf(mycalorie));
                    //final Handler handler = new Handler();

                }



            }
        });




    }

    public double BMR_Men(double weight_kg, double height_cm, int age_yrs){
        /*
        * This is the function to calculate the BMR value for Men
        * */
        double BMR = 66.5 + (13.75 * weight_kg) + (5.003 * height_cm) - (6.75 * age_yrs);
        return BMR;
    }

    public double BMR_Women(double weight_kg, double height_cm, int age_yrs){
        /*
         * This is the function to calculate the BMR value for Women
         * */
        double BMR = 655.1 + (9.563 * weight_kg) + (1.850 * height_cm) - (4.676 * age_yrs);
        return BMR;
    }

    public double Required_calories(double my_bmr, String my_activity){

        /*
        * This is the function is build to calculate the required calorie for each person according to their daily routine.
        * We have use the Benedict equations in the BMR_Men and BMR_Women functions above to calculate the exact BMR value.
        * And after that according to that users daily routine we rae calculating the required calories per day.
        * */


        double calories = 0;
        if(my_activity.equals("Sedentary (little or no exercise)")){
            calories = my_bmr * 1.2;
        }
        else if(my_activity.equals("Lightly active (light exercise/sports 1-3 days/week)")){
             calories = my_bmr * 1.375;
        }
        else if(my_activity.equals("Moderately active (moderate exercise/sports 3-5 days/week)")){
              calories = my_bmr * 1.55;
        }
        else if(my_activity.equals("Very active (hard exercise/sports 6-7 days a week)")){
             calories = my_bmr * 1.725;
        }
        else if(my_activity.equals("If you are extra active (very hard exercise/sports & a physical job)")){
            calories = my_bmr * 1.9;
        }
        return calories;
    }





}