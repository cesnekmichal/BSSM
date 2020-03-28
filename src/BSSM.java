
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author Česnek Michal
 */
public class BSSM {


    /** Vyčte seznam všech subvolumů i snapshotů! */
    public static String cmd_btrfs_subvolume_list(String btrfs_subvolume_path) throws IOException {
        return cmd.call(new String[]{"/bin/bash","-c","sudo btrfs subvolume list -t --sort=path "+btrfs_subvolume_path});
    }

    /** Vyčte seznam všech subvolumů mimo snapshoty! */
    public static List<String> getBtrfsSubvolumeNames(String btrfs_subvolume_path) throws IOException{
        String cmd_out = cmd_btrfs_subvolume_list(btrfs_subvolume_path);
        //Zjistíme si seznam všech subvolumes
        List<String> all_subvolumes = extractSubvolumeNames(cmd_out);
        cmd_out = cmd_btrfs_subvolume_snapshot_list(btrfs_subvolume_path);
        //Zjistíme si seznam všech snapshots
        List<String> only_snapshots = extractSubvolumeSnapshotNames(cmd_out);
        //Odstraníme všechny snapshoty, čímž získáme seznam pouze subvolumů mimo snapshoty!
        all_subvolumes.removeAll(only_snapshots);
        return all_subvolumes;
    }
    
    /** Zjistí snapshoty tj. ty co jsou pouze pro čtení! */
    public static String cmd_btrfs_subvolume_snapshot_list(String btrfs_subvolume_path) throws IOException {
        return cmd.call(new String[]{"/bin/bash","-c","sudo btrfs subvolume list -t -s --sort=path "+btrfs_subvolume_path});
    }
    
    /** Vrací seznam snapshotů. */
    public static List<String> getBtrfsSubvolumeSnapshotNames(String btrfs_subvolume_path) throws IOException{
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
    
    
    public static void printBtrfsSubvolumeSnapshotNames(String btrfs_subvolume_full_path) throws IOException{
        getBtrfsSubvolumeSnapshotNames(btrfs_subvolume_full_path).forEach((l)->System.out.println(l));
    }
    
    /** Volá příkaz pro odstranění "btrfs subvolume" */
    public static String cmd_btrfs_subvolume_delete(String btrfs_subvolume_full_path) throws IOException {
        System.out.println("delete subvolume: "+btrfs_subvolume_full_path);
        return cmd.call(new String[]{"/bin/bash","-c","sudo btrfs subvolume delete -c "+btrfs_subvolume_full_path});
    }
    /** Volá příkaz pro odstranění "btrfs subvolume" */
    public static String cmd_btrfs_subvolume_snapshot_delete(String btrfs_subvolume_full_path) throws IOException {
        System.out.println("delete subvolume snapshot: "+btrfs_subvolume_full_path);
        return cmd.call(new String[]{"/bin/bash","-c","sudo btrfs subvolume delete -c "+btrfs_subvolume_full_path});
    }
    
    /** Volá příkaz pro odstranění "btrfs subvolume" */
    public static String cmd_btrfs_subvolume_snapshot_create(String src_btrfs_subvolume_full_path,String target_btrfs_snapshot_fullpath) throws IOException {
        System.out.println("create subvolume snapshot: "+target_btrfs_snapshot_fullpath);
        //sudo btrfs subvolume snapshot -r /srv/dev-disk-by-label-btrfs/rock/ /srv/dev-disk-by-label-btrfs/rock/.snapshots/2020
        return cmd.call(new String[]{"/bin/bash","-c","sudo btrfs subvolume snapshot -r "+src_btrfs_subvolume_full_path+" "+target_btrfs_snapshot_fullpath});
    }
    
    /** Volá příkaz pro vytvoření "btrfs subvolume" */
    public static String cmd_btrfs_subvolume_create(String btrfs_subvolume_full_path) throws IOException {
        System.out.println("create subvolume: "+btrfs_subvolume_full_path);
        //sudo btrfs subvolume create /srv/dev-disk-by-label-btrfs/rock
        return cmd.call(new String[]{"/bin/bash","-c","sudo btrfs subvolume create "+btrfs_subvolume_full_path});
    }
    
    /** Volá příkaz pro vytvoření "btrfs subvolume" */
    public static String cmd_mkdir(String dir_full_path) throws IOException {
        System.out.println("mkdir: "+dir_full_path);
        //sudo mkdir -p create /srv/dev-disk-by-label-btrfs/rock/.snapshot
        return cmd.call(new String[]{"/bin/bash","-p",dir_full_path});
    }
    
    public static List<String> getMonthSnapshots(List<String> snapshots_all,String snapshot_dir){
        List<String> months = new ArrayList<>();
        //.snapshots/2020
        //.snapshots/2020.03
        //.snapshots/2020.03.28
        if(snapshot_dir==null) snapshot_dir = ".snapshots";
        String regex = "^"+Pattern.quote(snapshot_dir)+"/\\d{4}\\.\\d{2}$";
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
        if(snapshot_dir==null) snapshot_dir = ".snapshots";
        String regex = "^"+Pattern.quote(snapshot_dir)+"/\\d{4}\\.\\d{2}.\\d{2}$";
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
     * @param args the command line arguments
     */
    public static void main(String[] args) throws IOException {
        //idx parametru
        int n = 0;
        String btrfs_root_path = "/srv/dev-disk-by-label-btrfs/";
        if(args!=null && args.length>n && args[n]!=null && !args[n].isEmpty()){
            btrfs_root_path = args[n];
            if(!btrfs_root_path.endsWith("/")) btrfs_root_path += "/";
        }
        
        n++;
        String btrfs_subvolume_path = "rock/";
        if(args!=null && args.length>n && args[n]!=null && !args[n].isEmpty()){
            btrfs_subvolume_path = args[n];
            if(!btrfs_subvolume_path.endsWith("/")) btrfs_subvolume_path += "/";
        }
        
        //Výchozí plná cesta k subvolume adresáři
        //String btrfs_subvolume_full_path = "/srv/dev-disk-by-label-btrfs/rock/";
        String btrfs_subvolume_full_path = btrfs_root_path+btrfs_subvolume_path;
//        printBtrfsSubvolumeSnapshotNames(btrfs_subvolume_full_path);
        
        List<String> subvolumes = getBtrfsSubvolumeNames(btrfs_root_path);
        String btrfs_subvolume_name = btrfs_subvolume_path.replace("/","");
        //Pokud neexistuje btrfs subvolume musíme jej založit
        if(!subvolumes.contains(btrfs_subvolume_name)){
            cmd_btrfs_subvolume_create(btrfs_subvolume_full_path);
        }
        subvolumes.remove(btrfs_subvolume_name);
        //Ostatní subvolumy odstraníme!!!
        for (String btrfs_subvolume_name_other : subvolumes) {
            String btrfs_subvolume_full_path_other = btrfs_root_path+btrfs_subvolume_name_other;
            cmd_btrfs_subvolume_delete(btrfs_subvolume_full_path_other);
        }
        
        //Adresář pro snapshoty
        String snapshot_dir = ".snapshots";
        n++;
        if(args!=null && args.length>n && args[n]!=null && !args[n].isEmpty()){
            snapshot_dir = args[n];
        }
        
        cmd_mkdir(btrfs_subvolume_full_path+snapshot_dir);
        
        List list = getBtrfsSubvolumeSnapshotNames(btrfs_subvolume_full_path);
//        printBtrfsSubvolumeSnapshotNames(btrfs_subvolume_full_path);
        
        Calendar now = Calendar.getInstance();
//        now.set(Calendar.MONTH, ThreadLocalRandom.current().nextInt(0, 12));
//        now.set(Calendar.DAY_OF_MONTH, ThreadLocalRandom.current().nextInt(1, 28));

        //1. Pokud existuje snapshot pro tento rok, tak jej smaž a vytvoř znovu
        int year = now.get(Calendar.YEAR);
        String year_snapshot_name = snapshot_dir+"/"+String.format("%04d",year);
        String year_snapshot_full_name = btrfs_subvolume_full_path+year_snapshot_name;
        boolean year_snapshot_exists = list.contains(year_snapshot_name);
        if(year_snapshot_exists){
            cmd_btrfs_subvolume_snapshot_delete(year_snapshot_full_name);
        }
        cmd_btrfs_subvolume_snapshot_create(btrfs_subvolume_full_path, year_snapshot_full_name);
        
        //2. Pokud existuje snapshot pro tento měsíc, tak jej smaž a založ nový
        int month = now.get(Calendar.MONTH)+1;
        String month_snapshot_name = snapshot_dir+"/"+String.format("%04d",year)+"."+String.format("%02d",month);
        String month_snapshot_full_name = btrfs_subvolume_full_path+month_snapshot_name;
        boolean month_snapshot_exists = list.contains(month_snapshot_name);
        if(month_snapshot_exists){
            cmd_btrfs_subvolume_snapshot_delete(month_snapshot_full_name);
        }
        cmd_btrfs_subvolume_snapshot_create(btrfs_subvolume_full_path, month_snapshot_full_name);
        
//        printBtrfsSubvolumeSnapshotNames(btrfs_subvolume_full_path);
        
        //2.1 Pokud existuje více než 12 měsíčních snapshotů, pak ty nadlimitní nejstarší odstraň.
        List<String> snapshots_all = getBtrfsSubvolumeSnapshotNames(btrfs_subvolume_full_path);
        List<String> monthSnapshots = getMonthSnapshots(snapshots_all,snapshot_dir);
        int max_month_snapshots = 12;
        if(monthSnapshots.size()>max_month_snapshots){
            for (int i = 0; i < monthSnapshots.size()-max_month_snapshots; i++) {
                String month_snapshot_name_old = monthSnapshots.get(i);
                String month_snapshot_full_name_old = btrfs_subvolume_full_path+month_snapshot_name_old;
                cmd_btrfs_subvolume_snapshot_delete(month_snapshot_full_name_old);
            }
        }
        
//        for (String month_snapshot_name0 : snapshots_all) {
//            String month_snapshot_full_name0 = btrfs_subvolume_full_path+month_snapshot_name0;
//            cmd_btrfs_subvolume_snapshot_delete(month_snapshot_full_name0);
//        }
//        if(true) return;
        
        //3. Pokud existuje snapshot pro tento den, tak jej smaž a založ nový
        int day = now.get(Calendar.DAY_OF_MONTH);
        String day_snapshot_name = snapshot_dir+"/"+String.format("%04d",year)+"."+String.format("%02d",month)+"."+String.format("%02d",day);
        String day_snapshot_full_name = btrfs_subvolume_full_path+day_snapshot_name;
        boolean day_snapshot_exists = list.contains(day_snapshot_name);
        if(day_snapshot_exists){
            cmd_btrfs_subvolume_snapshot_delete(day_snapshot_full_name);
        }
        cmd_btrfs_subvolume_snapshot_create(btrfs_subvolume_full_path, day_snapshot_full_name);
        
        //3.1 Pokud existuje více než 31 denních snapshotů, pak ty nejstarší odstraň.
        snapshots_all = getBtrfsSubvolumeSnapshotNames(btrfs_subvolume_full_path);
        List<String> daySnapshots = getDaySnapshots(snapshots_all,snapshot_dir);
        int max_day_snapshots = 31;
        if(daySnapshots.size()>max_day_snapshots){
            for (int i = 0; i < daySnapshots.size()-max_day_snapshots; i++) {
                String day_snapshot_name_old = daySnapshots.get(i);
                String day_snapshot_full_name_old = btrfs_subvolume_full_path+day_snapshot_name_old;
                cmd_btrfs_subvolume_snapshot_delete(day_snapshot_full_name_old);
            }
        }        
        
//        printBtrfsSubvolumeSnapshotNames(btrfs_subvolume_full_path);
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
        public static String call(String[] cmds) throws IOException{
            Process p = Runtime.getRuntime().exec(cmds);
            return IS.toString(p.getInputStream()) + IS.toString(p.getErrorStream());
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
