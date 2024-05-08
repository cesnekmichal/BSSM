import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 *
 * @author Michal
 */
public class SnapshotBrowser extends javax.swing.JFrame {

    File snapshotsRootDir = new File("d:\\.snapshots\\");
    File currentRootDir   = new File("d:\\");
    String subDirPath = "";
    /**
     * Creates new form SnapshotBrowser
     */
    public SnapshotBrowser() {
        initComponents();
        setLocationRelativeTo(null);
        
        jList1.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount()==2){
                    btn_BrowseSelectedDirectory.doClick();
                }
            }
        });

        browse("",null);
        
    }
    
    public void browse(String subDirPath,H<Boolean> cancelH){
        ArrayList<Item> items = new ArrayList<>();
        
        items.add(new Item(subDirPath));
        
        for (File snapshotRootDir : snapshotsRootDir.listFiles()) {
            File snapshotRootDirX = new File(snapshotRootDir, subDirPath);
            if(snapshotRootDirX.exists() && snapshotRootDirX.isDirectory()) {
                for (File f : snapshotRootDirX.listFiles()) {
                    add(items, new Item(f));
                    if(cancelH!=null && cancelH.v) break;
                }
            }
        }
        
        File currentRootDirX   = new File(currentRootDir, subDirPath);
        if(currentRootDirX.exists() && currentRootDirX.isDirectory()){
            for (File f : currentRootDirX.listFiles()) {
                add(items, new Item(f));
                if(cancelH!=null && cancelH.v) break;
            }
        }
        
        sort(items);
        
        MyListModel myListModel = new MyListModel(items);
        jList1.setModel(myListModel);
        jList1.repaint();
    }
    
    public static void sort(ArrayList<Item> items){
        items.sort(Comparator
                .comparing((Item t) -> !t.toParent)
            .thenComparing((Item t) -> t.file.isFile())
            .thenComparing((Item t) -> t.file.getName())
        );
    }
    
    public static void add(ArrayList<Item> items, Item item){
        if(item.toParent){
            items.add(item);
        } else {
            for (Item i : items) {
                if(i.toParent) continue;
                if(i.file.isDirectory() == item.file.isDirectory() && i.file.getName().equals(item.file.getName())){
                    i.count++;
                    return;
                }
            }
            items.add(item);
        }
    }
    
    public static class Item {
        public Item(File file){
            this.file = file;
        }
        public Item(String subdirPath){
            this.subdirPath = subdirPath;
            this.toParent = true;
        }
        public String subdirPath = "";
        public Boolean toParent = false;
        public File file;
        public int count = 1;

        @Override
        public String toString() {
            if(toParent){
                return ".." + " ["+subdirPath+"]";
            } else {
                return file.isDirectory() ? "["+file.getName()+"]" + " /"+count+"x/"
                                          :     file.getName()     + " /"+count+"x/";
            }
        }
    }
    
    public static class MyListModel extends javax.swing.AbstractListModel {
        ArrayList<Item> items;
        public MyListModel(ArrayList<Item> items){
            this.items = items;
        }
        @Override
        public int getSize() {
            return items.size();
        }
        @Override
        public Object getElementAt(int index) {
            return items.get(index).toString();
        }
    }
    
    public static class H<T>{
        public H(T v) {
            this.v = v;
        }
        public T v;
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jList1 = new javax.swing.JList<>();
        btn_BrowseSelectedDirectory = new javax.swing.JButton();
        btn_Cancel = new javax.swing.JButton();
        label_ActionText = new javax.swing.JLabel();
        btn_Delete = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jList1.setModel(new javax.swing.AbstractListModel<String>() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public String getElementAt(int i) { return strings[i]; }
        });
        jList1.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        jScrollPane1.setViewportView(jList1);

        btn_BrowseSelectedDirectory.setText("Go to Dir");
        btn_BrowseSelectedDirectory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_BrowseSelectedDirectoryActionPerformed(evt);
            }
        });

        btn_Cancel.setText("Cancel");
        btn_Cancel.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_CancelActionPerformed(evt);
            }
        });

        label_ActionText.setText("...");

        btn_Delete.setText("Delete");
        btn_Delete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btn_DeleteActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btn_BrowseSelectedDirectory)
                        .addGap(18, 18, 18)
                        .addComponent(btn_Delete)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 460, Short.MAX_VALUE)
                        .addComponent(label_ActionText)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(btn_Cancel)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 376, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btn_BrowseSelectedDirectory)
                    .addComponent(btn_Cancel)
                    .addComponent(label_ActionText)
                    .addComponent(btn_Delete))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    public void browseSelectedDirectory(){
        int idx = jList1.getSelectedIndex();
        if(idx==-1) return;
        MyListModel model = (MyListModel) jList1.getModel();
        Item item = model.items.get(idx);
        if(item.toParent){
            this.subDirPath = item.subdirPath.contains("/") ? item.subdirPath.substring(0, item.subdirPath.lastIndexOf("/"))
                                                            : "";
            browse(subDirPath,cancelH);
        } else if(item.file.isDirectory()){
            this.subDirPath += "/"+item.file.getName();
            browse(subDirPath,cancelH);
        }        
    }
    
    private void btn_BrowseSelectedDirectoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_BrowseSelectedDirectoryActionPerformed
        
        //browseSelectedDirectory();
        runAction("Go to Dir", () -> {
            browseSelectedDirectory();
        });
        
    }//GEN-LAST:event_btn_BrowseSelectedDirectoryActionPerformed

    Thread t = null;
    
    public void runAction(String name,Runnable action){
        if(t!=null) return;
        t = new Thread(new Runnable() {
            @Override
            public void run() {
                label_ActionText.setText(name);
                cancelH.v = false;
                action.run();
                label_ActionText.setText("...");
                t = null;
            }
        },name);
        t.start();
    }
    
    H<Boolean> cancelH = new H<>(false);
    
    private void btn_CancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_CancelActionPerformed
        this.cancelH.v = true;
    }//GEN-LAST:event_btn_CancelActionPerformed

    private void btn_DeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btn_DeleteActionPerformed
        
        runAction("Deleting selected file or directory", () -> {
            deleteSelectedFileOrDirectory();
            label_ActionText.setText("Reloading files...");
            browse(subDirPath, cancelH);
        });
        
    }//GEN-LAST:event_btn_DeleteActionPerformed

    public void deleteSelectedFileOrDirectory(){
        int idx = jList1.getSelectedIndex();
        if(idx==-1) return;
        MyListModel model = (MyListModel) jList1.getModel();
        Item item = model.items.get(idx);
        if(item.toParent) return;
        File file = new File(new File(currentRootDir, subDirPath), item.file.getName());
        delete(file,cancelH);
        for (File snapshotRootDir : snapshotsRootDir.listFiles()) {
            File fileS = new File(new File(snapshotRootDir, subDirPath), item.file.getName());
            delete(fileS,cancelH);
        }
    }
    
    public static void delete(File file,H<Boolean> cancelH){
        if(file.isFile()){
            file.delete();
            if(cancelH!=null && cancelH.v) return;
        } else if(file.isDirectory()){
            List<File> files = Arrays.asList(file.listFiles());
            files.sort(Comparator.comparing((File f)->{
                return !f.isDirectory();
            }));
            for (File f : files) {
                delete(f,cancelH);
                if(cancelH!=null && cancelH.v) return;
            }
            file.delete();
        }
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        /*
            Metal
            Nimbus
            CDE/Motif
            Windows
            Windows Classic        
        */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Windows Classic".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(SnapshotBrowser.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(SnapshotBrowser.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(SnapshotBrowser.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(SnapshotBrowser.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new SnapshotBrowser().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btn_BrowseSelectedDirectory;
    private javax.swing.JButton btn_Cancel;
    private javax.swing.JButton btn_Delete;
    private javax.swing.JList<String> jList1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel label_ActionText;
    // End of variables declaration//GEN-END:variables
}
