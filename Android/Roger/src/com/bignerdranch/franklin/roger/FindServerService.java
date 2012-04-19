package com.bignerdranch.franklin.roger;

import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

import java.util.ArrayList;
import java.util.Collections;

import android.app.IntentService;

import android.content.Context;
import android.content.Intent;

import android.net.wifi.WifiManager;

import android.util.Log;

public class FindServerService extends IntentService {
    public static final String TAG = "FindServerService";
    public static final int OUTGOING_PORT = 8099;
    public static final int TIMEOUT = 1000; //ms

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
	}

    private InetAddress getWifiAddress() {
        WifiManager wifi = (WifiManager)getSystemService(Context.WIFI_SERVICE);
        return getInetAddress(wifi.getConnectionInfo().getIpAddress());
    }

    private void findServers() throws IOException {
        MulticastSocket socket = new MulticastSocket(OUTGOING_PORT);

        socket.joinGroup(InetAddress.getByName("234.5.6.7"));

        socket.setTimeToLive(1);
        socket.setSoTimeout(TIMEOUT);

        broadcastSelf(socket);
        ArrayList<InetAddress> addresses = waitForResponses(socket);
        socket.close();

        Log.i(TAG, "got the following addresses:");
        for (InetAddress address : addresses) {
            Log.i(TAG, "    " + address.getHostName() + "");
        }
    }

    private void broadcastSelf(MulticastSocket socket) throws IOException {
        byte[] message = new byte[] { (byte)0xde, (byte)0xad, (byte)0xbe, (byte)0xef };
        InetAddress multicast = InetAddress.getByName("234.5.6.7");
        DatagramPacket packet = new DatagramPacket(message, message.length, multicast, OUTGOING_PORT);
        socket.send(packet);
    }

    private InetAddress getInetAddress(int ipAddress) {
        byte[] bytes = new byte[] { (byte)(ipAddress >> 0), (byte)(ipAddress >> 8), (byte)(ipAddress >> 16), (byte) (ipAddress >> 24)};

        try {
            InetAddress address = InetAddress.getByAddress(bytes);
            Log.i(TAG, "ip address from " + ipAddress + " is " + address.getHostAddress() + "");
            return address;
        } catch (UnknownHostException uhe) {
            Log.e(TAG, "do not want", uhe);
            return null;
        }
    }

    private ArrayList<InetAddress> waitForResponses(MulticastSocket socket) throws IOException {
        long startTime = System.currentTimeMillis();

        byte[] responseMessage = new byte[128];

        ArrayList<InetAddress> addresses = new ArrayList<InetAddress>();
        String localAddress = getWifiAddress().getHostAddress();
        for (InetAddress a : Collections.list(socket.getNetworkInterface().getInetAddresses())) {
            Log.i(TAG, "    " + a.getHostAddress() + "");
        }

        while (System.currentTimeMillis() < startTime + TIMEOUT) {
            DatagramPacket response = new DatagramPacket(responseMessage, responseMessage.length);

            try {
                socket.receive(response);
                if (!localAddress.equals(response.getAddress().getHostAddress())) {
                    Log.i(TAG, "received an address");
                    addresses.add(response.getAddress());
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
