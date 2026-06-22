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
import javax.swing.JFrame;

public class NSEComplianceFrameHandler  implements WindowHandler {

    private static final Logger logger = LoggerFactory.getLogger(NSEComplianceFrameHandler.class);
    public boolean filterEvent(Window window, int eventId) {
        switch (eventId) {
            case WindowEvent.WINDOW_OPENED:
            case WindowEvent.WINDOW_ACTIVATED:
                return true;
            default:
                return false;
        }
    }

    public void handleWindow(Window window, int eventID) {
        if (! Settings.settings().getBoolean("DismissNSEComplianceNotice", true)) return;
        window.setVisible(false);
        window.dispose();
        logger.info("NSE Compliance Dialog disposed");
    }

    public boolean recogniseWindow(Window window) {
        if (! (window instanceof JFrame)) return false;

        // [AST重构审查] 来源Jar: jars/jts4launch-1045.jar | 规则: 包含与模糊匹配 | 相似度: 42.9%
        return (SwingUtils.titleContainsByBundle(window, "ji18n.Language", "Startup_NSE_Compliance"));
    }
}
