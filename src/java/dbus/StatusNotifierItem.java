package dbus;

import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.messages.DBusSignal;

import java.util.concurrent.atomic.AtomicLong;

/**
 * StatusNotifierItem
 *
 * See: https://www.freedesktop.org/wiki/Specifications/StatusNotifierItem/StatusNotifierItem/
 * The actual package/service name is 'org.kde' instead of 'org.freedesktop'.
 */
public class StatusNotifierItem {

    public final static String INTERFACE_NAME = "org.kde.StatusNotifierItem";
    public final static String OBJECT_PATH = "/StatusNotifierItem";

    private static AtomicLong nextId = new AtomicLong(0);

    public static long getPID() {
        String processName = java.lang.management.ManagementFactory.getRuntimeMXBean().getName();
        return Long.parseLong(processName.split("@")[0]);
    }

    // What matters is to get a unique name for each instance.
    public static String newBusName() {
        return "davmail.dbus.StatusNotifierItem-" + getPID() + "-" + nextId.getAndIncrement();
    }

    // Properties
    /** Describes the category of this item. */
    public static String PROP_CATEGORY = "Category";
    /**
     * Name unique for this application and consistent between sessions,
     * such as the application name itself.
     */
    public static String PROP_ID = "Id";
    /** Name that describes the application. */
    public static String PROP_TITLE = "Title";
    /** Status of this item or of the associated application. */
    public static String PROP_STATUS = "Status";
    /** Icon name: known/registered on system, or absolute path to file. */
    public static String PROP_ICON_NAME = "IconName";
    /** Icon pixmap (ARGB). */
    public static String PROP_ICON_PIXMAP = "IconPixmap";
    /** Object which should implement the menu interface. */
    public static String PROP_MENU = "Menu";

    @DBusInterfaceName(INTERFACE_NAME)
    public interface Interface extends DBusInterface {
        // Methods
        /** Asks the status notifier item to show a context menu. */
        void ContextMenu(int x, int y);
        /** Asks the status notifier item for activation (e.g. mouse left click). */
        void Activate(int x, int y);
        /**
         * Secondary and less important form of activation compared to Activate (e.g.
         * mouse middle click).
         */
        void SecondaryActivate(int x, int y);
        /** User asked for a scroll action. */
        void Scroll(int delta, String orientation);

        // Signals: we only need to change icon.
        /** Item has a new icon. */
        class NewIcon extends DBusSignal {
            public NewIcon(String path) throws DBusException { super(path); }
        }
    }

    public static class Pixmap extends Struct {

        @Position(0) public final int width;
        @Position(1) public final int height;
        @Position(2) public final byte[] argb;

        public Pixmap(int width, int height, byte[] argb) {
            this.width = width;
            this.height = height;
            this.argb = argb;
        }

    }

}
