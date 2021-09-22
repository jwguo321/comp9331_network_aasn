


import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.locks.*;

public class Client {

    public static void main(String args[]) throws IOException {

        int port = Integer.parseInt(args[1]);
        InetAddress address = InetAddress.getByName(args[0]);
        Socket s1 = null;
        String line = null;
        BufferedReader br = null;
        BufferedReader is = null;
        PrintWriter os = null;
        int isLogin = 0;
        int skipResponse = 0;
        String usr = "";
        try {
            s1 = new Socket(address, port);
            s1.setSoTimeout(1000);
            br = new BufferedReader(new InputStreamReader(System.in));
            is = new BufferedReader(new InputStreamReader(s1.getInputStream()));
            os = new PrintWriter(s1.getOutputStream());

        } catch (IOException e) {
            e.printStackTrace();
            System.err.print("IO Exception");
        }


        String response = null;
        try {
            System.out.print("Enter username: ");
            line = "/usr " + br.readLine();
            while (line.equals("/usr ")) {
                System.out.println("Error, username cannot be empty");
                System.out.print("Enter username: ");
                line = "/usr " + br.readLine();
            }
            usr = line.substring(5);
            os.println(line);
            os.flush();
            while (line.compareTo("XIT") != 0) {
                // check whether client sent a message to server
                if (skipResponse == 0)
                    response = is.readLine();
                skipResponse = 0;
                // server requests password
                if (response.equals("/rpwd")) {
                    System.out.print("Enter password: ");
                    line = "/pwd " + br.readLine();
                    while (line.equals("/pwd ")) {
                        System.out.println("Error, password cannot be empty");
                        System.out.print("Enter password: ");
                        line = "/pwd " + br.readLine();
                    }
                    os.println(line);
                    os.flush();
                }
                // request a new password
                else if (response.startsWith("/npwd")) {
                    System.out.print("Enter new password for " + response.substring(6) + ": ");
                    line = "/npwd " + br.readLine();
                    while (line.equals("/npwd ")) {
                        System.out.println("Error, password cannot be empty");
                        System.out.print("Enter new password for " + response.substring(6) + ": ");
                        line = "/npwd " + br.readLine();
                    }
                    os.println(line);
                    os.flush();
                }
                // server sends a error password notification
                else if (response.equals("/errpwd")) {
                    System.out.println("Incorrect password");
                    System.out.print("Enter username: ");
                    line = "/usr " + br.readLine();
                    usr = line.substring(5);
                    os.println(line);
                    os.flush();
                } else if (response.equals("/slogin")) {
                    isLogin = 1;
                    System.out.println("Welcome to the forum");

                } else if (response.startsWith("/logged")) {
                    System.out.println(response.split(" ")[1] + " has already logged in");
                    System.out.print("Enter username: ");
                    line = "/usr " + br.readLine();
                    usr = line.substring(5);
                    os.println(line);
                    os.flush();
                }
                // CRT
                else if (response.startsWith("/crted")) {
                    String threadNo = response.substring(7);
                    System.out.println("Thread " + threadNo + " created");
                } else if (response.startsWith("/exist")) {
                    System.out.println("Thread " + response.substring(7) + " exists");
                } else if (response.startsWith("/notexist")) {
                    System.out.println("Thread " + response.substring(10) + " not exist");
                } else if (response.startsWith("/post")) {
                    System.out.println("Message posted to " + response.substring(6) + " thread");
                } else if (response.startsWith("/list")) {
                    String[] allThreads = response.substring(6).split(" ");
                    System.out.println("The list of active threads:");
                    for (String s : allThreads) {
                        System.out.println(s);
                    }
                } else if (response.startsWith("/erradmin")) {
                    System.out.println("Incorrect admin password");
                } else if (response.startsWith("/nothread")) {
                    System.out.println("No threads in the forum");
                } else if (response.startsWith("/dlt")) {
                    System.out.println("Delete " + response.split(" ")[2] + " in " + response.split(" ")[1] + " successfully");
                } else if (response.startsWith("/usrunmatch")) {
                    System.out.println("Cannot delete or edit a message does not belong to you");
                } else if (response.startsWith("/tusrunmatch")) {
                    System.out.println("Cannot remove a thread that was created not by you");
                } else if (response.startsWith("/nomsg")) {
                    System.out.println("No message number " + response.split(" ")[1] + " in the thread");
                } else if (response.startsWith("/edt")) {
                    System.out.println("Message " + response.split(" ")[2] + " in thread " + response.split(" ")[1] + " edited succesfully");
                } else if (response.startsWith("/rmv")) {
                    System.out.println("Thread " + response.split(" ")[1] + " removed successfully");
                } else if (response.startsWith("/empty")) {
                    System.out.println("Thread " + response.split(" ")[1] + " is empty");
                }

                else if (response.startsWith("/rdt")) {
                    int i;
                    int total = Integer.parseInt(response.split(" ")[1]);
                    for (i = 0; i < total; i++) {
                        System.out.println(response.split(" ", 3)[2]);
                        if (i != total - 1)
                            response = is.readLine();
                        else
                            response = "";
                    }
                } else if (response.startsWith("/confirm")) {
                    String u = "";
                    String fname = response.split(" ")[2];
                    String p = "";
                    String threadName = response.split(" ")[1];
                    System.out.print("Confirm the user name: ");
                    u = br.readLine();
                    if (!u.equals(usr)) {
                        System.out.println("The user name is wrong");
                    } else {
                        System.out.print("Confirm the file name: ");
                        p = br.readLine();
                        if (p.equals(fname)) {

                            File file = new File(fname);
                            FileInputStream fis = new FileInputStream(fname);
                            long fileSize = file.length();
                            OutputStream os1 = s1.getOutputStream();
                            os.println("SUPD " + threadName + " " + fname + " " + Long.toString(fileSize));
                            os.flush();
                            byte[] flush = new byte[1024];
                            int len = -1;
                            while ((len = fis.read(flush)) != -1) {
                                os1.write(flush, 0, len);

                            }
                            fis.close();
                            response = is.readLine();
                            skipResponse = 1;

                        } else {
                            System.out.println("The file name is incorrect, upload failed");

                        }
                    }
                } else if (response.startsWith("/supd")) {
                    System.out.println("The file " + response.split(" ")[2] + " has been uploaded to thread " + response.split(" ")[1]);
                    response = "";
                } else if (response.startsWith("/dlfile")) {
                    String fileName = response.split(" ")[1];
                    int fileSize = Integer.parseInt(response.split(" ")[2]);
                    File file = new File(fileName);
                    FileOutputStream fos = new FileOutputStream(fileName);
                    InputStream is1 = s1.getInputStream();
                    byte[] buffer = new byte[(int) fileSize];
                    int count = 0;
                    while (count != fileSize) {
                        buffer[count] = (byte) is1.read();
                        count++;
                    }
                    fos.write(buffer);
                    fos.close();
                    System.out.println("File " + fileName + " received");
                    response = "";

                } else if (response.startsWith("/nofile")) {
                    System.out.println("File " + response.split(" ")[1] + " not exist");
                    response = "";
                    skipResponse = 1;
                } else if (response.startsWith("/sht")) {
                    System.out.println("Server is shutting down");
                    isLogin = 0;
                    line = "XIT";
                }
                if (isLogin == 1 && !response.startsWith("/supd")) {
                    try {


                        System.out.println("Enter one of the following commands: CRT, MSG, DLT, EDT, LST, RDT, UPD, DWN, RMV, XIT, SHT");
                        os.write("");
                        os.flush();
                        line = br.readLine();
                        if (line.startsWith("CRT")) {
                            String[] arg = line.split(" ");
                            if (arg.length != 2) {
                                System.out.println("Invalid argument");
                                skipResponse = 1;
                                response = "";
                            } else {

                                os.println(line);
                                os.flush();
                            }
                        } else if (line.startsWith("MSG")) {
                            String[] arg = line.split(" ", 3);
                            if (arg.length != 3) {
                                System.out.println("Incorrect syntax for MSG");
                                skipResponse = 1;
                                response = "";
                            } else if (arg[2].equals("") || arg[2].equals(" ")) {
                                System.out.println("The message cannot be empty");
                                skipResponse = 1;
                                response = "";
                            } else {
                                os.println(line);
                                os.flush();
                            }
                        } else if (line.startsWith("LST")) {
                            if (line.equals("LST")) {
                                os.println("LST");
                                os.flush();
                            } else {
                                System.out.println("Error, LST takes no arguments");
                                skipResponse = 1;
                                response = "";
                            }
                        } else if (line.startsWith("DLT")) {
                            String[] delete = line.split(" ");
                            if (delete.length != 3) {
                                System.out.println("Error, DLT takes 2 arguments");
                                skipResponse = 1;
                                response = "";
                            } else {
                                os.println("DLT " + delete[1] + " " + delete[2]);
                                os.flush();
                            }
                        } else if (line.startsWith("EDT")) {
                            String[] edit = line.split(" ", 4);
                            if (edit.length != 4) {
                                System.out.println("Error, EDT takes 3 arguments");
                                skipResponse = 1;
                                response = "";
                            } else if (edit[3].equals("") || edit[3].equals(" ")) {
                                System.out.println("Error, the message cannot be empty");
                                skipResponse = 1;
                                response = "";
                            } else {
                                os.println(line);
                                os.flush();
                            }
                        } else if (line.startsWith("RDT")) {
                            String[] read = line.split((" "));
                            if (read.length != 2) {
                                System.out.println("Error, EDT takes 1 argument");
                                skipResponse = 1;
                                response = "";
                            } else if (read[1].equals("") || read[1].equals(" ")) {
                                System.out.println("Error, the thread cannot be empty");
                                skipResponse = 1;
                                response = "";
                            } else {
                                os.println(line);
                                os.flush();
                            }
                        } else if (line.startsWith("UPD")) {
                            String[] upload = line.split((" "));
                            if (upload.length != 3) {
                                System.out.println("Error, UPD takes 2 argument");
                                skipResponse = 1;
                                response = "";
                            } else if (upload[2].equals("") || upload[2].equals(" ")) {
                                System.out.println("Error, the file cannot be empty");
                                skipResponse = 1;
                                response = "";
                            } else {
                                File temp = new File(upload[2]);
                                if (!temp.exists()) {
                                    System.out.println("Error, the file does not exist");
                                    skipResponse = 1;
                                    response = "";
                                } else {
                                    os.println(line);
                                    os.flush();
                                }
                            }
                        } else if (line.startsWith("DWN")) {
                            String[] download = line.split((" "));
                            if (download.length != 3) {
                                System.out.println("Error, DWN takes 2 argument");
                                skipResponse = 1;
                                response = "";
                            } else if (download[2].equals("") || download[2].equals(" ")) {
                                System.out.println("Error, the file cannot be empty");
                                skipResponse = 1;
                                response = "";
                            } else {
                                os.println(line);
                                os.flush();

                            }

                        } else if (line.startsWith("RMV")) {
                            String[] remove = line.split(" ");
                            if (remove.length != 2) {
                                System.out.println("Error, RMV takes 1 argument");
                                skipResponse = 1;
                                response = "";
                            } else {
                                os.println(line + " " + usr);
                                os.flush();
                            }
                        } else if (line.startsWith("SHT")) {
                            String[] exit = line.split(" ");
                            if (exit.length != 2) {
                                System.out.println("Error, XIT takes 2 argument");
                                skipResponse = 1;
                                response = "";
                            } else {
                                os.println(line);
                                os.flush();
                            }
                        } else if (!line.equals("XIT")) {
                            System.out.println("No such command");
                            skipResponse = 1;
                            response = "";
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }




        }
        catch(SocketTimeoutException e){
            is.close();os.close();br.close();s1.close();
            System.out.println("Goodbye ^_^");
        }

        finally{

            is.close();os.close();br.close();s1.close();
            System.out.println("Goodbye ^_^");

        }

    }
}
