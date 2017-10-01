import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

class udpClient{
    public static void main(String args[]){
        try{
            SocketChannel sc = SocketChannel.open();
            Console cons = System.console();

            //obtain IP address from user and checks for validity
            String ip = cons.readLine("Enter IP address: ");
            if(!validitycheck(ip)){
                return;
            }

            //Checks for valid port number
            int port = 0;
            try{
                port = Integer.parseInt(cons.readLine("Enter port number: "));
                if(port < 1024 || port > 65535){
                    throw new NumberFormatException();
                }
            }catch(NumberFormatException nfe){
                System.out.println("Port must be a valid integer between 1024 and 65535. Closing program...");
                return;
            }
            //connect to server
            sc.connect(new InetSocketAddress(ip,port));

            while(true){
                //read command from user and make sure they actually enter something
                String fileName = "";
                while(fileName.equals("")){
                    fileName = cons.readLine("Enter command or file to send: ");
                    fileName = fileName.trim();
                }
                String message;
                ByteBuffer buff = ByteBuffer.allocate(65535);
                ByteBuffer buffer;
                switch(fileName){
                    //Lists commands
                    case "help":
                        System.out.println("exit - close the program \n" +
                                            "ls - list available files to transfer \n" +
                                            "/{filename} - without brackets to request a file");
                        break;
                    //Incoming list of files
                    case "ls":
                        buffer = ByteBuffer.wrap(fileName.getBytes());
                        sc.write(buffer);
                        sc.read(buff);
                        message = new String(buff.array());
                        System.out.println(message);
                        break;
                    case "exit":
                        buffer = ByteBuffer.wrap(fileName.getBytes());
                        sc.write(buffer);
                        return;
                    //incoming file
                    default:
                        if(fileName.charAt(0) != '/'){
                            System.out.println("File name must start with '/'. Type help for more info");
                            break;
                        }
                        //create new buffer and allocate space for return code
                        buffer = ByteBuffer.wrap(fileName.getBytes());
                        sc.write(buffer);

                        sc.read(buff);
                        String code = new String(buff.array());
                        code = code.trim();
                        if(code.equals("error")){
                            System.out.println("There was an error retrieving the file");
                        }else if(code.equals("filenotfound")){
                            System.out.println("The file was not found.");
                        }else{
                            try {
                                try {
                                    //tell server we are ready to recieve the size of file
                                    String sendIt = "sendit";
                                    buffer = ByteBuffer.wrap(sendIt.getBytes());
                                    sc.write(buffer);

                                    ByteBuffer fileBuff = ByteBuffer.allocate(4096);
                                    buffer = ByteBuffer.allocate(4096);
                                    sc.read(buffer);
                                    String sizeString = new String(buffer.array());
                                    sizeString = sizeString.trim();
                                    System.out.println(sizeString);
                                    long fileSize = Long.valueOf(sizeString).longValue();

                                    //tell server ready to recieve file
                                    buffer = ByteBuffer.wrap(sendIt.getBytes());
                                    sc.write(buffer);

                                    System.out.println("waiting for data..");

                                    File f = new File(fileName.substring(1));
                                    fileBuff = ByteBuffer.allocate(4096);
                                    int inBytes = 0;
                                    FileChannel fc = new FileOutputStream(f, false).getChannel();

                                    while (inBytes != fileSize) {
                                        inBytes += sc.read(fileBuff);
                                        fileBuff.flip();
                                        fc.write(fileBuff);
                                        fileBuff = ByteBuffer.allocate(4096);
                                    }
                                    fc.close();
                                    System.out.println("Success!");

                                }catch(NumberFormatException nfe){
                                    System.out.println("Error");
                                }
                            }catch(IOException e){
                                System.out.println("There was an error retrieving the file");
                            }
                        }
                }
            }
        }catch(IOException e){
            System.out.println("Server Unreachable. Closing program..");
            return;
        }
    }

    /****
    * Checks validity of user given IP address
    *
    * @param ip user typed IP address
    * @return true if valid, false if not
    ****/
    public static boolean validitycheck(String ip){
        try{
            String[] iparray = ip.split("\\.");
            int[] ipintarray = new int[iparray.length];
            for(int i = 0; i < iparray.length; i++){
                ipintarray[i] = Integer.parseInt(iparray[i]);
            }
            if(ipintarray.length != 4){
                throw new NumberFormatException();
            }else{
                return true;
            }
        }catch(NumberFormatException nfe){
            System.out.println("Invalid IP address.  Closing program..");
            return false;
        }
    }

}
