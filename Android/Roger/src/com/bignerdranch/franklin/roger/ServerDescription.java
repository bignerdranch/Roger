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
    
    @Override
    public int hashCode() {
    	return hostAddress.hashCode() + name.hashCode();
    }
    
    @Override
    public boolean equals(Object o) {
    	
    	if (o instanceof ServerDescription) {
    		ServerDescription other = (ServerDescription) o;
    		if (other.getHostAddress().compareTo(hostAddress) == 0
    				&& other.getName().compareTo(name) == 0) {
    			return true;
    		}
    	}
    	
    	return false;
    }
}
