import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.file.Path.*;
import java.nio.file.*;

/*********************************
* UDP File Transfer Project
*
* @author Dustin Thurston
* @author Ryan Walt
*********************************/

class udpServer{

    private static final int SWS = 5;

    public static void main(String args[]){
        try{
            DatagramChannel c = DatagramChannel.open();
            Console cons = System.console();

            //Checks for valid port number
            try{
                int port = 0;
                if(args.length != 1){
                    port = Integer.parseInt(cons.readLine("Enter port number: "));
                }else{
                    port = Integer.parseInt(args[0]);
                }
                if(port < 1024 || port > 65535){
                    throw new NumberFormatException();
                }
                c.bind(new InetSocketAddress(port));
            }catch(NumberFormatException nfe){
                System.out.println("Port must be a valid integer between 1024 and 65535. Closing program...");
                return;
            }

            //Accept and handle connections
            while(true){
                try{
                    ByteBuffer buffer = ByteBuffer.allocate(1024);
                    SocketAddress client = c.receive(buffer);
                    String fileName = new String(buffer.array());
                    fileName = fileName.trim();

                        //exit, ls, and file request commands
                        if(fileName.equals("exit")){
                            System.out.println("Client disconnected");
                            return;

                        //Tell client which files are available
                        }else if(fileName.equals("ls")){
                            File flocation = new File(udpServer.class.getProtectionDomain().getCodeSource().getLocation().getPath());
                            File[] files = flocation.listFiles();
                            String fileList="";
                            for(File f: files){
                                fileList += (f.getName() + "\n");
                            }
                            buffer = ByteBuffer.wrap(fileList.getBytes());
                            c.send(buffer,client);

                        }else if(fileName != null){
                            try{
                                System.out.println("Client trying to recieve " + fileName);

                                try{
                                    Path filelocation = null;
                                    String l = null;
                                    try{
                                        filelocation = Paths.get(udpServer.class.getResource(fileName).toURI());
                                        File f = new File(filelocation.toString());

                                        String incoming = "incoming";
                                        buffer = ByteBuffer.wrap(incoming.getBytes());
                                        c.send(buffer,client);

                                        //wait until client sends ready to recieve message
                                        buffer = ByteBuffer.allocate(1024);
                                        c.receive(buffer);
                                        buffer.compact();

                                        //sends length of file and then waits until client accepts it
                                        Long size = f.length();
                                        String fileSize = size.toString();
                                        buffer = ByteBuffer.wrap(fileSize.getBytes());

                                        c.send(buffer,client);
                                        buffer = ByteBuffer.allocate(4096);
                                        c.receive(buffer);

                                        byte[] fileBytes;
                                        FileInputStream fis = new FileInputStream(f);
                                        BufferedInputStream bis = new BufferedInputStream(fis);
                                        long bytesRead = 0;

                                        //TESTING STUFF

                                        int lastAck = -1;
                                        int lastSent = 0;
                                        buffer = ByteBuffer.allocate(1024);
                                        ByteBuffer acks = ByteBuffer.allocate(1024);
                                        //TESTING STUFF END
                                        while(bytesRead != size){
                                            int bytesToSend = 1023;
                                            int numPackets = 0;

                                            while(lastSent - lastAck <= SWS){
                                                //while(numPackets < SWS){

                                                    //numPackets ++;
                                                    if(size - bytesRead >= bytesToSend){
                                                        bytesRead += bytesToSend;
                                                    }else{
                                                        bytesToSend = (int)(size-bytesRead);
                                                        bytesRead = size;
                                                    }
                                                    fileBytes = new byte[bytesToSend +  1];
                                                    fileBytes[0] = (byte) lastSent;
                                                    bis.read(fileBytes, 1, bytesToSend);
                                                    buffer = ByteBuffer.wrap(fileBytes);
                                                    //buffer.putInt(1021, lastSent);
                                                    System.out.println("About to send");
                                                    c.send(buffer,client);
                                                    lastSent++;
                                                    System.out.println("Packet Sent");

                                                    
                                                //}
                                            }
                                            c.receive(acks);
                                            byte[] ack = acks.array();
                                            lastAck = (int)ack[0];
                                            System.out.println("Last acknowledged: " + lastAck);
                                        }
                                    }catch(URISyntaxException u){
                                        System.out.println("Error converting file");
                                    }
                                    System.out.println("The file has been sent.");
                                    fileName = null;
                                    break;
                                }catch(IOException ioe){
                                    String error = "error";

                                    //tells client an error occurred
                                    buffer = ByteBuffer.wrap(error.getBytes());
                                    c.send(buffer,client);
                                }
                            }catch(NullPointerException npe){
                                String error = "filenotfound";
                                System.out.println("The client's file doesn't exist.");
                                buffer = ByteBuffer.wrap(error.getBytes());
                                c.send(buffer,client);
                            }
                        }
                }catch(IOException e){
                    System.out.println("Client figured out how to use ctrl+c. " +
                    "I guess I'll take care of it and close their connection the nice way.");
                    return;
                }
            }

        }catch(IOException e){
            System.out.println("Got an IO exception. Closing program...");
            return;
        }
    }
}
