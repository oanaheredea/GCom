/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * GComWindow.java
 *
 * Created on Dec 16, 2013, 1:26:05 PM
 */
package gui;

import gcom.RMIServer;
import gcom.interfaces.IGroupManagement;
import gcom.interfaces.IMember;
import gcom.interfaces.MESSAGE_TYPE;
import gcom.modules.group.Group;
import gcom.modules.group.GroupDef;
import gcom.modules.group.GroupManagement;
import gcom.modules.group.GroupManagementException;
import gcom.modules.group.Member;
import gcom.modules.group.Message;
import gnomezgrave.fisheye.packages.FishEyeDock;
import java.awt.HeadlessException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.rmi.NoSuchObjectException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import javax.swing.JOptionPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import pgcom.cassandra.DBConnect;

/**
 *
 * @author ens13pps
 */
public class GComWindow extends javax.swing.JFrame {

    private RMIServer server;
    private DefaultTreeModel tm;
    private DefaultMutableTreeNode root;
    private static HashMap<String, DefaultMutableTreeNode> nodes;
    private boolean isVisible;
    private DBConnect cdb;

    /**
     * Creates new form GComWindow
     */
    public GComWindow() {
        initComponents();
        setLocationRelativeTo(null);
        tm = (DefaultTreeModel) trGComStructure.getModel();
        root = (DefaultMutableTreeNode) tm.getRoot();
        GroupManagement.setGComWindow(this);
        nodes = new HashMap<String, DefaultMutableTreeNode>();
        setIconImage(new ImageIcon(GComWindow.class.getResource("/pics/logo.png")).getImage());

        String[] hosts = new String[]{"127.0.1.1", "127.0.1.2", "127.0.1.3"};
//        for (int host = 0; host < hosts.length; host++) {
//            try {
//                new ProcessBuilder("sh", "./cassandra/start.sh", (host + 1) + "").loadRMIEntries();
//                Runtime.getRuntime().exec("./cassandra/start.sh "+(host+1));
//            } catch (IOException ex) {
//                Logger.getLogger(GComWindow.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
        cdb = new DBConnect(hosts);
        cdb = new DBConnect(hosts);
        cdb.connectKeySpace("pgcomkeyspace");
//        String query = "CREATE KEYSPACE pgcomkeyspace\n"
//                + "WITH REPLICATION = { 'class' : 'SimpleStrategy',\n"
//                + "'replication_factor' : 3 }";

    }

    private void initializeDock() {
        FishEyeDock fd = new FishEyeDock(FishEyeDock.HORIZONTAL_ALIGNMENT, 10);
        fd.setImageZoomMode(FishEyeDock.ZOOM_MODE_SMOOTH);
        fd.setZoomLevel(20);
        fd.setAnimationSpeed(6);
        fd.insert(new ImageIcon(GComWindow.class.getResource("/pics/addGroup.png")), "Add Group", new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                addNewGroup();
            }
        });
        fd.insert(new ImageIcon(GComWindow.class.getResource("/pics/settings.png")), "Settings", null);
        dockPanel.add(fd);
    }

    public void updateStatus(String newStatus) {
        txtLog.setText(txtLog.getText() + newStatus + "\n");
        Logger.getLogger(Member.class.getName()).log(Level.INFO, "Status Updated : {0}", newStatus);
    }

    public void updateStatus(ArrayList<String> newStatus) {
    }

    public void updateStatus(Message msg, MESSAGE_TYPE type) throws RemoteException {
        IMember member = msg.getSource();
        if (type == MESSAGE_TYPE.GROUP_CREATED) {

            DefaultMutableTreeNode tn = new DefaultMutableTreeNode(member.getName());
            String parentName = member.getParentGroup().getGroupName();
            DefaultMutableTreeNode parent = nodes.get(parentName);
            tm.insertNodeInto(tn, parent, parent.getChildCount());
            String m = type + " " + parentName + " => " + member.getName() + "(" + member.getParentGroup().getMemberCount() + ") [Leader]";
            updateStatus(m);
        } else if (type == MESSAGE_TYPE.MEMBER_JOINED) {
            // msg.message contains the name of the member
            String mem = msg.getMessage();
            DefaultMutableTreeNode tn = new DefaultMutableTreeNode(mem);
            String group = msg.getParams().get(0);
            DefaultMutableTreeNode parent = nodes.get(group);
            tm.insertNodeInto(tn, parent, parent.getChildCount());
            String m = type + " " + group + " => " + mem + "(" + member.getParentGroup().getMemberCount() + ")";
            updateStatus(m);
        }
    }

    private void addGroupToTree(String child, DefaultMutableTreeNode parent) {
        DefaultMutableTreeNode ch = new DefaultMutableTreeNode(child, true);
        tm.insertNodeInto(ch, parent, parent.getChildCount());
        nodes.put(child, ch);
    }

    private void addNewGroup() {
        NewGroup ng = new NewGroup(GComWindow.this, true);
        ng.setVisible(true);
        Group createdGroup = ng.getCreatedGroup();
        if (createdGroup != null) {
            GroupDef groupDef = createdGroup.getGroupDef();
            updateStatus("New Group Created : " + createdGroup.getGroupName() + " [" + groupDef.getComType() + "," + groupDef.getOrdType() + "]");
            addGroupToTree(createdGroup.getGroupName(), root);
        }
    }

    public void addMember(Group group, IMember member) throws RemoteException {
        DefaultMutableTreeNode ch = new DefaultMutableTreeNode(member.getName(), true);
        DefaultMutableTreeNode parent = nodes.get(group.getGroupName());
        tm.insertNodeInto(ch, parent, parent.getChildCount());

        updateStatus("Member " + member.getName() + " added to group " + group.getGroupName());
    }

    private void startTMIServer(boolean state) throws HeadlessException {
        if (state) {
            String input = null;
            Object value = JOptionPane.showInputDialog(GComWindow.this, "Enter port number :", "Port", JOptionPane.DEFAULT_OPTION, new ImageIcon(GComWindow.class.getResource("/pics/port.png")), null, "1099");
            if (value != null && !(input = value.toString().trim()).isEmpty()) {

                try {
                    int port = Integer.parseInt(input);
                    server = new RMIServer(port, cdb);
                    server.createRegistry();
                    String msg = "RMI Registry Server started on port " + port;
                    updateStatus(msg);

                    GroupManagement obj = new GroupManagement();
                    IGroupManagement stub;

                    try {
                        stub = server.regLookUp("IGroupManagement");                        
                        System.out.println("Found! " + stub);
                    }catch(NotBoundException nex){ 
                        stub = (IGroupManagement) UnicastRemoteObject.exportObject(obj, 0);
                        System.out.println("Not Found! " + stub);
                        server.rebind("IGroupManagement", stub);
                    }catch (Exception e) {
                        e.printStackTrace();                        
                    }

                    msg = "Default stub binded:" + " IGroupManagement";
                    updateStatus(msg);

                    mnuStartServer.setSelected(true);
                    btnServer.setSelected(true);
                } catch (RemoteException e) {
                    JOptionPane.showMessageDialog(this, "Cannot connect to the RMIRegistry on given port : " + input, "Invalid Port", JOptionPane.ERROR_MESSAGE);
                    mnuStartServer.setState(false);
                } catch (Exception e) {
                    e.printStackTrace();
                    JOptionPane.showMessageDialog(this, "Invalid port number : " + input, "Invalid Port", JOptionPane.ERROR_MESSAGE);
                    mnuStartServer.setState(false);
                }
            } else {
                btnServer.setSelected(false);
                mnuStartServer.setSelected(false);
            }
        } else {
            try {
                server.stop();
                String msg = "RMI Registry Server stopped.";
                updateStatus(msg);
                btnServer.setSelected(false);
                mnuStartServer.setSelected(false);
            } catch (NoSuchObjectException ex) {
                Logger.getLogger(GComWindow.class.getName()).log(Level.SEVERE, null, ex);
                btnServer.setSelected(true);
                mnuStartServer.setState(true);
            }
        }
    }

    private void removeGroup() {
        DefaultMutableTreeNode dmt = (DefaultMutableTreeNode) trGComStructure.getSelectionPath().getLastPathComponent();
        String group = dmt.getUserObject().toString();
        if (nodes.containsKey(group)) {
            try {
                GroupManagement.removeGroupMulticast(group);
                server.unbind(group);
            } catch (RemoteException ex) {
                Logger.getLogger(GComWindow.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NotBoundException ex) {
                Logger.getLogger(GComWindow.class.getName()).log(Level.SEVERE, null, ex);
            } catch (GroupManagementException ex) {
                Logger.getLogger(GComWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
            tm.removeNodeFromParent(dmt);
            updateStatus("Group Removed : " + dmt.getUserObject());
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        treeMenu = new javax.swing.JPopupMenu();
        mnuRemoveGroup = new javax.swing.JMenuItem();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtLog = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        trGComStructure = new javax.swing.JTree();
        jPanel1 = new javax.swing.JPanel();
        dockPanel = new javax.swing.JPanel();
        btnServer = new javax.swing.JToggleButton();
        mainMenu = new javax.swing.JMenuBar();
        mnuFile = new javax.swing.JMenu();
        mnuStartServer = new javax.swing.JCheckBoxMenuItem();
        mnuNewGroup = new javax.swing.JMenuItem();
        mnuNewMember = new javax.swing.JMenuItem();
        mnuAbout = new javax.swing.JMenu();
        mnuAboutGCom = new javax.swing.JMenuItem();

        mnuRemoveGroup.setText("Remove Group");
        mnuRemoveGroup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuRemoveGroupActionPerformed(evt);
            }
        });
        treeMenu.add(mnuRemoveGroup);

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("GCom Server Management");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        txtLog.setEditable(false);
        txtLog.setColumns(20);
        txtLog.setRows(5);
        jScrollPane1.setViewportView(txtLog);

        javax.swing.tree.DefaultMutableTreeNode treeNode1 = new javax.swing.tree.DefaultMutableTreeNode("GCom");
        trGComStructure.setModel(new javax.swing.tree.DefaultTreeModel(treeNode1));
        trGComStructure.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                trGComStructureMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(trGComStructure);

        dockPanel.setLayout(new java.awt.BorderLayout());
        jPanel1.add(dockPanel);

        btnServer.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pics/server.png"))); // NOI18N
        btnServer.setToolTipText("Start/Stop the RMI Server");
        btnServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnServerActionPerformed(evt);
            }
        });

        mnuFile.setText("File");

        mnuStartServer.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
        mnuStartServer.setText("RMI Server");
        mnuStartServer.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuStartServerActionPerformed(evt);
            }
        });
        mnuFile.add(mnuStartServer);

        mnuNewGroup.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_G, java.awt.event.InputEvent.CTRL_MASK));
        mnuNewGroup.setText("New Group");
        mnuNewGroup.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuNewGroupActionPerformed(evt);
            }
        });
        mnuFile.add(mnuNewGroup);

        mnuNewMember.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_M, java.awt.event.InputEvent.CTRL_MASK));
        mnuNewMember.setText("Add Member");
        mnuNewMember.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuNewMemberActionPerformed(evt);
            }
        });
        mnuFile.add(mnuNewMember);

        mainMenu.add(mnuFile);

        mnuAbout.setText("About");

        mnuAboutGCom.setText("About GCom");
        mnuAboutGCom.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuAboutGComActionPerformed(evt);
            }
        });
        mnuAbout.add(mnuAboutGCom);

        mainMenu.add(mnuAbout);

        setJMenuBar(mainMenu);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(btnServer, javax.swing.GroupLayout.PREFERRED_SIZE, 96, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 178, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 552, Short.MAX_VALUE)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(btnServer, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 389, Short.MAX_VALUE)
                    .addComponent(jScrollPane1))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

private void mnuStartServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuStartServerActionPerformed
    new Thread() {
        public void run() {
            startTMIServer(mnuStartServer.getState());
        }
    }.start();
}//GEN-LAST:event_mnuStartServerActionPerformed

private void mnuNewGroupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuNewGroupActionPerformed
    addNewGroup();
}//GEN-LAST:event_mnuNewGroupActionPerformed

    private void trGComStructureMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_trGComStructureMouseClicked
        if (evt.getButton() == MouseEvent.BUTTON3) {
            int row = trGComStructure.getClosestRowForLocation(evt.getX(), evt.getY());
            trGComStructure.setSelectionRow(row);
            int[] selectionRows = trGComStructure.getSelectionRows();

            treeMenu.show(evt.getComponent(), evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_trGComStructureMouseClicked

    private void btnServerActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnServerActionPerformed
        new Thread() {
            public void run() {
                startTMIServer(btnServer.isSelected());
            }
        }.start();

    }//GEN-LAST:event_btnServerActionPerformed

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        initializeDock();
    }//GEN-LAST:event_formWindowOpened

    private void mnuNewMemberActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuNewMemberActionPerformed
        try {
            Runtime.getRuntime().exec("./member.sh");
        } catch (IOException ex) {
            Logger.getLogger(GComWindow.class.getName()).log(Level.SEVERE, null, ex);
        }
    }//GEN-LAST:event_mnuNewMemberActionPerformed

    private void mnuRemoveGroupActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuRemoveGroupActionPerformed
//        new Thread() {
//            public void run() {
        removeGroup();
//            }
//        }.loadRMIEntries();

    }//GEN-LAST:event_mnuRemoveGroupActionPerformed

    private void mnuAboutGComActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuAboutGComActionPerformed
        new About(this, true).setVisible(true);
    }//GEN-LAST:event_mnuAboutGComActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JToggleButton btnServer;
    private javax.swing.JPanel dockPanel;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JMenuBar mainMenu;
    private javax.swing.JMenu mnuAbout;
    private javax.swing.JMenuItem mnuAboutGCom;
    private javax.swing.JMenu mnuFile;
    private javax.swing.JMenuItem mnuNewGroup;
    private javax.swing.JMenuItem mnuNewMember;
    private javax.swing.JMenuItem mnuRemoveGroup;
    private javax.swing.JCheckBoxMenuItem mnuStartServer;
    private javax.swing.JTree trGComStructure;
    private javax.swing.JPopupMenu treeMenu;
    private javax.swing.JTextArea txtLog;
    // End of variables declaration//GEN-END:variables

    /**
     * @return the server
     */
    public RMIServer getServer() {
        return server;
    }
}
