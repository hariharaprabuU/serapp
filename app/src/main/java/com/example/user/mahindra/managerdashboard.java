package com.example.user.mahindra;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;

import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.OkHttpClientFactory;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import com.squareup.okhttp.OkHttpClient;

import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.example.user.mahindra.MainActivity.MyPREFERENCES;

/**
 * Created by ets-prabu on 2/11/17.
 */

public class managerdashboard extends Activity{
    private MobileServiceClient mClient;
    String vehicle_id ;
    private MobileServiceTable<vehicle> vehicleTable;
    private ListAdapter vehicleAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.complaints);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.custom_toolbar);
        //setSupportActionBar(myToolbar);
        ImageButton logout = (ImageButton) findViewById(R.id.logout);
        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(managerdashboard.this, MainActivity.class);
                SharedPreferences sharedpreferences = getSharedPreferences(MyPREFERENCES, Context.MODE_PRIVATE);
                sharedpreferences.edit().remove("usertype").commit();
                finish();
                startActivity(intent);
            }
        });
        TextView test = (TextView)findViewById(R.id.username1);
        Intent intent = getIntent();
        Bundle extras = intent.getExtras();
        String username = extras.getString("username");
        test.setText(username);

        try {
            // Create the Mobile Service Client instance, using the provided

            // Mobile Service URL and key
            mClient = new MobileServiceClient(
                    "https://servicapp.azurewebsites.net",
                    this);

            // Extend timeout from default of 10s to 20s
            mClient.setAndroidHttpClientFactory(new OkHttpClientFactory() {
                @Override
                public OkHttpClient createOkHttpClient() {
                    OkHttpClient client = new OkHttpClient();
                    client.setReadTimeout(20, TimeUnit.SECONDS);
                    client.setWriteTimeout(20, TimeUnit.SECONDS);
                    return client;
                }
            });

            // Get the Mobile Service Table instance to use
            vehicleTable = mClient.getTable("vehicle",vehicle.class);

            // Offline Sync
            //mToDoTable = mClient.getSyncTable("ToDoItem", ToDoItem.class);

            // Create an adapter to bind the items with the view
            vehicleAdapter = new ListAdapter(this, R.layout.vehicle_list);
            ListView vehicleList = (ListView) findViewById(R.id.vehicleList);
            vehicleList.setAdapter(vehicleAdapter);
            // Load the items from the Mobile Service
            AsyncTask<Void, Void, Void> task = new AsyncTask<Void, Void, Void>(){
                @Override
                protected Void doInBackground(Void... params) {

                    try {
                        final List<vehicle> results = vehicleTable.where().select("vehicle_no","vehicle_id").execute().get();

                        //Offline Sync
                        //final List<ToDoItem> results = refreshItemsFromMobileServiceTableSyncTable();
                        StringBuilder commaSepValueBuilder = new StringBuilder();
                        //Looping through the list
                        for (int i = 0; i < results.size(); i++) {
                            commaSepValueBuilder.append(results.get(i));

                            if (i != results.size() - 1) {
                                commaSepValueBuilder.append(", ");
                            }
                        }
                        final String vehicle = commaSepValueBuilder.toString();
                        final String[] temp = vehicle.split(",");
                        vehicle_id = temp[0];
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                vehicleAdapter.clear();

                                for (vehicle item : results) {
                                    vehicleAdapter.add(item);
                                }
                            }
                        });
                    } catch (final Exception e){
                        createAndShowDialogFromTask(e, "Error");
                    }

                    return null;
                }
            };

            runAsyncTask(task);

        } catch (MalformedURLException e) {
            createAndShowDialog(new Exception("There was an error creating the Mobile Service. Verify the URL"), "Error");
        } catch (Exception e){
            createAndShowDialog(e, "Error");
        }
    }

    public void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(managerdashboard.this,manager.class);
        intent.putExtra("vehicle_id",vehicle_id);
        startActivity(intent);
    }

    private void createAndShowDialog(Exception exception, String title) {
        Throwable ex = exception;
        if(exception.getCause() != null){
            ex = exception.getCause();
        }
        createAndShowDialog(ex.getMessage(), title);
    }

    private void createAndShowDialog(final String message, final String title) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setMessage(message);
        builder.setTitle(title);
        builder.create().show();
    }

    private AsyncTask<Void, Void, Void> runAsyncTask(AsyncTask<Void, Void, Void> task) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            return task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            return task.execute();
        }
    }

    private void createAndShowDialogFromTask(final Exception exception, String title) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                createAndShowDialog(exception, "Error");
            }
        });
    }

}
