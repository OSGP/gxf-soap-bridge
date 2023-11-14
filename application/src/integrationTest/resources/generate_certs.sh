#!/bin/bash
# SPDX-FileCopyrightText: Copyright Contributors to the GXF project
#
# SPDX-License-Identifier: Apache-2.0

set -x

mkdir -p organisations

# Clean up previous key/trust store
rm *.jks
rm organisations/*


PASSWORD=123456
echo Generating proxy certificate
openssl req -x509 -sha256 -days 3650 -newkey rsa:4096 -keyout rootCA.key -out rootCA.crt -passout pass:$PASSWORD -subj "/C=NL/ST=Gelderland/L=Arnhem/O=Alliander/OU=IT/CN=soap-bridge-CA"
openssl req -new -newkey rsa:4096 -passout pass:$PASSWORD -keyout proxy.key -out proxy.csr -subj "/C=NL/ST=Gelderland/L=Arnhem/O=Alliander/OU=IT/CN=localhost"
openssl x509 -req -CA rootCA.crt -CAkey rootCA.key -in proxy.csr -out proxy.crt -days 3650 -CAcreateserial -passin pass:$PASSWORD  -extfile localhost.ext
openssl pkcs12 -export -out proxy.p12 -name "localhost" -inkey proxy.key -in proxy.crt -passout pass:$PASSWORD -passin pass:$PASSWORD

echo
echo
echo Creating proxy keystore
keytool -importkeystore -srckeystore proxy.p12 -srcstoretype PKCS12 -destkeystore proxy.keystore.jks -deststoretype PKCS12 -srcstorepass $PASSWORD -deststorepass $PASSWORD -noprompt

echo
echo
echo Creating proxy truststore
keytool -import -trustcacerts -noprompt -alias ca -ext san=dns:localhost,ip:127.0.0.1 -file rootCA.crt -keystore proxy.truststore.jks -srcstorepass $PASSWORD -deststorepass $PASSWORD

echo
echo
echo Generating client certificate
openssl req -new -newkey rsa:4096 -nodes -keyout testClient.key -out testClient.csr -passout pass:$PASSWORD \
    -subj "/C=NL/ST=Gelderland/L=Arnhem/O=Alliander/OU=IT/CN=testClient"


openssl x509 -req -CA rootCA.crt -CAkey rootCA.key -in proxy.csr -out proxy.crt -days 3650 -CAcreateserial -passin pass:$PASSWORD  -extfile localhost.ext
openssl x509 -req -CA rootCA.crt -CAkey rootCA.key -in testClient.csr -out testClient.crt -days 3650 -CAcreateserial -passin pass:$PASSWORD
openssl pkcs12 -export -out organisations/testClient.pfx -name "testClient" -inkey testClient.key -in testClient.crt -passout pass:$PASSWORD -passin pass:$PASSWORD

echo Creating sign and verify keys
openssl genrsa -out signing.pem 2048
openssl rsa -in signing.pem -pubout -outform DER -out verify-key.der
openssl pkcs8 -topk8 -inform PEM -outform DER -in signing.pem -out sign-key.der -nocrypt

# Clean up intermediate files
rm *.crt *.key *.csr *.p12 *.pem
