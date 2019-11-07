package nirmal.developer.videoupload;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private Button buttonChoose;
    private Button buttonUpload;
    private TextView textView;
    private TextView textViewResponse;

    private static final int SELECT_VIDEO = 3;

    private String selectedPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        buttonChoose = findViewById(R.id.buttonChoose);
        buttonUpload = findViewById(R.id.buttonUpload);

        textView =  findViewById(R.id.textView);
        textViewResponse =  findViewById(R.id.textViewResponse);

        buttonChoose.setOnClickListener(this);
        buttonUpload.setOnClickListener(this);
    }

    private void chooseVideo() {
        Intent intent = new Intent();
        intent.setType("video/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select a Video "), SELECT_VIDEO);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_VIDEO) {
                System.out.println("SELECT_VIDEO");
                Uri selectedImageUri = data.getData();
                selectedPath = getPath(selectedImageUri);
                textView.setText(selectedPath);
            }
        }
    }

    public String getPath(Uri uri) {
        Cursor cursor = getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
        cursor.close();

        cursor = getContentResolver().query(
                android.provider.MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Images.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Video.Media.DATA));
        cursor.close();

        return path;
    }

    private void uploadVideo() {
        class UploadVideo extends AsyncTask<Void, Void, String> {

            ProgressDialog uploading;

            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                uploading = ProgressDialog.show(MainActivity.this, "Uploading File", "Please wait...", false, false);
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                uploading.dismiss();
                textViewResponse.setText(Html.fromHtml("<b>Uploaded at <a href='" + s + "'>" + s + "</a></b>"));
                textViewResponse.setMovementMethod(LinkMovementMethod.getInstance());
            }

            @Override
            protected String doInBackground(Void... params) {
                Upload u = new Upload();
                String msg = u.uploadVideo(selectedPath);
                return msg;
            }
        }
        UploadVideo uv = new UploadVideo();
        uv.execute();
    }


    @Override
    public void onClick(View v) {
        if (v == buttonChoose) {
            chooseVideo();
        }
        if (v == buttonUpload) {
            uploadVideo();
        }
    }

    public static class Upload {
        public static final String UPLOAD_URL= "https://appealable-merchant.000webhostapp.com/videoupload.php";

        private int serverResponseCode;

        public String uploadVideo(String file) {

            String fileName = file;
            HttpURLConnection conn = null;
            DataOutputStream dos = null;
            String lineEnd = "\r\n";
            String twoHyphens = "--";
            String boundary = "*****";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;

            File sourceFile = new File(file);
            if (!sourceFile.isFile()) {
                Log.e("Huzza", "Source File Does not exist");
                return null;
            }

            try {
                FileInputStream fileInputStream = new FileInputStream(sourceFile);
                URL url = new URL(UPLOAD_URL);
                conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.setDoOutput(true);
                conn.setUseCaches(false);
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Connection", "Keep-Alive");
                conn.setRequestProperty("ENCTYPE", "multipart/form-data");
                conn.setRequestProperty("Content-Type", "multipart/form-data;boundary=" + boundary);
                conn.setRequestProperty("myFile", fileName);
                dos = new DataOutputStream(conn.getOutputStream());

                dos.writeBytes(twoHyphens + boundary + lineEnd);
                dos.writeBytes("Content-Disposition: form-data; name=\"myFile\";filename=\"" + fileName + "\"" + lineEnd);
                dos.writeBytes(lineEnd);

                bytesAvailable = fileInputStream.available();
                Log.i("Huzza", "Initial .available : " + bytesAvailable);

                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                buffer = new byte[bufferSize];

                bytesRead = fileInputStream.read(buffer, 0, bufferSize);

                while (bytesRead > 0) {
                    dos.write(buffer, 0, bufferSize);
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                dos.writeBytes(lineEnd);
                dos.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                serverResponseCode = conn.getResponseCode();

                fileInputStream.close();
                dos.flush();
                dos.close();
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (serverResponseCode == 200) {
                StringBuilder sb = new StringBuilder();
                try {
                    BufferedReader rd = new BufferedReader(new InputStreamReader(conn
                            .getInputStream()));
                    String line;
                    while ((line = rd.readLine()) != null) {
                        sb.append(line);
                    }
                    rd.close();
                } catch (IOException ioex) {
                }
                return sb.toString();
            }else {
                return "Could not upload";
            }
        }

    }
}