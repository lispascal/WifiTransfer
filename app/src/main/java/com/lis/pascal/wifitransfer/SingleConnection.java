package com.lis.pascal.wifitransfer;

import android.net.Uri;
import android.os.Environment;
import android.widget.CheckBox;

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
import java.net.Socket;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

/**
* Created by lispascal on 2/15/2015.
*/
public class SingleConnection implements Runnable, AutoCloseable {

    private MainActivity mainActivity;
    private ConnectionAcceptor parent;
    Socket sock;
    String url; // in case Absolute URL is needed
    private boolean auth;

    SingleConnection(MainActivity mainActivity, ConnectionAcceptor connectionAcceptor, Socket s, String ipstr, boolean authorized) {
        this.mainActivity = mainActivity;
        parent = connectionAcceptor;
        sock = s;
        url = ipstr;
        auth = authorized;
    }

    @Override
    public void close() throws Exception {
        sock.shutdownInput();
        sock.shutdownOutput();
        sock.close();
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
            final int WRITE_BUFFER_SIZE = 1024*1024; // 1 MB
            BufferedOutputStream os = new BufferedOutputStream(sock.getOutputStream(), WRITE_BUFFER_SIZE);
            PostInfo pi = new PostInfo(hr.getRequestLine().getMethod().equalsIgnoreCase("post"), boundary, length);
            processRequest(is, os, conn, pi, hr);

            os.close();
            sock.close();


        } catch (IOException e) {
            System.out.println("Singleserver failed to bind");
            e.printStackTrace();
        } catch (HttpException e) {
            e.printStackTrace();
        }

        // after page is served remove this conn from list.
        parent.removeConn(this);

    }


    final String js = "<script>" + "\n" +
            "function rename(element, dir) {" + "\n" +
            "   oldName = element.parentElement.getAttribute(\"name\");" + "\n" +
//            "   oldName = oldName.substr(0, oldName.length);" + "\n" + // for some reason a space is at the end
            "   var result = prompt(\"New File Name?\", oldName);" + "\n" +

            // if they didn't hit cancel & name is different, create form then submit it
            "   if(result != null && result != oldName)" +  "\n" +
            "   {" + "\n" +
            "       var form = document.createElement(\"form\");" + "\n" +
            "       form.setAttribute(\"method\", \"POST\");" + "\n" +
            // path
            "       form.setAttribute(\"action\", \"rename.html?path=\" + dir);" + "\n" +
            "       form.setAttribute(\"accept-charset\", \"UTF-8\");" + "\n" +
            "       form.setAttribute(\"enctype\", \"multipart/form-data\");" + "\n" +

    // first argument in post
            "       var inp1 = document.createElement(\"input\");" + "\n" +
            "       inp1.setAttribute(\"type\", \"hidden\");" + "\n" +
            "       inp1.setAttribute(\"name\", \"oldName\");" + "\n" +
            "       inp1.setAttribute(\"value\", oldName);" + "\n" +
            "       form.appendChild(inp1);" + "\n" +
            // second argument in post
            "       var inp2 = document.createElement(\"input\");" + "\n" +
            "       inp2.setAttribute(\"type\", \"hidden\");" + "\n" +
            "       inp2.setAttribute(\"name\", \"newName\");" + "\n" +
            "       inp2.setAttribute(\"value\", result);" + "\n" +
            "       form.appendChild(inp2);" + "\n" +
            // submit form
            "       document.body.appendChild(form);" + "\n" +
            "       form.submit();" +
            "    }" + "\n" +
            "}" + "\n" +
            "</script>";

    final String css = "<style type=\"text/css\">" +
            "h1 {border-radius: 12px;" +
            "background-clip: padding-box;" +
            "width: 95%;" +
            "padding: 10px;" +
            "background-color: gray;" +
            "   }\n" +
            "#bodyContainer {width: 100%;" +
            "   height: 100%;" +
            "   background-color: white" +
            "   }\n" +
            "#contentContainer {width:95%;" +
//            "   background-clip: padding-box;" +
            "   }\n" +
            "#navigationContainer {width:15%;" +
            "   border-radius: 12px;" +
            "   padding: 4px;" +
            "   background-color: lightgray;" +
            "   vertical-align: top;" +
            "   }\n" +
            "#directoryContainer {width:85%;" +
            "   padding: 4px;" +
            "   }\n" +

            // list sort styling
            "div.list {border: 1px solid black;" +
            "   }\n" +
            "div.dir.list {background-color: orange;" +
            "   }\n" +
            "div.list img, div.list input {margin-left: 1px;" +
            "   height:1em;width:1em;" +
            "   cursor: pointer; " +
            "   border: 1px solid black;" +
            "   }\n" +
            "div.file.list>form {display: inline;" +
            "   }\n" +
            "div.file.list span.info {float:right;" +
            "   width:15%;" +
            "   text-align:right" +
            "   }\n" +

            "div.icon {width: 100px;" +
            "   height:100px;" +
            "   display:inline-block;" + // not supported in a very few browsers
            "   background-image: url(\"wf_images/unknownIcon.gif\");" +
            "   }\n" +
            "div.icon.dir {background-image: url(\"wf_images/folderIcon.gif\");" +
            "   }\n" +
            "div.icon img, div.icon input, div.file.icon>form, div.file.icon span.info {" +
            "   display: none;" +
            "   }\n" +



            "#tableHead {" +
            "   cursor: pointer;" +
            "   border: 1px solid black" +
            "   }\n" +
            "#sortByNameSpan {" +
            "   width: 70%" +
            "   }\n" +
            "#sortByDateSpan {" +
            "   float:right;" +
            "   width: 15%" +
            "   }\n" +
            "#sortBySizeSpan {" +
            "   float:right;" +
            "   width: 15%" +
            "   }\n" +
            "</style>\n";

    final String header_start = "<html><head>";
    final String header_end = "</head><body><div id=\"bodyContainer\">";
    final String upload_form_start = "<h1>FileServer"
            + "<form method=\"post\" action=\"";

    final String upload_form_end = "\" enctype=\"multipart/form-data\">"
            + "<input type=\"file\" name=\"upfile\" />"
            + "<input type=\"submit\" value=\"Send\" />"
            + "</form>"
            + "<button onclick=\"changeLayout()\">Change Layout</button>"
            + "</h1>";
    final String container_start = "<table id=\"contentContainer\"><tbody><tr>";
    final String navigation_start = "<td id=\"navigationContainer\">";
    final String navigation_end = "</td>";

    final String directory_start= "<td id=\"directoryContainer\">";
    final String table_head = "<div id=\"tableHead\">"
            + "<span id=\"sortByNameSpan\" onclick=\"sortByName()\">Name</span>"
            + "<span id=\"sortBySizeSpan\" onclick=\"sortBySize()\">Size</span>"
            + "<span id=\"sortByDateSpan\" onclick=\"sortByDate()\">Last Modified</span>"
            + "</div>";
    final String directory_end = "</td>";
    final String container_end = "</tr></tbody></table></div></body></html>";

    final String login_markup_start = "<form action=\"";
    // insert url
    final String login_markup_end = "\" enctype=\"multipart/form-data\" method=\"post\">" +
            "Password: <input type=\"password\" name=password>" +
            "<input type=\"submit\" value=\"Submit\"" +
            "</form>";


    final String BASE_DIRECTORY = "/storage/emulated/0";

    private void processRequest(BufferedInputStream is, BufferedOutputStream os, DefaultHttpServerConnection conn, PostInfo pi, HttpRequest hr) throws IOException {

        String uriStr = hr.getRequestLine().getUri();

        Uri uriObj = Uri.parse(uriStr);
        System.out.println("uri: " + uriObj.getPath());
        String uri = uriObj.getPath();

        StringBuilder dir = new StringBuilder();
        if(uriObj.getQueryParameter("path") != null)
            dir.append(uriObj.getQueryParameter("path"));
        else
            dir.append(BASE_DIRECTORY);

        System.out.println("dir preproc = " + dir);

        if (!Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            System.out.println("Can't open external storage");
            return;
        }

        File exStorage = Environment.getExternalStorageDirectory();
        if (!dir.toString().contains(exStorage.getPath()))
            dir.insert(0, exStorage.getPath());



        File dirFile;
        dirFile = new File(dir.toString()); // convert spaces appropriately
        if(!dirFile.exists() || !dirFile.isDirectory()) // catch issues in the directory path
        {
            dir.replace(0,dir.length(), BASE_DIRECTORY); // replace it with BASE_DIRECTORY if invalid
            dirFile = new File(dir.toString());
        }
        System.out.println("directory = " + dirFile.getAbsolutePath());

        // when not auth and dir != base, serve login page instead
        if(!auth && !dirFile.getAbsolutePath().contentEquals(BASE_DIRECTORY) && !uri.equalsIgnoreCase("/login.html") && mainActivity.isPasswordRequired())
        {
            sendOk(conn, hr);
            serveLoginPage(dirFile, os);
            return;
        }

        //handle uploading of files
        if (pi.isPost && uri.equalsIgnoreCase("/upload.html"))
        {
            sendContinue(conn, hr); // change fileUpload() later to break connection as http should if no perms
            fileUpload(pi, is, dirFile);
            try {
                is.close();
            } catch (IOException ex) {
                System.out.println("error writing or creating file");
                ex.printStackTrace();
            }
        } else if (uri.equalsIgnoreCase("/delete.html")) // if going to delete
        {
            String fileName = uriObj.getQueryParameter("file");
            if(fileName != null)
                deleteFile(dirFile, fileName);
            sendOk(conn, hr);
        } else if (pi.isPost && uri.equalsIgnoreCase("/rename.html")) // if going to rename
        {
            fileRename(dirFile, is, pi);
            sendOk(conn, hr);
        } else if (pi.isPost && uri.equalsIgnoreCase("/login.html")) // if going to rename
        {
            if(login(is, pi)) {
                sendOk(conn, hr);
            }
            else // login failed, serve login page again.
            {
                // if auth still required, serve login
                if(mainActivity.isPasswordRequired()) {
                    sendOk(conn, hr);
                    serveLoginPage(dirFile, os);
                    return;
                }
                else // otherwise, it should act as if there was never a login page. Doesn't auth user but doesn't require it
                    sendOk(conn, hr); // a very niche case
            }



        } else if (uri.contains("wf_images/") || uri.endsWith("favicon.ico")) // if a website image
        {
            String uriEnd = uri.substring(uri.lastIndexOf('/') + 1);
            uriEnd = uriEnd.replace("%20", " ");
            sendOk(conn, hr);
            serveAsset(uriEnd, os);
        } else if (uri.contains("/download/")) // download file at url
        {
            String fileName = uriObj.getQueryParameter("file");
            if(fileName != null) {
                serveFile(fileName, dirFile, conn, hr, os);
            }
        }

//        if (uri.endsWith("/") || pi.isPost) // if not downloading, show directory.
        if (uri.equalsIgnoreCase("/index.html") || uri.equalsIgnoreCase("/upload.html") ||
                uri.equalsIgnoreCase("/delete.html") || uri.equalsIgnoreCase("/rename.html") ||
                uri.equalsIgnoreCase("/") || pi.isPost) // if not downloading, show directory.
        {
            if (!pi.isPost) // send ok if not post, because post already sent headers.
                sendOk(conn, hr);
            servePage(dirFile, os);
        }
    }

    synchronized private boolean login(BufferedInputStream is, PostInfo pi) {
        HashMap<String, String> i = getArgs(is, pi);
        if(parent.authUser(this, i.get("password"))) // if login successful
        {
            auth = true;
            mainActivity.makeToast("User authorized: " + sock.getInetAddress(), false);
            return true;
        }
        else {
            mainActivity.makeToast("User failed to authorize: " + sock.getInetAddress(), false);
            return false;
        }
    }

    // to be called from settings activity on checkbox switch on "de-authenticate users"
    synchronized void logout() {
        auth = false;
    }

    private void serveLoginPage(File d, BufferedOutputStream os) {
        try {
            os.write(header_start.getBytes());
            String title = "<title>Log in</title>\n";
            os.write(title.getBytes());
//            os.write(css.getBytes());
//            os.write(js.getBytes());
            os.write(header_end.getBytes());
            os.write(container_start.getBytes());
            os.write(login_markup_start.getBytes());
            os.write(getLoginUrl(d).getBytes());
            os.write(login_markup_end.getBytes());
            os.write(container_end.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    private void servePage(File d, BufferedOutputStream os) {
        try {
            os.write(header_start.getBytes());
            String title = "<title>" + d.getPath() + "</title>\n";
            os.write(title.getBytes());
            os.write(css.getBytes());
            os.write(js.getBytes());
            os.write(("<script>" + mainActivity.getAppContext().getResources().getString(R.string.sortFunctions) + "</script>").getBytes());
            os.write(("<script>" + mainActivity.getAppContext().getResources().getString(R.string.changeLayoutFunction) + "</script>").getBytes());
            os.write(header_end.getBytes());
            os.write(upload_form_start.getBytes());
            os.write(getUploadUrl(d).getBytes());
            os.write(upload_form_end.getBytes());
            os.write(container_start.getBytes());
            os.write(navigation_start.getBytes());
            displayNavigationTree(d, os);
            os.write(navigation_end.getBytes());
            os.write(directory_start.getBytes());
            os.write(table_head.getBytes());
            displayFolder(d, os);
            os.write(directory_end.getBytes());
            os.write(container_end.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }


    }


    private HashMap<String, String> getArgs(BufferedInputStream is, PostInfo pi)
    {
        if(pi.length > 3000) // if so, something is wrong because the post info is far bigger than expected
            return null;
        byte[] byteArr = new byte[pi.length];
        int count = 0;
        while (count < pi.length) {
            try {
                count += is.read(byteArr);
            } catch (IOException e) {
                System.out.println("could not read from http header");
                e.printStackTrace();
                return null;
            }
        }

        String argStr = new String(byteArr, Charset.forName("UTF-8"));
        System.out.println("argstr:\n" + argStr);
        System.out.println("boundary:\n" + pi.boundary);

        return parseFormData(argStr, pi.boundary);


    }
    private void fileRename(File d, BufferedInputStream is, PostInfo pi) throws IOException {
        HashMap<String, String> hm = getArgs(is, pi);
        if (hm == null) {
            System.out.println("rename failed");
            return;
        }

        System.out.println("hashmap:");
        for (Map.Entry<String, String> ent : hm.entrySet()) {
            System.out.println(ent.getKey() + " : " + ent.getValue());
        }

        String oldName = hm.get("oldName");
        String newName = hm.get("newName");

        // get two arguments out of uriEnd

        CheckBox cb = (CheckBox) mainActivity.findViewById(R.id.checkbox_rename);

        if (cb.isChecked()) {
            if (oldName != null && newName != null && !newName.equalsIgnoreCase("")) {
                File from = new File(d, oldName);
                File to = new File(d, newName);
                System.out.println("oldFile exists?:" + from.exists());
                System.out.println("newFile exists?:" + to.exists());

                boolean success = from.renameTo(to);
                if (success) {
                    System.out.println("File " + oldName + " renamed to " + newName);
                    mainActivity.makeToast("File " + oldName + " renamed to " + newName, true);
                } else {
                    System.out.println("rename failed");
                    System.out.print("oldFile:" + from.getAbsolutePath() + "newFile:" + to.getAbsolutePath() + "");

                }
            }

        }
    }

    private void deleteFile(File d, String fileName) {
        CheckBox cb = (CheckBox) mainActivity.findViewById(R.id.checkbox_deletion);
        if(cb.isChecked()) {
            File desiredFile = new File(d, fileName);
            if(desiredFile.delete()) {
                System.out.println("File " + fileName + " deleted");
                mainActivity.makeToast("File deleted: " + fileName, true);
                return;
            }
        }
        mainActivity.makeToast("Delete attempted by " + sock.getInetAddress() + ", and failed", false);

    }

    private HashMap<String, String> parseFormData(String argStr, String boundary) {
        HashMap<String, String> result = new HashMap<>();
        while(argStr.contains(boundary) && argStr.contains("name=\""))
        {
            int nameStart = argStr.indexOf("name=\"");
            if(nameStart == -1)
                break;
            nameStart += "name=\"".length();
            int nameEnd = argStr.substring(nameStart).indexOf('"'); // relative to namestart
            if(nameEnd == -1)
                break;
            nameEnd += nameStart;
            String name = argStr.substring(nameStart, nameEnd);


            int lineStart = argStr.substring(nameEnd).indexOf('\n'); // relative to nameEnd
            if(lineStart == -1)
                break;

            lineStart += nameEnd + 1; // +1 skips over the newline itself
            lineStart += argStr.substring(lineStart).indexOf('\n') + 1; // skip over a second newline. since request puts two

            int lineEnd = argStr.substring(lineStart).indexOf('\n'); // relative to linestart
            if(lineEnd == -1)
                break;
            lineEnd += lineStart;
            String line = argStr.substring(lineStart, lineEnd);
            if(line.endsWith("\r"))
                line = line.substring(0, line.length()-1); // strip a carriage return


            result.put(name, line);
            System.out.println("added:" + name + "=" + line);
            System.out.println("line ended with " + (int)line.charAt(line.length()-1));
            argStr = argStr.substring(lineEnd);
        }


        return result;
    }


    /**
     * Serves an image with name fileName to output. Used to load images and such.
     * @param fileName Name of file to output.
     * @param output Stream to which the file will be written.
     */
    private void serveAsset(String fileName, BufferedOutputStream output) {
        fileName = fileName.replace("%20", " ");
        BufferedInputStream fis;
        try {
            fis = new BufferedInputStream(mainActivity.getAssets().open(fileName));
            System.out.println("file exists");
        } catch (IOException e) {
            e.printStackTrace();
            return; // can't open file
        }


        byte[] buf = new byte[100000];
        int bytesRead = 0;
        try {
            bytesRead = fis.read(buf);
            while(bytesRead >= 0)
            {
                output.write(buf, 0, bytesRead);
                bytesRead = fis.read(buf);
            }
            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void serveFile(String fileName, File dir, DefaultHttpServerConnection conn, HttpRequest request, BufferedOutputStream output) {

        File desiredFile = new File(dir, fileName);

        System.out.println("file created: " + desiredFile.getAbsolutePath());


        CheckBox cb = (CheckBox) mainActivity.findViewById(R.id.checkbox_download);
        if (!cb.isChecked()) {
            mainActivity.makeToast("Download attempted by " + sock.getInetAddress() + ", and failed", false);
            sendOk(conn, request);
            return;
        }
        if(desiredFile.canRead()) // serve it
        {
            sendFileDownloadHeader(conn, request, desiredFile);
            BufferedInputStream fis = null;
            System.out.println("file exists");
            try {
                fis = new BufferedInputStream(new FileInputStream(desiredFile), 100000);
            } catch(IOException ex){
                ex.printStackTrace();
                return; // kill it early if for some reason can't read the file.
            }

            byte[] buf = new byte[100000];
            int bytesRead = 0;
            try {
                bytesRead = fis.read(buf);
                while(bytesRead >= 0)
                {
                    output.write(buf, 0, bytesRead);
                    bytesRead = fis.read(buf);
                }
                fis.close();
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            mainActivity.makeToast("File sent: " + fileName, false);

        }
    }

    /**
     * Outputs the html document part displaying the folders and files in directory d.
     * @param d - Directory to look at all the files in.
     * @param os - Stream to output the document part to.
     */
    private void displayFolder(File d, BufferedOutputStream os) {
        try {


            System.out.println(d.getPath());
            System.out.println(d.exists());
//                first, if not in top directory, put a link for it.

            if(!d.getPath().equals(BASE_DIRECTORY)) // if not top directory, look in above directory.
            {
                os.write("<div class=\"dir list\"><img src=\"/wf_images/upfolder.gif\" /><a href=\"".getBytes());
                os.write(getDirectoryUrl(d.getParentFile()).getBytes());
                os.write("\" />".getBytes());
                os.write("<span>Parent Folder</span>".getBytes());
                os.write("</a></div>".getBytes());
            }


            for(File f : d.listFiles())
            {
//                            System.out.println(f.getPath());
                //                        System.out.println(f.getName());
                if(f.isDirectory()) // show each directory first
                {

                    os.write("<div class=\"dir list\"><a href=\"".getBytes());

                    os.write(getDirectoryUrl(f).getBytes());
                    os.write("\"><img src=\"/wf_images/openfolder.gif\"  title=\"Open Folder\" /><span>".getBytes());
                    os.write(f.getName().getBytes());
                    os.write("</span></a></div>".getBytes());

                }
            }

            GregorianCalendar currentTime = new GregorianCalendar();
            for(File f : d.listFiles())
            {
                if(!f.isDirectory()) // then show each non-directory file, and its size
                {
//                    String fpath = f.toURI().getPath();
////                                System.out.println(fpath);
//                    while(fpath.indexOf(' ') < fpath.lastIndexOf('/') && fpath.indexOf(' ') != -1) // replace all spaces in directory, but not in files
//                        fpath = fpath.replaceFirst(" ", "+");

                    String fname = f.getName();
                    os.write(("<div class=\"file list\" name=\"" + fname + "\">").getBytes());


                    // rename button
                    os.write("<img src=\"/wf_images/rename.gif\" alt=\"Rename file\" title=\"Rename file\" name=\"".getBytes());
                    os.write(fname.getBytes());
                    os.write(("\" onclick=\"rename(this, \'" + d.getPath() + "\')\" />").getBytes());
                        // results in onclick="rename(this, '<path>')" where <path> is the directory


                    // delete button
                    os.write("<form method=\"post\" action=\"".getBytes());
                    os.write(getDeleteUrl(f).getBytes());
                    os.write("\"><input type=\"image\" src=\"/wf_images/delete.gif\" alt=\"Delete file\" title=\"Delete file\" /></form></a>".getBytes());

                    //download button and link
                    os.write("<a target=\"_blank\" href=\"".getBytes());
                    os.write(getFileUrl(f).getBytes());
                    os.write("\"><img src=\"/wf_images/download.gif\" title=\"Download file (opens new tab)\" /><span>".getBytes());
                    os.write(fname.getBytes());
                    os.write("</span></a>".getBytes());

                    printFileSize(os, f);
                    printModifiedTime(os, currentTime, f);


                    os.write("</div>".getBytes());
                }
            }
        } catch (IOException ex) {
            System.out.println("couldn't write html file back to requester");
            ex.printStackTrace();
        }
    }

    /**
     * Prints an HTML <span> element containing how long ago file was modified. The
     * data-modified attribute contains the time in millis the file was modified, for use
     * in sorting.
     * @param os The output stream to which the element will be printed.
     * @param currentTime A GregorianCalendar representing the current time.
     * @param file The file about which the time modified will be retrieved.
     * @throws IOException
     */
    private void printModifiedTime(BufferedOutputStream os, GregorianCalendar currentTime, File file) throws IOException {
        String timeString;
        long millis = currentTime.getTimeInMillis() - file.lastModified();
        if(millis < 1000*60) // 1 minute
            timeString = String.valueOf(millis/1000) + " seconds ago";
        else if(millis < 1000*60*60) // 1 hour
            timeString = String.valueOf(millis/1000/60) + " minutes ago";
        else if(millis < 1000*60*60*24) // 1 day
            timeString = String.valueOf(millis/1000/60/60) + " hours ago";
        else // multiple days
            timeString = String.valueOf(millis/1000/60/60/24) + " days ago";
        os.write(("<span class=\"info\" data-modified=\"" + millis + "\">").getBytes());
        os.write(timeString.getBytes());
        os.write("</span>".getBytes());
    }

    /**
     * Prints size of a file to the outputstream as an HTML <span> element.
     * Sets the data-size attribute to the bytesize for sorting use.
     * @param os BufferedOutputStream to print to
     * @param f file of which to get the size
     * @throws IOException
     */
    private void printFileSize(BufferedOutputStream os, File f) throws IOException {
        os.write(("<span class=\"info\" data-size=\"" + String.valueOf(f.length()) + "\">").getBytes());
        os.write(String.valueOf(f.length()).getBytes());
        os.write(" bytes</span>".getBytes());
    }

    private String getLoginUrl(File dir) {
        String fp = "/login.html?path=" + Uri.encode(dir.getPath() + "/");
        return fp;
    }
    private String getDirectoryUrl(File dir) {
        String fp = "/index.html?path=" + Uri.encode(dir.getPath() + "/");
        return fp;
    }
    private String getUploadUrl(File dir) {
        String fp = "/upload.html?path=" + Uri.encode(dir.getPath() + "/");
        return fp;
    }
    private String getDeleteUrl(File dir) {
        String fp = "/delete.html?path=" + Uri.encode(dir.getParent() + "/") + "&file=" + Uri.encode(dir.getName());
        return fp;
    }
    private String getFileUrl(File f) {
        String fp = "/download/" + Uri.encode(f.getName()) + "?path=" + Uri.encode(f.getParent() + "/") + "&file=" + Uri.encode(f.getName());
        return fp;
    }


    /**
     * Writes a string of HTML representing the navigation tree of a directory
     * to a stream.
     * @param dir Directory of which the navigation tree will be calculated.
     * @param os Stream to write the HTML to.
     */
    private void displayNavigationTree(File dir, BufferedOutputStream os) throws IOException {
        File curr = dir;
        ArrayList<File> list = new ArrayList<>();

        list.add(new File(curr.getPath()));

        while(curr != null && !curr.getPath().contentEquals(BASE_DIRECTORY))
        {
            curr = curr.getParentFile();
            list.add(new File(curr.getPath()));
            System.out.println(curr.getPath());
        }

        for(int i = list.size()-1 ; i >= 0; i--) // start at highest parent string, then go down.
        {
            String name = list.get(i).getName();
            if(name.equals("0"))
                name = "Root";
            int spaces = list.size()-1 - i;
            os.write(("<div><a style=\"padding-left:" + spaces + "em\" " +
                    "href=\"" + getDirectoryUrl(list.get(i)) + "\">" +
                    name +
                    "</a></div>").getBytes());
        }
    }

    private void fileUpload(PostInfo pi, BufferedInputStream is, File dir) throws IOException {
        BufferedOutputStream fs = null;
        String fileName = "";

        CheckBox cb = (CheckBox) mainActivity.findViewById(R.id.checkbox_upload);
        if (!cb.isChecked()) {
            mainActivity.makeToast("File uploaded attempted and failed", false);
            return;
        }


        int x = 0;

        boolean fileCopying = false;
        boolean endOfLine = false;

        int newLines = 0;

        final int LIMIT = 1460 * 10; // usual max packet size for tcp over ip, times 10
        byte[] xArr = new byte[LIMIT];

        char[] lineArr = new char[LIMIT];
        int pos = 0;
        System.out.println(pi.length);
        int bytesProcessed = 0;
        int bytesAdded = 0;
        int prevInArray = 0;
        while(bytesProcessed < pi.length)
        {
            if(!fileCopying)
            {
                x = is.read();
                System.out.print((char)x);
                bytesProcessed++;

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
                    String lStr = new String(lineArr);

                    if(lStr.contains("filename=\""))
                    {
                        // new string starts where (filename=") ends
                        String subStr = lStr.substring(lStr.indexOf("filename=\"") + "filename=\"".length());
                        // filename is name from 0 to
                        fileName = subStr.substring(0, subStr.indexOf('"'));

                        System.out.println("filename get:" + fileName);
                        fs = new BufferedOutputStream(new FileOutputStream(new File(dir, fileName)));

                        if(pi.length > 1024*1024)   // if file bigger than 1MB, display that it started uploading.
                                                    // Otherwise, it will see the "received" message quickly anyway
                            mainActivity.makeToast("receiving file: " + fileName, false);
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
                // make new string only out of the array part written
                String xStr = new String(xArr, 0, validInArray, Charset.forName("US-ASCII"));


                if(xStr.contains(pi.boundary)) // if boundary found, write everything before last newline prior to boundary
                {
                    // gets spot of last newline. if browser is from windows, httprequest might have carriage return
                    // not sure if other OSes will though. Kind of dangerous, but remove last char if it is carriage return
                    // safer fix might be to check the client's OS via User-Agent header and condition behavior on that
                    int boundaryIndex = xStr.indexOf(pi.boundary);
                    int endIndex = xStr.substring(0, boundaryIndex)
                                   .lastIndexOf('\n'); // gets position of last newline before the boundary.

                    if(xStr.charAt(endIndex-1) == '\r') // if carriage return
                        --endIndex;

                    fs.write(xArr, 0, endIndex);
                    System.out.println("exit boundary found: length");
                    System.out.println(pi.boundary.length());
                    break;
                }
                else // if not, write everything up to the last newline, and shift string stuff over
                {
                    int endIndex = xStr.lastIndexOf('\n');

                    if(endIndex == -1)
                        endIndex = validInArray; // set to write the whole string
                    else if(endIndex == 0) // write the newline if it's first character instead of breaking.
                        endIndex = 1;
                    fs.write(xArr, 0, endIndex);

                    //whole string size - written string size = amount left over.
                    // copy these elements to the start of the array.
                    prevInArray = validInArray - endIndex; //
                    System.arraycopy(xArr, endIndex, xArr, 0, prevInArray);
                }
            }
        }

        mainActivity.makeToast("File received: " + fileName, false);

        fs.close();

    }

    /**
     * Sends the Http header for a file download.
     * @param conn The connection the download header will be sent to.
     * @param hr The request that this header will be responding to.
     *           Used for the purposes of giving a ProtocolVersion
     * @param desiredFile The file that is being downloaded, about which name is needed.
     */
    private void sendFileDownloadHeader(DefaultHttpServerConnection conn, HttpRequest hr, File desiredFile) {
        BasicHttpResponse response = new BasicHttpResponse(hr.getProtocolVersion(), HttpStatus.SC_OK, null);
        response.addHeader("Content-Disposition", "attachment; filename=" + desiredFile.getName());
        try {
            conn.sendResponseHeader(response);
        } catch (HttpException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends OK (200) Http response code on a connection.
     * @param conn The connection the header will be sent to.
     * @param hr The request that this header will be responding to.
     *           Used for the purposes of giving a ProtocolVersion
     */
    private void sendOk(DefaultHttpServerConnection conn, HttpRequest hr) {
        try {
            conn.sendResponseHeader(new BasicHttpResponse(hr.getProtocolVersion(), HttpStatus.SC_ACCEPTED, null));
        } catch (IOException ex) {
            System.out.println("http headers couldn't send");
        } catch (HttpException e) {
            e.printStackTrace();
        }
    }
    /**
     * Sends Continue (100) Http status code on a connection.
     * Used when uploading files, to tell the client to send the file data.
     * @param conn The connection the header will be sent to.
     * @param hr The request that this header will be responding to.
     *           Used for the purposes of giving a ProtocolVersion.
     */
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
