package com.lis.pascal.wifitransfer;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpStatus;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.message.BasicHttpEntityEnclosingRequest;
import org.apache.http.message.BasicHttpResponse;
import org.apache.http.params.BasicHttpParams;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ServerAcceptor acceptor = new ServerAcceptor();
        new Thread(acceptor).start();
    }

    public class ServerAcceptor implements Runnable {

        int port = 0;
        ServerSocket ssock;
        ServerAcceptor(){

        }

        @Override
        public void run() {
            try {
                ssock = new ServerSocket(port);
            } catch (IOException e) {
                e.printStackTrace();
            }



            WifiManager wm = (WifiManager) getSystemService(Context.WIFI_SERVICE);
            WifiInfo wi = wm.getConnectionInfo();
            int ip = wi.getIpAddress();
            final String ipstr = (ip & 0xff) + "." + ((ip >> 8) & 0xff) + "." + ((ip >> 16) & 0xff) + "." + ((ip >> 24) & 0xff) + ":" + ssock.getLocalPort();


            runOnUiThread(new Runnable() {
                @Override
                public void run() { // changes the ip address shown
                    TextView tv = (TextView) findViewById(R.id.ip);
                    tv.setText(ipstr);

                }
            });

            System.out.println("Set ip: " + ipstr);
            while(true)
            {
                try {
                    Socket s = ssock.accept();
                    System.out.println("sendbuffersize:" + s.getSendBufferSize());
                    System.out.println("recvbuffersize:" + s.getReceiveBufferSize());
                    s.setReceiveBufferSize(1000000);
                    s.setSendBufferSize(1000000);
                    SingleServer serv = new SingleServer(s);
                    new Thread(serv).start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    final String css = "<style type=\"text/css\">" +
            "div.dir {padding: 1px;" +
            "   background-color: orange;" +
            "   }\n" +
            "div.file {padding:1px;" +
            "   background-color: lightgray;" +
            "   }" +
            "</style>";

    final String headstyle = "border-radius: 12px;"
            + "background-clip: padding-box;"
            + "width: 90%;"
            + "padding: 10px;"
            + "background-color: gray";
    final String header = "<html><head>" + css + "</head><body>"
            + "<h1 style=\"" + headstyle + "\">FileServer"
            + "<form method=\"post\" action=\"upload.html?\" enctype=\"multipart/form-data\">"
            + "<input type=\"file\" name=\"upfile\" />"
            + "<input type=\"submit\" value=\"Send\" />"
            + "</form>"
            + "</h1>"
            + "<div id=\"fileListContainer\" style=\"width:90%\">";

    final String footer = "</div></body></html>";

    public class SingleServer implements Runnable {

        Socket sock;

        SingleServer(Socket s) {
            sock = s;
        }

        private class PostInfo{
            boolean isPost = false;
            String boundary = "";
            int length = 0;
            PostInfo(boolean post, String b, int l){
                isPost = post;
                boundary = b;
                length = l;
            }
        }

        @Override
        public void run() {
            DefaultHttpServerConnection conn = new DefaultHttpServerConnection();
            try {
                conn.bind(sock, new BasicHttpParams());
                HttpRequest hr = conn.receiveRequestHeader();
                String method = hr.getRequestLine().getMethod();
                String uri = hr.getRequestLine().getUri();

                System.out.println("method_" + method);
                System.out.println("uri_" + uri);

                BasicHttpEntityEnclosingRequest container = new BasicHttpEntityEnclosingRequest(hr.getRequestLine());
                conn.receiveRequestEntity(container);

                System.out.println("\nHeaders:");


                String boundary = null;
                int length = 0;
                for (Header header : hr.getAllHeaders()) {
                    System.out.println(header.getName() + ":" + header.getValue());
                    if(header.getName().toLowerCase().contains("content-type") && header.getValue().toLowerCase().contains("boundary="))
                        boundary = header.getValue().substring(header.getValue().indexOf("boundary=") + "boundary=".length());
                    else if(header.getName().toLowerCase().contains("content-length"))
                        length = Integer.decode(header.getValue());
                }

                System.out.println("\nHeaders Done");

//                InputStream isS = sock.getInputStream();
//                while(isS.available() > 0)
//                    System.out.print(isS.read());
                HttpEntity he = container.getEntity();

                final int READ_BUFFER_SIZE = 512*1024; // 0.5 MB
                BufferedInputStream is = new BufferedInputStream(he.getContent(), READ_BUFFER_SIZE);


                System.out.println("\n responding");

                BufferedOutputStream os = new BufferedOutputStream(sock.getOutputStream(), 100000);
                PostInfo pi = new PostInfo(hr.getRequestLine().getMethod().equalsIgnoreCase("post"), boundary, length);
                writeResponse(is, os, conn, pi, hr);



                os.close();


            } catch (IOException e) {
                System.out.println("Singleserver failed to bind");
                e.printStackTrace();
            } catch (HttpException e) {
                e.printStackTrace();
            }
        }

        private void writeResponse(BufferedInputStream is, BufferedOutputStream os, DefaultHttpServerConnection conn, PostInfo pi, HttpRequest hr) throws IOException {

            String uri = hr.getRequestLine().getUri();

            //c/p start
            if(uri.startsWith("/")) // ...always will
            {




                StringBuilder dir;

                // read directory up to and including last /
                dir = new StringBuilder(uri.substring(0, uri.lastIndexOf('/') + 1));


                System.out.println("dir preproc = " + dir.toString());

                if(!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
                {
                    System.out.println("Can't open external storage");
                    return;
                }

                File exStorage = Environment.getExternalStorageDirectory();
                System.out.println(exStorage.getPath());
                if(!dir.toString().contains(exStorage.getPath()))
                    dir.insert(0,exStorage.getPath());

                File d;
                d = new File(dir.toString().replace("+", " ")); // strip spaces from url and change to +
                System.out.println("directory = " + d.getAbsolutePath());

                //handle uploading of files
                if(pi.isPost && uri.endsWith("upload.html?"))
                {
                    sendContinue(conn, hr);

                    System.out.println("body:");



                    try {
                        newProcessFile(pi, is, d);
//                        completeWrite(fs);
//                        fs.close();
                        is.close();
                    } catch (IOException ex) {
                        System.out.println("error writing or creating file");
                        ex.printStackTrace();
                    }

//                    System.out.println();

                }
                else if(!uri.endsWith("/") && !uri.contains("favicon.ico")) // download file at url
                {
                    System.out.println("in download part");
                    String uriEnd = uri.substring(uri.lastIndexOf('/')+1);
                    uriEnd = uriEnd.replaceAll("%20", " ");
                    File desiredFile = new File(d, uriEnd);

                    System.out.println("file created: " + desiredFile.getAbsolutePath());
                    if(desiredFile.canRead()) // serve it
                    {
                        sendFileDownloadHeader(conn, hr, desiredFile);
                        BufferedInputStream fis = null;
                        System.out.println("file exists");
                        try {
                            fis = new BufferedInputStream(new FileInputStream(desiredFile), 100000);
                        } catch(IOException ex){
                            ex.printStackTrace();
                        }

                        byte[] buf = new byte[100000];
                        int bytesRead = 0;
                        int bytesGiven = 0;
                        bytesRead = fis.read(buf);

                        while(bytesRead >= 0)
                        {
                            os.write(buf, 0, bytesRead);
                            bytesGiven+= bytesRead;
                            bytesRead = fis.read(buf);
                        }
                        fis.close();
                        os.close();
                    }
                }
                if(uri.endsWith("/") || pi.isPost) // if not downloading, show directory
                {
                    if(!pi.isPost) // send ok if not post, because post already sent headers.
                        sendOk(conn, hr);

                    try {
                        os.write(header.getBytes());
                        System.out.println(d.getPath());
                        System.out.println(d.exists());

                        for(File f : d.listFiles())
                        {
//                            System.out.println(f.getPath());
                            //                        System.out.println(f.getName());
                            if(f.isDirectory()) // show each directory first
                            {

                                os.write("<div class=\"dir\"><a href=\"".getBytes());
                                String fp = f.getPath() + "/";
                                fp = fp.replaceAll(" ", "+");
                                os.write(fp.getBytes());
                                os.write("\" />".getBytes());
                                os.write(f.getName().getBytes());
                                os.write(" -> </a></div><br />".getBytes());

                            }
                        }
                        for(File f : d.listFiles())
                        {
                            if(!f.isDirectory()) // then show each non-directory file, and its size
                            {
                                String fpath = f.toURI().getPath();
//                                System.out.println(fpath);
                                while(fpath.indexOf(' ') < fpath.lastIndexOf('/') && fpath.indexOf(' ') != -1) // replace all spaces in directory, but not in files
                                    fpath = fpath.replace(' ', '+');
                                os.write("<div class=\"file\" ><a target=\"_blank\" href=\"".getBytes());
                                os.write(fpath.getBytes());
                                os.write("\" />".getBytes());
                                os.write(f.getName().getBytes());
                                os.write("</a><span style=\"float:right;\">".getBytes());
                                os.write(String.valueOf(f.length()).getBytes());
                                os.write(" bytes</span></div><br />".getBytes());
                            }
                        }

                        os.write(footer.getBytes());
                        os.close();
                        sock.close();
                    } catch (IOException ex) {
                        System.out.println("couldn't write html file back to requester");
                        ex.printStackTrace();
                    }
                }



            }
            else
            {
                try {
                    conn.sendResponseHeader(new BasicHttpResponse(hr.getProtocolVersion(), HttpStatus.SC_NOT_FOUND, null));
                    os.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                } catch (HttpException e) {
                    e.printStackTrace();
                }
            }
            //c/p end
        }

        private void newProcessFile(PostInfo pi, BufferedInputStream is, File dir) throws IOException {
            BufferedOutputStream fs = null;
            String fileName = "";

            int x = 0;

            boolean fileCopying = false;
            boolean endOfLine = false;

            int newLines = 0;
//            StringBuilder line = new StringBuilder("");

            final int LIMIT = 1460 * 10; // usual max packet size for tcp over ip, times 10
            byte[] xArr = new byte[LIMIT];
//            byte[] bArr = new byte[LIMIT];
            char[] lineArr = new char[LIMIT];
            int pos = 0;
            System.out.println(pi.length);
            int bytesProcessed = 0;
            int bytesCompleted = 0;
            int bytesAdded = 0;
            int prevInArray = 0;
            int preambleSize = 0;
            while(bytesProcessed < pi.length)
            {
                if(!fileCopying)
                {
                    x = is.read();
                    System.out.print((char)x);
                    bytesProcessed++;
                    bytesCompleted++;
                    preambleSize++;
//                    bArr[pos] = (byte)x;
                    lineArr[pos++] = (char) x;

                    if(x == '\n')
                        endOfLine = true;
                    else
                    {
                        if(pos == LIMIT) // make sure we dont keep adding to charArr
                            endOfLine = true;
                    }

                    if(endOfLine) // if not copying yet, look for file name.
                    {
//                                String lStr = line.toString();
                        String lStr = new String(lineArr);

//                                    System.out.println(lStr);
                        if(lStr.contains("filename=\""))
                        {
                            // new string starts where (filename=") ends
                            String subStr = lStr.substring(lStr.indexOf("filename=\"") + "filename=\"".length());
                            // filename is name from 0 to
                            fileName = subStr.substring(0, subStr.indexOf('"'));

                            System.out.println("filename get:" + fileName);
                            fs = new BufferedOutputStream(new FileOutputStream(new File(dir, fileName)));
                        }
                        if (++newLines == 4)
                        {
                            fileCopying = true;
                            System.out.println("Starting file copy");
                            newLines++;
                        }
                        pos = 0;
                        endOfLine = false;
                    }
                }
                else
                {
                    int validInArray;
                    // want to read x bytes, where x is the number of available bytes in xArr.
                    // the first writtenInArray bytes are taken by previous read
                    try {
                        bytesAdded = is.read(xArr, prevInArray, xArr.length - prevInArray);
                        validInArray = prevInArray + bytesAdded;
                        bytesProcessed += bytesAdded;
                    } catch(ArrayIndexOutOfBoundsException ai){
                        System.out.println("l:" + xArr.length + " w:" + prevInArray);
                        throw ai;
                    }
//                    System.out.println("bA:" + bytesAdded + "  bP:" + bytesProcessed);
                    // make new string only out of the array part written
                    String xStr = new String(xArr, 0, validInArray); // index is to limit string to what we just made in array.



                    if(xStr.contains(pi.boundary)) // if boundary found, write everything before last newline prior to boundary
                    {
//                        in xStr, from 0 to boundary
//                        in that string, find index of last newline, and write that many bytes.
//                        ie;if newline is at index 2, write 2 bytes (0-1)
//                        System.out.println(xStr);
                        int boundaryIndex = xStr.indexOf(pi.boundary);
                        int pivotIndex = xStr.substring(0, boundaryIndex)
                                       .lastIndexOf('\n'); // gets position of last newline before the boundary.

                        // get the number of bytes that should be disregarded. Add one for the newline itself
                        int disregardedBytes = xStr.length() - pivotIndex + 1;

                        // then subtract that amount from total byte array length. This accounts for discrepancies in length
                        // of each character in UTF-8 (android's default charset).
                        int endIndex = validInArray - disregardedBytes;

                        System.out.println("p:" + prevInArray + "  b:" + bytesAdded);
                        System.out.println("v:" + validInArray);
                        System.out.println("length:" + pi.length);
                        System.out.println("eI:" + (bytesProcessed - validInArray + endIndex - preambleSize));
                        System.out.print("ix_");
                        for(int ix = endIndex; ix < xStr.length(); ix++)
                            System.out.print(ix - endIndex + "" + xStr.charAt(ix));
//                        System.out.print("ix2_");
//                        for(int ix = pi.boundary.length(); ix > 0; ix--)
//                            System.out.print(ix + "" + (char)xArr[xArr.length - ix - 1]);
                        fs.write(xArr, 0, endIndex);
                        System.out.println("exit boundary found: length");
                        System.out.println(pi.boundary.length());
                        break;
                    }
                    else // if not, write everything up to the last newline, and shift string stuff over
                    {
                        int endIndex = xStr.lastIndexOf('\n');

//                        System.out.println(xStr);
//                            delayWrite(fs, bArr, 0, pos);
//                        System.out.println("e" + endIndex + " w" + prevInArray + " b" + bytesAdded);
                        if(endIndex == -1)
                            endIndex = validInArray; // set to write the whole string
                        else if(endIndex == 0) // write the newline if it's first character instead of breaking.
                            endIndex = 1;
                        fs.write(xArr, 0, endIndex);
                        bytesCompleted += endIndex;
//                        System.out.println("cmp:" + bytesCompleted);

                        //whole string size - written string size = amount left over.
                        // copy these elements to the start of the array.
                        prevInArray = validInArray - endIndex; //
                        System.arraycopy(xArr, endIndex, xArr, 0, prevInArray);
                    }
                }
            }
            fs.close();
        }

        private void processFile(PostInfo pi, BufferedInputStream is) throws IOException {
            BufferedOutputStream fs = null;
            String fileName = "";

            int x = 0;

            boolean fileCopying = false;
            boolean endOfLine = false;

            int newLines = 0;
            StringBuilder line = new StringBuilder("");

            final int LIMIT = 1000;
            byte[] xArr = new byte[LIMIT / 10];
            byte[] bArr = new byte[LIMIT];
            int pos = 0;
            System.out.println(pi.length);
            int i = 0;
            while(i < pi.length)
            {
                x = is.read();

//                            System.out.print((char)x);
                line.append((char)x);
                bArr[pos++] = (byte)x;
//                                System.out.print("." + (char)x);
//                                System.out.println("=" + x);

                if(x == '\n')
                    endOfLine = true;
                else
                {
//                                if(line.length() == LIMIT) // make sure we dont keep adding to stringbuilder
                    if(pos == LIMIT) // make sure we dont keep adding to charArr
                        endOfLine = true;
                }

                if(!fileCopying && endOfLine) // if not copying yet, look for file name.
                {
//                                String lStr = line.toString();
                    String lStr = line.toString();

//                                    System.out.println(lStr);
                    if(lStr.contains("filename=\""))
                    {
                        // new string starts where (filename=") ends
                        String subStr = lStr.substring(lStr.indexOf("filename=\"") + "filename=\"".length());
                        // filename is name from 0 to
                        fileName = subStr.substring(0, subStr.indexOf('"'));

                        System.out.println("filename get:" + fileName);
//                        fs = new BufferedOutputStream(new FileOutputStream(new File(d, fileName)));
                    }
                }
                else if(endOfLine) // if copying, check if boundary is here, if not copy.
                {
                    String lStr = line.toString();
                    if(lStr.contains(pi.boundary))
                    {
                        System.out.println("exit boundary found");
                        break;
                    }
                    else
                    {


//                                    delayWrite(fs, bArr, 0, pos);
                        fs.write(bArr, 0, pos);
                   }
                }

                if(endOfLine)
                {
//                                line.delete(0, line.length()); // empty the line
                    ++newLines;
                    if (newLines == 4)
                    {
                        fileCopying = true;
                        System.out.println("Starting file copy");
                        newLines++;
                    }
                    pos = 0;
                    endOfLine = false;
                }
            }
        }

        private void sendFileDownloadHeader(DefaultHttpServerConnection conn, HttpRequest hr, File desiredFile) {
            BasicHttpResponse response = new BasicHttpResponse(hr.getProtocolVersion(), HttpStatus.SC_OK, null);
            response.addHeader("content-disposition", "attachment; filename=" + desiredFile.getName());
            System.out.println("content-disposition" + "attachment; filename=" + desiredFile.getName());
            try {
                conn.sendResponseHeader(response);
            } catch (HttpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void sendOk(DefaultHttpServerConnection conn, HttpRequest hr) {
            try {
                conn.sendResponseHeader(new BasicHttpResponse(hr.getProtocolVersion(), HttpStatus.SC_ACCEPTED, null));
            } catch (IOException ex) {
                System.out.println("http headers couldn't send");
            } catch (HttpException e) {
                e.printStackTrace();
            }
        }

        private void sendContinue(DefaultHttpServerConnection conn, HttpRequest hr) {
            try {
                conn.sendResponseHeader(new BasicHttpResponse(hr.getProtocolVersion(), HttpStatus.SC_CONTINUE, null));
            } catch (IOException ex) {
                System.out.println("http headers couldn't send");
            } catch (HttpException e) {
                e.printStackTrace();
            }
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
