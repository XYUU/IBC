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

import static ibcalpha.ibc.SwingUtils.findCheckBox;
import java.awt.Window;
import java.awt.event.WindowEvent;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class TradesFrameHandler implements WindowHandler {

    private static final Logger logger = LoggerFactory.getLogger(TradesFrameHandler.class);

    boolean firstTradesWindowOpened;

    boolean showAllTrades;

    @Override
    public boolean filterEvent(Window window, int eventId) {
        switch (eventId) {
            case WindowEvent.WINDOW_OPENED:
            case WindowEvent.WINDOW_CLOSING:
            case WindowEvent.WINDOW_CLOSED:
                return true;
            default:
                return false;
        }
    }

    @Override
    public void handleWindow(final Window window, int eventID) {
        if (!firstTradesWindowOpened) {
            showAllTrades = Settings.settings().getBoolean("ShowAllTrades", false);
        }
        if (!showAllTrades) {
            firstTradesWindowOpened = true;
            return;
        }
        if (eventID == WindowEvent.WINDOW_OPENED) {
            if (findCheckBox(window, "Sun") != null) {
                logger.info("Setting trades log to show all trades");
                // TWS versions before 955
                SwingUtils.findAndSetCheckBoxSelected(window, true, "Sun");
                SwingUtils.findAndSetCheckBoxSelected(window, true, "Mon");
                SwingUtils.findAndSetCheckBoxSelected(window, true, "Tue");
                SwingUtils.findAndSetCheckBoxSelected(window, true, "Wed");
                SwingUtils.findAndSetCheckBoxSelected(window, true, "Thu");
                SwingUtils.findAndSetCheckBoxSelected(window, true, "Fri");
                SwingUtils.findAndSetCheckBoxSelected(window, true, "Sat");
                SwingUtils.findAndSetCheckBoxSelected(window, true, "All");

                monitorAllTradesCheckbox(window, "All");

                if (! firstTradesWindowOpened) {
                    if (Settings.settings().getBoolean("MinimizeMainWindow", false)) {
                        ((JFrame) window).setExtendedState(java.awt.Frame.ICONIFIED);
                    }
                }
            } else {
                logger.info("IBC can't set Trade History window to show all trades with this TWS version: user must do this");
                /*
                 * For TWS 955 onwards, IB have replaced the row of daily 
                 * checkboxes with what appears visually to be a combo box:
                 * it is indeed derived from a JComboBox, but setting the
                 * selected item to 'Last 7 Days' doesn't have the desired
                 * effect.
                 * 
                 * At present I don't see a way of getting round this, but 
                 * the setting chosen by the user can now be persisted
                 * between sessions, so there is really no longer a need for
                 * 'ShowAllTrades'.
                 * 
                 */

                showAllTrades = false;
                ((JFrame) window).dispose();
            }

            firstTradesWindowOpened = true;

        } else if (eventID == WindowEvent.WINDOW_CLOSING) {
            logger.info("User closing trades log");
        } else if (eventID == WindowEvent.WINDOW_CLOSED) {
            if (showAllTrades) {
                logger.info("Trades log closed by user - recreating");
                Utils.showTradesLogWindow();
            }
        }

    }

    @Override
    public boolean recogniseWindow(Window window) {
        if (! (window instanceof JFrame))  return false;

        // [AST重构审查] 来源Jar: jars/jts4launch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
        return (SwingUtils.titleContainsByBundle(window, "ji18n.Language", "Executions"));
    }

    private void monitorAllTradesCheckbox(Window window, String text) {
        final JCheckBox check = SwingUtils.findCheckBox(window, text);
        if (check != null) check.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent ce) {
                logger.info("Checkbox: {}; selected={}", check.getText(), check.isSelected());
                if (!check.isSelected()) {
                    GuiDeferredExecutor.instance().execute(() -> {
                        logger.info("Checkbox: {}; setting selected", check.getText());
                        if (!check.isSelected()) check.doClick();
                    });
                }
            }
        });
    }
}
