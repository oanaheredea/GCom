/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gcom.modules.group;

import gcom.RMIServer;
import gcom.interfaces.IGroupManagement;
import gcom.interfaces.IMember;
import gcom.interfaces.MESSAGE_TYPE;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.rmi.AccessException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author praneeth
 */
public class Member extends UnicastRemoteObject implements IMember {

    private String name;
    private int identifier;
    private Group parentGroup;
    private boolean isElectionParticipant = false;
    private boolean isGroupLeader;
    private final LinkedList<IMember> members;
    private transient RMIServer srv;
    protected PropertyChangeSupport propertyChangeSupport;
    private LinkedList<Message> holdingQueue;
    private static LinkedList<Message> receivedMessages;
    private HashMap<String, Integer> vectorClock;//*
    private Date joined;
    private boolean isOffline;

    public Member(String name, Group parent) throws RemoteException {
        this.parentGroup = parent;
        this.name = name;
        members = new LinkedList<IMember>();
        identifier = new Random().nextInt(100) + 1;
        holdingQueue = new LinkedList<Message>();
        receivedMessages = new LinkedList<Message>();
        setVectorClock(new HashMap<String, Integer>());
        propertyChangeSupport = new PropertyChangeSupport(this);
        joined = new Date();
        //initVectorClock();
    }

    public void addPropertyChangeListener(PropertyChangeListener listener) {
        propertyChangeSupport.addPropertyChangeListener(listener);
    }

    @Override
    public void memberAdded(IMember member) throws RemoteException {
        propertyChangeSupport.firePropertyChange("MemberAdded", null, member);
    }

    /**
     *
     * @param message
     * @return
     * @throws RemoteException
     * @throws AccessException
     */
    @Override
    public IMember sendRequest(Message message) throws RemoteException, AccessException {
        IMember m = message.getSource();

        if (message.getMessageType() == MESSAGE_TYPE.JOIN_REQUEST) {
            try {
                if (parentGroup.getGroupType() == Group.STATIC_GROUP && parentGroup.isFilled()) {
                    return null;
                }

                if (!this.parentGroup.getMembersList().containsKey(m.getName())) {
                    this.parentGroup.addMember(m);
                    this.addMember(m);
                    
                }

                    
                m.setParentGroup(this.parentGroup);
                Logger.getLogger(Member.class.getName()).log(Level.INFO, "Leader added member : {0}", m.getName());

                

                updateMembers(m);
                multicastMembersList(message);

                m.setVectorClock(this.vectorClock);
            } catch (GroupManagementException ex) {
                Logger.getLogger(Member.class.getName()).log(Level.SEVERE, null, ex);
            } catch (NotBoundException ex) {
                Logger.getLogger(Member.class.getName()).log(Level.SEVERE, null, ex);
            } catch (Exception ex) {
                Logger.getLogger(Member.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (message.getMessageOrderType() == MESSAGE_TYPE.MEMBER_LEAVES) {

            final HashMap<String, IMember> membersList = this.parentGroup.getMembersList();

            final IMember source = membersList.get(message.getMessage());

            Logger.getLogger(Member.class.getName()).log(Level.INFO, "Member Leaves : {0}", message.getMessage());

            try {
                LinkedList<IMember> llist = this.getMembers();
                int position = llist.indexOf(this.parentGroup.getMembersList().get(this.getName()));
                IMember neighbour = source.getNeighbour(position);

                parentGroup.removeMember(source.getName());
                //source.setOffline(true);

                Message emessage = new Message(parentGroup.getGroupName(), members.indexOf(neighbour), neighbour.getIdentifier(), MESSAGE_TYPE.ELECTION);

                for (final String key : membersList.keySet()) {

                    //new Thread() {
                    //  @Override
                    //public void run() {
                    try {
                        IMember mem = membersList.get(key);

                        mem.updateGroup(Member.this.parentGroup);
                        mem.removeMember(source);
                    } catch (RemoteException ex) {
                        Logger.getLogger(Member.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    //  }
                    //}.loadRMIEntries();

                }
                if (source.isGroupLeader()) {
                    if (!source.getName().equals(neighbour.getName())) {
                        neighbour.callElection(emessage);
                    }
                }

            } catch (RemoteException ex) {
                Logger.getLogger(Member.class.getName()).log(Level.SEVERE, null, ex);
            } catch (GroupManagementException ex) {
                Logger.getLogger(Member.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else if (message.getMessageOrderType() == MESSAGE_TYPE.MEMBER_OFFLINE) {

            final HashMap<String, IMember> membersList = this.parentGroup.getMembersList();

            final IMember source = membersList.get(message.getMessage());

            Logger.getLogger(Member.class.getName()).log(Level.INFO, "Member is offline : {0}", message.getMessage());

            try {
                LinkedList<IMember> llist = this.getMembers();
                int position = llist.indexOf(this.parentGroup.getMembersList().get(this.getName()));
                IMember neighbour = source.getNeighbour(position);

                //parentGroup.removeMember(source.getName());
                //source.setOffline(true);
                this.parentGroup.getMembersList().get(source.getName()).setOffline(true);

                Message emessage = new Message(parentGroup.getGroupName(), members.indexOf(neighbour), neighbour.getIdentifier(), MESSAGE_TYPE.ELECTION);

                for (final String key : membersList.keySet()) {

                    //new Thread() {
                    //  @Override
                    //public void run() {
                    try {
                        IMember mem = membersList.get(key);

                        mem.updateGroup(Member.this.parentGroup);
                        mem.offlineMember(source);
                    } catch (RemoteException ex) {
                        Logger.getLogger(Member.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    //  }
                    //}.loadRMIEntries();

                }
                if (source.isGroupLeader()) {
                    if (!source.getName().equals(neighbour.getName())) {
                        neighbour.callElection(emessage);
                    }
                }

            } catch (RemoteException ex) {
                Logger.getLogger(Member.class.getName()).log(Level.SEVERE, null, ex);
            }
        }




        try {
            IGroupManagement igm = srv.regLookUp("IGroupManagement");
            igm.updateGroup(parentGroup);
        } catch (GroupManagementException e) {
            Logger.getLogger(Member.class.getName()).log(Level.SEVERE, null, e);
        } catch (NotBoundException e) {
            Logger.getLogger(Member.class.getName()).log(Level.SEVERE, null, e);
        } catch (RemoteException e) {
            Logger.getLogger(Member.class.getName()).log(Level.SEVERE, null, e);
        }

        return m;
    }

    /**
     *
     * @param message
     * @throws AccessException
     * @throws NotBoundException
     */
    @Override
    public void multicastMembersList(Message message) throws RemoteException, AccessException, NotBoundException {
        final IMember newmember = message.getSource();
        final HashMap<String, IMember> membersList = this.parentGroup.getMembersList();
        for (final String key : membersList.keySet()) {
            // Create separate Threads for each multicastMembersList.
//            new Thread() {
//                @Override
//                public void run() {
            try {
                IMember m = membersList.get(key);

                m.updateGroup(Member.this.parentGroup);
                if (!(key.equals(this.getName()) || key.equals(newmember.getName()))) {
                    m.addMember(newmember);
                }
                m.memberAdded(newmember);
            } catch (RemoteException ex) {
                Logger.getLogger(Member.class.getName()).log(Level.SEVERE, null, ex);
            }
//                }
//            }.loadRMIEntries();

        }
    }

    /**
     *
     * @param parentGroup
     * @throws RemoteException
     */
    @Override
    public void updateGroup(Group parentGroup) throws RemoteException {
        this.parentGroup = parentGroup;
        Logger.getLogger(Member.class.getName()).log(Level.INFO, "Group Updated : {0}", this.parentGroup.getMemberCount());
    }

    /**
     *
     * @param member
     * @throws RemoteException
     */
    @Override
    public void updateMembers(final IMember member) throws RemoteException {
        for (final IMember m : this.getMembers()) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        member.addMember(m);
                    } catch (RemoteException ex) {
                        Logger.getLogger(Member.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }.start();
        }
    }

    /**
     * @return the members
     */
    @Override
    public LinkedList<IMember> getMembers() {
        return members;
    }

    @Override
    public void addMember(IMember member) throws RemoteException {
        if (!this.getMembers().contains(member)) {
            this.getMembers().add(member);
            this.uinitVectorClock(member);
        }
    }

    @Override
    public void removeMember(IMember member) throws RemoteException {
        if (this.getMembers().contains(member)) {
            this.getMembers().remove(member);
            vectorClock.remove(member.getName());
            propertyChangeSupport.firePropertyChange("MemberLeft", parentGroup, member);
        }
        Logger.getLogger(Member.class.getName()).log(Level.INFO, "Group Updated : Member Removed : {0}", members.size());
    }

    @Override
    public IMember getNeighbour(int pos) throws RemoteException {
        IMember neighbor = this.getMembers().get((pos++) % this.getMembers().size());

        while (neighbor.isOffline()) {
            neighbor = this.getMembers().get((pos++) % this.getMembers().size());
        }

        return neighbor;
    }

    public void printJoinOrder() throws RemoteException {
        System.out.print("Newbie Joined: Order: ");
        for (IMember m : this.getMembers()) {
            System.out.print(m.getName() + "->");
        }
        System.out.println("");
    }

    @Override
    public void callElection(Message emessage) throws RemoteException {
        LinkedList<IMember> llist = this.getMembers();
        int position = llist.indexOf(this.parentGroup.getMembersList().get(this.getName()));
        MESSAGE_TYPE messageType = emessage.getMessageType();

        if (messageType == MESSAGE_TYPE.ELECTION) {
            Logger.getLogger(Member.class.getName()).log(Level.INFO, "{0} : called an election.", getName());
            this.getNeighbour(position).voteElection(emessage);
        }
    }

    @Override
    public void voteElection(Message emessage) throws RemoteException {
        LinkedList<IMember> llist = this.getMembers();
        int position = llist.indexOf(this.parentGroup.getMembersList().get(this.getName()));
        if (emessage.getMessageType() == MESSAGE_TYPE.ELECTION) {
            if (this.getIdentifier() <= emessage.getMessageID()) {
                this.setElectionParticipant(true);
                Logger.getLogger(Member.class.getName()).log(Level.INFO, "{0} : forwarded an election message.", getName());
                if (emessage.getMessageID() == this.getNeighbour(position).getIdentifier()) {
                    this.getNeighbour(position).stopElection(emessage);
                } else {
                    this.getNeighbour(position).callElection(emessage);
                }

            } else {
                if (!this.isElectionParticipant()) {
                    this.setElectionParticipant(true);
                    emessage.setMessageID(this.getIdentifier());
                    Logger.getLogger(Member.class.getName()).log(Level.INFO, "{0} : modified the election message.", getName());
                    this.callElection(emessage);
                }
            }
        }

        if (emessage.getMessageType() == MESSAGE_TYPE.ELECTED && this.getIdentifier() != emessage.getMessageID()) {
            this.setElectionParticipant(false);
            this.setGroupLeader(false);
            this.getParentGroup().setLeader(emessage.getSource());
            electionCompleted(emessage.getSource());
            Logger.getLogger(Member.class.getName()).log(Level.INFO, "{0} : elected as new leader.", emessage.getSource().getName());
            this.getNeighbour(position).voteElection(emessage);
        }

    }

    @Override
    public void stopElection(Message emessage) throws RemoteException {
        LinkedList<IMember> llist = this.getMembers();
        IMember leader = this.parentGroup.getMembersList().get(this.getName());

        int position = llist.indexOf(leader);

        this.setElectionParticipant(false);

        this.setGroupLeader(true);
        this.getParentGroup().setLeader(leader);
        emessage.setType(MESSAGE_TYPE.ELECTED);
        emessage.setSource(leader);
        Logger.getLogger(Member.class.getName()).log(Level.INFO, "New Leader Appointed : {0}[{1}] : {2}", new Object[]{getName(), getIdentifier(), emessage.getMessageID()});

        electionCompleted(leader);
        this.getNeighbour(position).voteElection(emessage);
    }

    @Override
    public void electionCompleted(IMember member) throws RemoteException {
        propertyChangeSupport.firePropertyChange("ElectionFinished", null, member);
    }

///////////////////////// Causal Ordering
    @Override
    public void multicastMessages(final Message message, boolean isInitReliable) throws RemoteException {
        //MESSAGE_TYPE multicasttype = message.getMulticastType();
        MESSAGE_TYPE messageOrderType = message.getMessageOrderType();
        final HashMap<String, IMember> membersList = this.parentGroup.getMembersList();
        if (messageOrderType == MESSAGE_TYPE.CAUSAL) {
            if (isInitReliable) {
                updateVectorCell(this.getName());
                message.setVectorClock(this.getVectorClock());
                Logger.getLogger(Member.class.getName()).log(Level.INFO, "Vector clock received : ", this.getVectorClock());
            }

            for (final String key : membersList.keySet()) {
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            IMember m = membersList.get(key);
                            if (!m.isOffline()) {
                                m.deliver(message);
                            }
                        } catch (RemoteException ex) {
                            Logger.getLogger(Member.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }.start();

            }
        } else if (messageOrderType == MESSAGE_TYPE.UNORDERED) {
            for (final String key : membersList.keySet()) {
                // Create separate Threads for each multicastMembersList.
                new Thread() {
                    @Override
                    public void run() {
                        try {
                            IMember m = membersList.get(key);
                            if (!m.isOffline()) {
                                m.deliver(message);
                            }
                        } catch (RemoteException ex) {
                            Logger.getLogger(Member.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }.start();

            }
        }
        propertyChangeSupport.firePropertyChange("InsertMsgToDB", message, membersList.keySet());
        Logger.getLogger(Member.class.getName()).log(Level.INFO, "Message Multicasted. : {0} : {1}", new Object[]{message.getMessage(), message.getMessageOrderType()});
    }

    /**
     *
     * @param message
     * @throws RemoteException
     */
    @Override
    public synchronized void deliver(Message message) throws RemoteException {

        if (message.getMulticastType() == MESSAGE_TYPE.RELIABLE) {
            if (!isEntry(message)) {
                receivedMessages.add(message);

                holdingQueue.add(message);
                Logger.getLogger(Member.class.getName()).log(Level.INFO, "Message Receieved :  {0}", message.getMessage());

                messageReceived(message);

                if (!message.getSource().getName().equals(this.getName())) {
                    //message.setSource(this);
                    this.multicastMessages(message, false);
                }
            }
        } else if (message.getMulticastType() == MESSAGE_TYPE.BASIC) {
            holdingQueue.add(message);
            Logger.getLogger(Member.class.getName()).log(Level.INFO, "Local:  {0}", message.getMessage());
            messageReceived(message);
        }
        if (message.getSource().getName().equals(getName())) {
            holdingQueue.remove(message);
            Logger.getLogger(Member.class.getName()).log(Level.INFO, "My Message Released:  {0}", message.getMessage());
            propertyChangeSupport.firePropertyChange("MyOwnMessageReleased", null, message);
        }
    }

    public boolean isEntry(Message message) {
        for (int i = 0; i < receivedMessages.size(); i++) {
            if (receivedMessages.get(i).getTimeStamp() == message.getTimeStamp() && receivedMessages.get(i).getMessage().equals(message.getMessage())) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @param message
     */
    public void messageReceived(Message message) {
        propertyChangeSupport.firePropertyChange("MessageReceived", null, message);
    }

    /**
     *
     * @param message
     * @return
     * @throws RemoteException
     */
    @Override
    public boolean releaseMessages(Message message) throws RemoteException {
        boolean isReleased = false;
        if (parentGroup.getGroupDef().getOrdType() == MESSAGE_TYPE.CAUSAL) {
            HashMap<String, Integer> temp = new HashMap<String, Integer>(this.getVectorClock());
            Logger.getLogger(Member.class.getName()).log(Level.INFO, "Local:  {0}", this.getVectorClock().toString());
            Logger.getLogger(Member.class.getName()).log(Level.INFO, "Remote: {0}", message.getVectorClock().toString());

            if ((vectorClock.get(message.getSource().getName()) == message.getVectorClock().get(message.getSource().getName()) - 1) && compareClock(message)) {
                //if ((vectorClock.get(message.getSource().getName()) == message.getVectorClock().get(message.getSource().getName()) - 1)) {
                holdingQueue.remove(message);

                Logger.getLogger(Member.class.getName()).log(Level.INFO, "Message Released:  {0}", message.getMessage());

                propertyChangeSupport.firePropertyChange("MessageReleased", null, message);

                int uvalue = vectorClock.get(message.getSource().getName()) + 1;
                vectorClock.put(message.getSource().getName(), uvalue);

                syncClock(message.getVectorClock());

                Message rmessage = new Message(this.getParentGroup().getGroupName(), this.parentGroup.getMembersList().get(this.getName()), message.getMessage(), MESSAGE_TYPE.ACKNOWLEDGEMENT);
                rmessage.setDestination(message.getSource());

                message.getSource().getAcknowledgement(rmessage);

                isReleased = true;
            }
            propertyChangeSupport.firePropertyChange("VectorReceived", isReleased, new Object[]{temp, message.getVectorClock()});
        } else if (parentGroup.getGroupDef().getOrdType() == MESSAGE_TYPE.UNORDERED) {
            holdingQueue.remove(message);
            Logger.getLogger(Member.class.getName()).log(Level.INFO, "Message Released:  {0}", message.getMessage());
            propertyChangeSupport.firePropertyChange("MessageReleased", null, message);
            Message rmessage = new Message(this.getParentGroup().getGroupName(), this.parentGroup.getMembersList().get(this.getName()), message.getMessage(), MESSAGE_TYPE.ACKNOWLEDGEMENT);
            rmessage.setDestination(message.getSource());
            message.getSource().getAcknowledgement(rmessage);
            isReleased = true;
        }

        return isReleased;
    }

    private void syncClock(HashMap<String, Integer> vectorClock) {
        for (String memName : vectorClock.keySet()) {
            if (!(memName.equals(this.getName()))) {
                Integer k1 = this.getVectorClock().get(memName);
                Integer k2 = vectorClock.get(memName);
                if (k1 < k2) {
                    vectorClock.put(memName, k2);
                }
            }
        }
    }

    private boolean compareClock(Message message) throws RemoteException {
        boolean flag = true;
        for (String memName : message.getVectorClock().keySet()) {
            if (!(memName.equals(message.getSource().getName()))) {
                Integer k1 = this.getVectorClock().get(memName);
                Integer k2 = message.getVectorClock().get(memName);
                if (k1 < k2) {
                    flag &= false;
                    return flag;
                }
            }
        }
        return flag;
    }

    /**
     *
     * @param message
     * @throws RemoteException
     */
    @Override
    public void getAcknowledgement(Message message) throws RemoteException {
        propertyChangeSupport.firePropertyChange("AckReceived", null, message);
    }

    /**
     *
     * @throws RemoteException
     */
    @Override
    public void initVectorClock() throws RemoteException {
        HashMap<String, IMember> membersList = this.parentGroup.getMembersList();
        for (String key : membersList.keySet()) {
            IMember m = membersList.get(key);
            getVectorClock().put(m.getName(), 0);
        }
    }

    /**
     *
     * @param newmember
     * @throws RemoteException
     */
    @Override
    public void uinitVectorClock(IMember newmember) throws RemoteException {
        this.getVectorClock().put(newmember.getName(), 0);
        Logger.getLogger(Member.class.getName()).log(Level.INFO, this.getVectorClock().toString());
    }

    public void updateVectorCell(String memName) {
        Integer pi = getVectorClock().get(memName);
        pi += 1;
        getVectorClock().put(memName, pi);
    }

    /**
     * @return the vectorClock
     */
    public HashMap<String, Integer> getVectorClock() {
        return vectorClock;
    }

    /**
     * @param vectorClock the vectorClock to set
     */
    public void setVectorClock(HashMap<String, Integer> vectorClock) throws RemoteException {
        this.vectorClock = vectorClock;
    }

    /**
     * @return the parentGroup
     */
    @Override
    public Group getParentGroup() {
        return parentGroup;
    }

    /**
     * @param parentGroup the parentGroup to set
     */
    @Override
    public void setParentGroup(Group parentGroup) {
        this.parentGroup = parentGroup;

    }

    /**
     * @return the id
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * @param id the id to set
     */
    public void setName(String id) {
        this.name = id;
    }

    /**
     * @return the identifier
     */
    @Override
    public int getIdentifier() {
        return identifier;
    }

    /**
     * @param identifier the identifier to set
     */
    @Override
    public void setIdentifier(int identifier) {
        this.identifier = identifier;
    }

    /**
     * @return the isElectionParticipant
     */
    @Override
    public boolean isElectionParticipant() {
        return isElectionParticipant;
    }

    /**
     * @param isElectionParticipant the isElectionParticipant to set
     */
    @Override
    public void setElectionParticipant(boolean isElectionParticipant) {
        this.isElectionParticipant = isElectionParticipant;
    }

    /**
     * @return the isGroupLeader
     */
    @Override
    public boolean isGroupLeader() {
        return isGroupLeader;
    }

    /**
     * @param isGroupLeader the isGroupLeader to set
     */
    @Override
    public void setGroupLeader(boolean isGroupLeader) {
        this.isGroupLeader = isGroupLeader;
    }

    /**
     * @return the holdingQueue
     */
    @Override
    public LinkedList<Message> getHoldingQueue() {
        return holdingQueue;
    }

    /**
     * @return the srv
     */
    public RMIServer getSrv() {
        return srv;
    }

    /**
     * @param srv the srv to set
     */
    public void setSrv(RMIServer srv) {
        this.srv = srv;
    }

    @Override
    public void killProcess() throws RemoteException {
        final HashMap<String, IMember> membersList = this.parentGroup.getMembersList();
        for (final String key : membersList.keySet()) {
            new Thread() {
                @Override
                public void run() {
                    try {
                        IMember m = membersList.get(key);
                        if (!m.isGroupLeader()) {
                            m.kill();
                        }
                    } catch (RemoteException ex) {
                        Logger.getLogger(Member.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }.start();

        }
        propertyChangeSupport.firePropertyChange("Kill", null, null);
    }

    @Override
    public void kill() throws RemoteException {
        propertyChangeSupport.firePropertyChange("Kill", null, null);
    }

    /**
     * @return the joined
     */
    public Date getJoined() throws RemoteException {
        return joined;
    }

    /**
     * @param joined the joined to set
     */
    public void setJoined(Date joined) throws RemoteException {
        this.joined = joined;
    }

    /**
     * @return the isOffline
     */
    public boolean isOffline() throws RemoteException {
        return isOffline;
    }

    /**
     * @param isOffline the isOffline to set
     */
    public void setOffline(boolean isOffline) throws RemoteException {
        this.isOffline = isOffline;
    }

    @Override
    public void offlineMember(IMember source) throws RemoteException {
        propertyChangeSupport.firePropertyChange("MemberOffline", parentGroup, source);
    }
}
