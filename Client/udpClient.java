import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;

class udpClient{
  private static final int SWS = 5;

  public static void main(String args[]){
    String ip = "";
    int port = 0;
    boolean commands = false;
    int tempNum = 0;
    InetSocketAddress server;

    //System.out.println(args.length);

    if (args.length == 2){
      tempNum = Integer.parseInt(args[1]);

      if (!validitycheck(args[0].trim())){
        return;
      }
      else if (tempNum < 1024 || tempNum > 65535){
        System.out.println("Invalid port num. Closing");
        return;
      }
      else{
        port = tempNum;
        ip = args[0];
        commands = true;
      }
    }

    try{
      DatagramChannel sc = DatagramChannel.open();
      Console cons = System.console();

      //obtain IP address from user and checks for validity
      if (!commands){
        ip = cons.readLine("Enter IP address: ");
        if(!validitycheck(ip)){
          return;
        }

        //Checks for valid port number
        try{
          port = Integer.parseInt(cons.readLine("Enter port number: "));
          if(port < 1024 || port > 65535){
            throw new NumberFormatException();
          }
        }catch(NumberFormatException nfe){
          System.out.println("Port must be a valid integer between 1024 and 65535. Closing program...");
          return;
        }
      }
      server = new InetSocketAddress(ip, port);
      //connect to server
      //sc.connect(new InetSocketAddress(ip,port));

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
            sc.send(buffer, server);
            sc.receive(buff);
            message = new String(buff.array());
            System.out.println(message);
            break;
          case "exit":
            buffer = ByteBuffer.wrap(fileName.getBytes());
            sc.send(buffer, server);
          return;
          //incoming file
          default:
            if(fileName.charAt(0) != '/'){
              System.out.println("File name must start with '/'. Type help for more info");
              break;
            }
          //create new buffer and allocate space for return code
          buffer = ByteBuffer.wrap(fileName.getBytes());
          sc.send(buffer, server);

          sc.receive(buff);
          String code = new String(buff.array());
          code = code.trim();
          if(code.equals("error")){
            System.out.println("There was an error retrieving the file");
          }else if(code.equals("filenotfound")){
            System.out.println("The file was not found.");
          }else{
            try {
              try {
                //tell server we are ready to receive the size of file
                String sendIt = "sendit";
                buffer = ByteBuffer.wrap(sendIt.getBytes());
                sc.send(buffer, server);

                ByteBuffer fileBuff = ByteBuffer.allocate(4096);
                buffer = ByteBuffer.allocate(4096);
                sc.receive(buffer);
                String sizeString = new String(buffer.array());
                sizeString = sizeString.trim();
                System.out.println(sizeString);
                long fileSize = Long.valueOf(sizeString).longValue();

                //tell server ready to receive file
                buffer = ByteBuffer.wrap(sendIt.getBytes());
                sc.send(buffer, server);

                System.out.println("waiting for data..");
                int inBytes = 0;
                File f = new File(fileName.substring(1));
                if((int)fileSize > 1024){
                  fileBuff = ByteBuffer.allocate(1024);
                }else{
                  fileBuff = ByteBuffer.allocate((int)fileSize);
                }
                
                FileChannel fc = new FileOutputStream(f, false).getChannel();


                //buffer = new ByteBuffer();
                int numPackets = 0;
                int lastRec = 0;
                int lFrame = 4;
                ByteBuffer acks = ByteBuffer.allocate(1024);



                while (inBytes <= (int)fileSize) {
                  if (lFrame - lastRec <= SWS){
                    //numPackets = 0;
                    while (numPackets < SWS){
                      if (inBytes >= (int)fileSize){
                        numPackets = SWS;
                        break;
                      }
                      System.out.println("Packet Received");
                      sc.receive(fileBuff);

                      byte[] tempBytes = fileBuff.array();
                      lastRec++;
                      //lastRec = (int) tempBytes[0];
                      System.out.println(lastRec);
                      lFrame++;
                      inBytes += tempBytes.length-1;

                      ByteBuffer sizeBuf = ByteBuffer.allocate(tempBytes.length-1);


                      fileBuff.flip();
                      fileBuff = ByteBuffer.wrap(tempBytes, 1, tempBytes.length-1);
                      //fileBuff = fileBuff.compact();
                      fc.write(fileBuff);
                      fileBuff = ByteBuffer.allocate(tempBytes.length);
                      
                      //byte[] ack = new byte[1];
                      //ack[0] = (byte)(tempBytes[0]);
                      //ack[1] = (byte)(tempBytes[0]);
                      //System.out.println("" + (int)ack[0]);
                      //acks = ByteBuffer.wrap(ack);
                      //acks = acks.putInt(lastRec);
                      //System.out.println(acks.getInt());
                      //sc.send(acks, server);
                      //acks = ByteBuffer.allocate(1024);
                      }
                    }
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