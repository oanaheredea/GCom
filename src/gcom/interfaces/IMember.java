/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gcom.interfaces;

import gcom.modules.group.Group;
import gcom.modules.group.Message;
import java.io.Serializable;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

/**
 *
 * @author samtube405
 */
public interface IMember extends Remote, Serializable {

    public IMember sendRequest(Message message) throws RemoteException;

    public void multicastMembersList(Message message) throws RemoteException, AccessException, NotBoundException;

    public void updateGroup(Group parentGroup) throws RemoteException;

    public void updateMembers(IMember member) throws RemoteException;

    public Group getParentGroup() throws RemoteException;

    public void setParentGroup(Group parentGroup) throws RemoteException;

    public boolean isGroupLeader() throws RemoteException;

    public void setGroupLeader(boolean isGroupLeader) throws RemoteException;

    public String getName() throws RemoteException;

    public LinkedList<IMember> getMembers() throws RemoteException;

    public void addMember(IMember member) throws RemoteException;

    public void removeMember(IMember member) throws RemoteException;

    public IMember getNeighbour(int pos) throws RemoteException;

    public boolean isElectionParticipant() throws RemoteException;

    public void setElectionParticipant(boolean isElectionParticipant) throws RemoteException;

    public void callElection(Message emessage) throws RemoteException;

    public void voteElection(Message emessage) throws RemoteException;

    public void stopElection(Message emessage) throws RemoteException;

    public int getIdentifier() throws RemoteException;

    public void setIdentifier(int identifier) throws RemoteException;

    public void memberAdded(IMember member) throws RemoteException;

    public void electionCompleted(IMember member) throws RemoteException;

    public void multicastMessages(Message message, boolean isInitReliable) throws RemoteException;

    public void deliver(Message message) throws RemoteException;

    public void getAcknowledgement(Message message) throws RemoteException;

    public boolean releaseMessages(Message message) throws RemoteException;

    public LinkedList<Message> getHoldingQueue() throws RemoteException;

    public void initVectorClock() throws RemoteException;

    public void setVectorClock(HashMap<String, Integer> vectorClock) throws RemoteException;

    public void uinitVectorClock(IMember newmember) throws RemoteException;

    public void killProcess() throws RemoteException;

    public void kill() throws RemoteException;

    public Date getJoined() throws RemoteException;

    public void setJoined(Date joined) throws RemoteException;

    public boolean isOffline() throws RemoteException;

    public void setOffline(boolean isOffline) throws RemoteException;

    public void offlineMember(IMember source) throws RemoteException;
}
