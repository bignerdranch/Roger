package com.bignerdranch.franklin.roger;

public class Constants {
    private static final String PKG = Constants.class.getPackage().getName();

    public static final String CATEGORY_DEDUPE = PKG + ".CATEGORY_DEDUPE";

    public static final String ACTION_BUILD_START = PKG + ".ACTION_BUILD_START";

    public static final String ACTION_BUILD_ERROR = PKG + ".ACTION_BUILD_ERROR";
    public static final String EXTRA_MESSAGE = PKG + ".EXTRA_MESSAGE";

    public static final String ACTION_PING = PKG + ".ACTION_PING";

    public static final String ACTION_FOUND_SERVERS = PKG + ".ACTION_FOUND_SERVERS";
    public static final String EXTRA_IP_ADDRESSES = PKG + ".EXTRA_IP_ADDRESSES";

    public static final String ACTION_DISCONNECT = PKG + ".ACTION_DISCONNECT";

	public static final String ACTION_CONNECT = PKG + ".ACTION_CONNECT";
	public static final String EXTRA_SERVER_DESCRIPTION = PKG + ".EXTRA_SERVER_DESCRIPTION";

    public static final String ACTION_NEW_LAYOUT = PKG + ".ACTION_NEW_LAYOUT";

    public static final String ACTION_INCOMING_ADB_TXN = PKG + ".ACTION_INCOMING_TXN";

    public static final String EXTRA_LAYOUT_APK_PATH = PKG + ".EXTRA_LAYOUT_APK_PATH";
    public static final String EXTRA_LAYOUT_LAYOUT_NAME = PKG + ".EXTRA_LAYOUT_LAYOUT_NAME";
    public static final String EXTRA_LAYOUT_LAYOUT_TYPE = PKG + ".EXTRA_LAYOUT_LAYOUT_TYPE";
    public static final String EXTRA_LAYOUT_PACKAGE_NAME = PKG + ".EXTRA_LAYOUT_PACKAGE_NAME";
    public static final String EXTRA_LAYOUT_MIN_VERSION = PKG + ".EXTRA_LAYOUT_MIN_VERSION";
    public static final String EXTRA_LAYOUT_TXN_ID = PKG + ".EXTRA_LAYOUT_TXN_ID";

    public static final String CATEGORY_LOCAL = PKG + ".CATEGORY_LOCAL";
    public static final String CATEGORY_REMOTE = PKG + ".CATEGORY_REMOTE";
}
