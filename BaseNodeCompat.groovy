import org.arl.fjage.*
import org.arl.unet.*
import org.arl.fjage.param.Parameter;
import org.arl.unet.DatagramReq
import org.arl.fjage.WakerBehavior
import org.arl.fjage.OneShotBehavior

class BaseNodeCompat extends UnetAgent {
  
  final String title = 'Base Node Compat'        
  final String description = 'For Analysis using simulations'

  class Protocols  { // Protocols used by BaseNode
    final static int TDMA = 35 // TDMA protocol
    final static int CSMA = 36 // CSMA protocol
  }

  PDU pos = PDU.withFormat  { // Position PDU format
    int16('x') // x coordinate
    int16('y') // y coordinate
    int16('z') // z coordinate
  }

  int delayLength = 20000 // Default delay length             
  int tdmaSlotLength = 2500 // Default TDMA slot length
  int csmaSlotLength = 10000 // Default CSMA slot length
  TreeSet neighbours = [] // Set of neighbours
  AgentID phy // Agent ID for the physical layer

  int countNodes;

  BaseNodeCompat(int nodes)
  {
    countNodes = nodes;
  } 

  @Override
  void startup() { // Startup method called when the node starts
    subscribeForService(Services.DATAGRAM) // Subscribe to DATAGRAM service
    phy = agentForService Services.PHYSICAL // Get agent for PHYSICAL service

    if (phy == null) { // Check if physical agent is null
      phy = agentForService Services.PHYSICAL // Get agent for PHYSICAL service
    }

    subscribe topic(phy) // Subscribe to physical agent's topic

    for(int i = 2; i<=countNodes; i++)
        neighbours.add(i);
  }

  @Override
  void processMessage(Message msg) { // Method to process received messages
    if (msg instanceof DatagramNtf && msg.protocol == Protocols.TDMA) { // Check if received message is TDMA
      def bytes = pos.decode(msg.data) // Decode TDMA message
      log.info "TDMA Data received, Location: " + bytes.x+","+bytes.y+","+bytes.z // Log TDMA data
    }
    else if (msg instanceof DatagramNtf && msg.protocol == Protocols.CSMA) { // Check if received message is CSMA
      log.info "CSMA Data received, Data: " + msg.data // Log CSMA data
    }
  }
}