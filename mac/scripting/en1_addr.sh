#!/bin/sh

ifconfig | awk '
/^[a-z]/ { 
    if ($1 == "en1:") {
        wifi = 1; 
    } else {
        wifi = 0;
    }
} 

/^[ \t]*inet / { 
    if (wifi) wifiAddress = $2; 
}

END {
    if (wifiAddress) print wifiAddress;
    else exit(1);
}'
