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

import javax.swing.JCheckBox;
import javax.swing.JDialog;

class EnableApiTask implements ConfigurationAction{

    private static final Logger logger = LoggerFactory.getLogger(EnableApiTask.class);

    private final CommandChannel mChannel;

    private JDialog configDialog;

    EnableApiTask(final CommandChannel channel) {
        mChannel = channel;
    }

    @Override public void run() {
        try {
            logger.info("Doing ENABLEAPI configuration");

            Utils.selectApiSettings(configDialog);

            // [AST重构审查] 来源Jar: jars/jts4launch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
            JCheckBox cb = SwingUtils.findCheckBoxByBundle(configDialog, "ji18n.Language", "Conf_Item_23_Txt");
            if (cb == null) throw new IbcException("could not find Enable ActiveX checkbox");

            if (!cb.isSelected()) {
                cb.doClick();
                // [AST重构审查] 来源Jar: jars/twslaunch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
                SwingUtils.clickButtonByBundle(configDialog, "twslaunch.ji18n.LauncherLanguage", "OK");
                logger.info("TWS has been configured to accept API connections");
                mChannel.writeAck("configured");
            } else {
                logger.info("TWS is already configured to accept API connections");
                mChannel.writeAck("already configured");
            }
        } catch (IbcException e) {
            logger.error("CommandServer: {}", e.getMessage());
            mChannel.writeNack(e.getMessage());
        }
    }

    @Override
    public void initialise(JDialog configDialog) {
        this.configDialog = configDialog;
    }

}
