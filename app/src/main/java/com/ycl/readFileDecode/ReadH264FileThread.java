package com.ycl.readFileDecode;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;


public class ReadH264FileThread extends Thread {
    //解码器
    private MediaCodecUtil util;
    //文件路径
    private String path;
    //文件读取完成标识
    private boolean isFinish = false;
    //这个值用于找到第一个帧头后，继续寻找第二个帧头，如果解码失败可以尝试缩小这个值
    private int FRAME_MIN_LEN = 1024;
    //一般H264帧大小不超过200k,如果解码失败可以尝试增大这个值
    private static int FRAME_MAX_LEN = 100 * 1024;
    //根据帧率获取的解码每帧需要休眠的时间,根据实际帧率进行操作
    private int PRE_FRAME_TIME = 1000 / 25;

    /**
     * 初始化解码器
     *
     * @param util 解码Util
     * @param path 文件路径
     */
    public ReadH264FileThread(MediaCodecUtil util, String path) {
        this.util = util;
        this.path = path;
    }

    /**
     * 寻找指定buffer中h264头的开始位置
     *
     * @param data   数据
     * @param offset 偏移量
     * @param max    需要检测的最大值
     * @return h264头的开始位置 ,-1表示未发现
     */
    private int findHead(byte[] data, int offset, int max) {
        int i;
        for (i = offset; i <= max; i++) {
            //发现帧头
            if (isHead(data, i))
                break;
        }
        //检测到最大值，未发现帧头
        if (i == max) {
            i = -1;
        }
        return i;
    }

    /**
     * 判断是否是I帧/P帧头:
     * 00 00 00 01 65    (I帧)
     * 00 00 00 01 61 / 41   (P帧)
     *
     * @param data
     * @param offset
     * @return 是否是帧头
     */
    private boolean isHead(byte[] data, int offset) {
        boolean result = false;
        // 00 00 00 01
        if (data[offset] == 0x00 && data[offset + 1] == 0x00
                && data[offset + 2] == 0x00 && data[3] == 0x01 && isVideoFrameHeadType(data[offset + 4])) {
            result = true;
        }
        // 00 00 01
        if (data[offset] == 0x00 && data[offset + 1] == 0x00
                && data[offset + 2] == 0x01 && isVideoFrameHeadType(data[offset + 3])) {
            result = true;
        }
        return result;
    }

    /**
     * I帧或者P帧
     */
    private boolean isVideoFrameHeadType(byte head) {
        return head == (byte) 0x65 || head == (byte) 0x61 || head == (byte) 0x41;
    }

    @Override
    public void run() {
        super.run();
        decode();
    }

    private void decode2(){
        File file = new File(path);
        FileInputStream fis = null;
        //判断文件是否存在
        if (file.exists()) {
            try {
                fis = new FileInputStream(file);
                //保存完整数据帧
                byte[] frame = new byte[FRAME_MAX_LEN];
                //当前帧长度
                int frameLen = 0;
                //每次从文件读取的数据
                byte[] readData = new byte[10 * 1024];
                //开始时间
                long startTime = System.currentTimeMillis();
                //循环读取数据
                while (!isFinish) {
                    if (fis.available() > 0) {
                        int readLen = fis.read(readData);
                        System.out.println("读取数据：");
                        //当前长度小于最大值
                        if (readLen > 0) {
                            //将readData拷贝到frame
                            System.arraycopy(readData, 0, frame, frameLen, readLen);
                            //寻找第一个帧头
                            frameLen = frameLen + readLen;
                            //开始解析协议
                            int headFirstIndex = -1;
                            int count = 0;
                            while((headFirstIndex = findHead(frame, 0, frameLen))>=0) {
                                if (headFirstIndex >= 0 && isHead(frame, headFirstIndex)){
                                    //寻找第二个帧头
                                    int headSecondIndex = findHead(frame, headFirstIndex + 4, frameLen);
                                    //如果第二个帧头存在，则两个帧头之间的就是一帧完整的数据
                                    if (headSecondIndex > 0 && isHead(frame, headSecondIndex)) {
//                                    Log.e("ReadH264FileThread", "headSecondIndex:" + headSecondIndex);
                                        //视频解码
                                        System.out.println("第"+count+"解析成功");
                                        onFrame(frame, headFirstIndex, headSecondIndex - headFirstIndex);
                                        //截取headSecondIndex之后到frame的有效数据,并放到frame最前面
                                        byte[] temp = Arrays.copyOfRange(frame, headSecondIndex, frameLen);
                                        System.arraycopy(temp, 0, frame, 0, temp.length);
                                        //修改frameLen的值
                                        frameLen = temp.length;
                                        //线程休眠
                                        sleepThread(startTime, System.currentTimeMillis()+100);
                                        //重置开始时间
                                        startTime = System.currentTimeMillis();
                                    } else {
                                        //找不到第二个帧头
                                        System.out.println("找不到第二个帧头，开始下次解析");
                                        break;
                                    }
                                }else{
                                    //找不到第一个帧头
                                    System.out.println("找不到第一个帧头");
                                    break;
                                }
                            }
                        } else {
                            isFinish = true;
                            System.out.println("文件读取结束");
                        }
                    } else {
                        //文件读取结束
                        isFinish = true;
                        System.out.println("文件读取结束");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if(fis != null){
                    try {
                        fis.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        } else {
            Log.e("ReadH264FileThread", "File not found");
        }
    }

    private void decode(){
        File file = new File(path);
        //判断文件是否存在
        if (file.exists()) {
            try {
                FileInputStream fis = new FileInputStream(file);
                //保存完整数据帧
                byte[] frame = new byte[FRAME_MAX_LEN];
                //当前帧长度
                int frameLen = 0;
                //每次从文件读取的数据
                byte[] readData = new byte[10 * 1024];
                //开始时间
                long startTime = System.currentTimeMillis();
                //循环读取数据
                while (!isFinish) {
                    if (fis.available() > 0) {
                        int readLen = fis.read(readData);
                        //当前长度小于最大值
                        if (frameLen + readLen < FRAME_MAX_LEN) {
                            //将readData拷贝到frame
                            System.arraycopy(readData, 0, frame, frameLen, readLen);
                            //修改frameLen
                            frameLen += readLen;
                            //寻找第一个帧头
                            int headFirstIndex = findHead(frame, 0, frameLen);
                            while (headFirstIndex >= 0 && isHead(frame, headFirstIndex)) {
                                //寻找第二个帧头
                                int headSecondIndex = findHead(frame, headFirstIndex + FRAME_MIN_LEN, frameLen);
                                //如果第二个帧头存在，则两个帧头之间的就是一帧完整的数据
                                if (headSecondIndex > 0 && isHead(frame, headSecondIndex)) {
//                                    Log.e("ReadH264FileThread", "headSecondIndex:" + headSecondIndex);
                                    //视频解码
                                    onFrame(frame, headFirstIndex, headSecondIndex - headFirstIndex);
                                    //截取headSecondIndex之后到frame的有效数据,并放到frame最前面
                                    byte[] temp = Arrays.copyOfRange(frame, headSecondIndex, frameLen);
                                    System.arraycopy(temp, 0, frame, 0, temp.length);
                                    //修改frameLen的值
                                    frameLen = temp.length;
                                    //线程休眠
                                    sleepThread(startTime, System.currentTimeMillis());
                                    //重置开始时间
                                    startTime = System.currentTimeMillis();
                                    //继续寻找数据帧
                                    headFirstIndex = findHead(frame, 0, frameLen);
                                } else {
                                    //找不到第二个帧头
                                    headFirstIndex = -1;
                                    System.out.println("找不到第二个帧头");
                                }
                            }
                        } else {
                            //如果长度超过最大值，frameLen置0
                            frameLen = 0;
                        }
                    } else {
                        //文件读取结束
                        isFinish = true;
                        System.out.println("文件读取结束");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e("ReadH264FileThread", "File not found");
        }
    }

    //视频解码
    private void onFrame(byte[] frame, int offset, int length) {
        if (util != null) {
            try {
                util.onFrame(frame, offset, length);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Log.e("MediaCodecRunnable", "mediaCodecUtil is NULL");
        }
    }

    //修眠
    private void sleepThread(long startTime, long endTime) {
        //根据读文件和解码耗时，计算需要休眠的时间
        long time = PRE_FRAME_TIME - (endTime - startTime);
        if (time > 0) {
            try {
                Thread.sleep(time);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    //手动终止读取文件，结束线程
    public void stopThread() {
        isFinish = true;
    }

    public String byteArrayToHex(byte[] byteArray) {
        // 首先初始化一个字符数组，用来存放每个16进制字符
        char[] hexDigits = {'0','1','2','3','4','5','6','7','8','9', 'A','B','C','D','E','F' };
        // new一个字符数组，这个就是用来组成结果字符串的（解释一下：一个byte是八位二进制，也就是2位十六进制字符（2的8次方等于16的2次方））
        char[] resultCharArray =new char[byteArray.length * 2];
        // 遍历字节数组，通过位运算（位运算效率高），转换成字符放到字符数组中去
        int index = 0;
        for (byte b : byteArray) {
            resultCharArray[index++] = hexDigits[b>>> 4 & 0xf];
            resultCharArray[index++] = hexDigits[b& 0xf];
        }
        // 字符数组组合成字符串返回
        return new String(resultCharArray);
    }

    /**
     * 将byte数组转换为字符串形式表示的十六进制数方便查看
     */
    public StringBuffer bytesToString(byte[] bytes)
    {
        StringBuffer sBuffer = new StringBuffer();
        for (int i = 0; i < bytes.length; i++)
        {
            String s = Integer.toHexString(bytes[i] & 0xff);
            if (s.length() < 2)
                sBuffer.append('0');
            sBuffer.append(s + " ");
        }
        return sBuffer;
    }


    private byte charToByte(char c)
    {
        return (byte) "0123456789abcdef".indexOf(c);
        // 个人喜好,我喜欢用小写的 return (byte) "0123456789ABCDEF".indexOf(c);
    }

}
