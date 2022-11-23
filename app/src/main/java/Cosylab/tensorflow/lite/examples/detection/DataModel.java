package Cosylab.tensorflow.lite.examples.detection;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.tensorflow.lite.examples.detection.R;

public class DataModel {
    String Dish;
    String Calories;
    String Protien;
    String Fat;
    String Carboydrates;

    public DataModel(String[] dataarray){
        this.Dish = dataarray[0];
        this.Calories = dataarray[1];
        this.Protien = dataarray[2];
        this.Fat = dataarray[3];
        this.Carboydrates = dataarray[4];
    }

    public String getDish() {
        return Dish;
    }

    public String getCalories() {
        return Calories;
    }

    public String getProtien() {
        return Protien;
    }

    public String getFat() {
        return Fat;
    }

    public String getCarboydrates() {
        return Carboydrates;
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView item_dish;
        public TextView item_calories;
        public TextView item_protien;
        public TextView item_fat;
        public TextView item_carboydrates;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            item_dish = itemView.findViewById(R.id.item_dish);
            item_calories = itemView.findViewById(R.id.item_calories);
            item_protien = itemView.findViewById(R.id.item_protien);
            item_fat = itemView.findViewById(R.id.item_fat);
            item_carboydrates = itemView.findViewById(R.id.item_carboydrates);
        }
    }
}
