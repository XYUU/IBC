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

import java.time.Duration;
import java.time.Instant;
import java.util.ServiceLoader;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;

import ibcalpha.ibc.spi.PostLoginHook;

public abstract class LoginManager {

    private static final Logger logger = LoggerFactory.getLogger(LoginManager.class);

    private static LoginManager _LoginManager;

    static {
        _LoginManager = new DefaultLoginManager();
    }

    public static void initialise(LoginManager loginManager){
        if (loginManager == null) throw new IllegalArgumentException("loginManager");
        _LoginManager = loginManager;
    }

    public static void setDefault() {
        _LoginManager = new DefaultLoginManager();
    }

    public static LoginManager loginManager() {
        return _LoginManager;
    }

    public enum LoginState{
        LOGGED_OUT,
        LOGGED_IN,
        LOGGING_IN,
        TWO_FA_IN_PROGRESS,
        LOGIN_FAILED,
        AWAITING_CREDENTIALS
    }

    boolean readonlyLoginRequired() {
        boolean readOnly = Settings.settings().getBoolean("ReadOnlyLogin", false);
        if (readOnly && SessionManager.isGateway()) {
            logger.error("Read-only login not supported by Gateway");
            return false;
        }
        return readOnly;
    }
    
    private volatile JFrame loginFrame = null;
    JFrame getLoginFrame() {
        return loginFrame;
    }

    void setLoginFrame(JFrame window) {
        loginFrame = window;
    }
    
    private volatile LoginState loginState = LoginState.LOGGED_OUT;
    public LoginState getLoginState() {
        return loginState;
    }

    public void setLoginState(LoginState state) {
        if (state == loginState) return;
        loginState = state;
        if (null != loginState) switch (loginState) {
            case TWO_FA_IN_PROGRESS:
                logger.info("Second Factor Authentication initiated");
                if (LoginStartTime == null) LoginStartTime = Instant.now();
                break;
            case LOGGING_IN:
                if (LoginStartTime == null) LoginStartTime = Instant.now();
                break;
            case LOGGED_IN:
                logger.info("Login has completed");
                if (shutdownAfterTimeTask != null) {
                    shutdownAfterTimeTask.cancel(false);
                    shutdownAfterTimeTask = null;
                }
                firePostLoginHooks();
                break;
            default:
                break;
        }
    }

    private Instant LoginStartTime;
    private ScheduledFuture<?> shutdownAfterTimeTask;
    private volatile boolean postLoginHooksFired = false;

    private void firePostLoginHooks() {
        if (postLoginHooksFired) return;
        postLoginHooksFired = true;
        Thread t = new Thread(() -> {
            for (PostLoginHook hook : ServiceLoader.load(PostLoginHook.class)) {
                try {
                    hook.onLoginCompleted();
                } catch (Throwable ex) {
                    logger.error("PostLoginHook {} failed: {}", hook.getClass().getName(), ex);
                }
            }
        }, "ibc-post-login-hook");
        t.setDaemon(true);
        t.start();
    }

    void secondFactorAuthenticationDialogClosed() {
        if (LoginStartTime == null) {
            // login did not proceed from the SecondFactorAuthentication dialog - for
            // example because no second factor device could be selected
            return;
        }
        
        // Second factor authentication dialog timeout period
        final int SecondFactorAuthenticationTimeout = Settings.settings().getInt("SecondFactorAuthenticationTimeout", 180);

        // time (seconds) to allow for login to complete before exiting
        final int exitInterval = Settings.settings().getInt("SecondFactorAuthenticationExitInterval", 60);

        final Duration d = Duration.between(LoginStartTime, Instant.now());
        LoginStartTime = null;
        
        logger.info("Duration since login: {} seconds", d.getSeconds());

        if (d.getSeconds() < SecondFactorAuthenticationTimeout) {
            // The 2FA prompt must have been handled by the user, so authentication
            // should be under way
            
            if (SessionManager.isFIX()) {
                // no Splash screen is dislayed for FIX Gateway - just let things run
                LoginManager.loginManager().setLoginState(LoginManager.LoginState.LOGGED_IN);
                return;
            }
            
            if (!reloginPermitted()) {
                // just let loading continue
                return;
            }

            logger.info("If login has not completed, IBC will exit in {} seconds", exitInterval);
            restartAfterTime(exitInterval, "IBC closing because login has not completed after Second Factor Authentication");
            return;
        }
        
        if (!reloginPermitted()) {
            logger.info("Re-login after second factor authentication timeout not required");
            return;
        }
        
        // The 2FA prompt hasn't been handled by the user, so we re-initiate the login
        // sequence after a short delay
        logger.info("Re-login after second factor authentication timeout in 5 second");
        MyScheduledExecutorService.getInstance().schedule(() -> {
            GuiDeferredExecutor.instance().execute(
                () -> {getLoginHandler().initiateLogin(getLoginFrame());}
            );
        }, 5, TimeUnit.SECONDS);
    }
    
    private boolean reloginPermitted() {
        if (Settings.settings().getString("ReloginAfterSecondFactorAuthenticationTimeout", "").isEmpty()) {
            if (!Settings.settings().getString("ExitAfterSecondFactorAuthenticationTimeout", "").isEmpty()) {
                return Settings.settings().getBoolean("ExitAfterSecondFactorAuthenticationTimeout", false);
            }
            return false;
        }
        return Settings.settings().getBoolean("ReloginAfterSecondFactorAuthenticationTimeout", false);
    }
    
    void restartAfterTime(final int secondsTillShutdown, final String message) {
        try {
            shutdownAfterTimeTask = MyScheduledExecutorService.getInstance().schedule(()->{
                GuiExecutor.instance().execute(()->{
                    if (getLoginState() == LoginManager.LoginState.LOGGED_IN) {
                        logger.info("Login has already completed - no need for IBC to exit");
                        return;
                    }
                    logger.error(message);
                    IbcExit.exit(ErrorCodes.SECOND_FACTOR_AUTH_LOGIN_TIMED_OUT);
                });
            }, secondsTillShutdown, TimeUnit.SECONDS);
        } catch (Throwable e) {
            logger.error("An exception has occurred", e);
            IbcExit.exit(99999);
        }
    }

    public abstract void logDiagnosticMessage();

    public abstract String FIXPassword();

    public abstract String FIXUserName();

    public abstract String IBAPIPassword();

    public abstract String IBAPIUserName();

    public abstract AbstractLoginHandler getLoginHandler();

    public abstract void setLoginHandler(AbstractLoginHandler handler);

}
