package davmail.ui.tray;

import davmail.BundleMessage;
import davmail.DavGateway;
import davmail.Settings;
import davmail.ui.AboutFrame;
import davmail.ui.SettingsFrame;
import dbus.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.lf5.LF5Appender;
import org.apache.log4j.lf5.LogLevel;
import org.apache.log4j.lf5.viewer.LogBrokerMonitor;
import org.freedesktop.dbus.connections.impl.DBusConnection;
import org.freedesktop.dbus.exceptions.DBusException;
import org.freedesktop.dbus.interfaces.Properties;
import org.freedesktop.dbus.messages.Message;
import org.freedesktop.dbus.types.UInt32;
import org.freedesktop.dbus.types.Variant;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import static dbus.DBusMenu.DBusMenuItem;
import static dbus.DBusMenu.DBusMenuLayoutItem;
import static dbus.StatusNotifierItem.*;

/**
 * Tray icon handler based on SWT
 */
public class DBusSNIGatewayTray implements DavGatewayTrayInterface {
    private static final Logger LOGGER = Logger.getLogger(SwtGatewayTray.class);

    private SNI sni;

    DBusSNIGatewayTray() {
    }

    private static ArrayList<java.awt.Image> frameIcons;
    private static StatusNotifierItem.Pixmap iconPixmap;
    private static StatusNotifierItem.Pixmap iconActivePixmap;
    private static StatusNotifierItem.Pixmap iconInactivePixmap;

    private AboutFrame aboutFrame;
    private SettingsFrame settingsFrame;
    private LogBrokerMonitor logBrokerMonitor;

    private boolean isActive = true;

    /**
     * Return AWT Image icon for frame title.
     *
     * @return frame icon
     */
    @Override
    public java.util.List<java.awt.Image> getFrameIcons() {
        return frameIcons;
    }

    private StatusNotifierItem.Pixmap buildPixmap(BufferedImage image) {
        int[] pixels = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
        byte[] argb = new byte[pixels.length * 4];
        int offset = 0;
        for (int pixel: pixels) {
            argb[offset] = (byte)((pixel >>> 24) & 0xFF);
            argb[offset + 1] = (byte)((pixel >>> 16) & 0xFF);
            argb[offset + 2] = (byte)((pixel >>> 8) & 0xFF);
            argb[offset + 3] = (byte)(pixel & 0xFF);
            offset += 4;
        }
        return new StatusNotifierItem.Pixmap(image.getWidth(), image.getHeight(), argb);
    }

    private void setIcon(StatusNotifierItem.Pixmap pixmap) {
        try {
            sni.setIconPixmap(pixmap);
        } catch (Exception e) {
            LOGGER.error("Failed to change icon pixmap", e);
        }
    }

    /**
     * Switch tray icon between active and standby icon.
     */
    public void switchIcon() {
        isActive = true;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                StatusNotifierItem.Pixmap current = sni.getIconPixmap();
                if (current == iconActivePixmap) {
                    setIcon(iconPixmap);
                } else {
                    setIcon(iconActivePixmap);
                }
            }
        });
    }

    /**
     * Set tray icon to inactive (network down)
     */
    public void resetIcon() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setIcon(iconPixmap);
            }
        });
    }

    /**
     * Set tray icon to inactive (network down)
     */
    public void inactiveIcon() {
        isActive = false;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                setIcon(iconInactivePixmap);
            }
        });
    }

    /**
     * Check if current tray status is inactive (network down).
     *
     * @return true if inactive
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Log and display balloon message according to log level.
     *
     * @param message text message
     * @param level   log level
     */
    public void displayMessage(final String message, final Level level) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (sni == null) return;

                String icon = null;
                if (level.equals(Level.INFO)) {
                    icon = "dialog-information";
                } else if (level.equals(Level.WARN)) {
                    icon = "dialog-warning";
                } else if (level.equals(Level.ERROR)) {
                    icon = "dialog-error";
                }
                if (icon == null) return;
                sni.newNotification(BundleMessage.format("UI_DAVMAIL_GATEWAY"), message, icon);
            }
        });
    }

    /**
     * Create tray icon and register frame listeners.
     */
    public void init() {
        final Semaphore sem = new Semaphore(0);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    initDBus();
                } catch (Exception e) {
                    LOGGER.error("Cannot use DBus notification", e);
                } finally {
                    sem.release();
                }
            }
        });
        try {
            sem.tryAcquire(10, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            LOGGER.error("Error waiting for DBus init");
        }
        if (sni == null) {
            throw new Error("Cannot use DBus notification");
        }
    }

    private void initDBus() throws SNIException, DBusException {
        iconPixmap = buildPixmap(davmail.ui.tray.DavGatewayTray.adjustTrayIcon(davmail.ui.tray.DavGatewayTray.loadImage(AwtGatewayTray.TRAY_PNG)));
        iconActivePixmap = buildPixmap(davmail.ui.tray.DavGatewayTray.adjustTrayIcon(davmail.ui.tray.DavGatewayTray.loadImage(AwtGatewayTray.TRAY_ACTIVE_PNG)));
        iconInactivePixmap = buildPixmap(davmail.ui.tray.DavGatewayTray.adjustTrayIcon(davmail.ui.tray.DavGatewayTray.loadImage(AwtGatewayTray.TRAY_INACTIVE_PNG)));

        frameIcons = new ArrayList<java.awt.Image>();
        frameIcons.add(davmail.ui.tray.DavGatewayTray.loadImage(AwtGatewayTray.TRAY128_PNG));
        frameIcons.add(davmail.ui.tray.DavGatewayTray.loadImage(AwtGatewayTray.TRAY_PNG));

        sni = new SNI();
        sni.setIconPixmap(iconPixmap);
        sni.connect();
    }

    public void dispose() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                sni.disconnect();

                if (settingsFrame != null) {
                    settingsFrame.dispose();
                }
                if (aboutFrame != null) {
                    aboutFrame.dispose();
                }
                if (logBrokerMonitor != null) {
                    logBrokerMonitor.dispose();
                }
            }
        });
    }

    private void showAboutFrame() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (aboutFrame == null) {
                    aboutFrame = new AboutFrame();
                }
                aboutFrame.update();
                aboutFrame.setVisible(true);
                aboutFrame.toFront();
                aboutFrame.requestFocus();
            }
        });
    }

    private void showSettingsFrame() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (settingsFrame == null) {
                    settingsFrame = new SettingsFrame();
                }
                settingsFrame.reload();
                settingsFrame.setVisible(true);
                settingsFrame.toFront();
                settingsFrame.requestFocus();
            }
        });
    }

    private void showLogsFrame() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                Logger rootLogger = Logger.getRootLogger();
                LF5Appender lf5Appender = (LF5Appender) rootLogger.getAppender("LF5Appender");
                if (lf5Appender == null) {
                    logBrokerMonitor = new LogBrokerMonitor(LogLevel.getLog4JLevels()) {
                        @Override
                        protected void closeAfterConfirm() {
                            hide();
                        }
                    };
                    lf5Appender = new LF5Appender(logBrokerMonitor);
                    lf5Appender.setName("LF5Appender");
                    rootLogger.addAppender(lf5Appender);
                }
                lf5Appender.getLogBrokerMonitor().show();
            }
        });
    }

    public class SNI implements StatusNotifierItem.Interface, Properties {

        private DBusConnection dbusCnx = null;
        private String serviceName = newBusName();

        private Notifications.Interface notifier = null;

        private Map<String, Variant<?>> properties = new HashMap<String, Variant<?>>();

        SNI() {
            setProperty(PROP_ID, "davmail");
            setProperty(PROP_TITLE, "davmail");
            setProperty(PROP_CATEGORY, "ApplicationStatus");
            setProperty(PROP_ICON_NAME, "");
            setProperty(PROP_STATUS, "Active");
        }

        private <A> Variant<A> getProperty(String name) {
            return (Variant<A>) properties.get(name);
        }

        private <A> A getPropertyValue(String name) {
            Variant<A> prop = getProperty(name);
            if (prop == null) return null;
            return prop.getValue();
        }

        private <A> void setProperty(String name, Variant<A> value) {
            properties.put(name, value);
        }

        private <A> void setProperty(String name, A value) {
            properties.put(name, new Variant<A>(value));
        }

        void connect() throws SNIException {
            try {
                dbusCnx = DBusConnection.getConnection(DBusConnection.DBusBusType.SESSION);
                dbusCnx.requestBusName(serviceName);
                dbusCnx.exportObject(getObjectPath(), this);
                setProperty(PROP_MENU, DBusMenu.OBJECT_PATH);
                dbusCnx.exportObject(DBusMenu.OBJECT_PATH, new Menu());
                StatusNotifierWatcher.Interface watcher = dbusCnx.getRemoteObject(
                    StatusNotifierWatcher.INTERFACE_NAME,
                    StatusNotifierWatcher.OBJECT_PATH,
                    StatusNotifierWatcher.Interface.class
                );
                watcher.RegisterStatusNotifierItem(serviceName);
                notifier = dbusCnx.getRemoteObject(
                    Notifications.INTERFACE_NAME,
                    Notifications.OBJECT_PATH,
                    Notifications.Interface.class
                );
            } catch (Exception ex) {
                throw new SNIException("Failed to connect SNI to DBus", ex);
            }
        }

        void disconnect() {
            if (dbusCnx == null) return;
            DBusConnection cnx = dbusCnx;
            dbusCnx = null;
            cnx.disconnect();
        }

        void newNotification(String summary, String body, String icon) {
            if (notifier == null) return;

            Map<String, Variant<?>> hints = new HashMap<String, Variant<?>>();
            hints.put("transient", new Variant<Boolean>(true));
            notifier.Notify("davmail", new UInt32(0), icon, summary, body,
                new ArrayList<String>(), hints, -1).longValue();
        }

        Pixmap getIconPixmap() {
            Pixmap[] pixmaps = getPropertyValue(PROP_ICON_PIXMAP);
            if ((pixmaps == null) || (pixmaps.length == 0)) return null;
            return pixmaps[0];
        }

        void setIconPixmap(Pixmap value) throws DBusException, SNIException {
            Pixmap[] pixmaps = new Pixmap[1];
            pixmaps[0] = value;
            changeProperty(PROP_ICON_PIXMAP, pixmaps, new NewIcon(getObjectPath()));
        }

        private <A> void changeProperty(String name, Variant<A> value, Message msg) throws SNIException {
            Variant<A> current = getProperty(name);
            setProperty(name, value);
            if (dbusCnx == null) return;
            try {
                dbusCnx.sendMessage(msg);
            } catch (Exception ex) {
                setProperty(name, current);
                throw new SNIException("Could not set " + name + ": " + ex.getMessage(), ex);
            }
        }

        private <A> void changeProperty(String name, A value, Message msg) throws SNIException {
            changeProperty(name, new Variant<A>(value), msg);
        }

        @Override
        public void ContextMenu(int x, int y) {
            // Ignore
        }

        @Override
        public void Activate(int x, int y) {
            showSettingsFrame();
        }

        @Override
        public void SecondaryActivate(int x, int y) {
            showSettingsFrame();
        }

        @Override
        public void Scroll(int delta, String orientation) {
            // Ignore
        }

        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public String getObjectPath() {
            return StatusNotifierItem.OBJECT_PATH;
        }

        @Override
        public <A> A Get(String interfaceName, String propertyName) {
            return (A) getProperty(propertyName);
        }

        @Override
        public <A> void Set(String interfaceName, String propertyName, A value) {
            // We don't allow changing our values from DBus.
        }

        @Override
        public Map<String, Variant<?>> GetAll(String interfaceName) {
            return properties;
        }

    }

    public class Menu implements DBusMenu.Interface, Properties {

        private Map<Integer, DBusMenu.DBusMenuLayoutItem> layoutItems = new HashMap<Integer, DBusMenuLayoutItem>();

        private final static int MENU_ID_ABOUT = 1;
        private final static int MENU_ID_SETTINGS = 2;
        private final static int MENU_ID_SHOW_LOGS = 3;
        private final static int MENU_ID_EXIT = 4;

        Menu() {
            buildRootItem();

            // display settings frame on first start
            if (Settings.isFirstStart()) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        showSettingsFrame();
                    }
                });

            }
        }

        @Override
        public boolean isRemote() {
            return false;
        }

        @Override
        public String getObjectPath() {
            return DBusMenu.OBJECT_PATH;
        }

        private Map<String, Variant<?>> filterProperties(Map<String, Variant<?>> properties, List<String> propertyNames) {
            Map<String, Variant<?>> filtered = new HashMap<String, Variant<?>>();
            boolean all = propertyNames.isEmpty();
            for (String key: properties.keySet()) {
                if (all || propertyNames.contains(key)) filtered.put(key, properties.get(key));
            }
            return filtered;
        }

        private DBusMenu.DBusMenuLayoutItem buildAboutItem() {
            Map<String, Variant<?>> properties = new HashMap<String, Variant<?>>();
            properties.put("label", new Variant<String>(BundleMessage.format("UI_ABOUT")));
            DBusMenu.DBusMenuLayoutItem item = new DBusMenu.DBusMenuLayoutItem(
                MENU_ID_ABOUT,
                properties,
                new ArrayList<Variant<DBusMenuLayoutItem>>()
            );
            layoutItems.put(item.id, item);
            return item;
        }

        private DBusMenu.DBusMenuLayoutItem buildSettingsItem() {
            Map<String, Variant<?>> properties = new HashMap<String, Variant<?>>();
            properties.put("label", new Variant<String>(BundleMessage.format("UI_SETTINGS")));
            DBusMenu.DBusMenuLayoutItem item = new DBusMenu.DBusMenuLayoutItem(
                MENU_ID_SETTINGS,
                properties,
                new ArrayList<Variant<DBusMenuLayoutItem>>()
            );
            layoutItems.put(item.id, item);
            return item;
        }

        private DBusMenu.DBusMenuLayoutItem buildShowLogsItem() {
            Map<String, Variant<?>> properties = new HashMap<String, Variant<?>>();
            properties.put("label", new Variant<String>(BundleMessage.format("UI_SHOW_LOGS")));
            DBusMenu.DBusMenuLayoutItem item = new DBusMenu.DBusMenuLayoutItem(
                MENU_ID_SHOW_LOGS,
                properties,
                new ArrayList<Variant<DBusMenuLayoutItem>>()
            );
            layoutItems.put(item.id, item);
            return item;
        }

        private DBusMenu.DBusMenuLayoutItem buildExitItem() {
            Map<String, Variant<?>> properties = new HashMap<String, Variant<?>>();
            properties.put("label", new Variant<String>(BundleMessage.format("UI_EXIT")));
            DBusMenu.DBusMenuLayoutItem item = new DBusMenu.DBusMenuLayoutItem(
                MENU_ID_EXIT,
                properties,
                new ArrayList<Variant<DBusMenuLayoutItem>>()
            );
            layoutItems.put(item.id, item);
            return item;
        }

        private void buildRootItem() {
            Map<String, Variant<?>> properties = new HashMap<String, Variant<?>>();
            properties.put("children-display", new Variant<String>("submenu"));
            List<Variant<DBusMenuLayoutItem>> children = new ArrayList<Variant<DBusMenuLayoutItem>>();
            children.add(new Variant<DBusMenuLayoutItem>(buildAboutItem()));
            children.add(new Variant<DBusMenuLayoutItem>(buildSettingsItem()));
            children.add(new Variant<DBusMenuLayoutItem>(buildShowLogsItem()));
            children.add(new Variant<DBusMenuLayoutItem>(buildExitItem()));
            DBusMenu.DBusMenuLayoutItem item = new DBusMenu.DBusMenuLayoutItem(
                0,
                properties,
                children
            );
            layoutItems.put(item.id, item);
        }

        private DBusMenu.DBusMenuLayoutItem getLayoutItem(int id, int recursionDepth, List<String> propertyNames) {
            DBusMenu.DBusMenuLayoutItem item = layoutItems.get(id);
            if (item == null) return null;
            List<Variant<DBusMenu.DBusMenuLayoutItem>> children = new ArrayList<Variant<DBusMenuLayoutItem>>();
            if (recursionDepth != 0) {
                for (Variant<DBusMenu.DBusMenuLayoutItem> child : item.children) {
                    DBusMenu.DBusMenuLayoutItem r = getLayoutItem(child.getValue().id, recursionDepth - 1, propertyNames);
                    children.add(new Variant<DBusMenu.DBusMenuLayoutItem>(r));
                }
            }
            return new DBusMenu.DBusMenuLayoutItem(
                item.id,
                filterProperties(item.properties, propertyNames),
                children
            );
        }

        private DBusMenu.DBusMenuItem getItem(int id, List<String> propertyNames) {
            DBusMenu.DBusMenuLayoutItem item = layoutItems.get(id);
            if (item == null) return null;
            return new DBusMenu.DBusMenuItem(
                item.id,
                filterProperties(item.properties, propertyNames)
            );
        }

        @Override
        public DBusMenu.GetLayoutResponse<UInt32, DBusMenu.DBusMenuLayoutItem> GetLayout(int parentId, int recursionDepth, List<String> propertyNames) {
            return new DBusMenu.GetLayoutResponse<UInt32, DBusMenu.DBusMenuLayoutItem>(
                new UInt32(0),
                getLayoutItem(parentId, recursionDepth, propertyNames)
            );
        }

        @Override
        public List<DBusMenu.DBusMenuItem> GetGroupProperties(List<Integer> ids, List<String> propertyNames) {
            List<DBusMenu.DBusMenuItem> items = new ArrayList<DBusMenuItem>();
            for (int id: ids) {
                items.add(getItem(id, propertyNames));
            }
            return items;
        }

        @Override
        public void Event(int id, String eventId, Variant<?> data, UInt32 timestamp) {
            if (!"clicked".equalsIgnoreCase(eventId)) return;
            switch (id) {
                case MENU_ID_ABOUT:
                    showAboutFrame();
                    break;

                case MENU_ID_SETTINGS:
                    showSettingsFrame();
                    break;

                case MENU_ID_SHOW_LOGS:
                    showLogsFrame();
                    break;

                case MENU_ID_EXIT:
                    try {
                        DavGateway.stop();
                    } catch (Exception exc) {
                        DavGatewayTray.error(exc);
                    }
                    // make sure we do exit
                    System.exit(0);
                    break;

                default:
                    break;
            }
        }

        @Override
        public boolean AboutToShow(int id) {
            // Menu is not dynamic, so there is nothing to update.
            return false;
        }

        @Override
        public <A> A Get(String interfaceName, String propertyName) {
            return null;
        }

        @Override
        public <A> void Set(String interfaceName, String propertyName, A value) {
        }

        @Override
        public Map<String, Variant<?>> GetAll(String interfaceName) {
            return new HashMap<String, Variant<?>>();
        }

    }

}
