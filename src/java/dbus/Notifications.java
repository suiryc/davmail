package dbus;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

import java.util.List;
import java.util.Map;

/**
 * DBus Notifications.
 *
 * Specs: https://developer.gnome.org/notification-spec/
 * Other: https://people.gnome.org/~mccann/docs/notification-spec/notification-spec-latest.html
 * We only care about sending notifications.
 */
public class Notifications {

    public final static String INTERFACE_NAME = "org.freedesktop.Notifications";
    public final static String OBJECT_PATH = "/org/freedesktop/Notifications";

    @DBusInterfaceName(INTERFACE_NAME)
    public interface Interface extends DBusInterface {
        /** Sends a notification to display. */
        UInt32 Notify(String appName, UInt32 replacesId, String appIcon, String summary, String body,
                      List<String> actions, Map<String, Variant<?>> hints, int expireTimeout);
    }

}
