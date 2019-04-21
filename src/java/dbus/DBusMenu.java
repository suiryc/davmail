package dbus;


import org.freedesktop.dbus.Struct;
import org.freedesktop.dbus.Tuple;
import org.freedesktop.dbus.annotations.DBusInterfaceName;
import org.freedesktop.dbus.annotations.Position;
import org.freedesktop.dbus.interfaces.DBusInterface;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

import java.util.List;
import java.util.Map;

/**
 * DBus StatusNotifierItem menu.
 *
 * Specs example: https://github.com/gnustep/libs-dbuskit/blob/master/Bundles/DBusMenu/com.canonical.dbusmenu.xml
 */
public class DBusMenu {

    public final static String OBJECT_PATH = "/MenuBar";

    @DBusInterfaceName("com.canonical.dbusmenu")
    public interface Interface extends DBusInterface {
        // Methods
        GetLayoutResponse<UInt32, DBusMenuLayoutItem> GetLayout(int parentId, int recursionDepth, List<String> propertyNames);
        List<DBusMenuItem> GetGroupProperties(List<Integer> ids, List<String> propertyNames);
        void Event(int id, String eventId, Variant<?> data, UInt32 timestamp);
        boolean AboutToShow(int id);

        // We don't care about signals.
    }

    public static class GetLayoutResponse<A, B> extends Tuple {

        @Position(0) public final A revision;
        @Position(1) public final B layout;

        public GetLayoutResponse(A revision, B layout) {
            this.revision = revision;
            this.layout = layout;
        }

    }

    public static class DBusMenuLayoutItem extends Struct {

        @Position(0) public final int id;
        @Position(1) public final Map<String, Variant<?>> properties;
        @Position(2) public final List<Variant<DBusMenuLayoutItem>> children;

        public DBusMenuLayoutItem(int id, Map<String, Variant<?>> properties, List<Variant<DBusMenuLayoutItem>> children) {
            this.id = id;
            this.properties = properties;
            this.children = children;
        }

    }

    public static class DBusMenuItem extends Struct {

        @Position(0) public final int id;
        @Position(1) public final Map<String, Variant<?>> properties;

        public DBusMenuItem(int id, Map<String, Variant<?>> properties) {
            this.id = id;
            this.properties = properties;
        }

    }

}
