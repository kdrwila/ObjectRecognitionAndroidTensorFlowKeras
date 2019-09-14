package karoldrwila.human_detection.models;

import android.content.res.AssetManager;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

public class TensorFlowClassifier implements Classifier
{
    private static final float THRESHOLD = 0.5f;

    private TensorFlowInferenceInterface tfHelper;

    private String name;
    private String inputName;
    private String outputName;

    private String[] labels = {"NOT_HUMAN", "HUMAN"};
    private float[] output;
    private String[] outputNames;

    public TensorFlowClassifier(AssetManager assetManager, String name,
                                              String modelPath, String inputName, String outputName)
    {
        this.name = name;

        this.inputName = inputName;
        this.outputName = outputName;

        this.tfHelper = new TensorFlowInferenceInterface(assetManager, modelPath);

        this.outputNames = new String[]{outputName};

        this.outputName = outputName;
        this.output = new float[2];
    }

    @Override
    public String name()
    {
        return name;
    }

    @Override
    public Classification recognize(final float[] pixels)
    {
        tfHelper.feed(inputName, pixels, 1, 100, 100, 3);
        tfHelper.run(outputNames);
        tfHelper.fetch(outputName, output);

        Classification ans = new Classification();
        for (int i = 0; i < output.length; ++i)
        {
            if (output[i] > THRESHOLD)
            {
                ans.update(output[i], labels[i]);
            }
        }

        return ans;
    }
}
