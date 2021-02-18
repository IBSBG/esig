/**
 * © Nowina Solutions, 2015-2017
 * <p>
 * Concédée sous licence EUPL, version 1.1 ou – dès leur approbation par la Commission européenne - versions ultérieures de l’EUPL (la «Licence»).
 * Vous ne pouvez utiliser la présente œuvre que conformément à la Licence.
 * Vous pouvez obtenir une copie de la Licence à l’adresse suivante:
 * <p>
 * http://ec.europa.eu/idabc/eupl5
 * <p>
 * Sauf obligation légale ou contractuelle écrite, le logiciel distribué sous la Licence est distribué «en l’état»,
 * SANS GARANTIES OU CONDITIONS QUELLES QU’ELLES SOIENT, expresses ou implicites.
 * Consultez la Licence pour les autorisations et les restrictions linguistiques spécifiques relevant de la Licence.
 */
package lu.nowina.nexu;

import javafx.application.Platform;
import lu.nowina.nexu.api.NexuAPI;
import lu.nowina.nexu.api.SystrayMenuItem;
import lu.nowina.nexu.api.flow.FutureOperationInvocation;
import lu.nowina.nexu.api.flow.OperationFactory;
import lu.nowina.nexu.api.flow.OperationResult;
import lu.nowina.nexu.systray.SystrayMenuInitializer;
import lu.nowina.nexu.view.core.NonBlockingUIOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.net.URL;
import java.util.List;
import java.util.*;

public class SystrayMenu {

    private static final Logger LOGGER = LoggerFactory.getLogger(SystrayMenu.class.getName());

    final static Locale localeBG = new Locale("bg", "BG");

    public static Locale currentLocale = Locale.ENGLISH;
    public static ResourceBundle resourceBundleEN;
    public static ResourceBundle resourceBundleBG;
    public static PopupMenu popupMenu = new PopupMenu();

    public SystrayMenu(OperationFactory operationFactory, NexuAPI api, UserPreferences prefs) {

        final List<SystrayMenuItem> extensionSystrayMenuItems = api.getExtensionSystrayMenuItems();
        final SystrayMenuItem[] systrayMenuItems = new SystrayMenuItem[extensionSystrayMenuItems.size() + 2];

        resourceBundleEN = ResourceBundle.getBundle("bundles/nexu", Locale.ENGLISH);
        resourceBundleBG = ResourceBundle.getBundle("bundles/nexu", localeBG);

        systrayMenuItems[0] = createAboutSystrayMenuItem(operationFactory, api, resourceBundleEN);
        systrayMenuItems[1] = createPreferencesSystrayMenuItem(operationFactory, api, prefs, resourceBundleEN);

        int i = 2;
        for (final SystrayMenuItem systrayMenuItem : extensionSystrayMenuItems) {
            systrayMenuItems[i++] = systrayMenuItem;
        }

        Menu langMenu = new Menu(resourceBundleEN.getString("systray.menu.language"));
        MenuItem bgLangMenuItem = new MenuItem(resourceBundleEN.getString("systray.menu.language.bg.item"));
        MenuItem engLangMenuItem = new MenuItem(resourceBundleEN.getString("systray.menu.language.en.item"));
        bgLangMenuItem.addActionListener(e -> changeLang(localeBG));
        engLangMenuItem.addActionListener(e -> changeLang(Locale.ENGLISH));
        langMenu.add(bgLangMenuItem);
        langMenu.add(engLangMenuItem);

        final SystrayMenuItem exitMenuItem = createExitSystrayMenuItem(resourceBundleEN);

        final String tooltip = api.getAppConfig().getApplicationName();
        final URL trayIconURL = this.getClass().getResource("/tray-icon.png");
        try {
            switch (api.getEnvironmentInfo().getOs()) {
                case WINDOWS:
                case MACOSX:
                    // Use reflection to avoid wrong initialization issues
                    Class.forName("lu.nowina.nexu.systray.AWTSystrayMenuInitializer")
                            .asSubclass(SystrayMenuInitializer.class).newInstance()
                            .init(popupMenu, tooltip, trayIconURL, operationFactory, exitMenuItem, langMenu, systrayMenuItems);
                    break;
                case LINUX:
                    // Use reflection to avoid wrong initialization issues
                    Class.forName("lu.nowina.nexu.systray.DorkboxSystrayMenuInitializer")
                            .asSubclass(SystrayMenuInitializer.class).newInstance()
                            .init(popupMenu, tooltip, trayIconURL, operationFactory, exitMenuItem, langMenu, systrayMenuItems);
                    break;
                case NOT_RECOGNIZED:
                    LOGGER.warn("System tray is currently not supported for NOT_RECOGNIZED OS.");
                    break;
                default:
                    throw new IllegalArgumentException("Unhandled value: " + api.getEnvironmentInfo().getOs());
            }
        } catch (InstantiationException e) {
            LOGGER.error("Cannot initialize systray menu", e);
        } catch (IllegalAccessException e) {
            LOGGER.error("Cannot initialize systray menu", e);
        } catch (ClassNotFoundException e) {
            LOGGER.error("Cannot initialize systray menu", e);
        }
    }

    private void changeLang(Locale targetLocale) {

        ResourceBundle refResourceBundle = resourceBundleEN;
        if(currentLocale.equals(SystrayMenu.localeBG)) {
            refResourceBundle = resourceBundleBG;
        }

        ResourceBundle targetResourceBundle = resourceBundleEN;
        if(targetLocale.equals(SystrayMenu.localeBG)) {
            targetResourceBundle = resourceBundleBG;
        }

        Enumeration<String> enumerationRef = refResourceBundle.getKeys();
        Hashtable <String, String> refHashtable = new Hashtable<>(10);
        String key;
        while (enumerationRef.hasMoreElements()) {
            key = enumerationRef.nextElement();
            refHashtable.put(refResourceBundle.getString(key), key);
        }

        String targetKey;
        for (int i = 0; i < popupMenu.getItemCount(); i++) {
            MenuItem menuItem = popupMenu.getItem(i);
            targetKey = refHashtable.get(menuItem.getLabel());
            if(targetKey != null) {
                menuItem.setLabel(targetResourceBundle.getString(targetKey));
            }
        }

        currentLocale = targetLocale;
        LOGGER.info("!!!!!!!!!!!!!!!!! " + targetLocale.getLanguage());
    }

    private SystrayMenuItem createAboutSystrayMenuItem(final OperationFactory operationFactory, final NexuAPI api,
                                                       final ResourceBundle resources) {
        return new SystrayMenuItem() {
            @Override
            public String getLabel() {
                return resources.getString("systray.menu.about");
            }

            @Override
            public FutureOperationInvocation<Void> getFutureOperationInvocation() {
                return new FutureOperationInvocation<Void>() {
                    @Override
                    public OperationResult<Void> call(OperationFactory operationFactory) {
                        return operationFactory.getOperation(NonBlockingUIOperation.class, "/fxml/about.fxml",
                                api.getAppConfig().getApplicationName(), api.getAppConfig().getApplicationVersion(),
                                resources).perform();
                    }
                };
            }
        };
    }

    private SystrayMenuItem createPreferencesSystrayMenuItem(final OperationFactory operationFactory,
                                                             final NexuAPI api, final UserPreferences prefs, final ResourceBundle resources) {
        return new SystrayMenuItem() {
            @Override
            public String getLabel() {
                return resources.getString("systray.menu.preferences");
            }

            @Override
            public FutureOperationInvocation<Void> getFutureOperationInvocation() {
                return new FutureOperationInvocation<Void>() {
                    @Override
                    public OperationResult<Void> call(OperationFactory operationFactory) {
                        final ProxyConfigurer proxyConfigurer = new ProxyConfigurer(api.getAppConfig(), prefs);

                        return operationFactory.getOperation(NonBlockingUIOperation.class, "/fxml/preferences.fxml",
                                proxyConfigurer, prefs, !api.getAppConfig().isUserPreferencesEditable()).perform();
                    }
                };
            }
        };
    }

    private SystrayMenuItem createExitSystrayMenuItem(final ResourceBundle resources) {
        return new SystrayMenuItem() {
            @Override
            public String getLabel() {
                return resources.getString("systray.menu.exit");
            }

            @Override
            public FutureOperationInvocation<Void> getFutureOperationInvocation() {
                return new FutureOperationInvocation<Void>() {
                    @Override
                    public OperationResult<Void> call(OperationFactory operationFactory) {
                        LOGGER.info("Exiting...");
                        Platform.exit();
                        return new OperationResult<Void>((Void) null);
                    }
                };
            }
        };
    }
}
