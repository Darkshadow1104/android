package Cosylab.tensorflow.lite.examples.detection;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import org.tensorflow.lite.examples.detection.R;


public class Fragment2 extends Fragment {

    protected EditText edit1, edit2;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_2, container, false);
        edit1 = (EditText) view.findViewById(R.id.editText1);
        edit2 = (EditText) view.findViewById(R.id.editText2);
        String height = (String) edit1.getText().toString();
        String weight = (String) edit2.getText().toString();
        return view;
    }
}