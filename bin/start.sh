#!/bin/bash

./mvn-clean-install.sh
if [ $? -eq 0 ]; then
    java -jar ../target/path-0.0.1-SNAPSHOT.jar
else
    echo "Build failed."
fi

read -p "Press Enter to continue..."
