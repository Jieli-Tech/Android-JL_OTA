package com.jieli.otasdk.util;

import java.io.File;
import java.io.FileFilter;

/**
 * @ClassName: CustomFileFilter
 * @Description: java类作用描述
 * @Author: ZhangHuanMing
 * @CreateDate: 2022/4/1 16:14
 */
public class CustomFileFilter implements FileFilter {
    @Override
    public boolean accept(File pathname) {
        if (pathname.isDirectory()) {
            return true;
        } else
            return pathname.getName().endsWith(".ufw") || pathname.getName().endsWith(".UFW")
                    || pathname.getName().endsWith(".txt") || pathname.getName().endsWith(".TXT");
    }
}
