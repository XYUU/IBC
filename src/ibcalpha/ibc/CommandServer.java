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

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

class CommandServer
        implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(CommandServer.class);

    private ServerSocket mSocket = null;
    private volatile boolean mQuitting = false;

    private static CommandServer _commandServer;


    CommandServer() {
        if (_commandServer != null) throw new IllegalArgumentException();
        _commandServer = this;
    }
    
    public static CommandServer commandServer() {
        return _commandServer;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("CommandServer");

        final int port = Settings.settings().getInt("CommandServerPort", 0);
        if (port == 0) {
            logger.info("CommandServer is not started because the port is not configured");
            return;
        }

        logger.info("CommandServer is starting with port {}", port);

        if (createSocket(port)) {
            logger.info("CommandServer started and is ready to accept commands");
            for (; !mQuitting;) {
                // this will return null if the shutDown method is called
                Socket socket = getClient();

                if (socket != null) {
                    MyCachedThreadPool.getInstance().execute(new CommandDispatcher(new CommandChannel(socket)));
                }
            }
        }

        logger.info("CommandServer is shutdown");
    }

    public void shutdown() {
        mQuitting = true;
        if (mSocket != null) {
            try {
                logger.info("CommandServer closing");
                mSocket.close();
            } catch (IOException ex) {
                logger.error("An exception has occurred", ex);
            }
            mSocket = null;
        }
    }

    private boolean createSocket(final int port) {
        final int backlog = 5;
        try {
            final String bindaddr = Settings.settings().getString("BindAddress", "");
            if (!bindaddr.isEmpty()) {
                mSocket = new ServerSocket(port,
                                            backlog,
                                            InetAddress.getByName(bindaddr));
                logger.info("CommandServer listening on address: {} port: {}", bindaddr, java.lang.String.valueOf(port));
            } else {
                mSocket = new ServerSocket(port, backlog);
                logger.info("CommandServer listening on addresses: {}; port: {}", getAddresses(), java.lang.String.valueOf(port));
            }
        } catch (java.net.BindException e) {
            logger.error("An exception has occurred", e);
            logger.info("CommandServer failed to create socket");
            logger.info("CommandServer cannot process commands");
            mSocket = null;
            return false;
        } catch (IOException e) {
            logger.error("An exception has occurred", e);
            logger.info("CommandServer failed to create socket");
            logger.info("CommandServer cannot process commands");
            mSocket = null;
            return false;
        }
        return true;
    }

    private Socket getClient() {
        try {
            if (mSocket.isClosed()) return null;
            
            final Socket socket = mSocket.accept();

            final String allowedAddresses = Settings.settings().getString("ControlFrom", "");
            logger.info("CommandServer: ControlFrom setting = {}", allowedAddresses);

            if (!isPermittedClient(socket, allowedAddresses)) {
                logger.info("CommandServer denied access to: {}", socket.getInetAddress().toString());
                socket.close();
                return null;
            }
             
            logger.info("CommandServer accepted connection from: {}", socket.getInetAddress().toString());
            return socket;

        } catch (java.net.SocketException e) {
            // occurs if mSocket is closed during the call to mSocket.accept()
            return null;
        } catch (Exception e) {
            logger.error("An exception has occurred", e);
            return null;
        }
    }

    private String getAddresses() {
        final List<String> addressList = getAddressList();
        String s = addressList.isEmpty() ? "" : addressList.get(0);
        for (int i = 1; i < addressList.size(); i++) {
            s = s + "," + addressList.get(i);
        }
        return s;
    }

    private List<String> getAddressList() {
        List<String> addressList = new ArrayList<>(); 
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while(addresses.hasMoreElements()) {
                    InetAddress address = addresses.nextElement();
                    addressList.add(address.getHostAddress());
                }
            }
        } catch (SocketException e) {
            logger.info("SocketException occurred while enumerating network interfaces");
            logger.error("An exception has occurred", e);
        }
        return addressList;
    }
    
    private boolean isPermittedClient(final Socket socket, final String allowedAddresses) {
        if (socket.getInetAddress().getHostAddress().equals(mSocket.getInetAddress().getHostAddress())) return true;
        
        if (socket.getInetAddress().getHostAddress().equals(InetAddress.getLoopbackAddress().getHostAddress())) return true;

        for (String allowedClient : allowedAddresses.split(",")){
            if (allowedClient.equals(socket.getInetAddress().getHostAddress()) ||
                allowedClient.equalsIgnoreCase(socket.getInetAddress().getHostName())) {
                return true;
            }
        }
        
        return false;
    }
}
