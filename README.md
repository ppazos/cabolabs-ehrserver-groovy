# cabolabs-ehrserver-groovy
Groovy client for the CaboLabs openEHR EHRServer (https://github.com/ppazos/cabolabs-ehrserver)

## SSL Certificate configuration for HTTPS access

You need to have openssl installed: https://www.openssl.org/

On the command line / terminal

    $ openssl s_client -connect your_domain_here:443

Will return something like:

```
CONNECTED(00000003)
depth=1 /C=US/O=Let's Encrypt/CN=Let's Encrypt Authority X3
verify error:num=20:unable to get local issuer certificate
verify return:0
---
Certificate chain
 0 s:/CN=your_domain_here
   i:/C=US/O=Let's Encrypt/CN=Let's Encrypt Authority X3
 1 s:/C=US/O=Let's Encrypt/CN=Let's Encrypt Authority X3
   i:/O=Digital Signature Trust Co./CN=DST Root CA X3
---
Server certificate
-----BEGIN CERTIFICATE-----
MIIFGzCCBAOgAwIBAgIS....
-----END CERTIFICATE-----
subject=/CN=your_domain_here
issuer=/C=US/O=Let's Encrypt/CN=Let's Encrypt Authority X3
---
No client certificate CA names sent
---
SSL handshake has read 3421 bytes and written 444 bytes
---
New, TLSv1/SSLv3, Cipher is DHE-RSA-AES128-SHA
Server public key is 2048 bit
Compression: NONE
Expansion: NONE
SSL-Session:
    Protocol  : TLSv1
    Cipher    : DHE-RSA-AES128-SHA
    Session-ID: 59FB6B...
    Session-ID-ctx:
    Master-Key: AA31121...
    Key-Arg   : None
    Start Time: 1509649408
    Timeout   : 300 (sec)
    Verify return code: 20 (unable to get local issuer certificate)
---
read:errno=0
```

Copy the text from -----BEGIN CERTIFICATE----- to -----END CERTIFICATE-----, including those lines.

Create a file "my_certificate.crt" and paste the content there, including the BEGIN CERTIFICATE and END CERTIFICATE lines.

From the console, use the keytool from Java:

    $ keytool -importcert -alias "server_alias" -file my_certificate.crt -keystore store.jks -storepass test1234

Write "y" when it asks if you trust the certificate.


If the alias already exists, you can delete it:

    $ keytool -delete -alias "server_alias" -keystore store.jks -storepass test1234


keytool reference: 

https://www.sslshopper.com/article-most-common-java-keytool-keystore-commands.html
