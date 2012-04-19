package com.bignerdranch.franklin.roger;

import java.io.IOException;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketTimeoutException;

import java.util.ArrayList;

import android.app.IntentService;

import android.content.Intent;

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

    private void findServers() throws IOException {
        MulticastSocket socket = new MulticastSocket(OUTGOING_PORT);
        InetAddress myAddress = socket.getLocalAddress();
        byte[] multicastGroup = myAddress.getAddress();
        multicastGroup[3] = (byte)255;
        socket.joinGroup(InetAddress.getByAddress(multicastGroup));

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
        DatagramPacket packet = new DatagramPacket(message, message.length, socket.getLocalAddress(), OUTGOING_PORT);
        socket.send(packet);
    }

    private ArrayList<InetAddress> waitForResponses(DatagramSocket socket) throws IOException {
        long startTime = System.currentTimeMillis();

        ArrayList<InetAddress> addresses = new ArrayList<InetAddress>();
        byte[] responseMessage = new byte[128];

        while (System.currentTimeMillis() < startTime + TIMEOUT) {
            DatagramPacket response = new DatagramPacket(responseMessage, responseMessage.length);

            try {
                socket.receive(response);
                Log.i(TAG, "received an address");
                if (!response.getAddress().getHostName().equals("localhost")) 
                    addresses.add(response.getAddress());
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
