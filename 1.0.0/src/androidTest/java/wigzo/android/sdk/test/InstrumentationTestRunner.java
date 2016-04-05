
package wigzo.android.sdk.test;

import android.os.Bundle;

public class InstrumentationTestRunner extends android.test.InstrumentationTestRunner {
    // TODO: since Android 4.3 dexmaker requires this workaround, can be removed once dexmaker fixes this issue http://code.google.com/p/dexmaker/issues/detail?id=2
    @Override
    public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        System.setProperty("dexmaker.dexcache", getTargetContext().getCacheDir().toString());
    }
}
