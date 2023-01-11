

package Cosylab.tensorflow.lite.examples.detection.customview;

import java.util.List;
import Cosylab.tensorflow.lite.examples.detection.tflite.Classifier.Recognition;

public interface ResultsView {
  public void setResults(final List<Recognition> results);
}
