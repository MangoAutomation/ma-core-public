package com.serotonin.io;

/**
 * Acts as both a reader and a writer, in that you can append content and read individual lines from it. This class
 * is not thread safe.
 * 
 * @author Matthew
 */
public class LineReader {
    private final StringBuilder content = new StringBuilder();

    public void append(String s) {
        content.append(s);
    }

    public String readLine() {
        int pos = 0;
        int cutlen = 0;
        while (pos < content.length()) {
            char c = content.charAt(pos);
            if (c == '\n')
                cutlen = 1;
            else if (c == '\r') {
                if (pos + 1 < content.length() && content.charAt(pos + 1) == '\n')
                    cutlen = 2;
                else
                    cutlen = 1;
            }

            if (cutlen > 0) {
                String s = content.substring(0, pos);
                content.delete(0, pos + cutlen);
                return s;
            }

            pos++;
        }

        return null;
    }
    //
    //    public static void main(String[] args) {
    //        LineReader r = new LineReader();
    //        r.append("asdf\nqwer\r\nzxcv\rhjkl");
    //
    //        System.out.println(r.readLine());
    //        System.out.println(r.readLine());
    //        System.out.println(r.readLine());
    //        System.out.println(r.readLine());
    //        System.out.println(r.readLine());
    //    }
}
