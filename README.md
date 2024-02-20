# Network Simulation

This readme provides an overview of the code and log files related to a network simulation.

## Code Overview

### BaseNode Class

The `BaseNode` class, defined in the provided code (`BaseNode.groovy`), represents a node in a network simulation using the UNet framework. It includes functionalities for neighbor discovery, TDMA broadcasting.

#### Functionality Highlights:

- **Neighbor Discovery**: The `BaseNode` periodically broadcasts initialization datagrams to nearby nodes for neighbor discovery.

- **TDMA Broadcasting**: After discovering neighbors, the `BaseNode` broadcasts TDMA time slots to coordinate communication.

#### Parameters:

The `BaseNode` class defines the following parameters:

- `tdmaSlotLength`: Length of TDMA time slots.
- `csmaSlotLength`: Length of CSMA time slots.
- `delayLength`: Delay length between actions.
- `neighbours`: List of neighboring nodes.

#### Communication Protocols:

The class defines the following communication protocols:

- `INIT`: Initialization protocol.
- `ACK`: Acknowledgment protocol.
- `TDMA_INIT`: TDMA initialization protocol.
- `TDMA`: TDMA data transmission protocol.
- `CSMA`: CSMA data transmission protocol.

#### Message Processing:

The class includes a method for processing incoming messages, handling acknowledgments, TDMA data, and CSMA data.

### DataNode Class

Similarly, the `DataNode` class, defined in the provided code (`DataNode.groovy`), represents a data node in the network simulation. It includes functionalities for processing messages, handling initialization requests, and managing TDMA and CSMA transmissions.

#### Functionality Highlights:

- **Initialization Handling**: The `DataNode` processes initialization requests from neighboring nodes, acknowledging requests and exchanging protocol parameters.

- **TDMA Transmission**: Upon receiving TDMA initialization, the `DataNode` starts transmitting data in assigned TDMA slots.

- **CSMA Transmission**: The `DataNode` initiates CSMA transmission after completing TDMA transmission, managing collisions and backoff periods.

#### Parameters:

The `DataNode` class defines parameters such as `tdmaSlot`, `csmaSlotLength`, `tdmaSlotLength`, and `delayLength`.

#### Communication Protocols:

Similar to `BaseNode`, the `DataNode` class defines communication protocols such as `INIT`, `ACK`, `TDMA_INIT`, `TDMA`, and `CSMA`.

#### Message Processing:

The class includes a method for processing incoming messages, handling initialization requests, TDMA initialization, and CSMA transmission.

## Log File Overview

The provided log file (`simulation.log`) captures interactions between different nodes in the network simulation. It includes details such as timestamps, node interactions, network operations, node acknowledgments, and more.

[Click here](https://github.com/BhavyaMehta2/UNET/blob/main/one_cycle.log) to view the log file.

Start

**BaseNode startup()**
- Initiates neighbor discovery process.
- Sends out a broadcast with data packets representing:
  - delayLength
  - tdmaSlotLength
  - csmaSlotLength
- Waits for responses from other nodes.
- Processes responses from data nodes:
  - If channel is busy, data nodes initiate a random backoff before responding.
  - Data nodes send back their addresses.
- Once all nodes are acknowledged, the base node orders their addresses in ascending order.
- Broadcasts the ordered list of addresses to assign TDMA time slots.
- Waits for responses from data nodes.
- Initiates transmission of location data in their assigned TDMA time slots.
- Once transmission is over, initiates data transmission using CSMA if there is data to be sent back.
- After TDMA slot broadcast is sent out, the base node waits for incoming messages until it is time again for neighbor broadcast.

**DataNode startup()**
- Receives initialization broadcast from the base node.
- Decodes initialization data packets to extract parameters such as:
  - delayLength
  - tdmaSlotLength
  - csmaSlotLength
- Responds to the base node with its address.
- If channel is busy, initiates a random backoff before responding.
- Waits for acknowledgment from the base node.
- Upon acknowledgment, awaits further instructions.
- Receives the ordered list of addresses from the base node to assign TDMA time slots.
- Waits for TDMA slot transmission from the base node.
- Initiates transmission of location data in its assigned TDMA time slot.
- Waits for data transmission instructions from the base node.
- If data needs to be sent back, initiates transmission using CSMA.
- Waits for further instructions after completing transmissions.

End

Start

**BaseNode startup()**
- Subscribe for DATAGRAM service to listen for incoming messages.
- Initialize phy agent for physical layer communication.
- If phy is null, attempt to initialize phy again.
- Subscribe to the topic for phy agent to receive messages from the physical layer.
- Add WakerBehavior to trigger periodic neighborBroadcast() for neighbor discovery.
- tdmaBroadcast() is triggered after the nodes are acknowledged.

**DataNode startup()**
- Initialize a random number generator (rnd) for randomization.
- Subscribe for DATAGRAM service to listen for incoming messages.
- Initialize phy agent for physical layer communication.
- Initialize node agent to obtain node information.
- Subscribe to the topic for phy agent to receive messages from the physical layer.
- Process incoming messages:
  - If DatagramNtf is received from another node:
    - Set channelBusy flag to indicate that the communication channel is busy.
    - Schedule WakerBehavior to reset the channelBusy flag after 1500 milliseconds.
  - If DatagramNtf with INIT protocol is received:
    - Decode initialization data and extract parameters such as tdmaSlotLength, csmaSlotLength, and delayLength.
    - Respond with an acknowledgment (ACK) if the channel is not busy.
    - If the channel is busy, schedule a WakerBehavior for backoff and CSMA transmission.
  - If DatagramNtf with TDMA_INIT protocol is received:
    - Decode TDMA initialization data and process it.
    - Setup transmission in assigned TDMA slots.
    - Schedule a WakerBehavior for CSMA transmission after completing TDMA transmission.
  - If DatagramNtf with other protocols is received:
    - Process other protocols (not detailed in the flowchart).

**BaseNode processMessage()**
- Process incoming messages:
  - If DatagramNtf with ACK protocol is received:
    - Decode acknowledgment data and update the neighbors list.
    - Increment the counter for received acknowledgments (NBRx).
  - If DatagramNtf with TDMA protocol is received:
    - Decode TDMA data and update counters.
    - Increment the counter for TDMA receptions (TDMARx).
    - Process TDMA data.
  - If DatagramNtf with CSMA protocol is received:
    - Process CSMA data.
    - Increment the counter for CSMA receptions (CSMARx).
- Provide access methods for retrieving parameter lists (not detailed in the flowchart).

End
