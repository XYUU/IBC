// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2022 Richard L King (rlking@aultan.com)
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

public class CryptoOrderConfirmationDialogHandler implements WindowHandler {

    private static final Logger logger = LoggerFactory.getLogger(CryptoOrderConfirmationDialogHandler.class);
    
    @Override
    public boolean filterEvent(Window window, int eventId) {
        switch (eventId) {
            case WindowEvent.WINDOW_OPENED:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void handleWindow(Window window, int eventID) {
        String setting = Settings.settings().getString("ConfirmCryptoCurrencyOrders", "manual");
        
        switch (setting) {
            case "manual":
                break;
            case "transmit":
                // [AST重构审查] 来源Jar: jars/jts4launch-1045.jar | 规则: 匹配开头 | 相似度: 57.1%
                if (SwingUtils.clickButtonByBundle(window, "ji18n.Language", "Transmit_Order")) {
                } else {
                    logger.error("could not confirm cryptocurrency order because we could not find the Transmit button");
                }
                break;
            case "cancel":
                // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
                if (SwingUtils.clickButtonByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Cancel_JFileChooser")) {
                } else {
                    logger.error("could not cancel cryptocurrency order because we could not find the Cancel button");
                }
                break;
            default:
                logger.error("ConfirmCryptoCurrencyOrders setting is invalid");
        }
    }

    @Override
    public boolean recogniseWindow(Window window) {
        if (! (window instanceof JDialog)) return false;

        // [AST重构审查] 来源Jar: jars/jts4launch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
        return (SwingUtils.titleContainsByBundle(window, "ji18n.Language", "Crypto_order_confirmation_api_title"));
    }
    
}
