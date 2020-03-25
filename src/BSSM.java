
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Česnek Michal
 */
public class BSSM {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        if(args!=null && args.length>0 && args[0]!=null){
            String subclass = args[0];
            if(regex.class.getSimpleName().equals(subclass)){
                regex.main(Arrays.copyOfRange(args, 1, args.length));
                return;
            } else if(IS.class.getSimpleName().equals(subclass)){
                IS.main(Arrays.copyOfRange(args, 1, args.length));
                return;
            } else if(cmd.class.getSimpleName().equals(subclass)){
                cmd.main(Arrays.copyOfRange(args, 1, args.length));
                return;
            }
        } else {
            System.out.println("Ahoj světe");
            for (String arg : args) {
                System.out.println(arg);
            }
        }
    }
    
    public static class regex{
        public static void main(String[] args) throws IOException {
            String in = IS.toString(System.in);
            String regex = args[0];
            Pattern p = Pattern.compile(regex);
            for (String line : in.split("\r\n|\r|\n")) {
                Matcher m = p.matcher(line);
                if(m.find()){
                    System.out.println(m.group(0));
                }
            }
        }
    }
    
    public static class cmd{
        public static void main(String[] args) throws IOException {
            Process p = Runtime.getRuntime().exec(args);
            System.out.println(IS.toString(p.getInputStream()));
            System.err.println(IS.toString(p.getErrorStream()));
        }
    }
    
    public static class IS {
        
        public static void main(String[] args) throws IOException {
            String in = toString(System.in);
            System.out.println(in);
            for (String arg : args) {
                System.out.println(arg);
            }
        }
        
        public static String toString(InputStream is) throws IOException{
            StringBuilder textBuilder = new StringBuilder();
            try (Reader reader = new BufferedReader(new InputStreamReader(is, Charset.forName(StandardCharsets.UTF_8.name())))) {
                int c = 0;
                while ((c = reader.read()) != -1) {
                    textBuilder.append((char) c);
                }
            }
            return textBuilder.toString();
        }
    }
    
}
