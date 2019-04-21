package dbus;

import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.interfaces.DBusInterface;

/**
 * StatusNotifierWatcher
 *
 * See: https://www.freedesktop.org/wiki/Specifications/StatusNotifierItem/StatusNotifierWatcher/
 * The actual package/service name is 'org.kde' instead of 'org.freedesktop'.
 * We only care about registration.
 */
public class StatusNotifierWatcher {

    public final static String INTERFACE_NAME = "org.kde.StatusNotifierWatcher";
    public final static String OBJECT_PATH = "/StatusNotifierWatcher";

    @DBusInterfaceName(INTERFACE_NAME)
    public interface Interface extends DBusInterface {
        /** Registers a StatusNotifierItem into the StatusNotifierWatcher. */
        void RegisterStatusNotifierItem(String service);
    }

}
