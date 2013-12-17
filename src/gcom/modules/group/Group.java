/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package gcom.modules.group;

import gcom.interfaces.IGroup;
import gcom.modules.com.CommunicationMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

/**
 *
 * @author praneeth
 */
public class Group implements IGroup {

    private String groupID;
    private HashMap<String, Member> members;
    private CommunicationMode comMode;
    private int maxMembers = 2000;
    private static int counter;
    private GroupDef gDef;

    public Group() {
        this("G-" + counter++);
    }

    public Group(String groupID) {
        this.groupID = groupID;
        members = new HashMap<String, Member>();
    }

    public Group(String groupID, int maxMembers) {
        this(groupID);
        this.maxMembers = maxMembers;
    }

    public Group(GroupDef gDef) {
        this.gDef = gDef;
        members = new HashMap<String, Member>();
    }

    public void addMember(Member member) throws GroupManagementException {
        String memberId = member.getName();
        if (members.containsKey(member.getName())) {
            throw new GroupManagementException("Duplicate Member Id : Member id" + memberId + " already exists in " + groupID);
        } else {

            members.put(memberId, member);

        }
    }

    public void removeMember(String memberId) throws GroupManagementException {
        if (members.containsKey(memberId)) {
            members.remove(memberId);
        } else {
            throw new GroupManagementException("Invalid Member Id : Member id" + memberId + " does not exist in " + groupID);
        }
    }

    @Override
    public Member getMember(String memberId) throws GroupManagementException {
        if (members.containsKey(memberId)) {
            return members.get(memberId);
        } else {
            throw new GroupManagementException("Invalid Member Id : Member id" + memberId + " does not exist in " + groupID);
        }
    }

    @Override
    public ArrayList<Member> getMembersList() {
        return new ArrayList(members.values());
    }

    /**
     * @return the groupID
     */
    @Override
    public String getGroupName() {
        return gDef.getGroupName();
    }

    /**
     * @return the comMode
     */
    @Override
    public CommunicationMode getComMode() {
        return comMode;
    }

    /**
     * @param comMode the comMode to set
     */
    public void setComMode(CommunicationMode comMode) {
        this.comMode = comMode;
    }

    /**
     * @return the maxMembers
     */
    @Override
    public int getMaxMembers() {
        return maxMembers;
    }

    @Override
    public int getMemberCount() {
        if (members != null) {
            return members.size();
        }
        return 0;
    }

    /**
     * @param maxMembers the maxMembers to set
     */
    public void setMaxMembers(int maxMembers) {
        this.maxMembers = maxMembers;
    }

    @Override
    public void send(Message message) {
        Set<String> keySet = members.keySet();
        Iterator<String> iterator = keySet.iterator();
        while (iterator.hasNext()) {
            String next = iterator.next();
            Member m = members.get(next);
            send(m, message);
        }
    }

    @Override
    public void initiateElection(Member initiater) {
        ArrayList<Member> membersList = getMembersList();
        for (Member m : membersList) {
            m.setElectionParticipant(false);
        }
        initiater.setElectionParticipant(true);

        // make the initiater 1st in the list
        membersList.remove(initiater);
        membersList.add(0, initiater);

    }

    private void send(Member member, Message message) {
        // call members server method ?
    }

    @Override
    public void finalize() throws Throwable {
        super.finalize();
        System.out.println("Group " + groupID + " is destroyed.");
    }
}
