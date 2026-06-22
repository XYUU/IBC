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
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import javax.swing.JDialog;
import java.util.regex.*;

public class TooManyFailedLoginAttemptsDialogHandler implements WindowHandler {

    private static final Logger logger = LoggerFactory.getLogger(TooManyFailedLoginAttemptsDialogHandler.class);
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
        // this dialog will contain a text area with a message like this:
        //      "Too many failed login attempts. Please wait 53 seconds before attempting to re-login again."
        // or like this:
        //      "Too many failed login attempts. Please wait 4 minutes & 47 seconds before attempting to re-login again."
        //
        // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 匹配开头 | 相似度: 68.2%
        String message = SwingUtils.findTextAreaByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Frequent_Login").getText();
            logger.info(message);
            Pattern p = Pattern.compile("(?:Too many failed login attempts. Please wait (?:(\\d\\d?) minute(?:s)? )?(?:& )?(?:(\\d\\d?) second(?:s)?)?)?");
            Matcher m = p.matcher(message);
            String minutes = "";
            String seconds = "";
            if (m.find()) {
                minutes = m.group(1);
                if (minutes == null || minutes.isEmpty()) minutes = "0";
                seconds = m.group(2);
                if (seconds == null || seconds.isEmpty()) seconds = "0";
            }
            Duration waitfor = Duration.parse("PT" + minutes + "M" + seconds + "S").plus(Duration.ofSeconds(3));

            if (Settings.settings().getBoolean("ReloginAfterSecondFactorAuthenticationTimeout", false)) {
                logger.info("Will re-login at {}; login number: {}", Utils.formatDate(LocalDateTime.now().plus(waitfor)), (LoginManager.loginManager().getLoginHandler().currentLoginAttemptNumber() + 1));

                MyScheduledExecutorService.getInstance().schedule(() -> {
                    GuiDeferredExecutor.instance().execute(
                        () -> {
                            LoginManager.loginManager().getLoginHandler().initiateLogin(LoginManager.loginManager().getLoginFrame());
                        }
                    );
                }, waitfor.getSeconds(), TimeUnit.SECONDS);

                // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
                if (!SwingUtils.clickButtonByBundle(window, "twslaunch.ji18n.LauncherLanguage", "OK")) {
                    logger.error("could not dismiss \"Too many failed login attempts\" dialog because we could not find one of the controls.");
                }
            }
    }

    @Override
    public boolean recogniseWindow(Window window) {
        if (! (window instanceof JDialog)) return false;

        // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 匹配开头 | 相似度: 68.2%
        return (SwingUtils.findTextAreaByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Frequent_Login") != null);
    }

}
