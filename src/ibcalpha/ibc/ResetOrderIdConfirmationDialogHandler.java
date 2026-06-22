// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2023 Richard L King (rlking@aultan.com)
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

public class ResetOrderIdConfirmationDialogHandler implements WindowHandler {

    private static final Logger logger = LoggerFactory.getLogger(ResetOrderIdConfirmationDialogHandler.class);
    
    static volatile boolean orderIdResetRequestedAtStart;

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
        final String CONFIRM_RESET = "confirm";
        final String REJECT_RESET = "reject";
        final String IGNORE_DIALOG = "ignore";
        final String INVALID_SETTING = "invalid";
        
        String confirmSetting=Settings.settings().getString("ConfirmOrderIdReset", "");
        
        String[] confirmSettings = confirmSetting.split("/");
        String confirmAtStart=confirmSettings[0];
        String confirmUserInitiated=confirmSettings[1];
        
        String action;
        if (orderIdResetRequestedAtStart) {
            logger.info("Order id reset requested at startup");
            switch (confirmAtStart) {
                case CONFIRM_RESET:
                case REJECT_RESET:
                case IGNORE_DIALOG:
                    action = confirmAtStart;
                    break;
                default:
                    action = INVALID_SETTING;
            }
            orderIdResetRequestedAtStart = false;
        } else {
            logger.info("Order id reset requested by user");
            switch (confirmUserInitiated) {
                case CONFIRM_RESET:
                case REJECT_RESET:
                case IGNORE_DIALOG:
                    action = confirmUserInitiated;
                    break;
                default:
                    action = INVALID_SETTING;
            }
        }
        
        try {
            switch (action) {
                case INVALID_SETTING:
                    throw new IbcException("Invalid setting ConfirmOrderIdReset=" + confirmSetting);
                case CONFIRM_RESET:
                    logger.info("Accepting order id reset");
                    // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
                    if (!SwingUtils.clickButtonByBundle(window, "twslaunch.ji18n.LauncherLanguage", "yes")) throw new IbcException("Can't find 'Yes' button");
                    break;
                case REJECT_RESET:
                    logger.info("Rejecting order id reset");
                    // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
                    if (!SwingUtils.clickButtonByBundle(window, "twslaunch.ji18n.LauncherLanguage", "no")) throw new IbcException("Can't find 'Yes' button");
                    break;
                case IGNORE_DIALOG:
                    logger.info("User must handle order id reset confirmation dialog");
            }
        } catch (IbcException e) {
            logger.error("An exception has occurred", e);
        }
    }

    @Override
    public boolean recogniseWindow(Window window) {
        if (! (window instanceof JDialog)) return false;

        // [AST重构审查] 来源Jar: jars/jts4launch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
        return (SwingUtils.findLabelByBundle(window, "ji18n.Language", "Confirm_reset_API_order_ids_msg") != null);
    }
    
}
