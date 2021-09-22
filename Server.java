import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.*;


public class Server {
    static Map<String, String> userInfo =new HashMap<String, String>();
    static ArrayList<String> threads = new ArrayList<String>();
    static ReentrantLock syncLock = new ReentrantLock();
    static ArrayList<String> currentUsers = new ArrayList<String>();
    static String ADMIN_PW;
    static ConcurrentHashMap<Socket, String> activeClients = new ConcurrentHashMap<Socket, String>();
    volatile static int shutDown = 0;
    public static void main(String args[]) throws Exception{
        // loading user information from credentials.txt
        File file = new File("credentials.txt");
        BufferedReader bReader = new BufferedReader(new FileReader(file));
        int serverPort = Integer.parseInt(args[0]);
        ADMIN_PW = args[1];
        String s = "";
        String kv[] = null;
        String usr = "";
        String pwd = "";
        while ((s = bReader.readLine())!= null) {
            kv = s.split(" ");
            if (kv.length!=1) {
                usr = kv[0];
                pwd = kv[1];
                userInfo.put(usr, pwd);
            }
        }
        bReader.close();


        Socket socket;
        ServerSocket welcomeSocket=null;
        System.out.println("Server Listening......");
        try{
            welcomeSocket = new ServerSocket(serverPort);

        }
        catch(IOException e){
            e.printStackTrace();
            System.out.println("Server error");

        }

        while(true){

            try{

                socket= welcomeSocket.accept();
                if (shutDown==1)
                    break;
                System.out.println(shutDown);
                System.out.println("Current user(s): "+currentUsers.size());
                activeClients.put(socket, socket.getInetAddress().getHostAddress());
                System.out.println("connection Established" + socket.getInetAddress() +":"+socket.getPort());
                syncLock.lock();
                ServerThread st=new ServerThread(socket,userInfo,threads,syncLock,currentUsers,ADMIN_PW,activeClients,welcomeSocket);
                st.start();
                syncLock.unlock();

            }

            catch(Exception e){

                break;
            }
        }
        //welcomeSocket.close();
        for (Socket ss:activeClients.keySet()) {
            ss.close();
        }
        // delete all files
        if (threads.size()!=0) {
            String currentDir = System.getProperty("user.dir");
            File delFile = new File(currentDir);

            for(String thread:threads) {
                File[] fs = delFile.listFiles();
                for (File f:fs){
                    String fileName =f.getName();
                    if (fileName.startsWith(thread+"-")||fileName.equals(thread)||fileName.equals("credentials.txt")) {
                        f.delete();
                    }

                }
            }
        }
        else {
            String currentDir = System.getProperty("user.dir");
            File delFile = new File(currentDir);
            File[] fs =delFile.listFiles();
            for (File f:fs) {
                String fileName =f.getName();
                if(fileName.equals("credentials.txt"))
                    f.delete();
            }
        }
        System.out.println("Server closed");

    }

}

class ServerThread extends Thread{

    String line=null;
    BufferedReader  is = null;
    PrintWriter os=null;
    Socket s=null;
    Map<String, String> userInfo =new HashMap<String, String>();
    ArrayList<String> threads = new ArrayList<String>();
    ReentrantLock syncLock = new ReentrantLock();
    ArrayList<String> currentUsers = new ArrayList<String>();
    String admin = "";
    int shutDown;
    ServerSocket main;
    ConcurrentHashMap<Socket, String> activeClients = new ConcurrentHashMap<Socket, String>();
    public ServerThread(Socket s, Map<String, String> m, ArrayList<String> t, ReentrantLock syncLock, ArrayList<String> currentUsers, String admin, ConcurrentHashMap<Socket, String> activeClients, ServerSocket welcomSocket){
        this.s=s;
        this.userInfo = m;
        this.threads = t;
        this.syncLock = syncLock;
        this.currentUsers = currentUsers;
        this.admin = admin;
        this.activeClients =activeClients;
        this.main = welcomSocket;
    }
    // check if a user has already logged
    static boolean isLogin(ArrayList<String> users, String user) {
        if (users.size()==0)
            return false;
        for (String s:users) {
            if (s.equals(user))
                return true;
        }
        return false;
    }
    // calculate total lines in a file
    static int getLine(File f){
        int line=0;
        try {
            FileInputStream fis = new FileInputStream(f);
            Scanner scanner = new Scanner(fis);
            while (scanner.hasNextLine()) {
                scanner.nextLine();
                line++;
            }
            try{
                fis.close();
            }
            catch (IOException e) {
                System.out.println(e);
            }
        }
        catch (FileNotFoundException e) {
            System.out.println(e);
        }

        return line;
    }
    // check if a thread exists;
    static int checkThread(ArrayList<String> threads, String t) {
        int index = 0;
        if (threads.size()==0)
            return -1;
        for (String s:threads) {

            if (s.equals(t))
                return index;
            index ++;
        }
        return -1;
    }
    // find a thread's author
    static String findPoster(File f) {
        try {
            String msg = "";
            FileReader fr = new FileReader(f);
            LineNumberReader lr = new LineNumberReader(fr);
            msg = lr.readLine();
            lr.close();
            fr.close();
            return msg;
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
    // find a message's author
    static String findAuthor(File f, String msgNo) throws Exception {
        String msg = "";
        int line = 0;
        int expectLine = Integer.parseInt(msgNo)+1;
        try {
            FileReader fr = new FileReader(f);
            LineNumberReader lr = new LineNumberReader(fr);
            while(line<expectLine) {
                line ++;
                msg = lr.readLine();
            }

            msg = msg.split(" ")[1].replace(":","");
            lr.close();
            fr.close();
            return msg;

        }
        catch (FileNotFoundException e) {
            System.out.println(e);
        }
        catch (Exception e) {
            System.out.println(e);
        }
        return msg;
    }
    // delelte a message from particular thread
    static int deleteMessage(File f, String msgNo, String user) {
        ArrayList <String> temp = new ArrayList<String >();
        try {
            // load message from file to an arraylist
            InputStream is = new FileInputStream(f);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String str = null;
            while (true) {
                str = reader.readLine();
                if(str!=null)
                    temp.add(str);
                else
                    break;
            }
            reader.close();
            is.close();
            int index = 0;
            String reg = "";
            String rpl = "";
            String remove = "";
            String no ="";
            // edit the messages
            int  number = Integer.parseInt(msgNo);
            int offset  = -1;
            for (String s:temp) {
                // index of current element
                offset ++;
                no = s.split(" ")[0];
                if(String.valueOf(index+1).equals(no))
                    index++;
                if (String.valueOf(index).equals(msgNo)&&String.valueOf(index).equals(s.split(" ")[0])) {
                    String[] t = s.split(" ");
                    String uname = t[1].replace(":","");
                    if (uname.equals(user)) {
                        remove = s;

                    }
                    // if the message was not created by current user, delete fails
                    else
                        return 0;
                }
                else if(index>number&&String.valueOf(index).equals(s.split(" ")[0])) {
                    reg = Integer.toString(index);
                    rpl = Integer.toString(index-1);
                    temp.set(offset,s.replaceFirst(reg,rpl));
                }

            }
            temp.remove(remove);
            // write messages edited into thread file
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                for(String s: temp) {
                    bw.write(s);
                    bw.newLine();
                    bw.flush();
                }
                bw.close();
                return 1;
            }
            catch (Exception e){
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
    // get numbers of message in a thread
    static int messageNum(ArrayList<String> threads, String thread) {
        File f = new File(thread);
        int number = 0;
        String temp = "";
        try {
            InputStream is = new FileInputStream(f);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String str = null;
            while (true) {
                str = reader.readLine();
                if (str!=null) {
                    temp = str.split(" ")[0];
                    if (String.valueOf(number+1).equals(temp)) {
                        number +=1;
                    }
                }
                else
                    break;
            }
            reader.close();
            is.close();

        }
        catch (Exception e) {
            e.printStackTrace();
        }

        return number;
    }
    // edit a message from particular thread
    static int editMessage(File f, String msgNo, String user, String msg) {
        ArrayList <String> temp = new ArrayList<String >();
        try {
            // load message from file to an arraylist
            InputStream is = new FileInputStream(f);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String str = null;
            while (true) {
                str = reader.readLine();
                if(str!=null)
                    temp.add(str);
                else
                    break;
            }
            reader.close();
            is.close();
            int index = 0;
            // edit the messages
            int  number = Integer.parseInt(msgNo);
            String no= "";
            int offset = -1;
            for (String s:temp) {
                offset++;
                no = s.split(" ")[0];
                if (no.equals(msgNo)) {

                    String[] t = s.split(" ", 3);
                    String uname = t[1].replace(":", "");
                    if (uname.equals(user)){
                        temp.set(offset, msgNo + " " + uname + ": " + msg);

                    }
                    // if the message was not created by current user, delete fails
                    else
                        return 0;
            }

            }
            // write messages edited into thread file
            try {
                BufferedWriter bw = new BufferedWriter(new FileWriter(f));
                for(String s: temp) {
                    bw.write(s);
                    bw.newLine();
                    bw.flush();
                }
                bw.close();
                return 1;
            }
            catch (Exception e){
                e.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }
    // read all message in a thread
    static ArrayList<String> readMessage(File f) {
        ArrayList<String> temp = new ArrayList<String>();
        try {
            // load message from file to an arraylist
            InputStream is = new FileInputStream(f);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));
            String str = null;
            while (true) {
                str = reader.readLine();
                if (str != null)
                    temp.add(str);
                else
                    break;
            }
            reader.close();
            is.close();
            temp.remove(0);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return temp;
    }
    public void run() {
        String user = "";
        int exist = 0;
        try{
            is= new BufferedReader(new InputStreamReader(s.getInputStream()));
            os=new PrintWriter(s.getOutputStream());

        }catch(IOException e){
            System.out.println("IO error in server thread");
        }

        try {

            line=is.readLine();
            while(line.compareTo("QUIT")!=0){

                syncLock.lock();
                if (line.startsWith("/usr")) {
                    user = line.substring(5);
                    System.out.println(user);
                    if (isLogin(currentUsers,user)) {
                        System.out.println(user+" has logged in");
                        os.println("/logged " +user);
                        os.flush();
                    }
                    else {
                        if (userInfo.containsKey(user)) {
                            os.println("/rpwd");
                            os.flush();
                        } else {
                            System.out.println(user + " not exist.");
                            os.println("/npwd " + user);
                            os.flush();

                        }
                    }

                }
                else if(line.startsWith("/npwd")) {
                    File file = new File("credentials.txt");
                    FileWriter fw = new FileWriter(file,true);
                    String data = "\r\n"+user + " " +line.substring(6);
                    fw.write(data);
                    fw.close();
                    userInfo.put(user,line.substring(6));
                    currentUsers.add(user);
                    System.out.println(user+" successfully login");
                    os.println("/slogin");  //successful login
                    os.flush();
                }
                else if (line.startsWith("/pwd")) {
                    if(userInfo.get(user).equals(line.substring(5)) ){
                        currentUsers.add(user);
                        System.out.println(user+" successfully login");
                        os.println("/slogin");  //successful login
                        os.flush();
                    }
                    else {
                        System.out.println("Incorrect password");
                        os.println("/errpwd");
                        os.flush();
                    }
                }

                // create thread
                else if (line.startsWith("CRT")) {
                    System.out.println(user + " issue CRT command");
                    String threadName = line.split(" ")[1];

                    File file = new File(threadName);
                    FileWriter fw = null;
                    if (checkThread(threads,threadName)==-1) {
                        if (!file.exists())
                            file.createNewFile();
                        fw = new FileWriter(file,false);
                        fw.write(user+"\r\n");
                        fw.close();
                        threads.add(threadName);
                        System.out.println("Thread "+threadName+" created");
                        os.println("/crted "+ threadName);
                        os.flush();
                    }
                    else
                    {
                        System.out.println("Thread "+threadName+" exists");
                        os.println("/exist "+threadName);
                        os.flush();
                    }
                }
                // create message
                else if (line.startsWith("MSG")) {
                    System.out.println(user + " issue MSG command");
                    String threadName = line.split(" ")[1];
                    File file = new File(threadName);
                    FileWriter fw = null;
                    String text = line.split(" ",3)[2];
                    if (checkThread(threads,threadName)==-1) {

                        System.out.println("Thread "+threadName+" not exist");
                        os.println("/notexist "+ threadName);
                        os.flush();
                    }
                    else
                    {
                        //get the number of message in a file
                        int msgNumber = messageNum(threads,threadName)+1;
                        fw = new FileWriter(file,true);
                        fw.write(Integer.toString(msgNumber)+" "+user+": "+text+"\r\n");
                        fw.close();
                        System.out.println(user+" posted to "+threadName+" thread");
                        os.println("/post "+threadName);
                        os.flush();
                    }

                }
                // List all threads
                else if (line.equals("LST")) {
                    // threads not exist
                    System.out.println(user+" issue LST command");
                    if (threads.size()==0) {
                        os.println("/nothread");
                        os.flush();
                    }
                    else {
                        String temp="/list";
                        for (String s:threads)
                            temp = temp+" "+s;
                        os.println(temp);
                        os.flush();
                    }
                }
                else if(line.startsWith("DLT")) {
                    System.out.println(user + " issue DLT command");
                    String threadName = line.split(" ")[1];
                    String msgNo = line.split(" ")[2];
                    // thread not exist
                    if (checkThread(threads,threadName)==-1) {
                        System.out.println("Thread "+threadName+" not exist");
                        os.println("/notexist "+threadName);
                        os.flush();
                    }
                    else
                    {

                        File f = new File(threadName);
                        int numAllmessage = messageNum(threads,threadName);
                        // message number not exist
                        if (Integer.parseInt(msgNo)>numAllmessage) {
                            System.out.println("Message not exist");
                            os.println("/nomsg "+msgNo);
                            os.flush();
                        }
                        // delete message
                        else {
                            int deleteSuccess = 0;
                            deleteSuccess = deleteMessage(f,msgNo,user);
                            if (deleteSuccess == 1) {
                                System.out.println("Message has been deleted");
                                os.println("/dlt "+threadName+" "+msgNo);
                                os.flush();

                            }
                            else {
                                System.out.println("Message cannot be deleted");
                                os.println("/usrunmatch");
                                os.flush();
                            }
                        }
                    }
                }
                else if (line.startsWith("EDT")) {
                    System.out.println(user + " issue EDT command");
                    String threadName = line.split(" ")[1];
                    String msgNo = line.split(" ")[2];
                    String msg = line.split(" ",4)[3];
                    File f = new File(threadName);
                    // thread not exist
                    if (checkThread(threads,threadName) == -1) {
                        System.out.println("Thread "+ threadName+" not exist");
                        os.println("/notexist "+threadName);
                        os.flush();
                    }
                    else
                    {
                        int numMessage = messageNum(threads,threadName);
                        int editSuccess = 0;
                        if (Integer.parseInt(msgNo)>numMessage) {
                            System.out.println("Message not exist");
                            os.println("/nomsg "+msgNo);
                            os.flush();
                        }
                        else {
                            editSuccess = editMessage(f, msgNo, user, msg);
                            if (editSuccess == 1) {
                                System.out.println("Message has been edited");
                                os.println("/edt " + threadName + " " + msgNo);
                                os.flush();
                            } else {
                                System.out.println("Message cannot be edited");
                                os.println("/usrunmatch");
                                os.flush();
                            }
                        }
                    }
                }
                else if (line.startsWith("RDT")) {
                    System.out.println(user + " issue RDT command");
                    String threadName = line.split(" ")[1];
                    if (checkThread(threads,threadName)==-1) {
                        System.out.println("Thread "+threadName+" not exist");
                        os.println("/notexist "+threadName);
                        os.flush();
                    }
                    else {
                        File f = new File(threadName);
                        int line = getLine(f);
                        ArrayList <String> content = new ArrayList<String>();
                        if (line==1){
                            os.println("/empty "+ threadName);
                            os.flush();
                        }
                        else {
                            content = readMessage(f);
                            for (String s:content)
                            {
                                os.println("/rdt "+content.size()+" "+ s);
                                os.flush();
                            }

                        }
                    }
                }
                else if (line.startsWith("UPD")) {
                    System.out.println(user + " issue UPD command");
                    String threadName = line.split(" ")[1];
                    String fileName = line.split(" ")[2];
                    if (checkThread(threads,threadName)==-1) {
                        System.out.println("Thread "+threadName+" not exist");
                        os.println("/notexist "+threadName);
                        os.flush();
                    }
                    else {
                        os.println("/confirm "+ threadName+ " "+fileName);
                        os.flush();
                    }
                }
                else if (line.startsWith("SUPD")) {
                    long fileSize = Long.parseLong(line.split(" ")[3]);
                    String threadName = line.split(" ")[1];
                    String fileName = threadName+"-"+line.split(" ")[2];
                    File file = new File(threadName);
                    FileOutputStream fos = new FileOutputStream(fileName);
                    InputStream is1 = s.getInputStream();
                    byte[] buffer = new byte[(int) fileSize];
                    int count = 0;
                    while(count!=fileSize) {
                        buffer[count] = (byte) is1.read();
                        count++;

                    }
                    fos.write(buffer);
                    fos.close();
                    FileWriter fw = new FileWriter(file,true);
                    fw.write(user+" "+line.split(" ")[2]+"\r\n");
                    fw.close();
                    System.out.println(user + " upload file "+line.split(" ")[2]+" to thread "+threadName);
                    os.println("/supd "+ threadName+" "+line.split(" ")[2]);
                    os.flush();


                }
                else if (line.startsWith("DWN")) {
                    System.out.println(user + " issue DWN command");
                    String threadName = line.split(" ")[1];
                    String originalFile = line.split(" ")[2];
                    String fileName = threadName+ "-"+originalFile;
                    long fileSize = 0;
                    if (checkThread(threads,threadName)==-1){
                        System.out.println("Thread "+threadName+" not exist");
                        os.println("/notexist "+threadName);
                        os.flush();
                    }
                    else {
                        File dlFile = new File(fileName);
                        if (!dlFile.exists()) {
                            System.out.println(originalFile+ "not exist in thread "+threadName);
                            os.println("/nofile "+originalFile);
                            os.flush();
                        }
                        else {
                            fileSize = dlFile.length();
                            System.out.println(originalFile +" downloaded from thread "+threadName);
                            os.println("/dlfile "+ originalFile+" "+Long.toString(fileSize));
                            os.flush();
                            FileInputStream fis = new FileInputStream(dlFile);
                            OutputStream os1 = s.getOutputStream();
                            byte[] flush = new byte[1024];
                            int len = -1;
                            while((len = fis.read(flush)) != -1) {
                                os1.write(flush, 0, len);

                            }
                            fis.close();
                        }
                    }
                }
                // remove thread
                else if (line.startsWith("RMV")) {
                    System.out.println(user + " issue RMV command");
                    String threadName = line.split(" ")[1];
                    String usr = line.split(" ")[2];
                    String currentDir = System.getProperty("user.dir");
                    File temp = new File(threadName);
                    if (checkThread(threads,threadName)==-1) {
                        System.out.println("Thread "+threadName+" not exist");
                        os.println("/notexist "+threadName);
                        os.flush();
                    }
                    else if(!findPoster(temp).equals(usr)) {
                        System.out.println("Thread "+threadName+" cannot be removed");
                        os.println("/tusrunmatch");
                        os.flush();
                    }
                    else {
                        // remove all files attched in the thread

                        File file = new File(currentDir);
                        File[] fs = file.listFiles();
                        for (File f:fs) {
                            String fileName =f.getName();
                            if (fileName.startsWith(threadName+"-")||fileName.equals(threadName)) {
                                f.delete();
                            }
                        }
                        // remove thread from array list
                        threads.remove(threadName);
                        System.out.println("Thread " + threadName +" has been removed");
                        os.println("/rmv "+threadName);
                        os.flush();
                    }
                }
                else if (line.startsWith("SHT")) {
                    System.out.println(user + " issue SHT command");
                    String pw = line.split(" ")[1];
                    if (pw.equals(admin)) {
                        shutDown = 1;
                        line = "QUIT";
                    }
                    else {
                        System.out.println("Admin password wrong");
                        os.println("/erradmin");
                        os.flush();
                    }
                }
                else {
                    os.println("n");
                    os.flush();
                }
                syncLock.unlock();
                if(line!="QUIT")
                    line=is.readLine();
                else {
                    main.close();
                    shutDown = 1;
                    os.println("/sht");
                    os.flush();
                }

                try{
                    Thread.sleep(100);//in milliseconds
                } catch (InterruptedException e){
                    System.out.println(e);
                }
            }
        } catch (IOException e) {

            line=this.getName(); //reused String line for getting thread name
            System.out.println("IO Error/ Client "+line+" terminated abruptly");
        }
        catch(Exception e){
            line=this.getName();

        }

        finally{
            try{
                System.out.println("Connection Closing..");
                if (is!=null){
                    is.close();

                }

                if(os!=null){
                    os.close();

                }
                if (s!=null){
                    activeClients.remove(s);
                    s.close();
                    currentUsers.remove(user);
                    System.out.println(user + " exited");
                }


            }
            catch(IOException ie){
                System.out.println("Socket Close Error");
            }
        }
    }
}


