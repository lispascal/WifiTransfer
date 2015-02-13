# WifiTransfer
Android App for File Transfer through HTTP on LAN.

## Build Information
WifiTransfer is made in Android Studio 1.0.1. 

1. Import using VCS-> Checkout From Version Control -> GitHub
2. Put this repository's https clone url in the URL spot
3. Choose other directory names
4. Press "Clone".

## Use
1. Run the application with a WiFi connection.
2. Open the provided IP on another device or computer on the same network.
3. The webpage served will allow you to navigate directories, upload files to, and download files from those directories .

## Features
* Uploading and downloading single files at a time.
* Hosts an imitation web server (handles requests manually)
* No file size limit

## To-do
* Bandwidth in tests varied from 100-600 kBps, depending on processing or connection/distance from access point.
Use profiling and refactoring to improve those speeds and make them reliable.
* Bypass string creation, comparison, and searching by implementing a byte array/bytewise version of each.
