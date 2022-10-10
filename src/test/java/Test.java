import org.swdc.filetype.umd.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class Test {


    public static void main(String[] args) throws IOException {

        UMDFile file = new UMDFile(new File("test.umd"));
        file.read();
        System.err.println(file.getChapterText(0));
    }

}
