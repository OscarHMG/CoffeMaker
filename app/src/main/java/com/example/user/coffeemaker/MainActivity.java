package com.example.user.coffeemaker;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TimePicker;
import android.widget.Toast;
import com.eftimoff.androidplayer.Player;
import com.eftimoff.androidplayer.actions.property.PropertyAction;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;

import cn.pedant.SweetAlert.SweetAlertDialog;
import xyz.hanks.library.SmallBang;
import xyz.hanks.library.SmallBangListener;

/**
 * Created by user on 10/02/2016.
 */
public  class MainActivity extends Activity implements View.OnClickListener, SmallBangListener {
    private static final int FLAG_ALARM_DIALOG =999 ;
    private RelativeLayout alarm, turn, temperature, status, about;
    private SmallBang animation;
    private int hourOfDay, minuteOfDay;
    private AlarmManager alarmManager;
    private Calendar calendar;
    private Intent myIntent;
    private PendingIntent pendingIntent;
    private ConnectionDetector connection;
    private static String typeOfOp;
    private static String id= "0";
    protected SweetAlertDialog dialog;
    JSonTask mWorker  = new JSonTask(this);
    private final static String GET_URL = "http://api.thingspeak.com/channels/81636/fields";
    private final static String POST_URL="https://api.thingspeak.com/update";
    private final static String API_KEY= "JOXZJ0MWXKS66ENR";

    protected MainActivity principalActivity;
    private TableLayout menu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.user.coffeemaker.R.layout.menu);
        initElementes();
        connection = new ConnectionDetector(MainActivity.this);
        animation = SmallBang.attach2Window(this);
        setAnimations();
        mWorker.mainActivity = this;
        typeOfOp = "GET";
        mWorker.execute(GET_URL+"/1/last.txt");
    }

    public MainActivity() {
        this.principalActivity = this;
    }

    public void initElementes() {
        calendar = Calendar.getInstance();
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        menu = (TableLayout) findViewById(com.example.user.coffeemaker.R.id.table);
        alarm = (RelativeLayout) findViewById(com.example.user.coffeemaker.R.id.containter_alarm);
        turn = (RelativeLayout) findViewById(com.example.user.coffeemaker.R.id.containter_turn);
        temperature = (RelativeLayout) findViewById(com.example.user.coffeemaker.R.id.container_temperature);
        status = (RelativeLayout) findViewById(com.example.user.coffeemaker.R.id.containter_status);
        about = (RelativeLayout) findViewById(com.example.user.coffeemaker.R.id.containter_about);
        myIntent = new Intent(MainActivity.this, AlarmReceiver.class);
        alarm.setOnClickListener(this);
        turn.setOnClickListener(this);
        temperature.setOnClickListener(this);
        status.setOnClickListener(this);
        about.setOnClickListener(this);
        setDisableStatusBar();
    }

    @Override
    public void onClick(View v) {

        if (!connection.isConnectingToInternet()) {
            new SweetAlertDialog(this, SweetAlertDialog.WARNING_TYPE)
                    .setTitleText("UPS!")
                    .setContentText("Check your internet Connection")
                    .setConfirmText("OK")
                    .setConfirmClickListener(new SweetAlertDialog.OnSweetClickListener() {
                        @Override
                        public void onClick(SweetAlertDialog sDialog) {
                            System.exit(1);
                        }
                    })
                    .show();
        } else {
            String tmp;
            switch (v.getId()) {
                case com.example.user.coffeemaker.R.id.containter_alarm:
                    animation.bang(alarm, 260, this);
                    typeOfOp = "POST";
                    id="1";
                    showDialog(FLAG_ALARM_DIALOG);
                    break;
                case com.example.user.coffeemaker.R.id.containter_turn:
                    animation.bang(turn, 260, this);
                    String msg = (mWorker.estado.equals("1")) ? " OFF" : " ON";
                    showDialogMessage("CoffeeMaker", "Succesfully " +msg, SweetAlertDialog.SUCCESS_TYPE);
                    typeOfOp = "POST";
                    id="2";
                    JSonTask turnTask= (JSonTask) new JSonTask(this).execute("2");
                    break;
                case com.example.user.coffeemaker.R.id.container_temperature:
                    animation.bang(temperature, 260, this);
                    typeOfOp = "GET";
                    id="3";
                    JSonTask getTempTask= (JSonTask) new JSonTask(this).execute(GET_URL+"2/last.txt");
                    break;
                case com.example.user.coffeemaker.R.id.containter_status:
                    animation.bang(status, 260, this);
                    typeOfOp = "GET";
                    id="4";
                    JSonTask getStatusTask= (JSonTask) new JSonTask(this).execute(GET_URL+"/3/last.txt");
                    break;
                case com.example.user.coffeemaker.R.id.containter_about:
                    animation.bang(about, 260, this);

                    break;
            }
        }
    }

    public void setDisableStatusBar() {
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }

    public void setAnimations() {
        setAnimationView(alarm);
        setAnimationView(turn);
        setAnimationView(temperature);
        setAnimationView(status);
        setAnimationView(about);
    }

    public void setAnimationView(View v) {
        PropertyAction alarmAnimation = PropertyAction.
                newPropertyAction(v).scaleX(0).scaleY(0).duration(1000).
                interpolator(new AccelerateDecelerateInterpolator()).build();
        Player.init().animate(alarmAnimation).play();
    }

    public void setAlarm() {
        int x =calendar.HOUR_OF_DAY;
        int y =calendar.MINUTE;
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minuteOfDay);

       /* String minute_string = String.valueOf(minute);
        String hour_string = String.valueOf(hour);

        if (minute < 10) {
            minute_string = "0" + String.valueOf(minute);
        }*/

        pendingIntent = PendingIntent.getBroadcast(this, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        //Toast.makeText(this, "Alarm set to " + hour_string + ":" + minute_string, Toast.LENGTH_LONG).show();
    }

    @Override
    public Dialog onCreateDialog(int id) {
        if (id == 999) {
            return new TimePickerDialog(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT, timePickerListener, hourOfDay, minuteOfDay, false);
        }
        return null;
    }

    private TimePickerDialog.OnTimeSetListener timePickerListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker timePicker, int hour, int minute) {
            hourOfDay =  hour;
            minuteOfDay = minute;
            setAlarm();
            showDialogMessage("Alarm", "Alarm settings was successful ", SweetAlertDialog.SUCCESS_TYPE);
        }
    };

    public void showDialogMessage(String title, String context, int typeMessage) {
        new SweetAlertDialog(this, typeMessage)
                .setTitleText(title)
                .setContentText(context)
                .show();
    }

    @Override
    public void onAnimationStart() {

    }

    @Override
    public void onAnimationEnd() {

    }

    public void postValue() {

    }

    public static class JSonTask extends AsyncTask<String, String, String> {

        private static String finalEstado;
        private static String estado;
        private static int numTime = 0;
        private static String msgGET;
        private Context context;
        MainActivity mainActivity;

        public JSonTask(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @Override
        protected String doInBackground(String... params) {

            if (typeOfOp.equals("GET")) {
                estado= getRequest(params);
                Log.i("Estado Actual:", estado);
            } else {
                postRequest(params);
                //return null;
            }
            return estado;
        }


        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            switch(id){
                case "0":
                    mainActivity.showDialogMessage("Welcome", "", SweetAlertDialog.SUCCESS_TYPE);
                    estado= s; //App start, previosuly we have to know if the cofee maker is ON or OFF
                    break;
                case "1":

                    break;
                case "2":
                    break;
                case "3":
                    mainActivity.showDialogMessage("Actual Temperature", s+" °C", SweetAlertDialog.WARNING_TYPE);
                    break;
                case "4":
                    String result = s.equals("1")?"The jar is present in Coffee Maker":"The jar isn't present in the Coffee Maker";
                    mainActivity.showDialogMessage("Status",result,SweetAlertDialog.WARNING_TYPE);
                    break;
            }
            //showDialogMessage("Current Temperature", "Temperature:" + s, SweetAlertDialog.SUCCESS_TYPE);
            if(!s.equals(null)){
                //Toast.makeText(mainActivity,s, Toast.LENGTH_LONG).show();
               // mainActivity.showDialogMessage("Current Temperature", s, SweetAlertDialog.SUCCESS_TYPE);
            }


        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            SweetAlertDialog pDialog = new SweetAlertDialog(mainActivity, SweetAlertDialog.PROGRESS_TYPE);
            pDialog.getProgressHelper().setBarColor(Color.parseColor("#A5DC86"));
            pDialog.setTitleText("Loading");
            pDialog.setCancelable(false);
            pDialog.show();
        }

        public  Map<String, Object> setParams(String id) {
            Map<String, Object> params = new LinkedHashMap<>();
            params.put("api_key", API_KEY);
            if (id.equals("1")) { //Set Alarm
                params.put("field1", "1");
            } else if (id.equals("2")) { // Turn ON or Turn OFF?
                //JSonTask mTask = (JSonTask) new JSonTask(mainActivity).execute(GET_URL+"/1/last.txt");
                if (estado.equals("1")) { //CoffeeMaker is ON
                    params.put("field1", "0");
                    estado = "0";
                } else if (estado.equals("0") || estado.equals("-1") || estado.equals(null)){ //OFF=0; OFF at Start = -1
                    params.put("field1", "1");
                    estado = "1";
                }
                //finalEstado = estado;
                Log.i("Estado Cambiante:",estado);
            }
            return params;
        }

        private String postRequest(String... p) {
            URL url = null;
            try {
                url = new URL(POST_URL);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
            Map<String, Object> params = setParams(p[0]);

            StringBuilder postData = new StringBuilder();
            for (Map.Entry<String, Object> param : params.entrySet()) {
                if (postData.length() != 0) postData.append('&');
                try {
                    postData.append(URLEncoder.encode(param.getKey(), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                postData.append('=');
                try {
                    postData.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }
            byte[] postDataBytes = new byte[0];
            try {
                postDataBytes = postData.toString().getBytes("UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            HttpURLConnection conn = null;
            try {
                conn = (HttpURLConnection) url.openConnection();
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                conn.setRequestMethod("POST");
            } catch (ProtocolException e) {
                e.printStackTrace();
            }
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Content-Length", String.valueOf(postDataBytes.length));
            conn.setDoOutput(true);
            try {
                conn.getOutputStream().write(postDataBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }

            Reader in = null;
            try {
                in = new BufferedReader(new InputStreamReader(conn.getInputStream(), "UTF-8"));
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                for (int c = in.read(); c != -1; c = in.read())
                    System.out.print((char) c);
            } catch (IOException e) {
                e.printStackTrace();
            }
            numTime++;
            return "";
        }


        public String getRequest(String... params) {
            HttpURLConnection connection = null;
            StringBuffer buffer;
            BufferedReader reader = null;
            URL url;
            InputStream stream;
            try {
                url = new URL(params[0]);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();
                stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                buffer = new StringBuffer();
                String line = "";
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
                return buffer.toString();
            } catch (MalformedURLException ex) {
                ex.printStackTrace();
            } catch (IOException ioex) {
                ioex.printStackTrace();
            } finally {
                if (connection != null)
                    connection.disconnect();
                try {
                    if (reader != null)
                        reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }
    }
}