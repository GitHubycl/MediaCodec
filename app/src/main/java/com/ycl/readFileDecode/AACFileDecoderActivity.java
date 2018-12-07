package com.ycl.readFileDecode;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.ycl.test.R;


/**
 * 1、DVR音频测试Activity
 */
public class AACFileDecoderActivity extends AppCompatActivity {


    private ReadAACFileThread audioThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aactest);

    }

    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.play_audio_test:
                if (audioThread == null) {
                    audioThread = new ReadAACFileThread();
                    audioThread.start();
                }
                break;
        }
    }
}