package ai.string.ocrreader;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;

public class OcrReaderModule extends ReactContextBaseJavaModule {

  public OcrReaderModule(ReactApplicationContext reactContext) {
    super(reactContext);

  }

  @Override
  public String getName() {
    return "OcrReaderModule";
  }
}
