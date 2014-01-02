/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * MemberWindow.java
 *
 * Created on Dec 5, 2013, 6:36:59 PM
 */
package gui.member;

import gcom.RMIServer;
import gcom.interfaces.IGroupManagement;
import gcom.interfaces.IMember;
import gcom.interfaces.MESSAGE_TYPE;
import gcom.modules.group.Group;
import gcom.modules.group.Member;
import gcom.modules.group.Message;
import java.awt.Color;
import java.awt.Component;
import java.awt.HeadlessException;
import java.io.FileReader;
import java.io.IOException;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;

/**
 *
 * @author Praneeth
 */
public class MemberWindow extends javax.swing.JFrame {

    //** Creates new form MemberWindow */
    private ArrayList<String> contacts;
    private Member member;
    private IMember stub;
    private String memName;
    private Group group;
    private DebugWindow debug;
    // private Member member;
    private RMIServer server;
    private MemberContainer memContainer;
    private HashMap<String, SingleChat> chatWindows;
    private boolean isOffline;

    public MemberWindow(Member member, IMember stub) throws RemoteException, AccessException, NotBoundException {
        initComponents();
        this.stub = stub;
        chatWindows = new HashMap<String, SingleChat>();

    }

    public void initialize(Member member, String statusLog) {
        // setLocationRelativeTo(null);
        try {
            this.member = member;
            debug = new DebugWindow(this, member, stub);
            debug.updateStatus(statusLog);
            debug.updateMemberTable();
            this.group = member.getParentGroup();
            this.memName = member.getName();
            lblMemberName.setText(member.getName());

        } catch (Exception ex) {
            lblMemberName.setText("Unknown");
            Logger.getLogger(MemberWindow.class.getName()).log(Level.SEVERE, null, ex);
        }

        setTitle(getTitle() + " : " + group.getGroupName());
        setIconImage(new ImageIcon(MemberWindow.class.getResource("/pics/logo.png")).getImage());
        contacts = getContacts();
        fillContacts(contacts);
        lstContacts.setCellRenderer(new MyListRenderer());
        cmbStatus.setRenderer(new MyListRenderer());
        loadStatusEntries();
    }

    public void loadStatusEntries() {
        //Online, Away, Busy, Invisible
        cmbStatus.removeAllItems();
        cmbStatus.addItem(new JLabel("Online", new ImageIcon(new ImageIcon(MemberWindow.class.getResource("/pics/online.png")).getImage()), JLabel.LEFT));
        //cmbStatus.addItem(new JLabel("Away", new ImageIcon(new ImageIcon(MemberWindow.class.getResource("/pics/away.png")).getImage()), JLabel.LEFT));
        //cmbStatus.addItem(new JLabel("Busy", new ImageIcon(new ImageIcon(MemberWindow.class.getResource("/pics/busy.png")).getImage()), JLabel.LEFT));
        cmbStatus.addItem(new JLabel("Offline", new ImageIcon(new ImageIcon(MemberWindow.class.getResource("/pics/invisible.png")).getImage()), JLabel.LEFT));
    }

    private void fillContacts(ArrayList<String> contacts) {
        DefaultListModel l = new DefaultListModel();
        Vector v = new Vector();
        int c = 0;
        for (int i = 0; i < contacts.size(); i++) {
            JLabel lb = new JLabel(contacts.get(i));
            lb.setIcon(new ImageIcon(new ImageIcon(MemberWindow.class.getResource("/pics/online.png")).getImage()));
            System.out.println(contacts.get(i));
            //lstContacts.add(j);
            v.add(lb);
        }
        lstContacts.setListData(v);
    }

    public void debugWindowClosed() {
        mnuDebug.setSelected(false);
    }

    public void updateMembers(IMember member) throws RemoteException {
        System.out.println("Member name " + member.getName());

        Group parentGroup = member.getParentGroup();
        this.group = parentGroup;

        try {
            contacts = getContacts();
            fillContacts(contacts);
            String statusLog = "Member," + member.getName() + " (" + member.getIdentifier() + ") added to Group " + member.getParentGroup().getGroupName();
            debug.updateStatus(statusLog);
            debug.updateMemberTable();
        } catch (Exception e) {
        }

    }

    public void updateMembers(Group group, IMember member) throws RemoteException {
        System.out.println("Member name " + member.getName());

        this.group = group;

        try {
            contacts = getContacts();
            fillContacts(contacts);
            String statusLog = "Member," + member.getName() + " (" + member.getIdentifier() + ") left the Group " + member.getParentGroup().getGroupName();
            debug.updateStatus(statusLog);
            debug.updateMemberTable();
        } catch (Exception e) {
        }

    }

    public void electionCompleted(IMember member) throws RemoteException {

        if (this.member.isGroupLeader()) {
            //stub = (IMember) UnicastRemoteObject.exportObject(this.member, 0);
            String statusLog = "This Process (" + member.getIdentifier() + ") selected as the group leader in " + member.getParentGroup().getGroupName();
            debug.updateStatus(statusLog);
            System.out.println("HURAAAYYY.....AM THE LEADER");

            server.rebind(member.getParentGroup().getGroupName(), stub);

        } else {
            String statusLog = "Member," + member.getName() + " (" + member.getIdentifier() + ") selected as the group leader in " + member.getParentGroup().getGroupName();
            debug.updateStatus(statusLog);
        }
        System.out.println("Elected : " + member.getName());
        debug.updateLeaderInTable(member.getName());
    }

    private ArrayList<String> getContacts() {
        ArrayList<String> c = new ArrayList<String>();
        HashMap<String, IMember> membersList = group.getMembersList();
        for (String m : membersList.keySet()) {
            c.add(m);
        }
        c.remove(memName);
        return c;
    }

    public void messageReceived(Message message) throws RemoteException {
        debug.messageReceived(message);
    }

    public void messageReleased(Message message) throws RemoteException {
        String dest = message.getDestination().getName();
        String src = message.getSource().getName();
        Logger.getLogger(Member.class.getName()).log(Level.INFO, "Message Released : from {0} to {1}", new Object[]{src, dest});
        if (dest.equals(memName)) {
            SingleChat c = chatWindows.get(src);
            if (c == null) {
                c = new SingleChat(this, false, src, member);
            }
            c.setVisible(true);
            c.recieveMessage(message);
            debug.messageReleased(message);
        }
    }

    public void ackReceived(Message message) throws RemoteException {
        String msg = "Acknowledgement received from " + message.getDestination().getName();
        String name = ((IMember) message.getSource()).getName();
        if (chatWindows.containsKey(name)) {
            SingleChat get = chatWindows.get(name);
            get.setVisible(true);
            get.sendMessage(message);
        }
        debug.updateStatus(msg);
    }

    public void multicastChat(Message message) {
        String msg = "Message multicasted to other " + member.getMembers().size() + " members";
        String txt = "";
        for (int i = 0; i < msg.length() / 2 + 1; i++) {
            txt += "- ";
        }
        debug.updateStatus(txt + "\n" + msg);
    }

    private void rejoin() throws HeadlessException {
        String groupName = group.getGroupName();
        try {
            ArrayList<String> params = new ArrayList<String>();
            params.add(groupName);
            params.add(memName);
            //member = new Member(memName, null);
            member.setGroupLeader(false);
            memContainer.setMember(member);

            this.stub = (IMember) member;
            Message msg = new Message(groupName, member, params, MESSAGE_TYPE.JOIN_REQUEST);

            memContainer.setStub(stub);

            String statusLog = "Member," + memContainer.getMember().getName() + " (" + memContainer.getMember().getIdentifier() + ") rejoined the Group " + groupName;
            System.out.println("############### " + group.getMemberCount());
            if (group.getMemberCount() < 1) {
                IGroupManagement igm = server.regLookUp("IGroupManagement");
                member.setGroupLeader(true);
                memContainer.setMember(igm.sendRequest(msg));
                server.rebind(groupName, stub);
                statusLog += " as the Group Leader";
            } else {
                stub = server.regMemLookUp(groupName);
                memContainer.setMember(stub.sendRequest(msg));
            }
            statusLog += ".";

            setMember(member);
            setMemContainer(memContainer);
            initialize(member, statusLog);

        } catch (RemoteException ex) {
            Logger.getLogger(NewMember.class.getName()).log(Level.SEVERE, null, ex);
        } catch (NotBoundException ex) {
            Logger.getLogger(NewMember.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void logout() throws NotBoundException, RemoteException {
        Message msg = new Message(member.getParentGroup().getGroupName(), member, memName, MESSAGE_TYPE.MEMBER_LEAVES);
        String groupName = member.getParentGroup().getGroupName();
        IMember leader = server.regMemLookUp(groupName);
        if (member.isGroupLeader()) {
            server.unbind(groupName);

        }
        leader.sendRequest(msg);
        lstContacts.setListData(new Vector());
        debug.updateStatus("# LOGGED OUT. # " + group.getMemberCount());
    }

    private void startChat() {
        String contactName = ((JLabel) lstContacts.getSelectedValue()).getText();
        SingleChat c;
        if (!chatWindows.containsKey(contactName)) {
            c = new SingleChat(this, false, contactName, memContainer.getStub());
            chatWindows.put(contactName, c);
        } else {
            c = chatWindows.get(contactName);
        }
        c.setVisible(true);
    }

    private void changeStatus() throws HeadlessException {
        if (!isOffline && ((JLabel) cmbStatus.getSelectedItem()).getText().equals("Offline")) {
            isOffline = true;
            try {
                logout();
                System.out.println("LOGGED OUT");
            } catch (NotBoundException ex) {
                Logger.getLogger(MemberWindow.class.getName()).log(Level.SEVERE, null, ex);
            } catch (RemoteException ex) {
                Logger.getLogger(MemberWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (isOffline && ((JLabel) cmbStatus.getSelectedItem()).getText().equals("Online")) {
            isOffline = false;
            System.out.println("Re-joined");
            rejoin();
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

        contactListPopUp = new javax.swing.JPopupMenu();
        refreshList = new javax.swing.JMenuItem();
        logoLabel = new javax.swing.JLabel();
        lblMemberName = new javax.swing.JLabel();
        cmbStatus = new javax.swing.JComboBox();
        jSeparator1 = new javax.swing.JSeparator();
        jScrollPane1 = new javax.swing.JScrollPane();
        lstContacts = new javax.swing.JList();
        jPanel1 = new javax.swing.JPanel();
        txtSearch = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        mnuMainMenu = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        toolsMenu = new javax.swing.JMenu();
        mnuDebug = new javax.swing.JCheckBoxMenuItem();
        mnuSingleChat = new javax.swing.JCheckBoxMenuItem();
        aboutMenu = new javax.swing.JMenu();

        refreshList.setMnemonic('r');
        refreshList.setText("Refresh");
        contactListPopUp.add(refreshList);

        setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
        setTitle("GCom Chat");
        setLocationByPlatform(true);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        logoLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pics/logo.png"))); // NOI18N

        lblMemberName.setFont(new java.awt.Font("Dialog", 0, 18));
        lblMemberName.setText("Praneeth Nilanga Peiris");

        cmbStatus.setFont(new java.awt.Font("Dialog", 0, 12));
        cmbStatus.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cmbStatusActionPerformed(evt);
            }
        });

        lstContacts.setFont(new java.awt.Font("Dialog", 0, 14));
        lstContacts.setComponentPopupMenu(contactListPopUp);
        lstContacts.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                lstContactsMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(lstContacts);

        jPanel1.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        txtSearch.setForeground(new java.awt.Color(-8355712,true));
        txtSearch.setText("Search Contacts");
        txtSearch.setBorder(null);
        txtSearch.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txtSearchFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtSearchFocusLost(evt);
            }
        });
        txtSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtSearchKeyReleased(evt);
            }
        });

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pics/search.png"))); // NOI18N

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 26, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(txtSearch, javax.swing.GroupLayout.DEFAULT_SIZE, 301, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE)
            .addComponent(txtSearch, javax.swing.GroupLayout.DEFAULT_SIZE, 25, Short.MAX_VALUE)
        );

        fileMenu.setMnemonic('F');
        fileMenu.setText("File");
        mnuMainMenu.add(fileMenu);

        toolsMenu.setMnemonic('T');
        toolsMenu.setText("Tools");

        mnuDebug.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_D, java.awt.event.InputEvent.CTRL_MASK));
        mnuDebug.setText("Debug");
        mnuDebug.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mnuDebugActionPerformed(evt);
            }
        });
        toolsMenu.add(mnuDebug);

        mnuSingleChat.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        mnuSingleChat.setSelected(true);
        mnuSingleChat.setText("Single Window Chat");
        toolsMenu.add(mnuSingleChat);

        mnuMainMenu.add(toolsMenu);

        aboutMenu.setMnemonic('A');
        aboutMenu.setText("About");
        mnuMainMenu.add(aboutMenu);

        setJMenuBar(mnuMainMenu);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jSeparator1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 343, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(logoLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(lblMemberName, javax.swing.GroupLayout.DEFAULT_SIZE, 240, Short.MAX_VALUE)
                            .addComponent(cmbStatus, 0, 240, Short.MAX_VALUE))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(lblMemberName)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cmbStatus, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(logoLabel))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSeparator1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 321, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents


    private void txtSearchFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtSearchFocusGained
        if (txtSearch.getText().trim().equalsIgnoreCase("Search Contacts")) {
            txtSearch.setText(null);
            txtSearch.setForeground(Color.BLACK);
        }
    }//GEN-LAST:event_txtSearchFocusGained

    private void txtSearchFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtSearchFocusLost
        if (txtSearch.getText().trim().equalsIgnoreCase("")) {
            txtSearch.setText("Search Contacts");
            txtSearch.setForeground(Color.GRAY);
        }
    }//GEN-LAST:event_txtSearchFocusLost

    private void lstContactsMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_lstContactsMouseClicked
        if (evt.getClickCount() == 2 && lstContacts.getSelectedIndex() != -1) {
            new Thread() {
                public void run() {
                    startChat();
                }
            }.start();

        }
    }//GEN-LAST:event_lstContactsMouseClicked


    private void txtSearchKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtSearchKeyReleased
        String txt = txtSearch.getText().trim();
        if (txt.isEmpty()) {
            fillContacts(contacts);
        } else {
            ArrayList<String> c = new ArrayList<String>();
            for (int i = 0; i < contacts.size(); i++) {
                String t = contacts.get(i);
                if (t.toLowerCase().contains(txt.toLowerCase())) {
                    c.add(t);
                }
            }
            fillContacts(c);
        }
    }//GEN-LAST:event_txtSearchKeyReleased

    private void mnuDebugActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mnuDebugActionPerformed
        if (mnuDebug.isSelected()) {
            debug.setVisible(true);
        } else {
            debug.setVisible(false);
        }
    }//GEN-LAST:event_mnuDebugActionPerformed

    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing

        int response = JOptionPane.showConfirmDialog(MemberWindow.this, "Are you sure want to exit?", "Exit GCom", JOptionPane.OK_CANCEL_OPTION, JOptionPane.INFORMATION_MESSAGE);
        if (response == JOptionPane.OK_OPTION) {
            try {
                logout();
                System.exit(0);
            } catch (AccessException ex) {
                Logger.getLogger(MemberWindow.class.getName()).log(Level.SEVERE, null, ex);
            } catch (RemoteException ex) {
                Logger.getLogger(MemberWindow.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NotBoundException ex) {
                Logger.getLogger(MemberWindow.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_formWindowClosing

    private void cmbStatusActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cmbStatusActionPerformed
        changeStatus();
    }//GEN-LAST:event_cmbStatusActionPerformed

//    /**
//     * @param args the command line arguments
//     */
//    public static void main(String args[]) {
//        java.awt.EventQueue.invokeLater(new Runnable() {
//
//            public void run() {
//                new MemberWindow().setVisible(true);
//            }
//        });
//    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenu aboutMenu;
    private javax.swing.JComboBox cmbStatus;
    private javax.swing.JPopupMenu contactListPopUp;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSeparator jSeparator1;
    private javax.swing.JLabel lblMemberName;
    private javax.swing.JLabel logoLabel;
    private javax.swing.JList lstContacts;
    private javax.swing.JCheckBoxMenuItem mnuDebug;
    private javax.swing.JMenuBar mnuMainMenu;
    private javax.swing.JCheckBoxMenuItem mnuSingleChat;
    private javax.swing.JMenuItem refreshList;
    private javax.swing.JMenu toolsMenu;
    private javax.swing.JTextField txtSearch;
    // End of variables declaration//GEN-END:variables

    /**
     * @return the group
     */
    public Group getGroup() {
        return group;
    }

    /**
     * @param group the group to set
     */
    public void setGroup(Group group) {
        this.group = group;
    }

    /**
     * @return the server
     */
    public RMIServer getServer() {
        return server;
    }

    /**
     * @param server the server to set
     */
    public void setServer(RMIServer server) {
        this.server = server;
    }

    /**
     * @return the member
     */
    public Member getMember() {
        return member;
    }

    /**
     * @param member the member to set
     */
    public void setMember(Member member) {
        this.member = member;
    }

    /**
     * @return the memContainer
     */
    public MemberContainer getMemContainer() {
        return memContainer;
    }

    /**
     * @param memContainer the memContainer to set
     */
    public void setMemContainer(MemberContainer memContainer) {
        this.memContainer = memContainer;
    }

    /**
     * @return the chatWindows
     */
    public HashMap<String, SingleChat> getChatWindows() {
        return chatWindows;
    }

    /**
     * @param chatWindows the chatWindows to set
     */
    public void setChatWindows(HashMap<String, SingleChat> chatWindows) {
        this.chatWindows = chatWindows;
    }

    public void killProcess() {
        System.exit(0);
    }

    public void vectorReceived(Object oldValue, Object newValue) {
        debug.vectorReceived(oldValue, newValue);
    }

}

class MyListRenderer extends DefaultListCellRenderer {

    @Override
    public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        //JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof JLabel) {
            this.setText(((JLabel) value).getText());
            this.setIcon(((JLabel) value).getIcon());
            this.setBackground(isSelected ? Color.GRAY : Color.white);
            //this.setForeground(isSelected ? Color.white : Color.black);
        }
        return this;
    }
}
