package compile;

import org.apache.jasper.JspC;

public class JspCompile {
    public static void main(String[] args) throws Exception {
        JspC jspc = new JspC();
        jspc.setUriroot("target/web");
        jspc.setOutputDir("target/work/jsp");
        jspc.setCompile(true);
        jspc.execute();
    }
}
