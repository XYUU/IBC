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

import java.io.File;
import java.util.Arrays;

class StopTask
        implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(StopTask.class);

    private static final SwitchLock _Running = new SwitchLock();

    private final CommandChannel mChannel;
    private final boolean mForceColdRestart;
    private final String mReason;

    StopTask(final CommandChannel channel, final boolean forceColdRestart, final String reason) {
        mChannel = channel;
        mForceColdRestart = forceColdRestart;
        mReason = reason;
    }

    @Override
    public void run() {
        if (! _Running.set()) {
            logger.info("STOP already in progress");
            writeNack("STOP already in progress");
            mChannel.close();
            return;
        }

        try {
            writeInfo("Closing IBC");
            if (mForceColdRestart) createColdRestartFlagFile();
            stop(mReason);
        } catch (Exception ex) {
            writeNack(ex.getMessage());
            logger.error("An exception has occurred", ex);
            IbcExit.exit(ErrorCodes.UNHANDLED_EXCEPTION);
        }
    }
    
    private void createColdRestartFlagFile() {
        try {
        new File(System.getProperty("jtsConfigDir") + 
                 File.separator + 
                 "COLDRESTART" + 
                 System.getProperty("ibcsessionid"))
                .createNewFile();
        } catch (java.io.IOException e) {
            logger.error("An exception has occurred", e);
            IbcExit.exit(ErrorCodes.UNHANDLED_EXCEPTION);
        }
    }

    public final static boolean shutdownInProgress()
    {
        return _Running.query();
    }

    private void stop(String reason) {
        try {
            writeAck("Shutting down: " + reason);
            if (mChannel != null) mChannel.close();
            if (LoginManager.loginManager().getLoginState() != LoginManager.LoginState.LOGGED_IN) {
                CommandServer.commandServer().shutdown();
                logger.info("Login has not completed: exiting immediately");
                Runtime.getRuntime().halt(0);
            } else {
                String[] closeMenuPath = SessionManager.isGateway() ? new String[] {"File", "Close"} : new String[] {"File", "Exit"};
                logger.info("Login has completed: exiting via {} menu", Arrays.deepToString(closeMenuPath));
                Utils.invokeMenuItem(MainWindowManager.mainWindowManager().getMainWindow(), closeMenuPath);
            }
            
        } catch (Exception e) {
        }
    }

    private void writeAck(String message) {if (mChannel != null) mChannel.writeAck(message);}
    private void writeInfo(String message) {if (mChannel != null) mChannel.writeInfo(message);}
    private void writeNack(String message) {if (mChannel != null) mChannel.writeNack(message);}

}
