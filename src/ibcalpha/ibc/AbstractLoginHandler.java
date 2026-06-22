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
import java.util.concurrent.TimeUnit;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;

public abstract class AbstractLoginHandler implements WindowHandler {

    private static final Logger logger = LoggerFactory.getLogger(AbstractLoginHandler.class);

    @Override
    public boolean filterEvent(Window window, int eventId) {
        switch (eventId) {
            case WindowEvent.WINDOW_OPENED:
                logger.info("Login dialog WINDOW_OPENED: LoginState is {}", LoginManager.loginManager().getLoginState().toString());
                switch (LoginManager.loginManager().getLoginState()) {
                    case LOGGED_IN:
                        return false;
                    case LOGIN_FAILED:
                        return true;
                    case LOGGING_IN:
                        return false;
                    case TWO_FA_IN_PROGRESS:
                        return false;
                    default:
                        return true;
                }
            default:
                return false;
        }
    }

    @Override
    public final void handleWindow(Window window, int eventID) {
        if (LoginManager.loginManager().getLoginHandler() == null) LoginManager.loginManager().setLoginHandler(this);
        LoginManager.loginManager().setLoginFrame((JFrame) window);
        switch (LoginManager.loginManager().getLoginState()){
            case LOGGED_OUT:
                if (! SessionManager.isRestart()) {
                    initiateLogin(window);
                } else {

                    // allow automatic relogin to ptoceed

                }
        }
    }

    @Override
    public abstract boolean recogniseWindow(Window window);
    
    private static int loginAttemptNumber = 0;
    int currentLoginAttemptNumber() {
        return loginAttemptNumber;
    }
    
    void initiateLogin(Window window) {
        LoginManager.loginManager().setLoginState(LoginManager.LoginState.AWAITING_CREDENTIALS);
        try {
            if (!initialise(window, WindowEvent.WINDOW_OPENED)) return;
            if (!setFields(window, WindowEvent.WINDOW_OPENED)) return;
            if (!preLogin(window, WindowEvent.WINDOW_OPENED)) return;

            logger.info("Login attempt: {}", ++loginAttemptNumber);
            doLogin(window);
        } catch (IbcException e) {
            logger.error("could not login: could not find control: {}", e.getMessage());
            IbcExit.exit(ErrorCodes.CANT_FIND_CONTROL);
        }
    }

    private void doLogin(final Window window) throws IbcException {
        
        // this JLabel is only present for the 1016+ versions
        // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(忽略大小写) | 相似度: 100.0%
        final JLabel initialTitleLabel = SwingUtils.findLabelByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Login");
        
        GuiDeferredExecutor.instance().execute(() -> {
            final JButton loginButton = findLoginButton(window);
            LoginManager.loginManager().setLoginState(LoginManager.LoginState.LOGGING_IN);
            SwingUtils.clickButton(loginButton);
        });
        
        String tradingMode = TradingModeManager.tradingModeManager().getTradingMode();
        if (tradingMode.equalsIgnoreCase(TradingModeManager.TRADING_MODE_PAPER)) {
            // paper trading mode doesn't use Second Factor Authentication, so nothing
            // to do here
        } else if (initialTitleLabel != null) {
            // Starting with TWS 1016, there is no longer a separate Second Factor
            // Authentication dialog. Instead, TWS replaces the Login frame's controls
            // with the controls that used to be in the 2FA dialog (so the Login frame
            // effectively becomes the 2FA frame). This doesn't generate any events
            // that IBC normally handles, so it goes undetected, and thus IBC doesn't
            // know when to process the Second Factor Authentication dialog. 
            //
            // To avoid this problem, we make a periodic check that the JLabel that
            // initially contained "LOGIN" has changed to "SECOND FACTOR AUTHENTICATION":
            // when this happens, we can pass the window to the SecondFactorAuthenticationDialogHandler
            // to be actioned.

            logger.info("Waiting for Login frame to become SecondFactorAuthenticationDialog");
            MyScheduledExecutorService.getInstance().schedule(
                    () -> {
                        checkChangeToSecondFactorAuthenticationDialog(window);
                    }, 
                    200, TimeUnit.MILLISECONDS);
        }
    }

    private void checkChangeToSecondFactorAuthenticationDialog(Window window) {
        // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(忽略大小写) | 相似度: 100.0%
        JLabel currentTitleLabel = SwingUtils.findLabelByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Second_Factor_Auth");
        if (currentTitleLabel != null) {
            // the login frame has now become the 2FA dialog, so invoke the 
            // handler for that as if it had just been opened
            logger.info("Login frame has now become SecondFactorAuthenticationDialog");
            TwsListener.logWindow(window, WindowEvent.WINDOW_OPENED);
            TwsListener.logWindowStructure(window, WindowEvent.WINDOW_OPENED, true);
            SecondFactorAuthenticationDialogHandler.getInstance().handleWindow(window, WindowEvent.WINDOW_OPENED);
        } else {
            MyScheduledExecutorService.getInstance().schedule(
                    () -> {
                        checkChangeToSecondFactorAuthenticationDialog(window);
                    }, 
                    200, TimeUnit.MILLISECONDS);
        }
    }
    
    protected abstract boolean initialise(final Window window, int eventID) throws IbcException;

    protected abstract boolean preLogin(final Window window, int eventID) throws IbcException;

    protected abstract boolean setFields(Window window, int eventID) throws IbcException;
    
    protected abstract boolean isUserIdDisabledOrAbsent(Window window);

    protected abstract boolean isPasswordDisabledOrAbsent(Window window);

    private JButton findLoginButton(final Window window) {
        // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 匹配开头 | 相似度: 60.0%
        JButton b = SwingUtils.findButtonByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Login");
        if (b == null) // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
            b = SwingUtils.findButtonByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Log_in");
        if (b == null) // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
            b = SwingUtils.findButtonByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Paper_Log_in");
        return b;
    }

    protected final void setMissingCredential(final Window window, final int credentialIndex) {
        SwingUtils.findTextField(window, credentialIndex).requestFocus();
    }

    protected final void setCredential(final Window window, 
                                            final String credentialName,
                                            final int credentialIndex, 
                                            final String value) throws IbcException {
        if (! SwingUtils.setTextField(window, credentialIndex, value)) throw new IbcException(credentialName);
    }

    protected final boolean setTradingMode(final Window window) {
        String tradingMode = TradingModeManager.tradingModeManager().getTradingMode();

        // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
// [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
        if (SwingUtils.findToggleButtonByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Live_Trading") != null &&
                SwingUtils.findToggleButtonByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Paper_Trading") != null) {
            // TWS 974 onwards uses toggle buttons rather than a combo box
            logger.info("Setting Trading mode = {}", tradingMode);
            if (tradingMode.equalsIgnoreCase(TradingModeManager.TRADING_MODE_LIVE)) {
                // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
                SwingUtils.findToggleButtonByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Live_Trading").doClick();
            } else {
                // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
                SwingUtils.findToggleButtonByBundle(window, "twslaunch.ji18n.LauncherLanguage", "Paper_Trading").doClick();
            }
            return true;
        } else {
            // the dialog appears to have been deconstructed, stop tidily
            // and do a cold restart
            
            logger.info("Login dialog has been invalidated - initiate cold restart");
            MyCachedThreadPool.getInstance().execute(new StopTask(null, true, "Login Error dialog encountered"));
            return false;
        }
    }
    
}
