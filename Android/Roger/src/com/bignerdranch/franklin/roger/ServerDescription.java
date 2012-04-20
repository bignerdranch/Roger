package com.bignerdranch.franklin.roger;

import java.io.Serializable;

public class ServerDescription implements Serializable {
    public static final long serialVersionUID = 0l;
    
    protected String hostAddress;
    protected String name;

    public String getHostAddress() {
        return this.hostAddress;
    }

    public void setHostAddress(String hostAddress) {
        this.hostAddress = hostAddress;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return getName();
    }
}
