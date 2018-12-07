package com.ycl.readFileDecode;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;



public class AudioRecordThread extends Thread {

    private AudioRecord audioRecord;
    private int m_in_buf_size;
    private boolean isRecording = true;
    private MyAudioTrack audioTrack;
    private static int sampleRateInHz = 44100;

    @Override
    public void run() {
        super.run();
        byte[] audio;
        prepare();
        audio = new byte[m_in_buf_size];
        while (isRecording) {
            //采集
            int length = audioRecord.read(audio, 0, m_in_buf_size);
            //播放
            audioTrack.playAudioTrack(audio, 0, length);
        }
        releaseRecord();
    }

    private void prepare() {
        //初始化采集
        m_in_buf_size = AudioRecord.getMinBufferSize(sampleRateInHz,
                AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_16BIT);
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.DEFAULT, sampleRateInHz,
                AudioFormat.CHANNEL_CONFIGURATION_STEREO,
                AudioFormat.ENCODING_PCM_16BIT, m_in_buf_size);
        audioRecord.startRecording();
        //初始化播放
        audioTrack = new MyAudioTrack(sampleRateInHz, AudioFormat.CHANNEL_CONFIGURATION_STEREO, AudioFormat.ENCODING_PCM_16BIT);
        audioTrack.init();
//        writeFileUtil = new WriteFileUtil(Environment.getExternalStorageDirectory().toString() + "/SONA/test.pcm");
//        writeFileUtil.init();
    }

    private void releaseRecord() {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
        }
        audioRecord = null;
        if (audioTrack != null) {
            audioTrack.release();
        }
    }

    public void stopRecord() {
        isRecording = false;
    }
}
