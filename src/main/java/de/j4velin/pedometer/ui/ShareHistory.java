package de.j4velin.pedometer.ui;

import android.app.Activity;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.os.Environment;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import de.j4velin.pedometer.R;

public class ShareHistory extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.share_history);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        //List<String> list_t = new ArrayList<String>();
        //list_t.add("first");
        //list_t.add("second");
        //list_t.add("third");
        ListView lv = (ListView)findViewById(R.id.lv);
        lv.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1, getData()));
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

    private List<String> getData()
    {
        File rootFile = Environment.getExternalStorageDirectory();
        String tmpFilePath = rootFile.getPath() + "/PedometerShareRecord";
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

        //解析文件
        List<String> list = new ArrayList<String>();
        try {
            //读取
            FileInputStream is = new FileInputStream(txtFile);
            byte[] b = new byte[is.available()];
            is.read(b);
            String result = new String(b);

            //解析
            int begin = -1, end = -1;
            for(int i = 0;i < result.length();i++)
            {
                if(result.charAt(i) == 's')
                {
                    begin = i;
                }
                else if(result.charAt(i) == 'e')
                {
                    end = i;
                    byte[] bTmp = new byte[end - begin];
                    for(int j = begin + 1,m = 0;j < end; j ++,m++)
                        bTmp[m] = (byte)result.charAt(j);
                    bTmp[end - begin - 1] = '/';

                    //解析数据
                    int pattern = 0;
                    byte[] bTmp2 = new byte[end - begin];
                    String display = new String();
                    int count = 0;
                    for(int j = 0;j < end-begin; j++)
                    {
                        if(bTmp[j] == '/')
                        {
                            bTmp2[count] = '\0';
                            String element = new String(bTmp2);
                            if(pattern == 0)
                                display += "Days:" + element + " ";
                            else if(pattern == 1)
                                display += "AllStep:" + element + " ";
                            else if(pattern == 2)
                                display += "TodaysGoal:" + element + " ";
                            else if(pattern == 3)
                                display += "TodaysStep:" + element + " ";
                            else if(pattern == 4)
                            {
                                if(element.charAt(0) == '0')
                                    display += "Platform:Weibo\n";
                                else if(element.charAt(0) == '1')
                                    display += "Platform:Wechat\n";
                            }
                            count = 0;
                            pattern++;
                        }
                        else
                        {
                            bTmp2[count] = bTmp[j];
                            count++;
                        }
                    }
                    list.add(display);
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
