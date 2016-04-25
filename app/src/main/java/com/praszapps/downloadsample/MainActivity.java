package com.praszapps.downloadsample;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.jar.Manifest;

public class MainActivity extends AppCompatActivity {

    private ProgressDialog mProgressDialog;
    private final DownloadFileTask mDownloadTask = new DownloadFileTask(MainActivity.this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setMessage("Downloading...");
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        mProgressDialog.setCancelable(true);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if(checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                if(shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    requestPermissions(new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE} , 123);
                } else {
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("Give the permission bitch!!!");
                    builder.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                requestPermissions(new String[] {android.Manifest.permission.WRITE_CONTACTS}, 123);
                            }
                        }
                    });
                    builder.setNegativeButton("Cancel", null);
                    builder.show();
                }


            } else {
                mDownloadTask.execute("https://wordpress.org/plugins/about/readme.txt");
            }
        } else {
            mDownloadTask.execute("https://wordpress.org/plugins/about/readme.txt");
        }

        mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                mDownloadTask.cancel(true);
            }
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 123) {
            if(grantResults[0] == getPackageManager().PERMISSION_GRANTED) {
                mDownloadTask.execute("https://wordpress.org/plugins/about/readme.txt");
            } else {
                Toast.makeText(MainActivity.this, "Permission Denied.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private class DownloadFileTask extends AsyncTask<String, Integer, String> {

        private Context mContext;
        private HttpURLConnection connection = null;
        private InputStream inputStream;
        private OutputStream outputStream;

        public DownloadFileTask(Context context) {
            this.mContext = context;
        }

        @Override
        protected String doInBackground(String... params) {
            try {
                URL url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(50000);
                connection.setRequestMethod("GET");
                connection.connect();
                if(connection.getResponseCode() == HttpURLConnection.HTTP_OK) {


                    File file = new File(Environment
                            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                            + "/Filename.txt");
                    if(file.exists()) {
                        file.delete();
                    }
                    inputStream = connection.getInputStream();
                    outputStream = new FileOutputStream(file);
                    byte [] buffer = new byte[connection.getContentLength()];
                    int count;
                    long total = 0;
                    while ((count = inputStream.read(buffer)) != -1) {

                        if(isCancelled()) {
                            inputStream.close();
                            return "Cancelled";
                        } else {
                            total += count;
                            // publishing the progress....
                            if (connection.getContentLength() > 0) // only if total length is known
                                publishProgress((int) (total * 100 / connection.getContentLength()));

                            outputStream.write(buffer, 0, count);
                        }

                    }

                    outputStream.flush();
                    outputStream.close();
                    inputStream.close();

                } else {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }

            } catch (MalformedURLException e) {
                e.printStackTrace();
                return null;
            } catch (IOException e) {
                e.printStackTrace();
                return null;
            } finally {

                if(connection != null) {
                    connection.disconnect();
                }
                return null;
            }
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog.show();
        }

        @Override
        protected void onPostExecute(String result) {

            mProgressDialog.dismiss();

        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setMax(100);
            mProgressDialog.setProgress(progress[0]);
        }
    }

    private static String getStringFromInputStream(InputStream is) {

        BufferedReader br = null;
        StringBuilder sb = new StringBuilder();

        String line;
        try {

            br = new BufferedReader(new InputStreamReader(is));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();

    }
}
