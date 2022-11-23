package Cosylab.tensorflow.lite.examples.detection.tracking;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import org.tensorflow.lite.examples.detection.R;

import java.util.List;

import Cosylab.tensorflow.lite.examples.detection.DataModel;

public class DataAdapter extends RecyclerView.Adapter<DataModel.ViewHolder> {

    Context context;
    List<DataModel> dataModelList;

    public DataAdapter(Context context, List<DataModel> dataModelList) {
        this.context = context;
        this.dataModelList = dataModelList;
    }

    @NonNull
    @Override
    public DataModel.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view  = LayoutInflater.from(context).inflate(R.layout.item_layout, parent, false);

        return new DataModel.ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull DataModel.ViewHolder holder, int position) {
                  if(dataModelList!=null && dataModelList.size()>0){
                      DataModel model = dataModelList.get(position);
                      holder.item_dish.setText(model.getDish());
                      holder.item_calories.setText(model.getCalories());
                      holder.item_protien.setText(model.getProtien());
                      holder.item_fat.setText(model.getFat());
                      holder.item_carboydrates.setText(model.getCarboydrates());
                  }else{
                      return;
                  }
    }

    @Override
    public int getItemCount() {
        return dataModelList.size();
    }

    public class ViewHolder {
    }
}
