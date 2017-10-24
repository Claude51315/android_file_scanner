package com.example.esslab.snapshot2;

import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;

import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import android.os.Handler;
import android.os.Message;
import java.util.Set;
import java.util.Collections;

import org.w3c.dom.Text;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

public class MainActivity extends AppCompatActivity {
    int click;
    File[] ls;
    private Handler updateHandler, updateCount;
    Message tmpMsg;
    String processingStr ;
    int filecount = 0;
    Set<String> visited_dir;
    Thread t1,t2;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        final TextView textObj = (TextView) findViewById(R.id.hello);
        final TextView textCount = (TextView) findViewById(R.id.fileCount);
        final Button startButton = (Button) findViewById(R.id.startButton);

        visited_dir = Collections.emptySet();
        click = 0;
        setSupportActionBar(toolbar);

        String storage_path = Environment.getExternalStorageDirectory().toString()+"/";


        Log.d("Files", "storage Path: " + storage_path);

        final File storage_directory = new File(storage_path);

        updateHandler = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                String msgStr = (String) msg.obj;
                textObj.setText(msgStr);
                if(msg.what ==1)
                    startButton.setEnabled(true);
            }
        };
        updateCount = new Handler(){
            @Override
            public void handleMessage(Message msg) {
                int count = (int) msg.obj;
                textCount.setText(Integer.toString(count));
            }
        };
        startButton.setX(500);
        startButton.setY(900);
        startButton.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v) {
                startButton.setEnabled(false);
                tmpMsg = new Message();
                processingStr = "Processing ...";
                tmpMsg.obj = (Object) processingStr;
                updateHandler.sendMessage(tmpMsg);
                filecount = 0;
                t1 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        File outputFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "snapshot_entrophy.txt");
                        File outputFilePath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "absolute_path.txt");
                        FileOutputStream dataStream = null, pathStream ;

                        try {

                            dataStream = new FileOutputStream(outputFile);
                            pathStream = new FileOutputStream(outputFilePath);

                            traverse_dir(storage_directory, dataStream, pathStream);


                            // scan finished
                            dataStream.close();
                            pathStream.close();
                            processingStr = "Done! number of File = " + filecount;
                            tmpMsg = new Message();
                            tmpMsg.obj = (Object) processingStr;
                            tmpMsg.what = 1;
                            updateHandler.sendMessage(tmpMsg);

                        } catch (FileNotFoundException e){
                            e.printStackTrace();
                        } catch (IOException e){
                            e.printStackTrace();
                        } catch (InterruptedException e){
                            e.printStackTrace();
                        }
                    }
                });
                t1.start();



            }
        });
    }
    public void traverse_dir (File dir,FileOutputStream dataStream,FileOutputStream pathStream ) throws InterruptedException {

        if(dir.isDirectory()){

            File[] files = dir.listFiles();

            Log.d("GG", dir.getPath());
            for (int i = 0; i < files.length; i++)
            {
                traverse_dir(files[i],dataStream, pathStream);

                    if(files[i].isFile() && files[i].getName() != "snapshot_entrophy.txt" && files[i].getName() != "absolute_path.txt"){
                    Message tmpMsg = new Message();
                    Message tmpMsg2 = new Message();
                    tmpMsg.obj = (Object) files[i].getAbsolutePath();

                    filecount ++;
                    tmpMsg2.obj = (Object)filecount;
                    updateCount.sendMessage(tmpMsg2);
                    updateHandler.sendMessage(tmpMsg);
                    Thread.sleep(10);

                    long filesize = files[i].length();

                    byte[] buf = new byte[4096];

                    double log4096 = Math.log(4096.0)/Math.log(2.0);
                    double ent;
                    try {
                        long chunks = (filesize >> 12 );
                        if (filesize > 0)
                                chunks += 1;

                        Log.d("AA" , files[i].getName() + " " + filesize);
                        pathStream.write((files[i].getAbsolutePath()+ "\n").getBytes());
                        pathStream.flush();
                        String outputStr;
                        if(chunks > 0) {
                            BufferedInputStream inStream = new BufferedInputStream( new FileInputStream(files[i]));
                            for(long j = 0 ; j < chunks ; j++){

                                Message tmpMsg3 = new Message();
                                String str = files[i].getName() + " " + j + "/" + chunks;
                                tmpMsg3.obj = (Object) str;
                                updateHandler.sendMessage(tmpMsg3);
                                int read = inStream.read(buf, 0, 4096);
                                if(read < 0)
                                    break;
                                ent = entrophy(buf, read);
                                String tmp = SHAsum(buf);
                                //outputStr = files[i].getName() + " chunk  " + j + " sha1 = " + tmp + "\n";
                                outputStr = files[i].getName() + "&chunk&" + j + "&entrophy&" + ent  + "&sha1&" + tmp + "\n";

                                dataStream.write(outputStr.getBytes());
                                dataStream.flush();
                                //Log.d("AA" , files[i].getName() + " chunk  " + j + " sha1 = " + tmp);
                            }
                            inStream.close();
                        }
                    } catch (FileNotFoundException e){
                        Log.d("ERROR", e.toString());
                    } catch (IOException e ){
                        Log.d("ERROR", e.toString());
                    } catch (NoSuchAlgorithmException e) {
                        Log.d("ERROR", e.toString());
                    }


                }
            }


        }

    }
    public static double entrophy(byte [] buf, int length){
        double e = 0.0, p;
        int [] count = new int [256];
        int tmp;
        for(int i = 0 ; i < 256 ; i ++){
            count[i] = 0;
        }
        for(int i = 0 ; i < length ; i++){
            count[(buf[i]&0xff)]++;
        }
        for(int i = 0 ; i < 256 ; i++){
            if(count[i] >0 ){
                p = ((double)count[i])/length;
                e += p * (Math.log(p)/Math.log(2));
            }
        }
        return -e;
    }
    public static String SHAsum(byte[] convertme) throws NoSuchAlgorithmException{
        MessageDigest md = MessageDigest.getInstance("SHA-1");
        return byteArray2Hex(md.digest(convertme));
    }
    private static String byteArray2Hex(final byte[] hash) {
        Formatter formatter = new Formatter();
        for (byte b : hash) {
            formatter.format("%02x", b);
        }
        return formatter.toString();
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
