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
import java.awt.event.WindowEvent;
import javax.swing.JDialog;

class ApiChangeConfirmationDialogHandler implements WindowHandler {

    private static final Logger logger = LoggerFactory.getLogger(ApiChangeConfirmationDialogHandler.class);

    @Override
    public boolean filterEvent(Window window, int eventId) {
        switch (eventId) {
            case WindowEvent.WINDOW_OPENED:
                if (ConfigDialogManager.configDialogManager().getApiConfigChangeConfirmationExpected()) return true;
                return false;
            default:
                return false;
        }
    }

    @Override
    public void handleWindow(Window window, int eventID) {
        ConfigDialogManager.configDialogManager().setApiConfigChangeConfirmationHandled();
        // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
        if (SwingUtils.clickButtonByBundle(window, "twslaunch.ji18n.LauncherLanguage", "yes")) {
        } else {
            logger.error("can't apply new API socket port: 'Yes' button not found");
        }
    }

    @Override
    public boolean recogniseWindow(Window window) {
        if (! (window instanceof JDialog)) return false;

        // [AST重构审查] 来源Jar: jars/total-2024.jar | 规则: 包含与模糊匹配 | 相似度: 44.7%
        return (SwingUtils.findLabelByBundle(window, "ji18n.Language", "Gnrl_Socket_Reset_Msg") != null);
    }

}
