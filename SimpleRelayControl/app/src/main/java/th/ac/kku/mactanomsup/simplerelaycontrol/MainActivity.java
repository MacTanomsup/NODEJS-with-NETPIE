package th.ac.kku.mactanomsup.simplerelaycontrol;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import netpie.io.netpiegear.EventListener;
import netpie.io.netpiegear.Microgear;

public class MainActivity extends AppCompatActivity {

    TextView status_tv;
    ToggleButton relay1_tgb, relay2_tgb, relay3_tgb, relay4_tgb;
    Button connect_bt;

    public Microgear microgear = new Microgear(this);

    EventListener eventListener = new EventListener();

    //Fill your key first
    String appid = ""; //APP_ID
    String key = ""; //KEY
    String secret = ""; //SECRET

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        init();

        connect_bt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                microgear.connect(appid,key,secret);
            }
        });

        relay1_tgb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                microgear.publish("relay", "1");
            }
        });

        relay2_tgb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                microgear.publish("relay", "2");
            }
        });

        relay3_tgb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                microgear.publish("relay", "3");
            }
        });

        relay4_tgb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                microgear.publish("relay", "4");
            }
        });

        eventListener.setConnectEventListener(new EventListener.OnServiceConnect() {
            @Override
            public void onConnect(Boolean status) {
                if(status == true){
                    Message msg = handler.obtainMessage();
                    status_tv.setText("Now I'm connected with NETPIE");
                    Log.i("Connected","Now I'm connected with NETPIE");
                }
                else{
                    status_tv.setText("Can't connect to NETPIE");
                    Log.i("NotConnect","Can't connect to NETPIE");
                }
            }

        });

        eventListener.setMessageEventListener(new EventListener.OnMessageReceived() {
            @Override
            public void onMessage(final String topic, final String message) {
                Message msg = handler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putString("myKey", topic+" : " + message);
                msg.setData(bundle);
                handler.sendMessage(msg);
                Log.i("Message",topic+" : " + message);
            }
        });

        eventListener.setPresentEventListener(new EventListener.OnPresent() {
            @Override
            public void onPresent(String name) {
                Message msg = handler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putString("myKey", "New friend Connect :"+name);
                msg.setData(bundle);
                handler.sendMessage(msg);
                Log.i("present","New friend Connect :"+name);
            }
        });

        eventListener.setAbsentEventListener(new EventListener.OnAbsent() {
            @Override
            public void onAbsent(String name) {
                Message msg = handler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putString("myKey", "Friend lost :"+name);
                msg.setData(bundle);
                handler.sendMessage(msg);
                Log.i("absent","Friend lost :"+name);
            }
        });

        eventListener.setDisconnectEventListener(new EventListener.OnClose() {
            @Override
            public void onDisconnect(Boolean status) {
                Message msg = handler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putString("myKey", "Disconnected");
                msg.setData(bundle);
                handler.sendMessage(msg);
                Log.i("disconnect","Disconnected");
            }
        });

        eventListener.setOnException(new EventListener.OnException() {
            @Override
            public void onException(String error) {
                Message msg = handler.obtainMessage();
                Bundle bundle = new Bundle();
                bundle.putString("myKey", "Exception : "+error);
                msg.setData(bundle);
                handler.sendMessage(msg);
                Log.i("exception","Exception : "+error);
            }
        });

    }

    private void init() {
        status_tv = (TextView) findViewById(R.id.status_tv);
        relay1_tgb = (ToggleButton) findViewById(R.id.relay1_tgb);
        relay2_tgb = (ToggleButton) findViewById(R.id.relay2_tgb);
        relay3_tgb = (ToggleButton) findViewById(R.id.relay3_tgb);
        relay4_tgb = (ToggleButton) findViewById(R.id.relay4_tgb);
        connect_bt = (Button) findViewById(R.id.connect_bt);
    }

    Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Bundle bundle = msg.getData();
            String string = bundle.getString("myKey");

        }
    };

    protected void onDestroy() {
        super.onDestroy();
        microgear.disconnect();
    }

    protected void onResume() {
        super.onResume();
        microgear.bindServiceResume();
    }

    public void showToast(String str) {
        Context context = getApplicationContext();
        CharSequence text = str;
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
    }




}
