/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/*
 * SingleChat.java
 *
 * Created on Dec 5, 2013, 7:28:30 PM
 */
package gui.member;

import gcom.interfaces.IMember;
import gcom.interfaces.MESSAGE_ORDERING;
import gcom.interfaces.MESSAGE_TYPE;
import gcom.modules.group.GroupDef;
import gcom.modules.group.Message;
import gui.GComWindow;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ImageIcon;

/**
 *
 * @author Praneeth
 */
public class SingleChat extends javax.swing.JFrame {

    /**
     * Creates new form SingleChat
     */
    private String contact;
    private IMember member;
    
    private MemberWindow memWindow;
    
    public SingleChat(MemberWindow parent, boolean modal, String contact, IMember member) {
        initComponents();
        this.memWindow = parent;
        this.contact = contact;
        this.member = member;
        txtHistory.setBackground(this.getBackground());
        lblContactName.setText(contact);
        setTitle("Chat with " + contact);
        setIconImage(new ImageIcon(GComWindow.class.getResource("/pics/logo.png")).getImage());
    }
    
    public void sendMessage(Message msg) {
        updateChat("Me : " + msg.getMessage());
        txtChat.setText("");
    }
    
    public void recieveMessage(Message msg) throws RemoteException {
        updateChat(msg.getSource().getName() + " : " + msg.getMessage());
        txtChat.setText("");
    }
    
    private void updateChat(String msg) {
        txtHistory.setText(txtHistory.getText() + msg + "\n");
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        txtHistory = new javax.swing.JTextArea();
        jScrollPane2 = new javax.swing.JScrollPane();
        txtChat = new javax.swing.JTextArea();
        jPanel1 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        lblContactName = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setLocationByPlatform(true);
        setResizable(false);

        jSplitPane1.setDividerLocation(325);
        jSplitPane1.setDividerSize(2);
        jSplitPane1.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        jSplitPane1.setResizeWeight(0.75);

        txtHistory.setEditable(false);
        txtHistory.setColumns(20);
        txtHistory.setLineWrap(true);
        txtHistory.setRows(5);
        jScrollPane1.setViewportView(txtHistory);

        jSplitPane1.setTopComponent(jScrollPane1);

        txtChat.setColumns(20);
        txtChat.setForeground(new java.awt.Color(-8355712,true));
        txtChat.setRows(5);
        txtChat.setText("Type your text here...");
        txtChat.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusGained(java.awt.event.FocusEvent evt) {
                txtChatFocusGained(evt);
            }
            public void focusLost(java.awt.event.FocusEvent evt) {
                txtChatFocusLost(evt);
            }
        });
        txtChat.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                txtChatKeyPressed(evt);
            }
        });
        jScrollPane2.setViewportView(txtChat);

        jSplitPane1.setRightComponent(jScrollPane2);

        jLabel2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/pics/online.png"))); // NOI18N

        lblContactName.setFont(new java.awt.Font("Dialog", 0, 18));
        lblContactName.setText("Praneeth Nilanga Peiris");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 25, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(lblContactName, javax.swing.GroupLayout.PREFERRED_SIZE, 339, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(87, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(lblContactName, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel2, javax.swing.GroupLayout.DEFAULT_SIZE, 24, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jSplitPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 455, Short.MAX_VALUE))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jSplitPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 413, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void txtChatFocusGained(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtChatFocusGained
        if (txtChat.getText().trim().equalsIgnoreCase("Type your text here...")) {
            txtChat.setText(null);
            txtChat.setForeground(Color.BLACK);
        }
    }//GEN-LAST:event_txtChatFocusGained

    private void txtChatFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_txtChatFocusLost
        if (txtChat.getText().trim().equalsIgnoreCase("")) {
            txtChat.setText("Type your text here...");
            txtChat.setForeground(Color.GRAY);
        }
    }//GEN-LAST:event_txtChatFocusLost

    private void txtChatKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtChatKeyPressed
        if (evt.getKeyCode() == KeyEvent.VK_ENTER) {
            String msg = txtChat.getText();
            Message message;
            try {
                message = new Message(member.getParentGroup().getGroupName(), member, null, msg, MESSAGE_TYPE.CAUSAL_MULTICAST);
                message.setDestination(member.getParentGroup().getMembersList().get(contact));
                GroupDef groupDef = member.getParentGroup().getGroupDef();
                if (groupDef.getOrdType() == MESSAGE_ORDERING.CAUSAL) {
                    member.multicastMessages(message);
                } else if (groupDef.getOrdType() == MESSAGE_ORDERING.UNORDERED) {
                    member.multicastMembersList(message);
                }
                memWindow.multicastChat(message);
            } catch (RemoteException | NotBoundException ex) {
                Logger.getLogger(SingleChat.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }//GEN-LAST:event_txtChatKeyPressed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel jLabel2;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JLabel lblContactName;
    private javax.swing.JTextArea txtChat;
    private javax.swing.JTextArea txtHistory;
    // End of variables declaration//GEN-END:variables
}
