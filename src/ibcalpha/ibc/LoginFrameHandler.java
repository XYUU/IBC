// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2018 Richard L King (rlking@aultan.com)
// For conditions of distribution and use, see copyright notice in COPYING.txt

// IBC is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.

// IBC is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.

// You should have received a copy of the GNU General Public License
// along with IBC.  If not, see <http://www.gnu.org/licenses/>.

package ibcalpha.ibc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Window;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

final class LoginFrameHandler extends AbstractLoginHandler {

    private static final Logger logger = LoggerFactory.getLogger(LoginFrameHandler.class);

    @Override
    public boolean recogniseWindow(Window window) {
        if (! (window instanceof JFrame)) return false;

        // we check for the presence of the Login button because 
        // TWS displays a different (information-only) dialog, also 
        // entitled Login, when it's trying to reconnect. (Not sure if this 
        // is still true)
        // Also when doing autorestart there is no login button.
        // [AST重构审查] 来源Jar: jars/jts4launch-1045.jar | 规则: 包含与模糊匹配 | 相似度: 40.0%
// [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 匹配开头 | 相似度: 60.0%
// [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
// [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
        return ((SwingUtils.titleEquals(window, "New Login") ||
                SwingUtils.titleEqualsByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Login")) &&
                (SwingUtils.findButtonByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Log_in") != null ||
                SwingUtils.findButtonByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Paper_Log_in") != null ||
                SessionManager.isRestart()));
    }
    
    private boolean listeningForUsernameChange;

    @Override
    protected final boolean initialise(final Window window, int eventID) throws IbcException {
        if (!setTradingMode(window)) return false;

        JtsIniManager.reload();     // because TWS/Gateway modify the jts.ini file before this point

        final JTextField userName = SwingUtils.findTextField(window, 0);
        if (userName == null) throw new IbcException("Username field");

        if (! listeningForUsernameChange) {
            listeningForUsernameChange = true;

            // Add a DocumentListener to the username field, which will set the 
            // "Use/store settings on server" checkbox as required. This is
            // necessary because when a valid username is entered, TWS sets the
            // checkbox according to the latest saved settings for that user, which
            // may not be what is now required by the StoreSettingsOnServer setting.
            userName.getDocument().addDocumentListener(new DocumentListener(){ 
                @Override
                public void insertUpdate(DocumentEvent de) {
                    setStoreSettingsOnServerCheckbox();
                }

                @Override
                public void removeUpdate(DocumentEvent de) {
                    //setStoreSettingsOnServerCheckbox();
                }

                @Override
                public void changedUpdate(DocumentEvent de) {
                    setStoreSettingsOnServerCheckbox();
                }

                private void setStoreSettingsOnServerCheckbox() {
                    if (Settings.settings().getString("StoreSettingsOnServer", "").length() != 0) {
                        // we defer setting the checkbox: if we do it inline, TWS's setting
                        // overwrites it
                        GuiDeferredExecutor.instance().execute(() -> {
                            boolean storeSettingsOnServer = Settings.settings().getBoolean("StoreSettingsOnServer", false);
                            // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
                            if (! SwingUtils.findAndSetCheckBoxSelectedByBundle(
                                    window,
                                    storeSettingsOnServer,
                                    "twslaunch.ji18n.LauncherLanguage", "Store_settings_on_server"
                                    )) {
                                // this is expected when autorestarting, so we just log the fact
                                logger.info("could not find control: 'Use/store settings on server' this is expected when restarting");
                            } else {
                                logger.info("Use/store settings on server selected: {}", storeSettingsOnServer);
                            }
                        });
                    }
                }

            });
        }
        return true;
    }

    @Override
    protected final boolean preLogin(final Window window, int eventID) throws IbcException {
        if (LoginManager.loginManager().IBAPIUserName().length() == 0) {
            setMissingCredential(window, 0);
        } else if (LoginManager.loginManager().IBAPIPassword().length() == 0) {
            setMissingCredential(window, 1);
        } else {
            return true;
        }
        return false;
    }

    @Override
    protected final boolean setFields(Window window, int eventID) throws IbcException {
        logger.info("Setting user name");
        setCredential(window, "IBAPI user name", 0, LoginManager.loginManager().IBAPIUserName());
        logger.info("Setting password");
        setCredential(window, "IBAPI password", 1, LoginManager.loginManager().IBAPIPassword());
        return true;
    }

    @Override
    protected boolean isUserIdDisabledOrAbsent(Window window) {
        JTextField userID = SwingUtils.findTextField(window, 0);
        if (userID == null) return true;
        return ! userID.isEnabled();
    }

    @Override
    protected boolean isPasswordDisabledOrAbsent(Window window) {
        JTextField password = SwingUtils.findTextField(window, 1);
        if (password == null) return true;
        return ! password.isEnabled();
    }

}

