package csp;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

/**
 * 木材切割算例.
 * 
 * @author Xiong Wangqi
 * @version V1.0
 * @since JDK1.8
 */
class Csp {
    /** 原木料长度. */
    private final double rollLength;
    /** 各类木材的要求长度. */
    private final double[] size;
    /** 各类木材的要求数量. */
    private final double[] amount;

    /**
     * 读取算例数据创建 CutStock Problem 实例.
     */
    Csp(String filename) throws IOException {
        BufferedReader bfr = new BufferedReader(new FileReader(filename));
        ArrayList<String> data = new ArrayList<String>();

        String line;
        while ((line = bfr.readLine()) != null) {
            data.add(line.trim());
        }

        rollLength = Double.parseDouble(data.get(0));

        String[] sizeStr = data.get(1).split("\\s+");
        String[] amountStr = data.get(2).split("\\s+");

        int kinds = sizeStr.length;
        size = new double[kinds];
        amount = new double[kinds];

        for (int i = 0; i < sizeStr.length; i++) {
            size[i] = Double.parseDouble(sizeStr[i]);
            amount[i] = Double.parseDouble(amountStr[i]);
        }
        bfr.close();
    }
    
    double getRollLength() {
        return rollLength;
    }
    
    double[] getSize() {
        return size;
    }
    
    double[] getAmount() {
        return amount;
    }

}
