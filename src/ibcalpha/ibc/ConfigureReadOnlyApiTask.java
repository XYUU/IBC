// This file is part of IBC.
// Copyright (C) 2004 Steven M. Kearns (skearns23@yahoo.com )
// Copyright (C) 2004 - 2019 Richard L King (rlking@aultan.com)
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

public class ConfigureReadOnlyApiTask implements ConfigurationAction{

    private static final Logger logger = LoggerFactory.getLogger(ConfigureReadOnlyApiTask.class);

    private final boolean readOnlyApi;
    private JDialog configDialog;

    ConfigureReadOnlyApiTask(boolean readOnlyApi) {
        this.readOnlyApi = readOnlyApi;
    }

    @Override
    public void run() {
        try {
            logger.info("Setting ReadOnlyApi");

            Utils.selectApiSettings(configDialog);

            // [AST重构审查] 来源Jar: jars/jts4launch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
            JCheckBox readOnlyApiCheckbox = SwingUtils.findCheckBoxByBundle(configDialog, "ji18n.Language", "Api_Enable_ReadOnly");
            if (readOnlyApiCheckbox == null) {
                // NB: we don't throw here because older TWS versions did not have this setting
                logger.error("could not find Read-Only API checkbox");
                return;
            }

            if (readOnlyApiCheckbox.isSelected() == readOnlyApi) {
                logger.info("Read-Only API checkbox is already set to: {}", readOnlyApi);
            } else {
                if (!SessionManager.isGateway()) {
                    // [AST重构审查] 来源Jar: jars/jts4launch-1045.jar | 规则: 完全匹配(区分大小写) | 相似度: 100.0%
                    JCheckBox cb = SwingUtils.findCheckBoxByBundle(configDialog, "ji18n.Language", "Conf_Item_23_Txt");
                    if (cb == null) throw new IbcException("could not find Enable ActiveX checkbox");
                    if (cb.isSelected()) ConfigDialogManager.configDialogManager().setApiConfigChangeConfirmationExpected();
                }
                readOnlyApiCheckbox.setSelected(readOnlyApi);
                logger.info("Read-Only API checkbox is now set to: {}", readOnlyApi);
            }
        } catch (IbcException e) {
            logger.error("An exception has occurred", e);
        }
    }

    @Override
    public void initialise(JDialog configDialog) {
        this.configDialog = configDialog;
    }
}
