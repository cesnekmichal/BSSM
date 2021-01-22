import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * file lacation: /opt/bssm.java
 * @author Cesnek Michal
 */
public class bssm {

    public static void main(String[] args) {
        if(args==null || args.length==0) {
            help.doAction();
            return;
        }
        for (String arg : args) {
            for (Option o : options) {
                o.loadAndDo(new String[]{arg});
            }
        }
    }
    
    public static final List<Option> options = new ArrayList<>();
    public static final Option help;
    
    static {
        help = new Option("help","Show all options"){{
            action = ()->{
                System.out.println("<-------------------------------------------------->");
                for (Option option : options) {
                    System.out.println(option);
                }
                System.out.println("<-------------------------------------------------->");
            };
        }};
        options.add(help);
    }
                
    public static String root_path = "/srv/dev-disk-by-label-data/";
    static {
        options.add(new Option("RP","(R)oot full (P)ath to BTRFS file system","/srv/dev-disk-by-label-data/"){{
            action = ()->{
                root_path = params[0];
                if(!root_path.endsWith("/")) root_path += "/";
            };
        }});    
    }
    
    public static String subvolume_path = "data/";
    static {
        options.add(new Option("SP","(S)ubvolume (P)ath as source snapshooted directory","data/"){{
            action = ()->{
                subvolume_path = params[0];
                if(!subvolume_path.endsWith("/")) subvolume_path += "/";
            };
        }});
    }
    
    //Vychozi plna cesta k subvolume adresari
    public static String btrfs_subvolume_full_path(){
        return root_path + subvolume_path;
    }
    
    //Adresar pro snapshoty
    public static String snapshots_path = ".snapshots/";
    static {
        options.add(new Option("SSP","(S)ubvolume (S)napshots (P)ath as target directory",".snapshots/"){{
            action = ()->{
                snapshots_path = params[0];
                if(!snapshots_path.endsWith("/")) snapshots_path += "/";
            };
        }});
    }

    //Maximalni pocet rocnich snapshotu
    public static int Ymax = 10;
    static {
        options.add(new Option("Ymax","Maximum of year snapshots","10"){{
            action = ()->{
                Ymax = toInt(params[0], Ymax);
            };
        }});
    }
    
    //Maximalni pocet mesicnich snapshotu
    public static int Mmax = 12;
    static {
        options.add(new Option("Mmax","Maximum of month snapshots","12"){{
            action = ()->{
                Mmax = toInt(params[0], Mmax);
            };
        }});
    }
    
    //Maximalni pocet dennich snapshotu
    public static int Dmax = 31;
    static {
        options.add(new Option("Dmax","Maximum of day snapshots","31"){{
            action = ()->{
                Mmax = toInt(params[0], Mmax);
            };
        }});
    }
    
    static {
        options.add(new Option("doSnapshots","Make year, month and day snapshots now"){{
            action = ()->{
                doSnapshots();
            };
        }});
    }
    
    static {
        options.add(new Option("list","List subvolumes and snapshots"){{
            action = ()->{
                for (String btrfs_subvolume_name : getBtrfsSubvolumeAndSnapshotsNames(root_path)) {
                    System.out.println(btrfs_subvolume_name);
                }
            };
        }});
        options.add(new Option("snapshot","Make custom snapshot in snapshot dir","custom_name"){{
            action = ()->{
                cmd_btrfs_subvolume_snapshot_create(btrfs_subvolume_full_path(),snapshots_path+params[0]);
            };
        }});
        options.add(new Option("subvolume","Make custom subvolume in root dir","custom_name"){{
            action = ()->{
                cmd_btrfs_subvolume_create(root_path+params[0]);
            };
        }});
        options.add(new Option("delete","Delete subvolume/snapshot","custom_name"){{
            action = ()->{
                cmd_btrfs_subvolume_or_snapshot_delete(root_path+params[0]);
            };
        }});
        options.add(new Option("manage","Managed delete subvolume/snapshot","custom_name"){{
            action = ()->{
                Scanner sc = new Scanner(System.in);
                System.out.println("");
                while(true){
                    List<String> btrfs_subvolume_names = getBtrfsSubvolumeAndSnapshotsNames(root_path);
                    if(btrfs_subvolume_names.isEmpty()){
                        System.out.println("None Subvolume/snapshots!");
                        break;
                    }
                    System.out.println("[N] Subvolume/snapshot name.");
                    for (int i = 1; i <= btrfs_subvolume_names.size(); i++) {
                        String btrfs_subvolume_name = btrfs_subvolume_names.get(i-1);
                        System.out.println("["+i+"] "+btrfs_subvolume_name);
                    }
                    System.out.println("[0] Exit.");
                    System.out.print("Enter Subvolume/snapshot number for delete [N]:");
                    String userOption = sc.nextLine();
                    int i = toInt(userOption, 0);
                    i = i<1 || i>btrfs_subvolume_names.size() ? 0 : i;
                    if(i==0) break;
                    cmd_btrfs_subvolume_or_snapshot_delete(root_path+btrfs_subvolume_names.get(i-1));
                    System.out.println("");
                }
            };
        }});
    }
    
    public static int toInt(String s,int defaultValue){
        try {
            return Integer.valueOf(s);
        } catch (NumberFormatException ex) {
            return defaultValue;
        }
    }
    
    /** Vycte seznam vsech subvolumu i snapshotu! */
    public static String cmd_btrfs_subvolume_list(String btrfs_subvolume_path){
        return cmd.call(new String[]{"/bin/bash","-c","sudo btrfs subvolume list -t --sort=path "+btrfs_subvolume_path});
    }

    /** Vycte seznam vsech subvolumu mimo snapshoty! */
    public static List<String> getBtrfsSubvolumeAndSnapshotsNames(String btrfs_subvolume_path){
        String cmd_out = cmd_btrfs_subvolume_list(btrfs_subvolume_path);
        //Zjistime si seznam vsech subvolumes
        return extractSubvolumeNames(cmd_out);
    }
    
    /** Vycte seznam vsech subvolumu mimo snapshoty! */
    public static List<String> getBtrfsSubvolumeNames(String btrfs_subvolume_path){
        String cmd_out = cmd_btrfs_subvolume_list(btrfs_subvolume_path);
        //Zjistime si seznam vsech subvolumes
        List<String> all_subvolumes = extractSubvolumeNames(cmd_out);
        cmd_out = cmd_btrfs_subvolume_snapshot_list(btrfs_subvolume_path);
        //Zjistime si seznam vsech snapshots
        List<String> only_snapshots = extractSubvolumeSnapshotNames(cmd_out);
        //Odstranime vsechny snapshoty, cimz ziskame seznam pouze subvolumu mimo snapshoty!
        all_subvolumes.removeAll(only_snapshots);
        return all_subvolumes;
    }
    
    /** Zjisti snapshoty tj. ty co jsou pouze pro cteni! */
    public static String cmd_btrfs_subvolume_snapshot_list(String btrfs_subvolume_path){
        return cmd.call(new String[]{"/bin/bash","-c","sudo btrfs subvolume list -t -s --sort=path "+btrfs_subvolume_path});
    }
    
    /** Vraci seznam snapshotu. */
    public static List<String> getBtrfsSubvolumeSnapshotNames(String btrfs_subvolume_path){
        String cmd_out = cmd_btrfs_subvolume_snapshot_list(btrfs_subvolume_path);
        return extractSubvolumeSnapshotNames(cmd_out);
    }
    
    private static List<String> extractSubvolumeNames(String cmd_out){
        /*
            ID      gen     top level       path
            --      ---     ---------       ----
            306     425     5               rock
            313     288     306             rock/.snapshots/2019
            321     422     306             rock/.snapshots/2020
            312     287     306             rock/.snapshots/2020.02
            311     286     306             rock/.snapshots/2020.03
            318     407     5               rockX
            319     408     5               rockXY
        */
        String[] lines = cmd_out.split("\r|\n|\r\n");
        List<String> list = new ArrayList<>();
        for (String line : lines) {
            Pattern p = Pattern.compile("^\\d+\\s+\\d+\\s+\\d+\\s+(.*)$");
            Matcher m = p.matcher(line);
            if(m.matches() && m.groupCount()>0){
                list.add(m.group(1));
            }
        }
        return list;
    }
    
    private static List<String> extractSubvolumeSnapshotNames(String cmd_out){
        /*
            ID      gen     cgen    top level       otime   path
            --      ---     ----    ---------       -----   ----
            313     288     288     306             2020-03-24 20:59:27     .snapshots/2019
            309     280     280     306             2020-03-24 20:46:58     .snapshots/2020
            312     287     287     306             2020-03-24 20:58:10     .snapshots/2020.02
            311     286     286     306             2020-03-24 20:55:23     .snapshots/2020.03
        */    
        String[] lines = cmd_out.split("\r|\n|\r\n");
        List<String> list = new ArrayList<>();
        for (String line : lines) {
            Pattern p = Pattern.compile("^\\d+\\s+\\d+\\s+\\d+\\s+\\d+\\s+\\d{4}-\\d{2}-\\d{2}\\s\\d{2}:\\d{2}:\\d{2}\\s+(.*)$");
            Matcher m = p.matcher(line);
            if(m.matches() && m.groupCount()>0){
                list.add(m.group(1));
            }
        }
        return list;
    }
    
    public static void printBtrfsSubvolumeSnapshotNames(String btrfs_subvolume_full_path){
        getBtrfsSubvolumeSnapshotNames(btrfs_subvolume_full_path).forEach((l)->System.out.println(l));
    }
    
    /** Vola prikaz pro odstraneni "btrfs subvolume" nebo "btrfs subvolume snapshot" */
    public static String cmd_btrfs_subvolume_or_snapshot_delete(String btrfs_subvolume_full_path){
        System.out.println("delete subvolume/snapshot: "+btrfs_subvolume_full_path);
        return cmd.call(new String[]{"/bin/bash","-c","sudo btrfs subvolume delete -c '"+btrfs_subvolume_full_path+"'"});
    }
    
    /** Vola prikaz pro odstraneni "btrfs subvolume" */
    public static String cmd_btrfs_subvolume_snapshot_create(String src_btrfs_subvolume_full_path,String target_btrfs_snapshot_fullpath){
        System.out.println("create subvolume snapshot: "+target_btrfs_snapshot_fullpath);
        //sudo btrfs subvolume snapshot -r /srv/dev-disk-by-label-data/data/ /srv/dev-disk-by-label-data/data/.snapshots/2020
        return cmd.call(new String[]{"/bin/bash","-c","sudo btrfs subvolume snapshot -r '"+src_btrfs_subvolume_full_path+"' '"+target_btrfs_snapshot_fullpath+"'"});
    }
    
    /** Vola prikaz pro vytvoreni "btrfs subvolume" */
    public static String cmd_btrfs_subvolume_create(String btrfs_subvolume_full_path){
        System.out.println("create subvolume: "+btrfs_subvolume_full_path);
        //sudo btrfs subvolume create /srv/dev-disk-by-label-data/data
        return cmd.call(new String[]{"/bin/bash","-c","sudo btrfs subvolume create '"+btrfs_subvolume_full_path+"'"});
    }
    
    /** Vola prikaz pro vytvoreni "btrfs subvolume" */
    public static String cmd_mkdir(String dir_full_path){
        System.out.println("mkdir: "+dir_full_path);
        //sudo mkdir -p create /srv/dev-disk-by-label-data/data/.snapshot
        return cmd.call(new String[]{"/bin/bash","-c","sudo mkdir -p create '"+dir_full_path+"'"});
    }
    
    public static List<String> getYearSnapshots(List<String> snapshots_all,String snapshot_dir){
        List<String> months = new ArrayList<>();
        //.snapshots/2020
        //.snapshots/2020.03
        //.snapshots/2020.03.28
        if(snapshot_dir==null) snapshot_dir = ".snapshots/";
        String regex = "^"+Pattern.quote(snapshot_dir)+"\\d{4}$";
        Pattern p = Pattern.compile(regex);
        for (String snapshot : snapshots_all) {
            Matcher m = p.matcher(snapshot);
            if(m.matches()){
                months.add(snapshot);
            }
        }
        return months;
    }
    public static List<String> getMonthSnapshots(List<String> snapshots_all,String snapshot_dir){
        List<String> months = new ArrayList<>();
        //.snapshots/2020
        //.snapshots/2020.03
        //.snapshots/2020.03.28
        if(snapshot_dir==null) snapshot_dir = ".snapshots/";
        String regex = "^"+Pattern.quote(snapshot_dir)+"\\d{4}\\.\\d{2}$";
        Pattern p = Pattern.compile(regex);
        for (String snapshot : snapshots_all) {
            Matcher m = p.matcher(snapshot);
            if(m.matches()){
                months.add(snapshot);
            }
        }
        return months;
    }
    
    public static List<String> getDaySnapshots(List<String> snapshots_all,String snapshot_dir){
        
        List<String> months = new ArrayList<>();
        //.snapshots/2020
        //.snapshots/2020.03
        //.snapshots/2020.03.28
        if(snapshot_dir==null) snapshot_dir = ".snapshots/";
        String regex = "^"+Pattern.quote(snapshot_dir)+"\\d{4}\\.\\d{2}.\\d{2}$";
        Pattern p = Pattern.compile(regex);
        for (String snapshot : snapshots_all) {
            Matcher m = p.matcher(snapshot);
            if(m.matches()){
                months.add(snapshot);
            }
        }
        return months;
    }
    
    /**
     * -RP=
     * @param args the command line arguments
     */
    public static void doSnapshots(){
        System.out.println("Doing Snapshots "+getDate());
        List<String> subvolumes = getBtrfsSubvolumeNames(root_path);
        String btrfs_subvolume_name = subvolume_path.replace("/","");
        //Pokud neexistuje btrfs subvolume musime jej zalozit
        if(!subvolumes.contains(btrfs_subvolume_name)){
            cmd_btrfs_subvolume_create(btrfs_subvolume_full_path());
        }
        cmd_mkdir(btrfs_subvolume_full_path()+snapshots_path);
        
        List list = getBtrfsSubvolumeSnapshotNames(btrfs_subvolume_full_path());
        
        Calendar now = Calendar.getInstance();

        //1. Pokud existuje snapshot pro tento rok, tak jej smaz a vytvor znovu
        int year = now.get(Calendar.YEAR);
        String year_snapshot_name = snapshots_path+String.format("%04d",year);
        String year_snapshot_full_name = btrfs_subvolume_full_path()+year_snapshot_name;
        boolean year_snapshot_exists = list.contains(year_snapshot_name);
        if(year_snapshot_exists){
            cmd_btrfs_subvolume_or_snapshot_delete(year_snapshot_full_name);
        }
        cmd_btrfs_subvolume_snapshot_create(btrfs_subvolume_full_path(), year_snapshot_full_name);
        
        //1.1 Pokud existuje vice nez X rocnich snapshotu, pak ty nadlimitni nejstarsi odstran.
        List<String> snapshots_all = getBtrfsSubvolumeSnapshotNames(btrfs_subvolume_full_path());
        List<String> yearSnapshots = getYearSnapshots(snapshots_all,snapshots_path);
        if(yearSnapshots.size()>Ymax){
            for (int i = 0; i < yearSnapshots.size()-Ymax; i++) {
                String year_snapshot_name_old = yearSnapshots.get(i);
                String year_snapshot_full_name_old = btrfs_subvolume_full_path()+year_snapshot_name_old;
                cmd_btrfs_subvolume_or_snapshot_delete(year_snapshot_full_name_old);
            }
        }        
        
        //2. Pokud existuje snapshot pro tento mesic, tak jej smaz a zaloz novy
        int month = now.get(Calendar.MONTH)+1;
        String month_snapshot_name = snapshots_path+String.format("%04d",year)+"."+String.format("%02d",month);
        String month_snapshot_full_name = btrfs_subvolume_full_path()+month_snapshot_name;
        boolean month_snapshot_exists = list.contains(month_snapshot_name);
        if(month_snapshot_exists){
            cmd_btrfs_subvolume_or_snapshot_delete(month_snapshot_full_name);
        }
        cmd_btrfs_subvolume_snapshot_create(btrfs_subvolume_full_path(), month_snapshot_full_name);
        
        //2.1 Pokud existuje vice nez 12 mesicnich snapshotu, pak ty nadlimitni nejstarsi odstran.
        List<String> monthSnapshots = getMonthSnapshots(snapshots_all,snapshots_path);
        if(monthSnapshots.size()>Mmax){
            for (int i = 0; i < monthSnapshots.size()-Mmax; i++) {
                String month_snapshot_name_old = monthSnapshots.get(i);
                String month_snapshot_full_name_old = btrfs_subvolume_full_path()+month_snapshot_name_old;
                cmd_btrfs_subvolume_or_snapshot_delete(month_snapshot_full_name_old);
            }
        }
        
        //3. Pokud existuje snapshot pro tento den, tak jej smaz a zaloz novy
        int day = now.get(Calendar.DAY_OF_MONTH);
        String day_snapshot_name = snapshots_path+String.format("%04d",year)+"."+String.format("%02d",month)+"."+String.format("%02d",day);
        String day_snapshot_full_name = btrfs_subvolume_full_path()+day_snapshot_name;
        boolean day_snapshot_exists = list.contains(day_snapshot_name);
        if(day_snapshot_exists){
            cmd_btrfs_subvolume_or_snapshot_delete(day_snapshot_full_name);
        }
        cmd_btrfs_subvolume_snapshot_create(btrfs_subvolume_full_path(), day_snapshot_full_name);
        
        //3.1 Pokud existuje vice nez 31 dennich snapshotu, pak ty nejstarsi odstran.
        snapshots_all = getBtrfsSubvolumeSnapshotNames(btrfs_subvolume_full_path());
        List<String> daySnapshots = getDaySnapshots(snapshots_all,snapshots_path);
        if(daySnapshots.size()>Dmax){
            for (int i = 0; i < daySnapshots.size()-Dmax; i++) {
                String day_snapshot_name_old = daySnapshots.get(i);
                String day_snapshot_full_name_old = btrfs_subvolume_full_path()+day_snapshot_name_old;
                cmd_btrfs_subvolume_or_snapshot_delete(day_snapshot_full_name_old);
            }
        }
        
    }
    
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    public static String getDate(){
        return sdf.format(new Date());
    }    
    
    public static class regex{
        public static void main(String[] args) throws IOException {
            String in = IS.toString(System.in);
            String regex = args[0];
            Pattern p = Pattern.compile(regex);
            for (String line : in.split("\r\n|\r|\n")) {
                Matcher m = p.matcher(line);
                if(m.matches()){
                    System.out.println(m.group(1));
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
        public static String call(String[] cmds) {
            try {
                Process p = Runtime.getRuntime().exec(cmds);
                String sout = IS.toString(p.getInputStream());
                String serr = IS.toString(p.getErrorStream());
                if(serr!=null && !serr.isBlank()) System.err.println(serr);
                return sout+serr;
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
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
    
    /**
     * Syntax example: <br/>
     * -name="first parameter",'secondParameter',thirdParameter <br/>
     */
    public static class Option{
        String name;
        String description;
        String[] params_default;
        String[] params;
        boolean loaded = false;
        Runnable action = null;
        public Option(String name, String description,String... params) {
            this.name = name;
            this.description = description;
            this.params_default = params;
            //Clone orig param array to current params array
            this.params = Arrays.stream(this.params_default).toArray(String[]::new);
        }
//        /** Nastavi akci. */
//        public Option toDo(Function<Option,Void> action){
//            this.action = action;
//            return this;
//        }
        /** Vycte se z pole parametru. Pokud je vycten, pak provede prednastavenou akci. */
        public void loadAndDo(String[] args){
            if(load(args)){
                doAction();
            }
        }
        /** Provede prednastavenou akci. */
        public void doAction(){
            if(action!=null) action.run();
        }
        /** Vycte se z pole parametru. Pokud je uspesne vycten pak vraci true, jinak false. */
        public boolean load(String[] args){
            if(args==null) return false;
            for (String arg : args) {
                if(arg==null) continue;
                if(arg.startsWith("-"+name)){
                    //Clone orig param array to current params array
                    this.params = Arrays.stream(this.params_default).toArray(String[]::new);
                    if(arg.startsWith("-"+name+"=")){
                        String ss = arg.substring(name.length()+2);
                        String[] params2 = ss.split(",",-1);
                        for (int i = 0; i < params.length; i++) {
                            if(params2.length>i) {
                                params[i] = params2[i].replaceAll("((?<=^)'|'(?=$))", "").replaceAll("((?<=^)\"|\"(?=$))", "");
                            }
                        }
                    }
                    loaded = true;
                    return true;
                }
            }
            return false;
        }
        @Override
        public String toString() {
            String ps = Arrays.stream(params).collect(Collectors.joining(","));
            return "-"+name+(!ps.isEmpty()?"="+ps+"\t"+(loaded?"":"(default)"):"\t")+"\t"+"["+description+"]";
        }
        public void print(){
            System.out.println(this);
        }
    }
    
}
