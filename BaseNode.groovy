import org.arl.fjage.*
import org.arl.unet.*
import org.arl.fjage.param.Parameter;
import org.arl.unet.DatagramReq
import org.arl.fjage.WakerBehavior
import org.arl.fjage.OneShotBehavior

class BaseNode extends UnetAgent {
  
  final String title = 'Base Node'        
  final String description = 'Serves as the base for a network of nodes' 

  enum BaseParams implements Parameter { // Enum for BaseNode parameters
    tdmaSlotLength, // Length of TDMA slot
    csmaSlotLength, // Length of CSMA slot
    delayLength, // Delay length for neighbour discovery
    neighbours // Neighbours of the BaseNode
  }

  class Protocols  { // Protocols used by BaseNode
    final static int INIT = 32 // Initialization protocol
    final static int ACK = 33 // Acknowledgment protocol
    final static int TDMA_INIT = 34 // TDMA initialization protocol
    final static int TDMA = 35 // TDMA protocol
    final static int CSMA = 36 // CSMA protocol
  }

  PDU init = PDU.withFormat { // Initialization PDU format
    uint16('tdmaSlotLength') // TDMA slot length
    uint16('csmaSlotLength') // CSMA slot length
    uint16('delayLength')  // Delay length
  }

  PDU ack = PDU.withFormat { // Acknowledgment PDU format
    int16('address') // Address of the acknowledgment
  }

  PDU pos = PDU.withFormat  { // Position PDU format
    int16('x') // x coordinate
    int16('y') // y coordinate
    int16('z') // z coordinate
  }

  int delayLength = 20000 // Default delay length             
  int tdmaSlotLength = 2500 // Default TDMA slot length
  int csmaSlotLength = 10000 // Default CSMA slot length
  int buffer = 0 // Buffer size
  TreeSet neighbours = [] // Set of neighbours
  AgentID phy // Agent ID for the physical layer

  int NBTx = 0 // Number of transmissions
  int TDMATx = 0 // TDMA transmissions
  int NBRx = 0 // Number of receptions
  int TDMARx = 0 // TDMA receptions
  int CSMARx = 0 // CSMA receptions

  @Override
  void startup() { // Startup method called when the node starts
    subscribeForService(Services.DATAGRAM) // Subscribe to DATAGRAM service
    phy = agentForService Services.PHYSICAL // Get agent for PHYSICAL service

    if (phy == null) { // Check if physical agent is null
      phy = agentForService Services.PHYSICAL // Get agent for PHYSICAL service
    }

    subscribe topic(phy) // Subscribe to physical agent's topic
    
    add new WakerBehavior(3000, { // Schedule neighbourBroadcast to start after 3000ms
      neighbourBroadcast()
    })
  }

  void neighbourBroadcast() { // Method to broadcast neighbour information
    add new WakerBehavior(csmaSlotLength, { // Schedule neighbour broadcast every CSMA slot length
      log.info "Base Node: NBTx: $NBTx, TDMATx: $TDMATx, NBRx: $NBRx, TDMARx: $TDMARx, CSMARx: $CSMARx" // Log transmission and reception statistics
      log.info "Starting Neighbour Discovery..." // Log neighbour discovery start
      neighbours = [] // Clear neighbours set
      def bytes = init.encode(tdmaSlotLength: tdmaSlotLength, csmaSlotLength:csmaSlotLength, delayLength:delayLength) // Encode initialization data
      add new OneShotBehavior({ // Send initialization datagram
        phy << new ClearReq()
        phy << new DatagramReq(
          protocol: Protocols.INIT, // Set protocol to INIT
          data: bytes // Set data to encoded initialization data
        )
      })
      NBTx++ // Increment transmission counter

      tdmaBroadcast() // Start TDMA broadcast
    })
  }

  void tdmaBroadcast()  { // Method to broadcast TDMA information
    add new WakerBehavior(delayLength, { // Schedule TDMA broadcast after delayLength
      log.info "Broadcasting TDMA time slots..." // Log TDMA broadcast start
      add new OneShotBehavior({ // Send TDMA initialization datagram
          phy << new ClearReq()
          phy << new DatagramReq(
            protocol: Protocols.TDMA_INIT, // Set protocol to TDMA_INIT
            data: neighbours // Set data to neighbours set
          )
        })
      TDMATx++ // Increment TDMA transmission counter
      
      csmaMode() // Start CSMA broadcast
    })
  }

  void csmaMode()  { // Method to broadcast CSMA information
    add new WakerBehavior(neighbours.size()*tdmaSlotLength, // Schedule neighbour broadcast duration
    {   
      log.info "Starting CSMA..." // Log start of CSMA
      neighbourBroadcast() // Restart neighbour broadcast
    })
  }

  @Override
  void processMessage(Message msg) { // Method to process received messages
    if (msg instanceof DatagramNtf && msg.protocol == Protocols.ACK) { // Check if received message is ACK
      log.info "Node acknowledged" // Log acknowledgment
      def bytes = ack.decode(msg.data) // Decode acknowledgment message
      neighbours.add(bytes.address) // Add neighbor's address to set
      NBRx++ // Increment reception counter
    }
    else if (msg instanceof DatagramNtf && msg.protocol == Protocols.TDMA) { // Check if received message is TDMA
      def bytes = pos.decode(msg.data) // Decode TDMA message
      log.info "TDMA Data received, Location: " + bytes.x+","+bytes.y+","+bytes.z // Log TDMA data
      TDMARx++ // Increment TDMA reception counter
    }
    else if (msg instanceof DatagramNtf && msg.protocol == Protocols.CSMA) { // Check if received message is CSMA
      log.info "CSMA Data received, Data: " + msg.data // Log CSMA data
      CSMARx++ // Increment CSMA reception counter
    }
  }

  List<Parameter> getParameterList() {      
    allOf(BaseParams)
  }
}