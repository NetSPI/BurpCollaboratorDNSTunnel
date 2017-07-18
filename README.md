# BurpCollaboratorDNSTunnel
A DNS tunnel utilizing the Burp Collaborator.

This extension will create a DNS tunnel between two Burp Suite instances.  One instance will listen on its Burp Collaborator server and the other will tunnel data through that DNS server.

### Usage
_[R] Receiving Burp instance_

_[S] Sending Burp instance_

1) [R] Click "Start listening"
2) [R] Copy the printed location of the Burp Collaborator server
3) [S] Enter the address from (2) in the "Burp Collaborator Address" text box
4) [S] Paste data to be tunneled in the "Data to tunnel" text box
5) [S] Click "Tunnel Data"
6) [R] After tunneling is completed click "Poll now"

Check the "Verbose" box for debugging information to see any errors in sending/receiving data.

### Example
An example is below (click to enlarge).  The example is using one Burp Suite instance, but the functionality works across two instances as well.
<a href="https://github.com/NetSPI/BurpCollaboratorDNSTunnel/blob/master/demo.png?raw=true" target="_blank"><img src="./demo.png"/></a>
