import java.io.*;
import java.net.*;
import java.util.*;

public class MultiClientServer {

  private static ServerSocket serverSocket = null;

  private static Socket clientSocket = null;

  private static final int maxClientsCount = 10;
  private static final clientThread[] threads = new clientThread[maxClientsCount];
  public static void main(String args[]) {

    int portNumber = 3000;
    if (args.length < 1) {
      System.out.println("Usage: java MultiClientServer <portNumber>\n"
          + "Now using port number=" + portNumber);
    } else {
      portNumber = Integer.valueOf(args[0]).intValue();
    }

    try {
      serverSocket = new ServerSocket(portNumber);
    } catch (IOException e) {
      System.out.println(e);
    }

    while (true) {
      try {
        clientSocket = serverSocket.accept();
        int i = 0;
        for (i = 0; i < maxClientsCount; i++) {
          if (threads[i] == null) {
            threads[i] = new clientThread(clientSocket, threads, "USER"+(i));
            threads[i].start();
            break;
          }
        }
        if (i == maxClientsCount) {
          PrintStream os = new PrintStream(clientSocket.getOutputStream());
          os.println("Server too busy. Try later.");
          os.close();
          clientSocket.close();
        }
      } catch (IOException e) {
        System.out.println(e);
      }
    }
  }
}

/*
 * The chat client thread. This client thread opens the input and the output
 * streams for a particular client, ask the client's name, informs all the
 * clients connected to the server about the fact that a new client has joined
 * the chat room, and as long as it receive data, echos that data back to all
 * other clients. The thread broadcast the incoming messages to all clients and
 * routes the private message to the particular client. When a client leaves the
 * chat room this thread informs also all the clients about that and terminates.
 */
class clientThread extends Thread {

  private String userName = null;
  private DataInputStream is = null;
  private InputStream istream = null;
  private OutputStream ostream = null;
  public String[] all_chatrooms = new String[100];
  private PrintStream os = null;
  private Socket clientSocket = null;
  private final clientThread[] threads;
  private int maxClientsCount;
  public String chatroom;
  public int num_rooms;
  // private ;

  public clientThread(Socket clientSocket, clientThread[] threads, String un) {
    this.clientSocket = clientSocket;
    this.threads = threads;
    maxClientsCount = threads.length;
    this.userName = un;
    this.chatroom = "";
    this.num_rooms = 0;
  }

  public void run() {
    int maxClientsCount = this.maxClientsCount;
    clientThread[] threads = this.threads;
    try{
      istream = clientSocket.getInputStream();
    }
    catch (IOException e) {
    }
    try{
      ostream = clientSocket.getOutputStream();
    }
    catch (IOException e) {
    }

    try {

      is = new DataInputStream(istream);
      os = new PrintStream(ostream);

      os.println("Welcome " + userName
          + " to our chat room.\nTo leave enter /q in a new line.");
      synchronized (this) {

        for (int i = 0; i < maxClientsCount; i++) {
          if (threads[i] != null && threads[i] != this) {
            threads[i].os.println("*** A new user " + userName
                + " entered the chat room !!! ***");
          }
        }
      }

      while (true) {
        String line = is.readLine();
        System.out.println("yonigga");
        System.out.println(line);
        if (line.startsWith("/quit")) {
          break;
        }

        else if(line.startsWith("leave")){
          int flag = 0;
          synchronized (this) {
            for (int i = 0; i < maxClientsCount; i++) {
              if (threads[i] != null && threads[i] != this) {
                if(chatroom.equals(threads[i].chatroom)){
                  flag = 1;
                  break;
                }
              }
            }
          }
          if(flag == 0){
            String[] temp = new String[100];
            int j = 0;
            for(int i = 0;i< num_rooms;i++){
              if(all_chatrooms[i].equals(chatroom))
                continue;
              else
                temp[j++] = all_chatrooms[i];

            }
            all_chatrooms = temp.clone();
            synchronized (this) {
              for (int i = 0; i < maxClientsCount; i++) {
                if (threads[i] != null && threads[i] != this) {
                threads[i].all_chatrooms = all_chatrooms.clone();
                threads[i].num_rooms = num_rooms;
                }
              }
            }
          }

          this.chatroom = "";

        }
        else if(line.startsWith("list")&&line.split("\\s+")[1].equals("users")){
          synchronized (this) {
            for (int i = 0; i < maxClientsCount; i++) {
              if (threads[i] != null) {
                if(chatroom.equals(threads[i].chatroom))
                  os.println(threads[i].userName);
              }
            }
          }
        }
        else if(line.startsWith("add")){
          int flag = 0;
          synchronized (this) {
            for (int i = 0; i < maxClientsCount; i++) {
              if (threads[i] != null) {
                if(line.split("\\s+")[1].equals(threads[i].userName)){
                  if(threads[i].chatroom.equals("")){
                    threads[i].chatroom = chatroom;
                    for (int j = 0; j < maxClientsCount; j++) {
                      if (threads[j] != null && threads[j].chatroom.equals(chatroom)) {
                        threads[j].os.println(threads[i].userName+" joined chatroom!!");
                        break;
                      }
                    }
                    threads[i].os.println("You joined "+chatroom);
                    flag = 1;
                    break;
                  }
                  else
                  {
                    flag = 2;
                    break;
                  }
                }
              }
            }
            if(flag == 0)
              os.println("User doesn't exist");
            else if(flag == 2)
              os.println("User part of another chatroom");

          }
        }
        else if(line.startsWith("create")){
          // System.out.println("hello");
          if (this.chatroom == ""){

            String temp = line.split("\\s+")[2];
            this.chatroom = temp;
            all_chatrooms[num_rooms] = temp;
            System.out.println(all_chatrooms[num_rooms]+" "+num_rooms);
            num_rooms++;
            synchronized (this) {
              for (int i = 0; i < maxClientsCount; i++) {
                if (threads[i] != null && threads[i] != this) {
                threads[i].all_chatrooms = all_chatrooms.clone();
                threads[i].num_rooms = num_rooms;
                }
              }
            }
            
          }
          else
            os.println("Already part of chatroom : "+chatroom);

        }
        else if(line.startsWith("list")&& line.split("\\s+")[1].equals("chatrooms")){

          for(int i = 0; i< num_rooms;i++)
            {
              // if(all_chatrooms[i]==null)
              //   break;
              os.println(all_chatrooms[i]);
            }
        }

        else if (line.startsWith("join")){
          if (this.chatroom == ""){
            String temp = line.split("\\s+")[1];
            if(Arrays.asList(all_chatrooms).contains(temp)){
              this.chatroom = temp;
              synchronized (this) {
                for (int i = 0; i < maxClientsCount; i++) {
                  if (threads[i] != null && threads[i].userName != null && threads[i]!=this && threads[i].chatroom.equals(chatroom)) {
                    threads[i].os.println(userName + " " + " joined "+chatroom);
                  }
                }
              }
            }
            else
              os.println("Chatroom doesn't exist!!");
          }
          else{
            os.println("Already part of chatroom : "+chatroom);
          }
        }
        else if (line.startsWith("reply")) {
          synchronized (this) {
            for (int i = 0; i < maxClientsCount; i++) {
              // System.out.println(threads[i].userName);
              if (threads[i] != null && threads[i].userName != null && threads[i]!=this && threads[i].chatroom.equals(chatroom)) {
                System.out.println(threads[i].userName+":"+threads[i].chatroom);
                
                
                // Thread.sleep(1L);
              }
            }
            // this.os.println(line);
          }
          String[] words = line.split("\"");
          String[] toks = line.split("\\s+"); 
          if (words.length == 2) {
            
            words[1] = words[1].trim();
            if (!words[1].isEmpty()) {
              synchronized (this) { 
                for (int i = 0; i < maxClientsCount; i++) {
                  if (threads[i] != null && threads[i] != this && threads[i].chatroom.equals(chatroom)) {
                    threads[i].os.println("<" + userName + "> " + words[1]);
                  }
                }
                this.os.println(">" + userName + "> " + words[1]);
              }
            }
          // System.out.println("Hello");

          }
          else if (toks.length == 3 && toks[2].equals("tcp")){
            FileOutputStream fos = new FileOutputStream(toks[0]);
            BufferedOutputStream bos = new BufferedOutputStream(fos);
            String fileLength = is.readLine();
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
                   System.out.print("Receiving "+toks[1]+" [");
                   for(int y=1;y<=percentage/10;y++)
                     System.out.print("*");
                   System.out.print("] "+Integer.toString(percentage)+"% from" + userName);
                   if (bytesRead == 10000)
                     System.out.print("\r");
                   else
                     System.out.print("                     \n");
                   prevper = percentage/10;
                 }
             }
             bos.flush();     
             System.out.println("Recieved "+ toks[1]+ " from " + userName);
             System.out.printf(" >> ");  
             fos.close();
             bos.close();

             //sending to all other clients
             File file = new File(toks[1]);             
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bis = new BufferedInputStream(fis);  
            int f_length = (int)file.length(); 
            
            synchronized (this) {
              for (int i = 0; i < maxClientsCount; i++) {
                if (threads[i] != null && threads[i].userName != null && threads[i]!=this && threads[i].chatroom.equals(chatroom)) {

                  threads[i].os.println(line+"_"+f_length);
                  threads[i].os.flush();
                }
              }
            }


            System.out.printf("Sending [");
            byte[] contnts ;
            long current = 0;
            prevper = 0;
            while(current!=f_length)
            { 
                int size = 10000;
                if(f_length - current >= size)
                    current += size;    
                else
                { 
                    size = (int)(f_length - current); 
                    current = f_length;
                } 
                contnts = new byte[size]; 
                bis.read(contnts, 0, size); 
                synchronized (this) {
                  for (int i = 0; i < maxClientsCount; i++) {
                    if (threads[i] != null && threads[i].userName != null && threads[i]!=this && threads[i].chatroom.equals(chatroom)) {
                      threads[i].ostream.write(contents);
                    }
                  }
                }
                int percentage = (int)current*100/f_length;
                if (percentage/10 - prevper>=1)
                {
                  System.out.print(" Sending "+toks[1]+" [");
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
          else if (toks.length == 3 && toks[2].equals("udp"))
            {
              DatagramSocket serverSocket = new DatagramSocket(4001);    
              FileOutputStream fos = new FileOutputStream(toks[0]);
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
                    System.out.print(" Receiving "+ toks[1] +" [");
                    for(int y=1;y<=percentage/10;y++)
                      System.out.print("*");
                    System.out.print("] "+Integer.toString(percentage)+"% from" + userName);
                    if (i != noofpackets)
                      System.out.print("\r");
                    else
                      System.out.print("                     \n");
                    prevper = percentage/10;
                  }
               }

              bos.flush();
              System.out.println("Recieved "+ toks[1]+ " from " + userName);
              System.out.printf(" >> ");  
              fos.close();
              bos.close();
              serverSocket.close();
           }

        } else {
          synchronized (this) {
            for (int i = 0; i < maxClientsCount; i++) {
              if (threads[i] != null && threads[i].userName != null && threads[i].chatroom.equals(chatroom)) {
                threads[i].os.println("<" + userName + "> " + line);
              }
            }
          }
        }
      }
      synchronized (this) {
        for (int i = 0; i < maxClientsCount; i++) {
          if (threads[i] != null && threads[i] != this
              && threads[i].userName != null) {
            threads[i].os.println("*** The user " + userName
                + " is leaving the chat room !!! ***");
          }
        }
      }
      os.println("*** Bye " + userName + " ***");

      synchronized (this) {
        for (int i = 0; i < maxClientsCount; i++) {
          if (threads[i] == this) {
            threads[i] = null;
          }
        }
      }

      is.close();
      os.close();
      clientSocket.close();
    } catch (IOException e) {
    }
  }
}
