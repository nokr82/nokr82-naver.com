package com.neung.playkok;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.List;
import java.util.Map;

import static android.content.ContentValues.TAG;


public class FirebaseInstanceIDService extends FirebaseMessagingService {

    /**
     * 구글 토큰을 얻는 값입니다.
     * 아래 토큰은 앱이 설치된 디바이스에 대한 고유값으로 푸시를 보낼때 사용됩니다.
     * **/

    @Override
    public void onNewToken(String s) {
        super.onNewToken(s);
        Log.e("Firebase", "FirebaseInstanceIDService : " + s);
    }

    /**
     * 메세지를 받았을 경우 그 메세지에 대하여 구현하는 부분입니다.
     * **/
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> data = remoteMessage.getData();
        if (data == null) {
            return;
        }

        String title = "";
        String body = "";
        String message = "";
        String url = "";

        if (remoteMessage.getNotification() != null) {
            /*
             * GCM 방식
             * firebase console 테스트 메시지 받는 방법
             */
            System.out.println("----- title : " + remoteMessage.getNotification().getTitle());
            System.out.println("----- body : " + remoteMessage.getNotification().getBody());
            System.out.println("----- 링크 : " + remoteMessage.getData().get("url"));
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
            url = remoteMessage.getData().get("url");

            sendNotification(title,body,url);
        }


//        if (remoteMessage.getData().size() > 0) {
//            Log.d(TAG, "Message data payload: " + remoteMessage.getData());
//           /* title = remoteMessage.getData().get("title");
//            message = remoteMessage.getData().get("message");
//            url = remoteMessage.getData().get("url");
//            System.out.println("----- title : " + title);
//            System.out.println("----- url : " + url);*/
//        }

    }

    /**
     * remoteMessage 메세지 안애 getData와 getNotification이 있습니다.
     * 이부분은 차후 테스트 날릴때 설명 드리겠습니다.
     * **/
    private void sendNotification(String title,String message,String url) {

        /*
         * FCM 방식
         * PHP 연동 및 일반 테스트 메시지 받는 방법
         */
        Intent intent = new Intent();
        //중복 앱 실행 막기
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        //앱 실행중일 시
        if (isAppRunning(this)) {
            Intent broadcastIntent = new Intent("PUSH");
            broadcastIntent.putExtra("url",url);
            sendBroadcast(broadcastIntent);
        }

        // 푸쉬 클릭 시 이벤트 설정
        intent = new Intent(this,MainActivity.class);
        intent.putExtra("url",url);

        //채널 id 세팅 android 9.0 이상 푸쉬 안옴
        String channelId = getString(R.string.default_notification_channel_id);
        Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        /*
         *  모든 (int)System.currentTimeMillis()/1000 는 푸쉬가 곂치기 않기 위해 설정
         * currentTime 으로 하면 푸쉬가 쌓임
         */
        PendingIntent pendingIntent = PendingIntent.getActivity(this,(int)System.currentTimeMillis()/1000,intent,PendingIntent.FLAG_ONE_SHOT);
        Uri soundUri = Uri.parse("android.resuource://" + getPackageName() + "/" + R.raw.alram45);
        // 푸쉬 세팅 (푸쉬 제목, 내용, 아이콘, 사운드)
        NotificationCompat.Builder notificationBuilder =
                new NotificationCompat.Builder(this, channelId)
                        .setGroup(channelId)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setAutoCancel(true)
                        .setSound(soundUri)
                        .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channel_nm = getString(R.string.default_notification_channel_name);
            NotificationChannel channel = new NotificationChannel(channelId,channel_nm,NotificationManager.IMPORTANCE_HIGH);
            notificationManager.createNotificationChannel(channel);
        }

        //화면 깨우기
        if (!isScreenOn(this)) {
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            @SuppressLint("InvalidWakeLockTag") PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "extrade2");

            wakeLock.acquire(3000);
            notificationManager.notify((int)(System.currentTimeMillis()/1000), notificationBuilder.build());
        }

        //푸쉬 보내기
        notificationManager.notify((int)(System.currentTimeMillis()/1000), notificationBuilder.build());

    }

    public static boolean isScreenOn(Context context) {
        return ((PowerManager)context.getSystemService(Context.POWER_SERVICE)).isScreenOn();
    }

    // 앱이 실행중일때 아닐때
    @RequiresApi(api = Build.VERSION_CODES.Q)
    private boolean isAppRunning(Context context) {
        String PackageName = getPackageName();
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ComponentName componentInfo = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            List<ActivityManager.AppTask> tasks = manager.getAppTasks();
            if (tasks != null && tasks.size() > 0) {
                componentInfo = tasks.get(0).getTaskInfo().topActivity;
            }
        } else {
            List<ActivityManager.RunningTaskInfo> tasks = manager.getRunningTasks(1);
            if (tasks != null && tasks.size() > 0 && tasks.size() > 0) {
                componentInfo = tasks.get(0).topActivity;
            }
        }

        if (null != componentInfo) {
            if (componentInfo.getPackageName().equals(PackageName)) {
                return true;
            }
        }

        return false;
    }


}


