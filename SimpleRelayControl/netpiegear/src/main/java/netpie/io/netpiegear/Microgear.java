package netpie.io.netpiegear;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.regex.Pattern;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;


public class Microgear extends Activity {
    public Context context;
    public Messenger service;
    public IntentFilter intentFilter;
    public OauthNetpieLibrary oauthNetpieLibrary = new OauthNetpieLibrary();
    public MicrogearService senddatatoservice = new MicrogearService();
    public String name = "microgear.cache";
    public static String appidvalue, keyvalue, secretvalue;
    public File tempFile;
    public File cDir;
    static String Namedrive=null;
    static ArrayList<Publish> PublishList = new ArrayList<Publish>();
    static ArrayList<String> SubscribeList = new ArrayList<String>();
    static ArrayList<String> UnsubscribeList = new ArrayList<String>();
    public EventListener eventListener = new EventListener();
    public Microgear(Context context) {
        this.context = context;
        intentFilter = new IntentFilter();
        intentFilter.addAction("com.example.MQTT.PushReceived");
    }

    public class Publish {
        public String Topic;
        public String Message;

        public Publish(String Topic,String Message){
            this.Topic = Topic;
            this.Message = Message;
        }
    }

    public void connect(String appid, String key, String secret) {
        if (appid.isEmpty() || key.isEmpty() || secret.isEmpty()) {
            eventListener.mError.onException("App id ,Key or Secret is Empty");
            eventListener.mConnect.onConnect(false);
        } else {
            appidvalue = appid;
            keyvalue = key;
            secretvalue = secret;

            cDir = context.getCacheDir();
            tempFile = new File(cDir.getPath() + "/" + name);


            if (isConnectingToInternet()) {

                String a = oauthNetpieLibrary.create(appid, key, secret, tempFile.toString());
                if (a.equals("yes")) {
                    brokerconnect(appid, key, secret);
                    context.bindService(new Intent(context, MicrogearService.class), serviceConnection, 0);
                } else if (a.equals("id")) {
                    eventListener.mError.onException("App id Invalid");
                    eventListener.mConnect.onConnect(false);
                    disconnect();
                } else if (a.equals("secretandid")) {
                    eventListener.mError.onException("App id,Key or Secret Invalid");
                    eventListener.mConnect.onConnect(false);
                    disconnect();
                } else {
                    brokerconnect(appid, key, secret);
                    context.bindService(new Intent(context, MicrogearService.class), serviceConnection, 0);
                }


            } else {
                eventListener.mError.onException("No internet connection");
                eventListener.mConnect.onConnect(false);
            }
        }
    }


    public void reconnect() {
        cDir = context.getCacheDir();
        tempFile = new File(cDir.getPath() + "/" + name);
        File file = new File(tempFile.toString());
        file.delete();

        if (isConnectingToInternet()) {
            oauthNetpieLibrary.create(appidvalue, keyvalue, secretvalue, tempFile.toString());
            context.bindService(new Intent(context, MicrogearService.class), serviceConnection, 0);
            brokerconnect(appidvalue, keyvalue, secretvalue);


        } else {
            eventListener.mError.onException("No internet connection");
            eventListener.mConnect.onConnect(false);
        }
    }


    public void bindServiceResume() {
        context.bindService(new Intent(context, MicrogearService.class), serviceConnection, 0);
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        disconnect();
    }

    public void disconnect() {
        try {
            Intent intent = new Intent(context, MicrogearService.class);
            context.unbindService(serviceConnection);
            context.stopService(intent);
        } catch (Exception e) {

        }

    }

    private void brokerconnect(String appid, String key, String secret) {
        File fi = new File(tempFile.toString());
        Intent intent = new Intent(context, MicrogearService.class);
        BufferedReader br;
        StringBuilder sb = new StringBuilder();
        String line;
        String mqttuser, secrettoken, mqttclientid, secretid, mqttpassword, hkey, ckappkey;
        FileInputStream fis;
        try {
            fis = new FileInputStream(tempFile.toString());
            br = new BufferedReader(new InputStreamReader(fis));
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            JSONObject json = new JSONObject(sb.toString());
            mqttuser = json.getJSONObject("_").getString("key");
            secrettoken = json.getJSONObject("_").getJSONObject("accesstoken").getString("secret");
            mqttclientid = json.getJSONObject("_").getJSONObject("accesstoken").getString("token");
            secretid = secret;
            hkey = secrettoken + "&" + secretid; //okay
            long date = new Date().getTime();
            date = date / 1000;
            mqttuser = mqttuser + "%" + date;
            SecretKeySpec keySpec = new SecretKeySpec(hkey.getBytes(), "HmacSHA1");
            try {
                Mac mac = Mac.getInstance("HmacSHA1");
                mac.init(keySpec);
                mqttpassword = mqttclientid + "%" + mqttuser;
                byte[] result = mac.doFinal(mqttpassword.getBytes());
                Base64 base64 = new Base64();
                mqttpassword = base64.encode(result);
                senddatatoservice.setValue(mqttuser, mqttpassword, mqttclientid, appid, key, secret, tempFile.toString());
                context.stopService(intent);
                context.startService(intent);

            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (InvalidKeyException e) {
                e.printStackTrace();
            }

        } catch (FileNotFoundException e){

        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }

    }

    public void publish(final String topic, final String message) {
        if (isConnectingToInternet()) {
            if (Checktopic(topic)!=null) {
                if (topic.isEmpty() || message.isEmpty()) {
                    eventListener.mError.onException("Topic and Message Require");
                } else {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Bundle data = new Bundle();
                            data.putCharSequence(MicrogearService.TOPIC, Checktopic(topic));
                            data.putCharSequence(MicrogearService.MESSAGE, message);
                            Message msg = Message.obtain(null, MicrogearService.PUBLISH);
                            msg.setData(data);
                            try {
                                service.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                                eventListener.mError.onException("Publish Fail");
                            } catch (NullPointerException e) {
                                Publish publish = new Publish(Checktopic(topic),message);
                                PublishList.add(publish);
                            }
                        }
                    }, 300);


                }
            }
        } else {
            eventListener.mError.onException("No internet connection");
        }

    }

    public void publish(final String topic, final String message, final Integer qos, final Boolean retain) {
        if (isConnectingToInternet()) {
            if (Checktopic(topic)!=null) {
                if (topic.isEmpty() || message.isEmpty()) {
                    eventListener.mError.onException("Topic and Message Require");
                } else {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Bundle data = new Bundle();
                            data.putCharSequence(MicrogearService.TOPIC, Checktopic(topic));
                            data.putCharSequence(MicrogearService.MESSAGE, message);
                            data.putBoolean(MicrogearService.RETAIN, retain);
                            data.putInt(MicrogearService.QOS, qos);
                            Message msg = Message.obtain(null, MicrogearService.PUBLISH);
                            msg.setData(data);
                            try {
                                service.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                                eventListener.mError.onException("Publish Fail");
                            } catch (NullPointerException e) {
                                Publish publish = new Publish(Checktopic(topic),message);
                                PublishList.add(publish);

                            }
                        }
                    }, 300);


                }
            }
        } else {
            eventListener.mError.onException("No internet connection");
        }

    }

    public void subscribe(final String topicforsubscribe) {
        if (isConnectingToInternet()) {
            if (!topicforsubscribe.isEmpty()) {
                if (Checktopic(topicforsubscribe)!=null) {
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Bundle data = new Bundle();
                            data.putCharSequence(MicrogearService.TOPIC, Checktopic(topicforsubscribe));
                            Message msg = Message.obtain(null, MicrogearService.SUBSCRIBE);
                            msg.setData(data);
                            try {
                                service.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            } catch (NullPointerException e) {
                                SubscribeList.add(Checktopic(topicforsubscribe));
                            }
                        }
                    }, 300);

                }
            } else {
                eventListener.mError.onException("Topic  required.");
            }
        } else {
            eventListener.mError.onException("No internet connection");
        }

    }

    public void unsubscribe(final String topicforunsubscribe) {
        if (isConnectingToInternet()) {
            if (!topicforunsubscribe.isEmpty()) {
                if (Checktopic(topicforunsubscribe)!=null) {
                    Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Bundle data = new Bundle();
                            data.putCharSequence(MicrogearService.TOPIC, Checktopic(topicforunsubscribe));
                            Message msg = Message.obtain(null, MicrogearService.UNSUBSCRIBE);
                            msg.setData(data);
                            try {
                                service.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            } catch (NullPointerException e) {
                                UnsubscribeList.add(Checktopic(topicforunsubscribe));
                                //eventListener.mError.onException("Please Connect");
                            }
                        }
                    }, 300);

                }

            } else {
                eventListener.mError.onException("Topic  required.");
            }
        } else {
            eventListener.mError.onException("No internet connection");
        }

    }

    public void setalias(final String namedevice) {
        if (isConnectingToInternet()) {
            if (!namedevice.isEmpty()) {
                if (Checkname(namedevice)) {
                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Bundle data = new Bundle();
                            data.putCharSequence(MicrogearService.TOPIC, namedevice);
                            Message msg = Message.obtain(null, MicrogearService.SETNAME);
                            msg.setData(data);
                            try {
                                service.send(msg);
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            } catch (NullPointerException e) {
                                Namedrive = namedevice;
                            }
                        }
                    }, 300);


                }

            } else {
                eventListener.mError.onException("Topic  required.");
            }
        } else {
            eventListener.mError.onException("No internet connection");
        }
    }

    public void chat(final String topicforsendchat, final String message) {
        if (isConnectingToInternet()) {
            if (!topicforsendchat.isEmpty() && !message.isEmpty()) {
                if (Checkname(topicforsendchat)) {

                    final Handler handler = new Handler();
                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Bundle data = new Bundle();
                            data.putCharSequence(MicrogearService.TOPIC, topicforsendchat);
                            data.putCharSequence(MicrogearService.MESSAGE, message);
                            Message msg = Message.obtain(null, MicrogearService.CHAT);
                            msg.setData(data);
                            try {
                                service.send(msg);

                            } catch (RemoteException e) {
                                e.printStackTrace();
                            } catch (NullPointerException e) {
                                Publish publish = new Publish(topicforsendchat,message);
                                PublishList.add(publish);
                                //eventListener.mError.onException("Please Connect");
                            }

                        }
                    }, 300);

                }

            } else {
                eventListener.mError.onException("Topic and Message required.");
            }
        } else {
            eventListener.mError.onException("No internet connection");
        }
    }


    private boolean isConnectingToInternet() {
        ConnectivityManager connectivity = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivity != null) {
            NetworkInfo[] info = connectivity.getAllNetworkInfo();
            if (info != null)
                for (int i = 0; i < info.length; i++)
                    if (info[i].getState() == NetworkInfo.State.CONNECTED) {
                        return true;
                    }

        }
        return false;
    }


    private boolean Checkname(String Topic) {
        Pattern p = Pattern.compile("[^A-Za-z0-9_]");
        if (!p.matcher(Topic).find() && !Topic.isEmpty()) {
            return true;
        } else {
            eventListener.mError.onException("name must be A-Z,a-z,0-9,_ and must not spaces.");
            return false;
        }
    }

    private String Checktopic(String Topic) {
        Pattern p = Pattern.compile("[^A-Za-z0-9/_]");
        if (!p.matcher(Topic).find() && !Topic.isEmpty()) {
            Pattern p1 = Pattern.compile("[\\._/]");
            if(p1.matcher(Topic).find()){
                return Topic.substring(1);
            }
            else{
                return Topic;
            }
        } else {
            eventListener.mError.onException("name must be A-Z,a-z,0-9,_ and must not spaces.");
            return null;
        }
    }



    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName arg0, IBinder binder) {
            service = new Messenger(binder);
            Bundle data = new Bundle();
            data.putCharSequence(MicrogearService.INTENTNAME, "com.example.MQTT.PushReceived");
            Message msg = Message.obtain(null, MicrogearService.REGISTER);
            msg.setData(data);
            try {
                service.send(msg);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            eventListener.mError.onException("service disconnect");
        }
    };


}
