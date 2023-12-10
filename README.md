---
Title: Reliable File Transfer Protocol UDP
Author: Aaron Cunningham
---


Reliable File Transfer Protocol UDP
===================
This program implements a Reliable File Transfer (RFT) protocol using the PAR protocol on top of UDP.
The RFT protocol ensures reliable delivery of a file between a client and a server. The program is divided into three parts,
each focusing on different aspects of the protocol.


- Part 1: Protocol RFT using PAR (nm)
  Implements the RFT protocol using the PAR (Stop-and-Wait ARQ) protocol in the absence of errors. The client and server transfer mode is normal ("nm").

- Part 2: Protocol RFT with Timeout (wt)
  Enhances the RFT protocol to handle data segment loss and corruption by implementing re-transmission with a timeout. The client transfer mode is with timeout ("wt"), and the server transfer mode is normal ("nm").

- Part 3: Protocol RFT with Lost Acknowledgments (wl)
  Adds functionality to handle lost acknowledgments. The client transfer mode is with timeout ("wt"), and the server transfer mode is with lost acknowledgments ("wl"). Simulates ACK loss and checks for duplication on the server side.


Getting started
====================================
1) Download the file by clicking ***code*** and ***download zip***.
2) Make sure all the requirements are installed/imported.
3) Compile both Client and Server with `Javac Client.java` and `Javac Server.java`


Running the program
===============================
1) First create the server with `java Server <port_number> nm` (This will run in nm mode) Alternatively run `java Server <port_number> wl` (This will run the server with loss simulation.)
2) Next create the connection from client to server, reusing the same port number to set up the server with `Java Client 127.0.0.1 <port_number> input.txt output.txt nm`(This will run mode normal) Alternatively run `Java Client 127.0.0.1 203XXX input.txt output.txt wt`(To run with mode time-out)
- Make sure to replace port_number with your own e.g `2030`


Technologies used
=================
- Java
- Java Socket Programming
- Object Serialization
- UDP
- File I/O
- Checksum
- Error simulation
- Command line arguments


