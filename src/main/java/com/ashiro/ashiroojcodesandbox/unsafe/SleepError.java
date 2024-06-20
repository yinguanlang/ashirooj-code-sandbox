package com.ashiro.ashiroojcodesandbox.unsafe;

/**
 * @author ashiro
 * @description 睡眠阻塞
 */
public class SleepError {
    public static void main(String[] args) throws InterruptedException {
        long ONE_HOUR = 60 * 60 * 1000L;
        Thread.sleep(ONE_HOUR);
        System.out.println("睡眠结束");
    }

}
