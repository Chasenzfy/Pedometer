package de.j4velin.pedometer.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.sina.weibo.sdk.WbSdk;
import com.sina.weibo.sdk.api.ImageObject;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.WeiboMultiMessage;
import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.share.WbShareCallback;
import com.sina.weibo.sdk.share.WbShareHandler;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.modelmsg.WXTextObject;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import de.j4velin.pedometer.Database;
import de.j4velin.pedometer.R;
import de.j4velin.pedometer.ui.Fragment_Overview;
import de.j4velin.pedometer.util.Util;

//This file is added by njucszxy

public class ShareActivity extends Activity implements WbShareCallback {

    private int since_boot,goal,total_days,total_start,todayOffset,steps_today;
    String UserMotto;
    WbShareHandler shareHandler;
    public static final String TAG = "Pedometer.ShareActivity";
    private IWXAPI wxApi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_share);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        Database db = Database.getInstance(ShareActivity.this);
        since_boot = db.getCurrentSteps();
        todayOffset = db.getSteps(Util.getToday());
        SharedPreferences prefs = (ShareActivity.this).getSharedPreferences("pedometer", Context.MODE_PRIVATE);
        goal = prefs.getInt("goal", Fragment_Settings.DEFAULT_GOAL);
        int pauseDifference = since_boot - prefs.getInt("pauseCount", since_boot);
        since_boot -= pauseDifference;
        steps_today = Math.max(todayOffset + since_boot, 0);
        total_start = db.getTotalWithoutToday() + steps_today;
        total_days = db.getDays();
        db.close();

        TextView myText = (TextView)findViewById(R.id.Text);
        myText.setText("今日目标:" + goal + "\n当前已走步数:" + steps_today + "\n运动天数:" + total_days + "\n总步数:" + total_start);

        Button History = (Button)findViewById(R.id.history);
        History.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View view) {
                                           Intent intent = new Intent(ShareActivity.this,ShareHistory.class);
                                           startActivity(intent);
                                       }
                                   }
        );

        EditText Motto = (EditText)findViewById(R.id.Mottor);
        Motto.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                UserMotto = editable.toString();
            }
        });

        Button StartWeibo = (Button)findViewById(R.id.weibo);
        StartWeibo.setOnClickListener(new View.OnClickListener() {
                                       @Override
                                       public void onClick(View view) {
                                           recordShare(0);
                                           sendMessageToWb(true,false,-1);
                                       }
                                   }
        );

        initWeiBo();

        wxApi = WXAPIFactory.createWXAPI(this, Constants.WX_APP_ID);
        wxApi.registerApp(Constants.WX_APP_ID);

        Button StartWechat = (Button)findViewById(R.id.wechat);
        StartWechat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                recordShare(1);
                shareOnWechat();
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                break;
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initWeiBo() {
        WbSdk.install(this,new AuthInfo(this, Constants.APP_KEY,Constants.REDIRECT_URL, Constants.SCOPE));//创建微博API接口类对象
        shareHandler = new WbShareHandler(this);
        shareHandler.registerApp();
    }

    private void sendMessageToWb(boolean hasText, boolean hasImage,int id) {
        sendMultiMessage(hasText, hasImage, id);
    }

    private void sendMultiMessage(boolean hasText, boolean hasImage,int id) {

        WeiboMultiMessage weiboMessage = new WeiboMultiMessage();
        if (hasText) {
            weiboMessage.textObject = getTextObj();
        }
        if (hasImage) {
            weiboMessage.imageObject = getImageObj(this,id);
        }
        shareHandler.shareMessage(weiboMessage, false);
    }

    private TextObject getTextObj() {
        TextObject textObject = new TextObject();
        textObject.text = UserMotto;
        textObject.title = "Pedometer";
        textObject.actionUrl = "http://www.j4velin.de";
        return textObject;
    }

    private ImageObject getImageObj(Context context,int id) {
        ImageObject imageObject = new ImageObject();
        Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), id);
        imageObject.setImageObject(bitmap);
        return imageObject;
    }

    @Override
    public void onWbShareSuccess() {
        Log.d(TAG,"分享成功");
    }

    @Override
    public void onWbShareCancel() {
        Log.d(TAG,"分享取消");
    }

    @Override
    public void onWbShareFail() {
        Log.d(TAG,"分享失败");
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        shareHandler.doResultIntent(intent,this);
    }

    private void shareOnWechat()
    {
        WXTextObject text = new WXTextObject(UserMotto);
        WXMediaMessage msg = new WXMediaMessage(text);
        msg.title = "Pedometer";
        msg.description = "我的运动历程";
        SendMessageToWX.Req req = new SendMessageToWX.Req();
        req.transaction = String.valueOf(System.currentTimeMillis());
        req.message = msg;
        req.scene = SendMessageToWX.Req.WXSceneTimeline;
        wxApi.sendReq(req);
    }

    private void recordShare(int platform)
    {
        if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            //SD卡可挂载
            //获取扩展存储设备的文件目录
            File rootFile = Environment.getExternalStorageDirectory();
            String tmpFilePath = rootFile.getPath() + "/PedometerShareRecord";
            File tmpFile = new File(tmpFilePath);
            if (!tmpFile.exists()) {
                //创建文件夹
                tmpFile.mkdir();
            } else {
                Log.i(TAG, "文件夹已存在");
            }
            //创建文件
            File txtFile = new File (tmpFilePath, "Record.txt");
            if (!txtFile.exists()) {
                try {
                    FileOutputStream outputStream = new FileOutputStream(txtFile);
                    outputStream.write("".getBytes());
                    outputStream.flush();
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //写入文件
            String info = "s" + total_days + "/" + total_start + "/" + goal + "/" + steps_today + "/" + platform + "e" + "\n";
            try {
                FileInputStream is = new FileInputStream(txtFile);
                byte[] b = new byte[is.available()];
                is.read(b);
                String result = new String(b);
                info = result + info;

                FileOutputStream out = new FileOutputStream(txtFile);
                out.write(info.getBytes());
                out.close();
            } catch (Exception e) {
                e.printStackTrace();
            }

        }
    }
}
