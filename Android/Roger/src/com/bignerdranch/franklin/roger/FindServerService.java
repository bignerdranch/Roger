package com.bignerdranch.franklin.roger;

import java.io.IOException;

import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;

import android.app.IntentService;

import android.content.Context;
import android.content.Intent;

import android.net.wifi.WifiManager;

import android.support.v4.content.LocalBroadcastManager;

import android.text.TextUtils;

import android.util.Log;

public class FindServerService extends IntentService {
    public static final String TAG = "FindServerService";
    public static final int OUTGOING_PORT = 8099;
    public static final int TIMEOUT = 1000; //ms
    public static final int BROADCAST_TRIES = 3; 

    public static final String SECRET_STRING = "SECRETS!";

    public static String ACTION_FOUND_SERVERS = 
        FindServerService.class.getPackage() + ".ACTION_FOUND_SERVERS";
    public static String EXTRA_IP_ADDRESSES = 
        FindServerService.class.getPackage() + ".EXTRA_IP_ADDRESSES";

    public FindServerService() {
        super("FindServerService");
    }

	@Override
	protected void onHandleIntent(Intent intent) {
        try {
            findServers();
        } catch (IOException ioe) {
            Log.e(TAG, "failed to find servers", ioe);
        }
        DiscoveryHelper.getInstance(this).finishDiscovery();
	}

    private InetAddress getWifiAddress() {
        WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        return getInetAddress(wifi.getConnectionInfo().getIpAddress());
    }

    private void findServers() throws IOException {
        MulticastSocket socket = new MulticastSocket(OUTGOING_PORT);

        socket.joinGroup(InetAddress.getByName("234.5.6.7"));

        socket.setTimeToLive(5);
        socket.setSoTimeout(TIMEOUT);

        HashSet<ServerDescription> addresses = new HashSet<ServerDescription>();
        for (int i = 0; i < BROADCAST_TRIES; i++) {
            broadcastSelf(socket);
            for (ServerDescription address : waitForResponses(socket)) {
                addresses.add(address);
            }
        }

        socket.close();

        Log.i(TAG, "got the following addresses:");
        for (ServerDescription address : addresses) {
            Log.i(TAG, "    " + address.getName() + " : " + address.getHostAddress() + "");
        }

        broadcastAddresses(new ArrayList<ServerDescription>(addresses));
        Log.i(TAG, "done finding");
    }

    private void broadcastAddresses(ArrayList<ServerDescription> addresses) {
        ArrayList<ServerDescription> hostAddresses = new ArrayList<ServerDescription>(addresses);

        Intent i = new Intent(ACTION_FOUND_SERVERS);
        i.putExtra(EXTRA_IP_ADDRESSES, hostAddresses);
        
        LocalBroadcastManager manager = LocalBroadcastManager.getInstance(this);
        manager.sendBroadcast(i);
    }

    private void broadcastSelf(MulticastSocket socket) throws IOException {
        byte[] message = new byte[] { (byte)0xde, (byte)0xad, (byte)0xbe, (byte)0xef };
        InetAddress multicast = InetAddress.getByName("234.5.6.7");
        DatagramPacket packet = new DatagramPacket(message, message.length, multicast, OUTGOING_PORT);
        socket.send(packet);
    }

    private InetAddress getInetAddress(int ipAddress) {
        byte[] bytes = new byte[] { 
            (byte)(ipAddress >> 0), 
            (byte)(ipAddress >> 8), 
            (byte)(ipAddress >> 16), 
            (byte) (ipAddress >> 24)
        };

        try {
            InetAddress address = InetAddress.getByAddress(bytes);
            return address;
        } catch (UnknownHostException uhe) {
            Log.e(TAG, "do not want", uhe);
            return null;
        }
    }

    private ArrayList<ServerDescription> waitForResponses(MulticastSocket socket) throws IOException {
        long startTime = System.currentTimeMillis();

        byte[] responseMessage = new byte[128];

        ArrayList<ServerDescription> addresses = new ArrayList<ServerDescription>();
        String localAddress = getWifiAddress().getHostAddress();

        while (System.currentTimeMillis() < startTime + TIMEOUT) {
            DatagramPacket response = new DatagramPacket(responseMessage, responseMessage.length);

            try {
                socket.receive(response);
                if (!localAddress.equals(response.getAddress().getHostAddress())) {
                	String hostname = new String(response.getData());
                	hostname = hostname.substring(0, response.getLength());
                    if (hostname.startsWith(SECRET_STRING)) {
                        hostname = hostname.substring(SECRET_STRING.length(), hostname.length());

                        ArrayList<String> parts = new ArrayList<String>(Arrays.asList(hostname.split("\\.")));

                        if (parts.get(parts.size() - 1).equals("local")) {
                            parts.remove(parts.size() - 1);
                        }

                        hostname = TextUtils.join(".", parts);

                        Log.i(TAG, "received an address with hostname " + hostname);
                        
                        ServerDescription description = new ServerDescription();
                        description.setName(hostname);
                        description.setHostAddress(response.getAddress().getHostAddress());
                        addresses.add(description);
                    }
                }
            } catch (SocketTimeoutException ste) {
                // done
            } catch (IOException ioe) {
                // some other error
                Log.e(TAG, "failed to receive packet", ioe);
            }
        }

        return addresses;
    }
}
