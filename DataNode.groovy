import org.arl.fjage.*
import org.arl.unet.*
import org.arl.fjage.param.Parameter
import org.arl.fjage.WakerBehavior
import org.arl.unet.phy.*

class DataNode extends UnetAgent {

  final String title = 'Data Node'        
  final String description = 'Serves as data collection nodes in the network' 

  enum DataParams implements Parameter { // Enum for DataNode parameters
    tdmaSlot, // TDMA slot
    csmaSlotLength, // Length of CSMA slot
    tdmaSlotLength, // Length of TDMA slot
    delayLength // Delay length
  }

  class Protocols  { // Protocols used by DataNode
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

  int delayLength = 0 // Default delay length             
  int tdmaSlotLength = 0 // Default TDMA slot length
  int csmaSlotLength = 0 // Default CSMA slot length       

  TreeSet tdmaSlot = [] // Set of TDMA slots

  AgentID phy // Agent ID for the physical layer
  AgentID node // Agent ID for the node
  AgentLocalRandom rnd // Random number generator

  boolean channelBusy // Flag indicating whether the channel is busy

  int NBTx = 0 // Number of transmissions
  int TDMATx = 0 // TDMA transmissions
  int CSMATx = 0 // CSMA transmissions
  int NBRx = 0 // Number of receptions
  int TDMARx = 0 // TDMA receptions

  @Override
  void startup() { // Startup method called when the node starts
    rnd = AgentLocalRandom.current() // Initialize random number generator
    subscribeForService Services.DATAGRAM // Subscribe to DATAGRAM service
    phy = agentForService Services.PHYSICAL // Get agent for PHYSICAL service
    node = agentForService Services.NODE_INFO // Get agent for NODE_INFO service

    subscribe(topic(phy, Physical.SNOOP)) // Subscribe to physical layer with snoop option
  }

  @Override
  void processMessage(Message msg) { // Method to process received messages
    if (msg instanceof DatagramNtf && msg.protocol == Protocols.INIT) { // Check if received message is INIT protocol
      log.info "Data Node: NBRx: $NBRx, TDMARx: $TDMARx, NBTx: $NBTx, TDMATx: $TDMATx, CSMATx: $CSMATx" // Log reception and transmission statistics
      def bytes = init.decode(msg.data) // Decode initialization data
      NBRx++ // Increment reception counter

      csmaSlotLength = bytes.csmaSlotLength // Update CSMA slot length
      tdmaSlotLength = bytes.tdmaSlotLength // Update TDMA slot length
      delayLength = bytes.delayLength // Update delay length

      long backoff = rnd.nextDouble(0, delayLength-2000) // Calculate backoff time
      log.info "Discovery request from ${msg.from}, will respond after ${backoff} ms" // Log discovery request
      add new WakerBehavior(backoff, { // Schedule response after backoff time
        bytes = ack.encode(address:node.address) // Encode acknowledgment
        if(!phy.busy){ // Check if channel is not busy
          log.info 'Responding...' // Log response

          phy << new ClearReq()
          phy << new DatagramReq( // Send acknowledgment
            recipient: msg.sender,
            to: msg.from,
            protocol: Protocols.ACK,
            data: bytes
          )
          NBTx++ // Increment transmission counter
        }
        else{
          log.info 'Backoff due to clash...' // Log backoff due to clash
          add new WakerBehavior(rnd.nextInt(delayLength-2000-(int)backoff),  { // Schedule transmission after random backoff time
            phy << new ClearReq()
            phy << new DatagramReq(
              recipient: msg.sender,
              to: msg.from,
              protocol: Protocols.ACK,
              data: bytes
            )
            NBTx++ // Increment transmission counter
          })
        }
      })
    }

    else if(msg instanceof DatagramNtf && msg.protocol == Protocols.TDMA_INIT) { // Check if received message is TDMA_INIT protocol
      TDMARx++ // Increment TDMA reception counter
      int slot = Arrays.binarySearch(msg.data, (Byte)node.address) // Get assigned slot
      log.info "TDMA Started with assigned slot " + slot // Log TDMA slot assignment

      if(slot>=0) // Check if slot is valid
        add new WakerBehavior(tdmaSlotLength*slot, { // Schedule transmission in assigned TDMA slot
          log.info 'Transmitting in TDMA...' // Log TDMA transmission
          def bytes = pos.encode(x:node.location[0], y:node.location[1], z:node.location[2]) // Encode position data

          phy << new ClearReq()
          phy << new DatagramReq( // Send position data
            recipient: msg.sender,
            to: msg.from,
            protocol: Protocols.TDMA,
            data: bytes
          )

          TDMATx++ // Increment TDMA transmission counter
        })

      //the random delay in the next line is to simulate randomised sensor data, not backoff

      int senseTime = rnd.nextInt(csmaSlotLength-2000) // Generate random sense time
      int dataGathered = rnd.nextInt(3) // Generate random data gathering status

      if(dataGathered != 2) { // Check if data is gathered
        add new WakerBehavior(tdmaSlotLength*msg.data.size() + senseTime,  { // Schedule CSMA transmission after sense time
          if(!phy.busy){ // Check if channel is not busy
            log.info 'Transmitting in CSMA...' // Log CSMA transmission
            phy << new ClearReq()
            phy << new DatagramReq( // Send CSMA transmission
              recipient: msg.sender,
              to: msg.from,
              protocol: Protocols.CSMA,
              data: dataGathered
            )
            CSMATx++ // Increment CSMA transmission counter
          }
          else{
            log.info 'Backoff due to clash...' // Log backoff due to clash
            add new WakerBehavior(Math.min(1500,rnd.nextInt(csmaSlotLength-2000-senseTime)),  { // Schedule transmission after random backoff time
              phy << new ClearReq()
              phy << new DatagramReq(
                recipient: msg.sender,
                to: msg.from,
                protocol: Protocols.CSMA,
                data: dataGathered
              )
              CSMATx++ // Increment CSMA transmission counter
            })
          }
        })
      }
    }
  }

  List<Parameter> getParameterList() {      
    allOf(DataParams)
  }
}import org.arl.fjage.*
import org.arl.unet.*
import org.arl.fjage.param.Parameter
import org.arl.fjage.WakerBehavior
import org.arl.unet.phy.*

class DataNode extends UnetAgent {

  final String title = 'Data Node'        
  final String description = 'Serves as data collection nodes in the network' 

  enum DataParams implements Parameter { // Enum for DataNode parameters
    tdmaSlot, // TDMA slot
    csmaSlotLength, // Length of CSMA slot
    tdmaSlotLength, // Length of TDMA slot
    delayLength // Delay length
  }

  class Protocols  { // Protocols used by DataNode
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

  int delayLength = 0 // Default delay length             
  int tdmaSlotLength = 0 // Default TDMA slot length
  int csmaSlotLength = 0 // Default CSMA slot length       

  TreeSet tdmaSlot = [] // Set of TDMA slots

  AgentID phy // Agent ID for the physical layer
  AgentID node // Agent ID for the node
  AgentLocalRandom rnd // Random number generator

  boolean channelBusy // Flag indicating whether the channel is busy

  int NBTx = 0 // Number of transmissions
  int TDMATx = 0 // TDMA transmissions
  int CSMATx = 0 // CSMA transmissions
  int NBRx = 0 // Number of receptions
  int TDMARx = 0 // TDMA receptions

  @Override
  void startup() { // Startup method called when the node starts
    rnd = AgentLocalRandom.current() // Initialize random number generator
    subscribeForService Services.DATAGRAM // Subscribe to DATAGRAM service
    phy = agentForService Services.PHYSICAL // Get agent for PHYSICAL service
    node = agentForService Services.NODE_INFO // Get agent for NODE_INFO service

    subscribe(topic(phy, Physical.SNOOP)) // Subscribe to physical layer with snoop option
  }

  @Override
  void processMessage(Message msg) { // Method to process received messages
    if (msg instanceof DatagramNtf && msg.protocol == Protocols.INIT) { // Check if received message is INIT protocol
      log.info "Data Node: NBRx: $NBRx, TDMARx: $TDMARx, NBTx: $NBTx, TDMATx: $TDMATx, CSMATx: $CSMATx" // Log reception and transmission statistics
      def bytes = init.decode(msg.data) // Decode initialization data
      NBRx++ // Increment reception counter

      csmaSlotLength = bytes.csmaSlotLength // Update CSMA slot length
      tdmaSlotLength = bytes.tdmaSlotLength // Update TDMA slot length
      delayLength = bytes.delayLength // Update delay length

      long backoff = rnd.nextDouble(0, delayLength-2000) // Calculate backoff time
      log.info "Discovery request from ${msg.from}, will respond after ${backoff} ms" // Log discovery request
      add new WakerBehavior(backoff, { // Schedule response after backoff time
        bytes = ack.encode(address:node.address) // Encode acknowledgment
        if(!phy.busy){ // Check if channel is not busy
          log.info 'Responding...' // Log response

          phy << new ClearReq()
          phy << new DatagramReq( // Send acknowledgment
            recipient: msg.sender,
            to: msg.from,
            protocol: Protocols.ACK,
            data: bytes
          )
          NBTx++ // Increment transmission counter
        }
        else{
          log.info 'Backoff due to clash...' // Log backoff due to clash
          add new WakerBehavior(rnd.nextInt(delayLength-2000-(int)backoff),  { // Schedule transmission after random backoff time
            phy << new ClearReq()
            phy << new DatagramReq(
              recipient: msg.sender,
              to: msg.from,
              protocol: Protocols.ACK,
              data: bytes
            )
            NBTx++ // Increment transmission counter
          })
        }
      })
    }

    else if(msg instanceof DatagramNtf && msg.protocol == Protocols.TDMA_INIT) { // Check if received message is TDMA_INIT protocol
      TDMARx++ // Increment TDMA reception counter
      int slot = Arrays.binarySearch(msg.data, (Byte)node.address) // Get assigned slot
      log.info "TDMA Started with assigned slot " + slot // Log TDMA slot assignment

      if(slot>=0) // Check if slot is valid
        add new WakerBehavior(tdmaSlotLength*slot, { // Schedule transmission in assigned TDMA slot
          log.info 'Transmitting in TDMA...' // Log TDMA transmission
          def bytes = pos.encode(x:node.location[0], y:node.location[1], z:node.location[2]) // Encode position data

          phy << new ClearReq()
          phy << new DatagramReq( // Send position data
            recipient: msg.sender,
            to: msg.from,
            protocol: Protocols.TDMA,
            data: bytes
          )

          TDMATx++ // Increment TDMA transmission counter
        })

      //the random delay in the next line is to simulate randomised sensor data, not backoff

      int senseTime = rnd.nextInt(csmaSlotLength-2000) // Generate random sense time
      int dataGathered = rnd.nextInt(3) // Generate random data gathering status

      if(dataGathered != 2) { // Check if data is gathered
        add new WakerBehavior(tdmaSlotLength*msg.data.size() + senseTime,  { // Schedule CSMA transmission after sense time
          if(!phy.busy){ // Check if channel is not busy
            log.info 'Transmitting in CSMA...' // Log CSMA transmission
            phy << new ClearReq()
            phy << new DatagramReq( // Send CSMA transmission
              recipient: msg.sender,
              to: msg.from,
              protocol: Protocols.CSMA,
              data: dataGathered
            )
            CSMATx++ // Increment CSMA transmission counter
          }
          else{
            log.info 'Backoff due to clash...' // Log backoff due to clash
            add new WakerBehavior(Math.min(1500,rnd.nextInt(csmaSlotLength-2000-senseTime)),  { // Schedule transmission after random backoff time
              phy << new ClearReq()
              phy << new DatagramReq(
                recipient: msg.sender,
                to: msg.from,
                protocol: Protocols.CSMA,
                data: dataGathered
              )
              CSMATx++ // Increment CSMA transmission counter
            })
          }
        })
      }
    }
  }

  List<Parameter> getParameterList() {      
    allOf(DataParams)
  }
}