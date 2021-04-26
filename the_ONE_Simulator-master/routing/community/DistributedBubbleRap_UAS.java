/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package routing.community;

import core.Connection;
import core.DTNHost;
import core.Message;
import core.Settings;
import core.SimClock;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import routing.DecisionEngineRouter;
import routing.MessageRouter;
import routing.RoutingDecisionEngine;
import static routing.community.UasBubbleRap.CENTRALITY_ALG_SETTING;
import static routing.community.UasBubbleRap.COMMUNITY_ALG_SETTING;

/**
 *
 * @author M S I
 */
public class DistributedBubbleRap_UAS implements RoutingDecisionEngine, CommunityDetectionEngine, bantuReport_DistBubb {

    /**
     * Community Detection Algorithm to employ -setting id {@value}
     */
    public static final String COMMUNITY_ALG_SETTING = "communityDetectAlg";
    /**
     * Centrality Computation Algorithm to employ -setting id {@value}
     */
    public static final String CENTRALITY_ALG_SETTING = "centralityAlg";

    protected Map<DTNHost, Double> startTimestamps;
    protected Map<DTNHost, List<Duration>> connHistory;

    protected CommunityDetection community;
    protected CentralityKu centrality;

    /**
     * Constructs a DistributedBubbleRap Decision Engine based upon the settings
     * defined in the Settings object parameter. The class looks for the class
     * names of the community detection and centrality algorithms that should be
     * employed used to perform the routing.
     *
     * @param s Settings to configure the object
     */
    public DistributedBubbleRap_UAS(Settings s) {
        if (s.contains(COMMUNITY_ALG_SETTING)) {
            this.community = (CommunityDetection) s.createIntializedObject(s.getSetting(COMMUNITY_ALG_SETTING));
        } else {
            this.community = new SimpleCommunityDetection(s);
        }

        if (s.contains(CENTRALITY_ALG_SETTING)) {
            this.centrality = (CentralityKu) s.createIntializedObject(s.getSetting(CENTRALITY_ALG_SETTING));
        }
        //else
        //this.centrality = new SWindowCentrality(s);
    }

    public DistributedBubbleRap_UAS(DistributedBubbleRap_UAS proto) {
        this.community = proto.community.replicate();
        this.centrality = (CentralityKu) proto.centrality.replicate();
        startTimestamps = new HashMap<DTNHost, Double>();
        connHistory = new HashMap<DTNHost, List<Duration>>();;
    }

    @Override
    public void connectionUp(DTNHost thisHost, DTNHost peer) {
        /**
         * Starts timing the duration of this new connection and informs the
         * community detection object that a new connection was formed.
         *
         * @see
         * routing.RoutingDecisionEngine#doExchangeForNewConnection(core.Connection,
         * core.DTNHost)
         */
    }

    @Override
    public void connectionDown(DTNHost thisHost, DTNHost peer) {
        //    double time = startTimestamps.get(peer);
        double time = cek(thisHost, peer);
        double etime = SimClock.getTime();

        // Find or create the connection history list
        List<Duration> history;
        if (!connHistory.containsKey(peer)) {
            history = new LinkedList<Duration>();
            connHistory.put(peer, history);
        } else {
            history = connHistory.get(peer);
        }

        // add this connection to the list
        if (etime - time > 0) {
            history.add(new Duration(time, etime));
        }

        CommunityDetection peerCD = this.getOtherDecisionEngine(peer).community;

        // inform the community detection object that a connection was lost.
        // The object might need the whole connection history at this point.
        community.connectionLost(thisHost, peer, peerCD, history);

        startTimestamps.remove(peer);
    }

    @Override
    public void doExchangeForNewConnection(Connection con, DTNHost peer) {
        DTNHost myHost = con.getOtherNode(peer);
        DistributedBubbleRap_UAS de = this.getOtherDecisionEngine(peer);

        this.startTimestamps.put(peer, SimClock.getTime());
        de.startTimestamps.put(myHost, SimClock.getTime());

        this.community.newConnection(myHost, peer, de.community);
    }

    public double cek(DTNHost thisHost, DTNHost peer) {
        if (startTimestamps.containsKey(thisHost)) {
            startTimestamps.get(peer);
        }
        return 0;
    }

    public boolean newMessage(Message m) {
        return true; // Always keep and attempt to forward a created message
    }

    public boolean isFinalDest(Message m, DTNHost aHost) {
        return m.getTo() == aHost; // Unicast Routing
    }

    public boolean shouldSaveReceivedMessage(Message m, DTNHost thisHost) {
        return m.getTo() != thisHost;
    }

    public boolean shouldSendMessageToHost(Message m, DTNHost otherHost, DTNHost thisHost) {
        if (m.getTo() == otherHost) {
            return true; // trivial to deliver to final dest
        }

        DTNHost dest = m.getTo();
        DistributedBubbleRap_UAS de = getOtherDecisionEngine(otherHost);
        // Which of us has the dest in our local communities, this host or the peer
        boolean peerInCommunity = de.commumesWithHost(dest);
        boolean meInCommunity = this.commumesWithHost(dest);

        if (peerInCommunity && !meInCommunity) // peer is in local commun. of dest
        {
            return true;
        } else if (!peerInCommunity && meInCommunity) // I'm in local commun. of dest
        {
            return false;
        } else if (peerInCommunity) // we're both in the local community of destination
        {
            // Forward to the one with the higher local centrality (in our community)
            if (de.getLocalCentrality() > this.getLocalCentrality()) {
                return true;
            } else {
                return false;
            }
        } // Neither in local community, forward to more globally central node
        else if (de.getGlobalCentrality() > this.getGlobalCentrality()) {
            return true;
        }

        return false;
    }

    public boolean shouldDeleteSentMessage(Message m, DTNHost otherHost) {
        // DiBuBB allows a node to remove a message once it's forwarded it into the
        // local community of the destination
        DistributedBubbleRap_UAS de = this.getOtherDecisionEngine(otherHost);
        return de.commumesWithHost(m.getTo())
                && !this.commumesWithHost(m.getTo());
    }

    public boolean shouldDeleteOldMessage(Message m, DTNHost hostReportingOld) {
        DistributedBubbleRap_UAS de = this.getOtherDecisionEngine(hostReportingOld);
        return de.commumesWithHost(m.getTo())
                && !this.commumesWithHost(m.getTo());
    }

    public RoutingDecisionEngine replicate() {
        return new DistributedBubbleRap_UAS(this);
    }

    protected boolean commumesWithHost(DTNHost h) {
        return community.isHostInCommunity(h);
    }

    protected double getLocalCentrality() {
        return this.centrality.getLocalCentrality(connHistory, community);
    }

    protected double getGlobalCentrality() {
        return this.centrality.getGlobalCentrality(connHistory);
    }

    private DistributedBubbleRap_UAS getOtherDecisionEngine(DTNHost h) {
        MessageRouter otherRouter = h.getRouter();
        assert otherRouter instanceof DecisionEngineRouter : "This router only works "
                + " with other routers of same type";

        return (DistributedBubbleRap_UAS) ((DecisionEngineRouter) otherRouter).getDecisionEngine();
    }

    public Set<DTNHost> getLocalCommunity() {
        return this.community.getLocalCommunity();
    }

    public void update(DTNHost thisHost) {
    }

    @Override
    public int[] Centrality() {
        return this.centrality.getArrayGlobalCentrality(connHistory);
    }

}
