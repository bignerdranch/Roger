#!/bin/sh

ifconfig | awk '
/^[a-z]/ { 
    if ($1 == "en1:") 
        wifi = 1; 
    else 
        wifi = 0 
} 

/^[ \t]*inet / { 
    if (wifi) wifiAddres = $2; 
}

END {
    if (wifi) print wifi;
    else exit(1);
}'
