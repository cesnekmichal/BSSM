import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
                System.out.println("<-BTRFS-SUBVOLUME-SNAPSHOT-MANAGER-HELP------------>");
                for (Option option : options) {
                    System.out.println(option);
                }
                System.out.println("<-------------------------------------------------->");
                for (Option o1 : options) {
                    for (Option o2 : options) {
                        if(o1!=o2 && o1.name.startsWith(o2.name)){
                            System.err.println("Pozor!!! Nejednoznacne prekryvajici se volby: "+o1.name+" <=> "+o2.name);
                        }
                    }
                }
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
        options.add(new Option("RD","(R)oot (D)evice with BTRFS file system","/dev/nvme0n1p1"){{
            action = ()->{
                root_path = dev_path_2_mountpoint_path(params[0]);
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
        options.add(new Option("listSS","List (s)ubvolumes and (s)napshots"){{
            action = ()->{
                for (String btrfs_subvolume_name : getBtrfsSubvolumeAndSnapshotsNames(root_path)) {
                    System.out.println(btrfs_subvolume_name);
                }
            };
        }});
        options.add(new Option("snapshot","Make custom snapshot in snapshot dir","custom_name"){{
            action = ()->{
                cmd_btrfs_subvolume_snapshot_create(btrfs_subvolume_full_path(),root_path+subvolume_path+snapshots_path+params[0]);
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
        options.add(new Option("readonly","Change all subvolume/snapshots to readonly","true/false/test"){{
            action = ()->{
                switch(params[0]){
                    case "true":
                    case "false":
                        for (String btrfs_subvolume_name : getBtrfsSnapshotsNames(root_path)) {
                            cmd_btrfs_subvolume_or_snapshot_readonly(root_path+btrfs_subvolume_name, params[0], false);
                            String readOnlyStatus = cmd_btrfs_subvolume_or_snapshot_readonly_test(root_path+btrfs_subvolume_name);
                            System.out.println(btrfs_subvolume_name + " [" + readOnlyStatus + "]");
                        }
                        break;
                    case "test":
                        for (String btrfs_subvolume_name : getBtrfsSnapshotsNames(root_path)) {
                            String readOnlyStatus = cmd_btrfs_subvolume_or_snapshot_readonly_test(root_path+btrfs_subvolume_name);
                            System.out.println(btrfs_subvolume_name + " [" + readOnlyStatus + "]");
                        }
                        break;
                    default:
                        System.out.println("Missing or wong value of parameter -readonly=true/false/test");
                }
            };
        }});
    }

    static{
        options.add(new Option.OptionDelimiter("<-RSYNC-------------------------------------------->"));
    }

    public static String remote_storage          = "rsync://data@data.local:873/data/";
    public static String remote_storage_password = "data";
    public static String remote_snapshots        = ".snapshots/";
    public static String[] rsync_excludes        = {".recycle",".snapshots",".watchdog"};
    static{
        options.add(new Option("RRS","(R)emote (r)sync (s)torage with BTRFS file system,password","rsync://data@data.local:873/data/","data"){{
            action = ()->{
                remote_storage          = params[0];
                if(!remote_storage.endsWith("/")) remote_storage += "/";
                remote_storage_password = params[1];
            };
        }});
        options.add(new Option("RSP","(R)emote (S)napshots (P)ath",".snapshots/"){{
            action = ()->{
                remote_snapshots = params[0];
                if(!remote_snapshots.endsWith("/")) remote_snapshots += "/";
            };
        }});
        options.add(new Option("listRS","List (R)emote (S)napshots"){{
            action = ()->{
                for (String remote_snapshot : getRemoteSnapshots()) {
                    System.out.println(remote_snapshot);
                }
            };
        }});
        options.add(new Option("setSyncExcludes","Set Excludes from calling rsync",Stream.of(rsync_excludes).collect(Collectors.joining("|"))){{
            action = ()->{
                rsync_excludes = params[0].split("|");
            };
        }});
        options.add(new Option("syncSnapshots","Sync with all Remote snapshots (dry/full)","dry"){{
            action = ()->{
                fullSync = params[0].equals("full");
                syncRemoteSnapshtos();
            };
        }});
    }

//    static {
//        options.add(new Option("runDev","Running some test development function."){{
//            action = ()->{
//                runDev();
//            };
//        }});          
//    }
//    
//    public static void runDev(){
//        
//    }
    static boolean fullSync = false;
    public static void syncRemoteSnapshtos(){
        List<String> remoteSnapshots = getRemoteSnapshots();
        List<String> localSnapshots = getLocalSnapshots();
        if(remoteSnapshots.isEmpty()) return;
        
        System.out.println("[Sync snapshots]");
        
        if(!fullSync){
            System.out.println("{Local snapshots}: "+root_path+subvolume_path+snapshots_path);
            localSnapshots.forEach((s)->System.out.println(s));
            System.out.println("{Remote snapshots}: "+remote_storage+remote_snapshots);
            remoteSnapshots.forEach((s)->System.out.println(s));
            System.out.println("{---------------}");
        }
        
        //Odstranit nadbytecne snapshoty
        for (String localSnapshot : localSnapshots.toArray(new String[0])) {
            if(!remoteSnapshots.contains(localSnapshot)){
                System.out.println("[Removing unnecessary snapshot: "+localSnapshot+" ]");
                if(fullSync){
                cmd_btrfs_subvolume_or_snapshot_delete(root_path+subvolume_path+snapshots_path+localSnapshot);
                localSnapshots.remove(localSnapshot);
                }
            }
        }
        
        //Odstraníme změněné snapshoty
        // - odstranění provádíme tak, že odstaníme všechny snapshoty od prvního změněného
        boolean anyChanged = false;
        for (String localSnapshot : localSnapshots.toArray(new String[0])) {
            if(!anyChanged)
                System.out.println("[Test snapshot changed: "+localSnapshot+" ]");
            if(!anyChanged && isSnapshotChanged(localSnapshot)){
                anyChanged = true;
            }
            if(anyChanged) {
                System.out.println("[Removing changed snapshot: "+localSnapshot+" ]");
                if(fullSync){
                cmd_btrfs_subvolume_or_snapshot_delete(root_path+subvolume_path+snapshots_path+localSnapshot);
                localSnapshots.remove(localSnapshot);
                }
            }
        }
        
        //Synchronizujeme snapshoty
        for (String remoteSnapshot : remoteSnapshots) {
            if(localSnapshots.contains(remoteSnapshot)) continue;
            System.out.println("[Sync snapshot: "+remoteSnapshot+" ]");
            if(fullSync){
            //Sync remote snapshot to local root dir
            syncRemoteSnapshotToLocalRootDir(remoteSnapshot);
            }
            System.out.println("[Create snapshot: "+remoteSnapshot+" ]");
            if(fullSync){
            //Create snapshot
            cmd_btrfs_subvolume_snapshot_create(btrfs_subvolume_full_path(),root_path+subvolume_path+snapshots_path+remoteSnapshot);
            }
        }
        
        System.out.println("[Sync current data]");
        if(fullSync)
        //Synchronizujeme aktualni stav
        syncRemoteRootDirToLocalRootDir();
        
    }
    
    private static void syncRemoteRootDirToLocalRootDir(){
        String[] exclude_params = !isEmpty(rsync_excludes) ? Stream.of(rsync_excludes).map((e)->"--exclude="+e+"").toArray(String[]::new) : null;
        //Sample call rsync as user data
        //sudo -u data -i rsync -rtP --delete --force rsync://data@data.local:873/data/ /srv/dev-disk-by-label-data/data/ --log-file /var/log/rsync.log
        cmd.call2(flat(new String[][]{{"sudo","-u","data","-i","rsync","-rtP","--delete","--force"},exclude_params,{remote_storage,root_path+subvolume_path},{"--log-file","/var/log/rsync.log"}}),
                          new String[]{"RSYNC_PASSWORD="+remote_storage_password});//env variable
    }
    
    private static void syncRemoteSnapshotToLocalRootDir(String snapshotName){
        if(snapshotName==null || snapshotName.isBlank()) return;
        if(!snapshotName.endsWith("/")) snapshotName += "/";
        String[] exclude_params = !isEmpty(rsync_excludes) ? Stream.of(rsync_excludes).map((e)->"--exclude="+e+"").toArray(String[]::new) : null;
        //Sample call rsync as user data
        //sudo -u data -i rsync -rtP --delete --force rsync://data@data.local:873/data/.snapshots/2024/ /srv/dev-disk-by-label-data/data/ --log-file /var/log/rsync.log
        cmd.call2(flat(new String[][]{{"sudo","-u","data","-i","rsync","-rtP","--delete","--force"},exclude_params,{remote_storage+remote_snapshots+snapshotName,root_path+subvolume_path},{"--log-file","/var/log/rsync.log"}}),
                          new String[]{"RSYNC_PASSWORD="+remote_storage_password});//env variable
    }
    
    private static boolean isSnapshotChanged(String snapshotName){
        if(snapshotName==null || snapshotName.isBlank()) return false;
        if(!snapshotName.endsWith("/")) snapshotName += "/";
        String[] exclude_params = !isEmpty(rsync_excludes) ? Stream.of(rsync_excludes).map((e)->"--exclude="+e+"").toArray(String[]::new) : null;
        String std_out = cmd.call(flat(new String[][]{{"rsync","-rtP","--dry-run"},exclude_params,{remote_storage+remote_snapshots+snapshotName,root_path+subvolume_path+snapshots_path+snapshotName}}),
                                          new String[]{"RSYNC_PASSWORD="+remote_storage_password}); //env variable
        System.out.println(std_out);
        
        List<String> std_outs = new ArrayList<>(Arrays.asList(std_out.split("\r\n|\r|\n")));
        std_outs.remove("receiving incremental file list");
        //std_outs.remove("./");//pokud nelze změnit modifiet time (špatný vlastník uživatel/skupina složky data, musí to být data/users)
        return !std_outs.isEmpty();
    }
    
    private static boolean isEmpty(String string){
        return string==null || string.trim().isEmpty();
    }
    private static boolean isEmpty(String[] strings){
        return Stream.of(strings).filter(Objects::nonNull).collect(Collectors.joining()).trim().isEmpty();
    }
    private static String[] flat(String[][] strings){
        return flatten(strings).filter(Objects::nonNull).toArray(String[]::new);
    }
    private static Stream<Object> flatten(Object[] array) {
        return Arrays.stream(array).flatMap(o -> o instanceof Object[] ? flatten((Object[]) o) : Stream.of(o));
    }
    
    public static List<String> getLocalSnapshots(){
        File snapshotsDir = new File(root_path+subvolume_path+snapshots_path);
        snapshotsDir.mkdirs();
        chown("data", "users", root_path+subvolume_path);
        chown("data", "users", root_path+subvolume_path+snapshots_path);
        return Stream.of(snapshotsDir.list()).sorted(snapshotComparator).collect(Collectors.toList());
    }
    
    public static String chown(String user,String group,String full_path){
        if(group==null || user==null || full_path==null) return null;
        return cmd.call(new String[]{"/bin/bash","-c","sudo chown "+user+":"+group+" "+full_path});
    }
    
    public static List<String> getRemoteSnapshots(){
        //env RSYNC_PASSWORD=data rsync --list-only rsync://data@data.local:873/data/.snapshots/
        String cmd_out = cmd.call(new String[]{"rsync","--list-only",remote_storage+remote_snapshots},new String[]{"RSYNC_PASSWORD="+remote_storage_password});
        /*
        drwxrwsrwx            130 2024/05/07 21:08:30 .
        drwxrwsr-x              8 2024/05/07 21:08:30 .recycle
        drwxr-xr-x            828 2024/05/08 08:59:03 .snapshots
        drwxrwsr-x            280 2024/04/21 12:56:24 CHAPPIE
        drwxrwsr-x             74 2022/04/22 15:08:17 Dokumenty
        drwxrwsr-x          3,372 2024/03/26 18:35:56 Filmy
        drwxrwsr-x            136 2024/01/30 15:13:35 Hry
        drwxrwsr-x            686 2023/11/07 10:07:30 Install-OS
        */
        String[] lines = cmd_out.trim().split("\r|\n|\r\n");
        //Delší než 46 znaků, ořízneme od 46 znaku, nesmí to být tečka.
        List<String> list = Stream.of(lines).filter((l)->l.length()>46).map((l)->l.substring(46)).filter((s)->!s.equals(".")).collect(Collectors.toList());
        list.sort(snapshotComparator);
        return list;
    } 
    
    public static Comparator<String> snapshotComparator = new Comparator<String>() {
        private String normalize(String snapshotName){
            switch (snapshotName.length()) {
                case 4 : return snapshotName+".99.99"; //2024       -> 2024.99.99
                case 7 : return snapshotName+".99";    //2024.05    -> 2024.05.99
                case 10: return snapshotName;          //2024.05.19 -> 2024.05.19
            }
            return snapshotName;
        }
        @Override
        public int compare(String o1, String o2) {
            return normalize(o1).compareTo(normalize(o2));
        }
    };
    
    /** Najde pro zařížení napr "/dev/nvme0n1" cestu narp "/srv/dev-disk-by-uuid-052e89dd-b76a-42fc-bd8f-c812bc1b822a"  */
    public static String dev_path_2_mountpoint_path(String dev_path){
        return cmd.call(new String[]{"lsblk","--nodeps",dev_path,"--output","MOUNTPOINT","--noheadings"}).trim();
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

    /** Vycte seznam vsech subvolume/snapshots! */
    public static List<String> getBtrfsSubvolumeAndSnapshotsNames(String btrfs_subvolume_path){
        String cmd_out = cmd_btrfs_subvolume_list(btrfs_subvolume_path);
        //Zjistime si seznam vsech subvolume/snapshots
        return extractSubvolumeNames(cmd_out);
    }

    /** Vycte seznam vsech subvolumu mimo snapshoty! */
    public static List<String> getBtrfsSnapshotsNames(String btrfs_subvolume_path){
        //Zjistime si seznam vsech snapshots
        String cmd_out = cmd_btrfs_subvolume_snapshot_list(btrfs_subvolume_path);
        List<String> only_snapshots = extractSubvolumeSnapshotNames(cmd_out);
        return only_snapshots;
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
    
    /** Vola prikaz pro "btrfs subvolume" nebo "btrfs subvolume snapshot" nastaveni readonly parametru true/false. */
    public static String cmd_btrfs_subvolume_or_snapshot_readonly(String btrfs_subvolume_full_path,String true_false,boolean printCommand){
        if(printCommand) System.out.println("readonly '"+true_false+"' subvolume/snapshot: "+btrfs_subvolume_full_path);
        return cmd.call(new String[]{"/bin/bash","-c","sudo btrfs property set -ts '"+btrfs_subvolume_full_path+"' "+" ro "+true_false});
    }
    
    /** Vola prikaz pro "btrfs subvolume" nebo "btrfs subvolume snapshot" nastaveni readonly parametru true/false. */
    public static String cmd_btrfs_subvolume_or_snapshot_readonly_test(String btrfs_subvolume_full_path){
        //System.out.println("readonly test subvolume/snapshot: "+btrfs_subvolume_full_path);
        String result = cmd.call(new String[]{"/bin/bash","-c","sudo btrfs property get -ts '"+btrfs_subvolume_full_path+"'"});
        switch (result.trim()) {
            case "ro=false":
                return "readonly=FALSE";
            case "ro=true":
                return "readonly=TRUE";
            default:
                return "ERROR: "+result;
        }
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
        //sudo mkdir -p create /srv/dev-disk-by-label-data/data/.snapshots
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
            return call(cmds, null);
        }
        public static String call(String[] cmds,String[] envp) {
            try {
                System.out.println(Stream.of(cmds).collect(Collectors.joining(" ")));
                Process p = Runtime.getRuntime().exec(cmds,envp);
                String sout = IS.toString(p.getInputStream());
                String serr = IS.toString(p.getErrorStream());
                if(serr!=null && !serr.isBlank()) System.err.println(serr);
                return sout+serr;
            } catch (IOException ex) {
                ex.printStackTrace();
                return null;
            }
        }
        public static void call2(String[] cmds,String[] envp) {
            try {
                System.out.println(Stream.of(cmds).collect(Collectors.joining(" ")));
                Process p = Runtime.getRuntime().exec(cmds,envp);
                IS.toStream(p.getInputStream(),System.out);
                IS.toStream(p.getErrorStream(),System.err);
            } catch (IOException ex) {
                ex.printStackTrace();
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
            try (Reader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                int c = 0;
                while ((c = reader.read()) != -1) {
                    textBuilder.append((char) c);
                }
            }
            return textBuilder.toString();
        }
        public static void toStream(InputStream is,PrintStream ps) throws IOException{
            try (Reader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                int c = 0;
                while ((c = reader.read()) != -1) {
                    ps.append((char) c);
                }
            }
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
        public static class OptionDelimiter extends Option{
            public OptionDelimiter(String text) {
                super(text, "", new String[0]);
            }
            @Override
            public String toString() {
                return name;
            }
        }
    }
    
}
