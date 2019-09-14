package karoldrwila.human_detection.models;

public interface Classifier
{
    String name();
    Classification recognize(final float[] pixels);
}
