package wigzo.android.sdk;

import android.os.Build;
import android.util.Log;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;

/**
 * Created by wigzo on 17/3/16.
 */
public class ConnectionProcessorWigzoApp implements Runnable {

    private static final int CONNECT_TIMEOUT_IN_MILLISECONDS = 30000;
    private static final int READ_TIMEOUT_IN_MILLISECONDS = 30000;

    private WigzoAppStore wigzoAppStore =null;
    private final DeviceId deviceId_;
    private final String serverURL_;
    private final SSLContext sslContext_;

    ConnectionProcessorWigzoApp(final String serverURL, final WigzoAppStore store, final DeviceId deviceId, final SSLContext sslContext) {
        serverURL_ = serverURL;
        wigzoAppStore = store;
        deviceId_ = deviceId;
        sslContext_ = sslContext;

        // HTTP connection reuse which was buggy pre-froyo
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }

    URLConnection mobileEventData(final String eventData) throws IOException {
        String urlStr = "http://vikram.wigzoes.com/mobile/events/i?";
        Log.d("server url: ", urlStr);
        // if(!eventData.contains("&crash="))
        urlStr += eventData;
        final URL url = new URL(urlStr);
        HttpURLConnection conn = null;
        if (Wigzo.publicKeyPinCertificates == null) {
            conn = (HttpURLConnection)url.openConnection();
        } else {
            HttpsURLConnection c = (HttpsURLConnection)url.openConnection();
            c.setSSLSocketFactory(sslContext_.getSocketFactory());
            conn = c;
        }
        conn.setConnectTimeout(CONNECT_TIMEOUT_IN_MILLISECONDS);
        conn.setReadTimeout(READ_TIMEOUT_IN_MILLISECONDS);
        conn.setUseCaches(false);
        conn.setRequestProperty("Content-Type", "Application/json");
        conn.setDoInput(true);
        String picturePath = UserData.getPicturePathFromQuery(url);
        Log.d("Url : ", ""+url);
        if(!picturePath.equals("")){
            conn = null;
        }
        else if(eventData.contains("&crash=")){
            conn = null;
        }
        return conn;
    }


    @Override
    public void run() {
        while (true) {

            final String[] mobileEvents = wigzoAppStore.connections();
            Log.d("mobile events size: " , ""+mobileEvents.length);
            if (mobileEvents == null || mobileEvents.length == 0) {
                // currently no data to send, we are done for now
                break;
            }

            // get first event from collection
            if (deviceId_.getId() == null) {
                // When device ID is supplied by OpenUDID or by Google Advertising ID.
                // In some cases it might take time for them to initialize. So, just wait for it.
                if (Wigzo.sharedInstance().isLoggingEnabled()) {
                    Log.i(Wigzo.TAG, "No Device ID available yet, skipping request " + mobileEvents[0]);
                }
                break;
            }

            final String mobileData = mobileEvents[0]+ "&device_id=" + deviceId_.getId();


            URLConnection conn = null;
            URLConnection mobileconn = null;
            BufferedInputStream responseStream = null;
            BufferedInputStream responseMobile = null;
            try {
                // initialize and open connection

                mobileconn = mobileEventData(mobileData);
                if (null != mobileconn) {
                    mobileconn.connect();
                    // consume response stream
                    responseMobile = new BufferedInputStream(mobileconn.getInputStream());
                    if (null != responseMobile) {
                        final ByteArrayOutputStream responseDatas = new ByteArrayOutputStream(256);
                        int c;
                        while ((c = responseMobile.read()) != -1) {
                            responseDatas.write(c);
                        }

                        boolean success = true;
                        if (mobileconn instanceof HttpURLConnection) {
                            final HttpURLConnection httpConnmobile = (HttpURLConnection) mobileconn;

                            final int responseCode = httpConnmobile.getResponseCode();
                            success = responseCode >= 200 && responseCode < 300;
                            if (!success && Wigzo.sharedInstance().isLoggingEnabled()) {
                                Log.w(Wigzo.TAG, "HTTP error response code was " + responseCode + " from submitting event data: " + mobileData);
                            }
                        }

                        if (success) {
                            final JSONObject responseDict = new JSONObject(responseDatas.toString());
                            success = responseDict.get("result").toString().equalsIgnoreCase("success");
                            if (!success && Wigzo.sharedInstance().isLoggingEnabled()) {
                                Log.w(Wigzo.TAG, "Response from Wigzo server did not report success, it was: " + responseDatas.toString("UTF-8"));
                            }
                        }

                        if (success) {
                            if (Wigzo.sharedInstance().isLoggingEnabled()) {
                                Log.d(Wigzo.TAG, "ok ->" + mobileData);

                            }

                            // successfully submitted event data to Count.wigzo.ly server, so remove
                            // this one from the stored events collection

                            wigzoAppStore.removeConnection(mobileEvents[0]);
                        }
                        else {
                            // warning was logged above, stop processing, let next tick take care of retrying
                            break;
                        }
                        Log.d("response Mobile server", ""+responseDatas.toString());
                    }
                }


            }
            catch (Exception e) {
                if (Wigzo.sharedInstance().isLoggingEnabled()) {
                    Log.w(Wigzo.TAG, "Got exception while trying to submit event data: " + mobileData, e);
                }
                // if exception occurred, stop processing, let next tick take care of retrying
                break;
            }
            finally {
                // free connection resources
                if (responseStream != null) {
                    try { responseStream.close(); } catch (IOException ignored) {}
                }
                if (conn != null && conn instanceof HttpURLConnection) {
                    ((HttpURLConnection)conn).disconnect();
                }
            }
        }
    }

    // for unit testing
    String getServerURL() { return serverURL_; }

    WigzoAppStore getWigzoAppStore(){ return wigzoAppStore;}
    DeviceId getDeviceId() { return deviceId_; }
}
