#!/bin/bash

keytool -genkey -v \
        -keystore ci-keystore.jks \
        -keyalg EC \
        -groupname secp256r1 \
        -validity 10000 \
        -alias ci-key
