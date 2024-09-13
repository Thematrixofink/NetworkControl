package com.inkslab.networkManager.utils;

import java.io.*;

public class StreamConvertUtil {

    /**
     * 将字节输出流转换为BufferedWriter
     * @param outputStream
     * @return
     */
    public static BufferedWriter toBufferWriter(OutputStream outputStream) throws IOException {
        OutputStreamWriter writer = null;
        try {
            // 将OutputStream转换为Writer
            writer = new OutputStreamWriter(outputStream, "GBK");
        }catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        // 使用BufferedWriter来提高写入性能
        BufferedWriter bufferedWriter = new BufferedWriter(writer);
        return bufferedWriter;
    }


    /**
     * 将字节输入流转换为BufferedReader
     * @param inputStream
     * @return
     * @throws IOException
     */
    public static BufferedReader toBufferReader(InputStream inputStream) throws IOException {
        InputStreamReader reader = null;
        try {
            reader = new InputStreamReader(inputStream,"GBK");
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        BufferedReader bufferedReader = new BufferedReader(reader);
        return bufferedReader;
    }

}
