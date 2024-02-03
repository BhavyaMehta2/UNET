import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

import org.arl.fjage.Agent;
import org.arl.fjage.AgentID;
import org.arl.fjage.AgentLocalRandom;
import org.arl.fjage.Message;
import org.arl.fjage.OneShotBehavior;
import org.arl.fjage.Performative;
import org.arl.fjage.WakerBehavior;
import org.arl.fjage.param.Parameter;
import org.arl.unet.RefuseRsp;
import org.arl.unet.Services;
import org.arl.unet.UnetAgent;
import org.arl.unet.mac.MacCapability;
import org.arl.unet.mac.MacParam;
import org.arl.unet.mac.ReservationCancelReq;
import org.arl.unet.mac.ReservationReq;
import org.arl.unet.mac.ReservationRsp;
import org.arl.unet.mac.ReservationStatus;
import org.arl.unet.mac.ReservationStatusNtf;
import org.arl.unet.net.RouteDiscoveryNtf;
import org.arl.unet.net.RouteDiscoveryReq;
import org.arl.unet.phy.PhysicalChannelParam;
import org.arl.unet.phy.PhysicalParam;
import org.arl.unet.phy.RxFrameNtf;
import org.arl.unet.phy.RxFrameStartNtf;
import org.arl.unet.phy.TxFrameStartNtf;

public class MyCSMA extends UnetAgent {

   public enum CSMAParam implements Parameter {
      minBackoff,
      maxBackoff,
      reservationsPending,
      phy;
   }

   public static final String title = "Carrier-sense multiple access";
   public static final String description = "Carrier-sense multiple access (CSMA) medium access control (MAC) protocol.";
   private static final int MAX_FRAME_DURATION = 1500;
   private static final int GUARD_TIME = 500;
   private AgentLocalRandom rnd;
   private AgentID phy;
   private AgentID rdp;
   private Queue<ReservationReq> incomingReqQ = new ArrayDeque<>();
   private boolean busy = false;
   private boolean processing = false;
   private ReservationReq current = null;
   private WakerBehavior timer = null;
   private long t0;
   private float minBackoff = 0.5F;
   private float maxBackoff = 30.0F;
   private float maxReservationDuration = 60.0F;
   private float recommendedReservationDuration = 15.0F;
   private int csbusy = 0;
   private long busyUntil = 0L;
   private List<Integer> nodeList;
   private int NUM_NODES;

   private void discoverNodes(AgentID rdp) {
      nodeList = new ArrayList<>();

      int attempts = 2; // try only a single attempt at discovery
      int phantom = 132; // non-existent node address
      int timeout = 10000; // 10 second timeout

      RouteDiscoveryReq l = new org.arl.unet.net.RouteDiscoveryReq(phantom);
      l.setCount(attempts);

      rdp.send(l);
      Message ntf;

      while ((ntf = receive(org.arl.unet.net.RouteDiscoveryNtf.class, timeout)) != null) {
         println("  Discovered neighbor: " + ((RouteDiscoveryNtf) ntf).getNextHop());
         nodeList.add(((RouteDiscoveryNtf) ntf).getNextHop());
      }

      nodeList = nodeList.stream().distinct().collect(Collectors.toList());
      println("Neighbors are " + nodeList);
      NUM_NODES = nodeList.size();
   }

   protected void setup() {
      this.t0 = this.currentTimeMillis();
      this.rnd = AgentLocalRandom.current();
      this.register(Services.MAC);
      this.register(Services.DATAGRAM);
      this.register(Services.ROUTE_MAINTENANCE);
      this.addCapability(MacCapability.TTL);
   }

   protected void startup() {
      if (this.phy == null) {
         this.phy = this.agentForService(Services.PHYSICAL);
      }

      if (this.phy == null) {
         this.log.warning("No PHY found, carrier sensing disabled!");
      }

      this.subscribe(this.topic(this.phy, "snoop"));
      this.rdp = this.agentForService(Services.ROUTE_MAINTENANCE);
      this.subscribe(this.topic(this.rdp));

      AgentID datagram = this.agentForService(org.arl.unet.Services.DATAGRAM);
      this.subscribe(this.topic(datagram));

      discoverNodes(this.rdp);
   }

   protected List<Parameter> getParameterList() {
      return this.allOf(new Class[] { MacParam.class, CSMAParam.class });
   }

   public boolean getChannelBusy() {
      if (this.busy) {
         return true;
      } else {
         return this.phyBusy();
      }
   }

   public int getReservationsPending() {
      return this.incomingReqQ.size();
   }

   public int getReservationPayloadSize() {
      return 0;
   }

   public int getAckPayloadSize() {
      return 0;
   }

   public float getMaxReservationDuration() {
      return this.maxReservationDuration;
   }

   public void setMaxReservationDuration(float x) {
      if (x >= 0.0F) {
         this.maxReservationDuration = x;
      }
   }

   public float getRecommendedReservationDuration() {
      return this.recommendedReservationDuration;
   }

   public void setRecommendedReservationDuration(float x) {
      if (x >= 0.0F) {
         this.recommendedReservationDuration = x;
      }
   }

   public float getMaxBackoff() {
      return this.maxBackoff;
   }

   public void setMaxBackoff(float max) {
      if (this.maxBackoff >= 0.0F) {
         this.maxBackoff = max;
      }
   }

   public float getMinBackoff() {
      return this.minBackoff;
   }

   public void setMinBackoff(float min) {
      if (this.minBackoff >= 0.0F) {
         this.minBackoff = min;
      }
   }

   public AgentID getPhy() {
      return this.phy;
   }

   public AgentID setPhy(AgentID phy) {
      this.phy = phy;
      return phy;
   }

   public String setPhy(String phy) {
      if (phy == null) {
         this.phy = null;
      } else {
         this.phy = new AgentID(phy);
      }

      return phy;
   }

   protected Message processRequest(Message msg) {
      this.log.warning(String.valueOf(msg.getClass()));

      if (msg instanceof ReservationReq) {
         ReservationReq rmsg = (ReservationReq) msg;
         float duration = rmsg.getDuration();
         if (duration > 0.0F && duration <= this.maxReservationDuration) {
            if (rmsg.getStartTime() != null) {
               return new RefuseRsp(msg, "Timed reservations not supported");
            } else {
               float ttl = rmsg.getTtl();
               if (!Float.isNaN(ttl)) {
                  ttl += (float) (this.currentTimeMillis() - this.t0) / 1000.0F;
                  rmsg.setTtl(ttl);
               }

               this.log.fine("Reservation request " + rmsg.getMessageID() + " queued");
               this.incomingReqQ.add(rmsg);
               if (!this.processing) {
                  this.processReservationReq();
               }

               return new ReservationRsp(msg);
            }
         } else {
            return new RefuseRsp(msg, "Bad reservation duration");
         }
      } else if (msg instanceof ReservationCancelReq) {
         return (Message) (this.processReservationCancelReq((ReservationCancelReq) msg)
               ? new Message(msg, Performative.AGREE)
               : new RefuseRsp(msg, "No such reservation"));
      } else {
         return null;
      }
   }

   protected void processMessage(Message msg) {
      Integer duration;
      if (msg instanceof RxFrameStartNtf) {
         duration = ((RxFrameStartNtf) msg).getRxDuration();
         if (duration == null) {
            duration = 1500;
         }

         this.busyUntil = this.currentTimeMillis() + (long) duration + 500L;
      }

      if (msg instanceof RxFrameNtf) {
         this.busyUntil = this.currentTimeMillis() + 500L;
      } else if (msg instanceof TxFrameStartNtf) {
         duration = ((TxFrameStartNtf) msg).getTxDuration();
         if (duration == null) {
            duration = 1500;
         }

         this.busyUntil = this.currentTimeMillis() + (long) duration + 500L;
      }

   }

   private void processReservationReq() {
      if (!this.incomingReqQ.isEmpty()) {
         long backoff = this.computeBackoff();
         if (backoff == 0L) {
            this.processing = true;
            this.add(new OneShotBehavior() {
               public void action() {
                  MyCSMA.this.grantReservationReq();
               }
            });
         } else {
            this.processing = true;
            this.add(new WakerBehavior(backoff) {
               public void onWake() {
                  MyCSMA.this.grantReservationReq();
               }
            });
         }

      }
   }

   private long computeBackoff() {
      double backoff = (double) (this.phyFrameDuration() * (float) ((1 << this.csbusy) - 1));
      if (backoff < (double) this.minBackoff) {
         backoff = (double) this.minBackoff;
      } else if (backoff > (double) this.maxBackoff) {
         backoff = (double) this.maxBackoff;
      }

      return backoff == 0.0D ? 0L : Math.round(1000.0D * this.rnd.nextDouble(0.0D, backoff));
   }

   private boolean phyBusy() {
      if (this.busyUntil > this.currentTimeMillis()) {
         return true;
      } else if (this.phy == null) {
         return false;
      } else {
         Object rsp = this.get(this.phy, PhysicalParam.busy);
         return rsp != null && (Boolean) rsp;
      }
   }

   private float phyFrameDuration() {
      if (this.phy == null) {
         return 1.0F;
      } else {
         Object rsp = this.get(this.phy, 2, PhysicalChannelParam.frameDuration);
         return rsp != null ? ((Number) rsp).floatValue() : 1.0F;
      }
   }

   private void grantReservationReq() {
      if (this.incomingReqQ.isEmpty()) {
         this.processing = false;
      } else if (this.phyBusy()) {
         ++this.csbusy;
         this.log.fine("Carrier sense BUSY: " + this.csbusy);
         this.processing = false;
         this.processReservationReq();
      } else {
         this.csbusy = 0;
         final ReservationReq req = (ReservationReq) this.incomingReqQ.poll();
         float ttl = req.getTtl();
         if (!Float.isNaN(ttl) && ttl < (float) (this.currentTimeMillis() - this.t0) / 1000.0F) {
            this.log.fine("Reservation request " + req.getMessageID() + " ttl expired");
            this.processing = false;
            this.send(this.createNtfMsg(req, ReservationStatus.FAILURE));
            this.processReservationReq();
         } else {
            this.log.fine("Reservation request " + req.getMessageID() + " granted");
            this.current = req;
            this.busy = true;
            this.send(this.createNtfMsg(req, ReservationStatus.START));
            this.timer = new WakerBehavior((long) Math.ceil((double) (req.getDuration() * 1000.0F))) {
               public void onWake() {
                  this.log.fine("Reservation request " + req.getMessageID() + " completed");
                  MyCSMA.this.send(MyCSMA.this.createNtfMsg(req, ReservationStatus.END));
                  MyCSMA.this.current = null;
                  MyCSMA.this.busy = false;
                  MyCSMA.this.processing = false;
                  MyCSMA.this.timer = null;
                  MyCSMA.this.processReservationReq();
               }
            };
            this.add(this.timer);
         }
      }
   }

   private boolean processReservationCancelReq(ReservationCancelReq msg) {
      String id = msg.getId();
      if (this.current == null || id != null && !id.equals(this.current.getMessageID())) {
         if (this.current != null) {
            return false;
         } else {
            Iterator<ReservationReq> iter = this.incomingReqQ.iterator();

            ReservationReq req;
            do {
               if (!iter.hasNext()) {
                  return false;
               }

               req = (ReservationReq) iter.next();
            } while (!req.getMessageID().equals(id));

            this.log.fine("Reservation request " + id + " cancelled");
            iter.remove();
            this.send(this.createNtfMsg(req, ReservationStatus.CANCEL));
            return true;
         }
      } else {
         if (this.timer != null) {
            this.timer.stop();
         }

         this.add(new OneShotBehavior() {
            public void action() {
               this.log.fine("Ongoing reservation request cancelled");
               MyCSMA.this.send(MyCSMA.this.createNtfMsg(MyCSMA.this.current, ReservationStatus.END));
               MyCSMA.this.current = null;
               MyCSMA.this.busy = false;
               MyCSMA.this.processing = false;
               MyCSMA.this.timer = null;
               MyCSMA.this.processReservationReq();
            }
         });
         return true;
      }
   }

   private ReservationStatusNtf createNtfMsg(ReservationReq req, ReservationStatus status) {
      ReservationStatusNtf ntfMsg = new ReservationStatusNtf(req);
      ntfMsg.setStatus(status);
      this.trace(req, ntfMsg);
      return ntfMsg;
   }

   public static void main(String[] args) {
      System.out.println("Run");
   }
}
