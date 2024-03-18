import org.arl.fjage.*
import org.arl.unet.*
import org.arl.fjage.param.Parameter
import org.arl.fjage.WakerBehavior
import org.arl.fjage.TickerBehavior
import org.arl.unet.phy.*

class DataNodeCompat extends UnetAgent {

  final String title = 'Data Node Compat'        
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

  int delayLength = 0 // Default delay length             
  int tdmaSlotLength = 1500 // Default TDMA slot length
  int csmaSlotLength = 2000 // Default CSMA slot length
  ArrayList neighbours = [] // Set of neighbours
  AgentID phy // Agent ID for the physical layer

  AgentLocalRandom rnd // Random number generator
  AgentID node // Agent ID for the node

  int countNodes;

  DataNodeCompat(int nodes)
  {
    countNodes = nodes;
  } 

  @Override
  void startup() { // Startup method called when the node starts
    rnd = AgentLocalRandom.current() // Initialize random number generator
    subscribeForService Services.DATAGRAM // Subscribe to DATAGRAM service
    phy = agentForService Services.PHYSICAL // Get agent for PHYSICAL service
    node = agentForService Services.NODE_INFO // Get agent for NODE_INFO service

    subscribe(topic(phy, Physical.SNOOP)) // Subscribe to physical layer with snoop option

    for(int i = 2; i<=countNodes; i++)
        neighbours.add(i);

    add new TickerBehavior(delayLength+neighbours.size()*tdmaSlotLength+csmaSlotLength,{
        process()
    })
  }

  void process()
  {
    int slot = Arrays.binarySearch(neighbours.toArray(), node.address)
    log.info "TDMA Started with assigned slot " + slot

    if(slot>=0) // Check if slot is valid
        add new WakerBehavior(tdmaSlotLength*slot, { 
            log.info 'Transmitting in TDMA...' // Log TDMA transmission
            def bytes = pos.encode(x:node.location[0], y:node.location[1], z:node.location[2])

            phy << new ClearReq()
            phy << new DatagramReq( // Send position data
                to: 1,
                protocol: Protocols.TDMA,
                data: bytes
            )
        })

    //the random delay in the next line is to simulate randomised sensor data, not backoff

    int senseTime = rnd.nextInt(Math.max(1,csmaSlotLength-2000)) // Generate random sense time
    int dataGathered = rnd.nextInt(3) // Generate random data gathering status

    if(dataGathered != 2) { // Check if data is gathered
        add new WakerBehavior(tdmaSlotLength*neighbours.size() + senseTime,  { // Schedule CSMA transmission after sense time
            if(!phy.busy){ // Check if channel is not busy
                log.info 'Transmitting in CSMA...' // Log CSMA transmission
                phy << new ClearReq()
                phy << new DatagramReq( // Send CSMA transmission
                    to: 1,
                    protocol: Protocols.CSMA,
                    data: dataGathered
                )
            }
            else{
                log.info 'Backoff due to clash...' // Log backoff due to clash
                add new WakerBehavior(Math.min(1500,rnd.nextInt(Math.max(1,csmaSlotLength-2000-senseTime))),  { // Schedule transmission after random backoff time
                    phy << new ClearReq()
                    phy << new DatagramReq(
                        to: 1,
                        protocol: Protocols.CSMA,
                        data: dataGathered
                    )
                })
            }
        })
    }
  }
}