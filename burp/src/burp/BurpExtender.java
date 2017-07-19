package burp;

import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JPanel;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import javax.swing.JTextArea;
import javax.swing.JCheckBox;
import javax.swing.border.TitledBorder;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import javax.swing.SwingUtilities;
import java.util.Map;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

//Could make a ton of class files pulling this out.
//In the future if more functionality gets added that should be done,
// for now this is a manageable size.
public class BurpExtender implements IBurpExtender, ITab
{
    private IBurpExtenderCallbacks callbacks;
    private IExtensionHelpers helpers;
    private JSplitPane mainPane;
    final JTextArea burpCollabTextArea = new JTextArea();
    final JTextArea sendTextArea = new JTextArea(5, 10);
    final JTextArea receiveTextArea = new JTextArea();
    final JCheckBox verboseOutput = new JCheckBox("Verbose");
    private IBurpCollaboratorClientContext collabContext;
    private String burpCollabServerLocation;
    private HashMap<Integer, String> pollHash = new HashMap<Integer,String>();
    private final String dnsFlag = "nspi";
    private final String amountFlag = "amount";
    private final DNSLogger logger = new DNSLogger();
    private final JButton startCollabButton = new JButton("Start listening");
    private final JButton pollCollabButton = new JButton("Poll now");
    private final JButton clearReceiveButton = new JButton("Clear");

    //Log to the receiveTextArea is verbosity is set to true
    class DNSLogger {
        public void log(String text) {
            if (verboseOutput.isSelected()) {
                receiveTextArea.append(text+"\n");
            }
        }
    }

    //Listener for when the "Start listening" button is clicked
    class StartCollabButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent ae) {
            //Clear any existing data from the pollHash
            pollHash = new HashMap<Integer, String>();
            collabContext = callbacks.createBurpCollaboratorClientContext();
            String payload = collabContext.generatePayload(true);
            burpCollabServerLocation = payload;
            receiveTextArea.setText("Send tunneled data to: " + payload+"\n");
        }
    }

    //Listener for when the "Poll now" button is clicked
    class PollNowButtonListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent ae) {
            //Get the collaborator interactions
            List<IBurpCollaboratorInteraction> interactions = collabContext.fetchCollaboratorInteractionsFor(burpCollabServerLocation);
            String output = "";
            int tunnelSize = 0;
            //For every interaction parse the raw packet, add the base32 decoded data to a hashmap,
            // with each key being the index # of the request.
            for (int i = 0; i < interactions.size(); i++) {
                //Get interaction
                IBurpCollaboratorInteraction interaction = interactions.get(i);
                Map<String, String> properties = interaction.getProperties();

                //Verify interaction is a DNS Address Record (A) query
                if (properties.get("type").equals("DNS") && properties.get("query_type").equals("A")) {

                    //Burp base64 encodes the raw queries
                    byte[] outputBytes = null;
                    outputBytes = helpers.base64Decode(properties.get("raw_query"));

                    //Parse the raw data and get an array of subdomains
                    DNSRequestParser parser = new DNSRequestParser();
                    List<String> outputParser = parser.parseRequestForDomain(outputBytes);

                    //Look for our dnsFlag as the first subdomain and our amountFlag as the second,
                    // this will tell us how much data to expect
                    if (outputParser.get(0).equals(dnsFlag) && outputParser.get(1).equals(amountFlag)) {
                        //Get the tunnel size so we know how much data to expect
                        tunnelSize = Integer.parseInt(outputParser.get(2));
                    }
                    //Look for our dnsFlag as the first subdomain, this will be our data
                    else if (outputParser.get(0).equals(dnsFlag)) {
                        logger.log("Chunk " + outputParser.get(2) + " received: " + outputParser.get(1));
                        pollHash.put(Integer.parseInt(outputParser.get(2)), outputParser.get(1));
                    }
                }
            }

            //Concatenate and base32 decode the tunnel data we've received
            String outputDecode = "";
            outputDecode = pollHash.values().stream().map((value) -> value).reduce(outputDecode, String::concat);
            try {
                outputDecode = helpers.bytesToString(Base32.decode(outputDecode))+"\n";
            } catch (Base32.DecodingException ex) {
                logger.log("Chunks missing, please tunnel data again");
            }

            //Output data about any missing chunks
            if (pollHash.size() != tunnelSize && tunnelSize>0) {
                logger.log("Missing " + (pollHash.size() - tunnelSize) + " chunks");
            } else if (tunnelSize==0) {
                logger.log("No chunks received");
            } else {
                logger.log(pollHash.size() +" chunks received.  All chunks received.");
            }
            receiveTextArea.append("----Data----\n\n"+outputDecode+"------------\n");
        }
    }

    //Listener for when the "Tunnel data" button is clicked
    class TunnelDataButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ae) {
            DataTunneler tunnel = new DataTunneler(burpCollabTextArea.getText(), sendTextArea.getText());
            try {
                logger.log("Tunneling data, please wait.");
                tunnel.tunnelData();
            } catch (UnknownHostException ex) {
                receiveTextArea.setText("Error tunneling data, please try again.\n"+ex.toString());
            }
        }
    }

    //Listener for when the "Tunnel data" button is clicked
    class ClearReceiveButtonListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent ae) {
            pollHash = new HashMap<Integer, String>();
            //Trigger a new listening session
            //This helps avoid any dns errors
            for(ActionListener a: startCollabButton.getActionListeners()) {
                a.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));
            }
        }
    }

    //Parse a raw DNS A name request
    //Header will be in the format of
    //12 bytes of headers
    //1 byte detailing the length of the following subdomain/hostname/tld
    //[length] bytes of data
    //repeat last two lines for each subdomain/hostname/tld
    //0x00 when data is done
    class DNSRequestParser {
        DatagramPacket packet;
        byte data[];
        private int off;
        private int len;
        String outputString = "";
        List<String> outputList = new ArrayList<String>();
        public List<String> parseRequestForDomain (byte[] request) {
            packet = new DatagramPacket(request, request.length);
            this.data = packet.getData();
            this.len = packet.getLength();
            this.off = packet.getOffset();
            //Skip 12 bytes of flags
            //I'm 50% sure it will only ever be 12 bytes...
            this.off = this.off+12;
            while(true) {
                int amount = (int)this.data[this.off];
                outputString = "";
                for (int i = 0; i < amount; i++) {
                    this.off++;
                    outputString += Character.toString((char)this.data[this.off]);
                }
                outputList.add(outputString);
                if (this.data[this.off] == 0x00) {
                    break;
                }
                this.off++;
            }
            return outputList;
        }
    }

    //Class for tunneling data through DNS.
    class DataTunneler {
        String burpCollabAddress = "";
        String tunnelData = "";
        public DataTunneler(String burpCollabAddress, String tunnelData) {
            this.burpCollabAddress = burpCollabAddress;
            this.tunnelData = "";

            //Base32 encode our data before storing, as it is pulled directly from here,
            // when being sent.
            try {
                this.tunnelData = Base32.encode(tunnelData.getBytes());
            } catch (Exception ex) {
                logger.log("Error tunneling data, please try again.\n"+ex.toString());
            }
            if (burpCollabAddress.isEmpty() || tunnelData.isEmpty()) {
                receiveTextArea.setText("Make sure there is a valid collab address and data to tunnel");
            }
        }

        //Tunnel data through DNS A name lookups, in equal sized chunks
        public void tunnelData() throws UnknownHostException {
            //TODO: Check length of collabhost to make sure we aren't exceeding 255.
            int splitLength = 63;
            List<String> splitData = splitEqually(this.tunnelData, splitLength);
            InetAddress.getAllByName(this.burpCollabAddress);

            //Had previous issues with caching, I don't think this fixed it though.
            java.security.Security.setProperty("networkaddress.cache.ttl" , "0");

            //Let the tunnel know the amount of data we are going to send
            InetAddress.getAllByName(dnsFlag+"."+amountFlag+"."+splitData.size()+"."+this.burpCollabAddress);

            //Thread so we don't hold anything up
            //TODO: Add multiple threads
            Runnable r = () -> {
                for (int i=0; i < splitData.size() ;i++) {
                    logger.log("Tunneling chunk " + i + ": "+dnsFlag+"."+splitData.get(i)+"."+i+"."+burpCollabAddress);
                    try {
                        InetAddress.getAllByName(dnsFlag+"."+splitData.get(i)+"."+i+"."+burpCollabAddress);
                    } catch (UnknownHostException ex) {
                        logger.log("Unknown host exception: " + ex.toString());
                    }
                }
                logger.log("All data tunneled");
            };
            new Thread(r).start();
        }

        //Split a string into equal sizes
        private List<String> splitEqually(String text, int size) {
            List<String> ret = new ArrayList<String>((text.length() + size - 1) / size);
            for (int start = 0; start < text.length(); start += size) {
                ret.add(text.substring(start, Math.min(text.length(), start + size)));
            }
            return ret;
        }
    }

    @Override
    public void registerExtenderCallbacks(final IBurpExtenderCallbacks callbacks)
    {
        // keep a reference to our callbacks object
        this.callbacks = callbacks;

        // obtain an extension helpers object
        helpers = callbacks.getHelpers();
        // set our extension name
        callbacks.setExtensionName("DNS Tunnel");

        // create our UI
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                //Main split pane
                mainPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT);

                //Starting tunnel pane
                //Wrapper for collab address and title
                JPanel collabAddressWrapper = new JPanel(new BorderLayout());
                collabAddressWrapper.setBorder(new TitledBorder("Burp Collaborator address"));

                //No newlines in urls
                burpCollabTextArea.getDocument().putProperty("filterNewlines",
                Boolean.TRUE);
                burpCollabTextArea.setLineWrap(true);
                collabAddressWrapper.add(burpCollabTextArea);

                //Wrapper for sending text box, label, and button
                JPanel sendTextWrapper = new JPanel(new BorderLayout());
                sendTextWrapper.setBorder(new TitledBorder("Data to tunnel"));

                //Tunnel data button and action listener
                JButton tunnelDataButton = new JButton("Tunnel data");
                tunnelDataButton.addActionListener(new TunnelDataButtonListener());

                //Send text area config and add a scroll pane
                sendTextArea.setLineWrap(true);
                JScrollPane sendScrollPane = new JScrollPane(sendTextArea);
                sendTextWrapper.add(sendScrollPane, BorderLayout.CENTER);
                sendTextWrapper.add(tunnelDataButton, BorderLayout.EAST);

                //Wrap the collab address and tunnel data box in one pane
                JPanel sendPane = new JPanel(new BorderLayout());
                sendPane.add(collabAddressWrapper, BorderLayout.NORTH);
                sendPane.add(sendTextWrapper, BorderLayout.CENTER);
                mainPane.setLeftComponent(sendPane);

                //Starting receive pane
                //Receive text area config and scroll pane
                receiveTextArea.setLineWrap(true);
                JScrollPane receievedScrollPane = new JScrollPane(receiveTextArea);

                //Wrapper for received data text and label
                JPanel receivedDataWrapper = new JPanel(new BorderLayout());
                receivedDataWrapper.setBorder(new TitledBorder("Received data"));

                //Buttons and action listeners for receieve pane
                JPanel receiveOptions = new JPanel();
                startCollabButton.addActionListener(new StartCollabButtonListener());
                pollCollabButton.addActionListener(new PollNowButtonListener());
                clearReceiveButton.addActionListener(new ClearReceiveButtonListener());
                receiveOptions.add(startCollabButton, BorderLayout.CENTER);
                receiveOptions.add(pollCollabButton, BorderLayout.CENTER);
                receiveOptions.add(clearReceiveButton, BorderLayout.CENTER);
                receiveOptions.add(verboseOutput, BorderLayout.CENTER);

                //Add buttons and scroll pane to receive pane
                receivedDataWrapper.add(receiveOptions, BorderLayout.SOUTH);
                receivedDataWrapper.add(receievedScrollPane, BorderLayout.CENTER);
                mainPane.setRightComponent(receivedDataWrapper);

                // Customize our UI components
                // Will this customize all sub components?
                callbacks.customizeUiComponent(mainPane);

                // Add the custom tab to Burp's UI
                callbacks.addSuiteTab(BurpExtender.this);

            }
        });
    }

    //
    // implement ITab
    //

    @Override
    public String getTabCaption()
    {
        return "DNS Tunnel";
    }

    @Override
    public Component getUiComponent()
    {
        return mainPane;
    }
}
