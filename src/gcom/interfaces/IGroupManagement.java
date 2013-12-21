/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gcom.interfaces;

import gcom.modules.group.GroupManagementException;
import gcom.modules.group.Message;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.HashMap;

/**
 *
 * @author samtube405
 */
public interface IGroupManagement extends Remote {

    public HashMap<String, Integer> getGroupDetails() throws RemoteException;

    public IMember sendRequest(gcom.modules.group.Message message) throws RemoteException;

    public void updateStatus(IMessage message) throws RemoteException, GroupManagementException;

    public void updateStatus(Message message, MESSAGE_TYPE type) throws RemoteException, GroupManagementException;
}
