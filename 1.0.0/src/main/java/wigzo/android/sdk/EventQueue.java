
package wigzo.android.sdk;

import org.json.JSONArray;

import java.io.UnsupportedEncodingException;
import java.util.List;
import java.util.Map;

/**
 * This class queues event data locally and can convert that event data to JSON
 * for submission to a wigzo server.
 *
 * None of the methods in this class are synchronized because access to this class is
 * controlled by the Wigzo singleton, which is synchronized.
 *
 * NOTE: This class is only public to facilitate unit testing, because
 *       of this bug in dexmaker: https://code.google.com/p/dexmaker/issues/detail?id=34
 */
public class EventQueue {
    private  WigzoStore wigzoStore_ = null;
    private WigzoAppStore wigzoAppStore_ = null;

    /**
     * Constructs an EventQueue.
     * @param wigzoStore backing store to be used for local event queue persistence
     */
    EventQueue(final WigzoStore wigzoStore) {
        wigzoStore_ = wigzoStore;
    }
    EventQueue(final WigzoAppStore wigzoAppStores) {
        wigzoAppStore_ = wigzoAppStores;
    }

    /**
     * Returns the number of events in the local event queue.
     * @return the number of events in the local event queue
     */
    int size() {
        return wigzoStore_.events().length;
   }
    int mobilesize() {
        return wigzoAppStore_.events().length;
    }

    /**
     * Removes all current events from the local queue and returns them as a
     * URL-encoded JSON string that can be submitted to a ConnectionQueue.
     * @return URL-encoded JSON string of event data from the local event queue
     */
    String events() {
        String result;


        final List<Event> events = wigzoStore_.eventsList();


        final JSONArray eventArray = new JSONArray();
        final JSONArray mobileeventArray = new JSONArray();
        for (Event e : events) {
            eventArray.put(e.toJSON());
        }
        result = eventArray.toString();

        wigzoStore_.removeEvents(events);

        try {
            result = java.net.URLEncoder.encode(result, "UTF-8");

        } catch (UnsupportedEncodingException e) {
            // should never happen because Android guarantees UTF-8 support
        }

        return result;
    }
    String mobileEvents() {

        String mobileResult;

        final List<Event> mobileevent = wigzoAppStore_.eventsList();


        final JSONArray mobileeventArray = new JSONArray();

        for (Event e : mobileevent) {
            mobileeventArray.put(e.toJSON());
        }

        mobileResult = mobileevent.toString();

        wigzoAppStore_.removeEvents(mobileevent);

        try {
            mobileResult = java.net.URLEncoder.encode(mobileResult, "UTF-8");

        } catch (UnsupportedEncodingException e) {
            // should never happen because Android guarantees UTF-8 support
        }

        return mobileResult;
    }

    /**
     * Records a custom wigzo event to the local event queue.
     * @param key name of the custom event, required, must not be the empty string
     * @param segmentation segmentation values for the custom event, may be null
     * @param count count associated with the custom event, should be more than zero
     * @param sum sum associated with the custom event, if not used, pass zero.
     *            NaN and infinity values will be quietly ignored.
     * @throws IllegalArgumentException if key is null or empty
     */
    void recordEvent(final String key, final Map<String, String> segmentation, final int count, final double sum) {
        final int timestamp = Wigzo.currentTimestamp();
        final int hour = Wigzo.currentHour();
        final int dow = Wigzo.currentDayOfWeek();

        wigzoStore_.addEvent(key, segmentation, timestamp, hour, dow, count, sum);

    }

    void recordMobileEvent(final String key, final Map<String, String> segmentation, final int count, final double sum) {
        final int timestamp = Wigzo.currentTimestamp();
        final int hour = Wigzo.currentHour();
        final int dow = Wigzo.currentDayOfWeek();

        wigzoAppStore_.addEvent(key, segmentation, timestamp, hour, dow, count, sum);
    }

    // for unit tests
    WigzoStore getWigzoStore() {
        return wigzoStore_;
    }

    WigzoAppStore getWigzoAppStore_(){ return wigzoAppStore_; }
}
