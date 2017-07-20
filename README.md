# BurpCollaboratorDNSTunnel
A DNS tunnel utilizing the Burp Collaborator.

This extension sets up the Burp Collaborator as a DNS tunnel.  One of the provided scripts will be used to exfiltrate data from a server through the DNS tunnel, displaying the tunneled data in Burp Suite.

### Usage with scripts
Multiple scripts exist for exfiltrating data from different environments.  No matter the script the setup is the same.

_[B] Burp Suite_

_[S] Script_

1) [B] Click "Start listening"
2) [B] Copy the printed location of the Burp Collaborator server
3) [S] Run the script
4) [S] Enter the Burp Collaborator address from (2) when prompted
5) [S] Paste data or file to be tunneled when prompted
6) [B] After tunneling is completed click "Poll now"

### Usage between 2 Burp Suite instances
_[R] Receiving Burp instance_

_[S] Sending Burp instance_

1) [R] Click "Start listening"
2) [R] Copy the printed location of the Burp Collaborator server
3) [S] Enter the address from (2) in the "Burp Collaborator Address" text box
4) [S] Paste data to be tunneled in the "Data to tunnel" text box
5) [S] Click "Tunnel Data"
6) [R] After tunneling is completed click "Poll now"

Check the "Verbose" box for debugging information to see any errors in sending/receiving data.

### Contributing
It would be nice to have more compact versions of the scripts for instances where you have to hand-type the scripts into the environment.  If you would like to modify or add a script follow the basic protocol below:

#### 1) Tunneling Data
```
[dnsFlag].[chunk].[chunkNumber].[burpcollaborator] #eg: nspi.JZSXIU2QJEQGS4ZAMF3WK43PNVSSC.0.fdwkpqtwvgxpk4toz2yduzx75ybozd.burpcollaborator.net
[dnsFlag] All data being tunneled needs to start with the 'nspi' subdomain, this is a flag for the tunnel to identify traffic
[chunk] Will be a 63-character base32-encoded chunk of data
[chunkNumber] Is the index of the current chunk in the overall tunneled data
[burpcollaborator] Is the full address of the Burp Collaborator server
```

#### 2) Notifying size of data sent
```
[dnsFlag].[amountFlag].[totalChunkCount].[burpcollaborator] #eg: nspi.amount.1.fdwkpqtwvgxpk4toz2yduzx75ybozd.burpcollaborator.net
[dnsFlag] All data being tunneled needs to start with the 'nspi' subdomain, this is a flag for the tunnel to identify traffic
[amountFlag] This subdomain's value will be 'amount' so the tunnel can identify this is as the request declaring the amount of data sent
[totalChunkCount] The total number of chunks sent through the tunnel, excluding this request 
[burpcollaborator] Is the full address of the Burp Collaborator server
```

### Example
An example is below (click to enlarge).  The example is using one Burp Suite instance, but the functionality works across two instances as well.
<a href="https://github.com/NetSPI/BurpCollaboratorDNSTunnel/blob/master/images/demo.png?raw=true" target="_blank"><img src="./images/demo.png"/></a>
