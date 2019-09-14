package karoldrwila.human_detection.models;

public class Classification
{
    private float conf;
    private String label;

    void update(float conf, String label)
    {
        if(this.conf < conf)
        {
            this.conf = conf;
            this.label = label;
        }
    }

    public String getLabel()
    {
        return label;
    }

    public float getConf()
    {
        return conf;
    }
}
