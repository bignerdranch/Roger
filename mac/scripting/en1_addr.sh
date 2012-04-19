#!/bin/sh

ifconfig | awk '
/^[a-z]/ { 
    if ($1 == "en1:") 
        wifi = 1; 
    else 
        wifi = 0 
} 

/^[ \t]*inet / { 
    if (wifi) print $2; 
}'
