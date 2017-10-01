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

    public static void main(String args[]){
        try{
            DatagramChannel c = ServerSocketChannel.open();
            Console cons = System.console();

            //Checks for valid port number
            try{
                int port = Integer.parseInt(cons.readLine("Enter port number: "));
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
                        sc.read(buffer);
                        String fileName = new String(buffer.array());
                        fileName = fileName.trim();
    
                        //exit, ls, and file request commands
                        if(fileName.equals("exit")){
                            System.out.println("Client disconnected");
                            sc.close();
                            return;
    
                        //Tell client which files are available
                        }else if(fileName.equals("ls")){
                            File flocation = new File(ftserver.class.getProtectionDomain().getCodeSource().getLocation().getPath());
                            File[] files = flocation.listFiles();
                            String fileList="";
                            for(File f: files){
                                fileList += (f.getName() + "\n");
                            }
                            buffer = ByteBuffer.wrap(fileList.getBytes());
                            sc.write(buffer);
    
                        }else if(fileName != null){
                            try{
                                System.out.println("Client trying to recieve " + fileName);
    
                                try{
                                    Path filelocation = null;
                                    String l = null;
                                    try{
                                        filelocation = Paths.get(ftserver.class.getResource(fileName).toURI());
                                        File f = new File(filelocation.toString());
    
                                        String incoming = "incoming";
                                        buffer = ByteBuffer.wrap(incoming.getBytes());
                                        sc.write(buffer);
    
                                        //wait until client sends ready to recieve message
                                        buffer = ByteBuffer.allocate(4096);
                                        sc.read(buffer);
                                        buffer.compact();
    
                                        //sends length of file and then waits until client accepts it
                                        Long size = f.length();
                                        String fileSize = size.toString();
                                        buffer = ByteBuffer.wrap(fileSize.getBytes());
    
                                        sc.write(buffer);
                                        buffer = ByteBuffer.allocate(4096);
                                        sc.read(buffer);
    
                                        byte[] fileBytes;
                                        FileInputStream fis = new FileInputStream(f);
                                        BufferedInputStream bis = new BufferedInputStream(fis);
                                        long bytesRead = 0;
                                        while(bytesRead != size){
                                            int bytesToSend = 4096;
                                            if(size - bytesRead >= bytesToSend){
                                                bytesRead += bytesToSend;
                                            }else{
                                                bytesToSend = (int)(size-bytesRead);
                                                bytesRead = size;
                                            }
                                            fileBytes = new byte[bytesToSend];
                                            bis.read(fileBytes, 0, bytesToSend);
                                            buffer = ByteBuffer.wrap(fileBytes);
                                            sc.write(buffer);
                                        }
                                    }catch(URISyntaxException u){
                                        System.out.println("Error converting file");
                                    }
                                    System.out.println("The file has been sent.");
                                }catch(IOException ioe){
                                    String error = "error";
    
                                    //tells client an error occurred
                                    buffer = ByteBuffer.wrap(error.getBytes());
                                    sc.write(buffer);
                                }
                            }catch(NullPointerException npe){
                                String error = "filenotfound";
                                System.out.println("The client's file doesn't exist.");
                                buffer = ByteBuffer.wrap(error.getBytes());
                                sc.write(buffer);
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

