import java.io.*;
import java.net.*;
import java.util.*;


public class ChatClient implements Runnable {

  private static Socket clientSocket = null;

  private static PrintStream os = null;
  private static DataInputStream is = null;
  private static InputStream istream = null;
  private static OutputStream ostream = null;


  private static BufferedReader inputLine = null;
  private static boolean closed = false;
  public static void main(String[] args) {


    int portNumber = 3000;
    String host = "127.0.0.1";

    if (args.length < 2) {
      System.out
          .println("Usage: java MultiThreadChatClient <host> <portNumber>\n"
              + "Now using host=" + host + ", portNumber=" + portNumber);
    } else {
      host = args[0];
      portNumber = Integer.valueOf(args[1]).intValue();
    }

    try {
      clientSocket = new Socket(host, portNumber);
      inputLine = new BufferedReader(new InputStreamReader(System.in));
      ostream = clientSocket.getOutputStream();
      os = new PrintStream(ostream);
      istream = clientSocket.getInputStream();
      is = new DataInputStream(istream);
    } catch (UnknownHostException e) {
      System.err.println("Don't know about host " + host);
    } catch (IOException e) {
      System.err.println("Couldn't get I/O for the connection to the host "+ host);
    }

    if (clientSocket != null && os != null && is != null) {
      try {

        new Thread(new ChatClient()).start();
        while (!closed) {
          System.out.printf(">> ");
          String sendMessage = inputLine.readLine();
          String[] toks = sendMessage.split("\\s+");
          int temp = toks.length;
          if(temp == 3){
          if(sendMessage.split("\\s+")[2].equals("tcp"))  
          {
            os.println(sendMessage.trim());
            File file = new File(sendMessage.split("\\s+")[1]);
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);  
            int fileLength = (int)file.length(); 
            os.println(fileLength);
            os.flush();

            System.out.printf("Sending [");
            byte[] contents ;
            long current = 0;
            int prevper = 0;
            while(current!=fileLength)
            { 
                int size = 10000;
                if(fileLength - current >= size)
                    current += size;    
                else
                { 
                    size = (int)(fileLength - current); 
                    current = fileLength;
                } 
                contents = new byte[size]; 
                bis.read(contents, 0, size); 
                ostream.write(contents);
                int percentage = (int)current*100/fileLength;
                if (percentage/10 - prevper>=1)
                {
                  System.out.print(" Sending "+sendMessage.split("\\s+")[1]+" [");
                  for(int y=1;y<=percentage/10;y++)
                    System.out.print("*");
                  System.out.print("] "+Integer.toString(percentage)+"%");
                  if (size == 10000)
                    System.out.print("\r");
                  else
                    System.out.print("                     \n");
                  prevper = percentage/10;
                }
                
            }
            ostream.flush();
            System.out.print(" ");
            System.out.println("Sent file");
          }
          else if (sendMessage.split("\\s+")[2].equals("udp")){
            DatagramSocket ds = null;
            ds = new DatagramSocket();
            File file = new File(sendMessage.split("\\s+")[1]);
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);
            os.println(sendMessage.trim());
            DatagramPacket dp;
            int packetsize = 1024;
            Double noofpackets;
            noofpackets = Math.ceil(((int) file.length()) / packetsize);
            int nosofpackets = (int)Math.round(noofpackets);
            os.println(nosofpackets);
            os.flush();
            int prevper = 0;
            for (int i = 0; i < nosofpackets + 1; i++) {
                byte[] mybytearray = new byte[packetsize];
                bis.read(mybytearray, 0, mybytearray.length);
                dp = new DatagramPacket(mybytearray, mybytearray.length, InetAddress.getByName("127.0.0.1"), 4001);
                ds.send(dp); 
                try{
                  Thread.sleep(5L); 
                }
                catch(InterruptedException e){
                }
                int percentage = i*100/nosofpackets;
                if (percentage/10 - prevper>=1)
                {
                  System.out.print(" Sending "+sendMessage.split("\\s+")[1]+" [");
                  for(int y=1;y<=percentage/10;y++)
                    System.out.print("*");
                  System.out.print("] "+Integer.toString(percentage)+"%");
                  if (i != nosofpackets)
                    System.out.print("\r");
                  else
                    System.out.print("                     \n");
                  prevper = percentage/10;
                }                  
            }
            ds.close();
            System.out.println("Sent file");
                    
          }  
        else 
          os.println(sendMessage.trim()); 

        }
          else 
          {
            // System.out.println("whatsup");
            os.println(sendMessage.trim());
          }
        }
        System.out.println("whatsup");

        is.close();        
        os.close();
        clientSocket.close();
      } catch (IOException e) {
        System.err.println("IOException:  " + e);
      }
    }
  }

  public void run() {

    String responseLine;
    try {
      while ((responseLine = is.readLine().trim()) != null) {
        // System.out.println(responseLine);
        int l = responseLine.split("\\s+").length;
        if(l == 3 && !responseLine.split("\\s+")[1].equals("joined") && !responseLine.split("\\s+")[1].equals("doesn't")){
          String temp = responseLine.split("_")[1];
          responseLine = responseLine.split("_")[0];
          System.out.println(temp);
          System.out.println(responseLine.split("\\s+")[2]);
        if(responseLine.split("\\s+")[2].equals("tcp")){
          System.out.println(responseLine);
          FileOutputStream fos = new FileOutputStream(responseLine.split("\\s+")[1]);
          BufferedOutputStream bos = new BufferedOutputStream(fos);
          String fileLength = temp;
          byte[] contents = new byte[10000];     
          int totalBytesRead = 0;
          int bytesRead;
          int prevper = 0;
          while(totalBytesRead<Integer.parseInt(fileLength))
          {
              bytesRead = istream.read(contents);
              bos.write(contents, 0, bytesRead); 
              totalBytesRead += bytesRead;
              int percentage = (int)totalBytesRead*100/Integer.parseInt(fileLength);
              if (percentage/10 - prevper>=1)
              {
                System.out.print("Receiving "+responseLine.split("\\s+")[1]+" [");
                for(int y=1;y<=percentage/10;y++)
                  System.out.print("*");
                System.out.print("] "+Integer.toString(percentage));
                if (bytesRead == 10000)
                  System.out.print("\r");
                else
                  System.out.print("                     \n");
                prevper = percentage/10;
              }
          }
          bos.flush();     
          System.out.println("Recieved "+ responseLine.split("\\s+")[1]);
          System.out.printf(" >> ");  
          fos.close();
          bos.close();  
        }

        else if(responseLine.split("\\s+")[2].equals("udp")) {
          DatagramSocket serverSocket = new DatagramSocket(4001);    
          FileOutputStream fos = new FileOutputStream(responseLine.split("\\s+")[1]);
          BufferedOutputStream bos = new BufferedOutputStream(fos);
          int packetsize=1024;
          String nofpackets = is.readLine();  
          int noofpackets = Integer.parseInt(nofpackets);
          byte[] mybytearray = new byte[packetsize];
          DatagramPacket receivePacket = new DatagramPacket(mybytearray,mybytearray.length);

          // System.out.println(noofpackets+" "+mybytearray+" "+ packetsize);
          int prevper = 0;
          for(int i=0;i<noofpackets+1;i++)
           {
             serverSocket.receive(receivePacket); 
             byte audioData[] = receivePacket.getData();
             // System.out.println("Packet:"+(i+1));
             bos.write(audioData, 0,audioData.length);
             int percentage = (int)i*100/noofpackets;
             if (percentage/10 - prevper>=1)
              {
                System.out.print(" Receiving "+ responseLine.split("\\s+")[1] +" [");
                for(int y=1;y<=percentage/10;y++)
                  System.out.print("*");
                System.out.print("] "+Integer.toString(percentage));
                if (i != noofpackets)
                  System.out.print("\r");
                else
                  System.out.print("                     \n");
                prevper = percentage/10;
              }
           }

          bos.flush();
          System.out.println("Recieved "+ responseLine.split("\\s+")[1]);
          System.out.printf(" >> ");  
          fos.close();
          bos.close();
          serverSocket.close();
       }}
        else 
          System.out.println(responseLine);
        if (responseLine.indexOf("*** Bye") != -1)
          break;
      }
    } catch (IOException e) {
      System.err.println("IOException:  " + e);
    }
  }
}
