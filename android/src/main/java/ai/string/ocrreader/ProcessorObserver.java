package ai.string.ocrreader;

/**
 * Created by rafael on 30/01/17.
 */
public interface ProcessorObserver {

    void notifyDetections(String value);
}
