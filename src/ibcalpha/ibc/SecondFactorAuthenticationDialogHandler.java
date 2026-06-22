// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2021 Richard L King (rlking@aultan.com)
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
import java.awt.event.WindowEvent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.ListModel;

public class SecondFactorAuthenticationDialogHandler implements WindowHandler {

    private static final Logger logger = LoggerFactory.getLogger(SecondFactorAuthenticationDialogHandler.class);
    private SecondFactorAuthenticationDialogHandler() {
    }

    ;

    static SecondFactorAuthenticationDialogHandler _secondFactorAuthenticationDialogHandler = new SecondFactorAuthenticationDialogHandler();

    static SecondFactorAuthenticationDialogHandler getInstance() {
        return _secondFactorAuthenticationDialogHandler;
    }

    @Override
    public boolean filterEvent(Window window, int eventId) {
        switch (eventId) {
            case WindowEvent.WINDOW_OPENED:
                return true;
            case WindowEvent.WINDOW_CLOSED:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void handleWindow(Window window, int eventID) {
        if (eventID == WindowEvent.WINDOW_OPENED) {
//            logger.info("Second Factor Authentication dialog opened. JLabel texts:{}{}", SwingUtils.NEWLINE, SwingUtils.getLabelTexts(window));
            if (LoginManager.loginManager().readonlyLoginRequired()) {
                doReadonlyLogin(window);
            } else if (secondFactorDeviceSelectionRequired(window)) {
                selectSecondFactorDevice(window);
            } else {
                LoginManager.loginManager().setLoginState(LoginManager.LoginState.TWO_FA_IN_PROGRESS);
            }
        } else if (eventID == WindowEvent.WINDOW_CLOSED) {
            if (LoginManager.loginManager().readonlyLoginRequired()) {
                LoginManager.loginManager().setLoginState(LoginManager.LoginState.LOGGED_IN);
                return;
            }
            LoginManager.loginManager().secondFactorAuthenticationDialogClosed();
        }
    }

    @Override
    public boolean recogniseWindow(Window window) {
        // For TWS this window is a JFrame; for Gateway it is a JDialog
        if (!(window instanceof JDialog || window instanceof JFrame)) return false;

        // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
// [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
        return SwingUtils.titleContainsByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Second_Factor_Auth") ||
                SwingUtils.titleContainsByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Passcode_Card_Title");
    }

    private void doReadonlyLogin(Window window) {
        // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
        if (SwingUtils.clickButtonByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Enter_Read_Only")) {
            logger.info("initiating read-only login.");
        } else {
            logger.error("could not initiate read-only login.");
        }
    }

    private boolean secondFactorDeviceSelectionRequired(Window window) {
        // this area appears in the Second Factor Authentication dialog when the
        // user has enabled more than one second factor authentication method

        // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
        return (SwingUtils.findTextAreaByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Select_second_factor_device") != null);
    }

    private void selectSecondFactorDevice(Window window) {
        JList<?> deviceList = SwingUtils.findList(window, 0);
        if (deviceList == null) {
            logger.error("could not find second factor device list.");
            IbcExit.exit(ErrorCodes.CANT_FIND_CONTROL);
            return;
        }

        String secondFactorDevice = Settings.settings().getString("SecondFactorDevice", "");
        if (secondFactorDevice.length() == 0) {
            logger.error("You should specify the required second factor device using the SecondFactorDevice setting in config.ini");
            return;
        }

        ListModel<?> model = deviceList.getModel();
        for (int i = 0; i < model.getSize(); i++) {
            String entry = model.getElementAt(i).toString().trim();
            if (entry.equals(secondFactorDevice)) {
                deviceList.setSelectedIndex(i);

                // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
                if (!SwingUtils.clickButtonByBundle(window, "twslaunch.ji18n.LauncherLanguage", "OK")) {
                    logger.error("could not select second factor device: OK button not found");
                }
                return;
            }
        }
        logger.error("could not find second factor device '{}' in the list", secondFactorDevice);
    }

}
