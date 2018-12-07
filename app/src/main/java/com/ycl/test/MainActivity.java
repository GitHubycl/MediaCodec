package com.ycl.test;

import android.content.Intent;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

import com.ycl.readFileDecode.AACFileDecoderActivity;
import com.ycl.readFileDecode.H264FileDecodeActivity;

import static android.media.MediaCodecList.ALL_CODECS;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        MediaCodecInfo[] mediaCodecInfo = new MediaCodecList(ALL_CODECS).getCodecInfos();
        System.out.println(mediaCodecInfo);
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_video:
                Intent i = new Intent(this, H264FileDecodeActivity.class);
                startActivity(i);
                break;
            case R.id.play_audio:
                Intent i1 = new Intent(this, AACFileDecoderActivity.class);
                startActivity(i1);
                break;
        }
    }
}
