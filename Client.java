import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Client {
	DatagramSocket socket;
	static final int RETRY_LIMIT = 4;	/* 


	/* exit unimplemented method */
	public void exitErr(String msg) {
		System.out.println("Error: " + msg);
		System.exit(0);
	}	

	/* calculate the segment checksum by adding the payload */
	public int checksum(String content, Boolean corrupted)
	{
		if (!corrupted)  
		{
			int i; 
			int sum = 0;
			for (i = 0; i < content.length(); i++)
				sum += (int)content.charAt(i);
			return sum;
		}
		return 0;
	}


	/* check if the input file does exist */
	File checkFile(String fileName)
	{
		File file = new File(fileName);
		if(!file.exists()) {
			System.out.println("SENDER: File does not exists"); 
			System.out.println("SENDER: Exit .."); 
			System.exit(0);
		}
		return file;
	}


	/* 
	 * returns true with the given probability 
	 * 
	 * The result can be passed to the checksum function to "corrupt" a 
	 * checksum with the given probability to simulate network errors in 
	 * file transfer 
	 */
	public boolean isCorrupted(float prob)
	{ 
		double randomValue = Math.random();   
		return randomValue <= prob; 
	}



	/*
	 * The main method for the client.
	 *
	 * Only specify one transfer mode. That is, either nm or wt
	 */

	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length < 5) {
			System.err.println("Usage: java Client <host name> <port number> <input file name> <output file name> <nm|wt>");
			System.err.println("host name: is server IP address (e.g. 127.0.0.1) ");
			System.err.println("port number: is a positive number in the range 1025 to 65535");
			System.err.println("input file name: is the file to send");
			System.err.println("output file name: is the name of the output file");
			System.err.println("nm selects normal transfer|wt selects transfer with time out");
			System.exit(1);
		}

		Client client = new Client();
		String hostName = args[0];
		int portNumber = Integer.parseInt(args[1]);
		InetAddress ip = InetAddress.getByName(hostName);
		File file = client.checkFile(args[2]);
		String outputFile =  args[3];
		System.out.println ("----------------------------------------------------");
		System.out.println ("SENDER: File "+ args[2] +" exists  " );
		System.out.println ("----------------------------------------------------");
		System.out.println ("----------------------------------------------------");
		String choice=args[4];
		float loss = 0;
		Scanner sc=new Scanner(System.in);  


		System.out.println ("SENDER: Sending meta data");
		client.sendMetaData(portNumber, ip, file, outputFile); 

		if (choice.equalsIgnoreCase("wt")) {
			System.out.println("Enter the probability of a corrupted checksum (between 0 and 1): ");
			loss = sc.nextFloat();
		} 

		System.out.println("------------------------------------------------------------------");
		System.out.println("------------------------------------------------------------------");
		switch(choice)
		{
		case "nm":
			client.sendFileNormal (portNumber, ip, file);
			break;

		case "wt": 
			client.sendFileWithTimeOut(portNumber, ip, file, loss);
			break; 
		default:
			System.out.println("Error! mode is not recognised");
		} 


		System.out.println("SENDER: File is sent\n");
		sc.close(); 
	}



	public void sendMetaData(int portNumber, InetAddress IPAddress, File file, String outputFile) {

		DatagramSocket client = null;
		DatagramPacket DpSend;


		//Try catch method to catch any errors relating to the DatagramSocket
		try {
			client = new DatagramSocket();
		} catch (SocketException e) {
			System.err.println("the socket could not be opened, or the socket could not bind to the specified port " + portNumber);
			System.exit(1);
		}

		try {
			//Created a metadata object to store information about it
			MetaData metaData = new MetaData();
			//Set the size of the metadata object from size of file
			metaData.setSize((int) file.length());
			//Set the name of the metadata object to the outputFile
			metaData.setName(outputFile);
			//Created a ByteArrayOutputSteam object
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			//Created ObjectOutputStream object
			ObjectOutputStream objectStream = new ObjectOutputStream(outputStream);
			//Writing the metaData to the objectStream
			objectStream.writeObject(metaData);

			byte[] buffer = outputStream.toByteArray();
			DpSend = new DatagramPacket(buffer, buffer.length, IPAddress, portNumber);
			//Sending the metadata to the server
			client.send(DpSend);

			//Print statement for the sender to see that the metadata was sent successfully
			System.out.println("SENDER: meta data is sent (file name, size): (" + metaData.getName() + ", " + metaData.getSize() + ")");

			//Closing the client DatagramSocket
			client.close();

		} catch (IOException e) {
			System.out.println(e.getMessage());
			System.exit(1);
		}

	}



	public void sendFileNormal(int portNumber, InetAddress serverAddress, File file) {
		DatagramSocket client = null;
		//Used to store the data been sent
		String dataSending;

		try {
			client = new DatagramSocket();
		} catch (SocketException e) {
			System.err.println("the socket could not be opened, or the socket could not bind to the specified port " + portNumber);
			System.exit(1);
		}

		try {
			// Sequence number initially set to 0
			int sequence_number = 0;
			byte[] incomingData = new byte[1024];
			// Segment object created to set size of payload, set the sequence number, and set the payload
			Segment dataSegment = new Segment();
			BufferedReader br = new BufferedReader(new FileReader(file));
			while ((dataSending = br.readLine()) != null) {
				List<String> chunks = chunk(dataSending, 4);

				for (String chunkData : chunks) {
					//Setting size of the data
					dataSegment.setSize(chunkData.length());
					//Setting the payload to be sent (4 bytes)
					dataSegment.setPayLoad(chunkData);
					//Updating the sequence number each loop
					dataSegment.setSq(sequence_number);
					//Setting the checksum
					dataSegment.setChecksum(checksum(chunkData, false));


					//Created a ByteArrayOutputSteam object
					ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
					//Created ObjectOutputStream object
					ObjectOutputStream objectStream = new ObjectOutputStream(outputStream);
					//Writing the dataSegment to the objectStream
					objectStream.writeObject(dataSegment);

					byte[] buffer = outputStream.toByteArray();
					DatagramPacket DpSend = new DatagramPacket(buffer, buffer.length, serverAddress, portNumber);

					//System message with content of the data degment been sent
					System.out.println("SENDER: Sending segment: sq:" + dataSegment.getSq() + " Size:" + dataSegment.getSize() + " Checksum:" + dataSegment.getChecksum() + " Content:(" + dataSegment.getPayLoad() + ")");
					System.out.println("SENDER: Waiting for an ack");
					//Sending the packet to the server
					client.send(DpSend);


					//Changes sequence number between using XOR 1 & 0
					sequence_number ^= 1;

					DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
					//receive from the server
					client.receive(incomingPacket);
					ByteArrayInputStream in = new ByteArrayInputStream(incomingData);
					ObjectInputStream is = new ObjectInputStream(in);

					//Code adapted from Server.java
					try {
						Segment dataSeg = (Segment) is.readObject();
						System.out.println("SENDER: ACK sq=" + dataSeg.getSq() + " RECEIVED ");
						System.out.println("------------------------------------------------");
						System.out.println("------------------------------------------------");
					} catch (ClassNotFoundException e) {
						e.printStackTrace();
					}
				}
				//Closing the socket once loop has finished processing all the bytes
			}
			client.close();
			System.out.println("Closed");
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	private static final List<String> chunk(final String text, final int chunkSize) {
		// Figure out how many chunks we are going to make.
		final int textLength = text.length();
		final int numberOfChunks =
				textLength % chunkSize == 0
						? textLength / chunkSize
						: textLength / chunkSize + 1;

		// Create an array list of just the right size.
		final ArrayList<String> chunks = new ArrayList<String>(numberOfChunks);

		// Do all the chunking but the last one - here we know that all chunks
		// are exactly chunkSize long
		for (int i = 0; i < numberOfChunks - 1; i++) {
			chunks.add(text.substring(i * chunkSize, (i + 1) * chunkSize));
		}

		// Add final chunk, which may be shorter than chunkSize, so we use textLength
		// as the end index
		chunks.add(text.substring((numberOfChunks - 1) * chunkSize, textLength));

		return chunks;
	}


	public void sendFileWithTimeOut(int portNumber, InetAddress IPAddress, File file, float loss) throws IOException {
		//Resets the colour of text in console
		String RESET = "\033[0m";
		//sets colour of text to red
		String ANSI_RED = "\u001B[31m";
		DatagramSocket client = null;
		// Sequence number initially set to 0
		int sequence_number = 0;
		//Byte array buffer for the incoming data from server
		byte[] incomingData = new byte[1024];
		// Segment object created to set size of payload, set the sequence number, and set the payload
		Segment dataSegment = new Segment();
		//Used to store the data been sent
		String dataSending;
		//Used to count the amount of segments sent, initially set to 0
		int segments = 0;

		try {
			//creates a client socket
			client = new DatagramSocket();
		} catch (SocketException e) {
			//Handles exception if socket couldn't be opened
			System.err.println("the socket could not be opened, or the socket could not bind to the specified port " + portNumber);
			System.exit(1);
		}

		BufferedReader br = null;
		try {
			//reads the file
			br = new BufferedReader(new FileReader(file));
		} catch (FileNotFoundException e) {
			//Handles exception if file isn't found
			System.err.println("File not found");
			System.exit(1);
		}

		//Reads in a line of text from the file and assigns it to data
		while ((dataSending = br.readLine()) != null) {
			//sets the chunk size for the data(4 Bytes)
			List<String> chunks = chunk(dataSending, 4);
			for (String chunkData : chunks) {
				// Setting size of the data
				dataSegment.setSize(chunkData.length());
				// Setting the payload to be sent (4 bytes)
				dataSegment.setPayLoad(chunkData);
				// Updating the sequence number each loop
				dataSegment.setSq(sequence_number);
				// Setting the new checksum each loop
				dataSegment.setChecksum(checksum(chunkData, isCorrupted(loss)));

				// Created a Byte array outputSteam object
				ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
				// Created ObjectOutputStream object
				ObjectOutputStream objectStream = new ObjectOutputStream(outputStream);
				// Writing the dataSegment to the objectStream
				objectStream.writeObject(dataSegment);

				byte[] data = outputStream.toByteArray();
				DatagramPacket DpSend = new DatagramPacket(data, data.length, IPAddress, portNumber);

				// System message with content of the data degment been sent
				System.out.println("------------------------------------------------");
				System.out.println("SENDER: Sending segment: sq:" + dataSegment.getSq() + " Size:" + dataSegment.getSize() + " Checksum:" + dataSegment.getChecksum() + " Content:(" + dataSegment.getPayLoad() + ")");
				System.err.println("SENDER: Waiting for an ack");

				sequence_number ^= 1;
				//Setting the time before timeout occurs (1 second)
				client.setSoTimeout(1000);
				//Sending the data to the server
				client.send(DpSend);
				//Resets the retries to 0 for each segment
				int retries = 0;
				//Initially setting the receivedAck to false
				boolean receivedAck = false;
				//While loop that runs until receivedAck is true
				while(receivedAck==false) {
					try {

						//Code adapted from Server.java
						DatagramPacket incomingPacket = new DatagramPacket(incomingData, incomingData.length);
						// receive from the server
						client.receive(incomingPacket);
						//Byte array stream that reads data from incomingData
						ByteArrayInputStream in = new ByteArrayInputStream(incomingData);
						ObjectInputStream is = new ObjectInputStream(in);

						try {
							//Code adapted from Server.java
							Segment dataSeg = (Segment) is.readObject();
							System.out.println(RESET + "SENDER: ACK sq = " + dataSeg.getSq() + " RECEIVED ");
							System.out.println("------------------------------------------------");
							//Converts receivedAck to true once a server ACK has been received (breaks out of loop)
							receivedAck = true;
							//Keeps count of the segments sent, adds 1
							segments++;
						} catch (ClassNotFoundException e) {
							e.printStackTrace();
						}

						//Catches this exception when timeout occurs
					} catch (SocketTimeoutException e) {
						if (retries <= RETRY_LIMIT) {
							//If timeout happens with corruption simulation
							if(dataSegment.getChecksum() == 0){
								System.out.println(ANSI_RED + "------------------------------------------------");
								System.err.println("SENDER: Sending segment: sq:" + dataSegment.getSq() + " Size:" + dataSegment.getSize() + " Checksum:" + dataSegment.getChecksum() + " Content:(" + dataSegment.getPayLoad() + ")");
								System.err.println(">>>>>>>Network ERROR: segment checksum is corrupted<<<<<<<");
								//Resends the data segment with helper method
								resendSegment(dataSegment, IPAddress, portNumber, client, chunkData, loss, retries);
								//Keeping count of the amount of retries (MAX = 4)
								retries++;
								//Keeps count of the segments sent, adds 1
								segments++;
							}else{
								//If timeout occurs without corruption
								System.out.println(ANSI_RED + "------------------------------------------------");
								System.err.println("SENDER: Sending segment: sq:" + dataSegment.getSq() + " Size:" + dataSegment.getSize() + " Checksum:" + dataSegment.getChecksum() + " Content:(" + dataSegment.getPayLoad() + ")");
								System.err.println(">>>>>>>Network ERROR: HAVEN'T RECEIVED ACK<<<<<<<");
								//Resends the data segment with helper method
								resendSegment(dataSegment, IPAddress, portNumber, client, chunkData, loss, retries);
								//Keeping count of the amount of retries (MAX = 4)
								retries++;
								//Keeps count of the segments sent, adds 1
								segments++;
							}
						}else{
							//If retries is exceeded, will exit
							System.out.println(RESET + "No more retries left, exciting the program");
							System.exit(1);
						}

					}
				}
			}
		}
		System.out.println("Total segments sent: " + segments);
		System.out.println("Socket is now closed");
		//Closing the socket
		client.close();
	}

	//Helper method to resend segments
	public void resendSegment(Segment dataSegment, InetAddress IPAddress, int portNumber, DatagramSocket client, String chunkData, float loss, int retries){
		try{
			//Updating the checksum again
			dataSegment.setChecksum(checksum(chunkData, isCorrupted(loss)));
			// Created a Byte array outputSteam object for updated data
			OutputStream outputStream = new ByteArrayOutputStream();
			// Created ObjectOutputStream object
			ObjectOutputStream objectStream = new ObjectOutputStream(outputStream);
			//Writing the updated data to the object stream
			objectStream.writeObject(dataSegment);
			byte[] data = ((ByteArrayOutputStream) outputStream).toByteArray();
			DatagramPacket DpSend = new DatagramPacket(data, data.length, IPAddress, portNumber);

			System.out.println();
			System.out.println();
			System.out.println("SENDER: Waiting for an ack");
			System.err.println("SENDER: TIMEOUT ALERT: Re-sending the same segment again, current retry: " + retries);
			System.err.println("SENDER: Sending segment: sq:" + dataSegment.getSq() + " Size:" + dataSegment.getSize() + " Checksum:" + dataSegment.getChecksum() + " Content:(" + dataSegment.getPayLoad() + ")");
			System.err.println("SENDER: Waiting for an ack");


			//Sending the updated data to the server
			client.send(DpSend);


		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}