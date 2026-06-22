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
import javax.swing.JToggleButton;

final class GatewayLoginFrameHandler extends AbstractLoginHandler {

    private static final Logger logger = LoggerFactory.getLogger(GatewayLoginFrameHandler.class);

    @Override
    public boolean recogniseWindow(Window window) {
        if (! (window instanceof JFrame)) return false;

        // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 包含与模糊匹配 | 相似度: 58.3%
// [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 包含与模糊匹配 | 相似度: 40.0%
// [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 包含与模糊匹配 | 相似度: 70.0%
// [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 匹配开头 | 相似度: 60.0%
// [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
// [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
        return ((SwingUtils.titleContains(window, "IBKR Gateway")  ||
                    SwingUtils.titleContains(window, "IB Gateway") ||
                    SwingUtils.titleContains(window, "Interactive Brokers Gateway")) &&
               (SwingUtils.findButtonByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Login") != null ||
                SwingUtils.findButtonByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Log_in") != null ||          // TWS 974+
                SwingUtils.findButtonByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Paper_Log_in") != null));    // TWS 974+
    }

    @Override
    protected final boolean initialise(final Window window, int eventID) throws IbcException {
        selectGatewayMode(window);
        // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
        if (SwingUtils.findLabelByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Trading_Mode") != null)  {
            setTradingMode(window);
        }
        return true;
    }

    @Override
    protected final boolean preLogin(final Window window, int eventID) throws IbcException {
        boolean result;
        if (SessionManager.isFIX()) {
            result = setMissingFIXCredentials(window);
        } else {
            result = setMissingIBAPICredentials(window);
        }
        return result;
    }

    private boolean setMissingFIXCredentials(Window window) {
        boolean result = false;
        if (LoginManager.loginManager().FIXUserName().length() == 0) {
            setMissingCredential(window, 0);
        } else if (LoginManager.loginManager().FIXPassword().length() == 0) {
            setMissingCredential(window, 1);
        } else if (LoginManager.loginManager().IBAPIUserName().length() != 0 || LoginManager.loginManager().IBAPIPassword().length() != 0) {
            if (LoginManager.loginManager().IBAPIUserName().length() == 0) {
                setMissingCredential(window, 3);
            } else if (LoginManager.loginManager().IBAPIPassword().length() == 0) {
                setMissingCredential(window, 4);
            } else {
                result = true;
            }
        } else {
            result = true;
        }
        return result;
    }

    private boolean setMissingIBAPICredentials(Window window) {
        boolean result = false;
        if (LoginManager.loginManager().IBAPIUserName().length() == 0) {
            setMissingCredential(window, 0);
        } else if (LoginManager.loginManager().IBAPIPassword().length() == 0) {
            setMissingCredential(window, 1);
        } else {
            result = true;
        }
        return result;
    }

    @Override
    protected final boolean setFields(Window window, int eventID) throws IbcException {
        if (SessionManager.isFIX()) {
            logger.info("Setting FIX user name");
            setCredential(window, "FIX user name", 0, LoginManager.loginManager().FIXUserName());
            logger.info("Setting FIX password");
            setCredential(window, "FIX password", 1, LoginManager.loginManager().FIXPassword());
            logger.info("Setting API user name");
            setCredential(window, "IBAPI user name", 2, LoginManager.loginManager().IBAPIUserName());
            logger.info("Setting API password");
            setCredential(window, "IBAPI password", 3, LoginManager.loginManager().IBAPIPassword());
        } else {
            logger.info("Setting user name");
            setCredential(window, "IBAPI user name", 0, LoginManager.loginManager().IBAPIUserName());
            logger.info("Setting password");
            setCredential(window, "IBAPI password", 1, LoginManager.loginManager().IBAPIPassword());
        }
        return true;
    }

    private void selectGatewayMode(Window window) throws IbcException {
        if (SessionManager.isFIX()) {
            switchToFIX(window);
        } else {
            switchToIBAPI(window);
        }
    }

    private void switchToFIX(Window window) throws IbcException {
        // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
        JToggleButton button = SwingUtils.findToggleButtonByBundle(window, "twslaunch.ji18n.LauncherLanguage", "FIX");
        if (button == null) throw new IbcException("FIX CTCI selector");

        if (! button.isSelected()) {
            logger.info("Clicking FIX CTCI selector");
            button.doClick();
        }
    }

    private void switchToIBAPI(Window window) throws IbcException {
        // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
        JToggleButton button = SwingUtils.findToggleButtonByBundle(window, "twslaunch.ji18n.LauncherLanguage", "TWS_API");
        if (button == null) // [AST重构审查] 来源Jar: jars/jts4launch-1045.jar | 规则: 包含与模糊匹配 | 相似度: 42.9%
            button = SwingUtils.findToggleButton(window, "TWS/API") ;
        if (button == null) throw new IbcException("IB API selector");

        if (! button.isSelected()) {
            logger.info("Clicking IB API selector");
            button.doClick();
        }
    }

    @Override
    protected boolean isUserIdDisabledOrAbsent(Window window) {
        int index = SessionManager.isFIX() ? 2 : 0;
        JTextField userID = SwingUtils.findTextField(window, index);
        if (userID == null) return true;
        return ! userID.isEnabled();
    }

    @Override
    protected boolean isPasswordDisabledOrAbsent(Window window) {
        int index = SessionManager.isFIX() ? 3 : 1;
        JTextField password = SwingUtils.findTextField(window, index);
        if (password == null) return true;
        return ! password.isEnabled();
    }

}
