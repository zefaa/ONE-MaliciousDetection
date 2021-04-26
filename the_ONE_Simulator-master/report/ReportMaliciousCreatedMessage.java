/* 
 * Copyright 2010 Aalto University, ComNet
 * Released under GPLv3. See LICENSE.txt for details. 
 */
package report;

import core.DTNHost;
import core.Message;
import core.MessageListener;

/**
 * Report for of amount of messages delivered vs. time. A new report line is
 * created every time when either a message is created or delivered. Messages
 * created during the warm up period are ignored. For output syntax, see
 * {@link #HEADER}.
 */
public class ReportMaliciousCreatedMessage extends Report implements MessageListener {

    public static String HEADER = "# Created | S | D | M |";
    private int created;
    private int delivered;

    /**
     * Constructor.
     */
    public ReportMaliciousCreatedMessage() {
        init();
    }

    @Override
    public void init() {
        super.init();
        created = 0;
        delivered = 0;
        write(HEADER);
    }

    public void messageTransferred(Message m, DTNHost from, DTNHost to,
            boolean firstDelivery) {
//		if (firstDelivery && !isWarmup() && !isWarmupID(m.getId())) {
//			delivered++;
////			reportValues();
//		}
       // write(m.getCreationTime() + " " + m.getFrom() + " " + m.getTo() + " " + m.getId() + " ");
    }

    public void newMessage(Message m) {
        if (isWarmup()) {
            addWarmupID(m.getId());
            return;
        }
        created++;
        write(m.getCreationTime() + " " + m.getFrom() + " " + m.getTo() + " " + m.getId() + " ");
//		reportValues();
    }

    // nothing to implement for the rest
    public void messageDeleted(Message m, DTNHost where, boolean dropped) {
    }

    public void messageTransferAborted(Message m, DTNHost from, DTNHost to) {
    }

    public void messageTransferStarted(Message m, DTNHost from, DTNHost to) {
          

    }

    @Override
    public void done() {
        super.done();
    }
}
