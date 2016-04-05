
package wigzo.android.sdk;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * This class is the public API for the Wigzo Android SDK.
 * Get more details <a href="https://github.com/Wigzo/wigzo-sdk-android">here</a>.
 */
public class Wigzo {

    /**
     * Current version of the wigzo Android SDK as a displayable string.
     */
    public static final String WIGZO_SDK_VERSION_STRING = "1.0";
    /**
     * Default string used in the begin session metrics if the
     * app version cannot be found.
     */
    public static final String DEFAULT_APP_VERSION = "1.0";
    /**
     * Tag used in all logging in the wigzo SDK.
     */
    public static final String TAG = "Wigzo";

    /**
     * Determines how many custom events can be queued locally before
     * an attempt is made to submit them to a wigzo server.
     */
    private static final int EVENT_QUEUE_SIZE_THRESHOLD = 10;
    /**
     * How often onTimer() is called.
     */
    private static final long TIMER_DELAY_IN_SECONDS = 60;

    protected static List<String> publicKeyPinCertificates;

    /**
     * Enum used in Wigzo.initMessaging() method which controls what kind of
     * app installation it is. Later (in Wigzo Dashboard or when calling Wigzo API method),
     * you'll be able to choose whether you want to send a message to test devices,
     * or to production ones.
     */
    public static enum WigzoMessagingMode {
        TEST,
        PRODUCTION,
    }

    // see http://stackoverflow.com/questions/7048198/thread-safe-singletons-in-java
    private static class SingletonHolder {
        static final Wigzo instance = new Wigzo();
    }

    private ConnectionQueue connectionQueue_;
    @SuppressWarnings("FieldCanBeLocal")
    private ScheduledExecutorService timerService_;
    private EventQueue eventQueue_, mobileQueue;
    private long prevSessionDurationStartTime_;
    private int activityCount_;
    private boolean disableUpdateSessionRequests_;
    private boolean enableLogging_;
    private WigzoMessagingMode messagingMode_;
    private Context context_;

    //user data access
    public static UserData userData;

    //track views
    private String lastView = null;
    private int lastViewStart = 0;
    private boolean firstView = true;
    private boolean autoViewTracker = false;

    /**
     * Returns the Wigzo singleton.
     */
    public static Wigzo sharedInstance() {

        return SingletonHolder.instance;
    }

    /**
     * Constructs a Wigzo object.
     * Creates a new ConnectionQueue and initializes the session timer.
     */
    Wigzo() {
        connectionQueue_ = new ConnectionQueue();
        Wigzo.userData = new UserData(connectionQueue_);
        timerService_ = Executors.newSingleThreadScheduledExecutor();
        timerService_.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                onTimer();
            }
        }, TIMER_DELAY_IN_SECONDS, TIMER_DELAY_IN_SECONDS, TimeUnit.SECONDS);
    }


    /**
     * Initializes the Wigzo SDK. Call from your main Activity's onCreate() method.
     * Must be called before other SDK methods can be used.
     * Device ID is supplied by OpenUDID service if available, otherwise Advertising ID is used.
     * BE CAUTIOUS!!!! If neither OpenUDID, nor Advertising ID is available, Wigzo will ignore this user.
     * @param context application context
     * @param serverURL URL of the Wigzo server to submit data to; use "https://analytics.wigzo.com" for Wigzo Cloud
     * @param appKey app key for the application being tracked; find in the Wigzo Dashboard under Management &gt; Applications
     * @param orgId    unique Id for organization provided by wigzo to organization
     * @return Wigzo instance for easy method chaining
     * @throws java.lang.IllegalArgumentException if context, serverURL, appKey, or deviceID are invalid
     * @throws java.lang.IllegalStateException if the Wigzo SDK has already been initialized
     */
    public Wigzo init(final Context context, final String serverURL, final String appKey,final String orgId) {
        return init(context, serverURL, appKey, null, OpenUDIDAdapter.isOpenUDIDAvailable() ? DeviceId.Type.OPEN_UDID : DeviceId.Type.ADVERTISING_ID, orgId);
    }

    /**
     * Initializes the Wigzo SDK. Call from your main Activity's onCreate() method.
     * Must be called before other SDK methods can be used.
     * @param context application context
     * @param serverURL URL of the Wigzo server to submit data to; use "https://analytics.wigzo.com" for Wigzo Cloud
     * @param appKey app key for the application being tracked; find in the Wigzo Dashboard under Management &gt; Applications
     * @param deviceID unique ID for the device the app is running on; note that null in deviceID means that Wigzo will fall back to OpenUDID, then, if it's not available, to Google Advertising ID
     * @param orgId    unique Id for organization provided by wigzo to organization
     * @return Wigzo instance for easy method chaining
     * @throws IllegalArgumentException if context, serverURL, appKey, or deviceID are invalid
     * @throws IllegalStateException if init has previously been called with different values during the same application instance
     */
    public Wigzo init(final Context context, final String serverURL, final String appKey, final String deviceID, String orgId) {
        return init(context, serverURL, appKey, deviceID, orgId);
    }

    /**
     * Initializes the Wigzo SDK. Call from your main Activity's onCreate() method.
     * Must be called before other SDK methods can be used.
     * @param context application context
     * @param serverURL URL of the Wigzo server to submit data to; use "https://analytics.wigzo.com" for Wigzo Cloud
     * @param appKey app key for the application being tracked; find in the Wigzo Dashboard under Management &gt; Applications
     * @param orgId    unique Id for organization provided by wigzo to organization
     * @param deviceID unique ID for the device the app is running on; note that null in deviceID means that Wigzo will fall back to OpenUDID, then, if it's not available, to Google Advertising ID
     * @param idMode enum value specifying which device ID generation strategy Wigzo should use: OpenUDID or Google Advertising ID
     * @return Wigzo instance for easy method chaining
     * @throws IllegalArgumentException if context, serverURL, appKey, or deviceID are invalid
     * @throws IllegalStateException if init has previously been called with different values during the same application instance
     */
    public synchronized Wigzo init(final Context context, final String serverURL, final String appKey, final String deviceID, DeviceId.Type idMode, final String orgId) {
        if (context == null) {
            throw new IllegalArgumentException("valid context is required");
        }
        if(orgId==null || orgId.length() == 0){
            throw new IllegalArgumentException("Valid Organization Id is required!");
        }
        if (!isValidURL(serverURL)) {
            throw new IllegalArgumentException("valid serverURL is required");
        }
        if (appKey == null || appKey.length() == 0) {
            throw new IllegalArgumentException("valid appKey is required");
        }
        if (deviceID != null && deviceID.length() == 0) {
            throw new IllegalArgumentException("valid deviceID is required");
        }
        if (deviceID == null && idMode == null) {
            if (OpenUDIDAdapter.isOpenUDIDAvailable()) idMode = DeviceId.Type.OPEN_UDID;
            else if (AdvertisingIdAdapter.isAdvertisingIdAvailable()) idMode = DeviceId.Type.ADVERTISING_ID;
        }
        if (deviceID == null && idMode == DeviceId.Type.OPEN_UDID && !OpenUDIDAdapter.isOpenUDIDAvailable()) {
            throw new IllegalArgumentException("valid deviceID is required because OpenUDID is not available");
        }
        if (deviceID == null && idMode == DeviceId.Type.ADVERTISING_ID && !AdvertisingIdAdapter.isAdvertisingIdAvailable()) {
            throw new IllegalArgumentException("valid deviceID is required because Advertising ID is not available (you need to include Google Play services 4.0+ into your project)");
        }
        if (eventQueue_ != null && (!connectionQueue_.getServerURL().equals(serverURL) ||
                                    !connectionQueue_.getAppKey().equals(appKey) ||
                                    !DeviceId.deviceIDEqualsNullSafe(deviceID, idMode, connectionQueue_.getDeviceId()) )) {
            throw new IllegalStateException("Wigzo cannot be reinitialized with different values");
        }

        // In some cases WigzoMessaging does some background processing, so it needs a way
        // to start Wigzo on itself
        if (MessagingAdapter.isMessagingAvailable()) {
            MessagingAdapter.storeConfiguration(context, serverURL, appKey, deviceID, idMode, orgId);
        }

        // if we get here and eventQueue_ != null, init is being called again with the same values,
        // so there is nothing to do, because we are already initialized with those values
        if (eventQueue_ == null) {
            DeviceId deviceIdInstance;
            if (deviceID != null) {
                deviceIdInstance = new DeviceId(deviceID);
            } else {
                deviceIdInstance = new DeviceId(idMode);
            }

            final WigzoStore wigzoStore = new WigzoStore(context);

            deviceIdInstance.init(context, wigzoStore, true);
            connectionQueue_.setOrganizationId(orgId);
            connectionQueue_.setServerURL(serverURL);
            connectionQueue_.setAppKey(appKey);
            connectionQueue_.setWigzoStore(wigzoStore);
            connectionQueue_.setDeviceId(deviceIdInstance);

            eventQueue_ = new EventQueue(wigzoStore);
        }

        if (mobileQueue == null) {
            DeviceId deviceIdInstance;
            if (deviceID != null) {
                deviceIdInstance = new DeviceId(deviceID);
            } else {
                deviceIdInstance = new DeviceId(idMode);
            }

            final WigzoAppStore wigzoAppStore = new WigzoAppStore(context);

            deviceIdInstance.init(context, wigzoAppStore, true);

            connectionQueue_.setServerURL(serverURL);
            connectionQueue_.setAppKey(appKey);
            connectionQueue_.setOrganizationId(orgId);
            connectionQueue_.setWigzoAppStore(wigzoAppStore);
            connectionQueue_.setDeviceId(deviceIdInstance);

            mobileQueue = new EventQueue(wigzoAppStore);
        }

        context_ = context;

        // context is allowed to be changed on the second init call
        connectionQueue_.setContext(context);

        return this;
    }

    /**
     * Checks whether Wigzo.init has been already called.
     * @return true if Wigzo is ready to use
     */
    public synchronized boolean isInitialized() {
        return eventQueue_ != null;
    }

    public synchronized boolean isInitializedMobile() {
        return mobileQueue != null;
    }

    /**
     * Initializes the Wigzo MessagingSDK. Call from your main Activity's onCreate() method.
     * @param activity application activity which acts as a final destination for notifications
     * @param activityClass application activity class which acts as a final destination for notifications
     * @param projectID ProjectID for this app from Google API Console
     * @param mode whether this app installation is a test release or production
     * @return Wigzo instance for easy method chaining
     * @throws IllegalStateException if no WigzoMessaging class is found (you need to use wigzo-messaging-sdk-android library instead of wigzo-sdk-android)
     */
    public Wigzo initMessaging(Activity activity, Class<? extends Activity> activityClass, String projectID, WigzoMessagingMode mode, String orgId) {
        return initMessaging(activity, activityClass, projectID, null, mode, orgId);
    }
    /**
     * Initializes the Wigzo MessagingSDK. Call from your main Activity's onCreate() method.
     * @param activity application activity which acts as a final destination for notifications
     * @param activityClass application activity class which acts as a final destination for notifications
     * @param projectID ProjectID for this app from Google API Console
     * @param buttonNames Strings to use when displaying Dialogs (uses new String[]{"Open", "Review"} by default)
     * @param orgId    unique Id for organization provided by wigzo to organization
     * @param mode whether this app installation is a test release or production
     * @return Wigzo instance for easy method chaining
     * @throws IllegalStateException if no WigzoMessaging class is found (you need to use wigzo-messaging-sdk-android library instead of wigzo-sdk-android)
     */
    public synchronized Wigzo initMessaging(Activity activity, Class<? extends Activity> activityClass, String projectID, String[] buttonNames, WigzoMessagingMode mode, String orgId) {
        if (mode != null && !MessagingAdapter.isMessagingAvailable()) {
            throw new IllegalStateException("you need to include wigzo-messaging-sdk-android library instead of wigzo-sdk-android if you want to use Wigzo Messaging");
        } else {
            messagingMode_ = mode;
            if (!MessagingAdapter.init(activity, activityClass, projectID, buttonNames)) {
                throw new IllegalStateException("couldn't initialize Wigzo Messaging");
            }
        }

        if (MessagingAdapter.isMessagingAvailable()) {
            MessagingAdapter.storeConfiguration(connectionQueue_.getContext(), connectionQueue_.getServerURL(), connectionQueue_.getAppKey(), connectionQueue_.getDeviceId().getId(), connectionQueue_.getDeviceId().getType(), connectionQueue_.getOrganizationId());
        }

        return this;
    }

    /**
     * Immediately disables session &amp; event tracking and clears any stored session &amp; event data.
     * This API is useful if your app has a tracking opt-out switch, and you want to immediately
     * disable tracking when a user opts out. The onStart/onStop/recordEvent methods will throw
     * IllegalStateException after calling this until Wigzo is reinitialized by calling init
     * again.
     */
    public synchronized void halt() {
        eventQueue_ = null;
        final WigzoStore wigzoStore = connectionQueue_.getWigzoStore();
        final WigzoAppStore wigzoAppStore = connectionQueue_.getWigzoAppStore();
        if (wigzoStore != null) {
            wigzoStore.clear();
        }
        if (wigzoAppStore != null) {
            wigzoAppStore.clear();
        }
        connectionQueue_.setOrganizationId(null);
        connectionQueue_.setContext(null);
        connectionQueue_.setServerURL(null);
        connectionQueue_.setAppKey(null);
        connectionQueue_.setWigzoStore(null);
        connectionQueue_.setWigzoAppStore(null);
        prevSessionDurationStartTime_ = 0;
        activityCount_ = 0;
    }

    /**
     * Tells the Wigzo SDK that an Activity has started. Since Android does not have an
     * easy way to determine when an application instance starts and stops, you must call this
     * method from every one of your Activity's onStart methods for accurate application
     * session tracking.
     * @throws IllegalStateException if Wigzo SDK has not been initialized
     */
    public synchronized void onStart(Activity activity) {
        if (eventQueue_ == null) {
            throw new IllegalStateException("init must be called before onStart");
        }

        ++activityCount_;
        if (activityCount_ == 1) {
            onStartHelper();
        }

        //check if there is an install referrer data
        String referrer = ReferrerReceiver.getReferrer(context_);
        if (Wigzo.sharedInstance().isLoggingEnabled()) {
            Log.d(Wigzo.TAG, "Checking referrer: " + referrer);
        }
        if(referrer != null){
            connectionQueue_.sendReferrerData(referrer);
            ReferrerReceiver.deleteReferrer(context_);
        }

        CrashDetails.inForeground();

        if(autoViewTracker){
            recordView(activity.getClass().getName());
        }
    }

    /**
     * Called when the first Activity is started. Sends a begin session event to the server
     * and initializes application session tracking.
     */
    void onStartHelper() {
        prevSessionDurationStartTime_ = System.nanoTime();
        connectionQueue_.beginSession();
    }

    /**
     * Tells the Wigzo SDK that an Activity has stopped. Since Android does not have an
     * easy way to determine when an application instance starts and stops, you must call this
     * method from every one of your Activity's onStop methods for accurate application
     * session tracking.
     * @throws IllegalStateException if Wigzo SDK has not been initialized, or if
     *                               unbalanced calls to onStart/onStop are detected
     */
    public synchronized void onStop() {
        if (eventQueue_ == null) {
            throw new IllegalStateException("init must be called before onStop");
        }
        if (activityCount_ == 0) {
            throw new IllegalStateException("must call onStart before onStop");
        }

        --activityCount_;
        if (activityCount_ == 0) {
            onStopHelper();
        }

        CrashDetails.inBackground();

        //report current view duration
        reportViewDuration();
    }

    /**
     * Called when final Activity is stopped. Sends an end session event to the server,
     * also sends any unsent custom events.
     */
    void onStopHelper() {
        connectionQueue_.endSession(roundedSecondsSinceLastSessionDurationUpdate());
        prevSessionDurationStartTime_ = 0;

        if (eventQueue_.size() > 0) {
            connectionQueue_.recordEvents(eventQueue_.events());
        }
        if (mobileQueue.mobilesize() > 0) {
            connectionQueue_.recordEvents(mobileQueue.mobileEvents());
        }
    }

    /**
     * Called when GCM Registration ID is received. Sends a token session event to the server.
     */
    public void onRegistrationId(String registrationId) {
        connectionQueue_.tokenSession(registrationId, messagingMode_);
    }

    /**
     * Records a custom event with no segmentation values, a count of one and a sum of zero.
     * @param key name of the custom event, required, must not be the empty string
     * @throws IllegalStateException if Wigzo SDK has not been initialized
     * @throws IllegalArgumentException if key is null or empty
     */
    public void recordEvent(final String key) {
        recordEvent(key, null, 1, 0);

    }

    /**
     * Records a custom event with no segmentation values, the specified count, and a sum of zero.
     * @param key name of the custom event, required, must not be the empty string
     * @param count count to associate with the event, should be more than zero
     * @throws IllegalStateException if Wigzo SDK has not been initialized
     * @throws IllegalArgumentException if key is null or empty
     */
    public void recordEvent(final String key, final int count) {
        recordEvent(key, null, count, 0);
    }

    /**
     * Records a custom event with no segmentation values, and the specified count and sum.
     * @param key name of the custom event, required, must not be the empty string
     * @param count count to associate with the event, should be more than zero
     * @param sum sum to associate with the event
     * @throws IllegalStateException if Wigzo SDK has not been initialized
     * @throws IllegalArgumentException if key is null or empty
     */
    public void recordEvent(final String key, final int count, final double sum) {
        recordEvent(key, null, count, sum);
    }

    /**
     * Records a custom event with the specified segmentation values and count, and a sum of zero.
     * @param key name of the custom event, required, must not be the empty string
     * @param segmentation segmentation dictionary to associate with the event, can be null
     * @param count count to associate with the event, should be more than zero
     * @throws IllegalStateException if Wigzo SDK has not been initialized
     * @throws IllegalArgumentException if key is null or empty
     */
    public void recordEvent(final String key, final Map<String, String> segmentation, final int count) {
        recordEvent(key, segmentation, count, 0);
    }

    /**
     * Records a custom event with the specified values.
     * @param key name of the custom event, required, must not be the empty string
     * @param segmentation segmentation dictionary to associate with the event, can be null
     * @param count count to associate with the event, should be more than zero
     * @param sum sum to associate with the event
     * @throws IllegalStateException if Wigzo SDK has not been initialized
     * @throws IllegalArgumentException if key is null or empty, count is less than 1, or if
     *                                  segmentation contains null or empty keys or values
     */
    public synchronized void recordEvent(final String key, final Map<String, String> segmentation, final int count, final double sum) {
        System.out.println("WIGZO OBJECT INSIDE recordEvent:::::::: " + this);
        if (!isInitialized()) {
            throw new IllegalStateException("Wigzo.sharedInstance().init must be called before recordEvent");
        }
        if (!isInitializedMobile()) {
            throw new IllegalStateException("Mobile.sharedInstance().init must be called before recordEvent");
        }
        if (key == null || key.length() == 0) {
            throw new IllegalArgumentException("Valid Wigzo event key is required");
        }
        if (count < 1) {
            throw new IllegalArgumentException("Wigzo event count should be greater than zero");
        }
        if (segmentation != null) {
            for (String k : segmentation.keySet()) {
                if (k == null || k.length() == 0) {
                    throw new IllegalArgumentException("Wigzo event segmentation key cannot be null or empty");
                }
                if (segmentation.get(k) == null || segmentation.get(k).length() == 0) {
                    throw new IllegalArgumentException("Wigzo event segmentation value cannot be null or empty");
                }
            }
        }
        eventQueue_.recordEvent(key, segmentation, count, sum);
        mobileQueue.recordMobileEvent(key, segmentation, count, sum);


        sendEventsIfNeeded();
    }

    /**
     * Enable or disable automatic view tracking
     * @param enable boolean for the state of automatic view tracking
     */
    public synchronized Wigzo setViewTracking(boolean enable){
        autoViewTracker = enable;
        return this;
    }

    /**
     * Check state of automatic view tracking
     * @return boolean - true if enabled, false if disabled
     */
    public synchronized boolean isViewTrackingEnabled(){
        return autoViewTracker;
    }

    /* Record a view manualy, without automatic tracking
     * or track view that is not automatically tracked
     * like fragment, Message box or transparent Activity
     * @param boolean - true if enabled, false if disabled
     */
    public synchronized Wigzo recordView(String viewName){
        reportViewDuration();
        lastView = viewName;
        lastViewStart = Wigzo.currentTimestamp();
        HashMap<String, String> segments = new HashMap<String, String>();
        segments.put("name", viewName);
        segments.put("visit", "1");
        segments.put("segment", "Android");
        if(firstView) {
            firstView = false;
            segments.put("start", "1");
        }
        recordEvent("[CLY]_view", segments, 1);
        return this;
    }

    /**
     * Sets information about user. Possible keys are:
     * <ul>
     * <li>
     * name - (String) providing user's full name
     * </li>
     * <li>
     * username - (String) providing user's nickname
     * </li>
     * <li>
     * email - (String) providing user's email address
     * </li>
     * <li>
     * organization - (String) providing user's organization's name where user works
     * </li>
     * <li>
     * phone - (String) providing user's phone number
     * </li>
     * <li>
     * picture - (String) providing WWW URL to user's avatar or profile picture
     * </li>
     * <li>
     * picturePath - (String) providing local path to user's avatar or profile picture
     * </li>
     * <li>
     * gender - (String) providing user's gender as M for male and F for female
     * </li>
     * <li>
     * byear - (int) providing user's year of birth as integer
     * </li>
     * </ul>
     * @param data Map&lt;String, String&gt; with user data
     * @deprecated use {@link #Wigzo().sharedInstance().userData.setUserData(Map<String, String>)} to set data and {@link #Wigzo().sharedInstance().userData.save()} to send it to server.
     */
    public synchronized Wigzo setUserData(Map<String, String> data) {
        return setUserData(data, null);
    }

    /**
     * Sets information about user with custom properties.
     * In custom properties you can provide any string key values to be stored with user
     * Possible keys are:
     * <ul>
     * <li>
     * name - (String) providing user's full name
     * </li>
     * <li>
     * username - (String) providing user's nickname
     * </li>
     * <li>
     * email - (String) providing user's email address
     * </li>
     * <li>
     * organization - (String) providing user's organization's name where user works
     * </li>
     * <li>
     * phone - (String) providing user's phone number
     * </li>
     * <li>
     * picture - (String) providing WWW URL to user's avatar or profile picture
     * </li>
     * <li>
     * picturePath - (String) providing local path to user's avatar or profile picture
     * </li>
     * <li>
     * gender - (String) providing user's gender as M for male and F for female
     * </li>
     * <li>
     * byear - (int) providing user's year of birth as integer
     * </li>
     * </ul>
     * @param data Map&lt;String, String&gt; with user data
     * @param customdata Map&lt;String, String&gt; with custom key values for this user
     * @deprecated use {@link #Wigzo().sharedInstance().userData.setUserData(Map<String, String>, Map<String, String>)} to set data and {@link #Wigzo().sharedInstance().userData.save()} to send it to server.
     */
    public synchronized Wigzo setUserData(Map<String, String> data, Map<String, String> customdata) {
        UserData.setData(data);
        if(customdata != null)
            UserData.setCustomData(customdata);
        connectionQueue_.sendUserData();
        UserData.clear();
        return this;
    }

    /**
     * Sets custom properties.
     * In custom properties you can provide any string key values to be stored with user
     * @param customdata Map&lt;String, String&gt; with custom key values for this user
     * @deprecated use {@link #Wigzo().sharedInstance().userData.setCustomUserData(Map<String, String>)} to set data and {@link #Wigzo().sharedInstance().userData.save()} to send it to server.
     */
    public synchronized Wigzo setCustomUserData(Map<String, String> customdata) {
        if(customdata != null)
            UserData.setCustomData(customdata);
        connectionQueue_.sendUserData();
        UserData.clear();
        return this;
    }

    /**
     * Set user location.
     *
     * Wigzo detects user location based on IP address. But for geolocation-enabled apps,
     * it's better to supply exact location of user.
     * Allows sending messages to a custom segment of users located in a particular area.
     *
     * @param lat Latitude
     * @param lon Longitude
     */
    public synchronized Wigzo setLocation(double lat, double lon) {
        connectionQueue_.getWigzoStore().setLocation(lat, lon);
        connectionQueue_.getWigzoAppStore().setLocation(lat,lon);

        if (disableUpdateSessionRequests_) {
            connectionQueue_.updateSession(roundedSecondsSinceLastSessionDurationUpdate());
        }

        return this;
    }

    /**
     * Sets custom segments to be reported with crash reports
     * In custom segments you can provide any string key values to segments crashes by
     * @param segments Map&lt;String, String&gt; key segments and their values
     */
    public synchronized Wigzo setCustomCrashSegments(Map<String, String> segments) {
        if(segments != null)
            CrashDetails.setCustomSegments(segments);
        return this;
    }

    /**
     * Add crash breadcrumb like log record to the log that will be send together with crash report
     * @param record String a bread crumb for the crash report
     */
    public synchronized Wigzo addCrashLog(String record) {
        CrashDetails.addLog(record);
        return this;
    }

    /**
     * Log handled exception to report it to server as non fatal crash
     * @param exception Exception to log
     */
    public synchronized Wigzo logException(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        connectionQueue_.sendCrashReport(sw.toString(), true);
        return this;
    }

    /**
     * Enable crash reporting to send unhandled crash reports to server
     */
    public synchronized Wigzo enableCrashReporting() {
        //get default handler
        final Thread.UncaughtExceptionHandler oldHandler = Thread.getDefaultUncaughtExceptionHandler();

        Thread.UncaughtExceptionHandler handler = new Thread.UncaughtExceptionHandler() {

            @Override
            public void uncaughtException(Thread t, Throwable e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                Wigzo.sharedInstance().connectionQueue_.sendCrashReport(sw.toString(), false);

                //if there was another handler before
                if(oldHandler != null){
                    //notify it also
                    oldHandler.uncaughtException(t,e);
                }
            }
        };

        Thread.setDefaultUncaughtExceptionHandler(handler);
        return this;
    }

    /**
     * Disable periodic session time updates.
     * By default, Wigzo will send a request to the server each 30 seconds with a small update
     * containing session duration time. This method allows you to disable such behavior.
     * Note that event updates will still be sent every 10 events or 30 seconds after event recording.
     * @param disable whether or not to disable session time updates
     * @return Wigzo instance for easy method chaining
     */
    public synchronized Wigzo setDisableUpdateSessionRequests(final boolean disable) {
        disableUpdateSessionRequests_ = disable;
        return this;
    }

    /**
     * Sets whether debug logging is turned on or off. Logging is disabled by default.
     * @param enableLogging true to enable logging, false to disable logging
     * @return Wigzo instance for easy method chaining
     */
    public synchronized Wigzo setLoggingEnabled(final boolean enableLogging) {
        enableLogging_ = enableLogging;
        return this;
    }

    public synchronized boolean isLoggingEnabled() {
        return enableLogging_;
    }

    /**
     * Reports duration of last view
     */
    void reportViewDuration(){
        if(lastView != null){
            HashMap<String, String> segments = new HashMap<String, String>();
            segments.put("name", lastView);
            /*
            TODO:
            report event with duration
             */
            //recordEvent("[CLY]_view", Wigzo.currentTimestamp()-lastViewStart, segments);
            lastView = null;
            lastViewStart = 0;
        }
    }

    /**
     * Submits all of the locally queued events to the server if there are more than 10 of them.
     */
    void sendEventsIfNeeded() {
        if (eventQueue_.size() >= EVENT_QUEUE_SIZE_THRESHOLD) {
            connectionQueue_.recordEvents(eventQueue_.events());

        }
        if (mobileQueue.mobilesize() >= EVENT_QUEUE_SIZE_THRESHOLD) {
            connectionQueue_.recordEvents(mobileQueue.mobileEvents());

        }

    }

    /**
     * Called every 60 seconds to send a session heartbeat to the server. Does nothing if there
     * is not an active application session.
     */
    synchronized void onTimer() {
        final boolean hasActiveSession = activityCount_ > 0;
        if (hasActiveSession) {
            if (!disableUpdateSessionRequests_) {
                connectionQueue_.updateSession(roundedSecondsSinceLastSessionDurationUpdate());
            }
            if (eventQueue_.size() > 0) {
                connectionQueue_.recordEvents(eventQueue_.events());
            }
            if (mobileQueue.mobilesize() > 0) {
                connectionQueue_.recordEvents(mobileQueue.events());
            }
        }
    }

    /**
     * Calculates the unsent session duration in seconds, rounded to the nearest int.
     */
    int roundedSecondsSinceLastSessionDurationUpdate() {
        final long currentTimestampInNanoseconds = System.nanoTime();
        final long unsentSessionLengthInNanoseconds = currentTimestampInNanoseconds - prevSessionDurationStartTime_;
        prevSessionDurationStartTime_ = currentTimestampInNanoseconds;
        return (int) Math.round(unsentSessionLengthInNanoseconds / 1000000000.0d);
    }

    /**
     * Utility method to return a current timestamp that can be used in the wigzo API.
     */
   public static int currentTimestamp() {
        return ((int)(System.currentTimeMillis() / 1000l));
    }

    /**
     * Utility method to return a current hour of the day that can be used in the wigzo API.
     */
    static int currentHour(){return Calendar.getInstance().get(Calendar.HOUR_OF_DAY); }

    /**
     * Utility method to return a current day of the week that can be used in the wigzo API.
     */
    static int currentDayOfWeek(){
        int day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
        switch (day) {
            case Calendar.MONDAY:
                return 1;
            case Calendar.TUESDAY:
                return 2;
            case Calendar.WEDNESDAY:
                return 3;
            case Calendar.THURSDAY:
                return 4;
            case Calendar.FRIDAY:
                return 5;
            case Calendar.SATURDAY:
                return 6;
        }
        return 0;
    }

    /**
     * Utility method for testing validity of a URL.
     */
    static boolean isValidURL(final String urlStr) {
        boolean validURL = false;
        if (urlStr != null && urlStr.length() > 0) {
            try {
                new URL(urlStr);
                validURL = true;
            }
            catch (MalformedURLException e) {
                validURL = false;
            }
        }
        return validURL;
    }

    /**
     * Allows public key pinning.
     * Supply list of SSL certificates (base64-encoded strings between "-----BEGIN CERTIFICATE-----" and "-----END CERTIFICATE-----" without end-of-line)
     * along with server URL starting with "https://". Wigzo will only accept connections to the server
     * if public key of SSL certificate provided by the server matches one provided to this method.
     * @param certificates List of SSL certificates
     * @return Wigzo instance
     */
    public static Wigzo enablePublicKeyPinning(List<String> certificates) {
        publicKeyPinCertificates = certificates;
        return Wigzo.sharedInstance();
    }

    // for unit testing
    ConnectionQueue getConnectionQueue() { return connectionQueue_; }
    void setConnectionQueue(final ConnectionQueue connectionQueue) { connectionQueue_ = connectionQueue; }
    ExecutorService getTimerService() { return timerService_; }
    EventQueue getEventQueue() { return eventQueue_; }
    void setEventQueue(final EventQueue eventQueue) { eventQueue_ = eventQueue; }
    long getPrevSessionDurationStartTime() { return prevSessionDurationStartTime_; }
    void setPrevSessionDurationStartTime(final long prevSessionDurationStartTime) { prevSessionDurationStartTime_ = prevSessionDurationStartTime; }
    int getActivityCount() { return activityCount_; }
    synchronized boolean getDisableUpdateSessionRequests() { return disableUpdateSessionRequests_; }

    public void stackOverflow() {
        this.stackOverflow();
    }

    public synchronized Wigzo crashTest(int crashNumber) {

        if (crashNumber == 1){
            if (Wigzo.sharedInstance().isLoggingEnabled()) {
                Log.d(Wigzo.TAG, "Running crashTest 1");
            }

            stackOverflow();

        }else if (crashNumber == 2){

            if (Wigzo.sharedInstance().isLoggingEnabled()) {
                Log.d(Wigzo.TAG, "Running crashTest 2");
            }

            int test = 10/0;

        }else if (crashNumber == 3){

            if (Wigzo.sharedInstance().isLoggingEnabled()) {
                Log.d(Wigzo.TAG, "Running crashTest 3");
            }

            Object[] o = null;
            while (true) { o = new Object[] { o }; }


        }else if (crashNumber == 4){

            if (Wigzo.sharedInstance().isLoggingEnabled()) {
                Log.d(Wigzo.TAG, "Running crashTest 4");
            }

            throw new RuntimeException("This is a crash");
        }
        else{
            if (Wigzo.sharedInstance().isLoggingEnabled()) {
                Log.d(Wigzo.TAG, "Running crashTest 5");
            }

            String test = null;
            test.charAt(1);
        }
        return Wigzo.sharedInstance();
    }
}
