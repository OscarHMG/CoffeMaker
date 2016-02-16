package com.example.user.coffeemaker;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.R;
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
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Calendar;
import cn.pedant.SweetAlert.SweetAlertDialog;
import xyz.hanks.library.SmallBang;
import xyz.hanks.library.SmallBangListener;

/**
 * Created by user on 10/02/2016.
 */
public class MainActivity extends Activity implements View.OnClickListener, SmallBangListener{
    private RelativeLayout alarm,turn,temperature,status,about;
    private TableLayout menu;
    private SmallBang animation;
    private int hourOfDay, minuteOfDay;
    private AlarmManager alarmManager;
    private Calendar calendar;
    private Intent myIntent;
    private PendingIntent pendingIntent;
    private ConnectionDetector connection;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.example.user.coffeemaker.R.layout.menu);
        initElementes();
        connection=new ConnectionDetector(MainActivity.this);
        animation = SmallBang.attach2Window(this);
        setAnimations();
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

        if(!connection.isConnectingToInternet()){
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
        }else {
            switch (v.getId()) {
                case com.example.user.coffeemaker.R.id.containter_alarm:
                    animation.bang(alarm, 260, this);

                    showDialog(999);
                    break;
                case com.example.user.coffeemaker.R.id.containter_turn:
                    animation.bang(turn, 260, this);
                    showDialogMessage("Actual State: ON/OFF?", "Succesfully ON/OFF ?", SweetAlertDialog.SUCCESS_TYPE);
                    break;
                case com.example.user.coffeemaker.R.id.container_temperature:
                    animation.bang(temperature, 260, this);

                    new JSonTask().execute("http://api.thingspeak.com/channels/81636/fields/1/last.txt");

                    break;
                case com.example.user.coffeemaker.R.id.containter_status:
                    animation.bang(status, 260, this);
                    showDialogMessage("Status", "The jug is not placed in the coffee maker?", SweetAlertDialog.WARNING_TYPE);
                    break;
                case com.example.user.coffeemaker.R.id.containter_about:
                    animation.bang(about, 260, this);

                    break;
            }
        }
    }

    public void setDisableStatusBar(){
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
    }
    public void setAnimations(){
        setAnimationView(alarm);
        setAnimationView(turn);
        setAnimationView(temperature);
        setAnimationView(status);
        setAnimationView(about);
    }
    public void setAnimationView(View v){
        PropertyAction alarmAnimation = PropertyAction.
                                        newPropertyAction(v).scaleX(0).scaleY(0).duration(1000).
                                        interpolator(new AccelerateDecelerateInterpolator()).build();
        Player.init().animate(alarmAnimation).play();
    }

    public void setAlarm(){
        final int hour = hourOfDay;
        final int minute = minuteOfDay;;

        String minute_string = String.valueOf(minute);
        String hour_string = String.valueOf(hour);

        if (minute < 10) {
            minute_string = "0" + String.valueOf(minute);
        }

        pendingIntent = PendingIntent.getBroadcast(this, 0, myIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        alarmManager.set(AlarmManager.RTC_WAKEUP,calendar.getTimeInMillis(),pendingIntent);
        Toast.makeText(this,"Alarm set to "+hour_string+":"+minute_string,Toast.LENGTH_LONG).show();
    }

    @Override
    public Dialog onCreateDialog(int id){
        if(id==999){
            hourOfDay = calendar.getTime().getHours();
            minuteOfDay = calendar.getTime().getMinutes();
            return new TimePickerDialog(this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT,timePickerListener, hourOfDay,minuteOfDay, false);
        }
        return null;
    }
    private TimePickerDialog.OnTimeSetListener timePickerListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker timePicker, int hour, int minute) {
            hourOfDay = hour;
            minuteOfDay = minute;
            setAlarm();
            showDialogMessage("Alarm", "Alarm settings was successful", SweetAlertDialog.SUCCESS_TYPE);
        }
    };

    public void showDialogMessage(String title, String context, int typeMessage){
        new SweetAlertDialog(this,typeMessage)
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



    public class JSonTask extends AsyncTask<String, String, String>{


        @Override
        protected String doInBackground(String... params) {
            HttpURLConnection connection = null;
            StringBuffer buffer;
            BufferedReader reader = null;
            URL url;
            InputStream stream;
            try{
                url = new URL(params[0]);
                connection = (HttpURLConnection)url.openConnection();
                connection.connect();
                stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                buffer = new StringBuffer();
                String line = "";
                while((line = reader.readLine())!=null){
                    buffer.append(line);
                }
                return buffer.toString();
            }catch(MalformedURLException ex){
                ex.printStackTrace();
            }catch(IOException ioex){
                ioex.printStackTrace();
            }finally{
                if(connection!=null)
                    connection.disconnect();
                try {
                    if(reader!=null)
                        reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            showDialogMessage("Current Temperature", "Temperature:"+s, SweetAlertDialog.SUCCESS_TYPE);
        }
    }

}
